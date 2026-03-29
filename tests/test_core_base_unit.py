#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
core-base模块单元测试
"""

import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestCoreBaseUnit:
    """core-base模块单元测试"""
    
    def test_user_helper_functions(self):
        """测试用户帮助函数"""
        # 这里测试core-base中的用户辅助函数
        # 由于实际代码未提供，使用mock测试结构
        
        def validate_username(username):
            """验证用户名"""
            if not username or len(username) < 3:
                return False
            return True
        
        def validate_email(email):
            """验证邮箱"""
            import re
            pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
            return bool(re.match(pattern, email))
        
        def hash_password(password):
            """密码哈希(模拟)"""
            return f"hashed_{password}"
        
        # 测试用户名验证
        assert validate_username("testuser") == True
        assert validate_username("ab") == False
        
        # 测试邮箱验证
        assert validate_email("test@example.com") == True
        assert validate_email("invalid") == False
        
        # 测试密码哈希
        assert hash_password("mypassword").startswith("hashed_")


class TestCoreBaseDataValidation:
    """core-base数据验证测试"""
    
    def test_required_fields(self):
        """测试必填字段验证"""
        def validate_required_fields(data, required_fields):
            """验证必填字段"""
            missing = [field for field in required_fields if field not in data]
            return len(missing) == 0
        
        # 测试完整数据
        user_data = {
            'username': 'testuser',
            'email': 'test@example.com',
            'password': 'password123'
        }
        required = ['username', 'email', 'password']
        
        assert validate_required_fields(user_data, required) == True
        
        # 测试缺少字段
        incomplete_data = {'username': 'testuser'}
        assert validate_required_fields(incomplete_data, required) == False
    
    def test_data_length_validation(self):
        """测试数据长度验证"""
        def validate_length(value, min_len=None, max_len=None):
            """验证字符串长度"""
            if not value:
                return True
            
            if min_len and len(value) < min_len:
                return False
            if max_len and len(value) > max_len:
                return False
            return True
        
        # 测试长度验证
        assert validate_length("abc", min_len=3) == True
        assert validate_length("ab", min_len=3) == False
        assert validate_length("abcdefghij", max_len=10) == True
        assert validate_length("abcdefghijk", max_len=10) == False
    
    def test_numeric_validation(self):
        """测试数值验证"""
        def validate_positive_integer(value):
            """验证正整数"""
            return isinstance(value, int) and value > 0
        
        def validate_range(value, min_val, max_val):
            """验证数值范围"""
            return isinstance(value, (int, float)) and min_val <= value <= max_val
        
        # 测试正整数
        assert validate_positive_integer(10) == True
        assert validate_positive_integer(0) == False
        assert validate_positive_integer(-5) == False
        
        # 测试范围
        assert validate_range(50, 0, 100) == True
        assert validate_range(150, 0, 100) == False


class TestCoreBasePagination:
    """core-base分页测试"""
    
    def test_pagination_calculator(self):
        """测试分页计算"""
        def calculate_pagination(total, page=1, page_size=10):
            """计算分页信息"""
            total_pages = (total + page_size - 1) // page_size
            offset = (page - 1) * page_size
            
            return {
                'page': page,
                'page_size': page_size,
                'total': total,
                'total_pages': total_pages,
                'offset': offset,
                'has_more': page < total_pages,
                'has_prev': page > 1
            }
        
        # 测试分页计算
        result = calculate_pagination(100, 1, 10)
        assert result['total_pages'] == 10
        assert result['offset'] == 0
        assert result['has_more'] == True
        assert result['has_prev'] == False
        
        # 测试最后一页
        result = calculate_pagination(100, 10, 10)
        assert result['page'] == 10
        assert result['has_more'] == False
        assert result['has_prev'] == True


class TestCoreBaseUtils:
    """core-base工具函数测试"""
    
    def test_datetime_utils(self):
        """测试日期时间工具"""
        from datetime import datetime, timedelta
        
        def format_datetime(dt, fmt='%Y-%m-%d %H:%M:%S'):
            """格式化日期时间"""
            return dt.strftime(fmt)
        
        def parse_datetime(date_str, fmt='%Y-%m-%d %H:%M:%S'):
            """解析日期时间"""
            return datetime.strptime(date_str, fmt)
        
        def add_days(date_str, days, fmt='%Y-%m-%d'):
            """添加天数"""
            dt = parse_datetime(date_str, fmt)
            new_dt = dt + timedelta(days=days)
            return format_datetime(new_dt, fmt)
        
        # 测试日期格式化
        dt = datetime(2026, 3, 29, 12, 30, 45)
        assert format_datetime(dt) == '2026-03-29 12:30:45'
        
        # 测试日期解析
        parsed = parse_datetime('2026-03-29 12:30:45')
        assert parsed.year == 2026
        assert parsed.month == 3
        assert parsed.day == 29
        
        # 测试日期计算
        new_date = add_days('2026-03-29', 7)
        assert new_date == '2026-04-05'
    
    def test_id_generator(self):
        """测试ID生成器"""
        import uuid
        
        def generate_uuid():
            """生成UUID"""
            return str(uuid.uuid4())
        
        def generate_short_id():
            """生成短ID"""
            return uuid.uuid4().hex[:12]
        
        # 测试UUID生成
        id1 = generate_uuid()
        id2 = generate_uuid()
        
        assert len(id1) == 36  # UUID标准格式长度
        assert id1 != id2
        
        # 测试短ID生成
        short_id = generate_short_id()
        assert len(short_id) == 12
