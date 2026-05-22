package com.global.order_api.core.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    /// instead of built configuration of redis from zero
    /// we use default configuration and ew only customize it
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                /// ttl => time to live , to save our memory from filling and if any changing in db
                .entryTtl(Duration.ofMinutes(60))
                /// not saving any null values , to free space in mem and avoid NUllpointerException
                .disableCachingNullValues()
                /// java save data in binary format in memory because this a fast way to jvm
                /// and to enable storing in cache we must convert it into 0s , 1s
                /// and here convert it or serialize it to JSON
                /// why => to if i wanna to read data from redis or any node js project or any framework
                /// wanna to read from cache
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair.fromSerializer(RedisSerializer.json()));
    }
}
