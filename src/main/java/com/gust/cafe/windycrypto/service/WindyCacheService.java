package com.gust.cafe.windycrypto.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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

    private Windy lockExecute(String absPath, Supplier<Windy> supplier) {
        return null;
    }


    /**
     * 根据绝对路径获取全局ID,缓存此对应关系
     *
     * @param absPath 绝对路径
     * @return 简化, 雪花算法生成的ID
     */
    public String parseId(String absPath) {
        Assert.notBlank(absPath);
        // 统一转正斜杠"/"
        absPath = FileUtil.file(absPath).getAbsolutePath();
        absPath = absPath.replace("\\", "/");
        // 先查缓存
        String id = redisMasterCache.getCacheMapValue(CacheConstants.PATH_ID_MAP, absPath);
        if (StrUtil.isNotBlank(id)) return id;
        // 如果缓存中没有,新生成并缓存
        // 使用雪花算法生成ID
        String nextIdStr = IdUtil.getSnowflakeNextIdStr();
        // 缓存
        redisMasterCache.setCacheMapValue(CacheConstants.PATH_ID_MAP, absPath, nextIdStr);
        return nextIdStr;
    }

}
