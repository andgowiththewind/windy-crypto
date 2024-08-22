package com.gust.cafe.windycrypto.config.redis;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Optional;

/**
 * REDIS配置类
 *
 * @author Dororo
 * @date 2024-01-11 01:56
 */
@Configuration
public class RedisConfig {

    private final RedisYmlProperties redisYmlProperties;

    public RedisConfig(RedisYmlProperties redisYmlProperties) {
        this.redisYmlProperties = redisYmlProperties;
    }

    @Bean
    @Primary
    public LettuceConnectionFactory masterConnectionFactory() {
        return createConnectionFactory(redisYmlProperties.getPropertiesMap().get(RedisDsEnum.MASTER.getValue()));
    }

    @Bean
    public LettuceConnectionFactory slaveConnectionFactory() {
        return createConnectionFactory(redisYmlProperties.getPropertiesMap().get(RedisDsEnum.SLAVE.getValue()));
    }

    @Bean
    @Primary
    public RedisTemplate<Object, Object> masterRedisTemplate() {
        return createRedisTemplate(masterConnectionFactory());
    }

    @Bean
    // @DependsOn("slaveConnectionFactory")
    public RedisTemplate<Object, Object> slaveRedisTemplate() {
        return createRedisTemplate(slaveConnectionFactory());
    }

    private RedisTemplate<Object, Object> createRedisTemplate(LettuceConnectionFactory connectionFactory) {
        // 模板
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(connectionFactory);
        // 采用Jackson序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        // 配置序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        //
        template.afterPropertiesSet();
        return template;
    }

    private LettuceConnectionFactory createConnectionFactory(RedisYmlProperties.PropertyDTO properties) {
        // (1) `RedisStandaloneConfiguration`
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getHost());
        redisConfig.setPort(properties.getPort());
        redisConfig.setDatabase(properties.getDatabase());
        Optional.ofNullable(properties.getPassword()).filter(StrUtil::isNotBlank).ifPresent(redisConfig::setPassword);

        // (2) `LettuceClientConfiguration`
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(properties.getTimeout()))
                .poolConfig(getPoolConfig(properties.getLettuce().getPool()))
                .build();

        // (1)+(2) =`LettuceConnectionFactory`
        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    private GenericObjectPoolConfig<?> getPoolConfig(RedisYmlProperties.PropertyDTO.Pool pool) {
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMinIdle(pool.getMinIdle());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxWaitMillis(pool.getMaxWait());
        return poolConfig;
    }
}