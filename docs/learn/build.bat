@echo off
chcp 65001 >nul
echo 🔨 开始构建静态文档站点...

:: 1. 安装依赖
echo 📦 安装 Python 依赖...
pip install -r requirements.txt

:: 2. 构建站点
echo 🏗️  构建 MkDocs 站点...
mkdocs build --clean --config-file mkdocs.yml

echo ✅ 构建完成！
echo 📁 构建产物在 site/ 目录
