# GitHub CI/CD 部署配置指南

## 服务器信息
- **IP地址**: 111.230.60.71
- **用户**: root
- **部署路径**: /opt/dorm-power

## 第一步：在服务器上配置SSH密钥

### 1. SSH登录到服务器
```bash
ssh root@111.230.60.71
# 密码: Lrx563647
```

### 2. 生成SSH密钥对（如果没有）
```bash
# 生成新的SSH密钥对
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_deploy -N ""

# 查看公钥
cat ~/.ssh/github_deploy.pub

# 将公钥添加到authorized_keys
cat ~/.ssh/github_deploy.pub >> ~/.ssh/authorized_keys

# 设置正确的权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

### 3. 获取私钥内容
```bash
cat ~/.ssh/github_deploy
```
复制输出的完整内容（包括 `-----BEGIN OPENSSH PRIVATE KEY-----` 和 `-----END OPENSSH PRIVATE KEY-----`）

## 第二步：配置GitHub Secrets

进入你的GitHub仓库 → Settings → Secrets and variables → Actions → New repository secret

添加以下Secrets：

| Secret名称 | 值 | 说明 |
|-----------|-----|------|
| `SSH_PRIVATE_KEY` | 服务器上生成的私钥内容 | 用于SSH连接服务器 |
| `DB_PASSWORD` | DormPower2024!@#Secure | PostgreSQL数据库密码 |
| `JWT_SECRET` | DormPowerSecureKey2024ProductionEnvironment111.230.60.71 | JWT签名密钥 |
| `MQTT_PASSWORD` | MqttSecure2024!@# | MQTT认证密码 |

## 第三步：配置服务器环境

### 1. 安装Docker和Docker Compose
```bash
# 更新系统
apt update && apt upgrade -y

# 安装Docker
curl -fsSL https://get.docker.com | sh

# 启动Docker服务
systemctl start docker
systemctl enable docker

# 安装Docker Compose
apt install docker-compose-plugin -y

# 验证安装
docker --version
docker compose version
```

### 2. 创建部署目录
```bash
mkdir -p /opt/dorm-power
```

### 3. 配置防火墙（开放必要端口）
```bash
# 开放HTTP端口
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 8000/tcp
ufw allow 3000/tcp

# 如果是云服务器，还需要在安全组中开放这些端口
```

## 第四步：触发部署

### 方式1：推送代码到main分支
```bash
git push origin main
```

### 方式2：手动触发
进入GitHub仓库 → Actions → Deploy to Production Server → Run workflow

## 部署后验证

### 1. 检查服务状态
```bash
ssh root@111.230.60.71
docker ps
docker compose -f /opt/dorm-power/docker-compose.production.yml logs -f
```

### 2. 访问应用
- **前端**: http://111.230.60.71:80 或 http://111.230.60.71:3000
- **后端API**: http://111.230.60.71:8000
- **健康检查**: http://111.230.60.71:8000/actuator/health

## 常见问题

### 1. SSH连接失败
```bash
# 检查SSH服务
systemctl status sshd

# 检查防火墙
ufw status

# 查看SSH日志
tail -f /var/log/auth.log
```

### 2. Docker容器启动失败
```bash
# 查看容器日志
docker logs dorm-power-backend
docker logs dorm-power-frontend

# 查看所有容器状态
docker ps -a
```

### 3. 数据库连接失败
```bash
# 检查PostgreSQL容器
docker exec -it dorm-power-postgres psql -U postgres -c "SELECT 1;"

# 重启数据库
docker compose -f /opt/dorm-power/docker-compose.production.yml restart postgres
```

## 安全建议

1. **修改默认密码**: 部署后立即修改数据库、MQTT等服务的密码
2. **启用HTTPS**: 配置SSL证书，启用Nginx HTTPS配置
3. **限制端口暴露**: 生产环境建议只开放80和443端口
4. **定期备份**: 设置数据库定时备份