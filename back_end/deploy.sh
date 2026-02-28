#!/usr/bin/env bash
# 部署脚本
# 用于自动拉取最新代码并部署应用

set -euo pipefail  # 遇到错误立即退出

# 获取脚本所在目录
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
# 设置部署分支，默认为master
BRANCH="${1:-master}"

# 切换到应用目录
cd "$APP_DIR"

# 打印部署信息
echo "[deploy] app dir: $APP_DIR"
echo "[deploy] branch: $BRANCH"

# 如果是Git仓库，拉取最新代码
if [[ -d .git ]]; then
  echo "[deploy] fetching latest code..."
  git fetch --all --prune
  git checkout "$BRANCH"
  git pull --ff-only origin "$BRANCH"
fi

# 启动Docker Compose服务
echo "[deploy] starting docker compose..."
docker compose up -d --build

# 显示服务状态
echo "[deploy] service status:"
docker compose ps

# 显示后端日志（最后80行）
echo "[deploy] backend logs (tail 80):"
docker compose logs --tail=80 backend

echo "[deploy] done"

