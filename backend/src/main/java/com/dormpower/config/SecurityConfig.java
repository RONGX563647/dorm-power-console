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
 * 安全配置类
 * 
 * 配置Spring Security的安全策略，包括：
 * - CORS跨域配置
 * - CSRF禁用
 * - 请求路径权限控制
 * - JWT认证过滤器
 * - 方法级安全控制
 * 
 * 权限说明：
 * - 公开路径：无需认证，如登录、注册、健康检查
 * - 认证路径：需要Bearer Token，如设备管理、命令控制
 * - 管理员路径：需要ADMIN角色，如RBAC、系统管理
 * 
 * @author dormpower team
 * @version 1.0
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
     * @param http HttpSecurity
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
