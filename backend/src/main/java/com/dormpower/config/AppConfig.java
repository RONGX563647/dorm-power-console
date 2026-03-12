package com.dormpower.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 应用配置类
 */
@Configuration
public class AppConfig {

    /**
     * 创建RestTemplate实例

     * 该方法使用@Bean注解，将RestTemplate实例纳入Spring容器管理
     * @return RestTemplate实例，用于进行HTTP请求
     */
    @Bean
    public RestTemplate restTemplate() {
        // 创建并返回一个新的RestTemplate实例
        // RestTemplate是Spring提供的一个用于同步HTTP客户端的工具类
        return new RestTemplate();
    }

}