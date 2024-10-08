package com.gust.cafe.windycrypto.job;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.gust.cafe.windycrypto.components.RedisMasterCache;
import com.gust.cafe.windycrypto.constant.CacheConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
public class IoBytesBySecondJob {
    @Autowired
    private RedisMasterCache redisMasterCache;

    // 每隔2秒执行一次
    @Scheduled(cron = "0/2 * * * * ?")
    public void delLastMinuteIo() {
        // 删除1分钟前的缓存list
        Collection<String> keys = redisMasterCache.keys(StrUtil.format("{}_*", CacheConstants.LAST_MINUTE_IO));
        keys.stream().forEach(keyStr -> {
            String str = StrUtil.subAfter(keyStr, StrUtil.format("{}_", CacheConstants.LAST_MINUTE_IO), false);
            DateTime dateTime = DateUtil.parse(str, "yyyyMMddHHmmss");
            DateTime offsetSecond = DateUtil.offsetSecond(DateUtil.date(), -1 * 60);// 只记录最近N秒的数据
            // 如果是N分钟前则删除，避免积压
            if (dateTime.before(offsetSecond)) {
                redisMasterCache.listDelete(keyStr);
                log.debug("[IO BYTES BY SECOND JOB]-删除缓存:[key={}]", keyStr);
            }
        });
    }
}
