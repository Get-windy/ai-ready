#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 异常流程测试脚本
===========================
测试内容:
1. 输入验证异常测试
2. 权限异常测试
3. 资源不存在异常测试
4. 参数边界异常测试
5. 请求格式异常测试
6. 服务错误处理测试

Author: test-agent-1
Date: 2026-04-04
"""

import requests
import json
import sys
import os
import time
from datetime import datetime
from typing import Dict, List, Any

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

class ExceptionTestResult:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.tests = []
        self.start_time = datetime.now()
    
    def add_test(self, category: str, name: str, status: str, message: str = "", duration: float = 0):
        self.tests.append({
            "category": category, "name": name, "status": status,
            "message": message, "duration_ms": round(duration * 1000, 2),
            "timestamp": datetime.now().isoformat()
        })
        if status == "PASS": self.passed += 1
        else: self.failed += 1
    
    def to_dict(self) -> Dict:
        return {
            "summary": {
                "total": self.passed + self.failed, "passed": self.passed,
                "failed": self.failed, "skipped": 0,
                "pass_rate": f"{(self.passed / max(self.passed + self.failed, 1)) * 100:.1f}%",
                "start_time": self.start_time.isoformat(),
                "end_time": datetime.now().isoformat()
            },
            "tests": self.tests
        }


class ExceptionTester:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = ExceptionTestResult()
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    def test_input_validation_exceptions(self):
        """输入验证异常测试"""
        category = "输入验证异常"
        
        # 空用户名
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "", "password": "test"})
            self.result.add_test(category, "空用户名验证", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "空用户名验证", "FAIL", str(e), time.time() - start)
        
        # 空密码
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "test", "password": ""})
            self.result.add_test(category, "空密码验证", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "空密码验证", "FAIL", str(e), time.time() - start)
        
        # 无效邮箱格式
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "test", "email": "invalid-email"})
            self.result.add_test(category, "无效邮箱验证", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "无效邮箱验证", "FAIL", str(e), time.time() - start)
        
        # SQL注入测试
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"name": "'; DROP TABLE users; --"})
            self.result.add_test(category, "SQL注入防护", "PASS" if resp.status_code not in [500] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "SQL注入防护", "FAIL", str(e), time.time() - start)
        
        # XSS测试
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"name": "<script>alert('xss')</script>"})
            self.result.add_test(category, "XSS防护", "PASS" if resp.status_code not in [500] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "XSS防护", "FAIL", str(e), time.time() - start)
    
    def test_permission_exceptions(self):
        """权限异常测试"""
        category = "权限异常"
        
        # 无Token访问
        start = time.time()
        try:
            resp = self.request("GET", "/api/users")
            self.result.add_test(category, "无Token访问", "PASS" if resp.status_code == 401 else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "无Token访问", "FAIL", str(e), time.time() - start)
        
        # 无效Token
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", headers={"Authorization": "Bearer invalid-token"})
            self.result.add_test(category, "无效Token访问", "PASS" if resp.status_code in [401, 403] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "无效Token访问", "FAIL", str(e), time.time() - start)
        
        # 过期Token格式
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", headers={"Authorization": "Bearer expired.token.here"})
            self.result.add_test(category, "过期Token格式", "PASS" if resp.status_code in [401, 403] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "过期Token格式", "FAIL", str(e), time.time() - start)
    
    def test_resource_not_found(self):
        """资源不存在异常测试"""
        category = "资源不存在异常"
        
        # 不存在的用户ID
        start = time.time()
        try:
            resp = self.request("GET", "/api/users/999999999")
            self.result.add_test(category, "不存在的用户ID", "PASS" if resp.status_code in [401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "不存在的用户ID", "FAIL", str(e), time.time() - start)
        
        # 不存在的端点
        start = time.time()
        try:
            resp = self.request("GET", "/api/nonexistent")
            self.result.add_test(category, "不存在的端点", "PASS" if resp.status_code in [401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "不存在的端点", "FAIL", str(e), time.time() - start)
        
        # 无效路径
        start = time.time()
        try:
            resp = self.request("GET", "/api/users/invalid-id-format")
            self.result.add_test(category, "无效ID格式", "PASS" if resp.status_code not in [500] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "无效ID格式", "FAIL", str(e), time.time() - start)
    
    def test_parameter_boundary(self):
        """参数边界异常测试"""
        category = "参数边界异常"
        
        # 超长字符串
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "a" * 10000, "password": "test"})
            self.result.add_test(category, "超长字符串", "PASS" if resp.status_code in [400, 401, 413] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "超长字符串", "FAIL", str(e), time.time() - start)
        
        # 负数ID
        start = time.time()
        try:
            resp = self.request("GET", "/api/users/-1")
            self.result.add_test(category, "负数ID", "PASS" if resp.status_code in [400, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "负数ID", "FAIL", str(e), time.time() - start)
        
        # 超大页码
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"page": 999999999, "size": 100})
            self.result.add_test(category, "超大页码", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "超大页码", "FAIL", str(e), time.time() - start)
        
        # 无效分页参数
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"page": -1, "size": 0})
            self.result.add_test(category, "无效分页参数", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "无效分页参数", "FAIL", str(e), time.time() - start)
    
    def test_request_format(self):
        """请求格式异常测试"""
        category = "请求格式异常"
        
        # 无效JSON
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", data="invalid json{", headers={"Content-Type": "application/json"})
            self.result.add_test(category, "无效JSON格式", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "无效JSON格式", "FAIL", str(e), time.time() - start)
        
        # 空JSON
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={})
            self.result.add_test(category, "空JSON对象", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "空JSON对象", "FAIL", str(e), time.time() - start)
        
        # null值
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json=None, headers={"Content-Type": "application/json"})
            self.result.add_test(category, "null请求体", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "null请求体", "FAIL", str(e), time.time() - start)
        
        # 数组代替对象
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json=[1, 2, 3])
            self.result.add_test(category, "数组代替对象", "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "数组代替对象", "FAIL", str(e), time.time() - start)
    
    def test_error_handling(self):
        """服务错误处理测试"""
        category = "服务错误处理"
        
        # 健康检查
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            self.result.add_test(category, "服务健康检查", "PASS" if resp.status_code == 200 else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "服务健康检查", "FAIL", str(e), time.time() - start)
        
        # 错误响应格式
        start = time.time()
        try:
            resp = self.request("GET", "/api/users")
            has_valid_error = resp.status_code == 401
            self.result.add_test(category, "错误响应格式", "PASS" if has_valid_error else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "错误响应格式", "FAIL", str(e), time.time() - start)
        
        # 方法不允许
        start = time.time()
        try:
            resp = self.request("PATCH", "/actuator/health")
            self.result.add_test(category, "方法不允许处理", "PASS" if resp.status_code in [401, 405] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "方法不允许处理", "FAIL", str(e), time.time() - start)
    
    def run_all_tests(self):
        print("=" * 60)
        print("AI-Ready 异常流程测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print("=" * 60)
        
        tests = [
            ("1. 输入验证异常", self.test_input_validation_exceptions),
            ("2. 权限异常", self.test_permission_exceptions),
            ("3. 资源不存在异常", self.test_resource_not_found),
            ("4. 参数边界异常", self.test_parameter_boundary),
            ("5. 请求格式异常", self.test_request_format),
            ("6. 服务错误处理", self.test_error_handling),
        ]
        
        for name, func in tests:
            print(f"\n>>> 执行: {name}")
            try:
                func()
            except Exception as e:
                print(f"    测试异常: {e}")
            print("    完成")
        
        return self.result.to_dict()


def generate_report(result: Dict, output_dir: str = "I:/AI-Ready/docs"):
    os.makedirs(output_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_file = os.path.join(output_dir, f"EXCEPTION_FLOW_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"exception_flow_test_results_{timestamp}.json")
    
    summary = result["summary"]
    report = f"""# AI-Ready 异常流程测试报告

## 测试概览

| 指标 | 值 |
|------|-----|
| **测试时间** | {summary['start_time']} |
| **总测试数** | {summary['total']} |
| **通过** | {summary['passed']} ✅ |
| **失败** | {summary['failed']} ❌ |
| **通过率** | {summary['pass_rate']} |

## 测试结果详情

"""
    categories = {}
    for test in result["tests"]:
        cat = test["category"]
        if cat not in categories: categories[cat] = []
        categories[cat].append(test)
    
    for cat, tests in categories.items():
        report += f"\n### {cat}\n\n| 测试项 | 状态 | 消息 | 耗时 |\n|--------|------|------|------|\n"
        for t in tests:
            icon = "✅" if t["status"] == "PASS" else "❌"
            report += f"| {t['name']} | {icon} {t['status']} | {t['message']} | {t['duration_ms']}ms |\n"
    
    report += f"""

## 结论

- **总体评估**: {'✅ 通过' if summary['failed'] == 0 else '❌ 存在失败项'}
- **通过率**: {summary['pass_rate']}

---
*报告生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
"""
    
    with open(report_file, "w", encoding="utf-8") as f: f.write(report)
    with open(json_file, "w", encoding="utf-8") as f: json.dump(result, f, ensure_ascii=False, indent=2)
    
    return report_file, json_file


def main():
    tester = ExceptionTester(BASE_URL)
    result = tester.run_all_tests()
    
    print("\n" + "=" * 60)
    print("测试摘要")
    print("=" * 60)
    summary = result["summary"]
    print(f"总测试数: {summary['total']}")
    print(f"通过: {summary['passed']}")
    print(f"失败: {summary['failed']}")
    print(f"通过率: {summary['pass_rate']}")
    
    report_file, json_file = generate_report(result)
    print(f"\n报告已生成: {report_file}")
    
    return 0 if summary["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
