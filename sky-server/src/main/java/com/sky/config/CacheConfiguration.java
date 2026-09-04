package com.sky.config;

import com.sky.json.JacksonObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache + Redis 的缓存配置
 *
 * 不配这个类时，Spring Boot 会用默认的 JDK 序列化把对象存进 Redis，
 * 结果是二进制（用工具看像乱码 \xAC\xED...），而且一旦实体类的字段变了，
 * 旧缓存反序列化会直接报错。
 *
 * 这里改成 JSON 存储：key 是字符串（可读），value 是 JSON（可读、跨语言通用）。
 * 注意：JSON 必须能处理 LocalDateTime 等时间类型，
 * 所以复用项目自带的 JacksonObjectMapper（已注册时间序列化器）。
 */
@Configuration
public class CacheConfiguration {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                // 兜底过期时间：管理端 @CacheEvict 是主动清缓存，这里是保底，
                // 防止将来漏清时脏数据一直存在。不需要可以删掉这行。
                .entryTtl(Duration.ofMinutes(60))
                // 方法返回 null 时不缓存，避免把 null 存进去
                .disableCachingNullValues()
                // key 用字符串序列化，Redis 里看到的是 dishCache::1 这种可读 key
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // value 用 JSON 序列化（带类型信息，反序列化才能还原成原对象）
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(new JacksonObjectMapper())));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}
