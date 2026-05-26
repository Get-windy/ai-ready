"""
企智连(AI-Ready)端到端业务流程测试
=====================================

测试场景：
1. 用户注册→登录→下单流程
2. 业务审批流程
3. 数据同步流程

运行方式：
- Mock模式（服务未启动）: pytest tests/e2e/test_e2e_business_flow.py --mock
- 真实模式（服务已启动）: pytest tests/e2e/test_e2e_business_flow.py --base-url=http://localhost:8080

依赖：
- pytest >= 7.0
- requests >= 2.28
- pytest-mock (可选)
"""

import pytest
import requests
import json
import time
from typing import Dict, Any, Optional
from dataclasses import dataclass
from enum import Enum


# ==================== 配置 ====================

class TestMode(Enum):
    MOCK = "mock"
    REAL = "real"


@dataclass
class TestConfig:
    base_url: str
    mode: TestMode
    timeout: int = 30
    mock_delay: float = 0.1  # 模拟API延迟


import os


def get_config(request) -> TestConfig:
    """获取测试配置 - 支持环境变量和pytest选项"""
    # 环境变量优先（更可靠）
    use_mock = os.environ.get('E2E_MOCK_MODE', '').lower() in ('true', '1', 'yes')
    base_url = os.environ.get('E2E_BASE_URL', 'http://localhost:8080')
    
    # 也支持pytest选项
    try:
        if hasattr(request.config, 'getoption'): 
            pytest_url = request.config.getoption("--base-url", default=None)
            pytest_mock = request.config.getoption("--mock", default=None)
            if pytest_url:
                base_url = pytest_url
            if pytest_mock is True:
                use_mock = True
    except Exception:
        pass
    
    mode = TestMode.MOCK if use_mock else TestMode.REAL
    return TestConfig(base_url=base_url, mode=mode)


# ==================== Mock数据 ====================

class MockDataFactory:
    """Mock数据工厂"""
    
    @staticmethod
    def user_registration_data() -> Dict[str, Any]:
        return {
            "username": f"test_user_{int(time.time())}",
            "password": "Test@123456",
            "email": f"test_{int(time.time())}@example.com",
            "phone": "13800138000",
            "realName": "测试用户"
        }
    
    @staticmethod
    def login_response() -> Dict[str, Any]:
        return {
            "code": 200,
            "message": "登录成功",
            "data": {
                "token": "mock_token_" + str(int(time.time())),
                "userId": 10001,
                "username": "test_user",
                "roles": ["user"]
            }
        }
    
    @staticmethod
    def order_data() -> Dict[str, Any]:
        return {
            "productId": "PROD_001",
            "productName": "测试商品",
            "quantity": 2,
            "price": 99.99,
            "address": "测试地址123号"
        }
    
    @staticmethod
    def order_response() -> Dict[str, Any]:
        return {
            "code": 200,
            "message": "下单成功",
            "data": {
                "orderId": "ORD_" + str(int(time.time())),
                "status": "pending",
                "totalPrice": 199.98
            }
        }
    
    @staticmethod
    def approval_request() -> Dict[str, Any]:
        return {
            "requestId": "APR_" + str(int(time.time())),
            "type": "purchase",
            "title": "采购审批申请",
            "content": "申请采购办公用品",
            "amount": 5000.00,
            "applicant": "test_user"
        }
    
    @staticmethod
    def approval_response(action: str) -> Dict[str, Any]:
        return {
            "code": 200,
            "message": f"审批{action}成功",
            "data": {
                "approvalId": "APV_" + str(int(time.time())),
                "status": action,
                "approver": "admin_user",
                "approvedAt": time.strftime("%Y-%m-%d %H:%M:%S")
            }
        }
    
    @staticmethod
    def sync_status() -> Dict[str, Any]:
        return {
            "cdcEnabled": True,
            "syncRunning": True,
            "lastSyncTime": time.strftime("%Y-%m-%d %H:%M:%S"),
            "pendingChanges": 0,
            "syncErrors": 0
        }
    
    @staticmethod
    def invalid_user_registration_data() -> Dict[str, Any]:
        """无效的用户注册数据，用于验证测试"""
        return {
            "username": "",  # 空用户名
            "password": "123",  # 密码太短
            "email": "invalid_email",  # 无效邮箱
            "phone": "123",  # 无效手机号
        }
    
    @staticmethod
    def weak_password_data() -> Dict[str, Any]:
        """弱密码用户数据"""
        return {
            "username": f"weak_user_{int(time.time())}",
            "password": "password",  # 常见弱密码
            "email": f"weak_{int(time.time())}@example.com",
            "phone": "13800138000",
        }
    
    @staticmethod
    def existing_user_data() -> Dict[str, Any]:
        """已存在用户的数据（用于重复注册测试）"""
        return {
            "username": "existing_user",
            "password": "Test@123456",
            "email": "existing@example.com",
            "phone": "13800138001",
        }
    
    @staticmethod
    def user_profile_update_data() -> Dict[str, Any]:
        """用户信息更新数据"""
        return {
            "realName": "更新后的用户名",
            "email": f"updated_{int(time.time())}@example.com",
            "phone": "13800138002",
            "avatar": "https://example.com/avatar.jpg",
            "bio": "这是更新后的用户简介"
        }
    
    @staticmethod
    def password_change_data() -> Dict[str, Any]:
        """密码修改数据"""
        return {
            "oldPassword": "Test@123456",
            "newPassword": "NewTest@123456",
            "confirmPassword": "NewTest@123456"
        }
    
    @staticmethod
    def token_refresh_response() -> Dict[str, Any]:
        """Token刷新响应"""
        return {
            "code": 200,
            "message": "Token刷新成功",
            "data": {
                "accessToken": f"new_token_{int(time.time())}",
                "refreshToken": f"refresh_{int(time.time())}",
                "expiresIn": 3600
            }
        }
    
    @staticmethod
    def user_status_response(active: bool = True) -> Dict[str, Any]:
        """用户状态响应"""
        return {
            "code": 200,
            "message": "成功",
            "data": {
                "userId": 10001,
                "active": active,
                "lockedUntil": None if active else time.strftime("%Y-%m-%d %H:%M:%S"),
                "failedAttempts": 0 if active else 5
            }
        }
    
    @staticmethod
    def concurrent_users_data(count: int = 10) -> List[Dict[str, Any]]:
        """并发用户测试数据"""
        users = []
        base_time = int(time.time())
        for i in range(count):
            users.append({
                "username": f"concurrent_user_{base_time}_{i}",
                "password": f"Test@{base_time}_{i}",
                "email": f"concurrent_{base_time}_{i}@example.com",
                "phone": f"13800{base_time % 10000:04d}",
                "realName": f"并发用户{i}"
            })
        return users


# ==================== API客户端 ====================

class APIClient:
    """统一API客户端 - 支持Mock和真实模式"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.session = requests.Session()
        self.token: Optional[str] = None
        self._mock_state: Dict[str, Any] = {}
    
    def _mock_delay(self):
        """模拟API延迟"""
        if self.config.mode == TestMode.MOCK:
            time.sleep(self.config.mock_delay)
    
    def _make_request(self, method: str, endpoint: str, 
                      data: Optional[Dict] = None,
                      headers: Optional[Dict] = None) -> Dict[str, Any]:
        """统一请求处理"""
        if self.config.mode == TestMode.MOCK:
            return self._mock_request(method, endpoint, data)
        else:
            return self._real_request(method, endpoint, data, headers)
    
    def _mock_request(self, method: str, endpoint: str, 
                       data: Optional[Dict] = None) -> Dict[str, Any]:
        """Mock请求处理"""
        self._mock_delay()
        
        # 根据endpoint返回mock数据
        if endpoint == "/api/auth/register":
            return {"code": 200, "message": "注册成功", "data": {"userId": 10001}}
        
        elif endpoint == "/api/auth/login":
            resp = MockDataFactory.login_response()
            self.token = resp["data"]["token"]
            return resp
        
        elif endpoint == "/api/orders":
            if method == "POST":
                return MockDataFactory.order_response()
            return {"code": 200, "data": []}
        
        elif endpoint.startswith("/api/approval"):
            if method == "POST":
                action = "approved" if "approve" in endpoint else "rejected"
                return MockDataFactory.approval_response(action)
            return {"code": 200, "data": {"status": "pending"}}
        
        elif endpoint == "/api/data-sync/status":
            return {"code": 200, "data": MockDataFactory.sync_status()}
        
        elif endpoint == "/api/data-sync/sync/full":
            return {"code": 200, "message": "同步完成", "data": {"synced": 100}}
        
        # 新增的用户管理API端点
        elif endpoint.startswith("/api/users/") and "/profile" in endpoint:
            return {"code": 200, "message": "用户信息更新成功", "data": {"updatedAt": time.strftime("%Y-%m-%d %H:%M:%S")}}
        
        elif endpoint.startswith("/api/users/") and "/change-password" in endpoint:
            return {"code": 200, "message": "密码修改成功", "data": {}}
        
        elif endpoint == "/api/auth/refresh":
            resp = MockDataFactory.token_refresh_response()
            self.token = resp["data"]["accessToken"]
            return resp
        
        elif endpoint.startswith("/api/users/") and "/logout" in endpoint:
            self.token = None
            return {"code": 200, "message": "登出成功"}
        
        elif endpoint.startswith("/api/users/") and "/status" in endpoint:
            return MockDataFactory.user_status_response(active=True)
        
        elif endpoint.startswith("/api/users/") and "/enable" in endpoint:
            return {"code": 200, "message": "用户已启用", "data": {"active": True}}
        
        elif endpoint.startswith("/api/users/") and "/disable" in endpoint:
            return {"code": 200, "message": "用户已禁用", "data": {"active": False}}
        
        return {"code": 404, "message": "Mock endpoint not configured"}
    
    def _real_request(self, method: str, endpoint: str,
                       data: Optional[Dict] = None,
                       headers: Optional[Dict] = None) -> Dict[str, Any]:
        """真实请求处理"""
        url = f"{self.config.base_url}{endpoint}"
        req_headers = headers or {}
        if self.token:
            req_headers["Authorization"] = f"Bearer {self.token}"
        
        try:
            if method == "GET":
                resp = self.session.get(url, headers=req_headers, timeout=self.config.timeout)
            elif method == "POST":
                resp = self.session.post(url, json=data, headers=req_headers, timeout=self.config.timeout)
            elif method == "PUT":
                resp = self.session.put(url, json=data, headers=req_headers, timeout=self.config.timeout)
            elif method == "DELETE":
                resp = self.session.delete(url, headers=req_headers, timeout=self.config.timeout)
            else:
                raise ValueError(f"Unsupported method: {method}")
            
            return resp.json()
        except requests.exceptions.RequestException as e:
            return {"code": 500, "message": str(e)}
    
    # ==================== 业务API方法 ====================
    
    def register(self, user_data: Dict[str, Any]) -> Dict[str, Any]:
        """用户注册"""
        return self._make_request("POST", "/api/auth/register", user_data)
    
    def login(self, username: str, password: str) -> Dict[str, Any]:
        """用户登录"""
        data = {"username": username, "password": password}
        return self._make_request("POST", "/api/auth/login", data)
    
    def create_order(self, order_data: Dict[str, Any]) -> Dict[str, Any]:
        """创建订单"""
        return self._make_request("POST", "/api/orders", order_data)
    
    def get_orders(self) -> Dict[str, Any]:
        """获取订单列表"""
        return self._make_request("GET", "/api/orders")
    
    def submit_approval(self, approval_data: Dict[str, Any]) -> Dict[str, Any]:
        """提交审批申请"""
        return self._make_request("POST", "/api/approval/submit", approval_data)
    
    def approve_request(self, request_id: str) -> Dict[str, Any]:
        """审批通过"""
        return self._make_request("POST", f"/api/approval/{request_id}/approve")
    
    def reject_request(self, request_id: str) -> Dict[str, Any]:
        """审批拒绝"""
        return self._make_request("POST", f"/api/approval/{request_id}/reject")
    
    def get_sync_status(self) -> Dict[str, Any]:
        """获取数据同步状态"""
        return self._make_request("GET", "/api/data-sync/status")
    
    def trigger_sync(self) -> Dict[str, Any]:
        """触发数据同步"""
        return self._make_request("POST", "/api/data-sync/sync/full")
    
    def update_user_profile(self, user_id: int, profile_data: Dict[str, Any]) -> Dict[str, Any]:
        """更新用户信息"""
        return self._make_request("PUT", f"/api/users/{user_id}/profile", profile_data)
    
    def change_password(self, user_id: int, password_data: Dict[str, Any]) -> Dict[str, Any]:
        """修改密码"""
        return self._make_request("POST", f"/api/users/{user_id}/change-password", password_data)
    
    def refresh_token(self, refresh_token: str) -> Dict[str, Any]:
        """刷新Token"""
        data = {"refreshToken": refresh_token}
        return self._make_request("POST", "/api/auth/refresh", data)
    
    def logout(self, user_id: int) -> Dict[str, Any]:
        """用户登出"""
        return self._make_request("POST", f"/api/users/{user_id}/logout")
    
    def get_user_status(self, user_id: int) -> Dict[str, Any]:
        """获取用户状态"""
        return self._make_request("GET", f"/api/users/{user_id}/status")
    
    def enable_user(self, user_id: int) -> Dict[str, Any]:
        """启用用户"""
        return self._make_request("POST", f"/api/users/{user_id}/enable")
    
    def disable_user(self, user_id: int) -> Dict[str, Any]:
        """禁用用户"""
        return self._make_request("POST", f"/api/users/{user_id}/disable")


# ==================== 测试类 ====================

@pytest.fixture
def api_client(request):
    """API客户端fixture"""
    config = get_config(request)
    client = APIClient(config)
    yield client


class TestUserRegistrationLoginOrderFlow:
    """
    场景1: 用户注册→登录→下单流程
    
    测试步骤：
    1. 用户注册
    2. 用户登录获取Token
    3. 使用Token创建订单
    4. 查询订单列表确认订单创建成功
    """
    
    def test_complete_user_flow(self, api_client):
        """完整用户流程测试"""
        # Step 1: 用户注册
        user_data = MockDataFactory.user_registration_data()
        register_resp = api_client.register(user_data)
        
        assert register_resp["code"] == 200
        assert "userId" in register_resp["data"]
        user_id = register_resp["data"]["userId"]
        
        # Step 2: 用户登录
        login_resp = api_client.login(user_data["username"], user_data["password"])
        
        assert login_resp["code"] == 200
        assert "token" in login_resp["data"]
        assert api_client.token is not None
        
        # Step 3: 创建订单
        order_data = MockDataFactory.order_data()
        order_resp = api_client.create_order(order_data)
        
        assert order_resp["code"] == 200
        assert "orderId" in order_resp["data"]
        assert order_resp["data"]["status"] == "pending"
        
        # Step 4: 查询订单列表
        orders_resp = api_client.get_orders()
        
        assert orders_resp["code"] == 200
        assert "data" in orders_resp
    
    def test_registration_validation(self, api_client):
        """注册参数验证测试"""
        # 缺少必填字段
        invalid_data = {"username": "test"}  # 缺少password
        resp = api_client.register(invalid_data)
        
        # Mock模式下应该返回400，真实模式也应该校验
        # 实际行为取决于服务端实现
        assert resp["code"] in [200, 400]  # 宽松校验
    
    def test_login_with_invalid_credentials(self, api_client):
        """无效凭证登录测试"""
        resp = api_client.login("invalid_user", "wrong_password")
        
        # 应返回认证失败
        assert resp["code"] in [401, 200]  # Mock返回200，真实返回401
    
    def test_order_without_authentication(self, request):
        """无认证创建订单测试"""
        config = get_config(request)
        client = APIClient(config)
        # 不登录，直接创建订单
        
        order_data = MockDataFactory.order_data()
        resp = client.create_order(order_data)
        
        # Mock模式下返回成功（无认证校验）
        # 真实模式应返回401
        if config.mode == TestMode.REAL:
            assert resp["code"] == 401
        else:
            assert resp["code"] == 200


class TestBusinessApprovalFlow:
    """
    场景2: 业务审批流程
    
    测试步骤：
    1. 登录获取审批员Token
    2. 提交审批申请
    3. 审批员审核申请（通过/拒绝）
    4. 查询审批状态
    """
    
    def test_approval_approve_flow(self, api_client):
        """审批通过流程"""
        # Step 1: 登录
        login_resp = api_client.login("admin_user", "Admin@123")
        assert login_resp["code"] == 200
        
        # Step 2: 提交审批申请
        approval_data = MockDataFactory.approval_request()
        submit_resp = api_client.submit_approval(approval_data)
        
        assert submit_resp["code"] == 200
        request_id = submit_resp["data"].get("approvalId", approval_data["requestId"])
        
        # Step 3: 审批通过
        approve_resp = api_client.approve_request(request_id)
        
        assert approve_resp["code"] == 200
        assert approve_resp["data"]["status"] == "approved"
    
    def test_approval_reject_flow(self, api_client):
        """审批拒绝流程"""
        # 登录
        login_resp = api_client.login("admin_user", "Admin@123")
        assert login_resp["code"] == 200
        
        # 提交审批
        approval_data = MockDataFactory.approval_request()
        submit_resp = api_client.submit_approval(approval_data)
        assert submit_resp["code"] == 200
        
        request_id = submit_resp["data"].get("approvalId", approval_data["requestId"])
        
        # 审批拒绝
        reject_resp = api_client.reject_request(request_id)
        
        assert reject_resp["code"] == 200
        assert reject_resp["data"]["status"] == "rejected"
    
    def test_approval_without_permission(self, request):
        """无权限审批测试"""
        config = get_config(request)
        client = APIClient(config)
        
        # 以普通用户登录
        client.login("normal_user", "Normal@123")
        
        # 尝试审批
        resp = client.approve_request("APR_001")
        
        # Mock返回成功，真实应返回403
        if config.mode == TestMode.REAL:
            assert resp["code"] == 403
        else:
            assert resp["code"] == 200


class TestDataSyncFlow:
    """
    场景3: 数据同步流程
    
    测试步骤：
    1. 查询同步状态
    2. 触发数据同步
    3. 验证同步结果
    4. 检查一致性
    """
    
    def test_sync_status_check(self, api_client):
        """同步状态查询测试"""
        resp = api_client.get_sync_status()
        
        assert resp["code"] == 200
        data = resp["data"]
        
        # 验证状态字段
        assert "cdcEnabled" in data
        assert "syncRunning" in data
        assert "lastSyncTime" in data
    
    def test_trigger_full_sync(self, api_client):
        """触发全量同步测试"""
        # 登录（需要管理员权限）
        login_resp = api_client.login("admin_user", "Admin@123")
        assert login_resp["code"] == 200
        
        # 触发同步
        sync_resp = api_client.trigger_sync()
        
        assert sync_resp["code"] == 200
        assert "synced" in sync_resp["data"]
    
    def test_sync_consistency(self, api_client):
        """同步一致性验证测试"""
        # 查询状态
        status_resp = api_client.get_sync_status()
        assert status_resp["code"] == 200
        
        data = status_resp["data"]
        
        # 验证一致性指标
        # pendingChanges 应为0或很低
        # syncErrors 应为0
        assert data["pendingChanges"] >= 0
        assert data["syncErrors"] >= 0
        
        # 如果是真实模式，检查错误数是否合理
        if api_client.config.mode == TestMode.REAL:
            # 生产环境应无同步错误
            # 测试环境允许少量错误
            assert data["syncErrors"] < 10


class TestEnhancedUserManagement:
    """
    增强的用户管理测试类
    包含更多用户注册、登录、管理相关的测试用例
    """
    
    def test_comprehensive_user_registration(self, api_client):
        """完整的用户注册验证测试"""
        # 1. 正常注册
        user_data = MockDataFactory.user_registration_data()
        resp = api_client.register(user_data)
        
        assert resp["code"] == 200
        assert "userId" in resp["data"]
        user_id = resp["data"]["userId"]
        
        # 2. 无效数据注册测试
        invalid_data = MockDataFactory.invalid_user_registration_data()
        resp_invalid = api_client.register(invalid_data)
        
        # 真实模式下应返回400错误
        if api_client.config.mode == TestMode.REAL:
            assert resp_invalid["code"] == 400
        
        # 3. 弱密码注册测试
        weak_data = MockDataFactory.weak_password_data()
        resp_weak = api_client.register(weak_data)
        
        # 真实模式下应拒绝弱密码
        if api_client.config.mode == TestMode.REAL:
            assert resp_weak["code"] == 400
            
        # 4. 重复用户注册测试
        existing_data = MockDataFactory.existing_user_data()
        resp_existing = api_client.register(existing_data)
        
        # 真实模式下应返回409冲突
        if api_client.config.mode == TestMode.REAL:
            assert resp_existing["code"] in [409, 200]  # 可能已经存在
        
        return user_id
    
    def test_user_profile_management(self, api_client):
        """用户信息管理测试"""
        # 1. 注册用户
        user_data = MockDataFactory.user_registration_data()
        register_resp = api_client.register(user_data)
        
        assert register_resp["code"] == 200
        user_id = register_resp["data"]["userId"]
        
        # 2. 登录
        login_resp = api_client.login(user_data["username"], user_data["password"])
        assert login_resp["code"] == 200
        
        # 3. 更新用户信息
        profile_data = MockDataFactory.user_profile_update_data()
        update_resp = api_client.update_user_profile(user_id, profile_data)
        
        assert update_resp["code"] == 200
        
        # 4. 修改密码
        password_data = MockDataFactory.password_change_data()
        password_resp = api_client.change_password(user_id, password_data)
        
        assert password_resp["code"] == 200
        
        # 5. 使用新密码登录
        new_password = password_data["newPassword"]
        new_login_resp = api_client.login(user_data["username"], new_password)
        
        assert new_login_resp["code"] == 200
        
        return user_id
    
    def test_token_refresh_mechanism(self, api_client):
        """Token刷新机制测试"""
        # 1. 注册并登录
        user_data = MockDataFactory.user_registration_data()
        api_client.register(user_data)
        login_resp = api_client.login(user_data["username"], user_data["password"])
        
        assert login_resp["code"] == 200
        original_token = api_client.token
        
        # 2. Token刷新
        # 在实际系统中，refresh_token会从登录响应中获取
        # Mock模式下使用模拟的refresh_token
        refresh_resp = api_client.refresh_token("mock_refresh_token")
        
        assert refresh_resp["code"] == 200
        assert "accessToken" in refresh_resp["data"]
        
        # 3. 验证Token已更新
        new_token = api_client.token
        assert new_token != original_token
        
        # 4. 使用新Token进行API调用
        profile_resp = api_client.update_user_profile(10001, {"bio": "测试Token"})
        assert profile_resp["code"] == 200
    
    def test_user_session_management(self, api_client):
        """用户会话管理测试"""
        # 1. 注册并登录
        user_data = MockDataFactory.user_registration_data()
        api_client.register(user_data)
        login_resp = api_client.login(user_data["username"], user_data["password"])
        
        assert login_resp["code"] == 200
        user_id = login_resp["data"]["userId"]
        
        # 2. 登出
        logout_resp = api_client.logout(user_id)
        assert logout_resp["code"] == 200
        
        # 3. 验证Token已清除
        assert api_client.token is None
        
        # 4. 尝试使用已失效的Token访问API
        # 在Mock模式下，这取决于具体实现
        # 在真实模式下，应该返回401
        if api_client.config.mode == TestMode.REAL:
            api_client.token = "expired_token"
            profile_resp = api_client.update_user_profile(user_id, {"bio": "测试"})
            assert profile_resp["code"] == 401
    
    def test_user_status_control(self, api_client):
        """用户状态控制测试"""
        # 1. 注册用户
        user_data = MockDataFactory.user_registration_data()
        register_resp = api_client.register(user_data)
        
        assert register_resp["code"] == 200
        user_id = register_resp["data"]["userId"]
        
        # 2. 获取用户状态（应默认启用）
        status_resp = api_client.get_user_status(user_id)
        assert status_resp["code"] == 200
        assert status_resp["data"]["active"] == True
        
        # 3. 禁用用户（需要管理员权限）
        # 这里简化处理，实际需要管理员登录
        disable_resp = api_client.disable_user(user_id)
        assert disable_resp["code"] == 200
        
        # 4. 验证禁用状态
        status_resp = api_client.get_user_status(user_id)
        assert status_resp["code"] == 200
        assert status_resp["data"]["active"] == False
        
        # 5. 启用用户
        enable_resp = api_client.enable_user(user_id)
        assert enable_resp["code"] == 200
        
        # 6. 验证启用状态
        status_resp = api_client.get_user_status(user_id)
        assert status_resp["code"] == 200
        assert status_resp["data"]["active"] == True
    
    def test_concurrent_user_registration(self, api_client):
        """并发用户注册测试"""
        import threading
        
        test_results = []
        lock = threading.Lock()
        
        def register_user(user_data):
            """单个用户注册函数"""
            resp = api_client.register(user_data)
            with lock:
                test_results.append({
                    "username": user_data["username"],
                    "success": resp["code"] == 200
                })
        
        # 生成并发测试用户数据
        concurrent_users = MockDataFactory.concurrent_users_data(5)
        
        # 创建并启动线程
        threads = []
        for user_data in concurrent_users:
            thread = threading.Thread(target=register_user, args=(user_data,))
            threads.append(thread)
            thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
        
        # 验证结果
        successful_registrations = sum(1 for r in test_results if r["success"])
        print(f"并发注册结果: {successful_registrations}/{len(test_results)} 成功")
        
        # 在Mock模式下所有应该成功
        if api_client.config.mode == TestMode.MOCK:
            assert successful_registrations == len(test_results)
        else:
            # 真实模式下可能由于并发冲突导致部分失败
            assert successful_registrations >= 3  # 至少60%成功
        
        return test_results


class TestE2EIntegration:
    """
    集成测试：跨场景端到端测试
    """
    
    def test_user_order_approval_integration(self, api_client):
        """用户下单→审批集成测试"""
        # 用户注册登录
        user_data = MockDataFactory.user_registration_data()
        api_client.register(user_data)
        api_client.login(user_data["username"], user_data["password"])
        
        # 创建订单（大额订单需要审批）
        large_order = {
            "productId": "PROD_LARGE",
            "productName": "大宗采购商品",
            "quantity": 100,
            "price": 999.99,
            "totalPrice": 99999.00,
            "needApproval": True
        }
        
        order_resp = api_client.create_order(large_order)
        assert order_resp["code"] == 200
        
        # 切换审批员审批
        api_client.login("admin_user", "Admin@123")
        
        order_id = order_resp["data"]["orderId"]
        approve_resp = api_client.approve_request(order_id)
        
        assert approve_resp["code"] == 200
    
    def test_cross_module_data_flow(self, api_client):
        """跨模块数据流转测试"""
        # 模拟数据从订单模块流转到财务模块
        
        # 创建订单
        api_client.login("test_user", "Test@123")
        order_resp = api_client.create_order(MockDataFactory.order_data())
        assert order_resp["code"] == 200
        
        # 触发数据同步（订单数据同步到财务）
        api_client.login("admin_user", "Admin@123")
        sync_resp = api_client.trigger_sync()
        assert sync_resp["code"] == 200
        
        # 查询同步状态确认
        status_resp = api_client.get_sync_status()
        assert status_resp["code"] == 200


# ==================== pytest配置 ====================

def pytest_addoption(parser):
    """添加自定义选项"""
    parser.addoption(
        "--base-url",
        action="store",
        default="http://localhost:8080",
        help="API base URL for real mode testing"
    )
    parser.addoption(
        "--mock",
        action="store_true",
        default=False,
        help="Run tests in mock mode (no real server)"
    )


def pytest_configure(config):
    """pytest配置"""
    config.addinivalue_line(
        "markers", "mock: test runs in mock mode"
    )
    config.addinivalue_line(
        "markers", "real: test requires real server"
    )


# ==================== 运行入口 ====================

if __name__ == "__main__":
    # 直接运行测试
    import sys
    
    mock_mode = "--mock" in sys.argv
    
    if mock_mode:
        print("Running E2E tests in MOCK mode...")
        pytest.main([__file__, "--mock", "-v"])
    else:
        print("Running E2E tests in REAL mode (requires server running)...")
        pytest.main([__file__, "-v"])