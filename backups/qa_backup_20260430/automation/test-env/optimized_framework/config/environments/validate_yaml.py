#!/usr/bin/env python3
"""
验证YAML配置文件语法
"""

import yaml
import sys
import os

def validate_yaml_file(file_path):
    """验证YAML文件语法"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = yaml.safe_load(f)
        print(f"[OK] {file_path} - 语法正确")
        return True
    except yaml.YAMLError as e:
        print(f"[ERROR] {file_path} - YAML语法错误: {e}")
        return False
    except Exception as e:
        print(f"[ERROR] {file_path} - 读取错误: {e}")
        return False

def main():
    """主函数"""
    files_to_check = ['dev.yaml', 'staging.yaml', 'prod.yaml', 'test.yaml']
    
    print("开始验证YAML配置文件语法...")
    print("=" * 50)
    
    all_valid = True
    for filename in files_to_check:
        if os.path.exists(filename):
            if not validate_yaml_file(filename):
                all_valid = False
        else:
            print(f"[WARN] {filename} - 文件不存在")
    
    print("=" * 50)
    if all_valid:
        print("[OK] 所有配置文件语法验证通过")
    else:
        print("[ERROR] 存在语法错误的配置文件")
        sys.exit(1)

if __name__ == "__main__":
    main()