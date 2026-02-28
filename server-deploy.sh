#!/bin/bash
# Server-side deployment script for CI/CD
# This script is executed on the server after files are synced

set -e

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_DIR="/opt/dorm-power"
BACKUP_DIR="${PROJECT_DIR}/backup/old"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Dorm Power CI/CD Deployment${NC}"
echo -e "${GREEN}============================================${NC}"

# 创建备份目录
mkdir -p ${BACKUP_DIR}

# 备份当前运行的容器配置
echo -e "${YELLOW}备份当前配置...${NC}"
if [ -f ${PROJECT_DIR}/.env.production ]; then
    cp ${PROJECT_DIR}/.env.production ${BACKUP_DIR}/.env.production.backup
    echo -e "${GREEN}配置已备份${NC}"
fi

# 停止旧服务
echo -e "${YELLOW}停止旧服务...${NC}"
cd ${PROJECT_DIR}
docker-compose -f docker-compose.production.yml down 2>/dev/null || true

# 清理旧镜像（可选，节省空间）
echo -e "${YELLOW}清理Docker缓存...${NC}"
docker system prune -f --volumes 2>/dev/null || true

# 构建并启动新服务
echo -e "${YELLOW}构建并启动服务...${NC}"
docker-compose -f docker-compose.production.yml build --no-cache
docker-compose -f docker-compose.production.yml up -d

# 等待服务启动
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 30

# 检查服务状态
echo -e "${YELLOW}检查服务状态...${NC}"
docker-compose -f docker-compose.production.yml ps

# 健康检查
echo -e "${YELLOW}执行健康检查...${NC}"
MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s http://localhost:8000/health > /dev/null 2>&1; then
        echo -e "${GREEN}后端服务健康检查通过${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -e "${YELLOW}等待后端服务启动... ($RETRY_COUNT/$MAX_RETRIES)${NC}"
    sleep 5
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo -e "${RED}后端服务启动失败${NC}"
    echo -e "${YELLOW}查看日志...${NC}"
    docker-compose -f docker-compose.production.yml logs backend
    exit 1
fi

# 检查前端服务
if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo -e "${GREEN}前端服务健康检查通过${NC}"
else
    echo -e "${YELLOW}前端服务可能未完全启动，请稍后检查${NC}"
fi

# 清理旧备份（保留最近3次）
echo -e "${YELLOW}清理旧备份...${NC}"
find ${BACKUP_DIR} -name "*.backup" -type f -mtime +3 -delete 2>/dev/null || true

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}访问地址:${NC}"
echo -e "  前端: ${YELLOW}http://117.72.210.10:3000${NC}"
echo -e "  后端: ${YELLOW}http://117.72.210.10:8000${NC}"
echo ""
echo -e "${BLUE}查看日志:${NC}"
echo -e "  ${YELLOW}cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml logs -f${NC}"
echo ""
