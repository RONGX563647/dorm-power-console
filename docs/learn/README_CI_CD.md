# 🚀 GitHub Pages CI/CD 部署 - 快速开始

## ⚡ 5 分钟快速部署

### 第一步：修改配置（1 分钟）

打开 `docs/learn/mkdocs.yml` 文件，修改以下两处：

```yaml
# 第 4 行：修改为你的 GitHub 用户名
site_url: https://你的 GitHub 用户名.github.io/dorm-power-console

# 第 7-8 行：修改为你的 GitHub 仓库信息
repo_name: dorm-power-console
repo_url: https://github.com/你的 GitHub 用户名/dorm-power-console
```

**示例**（GitHub 用户名为 `zhangsan`）：

```yaml
site_url: https://zhangsan.github.io/dorm-power-console
repo_name: dorm-power-console
repo_url: https://github.com/zhangsan/dorm-power-console
```

### 第二步：推送代码到 GitHub（2 分钟）

```bash
# 1. 初始化 Git（如果还未初始化）
git init

# 2. 添加所有文件
git add .

# 3. 提交
git commit -m "docs: 添加教学文档和 CI/CD 配置"

# 4. 关联远程仓库（替换为你的仓库地址）
git remote add origin https://github.com/你的用户名/dorm-power-console.git

# 5. 推送到 main 分支
git branch -M main
git push -u origin main
```

### 第三步：配置 GitHub Pages（1 分钟）

1. 打开你的 GitHub 仓库
2. 进入 **Settings** → **Pages**
3. 在 **Build and deployment** 部分：
   - **Source**: 选择 **GitHub Actions** ✅

### 第四步：等待自动部署（1-2 分钟）

推送后 GitHub Actions 会自动触发：

1. 访问仓库的 **Actions** 标签
2. 查看 `Deploy Documentation to GitHub Pages` 工作流
3. 等待绿色的 ✅ 标记

### 第五步：访问部署的文档

部署成功后，访问：

```
https://你的 GitHub 用户名.github.io/dorm-power-console/
```

🎉 完成！

---

## 📋 详细配置说明

### 工作流触发条件

以下操作会自动触发部署：

| 操作 | 说明 |
|------|------|
| 推送到 main 分支 | 修改 `docs/learn/` 目录或 `mkdocs.yml` |
| 手动触发 | Actions → Deploy Documentation → Run workflow |
| Pull Request | 会构建但不会部署 |

### 部署流程

```mermaid
graph LR
    A[推送代码] --> B[GitHub Actions 触发]
    B --> C[安装 Python 3.11]
    C --> D[安装依赖]
    D --> E[构建 MkDocs 站点]
    E --> F[上传构建产物]
    F --> G[部署到 GitHub Pages]
    G --> H[部署完成]
```

### 工作流文件

位置：`.github/workflows/docs-deploy.yml`

```yaml
name: Deploy Documentation to GitHub Pages

on:
  push:
    branches:
      - main
    paths:
      - 'docs/learn/**'
      - 'mkdocs.yml'
  workflow_dispatch:  # 支持手动触发

permissions:
  contents: write
  pages: write
  id-token: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.11'
          cache: 'pip'
      - run: pip install -r requirements.txt
      - run: mkdocs build --clean --config-file mkdocs.yml
      
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/deploy-pages@v4
```

---

## 🛠️ 本地测试

### 方法 1: 使用部署脚本

**Windows PowerShell:**
```powershell
# 安装依赖
pip install -r requirements.txt

# 启动本地服务器
.\serve.ps1

# 访问 http://localhost:8000
```

**Linux/Mac:**
```bash
# 安装依赖
pip install -r requirements.txt

# 启动本地服务器
./serve.sh

# 访问 http://localhost:8000
```

### 方法 2: 使用 Python 脚本

```bash
# 运行配置检查
python check_config.py

# 本地构建测试
mkdocs build --clean --config-file mkdocs.yml
```

---

## 🔧 常见问题

### Q1: 部署失败，提示权限错误

**解决方案:**
1. Settings → Actions → General
2. Workflow permissions → Read and write permissions ✅

### Q2: 部署成功但访问 404

**解决方案:**
1. 等待 2-3 分钟（GitHub Pages 缓存）
2. 检查 `site_url` 配置是否正确
3. 清除浏览器缓存

### Q3: 样式丢失

**解决方案:**
1. 检查 `docs/learn/stylesheets/extra.css` 是否存在
2. 检查 `mkdocs.yml` 中的 `extra_css` 配置
3. 清除浏览器缓存

### Q4: 如何查看部署日志

**方法:**
1. GitHub 仓库 → Actions 标签
2. 选择对应的部署运行
3. 查看详细日志

---

## 📊 部署状态

| 检查项 | 状态 |
|--------|------|
| GitHub Actions 配置 | ✅ 已完成 |
| MkDocs 配置 | ✅ 已完成 |
| 依赖文件 | ✅ 已完成 |
| 部署脚本 | ✅ 已完成 |
| 配置检查脚本 | ✅ 已完成 |

---

## 🎯 下一步

### 自动部署

配置完成后，每次推送到 main 分支都会自动部署：

```bash
# 日常开发
git add .
git commit -m "docs: 更新模块 03 文档"
git push origin main

# 自动触发部署 ✅
```

### 版本发布

发布重要版本时：

```bash
# 打标签
git tag v1.0.0
git push origin v1.0.0

# 查看部署状态
# Actions → 查看工作流运行
```

### 自定义域名（可选）

1. 创建 `docs/learn/CNAME` 文件：
   ```
   docs.example.com
   ```

2. 配置 DNS CNAME 记录

3. 更新 `mkdocs.yml`:
   ```yaml
   site_url: https://docs.example.com
   ```

---

## 📚 参考文档

- [GITHUB_PAGES_GUIDE.md](GITHUB_PAGES_GUIDE.md) - 详细部署指南
- [DEPLOYMENT.md](DEPLOYMENT.md) - 本地部署指南
- [部署完成说明.md](部署完成说明.md) - 完成总结

---

## 🆘 获取帮助

遇到问题？

1. 查看 [GITHUB_PAGES_GUIDE.md](GITHUB_PAGES_GUIDE.md) 故障排除部分
2. 检查 Actions 日志
3. 运行 `python check_config.py` 检查配置

---

**最后更新**: 2024-04-21  
**状态**: ✅ 已就绪，可以部署
