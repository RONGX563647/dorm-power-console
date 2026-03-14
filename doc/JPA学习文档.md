# JPA 学习文档

## 目录
- [1. JPA 基础概念](#1-jpa-基础概念)
- [2. JPA 核心注解](#2-jpa-核心注解)
- [3. JPA 实体关系映射](#3-jpa-实体关系映射)
- [4. JPA 查询方式](#4-jpa-查询方式)
- [5. JPA 性能优化](#5-jpa-性能优化)
- [6. JPA 原理分析](#6-jpa-原理分析)
- [7. JPA 面试要点](#7-jpa-面试要点)

---

## 1. JPA 基础概念

### 1.1 什么是 JPA

JPA（Java Persistence API）是 Java 平台的标准 ORM 规范，用于管理 Java 应用中的关系型数据。

### 1.2 JPA 与其他 ORM 框架对比

| 对比维度 | JPA | MyBatis | JDBC |
|----------|-----|---------|------|
| **类型** | ORM 规范 | 半自动 ORM | 底层 API |
| **SQL 控制** | 自动生成 | 手动编写 | 手动编写 |
| **学习曲线** | 中等 | 平缓 | 简单 |
| **开发效率** | 高 | 中 | 低 |
| **性能优化** | 中 | 高 | 高 |
| **数据库移植** | 容易 | 困难 | 困难 |
| **适用场景** | 标准 CRUD | 复杂 SQL | 性能要求高 |

### 1.3 JPA 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      JPA 架构                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Java 应用层                            ││
│  └──────────────────────┬──────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   JPA API                                ││
│  │  - EntityManager                                        ││
│  │  - EntityTransaction                                    ││
│  │  - Query                                                ││
│  │  - Criteria API                                         ││
│  └──────────────────────┬──────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   JPA 实现层                             ││
│  │  - Hibernate (最流行)                                   ││
│  │  - EclipseLink                                          ││
│  │  - OpenJPA                                              ││
│  └──────────────────────┬──────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   JDBC                                   ││
│  └──────────────────────┬──────────────────────────────────┘│
│                         │                                    │
│                         ▼                                    │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   数据库                                 ││
│  │  - PostgreSQL                                           ││
│  │  - MySQL                                                ││
│  │  - Oracle                                               ││
│  └─────────────────────────────────────────────────────────┘│
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. JPA 核心注解

### 2.1 实体类注解

```java
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_device_room", columnList = "room")
})
public class Device {
    
    @Id
    private String id;
    
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "device_name", nullable = false, unique = true, length = 100)
    private String name;
    
    @NotNull
    private String room;
    
    @Transient
    private String tempField;
    
    // Getters and Setters
}
```

### 2.2 主键生成策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `IDENTITY` | 数据库自增 | MySQL、PostgreSQL |
| `SEQUENCE` | 数据库序列 | Oracle、PostgreSQL |
| `TABLE` | 序列表模拟 | 跨数据库 |
| `AUTO` | 自动选择 | 默认策略 |
| `UUID` | UUID 生成 | 分布式系统 |

```java
// 自增主键
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// 序列主键
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_seq")
@SequenceGenerator(name = "device_seq", sequenceName = "device_sequence", allocationSize = 1)
private Long id;

// UUID 主键
@Id
@GeneratedValue(generator = "UUID")
private String id;
```

### 2.3 字段映射注解

```java
@Entity
@Table(name = "telemetry")
public class Telemetry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "device_id", nullable = false)
    private String deviceId;
    
    @NotNull
    @Column(name = "ts", nullable = false)
    private Long timestamp;
    
    @NotNull
    @Column(precision = 10, scale = 2)
    private Double powerW;
    
    @Enumerated(EnumType.STRING)
    private DeviceStatus status;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Lob
    private String description;
}
```

---

## 3. JPA 实体关系映射

### 3.1 一对一关系

```java
@Entity
public class User {
    
    @Id
    private String id;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}

@Entity
public class UserProfile {
    
    @Id
    private String id;
    
    @OneToOne(mappedBy = "profile")
    private User user;
}
```

### 3.2 一对多关系

```java
@Entity
public class Building {
    
    @Id
    private String id;
    
    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<DormRoom> rooms;
}

@Entity
public class DormRoom {
    
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;
}
```

### 3.3 多对多关系

```java
@Entity
public class Role {
    
    @Id
    private String id;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
}

@Entity
public class Permission {
    
    @Id
    private String id;
    
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles;
}
```

### 3.4 关系映射对比

| 关系类型 | 注解 | 外键位置 | 默认加载 |
|----------|------|----------|----------|
| 一对一 | @OneToOne | 任意一方 | EAGER |
| 一对多 | @OneToMany | 多的一方 | LAZY |
| 多对一 | @ManyToOne | 多的一方 | EAGER |
| 多对多 | @ManyToMany | 中间表 | LAZY |

### 3.5 加载策略

```
┌─────────────────────────────────────────────────────────────┐
│                      加载策略对比                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  EAGER（立即加载）                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  优点：                                              │   │
│  │  - 一次查询获取所有关联数据                          │   │
│  │  - 避免 N+1 问题                                     │   │
│  │                                                      │   │
│  │  缺点：                                              │   │
│  │  - 可能加载不需要的数据                              │   │
│  │  - 性能开销大                                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  LAZY（延迟加载）                                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  优点：                                              │   │
│  │  - 按需加载，节省资源                                │   │
│  │  - 性能更好                                          │   │
│  │                                                      │   │
│  │  缺点：                                              │   │
│  │  - 可能产生 N+1 问题                                 │   │
│  │  - 需要在事务内访问                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. JPA 查询方式

### 4.1 方法命名查询

```java
@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    
    // 精确查询
    Device findById(String id);
    Device findByName(String name);
    
    // 条件查询
    List<Device> findByRoom(String room);
    List<Device> findByOnlineTrue();
    List<Device> findByOnlineFalse();
    
    // 组合查询
    List<Device> findByRoomAndOnline(String room, boolean online);
    List<Device> findByRoomOrOnline(String room, boolean online);
    
    // 排序
    List<Device> findByRoomOrderByCreatedAtDesc(String room);
    
    // 分页
    Page<Device> findByRoom(String room, Pageable pageable);
    
    // 范围查询
    List<Device> findByLastSeenTsBetween(Long start, Long end);
    List<Device> findByLastSeenTsGreaterThan(Long timestamp);
    
    // 模糊查询
    List<Device> findByNameContaining(String name);
    List<Device> findByNameLike(String name);
    
    // In 查询
    List<Device> findByIdIn(List<String> ids);
    
    // 统计
    long countByRoom(String room);
    long countByOnlineTrue();
    
    // 存在性
    boolean existsByRoom(String room);
    
    // 删除
    void deleteByRoom(String room);
}
```

### 4.2 @Query 自定义查询

```java
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    // JPQL 查询
    @Query("SELECT r FROM Role r WHERE r.code = :code")
    Optional<Role> findByCode(@Param("code") String code);
    
    // 关联查询
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.code = :permissionCode")
    List<Role> findByPermissionCode(@Param("permissionCode") String permissionCode);
    
    // 原生 SQL
    @Query(value = "SELECT * FROM roles WHERE enabled = true", nativeQuery = true)
    List<Role> findEnabledRoles();
    
    // 更新操作
    @Modifying
    @Query("UPDATE Device d SET d.online = :online WHERE d.id = :id")
    int updateOnlineStatus(@Param("id") String id, @Param("online") boolean online);
    
    // 删除操作
    @Modifying
    @Query("DELETE FROM Device d WHERE d.room = :room")
    int deleteByRoom(@Param("room") String room);
}
```

### 4.3 Specification 动态查询

```java
public class DeviceSpecs {
    
    public static Specification<Device> hasRoom(String room) {
        return (root, query, cb) -> 
            room == null ? null : cb.equal(root.get("room"), room);
    }
    
    public static Specification<Device> isOnline(Boolean online) {
        return (root, query, cb) -> 
            online == null ? null : cb.equal(root.get("online"), online);
    }
    
    public static Specification<Device> nameContains(String name) {
        return (root, query, cb) -> 
            name == null ? null : cb.like(root.get("name"), "%" + name + "%");
    }
}

// 使用
Specification<Device> spec = Specification
    .where(DeviceSpecs.hasRoom(room))
    .and(DeviceSpecs.isOnline(online))
    .and(DeviceSpecs.nameContains(name));

List<Device> devices = deviceRepository.findAll(spec);
```

### 4.4 Criteria API

```java
public List<Device> searchDevices(String room, Boolean online) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Device> query = cb.createQuery(Device.class);
    Root<Device> root = query.from(Device.class);
    
    List<Predicate> predicates = new ArrayList<>();
    
    if (room != null) {
        predicates.add(cb.equal(root.get("room"), room));
    }
    
    if (online != null) {
        predicates.add(cb.equal(root.get("online"), online));
    }
    
    query.where(predicates.toArray(new Predicate[0]));
    
    return entityManager.createQuery(query).getResultList();
}
```

---

## 5. JPA 性能优化

### 5.1 批量操作

```java
@Repository
public class TelemetryBulkRepository {
    
    private static final int BATCH_SIZE = 50;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Transactional
    public void batchInsert(List<Telemetry> telemetryList) {
        int count = 0;
        
        for (Telemetry telemetry : telemetryList) {
            entityManager.persist(telemetry);
            count++;
            
            // 每 BATCH_SIZE 条刷新一次
            if (count % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        // 最后刷新剩余数据
        entityManager.flush();
        entityManager.clear();
    }
}
```

### 5.2 延迟加载优化

```java
@Entity
public class Role {
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(...)
    @BatchSize(size = 20)  // 批量加载
    private Set<Permission> permissions;
}

@Entity
public class Permission {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    @Fetch(FetchMode.SUBSELECT)  // 子查询加载
    private Resource resource;
}
```

### 5.3 缓存配置

```java
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Device {
    
    @Id
    private String id;
    
    // ...
}
```

### 5.4 N+1 问题解决

```
┌─────────────────────────────────────────────────────────────┐
│                      N+1 问题                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  问题场景：                                                 │
│  List<Building> buildings = buildingRepository.findAll();  │
│  for (Building b : buildings) {                            │
│      b.getRooms().size();  // 触发 N 次查询                │
│  }                                                         │
│                                                             │
│  解决方案：                                                 │
│                                                             │
│  1. JOIN FETCH                                             │
│  @Query("SELECT b FROM Building b JOIN FETCH b.rooms")     │
│  List<Building> findAllWithRooms();                        │
│                                                             │
│  2. EntityGraph                                            │
│  @EntityGraph(attributePaths = {"rooms"})                  │
│  List<Building> findAll();                                 │
│                                                             │
│  3. BatchSize                                              │
│  @BatchSize(size = 20)                                     │
│  private List<DormRoom> rooms;                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. JPA 原理分析

### 6.1 实体生命周期

```
┌─────────────────────────────────────────────────────────────┐
│                    实体生命周期                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐                                          │
│  │   New       │ 新建状态，未与持久化上下文关联            │
│  │  (新建)     │                                          │
│  └──────┬──────┘                                          │
│         │ persist()                                        │
│         ▼                                                  │
│  ┌─────────────┐                                          │
│  │  Managed    │ 托管状态，在持久化上下文中                │
│  │  (托管)     │ 自动检测变更                              │
│  └──────┬──────┘                                          │
│         │ remove()                                         │
│         ▼                                                  │
│  ┌─────────────┐                                          │
│  │  Removed    │ 删除状态                                 │
│  │  (删除)     │                                          │
│  └─────────────┘                                          │
│                                                             │
│  ┌─────────────┐                                          │
│  │  Detached   │ 游离状态，脱离持久化上下文                │
│  │  (游离)     │ merge() → Managed                        │
│  └─────────────┘                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 持久化上下文

```
┌─────────────────────────────────────────────────────────────┐
│                    持久化上下文                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  EntityManager                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Persistence Context                     │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │  Entity Cache (一级缓存)                    │   │   │
│  │  │  ┌────────────────────────────────────────┐│   │   │
│  │  │  │  Device@1c20cdda                       ││   │   │
│  │  │  │  Device@1c20cddb                       ││   │   │
│  │  │  │  Role@1c20cdec                         ││   │   │
│  │  │  └────────────────────────────────────────┘│   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  │  功能：                                             │   │
│  │  1. 实体缓存                                        │   │
│  │  2. 脏检查                                          │   │
│  │  3. 延迟加载                                        │   │
│  │  4. 事务写延迟                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  特点：                                                    │
│  - 事务范围内有效                                          │
│  - 自动脏检查                                              │
│  - 相同 ID 返回同一对象                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 脏检查机制

```
┌─────────────────────────────────────────────────────────────┐
│                      脏检查机制                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 加载实体时保存快照                                      │
│     ┌─────────────────────────────────────────────────────┐│
│     │  Entity: Device {id="1", name="设备A", online=true} ││
│     │  Snapshot: {id="1", name="设备A", online=true}     ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  2. 修改实体                                               │
│     device.setOnline(false);                               │
│     ┌─────────────────────────────────────────────────────┐│
│     │  Entity: Device {id="1", name="设备A", online=false}││
│     │  Snapshot: {id="1", name="设备A", online=true}     ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  3. 事务提交时对比                                         │
│     ┌─────────────────────────────────────────────────────┐│
│     │  对比 Entity 和 Snapshot                           ││
│     │  发现 online 字段变化                              ││
│     │  生成 UPDATE 语句                                  ││
│     └─────────────────────────────────────────────────────┘│
│                                                             │
│  4. 执行 SQL                                               │
│     UPDATE devices SET online=false WHERE id='1';         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. JPA 面试要点

### 7.1 基础问题

#### Q1: JPA 和 Hibernate 的关系？

**答案**：
- JPA 是 Java EE 的标准规范
- Hibernate 是 JPA 的实现之一
- JPA 定义了接口，Hibernate 提供了实现

#### Q2: JPA 的优点和缺点？

**答案**：

**优点**：
1. 开发效率高，减少样板代码
2. 数据库移植性好
3. 缓存支持好
4. 支持 Criteria API 动态查询

**缺点**：
1. 复杂 SQL 难以优化
2. 学习曲线较陡
3. 可能产生 N+1 问题
4. 性能调优难度大

#### Q3: JPA 的一级缓存和二级缓存？

**答案**：

**一级缓存**：
- 持久化上下文级别
- 事务范围内有效
- 默认开启
- 不能关闭

**二级缓存**：
- SessionFactory 级别
- 跨事务共享
- 需要配置开启
- 需要第三方实现

### 7.2 进阶问题

#### Q4: 如何解决 N+1 问题？

**答案**：
1. 使用 JOIN FETCH
2. 使用 EntityGraph
3. 配置 BatchSize
4. 使用 DTO 投影

#### Q5: JPA 的延迟加载原理？

**答案**：
1. 使用代理对象
2. 访问属性时触发查询
3. 需要在事务内访问
4. 可配置批量加载

#### Q6: JPA 的事务传播机制？

**答案**：
- REQUIRED：有事务则加入，无则新建
- REQUIRES_NEW：总是新建事务
- SUPPORTS：有事务则加入，无则非事务
- NOT_SUPPORTED：非事务执行
- MANDATORY：必须在事务中
- NEVER：不能在事务中
- NESTED：嵌套事务

---

## 总结

JPA 是 Java 平台的标准 ORM 规范，掌握其核心概念和最佳实践对于构建企业级应用至关重要。本文档涵盖了 JPA 的基础概念、核心注解、实体关系映射、查询方式、性能优化和原理分析，为实际开发提供参考。

---

**文档版本**：v1.0  
**编写日期**：2026年3月14日  
**作者**：DormPower Team
