# 项目进展记录

## 已完成功能

### 1. 计费管理模块 (核心功能)
- ✅ 电价规则设置：支持阶梯电价、时段电价、混合电价
- ✅ 宿舍用电账单生成：自动生成月度电费账单
- ✅ 个人用电费用分摊：支持按房间统计用电费用
- ✅ 充值/缴费记录：支持多种支付方式（现金、微信、支付宝）
- ✅ 欠费预警和断电控制：余额不足时自动预警
- ✅ 用电余额查询：实时查询房间电费余额

### 2. 房间/宿舍管理
- ✅ 宿舍楼栋管理：支持楼栋、楼层、房间层级管理
- ✅ 房间与设备绑定：房间关联智能电表设备
- ✅ 房间用电配额设置：设置月度用电配额
- ✅ 入住/退宿管理：支持办理入住和退宿手续

### 3. 设备管理增强
- ✅ 设备CRUD操作：添加、编辑、删除设备的功能
- ✅ 设备分组管理：按房间、楼层等维度对设备进行分组
- ✅ 设备状态历史：记录设备状态变更历史

### 4. 用户权限管理
- ✅ 角色权限系统：实现基于角色的权限控制（如管理员、普通用户）
- ✅ 用户密码修改：用户自主修改密码的功能
- ✅ 用户个人资料管理：修改邮箱、个人信息等

### 5. 遥测数据增强
- ✅ 用电统计报表：生成日/周/月/年用电统计报表
- ✅ 数据导出：支持CSV、Excel等格式导出遥测数据
- ✅ 异常告警：设置功率阈值，超出时触发告警

### 6. 命令控制增强
- ✅ 命令历史记录：记录所有下发的命令及其执行状态
- ✅ 定时任务管理：设置定时开关设备的任务
- ✅ 批量命令：同时向多个设备下发命令

### 7. 系统管理
- ✅ 系统配置管理：通过界面管理系统配置参数
- ✅ 日志管理：查看系统日志、操作日志
- ✅ 数据备份与恢复：定期备份数据并支持恢复

### 8. 通知系统
- ✅ 邮件通知：重要事件通过邮件通知
- ✅ 系统通知：系统内通知中心
- ✅ 告警通知：设备异常、用电异常等告警通知

### 9. 监控系统
- ✅ 系统监控：监控系统运行状态、资源使用情况
- ✅ 设备在线状态监控：实时监控设备在线状态
- ✅ 性能监控：监控API响应时间、系统性能

## 技术实现

### 核心模块
- **计费管理**：BillingController、BillingService
- **宿舍管理**：DormRoomController、DormRoomService
- **设备管理**：DeviceController、DeviceGroupController、DeviceStatusHistoryController
- **用户管理**：UserController、AuthController、SecurityConfig
- **遥测数据**：TelemetryController、TelemetryService
- **命令控制**：CommandController、ScheduledTaskController、CommandService
- **告警系统**：AlertController、AlertService
- **系统管理**：SystemConfigController、SystemLogController、DataBackupController
- **通知系统**：NotificationController、EmailService
- **监控系统**：MonitoringController、MonitoringService

### 数据模型
- ElectricityPriceRule：电价规则（阶梯电价、时段电价）
- ElectricityBill：电费账单
- RechargeRecord：充值记录
- RoomBalance：房间余额
- Building：宿舍楼栋
- DormRoom：宿舍房间
- Device：设备信息
- DeviceGroup：设备分组
- DeviceStatusHistory：设备状态历史
- UserAccount：用户账户
- CommandRecord：命令记录
- ScheduledTask：定时任务
- DeviceAlert：设备告警
- Telemetry：遥测数据
- SystemConfig：系统配置
- SystemLog：系统日志
- DataBackup：数据备份
- Notification：系统通知
- SystemMetrics：系统监控指标
- Student：学生/住户信息
- StudentRoomHistory：学生入住历史

### API接口
- **计费管理**：/api/billing/price-rules、/api/billing/bills、/api/billing/recharge、/api/billing/balance
- **宿舍管理**：/api/dorm/buildings、/api/dorm/rooms
- **设备管理**：/api/devices、/api/groups
- **用户管理**：/api/auth、/api/users
- **遥测数据**：/api/telemetry
- **命令控制**：/api/strips/{deviceId}/cmd、/api/commands、/api/tasks
- **告警系统**：/api/alerts
- **系统配置**：/api/admin/config
- **日志管理**：/api/admin/logs
- **数据备份**：/api/admin/backup
- **通知系统**：/api/notifications
- **监控系统**：/api/admin/monitor
- **学生管理**：/api/students

### 10. 学生/住户管理 ✅
- ✅ 学生/住户数据模型：Student、StudentRoomHistory实体类
- ✅ 学生入住/退宿管理：支持办理入住和退宿手续
- ✅ 学生与房间绑定：学生与宿舍房间的关联管理
- ✅ 学生入住历史记录：记录学生的所有入住历史
- ✅ 学生统计信息：获取学生数量、入住率等统计数据
- ✅ 批量毕业处理：批量处理指定毕业年份的学生
- ✅ 未分配房间学生查询：查询所有未分配宿舍的在读学生

### API覆盖详情
已测试的API端点包括：
- **认证模块**: /api/auth/login, /api/auth/register, /api/auth/logout, /api/auth/me, /api/auth/refresh, /api/auth/forgot-password
- **用户管理**: /api/users, /api/users/{username}, /api/users/{username}/password
- **计费管理**: /api/billing/price-rules, /api/billing/bills, /api/billing/bills/pending, /api/billing/recharge, /api/billing/recharge-records, /api/billing/balance/{roomId}, /api/billing/low-balance
- **宿舍管理**: /api/dorm/buildings, /api/dorm/rooms, /api/dorm/buildings/{buildingId}/rooms, /api/dorm/rooms/statistics
- **设备管理**: /api/devices, /api/devices/{deviceId}/status, /api/devices/room/{room}, /api/devices/{deviceId}/history
- **设备分组**: /api/groups, /api/groups/{groupId}, /api/groups/{groupId}/devices
- **命令控制**: /api/strips/{deviceId}/cmd, /api/cmd/{cmdId}, /api/commands/device/{deviceId}, /api/commands/batch
- **遥测数据**: /api/telemetry, /api/telemetry/statistics, /api/telemetry/export
- **AI报告**: /api/rooms/{roomId}/ai_report
- **告警管理**: /api/alerts/device/{deviceId}, /api/alerts/unresolved, /api/alerts/config/{deviceId}
- **定时任务**: /api/tasks, /api/tasks/device/{deviceId}
- **系统配置**: /api/admin/config, /api/admin/config/category/{category}, /api/admin/config/init
- **日志管理**: /api/admin/logs, /api/admin/logs/statistics
- **数据备份**: /api/admin/backup, /api/admin/backup/statistics
- **通知系统**: /api/notifications, /api/notifications/unread/count
- **监控系统**: /api/admin/monitor/system, /api/admin/monitor/devices, /api/admin/monitor/api-performance
- **健康检查**: /health
- **学生管理**: /api/students, /api/students/{id}, /api/students/search, /api/students/unassigned, /api/students/statistics
- **RBAC管理**: /api/rbac/roles, /api/rbac/permissions, /api/rbac/resources, /api/rbac/resources/tree, /api/rbac/users/{username}/roles, /api/rbac/users/{username}/has-permission, /api/rbac/init

### 学生管理核心模块
- **StudentController**: 学生管理REST API接口
- **StudentService**: 学生管理业务逻辑
- **StudentRepository**: 学生数据访问层
- **StudentRoomHistoryRepository**: 学生入住历史数据访问层

## 技术栈

- **后端框架**：Spring Boot 3.2.3
- **数据库**：PostgreSQL
- **认证**：JWT
- **消息队列**：MQTT
- **WebSocket**：实时通知
- **API文档**：Swagger OpenAPI 3.0
- **测试**：Python requests 库

## 部署说明

1. 构建项目：`mvn clean package -DskipTests`
2. 运行应用：`java -jar target/dorm-power-backend-1.0.0.jar`
3. 访问API文档：http://localhost:8000/swagger-ui.html
4. 运行测试：`python test_api.py`
5. 运行系统管理测试：`python test_system_management.py`

## 新增功能说明

### 系统配置管理
- 支持动态配置系统参数
- 支持分类管理配置（system、email、alert、backup、monitor）
- 支持批量更新配置
- 支持配置初始化

### 日志管理
- 支持多级别日志记录（INFO、WARN、ERROR）
- 支持按类型、级别、用户查询日志
- 支持日志搜索功能
- 支持自动清理过期日志

### 数据备份
- 支持数据库备份（pg_dump）
- 支持数据导出备份
- 支持自动定期备份
- 支持备份文件管理

### 通知系统
- 支持系统内通知
- 支持邮件通知
- 支持告警通知
- 支持通知已读/未读管理

### 监控系统
- 支持系统资源监控（CPU、内存、运行时间）
- 支持设备在线状态监控
- 支持API性能监控
- 支持定时任务监控

### 11. RBAC权限管理系统 ✅
- ✅ 角色管理：支持动态创建、编辑、删除角色
- ✅ 权限管理：支持细粒度的权限定义和分配
- ✅ 资源管理：支持API、菜单、按钮等资源类型管理
- ✅ 用户角色分配：支持为用户分配多个角色
- ✅ 权限继承：角色与权限的多对多关系
- ✅ 资源树结构：支持资源的层级管理
- ✅ 默认数据初始化：自动创建默认角色、权限和资源
- ✅ 权限检查API：支持检查用户是否拥有特定权限

#### RBAC核心模块
- **RbacController**: RBAC管理REST API接口
- **RbacService**: RBAC业务逻辑
- **RoleRepository**: 角色数据访问层
- **PermissionRepository**: 权限数据访问层
- **ResourceRepository**: 资源数据访问层
- **UserRoleRepository**: 用户角色关联数据访问层

#### RBAC数据模型
- Role：角色实体（code、name、description、enabled、system）
- Permission：权限实体（code、name、resource、action、enabled）
- Resource：资源实体（code、name、type、url、method、parentId、sortOrder）
- UserRole：用户角色关联实体（username、roleId、assignedAt、assignedBy）

#### RBAC API接口
- **角色管理**: /api/rbac/roles, /api/rbac/roles/{roleId}
- **权限管理**: /api/rbac/permissions, /api/rbac/permissions/{permissionId}
- **资源管理**: /api/rbac/resources, /api/rbac/resources/tree, /api/rbac/resources/{resourceId}
- **用户角色**: /api/rbac/users/{username}/roles, /api/rbac/users/{username}/has-permission
- **初始化**: /api/rbac/init

### 12. 通知偏好设置 ✅
- ✅ 用户通知偏好存储：支持用户个性化通知设置持久化
- ✅ 邮件通知开关：控制是否接收邮件通知
- ✅ 系统通知开关：控制是否接收系统内通知
- ✅ 告警通知开关：控制是否接收告警通知
- ✅ 账单通知开关：控制是否接收账单相关通知
- ✅ 维护通知开关：控制是否接收维护相关通知
- ✅ 免打扰时段：设置免打扰时间范围
- ✅ 告警级别过滤：按级别过滤告警通知

#### 通知偏好设置API
- **获取偏好设置**: GET /api/notifications/preferences
- **更新偏好设置**: PUT /api/notifications/preferences
- **重置偏好设置**: POST /api/notifications/preferences/reset
- **检查通知启用**: GET /api/notifications/preferences/enabled
- **检查免打扰**: GET /api/notifications/preferences/quiet-hours

## 测试结果

### 测试覆盖
- 功能测试：73/74 通过 (98.6%)
- 边界测试：8/8 通过 (100.0%)
- 安全测试：4/4 通过 (100.0%)
- 性能测试：4/4 通过 (100.0%)
- 数据测试：2/2 通过 (100.0%)
- 集成测试：3/3 通过 (100.0%)

### 总体统计
- 总测试数：97
- 通过：96
- 失败：1 (429限流)
- 警告：0
- 跳过：0
- 总耗时：22.02s
- 覆盖率：99.0%

### 13. 数据字典管理 ✅
- ✅ 字典类型管理：支持多种字典类型
- ✅ 字典项CRUD：完整的增删改查
- ✅ 树形结构：支持父子层级关系
- ✅ 系统字典：预置系统字典数据
- ✅ 字典标签获取：根据类型和编码获取标签

#### 数据字典API
- **获取字典类型列表**: GET /api/dict/types
- **获取字典项列表**: GET /api/dict/type/{dictType}
- **获取字典树**: GET /api/dict/tree/{dictType}
- **分页查询**: GET /api/dict/page
- **创建字典项**: POST /api/dict
- **批量创建**: POST /api/dict/batch
- **更新字典项**: PUT /api/dict/{id}
- **删除字典项**: DELETE /api/dict/{id}

### 14. 登录日志审计 ✅
- ✅ 登录成功记录：记录IP、浏览器、操作系统
- ✅ 登录失败记录：记录失败原因
- ✅ 登出记录：计算会话时长
- ✅ 账户锁定检测：连续失败锁定
- ✅ IP封禁检测：异常IP封禁
- ✅ 登录统计：活跃用户、IP统计

#### 登录日志API
- **获取日志列表**: GET /api/login-logs
- **获取用户历史**: GET /api/login-logs/user/{username}
- **时间范围查询**: GET /api/login-logs/range
- **登录统计**: GET /api/login-logs/statistics
- **检查锁定状态**: GET /api/login-logs/locked/{username}
- **检查IP封禁**: GET /api/login-logs/blocked-ip/{ipAddress}

### 15. 操作审计日志 ✅
- ✅ 操作记录：记录模块、操作、目标
- ✅ 请求响应记录：记录请求参数和响应
- ✅ 时间范围查询：支持按时间范围筛选
- ✅ 模块统计：按模块统计操作次数
- ✅ 用户审计：追踪用户操作历史

#### 审计日志API
- **获取日志列表**: GET /api/audit-logs
- **用户审计**: GET /api/audit-logs/user/{username}
- **模块审计**: GET /api/audit-logs/module/{module}
- **时间范围查询**: GET /api/audit-logs/range
- **审计统计**: GET /api/audit-logs/statistics

### 16. 自动断电控制 ✅
- ✅ 余额监控：实时监控房间余额
- ✅ 自动断电：余额不足自动断电
- ✅ 手动断电：管理员手动断电
- ✅ 恢复供电：充值后恢复供电
- ✅ 预警通知：低余额预警

#### 断电控制API
- **手动断电**: POST /api/power-control/cutoff/{roomId}
- **恢复供电**: POST /api/power-control/restore/{roomId}
- **断电状态**: GET /api/power-control/status/{roomId}
- **断电房间列表**: GET /api/power-control/cutoff-rooms
- **欠费房间列表**: GET /api/power-control/overdue-rooms

### 17. 自动催缴通知 ✅
- ✅ 催缴记录管理：创建和管理催缴记录
- ✅ 多渠道通知：邮件、系统通知、短信
- ✅ 定时发送：定时检查并发送催缴
- ✅ 重试机制：发送失败自动重试
- ✅ 余额预警：低余额自动预警

#### 催缴管理API
- **创建催缴记录**: POST /api/collections
- **获取催缴记录**: GET /api/collections
- **房间催缴记录**: GET /api/collections/room/{roomId}
- **账单催缴记录**: GET /api/collections/bill/{billId}

### 18. IP访问控制 ✅
- ✅ 白名单管理：添加、删除、更新白名单
- ✅ 黑名单管理：添加、删除、更新黑名单
- ✅ CIDR支持：支持网段匹配
- ✅ 过期时间：支持设置过期时间
- ✅ 访问检查：实时检查IP访问权限

#### IP访问控制API
- **添加白名单**: POST /api/ip-control/whitelist
- **添加黑名单**: POST /api/ip-control/blacklist
- **移除IP控制**: DELETE /api/ip-control/{ipAddress}
- **获取白名单**: GET /api/ip-control/whitelist
- **获取黑名单**: GET /api/ip-control/blacklist
- **检查IP权限**: GET /api/ip-control/check/{ipAddress}

### 19. 消息模板管理 ✅
- ✅ 模板CRUD：完整的增删改查
- ✅ 变量渲染：支持${variable}变量替换
- ✅ 多渠道模板：邮件、系统、短信模板
- ✅ 系统模板：预置系统消息模板
- ✅ HTML支持：支持HTML格式邮件

#### 消息模板API
- **创建模板**: POST /api/message-templates
- **更新模板**: PUT /api/message-templates/{id}
- **删除模板**: DELETE /api/message-templates/{id}
- **获取模板**: GET /api/message-templates/code/{templateCode}
- **渲染模板**: POST /api/message-templates/render/{templateCode}

### 20. 设备固件升级(OTA) ✅
- ✅ 升级任务管理：创建和管理升级任务
- ✅ 进度跟踪：实时跟踪升级进度
- ✅ 超时检测：自动检测升级超时
- ✅ 升级命令下发：通过MQTT下发升级命令
- ✅ 升级历史：查看设备升级历史

#### 固件升级API
- **发起升级**: POST /api/firmware/upgrade
- **发送升级命令**: POST /api/firmware/{firmwareId}/send
- **更新进度**: PUT /api/firmware/{firmwareId}/progress
- **完成升级**: POST /api/firmware/{firmwareId}/complete
- **取消升级**: POST /api/firmware/{firmwareId}/cancel
- **设备升级历史**: GET /api/firmware/device/{deviceId}

### 21. 数据批量导入 ✅
- ✅ 学生批量导入：CSV格式导入学生数据
- ✅ 房间批量导入：CSV格式导入房间数据
- ✅ 设备批量导入：CSV格式导入设备数据
- ✅ JSON导入：支持JSON格式数据导入
- ✅ 导入模板：提供CSV导入模板下载

#### 数据导入API
- **导入学生**: POST /api/import/students
- **导入房间**: POST /api/import/rooms
- **导入设备**: POST /api/import/devices
- **导入JSON**: POST /api/import/json
- **获取模板**: GET /api/import/template/{type}

### 22. 智能Agent系统 ✅
- ✅ 用电行为学习：K-Means聚类、周期性检测、模式挖掘
- ✅ 异常检测：Z-Score、IQR、趋势分析、预测性检测
- ✅ 智能场景引擎：自动生成、定时触发、效果评估
- ✅ 设备健康评估：健康评分、故障预测
- ✅ 综合分析：行为画像、风险评分、智能建议

#### 核心ML算法（纯Java实现，无外部依赖）
- **Z-Score异常检测**: 基于标准差的点异常检测
- **IQR异常检测**: 基于四分位距的鲁棒异常检测
- **K-Means聚类**: 用电时段自动分组
- **EWMA预测**: 指数加权移动平均预测
- **周期性检测**: 自相关系数计算
- **DTW距离**: 时间序列相似度比较
- **关联规则挖掘**: 设备使用关联分析

#### 智能Agent API
**行为学习**
- **学习用电行为**: GET /api/agent/behavior/learn/{roomId}
- **生成场景建议**: GET /api/agent/behavior/suggestions/{roomId}
- **预测用电**: GET /api/agent/behavior/predict/{roomId}
- **比较用电时段**: GET /api/agent/behavior/compare/{roomId}

**异常检测**
- **实时异常检测**: POST /api/agent/anomaly/detect/realtime/{deviceId}
- **批量异常检测**: GET /api/agent/anomaly/detect/batch/{deviceId}
- **预测设备异常**: GET /api/agent/anomaly/predict/{deviceId}
- **设备健康状态**: GET /api/agent/anomaly/health/{deviceId}
- **房间异常检测**: GET /api/agent/anomaly/room/{roomId}
- **设备故障预测**: GET /api/agent/anomaly/failure/{deviceId}

**智能场景**
- **自动生成场景**: POST /api/agent/scene/auto-generate/{roomId}
- **获取房间场景**: GET /api/agent/scene/room/{roomId}
- **执行场景**: POST /api/agent/scene/execute/{sceneId}
- **启用/禁用场景**: PUT /api/agent/scene/toggle/{sceneId}
- **删除场景**: DELETE /api/agent/scene/{sceneId}
- **场景统计**: GET /api/agent/scene/statistics/{sceneId}
- **执行历史**: GET /api/agent/scene/history/{sceneId}
- **优化场景**: POST /api/agent/scene/optimize/{sceneId}

**综合分析**
- **房间智能分析**: GET /api/agent/analyze/{roomId}

### 23. AI客服Agent系统 ✅
- ✅ 意图识别：基于关键词的轻量级意图识别
- ✅ 实体提取：自动提取房间号、设备名、时间等实体
- ✅ LLM集成：DeepSeek API集成，支持智能对话
- ✅ 快速匹配：高频问题的快速回复
- ✅ 上下文管理：对话历史记录
- ✅ API调用：根据意图自动调用对应API

#### AI客服核心模块
- **IntentRecognizer**: 意图识别模块（本地规则匹配）
- **LLMService**: 大模型服务（DeepSeek API集成）
- **AgentController**: AI客服REST API接口

#### 支持的意图类型
- **DEVICE_CONTROL**: 设备控制（开关、调节）
- **POWER_QUERY**: 用电查询（电量、功率、余额）
- **DEVICE_STATUS**: 设备状态（在线、离线、异常）
- **BILL_QUERY**: 账单查询（电费账单、缴费）
- **ALARM_QUERY**: 告警查询（告警、故障）
- **SCENE_CONTROL**: 场景控制（智能模式、定时）
- **SYSTEM_HELP**: 系统帮助（功能介绍、使用说明）

#### AI客服API
- **智能对话**: POST /api/agent/chat
- **意图识别**: POST /api/agent/intent
- **快速问答**: POST /api/agent/quick
- **健康检查**: GET /api/agent/health

#### 配置说明
```yaml
llm:
  provider: deepseek
  api-key: sk-9ae959c6e2f14426a593478848f5a60f
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
  max-tokens: 500
  temperature: 0.7
  timeout: 30
```

## 测试结果

### 测试覆盖（最新）
- 功能测试：87/87 通过 (100.0%)
- 边界测试：11/11 通过 (100.0%)
- 安全测试：4/4 通过 (100.0%)
- 性能测试：4/4 通过 (100.0%)
- 数据测试：2/2 通过 (100.0%)
- 集成测试：3/3 通过 (100.0%)

### 总体统计
- 总测试数：113
- 通过：113
- 失败：0
- 警告：0
- 跳过：0
- 总耗时：25.17s
- 覆盖率：100.0%
