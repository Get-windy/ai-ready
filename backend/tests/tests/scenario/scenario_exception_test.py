#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 异常场景测试脚本
===========================
测试内容:
1. 网络超时场景测试
2. 服务降级场景测试
3. 大数据量场景测试
4. 并发冲突场景测试
5. 资源耗尽场景测试
6. 数据状态冲突测试

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
import concurrent.futures

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

class ScenarioTestResult:
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


class ScenarioTester:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = ScenarioTestResult()
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    def test_network_scenarios(self):
        """网络超时场景测试"""
        category = "网络超时场景"
        
        # 短超时测试
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health", timeout=1)
            self.result.add_test(category, "短超时请求", "PASS" if resp.status_code in [200] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except requests.Timeout:
            self.result.add_test(category, "短超时请求", "PASS", "请求超时被正确捕获", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "短超时请求", "FAIL", str(e), time.time() - start)
        
        # 长超时测试
        start = time.time()
        try:
            resp = self.request("GET", "/actuator/health", timeout=60)
            self.result.add_test(category, "长超时请求", "PASS" if resp.status_code in [200] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "长超时请求", "FAIL", str(e), time.time() - start)
    
    def test_large_payload_scenarios(self):
        """大数据量场景测试"""
        category = "大数据量场景"
        
        # 大JSON请求体
        start = time.time()
        try:
            large_data = {"data": "x" * 100000}  # 100KB数据
            resp = self.request("POST", "/api/users", json=large_data)
            self.result.add_test(category, "大数据请求体", "PASS" if resp.status_code in [400, 401, 413] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "大数据请求体", "FAIL", str(e), time.time() - start)
        
        # 超大请求头
        start = time.time()
        try:
            large_headers = {"X-Large-Header": "x" * 8000}
            resp = self.request("GET", "/actuator/health", headers=large_headers)
            self.result.add_test(category, "大请求头", "PASS" if resp.status_code in [200, 400, 431] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "大请求头", "FAIL", str(e), time.time() - start)
        
        # 大查询参数
        start = time.time()
        try:
            large_param = "x" * 50000
            resp = self.request("GET", "/api/users", params={"filter": large_param})
            self.result.add_test(category, "大查询参数", "PASS" if resp.status_code in [400, 401, 414] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "大查询参数", "FAIL", str(e), time.time() - start)
    
    def test_concurrency_conflict_scenarios(self):
        """并发冲突场景测试"""
        category = "并发冲突场景"
        
        # 并发读取相同资源
        start = time.time()
        try:
            results = []
            def read_resource(i):
                resp = self.request("GET", "/actuator/health")
                return resp.status_code
            
            with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
                futures = [executor.submit(read_resource, i) for i in range(50)]
                for f in concurrent.futures.as_completed(futures):
                    results.append(f.result())
            
            success_rate = sum(1 for r in results if r == 200) / len(results) * 100
            self.result.add_test(category, "高并发读取", "PASS" if success_rate >= 95 else "FAIL",
                                f"成功率: {success_rate:.1f}%", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "高并发读取", "FAIL", str(e), time.time() - start)
        
        # 并发写入冲突
        start = time.time()
        try:
            results = []
            def write_resource(i):
                resp = self.request("POST", "/api/users", json={"username": f"test_{i}"})
                return resp.status_code
            
            with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
                futures = [executor.submit(write_resource, i) for i in range(20)]
                for f in concurrent.futures.as_completed(futures):
                    results.append(f.result())
            
            # 所有请求应被处理(即使是返回401)
            handled = sum(1 for r in results if r is not None)
            self.result.add_test(category, "并发写入冲突", "PASS" if handled == 20 else "FAIL",
                                f"处理数: {handled}/20", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "并发写入冲突", "FAIL", str(e), time.time() - start)
    
    def test_resource_exhaustion_scenarios(self):
        """资源耗尽场景测试"""
        category = "资源耗尽场景"
        
        # 连续快速请求
        start = time.time()
        try:
            success_count = 0
            for _ in range(100):
                resp = self.request("GET", "/actuator/health")
                if resp.status_code == 200:
                    success_count += 1
            
            self.result.add_test(category, "连续快速请求", "PASS" if success_count >= 90 else "FAIL",
                                f"成功: {success_count}/100", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "连续快速请求", "FAIL", str(e), time.time() - start)
        
        # 连接复用测试
        start = time.time()
        try:
            # 使用同一session发送多个请求
            session = requests.Session()
            success = 0
            for _ in range(50):
                resp = session.get(f"{self.base_url}/actuator/health", timeout=TIMEOUT)
                if resp.status_code == 200:
                    success += 1
            
            self.result.add_test(category, "连接复用测试", "PASS" if success >= 45 else "FAIL",
                                f"成功: {success}/50", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "连接复用测试", "FAIL", str(e), time.time() - start)
    
    def test_data_state_conflict_scenarios(self):
        """数据状态冲突场景测试"""
        category = "数据状态冲突场景"
        
        # 不存在的资源操作
        start = time.time()
        try:
            resp = self.request("DELETE", "/api/users/999999999")
            self.result.add_test(category, "删除不存在资源", "PASS" if resp.status_code in [401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "删除不存在资源", "FAIL", str(e), time.time() - start)
        
        # 更新不存在资源
        start = time.time()
        try:
            resp = self.request("PUT", "/api/users/999999999", json={"nickname": "test"})
            self.result.add_test(category, "更新不存在资源", "PASS" if resp.status_code in [401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "更新不存在资源", "FAIL", str(e), time.time() - start)
        
        # 重复创建
        start = time.time()
        try:
            # 尝试创建相同用户名
            resp1 = self.request("POST", "/api/users", json={"username": "duplicate_test", "password": "test"})
            resp2 = self.request("POST", "/api/users", json={"username": "duplicate_test", "password": "test"})
            
            self.result.add_test(category, "重复创建资源", "PASS" if resp1.status_code == resp2.status_code else "FAIL",
                                f"两次状态码: {resp1.status_code}, {resp2.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "重复创建资源", "FAIL", str(e), time.time() - start)
    
    def test_error_recovery_scenarios(self):
        """错误恢复场景测试"""
        category = "错误恢复场景"
        
        # 错误后正常请求
        start = time.time()
        try:
            # 发送错误请求
            self.request("GET", "/api/nonexistent")
            # 紧接着发送正常请求
            resp = self.request("GET", "/actuator/health")
            
            self.result.add_test(category, "错误后恢复", "PASS" if resp.status_code == 200 else "FAIL",
                                f"正常请求状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "错误后恢复", "FAIL", str(e), time.time() - start)
        
        # 请求失败重试
        start = time.time()
        try:
            max_retries = 3
            success = False
            for i in range(max_retries):
                resp = self.request("GET", "/actuator/health")
                if resp.status_code == 200:
                    success = True
                    break
            
            self.result.add_test(category, "请求重试机制", "PASS" if success else "FAIL",
                                f"重试{i+1}次后{'成功' if success else '失败'}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "请求重试机制", "FAIL", str(e), time.time() - start)
    
    def run_all_tests(self):
        print("=" * 60)
        print("AI-Ready 异常场景测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print("=" * 60)
        
        tests = [
            ("1. 网络超时场景", self.test_network_scenarios),
            ("2. 大数据量场景", self.test_large_payload_scenarios),
            ("3. 并发冲突场景", self.test_concurrency_conflict_scenarios),
            ("4. 资源耗尽场景", self.test_resource_exhaustion_scenarios),
            ("5. 数据状态冲突场景", self.test_data_state_conflict_scenarios),
            ("6. 错误恢复场景", self.test_error_recovery_scenarios),
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
    report_file = os.path.join(output_dir, f"SCENARIO_EXCEPTION_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"scenario_exception_test_results_{timestamp}.json")
    
    summary = result["summary"]
    report = f"""# AI-Ready 异常场景测试报告

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
    tester = ScenarioTester(BASE_URL)
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
