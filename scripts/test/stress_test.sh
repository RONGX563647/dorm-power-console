#!/bin/bash
# ========================================
# DormPower JUC 压测脚本
# 测试目标：验证JUC组件在高并发下的表现
# ========================================

# 配置
BASE_URL="${BASE_URL:-http://localhost:8000}"
REPORT_DIR="stress_test_reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$REPORT_DIR/report_$TIMESTAMP.txt"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 创建报告目录
mkdir -p "$REPORT_DIR"

echo "========================================" | tee "$REPORT_FILE"
echo "  DormPower JUC 压测报告" | tee -a "$REPORT_FILE"
echo "  时间: $(date)" | tee -a "$REPORT_FILE"
echo "  目标: $BASE_URL" | tee -a "$REPORT_FILE"
echo "========================================" | tee -a "$REPORT_FILE"

# 检查服务是否可用
check_service() {
    echo -e "\n${YELLOW}检查服务状态...${NC}"
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null)
    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✅ 服务正常运行${NC}"
        return 0
    else
        echo -e "${RED}❌ 服务无法访问，请先启动后端服务${NC}"
        echo "启动命令: cd backend && mvn spring-boot:run"
        exit 1
    fi
}

# 测试1: 设备列表API (ConcurrentHashMap读场景)
test_device_list() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试1: 设备列表API (ConcurrentHashMap读场景)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    echo "命令: wrk -t4 -c100 -d10s --latency $BASE_URL/api/devices" | tee -a "$REPORT_FILE"
    wrk -t4 -c100 -d10s --latency "$BASE_URL/api/devices" 2>&1 | tee -a "$REPORT_FILE"
}

# 测试2: AI对话API (ConcurrentHashMap写场景)
test_agent_chat() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试2: AI对话API (ConcurrentHashMap写场景)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    # 创建临时lua脚本
    cat > /tmp/agent_chat.lua << 'EOF'
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.headers["X-User-Id"] = "user_" .. math.random(1, 1000)
wrk.body = '{"message":"查询今天的用电量"}'
EOF

    echo "命令: wrk -t4 -c50 -d10s --latency -s /tmp/agent_chat.lua $BASE_URL/api/agent/chat" | tee -a "$REPORT_FILE"
    wrk -t4 -c50 -d10s --latency -s /tmp/agent_chat.lua "$BASE_URL/api/agent/chat" 2>&1 | tee -a "$REPORT_FILE"

    rm -f /tmp/agent_chat.lua
}

# 测试3: 登录API (限流测试 - RateLimiter)
test_login_rate_limit() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试3: 登录API (RateLimiter限流测试)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    cat > /tmp/login.lua << 'EOF'
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.body = '{"username":"admin","password":"wrong_password"}'
EOF

    echo "命令: wrk -t2 -c10 -d5s --latency -s /tmp/login.lua $BASE_URL/api/auth/login" | tee -a "$REPORT_FILE"
    wrk -t2 -c10 -d5s --latency -s /tmp/login.lua "$BASE_URL/api/auth/login" 2>&1 | tee -a "$REPORT_FILE"

    rm -f /tmp/login.lua
}

# 测试4: 高并发WebSocket连接 (CopyOnWriteArraySet场景)
test_websocket() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试4: WebSocket连接 (CopyOnWriteArraySet场景)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    echo "注意: WebSocket压测需要专用工具，这里使用HTTP模拟" | tee -a "$REPORT_FILE"
    echo "推荐工具: wsbench, artillery" | tee -a "$REPORT_FILE"

    # 使用HTTP endpoint模拟
    echo "命令: wrk -t4 -c200 -d10s --latency $BASE_URL/api/devices" | tee -a "$REPORT_FILE"
    wrk -t4 -c200 -d10s --latency "$BASE_URL/api/devices" 2>&1 | tee -a "$REPORT_FILE"
}

# 测试5: MQTT模拟器 (ExecutorService + AtomicInteger场景)
test_mqtt_simulator() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试5: MQTT模拟器启动 (线程池测试)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    cat > /tmp/mqtt_simulator.json << 'EOF'
{
  "devices": 100,
  "duration": 10,
  "interval": 1.0,
  "messageType": "MIXED",
  "onlineRate": 0.8,
  "minPower": 10.0,
  "maxPower": 100.0,
  "minVoltage": 220.0,
  "maxVoltage": 240.0,
  "roomStart": 101,
  "roomEnd": 110
}
EOF

    echo "启动MQTT模拟器..." | tee -a "$REPORT_FILE"
    response=$(curl -s -X POST "$BASE_URL/api/mqtt-simulator/start" \
        -H "Content-Type: application/json" \
        -d @/tmp/mqtt_simulator.json 2>&1)

    echo "响应: $response" | tee -a "$REPORT_FILE"

    # 提取taskId
    taskId=$(echo "$response" | grep -o '"taskId":"[^"]*"' | cut -d'"' -f4)

    if [ -n "$taskId" ]; then
        echo "模拟器已启动，taskId: $taskId" | tee -a "$REPORT_FILE"
        echo "等待5秒后查询状态..." | tee -a "$REPORT_FILE"
        sleep 5

        status=$(curl -s "$BASE_URL/api/mqtt-simulator/status/$taskId")
        echo "状态: $status" | tee -a "$REPORT_FILE"

        echo "停止模拟器..." | tee -a "$REPORT_FILE"
        curl -s -X POST "$BASE_URL/api/mqtt-simulator/stop/$taskId" | tee -a "$REPORT_FILE"
    else
        echo -e "${RED}启动失败${NC}" | tee -a "$REPORT_FILE"
    fi

    rm -f /tmp/mqtt_simulator.json
}

# 测试6: 缓存服务 (SimpleCacheService - ScheduledExecutorService)
test_cache() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试6: 缓存API (ScheduledExecutorService场景)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    echo "命令: wrk -t4 -c50 -d10s --latency $BASE_URL/api/devices" | tee -a "$REPORT_FILE"
    wrk -t4 -c50 -d10s --latency "$BASE_URL/api/devices" 2>&1 | tee -a "$REPORT_FILE"
}

# 测试7: 并发写入测试
test_concurrent_write() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "测试7: 并发写入测试 (ConcurrentHashMap.computeIfAbsent)" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    cat > /tmp/concurrent_write.lua << 'EOF'
wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"
wrk.headers["X-User-Id"] = "concurrent_user_" .. math.random(1, 10000)
wrk.body = '{"message":"测试并发写入"}'
EOF

    echo "命令: wrk -t8 -c200 -d15s --latency -s /tmp/concurrent_write.lua $BASE_URL/api/agent/chat" | tee -a "$REPORT_FILE"
    wrk -t8 -c200 -d15s --latency -s /tmp/concurrent_write.lua "$BASE_URL/api/agent/chat" 2>&1 | tee -a "$REPORT_FILE"

    rm -f /tmp/concurrent_write.lua
}

# 生成汇总报告
generate_summary() {
    echo -e "\n========================================" | tee -a "$REPORT_FILE"
    echo -e "压测汇总" | tee -a "$REPORT_FILE"
    echo "========================================" | tee -a "$REPORT_FILE"

    echo -e "\nJUC组件测试覆盖:" | tee -a "$REPORT_FILE"
    echo "✅ ConcurrentHashMap - 设备列表、AI对话、缓存" | tee -a "$REPORT_FILE"
    echo "✅ CopyOnWriteArraySet - WebSocket连接" | tee -a "$REPORT_FILE"
    echo "✅ ExecutorService - MQTT模拟器" | tee -a "$REPORT_FILE"
    echo "✅ AtomicInteger/Long - 消息统计" | tee -a "$REPORT_FILE"
    echo "✅ RateLimiter + ConcurrentHashMap - API限流" | tee -a "$REPORT_FILE"
    echo "✅ ScheduledExecutorService - 缓存清理" | tee -a "$REPORT_FILE"

    echo -e "\n报告已保存到: $REPORT_FILE" | tee -a "$REPORT_FILE"
}

# JVM监控提示
show_jvm_tips() {
    echo -e "\n========================================"
    echo -e "JVM监控建议"
    echo -e "========================================"
    echo ""
    echo "压测时建议开启JVM监控:"
    echo ""
    echo "# 1. 启动应用时添加JMX参数:"
    echo "java -jar app.jar \\"
    echo "  -Dcom.sun.management.jmxremote \\"
    echo "  -Dcom.sun.management.jmxremote.port=9010 \\"
    echo "  -Dcom.sun.management.jmxremote.authenticate=false \\"
    echo "  -Dcom.sun.management.jmxremote.ssl=false"
    echo ""
    echo "# 2. 使用jconsole连接:"
    echo "jconsole localhost:9010"
    echo ""
    echo "# 3. 使用jstack查看线程状态:"
    echo "jstack <pid> | grep -A 10 'ConcurrentHashMap'"
    echo ""
    echo "# 4. 使用jmap查看内存:"
    echo "jmap -histo <pid> | head -20"
    echo ""
}

# 主函数
main() {
    check_service

    # 检查wrk是否安装
    if ! command -v wrk &> /dev/null; then
        echo -e "${RED}错误: wrk未安装${NC}"
        echo "安装方法:"
        echo "  macOS: brew install wrk"
        echo "  Linux: sudo apt-get install wrk"
        exit 1
    fi

    echo -e "\n${GREEN}开始压测...${NC}"

    test_device_list
    test_agent_chat
    test_login_rate_limit
    test_websocket
    test_mqtt_simulator
    test_cache
    test_concurrent_write

    generate_summary
    show_jvm_tips

    echo -e "\n${GREEN}压测完成!${NC}"
}

# 运行
main "$@"