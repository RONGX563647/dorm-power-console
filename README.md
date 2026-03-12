# Dorm Power - 宿舍用电管理系统

宿舍用电管理系统，支持设备管理、实时监控、用电分析等功能。

## 项目架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Next.js)                        │
│                      http://117.72.210.10:3000              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Nginx 反向代理                         │
│                      http://117.72.210.10                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    后端 (Spring Boot)                        │
│                    http://117.72.210.10:8000                │
└────────┬────────────────┬──────────────────┬────────────────┘
         │                │                  │
         ▼                ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  PostgreSQL  │  │    MQTT      │  │   WebSocket  │
│   Database   │  │   Broker     │  │   Server     │
└──────────────┘  └──────────────┘  └──────────────┘
```

## 技术栈

### 前端
- **框架**: Next.js 14 (App Router)
- **UI组件**: Ant Design 6.x
- **语言**: TypeScript
- **状态管理**: React Context API
- **图表**: Recharts

### 后端
- **框架**: Spring Boot 3.x
- **语言**: Java 21
- **数据库**: PostgreSQL 16
- **ORM**: Spring Data JPA
- **认证**: JWT
- **消息队列**: MQTT (Eclipse Mosquitto)
- **实时通信**: WebSocket

### 部署
- **容器化**: Docker & Docker Compose
- **CI/CD**: GitHub Actions
- **反向代理**: Nginx
- **服务器**: 2核2G (117.72.210.10)

## 快速开始

### 前置要求

- Node.js 18+
- Java 21
- PostgreSQL 16
- Docker & Docker Compose

### 本地开发

#### 1. 克隆仓库

```bash
git clone https://github.com/RONGX563647/dorm-power-console.git
cd dorm-power-console
```

#### 2. 配置环境变量

```bash
# 后端配置
cd backend
cp .env.example .env
vim .env

# 前端配置
cd ../dorm-power-console
cp .env.example .env.local
vim .env.local
```

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端将在 http://localhost:8000 启动

#### 4. 启动前端

```bash
cd dorm-power-console
npm install
npm run dev
```

前端将在 http://localhost:3000 启动

### Docker 部署

#### 快速部署（本地）

```bash
# 复制环境变量配置
cp .env.docker.example .env.docker

# 启动所有服务
docker-compose -f docker-compose.full.yml --env-file .env.docker up -d

# 查看日志
docker-compose -f docker-compose.full.yml logs -f
```

#### 生产环境部署

详细部署文档请查看：
- [生产环境部署指南](DEPLOY_117.72.210.10.md)
- [CI/CD 配置指南](CI_CD_SETUP.md)

## CI/CD 自动部署

### 自动部署

推送代码到 `main` 分支会自动触发部署：

```bash
git add .
git commit -m "feat: new feature"
git push origin main
```

### 手动部署

1. 进入 GitHub Actions: https://github.com/RONGX563647/dorm-power-console/actions
2. 选择 "Deploy to Production Server"
3. 点击 "Run workflow"

### 配置 GitHub Secrets

首次部署前需要配置以下 Secrets：

| Secret 名称 | 说明 |
|------------|------|
| `SSH_PRIVATE_KEY` | 服务器SSH私钥 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT签名密钥 |
| `MQTT_PASSWORD` | MQTT密码 |

详细配置步骤请查看 [CI/CD 配置指南](CI_CD_SETUP.md)

## 项目结构

```
dorm-power/
├── backend/                 # 后端代码 (Spring Boot)
│   ├── src/
│   │   └── main/
│   │       ├── java/com/dormpower/
│   │       │   ├── controller/     # 控制器
│   │       │   ├── service/        # 服务层
│   │       │   ├── repository/     # 数据访问层
│   │       │   ├── entity/         # 实体类
│   │       │   ├── dto/            # 数据传输对象
│   │       │   ├── mqtt/          # MQTT客户端
│   │       │   └── config/        # 配置类
│   │       └── resources/
│   │           ├── application.yml # 应用配置
│   │           └── data.sql       # 初始化数据
│   ├── Dockerfile
│   └── pom.xml
│
├── dorm-power-console/       # 前端代码 (Next.js)
│   ├── src/
│   │   ├── app/              # App Router页面
│   │   ├── components/       # React组件
│   │   ├── lib/              # 工具函数
│   │   └── hooks/            # 自定义Hooks
│   ├── Dockerfile
│   └── package.json
│
├── .github/workflows/        # GitHub Actions工作流
│   └── deploy.yml           # 自动部署配置
│
├── nginx/                    # Nginx配置
│   ├── nginx.conf
│   └── conf.d/
│
├── doc/                     # 项目文档
│   ├── 需求分析文档.md
│   ├── 数据库设计文档.md
│   └── Java重构项目方案文档.md
│
├── .env.production.example  # 生产环境配置示例
├── .env.docker.example      # Docker环境配置示例
├── docker-compose.production.yml  # 生产环境Docker配置
├── deploy-production.sh     # 生产环境部署脚本
├── server-deploy.sh        # 服务器端部署脚本
├── backup.sh              # 数据库备份脚本
└── README.md              # 本文件
```

## API 文档

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户登录 |
| POST | `/api/auth/logout` | 用户登出 |
| GET | `/api/auth/me` | 获取当前用户信息 |

### 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/devices` | 获取设备列表 |
| GET | `/api/devices/{id}/status` | 获取设备状态 |

### 命令控制

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/strips/{id}/cmd` | 发送控制命令 |
| GET | `/api/cmd/{cmdId}` | 查询命令状态 |

### 遥测数据

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/telemetry` | 获取遥测数据 |
| GET | `/api/strips/{id}/telemetry` | 获取设备遥测数据 |

### AI报告

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rooms/{room_id}/ai_report` | 获取AI分析报告 |

## 功能特性

### 核心功能

- ✅ 用户认证与授权（JWT）
- ✅ 设备管理与监控
- ✅ 实时用电数据展示
- ✅ 远程设备控制
- ✅ 历史数据查询
- ✅ AI用电分析报告
- ✅ 实时WebSocket推送
- ✅ MQTT设备通信

### 技术特性

- ✅ RESTful API设计
- ✅ 响应式前端界面
- ✅ 容器化部署
- ✅ 自动化CI/CD
- ✅ 健康检查与监控
- ✅ 数据库自动备份
- ✅ 资源使用优化（2核2G服务器）

## 服务器信息

- **IP地址**: 117.72.210.10
- **配置**: 2核2G
- **操作系统**: Ubuntu 20.04/22.04 LTS

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://117.72.210.10:3000 |
| 后端API | http://117.72.210.10:8000 |
| 健康检查 | http://117.72.210.10:8000/health |
| Nginx代理 | http://117.72.210.10 |

## 文档

- [生产环境部署指南](DEPLOY_117.72.210.10.md)
- [CI/CD 配置指南](CI_CD_SETUP.md)
- [Docker 部署文档](DOCKER_DEPLOYMENT.md)
- [宝塔面板部署](BAOTA_DOCKER_DEPLOYMENT.md)
- [需求分析文档](doc/需求分析文档.md)
- [数据库设计文档](doc/数据库设计文档.md)
- [Java重构方案](doc/Java重构项目方案文档.md)

## 开发规范

### 代码规范

- 后端遵循阿里巴巴Java开发手册
- 前端遵循Airbnb JavaScript规范
- 使用ESLint和Prettier进行代码格式化

### 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具链相关
```

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- GitHub: https://github.com/RONGX563647/dorm-power-console
- Issues: https://github.com/RONGX563647/dorm-power-console/issues

## 致谢

感谢所有为本项目做出贡献的开发者！
