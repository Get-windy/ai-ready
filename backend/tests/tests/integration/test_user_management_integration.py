#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 用户管理业务流程集成测试
覆盖用户生命周期管理的完整流程
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.mocks.mock_services import mock_db, mock_auth, mock_permission


@pytest.mark.integration
class TestUserManagementIntegration:
    """用户管理业务流程集成测试"""
    
    def test_user_lifecycle_flow(self):
        """测试用户完整生命周期流程"""
        mock_db._init_tables()
        
        # 1. 创建用户
        user_data = {
            'username': 'integration_test_user',
            'email': 'integration@test.com',
            'phone': '13800138000',
            'status': 1,
            'tenant_id': 1
        }
        result = mock_db.insert('sys_user', user_data)
        assert result['id'] > 0
        user_id = result['id']
        
        # 2. 分配角色
        mock_permission.assign_role(user_id, 'USER')
        assert mock_permission.check_permission(user_id, 'user:read') == True
        
        # 3. 用户登录
        login_result = mock_auth.login('integration_test_user', 'password123')
        assert login_result['success'] == True
        
        # 4. 更新用户信息
        mock_db.update('sys_user', user_id, {'email': 'updated@test.com'})
        updated = mock_db.select('sys_user', {'id': user_id})[0]
        assert updated['email'] == 'updated@test.com'
        
        # 5. 禁用用户
        mock_db.update('sys_user', user_id, {'status': 0})
        disabled = mock_db.select('sys_user', {'id': user_id})[0]
        assert disabled['status'] == 0
        
        # 6. 删除用户
        mock_db.delete('sys_user', user_id)
        deleted = mock_db.select('sys_user', {'id': user_id})
        assert len(deleted) == 0
    
    def test_user_role_permission_flow(self):
        """测试用户-角色-权限分配流程"""
        mock_db._init_tables()
        mock_permission.user_roles = {}
        
        # 1. 创建管理员用户
        admin = mock_db.insert('sys_user', {
            'username': 'admin_user',
            'email': 'admin@test.com',
            'tenant_id': 1
        })
        admin_id = admin['id']
        
        # 2. 分配管理员角色
        mock_permission.assign_role(admin_id, 'ADMIN')
        
        # 3. 验证管理员权限
        assert mock_permission.check_permission(admin_id, 'user:create') == True
        assert mock_permission.check_permission(admin_id, 'user:delete') == True
        assert mock_permission.check_permission(admin_id, 'system:config') == True
        
        # 4. 创建普通用户
        user = mock_db.insert('sys_user', {
            'username': 'normal_user',
            'email': 'user@test.com',
            'tenant_id': 1
        })
        user_id = user['id']
        
        # 5. 分配普通角色
        mock_permission.assign_role(user_id, 'USER')
        
        # 6. 验证普通用户权限
        assert mock_permission.check_permission(user_id, 'user:read') == True
        assert mock_permission.check_permission(user_id, 'user:delete') == False
    
    def test_multi_tenant_user_management(self):
        """测试多租户用户管理"""
        mock_db._init_tables()
        
        # 租户1创建用户
        user_t1 = mock_db.insert('sys_user', {
            'username': 'user_tenant1',
            'email': 't1@test.com',
            'tenant_id': 1
        })
        
        # 租户2创建用户
        user_t2 = mock_db.insert('sys_user', {
            'username': 'user_tenant2',
            'email': 't2@test.com',
            'tenant_id': 2
        })
        
        # 验证租户隔离
        t1_users = mock_db.select('sys_user', {'tenant_id': 1})
        t2_users = mock_db.select('sys_user', {'tenant_id': 2})
        
        assert len(t1_users) == 1
        assert len(t2_users) == 1
        assert t1_users[0]['username'] == 'user_tenant1'
        assert t2_users[0]['username'] == 'user_tenant2'
    
    def test_user_login_authentication_flow(self):
        """测试用户登录认证流程"""
        mock_db._init_tables()
        
        # 1. 创建测试用户
        user = mock_db.insert('sys_user', {
            'username': 'auth_test_user',
            'password': 'hashed_password',
            'status': 1,
            'tenant_id': 1
        })
        user_id = user['id']
        
        # 2. 成功登录
        login_result = mock_auth.login('auth_test_user', 'password123')
        assert login_result['success'] == True
        assert 'token' in login_result
        
        # 3. Token验证
        token = login_result['token']
        validation = mock_auth.validate_token(token)
        assert validation['valid'] == True
        # Note: mock auth returns hash-based user_id, not the actual user_id
        
        # 4. 错误密码登录
        failed_login = mock_auth.login('auth_test_user', 'wrong_password')
        assert failed_login['success'] == False
        
        # 5. 无效Token验证
        invalid_validation = mock_auth.validate_token('invalid_token')
        assert invalid_validation['valid'] == False


@pytest.mark.integration
class TestUserBatchOperations:
    """用户批量操作集成测试"""
    
    def test_batch_user_creation(self):
        """测试批量创建用户"""
        mock_db._init_tables()
        
        users = [
            {'username': 'batch_user_1', 'email': 'batch1@test.com', 'tenant_id': 1},
            {'username': 'batch_user_2', 'email': 'batch2@test.com', 'tenant_id': 1},
            {'username': 'batch_user_3', 'email': 'batch3@test.com', 'tenant_id': 1}
        ]
        
        created_ids = []
        for user_data in users:
            user = mock_db.insert('sys_user', user_data)
            created_ids.append(user['id'])
        
        # 验证批量创建
        all_users = mock_db.select('sys_user', {'tenant_id': 1})
        assert len(all_users) == 3
        assert len(created_ids) == 3
    
    def test_batch_user_status_update(self):
        """测试批量更新用户状态"""
        mock_db._init_tables()
        
        # 创建多个用户
        for i in range(5):
            mock_db.insert('sys_user', {
                'username': f'status_user_{i}',
                'status': 1,
                'tenant_id': 1
            })
        
        # 批量禁用
        all_users = mock_db.select('sys_user', {'tenant_id': 1})
        for user in all_users:
            mock_db.update('sys_user', user['id'], {'status': 0})
        
        # 验证批量更新
        updated_users = mock_db.select('sys_user', {'tenant_id': 1, 'status': 0})
        assert len(updated_users) == 5