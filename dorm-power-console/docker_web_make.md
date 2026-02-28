

```bash
# 1) 拉代码（首次）
mkdir -p ~/Embedding_competition
cd ~/Embedding_competition
git clone <你的GitHub仓库地址> .

# 2) 进入前端项目目录（注意是 front）
cd ~/Embedding_competition/web/front/dorm-power-console

# 3) 构建并后台启动
docker compose up -d --build

# 4) 查看运行状态
docker compose ps

# 5) 查看日志
docker compose logs --tail=100
docker compose logs -f
```

访问地址：

```bash
http://<服务器公网IP>:3000/dashboard
```

---

如果外网打不开，放通腾讯云安全组 3000 端口后再试。  
（TCP 3000，来源 `0.0.0.0/0`）

---

常用运维命令：

```bash
# 更新代码后重发
cd ~/Embedding_competition/web/front/dorm-power-console
git pull
docker compose up -d --build

# 重启
docker compose restart

# 停止并删除容器
docker compose down
```

---

可选：去掉 `docker-compose.yml` 的过时警告（`version` 字段）：

```bash
cd ~/Embedding_competition/web/front/dorm-power-console
sed -i '/^version:/d' docker-compose.yml
```