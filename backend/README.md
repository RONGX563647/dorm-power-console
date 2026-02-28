# 宿舍电源管理系统后端

## 项目简介

宿舍电源管理系统是一个面向高校宿舍的智能电源监控和控制系统，旨在通过物联网技术实现对宿舍用电设备的实时监控、远程控制和智能分析，提高用电安全性和能源利用效率。

## 技术栈

- **基础框架**：Spring Boot 3.2.x
- **Web框架**：Spring Web 6.1.x
- **数据访问**：Spring Data JPA 3.2.x
- **数据库**：PostgreSQL 15.x
- **实时通信**：Spring WebSocket 6.1.x
- **MQTT客户端**：Eclipse Paho MQTT Client 1.2.5
- **安全框架**：Spring Security 6.2.x
- **依赖管理**：Maven 3.9.x
- **日志框架**：Log4j2 2.20.x
- **测试框架**：JUnit 5 + Mockito

## 项目结构

```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── dormpower/
│   │   │           ├── DormPowerApplication.java        # 应用入口
│   │   │           ├── config/                          # 配置类
│   │   │           ├── controller/                      # 控制器
│   │   │           ├── model/                          # 数据模型
│   │   │           ├── repository/                     # 数据仓库
│   │   │           ├── service/                        # 业务服务
│   │   │           ├── websocket/                      # WebSocket
│   │   │           ├── mqtt/                           # MQTT
│   │   │           ├── dto/                            # 数据传输对象
│   │   │           └── util/                           # 工具类
│   │   └── resources/
│   │       ├── application.yml                        # 应用配置文件
│   │       └── application-dev.yml                    # 开发环境配置
│   └── test/                                           # 测试代码
├── pom.xml                                            # Maven配置文件
└── README.md                                          # 项目说明
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.9+
- PostgreSQL 15+
- MQTT Broker (如Mosquitto)

### 配置步骤

1. **配置数据库**
   - 创建PostgreSQL数据库：`dorm_power`
   - 创建用户：`postgres`，密码：`postgres`

2. **配置MQTT Broker**
   - 安装并启动Mosquitto
   - 配置用户名：`admin`，密码：`admin`

3. **配置应用**
   - 修改 `src/main/resources/application.yml` 文件，根据实际环境调整配置

### 运行项目

```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run
```

### 访问接口

- 健康检查：`http://localhost:8000/api/health`
- 登录接口：`POST http://localhost:8000/api/api/auth/login`
- 设备列表：`GET http://localhost:8000/api/api/devices`
- WebSocket：`ws://localhost:8000/api/ws`

## API接口

| 接口路径 | 方法 | 功能描述 |
|----------|------|----------|
| `/health` | GET | 健康检查 |
| `/api/auth/login` | POST | 管理员登录 |
| `/api/devices` | GET | 获取设备列表 |
| `/api/devices/{deviceId}/status` | GET | 获取设备状态 |
| `/api/telemetry` | GET | 获取遥测数据 |
| `/api/strips/{deviceId}/cmd` | POST | 下发控制命令 |
| `/api/cmd/{cmdId}` | GET | 查询命令状态 |
| `/api/rooms/{roomId}/ai_report` | GET | 获取AI分析报告 |
| `/ws` | WebSocket | 实时通信 |

## 功能特性

- **设备管理**：自动注册、列表查询、状态查询、在线状态管理
- **控制功能**：命令下发、冲突检测、状态查询、超时处理
- **数据采集与分析**：遥测数据采集、历史数据查询、AI分析报告
- **实时通信**：WebSocket推送、MQTT通信
- **系统管理**：管理员登录、健康检查

## 开发指南

### 代码规范

- 遵循Spring Boot编码规范
- 使用Java 17+特性
- 代码注释清晰
- 单元测试覆盖率≥80%

### 开发流程

1. 从`develop`分支创建功能分支
2. 开发完成后提交代码
3. 运行单元测试
4. 提交Pull Request
5. 代码审查通过后合并到`develop`分支

## 部署指南

### 本地开发环境

- 使用`application-dev.yml`配置
- 数据库：`dorm_power_dev`
- MQTT客户端ID：`dorm-power-backend-dev`

### 生产环境

- 使用`application.yml`配置
- 数据库：`dorm_power`
- MQTT客户端ID：`dorm-power-backend`
- 建议使用Docker容器化部署

## 监控与维护

- **日志监控**：使用ELK Stack收集和分析日志
- **性能监控**：使用Prometheus和Grafana监控系统性能
- **告警机制**：设置关键指标告警
- **定期备份**：定期备份数据库和配置文件

## 许可证

本项目采用MIT许可证。
