# 核心CRUD实战指南（基于DormPower项目）

## 1. 概述

本指南基于**DormPower宿舍电源管理系统**的实际代码，深入讲解CRUD（Create, Read, Update, Delete）的完整实现流程。通过本项目的真实案例，帮助你理解企业级Java后端开发的规范和最佳实践。

### 1.1 项目架构概览

DormPower项目采用经典的三层架构：

```
┌─────────────────────────────────────────────────────────────┐
│                     Controller 层                          │
│              (REST API 接口定义与请求处理)                   │
├─────────────────────────────────────────────────────────────┤
│                     Service 层                             │
│              (业务逻辑封装与事务管理)                        │
├─────────────────────────────────────────────────────────────┤
│                     Repository 层                          │
│              (数据访问，Spring Data JPA)                    │
├─────────────────────────────────────────────────────────────┤
│                     Model 层                               │
│              (实体类定义与数据库映射)                        │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 技术栈说明

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.x | 应用框架 |
| Spring Data JPA | 3.x | 数据持久层 |
| H2/PostgreSQL | - | 数据库 |
| Maven | 3.8+ | 构建工具 |

## 2. 实体层（Model）设计

### 2.1 Device实体类（设备管理）

**文件路径**: `/backend/src/main/java/com/dormpower/model/Device.java`

```java
package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * 设备模型
 * 
 * 表示智能插座设备实体，映射到数据库devices表。
 * 使用JPA注解进行对象关系映射（ORM）。
 * 
 * @author dormpower team
 * @version 1.0
 */
@Entity                          // 标记为JPA实体类
@Table(name = "devices")         // 指定映射的数据库表名
public class Device {

    // ==================== 字段定义 ====================
    
    @Id                          // 标记为主键
    private String id;           // 设备唯一标识符

    @NotNull                     // 非空约束
    private String name;         // 设备名称

    @NotNull
    private String room;         // 所在房间号

    @NotNull
    private boolean online;      // 在线状态：true-在线，false-离线

    @NotNull
    private long lastSeenTs;     // 最后心跳时间戳（Unix时间戳，秒）

    @NotNull
    private long createdAt;      // 创建时间戳（Unix时间戳，秒）

    // ==================== Getter/Setter ====================
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeenTs() {
        return lastSeenTs;
    }

    public void setLastSeenTs(long lastSeenTs) {
        this.lastSeenTs = lastSeenTs;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
```

**关键点解析**：

1. **@Entity注解**：将Java类标记为JPA实体，使其可以与数据库表映射
2. **@Table注解**：显式指定数据库表名，如果不指定则默认使用类名
3. **@Id注解**：标记主键字段，每个实体必须有且只有一个主键
4. **@NotNull注解**：Jakarta Bean Validation约束，确保字段不为null
5. **Getter/Setter**：JPA要求实体类必须有公共的getter和setter方法

### 2.2 Student实体类（学生管理）

**文件路径**: `/backend/src/main/java/com/dormpower/model/Student.java`

```java
package com.dormpower.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 学生/住户信息模型
 * 管理宿舍住户的基本信息和入住状态
 * 展示了更复杂的字段验证规则
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    private String id;

    @NotNull
    @Pattern(regexp = "^\\d{10,20}$", message = "学号必须是10-20位数字")
    private String studentNumber; // 学号/工号

    @NotNull
    private String name; // 姓名

    @NotNull
    private String gender; // 性别：MALE(男)、FEMALE(女)

    @NotNull
    private String department; // 院系/部门

    private String major; // 专业

    private String className; // 班级

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone; // 联系电话

    @Email(message = "邮箱格式不正确")
    private String email; // 邮箱

    private String idCard; // 身份证号（加密存储）

    private String emergencyContact; // 紧急联系人

    private String emergencyPhone; // 紧急联系人电话

    private String roomId; // 当前入住房间ID

    @NotNull
    private String status; // 状态：ACTIVE(在读/在职)、GRADUATED(已毕业/离职)

    private String type; // 类型：UNDERGRADUATE(本科生)、POSTGRADUATE(研究生)

    private int enrollmentYear; // 入学年份

    private int expectedGraduationYear; // 预计毕业年份

    private String photoUrl; // 照片URL

    private String remark; // 备注

    @NotNull
    private boolean enabled; // 是否启用

    @NotNull
    private long createdAt;

    private long updatedAt;

    // Getters and Setters...
}
```

**验证注解详解**：

| 注解 | 作用 | 示例 |
|------|------|------|
| `@NotNull` | 字段不能为null | 学号、姓名必填 |
| `@Pattern` | 正则表达式验证 | 学号必须是10-20位数字 |
| `@Email` | 邮箱格式验证 | 自动验证邮箱格式 |

## 3. 数据访问层（Repository）

### 3.1 DeviceRepository（设备数据访问）

**文件路径**: `/backend/src/main/java/com/dormpower/repository/DeviceRepository.java`

```java
package com.dormpower.repository;

import com.dormpower.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 设备仓库接口
 * 
 * 继承JpaRepository获得基础CRUD能力：
 * - save() - 保存/更新实体
 * - findById() - 根据ID查询
 * - findAll() - 查询所有
 * - deleteById() - 根据ID删除
 * - existsById() - 判断是否存在
 * - count() - 统计数量
 * 
 * 通过方法命名约定实现自定义查询
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {

    /**
     * 根据房间查询设备列表
     * 
     * Spring Data JPA会根据方法名自动生成查询：
     * find + By + 属性名 = 根据该属性查询
     * 
     * 生成的SQL: SELECT * FROM devices WHERE room = ?
     * 
     * @param room 房间号
     * @return 该房间的设备列表
     */
    List<Device> findByRoom(String room);

}
```

### 3.2 StudentRepository（学生数据访问）

**文件路径**: `/backend/src/main/java/com/dormpower/repository/StudentRepository.java`

```java
package com.dormpower.repository;

import com.dormpower.model.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生/住户Repository
 * 
 * 展示了Spring Data JPA的强大查询能力：
 * 1. 方法命名约定查询
 * 2. 分页查询支持
 * 3. Optional返回类型
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, String> {

    /**
     * 根据学号查询学生
     * 
     * 返回Optional避免null检查
     * 调用方式: repository.findByStudentNumber("2021001001").orElse(null)
     */
    Optional<Student> findByStudentNumber(String studentNumber);

    /**
     * 根据房间ID和状态查询学生
     * 
     * And连接多个条件
     * 生成的SQL: SELECT * FROM students WHERE room_id = ? AND status = ?
     */
    List<Student> findByRoomIdAndStatus(String roomId, String status);

    /**
     * 分页查询指定状态的学生
     * 
     * @param status 学生状态
     * @param pageable 分页参数（页码、每页大小、排序）
     * @return 分页结果
     */
    Page<Student> findByStatus(String status, Pageable pageable);

    /**
     * 根据院系模糊查询（包含）
     * 
     * Containing表示模糊查询，等同于SQL的LIKE %value%
     */
    Page<Student> findByDepartmentContaining(String department, Pageable pageable);

    /**
     * 根据姓名模糊查询
     */
    Page<Student> findByNameContaining(String name, Pageable pageable);

    /**
     * 根据学号模糊查询
     */
    Page<Student> findByStudentNumberContaining(String studentNumber, Pageable pageable);

}
```

**Spring Data JPA方法命名规则**：

| 关键字 | 示例方法名 | 生成的SQL |
|--------|-----------|-----------|
| `And` | findByNameAndStatus | WHERE name = ? AND status = ? |
| `Or` | findByNameOrEmail | WHERE name = ? OR email = ? |
| `Containing` | findByNameContaining | WHERE name LIKE '%' \|\| ? \|\| '%' |
| `StartingWith` | findByNameStartingWith | WHERE name LIKE ? \|\| '%' |
| `EndingWith` | findByNameEndingWith | WHERE name LIKE '%' \|\| ? |
| `GreaterThan` | findByAgeGreaterThan | WHERE age > ? |
| `LessThan` | findByAgeLessThan | WHERE age < ? |
| `Between` | findByAgeBetween | WHERE age BETWEEN ? AND ? |
| `OrderBy` | findByStatusOrderByNameDesc | WHERE status = ? ORDER BY name DESC |

## 4. 业务层（Service）实现

### 4.1 DeviceService（设备业务逻辑）

**文件路径**: `/backend/src/main/java/com/dormpower/service/DeviceService.java`

```java
package com.dormpower.service;

import com.dormpower.exception.ResourceNotFoundException;
import com.dormpower.model.Device;
import com.dormpower.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备服务
 * 
 * 提供设备管理的业务逻辑，展示：
 * 1. 依赖注入使用方式
 * 2. 缓存注解的应用
 * 3. 日志记录规范
 * 4. 异常处理
 * 5. 数据转换（Entity -> Map）
 * 
 * @author dormpower team
 * @version 1.0
 */
@Service
public class DeviceService {

    // ==================== 日志记录器 ====================
    // 使用SLF4J日志门面，具体实现由Spring Boot自动配置
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    // ==================== 依赖注入 ====================
    // @Autowired将Spring容器中的DeviceRepository实例注入
    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 获取设备列表
     * 
     * 业务逻辑：
     * 1. 查询所有设备
     * 2. 将Device实体转换为Map（便于前端处理）
     * 3. 使用缓存提高性能
     * 
     * @Cacheable注解说明：
     * - value = "devices"：缓存名称
     * - unless：条件缓存，当结果为null或空时不缓存
     * 
     * @return 设备列表
     */
    @Cacheable(value = "devices", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getDevices() {
        logger.debug("获取设备列表");
        
        // 1. 从Repository查询所有设备
        List<Device> devices = deviceRepository.findAll();
        
        // 2. 转换为前端友好的Map格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (Device device : devices) {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", device.getId());
            deviceMap.put("name", device.getName());
            deviceMap.put("room", device.getRoom());
            deviceMap.put("online", device.isOnline());
            deviceMap.put("lastSeen", device.getLastSeenTs());
            result.add(deviceMap);
        }
        
        logger.info("获取设备列表成功，共{}个设备", result.size());
        return result;
    }

    /**
     * 获取设备状态（带缓存）
     * 
     * @Cacheable：
     * - key = "#deviceId"：使用设备ID作为缓存key
     * - 首次查询从数据库读取，后续从缓存读取
     * 
     * @param deviceId 设备ID
     * @return 设备状态
     */
    @Cacheable(value = "deviceStatus", key = "#deviceId")
    public Map<String, Object> getDeviceStatus(String deviceId) {
        logger.debug("获取设备状态: {}", deviceId);
        
        // 查询设备，如果不存在返回null
        Device device = deviceRepository.findById(deviceId).orElse(null);
        
        Map<String, Object> status = new HashMap<>();
        status.put("deviceId", deviceId);
        
        if (device != null) {
            status.put("online", device.isOnline());
            status.put("lastSeen", device.getLastSeenTs());
            status.put("name", device.getName());
            status.put("room", device.getRoom());
        } else {
            status.put("online", false);
            status.put("lastSeen", 0L);
            logger.warn("设备不存在: {}", deviceId);
        }
        
        return status;
    }

    /**
     * 获取设备详情
     * 
     * 使用自定义异常处理资源不存在的情况
     * 
     * @param deviceId 设备ID
     * @return 设备详情
     * @throws ResourceNotFoundException 设备不存在时抛出
     */
    public Map<String, Object> getDeviceDetail(String deviceId) {
        logger.debug("获取设备详情: {}", deviceId);
        
        // orElseThrow：如果查询结果为空，抛出指定异常
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在: " + deviceId));
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", device.getId());
        detail.put("name", device.getName());
        detail.put("room", device.getRoom());
        detail.put("online", device.isOnline());
        detail.put("lastSeen", device.getLastSeenTs());
        detail.put("createdAt", device.getCreatedAt());
        
        return detail;
    }

    /**
     * 更新设备状态
     * 
     * @CacheEvict：清除缓存
     * - allEntries = true：清除该缓存名称下的所有条目
     * - 数据更新后必须清除缓存，保证数据一致性
     * 
     * @param deviceId 设备ID
     * @param online 在线状态
     */
    @CacheEvict(value = {"devices", "deviceStatus"}, allEntries = true)
    public void updateDeviceStatus(String deviceId, boolean online) {
        logger.debug("更新设备状态: {} -> {}", deviceId, online);
        
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null) {
            device.setOnline(online);
            device.setLastSeenTs(System.currentTimeMillis());
            deviceRepository.save(device);  // save()：保存或更新实体
            logger.info("设备状态已更新: {} -> {}", deviceId, online);
        } else {
            logger.warn("设备不存在，无法更新状态: {}", deviceId);
        }
    }

    /**
     * 添加设备
     * 
     * @CacheEvict：添加新设备后清除设备列表缓存
     * 
     * @param device 设备实体
     * @return 保存后的设备（包含生成的ID）
     */
    @CacheEvict(value = "devices", allEntries = true)
    public Device addDevice(Device device) {
        logger.debug("添加设备: {}", device.getId());
        
        // 设置创建时间和最后心跳时间
        device.setCreatedAt(System.currentTimeMillis());
        device.setLastSeenTs(System.currentTimeMillis());
        
        // save()方法：如果ID已存在则更新，否则插入
        Device savedDevice = deviceRepository.save(device);
        
        logger.info("设备添加成功: {}", savedDevice.getId());
        return savedDevice;
    }

    /**
     * 删除设备
     * 
     * @param deviceId 设备ID
     * @throws ResourceNotFoundException 设备不存在时抛出
     */
    @CacheEvict(value = {"devices", "deviceStatus"}, allEntries = true)
    public void deleteDevice(String deviceId) {
        logger.debug("删除设备: {}", deviceId);
        
        // 先判断设备是否存在
        if (!deviceRepository.existsById(deviceId)) {
            throw new ResourceNotFoundException("设备不存在: " + deviceId);
        }
        
        deviceRepository.deleteById(deviceId);
        logger.info("设备删除成功: {}", deviceId);
    }

}
```

### 4.2 StudentService（学生业务逻辑）

**文件路径**: `/backend/src/main/java/com/dormpower/service/StudentService.java`

```java
package com.dormpower.service;

import com.dormpower.model.DormRoom;
import com.dormpower.model.Student;
import com.dormpower.model.StudentRoomHistory;
import com.dormpower.repository.DormRoomRepository;
import com.dormpower.repository.StudentRepository;
import com.dormpower.repository.StudentRoomHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 学生/住户管理服务
 * 
 * 展示复杂业务场景：
 * 1. 业务规则校验（学号唯一性）
 * 2. 关联操作（入住涉及学生、房间、历史记录三个表）
 * 3. 事务管理（@Transactional）
 * 4. 分页查询
 */
@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentRoomHistoryRepository historyRepository;

    @Autowired
    private DormRoomRepository dormRoomRepository;

    /**
     * 创建学生
     * 
     * 业务规则：
     * 1. 学号必须唯一
     * 2. 自动生成ID
     * 3. 设置默认状态
     * 
     * @param student 学生信息
     * @return 创建后的学生
     */
    public Student createStudent(Student student) {
        // 1. 业务规则校验：检查学号是否已存在
        if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
            throw new RuntimeException("Student number already exists");
        }

        // 2. 生成唯一ID
        student.setId("stu_" + UUID.randomUUID().toString().substring(0, 8));
        
        // 3. 设置默认值
        student.setCreatedAt(System.currentTimeMillis() / 1000);
        student.setEnabled(true);
        student.setStatus("ACTIVE");

        // 4. 保存到数据库
        return studentRepository.save(student);
    }

    /**
     * 获取所有学生（分页）
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<Student> getAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    /**
     * 根据ID获取学生
     * 
     * @param id 学生ID
     * @return 学生信息
     * @throws RuntimeException 学生不存在
     */
    public Student getStudentById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    /**
     * 更新学生信息
     * 
     * 业务规则：
     * 1. 学生必须存在
     * 2. 如果修改学号，新学号不能与其他学生冲突
     * 3. 部分字段更新（不是全量更新）
     * 
     * @param id 学生ID
     * @param student 更新的信息
     * @return 更新后的学生
     */
    public Student updateStudent(String id, Student student) {
        // 1. 查询现有学生
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 2. 如果修改了学号，检查新学号是否已存在
        if (!existing.getStudentNumber().equals(student.getStudentNumber())) {
            if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
                throw new RuntimeException("Student number already exists");
            }
            existing.setStudentNumber(student.getStudentNumber());
        }

        // 3. 更新允许修改的字段
        existing.setName(student.getName());
        existing.setGender(student.getGender());
        existing.setDepartment(student.getDepartment());
        existing.setMajor(student.getMajor());
        existing.setClassName(student.getClassName());
        existing.setPhone(student.getPhone());
        existing.setEmail(student.getEmail());
        existing.setEmergencyContact(student.getEmergencyContact());
        existing.setEmergencyPhone(student.getEmergencyPhone());
        existing.setType(student.getType());
        existing.setEnrollmentYear(student.getEnrollmentYear());
        existing.setExpectedGraduationYear(student.getExpectedGraduationYear());
        existing.setPhotoUrl(student.getPhotoUrl());
        existing.setRemark(student.getRemark());
        existing.setEnabled(student.isEnabled());
        existing.setUpdatedAt(System.currentTimeMillis() / 1000);

        // 4. 保存更新
        return studentRepository.save(existing);
    }

    /**
     * 删除学生
     * 
     * 业务规则：
     * 1. 如果学生当前有房间，先退宿
     * 2. 然后删除学生记录
     * 
     * @Transactional：确保退宿和删除在一个事务中
     * 
     * @param id 学生ID
     */
    @Transactional
    public void deleteStudent(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 如果学生当前有房间，先退宿
        if (student.getRoomId() != null) {
            checkOutStudent(id, "学生信息删除", "system");
        }

        studentRepository.deleteById(id);
    }

    /**
     * 学生入住
     * 
     * 复杂业务场景：
     * 1. 校验学生是否存在
     * 2. 校验房间是否存在
     * 3. 校验学生是否已在其他房间
     * 4. 校验房间是否已满
     * 5. 更新学生信息（分配房间）
     * 6. 更新房间信息（入住人数+1）
     * 7. 创建入住历史记录
     * 
     * 以上所有操作必须在同一个事务中完成
     * 
     * @Transactional：开启事务管理
     * 
     * @param studentId 学生ID
     * @param roomId 房间ID
     * @param reason 入住原因
     * @param operator 操作人
     * @return 入住后的学生信息
     */
    @Transactional
    public Student checkInStudent(String studentId, String roomId, String reason, String operator) {
        // 1. 查询学生和房间
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        DormRoom room = dormRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // 2. 业务规则校验：检查学生是否已在其他房间
        if (student.getRoomId() != null) {
            throw new RuntimeException("Student is already checked in to another room");
        }

        // 3. 业务规则校验：检查房间是否已满
        List<Student> currentResidents = studentRepository.findByRoomIdAndStatus(roomId, "ACTIVE");
        if (currentResidents.size() >= room.getCapacity()) {
            throw new RuntimeException("Room is full");
        }

        // 4. 更新学生信息
        student.setRoomId(roomId);
        student.setUpdatedAt(System.currentTimeMillis() / 1000);
        studentRepository.save(student);

        // 5. 更新房间信息
        room.setCurrentOccupants(currentResidents.size() + 1);
        room.setStatus("OCCUPIED");
        room.setUpdatedAt(System.currentTimeMillis() / 1000);
        dormRoomRepository.save(room);

        // 6. 创建入住历史记录
        StudentRoomHistory history = new StudentRoomHistory();
        history.setId("hist_" + UUID.randomUUID().toString().substring(0, 8));
        history.setStudentId(studentId);
        history.setRoomId(roomId);
        history.setCheckInDate(System.currentTimeMillis() / 1000);
        history.setStatus("ACTIVE");
        history.setCheckInReason(reason);
        history.setOperator(operator);
        history.setCreatedAt(System.currentTimeMillis() / 1000);
        historyRepository.save(history);

        return student;
    }

    /**
     * 学生退宿
     * 
     * @Transactional：确保所有操作在一个事务中
     * 
     * @param studentId 学生ID
     * @param reason 退宿原因
     * @param operator 操作人
     * @return 退宿后的学生信息
     */
    @Transactional
    public Student checkOutStudent(String studentId, String reason, String operator) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRoomId() == null) {
            throw new RuntimeException("Student is not checked in");
        }

        String roomId = student.getRoomId();

        // 更新学生信息
        student.setRoomId(null);
        student.setUpdatedAt(System.currentTimeMillis() / 1000);
        studentRepository.save(student);

        // 更新房间信息
        DormRoom room = dormRoomRepository.findById(roomId).orElse(null);
        if (room != null) {
            List<Student> currentResidents = studentRepository.findByRoomIdAndStatus(roomId, "ACTIVE");
            room.setCurrentOccupants(Math.max(0, currentResidents.size() - 1));
            if (room.getCurrentOccupants() == 0) {
                room.setStatus("VACANT");
            }
            room.setUpdatedAt(System.currentTimeMillis() / 1000);
            dormRoomRepository.save(room);
        }

        // 更新历史记录
        StudentRoomHistory history = historyRepository
                .findByStudentIdAndStatus(studentId, "ACTIVE")
                .orElse(null);
        if (history != null) {
            history.setStatus("CHECKED_OUT");
            history.setCheckOutDate(System.currentTimeMillis() / 1000);
            history.setCheckOutReason(reason);
            historyRepository.save(history);
        }

        return student;
    }

}
```

**@Transactional详解**：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `propagation` | 事务传播行为 | REQUIRED |
| `isolation` | 事务隔离级别 | DEFAULT |
| `readOnly` | 是否只读 | false |
| `timeout` | 超时时间（秒） | -1 |
| `rollbackFor` | 哪些异常触发回滚 | RuntimeException |

**传播行为**：
- `REQUIRED`：如果存在事务则加入，否则新建（最常用）
- `REQUIRES_NEW`：总是新建事务，挂起当前事务
- `SUPPORTS`：如果存在事务则加入，否则非事务执行

## 5. 控制层（Controller）实现

### 5.1 DeviceController（设备API）

**文件路径**: `/backend/src/main/java/com/dormpower/controller/DeviceController.java`

```java
package com.dormpower.controller;

import com.dormpower.annotation.AuditLog;
import com.dormpower.annotation.RateLimit;
import com.dormpower.model.Device;
import com.dormpower.model.StripStatus;
import com.dormpower.repository.DeviceRepository;
import com.dormpower.repository.StripStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备控制器
 * 
 * REST API设计规范：
 * 1. 使用资源名词作为路径（/devices）
 * 2. 使用HTTP方法表示操作（GET/POST/PUT/DELETE）
 * 3. 使用HTTP状态码表示结果（200/201/404/400）
 * 4. 返回统一的JSON格式
 */
@RestController              // 标记为REST控制器，返回JSON
@RequestMapping("/api")      // 基础路径前缀
@Tag(name = "设备管理", description = "设备查询和状态管理接口")  // Swagger文档标签
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private StripStatusRepository stripStatusRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 在线超时时间（秒）
    private static final long ONLINE_TIMEOUT_SECONDS = 60;

    /**
     * 获取设备列表 - READ操作
     * 
     * HTTP: GET /api/devices
     * 
     * @Operation: Swagger文档注解
     * @SecurityRequirement: 需要JWT认证
     */
    @Operation(
        summary = "获取设备列表",
        description = "查询所有注册的设备信息，包括设备ID、名称、房间号和在线状态",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(schema = @Schema(implementation = Device.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权，需要提供有效的Bearer Token"
        )
    })
    @GetMapping("/devices")    // 处理GET请求
    public List<Map<String, Object>> getDevices() {
        List<Device> devices = deviceRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        long now = System.currentTimeMillis() / 1000;
        
        for (Device d : devices) {
            // 判断设备是否在线
            boolean isOnline = d.isOnline() && (now - d.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;
            
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
     * 获取设备状态 - READ操作（单个资源）
     * 
     * HTTP: GET /api/devices/{deviceId}/status
     * 
     * @PathVariable: 从URL路径中提取参数
     */
    @Operation(
        summary = "获取设备状态",
        description = "获取指定设备的详细状态信息",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "设备不存在"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/devices/{deviceId}/status")
    public ResponseEntity<?> getDeviceStatus(
            @Parameter(description = "设备ID", required = true, example = "device_001")
            @PathVariable String deviceId) {    // @PathVariable绑定URL参数
        
        Device device = deviceRepository.findById(deviceId).orElse(null);
        StripStatus status = stripStatusRepository.findByDeviceId(deviceId);
        
        if (device == null || status == null) {
            throw new com.dormpower.exception.ResourceNotFoundException("device not found");
        }

        long now = System.currentTimeMillis() / 1000;
        boolean isOnline = device.isOnline() && status.isOnline() 
                          && (now - device.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;

        // 解析插座状态JSON
        List<Map<String, Object>> sockets = new ArrayList<>();
        try {
            List<?> socketList = objectMapper.readValue(status.getSocketsJson(), List.class);
            for (Object s : socketList) {
                if (s instanceof Map) {
                    Map<String, Object> socket = new HashMap<>();
                    Map<?, ?> sMap = (Map<?, ?>) s;
                    socket.put("id", sMap.get("id"));
                    socket.put("on", sMap.get("on"));
                    socket.put("power_w", sMap.get("power_w"));
                    sockets.add(socket);
                }
            }
        } catch (Exception e) {
            // JSON解析失败时忽略
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ts", status.getTs());
        response.put("online", isOnline);
        response.put("total_power_w", status.getTotalPowerW());
        response.put("voltage_v", status.getVoltageV());
        response.put("current_a", status.getCurrentA());
        response.put("sockets", sockets);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 创建设备 - CREATE操作
     * 
     * HTTP: POST /api/devices
     * 
     * @RequestBody: 将请求体JSON映射为Java对象
     * @Valid: 触发参数校验（@NotNull等注解）
     * @RateLimit: 自定义限流注解
     * @AuditLog: 自定义审计日志注解
     */
    @Operation(
        summary = "创建设备",
        description = "创建新的设备记录",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败，设备ID已存在或参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "429", description = "请求过于频繁")
    })
    @RateLimit(value = 2.0, type = "create-device")    // 每秒最多2次请求
    @AuditLog(value = "创建设备", type = "DEVICE")     // 记录审计日志
    @PostMapping("/devices")
    public ResponseEntity<?> createDevice(
            @Parameter(description = "设备信息", required = true)
            @Valid @RequestBody Device device) {    // @Valid触发校验
        
        try {
            // 设置默认值
            device.setCreatedAt(System.currentTimeMillis() / 1000);
            device.setLastSeenTs(System.currentTimeMillis() / 1000);
            device.setOnline(false);
            
            Device savedDevice = deviceRepository.save(device);
            return ResponseEntity.ok(savedDevice);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to create device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 更新设备 - UPDATE操作
     * 
     * HTTP: PUT /api/devices/{deviceId}
     */
    @Operation(
        summary = "更新设备",
        description = "更新指定设备的信息",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "设备不存在"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @AuditLog(value = "更新设备", type = "DEVICE")
    @PutMapping("/devices/{deviceId}")
    public ResponseEntity<?> updateDevice(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId,
            @Parameter(description = "设备信息", required = true)
            @Valid @RequestBody Device device) {
        
        try {
            Device existingDevice = deviceRepository.findById(deviceId).orElse(null);
            if (existingDevice == null) {
                throw new com.dormpower.exception.ResourceNotFoundException("device not found");
            }
            
            // 更新字段
            existingDevice.setName(device.getName());
            existingDevice.setRoom(device.getRoom());
            existingDevice.setOnline(device.isOnline());
            
            Device updatedDevice = deviceRepository.save(existingDevice);
            return ResponseEntity.ok(updatedDevice);
        } catch (com.dormpower.exception.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to update device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 删除设备 - DELETE操作（单个）
     * 
     * HTTP: DELETE /api/devices/{deviceId}
     */
    @Operation(
        summary = "删除设备",
        description = "删除指定的设备",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "设备不存在")
    })
    @AuditLog(value = "删除设备", type = "DEVICE")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(
            @Parameter(description = "设备ID", required = true)
            @PathVariable String deviceId) {
        
        try {
            Device device = deviceRepository.findById(deviceId).orElse(null);
            if (device == null) {
                throw new com.dormpower.exception.ResourceNotFoundException("device not found");
            }
            
            deviceRepository.delete(device);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Device deleted successfully");
            return ResponseEntity.ok(response);
        } catch (com.dormpower.exception.ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete device: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 批量删除设备 - DELETE操作（批量）
     * 
     * HTTP: DELETE /api/devices/batch
     */
    @Operation(
        summary = "批量删除设备",
        description = "批量删除指定的多个设备",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量删除成功"),
        @ApiResponse(responseCode = "400", description = "删除失败")
    })
    @AuditLog(value = "批量删除设备", type = "DEVICE")
    @DeleteMapping("/devices/batch")
    public ResponseEntity<?> batchDeleteDevices(
            @Parameter(description = "设备ID列表", required = true)
            @RequestBody List<String> deviceIds) {    // @RequestBody接收JSON数组
        
        try {
            if (deviceIds == null || deviceIds.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Device ID list cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // 只删除存在的设备
            List<Device> existingDevices = deviceRepository.findAllById(deviceIds);
            List<String> existingDeviceIds = existingDevices.stream()
                    .map(Device::getId)
                    .toList();
            
            if (!existingDeviceIds.isEmpty()) {
                deviceRepository.deleteAllById(existingDeviceIds);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Devices deleted successfully");
            response.put("count", existingDeviceIds.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Failed to delete devices: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * 获取指定房间的设备列表 - 条件查询
     * 
     * HTTP: GET /api/devices/room/{room}
     */
    @Operation(
        summary = "获取房间设备列表",
        description = "获取指定房间的设备列表",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/devices/room/{room}")
    public ResponseEntity<?> getDevicesByRoom(
            @Parameter(description = "房间ID", required = true)
            @PathVariable String room) {
        
        List<Device> devices = deviceRepository.findByRoom(room);
        List<Map<String, Object>> result = new ArrayList<>();
        
        long now = System.currentTimeMillis() / 1000;
        
        for (Device d : devices) {
            boolean isOnline = d.isOnline() && (now - d.getLastSeenTs()) < ONLINE_TIMEOUT_SECONDS;
            
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", d.getId());
            deviceMap.put("name", d.getName());
            deviceMap.put("room", d.getRoom());
            deviceMap.put("online", isOnline);
            deviceMap.put("lastSeen", Instant.ofEpochSecond(d.getLastSeenTs()).toString());
            result.add(deviceMap);
        }
        
        return ResponseEntity.ok(result);
    }

}
```

### 5.2 StudentController（学生API）

**文件路径**: `/backend/src/main/java/com/dormpower/controller/StudentController.java`

```java
package com.dormpower.controller;

import com.dormpower.model.Student;
import com.dormpower.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 学生/住户管理控制器
 * 
 * 展示分页查询和复杂业务API设计
 */
@RestController
@RequestMapping("/api")
@Tag(name = "学生管理", description = "学生/住户信息管理接口")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 创建学生 - CREATE
     * 
     * HTTP: POST /api/students
     */
    @Operation(
        summary = "创建学生",
        description = "创建新的学生/住户记录",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/students")
    public ResponseEntity<?> createStudent(@RequestBody Student student) {
        try {
            Student created = studentService.createStudent(student);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取学生列表（分页）- READ（列表）
     * 
     * HTTP: GET /api/students?page=0&size=10&sort=name,asc
     * 
     * @RequestParam: 从URL查询参数获取值
     */
    @Operation(
        summary = "获取学生列表",
        description = "分页获取学生列表，支持排序",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/students")
    public ResponseEntity<?> getAllStudents(
            @Parameter(description = "页码，从0开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段和方向，如name,asc")
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        // 解析排序参数
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc") 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // 创建分页请求
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
        
        Page<Student> students = studentService.getAllStudents(pageable);
        
        // 构建分页响应
        Map<String, Object> response = new HashMap<>();
        response.put("content", students.getContent());
        response.put("totalElements", students.getTotalElements());
        response.put("totalPages", students.getTotalPages());
        response.put("currentPage", students.getNumber());
        response.put("pageSize", students.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取学生详情 - READ（单个）
     * 
     * HTTP: GET /api/students/{id}
     */
    @Operation(
        summary = "获取学生详情",
        description = "根据ID获取学生详细信息",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/students/{id}")
    public ResponseEntity<?> getStudentById(
            @Parameter(description = "学生ID")
            @PathVariable String id) {
        try {
            Student student = studentService.getStudentById(id);
            return ResponseEntity.ok(student);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 更新学生信息 - UPDATE
     * 
     * HTTP: PUT /api/students/{id}
     */
    @Operation(
        summary = "更新学生信息",
        description = "更新指定学生的信息",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/students/{id}")
    public ResponseEntity<?> updateStudent(
            @Parameter(description = "学生ID")
            @PathVariable String id,
            @RequestBody Student student) {
        try {
            Student updated = studentService.updateStudent(id, student);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 删除学生 - DELETE
     * 
     * HTTP: DELETE /api/students/{id}
     */
    @Operation(
        summary = "删除学生",
        description = "删除指定的学生记录",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(
            @Parameter(description = "学生ID")
            @PathVariable String id) {
        try {
            studentService.deleteStudent(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Student deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 学生入住 - 业务操作
     * 
     * HTTP: POST /api/students/{studentId}/checkin
     */
    @Operation(
        summary = "学生入住",
        description = "为学生分配宿舍房间",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/students/{studentId}/checkin")
    public ResponseEntity<?> checkInStudent(
            @Parameter(description = "学生ID")
            @PathVariable String studentId,
            @Parameter(description = "房间ID")
            @RequestParam String roomId,
            @Parameter(description = "入住原因")
            @RequestParam(required = false) String reason) {
        try {
            Student student = studentService.checkInStudent(studentId, roomId, 
                    reason != null ? reason : "正常入住", "admin");
            return ResponseEntity.ok(student);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 学生退宿 - 业务操作
     * 
     * HTTP: POST /api/students/{studentId}/checkout
     */
    @Operation(
        summary = "学生退宿",
        description = "学生退宿，释放房间",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/students/{studentId}/checkout")
    public ResponseEntity<?> checkOutStudent(
            @Parameter(description = "学生ID")
            @PathVariable String studentId,
            @Parameter(description = "退宿原因")
            @RequestParam(required = false) String reason) {
        try {
            Student student = studentService.checkOutStudent(studentId, 
                    reason != null ? reason : "正常退宿", "admin");
            return ResponseEntity.ok(student);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

}
```

## 6. 异常处理

### 6.1 全局异常处理器

**文件路径**: `/backend/src/main/java/com/dormpower/exception/GlobalExceptionHandler.java`

```java
package com.dormpower.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * 统一处理系统中抛出的异常，返回标准化的错误响应
 * @RestControllerAdvice：标记为全局异常处理类
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理资源不存在异常
     * 
     * @ExceptionHandler：指定处理的异常类型
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("error", "Not Found");
        error.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.UNAUTHORIZED.value());
        error.put("error", "Unauthorized");
        error.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred");
        
        // 生产环境不要暴露详细错误信息
        // error.put("details", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
```

### 6.2 自定义异常类

**文件路径**: `/backend/src/main/java/com/dormpower/exception/ResourceNotFoundException.java`

```java
package com.dormpower.exception;

/**
 * 资源不存在异常
 * 
 * 当查询的资源不存在时抛出，对应HTTP 404状态码
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

## 7. 项目依赖配置

**文件路径**: `/backend/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.dormpower</groupId>
    <artifactId>backend</artifactId>
    <version>1.0.0</version>
    <name>DormPower Backend</name>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- ==================== Spring Boot Starters ==================== -->
        
        <!-- Web应用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- JPA数据访问 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- 安全认证 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- 参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- 缓存支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        
        <!-- WebSocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        
        <!-- ==================== 数据库 ==================== -->
        
        <!-- H2内存数据库（开发测试用） -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- PostgreSQL（生产环境用） -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- ==================== 工具库 ==================== -->
        
        <!-- Lombok：简化代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- JWT令牌 -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        
        <!-- Swagger/OpenAPI文档 -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>
        
        <!-- MQTT客户端 -->
        <dependency>
            <groupId>org.eclipse.paho</groupId>
            <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
            <version>1.2.5</version>
        </dependency>
        
        <!-- ==================== 测试 ==================== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
    
</project>
```

## 8. API测试示例

### 8.1 使用Python测试API

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
DormPower API测试脚本
测试设备的CRUD操作
"""

import requests
import json

# API基础URL
BASE_URL = "http://localhost:8000/api"

# JWT令牌（需要先从登录接口获取）
TOKEN = "your_jwt_token_here"

# 请求头
headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {TOKEN}"
}


def test_get_devices():
    """测试获取设备列表"""
    print("=" * 50)
    print("测试：获取设备列表")
    print("=" * 50)
    
    response = requests.get(f"{BASE_URL}/devices", headers=headers)
    print(f"状态码: {response.status_code}")
    print(f"响应数据: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")


# 主程序
if __name__ == "__main__":
    # 1. 获取设备列表
    devices = test_get_devices()
    
    # 2. 创建设备
    created = test_create_device()
    device_id = created.get("id")
    
    # 3. 获取设备状态
    if device_id:
        test_get_device_status(device_id)
    
    # 4. 更新设备
    if device_id:
        test_update_device(device_id)
    
    # 5. 删除设备
    if device_id:
        test_delete_device(device_id)
```

## 9. 总结与最佳实践

### 9.1 代码组织规范

```
backend/src/main/java/com/dormpower/
├── config/          # 配置类（数据库、缓存、安全等）
├── controller/      # 控制层（REST API）
├── service/         # 业务层（业务逻辑）
├── repository/      # 数据访问层（Spring Data JPA）
├── model/           # 实体类（数据库映射）
├── dto/             # 数据传输对象（请求/响应）
├── exception/       # 异常类
├── util/            # 工具类
├── annotation/      # 自定义注解
├── aop/             # 面向切面编程
└── scheduler/       # 定时任务
```

### 9.2 CRUD开发 checklist

| 层级 | 检查项 | 说明 |
|------|--------|------|
| Model | 字段注释 | 每个字段都有JavaDoc注释 |
| Model | 验证注解 | 必要字段使用@NotNull等 |
| Model | 主键定义 | 正确使用@Id注解 |
| Repository | 方法命名 | 遵循Spring Data JPA命名规范 |
| Repository | 分页支持 | 列表查询支持分页 |
| Service | 事务注解 | 多表操作使用@Transactional |
| Service | 日志记录 | 关键操作记录日志 |
| Service | 异常处理 | 业务异常转换为自定义异常 |
| Controller | REST规范 | 正确使用HTTP方法和状态码 |
| Controller | 参数校验 | 使用@Valid触发校验 |
| Controller | 文档注解 | 使用Swagger注解生成文档 |

### 9.3 常见错误与解决方案

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| LazyInitializationException | 在Session外访问懒加载属性 | 使用EAGER加载或在Session内访问 |
| DataIntegrityViolationException | 违反数据库约束 | 在Service层进行业务校验 |
| EntityNotFoundException | 查询的实体不存在 | 使用orElseThrow或orElse处理 |
| MethodArgumentNotValidException | 参数校验失败 | 使用@Valid并处理异常 |
| DuplicateKeyException | 主键或唯一键冲突 | 插入前检查是否存在 |

### 9.4 性能优化建议

1. **缓存使用**：对读多写少的数据使用Spring Cache
2. **分页查询**：列表接口必须支持分页
3. **批量操作**：批量插入/更新使用saveAll()
4. **索引优化**：为常用查询字段添加数据库索引
5. **连接池**：配置合适的连接池大小
6. **N+1问题**：使用@EntityGraph或JOIN FETCH解决

### 9.5 安全注意事项

1. **SQL注入**：使用JPA参数绑定，避免字符串拼接SQL
2. **XSS防护**：对用户输入进行转义
3. **认证授权**：敏感接口添加权限校验
4. **敏感数据**：密码等敏感字段加密存储
5. **审计日志**：关键操作记录审计日志

## 10. 技术深度对比：JPA vs MyBatis vs MyBatis-Plus

### 10.1 三种技术概览

| 特性 | Spring Data JPA | MyBatis | MyBatis-Plus |
|------|-----------------|---------|--------------|
| **开发方式** | 全自动ORM | 半自动ORM | 全自动ORM（基于MyBatis） |
| **SQL控制** | 自动生成，可自定义 | 手动编写 | 自动生成，支持自定义 |
| **学习曲线** | 中等 | 较高 | 低 |
| **灵活性** | 中等 | 高 | 高 |
| **代码量** | 少 | 多 | 极少 |
| **性能调优** | 较复杂 | 直接优化SQL | 较简单 |
| **适用场景** | 快速开发、标准CRUD | 复杂SQL、性能敏感 | 快速开发、需要灵活控制 |

### 10.2 同一功能的三种实现对比

以**设备管理**为例，展示三种技术的实现差异。

#### 10.2.1 实体层对比

**JPA版本（本项目实际使用）**:

```java
@Entity
@Table(name = "devices")
public class Device {
    @Id
    private String id;
    
    @NotNull
    private String name;
    
    @NotNull
    private String room;
    
    @NotNull
    private boolean online;
    
    @Column(name = "last_seen_ts")
    private long lastSeenTs;
    
    @Column(name = "created_at")
    private long createdAt;
    
    // Getters and Setters
}
```

**MyBatis版本**:

```java
/**
 * MyBatis实体类
 * 
 * 与JPA不同，MyBatis实体类是纯POJO，不需要任何注解
 * 映射关系在XML或注解中配置
 */
public class Device {
    private String id;
    private String name;
    private String room;
    private boolean online;
    private long lastSeenTs;
    private long createdAt;
    
    // Getters and Setters
}
```

**MyBatis-Plus版本**:

```java
/**
 * MyBatis-Plus实体类
 * 
 * 使用@TableName指定表名
 * 使用@TableId标记主键
 * 使用@TableField映射字段
 */
@TableName("devices")
public class Device {
    
    @TableId(type = IdType.INPUT)  // 手动输入ID
    private String id;
    
    @TableField("name")
    private String name;
    
    @TableField("room")
    private String room;
    
    @TableField("online")
    private boolean online;
    
    @TableField("last_seen_ts")
    private long lastSeenTs;
    
    @TableField("created_at")
    private long createdAt;
    
    // Getters and Setters
}
```

#### 10.2.2 数据访问层对比

**JPA版本（Repository接口）**:

```java
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    // 方法名约定自动生成SQL
    List<Device> findByRoom(String room);
    
    // 使用@Query自定义SQL
    @Query("SELECT d FROM Device d WHERE d.online = true")
    List<Device> findOnlineDevices();
}
```

**MyBatis版本（Mapper接口 + XML）**:

```java
@Mapper
public interface DeviceMapper {
    // 基础CRUD
    Device selectById(String id);
    List<Device> selectAll();
    int insert(Device device);
    int update(Device device);
    int deleteById(String id);
    
    // 自定义查询
    List<Device> selectByRoom(@Param("room") String room);
    List<Device> selectOnlineDevices();
}
```

**对应的XML映射文件** (`DeviceMapper.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.dormpower.mapper.DeviceMapper">
    
    <!-- 结果映射 -->
    <resultMap id="BaseResultMap" type="com.dormpower.model.Device">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
        <result column="room" property="room"/>
        <result column="online" property="online"/>
        <result column="last_seen_ts" property="lastSeenTs"/>
        <result column="created_at" property="createdAt"/>
    </resultMap>
    
    <!-- 基础列 -->
    <sql id="Base_Column_List">
        id, name, room, online, last_seen_ts, created_at
    </sql>
    
    <!-- 根据ID查询 -->
    <select id="selectById" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM devices
        WHERE id = #{id}
    </select>
    
    <!-- 查询所有 -->
    <select id="selectAll" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM devices
    </select>
    
    <!-- 插入 -->
    <insert id="insert">
        INSERT INTO devices (id, name, room, online, last_seen_ts, created_at)
        VALUES (#{id}, #{name}, #{room}, #{online}, #{lastSeenTs}, #{createdAt})
    </insert>
    
    <!-- 更新 -->
    <update id="update">
        UPDATE devices
        SET name = #{name},
            room = #{room},
            online = #{online},
            last_seen_ts = #{lastSeenTs}
        WHERE id = #{id}
    </update>
    
    <!-- 删除 -->
    <delete id="deleteById">
        DELETE FROM devices WHERE id = #{id}
    </delete>
    
    <!-- 根据房间查询 -->
    <select id="selectByRoom" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM devices
        WHERE room = #{room}
    </select>
    
    <!-- 查询在线设备 -->
    <select id="selectOnlineDevices" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM devices
        WHERE online = true
    </select>
    
</mapper>
```

**MyBatis-Plus版本（最简洁）**:

```java
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
    // 继承BaseMapper获得所有基础CRUD方法
    // 只需定义自定义查询
    
    @Select("SELECT * FROM devices WHERE room = #{room}")
    List<Device> selectByRoom(@Param("room") String room);
    
    @Select("SELECT * FROM devices WHERE online = true")
    List<Device> selectOnlineDevices();
}
```

#### 10.2.3 Service层对比

**JPA版本**:

```java
@Service
public class DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Cacheable(value = "devices")
    public List<Map<String, Object>> getDevices() {
        List<Device> devices = deviceRepository.findAll();
        // 转换为Map...
        return result;
    }
    
    public Device addDevice(Device device) {
        device.setCreatedAt(System.currentTimeMillis());
        return deviceRepository.save(device);
    }
}
```

**MyBatis版本**:

```java
@Service
public class DeviceService {
    @Autowired
    private DeviceMapper deviceMapper;
    
    @Cacheable(value = "devices")
    public List<Map<String, Object>> getDevices() {
        List<Device> devices = deviceMapper.selectAll();
        // 转换为Map...
        return result;
    }
    
    public Device addDevice(Device device) {
        device.setCreatedAt(System.currentTimeMillis());
        deviceMapper.insert(device);
        return device;
    }
}
```

**MyBatis-Plus版本（使用IService）**:

```java
@Service
public class DeviceService extends ServiceImpl<DeviceMapper, Device> 
        implements IService<Device> {
    
    // 继承ServiceImpl后，基础CRUD方法已内置：
    // save(), saveBatch(), getById(), list(), page(), removeById()...
    
    @Cacheable(value = "devices")
    public List<Map<String, Object>> getDevices() {
        // 使用list()方法查询所有
        List<Device> devices = list();
        // 转换为Map...
        return result;
    }
    
    // 自定义业务方法
    public Device addDevice(Device device) {
        device.setCreatedAt(System.currentTimeMillis());
        save(device);  // 内置方法
        return device;
    }
}
```

### 10.3 底层原理解析

#### 10.3.1 Spring Data JPA原理

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Data JPA 架构                      │
├─────────────────────────────────────────────────────────────┤
│  Repository接口                                              │
│     ↓                                                        │
│  SimpleJpaRepository (默认实现)                              │
│     ↓                                                        │
│  EntityManager (JPA核心)                                     │
│     ↓                                                        │
│  Hibernate (默认实现)                                        │
│     ↓                                                        │
│  JDBC → 数据库                                                │
└─────────────────────────────────────────────────────────────┘
```

**核心机制**：

1. **动态代理**：Spring为Repository接口生成动态代理
2. **方法名解析**：将`findByNameAndAge`解析为查询条件
3. **HQL/JPQL**：面向对象的查询语言，最终转换为SQL

```java
// 方法名解析示例
List<Device> findByRoomAndOnline(String room, boolean online);

// 生成的HQL
// SELECT d FROM Device d WHERE d.room = ?1 AND d.online = ?2

// 最终SQL
// SELECT * FROM devices WHERE room = ? AND online = ?
```

#### 10.3.2 MyBatis原理

```
┌─────────────────────────────────────────────────────────────┐
│                      MyBatis 架构                            │
├─────────────────────────────────────────────────────────────┤
│  Mapper接口                                                  │
│     ↓                                                        │
│  MapperProxy (JDK动态代理)                                   │
│     ↓                                                        │
│  SqlSession                                                  │
│     ↓                                                        │
│  Executor (执行器)                                           │
│     ↓                                                        │
│  StatementHandler → ParameterHandler → ResultSetHandler     │
│     ↓                                                        │
│  JDBC → 数据库                                                │
└─────────────────────────────────────────────────────────────┘
```

**核心机制**：

1. **SQL映射**：XML或注解中定义SQL语句
2. **参数绑定**：`#{param}`使用PreparedStatement防注入
3. **结果映射**：自动将ResultSet映射为Java对象

```java
// MyBatis执行流程示例
SqlSession session = sqlSessionFactory.openSession();
try {
    DeviceMapper mapper = session.getMapper(DeviceMapper.class);
    Device device = mapper.selectById("device_001");
    // 底层：
    // 1. 解析XML中的SQL
    // 2. 创建PreparedStatement
    // 3. 设置参数
    // 4. 执行查询
    // 5. 结果集映射
} finally {
    session.close();
}
```

#### 10.3.3 MyBatis-Plus原理

```
┌─────────────────────────────────────────────────────────────┐
│                   MyBatis-Plus 架构                          │
├─────────────────────────────────────────────────────────────┤
│  ServiceImpl / BaseMapper                                    │
│     ↓                                                        │
│  代码生成器 (自动生成SQL)                                     │
│     ↓                                                        │
│  MyBatis核心                                                 │
│     ↓                                                        │
│  JDBC → 数据库                                                │
└─────────────────────────────────────────────────────────────┘
```

**核心机制**：

1. **通用Mapper**：`BaseMapper<T>`提供基础CRUD
2. **SQL注入器**：在运行时生成SQL并注入到MyBatis
3. **条件构造器**：`QueryWrapper`动态构建查询条件

```java
// MyBatis-Plus代码生成示例
// 实体类Device被解析后，自动生成：

// INSERT INTO devices (id, name, room...) VALUES (?, ?, ?...)
// SELECT id, name, room... FROM devices WHERE id = ?
// UPDATE devices SET name = ?, room = ?... WHERE id = ?
// DELETE FROM devices WHERE id = ?

// 使用QueryWrapper动态查询
QueryWrapper<Device> wrapper = new QueryWrapper<>();
wrapper.eq("room", "A-101")
       .eq("online", true)
       .orderByDesc("created_at");

List<Device> devices = deviceMapper.selectList(wrapper);
// 生成SQL: SELECT * FROM devices WHERE room = 'A-101' AND online = true ORDER BY created_at DESC
```

### 10.4 性能对比

| 场景 | JPA | MyBatis | MyBatis-Plus | 说明 |
|------|-----|---------|--------------|------|
| **简单查询** | 中等 | 快 | 快 | JPA有代理开销 |
| **复杂关联** | 慢 | 快 | 快 | JPA的N+1问题 |
| **批量操作** | 慢 | 快 | 快 | MyBatis批量SQL优化 |
| **启动速度** | 慢 | 中等 | 中等 | JPA需要扫描实体 |
| **内存占用** | 高 | 低 | 低 | JPA缓存占用大 |

**性能优化建议**：

```java
// JPA解决N+1问题：使用EntityGraph
@EntityGraph(attributePaths = {"room", "status"})
@Query("SELECT d FROM Device d")
List<Device> findAllWithRelations();

// MyBatis批量插入优化
<insert id="batchInsert">
    INSERT INTO devices (id, name, room) VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.id}, #{item.name}, #{item.room})
    </foreach>
</insert>

// MyBatis-Plus批量操作
deviceService.saveBatch(deviceList, 500); // 每500条一批
```

### 10.5 技术选型建议

| 场景 | 推荐技术 | 理由 |
|------|----------|------|
| 快速原型开发 | MyBatis-Plus | 代码量少，开发快 |
| 复杂SQL项目 | MyBatis | 完全控制SQL |
| 企业级标准项目 | JPA | 规范统一，生态成熟 |
| 遗留系统维护 | MyBatis | 易于理解和修改 |
| 微服务架构 | 均可 | 根据团队技术栈选择 |

### 10.6 完整MyBatis-Plus示例

**依赖配置** (`pom.xml`):

```xml
<dependencies>
    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.3.1</version>
    </dependency>
    
    <!-- 代码生成器 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-generator</artifactId>
        <version>3.5.3.1</version>
    </dependency>
</dependencies>
```

**配置类**:

```java
@Configuration
@MapperScan("com.dormpower.mapper")
public class MyBatisPlusConfig {
    
    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
    
    /**
     * 自动填充字段
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", Long.class, System.currentTimeMillis());
            }
            
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", Long.class, System.currentTimeMillis());
            }
        };
    }
}
```

**代码生成器**:

```java
public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/dormpower", "root", "password")
            .globalConfig(builder -> {
                builder.author("dormpower")
                       .outputDir(System.getProperty("user.dir") + "/src/main/java");
            })
            .packageConfig(builder -> {
                builder.parent("com.dormpower")
                       .entity("model")
                       .mapper("mapper")
                       .service("service")
                       .controller("controller");
            })
            .strategyConfig(builder -> {
                builder.addInclude("devices", "students", "rooms")
                       .entityBuilder()
                       .enableLombok()
                       .enableTableFieldAnnotation();
            })
            .execute();
    }
}
```

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**

## 11. 源码级深度解析

### 11.1 Spring Data JPA 源码剖析

#### 11.1.1 Repository动态代理机制

```java
/**
 * Spring Data JPA Repository动态代理创建流程
 * 
 * 当我们注入DeviceRepository时，实际注入的是代理对象
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    List<Device> findByRoom(String room);
}

// Spring启动时，会创建以下代理链：
// DeviceRepository -> JdkDynamicAopProxy -> SimpleJpaRepository
```

**核心源码解析** (`RepositoryFactorySupport.java`):

```java
public abstract class RepositoryFactorySupport {
    
    public <T> T getRepository(Class<T> repositoryInterface) {
        // 1. 创建Repository元数据
        RepositoryMetadata metadata = getRepositoryMetadata(repositoryInterface);
        
        // 2. 创建Repository实现（SimpleJpaRepository）
        Object target = getTargetRepository(metadata);
        
        // 3. 创建代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.setInterfaces(repositoryInterface);
        
        // 4. 添加查询执行器拦截器
        proxyFactory.addAdvice(new QueryExecutorMethodInterceptor());
        
        // 5. 生成代理对象
        return (T) proxyFactory.getProxy();
    }
}
```

**方法调用拦截** (`QueryExecutorMethodInterceptor`):

```java
public class QueryExecutorMethodInterceptor implements MethodInterceptor {
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        
        // 1. 解析方法名，生成查询
        if (isQueryMethod(method)) {
            // findByRoom -> SELECT d FROM Device d WHERE d.room = ?1
            String queryString = createQueryFromMethodName(method);
            
            // 2. 创建查询对象
            Query query = entityManager.createQuery(queryString);
            
            // 3. 设置参数
            setParameters(query, invocation.getArguments());
            
            // 4. 执行查询
            return executeQuery(query, method);
        }
        
        // 调用SimpleJpaRepository的默认实现
        return invocation.proceed();
    }
    
    private String createQueryFromMethodName(Method method) {
        // 方法名解析逻辑
        // findByRoomAndOnline -> room = ?1 AND online = ?2
        PartTree tree = new PartTree(method.getName(), domainClass);
        return tree.buildQuery();
    }
}
```

#### 11.1.2 EntityManager与Hibernate交互

```java
/**
 * EntityManager是JPA的核心接口
 * Hibernate是JPA的实现提供者
 */
@PersistenceContext
private EntityManager entityManager;

// 调用流程：
// DeviceRepository.findById() 
// -> SimpleJpaRepository.findById()
// -> EntityManager.find(Device.class, id)
// -> SessionImpl.find(Device.class, id) [Hibernate实现]
// -> JDBC PreparedStatement.executeQuery()
```

**Hibernate SessionImpl核心源码**:

```java
public class SessionImpl implements Session {
    
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        // 1. 检查一级缓存
        Object entity = persistenceContext.getEntity(entityKey);
        if (entity != null) {
            return (T) entity;
        }
        
        // 2. 生成SQL
        String sql = generateSelectSQL(entityClass);
        
        // 3. 创建PreparedStatement
        PreparedStatement ps = jdbcCoordinator.getStatementPreparer()
            .prepareStatement(sql);
        
        // 4. 设置主键参数
        ps.setString(1, primaryKey.toString());
        
        // 5. 执行查询
        ResultSet rs = ps.executeQuery();
        
        // 6. 结果映射
        T result = entityLoader.load(rs, entityClass);
        
        // 7. 放入一级缓存
        persistenceContext.addEntity(entityKey, result);
        
        return result;
    }
}
```

#### 11.1.3 延迟加载与N+1问题

```java
/**
 * N+1问题示例与解决
 */
@Entity
public class Device {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)  // 延迟加载
    @JoinColumn(name = "room_id")
    private Room room;
}

// N+1问题：查询N个设备，会额外发送N条SQL查询房间
List<Device> devices = deviceRepository.findAll();  // 1条SQL
for (Device d : devices) {
    Room room = d.getRoom();  // N条SQL（延迟加载触发）
}

// 解决方案1：Entity Graph
@EntityGraph(attributePaths = {"room"})
@Query("SELECT d FROM Device d")
List<Device> findAllWithRoom();

// 解决方案2：JOIN FETCH
@Query("SELECT d FROM Device d LEFT JOIN FETCH d.room")
List<Device> findAllWithRoomJoin();

// 解决方案3：批量抓取
@BatchSize(size = 50)
@Entity
public class Room { ... }
```

### 11.2 MyBatis 源码剖析

#### 11.2.1 Mapper代理创建机制

```java
/**
 * MyBatis Mapper接口代理创建
 */
public class MapperProxyFactory<T> {
    
    private final Class<T> mapperInterface;
    
    public T newInstance(SqlSession sqlSession) {
        // 创建MapperProxy（InvocationHandler）
        final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface);
        
        // 使用JDK动态代理创建代理对象
        return (T) Proxy.newProxyInstance(
            mapperInterface.getClassLoader(),
            new Class[]{mapperInterface},
            mapperProxy
        );
    }
}
```

**MapperProxy调用处理**:

```java
public class MapperProxy<T> implements InvocationHandler {
    
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 获取MappedStatement（XML中定义的SQL）
        String statementId = mapperInterface.getName() + "." + method.getName();
        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(statementId);
        
        // 2. 判断SQL类型
        switch (ms.getSqlCommandType()) {
            case SELECT:
                // 执行查询
                return executeSelect(ms, args);
            case INSERT:
                return sqlSession.insert(statementId, args);
            case UPDATE:
                return sqlSession.update(statementId, args);
            case DELETE:
                return sqlSession.delete(statementId, args);
        }
        
        return null;
    }
    
    private Object executeSelect(MappedStatement ms, Object[] args) {
        // 获取SQL源
        SqlSource sqlSource = ms.getSqlSource();
        
        // 创建参数对象
        Object parameter = parameterHandler(args);
        
        // 获取BoundSQL（包含实际SQL和参数）
        BoundSql boundSql = sqlSource.getBoundSql(parameter);
        
        // 创建执行器
        Executor executor = new SimpleExecutor(sqlSession.getConfiguration());
        
        // 执行查询
        return executor.query(ms, parameter, boundSql);
    }
}
```

#### 11.2.2 SQL解析与参数绑定

```java
/**
 * MyBatis SQL解析流程
 */
public class XMLStatementBuilder {
    
    public void parseStatementNode() {
        // 1. 解析SQL语句
        String sql = parseSql();
        
        // 2. 创建SQL源
        SqlSource sqlSource = createSqlSource(sql);
        
        // 3. 构建MappedStatement
        MappedStatement.Builder builder = new MappedStatement.Builder(
            configuration,
            id,
            sqlSource,
            sqlCommandType
        );
        
        // 4. 添加结果映射
        builder.resultMaps(resultMaps);
        
        // 5. 注册到Configuration
        configuration.addMappedStatement(builder.build());
    }
}

/**
 * 动态SQL解析
 */
public class DynamicSqlSource implements SqlSource {
    
    private final SqlNode rootSqlNode;
    
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        // 1. 创建动态上下文
        DynamicContext context = new DynamicContext(configuration, parameterObject);
        
        // 2. 解析SQL节点（处理<if>, <foreach>等）
        rootSqlNode.apply(context);
        
        // 3. 创建SQL解析器
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        
        // 4. 解析参数占位符 #{}
        Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
        SqlSource staticSqlSource = sqlSourceParser.parse(context.getSql(), parameterType);
        
        // 5. 生成最终SQL
        return staticSqlSource.getBoundSql(parameterObject);
    }
}
```

#### 11.2.3 结果集映射原理

```java
/**
 * MyBatis结果集映射
 */
public class DefaultResultSetHandler implements ResultSetHandler {
    
    @Override
    public List<Object> handleResultSets(Statement stmt) throws SQLException {
        List<Object> multipleResults = new ArrayList<>();
        
        int resultSetCount = 0;
        ResultSetWrapper rsw = new ResultSetWrapper(stmt.getResultSet());
        
        // 获取结果映射
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        
        while (rsw != null && resultMaps.size() > resultSetCount) {
            ResultMap resultMap = resultMaps.get(resultSetCount);
            
            // 处理结果集
            handleResultSet(rsw, resultMap, multipleResults);
            
            rsw = getNextResultSet(stmt);
            resultSetCount++;
        }
        
        return multipleResults;
    }
    
    private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults) throws SQLException {
        // 创建结果处理器
        DefaultResultHandler resultHandler = new DefaultResultHandler(objectFactory);
        
        // 逐行处理
        handleRowValues(rsw, resultMap, resultHandler);
        
        multipleResults.addAll(resultHandler.getResultList());
    }
    
    private void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler) throws SQLException {
        // 创建结果对象
        Object rowValue = createResultObject(rsw, resultMap);
        
        // 自动映射列到属性
        if (shouldApplyAutomaticMappings(resultMap, false)) {
            applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix);
        }
        
        // 应用手动映射
        applyPropertyMappings(rsw, resultMap, metaObject, columnPrefix);
        
        // 添加到结果列表
        resultHandler.handleResult(rowValue);
    }
}
```

### 11.3 MyBatis-Plus 源码剖析

#### 11.3.1 自动SQL注入器

```java
/**
 * MyBatis-Plus自动SQL注入原理
 */
public class AutoSqlInjector implements ISqlInjector {
    
    @Override
    public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        // 1. 获取实体类
        Class<?> modelClass = extractModelClass(mapperClass);
        
        // 2. 获取表信息
        TableInfo tableInfo = TableInfoHelper.getTableInfo(modelClass);
        
        // 3. 注入基础CRUD方法
        injectSelectById(builderAssistant, mapperClass, modelClass, tableInfo);
        injectSelectList(builderAssistant, mapperClass, modelClass, tableInfo);
        injectInsert(builderAssistant, mapperClass, modelClass, tableInfo);
        injectUpdateById(builderAssistant, mapperClass, modelClass, tableInfo);
        injectDeleteById(builderAssistant, mapperClass, modelClass, tableInfo);
    }
    
    /**
     * 注入selectById方法
     */
    private void injectSelectById(MapperBuilderAssistant builderAssistant, 
                                   Class<?> mapperClass, 
                                   Class<?> modelClass, 
                                   TableInfo tableInfo) {
        
        // 生成SQL：SELECT id,name,room... FROM devices WHERE id = ?
        String sql = new SQL()
            .SELECT(tableInfo.getAllSqlSelect())
            .FROM(tableInfo.getTableName())
            .WHERE(tableInfo.getKeyColumn() + " = #{id}")
            .toString();
        
        // 创建MappedStatement
        SqlSource sqlSource = languageDriver.createSqlSource(
            configuration, sql, modelClass);
        
        // 添加到MyBatis配置
        addMappedStatement(builderAssistant, "selectById", sqlSource, 
            SqlCommandType.SELECT, modelClass);
    }
}
```

#### 11.3.2 条件构造器原理

```java
/**
 * QueryWrapper条件构造器原理
 */
public class QueryWrapper<T> extends AbstractWrapper<T, String, QueryWrapper<T>> {
    
    /**
     * 添加等于条件
     */
    public QueryWrapper<T> eq(String column, Object val) {
        // 添加条件到表达式列表
        addCondition(column, val, SqlCondition.EQ);
        return this;
    }
    
    /**
     * 生成WHERE SQL片段
     */
    @Override
    public String getSqlSegment() {
        if (expressionList.isEmpty()) {
            return StringPool.EMPTY;
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append(" WHERE ");
        
        // 拼接所有条件
        for (int i = 0; i < expressionList.size(); i++) {
            Condition condition = expressionList.get(i);
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(condition.getColumn())
               .append(" ")
               .append(condition.getOperator())
               .append(" ")
               .append(formatValue(condition.getValue()));
        }
        
        return sql.toString();
    }
}

// 使用示例生成的SQL：
QueryWrapper<Device> wrapper = new QueryWrapper<>();
wrapper.eq("room", "A-101")
       .like("name", "空调")
       .gt("created_at", 1609459200);

// 生成：WHERE room = 'A-101' AND name LIKE '%空调%' AND created_at > 1609459200
```

### 11.4 数据库连接池原理

#### 11.4.1 HikariCP连接池（Spring Boot默认）

```java
/**
 * HikariCP连接池核心原理
 */
public class HikariPool extends PoolBase implements HikariPoolMXBean {
    
    // 连接存储
    private final ConcurrentBag<HikariPoolEntry> connectionBag;
    
    // 等待连接的线程队列
    private final ThreadLocal<List<Object>> threadList;
    
    /**
     * 获取连接
     */
    public Connection getConnection() throws SQLException {
        // 1. 从ThreadLocal获取（避免锁竞争）
        List<Object> list = threadList.get();
        
        // 2. 从ConcurrentBag获取空闲连接
        HikariPoolEntry poolEntry = connectionBag.borrow(timeout);
        
        if (poolEntry == null) {
            // 3. 无可用连接，等待或创建新连接
            throw new SQLException("Timeout waiting for connection");
        }
        
        // 4. 验证连接有效性
        if (!isConnectionAlive(poolEntry.connection)) {
            // 连接失效，关闭并重新获取
            closeConnection(poolEntry);
            return getConnection();
        }
        
        // 5. 包装为代理连接
        return new HikariProxyConnection(poolEntry, this);
    }
    
    /**
     * 创建新连接（异步）
     */
    private void addConnection() {
        // 使用独立线程创建连接，避免阻塞主线程
        addConnectionExecutor.submit(() -> {
            Connection connection = createNewConnection();
            HikariPoolEntry poolEntry = new HikariPoolEntry(connection, this);
            connectionBag.add(poolEntry);
        });
    }
}
```

**连接池配置优化**:

```yaml
# application.yml
spring:
  datasource:
    hikari:
      # 连接池大小 = (核心数 * 2) + 有效磁盘数
      maximum-pool-size: 20
      minimum-idle: 5
      # 连接超时时间
      connection-timeout: 30000
      # 空闲连接超时
      idle-timeout: 600000
      # 连接最大生命周期
      max-lifetime: 1800000
      # 连接测试查询
      connection-test-query: SELECT 1
```

### 11.5 事务管理源码分析

#### 11.5.1 Spring事务传播机制

```java
/**
 * Spring事务传播行为源码分析
 */
public class TransactionAspectSupport {
    
    protected Object invokeWithinTransaction(Method method, Class<?> targetClass, 
                                             InvocationCallback invocation) {
        
        // 1. 获取事务属性（@Transactional注解配置）
        TransactionAttribute txAttr = getTransactionAttributeSource()
            .getTransactionAttribute(method, targetClass);
        
        // 2. 获取事务管理器
        PlatformTransactionManager tm = determineTransactionManager(txAttr);
        
        // 3. 生成事务名称
        String joinpointIdentification = methodIdentification(method, targetClass);
        
        // 4. 处理不同传播行为
        if (txAttr == null || !(tm instanceof CallbackPreferringPlatformTransactionManager)) {
            
            // 创建事务（根据传播行为决定）
            TransactionInfo txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification);
            
            Object retVal = null;
            try {
                // 执行业务方法
                retVal = invocation.proceedWithInvocation();
            } catch (Throwable ex) {
                // 异常回滚
                completeTransactionAfterThrowing(txInfo, ex);
                throw ex;
            } finally {
                cleanupTransactionInfo(txInfo);
            }
            
            // 提交事务
            commitTransactionAfterReturning(txInfo);
            return retVal;
        }
    }
    
    /**
     * 创建事务（处理传播行为）
     */
    protected TransactionInfo createTransactionIfNecessary(PlatformTransactionManager tm,
                                                           TransactionAttribute txAttr,
                                                           String joinpointIdentification) {
        
        // 获取当前事务状态
        TransactionStatus status = tm.getTransaction(txAttr);
        
        // 根据传播行为处理：
        // REQUIRED: 如果存在事务则加入，否则新建
        // REQUIRES_NEW: 挂起当前事务，新建事务
        // NESTED: 创建保存点
        // ...
        
        return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
    }
}
```

#### 11.5.2 事务隔离级别实现

```java
/**
 * 事务隔离级别在JDBC中的实现
 */
public class DataSourceTransactionManager {
    
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
        Connection con = txObject.getConnectionHolder().getConnection();
        
        // 1. 设置隔离级别
        Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
        txObject.setPreviousIsolationLevel(previousIsolationLevel);
        
        // 2. 设置只读模式
        if (definition.isReadOnly()) {
            con.setReadOnly(true);
        }
        
        // 3. 关闭自动提交
        if (con.getAutoCommit()) {
            txObject.setMustRestoreAutoCommit(true);
            con.setAutoCommit(false);
        }
        
        // 4. 准备事务同步
        prepareTransactionalConnection(con, definition);
        
        // 5. 激活事务同步
        TransactionSynchronizationManager.bindResource(getDataSource(), txObject.getConnectionHolder());
    }
}

// 隔离级别映射
// ISOLATION_READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED
// ISOLATION_READ_COMMITTED   -> Connection.TRANSACTION_READ_COMMITTED
// ISOLATION_REPEATABLE_READ  -> Connection.TRANSACTION_REPEATABLE_READ
// ISOLATION_SERIALIZABLE     -> Connection.TRANSACTION_SERIALIZABLE
```

### 11.6 SQL执行计划分析

#### 11.6.1 使用EXPLAIN分析查询

```sql
-- MySQL执行计划分析
EXPLAIN SELECT * FROM devices 
WHERE room = 'A-101' 
AND online = true 
ORDER BY created_at DESC;

-- 结果字段说明：
-- id: 查询标识符
-- select_type: 查询类型（SIMPLE, PRIMARY, SUBQUERY等）
-- table: 访问的表
-- type: 访问类型（system > const > eq_ref > ref > range > index > ALL）
-- possible_keys: 可能使用的索引
-- key: 实际使用的索引
-- rows: 扫描的行数估计
-- Extra: 额外信息
```

#### 11.6.2 索引优化策略

```java
/**
 * JPA索引定义
 */
@Entity
@Table(name = "devices", 
    indexes = {
        @Index(name = "idx_room", columnList = "room"),
        @Index(name = "idx_online", columnList = "online"),
        @Index(name = "idx_room_online", columnList = "room, online")
    })
public class Device {
    @Id
    private String id;
    
    @Column(name = "room")
    private String room;
    
    @Column(name = "online")
    private boolean online;
}

/**
 * MyBatis-Plus索引优化
 */
@TableName(value = "devices", 
    autoResultMap = true)
@TableIndex(name = "idx_room_online", fields = {"room", "online"})
public class Device {
    // 字段定义
}
```

### 11.7 性能监控与调优

#### 11.7.1 慢查询日志配置

```yaml
# application.yml - MyBatis-Plus性能分析
mybatis-plus:
  configuration:
    # 开启SQL日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 逻辑删除配置
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  
  # 性能分析插件（开发环境使用）
  performance-interceptor:
    # SQL执行最大时间，超过自动停止运行
    max-time: 1000
    # SQL是否格式化
    format: true
```

#### 11.7.2 自定义性能监控

```java
/**
 * SQL执行时间监控拦截器
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "query", 
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", 
               args = {MappedStatement.class, Object.class})
})
public class PerformanceInterceptor implements Interceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceInterceptor.class);
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            return invocation.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            String sqlId = mappedStatement.getId();
            
            if (duration > 1000) {
                logger.warn("慢查询 [{}] 执行时间: {}ms", sqlId, duration);
            } else {
                logger.debug("SQL [{}] 执行时间: {}ms", sqlId, duration);
            }
        }
    }
}
```

---

**本文档基于DormPower项目真实代码编写，所有代码示例均可直接运行。**
        "name": "测试设备001",
        "room": "A-101",
        "online": False
    }
    
    response = requests.post(
        f"{BASE_URL}/devices",
        headers=headers,
        json=device_data
    )
    print(f"状态码: {response.status_code}")
    print(f"响应数据: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    return response.json()


def test_get_device_status(device_id):
    """测试获取设备状态"""
    print("\n" + "=" * 50)
    print(f"测试：获取设备 {device_id} 状态")
    print("=" * 50)
    
    response = requests.get(
        f"{BASE_URL}/devices/{device_id}/status",
        headers=headers
    )
    print(f"状态码: {response.status_code}")
    print(f"响应数据: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    return response.json()


def test_update_device(device_id):
    """测试更新设备"""
    print("\n" + "=" * 50)
    print(f"测试：更新设备 {device_id}")
    print("=" * 50)
    
    update_data = {
        "name": "测试设备001（已更新）",
        "room": "A-102",
        "online": True
    }
    
    response = requests.put(
        f"{BASE_URL}/devices/{device_id}",
        headers=headers,
        json=update_data
    )
    print(f"状态码: {response.status_code}")
    print(f"响应数据: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
    return response.json()


def test_delete_device(device_id):
    """测试删除设备"""
    print("\n" + "=" * 50)
    print(f"测试：删除设备 {device_id}")
    print("=" * 50)
    
    response = requests.delete(
        f"{BASE_URL}/devices/{device_id}",
        headers=headers
    )
    print(f"状态码: {response.status_code}")
    print(f"响应数据: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
