#!/bin/bash
# Dorm Power Production Deployment Script
# 服务器: 117.72.210.10 (2核2G)
# 使用方法: ./deploy-production.sh

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 服务器配置
SERVER_IP="117.72.210.10"
PROJECT_DIR="/opt/dorm-power"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Dorm Power Production Deployment${NC}"
echo -e "${GREEN}  Server: ${SERVER_IP}${NC}"
echo -e "${GREEN}============================================${NC}"

# 检查是否以root运行
if [ "$EUID" -ne 0 ]; then 
    echo -e "${RED}请使用 sudo 运行此脚本${NC}"
    exit 1
fi

# 检查Docker是否安装
check_docker() {
    echo -e "${BLUE}检查 Docker 环境...${NC}"
    if ! command -v docker &> /dev/null; then
        echo -e "${YELLOW}Docker 未安装，正在安装...${NC}"
        curl -fsSL https://get.docker.com | sh
        systemctl enable docker
        systemctl start docker
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${YELLOW}Docker Compose 未安装，正在安装...${NC}"
        curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        chmod +x /usr/local/bin/docker-compose
    fi
    
    echo -e "${GREEN}Docker 环境检查完成${NC}"
}

# 创建项目目录
setup_directories() {
    echo -e "${BLUE}创建项目目录...${NC}"
    mkdir -p ${PROJECT_DIR}
    mkdir -p ${PROJECT_DIR}/backup/postgres
    mkdir -p ${PROJECT_DIR}/backend/mosquitto/config
    mkdir -p ${PROJECT_DIR}/backend/mosquitto/data
    mkdir -p ${PROJECT_DIR}/backend/mosquitto/log
    mkdir -p ${PROJECT_DIR}/nginx/conf.d
    mkdir -p ${PROJECT_DIR}/nginx/ssl
    echo -e "${GREEN}目录创建完成${NC}"
}

# 配置防火墙
setup_firewall() {
    echo -e "${BLUE}配置防火墙...${NC}"
    
    # 检查是否已安装ufw
    if command -v ufw &> /dev/null; then
        ufw default deny incoming
        ufw default allow outgoing
        ufw allow 22/tcp      # SSH
        ufw allow 80/tcp      # HTTP
        ufw allow 443/tcp     # HTTPS
        ufw allow 8000/tcp    # Backend API
        ufw allow 3000/tcp    # Frontend
        ufw --force enable
        echo -e "${GREEN}UFW防火墙配置完成${NC}"
    elif command -v firewall-cmd &> /dev/null; then
        firewall-cmd --permanent --add-port=80/tcp
        firewall-cmd --permanent --add-port=443/tcp
        firewall-cmd --permanent --add-port=8000/tcp
        firewall-cmd --permanent --add-port=3000/tcp
        firewall-cmd --reload
        echo -e "${GREEN}FirewallD配置完成${NC}"
    else
        echo -e "${YELLOW}未检测到防火墙，请手动配置端口${NC}"
    fi
}

# 部署服务
deploy_services() {
    echo -e "${BLUE}开始部署服务...${NC}"
    
    cd ${PROJECT_DIR}
    
    # 检查环境变量文件
    if [ ! -f .env.production ]; then
        echo -e "${RED}错误: .env.production 文件不存在${NC}"
        echo -e "${YELLOW}请复制 .env.production.example 为 .env.production 并修改配置${NC}"
        exit 1
    fi
    
    # 停止旧服务
    echo -e "${YELLOW}停止旧服务...${NC}"
    docker-compose -f docker-compose.production.yml down 2>/dev/null || true
    
    # 清理旧镜像
    echo -e "${YELLOW}清理旧镜像...${NC}"
    docker system prune -f
    
    # 构建并启动服务
    echo -e "${YELLOW}构建并启动服务...${NC}"
    docker-compose -f docker-compose.production.yml --env-file .env.production build --no-cache
    docker-compose -f docker-compose.production.yml --env-file .env.production up -d
    
    # 等待服务启动
    echo -e "${YELLOW}等待服务启动...${NC}"
    sleep 30
    
    echo -e "${GREEN}服务部署完成${NC}"
}

# 检查服务状态
check_services() {
    echo -e "${BLUE}检查服务状态...${NC}"
    
    cd ${PROJECT_DIR}
    
    # 检查容器状态
    echo -e "${YELLOW}容器状态:${NC}"
    docker-compose -f docker-compose.production.yml ps
    
    # 检查后端健康状态
    echo -e "${YELLOW}检查后端服务...${NC}"
    if curl -s http://localhost:8000/health > /dev/null; then
        echo -e "${GREEN}后端服务运行正常${NC}"
    else
        echo -e "${RED}后端服务可能未正常启动${NC}"
    fi
    
    # 检查前端
    echo -e "${YELLOW}检查前端服务...${NC}"
    if curl -s http://localhost:3000 > /dev/null; then
        echo -e "${GREEN}前端服务运行正常${NC}"
    else
        echo -e "${RED}前端服务可能未正常启动${NC}"
    fi
}

# 显示访问信息
show_access_info() {
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}  部署成功！${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "${BLUE}访问地址:${NC}"
    echo -e "  前端: ${YELLOW}http://${SERVER_IP}:3000${NC}"
    echo -e "  后端API: ${YELLOW}http://${SERVER_IP}:8000${NC}"
    echo -e "  健康检查: ${YELLOW}http://${SERVER_IP}:8000/health${NC}"
    echo ""
    echo -e "${BLUE}Nginx代理:${NC}"
    echo -e "  HTTP: ${YELLOW}http://${SERVER_IP}${NC}"
    echo ""
    echo -e "${BLUE}管理命令:${NC}"
    echo -e "  查看日志: ${YELLOW}cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml logs -f${NC}"
    echo -e "  停止服务: ${YELLOW}cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml down${NC}"
    echo -e "  重启服务: ${YELLOW}cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml restart${NC}"
    echo -e "  查看状态: ${YELLOW}cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml ps${NC}"
    echo ""
    echo -e "${BLUE}数据备份:${NC}"
    echo -e "  备份数据库: ${YELLOW}${PROJECT_DIR}/backup.sh${NC}"
    echo ""
}

# 主函数
main() {
    echo -e "${GREEN}开始部署 Dorm Power 生产环境...${NC}"
    
    check_docker
    setup_directories
    setup_firewall
    deploy_services
    check_services
    show_access_info
    
    echo -e "${GREEN}部署完成！${NC}"
}

# 处理命令行参数
case "${1:-}" in
    --check)
        check_services
        ;;
    --stop)
        echo -e "${YELLOW}停止服务...${NC}"
        cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml down
        echo -e "${GREEN}服务已停止${NC}"
        ;;
    --restart)
        echo -e "${YELLOW}重启服务...${NC}"
        cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml restart
        echo -e "${GREEN}服务已重启${NC}"
        ;;
    --logs)
        cd ${PROJECT_DIR} && docker-compose -f docker-compose.production.yml logs -f
        ;;
    --update)
        echo -e "${YELLOW}更新部署...${NC}"
        cd ${PROJECT_DIR}
        
        echo -e "${YELLOW}清理系统资源...${NC}"
        docker system prune -f 2>/dev/null || true
        sync && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true
        
        echo -e "${YELLOW}停止旧服务...${NC}"
        docker-compose -f docker-compose.production.yml down --remove-orphans 2>/dev/null || true
        
        echo -e "${YELLOW}启动新服务...${NC}"
        docker-compose -f docker-compose.production.yml up -d --build
        
        echo -e "${GREEN}更新完成${NC}"
        ;;
    --help|-h)
        echo "使用方法: $0 [选项]"
        echo ""
        echo "选项:"
        echo "  (无)       执行完整部署"
        echo "  --check    检查服务状态"
        echo "  --stop     停止所有服务"
        echo "  --restart  重启所有服务"
        echo "  --logs     查看实时日志"
        echo "  --update   更新部署（重新构建）"
        echo "  --help     显示帮助信息"
        ;;
    *)
        main
        ;;
esac
