package com.dormpower.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security安全配置类
 * 
 * 该类负责配置整个应用的安全策略，包括认证、授权、跨域处理等核心安全功能。
 * 基于JWT令牌进行无状态认证，支持细粒度的权限控制和方法级安全。
 * 
 * 核心功能：
 * 1. CORS跨域配置：允许前端应用跨域访问API
 * 2. CSRF防护：禁用CSRF保护（JWT无状态应用不需要）
 * 3. 请求路径权限控制：基于URL路径的细粒度访问控制
 * 4. JWT认证过滤器：拦截请求并验证JWT令牌
 * 5. 方法级安全控制：支持@PreAuthorize等注解进行方法级权限控制
 * 
 * 权限分级：
 * 1. 公开路径(PERMIT_ALL)：无需任何认证，如登录、注册、健康检查
 * 2. 认证路径(AUTHENTICATED)：需要有效的JWT令牌，如设备管理、数据查询
 * 3. 管理员路径(ADMIN)：需要ADMIN角色，如RBAC管理、系统配置
 * 
 * 安全特性：
 * - 无状态认证：基于JWT令牌，不依赖Session
 * - 细粒度授权：支持URL路径和方法级双重权限控制
 * - 跨域支持：通过CORS配置允许指定来源的跨域请求
 * - 预检请求支持：自动处理OPTIONS预检请求
 * 
 * @author DormPower Team
 * @version 1.0
 * @since 2023-01-01
 */

/**
 * 安全配置类
 * 用于配置Spring Security的安全策略，包括认证、授权和CORS等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // JWT认证过滤器，用于验证请求头中的Token
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // CORS配置源，用于处理跨域请求
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    /**
     * 配置安全过滤器链

     * 定义了哪些URL需要认证，哪些URL可以公开访问，以及基于角色的访问控制
     * @param http HttpSecurity 配置好的安全过滤器链对象，用于构建安全过滤器链
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                // OPTIONS预检请求放行
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // 公开访问
                .requestMatchers(
                    "/health", 
                    "/actuator/**", 
                    "/api/auth/**",
                    "/api/agent/**",
                    "/api/simulator/**",
                    "/ws",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml"
                ).permitAll()
                // RBAC管理 - 仅管理员
                .requestMatchers("/api/rbac/**").hasRole("ADMIN")
                // 系统管理 - 仅管理员
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 用户管理 - 需要认证
                .requestMatchers("/api/users/**").authenticated()
                // 设备管理 - 需要认证
                .requestMatchers("/api/devices/**").authenticated()
                // 遥测数据 - 需要认证
                .requestMatchers("/api/telemetry/**").authenticated()
                // 命令控制 - 需要认证
                .requestMatchers("/api/commands/**").authenticated()
                .requestMatchers("/api/strips/**").authenticated()
                .requestMatchers("/api/cmd/**").authenticated()
                // 设备分组 - 需要认证
                .requestMatchers("/api/groups/**").authenticated()
                // 告警管理 - 需要认证
                .requestMatchers("/api/alerts/**").authenticated()
                // 定时任务 - 需要认证
                .requestMatchers("/api/tasks/**").authenticated()
                // 计费管理 - 需要认证
                .requestMatchers("/api/billing/**").authenticated()
                // 宿舍管理 - 需要认证
                .requestMatchers("/api/dorm/**").authenticated()
                // 学生管理 - 需要认证
                .requestMatchers("/api/students/**").authenticated()
                // AI报告 - 需要认证
                .requestMatchers("/api/rooms/**").authenticated()
                // AI客服Agent - 需要认证
                .requestMatchers("/api/agent/**").authenticated()
                // 通知系统 - 需要认证
                .requestMatchers("/api/notifications/**").authenticated()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
