# WebSocket 学习文档

## 目录
- [1. WebSocket 基础概念](#1-websocket-基础概念)
- [2. WebSocket 核心特性](#2-websocket-核心特性)
- [3. WebSocket 应用场景](#3-websocket-应用场景)
- [4. Spring WebSocket 实战](#4-spring-websocket-实战)
- [5. WebSocket 原理分析](#5-websocket-原理分析)
- [6. WebSocket 性能优化](#6-websocket-性能优化)
- [7. WebSocket 面试要点](#7-websocket-面试要点)

---

## 1. WebSocket 基础概念

### 1.1 什么是 WebSocket

WebSocket 是一种基于 TCP 协议的全双工通信协议，允许服务器主动向客户端推送数据。

### 1.2 HTTP 与 WebSocket 对比

| 对比维度 | HTTP | WebSocket |
|----------|------|-----------|
| **通信模式** | 请求-响应 | 全双工 |
| **服务器推送** | 不支持 | 支持 |
| **连接方式** | 短连接 | 长连接 |
| **头部开销** | 较大 | 较小 |
| **实时性** | 轮询/长轮询 | 实时 |
| **适用场景** | 资源获取 | 实时通信 |

### 1.3 WebSocket 握手过程

```
┌─────────────────────────────────────────────────────────────┐
│                    WebSocket 握手过程                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 客户端发起 HTTP 请求                                    │
│     GET /ws HTTP/1.1                                       │
│     Host: example.com                                       │
│     Upgrade: websocket                                      │
│     Connection: Upgrade                                     │
│     Sec-WebSocket-Version: 13                              │
│     Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==           │
│                                                             │
│  2. 服务器响应                                             │
│     HTTP/1.1 101 Switching Protocols                        │
│     Upgrade: websocket                                     │
│     Connection: Upgrade                                     │
│     Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=   │
│                                                             │
│  3. WebSocket 连接建立                                      │
│     ←─────────────────────→                                 │
│     全双工通信                                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.4 WebSocket 帧结构

```
┌─────────────────────────────────────────────────────────────┐
│                    WebSocket 帧结构                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  0                   1                   2                   │
│  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1│
│ +-+-+-+-+-------+-+-------------+-------------------------------+ │
│ |F|R|R|R| opcode|M| Payload len |    Extended payload length    | │
│ |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           | │
│ |N|V|V|V|       |S|             |   (if payload len==126/127)   | │
│ | |1|2|3|       |K|             |                               | │
│ +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - + │
│ |     Extended payload length continued, if payload len == 127  | │
│ + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - + │
│ |                               | Masking-key, if MASK set to 1  | │
│ +-------------------------------+-------------------------------+ │
│ | Masking-key (continued)       |          Payload Data         │ |
│ +-------------------------------- - - - - - - - - - - - - - - - + │
│ :                     Payload Data continued ...              : │
│ + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - + │
│ |                     Payload Data continued ...              : │
│ +---------------------------------------------------------------+ │
│                                                             │
│  FIN: 1位 - 是否是消息的最后一个帧                            │
│  opcode: 4位 - 帧类型 (0x0=延续, 0x1=文本, 0x2=二进制,        │
│                    0x8=关闭, 0x9=PING, 0xA=PONG)            │
│  MASK: 1位 - 是否使用掩码                                     │
│  Payload len: 7位 - 负载长度                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. WebSocket 核心特性

### 2.1 连接建立与关闭

```javascript
// 客户端建立连接
const ws = new WebSocket('ws://example.com/ws');

// 连接建立
ws.onopen = function() {
    console.log('WebSocket connected');
};

// 接收消息
ws.onmessage = function(event) {
    console.log('Received:', event.data);
};

// 发送消息
ws.send('Hello Server');

// 关闭连接
ws.close();
```

### 2.2 心跳机制

```javascript
// 客户端心跳
const HEARTBEAT_INTERVAL = 30000;

setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }));
    }
}, HEARTBEAT_INTERVAL);

// 服务器响应
ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    if (data.type === 'ping') {
        ws.send(JSON.stringify({ type: 'pong', timestamp: Date.now() }));
    }
};
```

### 2.3 重连机制

```javascript
class WebSocketClient {
    constructor(url) {
        this.url = url;
        this.reconnectDelay = 1000;
        this.maxReconnectDelay = 30000;
    }

    connect() {
        this.ws = new WebSocket(this.url);
        
        this.ws.onclose = () => {
            console.log('Connection closed, reconnecting...');
            setTimeout(() => {
                this.reconnectDelay = Math.min(
                    this.reconnectDelay * 2,
                    this.maxReconnectDelay
                );
                this.connect();
            }, this.reconnectDelay);
        };
    }
}
```

### 2.4 消息类型

| opcode | 含义 | 说明 |
|--------|------|------|
| 0x0 | 延续帧 | 消息分片时使用 |
| 0x1 | 文本帧 | UTF-8 文本 |
| 0x2 | 二进制帧 | 二进制数据 |
| 0x8 | 关闭帧 | 连接关闭 |
| 0x9 | Ping 帧 | 心跳请求 |
| 0xA | Pong 帧 | 心跳响应 |

---

## 3. WebSocket 应用场景

### 3.1 实时通信

```
┌─────────────────────────────────────────────────────────────┐
│                    实时通信场景                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐                 │
│  │ 用户 A  │   │ 用户 B  │   │ 用户 C  │                 │
│  └────┬────┘   └────┬────┘   └────┬────┘                 │
│       │             │             │                        │
│       └─────────────┼─────────────┘                        │
│                     │                                       │
│                     ▼                                       │
│            ┌─────────────────┐                             │
│            │   WebSocket    │                             │
│            │     服务器      │                             │
│            └─────────────────┘                             │
│                     │                                       │
│       实时消息推送：聊天、协作、游戏                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 实时数据推送

```
┌─────────────────────────────────────────────────────────────┐
│                    实时数据推送场景                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐ │
│  │  股票行情    │   │  设备监控    │   │  实时地图    │ │
│  │  实时更新    │   │  状态推送    │   │  位置追踪    │ │
│  └──────────────┘   └──────────────┘   └──────────────┘ │
│                                                             │
│  典型应用：                                                │
│  - 股票、期货行情推送                                       │
│  - IoT 设备状态监控                                        │
│  - 物流跟踪                                                │
│  - 在线游戏状态同步                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 在线客服

```
┌─────────────────────────────────────────────────────────────┐
│                      在线客服场景                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端                                                          │
│  ┌────────────────────────────────────────────────────┐   │
│  │                   客服窗口                          │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │            聊天记录区域                     │  │   │
│  │  │                                          │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  │  ┌────────────────────────────────────────────┐  │   │
│  │  │  输入框                              发送  │  │   │
│  │  └────────────────────────────────────────────┘  │   │
│  └────────────────────────────────────────────────────┘   │
│                                                             │
│  优势：                                                    │
│  - 消息实时送达                                           │
│  - 无需频繁轮询                                           │
│  - 支持富媒体                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 通知系统

```javascript
// 通知类型
const notificationTypes = {
    // 系统通知
    SYSTEM: 'system_notification',
    
    // 设备告警
    ALERT: 'alert',
    
    // 消息提醒
    MESSAGE: 'message',
    
    // 任务提醒
    TASK: 'task'
};

// 消息格式
{
    type: 'alert',
    deviceId: 'device_001',
    payload: {
        level: 'warning',
        message: '功率超限',
        value: 2500
    },
    timestamp: 1640000000000
}
```

---

## 4. Spring WebSocket 实战

### 4.1 基础配置

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public MyHandler myHandler() {
        return new MyHandler();
    }
}
```

### 4.2 消息处理器

```java
public class MyHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 连接建立
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理文本消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 连接关闭
    }
}
```

### 4.3 STOMP 协议

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

@Controller
public class GreetingController {

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) {
        return new Greeting("Hello, " + message.getName());
    }
}
```

### 4.4 连接管理

```java
@Component
public class WebSocketManager {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcast(String message) {
        sessions.forEach(session -> {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        });
    }
}
```

---

## 5. WebSocket 原理分析

### 5.1 为什么 WebSocket 能实现全双工

```
┌─────────────────────────────────────────────────────────────┐
│               HTTP 请求-响应模式                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端 ──────────────> 服务器 ──────────────> 客户端        │
│  (请求)              (响应)                                │
│                                                             │
│  问题：服务器无法主动推送数据                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│               WebSocket 全双工模式                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端 <────────────────────> 服务器                       │
│        <双向通信>                                          │
│                                                             │
│  1. 握手阶段：HTTP Upgrade                                 │
│  2. 传输阶段：TCP 长连接                                   │
│  3. 全双工：双方可随时发送数据                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 WebSocket 与 TCP 的关系

```
┌─────────────────────────────────────────────────────────────┐
│                  WebSocket 与 TCP 的关系                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  应用层                                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              WebSocket 协议                         │   │
│  │  - 握手过程 (HTTP Upgrade)                          │   │
│  │  - 帧格式 (FIN, opcode, mask, payload)             │   │
│  │  - 生命周期管理                                    │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│  传输层                                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  TCP 协议                           │   │
│  │  - 连接管理 (三次握手，四次挥手)                    │   │
│  │  - 可靠性保证 (ACK, 序列号)                        │   │
│  │  - 流量控制 (滑动窗口)                             │   │
│  │  - 拥塞控制                                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 心跳检测原理

```
┌─────────────────────────────────────────────────────────────┐
│                    心跳检测原理                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  客户端                                                      │
│     │                                                       │
│     │ ─────────── Ping (opcode=0x9) ───────────>         │
│     │                                                       │
│     │ <────────── Pong (opcode=0xA) ───────────            │
│     │                                                       │
│     │                                                       │
│  问题：检测连接是否存活                                      │
│  原因：TCP keepalive 无法满足应用层需求                      │
│                                                             │
│  解决：                                                    │
│  1. 定期发送应用层心跳                                      │
│  2. 检测对端是否响应                                        │
│  3. 超过阈值则判定连接失效                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. WebSocket 性能优化

### 6.1 消息聚合

```java
@Component
public class MessageAggregator {

    private final Map<String, Object> pendingMessages = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 100)
    public void flushMessages() {
        // 批量发送聚合的消息
        // 减少网络往返次数
    }
}
```

### 6.2 异步发送

```java
@Component
public class AsyncWebSocketSender {

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        4, 4, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000)
    );

    public void sendAsync(WebSocketSession session, String message) {
        executor.submit(() -> {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                // 异常处理
            }
        });
    }
}
```

### 6.3 连接池管理

```java
@Component
public class WebSocketPool {

    private final Map<String, WebSocketSession> pool = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        pool.put(userId, session);
    }

    public WebSocketSession get(String userId) {
        return pool.get(userId);
    }

    public void unregister(String userId) {
        pool.remove(userId);
    }
}
```

---

## 7. WebSocket 面试要点

### 7.1 基础问题

#### Q1: WebSocket 和 HTTP 的区别？

**答案**：
- HTTP 是请求-响应模式，服务器无法主动推送
- WebSocket 是全双工通信，服务器可主动推送
- HTTP 每次请求都需要建立连接，WebSocket 只需一次握手
- WebSocket 头部开销更小

#### Q2: WebSocket 握手过程？

**答案**：
1. 客户端发送 HTTP 请求，带有 Upgrade 头
2. 服务器响应 101 Switching Protocols
3. 协议从 HTTP 切换到 WebSocket
4. 建立全双工连接

#### Q3: WebSocket 如何保持连接？

**答案**：
1. TCP 层面的 keepalive
2. 应用层面的心跳机制（Ping-Pong）
3. 定期检测连接状态
4. 断线重连机制

### 7.2 进阶问题

#### Q4: 如何保证消息可靠性？

**答案**：
1. 消息确认机制（Ack）
2. 消息重发机制
3. 消息序号
4. 离线消息存储

#### Q5: 如何处理高并发？

**答案**：
1. 连接池管理
2. 消息聚合发送
3. 异步处理
4. 负载均衡
5. 分布式部署

#### Q6: WebSocket 安全考虑？

**答案**：
1. WSS (WebSocket Secure) 加密
2. 起源验证
3. 认证和授权
4. 输入验证
5. 速率限制

---

## 总结

WebSocket 是一种强大的实时通信技术，适用于需要服务器主动推送数据的场景。掌握其原理和实战技巧对于构建实时应用至关重要。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
