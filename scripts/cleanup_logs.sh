#!/bin/bash
# 日志清理脚本（轻量级）
# 保留最近7天日志

set -e

# 配置
LOG_DIR="/opt/dorm-power/logs"
RETENTION_DAYS=7

# 日志文件列表
LOG_FILES=(
    "logs/dorm-power.log"
    "logs/audit.log"
    "logs/gc.log"
)

echo "=========================================="
echo "  日志清理开始: $(date)"
echo "=========================================="

# 清理每个日志文件
for LOG_FILE in "${LOG_FILES[@]}"; do
    FULL_PATH="${LOG_DIR}/${LOG_FILE}"
    
    if [ -f "$FULL_PATH" ]; then
        SIZE=$(du -h "$FULL_PATH" | cut -f1)
        echo "清理日志: $LOG_FILE ($SIZE)"
        
        # 使用logrotate清理
        logrotate "$FULL_PATH" --keep 7 --force 2>/dev/null || true
        
        # 手动清理旧日志
        find "$(dirname "$FULL_PATH")" -name "$(basename "$FULL_PATH").*" -type f -mtime +${RETENTION_DAYS} -delete 2>/dev/null || true
    fi
done

# 清理空目录
echo ""
echo "清理空目录..."
find "$LOG_DIR" -type d -empty -delete 2>/dev/null || true

# 显示清理后状态
echo ""
echo "清理后日志目录:"
du -sh "$LOG_DIR" 2>/dev/null | tail -10

# 检查磁盘空间
DISK_USAGE=$(df -h /opt/dorm-power | awk 'NR==2 {print $5}' | sed 's/%//')
echo ""
echo "磁盘使用率: $DISK_USAGE"

if [ ${DISK_USAGE%\%} -gt 80 ]; then
    echo "⚠️  警告: 磁盘使用率超过80%"
fi

echo ""
echo "=========================================="
echo "  日志清理完成: $(date)"
echo "=========================================="
