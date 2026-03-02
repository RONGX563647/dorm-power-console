package com.dormpower.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 
 * 提供JWT令牌的生成、验证和黑名单管理功能。
 * 用于用户认证和授权。
 * 
 * 功能特性：
 * - 生成包含用户名和角色的JWT令牌
 * - 从令牌中提取用户名
 * - 验证令牌有效性
 * - 令牌黑名单管理（用于登出）
 * 
 * @author dormpower team
 * @version 1.0
 */
@Component
public class JwtUtil {

    // JWT签名密钥，从配置文件读取
    @Value("${security.jwt.secret}")
    private String secret;

    // JWT令牌有效期（毫秒），从配置文件读取
    @Value("${security.jwt.expiration}")
    private long expiration;

    // 令牌黑名单，用于存储已登出的令牌
    @Autowired
    private TokenBlacklist tokenBlacklist;

    /**
     * 生成JWT令牌
     * @param username 用户名
     * @param role 角色
     * @return JWT令牌
     */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成JWT令牌（默认角色）
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(String username) {
        return generateToken(username, "user");
    }

    /**
     * 从令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名，如果token无效则返回null
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证令牌
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            // 检查token是否在黑名单中
            if (tokenBlacklist.isBlacklisted(token)) {
                return false;
            }
            
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将token添加到黑名单
     * @param token JWT令牌
     */
    public void blacklistToken(String token) {
        tokenBlacklist.addToBlacklist(token);
    }

    /**
     * 获取签名密钥
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

}
