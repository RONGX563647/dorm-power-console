package com.dormpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS跨域配置
 */
/**
 * 跨域配置类
 * 实现WebMvcConfigurer接口，用于配置Spring MVC的跨域资源共享(CORS)规则
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置跨域映射规则
     * @param registry CORS注册对象，用于添加跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 配置允许跨域的路径，/**表示所有路径
            .allowedOriginPatterns("*")  // 配置允许跨域的源，*表示所有源
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")  // 配置允许的HTTP方法
            .allowedHeaders("*")  // 配置允许的请求头，*表示所有请求头
            .allowCredentials(true)  // 是否允许发送Cookie信息
            .maxAge(3600);  // 预检请求的有效期，单位为秒
    }

    /**
     * 配置跨域资源源
     * @return 返回CorsConfigurationSource对象，用于定义跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 创建CorsConfiguration对象，用于配置跨域规则
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));  // 设置允许跨域的源
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));  // 设置允许的HTTP方法
        configuration.setAllowedHeaders(Arrays.asList("*"));  // 设置允许的请求头
        configuration.setAllowCredentials(true);  // 设置是否允许发送Cookie信息
        configuration.setMaxAge(3600L);  // 设置预检请求的有效期

        // 创建URL基础的跨域配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 注册跨域配置，/**表示所有路径
        return source;  // 返回配置好的跨域资源源
    }
}   
