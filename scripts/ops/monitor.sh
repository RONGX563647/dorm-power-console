#!/bin/bash
# 基础监控脚本（轻量级）
# 监控内存、磁盘、服务状态

set -e

# 配置
ALERT_WEBHOOK="https://sctapi.ftqq.com/SERVER123456.send"
MEMORY_THRESHOLD=80
DISK_THRESHOLD=80
LOG_FILE="/opt/dorm-power/logs/monitor.log"

# 发送告警函数
send_alert() {
    local message="$1"
    local level="$2"
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [$level] $message" >> "$LOG_FILE"
    
    # 发送到Server酱（可选）
    if [ -n "$ALERT_WEBHOOK" ]; then
        curl -s -X POST "$ALERT_WEBHOOK" \
            -d "text=$message" \
            -d "desp=宿舍电源系统告警" 2>/dev/null || true
    fi
}

# 检查内存使用
check_memory() {
    local memory_usage=$(free | awk 'NR==2{printf "%.0f", $3/$2*100}')
    local memory_int=${memory_usage%.*}
    
    echo "内存使用率: ${memory_usage}%"
    
    if [ $memory_int -gt $MEMORY_THRESHOLD ]; then
        send_alert "内存使用率过高: ${memory_usage}%" "WARNING"
    fi
}

# 检查磁盘使用
check_disk() {
    local disk_usage=$(df -h /opt/dorm-power | awk 'NR==2 {print $5}' | sed 's/%//')
    local disk_int=${disk_usage%.*}
    
    echo "磁盘使用率: ${disk_usage}%"
    
    if [ $disk_int -gt $DISK_THRESHOLD ]; then
        send_alert "磁盘使用率过高: ${disk_usage}%" "WARNING"
    fi
}

# 检查服务状态
check_services() {
    echo "检查服务状态..."
    
    # 检查PostgreSQL
    if pg_isready -h localhost -p 5432 -U postgres 2>/dev/null; then
        echo "✅ PostgreSQL: 运行中"
    else
        send_alert "PostgreSQL服务异常" "ERROR"
    fi
    
    # 检查后端服务
    if curl -s -f http://localhost:8000/health 2>/dev/null | grep -q "UP"; then
        echo "✅ 后端服务: 运行中"
    else
        send_alert "后端服务异常" "ERROR"
    fi
    
    # 检查Nginx
    if pgrep -x nginx >/dev/null; then
        echo "✅ Nginx: 运行中"
    else
        send_alert "Nginx服务异常" "ERROR"
    fi
}

# 主监控循环
echo "=========================================="
echo "  系统监控开始: $(date)"
echo "=========================================="

check_memory
check_disk
check_services

echo ""
echo "=========================================="
echo "  监控完成: $(date)"
echo "=========================================="
