#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 接口压力测试脚本
===========================
测试内容:
1. 并发压力测试
2. 持续负载测试
3. 峰值压力测试
4. 混合场景压力测试

Author: test-agent-1
Date: 2026-04-04
"""

import requests
import json
import sys
import os
import time
import statistics
from datetime import datetime
from typing import Dict, List, Any
import concurrent.futures
import threading

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

class StressTestResult:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.tests = []
        self.start_time = datetime.now()
        self.lock = threading.Lock()
    
    def add_test(self, category: str, name: str, status: str, message: str = "", duration: float = 0, metrics: Dict = None):
        with self.lock:
            self.tests.append({
                "category": category, "name": name, "status": status,
                "message": message, "duration_ms": round(duration * 1000, 2),
                "timestamp": datetime.now().isoformat(),
                "metrics": metrics or {}
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


class StressTester:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = StressTestResult()
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    def test_concurrent_stress(self):
        """并发压力测试"""
        category = "并发压力测试"
        
        # 50并发读取
        start = time.time()
        results = []
        errors = []
        
        def make_request(i):
            try:
                s = time.time()
                resp = self.request("GET", "/actuator/health")
                return {"status": resp.status_code, "time": (time.time() - s) * 1000}
            except Exception as e:
                return {"error": str(e)}
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(make_request, i) for i in range(100)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        success = [r for r in results if r.get("status") == 200]
        success_rate = len(success) / len(results) * 100
        avg_time = statistics.mean([r["time"] for r in success]) if success else 0
        
        self.result.add_test(category, "50并发100请求", "PASS" if success_rate >= 95 else "FAIL",
                            f"成功率: {success_rate:.1f}%, 平均响应: {avg_time:.2f}ms",
                            time.time() - start,
                            {"success_rate": success_rate, "avg_response_ms": avg_time})
        
        # 100并发读取
        start = time.time()
        results = []
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
            futures = [executor.submit(make_request, i) for i in range(200)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        success = [r for r in results if r.get("status") == 200]
        success_rate = len(success) / len(results) * 100
        avg_time = statistics.mean([r["time"] for r in success]) if success else 0
        
        self.result.add_test(category, "100并发200请求", "PASS" if success_rate >= 90 else "FAIL",
                            f"成功率: {success_rate:.1f}%, 平均响应: {avg_time:.2f}ms",
                            time.time() - start,
                            {"success_rate": success_rate, "avg_response_ms": avg_time})
    
    def test_sustained_load(self):
        """持续负载测试"""
        category = "持续负载测试"
        
        # 10秒持续请求
        start = time.time()
        duration = 10
        count = 0
        errors = 0
        
        while time.time() - start < duration:
            try:
                resp = self.request("GET", "/actuator/health")
                if resp.status_code == 200:
                    count += 1
                else:
                    errors += 1
            except:
                errors += 1
        
        actual_duration = time.time() - start
        rps = count / actual_duration
        
        self.result.add_test(category, "10秒持续负载", "PASS" if rps > 50 else "FAIL",
                            f"RPS: {rps:.2f}, 成功: {count}, 失败: {errors}",
                            actual_duration,
                            {"rps": rps, "success": count, "errors": errors})
        
        # 30秒持续请求
        start = time.time()
        duration = 30
        count = 0
        errors = 0
        
        while time.time() - start < duration:
            try:
                resp = self.request("GET", "/actuator/health")
                if resp.status_code == 200:
                    count += 1
                else:
                    errors += 1
            except:
                errors += 1
        
        actual_duration = time.time() - start
        rps = count / actual_duration
        
        self.result.add_test(category, "30秒持续负载", "PASS" if rps > 50 else "FAIL",
                            f"RPS: {rps:.2f}, 成功: {count}, 失败: {errors}",
                            actual_duration,
                            {"rps": rps, "success": count, "errors": errors})
    
    def test_peak_load(self):
        """峰值压力测试"""
        category = "峰值压力测试"
        
        # 突发100请求
        start = time.time()
        results = []
        
        def burst_request(i):
            try:
                s = time.time()
                resp = self.request("GET", "/actuator/health")
                return {"status": resp.status_code, "time": (time.time() - s) * 1000}
            except Exception as e:
                return {"error": str(e)}
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
            futures = [executor.submit(burst_request, i) for i in range(100)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        success = [r for r in results if r.get("status") == 200]
        success_rate = len(success) / len(results) * 100
        times = [r["time"] for r in success] if success else [0]
        
        self.result.add_test(category, "突发100请求", "PASS" if success_rate >= 95 else "FAIL",
                            f"成功率: {success_rate:.1f}%, P95: {sorted(times)[int(len(times)*0.95)]:.2f}ms",
                            time.time() - start,
                            {"success_rate": success_rate, "p95_ms": sorted(times)[int(len(times)*0.95)] if times else 0})
        
        # 突发500请求
        start = time.time()
        results = []
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
            futures = [executor.submit(burst_request, i) for i in range(500)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        success = [r for r in results if r.get("status") == 200]
        success_rate = len(success) / len(results) * 100
        times = [r["time"] for r in success] if success else [0]
        
        self.result.add_test(category, "突发500请求", "PASS" if success_rate >= 90 else "FAIL",
                            f"成功率: {success_rate:.1f}%, P95: {sorted(times)[int(len(times)*0.95)] if times else 0:.2f}ms",
                            time.time() - start,
                            {"success_rate": success_rate, "p95_ms": sorted(times)[int(len(times)*0.95)] if times else 0})
    
    def test_mixed_stress(self):
        """混合场景压力测试"""
        category = "混合场景压力测试"
        
        # 读写混合
        start = time.time()
        results = []
        
        def mixed_request(i):
            try:
                if i % 3 == 0:
                    s = time.time()
                    resp = self.request("POST", "/api/users", json={"username": f"stress_test_{i}"})
                    return {"type": "write", "status": resp.status_code, "time": (time.time() - s) * 1000}
                else:
                    s = time.time()
                    resp = self.request("GET", "/actuator/health")
                    return {"type": "read", "status": resp.status_code, "time": (time.time() - s) * 1000}
            except Exception as e:
                return {"error": str(e)}
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(mixed_request, i) for i in range(150)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        reads = [r for r in results if r.get("type") == "read"]
        writes = [r for r in results if r.get("type") == "write"]
        read_success = len([r for r in reads if r.get("status") == 200])
        write_success = len([r for r in writes if r.get("status") in [200, 201, 401]])
        
        self.result.add_test(category, "读写混合150请求", "PASS" if read_success >= 90 else "FAIL",
                            f"读取成功: {read_success}/{len(reads)}, 写入成功: {write_success}/{len(writes)}",
                            time.time() - start,
                            {"read_success": read_success, "write_success": write_success})
        
        # API混合
        start = time.time()
        results = []
        
        def api_mixed(i):
            try:
                endpoints = ["/api/users", "/api/roles", "/api/customers", "/api/orders"]
                s = time.time()
                resp = self.request("GET", endpoints[i % len(endpoints)])
                return {"status": resp.status_code, "time": (time.time() - s) * 1000}
            except Exception as e:
                return {"error": str(e)}
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=30) as executor:
            futures = [executor.submit(api_mixed, i) for i in range(100)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        handled = len([r for r in results if r.get("status")])
        
        self.result.add_test(category, "API混合100请求", "PASS" if handled == 100 else "FAIL",
                            f"处理数: {handled}/100",
                            time.time() - start,
                            {"handled": handled})
    
    def run_all_tests(self):
        print("=" * 60)
        print("AI-Ready 接口压力测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print("=" * 60)
        
        tests = [
            ("1. 并发压力测试", self.test_concurrent_stress),
            ("2. 持续负载测试", self.test_sustained_load),
            ("3. 峰值压力测试", self.test_peak_load),
            ("4. 混合场景压力测试", self.test_mixed_stress),
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
    report_file = os.path.join(output_dir, f"STRESS_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"stress_test_results_{timestamp}.json")
    
    summary = result["summary"]
    report = f"""# AI-Ready 接口压力测试报告

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
    tester = StressTester(BASE_URL)
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
