# 🚀 GitHub Actions 自动部署说明

## ✅ 配置完成

您的教学文档现在将通过 **GitHub Actions** 自动部署到 GitHub Pages！

---

## 📦 部署流程

### 1. 自动触发

当您推送代码到 `main` 分支时，GitHub Actions 会自动：

```
推送代码
  ↓
触发 GitHub Actions
  ↓
安装 Python 和 Markdown 库
  ↓
转换 Markdown 为 HTML
  ↓
部署到 GitHub Pages
  ↓
访问：https://rongx563647.github.io/dorm-power-console/
```

---

## 🎯 立即部署

### 方式一：推送代码（推荐）

```bash
# 1. 进入项目目录
cd e:\CODE\developed-project\dorm-power-console

# 2. 添加所有更改
git add .

# 3. 提交
git commit -m "docs: 添加教学文档和 GitHub Actions 部署配置"

# 4. 推送到 GitHub
git push origin main
```

**推送后**:
- GitHub Actions 会自动触发
- 等待 2-5 分钟
- 访问：https://rongx563647.github.io/dorm-power-console/

---

### 方式二：手动触发

1. 访问：https://github.com/RONGX563647/dorm-power-console/actions/workflows/deploy-html-docs.yml
2. 点击 **"Run workflow"** 按钮
3. 选择分支（main）
4. 点击 **"Run workflow"**

---

## 📁 文件说明

### 已创建的文件

1. **转换脚本** (`docs/learn/convert.py`)
   - 将 Markdown 转换为 HTML
   - 使用 Prism.js 代码高亮
   - 添加侧边导航

2. **首页生成** (`docs/learn/generate_index.py`)
   - 生成精美的首页
   - 模块卡片展示
   - 响应式设计

3. **GitHub Actions** (`.github/workflows/deploy-html-docs.yml`)
   - 自动构建和部署
   - 监听 `docs/learn/` 目录
   - 部署到 GitHub Pages

---

## 🎨 页面特性

### 1. 代码高亮

使用 **Prism.js**（业界标准）：
- ✅ 支持 100+ 编程语言
- ✅ 深色主题（Tomorrow Night）
- ✅ 行号显示
- ✅ 语法高亮准确

### 2. 侧边导航

- 固定左侧导航栏
- 当前页面高亮
- 支持滚动
- 响应式设计

### 3. 页面布局

- 桌面端：左右布局（导航 + 内容）
- 移动端：单列布局
- 自适应字体和间距

### 4. 返回顶部

- 滚动后显示
- 点击回到顶部
- 平滑动画

---

## 📊 技术栈

| 组件 | 技术 | 说明 |
|------|------|------|
| **Markdown 解析** | Python `markdown` | 轻量级 Markdown 库 |
| **代码高亮** | Prism.js | 业界标准，支持 100+ 语言 |
| **页面样式** | GitHub Markdown CSS | 与 GitHub 一致的渲染 |
| **CI/CD** | GitHub Actions | 自动化部署 |
| **托管** | GitHub Pages | 免费静态网站托管 |

---

## ⚠️ 配置 GitHub Pages

首次部署需要配置：

1. 访问：https://github.com/RONGX563647/dorm-power-console/settings/pages

2. **Source** 选择：**GitHub Actions**

3. 保存配置

**注意**: 使用 GitHub Actions 部署时，不需要选择分支，它会自动使用构建产物。

---

## 🔍 查看部署状态

### 1. GitHub Actions

访问：https://github.com/RONGX563647/dorm-power-console/actions

**查看内容**:
- 工作流运行历史
- 构建日志
- 部署状态

### 2. GitHub Pages 设置

访问：https://github.com/RONGX563647/dorm-power-console/settings/pages

**查看内容**:
- 部署源（应为 GitHub Actions）
- 自定义域名（如有）
- HTTPS 证书状态

---

## 🔄 更新文档

每次修改文档后：

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

## 📞 常见问题

### Q1: 部署失败 - 权限错误

**错误**: `Permission denied` 或 `403 Forbidden`

**解决**:
1. 访问 https://github.com/RONGX563647/dorm-power-console/settings/pages
2. 确保 "Source" 设置为 "GitHub Actions"
3. 检查 workflow 文件的权限配置

---

### Q2: 文档不更新

**可能原因**:
- 缓存问题
- 构建失败

**解决**:
1. 手动触发部署（方式二）
2. 清除浏览器缓存
3. 检查 GitHub Actions 日志

---

### Q3: 部署后访问 404

**可能原因**:
- 部署还未完成
- GitHub Pages 配置错误

**解决**:
1. 等待 5-10 分钟
2. 检查 GitHub Pages 设置
3. 确认 workflow 运行成功

---

### Q4: 代码格式不对

**检查**:
1. 打开生成的 HTML 文件
2. 查看源代码中是否有 `<pre><code class="language-java">` 标签
3. 检查 Prism.js 是否正确加载

**解决**:
确保 Markdown 中的代码块指定了语言：
````markdown
```java
// Java 代码
public class Test {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```
````

---

## 📊 部署架构

```
推送代码到 GitHub
       ↓
GitHub Actions 触发
       ↓
安装 Python 和 Markdown
       ↓
转换 Markdown 为 HTML
       ↓
上传构建产物
       ↓
部署到 GitHub Pages
       ↓
全球可访问 🌍
```

---

## 🎉 访问地址

部署完成后访问：

**主地址**: https://rongx563647.github.io/dorm-power-console/

**直接访问特定页面**:
- 首页：https://rongx563647.github.io/dorm-power-console/index.html
- 模块 01: https://rongx563647.github.io/dorm-power-console/01-用户认证与权限管理模块.html
- 模块 02: https://rongx563647.github.io/dorm-power-console/02-设备管理模块.html
- ... 以此类推

---

## 📝 注意事项

1. **分支要求**: 只能从 `main` 分支部署
2. **路径限制**: 只监听 `docs/learn/` 目录
3. **部署时间**: 通常需要 2-5 分钟
4. **缓存**: GitHub 会缓存 Python 依赖，加快后续部署
5. **并发**: 同一时间只允许一个部署任务运行

---

## 🎯 下一步

### 立即部署

```bash
cd e:\CODE\developed-project\dorm-power-console
git add .
git commit -m "docs: 添加教学文档和 GitHub Actions 部署配置"
git push origin main
```

### 查看部署进度

访问：https://github.com/RONGX563647/dorm-power-console/actions

### 访问部署后的网站

等待 2-5 分钟后访问：
https://rongx563647.github.io/dorm-power-console/

---

**配置时间**: 2026-04-21  
**部署方案**: GitHub Actions 自动部署  
**部署地址**: https://rongx563647.github.io/dorm-power-console/  
**状态**: ✅ 已完成，可以推送
