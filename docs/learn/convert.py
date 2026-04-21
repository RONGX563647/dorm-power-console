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
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
            background: #ffffff;
            color: #333;
            line-height: 1.6;
            display: flex;
            min-height: 100vh;
        }}
        
        /* 侧边导航 */
        .nav {{
            position: fixed;
            left: 0;
            top: 0;
            width: 280px;
            height: 100vh;
            background: #fff;
            border-right: 1px solid #eaeaea;
            padding: 30px 0;
            overflow-y: auto;
            z-index: 1000;
        }}
        
        .nav-header {{
            padding: 0 25px 20px;
            border-bottom: 1px solid #eaeaea;
            margin-bottom: 20px;
        }}
        
        .nav-header h3 {{
            font-size: 16px;
            font-weight: 600;
            color: #333;
            margin-bottom: 5px;
        }}
        
        .nav-header p {{
            font-size: 12px;
            color: #999;
        }}
        
        .nav-section {{
            margin-bottom: 25px;
        }}
        
        .nav-section-title {{
            padding: 8px 25px;
            font-size: 12px;
            font-weight: 600;
            color: #999;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }}
        
        .nav a {{
            display: block;
            padding: 10px 25px;
            color: #666;
            text-decoration: none;
            transition: all 0.2s;
            font-size: 14px;
            border-left: 3px solid transparent;
        }}
        
        .nav a:hover {{
            background: #fff5f0;
            color: #ff6b35;
        }}
        
        .nav a.active {{
            background: #fff5f0;
            color: #ff6b35;
            border-left-color: #ff6b35;
        }}
        
        /* 主内容区 */
        .content {{
            margin-left: 280px;
            padding: 40px 60px;
            max-width: 1200px;
            flex: 1;
        }}
        
        .markdown-body {{
            font-size: 16px;
            line-height: 1.8;
        }}
        
        /* 标题样式 */
        .markdown-body h1 {{
            font-size: 32px;
            font-weight: 600;
            color: #333;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 1px solid #eaeaea;
        }}
        
        .markdown-body h2 {{
            font-size: 24px;
            font-weight: 600;
            color: #333;
            margin: 40px 0 20px;
        }}
        
        .markdown-body h3 {{
            font-size: 18px;
            font-weight: 600;
            color: #333;
            margin: 30px 0 15px;
        }}
        
        .markdown-body p {{
            margin: 15px 0;
            color: #555;
        }}
        
        .markdown-body ul, .markdown-body ol {{
            margin: 15px 0;
            padding-left: 30px;
        }}
        
        .markdown-body li {{
            margin: 8px 0;
            color: #555;
        }}
        
        .markdown-body a {{
            color: #ff6b35;
            text-decoration: none;
        }}
        
        .markdown-body a:hover {{
            text-decoration: underline;
        }}
        
        /* 代码块样式 */
        .markdown-body pre {{
            margin: 20px 0;
            border-radius: 4px;
            overflow: hidden;
            background: #f6f8fa;
        }}
        
        .markdown-body code {{
            font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
            font-size: 14px;
            line-height: 1.5;
        }}
        
        .markdown-body :not(pre) > code {{
            background: #f6f8fa;
            padding: 2px 6px;
            border-radius: 3px;
        }}
        
        /* 表格样式 */
        .markdown-body table {{
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }}
        
        .markdown-body th, .markdown-body td {{
            padding: 12px;
            border: 1px solid #eaeaea;
            text-align: left;
        }}
        
        .markdown-body th {{
            background: #f6f8fa;
            font-weight: 600;
        }}
        
        /* 引用块 */
        .markdown-body blockquote {{
            margin: 20px 0;
            padding: 15px 20px;
            border-left: 4px solid #ff6b35;
            background: #fff5f0;
            color: #666;
        }}
        
        /* 分割线 */
        .markdown-body hr {{
            height: 1px;
            border: none;
            background: #eaeaea;
            margin: 40px 0;
        }}
        
        /* 返回顶部 */
        .back-to-top {{
            position: fixed;
            bottom: 30px;
            right: 30px;
            background: #ff6b35;
            color: #fff;
            width: 45px;
            height: 45px;
            border-radius: 50%;
            text-align: center;
            line-height: 45px;
            text-decoration: none;
            display: none;
            z-index: 999;
            box-shadow: 0 2px 8px rgba(255, 107, 53, 0.3);
            transition: all 0.3s;
        }}
        
        .back-to-top:hover {{
            background: #ff5722;
            transform: translateY(-3px);
        }}
        
        /* 响应式 */
        @media (max-width: 768px) {{
            .nav {{
                width: 100%;
                height: auto;
                position: relative;
                border-right: none;
                border-bottom: 1px solid #eaeaea;
            }}
            
            .content {{
                margin-left: 0;
                padding: 30px 20px;
            }}
        }}
    </style>
</head>
<body>
    <!-- 侧边导航 -->
    <nav class="nav">
        <div class="nav-header">
            <h3>宿舍用电管理系统</h3>
            <p>教学文档</p>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">导航</div>
            <a href="index.html">首页</a>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">模块教程</div>
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
        </div>
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
