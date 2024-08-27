package com.gust.cafe.windycrypto.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.components.WindyLang;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.CryptoContext;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.CryptoSubmitReqVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        });
        // 涉及修改缓存状态的操作也要在加锁环境进行处理
    }

    // 异步加解密阶段
    private void futureCrypto(CryptoContext cryptoContext) {
    }

    private Function<Throwable, Void> captureUnknownExceptions(CryptoContext cryptoContext) {
        return throwable -> {
            log.error("加解密异常,文件回滚,异常信息:{}", throwable.getMessage());
            return null;
        };
    }
}
