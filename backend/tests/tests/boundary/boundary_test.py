#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 边界值测试脚本
===========================
测试内容:
1. 数值边界测试
2. 字符串边界测试
3. 时间边界测试
4. 分页边界测试
5. ID边界测试
6. 状态值边界测试

Author: test-agent-1
Date: 2026-04-04
"""

import requests
import json
import sys
import os
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

class BoundaryTestResult:
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


class BoundaryTester:
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = BoundaryTestResult()
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    def test_numeric_boundaries(self):
        """数值边界测试"""
        category = "数值边界"
        
        # 页码边界
        test_cases = [
            ("page=0", {"page": 0}),
            ("page=1", {"page": 1}),
            ("page=-1", {"page": -1}),
            ("page=999999", {"page": 999999}),
        ]
        
        for name, params in test_cases:
            start = time.time()
            try:
                resp = self.request("GET", "/api/users", params=params)
                self.result.add_test(category, f"分页参数 {name}", 
                                    "PASS" if resp.status_code in [200, 400, 401] else "FAIL",
                                    f"状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"分页参数 {name}", "FAIL", str(e), time.time() - start)
        
        # 大小边界
        size_cases = [
            ("size=0", {"size": 0}),
            ("size=1", {"size": 1}),
            ("size=100", {"size": 100}),
            ("size=1000", {"size": 1000}),
            ("size=-1", {"size": -1}),
        ]
        
        for name, params in size_cases:
            start = time.time()
            try:
                resp = self.request("GET", "/api/users", params=params)
                self.result.add_test(category, f"大小参数 {name}",
                                    "PASS" if resp.status_code in [200, 400, 401] else "FAIL",
                                    f"状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"大小参数 {name}", "FAIL", str(e), time.time() - start)
    
    def test_string_boundaries(self):
        """字符串边界测试"""
        category = "字符串边界"
        
        # 空字符串
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "", "password": "test"})
            self.result.add_test(category, "空字符串",
                                "PASS" if resp.status_code in [400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "空字符串", "FAIL", str(e), time.time() - start)
        
        # 单字符
        start = time.time()
        try:
            resp = self.request("POST", "/api/users", json={"username": "a", "password": "test"})
            self.result.add_test(category, "单字符",
                                "PASS" if resp.status_code in [200, 201, 400, 401] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "单字符", "FAIL", str(e), time.time() - start)
        
        # 最大长度
        start = time.time()
        try:
            long_str = "a" * 1000
            resp = self.request("POST", "/api/users", json={"username": long_str, "password": "test"})
            self.result.add_test(category, "1000字符",
                                "PASS" if resp.status_code in [200, 201, 400, 401, 413] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "1000字符", "FAIL", str(e), time.time() - start)
        
        # 超长字符串
        start = time.time()
        try:
            very_long = "a" * 10000
            resp = self.request("POST", "/api/users", json={"username": very_long, "password": "test"})
            self.result.add_test(category, "10000字符",
                                "PASS" if resp.status_code in [400, 401, 413] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "10000字符", "FAIL", str(e), time.time() - start)
        
        # 特殊字符
        special_chars = ["<", ">", "&", "'", '"', "\n", "\t", "\x00"]
        for i, char in enumerate(special_chars):
            start = time.time()
            try:
                resp = self.request("GET", "/api/users", params={"filter": char})
                self.result.add_test(category, f"特殊字符{i+1}",
                                    "PASS" if resp.status_code not in [500] else "FAIL",
                                    f"字符: {repr(char)}, 状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"特殊字符{i+1}", "FAIL", str(e), time.time() - start)
    
    def test_id_boundaries(self):
        """ID边界测试"""
        category = "ID边界"
        
        id_cases = [
            ("ID=0", 0),
            ("ID=1", 1),
            ("ID=-1", -1),
            ("ID=999999999", 999999999),
            ("ID=Long.MAX", 9223372036854775807),
        ]
        
        for name, id_val in id_cases:
            start = time.time()
            try:
                resp = self.request("GET", f"/api/users/{id_val}")
                self.result.add_test(category, name,
                                    "PASS" if resp.status_code in [200, 400, 401, 404] else "FAIL",
                                    f"状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, name, "FAIL", str(e), time.time() - start)
        
        # 非数字ID
        non_numeric_ids = ["abc", "null", "undefined", "1.5", "1e10", "1;2"]
        for id_val in non_numeric_ids:
            start = time.time()
            try:
                resp = self.request("GET", f"/api/users/{id_val}")
                self.result.add_test(category, f"非数字ID {id_val}",
                                    "PASS" if resp.status_code not in [500] else "FAIL",
                                    f"状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"非数字ID {id_val}", "FAIL", str(e), time.time() - start)
    
    def test_status_boundaries(self):
        """状态值边界测试"""
        category = "状态值边界"
        
        status_cases = [0, 1, 2, -1, 999, 255]
        
        for status in status_cases:
            start = time.time()
            try:
                resp = self.request("GET", "/api/users", params={"status": status})
                self.result.add_test(category, f"状态值 {status}",
                                    "PASS" if resp.status_code in [200, 400, 401] else "FAIL",
                                    f"状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, f"状态值 {status}", "FAIL", str(e), time.time() - start)
    
    def test_datetime_boundaries(self):
        """时间边界测试"""
        category = "时间边界"
        
        now = datetime.now()
        date_cases = [
            ("当前时间", now.strftime("%Y-%m-%d")),
            ("过去日期", "2020-01-01"),
            ("未来日期", "2099-12-31"),
            ("边界日期", "1970-01-01"),
            ("无效日期", "invalid-date"),
            ("空日期", ""),
        ]
        
        for name, date_val in date_cases:
            start = time.time()
            try:
                resp = self.request("GET", "/api/users", params={"date": date_val})
                self.result.add_test(category, name,
                                    "PASS" if resp.status_code in [200, 400, 401] else "FAIL",
                                    f"日期: {date_val}, 状态码: {resp.status_code}", time.time() - start)
            except Exception as e:
                self.result.add_test(category, name, "FAIL", str(e), time.time() - start)
    
    def test_array_boundaries(self):
        """数组边界测试"""
        category = "数组边界"
        
        # 空数组
        start = time.time()
        try:
            resp = self.request("POST", "/api/users/batch", json={"ids": []})
            self.result.add_test(category, "空数组",
                                "PASS" if resp.status_code in [200, 201, 400, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "空数组", "FAIL", str(e), time.time() - start)
        
        # 单元素数组
        start = time.time()
        try:
            resp = self.request("POST", "/api/users/batch", json={"ids": [1]})
            self.result.add_test(category, "单元素数组",
                                "PASS" if resp.status_code in [200, 201, 400, 401, 404] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "单元素数组", "FAIL", str(e), time.time() - start)
        
        # 大数组
        start = time.time()
        try:
            resp = self.request("POST", "/api/users/batch", json={"ids": list(range(1000))})
            self.result.add_test(category, "1000元素数组",
                                "PASS" if resp.status_code in [200, 201, 400, 401, 413] else "FAIL",
                                f"状态码: {resp.status_code}", time.time() - start)
        except Exception as e:
            self.result.add_test(category, "1000元素数组", "FAIL", str(e), time.time() - start)
    
    def run_all_tests(self):
        print("=" * 60)
        print("AI-Ready 边界值测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print("=" * 60)
        
        tests = [
            ("1. 数值边界", self.test_numeric_boundaries),
            ("2. 字符串边界", self.test_string_boundaries),
            ("3. ID边界", self.test_id_boundaries),
            ("4. 状态值边界", self.test_status_boundaries),
            ("5. 时间边界", self.test_datetime_boundaries),
            ("6. 数组边界", self.test_array_boundaries),
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
    report_file = os.path.join(output_dir, f"BOUNDARY_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"boundary_test_results_{timestamp}.json")
    
    summary = result["summary"]
    report = f"""# AI-Ready 边界值测试报告

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
    tester = BoundaryTester(BASE_URL)
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
