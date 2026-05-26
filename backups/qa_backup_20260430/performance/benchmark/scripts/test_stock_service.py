#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
库存管理服务性能测试脚本
"""

import requests
import time
import concurrent.futures
import json
from datetime import datetime

BASE_URL = "http://localhost:8083"

def test_get_stock(sku_id):
    """获取库存测试"""
    url = f"{BASE_URL}/api/stocks/{sku_id}"
    
    start_time = time.time()
    try:
        response = requests.get(url, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        return {
            "status_code": response.status_code,
            "elapsed_ms": elapsed,
            "data": response.json() if response.status_code == 200 else None
        }
    except Exception as e:
        return {
            "status_code": 0,
            "elapsed_ms": (time.time() - start_time) * 1000,
            "error": str(e)
        }

def test_update_stock(sku_id, quantity):
    """更新库存测试"""
    url = f"{BASE_URL}/api/stocks/{sku_id}/update"
    payload = {
        "quantity": quantity,
        "type": "INCREASE"
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, json=payload, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        return {
            "status_code": response.status_code,
            "elapsed_ms": elapsed,
            "data": response.json() if response.status_code == 200 else None
        }
    except Exception as e:
        return {
            "status_code": 0,
            "elapsed_ms": (time.time() - start_time) * 1000,
            "error": str(e)
        }

def test_lock_stock(sku_id, quantity):
    """锁定库存测试"""
    url = f"{BASE_URL}/api/stocks/{sku_id}/lock"
    payload = {
        "quantity": quantity,
        "orderNo": "TEST_ORDER_001"
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, json=payload, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        return {
            "status_code": response.status_code,
            "elapsed_ms": elapsed,
            "data": response.json() if response.status_code == 200 else None
        }
    except Exception as e:
        return {
            "status_code": 0,
            "elapsed_ms": (time.time() - start_time) * 1000,
            "error": str(e)
        }

def test_unlock_stock(sku_id, quantity):
    """解锁库存测试"""
    url = f"{BASE_URL}/api/stocks/{sku_id}/unlock"
    payload = {
        "quantity": quantity,
        "orderNo": "TEST_ORDER_001"
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, json=payload, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        return {
            "status_code": response.status_code,
            "elapsed_ms": elapsed,
            "data": response.json() if response.status_code == 200 else None
        }
    except Exception as e:
        return {
            "status_code": 0,
            "elapsed_ms": (time.time() - start_time) * 1000,
            "error": str(e)
        }

def run_concurrent_test(test_func, concurrency=50, iterations=500, *args):
    """并发测试执行"""
    results = []
    
    print(f"\n开始执行并发测试: {test_func.__name__}")
    print(f"并发数: {concurrency}, 迭代次数: {iterations}")
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(test_func, *args) for _ in range(iterations)]
        
        for i, future in enumerate(concurrent.futures.as_completed(futures), 1):
            result = future.result()
            results.append(result)
            
            if i % 100 == 0:
                print(f"完成 {i}/{iterations}")
    
    return results

def analyze_results(results, test_name):
    """分析测试结果"""
    success_results = [r for r in results if r["status_code"] in [200, 201]]
    failed_results = [r for r in results if r["status_code"] not in [200, 201]]
    
    elapsed_times = [r["elapsed_ms"] for r in success_results]
    
    if not elapsed_times:
        print(f"\n{test_name} - 警告: 没有成功的测试结果")
        return {
            "test_name": test_name,
            "total": len(results),
            "success": 0,
            "failed": len(results),
            "elapsed_times": [],
            "error": "No successful results"
        }
    
    elapsed_times_sorted = sorted(elapsed_times)
    
    p50_index = int(len(elapsed_times_sorted) * 0.50)
    p95_index = int(len(elapsed_times_sorted) * 0.95)
    p99_index = int(len(elapsed_times_sorted) * 0.99)
    
    p50 = elapsed_times_sorted[min(p50_index, len(elapsed_times_sorted)-1)]
    p95 = elapsed_times_sorted[min(p95_index, len(elapsed_times_sorted)-1)]
    p99 = elapsed_times_sorted[min(p99_index, len(elapsed_times_sorted)-1)]
    
    total_time_seconds = 60
    qps = len(elapsed_times) / total_time_seconds
    
    return {
        "test_name": test_name,
        "total": len(results),
        "success": len(success_results),
        "failed": len(failed_results),
        "success_rate": len(success_results) / len(results) * 100,
        "avg_ms": sum(elapsed_times) / len(elapsed_times),
        "min_ms": min(elapsed_times),
        "max_ms": max(elapsed_times),
        "p50_ms": p50,
        "p95_ms": p95,
        "p99_ms": p99,
        "qps": qps,
        "elapsed_times": elapsed_times
    }

def print_results(analysis):
    """打印测试结果"""
    print(f"\n{'='*60}")
    print(f"测试名称: {analysis['test_name']}")
    print(f"{'='*60}")
    print(f"总请求数: {analysis['total']}")
    print(f"成功次数: {analysis['success']}")
    print(f"失败次数: {analysis['failed']}")
    print(f"成功率: {analysis['success_rate']:.2f}%")
    print(f"\n响应时间 (ms):")
    print(f"  平均: {analysis['avg_ms']:.2f}")
    print(f"  最小: {analysis['min_ms']:.2f}")
    print(f"  最大: {analysis['max_ms']:.2f}")
    print(f"  P50:  {analysis['p50_ms']:.2f}")
    print(f"  P95:  {analysis['p95_ms']:.2f}")
    print(f"  P99:  {analysis['p99_ms']:.2f}")
    print(f"\n吞吐量: {analysis['qps']:.2f} QPS")
    print(f"{'='*60}")

def main():
    """主测试函数"""
    print("="*60)
    print("库存管理服务性能测试")
    print("="*60)
    print(f"测试环境: {BASE_URL}")
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    print("\n[测试1] 库存查询性能测试")
    results_get = run_concurrent_test(test_get_stock, concurrency=50, iterations=500, sku_id="SKU001")
    analysis_get = analyze_results(results_get, "库存查询")
    print_results(analysis_get)
    
    print("\n[测试2] 库存更新性能测试")
    results_update = run_concurrent_test(test_update_stock, concurrency=30, iterations=300, sku_id="SKU001", quantity=10)
    analysis_update = analyze_results(results_update, "库存更新")
    print_results(analysis_update)
    
    print("\n[测试3] 库存锁定性能测试")
    results_lock = run_concurrent_test(test_lock_stock, concurrency=20, iterations=200, sku_id="SKU001", quantity=5)
    analysis_lock = analyze_results(results_lock, "库存锁定")
    print_results(analysis_lock)
    
    print("\n[测试4] 库存解锁性能测试")
    results_unlock = run_concurrent_test(test_unlock_stock, concurrency=20, iterations=200, sku_id="SKU001", quantity=5)
    analysis_unlock = analyze_results(results_unlock, "库存解锁")
    print_results(analysis_unlock)
    
    print("\n" + "="*60)
    print("测试汇总")
    print("="*60)
    
    all_analyses = [analysis_get, analysis_update, analysis_lock, analysis_unlock]
    
    for analysis in all_analyses:
        if 'avg_ms' in analysis:
            if analysis['p95_ms'] > 500:
                print(f"❌ {analysis['test_name']}: P95响应时间 {analysis['p95_ms']:.2f}ms > 500ms (FAIL)")
            else:
                print(f"✅ {analysis['test_name']}: P95响应时间 {analysis['p95_ms']:.2f}ms <= 500ms (PASS)")
        if 'qps' in analysis:
            if analysis['qps'] < 100:
                print(f"❌ {analysis['test_name']}: QPS {analysis['qps']:.2f} < 100 (FAIL)")
            else:
                print(f"✅ {analysis['test_name']}: QPS {analysis['qps']:.2f} >= 100 (PASS)")
    
    results = {
        "test_time": datetime.now().isoformat(),
        "base_url": BASE_URL,
        "analyses": all_analyses
    }
    
    with open("STOCK_SERVICE_TEST_RESULTS.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print(f"\n测试结果已保存到: STOCK_SERVICE_TEST_RESULTS.json")
    
    return 0

if __name__ == "__main__":
    exit(main())