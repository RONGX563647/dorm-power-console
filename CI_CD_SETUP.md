# CI/CD 配置指南

## GitHub 仓库信息

- **仓库地址**: https://github.com/RONGX563647/dorm-power-console
- **服务器IP**: 117.72.210.10
- **部署方式**: GitHub Actions + Docker + 宝塔面板

## 第一步：配置 GitHub Secrets

进入 GitHub 仓库：https://github.com/RONGX563647/dorm-power-console/settings/secrets/actions

### 必需的 Secrets

| Secret 名称 | 说明 | 示例值 | 如何获取 |
|------------|------|--------|----------|
| `SSH_PRIVATE_KEY` | 服务器SSH私钥 | `-----BEGIN RSA PRIVATE KEY-----...` | 在服务器上生成 |
| `DB_PASSWORD` | 数据库密码 | `YourStrongPassword2024` | 自定义 |
| `JWT_SECRET` | JWT签名密钥 | `your-very-long-secret-key-must-be-at-least-256-bits` | 自定义（至少32位） |
| `MQTT_PASSWORD` | MQTT密码 | `YourMqttPassword2024` | 自定义 |

### 配置步骤

#### 1. 生成 SSH 密钥对

在本地或服务器上执行：

```bash
# 生成SSH密钥
ssh-keygen -t rsa -b 4096 -C "github-actions" -f ~/.ssh/github_actions_deploy

# 查看私钥（用于 GitHub Secrets）
cat ~/.ssh/github_actions_deploy
```

**重要**: 复制完整的私钥内容（包括 `-----BEGIN RSA PRIVATE KEY-----` 和 `-----END RSA PRIVATE KEY-----`）

#### 2. 配置服务器 SSH 公钥

将公钥添加到服务器的 authorized_keys：

```bash
# 在服务器上执行
echo "你的公钥内容" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
```

#### 3. 配置 GitHub Secrets

1. 进入仓库设置：https://github.com/RONGX563647/dorm-power-console/settings/secrets/actions
2. 点击 **"New repository secret"**
3. 添加以下 Secrets：

**SSH_PRIVATE_KEY**:
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...（完整的私钥内容）...
-----END RSA PRIVATE KEY-----
```

**DB_PASSWORD**:
```
DormPower2024!@#Secure
```

**JWT_SECRET**:
```
DormPowerSecureKey2024ProductionEnvironment117.72.210.10
```

**MQTT_PASSWORD**:
```
MqttSecure2024!@#
```

## 第二步：服务器初始化

### 首次部署前，需要在服务器上执行以下操作：

```bash
# 1. 连接服务器
ssh root@117.72.210.10

# 2. 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com | sh
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 3. 创建项目目录
mkdir -p /opt/dorm-power
mkdir -p /opt/dorm-power/backup/postgres
mkdir -p /opt/dorm-power/backup/old
mkdir -p /opt/dorm-power/backend/mosquitto/config
mkdir -p /opt/dorm-power/backend/mosquitto/data
mkdir -p /opt/dorm-power/backend/mosquitto/log
mkdir -p /opt/dorm-power/nginx/conf.d
mkdir -p /opt/dorm-power/nginx/ssl

# 4. 配置防火墙
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw allow 8000/tcp  # Backend API
ufw allow 3000/tcp  # Frontend
ufw --force enable

# 5. 添加执行权限
chmod +x /opt/dorm-power/deploy-production.sh
chmod +x /opt/dorm-power/server-deploy.sh
chmod +x /opt/dorm-power/backup.sh
```

## 第三步：触发部署

### 自动部署

推送代码到 `main` 分支会自动触发部署：

```bash
git add .
git commit -m "feat: update deployment"
git push origin main
```

### 手动部署

1. 进入 GitHub Actions 页面：https://github.com/RONGX563647/dorm-power-console/actions
2. 选择 **"Deploy to Production Server"** 工作流
3. 点击 **"Run workflow"**
4. 选择分支（通常是 `main`）
5. 点击 **"Run workflow"** 按钮

## 第四步：监控部署

### 查看部署日志

1. 进入 GitHub Actions 页面
2. 点击最新的工作流运行
3. 查看各个步骤的执行日志

### 部署步骤说明

工作流包含以下步骤：

1. **Checkout code** - 检出代码
2. **Setup Node.js** - 安装 Node.js 环境
3. **Setup Java** - 安装 Java 环境
4. **Build Backend** - 构建后端（Maven）
5. **Build Frontend** - 构建前端（npm）
6. **Configure SSH** - 配置 SSH 连接
7. **Copy files to server** - 同步文件到服务器
8. **Create environment file** - 创建环境变量文件
9. **Deploy with Docker** - Docker 部署
10. **Health check** - 健康检查
11. **Cleanup SSH key** - 清理 SSH 密钥
12. **Notify deployment status** - 通知部署状态

## 第五步：验证部署

### 访问服务

| 服务 | 地址 |
|------|------|
| 前端 | http://117.72.210.10:3000 |
| 后端API | http://117.72.210.10:8000 |
| 健康检查 | http://117.72.210.10:8000/health |

### 检查服务状态

```bash
# SSH 连接到服务器
ssh root@117.72.210.10

# 查看容器状态
cd /opt/dorm-power
docker-compose -f docker-compose.production.yml ps

# 查看日志
docker-compose -f docker-compose.production.yml logs -f

# 查看特定服务日志
docker logs dorm-power-backend
docker logs dorm-power-console
```

## 故障排查

### 部署失败

1. **检查 GitHub Actions 日志**
   - 查看失败步骤的详细日志
   - 检查错误信息

2. **检查服务器连接**
   ```bash
   # 测试 SSH 连接
   ssh -i ~/.ssh/github_actions_deploy root@117.72.210.10
   ```

3. **检查服务器资源**
   ```bash
   # 查看磁盘空间
   df -h

   # 查看内存使用
   free -h

   # 查看 Docker 状态
   docker ps -a
   ```

### 服务无法启动

```bash
# 查看容器日志
docker logs dorm-power-backend
docker logs dorm-power-postgres

# 重启服务
cd /opt/dorm-power
docker-compose -f docker-compose.production.yml restart

# 完全重新部署
docker-compose -f docker-compose.production.yml down
docker-compose -f docker-compose.production.yml up -d
```

### 健康检查失败

```bash
# 手动测试健康检查
curl http://117.72.210.10:8000/health

# 查看后端日志
docker logs dorm-power-backend --tail 100
```

## 安全建议

1. **定期更新密钥**
   - 每 90 天更换一次 SSH 密钥
   - 定期更新数据库密码

2. **限制访问**
   - 仅允许 GitHub Actions IP 访问（如可能）
   - 使用防火墙限制端口访问

3. **监控日志**
   - 定期检查服务器日志
   - 设置异常告警

4. **备份策略**
   - 定期备份数据库
   - 备份配置文件

## 宝塔面板集成

如果使用宝塔面板：

1. 在宝塔面板中安装 Docker 管理器
2. 通过宝塔面板查看容器状态
3. 通过宝塔面板管理 SSL 证书
4. 通过宝塔面板配置 Nginx 反向代理

## 联系支持

如有问题，请查看：
- GitHub Actions 文档: https://docs.github.com/en/actions
- Docker 文档: https://docs.docker.com/
- 项目文档: `DEPLOY_117.72.210.10.md`
