package com.dormpower.cache.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务
 *
 * 基于 Redis SET NX EX 命令实现分布式锁
 * 用于防止缓存击穿（热点 Key 过期时大量请求击穿到数据库）
 *
 * 特点：
 * 1. 原子性：SET NX EX 保证原子操作
 * 2. 防死锁：锁自动过期
 * 3. 防误删：Lua 脚本保证只删除自己持有的锁
 *
 * 使用场景：
 * 1. 缓存击穿防护
 * 2. 限流器
 * 3. 分布式任务调度
 *
 * @author dormpower team
 * @version 1.0
 */
@Component
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);

    /** 锁前缀 */
    private static final String LOCK_KEY_PREFIX = "lock:";

    /** 默认锁过期时间（秒） */
    private static final long DEFAULT_LOCK_EXPIRE_SECONDS = 30;

    /** 默认获取锁等待时间（毫秒） */
    private static final long DEFAULT_WAIT_TIMEOUT_MS = 5000;

    /** 默认重试间隔（毫秒） */
    private static final long DEFAULT_RETRY_INTERVAL_MS = 100;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 当前线程持有的锁标识 */
    private final ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

    /**
     * 尝试获取锁
     *
     * @param lockKey 锁键名
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_EXPIRE_SECONDS, DEFAULT_WAIT_TIMEOUT_MS);
    }

    /**
     * 尝试获取锁（带超时）
     *
     * @param lockKey 锁键名
     * @param expireSeconds 锁过期时间（秒）
     * @param waitTimeoutMs 等待超时时间（毫秒）
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, long expireSeconds, long waitTimeoutMs) {
        String redisKey = LOCK_KEY_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        while (true) {
            Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, lockValue, expireSeconds, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                lockValueHolder.set(lockValue);
                logger.debug("Lock acquired - key: {}, value: {}", lockKey, lockValue);
                return true;
            }

            // 检查是否超时
            if (System.currentTimeMillis() - startTime >= waitTimeoutMs) {
                logger.debug("Lock acquire timeout - key: {}", lockKey);
                return false;
            }

            // 等待重试
            try {
                TimeUnit.MILLISECONDS.sleep(DEFAULT_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Lock acquire interrupted - key: {}", lockKey);
                return false;
            }
        }
    }

    /**
     * 释放锁
     *
     * 使用 Lua 脚本保证原子性，只释放自己持有的锁
     *
     * @param lockKey 锁键名
     */
    public void unlock(String lockKey) {
        String redisKey = LOCK_KEY_PREFIX + lockKey;
        String lockValue = lockValueHolder.get();

        if (lockValue == null) {
            logger.warn("No lock to release - key: {}", lockKey);
            return;
        }

        // Lua 脚本：只有锁值匹配时才删除
        String luaScript =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

        // 使用 execute 方法执行 Lua 脚本，避免 deprecated API
        Long result = stringRedisTemplate.execute(
            (org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
                Object evalResult = connection.eval(
                    luaScript.getBytes(),
                    org.springframework.data.redis.connection.ReturnType.INTEGER,
                    1,
                    redisKey.getBytes(),
                    lockValue.getBytes()
                );
                return evalResult instanceof Long ? (Long) evalResult : Long.valueOf(0);
            }
        );

        if (result != null && result > 0) {
            logger.debug("Lock released successfully - key: {}", lockKey);
        } else {
            logger.warn("Lock release failed (not owner or expired) - key: {}", lockKey);
        }

        lockValueHolder.remove();
        logger.debug("Lock released - key: {}", lockKey);
    }

    /**
     * 检查锁是否存在
     *
     * @param lockKey 锁键名
     * @return 是否存在锁
     */
    public boolean isLocked(String lockKey) {
        String redisKey = LOCK_KEY_PREFIX + lockKey;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey));
    }

    /**
     * 在锁保护下执行任务
     *
     * @param lockKey 锁键名
     * @param task 要执行的任务
     * @param <T> 返回类型
     * @return 任务执行结果
     * @throws Exception 任务执行异常
     */
    public <T> T executeWithLock(String lockKey, LockTask<T> task) throws Exception {
        return executeWithLock(lockKey, DEFAULT_LOCK_EXPIRE_SECONDS, DEFAULT_WAIT_TIMEOUT_MS, task);
    }

    /**
     * 在锁保护下执行任务（带超时参数）
     *
     * @param lockKey 锁键名
     * @param expireSeconds 锁过期时间（秒）
     * @param waitTimeoutMs 等待超时时间（毫秒）
     * @param task 要执行的任务
     * @param <T> 返回类型
     * @return 任务执行结果
     * @throws Exception 任务执行异常
     */
    public <T> T executeWithLock(String lockKey, long expireSeconds, long waitTimeoutMs, LockTask<T> task) throws Exception {
        if (!tryLock(lockKey, expireSeconds, waitTimeoutMs)) {
            throw new LockAcquireException("Failed to acquire lock: " + lockKey);
        }

        try {
            return task.execute();
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 锁任务接口
     */
    @FunctionalInterface
    public interface LockTask<T> {
        T execute() throws Exception;
    }

    /**
     * 锁获取异常
     */
    public static class LockAcquireException extends RuntimeException {
        public LockAcquireException(String message) {
            super(message);
        }
    }
}