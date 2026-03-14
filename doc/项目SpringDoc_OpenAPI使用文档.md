# 项目 SpringDoc OpenAPI 使用文档

## 一、项目为什么使用 SpringDoc OpenAPI

### 1.1 技术选型背景

本项目是一个宿舍电源管理系统，包含大量 RESTful API 接口，涉及设备管理、遥测数据、告警管理、用户认证等多个业务模块。选择 SpringDoc OpenAPI 主要基于以下考虑：

#### 1.1.1 与 Spring Boot 3 的完美兼容

| 需求 | SpringFox (Swagger 2) | SpringDoc (OpenAPI 3) |
|------|----------------------|----------------------|
| Spring Boot 3.x 支持 | ❌ 不支持 | ✅ 完全支持 |
| Java 17/21 支持 | ❌ 兼容性问题 | ✅ 原生支持 |
| 维护状态 | ⚠️ 停止维护 | ✅ 活跃维护 |

项目使用 **Spring Boot 3.2.3 + Java 21**，SpringDoc 是唯一的选择。

#### 1.1.2 自动化文档生成

```
传统方式：
  编写代码 → 手动编写API文档 → 文档与代码不同步 → 维护困难

SpringDoc方式：
  编写代码 → 自动生成API文档 → 文档与代码同步 → 零维护成本
```

#### 1.1.3 提升开发效率

| 场景 | 传统方式 | SpringDoc 方式 |
|------|----------|----------------|
| 新增接口 | 编写代码 + 更新文档 | 只需编写代码 |
| 参数变更 | 修改代码 + 更新文档 | 自动同步 |
| 接口测试 | Postman 手动配置 | Swagger UI 一键测试 |
| 前后端联调 | 发送文档链接 | 直接访问 Swagger UI |

### 1.2 项目中的实际价值

#### 1.2.1 统一的 API 文档入口

项目通过 `OpenApiConfig` 配置类，统一管理所有 API 文档：

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Dorm Power API")
            .version("1.0.0")
            .description("宿舍电源管理系统API文档\n\n" +
                "## 功能模块\n" +
                "- **认证模块**: 用户登录、登出、令牌刷新\n" +
                "- **设备模块**: 设备CRUD操作、设备分组管理\n" +
                "- **遥测模块**: 功率数据查询、用电统计报表\n" +
                "- **命令模块**: 设备控制命令、批量命令\n" +
                "- **告警模块**: 异常告警、告警配置\n" +
                "- **AI报告**: 智能用电分析报告"))
        .servers(List.of(
            new Server().url("http://localhost:8000").description("本地开发服务器"),
            new Server().url("https://dorm.rongx.top").description("生产服务器")));
}
```

**价值**：
- 前端开发人员可以直接访问 `/swagger-ui.html` 查看所有接口
- 新成员快速了解系统 API 结构
- 减少沟通成本，避免"接口文档在哪里"的问题

#### 1.2.2 JWT 认证集成

项目使用 JWT 认证，SpringDoc 完美支持：

```java
.components(new Components()
    .addSecuritySchemes("bearerAuth",
        new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT认证令牌，格式: Bearer <token>")))
.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
```

**价值**：
- Swagger UI 支持"Authorize"按钮，一键填写 JWT Token
- 所有需要认证的接口自动携带 Token
- 方便测试需要认证的接口

#### 1.2.3 多环境服务器配置

```java
.servers(List.of(
    new Server().url("http://localhost:" + serverPort).description("本地开发服务器"),
    new Server().url("http://117.72.210.10").description("生产服务器"),
    new Server().url("https://dorm.rongx.top").description("生产服务器(HTTPS)")))
```

**价值**：
- 开发环境直接测试本地接口
- 生产环境问题排查时切换服务器
- 无需修改代码即可切换环境

---

## 二、项目中怎么使用 SpringDoc OpenAPI

### 2.1 依赖配置

在 [pom.xml](../backend/pom.xml) 中引入依赖：

```xml
<!-- Swagger/OpenAPI (轻量级) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2.2 配置类

项目配置类位于 [OpenApiConfig.java](../backend/src/main/java/com/dormpower/config/OpenApiConfig.java)：

```java
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8000}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("Dorm Power API")
                .version("1.0.0")
                .description("宿舍电源管理系统API文档...")
                .contact(new Contact()
                    .name("Dorm Power Team")
                    .email("admin@dorm.local")
                    .url("https://dorm.rongx.top"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server().url("http://localhost:" + serverPort).description("本地开发服务器"),
                new Server().url("https://dorm.rongx.top").description("生产服务器(HTTPS)")))
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
```

### 2.3 application.yml 配置

```yaml
# Springdoc OpenAPI配置
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
```

### 2.4 Controller 层使用示例

#### 2.4.1 类级别注解

```java
@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备管理", description = "设备查询和状态管理接口")
public class DeviceController {
    // ...
}
```

#### 2.4.2 方法级别注解

```java
@Operation(
    summary = "获取设备列表",
    description = "查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态",
    security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200", 
        description = "获取成功",
        content = @Content(schema = @Schema(implementation = Device.class))
    ),
    @ApiResponse(
        responseCode = "401", 
        description = "未授权，需要提供有效的Bearer Token"
    )
})
@GetMapping("/devices")
public List<Map<String, Object>> getDevices() {
    // ...
}
```

#### 2.4.3 参数级别注解

```java
@GetMapping("/devices/{deviceId}/status")
public ResponseEntity<?> getDeviceStatus(
    @Parameter(description = "设备ID", required = true, example = "device_001")
    @PathVariable String deviceId
) {
    // ...
}
```

#### 2.4.4 枚举参数约束

```java
@GetMapping("/telemetry")
public ResponseEntity<?> getTelemetry(
    @Parameter(description = "设备ID", required = true, example = "device_001")
    @RequestParam String device,
    
    @Parameter(description = "时间范围", required = true, example = "24h")
    @Schema(allowableValues = {"60s", "24h", "7d", "30d"})
    @RequestParam String range
) {
    // ...
}
```

### 2.5 DTO 层使用示例

```java
@Schema(description = "设备注册请求")
public class DeviceRegistrationRequest {

    @NotBlank(message = "设备ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,64}$", message = "设备ID只能包含字母、数字、下划线")
    @Schema(description = "设备ID", example = "device_001", required = true)
    private String id;

    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100字符")
    @Schema(description = "设备名称", example = "A1-301智能插座", required = true)
    private String name;

    @NotBlank(message = "房间号不能为空")
    @Pattern(regexp = "^[A-Za-z]\\d{1,2}-\\d{3,4}$", message = "房间格式错误")
    @Schema(description = "房间号", example = "A1-301", required = true)
    private String room;
}
```

### 2.6 Spring Security 配置

在 [SecurityConfig.java](../backend/src/main/java/com/dormpower/config/SecurityConfig.java) 中放行 OpenAPI 端点：

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
        ).permitAll()
        // ... 其他配置
    );
    return http.build();
}
```

### 2.7 访问方式

| 端点 | 说明 |
|------|------|
| `/swagger-ui.html` | Swagger UI 界面 |
| `/swagger-ui/index.html` | Swagger UI 界面（备用） |
| `/v3/api-docs` | OpenAPI JSON 文档 |
| `/v3/api-docs.yaml` | OpenAPI YAML 文档 |

---

## 三、面试要点

### 3.1 基础问题

#### Q1: 什么是 OpenAPI？和 Swagger 是什么关系？

**回答要点**：

1. **OpenAPI 规范**：原名 Swagger 规范，是一个描述 REST API 的标准格式（JSON/YAML）
2. **关系演变**：
   - 2015年：Swagger 规范捐赠给 Linux 基金会
   - 2017年：更名为 OpenAPI 规范
   - 当前：OpenAPI 3.x 是最新版本
3. **SpringDoc vs SpringFox**：
   - SpringFox：支持 Swagger 2.0，已停止维护
   - SpringDoc：支持 OpenAPI 3.x，活跃维护中

#### Q2: 为什么选择 SpringDoc 而不是 SpringFox？

**回答要点**：

| 维度 | SpringFox | SpringDoc |
|------|-----------|-----------|
| Spring Boot 3 | ❌ 不支持 | ✅ 完全支持 |
| Java 17+ | ⚠️ 兼容性问题 | ✅ 原生支持 |
| 维护状态 | 停止维护 | 活跃维护 |
| 性能 | 较慢 | 更快 |
| 配置复杂度 | 需要手动配置 | 自动配置 |

**项目实际情况**：我们使用 Spring Boot 3.2.3 + Java 21，SpringDoc 是唯一选择。

#### Q3: SpringDoc 的核心注解有哪些？

**回答要点**：

| 注解 | 位置 | 作用 |
|------|------|------|
| `@Tag` | 类 | API 分组描述 |
| `@Operation` | 方法 | 接口操作描述 |
| `@ApiResponses` | 方法 | 响应信息描述 |
| `@ApiResponse` | 方法 | 单个响应描述 |
| `@Parameter` | 参数 | 参数描述 |
| `@Schema` | 类/字段 | 数据模型描述 |
| `@SecurityRequirement` | 方法 | 安全认证要求 |

### 3.2 进阶问题

#### Q4: SpringDoc 是如何自动生成 API 文档的？

**回答要点**：

1. **自动配置**：通过 Spring Boot 的 `@EnableAutoConfiguration` 自动配置
2. **扫描机制**：扫描所有 `@RestController` 注解的类
3. **注解解析**：解析 `@RequestMapping`、`@Operation` 等注解
4. **模型构建**：根据返回值类型、参数类型构建 OpenAPI 模型
5. **端点暴露**：暴露 `/v3/api-docs` 和 `/swagger-ui.html` 端点

```
启动流程：
Spring 容器启动 → SpringDoc 自动配置 → 扫描 Controller → 
解析注解 → 构建 OpenAPI 模型 → 缓存 → 暴露端点
```

#### Q5: 如何在 SpringDoc 中集成 JWT 认证？

**回答要点**：

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        // 1. 定义安全方案
        .components(new Components()
            .addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
        // 2. 全局应用安全要求
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
}
```

**效果**：
- Swagger UI 显示"Authorize"按钮
- 输入 JWT Token 后，所有请求自动携带
- 方便测试需要认证的接口

#### Q6: 如何在生产环境禁用 Swagger UI？

**回答要点**：

**方式一：配置文件**

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

**方式二：Profile 区分**

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

# application-dev.yml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

**安全考虑**：
- 生产环境暴露 API 文档可能泄露系统结构
- 建议只在开发/测试环境启用
- 如果生产需要，配置访问控制

### 3.3 场景问题

#### Q7: 项目中如何处理枚举类型的参数文档？

**回答要点**：

```java
@GetMapping("/telemetry")
public ResponseEntity<?> getTelemetry(
    @Parameter(description = "时间范围", required = true, example = "24h")
    @Schema(allowableValues = {"60s", "24h", "7d", "30d"})
    @RequestParam String range
) {
    // ...
}
```

**效果**：
- Swagger UI 显示下拉选择框
- 限制用户只能选择预定义值
- 减少参数错误

#### Q8: 如何实现 API 分组？

**回答要点**：

```java
@Bean
public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("public")
        .pathsToMatch("/api/public/**")
        .build();
}

@Bean
public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
        .group("admin")
        .pathsToMatch("/api/admin/**")
        .build();
}
```

**适用场景**：
- 大型项目 API 数量多
- 按模块/权限分组
- 便于不同角色查看

#### Q9: 如何隐藏某些内部接口？

**回答要点**：

```java
@Operation(hidden = true)
@GetMapping("/internal/health")
public void internalHealth() {
    // 内部健康检查，不对外暴露
}
```

**适用场景**：
- 内部接口
- 废弃但未删除的接口
- 敏感接口

### 3.4 架构设计问题

#### Q10: SpringDoc 与 Spring Security 如何配合？

**回答要点**：

1. **放行 OpenAPI 端点**：

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
        ).permitAll()
        .anyRequest().authenticated()
    );
    return http.build();
}
```

2. **认证接口支持**：
   - 配置 JWT 安全方案
   - Swagger UI 支持 Token 输入
   - 自动携带认证信息

3. **安全考虑**：
   - 生产环境考虑禁用
   - 或配置特定角色访问

#### Q11: SpringDoc 的性能如何？有什么优化建议？

**回答要点**：

**性能特点**：
1. **缓存机制**：生成的 OpenAPI 文档会被缓存
2. **懒加载**：Swagger UI 资源懒加载
3. **扫描开销**：首次访问时扫描 Controller

**优化建议**：
1. 生产环境禁用
2. 使用 `@Operation(hidden = true)` 隐藏不需要文档的接口
3. 合理使用 `@Schema` 描述，避免过度复杂

#### Q12: 如何让 API 文档更专业？

**回答要点**：

1. **完整的描述**：
   - 每个接口都有 `summary` 和 `description`
   - 参数都有 `example` 示例值
   - 响应都有完整的 `@ApiResponses`

2. **统一的配置**：
   - 统一的 API 标题、版本、描述
   - 统一的服务器配置
   - 统一的安全认证配置

3. **规范的 DTO 文档**：
   - 使用 `@Schema` 描述每个字段
   - 标注必填项、格式约束
   - 提供示例值

4. **项目实践示例**：

```java
@Operation(
    summary = "获取设备状态",
    description = "获取指定设备的详细状态信息，包括总功率、电压、电流和各插座状态",
    security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200", 
        description = "获取成功",
        content = @Content(schema = @Schema(implementation = StripStatus.class))
    ),
    @ApiResponse(
        responseCode = "404", 
        description = "设备不存在或设备状态不存在"
    ),
    @ApiResponse(
        responseCode = "401", 
        description = "未授权，需要提供有效的Bearer Token"
    )
})
```

---

## 四、总结

### 4.1 项目使用总结

| 方面 | 实现方式 |
|------|----------|
| **依赖** | `springdoc-openapi-starter-webmvc-ui:2.3.0` |
| **配置类** | `OpenApiConfig.java` 定义全局配置 |
| **Controller** | `@Tag` + `@Operation` + `@ApiResponses` |
| **DTO** | `@Schema` 描述数据模型 |
| **认证** | JWT Bearer Token 集成 |
| **访问** | `/swagger-ui.html` |

### 4.2 核心价值

1. **开发效率**：自动生成文档，减少维护成本
2. **前后端协作**：统一的 API 文档入口，减少沟通成本
3. **接口测试**：Swagger UI 一键测试，无需额外工具
4. **团队规范**：注解驱动，强制文档规范

### 4.3 最佳实践

1. 每个接口都有完整的 `@Operation` 描述
2. 参数都有 `example` 示例值
3. 响应都有完整的 `@ApiResponses`
4. DTO 使用 `@Schema` 描述字段
5. 生产环境考虑禁用或配置访问控制

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日
