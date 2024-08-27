package com.gust.cafe.windycrypto.service;

import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import com.gust.cafe.windycrypto.dto.CryptoContext;
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
    public void cryptoSubmitAsync(List<String> absPathList, CryptoSubmitReqVo reqVo) {
        for (String beforePath : absPathList) {
            // 上下文对象记录必要信息,异步线程采用thenRunAsync,确保按顺序执行
            CryptoContext cryptoContext = CryptoContext.builder()
                    .askEncrypt(reqVo.getAskEncrypt())
                    .userPassword(reqVo.getUserPassword())
                    .beforePath(beforePath)
                    .build();
            // (1)处理排队
            CompletableFuture<Void> future01 = CompletableFuture.runAsync(() -> futureQueue(cryptoContext), dispatchTaskExecutor);
            // (2)处理实际加解密
            CompletableFuture<Void> future02 = future01.thenRunAsync(() -> futureCrypto(cryptoContext), cryptoTaskExecutor);
            // (3)异常处理,处理文件回滚
            future02.exceptionally(captureUnknownExceptions(cryptoContext));
        }
    }

    // 异步排队阶段,再次实时查询状态判断是否满足条件
    private void futureQueue(CryptoContext cryptoContext) {
    }

    // 异步加解密阶段
    private void futureCrypto(CryptoContext cryptoContext) {
    }

    private Function<Throwable, Void> captureUnknownExceptions(CryptoContext cryptoContext) {
        return null;
    }
}
