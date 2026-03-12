# 运维脚本使用说明

轻量级运维脚本，适合2核2G服务器。

## 脚本列表

| 脚本 | 功能 | 执行频率 |
|------|------|----------|
| `backup_db.sh` | 数据库备份 | 每天凌晨2点 |
| `cleanup_logs.sh` | 日志清理 | 每天凌晨3点 |
| `monitor.sh` | 系统监控 | 每5分钟 |

## 快速部署

```bash
# 1. 复制脚本到服务器
scp -r scripts/ root@your-server:/opt/dorm-power/

# 2. 设置执行权限
chmod +x /opt/dorm-power/scripts/*.sh

# 3. 安装定时任务
crontab /opt/dorm-power/scripts/crontab

# 4. 验证安装
crontab -l
```

## 手动执行

```bash
# 数据库备份
/opt/dorm-power/scripts/backup_db.sh

# 日志清理
/opt/dorm-power/scripts/cleanup_logs.sh

# 系统监控
/opt/dorm-power/scripts/monitor.sh
```

## 配置告警

### Server酱（推荐）

1. 访问 https://sct.ftqq.com/ 获取SendKey
2. 修改 `monitor.sh` 中的 `ALERT_WEBHOOK`：
```bash
ALERT_WEBHOOK="https://sctapi.ftqq.com/YOUR_SEND_KEY.send"
```

### 钉钉机器人

1. 创建钉钉群机器人
2. 获取Webhook地址
3. 修改 `monitor.sh` 中的告警函数

## 监控内容

| 监控项 | 阈值 | 说明 |
|--------|------|------|
| 内存使用率 | 80% | 超过阈值发送告警 |
| 磁盘使用率 | 80% | 超过阈值发送告警 |
| PostgreSQL | - | 检查服务是否运行 |
| 后端服务 | - | 检查健康检查接口 |
| Nginx | - | 检查进程是否运行 |

## 备份策略

| 备份项 | 保留时间 | 存储位置 |
|--------|----------|----------|
| 数据库 | 3天 | /opt/dorm-power/backups/ |
| 应用日志 | 7天 | /opt/dorm-power/logs/ |
| 审计日志 | 7天 | /opt/dorm-power/logs/ |

## 日志文件

| 日志文件 | 说明 |
|----------|------|
| `/opt/dorm-power/logs/cron.log` | 定时任务执行日志 |
| `/opt/dorm-power/logs/monitor.log` | 监控告警日志 |

## 故障排查

### 查看定时任务日志
```bash
tail -f /opt/dorm-power/logs/cron.log
```

### 查看监控日志
```bash
tail -f /opt/dorm-power/logs/monitor.log
```

### 手动测试脚本
```bash
# 测试数据库备份
bash -x /opt/dorm-power/scripts/backup_db.sh

# 测试日志清理
bash -x /opt/dorm-power/scripts/cleanup_logs.sh

# 测试监控
bash -x /opt/dorm-power/scripts/monitor.sh
```

## 注意事项

1. 确保脚本有执行权限
2. 确保目录存在且有写入权限
3. 定期检查磁盘空间
4. 定期检查备份文件
5. 配置告警通知方式

## 资源消耗

| 脚本 | CPU | 内存 | 执行时间 |
|------|-----|------|----------|
| backup_db.sh | 低 | 低 | 约10秒 |
| cleanup_logs.sh | 低 | 低 | 约1秒 |
| monitor.sh | 低 | 低 | 约1秒 |

总资源消耗：几乎可忽略不计
