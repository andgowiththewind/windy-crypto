package com.gust.cafe.windycrypto.config.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * REDIS数据源类型
 *
 * <p>当前项目设计上,与`application-redis.yml`定义的配置一一对应;</p>
 *
 * @author Dororo
 * @date 2023-11-23 15:43
 */
@Getter
@AllArgsConstructor
public enum RedisDsEnum {
    MASTER("master"),
    SLAVE("slave"),
    ;
    /**
     * 数据源名称,需要与YML中配置的名称完全一致
     */
    private String value;
}
