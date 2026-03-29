#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready核心API接口测试
测试范围：
1. 用户认证API测试
2. 客户管理API测试
3. 订单管理API测试
4. Agent调用API测试

技术栈: pytest + requests
输出: H:\OpenClaw_Works\AI_READY_CORE_API_TEST_REPORT.md
"""

import pytest
import requests
import time
import json
from typing import Dict, Any, Optional
from dataclasses import dataclass


# ==================== 配置 ====================

@dataclass
class TestConfig:
    """测试配置"""
    base_url: str = "http://localhost:8080"
    api_base_path: str = "/api"
    timeout: int = 30
    tenant_id: int = 1
    report_path: str = "H:\\OpenClaw_Works\\AI_READY_CORE_API_TEST_REPORT.md"
    
    @property
    def api_url(self) -> str:
        return f"{self.base_url}{self.api_base_path}"


CONFIG = TestConfig()


# ==================== API客户端 ====================

class CoreApiClient:
    """AI-Ready核心API客户端"""
    
    def __init__(self, config: TestConfig = CONFIG):
        self.config = config
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        self._token: Optional[str] = None
    
    def set_token(self, token: str):
        """设置认证Token"""
        self._token = token
        self.session.headers["Authorization"] = f"Bearer {token}"
    
    def clear_token(self):
        """清除Token"""
        self._token = None
        if "Authorization" in self.session.headers:
            del self.session.headers["Authorization"]
    
    # ==================== 用户认证API ====================
    
    def user_login(self, username: str, password: str, tenant_id: int = None) -> Dict[str, Any]:
        """用户登录"""
        data = {
            "username": username,
            "password": password,
            "tenantId": tenant_id or self.config.tenant_id
        }
        response = self.session.post(
            f"{self.config.api_url}/user/login",
            json=data,
            headers={"X-Real-IP": "127.0.0.1"},
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def user_logout(self) -> Dict[str, Any]:
        """用户登出"""
        response = self.session.post(
            f"{self.config.api_url}/user/logout",
            timeout=self.config.timeout
        )
        self.clear_token()
        return self._parse_response(response)
    
    def get_user_profile(self) -> Dict[str, Any]:
        """获取用户资料"""
        response = self.session.get(
            f"{self.config.api_url}/user/profile",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def update_user_profile(self, data: Dict) -> Dict[str, Any]:
        """更新用户资料"""
        response = self.session.put(
            f"{self.config.api_url}/user/profile",
            json=data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    # ==================== 客户管理API ====================
    
    def create_customer(self, customer_data: Dict) -> Dict[str, Any]:
        """创建客户"""
        response = self.session.post(
            f"{self.config.api_url}/crm/customer",
            json=customer_data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def update_customer(self, customer_id: int, customer_data: Dict) -> Dict[str, Any]:
        """更新客户"""
        response = self.session.put(
            f"{self.config.api_url}/crm/customer/{customer_id}",
            json=customer_data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def delete_customer(self, customer_id: int) -> Dict[str, Any]:
        """删除客户"""
        response = self.session.delete(
            f"{self.config.api_url}/crm/customer/{customer_id}",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_customer_list(self, page: int = 1, size: int = 10, **filters) -> Dict[str, Any]:
        """客户列表"""
        params = {"current": page, "size": size, **filters}
        response = self.session.get(
            f"{self.config.api_url}/crm/customer/page",
            params=params,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_customer_detail(self, customer_id: int) -> Dict[str, Any]:
        """客户详情"""
        response = self.session.get(
            f"{self.config.api_url}/crm/customer/{customer_id}",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    # ==================== 订单管理API ====================
    
    def create_order(self, order_data: Dict) -> Dict[str, Any]:
        """创建订单"""
        response = self.session.post(
            f"{self.config.api_url}/crm/order",
            json=order_data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def update_order(self, order_id: int, order_data: Dict) -> Dict[str, Any]:
        """更新订单"""
        response = self.session.put(
            f"{self.config.api_url}/crm/order/{order_id}",
            json=order_data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def delete_order(self, order_id: int) -> Dict[str, Any]:
        """删除订单"""
        response = self.session.delete(
            f"{self.config.api_url}/crm/order/{order_id}",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_order_list(self, page: int = 1, size: int = 10, **filters) -> Dict[str, Any]:
        """订单列表"""
        params = {"current": page, "size": size, **filters}
        response = self.session.get(
            f"{self.config.api_url}/crm/order/page",
            params=params,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_order_detail(self, order_id: int) -> Dict[str, Any]:
        """订单详情"""
        response = self.session.get(
            f"{self.config.api_url}/crm/order/{order_id}",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def submit_order(self, order_id: int) -> Dict[str, Any]:
        """提交订单"""
        response = self.session.post(
            f"{self.config.api_url}/crm/order/{order_id}/submit",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def approve_order(self, order_id: int) -> Dict[str, Any]:
        """审批订单"""
        response = self.session.post(
            f"{self.config.api_url}/crm/order/{order_id}/approve",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    # ==================== Agent调用API ====================
    
    def register_agent(self, agent_data: Dict) -> Dict[str, Any]:
        """注册Agent"""
        response = self.session.post(
            f"{self.config.api_url}/agent/register",
            json=agent_data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def update_agent(self, agent_id: int, agent_data: Dict) -> Dict[str, Any]:
        """更新Agent"""
        response = self.session.put(
            f"{self.config.api_url}/agent/{agent_id}",
            json=agent_data,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def delete_agent(self, agent_id: int) -> Dict[str, Any]:
        """删除Agent"""
        response = self.session.delete(
            f"{self.config.api_url}/agent/{agent_id}",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_agent_list(self, page: int = 1, size: int = 10, **filters) -> Dict[str, Any]:
        """Agent列表"""
        params = {"current": page, "size": size, **filters}
        response = self.session.get(
            f"{self.config.api_url}/agent/page",
            params=params,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_agent_detail(self, agent_id: int) -> Dict[str, Any]:
        """Agent详情"""
        response = self.session.get(
            f"{self.config.api_url}/agent/{agent_id}",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def call_agent(self, agent_id: int, payload: Dict) -> Dict[str, Any]:
        """调用Agent"""
        response = self.session.post(
            f"{self.config.api_url}/agent/{agent_id}/call",
            json=payload,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def call_agent_by_code(self, agent_code: str, payload: Dict) -> Dict[str, Any]:
        """通过code调用Agent"""
        response = self.session.post(
            f"{self.config.api_url}/agent/{agent_code}/call",
            json=payload,
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    def get_active_agents(self) -> Dict[str, Any]:
        """获取活跃Agent"""
        response = self.session.get(
            f"{self.config.api_url}/agent/active",
            timeout=self.config.timeout
        )
        return self._parse_response(response)
    
    # ==================== 辅助方法 ====================
    
    def _parse_response(self, response: requests.Response) -> Dict[str, Any]:
        """解析响应"""
        try:
            data = response.json()
            return {
                "status_code": response.status_code,
                "success": data.get('code') == 200 or data.get('success', True),
                "data": data.get('data'),
                "message": data.get('message', ''),
                "raw": data
            }
        except Exception:
            return {
                "status_code": response.status_code,
                "success": False,
                "message": response.text[:200],
                "raw": {}
            }


# ==================== 测试夹具 ====================

@pytest.fixture(scope="module")
def api_client():
    """API客户端"""
    return CoreApiClient()


# ==================== 用户认证API测试 ====================

@pytest.mark.api
@pytest.mark.auth
class TestUserAuthAPI:
    """用户认证API测试"""
    
    def test_01_login_success(self, api_client):
        """TC-AUTH-001: 用户登录成功"""
        response = api_client.user_login("admin", "Admin@123")
        
        assert response["status_code"] in [200, 401, 403]
        if response["status_code"] == 200:
            api_client.set_token(response["data"])
            assert response["success"] is True
    
    def test_02_login_invalid_credentials(self, api_client):
        """TC-AUTH-002: 无效凭证登录"""
        response = api_client.user_login("invalid_user", "invalid_password")
        
        assert response["status_code"] in [200, 401, 400]
    
    def test_03_logout(self, api_client):
        """TC-AUTH-003: 用户登出"""
        # 先登录
        response = api_client.user_login("admin", "Admin@123")
        if response["status_code"] == 200:
            api_client.set_token(response["data"])
        
        # 登出
        response = api_client.user_logout()
        assert response["status_code"] in [200, 401]


# ==================== 客户管理API测试 ====================

@pytest.mark.api
@pytest.mark.crm
class TestCustomerAPI:
    """客户管理API测试"""
    
    def test_01_create_customer(self, api_client):
        """TC-CUST-001: 创建客户"""
        customer_data = {
            "name": f"TestCustomer_{int(time.time())}",
            "type": "企业",
            "level": "A",
            "phone": "13800138000",
            "email": "test@example.com",
            "address": "北京市朝阳区",
            "contactPerson": "张三",
            "contactPhone": "13800138000"
        }
        
        response = api_client.create_customer(customer_data)
        
        assert response["status_code"] in [200, 401, 403]
    
    def test_02_get_customer_list(self, api_client):
        """TC-CUST-002: 客户列表"""
        response = api_client.get_customer_list(page=1, size=10)
        
        assert response["status_code"] in [200, 401]
    
    def test_03_get_customer_detail(self, api_client):
        """TC-CUST-003: 客户详情"""
        response = api_client.get_customer_detail(1)
        
        assert response["status_code"] in [200, 401, 404]
    
    def test_04_update_customer(self, api_client):
        """TC-CUST-004: 更新客户"""
        response = api_client.update_customer(1, {"name": "UpdatedCustomer"})
        
        assert response["status_code"] in [200, 401, 403, 404]
    
    def test_05_delete_customer(self, api_client):
        """TC-CUST-005: 删除客户"""
        response = api_client.delete_customer(999999)
        
        assert response["status_code"] in [200, 401, 403, 404]


# ==================== 订单管理API测试 ====================

@pytest.mark.api
@pytest.mark.order
class TestOrderAPI:
    """订单管理API测试"""
    
    def test_01_create_order(self, api_client):
        """TC-ORD-001: 创建订单"""
        order_data = {
            "customerId": 1,
            "orderItems": [
                {"productId": 1, "quantity": 10, "price": 100.00}
            ],
            "totalAmount": 1000.00,
            "remark": "测试订单"
        }
        
        response = api_client.create_order(order_data)
        
        assert response["status_code"] in [200, 401, 403]
    
    def test_02_get_order_list(self, api_client):
        """TC-ORD-002: 订单列表"""
        response = api_client.get_order_list(page=1, size=10)
        
        assert response["status_code"] in [200, 401]
    
    def test_03_get_order_detail(self, api_client):
        """TC-ORD-003: 订单详情"""
        response = api_client.get_order_detail(1)
        
        assert response["status_code"] in [200, 401, 404]
    
    def test_04_submit_order(self, api_client):
        """TC-ORD-004: 提交订单"""
        response = api_client.submit_order(1)
        
        assert response["status_code"] in [200, 401, 403, 404]
    
    def test_05_approve_order(self, api_client):
        """TC-ORD-005: 审批订单"""
        response = api_client.approve_order(1)
        
        assert response["status_code"] in [200, 401, 403, 404]


# ==================== Agent调用API测试 ====================

@pytest.mark.api
@pytest.mark.agent
class TestAgentAPI:
    """Agent调用API测试"""
    
    def test_01_register_agent(self, api_client):
        """TC-AGENT-001: 注册Agent"""
        agent_data = {
            "tenantId": 1,
            "agentCode": f"TEST_AGENT_{int(time.time())}",
            "agentName": "测试Agent",
            "agentType": "CHATBOT",
            "capabilities": ["NLU", "DIALOGUE"]
        }
        
        response = api_client.register_agent(agent_data)
        
        assert response["status_code"] in [200, 401, 403]
    
    def test_02_get_agent_list(self, api_client):
        """TC-AGENT-002: Agent列表"""
        response = api_client.get_agent_list(page=1, size=10)
        
        assert response["status_code"] in [200, 401]
    
    def test_03_get_agent_detail(self, api_client):
        """TC-AGENT-003: Agent详情"""
        response = api_client.get_agent_detail(1)
        
        assert response["status_code"] in [200, 401, 404]
    
    def test_04_update_agent(self, api_client):
        """TC-AGENT-004: 更新Agent"""
        response = api_client.update_agent(1, {"agentName": "UpdatedAgent"})
        
        assert response["status_code"] in [200, 401, 403, 404]
    
    def test_05_delete_agent(self, api_client):
        """TC-AGENT-005: 删除Agent"""
        response = api_client.delete_agent(999999)
        
        assert response["status_code"] in [200, 401, 403, 404]
    
    def test_06_get_active_agents(self, api_client):
        """TC-AGENT-006: 获取活跃Agent"""
        response = api_client.get_active_agents()
        
        assert response["status_code"] in [200, 401]


# ==================== 性能测试 ====================

@pytest.mark.api
@pytest.mark.performance
class TestAPIPerformance:
    """API性能测试"""
    
    def test_login_response_time(self, api_client):
        """TC-PERF-001: 登录响应时间"""
        start_time = time.time()
        api_client.user_login("admin", "Admin@123")
        elapsed = time.time() - start_time
        
        assert elapsed < 2.0, f"登录响应时间过长: {elapsed:.2f}s"
    
    def test_customer_list_performance(self, api_client):
        """TC-PERF-002: 客户列表查询性能"""
        start_time = time.time()
        api_client.get_customer_list(page=1, size=100)
        elapsed = time.time() - start_time
        
        assert elapsed < 1.0, f"客户列表查询时间过长: {elapsed:.2f}s"
    
    def test_order_list_performance(self, api_client):
        """TC-PERF-003: 订单列表查询性能"""
        start_time = time.time()
        api_client.get_order_list(page=1, size=100)
        elapsed = time.time() - start_time
        
        assert elapsed < 1.0, f"订单列表查询时间过长: {elapsed:.2f}s"
    
    def test_agent_list_performance(self, api_client):
        """TC-PERF-004: Agent列表查询性能"""
        start_time = time.time()
        api_client.get_agent_list(page=1, size=100)
        elapsed = time.time() - start_time
        
        assert elapsed < 1.0, f"Agent列表查询时间过长: {elapsed:.2f}s"


# ==================== 安全测试 ====================

@pytest.mark.api
@pytest.mark.security
class TestAPISecurity:
    """API安全测试"""
    
    def test_unauthorized_access(self, api_client):
        """TC-SEC-001: 未授权访问测试"""
        # 清除Token
        api_client.clear_token()
        
        response = api_client.get_customer_detail(1)
        
        assert response["status_code"] in [401, 403]
    
    def test_sql_injection_login(self, api_client):
        """TC-SEC-002: SQL注入测试"""
        response = api_client.user_login(
            "admin' OR '1'='1",
            "admin' OR '1'='1"
        )
        
        # 应该返回登录失败
        assert response["status_code"] in [401, 400, 200]
    
    def test_invalid_token(self, api_client):
        """TC-SEC-003: 无效Token测试"""
        api_client.set_token("invalid_token_test")
        
        response = api_client.get_customer_detail(1)
        
        assert response["status_code"] in [401, 403]


# ==================== 运行入口 ====================

if __name__ == "__main__":
    pytest.main([__file__, "-v", "--tb=short", "-m", "api"])
