# JUC (Java Util Concurrent) 基础教程

## 目录
1. [JUC 简介](#1-juc-简介)
2. [核心组件概览](#2-核心组件概览)
3. [线程基础](#3-线程基础)
4. [线程池](#4-线程池)
5. [并发集合](#5-并发集合)
6. [原子类](#6-原子类)
7. [锁机制](#7-锁机制)
8. [工具类](#8-工具类)
9. [并发问题与解决方案](#9-并发问题与解决方案)
10. [实战练习](#10-实战练习)
11. [性能优化技巧](#11-性能优化技巧)
12. [高级并发模式](#12-高级并发模式)
13. [JVM 并发原理](#13-jvm-并发原理)
14. [最佳实践总结](#14-最佳实践总结)

---

## 1. JUC 简介

### 1.1 什么是 JUC？

JUC (Java Util Concurrent) 是 Java 并发工具包，位于 `java.util.concurrent` 包下，提供了丰富的并发编程工具。它是在 Java 5 中引入的，旨在简化并发编程，提高代码质量和性能。

### 1.2 为什么需要 JUC？

- **提高性能**：充分利用多核CPU资源，实现高效的并发处理
- **简化并发编程**：提供高级并发工具，避免手动同步的复杂性
- **线程安全**：内置线程安全机制，避免常见的并发问题
- **提高代码质量**：规范并发编程模式，减少错误
- **降低开发成本**：提供现成的并发工具，减少重复开发

### 1.3 JUC 与传统线程 API 对比

| 特性 | 传统线程 API | JUC |
|-----|------------|-----|
| 线程管理 | Thread + Runnable | ExecutorService |
| 线程安全集合 | Vector, Hashtable | ConcurrentHashMap, CopyOnWriteArrayList |
| 原子操作 | synchronized | AtomicInteger, AtomicLong |
| 锁机制 | synchronized | ReentrantLock, ReadWriteLock |
| 并发工具 | 无 | CountDownLatch, CyclicBarrier, Semaphore |
| 异步编程 | 无 | CompletableFuture |
| 定时任务 | Timer | ScheduledExecutorService |
| 线程池管理 | 无 | ThreadPoolExecutor |

### 1.4 JUC 核心包结构

```
java.util.concurrent
├── atomic             // 原子类
├── locks              // 锁相关
├── concurrent         // 并发集合
├── executor           // 线程池
├── future             // 异步任务
├── semaphore          // 信号量
├── CountDownLatch     // 倒计时门闩
├── CyclicBarrier      // 循环屏障
├── Exchanger          // 线程交换器
├── Phaser             // 阶段同步器
└── TimeUnit           // 时间单位
```

---

## 2. 核心组件概览

### 2.1 线程池相关
- `Executor` - 执行器接口，定义执行任务的基本方法
- `ExecutorService` - 线程池接口，扩展了Executor，提供生命周期管理
- `ThreadPoolExecutor` - 线程池实现，核心线程池类
- `ScheduledExecutorService` - 定时任务线程池，支持延迟和周期性任务
- `Executors` - 线程池工厂类，提供便捷的线程池创建方法

### 2.2 并发集合
- `ConcurrentHashMap` - 线程安全的哈希表，支持高并发读写
- `CopyOnWriteArrayList` - 写时复制列表，适合读多写少场景
- `CopyOnWriteArraySet` - 写时复制集合，基于CopyOnWriteArrayList
- `ConcurrentLinkedQueue` - 无界并发队列，基于链表实现
- `LinkedBlockingQueue` - 可选择有界的阻塞队列
- `ArrayBlockingQueue` - 有界阻塞队列，基于数组实现
- `PriorityBlockingQueue` - 优先阻塞队列
- `SynchronousQueue` - 同步队列，无缓冲
- `BlockingDeque` - 阻塞双端队列

### 2.3 原子类
- `AtomicInteger` - 原子整型，支持原子操作
- `AtomicLong` - 原子长整型
- `AtomicBoolean` - 原子布尔型
- `AtomicReference` - 原子引用，支持泛型
- `AtomicStampedReference` - 带版本号的原子引用，解决ABA问题
- `AtomicIntegerArray` - 原子整型数组
- `AtomicLongArray` - 原子长整型数组
- `LongAdder` - 高性能计数器，适合高并发场景
- `LongAccumulator` - 高性能累加器，支持自定义累加操作

### 2.4 锁相关
- `Lock` - 锁接口，定义锁的基本操作
- `ReentrantLock` - 可重入锁，支持公平/非公平模式
- `ReadWriteLock` - 读写锁接口
- `ReentrantReadWriteLock` - 可重入读写锁实现
- `StampedLock` - 乐观读写锁，性能更高
- `Condition` - 条件变量，用于线程间通信

### 2.5 工具类
- `CountDownLatch` - 倒计时门闩，等待多个线程完成
- `CyclicBarrier` - 循环屏障，多个线程同步到达屏障
- `Semaphore` - 信号量，控制并发访问数量
- `Exchanger` - 线程交换器，两个线程交换数据
- `Phaser` - 阶段同步器，支持动态参与线程数

### 2.6 其他组件
- `CompletableFuture` - 异步任务，支持链式操作
- `ForkJoinPool` - 工作窃取线程池，适合分治任务
- `ThreadLocalRandom` - 线程本地随机数生成器
- `TimeUnit` - 时间单位枚举

---

## 3. 线程基础

### 3.1 线程创建方式

#### 方式 1：继承 Thread 类
```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread running: " + Thread.currentThread().getName());
        // 线程执行逻辑
    }
}

// 使用
MyThread thread = new MyThread();
thread.setName("MyThread-1");
thread.start();
```

#### 方式 2：实现 Runnable 接口
```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Runnable running: " + Thread.currentThread().getName());
        // 线程执行逻辑
    }
}

// 使用
Thread thread = new Thread(new MyRunnable(), "MyRunnable-1");
thread.start();
```

#### 方式 3：使用 Lambda 表达式
```java
// 简洁写法
Thread thread = new Thread(() -> {
    System.out.println("Lambda running: " + Thread.currentThread().getName());
    // 线程执行逻辑
}, "Lambda-1");
thread.start();
```

#### 方式 4：实现 Callable 接口
```java
class MyCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        System.out.println("Callable running: " + Thread.currentThread().getName());
        // 线程执行逻辑
        Thread.sleep(1000);
        return "Hello from Callable";
    }
}

// 使用
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<String> future = executor.submit(new MyCallable());
try {
    String result = future.get(); // 阻塞等待结果
    System.out.println("Result: " + result);
} catch (InterruptedException | ExecutionException e) {
    e.printStackTrace();
} finally {
    executor.shutdown();
}
```

#### 方式 5：使用 FutureTask
```java
// 创建 Callable
Callable<Integer> task = () -> {
    System.out.println("FutureTask running");
    Thread.sleep(1000);
    return 42;
};

// 创建 FutureTask
FutureTask<Integer> futureTask = new FutureTask<>(task);

// 启动线程
Thread thread = new Thread(futureTask, "FutureTask-1");
thread.start();

// 获取结果
try {
    Integer result = futureTask.get();
    System.out.println("FutureTask result: " + result);
} catch (InterruptedException | ExecutionException e) {
    e.printStackTrace();
}
```

#### 方式 6：使用 CompletableFuture
```java
// 异步执行任务
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    System.out.println("CompletableFuture running: " + Thread.currentThread().getName());
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "Hello from CompletableFuture";
});

// 处理结果
future.thenAccept(result -> {
    System.out.println("Result: " + result);
});

// 等待完成
future.join();
```

### 3.2 线程状态

```java
// 线程状态枚举
public enum State {
    NEW,         // 新建：线程已创建但未启动
    RUNNABLE,    // 可运行：线程正在运行或等待CPU时间片
    BLOCKED,     // 阻塞：线程等待获取锁
    WAITING,     // 等待：线程等待其他线程的特定操作
    TIMED_WAITING, // 限时等待：线程等待特定时间
    TERMINATED   // 终止：线程执行完成
}

// 获取线程状态
Thread thread = new Thread(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

System.out.println("初始状态: " + thread.getState()); // NEW
thread.start();
System.out.println("启动后: " + thread.getState()); // RUNNABLE
Thread.sleep(100);
System.out.println("睡眠中: " + thread.getState()); // TIMED_WAITING
thread.join();
System.out.println("结束后: " + thread.getState()); // TERMINATED
```

### 3.3 线程优先级

```java
Thread thread = new Thread(() -> {
    System.out.println("Thread with priority: " + Thread.currentThread().getPriority());
    // 线程执行逻辑
});

// 设置优先级（1-10，默认5）
thread.setPriority(Thread.MAX_PRIORITY); // 10
// thread.setPriority(Thread.MIN_PRIORITY); // 1
// thread.setPriority(Thread.NORM_PRIORITY); // 5

thread.start();
```

**注意**：线程优先级只是给操作系统的一个提示，不保证线程执行顺序。不同操作系统对优先级的支持程度不同，在某些系统上可能完全被忽略。

### 3.4 线程中断

```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        try {
            Thread.sleep(1000);
            System.out.println("Thread running");
        } catch (InterruptedException e) {
            // 中断异常，需要重新设置中断状态
            Thread.currentThread().interrupt();
            System.out.println("Thread interrupted");
            break;
        }
    }
});

thread.start();

// 中断线程
Thread.sleep(3000);
thread.interrupt();
```

#### 中断的正确处理方式
```java
class InterruptibleTask implements Runnable {
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 执行任务
                System.out.println("Working...");
                // 模拟长时间操作
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            // 捕获中断异常
            Thread.currentThread().interrupt(); // 重新设置中断状态
            System.out.println("Task interrupted");
        } finally {
            // 清理资源
            System.out.println("Cleaning up resources");
        }
    }
}
```

### 3.5 线程.join()

```java
Thread t1 = new Thread(() -> {
    System.out.println("Thread 1 started");
    try {
        Thread.sleep(2000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    System.out.println("Thread 1 finished");
});

Thread t2 = new Thread(() -> {
    System.out.println("Thread 2 started");
    try {
        t1.join(); // 等待t1完成
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    System.out.println("Thread 2 finished");
});

t1.start();
t2.start();

// 主线程等待t2完成
try {
    t2.join();
} catch (InterruptedException e) {
    e.printStackTrace();
}
System.out.println("Main thread finished");
```

#### 带超时的join()
```java
Thread thread = new Thread(() -> {
    try {
        Thread.sleep(5000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

thread.start();

// 等待最多2秒
try {
    boolean completed = thread.join(2000);
    System.out.println("Thread completed: " + completed);
} catch (InterruptedException e) {
    e.printStackTrace();
}
```

### 3.6 守护线程

```java
Thread daemonThread = new Thread(() -> {
    while (true) {
        try {
            Thread.sleep(1000);
            System.out.println("Daemon thread running");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

daemonThread.setDaemon(true); // 设置为守护线程
daemonThread.start();

// 主线程睡眠3秒后结束
Thread.sleep(3000);
System.out.println("Main thread finished");
// 守护线程会随着主线程结束而结束
```

#### 守护线程的使用场景
- 后台监控
- 垃圾回收
- 日志记录
- 内存管理

**注意**：守护线程不能持有需要关闭的资源，因为它们可能在任何时候被终止。

### 3.7 线程本地变量 (ThreadLocal)

```java
// 创建线程本地变量
ThreadLocal<String> threadLocal = new ThreadLocal<>();

// 线程1
Thread thread1 = new Thread(() -> {
    threadLocal.set("Value for thread 1");
    System.out.println("Thread 1: " + threadLocal.get());
    // 清理
    threadLocal.remove();
});

// 线程2
Thread thread2 = new Thread(() -> {
    threadLocal.set("Value for thread 2");
    System.out.println("Thread 2: " + threadLocal.get());
    // 清理
    threadLocal.remove();
});

thread1.start();
thread2.start();

// 主线程
threadLocal.set("Value for main thread");
System.out.println("Main thread: " + threadLocal.get());
threadLocal.remove();
```

#### ThreadLocal 的内存泄漏问题
```java
// 正确使用 ThreadLocal
class SafeThreadLocalUsage {
    private static final ThreadLocal<Resource> threadLocal = ThreadLocal.withInitial(() -> new Resource());
    
    public void useResource() {
        Resource resource = threadLocal.get();
        try {
            // 使用资源
        } finally {
            // 必须清理，防止内存泄漏
            threadLocal.remove();
        }
    }
    
    static class Resource implements AutoCloseable {
        @Override
        public void close() {
            // 清理资源
        }
    }
}
```

### 3.8 线程调度

#### 线程让步 (yield)
```java
Thread highPriorityThread = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("High priority thread: " + i);
        Thread.yield(); // 让出CPU时间片
    }
});

highPriorityThread.setPriority(Thread.MAX_PRIORITY);

highPriorityThread.start();
```

#### 线程睡眠 (sleep)
```java
Thread thread = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("Thread: " + i);
        try {
            Thread.sleep(1000); // 睡眠1秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

thread.start();
```

### 3.9 线程组

```java
// 创建线程组
ThreadGroup group = new ThreadGroup("WorkerGroup");

// 创建线程
Thread thread1 = new Thread(group, () -> {
    System.out.println("Thread 1 in group: " + Thread.currentThread().getThreadGroup().getName());
});

Thread thread2 = new Thread(group, () -> {
    System.out.println("Thread 2 in group: " + Thread.currentThread().getThreadGroup().getName());
});

// 启动线程
thread1.start();
thread2.start();

// 获取线程组信息
System.out.println("Group name: " + group.getName());
System.out.println("Active threads: " + group.activeCount());
System.out.println("Active groups: " + group.activeGroupCount());

// 中断所有线程
group.interrupt();
```

### 3.10 线程异常处理

#### 未捕获异常处理器
```java
// 设置默认未捕获异常处理器
Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
    System.err.println("Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage());
    throwable.printStackTrace();
});

// 创建会抛出异常的线程
Thread thread = new Thread(() -> {
    System.out.println("Thread started");
    // 故意抛出异常
    throw new RuntimeException("Test exception");
});

thread.start();
```

#### 线程组的异常处理
```java
ThreadGroup group = new ThreadGroup("ExceptionGroup") {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        System.err.println("Thread group caught exception: " + e.getMessage());
        e.printStackTrace();
    }
};

Thread thread = new Thread(group, () -> {
    throw new RuntimeException("Thread group exception");
});

thread.start();
```

### 3.11 线程生命周期管理

#### 线程状态转换
```
NEW → RUNNABLE → BLOCKED → WAITING → TIMED_WAITING → TERMINATED
```

#### 线程状态监控
```java
class ThreadStateMonitor {
    public static void monitorThread(Thread thread) {
        new Thread(() -> {
            while (thread.isAlive()) {
                System.out.println("Thread " + thread.getName() + " state: " + thread.getState());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Thread " + thread.getName() + " terminated");
        }).start();
    }
}

// 使用
Thread testThread = new Thread(() -> {
    try {
        Thread.sleep(2000);
        synchronized (ThreadStateMonitor.class) {
            ThreadStateMonitor.class.wait(1000);
        }
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

ThreadStateMonitor.monitorThread(testThread);
testThread.start();
```

### 3.12 线程安全的单例模式

#### 双重检查锁定
```java
class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

#### 静态内部类
```java
class Singleton {
    private Singleton() {}
    
    private static class SingletonHolder {
        private static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

#### 枚举单例
```java
enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        System.out.println("Singleton doing something");
    }
}

// 使用
Singleton.INSTANCE.doSomething();
```

---

## 4. 线程池

### 4.1 线程池优势

- **线程复用**：减少线程创建销毁的开销
- **控制并发数**：避免线程过多导致的资源耗尽
- **管理任务队列**：有序处理任务，避免任务丢失
- **统一监控**：方便管理和监控线程状态
- **异常处理**：提供统一的异常处理机制
- **提高响应速度**：线程池中的线程可以立即执行任务
- **任务调度**：支持任务优先级、延迟执行和周期性执行
- **资源管理**：合理分配系统资源，避免资源竞争

### 4.2 线程池创建

#### 方式 1：使用 Executors 工厂方法

```java
// 1. 单线程线程池：只有一个线程，任务顺序执行
ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

// 2. 固定大小线程池：固定数量的线程
ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);

// 3. 可缓存线程池：线程数可动态调整
ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

// 4. 定时任务线程池：支持延迟和周期性任务
ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(3);

// 5. 工作窃取线程池：适合分治任务
ExecutorService workStealingPool = Executors.newWorkStealingPool();

// 6. 单线程定时任务线程池
ScheduledExecutorService singleThreadScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
```

#### 方式 2：自定义 ThreadPoolExecutor

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,              // 核心线程数
    10,             // 最大线程数
    60L,            // 非核心线程存活时间
    TimeUnit.SECONDS, // 时间单位
    new ArrayBlockingQueue<>(100), // 工作队列
    Executors.defaultThreadFactory(), // 线程工厂
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);
```

#### 方式 3：使用 ThreadPoolExecutor 的扩展

```java
// 自定义线程工厂
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("worker-%d")
    .setDaemon(false)
    .build();

// 自定义拒绝策略
RejectedExecutionHandler customHandler = (r, executor) -> {
    // 自定义处理逻辑
    System.err.println("Task rejected: " + r.toString());
};

// 创建线程池
ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
    4,              // 核心线程数
    8,              // 最大线程数
    30,             // 非核心线程存活时间
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(50),
    namedThreadFactory,
    customHandler
);
```

### 4.3 线程池参数详解

1. **corePoolSize**：核心线程数，线程池保持的最小线程数
2. **maximumPoolSize**：最大线程数，线程池允许的最大线程数
3. **keepAliveTime**：非核心线程的存活时间
4. **unit**：keepAliveTime 的时间单位
5. **workQueue**：工作队列，用于存储等待执行的任务
6. **threadFactory**：线程工厂，用于创建线程
7. **handler**：拒绝策略，当任务无法处理时的处理方式

### 4.4 工作队列类型

| 队列类型 | 特点 | 适用场景 |
|---------|-----|---------|
| LinkedBlockingQueue | 无界队列 | 任务处理速度稳定，无突发流量 |
| ArrayBlockingQueue | 有界队列 | 控制队列大小，防止OOM |
| SynchronousQueue | 无缓冲队列 | 任务需要立即处理，不存储 |
| PriorityBlockingQueue | 优先队列 | 任务有优先级要求 |
| DelayQueue | 延迟队列 | 任务需要延迟执行 |
| LinkedTransferQueue | 无界队列，支持 transfer | 高吞吐量场景 |
| LinkedBlockingDeque | 双端阻塞队列 | 需要双向操作的场景 |

### 4.5 拒绝策略

1. **AbortPolicy**：直接抛出 RejectedExecutionException 异常
2. **CallerRunsPolicy**：在调用者线程执行任务
3. **DiscardPolicy**：静默丢弃任务
4. **DiscardOldestPolicy**：丢弃队列中最老的任务
5. **自定义拒绝策略**：实现 RejectedExecutionHandler 接口

#### 自定义拒绝策略示例

```java
public class CustomRejectedHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 记录日志
        System.err.println("Task rejected: " + r.toString());
        // 尝试重试
        try {
            Thread.sleep(100);
            executor.execute(r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4.6 线程池使用示例

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

// 提交 Runnable 任务
for (int i = 0; i < 10; i++) {
    final int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}

// 提交 Callable 任务
List<Future<Integer>> futures = new ArrayList<>();
for (int i = 0; i < 5; i++) {
    final int taskId = i;
    Future<Integer> future = executor.submit(() -> {
        System.out.println("Callable task " + taskId + " executed");
        Thread.sleep(500);
        return taskId * 10;
    });
    futures.add(future);
}

// 获取 Callable 任务结果
for (Future<Integer> future : futures) {
    try {
        Integer result = future.get();
        System.out.println("Callable result: " + result);
    } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
    }
}

// 关闭线程池
executor.shutdown();
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}

System.out.println("All tasks completed");
```

### 4.7 定时任务示例

```java
ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(3);

// 延迟执行
System.out.println("Current time: " + System.currentTimeMillis());
scheduledExecutor.schedule(() -> {
    System.out.println("Delayed task executed at: " + System.currentTimeMillis());
}, 2, TimeUnit.SECONDS);

// 固定频率执行
ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(() -> {
    System.out.println("Fixed rate task executed at: " + System.currentTimeMillis());
}, 1, 3, TimeUnit.SECONDS);

// 固定延迟执行
scheduledExecutor.scheduleWithFixedDelay(() -> {
    System.out.println("Fixed delay task executed at: " + System.currentTimeMillis());
    try {
        Thread.sleep(1000); // 模拟任务执行时间
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}, 1, 2, TimeUnit.SECONDS);

// 5秒后取消固定频率任务
Thread.sleep(5000);
future.cancel(false);
System.out.println("Fixed rate task canceled");

// 等待一段时间后关闭
Thread.sleep(10000);
scheduledExecutor.shutdown();
```

### 4.8 线程池监控

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5, 10, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100)
);

// 监控线程池状态
Runnable monitor = () -> {
    while (true) {
        System.out.println("\nThreadPool Status:");
        System.out.println("Core Pool Size: " + executor.getCorePoolSize());
        System.out.println("Maximum Pool Size: " + executor.getMaximumPoolSize());
        System.out.println("Active Threads: " + executor.getActiveCount());
        System.out.println("Completed Tasks: " + executor.getCompletedTaskCount());
        System.out.println("Queue Size: " + executor.getQueue().size());
        System.out.println("Pool Size: " + executor.getPoolSize());
        System.out.println("Largest Pool Size: " + executor.getLargestPoolSize());
        System.out.println("Task Count: " + executor.getTaskCount());
        System.out.println("Is Shutdown: " + executor.isShutdown());
        System.out.println("Is Terminated: " + executor.isTerminated());
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
};

new Thread(monitor).start();

// 提交任务
for (int i = 0; i < 20; i++) {
    executor.submit(() -> {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}

// 等待一段时间后关闭
Thread.sleep(10000);
executor.shutdown();
```

### 4.9 线程池调优指南

#### 4.9.1 核心线程数计算

**CPU密集型任务**：核心线程数 = CPU核心数 + 1

**IO密集型任务**：核心线程数 = CPU核心数 × 2

**混合任务**：核心线程数 = CPU核心数 × (1 + IO等待时间/CPU处理时间)

#### 4.9.2 队列大小设置

- **有界队列**：建议设置为核心线程数的2-4倍
- **无界队列**：谨慎使用，可能导致OOM
- **队列选择**：根据任务特性选择合适的队列类型

#### 4.9.3 最大线程数设置

- **CPU密集型**：最大线程数 = CPU核心数 + 1
- **IO密集型**：最大线程数 = CPU核心数 × 2-4
- **考虑因素**：系统资源、任务类型、响应时间要求

#### 4.9.4 线程池调优步骤

1. **监控现状**：收集线程池运行数据
2. **分析瓶颈**：识别性能瓶颈
3. **调整参数**：根据分析结果调整参数
4. **验证效果**：测试调优效果
5. **持续优化**：根据实际运行情况持续调整

### 4.10 线程池生命周期管理

#### 4.10.1 线程池状态

```java
// ThreadPoolExecutor 中的状态定义
private static final int RUNNING    = -1 << COUNT_BITS; // 运行中
private static final int SHUTDOWN   =  0 << COUNT_BITS; // 关闭中
private static final int STOP       =  1 << COUNT_BITS; // 停止
private static final int TIDYING    =  2 << COUNT_BITS; // 整理中
private static final int TERMINATED =  3 << COUNT_BITS; // 已终止
```

#### 4.10.2 线程池关闭

```java
// 优雅关闭
ExecutorService executor = Executors.newFixedThreadPool(5);

// 提交任务...

// 关闭线程池
executor.shutdown(); // 不再接受新任务，等待现有任务完成
try {
    // 等待60秒
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        // 强制关闭
        executor.shutdownNow();
        // 再次等待
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Executor did not terminate");
        }
    }
} catch (InterruptedException e) {
    // 线程被中断
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### 4.11 线程池最佳实践

1. **避免使用 Executors 工厂方法**：可能导致资源耗尽
2. **使用有界队列**：防止任务积压导致OOM
3. **设置合理的核心线程数**：根据任务类型和系统资源
4. **实现拒绝策略**：处理任务拒绝的情况
5. **监控线程池状态**：及时发现问题
6. **正确关闭线程池**：避免资源泄漏
7. **使用线程池命名**：便于调试和监控
8. **考虑任务优先级**：使用 PriorityBlockingQueue
9. **避免长时间运行的任务**：可能阻塞线程池
10. **使用 CompletableFuture**：实现更灵活的任务处理

### 4.12 线程池常见问题及解决方案

#### 4.12.1 线程池饱和

**症状**：任务提交缓慢，响应时间变长
**原因**：任务处理速度跟不上提交速度
**解决方案**：
- 增加核心线程数
- 使用更大的队列
- 优化任务处理逻辑
- 考虑使用工作窃取线程池

#### 4.12.2 线程池OOM

**症状**：OutOfMemoryError
**原因**：使用无界队列，任务积压
**解决方案**：
- 使用有界队列
- 设置合理的队列大小
- 实现有效的拒绝策略

#### 4.12.3 线程池死锁

**症状**：线程池中的线程相互等待
**原因**：任务间存在循环依赖
**解决方案**：
- 避免任务间的循环依赖
- 使用超时机制
- 监控线程池状态

#### 4.12.4 线程池性能下降

**症状**：线程池吞吐量下降
**原因**：线程竞争激烈，上下文切换频繁
**解决方案**：
- 调整线程池大小
- 使用更合适的队列类型
- 优化任务处理逻辑
- 减少锁竞争

### 4.13 线程池在实际项目中的应用

#### 4.13.1 Web服务器线程池

```java
// Tomcat线程池配置示例
ThreadPoolExecutor tomcatExecutor = new ThreadPoolExecutor(
    10,      // 核心线程数
    200,     // 最大线程数
    60,      // 非核心线程存活时间
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000), // 队列大小
    new NamedThreadFactory("tomcat-worker-"),
    new TomcatRejectedHandler()
);
```

#### 4.13.2 数据处理线程池

```java
// 数据处理线程池
ThreadPoolExecutor dataProcessor = new ThreadPoolExecutor(
    4,      // 核心线程数
    8,      // 最大线程数
    30,     // 非核心线程存活时间
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),
    new NamedThreadFactory("data-processor-"),
    new CallerRunsPolicy()
);
```

#### 4.13.3 定时任务线程池

```java
// 定时任务线程池
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
    3, 
    new NamedThreadFactory("scheduler-")
);

// 定期清理任务
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Cleaning up resources...");
    // 清理逻辑
}, 0, 1, TimeUnit.HOURS);
```

### 4.14 线程池监控与告警

#### 4.14.1 自定义监控

```java
public class ThreadPoolMonitor {
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService monitorExecutor;
    
    public ThreadPoolMonitor(ThreadPoolExecutor executor) {
        this.executor = executor;
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void startMonitoring(long interval, TimeUnit unit) {
        monitorExecutor.scheduleAtFixedRate(() -> {
            int activeThreads = executor.getActiveCount();
            int poolSize = executor.getPoolSize();
            int queueSize = executor.getQueue().size();
            long completedTasks = executor.getCompletedTaskCount();
            
            System.out.printf("ThreadPool Monitor - Active: %d, Pool: %d, Queue: %d, Completed: %d%n",
                activeThreads, poolSize, queueSize, completedTasks);
            
            // 告警逻辑
            if (queueSize > 100) {
                System.err.println("WARNING: Queue size exceeds threshold!");
            }
            
            if (activeThreads == poolSize && queueSize > 0) {
                System.err.println("WARNING: Thread pool is at capacity!");
            }
        }, 0, interval, unit);
    }
    
    public void stopMonitoring() {
        monitorExecutor.shutdown();
    }
}
```

#### 4.14.2 集成监控系统

- **Micrometer**：提供线程池指标收集
- **Prometheus**：监控线程池状态
- **Grafana**：可视化线程池指标
- **Spring Boot Actuator**：暴露线程池健康状态

### 4.15 ForkJoinPool 详解

#### 4.15.1 ForkJoinPool 特点

- **工作窃取算法**：空闲线程主动窃取其他线程的任务
- **分治任务**：适合递归分解的任务
- **并行性能**：充分利用多核CPU
- **轻量级线程**：使用ForkJoinTask，比普通线程更轻量

#### 4.15.2 ForkJoinPool 使用示例

```java
// 创建 ForkJoinPool
ForkJoinPool forkJoinPool = new ForkJoinPool(
    Runtime.getRuntime().availableProcessors(),
    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
    null,
    true // 异步模式
);

// 提交任务
long result = forkJoinPool.invoke(new FibonacciTask(40));
System.out.println("Fibonacci result: " + result);

// 关闭
forkJoinPool.shutdown();

// 斐波那契任务
class FibonacciTask extends RecursiveTask<Long> {
    private final int n;
    
    public FibonacciTask(int n) {
        this.n = n;
    }
    
    @Override
    protected Long compute() {
        if (n <= 1) {
            return (long) n;
        }
        
        FibonacciTask f1 = new FibonacciTask(n - 1);
        f1.fork();
        
        FibonacciTask f2 = new FibonacciTask(n - 2);
        return f2.compute() + f1.join();
    }
}
```

#### 4.15.3 ForkJoinPool 适用场景

- **大型数据集处理**：排序、搜索、统计
- **递归算法**：斐波那契、阶乘
- **分治任务**：归并排序、快速排序
- **并行计算**：矩阵运算、数值计算

### 4.16 线程池与 CompletableFuture

```java
// 使用线程池创建 CompletableFuture
ExecutorService executor = Executors.newFixedThreadPool(5);

CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 1 executed by " + Thread.currentThread().getName());
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "Result 1";
}, executor);

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    System.out.println("Task 2 executed by " + Thread.currentThread().getName());
    try {
        Thread.sleep(1500);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return "Result 2";
}, executor);

// 组合结果
CompletableFuture<String> combined = future1.thenCombine(future2, (result1, result2) -> {
    return result1 + " + " + result2;
});

// 获取结果
try {
    String result = combined.get();
    System.out.println("Combined result: " + result);
} catch (Exception e) {
    e.printStackTrace();
}

// 关闭线程池
executor.shutdown();
```

---

## 5. 并发集合

### 5.1 ConcurrentHashMap

**特点**：
- 线程安全
- 高效并发读写
- 分段锁（JDK 1.7）/ CAS + synchronized（JDK 1.8+）
- 支持高并发操作
- 弱一致性迭代器
- 无锁读取

**实现原理**：
- **JDK 1.7**：使用分段锁（Segment），每个Segment是一个小的HashMap
- **JDK 1.8+**：使用CAS + synchronized，减少锁竞争，提高并发性能
- **存储结构**：数组 + 链表 + 红黑树（与HashMap类似）

**使用示例**：

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// 基本操作
map.put("key1", 1);
map.put("key2", 2);
Integer value = map.get("key1");
System.out.println("Value: " + value);

// 原子操作
map.putIfAbsent("key3", 3); // 不存在才放入
map.computeIfAbsent("key4", k -> 4); // 不存在则计算
map.computeIfPresent("key1", (k, v) -> v + 1); // 存在则计算
map.merge("key5", 10, (oldValue, newValue) -> oldValue + newValue); // 合并操作

// 批量操作
Map<String, Integer> newEntries = new HashMap<>();
newEntries.put("key6", 6);
newEntries.put("key7", 7);
map.putAll(newEntries);

// 遍历（弱一致性）
map.forEach((k, v) -> System.out.println(k + ": " + v));

// 移除
map.remove("key2");

// 大小
System.out.println("Size: " + map.size());

// 其他操作
boolean contains = map.containsKey("key1");
System.out.println("Contains key1: " + contains);

// 替换
map.replace("key1", 100);
System.out.println("After replace: " + map.get("key1"));

// 条件替换
boolean replaced = map.replace("key1", 100, 200);
System.out.println("Replaced: " + replaced + ", New value: " + map.get("key1"));
```

**ConcurrentHashMap 与 Hashtable 对比**：

| 特性 | ConcurrentHashMap | Hashtable |
|-----|-----------------|-----------|
| 锁粒度 | 细粒度（分段锁/CAS） | 粗粒度（整个表） |
| 并发性能 | 高 | 低 |
| 迭代器 | 弱一致性 | 快速失败 |
| null 值 | 不允许 | 不允许 |
| 复杂度 | 高 | 低 |
| 扩展性 | 好 | 差 |

### 5.2 CopyOnWriteArrayList

**特点**：
- 写时复制
- 读操作无锁，性能高
- 写操作有锁，需要复制数组
- 适合读多写少场景
- 弱一致性迭代器
- 线程安全

**实现原理**：
- 内部维护一个不可变的数组
- 写操作时创建新数组，复制原数组内容，修改后替换原数组
- 读操作直接访问原数组，无需加锁

**使用示例**：

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// 写操作（有锁）
list.add("item1");
list.add("item2");
list.add("item3");
list.remove("item2");
list.set(0, "updated item1");

// 读操作（无锁）
for (String item : list) {
    System.out.println(item);
}

// 批量操作
List<String> newItems = Arrays.asList("item4", "item5");
list.addAll(newItems);
list.addAll(1, Arrays.asList("item6", "item7")); // 指定位置添加

// 随机访问
String item = list.get(0);
System.out.println("First item: " + item);

// 大小
System.out.println("Size: " + list.size());

// 迭代器（快照）
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    System.out.println("Iterator: " + iterator.next());
}

// 注意：迭代器不会反映后续的修改
list.add("item8");
System.out.println("After add, iterator still has:");
while (iterator.hasNext()) {
    System.out.println("Iterator: " + iterator.next()); // 不会显示item8
}

// 其他操作
boolean contains = list.contains("item4");
System.out.println("Contains item4: " + contains);

int index = list.indexOf("item5");
System.out.println("Index of item5: " + index);

list.clear();
System.out.println("After clear, size: " + list.size());
```

### 5.3 CopyOnWriteArraySet

**特点**：
- 基于 CopyOnWriteArrayList 实现
- 写时复制
- 读操作无锁，性能高
- 写操作有锁，需要复制数组
- 适合读多写少场景
- 线程安全

**使用示例**：

```java
CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();

// 添加元素
set.add("item1");
set.add("item2");
set.add("item3");
set.add("item1"); // 重复元素，不会添加

// 移除元素
set.remove("item2");

// 遍历
for (String item : set) {
    System.out.println(item);
}

// 大小
System.out.println("Size: " + set.size());

// 检查是否包含
boolean contains = set.contains("item3");
System.out.println("Contains item3: " + contains);

// 清空
set.clear();
System.out.println("After clear, size: " + set.size());
```

### 5.4 BlockingQueue

**常见实现**：
- `ArrayBlockingQueue`：有界数组队列
- `LinkedBlockingQueue`：可选择有界的链表队列
- `PriorityBlockingQueue`：优先队列
- `SynchronousQueue`：同步队列
- `DelayQueue`：延迟队列
- `LinkedTransferQueue`：无界队列，支持 transfer
- `LinkedBlockingDeque`：双端阻塞队列

#### 5.4.1 ArrayBlockingQueue

**特点**：
- 有界队列
- 基于数组实现
- FIFO
- 线程安全
- 支持阻塞操作

**使用示例**：

```java
// 创建有界阻塞队列
ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

// 生产者线程
Thread producer = new Thread(() -> {
    for (int i = 0; i < 20; i++) {
        try {
            String task = "Task " + i;
            boolean added = queue.offer(task, 1, TimeUnit.SECONDS); // 带超时的添加
            if (added) {
                System.out.println("Produced: " + task);
            } else {
                System.out.println("Failed to add: " + task);
            }
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

// 消费者线程
Thread consumer = new Thread(() -> {
    for (int i = 0; i < 20; i++) {
        try {
            String task = queue.poll(2, TimeUnit.SECONDS); // 带超时的获取
            if (task != null) {
                System.out.println("Consumed: " + task);
            } else {
                System.out.println("No task available");
            }
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

// 启动线程
producer.start();
consumer.start();

// 等待完成
try {
    producer.join();
    consumer.join();
} catch (InterruptedException e) {
    e.printStackTrace();
}

System.out.println("All tasks processed");
```

#### 5.4.2 LinkedBlockingQueue

**特点**：
- 可选择有界的链表队列
- FIFO
- 线程安全
- 支持阻塞操作
- 吞吐量高于 ArrayBlockingQueue

**使用示例**：

```java
// 创建无界阻塞队列
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

// 或创建有界阻塞队列
// LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

// 生产者线程
Thread producer = new Thread(() -> {
    for (int i = 0; i < 10; i++) {
        try {
            String task = "Task " + i;
            queue.put(task); // 阻塞添加
            System.out.println("Produced: " + task);
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

// 消费者线程
Thread consumer = new Thread(() -> {
    for (int i = 0; i < 10; i++) {
        try {
            String task = queue.take(); // 阻塞获取
            System.out.println("Consumed: " + task);
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

// 启动线程
producer.start();
consumer.start();

// 等待完成
try {
    producer.join();
    consumer.join();
} catch (InterruptedException e) {
    e.printStackTrace();
}

System.out.println("All tasks processed");
```

#### 5.4.3 PriorityBlockingQueue

**特点**：
- 优先队列
- 无界队列
- 基于堆实现
- 线程安全
- 支持阻塞操作
- 元素需要实现 Comparable 接口

**使用示例**：

```java
// 创建优先阻塞队列
PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

// 任务类
class Task implements Comparable<Task> {
    private final int priority;
    private final String name;
    
    public Task(int priority, String name) {
        this.priority = priority;
        this.name = name;
    }
    
    @Override
    public int compareTo(Task other) {
        return Integer.compare(this.priority, other.priority); // 小的优先
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "priority=" + priority +
                ", name='" + name + '\'' +
                '}';
    }
}

// 添加任务
queue.put(new Task(3, "Low priority task"));
queue.put(new Task(1, "High priority task"));
queue.put(new Task(2, "Medium priority task"));

// 取出任务（按优先级）
while (!queue.isEmpty()) {
    try {
        Task task = queue.take();
        System.out.println("Processing: " + task);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

#### 5.4.4 SynchronousQueue

**特点**：
- 同步队列
- 无缓冲
- 每个插入操作必须等待一个相应的删除操作
- 适用于直接传递场景
- 吞吐量高

**使用示例**：

```java
// 创建同步队列
SynchronousQueue<String> queue = new SynchronousQueue<>();

// 生产者线程
Thread producer = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        try {
            String task = "Task " + i;
            queue.put(task); // 阻塞，直到有消费者取走
            System.out.println("Produced: " + task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

// 消费者线程
Thread consumer = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        try {
            Thread.sleep(1000); // 模拟处理时间
            String task = queue.take(); // 阻塞，直到有生产者放入
            System.out.println("Consumed: " + task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});

// 启动线程
producer.start();
consumer.start();

// 等待完成
try {
    producer.join();
    consumer.join();
} catch (InterruptedException e) {
    e.printStackTrace();
}

System.out.println("All tasks processed");
```

### 5.5 ConcurrentLinkedQueue

**特点**：
- 无界并发队列
- 基于链表实现
- 无锁设计，使用 CAS 操作
- 高并发性能
- 弱一致性迭代器
- FIFO

**使用示例**：

```java
// 创建并发队列
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

// 添加元素
queue.offer("item1");
queue.offer("item2");
queue.offer("item3");

// 取出元素
String item1 = queue.poll();
System.out.println("Polled: " + item1);

// 查看队首元素
String head = queue.peek();
System.out.println("Peeked: " + head);

// 遍历
System.out.println("Queue elements:");
for (String item : queue) {
    System.out.println(item);
}

// 大小
System.out.println("Size: " + queue.size());

// 清空
queue.clear();
System.out.println("After clear, size: " + queue.size());
```

### 5.6 并发集合性能对比

| 集合类型 | 读操作性能 | 写操作性能 | 内存占用 | 适用场景 |
|---------|-----------|-----------|---------|----------|
| ConcurrentHashMap | 高 | 高 | 中 | 高并发读写 |
| CopyOnWriteArrayList | 很高 | 低 | 高 | 读多写少 |
| CopyOnWriteArraySet | 很高 | 低 | 高 | 读多写少 |
| ConcurrentLinkedQueue | 高 | 高 | 中 | 高并发队列操作 |
| LinkedBlockingQueue | 中 | 中 | 中 | 生产者-消费者 |
| ArrayBlockingQueue | 中 | 中 | 低 | 有界队列场景 |
| PriorityBlockingQueue | 中 | 中 | 中 | 优先级任务 |

### 5.7 并发集合最佳实践

1. **根据场景选择合适的集合**：
   - 高并发读写：ConcurrentHashMap
   - 读多写少：CopyOnWriteArrayList
   - 队列操作：ConcurrentLinkedQueue
   - 阻塞场景：LinkedBlockingQueue
   - 优先级任务：PriorityBlockingQueue

2. **注意并发集合的特性**：
   - 弱一致性迭代器：迭代过程中不会抛出 ConcurrentModificationException
   - 无锁设计：减少线程竞争，提高性能
   - 写时复制：适合读多写少场景

3. **避免常见错误**：
   - 不要在迭代过程中修改集合（虽然不会抛异常，但可能看不到最新数据）
   - 不要依赖 size() 方法的准确性（某些集合的 size() 是近似值）
   - 注意内存占用（如 CopyOnWriteArrayList 在写操作时会复制整个数组）

4. **性能优化**：
   - 对于大集合，考虑使用分段处理
   - 合理设置初始容量，减少扩容开销
   - 避免频繁的写操作（对于写时复制集合）

---

## 6. 原子类

### 6.1 原子类简介

原子类是 JUC 提供的一组线程安全的变量操作类，基于 CAS 算法实现，无需使用锁即可实现线程安全。原子类提供了原子性的操作，确保在多线程环境下的操作不会被中断。

### 6.2 常用原子类

- **AtomicInteger**：原子整型
- **AtomicLong**：原子长整型
- **AtomicBoolean**：原子布尔型
- **AtomicReference**：原子引用
- **AtomicStampedReference**：带版本号的原子引用（解决ABA问题）
- **AtomicIntegerArray**：原子整型数组
- **AtomicLongArray**：原子长整型数组
- **LongAdder**：高性能计数器（JDK 8+）
- **LongAccumulator**：高性能累加器（JDK 8+）

### 6.3 AtomicInteger 使用示例

```java
// 创建原子整型
AtomicInteger counter = new AtomicInteger(0);

// 基本操作
int value = counter.get(); // 获取当前值
System.out.println("Current value: " + value);

counter.set(10); // 设置新值
System.out.println("After set: " + counter.get());

int oldValue = counter.getAndSet(20); // 获取旧值并设置新值
System.out.println("Old value: " + oldValue + ", New value: " + counter.get());

// 原子递增
int incremented = counter.incrementAndGet(); // 先递增再获取
System.out.println("After incrementAndGet: " + incremented);

int beforeIncrement = counter.getAndIncrement(); // 先获取再递增
System.out.println("Before increment: " + beforeIncrement + ", After increment: " + counter.get());

// 原子递减
int decremented = counter.decrementAndGet(); // 先递减再获取
System.out.println("After decrementAndGet: " + decremented);

int beforeDecrement = counter.getAndDecrement(); // 先获取再递减
System.out.println("Before decrement: " + beforeDecrement + ", After decrement: " + counter.get());

// 原子加法
int added = counter.addAndGet(5); // 先加法再获取
System.out.println("After addAndGet: " + added);

int beforeAdd = counter.getAndAdd(3); // 先获取再加法
System.out.println("Before add: " + beforeAdd + ", After add: " + counter.get());

// 比较并设置
boolean success = counter.compareAndSet(27, 30); // 期望值为27，设置为30
System.out.println("CAS success: " + success + ", Value: " + counter.get());

// 自定义更新
int updated = counter.updateAndGet(x -> x * 2); // 使用Lambda表达式更新
System.out.println("After updateAndGet: " + updated);

int beforeUpdate = counter.getAndUpdate(x -> x / 2); // 先获取再更新
System.out.println("Before update: " + beforeUpdate + ", After update: " + counter.get());
```

### 6.4 AtomicReference 使用示例

```java
// 创建原子引用
AtomicReference<String> atomicRef = new AtomicReference<>("initial");

// 获取当前值
String current = atomicRef.get();
System.out.println("Current value: " + current);

// 设置新值
atomicRef.set("updated");
System.out.println("After set: " + atomicRef.get());

// 获取旧值并设置新值
String oldValue = atomicRef.getAndSet("new value");
System.out.println("Old value: " + oldValue + ", New value: " + atomicRef.get());

// 比较并设置
boolean success = atomicRef.compareAndSet("new value", "CAS success");
System.out.println("CAS success: " + success + ", Value: " + atomicRef.get());

// 自定义更新
String updated = atomicRef.updateAndGet(s -> s + " (updated)");
System.out.println("After updateAndGet: " + updated);

String beforeUpdate = atomicRef.getAndUpdate(s -> s.toUpperCase());
System.out.println("Before update: " + beforeUpdate + ", After update: " + atomicRef.get());
```

### 6.5 AtomicStampedReference 使用示例

```java
// 创建带版本号的原子引用
AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("initial", 0);

// 获取当前值和版本号
String current = stampedRef.getReference();
int stamp = stampedRef.getStamp();
System.out.println("Current value: " + current + ", Stamp: " + stamp);

// 尝试更新
boolean success = stampedRef.compareAndSet("initial", "updated", 0, 1);
System.out.println("CAS success: " + success);

// 获取新值和版本号
current = stampedRef.getReference();
stamp = stampedRef.getStamp();
System.out.println("New value: " + current + ", New stamp: " + stamp);

// 尝试使用旧版本号更新（应该失败）
success = stampedRef.compareAndSet("updated", "failed", 0, 2);
System.out.println("CAS with old stamp success: " + success);

// 尝试使用新版本号更新（应该成功）
success = stampedRef.compareAndSet("updated", "success", 1, 2);
System.out.println("CAS with new stamp success: " + success);

// 获取最终值和版本号
current = stampedRef.getReference();
stamp = stampedRef.getStamp();
System.out.println("Final value: " + current + ", Final stamp: " + stamp);
```

### 6.6 LongAdder 使用示例

```java
// 创建 LongAdder
LongAdder counter = new LongAdder();

// 加法操作
counter.increment(); // 加1
counter.add(5); // 加5

// 获取当前值
long value = counter.sum();
System.out.println("Current value: " + value);

// 重置
counter.reset();
System.out.println("After reset: " + counter.sum());

// 累加后获取
long sumThenReset = counter.sumThenReset();
System.out.println("Sum then reset: " + sumThenReset);

// 高并发场景测试
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10000; i++) {
    executor.submit(counter::increment);
}

executor.shutdown();
try {
    executor.awaitTermination(1, TimeUnit.MINUTES);
} catch (InterruptedException e) {
    e.printStackTrace();
}

System.out.println("Final count: " + counter.sum());
```

### 6.7 LongAccumulator 使用示例

```java
// 创建 LongAccumulator，初始值为0，使用加法
LongAccumulator accumulator = new LongAccumulator(Long::sum, 0);

// 累加操作
accumulator.accumulate(1);
accumulator.accumulate(2);
accumulator.accumulate(3);

// 获取当前值
long value = accumulator.get();
System.out.println("Current value: " + value);

// 使用乘法
LongAccumulator multiplier = new LongAccumulator((a, b) -> a * b, 1);
multiplier.accumulate(2);
multiplier.accumulate(3);
multiplier.accumulate(4);
System.out.println("Multiplication result: " + multiplier.get()); // 24

// 高并发场景测试
ExecutorService executor = Executors.newFixedThreadPool(10);
LongAccumulator parallelAccumulator = new LongAccumulator(Long::sum, 0);

for (int i = 0; i < 10000; i++) {
    final int valueToAdd = i;
    executor.submit(() -> parallelAccumulator.accumulate(valueToAdd));
}

executor.shutdown();
try {
    executor.awaitTermination(1, TimeUnit.MINUTES);
} catch (InterruptedException e) {
    e.printStackTrace();
}

System.out.println("Final sum: " + parallelAccumulator.get()); // 0+1+2+...+9999 = 49995000
```

### 6.8 原子类最佳实践

1. **选择合适的原子类**：
   - 简单计数器：AtomicInteger/AtomicLong
   - 高并发计数器：LongAdder
   - 复杂累加：LongAccumulator
   - 对象引用：AtomicReference
   - ABA问题：AtomicStampedReference/AtomicMarkableReference

2. **性能考虑**：
   - LongAdder 在高并发下比 AtomicLong 性能更好
   - 频繁读取使用 AtomicLong，频繁更新使用 LongAdder

3. **常见错误**：
   - 不要在原子操作中进行非原子操作
   - 注意复合操作的原子性问题

---

## 7. 锁机制

### 7.1 锁概述

Java 提供了多种锁机制，用于协调多线程对共享资源的访问。主要包括：
- synchronized 关键字
- ReentrantLock 可重入锁
- ReadWriteLock 读写锁
- StampedLock 戳记锁

### 7.2 synchronized 关键字

synchronized 是 Java 最基本的同步机制，用于保证方法或代码块的原子性。

**特性**：
- 可重入锁
- 非公平锁
- 隐式获取释放锁
- 可修饰方法或代码块

```java
// 同步方法
public synchronized void method() {
    // 线程安全操作
}

// 同步代码块
public void method() {
    synchronized (this) {
        // 线程安全操作
    }
}

// 静态同步方法
public static synchronized void staticMethod() {
    // 线程安全操作
}
```

### 7.3 ReentrantLock

ReentrantLock 是显式锁，提供比 synchronized 更多的功能。

**特性**：
- 可重入锁
- 可以设置为公平锁或非公平锁
- 提供 tryLock() 方法
- 可以设置超时

```java
// 创建可重入锁（默认非公平）
ReentrantLock lock = new ReentrantLock();

// 或创建公平锁
// ReentrantLock fairLock = new ReentrantLock(true);

public void method() {
    lock.lock();
    try {
        // 线程安全操作
    } finally {
        lock.unlock(); // 必须在 finally 中释放
    }
}

// tryLock 使用
public boolean tryLockMethod() {
    if (lock.tryLock()) {
        try {
            // 线程安全操作
            return true;
        } finally {
            lock.unlock();
        }
    }
    return false;
}

// 带超时的 tryLock
public boolean tryLockWithTimeout() {
    try {
        if (lock.tryLock(5, TimeUnit.SECONDS)) {
            try {
                // 线程安全操作
                return true;
            } finally {
                lock.unlock();
            }
        }
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
    return false;
}
```

### 7.4 ReadWriteLock

ReadWriteLock 维护一对锁：读锁和写锁。

**特性**：
- 多个线程可以同时持有读锁
- 写锁是独占的
- 读锁和写锁互斥

```java
// 创建读写锁
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// 获取读锁和写锁
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// 读操作
public String read(String key) {
    readLock.lock();
    try {
        return cache.get(key);
    } finally {
        readLock.unlock();
    }
}

// 写操作
public void write(String key, String value) {
    writeLock.lock();
    try {
        cache.put(key, value);
    } finally {
        writeLock.unlock();
    }
}
```

### 7.5 StampedLock

StampedLock 是 JDK 8 引入的乐观读锁，性能优于 ReadWriteLock。

**特性**：
- 支持乐观读
- 支持悲观读
- 支持写锁
- 不可重入

```java
// 创建戳记锁
StampedLock stampLock = new StampedLock();

// 乐观读
public String optimisticRead(String key) {
    long stamp = stampLock.tryOptimisticRead();
    String value = cache.get(key);
    
    // 验证戳记是否有效
    if (!stampLock.validate(stamp)) {
        // 升级为悲观读
        stamp = stampLock.readLock();
        try {
            value = cache.get(key);
        } finally {
            stampLock.unlockRead(stamp);
        }
    }
    return value;
}

// 悲观读
public String read(String key) {
    long stamp = stampLock.readLock();
    try {
        return cache.get(key);
    } finally {
        stampLock.unlockRead(stamp);
    }
}

// 写操作
public void write(String key, String value) {
    long stamp = stampLock.writeLock();
    try {
        cache.put(key, value);
    } finally {
        stampLock.unlockWrite(stamp);
    }
}

// 乐观读转为写锁
public boolean readAndWrite(String key, Function<String, String> transformer) {
    long stamp = stampLock.readLock();
    try {
        String oldValue = cache.get(key);
        String newValue = transformer.apply(oldValue);
        
        // 尝试升级为写锁
        long ws = stampLock.tryConvertToWriteLock(stamp);
        if (ws != 0) {
            stamp = ws;
            cache.put(key, newValue);
            return true;
        } else {
            // 升级失败，释放读锁，获取写锁
            stampLock.unlockRead(stamp);
            stamp = stampLock.writeLock();
            try {
                cache.put(key, newValue);
                return true;
            } finally {
                stampLock.unlock(stamp);
            }
        }
    } finally {
        stampLock.unlock(stamp);
    }
}
```

### 7.6 锁机制对比

| 特性 | synchronized | ReentrantLock | ReadWriteLock | StampedLock |
|-----|-------------|---------------|---------------|-------------|
| 公平性 | 非公平 | 可配置 | 可配置 | 非公平 |
| 重入性 | 可重入 | 可重入 | 可重入 | 不可重入 |
| 锁超时 | 不支持 | 支持 | 不支持 | 支持 |
| 条件变量 | 不支持 | 支持 | 不支持 | 不支持 |
| 乐观读 | 不支持 | 不支持 | 不支持 | 支持 |
| 性能 | 较好 | 较好 | 读多写少好 | 最优 |

### 7.7 锁机制最佳实践

1. **选择合适的锁**：
   - 简单同步：synchronized
   - 需要灵活性：ReentrantLock
   - 读多写少：ReadWriteLock/StampedLock
   - 高并发乐观读：StampedLock

2. **避免死锁**：
   - 统一锁获取顺序
   - 使用 tryLock 超时
   - 减少锁粒度

3. **性能优化**：
   - 减小锁粒度
   - 读写分离
   - 使用乐观锁

---

## 8. 工具类

### 8.1 CountDownLatch

CountDownLatch 是倒计时门闩，用于等待一组线程完成。

**特性**：
- 初始化计数
- 线程完成后计数减一
- 计数为0时等待线程继续

```java
// 创建倒计时门闩，初始计数为3
CountDownLatch latch = new CountDownLatch(3);

// 工作线程
ExecutorService executor = Executors.newFixedThreadPool(3);
for (int i = 0; i < 3; i++) {
    final int taskId = i;
    executor.submit(() -> {
        try {
            System.out.println("Task " + taskId + " started");
            Thread.sleep(1000 * (taskId + 1));
            System.out.println("Task " + taskId + " completed");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            latch.countDown(); // 计数减一
        }
    });
}

// 主线程等待
try {
    System.out.println("Waiting for tasks...");
    latch.await(); // 等待计数为0
    System.out.println("All tasks completed!");
} catch (InterruptedException e) {
    e.printStackTrace();
}

// 带超时等待
boolean completed = latch.await(5, TimeUnit.SECONDS);
if (completed) {
    System.out.println("All tasks completed within timeout");
} else {
    System.out.println("Timeout, some tasks not completed");
}

executor.shutdown();
```

### 8.2 CyclicBarrier

CyclicBarrier 是循环栅栏，用于让一组线程相互等待到达某个状态。

**特性**：
- 可循环使用
- 线程到达栅栏后等待
- 到达一定数量后全部释放

```java
// 创建循环栅栏， parties = 3
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("All parties reached, proceeding to next phase");
});

// 工作线程
ExecutorService executor = Executors.newFixedThreadPool(3);
for (int i = 0; i < 3; i++) {
    final int taskId = i;
    executor.submit(() -> {
        try {
            System.out.println("Task " + taskId + " phase 1 started");
            Thread.sleep(1000 * (taskId + 1));
            System.out.println("Task " + taskId + " waiting at barrier");
            barrier.await(); // 等待其他线程
            
            System.out.println("Task " + taskId + " phase 2 started");
            barrier.await(); // 第二个屏障
            
            System.out.println("Task " + taskId + " completed");
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    });
}

executor.shutdown();
```

### 8.3 Semaphore

Semaphore 是信号量，用于控制同时访问资源的线程数量。

**特性**：
- 维护许可数量
- 获取和释放许可
- 可用于限流

```java
// 创建信号量，允许3个并发访问
Semaphore semaphore = new Semaphore(3);

// 工作线程
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10; i++) {
    final int taskId = i;
    executor.submit(() -> {
        try {
            System.out.println("Task " + taskId + " trying to acquire");
            semaphore.acquire(); // 获取许可
            
            try {
                System.out.println("Task " + taskId + " acquired, processing");
                Thread.sleep(2000); // 模拟处理
                System.out.println("Task " + taskId + " released");
            } finally {
                semaphore.release(); // 释放许可
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}

executor.shutdown();
```

### 8.4 Exchanger

Exchanger 用于两个线程之间交换数据。

**特性**：
- 线程配对交换
- 阻塞等待
- 适用于生产者-消费者场景

```java
// 创建交换器
Exchanger<String> exchanger = new Exchanger<>();

// 线程1
Thread producer = new Thread(() -> {
    try {
        String data = "Data from Producer";
        System.out.println("Producer sending: " + data);
        String received = exchanger.exchange(data);
        System.out.println("Producer received: " + received);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

// 线程2
Thread consumer = new Thread(() -> {
    try {
        String data = "Data from Consumer";
        System.out.println("Consumer sending: " + data);
        String received = exchanger.exchange(data);
        System.out.println("Consumer received: " + received);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

producer.start();
consumer.start();
producer.join();
consumer.join();
```

### 8.5 Phaser

Phaser 是 JDK 7 引入的阶段器，比 CyclicBarrier 更灵活。

**特性**：
- 支持多个阶段
- 动态注册参与者
- 可分阶段控制

```java
// 创建阶段器
Phaser phaser = new Phaser(3); // 初始3个参与者

// 工作线程
ExecutorService executor = Executors.newFixedThreadPool(3);
for (int i = 0; i < 3; i++) {
    final int taskId = i;
    executor.submit(() -> {
        try {
            // 阶段1
            System.out.println("Task " + taskId + " phase 1");
            phaser.arriveAndAwaitAdvance();
            
            // 阶段2
            System.out.println("Task " + taskId + " phase 2");
            phaser.arriveAndAwaitAdvance();
            
            // 阶段3
            System.out.println("Task " + taskId + " phase 3");
            phaser.arriveAndDeregister();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    });
}

executor.shutdown();
```

### 8.6 工具类对比

| 工具类 | 用途 | 特点 |
|--------|------|------|
| CountDownLatch | 等待一组线程完成 | 一次性，不可重置 |
| CyclicBarrier | 等待一组线程到达 | 可循环使用 |
| Semaphore | 控制并发数量 | 限流 |
| Exchanger | 线程间数据交换 | 配对交换 |
| Phaser | 多阶段同步 | 动态注册 |

---

## 9. 并发问题与解决方案

### 9.1 可见性问题

**问题**：一个线程对共享变量的修改，对其他线程不可见。

**解决方案**：使用 volatile 关键字或 synchronized。

```java
// 使用 volatile
volatile boolean flag = false;

// 线程1
new Thread(() -> {
    while (!flag) {
        // 等待
    }
    System.out.println("Flag is true");
}).start();

// 线程2
new Thread(() -> {
    flag = true;
    System.out.println("Flag set to true");
}).start();
```

### 9.2 原子性问题

**问题**：非原子操作被中断，导致数据不一致。

**解决方案**：使用原子类或 synchronized。

```java
// 不安全的操作
int count = 0;
count++; // 非原子操作

// 解决方案1: 使用 synchronized
synchronized (this) {
    count++;
}

// 解决方案2: 使用原子类
AtomicInteger atomicCount = new AtomicInteger(0);
atomicCount.incrementAndGet();
```

### 9.3 有序性问题

**问题**：指令重排序导致执行顺序不可预测。

**解决方案**：使用 volatile 或 synchronized 禁止重排序。

### 9.4 死锁

**问题**：多个线程相互等待对方持有的锁。

**示例**：
```java
// 死锁示例
Object lock1 = new Object();
Object lock2 = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lock1) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock2) {
            System.out.println("Thread 1");
        }
    }
});

Thread t2 = new Thread(() -> {
    synchronized (lock2) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
        synchronized (lock1) {
            System.out.println("Thread 2");
        }
    }
});

t1.start();
t2.start();
```

**解决方案**：
1. 统一锁获取顺序
2. 使用 tryLock 超时
3. 减少锁粒度

### 9.5 活锁

**问题**：线程不断改变状态但无法继续执行。

**解决方案**：添加随机等待或重试机制。

```java
// 解决活锁示例
while (true) {
    if (lock1.tryLock()) {
        try {
            if (lock2.tryLock()) {
                try {
                    // 执行操作
                    break;
                } finally {
                    lock2.unlock();
                }
            }
        } finally {
            lock1.unlock();
        }
    }
    // 添加随机等待
    Thread.sleep((long) (Math.random() * 100));
}
```

### 9.6 线程饥饿

**问题**：线程长时间无法获得CPU时间片。

**解决方案**：使用公平锁或调整线程优先级。

---

## 10. 实战练习

### 练习1：生产者-消费者模式

```java
public class ProducerConsumerDemo {
    private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
    private final Random random = new Random();
    
    public void start() {
        // 启动生产者
        for (int i = 0; i < 3; i++) {
            final int producerId = i;
            new Thread(() -> {
                while (true) {
                    try {
                        int value = random.nextInt(100);
                        queue.put(value);
                        System.out.println("Producer " + producerId + " produced: " + value);
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }
        
        // 启动消费者
        for (int i = 0; i < 2; i++) {
            final int consumerId = i;
            new Thread(() -> {
                while (true) {
                    try {
                        Integer value = queue.take();
                        System.out.println("Consumer " + consumerId + " consumed: " + value);
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }
    }
    
    public static void main(String[] args) {
        new ProducerConsumerDemo().start();
    }
}
```

### 练习2：并发任务协调

```java
public class TaskCoordinationDemo {
    public static void main(String[] args) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(5);
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // 等待开始信号
                    System.out.println("Task " + taskId + " running");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }
        
        System.out.println("Starting all tasks...");
        startLatch.countDown(); // 触发所有任务开始
        endLatch.await(); // 等待所有任务完成
        System.out.println("All tasks completed");
    }
}
```

### 练习3：并发缓存

```java
public class ConcurrentCache<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Long> timestamps = new ConcurrentHashMap<>();
    private final long expireMillis;
    
    public ConcurrentCache(long expireMillis) {
        this.expireMillis = expireMillis;
    }
    
    public V get(K key, Function<K, V> loader) {
        V value = cache.get(key);
        Long timestamp = timestamps.get(key);
        
        if (value == null || isExpired(timestamp)) {
            value = loader.apply(key);
            cache.put(key, value);
            timestamps.put(key, System.currentTimeMillis());
        }
        
        return value;
    }
    
    private boolean isExpired(Long timestamp) {
        return timestamp == null || 
               (System.currentTimeMillis() - timestamp) > expireMillis;
    }
    
    public void clear() {
        cache.clear();
        timestamps.clear();
    }
}
```

### 练习4：并发限流

```java
public class RateLimiter {
    private final Semaphore permits;
    private final long timeWindow;
    private final long lastResetTime;
    
    public RateLimiter(int maxPermits, long timeWindowMs) {
        this.permits = new Semaphore(maxPermits);
        this.timeWindow = timeWindowMs;
        this.lastResetTime = System.currentTimeMillis();
    }
    
    public boolean tryAcquire() {
        return permits.tryAcquire();
    }
    
    public void acquire() throws InterruptedException {
        permits.acquire();
        resetIfNeeded();
    }
    
    private synchronized void resetIfNeeded() {
        if (System.currentTimeMillis() - lastResetTime >= timeWindow) {
            permits.release(permits.availablePermits());
            permits.drainPermits();
            // 重置逻辑需要重新获取许可
        }
    }
}
```

### 练习5：并发文件下载

```java
public class ConcurrentDownloader {
    private final ExecutorService executor;
    private final List<Future<String>> results = new CopyOnWriteArrayList<>();
    
    public ConcurrentDownloader(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
    }
    
    public Future<String> download(String url) {
        Future<String> future = executor.submit(() -> {
            // 模拟下载
            System.out.println("Downloading: " + url);
            Thread.sleep(2000);
            return "Content of " + url;
        });
        results.add(future);
        return future;
    }
    
    public void shutdown() {
        executor.shutdown();
    }
    
    public static void main(String[] args) throws Exception {
        ConcurrentDownloader downloader = new ConcurrentDownloader(3);
        
        List<String> urls = Arrays.asList(
            "http://example.com/file1",
            "http://example.com/file2",
            "http://example.com/file3",
            "http://example.com/file4",
            "http://example.com/file5"
        );
        
        for (String url : urls) {
            downloader.download(url);
        }
        
        Thread.sleep(10000);
        downloader.shutdown();
    }
}
```

---

## 11. 性能优化技巧

### 11.1 线程池优化

1. **合理设置线程数**：
   - CPU密集型：coreSize = CPU核心数 + 1
   - IO密集型：coreSize = CPU核心数 * 2
   - 混合型：根据比例调整

2. **选择合适的队列**：
   - 有界队列：防止内存溢出
   - 无界队列：适合任务量可控的场景

3. **拒绝策略**：
   - CallerRunsPolicy：调用者执行
   - AbortPolicy：抛出异常
   - DiscardPolicy：丢弃任务
   - DiscardOldestPolicy：丢弃最老任务

```java
// 优化后的线程池
public class OptimizedThreadPool {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    
    public static ExecutorService createForCPUIntensive() {
        return new ThreadPoolExecutor(
            CPU_COUNT + 1,
            CPU_COUNT + 1,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("cpu-pool-" + count++);
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    public static ExecutorService createForIOIntensive() {
        return new ThreadPoolExecutor(
            CPU_COUNT * 2,
            CPU_COUNT * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("io-pool-" + count++);
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

### 11.2 并发集合优化

1. **选择合适的集合**：
   - 读多写少：CopyOnWriteArrayList
   - 读写均衡：ConcurrentHashMap
   - 队列操作：ConcurrentLinkedQueue

2. **合理设置容量**：
   - 预估大小，避免频繁扩容

3. **使用并发视图**：
   - 使用 Collections.synchronizedMap() 包装

```java
// 并发集合优化示例
public class OptimizedCache<K, V> {
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    
    public V getOrCompute(K key, Function<K, V> computer) {
        return cache.computeIfAbsent(key, computer);
    }
    
    public void evictExpired(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> 
            now - entry.getValue() instanceof CacheEntry && 
            ((CacheEntry<?>) entry.getValue()).getTimestamp() + maxAgeMillis < now
        );
    }
}

class CacheEntry<V> {
    private final V value;
    private final long timestamp;
    
    public CacheEntry(V value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }
    
    public V getValue() { return value; }
    public long getTimestamp() { return timestamp; }
}
```

### 11.3 锁优化

1. **减小锁粒度**：
   - 分段锁
   - 读写分离

2. **使用乐观锁**：
   - CAS 操作
   - 无锁数据结构

3. **减少锁持有时间**：
   - 异步操作
   - 批量处理

```java
// 优化锁使用示例
public class OptimizedCounter {
    private final LongAdder counter = new LongAdder();
    
    public void increment() {
        counter.increment();
    }
    
    public long get() {
        return counter.sum();
    }
}

// 分段锁示例
public class SegmentedMap<K, V> {
    private final int segmentCount = 16;
    private final ConcurrentHashMap<K, V>[] segments;
    
    public SegmentedMap() {
        segments = new ConcurrentHashMap[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = new ConcurrentHashMap<>();
        }
    }
    
    private int getSegment(K key) {
        return Math.abs(key.hashCode() % segmentCount);
    }
    
    public V put(K key, V value) {
        return segments[getSegment(key)].put(key, value);
    }
    
    public V get(K key) {
        return segments[getSegment(key)].get(key);
    }
}
```

### 11.4 内存优化

1. **减少对象创建**：
   - 对象池
   - 复用对象

2. **使用弱引用**：
   - WeakHashMap
   - 缓存优化

3. **批量操作**：
   - 减少网络往返
   - 减少锁竞争

```java
// 对象池示例
public class ObjectPool<T> {
    private final ConcurrentLinkedQueue<T> pool;
    private final Supplier<T> factory;
    private final Consumer<T> reset;
    
    public ObjectPool(Supplier<T> factory, Consumer<T> reset, int initialSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.factory = factory;
        this.reset = reset;
        
        for (int i = 0; i < initialSize; i++) {
            pool.add(factory.get());
        }
    }
    
    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }
    
    public void release(T obj) {
        reset.accept(obj);
        pool.offer(obj);
    }
}
```

### 11.5 性能测试工具

1. **JMH**：
```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MyBenchmark {
    @Benchmark
    public void testMethod() {
        // 测试代码
    }
}
```

2. **性能监控**：
   - JConsole
   - VisualVM
   - YourKit

### 11.6 最佳实践总结

1. **优先使用高级并发工具**：
   - ExecutorService > Thread
   - ConcurrentHashMap > Hashtable

2. **避免过度同步**：
   - 最小化锁范围
   - 使用无锁数据结构

3. **正确处理异常**：
   - 线程中的异常需要妥善处理
   - 使用 UncaughtExceptionHandler

4. **优雅关闭**：
   - 使用 shutdown() 而非 stop()
   - 等待任务完成

5. **监控和调优**：
   - 定期检查线程池状态
   - 根据负载调整参数

---

## 12. 高级并发模式

### 12.1 生产者-消费者模式

```java
public class ProducerConsumerPattern<T> {
    private final BlockingQueue<T> queue;
    private final ExecutorService executor;
    
    public ProducerConsumerPattern(int queueSize, int producerCount, int consumerCount) {
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.executor = Executors.newFixedThreadPool(producerCount + consumerCount);
        
        startProducers(producerCount);
        startConsumers(consumerCount);
    }
    
    private void startProducers(int count) {
        for (int i = 0; i < count; i++) {
            executor.submit(this::produce);
        }
    }
    
    private void startConsumers(int count) {
        for (int i = 0; i < count; i++) {
            executor.submit(this::consume);
        }
    }
    
    private void produce() {
        // 生产逻辑
    }
    
    private void consume() {
        // 消费逻辑
    }
}
```

### 12.2 工作线程模式

```java
public class WorkerThreadPool {
    private final ExecutorService executor;
    
    public WorkerThreadPool(int size) {
        executor = Executors.newFixedThreadPool(size, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }
    
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}
```

### 12.3 主从模式

```java
public class MasterSlavePattern<T> {
    private final Queue<T> masterQueue = new ConcurrentLinkedQueue<>();
    private final List<BlockingQueue<T>> slaveQueues = new ArrayList<>();
    
    public MasterSlavePattern(int slaveCount, Consumer<T> worker) {
        for (int i = 0; i < slaveCount; i++) {
            BlockingQueue<T> queue = new LinkedBlockingQueue<>();
            slaveQueues.add(queue);
            new Thread(() -> {
                while (true) {
                    try {
                        T task = queue.take();
                        worker.accept(task);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }
    }
    
    public void submit(T task) {
        masterQueue.offer(task);
        distribute();
    }
    
    private void distribute() {
        T task;
        while ((task = masterQueue.poll()) != null) {
            for (BlockingQueue<T> queue : slaveQueues) {
                if (queue.offer(task)) {
                    return;
                }
            }
            masterQueue.offer(task);
        }
    }
}
```

---

## 13. JVM 并发原理

### 13.1 Java 内存模型

JMM 定义了线程和主内存之间的抽象关系：
- 每个线程有自己的工作内存
- 线程操作数据从主内存拷贝
- 线程操作完成后写回主内存

### 13.2 可见性保证

**volatile**：
- 保证可见性
- 禁止指令重排序
- 不保证原子性

**synchronized**：
- 保证可见性和原子性
- 释放锁时刷新主内存
- 获取锁时从主内存读取

### 13.3 有序性保证

**happens-before 原则**：
- 程序顺序规则
- 监视器锁规则
- volatile 变量规则
- 线程启动规则
- 线程终止规则
- 传递性

### 13.4 CAS 原理

CAS (Compare-And-Swap) 是硬件支持的原子操作：
- 读取内存值
- 比较预期值
- 交换新值
- 失败重试

```java
// CAS 示例
public class CASCounter {
    private volatile int value = 0;
    
    public int increment() {
        int current;
        do {
            current = value;
        } while (!compareAndSet(current, current + 1));
        return current + 1;
    }
    
    private synchronized boolean compareAndSet(int expect, int update) {
        if (value == expect) {
            value = update;
            return true;
        }
        return false;
    }
}
```

---

## 14. 最佳实践总结

### 14.1 并发编程原则

1. **最小化并发范围**：
   - 只在必要时使用同步
   - 减小临界区

2. **使用正确的工具**：
   - 根据场景选择并发类
   - 不要重复造轮子

3. **线程安全设计**：
   - 不可变对象
   - 线程局部变量
   - 原子操作

### 14.2 常见错误避免

1. **不要在锁内做耗时操作**：
```java
// 错误
synchronized (lock) {
    Thread.sleep(10000); // 阻塞其他线程
}

// 正确
synchronized (lock) {
    // 快速操作
}
```

2. **避免死锁**：
```java
// 统一顺序获取锁
synchronized (lockA) {
    synchronized (lockB) {
        // 操作
    }
}
```

3. **正确关闭线程池**：
```java
executor.shutdown();
if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
    executor.shutdownNow();
}
```

### 14.3 性能考量

1. **避免过度创建线程**：
   - 使用线程池
   - 控制并发数

2. **减少上下文切换**：
   - 批量处理
   - 减少锁竞争

3. **使用合适的并发类**：
   - ConcurrentHashMap 替代 Hashtable
   - LongAdder 替代 AtomicLong

### 14.4 调试技巧

1. **使用 Thread.dumpStack()**：
```java
Thread.dumpStack();
```

2. **使用 jstack**：
```bash
jstack <pid>
```

3. **使用 VisualVM**：
   - 线程监控
   - CPU 采样
   - 内存分析

---

**文档结束**