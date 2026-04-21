# 技术深度：Bug 修复与调试技巧

> **学习时长**: 3-5 天  
> **难度**: ⭐⭐⭐⭐  
> **前置知识**: 完成所有功能模块，熟悉系统运行

---

## 一、典型 Bug 分析

### 1.1 并发 Bug

**Bug 描述**: WebSocket 消息丢失

**现象**: 前端设备状态更新延迟或不更新

**原因**: 
```java
// 错误示例 - 非线程安全的集合操作
private List<String> subscribers = new ArrayList<>();

public void addSubscriber(String sessionId) {
    subscribers.add(sessionId);  // 多线程下可能丢失数据
}
```

**修复方案**:
```java
// 正确示例 - 使用线程安全的集合
private final Set<String> subscribers = new CopyOnWriteArraySet<>();

public void addSubscriber(String sessionId) {
    subscribers.add(sessionId);  // 线程安全
}
```

### 1.2 内存泄漏

**Bug 描述**: 应用运行几天后 OOM

**现象**: 内存使用持续增长，GC 无效

**原因**:
```java
// 错误示例 - 静态集合持续增长
private static final Map<String, Device> deviceCache = new HashMap<>();

public void updateDevice(Device device) {
    deviceCache.put(device.getId(), device);  // 只添加不清理
}
```

**修复方案**:
```java
// 正确示例 - 使用 Caffeine 缓存
private final Cache<String, Device> deviceCache = Caffeine.newBuilder()
    .maximumSize(1000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build();

public void updateDevice(Device device) {
    deviceCache.put(device.getId(), device);  // 自动过期清理
}
```

### 1.3 空指针异常

**Bug 描述**: 设备详情页面崩溃

**现象**: 前端显示错误，后端报 NPE

**原因**:
```java
// 错误示例 - 未检查 null
@GetMapping("/devices/{id}")
public DeviceDTO getDevice(@PathVariable String id) {
    Device device = deviceRepository.findById(id);
    Room room = device.getRoom();  // 可能为 null
    return new DeviceDTO(device, room.getName());  // NPE!
}
```

**修复方案**:
```java
// 正确示例 - 使用 Optional
@GetMapping("/devices/{id}")
public DeviceDTO getDevice(@PathVariable String id) {
    Device device = deviceRepository.findById(id)
        .orElseThrow(() -> new DeviceNotFoundException(id));
    
    String roomName = Optional.ofNullable(device.getRoom())
        .map(Room::getName)
        .orElse("未分配");
    
    return new DeviceDTO(device, roomName);
}
```

---

## 二、调试技巧

### 2.1 日志分析

**配置结构化日志**:

```java
@Configuration
public class LoggingConfig {
    
    @Bean
    public JacksonJsonProvider jsonProvider() {
        return new JacksonJsonProvider();
    }
}

// 使用示例
@Slf4j
@Service
public class DeviceService {
    
    public void processDeviceData(String deviceId, String data) {
        log.info("处理设备数据",
            kv("deviceId", deviceId),
            kv("dataLength", data.length()),
            kv("timestamp", System.currentTimeMillis()));
    }
}
```

**日志格式**:
```json
{
  "timestamp": "2024-04-21T10:30:00.000Z",
  "level": "INFO",
  "message": "处理设备数据",
  "deviceId": "DEV-001",
  "dataLength": 1024,
  "timestamp": 1713702600000,
  "thread": "http-nio-8080-exec-1",
  "logger": "com.dormpower.service.DeviceService"
}
```

### 2.2 性能分析

**使用 JProfiler 分析**:

```bash
# 启动应用并启用 JProfiler
java -agentpath:/path/to/jprofiler/bin/linux-x64/libjprofilerti.so=port=8849 \
     -jar dorm-power-backend.jar
```

**热点代码分析**:
```java
// 分析耗时操作
@Profile("debug")
@Component
public class PerformanceAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object profile(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            String methodName = pjp.getSignature().getName();
            if (duration > 1000) {
                log.warn("慢请求: {} 耗时: {}ms", methodName, duration);
            }
        }
    }
}
```

### 2.3 内存分析

**使用 jmap 分析堆**:

```bash
# 生成堆转储文件
jmap -dump:live,format=b,file=heapdump.hprof <pid>

# 分析堆转储
jhat heapdump.hprof
```

**查找内存泄漏**:
```java
// 使用 MAT (Memory Analyzer Tool) 分析
// 1. 打开 heapdump.hprof
// 2. 运行 Dominator Tree
// 3. 查找最大的对象树
// 4. 分析 GC Root
```

---

## 三、常见错误处理

### 3.1 数据库连接错误

**错误**: `Connection refused`

**排查步骤**:
1. 检查数据库服务是否运行
2. 检查网络连接
3. 检查防火墙规则
4. 检查连接池配置

**修复方案**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
```

### 3.2 MQTT 连接错误

**错误**: `Unable to connect to server`

**排查步骤**:
1. 检查 MQTT Broker 状态
2. 检查网络连通性
3. 检查认证信息
4. 检查主题权限

**修复方案**:
```java
@Component
public class MqttReconnectHandler {
    
    @Scheduled(fixedDelay = 5000)
    public void checkAndReconnect() {
        if (!mqttClient.isConnected()) {
            try {
                mqttClient.reconnect();
                log.info("MQTT 重连成功");
            } catch (MqttException e) {
                log.error("MQTT 重连失败", e);
            }
        }
    }
}
```

### 3.3 前端请求错误

**错误**: `401 Unauthorized`

**排查步骤**:
1. 检查 Token 是否过期
2. 检查 Token 格式
3. 检查权限配置
4. 检查拦截器配置

**修复方案**:
```java
@Component
public class JwtTokenProvider {
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期");
            return false;
        } catch (Exception e) {
            log.error("Token 验证失败", e);
            return false;
        }
    }
}
```

---

## 四、测试技巧

### 4.1 单元测试

```java
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    
    @Mock
    private DeviceRepository deviceRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private DeviceService deviceService;
    
    @Test
    void testCreateDevice() {
        // Given
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId("DEV-001");
        dto.setName("测试设备");
        
        when(deviceRepository.existsByDeviceId("DEV-001")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            device.setId(UUID.randomUUID());
            return device;
        });
        
        // When
        Device result = deviceService.createDevice(dto);
        
        // Then
        assertNotNull(result.getId());
        assertEquals("DEV-001", result.getDeviceId());
        verify(eventPublisher).publishEvent(any(DeviceCreatedEvent.class));
    }
}
```

### 4.2 集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DeviceControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
    }
    
    @Test
    void testCreateDeviceEndpoint() {
        // Given
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId("DEV-001");
        dto.setName("测试设备");
        
        // When
        ResponseEntity<Device> response = restTemplate.postForEntity(
            "/api/devices", dto, Device.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("DEV-001", response.getBody().getDeviceId());
    }
}
```

---

## 五、扩展练习

1. 实现自动化性能测试
2. 实现分布式链路追踪
3. 实现错误码统一管理
4. 实现灰度发布策略
5. 实现故障自动恢复机制

---

**最后更新**: 2024-04-21  
**文档版本**: 1.0
