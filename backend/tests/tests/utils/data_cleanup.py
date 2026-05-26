#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据清理工具

提供测试数据的清理功能
"""

import os
import glob
from pathlib import Path
from typing import List, Optional


class DataCleanup:
    """数据清理工具"""
    
    def __init__(self, base_dir: str = "data"):
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(exist_ok=True)
    
    def cleanup_files(self, pattern: str = "*.json", keep_recent: int = 5):
        """清理文件，保留最近N个"""
        files = sorted(
            self.base_dir.glob(pattern),
            key=lambda x: x.stat().st_mtime,
            reverse=True
        )
        
        # 保留最近的文件
        files_to_delete = files[keep_recent:]
        
        for file_path in files_to_delete:
            file_path.unlink()
            print(f"已删除: {file_path}")
        
        return len(files_to_delete)
    
    def cleanup_all(self, pattern: str = "*.json", confirm: bool = True):
        """清理所有匹配文件"""
        files = list(self.base_dir.glob(pattern))
        
        if not files:
            print("没有找到匹配的文件")
            return 0
        
        if confirm:
            response = input(f"确认删除 {len(files)} 个文件? (y/N): ")
            if response.lower() != 'y':
                print("操作已取消")
                return 0
        
        for file_path in files:
            file_path.unlink()
            print(f"已删除: {file_path}")
        
        return len(files)
    
    def cleanup_by_age(self, days: int = 7, pattern: str = "*.json"):
        """按年龄清理文件"""
        from datetime import datetime, timedelta
        
        cutoff_date = datetime.now() - timedelta(days=days)
        files = self.base_dir.glob(pattern)
        
        deleted_count = 0
        for file_path in files:
            file_mtime = datetime.fromtimestamp(file_path.stat().st_mtime)
            if file_mtime < cutoff_date:
                file_path.unlink()
                print(f"已删除: {file_path} (创建于 {file_mtime})")
                deleted_count += 1
        
        return deleted_count
    
    def cleanup_database(self, db_connection, tables: Optional[List[str]] = None):
        """清理数据库测试数据"""
        # 注意：此功能需要具体的数据库连接实现
        print("数据库清理功能需要具体的数据库连接配置")
        print("支持的表:", tables or ["users", "orders", "products", "invoices"])
    
    def cleanup_cache(self, cache_dir: str = ".cache"):
        """清理缓存文件"""
        cache_path = Path(cache_dir)
        if cache_path.exists():
            import shutil
            shutil.rmtree(cache_path)
            print(f"已清理缓存目录: {cache_path}")
    
    def cleanup_reports(self, keep_recent: int = 10):
        """清理测试报告"""
        reports_dir = Path("reports")
        if not reports_dir.exists():
            return 0
        
        # 清理HTML报告
        html_files = sorted(reports_dir.glob("*.html"), key=lambda x: x.stat().st_mtime, reverse=True)
        for file_path in html_files[keep_recent:]:
            file_path.unlink()
            print(f"已删除报告: {file_path}")
        
        # 清理JSON报告
        json_files = sorted(reports_dir.glob("*.json"), key=lambda x: x.stat().st_mtime, reverse=True)
        for file_path in json_files[keep_recent:]:
            file_path.unlink()
            print(f"已删除报告: {file_path}")
        
        return len(html_files[keep_recent:]) + len(json_files[keep_recent:])
    
    def cleanup_screenshots(self, keep_recent: int = 20):
        """清理截图文件"""
        screenshots_dir = Path("screenshots")
        if not screenshots_dir.exists():
            return 0
        
        files = sorted(screenshots_dir.glob("*.png"), key=lambda x: x.stat().st_mtime, reverse=True)
        
        for file_path in files[keep_recent:]:
            file_path.unlink()
            print(f"已删除截图: {file_path}")
        
        return len(files[keep_recent:])
    
    def cleanup_all_temp(self):
        """清理所有临时文件"""
        patterns = [
            "*.tmp",
            "*.log",
            "__pycache__",
            ".pytest_cache",
            "*.pyc"
        ]
        
        total_deleted = 0
        for pattern in patterns:
            if pattern.startswith("."):
                # 清理目录
                for path in Path(".").rglob(pattern):
                    if path.is_dir():
                        import shutil
                        shutil.rmtree(path)
                        print(f"已删除目录: {path}")
                        total_deleted += 1
            else:
                # 清理文件
                for path in Path(".").rglob(pattern):
                    path.unlink()
                    print(f"已删除文件: {path}")
                    total_deleted += 1
        
        return total_deleted


if __name__ == "__main__":
    cleanup = DataCleanup()
    
    # 示例：清理7天前的数据文件
    # cleanup.cleanup_by_age(days=7)
    
    # 示例：保留最近5个数据文件
    # cleanup.cleanup_files(keep_recent=5)
    
    # 示例：清理所有临时文件
    cleanup.cleanup_all_temp()
