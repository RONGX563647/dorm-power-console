#!/usr/bin/env python3
"""
AI客服Agent API测试脚本 - DeepSeek版本
"""

import requests
import json

BASE_URL = "http://localhost:8000"

def test_health():
    print("\n=== 测试健康检查 ===")
    response = requests.get(f"{BASE_URL}/api/agent/health")
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")
    return response.status_code == 200

def test_quick_match():
    print("\n=== 测试快速匹配 ===")
    test_cases = [
        "你好",
        "谢谢",
        "再见"
    ]
    
    for msg in test_cases:
        response = requests.post(
            f"{BASE_URL}/api/agent/quick",
            json={"message": msg}
        )
        print(f"消息: {msg}")
        print(f"响应: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")

def test_intent_recognition():
    print("\n=== 测试意图识别 ===")
    test_cases = [
        "打开A301房间的空调",
        "查询今天的用电量",
        "A302的电费账单",
        "有什么告警吗",
        "帮我关灯",
        "怎么使用这个系统"
    ]
    
    for msg in test_cases:
        response = requests.post(
            f"{BASE_URL}/api/agent/intent",
            json={"message": msg}
        )
        print(f"\n消息: {msg}")
        result = response.json()
        print(f"意图: {result.get('intent')}")
        print(f"置信度: {result.get('confidence')}")
        print(f"实体: {result.get('entities')}")
        print(f"需要LLM: {result.get('needLLM')}")
        print(f"API端点: {result.get('apiEndpoint')}")

def test_chat_with_deepseek():
    print("\n=== 测试DeepSeek智能对话 ===")
    test_cases = [
        "你好",
        "打开A301的空调",
        "查询A302今天的用电量",
        "这个系统有什么功能",
        "如何节省电费",
        "宿舍用电安全要注意什么"
    ]
    
    for msg in test_cases:
        print(f"\n用户: {msg}")
        try:
            response = requests.post(
                f"{BASE_URL}/api/agent/chat",
                json={"message": msg},
                headers={"X-User-Id": "test-user"},
                timeout=35
            )
            result = response.json()
            print(f"助手: {result.get('response')}")
        except requests.exceptions.Timeout:
            print("助手: [请求超时]")
        except Exception as e:
            print(f"助手: [错误: {e}]")

def test_deepseek_direct():
    """直接测试DeepSeek API"""
    print("\n=== 直接测试DeepSeek API ===")
    
    url = "https://api.deepseek.com/v1/chat/completions"
    headers = {
        "Content-Type": "application/json",
        "Authorization": "Bearer sk-9ae959c6e2f14426a593478848f5a60f"
    }
    data = {
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": "你是宿舍用电管理系统的智能客服助手。"},
            {"role": "user", "content": "你好，请介绍一下你自己"}
        ],
        "max_tokens": 100
    }
    
    try:
        response = requests.post(url, headers=headers, json=data, timeout=30)
        print(f"状态码: {response.status_code}")
        if response.status_code == 200:
            result = response.json()
            content = result.get('choices', [{}])[0].get('message', {}).get('content', '')
            print(f"DeepSeek响应: {content}")
        else:
            print(f"错误: {response.text}")
    except Exception as e:
        print(f"请求失败: {e}")

if __name__ == "__main__":
    print("AI客服Agent API测试 (DeepSeek)")
    print("=" * 50)
    
    print("\n选择测试模式:")
    print("1. 完整测试（需要启动后端服务）")
    print("2. 仅测试DeepSeek API连接")
    print("3. 测试意图识别")
    
    choice = input("\n请输入选项 (1/2/3): ").strip()
    
    if choice == "1":
        try:
            if test_health():
                test_quick_match()
                test_intent_recognition()
                test_chat_with_deepseek()
            else:
                print("服务未启动，请先启动后端服务")
        except requests.exceptions.ConnectionError:
            print("无法连接到服务器，请确保后端服务正在运行")
            print("启动命令: ./start-low-memory.sh")
    elif choice == "2":
        test_deepseek_direct()
    elif choice == "3":
        try:
            test_intent_recognition()
        except requests.exceptions.ConnectionError:
            print("无法连接到服务器")
    else:
        print("无效选项")
