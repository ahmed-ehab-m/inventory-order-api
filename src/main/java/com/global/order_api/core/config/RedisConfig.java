package com.global.order_api.core.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    /// redisconnectionfactory=> responsible for low-level connection between app and redis server
    /// open connection channel using ip and port
    //// redisCacheManager=> responsible for understanding @cache annotations
    //// and cache policies
    //// pass factory to send data to redis after applying policies
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)
    {
        /////////////////////// 60 MINUTES //////////////
        /// 1=> default configuration for any caching
        /// instead of built configuration of redis from zero
        /// we use default configuration and ew only customize it
        RedisCacheConfiguration defaultConfig= RedisCacheConfiguration.defaultCacheConfig()
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

        /////////////////////// 5 MINUTES //////////////
        /// 2=> short ttl for pages
        RedisCacheConfiguration shortTtlConfig= RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        /////////////////////// 5 Seconds //////////////
        /// 2=> micro ttl for product
        RedisCacheConfiguration microTtlConfig= RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));


        /// 3=> ////////////////// Long TTL (1 Days) ////////////////////////
        RedisCacheConfiguration longTtlConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                /// LONG TTL 1 Day
                .withCacheConfiguration("categories", longTtlConfig)
                //// 60 Minutes
                .withCacheConfiguration("productsPage", shortTtlConfig)
                .withCacheConfiguration("ordersPage", shortTtlConfig)
                .withCacheConfiguration("usersPage", shortTtlConfig)
                /// 5 seconds
                .withCacheConfiguration("product", microTtlConfig)
                .build();
    }

}
