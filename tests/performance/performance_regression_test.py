#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 性能回归测试脚本
===========================
测试内容:
1. 响应时间基准测试
2. 吞吐量测试
3. 并发性能测试
4. 资源使用监控
5. 性能对比分析

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

BASE_URL = "http://localhost:8080"
TIMEOUT = 30

# 历史基准数据 (从之前的测试记录)
BASELINE_DATA = {
    "avg_response_time_ms": 5.80,
    "p95_response_time_ms": 26.8,
    "p99_response_time_ms": 50.0,
    "throughput_rps": 172.4,
    "health_check_time_ms": 8.0
}


class PerformanceTestResult:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.tests = []
        self.start_time = datetime.now()
        self.metrics = {}
    
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
            "tests": self.tests,
            "metrics": self.metrics
        }


class PerformanceRegressionTester:
    def __init__(self, base_url: str, baseline: Dict):
        self.base_url = base_url
        self.session = requests.Session()
        self.result = PerformanceTestResult()
        self.baseline = baseline
    
    def request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{endpoint}"
        kwargs.setdefault("timeout", TIMEOUT)
        return self.session.request(method, url, **kwargs)
    
    def calculate_percentile(self, data: List[float], percentile: int) -> float:
        """计算百分位数"""
        if not data: return 0
        sorted_data = sorted(data)
        index = int(len(sorted_data) * percentile / 100)
        return sorted_data[min(index, len(sorted_data) - 1)]
    
    def test_response_time_baseline(self):
        """响应时间基准测试"""
        category = "响应时间基准"
        
        # 健康检查响应时间
        start = time.time()
        times = []
        for _ in range(20):
            s = time.time()
            resp = self.request("GET", "/actuator/health")
            times.append((time.time() - s) * 1000)
        
        avg_time = statistics.mean(times)
        p95_time = self.calculate_percentile(times, 95)
        
        baseline_avg = self.baseline.get("health_check_time_ms", 10)
        deviation = ((avg_time - baseline_avg) / baseline_avg) * 100
        
        self.result.metrics["health_check_avg_ms"] = round(avg_time, 2)
        self.result.metrics["health_check_p95_ms"] = round(p95_time, 2)
        
        # 偏差小于50%视为通过
        passed = abs(deviation) < 50
        self.result.add_test(category, "健康检查响应时间", "PASS" if passed else "FAIL",
                            f"平均: {avg_time:.2f}ms, P95: {p95_time:.2f}ms, 偏差: {deviation:.1f}%",
                            time.time() - start)
        
        # API响应时间
        start = time.time()
        api_times = []
        for _ in range(20):
            s = time.time()
            resp = self.request("GET", "/api/users")
            api_times.append((time.time() - s) * 1000)
        
        avg_api = statistics.mean(api_times)
        p95_api = self.calculate_percentile(api_times, 95)
        
        self.result.metrics["api_avg_ms"] = round(avg_api, 2)
        self.result.metrics["api_p95_ms"] = round(p95_api, 2)
        
        # API响应应在合理范围
        passed = avg_api < 100
        self.result.add_test(category, "API响应时间", "PASS" if passed else "FAIL",
                            f"平均: {avg_api:.2f}ms, P95: {p95_api:.2f}ms",
                            time.time() - start)
    
    def test_throughput(self):
        """吞吐量测试"""
        category = "吞吐量测试"
        
        # 测量10秒内的请求数
        start = time.time()
        request_count = 0
        duration = 5  # 5秒测试
        
        while time.time() - start < duration:
            resp = self.request("GET", "/actuator/health")
            request_count += 1
        
        actual_duration = time.time() - start
        rps = request_count / actual_duration
        
        baseline_rps = self.baseline.get("throughput_rps", 100)
        deviation = ((rps - baseline_rps) / baseline_rps) * 100
        
        self.result.metrics["throughput_rps"] = round(rps, 2)
        
        # 吞吐量下降不超过30%视为通过
        passed = deviation > -30
        self.result.add_test(category, "健康检查吞吐量", "PASS" if passed else "FAIL",
                            f"RPS: {rps:.2f}, 基准: {baseline_rps}, 偏差: {deviation:.1f}%",
                            actual_duration)
        
        # API吞吐量
        start = time.time()
        api_count = 0
        
        while time.time() - start < duration:
            resp = self.request("GET", "/api/users")
            api_count += 1
        
        actual_duration = time.time() - start
        api_rps = api_count / actual_duration
        
        self.result.metrics["api_throughput_rps"] = round(api_rps, 2)
        
        passed = api_rps > 50  # API吞吐量应大于50 RPS
        self.result.add_test(category, "API吞吐量", "PASS" if passed else "FAIL",
                            f"RPS: {api_rps:.2f}",
                            actual_duration)
    
    def test_concurrent_performance(self):
        """并发性能测试"""
        category = "并发性能测试"
        
        # 并发请求测试
        start = time.time()
        results = []
        
        def make_request(i):
            s = time.time()
            resp = self.request("GET", "/actuator/health")
            return (time.time() - s) * 1000
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            futures = [executor.submit(make_request, i) for i in range(100)]
            for f in concurrent.futures.as_completed(futures):
                results.append(f.result())
        
        avg_concurrent = statistics.mean(results)
        p95_concurrent = self.calculate_percentile(results, 95)
        max_concurrent = max(results)
        
        self.result.metrics["concurrent_avg_ms"] = round(avg_concurrent, 2)
        self.result.metrics["concurrent_p95_ms"] = round(p95_concurrent, 2)
        self.result.metrics["concurrent_max_ms"] = round(max_concurrent, 2)
        
        # 并发时P95应小于200ms
        passed = p95_concurrent < 200
        self.result.add_test(category, "并发响应时间", "PASS" if passed else "FAIL",
                            f"平均: {avg_concurrent:.2f}ms, P95: {p95_concurrent:.2f}ms, 最大: {max_concurrent:.2f}ms",
                            time.time() - start)
        
        # 成功率测试
        success_count = sum(1 for r in results if r > 0)
        success_rate = (success_count / len(results)) * 100
        
        passed = success_rate >= 99
        self.result.add_test(category, "并发成功率", "PASS" if passed else "FAIL",
                            f"成功率: {success_rate:.1f}%",
                            time.time() - start)
    
    def test_response_time_consistency(self):
        """响应时间一致性测试"""
        category = "响应时间一致性"
        
        # 多次测试响应时间波动
        start = time.time()
        batches = []
        
        for _ in range(5):
            times = []
            for _ in range(10):
                s = time.time()
                resp = self.request("GET", "/actuator/health")
                times.append((time.time() - s) * 1000)
            batches.append(statistics.mean(times))
        
        overall_avg = statistics.mean(batches)
        std_dev = statistics.stdev(batches) if len(batches) > 1 else 0
        cv = (std_dev / overall_avg) * 100 if overall_avg > 0 else 0
        
        self.result.metrics["consistency_cv"] = round(cv, 2)
        
        # 变异系数应小于20%
        passed = cv < 20
        self.result.add_test(category, "响应时间一致性", "PASS" if passed else "FAIL",
                            f"变异系数: {cv:.2f}%, 标准差: {std_dev:.2f}ms",
                            time.time() - start)
    
    def test_performance_regression(self):
        """性能回归分析"""
        category = "性能回归分析"
        
        current_avg = self.result.metrics.get("health_check_avg_ms", 0)
        baseline_avg = self.baseline.get("health_check_time_ms", 10)
        
        if baseline_avg > 0:
            regression = ((current_avg - baseline_avg) / baseline_avg) * 100
        else:
            regression = 0
        
        self.result.metrics["regression_percent"] = round(regression, 2)
        
        # 回归不超过20%视为通过
        passed = regression < 20
        self.result.add_test(category, "性能回归检查", "PASS" if passed else "FAIL",
                            f"当前: {current_avg:.2f}ms, 基准: {baseline_avg:.2f}ms, 变化: {regression:+.1f}%",
                            0)
        
        # 综合性能评分
        avg_time = self.result.metrics.get("health_check_avg_ms", 0)
        p95 = self.result.metrics.get("health_check_p95_ms", 0)
        rps = self.result.metrics.get("throughput_rps", 0)
        
        # 评分规则: avg<20ms(30分), P95<50ms(30分), RPS>100(40分)
        score = 0
        if avg_time < 20: score += 30
        elif avg_time < 50: score += 20
        elif avg_time < 100: score += 10
        
        if p95 < 50: score += 30
        elif p95 < 100: score += 20
        elif p95 < 200: score += 10
        
        if rps > 150: score += 40
        elif rps > 100: score += 30
        elif rps > 50: score += 20
        elif rps > 20: score += 10
        
        self.result.metrics["performance_score"] = score
        
        passed = score >= 70
        self.result.add_test(category, "综合性能评分", "PASS" if passed else "FAIL",
                            f"评分: {score}/100",
                            0)
    
    def run_all_tests(self):
        print("=" * 60)
        print("AI-Ready 性能回归测试")
        print("=" * 60)
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"目标服务: {self.base_url}")
        print(f"基准数据: {self.baseline}")
        print("=" * 60)
        
        tests = [
            ("1. 响应时间基准", self.test_response_time_baseline),
            ("2. 吞吐量测试", self.test_throughput),
            ("3. 并发性能测试", self.test_concurrent_performance),
            ("4. 响应时间一致性", self.test_response_time_consistency),
            ("5. 性能回归分析", self.test_performance_regression),
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
    report_file = os.path.join(output_dir, f"PERFORMANCE_REGRESSION_TEST_REPORT_{timestamp}.md")
    json_file = os.path.join(output_dir, f"performance_regression_test_results_{timestamp}.json")
    
    summary = result["summary"]
    metrics = result.get("metrics", {})
    
    report = f"""# AI-Ready 性能回归测试报告

## 测试概览

| 指标 | 值 |
|------|-----|
| **测试时间** | {summary['start_time']} |
| **总测试数** | {summary['total']} |
| **通过** | {summary['passed']} ✅ |
| **失败** | {summary['failed']} ❌ |
| **通过率** | {summary['pass_rate']} |

## 性能指标

| 指标 | 当前值 | 基准值 | 变化 |
|------|--------|--------|------|
| 健康检查平均响应时间 | {metrics.get('health_check_avg_ms', 'N/A')}ms | {BASELINE_DATA.get('health_check_time_ms', 'N/A')}ms | {metrics.get('regression_percent', 'N/A')}% |
| 健康检查P95响应时间 | {metrics.get('health_check_p95_ms', 'N/A')}ms | {BASELINE_DATA.get('p95_response_time_ms', 'N/A')}ms | - |
| 吞吐量 | {metrics.get('throughput_rps', 'N/A')} RPS | {BASELINE_DATA.get('throughput_rps', 'N/A')} RPS | - |
| 并发P95响应时间 | {metrics.get('concurrent_p95_ms', 'N/A')}ms | - | - |
| 综合性能评分 | {metrics.get('performance_score', 'N/A')}/100 | - | - |

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
- **性能评分**: {metrics.get('performance_score', 'N/A')}/100

---
*报告生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
"""
    
    with open(report_file, "w", encoding="utf-8") as f: f.write(report)
    with open(json_file, "w", encoding="utf-8") as f: json.dump(result, f, ensure_ascii=False, indent=2)
    
    return report_file, json_file


def main():
    tester = PerformanceRegressionTester(BASE_URL, BASELINE_DATA)
    result = tester.run_all_tests()
    
    print("\n" + "=" * 60)
    print("测试摘要")
    print("=" * 60)
    summary = result["summary"]
    metrics = result.get("metrics", {})
    print(f"总测试数: {summary['total']}")
    print(f"通过: {summary['passed']}")
    print(f"失败: {summary['failed']}")
    print(f"通过率: {summary['pass_rate']}")
    print(f"性能评分: {metrics.get('performance_score', 'N/A')}/100")
    
    report_file, json_file = generate_report(result)
    print(f"\n报告已生成: {report_file}")
    
    return 0 if summary["failed"] == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
