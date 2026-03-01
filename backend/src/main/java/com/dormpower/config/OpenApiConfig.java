package com.dormpower.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置OpenAPI
     * @return OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dorm Power API")
                        .version("1.0.0")
                        .description("宿舍电源管理系统API文档")
                        .contact(new Contact()
                                .name("Dorm Power Team")
                                .email("admin@dorm.local")));
    }

}
