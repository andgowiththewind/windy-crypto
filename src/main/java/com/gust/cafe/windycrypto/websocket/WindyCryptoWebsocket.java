package com.gust.cafe.windycrypto.websocket;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加解密页面交互WebSocket
 * <p>
 * 一个`WindyCryptoWebsocket`负责整个页面的多个更新;
 * 每个页面维护一个WebSocket连接,通过sessionId进行区分;
 * </p>
 *
 * @author dororo
 * @date 2024-08-30 09:12
 */
@Slf4j
@Component
@ServerEndpoint("/windyCryptoWebsocket/{sessionId}")// 前端对接路径参考`ws://localhost:8080/windyCryptoWebsocket/{sessionId}`
public class WindyCryptoWebsocket {
    private String sessionId;// 每个连接通过传入的sessionId进行区分
    private Session session;// 每个连接的session
    private static final Map<String, Session> SESSION_POOL = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        this.sessionId = sessionId;
        this.session = session;
        SESSION_POOL.put(sessionId, session);
        log.debug("[WINDY CRYPTO WEBSOCKET]-连接加入:[ID={}],[当前在线连接数={}]", sessionId, SESSION_POOL.size());
    }

    @OnClose
    public void onClose(Session session) {
        SESSION_POOL.remove(sessionId);
        log.debug("[WINDY CRYPTO WEBSOCKET]-连接关闭:[ID={}],[当前在线连接数={}]", sessionId, SESSION_POOL.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.debug("[WINDY CRYPTO WEBSOCKET]连接异常:[ID={}]", sessionId, error);
    }

    @OnMessage
    public void onMessage(String message) {
        log.debug("[WINDY CRYPTO WEBSOCKET]-收到前端消息:[ID={}],[消息={}]", sessionId, message);
        // 处理消息
    }

    // 发送消息:单点信息发送
    public static void sendMessage(String sessionId, String message) {
        Session session = SESSION_POOL.get(sessionId);
        if (session != null) {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.debug("[WINDY CRYPTO WEBSOCKET]-发送消息异常:[ID={}],[消息={}]", sessionId, message, e);
            }
        }
    }

    // 发送消息:多点信息发送
    public static void sendMessage(Set<String> sessionIdSet, String message) {
        sessionIdSet.forEach(sessionId -> sendMessage(sessionId, message));
    }

    // 广播消息
    public static void broadcast(String message) {
        if (CollectionUtil.isEmpty(SESSION_POOL) || SESSION_POOL.size() <= 0) return;
        SESSION_POOL.forEach((sessionId, session) -> {
            try {
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                log.debug("[WINDY CRYPTO WEBSOCKET]-广播消息异常:[ID={}],[消息={}]", sessionId, message, e);
            }
        });
    }
}
