# 宿舍用电管理系统 API 对接文档（前端版）
## 一、基础信息
### 1.1 API 基础地址
```
http://localhost:8000
```

### 1.2 认证方式
所有需要权限的接口均需在请求头中携带 `Bearer Token`，格式如下：
```
Authorization: Bearer {token}
```
- Token 获取：登录接口返回 `token` 字段
- Token 刷新：通过刷新令牌接口更新 Token
- Token 失效：Token 过期或无效时，接口返回 401 状态码，前端需引导用户重新登录

### 1.3 通用响应格式
#### 成功响应（200）
```json
{
  "code": 200,
  "message": "success",
  "data": {} // 具体业务数据，不同接口结构不同
}
```

#### 失败响应（4xx/5xx）
```json
{
  "code": 400/401/403/404/500,
  "message": "错误描述信息",
  "data": null
}
```

## 二、接口列表
### 2.1 健康检查（无需认证）
| 接口名称 | 请求URL | 请求方法 | 说明 |
|----------|---------|----------|------|
| 服务健康检查 | `/health` | GET | 检查后端服务是否正常运行 |

#### 响应示例
```json
{
  "service": "dorm-power-backend",
  "status": "UP",
  "timestamp": 1740987654321
}
```

### 2.2 认证模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 用户登录 | `/api/auth/login` | POST | ❌ |
| 获取当前用户信息 | `/api/auth/me` | GET | ✅ |
| 刷新令牌 | `/api/auth/refresh` | POST | ✅ |
| 用户登出 | `/api/auth/logout` | POST | ✅ |
| 用户注册 | `/api/auth/register` | POST | ✅ |
| 忘记密码 | `/api/auth/forgot-password` | POST | ❌ |

#### 2.2.1 用户登录
**请求体**：
```json
{
  "account": "admin", // 账号
  "password": "admin123" // 密码
}
```

**响应示例**：
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", // 认证令牌
  "user": {
    "id": "1",
    "username": "admin",
    "email": "admin@dorm.local",
    "role": "ADMIN"
  }
}
```

#### 2.2.2 用户注册
**请求体**：
```json
{
  "username": "testuser_abc123", // 用户名
  "email": "test@test.com", // 邮箱
  "password": "password123" // 密码（需符合复杂度要求）
}
```

### 2.3 设备模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取设备列表 | `/api/devices` | GET | ✅ |
| 获取单个设备状态 | `/api/devices/{deviceId}/status` | GET | ✅ |
| 按房间查询设备 | `/api/devices/room/{roomId}` | GET | ✅ |
| 创建设备 | `/api/devices` | POST | ✅ |
| 获取设备状态历史 | `/api/devices/{deviceId}/history` | GET | ✅ |

#### 2.3.1 获取设备列表
**响应示例**：
```json
[
  {
    "id": "strip01", // 设备ID
    "name": "302宿舍插座", // 设备名称
    "room": "A-302", // 所属房间
    "online": true // 在线状态
  }
]
```

#### 2.3.2 获取单个设备状态
**响应示例**：
```json
{
  "online": true,
  "total_power_w": 120.5, // 总功率（瓦）
  "socket_status": [true, false], // 插座状态
  "last_update": 1740987654321 // 最后更新时间戳
}
```

### 2.4 命令模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 发送设备命令 | `/api/strips/{deviceId}/cmd` | POST | ✅ |
| 查询命令状态 | `/api/cmd/{cmdId}` | GET | ✅ |
| 获取设备命令历史 | `/api/commands/device/{deviceId}` | GET | ✅ |

#### 2.4.1 发送设备命令
**请求体**：
```json
{
  "action": "on", // on/off（打开/关闭）
  "socket": 1 // 插座编号（1开始）
}
```

**响应示例**：
```json
{
  "cmdId": "cmd_12345678", // 命令ID，用于查询状态
  "status": "PENDING" // 命令状态：PENDING/EXECUTED/FAILED
}
```

### 2.5 遥测数据模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取遥测数据 | `/api/telemetry` | GET | ✅ |
| 获取用电统计 | `/api/telemetry/statistics` | GET | ✅ |

#### 2.5.1 获取遥测数据
**请求参数（Query）**：
| 参数名 | 说明 | 示例值 |
|--------|------|--------|
| device | 设备ID | strip01 |
| range | 时间范围 | 60s/24h/7d/30d |

**响应示例**：
```json
[
  {
    "ts": 1740987654321, // 时间戳
    "power_w": 120.5 // 功率（瓦）
  },
  {
    "ts": 1740987655321,
    "power_w": 118.3
  }
]
```

### 2.6 AI报告模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取房间AI报告 | `/api/rooms/{roomId}/ai_report` | GET | ✅ |

**请求参数（Query）**：
| 参数名 | 说明 | 示例值 |
|--------|------|--------|
| period | 统计周期 | 7d/30d |

**响应示例**：
```json
{
  "room_id": "A-302",
  "summary": "302宿舍本周用电量8.5度，峰值功率250W，整体用电正常",
  "anomalies": ["无异常用电行为"],
  "recommendations": ["建议避开高峰用电时段"],
  "power_stats": {
    "avg_power_w": 120.5, // 平均功率
    "peak_power_w": 250.0, // 峰值功率
    "peak_time": 1740987654321, // 峰值时间
    "total_kwh": 8.5 // 总用电量（度）
  }
}
```

### 2.7 用户管理模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取用户列表 | `/api/users` | GET | ✅ |
| 获取单个用户详情 | `/api/users/{userId}` | GET | ✅ |
| 更新用户信息 | `/api/users/{userId}` | PUT | ✅ |
| 修改用户密码 | `/api/users/{userId}/password` | POST | ✅ |

### 2.8 通知系统模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取用户通知 | `/api/notifications` | GET | ✅ |
| 获取未读通知数 | `/api/notifications/unread/count` | GET | ✅ |
| 获取通知偏好设置 | `/api/notifications/preferences` | GET | ✅ |
| 更新通知偏好设置 | `/api/notifications/preferences` | PUT | ✅ |

**请求参数（Query）**：
所有通知接口均需携带 `username` 参数指定用户

### 2.9 告警管理模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取设备告警列表 | `/api/alerts/device/{deviceId}` | GET | ✅ |
| 获取未解决告警 | `/api/alerts/unresolved` | GET | ✅ |
| 获取设备告警配置 | `/api/alerts/config/{deviceId}` | GET | ✅ |

### 2.10 宿舍管理模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取楼栋列表 | `/api/dorm/buildings` | GET | ✅ |
| 获取房间列表 | `/api/dorm/rooms` | GET | ✅ |
| 房间入住 | `/api/dorm/rooms/{roomId}/check-in` | POST | ✅ |
| 房间退宿 | `/api/dorm/rooms/{roomId}/check-out` | POST | ✅ |

### 2.11 RBAC权限模块
| 接口名称 | 请求URL | 请求方法 | 是否需要认证 |
|----------|---------|----------|--------------|
| 获取用户角色 | `/api/rbac/users/{username}/roles` | GET | ✅ |
| 检查用户权限 | `/api/rbac/users/{username}/has-permission` | GET | ✅ |

**请求参数（Query）**：
`permissionCode`：权限编码（如 `api:devices:read`）

## 三、通用注意事项
1. **请求超时**：所有接口建议设置 10s 超时时间
2. **参数校验**：前端需对请求参数进行基础校验（如非空、格式），减少无效请求
3. **Token 处理**：
   - Token 过期后接口返回 401，需引导用户重新登录
   - 建议在请求拦截器中统一添加 Authorization 请求头
4. **分页处理**：列表类接口（如日志、通知）返回格式包含 `content`（数据列表）、`totalElements`（总条数），需支持分页
5. **错误处理**：
   - 400：参数错误，需提示用户检查输入
   - 401：未认证/Token失效，需重新登录
   - 403：无权限，需提示用户无操作权限
   - 404：资源不存在，需友好提示
   - 500：服务器错误，需提示用户稍后重试

## 总结
### 核心对接要点
1. **认证流程**：登录获取 Token → 请求头携带 Token → 过期刷新/重新登录
2. **核心接口**：设备列表/状态、命令发送、遥测数据、AI报告是前端核心对接接口
3. **数据格式**：
   - 时间字段均为毫秒级时间戳
   - 功率单位为瓦（W），电量单位为度（kWh）
   - 布尔值字段统一使用 true/false

### 开发建议
1. 封装统一的请求工具类，处理 Token 携带、超时、错误统一拦截
2. 对遥测数据等高频接口做本地缓存，减少重复请求
3. 设备命令发送后，轮询查询命令状态直到完成/失败
4. 针对不同权限角色（管理员/普通用户），控制前端功能按钮的显示/隐藏