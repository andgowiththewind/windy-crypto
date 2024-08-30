package com.gust.cafe.windycrypto.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.ConvertException;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class WsMessageService {
    @Autowired
    @Qualifier(ThreadPoolConstants.STAT)
    private ThreadPoolTaskExecutor statTaskExecutor;
    @Autowired
    private StatService statService;


    public void onMessage(String sessionId, String message) {
        // 异步处理
        CompletableFuture
                .runAsync(getRunnable(sessionId, message), statTaskExecutor)
                .exceptionally(getThrowableVoidFunction(sessionId, message));
    }

    private Runnable getRunnable(String sessionId, String message) {
        return () -> {
            // log.debug("[WINDY CRYPTO WEBSOCKET]-收到消息:[ID={}],[消息={}]", sessionId, message);// 太频繁了,不打印
            // 业务处理
            // message是前端JSON.stringify后的字符串
            MsgPayloadDTO convert = null;
            try {
                convert = Convert.convert(MsgPayloadDTO.class, message);
            } catch (ConvertException e) {
                log.error("[WINDY CRYPTO WEBSOCKET]-消息格式错误:[ID={}],[消息={}]", sessionId, message, e);
            }
            if (convert != null && convert.getCode() != null) {
                if (convert.getCode() == 555) {
                    // 当前约定555获取两个table的缓存数据
                    List<String> ids = (List<String>) convert.getData();
                    statService.onMessageUpdateTableCache(sessionId, ids);
                } else if (convert.getCode() == 556) {
                    // 举例
                } else {
                    log.error("[WINDY CRYPTO WEBSOCKET]-消息类型错误(非法code):[ID={}],[消息={}]", sessionId, message);
                }
            }
        };
    }

    private Function<Throwable, Void> getThrowableVoidFunction(String sessionId, String message) {
        return e -> {
            log.error("[WINDY CRYPTO WEBSOCKET]-消息处理异常:[ID={}],[消息={}]", sessionId, message, e);
            return null;
        };
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MsgPayloadDTO {
        private Integer code;
        private Object data;
    }
}
