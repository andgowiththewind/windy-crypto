package com.gust.cafe.windycrypto.service;

import cn.hutool.core.util.StrUtil;
import com.esotericsoftware.minlog.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SysService {
    @Value("${server.port}")
    private Integer port;

    /**
     * 获取websocket前缀
     * {@link com.gust.cafe.windycrypto.websocket.WindyCryptoWebsocket} # ServerEndpoint.value
     */
    public String getWsUrlPrefix() {
        return StrUtil.format("ws://localhost:{}/windyCryptoWebsocket", port);
    }
}
