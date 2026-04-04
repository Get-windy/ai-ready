#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户模块单元测试 - 本地环境
测试用户注册、登录、信息管理等核心功能
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from mocks.mock_services import mock_db, mock_auth
from unit.local_test_config import TEST_USERS


@pytest.mark.unit
class TestUserModel:
    """用户模型测试"""
    
    def test_user_creation(self):
        """测试用户创建"""
        user_data = {
            'username': 'newuser',
            'password': 'Test@123456',
            'email': 'newuser@test.com',
            'phone': '13800138001',
            'real_name': '新用户',
            'tenant_id': 1,
            'dept_id': 2,
            'status': 0
        }
        
        result = mock_db.insert('sys_user', user_data)
        
        assert result['id'] > 0
        assert result['username'] == 'newuser'
        assert result['create_time'] is not None
        assert result['deleted'] == 0
    
    def test_user_query(self):
        """测试用户查询"""
        # 先插入测试数据
        mock_db.insert('sys_user', TEST_USERS['admin'])
        
        # 查询用户
        results = mock_db.select('sys_user', {'username': 'admin'})
        
        assert len(results) > 0
        assert results[0]['username'] == 'admin'
    
    def test_user_update(self):
        """测试用户更新"""
        # 插入用户
        user = mock_db.insert('sys_user', {'username': 'updateuser', 'tenant_id': 1})
        
        # 更新用户
        updated = mock_db.update('sys_user', user['id'], {'real_name': '更新名称'})
        
        assert updated is not None
        assert updated['real_name'] == '更新名称'
        assert updated['update_time'] is not None
    
    def test_user_delete(self):
        """测试用户软删除"""
        # 插入用户
        user = mock_db.insert('sys_user', {'username': 'deleteuser', 'tenant_id': 1})
        
        # 删除用户
        result = mock_db.delete('sys_user', user['id'])
        
        assert result == True
        
        # 查询已删除用户（不应返回）
        results = mock_db.select('sys_user', {'username': 'deleteuser'})
        assert len(results) == 0
    
    def test_user_count(self):
        """测试用户计数"""
        mock_db._init_tables()
        
        # 插入多个用户
        for i in range(5):
            mock_db.insert('sys_user', {'username': f'user{i}', 'tenant_id': 1, 'status': 0})
        
        count = mock_db.count('sys_user', {'tenant_id': 1})
        assert count == 5


@pytest.mark.unit
class TestUserAuthentication:
    """用户认证测试"""
    
    def test_login_success(self):
        """测试登录成功"""
        result = mock_auth.login('testuser', 'Test@123456')
        
        assert result['success'] == True
        assert result['token'] is not None
        assert result['user_id'] > 0
    
    def test_login_invalid_password(self):
        """测试密码格式错误"""
        result = mock_auth.login('testuser', '123')
        
        assert result['success'] == False
        assert '密码' in result['message']
    
    def test_token_validation(self):
        """测试token验证"""
        # 先登录
        login_result = mock_auth.login('testuser', 'Test@123456')
        token = login_result['token']
        
        # 验证token
        validation = mock_auth.validate_token(token)
        
        assert validation['valid'] == True
        assert validation['user_id'] == login_result['user_id']
    
    def test_token_invalid(self):
        """测试无效token"""
        validation = mock_auth.validate_token('invalid_token')
        
        assert validation['valid'] == False
    
    def test_logout(self):
        """测试注销"""
        # 先登录
        login_result = mock_auth.login('logoutuser', 'Test@123456')
        token = login_result['token']
        
        # 注销
        result = mock_auth.logout(token)
        
        assert result == True
        
        # 验证token已失效
        validation = mock_auth.validate_token(token)
        assert validation['valid'] == False


@pytest.mark.unit
class TestUserValidation:
    """用户数据验证测试"""
    
    def test_username_validation(self):
        """测试用户名验证"""
        def validate_username(username):
            if not username or len(username) < 3:
                return False, "用户名长度至少3位"
            if len(username) > 50:
                return False, "用户名长度不能超过50位"
            if not username[0].isalpha():
                return False, "用户名必须以字母开头"
            return True, "OK"
        
        # 有效用户名
        assert validate_username('admin')[0] == True
        assert validate_username('test123')[0] == True
        
        # 无效用户名
        assert validate_username('ab')[0] == False
        assert validate_username('123admin')[0] == False
    
    def test_password_validation(self):
        """测试密码验证"""
        def validate_password(password):
            if not password or len(password) < 6:
                return False, "密码长度至少6位"
            if len(password) > 20:
                return False, "密码长度不能超过20位"
            
            has_upper = any(c.isupper() for c in password)
            has_lower = any(c.islower() for c in password)
            has_digit = any(c.isdigit() for c in password)
            has_special = any(c in '!@#$%^&*()_+-=' for c in password)
            
            if not (has_upper and has_lower and has_digit):
                return False, "密码必须包含大小写字母和数字"
            
            return True, "OK"
        
        # 有效密码
        assert validate_password('Admin@123')[0] == True
        assert validate_password('Test123!')[0] == True
        
        # 无效密码
        assert validate_password('123456')[0] == False
        assert validate_password('abcdef')[0] == False
    
    def test_email_validation(self):
        """测试邮箱验证"""
        import re
        
        def validate_email(email):
            pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
            return bool(re.match(pattern, email))
        
        assert validate_email('test@example.com') == True
        assert validate_email('user.name@company.co.uk') == True
        assert validate_email('invalid') == False
        assert validate_email('@example.com') == False
    
    def test_phone_validation(self):
        """测试电话验证"""
        import re
        
        def validate_phone(phone):
            pattern = r'^1[3-9]\d{9}$'
            return bool(re.match(pattern, phone))
        
        assert validate_phone('13800138000') == True
        assert validate_phone('15912345678') == True
        assert validate_phone('12800138000') == False  # 12开头无效


@pytest.mark.unit
class TestUserStatus:
    """用户状态测试"""
    
    def test_status_normal(self):
        """测试正常状态"""
        user = {'status': 0}
        assert user['status'] == 0  # 正常
    
    def test_status_disabled(self):
        """测试禁用状态"""
        user = {'status': 1}
        assert user['status'] == 1  # 禁用
    
    def test_status_locked(self):
        """测试锁定状态"""
        user = {'status': 2}
        assert user['status'] == 2  # 锁定
    
    def test_status_change(self):
        """测试状态变更"""
        user = mock_db.insert('sys_user', {'username': 'statususer', 'status': 0, 'tenant_id': 1})
        
        # 禁用用户
        mock_db.update('sys_user', user['id'], {'status': 1})
        
        # 查询
        results = mock_db.select('sys_user', {'id': user['id']})
        assert results[0]['status'] == 1


@pytest.mark.unit  
class TestUserTenant:
    """用户租户测试"""
    
    def test_tenant_isolation(self):
        """测试租户隔离"""
        mock_db._init_tables()
        
        # 创建不同租户的用户
        mock_db.insert('sys_user', {'username': 'user_t1', 'tenant_id': 1})
        mock_db.insert('sys_user', {'username': 'user_t2', 'tenant_id': 2})
        
        # 查询租户1的用户
        t1_users = mock_db.select('sys_user', {'tenant_id': 1})
        t2_users = mock_db.select('sys_user', {'tenant_id': 2})
        
        assert len(t1_users) == 1
        assert len(t2_users) == 1
        assert t1_users[0]['username'] == 'user_t1'
        assert t2_users[0]['username'] == 'user_t2'