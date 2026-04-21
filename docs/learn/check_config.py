#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GitHub Pages 配置检查脚本
自动检查和更新 mkdocs.yml 配置文件
"""

import os
import re
import sys

def check_mkdocs_config():
    """检查 mkdocs.yml 配置文件"""
    
    config_file = os.path.join(os.path.dirname(__file__), 'mkdocs.yml')
    
    if not os.path.exists(config_file):
        print(f"❌ 配置文件不存在：{config_file}")
        return False
    
    with open(config_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    issues = []
    warnings = []
    
    # 检查 site_url
    site_url_match = re.search(r'site_url:\s*(.+)', content)
    if site_url_match:
        site_url = site_url_match.group(1).strip()
        if 'your-github-username' in site_url:
            warnings.append(f"⚠️  site_url 使用默认值：{site_url}")
            warnings.append("   请修改为你的 GitHub 用户名")
    else:
        issues.append("❌ 缺少 site_url 配置")
    
    # 检查 repo_url
    repo_url_match = re.search(r'repo_url:\s*(.+)', content)
    if repo_url_match:
        repo_url = repo_url_match.group(1).strip()
        if 'your-github-username' in repo_url:
            warnings.append(f"⚠️  repo_url 使用默认值：{repo_url}")
            warnings.append("   请修改为你的 GitHub 仓库地址")
    else:
        issues.append("❌ 缺少 repo_url 配置")
    
    # 检查 repo_name
    repo_name_match = re.search(r'repo_name:\s*(.+)', content)
    if not repo_name_match:
        warnings.append("⚠️  缺少 repo_name 配置")
    
    # 检查 docs_dir
    docs_dir_match = re.search(r'docs_dir:\s*(.+)', content)
    if not docs_dir_match:
        warnings.append("⚠️  缺少 docs_dir 配置，将使用默认值")
    
    # 检查 site_dir
    site_dir_match = re.search(r'site_dir:\s*(.+)', content)
    if not site_dir_match:
        warnings.append("⚠️  缺少 site_dir 配置，将使用默认值")
    
    # 检查 theme
    theme_match = re.search(r'theme:\s*\n\s*name:\s*(.+)', content)
    if not theme_match:
        warnings.append("⚠️  未配置 theme，建议使用 material 主题")
    
    # 检查 navigation
    nav_match = re.search(r'nav:\s*\n', content)
    if not nav_match:
        warnings.append("⚠️  未配置 nav（导航），文档将自动包含所有页面")
    
    # 打印结果
    print("=" * 60)
    print("GitHub Pages 配置检查报告")
    print("=" * 60)
    print()
    
    if issues:
        print("【严重问题】")
        for issue in issues:
            print(f"  {issue}")
        print()
    
    if warnings:
        print("【警告】")
        for warning in warnings:
            print(f"  {warning}")
        print()
    
    if not issues and not warnings:
        print("✅ 配置检查通过！")
        print()
    
    # 提供修改建议
    if warnings:
        print("=" * 60)
        print("【修改建议】")
        print()
        print("1. 打开 docs/learn/mkdocs.yml 文件")
        print("2. 修改以下配置项：")
        print()
        print("   # 将 your-github-username 替换为你的 GitHub 用户名")
        print("   site_url: https://your-github-username.github.io/dorm-power-console")
        print()
        print("   # 将 your-github-username 和 repo-name 替换为实际值")
        print("   repo_name: dorm-power-console")
        print("   repo_url: https://github.com/your-github-username/dorm-power-console")
        print()
    
    print("=" * 60)
    print()
    
    return len(issues) == 0

def check_github_actions():
    """检查 GitHub Actions 工作流配置"""
    
    workflow_file = os.path.join(
        os.path.dirname(__file__),
        '..', '..', '.github', 'workflows', 'docs-deploy.yml'
    )
    
    if not os.path.exists(workflow_file):
        print(f"❌ GitHub Actions 工作流文件不存在：{workflow_file}")
        return False
    
    print("✅ GitHub Actions 工作流文件存在")
    print(f"   位置：{workflow_file}")
    print()
    
    return True

def check_requirements():
    """检查依赖文件"""
    
    requirements_file = os.path.join(os.path.dirname(__file__), 'requirements.txt')
    
    if not os.path.exists(requirements_file):
        print(f"❌ 依赖文件不存在：{requirements_file}")
        return False
    
    with open(requirements_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    required_packages = [
        'mkdocs',
        'mkdocs-material',
        'pymdown-extensions'
    ]
    
    missing = []
    for package in required_packages:
        if package.lower() not in content.lower():
            missing.append(package)
    
    if missing:
        print(f"⚠️  缺少必要的依赖包：{', '.join(missing)}")
        return False
    
    print("✅ 依赖文件检查通过")
    print(f"   位置：{requirements_file}")
    print()
    
    return True

def check_deployment_scripts():
    """检查部署脚本"""
    
    scripts = {
        'deploy.sh': 'Linux/Mac 部署脚本',
        'deploy.ps1': 'Windows PowerShell 部署脚本',
        'build.sh': 'Linux/Mac 构建脚本',
        'build.bat': 'Windows 批处理构建脚本'
    }
    
    print("【部署脚本检查】")
    for script, description in scripts.items():
        script_path = os.path.join(os.path.dirname(__file__), script)
        if os.path.exists(script_path):
            print(f"  ✅ {script} - {description}")
        else:
            print(f"  ❌ {script} - {description} (不存在)")
    print()

def main():
    """主函数"""
    
    print()
    print("🔍 开始检查 GitHub Pages 配置...")
    print()
    
    # 检查各项配置
    config_ok = check_mkdocs_config()
    actions_ok = check_github_actions()
    requirements_ok = check_requirements()
    check_deployment_scripts()
    
    # 总结
    print("=" * 60)
    print("【检查总结】")
    print()
    
    if config_ok and actions_ok and requirements_ok:
        print("✅ 所有检查通过！可以开始部署")
        print()
        print("下一步:")
        print("  1. 确认 mkdocs.yml 中的 site_url 和 repo_url 已修改")
        print("  2. 推送代码到 GitHub")
        print("  3. 在 GitHub Actions 中查看部署状态")
        print()
    else:
        print("⚠️  部分检查未通过，请先修复问题")
        print()
    
    print("=" * 60)
    print()
    
    return 0 if (config_ok and actions_ok and requirements_ok) else 1

if __name__ == '__main__':
    sys.exit(main())
