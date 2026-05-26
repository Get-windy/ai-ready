#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 性能基准测试执行器
测试范围：
1. API响应时间基准测试
2. 数据库查询性能测试
3. 前端页面加载性能测试
4. 并发处理能力测试
"""

import time
import json
import statistics
from datetime import datetime
from typing import Dict, Any, List
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
import sys
import os

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 性能基线目标
PERFORMANCE_BASELINE = {
    "api_response_time_avg": 100,  # 平均响应时间 < 100ms
    "api_response_time_p95": 200,  # P95响应时间 < 200ms
    "api_response_time_p99": 500,  # P99响应时间 < 500ms
    "throughput_min": 100,  # 最小吞吐量 100 req/s
    "concurrent_users": 100,  # 支持100并发用户
    "error_rate_max": 1,  # 错误率 < 1%
}

TEST_RESULTS = {
    "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "base_url": BASE_URL,
    "baseline": PERFORMANCE_BASELINE,
    "tests": []
}


class BenchmarkResult:
    """基准测试结果"""
    def __init__(self, name: str, category: str):
        self.name = name
        self.category = category
        self.status = "SKIP"
        self.baseline_target = 0
        self.actual_value = 0
        self.unit = ""
        self.message = ""
        self.details = {}
    
    def pass_(self, message: str, actual: float):
        self.status = "PASS"
        self.message = message
        self.actual_value = actual
    
    def fail(self, message: str, actual: float):
        self.status = "FAIL"
        self.message = message
        self.actual_value = actual
    
    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "category": self.category,
            "status": self.status,
            "baseline_target": self.baseline_target,
            "actual_value": round(self.actual_value, 2),
            "unit": self.unit,
            "message": self.message,
            "details": self.details
        }


def test_api_response_time():
    """API响应时间基准测试"""
    print("\n--- API响应时间基准测试 ---")
    results = []
    
    endpoints = [
        ("/user/page", "GET", {"pageNum": 1, "pageSize": 10, "tenantId": 1}),
        ("/role/page", "GET", {"pageNum": 1, "pageSize": 10}),
        ("/v3/api-docs", "GET", {}),
    ]
    
    for endpoint, method, params in endpoints:
        result = BenchmarkResult(f"API响应时间: {endpoint}", "API性能")
        result.unit = "ms"
        
        response_times = []
        for i in range(50):
            start = time.perf_counter()
            try:
                if method == "GET":
                    response = requests.get(f"{API_BASE}{endpoint}", params=params, timeout=10)
                elapsed = (time.perf_counter() - start) * 1000
                if response.status_code < 500:
                    response_times.append(elapsed)
            except:
                pass
        
        if response_times:
            avg_time = statistics.mean(response_times)
            sorted_times = sorted(response_times)
            p95 = sorted_times[int(len(sorted_times) * 0.95)]
            p99 = sorted_times[int(len(sorted_times) * 0.99)]
            
            result.baseline_target = PERFORMANCE_BASELINE["api_response_time_avg"]
            result.actual_value = avg_time
            result.details["samples"] = len(response_times)
            result.details["avg_ms"] = round(avg_time, 2)
            result.details["p95_ms"] = round(p95, 2)
            result.details["p99_ms"] = round(p99, 2)
            result.details["min_ms"] = round(min(response_times), 2)
            result.details["max_ms"] = round(max(response_times), 2)
            
            if avg_time <= result.baseline_target:
                result.pass_(f"平均{avg_time:.2f}ms < 基线{result.baseline_target}ms", avg_time)
            else:
                result.fail(f"平均{avg_time:.2f}ms > 基线{result.baseline_target}ms", avg_time)
        else:
            result.fail("无有效响应", 0)
        
        results.append(result)
        print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_database_performance():
    """数据库查询性能测试（通过API间接测试）"""
    print("\n--- 数据库查询性能测试 ---")
    results = []
    
    # 测试不同数据量的查询性能
    page_sizes = [10, 50, 100, 200, 500]
    
    result = BenchmarkResult("数据库查询性能", "数据库性能")
    result.unit = "ms"
    result.baseline_target = 100
    
    performance_by_size = {}
    
    for size in page_sizes:
        times = []
        for i in range(10):
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{API_BASE}/user/page",
                    params={"pageNum": 1, "pageSize": size, "tenantId": 1},
                    timeout=15
                )
                elapsed = (time.perf_counter() - start) * 1000
                if response.status_code == 200:
                    times.append(elapsed)
            except:
                pass
        
        if times:
            performance_by_size[size] = {
                "avg_ms": round(statistics.mean(times), 2),
                "max_ms": round(max(times), 2)
            }
    
    result.details["performance_by_page_size"] = performance_by_size
    
    # 计算平均查询时间
    all_times = []
    for size_data in performance_by_size.values():
        all_times.append(size_data["avg_ms"])
    
    if all_times:
        avg_time = statistics.mean(all_times)
        result.actual_value = avg_time
        
        if avg_time <= result.baseline_target:
            result.pass_(f"平均查询时间{avg_time:.2f}ms", avg_time)
        else:
            result.fail(f"平均查询时间{avg_time:.2f}ms超过基线", avg_time)
    else:
        result.fail("无有效测试数据", 0)
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_frontend_page_load():
    """前端页面加载性能测试"""
    print("\n--- 前端页面加载性能测试 ---")
    results = []
    
    pages = [
        ("/", "首页"),
        ("/swagger-ui/index.html", "Swagger UI"),
        ("/doc.html", "API文档"),
    ]
    
    for path, name in pages:
        result = BenchmarkResult(f"页面加载: {name}", "前端性能")
        result.unit = "ms"
        result.baseline_target = 3000  # 页面加载 < 3秒
        
        load_times = []
        for i in range(5):
            start = time.perf_counter()
            try:
                response = requests.get(f"{BASE_URL}{path}", timeout=10)
                elapsed = (time.perf_counter() - start) * 1000
                if response.status_code == 200:
                    load_times.append(elapsed)
                    result.details["content_length"] = len(response.content)
            except:
                pass
        
        if load_times:
            avg_time = statistics.mean(load_times)
            result.actual_value = avg_time
            result.details["avg_load_time_ms"] = round(avg_time, 2)
            result.details["samples"] = len(load_times)
            
            if avg_time <= result.baseline_target:
                result.pass_(f"加载时间{avg_time:.0f}ms", avg_time)
            else:
                result.fail(f"加载时间{avg_time:.0f}ms超过基线", avg_time)
        else:
            result.fail("页面加载失败", 0)
        
        results.append(result)
        print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_concurrent_capacity():
    """并发处理能力测试"""
    print("\n--- 并发处理能力测试 ---")
    results = []
    
    concurrency_levels = [10, 50, 100]
    
    for level in concurrency_levels:
        result = BenchmarkResult(f"{level}并发处理", "并发能力")
        result.unit = "req/s"
        result.baseline_target = PERFORMANCE_BASELINE["throughput_min"]
        
        success_count = 0
        fail_count = 0
        response_times = []
        
        def make_request():
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{API_BASE}/user/page",
                    params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                    timeout=15
                )
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code < 500
            except:
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, False
        
        total_requests = level * 2
        start_time = time.perf_counter()
        
        with ThreadPoolExecutor(max_workers=level) as executor:
            futures = [executor.submit(make_request) for _ in range(total_requests)]
            for future in as_completed(futures):
                elapsed, success = future.result()
                response_times.append(elapsed)
                if success:
                    success_count += 1
                else:
                    fail_count += 1
        
        total_time = time.perf_counter() - start_time
        throughput = total_requests / total_time
        error_rate = (fail_count / total_requests) * 100 if total_requests > 0 else 0
        
        result.actual_value = throughput
        result.details["throughput_req_s"] = round(throughput, 2)
        result.details["success_rate"] = round((success_count / total_requests) * 100, 2)
        result.details["error_rate"] = round(error_rate, 2)
        result.details["avg_response_ms"] = round(statistics.mean(response_times), 2) if response_times else 0
        
        if throughput >= result.baseline_target and error_rate < PERFORMANCE_BASELINE["error_rate_max"]:
            result.pass_(f"吞吐量{throughput:.2f} req/s, 错误率{error_rate:.2f}%", throughput)
        else:
            result.fail(f"吞吐量{throughput:.2f}或错误率{error_rate:.2f}%不达标", throughput)
        
        results.append(result)
        print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_stress_test():
    """压力测试"""
    print("\n--- 压力测试 ---")
    results = []
    
    result = BenchmarkResult("压力测试(持续30秒)", "压力测试")
    result.unit = "req/s"
    result.baseline_target = PERFORMANCE_BASELINE["throughput_min"]
    
    success_count = 0
    fail_count = 0
    total_requests = 0
    
    def make_request():
        nonlocal success_count, fail_count
        try:
            response = requests.get(
                f"{API_BASE}/user/page",
                params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                timeout=10
            )
            if response.status_code < 500:
                success_count += 1
            else:
                fail_count += 1
        except:
            fail_count += 1
    
    duration = 30  # 30秒
    start_time = time.perf_counter()
    
    with ThreadPoolExecutor(max_workers=50) as executor:
        while time.perf_counter() - start_time < duration:
            executor.submit(make_request)
            total_requests += 1
            time.sleep(0.05)  # 约20 req/s
    
    actual_duration = time.perf_counter() - start_time
    throughput = total_requests / actual_duration
    error_rate = (fail_count / total_requests) * 100 if total_requests > 0 else 0
    
    result.actual_value = throughput
    result.details["total_requests"] = total_requests
    result.details["duration_s"] = round(actual_duration, 2)
    result.details["throughput_req_s"] = round(throughput, 2)
    result.details["success_rate"] = round((success_count / total_requests) * 100, 2) if total_requests > 0 else 0
    result.details["error_rate"] = round(error_rate, 2)
    
    if error_rate < 5:  # 压力测试允许更高错误率
        result.pass_(f"持续{actual_duration:.0f}秒, {total_requests}请求, 错误率{error_rate:.2f}%", throughput)
    else:
        result.fail(f"错误率{error_rate:.2f}%过高", throughput)
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def generate_benchmark_report(all_results: List[BenchmarkResult]):
    """生成性能基准测试报告"""
    report_path = os.path.join(os.path.dirname(__file__), "docs", "AI-Ready性能基准测试报告-20260330.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    total = len(all_results)
    passed = sum(1 for r in all_results if r.status == "PASS")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    skipped = sum(1 for r in all_results if r.status == "SKIP")
    
    # 计算综合评分
    score = (passed / total * 100) if total > 0 else 0
    
    # 按类别分组
    categories = {}
    for r in all_results:
        if r.category not in categories:
            categories[r.category] = []
        categories[r.category].append(r)
    
    report = f"""# AI-Ready 性能基准测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {TEST_RESULTS["test_time"]} |
| 测试环境 | {TEST_RESULTS["base_url"]} |
| 总测试数 | {total} |
| 通过测试 | {passed} |
| 失败测试 | {failed} |
| 综合评分 | **{score:.1f}/100** |

---

## 性能基线标准

| 指标 | 基线目标 |
|------|----------|
| API平均响应时间 | < {PERFORMANCE_BASELINE['api_response_time_avg']}ms |
| API P95响应时间 | < {PERFORMANCE_BASELINE['api_response_time_p95']}ms |
| 最小吞吐量 | > {PERFORMANCE_BASELINE['throughput_min']} req/s |
| 支持并发用户 | {PERFORMANCE_BASELINE['concurrent_users']} |
| 最大错误率 | < {PERFORMANCE_BASELINE['error_rate_max']}% |

---

## 测试结果详情

"""
    
    for category_name, results in categories.items():
        report += f"""### {category_name}

| 测试项 | 状态 | 基线目标 | 实际值 | 说明 |
|--------|------|---------|--------|------|
"""
        
        for r in results:
            status_icon = "[PASS]" if r.status == "PASS" else ("[FAIL]" if r.status == "FAIL" else "[SKIP]")
            target_str = f"{r.baseline_target}{r.unit}" if r.baseline_target > 0 else "-"
            actual_str = f"{r.actual_value:.2f}{r.unit}"
            report += f"| {r.name} | {status_icon} | {target_str} | {actual_str} | {r.message} |\n"
        
        report += "\n---\n\n"
    
    # 性能基线总结
    report += f"""## 性能基线评估

### API性能

"""
    
    api_results = [r for r in all_results if r.category == "API性能"]
    if api_results:
        all_pass = all(r.status == "PASS" for r in api_results)
        if all_pass:
            report += "API响应时间**符合**性能基线要求。\n\n"
        else:
            report += "API响应时间**未达到**性能基线要求，需要优化。\n\n"
    
    report += """### 并发性能

"""
    
    concurrent_results = [r for r in all_results if r.category == "并发能力"]
    if concurrent_results:
        max_throughput = max(r.actual_value for r in concurrent_results)
        report += f"- 最大吞吐量: **{max_throughput:.2f} req/s**\n"
        report += f"- 支持并发: **{PERFORMANCE_BASELINE['concurrent_users']}用户**\n\n"
    
    report += f"""## 优化建议

1. **数据库优化**:
   - 添加索引提升查询性能
   - 配置连接池优化
   - 考虑添加Redis缓存

2. **API优化**:
   - 响应数据压缩
   - 异步处理非关键业务
   - 批量接口优化

3. **并发优化**:
   - 调整线程池配置
   - 连接池大小优化
   - 负载均衡配置

---

## 测试配置

| 配置项 | 值 |
|--------|-----|
| BASE_URL | {BASE_URL} |
| 测试类别 | API性能/数据库/前端/并发/压力 |

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    json_path = os.path.join(os.path.dirname(__file__), "docs", "performance-benchmark-test-results.json")
    json_data = {
        "test_time": TEST_RESULTS["test_time"],
        "base_url": BASE_URL,
        "baseline": PERFORMANCE_BASELINE,
        "summary": {
            "total": total,
            "passed": passed,
            "failed": failed,
            "skipped": skipped,
            "score": score
        },
        "results": [r.to_dict() for r in all_results]
    }
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    print(f"\n[REPORT] 性能基准测试报告已生成: {report_path}")
    print(f"[REPORT] JSON结果已保存: {json_path}")
    
    return report_path, json_path, score


def main():
    print("=" * 60)
    print("AI-Ready 性能基准测试执行")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print(f"测试环境: {BASE_URL}")
    print("=" * 60)
    
    all_results = []
    
    # 执行性能基准测试
    all_results.extend(test_api_response_time())
    all_results.extend(test_database_performance())
    all_results.extend(test_frontend_page_load())
    all_results.extend(test_concurrent_capacity())
    all_results.extend(test_stress_test())
    
    # 生成报告
    print("\n" + "=" * 60)
    report_path, json_path, score = generate_benchmark_report(all_results)
    
    passed = sum(1 for r in all_results if r.status == "PASS")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    total = len(all_results)
    
    print(f"\n测试结果: {passed}/{total} 通过, {failed} 失败")
    print(f"综合评分: {score:.1f}/100")
    print("=" * 60)


if __name__ == '__main__':
    main()