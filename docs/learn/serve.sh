#!/bin/bash

# 本地开发服务器启动脚本

set -e

echo "🚀 启动 MkDocs 本地开发服务器..."

# 检查依赖
if ! command -v mkdocs &> /dev/null; then
    echo "❌ MkDocs 未安装，正在安装依赖..."
    pip install -r requirements.txt
fi

# 启动服务器
echo "🌐 访问地址：http://localhost:8000"
echo "📝 实时预览：文件修改后自动刷新"
mkdocs serve --dev-addr=0.0.0.0:8000 --config-file mkdocs.yml
