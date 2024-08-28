package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.system.SystemUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.CoverNameDTO;
import com.gust.cafe.windycrypto.dto.CryptoContext;
import com.gust.cafe.windycrypto.dto.NameConcatDTO;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.util.AesUtils;
import com.gust.cafe.windycrypto.util.PollUtils;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CryptoService {
    @Autowired
    @Qualifier(ThreadPoolConstants.DISPATCH)
    private ThreadPoolTaskExecutor dispatchTaskExecutor;
    //
    @Autowired
    @Qualifier(ThreadPoolConstants.CRYPTO)
    private ThreadPoolTaskExecutor cryptoTaskExecutor;
    //
    @Autowired
    private WindyCacheService windyCacheService;
    @Autowired
    private RedisMasterCache redisMasterCache;
    @Autowired
    private RedissonClient redissonClient;

    //
    public void actionAsync(List<String> absPathList, CryptoSubmitReqVo reqVo) {
        TimeInterval timer = DateUtil.timer();
        for (String beforePath : absPathList) {
            // 上下文对象记录必要信息,异步线程采用thenRunAsync,确保按顺序执行
            CryptoContext cryptoContext = CryptoContext.builder()
                    .askEncrypt(reqVo.getAskEncrypt())
                    .userPassword(reqVo.getUserPassword())
                    .userPasswordSha256Hex(DigestUtil.sha256Hex(reqVo.getUserPassword()))
                    .beforePath(beforePath)
                    .beforeCacheId(windyCacheService.parseId(beforePath))
                    .bitSwitchList(ListUtil.toList(1, 0, 0, 0))
                    .build();
            // 处理排队
            CompletableFuture<Void> future01 = CompletableFuture.runAsync(() -> futureQueue(cryptoContext), dispatchTaskExecutor);
            // 处理实际加解密
            CompletableFuture<Void> future02 = future01.thenRunAsync(() -> futureCrypto(cryptoContext), cryptoTaskExecutor);
            // 异常处理,处理文件回滚
            future02.exceptionally(captureUnknownExceptions(cryptoContext));
        }
        log.debug("耗时[{}]ms指定异步任务[{}]个", timer.intervalMs(), absPathList.size());
    }

    private void futureQueue(CryptoContext cryptoContext) {
        log.debug("[{}]-成功进入排队线程", cryptoContext.getBeforeCacheId());
        String beforePath = cryptoContext.getBeforePath();
        WindyException.run((Void) -> {
            Assert.isTrue(FileUtil.exist(beforePath),
                    "[{}]-{}", cryptoContext.getBeforeCacheId(), WindyLang.msg("i18n_1828354895514832896"));// 文件已经不存在,当前任务终止
            // 实时查询缓存
            Windy windy = windyCacheService.lockGetOrDefault(beforePath);
            // 状态要求FREE
            WindyStatusEnum anEnum = WindyStatusEnum.getByCode(windy.getCode());
            Assert.isTrue(anEnum != null && anEnum.equals(WindyStatusEnum.FREE),
                    "[{}]-{}", cryptoContext.getBeforeCacheId(), WindyLang.msg("i18n_1828354895519027200"));// 空闲状态,已有其他任务在处理,当前任务终止
        });

        // 满足条件则将状态更新为排队中
        Windy windy = windyCacheService.lockGetOrDefault(beforePath);
        windy.setCode(WindyStatusEnum.WAITING.getCode());
        windy.setLabel(WindyStatusEnum.WAITING.getLabel());
        windy.setDesc(WindyStatusEnum.WAITING.getRemark());
        windy.setLatestMsg("queued");
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        log.debug("[{}]-更新状态为WAITING", cryptoContext.getBeforeCacheId());
    }

    // 异步加解密阶段
    private void futureCrypto(CryptoContext cryptoContext) {
        log.debug("[{}]-成功进入加解密线程", cryptoContext.getBeforeCacheId());
        String beforePath = cryptoContext.getBeforePath();
        // 成功进入加解密阶段,更新状态
        Windy windy = windyCacheService.lockGetOrDefault(beforePath);
        WindyStatusEnum anEnum = WindyStatusEnum.getByCode(windy.getCode());
        Assert.isTrue(anEnum != null && anEnum.equals(WindyStatusEnum.WAITING), "状态码应该为WAITING,请检查代码");
        windy.setCode(WindyStatusEnum.OUTPUTTING.getCode());
        windy.setLabel(WindyStatusEnum.OUTPUTTING.getLabel());
        windy.setDesc(WindyStatusEnum.OUTPUTTING.getRemark());
        windy.setLatestMsg("outputting");
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        //
        // [FUTURE_CRYPTO]:处理临时文件,注册入缓存,记录上下文
        futureCryptoRegisterTmp(cryptoContext);

        // [FUTURE_CRYPTO]:处理加解密操作对应输入输出流
        futureCryptoStream(cryptoContext);

        // [FUTURE_CRYPTO]:处理盐值数组
        futureCryptoSalt(cryptoContext);

        // [FUTURE_CRYPTO]:处理核心IO操作
        futureCryptoCoreIO(cryptoContext);

        // [FUTURE_CRYPTO]:处理临时文件改名最终文件等相关操作
        futureCryptoFinal(cryptoContext);
    }

    private void futureCryptoRegisterTmp(CryptoContext cryptoContext) {
        log.debug("[{}]-成功进入临时文件注册线程", cryptoContext.getBeforeCacheId());
        String beforePath = cryptoContext.getBeforePath();
        // 临时文件名
        String tmpName = StrUtil.format("0000000000-{}-{}.{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.getSnowflakeNextIdStr(), CommonConstants.TMP_EXT_NAME);
        // 在源文件的同级目录下创建临时文件
        String tmpAbsPath = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(beforePath, 1), tmpName));
        Windy windyTmp = windyCacheService.lockGetOrDefault(tmpAbsPath);
        // 更新缓存状态信息
        windyTmp.setCode(WindyStatusEnum.INPUTTING.getCode());
        windyTmp.setLabel(WindyStatusEnum.INPUTTING.getLabel());
        windyTmp.setDesc(WindyStatusEnum.INPUTTING.getRemark());
        windyTmp.setLatestMsg("inputting");
        windyTmp.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyTmp.getId(), windyTmp);
        // 记录上下文
        cryptoContext.setTmpPath(tmpAbsPath);
        cryptoContext.setTmpCacheId(windyTmp.getId());
        log.debug("[{}]-临时文件注册成功:windyTmpId=[{}]", cryptoContext.getBeforeCacheId(), windyTmp.getId());
    }

    private void futureCryptoStream(CryptoContext cryptoContext) {
        String beforePath = cryptoContext.getBeforePath();
        String tmpPath = cryptoContext.getTmpPath();
        Assert.notBlank(beforePath, "beforePath不能为空");
        Assert.notBlank(tmpPath, "tmpPath不能为空");
        // 准备输入输出流
        BufferedInputStream bis = FileUtil.getInputStream(FileUtil.file(beforePath));
        BufferedOutputStream bos = FileUtil.getOutputStream(FileUtil.file(tmpPath));
        // 记录上下文
        cryptoContext.setBis(bis);
        cryptoContext.setBos(bos);
    }

    private void futureCryptoSalt(CryptoContext cryptoContext) {
        if (cryptoContext.getAskEncrypt()) {
            // 如果是加密操作,则生成盐值数组,三位大于0小于256的随机数
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                list.add(RandomUtil.randomInt(0, 256));
            }
            cryptoContext.setIntSaltList(list);
            cryptoContext.setIntSaltStr(list.stream().map(String::valueOf).collect(Collectors.joining(StrUtil.COMMA)));
            String intSaltStrEncryptHex = AesUtils.getAes(cryptoContext.getUserPassword()).encryptHex(cryptoContext.getIntSaltStr());
            cryptoContext.setIntSaltStrEncryptHex(intSaltStrEncryptHex);
            //
            log.debug("[{}]-本次请求加密,新生成盐值数组:[{}]", cryptoContext.getBeforeCacheId(), cryptoContext.getIntSaltStr());
        } else {
            // 如果是解密操作,则从文件名中解析盐值数组,此时需要校验密码是否正确
            CoverNameDTO coverNameDTO = CoverNameDTO.analyse(FileUtil.getName(cryptoContext.getBeforePath()), cryptoContext.getUserPassword());
            System.out.println("coverNameDTO=" + coverNameDTO);
        }

    }

    private void futureCryptoCoreIO(CryptoContext cryptoContext) {
        // 缓冲区大小
        try {
            // 读取源文件的缓存对象
            Windy windyBefore = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());

            // 每次读取的字节数
            Integer bufferSize = 1024;

            // 每次实际读取到字节数
            int len;

            // 已经读取的字节数
            long total = 0;

            // 缓冲区
            byte[] buffer = new byte[bufferSize];

            // 计时器,控制打印频率
            TimeInterval timer = DateUtil.timer();

            // 源文件大小,用于计算百分比
            long windyBeforeSize = windyBefore.getSize();

            // 记录所有字节的位置
            long position = 0;

            // 整数盐值列表
            List<Integer> intSaltList = cryptoContext.getIntSaltList();

            // 本次操作是否为加密
            Boolean askEncrypt = cryptoContext.getAskEncrypt();

            // 循环读取源文件的输入流,写入到临时文件的输出流
            while ((len = cryptoContext.getBis().read(buffer)) != -1) {
                // 此处已经将字节读取到缓冲区,不能在此缓冲区中直接修改,应该用一个新的字节数组来接收加盐后的字节,定义临时缓冲区副本
                byte[] newBuffer = new byte[len];
                // 对每个字节加减整数盐后重新收集:(1)加密时,增加整数盐;(2)解密时,减去整数盐
                for (int i = 0; i < len; i++) {
                    position = position + 1;
                    // 位置数值与整数盐值列表长度取余,得到当前位置对应的整数盐值
                    int salt = intSaltList.get((int) (position % intSaltList.size()));
                    // 加密时,增加整数盐;解密时,减去整数盐
                    newBuffer[i] = (byte) (buffer[i] + (askEncrypt ? salt : -salt));
                }
                // 加盐后的字节写入到临时文件的输出流
                cryptoContext.getBos().write(newBuffer);
                // 更新已经读取的字节数
                total += len;
                //
                // 流读取的频率是非常快的,如果每次都更新缓存和发布消息,会导致卡死,所以需要通过间隔时间控制频率
                if (timer.intervalMs() > 800L) {
                    // 计算当前百分比
                    Integer percentage = Convert.toInt(StrUtil.replaceLast(NumberUtil.formatPercent(NumberUtil.div(total, windyBeforeSize, 4), 0), "%", ""));
                    log.debug("[{}]-当前百分比:[{}%]", cryptoContext.getBeforeCacheId(), percentage);
                    // TODO 更新缓存状态信息
                    // TODO 更新缓存状态信息
                    // TODO 更新缓存状态信息
                    // 重置计时器,重新计时直至下一次周期
                    timer.restart();
                }
            }

            // 防止最后一次结果丢失,循环结束后指定百分比更新一次
            // TODO 直接更新100%
            log.debug("[{}]-当前百分比:[{}%]", cryptoContext.getBeforeCacheId(), 100);

            // 清除计时器
            timer.clear();
            timer = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(cryptoContext.getBis());
            IoUtil.close(cryptoContext.getBos());
        }
    }

    private void futureCryptoFinal(CryptoContext cryptoContext) {
        // 注册最终文件信息
        finalRegisterAfter(cryptoContext);
        // 处理临时文件改名
        finalTmpRenameToAfter(cryptoContext);
        // 处理临时文件删除、原始文件删除、最终文件开放状态等操作
        finalDel(cryptoContext);
    }

    private void finalTmpRenameToAfter(CryptoContext cryptoContext) {
        // GPT也是建议采用轮询解决"IO流未释放导致改名失败"的问题
        long intervalMs = 2_000L;
        long maxMs = 60_000L;
        // 业务操作回调
        Consumer<Void> actionCs = aVoid -> {
            IoUtil.close(cryptoContext.getBis());
            IoUtil.close(cryptoContext.getBos());
            File tmp = FileUtil.file(cryptoContext.getTmpPath());
            String afterName = FileUtil.getName(cryptoContext.getAfterPath());
            FileUtil.rename(tmp, afterName, false, true);
            // 健壮
            if (!FileUtil.exist(cryptoContext.getAfterPath())) {
                // 说明改名失败,抛出异常触发进入下一循环
                throw new CryptoException("本次改名失败,触发重试");
            }
        };
        // 成功回调
        TimeInterval timer = DateUtil.timer();
        Consumer<Void> successCs = aVoid -> {
            log.debug("[{}]-临时文件改名成功,耗时[{}]ms,[tmp={}]>>[after={}]",
                    cryptoContext.getBeforeCacheId(),
                    timer.intervalMs(),
                    cryptoContext.getTmpCacheId()
                    , cryptoContext.getAfterCacheId()
            );
            // 三个文件都更新状态,改名成功说明临时文件已经不存在,最终文件生成成功
            Windy windyBefore = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
            windyBefore.setCode(WindyStatusEnum.ALMOST.getCode());
            windyBefore.setLabel(WindyStatusEnum.ALMOST.getLabel());
            windyBefore.setDesc(WindyStatusEnum.ALMOST.getRemark());
            windyBefore.setLatestMsg("almost");
            windyBefore.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyBefore.getId(), windyBefore);
            //
            Windy windyTmp = windyCacheService.lockGetOrDefault(cryptoContext.getTmpPath());
            windyTmp.setCode(WindyStatusEnum.ALMOST.getCode());
            windyTmp.setLabel(WindyStatusEnum.ALMOST.getLabel());
            windyTmp.setDesc(WindyStatusEnum.ALMOST.getRemark());
            windyTmp.setLatestMsg("almost");
            windyTmp.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyTmp.getId(), windyTmp);
            //
            Windy windyAfter = windyCacheService.lockGetOrDefault(cryptoContext.getAfterPath());
            windyAfter.setCode(WindyStatusEnum.ALMOST.getCode());
            windyAfter.setLabel(WindyStatusEnum.ALMOST.getLabel());
            windyAfter.setDesc(WindyStatusEnum.ALMOST.getRemark());
            windyAfter.setLatestMsg("almost");
            windyAfter.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyAfter.getId(), windyAfter);
            //
        };
        // 失败回调
        Consumer<Void> errorCs = aVoid -> {
            // 临时文件改名失败,抛异常
            throw new WindyException(StrUtil.format("改名(超时)失败:[{}ms]", timer.intervalMs()));
        };
        // 执行
        PollUtils.poll(intervalMs, maxMs, actionCs, successCs, errorCs);
    }

    private void finalRegisterAfter(CryptoContext cryptoContext) {
        String afterName = null;
        String afterPath = null;
        Windy windy = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
        if (cryptoContext.getAskEncrypt()) {
            // 如果是加密,拼接加密后的文件名
            // 先判断是否需要加密原文件名
            boolean isRequireCoverName = cryptoContext.getBitSwitchList() != null
                    && cryptoContext.getBitSwitchList().get(0) != null
                    && cryptoContext.getBitSwitchList().get(0) == 1;
            /**
             * @see {@link CommonConstants#CFG_NAME}
             */
            if (isRequireCoverName) {
                String k = StrUtil.format("{}-{}", cryptoContext.getUserPasswordSha256Hex(), windy.getId());
                String v = AesUtils.getAes(cryptoContext.getUserPassword()).encryptHex(k);
                //
                // 如果本次要求加密文件名,则在同级目录下创建一个配置文件,记录原文件名的加密信息
                File cfg = FileUtil.file(FileUtil.getParent(cryptoContext.getTmpPath(), 1), CommonConstants.CFG_NAME);
                // 需要加锁确保创建和写入
                lockUpdateCfg(cfg, k, v);
                // 记录上下文
                cryptoContext.setCfgTxtPath(cfg.getAbsolutePath());
                //
                // 此时的加密文件名拼接,eg:$safeLockedV2$ 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92 $ 0019930b8e4466ef1157919ad97ddf64 $ 1000 $ 123123123123.txt
                String concatName = new NameConcatDTO(windy.getId(), windy.getExtName()).getConcatName();
                afterName = StrUtil.format("{}{}{}{}{}{}{}{}"
                        , CommonConstants.ENCRYPTED_PREFIX
                        , cryptoContext.getUserPasswordSha256Hex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getIntSaltStrEncryptHex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getBitSwitchList().stream().map(String::valueOf).collect(Collectors.joining())
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , concatName);
            } else {
                // 不要求加密源文件名,直接拼接加密后的文件名
                afterName = StrUtil.format("{}{}{}{}{}{}{}{}"
                        , CommonConstants.ENCRYPTED_PREFIX
                        , cryptoContext.getUserPasswordSha256Hex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getIntSaltStrEncryptHex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getBitSwitchList().stream().map(String::valueOf).collect(Collectors.joining())
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , windy.getName());
            }
        } else {
            // 如果是解密,从加密文件文件名中截取源文件名,考虑到多个加密文件可能同时解锁出同名文件的场景,需要加锁处理
            CoverNameDTO coverNameDTO = CoverNameDTO.analyse(FileUtil.getName(cryptoContext.getBeforePath()), cryptoContext.getUserPassword());
            // TODO
            // TODO
            // TODO
            // TODO
        }

        // 如果是WIN系统,对文件名长度有要求
        if (SystemUtil.getOsInfo().isWindows()) {
            String finalAfterName = afterName;
            WindyException.run((Void) -> Assert.isTrue(finalAfterName.length() < 255, WindyLang.msg("i18n_1828639187784568832")));
        }
        //
        // 基本不会发生重复(盐值和文件名ID保证),但是还是要做一次校验
        afterPath = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(cryptoContext.getTmpPath(), 1), afterName));
        String afterCacheId = windyCacheService.parseId(afterPath);
        Windy windyCache = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, afterCacheId);
        WindyException.run((Void) -> Assert.isNull(windyCache, WindyLang.msg("i18n_1828639187788763138")));
        //
        // 登记缓存
        Windy windyAfter = windyCacheService.lockGetOrDefault(afterPath);
        // 更新状态信息
        windyAfter.setCode(WindyStatusEnum.INPUTTING.getCode());
        windyAfter.setLabel(WindyStatusEnum.INPUTTING.getLabel());
        windyAfter.setDesc(WindyStatusEnum.INPUTTING.getRemark());
        windyAfter.setLatestMsg("inputting");
        windyAfter.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyAfter.getId(), windyAfter);
        //
        // 记录上下文
        cryptoContext.setAfterPath(afterPath);
        cryptoContext.setAfterCacheId(windyAfter.getId());
    }

    @SneakyThrows
    private void lockUpdateCfg(File cfg, String key, String val) {
        String id = DigestUtil.sha256Hex(FileUtil.getAbsolutePath(cfg));
        String lockKey = StrUtil.format("{}:{}", CacheConstants.CFG_UPDATE_LOCK, id);
        RLock lock = redissonClient.getLock(lockKey);
        // tryLock(最多等待锁的时间,获取锁成功后锁的过期时间,时间单位)
        if (lock.tryLock(10, 15, TimeUnit.SECONDS)) {
            // 当前线程加锁成功,执行业务操作
            try {
                if (!FileUtil.exist(cfg)) {
                    FileUtil.touch(cfg);
                }
                List<String> lines = FileUtil.readUtf8Lines(cfg);
                // 配置文件中不应该存在相同KEY
                boolean anyMatch = lines.stream().filter(r -> StrUtil.isNotBlank(r)).anyMatch(row -> row.startsWith(key));
                WindyException.run((Void) -> Assert.isFalse(anyMatch, WindyLang.msg("i18n_1828802439705399296")));
                // 增加一行
                lines.add(StrUtil.format("{}={}", key, val));
                // 写入文件
                FileUtil.writeUtf8Lines(lines, cfg);
            } finally {
                // 确保释放锁
                lock.unlock();
            }
        } else {
            // 如果当前线程获取锁失败,说明有其他线程正在处理相同的绝对路径,但是耗时过长
            log.debug("[{}]被长时间占用,无法更新", cfg.getAbsolutePath());
            throw new WindyException("获取锁失败");
        }

    }

    private void finalDel(CryptoContext cryptoContext) {
        // 源文件与临时文件都需要删除,注意顺序,安全起见最后才能删除源文件
        LinkedList<String> pathList = ListUtil.toLinkedList(cryptoContext.getTmpPath(), cryptoContext.getBeforePath());
        for (String path : pathList) {
            TimeInterval timer = DateUtil.timer();
            long intervalMs = 2_000L;
            long maxMs = 60_000L;
            Consumer<Void> actionCs = aVoid -> {
                FileUtil.del(path);
                if (FileUtil.exist(path)) throw new WindyException(StrUtil.format("本次删除失败,触发重试"));
            };
            Consumer<Void> successCs = aVoid -> {
                log.debug("[{}]-删除成功,耗时[{}]ms,被刪除:[{}]", cryptoContext.getBeforeCacheId(), timer.intervalMs(), windyCacheService.parseId(path));
                Windy windy = windyCacheService.lockGetOrDefault(path);
                windy.setCode(WindyStatusEnum.NOT_EXIST.getCode());
                windy.setLabel(WindyStatusEnum.NOT_EXIST.getLabel());
                windy.setDesc(WindyStatusEnum.NOT_EXIST.getRemark());
                windy.setLatestMsg("not exist");
                windy.setUpdateTime(DateUtil.now());
                redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
            };
            Consumer<Void> errorCs = aVoid -> {
                throw new WindyException(StrUtil.format("删除(超时)失败:[{}ms]", timer.intervalMs()));
            };
            PollUtils.poll(intervalMs, maxMs, actionCs, successCs, errorCs);
        }
        //
        //
        //  开放最终文件状态
        long size = FileUtil.size(FileUtil.file(cryptoContext.getAfterPath()));
        String sizeLabel = FileUtil.readableFileSize(size);
        Windy windy = windyCacheService.lockGetOrDefault(cryptoContext.getAfterPath());
        windy.setCode(WindyStatusEnum.FREE.getCode());
        windy.setLabel(WindyStatusEnum.FREE.getLabel());
        windy.setDesc(WindyStatusEnum.FREE.getRemark());
        windy.setLatestMsg("free");
        windy.setSize(size);
        windy.setSizeLabel(sizeLabel);
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        //
    }

    /**
     * 设计上,任何一个中间线程的任何一个步骤发生未知异常,都会导致文件回滚,确保原始文件安全
     */
    private Function<Throwable, Void> captureUnknownExceptions(CryptoContext cryptoContext) {
        return throwable -> {
            if (throwable == null) return null;
            try {
                log.debug("=================== 触发全局回滚 ===================");
                // 处理文件回滚等
                globalWindyRollback(cryptoContext, throwable);
            } catch (Exception e) {
                // 如果在全局异常处理时发生未知异常,那么就是极其严重的异常,需要打印日志,及时修复
                Console.error("======================================================================================");
                log.error(WindyLang.msg("i18n_1828691686704943113"), e);
                Console.error("======================================================================================");
                throw new RuntimeException(e);
            }
            return null;// 无后续线程
        };
    }

    private void globalWindyRollback(CryptoContext cryptoContext, Throwable throwable) {
        // 如果是自定义异常
        if (throwable instanceof WindyException) {
            log.debug("自定义异常", throwable);
        } else {
            log.error(StrUtil.format("[{}]-发生未知异常", Thread.currentThread().getName()), throwable);
        }
        if (cryptoContext == null) return;
        // 1.0 删除可能存在的临时文件以及最终文件
        List<String> pathList = ListUtil.toLinkedList(cryptoContext.getTmpPath(), cryptoContext.getAfterPath());
        for (String path : pathList) {
            if (StrUtil.isBlank(path) || !FileUtil.exist(path)) {
                continue;
            }
            // 同样需要考虑锁的问题
            long intervalMs = 2_000L;
            long maxMs = 60_000L;
            TimeInterval timer = DateUtil.timer();
            Consumer<Void> actionCs = aVoid -> {
                IoUtil.close(cryptoContext.getBis());
                IoUtil.close(cryptoContext.getBos());
                FileUtil.del(FileUtil.file(path));
                if (FileUtil.exist(path)) throw new WindyException(StrUtil.format("本次删除失败,触发重试"));
            };
            Consumer<Void> successCs = aVoid -> {
                // log.debug("删除成功,耗时[{}]ms", timer.intervalMs());
                log.debug("[{}]-删除成功,耗时[{}]ms,被刪除:[{}]", cryptoContext.getBeforeCacheId(), timer.intervalMs(), windyCacheService.parseId(path));
                Windy windy = windyCacheService.lockGetOrDefault(path);
                windy.setCode(WindyStatusEnum.NOT_EXIST.getCode());
                windy.setLabel(WindyStatusEnum.NOT_EXIST.getLabel());
                windy.setDesc(WindyStatusEnum.NOT_EXIST.getRemark());
                windy.setLatestMsg("not exist");
                windy.setUpdateTime(DateUtil.now());
                redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
                //
            };
            Consumer<Void> errorCs = aVoid -> {
                throw new WindyException(StrUtil.format("最终删除(超时)失败:[{}ms],需要手动删除", timer.intervalMs()));
            };
            // 执行
            PollUtils.poll(intervalMs, maxMs, actionCs, successCs, errorCs);
        }

        // 2.0 重新开放源文件
        Windy windy = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
        windy.setCode(WindyStatusEnum.FREE.getCode());
        windy.setLabel(WindyStatusEnum.FREE.getLabel());
        windy.setDesc(WindyStatusEnum.FREE.getRemark());
        windy.setLatestMsg("free");
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        //
    }
}
