

```bash
# ===== 0) 基础依赖 =====
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg lsb-release

# ===== 1) 清理可能冲突的旧包（可选但推荐）=====
sudo apt-get remove -y docker docker.io docker-ce docker-ce-cli containerd runc docker-compose docker-compose-plugin || true
sudo snap remove docker || true

# ===== 2) 添加 Docker 官方源 =====
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# ===== 3) 安装 Docker + Compose 插件 =====
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# ===== 4) 配置腾讯云镜像加速 =====
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json > /dev/null << 'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com"
  ]
}
EOF

# ===== 5) 启动 Docker 服务 =====
sudo systemctl daemon-reload
sudo systemctl enable docker
sudo systemctl restart docker

# ===== 6) 给当前用户 docker 权限（避免每次 sudo）=====
sudo usermod -aG docker $USER
newgrp docker

# ===== 7) 验证 =====
docker --version
docker compose version
```

然后部署你的项目：

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
sed -i '/^version:/d' docker-compose.yml   # 去掉过时字段警告（可选）
docker compose up -d --build
docker compose ps
docker compose logs --tail=100
```
