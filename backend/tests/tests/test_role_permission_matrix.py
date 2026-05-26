#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
角色权限矩阵验证脚本
Sprint 27+1 测试环境

验证每个角色是否拥有正确的权限集合，确保权限矩阵符合设计规范。
"""

import pytest
import requests
import time
from typing import Dict, List, Set, Any


class TestRolePermissionMatrix:
    """角色权限矩阵验证测试类"""

    BASE_URL = "http://localhost:8080/api/v1"
    TOKEN = None

    # 预定义的权限矩阵（期望的规范）
    EXPECTED_PERMISSION_MATRIX = {
        "super_admin": {
            "permissions": [
                "user.create", "user.read", "user.update", "user.delete",
                "role.create", "role.read", "role.update", "role.delete",
                "permission.create", "permission.read", "permission.update", "permission.delete",
                "system.config", "system.log", "system.monitor",
                "order.create", "order.read", "order.update", "order.delete",
                "inventory.create", "inventory.read", "inventory.update", "inventory.delete",
                "report.read", "report.export",
                "audit.read"
            ],
            "description": "超级管理员 - 拥有系统所有权限"
        },
        "admin": {
            "permissions": [
                "user.create", "user.read", "user.update",
                "role.read", "role.update",
                "permission.read",
                "order.create", "order.read", "order.update",
                "inventory.create", "inventory.read", "inventory.update",
                "report.read", "report.export",
                "audit.read"
            ],
            "description": "管理员 - 拥有大部分管理权限，但不能删除核心数据"
        },
        "operator": {
            "permissions": [
                "user.read",
                "order.create", "order.read", "order.update",
                "inventory.read", "inventory.update",
                "report.read"
            ],
            "description": "操作员 - 日常业务操作权限"
        },
        "viewer": {
            "permissions": [
                "user.read",
                "order.read",
                "inventory.read",
                "report.read"
            ],
            "description": "观察员 - 只读权限"
        },
        "finance": {
            "permissions": [
                "user.read",
                "order.read",
                "report.read", "report.export",
                "audit.read"
            ],
            "description": "财务 - 查看订单和报表，导出数据"
        },
        "warehouse": {
            "permissions": [
                "inventory.create", "inventory.read", "inventory.update",
                "order.read"
            ],
            "description": "仓库管理员 - 库存管理和订单查看"
        }
    }

    @pytest.fixture(scope='class', autouse=True)
    def setup_class(self):
        """测试环境准备 - 获取认证Token并初始化角色数据"""
        login_data = {
            "username": "admin",
            "password": "Admin123!"
        }

        try:
            response = requests.post(
                f"{self.BASE_URL}/auth/login",
                json=login_data,
                timeout=10
            )

            if response.status_code == 200:
                self.TOKEN = response.json().get("data", {}).get("token")
                print("✓ 成功获取认证Token")
            else:
                pytest.fail(f"登录失败，状态码: {response.status_code}")
        except Exception as e:
            pytest.fail(f"登录请求失败: {str(e)}")

        yield

        # 清理逻辑
        self.TOKEN = None

    def _get_headers(self) -> Dict[str, str]:
        """获取认证请求头"""
        return {
            "Authorization": f"Bearer {self.TOKEN}",
            "Content-Type": "application/json"
        }

    def _create_role_if_not_exists(self, role_code: str, role_config: Dict) -> str:
        """如果角色不存在则创建，返回角色ID"""
        headers = self._get_headers()

        # 先查询角色是否存在
        search_response = requests.get(
            f"{self.BASE_URL}/roles/search?code={role_code}",
            headers=headers,
            timeout=10
        )

        if search_response.status_code == 200:
            data = search_response.json().get("data", {})
            items = data.get("items", [])
            if items:
                return items[0].get("id")

        # 创建角色
        role_data = {
            "name": role_config["description"].split(" - ")[0],
            "code": role_code,
            "description": role_config["description"],
            "permissions": role_config["permissions"]
        }

        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )

        if create_response.status_code in [200, 201]:
            return create_response.json().get("data", {}).get("id")
        else:
            pytest.fail(f"创建角色 {role_code} 失败: {create_response.status_code}")

    @pytest.mark.parametrize("role_code,config", EXPECTED_PERMISSION_MATRIX.items())
    def test_role_has_expected_permissions(self, role_code: str, config: Dict):
        """测试场景1: 验证每个角色拥有预期的权限集合"""
        role_id = self._create_role_if_not_exists(role_code, config)
        headers = self._get_headers()

        # 获取角色详情
        response = requests.get(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )

        assert response.status_code == 200, f"查询角色 {role_code} 失败"

        role_data = response.json().get("data", {})
        actual_permissions = set(role_data.get("permissions", []))
        expected_permissions = set(config["permissions"])

        # 验证权限完全匹配
        missing = expected_permissions - actual_permissions
        extra = actual_permissions - expected_permissions

        assert not missing, f"角色 {role_code} 缺少权限: {missing}"
        assert not extra, f"角色 {role_code} 拥有未授权的权限: {extra}"

        print(f"✓ 角色 {role_code} 权限矩阵验证通过，共 {len(expected_permissions)} 个权限")

    @pytest.mark.parametrize("role_code,config", EXPECTED_PERMISSION_MATRIX.items())
    def test_role_does_not_have_unauthorized_permissions(self, role_code: str, config: Dict):
        """测试场景2: 验证角色不包含未授权的权限"""
        role_id = self._create_role_if_not_exists(role_code, config)
        headers = self._get_headers()

        response = requests.get(
            f"{self.BASE_URL}/roles/{role_id}",
            headers=headers,
            timeout=10
        )

        assert response.status_code == 200

        role_data = response.json().get("data", {})
        actual_permissions = set(role_data.get("permissions", []))
        expected_permissions = set(config["permissions"])

        # 计算所有角色应有的权限并集
        all_expected_permissions = set()
        for rc, cfg in self.EXPECTED_PERMISSION_MATRIX.items():
            all_expected_permissions.update(cfg["permissions"])

        # 验证实际权限都在预期范围内
        unexpected = actual_permissions - all_expected_permissions
        assert not unexpected, f"角色 {role_code} 拥有系统未定义的权限: {unexpected}"

        print(f"✓ 角色 {role_code} 未授权权限验证通过")

    def test_permission_matrix_completeness(self):
        """测试场景3: 验证权限矩阵完整性 - 每个权限至少分配给一个角色"""
        headers = self._get_headers()

        # 获取所有角色
        response = requests.get(
            f"{self.BASE_URL}/roles?page=1&size=100",
            headers=headers,
            timeout=10
        )

        assert response.status_code == 200

        roles = response.json().get("data", {}).get("items", [])
        all_assigned_permissions = set()

        for role in roles:
            all_assigned_permissions.update(role.get("permissions", []))

        # 获取所有系统权限
        perm_response = requests.get(
            f"{self.BASE_URL}/permissions?page=1&size=100",
            headers=headers,
            timeout=10
        )

        assert perm_response.status_code == 200

        all_system_permissions = set()
        for perm in perm_response.json().get("data", {}).get("items", []):
            all_system_permissions.add(perm.get("code"))

        # 验证所有系统权限都已分配（排除系统保留权限）
        unassigned = all_system_permissions - all_assigned_permissions
        # 允许部分系统级权限未分配（如system.root等）
        acceptable_unassigned = {p for p in unassigned if p.startswith("system.")}
        problematic_unassigned = unassigned - acceptable_unassigned

        assert not problematic_unassigned, f"以下权限未分配给任何角色: {problematic_unassigned}"

        print(f"✓ 权限矩阵完整性验证通过，共 {len(all_system_permissions)} 个系统权限，"
              f"{len(all_assigned_permissions)} 个已分配")

    def test_role_permission_no_duplicates(self):
        """测试场景4: 验证角色权限列表无重复"""
        headers = self._get_headers()

        response = requests.get(
            f"{self.BASE_URL}/roles?page=1&size=100",
            headers=headers,
            timeout=10
        )

        assert response.status_code == 200

        roles = response.json().get("data", {}).get("items", [])
        issues = []

        for role in roles:
            permissions = role.get("permissions", [])
            if len(permissions) != len(set(permissions)):
                duplicates = [p for p in permissions if permissions.count(p) > 1]
                issues.append(f"角色 {role['code']} 存在重复权限: {set(duplicates)}")

        assert not issues, "; ".join(issues)
        print(f"✓ 已验证 {len(roles)} 个角色，权限列表无重复")

    def test_sensitive_permissions_restricted(self):
        """测试场景5: 验证敏感权限仅分配给高级角色"""
        sensitive_permissions = {
            "user.delete": ["super_admin"],
            "role.delete": ["super_admin"],
            "permission.delete": ["super_admin"],
            "system.config": ["super_admin"],
            "audit.read": ["super_admin", "finance"]
        }

        headers = self._get_headers()
        issues = []

        for perm_code, allowed_roles in sensitive_permissions.items():
            # 查询拥有此权限的所有角色
            response = requests.get(
                f"{self.BASE_URL}/roles?permission={perm_code}&page=1&size=100",
                headers=headers,
                timeout=10
            )

            if response.status_code != 200:
                continue

            roles = response.json().get("data", {}).get("items", [])
            for role in roles:
                if role["code"] not in allowed_roles:
                    issues.append(
                        f"敏感权限 {perm_code} 被分配给了非授权角色 {role['code']}"
                    )

        assert not issues, "敏感权限分配违规: " + "; ".join(issues)
        print("✓ 敏感权限分配验证通过")

    def test_matrix_consistency_after_update(self):
        """测试场景6: 验证更新角色权限后矩阵仍保持一致性"""
        headers = self._get_headers()
        role_code = f"matrix_test_{int(time.time())}"

        # 创建测试角色
        role_data = {
            "name": "矩阵一致性测试角色",
            "code": role_code,
            "description": "用于测试权限矩阵一致性",
            "permissions": ["user.read", "order.read"]
        }

        create_response = requests.post(
            f"{self.BASE_URL}/roles",
            json=role_data,
            headers=headers,
            timeout=10
        )

        assert create_response.status_code in [200, 201]
        role_id = create_response.json().get("data", {}).get("id")

        try:
            # 更新权限
            update_data = {
                "permissions": ["user.read", "user.update", "order.read", "inventory.read"]
            }

            update_response = requests.put(
                f"{self.BASE_URL}/roles/{role_id}",
                json=update_data,
                headers=headers,
                timeout=10
            )

            assert update_response.status_code == 200

            # 重新查询验证
            query_response = requests.get(
                f"{self.BASE_URL}/roles/{role_id}",
                headers=headers,
                timeout=10
            )

            assert query_response.status_code == 200
            updated_permissions = set(query_response.json().get("data", {}).get("permissions", []))
            expected = set(update_data["permissions"])

            assert updated_permissions == expected, (
                f"更新后权限不匹配: 期望 {expected}, 实际 {updated_permissions}"
            )

            print("✓ 权限矩阵更新一致性验证通过")

        finally:
            # 清理
            requests.delete(
                f"{self.BASE_URL}/roles/{role_id}",
                headers=headers,
                timeout=10
            )


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
