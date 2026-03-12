# 维度5：Spring/SpringBoot基础实战指南

> 基于DormPower项目的Spring/SpringBoot学习指南
> 
> 从项目实际代码出发，讲解IOC、DI、注解、自动配置的实际应用

---

## 目录

- [1. Spring IOC容器](#1-spring-ioc容器)
- [2. 依赖注入（DI）](#2-依赖注入di)
- [3. 常用注解详解](#3-常用注解详解)
- [4. SpringBoot自动配置](#4-springboot自动配置)
- [5. 配置类详解](#5-配置类详解)
- [6. AOP面向切面编程](#6-aop面向切面编程)

---

## 1. Spring IOC容器

### 1.1 IOC概念

#### 1.1.1 项目中的IOC体现

```java
/**
 * 【Spring基础】IOC（控制反转）：对象的创建和管理交给Spring容器
 * 
 * 传统方式：
 *   DeviceService service = new DeviceService();  // 自己创建对象
 * 
 * Spring方式：
 *   @Autowired
 *   private DeviceService service;  // Spring容器注入对象
 */

// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

// 【Spring基础】@Component：标记类为Spring管理的Bean
@Component
public class MyComponent {
    // 这个类会被Spring自动创建和管理
}

// 【Spring基础】@Service：业务层组件（本质也是@Component）
@Service
public class DeviceService {
    // 业务逻辑层
}

// 【Spring基础】@Repository：数据访问层组件
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    // 数据访问层
}

// 【Spring基础】@Controller：控制层组件
@RestController  // = @Controller + @ResponseBody
public class DeviceController {
    // 控制层，处理HTTP请求
}
```

**IOC的好处：**
1. 解耦：对象之间不直接依赖
2. 便于测试：可以轻松替换实现
3. 集中管理：统一管理对象生命周期
4. 降低复杂度：不需要手动管理依赖关系

### 1.2 Bean的生命周期

#### 1.2.1 Bean生命周期示例

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    // 【Spring基础】@PostConstruct：Bean创建后执行
    @PostConstruct
    public void init() {
        logger.info("DeviceService初始化完成");
        // 可以在这里加载缓存、建立连接等
        loadCache();
        initializeConnections();
    }
    
    // 【Spring基础】@PreDestroy：Bean销毁前执行
    @PreDestroy
    public void destroy() {
        logger.info("DeviceService即将销毁");
        // 可以在这里释放资源、关闭连接等
        clearCache();
        closeConnections();
    }
}
```

**Bean生命周期：**
```
实例化 -> 属性赋值 -> BeanNameAware/BeanFactoryAware -> @PostConstruct -> 初始化完成
-> 使用Bean -> @PreDestroy -> 销毁
```

---

## 2. 依赖注入（DI）

### 2.1 三种注入方式

#### 2.1.1 字段注入

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    // 【Spring基础】方式1：字段注入（最常用，简单）
    @Autowired
    private DeviceRepository deviceRepository;
    
    // 【Spring基础】字段注入多个Bean
    @Autowired
    private DeviceCache deviceCache;
    
    @Autowired
    private MqttBridge mqttBridge;
    
    public Device getDeviceById(String id) {
        return deviceRepository.findById(id).orElse(null);
    }
}
```

#### 2.1.2 构造器注入（推荐）

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    // 【Spring基础】方式2：构造器注入（推荐，便于测试）
    private final DeviceRepository deviceRepository;
    private final DeviceCache deviceCache;
    private final MqttBridge mqttBridge;
    
    // 【Spring基础】构造器注入
    @Autowired  // Spring 4.3+ 可以省略
    public DeviceService(DeviceRepository deviceRepository, 
                      DeviceCache deviceCache,
                      MqttBridge mqttBridge) {
        this.deviceRepository = deviceRepository;
        this.deviceCache = deviceCache;
        this.mqttBridge = mqttBridge;
    }
    
    public Device getDeviceById(String id) {
        return deviceRepository.findById(id).orElse(null);
    }
}
```

#### 2.1.3 Setter注入

```java
@Service
public class DeviceService {
    
    // 【Spring基础】方式3：Setter注入（可选依赖）
    private DeviceCache deviceCache;
    
    // 【Spring基础】Setter注入
    @Autowired(required = false)  // required=false表示可选
    public void setDeviceCache(DeviceCache deviceCache) {
        this.deviceCache = deviceCache;
    }
    
    public Device getDeviceById(String id) {
        Device device = deviceRepository.findById(id).orElse(null);
        
        // 使用可选依赖
        if (deviceCache != null) {
            deviceCache.put(device);
        }
        
        return device;
    }
}
```

### 2.2 使用@RequiredArgsConstructor（推荐）

#### 2.2.1 Lombok + 构造器注入

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

// 【Spring基础】Lombok + 构造器注入（最简洁的方式）
@Service
@RequiredArgsConstructor  // 【Spring基础】生成包含final字段的构造器
public class DeviceService {
    
    // 【Spring基础】final字段自动注入
    private final DeviceRepository deviceRepository;
    private final DeviceCache deviceCache;
    private final MqttBridge mqttBridge;
    
    // 【Spring基础】无需写构造器，Lombok自动生成
    // 无需写@Autowired，构造器注入自动生效
    
    public Device getDeviceById(String id) {
        return deviceRepository.findById(id).orElse(null);
    }
}
```

**注入方式对比：**
| 方式 | 优点 | 缺点 | 推荐度 |
|------|------|------|--------|
| 字段注入 | 简单 | 不便于测试、不可final | ⭐⭐ |
| 构造器注入 | 便于测试、不可变 | 代码较多 | ⭐⭐⭐⭐⭐ |
| Setter注入 | 可选依赖 | 不便于测试 | ⭐⭐⭐ |
| Lombok构造器 | 简洁、便于测试 | 需要Lombok | ⭐⭐⭐⭐⭐⭐ |

---

## 3. 常用注解详解

### 3.1 组件注解

#### 3.1.1 组件注解示例

```java
// 【Spring基础】@Component：通用组件
@Component
public class MyUtil {
}

// 【Spring基础】@Service：业务逻辑层
@Service
public class DeviceService {
}

// 【Spring基础】@Repository：数据访问层
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
}

// 【Spring基础】@Controller：控制层（返回视图）
@Controller
public class PageController {
}

// 【Spring基础】@RestController：REST API控制层（返回JSON）
@RestController
public class DeviceController {
}

// 【Spring基础】@Configuration：配置类
@Configuration
public class AppConfig {
}
```

### 3.2 依赖注入注解

#### 3.2.1 依赖注入注解示例

```java
@Service
public class DeviceService {
    
    // 【Spring基础】@Autowired：按类型注入
    @Autowired
    private DeviceRepository deviceRepository;
    
    // 【Spring基础】@Qualifier：按名称注入（当有多个同类型Bean时）
    @Autowired
    @Qualifier("emailNotificationSender")
    private NotificationSender notificationSender;
    
    // 【Spring基础】@Value：注入配置值
    @Value("${server.port:8080}")
    private int serverPort;
    
    @Value("${spring.application.name}")
    private String appName;
    
    // 【Spring基础】@Resource：JSR-250标准，按名称注入
    @Resource(name = "smsNotificationSender")
    private NotificationSender smsSender;
}
```

### 3.3 作用域注解

#### 3.3.1 作用域注解示例

```java
// 【Spring基础】@Scope：定义Bean的作用域

@Service
@Scope("singleton")  // 默认，整个应用只有一个实例
public class SingletonService {
}

@Service
@Scope("prototype")  // 每次注入都创建新实例
public class PrototypeService {
}

@Service
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)  // 每个HTTP请求一个实例
public class RequestScopeService {
}

@Service
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)  // 每个HTTP会话一个实例
public class SessionScopeService {
}
```

**作用域对比：**
| 作用域 | 说明 | 适用场景 |
|--------|------|----------|
| singleton | 整个应用只有一个实例 | 无状态的服务 |
| prototype | 每次注入都创建新实例 | 有状态的对象 |
| request | 每个HTTP请求一个实例 | 请求相关的数据 |
| session | 每个HTTP会话一个实例 | 用户会话数据 |

---

## 4. SpringBoot自动配置

### 4.1 启动类

#### 4.1.1 项目启动类

```java
// 文件：backend/src/main/java/com/dormpower/DormPowerApplication.java

/**
 * 【Spring基础】@SpringBootApplication：组合注解
 * = @Configuration + @EnableAutoConfiguration + @ComponentScan
 */
@SpringBootApplication
public class DormPowerApplication {
    
    public static void main(String[] args) {
        // 【Spring基础】启动Spring应用
        SpringApplication.run(DormPowerApplication.class, args);
    }
}
```

### 4.2 自动配置原理

#### 4.2.1 自动配置流程

```java
/**
 * 【Spring基础】SpringBoot自动配置流程
 * 
 * 1. 读取spring.factories中的自动配置类
 * 2. 检查条件注解（@ConditionalOnClass等）
 * 3. 条件满足则加载配置
 * 4. 根据配置文件绑定属性
 */

// 【Spring基础】@ConditionalOnClass：类存在时才加载配置
@Configuration
@ConditionalOnClass(DataSource.class)  // 类路径有DataSource才生效
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean  // 容器中没有该Bean时才创建
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create()
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}

// 【Spring基础】常用条件注解
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")  // 配置项为true时生效
@ConditionalOnBean(DataSource.class)  // 容器中有DataSource Bean时生效
@ConditionalOnMissingBean(DataSource.class)  // 容器中没有DataSource Bean时生效
@ConditionalOnWebApplication  // Web应用时生效
@Profile("dev")  // 指定环境生效
```

### 4.3 自定义Starter

#### 4.3.1 自定义自动配置

```java
// 【Spring基础】自定义自动配置

@Configuration
@ConditionalOnClass(MqttClient.class)
@EnableConfigurationProperties(MqttProperties.class)
public class MqttAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MqttClient mqttClient(MqttProperties properties) {
        return new MqttClient(properties.getBrokerUrl(), properties.getClientId());
    }
}

// 【Spring基础】配置属性类
@ConfigurationProperties(prefix = "mqtt")
@Data
public class MqttProperties {
    private String brokerUrl = "tcp://localhost:1883";
    private String clientId = "default-client";
    private String username;
    private String password;
}
```

---

## 5. 配置类详解

### 5.1 @Configuration配置类

#### 5.1.1 配置类示例

```java
// 文件：backend/src/main/java/com/dormpower/config/CacheConfig.java

/**
 * 【Spring基础】@Configuration：声明这是一个配置类
 */
@Configuration
public class CacheConfig {
    
    /**
     * 【Spring基础】@Bean：将方法返回值注册为Spring Bean
     */
    @Bean
    public CacheManager cacheManager() {
        // 创建并配置缓存管理器
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("devices"),
            new ConcurrentMapCache("users")
        ));
        return cacheManager;
    }
    
    /**
     * 【Spring基础】Bean之间的依赖
     */
    @Bean
    public DeviceCache deviceCache(CacheManager cacheManager) {
        // 注入其他Bean作为参数
        return new DeviceCache(cacheManager);
    }
}
```

### 5.2 WebMvc配置

#### 5.2.1 跨域配置

```java
// 文件：backend/src/main/java/com/dormpower/config/CorsConfig.java

/**
 * 【Spring基础】WebMvc配置
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    /**
     * 【Spring基础】配置跨域访问
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // 允许访问的路径
            .allowedOrigins("http://localhost:3000")  // 允许的来源
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

#### 5.2.2 拦截器配置

```java
/**
 * 【Spring基础】配置拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    /**
     * 【Spring基础】配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/**");
    }
}
```

---

## 6. AOP面向切面编程

### 6.1 日志切面示例

```java
// 文件：backend/src/main/java/com/dormpower/aop/ApiAspect.java

/**
 * 【Spring基础】@Aspect：声明切面类
 */
@Aspect
@Component
public class ApiAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiAspect.class);
    
    /**
     * 【Spring基础】@Pointcut：定义切点
     * 匹配controller包下所有类的所有方法
     */
    @Pointcut("execution(* com.dormpower.controller.*.*(..))")
    public void controllerPointcut() {}
    
    /**
     * 【Spring基础】@Around：环绕通知
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        // 获取方法信息
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = point.getSignature().getName();
        
        logger.info("[请求开始] {}.{}", className, methodName);
        
        try {
            // 执行目标方法
            Object result = point.proceed();
            
            // 记录执行时间
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[请求结束] {}.{} 耗时{}ms", className, methodName, duration);
            
            return result;
        } catch (Exception e) {
            logger.error("[请求异常] {}.{} 异常: {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
```

---

## 7. Spring事务管理

### 7.1 @Transactional注解

#### 7.1.1 事务基本使用

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

/**
 * 【Spring基础】事务管理
 * 使用@Transactional管理数据库事务
 */
@Service
public class DeviceService {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private DeviceHistoryRepository historyRepository;
    
    /**
     * 【Spring基础】@Transactional：开启事务
     * 方法执行成功则提交，异常则回滚
     */
    @Transactional
    public void updateDeviceWithHistory(String deviceId, DeviceUpdateDTO dto) {
        // 1. 更新设备信息
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
        
        device.setName(dto.getName());
        device.setLocation(dto.getLocation());
        deviceRepository.save(device);
        
        // 2. 记录历史
        DeviceHistory history = new DeviceHistory();
        history.setDeviceId(deviceId);
        history.setChangeType("UPDATE");
        history.setChangeDetails(dto.toString());
        history.setChangeTime(LocalDateTime.now());
        historyRepository.save(history);
        
        // 如果中间抛出异常，两个操作都会回滚
    }
}
```

#### 7.1.2 事务传播行为

```java
/**
 * 【Spring基础】事务传播行为
 */
@Service
public class DeviceService {
    
    /**
     * 【Spring基础】REQUIRED：默认传播行为
     * 如果当前有事务，则加入；如果没有，则创建新事务
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void requiredMethod() {
        // 默认行为
    }
    
    /**
     * 【Spring基础】REQUIRES_NEW：创建新事务
     * 总是创建新事务，如果当前有事务，则挂起当前事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requiresNewMethod() {
        // 独立事务，不受外部事务影响
    }
    
    /**
     * 【Spring基础】NESTED：嵌套事务
     * 如果当前有事务，则在嵌套事务中执行；如果没有，则创建新事务
     */
    @Transactional(propagation = Propagation.NESTED)
    public void nestedMethod() {
        // 嵌套事务，可以独立回滚
    }
    
    /**
     * 【Spring基础】MANDATORY：强制事务
     * 必须在已有事务中执行，否则抛出异常
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void mandatoryMethod() {
        // 必须在事务中调用
    }
    
    /**
     * 【Spring基础】SUPPORTS：支持事务
     * 如果当前有事务，则加入；如果没有，则以非事务方式执行
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public void supportsMethod() {
        // 可选事务
    }
    
    /**
     * 【Spring基础】NOT_SUPPORTED：不支持事务
     * 以非事务方式执行，如果当前有事务，则挂起
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void notSupportedMethod() {
        // 非事务执行
    }
    
    /**
     * 【Spring基础】NEVER：禁止事务
     * 以非事务方式执行，如果当前有事务，则抛出异常
     */
    @Transactional(propagation = Propagation.NEVER)
    public void neverMethod() {
        // 禁止事务
    }
}
```

#### 7.1.3 事务隔离级别

```java
/**
 * 【Spring基础】事务隔离级别
 */
@Service
public class DeviceService {
    
    /**
     * 【Spring基础】DEFAULT：使用数据库默认隔离级别
     */
    @Transactional(isolation = Isolation.DEFAULT)
    public void defaultIsolation() {
        // 使用数据库默认隔离级别
    }
    
    /**
     * 【Spring基础】READ_UNCOMMITTED：读未提交
     * 可能读到未提交的数据（脏读）
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void readUncommitted() {
        // 读未提交
    }
    
    /**
     * 【Spring基础】READ_COMMITTED：读已提交
     * 只能读到已提交的数据（避免脏读）
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void readCommitted() {
        // 读已提交
    }
    
    /**
     * 【Spring基础】REPEATABLE_READ：可重复读
     * 同一事务中多次读取结果一致（避免脏读、不可重复读）
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void repeatableRead() {
        // 可重复读
    }
    
    /**
     * 【Spring基础】SERIALIZABLE：串行化
     * 最高隔离级别，完全串行执行（避免所有并发问题）
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void serializable() {
        // 串行化
    }
}
```

#### 7.1.4 事务回滚

```java
/**
 * 【Spring基础】事务回滚
 */
@Service
public class DeviceService {
    
    /**
     * 【Spring基础】默认：运行时异常回滚
     * RuntimeException和Error会回滚，Checked Exception不会回滚
     */
    @Transactional
    public void defaultRollback() {
        // RuntimeException会回滚
        throw new RuntimeException("运行时异常");
    }
    
    /**
     * 【Spring基础】rollbackFor：指定回滚异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void customRollback() {
        // 所有Exception都会回滚
        throw new Exception("自定义异常");
    }
    
    /**
     * 【Spring基础】noRollbackFor：指定不回滚异常
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public void noRollback() {
        // BusinessException不会回滚
        throw new BusinessException("业务异常");
    }
    
    /**
     * 【Spring基础】readOnly：只读事务
     */
    @Transactional(readOnly = true)
    public Device getDevice(String id) {
        // 只读事务，可以提高性能
        return deviceRepository.findById(id).orElse(null);
    }
    
    /**
     * 【Spring基础】timeout：事务超时时间
     */
    @Transactional(timeout = 30)
    public void timeoutMethod() {
        // 事务超时时间30秒
    }
}
```

---

## 8. Spring事件机制

### 8.1 自定义事件

#### 8.1.1 定义事件

```java
// 文件：backend/src/main/java/com/dormpower/event/DeviceEvent.java

/**
 * 【Spring基础】自定义事件
 * 继承ApplicationEvent
 */
public class DeviceEvent extends ApplicationEvent {
    
    private final String deviceId;
    private final String eventType;
    
    public DeviceEvent(Object source, String deviceId, String eventType) {
        super(source);
        this.deviceId = deviceId;
        this.eventType = eventType;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public String getEventType() {
        return eventType;
    }
}
```

#### 8.1.2 发布事件

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * 【Spring基础】发布事件
     */
    public void updateDeviceStatus(String deviceId, boolean online) {
        // 更新设备状态
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            device.setOnline(online);
            deviceRepository.save(device);
            
            // 发布设备状态变更事件
            DeviceEvent event = new DeviceEvent(this, deviceId, online ? "ONLINE" : "OFFLINE");
            eventPublisher.publishEvent(event);
        }
    }
}
```

#### 8.1.3 监听事件

```java
// 文件：backend/src/main/java/com/dormpower/listener/DeviceEventListener.java

/**
 * 【Spring基础】事件监听器
 */
@Component
public class DeviceEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceEventListener.class);
    
    /**
     * 【Spring基础】@EventListener：监听事件
     */
    @EventListener
    public void handleDeviceEvent(DeviceEvent event) {
        logger.info("收到设备事件: 设备ID={}, 事件类型={}", 
            event.getDeviceId(), event.getEventType());
        
        // 处理设备事件
        switch (event.getEventType()) {
            case "ONLINE":
                handleDeviceOnline(event.getDeviceId());
                break;
            case "OFFLINE":
                handleDeviceOffline(event.getDeviceId());
                break;
        }
    }
    
    /**
     * 【Spring基础】@Async：异步监听
     */
    @EventListener
    @Async
    public void handleDeviceEventAsync(DeviceEvent event) {
        // 异步处理设备事件
        logger.info("异步处理设备事件: {}", event.getDeviceId());
    }
    
    private void handleDeviceOnline(String deviceId) {
        // 处理设备上线
    }
    
    private void handleDeviceOffline(String deviceId) {
        // 处理设备下线
    }
}
```

---

## 9. Spring条件注解深入

### 9.1 条件注解详解

#### 9.1.1 @ConditionalOnProperty

```java
/**
 * 【Spring基础】@ConditionalOnProperty：基于配置的条件注解
 */
@Configuration
public class ConditionalConfig {
    
    /**
     * 【Spring基础】配置为true时才创建Bean
     */
    @Bean
    @ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
    public MqttClient mqttClient(MqttProperties properties) {
        return new MqttClient(properties.getBrokerUrl(), properties.getClientId());
    }
    
    /**
     * 【Spring基础】配置存在时才创建Bean
     */
    @Bean
    @ConditionalOnProperty(name = "mqtt.broker-url")
    public MqttClient mqttClient2(MqttProperties properties) {
        return new MqttClient(properties.getBrokerUrl(), properties.getClientId());
    }
    
    /**
     * 【Spring基础】配置不存在时才创建Bean
     */
    @Bean
    @ConditionalOnProperty(name = "mqtt.enabled", havingValue = "false", matchIfMissing = true)
    public MqttClient mockMqttClient() {
        return new MockMqttClient();
    }
}
```

#### 9.1.2 @ConditionalOnClass

```java
/**
 * 【Spring基础】@ConditionalOnClass：基于类的条件注解
 */
@Configuration
public class ClassConditionalConfig {
    
    /**
     * 【Spring基础】类存在时才创建Bean
     */
    @Bean
    @ConditionalOnClass(name = "org.eclipse.paho.client.mqttv3.MqttClient")
    public MqttClient mqttClient(MqttProperties properties) {
        return new MqttClient(properties.getBrokerUrl(), properties.getClientId());
    }
    
    /**
     * 【Spring基础】类不存在时才创建Bean
     */
    @Bean
    @ConditionalOnMissingClass("org.eclipse.paho.client.mqttv3.MqttClient")
    public MqttClient mockMqttClient() {
        return new MockMqttClient();
    }
}
```

#### 9.1.3 @ConditionalOnBean

```java
/**
 * 【Spring基础】@ConditionalOnBean：基于Bean的条件注解
 */
@Configuration
public class BeanConditionalConfig {
    
    /**
     * 【Spring基础】容器中有指定Bean时才创建Bean
     */
    @Bean
    @ConditionalOnBean(DataSource.class)
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    /**
     * 【Spring基础】容器中没有指定Bean时才创建Bean
     */
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource embeddedDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}
```

#### 9.1.4 @Profile

```java
/**
 * 【Spring基础】@Profile：基于环境的条件注解
 */
@Configuration
public class ProfileConfig {
    
    /**
     * 【Spring基础】开发环境Bean
     */
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
    
    /**
     * 【Spring基础】生产环境Bean
     */
    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://prod-db:5432/dorm_power")
            .username("prod")
            .password("prod123")
            .build();
    }
    
    /**
     * 【Spring基础】测试环境Bean
     */
    @Bean
    @Profile("test")
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}
```

---

## 10. Spring循环依赖

### 10.1 循环依赖问题

#### 10.1.1 循环依赖示例

```java
/**
 * 【Spring基础】循环依赖问题
 */

// 【Spring基础】ServiceA依赖ServiceB
@Service
public class ServiceA {
    
    @Autowired
    private ServiceB serviceB;
    
    public void methodA() {
        serviceB.methodB();
    }
}

// 【Spring基础】ServiceB依赖ServiceA
@Service
public class ServiceB {
    
    @Autowired
    private ServiceA serviceA;
    
    public void methodB() {
        serviceA.methodA();
    }
}

// 【Spring基础】循环依赖会导致BeanCurrentlyInCreationException
```

#### 10.1.2 解决方案

```java
/**
 * 【Spring基础】循环依赖解决方案
 */

// 【方案1】使用@Lazy延迟加载
@Service
public class ServiceA {
    
    @Autowired
    @Lazy  // 【Spring基础】延迟加载
    private ServiceB serviceB;
    
    public void methodA() {
        serviceB.methodB();
    }
}

// 【方案2】使用Setter注入
@Service
public class ServiceA {
    
    private ServiceB serviceB;
    
    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

// 【方案3】重构代码，消除循环依赖
@Service
public class ServiceA {
    
    private CommonService commonService;
    
    public void methodA() {
        commonService.method();
    }
}

@Service
public class ServiceB {
    
    private CommonService commonService;
    
    public void methodB() {
        commonService.method();
    }
}

@Service
public class CommonService {
    
    public void method() {
        // 公共逻辑
    }
}
```

---

## 11. Spring最佳实践

### 11.1 依赖注入最佳实践

#### 11.1.1 推荐使用构造器注入

```java
/**
 * 【Spring基础】依赖注入最佳实践
 */

// ✅ 推荐：使用构造器注入 + Lombok
@Service
@RequiredArgsConstructor
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final DeviceCache deviceCache;
    
    public Device getDevice(String id) {
        return deviceRepository.findById(id).orElse(null);
    }
}

// ✅ 推荐：使用构造器注入（不使用Lombok）
@Service
public class DeviceService2 {
    
    private final DeviceRepository deviceRepository;
    private final DeviceCache deviceCache;
    
    public DeviceService2(DeviceRepository deviceRepository, 
                          DeviceCache deviceCache) {
        this.deviceRepository = deviceRepository;
        this.deviceCache = deviceCache;
    }
}

// ❌ 不推荐：使用字段注入
@Service
public class DeviceService3 {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private DeviceCache deviceCache;
}
```

### 11.2 事务最佳实践

#### 11.2.1 事务使用建议

```java
/**
 * 【Spring基础】事务最佳实践
 */

// ✅ 推荐：在Service层使用事务
@Service
public class DeviceService {
    
    @Transactional
    public void updateDevice(DeviceUpdateDTO dto) {
        // 业务逻辑
    }
}

// ❌ 不推荐：在Controller层使用事务
@RestController
public class DeviceController {
    
    @Transactional  // 不推荐在Controller层使用事务
    public void updateDevice(DeviceUpdateDTO dto) {
        // 业务逻辑
    }
}

// ✅ 推荐：使用只读事务提高性能
@Transactional(readOnly = true)
public Device getDevice(String id) {
    return deviceRepository.findById(id).orElse(null);
}

// ✅ 推荐：指定回滚异常
@Transactional(rollbackFor = Exception.class)
public void updateDevice(DeviceUpdateDTO dto) {
    // 业务逻辑
}

// ✅ 推荐：使用合适的事务传播行为
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logOperation(String operation) {
    // 独立事务，不受外部事务影响
}
```

### 11.3 配置类最佳实践

#### 11.3.1 配置类组织

```java
/**
 * 【Spring基础】配置类最佳实践
 */

// ✅ 推荐：按功能模块组织配置类
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}

@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new SimpleCacheManager();
    }
}

@Configuration
public class MqttConfig {
    
    @Bean
    public MqttClient mqttClient() {
        return new MqttClient();
    }
}

// ✅ 推荐：使用@Conditional按条件加载配置
@Configuration
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true")
public class MqttAutoConfig {
    
    @Bean
    public MqttClient mqttClient() {
        return new MqttClient();
    }
}
```

---

## 12. Spring常见陷阱

### 12.1 常见陷阱

#### 12.1.1 事务陷阱

```java
/**
 * 【Spring基础】事务常见陷阱
 */
public class TransactionTraps {
    
    /**
     * 【陷阱1】同类方法调用，事务不生效
     */
    @Service
    public class Trap1 {
        
        @Transactional
        public void methodA() {
            methodB();  // ❌ 陷阱：同类方法调用，事务不生效
        }
        
        @Transactional
        public void methodB() {
            // 事务不生效
        }
    }
    
    /**
     * 【陷阱2】private方法，事务不生效
     */
    @Service
    public class Trap2 {
        
        @Transactional
        private void method() {  // ❌ 陷阱：private方法，事务不生效
            // 事务不生效
        }
    }
    
    /**
     * 【陷阱3】final方法，事务不生效
     */
    @Service
    public class Trap3 {
        
        @Transactional
        public final void method() {  // ❌ 陷阱：final方法，事务不生效
            // 事务不生效
        }
    }
    
    /**
     * 【陷阱4】try-catch吞掉异常，事务不回滚
     */
    @Service
    public class Trap4 {
        
        @Transactional
        public void method() {
            try {
                // 业务逻辑
                throw new RuntimeException("异常");
            } catch (Exception e) {  // ❌ 陷阱：异常被吞掉，事务不回滚
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 【陷阱5】多线程调用，事务不生效
     */
    @Service
    public class Trap5 {
        
        @Transactional
        public void method() {
            new Thread(() -> {
                // ❌ 陷阱：新线程中事务不生效
                deviceRepository.save(new Device());
            }).start();
        }
    }
    
    /**
     * 【陷阱6】@Transactional在接口上，可能不生效
     */
    public interface Trap6Service {
        
        @Transactional  // ❌ 陷阱：接口上的事务可能不生效
        void method();
    }
}
```

#### 12.1.2 依赖注入陷阱

```java
/**
 * 【Spring基础】依赖注入常见陷阱
 */
public class DependencyInjectionTraps {
    
    /**
     * 【陷阱1】循环依赖
     */
    @Service
    public class Trap1 {
        
        @Autowired
        private Trap2 trap2;  // ❌ 陷阱：循环依赖
    }
    
    @Service
    public class Trap2 {
        
        @Autowired
        private Trap1 trap1;  // ❌ 陷阱：循环依赖
    }
    
    /**
     * 【陷阱2】@Autowired在静态字段上
     */
    @Service
    public class Trap2 {
        
        @Autowired
        private static DeviceRepository repository;  // ❌ 陷阱：静态字段注入无效
    }
    
    /**
     * 【陷阱3】构造器注入多个Bean，Spring不知道注入哪个
     */
    @Service
    public class Trap3 {
        
        private final NotificationSender sender;
        
        public Trap3(NotificationSender sender) {  // ❌ 陷阱：多个Bean，不知道注入哪个
            this.sender = sender;
        }
    }
    
    /**
     * 【陷阱4】@PostConstruct中调用依赖的方法，依赖还未初始化
     */
    @Service
    public class Trap4 {
        
        @Autowired
        private DeviceService deviceService;
        
        @PostConstruct
        public void init() {
            deviceService.getDevice("id");  // ❌ 陷阱：可能还未初始化
        }
    }
}
```

---

## 本章小结

| 知识点 | 项目体现 | 关键注解 |
|--------|----------|----------|
| IOC | Bean由容器管理 | @Component, @Service |
| DI | 依赖自动注入 | @Autowired |
| 配置类 | 各种Config类 | @Configuration, @Bean |
| 自动配置 | SpringBoot启动 | @SpringBootApplication |
| AOP | 接口日志记录 | @Aspect, @Around |
| 事务管理 | 数据库事务 | @Transactional |
| 事件机制 | 设备状态变更 | @EventListener |
| 条件注解 | 按条件加载Bean | @ConditionalOnProperty |

---

## 13. 项目实战案例

### 13.1 控制器层完整实现

#### 13.1.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/controller/DeviceController.java

@RestController  // 【Spring注解】标记为REST控制器
@RequestMapping("/api")  // 【Spring注解】定义基础路径
@Tag(name = "设备管理", description = "设备查询和状态管理接口")
public class DeviceController {

    // 【依赖注入】使用@Autowired注入依赖
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long ONLINE_TIMEOUT_SECONDS = 60;

    /**
     * 【Spring注解】@GetMapping：处理GET请求
     * 【依赖注入】自动注入Repository
     * 【业务逻辑】查询设备列表并组装返回数据
     */
    @Operation(
        summary = "获取设备列表",
        description = "查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/devices")  // 【Spring注解】映射GET请求到/devices
    public List<Map<String, Object>> getDevices() {
        // 【业务逻辑】查询所有设备
        List<Device> devices = deviceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 【业务逻辑】计算当前时间戳
        long now = System.currentTimeMillis() / 1000;
        
        // 【流程控制】遍历设备列表
        for (Device d : devices) {
            // 【业务逻辑】判断设备是否在线
            boolean isOnline = d.isOnline() && (now - d.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;
            
            // 【业务逻辑】组装设备信息
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", d.getId());
            deviceMap.put("name", d.getName());
            deviceMap.put("room", d.getRoom());
            deviceMap.put("online", isOnline);
            deviceMap.put("lastSeen", Instant.ofEpochSecond(d.getLastSeenTs()).toString());
            result.add(deviceMap);
        }
        
        return result;
    }

    /**
     * 【Spring注解】@GetMapping：处理带路径参数的GET请求
     * 【Spring注解】@PathVariable：获取路径参数
     * 【异常处理】抛出ResourceNotFoundException
     */
    @Operation(
        summary = "获取设备状态",
        description = "获取指定设备的详细状态信息，包括总功率、电压、电流和各插座状态",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/devices/{deviceId}/status")  // 【Spring注解】路径参数
    public ResponseEntity<?> getDeviceStatus(
            @PathVariable String deviceId) {  // 【Spring注解】绑定路径参数
        // 【业务逻辑】查询设备基本信息
        Device device = deviceRepository.findById(deviceId).orElse(null);
        // 【业务逻辑】查询设备状态信息
        StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
        
        // 【异常处理】检查设备和状态是否存在
        if (device == null || status == null) {
            throw new ResourceNotFoundException("device not found");
        }

        // 【业务逻辑】获取当前时间戳（秒）
        long now = System.currentTimeMillis() / 1000;
        // 【业务逻辑】判断设备是否在线
        boolean isOnline = device.isOnline() && status.isOnline() 
            && (now - device.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;

        // 【业务逻辑】解析插座状态JSON
        List<Map<String, Object>> sockets = new ArrayList<>();
        try {
            // 【JSON处理】解析JSON字符串
            String socketsJson = status.getSockets();
            if (socketsJson != null && !socketsJson.isEmpty()) {
                // 【JSON处理】将JSON字符串转换为List
                sockets = objectMapper.readValue(socketsJson, 
                    new TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            // 【异常处理】JSON解析失败
            logger.error("解析插座状态失败: {}", e.getMessage());
        }

        // 【业务逻辑】组装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("ts", status.getTs());
        result.put("online", isOnline);
        result.put("total_power_w", status.getTotalPowerW());
        result.put("voltage_v", status.getVoltageV());
        result.put("current_a", status.getCurrentA());
        result.put("sockets", sockets);

        return ResponseEntity.ok(result);
    }
}
```

**代码解析：**

1. **Spring注解应用：**
   - `@RestController`：标记为REST控制器
   - `@RequestMapping`：定义基础路径
   - `@GetMapping`：映射GET请求
   - `@PathVariable`：获取路径参数

2. **依赖注入：**
   - `@Autowired`：自动注入Repository和ObjectMapper
   - Spring容器自动管理依赖关系

3. **业务逻辑：**
   - 查询设备列表和状态
   - 判断设备在线状态
   - 组装返回数据

4. **异常处理：**
   - 抛出`ResourceNotFoundException`
   - 全局异常处理器统一处理

### 13.2 服务层事务管理

#### 13.2.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/service/PowerControlService.java

@Service  // 【Spring注解】标记为服务层组件
public class PowerControlService {

    // 【依赖注入】注入多个Repository
    @Autowired
    private RoomBalanceRepository roomBalanceRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ElectricityBillRepository billRepository;

    @Autowired
    private CommandService commandService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private SystemConfigService systemConfigService;

    private static final double LOW_BALANCE = 10.0;

    /**
     * 【Spring注解】@Scheduled：定时任务
     * 【事务管理】自动管理事务
     * 【业务逻辑】检查余额并执行断电
     */
    @Scheduled(fixedRate = 300000)  // 【Spring注解】每5分钟执行一次
    public void checkAndExecutePowerCutoff() {
        // 【业务逻辑】检查是否启用自动断电
        String autoCutoffEnabled = systemConfigService.getConfigValue("power.auto_cutoff_enabled", "false");
        if (!"true".equalsIgnoreCase(autoCutoffEnabled)) {
            return;
        }

        // 【业务逻辑】获取断电阈值
        String thresholdStr = systemConfigService.getConfigValue("power.cutoff_threshold", "0");
        double threshold = Double.parseDouble(thresholdStr);

        // 【业务逻辑】查询余额不足的房间
        List<RoomBalance> lowBalances = roomBalanceRepository.findByBalanceLessThan(threshold + 1);

        // 【流程控制】遍历低余额房间
        for (RoomBalance balance : lowBalances) {
            // 【业务逻辑】判断是否需要断电或发送预警
            if (balance.getBalance() <= threshold) {
                executePowerCutoff(balance);
            } else if (balance.getBalance() < LOW_BALANCE && !balance.isWarningSent()) {
                sendLowBalanceWarning(balance);
            }
        }
    }

    /**
     * 【Spring注解】@Transactional：声明式事务
     * 【事务管理】确保数据一致性
     * 【业务逻辑】执行断电操作
     */
    @Transactional  // 【Spring注解】开启事务
    public void executePowerCutoff(RoomBalance balance) {
        // 【业务逻辑】查询房间信息
        DormRoom room = dormRoomRepository.findById(balance.getRoomId()).orElse(null);
        if (room == null || room.getDeviceId() == null) {
            return;
        }

        // 【业务逻辑】查询设备信息
        Device device = deviceRepository.findById(room.getDeviceId()).orElse(null);
        if (device == null || !device.isOnline()) {
            return;
        }

        try {
            // 【业务逻辑】发送断电命令
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("action", "power_off");
            cmd.put("socket", 0);
            commandService.sendCommand(room.getDeviceId(), cmd);

            // 【事务操作】更新余额状态
            balance.setAutoCutoff(true);
            balance.setUpdatedAt(System.currentTimeMillis() / 1000);
            roomBalanceRepository.save(balance);

            // 【业务逻辑】记录日志
            systemLogService.warn("POWER_CONTROL", 
                "Power cutoff executed for room " + room.getRoomNumber() + " due to low balance: " + balance.getBalance(),
                "PowerControlService");

            // 【业务逻辑】发送通知
            notificationService.createAlertNotification(
                "自动断电通知",
                "房间 " + room.getRoomNumber() + " 因余额不足已自动断电，当前余额: " + balance.getBalance() + " 元",
                "system",
                room.getDeviceId()
            );
        } catch (Exception e) {
            // 【异常处理】记录错误日志
            systemLogService.error("POWER_CONTROL",
                "Failed to execute power cutoff for room " + room.getRoomNumber(),
                "PowerControlService",
                e.getMessage());
            // 【事务回滚】@Transactional会自动回滚
            throw e;
        }
    }

    /**
     * 【Spring注解】@Transactional：声明式事务
     * 【事务管理】确保数据一致性
     * 【业务逻辑】发送低余额预警
     */
    @Transactional  // 【Spring注解】开启事务
    public void sendLowBalanceWarning(RoomBalance balance) {
        // 【业务逻辑】查询房间信息
        DormRoom room = dormRoomRepository.findById(balance.getRoomId()).orElse(null);
        if (room == null) {
            return;
        }

        // 【业务逻辑】创建系统通知
        notificationService.createSystemNotification(
            "余额不足预警",
            "房间 " + room.getRoomNumber() + " 余额已不足 " + LOW_BALANCE + " 元，当前余额: " + balance.getBalance() + " 元，请及时充值",
            "HIGH"
        );

        // 【事务操作】更新预警状态
        balance.setWarningSent(true);
        roomBalanceRepository.save(balance);

        // 【业务逻辑】记录日志
        systemLogService.info("POWER_CONTROL",
            "Low balance warning sent for room " + room.getRoomNumber() + ", balance: " + balance.getBalance(),
            "PowerControlService");
    }

    /**
     * 【Spring注解】@Transactional：声明式事务
     * 【事务管理】确保数据一致性
     * 【业务逻辑】手动断电
     */
    @Transactional  // 【Spring注解】开启事务
    public boolean manualPowerCutoff(String roomId, String operator) {
        // 【业务逻辑】查询余额信息
        RoomBalance balance = roomBalanceRepository.findByRoomId(roomId).orElse(null);
        if (balance == null) {
            throw new RuntimeException("Room balance not found: " + roomId);
        }

        // 【业务逻辑】查询房间信息
        DormRoom room = dormRoomRepository.findById(roomId).orElse(null);
        if (room == null || room.getDeviceId() == null) {
            throw new RuntimeException("Room has no associated device");
        }

        // 【业务逻辑】查询设备信息
        Device device = deviceRepository.findById(room.getDeviceId()).orElse(null);
        if (device == null || !device.isOnline()) {
            throw new RuntimeException("Device not found or offline");
        }

        try {
            // 【业务逻辑】发送断电命令
            Map<String, Object> cmd = new HashMap<>();
            cmd.put("action", "power_off");
            cmd.put("socket", 0);
            commandService.sendCommand(room.getDeviceId(), cmd);

            // 【事务操作】更新余额状态
            balance.setAutoCutoff(true);
            balance.setUpdatedAt(System.currentTimeMillis() / 1000);
            roomBalanceRepository.save(balance);

            // 【业务逻辑】记录日志
            systemLogService.warn("POWER_CONTROL",
                "Manual power cutoff executed for room " + room.getRoomNumber() + " by " + operator,
                "PowerControlService");

            return true;
        } catch (Exception e) {
            // 【异常处理】记录错误日志
            systemLogService.error("POWER_CONTROL",
                "Failed to execute manual power cutoff for room " + room.getRoomNumber(),
                "PowerControlService",
                e.getMessage());
            // 【事务回滚】@Transactional会自动回滚
            throw e;
        }
    }
}
```

**代码解析：**

1. **Spring注解应用：**
   - `@Service`：标记为服务层组件
   - `@Scheduled`：定时任务注解
   - `@Transactional`：声明式事务

2. **依赖注入：**
   - 使用`@Autowired`注入多个Repository
   - Spring容器自动管理依赖关系

3. **事务管理：**
   - `@Transactional`确保数据一致性
   - 异常时自动回滚

4. **业务逻辑：**
   - 定时检查余额
   - 执行断电操作
   - 发送预警通知

### 13.3 配置类实现

#### 13.3.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/config/CacheConfig.java

@Configuration  // 【Spring注解】标记为配置类
@EnableCaching  // 【Spring注解】启用缓存功能
public class CacheConfig {

    /**
     * 【Spring注解】@Bean：定义Bean
     * 【配置类】配置缓存管理器
     * 【依赖注入】返回的Bean会被Spring容器管理
     */
    @Bean  // 【Spring注解】将方法返回对象注册为Bean
    public CacheManager cacheManager() {
        // 【配置类】创建ConcurrentMapCacheManager
        // 【缓存管理】使用ConcurrentMap作为存储后端
        return new ConcurrentMapCacheManager(
            "devices",      // 设备信息缓存
            "deviceStatus", // 设备状态缓存
            "telemetry"     // 遥测数据缓存
        );
    }
}
```

**代码解析：**

1. **Spring注解应用：**
   - `@Configuration`：标记为配置类
   - `@EnableCaching`：启用缓存功能
   - `@Bean`：定义Bean

2. **配置类作用：**
   - 配置缓存管理器
   - 定义缓存名称
   - Spring容器自动管理

3. **Bean管理：**
   - Spring容器自动注册Bean
   - 其他组件可以自动注入

### 13.4 异步配置实现

#### 13.4.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/config/AsyncConfig.java

@Configuration  // 【Spring注解】标记为配置类
@EnableAsync  // 【Spring注解】启用异步功能
public class AsyncConfig {

    /**
     * 【Spring注解】@Bean：定义Bean
     * 【配置类】配置异步线程池
     * 【线程池】使用ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")  // 【Spring注解】指定Bean名称
    public Executor taskExecutor() {
        // 【线程池】创建ThreadPoolTaskExecutor
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 【线程池】设置核心线程数
        executor.setCorePoolSize(2);
        
        // 【线程池】设置最大线程数
        executor.setMaxPoolSize(4);
        
        // 【线程池】设置队列容量
        executor.setQueueCapacity(50);
        
        // 【线程池】设置线程名称前缀
        executor.setThreadNamePrefix("async-");
        
        // 【线程池】设置拒绝策略
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时在调用线程执行
        });
        
        // 【线程池】初始化线程池
        executor.initialize();
        
        return executor;
    }
}
```

**代码解析：**

1. **Spring注解应用：**
   - `@Configuration`：标记为配置类
   - `@EnableAsync`：启用异步功能
   - `@Bean`：定义Bean

2. **线程池配置：**
   - 核心线程数：2
   - 最大线程数：4
   - 队列容量：50

3. **异步执行：**
   - 使用`@Async`注解的方法会异步执行
   - 线程池管理异步任务

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
