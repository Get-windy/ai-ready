#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户权限管理自动化测试脚本
pytest测试框架实现
覆盖角色创建、修改、删除和权限分配、回收、验证等核心场景
"""

import pytest
import requests
from typing import Dict, Any
import time
import os


class TestUserPermissionManagement:
    """用户权限管理功能测试类"""
    
    # 测试基础URL配置
    BASE_URL = "http://localhost:8080/api/v1"
    TOKEN = None
    
    @pytest.fixture(scope='class', autouse=True)
    def setup_class(self):
        """测试环境准备 - 获取认证Token"""
        # 配置文件读取或使用默认值
        self.BASE_URL = os.getenv('API_BASE_URL', self.BASE_URL)
        
        # 登录获取Token
        login_data = {
            "username": "admin",
            "password": os.getenv('ADMIN_PASSWORD', 'Admin123!')
        }
        
        response = requests.post(
            f"{self.BASE_URL}/auth/login",
            json=login_data,
            timeout=10
        )
        
        if response.status_code == 200:
            self.TOKEN = response.json().get("token")
            print("✓ 成功获取认证Token")
        else:
            pytest.fail(f"登录失败，状态码: {response.status_code}, 响应: {response.text}")
        
        yield
        
        # 清理逻辑
        self.TOKEN = None
    
    def test_create_role(self, setup_class):
        """测试场景1: 创建新角色"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 生成唯一(role code
        role_code = f"test_role_{int(time.time())}"
        role_data = {
            "name": "测试角色",
            "code": role_code,
            "description": "用于测试的角色",
            "permissions": ["user.read", "user.write"]
        }
        
        response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        # 验证响应
        assert response.status_code == 201, f"创建角色失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["code"] == 200 or response_data["code"] == 201, "创建角色失败"
        
        role_id = response_data["data"]["id"]
        assert role_id is not None, "角色ID为空"
        
        print(f"✓ 角色创建成功，角色ID: {role_id}")
        
        # 保存角色ID用于后续测试
        self.test_role_id = role_id
        self.test_role_code = role_code
    
    def test_create_role_duplicate(self, setup_class):
        """测试场景2: 创建重复角色（应失败）"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 首先创建一个角色
        role_code = f"dup_role_{int(time.time())}"
        role_data = {
            "name": "重复角色测试",
            "code": role_code,
            "description": "用于测试重复角色",
            "permissions": []
        }
        
        response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 201, f"首次创建角色失败，状态码: {response.status_code}"
        
        # 再次创建相同角色（应失败）
        response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        # 应返回400或409错误
        assert response.status_code in [400, 409], f"期望重复角色创建失败，但状态码: {response.status_code}"
        
        print("✓ 重复角色创建正确拒绝")
    
    def test_query_role(self, setup_class):
        """测试场景3: 查询角色详情"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 首先创建一个角色
        role_code = f"query_role_{int(time.time())}"
        role_data = {
            "name": "查询测试角色",
            "code": role_code,
            "description": "用于测试查询角色",
            "permissions": ["user.read"]
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建角色失败"
        role_id = create_response.json()["data"]["id"]
        
        # 查询角色
        response = requests.get(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"查询角色失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["data"]["code"] == role_code, "角色编码不匹配"
        
        print(f"✓ 角色查询成功，角色ID: {role_id}")
    
    def test_update_role(self, setup_class):
        """测试场景4: 更新角色信息"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 首先创建一个角色
        role_code = f"update_role_{int(time.time())}"
        role_data = {
            "name": "更新前角色",
            "code": role_code,
            "description": "用于测试更新角色",
            "permissions": ["user.read"]
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建角色失败"
        role_id = create_response.json()["data"]["id"]
        
        # 更新角色
        update_data = {
            "name": "更新后角色",
            "description": "更新后的描述",
            "permissions": ["user.read", "user.write", "user.delete"]
        }
        
        response = requests.put(
            f"{self.BASE_URL}/roles/{role_id}",
            json=update_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"更新角色失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert response_data["data"]["name"] == "更新后角色", "角色名称未更新"
        assert "user.write" in response_data["data"]["permissions"], "权限未完全更新"
        
        print(f"✓ 角色更新成功，角色ID: {role_id}")
    
    def test_delete_role(self, setup_class):
        """测试场景5: 删除角色"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 首先创建一个角色
        role_code = f"delete_role_{int(time.time())}"
        role_data = {
            "name": "删除测试角色",
            "code": role_code,
            "description": "用于测试删除角色",
            "permissions": []
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建角色失败"
        role_id = create_response.json()["data"]["id"]
        
        # 删除角色
        response = requests.delete(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"删除角色失败，状态码: {response.status_code}"
        
        # 验证角色已删除（查询应返回404）
        query_response = requests.get(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )
        
        assert query_response.status_code == 404, "角色删除后仍可查询"
        
        print(f"✓ 角色删除成功，角色ID: {role_id}")
    
    def test_assign_permission_to_role(self, setup_class):
        """测试场景6: 为角色分配权限"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 首先创建一个角色
        role_code = f"perm_role_{int(time.time())}"
        role_data = {
            "name": "权限分配测试角色",
            "code": role_code,
            "description": "用于测试权限分配",
            "permissions": []
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建角色失败"
        role_id = create_response.json()["data"]["id"]
        
        # 分配权限
        permission_data = {
            "permissions": ["user.read", "user.write"]
        }
        
        response = requests.post(
            f"{self.BASE_URL}/roles/{role_id}/permissions",
            json=permission_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"分配权限失败，状态码: {response.status_code}"
        
        # 验证权限已分配
        query_response = requests.get(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )
        
        assert query_response.status_code == 200, "查询角色失败"
        query_data = query_response.json()
        assert "user.read" in query_data["data"]["permissions"], "权限未正确分配"
        
        print(f"✓ 权限分配成功，角色ID: {role_id}")
    
    def test_revoke_permission_from_role(self, setup_class):
        """测试场景7: 从角色回收权限"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 首先创建一个角色并分配权限
        role_code = f"revoke_role_{int(time.time())}"
        role_data = {
            "name": "权限回收测试角色",
            "code": role_code,
            "description": "用于测试权限回收",
            "permissions": ["user.read", "user.write", "user.delete"]
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建角色失败"
        role_id = create_response.json()["data"]["id"]
        
        # 回收部分权限
        permission_data = {
            "permissions": ["user.write"]
        }
        
        response = requests.delete(
            f"{self.BASE_URL}/roles/{role_id}/permissions",
            json=permission_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"回收权限失败，状态码: {response.status_code}"
        
        # 验证权限已回收
        query_response = requests.get(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )
        
        assert query_response.status_code == 200, "查询角色失败"
        query_data = query_response.json()
        assert "user.write" not in query_data["data"]["permissions"], "权限未被回收"
        assert "user.read" in query_data["data"]["permissions"], "其他权限被错误移除"
        
        print(f"✓ 权限回收成功，角色ID: {role_id}")
    
    def test_verify_user_permission(self, setup_class):
        """测试场景8: 验证用户权限"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 首先创建一个用户
        user_data = {
            "username": f"perm_verify_{int(time.time())}",
            "password": "Test123!",
            "email": f"perm_verify_{int(time.time())}@example.com",
            "realName": "权限验证测试用户"
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/users",
            json=user_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, f"创建用户失败，状态码: {create_response.status_code}"
        user_id = create_response.json()["data"]["id"]
        
        # 分配权限给用户
        permission_data = {
            "permissions": ["user.read", "order.read"]
        }
        
        assign_response = requests.post(
            f"{self.BASE_URL}/users/{user_id}/permissions",
            json=permission_data,
            headers=headers,
            timeout=10
        )
        
        assert assign_response.status_code == 200, f"分配权限失败，状态码: {assign_response.status_code}"
        
        # 验证用户权限
        response = requests.get(
            f"{self.BASE_URL}/users/{user_id}/permissions",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"验证用户权限失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "user.read" in response_data["data"]["permissions"], "用户权限未正确分配"
        assert "order.read" in response_data["data"]["permissions"], "订单读权限未分配"
        
        print(f"✓ 用户权限验证成功，用户ID: {user_id}")
        
        # 清理用户
        requests.delete(
            f"{self.BASE_URL}/users/{user_id}",
            headers=headers,
            timeout=10
        )
    
    def test_query_role_list(self, setup_class):
        """测试场景9: 查询角色列表"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 查询角色列表
        response = requests.get(
            f"{self.BASE_URL}/roles?page=1&size=10",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"查询角色列表失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert "items" in response_data["data"], "响应缺少items字段"
        assert "total" in response_data["data"], "响应缺少total字段"
        
        print(f"✓ 角色列表查询成功，共 {response_data['data']['total']} 个角色")
    
    def test_search_roles_by_name(self, setup_class):
        """测试场景10: 按名称搜索角色"""
        headers = {"Authorization": f"Bearer {self.TOKEN}"}
        
        # 创建一个带特定名称的角色
        search_name = f"search_{int(time.time())}"
        role_data = {
            "name": f"搜索测试_{search_name}",
            "code": f"search_role_{search_name}",
            "description": "用于测试搜索角色",
            "permissions": []
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, "创建搜索测试角色失败"
        
        # 搜索角色
        response = requests.get(
            f"{self.BASE_URL}/roles/search?name={search_name}",
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"搜索角色失败，状态码: {response.status_code}"
        
        response_data = response.json()
        assert len(response_data["data"]["items"]) > 0, "搜索结果为空"
        
        print(f"✓ 角色搜索成功，找到 {len(response_data['data']['items'])} 个匹配角色")
    
    def test_assign_role_to_user(self, setup_class):
        """测试场景11: 为用户分配角色"""
        headers = {"Authorization": f"Bearer {self.TOKEN}", "Content-Type": "application/json"}
        
        # 首先创建一个用户
        user_data = {
            "username": f"role_assign_{int(time.time())}",
            "password": "Test123!",
            "email": f"role_assign_{int(time.time())}@example.com",
            "realName": "角色分配测试用户"
        }
        
        create_response = requests.post(
            f"{self.BASE_URL}/users",
            json=user_data,
            headers=headers,
            timeout=10
        )
        
        assert create_response.status_code == 201, f"创建用户失败，状态码: {create_response.status_code}"
        user_id = create_response.json()["data"]["id"]
        
        # 创建一个角色
        role_code = f"user_role_{int(time.time())}"
        role_data = {
            "name": "用户角色分配测试",
            "code": role_code,
            "description": "用于测试角色分配给用户",
            "permissions": ["user.read"]
        }
        
        role_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )
        
        assert role_response.status_code == 201, "创建角色失败"
        role_id = role_response.json()["data"]["id"]
        
        # 为用户分配角色
        role_assign_data = {
            "roleIds": [role_id]
        }
        
        response = requests.post(
            f"{self.BASE_URL}/users/{user_id}/roles",
            json=role_assign_data,
            headers=headers,
            timeout=10
        )
        
        assert response.status_code == 200, f"分配角色失败，状态码: {response.status_code}"
        
        # 验证角色已分配
        user_response = requests.get(
            f"{self.BASE_URL}/users/{user_id}",
            headers=headers,
            timeout=10
        )
        
        assert user_response.status_code == 200, "查询用户失败"
        user_data_result = user_response.json()
        assert role_id in user_data_result.get("data", {}).get("roleIds", []), "角色未正确分配给用户"
        
        print(f"✓ 角色分配给用户成功，用户ID: {user_id}, 角色ID: {role_id}")
        
        # 清理
        requests.delete(
            f"{self.BASE_URL}/users/{user_id}",
            headers=headers,
            timeout=10
        )


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
