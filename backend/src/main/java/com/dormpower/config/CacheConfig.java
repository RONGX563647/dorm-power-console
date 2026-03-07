package com.dormpower.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.Arrays;

/**
 * 缓存配置类
 * todo: 后续可以根据需要替换为更高级的缓存实现，如RedisCacheManager，以支持分布式缓存和更丰富的功能
 */
@Configuration // 标记此类为配置类，Spring容器会将其作为配置类处理
@EnableCaching // 启用Spring缓存功能，允许在应用中使用缓存注解
public class CacheConfig { // 缓存配置类，用于配置Spring缓存相关设置

    /**
     * 配置缓存管理器
     * @return 缓存管理器 Bean，用于管理应用中的缓存
     */
    @Bean // 将该方法返回的对象作为Spring容器中的Bean
    public CacheManager cacheManager() { // 定义缓存管理器方法
        // 创建并返回一个ConcurrentMapCacheManager实例，它使用ConcurrentMap作为存储后端
        // 同时指定了三个缓存名称：devices、deviceStatus和telemetry
        return new ConcurrentMapCacheManager(
            "devices",      // 设备信息缓存
            "deviceStatus", // 设备状态缓存
            "telemetry"     // 遥测数据缓存
        );
    }

}
