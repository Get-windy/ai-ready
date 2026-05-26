#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready测试数据版本管理
支持数据版本的创建、切换和回滚
"""

import argparse
import json
import os
from datetime import datetime
from pathlib import Path

DATA_DIR = Path(r"I:\AI-Ready\tests\data")
VERSIONS_DIR = DATA_DIR / "versions"
HISTORY_DIR = DATA_DIR / "_history"

class TestDataVersionManager:
    """测试数据版本管理器"""
    
    def __init__(self):
        VERSIONS_DIR.mkdir(parents=True, exist_ok=True)
        HISTORY_DIR.mkdir(parents=True, exist_ok=True)
    
    def create_version(self, version_name: str, tags: list = None):
        """创建数据版本"""
        version_dir = VERSIONS_DIR / version_name
        version_dir.mkdir(parents=True, exist_ok=True)
        
        # 创建版本信息
        version_info = {
            "version": version_name,
            "created_at": datetime.now().isoformat(),
            "tags": tags or [],
            "data_files": []
        }
        
        # 记录版本信息
        info_file = version_dir / "info.json"
        with open(info_file, 'w', encoding='utf-8') as f:
            json.dump(version_info, f, indent=2, ensure_ascii=False)
        
        print(f"已创建数据版本: {version_name}")
        print(f"版本信息: {info_file}")
        return version_info
    
    def archive_version(self, version_name: str):
        """归档数据版本"""
        version_dir = VERSIONS_DIR / version_name
        if not version_dir.exists():
            print(f"版本不存在: {version_name}")
            return None
        
        # 创建归档目录
        month_dir = HISTORY_DIR / datetime.now().strftime("%Y-%m")
        month_dir.mkdir(parents=True, exist_ok=True)
        
        # 移动版本目录
        archive_dir = month_dir / version_name
        version_dir.rename(archive_dir)
        
        print(f"已归档版本: {version_name} -> {archive_dir}")
        return str(archive_dir)
    
    def list_versions(self):
        """列出所有版本"""
        versions = []
        
        for version_dir in VERSIONS_DIR.iterdir():
            if version_dir.is_dir() and (version_dir / "info.json").exists():
                with open(version_dir / "info.json", 'r', encoding='utf-8') as f:
                    info = json.load(f)
                    versions.append(info)
        
        # 从归档中读取
        for month_dir in HISTORY_DIR.iterdir():
            if month_dir.is_dir():
                for version_dir in month_dir.iterdir():
                    if version_dir.is_dir() and (version_dir / "info.json").exists():
                        with open(version_dir / "info.json", 'r', encoding='utf-8') as f:
                            info = json.load(f)
                            info['archived'] = True
                            info['archive_path'] = str(version_dir)
                            versions.append(info)
        
        return versions
    
    def restore_version(self, version_name: str):
        """恢复数据版本"""
        version_dir = VERSIONS_DIR / version_name
        if not version_dir.exists():
            # 尝试从归档恢复
            for month_dir in HISTORY_DIR.iterdir():
                if month_dir.is_dir():
                    target_dir = month_dir / version_name
                    if target_dir.exists():
                        target_dir.rename(VERSIONS_DIR / version_name)
                        version_dir = VERSIONS_DIR / version_name
                        break
        
        if not version_dir.exists():
            print(f"版本不存在: {version_name}")
            return None
        
        # 恢复数据
        info_file = version_dir / "info.json"
        with open(info_file, 'r', encoding='utf-8') as f:
            info = json.load(f)
        
        print(f"已恢复版本: {version_name}")
        print(f"版本信息: {version_info}")
        return info

def main():
    parser = argparse.ArgumentParser(description='AI-Ready测试数据版本管理')
    parser.add_argument('command', choices=['create', 'archive', 'list', 'restore'],
                       help='命令: create/archive/list/restore')
    parser.add_argument('--version', help='版本名称')
    parser.add_argument('--tags', nargs='*', help='标签')
    
    args = parser.parse_args()
    
    manager = TestDataVersionManager()
    
    if args.command == 'create':
        if not args.version:
            print("错误: --version 参数必填")
            return
        manager.create_version(args.version, args.tags)
    
    elif args.command == 'archive':
        if not args.version:
            print("错误: --version 参数必填")
            return
        manager.archive_version(args.version)
    
    elif args.command == 'list':
        versions = manager.list_versions()
        print("\n数据版本列表:")
        print("-" * 80)
        for v in versions:
            status = "[归档]" if v.get('archived') else "[当前]"
            tags = " ".join(v.get('tags', []))
            print(f"{status} {v['version']} | {v['created_at']} | {tags}")
    
    elif args.command == 'restore':
        if not args.version:
            print("错误: --version 参数必填")
            return
        manager.restore_version(args.version)

if __name__ == '__main__':
    main()
