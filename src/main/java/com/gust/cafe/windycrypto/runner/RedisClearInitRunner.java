package com.gust.cafe.windycrypto.runner;

import cn.hutool.core.collection.ListUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 初始化系统-REDIS清理
 *
 * @author Dororo
 * @date 2024-08-26 14:34
 */
@Slf4j
@Component
public class RedisClearInitRunner implements ApplicationRunner {
    private final RedisMasterCache redisMasterCache;

    public RedisClearInitRunner(RedisMasterCache redisMasterCache) {
        this.redisMasterCache = redisMasterCache;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        action();
    }

    // 根据业务设计,一些KEY需要再系统启动之际清理
    private void action() {
        // (1)按照完全匹配进行删除
        deleteByExactMatch();
        // (2)按照前缀匹配进行删除
        deleteByPrefixMatch();
    }

    private void deleteByExactMatch() {
        List<String> exactList = ListUtil.toList(
                CacheConstants.WINDY_MAP
                , CacheConstants.PATH_ID_MAP
        );
        exactList.forEach(key -> {
            redisMasterCache.deleteObject(key);
            log.debug("根据[KEY={}]删除缓存,规则:完全匹配", key);
        });
    }

    private void deleteByPrefixMatch() {
        List<String> prefixList = ListUtil.toList(
                CacheConstants.CAFFEINE_ANTI_SHAKE_LOCK
                , CacheConstants.WINDY_GET_OR_DEFAULT_LOCK
                , CacheConstants.CFG_CRUD_LOCK
        );
        prefixList.forEach(prefix -> {
            Collection<String> keys = redisMasterCache.keys(prefix + "*");
            if (keys != null && keys.size() > 0) {
                keys.forEach(key -> {
                    redisMasterCache.deleteObject(key);
                    log.debug("根据[KEY={}]删除缓存,规则:前缀匹配", key);
                });
            }
        });
    }
}
