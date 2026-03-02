#!/usr/bin/env python3
"""
系统管理功能测试脚本
测试以下功能：
1. 系统配置管理
2. 日志管理
3. 数据备份与恢复
4. 通知系统
5. 监控系统
"""

import requests
import json
import time
from datetime import datetime

BASE_URL = "http://localhost:8000"

def get_token():
    """获取管理员token"""
    response = requests.post(f"{BASE_URL}/api/auth/login", json={
        "account": "admin",
        "password": "admin123"
    })
    if response.status_code == 200:
        return response.json().get("token")
    return None

def test_system_config_management(token):
    """测试系统配置管理"""
    print("\n=== 测试系统配置管理 ===")
    headers = {"Authorization": f"Bearer {token}"}
    
    # 获取所有配置
    print("1. 获取所有配置...")
    response = requests.get(f"{BASE_URL}/api/admin/config", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        configs = response.json()
        print(f"   配置数量: {len(configs)}")
    
    # 获取分类配置
    print("2. 获取email分类配置...")
    response = requests.get(f"{BASE_URL}/api/admin/config/category/email", headers=headers)
    print(f"   状态: {response.status_code}")
    
    # 获取单个配置
    print("3. 获取system.name配置...")
    response = requests.get(f"{BASE_URL}/api/admin/config/system.name", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        config = response.json()
        print(f"   配置值: {config.get('value')}")
    
    # 更新配置
    print("4. 更新配置...")
    response = requests.put(f"{BASE_URL}/api/admin/config/system.maintenance_mode", 
                           headers=headers,
                           json={"value": "false"})
    print(f"   状态: {response.status_code}")
    
    # 初始化默认配置
    print("5. 初始化默认配置...")
    response = requests.post(f"{BASE_URL}/api/admin/config/init", headers=headers)
    print(f"   状态: {response.status_code}")
    
    print("✓ 系统配置管理测试完成")

def test_log_management(token):
    """测试日志管理"""
    print("\n=== 测试日志管理 ===")
    headers = {"Authorization": f"Bearer {token}"}
    
    # 获取所有日志
    print("1. 获取所有日志...")
    response = requests.get(f"{BASE_URL}/api/admin/logs", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        logs = response.json()
        print(f"   日志数量: {logs.get('totalElements', 0)}")
    
    # 根据级别获取日志
    print("2. 获取ERROR级别日志...")
    response = requests.get(f"{BASE_URL}/api/admin/logs/level/ERROR", headers=headers)
    print(f"   状态: {response.status_code}")
    
    # 根据类型获取日志
    print("3. 获取AUTH类型日志...")
    response = requests.get(f"{BASE_URL}/api/admin/logs/type/AUTH", headers=headers)
    print(f"   状态: {response.status_code}")
    
    # 搜索日志
    print("4. 搜索日志...")
    response = requests.get(f"{BASE_URL}/api/admin/logs/search?keyword=login", headers=headers)
    print(f"   状态: {response.status_code}")
    
    # 获取日志统计
    print("5. 获取日志统计...")
    response = requests.get(f"{BASE_URL}/api/admin/logs/statistics?days=7", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        stats = response.json()
        print(f"   统计: {json.dumps(stats, indent=2)[:200]}...")
    
    print("✓ 日志管理测试完成")

def test_backup_management(token):
    """测试数据备份管理"""
    print("\n=== 测试数据备份管理 ===")
    headers = {"Authorization": f"Bearer {token}"}
    
    # 获取所有备份
    print("1. 获取所有备份...")
    response = requests.get(f"{BASE_URL}/api/admin/backup", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        backups = response.json()
        print(f"   备份数量: {len(backups)}")
    
    # 获取最近的备份
    print("2. 获取最近的备份...")
    response = requests.get(f"{BASE_URL}/api/admin/backup/recent", headers=headers)
    print(f"   状态: {response.status_code}")
    
    # 获取备份统计
    print("3. 获取备份统计...")
    response = requests.get(f"{BASE_URL}/api/admin/backup/statistics", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        stats = response.json()
        print(f"   统计: {json.dumps(stats, indent=2)}")
    
    print("✓ 数据备份管理测试完成")

def test_notification_system(token):
    """测试通知系统"""
    print("\n=== 测试通知系统 ===")
    headers = {"Authorization": f"Bearer {token}"}
    
    # 获取用户通知
    print("1. 获取用户通知...")
    response = requests.get(f"{BASE_URL}/api/notifications?username=admin", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        notifications = response.json()
        print(f"   通知数量: {notifications.get('totalElements', 0)}")
    
    # 获取未读通知
    print("2. 获取未读通知...")
    response = requests.get(f"{BASE_URL}/api/notifications/unread?username=admin", headers=headers)
    print(f"   状态: {response.status_code}")
    
    # 获取未读数量
    print("3. 获取未读数量...")
    response = requests.get(f"{BASE_URL}/api/notifications/unread/count?username=admin", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        count = response.json().get('count', 0)
        print(f"   未读数量: {count}")
    
    # 获取通知统计
    print("4. 获取通知统计...")
    response = requests.get(f"{BASE_URL}/api/notifications/statistics?username=admin", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        stats = response.json()
        print(f"   统计: {json.dumps(stats, indent=2)}")
    
    print("✓ 通知系统测试完成")

def test_monitoring_system(token):
    """测试监控系统"""
    print("\n=== 测试监控系统 ===")
    headers = {"Authorization": f"Bearer {token}"}
    
    # 获取系统状态
    print("1. 获取系统状态...")
    response = requests.get(f"{BASE_URL}/api/admin/monitor/system", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        status = response.json()
        print(f"   CPU核心数: {status.get('cpuCores')}")
        print(f"   内存使用: {status.get('memory', {}).get('usagePercent', 0):.2f}%")
        print(f"   运行时间: {status.get('uptimeFormatted')}")
    
    # 获取设备状态
    print("2. 获取设备状态...")
    response = requests.get(f"{BASE_URL}/api/admin/monitor/devices", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        status = response.json()
        print(f"   设备总数: {status.get('totalDevices')}")
        print(f"   在线设备: {status.get('onlineDevices')}")
        print(f"   在线率: {status.get('onlineRate', 0):.2f}%")
    
    # 获取API性能统计
    print("3. 获取API性能统计...")
    response = requests.get(f"{BASE_URL}/api/admin/monitor/api-performance?hours=24", headers=headers)
    print(f"   状态: {response.status_code}")
    if response.status_code == 200:
        stats = response.json()
        print(f"   平均响应时间: {stats.get('averageResponseTime', 0):.2f}ms")
        print(f"   最大响应时间: {stats.get('maxResponseTime', 0):.2f}ms")
    
    # 手动收集指标
    print("4. 手动收集指标...")
    response = requests.post(f"{BASE_URL}/api/admin/monitor/collect", headers=headers)
    print(f"   状态: {response.status_code}")
    
    print("✓ 监控系统测试完成")

def main():
    """主函数"""
    print("=" * 60)
    print("系统管理功能测试")
    print("=" * 60)
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"基础URL: {BASE_URL}")
    
    # 获取token
    print("\n获取管理员token...")
    token = get_token()
    if not token:
        print("✗ 获取token失败，请检查服务是否运行")
        return
    print(f"✓ 获取token成功")
    
    try:
        # 测试各个功能
        test_system_config_management(token)
        test_log_management(token)
        test_backup_management(token)
        test_notification_system(token)
        test_monitoring_system(token)
        
        print("\n" + "=" * 60)
        print("✓ 所有测试完成！")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n✗ 测试过程中发生错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
