#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
权限管理测试数据生成器
Sprint 27+1 测试环境

提供用户权限管理自动化测试所需的测试数据：
- 测试用户数据
- 测试角色数据
- 测试权限数据
"""

import time
from typing import Dict, Any, List, Optional


class PermissionTestData:
    """权限管理测试数据生成器"""

    # 标准权限定义
    STANDARD_PERMISSIONS = {
        "user": {
            "resource": "user",
            "actions": ["create", "read", "update", "delete"],
            "codes": ["user.create", "user.read", "user.update", "user.delete"],
            "descriptions": {
                "user.create": "创建用户",
                "user.read": "查看用户",
                "user.update": "编辑用户",
                "user.delete": "删除用户"
            }
        },
        "role": {
            "resource": "role",
            "actions": ["create", "read", "update", "delete"],
            "codes": ["role.create", "role.read", "role.update", "role.delete"],
            "descriptions": {
                "role.create": "创建角色",
                "role.read": "查看角色",
                "role.update": "编辑角色",
                "role.delete": "删除角色"
            }
        },
        "permission": {
            "resource": "permission",
            "actions": ["create", "read", "update", "delete"],
            "codes": ["permission.create", "permission.read", "permission.update", "permission.delete"],
            "descriptions": {
                "permission.create": "创建权限",
                "permission.read": "查看权限",
                "permission.update": "编辑权限",
                "permission.delete": "删除权限"
            }
        },
        "order": {
            "resource": "order",
            "actions": ["create", "read", "update", "delete"],
            "codes": ["order.create", "order.read", "order.update", "order.delete"],
            "descriptions": {
                "order.create": "创建订单",
                "order.read": "查看订单",
                "order.update": "编辑订单",
                "order.delete": "删除订单"
            }
        },
        "inventory": {
            "resource": "inventory",
            "actions": ["create", "read", "update", "delete"],
            "codes": ["inventory.create", "inventory.read", "inventory.update", "inventory.delete"],
            "descriptions": {
                "inventory.create": "创建库存",
                "inventory.read": "查看库存",
                "inventory.update": "编辑库存",
                "inventory.delete": "删除库存"
            }
        },
        "report": {
            "resource": "report",
            "actions": ["read", "export"],
            "codes": ["report.read", "report.export"],
            "descriptions": {
                "report.read": "查看报表",
                "report.export": "导出报表"
            }
        },
        "system": {
            "resource": "system",
            "actions": ["config", "log", "monitor"],
            "codes": ["system.config", "system.log", "system.monitor"],
            "descriptions": {
                "system.config": "系统配置",
                "system.log": "查看日志",
                "system.monitor": "系统监控"
            }
        },
        "audit": {
            "resource": "audit",
            "actions": ["read"],
            "codes": ["audit.read"],
            "descriptions": {
                "audit.read": "查看审计日志"
            }
        }
    }

    # 预定义角色模板
    ROLE_TEMPLATES = {
        "super_admin": {
            "name": "超级管理员",
            "code": "super_admin",
            "description": "超级管理员 - 拥有系统所有权限",
            "permissions": [
                "user.create", "user.read", "user.update", "user.delete",
                "role.create", "role.read", "role.update", "role.delete",
                "permission.create", "permission.read", "permission.update", "permission.delete",
                "system.config", "system.log", "system.monitor",
                "order.create", "order.read", "order.update", "order.delete",
                "inventory.create", "inventory.read", "inventory.update", "inventory.delete",
                "report.read", "report.export",
                "audit.read"
            ]
        },
        "admin": {
            "name": "管理员",
            "code": "admin",
            "description": "管理员 - 拥有大部分管理权限，但不能删除核心数据",
            "permissions": [
                "user.create", "user.read", "user.update",
                "role.read", "role.update",
                "permission.read",
                "order.create", "order.read", "order.update",
                "inventory.create", "inventory.read", "inventory.update",
                "report.read", "report.export",
                "audit.read"
            ]
        },
        "operator": {
            "name": "操作员",
            "code": "operator",
            "description": "操作员 - 日常业务操作权限",
            "permissions": [
                "user.read",
                "order.create", "order.read", "order.update",
                "inventory.read", "inventory.update",
                "report.read"
            ]
        },
        "viewer": {
            "name": "观察员",
            "code": "viewer",
            "description": "观察员 - 只读权限",
            "permissions": [
                "user.read",
                "order.read",
                "inventory.read",
                "report.read"
            ]
        },
        "finance": {
            "name": "财务",
            "code": "finance",
            "description": "财务 - 查看订单和报表，导出数据",
            "permissions": [
                "user.read",
                "order.read",
                "report.read", "report.export",
                "audit.read"
            ]
        },
        "warehouse": {
            "name": "仓库管理员",
            "code": "warehouse",
            "description": "仓库管理员 - 库存管理和订单查看",
            "permissions": [
                "inventory.create", "inventory.read", "inventory.update",
                "order.read"
            ]
        }
    }

    # 测试用户模板
    USER_TEMPLATES = {
        "admin": {
            "username": "test_admin",
            "password": "Admin123!",
            "email": "test_admin@example.com",
            "realName": "测试管理员",
            "phone": "13800138000"
        },
        "operator": {
            "username": "test_operator",
            "password": "Operator123!",
            "email": "test_operator@example.com",
            "realName": "测试操作员",
            "phone": "13800138001"
        },
        "viewer": {
            "username": "test_viewer",
            "password": "Viewer123!",
            "email": "test_viewer@example.com",
            "realName": "测试观察员",
            "phone": "13800138002"
        },
        "finance": {
            "username": "test_finance",
            "password": "Finance123!",
            "email": "test_finance@example.com",
            "realName": "测试财务",
            "phone": "13800138003"
        },
        "warehouse": {
            "username": "test_warehouse",
            "password": "Warehouse123!",
            "email": "test_warehouse@example.com",
            "realName": "测试仓库管理员",
            "phone": "13800138004"
        }
    }

    def __init__(self):
        self.timestamp = int(time.time())

    def get_standard_permissions(self) -> List[Dict[str, Any]]:
        """获取所有标准权限数据"""
        permissions = []
        for resource, config in self.STANDARD_PERMISSIONS.items():
            for i, action in enumerate(config["actions"]):
                code = config["codes"][i]
                permissions.append({
                    "name": config["descriptions"].get(code, f"{resource}.{action}"),
                    "code": code,
                    "resource": resource,
                    "action": action,
                    "description": config["descriptions"].get(code, "")
                })
        return permissions

    def get_role_templates(self) -> Dict[str, Dict[str, Any]]:
        """获取角色模板"""
        return self.ROLE_TEMPLATES

    def get_user_templates(self) -> Dict[str, Dict[str, Any]]:
        """获取用户模板"""
        return self.USER_TEMPLATES

    def generate_test_role(self, template_key: str = None, suffix: str = None,
                           parent_id: str = None, extra_permissions: List[str] = None) -> Dict[str, Any]:
        """
        生成测试角色数据

        Args:
            template_key: 使用预定义模板（super_admin/admin/operator/viewer/finance/warehouse）
            suffix: 自定义后缀，避免命名冲突
            parent_id: 父角色ID（用于继承测试）
            extra_permissions: 额外权限（在模板基础上增加）
        """
        ts = suffix or str(int(time.time()))

        if template_key and template_key in self.ROLE_TEMPLATES:
            template = self.ROLE_TEMPLATES[template_key].copy()
            template["code"] = f"{template['code']}_{ts}"
            template["name"] = f"{template['name']}_{ts}"
        else:
            template = {
                "name": f"测试角色_{ts}",
                "code": f"test_role_{ts}",
                "description": f"自动生成的测试角色 - {ts}",
                "permissions": ["user.read"]
            }

        if extra_permissions:
            perms = set(template.get("permissions", []))
            perms.update(extra_permissions)
            template["permissions"] = list(perms)

        if parent_id:
            template["parentId"] = parent_id

        return template

    def generate_test_user(self, template_key: str = None, suffix: str = None) -> Dict[str, Any]:
        """
        生成测试用户数据

        Args:
            template_key: 使用预定义模板（admin/operator/viewer/finance/warehouse）
            suffix: 自定义后缀
        """
        ts = suffix or str(int(time.time()))

        if template_key and template_key in self.USER_TEMPLATES:
            template = self.USER_TEMPLATES[template_key].copy()
            template["username"] = f"{template['username']}_{ts}"
            template["email"] = f"test_{template_key}_{ts}@example.com"
        else:
            template = {
                "username": f"test_user_{ts}",
                "password": "Test123!",
                "email": f"test_user_{ts}@example.com",
                "realName": f"测试用户_{ts}",
                "phone": f"138{ts[-8:]:0>8}" if len(ts) >= 8 else f"138{ts:0>8}"
            }

        return template

    def generate_permission_matrix_test_data(self) -> Dict[str, Any]:
        """
        生成权限矩阵测试所需数据
        """
        return {
            "roles": [
                self.generate_test_role("super_admin", "matrix_1"),
                self.generate_test_role("admin", "matrix_1"),
                self.generate_test_role("operator", "matrix_1"),
                self.generate_test_role("viewer", "matrix_1"),
            ],
            "expected_matrix": {
                "super_admin": self.ROLE_TEMPLATES["super_admin"]["permissions"],
                "admin": self.ROLE_TEMPLATES["admin"]["permissions"],
                "operator": self.ROLE_TEMPLATES["operator"]["permissions"],
                "viewer": self.ROLE_TEMPLATES["viewer"]["permissions"],
            }
        }

    def generate_inheritance_test_data(self) -> Dict[str, Any]:
        """
        生成权限继承测试所需数据
        """
        ts = str(int(time.time()))

        grandparent = self.generate_test_role(
            suffix=f"grandparent_{ts}",
            extra_permissions=["user.read", "system.log"]
        )

        parent = self.generate_test_role(
            suffix=f"parent_{ts}",
            extra_permissions=["order.read"]
        )

        child = self.generate_test_role(
            suffix=f"child_{ts}",
            extra_permissions=["inventory.read", "user.update"]
        )

        return {
            "grandparent": grandparent,
            "parent": parent,
            "child": child,
            "expected_inherited": {
                "parent": {"user.read", "system.log", "order.read"},
                "child": {"user.read", "system.log", "order.read", "inventory.read", "user.update"}
            }
        }

    def generate_audit_test_data(self) -> Dict[str, Any]:
        """
        生成审计测试所需数据
        """
        ts = str(int(time.time()))

        role = self.generate_test_role(
            suffix=f"audit_{ts}",
            extra_permissions=["user.read", "order.read"]
        )

        user = self.generate_test_user(suffix=f"audit_{ts}")

        return {
            "role": role,
            "user": user,
            "operations": [
                {"action": "create", "resourceType": "role", "description": "创建角色"},
                {"action": "assign", "resourceType": "role_permission", "description": "分配权限"},
                {"action": "revoke", "resourceType": "role_permission", "description": "回收权限"},
                {"action": "assign", "resourceType": "user_role", "description": "分配用户角色"},
                {"action": "delete", "resourceType": "role", "description": "删除角色"},
            ]
        }

    def get_sensitive_permissions(self) -> Dict[str, List[str]]:
        """获取敏感权限及其允许分配的角色"""
        return {
            "user.delete": ["super_admin"],
            "role.delete": ["super_admin"],
            "permission.delete": ["super_admin"],
            "system.config": ["super_admin"],
            "audit.read": ["super_admin", "finance"]
        }

    def get_all_permission_codes(self) -> List[str]:
        """获取所有权限编码"""
        codes = []
        for resource, config in self.STANDARD_PERMISSIONS.items():
            codes.extend(config["codes"])
        return codes


# 全局测试数据实例
permission_test_data = PermissionTestData()


if __name__ == "__main__":
    # 测试数据生成示例
    data = PermissionTestData()

    print("=" * 60)
    print("权限管理测试数据")
    print("=" * 60)

    print("\n1. 标准权限列表:")
    for perm in data.get_standard_permissions()[:5]:
        print(f"   {perm['code']}: {perm['name']}")
    print(f"   ... 共 {len(data.get_standard_permissions())} 个权限")

    print("\n2. 角色模板:")
    for key, role in data.get_role_templates().items():
        print(f"   {key}: {role['name']} ({len(role['permissions'])} 个权限)")

    print("\n3. 测试角色示例:")
    test_role = data.generate_test_role("operator", "demo_001")
    print(f"   编码: {test_role['code']}")
    print(f"   权限: {test_role['permissions']}")

    print("\n4. 测试用户示例:")
    test_user = data.generate_test_user("admin", "demo_001")
    print(f"   用户名: {test_user['username']}")
    print(f"   邮箱: {test_user['email']}")

    print("\n5. 继承测试数据:")
    inherit_data = data.generate_inheritance_test_data()
    print(f"   祖父角色: {inherit_data['grandparent']['code']}")
    print(f"   父角色: {inherit_data['parent']['code']}")
    print(f"   子角色: {inherit_data['child']['code']}")
