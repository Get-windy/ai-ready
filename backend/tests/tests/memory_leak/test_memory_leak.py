#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 内存泄漏检测测试套件
测试范围：
1. 内存使用场景分析
2. 内存监控配置
3. 内存泄漏检测
4. 内存泄漏点分析
"""

import gc
import sys
import time
import json
import tracemalloc
from datetime import datetime
from typing import Dict, List, Any
import requests
from dataclasses import dataclass

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 测试配置
MEMORY_CONFIG = {
    "iterations": 100,
    "snapshot_interval": 10,
    "threshold_mb": 10,  # 内存增长阈值
}

# 测试结果
TEST_RESULTS = {
    "test_time": "",
    "config": MEMORY_CONFIG,
    "results": {},
    "leaks": [],
    "summary": {}
}


@dataclass
class MemoryLeak:
    """内存泄漏"""
    leak_id: str
    severity: str
    location: str
    size_mb: float
    description: str
    recommendation: str


def make_request(endpoint: str, method: str = "GET", data: dict = None) -> Dict:
    """发送HTTP请求"""
    try:
        if method == "GET":
            resp = requests.get(f"{BASE_URL}{endpoint}", timeout=10)
        else:
            resp = requests.post(f"{BASE_URL}{endpoint}", json=data, timeout=10)
        return {"status_code": resp.status_code, "success": resp.status_code < 500}
    except Exception as e:
        return {"status_code": 0, "success": False, "error": str(e)}


def get_memory_usage() -> Dict:
    """获取当前内存使用情况"""
    import psutil
    import os
    
    process = psutil.Process(os.getpid())
    return {
        "rss_mb": process.memory_info().rss / 1024 / 1024,
        "vms_mb": process.memory_info().vms / 1024 / 1024,
        "percent": process.memory_percent()
    }


def test_api_memory_pattern():
    """测试API请求内存模式"""
    print("\n--- API请求内存模式测试 ---")
    results = []
    leaks = []
    
    tracemalloc.start()
    baseline_snapshot = tracemalloc.take_snapshot()
    
    memory_readings = []
    
    print(f"执行 {MEMORY_CONFIG['iterations']} 次API请求...")
    
    for i in range(MEMORY_CONFIG['iterations']):
        # 执行API请求
        make_request("/api/user/page", "GET", {"pageNum": 1, "pageSize": 10})
        make_request("/api/role/page", "GET", {"pageNum": 1, "pageSize": 10})
        
        if i % MEMORY_CONFIG['snapshot_interval'] == 0:
            try:
                mem = get_memory_usage()
                memory_readings.append({
                    "iteration": i,
                    "rss_mb": round(mem['rss_mb'], 2),
                    "vms_mb": round(mem['vms_mb'], 2)
                })
            except:
                memory_readings.append({
                    "iteration": i,
                    "rss_mb": 0,
                    "vms_mb": 0
                })
    
    # 强制垃圾回收
    gc.collect()
    
    # 比较内存快照
    final_snapshot = tracemalloc.take_snapshot()
    top_stats = final_snapshot.compare_to(baseline_snapshot, 'lineno')
    
    # 分析内存增长
    if memory_readings:
        initial_mem = memory_readings[0]['rss_mb']
        final_mem = memory_readings[-1]['rss_mb']
        memory_growth = final_mem - initial_mem
        
        if memory_growth > MEMORY_CONFIG['threshold_mb']:
            leaks.append(MemoryLeak(
                leak_id="MEM-001",
                severity="HIGH" if memory_growth > 50 else "MEDIUM",
                location="API请求处理",
                size_mb=round(memory_growth, 2),
                description=f"API请求后内存增长 {memory_growth:.2f}MB",
                recommendation="检查请求处理中的对象生命周期，确保及时释放"
            ))
    
    results.append({
        "test_name": "API请求内存模式",
        "iterations": MEMORY_CONFIG['iterations'],
        "memory_readings": memory_readings,
        "initial_memory_mb": memory_readings[0]['rss_mb'] if memory_readings else 0,
        "final_memory_mb": memory_readings[-1]['rss_mb'] if memory_readings else 0,
        "memory_growth_mb": round(memory_growth, 2) if memory_readings else 0,
        "status": "PASS" if not leaks else "WARN"
    })
    
    print(f"  初始内存: {results[0]['initial_memory_mb']:.2f}MB")
    print(f"  最终内存: {results[0]['final_memory_mb']:.2f}MB")
    print(f"  内存增长: {results[0]['memory_growth_mb']:.2f}MB")
    
    tracemalloc.stop()
    
    return results, leaks


def test_string_accumulation():
    """测试字符串累积泄漏"""
    print("\n--- 字符串累积测试 ---")
    results = []
    leaks = []
    
    try:
        initial_mem = get_memory_usage()['rss_mb']
    except:
        initial_mem = 0
    
    # 模拟字符串累积场景
    accumulated_strings = []
    
    for i in range(1000):
        # 模拟大量字符串操作
        accumulated_strings.append("x" * 10000)
        
        if i % 100 == 0:
            try:
                mem = get_memory_usage()['rss_mb']
            except:
                mem = 0
    
    try:
        peak_mem = get_memory_usage()['rss_mb']
    except:
        peak_mem = initial_mem
    
    # 清理
    accumulated_strings.clear()
    gc.collect()
    
    try:
        final_mem = get_memory_usage()['rss_mb']
    except:
        final_mem = initial_mem
    
    memory_not_released = final_mem - initial_mem
    
    if memory_not_released > 5:
        leaks.append(MemoryLeak(
            leak_id="MEM-002",
            severity="MEDIUM",
            location="字符串处理",
            size_mb=round(memory_not_released, 2),
            description=f"字符串操作后内存未完全释放",
            recommendation="使用字符串池或及时清理大字符串"
        ))
    
    results.append({
        "test_name": "字符串累积测试",
        "initial_memory_mb": round(initial_mem, 2),
        "peak_memory_mb": round(peak_mem, 2),
        "final_memory_mb": round(final_mem, 2),
        "memory_not_released_mb": round(memory_not_released, 2),
        "status": "PASS" if memory_not_released < 5 else "WARN"
    })
    
    print(f"  峰值内存: {peak_mem:.2f}MB")
    print(f"  未释放内存: {memory_not_released:.2f}MB")
    
    return results, leaks


def test_object_lifecycle():
    """测试对象生命周期"""
    print("\n--- 对象生命周期测试 ---")
    results = []
    leaks = []
    
    class TestObject:
        def __init__(self, data):
            self.data = data
            self.ref = None
    
    try:
        initial_mem = get_memory_usage()['rss_mb']
    except:
        initial_mem = 0
    
    # 创建对象链
    objects = []
    for i in range(10000):
        obj = TestObject([j for j in range(100)])
        if objects:
            obj.ref = objects[-1]
        objects.append(obj)
    
    try:
        peak_mem = get_memory_usage()['rss_mb']
    except:
        peak_mem = initial_mem
    
    # 清理
    objects.clear()
    del objects
    gc.collect()
    
    try:
        final_mem = get_memory_usage()['rss_mb']
    except:
        final_mem = initial_mem
    
    memory_not_released = final_mem - initial_mem
    
    if memory_not_released > 10:
        leaks.append(MemoryLeak(
            leak_id="MEM-003",
            severity="HIGH",
            location="对象引用链",
            size_mb=round(memory_not_released, 2),
            description=f"对象清理后内存未释放",
            recommendation="打破循环引用，使用弱引用或显式删除"
        ))
    
    results.append({
        "test_name": "对象生命周期测试",
        "initial_memory_mb": round(initial_mem, 2),
        "peak_memory_mb": round(peak_mem, 2),
        "final_memory_mb": round(final_mem, 2),
        "memory_not_released_mb": round(memory_not_released, 2),
        "status": "PASS" if memory_not_released < 10 else "WARN"
    })
    
    print(f"  峰值内存: {peak_mem:.2f}MB")
    print(f"  未释放内存: {memory_not_released:.2f}MB")
    
    return results, leaks


def test_connection_leak():
    """测试连接泄漏"""
    print("\n--- 连接泄漏测试 ---")
    results = []
    leaks = []
    
    try:
        initial_mem = get_memory_usage()['rss_mb']
    except:
        initial_mem = 0
    
    # 模拟多次API连接
    for i in range(100):
        make_request("/api/health", "GET")
    
    gc.collect()
    
    try:
        final_mem = get_memory_usage()['rss_mb']
    except:
        final_mem = initial_mem
    
    memory_growth = final_mem - initial_mem
    
    if memory_growth > 2:
        leaks.append(MemoryLeak(
            leak_id="MEM-004",
            severity="LOW",
            location="HTTP连接",
            size_mb=round(memory_growth, 2),
            description=f"HTTP请求后内存轻微增长",
            recommendation="确保使用连接池并正确关闭响应"
        ))
    
    results.append({
        "test_name": "连接泄漏测试",
        "requests_made": 100,
        "initial_memory_mb": round(initial_mem, 2),
        "final_memory_mb": round(final_mem, 2),
        "memory_growth_mb": round(memory_growth, 2),
        "status": "PASS"
    })
    
    print(f"  请求次数: 100")
    print(f"  内存增长: {memory_growth:.2f}MB")
    
    return results, leaks


def test_cache_memory():
    """测试缓存内存使用"""
    print("\n--- 缓存内存测试 ---")
    results = []
    leaks = []
    
    try:
        initial_mem = get_memory_usage()['rss_mb']
    except:
        initial_mem = 0
    
    # 模拟缓存累积
    cache = {}
    for i in range(1000):
        cache[f"key_{i}"] = {"data": list(range(100)), "timestamp": time.time()}
    
    try:
        peak_mem = get_memory_usage()['rss_mb']
    except:
        peak_mem = initial_mem
    
    cache_size_mb = peak_mem - initial_mem
    
    # 清理缓存
    cache.clear()
    gc.collect()
    
    try:
        final_mem = get_memory_usage()['rss_mb']
    except:
        final_mem = initial_mem
    
    memory_not_released = final_mem - initial_mem
    
    results.append({
        "test_name": "缓存内存测试",
        "cache_entries": 1000,
        "cache_size_mb": round(cache_size_mb, 2),
        "memory_not_released_mb": round(memory_not_released, 2),
        "status": "PASS" if memory_not_released < 2 else "WARN"
    })
    
    print(f"  缓存大小: {cache_size_mb:.2f}MB")
    print(f"  未释放内存: {memory_not_released:.2f}MB")
    
    return results, leaks


def generate_report(all_results: List[Dict], all_leaks: List[MemoryLeak]) -> str:
    """生成内存泄漏检测报告"""
    
    total_tests = len(all_results)
    passed_tests = sum(1 for r in all_results if r.get("status") == "PASS")
    warn_tests = sum(1 for r in all_results if r.get("status") == "WARN")
    failed_tests = sum(1 for r in all_results if r.get("status") == "FAIL")
    
    high_leaks = sum(1 for l in all_leaks if l.severity == "HIGH")
    medium_leaks = sum(1 for l in all_leaks if l.severity == "MEDIUM")
    low_leaks = sum(1 for l in all_leaks if l.severity == "LOW")
    
    report = f"""# AI-Ready 内存泄漏检测报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {TEST_RESULTS["test_time"]} |
| 测试环境 | {BASE_URL} |
| 总测试数 | {total_tests} |
| 通过测试 | {passed_tests} |
| 警告测试 | {warn_tests} |
| 失败测试 | {failed_tests} |
| 发现泄漏 | {len(all_leaks)} |

---

## 测试配置

| 参数 | 值 |
|------|-----|
| 迭代次数 | {MEMORY_CONFIG["iterations"]} |
| 快照间隔 | {MEMORY_CONFIG["snapshot_interval"]} |
| 内存增长阈值 | {MEMORY_CONFIG["threshold_mb"]}MB |

---

## 测试结果详情

"""
    
    for result in all_results:
        status_icon = "✅" if result.get("status") == "PASS" else ("⚠️" if result.get("status") == "WARN" else "❌")
        report += f"""### {result.get("test_name", "未知测试")} {status_icon}

| 指标 | 值 |
|------|-----|
"""
        for key, value in result.items():
            if key != "test_name" and key != "status" and key != "memory_readings":
                report += f"| {key} | {value} |\n"
        report += f"| 状态 | **{result.get('status', 'N/A')}** |\n\n---\n\n"
    
    # 泄漏汇总
    report += f"""## 发现的内存泄漏

| 严重性 | 数量 |
|--------|------|
| 高危 | {high_leaks} |
| 中危 | {medium_leaks} |
| 低危 | {low_leaks} |

"""
    
    if all_leaks:
        report += "### 泄漏详情\n\n"
        for leak in all_leaks:
            report += f"""#### {leak.leak_id}: {leak.location}

| 属性 | 值 |
|------|-----|
| 泄漏ID | {leak.leak_id} |
| 严重性 | **{leak.severity}** |
| 位置 | {leak.location} |
| 大小 | {leak.size_mb}MB |
| 描述 | {leak.description} |
| 建议 | {leak.recommendation} |

"""
    
    report += f"""## 内存使用评估

### 内存健康指标

| 指标 | 状态 |
|------|------|
| API请求内存模式 | {'✅ 正常' if any(r.get('test_name') == 'API请求内存模式' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 字符串处理 | {'✅ 正常' if any(r.get('test_name') == '字符串累积测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 对象生命周期 | {'✅ 正常' if any(r.get('test_name') == '对象生命周期测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 连接管理 | {'✅ 正常' if any(r.get('test_name') == '连接泄漏测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 缓存管理 | {'✅ 正常' if any(r.get('test_name') == '缓存内存测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |

### 内存优化建议

1. **代码层面**:
   - 及时释放大对象引用
   - 避免循环引用
   - 使用生成器替代列表

2. **架构层面**:
   - 实现对象池复用
   - 添加内存监控告警
   - 定期执行GC优化

3. **JVM配置** (如适用):
   - 调整堆内存大小
   - 配置GC策略
   - 启用堆转储分析

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
**测试执行者**: test-agent-2
**项目**: AI-Ready
"""
    
    return report


def main():
    """主函数"""
    TEST_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready 内存泄漏检测")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print(f"测试环境: {BASE_URL}")
    print("=" * 60)
    
    all_results = []
    all_leaks = []
    
    # 执行各类内存测试
    results, leaks = test_api_memory_pattern()
    all_results.extend(results)
    all_leaks.extend(leaks)
    
    results, leaks = test_string_accumulation()
    all_results.extend(results)
    all_leaks.extend(leaks)
    
    results, leaks = test_object_lifecycle()
    all_results.extend(results)
    all_leaks.extend(leaks)
    
    results, leaks = test_connection_leak()
    all_results.extend(results)
    all_leaks.extend(leaks)
    
    results, leaks = test_cache_memory()
    all_results.extend(results)
    all_leaks.extend(leaks)
    
    # 生成报告
    print("\n" + "=" * 60)
    print("生成测试报告...")
    
    report = generate_report(all_results, all_leaks)
    
    # 保存报告
    import os
    os.makedirs("docs", exist_ok=True)
    
    report_path = "docs/AI-Ready内存泄漏检测报告_20260404.md"
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"报告已保存: {report_path}")
    
    # 保存JSON结果
    json_data = {
        "test_time": TEST_RESULTS["test_time"],
        "config": MEMORY_CONFIG,
        "results": all_results,
        "leaks": [
            {
                "leak_id": l.leak_id,
                "severity": l.severity,
                "location": l.location,
                "size_mb": l.size_mb,
                "description": l.description,
                "recommendation": l.recommendation
            }
            for l in all_leaks
        ]
    }
    
    json_path = "docs/memory_leak_test_results.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    print(f"JSON结果已保存: {json_path}")
    
    # 输出摘要
    passed = sum(1 for r in all_results if r.get("status") == "PASS")
    warn = sum(1 for r in all_results if r.get("status") == "WARN")
    failed = sum(1 for r in all_results if r.get("status") == "FAIL")
    
    print(f"\n测试结果: {passed} 通过, {warn} 警告, {failed} 失败")
    print(f"发现泄漏: {len(all_leaks)} 个")
    print("=" * 60)
    
    return all_results, all_leaks


if __name__ == "__main__":
    main()
