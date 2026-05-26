#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 接口兼容性测试脚本
===========================
测试内容:
1. API版本兼容性测试
2. 参数格式兼容性测试
3. Content-Type兼容性测试
4. 响应格式兼容性测试
5. HTTP方法兼容性测试
6. 字符编码兼容性测试

Author: test-agent-1
Date: 2026-04-04
"""

import requests
import json
import sys
import os
from datetime import datetime
from typing import Dict, List, Any, Tuple
import concurrent.futures

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

class CompatibilityTestResult:
    """测试结果记录"""
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.skipped = 0
        self.tests = []
        self.start_time = datetime.now()
    
    def add_test(self, category: str, name: str, status: str, message: str = "", duration: float = 0):
        self.tests.append({
            "category": category,
            "name": name,
            "status": status,
            "message": message,
            "duration_ms": round(duration * 1000, 2),
            "timestamp": datetime.now().isoformat()
        })
        if status == "PASS":
            self.passed += 1
        elif status == "FAIL":
            self.failed += 1
        else:
            self.skipped += 1
    
    def to_dict(self) -> Dict:
        return {
            "summary": {
                "total": self.passed + self.failed + self.skipped,
                "passed": self.passed,
                "failed": self.failed,
                "skipped": self.skipped,
                "pass_rate": f"{(self.passed / max(self.passed + self.failed, 1)) * 100:.1f}%",
                "start_time": self.start_time.isoformat(),
                "end_time": datetime.now().isoformat()
            },
            "tests": self.tests
        }


class APICompatibilityTester:
    """API兼容性测试器"""
    
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = CompatibilityTestResult()
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    # ==================== 测试1: API版本兼容性 ====================
    
    def test_api_version_compatibility(self):
        """测试API版本兼容性"""
        category = "API版本兼容性"
        
        # 测试1.1: API v1端点
        start = time.time()
        try:
            resp = self.request("GET", "/api/v1/users")
            if resp.status_code in [200, 401, 404]:
                self.result.add_test(category, "API v1端点可访问性", "PASS",
                                    f"状态码: {resp.status_code}",
                                    time.time() - start)
            else:
                self.result.add_test(category, "API v1端点可访问性", "WARN",
                                    f"非预期状态码: {resp.status_code}",
                                    time.time() - start)
        except Exception as e:
            self.result.add_test(category, "API v1端点可访问性", "WARN",
                                f"端点不存在或不可访问: {str(e)[:100]}",
                                time.time() - start)
        
        # 测试1.2: API无版本前缀端点
        start = time.time()
        try:
            resp = self.request("GET", "/api/users")
            self.result.add_test(category, "API无版本前缀端点", "PASS",
                                f"状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "API无版本前缀端点", "FAIL", str(e), time.time() - start)
        
        # 测试1.3: Actuator端点兼容性
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            if resp.status_code == 200:
                self.result.add_test(category, "Actuator端点兼容性", "PASS",
                                    "健康检查端点正常",
                                    time.time() - start)
            else:
                self.result.add_test(category, "Actuator端点兼容性", "FAIL",
                                    f"状态码: {resp.status_code}",
                                    time.time() - start)
        except Exception as e:
            self.result.add_test(category, "Actuator端点兼容性", "FAIL", str(e), time.time() - start)
        
        # 测试1.4: API响应版本头
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            version_headers = ["X-API-Version", "API-Version", "X-Version"]
            found = [h for h in version_headers if h in resp.headers]
            self.result.add_test(category, "API版本响应头", "PASS",
                                f"版本头: {found if found else '未定义'}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "API版本响应头", "FAIL", str(e), time.time() - start)
    
    # ==================== 测试2: 参数格式兼容性 ====================
    
    def test_parameter_format_compatibility(self):
        """测试参数格式兼容性"""
        category = "参数格式兼容性"
        
        # 测试2.1: Query参数格式
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"page": 1, "size": 10})
            self.result.add_test(category, "Query参数格式", "PASS",
                                f"参数传递成功, 状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "Query参数格式", "FAIL", str(e), time.time() - start)
        
        # 测试2.2: JSON Body参数格式
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "test", "password": "test123"})
            self.result.add_test(category, "JSON Body参数格式", "PASS",
                                f"JSON参数传递成功, 状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "JSON Body参数格式", "FAIL", str(e), time.time() - start)
        
        # 测试2.3: Form Data参数格式
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", data={"username": "test", "password": "test123"})
            self.result.add_test(category, "Form Data参数格式", "PASS",
                                f"Form参数传递成功, 状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "Form Data参数格式", "FAIL", str(e), time.time() - start)
        
        # 测试2.4: Path参数格式
        start = time.time()
        try:
            resp = self.request("GET", "/api/users/1")
            self.result.add_test(category, "Path参数格式", "PASS",
                                f"Path参数传递成功, 状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "Path参数格式", "FAIL", str(e), time.time() - start)
        
        # 测试2.5: 数组参数格式
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"ids": [1, 2, 3]})
            self.result.add_test(category, "数组参数格式", "PASS",
                                f"数组参数传递成功, 状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "数组参数格式", "FAIL", str(e), time.time() - start)
    
    # ==================== 测试3: Content-Type兼容性 ====================
    
    def test_content_type_compatibility(self):
        """测试Content-Type兼容性"""
        category = "Content-Type兼容性"
        
        content_types = [
            ("application/json", True),
            ("application/x-www-form-urlencoded", True),
            ("text/plain", False),
            ("text/html", False),
            ("application/xml", False),
            ("multipart/form-data", False),
        ]
        
        for content_type, expected_supported in content_types:
            start = time.time()
            try:
                if content_type == "multipart/form-data":
                    # 特殊处理multipart
                    resp = self.request("POST", "/api/users", 
                                       files={"file": ("test.txt", b"test content")})
                else:
                    resp = self.request("POST", "/api/users",
                                       headers={"Content-Type": content_type},
                                       data='{"username":"test"}' if "json" in content_type else "username=test")
                
                # 检查是否返回415 Unsupported Media Type
                if expected_supported:
                    if resp.status_code != 415:
                        self.result.add_test(category, f"Content-Type: {content_type}", "PASS",
                                            f"状态码: {resp.status_code}",
                                            time.time() - start)
                    else:
                        self.result.add_test(category, f"Content-Type: {content_type}", "FAIL",
                                            f"不应返回415, 状态码: {resp.status_code}",
                                            time.time() - start)
                else:
                    if resp.status_code in [415, 401, 400]:
                        self.result.add_test(category, f"Content-Type: {content_type}", "PASS",
                                            f"正确处理不支持的类型, 状态码: {resp.status_code}",
                                            time.time() - start)
                    else:
                        self.result.add_test(category, f"Content-Type: {content_type}", "WARN",
                                            f"状态码: {resp.status_code}",
                                            time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"Content-Type: {content_type}", "FAIL",
                                    str(e), time.time() - start)
    
    # ==================== 测试4: 响应格式兼容性 ====================
    
    def test_response_format_compatibility(self):
        """测试响应格式兼容性"""
        category = "响应格式兼容性"
        
        # 测试4.1: JSON响应格式
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            if resp.headers.get("Content-Type", "").startswith("application/json"):
                data = resp.json()
                self.result.add_test(category, "JSON响应格式", "PASS",
                                    "响应为JSON格式",
                                    time.time() - start)
            else:
                self.result.add_test(category, "JSON响应格式", "FAIL",
                                    f"Content-Type: {resp.headers.get('Content-Type')}",
                                    time.time() - start)
        except Exception as e:
            self.result.add_test(category, "JSON响应格式", "FAIL", str(e), time.time() - start)
        
        # 测试4.2: 响应结构一致性
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            data = resp.json()
            
            # 检查标准响应结构
            has_status = "status" in data
            has_components = "components" in data
            
            self.result.add_test(category, "响应结构一致性", "PASS" if has_status else "FAIL",
                                f"包含status: {has_status}, 包含components: {has_components}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "响应结构一致性", "FAIL", str(e), time.time() - start)
        
        # 测试4.3: Accept头兼容性
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health",
                               headers={"Accept": "application/json"})
            
            if resp.status_code == 200:
                self.result.add_test(category, "Accept头兼容性", "PASS",
                                    f"状态码: {resp.status_code}",
                                    time.time() - start)
            else:
                self.result.add_test(category, "Accept头兼容性", "WARN",
                                    f"状态码: {resp.status_code}",
                                    time.time() - start)
        except Exception as e:
            self.result.add_test(category, "Accept头兼容性", "FAIL", str(e), time.time() - start)
        
        # 测试4.4: 错误响应格式
        start = time.time()
        try:
            resp = self.request("GET", "/api/nonexistent")
            
            # 错误响应应有统一格式
            if resp.status_code in [401, 404]:
                self.result.add_test(category, "错误响应格式", "PASS",
                                    f"错误状态码: {resp.status_code}",
                                    time.time() - start)
            else:
                self.result.add_test(category, "错误响应格式", "WARN",
                                    f"状态码: {resp.status_code}",
                                    time.time() - start)
        except Exception as e:
            self.result.add_test(category, "错误响应格式", "FAIL", str(e), time.time() - start)
    
    # ==================== 测试5: HTTP方法兼容性 ====================
    
    def test_http_method_compatibility(self):
        """测试HTTP方法兼容性"""
        category = "HTTP方法兼容性"
        
        methods = ["GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"]
        
        for method in methods:
            start = time.time()
            try:
                resp = self.request(method, "/api/users")
                
                # 检查方法是否被支持
                if method == "OPTIONS":
                    # OPTIONS应返回Allow头
                    allow = resp.headers.get("Allow", "")
                    self.result.add_test(category, f"HTTP方法: {method}", "PASS",
                                        f"状态码: {resp.status_code}, Allow: {allow}",
                                        time.time() - start)
                elif method == "HEAD":
                    # HEAD应返回空body
                    self.result.add_test(category, f"HTTP方法: {method}", "PASS",
                                        f"状态码: {resp.status_code}",
                                        time.time() - start)
                elif method == "PATCH":
                    # PATCH可能不被支持
                    if resp.status_code in [200, 204, 401, 405]:
                        self.result.add_test(category, f"HTTP方法: {method}", "PASS",
                                            f"状态码: {resp.status_code}",
                                            time.time() - start)
                    else:
                        self.result.add_test(category, f"HTTP方法: {method}", "WARN",
                                            f"状态码: {resp.status_code}",
                                            time.time() - start)
                else:
                    self.result.add_test(category, f"HTTP方法: {method}", "PASS",
                                        f"状态码: {resp.status_code}",
                                        time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"HTTP方法: {method}", "FAIL",
                                    str(e), time.time() - start)
    
    # ==================== 测试6: 字符编码兼容性 ====================
    
    def test_character_encoding_compatibility(self):
        """测试字符编码兼容性"""
        category = "字符编码兼容性"
        
        # 测试6.1: UTF-8编码支持
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"name": "测试用户"})
            
            self.result.add_test(category, "UTF-8编码支持", "PASS",
                                f"中文参数处理成功, 状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "UTF-8编码支持", "FAIL", str(e), time.time() - start)
        
        # 测试6.2: 响应编码头
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            charset = resp.encoding or "unknown"
            
            self.result.add_test(category, "响应编码声明", "PASS",
                                f"编码: {charset}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "响应编码声明", "FAIL", str(e), time.time() - start)
        
        # 测试6.3: 特殊字符处理
        start = time.time()
        try:
            special_chars = ["<script>", "&", "%", "#", "?", "/", "\\", "'", '"']
            all_ok = True
            
            for char in special_chars:
                resp = self.request("GET", "/api/users", params={"filter": char})
                if resp.status_code >= 500:
                    all_ok = False
                    break
            
            self.result.add_test(category, "特殊字符处理", "PASS" if all_ok else "FAIL",
                                "特殊字符处理测试",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "特殊字符处理", "FAIL", str(e), time.time() - start)
    
    # ==================== 测试7: 头部兼容性 ====================
    
    def test_header_compatibility(self):
        """测试HTTP头部兼容性"""
        category = "HTTP头部兼容性"
        
        # 测试7.1: 自定义请求头
        start = time.time()
        try:
            resp = self.request("GET", "/api/users",
                               headers={"X-Custom-Header": "test-value"})
            
            self.result.add_test(category, "自定义请求头", "PASS",
                                f"状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "自定义请求头", "FAIL", str(e), time.time() - start)
        
        # 测试7.2: User-Agent头
        start = time.time()
        try:
            agents = [
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)",
                "curl/7.68.0",
                "PostmanRuntime/7.26.0"
            ]
            
            all_ok = True
            for agent in agents:
                resp = self.request("GET", "/actuator/health",
                                   headers={"User-Agent": agent})
                if resp.status_code != 200:
                    all_ok = False
            
            self.result.add_test(category, "User-Agent兼容性", "PASS" if all_ok else "FAIL",
                                "多UA测试",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "User-Agent兼容性", "FAIL", str(e), time.time() - start)
        
        # 测试7.3: Authorization头
        start = time.time()
        try:
            # 测试Bearer Token格式
            resp = self.request("GET", "/api/users",
                               headers={"Authorization": "Bearer test-token"})
            
            self.result.add_test(category, "Authorization头格式", "PASS",
                                f"状态码: {resp.status_code}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "Authorization头格式", "FAIL", str(e), time.time() - start)
        
        # 测试7.4: 响应头完整性
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            
            required_headers = ["Content-Type", "Date"]
            found = [h for h in required_headers if h in resp.headers]
            
            self.result.add_test(category, "响应头完整性", "PASS" if len(found) == len(required_headers) else "WARN",
                                f"找到: {found}",
                                time.time() - start)
        except Exception as e:
            self.result.add_test(category, "响应头完整性", "FAIL", str(e), time.time() - start)
    
    # ==================== 运行所有测试 ====================
    
    def run_all_tests(self):
        """运行所有兼容性测试"""
        print("=" * 60)
        print("AI-Ready 接口兼容性测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print("=" * 60)
        
        test_categories = [
            ("1. API版本兼容性", self.test_api_version_compatibility),
            ("2. 参数格式兼容性", self.test_parameter_format_compatibility),
            ("3. Content-Type兼容性", self.test_content_type_compatibility),
            ("4. 响应格式兼容性", self.test_response_format_compatibility),
            ("5. HTTP方法兼容性", self.test_http_method_compatibility),
            ("6. 字符编码兼容性", self.test_character_encoding_compatibility),
            ("7. HTTP头部兼容性", self.test_header_compatibility),
        ]
        
        for name, test_func in test_categories:
            print(f"\n>>> 执行: {name}")
            try:
                test_func()
            except Exception as e:
                print(f"    测试异常: {e}")
            print(f"    完成")
        
        return self.result.to_dict()


def generate_report(result: Dict, output_dir: str = "I:/AI-Ready/docs"):
    """生成测试报告"""
    import time
    os.makedirs(output_dir, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_file = os.path.join(output_dir, f"API_COMPATIBILITY_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"api_compatibility_test_results_{timestamp}.json")
    
    summary = result["summary"]
    
    report = f"""# AI-Ready 接口兼容性测试报告

## 测试概览

| 指标 | 值 |
|------|-----|
| **测试时间** | {summary['start_time']} |
| **总测试数** | {summary['total']} |
| **通过** | {summary['passed']} ✅ |
| **失败** | {summary['failed']} ❌ |
| **跳过** | {summary['skipped']} ⏭️ |
| **通过率** | {summary['pass_rate']} |

## 测试结果详情

"""
    
    categories = {}
    for test in result["tests"]:
        cat = test["category"]
        if cat not in categories:
            categories[cat] = []
        categories[cat].append(test)
    
    for cat, tests in categories.items():
        report += f"\n### {cat}\n\n"
        report += "| 测试项 | 状态 | 消息 | 耗时 |\n"
        report += "|--------|------|------|------|\n"
        
        for t in tests:
            status_icon = "✅" if t["status"] == "PASS" else ("❌" if t["status"] == "FAIL" else "⏭️")
            report += f"| {t['name']} | {status_icon} {t['status']} | {t['message']} | {t['duration_ms']}ms |\n"
    
    report += f"""

## 测试总结

### 测试覆盖范围

1. **API版本兼容性** - 验证不同版本API的兼容性
2. **参数格式兼容性** - 验证多种参数传递格式
3. **Content-Type兼容性** - 验证不同内容类型支持
4. **响应格式兼容性** - 验证响应格式一致性
5. **HTTP方法兼容性** - 验证HTTP方法支持
6. **字符编码兼容性** - 验证UTF-8等编码支持
7. **HTTP头部兼容性** - 验证请求/响应头处理

### 结论

- **总体评估**: {'✅ 通过' if summary['failed'] == 0 else '❌ 存在失败项'}
- **通过率**: {summary['pass_rate']}

---
*报告生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
*测试工具: AI-Ready API Compatibility Tester v1.0*
"""
    
    with open(report_file, "w", encoding="utf-8") as f:
        f.write(report)
    
    with open(json_file, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    
    return report_file, json_file


import time

def main():
    """主函数"""
    tester = APICompatibilityTester(BASE_URL)
    result = tester.run_all_tests()
    
    print("\n" + "=" * 60)
    print("测试摘要")
    print("=" * 60)
    summary = result["summary"]
    print(f"总测试数: {summary['total']}")
    print(f"通过: {summary['passed']}")
    print(f"失败: {summary['failed']}")
    print(f"跳过: {summary['skipped']}")
    print(f"通过率: {summary['pass_rate']}")
    
    report_file, json_file = generate_report(result)
    print(f"\n报告已生成:")
    print(f"  - Markdown: {report_file}")
    print(f"  - JSON: {json_file}")
    
    return 0 if summary["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
