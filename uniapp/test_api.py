#!/usr/bin/env python3
import requests
import json

BASE_URL = "http://localhost:8000"

def test_health_check():
    """测试健康检查接口"""
    print("=" * 60)
    print("测试1: 健康检查接口")
    print("=" * 60)
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=10)
        print(f"状态码: {response.status_code}")
        print(f"响应内容: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        return response.status_code == 200
    except Exception as e:
        print(f"❌ 健康检查失败: {e}")
        return False

def test_login():
    """测试登录接口"""
    print("\n" + "=" * 60)
    print("测试2: 登录接口")
    print("=" * 60)
    try:
        data = {
            "account": "admin",
            "password": "admin123"
        }
        response = requests.post(f"{BASE_URL}/api/auth/login", json=data, timeout=10)
        print(f"状态码: {response.status_code}")
        print(f"响应内容: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        
        if response.status_code == 200:
            result = response.json()
            if "code" in result:
                if result["code"] == 200:
                    token = result["data"].get("token", "")
                    print(f"✅ 登录成功，获取到Token")
                    return token
                else:
                    print(f"❌ 登录失败: {result.get('message', '未知错误')}")
            else:
                token = result.get("token", "")
                if token:
                    print(f"✅ 登录成功，获取到Token")
                    return token
                else:
                    print(f"❌ 登录响应中未找到token")
        return None
    except Exception as e:
        print(f"❌ 登录请求失败: {e}")
        return None

def test_devices_list(token):
    """测试获取设备列表接口"""
    print("\n" + "=" * 60)
    print("测试3: 获取设备列表")
    print("=" * 60)
    try:
        headers = {"Authorization": f"Bearer {token}"}
        response = requests.get(f"{BASE_URL}/api/devices", headers=headers, timeout=10)
        print(f"状态码: {response.status_code}")
        print(f"响应内容: {json.dumps(response.json(), indent=2, ensure_ascii=False)}")
        return response.status_code == 200
    except Exception as e:
        print(f"❌ 获取设备列表失败: {e}")
        return False

def main():
    print("\n🏠 宿舍用电管理系统 - API测试")
    print(f"后端地址: {BASE_URL}")
    print()
    
    all_passed = True
    
    if not test_health_check():
        all_passed = False
        print("\n❌ 后端服务未启动或无法连接！")
        print("请确保后端服务在 http://localhost:8000 上运行")
        return
    
    token = test_login()
    if not token:
        all_passed = False
    else:
        test_devices_list(token)
    
    print("\n" + "=" * 60)
    if all_passed:
        print("✅ 所有测试通过！")
    else:
        print("⚠️  部分测试失败，请检查")
    print("=" * 60)

if __name__ == "__main__":
    main()
