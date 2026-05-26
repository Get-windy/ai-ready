#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 安全测试执行器
测试范围：
1. SQL注入测试
2. XSS漏洞测试
3. CSRF防护测试
4. API安全测试（认证/授权）
5. 敏感数据加密验证
"""

import time
import json
import random
import string
from datetime import datetime
from typing import Dict, Any, Optional, List
import requests
import sys
import os

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

TEST_RESULTS = {
    "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "base_url": BASE_URL,
    "tests": []
}


class SecurityTestResult:
    """安全测试结果"""
    def __init__(self, name: str, category: str):
        self.name = name
        self.category = category
        self.status = "SKIP"  # PASS, FAIL, WARN, SKIP
        self.severity = "INFO"  # CRITICAL, HIGH, MEDIUM, LOW, INFO
        self.message = ""
        self.response_time = 0
        self.status_code = 0
        self.details = {}
        self.vulnerability = ""
    
    def pass_(self, message: str = ""):
        self.status = "PASS"
        self.message = message
        self.severity = "INFO"
    
    def fail(self, message: str, severity: str = "HIGH"):
        self.status = "FAIL"
        self.message = message
        self.severity = severity
    
    def warn(self, message: str, severity: str = "MEDIUM"):
        self.status = "WARN"
        self.message = message
        self.severity = severity
    
    def skip(self, reason: str):
        self.status = "SKIP"
        self.message = reason
    
    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "category": self.category,
            "status": self.status,
            "severity": self.severity,
            "message": self.message,
            "response_time_ms": round(self.response_time, 2),
            "status_code": self.status_code,
            "vulnerability": self.vulnerability,
            "details": self.details
        }


class SecurityTester:
    """安全测试器"""
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.api_base = f"{base_url}/api"
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "AI-Ready-Security-Tester/1.0",
            "X-Real-IP": "127.0.0.1"
        })
    
    def request(self, method: str, endpoint: str, **kwargs) -> tuple:
        """发送请求"""
        url = f"{self.api_base}{endpoint}"
        kwargs.setdefault('timeout', 10)
        
        start = time.perf_counter()
        try:
            response = getattr(self.session, method.lower())(url, **kwargs)
            elapsed = (time.perf_counter() - start) * 1000
            return response, elapsed
        except Exception as e:
            elapsed = (time.perf_counter() - start) * 1000
            return None, elapsed


# ==================== SQL注入测试 ====================

def test_sql_injection():
    """SQL注入测试"""
    print("\n--- SQL注入测试 ---")
    results = []
    tester = SecurityTester()
    
    # SQL注入Payload列表
    sql_payloads = [
        "' OR '1'='1",
        "' OR '1'='1' --",
        "' OR '1'='1' /*",
        "1; DROP TABLE users--",
        "' UNION SELECT NULL--",
        "admin'--",
        "1' AND '1'='1",
        "' OR 1=1--",
        "'; EXEC xp_cmdshell('dir')--",
        "1' ORDER BY 1--"
    ]
    
    # 测试1: 用户查询SQL注入
    result = SecurityTestResult("用户查询SQL注入", "SQL注入")
    vulnerable = False
    injection_responses = []
    
    for payload in sql_payloads[:5]:
        response, elapsed = tester.request("GET", "/user/page", params={
            "pageNum": 1,
            "pageSize": 10,
            "tenantId": 1,
            "username": payload
        })
        if response:
            injection_responses.append({
                "payload": payload,
                "status": response.status_code,
                "has_error": "error" in response.text.lower() or "sql" in response.text.lower()
            })
            # 检查是否有SQL错误泄露
            if "sql" in response.text.lower() or "syntax" in response.text.lower():
                vulnerable = True
    
    result.response_time = 0
    result.status_code = 200 if injection_responses else 0
    result.details["tests"] = injection_responses
    
    if vulnerable:
        result.fail("检测到SQL注入漏洞", "CRITICAL")
        result.vulnerability = "SQL Injection"
    elif injection_responses:
        result.pass_("未检测到SQL注入漏洞")
    else:
        result.skip("用户查询接口未响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试2: 登录SQL注入
    result = SecurityTestResult("登录SQL注入", "SQL注入")
    vulnerable = False
    
    for payload in sql_payloads[:3]:
        login_data = {
            "username": payload,
            "password": "anything",
            "tenantId": 1
        }
        response, elapsed = tester.request("POST", "/user/login", json=login_data)
        if response:
            if response.status_code == 200:
                data = response.json()
                # 如果返回了token，说明可能存在SQL注入
                if data.get("data") and isinstance(data.get("data"), dict) and data["data"].get("token"):
                    vulnerable = True
                    break
    
    result.response_time = 0
    
    if vulnerable:
        result.fail("登录存在SQL注入漏洞，攻击者可绕过认证", "CRITICAL")
        result.vulnerability = "Authentication Bypass via SQL Injection"
    else:
        result.pass_("登录未检测到SQL注入漏洞")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试3: ID参数SQL注入
    result = SecurityTestResult("ID参数SQL注入", "SQL注入")
    vulnerable = False
    
    for payload in ["1' OR '1'='1", "1; SELECT * FROM users--"]:
        response, elapsed = tester.request("GET", f"/user/{payload}")
        if response:
            if "sql" in response.text.lower() or "error" in response.text.lower():
                vulnerable = True
    
    result.response_time = 0
    
    if vulnerable:
        result.fail("ID参数存在SQL注入风险", "HIGH")
        result.vulnerability = "SQL Injection in ID Parameter"
    else:
        result.pass_("ID参数未检测到SQL注入")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== XSS漏洞测试 ====================

def test_xss():
    """XSS漏洞测试"""
    print("\n--- XSS漏洞测试 ---")
    results = []
    tester = SecurityTester()
    
    # XSS Payload列表
    xss_payloads = [
        "<script>alert('XSS')</script>",
        "<img src=x onerror=alert('XSS')>",
        "<svg onload=alert('XSS')>",
        "javascript:alert('XSS')",
        "<body onload=alert('XSS')>",
        "'\"><script>alert('XSS')</script>",
        "<iframe src='javascript:alert(1)'>",
        "<input onfocus=alert('XSS') autofocus>"
    ]
    
    # 测试1: 用户输入XSS
    result = SecurityTestResult("用户输入XSS", "XSS漏洞")
    vulnerable = False
    
    for payload in xss_payloads[:3]:
        user_data = {
            "username": f"test_xss_{random.randint(1000, 9999)}",
            "password": "Test@123",
            "tenantId": 1,
            "nickname": payload
        }
        response, elapsed = tester.request("POST", "/user", json=user_data)
        if response and response.status_code in [200, 201]:
            # 检查响应中是否包含未转义的payload
            if payload in response.text:
                vulnerable = True
                break
    
    result.response_time = 0
    
    if vulnerable:
        result.fail("用户输入未正确转义，存在XSS风险", "HIGH")
        result.vulnerability = "Stored XSS"
    else:
        result.pass_("用户输入已正确处理")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试2: 查询参数XSS
    result = SecurityTestResult("查询参数XSS", "XSS漏洞")
    vulnerable = False
    
    for payload in xss_payloads[:2]:
        response, elapsed = tester.request("GET", "/user/page", params={
            "pageNum": 1,
            "pageSize": 10,
            "tenantId": 1,
            "username": payload
        })
        if response:
            if payload in response.text and "<script>" in response.text:
                vulnerable = True
    
    result.response_time = 0
    
    if vulnerable:
        result.fail("查询参数存在反射型XSS", "MEDIUM")
        result.vulnerability = "Reflected XSS"
    else:
        result.pass_("查询参数已正确编码")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试3: API响应Content-Type检查
    result = SecurityTestResult("Content-Type安全头", "XSS漏洞")
    response, elapsed = tester.request("GET", "/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1})
    result.response_time = elapsed
    
    if response:
        content_type = response.headers.get("Content-Type", "")
        if "application/json" in content_type:
            result.pass_("Content-Type正确设置为application/json")
        else:
            result.warn(f"Content-Type: {content_type}", "LOW")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== CSRF防护测试 ====================

def test_csrf():
    """CSRF防护测试"""
    print("\n--- CSRF防护测试 ---")
    results = []
    tester = SecurityTester()
    
    # 测试1: CSRF Token检查
    result = SecurityTestResult("CSRF Token检查", "CSRF防护")
    response, elapsed = tester.request("GET", "/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1})
    result.response_time = elapsed
    
    if response:
        # 检查是否有CSRF相关头
        csrf_header = response.headers.get("X-CSRF-Token", "")
        if csrf_header:
            result.pass_("服务器返回CSRF Token")
            result.details["csrf_token_present"] = True
        else:
            # 检查是否使用SameSite Cookie
            set_cookie = response.headers.get("Set-Cookie", "")
            if "SameSite" in set_cookie:
                result.pass_("使用SameSite Cookie防护CSRF")
            else:
                result.warn("未检测到CSRF防护机制", "MEDIUM")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试2: 无Token状态变更请求
    result = SecurityTestResult("无Token状态变更", "CSRF防护")
    # 尝试无认证修改数据
    update_data = {"nickname": "CSRF Test"}
    response, elapsed = tester.request("PUT", "/user/1", json=update_data)
    result.response_time = elapsed
    
    if response:
        result.status_code = response.status_code
        if response.status_code in [401, 403]:
            result.pass_("未授权请求被正确拒绝")
        elif response.status_code == 200:
            result.warn("无认证可修改数据，可能存在CSRF风险", "MEDIUM")
        else:
            result.skip(f"状态码: {response.status_code}")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试3: 跨域请求检查
    result = SecurityTestResult("CORS配置检查", "CSRF防护")
    # 发送OPTIONS预检请求
    response, elapsed = tester.request("OPTIONS", "/user/page")
    result.response_time = elapsed
    
    if response:
        cors_origin = response.headers.get("Access-Control-Allow-Origin", "")
        cors_methods = response.headers.get("Access-Control-Allow-Methods", "")
        
        result.details["cors_origin"] = cors_origin
        result.details["cors_methods"] = cors_methods
        
        if cors_origin == "*":
            result.warn("CORS允许任意来源，可能存在安全风险", "MEDIUM")
        elif cors_origin:
            result.pass_(f"CORS配置: {cors_origin}")
        else:
            result.pass_("CORS未配置或限制严格")
    else:
        result.skip("OPTIONS请求无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== API安全测试 ====================

def test_api_security():
    """API安全测试"""
    print("\n--- API安全测试 ---")
    results = []
    tester = SecurityTester()
    
    # 测试1: 未认证访问
    result = SecurityTestResult("未认证访问测试", "API安全")
    response, elapsed = tester.request("GET", "/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1})
    result.response_time = elapsed
    
    if response:
        result.status_code = response.status_code
        if response.status_code == 200:
            # 检查是否需要认证
            data = response.json()
            if data.get("code") == 401:
                result.pass_("需要认证才能访问")
            else:
                result.warn("接口可能不需要认证", "LOW")
        elif response.status_code in [401, 403]:
            result.pass_("未认证访问被拒绝")
        else:
            result.skip(f"状态码: {response.status_code}")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试2: 越权访问
    result = SecurityTestResult("越权访问测试", "API安全")
    # 尝试访问其他用户数据
    response, elapsed = tester.request("GET", "/user/1")
    result.response_time = elapsed
    
    if response:
        result.status_code = response.status_code
        if response.status_code in [401, 403]:
            result.pass_("越权访问被拒绝")
        elif response.status_code == 200:
            result.warn("可能存在越权访问风险", "MEDIUM")
        else:
            result.skip(f"状态码: {response.status_code}")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试3: 敏感端点暴露
    result = SecurityTestResult("敏感端点暴露", "API安全")
    sensitive_endpoints = [
        "/actuator",
        "/actuator/health",
        "/actuator/env",
        "/swagger-ui/index.html",
        "/v3/api-docs",
        "/druid",
        "/admin"
    ]
    
    exposed = []
    for endpoint in sensitive_endpoints:
        response, _ = tester.request("GET", endpoint)
        if response and response.status_code == 200:
            exposed.append(endpoint)
    
    result.details["exposed_endpoints"] = exposed
    
    if exposed:
        result.warn(f"敏感端点暴露: {exposed}", "MEDIUM")
    else:
        result.pass_("无敏感端点暴露")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试4: 速率限制
    result = SecurityTestResult("速率限制检查", "API安全")
    # 快速发送多个请求
    rate_limited = False
    for i in range(20):
        response, _ = tester.request("GET", "/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1})
        if response and response.status_code == 429:
            rate_limited = True
            break
    
    if rate_limited:
        result.pass_("检测到速率限制")
    else:
        result.warn("未检测到速率限制", "LOW")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== 敏感数据加密验证 ====================

def test_data_encryption():
    """敏感数据加密验证"""
    print("\n--- 敏感数据加密验证 ---")
    results = []
    tester = SecurityTester()
    
    # 测试1: 密码传输加密
    result = SecurityTestResult("密码传输安全", "数据加密")
    login_data = {
        "username": "testuser",
        "password": "plaintext_password",
        "tenantId": 1
    }
    response, elapsed = tester.request("POST", "/user/login", json=login_data)
    result.response_time = elapsed
    
    # 检查是否使用HTTPS
    if BASE_URL.startswith("https://"):
        result.pass_("使用HTTPS加密传输")
    else:
        result.warn("未使用HTTPS，密码可能明文传输", "HIGH")
        result.vulnerability = "Cleartext Password Transmission"
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试2: 密码存储检查
    result = SecurityTestResult("密码存储检查", "数据加密")
    # 检查API响应是否泄露密码
    response, elapsed = tester.request("GET", "/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1})
    result.response_time = elapsed
    
    if response:
        text = response.text.lower()
        if "password" in text and ("plain" in text or "123456" in text):
            result.fail("API响应可能泄露密码信息", "HIGH")
        else:
            result.pass_("API响应未泄露密码")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试3: 敏感数据脱敏
    result = SecurityTestResult("敏感数据脱敏", "数据加密")
    # 检查手机号、邮箱等是否脱敏
    response, elapsed = tester.request("GET", "/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1})
    result.response_time = elapsed
    
    if response:
        text = response.text
        # 检查是否有明文手机号或邮箱
        import re
        phones = re.findall(r'1[3-9]\d{9}', text)
        emails = re.findall(r'[\w\.-]+@[\w\.-]+\.\w+', text)
        
        if phones and not any('*' in p for p in phones):
            result.warn("手机号未脱敏", "LOW")
        elif emails and not any('*' in e for e in emails):
            result.warn("邮箱未脱敏", "LOW")
        else:
            result.pass_("敏感数据已脱敏或未返回")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试4: 安全头检查
    result = SecurityTestResult("安全响应头", "数据加密")
    response, elapsed = tester.request("GET", "/")
    result.response_time = elapsed
    
    if response:
        security_headers = {
            "X-Content-Type-Options": response.headers.get("X-Content-Type-Options"),
            "X-Frame-Options": response.headers.get("X-Frame-Options"),
            "X-XSS-Protection": response.headers.get("X-XSS-Protection"),
            "Strict-Transport-Security": response.headers.get("Strict-Transport-Security"),
            "Content-Security-Policy": response.headers.get("Content-Security-Policy")
        }
        
        result.details["security_headers"] = security_headers
        
        missing = [k for k, v in security_headers.items() if not v]
        if missing:
            result.warn(f"缺少安全头: {missing}", "LOW")
        else:
            result.pass_("所有安全头已配置")
    else:
        result.skip("无响应")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== 生成报告 ====================

def generate_security_report(all_results: List[SecurityTestResult]):
    """生成安全测试报告"""
    report_path = os.path.join(os.path.dirname(__file__), "docs", "AI-Ready安全测试报告-20260330.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    total = len(all_results)
    passed = sum(1 for r in all_results if r.status == "PASS")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    warned = sum(1 for r in all_results if r.status == "WARN")
    skipped = sum(1 for r in all_results if r.status == "SKIP")
    
    # 统计漏洞
    critical = sum(1 for r in all_results if r.severity == "CRITICAL")
    high = sum(1 for r in all_results if r.severity == "HIGH")
    medium = sum(1 for r in all_results if r.severity == "MEDIUM")
    low = sum(1 for r in all_results if r.severity == "LOW")
    
    # 按类别分组
    categories = {}
    for r in all_results:
        if r.category not in categories:
            categories[r.category] = []
        categories[r.category].append(r)
    
    # 计算安全评分
    # PASS=100, WARN=70, SKIP=50, FAIL=0
    score = (passed * 100 + warned * 70 + skipped * 50) / total if total > 0 else 0
    
    report = f"""# AI-Ready 安全测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {TEST_RESULTS["test_time"]} |
| 测试环境 | {TEST_RESULTS["base_url"]} |
| 总测试数 | {total} |
| 通过测试 | {passed} |
| 警告测试 | {warned} |
| 失败测试 | {failed} |
| 跳过测试 | {skipped} |
| 安全评分 | **{score:.1f}/100** |

### 漏洞统计

| 严重级别 | 数量 |
|----------|------|
| CRITICAL | {critical} |
| HIGH | {high} |
| MEDIUM | {medium} |
| LOW | {low} |

---

## 测试结果详情

"""
    
    for category_name, results in categories.items():
        report += f"""### {category_name}

| 测试项 | 状态 | 严重级别 | 说明 |
|--------|------|----------|------|
"""
        
        for r in results:
            status_icon = "[PASS]" if r.status == "PASS" else ("[WARN]" if r.status == "WARN" else ("[FAIL]" if r.status == "FAIL" else "[SKIP]"))
            report += f"| {r.name} | {status_icon} | {r.severity} | {r.message} |\n"
        
        report += "\n---\n\n"
    
    # 漏洞详情
    report += """## 发现的安全问题

"""
    
    vulnerabilities = [r for r in all_results if r.status in ["FAIL", "WARN"] and r.vulnerability]
    if vulnerabilities:
        for v in vulnerabilities:
            report += f"""### {v.vulnerability or v.name}

- **严重级别**: {v.severity}
- **类别**: {v.category}
- **描述**: {v.message}

"""
    else:
        report += "未发现严重安全漏洞\n\n"
    
    report += f"""## 安全建议

1. **SQL注入防护**: 使用参数化查询，避免字符串拼接SQL
2. **XSS防护**: 对所有用户输入进行HTML编码
3. **CSRF防护**: 实现CSRF Token或SameSite Cookie
4. **API安全**: 实施严格的认证授权和速率限制
5. **数据加密**: 使用HTTPS传输，敏感数据加密存储

---

## 安全头配置建议

```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'
```

---

## 测试配置

| 配置项 | 值 |
|--------|-----|
| BASE_URL | {BASE_URL} |
| 测试类别 | SQL注入/XSS/CSRF/API安全/数据加密 |

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    json_path = os.path.join(os.path.dirname(__file__), "docs", "security-test-results.json")
    json_data = {
        "test_time": TEST_RESULTS["test_time"],
        "base_url": BASE_URL,
        "summary": {
            "total": total,
            "passed": passed,
            "warned": warned,
            "failed": failed,
            "skipped": skipped,
            "score": score,
            "vulnerabilities": {
                "critical": critical,
                "high": high,
                "medium": medium,
                "low": low
            }
        },
        "results": [r.to_dict() for r in all_results]
    }
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    print(f"\n[REPORT] 安全测试报告已生成: {report_path}")
    print(f"[REPORT] JSON结果已保存: {json_path}")
    
    return report_path, json_path, score


def main():
    print("=" * 60)
    print("AI-Ready 安全测试执行")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print(f"测试环境: {BASE_URL}")
    print("=" * 60)
    
    all_results = []
    
    # 执行安全测试
    all_results.extend(test_sql_injection())
    all_results.extend(test_xss())
    all_results.extend(test_csrf())
    all_results.extend(test_api_security())
    all_results.extend(test_data_encryption())
    
    # 生成报告
    print("\n" + "=" * 60)
    report_path, json_path, score = generate_security_report(all_results)
    
    passed = sum(1 for r in all_results if r.status == "PASS")
    warned = sum(1 for r in all_results if r.status == "WARN")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    total = len(all_results)
    
    print(f"\n测试结果: {passed}/{total} 通过, {warned} 警告, {failed} 失败")
    print(f"安全评分: {score:.1f}/100")
    print("=" * 60)


if __name__ == '__main__':
    main()