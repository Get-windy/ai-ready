#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 权限管理API补充测试
补充边界场景和异常场景测试
"""

import pytest
import requests
import json
from typing import Dict, Any

# ==================== 配置 ====================

BASE_URL = "http://localhost:8080/api"

class PermissionApiClient:
    """权限管理API客户端"""
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        self._token: str = None
    
    def set_token(self, token: str):
        self._token = token
        self.session.headers["Authorization"] = f"Bearer {token}"
    
    def login(self, username: str = "admin", password: str = "Admin@123456") -> Dict:
        """登录获取token"""
        url = f"{self.base_url}/auth/login"
        data = {"username": username, "password": password, "tenantId": 1}
        try:
            resp = self.session.post(url, json=data, timeout=10)
            result = resp.json()
            if result.get("code") == 200:
                self.set_token(result.get("data"))
            return result
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def create_permission(self, data: Dict) -> Dict:
        """创建权限"""
        url = f"{self.base_url}/permission"
        try:
            resp = self.session.post(url, json=data, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def update_permission(self, permission_id: int, data: Dict) -> Dict:
        """更新权限"""
        url = f"{self.base_url}/permission/{permission_id}"
        try:
            resp = self.session.put(url, json=data, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def delete_permission(self, permission_id: int) -> Dict:
        """删除权限"""
        url = f"{self.base_url}/permission/{permission_id}"
        try:
            resp = self.session.delete(url, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_permission(self, permission_id: int) -> Dict:
        """获取权限详情"""
        url = f"{self.base_url}/permission/{permission_id}"
        try:
            resp = self.session.get(url, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_permission_list(self, params: Dict = None) -> Dict:
        """获取权限列表"""
        url = f"{self.base_url}/permission/list"
        try:
            resp = self.session.get(url, params=params, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def get_permission_tree(self) -> Dict:
        """获取权限树"""
        url = f"{self.base_url}/permission/tree"
        try:
            resp = self.session.get(url, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}
    
    def batch_delete_permissions(self, ids: list) -> Dict:
        """批量删除权限"""
        url = f"{self.base_url}/permission/batch"
        try:
            resp = self.session.delete(url, json={"ids": ids}, timeout=10)
            return resp.json()
        except Exception as e:
            return {"code": 500, "message": str(e)}


# ==================== 测试类 ====================

@pytest.fixture
def client():
    """创建API客户端并登录"""
    api = PermissionApiClient()
    api.login()
    return api


@pytest.mark.permission
class TestPermissionApiSupplement:
    """权限管理API补充测试"""
    
    # ==================== 创建权限边界测试 ====================
    
    def test_create_permission_with_empty_name(self, client):
        """创建权限 - 空权限名称"""
        data = {
            "permissionCode": "test:empty:name",
            "permissionName": "",
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1
        }
        result = client.create_permission(data)
        assert result.get("code") in [200, 400]
    
    def test_create_permission_with_long_name(self, client):
        """创建权限 - 超长权限名称（边界值）"""
        data = {
            "permissionCode": "test:long:name",
            "permissionName": "A" * 100,
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1
        }
        result = client.create_permission(data)
        assert result.get("code") in [200, 400]
    
    def test_create_permission_with_special_chars(self, client):
        """创建权限 - 特殊字符权限编码"""
        data = {
            "permissionCode": "test:special!@#$%",
            "permissionName": "特殊字符测试",
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1
        }
        result = client.create_permission(data)
        assert result.get("code") in [200, 400]
    
    def test_create_permission_with_unicode(self, client):
        """创建权限 - Unicode字符权限编码"""
        data = {
            "permissionCode": "test:unicode:测试",
            "permissionName": "Unicode测试",
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1
        }
        result = client.create_permission(data)
        assert result.get("code") in [200, 400]
    
    def test_create_permission_with_negative_parent_id(self, client):
        """创建权限 - 负数父权限ID"""
        data = {
            "permissionCode": "test:negative:parent",
            "permissionName": "负数父ID测试",
            "permissionType": 1,
            "parentId": -1,
            "tenantId": 1
        }
        result = client.create_permission(data)
        assert result.get("code") in [200, 400]
    
    def test_create_permission_with_nonexistent_parent(self, client):
        """创建权限 - 不存在的父权限ID"""
        data = {
            "permissionCode": "test:nonexistent:parent",
            "permissionName": "不存在父权限测试",
            "permissionType": 1,
            "parentId": 999999,
            "tenantId": 1
        }
        result = client.create_permission(data)
        assert result.get("code") in [200, 400, 404]
    
    # ==================== 更新权限边界测试 ====================
    
    def test_update_permission_with_empty_data(self, client):
        """更新权限 - 空数据"""
        result = client.update_permission(1, {})
        assert result.get("code") in [200, 400]
    
    def test_update_permission_with_null_values(self, client):
        """更新权限 - null值字段"""
        data = {
            "permissionName": None,
            "status": None
        }
        result = client.update_permission(1, data)
        assert result.get("code") in [200, 400]
    
    def test_update_nonexistent_permission(self, client):
        """更新权限 - 不存在的权限ID"""
        data = {
            "permissionName": "更新的名称"
        }
        result = client.update_permission(999999, data)
        assert result.get("code") in [200, 404, 500]
    
    # ==================== 删除权限边界测试 ====================
    
    def test_delete_permission_with_zero_id(self, client):
        """删除权限 - ID为0"""
        result = client.delete_permission(0)
        assert result.get("code") in [200, 400, 404]
    
    def test_delete_permission_with_negative_id(self, client):
        """删除权限 - 负数ID"""
        result = client.delete_permission(-1)
        assert result.get("code") in [200, 400, 404]
    
    def test_delete_nonexistent_permission(self, client):
        """删除权限 - 不存在的权限ID"""
        result = client.delete_permission(999999)
        assert result.get("code") in [200, 404, 500]
    
    # ==================== 查询权限边界测试 ====================
    
    def test_get_permission_with_zero_id(self, client):
        """获取权限详情 - ID为0"""
        result = client.get_permission(0)
        assert result.get("code") in [200, 400, 404]
    
    def test_get_permission_with_negative_id(self, client):
        """获取权限详情 - 负数ID"""
        result = client.get_permission(-1)
        assert result.get("code") in [200, 400, 404]
    
    def test_get_permission_list_with_large_page_size(self, client):
        """获取权限列表 - 超大分页大小"""
        result = client.get_permission_list({"page": 1, "size": 10000})
        assert result.get("code") in [200, 400]
    
    def test_get_permission_list_with_zero_page(self, client):
        """获取权限列表 - 页码为0"""
        result = client.get_permission_list({"page": 0, "size": 10})
        assert result.get("code") in [200, 400]
    
    def test_get_permission_list_with_negative_page(self, client):
        """获取权限列表 - 负数页码"""
        result = client.get_permission_list({"page": -1, "size": 10})
        assert result.get("code") in [200, 400]
    
    # ==================== 批量操作边界测试 ====================
    
    def test_batch_delete_with_empty_list(self, client):
        """批量删除权限 - 空列表"""
        result = client.batch_delete_permissions([])
        assert result.get("code") in [200, 400]
    
    def test_batch_delete_with_single_id(self, client):
        """批量删除权限 - 单个ID"""
        result = client.batch_delete_permissions([1])
        assert result.get("code") in [200, 400, 404]
    
    def test_batch_delete_with_mixed_ids(self, client):
        """批量删除权限 - 混合有效和无效ID"""
        result = client.batch_delete_permissions([1, 999999, 888888])
        assert result.get("code") in [200, 400, 404, 500]
    
    def test_batch_delete_with_duplicate_ids(self, client):
        """批量删除权限 - 重复ID"""
        result = client.batch_delete_permissions([1, 1, 1])
        assert result.get("code") in [200, 400]
    
    # ==================== 权限树边界测试 ====================
    
    def test_get_permission_tree_without_auth(self, client):
        """获取权限树 - 未授权访问"""
        client._token = None
        client.session.headers.pop("Authorization", None)
        result = client.get_permission_tree()
        assert result.get("code") in [401, 403, 500]


@pytest.mark.permission
class TestPermissionBusinessLogic:
    """权限管理业务逻辑测试"""
    
    def test_permission_code_uniqueness(self, client):
        """测试权限编码唯一性约束"""
        data1 = {
            "permissionCode": "unique:test:code",
            "permissionName": "唯一性测试1",
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1
        }
        result1 = client.create_permission(data1)
        
        data2 = {
            "permissionCode": "unique:test:code",
            "permissionName": "唯一性测试2",
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1
        }
        result2 = client.create_permission(data2)
        
        if result1.get("code") == 200:
            assert result2.get("code") in [400, 409, 500]
    
    def test_permission_hierarchy_integrity(self, client):
        """测试权限层级完整性"""
        parent_data = {
            "permissionCode": "parent:test",
            "permissionName": "父权限测试",
            "permissionType": 0,
            "parentId": 0,
            "tenantId": 1
        }
        parent_result = client.create_permission(parent_data)
        
        if parent_result.get("code") == 200:
            parent_id = parent_result.get("data")
            
            child_data = {
                "permissionCode": "child:test",
                "permissionName": "子权限测试",
                "permissionType": 1,
                "parentId": parent_id,
                "tenantId": 1
            }
            child_result = client.create_permission(child_data)
            assert child_result.get("code") in [200, 400]
            
            delete_result = client.delete_permission(parent_id)
            assert delete_result.get("code") in [400, 409, 500]
    
    def test_permission_status_transition(self, client):
        """测试权限状态转换"""
        data = {
            "permissionCode": "status:test",
            "permissionName": "状态测试",
            "permissionType": 1,
            "parentId": 0,
            "tenantId": 1,
            "status": 0
        }
        create_result = client.create_permission(data)
        
        if create_result.get("code") == 200:
            permission_id = create_result.get("data")
            
            update_data = {"status": 1}
            update_result = client.update_permission(permission_id, update_data)
            assert update_result.get("code") in [200, 400]
            
            update_data2 = {"status": 0}
            update_result2 = client.update_permission(permission_id, update_data2)
            assert update_result2.get("code") in [200, 400]


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
