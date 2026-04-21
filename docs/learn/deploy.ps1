# GitHub Pages 部署脚本 (Windows PowerShell)

Write-Host "🔨 开始构建静态文档站点..." -ForegroundColor Green

# 1. 安装依赖
Write-Host "📦 安装 Python 依赖..." -ForegroundColor Cyan
pip install -r requirements.txt

# 2. 构建站点
Write-Host "🏗️  构建 MkDocs 站点..." -ForegroundColor Cyan
mkdocs build --clean --config-file mkdocs.yml

# 3. 部署到 GitHub Pages
Write-Host "🚀 部署到 GitHub Pages..." -ForegroundColor Cyan
mkdocs gh-deploy --force --config-file mkdocs.yml

Write-Host "✅ 部署完成！" -ForegroundColor Green
Write-Host "📱 访问地址：https://your-github-username.github.io/dorm-power-console/" -ForegroundColor Yellow
