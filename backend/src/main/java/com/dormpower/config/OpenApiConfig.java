package com.dormpower.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI配置类
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8000}")
    private int serverPort;

    /**
     * 配置OpenAPI
     * @return OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Dorm Power API")
                        .version("1.0.0")
                        .description("宿舍电源管理系统API文档\n\n" +
                                "## 功能模块\n" +
                                "- **认证模块**: 用户登录、登出、令牌刷新\n" +
                                "- **设备模块**: 设备列表、设备状态查询\n" +
                                "- **遥测模块**: 功率数据查询\n" +
                                "- **命令模块**: 设备控制命令\n" +
                                "- **AI报告**: 智能用电分析报告\n\n" +
                                "## 认证方式\n" +
                                "使用JWT Bearer Token认证，在请求头中添加: `Authorization: Bearer <token>`")
                        .contact(new Contact()
                                .name("Dorm Power Team")
                                .email("admin@dorm.local")
                                .url("https://dorm.rongx.top"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("本地开发服务器"),
                        new Server()
                                .url("http://117.72.210.10")
                                .description("生产服务器"),
                        new Server()
                                .url("https://dorm.rongx.top")
                                .description("生产服务器(HTTPS)")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT认证令牌，格式: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

}
