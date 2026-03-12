package com.dormpower.util;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Token黑名单
 */
@Component
public class TokenBlacklist {

    private final Set<String> blacklist = new HashSet<>();

    /**
     * 添加token到黑名单
     * @param token JWT令牌
     */
    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    /**
     * 检查token是否在黑名单中
     * @param token JWT令牌
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

    /**
     * 从黑名单中移除token
     * @param token JWT令牌
     */
    public void removeFromBlacklist(String token) {
        blacklist.remove(token);
    }

}
