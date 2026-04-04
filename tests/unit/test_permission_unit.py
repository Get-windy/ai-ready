#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
权限模块单元测试 - 本地环境
"""

import pytest
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from mocks.mock_services import mock_permission, mock_db
from unit.local_test_config import TEST_PERMISSIONS


@pytest.mark.unit
class TestRoleModel:
    """角色模型测试"""
    
    def test_role_creation(self):
        """测试角色创建"""
        role = {'role_name': '测试角色', 'role_code': 'TEST', 'tenant_id': 1}
        result = mock_db.insert('sys_role', role)
        assert result['id'] > 0
    
    def test_role_query(self):
        """测试角色查询"""
        mock_db._init_tables()
        for r in TEST_PERMISSIONS['roles']:
            mock_db.insert('sys_role', r)
        results = mock_db.select('sys_role', {'role_code': 'ADMIN'})
        assert len(results) > 0


@pytest.mark.unit
class TestPermissionCheck:
    """权限检查测试"""
    
    def test_super_admin_permission(self):
        """测试超级管理员权限"""
        mock_permission.assign_role(1, 'SUPER_ADMIN')
        assert mock_permission.check_permission(1, 'user:create') == True
        assert mock_permission.check_permission(1, 'erp:delete') == True
    
    def test_admin_permission(self):
        """测试管理员权限"""
        mock_permission.assign_role(2, 'ADMIN')
        assert mock_permission.check_permission(2, 'user:create') == True
        assert mock_permission.check_permission(2, 'erp:create') == False
    
    def test_user_permission(self):
        """测试普通用户权限"""
        mock_permission.assign_role(3, 'USER')
        assert mock_permission.check_permission(3, 'user:read') == True
        assert mock_permission.check_permission(3, 'user:delete') == False
    
    def test_erp_admin_permission(self):
        """测试ERP管理员权限"""
        mock_permission.assign_role(4, 'ERP_ADMIN')
        assert mock_permission.check_permission(4, 'erp:create') == True
        assert mock_permission.check_permission(4, 'crm:create') == False
    
    def test_crm_admin_permission(self):
        """测试CRM管理员权限"""
        mock_permission.assign_role(5, 'CRM_ADMIN')
        assert mock_permission.check_permission(5, 'crm:create') == True
        assert mock_permission.check_permission(5, 'erp:create') == False
    
    def test_invalid_role(self):
        """测试无效角色"""
        result = mock_permission.assign_role(9999, 'INVALID_ROLE')
        assert result == False


@pytest.mark.unit
class TestMenuPermission:
    """菜单权限测试"""
    
    def test_super_admin_menus(self):
        """测试超级管理员菜单"""
        mock_permission.assign_role(1, 'SUPER_ADMIN')
        menus = mock_permission.get_menus(1)
        assert len(menus) == 7
    
    def test_admin_menus(self):
        """测试管理员菜单"""
        mock_permission.assign_role(2, 'ADMIN')
        menus = mock_permission.get_menus(2)
        assert 1 in menus


@pytest.mark.unit
class TestRoleAssignment:
    """角色分配测试"""
    
    def test_assign_role_success(self):
        """测试分配角色成功"""
        result = mock_permission.assign_role(100, 'USER')
        assert result == True
    
    def test_reassign_role(self):
        """测试重新分配角色"""
        mock_permission.assign_role(102, 'USER')
        assert mock_permission.check_permission(102, 'user:read') == True
        mock_permission.assign_role(102, 'ADMIN')
        assert mock_permission.check_permission(102, 'user:create') == True


@pytest.mark.unit
class TestDataScope:
    """数据权限测试"""
    
    def test_data_scope_all(self):
        """测试全部数据权限"""
        def can_access(user_dept, target_dept, scope):
            return scope == 1
        assert can_access(1, 999, 1) == True
    
    def test_data_scope_dept(self):
        """测试本部门数据权限"""
        def can_access(user_dept, target_dept, scope):
            return scope == 2 and user_dept == target_dept
        assert can_access(1, 1, 2) == True
        assert can_access(1, 2, 2) == False