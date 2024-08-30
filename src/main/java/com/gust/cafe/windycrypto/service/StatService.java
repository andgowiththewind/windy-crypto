package com.gust.cafe.windycrypto.service;

import com.gust.cafe.windycrypto.components.RedisSlaveCache;
import com.gust.cafe.windycrypto.constant.ThreadPoolConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class StatService {
    @Autowired
    private RedisSlaveCache redisSlaveCache;// 高频数据请求必须走从库
    @Autowired
    @Qualifier(ThreadPoolConstants.STAT)
    private ThreadPoolTaskExecutor statTaskExecutor;

    //
    public void onMessageUpdateTableCache(String sessionId, List<String> ids) {
        System.out.println();
    }
}
