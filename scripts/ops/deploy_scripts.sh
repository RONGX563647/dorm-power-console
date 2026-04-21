#!/bin/bash
# 一键部署运维脚本到服务器

set -e

SCRIPTS_DIR="/opt/dorm-power/scripts/ops"
LOGS_DIR="/opt/dorm-power/logs"
BACKUPS_DIR="/opt/dorm-power/backups"

echo "=========================================="
echo "  部署运维脚本"
echo "=========================================="

# 创建目录
echo "创建目录..."
mkdir -p "$SCRIPTS_DIR"
mkdir -p "$LOGS_DIR"
mkdir -p "$BACKUPS_DIR"

# 复制脚本
echo "复制脚本..."
cp scripts/ops/backup_db.sh "$SCRIPTS_DIR/"
cp scripts/ops/cleanup_logs.sh "$SCRIPTS_DIR/"
cp scripts/ops/monitor.sh "$SCRIPTS_DIR/"

# 设置执行权限
echo "设置执行权限..."
chmod +x "$SCRIPTS_DIR"/*.sh

# 安装crontab
echo "安装定时任务..."
crontab scripts/ops/crontab

# 验证安装
echo ""
echo "验证安装:"
ls -lh "$SCRIPTS_DIR/"
echo ""
echo "当前定时任务:"
crontab -l

echo ""
echo "=========================================="
echo "  部署完成"
echo "=========================================="
echo ""
echo "使用说明:"
echo "  - 数据库备份: $SCRIPTS_DIR/backup_db.sh"
echo "  - 日志清理:   $SCRIPTS_DIR/cleanup_logs.sh"
echo "  - 系统监控:   $SCRIPTS_DIR/monitor.sh"
echo ""
echo "定时任务:"
echo "  - 每5分钟: 监控检查"
echo "  - 每天凌晨2点: 数据库备份"
echo "  - 每天凌晨3点: 日志清理"
echo ""
