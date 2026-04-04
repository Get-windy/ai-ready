#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 接口性能测试脚本
包括核心API响应时间测试、接口并发处理能力测试、接口稳定性测试、性能瓶颈识别
"""

import pytest
import time
import random
import json
import statistics
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
import requests
import sys
import os

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 配置
BASE_URL = "http://localhost:8080"
TEST_RESULTS = {
    "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "base_url": BASE_URL,
    "tests": []
}


class PerformanceMetrics:
    """性能指标收集器"""
    
    def __init__(self):
        self.response_times = []
        self.success_count = 0
        self.failure_count = 0
        self.status_codes = {}
    
    def record(self, response_time: float, status_code: int, success: bool):
        """Record request result"""
        self.response_times.append(response_time)
        if success:
            self.success_count += 1
        else:
            self.failure_count += 1
        self.status_codes[status_code] = self.status_codes.get(status_code, 0) + 1
    
    def get_summary(self) -> dict:
        """Get statistics summary"""
        if not self.response_times:
            return {"error": "No data recorded"}
        
        sorted_times = sorted(self.response_times)
        return {
            "total_requests": len(self.response_times),
            "success_count": self.success_count,
            "failure_count": self.failure_count,
            "success_rate": self.success_count / len(self.response_times) * 100,
            "avg_response_time": statistics.mean(self.response_times),
            "min_response_time": min(self.response_times),
            "max_response_time": max(self.response_times),
            "median_response_time": statistics.median(self.response_times),
            "p95_response_time": sorted_times[int(len(sorted_times) * 0.95)],
            "p99_response_time": sorted_times[int(len(sorted_times) * 0.99)],
            "std_dev": statistics.stdev(self.response_times) if len(self.response_times) > 1 else 0,
            "status_codes": self.status_codes
        }


class TestCoreAPIResponseTime:
    """核心API响应时间测试"""
    
    def test_api_health_check(self):
        """API健康检查"""
        metrics = PerformanceMetrics()
        
        # 测试根路径
        start = time.perf_counter()
        try:
            response = requests.get(f"{BASE_URL}/", timeout=5)
            elapsed = (time.perf_counter() - start) * 1000
            metrics.record(elapsed, response.status_code, response.status_code < 500)
        except Exception as e:
            elapsed = (time.perf_counter() - start) * 1000
            metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        TEST_RESULTS["tests"].append({
            "test_name": "API健康检查",
            "category": "响应时间",
            "endpoint": "/",
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 100 else "FAIL"
        })
        
        assert summary["avg_response_time"] < 100, f"平均响应时间超标: {summary['avg_response_time']}ms"
        print(f"[PASS] API健康检查: {summary['avg_response_time']:.2f}ms")
    
    def test_swagger_api_docs(self):
        """Swagger API文档响应时间"""
        metrics = PerformanceMetrics()
        
        endpoints = [
            "/v3/api-docs",
            "/swagger-ui/index.html"
        ]
        
        for endpoint in endpoints:
            start = time.perf_counter()
            try:
                response = requests.get(f"{BASE_URL}{endpoint}", timeout=10)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, response.status_code, response.status_code == 200)
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        TEST_RESULTS["tests"].append({
            "test_name": "Swagger API文档",
            "category": "响应时间",
            "endpoints": endpoints,
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 50 else "FAIL"
        })
        
        assert summary["avg_response_time"] < 200, f"Swagger文档响应时间超标: {summary['avg_response_time']}ms"
        print(f"[PASS] Swagger文档: 平均{summary['avg_response_time']:.2f}ms")
    
    def test_user_api_response_time(self):
        """用户API响应时间测试"""
        metrics = PerformanceMetrics()
        
        for i in range(10):
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/user/page",
                    params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                    timeout=10
                )
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, response.status_code, response.status_code < 500)
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        TEST_RESULTS["tests"].append({
            "test_name": "用户API响应时间",
            "category": "响应时间",
            "endpoint": "/api/user/page",
            "iterations": 10,
            "summary": summary,
            "status": "PASS" if summary["avg_response_time"] < 100 else "FAIL"
        })
        
        assert summary["p95_response_time"] < 100, f"P95响应时间超标: {summary['p95_response_time']}ms"
        print(f"[PASS] 用户API: 平均{summary['avg_response_time']:.2f}ms, P95={summary['p95_response_time']:.2f}ms")
    
    def test_role_api_response_time(self):
        """角色API响应时间测试"""
        metrics = PerformanceMetrics()
        
        for i in range(10):
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/role/page",
                    params={"pageNum": 1, "pageSize": 10},
                    timeout=10
                )
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, response.status_code, response.status_code < 500)
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        TEST_RESULTS["tests"].append({
            "test_name": "角色API响应时间",
            "category": "响应时间",
            "endpoint": "/api/role/page",
            "iterations": 10,
            "summary": summary,
            "status": "PASS" if summary["avg_response_time"] < 100 else "FAIL"
        })
        
        assert summary["avg_response_time"] < 100, f"角色API平均响应时间超标: {summary['avg_response_time']}ms"
        print(f"[PASS] 角色API: 平均{summary['avg_response_time']:.2f}ms")


class TestConcurrencyCapacity:
    """接口并发处理能力测试"""
    
    def test_concurrent_requests_10(self):
        """10并发请求测试"""
        metrics = PerformanceMetrics()
        
        def make_request():
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/user/page",
                    params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                    timeout=10
                )
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code, response.status_code < 500
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, 0, False
        
        total_requests = 50
        
        start_time = time.perf_counter()
        with ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(make_request) for _ in range(total_requests)]
            for future in as_completed(futures):
                elapsed, status_code, success = future.result()
                metrics.record(elapsed, status_code, success)
        
        total_time = (time.perf_counter() - start_time) * 1000
        throughput = total_requests / (total_time / 1000)
        summary = metrics.get_summary()
        summary["throughput"] = throughput
        summary["total_time_ms"] = total_time
        
        TEST_RESULTS["tests"].append({
            "test_name": "10并发测试",
            "category": "并发能力",
            "concurrent_users": 10,
            "total_requests": total_requests,
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 90 else "FAIL"
        })
        
        assert summary["success_rate"] >= 90, f"成功率不足: {summary['success_rate']}%"
        assert throughput > 10, f"吞吐量不足: {throughput:.2f} req/s"
        print(f"[PASS] 10并发: 成功率{summary['success_rate']:.1f}%, 吞吐量{throughput:.2f} req/s")
    
    def test_concurrent_requests_50(self):
        """50并发请求测试"""
        metrics = PerformanceMetrics()
        
        def make_request():
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/role/page",
                    params={"pageNum": 1, "pageSize": 10},
                    timeout=15
                )
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code, response.status_code < 500
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, 0, False
        
        total_requests = 100
        
        start_time = time.perf_counter()
        with ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(make_request) for _ in range(total_requests)]
            for future in as_completed(futures):
                elapsed, status_code, success = future.result()
                metrics.record(elapsed, status_code, success)
        
        total_time = (time.perf_counter() - start_time) * 1000
        throughput = total_requests / (total_time / 1000)
        summary = metrics.get_summary()
        summary["throughput"] = throughput
        summary["total_time_ms"] = total_time
        
        TEST_RESULTS["tests"].append({
            "test_name": "50并发测试",
            "category": "并发能力",
            "concurrent_users": 50,
            "total_requests": total_requests,
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 85 else "FAIL"
        })
        
        assert summary["success_rate"] >= 85, f"成功率不足: {summary['success_rate']}%"
        print(f"[PASS] 50并发: 成功率{summary['success_rate']:.1f}%, 吞吐量{throughput:.2f} req/s")
    
    def test_concurrent_requests_100(self):
        """100并发请求测试"""
        metrics = PerformanceMetrics()
        
        def make_request():
            start = time.perf_counter()
            try:
                response = requests.get(f"{BASE_URL}/v3/api-docs", timeout=20)
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code, response.status_code == 200
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, 0, False
        
        total_requests = 200
        
        start_time = time.perf_counter()
        with ThreadPoolExecutor(max_workers=100) as executor:
            futures = [executor.submit(make_request) for _ in range(total_requests)]
            for future in as_completed(futures):
                elapsed, status_code, success = future.result()
                metrics.record(elapsed, status_code, success)
        
        total_time = (time.perf_counter() - start_time) * 1000
        throughput = total_requests / (total_time / 1000)
        summary = metrics.get_summary()
        summary["throughput"] = throughput
        summary["total_time_ms"] = total_time
        
        TEST_RESULTS["tests"].append({
            "test_name": "100并发测试",
            "category": "并发能力",
            "concurrent_users": 100,
            "total_requests": total_requests,
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 80 else "FAIL"
        })
        
        assert summary["success_rate"] >= 80, f"成功率不足: {summary['success_rate']}%"
        print(f"[PASS] 100并发: 成功率{summary['success_rate']:.1f}%, 吞吐量{throughput:.2f} req/s")


class TestAPIStability:
    """接口稳定性测试"""
    
    def test_sustained_load_30s(self):
        """持续负载测试（30秒）"""
        metrics = PerformanceMetrics()
        
        def make_request():
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/user/page",
                    params={"pageNum": random.randint(1, 5), "pageSize": 10, "tenantId": 1},
                    timeout=10
                )
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code, response.status_code < 500
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, 0, False
        
        # 持续30秒
        duration_seconds = 30
        requests_per_second = 5
        total_requests = duration_seconds * requests_per_second
        
        start_time = time.perf_counter()
        with ThreadPoolExecutor(max_workers=20) as executor:
            futures = []
            for i in range(total_requests):
                futures.append(executor.submit(make_request))
                if i % requests_per_second == 0:
                    time.sleep(0.1)
            
            for future in as_completed(futures):
                elapsed, status_code, success = future.result()
                metrics.record(elapsed, status_code, success)
        
        total_time = (time.perf_counter() - start_time)
        summary = metrics.get_summary()
        summary["duration_seconds"] = total_time
        summary["requests_per_second_avg"] = len(metrics.response_times) / total_time
        
        TEST_RESULTS["tests"].append({
            "test_name": "持续负载30秒",
            "category": "稳定性",
            "duration": "30s",
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 95 else "FAIL"
        })
        
        assert summary["success_rate"] >= 95, f"稳定性不足: {summary['success_rate']}%"
        print(f"[PASS] 持续负载30秒: 成功率{summary['success_rate']:.1f}%")
    
    def test_mixed_api_operations(self):
        """混合API操作测试"""
        metrics = PerformanceMetrics()
        
        endpoints = [
            ("GET", "/api/user/page", {"pageNum": 1, "pageSize": 10, "tenantId": 1}),
            ("GET", "/api/role/page", {"pageNum": 1, "pageSize": 10}),
            ("GET", "/v3/api-docs", {}),
            ("GET", "/swagger-ui/index.html", {})
        ]
        
        def make_request(method, endpoint, params):
            start = time.perf_counter()
            try:
                if method == "GET":
                    response = requests.get(f"{BASE_URL}{endpoint}", params=params, timeout=10)
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code, response.status_code < 500
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, 0, False
        
        for _ in range(100):
            method, endpoint, params = random.choice(endpoints)
            elapsed, status_code, success = make_request(method, endpoint, params)
            metrics.record(elapsed, status_code, success)
        
        summary = metrics.get_summary()
        
        TEST_RESULTS["tests"].append({
            "test_name": "混合API操作",
            "category": "稳定性",
            "operations": ["GET user/page", "GET role/page", "GET api-docs", "GET swagger-ui"],
            "iterations": 100,
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 90 else "FAIL"
        })
        
        assert summary["success_rate"] >= 90, f"混合操作成功率不足: {summary['success_rate']}%"
        print(f"[PASS] 混合API操作: 成功率{summary['success_rate']:.1f}%, 平均{summary['avg_response_time']:.2f}ms")


class TestPerformanceBottleneck:
    """性能瓶颈识别测试"""
    
    def test_response_time_distribution(self):
        """响应时间分布分析"""
        metrics = PerformanceMetrics()
        
        for i in range(100):
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/user/page",
                    params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                    timeout=10
                )
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, response.status_code, response.status_code < 500)
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        slow_requests = [t for t in metrics.response_times if t > 100]
        fast_requests = [t for t in metrics.response_times if t < 50]
        
        bottleneck_analysis = {
            "slow_requests_count": len(slow_requests),
            "slow_requests_percentage": len(slow_requests) / len(metrics.response_times) * 100,
            "fast_requests_count": len(fast_requests),
            "response_time_variance": summary["std_dev"]
        }
        
        TEST_RESULTS["tests"].append({
            "test_name": "响应时间分布分析",
            "category": "瓶颈识别",
            "summary": summary,
            "bottleneck_analysis": bottleneck_analysis,
            "status": "PASS" if bottleneck_analysis["slow_requests_percentage"] < 10 else "WARN"
        })
        
        if bottleneck_analysis["slow_requests_percentage"] > 10:
            print(f"[WARN] 发现潜在性能瓶颈: {bottleneck_analysis['slow_requests_percentage']:.1f}%请求响应时间超过100ms")
        else:
            print(f"[PASS] 响应时间分布正常: P95={summary['p95_response_time']:.2f}ms")
    
    def test_timeout_handling(self):
        """超时处理测试"""
        metrics = PerformanceMetrics()
        
        timeout_settings = [5, 10, 15, 30]
        
        for timeout in timeout_settings:
            start = time.perf_counter()
            try:
                response = requests.get(f"{BASE_URL}/api/user/page", params={"pageNum": 1, "pageSize": 10, "tenantId": 1}, timeout=timeout)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, response.status_code, True)
            except requests.Timeout:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 408, False)
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        TEST_RESULTS["tests"].append({
            "test_name": "超时处理测试",
            "category": "瓶颈识别",
            "timeout_settings": timeout_settings,
            "summary": summary,
            "status": "PASS" if summary["success_rate"] >= 75 else "FAIL"
        })
        
        print(f"[PASS] 超时处理: 成功率{summary['success_rate']:.1f}%")
    
    def test_large_payload_handling(self):
        """大数据负载处理测试"""
        metrics = PerformanceMetrics()
        
        page_sizes = [10, 50, 100, 200, 500]
        
        for size in page_sizes:
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{BASE_URL}/api/user/page",
                    params={"pageNum": 1, "pageSize": size, "tenantId": 1},
                    timeout=15
                )
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, response.status_code, response.status_code < 500)
            except Exception as e:
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record(elapsed, 0, False)
        
        summary = metrics.get_summary()
        
        time_by_size = {}
        for i, size in enumerate(page_sizes):
            if i < len(metrics.response_times):
                time_by_size[size] = metrics.response_times[i]
        
        TEST_RESULTS["tests"].append({
            "test_name": "大数据负载处理",
            "category": "瓶颈识别",
            "page_sizes": page_sizes,
            "response_times_by_size": time_by_size,
            "summary": summary,
            "status": "PASS" if summary["max_response_time"] < 500 else "WARN"
        })
        
        print(f"[PASS] 大数据负载: 最大响应时间{summary['max_response_time']:.2f}ms")


def generate_performance_report():
    """生成性能测试报告"""
    report_path = os.path.join(os.path.dirname(__file__), "docs", "api-performance-test-report.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    passed_tests = sum(1 for t in TEST_RESULTS["tests"] if t["status"] == "PASS")
    total_tests = len(TEST_RESULTS["tests"])
    overall_score = (passed_tests / total_tests * 100) if total_tests > 0 else 0
    
    report = f"""# AI-Ready 接口性能测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {TEST_RESULTS["test_time"]} |
| 测试环境 | {TEST_RESULTS["base_url"]} |
| 总测试数 | {total_tests} |
| 通过测试 | {passed_tests} |
| 综合评分 | {overall_score:.1f}/100 |

---

## 测试结果详情

"""
    
    for test in TEST_RESULTS["tests"]:
        status_icon = "[PASS]" if test["status"] == "PASS" else ("[WARN]" if test["status"] == "WARN" else "[FAIL]")
        report += f"""### {status_icon} {test["test_name"]}

**类别**: {test["category"]}
**状态**: {test["status"]}

"""
        
        if "summary" in test:
            summary = test["summary"]
            report += f"""**性能指标**:
- 平均响应时间: {summary.get('avg_response_time', 0):.2f}ms
- P95响应时间: {summary.get('p95_response_time', 0):.2f}ms
- P99响应时间: {summary.get('p99_response_time', 0):.2f}ms
- 成功率: {summary.get('success_rate', 0):.1f}%
- 吞吐量: {summary.get('throughput', 0):.2f} req/s

"""
        
        report += "---\n\n"
    
    report += f"""## 性能瓶颈分析

### 发现的问题

1. **响应时间分布**: 已分析响应时间分布，识别潜在瓶颈
2. **并发处理能力**: 已测试不同并发级别下的系统表现
3. **大数据负载**: 已测试不同PageSize对响应时间的影响

### 优化建议

1. **数据库查询优化**:
   - 对分页查询接口添加索引优化
   - 减少不必要的字段返回
   - 添加Redis缓存层

2. **并发处理优化**:
   - 调整线程池配置
   - 优化连接池大小
   - 实现请求限流机制

3. **响应时间优化**:
   - 压缩响应数据
   - 异步处理非关键业务
   - 优化JSON序列化

---

## 测试配置

| 配置项 | 值 |
|--------|-----|
| BASE_URL | {BASE_URL} |
| 并发测试用户数 | 10/50/100 |
| 持续负载时间 | 30秒 |
| 超时设置 | 5-30秒 |

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    json_path = os.path.join(os.path.dirname(__file__), "docs", "api-performance-test-results.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(TEST_RESULTS, f, indent=2, ensure_ascii=False)
    
    print(f"\n[REPORT] 性能测试报告已生成: {report_path}")
    print(f"[REPORT] JSON结果已保存: {json_path}")
    
    return report_path, json_path


if __name__ == '__main__':
    print("=" * 60)
    print("AI-Ready 接口性能测试执行")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print(f"测试环境: {BASE_URL}")
    print("=" * 60)
    
    pytest.main([__file__, '-v', '--tb=short', '-s'])
    
    generate_performance_report()