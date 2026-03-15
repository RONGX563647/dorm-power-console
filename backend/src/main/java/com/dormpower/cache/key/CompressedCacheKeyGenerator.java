package com.dormpower.cache.key;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * 压缩缓存Key生成器
 * 
 * 生成缓存Key并自动压缩超长Key
 * 
 * 特点：
 * 1. 基于方法签名和参数生成Key
 * 2. 超长Key自动压缩
 * 3. 支持自定义前缀
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component("compressedKeyGenerator")
public class CompressedCacheKeyGenerator implements KeyGenerator {

    @Autowired
    private CacheKeyCompressor keyCompressor;

    @Override
    public Object generate(Object target, Method method, Object... params) {
        String rawKey = buildRawKey(target, method, params);
        return keyCompressor.compress(rawKey);
    }

    /**
     * 构建原始Key
     */
    private String buildRawKey(Object target, Method method, Object... params) {
        StringJoiner joiner = new StringJoiner(":");

        joiner.add(target.getClass().getSimpleName());
        joiner.add(method.getName());

        if (params != null && params.length > 0) {
            for (Object param : params) {
                joiner.add(paramToString(param));
            }
        }

        return joiner.toString();
    }

    /**
     * 参数转字符串
     */
    private String paramToString(Object param) {
        if (param == null) {
            return "null";
        }
        
        if (param.getClass().isArray()) {
            if (param instanceof Object[]) {
                return Arrays.toString((Object[]) param);
            } else if (param instanceof int[]) {
                return Arrays.toString((int[]) param);
            } else if (param instanceof long[]) {
                return Arrays.toString((long[]) param);
            } else if (param instanceof byte[]) {
                return Arrays.toString((byte[]) param);
            } else if (param instanceof char[]) {
                return Arrays.toString((char[]) param);
            } else if (param instanceof short[]) {
                return Arrays.toString((short[]) param);
            } else if (param instanceof float[]) {
                return Arrays.toString((float[]) param);
            } else if (param instanceof double[]) {
                return Arrays.toString((double[]) param);
            } else if (param instanceof boolean[]) {
                return Arrays.toString((boolean[]) param);
            }
        }
        
        return param.toString();
    }
}
