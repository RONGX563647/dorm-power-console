# Dorm Power Console 项目总结

本文档是当前项目的最终总结，覆盖：项目定位、技术栈、目录结构、开发与部署流程、脚本用途、常见命令与排障。

## 1. 项目定位

`dorm-power-console` 是一个宿舍用电管理前端控制台，核心能力包括：

- 实时功率监控与趋势展示
- 设备与插孔状态查看
- 快捷控制（开关/定时/模式）
- 历史报表与导出
- AI 分析展示（卡片化）

## 2. 技术栈

- Next.js（App Router）
- React + TypeScript
- Ant Design 6
- ECharts（`echarts-for-react`）
- Docker / Docker Compose

## 3. 当前关键功能状态

### 3.1 页面与路由

- `/dashboard`：总览、KPI、事件时间线、Top 消耗榜
- `/devices`：设备列表、在线率、筛选与搜索
- `/devices/[id]`：设备详情、风险提示、时间范围
- `/live`：实时监控、命令回执状态机、控制日志
- `/history`：历史统计、CSV 导出
- `/ai`：AI 分析卡片化展示

### 3.2 数据与图表

- 时间范围联动：`60s / 24h / 7d / 30d`
- 图表支持阈值线（`markLine`）
- 图表支持命令标记点（`markPoint`）
- 图表支持异常区间高亮（`markArea`）

### 3.3 命令回执状态机

已实现状态流：

`idle -> sending -> pending -> success/failed/timeout`

相关文件：

- `src/hooks/useCmdDispatcher.ts`
- `src/app/live/page.tsx`

## 4. 重要脚本与文件说明

### 4.1 生产部署

- `docker-compose.yml`：生产容器编排
- `deploy.sh`：生产一键更新（`git pull + compose up`）

### 4.2 开发热更新

- `docker-compose.dev.yml`：开发容器（`next dev` + 挂载代码）

### 4.3 代码同步

- `sync-git-dev.sh`：本地 `git push` + 远端 `git pull` + 启动 dev 容器
- `sync-dev.sh`：已废弃，占位提醒用

### 4.4 文档

- `LINUX_DEPLOYMENT.md`：Linux 直接部署
- `DOCKER_DEPLOYMENT.md`：Docker 部署
- `RUNBOOK.md`：日常运维与开发流程手册

## 5. 两种运行模式（必须区分）

### 5.1 开发调试（热更新）

用途：高频改代码、快速看效果

```bash
docker compose -f docker-compose.dev.yml up -d
```

特点：

- 改服务器代码后立即热更新
- 不适合生产长期运行

### 5.2 生产更新（一键）

用途：稳定上线

```bash
./deploy.sh main
```

特点：

- 走构建产物，稳定性高
- 更新通常会重建镜像

## 6. 标准工作流建议

### 6.1 开发阶段

1. 本地开发并提交：`git add/commit/push`
2. 用 `sync-git-dev.sh` 同步到服务器开发容器
3. 浏览器验证效果

### 6.2 发布阶段

1. 确认目标分支代码稳定
2. 服务器执行：`./deploy.sh <branch>`
3. 检查容器状态与日志

## 7. 常用命令速查

### 7.1 生产容器

```bash
# 启动/重建
docker compose up -d --build

# 仅启动（不重建）
docker compose up -d

# 查看状态
docker compose ps

# 日志
docker compose logs --tail=100
docker compose logs -f

# 停止/删除
docker compose down
```

### 7.2 开发容器

```bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f
docker compose -f docker-compose.dev.yml down
```

## 8. 常见问题

### 8.1 `docker compose` 不可用

原因：Compose 插件缺失或 Docker 安装冲突。

处理：安装 Docker CE + `docker-compose-plugin`。

### 8.2 `Cannot connect to Docker daemon`

原因：Docker 服务未启动或用户无权限。

处理：

```bash
sudo systemctl start docker
sudo usermod -aG docker $USER
newgrp docker
```

### 8.3 构建慢

原因：首次拉依赖/网络慢。

处理：

- 避免频繁 `--no-cache`
- 配置镜像加速器

### 8.4 端口访问失败

检查：

- `docker compose ps`
- `ss -lntp | grep 3000`
- 云安全组是否放通 TCP 3000

## 9. 下一步建议

- 增加 `docker-compose.prod.yml`（前端 + Nginx）
- 接入真实后端 API（替换 mock）
- 引入 CI/CD（自动构建与发布）
- 为 `deploy.sh` 增加健康检查与失败回滚

---

维护说明：本文件建议与 `RUNBOOK.md` 一起保留，前者偏全局总结，后者偏操作手册。
