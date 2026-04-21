# 宿舍电源管理系统API接口文档

## 1. 接口概述

本API接口文档详细描述了宿舍电源管理系统的后端API接口，包括健康检查、认证、设备管理、遥测数据、命令控制、AI分析、告警管理、系统监控等完整功能模块。接口采用RESTful设计风格，使用HTTP/HTTPS协议进行通信，数据格式为JSON。

### 1.1 基础信息

- **Base URL**：
  - 开发环境：`http://localhost:8000`
  - 生产环境：`http://175.27.162.174:8000`

- **API前缀**：`/api`

- **WebSocket**：`/ws`

- **数据格式**：`application/json`

- **时间戳格式**：Unix秒（number）

### 1.2 通用错误结构

```json
{
  "ok": false,
  "code": "BAD_REQUEST",
  "message": "range is invalid",
  "details": {}
}
```

### 1.3 状态码

| 状态码 | 描述 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 404 | 资源不存在 |
| 409 | 冲突 |
| 500 | 服务错误 |

### 1.4 认证方式

所有需要认证的接口使用Bearer Token认证：

```
Authorization: Bearer {token}
```

## 2. 接口列表

### 2.1 健康检查

#### 2.1.1 健康检查

- **接口路径**：`GET /health`

- **功能描述**：检查系统健康状态

- **响应**：

```json
{
  "status": "UP",
  "service": "dorm-power-backend",
  "version": "1.0.0"
}
```

### 2.2 认证管理

#### 2.2.1 用户登录

- **接口路径**：`POST /api/auth/login`

- **功能描述**：使用账号密码登录系统，返回JWT令牌

- **请求体**：

```json
{
  "account": "admin",
  "password": "admin123"
}
```

- **响应**：

```json
{
  "ok": true,
  "token": "xxxxx",
  "user": {
    "username": "admin",
    "email": "admin@dorm.local",
    "role": "admin"
  }
}
```

#### 2.2.2 用户注册

- **接口路径**：`POST /api/auth/register`

- **功能描述**：注册新用户，返回JWT令牌

- **请求体**：

```json
{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123"
}
```

#### 2.2.3 忘记密码

- **接口路径**：`POST /api/auth/forgot-password`

- **功能描述**：发送密码重置邮件

- **请求体**：

```json
{
  "email": "user@example.com"
}
```

#### 2.2.4 重置密码

- **接口路径**：`POST /api/auth/reset-password`

- **功能描述**：使用重置码更新密码

- **请求体**：

```json
{
  "email": "user@example.com",
  "resetCode": "xxx",
  "newPassword": "newpass123"
}
```

#### 2.2.5 用户登出

- **接口路径**：`POST /api/auth/logout`

- **功能描述**：退出登录状态

- **需要认证**：是

#### 2.2.6 获取当前用户

- **接口路径**：`GET /api/auth/me`

- **功能描述**：获取当前登录用户的信息

- **需要认证**：是

#### 2.2.7 刷新令牌

- **接口路径**：`POST /api/auth/refresh`

- **功能描述**：刷新JWT令牌，延长有效期

- **需要认证**：是

### 2.3 设备管理

#### 2.3.1 获取设备列表

- **接口路径**：`GET /api/devices`

- **功能描述**：获取所有设备的列表信息

- **需要认证**：是

- **响应**：

```json
[
  {
    "id": "strip01",
    "name": "宿舍302-插排01",
    "room": "A-302",
    "online": true,
    "lastSeen": "2026-02-21T10:20:30.000Z"
  }
]
```

#### 2.3.2 获取设备状态

- **接口路径**：`GET /api/devices/{deviceId}/status`

- **功能描述**：获取指定设备的详细状态信息

- **需要认证**：是

- **路径参数**：
  - `deviceId`：设备ID

- **响应**：

```json
{
  "ts": 1771650000,
  "online": true,
  "total_power_w": 132.5,
  "voltage_v": 221.3,
  "current_a": 0.62,
  "sockets": [
    {"id": 1, "on": true, "power_w": 92.1, "device": "PC"},
    {"id": 2, "on": false, "power_w": 0.0, "device": "None"}
  ]
}
```

#### 2.3.3 获取房间设备列表

- **接口路径**：`GET /api/devices/room/{room}`

- **功能描述**：获取指定房间的设备列表

- **需要认证**：是

- **路径参数**：
  - `room`：房间ID

#### 2.3.4 创建设备

- **接口路径**：`POST /api/devices`

- **功能描述**：创建新的设备

- **需要认证**：是

#### 2.3.5 更新设备

- **接口路径**：`PUT /api/devices/{deviceId}`

- **功能描述**：更新指定设备的信息

- **需要认证**：是

#### 2.3.6 删除设备

- **接口路径**：`DELETE /api/devices/{deviceId}`

- **功能描述**：删除指定的设备

- **需要认证**：是

#### 2.3.7 批量删除设备

- **接口路径**：`DELETE /api/devices/batch`

- **功能描述**：批量删除指定的多个设备

- **需要认证**：是

- **请求体**：

```json
["device_001", "device_002", "device_003"]
```

- **响应**：

```json
{
  "message": "Devices deleted successfully",
  "count": 3
}
```

### 2.4 设备分组

#### 2.4.1 获取分组详情

- **接口路径**：`GET /api/groups/{groupId}`

- **功能描述**：获取分组详情

- **需要认证**：是

#### 2.4.2 获取分组设备

- **接口路径**：`GET /api/groups/{groupId}/devices`

- **功能描述**：获取分组设备列表

- **需要认证**：是

#### 2.4.3 获取设备所属分组

- **接口路径**：`GET /api/groups/device/{deviceId}`

- **功能描述**：获取设备所属分组

- **需要认证**：是

#### 2.4.4 添加设备到分组

- **接口路径**：`POST /api/groups/{groupId}/devices`

- **功能描述**：添加设备到分组

- **需要认证**：是

#### 2.4.5 更新分组

- **接口路径**：`PUT /api/groups/{groupId}`

- **功能描述**：更新分组信息

- **需要认证**：是

#### 2.4.6 删除分组

- **接口路径**：`DELETE /api/groups/{groupId}`

- **功能描述**：删除分组

- **需要认证**：是

#### 2.4.7 从分组移除设备

- **接口路径**：`DELETE /api/groups/{groupId}/devices/{deviceId}`

- **功能描述**：从分组移除设备

- **需要认证**：是

### 2.5 设备固件

#### 2.5.1 获取设备固件列表

- **接口路径**：`GET /api/firmware/device/{deviceId}`

- **功能描述**：获取设备固件列表

- **需要认证**：是

#### 2.5.2 获取当前固件

- **接口路径**：`GET /api/firmware/device/{deviceId}/current`

- **功能描述**：获取设备当前固件

- **需要认证**：是

#### 2.5.3 获取待升级任务

- **接口路径**：`GET /api/firmware/pending`

- **功能描述**：获取待升级固件任务

- **需要认证**：是

#### 2.5.4 获取进行中任务

- **接口路径**：`GET /api/firmware/active`

- **功能描述**：获取进行中的升级任务

- **需要认证**：是

#### 2.5.5 获取固件详情

- **接口路径**：`GET /api/firmware/{firmwareId}`

- **功能描述**：获取固件详情

- **需要认证**：是

#### 2.5.6 创建升级任务

- **接口路径**：`POST /api/firmware/upgrade`

- **功能描述**：创建固件升级任务

- **需要认证**：是

#### 2.5.7 发送升级任务

- **接口路径**：`POST /api/firmware/{firmwareId}/send`

- **功能描述**：发送升级任务到设备

- **需要认证**：是

#### 2.5.8 完成升级任务

- **接口路径**：`POST /api/firmware/{firmwareId}/complete`

- **功能描述**：标记升级任务完成

- **需要认证**：是

#### 2.5.9 取消升级任务

- **接口路径**：`POST /api/firmware/{firmwareId}/cancel`

- **功能描述**：取消升级任务

- **需要认证**：是

#### 2.5.10 更新升级进度

- **接口路径**：`PUT /api/firmware/{firmwareId}/progress`

- **功能描述**：更新升级进度

- **需要认证**：是

### 2.6 宿舍管理

#### 2.6.1 获取楼栋列表

- **接口路径**：`GET /api/dorm/buildings`

- **功能描述**：获取所有楼栋

- **需要认证**：是

#### 2.6.2 获取房间列表

- **接口路径**：`GET /api/dorm/rooms`

- **功能描述**：获取所有房间

- **需要认证**：是

#### 2.6.3 获取楼栋房间

- **接口路径**：`GET /api/dorm/buildings/{buildingId}/rooms`

- **功能描述**：获取指定楼栋的房间

- **需要认证**：是

#### 2.6.4 获取楼层房间

- **接口路径**：`GET /api/dorm/buildings/{buildingId}/floors/{floor}/rooms`

- **功能描述**：获取指定楼层的房间

- **需要认证**：是

#### 2.6.5 获取统计信息

- **接口路径**：`GET /api/dorm/rooms/statistics`

- **功能描述**：获取房间统计信息

- **需要认证**：是

#### 2.6.6 创建楼栋

- **接口路径**：`POST /api/dorm/buildings`

- **功能描述**：创建新楼栋

- **需要认证**：是

#### 2.6.7 创建房间

- **接口路径**：`POST /api/dorm/rooms`

- **功能描述**：创建新房间

- **需要认证**：是

#### 2.6.8 学生入住

- **接口路径**：`POST /api/dorm/rooms/{roomId}/check-in`

- **功能描述**：学生入住

- **需要认证**：是

#### 2.6.9 学生退宿

- **接口路径**：`POST /api/dorm/rooms/{roomId}/check-out`

- **功能描述**：学生退宿

- **需要认证**：是

#### 2.6.10 更新楼栋

- **接口路径**：`PUT /api/dorm/buildings/{id}`

- **功能描述**：更新楼栋信息

- **需要认证**：是

#### 2.6.11 更新房间

- **接口路径**：`PUT /api/dorm/rooms/{id}`

- **功能描述**：更新房间信息

- **需要认证**：是

#### 2.6.12 删除楼栋

- **接口路径**：`DELETE /api/dorm/buildings/{id}`

- **功能描述**：删除楼栋

- **需要认证**：是

#### 2.6.13 删除房间

- **接口路径**：`DELETE /api/dorm/rooms/{id}`

- **功能描述**：删除房间

- **需要认证**：是

### 2.7 学生管理

#### 2.7.1 获取学生详情

- **接口路径**：`GET /api/students/{id}`

- **功能描述**：获取学生详情

- **需要认证**：是

#### 2.7.2 按学号获取

- **接口路径**：`GET /api/students/number/{studentNumber}`

- **功能描述**：按学号获取学生

- **需要认证**：是

#### 2.7.3 按房间获取

- **接口路径**：`GET /api/students/room/{roomId}`

- **功能描述**：按房间获取学生

- **需要认证**：是

#### 2.7.4 获取学生历史

- **接口路径**：`GET /api/students/{studentId}/history`

- **功能描述**：获取学生入住历史

- **需要认证**：是

#### 2.7.5 搜索学生

- **接口路径**：`GET /api/students/search`

- **功能描述**：搜索学生

- **需要认证**：是

#### 2.7.6 获取未分配学生

- **接口路径**：`GET /api/students/unassigned`

- **功能描述**：获取未分配房间的学生

- **需要认证**：是

#### 2.7.7 获取学生统计

- **接口路径**：`GET /api/students/statistics`

- **功能描述**：获取学生统计信息

- **需要认证**：是

#### 2.7.8 学生入住

- **接口路径**：`POST /api/students/{studentId}/check-in`

- **功能描述**：学生入住

- **需要认证**：是

#### 2.7.9 学生退宿

- **接口路径**：`POST /api/students/{studentId}/check-out`

- **功能描述**：学生退宿

- **需要认证**：是

#### 2.7.10 调换房间

- **接口路径**：`POST /api/students/{studentId}/swap-room`

- **功能描述**：调换房间

- **需要认证**：是

#### 2.7.11 批量毕业

- **接口路径**：`POST /api/students/batch/graduate`

- **功能描述**：批量学生毕业

- **需要认证**：是

#### 2.7.12 更新学生

- **接口路径**：`PUT /api/students/{id}`

- **功能描述**：更新学生信息

- **需要认证**：是

#### 2.7.13 删除学生

- **接口路径**：`DELETE /api/students/{id}`

- **功能描述**：删除学生

- **需要认证**：是

### 2.8 计费管理

#### 2.8.1 获取价格规则

- **接口路径**：`GET /api/billing/price-rules`

- **功能描述**：获取电价规则

- **需要认证**：是

#### 2.8.2 获取账单列表

- **接口路径**：`GET /api/billing/bills`

- **功能描述**：获取账单列表

- **需要认证**：是

#### 2.8.3 获取待支付账单

- **接口路径**：`GET /api/billing/bills/pending`

- **功能描述**：获取待支付账单

- **需要认证**：是

#### 2.8.4 获取充值记录

- **接口路径**：`GET /api/billing/recharge-records`

- **功能描述**：获取充值记录

- **需要认证**：是

#### 2.8.5 获取房间余额

- **接口路径**：`GET /api/billing/balance/{roomId}`

- **功能描述**：获取房间余额

- **需要认证**：是

#### 2.8.6 获取低余额房间

- **接口路径**：`GET /api/billing/low-balance`

- **功能描述**：获取低余额房间

- **需要认证**：是

#### 2.8.7 创建价格规则

- **接口路径**：`POST /api/billing/price-rules`

- **功能描述**：创建电价规则

- **需要认证**：是

#### 2.8.8 生成账单

- **接口路径**：`POST /api/billing/bills/generate`

- **功能描述**：生成账单

- **需要认证**：是

#### 2.8.9 支付账单

- **接口路径**：`POST /api/billing/bills/{billId}/pay`

- **功能描述**：支付账单

- **需要认证**：是

#### 2.8.10 充值

- **接口路径**：`POST /api/billing/recharge`

- **功能描述**：房间充值

- **需要认证**：是

#### 2.8.11 更新价格规则

- **接口路径**：`PUT /api/billing/price-rules/{id}`

- **功能描述**：更新电价规则

- **需要认证**：是

#### 2.8.12 删除价格规则

- **接口路径**：`DELETE /api/billing/price-rules/{id}`

- **功能描述**：删除电价规则

- **需要认证**：是

### 2.9 电源控制

#### 2.9.1 获取房间电源状态

- **接口路径**：`GET /api/power-control/status/{roomId}`

- **功能描述**：获取房间电源状态

- **需要认证**：是

#### 2.9.2 获取断电房间

- **接口路径**：`GET /api/power-control/cutoff-rooms`

- **功能描述**：获取已断电的房间

- **需要认证**：是

#### 2.9.3 获取欠费房间

- **接口路径**：`GET /api/power-control/overdue-rooms`

- **功能描述**：获取欠费房间

- **需要认证**：是

#### 2.9.4 断电

- **接口路径**：`POST /api/power-control/cutoff/{roomId}`

- **功能描述**：房间断电

- **需要认证**：是

#### 2.9.5 恢复供电

- **接口路径**：`POST /api/power-control/restore/{roomId}`

- **功能描述**：恢复房间供电

- **需要认证**：是

### 2.10 收款管理

#### 2.10.1 获取房间收款记录

- **接口路径**：`GET /api/collections/room/{roomId}`

- **功能描述**：获取房间收款记录

- **需要认证**：是

#### 2.10.2 获取账单收款记录

- **接口路径**：`GET /api/collections/bill/{billId}`

- **功能描述**：获取账单收款记录

- **需要认证**：是

#### 2.10.3 清理过期记录

- **接口路径**：`DELETE /api/collections/cleanup`

- **功能描述**：清理过期收款记录

- **需要认证**：是

### 2.11 遥测数据

#### 2.11.1 获取遥测数据

- **接口路径**：`GET /api/telemetry`

- **功能描述**：获取指定设备的功率遥测数据，支持多种时间范围查询

- **需要认证**：是

- **查询参数**：
  - `device`：设备ID（必填）
  - `range`：时间范围，可选值：`60s`、`24h`、`7d`、`30d`（必填）

- **响应**：

```json
[
  {
    "ts": 1771650000,
    "power_w": 118.3
  }
]
```

#### 2.11.2 获取用电统计报表

- **接口路径**：`GET /api/telemetry/statistics`

- **功能描述**：生成指定设备的日/周/月/年用电统计报表

- **需要认证**：是

- **查询参数**：
  - `device`：设备ID（必填）
  - `period`：统计周期，可选值：`day`、`week`、`month`、`year`（必填）
  - `start`：开始时间戳（必填）
  - `end`：结束时间戳（必填）

#### 2.11.3 导出遥测数据

- **接口路径**：`GET /api/telemetry/export`

- **功能描述**：导出指定设备的遥测数据，支持CSV格式

- **需要认证**：是

- **查询参数**：
  - `device`：设备ID（必填）
  - `format`：导出格式，可选值：`csv`（必填）
  - `start`：开始时间戳（必填）
  - `end`：结束时间戳（必填）

### 2.12 设备历史

#### 2.12.1 获取设备历史

- **接口路径**：`GET /api/devices/{deviceId}/history`

- **功能描述**：获取设备历史记录

- **需要认证**：是

#### 2.12.2 获取历史详情

- **接口路径**：`GET /api/devices/history/{historyId}`

- **功能描述**：获取历史记录详情

- **需要认证**：是

### 2.13 设备控制

#### 2.13.1 下发控制命令

- **接口路径**：`POST /api/strips/{deviceId}/cmd`

- **功能描述**：向指定设备下发控制命令，支持开关操作和定时功能

- **需要认证**：是

- **路径参数**：
  - `deviceId`：设备ID

- **请求体（开关控制）**：

```json
{
  "socket": 1,
  "action": "on"
}
```

- **请求体（模式控制）**：

```json
{
  "action": "mode",
  "mode": "eco"
}
```

- **请求体（定时控制）**：

```json
{
  "action": "timer_off",
  "duration": "10m"
}
```

- **响应**：

```json
{
  "ok": true,
  "cmdId": "cmd_20260221_001",
  "stripId": "strip01",
  "acceptedAt": 1771650000
}
```

#### 2.13.2 批量下发命令

- **接口路径**：`POST /api/commands/batch`

- **功能描述**：向多个设备同时下发相同的控制命令

- **需要认证**：是

#### 2.13.3 获取设备命令历史

- **接口路径**：`GET /api/commands/device/{deviceId}`

- **功能描述**：获取指定设备的命令执行历史记录

- **需要认证**：是

#### 2.13.4 获取命令详情

- **接口路径**：`GET /api/commands/{cmdId}`

- **功能描述**：获取指定命令的详细信息

- **需要认证**：是

### 2.14 命令查询

#### 2.14.1 查询命令状态

- **接口路径**：`GET /api/cmd/{cmdId}`

- **功能描述**：查询命令的执行状态

- **需要认证**：是

- **路径参数**：
  - `cmdId`：命令ID

- **响应**：

```json
{
  "cmdId": "cmd_20260221_001",
  "state": "success",
  "updatedAt": 1771650003,
  "message": "",
  "durationMs": 320
}
```

### 2.15 AI分析报告

#### 2.15.1 获取AI分析报告

- **接口路径**：`GET /api/rooms/{roomId}/ai_report`

- **功能描述**：获取指定房间的智能用电分析报告，包含用电趋势、异常检测和节能建议

- **需要认证**：是

- **路径参数**：
  - `roomId`：房间ID

- **查询参数**：
  - `period`：时间范围，可选值：`7d`、`30d`（默认：`7d`）

- **响应**：

```json
{
  "room_id": "A-302",
  "period": "7d",
  "summary": "本周夜间待机功耗偏高...",
  "anomalies": ["周二 23:30 出现高功率..."],
  "suggestions": ["00:30 后低负载插孔自动断电"]
}
```

### 2.16 AI智能代理

#### 2.16.1 行为学习

- **接口路径**：`GET /api/agent/behavior/learn/{roomId}`

- **功能描述**：学习房间用电行为

- **需要认证**：是

#### 2.16.2 获取行为建议

- **接口路径**：`GET /api/agent/behavior/suggestions/{roomId}`

- **功能描述**：获取用电行为建议

- **需要认证**：是

#### 2.16.3 行为预测

- **接口路径**：`GET /api/agent/behavior/predict/{roomId}`

- **功能描述**：预测用电行为

- **需要认证**：是

#### 2.16.4 行为对比

- **接口路径**：`GET /api/agent/behavior/compare/{roomId}`

- **功能描述**：对比用电行为

- **需要认证**：是

#### 2.16.5 批量异常检测

- **接口路径**：`GET /api/agent/anomaly/detect/batch/{deviceId}`

- **功能描述**：批量检测异常

- **需要认证**：是

#### 2.16.6 故障预测

- **接口路径**：`GET /api/agent/anomaly/predict/{deviceId}`

- **功能描述**：预测设备故障

- **需要认证**：是

#### 2.16.7 设备健康度

- **接口路径**：`GET /api/agent/anomaly/health/{deviceId}`

- **功能描述**：获取设备健康度

- **需要认证**：是

#### 2.16.8 房间异常

- **接口路径**：`GET /api/agent/anomaly/room/{roomId}`

- **功能描述**：获取房间异常

- **需要认证**：是

#### 2.16.9 设备故障

- **接口路径**：`GET /api/agent/anomaly/failure/{deviceId}`

- **功能描述**：获取设备故障信息

- **需要认证**：是

#### 2.16.10 房间场景

- **接口路径**：`GET /api/agent/scene/room/{roomId}`

- **功能描述**：获取房间场景

- **需要认证**：是

#### 2.16.11 场景统计

- **接口路径**：`GET /api/agent/scene/statistics/{sceneId}`

- **功能描述**：获取场景统计

- **需要认证**：是

#### 2.16.12 场景历史

- **接口路径**：`GET /api/agent/scene/history/{sceneId}`

- **功能描述**：获取场景历史

- **需要认证**：是

#### 2.16.13 综合分析

- **接口路径**：`GET /api/agent/analyze/{roomId}`

- **功能描述**：综合分析房间

- **需要认证**：是

#### 2.16.14 生成设备数据

- **接口路径**：`POST /api/agent/data/generate/{deviceId}`

- **功能描述**：生成设备模拟数据

- **需要认证**：是

#### 2.16.15 生成房间数据

- **接口路径**：`POST /api/agent/data/generate-room/{roomId}`

- **功能描述**：生成房间模拟数据

- **需要认证**：是

#### 2.16.16 生成所有数据

- **接口路径**：`POST /api/agent/data/generate-all`

- **功能描述**：生成所有模拟数据

- **需要认证**：是

#### 2.16.17 实时异常检测

- **接口路径**：`POST /api/agent/anomaly/detect/realtime/{deviceId}`

- **功能描述**：实时检测异常

- **需要认证**：是

#### 2.16.18 自动生成场景

- **接口路径**：`POST /api/agent/scene/auto-generate/{roomId}`

- **功能描述**：自动生成场景

- **需要认证**：是

#### 2.16.19 执行场景

- **接口路径**：`POST /api/agent/scene/execute/{sceneId}`

- **功能描述**：执行场景

- **需要认证**：是

#### 2.16.20 优化场景

- **接口路径**：`POST /api/agent/scene/optimize/{sceneId}`

- **功能描述**：优化场景

- **需要认证**：是

#### 2.16.21 切换场景

- **接口路径**：`PUT /api/agent/scene/toggle/{sceneId}`

- **功能描述**：切换场景启用状态

- **需要认证**：是

#### 2.16.22 删除场景

- **接口路径**：`DELETE /api/agent/scene/{sceneId}`

- **功能描述**：删除场景

- **需要认证**：是

### 2.17 设备告警

#### 2.17.1 获取设备告警列表

- **接口路径**：`GET /api/alerts/device/{deviceId}`

- **功能描述**：获取指定设备的告警列表

- **需要认证**：是

- **路径参数**：
  - `deviceId`：设备ID

- **查询参数**：
  - `onlyUnresolved`：仅获取未解决的告警（默认：false）

#### 2.17.2 获取未解决告警

- **接口路径**：`GET /api/alerts/unresolved`

- **功能描述**：获取所有未解决的告警

- **需要认证**：是

#### 2.17.3 解决告警

- **接口路径**：`PUT /api/alerts/{alertId}/resolve`

- **功能描述**：标记告警为已解决

- **需要认证**：是

#### 2.17.4 获取设备告警配置

- **接口路径**：`GET /api/alerts/config/{deviceId}`

- **功能描述**：获取指定设备的告警配置

- **需要认证**：是

#### 2.17.5 更新告警配置

- **接口路径**：`PUT /api/alerts/config/{deviceId}`

- **功能描述**：更新设备的告警阈值配置

- **需要认证**：是

#### 2.17.6 模拟告警

- **接口路径**：`POST /api/alerts/simulate/{deviceId}`

- **功能描述**：模拟设备告警用于测试

- **需要认证**：是

### 2.18 通知管理

#### 2.18.1 获取未读通知

- **接口路径**：`GET /api/notifications/unread`

- **功能描述**：获取未读通知

- **需要认证**：是

#### 2.18.2 获取未读数量

- **接口路径**：`GET /api/notifications/unread/count`

- **功能描述**：获取未读通知数量

- **需要认证**：是

#### 2.18.3 获取通知统计

- **接口路径**：`GET /api/notifications/statistics`

- **功能描述**：获取通知统计

- **需要认证**：是

#### 2.18.4 获取通知偏好

- **接口路径**：`GET /api/notifications/preferences`

- **功能描述**：获取通知偏好

- **需要认证**：是

#### 2.18.5 获取已启用偏好

- **接口路径**：`GET /api/notifications/preferences/enabled`

- **功能描述**：获取已启用的通知偏好

- **需要认证**：是

#### 2.18.6 获取免打扰时间

- **接口路径**：`GET /api/notifications/preferences/quiet-hours`

- **功能描述**：获取免打扰时间

- **需要认证**：是

#### 2.18.7 发送测试邮件

- **接口路径**：`POST /api/notifications/email/test`

- **功能描述**：发送测试邮件

- **需要认证**：是

#### 2.18.8 重置通知偏好

- **接口路径**：`POST /api/notifications/preferences/reset`

- **功能描述**：重置通知偏好

- **需要认证**：是

#### 2.18.9 标记通知已读

- **接口路径**：`PUT /api/notifications/{id}/read`

- **功能描述**：标记通知为已读

- **需要认证**：是

#### 2.18.10 标记全部已读

- **接口路径**：`PUT /api/notifications/read-all`

- **功能描述**：标记所有通知为已读

- **需要认证**：是

#### 2.18.11 更新通知偏好

- **接口路径**：`PUT /api/notifications/preferences`

- **功能描述**：更新通知偏好

- **需要认证**：是

#### 2.18.12 删除通知

- **接口路径**：`DELETE /api/notifications/{id}`

- **功能描述**：删除通知

- **需要认证**：是

### 2.19 定时任务

#### 2.19.1 获取设备定时任务

- **接口路径**：`GET /api/tasks/device/{deviceId}`

- **功能描述**：获取设备定时任务

- **需要认证**：是

#### 2.19.2 更新定时任务

- **接口路径**：`PUT /api/tasks/{taskId}`

- **功能描述**：更新定时任务

- **需要认证**：是

#### 2.19.3 切换定时任务

- **接口路径**：`PUT /api/tasks/{taskId}/toggle`

- **功能描述**：切换定时任务启用状态

- **需要认证**：是

#### 2.19.4 删除定时任务

- **接口路径**：`DELETE /api/tasks/{taskId}`

- **功能描述**：删除定时任务

- **需要认证**：是

### 2.20 自动节能

#### 2.20.1 预测用电

- **接口路径**：`GET /api/saving/predict/{roomId}`

- **功能描述**：预测房间用电

- **需要认证**：是

#### 2.20.2 按小时预测

- **接口路径**：`GET /api/saving/predict/{roomId}/hourly`

- **功能描述**：按小时预测用电

- **需要认证**：是

#### 2.20.3 获取节能策略

- **接口路径**：`GET /api/saving/strategies/{roomId}`

- **功能描述**：获取节能策略

- **需要认证**：是

#### 2.20.4 获取自动节能状态

- **接口路径**：`GET /api/saving/auto/status`

- **功能描述**：获取自动节能状态

- **需要认证**：是

#### 2.20.5 获取房间节能统计

- **接口路径**：`GET /api/saving/stats/{roomId}`

- **功能描述**：获取房间节能统计

- **需要认证**：是

#### 2.20.6 获取全部节能统计

- **接口路径**：`GET /api/saving/stats/all`

- **功能描述**：获取全部节能统计

- **需要认证**：是

#### 2.20.7 获取节能报告

- **接口路径**：`GET /api/saving/report/{roomId}`

- **功能描述**：获取节能报告

- **需要认证**：是

#### 2.20.8 执行节能策略

- **接口路径**：`POST /api/saving/strategies/{roomId}/execute/{strategyId}`

- **功能描述**：执行节能策略

- **需要认证**：是

#### 2.20.9 切换自动节能

- **接口路径**：`POST /api/saving/auto/toggle`

- **功能描述**：切换自动节能

- **需要认证**：是

#### 2.20.10 设置阈值

- **接口路径**：`POST /api/saving/auto/threshold`

- **功能描述**：设置节能阈值

- **需要认证**：是

### 2.21 权限管理

#### 2.21.1 获取角色列表

- **接口路径**：`GET /api/rbac/roles`

- **功能描述**：获取所有角色

- **需要认证**：是

#### 2.21.2 获取已启用角色

- **接口路径**：`GET /api/rbac/roles/enabled`

- **功能描述**：获取已启用角色

- **需要认证**：是

#### 2.21.3 获取角色详情

- **接口路径**：`GET /api/rbac/roles/{roleId}`

- **功能描述**：获取角色详情

- **需要认证**：是

#### 2.21.4 获取角色权限

- **接口路径**：`GET /api/rbac/roles/{roleId}/permissions`

- **功能描述**：获取角色权限

- **需要认证**：是

#### 2.21.5 获取角色用户数

- **接口路径**：`GET /api/rbac/roles/{roleId}/user-count`

- **功能描述**：获取角色用户数

- **需要认证**：是

#### 2.21.6 获取权限列表

- **接口路径**：`GET /api/rbac/permissions`

- **功能描述**：获取所有权限

- **需要认证**：是

#### 2.21.7 获取权限详情

- **接口路径**：`GET /api/rbac/permissions/{permissionId}`

- **功能描述**：获取权限详情

- **需要认证**：是

#### 2.21.8 获取资源列表

- **接口路径**：`GET /api/rbac/resources`

- **功能描述**：获取所有资源

- **需要认证**：是

#### 2.21.9 获取资源树

- **接口路径**：`GET /api/rbac/resources/tree`

- **功能描述**：获取资源树

- **需要认证**：是

#### 2.21.10 获取资源详情

- **接口路径**：`GET /api/rbac/resources/{resourceId}`

- **功能描述**：获取资源详情

- **需要认证**：是

#### 2.21.11 获取用户角色

- **接口路径**：`GET /api/rbac/users/{username}/roles`

- **功能描述**：获取用户角色

- **需要认证**：是

#### 2.21.12 获取用户权限

- **接口路径**：`GET /api/rbac/users/{username}/permissions`

- **功能描述**：获取用户权限

- **需要认证**：是

#### 2.21.13 检查权限

- **接口路径**：`GET /api/rbac/users/{username}/has-permission`

- **功能描述**：检查用户权限

- **需要认证**：是

#### 2.21.14 检查角色

- **接口路径**：`GET /api/rbac/users/{username}/has-role`

- **功能描述**：检查用户角色

- **需要认证**：是

#### 2.21.15 创建角色

- **接口路径**：`POST /api/rbac/roles`

- **功能描述**：创建角色

- **需要认证**：是

#### 2.21.16 分配角色权限

- **接口路径**：`POST /api/rbac/roles/{roleId}/permissions`

- **功能描述**：分配角色权限

- **需要认证**：是

#### 2.21.17 创建权限

- **接口路径**：`POST /api/rbac/permissions`

- **功能描述**：创建权限

- **需要认证**：是

#### 2.21.18 创建资源

- **接口路径**：`POST /api/rbac/resources`

- **功能描述**：创建资源

- **需要认证**：是

#### 2.21.19 分配用户角色

- **接口路径**：`POST /api/rbac/users/{username}/roles`

- **功能描述**：分配用户角色

- **需要认证**：是

#### 2.21.20 初始化RBAC

- **接口路径**：`POST /api/rbac/init`

- **功能描述**：初始化RBAC数据

- **需要认证**：是

#### 2.21.21 更新角色

- **接口路径**：`PUT /api/rbac/roles/{roleId}`

- **功能描述**：更新角色

- **需要认证**：是

#### 2.21.22 更新权限

- **接口路径**：`PUT /api/rbac/permissions/{permissionId}`

- **功能描述**：更新权限

- **需要认证**：是

#### 2.21.23 更新资源

- **接口路径**：`PUT /api/rbac/resources/{resourceId}`

- **功能描述**：更新资源

- **需要认证**：是

#### 2.21.24 更新用户角色

- **接口路径**：`PUT /api/rbac/users/{username}/roles`

- **功能描述**：更新用户角色

- **需要认证**：是

#### 2.21.25 删除角色

- **接口路径**：`DELETE /api/rbac/roles/{roleId}`

- **功能描述**：删除角色

- **需要认证**：是

#### 2.21.26 删除权限

- **接口路径**：`DELETE /api/rbac/permissions/{permissionId}`

- **功能描述**：删除权限

- **需要认证**：是

#### 2.21.27 删除资源

- **接口路径**：`DELETE /api/rbac/resources/{resourceId}`

- **功能描述**：删除资源

- **需要认证**：是

#### 2.21.28 删除用户角色

- **接口路径**：`DELETE /api/rbac/users/{username}/roles`

- **功能描述**：删除用户角色

- **需要认证**：是

### 2.22 IP访问控制

#### 2.22.1 获取白名单

- **接口路径**：`GET /api/ip-control/whitelist`

- **功能描述**：获取IP白名单

- **需要认证**：是

#### 2.22.2 获取黑名单

- **接口路径**：`GET /api/ip-control/blacklist`

- **功能描述**：获取IP黑名单

- **需要认证**：是

#### 2.22.3 获取活动IP

- **接口路径**：`GET /api/ip-control/active`

- **功能描述**：获取活动IP

- **需要认证**：是

#### 2.22.4 检查IP

- **接口路径**：`GET /api/ip-control/check/{ipAddress}`

- **功能描述**：检查IP访问权限

- **需要认证**：是

#### 2.22.5 检查IP是否被阻止

- **接口路径**：`GET /api/ip-control/blocked/{ipAddress}`

- **功能描述**：检查IP是否被阻止

- **需要认证**：是

#### 2.22.6 检查IP是否在白名单

- **接口路径**：`GET /api/ip-control/whitelisted/{ipAddress}`

- **功能描述**：检查IP是否在白名单

- **需要认证**：是

#### 2.22.7 获取IP详情

- **接口路径**：`GET /api/ip-control/{ipAddress}`

- **功能描述**：获取IP详情

- **需要认证**：是

#### 2.22.8 添加白名单

- **接口路径**：`POST /api/ip-control/whitelist`

- **功能描述**：添加IP到白名单

- **需要认证**：是

#### 2.22.9 添加黑名单

- **接口路径**：`POST /api/ip-control/blacklist`

- **功能描述**：添加IP到黑名单

- **需要认证**：是

#### 2.22.10 更新IP

- **接口路径**：`PUT /api/ip-control/{ipAddress}`

- **功能描述**：更新IP信息

- **需要认证**：是

#### 2.22.11 删除IP

- **接口路径**：`DELETE /api/ip-control/{ipAddress}`

- **功能描述**：删除IP

- **需要认证**：是

#### 2.22.12 清理过期IP

- **接口路径**：`DELETE /api/ip-control/cleanup`

- **功能描述**：清理过期IP记录

- **需要认证**：是

### 2.23 登录日志

#### 2.23.1 获取用户登录日志

- **接口路径**：`GET /api/login-logs/user/{username}`

- **功能描述**：获取用户登录日志

- **需要认证**：是

#### 2.23.2 按时间范围获取

- **接口路径**：`GET /api/login-logs/range`

- **功能描述**：按时间范围获取登录日志

- **需要认证**：是

#### 2.23.3 获取统计信息

- **接口路径**：`GET /api/login-logs/statistics`

- **功能描述**：获取登录统计

- **需要认证**：是

#### 2.23.4 检查用户是否被锁定

- **接口路径**：`GET /api/login-logs/locked/{username}`

- **功能描述**：检查用户是否被锁定

- **需要认证**：是

#### 2.23.5 检查IP是否被阻止

- **接口路径**：`GET /api/login-logs/blocked-ip/{ipAddress}`

- **功能描述**：检查IP是否被阻止

- **需要认证**：是

#### 2.23.6 获取用户最后登录

- **接口路径**：`GET /api/login-logs/last-login/{username}`

- **功能描述**：获取用户最后登录

- **需要认证**：是

#### 2.23.7 获取活跃会话

- **接口路径**：`GET /api/login-logs/active-sessions`

- **功能描述**：获取活跃会话

- **需要认证**：是

#### 2.23.8 清理过期日志

- **接口路径**：`DELETE /api/login-logs/cleanup`

- **功能描述**：清理过期登录日志

- **需要认证**：是

### 2.24 审计日志

#### 2.24.1 获取用户审计日志

- **接口路径**：`GET /api/audit-logs/user/{username}`

- **功能描述**：获取用户审计日志

- **需要认证**：是

#### 2.24.2 按模块获取

- **接口路径**：`GET /api/audit-logs/module/{module}`

- **功能描述**：按模块获取审计日志

- **需要认证**：是

#### 2.24.3 按时间范围获取

- **接口路径**：`GET /api/audit-logs/range`

- **功能描述**：按时间范围获取审计日志

- **需要认证**：是

#### 2.24.4 按用户和时间范围获取

- **接口路径**：`GET /api/audit-logs/user/{username}/range`

- **功能描述**：按用户和时间范围获取审计日志

- **需要认证**：是

#### 2.24.5 获取统计信息

- **接口路径**：`GET /api/audit-logs/statistics`

- **功能描述**：获取审计统计

- **需要认证**：是

#### 2.24.6 清理过期日志

- **接口路径**：`DELETE /api/audit-logs/cleanup`

- **功能描述**：清理过期审计日志

- **需要认证**：是

### 2.25 消息模板

#### 2.25.1 按代码获取模板

- **接口路径**：`GET /api/message-templates/code/{templateCode}`

- **功能描述**：按代码获取模板

- **需要认证**：是

#### 2.25.2 按类型获取模板

- **接口路径**：`GET /api/message-templates/type/{type}`

- **功能描述**：按类型获取模板

- **需要认证**：是

#### 2.25.3 按渠道获取模板

- **接口路径**：`GET /api/message-templates/channel/{channel}`

- **功能描述**：按渠道获取模板

- **需要认证**：是

#### 2.25.4 获取已启用模板

- **接口路径**：`GET /api/message-templates/enabled`

- **功能描述**：获取已启用模板

- **需要认证**：是

#### 2.25.5 渲染模板

- **接口路径**：`POST /api/message-templates/render/{templateCode}`

- **功能描述**：渲染消息模板

- **需要认证**：是

#### 2.25.6 提取变量

- **接口路径**：`POST /api/message-templates/extract-variables`

- **功能描述**：提取模板变量

- **需要认证**：是

#### 2.25.7 初始化模板

- **接口路径**：`POST /api/message-templates/init`

- **功能描述**：初始化消息模板

- **需要认证**：是

#### 2.25.8 更新模板

- **接口路径**：`PUT /api/message-templates/{id}`

- **功能描述**：更新消息模板

- **需要认证**：是

#### 2.25.9 删除模板

- **接口路径**：`DELETE /api/message-templates/{id}`

- **功能描述**：删除消息模板

- **需要认证**：是

### 2.26 数据字典

#### 2.26.1 获取字典类型

- **接口路径**：`GET /api/dict/types`

- **功能描述**：获取字典类型

- **需要认证**：是

#### 2.26.2 按类型获取字典

- **接口路径**：`GET /api/dict/type/{dictType}`

- **功能描述**：按类型获取字典

- **需要认证**：是

#### 2.26.3 获取字典树

- **接口路径**：`GET /api/dict/tree/{dictType}`

- **功能描述**：获取字典树

- **需要认证**：是

#### 2.26.4 分页获取字典

- **接口路径**：`GET /api/dict/page`

- **功能描述**：分页获取字典

- **需要认证**：是

#### 2.26.5 按代码获取字典

- **接口路径**：`GET /api/dict/code/{dictCode}`

- **功能描述**：按代码获取字典

- **需要认证**：是

#### 2.26.6 获取字典标签

- **接口路径**：`GET /api/dict/label`

- **功能描述**：获取字典标签

- **需要认证**：是

#### 2.26.7 批量操作

- **接口路径**：`POST /api/dict/batch`

- **功能描述**：批量操作字典

- **需要认证**：是

#### 2.26.8 初始化字典

- **接口路径**：`POST /api/dict/init`

- **功能描述**：初始化字典数据

- **需要认证**：是

#### 2.26.9 更新字典

- **接口路径**：`PUT /api/dict/{id}`

- **功能描述**：更新字典

- **需要认证**：是

#### 2.26.10 删除字典

- **接口路径**：`DELETE /api/dict/{id}`

- **功能描述**：删除字典

- **需要认证**：是

### 2.27 数据导入

#### 2.27.1 下载导入模板

- **接口路径**：`GET /api/import/template/{type}`

- **功能描述**：下载导入模板

- **需要认证**：是

#### 2.27.2 导入学生

- **接口路径**：`POST /api/import/students`

- **功能描述**：导入学生数据

- **需要认证**：是

#### 2.27.3 导入房间

- **接口路径**：`POST /api/import/rooms`

- **功能描述**：导入房间数据

- **需要认证**：是

#### 2.27.4 导入设备

- **接口路径**：`POST /api/import/devices`

- **功能描述**：导入设备数据

- **需要认证**：是

#### 2.27.5 导入JSON

- **接口路径**：`POST /api/import/json`

- **功能描述**：导入JSON数据

- **需要认证**：是

### 2.28 系统配置

#### 2.28.1 按分类获取配置

- **接口路径**：`GET /api/admin/config/category/{category}`

- **功能描述**：按分类获取配置

- **需要认证**：是

#### 2.28.2 获取配置

- **接口路径**：`GET /api/admin/config/{key}`

- **功能描述**：获取配置项

- **需要认证**：是

#### 2.28.3 初始化配置

- **接口路径**：`POST /api/admin/config/init`

- **功能描述**：初始化配置

- **需要认证**：是

#### 2.28.4 更新配置

- **接口路径**：`PUT /api/admin/config/{key}`

- **功能描述**：更新配置项

- **需要认证**：是

#### 2.28.5 批量更新配置

- **接口路径**：`PUT /api/admin/config/batch`

- **功能描述**：批量更新配置

- **需要认证**：是

### 2.29 用户管理

#### 2.29.1 获取用户详情

- **接口路径**：`GET /api/users/{username}`

- **功能描述**：获取用户详情

- **需要认证**：是

#### 2.29.2 修改密码

- **接口路径**：`POST /api/users/{username}/password`

- **功能描述**：修改用户密码

- **需要认证**：是

#### 2.29.3 更新用户

- **接口路径**：`PUT /api/users/{username}`

- **功能描述**：更新用户信息

- **需要认证**：是

#### 2.29.4 删除用户

- **接口路径**：`DELETE /api/users/{username}`

- **功能描述**：删除用户

- **需要认证**：是

#### 2.29.5 更新用户资料

- **接口路径**：`PATCH /api/users/{username}/profile`

- **功能描述**：更新用户资料

- **需要认证**：是

### 2.30 系统日志

#### 2.30.1 按级别获取日志

- **接口路径**：`GET /api/admin/logs/level/{level}`

- **功能描述**：按日志级别获取

- **需要认证**：是

#### 2.30.2 按类型获取日志

- **接口路径**：`GET /api/admin/logs/type/{type}`

- **功能描述**：按日志类型获取

- **需要认证**：是

#### 2.30.3 按用户获取日志

- **接口路径**：`GET /api/admin/logs/user/{username}`

- **功能描述**：按用户获取日志

- **需要认证**：是

#### 2.30.4 搜索日志

- **接口路径**：`GET /api/admin/logs/search`

- **功能描述**：搜索系统日志

- **需要认证**：是

#### 2.30.5 获取日志统计

- **接口路径**：`GET /api/admin/logs/statistics`

- **功能描述**：获取日志统计

- **需要认证**：是

#### 2.30.6 清理过期日志

- **接口路径**：`DELETE /api/admin/logs/cleanup`

- **功能描述**：清理过期系统日志

- **需要认证**：是

### 2.31 数据备份

#### 2.31.1 获取最近备份

- **接口路径**：`GET /api/admin/backup/recent`

- **功能描述**：获取最近备份

- **需要认证**：是

#### 2.31.2 获取备份统计

- **接口路径**：`GET /api/admin/backup/statistics`

- **功能描述**：获取备份统计

- **需要认证**：是

#### 2.31.3 数据库备份

- **接口路径**：`POST /api/admin/backup/database`

- **功能描述**：数据库备份

- **需要认证**：是

#### 2.31.4 导出数据

- **接口路径**：`POST /api/admin/backup/export`

- **功能描述**：导出数据

- **需要认证**：是

#### 2.31.5 删除备份

- **接口路径**：`DELETE /api/admin/backup/{id}`

- **功能描述**：删除备份

- **需要认证**：是

#### 2.31.6 清理过期备份

- **接口路径**：`DELETE /api/admin/backup/cleanup`

- **功能描述**：清理过期备份

- **需要认证**：是

### 2.32 系统监控

#### 2.32.1 获取系统状态

- **接口路径**：`GET /api/admin/monitor/system`

- **功能描述**：获取系统运行状态和资源使用情况

- **需要认证**：是

#### 2.32.2 获取设备状态

- **接口路径**：`GET /api/admin/monitor/devices`

- **功能描述**：获取设备在线状态统计

- **需要认证**：是

#### 2.32.3 获取API性能统计

- **接口路径**：`GET /api/admin/monitor/api-performance`

- **功能描述**：获取API响应时间等性能指标

- **需要认证**：是

- **查询参数**：
  - `hours`：统计小时数（默认：24）

#### 2.32.4 获取历史指标

- **接口路径**：`GET /api/admin/monitor/metrics`

- **功能描述**：获取指定类型的历史监控指标

- **需要认证**：是

- **查询参数**：
  - `type`：指标类型（必填）
  - `hours`：小时数（默认：24）

#### 2.32.5 手动收集指标

- **接口路径**：`POST /api/admin/monitor/collect`

- **功能描述**：手动触发系统指标收集

- **需要认证**：是

#### 2.32.6 清理过期指标

- **接口路径**：`DELETE /api/admin/monitor/cleanup`

- **功能描述**：清理指定天数前的监控指标

- **需要认证**：是

- **查询参数**：
  - `retentionDays`：保留天数（默认：7）

### 2.33 MQTT模拟器

#### 2.33.1 启动MQTT模拟器

- **接口路径**：`POST /api/simulator/start`

- **功能描述**：启动MQTT设备模拟器，模拟多个设备发送消息

- **需要认证**：是

- **请求体**：

```json
{
  "deviceNamePrefix": "sim_simulator_",
  "devices": 10,
  "duration": 300,
  "interval": 1.0,
  "minPower": 50.0,
  "maxPower": 200.0,
  "messageType": "MIXED",
  "enableDetailedMonitoring": false,
  "onlineRate": 0.95,
  "roomStart": 301,
  "roomEnd": 310,
  "enableHeartbeat": true,
  "heartbeatInterval": 30,
  "brokerUrl": "tcp://localhost:1883",
  "username": "",
  "password": "",
  "topicPrefix": "dorm",
  "minVoltage": 210.0,
  "maxVoltage": 230.0
}
```

- **响应**：

```json
{
  "taskId": "task_1234567890",
  "status": "RUNNING",
  "message": "MQTT模拟器已启动，正在模拟10个设备"
}
```

#### 2.33.2 停止MQTT模拟器

- **接口路径**：`POST /api/simulator/stop/{taskId}`

- **功能描述**：停止指定的MQTT设备模拟器任务

- **需要认证**：是

- **路径参数**：
  - `taskId`：任务ID

- **响应**：

```json
{
  "taskId": "task_1234567890",
  "status": "STOPPED",
  "message": "MQTT模拟器已停止"
}
```

#### 2.33.3 获取模拟器状态

- **接口路径**：`GET /api/simulator/status/{taskId}`

- **功能描述**：获取指定MQTT设备模拟器任务的状态

- **需要认证**：是

- **路径参数**：
  - `taskId`：任务ID

- **响应**：

```json
{
  "taskId": "task_1234567890",
  "status": "RUNNING",
  "devices": 10,
  "duration": 300,
  "interval": 1.0,
  "totalMessages": 1500,
  "errorMessages": 5,
  "successMessages": 1495,
  "messageType": "MIXED",
  "successRate": 0.997,
  "avgSendInterval": 1.02,
  "maxSendInterval": 1.5,
  "minSendInterval": 0.8,
  "runtime": 150,
  "startTime": 1234567890000,
  "endTime": 0,
  "onlineRate": 0.95,
  "avgPower": 125.5,
  "maxPower": 198.3,
  "minPower": 52.1,
  "cpuUsage": 15.2,
  "memoryUsage": 52428800,
  "message": "模拟器运行正常",
  "lastUpdateTime": 1234568040000,
  "enableDetailedMonitoring": false,
  "devicesPerCycle": 10,
  "monitoringMode": "SUMMARY",
  "recommendedPollingIntervalMs": 5000,
  "summaryOnly": true
}
```

#### 2.33.4 获取所有模拟器任务

- **接口路径**：`GET /api/simulator/tasks`

- **功能描述**：获取所有正在运行的MQTT设备模拟器任务

- **需要认证**：是

- **响应**：

```json
[
  {
    "taskId": "task_1234567890",
    "status": "RUNNING",
    "devices": 10,
    "duration": 300,
    "totalMessages": 1500,
    "successRate": 0.997
  }
]
```

## 3. WebSocket接口

### 3.1 WebSocket连接

- **接口路径**：`WS /ws`

- **功能描述**：实时推送设备状态、遥测数据和命令执行结果

### 3.2 事件类型

#### 3.2.1 设备状态更新

```json
{
  "type": "DEVICE_STATUS",
  "deviceId": "A-303 strip01",
  "payload": { "...": "..." }
}
```

#### 3.2.2 遥测更新

```json
{
  "type": "TELEMETRY",
  "deviceId": "A-303 strip01",
  "payload": { "...": "..." }
}
```

#### 3.2.3 命令ACK

```json
{
  "type": "CMD_ACK",
  "cmdId": "cmd_1772000000_ab12cd34",
  "state": "success",
  "ts": 1772000001,
  "updatedAt": 1772000001,
  "message": "",
  "durationMs": 320
}
```

#### 3.2.4 设备离线

```json
{
  "type": "DEVICE_OFFLINE",
  "deviceId": "A-303 strip01",
  "payload": {
    "reason": "设备断电"
  }
}
```

## 4. 数据模型

### 4.1 Device（设备）

```json
{
  "id": "strip01",
  "name": "宿舍302-插排01",
  "room": "A-302",
  "online": true,
  "lastSeen": "2026-02-21T10:20:30.000Z"
}
```

### 4.2 StripStatus（设备状态）

```json
{
  "ts": 1771650000,
  "online": true,
  "total_power_w": 132.5,
  "voltage_v": 221.3,
  "current_a": 0.62,
  "sockets": [
    {"id": 1, "on": true, "power_w": 92.1, "device": "PC"},
    {"id": 2, "on": false, "power_w": 0.0, "device": "None"}
  ]
}
```

### 4.3 TelemetryPoint（遥测数据点）

```json
{
  "ts": 1771650000,
  "power_w": 118.3
}
```

### 4.4 CommandRequest（命令请求）

```json
{
  "socket": 1,
  "action": "on",
  "mode": null,
  "duration": null,
  "payload": {}
}
```

### 4.5 CommandSubmitOut（命令提交响应）

```json
{
  "ok": true,
  "cmdId": "cmd_20260221_001",
  "stripId": "strip01",
  "acceptedAt": 1771650000
}
```

### 4.6 CommandStateOut（命令状态响应）

```json
{
  "cmdId": "cmd_20260221_001",
  "state": "success",
  "updatedAt": 1771650003,
  "message": "",
  "durationMs": 320
}
```

### 4.7 AIReportOut（AI分析报告）

```json
{
  "room_id": "A-302",
  "period": "7d",
  "summary": "本周夜间待机功耗偏高...",
  "anomalies": ["周二 23:30 出现高功率..."],
  "suggestions": ["00:30 后低负载插孔自动断电"]
}
```

## 5. 接口统计

### 5.1 接口总数

| 统计项 | 数量 |
|--------|------|
| 控制器数量 | 34个 |
| API端点总数 | 251个 |
| GET请求 | 145个 |
| POST请求 | 72个 |
| PUT请求 | 26个 |
| DELETE请求 | 9个 |

### 5.2 控制器列表

1. HealthController - 健康检查
2. AuthController - 认证管理
3. DeviceController - 设备管理
4. DeviceGroupController - 设备分组
5. DeviceFirmwareController - 设备固件
6. DormRoomController - 宿舍管理
7. StudentController - 学生管理
8. BillingController - 计费管理
9. PowerControlController - 电源控制
10. CollectionController - 收款管理
11. TelemetryController - 遥测数据
12. DeviceStatusHistoryController - 设备历史
13. CommandController - 设备控制
14. CommandQueryController - 命令查询
15. AiReportController - AI分析报告
16. AgentController - AI智能代理
17. AlertController - 设备告警
18. NotificationController - 通知管理
19. ScheduledTaskController - 定时任务
20. AutoSavingController - 自动节能
21. RbacController - 权限管理
22. IpAccessControlController - IP访问控制
23. LoginLogController - 登录日志
24. AuditLogController - 审计日志
25. MessageTemplateController - 消息模板
26. DataDictController - 数据字典
27. ImportController - 数据导入
28. SystemConfigController - 系统配置
29. UserController - 用户管理
30. SystemLogController - 系统日志
31. DataBackupController - 数据备份
32. MonitoringController - 系统监控
33. MqttSimulatorController - MQTT模拟器

## 6. 接口使用示例

### 6.1 获取设备列表

**请求**：
```
GET /api/devices
Authorization: Bearer {token}
```

**响应**：
```json
[
  {
    "id": "strip01",
    "name": "宿舍302-插排01",
    "room": "A-302",
    "online": true,
    "lastSeen": "2026-02-21T10:20:30.000Z"
  }
]
```

### 6.2 获取设备状态

**请求**：
```
GET /api/devices/strip01/status
Authorization: Bearer {token}
```

**响应**：
```json
{
  "ts": 1771650000,
  "online": true,
  "total_power_w": 132.5,
  "voltage_v": 221.3,
  "current_a": 0.62,
  "sockets": [
    {"id": 1, "on": true, "power_w": 92.1, "device": "PC"},
    {"id": 2, "on": false, "power_w": 0.0, "device": "None"}
  ]
}
```

### 6.3 下发控制命令

**请求**：
```
POST /api/strips/strip01/cmd
Authorization: Bearer {token}
Content-Type: application/json

{
  "socket": 1,
  "action": "on"
}
```

**响应**：
```json
{
  "ok": true,
  "cmdId": "cmd_20260221_001",
  "stripId": "strip01",
  "acceptedAt": 1771650000
}
```

---

## 更新说明

本文档最后更新时间：2026年3月14日

文档包含了宿舍电源管理系统的全部API接口，覆盖：
- 34个控制器
- 251个API端点
- 完整的数据模型定义
- WebSocket实时通信接口
- 详细的接口使用示例

### 本次更新内容（2026年3月14日）

1. **新增接口**
   - 新增批量删除设备接口（DELETE /api/devices/batch）
   - 新增MQTT模拟器模块（4个接口）
     - 启动MQTT模拟器（POST /api/simulator/start）
     - 停止MQTT模拟器（POST /api/simulator/stop/{taskId}）
     - 获取模拟器状态（GET /api/simulator/status/{taskId}）
     - 获取所有模拟器任务（GET /api/simulator/tasks）

2. **接口统计更新**
   - 控制器数量：38个 → 34个（修正统计错误）
   - API端点总数：247个 → 251个
   - GET请求：143个 → 145个
   - POST请求：70个 → 72个
   - DELETE请求：8个 → 9个

3. **文档优化**
   - 所有接口文档与后端代码保持同步
   - 添加完整的请求和响应示例
   - 完善接口参数说明
