# 宿舍电源管理系统 - WebSocket 使用文档

## 目录
- [1. 项目为什么使用 WebSocket](#1-项目为什么使用-websocket)
- [2. 项目如何使用 WebSocket](#2-项目如何使用-websocket)
- [3. 核心功能实现](#3-核心功能实现)
- [4. 实时消息推送流程](#4-实时消息推送流程)
- [5. 面试要点总结](#5-面试要点总结)

---

## 1. 项目为什么使用 WebSocket

### 1.1 业务需求分析

宿舍电源管理系统是一个典型的 IoT（物联网）应用场景，具有以下实时性需求：

| 业务需求 | 具体场景 | 传统方案 | WebSocket 方案 |
|----------|----------|-----------|----------------|
| **设备状态实时推送** | 设备开关状态、在线离线状态变化 | 轮询（延迟大、资源浪费） | 毫秒级推送 |
| **遥测数据实时显示** | 电压、电流、功率实时更新 | 轮询（数据量大、服务器压力大） | 实时推送 |
| **命令执行结果** | 控制命令下发后立即反馈结果 | 轮询或刷新（体验差） | 即时推送 |
| **告警实时通知** | 异常用电、设备故障告警 | 邮件/短信（延迟高） | 即时推送 |
| **多人协作** | 多个管理员同时管理设备 | 刷新获取最新状态 | 自动同步 |

### 1.2 技术选型对比

| 对比维度 | HTTP 轮询 | Server-Sent Events | WebSocket |
|----------|-----------|-------------------|-----------|
| **实时性** | 秒级延迟 | 实时 | 实时 |
| **服务器推送** | 不支持 | 支持 | 支持 |
| **双向通信** | 不支持 | 单向 | 全双工 |
| **连接复用** | 每次新建 | 复用 | 复用 |
| **浏览器支持** | 全部 | 现代浏览器 | 全部 |
| **适用场景** | 简单轮询 | 服务器推送 | 实时双向通信 |

### 1.3 WebSocket 在项目中的核心价值

#### 1.3.1 毫秒级实时推送

```
┌─────────────────────────────────────────────────────────────┐
│                   实时数据推送架构                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  IoT设备 ──MQTT──> 后端服务 ──WebSocket──> 前端           │
│                      │                                      │
│                      ├── 设备状态更新                        │
│                      ├── 遥测数据推送                       │
│                      ├── 命令结果通知                       │
│                      └── 告警通知                          │
│                                                             │
│  推送延迟：< 100ms                                         │
│  并发连接：500+                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 1.3.2 设备订阅机制

```
┌─────────────────────────────────────────────────────────────┐
│                   设备订阅机制                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端 A ──────────────────────┐                         │
│  订阅设备：device_001            │                         │
│                                   │                         │
│  客户端 B ──────────────────────┼──> WebSocket 服务器    │
│  订阅设备：device_001, device_002 │                         │
│                                   │                         │
│  客户端 C ──────────────────────┘                         │
│  订阅设备：device_003            │                         │
│                                   │                         │
│  当 device_001 状态变化：        │                         │
│  ──────────────────────────────────> 推送给 A、B           │
│  ──────────────────────────────────> 推送给 A、B           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 项目如何使用 WebSocket

### 2.1 核心组件

#### 2.1.1 WebSocket 配置类

**文件**：[WebSocketConfig.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/config/WebSocketConfig.java)

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "/ws")
                .setAllowedOrigins("*");
    }
}
```

**设计要点**：
1. **端点配置**：`/ws` 作为 WebSocket 端点
2. **跨域支持**：允许所有来源（开发环境）
3. **自定义处理器**：使用 `TextWebSocketHandler`

#### 2.1.2 WebSocket 消息处理器

**文件**：[WebSocketHandler.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/websocket/WebSocketHandler.java)

```java
public class WebSocketHandler extends TextWebSocketHandler {

    private final WebSocketManager webSocketManager = WebSocketManager.getInstance();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        webSocketManager.addSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        Map<String, Object> request = objectMapper.readValue(payload, Map.class);
        String type = (String) request.get("type");

        switch (type) {
            case "subscribe" -> handleSubscribe(session, request);
            case "unsubscribe" -> handleUnsubscribe(session, request);
            case "ping" -> handlePing(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        webSocketManager.removeSession(session);
    }
}
```

**设计要点**：
1. **连接管理**：连接建立和关闭时管理会话
2. **消息路由**：根据消息类型分发处理
3. **订阅机制**：支持设备订阅/取消订阅
4. **心跳支持**：支持 Ping-Pong 心跳

### 2.2 WebSocket 管理器

**文件**：[WebSocketManager.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/websocket/WebSocketManager.java)

```java
public class WebSocketManager {

    private static volatile WebSocketManager instance;

    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<WebSocketSession, Set<String>> sessionDeviceSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> deviceSubscribers = new ConcurrentHashMap<>();

    private final ThreadPoolExecutor sendExecutor;

    public void addSession(WebSocketSession session) {
        sessions.add(session);
        sessionDeviceSubscriptions.put(session, ConcurrentHashMap.newKeySet());
    }

    public void subscribeDevice(WebSocketSession session, String deviceId) {
        sessionDeviceSubscriptions.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet()).add(deviceId);
        deviceSubscribers.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void sendToDeviceSubscribers(String deviceId, String message) {
        Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
        if (subscribers != null) {
            subscribers.forEach(session -> sendSync(session, message));
        }
    }

    public void broadcast(String message) {
        sessions.forEach(session -> sendSync(session, message));
    }
}
```

**设计要点**：
1. **单例模式**：全局唯一实例
2. **线程安全**：使用 `CopyOnWriteArraySet` 和 `ConcurrentHashMap`
3. **设备订阅**：支持一对多订阅关系
4. **异步发送**：使用线程池异步发送
5. **优雅关闭**：支持优雅关闭机制

### 2.3 消息聚合器

**文件**：[MessageAggregator.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/websocket/MessageAggregator.java)

```java
@Component
public class MessageAggregator {

    private final ConcurrentHashMap<String, AggregatedMessage> pendingMessages = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 100)
    public void flushAggregatedMessages() {
        if (pendingMessages.isEmpty()) {
            return;
        }

        Map<String, AggregatedMessage> toSend = new HashMap<>(pendingMessages);
        pendingMessages.clear();

        // 按设备分组发送
        Map<String, List<AggregatedMessage>> byDevice = new HashMap<>();
        toSend.values().forEach(msg -> {
            byDevice.computeIfAbsent(msg.deviceId, k -> new ArrayList<>()).add(msg);
        });

        byDevice.forEach((deviceId, messages) -> {
            if (messages.size() == 1) {
                sendDirectly(messages.get(0).deviceId, messages.get(0).type, messages.get(0).data);
            } else {
                sendAggregated(deviceId, messages);
            }
        });
    }
}
```

**设计要点**：
1. **消息聚合**：100ms 内同一设备的消息聚合
2. **批量发送**：减少网络往返
3. **定时刷新**：使用 `@Scheduled` 定时任务
4. **告警优先**：告警消息不聚合，直接发送

### 2.4 通知服务

**文件**：[WebSocketNotificationService.java](file:///Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/service/WebSocketNotificationService.java)

```java
@Service
public class WebSocketNotificationService {

    private final WebSocketManager webSocketManager = WebSocketManager.getInstance();

    public void notifyDeviceStatusUpdate(String deviceId, Map<String, Object> status) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "device_status_update");
        message.put("deviceId", deviceId);
        message.put("data", status);
        message.put("timestamp", System.currentTimeMillis());

        webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
    }

    public void notifyTelemetryUpdate(String deviceId, Map<String, Object> telemetry) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "telemetry_update");
        message.put("deviceId", deviceId);
        message.put("data", telemetry);
        
        webSocketManager.sendToDeviceSubscribers(deviceId, jsonMessage);
    }

    public void broadcastSystemNotification(Map<String, Object> notification) {
        webSocketManager.broadcast(jsonMessage);
    }
}
```

**通知类型**：
- `device_status_update`：设备状态更新
- `telemetry_update`：遥测数据更新
- `command_result`：命令执行结果
- `anomaly_alert`：异常告警
- `system_notification`：系统广播

---

## 3. 核心功能实现

### 3.1 设备状态推送

```
┌─────────────────────────────────────────────────────────────┐
│                   设备状态推送流程                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 设备上报状态                                           │
│     MQTT: dorm/{room}/{device}/status                      │
│     {                                                      │
│       "online": true,                                      │
│       "total_power_w": 150.5,                             │
│       "voltage_v": 220.0,                                  │
│       "current_a": 0.68                                   │
│     }                                                      │
│                                                             │
│  2. MqttBridge 接收消息                                    │
│     ┌────────────────────────────────────────┐            │
│     │  - 解析 MQTT 消息                        │            │
│     │  - 更新设备状态到数据库                   │            │
│     │  - 触发告警检查                         │            │
│     └────────────────┬───────────────────────┘            │
│                      │                                      │
│  3. WebSocket 推送                                          │
│     ┌────────────────────────────────────────┐            │
│     │  - 构造推送消息                         │            │
│     │  - 推送给订阅设备的客户端                 │            │
│     │  {                                      │            │
│     │    "type": "device_status_update",      │            │
│     │    "deviceId": "device_001",              │            │
│     │    "payload": {...}                      │            │
│     │    "timestamp": 1640000000000            │            │
│     │  }                                      │            │
│     └────────────────┬───────────────────────┘            │
│                      │                                      │
│  4. 前端接收并更新 UI                                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 遥测数据推送

```
┌─────────────────────────────────────────────────────────────┐
│                   遥测数据推送流程                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 设备上报遥测数据                                       │
│     MQTT: dorm/{room}/{device}/telemetry                   │
│     {                                                      │
│       "power_w": 150.5,                                   │
│       "voltage_v": 220.0,                                  │
│       "current_a": 0.68,                                   │
│       "ts": 1640000000                                     │
│     }                                                      │
│                                                             │
│  2. 消息处理                                               │
│     ┌────────────────────────────────────────┐            │
│     │  方式一：Kafka 异步处理（推荐）           │            │
│     │  - 写入 Kafka                            │            │
│     │  - TelemetryConsumer 消费                │            │
│     │  - 检查告警                              │            │
│     │  - WebSocket 推送                       │            │
│     │                                          │            │
│     │  方式二：直接处理（低吞吐场景）           │            │
│     │  - 写入数据库                            │            │
│     │  - 检查告警                              │            │
│     │  - WebSocket 推送                       │            │
│     └────────────────┬───────────────────────┘            │
│                      │                                      │
│  3. 消息聚合（可选）                                        │
│     ┌────────────────────────────────────────┐            │
│     │  - 100ms 内数据聚合                     │            │
│     │  - 减少推送次数                         │            │
│     │  - 告警数据不聚合                       │            │
│     └────────────────┬───────────────────────┘            │
│                      │                                      │
│  4. 前端接收图表更新                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 告警推送

```
┌─────────────────────────────────────────────────────────────┐
│                      告警推送流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 告警触发条件                                          │
│     ┌────────────────────────────────────────┐            │
│     │  功率超限：power_w > 2000W             │            │
│     │  电压异常：voltage_v < 180 || > 250    │            │
│     │  电流异常：current_a > 16               │            │
│     └────────────────┬───────────────────────┘            │
│                      │                                      │
│  2. 告警生成                                               │
│     ┌────────────────────────────────────────┐            │
│     │  - 保存告警记录                         │            │
│     │  - 记录告警历史                        │            │
│     │  - 通知管理员                          │            │
│     └────────────────┬───────────────────────┘            │
│                      │                                      │
│  3. WebSocket 推送（不聚合，立即发送）                      │
│     {                                                      │
│       "type": "ALERT",                                    │
│       "deviceId": "device_001",                           │
│       "payload": {                                         │
│         "level": "warning",                               │
│         "message": "功率超限",                             │
│         "value": 2500,                                    │
│         "threshold": 2000                                  │
│       },                                                   │
│       "timestamp": 1640000000000                          │
│     }                                                      │
│                                                             │
│  4. 前端接收并显示告警                                      │
│     ┌────────────────────────────────────────┐            │
│     │  - 弹窗告警                            │            │
│     │  - 声音提醒                           │            │
│     │  - 告警列表更新                       │            │
│     └────────────────────────────────────────┘            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 实时消息推送流程

### 4.1 完整推送链路

```
┌─────────────────────────────────────────────────────────────┐
│                   完整消息推送链路                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐            │
│  │  IoT    │     │  后端   │     │  前端   │            │
│  │  设备   │     │  服务   │     │  应用   │            │
│  └────┬────┘     └────┬────┘     └────┬────┘            │
│       │               │               │                    │
│       │ MQTT 消息     │               │                    │
│       ├──────────────>│               │                    │
│       │               │               │                    │
│       │               │ 处理消息      │                    │
│       │               ├──────────────┤                    │
│       │               │              │                    │
│       │               │ 构造推送消息  │                    │
│       │               ├──────────────┤                    │
│       │               │              │                    │
│       │               │ WebSocket   │                    │
│       │               │ 推送        │                    │
│       │               ├─────────────>│                    │
│       │               │              │                    │
│       │               │              │ 更新 UI            │
│       │               │              ├─────────────       │
│                                                             │
│  延迟：< 100ms                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 消息格式

```json
{
    "type": "device_status_update",
    "deviceId": "device_001",
    "payload": {
        "online": true,
        "total_power_w": 150.5,
        "voltage_v": 220.0,
        "current_a": 0.68,
        "sockets": [
            {"id": 1, "on": true, "power_w": 60},
            {"id": 2, "on": true, "power_w": 90.5}
        ]
    },
    "timestamp": 1640000000000
}
```

### 4.3 客户端订阅

```javascript
// 建立 WebSocket 连接
const ws = new WebSocket('ws://localhost:8000/ws');

// 连接建立
ws.onopen = function() {
    console.log('WebSocket connected');
    
    // 订阅设备
    ws.send(JSON.stringify({
        type: 'subscribe',
        deviceId: 'device_001'
    }));
};

// 接收消息
ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    
    switch (message.type) {
        case 'device_status_update':
            updateDeviceStatus(message.deviceId, message.payload);
            break;
        case 'telemetry_update':
            updateTelemetry(message.deviceId, message.payload);
            break;
        case 'ALERT':
            showAlert(message.deviceId, message.payload);
            break;
        case 'CMD_ACK':
            showCommandResult(message.cmdId, message.state);
            break;
    }
};

// 发送心跳
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }));
    }
}, 30000);
```

---

## 5. 面试要点总结

### 5.1 项目实战问题

#### Q1: 你们项目为什么使用 WebSocket？

**答案**：

我们项目是一个 IoT 宿舍电源管理系统，使用 WebSocket 主要有以下几个原因：

1. **实时性要求高**：
   - 设备状态变化需要立即推送到前端
   - 遥测数据（电压、电流、功率）需要实时显示
   - 告警信息需要立即通知用户

2. **双向通信需求**：
   - 客户端需要订阅特定设备
   - 服务端需要向客户端推送消息

3. **技术选型对比**：
   - HTTP 轮询延迟大、资源浪费
   - Server-Sent Events 不支持双向通信
   - WebSocket 全双工通信，最适合

#### Q2: 你们项目中 WebSocket 用在哪些场景？

**答案**：

1. **设备状态推送**：
   - 设备在线/离线状态
   - 设备开关状态
   - 插座状态

2. **遥测数据推送**：
   - 实时功率
   - 电压、电流
   - 用电量统计

3. **命令执行结果**：
   - 控制命令执行结果
   - 命令确认/失败通知

4. **告警通知**：
   - 功率超限告警
   - 电压异常告警
   - 电流异常告警

5. **系统广播**：
   - 系统通知
   - 批量更新

#### Q3: 你们如何实现设备订阅机制？

**答案**：

我们实现了基于设备的订阅机制：

1. **数据结构**：
   - `sessionDeviceSubscriptions`：会话 -> 订阅设备列表
   - `deviceSubscribers`：设备 -> 订阅会话列表

2. **订阅流程**：
   - 客户端发送订阅消息：`{type: 'subscribe', deviceId: 'xxx'}`
   - 服务器将设备 ID 和会话关联
   - 该设备有消息时推送给对应会话

3. **取消订阅**：
   - 客户端发送取消订阅消息
   - 服务器删除关联关系

关键代码：
```java
public void subscribeDevice(WebSocketSession session, String deviceId) {
    sessionDeviceSubscriptions.computeIfAbsent(session, k -> ConcurrentHashMap.newKeySet()).add(deviceId);
    deviceSubscribers.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(session);
}

public void sendToDeviceSubscribers(String deviceId, String message) {
    Set<WebSocketSession> subscribers = deviceSubscribers.get(deviceId);
    if (subscribers != null) {
        subscribers.forEach(session -> sendSync(session, message));
    }
}
```

#### Q4: 你们如何保证消息的实时性？

**答案**：

1. **直接推送**：
   - MQTT 收到消息后立即 WebSocket 推送
   - 延迟 < 100ms

2. **消息聚合优化**：
   - 遥测数据 100ms 聚合
   - 减少网络往返次数

3. **异步发送**：
   - 使用线程池异步发送
   - 不阻塞业务线程

4. **告警优先**：
   - 告警消息不聚合，立即发送
   - 保障告警及时性

#### Q5: 你们如何处理高并发连接？

**答案**：

1. **线程池异步发送**：
   - 使用 `ThreadPoolExecutor` 发送消息
   - 队列缓冲，CallerRunsPolicy 背压

2. **线程安全数据结构**：
   - `CopyOnWriteArraySet`：会话集合
   - `ConcurrentHashMap`：订阅关系

3. **连接管理**：
   - 会话状态监控
   - 失效会话自动清理

4. **优雅关闭**：
   - 关闭前发送完所有消息
   - 等待线程池执行完成

关键代码：
```java
private final ThreadPoolExecutor sendExecutor = new ThreadPoolExecutor(
    poolSize, poolSize, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),
    r -> {
        Thread t = new Thread(r, "ws-sender");
        t.setDaemon(true);
        return t;
    },
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

#### Q6: 你们如何保证消息不丢失？

**答案**：

1. **数据库持久化**：
   - 所有数据先写入数据库
   - 再推送 WebSocket

2. **Kafka 缓冲**：
   - 遥测数据先写入 Kafka
   - 消费者处理后推送

3. **失败处理**：
   - 发送失败记录日志
   - 失效会话自动移除

4. **前端确认**：
   - 可选：前端收到消息后确认
   - 未确认可重新发送

### 5.2 技术深度问题

#### Q7: WebSocket 和 MQTT 的关系？

**答案**：

在项目中，WebSocket 和 MQTT 承担不同角色：

| 对比 | MQTT | WebSocket |
|------|------|-----------|
| **协议层** | 应用层协议 | 应用层协议 |
| **通信模式** | 发布/订阅 | 点对点 |
| **使用场景** | 设备 <-> 服务器 | 服务器 <-> 前端 |
| **优势** | 轻量级、物联网适配 | 浏览器原生支持、双向通信 |

**项目中的使用**：
```
IoT设备 --MQTT--> 后端服务 --WebSocket--> 前端应用
```

#### Q8: 如何保证 WebSocket 连接稳定？

**答案**：

1. **心跳机制**：
   - 客户端每 30 秒发送 Ping
   - 服务器响应 Pong
   - 检测连接是否存活

2. **断线重连**：
   - 前端实现重连逻辑
   - 指数退避策略

3. **服务器监控**：
   - 统计连接数
   - 监控发送成功率
   - 监控线程池状态

#### Q9: WebSocket 如何保证安全性？

**答案**：

1. **WSS 加密**：
   - 使用 WSS（WebSocket Secure）
   - TLS 加密传输

2. **认证机制**：
   - 连接时传递 Token
   - 验证通过后才建立连接

3. **来源验证**：
   - 检查 Origin 头
   - 防止跨站请求

4. **速率限制**：
   - 限制发送频率
   - 防止恶意攻击

#### Q10: WebSocket 如何做负载均衡？

**答案**：

1. **Sticky Session**：
   - 同一会话路由到同一服务器
   - Session 亲和性

2. **分布式部署**：
   - 消息队列（Kafka）解耦
   - 各实例独立推送

3. **前端重连**：
   - 连接断开后重新连接
   - 自动路由到新服务器

---

## 总结

本文档详细介绍了宿舍电源管理系统中 WebSocket 的使用：

1. **为什么用**：实时性要求高、双向通信需求、设备状态推送
2. **怎么用**：设备订阅机制、消息聚合、异步发送
3. **面试要点**：项目实战问题、技术深度问题

通过实际项目实践，深入理解 WebSocket 的应用场景和实现原理，为面试和实际开发提供参考。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
