#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将 Markdown 转换为 HTML
用于 GitHub Actions 自动部署
"""

import os
import re
from pathlib import Path
import markdown

# 配置
DOCS_DIR = Path(__file__).parent
OUTPUT_DIR = DOCS_DIR.parent / "html_docs"
OUTPUT_DIR.mkdir(exist_ok=True)

# HTML 模板
HTML_TEMPLATE = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{title} - 宿舍用电管理系统教学文档</title>
    <link href="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/github-markdown-css@5.2.0/github-markdown.min.css" rel="stylesheet">
    <style>
        body {{
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
            background: #f6f8fa;
        }}
        .markdown-body {{
            background: #fff;
            box-sizing: border-box;
            min-width: 200px;
            max-width: 100%;
            margin: 0 auto;
            padding: 45px;
            border-radius: 6px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }}
        @media (max-width: 768px) {{
            .markdown-body {{
                padding: 15px;
            }}
        }}
        /* 侧边导航 */
        .nav {{
            position: fixed;
            left: 0;
            top: 0;
            width: 250px;
            height: 100vh;
            background: #1e1e1e;
            color: #fff;
            padding: 20px 0;
            overflow-y: auto;
            z-index: 1000;
        }}
        .nav h3 {{
            margin: 0 0 20px 0;
            padding: 0 20px;
            font-size: 16px;
            border-bottom: 1px solid #333;
            padding-bottom: 15px;
        }}
        .nav a {{
            display: block;
            padding: 10px 20px;
            color: #ccc;
            text-decoration: none;
            transition: all 0.3s;
        }}
        .nav a:hover {{
            background: #333;
            color: #fff;
        }}
        .nav a.active {{
            background: #0366d6;
            color: #fff;
        }}
        .content {{
            margin-left: 250px;
            padding: 20px;
        }}
        /* 代码块样式 */
        pre {{
            border-radius: 6px;
            overflow: hidden;
            margin: 1em 0;
        }}
        pre[class*="language-"] {{
            font-size: 14px;
            line-height: 1.5;
        }}
        /* 返回顶部 */
        .back-to-top {{
            position: fixed;
            bottom: 30px;
            right: 30px;
            background: #0366d6;
            color: #fff;
            width: 50px;
            height: 50px;
            border-radius: 25px;
            text-align: center;
            line-height: 50px;
            text-decoration: none;
            display: none;
            z-index: 999;
        }}
        .back-to-top:hover {{
            background: #0255b3;
        }}
    </style>
</head>
<body>
    <!-- 侧边导航 -->
    <nav class="nav">
        <h3>📚 教学文档</h3>
        <a href="index.html">🏠 首页</a>
        <a href="01-用户认证与权限管理模块.html">01-用户认证与权限管理</a>
        <a href="02-设备管理模块.html">02-设备管理模块</a>
        <a href="03-数据采集与监控模块.html">03-数据采集与监控模块</a>
        <a href="04-计费管理模块.html">04-计费管理模块</a>
        <a href="05-告警管理模块.html">05-告警管理模块</a>
        <a href="06-控制管理模块.html">06-控制管理模块</a>
        <a href="07-宿舍管理模块.html">07-宿舍管理模块</a>
        <a href="08-系统管理模块.html">08-系统管理模块</a>
        <a href="09-AI 智能模块.html">09-AI 智能模块</a>
        <a href="10-通知管理模块.html">10-通知管理模块</a>
    </nav>
    
    <!-- 主内容区 -->
    <div class="content">
        <article class="markdown-body">
{content}
        </article>
    </div>
    
    <!-- 返回顶部按钮 -->
    <a href="#" class="back-to-top" id="backToTop">↑</a>
    
    <!-- Prism.js 代码高亮 -->
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/plugins/line-numbers/prism-line-numbers.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-java.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-typescript.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-javascript.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-python.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-bash.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-sql.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-yaml.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-json.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-vue.min.js"></script>
    
    <script>
        // 初始化 Prism 高亮
        Prism.highlightAll();
        
        // 返回顶部按钮
        const backToTop = document.getElementById('backToTop');
        window.addEventListener('scroll', () => {{
            if (window.scrollY > 300) {{
                backToTop.style.display = 'block';
            }} else {{
                backToTop.style.display = 'none';
            }}
        }});
        
        // 当前页面高亮
        const currentPage = window.location.pathname.split('/').pop();
        document.querySelectorAll('.nav a').forEach(link => {{
            const href = link.getAttribute('href');
            if (href === currentPage) {{
                link.classList.add('active');
            }}
        }});
    </script>
</body>
</html>
'''

def convert_markdown_to_html(md_file):
    """将 Markdown 文件转换为 HTML"""
    print(f"转换：{md_file.name}")
    
    # 读取 Markdown 文件
    with open(md_file, 'r', encoding='utf-8') as f:
        md_content = f.read()
    
    # 配置 Markdown 扩展
    md = markdown.Markdown(
        extensions=[
            'fenced_code',
            'codehilite',
            'toc',
            'tables',
            'admonition',
            'attr_list',
            'md_in_html',
        ]
    )
    
    # 转换为 HTML
    html_body = md.convert(md_content)
    
    # 生成标题
    title = md_file.stem.replace('-', ' ')
    
    # 填充模板
    html_content = HTML_TEMPLATE.format(title=title, content=html_body)
    
    # 保存 HTML 文件
    html_file = OUTPUT_DIR / f"{md_file.stem}.html"
    with open(html_file, 'w', encoding='utf-8') as f:
        f.write(html_content)
    
    print(f"[OK] {html_file.name}")
    return html_file

def main():
    print("=" * 60)
    print("Markdown 转 HTML 转换器")
    print("=" * 60)
    print()
    
    # 查找所有模块文档
    module_files = sorted(DOCS_DIR.glob("*.md"))
    module_files = [f for f in module_files if re.match(r'^\d{2}-.*\.md$', f.name)]
    
    if not module_files:
        print("[ERROR] 未找到模块文档文件")
        return
    
    print(f"找到 {len(module_files)} 个模块文档:")
    for f in module_files:
        print(f"  - {f.name}")
    print()
    
    # 转换每个文件
    for md_file in module_files:
        try:
            convert_markdown_to_html(md_file)
        except Exception as e:
            print(f"[ERROR] 转换失败 {md_file.name}: {e}")
            import traceback
            traceback.print_exc()
    
    print()
    print("=" * 60)
    print(f"[OK] 转换完成！输出目录：{OUTPUT_DIR}")
    print("=" * 60)

if __name__ == "__main__":
    main()
