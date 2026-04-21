#!/bin/bash

# GitHub Pages 部署脚本

set -e

echo "🔨 开始构建静态文档站点..."

# 1. 安装依赖
echo "📦 安装 Python 依赖..."
pip install -r requirements.txt

# 2. 构建站点
echo "🏗️  构建 MkDocs 站点..."
mkdocs build --clean --config-file mkdocs.yml

# 3. 部署到 GitHub Pages
echo "🚀 部署到 GitHub Pages..."
mkdocs gh-deploy --force --config-file mkdocs.yml

echo "✅ 部署完成！"
echo "📱 访问地址：https://your-github-username.github.io/dorm-power-console/"
