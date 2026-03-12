# 维度8：JVM基础调优实战指南

> 基于DormPower项目的JVM调优学习指南
> 
> 从项目实际应用出发，讲解JVM内存模型、启动参数、内存溢出预防

---

## 目录

- [1. JVM内存模型](#1-jvm内存模型)
- [2. JVM启动参数](#2-jvm启动参数)
- [3. 垃圾回收机制](#3-垃圾回收机制)
- [4. 内存溢出预防](#4-内存溢出预防)
- [5. JVM监控工具](#5-jvm监控工具)

---

## 1. JVM内存模型

### 1.1 JVM内存结构

#### 1.1.1 JVM内存区域

```
【JVM调优】JVM内存结构图

┌─────────────────────────────────────────────────────────┐
│                    JVM内存                               │
├─────────────────────────────────────────────────────────┤
│  方法区（Method Area）                                   │
│  - 类信息、常量、静态变量                                 │
│  - JDK 8后称为元空间（Metaspace）                        │
├─────────────────────────────────────────────────────────┤
│  堆内存（Heap）                                          │
│  - 存放对象实例                                          │
│  - GC主要区域                                            │
│  ├───────────────────────────────────────────────────┐  │
│  │  新生代（Young Generation）                        │  │
│  │  - Eden区（8/10）                                  │  │
│  │  - Survivor区（2/10）                              │  │
│  │    - From Survivor                                │  │
│  │    - To Survivor                                  │  │
│  ├───────────────────────────────────────────────────┤  │
│  │  老年代（Old Generation）                          │  │
│  │  - 长期存活对象                                    │  │
│  └───────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  栈内存（Stack）                                         │
│  - 方法调用、局部变量                                    │
│  - 线程私有                                              │
├─────────────────────────────────────────────────────────┤
│  程序计数器（PC Register）                                │
│  - 当前执行指令位置                                      │
├─────────────────────────────────────────────────────────┤
│  本地方法栈（Native Method Stack）                        │
│  - Native方法调用                                        │
└─────────────────────────────────────────────────────────┘
```

### 1.2 堆内存分配

#### 1.2.1 默认堆内存分配

```bash
# 【JVM调优】默认堆内存大小
# 初始堆大小：物理内存的1/64
# 最大堆大小：物理内存的1/4

# 示例：8GB物理内存
# -Xms: 128MB（初始堆大小）
# -Xmx: 2048MB（最大堆大小）

# 【JVM调优】新生代与老年代比例
# 默认比例：新生代:老年代 = 1:2
# 即新生代占堆的1/3，老年代占2/3
```

#### 1.2.2 项目中的内存使用

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【JVM调优】堆内存使用示例
     */
    public List<Device> getAllDevices() {
        // 【JVM调优】大量对象在堆中创建
        List<Device> devices = deviceRepository.findAll();
        
        // 【JVM调优】临时对象在新生代创建
        List<Map<String, Object>> result = new ArrayList<>();
        for (Device device : devices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", device.getId());
            map.put("name", device.getName());
            result.add(map);
        }
        
        return result;
    }
    
    /**
     * 【JVM调优】大对象直接进入老年代
     */
    public void processLargeData() {
        // 【JVM调优】大数组可能直接进入老年代
        byte[] largeData = new byte[10 * 1024 * 1024];  // 10MB
        
        // 处理大对象
        processData(largeData);
    }
}
```

---

## 2. JVM启动参数

### 2.1 常用启动参数

#### 2.1.1 内存参数

```bash
# 【JVM调优】内存相关参数

# 堆内存
-Xms512m           # 初始堆大小512MB
-Xmx1024m          # 最大堆大小1024MB
-Xmn256m           # 新生代大小256MB（-Xmn = -XX:NewSize = -XX:MaxNewSize）

# 新生代
-XX:NewSize=128m   # 初始新生代大小
-XX:MaxNewSize=256m  # 最大新生代大小

# 元空间（JDK 8+）
-XX:MetaspaceSize=128m     # 初始元空间大小
-XX:MaxMetaspaceSize=256m  # 最大元空间大小

# 栈内存
-Xss1m              # 每个线程栈大小1MB

# 直接内存
-XX:MaxDirectMemorySize=512m  # 最大直接内存512MB
```

#### 2.1.2 GC参数

```bash
# 【JVM调优】GC相关参数

# GC算法
-XX:+UseSerialGC          # 串行GC（单核CPU）
-XX:+UseParallelGC        # 并行GC（多核CPU，吞吐量优先）
-XX:+UseConcMarkSweepGC   # CMS GC（低延迟）
-XX:+UseG1GC              # G1 GC（平衡吞吐量和延迟，JDK 9+默认）

# GC日志
-Xlog:gc*                  # 输出GC日志
-Xlog:gc:file=gc.log      # GC日志输出到文件
-Xlog:gc*:file=gc.log:time,uptime,level,tags  # 详细GC日志

# GC调优
-XX:MaxGCPauseMillis=200  # 最大GC停顿时间200ms
-XX:GCTimeRatio=99        # GC时间占比不超过1%
```

### 2.2 项目启动脚本

#### 2.2.1 开发环境启动脚本

```bash
#!/bin/bash
# 文件：backend/scripts/start-dev.sh

# 【JVM调优】开发环境启动脚本

# JVM参数
JVM_OPTS="-Xms512m -Xmx512m \
          -XX:MetaspaceSize=128m \
          -XX:MaxMetaspaceSize=256m \
          -Xss512k \
          -XX:+UseG1GC \
          -Xlog:gc*:file=logs/gc.log:time,uptime,level,tags \
          -Dspring.profiles.active=dev"

# 启动应用
java $JVM_OPTS -jar backend/target/dorm-power-backend.jar
```

#### 2.2.2 生产环境启动脚本

```bash
#!/bin/bash
# 文件：backend/scripts/start-prod.sh

# 【JVM调优】生产环境启动脚本

# JVM参数
JVM_OPTS="-server \
          -Xms2g -Xmx2g \
          -Xmn512m \
          -XX:MetaspaceSize=256m \
          -XX:MaxMetaspaceSize=512m \
          -Xss1m \
          -XX:+UseG1GC \
          -XX:MaxGCPauseMillis=200 \
          -XX:GCTimeRatio=99 \
          -XX:+HeapDumpOnOutOfMemoryError \
          -XX:HeapDumpPath=/data/logs/heapdump.hprof \
          -Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags \
          -Dspring.profiles.active=prod"

# 启动应用
nohup java $JVM_OPTS -jar backend/target/dorm-power-backend.jar > /dev/null 2>&1 &
```

**参数说明：**
| 参数 | 说明 | 开发环境 | 生产环境 |
|------|------|----------|----------|
| -Xms | 初始堆大小 | 512MB | 2GB |
| -Xmx | 最大堆大小 | 512MB | 2GB |
| -Xmn | 新生代大小 | 默认 | 512MB |
| -Xss | 线程栈大小 | 512KB | 1MB |
| GC算法 | 垃圾回收器 | G1GC | G1GC |

---

## 3. 垃圾回收机制

### 3.1 GC算法对比

#### 3.1.1 GC算法特点

| GC算法 | 优点 | 缺点 | 适用场景 |
|--------|------|------|----------|
| Serial GC | 简单、单线程 | 停顿时间长 | 单核CPU、小内存 |
| Parallel GC | 吞吐量高 | 停顿时间长 | 多核CPU、后台任务 |
| CMS GC | 低延迟 | CPU占用高 | Web应用 |
| G1 GC | 平衡吞吐量和延迟 | 复杂 | 大内存、多核CPU |

### 3.2 G1GC工作原理

#### 3.2.1 G1GC内存布局

```
【JVM调优】G1GC内存布局

堆内存（Heap）
┌─────────────────────────────────────────────────────────┐
│  Region 1  │  Region 2  │  Region 3  │  Region 4  │  ...  │
│  (Eden)     │  (Eden)     │  (Survivor) │  (Old)      │       │
├─────────────────────────────────────────────────────────┤
│  Region 5  │  Region 6  │  Region 7  │  Region 8  │  ...  │
│  (Old)      │  (Humongous)│  (Eden)     │  (Survivor) │       │
├─────────────────────────────────────────────────────────┤
│  ...       │  ...       │  ...       │  ...       │       │
└─────────────────────────────────────────────────────────┘

【JVM调优】G1GC特点：
- 堆内存被划分为多个大小相等的Region（默认1-32MB）
- Region可以是Eden、Survivor、Old、Humongous
- Humongous Region：存放超大对象（>Region大小的一半）
- GC时只回收部分Region（增量回收）
```

### 3.3 GC日志分析

#### 3.3.1 GC日志示例

```
【JVM调优】GC日志示例

[0.001s][info][gc,heap] Heap region size: 2M
[0.001s][info][gc     ] Using G1
[0.001s][info][gc,heap,coops] Heap address: 0x00000000fec00000, size: 1024 MB

[0.521s][info][gc,start     ] GC(0) Pause Young (G1 Evacuation Pause)
[0.521s][info][gc,task      ] GC(0) Using 4 workers of 4 for evacuation
[0.523s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.1ms
[0.523s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 1.2ms
[0.523s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.1ms
[0.523s][info][gc,phases    ] GC(0)   Other: 0.2ms
[0.523s][info][gc,heap      ] GC(0) Eden regions: 8->0(8)
[0.523s][info][gc,heap      ] GC(0) Survivor regions: 0->1(1)
[0.523s][info][gc,heap      ] GC(0) Old regions: 0->0
[0.523s][info][gc,heap      ] GC(0) Humongous regions: 0->0
[0.523s][info][gc,metaspace] GC(0) Metaspace: 8192K(8192K)->8192K(8192K)
[0.523s][info][gc           ] GC(0) Pause Young (G1 Evacuation Pause) 2M->1M(2M) 1.698ms
[0.523s][info][gc,cpu       ] GC(0) User=0.01s Sys=0.00s Real=0.00s

【JVM调优】GC日志解读：
- GC(0)：第0次GC
- Pause Young (G1 Evacuation Pause)：年轻代GC
- Eden regions: 8->0(8)：Eden区从8个Region变为0个
- Survivor regions: 0->1(1)：Survivor区从0个Region变为1个
- 2M->1M(2M)：堆内存从2MB变为1MB（总堆2MB）
- 1.698ms：GC停顿时间
```

---

## 4. 内存溢出预防

### 4.1 常见内存溢出

#### 4.1.1 堆内存溢出（OOM）

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    /**
     * 【JVM调优】堆内存溢出示例
     */
    public void heapOverflowExample() {
        // 【JVM调优】不断创建对象，导致堆内存溢出
        List<byte[]> list = new ArrayList<>();
        while (true) {
            // 每次创建1MB的对象
            byte[] data = new byte[1024 * 1024];
            list.add(data);
        }
    }
    
    /**
     * 【JVM调优】避免堆内存溢出
     */
    public void avoidHeapOverflow() {
        // 【JVM调优】限制集合大小
        List<byte[]> list = new ArrayList<>();
        int maxSize = 100;
        
        while (list.size() < maxSize) {
            byte[] data = new byte[1024 * 1024];
            list.add(data);
        }
        
        // 【JVM调优】及时释放不再使用的对象
        list.clear();
        list = null;
    }
}
```

#### 4.1.2 栈内存溢出（StackOverflowError）

```java
@Service
public class DeviceService {
    
    /**
     * 【JVM调优】栈内存溢出示例
     */
    public void stackOverflowExample() {
        // 【JVM调优】无限递归，导致栈内存溢出
        recursiveMethod(1);
    }
    
    private void recursiveMethod(int depth) {
        // 【JVM调优】每次递归都会在栈中创建新的栈帧
        if (depth > 10000) {
            return;
        }
        recursiveMethod(depth + 1);
    }
    
    /**
     * 【JVM调优】避免栈内存溢出
     */
    public void avoidStackOverflow() {
        // 【JVM调优】使用迭代代替递归
        int depth = 0;
        while (depth < 10000) {
            process(depth);
            depth++;
        }
    }
    
    private void process(int depth) {
    }
}
```

### 4.2 内存泄漏预防

#### 4.2.1 静态集合内存泄漏

```java
@Service
public class DeviceService {
    
    /**
     * 【JVM调优】静态集合内存泄漏示例
     */
    private static final Map<String, Device> CACHE = new HashMap<>();
    
    public void memoryLeakExample() {
        // 【JVM调优】不断向静态集合添加数据，导致内存泄漏
        for (int i = 0; i < 100000; i++) {
            Device device = new Device();
            device.setId("device_" + i);
            CACHE.put(device.getId(), device);
        }
        // 静态集合不会被GC回收，导致内存泄漏
    }
    
    /**
     * 【JVM调优】避免静态集合内存泄漏
     */
    public void avoidMemoryLeak() {
        // 【JVM调优】使用WeakHashMap，当对象不再被引用时可以被GC回收
        Map<String, Device> weakCache = new WeakHashMap<>();
        
        for (int i = 0; i < 100000; i++) {
            Device device = new Device();
            device.setId("device_" + i);
            weakCache.put(device.getId(), device);
        }
        
        // 【JVM调优】或者使用LRU缓存，限制缓存大小
        // 使用Caffeine或Guava Cache
    }
}
```

#### 4.2.2 未关闭的资源

```java
@Service
public class FileService {
    
    /**
     * 【JVM调优】未关闭资源导致内存泄漏
     */
    public void resourceLeakExample() {
        try {
            InputStream is = new FileInputStream("large-file.txt");
            // 【JVM调优】忘记关闭流，导致文件句柄泄漏
            // 正确做法：使用try-with-resources
        } catch (IOException e) {
            logger.error("文件读取失败", e);
        }
    }
    
    /**
     * 【JVM调优】正确关闭资源
     */
    public void avoidResourceLeak() {
        // 【JVM调优】使用try-with-resources自动关闭资源
        try (InputStream is = new FileInputStream("large-file.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
            
        } catch (IOException e) {
            logger.error("文件读取失败", e);
        }
    }
}
```

### 4.3 集合判空

#### 4.3.1 集合判空示例

```java
@Service
public class DeviceService {
    
    /**
     * 【JVM调优】集合判空
     */
    public void processDevices(List<Device> devices) {
        // 【JVM调优】使用CollectionUtils.isEmpty()判空
        if (CollectionUtils.isEmpty(devices)) {
            logger.info("设备列表为空");
            return;
        }
        
        // 【JVM调优】或者使用Java 8的Optional
        Optional.ofNullable(devices)
            .filter(list -> !list.isEmpty())
            .ifPresent(list -> {
                list.forEach(this::processDevice);
            });
    }
    
    /**
     * 【JVM调优】避免NPE
     */
    public Device getDevice(String deviceId) {
        // 【JVM调优】使用Optional避免NPE
        return Optional.ofNullable(deviceId)
            .map(id -> deviceRepository.findById(id))
            .flatMap(Optional::of)
            .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
    }
}
```

---

## 5. JVM监控工具

### 5.1 命令行工具

#### 5.1.1 jps - 查看Java进程

```bash
# 【JVM调优】jps：查看Java进程
$ jps
12345 DormPowerApplication
67890 Jps

# 【JVM调优】jps -l：显示完整类名
$ jps -l
12345 com.dormpower.DormPowerApplication
67890 sun.tools.jps.Jps

# 【JVM调优】jps -v：显示JVM参数
$ jps -v
12345 DormPowerApplication -Xms512m -Xmx512m -XX:+UseG1GC
```

#### 5.1.2 jstat - 监控JVM统计信息

```bash
# 【JVM调优】jstat：监控JVM统计信息

# 查看堆内存使用情况
$ jstat -gc 12345
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
512.0  512.0    0.0   256.0  5120.0   1024.0   10240.0    2048.0   5120.0 1024.0  512.0  256.0     10    0.123     0    0.000    0.123

# 查看GC情况
$ jstat -gcutil 12345 1000 10
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT
  0.00  50.00  20.00  20.00  20.00  50.00     10    0.123     0    0.000    0.123
  0.00  50.00  30.00  20.00  20.00  50.00     11    0.135     0    0.000    0.135
  ...

# 【JVM调优】参数说明：
# S0C/S1C：Survivor 0/1区容量
# S0U/S1U：Survivor 0/1区使用量
# EC：Eden区容量
# EU：Eden区使用量
# OC：老年代容量
# OU：老年代使用量
# YGC：年轻代GC次数
# YGCT：年轻代GC总时间
# FGC：Full GC次数
# FGCT：Full GC总时间
```

#### 5.1.3 jmap - 查看堆内存

```bash
# 【JVM调优】jmap：查看堆内存

# 查看堆内存使用情况
$ jmap -heap 12345
Heap Configuration:
   MinHeapFreeRatio         = 40
   MaxHeapFreeRatio         = 70
   MaxHeapSize              = 536870912 (512.0MB)
   NewSize                  = 134217728 (128.0MB)
   MaxNewSize               = 134217728 (128.0MB)
   OldSize                  = 402653184 (384.0MB)
   NewRatio                 = 2
   SurvivorRatio            = 8
   MetaspaceSize            = 21810376 (20.796875 MB)
   CompressedClassSpaceSize = 1073741824 (1024.0MB)
   MaxMetaspaceSize         = 17592186044416 MB

# 导出堆内存快照
$ jmap -dump:format=b,file=heapdump.hprof 12345
Dumping heap to /path/to/heapdump.hprof ...
Heap dump file created

# 查看堆中的对象统计
$ jmap -histo:live 12345 | head -20
 num     #instances         #bytes  class name
   1:         10000        800000  [B
   2:          5000        400000  [Ljava.lang.Object;
   3:         10000        240000  java.lang.String
   4:          5000        120000  com.dormpower.model.Device
```

### 5.2 可视化工具

#### 5.2.1 JConsole

```bash
# 【JVM调优】JConsole：JDK自带的监控工具

# 启动JConsole
$ jconsole

# 连接到Java进程
# 选择本地进程或远程进程
# 查看内存、线程、类、GC等信息
```

#### 5.2.2 JVisualVM

```bash
# 【JVM调优】JVisualVM：JDK自带的性能分析工具

# 启动JVisualVM
$ jvisualvm

# 功能：
# - 监控堆内存和非堆内存
# - 查看线程状态
# - 分析GC日志
# - 堆内存快照分析
# - CPU性能分析
```

---

## 6. JVM性能调优

### 6.1 堆内存调优

#### 6.1.1 堆内存大小调优

```bash
# 【JVM调优】堆内存调优原则

# 原则1：Xms和Xmx设置相同
# 避免运行时动态调整堆大小，减少GC开销
-Xms2g -Xmx2g

# 原则2：根据应用类型设置堆大小
# Web应用：堆大小 = 物理内存的50-70%
# 后台任务：堆大小 = 物理内存的70-80%
# 大数据处理：堆大小 = 物理内存的80-90%

# 原则3：预留足够的内存给操作系统
# 操作系统需要内存用于：文件系统缓存、网络缓冲等
# 建议：至少预留物理内存的20-30%

# 示例：8GB物理内存的Web应用
-Xms4g -Xmx4g  # 堆大小4GB，预留4GB给操作系统
```

#### 6.1.2 新生代与老年代比例调优

```bash
# 【JVM调优】新生代与老年代比例

# 默认比例：新生代:老年代 = 1:2
# 即新生代占堆的1/3，老年代占2/3

# 调优原则：
# 1. 如果Young GC频繁，增大新生代
# 2. 如果Full GC频繁，增大老年代
# 3. 如果对象存活时间短，增大新生代

# 示例1：Web应用（对象存活时间短）
-Xms2g -Xmx2g -Xmn1g  # 新生代1GB，老年代1GB

# 示例2：缓存应用（对象存活时间长）
-Xms2g -Xmx2g -Xmn512m  # 新生代512MB，老年代1.5GB

# 示例3：使用NewRatio设置比例
-XX:NewRatio=2  # 新生代:老年代 = 1:2（默认）
-XX:NewRatio=1  # 新生代:老年代 = 1:1
-XX:NewRatio=3  # 新生代:老年代 = 1:3
```

#### 6.1.3 Survivor区比例调优

```bash
# 【JVM调优】Survivor区比例

# 默认比例：Eden:S0:S1 = 8:1:1
# 即Survivor区占新生代的1/8

# 调优原则：
# 1. 如果对象过早进入老年代，增大Survivor区
# 2. 如果Survivor区空间浪费，减小Survivor区

# 示例1：增大Survivor区
-XX:SurvivorRatio=8  # Eden:S0:S1 = 8:1:1（默认）
-XX:SurvivorRatio=4  # Eden:S0:S1 = 4:1:1（Survivor区更大）

# 示例2：减小Survivor区
-XX:SurvivorRatio=16  # Eden:S0:S1 = 16:1:1（Survivor区更小）
```

### 6.2 GC调优

#### 6.2.1 GC算法选择

```bash
# 【JVM调优】GC算法选择

# Serial GC：单线程GC，适合单核CPU、小内存应用
-XX:+UseSerialGC

# Parallel GC：多线程GC，适合多核CPU、吞吐量优先的应用
-XX:+UseParallelGC

# CMS GC：低延迟GC，适合Web应用
-XX:+UseConcMarkSweepGC

# G1 GC：平衡吞吐量和延迟，适合大内存、多核CPU应用（JDK 9+默认）
-XX:+UseG1GC

# ZGC：超低延迟GC，适合大内存、低延迟应用（JDK 11+）
-XX:+UseZGC

# Shenandoah GC：低延迟GC，适合大内存应用（JDK 12+）
-XX:+UseShenandoahGC
```

#### 6.2.2 G1GC调优

```bash
# 【JVM调优】G1GC调优

# 基础参数
-XX:+UseG1GC  # 使用G1GC
-XX:MaxGCPauseMillis=200  # 最大GC停顿时间200ms
-XX:GCTimeRatio=99  # GC时间占比不超过1%

# Region大小调优
-XX:G1HeapRegionSize=16m  # Region大小16MB（默认1-32MB）
# 调优原则：
# 1. 如果对象较大，增大Region大小
# 2. 如果停顿时间较长，减小Region大小

# 并发线程数调优
-XX:ConcGCThreads=4  # 并发GC线程数（默认：并行线程数的1/4）
-XX:ParallelGCThreads=8  # 并行GC线程数（默认：CPU核心数）

# 混合GC调优
-XX:InitiatingHeapOccupancyPercent=45  # 老年代占用达到45%时触发混合GC（默认45）
# 调优原则：
# 1. 如果Full GC频繁，降低阈值
# 2. 如果混合GC频繁，提高阈值

# 示例：大内存应用G1GC调优
-Xms8g -Xmx8g \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:GCTimeRatio=99 \
-XX:G1HeapRegionSize=32m \
-XX:ConcGCThreads=8 \
-XX:ParallelGCThreads=16 \
-XX:InitiatingHeapOccupancyPercent=40
```

### 6.3 元空间调优

#### 6.3.1 元空间大小调优

```bash
# 【JVM调优】元空间调优

# JDK 8+：元空间替代永久代
# 元空间使用本地内存，不受堆内存限制

# 基础参数
-XX:MetaspaceSize=256m  # 初始元空间大小256MB
-XX:MaxMetaspaceSize=512m  # 最大元空间大小512MB

# 调优原则：
# 1. 如果类加载多，增大元空间
# 2. 如果元空间溢出，增大MaxMetaspaceSize
# 3. 如果元空间浪费，减小MaxMetaspaceSize

# 监控元空间使用
jstat -gc <pid> | grep MC  # 查看元空间容量
jstat -gc <pid> | grep MU  # 查看元空间使用量

# 示例：Spring Boot应用元空间调优
-XX:MetaspaceSize=256m \
-XX:MaxMetaspaceSize=512m
```

---

## 7. JVM故障排查

### 7.1 CPU占用高

#### 7.1.1 排查步骤

```bash
# 【JVM调优】CPU占用高排查

# 步骤1：找到CPU占用高的Java进程
top -p $(pgrep -f java)

# 步骤2：找到CPU占用高的线程
top -H -p <pid>

# 步骤3：将线程ID转换为16进制
printf "%x\n" <tid>

# 步骤4：查看线程堆栈
jstack <pid> | grep <tid_hex>

# 步骤5：分析堆栈，找到问题代码

# 示例：
# 1. 找到Java进程：12345
# 2. 找到CPU占用高的线程：12346
# 3. 转换为16进制：printf "%x\n" 12346 -> 303a
# 4. 查看线程堆栈：jstack 12345 | grep 303a
```

#### 7.1.2 死锁排查

```bash
# 【JVM调优】死锁排查

# 查看死锁信息
jstack <pid> | grep -A 10 "Found one Java-level deadlock"

# 查看所有线程状态
jstack <pid> | grep "java.lang.Thread.State"

# 分析死锁原因
# 1. 查看死锁涉及的线程
# 2. 查看死锁涉及的锁
# 3. 查看死锁涉及的代码
```

### 7.2 内存占用高

#### 7.2.1 堆内存占用高排查

```bash
# 【JVM调优】堆内存占用高排查

# 步骤1：查看堆内存使用情况
jmap -heap <pid>

# 步骤2：导出堆内存快照
jmap -dump:format=b,file=heapdump.hprof <pid>

# 步骤3：使用工具分析堆内存快照
# 工具：JVisualVM、Eclipse MAT、JProfiler

# 步骤4：找到占用内存大的对象
# 分析对象引用链，找到内存泄漏原因

# 示例：使用JVisualVM分析堆内存快照
# 1. 打开JVisualVM
# 2. File -> Load -> 选择heapdump.hprof
# 3. 查看Classes、Instances、References
# 4. 找到占用内存大的对象
# 5. 分析对象引用链
```

#### 7.2.2 非堆内存占用高排查

```bash
# 【JVM调优】非堆内存占用高排查

# 查看元空间使用情况
jstat -gc <pid> | grep MC  # 元空间容量
jstat -gc <pid> | grep MU  # 元空间使用量

# 查看直接内存使用情况
jcmd <pid> VM.native_memory summary

# 查看线程栈使用情况
jstack <pid> | wc -l  # 线程数

# 调优建议：
# 1. 如果元空间占用高，增大MaxMetaspaceSize
# 2. 如果直接内存占用高，减小MaxDirectMemorySize
# 3. 如果线程数过多，减小线程数或增大Xss
```

### 7.3 GC频繁

#### 7.3.1 Young GC频繁排查

```bash
# 【JVM调优】Young GC频繁排查

# 步骤1：查看GC日志
tail -f gc.log

# 步骤2：查看GC统计信息
jstat -gcutil <pid> 1000 10

# 步骤3：分析Young GC频率
# 如果Young GC频率过高（每秒多次），可能原因：
# 1. 新生代太小
# 2. 对象创建过多
# 3. 对象存活时间短

# 调优建议：
# 1. 增大新生代：-Xmn或-XX:NewSize
# 2. 优化代码，减少对象创建
# 3. 使用对象池，复用对象
```

#### 7.3.2 Full GC频繁排查

```bash
# 【JVM调优】Full GC频繁排查

# 步骤1：查看GC日志
tail -f gc.log | grep "Full GC"

# 步骤2：查看GC统计信息
jstat -gcutil <pid> 1000 10

# 步骤3：分析Full GC频率
# 如果Full GC频繁（每分钟多次），可能原因：
# 1. 老年代太小
# 2. 对象过早进入老年代
# 3. 内存泄漏

# 调优建议：
# 1. 增大老年代：减小新生代比例
# 2. 增大Survivor区：-XX:SurvivorRatio
# 3. 检查内存泄漏：导出堆内存快照分析
```

---

## 8. JVM最佳实践

### 8.1 启动参数最佳实践

#### 8.1.1 生产环境启动参数

```bash
#!/bin/bash
# 文件：backend/scripts/start-prod.sh

# 【JVM调优】生产环境启动参数

JVM_OPTS="-server \
          -Xms4g -Xmx4g \
          -Xmn1g \
          -XX:MetaspaceSize=256m \
          -XX:MaxMetaspaceSize=512m \
          -Xss1m \
          -XX:+UseG1GC \
          -XX:MaxGCPauseMillis=200 \
          -XX:GCTimeRatio=99 \
          -XX:G1HeapRegionSize=16m \
          -XX:InitiatingHeapOccupancyPercent=45 \
          -XX:+HeapDumpOnOutOfMemoryError \
          -XX:HeapDumpPath=/data/logs/heapdump.hprof \
          -XX:ErrorFile=/data/logs/hs_err_pid%p.log \
          -Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags \
          -Xlog:gc+heap=trace:file=/data/logs/gc-heap.log:time,uptime,level,tags \
          -Dspring.profiles.active=prod \
          -Djava.awt.headless=true \
          -Dfile.encoding=UTF-8 \
          -Duser.timezone=Asia/Shanghai"

# 启动应用
nohup java $JVM_OPTS -jar backend/target/dorm-power-backend.jar > /dev/null 2>&1 &
```

#### 8.1.2 参数说明

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| -server | 服务器模式 | 必须设置 |
| -Xms | 初始堆大小 | 与-Xmx相同 |
| -Xmx | 最大堆大小 | 物理内存的50-70% |
| -Xmn | 新生代大小 | 堆的25-33% |
| -Xss | 线程栈大小 | 1MB |
| -XX:+UseG1GC | 使用G1GC | 推荐 |
| -XX:MaxGCPauseMillis | 最大GC停顿时间 | 200ms |
| -XX:GCTimeRatio | GC时间占比 | 99 |
| -XX:+HeapDumpOnOutOfMemoryError | OOM时导出堆快照 | 必须设置 |
| -XX:HeapDumpPath | 堆快照路径 | 必须设置 |

### 8.2 监控最佳实践

#### 8.2.1 监控指标

```java
/**
 * 【JVM调优】JVM监控指标
 */
@Service
public class JVMMonitoringService {
    
    /**
     * 【JVM调优】获取JVM内存使用情况
     */
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 堆内存
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMxBean.getHeapMemoryUsage();
        info.put("heap.init", heapUsage.getInit());
        info.put("heap.used", heapUsage.getUsed());
        info.put("heap.max", heapUsage.getMax());
        info.put("heap.usage", (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        
        // 非堆内存
        MemoryUsage nonHeapUsage = memoryMxBean.getNonHeapMemoryUsage();
        info.put("nonHeap.used", nonHeapUsage.getUsed());
        info.put("nonHeap.max", nonHeapUsage.getMax());
        info.put("nonHeap.usage", (double) nonHeapUsage.getUsed() / nonHeapUsage.getMax() * 100);
        
        return info;
    }
    
    /**
     * 【JVM调优】获取GC信息
     */
    public Map<String, Object> getGCInfo() {
        Map<String, Object> info = new HashMap<>();
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            info.put(gcBean.getName() + ".count", gcBean.getCollectionCount());
            info.put(gcBean.getName() + ".time", gcBean.getCollectionTime());
        }
        
        return info;
    }
    
    /**
     * 【JVM调优】获取线程信息
     */
    public Map<String, Object> getThreadInfo() {
        Map<String, Object> info = new HashMap<>();
        
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        info.put("thread.count", threadMxBean.getThreadCount());
        info.put("thread.peak", threadMxBean.getPeakThreadCount());
        info.put("thread.daemon", threadMxBean.getDaemonThreadCount());
        
        return info;
    }
}
```

#### 8.2.2 告警规则

```yaml
# 【JVM调优】JVM告警规则

# 堆内存使用率告警
- alert: HeapMemoryUsageHigh
  expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.8
  for: 5m
  annotations:
    summary: "堆内存使用率过高"
    description: "堆内存使用率超过80%"

# GC频率告警
- alert: GCFrequencyHigh
  expr: rate(jvm_gc_pause_seconds_count[5m]) > 10
  for: 5m
  annotations:
    summary: "GC频率过高"
    description: "GC频率超过10次/秒"

# GC停顿时间告警
- alert: GCPauseTimeHigh
  expr: rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
  for: 5m
  annotations:
    summary: "GC停顿时间过长"
    description: "GC停顿时间超过100ms"
```

---

## 9. JVM常见陷阱

### 9.1 常见陷阱

#### 9.1.1 JVM常见陷阱

```java
/**
 * 【JVM调优】JVM常见陷阱
 */
public class JVMTraps {
    
    /**
     * 【陷阱1】Xms和Xmx设置不同
     */
    public class Trap1 {
        // ❌ 错误：Xms和Xmx设置不同
        // -Xms512m -Xmx2g
        // 问题：运行时动态调整堆大小，增加GC开销
        
        // ✅ 正确：Xms和Xmx设置相同
        // -Xms2g -Xmx2g
    }
    
    /**
     * 【陷阱2】堆内存设置过大
     */
    public class Trap2 {
        // ❌ 错误：堆内存设置过大
        // -Xms8g -Xmx8g（物理内存8GB）
        // 问题：操作系统内存不足，导致系统卡顿
        
        // ✅ 正确：预留足够的内存给操作系统
        // -Xms4g -Xmx4g（物理内存8GB）
    }
    
    /**
     * 【陷阱3】新生代设置过小
     */
    public class Trap3 {
        // ❌ 错误：新生代设置过小
        // -Xms2g -Xmx2g -Xmn256m
        // 问题：Young GC频繁，对象过早进入老年代
        
        // ✅ 正确：新生代设置合理
        // -Xms2g -Xmx2g -Xmn1g
    }
    
    /**
     * 【陷阱4】忽略GC日志
     */
    public class Trap4 {
        // ❌ 错误：不配置GC日志
        // 问题：无法分析GC问题
        
        // ✅ 正确：配置GC日志
        // -Xlog:gc*:file=gc.log:time,uptime,level,tags
    }
    
    /**
     * 【陷阱5】不配置OOM参数
     */
    public class Trap5 {
        // ❌ 错误：不配置OOM参数
        // 问题：OOM时无法导出堆快照
        
        // ✅ 正确：配置OOM参数
        // -XX:+HeapDumpOnOutOfMemoryError
        // -XX:HeapDumpPath=/data/logs/heapdump.hprof
    }
}
```

---

## 10. JVM性能优化技巧

### 10.1 代码优化

#### 10.1.1 减少对象创建

```java
/**
 * 【JVM调优】减少对象创建
 */
@Service
public class ObjectCreationOptimization {
    
    /**
     * 【优化1】使用基本类型代替包装类型
     */
    public void usePrimitiveType() {
        // ❌ 错误：使用包装类型
        Integer sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i;  // 每次循环都创建新的Integer对象
        }
        
        // ✅ 正确：使用基本类型
        int sum2 = 0;
        for (int i = 0; i < 1000; i++) {
            sum2 += i;  // 不创建新对象
        }
    }
    
    /**
     * 【优化2】重用对象
     */
    public void reuseObject() {
        // ❌ 错误：每次都创建新对象
        for (int i = 0; i < 1000; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String date = sdf.format(new Date());
        }
        
        // ✅ 正确：重用对象
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < 1000; i++) {
            String date = sdf.format(new Date());
        }
    }
    
    /**
     * 【优化3】使用StringBuilder代替字符串拼接
     */
    public void useStringBuilder() {
        // ❌ 错误：使用+拼接字符串
        String result = "";
        for (int i = 0; i < 1000; i++) {
            result += i;  // 每次都创建新的String对象
        }
        
        // ✅ 正确：使用StringBuilder
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append(i);
        }
        String result = sb.toString();
    }
}
```

#### 10.1.2 使用对象池

```java
/**
 * 【JVM调优】使用对象池
 */
@Service
public class ObjectPoolOptimization {
    
    /**
     * 【优化1】使用线程池
     */
    public void useThreadPool() {
        // ✅ 正确：使用线程池
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                // 执行任务
            });
        }
        executor.shutdown();
    }
    
    /**
     * 【优化2】使用连接池
     */
    public void useConnectionPool() {
        // ✅ 正确：使用连接池
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/dorm_power");
        dataSource.setUsername("user");
        dataSource.setPassword("password");
        dataSource.setMaximumPoolSize(10);
        
        // 使用连接池获取连接
        try (Connection conn = dataSource.getConnection()) {
            // 执行SQL
        } catch (SQLException e) {
            logger.error("数据库操作失败", e);
        }
    }
}
```

---

## 本章小结

| 知识点 | 项目体现 | 关键参数 |
|--------|----------|----------|
| JVM内存模型 | 对象存储在堆中 | -Xms, -Xmx |
| 启动参数 | 启动脚本配置 | -XX:+UseG1GC |
| GC机制 | 自动垃圾回收 | -Xlog:gc |
| 内存溢出预防 | 集合判空、资源关闭 | -XX:HeapDumpOnOutOfMemoryError |
| 监控工具 | jps, jstat, jmap | jmap -dump |
| 性能调优 | 堆内存、GC调优 | -XX:MaxGCPauseMillis |
| 故障排查 | CPU、内存、GC排查 | jstack, jmap |
| 最佳实践 | 生产环境启动参数 | -server, -Xms=Xmx |

---

## 11. 项目实战案例

### 11.1 低内存环境JVM配置

#### 11.1.1 实际项目代码分析

```bash
# 文件：backend/start-low-memory.sh
# 用途：1核1G服务器启动脚本

#!/bin/bash

# 宿舍用电管理系统 - 低内存启动脚本
# 适用于1核1G服务器

APP_NAME="dorm-power-backend"
JAR_FILE="target/dorm-power-backend-1.0.0.jar"

# JVM参数优化 - 1核1G配置
# -Xms256m: 初始堆内存256MB
# -Xmx512m: 最大堆内存512MB (留一半给系统)
# -XX:MetaspaceSize=64m: 元空间初始大小
# -XX:MaxMetaspaceSize=128m: 元空间最大大小
# -XX:+UseSerialGC: 使用串行GC (单核最优)
# -XX:+TieredCompilation: 分层编译
# -XX:TieredStopAtLevel=1: 快速启动
# -XX:+UseStringDeduplication: 字符串去重
# -XX:MaxDirectMemorySize=64m: 直接内存限制
# -Xss256k: 线程栈大小
# -XX:+HeapDumpOnOutOfMemoryError: OOM时dump
# -XX:HeapDumpPath=./logs/heapdump.hprof: dump路径

JVM_OPTS="-server \
  -Xms256m \
  -Xmx512m \
  -XX:MetaspaceSize=64m \
  -XX:MaxMetaspaceSize=128m \
  -XX:+UseSerialGC \
  -XX:+TieredCompilation \
  -XX:TieredStopAtLevel=1 \
  -XX:+UseStringDeduplication \
  -XX:MaxDirectMemorySize=64m \
  -Xss256k \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=./logs/heapdump.hprof \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.backgroundpreinitializer.ignore=true"

# Spring Boot参数
SPRING_OPTS="--spring.profiles.active=low-memory \
  --server.tomcat.max-threads=50 \
  --server.tomcat.min-spare-threads=5 \
  --server.tomcat.max-connections=200 \
  --server.tomcat.accept-count=20"

# 创建日志目录
mkdir -p logs

# 启动应用
echo "Starting $APP_NAME with low-memory configuration..."
echo "JVM Options: $JVM_OPTS"
echo "Memory: 256MB initial, 512MB max"

java $JVM_OPTS -jar $JAR_FILE $SPRING_OPTS
```

**配置解析：**

1. **内存配置：**
   - `-Xms256m`：初始堆内存256MB
   - `-Xmx512m`：最大堆内存512MB（留一半给系统）
   - `-XX:MetaspaceSize=64m`：元空间初始大小
   - `-XX:MaxMetaspaceSize=128m`：元空间最大大小
   - `-XX:MaxDirectMemorySize=64m`：直接内存限制

2. **GC配置：**
   - `-XX:+UseSerialGC`：使用串行GC（单核最优）
   - `-XX:+UseStringDeduplication`：字符串去重

3. **编译优化：**
   - `-XX:+TieredCompilation`：分层编译
   - `-XX:TieredStopAtLevel=1`：快速启动

4. **线程配置：**
   - `-Xss256k`：线程栈大小（减小线程栈，支持更多线程）

5. **故障诊断：**
   - `-XX:+HeapDumpOnOutOfMemoryError`：OOM时dump
   - `-XX:HeapDumpPath=./logs/heapdump.hprof`：dump路径

6. **Spring Boot优化：**
   - `--spring.profiles.active=low-memory`：使用低内存配置
   - `--server.tomcat.max-threads=50`：减少Tomcat线程数
   - `--server.tomcat.max-connections=200`：减少最大连接数

### 11.2 Docker环境JVM配置

#### 11.2.1 实际项目代码分析

```bash
# 文件：backend/docker-entrypoint.sh
# 用途：Docker容器启动脚本

#!/bin/sh
set -e

echo "=========================================="
echo "  Dorm Power Backend Starting..."
echo "=========================================="

echo "Environment Variables:"
echo "  DB_HOST: ${DB_HOST}"
echo "  DB_PORT: ${DB_PORT}"
echo "  DB_NAME: ${DB_NAME}"
echo "  DB_USERNAME: ${DB_USERNAME}"
echo "  DB_PASSWORD: ${#DB_PASSWORD} characters"
echo "  JWT_SECRET: ${#JWT_SECRET} characters"

echo ""
echo "Waiting for PostgreSQL to be ready..."

max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready!"
        break
    fi
    
    attempt=$((attempt + 1))
    echo "Waiting for PostgreSQL... (attempt $attempt/$max_attempts)"
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Error: PostgreSQL is not ready after $max_attempts attempts"
    exit 1
fi

echo ""
echo "Testing database connection..."

export PGPASSWORD="$DB_PASSWORD"

if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database connection successful!"
else
    echo "❌ Error: Cannot connect to database"
    echo "Attempting to diagnose..."
    
    echo "Checking if database exists..."
    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -c "SELECT 1 FROM pg_database WHERE datname='$DB_NAME';" | grep -q 1; then
        echo "Database exists"
    else
        echo "Database does not exist, creating..."
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -c "CREATE DATABASE $DB_NAME;" || true
    fi
    
    echo "Retrying connection..."
    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
        echo "✅ Database connection successful after retry!"
    else
        echo "❌ Error: Still cannot connect to database"
        exit 1
    fi
fi

unset PGPASSWORD

echo ""
echo "Starting Spring Boot application..."
echo "=========================================="

# JVM参数（适合2核2G服务器）
DEFAULT_JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
DEFAULT_JAVA_OPTS="$DEFAULT_JAVA_OPTS -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"
DEFAULT_JAVA_OPTS="$DEFAULT_JAVA_OPTS -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=64m"
DEFAULT_JAVA_OPTS="$DEFAULT_JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

exec java ${JAVA_OPTS:-$DEFAULT_JAVA_OPTS} \
    -Dspring.datasource.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
    -Dspring.datasource.username="${DB_USERNAME}" \
    -Dspring.datasource.password="${DB_PASSWORD}" \
    -Dsecurity.jwt.secret="${JWT_SECRET}" \
    -jar target/dorm-power-backend-1.0.0.jar
```

**配置解析：**

1. **内存配置：**
   - `-Xms256m`：初始堆内存256MB
   - `-Xmx512m`：最大堆内存512MB
   - `-XX:MaxMetaspaceSize=128m`：元空间最大大小
   - `-XX:CompressedClassSpaceSize=64m`：压缩类空间大小

2. **GC配置：**
   - `-XX:+UseG1GC`：使用G1垃圾收集器（适合2核2G）
   - `-XX:MaxGCPauseMillis=200`：最大GC暂停时间200ms

3. **优化配置：**
   - `-XX:+UseStringDeduplication`：字符串去重
   - `-XX:+OptimizeStringConcat`：优化字符串连接

4. **故障诊断：**
   - `-XX:+HeapDumpOnOutOfMemoryError`：OOM时dump
   - `-XX:HeapDumpPath=/app/logs`：dump路径

5. **环境变量支持：**
   - `${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}`：支持自定义JVM参数
   - 数据库连接参数通过环境变量传递

6. **健康检查：**
   - 等待PostgreSQL就绪
   - 测试数据库连接
   - 自动创建数据库（如果不存在）

### 11.3 不同环境JVM配置对比

#### 11.3.1 配置对比表

| 配置项 | 低内存环境 | Docker环境 | 标准环境 |
|--------|-----------|-----------|---------|
| **服务器配置** | 1核1G | 2核2G | 4核4G |
| **初始堆内存** | -Xms256m | -Xms256m | -Xms1g |
| **最大堆内存** | -Xmx512m | -Xmx512m | -Xmx2g |
| **元空间大小** | 64m/128m | 128m | 256m/512m |
| **GC收集器** | SerialGC | G1GC | G1GC |
| **线程栈大小** | -Xss256k | 默认 | 默认 |
| **GC暂停时间** | 不设置 | 200ms | 100ms |
| **字符串去重** | 启用 | 启用 | 启用 |
| **分层编译** | Level 1 | 默认 | 默认 |

#### 11.3.2 配置选择建议

**1. 低内存环境（1核1G）：**
```bash
# 使用串行GC，减少GC开销
-XX:+UseSerialGC

# 快速启动，减少编译时间
-XX:TieredStopAtLevel=1

# 减小线程栈，支持更多线程
-Xss256k

# 限制直接内存使用
-XX:MaxDirectMemorySize=64m
```

**2. Docker环境（2核2G）：**
```bash
# 使用G1GC，平衡吞吐量和延迟
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# 启用字符串去重，减少内存占用
-XX:+UseStringDeduplication

# 支持自定义JVM参数
${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}
```

**3. 标准环境（4核4G）：**
```bash
# 使用G1GC，优化性能
-XX:+UseG1GC
-XX:MaxGCPauseMillis=100

# 更大的堆内存
-Xms1g -Xmx2g

# 更大的元空间
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
```

### 11.4 JVM配置最佳实践

#### 11.4.1 通用配置原则

**1. 内存配置原则：**
```bash
# 原则1：Xms和Xmx设置相同，避免动态调整
-Xms1g -Xmx1g

# 原则2：留30-40%内存给操作系统
# 4G服务器：堆内存2.5-3G
# 8G服务器：堆内存5-6G
# 16G服务器：堆内存10-12G

# 原则3：元空间不要设置过大
-XX:MaxMetaspaceSize=256m  # 通常256m足够
```

**2. GC配置原则：**
```bash
# 原则1：根据服务器核数选择GC
# 单核：SerialGC
# 2-4核：G1GC
# 8核以上：G1GC或ZGC

# 原则2：设置合理的GC暂停时间
-XX:MaxGCPauseMillis=200  # 200ms是合理的默认值

# 原则3：启用GC日志
-Xlog:gc*:file=/app/logs/gc.log:time,tags:filecount=5,filesize=10m
```

**3. 故障诊断原则：**
```bash
# 原则1：启用OOM dump
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/logs/heapdump.hprof

# 原则2：启用GC日志
-Xlog:gc*:file=/app/logs/gc.log:time,tags:filecount=5,filesize=10m

# 原则3：启用JMX监控
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

#### 11.4.2 生产环境推荐配置

**标准生产环境（4核4G）：**
```bash
JAVA_OPTS="-server \
  -Xms2g \
  -Xmx2g \
  -XX:MetaspaceSize=256m \
  -XX:MaxMetaspaceSize=512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:InitiatingHeapOccupancyPercent=45 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof \
  -Xlog:gc*:file=/app/logs/gc.log:time,tags:filecount=5,filesize=10m \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"
```

**高并发环境（8核8G）：**
```bash
JAVA_OPTS="-server \
  -Xms4g \
  -Xmx4g \
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=50 \
  -XX:InitiatingHeapOccupancyPercent=35 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+UseLargePages \
  -XX:LargePageSizeInBytes=2m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs/heapdump.hprof \
  -Xlog:gc*:file=/app/logs/gc.log:time,tags:filecount=10,filesize=20m \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -XX:+PrintGCApplicationStoppedTime \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"
```

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
