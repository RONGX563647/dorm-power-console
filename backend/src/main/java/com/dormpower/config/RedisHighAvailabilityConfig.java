package com.dormpower.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis 高可用配置
 *
 * 支持三种部署模式：
 * 1. 单机模式（默认）
 * 2. 哨兵模式
 * 3. 集群模式
 *
 * 配置示例：
 * # 单机模式
 * spring.data.redis.host=localhost
 * spring.data.redis.port=6379
 *
 * # 哨兵模式
 * spring.data.redis.sentinel.master=mymaster
 * spring.data.redis.sentinel.nodes=sentinel1:26379,sentinel2:26379,sentinel3:26379
 *
 * # 集群模式
 * spring.data.redis.cluster.nodes=redis1:6379,redis2:6379,redis3:6379
 *
 * @author dormpower team
 * @version 1.0
 */
@Configuration
public class RedisHighAvailabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisHighAvailabilityConfig.class);

    @Value("${spring.data.redis.host:}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.data.redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${spring.data.redis.cluster.max-redirects:3}")
    private int maxRedirects;

    /**
     * 哨兵模式 Redis 连接工厂
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.sentinel.master")
    public RedisConnectionFactory redisSentinelConnectionFactory() {
        RedisSentinelConfiguration config = new RedisSentinelConfiguration()
            .master(sentinelMaster);

        // 解析哨兵节点
        List<RedisNode> nodes = parseNodes(sentinelNodes);
        for (RedisNode node : nodes) {
            config.sentinel(node.getHost(), node.getPort());
        }

        // 设置密码
        if (StringUtils.hasText(password)) {
            config.setPassword(password);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        logger.info("Redis Sentinel connection factory created - master: {}, sentinels: {}",
            sentinelMaster, sentinelNodes);

        return factory;
    }

    /**
     * 集群模式 Redis 连接工厂
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.nodes")
    public RedisConnectionFactory redisClusterConnectionFactory() {
        List<RedisNode> nodes = parseNodes(clusterNodes);

        RedisClusterConfiguration config = new RedisClusterConfiguration();
        for (RedisNode node : nodes) {
            config.addClusterNode(node);
        }
        config.setMaxRedirects(maxRedirects);

        // 设置密码
        if (StringUtils.hasText(password)) {
            config.setPassword(password);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        logger.info("Redis Cluster connection factory created - nodes: {}, maxRedirects: {}",
            clusterNodes, maxRedirects);

        return factory;
    }

    /**
     * 单机模式 Redis 连接工厂（仅在没有配置哨兵或集群时生效）
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisStandaloneConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);

        if (StringUtils.hasText(password)) {
            factory.setPassword(password);
        }

        logger.info("Redis Standalone connection factory created - host: {}, port: {}", host, port);
        return factory;
    }

    /**
     * StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * 解析节点列表
     *
     * @param nodes 节点字符串，格式：host1:port1,host2:port2
     * @return 节点列表
     */
    private List<RedisNode> parseNodes(String nodes) {
        return Arrays.stream(nodes.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(node -> {
                String[] parts = node.split(":");
                String nodeHost = parts[0].trim();
                int nodePort = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 6379;
                return new RedisNode(nodeHost, nodePort);
            })
            .collect(Collectors.toList());
    }
}