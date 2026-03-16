package com.dormpower.cache;

import com.dormpower.cache.bloom.RedisBloomFilter;
import com.dormpower.cache.consistency.CacheInvalidationBroadcaster;
import com.dormpower.cache.hotkey.HotKeyDetectorService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 多级缓存实现
 *
 * 实现本地缓存(L1) + 分布式缓存(L2)的多级缓存策略
 *
 * 读取流程：
 * 1. 先查L1本地缓存
 * 2. L1未命中，检查布隆过滤器（防止穿透）
 * 3. 布隆过滤器通过后，查L2分布式缓存
 * 4. L2命中，回填L1缓存
 *
 * 写入流程：
 * 1. 同时写入L1和L2缓存
 * 2. 添加key到布隆过滤器
 * 3. 保证两级缓存一致性
 *
 * 多节点一致性：
 * - evictLocal(): 仅清除本地L1缓存
 * - clearLocal(): 仅清空本地L1缓存
 *
 * 高级功能：
 * - 熔断保护：Redis故障时自动降级
 * - 热点检测：自动识别热点Key
 * - 缓存广播：多节点缓存一致性
 * - 布隆过滤器：防止缓存穿透
 *
 * @author dormpower team
 * @version 3.1
 */
public class MultiLevelCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCache.class);

    private final String name;
    private final Cache localCache;
    private final Cache remoteCache;

    // 可选的高级功能组件
    private CircuitBreaker circuitBreaker;
    private HotKeyDetectorService hotKeyDetectorService;
    private CacheInvalidationBroadcaster cacheInvalidationBroadcaster;
    private RedisBloomFilter redisBloomFilter;

    public MultiLevelCache(String name, Cache localCache, Cache remoteCache) {
        this.name = name;
        this.localCache = localCache;
        this.remoteCache = remoteCache;
    }

    /**
     * 设置熔断器（可选）
     */
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * 设置热点检测服务（可选）
     */
    public void setHotKeyDetectorService(HotKeyDetectorService hotKeyDetectorService) {
        this.hotKeyDetectorService = hotKeyDetectorService;
    }

    /**
     * 设置缓存广播服务（可选）
     */
    public void setCacheInvalidationBroadcaster(CacheInvalidationBroadcaster cacheInvalidationBroadcaster) {
        this.cacheInvalidationBroadcaster = cacheInvalidationBroadcaster;
    }

    /**
     * 设置布隆过滤器（可选）
     * 用于防止缓存穿透
     */
    public void setRedisBloomFilter(RedisBloomFilter redisBloomFilter) {
        this.redisBloomFilter = redisBloomFilter;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        long startTime = System.currentTimeMillis();
        String keyStr = String.valueOf(key);

        try {
            // 1. 先查L1本地缓存
            ValueWrapper value = localCache.get(key);
            if (value != null) {
                // 记录热点Key访问
                recordHotKeyAccess(keyStr);
                logger.debug("Cache HIT (L1) - cache: {}, key: {}, time: {}ms",
                    name, key, System.currentTimeMillis() - startTime);
                return value;
            }

            // 2. 布隆过滤器检查（防止缓存穿透）
            if (redisBloomFilter != null && !redisBloomFilter.mightContain(name, keyStr)) {
                // 布隆过滤器确定key不存在，直接返回null，避免查询数据库
                logger.debug("Cache PENETRATION BLOCKED by BloomFilter - cache: {}, key: {}", name, key);
                return null;
            }

            // 3. L1未命中，使用熔断保护访问L2
            value = getFromRemoteWithCircuitBreaker(key);

            if (value != null) {
                // L2命中，回填L1
                logger.debug("Cache HIT (L2) - cache: {}, key: {}, time: {}ms",
                    name, key, System.currentTimeMillis() - startTime);
                localCache.put(key, value.get());
                // 记录热点Key访问
                recordHotKeyAccess(keyStr);
                return value;
            }

            logger.debug("Cache MISS - cache: {}, key: {}, time: {}ms",
                name, key, System.currentTimeMillis() - startTime);
            return null;

        } catch (Exception e) {
            logger.error("Cache GET error - cache: {}, key: {}, error: {}",
                name, key, e.getMessage());
            return null;
        }
    }

    /**
     * 使用熔断器保护Redis访问
     */
    private ValueWrapper getFromRemoteWithCircuitBreaker(Object key) {
        if (circuitBreaker == null) {
            // 无熔断器，直接访问
            return remoteCache.get(key);
        }

        try {
            Supplier<ValueWrapper> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> remoteCache.get(key));
            return supplier.get();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, skip L2 cache for key: {}", key);
            return null; // 熔断状态，返回null让业务查数据库
        } catch (Exception e) {
            logger.error("L2 cache access failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 记录热点Key访问
     */
    private void recordHotKeyAccess(String key) {
        if (hotKeyDetectorService != null) {
            hotKeyDetectorService.recordAccess(name, key);
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            Object value = wrapper.get();
            if (value != null && type != null && !type.isInstance(value)) {
                throw new IllegalStateException(
                    "Cached value is not of required type [" + type.getName() + "]: " + value);
            }
            return (T) value;
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        long startTime = System.currentTimeMillis();

        try {
            // 写入L1本地缓存
            localCache.put(key, value);

            // 使用熔断保护写入L2
            putRemoteWithCircuitBreaker(key, value);

            // 添加到布隆过滤器（防止后续穿透）
            addToBloomFilter(String.valueOf(key));

            logger.debug("Cache PUT - cache: {}, key: {}, time: {}ms",
                name, key, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Cache PUT error - cache: {}, key: {}, error: {}",
                name, key, e.getMessage());
        }
    }

    /**
     * 添加key到布隆过滤器
     */
    private void addToBloomFilter(String key) {
        if (redisBloomFilter != null) {
            redisBloomFilter.put(name, key);
        }
    }

    /**
     * 使用熔断器保护Redis写入操作
     */
    private void putRemoteWithCircuitBreaker(Object key, Object value) {
        if (circuitBreaker == null) {
            remoteCache.put(key, value);
            return;
        }

        try {
            Runnable runnable = CircuitBreaker.decorateRunnable(
                circuitBreaker, () -> remoteCache.put(key, value));
            runnable.run();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, skip L2 put for key: {}", key);
        } catch (Exception e) {
            logger.error("L2 cache put failed: {}", e.getMessage());
        }
    }

    @Override
    public void evict(Object key) {
        long startTime = System.currentTimeMillis();

        try {
            // 清除本地L1缓存
            localCache.evict(key);

            // 使用熔断保护清除L2缓存
            evictRemoteWithCircuitBreaker(key);

            // 发布缓存失效广播（通知其他节点）
            publishInvalidation(String.valueOf(key));

            logger.debug("Cache EVICT - cache: {}, key: {}, time: {}ms",
                name, key, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Cache EVICT error - cache: {}, key: {}, error: {}",
                name, key, e.getMessage());
        }
    }

    /**
     * 使用熔断器保护Redis删除操作
     */
    private void evictRemoteWithCircuitBreaker(Object key) {
        if (circuitBreaker == null) {
            remoteCache.evict(key);
            return;
        }

        try {
            Runnable runnable = CircuitBreaker.decorateRunnable(
                circuitBreaker, () -> remoteCache.evict(key));
            runnable.run();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, skip L2 evict for key: {}", key);
        } catch (Exception e) {
            logger.error("L2 cache evict failed: {}", e.getMessage());
        }
    }

    /**
     * 发布缓存失效广播
     */
    private void publishInvalidation(String key) {
        if (cacheInvalidationBroadcaster != null) {
            cacheInvalidationBroadcaster.publishInvalidation(name, key);
        }
    }

    @Override
    public void clear() {
        long startTime = System.currentTimeMillis();

        try {
            localCache.clear();

            // 使用熔断保护清除L2
            clearRemoteWithCircuitBreaker();

            // 发布清除广播
            if (cacheInvalidationBroadcaster != null) {
                cacheInvalidationBroadcaster.publishClear(name);
            }

            logger.debug("Cache CLEAR - cache: {}, time: {}ms",
                name, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.error("Cache CLEAR error - cache: {}, error: {}",
                name, e.getMessage());
        }
    }

    /**
     * 使用熔断器保护Redis清除操作
     */
    private void clearRemoteWithCircuitBreaker() {
        if (circuitBreaker == null) {
            remoteCache.clear();
            return;
        }

        try {
            Runnable runnable = CircuitBreaker.decorateRunnable(
                circuitBreaker, () -> remoteCache.clear());
            runnable.run();
        } catch (CallNotPermittedException e) {
            logger.warn("CircuitBreaker is OPEN, skip L2 clear for cache: {}", name);
        } catch (Exception e) {
            logger.error("L2 cache clear failed: {}", e.getMessage());
        }
    }
    
    /**
     * 仅清除本地L1缓存
     * 用于多节点部署下接收其他节点的失效广播
     */
    public void evictLocal(Object key) {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.evict(key);
            
            logger.debug("Cache EVICT_LOCAL (L1 only) - cache: {}, key: {}, time: {}ms", 
                name, key, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache EVICT_LOCAL error - cache: {}, key: {}, error: {}", 
                name, key, e.getMessage());
        }
    }
    
    /**
     * 仅清空本地L1缓存
     */
    public void clearLocal() {
        long startTime = System.currentTimeMillis();
        
        try {
            localCache.clear();
            
            logger.debug("Cache CLEAR_LOCAL (L1 only) - cache: {}, time: {}ms", 
                name, System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logger.error("Cache CLEAR_LOCAL error - cache: {}, error: {}", 
                name, e.getMessage());
        }
    }
    
    /**
     * 获取本地缓存实例
     */
    public Cache getLocalCache() {
        return localCache;
    }
    
    /**
     * 获取远程缓存实例
     */
    public Cache getRemoteCache() {
        return remoteCache;
    }
}
