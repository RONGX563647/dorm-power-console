# 本地开发服务器启动脚本 (Windows PowerShell)

Write-Host "🚀 启动 MkDocs 本地开发服务器..." -ForegroundColor Green

# 检查依赖
try {
    $null = Get-Command mkdocs -ErrorAction Stop
} catch {
    Write-Host "❌ MkDocs 未安装，正在安装依赖..." -ForegroundColor Yellow
    pip install -r requirements.txt
}

# 启动服务器
Write-Host "🌐 访问地址：http://localhost:8000" -ForegroundColor Cyan
Write-Host "📝 实时预览：文件修改后自动刷新" -ForegroundColor Cyan
mkdocs serve --dev-addr=0.0.0.0:8000 --config-file mkdocs.yml
