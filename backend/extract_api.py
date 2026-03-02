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
        
        class_mapping = ''
        class_match = re.search(r'@RequestMapping\("([^"]+)"\)', content)
        if class_match:
            class_mapping = class_match.group(1)
        
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
    
    by_controller = defaultdict(list)
    for api in apis:
        by_controller[api['controller']].append(api)
    
    by_method = defaultdict(int)
    for api in apis:
        by_method[api['method']] += 1
    
    import json
    with open('/Users/rongx/Desktop/Code/git/dorm/doc/api_data.json', 'w', encoding='utf-8') as f:
        json.dump({
            'apis': apis,
            'by_controller': dict(by_controller),
            'by_method': dict(by_method)
        }, f, ensure_ascii=False, indent=2)
    
    print(f'API数据已保存到 api_data.json, 共 {len(apis)} 个端点')

if __name__ == '__main__':
    main()
