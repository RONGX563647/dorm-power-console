# Dorm Power Console Linux 环境配置与部署文档

本文档用于在 Linux 服务器上部署 `dorm-power-console`（Next.js + Ant Design）。

## 1. 目标与架构

- 前端应用：Next.js（生产模式 `next start`）
- 进程托管：`systemd`（推荐）
- 反向代理：`Nginx`
- 访问路径：`http(s)://your-domain/`

## 2. 服务器要求

- OS：Ubuntu 20.04+/Debian 11+/CentOS 7+
- CPU：2 核及以上（建议）
- 内存：2GB+（建议）
- 磁盘：5GB+ 可用空间
- 网络：可访问 npm 源

## 3. Node.js 与 npm

建议使用 Node 20 LTS（更稳，兼容性更好）。

### 3.1 安装 Node 20（Ubuntu/Debian）

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
node -v
npm -v
```

## 4. 获取项目

```bash
# 示例目录
sudo mkdir -p /opt/dorm-power
sudo chown -R $USER:$USER /opt/dorm-power
cd /opt/dorm-power

# 你的代码仓库
# git clone <your-repo-url> .
```

前端目录假设为：

```bash
cd /opt/dorm-power/web/front/dorm-power-console
```

## 5. npm 缓存与依赖安装

为避免权限和缓存问题，建议在项目内固定 npm 缓存目录。

### 5.1 配置 `.npmrc`

```bash
cat > .npmrc << 'EOF'
cache=/opt/dorm-power/web/front/.npm-cache
fund=false
audit=false
EOF
```

### 5.2 安装依赖

```bash
npm install
```

## 6. 环境变量

如果有后端 API 域名，建议使用环境变量。

### 6.1 生产环境变量文件（可选）

```bash
cat > .env.production << 'EOF'
NODE_ENV=production
PORT=3000
# NEXT_PUBLIC_API_BASE_URL=https://api.your-domain.com
EOF
```

> 当前项目有本地 mock API，可不填 `NEXT_PUBLIC_API_BASE_URL`。

## 7. 构建与本地启动验证

```bash
npm run lint
npm run build
npm run start
```

浏览器访问：

- `http://<server-ip>:3000/dashboard`

## 8. 使用 systemd 托管（推荐）

### 8.1 创建 service 文件

```bash
sudo tee /etc/systemd/system/dorm-power.service > /dev/null << 'EOF'
[Unit]
Description=Dorm Power Console (Next.js)
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/dorm-power/web/front/dorm-power-console
Environment=NODE_ENV=production
Environment=PORT=3000
ExecStart=/usr/bin/npm run start
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

> 注意：
> - 如果 `npm` 不在 `/usr/bin/npm`，请执行 `which npm` 后替换。
> - `User=www-data` 可改为你的部署用户。

### 8.2 启动并开机自启

```bash
sudo systemctl daemon-reload
sudo systemctl enable dorm-power
sudo systemctl start dorm-power
sudo systemctl status dorm-power
```

### 8.3 查看日志

```bash
sudo journalctl -u dorm-power -f
```

## 9. Nginx 反向代理

### 9.1 新建站点配置

```bash
sudo tee /etc/nginx/sites-available/dorm-power > /dev/null << 'EOF'
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
EOF
```

### 9.2 启用配置

```bash
sudo ln -s /etc/nginx/sites-available/dorm-power /etc/nginx/sites-enabled/dorm-power
sudo nginx -t
sudo systemctl reload nginx
```

## 10. HTTPS（推荐）

可使用 Certbot：

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 11. 升级发布流程（标准）

```bash
cd /opt/dorm-power
# git pull
cd /opt/dorm-power/web/front/dorm-power-console
npm install
npm run build
sudo systemctl restart dorm-power
sudo systemctl status dorm-power
```

## 12. 常见问题排查

### 12.1 `ENOENT: package.json not found`

你不在前端目录。请先：

```bash
cd /opt/dorm-power/web/front/dorm-power-console
```

### 12.2 npm 权限或缓存报错（EPERM/ENOENT cache）

检查 `.npmrc` 的 `cache=` 路径是否可写。

```bash
mkdir -p /opt/dorm-power/web/front/.npm-cache
chmod -R 755 /opt/dorm-power/web/front/.npm-cache
```

### 12.3 端口占用

```bash
sudo lsof -i :3000
```

如需改端口，修改 service 中 `Environment=PORT=xxxx` 并重启服务。

### 12.4 页面更新不生效

确保执行了：

```bash
npm run build
sudo systemctl restart dorm-power
```

## 13. 快速命令汇总

```bash
# 进入项目
cd /opt/dorm-power/web/front/dorm-power-console

# 安装 + 构建
npm install
npm run build

# 启动（临时）
npm run start

# 服务管理
sudo systemctl restart dorm-power
sudo systemctl status dorm-power
sudo journalctl -u dorm-power -f
```

---

如需，我可以继续补一份 `Docker + docker-compose` 版部署文档（适合快速迁移和回滚）。
