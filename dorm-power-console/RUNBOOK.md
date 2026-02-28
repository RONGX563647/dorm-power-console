# Dorm Power Console 开发与部署工作流手册

本文档用于记录本项目在服务器上的常用运行方式，避免后续开发时遗忘命令。

- 项目目录：`~/Embedding_competition/web/front/dorm-power-console`
- 运行模式：
  - 开发热更新（`docker-compose.dev.yml`）
  - 生产部署（`docker-compose.yml` + `deploy.sh`）

---

## 1. 两种模式的区别

### 1.1 开发热更新模式

- 文件：`docker-compose.dev.yml`
- 目标：改代码后立即生效（热更新）
- 特点：
  - 容器内执行 `npm run dev`
  - 本地代码目录挂载进容器
  - 适合开发调试，不建议用于正式生产

### 1.2 生产部署模式

- 文件：`docker-compose.yml` + `deploy.sh`
- 目标：稳定运行，可上线
- 特点：
  - 构建后运行 `next start`
  - 更稳定，适合长期运行
  - 更新流程标准化（拉代码 -> 构建 -> 启动）

---

## 2. 开发热更新模式怎么运行

```bash
cd ~/Embedding_competition/web/front/dorm-power-console

docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f
```

访问：

- `http://<服务器IP>:3000/dashboard`

停止开发容器：

```bash
docker compose -f docker-compose.dev.yml down
```

---

## 3. 生产模式怎么运行

### 3.1 首次/手动部署

```bash
cd ~/Embedding_competition/web/front/dorm-power-console

docker compose up -d --build
docker compose ps
docker compose logs --tail=100
```

### 3.2 使用一键脚本更新（推荐）

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
chmod +x deploy.sh
./deploy.sh main
```

说明：

- `main` 是分支名，可换成 `dev`/`release` 等
- 脚本默认会：
  - `git fetch/pull`
  - `docker compose up -d --build`
  - 输出容器状态与日志

如果只想重启不重建：

```bash
NO_BUILD=1 ./deploy.sh main
```

---

## 4. 更新代码的标准流程

### 4.1 本地开发机

```bash
git add .
git commit -m "feat: xxx"
git push origin main
```

### 4.2 服务器

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
./deploy.sh main
```

---

## 5. 常用运维命令

```bash
# 查看容器状态
docker compose ps

# 查看日志
docker compose logs --tail=100
docker compose logs -f

# 重启
docker compose restart

# 停止并删除容器
docker compose down
```

---

## 6. 常见问题与解决

### 6.1 `Cannot connect to the Docker daemon`

Docker 服务未启动或用户无权限。

```bash
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
newgrp docker
```

### 6.2 `version is obsolete` 警告

`docker-compose.yml` 里的 `version` 字段过时。可删除首行 `version: "3.9"`，不影响运行。

### 6.3 构建很慢（尤其 `npm ci`）

常见于首次构建或网络慢。建议：

- 不要每次都用 `--no-cache`
- 配置 Docker 镜像加速器
- 耐心等待首次构建完成

### 6.4 端口 3000 无法访问

- 确认容器已启动：`docker compose ps`
- 确认服务器监听 3000：`sudo ss -lntp | grep 3000`
- 确认云安全组放通 TCP 3000

---

## 7. 重要建议（避免踩坑）

1. 开发模式和生产模式不要同时运行（都占 3000 端口）。
2. 每次生产更新优先使用 `deploy.sh`，避免漏步骤。
3. 线上建议使用固定分支和固定镜像标签，不要长期依赖 `latest`。
4. 更新前可先看：`git status`，避免覆盖服务器临时改动。

---

## 8. 快速执行清单

### 开发调试（热更新）

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
docker compose -f docker-compose.dev.yml up -d
```

### 生产更新（一键）

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
./deploy.sh main
```

### 访问地址

- `http://<服务器IP>:3000/dashboard`


---

## 9. 本地一键同步到服务器（Git Push + SSH Pull + 热更新）

新增脚本：`sync-git-dev.sh`

作用：

1. 本地 `git add/commit/push`
2. 远端自动 `git pull`
3. 远端自动执行 `docker compose -f docker-compose.dev.yml up -d`

示例：

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
chmod +x sync-git-dev.sh
./sync-git-dev.sh --host <服务器IP> --user ubuntu --remote-dir ~/Embedding_competition/web/front/dorm-power-console --branch main --message "feat: update"
```

可选参数：

```bash
--port 22
--identity ~/.ssh/id_rsa
--branch main
--message "your commit message"
--skip-commit   # 跳过本地提交推送，只做远端 pull + up
```
