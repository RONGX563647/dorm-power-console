package com.dormpower.cache.bloom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

/**
 * Redis布隆过滤器
 * 
 * 用于防止缓存穿透，快速判断数据是否可能存在
 * 
 * 原理：
 * 1. 使用多个hash函数计算key的多个位置
 * 2. 将这些位置设置为1
 * 3. 查询时检查所有位置是否都为1
 * 4. 存在误判可能，但不会漏判
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
public class RedisBloomFilter {

    private static final Logger logger = LoggerFactory.getLogger(RedisBloomFilter.class);

    private static final String BLOOM_FILTER_KEY_PREFIX = "bloom:";
    
    private static final int DEFAULT_EXPECTED_INSERTIONS = 100000;
    private static final double DEFAULT_FPP = 0.01;
    private static final int DEFAULT_HASH_ITERATIONS = 7;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final int hashIterations;
    private final long bitSize;

    public RedisBloomFilter() {
        this(DEFAULT_EXPECTED_INSERTIONS, DEFAULT_FPP);
    }

    public RedisBloomFilter(int expectedInsertions, double fpp) {
        this.bitSize = optimalNumOfBits(expectedInsertions, fpp);
        this.hashIterations = optimalNumOfHashFunctions(expectedInsertions, bitSize);
        logger.info("BloomFilter initialized - bitSize: {}, hashIterations: {}", bitSize, hashIterations);
    }

    /**
     * 添加元素到布隆过滤器
     */
    public void put(String filterName, String key) {
        long[] indices = getBitIndices(key);
        String redisKey = BLOOM_FILTER_KEY_PREFIX + filterName;
        
        for (long index : indices) {
            stringRedisTemplate.opsForValue().setBit(redisKey, index, true);
        }
        
        logger.debug("Added key to bloom filter - filter: {}, key: {}", filterName, key);
    }

    /**
     * 批量添加元素
     */
    public void putAll(String filterName, Iterable<String> keys) {
        String redisKey = BLOOM_FILTER_KEY_PREFIX + filterName;
        
        for (String key : keys) {
            long[] indices = getBitIndices(key);
            for (long index : indices) {
                stringRedisTemplate.opsForValue().setBit(redisKey, index, true);
            }
        }
        
        logger.debug("Added batch keys to bloom filter - filter: {}", filterName);
    }

    /**
     * 判断元素是否可能存在
     * 
     * @return true: 可能存在（有误判可能）
     *         false: 一定不存在（不会漏判）
     */
    public boolean mightContain(String filterName, String key) {
        long[] indices = getBitIndices(key);
        String redisKey = BLOOM_FILTER_KEY_PREFIX + filterName;
        
        for (long index : indices) {
            Boolean bit = stringRedisTemplate.opsForValue().getBit(redisKey, index);
            if (bit == null || !bit) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 清空布隆过滤器
     */
    public void clear(String filterName) {
        String redisKey = BLOOM_FILTER_KEY_PREFIX + filterName;
        stringRedisTemplate.delete(redisKey);
        logger.info("Cleared bloom filter - filter: {}", filterName);
    }

    /**
     * 计算最优的bit数组大小
     */
    private long optimalNumOfBits(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /**
     * 计算最优的hash函数数量
     */
    private int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    /**
     * 计算key对应的bit位置
     */
    private long[] getBitIndices(String key) {
        long[] indices = new long[hashIterations];
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            
            byte[] md5Hash = md5.digest(bytes);
            byte[] sha256Hash = sha256.digest(bytes);
            
            long hash1 = bytesToLong(md5Hash);
            long hash2 = bytesToLong(sha256Hash);
            
            for (int i = 0; i < hashIterations; i++) {
                long combinedHash = hash1 + (long) i * hash2;
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                indices[i] = combinedHash % bitSize;
            }
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to get hash algorithm", e);
            for (int i = 0; i < hashIterations; i++) {
                indices[i] = Math.abs((key + i).hashCode()) % bitSize;
            }
        }
        
        return indices;
    }

    /**
     * 字节数组转long
     */
    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8 && i < bytes.length; i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
    }
}
