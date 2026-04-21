# 项目脚本目录

本目录包含 DormPower 项目的各类脚本，按功能分类存放。

## 目录结构

```
scripts/
├── README.md                 # 本文件
├── dev/                      # 开发环境脚本
│   ├── start-low-memory.sh   # 低内存模式启动后端
│   └── jvm.options           # JVM配置参数
├── deploy/                   # 部署相关脚本
│   └── docker-entrypoint.sh  # Docker容器入口脚本
├── ops/                      # 运维监控脚本
│   ├── README.md             # 运维脚本使用说明
│   ├── backup_db.sh          # 数据库备份脚本
│   ├── cleanup_logs.sh       # 日志清理脚本
│   ├── monitor.sh            # 系统监控脚本
│   ├── crontab               # 定时任务配置
│   └── deploy_scripts.sh     # 运维脚本部署脚本
└── test/                     # 测试脚本
    └── stress_test.sh        # JUC压测脚本
```

## 使用说明

### 开发环境

```bash
# 低内存模式启动后端（适用于1核1G服务器）
cd scripts/dev
./start-low-memory.sh
```

### 部署

```bash
# Docker部署使用
cd scripts/deploy
# docker-entrypoint.sh 由Dockerfile调用
```

### 运维

```bash
# 查看运维脚本详细说明
cat scripts/ops/README.md

# 部署运维脚本到服务器
cd scripts/ops
./deploy_scripts.sh
```

### 测试

```bash
# 执行压力测试（需要安装wrk）
cd scripts/test
./stress_test.sh
```

## 注意事项

1. 所有脚本使用相对路径引用项目根目录
2. 生产环境部署前请检查脚本中的路径配置
3. 定时任务配置需要根据实际服务器环境调整
