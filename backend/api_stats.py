#!/usr/bin/env python3
import os
import re
from collections import defaultdict

def extract_apis(controller_dir):
    apis = []
    controller_files = [f for f in os.listdir(controller_dir) if f.endswith('.java')]
    
    for controller_file in controller_files:
        file_path = os.path.join(controller_dir, controller_file)
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 提取类级别的@RequestMapping
        class_mapping = ''
        class_match = re.search(r'@RequestMapping\("([^"]+)"\)', content)
        if class_match:
            class_mapping = class_match.group(1)
        
        # 提取方法级别的映射
        method_patterns = [
            (r'@GetMapping\("([^"]+)"\)', 'GET'),
            (r'@PostMapping\("([^"]+)"\)', 'POST'),
            (r'@PutMapping\("([^"]+)"\)', 'PUT'),
            (r'@DeleteMapping\("([^"]+)"\)', 'DELETE'),
            (r'@PatchMapping\("([^"]+)"\)', 'PATCH'),
        ]
        
        for pattern, method in method_patterns:
            matches = re.finditer(pattern, content)
            for match in matches:
                path = match.group(1)
                full_path = class_mapping + path
                apis.append({
                    'controller': controller_file.replace('.java', ''),
                    'method': method,
                    'path': full_path
                })
    
    return apis

def main():
    controller_dir = '/Users/rongx/Desktop/Code/git/dorm/backend/src/main/java/com/dormpower/controller'
    apis = extract_apis(controller_dir)
    
    # 按控制器分组统计
    by_controller = defaultdict(list)
    for api in apis:
        by_controller[api['controller']].append(api)
    
    # 按HTTP方法统计
    by_method = defaultdict(int)
    for api in apis:
        by_method[api['method']] += 1
    
    print("=" * 80)
    print("后端API统计报告")
    print("=" * 80)
    print(f"\n总计API端点数量: {len(apis)}\n")
    
    print("\n按HTTP方法统计:")
    print("-" * 40)
    for method in ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']:
        if method in by_method:
            print(f"{method:8} {by_method[method]:4} 个")
    
    print("\n\n按控制器统计:")
    print("-" * 80)
    for controller in sorted(by_controller.keys()):
        api_list = by_controller[controller]
        print(f"\n{controller} ({len(api_list)} 个端点):")
        for api in sorted(api_list, key=lambda x: x['path']):
            print(f"  {api['method']:6} {api['path']}")
    
    print("\n" + "=" * 80)
    print(f"控制器总数: {len(by_controller)}")
    print(f"API端点总数: {len(apis)}")
    print("=" * 80)

if __name__ == '__main__':
    main()
