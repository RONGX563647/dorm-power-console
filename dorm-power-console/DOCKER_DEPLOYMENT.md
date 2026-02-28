# Dorm Power Console Docker 部署文档

本文档用于在 Linux 服务器上通过 Docker / Docker Compose 部署 `dorm-power-console`（Next.js）。

## 1. 目标与方案

- 方案 A：单容器运行（`docker run`）
- 方案 B：`docker-compose`（推荐，易维护）
- 可选：Nginx 反向代理到容器端口

## 2. 前置条件

- 已安装 Docker 24+
- 已安装 Docker Compose Plugin（`docker compose`）
- 服务器可访问 npm 源

检查命令：

```bash
docker -v
docker compose version
```

## 3. 目录约定

项目目录示例：

```bash
/opt/dorm-power/web/front/dorm-power-console
```

以下命令默认都在该目录执行。

## 4. Dockerfile（生产）

在 `dorm-power-console` 目录创建 `Dockerfile`：

```dockerfile
# syntax=docker/dockerfile:1

FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000

# 安全起见使用非 root 用户
RUN addgroup -S nextjs && adduser -S nextjs -G nextjs

COPY --from=builder /app/package*.json ./
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/next.config.ts ./next.config.ts

USER nextjs
EXPOSE 3000
CMD ["npm", "run", "start"]
```

## 5. .dockerignore（建议）

创建 `.dockerignore`：

```gitignore
node_modules
.next
.git
.gitignore
npm-debug.log
Dockerfile*
docker-compose*.yml
README.md
LINUX_DEPLOYMENT.md
DOCKER_DEPLOYMENT.md
```

## 6. 方案 A：docker run

### 6.1 构建镜像

```bash
docker build -t dorm-power-console:latest .
```

### 6.2 启动容器

```bash
docker run -d \
  --name dorm-power-console \
  -p 3000:3000 \
  --restart always \
  -e NODE_ENV=production \
  -e PORT=3000 \
  dorm-power-console:latest
```

### 6.3 查看状态

```bash
docker ps
docker logs -f dorm-power-console
```

访问：

- `http://<server-ip>:3000/dashboard`

## 7. 方案 B：docker compose（推荐）

创建 `docker-compose.yml`：

```yaml
version: "3.9"
services:
  dorm-power-console:
    build:
      context: .
      dockerfile: Dockerfile
    image: dorm-power-console:latest
    container_name: dorm-power-console
    ports:
      - "3000:3000"
    environment:
      NODE_ENV: production
      PORT: 3000
      # NEXT_PUBLIC_API_BASE_URL: "https://api.your-domain.com"
    restart: unless-stopped
```

启动：

```bash
docker compose up -d --build
```

查看：

```bash
docker compose ps
docker compose logs -f
```

停止：

```bash
docker compose down
```

## 8. Nginx 反向代理（可选）

如果对外用 80/443，可让 Nginx 代理到 `127.0.0.1:3000`。

Nginx 站点配置示例：

```nginx
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
```

## 9. 更新发布

```bash
cd /opt/dorm-power/web/front/dorm-power-console
# git pull
docker compose up -d --build
```

## 10. 回滚（保留旧镜像标签）

建议发布时打版本标签：

```bash
docker build -t dorm-power-console:2026-02-21 .
```

回滚：

```bash
docker stop dorm-power-console || true
docker rm dorm-power-console || true
docker run -d \
  --name dorm-power-console \
  -p 3000:3000 \
  --restart always \
  dorm-power-console:2026-02-21
```

## 11. 常见问题

### 11.1 构建失败：依赖安装慢/失败

- 切换 npm 源或设置代理
- 确认服务器可访问 npm registry

### 11.2 容器启动后访问 502/连接失败

- 检查容器是否在运行：`docker ps`
- 看日志：`docker logs dorm-power-console`
- 检查端口映射：`-p 3000:3000`

### 11.3 Nginx 仍旧 502

- 容器是否监听 3000
- Nginx `proxy_pass` 是否指向 `127.0.0.1:3000`
- 防火墙/安全组是否放通

### 11.4 内存不足导致容器退出

- `docker stats`
- 给服务器增加内存或启用 swap

## 12. 快速命令清单

```bash
# 构建
docker build -t dorm-power-console:latest .

# 启动
docker run -d --name dorm-power-console -p 3000:3000 --restart always dorm-power-console:latest

# 日志
docker logs -f dorm-power-console

# compose 一键更新
docker compose up -d --build

# 停止并清理
docker compose down
```

---

如果你希望，我可以继续给你补：

1. `docker-compose.prod.yml`（含 Nginx + 前端双容器）
2. GitHub Actions 自动构建并推送镜像到 Docker Hub/阿里云 ACR
