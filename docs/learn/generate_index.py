#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成首页 HTML
"""

from pathlib import Path

DOCS_DIR = Path(__file__).parent
OUTPUT_DIR = DOCS_DIR.parent / "html_docs"
OUTPUT_DIR.mkdir(exist_ok=True)

INDEX_HTML = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>宿舍用电管理系统 - 教学文档</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
            background: #ffffff;
            color: #333;
            line-height: 1.6;
            display: flex;
            min-height: 100vh;
        }
        
        .nav {
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
        }
        
        .nav-header {
            padding: 0 25px 20px;
            border-bottom: 1px solid #eaeaea;
            margin-bottom: 20px;
        }
        
        .nav-header h3 {
            font-size: 16px;
            font-weight: 600;
            color: #333;
            margin-bottom: 5px;
        }
        
        .nav-header p {
            font-size: 12px;
            color: #999;
        }
        
        .nav-section {
            margin-bottom: 25px;
        }
        
        .nav-section-title {
            padding: 8px 25px;
            font-size: 12px;
            font-weight: 600;
            color: #999;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        
        .nav a {
            display: block;
            padding: 10px 25px;
            color: #666;
            text-decoration: none;
            transition: all 0.2s;
            font-size: 14px;
            border-left: 3px solid transparent;
        }
        
        .nav a:hover {
            background: #fff5f0;
            color: #ff6b35;
        }
        
        .nav a.active {
            background: #fff5f0;
            color: #ff6b35;
            border-left-color: #ff6b35;
        }
        
        .content {
            margin-left: 280px;
            padding: 40px 60px;
            max-width: 1200px;
            flex: 1;
        }
        
        .markdown-body {
            font-size: 16px;
            line-height: 1.8;
        }
        
        h1 {
            font-size: 32px;
            font-weight: 600;
            color: #333;
            margin-bottom: 20px;
            padding-bottom: 15px;
            border-bottom: 1px solid #eaeaea;
        }
        
        h2 {
            font-size: 24px;
            font-weight: 600;
            color: #333;
            margin: 40px 0 20px;
        }
        
        h3 {
            font-size: 18px;
            font-weight: 600;
            color: #333;
            margin: 30px 0 15px;
        }
        
        p {
            margin: 15px 0;
            color: #555;
        }
        
        ul, ol {
            margin: 15px 0;
            padding-left: 30px;
        }
        
        li {
            margin: 8px 0;
            color: #555;
        }
        
        a {
            color: #ff6b35;
            text-decoration: none;
        }
        
        a:hover {
            text-decoration: underline;
        }
        
        .module-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 20px;
            margin: 30px 0;
        }
        
        .module-card {
            background: #fff;
            border: 1px solid #eaeaea;
            border-radius: 4px;
            padding: 25px;
            transition: all 0.3s;
        }
        
        .module-card:hover {
            box-shadow: 0 4px 12px rgba(0,0,0,0.08);
            transform: translateY(-2px);
            border-color: #ff6b35;
        }
        
        .module-card h3 {
            margin: 0 0 10px 0;
            color: #333;
            font-size: 18px;
        }
        
        .module-card .difficulty {
            display: inline-block;
            padding: 2px 8px;
            background: #f6f8fa;
            border-radius: 3px;
            font-size: 12px;
            color: #666;
        }
        
        .module-card .duration {
            display: inline-block;
            padding: 2px 8px;
            background: #f6f8fa;
            border-radius: 3px;
            font-size: 12px;
            color: #666;
            margin-left: 8px;
        }
        
        .module-card p {
            margin: 15px 0;
            font-size: 14px;
            color: #666;
        }
        
        .module-card a {
            display: inline-block;
            margin-top: 15px;
            color: #ff6b35;
            font-weight: 600;
            font-size: 14px;
        }
        
        .module-card a:hover {
            text-decoration: underline;
        }
        
        blockquote {
            margin: 20px 0;
            padding: 15px 20px;
            border-left: 4px solid #ff6b35;
            background: #fff5f0;
            color: #666;
        }
        
        hr {
            height: 1px;
            border: none;
            background: #eaeaea;
            margin: 40px 0;
        }
        
        pre {
            margin: 20px 0;
            border-radius: 4px;
            overflow: hidden;
            background: #f6f8fa;
        }
        
        code {
            font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
            font-size: 14px;
            line-height: 1.5;
        }
        
        :not(pre) > code {
            background: #f6f8fa;
            padding: 2px 6px;
            border-radius: 3px;
        }
        
        @media (max-width: 768px) {
            .nav {
                width: 100%;
                height: auto;
                position: relative;
                border-right: none;
                border-bottom: 1px solid #eaeaea;
            }
            
            .content {
                margin-left: 0;
                padding: 30px 20px;
            }
        }
    </style>
</head>
<body>
    <nav class="nav">
        <div class="nav-header">
            <h3>宿舍用电管理系统</h3>
            <p>教学文档</p>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">导航</div>
            <a href="index.html" class="active">首页</a>
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
    
    <div class="content">
        <article class="markdown-body">
            <h1>宿舍用电管理系统 - 项目系列学习文档</h1>
            
            <blockquote>
                <p>采用任务驱动式教学法，从零开始掌握企业级 Spring Boot + Vue 全栈开发</p>
            </blockquote>
            
            <hr>
            
            <h2>文档概述</h2>
            
            <p>本系列文档是宿舍用电管理系统（DormPower）的模块化学习教程，包含 10 个核心模块的完整开发教程。每个模块按照统一的开发流程组织，涵盖从后端设计到前端实现的完整开发过程。</p>
            
            <h3>教学特色</h3>
            
            <ul>
                <li><strong>目标导向</strong> - 每个模块明确列出知识目标、能力目标、成果目标</li>
                <li><strong>分步实操</strong> - 后端前端都按步骤分解，每步都有实操任务</li>
                <li><strong>完整代码</strong> - 提供完整可运行的代码示例，代码高亮渲染</li>
                <li><strong>文件结构</strong> - 清晰展示完成后的文件结构，便于对照检查</li>
                <li><strong>联调验证</strong> - 提供详细的功能验证清单，确保功能正常</li>
                <li><strong>扩展练习</strong> - 分层级的练习题目（基础/进阶/挑战）</li>
            </ul>
            
            <hr>
            
            <h2>模块导航</h2>
            
            <div class="module-grid">
                <div class="module-card">
                    <h3>01-用户认证与权限管理</h3>
                    <span class="difficulty">难度：三星</span>
                    <span class="duration">3-5 天</span>
                    <p>Spring Security, JWT, RBAC 权限模型</p>
                    <a href="01-用户认证与权限管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>02-设备管理模块</h3>
                    <span class="difficulty">难度：四星</span>
                    <span class="duration">4-5 天</span>
                    <p>IoT 设备、MQTT 协议、设备生命周期</p>
                    <a href="02-设备管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>03-数据采集与监控模块</h3>
                    <span class="difficulty">难度：四星</span>
                    <span class="duration">4-5 天</span>
                    <p>实时数据采集、时序数据库、监控</p>
                    <a href="03-数据采集与监控模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>04-计费管理模块</h3>
                    <span class="difficulty">难度：三星</span>
                    <span class="duration">3-4 天</span>
                    <p>电费计算、账单管理、支付集成</p>
                    <a href="04-计费管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>05-告警管理模块</h3>
                    <span class="difficulty">难度：三星</span>
                    <span class="duration">3-4 天</span>
                    <p>告警规则、通知推送、闭环管理</p>
                    <a href="05-告警管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>06-控制管理模块</h3>
                    <span class="difficulty">难度：四星</span>
                    <span class="duration">4-5 天</span>
                    <p>远程控制、MQTT 命令、设备响应</p>
                    <a href="06-控制管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>07-宿舍管理模块</h3>
                    <span class="difficulty">难度：二星</span>
                    <span class="duration">2-3 天</span>
                    <p>宿舍楼、房间、入住退宿管理</p>
                    <a href="07-宿舍管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>08-系统管理模块</h3>
                    <span class="difficulty">难度：三星</span>
                    <span class="duration">3-4 天</span>
                    <p>系统配置、日志管理、审计追踪</p>
                    <a href="08-系统管理模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>09-AI 智能模块</h3>
                    <span class="difficulty">难度：五星</span>
                    <span class="duration">4-5 天</span>
                    <p>AI 报告生成、智能问答、数据分析</p>
                    <a href="09-AI 智能模块.html">查看详情</a>
                </div>
                
                <div class="module-card">
                    <h3>10-通知管理模块</h3>
                    <span class="difficulty">难度：三星</span>
                    <span class="duration">3-4 天</span>
                    <p>多渠道通知、模板管理、推送策略</p>
                    <a href="10-通知管理模块.html">查看详情</a>
                </div>
            </div>
            
            <hr>
            
            <h2>快速开始</h2>
            
            <h3>学习前准备</h3>
            
            <p><strong>必需知识</strong>:</p>
            <ul>
                <li>Java 编程基础（类、接口、继承、泛型）</li>
                <li>数据库基础（SQL、表设计）</li>
                <li>HTML/CSS/JavaScript 基础</li>
            </ul>
            
            <p><strong>必需软件</strong>:</p>
            <pre><code class="language-bash">JDK 17+
Node.js 18+
Maven 3.8+
Git</code></pre>
            
            <hr>
            
            <h2>技术栈总览</h2>
            
            <ul>
                <li><strong>后端</strong>: Spring Boot 3.2, Spring Security, Spring Data JPA, Spring WebSocket</li>
                <li><strong>前端</strong>: Vue 3, TypeScript, Pinia, Vue Router, Element Plus, ECharts</li>
                <li><strong>数据库</strong>: PostgreSQL, H2 (开发环境)</li>
                <li><strong>消息队列</strong>: Mosquitto MQTT</li>
                <li><strong>部署</strong>: Docker, Docker Compose</li>
            </ul>
            
            <hr>
            
            <h2>反馈与支持</h2>
            
            <ul>
                <li><strong>GitHub 仓库</strong>: <a href="https://github.com/RONGX563647/dorm-power-console">https://github.com/RONGX563647/dorm-power-console</a></li>
                <li><strong>项目 Issues</strong>: <a href="https://github.com/RONGX563647/dorm-power-console/issues">GitHub Issues</a></li>
                <li><strong>讨论区</strong>: <a href="https://github.com/RONGX563647/dorm-power-console/discussions">GitHub Discussions</a></li>
            </ul>
            
            <hr>
            
            <p><strong>最后更新</strong>: 2024-04-21<br>
            <strong>文档版本</strong>: 1.0<br>
            <strong>维护状态</strong>: 持续更新中</p>
        </article>
    </div>
    
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/prism.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/prismjs@1.29.0/components/prism-bash.min.js"></script>
    <script>
        Prism.highlightAll();
    </script>
</body>
</html>
'''

# 保存首页
index_file = OUTPUT_DIR / "index.html"
with open(index_file, 'w', encoding='utf-8') as f:
    f.write(INDEX_HTML)

print(f"[OK] 首页已生成：{index_file}")
