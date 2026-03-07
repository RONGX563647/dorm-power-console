# DormPower项目小白学习指南（8维度完整版）

> 基于DormPower宿舍电力管理系统的真实代码，按8个维度系统讲解Java项目开发
> 
> 维度4（CRUD）和维度7（JUC）已有独立文档，本文档补充其余6个维度

---

## 文档导航

| 维度 | 主题 | 状态 | 对应文档 |
|------|------|------|----------|
| 维度1 | Java基础语法 | 本文档 | 第1章 |
| 维度2 | 集合框架使用 | 本文档 | 第2章 |
| 维度3 | 基础配置 | 本文档 | 第3章 |
| 维度4 | 核心CRUD实战 | 已完成 | [CRUD_Practical_Guide.md](./CRUD_Practical_Guide.md) |
| 维度5 | Spring/SpringBoot基础 | 本文档 | 第5章 |
| 维度6 | 异常处理&日志 | 本文档 | 第6章 |
| 维度7 | JUC并发编程 | 已完成 | [JUC_Basic_Tutorial.md](./JUC_Basic_Tutorial.md) |
| 维度8 | JVM基础调优 | 本文档 | 第8章 |

---

# 第1章：维度1 - Java基础语法

## 1.1 本章学习目标

- 理解项目中变量、方法、流程控制的基础用法
- 掌握字符串处理和日期转换的实际场景
- 理解封装、继承、多态在项目中的体现

## 1.2 变量与数据类型

### 1.2.1 项目中的变量定义

```java
// 文件：backend/src/main/java/com/dormpower/model/Device.java

@Entity
@Table(name = "devices")
public class Device {
    
    // 【基础语法】实例变量（成员变量）
    // String类型：存储设备ID
    @Id
    private String id;
    
    // 【基础语法】实例变量
    // String类型：存储设备名称
    @NotNull
    private String name;
    
    // 【基础语法】实例变量
    // boolean类型：存储在线状态（true/false）
    @NotNull
    private boolean online;
    
    // 【基础语法】实例变量
    // long类型：存储时间戳（整数）
    @NotNull
    private long lastSeenTs;
    
    @NotNull
    private long createdAt;
}
```

**小白解读：**
- `String`：字符串类型，存储文本（如"device_001"）
- `boolean`：布尔类型，只有true/false两个值
- `long`：长整型，存储大整数（如时间戳1609459200）
- `private`：访问修饰符，表示只能在类内部访问（封装的体现）

### 1.2.2 局部变量vs实例变量

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    // 【基础语法】实例变量：属于对象，每个对象独立一份
    @Autowired
    private DeviceRepository deviceRepository;
    
    public Device getDeviceById(String id) {
        // 【基础语法】局部变量：只在方法内有效
        // Optional<Device>：泛型类型，可能包含Device或为空
        Optional<Device> device = deviceRepository.findById(id);
        
        // 【基础语法】局部变量
        // 使用orElseThrow处理可能为空的情况
        return device.orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
    }
}
```

**关键区别：**
| 类型 | 声明位置 | 生命周期 | 作用范围 |
|------|----------|----------|----------|
| 实例变量 | 类中，方法外 | 对象存在期间 | 整个类 |
| 局部变量 | 方法内部 | 方法执行期间 | 方法内部 |

## 1.3 方法定义与调用

### 1.3.1 项目中的方法示例

```java
// 文件：backend/src/main/java/com/dormpower/model/Device.java

public class Device {
    
    // 【基础语法】Getter方法：获取id的值
    // public：公开访问
    // String：返回类型
    // getId：方法名
    // ()：无参数
    public String getId() {
        return this.id;  // this指当前对象
    }
    
    // 【基础语法】Setter方法：设置id的值
    // void：无返回值
    // (String id)：参数列表，接收一个String类型的参数
    public void setId(String id) {
        this.id = id;  // 将参数id赋值给实例变量id
    }
    
    // 【基础语法】带返回值的方法
    // boolean：返回true/false
    public boolean isOnline() {
        return this.online;
    }
    
    // 【基础语法】带参数的方法
    // 接收boolean参数，设置在线状态
    public void setOnline(boolean online) {
        this.online = online;
    }
}
```

### 1.3.2 方法签名解析

```java
// 【基础语法】方法签名 = 访问修饰符 + 返回类型 + 方法名 + 参数列表

// 示例1：public String getId()
// - public：访问修饰符（公开）
// - String：返回类型（字符串）
// - getId：方法名
// - ()：无参数

// 示例2：public void setId(String id)
// - public：访问修饰符
// - void：返回类型（无返回值）
// - setId：方法名
// - (String id)：参数列表（一个String参数）

// 示例3：public List<Device> findByRoom(String room)
// - public：访问修饰符
// - List<Device>：返回类型（Device列表）
// - findByRoom：方法名
// - (String room)：参数列表
```

## 1.4 流程控制

### 1.4.1 if条件判断

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    public Device updateDevice(String id, Device deviceDetails) {
        // 【基础语法】if条件判断：检查设备是否存在
        Device device = deviceRepository.findById(id).orElse(null);
        
        if (device == null) {
            // 【基础语法】if分支：设备不存在时抛出异常
            throw new ResourceNotFoundException("设备不存在: " + id);
        }
        
        // 【基础语法】if-else：判断名称是否为空
        if (deviceDetails.getName() != null && !deviceDetails.getName().isEmpty()) {
            // 名称不为空，更新名称
            device.setName(deviceDetails.getName());
        } else {
            // 名称为空，记录日志
            logger.warn("设备名称为空，跳过更新");
        }
        
        // 【基础语法】if-else if-else：多条件判断
        if (deviceDetails.getRoom() != null) {
            device.setRoom(deviceDetails.getRoom());
        } else if (deviceDetails.isOnline()) {
            device.setOnline(true);
        } else {
            logger.info("无有效更新字段");
        }
        
        return deviceRepository.save(device);
    }
}
```

### 1.4.2 for循环遍历

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    public List<Map<String, Object>> getDevices() {
        // 【基础语法】for-each循环：遍历设备列表
        List<Device> devices = deviceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 【基础语法】增强for循环语法：for (元素类型 变量 : 集合)
        for (Device device : devices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", device.getId());
            map.put("name", device.getName());
            map.put("online", device.isOnline());
            result.add(map);
        }
        
        return result;
    }
    
    public void batchUpdateStatus(List<String> deviceIds, boolean status) {
        // 【基础语法】传统for循环：带索引的遍历
        for (int i = 0; i < deviceIds.size(); i++) {
            String deviceId = deviceIds.get(i);
            Device device = deviceRepository.findById(deviceId).orElse(null);
            
            if (device != null) {
                device.setOnline(status);
                deviceRepository.save(device);
                logger.info("第{}个设备状态已更新", i + 1);
            }
        }
    }
}
```

### 1.4.3 switch多分支选择

```java
// 文件：backend/src/main/java/com/dormpower/service/CommandService.java

@Service
public class CommandService {
    
    public String executeCommand(String deviceId, String commandType) {
        // 【基础语法】switch-case：多分支选择
        switch (commandType) {
            case "TURN_ON":
                return turnOnDevice(deviceId);
            case "TURN_OFF":
                return turnOffDevice(deviceId);
            case "REBOOT":
                return rebootDevice(deviceId);
            case "STATUS":
                return getDeviceStatus(deviceId);
            default:
                throw new BusinessException("未知命令类型: " + commandType);
        }
    }
    
    // Java 12+ 增强switch表达式
    public String getCommandDescription(String commandType) {
        return switch (commandType) {
            case "TURN_ON" -> "打开设备";
            case "TURN_OFF" -> "关闭设备";
            case "REBOOT" -> "重启设备";
            default -> "未知命令";
        };
    }
}
```

## 1.5 字符串处理

### 1.5.1 字符串基础操作

```java
// 文件：backend/src/main/java/com/dormpower/util/JwtUtil.java

@Component
public class JwtUtil {
    
    // 【基础语法】字符串常量
    private static final String TOKEN_PREFIX = "Bearer ";
    
    public String extractToken(String authHeader) {
        // 【基础语法】字符串判空
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }
        
        // 【基础语法】字符串前缀判断
        if (authHeader.startsWith(TOKEN_PREFIX)) {
            // 【基础语法】字符串截取
            return authHeader.substring(TOKEN_PREFIX.length());
        }
        
        return authHeader;
    }
    
    public String generateDeviceTopic(String deviceId, String topicType) {
        // 【基础语法】字符串拼接（+运算符）
        String topic = "dorm/" + deviceId + "/" + topicType;
        
        // 【基础语法】字符串格式化
        String formatted = String.format("设备%s的主题是%s", deviceId, topic);
        
        // 【基础语法】StringBuilder高效拼接（循环中推荐）
        StringBuilder sb = new StringBuilder();
        sb.append("dorm").append("/")
          .append(deviceId).append("/")
          .append(topicType);
        return sb.toString();
    }
}
```

### 1.5.2 字符串常用方法

```java
// 【基础语法】字符串常用方法速查

String str = "  Device_001  ";

// 1. 去除空白
str.trim();                    // "Device_001"

// 2. 转大小写
str.toUpperCase();             // "  DEVICE_001  "
str.toLowerCase();             // "  device_001  "

// 3. 判断是否包含
str.contains("Device");        // true

// 4. 替换
str.replace("Device", "Dev");  // "  Dev_001  "

// 5. 分割
String[] parts = "A-101,ONLINE".split(",");  // ["A-101", "ONLINE"]

// 6. 长度
str.length();                  // 14（包含空格）

// 7. 取字符
str.charAt(2);                 // 'D'

// 8. 查找位置
str.indexOf("Device");         // 2

// 9. 判断是否为空
str.isEmpty();                 // false
str.isBlank();                 // false（Java 11+，判断空白字符）
```

## 1.6 日期与时间处理

### 1.6.1 项目中的日期工具类

```java
// 文件：backend/src/main/java/com/dormpower/util/DateUtil.java

public class DateUtil {
    
    // 【基础语法】静态常量：日期格式化器
    // static：属于类，所有对象共享
    // final：不可修改
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 【基础语法】静态方法：时间戳转LocalDateTime
     * static方法可以直接通过类名调用，不需要创建对象
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        // 【基础语法】方法链式调用
        return Instant.ofEpochMilli(timestamp)
                     .atZone(ZoneId.systemDefault())
                     .toLocalDateTime();
    }
    
    /**
     * 【基础语法】静态方法：LocalDateTime转时间戳
     */
    public static long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault())
                           .toInstant()
                           .toEpochMilli();
    }
    
    /**
     * 【基础语法】静态方法：时间戳转字符串
     */
    public static String timestampToString(long timestamp) {
        LocalDateTime localDateTime = timestampToLocalDateTime(timestamp);
        return formatter.format(localDateTime);
    }
}
```

### 1.6.2 日期时间使用示例

```java
// 【基础语法】日期时间处理示例

// 1. 获取当前时间戳（毫秒）
long currentTimestamp = System.currentTimeMillis();

// 2. 获取当前日期时间
LocalDateTime now = LocalDateTime.now();

// 3. 创建指定日期时间
LocalDateTime specificTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

// 4. 日期时间加减
LocalDateTime tomorrow = now.plusDays(1);      // 加1天
LocalDateTime lastWeek = now.minusWeeks(1);    // 减1周
LocalDateTime nextHour = now.plusHours(1);     // 加1小时

// 5. 日期时间比较
boolean isBefore = now.isBefore(tomorrow);     // true
boolean isAfter = now.isAfter(lastWeek);       // true

// 6. 格式化输出
String formatted = now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
// 结果："2024年01月15日"

// 7. 解析字符串为日期
LocalDateTime parsed = LocalDateTime.parse("2024-01-15 10:30:00", 
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
```

## 1.7 面向对象三大特性

### 1.7.1 封装（Encapsulation）

```java
// 文件：backend/src/main/java/com/dormpower/model/Device.java

// 【基础语法】封装：将数据（属性）和操作数据的方法绑定在一起
// 通过private隐藏内部细节，通过public方法暴露接口
@Entity
@Table(name = "devices")
public class Device {
    
    // 【封装】private：私有属性，外部无法直接访问
    @Id
    private String id;
    
    @NotNull
    private String name;
    
    @NotNull
    private boolean online;
    
    // 【封装】public Getter/Setter：受控的访问接口
    // 可以在方法中添加验证逻辑
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        // 【封装】可以在setter中添加校验
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        this.id = id;
    }
    
    // 【封装】只读属性：只有getter，没有setter
    // 创建时间一旦设置，不允许修改
    public long getCreatedAt() {
        return createdAt;
    }
    // 注意：没有setCreatedAt方法，外部无法修改
}
```

**封装的好处：**
1. 隐藏实现细节
2. 控制数据访问
3. 便于修改内部实现
4. 提高代码安全性

### 1.7.2 继承（Inheritance）

```java
// 文件：backend/src/main/java/com/dormpower/exception/BusinessException.java

// 【基础语法】继承：子类继承父类的属性和方法
// extends关键字表示继承
public class BusinessException extends RuntimeException {
    
    // 【继承】子类可以定义自己的属性
    private String errorCode;
    
    // 【继承】super调用父类构造方法
    public BusinessException(String message) {
        super(message);  // 调用RuntimeException的构造方法
    }
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    // 【继承】子类自己的方法
    public String getErrorCode() {
        return errorCode;
    }
}

// 【继承】另一个自定义异常
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);  // 调用父类构造方法
    }
}
```

**继承关系图：**
```
Throwable（所有异常的根类）
    └── Exception（受检异常）
            └── RuntimeException（运行时异常）
                    └── BusinessException（自定义业务异常）
                            └── ResourceNotFoundException（资源不存在异常）
```

### 1.7.3 多态（Polymorphism）

```java
// 文件：backend/src/main/java/com/dormpower/exception/GlobalExceptionHandler.java

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // 【多态】方法重载：同名方法，不同参数
    // 编译时多态（静态多态）
    
    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleException(ResourceNotFoundException ex) {
        logger.info("资源未找到: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    
    /**
     * 处理业务异常（重载方法）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleException(BusinessException ex) {
        logger.info("业务异常: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    
    /**
     * 处理所有其他异常（重载方法）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        logger.error("系统异常: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");
    }
    
    // 【多态】方法重写：子类重写父类方法
    // 运行时多态（动态多态）
    // 例如：toString()方法在每个类中都可以重写
}

// 【多态】接口实现示例
public interface NotificationSender {
    void send(String recipient, String message);
}

@Service
public class EmailNotificationSender implements NotificationSender {
    @Override  // 【多态】重写接口方法
    public void send(String recipient, String message) {
        // 发送邮件的实现
    }
}

@Service
public class SmsNotificationSender implements NotificationSender {
    @Override  // 【多态】重写接口方法
    public void send(String recipient, String message) {
        // 发送短信的实现
    }
}
```

## 1.8 本章小结

| 知识点 | 项目体现 | 关键代码位置 |
|--------|----------|--------------|
| 变量类型 | Device实体类 | `private String id;` |
| 方法定义 | Getter/Setter | `public String getId()` |
| 流程控制 | Service业务逻辑 | `if (device == null)` |
| 字符串处理 | JwtUtil工具类 | `authHeader.startsWith()` |
| 日期处理 | DateUtil工具类 | `timestampToLocalDateTime()` |
| 封装 | 实体类私有属性 | `private` + Getter/Setter |
| 继承 | 自定义异常 | `extends RuntimeException` |
| 多态 | 异常处理器 | `@ExceptionHandler` |

---

# 第2章：维度2 - 集合框架使用

## 2.1 本章学习目标

- 理解List、Map、Set在项目中的实际使用场景
- 掌握集合的遍历、筛选、转换操作
- 理解线程安全集合的使用

## 2.2 List列表

### 2.2.1 ArrayList使用

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【集合框架】ArrayList：有序、可重复、支持随机访问
     * 适用于：查询多、插入删除少的场景
     */
    public List<Map<String, Object>> getDevices() {
        // 【集合框架】创建ArrayList
        List<Device> devices = deviceRepository.findAll();
        
        // 【集合框架】创建结果列表
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 【集合框架】遍历并转换
        for (Device device : devices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", device.getId());
            map.put("name", device.getName());
            map.put("room", device.getRoom());
            map.put("online", device.isOnline());
            result.add(map);  // 【集合框架】添加到列表
        }
        
        return result;
    }
    
    /**
     * 【集合框架】List常用操作
     */
    public void listOperations() {
        // 1. 创建并初始化
        List<String> deviceIds = new ArrayList<>();
        
        // 2. 添加元素
        deviceIds.add("dev_001");
        deviceIds.add("dev_002");
        deviceIds.add("dev_003");
        
        // 3. 获取元素（索引从0开始）
        String first = deviceIds.get(0);  // "dev_001"
        
        // 4. 获取大小
        int size = deviceIds.size();  // 3
        
        // 5. 判断是否包含
        boolean contains = deviceIds.contains("dev_001");  // true
        
        // 6. 删除元素
        deviceIds.remove("dev_001");  // 按值删除
        deviceIds.remove(0);          // 按索引删除
        
        // 7. 清空列表
        deviceIds.clear();
        
        // 8. 判断是否为空
        boolean isEmpty = deviceIds.isEmpty();  // true
    }
}
```

### 2.2.2 LinkedList使用

```java
// 【集合框架】LinkedList：双向链表，插入删除快，查询慢
// 适用于：频繁插入删除的场景

@Service
public class CommandQueueService {
    
    // 【集合框架】使用LinkedList实现队列
    private LinkedList<CommandRequest> commandQueue = new LinkedList<>();
    
    /**
     * 添加命令到队列尾部
     */
    public void enqueue(CommandRequest command) {
        commandQueue.addLast(command);  // 尾部添加
    }
    
    /**
     * 从队列头部取出命令
     */
    public CommandRequest dequeue() {
        if (commandQueue.isEmpty()) {
            return null;
        }
        return commandQueue.removeFirst();  // 头部移除
    }
    
    /**
     * 查看队列头部（不移除）
     */
    public CommandRequest peek() {
        return commandQueue.peekFirst();
    }
}
```

## 2.3 Map映射

### 2.3.1 HashMap使用

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【集合框架】HashMap：键值对存储，key唯一，查询快O(1)
     * 适用于：快速查找、缓存场景
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
        
        // 【集合框架】创建HashMap存储设备详情
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", device.getId());           // key="id", value=device.getId()
        detail.put("name", device.getName());       // key="name", value=device.getName()
        detail.put("room", device.getRoom());
        detail.put("online", device.isOnline());
        detail.put("lastSeen", DateUtil.timestampToString(device.getLastSeenTs()));
        
        return detail;
    }
    
    /**
     * 【集合框架】Map常用操作
     */
    public void mapOperations() {
        // 1. 创建Map
        Map<String, Integer> roomDeviceCount = new HashMap<>();
        
        // 2. 添加键值对
        roomDeviceCount.put("A-101", 5);
        roomDeviceCount.put("A-102", 3);
        roomDeviceCount.put("B-201", 8);
        
        // 3. 获取值
        Integer count = roomDeviceCount.get("A-101");  // 5
        
        // 4. 判断key是否存在
        boolean hasKey = roomDeviceCount.containsKey("A-101");  // true
        
        // 5. 判断value是否存在
        boolean hasValue = roomDeviceCount.containsValue(5);    // true
        
        // 6. 获取所有key
        Set<String> rooms = roomDeviceCount.keySet();
        
        // 7. 获取所有value
        Collection<Integer> counts = roomDeviceCount.values();
        
        // 8. 获取所有键值对
        Set<Map.Entry<String, Integer>> entries = roomDeviceCount.entrySet();
        
        // 9. 遍历Map
        for (Map.Entry<String, Integer> entry : entries) {
            String room = entry.getKey();
            Integer cnt = entry.getValue();
            System.out.println(room + "有" + cnt + "个设备");
        }
        
        // 10. 删除键值对
        roomDeviceCount.remove("A-101");
        
        // 11. 获取大小
        int size = roomDeviceCount.size();
        
        // 12. 清空
        roomDeviceCount.clear();
    }
}
```

### 2.3.2 Map作为缓存

```java
// 文件：backend/src/main/java/com/dormpower/service/SimpleCacheService.java

@Service
public class SimpleCacheService {
    
    // 【集合框架】使用Map作为简单缓存
    // key: 设备ID, value: 设备对象
    private Map<String, Device> deviceCache = new HashMap<>();
    
    /**
     * 从缓存获取设备
     */
    public Device getFromCache(String deviceId) {
        return deviceCache.get(deviceId);
    }
    
    /**
     * 添加设备到缓存
     */
    public void putToCache(Device device) {
        deviceCache.put(device.getId(), device);
    }
    
    /**
     * 从缓存移除设备
     */
    public void removeFromCache(String deviceId) {
        deviceCache.remove(deviceId);
    }
    
    /**
     * 清空缓存
     */
    public void clearCache() {
        deviceCache.clear();
    }
}
```

## 2.4 Set集合

### 2.4.1 HashSet使用

```java
// 【集合框架】HashSet：无序、不重复、查询快
// 适用于：去重、判断是否存在

@Service
public class DeviceGroupService {
    
    /**
     * 【集合框架】使用Set去重
     */
    public Set<String> getAllRooms() {
        List<Device> devices = deviceRepository.findAll();
        
        // 【集合框架】使用HashSet去重
        Set<String> rooms = new HashSet<>();
        
        for (Device device : devices) {
            rooms.add(device.getRoom());  // 重复的房间号会自动去重
        }
        
        return rooms;
    }
    
    /**
     * 【集合框架】Set常用操作
     */
    public void setOperations() {
        // 1. 创建Set
        Set<String> onlineDevices = new HashSet<>();
        
        // 2. 添加元素
        onlineDevices.add("dev_001");
        onlineDevices.add("dev_002");
        onlineDevices.add("dev_001");  // 重复，不会被添加
        
        // 3. 大小（去重后的数量）
        int size = onlineDevices.size();  // 2
        
        // 4. 判断是否包含
        boolean contains = onlineDevices.contains("dev_001");  // true
        
        // 5. 删除元素
        onlineDevices.remove("dev_001");
        
        // 6. 遍历
        for (String deviceId : onlineDevices) {
            System.out.println(deviceId);
        }
    }
}
```

## 2.5 集合遍历与Stream操作

### 2.5.1 传统遍历方式

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【集合框架】List遍历的4种方式
     */
    public void listTraversal(List<Device> devices) {
        // 方式1：普通for循环（带索引）
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            System.out.println(i + ": " + device.getName());
        }
        
        // 方式2：增强for循环（推荐）
        for (Device device : devices) {
            System.out.println(device.getName());
        }
        
        // 方式3：Iterator迭代器
        Iterator<Device> iterator = devices.iterator();
        while (iterator.hasNext()) {
            Device device = iterator.next();
            System.out.println(device.getName());
        }
        
        // 方式4：forEach + Lambda（Java 8+）
        devices.forEach(device -> System.out.println(device.getName()));
    }
}
```

### 2.5.2 Stream流操作

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【集合框架】Stream流操作：筛选、映射、排序、聚合
     */
    public List<Device> getOnlineDevices(List<Device> devices) {
        // 【集合框架】filter：筛选在线设备
        return devices.stream()
            .filter(Device::isOnline)  // 只保留online=true的设备
            .collect(Collectors.toList());
    }
    
    /**
     * 【集合框架】Stream：映射转换
     */
    public List<String> getDeviceNames(List<Device> devices) {
        // 【集合框架】map：将Device映射为name字符串
        return devices.stream()
            .map(Device::getName)  // 提取每个设备的名称
            .collect(Collectors.toList());
    }
    
    /**
     * 【集合框架】Stream：排序
     */
    public List<Device> getSortedDevices(List<Device> devices) {
        // 【集合框架】sorted：按创建时间排序
        return devices.stream()
            .sorted(Comparator.comparing(Device::getCreatedAt).reversed())  // 降序
            .collect(Collectors.toList());
    }
    
    /**
     * 【集合框架】Stream：分组
     */
    public Map<String, List<Device>> groupByRoom(List<Device> devices) {
        // 【集合框架】groupingBy：按房间分组
        return devices.stream()
            .collect(Collectors.groupingBy(Device::getRoom));
        // 结果：{A-101=[dev1, dev2], A-102=[dev3]}
    }
    
    /**
     * 【集合框架】Stream：统计
     */
    public long countOnlineDevices(List<Device> devices) {
        // 【集合框架】count：统计数量
        return devices.stream()
            .filter(Device::isOnline)
            .count();
    }
    
    /**
     * 【集合框架】Stream：查找
     */
    public Optional<Device> findFirstOnline(List<Device> devices) {
        // 【集合框架】findFirst：查找第一个符合条件的
        return devices.stream()
            .filter(Device::isOnline)
            .findFirst();
    }
}
```

## 2.6 线程安全集合

### 2.6.1 ConcurrentHashMap

```java
// 文件：backend/src/main/java/com/dormpower/service/SimpleCacheService.java

@Service
public class SimpleCacheService {
    
    // 【集合框架】ConcurrentHashMap：线程安全的HashMap
    // 适用于：多线程环境下的缓存
    private ConcurrentHashMap<String, Device> deviceCache = new ConcurrentHashMap<>();
    
    /**
     * 线程安全的获取操作
     */
    public Device getFromCache(String deviceId) {
        return deviceCache.get(deviceId);
    }
    
    /**
     * 线程安全的添加操作
     */
    public void putToCache(Device device) {
        deviceCache.put(device.getId(), device);
    }
    
    /**
     * 【集合框架】原子操作：如果不存在则添加
     */
    public Device putIfAbsent(String deviceId, Device device) {
        return deviceCache.putIfAbsent(deviceId, device);
    }
    
    /**
     * 【集合框架】原子操作：计算并更新
     */
    public Device computeIfAbsent(String deviceId, Function<String, Device> mappingFunction) {
        return deviceCache.computeIfAbsent(deviceId, mappingFunction);
    }
}
```

### 2.6.2 CopyOnWriteArrayList

```java
// 【集合框架】CopyOnWriteArrayList：线程安全的List
// 读多写少场景，写操作会复制新数组

@Service
public class DeviceStatusListener {
    
    // 【集合框架】存储监听器列表，线程安全
    private CopyOnWriteArrayList<StatusChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    /**
     * 添加监听器
     */
    public void addListener(StatusChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除监听器
     */
    public void removeListener(StatusChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知所有监听器
     */
    public void notifyListeners(String deviceId, boolean newStatus) {
        // 【集合框架】遍历是线程安全的
        for (StatusChangeListener listener : listeners) {
            listener.onStatusChange(deviceId, newStatus);
        }
    }
}
```

## 2.7 集合选择指南

| 集合类型 | 特点 | 适用场景 | 项目示例 |
|----------|------|----------|----------|
| ArrayList | 有序、可重复、查询快 | 查询多、插入删除少 | 设备列表查询 |
| LinkedList | 有序、插入删除快 | 频繁插入删除 | 命令队列 |
| HashMap | 键值对、查询快 | 缓存、快速查找 | 设备缓存 |
| HashSet | 无序、不重复 | 去重、判断存在 | 房间号去重 |
| ConcurrentHashMap | 线程安全Map | 多线程缓存 | 线程安全设备缓存 |
| CopyOnWriteArrayList | 线程安全List | 读多写少 | 监听器列表 |

## 2.8 本章小结

| 知识点 | 项目体现 | 关键代码 |
|--------|----------|----------|
| ArrayList | 查询结果存储 | `List<Device> devices = new ArrayList<>()` |
| HashMap | 设备详情组装 | `Map<String, Object> detail = new HashMap<>()` |
| HashSet | 房间号去重 | `Set<String> rooms = new HashSet<>()` |
| Stream | 筛选在线设备 | `devices.stream().filter(Device::isOnline)` |
| ConcurrentHashMap | 线程安全缓存 | `ConcurrentHashMap<String, Device>` |

---

# 第3章：维度3 - 基础配置

## 3.1 本章学习目标

- 理解配置文件的作用和格式（YAML/Properties）
- 掌握配置类中常量的定义和使用
- 学会将硬编码配置抽离到配置文件

## 3.2 YAML配置文件

### 3.2.1 application.yml基础配置

```yaml
# 文件：backend/src/main/resources/application.yml

# 【基础配置】Spring应用基础配置
spring:
  application:
    name: dorm-power-backend  # 应用名称
  
  # 【基础配置】数据源配置
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power  # 数据库连接URL
    username: rongx                                       # 数据库用户名
    password:                                             # 数据库密码
    driver-class-name: org.postgresql.Driver             # 数据库驱动
    
    # 【基础配置】连接池配置（HikariCP）
    hikari:
      minimum-idle: 1                # 最小空闲连接数
      maximum-pool-size: 5           # 最大连接数
      idle-timeout: 60000            # 空闲连接超时时间（毫秒）
      pool-name: DormPowerHikariPool # 连接池名称
      max-lifetime: 1800000          # 连接最大生命周期
      connection-timeout: 30000      # 连接超时时间
      leak-detection-threshold: 120000  # 连接泄漏检测阈值

# 【基础配置】服务器配置
server:
  port: 8000  # 服务端口

# 【基础配置】MQTT配置
mqtt:
  enabled: true
  broker-url: tcp://localhost:1883
  client-id: dorm-power-backend
  username: admin
  password: admin
  topic-prefix: dorm

# 【基础配置】安全配置
security:
  jwt:
    secret: your-secret-key  # JWT密钥
    expiration: 86400000     # Token过期时间（毫秒）

# 【基础配置】日志配置
logging:
  level:
    root: warn              # 根日志级别
    com.dormpower: info     # 项目包日志级别
```

### 3.2.2 多环境配置

```yaml
# 文件：backend/src/main/resources/application-dev.yml
# 【基础配置】开发环境配置

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dorm_power_dev
    username: dev
    password: dev123
  
  jpa:
    show-sql: true  # 开发环境显示SQL
    properties:
      hibernate:
        format_sql: true  # 格式化SQL

logging:
  level:
    com.dormpower: debug  # 开发环境DEBUG级别
```

```yaml
# 文件：backend/src/main/resources/application-prod.yml
# 【基础配置】生产环境配置

spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/dorm_power
    username: ${DB_USERNAME}  # 从环境变量读取
    password: ${DB_PASSWORD}
  
  jpa:
    show-sql: false  # 生产环境不显示SQL

logging:
  level:
    com.dormpower: warn  # 生产环境WARN级别
```

## 3.3 配置类与@ConfigurationProperties

### 3.3.1 配置属性类

```java
// 文件：backend/src/main/java/com/dormpower/config/MqttConfig.java

@Configuration
@ConfigurationProperties(prefix = "mqtt")  // 【基础配置】绑定mqtt前缀的配置
@Data  // Lombok自动生成getter/setter
public class MqttConfig {
    
    // 【基础配置】属性名与配置项对应
    // mqtt.enabled -> enabled
    private boolean enabled;
    
    // mqtt.broker-url -> brokerUrl
    private String brokerUrl;
    
    // mqtt.client-id -> clientId
    private String clientId;
    
    private String username;
    private String password;
    private String topicPrefix;
    
    // 【基础配置】嵌套配置对象
    private Topics topics;
    
    // 【基础配置】嵌套配置类
    @Data
    public static class Topics {
        private String deviceStatus;
        private String deviceTelemetry;
        private String deviceCommand;
        private String deviceAck;
        private String deviceEvent;
    }
}
```

### 3.3.2 使用配置类

```java
// 文件：backend/src/main/java/com/dormpower/mqtt/MqttBridge.java

@Service
public class MqttBridge {
    
    // 【基础配置】注入配置类
    @Autowired
    private MqttConfig mqttConfig;
    
    @PostConstruct
    public void init() {
        // 【基础配置】使用配置值
        if (mqttConfig.isEnabled()) {
            connectToBroker(mqttConfig.getBrokerUrl());
        }
    }
    
    public void publish(String deviceId, String message) {
        // 【基础配置】使用配置的主题前缀
        String topic = mqttConfig.getTopicPrefix() + "/" + deviceId + "/status";
        publishToMqtt(topic, message);
    }
}
```

## 3.4 常量定义

### 3.4.1 常量类

```java
// 文件：backend/src/main/java/com/dormpower/constant/SystemConstants.java

/**
 * 【基础配置】系统常量定义
 */
public final class SystemConstants {
    
    // 【基础配置】私有构造方法，防止实例化
    private SystemConstants() {}
    
    // 【基础配置】JWT相关常量
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_HEADER = "Authorization";
    
    // 【基础配置】时间相关常量（毫秒）
    public static final long ONE_SECOND = 1000L;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    public static final long ONE_DAY = 24 * ONE_HOUR;
    
    // 【基础配置】分页默认参数
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    
    // 【基础配置】设备相关常量
    public static final int DEVICE_OFFLINE_THRESHOLD_MINUTES = 5;
    public static final int MAX_DEVICE_NAME_LENGTH = 50;
    
    // 【基础配置】响应状态码
    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
}
```

### 3.4.2 枚举类型

```java
// 文件：backend/src/main/java/com/dormpower/constant/DeviceStatus.java

/**
 * 【基础配置】设备状态枚举
 */
public enum DeviceStatus {
    ONLINE("在线", true),
    OFFLINE("离线", false),
    MAINTENANCE("维护中", false),
    ERROR("故障", false);
    
    // 【基础配置】枚举属性
    private final String description;
    private final boolean active;
    
    // 【基础配置】枚举构造方法
    DeviceStatus(String description, boolean active) {
        this.description = description;
        this.active = active;
    }
    
    // 【基础配置】getter方法
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return active;
    }
    
    // 【基础配置】根据布尔值获取枚举
    public static DeviceStatus fromBoolean(boolean online) {
        return online ? ONLINE : OFFLINE;
    }
}
```

### 3.4.3 使用常量

```java
// 文件：backend/src/main/java/com/dormpower/util/JwtUtil.java

@Component
public class JwtUtil {
    
    /**
     * 【基础配置】使用常量
     */
    public String extractToken(String authHeader) {
        // 【基础配置】使用常量而不是硬编码字符串
        if (authHeader != null && authHeader.startsWith(SystemConstants.TOKEN_PREFIX)) {
            return authHeader.substring(SystemConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}

// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    public boolean isDeviceOffline(Device device) {
        long lastSeen = device.getLastSeenTs();
        long now = System.currentTimeMillis();
        
        // 【基础配置】使用常量计算离线阈值
        long offlineThreshold = SystemConstants.DEVICE_OFFLINE_THRESHOLD_MINUTES 
                               * SystemConstants.ONE_MINUTE;
        
        return (now - lastSeen) > offlineThreshold;
    }
}
```

## 3.5 环境变量与配置优先级

### 3.5.1 配置优先级（从高到低）

```
1. 命令行参数：--server.port=9000
2. JVM系统属性：-Dserver.port=9000
3. 环境变量：SERVER_PORT=9000
4. application-{profile}.yml
5. application.yml
6. @PropertySource
7. 默认值
```

### 3.5.2 使用环境变量

```yaml
# application.yml

spring:
  datasource:
    # 【基础配置】从环境变量读取，默认值localhost
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/dorm_power
    username: ${DB_USERNAME:rongx}
    password: ${DB_PASSWORD:}

server:
  # 【基础配置】从环境变量读取，默认值8000
  port: ${SERVER_PORT:8000}

security:
  jwt:
    # 【基础配置】敏感信息从环境变量读取
    secret: ${JWT_SECRET:default-secret-key}
```

## 3.6 本章小结

| 知识点 | 项目体现 | 关键代码 |
|--------|----------|----------|
| YAML配置 | application.yml | `spring.datasource.url` |
| 配置类 | MqttConfig | `@ConfigurationProperties(prefix = "mqtt")` |
| 常量类 | SystemConstants | `public static final String TOKEN_PREFIX` |
| 枚举 | DeviceStatus | `ONLINE("在线", true)` |
| 环境变量 | 敏感配置 | `${DB_PASSWORD:}` |

---

# 第5章：维度5 - Spring/SpringBoot基础

## 5.1 本章学习目标

- 理解Spring IOC和DI的核心概念
- 掌握常用注解的使用（@Controller, @Service, @Repository等）
- 理解SpringBoot自动配置原理
- 学会自定义配置类

## 5.2 Spring IOC容器

### 5.2.1 IOC概念

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

### 5.2.2 Bean的生命周期

```java
// 【Spring基础】Bean生命周期示例

@Service
public class DeviceService {
    
    // 【Spring基础】@PostConstruct：Bean创建后执行
    @PostConstruct
    public void init() {
        System.out.println("DeviceService初始化完成");
        // 可以在这里加载缓存、建立连接等
    }
    
    // 【Spring基础】@PreDestroy：Bean销毁前执行
    @PreDestroy
    public void destroy() {
        System.out.println("DeviceService即将销毁");
        // 可以在这里释放资源、关闭连接等
    }
}
```

## 5.3 依赖注入（DI）

### 5.3.1 三种注入方式

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    // 【Spring基础】方式1：字段注入（最常用，简单）
    @Autowired
    private DeviceRepository deviceRepository;
    
    // 【Spring基础】方式2：构造器注入（推荐，便于测试）
    private final DeviceRepository deviceRepository;
    
    @Autowired  // Spring 4.3+ 可以省略
    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    
    // 【Spring基础】方式3：Setter注入（可选依赖）
    private DeviceCache deviceCache;
    
    @Autowired(required = false)
    public void setDeviceCache(DeviceCache deviceCache) {
        this.deviceCache = deviceCache;
    }
}
```

### 5.3.2 使用@RequiredArgsConstructor（推荐）

```java
// 【Spring基础】Lombok + 构造器注入（最简洁的方式）

@Service
@RequiredArgsConstructor  // 【Spring基础】生成包含final字段的构造器
public class DeviceService {
    
    // 【Spring基础】final字段自动注入
    private final DeviceRepository deviceRepository;
    private final DeviceCache deviceCache;
    private final MqttBridge mqttBridge;
    
    // 无需写构造器，Lombok自动生成
    // 无需写@Autowired，构造器注入自动生效
}
```

## 5.4 常用注解详解

### 5.4.1 组件注解

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

### 5.4.2 依赖注入注解

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

### 5.4.3 作用域注解

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

## 5.5 SpringBoot自动配置

### 5.5.1 启动类

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

### 5.5.2 自动配置原理

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

### 5.5.3 自定义Starter

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

## 5.6 配置类详解

### 5.6.1 @Configuration配置类

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

### 5.6.2 WebMvc配置

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

## 5.7 AOP面向切面编程

### 5.7.1 日志切面示例

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

## 5.8 本章小结

| 知识点 | 项目体现 | 关键注解 |
|--------|----------|----------|
| IOC | Bean由容器管理 | @Component, @Service |
| DI | 依赖自动注入 | @Autowired |
| 配置类 | 各种Config类 | @Configuration, @Bean |
| 自动配置 | SpringBoot启动 | @SpringBootApplication |
| AOP | 接口日志记录 | @Aspect, @Around |

---

# 第6章：维度6 - 异常处理&日志

## 6.1 本章学习目标

- 掌握try-catch-finally的用法
- 学会自定义异常类
- 理解全局异常处理机制
- 掌握日志框架的使用

## 6.2 异常处理基础

### 6.2.1 try-catch-finally

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    /**
     * 【异常处理】try-catch：捕获并处理异常
     */
    public Device getDeviceSafe(String deviceId) {
        try {
            // 【异常处理】可能抛出异常的代码
            return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
        } catch (ResourceNotFoundException e) {
            // 【异常处理】捕获特定异常，进行处理
            logger.warn("设备未找到: {}", deviceId);
            return null;
        } catch (Exception e) {
            // 【异常处理】捕获其他异常
            logger.error("查询设备异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 【异常处理】try-catch-finally：无论是否异常都执行finally
     */
    public void processWithResource(String deviceId) {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            // 使用连接执行操作
            doSomething(connection, deviceId);
        } catch (SQLException e) {
            logger.error("数据库操作失败: {}", e.getMessage());
            throw new BusinessException("数据库操作失败");
        } finally {
            // 【异常处理】finally：无论是否异常都执行
            // 用于释放资源
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.error("关闭连接失败: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 【异常处理】try-with-resources：自动关闭资源（Java 7+）
     */
    public void processWithResourceModern(String deviceId) {
        // 【异常处理】自动关闭实现了AutoCloseable接口的资源
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM devices")) {
            
            ResultSet rs = stmt.executeQuery();
            // 处理结果
            
        } catch (SQLException e) {
            logger.error("数据库操作失败: {}", e.getMessage());
            throw new BusinessException("数据库操作失败");
        }
        // 自动调用connection.close()和stmt.close()
    }
}
```

### 6.2.2 抛出异常

```java
/**
 * 【异常处理】throw：主动抛出异常
 */
@Service
public class DeviceService {
    
    public Device createDevice(Device device) {
        // 【异常处理】参数校验，不满足条件抛出异常
        if (device.getId() == null || device.getId().isEmpty()) {
            throw new IllegalArgumentException("设备ID不能为空");
        }
        
        // 【异常处理】业务规则校验
        if (deviceRepository.existsById(device.getId())) {
            throw new BusinessException("设备ID已存在: " + device.getId());
        }
        
        return deviceRepository.save(device);
    }
}
```

## 6.3 自定义异常

### 6.3.1 业务异常基类

```java
// 文件：backend/src/main/java/com/dormpower/exception/BusinessException.java

/**
 * 【异常处理】自定义业务异常
 * 继承RuntimeException，无需强制捕获
 */
public class BusinessException extends RuntimeException {
    
    // 【异常处理】错误码
    private String errorCode;
    
    /**
     * 【异常处理】构造方法1：只传消息
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    /**
     * 【异常处理】构造方法2：传错误码和消息
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 【异常处理】构造方法3：传消息和原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

### 6.3.2 具体业务异常

```java
// 文件：backend/src/main/java/com/dormpower/exception/ResourceNotFoundException.java

/**
 * 【异常处理】资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super("NOT_FOUND", 
              String.format("%s not found with %s : '%s'", 
                          resourceName, fieldName, fieldValue));
    }
    
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}

// 文件：backend/src/main/java/com/dormpower/exception/AuthenticationException.java

/**
 * 【异常处理】认证异常
 */
public class AuthenticationException extends BusinessException {
    
    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }
}
```

## 6.4 全局异常处理

### 6.4.1 @RestControllerAdvice

```java
// 文件：backend/src/main/java/com/dormpower/exception/GlobalExceptionHandler.java

/**
 * 【异常处理】@RestControllerAdvice：全局异常处理器
 * 统一处理所有Controller抛出的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 【异常处理】处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        logger.info("参数验证异常: {}", ex.getMessage());
        
        // 收集所有验证错误
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "参数验证失败",
            errors,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.info("资源未找到异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        logger.info("业务异常: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 【异常处理】处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        logger.error("系统异常: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "系统内部错误，请联系管理员",
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

/**
 * 【异常处理】错误响应对象
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;           // HTTP状态码
    private String message;       // 错误消息
    private Map<String, String> errors;  // 详细错误信息
    private LocalDateTime timestamp;     // 时间戳
}
```

## 6.5 日志框架使用

### 6.5.1 SLF4J + Logback

```java
// 【日志使用】SLF4J日志门面 + Logback实现（SpringBoot默认）

@Service
public class DeviceService {
    
    // 【日志使用】创建Logger实例
    // 通常使用类名作为logger名称
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    public Device getDeviceById(String deviceId) {
        // 【日志使用】DEBUG级别：详细的调试信息
        logger.debug("开始查询设备: {}", deviceId);
        
        try {
            Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
            
            // 【日志使用】INFO级别：正常的业务信息
            logger.info("成功查询设备: {}, 名称: {}", deviceId, device.getName());
            
            return device;
            
        } catch (ResourceNotFoundException e) {
            // 【日志使用】WARN级别：警告信息，不影响系统运行
            logger.warn("设备未找到: {}", deviceId);
            throw e;
            
        } catch (Exception e) {
            // 【日志使用】ERROR级别：错误信息，需要处理
            // 最后一个参数传入异常对象，会打印堆栈
            logger.error("查询设备失败: {}, 异常: {}", deviceId, e.getMessage(), e);
            throw new BusinessException("查询设备失败", e);
        }
    }
}
```

### 6.5.2 日志级别

```yaml
# application.yml 日志配置

logging:
  level:
    # 【日志使用】根日志级别
    root: warn
    
    # 【日志使用】指定包的日志级别
    com.dormpower: info
    com.dormpower.controller: debug
    com.dormpower.service: info
    
    # 【日志使用】框架日志级别
    org.springframework: warn
    org.hibernate: error
```

**日志级别（从低到高）：**
| 级别 | 使用场景 | 示例 |
|------|----------|------|
| TRACE | 最详细的跟踪信息 | `logger.trace("进入方法")` |
| DEBUG | 调试信息 | `logger.debug("变量值: {}", value)` |
| INFO | 正常业务信息 | `logger.info("操作成功")` |
| WARN | 警告信息 | `logger.warn("参数为空")` |
| ERROR | 错误信息 | `logger.error("操作失败", e)` |

### 6.5.3 日志占位符

```java
@Service
public class DeviceService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
    
    public void logExamples() {
        String deviceId = "dev_001";
        String deviceName = "空调";
        int count = 10;
        
        // 【日志使用】使用{}占位符，避免字符串拼接
        logger.info("设备ID: {}, 设备名称: {}", deviceId, deviceName);
        
        // 【日志使用】多个占位符
        logger.info("查询到{}个设备，第一个设备ID: {}", count, deviceId);
        
        // 【日志使用】条件日志（避免不必要的计算）
        if (logger.isDebugEnabled()) {
            // 复杂的日志内容只在DEBUG级别计算
            logger.debug("复杂对象: {}", expensiveOperation());
        }
    }
    
    private String expensiveOperation() {
        // 耗时操作
        return "result";
    }
}
```

## 6.6 本章小结

| 知识点 | 项目体现 | 关键代码 |
|--------|----------|----------|
| try-catch | Service异常处理 | `try { ... } catch (Exception e)` |
| 自定义异常 | BusinessException | `extends RuntimeException` |
| 全局异常处理 | GlobalExceptionHandler | `@RestControllerAdvice` |
| 日志使用 | 各Service类 | `LoggerFactory.getLogger()` |

---

# 第8章：维度8 - JVM基础调优

## 8.1 本章学习目标

- 理解JVM内存模型基础
- 掌握JVM启动参数配置
- 学会避免常见内存问题

## 8.2 JVM内存模型简介

```
【JVM基础】JVM内存区域划分

┌─────────────────────────────────────────┐
│              堆内存（Heap）              │
│  ┌──────────────┐    ┌──────────────┐  │
│  │   年轻代      │    │    老年代     │  │
│  │  (Young)     │ -> │   (Old)      │  │
│  │  - Eden      │    │              │  │
│  │  - Survivor  │    │              │  │
│  └──────────────┘    └──────────────┘  │
│         存放对象实例                      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│           元空间（Metaspace）            │
│         存放类信息、常量池                │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│            栈内存（Stack）               │
│         每个线程私有，存放局部变量         │
└─────────────────────────────────────────┘
```

## 8.3 JVM启动参数配置

### 8.3.1 启动脚本配置

```bash
#!/bin/bash
# 【JVM基础】项目启动脚本 start.sh

# 堆内存设置
JAVA_OPTS="-Xms512m -Xmx1024m"

# 元空间设置
JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# 垃圾收集器设置（G1收集器）
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC"

# GC日志（Java 9+）
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=logs/gc.log:time:filecount=5,filesize=10m"

# OOM时生成堆转储
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/heapdump.hprof"

# 启动应用
java $JAVA_OPTS -jar dorm-power-backend.jar
```

### 8.3.2 参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `-Xms` | 初始堆内存 | `-Xms512m`（512MB） |
| `-Xmx` | 最大堆内存 | `-Xmx1024m`（1GB） |
| `-Xmn` | 年轻代大小 | `-Xmn256m` |
| `-XX:MetaspaceSize` | 初始元空间 | `-XX:MetaspaceSize=128m` |
| `-XX:MaxMetaspaceSize` | 最大元空间 | `-XX:MaxMetaspaceSize=256m` |
| `-XX:+UseG1GC` | 使用G1垃圾收集器 | - |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM时生成堆转储 | - |

## 8.4 内存溢出预防

### 8.4.1 集合判空

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【JVM基础】避免空指针和内存浪费
     */
    public List<Device> getDevicesByRoom(String room) {
        // 【JVM基础】参数判空，避免后续处理空值
        if (room == null || room.isEmpty()) {
            // 返回空列表而不是null，避免调用方判空
            return Collections.emptyList();
        }
        
        List<Device> devices = deviceRepository.findByRoom(room);
        
        // 【JVM基础】结果判空
        if (devices == null || devices.isEmpty()) {
            return Collections.emptyList();
        }
        
        return devices;
    }
}
```

### 8.4.2 大对象处理

```java
@Service
public class DeviceDataExportService {
    
    private static final int BATCH_SIZE = 1000;
    
    /**
     * 【JVM基础】分页处理大数据量，避免内存溢出
     */
    public void exportAllDevices() {
        int page = 0;
        List<Device> batch;
        
        // 【JVM基础】分批处理，不一次性加载所有数据
        do {
            // 每次只查询1000条
            batch = deviceRepository.findAll(PageRequest.of(page, BATCH_SIZE)).getContent();
            
            // 处理这批数据
            processBatch(batch);
            
            // 手动触发GC建议（通常不需要）
            // batch = null;  // 帮助GC
            
            page++;
        } while (!batch.isEmpty());
    }
    
    private void processBatch(List<Device> devices) {
        // 处理数据...
    }
}
```

### 8.4.3 资源释放

```java
@Service
public class FileService {
    
    /**
     * 【JVM基础】及时关闭资源，避免内存泄漏
     */
    public void processFile(String filePath) {
        // 【JVM基础】使用try-with-resources自动关闭
        try (InputStream is = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
            
        } catch (IOException e) {
            logger.error("文件处理失败: {}", e.getMessage());
        }
        // 自动调用close()，释放文件句柄
    }
}
```

## 8.5 监控与诊断

### 8.5.1 Actuator端点

```yaml
# application.yml

# 【JVM基础】Actuator监控配置
management:
  endpoints:
    web:
      exposure:
        # 暴露的端点
        include: health,info,metrics,env,heapdump,threaddump
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
```

**常用端点：**
- `/actuator/health`：健康检查
- `/actuator/metrics`：运行时指标
- `/actuator/metrics/jvm.memory.used`：JVM内存使用
- `/actuator/heapdump`：堆转储文件
- `/actuator/threaddump`：线程转储

### 8.5.2 内存使用监控

```java
@Service
public class SystemMonitorService {
    
    /**
     * 【JVM基础】获取JVM内存信息
     */
    public Map<String, Object> getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> memoryInfo = new HashMap<>();
        
        // 总内存
        memoryInfo.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + "MB");
        
        // 空闲内存
        memoryInfo.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + "MB");
        
        // 已用内存
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        memoryInfo.put("usedMemory", usedMemory + "MB");
        
        // 最大内存
        memoryInfo.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + "MB");
        
        return memoryInfo;
    }
}
```

## 8.6 本章小结

| 知识点 | 项目体现 | 关键配置 |
|--------|----------|----------|
| JVM参数 | 启动脚本 | `-Xms512m -Xmx1024m` |
| 内存优化 | 分页查询 | `PageRequest.of(page, size)` |
| 资源释放 | try-with-resources | `try (InputStream is = ...)` |
| 监控 | Actuator端点 | `/actuator/metrics` |

---

# 附录：8维度学习路径图

```
小白学习路径（按顺序）

维度1：Java基础语法
    ├── 变量类型、方法定义
    ├── 流程控制（if/for）
    ├── 字符串、日期处理
    └── 封装、继承、多态
    
维度2：集合框架使用
    ├── List/Map/Set基础
    ├── 集合遍历与操作
    └── 线程安全集合
    
维度3：基础配置
    ├── YAML配置文件
    ├── 配置类与常量
    └── 环境变量
    
维度4：核心CRUD实战 ← 已完成
    ├── 实体层、数据层
    ├── Service业务层
    └── Controller接口层
    
维度5：Spring/SpringBoot基础
    ├── IOC与DI
    ├── 常用注解
    └── 自动配置原理
    
维度6：异常处理&日志
    ├── try-catch-finally
    ├── 自定义异常
    ├── 全局异常处理
    └── 日志框架使用
    
维度7：JUC并发编程 ← 已完成
    ├── 线程池
    ├── 锁机制
    └── 并发集合
    
维度8：JVM基础调优
    ├── JVM内存模型
    ├── 启动参数配置
    └── 内存溢出预防
```

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
**维度4（CRUD）和维度7（JUC）详见独立文档。**