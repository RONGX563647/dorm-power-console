# ExecutorService 线程池管理 - 知识讲解

## 一、什么是 ExecutorService？

ExecutorService 是 Java并发包 (JUC) 中用于管理线程池的核心接口，它简化了线程的生命周期管理，提供了异步执行任务的能力。

### 为什么需要线程池？

```
❌ 没有线程池的问题：
1. 频繁创建/销毁线程 → 系统开销大
2. 线程数量不可控 → 可能导致 OOM
3. 任务执行无序 → 难以管理

✅ 使用线程池的好处：
1. 线程复用 → 减少创建/销毁开销
2. 控制并发数 → 保护系统资源
3. 任务队列 → 有序执行，防止过载
4. 提供监控 → 便于性能调优
```

---

## 二、ExecutorService 继承体系

```
                Executor (顶层接口)
                    │
            ExecutorService (核心接口)
                    │
        ┌───────────┴───────────┐
        │                       │
ScheduledExecutorService   AbstractExecutorService
        │                       │
        └───────────┬───────────┘
                    │
            ThreadPoolExecutor (核心实现类)
                    │
        ┌───────────┴───────────┐
        │                       │
ScheduledThreadPoolExecutor  ForkJoinPool
```

---

## 三、创建线程池的方式

### 方式1：Executors 工厂方法（不推荐生产使用）

```java
// ❌ 项目中的使用示例 - SimpleCacheService.java:28
private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

// ❌ 项目中的使用示例 - MqttSimulatorService.java:56
private final ExecutorService executorService = Executors.newFixedThreadPool(
    Math.min(10, Runtime.getRuntime().availableProcessors())
);
```

**Executors 提供的方法：**

| 方法 | 创建的线程池类型 | 问题 |
|------|-----------------|------|
| `newFixedThreadPool(n)` | 固定大小线程池 | 队列无界，可能 OOM |
| `newCachedThreadPool()` | 可缓存线程池 | 线程数无上限，可能 OOM |
| `newSingleThreadExecutor()` | 单线程池 | 队列无界，可能 OOM |
| `newScheduledThreadPool(n)` | 定时任务线程池 | 队列无界，可能 OOM |

### 方式2：ThreadPoolExecutor 直接创建（推荐）

```java
// ✅ 项目中的使用示例 - AsyncConfig.java:25-47
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // 核心参数配置
    executor.setCorePoolSize(2);           // 核心线程数
    executor.setMaxPoolSize(4);            // 最大线程数
    executor.setQueueCapacity(50);         // 队列容量
    executor.setThreadNamePrefix("async-"); // 线程名前缀

    // 拒绝策略
    executor.setRejectedExecutionHandler((r, e) -> {
        // 队列满时在调用线程执行
    });

    executor.initialize();
    return executor;
}
```

---

## 四、线程池核心参数详解

### 4.1 七大核心参数

```java
public ThreadPoolExecutor(
    int corePoolSize,        // 1. 核心线程数（常驻线程）
    int maximumPoolSize,     // 2. 最大线程数
    long keepAliveTime,      // 3. 非核心线程空闲存活时间
    TimeUnit unit,           // 4. 时间单位
    BlockingQueue<Runnable> workQueue,  // 5. 任务队列
    ThreadFactory threadFactory,         // 6. 线程工厂
    RejectedExecutionHandler handler     // 7. 拒绝策略
)
```

### 4.2 参数配置原则

```
                    任务到达
                       │
                       ▼
            ┌─────────────────────┐
            │ 核心线程是否已满？   │
            │ (corePoolSize)      │
            └─────────────────────┘
                 │           │
                否           是
                 │           │
                 ▼           ▼
            创建线程    ┌─────────────────────┐
            执行任务    │ 队列是否已满？      │
                        │ (workQueue)         │
                        └─────────────────────┘
                             │           │
                            否           是
                             │           │
                             ▼           ▼
                        任务入队    ┌─────────────────────┐
                        等待执行    │ 最大线程数是否已满？ │
                                    │ (maximumPoolSize)   │
                                    └─────────────────────┘
                                         │           │
                                        否           是
                                         │           │
                                         ▼           ▼
                                    创建临时线程  执行拒绝策略
                                    执行任务
```

### 4.3 项目中的配置分析

```java
// AsyncConfig.java - 适合2核2G服务器的轻量级配置
executor.setCorePoolSize(2);      // 核心线程数 = CPU核心数
executor.setMaxPoolSize(4);       // 最大线程数 = 2 * CPU核心数
executor.setQueueCapacity(50);    // 队列容量适中，避免内存压力
```

**配置建议：**

| 服务器配置 | corePoolSize | maxPoolSize | queueCapacity |
|-----------|--------------|-------------|---------------|
| 2核2G | 2 | 4 | 50 |
| 4核4G | 4 | 8 | 100 |
| 8核8G | 8 | 16 | 200 |

**CPU密集型 vs IO密集型：**
```java
// CPU密集型（计算任务）
int corePoolSize = CPU核心数 + 1;

// IO密集型（网络/文件操作）
int corePoolSize = CPU核心数 * 2;
```

---

## 五、四种拒绝策略

### 5.1 策略对比

| 策略 | 行为 | 适用场景 |
|------|------|---------|
| `AbortPolicy` | 抛出 RejectedExecutionException | 默认策略，要求严格 |
| `CallerRunsPolicy` | 在调用者线程执行任务 | 项目使用，降级但不丢失 |
| `DiscardPolicy` | 直接丢弃任务，不抛异常 | 允许任务丢失 |
| `DiscardOldestPolicy` | 丢弃队列最老任务，再尝试提交 | 允许丢失旧任务 |

### 5.2 项目中的拒绝策略

```java
// AsyncConfig.java:40-42 - 使用CallerRunsPolicy策略
executor.setRejectedExecutionHandler((r, e) -> {
    // 队列满时在调用线程执行
    // 实现：让提交任务的线程自己执行，起到削峰作用
});
```

**等价于：**
```java
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
```

---

## 六、项目中的实际应用

### 6.1 Spring @Async 异步执行

```java
// AsyncConfig.java - 配置异步线程池
@Configuration
@EnableAsync  // 启用异步支持
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}

// 使用示例
@Service
public class SomeService {

    @Async("taskExecutor")  // 指定使用的线程池
    public void asyncMethod() {
        // 此方法将在 async- 线程中异步执行
    }
}
```

### 6.2 ScheduledExecutorService 定时任务

```java
// SimpleCacheService.java:28 - 单线程定时任务
private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

public SimpleCacheService() {
    // 每5分钟执行一次缓存清理
    cleaner.scheduleAtFixedRate(this::cleanExpired, 5, 5, TimeUnit.MINUTES);
}

// 关闭钩子
public void shutdown() {
    cleaner.shutdown();  // 优雅关闭
}
```

**ScheduledExecutorService 方法：**
```java
// 延迟执行一次
schedule(Runnable command, long delay, TimeUnit unit)

// 固定频率执行（推荐）
scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)

// 固定延迟执行（考虑任务执行时间）
scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
```

### 6.3 ExecutorService 提交任务

```java
// MqttSimulatorService.java:79 - 提交任务到线程池
executorService.submit(() -> {
    try {
        task.run();
    } catch (Exception e) {
        log.error("模拟器任务执行异常", e);
    } finally {
        simulatorTasks.remove(taskId);
        saveToHistory(task);
    }
});
```

**提交任务的三种方式：**

```java
// 1. execute() - 无返回值
executorService.execute(() -> System.out.println("执行任务"));

// 2. submit() - 有返回值
Future<String> future = executorService.submit(() -> "结果");
String result = future.get();  // 阻塞获取结果

// 3. invokeAll() - 批量提交
List<Future<String>> futures = executorService.invokeAll(taskList);
```

---

## 七、线程池状态与关闭

### 7.1 线程池五种状态

```
RUNNING → SHUTDOWN → STOP → TIDYING → TERMINATED

┌─────────┐
│ RUNNING │  接受新任务，处理队列任务
└────┬────┘
     │ shutdown()
     ▼
┌─────────┐
│SHUTDOWN │  不接受新任务，但处理队列任务
└────┬────┘
     │ shutdownNow()
     ▼
┌─────────┐
│  STOP   │  不接受新任务，不处理队列任务，中断正在执行的任务
└────┬────┘
     │ 所有任务已终止
     ▼
┌─────────┐
│ TIDYING │  terminated() 方法执行
└────┬────┘
     │ terminated() 完成
     ▼
┌──────────┐
│TERMINATED│  线程池完全终止
└──────────┘
```

### 7.2 正确关闭线程池

```java
// 推荐的关闭方式
public void shutdown() {
    cleaner.shutdown();  // 优雅关闭，不再接受新任务
    try {
        // 等待已提交任务完成
        if (!cleaner.awaitTermination(60, TimeUnit.SECONDS)) {
            cleaner.shutdownNow();  // 强制关闭
        }
    } catch (InterruptedException e) {
        cleaner.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

---

## 八、线程池监控

### 8.1 关键监控指标

```java
ThreadPoolExecutor executor = (ThreadPoolExecutor) executorService;

// 核心监控指标
int activeCount = executor.getActiveCount();       // 正在执行的任务数
long completedTaskCount = executor.getCompletedTaskCount();  // 已完成任务数
int poolSize = executor.getPoolSize();             // 当前线程数
int corePoolSize = executor.getCorePoolSize();     // 核心线程数
int maximumPoolSize = executor.getMaximumPoolSize(); // 最大线程数
long taskCount = executor.getTaskCount();          // 总任务数
int queueSize = executor.getQueue().size();        // 队列中等待的任务数
```

### 8.2 Spring Boot 监控

```java
// 通过 ThreadPoolTaskExecutor 获取底层 ThreadPoolExecutor
ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) context.getBean("taskExecutor");
ThreadPoolExecutor tpe = taskExecutor.getThreadPoolExecutor();

// 监控信息
log.info("活跃线程: {}/{}, 队列大小: {}, 已完成: {}",
    tpe.getActiveCount(),
    tpe.getMaximumPoolSize(),
    tpe.getQueue().size(),
    tpe.getCompletedTaskCount());
```

---

## 九、常见问题与最佳实践

### 9.1 常见问题

```java
// ❌ 问题1：使用 Executors 创建线程池
Executors.newFixedThreadPool(10);  // 队列无界，可能OOM

// ✅ 解决：手动创建 ThreadPoolExecutor
new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>(100));  // 有界队列

// ❌ 问题2：忘记关闭线程池
// ✅ 解决：注册关闭钩子
Runtime.getRuntime().addShutdownHook(new Thread(() -> executor.shutdown()));

// ❌ 问题3：线程池命名不清晰
// ✅ 解决：使用自定义 ThreadFactory
executor.setThreadNamePrefix("业务名称-");
```

### 9.2 最佳实践清单

```
✅ 1. 线程池命名：使用有意义的线程名前缀
✅ 2. 合理配置：根据业务类型（CPU密集/IO密集）配置参数
✅ 3. 有界队列：避免无界队列导致的 OOM
✅ 4. 拒绝策略：根据业务选择合适的拒绝策略
✅ 5. 优雅关闭：实现 shutdown() 方法
✅ 6. 监控告警：监控队列大小、活跃线程数
✅ 7. 异常处理：任务内部捕获异常，避免静默失败
✅ 8. 资源隔离：不同业务使用独立线程池
```

---

## 十、项目代码参考

| 文件 | 用途 | 关键代码 |
|------|------|---------|
| [AsyncConfig.java](../backend/src/main/java/com/dormpower/config/AsyncConfig.java) | Spring异步线程池配置 | ThreadPoolTaskExecutor |
| [SimpleCacheService.java](../backend/src/main/java/com/dormpower/service/SimpleCacheService.java) | 定时清理缓存 | ScheduledExecutorService |
| [MqttSimulatorService.java](../backend/src/main/java/com/dormpower/service/MqttSimulatorService.java) | MQTT模拟器任务执行 | ExecutorService.submit() |

---

## 总结

ExecutorService 是 Java 并发编程的核心组件，掌握线程池的配置、使用和监控是构建高性能应用的关键。通过项目中的实际代码示例，我们可以看到：

1. **Spring Boot 项目**：使用 `@EnableAsync` + `ThreadPoolTaskExecutor`
2. **定时任务**：使用 `ScheduledExecutorService`
3. **资源受限环境**：合理配置核心参数，避免过度消耗资源
4. **优雅关闭**：确保任务完成后再关闭线程池