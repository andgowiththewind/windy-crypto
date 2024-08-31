package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.CollectionUtil;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
    @Autowired
    private StatService statService;

    //
    public void actionAsync(List<String> absPathList, CryptoSubmitReqVo reqVo) {
        TimeInterval timer = DateUtil.timer();
        //
        // 记录前端传递的要求:加密时是否隐藏原文件名
        Integer val = (reqVo.getIsRequireCoverName() != null && reqVo.getIsRequireCoverName() == true) ? 1 : 0;
        //
        //
        for (String beforePath : absPathList) {
            // 上下文对象记录必要信息,异步线程采用thenRunAsync,确保按顺序执行
            CryptoContext cryptoContext = CryptoContext.builder()
                    .askEncrypt(reqVo.getAskEncrypt())
                    .userPassword(reqVo.getUserPassword())
                    .userPasswordSha256Hex(DigestUtil.sha256Hex(reqVo.getUserPassword()))
                    .beforePath(beforePath)
                    .beforeCacheId(windyCacheService.parseId(beforePath))
                    .bitSwitchList(ListUtil.toList(val, 0, 0, 0))
                    .ignoreMissingHiddenFilename(reqVo.getIgnoreMissingHiddenFilename())
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
        log.debug("[{}]-记录一次ID与绝对路径对照:[{}]", cryptoContext.getBeforeCacheId(), cryptoContext.getBeforePath());
        String beforePath = cryptoContext.getBeforePath();
        WindyException.run((Void) -> {
            Assert.isTrue(FileUtil.exist(beforePath), "[{}]-{}", cryptoContext.getBeforeCacheId(), WindyLang.msg("i18n_1828354895514832896"));// 文件已经不存在,当前任务终止
            Windy windy = windyCacheService.lockGetOrDefault(beforePath);// 实时查询缓存
            WindyStatusEnum anEnum = WindyStatusEnum.getByCode(windy.getCode());// 状态要求FREE
            Assert.isTrue(anEnum != null && anEnum.equals(WindyStatusEnum.FREE), "[{}]-{}", cryptoContext.getBeforeCacheId(), WindyLang.msg("i18n_1828354895519027200"));// 非空闲状态,已有其他任务在处理,当前任务终止
        });

        // 满足条件则将状态更新为排队中
        Windy windy = windyCacheService.lockGetOrDefault(beforePath);
        windy.setCode(WindyStatusEnum.WAITING.getCode());
        windy.setLabel(WindyStatusEnum.WAITING.getLabel());
        windy.setDesc(WindyStatusEnum.WAITING.getRemark());
        windy.setLatestMsg("i18n_1829605209761001472");// 正在排队等待分配线程...
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
        windy.setLatestMsg(cryptoContext.getAskEncrypt() ? "i18n_1829606118809284608" : "i18n_1829606263315632128");
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);

        // [FUTURE_CRYPTO]:处理临时文件,注册入缓存,记录上下文
        futureCryptoRegisterTmp(cryptoContext);

        // [FUTURE_CRYPTO]:处理盐值数组
        futureCryptoSalt(cryptoContext);

        // [FUTURE_CRYPTO]:处理最终文件信息,注册入缓存,记录上下文
        futureCryptoRegisterAfter(cryptoContext);

        // [FUTURE_CRYPTO]:处理加解密操作对应输入输出流
        futureCryptoStream(cryptoContext);

        // [FUTURE_CRYPTO]:处理核心IO操作
        futureCryptoCoreIO(cryptoContext);

        // [FUTURE_CRYPTO]:处理临时文件改名最终文件等相关操作
        futureCryptoFinal(cryptoContext);
    }

    private void futureCryptoRegisterTmp(CryptoContext cryptoContext) {
        log.debug("[{}]-处理临时文件注册", cryptoContext.getBeforeCacheId());
        String beforePath = cryptoContext.getBeforePath();
        // 临时文件名,双雪花,绝不重复
        String tmpName = StrUtil.format("0000000000-{}-{}.{}", IdUtil.getSnowflakeNextIdStr(), IdUtil.getSnowflakeNextIdStr(), CommonConstants.TMP_EXT_NAME);
        // 临时文件的位置位于:源文件的同级目录下
        String tmpAbsPath = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(beforePath, 1), tmpName));
        // 临时文件不需要记录缓存
        // 记录上下文
        cryptoContext.setTmpPath(tmpAbsPath);
        log.debug("[{}]-临时文件注册成功:[{}]", cryptoContext.getBeforeCacheId(), tmpName);
        log.debug("[{}]-记录一次临时文件ID与绝对路径对照:[{}]----[{}]", cryptoContext.getBeforeCacheId(), windyCacheService.parseId(tmpAbsPath), tmpAbsPath);
    }

    // 主要任务是确定最终文件的名称
    private void futureCryptoRegisterAfter(CryptoContext cryptoContext) {
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
                // 关于KEY的设计:`密码摘要算法值-雪花算法ID`,前者能定位到用哪个密码解密,后者除了在这里使用,在拼接文件名时也会使用,目的是解密时能在cfg中定位到信息行
                String keyPartAndAfterNamePart = IdUtil.getSnowflakeNextIdStr();
                String k = StrUtil.format("{}-{}", cryptoContext.getUserPasswordSha256Hex(), keyPartAndAfterNamePart);
                String v = AesUtils.getAes(cryptoContext.getUserPassword()).encryptHex(windy.getName());
                //
                // 如果本次要求加密文件名,则在同级目录下创建一个配置文件,记录原文件名的加密信息
                File cfg = FileUtil.file(FileUtil.getParent(cryptoContext.getTmpPath(), 1), CommonConstants.CFG_NAME);
                // 需要加锁确保创建和写入
                lockUpdateCfg(cfg, k, v);
                //
                // 此时的期望加密之后的文件名,拼接,eg:
                // 源文件名:password.txt
                // 如果不加密:$safeLockedV2$    bb7f5fe493c0fe6a1c54bafc181ccb820351c1a051ac6cff78c8a22a0fd9c708  $    0a7450f53a28da82d8c7497278d953cd   $   0000  $       1829102994264821760    $              password.txt
                // 如果加密:  $safeLockedV2$    bb7f5fe493c0fe6a1c54bafc181ccb820351c1a051ac6cff78c8a22a0fd9c708   $   0a7450f53a28da82d8c7497278d953cd   $   0000  $       1829102994264821760    $              1829136955162628096.txt
                // 这里采用的是加密源文件名记录在配置文件中,源文件名用一个雪花ID代替
                String concatName = new NameConcatDTO(
                        keyPartAndAfterNamePart,// 与记录在cfg中的雪花ID一样
                        windy.getExtName()
                ).getConcatName();// 同样需要考虑是否有扩展名的问题
                // 拼接加密后的文件名
                afterName = StrUtil.format("{}{}{}{}{}{}{}{}{}{}"
                        , CommonConstants.ENCRYPTED_PREFIX
                        , cryptoContext.getUserPasswordSha256Hex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getIntSaltStrEncryptHex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getBitSwitchList().stream().map(String::valueOf).collect(Collectors.joining())
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , IdUtil.getSnowflakeNextIdStr() // 随机码
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , concatName);
            } else {
                // 不要求加密源文件名,直接拼接期望加密后的文件名
                afterName = StrUtil.format("{}{}{}{}{}{}{}{}{}{}"
                        , CommonConstants.ENCRYPTED_PREFIX
                        , cryptoContext.getUserPasswordSha256Hex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getIntSaltStrEncryptHex()
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , cryptoContext.getBitSwitchList().stream().map(String::valueOf).collect(Collectors.joining())
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , IdUtil.getSnowflakeNextIdStr() // 随机码
                        , CommonConstants.ENCRYPTED_SEPARATOR
                        , windy.getName());
            }
        } else {
            // 如果是解密,从加密文件文件名中截取源文件名,考虑到多个加密文件可能同时解锁出同名文件的场景,需要加锁处理
            AtomicReference<String> expectSourceNameAtomic = new AtomicReference<>();
            // 分析本次解密的文件名
            CoverNameDTO coverNameDTO = CoverNameDTO.analyse(FileUtil.getName(cryptoContext.getBeforePath()), cryptoContext.getUserPassword());
            String sourceName = coverNameDTO.getSourceName();
            String sourceMainName = coverNameDTO.getSourceMainName();
            //
            List<Integer> bitSwitchList = coverNameDTO.getBitSwitchList();
            // 被解密的文件名上面记录了它原来的文件名是否加密,如果没有加密,则直接提取;如果加密了,则需要在同级目录下找`windycfg`文件
            boolean isCoverName = CollectionUtil.isNotEmpty(bitSwitchList) && bitSwitchList.size() > 0 && bitSwitchList.get(0) != null && bitSwitchList.get(0).intValue() == 1;
            if (!isCoverName) {
                // 如果文件名不是加密形式,则直接提取
                expectSourceNameAtomic.set(sourceName);
            } else {
                // 如果是加密了文件名,根据设计,在同级目录下找`windycfg`文件
                File windycfg = FileUtil.file(FileUtil.getParent(cryptoContext.getBeforePath(), 1), CommonConstants.CFG_NAME);
                WindyException.run((Void) -> {
                    // 特殊情况:源文件名确实加密了,但是如果用户觉得解密时源文件名不重要,那么可以直接跳过源文件名的解密
                    Boolean ignoreMissingHiddenFilename = cryptoContext.getIgnoreMissingHiddenFilename();
                    boolean differentialIgnoring = ignoreMissingHiddenFilename != null && ignoreMissingHiddenFilename == true;
                    if (!differentialIgnoring) {
                        // 如果没有明确表名忽略,则要求文件必须存在,且对应记录也必须存在
                        Assert.isTrue(FileUtil.exist(windycfg), WindyLang.msg("i18n_1828802439709593604"));
                        // 查询记录
                        String k = StrUtil.format("{}-{}", cryptoContext.getUserPasswordSha256Hex(), sourceMainName);// 加密时`sourceMainName`记录在cfg中所以解密时也要用
                        String lineStr = FileUtil.readUtf8Lines(windycfg).stream().filter(StrUtil::isNotBlank).filter(row -> StrUtil.startWith(row, k)).findFirst().orElseThrow(() -> new WindyException(WindyLang.msg("i18n_1828820333835194368")));
                        List<String> split = StrUtil.split(lineStr, "=");
                        Assert.isTrue(CollectionUtil.isNotEmpty(split) && split.size() == 2, WindyLang.msg("i18n_1828820333835194369"));
                        String coverName = split.get(1);
                        String decryptStr = AesUtils.getAes(cryptoContext.getUserPassword()).decryptStr(coverName);
                        expectSourceNameAtomic.set(decryptStr);
                    } else {
                        log.debug("[{}]-忽略源文件名解密", cryptoContext.getBeforeCacheId());
                        expectSourceNameAtomic.set(sourceName);
                    }
                });
            }
            //
            // 确认源文件名,拼接路径
            File fileAfter = FileUtil.file(FileUtil.getParent(cryptoContext.getBeforePath(), 1), expectSourceNameAtomic.get());
            boolean notExist = !FileUtil.exist(fileAfter);
            String parseIdAfter = windyCacheService.parseId(fileAfter.getAbsolutePath());
            Windy expectCache = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, parseIdAfter);
            boolean cacheNotEnabled = expectCache == null || (expectCache.getCode() != null && expectCache.getCode() == WindyStatusEnum.NOT_EXIST.getCode());
            // 场景:`$safeLockedV2$bb7f5fe493c0fe6a1c54bafc181ccb820351c1a051ac6cff78c8a22a0fd9c708$0a7450f53a28da82d8c7497278d953cd$0000$1829102994264821760$password.txt`正在解密,但是同目录下此时其他文件已经解密出一个`password.txt`文件或者直接创建了一个`password.txt`文件,此时需要区别
            // 确保:(1)当实际文件不存在(2)且缓存中不存在时,才能使用本次处理后的文件名
            if (notExist && cacheNotEnabled) {
                afterName = expectSourceNameAtomic.get();
            } else {
                // 当实际文件存在或者缓存中存在时,需要增加区别码
                // mainName增加标识
                String expectSourceName = expectSourceNameAtomic.get();
                File ghost = FileUtil.file(SystemUtil.getUserInfo().getHomeDir(), expectSourceName);
                String ghostMainName = FileUtil.mainName(ghost);
                String newGhostMainName = StrUtil.format("{}(repeated-{})", ghostMainName, IdUtil.getSnowflakeNextIdStr());// 雪花ID确保不会重复,一次随机码就能解决
                afterName = new NameConcatDTO(newGhostMainName, FileUtil.extName(ghost)).getConcatName();// 同样需要考虑是否有扩展名的问题
            }

        }

        // 如果是WIN系统,对文件名长度有要求
        if (SystemUtil.getOsInfo().isWindows()) {
            String finalAfterName = afterName;
            WindyException.run((Void) -> Assert.isTrue(finalAfterName.length() < 255, WindyLang.msg("i18n_1828639187784568832")));
        }
        //
        // 基本不会发生重复,但是还是要做一次校验
        afterPath = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(cryptoContext.getTmpPath(), 1), afterName));
        String afterCacheId = windyCacheService.parseId(afterPath);
        Windy windyCache = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, afterCacheId);
        boolean notEnabled = windyCache == null || (windyCache.getCode() != null && windyCache.getCode() == WindyStatusEnum.NOT_EXIST.getCode());
        WindyException.run((Void) -> Assert.isTrue(notEnabled, WindyLang.msg("i18n_1828639187788763138")));
        //
        // 登记缓存,占位
        Windy windyAfter = windyCacheService.lockGetOrDefault(afterPath);
        // 更新状态信息
        windyAfter.setCode(WindyStatusEnum.INPUTTING.getCode());
        windyAfter.setLabel(WindyStatusEnum.INPUTTING.getLabel());
        windyAfter.setDesc(WindyStatusEnum.INPUTTING.getRemark());
        windyAfter.setLatestMsg(cryptoContext.getAskEncrypt() ? "i18n_1829606906281570304" : "i18n_1829607020437884928");
        windyAfter.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyAfter.getId(), windyAfter);
        //
        // 记录上下文
        cryptoContext.setAfterPath(afterPath);
        cryptoContext.setAfterCacheId(windyAfter.getId());
    }

    private void futureCryptoStream(CryptoContext cryptoContext) {
        TimeInterval timer = DateUtil.timer();
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
        log.debug("[{}]-处理创建加解密操作对应输入输出流成功,耗时[{}]ms", cryptoContext.getBeforeCacheId(), timer.intervalMs());
    }

    private void futureCryptoSalt(CryptoContext cryptoContext) {
        if (cryptoContext.getAskEncrypt()) {
            // 如果是加密操作,则生成盐值数组,三个大于0小于256的随机数
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 3; i++) list.add(RandomUtil.randomInt(0, 256));
            cryptoContext.setIntSaltList(list);// 集合
            cryptoContext.setIntSaltStr(list.stream().map(String::valueOf).collect(Collectors.joining(StrUtil.COMMA)));// 字符串拼接
            cryptoContext.setIntSaltStrEncryptHex(AesUtils.getAes(cryptoContext.getUserPassword()).encryptHex(cryptoContext.getIntSaltStr()));// 字符串拼接加密
            log.debug("[{}]-本次请求加密,新生成盐值数组:[{}]", cryptoContext.getBeforeCacheId(), cryptoContext.getIntSaltStr());
        } else {
            // 如果是解密操作,则从文件名中解析盐值数组,此时需要校验密码是否正确
            CoverNameDTO coverNameDTO = CoverNameDTO.analyse(FileUtil.getName(cryptoContext.getBeforePath()), cryptoContext.getUserPassword());
            cryptoContext.setIntSaltList(coverNameDTO.getIntSaltList());
            cryptoContext.setIntSaltStr(coverNameDTO.getIntSaltList().stream().map(String::valueOf).collect(Collectors.joining(StrUtil.COMMA)));
            cryptoContext.setIntSaltStrEncryptHex(AesUtils.getAes(cryptoContext.getUserPassword()).encryptHex(cryptoContext.getIntSaltStr()));
            log.debug("[{}]-本次请求解密,解析盐值数组:[{}]", cryptoContext.getBeforeCacheId(), cryptoContext.getIntSaltStr());
        }

    }

    private void futureCryptoCoreIO(CryptoContext cryptoContext) {
        log.debug("[{}]-开始处理核心IO操作!", cryptoContext.getBeforeCacheId());
        try {
            // 读取源文件的缓存对象,一些数据已经记录,无需重复查询
            Windy windyBefore = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());

            // 每次读取的字节数
            Integer bufferSize = 1024;

            // 每次实际读取到字节数
            int len;

            // 已经读取的字节总数
            long total = 0;

            // 缓冲区
            byte[] buffer = new byte[bufferSize];

            // 计时器,记录全局耗时
            TimeInterval globalTimer = DateUtil.timer();

            // 源文件大小,用于计算百分比
            long windyBeforeSize = windyBefore.getSize();

            // 记录所有字节的位置,计算整数盐值时使用,加解密阶段,相同的位置对应相同的整数盐值
            long position = 0;

            // 整数盐值列表
            List<Integer> intSaltList = cryptoContext.getIntSaltList();

            // 本次操作是否为加密
            Boolean askEncrypt = cryptoContext.getAskEncrypt();

            // 计时器,控制打印频率,一定频率会重置
            TimeInterval frequencyTimer = DateUtil.timer();

            // 循环读取源文件的输入流,写入到临时文件的输出流
            while ((len = cryptoContext.getBis().read(buffer)) != -1) {
                // 此处已经将字节读取到缓冲区,不能在此缓冲区中直接修改,应该用一个新的字节数组来接收加盐后的字节,定义临时缓冲区副本
                byte[] newBuffer = new byte[len];
                // 对每个字节加减整数盐后重新收集:(1)加密时,增加整数盐;(2)解密时,减去整数盐
                for (int i = 0; i < len; i++) {
                    position = position + 1;
                    // 位置数值与整数盐值列表长度取余,得到当前位置对应的整数盐值
                    // 此步骤确保了所有的字节不会是同样的加盐值,即便破解者强行破解,每个字节都有-128到127共256种可能,即便是8个字节也有组合数为256^8,即便是最简单的暴力破解,也已经超过了`SHA-256`的安全性,唯一的不足之处是被加密的文件不能太小,两三个字节还是会被暴力破解;
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
                if (frequencyTimer.intervalMs() > 800L) {
                    // 计算当前百分比
                    Integer percentage = Convert.toInt(StrUtil.replaceLast(NumberUtil.formatPercent(NumberUtil.div(total, windyBeforeSize, 4), 0), "%", ""));
                    log.debug("[{}]-处理核心IO操作-当前百分比:[{}%]", cryptoContext.getBeforeCacheId(), percentage);
                    // TODO 更新缓存状态信息
                    Windy windy = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
                    windy.setLatestMsg("i18n_1829607775228129280");
                    windy.setPercentage(percentage);
                    windy.setPercentageLabel(StrUtil.format("{}%", percentage));
                    windy.setUpdateTime(DateUtil.now());
                    redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
                    //
                    // 开一个异步线程记录秒级别的本次处理的字节数,用于统计
                    statService.addSecondLevelBytes(NumberUtil.toStr(NumberUtil.sub(BigDecimal.valueOf(windyBeforeSize), BigDecimal.valueOf(total))));
                    //
                    // 重置计时器,重新计时直至下一次周期
                    frequencyTimer.restart();
                }
            }

            // 防止最后一次结果丢失,循环结束后指定百分比更新一次
            // TODO 直接更新100%
            Windy windy = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
            windy.setLatestMsg("i18n_1829607944266944512");
            windy.setPercentage(100);
            windy.setPercentageLabel("100%");
            windy.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
            log.debug("[{}]-当前百分比:[{}%],总耗时[{}]ms", cryptoContext.getBeforeCacheId(), 100, globalTimer.intervalMs());
            //
            statService.addSecondLevelBytes(NumberUtil.toStr(NumberUtil.sub(BigDecimal.valueOf(windyBeforeSize), BigDecimal.valueOf(total))));
            //
            // 清除计时器
            frequencyTimer.clear();
            frequencyTimer = null;
            globalTimer.clear();
            globalTimer = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IoUtil.close(cryptoContext.getBis());
            IoUtil.close(cryptoContext.getBos());
        }
    }

    private void futureCryptoFinal(CryptoContext cryptoContext) {
        // 处理临时文件改名
        finalTmpRenameToAfter(cryptoContext);
        // 处理临时文件删除、原始文件删除、最终文件开放状态等操作
        finalDel(cryptoContext);
        // 纯打印信息
        finalSuccess(cryptoContext);

    }

    private void finalSuccess(CryptoContext cryptoContext) {
        log.debug("[{}]-加解密流程正常结束。", cryptoContext.getBeforeCacheId());
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
                    FileUtil.getName(cryptoContext.getTmpPath()),
                    cryptoContext.getAfterPath()
            );
            // 三个文件都更新状态,改名成功说明临时文件已经不存在,最终文件生成成功
            Windy windyBefore = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
            windyBefore.setCode(WindyStatusEnum.ALMOST.getCode());
            windyBefore.setLabel(WindyStatusEnum.ALMOST.getLabel());
            windyBefore.setDesc(WindyStatusEnum.ALMOST.getRemark());
            windyBefore.setLatestMsg("i18n_1829608424183312384");
            windyBefore.setUpdateTime(DateUtil.now());
            redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyBefore.getId(), windyBefore);
            //
            // 临时文件无缓存
            //
            // 最终文件
            Windy windyAfter = windyCacheService.lockGetOrDefault(cryptoContext.getAfterPath());
            windyAfter.setCode(WindyStatusEnum.ALMOST.getCode());
            windyAfter.setLabel(WindyStatusEnum.ALMOST.getLabel());
            windyAfter.setDesc(WindyStatusEnum.ALMOST.getRemark());
            windyAfter.setLatestMsg("i18n_1829608424183312384");
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

    @SneakyThrows
    private void lockUpdateCfg(File cfg, String key, String val) {
        String id = DigestUtil.sha256Hex(FileUtil.getAbsolutePath(cfg));
        String lockKey = StrUtil.format("{}:{}", CacheConstants.CFG_CRUD_LOCK, id);
        RLock lock = redissonClient.getLock(lockKey);
        // tryLock(最多等待锁的时间,获取锁成功后锁的过期时间,时间单位)
        if (lock.tryLock(10, 15, TimeUnit.SECONDS)) {
            // 当前线程加锁成功,执行业务操作
            try {
                if (!FileUtil.exist(cfg)) {
                    FileUtil.touch(cfg);
                }
                List<String> lines = FileUtil.readUtf8Lines(cfg);
                // 配置文件如果存在相同的KEY,则不用重复录入
                boolean anyMatch = lines.stream().filter(r -> StrUtil.isNotBlank(r)).anyMatch(row -> row.startsWith(key));
                if (anyMatch) {
                    return;
                }
                // 如果不存在则增加一行
                lines.add(StrUtil.format("{}={}", key, val));
                // 更新后的内容重新写入文件
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
        // 先删除临时文件
        physicalDelete(cryptoContext, ListUtil.toLinkedList(cryptoContext.getTmpPath()));
        // 缓存更新为删除状态
        Windy windyTmp = windyCacheService.lockGetOrDefault(cryptoContext.getTmpPath());
        windyTmp.setCode(WindyStatusEnum.NOT_EXIST.getCode());
        windyTmp.setLabel(WindyStatusEnum.NOT_EXIST.getLabel());
        windyTmp.setDesc(WindyStatusEnum.NOT_EXIST.getRemark());
        windyTmp.setLatestMsg("not exist");
        windyTmp.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyTmp.getId(), windyTmp);
        //
        //
        // 如果本次是解密操作且并没有忽略"cfg缺失对应文件名加密信息"的情况,说明正常解密成功,此时cfg中记录的信息行可以被删除
        if (!cryptoContext.getAskEncrypt() && (cryptoContext.getIgnoreMissingHiddenFilename() == null || cryptoContext.getIgnoreMissingHiddenFilename() == false)) {
            String nameBefore = FileUtil.getName(cryptoContext.getBeforePath());
            CoverNameDTO coverNameDTO = CoverNameDTO.analyse(nameBefore, cryptoContext.getUserPassword());
            String sourceMainName = coverNameDTO.getSourceMainName();
            String cfgTxtPath = FileUtil.getAbsolutePath(FileUtil.file(FileUtil.getParent(cryptoContext.getTmpPath(), 1), CommonConstants.CFG_NAME));
            lockDeleteCfgLineByKey(cfgTxtPath, StrUtil.format("{}-{}", cryptoContext.getUserPasswordSha256Hex(), sourceMainName));
        }
        //
        //
        //  开放最终文件状态
        long size = FileUtil.size(FileUtil.file(cryptoContext.getAfterPath()));
        String sizeLabel = FileUtil.readableFileSize(size);
        Windy windyAfter = windyCacheService.lockGetOrDefault(cryptoContext.getAfterPath());
        windyAfter.setCode(WindyStatusEnum.FREE.getCode());
        windyAfter.setLabel(WindyStatusEnum.FREE.getLabel());
        windyAfter.setDesc(WindyStatusEnum.FREE.getRemark());
        windyAfter.setLatestMsg(cryptoContext.getAskEncrypt() ? "i18n_1829609313837142016" : "i18n_1829609121201246208");
        windyAfter.setSize(size);
        windyAfter.setSizeLabel(sizeLabel);
        windyAfter.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyAfter.getId(), windyAfter);
        //
        // 最后的最后才删除源文件
        physicalDelete(cryptoContext, ListUtil.toLinkedList(cryptoContext.getBeforePath()));
        // 物理文件删除后更新缓存
        Windy windyBefore = windyCacheService.lockGetOrDefault(cryptoContext.getBeforePath());
        windyBefore.setCode(WindyStatusEnum.NOT_EXIST.getCode());
        windyBefore.setLabel(WindyStatusEnum.NOT_EXIST.getLabel());
        windyBefore.setDesc(WindyStatusEnum.NOT_EXIST.getRemark());
        windyBefore.setLatestMsg(cryptoContext.getAskEncrypt() ? "i18n_1829609800674267136" : "i18n_1829609885185249280");
        windyBefore.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windyBefore.getId(), windyBefore);
    }

    private void physicalDelete(CryptoContext cryptoContext, LinkedList<String> pathList) {
        for (String path : pathList) {
            TimeInterval timer = DateUtil.timer();
            long intervalMs = 2_000L;
            long maxMs = 60_000L;
            Consumer<Void> actionCs = aVoid -> {
                FileUtil.del(path);
                if (FileUtil.exist(path)) throw new WindyException(StrUtil.format("本次删除失败,触发重试"));
            };
            Consumer<Void> successCs = aVoid -> {
                String parseId = windyCacheService.parseId(path);
                log.debug("[{}]-[收尾阶段]-删除成功,耗时[{}]ms,被刪除:[{}]", cryptoContext.getBeforeCacheId(), timer.intervalMs(), parseId);
            };
            Consumer<Void> errorCs = aVoid -> {
                throw new WindyException(StrUtil.format("[{}]-[收尾阶段]-删除(超时)失败:[{}ms]", cryptoContext.getBeforeCacheId(), timer.intervalMs()));
            };
            PollUtils.poll(intervalMs, maxMs, actionCs, successCs, errorCs);
        }
    }

    @SneakyThrows
    private void lockDeleteCfgLineByKey(String cfgTxtPath, String k) {
        TimeInterval timer = DateUtil.timer();
        String id = DigestUtil.sha256Hex(cfgTxtPath);
        String lockKey = StrUtil.format("{}:{}", CacheConstants.CFG_CRUD_LOCK, id);
        RLock lock = redissonClient.getLock(lockKey);
        // tryLock(最多等待锁的时间,获取锁成功后锁的过期时间,时间单位)
        if (lock.tryLock(10, 15, TimeUnit.SECONDS)) {
            // 当前线程加锁成功,执行业务操作
            try {
                if (!FileUtil.exist(cfgTxtPath)) {
                    return;
                }
                List<String> lines = FileUtil.readUtf8Lines(cfgTxtPath);
                // 排除空行,排除以k开头的行
                List<String> collect = lines.stream().filter(r -> StrUtil.isNotBlank(r)).filter(row -> !row.startsWith(k)).collect(Collectors.toList());
                // 写入文件
                FileUtil.writeUtf8Lines(collect, cfgTxtPath);
                log.debug("配置文件删除可能存在的信息行成功,耗时[{}]ms,[{}]", timer.intervalMs(), cfgTxtPath);
            } finally {
                // 确保释放锁
                lock.unlock();
            }
        } else {
            // 如果当前线程获取锁失败,说明有其他线程正在处理相同的绝对路径,但是耗时过长
            log.debug("[{}]被长时间占用,无法更新", cfgTxtPath);
            throw new WindyException("获取锁失败");
        }
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
                log.debug("[{}]-删除成功,耗时[{}]ms,被刪除:[{}]", cryptoContext.getBeforeCacheId(), timer.intervalMs(), windyCacheService.parseId(path));
                // 缓存更新为删除状态
                String parseId = windyCacheService.parseId(path);
                Windy windy = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, parseId);
                if (windy != null) {
                    windy.setCode(WindyStatusEnum.NOT_EXIST.getCode());
                    windy.setLabel(WindyStatusEnum.NOT_EXIST.getLabel());
                    windy.setDesc(WindyStatusEnum.NOT_EXIST.getRemark());
                    windy.setLatestMsg("i18n_1829610650868011008");// 发生异常触发当前文件被删除
                    windy.setUpdateTime(DateUtil.now());
                    redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
                }
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
        windy.setLatestMsg("i18n_1829610946314772480");
        windy.setUpdateTime(DateUtil.now());
        redisMasterCache.setCacheMapValue(CacheConstants.WINDY_MAP, windy.getId(), windy);
        //
    }
}
