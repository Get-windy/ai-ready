#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 数据库性能基准测试数据准备
准备 MySQL、PostgreSQL 和 Redis 的测试数据
"""

import json
import random
import string
from datetime import datetime, timedelta


def generate_test_data(num_records: int = 10000):
    """生成测试数据"""
    data = []
    
    for i in range(num_records):
        record = {
            "id": i + 1,
            "name": f"test_user_{i + 1}",
            "email": f"user{i + 1}@example.com",
            "age": random.randint(18, 80),
            "balance": round(random.uniform(0, 10000), 2),
            "status": random.choice(["active", "inactive", "suspended"]),
            "created_at": (datetime.now() - timedelta(days=random.randint(0, 365))).isoformat(),
            "updated_at": datetime.now().isoformat(),
            "metadata": {
                "preferences": {
                    "theme": random.choice(["light", "dark"]),
                    "language": random.choice(["en", "zh", "es", "fr"]),
                    "notifications": random.choice([True, False])
                },
                "tags": [f"tag_{random.randint(1, 10)}" for _ in range(random.randint(1, 5))]
            }
        }
        data.append(record)
    
    return data


def save_test_data():
    """保存测试数据到文件"""
    test_data = generate_test_data(10000)
    
    # 保存为 JSON 文件
    with open("benchmark_test_data.json", "w", encoding="utf-8") as f:
        json.dump(test_data, f, indent=2, ensure_ascii=False)
    
    print(f"已生成 {len(test_data)} 条测试数据")
    print("测试数据文件: benchmark_test_data.json")
    
    # 显示数据样本
    print("\n数据样本:")
    for i in range(3):
        print(f"  {json.dumps(test_data[i], indent=2, ensure_ascii=False)}")


def generate_mysql_schema():
    """生成 MySQL 表结构"""
    schema = """
-- MySQL 基准测试表结构
CREATE TABLE IF NOT EXISTS benchmark_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INT,
    balance DECIMAL(10,2),
    status ENUM('active', 'inactive', 'suspended') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 索引
    INDEX idx_name (name),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_age (age)
);

-- 插入测试数据的示例
-- INSERT INTO benchmark_users (name, email, age, balance, status) 
-- VALUES ('test_user_1', 'user1@example.com', 25, 1234.56, 'active');
"""
    return schema


def generate_postgresql_schema():
    """生成 PostgreSQL 表结构"""
    schema = """
-- PostgreSQL 基准测试表结构
CREATE TABLE IF NOT EXISTS benchmark_users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INTEGER,
    balance NUMERIC(10,2),
    status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'suspended')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_benchmark_users_name ON benchmark_users (name);
CREATE INDEX IF NOT EXISTS idx_benchmark_users_email ON benchmark_users (email);
CREATE INDEX IF NOT EXISTS idx_benchmark_users_status ON benchmark_users (status);
CREATE INDEX IF NOT EXISTS idx_benchmark_users_created_at ON benchmark_users (created_at);
CREATE INDEX IF NOT EXISTS idx_benchmark_users_age ON benchmark_users (age);
CREATE INDEX IF NOT EXISTS idx_benchmark_users_metadata ON benchmark_users USING GIN (metadata);

-- 插入测试数据的示例
-- INSERT INTO benchmark_users (name, email, age, balance, status, metadata) 
-- VALUES ('test_user_1', 'user1@example.com', 25, 1234.56, 'active', 
--         '{"preferences": {"theme": "light", "language": "en", "notifications": true}, "tags": ["tag_1", "tag_3"]}'::jsonb);
"""
    return schema


def generate_redis_data_structure():
    """生成 Redis 数据结构说明"""
    structure = """
# Redis 基准测试数据结构

## String 类型
- user:{id}:name -> "test_user_{id}"
- user:{id}:email -> "user{id}@example.com"
- user:{id}:balance -> "1234.56"

## Hash 类型  
- user:{id} -> {name: "test_user_{id}", email: "user{id}@example.com", age: "25", balance: "1234.56"}

## Set 类型
- users:active -> {1, 2, 3, ...}
- users:inactive -> {...}
- users:suspended -> {...}

## Sorted Set 类型
- users:by_balance -> {1: 1234.56, 2: 2345.67, ...}
- users:by_created_at -> {1: 1640995200, 2: 1641081600, ...}

## List 类型
- user:{id}:activity_log -> ["login", "view_profile", "update_settings", ...]
"""
    return structure


def main():
    print("=" * 60)
    print("AI-Ready 数据库性能基准测试数据准备")
    print("=" * 60)
    
    # 生成测试数据
    save_test_data()
    
    print("\n" + "=" * 60)
    print("数据库表结构:")
    print("=" * 60)
    
    # 保存 MySQL 表结构
    mysql_schema = generate_mysql_schema()
    with open("mysql_benchmark_schema.sql", "w", encoding="utf-8") as f:
        f.write(mysql_schema)
    print("MySQL 表结构: mysql_benchmark_schema.sql")
    
    # 保存 PostgreSQL 表结构
    postgresql_schema = generate_postgresql_schema()
    with open("postgresql_benchmark_schema.sql", "w", encoding="utf-8") as f:
        f.write(postgresql_schema)
    print("PostgreSQL 表结构: postgresql_benchmark_schema.sql")
    
    # 保存 Redis 数据结构
    redis_structure = generate_redis_data_structure()
    with open("redis_benchmark_structure.md", "w", encoding="utf-8") as f:
        f.write(redis_structure)
    print("Redis 数据结构: redis_benchmark_structure.md")
    
    print("\n数据准备完成！")


if __name__ == '__main__':
    main()