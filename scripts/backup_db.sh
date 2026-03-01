#!/bin/bash
# 数据库备份脚本（轻量级）
# 保留最近3天备份

set -e

# 配置
BACKUP_DIR="/opt/dorm-power/backups"
DB_NAME="dorm_power"
DB_USER="postgres"
RETENTION_DAYS=3
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${DATE}.sql.gz"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

echo "=========================================="
echo "  数据库备份开始: $(date)"
echo "=========================================="

# 执行备份
echo "正在备份数据库: $DB_NAME"
pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"

# 检查备份是否成功
if [ -f "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✅ 备份成功: $BACKUP_FILE ($SIZE)"
else
    echo "❌ 备份失败"
    exit 1
fi

# 清理旧备份
echo "清理${RETENTION_DAYS}天前的旧备份..."
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete
echo "✅ 清理完成"

# 显示当前备份列表
echo ""
echo "当前备份文件:"
ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -5

echo ""
echo "=========================================="
echo "  数据库备份完成: $(date)"
echo "=========================================="
