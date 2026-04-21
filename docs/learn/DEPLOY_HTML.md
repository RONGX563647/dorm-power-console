# 部署 HTML 文档到 GitHub Pages

## ✅ 配置完成

您的教学文档现在将**直接转换为 HTML**并部署到 GitHub Pages！

---

## 🎯 配置说明

### 1. 转换流程

```
Markdown 文件 (.md)
    ↓
Python 脚本转换
    ↓
HTML 文件 (.html) + Prism.js 代码高亮
    ↓
GitHub Pages 部署
```

### 2. 使用的技术

- **Markdown 解析**: Python `markdown` 库
- **代码高亮**: [Prism.js](https://prismjs.com/) (业界标准，支持 100+ 语言)
- **页面样式**: GitHub Markdown CSS (与 GitHub 一致的渲染效果)
- **导航**: 固定侧边栏导航
- **响应式**: 支持手机、平板、电脑

---

## 🚀 部署步骤

### 方式一：自动部署（推荐）

```bash
# 1. 提交所有更改
cd e:\CODE\developed-project\dorm-power-console
git add .
git commit -m "docs: 添加 HTML 格式教学文档和部署配置"

# 2. 推送到 GitHub
git push origin main
```

**推送后**:
- GitHub Actions 会自动触发
- 转换所有 Markdown 为 HTML
- 部署到 GitHub Pages
- 等待 2-5 分钟

**访问地址**: https://rongx563647.github.io/dorm-power-console/html_docs/

---

### 方式二：手动部署

1. 访问 https://github.com/RONGX563647/dorm-power-console/actions/workflows/html-pages-deploy.yml
2. 点击 **"Run workflow"**
3. 选择分支（main）
4. 点击 **"Run workflow"** 按钮

---

## 📁 文件结构

```
docs/
├── learn/                          # Markdown 源文件
│   ├── 01-用户认证与权限管理模块.md
│   ├── 02-设备管理模块.md
│   ├── ...
│   ├── convert_to_html.py          # 转换脚本
│   └── generate_index.py           # 生成首页脚本
└── html_docs/                      # 生成的 HTML（本地测试用）
    ├── index.html                  # 首页
    ├── 01-用户认证与权限管理模块.html
    ├── 02-设备管理模块.html
    └── ...
```

**注意**: `html_docs/` 目录**不需要**提交到 Git，GitHub Actions 会自动生成。

---

## 🎨 页面特性

### 1. 代码高亮

使用 Prism.js，支持以下语言：
- ✅ Java
- ✅ TypeScript
- ✅ JavaScript
- ✅ Python
- ✅ Bash/Shell
- ✅ SQL
- ✅ YAML
- ✅ JSON
- ✅ Vue
- ✅ 以及 100+ 其他语言

**代码高亮主题**: Tomorrow Night（深色主题，保护视力）

### 2. 侧边导航

- 固定左侧导航栏
- 当前页面高亮
- 支持滚动
- 移动端自动隐藏

### 3. 响应式设计

- 桌面端：左右布局（导航 + 内容）
- 移动端：单列布局
- 自适应字体和间距

### 4. 返回顶部

- 滚动后显示
- 点击回到顶部
- 平滑动画

---

## 📊 与 MkDocs 方案对比

| 特性 | HTML 直出方案（当前） | MkDocs 方案（之前） |
|------|---------------------|-------------------|
| **代码高亮** | ✅ Prism.js（完美） | ❌ pymdownx 有 bug |
| **控制力** | ✅ 完全控制 HTML | ⚠️ 受框架限制 |
| **依赖** | ✅ 仅需 Python markdown | ⚠️ 需要 MkDocs + 插件 |
| **构建速度** | ✅ 快（10-20 秒） | ⚠️ 慢（30-60 秒） |
| **自定义** | ✅ 完全自定义 | ⚠️ 受主题限制 |
| **维护成本** | ✅ 低（纯 HTML） | ⚠️ 中（需维护配置） |

---

## 🔍 本地测试

### 1. 生成 HTML

```bash
cd e:\CODE\developed-project\dorm-power-console\docs\learn

# 转换所有模块文档
python convert_to_html.py

# 生成首页
python generate_index.py
```

### 2. 预览

**方式 1**: 直接用浏览器打开
```
file:///e:/CODE/developed-project/dorm-power-console/docs/html_docs/index.html
```

**方式 2**: 使用 HTTP 服务器
```bash
cd e:\CODE\developed-project\dorm-power-console\docs\html_docs
python -m http.server 8000
```

然后访问：http://localhost:8000

---

## ⚠️ 常见问题

### Q1: 代码格式不对

**A**: 现在使用 Prism.js，代码格式完美！支持：
- 行号显示
- 语法高亮
- 代码复制（需要添加插件）
- 语言标识自动识别

### Q2: 导航不工作

**A**: 检查文件名是否正确：
- 所有 HTML 文件必须在 `html_docs/` 目录
- 文件名必须匹配（如 `01-用户认证与权限管理模块.html`）

### Q3: 样式不显示

**A**: 检查：
- CDN 链接是否可访问
- 浏览器是否阻止了外部资源
- 尝试清除浏览器缓存

### Q4: 部署后 404

**A**: 等待 2-5 分钟，GitHub Pages 需要时间构建。
访问：https://github.com/RONGX563647/dorm-power-console/actions 查看进度。

---

## 🎯 下一步

### 1. 推送到 GitHub

```bash
git add .
git commit -m "docs: 添加 HTML 格式教学文档"
git push origin main
```

### 2. 配置 GitHub Pages

1. 访问 https://github.com/RONGX563647/dorm-power-console/settings/pages
2. **Source** 选择 "GitHub Actions"
3. 保存

### 3. 等待部署

- GitHub Actions 会自动运行
- 大约 2-5 分钟
- 完成后会发送邮件通知

### 4. 访问网站

部署完成后访问：
```
https://rongx563647.github.io/dorm-power-console/html_docs/
```

---

## 📞 需要帮助？

### 资源链接

- **Prism.js 文档**: https://prismjs.com/
- **GitHub Pages 文档**: https://pages.github.com/
- **GitHub Actions 文档**: https://docs.github.com/en/actions

### 检查工具

```bash
# 本地生成 HTML
cd docs/learn
python convert_to_html.py
python generate_index.py

# 检查生成的文件
ls -la ../html_docs/
```

---

## ✅ 配置清单

- [x] 转换脚本 (`convert_to_html.py`)
- [x] 首页生成脚本 (`generate_index.py`)
- [x] GitHub Actions workflow (`html-pages-deploy.yml`)
- [x] 10 个模块的 HTML 文件
- [x] 首页 HTML
- [x] 本部署说明

---

**配置时间**: 2026-04-21  
**部署方案**: HTML 直出 + Prism.js 代码高亮  
**部署地址**: https://rongx563647.github.io/dorm-power-console/html_docs/  
**状态**: ✅ 已完成，可以推送
