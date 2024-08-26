package com.gust.cafe.windycrypto.config.threadpool;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 请输入类描述
 *
 * @author Dororo
 * @date 2023-11-26 21:02
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "windy-thread-pool")
public class ThreadPoolProperties {
    private PropDTO dispatcher;
    private PropDTO crypto;
    private PropDTO stat;


    @Data
    public static class PropDTO {
        private Integer corePoolSize;
        private Integer maxPoolSize;
        private Integer keepAliveSeconds;
        private Integer queueCapacity;
    }
}
