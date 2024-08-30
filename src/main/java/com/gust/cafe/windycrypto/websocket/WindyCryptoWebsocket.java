package com.gust.cafe.windycrypto.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
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

}
