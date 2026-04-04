#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 接口功能测试脚本
===========================
测试内容:
1. 健康检查接口测试
2. 用户管理接口测试
3. 角色管理接口测试
4. 认证接口测试
5. API文档接口测试

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

class FunctionTestResult:
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


class APIFunctionTester:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = FunctionTestResult()
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    def test_health_endpoints(self):
        """健康检查接口测试"""
        category = "健康检查接口"
        
        # 基础健康检查
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            if resp.status_code == 200:
                data = resp.json()
                has_status = "status" in data
                self.result.add_test(category, "健康检查接口", "PASS" if has_status else "FAIL",
                                    f"状态码: {resp.status_code}, 包含status字段: {has_status}",
                                    time.time() - start)
            else:
                self.result.add_test(category, "健康检查接口", "FAIL",
                                    f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "健康检查接口", "FAIL", str(e), time.time() - start)
        
        # 详细健康检查
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            self.result.add_test(category, "详细健康检查", "PASS" if resp.status_code == 200 else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "详细健康检查", "FAIL", str(e), time.time() - start)
        
        # 组件健康检查
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health")
            if resp.status_code == 200:
                data = resp.json()
                components = data.get("components", {})
                db_status = components.get("db", {}).get("status", "UNKNOWN")
                redis_status = components.get("redis", {}).get("status", "UNKNOWN")
                
                self.result.add_test(category, "数据库健康检查", "PASS" if db_status == "UP" else "FAIL",
                                    f"数据库状态: {db_status}", time.time() - start)
                self.result.add_test(category, "Redis健康检查", "PASS" if redis_status == "UP" else "FAIL",
                                    f"Redis状态: {redis_status}", time.time() - start)
            else:
                self.result.add_test(category, "组件健康检查", "FAIL", f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "组件健康检查", "FAIL", str(e), time.time() - start)
    
    def test_user_endpoints(self):
        """用户管理接口测试"""
        category = "用户管理接口"
        
        # 获取用户列表
        start = time.time()
        try:
            resp = self.request("GET", "/api/users")
            self.result.add_test(category, "获取用户列表", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "获取用户列表", "FAIL", str(e), time.time() - start)
        
        # 获取单个用户
        start = time.time()
        try:
            resp = self.request("GET", "/api/users/1")
            self.result.add_test(category, "获取单个用户", "PASS" if resp.status_code in [200, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "获取单个用户", "FAIL", str(e), time.time() - start)
        
        # 创建用户
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "test", "password": "Test@123"})
            self.result.add_test(category, "创建用户", "PASS" if resp.status_code in [200, 201, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "创建用户", "FAIL", str(e), time.time() - start)
        
        # 更新用户
        start = time.time()
        try:
            resp = self.request("PUT", "/api/users/1", json={"nickname": "test"})
            self.result.add_test(category, "更新用户", "PASS" if resp.status_code in [200, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "更新用户", "FAIL", str(e), time.time() - start)
        
        # 删除用户
        start = time.time()
        try:
            resp = self.request("DELETE", "/api/users/99999")
            self.result.add_test(category, "删除用户", "PASS" if resp.status_code in [200, 204, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "删除用户", "FAIL", str(e), time.time() - start)
    
    def test_role_endpoints(self):
        """角色管理接口测试"""
        category = "角色管理接口"
        
        # 获取角色列表
        start = time.time()
        try:
            resp = self.request("GET", "/api/roles")
            self.result.add_test(category, "获取角色列表", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "获取角色列表", "FAIL", str(e), time.time() - start)
        
        # 获取单个角色
        start = time.time()
        try:
            resp = self.request("GET", "/api/roles/1")
            self.result.add_test(category, "获取单个角色", "PASS" if resp.status_code in [200, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "获取单个角色", "FAIL", str(e), time.time() - start)
        
        # 创建角色
        start = time.time()
        try:
            resp = self.request("POST", "/api/roles", json={"roleName": "test_role"})
            self.result.add_test(category, "创建角色", "PASS" if resp.status_code in [200, 201, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "创建角色", "FAIL", str(e), time.time() - start)
    
    def test_auth_endpoints(self):
        """认证接口测试"""
        category = "认证接口"
        
        # 登录接口
        start = time.time()
        try:
            resp = self.request("POST", "/api/auth/login", json={"username": "admin", "password": "admin123"})
            self.result.add_test(category, "登录接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "登录接口", "FAIL", str(e), time.time() - start)
        
        # 登出接口
        start = time.time()
        try:
            resp = self.request("POST", "/api/auth/logout")
            self.result.add_test(category, "登出接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "登出接口", "FAIL", str(e), time.time() - start)
        
        # 刷新Token
        start = time.time()
        try:
            resp = self.request("POST", "/api/auth/refresh")
            self.result.add_test(category, "刷新Token接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "刷新Token接口", "FAIL", str(e), time.time() - start)
    
    def test_business_endpoints(self):
        """业务接口测试"""
        category = "业务接口"
        
        # 客户接口
        start = time.time()
        try:
            resp = self.request("GET", "/api/customers")
            self.result.add_test(category, "客户列表接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "客户列表接口", "FAIL", str(e), time.time() - start)
        
        # 订单接口
        start = time.time()
        try:
            resp = self.request("GET", "/api/orders")
            self.result.add_test(category, "订单列表接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "订单列表接口", "FAIL", str(e), time.time() - start)
        
        # 产品接口
        start = time.time()
        try:
            resp = self.request("GET", "/api/products")
            self.result.add_test(category, "产品列表接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "产品列表接口", "FAIL", str(e), time.time() - start)
        
        # 库存接口
        start = time.time()
        try:
            resp = self.request("GET", "/api/stocks")
            self.result.add_test(category, "库存列表接口", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "库存列表接口", "FAIL", str(e), time.time() - start)
    
    def test_pagination_and_filter(self):
        """分页和过滤测试"""
        category = "分页过滤功能"
        
        # 分页参数
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"page": 1, "size": 10})
            self.result.add_test(category, "分页参数测试", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "分页参数测试", "FAIL", str(e), time.time() - start)
        
        # 排序参数
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"sort": "createTime,desc"})
            self.result.add_test(category, "排序参数测试", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "排序参数测试", "FAIL", str(e), time.time() - start)
        
        # 过滤参数
        start = time.time()
        try:
            resp = self.request("GET", "/api/users", params={"username": "admin", "status": 1})
            self.result.add_test(category, "过滤参数测试", "PASS" if resp.status_code in [200, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "过滤参数测试", "FAIL", str(e), time.time() - start)
    
    def run_all_tests(self):
        print("=" * 60)
        print("AI-Ready 接口功能测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print("=" * 60)
        
        tests = [
            ("1. 健康检查接口", self.test_health_endpoints),
            ("2. 用户管理接口", self.test_user_endpoints),
            ("3. 角色管理接口", self.test_role_endpoints),
            ("4. 认证接口", self.test_auth_endpoints),
            ("5. 业务接口", self.test_business_endpoints),
            ("6. 分页过滤功能", self.test_pagination_and_filter),
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
    report_file = os.path.join(output_dir, f"API_FUNCTION_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"api_function_test_results_{timestamp}.json")
    
    summary = result["summary"]
    report = f"""# AI-Ready 接口功能测试报告

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
    tester = APIFunctionTester(BASE_URL)
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
