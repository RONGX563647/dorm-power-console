package com.dormpower.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置类
 *
 * 功能：
 * 1. 配置 Redis 缓存管理器
 * 2. 支持不同缓存区域的过期时间配置
 * 3. 使用 JSON 序列化，便于调试和跨语言使用
 * 4. 支持空值缓存防止缓存穿透
 *
 * 仅在 Redis 可用时启用
 *
 * @author dormpower team
 * @version 2.0
 */
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCacheConfig {

    /**
     * 默认缓存过期时间（30分钟）
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    /**
     * 空值缓存过期时间（30秒）- 防止缓存穿透
     */
    private static final Duration NULL_VALUE_TTL = Duration.ofSeconds(30);

    /**
     * 设备状态缓存过期时间（5分钟）
     */
    private static final Duration DEVICE_STATUS_TTL = Duration.ofMinutes(5);

    /**
     * 遥测数据缓存过期时间（1分钟）
     */
    private static final Duration TELEMETRY_TTL = Duration.ofMinutes(1);

    /**
     * 设备列表缓存过期时间（10分钟）
     */
    private static final Duration DEVICES_TTL = Duration.ofMinutes(10);

    /**
     * 系统配置缓存过期时间（1小时）- 变化极少
     */
    private static final Duration SYSTEM_CONFIG_TTL = Duration.ofHours(1);

    /**
     * 数据字典缓存过期时间（30分钟）
     */
    private static final Duration DATA_DICT_TTL = Duration.ofMinutes(30);

    /**
     * IP黑白名单缓存过期时间（5分钟）- 安全相关，需要较短的过期时间
     */
    private static final Duration IP_CONTROL_TTL = Duration.ofMinutes(5);

    /**
     * 电价规则缓存过期时间（30分钟）
     */
    private static final Duration PRICE_RULES_TTL = Duration.ofMinutes(30);

    /**
     * 消息模板缓存过期时间（1小时）- 变化极少
     */
    private static final Duration MESSAGE_TEMPLATES_TTL = Duration.ofHours(1);

    /**
     * 楼栋列表缓存过期时间（10分钟）
     */
    private static final Duration BUILDINGS_TTL = Duration.ofMinutes(10);

    /**
     * 告警配置缓存过期时间（10分钟）
     */
    private static final Duration ALERT_CONFIGS_TTL = Duration.ofMinutes(10);

    /**
     * 资源树缓存过期时间（30分钟）
     */
    private static final Duration RESOURCE_TREE_TTL = Duration.ofMinutes(30);

    /**
     * 用户权限缓存过期时间（15分钟）- 权限变更需及时生效
     */
    private static final Duration USER_PERMISSIONS_TTL = Duration.ofMinutes(15);

    /**
     * 用户角色缓存过期时间（15分钟）
     */
    private static final Duration USER_ROLES_TTL = Duration.ofMinutes(15);

    /**
     * 房间余额缓存过期时间（2分钟）- 余额变化需及时更新
     */
    private static final Duration ROOM_BALANCE_TTL = Duration.ofMinutes(2);

    /**
     * 设备在线状态缓存过期时间（1分钟）- 实时性要求高
     */
    private static final Duration DEVICE_ONLINE_TTL = Duration.ofMinutes(1);

    /**
     * 未读消息计数缓存过期时间（1分钟）
     */
    private static final Duration UNREAD_COUNT_TTL = Duration.ofMinutes(1);

    /**
     * 统计数据缓存过期时间（5分钟）- 统计数据可延迟
     */
    private static final Duration STATISTICS_TTL = Duration.ofMinutes(5);

    /**
     * AI报告缓存过期时间（30分钟）- 分析结果可缓存
     */
    private static final Duration AI_REPORT_TTL = Duration.ofMinutes(30);

    /**
     * 告警列表缓存过期时间（2分钟）
     */
    private static final Duration DEVICE_ALERTS_TTL = Duration.ofMinutes(2);

    /**
     * 未解决告警缓存过期时间（1分钟）
     */
    private static final Duration UNRESOLVED_ALERTS_TTL = Duration.ofMinutes(1);

    /**
     * 待缴费账单缓存过期时间（5分钟）
     */
    private static final Duration PENDING_BILLS_TTL = Duration.ofMinutes(5);

    /**
     * 用电统计缓存过期时间（10分钟）
     */
    private static final Duration ELECTRICITY_STATS_TTL = Duration.ofMinutes(10);

    /**
     * StringRedisTemplate Bean
     *
     * 用于 Redis 基础操作，如限流器、分布式锁等
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Redis 缓存管理器
     *
     * 配置不同缓存区域的过期时间和序列化方式
     * 启用空值缓存防止缓存穿透
     *
     * 注意：不使用 @Primary，由 MultiLevelCacheManager 作为主缓存管理器
     * 当 Redis 可用时，此 Bean 被 MultiLevelCacheManager 组合使用
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = createSecureObjectMapper();

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(DEFAULT_TTL)
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .prefixCacheNameWith("dorm:cache:");

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("deviceStatus", defaultConfig.entryTtl(DEVICE_STATUS_TTL));
        cacheConfigurations.put("telemetry", defaultConfig.entryTtl(TELEMETRY_TTL));
        cacheConfigurations.put("devices", defaultConfig.entryTtl(DEVICES_TTL));
        cacheConfigurations.put("systemConfig", defaultConfig.entryTtl(SYSTEM_CONFIG_TTL));
        cacheConfigurations.put("dataDict", defaultConfig.entryTtl(DATA_DICT_TTL));
        cacheConfigurations.put("ipWhitelist", defaultConfig.entryTtl(IP_CONTROL_TTL));
        cacheConfigurations.put("ipBlacklist", defaultConfig.entryTtl(IP_CONTROL_TTL));
        cacheConfigurations.put("priceRules", defaultConfig.entryTtl(PRICE_RULES_TTL));
        cacheConfigurations.put("messageTemplates", defaultConfig.entryTtl(MESSAGE_TEMPLATES_TTL));
        cacheConfigurations.put("buildings", defaultConfig.entryTtl(BUILDINGS_TTL));
        cacheConfigurations.put("alertConfigs", defaultConfig.entryTtl(ALERT_CONFIGS_TTL));
        cacheConfigurations.put("resourceTree", defaultConfig.entryTtl(RESOURCE_TREE_TTL));
        cacheConfigurations.put("userPermissions", defaultConfig.entryTtl(USER_PERMISSIONS_TTL));
        cacheConfigurations.put("userRoles", defaultConfig.entryTtl(USER_ROLES_TTL));
        cacheConfigurations.put("roomBalance", defaultConfig.entryTtl(ROOM_BALANCE_TTL));
        cacheConfigurations.put("deviceOnline", defaultConfig.entryTtl(DEVICE_ONLINE_TTL));
        cacheConfigurations.put("unreadCount", defaultConfig.entryTtl(UNREAD_COUNT_TTL));
        cacheConfigurations.put("studentStats", defaultConfig.entryTtl(STATISTICS_TTL));
        cacheConfigurations.put("roomStats", defaultConfig.entryTtl(STATISTICS_TTL));
        cacheConfigurations.put("loginStats", defaultConfig.entryTtl(STATISTICS_TTL));
        cacheConfigurations.put("auditStats", defaultConfig.entryTtl(STATISTICS_TTL));
        cacheConfigurations.put("aiReport", defaultConfig.entryTtl(AI_REPORT_TTL));
        cacheConfigurations.put("deviceAlerts", defaultConfig.entryTtl(DEVICE_ALERTS_TTL));
        cacheConfigurations.put("unresolvedAlerts", defaultConfig.entryTtl(UNRESOLVED_ALERTS_TTL));
        cacheConfigurations.put("pendingBills", defaultConfig.entryTtl(PENDING_BILLS_TTL));
        cacheConfigurations.put("electricityStats", defaultConfig.entryTtl(ELECTRICITY_STATS_TTL));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
    
    /**
     * 创建安全的 ObjectMapper
     *
     * 配置类型白名单，防止反序列化漏洞
     *
     * @return 配置好的 ObjectMapper
     */
    private ObjectMapper createSecureObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 使用安全的类型验证器，限制允许反序列化的类型
        // 仅允许项目中使用的包路径，防止任意类反序列化攻击
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator()
                .allowIfBaseType("com.dormpower.")      // 项目自身类
                .allowIfBaseType("java.util.")          // Java集合类
                .allowIfBaseType("java.lang.")          // Java基础类型
                .allowIfBaseType("java.time.")          // Java时间类型
                .allowIfBaseType("org.springframework."),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        return objectMapper;
    }

    /**
     * 空值缓存配置
     * 用于防止缓存穿透
     */
    @Bean
    public RedisCacheConfiguration nullValueCacheConfiguration() {
        ObjectMapper objectMapper = createSecureObjectMapper();
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(NULL_VALUE_TTL)
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .prefixCacheNameWith("dorm:cache:null:");
    }
}
