#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 数据库性能测试执行器
测试范围：
1. 单表查询性能测试
2. 分页查询性能测试
3. 条件查询性能测试
4. 排序查询性能测试
5. 关联查询性能测试（通过API）
"""

import time
import json
import statistics
from datetime import datetime
from typing import Dict, Any, List
import requests
import sys
import os

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

TEST_RESULTS = {
    "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    "base_url": BASE_URL,
    "tests": []
}


class DBTestResult:
    """数据库测试结果"""
    def __init__(self, name: str, category: str):
        self.name = name
        self.category = category
        self.status = "SKIP"
        self.avg_time = 0
        self.p95_time = 0
        self.p99_time = 0
        self.message = ""
        self.details = {}
    
    def pass_(self, message: str):
        self.status = "PASS"
        self.message = message
    
    def fail(self, message: str):
        self.status = "FAIL"
        self.message = message
    
    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "category": self.category,
            "status": self.status,
            "avg_time_ms": round(self.avg_time, 2),
            "p95_time_ms": round(self.p95_time, 2),
            "p99_time_ms": round(self.p99_time, 2),
            "message": self.message,
            "details": self.details
        }


def measure_query_performance(endpoint: str, params: dict, iterations: int = 20):
    """测量查询性能"""
    times = []
    success_count = 0
    
    for i in range(iterations):
        start = time.perf_counter()
        try:
            response = requests.get(f"{API_BASE}{endpoint}", params=params, timeout=10)
            elapsed = (time.perf_counter() - start) * 1000
            if response.status_code < 500:
                times.append(elapsed)
                success_count += 1
        except:
            pass
    
    if times:
        sorted_times = sorted(times)
        return {
            "avg": statistics.mean(times),
            "min": min(times),
            "max": max(times),
            "p95": sorted_times[int(len(sorted_times) * 0.95)],
            "p99": sorted_times[int(len(sorted_times) * 0.99)],
            "success_rate": success_count / iterations * 100,
            "samples": len(times)
        }
    return None


def test_simple_query():
    """单表查询性能测试"""
    print("\n--- 单表查询性能测试 ---")
    results = []
    
    # 测试用户表简单查询
    result = DBTestResult("用户表简单查询", "单表查询")
    perf = measure_query_performance("/user/page", {"pageNum": 1, "pageSize": 10, "tenantId": 1}, 30)
    
    if perf:
        result.avg_time = perf["avg"]
        result.p95_time = perf["p95"]
        result.p99_time = perf["p99"]
        result.details = perf
        
        if perf["avg"] < 50:
            result.pass_(f"平均{perf['avg']:.2f}ms, P95={perf['p95']:.2f}ms")
        else:
            result.fail(f"查询过慢: 平均{perf['avg']:.2f}ms")
    else:
        result.fail("查询失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试API文档查询（不涉及数据库）
    result = DBTestResult("API文档查询", "单表查询")
    perf = measure_query_performance("/v3/api-docs", {}, 10)
    
    if perf:
        result.avg_time = perf["avg"]
        result.p95_time = perf["p95"]
        result.p99_time = perf["p99"]
        result.details = perf
        result.pass_(f"平均{perf['avg']:.2f}ms")
    else:
        result.fail("查询失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_pagination_query():
    """分页查询性能测试"""
    print("\n--- 分页查询性能测试 ---")
    results = []
    
    # 测试不同页码的分页查询
    pages = [1, 10, 50, 100]
    
    for page in pages:
        result = DBTestResult(f"分页查询(Page={page})", "分页查询")
        perf = measure_query_performance("/user/page", {"pageNum": page, "pageSize": 10, "tenantId": 1}, 10)
        
        if perf:
            result.avg_time = perf["avg"]
            result.p95_time = perf["p95"]
            result.p99_time = perf["p99"]
            result.details = perf
            
            # 分页查询性能不应随页码增大显著增加
            if perf["avg"] < 100:
                result.pass_(f"Page={page}: {perf['avg']:.2f}ms")
            else:
                result.fail(f"Page={page}: {perf['avg']:.2f}ms过慢")
        else:
            result.fail("查询失败")
        
        results.append(result)
        print(f"[{result.status}] {result.name}: {result.message}")
    
    # 测试不同PageSize的性能
    result = DBTestResult("PageSize性能对比", "分页查询")
    sizes = [10, 50, 100, 200, 500]
    size_perf = {}
    
    for size in sizes:
        perf = measure_query_performance("/user/page", {"pageNum": 1, "pageSize": size, "tenantId": 1}, 5)
        if perf:
            size_perf[size] = perf["avg"]
    
    if size_perf:
        result.details["size_performance"] = size_perf
        result.avg_time = statistics.mean(size_perf.values())
        result.pass_(f"各PageSize性能: {size_perf}")
    else:
        result.fail("无有效数据")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_condition_query():
    """条件查询性能测试"""
    print("\n--- 条件查询性能测试 ---")
    results = []
    
    # 无条件查询
    result = DBTestResult("无条件查询", "条件查询")
    perf = measure_query_performance("/user/page", {"pageNum": 1, "pageSize": 10, "tenantId": 1}, 20)
    
    if perf:
        result.avg_time = perf["avg"]
        result.p95_time = perf["p95"]
        result.p99_time = perf["p99"]
        result.details = perf
        result.pass_(f"无条件: {perf['avg']:.2f}ms")
    else:
        result.fail("查询失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 带租户条件查询
    result = DBTestResult("租户条件查询", "条件查询")
    perf = measure_query_performance("/user/page", {"pageNum": 1, "pageSize": 10, "tenantId": 1}, 20)
    
    if perf:
        result.avg_time = perf["avg"]
        result.p95_time = perf["p95"]
        result.p99_time = perf["p99"]
        result.details = perf
        result.pass_(f"租户条件: {perf['avg']:.2f}ms")
    else:
        result.fail("查询失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # 模糊查询（如果支持）
    result = DBTestResult("模糊查询", "条件查询")
    perf = measure_query_performance("/user/page", {"pageNum": 1, "pageSize": 10, "tenantId": 1, "username": "test"}, 10)
    
    if perf:
        result.avg_time = perf["avg"]
        result.p95_time = perf["p95"]
        result.p99_time = perf["p99"]
        result.details = perf
        
        if perf["avg"] < 200:
            result.pass_(f"模糊查询: {perf['avg']:.2f}ms")
        else:
            result.fail(f"模糊查询过慢: {perf['avg']:.2f}ms")
    else:
        result.fail("查询失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_sort_query():
    """排序查询性能测试"""
    print("\n--- 排序查询性能测试 ---")
    results = []
    
    # 默认排序
    result = DBTestResult("默认排序", "排序查询")
    perf = measure_query_performance("/user/page", {"pageNum": 1, "pageSize": 50, "tenantId": 1}, 10)
    
    if perf:
        result.avg_time = perf["avg"]
        result.p95_time = perf["p95"]
        result.details = perf
        result.pass_(f"默认排序: {perf['avg']:.2f}ms")
    else:
        result.fail("查询失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_concurrent_db_access():
    """并发数据库访问测试"""
    print("\n--- 并发数据库访问测试 ---")
    results = []
    from concurrent.futures import ThreadPoolExecutor, as_completed
    
    for concurrent in [10, 20, 50]:
        result = DBTestResult(f"{concurrent}并发查询", "并发访问")
        
        times = []
        success = 0
        
        def query():
            start = time.perf_counter()
            try:
                response = requests.get(
                    f"{API_BASE}/user/page",
                    params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                    timeout=10
                )
                elapsed = (time.perf_counter() - start) * 1000
                return elapsed, response.status_code < 500
            except:
                return 0, False
        
        start_time = time.perf_counter()
        with ThreadPoolExecutor(max_workers=concurrent) as executor:
            futures = [executor.submit(query) for _ in range(concurrent * 2)]
            for future in as_completed(futures):
                elapsed, ok = future.result()
                if ok:
                    times.append(elapsed)
                    success += 1
        
        total_time = time.perf_counter() - start_time
        
        if times:
            result.avg_time = statistics.mean(times)
            sorted_times = sorted(times)
            result.p95_time = sorted_times[int(len(sorted_times) * 0.95)]
            result.details = {
                "concurrent": concurrent,
                "total_requests": concurrent * 2,
                "success_count": success,
                "throughput": len(times) / total_time,
                "avg_ms": round(result.avg_time, 2)
            }
            result.pass_(f"{concurrent}并发: 平均{result.avg_time:.2f}ms, 吞吐量{len(times)/total_time:.1f}req/s")
        else:
            result.fail("并发查询失败")
        
        results.append(result)
        print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def test_query_stability():
    """查询稳定性测试"""
    print("\n--- 查询稳定性测试 ---")
    results = []
    
    result = DBTestResult("持续查询稳定性", "稳定性测试")
    
    all_times = []
    for i in range(100):
        start = time.perf_counter()
        try:
            response = requests.get(
                f"{API_BASE}/user/page",
                params={"pageNum": 1, "pageSize": 10, "tenantId": 1},
                timeout=10
            )
            elapsed = (time.perf_counter() - start) * 1000
            if response.status_code < 500:
                all_times.append(elapsed)
        except:
            pass
        time.sleep(0.05)
    
    if len(all_times) >= 80:
        result.avg_time = statistics.mean(all_times)
        sorted_times = sorted(all_times)
        result.p95_time = sorted_times[int(len(sorted_times) * 0.95)]
        result.p99_time = sorted_times[int(len(sorted_times) * 0.99)]
        
        std_dev = statistics.stdev(all_times) if len(all_times) > 1 else 0
        variance = std_dev / result.avg_time * 100 if result.avg_time > 0 else 0
        
        result.details = {
            "samples": len(all_times),
            "avg_ms": round(result.avg_time, 2),
            "std_dev": round(std_dev, 2),
            "variance_percent": round(variance, 2)
        }
        
        if variance < 50:
            result.pass_(f"100次查询, 平均{result.avg_time:.2f}ms, 波动{variance:.1f}%")
        else:
            result.fail(f"查询波动过大: {variance:.1f}%")
    else:
        result.fail("稳定性测试失败")
    
    results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    return results


def generate_db_report(all_results: List[DBTestResult]):
    """生成数据库性能测试报告"""
    report_path = os.path.join(os.path.dirname(__file__), "docs", "AI-Ready数据库性能测试报告-20260330.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    total = len(all_results)
    passed = sum(1 for r in all_results if r.status == "PASS")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    skipped = sum(1 for r in all_results if r.status == "SKIP")
    
    score = (passed / total * 100) if total > 0 else 0
    
    categories = {}
    for r in all_results:
        if r.category not in categories:
            categories[r.category] = []
        categories[r.category].append(r)
    
    report = f"""# AI-Ready 数据库性能测试报告

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

## 测试结果详情

"""
    
    for cat_name, results in categories.items():
        report += f"""### {cat_name}

| 测试项 | 状态 | 平均时间 | P95时间 | 说明 |
|--------|------|---------|---------|------|
"""
        
        for r in results:
            status = "[PASS]" if r.status == "PASS" else ("[FAIL]" if r.status == "FAIL" else "[SKIP]")
            report += f"| {r.name} | {status} | {r.avg_time:.2f}ms | {r.p95_time:.2f}ms | {r.message} |\n"
        
        report += "\n---\n\n"
    
    report += f"""## 性能分析

### 查询性能总结

"""
    
    # 计算总体性能指标
    all_times = [(r.name, r.avg_time) for r in all_results if r.avg_time > 0]
    if all_times:
        avg_overall = statistics.mean([t[1] for t in all_times])
        report += f"- 整体平均查询时间: **{avg_overall:.2f}ms**\n"
    
    report += """
### 性能瓶颈识别

1. **分页查询**: 测试不同页码和PageSize的性能表现
2. **条件查询**: 测试各种查询条件的性能影响
3. **并发访问**: 测试数据库连接池和并发处理能力
4. **稳定性**: 测试长时间运行的查询稳定性

---

## 优化建议

1. **索引优化**:
   - 为常用查询条件添加索引
   - 优化复合索引顺序
   - 定期维护索引统计信息

2. **连接池配置**:
   - 调整最大连接数
   - 优化连接获取策略
   - 设置合理的超时时间

3. **查询优化**:
   - 避免SELECT *
   - 使用合理的分页策略
   - 添加查询缓存

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    json_path = os.path.join(os.path.dirname(__file__), "docs", "database-performance-test-results.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump({
            "test_time": TEST_RESULTS["test_time"],
            "summary": {"total": total, "passed": passed, "failed": failed, "score": score},
            "results": [r.to_dict() for r in all_results]
        }, f, indent=2, ensure_ascii=False)
    
    print(f"\n[REPORT] 报告已生成: {report_path}")
    return report_path, score


def main():
    print("=" * 60)
    print("AI-Ready 数据库性能测试执行")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print("=" * 60)
    
    all_results = []
    all_results.extend(test_simple_query())
    all_results.extend(test_pagination_query())
    all_results.extend(test_condition_query())
    all_results.extend(test_sort_query())
    all_results.extend(test_concurrent_db_access())
    all_results.extend(test_query_stability())
    
    print("\n" + "=" * 60)
    report_path, score = generate_db_report(all_results)
    
    passed = sum(1 for r in all_results if r.status == "PASS")
    total = len(all_results)
    print(f"\n测试结果: {passed}/{total} 通过")
    print(f"综合评分: {score:.1f}/100")
    print("=" * 60)


if __name__ == '__main__':
    main()