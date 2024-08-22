package com.gust.cafe.windycrypto.config.springcache;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 创建 Caffeine 实例为 "token"
        cacheManager.registerCustomCache("testToken", Caffeine.newBuilder()
                .maximumSize(100)// 最多缓存N个条目
                .expireAfterWrite(30 * 60, TimeUnit.SECONDS)// 写入后N秒过期
                .expireAfterAccess(30 * 60, TimeUnit.SECONDS)// 最后一次访问后N秒过期
                .build());
        // 创建 Caffeine 实例为 "tablePoolOptions"
        cacheManager.registerCustomCache("testTablePoolOptions", Caffeine.newBuilder()
                .maximumSize(100)// 最多缓存N个条目
                .expireAfterWrite(30 * 60, TimeUnit.SECONDS)// 写入后N秒过期
                .expireAfterAccess(30 * 60, TimeUnit.SECONDS)// 最后一次访问后N秒过期
                .build());
        return cacheManager;
    }
}