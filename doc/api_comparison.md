# 前后端API对比分析

## 后端API统计
- **控制器总数**: 30个
- **API端点总数**: 234个

## 前端已实现的API模块（17个）

| 序号 | API模块 | 对应后端控制器 | 状态 |
|------|---------|----------------|------|
| 1 | authApi | AuthController | ✅ 已实现 |
| 2 | deviceApi | DeviceController | ✅ 已实现 |
| 3 | userApi | UserController | ✅ 已实现 |
| 4 | commandApi | CommandController | ✅ 已实现 |
| 5 | groupApi | DeviceGroupController | ✅ 已实现 |
| 6 | alertApi | AlertController | ✅ 已实现 |
| 7 | taskApi | ScheduledTaskController | ✅ 已实现 |
| 8 | systemConfigApi | SystemConfigController | ✅ 已实现 |
| 9 | logApi | SystemLogController | ✅ 已实现 |
| 10 | backupApi | DataBackupController | ✅ 已实现 |
| 11 | notificationApi | NotificationController | ✅ 已实现 |
| 12 | monitorApi | MonitoringController | ✅ 已实现 |
| 13 | healthApi | HealthController | ✅ 已实现 |
| 14 | billingApi | BillingController | ✅ 已实现 |
| 15 | aiReportApi | AiReportController | ✅ 已实现 |
| 16 | dormApi | DormRoomController | ✅ 已实现 |
| 17 | studentApi | StudentController | ✅ 已实现 |

## 前端缺失的API模块（13个）

| 序号 | 后端控制器 | API端点数 | 优先级 | 建议页面 |
|------|------------|-----------|--------|----------|
| 1 | **AgentController** | 19个 | ⭐⭐⭐ 高 | AI智能代理页面 |
| 2 | **RbacController** | 28个 | ⭐⭐⭐ 高 | 权限管理页面 |
| 3 | **IpAccessControlController** | 11个 | ⭐⭐ 中 | IP访问控制页面 |
| 4 | **LoginLogController** | 8个 | ⭐⭐ 中 | 登录日志页面 |
| 5 | **AuditLogController** | 6个 | ⭐⭐ 中 | 审计日志页面 |
| 6 | **MessageTemplateController** | 9个 | ⭐ 低 | 消息模板页面 |
| 7 | **DataDictController** | 10个 | ⭐ 低 | 数据字典页面 |
| 8 | **CollectionController** | 3个 | ⭐ 低 | 收款管理页面 |
| 9 | **DeviceFirmwareController** | 10个 | ⭐⭐ 中 | 固件管理页面 |
| 10 | **PowerControlController** | 5个 | ⭐⭐⭐ 高 | 电源控制页面 |
| 11 | **ImportController** | 5个 | ⭐⭐ 中 | 数据导入页面 |
| 12 | **DeviceStatusHistoryController** | 2个 | ⭐ 低 | 设备历史页面 |
| 13 | **TelemetryController** | 3个 | ⭐ 低 | 遥测数据页面 |

## 需要创建的页面（按优先级排序）

### 高优先级（3个）
1. **AI智能代理页面** - AgentController (19个端点)
   - 行为学习
   - 异常检测
   - 场景管理

2. **权限管理页面** - RbacController (28个端点)
   - 角色管理
   - 权限管理
   - 资源管理
   - 用户权限分配

3. **电源控制页面** - PowerControlController (5个端点)
   - 断电控制
   - 恢复供电
   - 欠费房间管理

### 中优先级（5个）
4. **IP访问控制页面** - IpAccessControlController (11个端点)
   - 黑名单管理
   - 白名单管理

5. **登录日志页面** - LoginLogController (8个端点)
   - 登录记录查询
   - 统计分析

6. **审计日志页面** - AuditLogController (6个端点)
   - 操作日志查询
   - 统计分析

7. **固件管理页面** - DeviceFirmwareController (10个端点)
   - 固件升级
   - 进度跟踪

8. **数据导入页面** - ImportController (5个端点)
   - 学生导入
   - 房间导入
   - 设备导入

### 低优先级（5个）
9. **消息模板页面** - MessageTemplateController (9个端点)
10. **数据字典页面** - DataDictController (10个端点)
11. **收款管理页面** - CollectionController (3个端点)
12. **设备历史页面** - DeviceStatusHistoryController (2个端点)
13. **遥测数据页面** - TelemetryController (3个端点)

## 实施计划

### 第一阶段：高优先级页面
1. 创建AI智能代理页面及相关API
2. 创建权限管理页面及相关API
3. 创建电源控制页面及相关API

### 第二阶段：中优先级页面
4. 创建IP访问控制页面及相关API
5. 创建登录日志页面及相关API
6. 创建审计日志页面及相关API
7. 创建固件管理页面及相关API
8. 创建数据导入页面及相关API

### 第三阶段：低优先级页面
9. 创建消息模板页面及相关API
10. 创建数据字典页面及相关API
11. 创建收款管理页面及相关API
12. 创建设备历史页面及相关API
13. 创建遥测数据页面及相关API
