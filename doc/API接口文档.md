# 宿舍电源管理系统API接口文档

## 1. 接口概述

本API接口文档描述了宿舍电源管理系统的后端API接口，包括设备管理、状态监控、命令控制、遥测数据采集和AI分析等功能。接口采用RESTful设计风格，使用HTTP/HTTPS协议进行通信，数据格式为JSON。

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
| 404 | 设备/房间不存在 |
| 409 | 命令冲突 |
| 500 | 服务错误 |

## 2. 接口列表

### 2.1 健康检查

- **接口路径**：`GET /health`

- **功能描述**：检查系统健康状态

- **响应**：

```json
{
  "ok": true,
  "mqtt_enabled": true,
  "mqtt_connected": true,
  "database_url": "postgresql+psycopg://..."
}
```

### 2.2 认证

#### 2.2.1 登录

- **接口路径**：`POST /api/auth/login`

- **功能描述**：管理员登录

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

### 2.3 设备管理

#### 2.3.1 设备列表

- **接口路径**：`GET /api/devices`

- **功能描述**：获取所有设备列表

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

#### 2.3.2 设备状态

- **接口路径**：`GET /api/devices/{device_id}/status`

- **功能描述**：获取单个设备的详细状态

- **路径参数**：
  - `device_id`：设备ID

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

### 2.4 遥测数据

- **接口路径**：`GET /api/telemetry`

- **功能描述**：获取设备的历史用电数据

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

### 2.5 命令控制

#### 2.5.1 下发命令

- **接口路径**：`POST /api/strips/{device_id}/cmd`

- **功能描述**：向设备下发控制命令

- **路径参数**：
  - `device_id`：设备ID

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

#### 2.5.2 查询命令状态

- **接口路径**：`GET /api/cmd/{cmd_id}`

- **功能描述**：查询命令的执行状态

- **路径参数**：
  - `cmd_id`：命令ID

- **响应**：

```json
{
  "cmdId": "cmd_20260221_001",
  "state": "pending",
  "updatedAt": 1771650003,
  "message": "waiting device ack"
}
```

### 2.6 AI分析

- **接口路径**：`GET /api/rooms/{room_id}/ai_report`

- **功能描述**：获取房间的AI分析报告

- **路径参数**：
  - `room_id`：房间ID

- **查询参数**：
  - `period`：分析周期，可选值：`7d`、`30d`（默认：`7d`）

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

### 2.7 WebSocket

- **接口路径**：`WS /ws`

- **功能描述**：实时推送设备状态、遥测数据和命令执行结果

- **事件类型**：

#### 2.7.1 设备状态更新

```json
{
  "type": "DEVICE_STATUS",
  "deviceId": "A-303 strip01",
  "payload": { "...": "..." }
}
```

#### 2.7.2 遥测更新

```json
{
  "type": "TELEMETRY",
  "deviceId": "A-303 strip01",
  "payload": { "...": "..." }
}
```

#### 2.7.3 命令ACK

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

#### 2.7.4 设备离线

```json
{
  "type": "DEVICE_OFFLINE",
  "deviceId": "A-303 strip01",
  "payload": {
    "reason": "设备断电"
  }
}
```

## 3. 数据模型

### 3.1 Device（设备）

```json
{
  "id": "strip01",
  "name": "宿舍302-插排01",
  "room": "A-302",
  "online": true,
  "lastSeen": "2026-02-21T10:20:30.000Z"
}
```

### 3.2 StripStatus（设备状态）

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

### 3.3 TelemetryPoint（遥测数据点）

```json
{
  "ts": 1771650000,
  "power_w": 118.3
}
```

### 3.4 CommandRequest（命令请求）

```json
{
  "socket": 1,
  "action": "on",
  "mode": null,
  "duration": null,
  "payload": {}
}
```

### 3.5 CommandSubmitOut（命令提交响应）

```json
{
  "ok": true,
  "cmdId": "cmd_20260221_001",
  "stripId": "strip01",
  "acceptedAt": 1771650000
}
```

### 3.6 CommandStateOut（命令状态响应）

```json
{
  "cmdId": "cmd_20260221_001",
  "state": "success",
  "updatedAt": 1771650003,
  "message": "",
  "durationMs": 320
}
```

### 3.7 AIReportOut（AI分析报告）

```json
{
  "room_id": "A-302",
  "period": "7d",
  "summary": "本周夜间待机功耗偏高...",
  "anomalies": ["周二 23:30 出现高功率..."],
  "suggestions": ["00:30 后低负载插孔自动断电"]
}
```

## 4. 接口使用示例

### 4.1 获取设备列表

**请求**：
```
GET /api/devices
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

### 4.2 获取设备状态

**请求**：
```
GET /api/devices/strip01/status
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

### 4.3 获取遥测数据

**请求**：
```
GET /api/telemetry?device=strip01&range=24h
```

**响应**：
```json
[
  {"ts": 1771646400, "power_w": 100.5},
  {"ts": 1771650000, "power_w": 118.3},
  {"ts": 1771653600, "power_w": 95.7}
]
```

### 4.4 下发控制命令

**请求**：
```
POST /api/strips/strip01/cmd
Content-Type: application/json

{
  "socket": 1,
  "action": "off"
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

### 4.5 查询命令状态

**请求**：
```
GET /api/cmd/cmd_20260221_001
```

**响应**：
```json
{
  "cmdId": "cmd_20260221_001",
  "state": "success",
  "updatedAt": 1771650003,
  "message": "",
  "durationMs": 320
}
```

### 4.6 获取AI分析报告

**请求**：
```
GET /api/rooms/A-302/ai_report?period=7d
```

**响应**：
```json
{
  "room_id": "A-302",
  "period": "7d",
  "summary": "本周平均功率为98.2W，峰值功率为186.0W。",
  "anomalies": ["周二 23:30 出现高功率使用，达到186.0W。"],
  "suggestions": ["00:30 后低负载插孔自动断电", "设置功率超过150W时的提醒"]
}
```

## 5. 接口验证

服务启动后，可通过以下步骤验证接口：

1. 访问 `http://127.0.0.1:8000/health` 检查系统健康状态
2. 访问 `http://127.0.0.1:8000/api/devices` 查看设备列表
3. 访问 `http://127.0.0.1:8000/api/devices/strip01/status` 查看设备状态
4. 访问 `http://127.0.0.1:8000/api/telemetry?device=strip01&range=24h` 查看遥测数据
5. 调用 `POST /api/strips/strip01/cmd` 下发控制命令
6. 访问 `http://127.0.0.1:8000/api/cmd/{cmdId}` 查看命令执行状态
7. 访问 `http://127.0.0.1:8000/api/rooms/A-302/ai_report?period=7d` 查看AI分析报告

## 6. 版本说明

- 文档版本：`v1.0`
- 对应后端：当前 `web/back_end/app/main.py` 实现
- 若后端新增接口或修改现有接口，需同步更新本文件

## 7. 总结

本API接口文档详细描述了宿舍电源管理系统的后端API接口，包括健康检查、认证、设备管理、遥测数据、命令控制、AI分析和WebSocket实时通信等功能。接口设计遵循RESTful风格，使用JSON格式进行数据交换，提供了完整的设备监控和控制能力。

前端应用可以通过这些接口实现设备状态的实时监控、远程控制、用电数据的历史查询和分析，以及获取AI分析报告等功能。WebSocket接口的实现使得前端可以实时接收设备状态变化和命令执行结果，提升了用户体验。

该API接口文档为前后端联调提供了明确的契约，确保了系统的一致性和可靠性。