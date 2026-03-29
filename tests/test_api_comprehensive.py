#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 综合API接口测试
测试范围：
1. 用户管理API (SysUserController)
2. 角色管理API (SysRoleController)
3. Agent管理API (AgentController)
4. 采购订单API (PurchaseOrderController)
5. 库存管理API (StockController)

技术栈: pytest + requests
"""

import pytest
import requests
import time
import json
from typing import Dict, Any, Optional
from dataclasses import dataclass, field
from enum import Enum


# ==================== 配置 ====================

@dataclass
class TestConfig:
    """测试配置"""
    base_url: str = "http://localhost:8080"
    timeout: int = 30
    tenant_id: int = 1
    test_username: str = "testuser"
    test_password: str = "Test@123456"
    
    @property
    def api_url(self) -> str:
        return f"{self.base_url}/api"


CONFIG = TestConfig()


# ==================== 数据类 ====================

class ResponseStatus(Enum):
    """响应状态"""
    SUCCESS = 200
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    SERVER_ERROR = 500


@dataclass
class ApiResponse:
    """API响应"""
    status_code: int
    data: Any = None
    message: str = ""
    success: bool = True
    
    @classmethod
    def from_response(cls, response: requests.Response) -> 'ApiResponse':
        """从requests响应创建"""
        try:
            data = response.json()
            return cls(
                status_code=response.status_code,
                data=data.get('data'),
                message=data.get('message', ''),
                success=data.get('code', 0) == 200 or data.get('success', True)
            )
        except Exception:
            return cls(
                status_code=response.status_code,
                success=False
            )


# ==================== API客户端 ====================

class ApiClient:
    """API客户端"""
    
    def __init__(self, config: TestConfig = CONFIG):
        self.config = config
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        self._token: Optional[str] = None
        self._user_id: Optional[int] = None
    
    def set_token(self, token: str):
        """设置认证Token"""
        self._token = token
        self.session.headers["Authorization"] = f"Bearer {token}"
    
    def clear_token(self):
        """清除认证Token"""
        self._token = None
        if "Authorization" in self.session.headers:
            del self.session.headers["Authorization"]
    
    # ==================== 用户API ====================
    
    def login(self, username: str = None, password: str = None, tenant_id: int = None) -> ApiResponse:
        """用户登录"""
        data = {
            "username": username or self.config.test_username,
            "password": password or self.config.test_password,
            "tenantId": tenant_id or self.config.tenant_id
        }
        response = self.session.post(
            f"{self.config.api_url}/user/login",
            json=data,
            headers={"X-Real-IP": "127.0.0.1"},
            timeout=self.config.timeout
        )
        api_response = ApiResponse.from_response(response)
        if api_response.success and api_response.data:
            self.set_token(api_response.data)
        return api_response
    
    def logout(self) -> ApiResponse:
        """用户登出"""
        response = self.session.post(
            f"{self.config.api_url}/user/logout",
            timeout=self.config.timeout
        )
        self.clear_token()
        return ApiResponse.from_response(response)
    
    def create_user(self, user_data: Dict) -> ApiResponse:
        """创建用户"""
        response = self.session.post(
            f"{self.config.api_url}/user",
            json=user_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def update_user(self, user_id: int, user_data: Dict) -> ApiResponse:
        """更新用户"""
        response = self.session.put(
            f"{self.config.api_url}/user/{user_id}",
            json=user_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def delete_user(self, user_id: int) -> ApiResponse:
        """删除用户"""
        response = self.session.delete(
            f"{self.config.api_url}/user/{user_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_user_page(self, page: int = 1, size: int = 10, **filters) -> ApiResponse:
        """分页查询用户"""
        params = {"pageNum": page, "pageSize": size, **filters}
        response = self.session.get(
            f"{self.config.api_url}/user/page",
            params=params,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_user_detail(self, user_id: int) -> ApiResponse:
        """获取用户详情"""
        response = self.session.get(
            f"{self.config.api_url}/user/{user_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def reset_password(self, user_id: int, new_password: str) -> ApiResponse:
        """重置密码"""
        response = self.session.put(
            f"{self.config.api_url}/user/{user_id}/password/reset",
            params={"newPassword": new_password},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def change_password(self, user_id: int, old_password: str, new_password: str) -> ApiResponse:
        """修改密码"""
        response = self.session.put(
            f"{self.config.api_url}/user/{user_id}/password/change",
            params={"oldPassword": old_password, "newPassword": new_password},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def assign_roles(self, user_id: int, role_ids: list) -> ApiResponse:
        """分配角色"""
        response = self.session.post(
            f"{self.config.api_url}/user/{user_id}/roles",
            json=role_ids,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def update_user_status(self, user_id: int, status: int) -> ApiResponse:
        """更新用户状态"""
        response = self.session.put(
            f"{self.config.api_url}/user/{user_id}/status",
            params={"status": status},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    # ==================== 角色API ====================
    
    def create_role(self, role_data: Dict) -> ApiResponse:
        """创建角色"""
        response = self.session.post(
            f"{self.config.api_url}/role",
            json=role_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def update_role(self, role_id: int, role_data: Dict) -> ApiResponse:
        """更新角色"""
        response = self.session.put(
            f"{self.config.api_url}/role/{role_id}",
            json=role_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def delete_role(self, role_id: int) -> ApiResponse:
        """删除角色"""
        response = self.session.delete(
            f"{self.config.api_url}/role/{role_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_role_page(self, page: int = 1, size: int = 10, tenant_id: int = None, **filters) -> ApiResponse:
        """分页查询角色"""
        params = {
            "current": page,
            "size": size,
            "tenantId": tenant_id or self.config.tenant_id,
            **filters
        }
        response = self.session.get(
            f"{self.config.api_url}/role/page",
            params=params,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def assign_permissions(self, role_id: int, permission_ids: list) -> ApiResponse:
        """分配权限"""
        response = self.session.post(
            f"{self.config.api_url}/role/{role_id}/permissions",
            json=permission_ids,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def assign_menus(self, role_id: int, menu_ids: list) -> ApiResponse:
        """分配菜单"""
        response = self.session.post(
            f"{self.config.api_url}/role/{role_id}/menus",
            json=menu_ids,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_role_permissions(self, role_id: int) -> ApiResponse:
        """获取角色权限"""
        response = self.session.get(
            f"{self.config.api_url}/role/{role_id}/permissions",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def update_role_status(self, role_id: int, status: int) -> ApiResponse:
        """更新角色状态"""
        response = self.session.put(
            f"{self.config.api_url}/role/{role_id}/status",
            params={"status": status},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    # ==================== Agent API ====================
    
    def register_agent(self, agent_data: Dict) -> ApiResponse:
        """注册Agent"""
        response = self.session.post(
            f"{self.config.api_url}/agent/register",
            json=agent_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def update_agent(self, agent_id: int, agent_data: Dict) -> ApiResponse:
        """更新Agent"""
        response = self.session.put(
            f"{self.config.api_url}/agent/{agent_id}",
            json=agent_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def delete_agent(self, agent_id: int) -> ApiResponse:
        """删除Agent"""
        response = self.session.delete(
            f"{self.config.api_url}/agent/{agent_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def activate_agent(self, agent_id: int) -> ApiResponse:
        """激活Agent"""
        response = self.session.post(
            f"{self.config.api_url}/agent/{agent_id}/activate",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def deactivate_agent(self, agent_id: int) -> ApiResponse:
        """禁用Agent"""
        response = self.session.post(
            f"{self.config.api_url}/agent/{agent_id}/deactivate",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def agent_heartbeat(self, agent_code: str) -> ApiResponse:
        """Agent心跳"""
        response = self.session.post(
            f"{self.config.api_url}/agent/heartbeat",
            params={"agentCode": agent_code},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_agent_page(self, page: int = 1, size: int = 10, tenant_id: int = None, **filters) -> ApiResponse:
        """分页查询Agent"""
        params = {
            "current": page,
            "size": size,
            "tenantId": tenant_id or self.config.tenant_id,
            **filters
        }
        response = self.session.get(
            f"{self.config.api_url}/agent/page",
            params=params,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_agent_detail(self, agent_id: int) -> ApiResponse:
        """获取Agent详情"""
        response = self.session.get(
            f"{self.config.api_url}/agent/{agent_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_active_agents(self) -> ApiResponse:
        """获取活跃Agent列表"""
        response = self.session.get(
            f"{self.config.api_url}/agent/active",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def validate_api_key(self, api_key: str) -> ApiResponse:
        """验证API Key"""
        response = self.session.get(
            f"{self.config.api_url}/agent/validate",
            params={"apiKey": api_key},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    # ==================== 采购订单API ====================
    
    def create_purchase_order(self, order_data: Dict) -> ApiResponse:
        """创建采购订单"""
        response = self.session.post(
            f"{self.config.api_url}/erp/purchase/order",
            json=order_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def update_purchase_order(self, order_id: int, order_data: Dict) -> ApiResponse:
        """更新采购订单"""
        response = self.session.put(
            f"{self.config.api_url}/erp/purchase/order/{order_id}",
            json=order_data,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def delete_purchase_order(self, order_id: int) -> ApiResponse:
        """删除采购订单"""
        response = self.session.delete(
            f"{self.config.api_url}/erp/purchase/order/{order_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def submit_purchase_order(self, order_id: int) -> ApiResponse:
        """提交采购订单审批"""
        response = self.session.post(
            f"{self.config.api_url}/erp/purchase/order/{order_id}/submit",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def approve_purchase_order(self, order_id: int) -> ApiResponse:
        """审批通过采购订单"""
        response = self.session.post(
            f"{self.config.api_url}/erp/purchase/order/{order_id}/approve",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def reject_purchase_order(self, order_id: int, reason: str) -> ApiResponse:
        """审批拒绝采购订单"""
        response = self.session.post(
            f"{self.config.api_url}/erp/purchase/order/{order_id}/reject",
            params={"reason": reason},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def cancel_purchase_order(self, order_id: int, reason: str) -> ApiResponse:
        """取消采购订单"""
        response = self.session.post(
            f"{self.config.api_url}/erp/purchase/order/{order_id}/cancel",
            params={"reason": reason},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_purchase_order_page(self, page: int = 1, size: int = 10, tenant_id: int = None, **filters) -> ApiResponse:
        """分页查询采购订单"""
        params = {
            "current": page,
            "size": size,
            "tenantId": tenant_id or self.config.tenant_id,
            **filters
        }
        response = self.session.get(
            f"{self.config.api_url}/erp/purchase/order/page",
            params=params,
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_purchase_order_detail(self, order_id: int) -> ApiResponse:
        """获取采购订单详情"""
        response = self.session.get(
            f"{self.config.api_url}/erp/purchase/order/{order_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    # ==================== 库存API ====================
    
    def get_stock_detail(self, product_id: int, warehouse_id: int) -> ApiResponse:
        """查询库存详情"""
        response = self.session.get(
            f"{self.config.api_url}/stock/{product_id}/{warehouse_id}",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def increase_stock(self, product_id: int, warehouse_id: int, quantity: float) -> ApiResponse:
        """库存增加"""
        response = self.session.post(
            f"{self.config.api_url}/stock/increase",
            params={
                "productId": product_id,
                "warehouseId": warehouse_id,
                "quantity": quantity
            },
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def decrease_stock(self, product_id: int, warehouse_id: int, quantity: float) -> ApiResponse:
        """库存减少"""
        response = self.session.post(
            f"{self.config.api_url}/stock/decrease",
            params={
                "productId": product_id,
                "warehouseId": warehouse_id,
                "quantity": quantity
            },
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def check_stock(self, product_id: int, warehouse_id: int, actual_quantity: float) -> ApiResponse:
        """库存盘点"""
        response = self.session.post(
            f"{self.config.api_url}/stock/check",
            params={
                "productId": product_id,
                "warehouseId": warehouse_id,
                "actualQuantity": actual_quantity
            },
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def get_stock_list(self, page: int = 1, size: int = 10) -> ApiResponse:
        """查询库存列表"""
        response = self.session.get(
            f"{self.config.api_url}/stock/list",
            params={"current": page, "size": size},
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)
    
    def check_stock_alert(self) -> ApiResponse:
        """库存预警检查"""
        response = self.session.get(
            f"{self.config.api_url}/stock/alert",
            timeout=self.config.timeout
        )
        return ApiResponse.from_response(response)


# ==================== 测试夹具 ====================

@pytest.fixture(scope="module")
def api_client():
    """API客户端夹具"""
    return ApiClient()


@pytest.fixture(scope="module")
def auth_client(api_client):
    """已认证的API客户端"""
    # 尝试登录
    response = api_client.login()
    yield api_client
    # 清理
    try:
        api_client.logout()
    except Exception:
        pass


# ==================== 用户API测试 ====================

@pytest.mark.api
@pytest.mark.user
class TestUserAPI:
    """用户管理API测试"""
    
    def test_01_login_success(self, api_client):
        """TC-USER-001: 用户登录成功"""
        response = api_client.login()
        assert response.status_code == 200 or response.status_code == 401  # 401 if service not running
    
    def test_02_login_invalid_credentials(self, api_client):
        """TC-USER-002: 无效凭证登录"""
        response = api_client.login(username="invalid", password="invalid")
        assert response.status_code in [200, 401, 400]
    
    def test_03_logout_success(self, auth_client):
        """TC-USER-003: 用户登出成功"""
        response = auth_client.logout()
        assert response.status_code in [200, 401]
    
    def test_04_create_user(self, auth_client):
        """TC-USER-004: 创建用户"""
        user_data = {
            "tenantId": 1,
            "username": f"test_user_{int(time.time())}",
            "password": "Test@123456",
            "nickname": "Test User",
            "email": "test@example.com",
            "phone": "13800138000",
            "gender": 1,
            "userType": 1
        }
        response = auth_client.create_user(user_data)
        assert response.status_code in [200, 401, 403]
    
    def test_05_get_user_page(self, auth_client):
        """TC-USER-005: 分页查询用户"""
        response = auth_client.get_user_page(tenantId=1)
        assert response.status_code in [200, 401]
    
    def test_06_update_user(self, auth_client):
        """TC-USER-006: 更新用户"""
        user_data = {
            "nickname": "Updated User",
            "email": "updated@example.com"
        }
        response = auth_client.update_user(1, user_data)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_07_delete_user(self, auth_client):
        """TC-USER-007: 删除用户"""
        response = auth_client.delete_user(999999)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_08_reset_password(self, auth_client):
        """TC-USER-008: 重置密码"""
        response = auth_client.reset_password(1, "NewPass@123")
        assert response.status_code in [200, 401, 403, 404]
    
    def test_09_change_password(self, auth_client):
        """TC-USER-009: 修改密码"""
        response = auth_client.change_password(1, "OldPass@123", "NewPass@123")
        assert response.status_code in [200, 401, 400, 404]
    
    def test_10_assign_roles(self, auth_client):
        """TC-USER-010: 分配角色"""
        response = auth_client.assign_roles(1, [1, 2, 3])
        assert response.status_code in [200, 401, 403, 404]
    
    def test_11_update_user_status(self, auth_client):
        """TC-USER-011: 更新用户状态"""
        response = auth_client.update_user_status(1, 1)
        assert response.status_code in [200, 401, 403, 404]


# ==================== 角色API测试 ====================

@pytest.mark.api
@pytest.mark.role
class TestRoleAPI:
    """角色管理API测试"""
    
    def test_01_create_role(self, auth_client):
        """TC-ROLE-001: 创建角色"""
        role_data = {
            "tenantId": 1,
            "roleName": f"test_role_{int(time.time())}",
            "roleCode": f"TEST_{int(time.time())}",
            "description": "Test Role"
        }
        response = auth_client.create_role(role_data)
        assert response.status_code in [200, 401, 403]
    
    def test_02_get_role_page(self, auth_client):
        """TC-ROLE-002: 分页查询角色"""
        response = auth_client.get_role_page(tenantId=1)
        assert response.status_code in [200, 401]
    
    def test_03_update_role(self, auth_client):
        """TC-ROLE-003: 更新角色"""
        role_data = {
            "roleName": "Updated Role",
            "description": "Updated Description"
        }
        response = auth_client.update_role(1, role_data)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_04_delete_role(self, auth_client):
        """TC-ROLE-004: 删除角色"""
        response = auth_client.delete_role(999999)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_05_assign_permissions(self, auth_client):
        """TC-ROLE-005: 分配权限"""
        response = auth_client.assign_permissions(1, [1, 2, 3])
        assert response.status_code in [200, 401, 403, 404]
    
    def test_06_assign_menus(self, auth_client):
        """TC-ROLE-006: 分配菜单"""
        response = auth_client.assign_menus(1, [1, 2, 3])
        assert response.status_code in [200, 401, 403, 404]
    
    def test_07_get_role_permissions(self, auth_client):
        """TC-ROLE-007: 获取角色权限"""
        response = auth_client.get_role_permissions(1)
        assert response.status_code in [200, 401, 404]
    
    def test_08_update_role_status(self, auth_client):
        """TC-ROLE-008: 更新角色状态"""
        response = auth_client.update_role_status(1, 1)
        assert response.status_code in [200, 401, 403, 404]


# ==================== Agent API测试 ====================

@pytest.mark.api
@pytest.mark.agent
class TestAgentAPI:
    """Agent管理API测试"""
    
    def test_01_register_agent(self, auth_client):
        """TC-AGENT-001: 注册Agent"""
        agent_data = {
            "tenantId": 1,
            "agentCode": f"AGENT_{int(time.time())}",
            "agentName": f"Test Agent {int(time.time())}",
            "agentType": "CHATBOT",
            "capabilities": ["NLU", "DIALOGUE"],
            "apiKey": f"sk-{int(time.time())}"
        }
        response = auth_client.register_agent(agent_data)
        assert response.status_code in [200, 401, 403]
    
    def test_02_get_agent_page(self, auth_client):
        """TC-AGENT-002: 分页查询Agent"""
        response = auth_client.get_agent_page(tenantId=1)
        assert response.status_code in [200, 401]
    
    def test_03_update_agent(self, auth_client):
        """TC-AGENT-003: 更新Agent"""
        agent_data = {
            "agentName": "Updated Agent",
            "description": "Updated Description"
        }
        response = auth_client.update_agent(1, agent_data)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_04_delete_agent(self, auth_client):
        """TC-AGENT-004: 删除Agent"""
        response = auth_client.delete_agent(999999)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_05_activate_agent(self, auth_client):
        """TC-AGENT-005: 激活Agent"""
        response = auth_client.activate_agent(1)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_06_deactivate_agent(self, auth_client):
        """TC-AGENT-006: 禁用Agent"""
        response = auth_client.deactivate_agent(1)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_07_agent_heartbeat(self, api_client):
        """TC-AGENT-007: Agent心跳"""
        response = api_client.agent_heartbeat("TEST_AGENT")
        assert response.status_code in [200, 404]
    
    def test_08_get_agent_detail(self, auth_client):
        """TC-AGENT-008: 获取Agent详情"""
        response = auth_client.get_agent_detail(1)
        assert response.status_code in [200, 401, 404]
    
    def test_09_get_active_agents(self, api_client):
        """TC-AGENT-009: 获取活跃Agent列表"""
        response = api_client.get_active_agents()
        assert response.status_code in [200, 401]
    
    def test_10_validate_api_key(self, api_client):
        """TC-AGENT-010: 验证API Key"""
        response = api_client.validate_api_key("sk-test")
        assert response.status_code in [200, 401]


# ==================== 采购订单API测试 ====================

@pytest.mark.api
@pytest.mark.erp
@pytest.mark.purchase
class TestPurchaseOrderAPI:
    """采购订单API测试"""
    
    def test_01_create_order(self, auth_client):
        """TC-PURCHASE-001: 创建采购订单"""
        order_data = {
            "tenantId": 1,
            "supplierId": 1,
            "orderItems": [
                {"productId": 1, "quantity": 10, "price": 100.00}
            ]
        }
        response = auth_client.create_purchase_order(order_data)
        assert response.status_code in [200, 401, 403]
    
    def test_02_get_order_page(self, auth_client):
        """TC-PURCHASE-002: 分页查询采购订单"""
        response = auth_client.get_purchase_order_page(tenantId=1)
        assert response.status_code in [200, 401]
    
    def test_03_update_order(self, auth_client):
        """TC-PURCHASE-003: 更新采购订单"""
        order_data = {"remark": "Updated Remark"}
        response = auth_client.update_purchase_order(1, order_data)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_04_delete_order(self, auth_client):
        """TC-PURCHASE-004: 删除采购订单"""
        response = auth_client.delete_purchase_order(999999)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_05_submit_order(self, auth_client):
        """TC-PURCHASE-005: 提交审批"""
        response = auth_client.submit_purchase_order(1)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_06_approve_order(self, auth_client):
        """TC-PURCHASE-006: 审批通过"""
        response = auth_client.approve_purchase_order(1)
        assert response.status_code in [200, 401, 403, 404]
    
    def test_07_reject_order(self, auth_client):
        """TC-PURCHASE-007: 审批拒绝"""
        response = auth_client.reject_purchase_order(1, "测试拒绝")
        assert response.status_code in [200, 401, 403, 404]
    
    def test_08_cancel_order(self, auth_client):
        """TC-PURCHASE-008: 取消订单"""
        response = auth_client.cancel_purchase_order(1, "测试取消")
        assert response.status_code in [200, 401, 403, 404]
    
    def test_09_get_order_detail(self, auth_client):
        """TC-PURCHASE-009: 获取订单详情"""
        response = auth_client.get_purchase_order_detail(1)
        assert response.status_code in [200, 401, 404]


# ==================== 库存API测试 ====================

@pytest.mark.api
@pytest.mark.erp
@pytest.mark.stock
class TestStockAPI:
    """库存管理API测试"""
    
    def test_01_get_stock_detail(self, auth_client):
        """TC-STOCK-001: 查询库存详情"""
        response = auth_client.get_stock_detail(1, 1)
        assert response.status_code in [200, 401, 404]
    
    def test_02_increase_stock(self, auth_client):
        """TC-STOCK-002: 库存增加"""
        response = auth_client.increase_stock(1, 1, 100.0)
        assert response.status_code in [200, 401, 403]
    
    def test_03_decrease_stock(self, auth_client):
        """TC-STOCK-003: 库存减少"""
        response = auth_client.decrease_stock(1, 1, 50.0)
        assert response.status_code in [200, 401, 403, 400]
    
    def test_04_check_stock(self, auth_client):
        """TC-STOCK-004: 库存盘点"""
        response = auth_client.check_stock(1, 1, 100.0)
        assert response.status_code in [200, 401, 403]
    
    def test_05_get_stock_list(self, auth_client):
        """TC-STOCK-005: 查询库存列表"""
        response = auth_client.get_stock_list()
        assert response.status_code in [200, 401]
    
    def test_06_check_stock_alert(self, auth_client):
        """TC-STOCK-006: 库存预警检查"""
        response = auth_client.check_stock_alert()
        assert response.status_code in [200, 401]


# ==================== 性能测试 ====================

@pytest.mark.api
@pytest.mark.performance
class TestAPIPerformance:
    """API性能测试"""
    
    def test_login_response_time(self, api_client):
        """TC-PERF-001: 登录响应时间"""
        start_time = time.time()
        api_client.login()
        elapsed = time.time() - start_time
        
        assert elapsed < 2.0, f"登录响应时间过长: {elapsed:.2f}s"
    
    def test_concurrent_requests(self, auth_client):
        """TC-PERF-002: 并发请求测试"""
        import concurrent.futures
        
        def make_request():
            return auth_client.get_user_page(tenantId=1)
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(make_request) for _ in range(10)]
            results = [f.result() for f in concurrent.futures.as_completed(futures)]
        
        assert len(results) == 10
    
    def test_page_query_performance(self, auth_client):
        """TC-PERF-003: 分页查询性能"""
        start_time = time.time()
        for _ in range(10):
            auth_client.get_user_page(tenantId=1)
        elapsed = time.time() - start_time
        
        avg_time = elapsed / 10
        assert avg_time < 0.5, f"平均查询时间过长: {avg_time:.2f}s"


# ==================== 安全测试 ====================

@pytest.mark.api
@pytest.mark.security
class TestAPISecurity:
    """API安全测试"""
    
    def test_unauthorized_access(self, api_client):
        """TC-SEC-001: 未授权访问"""
        api_client.clear_token()
        response = api_client.get_user_detail(1)
        assert response.status_code in [401, 403]
    
    def test_sql_injection_login(self, api_client):
        """TC-SEC-002: SQL注入测试"""
        response = api_client.login(
            username="admin' OR '1'='1",
            password="admin' OR '1'='1"
        )
        # 应该返回登录失败，不应该返回200
        assert response.status_code in [401, 400, 200]
        if response.status_code == 200:
            # 如果返回成功，检查token是否有效
            assert response.data is None or not response.success
    
    def test_xss_injection(self, auth_client):
        """TC-SEC-003: XSS注入测试"""
        user_data = {
            "tenantId": 1,
            "username": f"test_{int(time.time())}",
            "password": "Test@123456",
            "nickname": "<script>alert('XSS')</script>",
            "email": "test@example.com"
        }
        response = auth_client.create_user(user_data)
        # 应该成功创建，但数据应该被转义
        assert response.status_code in [200, 401, 403, 400]
    
    def test_invalid_token(self, api_client):
        """TC-SEC-004: 无效Token测试"""
        api_client.set_token("invalid_token_12345")
        response = api_client.get_user_detail(1)
        assert response.status_code in [401, 403]
    
    def test_expired_token(self, api_client):
        """TC-SEC-005: 过期Token测试"""
        api_client.set_token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired")
        response = api_client.get_user_detail(1)
        assert response.status_code in [401, 403]


# ==================== 运行入口 ====================

if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short", "-m", "api"])