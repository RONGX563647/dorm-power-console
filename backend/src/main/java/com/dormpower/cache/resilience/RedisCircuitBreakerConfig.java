package com.dormpower.cache.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis 熔断器配置
 *
 * 防止 Redis 故障导致级联故障，实现优雅降级
 *
 * 熔断状态：
 * - CLOSED: 正常状态，所有请求通过
 * - OPEN: 熔断状态，所有请求直接走降级逻辑
 * - HALF_OPEN: 半开状态，允许部分请求通过测试
 *
 * @author dormpower team
 * @version 1.1
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisCircuitBreakerConfig.class);

    /**
     * Redis 熔断器名称
     */
    public static final String REDIS_CIRCUIT_BREAKER = "redis";

    /**
     * 创建 Redis 熔断器配置
     */
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)                              // 失败率 50% 触发熔断
            .slowCallRateThreshold(80)                             // 慢调用率 80% 触发熔断
            .slowCallDurationThreshold(Duration.ofMillis(500))     // 慢调用阈值 500ms
            .waitDurationInOpenState(Duration.ofSeconds(30))       // 熔断状态持续 30 秒
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)                                 // 滑动窗口 10 次调用
            .minimumNumberOfCalls(5)                               // 最少 5 次调用才计算失败率
            .permittedNumberOfCallsInHalfOpenState(3)              // 半开状态允许 3 次测试调用
            .automaticTransitionFromOpenToHalfOpenEnabled(true)    // 自动从 OPEN 转到 HALF_OPEN
            .build();
    }

    /**
     * 创建熔断器注册表并注册指标
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig config,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // 注册指标到 Micrometer（避免循环依赖）
        if (meterRegistry != null) {
            TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(registry)
                .bindTo(meterRegistry);
            logger.info("CircuitBreaker metrics registered to Micrometer");
        }

        logger.info("CircuitBreaker registry initialized");
        return registry;
    }

    /**
     * 创建 Redis 熔断器
     */
    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreaker circuitBreaker = registry.circuitBreaker(REDIS_CIRCUIT_BREAKER);

        // 添加状态转换监听器
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                logger.warn("CircuitBreaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState());
            })
            .onError(event -> {
                logger.error("CircuitBreaker recorded error: {}", event.getThrowable().getMessage());
            })
            .onSuccess(event -> {
                logger.debug("CircuitBreaker recorded success, elapsed: {}ms",
                    event.getElapsedDuration().toMillis());
            });

        logger.info("Redis CircuitBreaker created with config: failureRateThreshold=50%, waitDuration=30s");
        return circuitBreaker;
    }
}