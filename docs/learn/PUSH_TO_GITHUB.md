# 🚀 立即部署 - 3 步完成

## ✅ 配置已完成

GitHub 用户名：**RONGX563647**  
仓库地址：**https://github.com/RONGX563647/dorm-power-console**  
部署后访问：**https://rongx563647.github.io/dorm-power-console/**

---

## 📦 部署步骤

### 步骤 1️⃣: 推送代码到 GitHub

打开终端（PowerShell 或 Git Bash），执行：

```bash
# 进入项目目录
cd e:\CODE\developed-project\dorm-power-console

# 初始化 Git（如果还未初始化）
git init

# 添加所有文件
git add .

# 提交
git commit -m "docs: 添加教学文档和 CI/CD 配置"

# 关联远程仓库（如果还未关联）
git remote add origin https://github.com/RONGX563647/dorm-power-console.git

# 推送到 main 分支
git branch -M main
git push -u origin main
```

**✅ 推送成功后，GitHub Actions 会自动触发部署！**

---

### 步骤 2️⃣: 查看部署进度

1. 访问：https://github.com/RONGX563647/dorm-power-console/actions
2. 找到 **"Deploy Teaching Docs to GitHub Pages"** 工作流
3. 点击正在运行的任务（通常显示为绿色或黄色圆点）
4. 查看详细日志

**等待时间**: 2-5 分钟

---

### 步骤 3️⃣: 访问部署后的文档

部署成功后，访问：

**📚 教学文档网站**: https://rongx563647.github.io/dorm-power-console/

**功能特性**:
- ✅ 响应式设计（支持手机和电脑）
- ✅ 深色/浅色模式切换
- ✅ 全文搜索
- ✅ 代码高亮和复制
- ✅ 目录导航

---

## 🔍 验证部署

### 检查部署状态

```bash
# 运行配置检查脚本
python docs/learn/check_github_pages.py
```

**预期输出**:
```
✅ 🎉 所有检查通过！配置正确！
ℹ️  
下一步操作：
1. 提交更改到 Git
2. 推送到 GitHub main 分支
3. 访问 GitHub Actions 查看部署进度
4. 部署完成后访问：https://rongx563647.github.io/dorm-power-console/
```

---

## ⚙️ 配置说明

### CI/CD 配置

**Workflow 文件**: `.github/workflows/github-pages-deploy.yml`

**触发条件**:
- 推送到 `main` 分支
- 且更改了 `docs/learn/` 目录

**部署流程**:
1. 检出代码
2. 安装 Python 和 MkDocs
3. 安装依赖
4. 构建静态网站
5. 部署到 GitHub Pages

---

### MkDocs 配置

**配置文件**: `docs/learn/mkdocs.yml`

**主题**: Material for MkDocs

**包含内容**:
- 首页
- 10 个模块教程（从 01 到 10）

---

## 📝 后续更新

每次修改文档后，只需：

```bash
# 1. 添加更改
git add docs/learn/

# 2. 提交
git commit -m "docs: 更新 XXX 内容"

# 3. 推送
git push origin main
```

**GitHub Actions 会自动重新部署！**

---

## ⚠️ 常见问题

### 1. 推送失败 - 权限错误

**错误**: `remote: Permission denied`

**解决**:
```bash
# 检查是否已配置 SSH 密钥
# 或使用 HTTPS 方式推送
git remote set-url origin https://github.com/RONGX563647/dorm-power-console.git
```

---

### 2. 部署失败 - GitHub Actions 错误

**检查步骤**:
1. 访问 GitHub Actions 页面
2. 查看失败的任务
3. 阅读错误日志
4. 根据错误信息修复

**常见错误**:
- `mkdocs.yml` 配置错误
- Python 依赖安装失败
- 权限配置错误

---

### 3. 部署后访问 404

**可能原因**:
- 部署还未完成（等待 5-10 分钟）
- 浏览器缓存（清除缓存或强制刷新）
- 仓库未启用 GitHub Pages

**解决方法**:
1. 访问 https://github.com/RONGX563647/dorm-power-console/settings/pages
2. 确保 "Source" 设置为 "GitHub Actions"
3. 等待几分钟后刷新页面

---

### 4. 文档不更新

**原因**: 浏览器缓存

**解决**:
- Windows: `Ctrl + F5` 强制刷新
- Mac: `Cmd + Shift + R` 强制刷新
- 或清除浏览器缓存

---

## 🎨 自定义配置

### 修改网站标题

编辑 `docs/learn/mkdocs.yml`:

```yaml
site_name: 您的网站标题
```

### 修改主题颜色

编辑 `docs/learn/mkdocs.yml`:

```yaml
theme:
  palette:
    - scheme: default
      primary: indigo  # 可改为：red, pink, purple, deep-purple, blue, etc.
      accent: indigo
```

### 添加新页面

1. 在 `docs/learn/` 目录创建新的 `.md` 文件
2. 在 `mkdocs.yml` 的 `nav` 部分添加导航
3. 提交并推送

---

## 📊 部署架构

```
推送代码到 GitHub
       ↓
GitHub Actions 触发
       ↓
安装 Python 和 MkDocs
       ↓
安装依赖 (requirements.txt)
       ↓
构建静态网站 (mkdocs build)
       ↓
上传到 GitHub Pages
       ↓
全球可访问 🌍
```

---

## 🎉 完成！

恭喜！您的教学文档已经部署到 GitHub Pages！

**访问地址**: https://rongx563647.github.io/dorm-power-console/

**分享您的文档**:
- 复制链接发送给同学或老师
- 添加到简历或项目介绍中
- 在 GitHub 仓库 README 中添加链接

---

## 📞 需要帮助？

**有用资源**:
- MkDocs Material 主题：https://squidfunk.github.io/mkdocs-material/
- GitHub Pages 文档：https://pages.github.com/
- GitHub Actions 文档：https://docs.github.com/en/actions

**检查工具**:
```bash
# 运行本地检查
python docs/learn/check_github_pages.py
```

---

**最后更新**: 2026-04-21  
**配置状态**: ✅ 已完成并验证通过
