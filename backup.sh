#!/bin/bash
# Dorm Power Database Backup Script
# 自动备份PostgreSQL数据库

set -e

# 配置
PROJECT_DIR="/opt/dorm-power"
BACKUP_DIR="${PROJECT_DIR}/backup/postgres"
DB_CONTAINER="dorm-power-postgres"
DB_NAME="dorm_power"
DB_USER="dormpower"
RETENTION_DAYS=7

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# 创建备份目录
mkdir -p ${BACKUP_DIR}

# 生成备份文件名
BACKUP_FILE="${BACKUP_DIR}/dorm_power_$(date +%Y%m%d_%H%M%S).sql"

echo -e "${YELLOW}开始备份数据库...${NC}"

# 执行备份
if docker exec ${DB_CONTAINER} pg_dump -U ${DB_USER} ${DB_NAME} > ${BACKUP_FILE}; then
    echo -e "${GREEN}备份成功: ${BACKUP_FILE}${NC}"
    
    # 压缩备份文件
    gzip ${BACKUP_FILE}
    echo -e "${GREEN}备份文件已压缩: ${BACKUP_FILE}.gz${NC}"
    
    # 删除旧备份
    echo -e "${YELLOW}清理旧备份文件（保留${RETENTION_DAYS}天）...${NC}"
    find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
    
    # 显示备份列表
    echo -e "${GREEN}当前备份文件:${NC}"
    ls -lh ${BACKUP_DIR}/*.sql.gz 2>/dev/null || echo "无备份文件"
    
    echo -e "${GREEN}备份完成！${NC}"
else
    echo -e "${RED}备份失败！${NC}"
    exit 1
fi
