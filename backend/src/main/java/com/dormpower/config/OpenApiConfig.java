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
                                "- **认证模块**: 用户登录、登出、令牌刷新、密码修改\n" +
                                "- **设备模块**: 设备CRUD操作、设备分组管理、设备状态历史\n" +
                                "- **遥测模块**: 功率数据查询、用电统计报表、数据导出\n" +
                                "- **命令模块**: 设备控制命令、批量命令、命令历史记录\n" +
                                "- **告警模块**: 异常告警、告警配置\n" +
                                "- **定时任务**: 定时开关设备、任务管理\n" +
                                "- **AI报告**: 智能用电分析报告\n\n" +
                                "## 认证方式\n" +
                                "使用JWT Bearer Token认证，在请求头中添加: `Authorization: Bearer <token>`\n\n" +
                                "## API端点\n" +
                                "- **认证**: /api/auth/**\n" +
                                "- **用户**: /api/users/**\n" +
                                "- **设备**: /api/devices/**\n" +
                                "- **分组**: /api/groups/**\n" +
                                "- **遥测**: /api/telemetry/**\n" +
                                "- **命令**: /api/commands/**\n" +
                                "- **告警**: /api/alerts/**\n" +
                                "- **任务**: /api/tasks/**\n" +
                                "- **AI报告**: /api/ai/**")
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
