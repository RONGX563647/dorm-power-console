#!/usr/bin/env python3
"""
前后端API联调测试脚本
验证前端API调用与后端接口的一致性
"""

import requests
import json
import time

BASE_URL = "http://localhost:8000"

def test_api_connection():
    """测试API连接"""
    print("=" * 60)
    print("测试API连接")
    print("=" * 60)
    
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=5)
        if response.status_code == 200:
            print(f"✅ 健康检查通过: {response.json()}")
            return True
        else:
            print(f"❌ 健康检查失败: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 连接失败: {e}")
        return False

def test_auth_api():
    """测试认证API"""
    print("\n" + "=" * 60)
    print("测试认证API")
    print("=" * 60)
    
    # 测试登录
    try:
        response = requests.post(
            f"{BASE_URL}/api/auth/login",
            json={"account": "admin", "password": "admin123"},
            timeout=5
        )
        if response.status_code == 200:
            data = response.json()
            token = data.get("token")
            print(f"✅ 登录成功")
            print(f"   用户: {data.get('user', {}).get('username')}")
            print(f"   Token: {token[:20]}..." if token else "   Token: None")
            return token
        else:
            print(f"❌ 登录失败: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ 登录异常: {e}")
        return None

def test_devices_api(token):
    """测试设备API"""
    print("\n" + "=" * 60)
    print("测试设备API")
    print("=" * 60)
    
    if not token:
        print("❌ 没有有效的token，跳过设备API测试")
        return None
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # 测试获取设备列表
    try:
        response = requests.get(
            f"{BASE_URL}/api/devices",
            headers=headers,
            timeout=5
        )
        if response.status_code == 200:
            devices = response.json()
            print(f"✅ 获取设备列表成功，共 {len(devices)} 个设备")
            if devices:
                device_id = devices[0].get("id")
                print(f"   第一个设备ID: {device_id}")
                return device_id
            else:
                print("⚠️  没有找到设备")
                return None
        else:
            print(f"❌ 获取设备列表失败: {response.status_code}")
            return None
    except Exception as e:
        print(f"❌ 获取设备列表异常: {e}")
        return None

def test_device_status_api(token, device_id):
    """测试设备状态API"""
    print("\n" + "=" * 60)
    print("测试设备状态API")
    print("=" * 60)
    
    if not token or not device_id:
        print("❌ 缺少必要的参数，跳过设备状态API测试")
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    try:
        response = requests.get(
            f"{BASE_URL}/api/devices/{device_id}/status",
            headers=headers,
            timeout=5
        )
        if response.status_code == 200:
            status = response.json()
            print(f"✅ 获取设备状态成功")
            print(f"   在线状态: {status.get('online')}")
            print(f"   总功率: {status.get('total_power_w')} W")
        else:
            print(f"❌ 获取设备状态失败: {response.status_code}")
    except Exception as e:
        print(f"❌ 获取设备状态异常: {e}")

def test_telemetry_api(token, device_id):
    """测试遥测数据API"""
    print("\n" + "=" * 60)
    print("测试遥测数据API")
    print("=" * 60)
    
    if not token or not device_id:
        print("❌ 缺少必要的参数，跳过遥测数据API测试")
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    try:
        response = requests.get(
            f"{BASE_URL}/api/telemetry?device={device_id}&range=60s",
            headers=headers,
            timeout=5
        )
        if response.status_code == 200:
            telemetry = response.json()
            print(f"✅ 获取遥测数据成功，共 {len(telemetry)} 个数据点")
            if telemetry:
                print(f"   最新功率: {telemetry[-1].get('power_w')} W")
                print(f"   时间戳: {telemetry[-1].get('ts')}")
        else:
            print(f"❌ 获取遥测数据失败: {response.status_code}")
    except Exception as e:
        print(f"❌ 获取遥测数据异常: {e}")

def test_command_api(token, device_id):
    """测试命令API"""
    print("\n" + "=" * 60)
    print("测试命令API")
    print("=" * 60)
    
    if not token or not device_id:
        print("❌ 缺少必要的参数，跳过命令API测试")
        return
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # 测试发送命令
    try:
        response = requests.post(
            f"{BASE_URL}/api/strips/{device_id}/cmd",
            headers=headers,
            json={"action": "toggle", "socket": 1},
            timeout=5
        )
        if response.status_code == 200:
            data = response.json()
            cmd_id = data.get("cmdId")
            print(f"✅ 发送命令成功")
            print(f"   命令ID: {cmd_id}")
            
            # 测试查询命令状态
            if cmd_id:
                time.sleep(0.5)
                try:
                    response = requests.get(
                        f"{BASE_URL}/api/cmd/{cmd_id}",
                        headers=headers,
                        timeout=5
                    )
                    if response.status_code == 200:
                        cmd_status = response.json()
                        print(f"✅ 查询命令状态成功")
                        print(f"   命令状态: {cmd_status.get('state')}")
                    else:
                        print(f"❌ 查询命令状态失败: {response.status_code}")
                except Exception as e:
                    print(f"❌ 查询命令状态异常: {e}")
        elif response.status_code == 409:
            print(f"⚠️  设备忙碌，命令发送失败 (409)")
        else:
            print(f"❌ 发送命令失败: {response.status_code}")
            print(f"   响应: {response.text}")
    except Exception as e:
        print(f"❌ 发送命令异常: {e}")

def main():
    """主函数"""
    print("\n" + "=" * 60)
    print("前后端API联调测试")
    print("=" * 60)
    print(f"后端地址: {BASE_URL}")
    print(f"前端地址: http://localhost:3000")
    
    # 测试API连接
    if not test_api_connection():
        print("\n❌ 无法连接到后端API，请确保后端服务正在运行")
        return
    
    # 测试认证API
    token = test_auth_api()
    if not token:
        print("\n❌ 认证失败，无法继续测试")
        return
    
    # 测试设备API
    device_id = test_devices_api(token)
    
    # 测试设备状态API
    if device_id:
        test_device_status_api(token, device_id)
    
    # 测试遥测数据API
    if device_id:
        test_telemetry_api(token, device_id)
    
    # 测试命令API
    if device_id:
        test_command_api(token, device_id)
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)
    print("\n✅ 前后端API联调测试完成")
    print("📝 请在浏览器中访问 http://localhost:3000 查看前端界面")

if __name__ == "__main__":
    main()