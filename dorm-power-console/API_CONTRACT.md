# Dorm Power Console 正式接口文档（去随机化）

本文档用于替换当前 Mock/随机接口，作为前后端联调契约。

- 适用前端：`web/front/dorm-power-console`
- 目标：所有页面数据由后端真实数据提供，不再随机生成
- 协议：HTTP + JSON（可扩展 WebSocket/SSE）

---

## 1. 通用约定

### 1.1 Base URL

- 开发：`http://<host>:3000/api`（或反向代理后的 `/api`）
- 生产：`https://<domain>/api`

### 1.2 时间格式

- 时间戳统一使用 `Unix 秒`（number）
- 可读时间由前端格式化

### 1.3 通用错误结构

```json
{
  "ok": false,
  "code": "BAD_REQUEST",
  "message": "range is invalid",
  "details": {}
}
```

推荐状态码：
- `200` 成功
- `400` 参数错误
- `404` 设备/房间不存在
- `409` 命令冲突
- `500` 服务错误

---

## 2. 数据模型

### 2.1 Device

```json
{
  "id": "strip01",
  "name": "宿舍302-插排01",
  "room": "A-302",
  "online": true,
  "lastSeen": "2026-02-21T10:20:30.000Z"
}
```

### 2.2 StripStatus

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

### 2.3 TelemetryPoint

```json
{
  "ts": 1771650000,
  "power_w": 118.3
}
```

---

## 3. 接口清单

## 3.1 设备列表

- `GET /api/devices`

响应：`Device[]`

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

前端使用页面：
- `/devices`

---

## 3.2 设备实时状态

- `GET /api/devices/{id}/status`

路径参数：
- `id`：设备 ID

响应：`StripStatus`

前端使用页面：
- `/dashboard`
- `/live`
- `/devices/[id]`

---

## 3.3 历史/实时趋势数据

- `GET /api/telemetry?device={id}&range={range}`

查询参数：
- `device`：设备 ID（必填）
- `range`：`60s | 24h | 7d | 30d`（必填）

响应：`TelemetryPoint[]`

### 数据采样建议（后端必须稳定输出）

- `60s`：60 点，1 秒间隔
- `24h`：96 点，15 分钟间隔
- `7d`：168 点，1 小时间隔
- `30d`：120 点，6 小时间隔

> 注意：后端返回必须按时间升序；不要随机抖动；允许真实波动。

前端使用页面：
- `/dashboard`
- `/live`
- `/history`
- `/devices/[id]`

---

## 3.4 控制命令下发

- `POST /api/strips/{id}/cmd`

路径参数：
- `id`：设备 ID

请求体（示例）：

```json
{
  "socket": 1,
  "action": "on"
}
```

或模式/定时类命令：

```json
{
  "action": "mode",
  "mode": "eco"
}
```

```json
{
  "action": "timer_off",
  "duration": "10m"
}
```

响应（建议返回 cmdId）：

```json
{
  "ok": true,
  "cmdId": "cmd_20260221_001",
  "stripId": "strip01",
  "acceptedAt": 1771650000
}
```

前端使用页面：
- `/live`

---

## 3.5 AI 周报

- `GET /api/rooms/{room_id}/ai_report?period=7d`

路径参数：
- `room_id`：房间 ID

查询参数：
- `period`：`7d | 30d`（默认 `7d`）

响应：

```json
{
  "room_id": "A-302",
  "period": "7d",
  "summary": "本周夜间待机功耗偏高...",
  "anomalies": ["周二 23:30 出现高功率..."],
  "suggestions": ["00:30 后低负载插孔自动断电"]
}
```

前端使用页面：
- `/ai`

---

## 4. 命令回执（建议增强）

当前前端已实现状态机：`sending -> pending -> success/failed/timeout`。

为实现真实回执，建议增加以下接口之一：

### 4.1 轮询式

- `GET /api/cmd/{cmdId}`

响应：

```json
{
  "cmdId": "cmd_20260221_001",
  "state": "pending",
  "updatedAt": 1771650003,
  "message": "waiting device ack"
}
```

### 4.2 推送式（推荐）

- WebSocket 或 SSE 推送 `CMD_ACK`

```json
{
  "type": "CMD_ACK",
  "cmdId": "cmd_20260221_001",
  "state": "success",
  "ts": 1771650004
}
```

---

## 5. 去随机化改造要求（后端侧）

必须满足：

1. `status` 和 `telemetry` 基于真实设备/数据库，不再随机计算
2. `telemetry` 时间点稳定、等间隔（按 range 采样规则）
3. 设备离线时：
   - `online=false`
   - `lastSeen` 为真实最近上报时间
   - `telemetry` 可返回空数组或最近缓存（需统一策略）
4. 命令接口返回可追踪 `cmdId`

---

## 6. 与当前代码映射

当前项目内 Mock 路由文件：

- `src/app/api/devices/route.ts`
- `src/app/api/devices/[id]/status/route.ts`
- `src/app/api/telemetry/route.ts`
- `src/app/api/strips/[id]/cmd/route.ts`
- `src/app/api/rooms/[room_id]/ai_report/route.ts`

后端接入时可选两种方式：

1. 保持同路径，直接替换为真实实现（推荐）
2. 新建真实后端服务，前端通过反向代理把 `/api/*` 转发到后端

---

## 7. 联调验收清单

1. 切换 `range` 后，图表横轴与点数正确变化
2. 切换设备后，Dashboard/Live/Detail 数据一致
3. 命令执行后，`cmdId` 可查询到最终状态
4. 离线设备在 `/devices`、`/dashboard`、`/live` 展示一致
5. `/history` 导出 CSV 与图表数据一致

---

维护建议：后端每次接口变更都更新此文档版本号（例如 `v1.1`）并记录变更日志。
