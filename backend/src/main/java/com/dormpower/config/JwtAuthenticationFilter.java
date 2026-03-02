package com.dormpower.config;

import com.dormpower.model.Permission;
import com.dormpower.model.Role;
import com.dormpower.service.RbacService;
import com.dormpower.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JWT认证过滤器
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RbacService rbacService;

    @Value("${security.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                
                // 获取用户的所有权限
                List<SimpleGrantedAuthority> authorities = getUserAuthorities(username, role);
                
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 设置认证信息到上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 令牌无效，继续过滤链
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * 获取用户的所有权限（角色 + 具体权限）
     */
    private List<SimpleGrantedAuthority> getUserAuthorities(String username, String defaultRole) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        try {
            // 从RBAC系统获取用户的角色
            List<Role> roles = rbacService.getUserRoles(username);
            
            if (roles.isEmpty() && defaultRole != null) {
                // 如果没有分配角色，使用默认角色
                authorities.add(new SimpleGrantedAuthority("ROLE_" + defaultRole.toUpperCase()));
            } else {
                // 添加角色和权限
                for (Role role : roles) {
                    if (role.isEnabled()) {
                        // 添加角色
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().toUpperCase()));
                        
                        // 添加角色的具体权限
                        Set<Permission> permissions = role.getPermissions();
                        if (permissions != null) {
                            for (Permission permission : permissions) {
                                if (permission.isEnabled()) {
                                    authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取权限失败，使用默认角色
            if (defaultRole != null) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + defaultRole.toUpperCase()));
            }
        }
        
        return authorities;
    }

}
