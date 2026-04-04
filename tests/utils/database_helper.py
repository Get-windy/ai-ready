#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库辅助工具 - 测试数据准备和清理
"""

import pymysql
from typing import List, Dict, Any, Optional
import logging

logger = logging.getLogger(__name__)


class DatabaseHelper:
    """数据库辅助工具"""
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        初始化数据库连接
        
        Args:
            config: 数据库配置，默认使用测试环境配置
        """
        self.config = config or {
            'host': 'localhost',
            'port': 3306,
            'user': 'root',
            'password': 'test123',
            'database': 'ai_ready_test',
            'charset': 'utf8mb4'
        }
        self.connection = None
        self.connect()
    
    def connect(self):
        """建立数据库连接"""
        try:
            self.connection = pymysql.connect(
                host=self.config['host'],
                port=self.config['port'],
                user=self.config['user'],
                password=self.config['password'],
                database=self.config['database'],
                charset=self.config['charset'],
                cursorclass=pymysql.cursors.DictCursor
            )
            logger.info("数据库连接成功")
        except Exception as e:
            logger.error(f"数据库连接失败: {e}")
            raise
    
    def close(self):
        """关闭数据库连接"""
        if self.connection:
            self.connection.close()
            logger.info("数据库连接已关闭")
    
    def execute(self, sql: str, params: tuple = None) -> int:
        """
        执行SQL语句
        
        Args:
            sql: SQL语句
            params: 参数
        
        Returns:
            影响的行数
        """
        with self.connection.cursor() as cursor:
            cursor.execute(sql, params)
            self.connection.commit()
            return cursor.rowcount
    
    def query(self, sql: str, params: tuple = None) -> List[Dict]:
        """
        查询数据
        
        Args:
            sql: SQL语句
            params: 参数
        
        Returns:
            查询结果列表
        """
        with self.connection.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()
    
    def query_one(self, sql: str, params: tuple = None) -> Optional[Dict]:
        """
        查询单条数据
        
        Args:
            sql: SQL语句
            params: 参数
        
        Returns:
            单条数据或None
        """
        with self.connection.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchone()
    
    def insert(self, table: str, data: Dict) -> int:
        """
        插入数据
        
        Args:
            table: 表名
            data: 数据字典
        
        Returns:
            插入的行ID
        """
        columns = ', '.join(data.keys())
        placeholders = ', '.join(['%s'] * len(data))
        sql = f"INSERT INTO {table} ({columns}) VALUES ({placeholders})"
        
        with self.connection.cursor() as cursor:
            cursor.execute(sql, tuple(data.values()))
            self.connection.commit()
            return cursor.lastrowid
    
    def delete(self, table: str, condition: str) -> int:
        """
        删除数据
        
        Args:
            table: 表名
            condition: 条件（如 "id = 1"）
        
        Returns:
            删除的行数
        """
        sql = f"DELETE FROM {table} WHERE {condition}"
        return self.execute(sql)
    
    def truncate(self, table: str):
        """
        清空表数据
        
        Args:
            table: 表名
        """
        sql = f"TRUNCATE TABLE {table}"
        self.execute(sql)
        logger.info(f"表 {table} 已清空")
    
    def prepare_test_data(self, table: str, data_list: List[Dict]):
        """
        批量准备测试数据
        
        Args:
            table: 表名
            data_list: 数据列表
        """
        for data in data_list:
            self.insert(table, data)
        logger.info(f"已插入 {len(data_list)} 条测试数据到 {table}")
    
    def cleanup_test_data(self, table: str, prefix: str = 'test_'):
        """
        清理测试数据
        
        Args:
            table: 表名
            prefix: 测试数据标识前缀
        """
        # 根据表名和前缀清理
        condition = f"name LIKE '{prefix}%' OR code LIKE '{prefix}%'"
        self.delete(table, condition)
        logger.info(f"已清理 {table} 中的测试数据")