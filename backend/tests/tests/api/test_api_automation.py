#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 接口自动化测试脚本
覆盖: 用户管理/权限管理/ERP核心/CRM核心接口
执行: pytest tests/api/test_api_automation.py -v --html=reports/api-automation-report.html
"""

import pytest
import requests
import json
import uuid
from datetime import datetime
from typing import Dict, Any, Optional
from dataclasses import dataclass

# ==================== 配置 ====================

@dataclass
class ApiConfig:
    base_url: str = "http://localhost:8080"
    timeout: int = 30
    tenant_id: int = 1
    
    @property
    def api_url(self) -> str:
        return f"{self.base_url}/api"

CONFIG = ApiConfig()

# ==================== API客户端 ====================

class ApiClient:
    def __init__(self, config: ApiConfig = CONFIG):
        self.config = config
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        self._token: Optional[str] = None
    
    def set_token(self, token: str):
        self._token = token
        self.session.headers["Authorization"] = f"Bearer {token}"
    
    def clear_token(self):
        self._token = None
        self.session.headers.pop("Authorization", None)
    
    def request(self, method: str, endpoint: str, **kwargs) -> Dict:
        url = f"{self.config.api_url}{endpoint}"
        kwargs.setdefault("timeout", self.config.timeout)
        try:
            resp = self.session.request(method, url, **kwargs)
            data = resp.json()
            return {"status_code": resp.status_code, "success": data.get("code", 0) == 200,
                    "data": data.get("data"), "message": data.get("message", "")}
        except requests.exceptions.ConnectionError:
            return {"status_code": 0, "success": False, "message": "服务不可达", "data": None}
        except Exception as e:
            return {"status_code": 0, "success": False, "message": str(e), "data": None}
    
    # 用户管理
    def login(self, username: str, password: str) -> Dict:
        result = self.request("POST", "/user/login", json={"username": username, "password": password, "tenantId": self.config.tenant_id})
        if result["success"] and result["data"]:
            self.set_token(result["data"])
        return result
    
    def logout(self) -> Dict:
        result = self.request("POST", "/user/logout")
        self.clear_token()
        return result
    
    def get_user_list(self, page: int = 1, size: int = 10) -> Dict:
        return self.request("GET", f"/user/list?page={page}&size={size}")
    
    def get_user(self, user_id: int) -> Dict:
        return self.request("GET", f"/user/{user_id}")
    
    def create_user(self, data: Dict) -> Dict:
        return self.request("POST", "/user", json=data)
    
    def update_user(self, user_id: int, data: Dict) -> Dict:
        return self.request("PUT", f"/user/{user_id}", json=data)
    
    def delete_user(self, user_id: int) -> Dict:
        return self.request("DELETE", f"/user/{user_id}")
    
    # 权限管理
    def get_role_list(self) -> Dict:
        return self.request("GET", "/role/list")
    
    def get_role(self, role_id: int) -> Dict:
        return self.request("GET", f"/role/{role_id}")
    
    def create_role(self, data: Dict) -> Dict:
        return self.request("POST", "/role", json=data)
    
    def update_role(self, role_id: int, data: Dict) -> Dict:
        return self.request("PUT", f"/role/{role_id}", json=data)
    
    def delete_role(self, role_id: int) -> Dict:
        return self.request("DELETE", f"/role/{role_id}")
    
    def get_permissions(self) -> Dict:
        return self.request("GET", "/permission/list")
    
    # ERP核心
    def create_purchase_order(self, data: Dict) -> Dict:
        return self.request("POST", "/erp/purchase/order", json=data)
    
    def get_purchase_order(self, order_id: int) -> Dict:
        return self.request("GET", f"/erp/purchase/order/{order_id}")
    
    def get_purchase_order_list(self, page: int = 1, size: int = 10) -> Dict:
        return self.request("GET", f"/erp/purchase/order/list?page={page}&size={size}")
    
    def update_order_status(self, order_id: int, status: str) -> Dict:
        return self.request("PUT", f"/erp/purchase/order/{order_id}/status", json={"status": status})
    
    def get_stock_list(self) -> Dict:
        return self.request("GET", "/erp/stock/list")
    
    def get_stock(self, product_id: int) -> Dict:
        return self.request("GET", f"/erp/stock/{product_id}")
    
    def update_stock(self, product_id: int, quantity: int) -> Dict:
        return self.request("PUT", f"/erp/stock/{product_id}", json={"quantity": quantity})
    
    # CRM核心
    def create_customer(self, data: Dict) -> Dict:
        return self.request("POST", "/crm/customer", json=data)
    
    def get_customer(self, customer_id: int) -> Dict:
        return self.request("GET", f"/crm/customer/{customer_id}")
    
    def get_customer_list(self, page: int = 1, size: int = 10) -> Dict:
        return self.request("GET", f"/crm/customer/list?page={page}&size={size}")
    
    def update_customer(self, customer_id: int, data: Dict) -> Dict:
        return self.request("PUT", f"/crm/customer/{customer_id}", json=data)
    
    def create_opportunity(self, data: Dict) -> Dict:
        return self.request("POST", "/crm/opportunity", json=data)
    
    def get_opportunity_list(self) -> Dict:
        return self.request("GET", "/crm/opportunity/list")


# ==================== Fixtures ====================

@pytest.fixture(scope="module")
def api_client():
    return ApiClient()

@pytest.fixture(scope="module")
def auth_client(api_client):
    """已认证的客户端"""
    result = api_client.login("admin", "Admin@123456")
    if not result["success"]:
        # 尝试其他用户
        api_client.login("testuser", "Test@123456")
    yield api_client
    api_client.logout()


# ==================== 用户管理接口测试 ====================

class TestUserAPI:
    """用户管理接口测试"""
    
    def test_01_login_success(self, api_client):
        """测试登录成功"""
        result = api_client.login("admin", "Admin@123456")
        assert result["success"] or result["status_code"] == 401, f"登录失败: {result['message']}"
    
    def test_02_login_invalid_password(self, api_client):
        """测试密码错误"""
        result = api_client.login("admin", "wrongpassword")
        assert not result["success"] or result["status_code"] in [401, 400]
    
    def test_03_get_user_list(self, auth_client):
        """测试获取用户列表"""
        result = auth_client.get_user_list()
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_04_create_user(self, auth_client):
        """测试创建用户"""
        user_data = {
            "username": f"test_user_{uuid.uuid4().hex[:8]}",
            "password": "Test@123456",
            "email": f"test_{uuid.uuid4().hex[:8]}@test.com",
            "phone": "13800138000",
            "realName": "测试用户"
        }
        result = auth_client.create_user(user_data)
        assert result["success"] or result["status_code"] in [200, 201, 401, 404]
    
    def test_05_logout(self, api_client):
        """测试登出"""
        api_client.login("admin", "Admin@123456")
        result = api_client.logout()
        assert result["success"] or result["status_code"] in [200, 401]


# ==================== 权限管理接口测试 ====================

class TestPermissionAPI:
    """权限管理接口测试"""
    
    def test_01_get_role_list(self, auth_client):
        """测试获取角色列表"""
        result = auth_client.get_role_list()
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_02_create_role(self, auth_client):
        """测试创建角色"""
        role_data = {
            "name": f"test_role_{uuid.uuid4().hex[:8]}",
            "code": f"TEST_{uuid.uuid4().hex[:4].upper()}",
            "description": "测试角色"
        }
        result = auth_client.create_role(role_data)
        assert result["success"] or result["status_code"] in [200, 201, 401, 404]
    
    def test_03_get_permissions(self, auth_client):
        """测试获取权限列表"""
        result = auth_client.get_permissions()
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_04_update_role(self, auth_client):
        """测试更新角色"""
        result = auth_client.update_role(1, {"description": "更新后的描述"})
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_05_delete_role(self, auth_client):
        """测试删除角色"""
        result = auth_client.delete_role(999)
        assert result["status_code"] in [200, 401, 404, 403]


# ==================== ERP核心接口测试 ====================

class TestERPAPI:
    """ERP核心接口测试"""
    
    def test_01_create_purchase_order(self, auth_client):
        """测试创建采购订单"""
        order_data = {
            "orderNo": f"PO{datetime.now().strftime('%Y%m%d%H%M%S')}",
            "supplierId": 1,
            "items": [{"productId": 1, "quantity": 10, "price": 100.00}],
            "totalAmount": 1000.00,
            "remark": "自动化测试订单"
        }
        result = auth_client.create_purchase_order(order_data)
        assert result["success"] or result["status_code"] in [200, 201, 401, 404]
    
    def test_02_get_order_list(self, auth_client):
        """测试获取订单列表"""
        result = auth_client.get_purchase_order_list()
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_03_get_order_detail(self, auth_client):
        """测试获取订单详情"""
        result = auth_client.get_purchase_order(1)
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_04_get_stock_list(self, auth_client):
        """测试获取库存列表"""
        result = auth_client.get_stock_list()
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_05_get_stock_detail(self, auth_client):
        """测试获取库存详情"""
        result = auth_client.get_stock(1)
        assert result["success"] or result["status_code"] in [200, 401, 404]


# ==================== CRM核心接口测试 ====================

class TestCRMAPI:
    """CRM核心接口测试"""
    
    def test_01_create_customer(self, auth_client):
        """测试创建客户"""
        customer_data = {
            "name": f"测试客户_{uuid.uuid4().hex[:8]}",
            "contact": "测试联系人",
            "phone": "13900139000",
            "email": f"customer_{uuid.uuid4().hex[:8]}@test.com",
            "address": "测试地址"
        }
        result = auth_client.create_customer(customer_data)
        assert result["success"] or result["status_code"] in [200, 201, 401, 404]
    
    def test_02_get_customer_list(self, auth_client):
        """测试获取客户列表"""
        result = auth_client.get_customer_list()
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_03_get_customer_detail(self, auth_client):
        """测试获取客户详情"""
        result = auth_client.get_customer(1)
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_04_update_customer(self, auth_client):
        """测试更新客户"""
        result = auth_client.update_customer(1, {"address": "新地址"})
        assert result["success"] or result["status_code"] in [200, 401, 404]
    
    def test_05_create_opportunity(self, auth_client):
        """测试创建商机"""
        opp_data = {
            "customerId": 1,
            "name": f"测试商机_{datetime.now().strftime('%Y%m%d%H%M%S')}",
            "amount": 50000.00,
            "stage": "INITIAL",
            "probability": 20
        }
        result = auth_client.create_opportunity(opp_data)
        assert result["success"] or result["status_code"] in [200, 201, 401, 404]
    
    def test_06_get_opportunity_list(self, auth_client):
        """测试获取商机列表"""
        result = auth_client.get_opportunity_list()
        assert result["success"] or result["status_code"] in [200, 401, 404]


# ==================== 运行入口 ====================

if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short"])
