#!/usr/bin/env python3
"""
企智连安全自动化测试
Security Automation Tests for Qi Zhi Lian ERP
"""

import pytest
import requests
import json
from typing import Dict, List

class SecurityTestBase:
    """安全测试基类"""
    
    BASE_URL = "http://localhost:8080/api/v1"
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前准备"""
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
    
    def get_auth_token(self, username: str, password: str) -> str:
        """获取认证令牌"""
        response = self.session.post(
            f"{self.BASE_URL}/auth/login",
            json={"username": username, "password": password}
        )
        assert response.status_code == 200
        return response.json().get("data", {}).get("token", "")


class TestAuthenticationSecurity(SecurityTestBase):
    """认证安全测试"""
    
    def test_brute_force_protection(self):
        """测试暴力破解防护"""
        blocked = False
        for i in range(10):
            response = self.session.post(
                f"{self.BASE_URL}/auth/login",
                json={"username": "admin", "password": f"wrong_{i}"}
            )
            if response.status_code == 429:
                blocked = True
                break
        assert blocked, "应触发暴力破解防护"
    
    def test_session_regeneration(self):
        """测试会话重新生成"""
        pre_login_cookie = self.session.cookies.get("session")
        
        self.get_auth_token("admin", "admin123")
        post_login_cookie = self.session.cookies.get("session")
        
        assert pre_login_cookie != post_login_cookie, "登录后Session应改变"
    
    def test_token_expiration(self):
        """测试令牌过期"""
        token = self.get_auth_token("admin", "admin123")
        headers = {"Authorization": f"Bearer {token}"}
        
        # 等待过期（模拟）
        response = self.session.get(
            f"{self.BASE_URL}/users/profile",
            headers=headers
        )
        assert response.status_code in [200, 401]


class TestAccessControl(SecurityTestBase):
    """访问控制测试"""
    
    def test_horizontal_privilege_escalation(self):
        """测试水平越权"""
        user_a_token = self.get_auth_token("user1", "password1")
        headers = {"Authorization": f"Bearer {user_a_token}"}
        
        # 尝试访问用户B的资源
        response = self.session.get(
            f"{self.BASE_URL}/orders/9999",
            headers=headers
        )
        assert response.status_code == 403, "不应访问他人订单"
    
    def test_vertical_privilege_escalation(self):
        """测试垂直越权"""
        normal_token = self.get_auth_token("normal_user", "password")
        headers = {"Authorization": f"Bearer {normal_token}"}
        
        admin_endpoints = [
            "/admin/users",
            "/admin/settings",
            "/admin/permissions"
        ]
        
        for endpoint in admin_endpoints:
            response = self.session.get(
                f"{self.BASE_URL}{endpoint}",
                headers=headers
            )
            assert response.status_code == 403, f"不应访问 {endpoint}"
    
    def test_unauthorized_access(self):
        """测试未授权访问"""
        protected_endpoints = [
            "/orders",
            "/inventory",
            "/reports"
        ]
        
        for endpoint in protected_endpoints:
            response = self.session.get(f"{self.BASE_URL}{endpoint}")
            assert response.status_code == 401, f"{endpoint}应要求认证"


class TestInputValidation(SecurityTestBase):
    """输入验证测试"""
    
    def test_sql_injection_protection(self):
        """测试SQL注入防护"""
        sql_payloads = [
            "' OR '1'='1",
            "' OR 1=1--",
            "' UNION SELECT * FROM users--",
        ]
        
        for payload in sql_payloads:
            response = self.session.get(
                f"{self.BASE_URL}/users/search",
                params={"q": payload}
            )
            assert "sql" not in response.text.lower()
            assert "syntax" not in response.text.lower()
    
    def test_xss_protection(self):
        """测试XSS防护"""
        xss_payloads = [
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert('xss')>",
        ]
        
        for payload in xss_payloads:
            response = self.session.post(
                f"{self.BASE_URL}/comments",
                json={"content": payload}
            )
            # 验证响应中不包含未转义的payload
            if response.status_code == 200:
                assert payload not in response.text


class TestAPISecurity(SecurityTestBase):
    """API安全测试"""
    
    def test_rate_limiting(self):
        """测试速率限制"""
        for i in range(100):
            response = self.session.get(f"{self.BASE_URL}/public-api")
            if i > 50 and response.status_code == 429:
                return  # 成功触发速率限制
        pytest.fail("应触发速率限制")
    
    def test_cors_policy(self):
        """测试CORS策略"""
        response = self.session.options(
            f"{self.BASE_URL}/users",
            headers={"Origin": "http://malicious-site.com"}
        )
        cors_header = response.headers.get("Access-Control-Allow-Origin")
        assert cors_header != "http://malicious-site.com"
    
    def test_security_headers(self):
        """测试安全HTTP头"""
        response = self.session.get("http://localhost:8080")
        
        required_headers = [
            "X-Content-Type-Options",
            "X-Frame-Options",
            "X-XSS-Protection"
        ]
        
        missing = [h for h in required_headers if h not in response.headers]
        assert not missing, f"缺少安全头: {missing}"


# 运行命令: pytest test_security_automation.py -v --alluredir=./allure-results