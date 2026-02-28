#!/bin/bash

# ============================================
# 服务器初始化脚本 - 针对2核2G服务器优化
# 用途：解决内存不足导致的Bus error问题
# 执行方式：bash server-init-2g.sh
# ============================================

set -e

echo "=========================================="
echo "  服务器初始化 - 2核2G优化"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 检查并创建Swap分区
echo -e "${YELLOW}步骤1: 检查Swap分区...${NC}"
if swapon --show | grep -q "/swapfile"; then
    echo -e "${GREEN}✓ Swap分区已存在${NC}"
else
    echo -e "${YELLOW}创建1G Swap分区...${NC}"
    
    # 创建Swap文件
    fallocate -l 1G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=1024
    
    # 设置权限
    chmod 600 /swapfile
    
    # 格式化Swap
    mkswap /swapfile
    
    # 启用Swap
    swapon /swapfile
    
    # 添加到fstab（永久生效）
    if ! grep -q "/swapfile" /etc/fstab; then
        echo '/swapfile none swap sw 0 0' >> /etc/fstab
    fi
    
    echo -e "${GREEN}✓ Swap分区创建完成${NC}"
fi

# 2. 优化Swap使用策略
echo -e "${YELLOW}步骤2: 优化Swap使用策略...${NC}"
sysctl vm.swappiness=10
echo "vm.swappiness=10" >> /etc/sysctl.conf
echo -e "${GREEN}✓ Swap策略优化完成${NC}"

# 3. 清理系统缓存
echo -e "${YELLOW}步骤3: 清理系统缓存...${NC}"
sync
echo 3 > /proc/sys/vm/drop_caches
echo -e "${GREEN}✓ 系统缓存清理完成${NC}"

# 4. 清理Docker资源
echo -e "${YELLOW}步骤4: 清理Docker资源...${NC}"
docker system prune -af --volumes || true
echo -e "${GREEN}✓ Docker资源清理完成${NC}"

# 5. 优化Docker配置（针对小内存服务器）
echo -e "${YELLOW}步骤5: 优化Docker配置...${NC}"
mkdir -p /etc/docker

cat > /etc/docker/daemon.json << 'EOF'
{
  "storage-driver": "overlay2",
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 65536,
      "Soft": 65536
    }
  }
}
EOF

# 重启Docker服务
systemctl restart docker
echo -e "${GREEN}✓ Docker配置优化完成${NC}"

# 6. 显示内存状态
echo -e "${YELLOW}步骤6: 内存状态检查...${NC}"
echo ""
echo "=========================================="
echo "  内存使用情况"
echo "=========================================="
free -m
echo ""

echo "=========================================="
echo "  Swap使用情况"
echo "=========================================="
swapon --show
echo ""

echo "=========================================="
echo "  Docker版本"
echo "=========================================="
docker --version
docker-compose --version
echo ""

# 7. 显示优化建议
echo "=========================================="
echo -e "${GREEN}  初始化完成！${NC}"
echo "=========================================="
echo ""
echo "优化建议："
echo "1. 定期执行: docker system prune -af --volumes"
echo "2. 监控内存: free -m"
echo "3. 查看容器: docker ps"
echo "4. 查看日志: docker-compose logs"
echo ""
echo "如果仍然出现内存不足，建议："
echo "- 升级服务器到4G内存"
echo "- 减少同时运行的容器数量"
echo "- 使用外部数据库服务"
