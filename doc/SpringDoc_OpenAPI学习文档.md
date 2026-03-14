# SpringDoc OpenAPI 学习文档

## 一、基础功能

### 1.1 什么是 SpringDoc OpenAPI？

SpringDoc OpenAPI 是一个用于 Spring Boot 项目的 API 文档自动生成工具，它基于 OpenAPI 3.0 规范（原名 Swagger 规范），能够自动扫描 Spring MVC 控制器并生成 API 文档。

#### 核心概念

| 概念 | 说明 |
|------|------|
| **OpenAPI 规范** | 一个描述 REST API 的标准格式，定义了 API 的结构、参数、响应等信息 |
| **Swagger UI** | 一个可视化界面，用于展示和测试 API |
| **SpringDoc** | Spring Boot 集成 OpenAPI 的库，自动生成 OpenAPI 规范文档 |

#### 与 Swagger 2 的区别

| 特性 | SpringFox (Swagger 2) | SpringDoc (OpenAPI 3) |
|------|----------------------|----------------------|
| OpenAPI 版本 | 2.0 | 3.0 |
| 维护状态 | 停止维护 | 活跃维护 |
| Spring Boot 3 支持 | 不支持 | 完全支持 |
| 配置方式 | `@EnableSwagger2` | 自动配置 |
| 性能 | 较慢 | 更快 |

### 1.2 核心依赖

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

这个依赖包含了：
- `springdoc-openapi-starter-webmvc`：核心功能，生成 OpenAPI 文档
- `springdoc-openapi-ui`：Swagger UI 界面

### 1.3 常用注解详解

#### 1.3.1 类级别注解

##### @Tag

用于对 API 进行分组和描述，通常标注在 Controller 类上。

```java
@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备管理", description = "设备查询和状态管理接口")
public class DeviceController {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| name | String | 分组名称 |
| description | String | 分组描述 |
| externalDocs | ExternalDocumentation | 外部文档链接 |

#### 1.3.2 方法级别注解

##### @Operation

描述一个 API 操作（接口方法）。

```java
@Operation(
    summary = "获取设备列表",
    description = "查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态",
    security = @SecurityRequirement(name = "bearerAuth")
)
@GetMapping("/devices")
public List<Device> getDevices() {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| summary | String | 简短描述 |
| description | String | 详细描述 |
| tags | String[] | 分组标签 |
| operationId | String | 操作唯一标识 |
| security | SecurityRequirement[] | 安全要求 |
| deprecated | boolean | 是否废弃 |

##### @ApiResponses / @ApiResponse

描述 API 的响应信息。

```java
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "200", 
        description = "获取成功",
        content = @Content(schema = @Schema(implementation = Device.class))
    ),
    @ApiResponse(
        responseCode = "401", 
        description = "未授权，需要提供有效的Bearer Token"
    ),
    @ApiResponse(
        responseCode = "404", 
        description = "设备不存在"
    )
})
```

#### 1.3.3 参数级别注解

##### @Parameter

描述方法参数。

```java
@GetMapping("/devices/{deviceId}")
public Device getDevice(
    @Parameter(
        description = "设备ID", 
        required = true, 
        example = "device_001"
    )
    @PathVariable String deviceId
) {
    // ...
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| description | String | 参数描述 |
| required | boolean | 是否必需 |
| example | String | 示例值 |
| schema | Schema | 参数模式 |
| hidden | boolean | 是否隐藏 |

##### @Schema

描述数据模型，可用于 DTO 类或字段。

```java
@Schema(description = "设备注册请求")
public class DeviceRegistrationRequest {

    @Schema(
        description = "设备ID", 
        example = "device_001", 
        required = true,
        pattern = "^[a-zA-Z0-9_]{1,64}$"
    )
    private String id;

    @Schema(
        description = "设备名称", 
        example = "A1-301智能插座", 
        required = true,
        maxLength = 100
    )
    private String name;
}
```

#### 1.3.4 安全相关注解

##### @SecurityRequirement

声明接口需要的安全认证。

```java
@Operation(
    summary = "获取设备状态",
    security = @SecurityRequirement(name = "bearerAuth")
)
```

### 1.4 配置类详解

```java
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8000}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            // API 基本信息
            .info(new Info()
                .title("Dorm Power API")
                .version("1.0.0")
                .description("宿舍电源管理系统API文档")
                .contact(new Contact()
                    .name("Dorm Power Team")
                    .email("admin@dorm.local"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            
            // 服务器配置
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("本地开发服务器"),
                new Server()
                    .url("https://dorm.rongx.top")
                    .description("生产服务器")))
            
            // 安全认证配置
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT认证令牌")))
            
            // 全局安全要求
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
```

### 1.5 application.yml 配置

```yaml
springdoc:
  api-docs:
    enabled: true                    # 是否启用 API 文档
    path: /v3/api-docs               # OpenAPI JSON 文档路径
  swagger-ui:
    enabled: true                    # 是否启用 Swagger UI
    path: /swagger-ui.html           # Swagger UI 访问路径
    tags-sorter: alpha               # 标签排序方式（alpha=字母排序）
    operations-sorter: alpha         # 操作排序方式
    display-request-duration: true   # 显示请求耗时
    show-extensions: true            # 显示扩展信息
```

---

## 二、场景使用

### 2.1 场景一：基础 CRUD 接口文档

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {

    @Operation(summary = "获取用户列表", description = "获取所有用户的列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        // ...
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        // ...
    }

    @Operation(summary = "获取用户详情", description = "根据用户名获取用户详情")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/{username}")
    public ResponseEntity<User> getUser(
        @Parameter(description = "用户名", required = true) 
        @PathVariable String username
    ) {
        // ...
    }
}
```

### 2.2 场景二：带认证的接口文档

```java
@RestController
@RequestMapping("/api/devices")
@Tag(name = "设备管理", description = "设备查询和状态管理接口")
public class DeviceController {

    @Operation(
        summary = "获取设备列表",
        description = "查询所有注册的设备信息",
        security = @SecurityRequirement(name = "bearerAuth")  // 声明需要认证
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未授权，需要提供有效的Bearer Token")
    })
    @GetMapping
    public List<Device> getDevices() {
        // ...
    }
}
```

### 2.3 场景三：带参数校验的接口文档

```java
@Schema(description = "设备注册请求")
public class DeviceRegistrationRequest {

    @NotBlank(message = "设备ID不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{1,64}$", message = "设备ID格式错误")
    @Schema(
        description = "设备ID", 
        example = "device_001", 
        required = true,
        pattern = "^[a-zA-Z0-9_]{1,64}$",
        maxLength = 64
    )
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

### 2.4 场景四：枚举类型参数文档

```java
@GetMapping("/telemetry")
public ResponseEntity<?> getTelemetry(
    @Parameter(description = "设备ID", required = true, example = "device_001")
    @RequestParam String device,
    
    @Parameter(
        description = "时间范围：60s(1分钟)、24h(24小时)、7d(7天)、30d(30天)", 
        required = true, 
        example = "24h"
    )
    @Schema(allowableValues = {"60s", "24h", "7d", "30d"})
    @RequestParam String range
) {
    // ...
}
```

### 2.5 场景五：文件上传/下载接口文档

```java
@Operation(summary = "导出遥测数据", description = "导出指定设备的遥测数据")
@GetMapping("/telemetry/export")
public ResponseEntity<byte[]> exportTelemetry(
    @Parameter(description = "设备ID", required = true)
    @RequestParam String device,
    
    @Parameter(description = "导出格式", example = "csv")
    @RequestParam String format
) {
    byte[] data = telemetryService.exportTelemetry(device, format);
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=telemetry.csv")
        .header("Content-Type", "text/csv")
        .body(data);
}
```

### 2.6 场景六：分页查询接口文档

```java
@Operation(summary = "分页查询告警记录")
@GetMapping("/alerts")
public Page<Alert> getAlerts(
    @Parameter(description = "页码，从0开始", example = "0")
    @RequestParam(defaultValue = "0") int page,
    
    @Parameter(description = "每页大小", example = "20")
    @RequestParam(defaultValue = "20") int size,
    
    @Parameter(description = "排序字段", example = "createdAt")
    @RequestParam(defaultValue = "createdAt") String sort,
    
    @Parameter(description = "排序方向", example = "desc")
    @RequestParam(defaultValue = "desc") String direction
) {
    // ...
}
```

---

## 三、原理分析

### 3.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot 应用                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    Controller 层                      │   │
│  │  @RestController, @GetMapping, @PostMapping, ...     │   │
│  └────────────────────────┬────────────────────────────┘   │
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              SpringDoc 自动扫描机制                   │   │
│  │  OpenApiResource, ControllerAdviceInfo, ...          │   │
│  └────────────────────────┬────────────────────────────┘   │
│                           │                                 │
│                           ▼                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  OpenAPI 模型构建                     │   │
│  │  OpenAPI, Info, Paths, Components, ...               │   │
│  └────────────────────────┬────────────────────────────┘   │
│                           │                                 │
└───────────────────────────┼─────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      OpenAPI 文档输出                        │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │  JSON 文档          │    │  Swagger UI         │        │
│  │  /v3/api-docs       │    │  /swagger-ui.html   │        │
│  └─────────────────────┘    └─────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 核心组件

#### 3.2.1 OpenApiResource

负责处理 HTTP 请求，返回 OpenAPI JSON 文档。

```java
@RestController
public class OpenApiResource {
    
    @GetMapping("/v3/api-docs")
    public String openapiJson() {
        OpenAPI openAPI = openApiService.build();
        return objectMapper.writeValueAsString(openAPI);
    }
}
```

#### 3.2.2 OpenApiService

核心服务，负责扫描 Controller 并构建 OpenAPI 模型。

```java
@Service
public class OpenApiService {
    
    public OpenAPI build() {
        OpenAPI openAPI = new OpenAPI();
        
        // 1. 扫描所有 @RestController
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        
        // 2. 解析每个 Controller 的方法
        for (Object controller : controllers.values()) {
            parseController(controller, openAPI);
        }
        
        // 3. 应用全局配置
        applyGlobalConfig(openAPI);
        
        return openAPI;
    }
}
```

#### 3.2.3 ControllerAdviceInfo

解析 Controller 方法上的注解，提取 API 信息。

```java
public class ControllerAdviceInfo {
    
    public Operation parseOperation(Method method) {
        Operation operation = new Operation();
        
        // 解析 @Operation 注解
        io.swagger.v3.oas.annotations.Operation operationAnnotation = 
            method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
        
        if (operationAnnotation != null) {
            operation.setSummary(operationAnnotation.summary());
            operation.setDescription(operationAnnotation.description());
        }
        
        // 解析 @ApiResponses 注解
        // 解析 @Parameter 注解
        // ...
        
        return operation;
    }
}
```

### 3.3 自动配置原理

SpringDoc 利用 Spring Boot 的自动配置机制：

```java
@Configuration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnWebApplication
@EnableConfigurationProperties(SpringDocConfigProperties.class)
public class SpringDocAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI() {
        return new OpenAPI();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OpenApiService openApiService() {
        return new OpenApiService();
    }
    
    @Bean
    @ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true")
    public SwaggerUiHome swaggerUiHome() {
        return new SwaggerUiHome();
    }
}
```

### 3.4 注解处理流程

```
1. Spring 容器启动
   │
   ▼
2. SpringDoc 自动配置生效
   │
   ▼
3. 扫描所有 @RestController 类
   │
   ▼
4. 解析类级别注解
   ├─ @Tag → 提取分组信息
   └─ @SecurityRequirement → 提取安全要求
   │
   ▼
5. 解析方法级别注解
   ├─ @RequestMapping → 提取路径和方法
   ├─ @Operation → 提取操作描述
   ├─ @ApiResponses → 提取响应信息
   └─ @Parameter → 提取参数信息
   │
   ▼
6. 解析参数和返回值类型
   ├─ @RequestBody → 请求体模型
   ├─ @PathVariable → 路径参数
   ├─ @RequestParam → 查询参数
   └─ 返回值类型 → 响应模型
   │
   ▼
7. 构建 OpenAPI 模型
   │
   ▼
8. 缓存并暴露端点
   ├─ /v3/api-docs → JSON 文档
   └─ /swagger-ui.html → Swagger UI
```

### 3.5 与 Spring Security 集成

当项目使用 Spring Security 时，需要配置允许访问 OpenAPI 端点：

```java
@Configuration
public class SecurityConfig {
    
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
}
```

### 3.6 性能优化

#### 3.6.1 缓存机制

SpringDoc 会缓存生成的 OpenAPI 文档，避免每次请求都重新扫描：

```java
@Service
public class OpenApiService {
    
    private OpenAPI cachedOpenAPI;
    
    public OpenAPI getOpenAPI() {
        if (cachedOpenAPI == null) {
            cachedOpenAPI = build();
        }
        return cachedOpenAPI;
    }
}
```

#### 3.6.2 懒加载

Swagger UI 资源采用懒加载，只在首次访问时初始化。

#### 3.6.3 生产环境禁用

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

---

## 四、最佳实践

### 4.1 文档规范

1. **每个接口都要有描述**：使用 `@Operation` 提供清晰的 summary 和 description
2. **参数要有示例**：使用 `example` 属性提供示例值
3. **响应要完整**：使用 `@ApiResponses` 描述所有可能的响应
4. **DTO 要有文档**：使用 `@Schema` 描述数据模型

### 4.2 安全配置

1. **生产环境考虑禁用**：避免暴露 API 结构
2. **配置访问控制**：只允许特定角色访问文档
3. **敏感信息脱敏**：不要在文档中暴露敏感参数

### 4.3 版本管理

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Dorm Power API")
            .version("1.0.0")  // 从配置或构建信息读取
            // ...
        );
}
```

### 4.4 分组管理

对于大型项目，可以按模块分组：

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

---

## 五、常见问题

### Q1: Swagger UI 页面空白？

检查：
1. 是否正确引入依赖
2. Spring Security 是否放行了相关路径
3. 是否有路径匹配冲突

### Q2: 中文乱码？

配置编码：

```yaml
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

### Q3: 如何隐藏某些接口？

```java
@Operation(hidden = true)
@GetMapping("/internal")
public void internalApi() {
    // ...
}
```

### Q4: 如何自定义响应示例？

```java
@ApiResponse(
    responseCode = "200",
    content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(
            name = "成功示例",
            value = "{\"id\": \"device_001\", \"name\": \"智能插座\"}"
        )
    )
)
```

---

## 六、总结

SpringDoc OpenAPI 是 Spring Boot 3 项目中生成 API 文档的最佳选择：

| 优势 | 说明 |
|------|------|
| **自动化** | 自动扫描 Controller 生成文档，减少维护成本 |
| **标准化** | 基于 OpenAPI 3.0 规范，支持多种工具集成 |
| **可视化** | 提供 Swagger UI 界面，方便测试和调试 |
| **活跃维护** | 持续更新，支持最新 Spring Boot 版本 |
| **易集成** | 与 Spring Security、Spring MVC 无缝集成 |

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日
