package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.CoverNameDTO;
import com.gust.cafe.windycrypto.dto.CryptoContext;
import com.gust.cafe.windycrypto.dto.core.Windy;
import com.gust.cafe.windycrypto.enums.WindyStatusEnum;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

    //
    public void actionAsync(List<String> absPathList, CryptoSubmitReqVo reqVo) {
        TimeInterval timer = DateUtil.timer();
        for (String beforePath : absPathList) {
            // 上下文对象记录必要信息,异步线程采用thenRunAsync,确保按顺序执行
            CryptoContext cryptoContext = CryptoContext.builder()
                    .askEncrypt(reqVo.getAskEncrypt())
                    .userPassword(reqVo.getUserPassword())
                    .beforePath(beforePath)
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
        String beforePath = cryptoContext.getBeforePath();
        WindyException.run((Void) -> {
            Assert.isTrue(FileUtil.exist(beforePath), WindyLang.msg("i18n_1828354895514832896"));
            // 实时查询缓存
            Windy windy = windyCacheService.lockGetOrDefault(beforePath);
            // 状态要求FREE
            WindyStatusEnum anEnum = WindyStatusEnum.getByCode(windy.getCode());
            Assert.isTrue(anEnum != null && anEnum.equals(WindyStatusEnum.FREE), WindyLang.msg("i18n_1828354895519027200"));
        });

        // 满足条件则将状态更新为排队中,加锁处理
        windyCacheService.lockUpdate(beforePath, (path) -> {
            Windy windy = windyCacheService.lockGetOrDefault(beforePath);
            windy.setCode(WindyStatusEnum.WAITING.getCode());
            windy.setLabel(WindyStatusEnum.WAITING.getLabel());
            windy.setDesc(WindyStatusEnum.WAITING.getRemark());
            windy.setLatestMsg("queued");
            windy.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        });
    }

    // 异步加解密阶段
    private void futureCrypto(CryptoContext cryptoContext) {
        String beforePath = cryptoContext.getBeforePath();
        // 成功进入加解密阶段,更新状态,加锁处理
        windyCacheService.lockUpdate(beforePath, (path) -> {
            Windy windy = windyCacheService.lockGetOrDefault(beforePath);
            WindyStatusEnum anEnum = WindyStatusEnum.getByCode(windy.getCode());
            Assert.isTrue(anEnum != null && anEnum.equals(WindyStatusEnum.WAITING), "状态码应该为WAITING,请检查代码");
            windy.setCode(WindyStatusEnum.OUTPUTTING.getCode());
            windy.setLabel(WindyStatusEnum.OUTPUTTING.getLabel());
            windy.setDesc(WindyStatusEnum.OUTPUTTING.getRemark());
            windy.setLatestMsg("outputting");
            windy.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        });
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
        String beforePath = cryptoContext.getBeforePath();
        // 临时文件名
        String tmpName = StrUtil.format("0000000000-{}-{}.{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"), IdUtil.getSnowflakeNextIdStr(), CommonConstants.TMP_EXT_NAME);
        // 在源文件的同级目录下创建临时文件
        String tmpAbsPath = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(beforePath, 1), tmpName));
        Windy windy = windyCacheService.lockGetOrDefault(tmpAbsPath);
        // 更新缓存状态信息
        windy.setCode(WindyStatusEnum.INPUTTING.getCode());
        windy.setLabel(WindyStatusEnum.INPUTTING.getLabel());
        windy.setDesc(WindyStatusEnum.INPUTTING.getRemark());
        windy.setLatestMsg("inputting");
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        // 记录上下文
        cryptoContext.setTmpPath(tmpAbsPath);
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
        } else {
            // 如果是解密操作,则从文件名中解析盐值数组,此时需要校验密码是否正确
            CoverNameDTO coverNameDTO = CoverNameDTO.analyse(FileUtil.getName(cryptoContext.getBeforePath()), cryptoContext.getUserPassword());
            System.out.println("coverNameDTO=" + coverNameDTO);
        }

    }

    private void futureCryptoCoreIO(CryptoContext cryptoContext) {
        // TODO
        // TODO
        // TODO
        // TODO
        // TODO
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
                    // TODO 更新缓存状态信息
                    // TODO 更新缓存状态信息
                    // TODO 更新缓存状态信息
                    // 重置计时器,重新计时直至下一次周期
                    timer.restart();
                }
            }

            // 防止最后一次结果丢失,循环结束后指定百分比更新一次
            // TODO 直接更新100%

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
    }

    private Function<Throwable, Void> captureUnknownExceptions(CryptoContext cryptoContext) {
        return throwable -> {
            log.error("加解密异常,文件回滚,异常信息:{}", throwable.getMessage());
            return null;
        };
    }
}
