package com.dormpower.cache.key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 缓存Key压缩器
 * 
 * 使用SHA-256压缩超长Key
 * 
 * 使用场景：
 * 1. Key长度超过阈值时自动压缩
 * 2. 减少Redis内存占用
 * 3. 降低网络传输开销
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
public class CacheKeyCompressor {

    private static final Logger logger = LoggerFactory.getLogger(CacheKeyCompressor.class);

    private static final int DEFAULT_KEY_LENGTH_THRESHOLD = 100;
    private static final String HASH_PREFIX = "hash:";

    @Value("${cache.key.compression.threshold:100}")
    private int keyLengthThreshold;

    @Value("${cache.key.compression.enabled:true}")
    private boolean compressionEnabled;

    /**
     * 压缩Key（如果需要）
     * 
     * @param key 原始Key
     * @return 压缩后的Key或原始Key
     */
    public String compress(String key) {
        if (!compressionEnabled || key == null) {
            return key;
        }

        if (key.length() <= keyLengthThreshold) {
            return key;
        }

        return HASH_PREFIX + sha256(key);
    }

    /**
     * 检查Key是否需要压缩
     */
    public boolean needsCompression(String key) {
        return compressionEnabled && key != null && key.length() > keyLengthThreshold;
    }

    /**
     * 计算SHA-256哈希
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return input;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 获取Key长度阈值
     */
    public int getKeyLengthThreshold() {
        return keyLengthThreshold;
    }

    /**
     * 是否启用压缩
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
}
