#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 本地性能测试套件
整合API性能、数据库性能、前端渲染性能测试

使用方式:
    python run_local_performance.py              # 运行所有测试
    python run_local_performance.py --api        # 仅运行API性能测试
    python run_local_performance.py --db         # 仅运行数据库测试
    python run_local_performance.py --frontend   # 仅运行前端测试
    python run_local_performance.py --benchmark  # 运行基准测试
"""

import time
import json
import statistics
import argparse
import subprocess
import sys
import os
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List, Any, Optional
import requests

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 性能基线
PERFORMANCE_BASELINE = {
    "api_response_time_avg_ms": 100,
    "api_response_time_p95_ms": 200,
    "db_query_time_avg_ms": 50,
    "frontend_load_time_ms": 3000,
    "throughput_min_req_s": 100,
    "concurrent_users": 100,
    "error_rate_max_percent": 1,
}

# 测试结果
TEST_RESULTS = {
    "test_time": "",
    "base_url": BASE_URL,
    "baseline": PERFORMANCE_BASELINE,
    "api_tests": [],
    "db_tests": [],
    "frontend_tests": [],
    "summary": {}
}


class TestResult:
    """测试结果"""
    def __init__(self, name: str, category: str):
        self.name = name
        self.category = category
        self.status = "SKIP"
        self.actual_value = 0
        self.baseline_value = 0
        self.unit = ""
        self.message = ""
        self.details = {}
    
    def pass_(self, message: str):
        self.status = "PASS"
        self.message = message
    
    def fail(self, message: str):
        self.status = "FAIL"
        self.message = message
    
    def warn(self, message: str):
        self.status = "WARN"
        self.message = message
    
    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "category": self.category,
            "status": self.status,
            "actual_value": round(self.actual_value, 2),
            "baseline_value": self.baseline_value,
            "unit": self.unit,
            "message": self.message,
            "details": self.details
        }


# ==================== API响应时间测试 ====================

def test_api_response_time():
    """API响应时间测试"""
    print("\n" + "="*50)
    print("API响应时间测试")
    print("="*50)
    
    results = []
    
    # 测试端点配置
    endpoints = [
        {"path": "/", "method": "GET", "params": {}, "name": "根路径"},
        {"path": "/api/user/page", "method": "GET", "params": {"pageNum": 1, "pageSize": 10, "tenantId": 1}, "name": "用户列表"},
        {"path": "/api/role/page", "method": "GET", "params": {"pageNum": 1, "pageSize": 10}, "name": "角色列表"},
        {"path": "/v3/api-docs", "method": "GET", "params": {}, "name": "API文档"},
    ]
    
    for ep in endpoints:
        result = TestResult(f"API响应: {ep['name']}", "API性能")
        result.unit = "ms"
        result.baseline_value = PERFORMANCE_BASELINE["api_response_time_avg_ms"]
        
        times = []
        for i in range(20):
            start = time.perf_counter()
            try:
                resp = requests.get(f"{BASE_URL}{ep['path']}", params=ep['params'], timeout=10)
                elapsed = (time.perf_counter() - start) * 1000
                if resp.status_code < 500:
                    times.append(elapsed)
            except:
                pass
        
        if times:
            avg = statistics.mean(times)
            sorted_times = sorted(times)
            p95 = sorted_times[int(len(sorted_times) * 0.95)]
            
            result.actual_value = avg
            result.details = {
                "samples": len(times),
                "avg_ms": round(avg, 2),
                "p95_ms": round(p95, 2),
                "min_ms": round(min(times), 2),
                "max_ms": round(max(times), 2)
            }
            
            if avg <= result.baseline_value:
                result.pass_(f"平均{avg:.2f}ms, P95={p95:.2f}ms")
            else:
                result.fail(f"平均{avg:.2f}ms超过基线{result.baseline_value}ms")
        else:
            result.fail("API无响应")
        
        results.append(result)
        print(f"  [{result.status}] {result.name}: {result.message}")
    
    return results


def test_api_throughput():
    """API吞吐量测试"""
    print("\n--- API吞吐量测试 ---")
    results = []
    
    for concurrency in [10, 50, 100]:
        result = TestResult(f"吞吐量({concurrency}并发)", "API吞吐")
        result.unit = "req/s"
        result.baseline_value = PERFORMANCE_BASELINE["throughput_min_req_s"]
        
        success_count = 0
        fail_count = 0
        
        def make_request():
            try:
                resp = requests.get(
                    f"{API_BASE}/user/page",
                    params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                    timeout=15
                )
                return resp.status_code < 500
            except:
                return False
        
        total_requests = concurrency * 3
        start_time = time.perf_counter()
        
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(make_request) for _ in range(total_requests)]
            for future in as_completed(futures):
                if future.result():
                    success_count += 1
                else:
                    fail_count += 1
        
        total_time = time.perf_counter() - start_time
        throughput = total_requests / total_time
        error_rate = (fail_count / total_requests) * 100
        
        result.actual_value = throughput
        result.details = {
            "concurrency": concurrency,
            "total_requests": total_requests,
            "success_rate": round((success_count / total_requests) * 100, 2),
            "error_rate": round(error_rate, 2),
            "duration_s": round(total_time, 2)
        }
        
        if throughput >= result.baseline_value and error_rate < PERFORMANCE_BASELINE["error_rate_max_percent"]:
            result.pass_(f"吞吐量{throughput:.2f}req/s, 错误率{error_rate:.2f}%")
        else:
            result.fail(f"吞吐量{throughput:.2f}或错误率{error_rate:.2f}%不达标")
        
        results.append(result)
        print(f"  [{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== 数据库查询性能测试 ====================

def test_db_query_performance():
    """数据库查询性能测试"""
    print("\n" + "="*50)
    print("数据库查询性能测试")
    print("="*50)
    
    results = []
    
    # 单表查询
    result = TestResult("单表查询性能", "数据库性能")
    result.unit = "ms"
    result.baseline_value = PERFORMANCE_BASELINE["db_query_time_avg_ms"]
    
    times = []
    for i in range(30):
        start = time.perf_counter()
        try:
            resp = requests.get(
                f"{API_BASE}/user/page",
                params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                timeout=10
            )
            elapsed = (time.perf_counter() - start) * 1000
            if resp.status_code < 500:
                times.append(elapsed)
        except:
            pass
    
    if times:
        avg = statistics.mean(times)
        result.actual_value = avg
        result.details = {
            "samples": len(times),
            "avg_ms": round(avg, 2),
            "min_ms": round(min(times), 2),
            "max_ms": round(max(times), 2)
        }
        
        if avg <= result.baseline_value:
            result.pass_(f"平均查询时间{avg:.2f}ms")
        else:
            result.fail(f"查询时间{avg:.2f}ms超过基线")
    else:
        result.fail("查询无响应")
    
    results.append(result)
    print(f"  [{result.status}] {result.name}: {result.message}")
    
    # 分页查询性能
    result = TestResult("分页查询性能", "数据库性能")
    result.unit = "ms"
    
    page_sizes = [10, 50, 100, 200, 500]
    size_perf = {}
    
    for size in page_sizes:
        size_times = []
        for i in range(10):
            start = time.perf_counter()
            try:
                resp = requests.get(
                    f"{API_BASE}/user/page",
                    params={"pageNum": 1, "pageSize": size, "tenantId": 1},
                    timeout=15
                )
                elapsed = (time.perf_counter() - start) * 1000
                if resp.status_code < 500:
                    size_times.append(elapsed)
            except:
                pass
        
        if size_times:
            size_perf[size] = round(statistics.mean(size_times), 2)
    
    if size_perf:
        result.details["size_performance"] = size_perf
        result.actual_value = statistics.mean(list(size_perf.values()))
        result.pass_(f"各PageSize性能: {size_perf}")
    else:
        result.fail("分页查询无响应")
    
    results.append(result)
    print(f"  [{result.status}] {result.name}: {result.message}")
    
    # 并发查询
    result = TestResult("并发查询能力", "数据库性能")
    result.unit = "req/s"
    
    concurrency = 50
    success = 0
    
    def query():
        try:
            resp = requests.get(
                f"{API_BASE}/user/page",
                params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                timeout=10
            )
            return resp.status_code < 500
        except:
            return False
    
    total = concurrency * 2
    start = time.perf_counter()
    
    with ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(query) for _ in range(total)]
        for f in as_completed(futures):
            if f.result():
                success += 1
    
    elapsed = time.perf_counter() - start
    throughput = total / elapsed
    
    result.actual_value = throughput
    result.details = {
        "concurrency": concurrency,
        "total_requests": total,
        "success_rate": round((success / total) * 100, 2),
        "throughput_req_s": round(throughput, 2)
    }
    result.pass_(f"吞吐量{throughput:.2f}req/s, 成功率{(success/total)*100:.1f}%")
    
    results.append(result)
    print(f"  [{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== 前端渲染性能测试 ====================

def test_frontend_performance():
    """前端渲染性能测试"""
    print("\n" + "="*50)
    print("前端渲染性能测试")
    print("="*50)
    
    results = []
    
    pages = [
        {"path": "/", "name": "首页"},
        {"path": "/swagger-ui/index.html", "name": "Swagger UI"},
        {"path": "/doc.html", "name": "API文档"},
    ]
    
    for page in pages:
        result = TestResult(f"页面加载: {page['name']}", "前端性能")
        result.unit = "ms"
        result.baseline_value = PERFORMANCE_BASELINE["frontend_load_time_ms"]
        
        times = []
        sizes = []
        
        for i in range(5):
            start = time.perf_counter()
            try:
                resp = requests.get(f"{BASE_URL}{page['path']}", timeout=15)
                elapsed = (time.perf_counter() - start) * 1000
                if resp.status_code == 200:
                    times.append(elapsed)
                    sizes.append(len(resp.content))
            except:
                pass
        
        if times:
            avg = statistics.mean(times)
            result.actual_value = avg
            result.details = {
                "samples": len(times),
                "avg_load_time_ms": round(avg, 2),
                "avg_content_size_kb": round(statistics.mean(sizes) / 1024, 2)
            }
            
            if avg <= result.baseline_value:
                result.pass_(f"加载时间{avg:.0f}ms, 大小{result.details['avg_content_size_kb']:.1f}KB")
            else:
                result.warn(f"加载时间{avg:.0f}ms超过基线")
        else:
            result.fail("页面加载失败")
        
        results.append(result)
        print(f"  [{result.status}] {result.name}: {result.message}")
    
    # 资源加载测试
    result = TestResult("静态资源加载", "前端性能")
    result.unit = "ms"
    
    static_resources = [
        "/swagger-ui/swagger-ui.css",
        "/swagger-ui/swagger-ui-bundle.js",
    ]
    
    resource_times = {}
    for res in static_resources:
        times = []
        for i in range(3):
            start = time.perf_counter()
            try:
                resp = requests.get(f"{BASE_URL}{res}", timeout=10)
                elapsed = (time.perf_counter() - start) * 1000
                if resp.status_code == 200:
                    times.append(elapsed)
            except:
                pass
        if times:
            resource_times[res] = round(statistics.mean(times), 2)
    
    if resource_times:
        result.details["resource_times"] = resource_times
        result.actual_value = statistics.mean(list(resource_times.values()))
        result.pass_(f"静态资源加载: {resource_times}")
    else:
        result.warn("静态资源加载测试跳过")
    
    results.append(result)
    print(f"  [{result.status}] {result.name}: {result.message}")
    
    return results


# ==================== 报告生成 ====================

def generate_report():
    """生成综合测试报告"""
    report_dir = os.path.join(os.path.dirname(__file__), "docs")
    os.makedirs(report_dir, exist_ok=True)
    
    all_results = TEST_RESULTS["api_tests"] + TEST_RESULTS["db_tests"] + TEST_RESULTS["frontend_tests"]
    
    total = len(all_results)
    passed = sum(1 for r in all_results if r.status == "PASS")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    warned = sum(1 for r in all_results if r.status == "WARN")
    
    score = (passed * 100 + warned * 70) / total if total > 0 else 0
    
    # 保存JSON结果
    json_path = os.path.join(report_dir, "local-performance-test-results.json")
    json_data = {
        "test_time": TEST_RESULTS["test_time"],
        "base_url": BASE_URL,
        "baseline": PERFORMANCE_BASELINE,
        "summary": {
            "total": total,
            "passed": passed,
            "failed": failed,
            "warned": warned,
            "score": round(score, 2)
        },
        "api_tests": [r.to_dict() for r in TEST_RESULTS["api_tests"]],
        "db_tests": [r.to_dict() for r in TEST_RESULTS["db_tests"]],
        "frontend_tests": [r.to_dict() for r in TEST_RESULTS["frontend_tests"]]
    }
    
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    # 生成Markdown报告
    report_path = os.path.join(report_dir, "AI-Ready本地性能测试报告.md")
    
    report = f"""# AI-Ready 本地性能测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {TEST_RESULTS["test_time"]} |
| 测试环境 | {BASE_URL} |
| 总测试数 | {total} |
| 通过 | {passed} |
| 失败 | {failed} |
| 警告 | {warned} |
| 综合评分 | **{score:.1f}/100** |

---

## 性能基线

| 指标 | 基线值 |
|------|--------|
| API平均响应时间 | < {PERFORMANCE_BASELINE["api_response_time_avg_ms"]}ms |
| API P95响应时间 | < {PERFORMANCE_BASELINE["api_response_time_p95_ms"]}ms |
| 数据库查询时间 | < {PERFORMANCE_BASELINE["db_query_time_avg_ms"]}ms |
| 前端加载时间 | < {PERFORMANCE_BASELINE["frontend_load_time_ms"]}ms |
| 最小吞吐量 | > {PERFORMANCE_BASELINE["throughput_min_req_s"]} req/s |
| 最大错误率 | < {PERFORMANCE_BASELINE["error_rate_max_percent"]}% |

---

## API性能测试结果

| 测试项 | 状态 | 实际值 | 基线 | 说明 |
|--------|------|--------|------|------|
"""
    
    for r in TEST_RESULTS["api_tests"]:
        status = "✅" if r.status == "PASS" else ("❌" if r.status == "FAIL" else "⚠️")
        report += f"| {r.name} | {status} | {r.actual_value:.2f}{r.unit} | {r.baseline_value}{r.unit} | {r.message} |\n"
    
    report += """
---

## 数据库性能测试结果

| 测试项 | 状态 | 实际值 | 说明 |
|--------|------|--------|------|
"""
    
    for r in TEST_RESULTS["db_tests"]:
        status = "✅" if r.status == "PASS" else ("❌" if r.status == "FAIL" else "⚠️")
        report += f"| {r.name} | {status} | {r.actual_value:.2f}{r.unit} | {r.message} |\n"
    
    report += """
---

## 前端性能测试结果

| 测试项 | 状态 | 加载时间 | 说明 |
|--------|------|----------|------|
"""
    
    for r in TEST_RESULTS["frontend_tests"]:
        status = "✅" if r.status == "PASS" else ("❌" if r.status == "FAIL" else "⚠️")
        report += f"| {r.name} | {status} | {r.actual_value:.0f}ms | {r.message} |\n"
    
    report += f"""
---

## 性能优化建议

### API优化
1. 使用响应压缩减少传输时间
2. 实现接口缓存减少数据库查询
3. 异步处理耗时操作

### 数据库优化
1. 为常用查询添加索引
2. 优化连接池配置
3. 使用读写分离

### 前端优化
1. 启用静态资源压缩
2. 使用CDN加速
3. 实现资源懒加载

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\n[报告] Markdown: {report_path}")
    print(f"[报告] JSON: {json_path}")
    
    return report_path, json_path, score


def check_api_available():
    """检查API是否可用"""
    try:
        resp = requests.get(f"{BASE_URL}/", timeout=5)
        return resp.status_code < 500
    except:
        return False


def main():
    parser = argparse.ArgumentParser(description="AI-Ready本地性能测试")
    parser.add_argument("--api", action="store_true", help="仅运行API性能测试")
    parser.add_argument("--db", action="store_true", help="仅运行数据库性能测试")
    parser.add_argument("--frontend", action="store_true", help="仅运行前端性能测试")
    parser.add_argument("--benchmark", action="store_true", help="运行完整基准测试")
    parser.add_argument("--all", action="store_true", help="运行所有测试(默认)")
    
    args = parser.parse_args()
    
    TEST_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready 本地性能测试")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print(f"测试环境: {BASE_URL}")
    print("=" * 60)
    
    # 检查API可用性
    if not check_api_available():
        print("\n[警告] API服务未启动，部分测试将跳过")
    
    run_all = not (args.api or args.db or args.frontend or args.benchmark)
    
    if run_all or args.all or args.api:
        TEST_RESULTS["api_tests"].extend(test_api_response_time())
        TEST_RESULTS["api_tests"].extend(test_api_throughput())
    
    if run_all or args.all or args.db:
        TEST_RESULTS["db_tests"].extend(test_db_query_performance())
    
    if run_all or args.all or args.frontend:
        TEST_RESULTS["frontend_tests"].extend(test_frontend_performance())
    
    if args.benchmark:
        # 运行现有基准测试脚本
        print("\n--- 执行完整基准测试 ---")
        try:
            subprocess.run([sys.executable, "run_performance_benchmark.py"], cwd=os.path.dirname(__file__))
        except:
            print("基准测试脚本执行失败")
    
    # 生成报告
    print("\n" + "=" * 60)
    report_path, json_path, score = generate_report()
    
    all_results = TEST_RESULTS["api_tests"] + TEST_RESULTS["db_tests"] + TEST_RESULTS["frontend_tests"]
    passed = sum(1 for r in all_results if r.status == "PASS")
    total = len(all_results)
    
    print(f"\n测试结果: {passed}/{total} 通过")
    print(f"综合评分: {score:.1f}/100")
    print("=" * 60)


if __name__ == "__main__":
    main()
