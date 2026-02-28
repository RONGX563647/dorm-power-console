# 服务器 117.72.210.10 部署指南

## 服务器配置

- **IP地址**: 117.72.210.10
- **配置**: 2核2G
- **操作系统**: Ubuntu 20.04/22.04 LTS (推荐)

## 快速部署步骤

### 第一步：准备服务器

```bash
# 1. 连接服务器
ssh root@117.72.210.10

# 2. 更新系统
apt update && apt upgrade -y

# 3. 安装必要工具
apt install -y git curl wget vim
```

### 第二步：上传项目文件

#### 方式1：通过 Git 克隆

```bash
cd /opt
git clone https://github.com/your-repo/dorm-power.git
cd dorm-power
```

#### 方式2：通过 SCP 上传

在本地执行：
```bash
# 压缩项目文件
tar -czvf dorm-power.tar.gz dorm-power/

# 上传到服务器
scp dorm-power.tar.gz root@117.72.210.10:/opt/

# 连接服务器解压
ssh root@117.72.210.10
cd /opt
tar -xzvf dorm-power.tar.gz
```

### 第三步：配置环境变量

```bash
cd /opt/dorm-power

# 复制生产环境配置
cp .env.production.example .env.production

# 编辑配置（必须修改密码）
vim .env.production
```

**必须修改的配置项：**
```bash
# 数据库密码（必须修改）
DB_PASSWORD=YourStrongPasswordHere

# JWT密钥（必须修改，至少32位）
JWT_SECRET=your-very-long-secret-key-must-be-at-least-256-bits-for-production

# MQTT密码（必须修改）
MQTT_PASSWORD=YourMqttPasswordHere
```

### 第四步：执行部署脚本

```bash
cd /opt/dorm-power

# 添加执行权限
chmod +x deploy-production.sh
chmod +x backup.sh

# 执行部署
./deploy-production.sh
```

部署脚本会自动：
1. 安装 Docker 和 Docker Compose
2. 配置防火墙
3. 构建并启动所有服务
4. 检查服务状态

### 第五步：验证部署

```bash
# 检查容器状态
docker ps

# 检查后端健康状态
curl http://117.72.210.10:8000/health

# 查看日志
./deploy-production.sh --logs
```

## 访问地址

部署完成后，可以通过以下地址访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://117.72.210.10:3000 | Web界面 |
| 后端API | http://117.72.210.10:8000 | REST API |
| 健康检查 | http://117.72.210.10:8000/health | 服务状态 |
| Nginx | http://117.72.210.10 | 反向代理入口 |

## 管理命令

```bash
cd /opt/dorm-power

# 查看服务状态
./deploy-production.sh --check

# 查看实时日志
./deploy-production.sh --logs

# 重启服务
./deploy-production.sh --restart

# 停止服务
./deploy-production.sh --stop

# 更新部署
./deploy-production.sh --update

# 手动备份数据库
./backup.sh
```

## 配置定时备份

```bash
# 编辑 crontab
crontab -e

# 添加定时任务（每天凌晨2点备份）
0 2 * * * /opt/dorm-power/backup.sh >> /var/log/dorm-power-backup.log 2>&1

# 查看定时任务
crontab -l
```

## 配置SSL证书（HTTPS）

### 方式1：使用 Let's Encrypt

```bash
# 安装 certbot
apt install -y certbot

# 申请证书
certbot certonly --standalone -d your-domain.com

# 复制证书到项目目录
cp /etc/letsencrypt/live/your-domain.com/fullchain.pem /opt/dorm-power/nginx/ssl/cert.pem
cp /etc/letsencrypt/live/your-domain.com/privkey.pem /opt/dorm-power/nginx/ssl/key.pem

# 修改 nginx 配置启用 HTTPS
vim /opt/dorm-power/nginx/conf.d/default.conf

# 重启服务
cd /opt/dorm-power && docker-compose restart nginx
```

### 方式2：使用宝塔面板

参考 `BAOTA_DOCKER_DEPLOYMENT.md` 文档。

## 性能优化（2核2G）

### 已优化的配置

1. **JVM内存**: 限制为 768MB
2. **PostgreSQL**: 优化连接数和缓冲区
3. **容器资源限制**:
   - 后端: 1GB内存，0.75核CPU
   - 前端: 512MB内存，0.5核CPU
   - 数据库: 512MB内存，0.5核CPU

### 监控资源使用

```bash
# 查看容器资源使用
docker stats

# 查看系统资源使用
htop

# 查看磁盘使用
df -h
du -sh /opt/dorm-power/*
```

## 故障排查

### 服务无法启动

```bash
# 查看详细日志
docker-compose -f docker-compose.production.yml logs

# 检查端口占用
netstat -tlnp | grep -E '3000|8000|80|443'

# 重启服务
./deploy-production.sh --restart
```

### 内存不足

```bash
# 清理Docker缓存
docker system prune -a

# 重启服务释放内存
./deploy-production.sh --restart
```

### 数据库连接失败

```bash
# 检查数据库容器
docker logs dorm-power-postgres

# 进入数据库容器
docker exec -it dorm-power-postgres psql -U dormpower -d dorm_power
```

## 安全建议

1. **修改默认密码**: 所有默认密码必须修改
2. **配置防火墙**: 仅开放必要端口
3. **启用HTTPS**: 使用SSL证书加密传输
4. **定期备份**: 已配置自动备份任务
5. **更新系统**: 定期更新Docker镜像和系统补丁

## 文件说明

```
/opt/dorm-power/
├── .env.production          # 生产环境配置（需修改）
├── docker-compose.production.yml  # 生产环境Docker配置
├── deploy-production.sh     # 部署脚本
├── backup.sh               # 备份脚本
├── nginx/                  # Nginx配置
│   ├── nginx.conf
│   └── conf.d/
├── backend/                # 后端代码
│   ├── Dockerfile
│   └── mosquitto/
├── dorm-power-console/     # 前端代码
│   └── Dockerfile
└── backup/                 # 备份目录
    └── postgres/
```

## 联系支持

如有问题，请查看：
- 部署文档: `DEPLOY_117.72.210.10.md`
- Docker文档: `DOCKER_DEPLOYMENT.md`
- 宝塔部署: `BAOTA_DOCKER_DEPLOYMENT.md`
