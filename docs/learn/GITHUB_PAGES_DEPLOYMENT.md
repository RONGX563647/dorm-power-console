# 📚 教学文档 GitHub Pages 部署指南

## ✅ 配置完成

您的教学文档已配置完成，可以通过 GitHub Actions 自动部署到 GitHub Pages！

---

## 🎯 配置概览

### GitHub 信息
- **GitHub 用户名**: RONGX563647
- **仓库地址**: https://github.com/RONGX563647/dorm-power-console
- **部署后访问**: https://rongx563647.github.io/dorm-power-console/

### CI/CD 配置
- **Workflow 文件**: `.github/workflows/github-pages-deploy.yml`
- **MkDocs 配置**: `docs/learn/mkdocs.yml`
- **文档目录**: `docs/learn/`

---

## 🚀 部署步骤

### 方式一：自动部署（推荐）

只需将代码推送到 GitHub 的 `main` 分支，GitHub Actions 会自动触发部署！

```bash
# 1. 进入项目目录
cd e:\CODE\developed-project\dorm-power-console

# 2. 添加所有更改
git add .

# 3. 提交更改
git commit -m "docs: 更新教学文档"

# 4. 推送到 main 分支
git push origin main
```

**推送成功后，GitHub Actions 会自动：**
1. 检出代码
2. 安装 Python 和 MkDocs 依赖
3. 构建静态网站
4. 部署到 GitHub Pages

**查看部署进度：**
1. 访问 https://github.com/RONGX563647/dorm-power-console/actions
2. 找到 "Deploy Teaching Docs to GitHub Pages" workflow
3. 点击正在运行的任务查看详情

---

### 方式二：手动部署

在 GitHub 仓库页面手动触发部署：

1. 访问 https://github.com/RONGX563647/dorm-power-console/actions/workflows/github-pages-deploy.yml
2. 点击右上角的 **"Run workflow"** 按钮
3. 选择分支（main）
4. 点击 **"Run workflow"**

---

## 📋 部署配置说明

### 1. Workflow 配置 (`.github/workflows/github-pages-deply.yml`)

```yaml
name: Deploy Teaching Docs to GitHub Pages

on:
  push:
    branches:
      - main
    paths:
      - 'docs/learn/**'           # 监听 docs/learn 目录
      - 'docs/learn/mkdocs.yml'   # 监听 MkDocs 配置
      - 'docs/learn/requirements.txt'  # 监听依赖
  workflow_dispatch:              # 支持手动触发
```

**触发条件：**
- 推送到 `main` 分支
- 且更改了 `docs/learn/` 目录下的文件
- 或手动触发

---

### 2. MkDocs 配置 (`docs/learn/mkdocs.yml`)

```yaml
site_name: 宿舍用电管理系统 - 教学文档
site_url: https://rongx563647.github.io/dorm-power-console/
repo_name: RONGX563647/dorm-power-console
repo_url: https://github.com/RONGX563647/dorm-power-console
```

**重要配置项：**
- `site_url`: 必须与 GitHub Pages 的访问地址一致
- `repo_name`: 仓库名称（用于编辑链接）
- `repo_url`: 仓库地址
- `edit_uri`: 编辑链接前缀

---

### 3. 导航配置

```yaml
nav:
  - 首页：index.md
  - 教学规划：教学规划.md
  - 快速开始：快速开始.md
  - 学习检查清单：学习检查清单.md
  - 教学文档总结：教学文档总结.md
  - 模块教程:
    - 01-用户认证与权限管理模块.md
    - 02-设备管理模块.md
    - 03-数据采集与监控模块.md
    - 04-计费管理模块.md
    - 05-告警管理模块.md
    - 06-控制管理模块.md
    - 07-宿舍管理模块.md
    - 08-系统管理模块.md
    - 09-AI 智能模块.md
    - 10-通知管理模块.md
```

---

## 🔍 验证部署

### 1. 检查部署状态

推送代码后，等待 2-5 分钟，然后：

1. 访问 https://github.com/RONGX563647/dorm-power-console/actions
2. 查看最近的工作流运行状态
3. 绿色 ✅ 表示部署成功

### 2. 访问文档

部署成功后，访问：

**主地址**: https://rongx563647.github.io/dorm-power-console/

**直接访问特定页面**:
- 首页：https://rongx563647.github.io/dorm-power-console/01-用户认证与权限管理模块/
- 教学规划：https://rongx563647.github.io/dorm-power-console/教学规划/

---

## ⚠️ 常见问题

### 1. 部署失败 - 权限错误

**错误信息**: `Permission denied` 或 `403 Forbidden`

**解决方案**:
1. 访问 https://github.com/RONGX563647/dorm-power-console/settings/pages
2. 确保 "Source" 设置为 "GitHub Actions"
3. 检查 workflow 文件的权限配置

---

### 2. 文档不更新

**可能原因**:
- 缓存问题
- 构建失败

**解决方案**:
1. 手动触发部署（方式二）
2. 清除浏览器缓存
3. 检查 GitHub Actions 日志

---

### 3. 404 错误

**可能原因**:
- 部署未完成
- 路径配置错误

**解决方案**:
1. 等待 5-10 分钟
2. 检查 `mkdocs.yml` 中的 `site_url` 配置
3. 确认仓库名称和 GitHub 用户名正确

---

### 4. 代码块渲染问题

**问题**: 代码块没有正确显示或没有语法高亮

**解决方案**:
确保 Markdown 文件中使用正确的代码块语法：

````markdown
```java
// Java 代码示例
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```
````

**支持的语言标识**:
- `java` - Java
- `typescript` - TypeScript
- `javascript` - JavaScript
- `python` - Python
- `bash` - Shell 命令
- `sql` - SQL
- `yaml` - YAML
- `json` - JSON

---

## 🎨 自定义样式

如果需要自定义样式，可以编辑：

- **CSS**: `docs/learn/assets/stylesheets/extra.css`
- **JavaScript**: `docs/learn/assets/javascripts/`
- **主题配置**: `docs/learn/mkdocs.yml` 中的 `theme` 部分

---

## 📊 部署流程图

```
推送代码到 GitHub
       ↓
GitHub Actions 触发
       ↓
检出代码
       ↓
安装 Python 和 MkDocs
       ↓
安装依赖 (requirements.txt)
       ↓
构建静态网站 (mkdocs build)
       ↓
上传构建产物
       ↓
部署到 GitHub Pages
       ↓
访问 https://rongx563647.github.io/dorm-power-console/
```

---

## 🔄 更新文档

每次修改 `docs/learn/` 目录下的文件后：

```bash
# 1. 添加更改
git add docs/learn/

# 2. 提交
git commit -m "docs: 更新 XXX 模块文档"

# 3. 推送
git push origin main
```

GitHub Actions 会自动重新部署！

---

## 📝 注意事项

1. **分支要求**: 只能从 `main` 分支部署
2. **路径限制**: 只监听 `docs/learn/` 目录
3. **部署时间**: 通常需要 2-5 分钟
4. **缓存**: GitHub 会缓存 Python 依赖，加快后续部署
5. **并发**: 同一时间只允许一个部署任务运行

---

## 🎉 完成！

配置完成后，您的教学文档将自动部署到 GitHub Pages，全球可访问！

**访问地址**: https://rongx563647.github.io/dorm-power-console/

---

## 📞 需要帮助？

如果遇到问题：
1. 检查 GitHub Actions 日志
2. 查看 MkDocs 官方文档：https://squidfunk.github.io/mkdocs-material/
3. 查看 GitHub Pages 文档：https://pages.github.com/
