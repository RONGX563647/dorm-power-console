# 教学文档部署指南

本目录包含宿舍用电管理系统教学文档的 MkDocs 配置和部署脚本。

## 📦 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 本地开发

启动本地开发服务器（支持热重载）：

**Linux/Mac:**
```bash
./serve.sh
```

**Windows:**
```powershell
.\serve.ps1
```

访问 http://localhost:8000 预览文档。

### 3. 构建静态站点

**Linux/Mac:**
```bash
./build.sh
```

**Windows:**
```powershell
.\build.ps1
```

构建产物在 `site/` 目录。

### 4. 部署到 GitHub Pages

**Linux/Mac:**
```bash
./deploy.sh
```

**Windows:**
```powershell
.\deploy.ps1
```

## 🚀 GitHub Actions 自动部署

推送到 main 分支时会自动部署文档到 GitHub Pages。

工作流程文件：`.github/workflows/docs-deploy.yml`

## 📁 目录结构

```
docs/learn/
├── mkdocs.yml              # MkDocs 配置文件
├── index.md                # 首页
├── README.md               # 本文件
├── requirements.txt        # Python 依赖
├── .gitignore             # Git 忽略文件
├── stylesheets/
│   └── extra.css          # 自定义样式
├── 01-用户认证与权限管理模块.md
├── 02-设备管理模块.md
├── ...
└── deploy.sh / deploy.ps1  # 部署脚本
```

## 🎨 主题特性

- ✅ **Material Design** 风格
- ✅ **深色/浅色模式**切换
- ✅ **代码高亮**渲染
- ✅ **响应式**布局
- ✅ **实时搜索**
- ✅ **目录导航**
- ✅ **代码复制**按钮
- ✅ **Mermaid**图表支持

## 📝 编写规范

### 代码块

使用带语言标识的代码块：

````markdown
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```
````

### 提示框

```markdown
!!! note "提示"
    这是提示内容

!!! warning "警告"
    这是警告内容

!!! tip "技巧"
    这是技巧内容
```

### 表格

```markdown
| 列 1 | 列 2 | 列 3 |
|------|------|------|
| 内容 1 | 内容 2 | 内容 3 |
```

### 图表

使用 Mermaid 绘制流程图：

````markdown
```mermaid
graph LR
    A --> B
    B --> C
```
````

## 🔧 配置说明

### mkdocs.yml

主要配置项：

- `site_name`: 站点名称
- `theme`: 主题配置
- `markdown_extensions`: Markdown 扩展
- `plugins`: 插件配置
- `nav`: 导航配置

### extra.css

自定义样式包括：

- 代码块样式增强
- 表格样式优化
- 提示框样式美化
- 响应式布局调整

## 📊 统计与分析

可配置 Google Analytics：

```yaml
extra:
  analytics:
    provider: google
    property: G-XXXXXXXXXX
```

## 🐛 常见问题

### 1. 中文乱码

确保文件编码为 UTF-8。

### 2. 代码高亮失效

检查 `pymdownx.highlight` 配置。

### 3. 图表不显示

确保安装了 `pymdown-extensions` 并启用了 `superfences`。

### 4. 部署失败

检查 GitHub Pages 权限设置：
- Settings → Pages → Build and deployment
- Source 选择 "GitHub Actions"

## 📚 参考资源

- [MkDocs 官方文档](https://www.mkdocs.org/)
- [Material 主题文档](https://squidfunk.github.io/mkdocs-material/)
- [Markdown 语法](https://www.markdownguide.org/)
- [Mermaid 图表](https://mermaid.js.org/)

## 📞 反馈与支持

遇到问题请提交 Issue 或联系维护者。

---

**最后更新**: 2024-04-21  
**维护者**: 教学文档编写组
