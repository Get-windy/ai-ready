#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
订单管理服务性能测试脚本
"""

import requests
import time
import concurrent.futures
import json
from datetime import datetime

BASE_URL = "http://localhost:8082"

def test_create_order():
    """创建订单测试"""
    url = f"{BASE_URL}/api/orders"
    payload = {
        "userId": 1001,
        "items": [
            {"skuId": "SKU001", "quantity": 2, "price": 99.99},
            {"skuId": "SKU002", "quantity": 1, "price": 199.99}
        ],
        "totalAmount": 399.97,
        "receiverInfo": {
            "name": "测试用户",
            "phone": "13800138000",
            "address": "北京市朝阳区测试路1号"
        }
    }
    
    start_time = time.time()
    try:
        response = requests.post(url, json=payload, timeout=10)
        elapsed = (time.time() - start_time) * 1000
        return {
            "status_code": response.status_code,
            "elapsed_ms": elapsed,
            "data": response.json() if response.status_code in [200, 201] else None
        }
    except Exception as e:
        return {
            "status_code": 0,
            "elapsed_ms": (time.time() - start_time) * 1000,
            "error": str(e)
        }

def test_get_order(order_id):
    """获取订单测试"""
    url = f"{BASE_URL}/api/orders/{order_id}"
    
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

def test_update_order(order_id):
    """更新订单测试"""
    url = f"{BASE_URL}/api/orders/{order_id}"
    payload = {
        "status": "CONFIRMED",
        "comment": "性能测试更新"
    }
    
    start_time = time.time()
    try:
        response = requests.put(url, json=payload, timeout=10)
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
    print("订单管理服务性能测试")
    print("="*60)
    print(f"测试环境: {BASE_URL}")
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    print("\n[测试1] 创建订单性能测试")
    results_create = run_concurrent_test(test_create_order, concurrency=50, iterations=500)
    analysis_create = analyze_results(results_create, "创建订单")
    print_results(analysis_create)
    
    print("\n[测试2] 获取订单性能测试")
    try:
        test_create = test_create_order()
        if test_create["status_code"] in [200, 201]:
            order_id = test_create["data"].get("id") or test_create["data"].get("orderId")
            results_get = run_concurrent_test(test_get_order, concurrency=30, iterations=300, order_id=order_id)
            analysis_get = analyze_results(results_get, "获取订单")
            print_results(analysis_get)
        else:
            print("创建订单失败，跳过获取订单测试")
    except Exception as e:
        print(f"获取订单测试出错: {e}")
    
    print("\n[测试3] 更新订单性能测试")
    try:
        test_create = test_create_order()
        if test_create["status_code"] in [200, 201]:
            order_id = test_create["data"].get("id") or test_create["data"].get("orderId")
            results_update = run_concurrent_test(test_update_order, concurrency=20, iterations=200, order_id=order_id)
            analysis_update = analyze_results(results_update, "更新订单")
            print_results(analysis_update)
        else:
            print("创建订单失败，跳过更新订单测试")
    except Exception as e:
        print(f"更新订单测试出错: {e}")
    
    print("\n" + "="*60)
    print("测试汇总")
    print("="*60)
    
    all_analyses = [analysis_create]
    
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
    
    with open("ORDER_SERVICE_TEST_RESULTS.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print(f"\n测试结果已保存到: ORDER_SERVICE_TEST_RESULTS.json")
    
    return 0

if __name__ == "__main__":
    exit(main())
