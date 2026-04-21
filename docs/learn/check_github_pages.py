#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GitHub Pages 部署配置检查脚本
检查所有配置是否正确
"""

import os
import sys
from pathlib import Path

# 颜色定义
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'
    BOLD = '\033[1m'

def print_header(text):
    print(f"\n{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.BLUE}{text:^60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.BLUE}{'='*60}{Colors.END}\n")

def print_success(text):
    print(f"{Colors.GREEN}✅ {text}{Colors.END}")

def print_error(text):
    print(f"{Colors.RED}❌ {text}{Colors.END}")

def print_warning(text):
    print(f"{Colors.YELLOW}⚠️  {text}{Colors.END}")

def print_info(text):
    print(f"{Colors.BLUE}ℹ️  {text}{Colors.END}")

def check_file_exists(file_path, description):
    """检查文件是否存在"""
    if os.path.exists(file_path):
        print_success(f"{description}: {file_path}")
        return True
    else:
        print_error(f"{description}不存在：{file_path}")
        return False

def check_content(file_path, patterns, description):
    """检查文件内容是否包含指定模式"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        all_found = True
        for pattern in patterns:
            if pattern not in content:
                print_error(f"{description}缺少配置：{pattern}")
                all_found = False
        
        if all_found:
            print_success(f"{description}配置正确")
        return all_found
    except Exception as e:
        print_error(f"读取 {description} 失败：{e}")
        return False

def main():
    print_header("GitHub Pages 部署配置检查")
    
    # 获取项目根目录
    project_root = Path(__file__).parent.parent.parent
    docs_dir = project_root / "docs" / "learn"
    github_dir = project_root / ".github" / "workflows"
    
    all_checks_passed = True
    
    # 检查 1: 检查必要文件是否存在
    print_info("检查必要文件...")
    required_files = [
        (docs_dir / "mkdocs.yml", "MkDocs 配置文件"),
        (docs_dir / "requirements.txt", "Python 依赖文件"),
        (github_dir / "github-pages-deploy.yml", "GitHub Actions 工作流"),
        (docs_dir / "index.md", "首页文档"),
    ]
    
    for file_path, description in required_files:
        if not check_file_exists(str(file_path), description):
            all_checks_passed = False
    
    # 检查 2: 检查 MkDocs 配置
    print_info("\n检查 MkDocs 配置...")
    mkdocs_patterns = [
        "site_url: https://rongx563647.github.io/dorm-power-console/",
        "repo_name: RONGX563647/dorm-power-console",
        "repo_url: https://github.com/RONGX563647/dorm-power-console",
        "theme:",
        "name: material",
    ]
    if not check_content(str(docs_dir / "mkdocs.yml"), mkdocs_patterns, "MkDocs 配置"):
        all_checks_passed = False
    
    # 检查 3: 检查 GitHub Actions 配置
    print_info("\n检查 GitHub Actions 配置...")
    workflow_patterns = [
        "name: Deploy Teaching Docs to GitHub Pages",
        "branches:",
        "  - main",
        "actions/deploy-pages",
    ]
    if not check_content(str(github_dir / "github-pages-deploy.yml"), workflow_patterns, "GitHub Actions 工作流"):
        all_checks_passed = False
    
    # 检查 4: 检查导航配置
    print_info("\n检查导航配置...")
    nav_patterns = [
        "nav:",
        "首页：index.md",
        "01-用户认证与权限管理模块.md",
    ]
    if not check_content(str(docs_dir / "mkdocs.yml"), nav_patterns, "导航配置"):
        all_checks_passed = False
    
    # 检查 5: 检查模块文档
    print_info("\n检查模块文档...")
    module_docs = [
        "01-用户认证与权限管理模块.md",
        "02-设备管理模块.md",
        "03-数据采集与监控模块.md",
        "04-计费管理模块.md",
        "05-告警管理模块.md",
        "06-控制管理模块.md",
        "07-宿舍管理模块.md",
        "08-系统管理模块.md",
        "09-AI 智能模块.md",
        "10-通知管理模块.md",
    ]
    
    for doc in module_docs:
        if not check_file_exists(str(docs_dir / doc), f"模块文档：{doc}"):
            all_checks_passed = False
    
    # 总结
    print_header("检查总结")
    
    if all_checks_passed:
        print_success("🎉 所有检查通过！配置正确！")
        print_info("\n下一步操作：")
        print("1. 提交更改到 Git")
        print("2. 推送到 GitHub main 分支")
        print("3. 访问 GitHub Actions 查看部署进度")
        print("4. 部署完成后访问：https://rongx563647.github.io/dorm-power-console/")
    else:
        print_error("❌ 部分检查未通过，请修复后再部署！")
        sys.exit(1)
    
    print("\n")

if __name__ == "__main__":
    main()
