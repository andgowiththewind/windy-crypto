package com.gust.cafe.windycrypto.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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
            Assert.isTrue(anEnum != null && anEnum.equals(WindyStatusEnum.FREE), "状态码应该为FREE,请检查代码");
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

    }

    private void futureCryptoStream(CryptoContext cryptoContext) {

    }

    private void futureCryptoSalt(CryptoContext cryptoContext) {


    }

    private void futureCryptoCoreIO(CryptoContext cryptoContext) {

    }

    private void futureCryptoFinal(CryptoContext cryptoContext) {

    }

    private Function<Throwable, Void> captureUnknownExceptions(CryptoContext cryptoContext) {
        return throwable -> {
            log.error("加解密异常,文件回滚,异常信息:{}", throwable.getMessage());
            return null;
        };
    }
}
