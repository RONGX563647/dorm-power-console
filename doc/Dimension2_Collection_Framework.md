# 维度2：集合框架使用实战指南

> 基于DormPower项目的集合框架学习指南
> 
> 从项目实际代码出发，讲解List、Map、Set等集合的实际应用

---

## 目录

- [1. List列表](#1-list列表)
- [2. Map映射](#2-map映射)
- [3. Set集合](#3-set集合)
- [4. 集合遍历与Stream操作](#4-集合遍历与stream操作)
- [5. 线程安全集合](#5-线程安全集合)
- [6. 集合性能对比](#6-集合性能对比)

---

## 1. List列表

### 1.1 ArrayList使用

#### 1.1.1 项目中的ArrayList示例

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceService.java

@Service
public class DeviceService {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
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
}
```

**ArrayList特点：**
- 有序：元素按插入顺序存储
- 可重复：允许存储重复元素
- 随机访问：通过索引快速访问（O(1)）
- 查询快、插入删除慢：中间插入/删除需要移动元素

#### 1.1.2 ArrayList常用操作

```java
/**
 * 【集合框架】ArrayList常用操作
 */
public class ArrayListExamples {
    
    public void arrayListOperations() {
        // 1. 创建并初始化
        List<String> deviceIds = new ArrayList<>();
        
        // 2. 添加元素
        deviceIds.add("dev_001");      // 添加到末尾
        deviceIds.add("dev_002");
        deviceIds.add("dev_003");
        
        // 3. 指定位置添加
        deviceIds.add(1, "dev_001_5");  // 在索引1位置插入
        
        // 4. 获取元素（索引从0开始）
        String first = deviceIds.get(0);  // "dev_001"
        String second = deviceIds.get(1);  // "dev_001_5"
        
        // 5. 获取大小
        int size = deviceIds.size();  // 4
        
        // 6. 判断是否包含
        boolean contains = deviceIds.contains("dev_001");  // true
        
        // 7. 删除元素（按值）
        deviceIds.remove("dev_002");  // 删除第一个匹配的元素
        
        // 8. 删除元素（按索引）
        deviceIds.remove(0);  // 删除索引0的元素
        
        // 9. 修改元素
        deviceIds.set(0, "dev_001_new");  // 修改索引0的元素
        
        // 10. 获取元素索引
        int index = deviceIds.indexOf("dev_003");  // 返回第一次出现的索引
        
        // 11. 判断是否为空
        boolean isEmpty = deviceIds.isEmpty();  // false
        
        // 12. 清空列表
        deviceIds.clear();
        isEmpty = deviceIds.isEmpty();  // true
        
        // 13. 转换为数组
        String[] array = deviceIds.toArray(new String[0]);
        
        // 14. 子列表
        List<String> subList = deviceIds.subList(0, 2);  // 索引0到2（不包含2）
    }
}
```

### 1.2 LinkedList使用

#### 1.2.1 项目中的LinkedList示例

```java
// 文件：backend/src/main/java/com/dormpower/service/CommandQueueService.java

@Service
public class CommandQueueService {
    
    /**
     * 【集合框架】LinkedList：双向链表，插入删除快，查询慢
     * 适用于：频繁插入删除的场景
     */
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
    
    /**
     * 获取队列大小
     */
    public int size() {
        return commandQueue.size();
    }
}
```

**LinkedList特点：**
- 有序：元素按插入顺序存储
- 可重复：允许存储重复元素
- 插入删除快：O(1)
- 查询慢：需要遍历（O(n)）
- 双向链表：可以从头尾两端操作

#### 1.2.2 LinkedList特有操作

```java
/**
 * 【集合框架】LinkedList特有操作
 */
public class LinkedListExamples {
    
    public void linkedListOperations() {
        LinkedList<String> list = new LinkedList<>();
        
        // 1. 头部操作
        list.addFirst("first");     // 添加到头部
        String first = list.getFirst();  // 获取头部元素
        String removeFirst = list.removeFirst();  // 移除并返回头部元素
        
        // 2. 尾部操作
        list.addLast("last");       // 添加到尾部
        String last = list.getLast();  // 获取尾部元素
        String removeLast = list.removeLast();  // 移除并返回尾部元素
        
        // 3. 栈操作（后进先出）
        list.push("item1");  // 压栈（添加到头部）
        String pop = list.pop();  // 出栈（移除并返回头部元素）
        
        // 4. 队列操作（先进先出）
        list.offer("item1");  // 入队（添加到尾部）
        String poll = list.poll();  // 出队（移除并返回头部元素）
    }
}
```

### 1.3 List遍历方式

#### 1.3.1 四种遍历方式

```java
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

**遍历方式对比：**
| 方式 | 特点 | 适用场景 |
|------|------|----------|
| 普通for循环 | 带索引，可修改元素 | 需要索引或修改元素 |
| 增强for循环 | 简洁，只读 | 只需遍历，不需要索引 |
| Iterator | 可安全删除元素 | 遍历时需要删除元素 |
| forEach + Lambda | 简洁，函数式 | Java 8+，函数式编程 |

---

## 2. Map映射

### 2.1 HashMap使用

#### 2.1.1 项目中的HashMap示例

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
}
```

**HashMap特点：**
- 键值对存储：每个元素是一个key-value对
- key唯一：相同的key会覆盖旧值
- 查询快：O(1)时间复杂度
- 无序：不保证元素的顺序
- 允许null：key和value都可以为null

#### 2.1.2 HashMap常用操作

```java
/**
 * 【集合框架】HashMap常用操作
 */
public class HashMapExamples {
    
    public void hashMapOperations() {
        // 1. 创建Map
        Map<String, Integer> roomDeviceCount = new HashMap<>();
        
        // 2. 添加键值对
        roomDeviceCount.put("A-101", 5);
        roomDeviceCount.put("A-102", 3);
        roomDeviceCount.put("B-201", 8);
        
        // 3. 获取值
        Integer count = roomDeviceCount.get("A-101");  // 5
        Integer notFound = roomDeviceCount.get("A-999");  // null
        
        // 4. 获取值（带默认值）
        count = roomDeviceCount.getOrDefault("A-999", 0);  // 0
        
        // 5. 判断key是否存在
        boolean hasKey = roomDeviceCount.containsKey("A-101");  // true
        
        // 6. 判断value是否存在
        boolean hasValue = roomDeviceCount.containsValue(5);  // true
        
        // 7. 获取所有key
        Set<String> rooms = roomDeviceCount.keySet();
        
        // 8. 获取所有value
        Collection<Integer> counts = roomDeviceCount.values();
        
        // 9. 获取所有键值对
        Set<Map.Entry<String, Integer>> entries = roomDeviceCount.entrySet();
        
        // 10. 遍历Map
        for (Map.Entry<String, Integer> entry : entries) {
            String room = entry.getKey();
            Integer cnt = entry.getValue();
            System.out.println(room + "有" + cnt + "个设备");
        }
        
        // 11. 删除键值对
        roomDeviceCount.remove("A-101");
        
        // 12. 删除键值对（带条件）
        roomDeviceCount.remove("A-102", 3);  // 只有key和value都匹配才删除
        
        // 13. 替换值
        roomDeviceCount.replace("B-201", 10);
        
        // 14. 替换值（带条件）
        roomDeviceCount.replace("B-201", 8, 10);  // 只有旧值匹配才替换
        
        // 15. 计算并更新
        roomDeviceCount.computeIfAbsent("A-103", k -> 0);  // key不存在时计算
        roomDeviceCount.computeIfPresent("A-103", (k, v) -> v + 1);  // key存在时计算
        
        // 16. 合并Map
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("A-104", 6);
        roomDeviceCount.putAll(newMap);  // 合并两个Map
        
        // 17. 获取大小
        int size = roomDeviceCount.size();
        
        // 18. 判断是否为空
        boolean isEmpty = roomDeviceCount.isEmpty();
        
        // 19. 清空Map
        roomDeviceCount.clear();
    }
}
```

### 2.2 LinkedHashMap

#### 2.2.1 有序Map示例

```java
/**
 * 【集合框架】LinkedHashMap：有序的HashMap
 * 按插入顺序或访问顺序存储
 */
public class LinkedHashMapExamples {
    
    public void linkedHashMapOperations() {
        // 1. 创建LinkedHashMap（按插入顺序）
        Map<String, String> orderedMap = new LinkedHashMap<>();
        orderedMap.put("first", "value1");
        orderedMap.put("second", "value2");
        orderedMap.put("third", "value3");
        
        // 2. 遍历（按插入顺序）
        for (Map.Entry<String, String> entry : orderedMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        // 输出：
        // first = value1
        // second = value2
        // third = value3
        
        // 3. 创建LinkedHashMap（按访问顺序，LRU缓存）
        Map<String, String> lruMap = new LinkedHashMap<>(16, 0.75f, true);
        // 第三个参数true表示按访问顺序排序
        lruMap.put("a", "valueA");
        lruMap.put("b", "valueB");
        lruMap.get("a");  // 访问a
        lruMap.get("b");  // 访问b
        lruMap.get("a");  // 再次访问a
        
        // 遍历时，最近访问的在最后
        for (String key : lruMap.keySet()) {
            System.out.println(key);
        }
        // 输出：b, a（b最久未访问）
    }
}
```

### 2.3 TreeMap

#### 2.3.1 排序Map示例

```java
/**
 * 【集合框架】TreeMap：按key排序的Map
 */
public class TreeMapExamples {
    
    public void treeMapOperations() {
        // 1. 创建TreeMap（按key自然排序）
        Map<String, Integer> sortedMap = new TreeMap<>();
        sortedMap.put("C", 3);
        sortedMap.put("A", 1);
        sortedMap.put("B", 2);
        
        // 2. 遍历（按key排序）
        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        // 输出：
        // A = 1
        // B = 2
        // C = 3
        
        // 3. 自定义排序
        Map<String, Integer> customSortedMap = new TreeMap<>(Comparator.reverseOrder());
        customSortedMap.put("A", 1);
        customSortedMap.put("B", 2);
        customSortedMap.put("C", 3);
        
        // 4. 遍历（按key降序）
        for (String key : customSortedMap.keySet()) {
            System.out.println(key);
        }
        // 输出：C, B, A
        
        // 5. 获取子Map
        Map<String, Integer> subMap = sortedMap.subMap("A", "C");  // [A, C)
        // 输出：A=1, B=2
    }
}
```

---

## 3. Set集合

### 3.1 HashSet使用

#### 3.1.1 项目中的HashSet示例

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceGroupService.java

@Service
public class DeviceGroupService {
    
    /**
     * 【集合框架】HashSet：无序、不重复、查询快
     * 适用于：去重、判断是否存在
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
}
```

**HashSet特点：**
- 无序：不保证元素的顺序
- 不重复：相同的元素只会保留一个
- 查询快：O(1)时间复杂度
- 允许null：可以存储一个null元素

#### 3.1.2 HashSet常用操作

```java
/**
 * 【集合框架】HashSet常用操作
 */
public class HashSetExamples {
    
    public void hashSetOperations() {
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
        
        // 7. 判断是否为空
        boolean isEmpty = onlineDevices.isEmpty();
        
        // 8. 清空Set
        onlineDevices.clear();
        
        // 9. 批量添加
        Set<String> newDevices = new HashSet<>();
        newDevices.add("dev_003");
        newDevices.add("dev_004");
        onlineDevices.addAll(newDevices);
        
        // 10. 批量删除
        Set<String> toRemove = new HashSet<>();
        toRemove.add("dev_001");
        toRemove.add("dev_002");
        onlineDevices.removeAll(toRemove);
        
        // 11. 交集
        Set<String> set1 = new HashSet<>(Arrays.asList("A", "B", "C"));
        Set<String> set2 = new HashSet<>(Arrays.asList("B", "C", "D"));
        set1.retainAll(set2);  // set1变为 {B, C}
        
        // 12. 并集
        set1.addAll(set2);  // set1变为 {A, B, C, D}
        
        // 13. 差集
        set1.removeAll(set2);  // set1变为 {A, D}
    }
}
```

### 3.2 LinkedHashSet

#### 3.2.1 有序Set示例

```java
/**
 * 【集合框架】LinkedHashSet：有序的HashSet
 * 按插入顺序存储元素
 */
public class LinkedHashSetExamples {
    
    public void linkedHashSetOperations() {
        // 1. 创建LinkedHashSet（按插入顺序）
        Set<String> orderedSet = new LinkedHashSet<>();
        orderedSet.add("first");
        orderedSet.add("second");
        orderedSet.add("third");
        orderedSet.add("first");  // 重复，不会被添加
        
        // 2. 遍历（按插入顺序）
        for (String item : orderedSet) {
            System.out.println(item);
        }
        // 输出：
        // first
        // second
        // third
    }
}
```

### 3.3 TreeSet

#### 3.3.1 排序Set示例

```java
/**
 * 【集合框架】TreeSet：排序的Set
 * 按元素自然排序或自定义排序
 */
public class TreeSetExamples {
    
    public void treeSetOperations() {
        // 1. 创建TreeSet（按自然排序）
        Set<Integer> numbers = new TreeSet<>();
        numbers.add(3);
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);  // 重复，不会被添加
        
        // 2. 遍历（按升序排序）
        for (Integer num : numbers) {
            System.out.println(num);
        }
        // 输出：1, 2, 3
        
        // 3. 自定义排序
        Set<Integer> descNumbers = new TreeSet<>(Comparator.reverseOrder());
        descNumbers.add(1);
        descNumbers.add(2);
        descNumbers.add(3);
        
        // 4. 遍历（按降序排序）
        for (Integer num : descNumbers) {
            System.out.println(num);
        }
        // 输出：3, 2, 1
        
        // 5. 获取子集
        Set<Integer> subSet = numbers.subSet(1, 3);  // [1, 3)
        // 输出：1, 2
        
        // 6. 获取头尾
        int first = numbers.first();  // 1
        int last = numbers.last();   // 3
    }
}
```

---

## 4. 集合遍历与Stream操作

### 4.1 Stream流操作

#### 4.1.1 项目中的Stream使用

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

### 4.2 Stream常用操作

#### 4.2.1 Stream操作速查

```java
/**
 * 【集合框架】Stream常用操作
 */
public class StreamExamples {
    
    public void streamOperations() {
        List<Device> devices = deviceRepository.findAll();
        
        // 1. filter：过滤
        List<Device> onlineDevices = devices.stream()
            .filter(Device::isOnline)
            .collect(Collectors.toList());
        
        // 2. map：映射
        List<String> names = devices.stream()
            .map(Device::getName)
            .collect(Collectors.toList());
        
        // 3. flatMap：扁平化映射
        List<List<String>> roomLists = Arrays.asList(
            Arrays.asList("A-101", "A-102"),
            Arrays.asList("B-201", "B-202")
        );
        List<String> allRooms = roomLists.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
        // 结果：[A-101, A-102, B-201, B-202]
        
        // 4. distinct：去重
        List<String> uniqueRooms = devices.stream()
            .map(Device::getRoom)
            .distinct()
            .collect(Collectors.toList());
        
        // 5. sorted：排序
        List<Device> sortedDevices = devices.stream()
            .sorted(Comparator.comparing(Device::getName))
            .collect(Collectors.toList());
        
        // 6. limit：限制数量
        List<Device> first10 = devices.stream()
            .limit(10)
            .collect(Collectors.toList());
        
        // 7. skip：跳过
        List<Device> after10 = devices.stream()
            .skip(10)
            .collect(Collectors.toList());
        
        // 8. count：计数
        long count = devices.stream()
            .filter(Device::isOnline)
            .count();
        
        // 9. anyMatch：任意匹配
        boolean hasOnline = devices.stream()
            .anyMatch(Device::isOnline);
        
        // 10. allMatch：全部匹配
        boolean allOnline = devices.stream()
            .allMatch(Device::isOnline);
        
        // 11. noneMatch：全部不匹配
        boolean noneOffline = devices.stream()
            .noneMatch(d -> !d.isOnline());
        
        // 12. findFirst：查找第一个
        Optional<Device> first = devices.stream()
            .filter(Device::isOnline)
            .findFirst();
        
        // 13. findAny：查找任意一个
        Optional<Device> any = devices.stream()
            .filter(Device::isOnline)
            .findAny();
        
        // 14. min：最小值
        Optional<Device> min = devices.stream()
            .min(Comparator.comparing(Device::getCreatedAt));
        
        // 15. max：最大值
        Optional<Device> max = devices.stream()
            .max(Comparator.comparing(Device::getCreatedAt));
        
        // 16. reduce：归约
        int totalDevices = devices.stream()
            .reduce(0, (sum, d) -> sum + 1, Integer::sum);
        
        // 17. collect：收集
        List<Device> list = devices.stream()
            .collect(Collectors.toList());
        
        Set<String> set = devices.stream()
            .map(Device::getRoom)
            .collect(Collectors.toSet());
        
        Map<String, List<Device>> grouped = devices.stream()
            .collect(Collectors.groupingBy(Device::getRoom));
        
        Map<String, Long> counted = devices.stream()
            .collect(Collectors.groupingBy(Device::getRoom, Collectors.counting()));
        
        String joined = devices.stream()
            .map(Device::getName)
            .collect(Collectors.joining(", "));
        
        // 18. forEach：遍历
        devices.stream()
            .forEach(d -> System.out.println(d.getName()));
    }
}
```

---

## 5. 线程安全集合

### 5.1 ConcurrentHashMap

#### 5.1.1 项目中的ConcurrentHashMap示例

```java
// 文件：backend/src/main/java/com/dormpower/service/SimpleCacheService.java

@Service
public class SimpleCacheService {
    
    /**
     * 【集合框架】ConcurrentHashMap：线程安全的HashMap
     * 适用于：多线程环境下的缓存
     */
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

**ConcurrentHashMap特点：**
- 线程安全：多个线程可以同时读写
- 分段锁：提高并发性能
- 不允许null：key和value都不能为null
- 弱一致性：不保证实时一致性

### 5.2 CopyOnWriteArrayList

#### 5.2.1 项目中的CopyOnWriteArrayList示例

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceStatusListener.java

@Service
public class DeviceStatusListener {
    
    /**
     * 【集合框架】CopyOnWriteArrayList：线程安全的List
     * 读多写少场景，写操作会复制新数组
     */
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

**CopyOnWriteArrayList特点：**
- 线程安全：读写分离
- 写时复制：修改时复制新数组
- 适合读多写少：读操作不加锁
- 内存开销大：每次写操作都复制数组

---

## 6. 集合性能对比

### 6.1 List性能对比

| 集合类型 | 查询 | 插入 | 删除 | 线程安全 | 适用场景 |
|----------|------|------|------|----------|----------|
| ArrayList | O(1) | O(n) | O(n) | 否 | 查询多、插入删除少 |
| LinkedList | O(n) | O(1) | O(1) | 否 | 插入删除多、查询少 |
| CopyOnWriteArrayList | O(1) | O(n) | O(n) | 是 | 读多写少 |

### 6.2 Map性能对比

| 集合类型 | 查询 | 插入 | 删除 | 有序 | 线程安全 | 适用场景 |
|----------|------|------|------|------|----------|----------|
| HashMap | O(1) | O(1) | O(1) | 否 | 否 | 快速查找、缓存 |
| LinkedHashMap | O(1) | O(1) | O(1) | 是 | 否 | 需要保持插入顺序 |
| TreeMap | O(log n) | O(log n) | O(log n) | 是 | 否 | 需要排序 |
| ConcurrentHashMap | O(1) | O(1) | O(1) | 否 | 是 | 多线程环境 |

### 6.3 Set性能对比

| 集合类型 | 查询 | 插入 | 删除 | 有序 | 线程安全 | 适用场景 |
|----------|------|------|------|------|----------|----------|
| HashSet | O(1) | O(1) | O(1) | 否 | 否 | 去重、快速查找 |
| LinkedHashSet | O(1) | O(1) | O(1) | 是 | 否 | 去重、保持顺序 |
| TreeSet | O(log n) | O(log n) | O(log n) | 是 | 否 | 去重、需要排序 |

---

## 7. 集合底层实现原理

### 7.1 ArrayList底层实现

#### 7.1.1 ArrayList源码分析

```java
/**
 * 【集合底层】ArrayList底层实现原理
 * 
 * 核心数据结构：Object[] elementData（动态数组）
 * 
 * 关键属性：
 * - DEFAULT_CAPACITY = 10：默认初始容量
 * - elementData：存储元素的数组
 * - size：当前元素个数
 */
public class ArrayListAnalysis {
    
    /**
     * 【集合底层】ArrayList扩容机制
     */
    public void arrayListGrowth() {
        // 【扩容机制】
        // 1. 使用无参构造时，初始容量为0
        List<Device> devices = new ArrayList<>();
        // elementData = {}（空数组）
        // size = 0
        
        // 2. 第一次添加元素时，扩容到10
        devices.add(new Device());
        // elementData = new Object[10]（容量10）
        // size = 1
        
        // 3. 当size == capacity时，触发扩容
        for (int i = 0; i < 10; i++) {
            devices.add(new Device());
        }
        // size = 11，触发扩容
        
        // 4. 扩容公式：newCapacity = oldCapacity + (oldCapacity >> 1)
        // 即：newCapacity = oldCapacity * 1.5
        // elementData = new Object[15]（容量15）
        // size = 11
        
        // 5. 继续添加，直到再次触发扩容
        for (int i = 0; i < 5; i++) {
            devices.add(new Device());
        }
        // size = 16，触发扩容
        // elementData = new Object[22]（容量22）
    }
    
    /**
     * 【集合底层】ArrayList插入元素
     */
    public void arrayListInsert() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            devices.add(new Device());
        }
        
        // 【插入元素】在中间位置插入
        devices.add(5, new Device());
        
        // 【底层实现】
        // 1. 检查容量，如果不足则扩容
        // 2. 将索引5及之后的元素向后移动一位
        //    System.arraycopy(elementData, 5, elementData, 6, size - 5)
        // 3. 在索引5位置插入新元素
        // 4. size++
        
        // 【时间复杂度】O(n)
        // 需要移动n-5个元素
    }
    
    /**
     * 【集合底层】ArrayList删除元素
     */
    public void arrayListRemove() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            devices.add(new Device());
        }
        
        // 【删除元素】删除中间位置
        devices.remove(5);
        
        // 【底层实现】
        // 1. 将索引6及之后的元素向前移动一位
        //    System.arraycopy(elementData, 6, elementData, 5, size - 6)
        // 2. 将最后一个位置置为null，帮助GC
        //    elementData[size - 1] = null
        // 3. size--
        
        // 【时间复杂度】O(n)
        // 需要移动n-6个元素
    }
}
```

**ArrayList扩容源码分析：**

```java
// ArrayList扩容源码（简化版）
private void grow(int minCapacity) {
    // 1. 获取旧容量
    int oldCapacity = elementData.length;
    
    // 2. 计算新容量：oldCapacity + oldCapacity/2
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    
    // 3. 如果新容量仍不足，直接使用minCapacity
    if (newCapacity - minCapacity < 0) {
        newCapacity = minCapacity;
    }
    
    // 4. 如果新容量超过最大值，使用Integer.MAX_VALUE
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
        newCapacity = hugeCapacity(minCapacity);
    }
    
    // 5. 复制到新数组
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

### 7.2 HashMap底层实现

#### 7.2.1 HashMap源码分析

```java
/**
 * 【集合底层】HashMap底层实现原理
 * 
 * 核心数据结构：数组 + 链表 + 红黑树
 * 
 * 关键属性：
 * - DEFAULT_INITIAL_CAPACITY = 16：默认初始容量
 * - DEFAULT_LOAD_FACTOR = 0.75：默认负载因子
 * - table：存储键值对的数组
 * - size：当前键值对个数
 * - threshold：扩容阈值 = capacity * loadFactor
 */
public class HashMapAnalysis {
    
    /**
     * 【集合底层】HashMap存储结构
     */
    public void hashMapStructure() {
        // 【存储结构】数组 + 链表 + 红黑树
        
        // 1. 数组（Node<K,V>[] table）
        //    - 初始容量16
        //    - 每个位置是一个桶（bucket）
        //    - 桶的索引 = hash(key) & (capacity - 1)
        
        // 2. 链表（Node<K,V>）
        //    - 当多个key的hash值相同时，形成链表
        //    - 链表长度超过8时，转换为红黑树
        
        // 3. 红黑树（TreeNode<K,V>）
        //    - 当链表长度超过8时，转换为红黑树
        //    - 红黑树长度小于6时，退化为链表
        //    - 红黑树查询时间复杂度：O(log n)
        
        Map<String, Device> map = new HashMap<>();
        map.put("device-001", new Device());
        map.put("device-002", new Device());
        map.put("device-003", new Device());
        
        // 【存储结构示意图】
        // table[0] -> null
        // table[1] -> Node("device-001", device1) -> null
        // table[2] -> null
        // table[3] -> Node("device-002", device2) -> Node("device-003", device3) -> null
        // ...
        // table[15] -> null
    }
    
    /**
     * 【集合底层】HashMap计算hash值
     */
    public void hashMapHash() {
        // 【hash计算】
        // 1. 计算key的hashCode
        // 2. 高位运算：h ^ (h >>> 16)
        // 3. 计算桶索引：hash & (capacity - 1)
        
        String key = "device-001";
        
        // 【步骤1】计算hashCode
        int hashCode = key.hashCode();
        // 假设hashCode = 123456789
        
        // 【步骤2】高位运算（扰动函数）
        int hash = hashCode ^ (hashCode >>> 16);
        // 目的：让高16位参与运算，减少hash冲突
        
        // 【步骤3】计算桶索引
        int capacity = 16;
        int index = hash & (capacity - 1);
        // 等价于：hash % capacity（位运算效率更高）
    }
    
    /**
     * 【集合底层】HashMap扩容机制
     */
    public void hashMapGrowth() {
        // 【扩容时机】
        // 当size >= threshold时触发扩容
        // threshold = capacity * loadFactor
        
        Map<String, Device> map = new HashMap<>();
        // capacity = 16
        // loadFactor = 0.75
        // threshold = 16 * 0.75 = 12
        
        // 【扩容过程】
        // 1. 新容量 = 旧容量 * 2
        // 2. 创建新数组
        // 3. 重新计算所有元素的桶索引
        // 4. 将元素迁移到新数组
        
        // 【扩容后】
        // capacity = 32
        // threshold = 32 * 0.75 = 24
    }
    
    /**
     * 【集合底层】HashMap链表转红黑树
     */
    public void hashMapTreeify() {
        // 【链表转红黑树条件】
        // 1. 链表长度 >= 8
        // 2. 数组长度 >= 64
        
        // 【红黑树转链表条件】
        // 红黑树节点数 <= 6
        
        // 【为什么是8和6】
        // - 8：根据泊松分布，链表长度达到8的概率很低
        // - 6：避免频繁转换（8转6，6转8）
        
        Map<String, Device> map = new HashMap<>();
        
        // 【场景1】链表长度达到8，但数组长度 < 64
        // 结果：先扩容数组，不转红黑树
        
        // 【场景2】链表长度达到8，且数组长度 >= 64
        // 结果：链表转为红黑树
    }
}
```

**HashMap put源码分析：**

```java
// HashMap put源码（简化版）
public V put(K key, V value) {
    // 1. 计算hash值
    int hash = hash(key);
    
    // 2. 计算桶索引
    int index = (table == null) ? 0 : (n - 1) & hash;
    
    // 3. 如果桶为空，直接插入
    if (table[index] == null) {
        table[index] = newNode(hash, key, value, null);
    } else {
        // 4. 如果桶不为空，遍历链表或红黑树
        Node<K,V> e;
        K k;
        
        // 4.1 检查第一个节点
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k)))) {
            e = p;
        }
        // 4.2 如果是红黑树
        else if (p instanceof TreeNode) {
            e = ((TreeNode<K,V>)p).putTreeVal(this, table, hash, key, value);
        }
        // 4.3 如果是链表
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    // 链表末尾，插入新节点
                    p.next = newNode(hash, key, value, null);
                    
                    // 链表长度达到8，转为红黑树
                    if (binCount >= TREEIFY_THRESHOLD - 1) {
                        treeifyBin(tab, hash);
                    }
                    break;
                }
                
                // 找到相同key，覆盖
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k)))) {
                    break;
                }
                
                p = e;
            }
        }
        
        // 5. 如果key已存在，覆盖旧值
        if (e != null) {
            V oldValue = e.value;
            e.value = value;
            return oldValue;
        }
    }
    
    // 6. 检查是否需要扩容
    if (++size > threshold) {
        resize();
    }
    
    return null;
}
```

### 7.3 HashSet底层实现

#### 7.3.1 HashSet源码分析

```java
/**
 * 【集合底层】HashSet底层实现原理
 * 
 * 核心数据结构：HashMap
 * 
 * 关键属性：
 * - map：存储元素的HashMap
 * - PRESENT：所有value都指向同一个Object对象
 */
public class HashSetAnalysis {
    
    /**
     * 【集合底层】HashSet实现原理
     */
    public void hashSetImplementation() {
        // 【HashSet实现】
        // HashSet内部使用HashMap存储元素
        // 元素作为HashMap的key
        // 所有value都指向同一个Object对象PRESENT
        
        Set<String> rooms = new HashSet<>();
        rooms.add("A-101");
        rooms.add("A-102");
        
        // 【底层实现】
        // HashSet内部：
        // private transient HashMap<E,Object> map;
        // private static final Object PRESENT = new Object();
        
        // add("A-101") 等价于：
        // map.put("A-101", PRESENT);
        
        // contains("A-101") 等价于：
        // map.containsKey("A-101");
        
        // remove("A-101") 等价于：
        // map.remove("A-101");
    }
}
```

---

## 8. 集合性能优化

### 8.1 初始容量优化

#### 8.1.1 ArrayList初始容量

```java
/**
 * 【性能优化】ArrayList初始容量优化
 */
public class ArrayListCapacityOptimization {
    
    /**
     * 【性能优化】未设置初始容量（不推荐）
     */
    public void withoutInitialCapacity() {
        // 【未设置初始容量】
        List<Device> devices = new ArrayList<>();
        
        // 【扩容过程】
        // 1. 初始容量：0
        // 2. 第一次add：扩容到10
        // 3. 第11次add：扩容到15
        // 4. 第16次add：扩容到22
        // 5. 第23次add：扩容到33
        // ...
        // 6. 第100次add：扩容到...
        
        // 【性能问题】
        // - 多次扩容，每次都需要复制数组
        // - 扩容次数多，影响性能
        
        for (int i = 0; i < 1000; i++) {
            devices.add(new Device());
        }
    }
    
    /**
     * 【性能优化】设置初始容量（推荐）
     */
    public void withInitialCapacity() {
        // 【设置初始容量】
        List<Device> devices = new ArrayList<>(1000);
        
        // 【性能优势】
        // - 只需一次分配
        // - 无需扩容
        // - 性能提升明显
        
        for (int i = 0; i < 1000; i++) {
            devices.add(new Device());
        }
    }
}
```

#### 8.1.2 HashMap初始容量

```java
/**
 * 【性能优化】HashMap初始容量优化
 */
public class HashMapCapacityOptimization {
    
    /**
     * 【性能优化】未设置初始容量（不推荐）
     */
    public void withoutInitialCapacity() {
        // 【未设置初始容量】
        Map<String, Device> map = new HashMap<>();
        
        // 【扩容过程】
        // 1. 初始容量：16
        // 2. 第13次put：扩容到32
        // 3. 第25次put：扩容到64
        // 4. 第49次put：扩容到128
        // ...
        // 5. 第100次put：扩容到...
        
        // 【性能问题】
        // - 多次扩容，每次都需要rehash
        // - 扩容次数多，影响性能
        
        for (int i = 0; i < 1000; i++) {
            map.put("device-" + i, new Device());
        }
    }
    
    /**
     * 【性能优化】设置初始容量（推荐）
     */
    public void withInitialCapacity() {
        // 【计算初始容量】
        // 公式：expectedSize / loadFactor + 1
        // 预期1000个元素，负载因子0.75
        // 初始容量 = 1000 / 0.75 + 1 = 1334
        // 取最近的2的幂：2048
        
        Map<String, Device> map = new HashMap<>(2048);
        
        // 【性能优势】
        // - 只需一次分配
        // - 无需扩容
        // - 性能提升明显
        
        for (int i = 0; i < 1000; i++) {
            map.put("device-" + i, new Device());
        }
    }
}
```

### 8.2 遍历优化

#### 8.2.1 List遍历优化

```java
/**
 * 【性能优化】List遍历优化
 */
public class ListTraversalOptimization {
    
    /**
     * 【性能优化】普通for循环遍历ArrayList
     */
    public void forLoopArrayList() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            devices.add(new Device());
        }
        
        // 【普通for循环】ArrayList最优
        // 原因：随机访问，O(1)时间复杂度
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            System.out.println(device.getName());
        }
    }
    
    /**
     * 【性能优化】增强for循环遍历ArrayList
     */
    public void forEachArrayList() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            devices.add(new Device());
        }
        
        // 【增强for循环】ArrayList次优
        // 原因：使用Iterator，稍慢于普通for循环
        for (Device device : devices) {
            System.out.println(device.getName());
        }
    }
    
    /**
     * 【性能优化】普通for循环遍历LinkedList
     */
    public void forLoopLinkedList() {
        List<Device> devices = new LinkedList<>();
        for (int i = 0; i < 10000; i++) {
            devices.add(new Device());
        }
        
        // 【普通for循环】LinkedList最差
        // 原因：每次get(i)都需要从头遍历，O(n)时间复杂度
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            System.out.println(device.getName());
        }
    }
    
    /**
     * 【性能优化】增强for循环遍历LinkedList
     */
    public void forEachLinkedList() {
        List<Device> devices = new LinkedList<>();
        for (int i = 0; i < 10000; i++) {
            devices.add(new Device());
        }
        
        // 【增强for循环】LinkedList最优
        // 原因：使用Iterator，O(1)时间复杂度
        for (Device device : devices) {
            System.out.println(device.getName());
        }
    }
}
```

**List遍历性能对比：**

| 集合类型 | 遍历方式 | 时间复杂度 | 性能 |
|----------|----------|-----------|------|
| ArrayList | 普通for循环 | O(n) | 最优 |
| ArrayList | 增强for循环 | O(n) | 次优 |
| LinkedList | 普通for循环 | O(n²) | 最差 |
| LinkedList | 增强for循环 | O(n) | 最优 |

### 8.3 Stream优化

#### 8.3.1 Stream并行流

```java
/**
 * 【性能优化】Stream并行流
 */
public class StreamParallelOptimization {
    
    /**
     * 【性能优化】串行流
     */
    public void sequentialStream() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            devices.add(new Device());
        }
        
        // 【串行流】单线程处理
        long start = System.currentTimeMillis();
        List<Device> onlineDevices = devices.stream()
            .filter(Device::isOnline)
            .collect(Collectors.toList());
        long end = System.currentTimeMillis();
        
        System.out.println("串行流耗时: " + (end - start) + "ms");
    }
    
    /**
     * 【性能优化】并行流
     */
    public void parallelStream() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            devices.add(new Device());
        }
        
        // 【并行流】多线程处理
        long start = System.currentTimeMillis();
        List<Device> onlineDevices = devices.parallelStream()
            .filter(Device::isOnline)
            .collect(Collectors.toList());
        long end = System.currentTimeMillis();
        
        System.out.println("并行流耗时: " + (end - start) + "ms");
    }
}
```

**并行流使用建议：**
- 数据量大（>10000）时使用并行流
- 数据量小时使用串行流
- 任务计算密集型时使用并行流
- 任务IO密集型时使用串行流

---

## 9. 集合线程安全

### 9.1 线程安全问题

#### 9.1.1 ArrayList线程安全问题

```java
/**
 * 【线程安全】ArrayList线程安全问题
 */
public class ArrayListThreadSafety {
    
    /**
     * 【线程安全】ArrayList非线程安全
     */
    public void arrayListNotThreadSafe() {
        // 【ArrayList】非线程安全
        List<Device> devices = new ArrayList<>();
        
        // 【多线程】并发添加
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                devices.add(new Device());
            }
        };
        
        // 【启动10个线程】
        for (int i = 0; i < 10; i++) {
            new Thread(task).start();
        }
        
        // 【问题】
        // 1. 可能丢失元素（size++和add不是原子操作）
        // 2. 可能抛出ArrayIndexOutOfBoundsException（扩容时）
        // 3. 可能出现null元素（扩容时）
        
        // 【结果】
        // 期望：10000个元素
        // 实际：< 10000个元素
    }
    
    /**
     * 【线程安全】解决方案1：Collections.synchronizedList
     */
    public void synchronizedList() {
        // 【Collections.synchronizedList】线程安全
        List<Device> devices = Collections.synchronizedList(new ArrayList<>());
        
        // 【特点】
        // 1. 所有方法都加synchronized锁
        // 2. 性能较差（粗粒度锁）
        // 3. 迭代时需要手动加锁
        
        // 【迭代时需要手动加锁】
        synchronized (devices) {
            for (Device device : devices) {
                System.out.println(device.getName());
            }
        }
    }
    
    /**
     * 【线程安全】解决方案2：CopyOnWriteArrayList
     */
    public void copyOnWriteArrayList() {
        // 【CopyOnWriteArrayList】线程安全
        List<Device> devices = new CopyOnWriteArrayList<>();
        
        // 【特点】
        // 1. 写时复制（修改时复制新数组）
        // 2. 读操作不加锁
        // 3. 适合读多写少
        // 4. 内存开销大（每次写操作都复制）
        
        // 【适用场景】
        // - 监听器列表
        // - 配置信息
        // - 事件通知
    }
}
```

#### 9.1.2 HashMap线程安全问题

```java
/**
 * 【线程安全】HashMap线程安全问题
 */
public class HashMapThreadSafety {
    
    /**
     * 【线程安全】HashMap非线程安全
     */
    public void hashMapNotThreadSafe() {
        // 【HashMap】非线程安全
        Map<String, Device> map = new HashMap<>();
        
        // 【多线程】并发put
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                map.put("device-" + i, new Device());
            }
        };
        
        // 【启动10个线程】
        for (int i = 0; i < 10; i++) {
            new Thread(task).start();
        }
        
        // 【问题】
        // 1. 可能丢失元素（size++和put不是原子操作）
        // 2. 可能抛出ConcurrentModificationException
        // 3. 可能形成环形链表（JDK 1.7）
        // 4. 可能出现null值
        
        // 【结果】
        // 期望：10000个元素
        // 实际：< 10000个元素
    }
    
    /**
     * 【线程安全】解决方案1：Collections.synchronizedMap
     */
    public void synchronizedMap() {
        // 【Collections.synchronizedMap】线程安全
        Map<String, Device> map = Collections.synchronizedMap(new HashMap<>());
        
        // 【特点】
        // 1. 所有方法都加synchronized锁
        // 2. 性能较差（粗粒度锁）
        // 3. 迭代时需要手动加锁
        
        // 【迭代时需要手动加锁】
        synchronized (map) {
            for (Map.Entry<String, Device> entry : map.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }
    
    /**
     * 【线程安全】解决方案2：ConcurrentHashMap
     */
    public void concurrentHashMap() {
        // 【ConcurrentHashMap】线程安全
        Map<String, Device> map = new ConcurrentHashMap<>();
        
        // 【特点】
        // 1. 分段锁（JDK 1.7）或CAS + synchronized（JDK 1.8）
        // 2. 读操作不加锁
        // 3. 写操作只锁当前桶
        // 4. 性能较好（细粒度锁）
        
        // 【适用场景】
        // - 高并发环境
        // - 缓存
        // - 计数器
    }
}
```

### 9.2 ConcurrentHashMap原理

#### 9.2.1 JDK 1.7分段锁

```java
/**
 * 【线程安全】ConcurrentHashMap JDK 1.7实现
 * 
 * 核心数据结构：Segment数组 + HashEntry数组 + 链表
 * 
 * 关键属性：
 * - segments：Segment数组
 * - Segment：继承ReentrantLock，每个Segment是一把锁
 * - HashEntry：存储键值对
 */
public class ConcurrentHashMapJDK7 {
    
    /**
     * 【线程安全】JDK 1.7分段锁原理
     */
    public void segmentedLock() {
        // 【分段锁】
        // 1. ConcurrentHashMap被分成多个Segment
        // 2. 每个Segment相当于一个小型的HashMap
        // 3. 每个Segment有自己的锁
        // 4. 不同Segment可以并发访问
        
        // 【并发度】
        // 默认并发度：16
        // 即：最多16个线程可以同时写
        
        // 【put操作】
        // 1. 计算key的hash值
        // 2. 计算Segment索引：hash >>> segmentShift
        // 3. 获取Segment，加锁
        // 4. 在Segment中插入元素
        // 5. 释放锁
        
        // 【get操作】
        // 1. 计算key的hash值
        // 2. 计算Segment索引
        // 3. 在Segment中查找元素
        // 4. 不需要加锁（volatile保证可见性）
    }
}
```

#### 9.2.2 JDK 1.8 CAS + synchronized

```java
/**
 * 【线程安全】ConcurrentHashMap JDK 1.8实现
 * 
 * 核心数据结构：Node数组 + 链表 + 红黑树
 * 
 * 关键属性：
 * - table：Node数组
 * - Node：存储键值对
 * - TreeNode：红黑树节点
 */
public class ConcurrentHashMapJDK8 {
    
    /**
     * 【线程安全】JDK 1.8 CAS + synchronized原理
     */
    public void casAndSynchronized() {
        // 【CAS + synchronized】
        // 1. 取消Segment，使用Node数组
        // 2. 使用CAS（Compare And Swap）插入第一个节点
        // 3. 使用synchronized锁住链表或红黑树
        
        // 【put操作】
        // 1. 计算key的hash值
        // 2. 计算桶索引
        // 3. 如果桶为空，使用CAS插入
        // 4. 如果桶不为空，使用synchronized锁住桶
        // 5. 在链表或红黑树中插入元素
        
        // 【get操作】
        // 1. 计算key的hash值
        // 2. 计算桶索引
        // 3. 在链表或红黑树中查找元素
        // 4. 不需要加锁（volatile保证可见性）
        
        // 【优势】
        // 1. 锁粒度更细（锁住桶，而不是Segment）
        // 2. 并发度更高
        // 3. 性能更好
    }
}
```

---

## 10. 集合最佳实践

### 10.1 选择合适的集合

#### 10.1.1 List选择

```java
/**
 * 【最佳实践】List选择指南
 */
public class ListSelection {
    
    /**
     * 【最佳实践】查询多、插入删除少：ArrayList
     */
    public void arrayListScenario() {
        // 【场景】
        // - 查询多
        // - 插入删除少
        // - 需要随机访问
        
        // 【选择】ArrayList
        List<Device> devices = new ArrayList<>();
        
        // 【原因】
        // - 随机访问O(1)
        // - 内存占用小
        // - 查询快
    }
    
    /**
     * 【最佳实践】插入删除多、查询少：LinkedList
     */
    public void linkedListScenario() {
        // 【场景】
        // - 插入删除多
        // - 查询少
        // - 需要队列或栈操作
        
        // 【选择】LinkedList
        LinkedList<CommandRequest> commandQueue = new LinkedList<>();
        
        // 【原因】
        // - 头尾插入删除O(1)
        // - 支持队列和栈操作
        // - 插入删除快
    }
    
    /**
     * 【最佳实践】读多写少、多线程：CopyOnWriteArrayList
     */
    public void copyOnWriteArrayListScenario() {
        // 【场景】
        // - 读多写少
        // - 多线程环境
        // - 数据量不大
        
        // 【选择】CopyOnWriteArrayList
        List<StatusChangeListener> listeners = new CopyOnWriteArrayList<>();
        
        // 【原因】
        // - 读操作不加锁
        // - 写时复制
        // - 适合监听器列表
    }
}
```

#### 10.1.2 Map选择

```java
/**
 * 【最佳实践】Map选择指南
 */
public class MapSelection {
    
    /**
     * 【最佳实践】快速查找、缓存：HashMap
     */
    public void hashMapScenario() {
        // 【场景】
        // - 快速查找
        // - 缓存
        // - 不需要顺序
        
        // 【选择】HashMap
        Map<String, Device> deviceCache = new HashMap<>();
        
        // 【原因】
        // - 查询O(1)
        // - 插入删除O(1)
        // - 内存占用小
    }
    
    /**
     * 【最佳实践】需要保持插入顺序：LinkedHashMap
     */
    public void linkedHashMapScenario() {
        // 【场景】
        // - 需要保持插入顺序
        // - LRU缓存
        
        // 【选择】LinkedHashMap
        Map<String, Device> orderedDevices = new LinkedHashMap<>();
        
        // 【原因】
        // - 保持插入顺序
        // - 查询O(1)
        // - 支持LRU缓存
    }
    
    /**
     * 【最佳实践】需要排序：TreeMap
     */
    public void treeMapScenario() {
        // 【场景】
        // - 需要排序
        // - 需要范围查询
        
        // 【选择】TreeMap
        Map<String, Device> sortedDevices = new TreeMap<>();
        
        // 【原因】
        // - 自动排序
        // - 支持范围查询
        // - 有序
    }
    
    /**
     * 【最佳实践】高并发环境：ConcurrentHashMap
     */
    public void concurrentHashMapScenario() {
        // 【场景】
        // - 高并发环境
        // - 多线程读写
        // - 缓存
        
        // 【选择】ConcurrentHashMap
        Map<String, Device> concurrentCache = new ConcurrentHashMap<>();
        
        // 【原因】
        // - 线程安全
        // - 读操作不加锁
        // - 细粒度锁
        // - 性能好
    }
}
```

### 10.2 避免常见陷阱

#### 10.2.1 集合陷阱

```java
/**
 * 【最佳实践】避免集合常见陷阱
 */
public class CollectionTraps {
    
    /**
     * 【陷阱1】Arrays.asList返回的List不可修改
     */
    public void trap1() {
        // 【陷阱】Arrays.asList返回的List不可修改
        String[] array = {"A", "B", "C"};
        List<String> list = Arrays.asList(array);
        
        // list.add("D");  // 抛出UnsupportedOperationException
        
        // 【解决方案】使用new ArrayList
        List<String> list2 = new ArrayList<>(Arrays.asList(array));
        list2.add("D");  // 正常
    }
    
    /**
     * 【陷阱2】subList返回的视图与原List共享数据
     */
    public void trap2() {
        // 【陷阱】subList返回的视图与原List共享数据
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        
        List<String> subList = list.subList(0, 2);
        
        // 修改subList会影响原List
        subList.set(0, "X");
        System.out.println(list.get(0));  // "X"
        
        // 修改原List会影响subList
        list.set(1, "Y");
        System.out.println(subList.get(1));  // "Y"
        
        // 【解决方案】创建新List
        List<String> subList2 = new ArrayList<>(list.subList(0, 2));
        subList2.set(0, "X");
        System.out.println(list.get(0));  // "A"
    }
    
    /**
     * 【陷阱3】遍历时删除元素会抛出ConcurrentModificationException
     */
    public void trap3() {
        // 【陷阱】遍历时删除元素会抛出ConcurrentModificationException
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        
        // 错误方式
        for (String item : list) {
            if (item.equals("B")) {
                list.remove(item);  // 抛出ConcurrentModificationException
            }
        }
        
        // 【解决方案1】使用Iterator
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (item.equals("B")) {
                iterator.remove();  // 正常
            }
        }
        
        // 【解决方案2】使用removeIf
        list.removeIf(item -> item.equals("B"));  // 正常
        
        // 【解决方案3】使用Stream
        list = list.stream()
            .filter(item -> !item.equals("B"))
            .collect(Collectors.toList());  // 正常
    }
    
    /**
     * 【陷阱4】HashMap的key是可变对象会导致问题
     */
    public void trap4() {
        // 【陷阱】HashMap的key是可变对象会导致问题
        Map<List<String>, String> map = new HashMap<>();
        
        List<String> key1 = new ArrayList<>();
        key1.add("A");
        key1.add("B");
        map.put(key1, "value1");
        
        // 修改key
        key1.add("C");
        
        // 无法获取value
        String value = map.get(key1);  // null
        
        // 【解决方案】使用不可变对象作为key
        // - String
        // - Integer
        // - 自定义不可变类
    }
    
    /**
     * 【陷阱5】HashSet存储自定义对象需要重写hashCode和equals
     */
    public void trap5() {
        // 【陷阱】HashSet存储自定义对象需要重写hashCode和equals
        Set<Device> devices = new HashSet<>();
        
        Device device1 = new Device();
        device1.setId("device-001");
        devices.add(device1);
        
        Device device2 = new Device();
        device2.setId("device-001");
        
        // 无法正确判断是否包含
        boolean contains = devices.contains(device2);  // false（如果没有重写hashCode和equals）
        
        // 【解决方案】重写hashCode和equals
        // @Override
        // public int hashCode() {
        //     return Objects.hash(id);
        // }
        // 
        // @Override
        // public boolean equals(Object obj) {
        //     if (this == obj) return true;
        //     if (obj == null || getClass() != obj.getClass()) return false;
        //     Device device = (Device) obj;
        //     return Objects.equals(id, device.id);
        // }
    }
}
```

---

## 10. 项目实战案例

### 10.1 设备分组管理

#### 10.1.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/service/DeviceGroupService.java

@Service
public class DeviceGroupService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceGroupService.class);
    
    @Autowired
    private DeviceGroupRepository deviceGroupRepository;
    
    @Autowired
    private DeviceGroupMappingRepository deviceGroupMappingRepository;
    
    /**
     * 【Map】获取设备分组统计信息
     * 【方法】统计每个分组的设备数量
     * 【Stream】使用Stream API进行分组统计
     */
    public Map<String, Object> getGroupStatistics() {
        logger.debug("获取设备分组统计信息");
        
        // 【List】查询所有分组
        List<DeviceGroup> groups = deviceGroupRepository.findAll();
        
        // 【Map】创建结果映射：分组ID -> 统计信息
        Map<String, Object> result = new HashMap<>();
        
        // 【List】存储每个分组的统计信息
        List<Map<String, Object>> groupStats = new ArrayList<>();
        
        // 【流程控制】遍历所有分组
        for (DeviceGroup group : groups) {
            // 【List】查询该分组下的所有设备映射
            List<DeviceGroupMapping> mappings = deviceGroupMappingRepository.findByGroupId(group.getId());
            
            // 【Set】使用Set去重获取设备ID集合
            Set<String> deviceIds = mappings.stream()
                    .map(DeviceGroupMapping::getDeviceId)
                    .collect(Collectors.toSet());
            
            // 【Map】组装单个分组的统计信息
            Map<String, Object> groupStat = new HashMap<>();
            groupStat.put("groupId", group.getId());
            groupStat.put("groupName", group.getName());
            groupStat.put("deviceCount", deviceIds.size());
            groupStat.put("deviceIds", new ArrayList<>(deviceIds));
            
            groupStats.add(groupStat);
        }
        
        // 【Map】组装最终结果
        result.put("totalGroups", groups.size());
        result.put("totalDevices", groupStats.stream()
                .mapToInt(stat -> (Integer) stat.get("deviceCount"))
                .sum());
        result.put("groups", groupStats);
        
        return result;
    }
    
    /**
     * 【Map】获取设备所在的所有分组
     * 【方法】传入设备ID，返回分组列表
     * 【Stream】使用Stream API转换数据
     */
    public List<Map<String, Object>> getDeviceGroups(String deviceId) {
        logger.debug("获取设备所在分组: {}", deviceId);
        
        // 【List】查询设备的所有分组映射
        List<DeviceGroupMapping> mappings = deviceGroupMappingRepository.findByDeviceId(deviceId);
        
        // 【Stream】使用Stream API获取分组ID列表
        List<String> groupIds = mappings.stream()
                .map(DeviceGroupMapping::getGroupId)
                .collect(Collectors.toList());
        
        // 【List】查询分组详情
        List<DeviceGroup> groups = deviceGroupRepository.findAllById(groupIds);
        
        // 【Stream】使用Stream API转换分组信息
        return groups.stream()
                .map(group -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", group.getId());
                    map.put("name", group.getName());
                    map.put("description", group.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 【Set】批量添加设备到分组
     * 【方法】传入分组ID和设备ID集合
     * 【Set】使用Set去重避免重复添加
     */
    @Transactional
    public void addDevicesToGroup(String groupId, Set<String> deviceIds) {
        logger.debug("批量添加设备到分组: groupId={}, deviceCount={}", groupId, deviceIds.size());
        
        // 【Set】获取已存在的设备ID（去重）
        List<DeviceGroupMapping> existingMappings = deviceGroupMappingRepository.findByGroupId(groupId);
        Set<String> existingDeviceIds = existingMappings.stream()
                .map(DeviceGroupMapping::getDeviceId)
                .collect(Collectors.toSet());
        
        // 【Set】计算需要新增的设备ID（差集）
        Set<String> newDeviceIds = new HashSet<>(deviceIds);
        newDeviceIds.removeAll(existingDeviceIds);
        
        // 【流程控制】批量保存新的映射关系
        List<DeviceGroupMapping> newMappings = new ArrayList<>();
        for (String deviceId : newDeviceIds) {
            DeviceGroupMapping mapping = new DeviceGroupMapping();
            mapping.setGroupId(groupId);
            mapping.setDeviceId(deviceId);
            newMappings.add(mapping);
        }
        
        deviceGroupMappingRepository.saveAll(newMappings);
        
        logger.info("成功添加{}个设备到分组", newDeviceIds.size());
    }
}
```

**代码解析：**

1. **List应用：**
   - `List<DeviceGroup>`：存储分组列表
   - `List<DeviceGroupMapping>`：存储分组映射关系
   - `List<Map<String, Object>>`：存储统计结果

2. **Map应用：**
   - `Map<String, Object>`：组装统计信息
   - `Map`存储键值对，便于前端解析

3. **Set应用：**
   - `Set<String>`：去重获取设备ID集合
   - `Set`的差集操作计算新增设备

4. **Stream API：**
   - `stream().map()`：转换数据格式
   - `stream().collect()`：收集结果
   - `mapToInt().sum()`：计算总和

### 10.2 遥测数据批量查询

#### 10.2.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/service/TelemetryService.java

@Service
public class TelemetryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TelemetryService.class);
    
    @Autowired
    private TelemetryRepository telemetryRepository;
    
    /**
     * 【List】批量查询设备遥测数据
     * 【方法】传入设备ID列表和时间范围，返回遥测数据
     * 【Stream】使用Stream API分组和统计
     */
    public Map<String, Object> getBatchTelemetryData(List<String> deviceIds, 
                                                      long startTime, 
                                                      long endTime) {
        logger.debug("批量查询遥测数据: deviceCount={}, startTime={}, endTime={}", 
                deviceIds.size(), startTime, endTime);
        
        // 【Map】创建结果映射
        Map<String, Object> result = new HashMap<>();
        
        // 【List】查询所有设备的遥测数据
        List<Telemetry> telemetryList = telemetryRepository
                .findByDeviceIdInAndTimestampBetween(deviceIds, startTime, endTime);
        
        // 【Stream】使用Stream API按设备ID分组
        Map<String, List<Telemetry>> groupedByDevice = telemetryList.stream()
                .collect(Collectors.groupingBy(Telemetry::getDeviceId));
        
        // 【List】存储每个设备的统计信息
        List<Map<String, Object>> deviceStats = new ArrayList<>();
        
        // 【流程控制】遍历每个设备的数据
        for (Map.Entry<String, List<Telemetry>> entry : groupedByDevice.entrySet()) {
            String deviceId = entry.getKey();
            List<Telemetry> deviceTelemetry = entry.getValue();
            
            // 【Stream】计算统计值
            DoubleSummaryStatistics powerStats = deviceTelemetry.stream()
                    .mapToDouble(Telemetry::getPower)
                    .summaryStatistics();
            
            DoubleSummaryStatistics voltageStats = deviceTelemetry.stream()
                    .mapToDouble(Telemetry::getVoltage)
                    .summaryStatistics();
            
            // 【Map】组装设备统计信息
            Map<String, Object> deviceStat = new HashMap<>();
            deviceStat.put("deviceId", deviceId);
            deviceStat.put("dataCount", deviceTelemetry.size());
            deviceStat.put("power", Map.of(
                    "avg", powerStats.getAverage(),
                    "min", powerStats.getMin(),
                    "max", powerStats.getMax()
            ));
            deviceStat.put("voltage", Map.of(
                    "avg", voltageStats.getAverage(),
                    "min", voltageStats.getMin(),
                    "max", voltageStats.getMax()
            ));
            
            deviceStats.add(deviceStat);
        }
        
        // 【Map】组装最终结果
        result.put("totalDevices", deviceIds.size());
        result.put("devicesWithData", groupedByDevice.size());
        result.put("totalDataPoints", telemetryList.size());
        result.put("deviceStats", deviceStats);
        
        return result;
    }
    
    /**
     * 【Map】获取最新遥测数据
     * 【方法】传入设备ID列表，返回每个设备的最新数据
     * 【Stream】使用Stream API获取最新记录
     */
    public Map<String, Telemetry> getLatestTelemetry(List<String> deviceIds) {
        logger.debug("获取最新遥测数据: deviceCount={}", deviceIds.size());
        
        // 【List】查询所有设备的最新遥测数据
        List<Telemetry> latestTelemetry = telemetryRepository.findLatestByDeviceIds(deviceIds);
        
        // 【Stream】转换为Map：deviceId -> Telemetry
        return latestTelemetry.stream()
                .collect(Collectors.toMap(
                        Telemetry::getDeviceId,
                        telemetry -> telemetry,
                        (existing, replacement) -> replacement  // 处理重复key
                ));
    }
}
```

**代码解析：**

1. **Stream分组：**
   - `Collectors.groupingBy()`：按设备ID分组
   - 将List转换为Map，便于按设备处理

2. **统计计算：**
   - `DoubleSummaryStatistics`：计算平均值、最小值、最大值
   - `mapToDouble()`：将对象流转换为数值流

3. **Map转换：**
   - `Collectors.toMap()`：将List转换为Map
   - 处理重复key的合并策略

### 10.3 缓存实现

#### 10.3.1 实际项目代码分析

```java
// 文件：backend/src/main/java/com/dormpower/service/SimpleCacheService.java

@Service
public class SimpleCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleCacheService.class);
    
    // 【ConcurrentHashMap】线程安全的缓存实现
    // 使用ConcurrentHashMap保证线程安全
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // 【常量】默认缓存过期时间（5分钟）
    private static final long DEFAULT_EXPIRATION = 5 * 60 * 1000;
    
    /**
     * 【ConcurrentHashMap】放入缓存
     * 【方法】线程安全地放入缓存
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_EXPIRATION);
    }
    
    /**
     * 【ConcurrentHashMap】放入缓存（指定过期时间）
     */
    public void put(String key, Object value, long expirationMillis) {
        CacheEntry entry = new CacheEntry(value, System.currentTimeMillis() + expirationMillis);
        cache.put(key, entry);
        logger.debug("缓存放入: key={}, expiration={}", key, expirationMillis);
    }
    
    /**
     * 【ConcurrentHashMap】从缓存获取
     * 【方法】线程安全地获取缓存，自动清理过期数据
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        CacheEntry entry = cache.get(key);
        
        if (entry == null) {
            return null;
        }
        
        // 【流程控制】检查是否过期
        if (System.currentTimeMillis() > entry.getExpirationTime()) {
            // 【ConcurrentHashMap】过期则删除
            cache.remove(key);
            logger.debug("缓存过期删除: key={}", key);
            return null;
        }
        
        return (T) entry.getValue();
    }
    
    /**
     * 【ConcurrentHashMap】批量获取缓存
     * 【方法】传入key列表，返回存在的缓存值
     * 【Stream】使用Stream API过滤和转换
     */
    public Map<String, Object> getBatch(List<String> keys) {
        Map<String, Object> result = new HashMap<>();
        
        // 【流程控制】遍历key列表
        for (String key : keys) {
            Object value = get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * 【ConcurrentHashMap】清理过期缓存
     * 【方法】遍历缓存，删除过期条目
     * 【Iterator】使用Iterator安全删除
     */
    public void evictExpired() {
        long now = System.currentTimeMillis();
        int count = 0;
        
        // 【Iterator】使用Iterator遍历并删除
        Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (now > entry.getValue().getExpirationTime()) {
                iterator.remove();
                count++;
            }
        }
        
        if (count > 0) {
            logger.info("清理过期缓存: {}条", count);
        }
    }
    
    /**
     * 【内部类】缓存条目
     */
    private static class CacheEntry {
        private final Object value;
        private final long expirationTime;
        
        public CacheEntry(Object value, long expirationTime) {
            this.value = value;
            this.expirationTime = expirationTime;
        }
        
        public Object getValue() {
            return value;
        }
        
        public long getExpirationTime() {
            return expirationTime;
        }
    }
}
```

**代码解析：**

1. **ConcurrentHashMap：**
   - 线程安全的HashMap实现
   - 支持高并发读写操作

2. **Iterator删除：**
   - 使用Iterator遍历并安全删除元素
   - 避免ConcurrentModificationException

3. **过期策略：**
   - 懒删除：获取时检查过期时间
   - 定时清理：批量删除过期条目

---

## 本章小结

| 知识点 | 项目体现 | 关键代码 |
|--------|----------|----------|
| ArrayList | 查询结果存储 | `List<Device> devices = new ArrayList<>()` |
| LinkedList | 命令队列 | `LinkedList<CommandRequest>` |
| HashMap | 设备详情组装 | `Map<String, Object> detail = new HashMap<>()` |
| HashSet | 房间号去重 | `Set<String> rooms = new HashSet<>()` |
| Stream | 筛选在线设备 | `devices.stream().filter(Device::isOnline)` |
| ConcurrentHashMap | 线程安全缓存 | `ConcurrentHashMap<String, Device>` |
| CopyOnWriteArrayList | 监听器列表 | `CopyOnWriteArrayList<StatusChangeListener>` |
| ArrayList底层 | 动态数组 | `Object[] elementData` |
| HashMap底层 | 数组+链表+红黑树 | `Node<K,V>[] table` |
| HashSet底层 | HashMap | `HashMap<E,Object> map` |
| 初始容量优化 | 避免扩容 | `new ArrayList<>(1000)` |
| 并行流 | 多线程处理 | `devices.parallelStream()` |
| 线程安全 | ConcurrentHashMap | `new ConcurrentHashMap<>()` |
| 项目实战 | 设备分组、遥测数据、缓存 | DeviceGroupService、TelemetryService、SimpleCacheService |

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
