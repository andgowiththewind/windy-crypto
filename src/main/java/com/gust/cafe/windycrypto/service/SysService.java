package com.gust.cafe.windycrypto.service;

import com.esotericsoftware.minlog.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SysService {
    @Value("${server.port}")
    private Integer port;


    public String getWsUrlPrefix() {
        Log.debug("port: {}", port + "");
        return null;
    }
}
