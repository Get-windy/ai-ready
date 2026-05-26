#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 数据库性能基准测试
支持 MySQL、PostgreSQL 和 Redis 性能测试
"""

import time
import json
import statistics
import random
from datetime import datetime
from typing import Dict, List, Any, Optional
from concurrent.futures import ThreadPoolExecutor, as_completed

# 尝试导入数据库驱动
try:
    import mysql.connector
    MYSQL_AVAILABLE = True
except ImportError:
    MYSQL_AVAILABLE = False

try:
    import psycopg2
    POSTGRESQL_AVAILABLE = True
except ImportError:
    POSTGRESQL_AVAILABLE = False

try:
    import redis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False

# 测试配置
DB_CONFIG = {
    'mysql': {
        'host': 'localhost',
        'port': 3306,
        'user': 'test_user',
        'password': 'test_password',
        'database': 'ai_ready_test'
    },
    'postgresql': {
        'host': 'localhost',
        'port': 5432,
        'user': 'test_user',
        'password': 'test_password',
        'database': 'ai_ready_test'
    },
    'redis': {
        'host': 'localhost',
        'port': 6379,
        'db': 0,
        'password': None
    }
}

# 测试结果存储
BENCHMARK_RESULTS = {
    "test_time": "",
    "mysql_results": {},
    "postgresql_results": {},
    "redis_results": {},
    "summary": {}
}


class BenchmarkResult:
    """基准测试结果"""
    def __init__(self, name: str, db_type: str):
        self.name = name
        self.db_type = db_type
        self.status = "SKIP"
        self.avg_time = 0
        self.p95_time = 0
        self.p99_time = 0
        self.message = ""
        self.metrics = {}
        self.details = {}
    
    def pass_(self, message: str):
        self.status = "PASS"
        self.message = message
    
    def fail(self, message: str):
        self.status = "FAIL"
        self.message = message
    
    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "db_type": self.db_type,
            "status": self.status,
            "avg_time_ms": round(self.avg_time, 2),
            "p95_time_ms": round(self.p95_time, 2),
            "p99_time_ms": round(self.p99_time, 2),
            "message": self.message,
            "metrics": self.metrics,
            "details": self.details
        }


def measure_operation_times(operation_func, iterations: int = 100) -> Optional[Dict[str, float]]:
    """测量操作执行时间"""
    times = []
    success_count = 0
    
    for i in range(iterations):
        start = time.perf_counter()
        try:
            result = operation_func()
            if result is not False:  # 假设 False 表示失败
                elapsed = (time.perf_counter() - start) * 1000
                times.append(elapsed)
                success_count += 1
        except Exception as e:
            # 操作失败，不记录时间
            pass
    
    if times:
        sorted_times = sorted(times)
        return {
            "avg": statistics.mean(times),
            "min": min(times),
            "max": max(times),
            "p95": sorted_times[int(len(sorted_times) * 0.95)],
            "p99": sorted_times[int(len(sorted_times) * 0.99)],
            "success_rate": success_count / iterations * 100,
            "samples": len(times)
        }
    return None


# ==================== MySQL 性能测试 ====================

class MySQLBenchmark:
    """MySQL 性能基准测试"""
    
    def __init__(self, config: Dict):
        self.config = config
        self.connection = None
    
    def connect(self):
        """建立数据库连接"""
        if not MYSQL_AVAILABLE:
            return False
        
        try:
            self.connection = mysql.connector.connect(**self.config)
            return True
        except Exception as e:
            print(f"MySQL 连接失败: {e}")
            return False
    
    def disconnect(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            self.connection = None
    
    def setup_test_tables(self):
        """创建测试表"""
        if not self.connection:
            return False
        
        cursor = self.connection.cursor()
        try:
            # 创建测试表
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS benchmark_test (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100),
                    value DECIMAL(10,2),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_created_at (created_at)
                )
            """)
            
            # 清空测试数据
            cursor.execute("DELETE FROM benchmark_test")
            self.connection.commit()
            return True
        except Exception as e:
            print(f"MySQL 创建表失败: {e}")
            return False
        finally:
            cursor.close()
    
    def cleanup_test_tables(self):
        """清理测试表"""
        if not self.connection:
            return False
        
        cursor = self.connection.cursor()
        try:
            cursor.execute("DROP TABLE IF EXISTS benchmark_test")
            self.connection.commit()
            return True
        except Exception as e:
            print(f"MySQL 清理表失败: {e}")
            return False
        finally:
            cursor.close()
    
    def test_simple_insert(self, iterations: int = 1000):
        """测试简单插入性能"""
        if not self.connection:
            return None
        
        def insert_operation():
            cursor = self.connection.cursor()
            try:
                cursor.execute(
                    "INSERT INTO benchmark_test (name, value) VALUES (%s, %s)",
                    (f"test_{random.randint(1, 10000)}", random.uniform(1, 1000))
                )
                self.connection.commit()
                return True
            except Exception:
                return False
            finally:
                cursor.close()
        
        return measure_operation_times(insert_operation, iterations)
    
    def test_batch_insert(self, batch_size: int = 100, iterations: int = 10):
        """测试批量插入性能"""
        if not self.connection:
            return None
        
        def batch_insert_operation():
            cursor = self.connection.cursor()
            try:
                data = [(f"batch_test_{i}", random.uniform(1, 1000)) for i in range(batch_size)]
                cursor.executemany(
                    "INSERT INTO benchmark_test (name, v