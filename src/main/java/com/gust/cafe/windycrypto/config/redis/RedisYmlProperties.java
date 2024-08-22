package com.gust.cafe.windycrypto.config.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * REDIS配置类
 * <p>将yml中配置的多个REDIS数据源属性映射到该类中,方便后续在其他Bean中引用</p>
 *
 * @author Dororo
 * @date 2023-11-23 16:26
 */
@Data
@Component
@ConfigurationProperties(prefix = "windy-dynamic-redis")
public class RedisYmlProperties {
    // 参考{"master":{"host":"127.0.0.1","port":6379,"database":0,"password":"dororosheep.cn","timeout":5000,"lettuce":{"pool":{"minIdle":0,"maxIdle":8,"maxActive":8,"maxWait":-1}}},"slave":{"host":"127.0.0.1","port":6380,"database":0,"password":"","timeout":5000,"lettuce":{"pool":{"minIdle":0,"maxIdle":8,"maxActive":8,"maxWait":-1}}}}
    private Map<String, PropertyDTO> propertiesMap;

    @Data
    public static class PropertyDTO {
        private String host;
        private Integer port;
        private Integer database;
        private String password;
        private Long timeout;
        private Lettuce lettuce;

        @Data
        public static class Lettuce {
            private Pool pool;
        }

        @Data
        public static class Pool {
            private Integer minIdle;
            private Integer maxIdle;
            private Integer maxActive;
            private Integer maxWait;
        }
    }
}


