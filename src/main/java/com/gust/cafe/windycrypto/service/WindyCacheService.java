package com.gust.cafe.windycrypto.service;

import cn.hutool.core.io.FileUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import com.gust.cafe.windycrypto.dto.core.Windy;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 处理文件缓存
 *
 * @author Dororo
 * @date 2024-08-23 17:06
 */
@Service
public class WindyCacheService {
    private final RedisMasterCache redisMasterCache;
    private final RedissonClient redissonClient;

    public WindyCacheService(RedisMasterCache redisMasterCache, RedissonClient redissonClient) {
        this.redisMasterCache = redisMasterCache;
        this.redissonClient = redissonClient;
    }

    // 通过绝对路径获取Windy对象
    public Windy lockGetOrDefault(String absPath) {
        // 定义业务操作
        Supplier<Windy> supplier = getSupplier(absPath);
        // 提交业务操作到分布式锁代码结构
        Windy windy = lockExecute(absPath, supplier);
        return windy;
    }

    private Windy lockExecute(String absPath, Supplier<Windy> supplier) {
        return null;
    }

    private Supplier<Windy> getSupplier(String absPath) {
        // Supplier主要内容:如果缓存中有,则返回缓存中的Windy对象,否则新建一个Windy对象返回
        return () -> {
            Windy cacheVo = redisMasterCache.getCacheMapValue(CacheConstants.WINDY_MAP, parseId(absPath));
            if (cacheVo != null) return cacheVo;
            // 如果缓存中不存在对应的文件信息,则新建并返回
            // Integer code=(FileUtil.exist(FileUtil.file(absPath)) ? 1 : 0);


            return null;
        };
    }

    public String parseId(String absPath) {
        return null;
    }

}
