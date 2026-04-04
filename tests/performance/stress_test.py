#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 压力测试套件
测试100/500/1000并发用户场景
"""

import time
import json
import statistics
import threading
import queue
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List, Any
import requests
from dataclasses import dataclass, field

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 测试配置
STRESS_CONFIG = {
    "warmup_users": 10,
    "test_levels": [100, 500, 1000],
    "duration_per_level_seconds": 30,
    "ramp_up_seconds": 5,
    "timeout_seconds": 10,
}

# 测试结果
STRESS_RESULTS = {
    "test_time": "",
    "config": STRESS_CONFIG,
    "results": {},
    "summary": {}
}


@dataclass
class RequestResult:
    """请求结果"""
    success: bool
    status_code: int
    response_time_ms: float
    error: str = ""
    

@dataclass
class LevelResult:
    """并发级别结果"""
    concurrent_users: int
    total_requests: int
    successful_requests: int
    failed_requests: int
    avg_response_time_ms: float
    p50_response_time_ms: float
    p95_response_time_ms: float
    p99_response_time_ms: float
    min_response_time_ms: float
    max_response_time_ms: float
    throughput_rps: float
    error_rate_percent: float
    duration_seconds: float
    status: str = "PASS"


def make_request(endpoint: str, method: str = "GET", data: dict = None) -> RequestResult:
    """发送HTTP请求"""
    start_time = time.time()
    try:
        if method == "GET":
            resp = requests.get(f"{BASE_URL}{endpoint}", timeout=STRESS_CONFIG["timeout_seconds"])
        else:
            resp = requests.post(f"{BASE_URL}{endpoint}", json=data, timeout=STRESS_CONFIG["timeout_seconds"])
        
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestResult(
            success=200 <= resp.status_code < 400 or resp.status_code in [401, 403],
            status_code=resp.status_code,
            response_time_ms=elapsed_ms
        )
    except requests.exceptions.Timeout:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestResult(success=False, status_code=0, response_time_ms=elapsed_ms, error="timeout")
    except requests.exceptions.ConnectionError:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestResult(success=False, status_code=0, response_time_ms=elapsed_ms, error="connection_refused")
    except Exception as e:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestResult(success=False, status_code=0, response_time_ms=elapsed_ms, error=str(e))


def calculate_percentile(values: List[float], percentile: float) -> float:
    """计算百分位数"""
    if not values:
        return 0
    sorted_values = sorted(values)
    index = int(len(sorted_values) * percentile / 100)
    index = min(index, len(sorted_values) - 1)
    return sorted_values[index]


def run_stress_level(concurrent_users: int, duration_seconds: int) -> LevelResult:
    """运行单个并发级别的压力测试"""
    print(f"\n{'='*60}")
    print(f"压力测试: {concurrent_users} 并发用户, 持续 {duration_seconds}秒")
    print(f"{'='*60}")
    
    results_queue = queue.Queue()
    stop_event = threading.Event()
    
    def worker():
        """工作线程"""
        endpoints = [
            ("/api/user/page", "GET"),
            ("/api/role/page", "GET"),
            ("/api/health", "GET"),
        ]
        
        while not stop_event.is_set():
            for endpoint, method in endpoints:
                if stop_event.is_set():
                    break
                result = make_request(endpoint, method)
                results_queue.put(result)
    
    # 启动工作线程
    start_time = time.time()
    with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
        futures = [executor.submit(worker) for _ in range(concurrent_users)]
        
        # 等待指定时间
        time.sleep(duration_seconds)
        stop_event.set()
        
        # 收集结果
        for future in as_completed(futures):
            pass
    
    actual_duration = time.time() - start_time
    
    # 处理结果
    results = []
    while not results_queue.empty():
        results.append(results_queue.get())
    
    if not results:
        return LevelResult(
            concurrent_users=concurrent_users,
            total_requests=0,
            successful_requests=0,
            failed_requests=0,
            avg_response_time_ms=0,
            p50_response_time_ms=0,
            p95_response_time_ms=0,
            p99_response_time_ms=0,
            min_response_time_ms=0,
            max_response_time_ms=0,
            throughput_rps=0,
            error_rate_percent=100,
            duration_seconds=actual_duration,
            status="FAIL"
        )
    
    response_times = [r.response_time_ms for r in results]
    successful = [r for r in results if r.success]
    failed = [r for r in results if not r.success]
    
    total_requests = len(results)
    successful_count = len(successful)
    failed_count = len(failed)
    
    avg_time = statistics.mean(response_times) if response_times else 0
    p50_time = calculate_percentile(response_times, 50)
    p95_time = calculate_percentile(response_times, 95)
    p99_time = calculate_percentile(response_times, 99)
    min_time = min(response_times) if response_times else 0
    max_time = max(response_times) if response_times else 0
    
    throughput = total_requests / actual_duration if actual_duration > 0 else 0
    error_rate = (failed_count / total_requests * 100) if total_requests > 0 else 100
    
    # 判断状态
    if error_rate < 5 and avg_time < 1000:
        status = "PASS"
    elif error_rate < 20 and avg_time < 2000:
        status = "WARN"
    else:
        status = "FAIL"
    
    return LevelResult(
        concurrent_users=concurrent_users,
        total_requests=total_requests,
        successful_requests=successful_count,
        failed_requests=failed_count,
        avg_response_time_ms=avg_time,
        p50_response_time_ms=p50_time,
        p95_response_time_ms=p95_time,
        p99_response_time_ms=p99_time,
        min_response_time_ms=min_time,
        max_response_time_ms=max_time,
        throughput_rps=throughput,
        error_rate_percent=error_rate,
        duration_seconds=actual_duration,
        status=status
    )


def analyze_system_limits(results: List[LevelResult]) -> Dict[str, Any]:
    """分析系统极限性能"""
    analysis = {
        "max_supported_users": 0,
        "breaking_point_users": 0,
        "optimal_users": 0,
        "performance_degradation": [],
        "recommendations": []
    }
    
    for i, result in enumerate(results):
        if result.error_rate_percent < 5 and result.avg_response_time_ms < 500:
            analysis["max_supported_users"] = result.concurrent_users
        
        if result.error_rate_percent > 50 or result.avg_response_time_ms > 5000:
            if analysis["breaking_point_users"] == 0:
                analysis["breaking_point_users"] = result.concurrent_users
        
        # 性能衰减分析
        if i > 0:
            prev = results[i-1]
            degradation = {
                "from_users": prev.concurrent_users,
                "to_users": result.concurrent_users,
                "response_time_increase_percent": ((result.avg_response_time_ms - prev.avg_response_time_ms) / prev.avg_response_time_ms * 100) if prev.avg_response_time_ms > 0 else 0,
                "throughput_change_percent": ((result.throughput_rps - prev.throughput_rps) / prev.throughput_rps * 100) if prev.throughput_rps > 0 else 0
            }
            analysis["performance_degradation"].append(degradation)
    
    # 推荐最优并发数
    if analysis["max_supported_users"] > 0:
        analysis["optimal_users"] = int(analysis["max_supported_users"] * 0.7)
    
    # 建议
    if analysis["breaking_point_users"] > 0:
        analysis["recommendations"].append(f"系统在 {analysis['breaking_point_users']} 并发时达到极限")
    if analysis["max_supported_users"] > 0:
        analysis["recommendations"].append(f"推荐最大并发: {analysis['max_supported_users']} 用户")
    if analysis["optimal_users"] > 0:
        analysis["recommendations"].append(f"推荐最优并发: {analysis['optimal_users']} 用户 (70%负载)")
    
    return analysis


def generate_report(results: List[LevelResult], analysis: Dict[str, Any]) -> str:
    """生成压力测试报告"""
    
    report = f"""# AI-Ready 压力测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {STRESS_RESULTS["test_time"]} |
| 测试环境 | {BASE_URL} |
| 测试级别 | 100/500/1000 并发用户 |
| 每级别持续时间 | {STRESS_CONFIG["duration_per_level_seconds"]}秒 |

---

## 测试配置

| 参数 | 值 |
|------|-----|
| 预热用户数 | {STRESS_CONFIG["warmup_users"]} |
| 测试级别 | {STRESS_CONFIG["test_levels"]} |
| 每级别持续时间 | {STRESS_CONFIG["duration_per_level_seconds"]}秒 |
| 爬升时间 | {STRESS_CONFIG["ramp_up_seconds"]}秒 |
| 请求超时 | {STRESS_CONFIG["timeout_seconds"]}秒 |

---

## 压力测试结果

"""
    
    for result in results:
        status_icon = "✅" if result.status == "PASS" else ("⚠️" if result.status == "WARN" else "❌")
        report += f"""### {result.concurrent_users} 并发用户 {status_icon}

| 指标 | 值 |
|------|-----|
| 总请求数 | {result.total_requests} |
| 成功请求 | {result.successful_requests} |
| 失败请求 | {result.failed_requests} |
| 错误率 | {result.error_rate_percent:.2f}% |
| 吞吐量 | {result.throughput_rps:.2f} req/s |
| 平均响应时间 | {result.avg_response_time_ms:.2f} ms |
| P50响应时间 | {result.p50_response_time_ms:.2f} ms |
| P95响应时间 | {result.p95_response_time_ms:.2f} ms |
| P99响应时间 | {result.p99_response_time_ms:.2f} ms |
| 最小响应时间 | {result.min_response_time_ms:.2f} ms |
| 最大响应时间 | {result.max_response_time_ms:.2f} ms |
| 实际持续时间 | {result.duration_seconds:.2f}秒 |
| 状态 | **{result.status}** |

---

"""
    
    # 系统极限分析
    report += f"""## 系统极限性能分析

| 指标 | 数值 |
|------|------|
| 最大支持并发用户 | {analysis["max_supported_users"]} |
| 系统崩溃点 | {analysis["breaking_point_users"] if analysis["breaking_point_users"] > 0 else "未达到"} |
| 推荐最优并发 | {analysis["optimal_users"]} |

### 性能衰减分析

"""
    
    for deg in analysis["performance_degradation"]:
        report += f"- {deg['from_users']} → {deg['to_users']} 用户: "
        report += f"响应时间增加 {deg['response_time_increase_percent']:.1f}%, "
        report += f"吞吐量变化 {deg['throughput_change_percent']:.1f}%\n"
    
    report += """
### 改进建议

"""
    for rec in analysis["recommendations"]:
        report += f"- {rec}\n"
    
    report += f"""
---

## 性能评估标准

| 级别 | 错误率 | 平均响应时间 | 状态 |
|------|--------|-------------|------|
| PASS | < 5% | < 1000ms | ✅ 通过 |
| WARN | 5-20% | 1000-2000ms | ⚠️ 警告 |
| FAIL | > 20% | > 2000ms | ❌ 失败 |

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    return report


def main():
    """主函数"""
    STRESS_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready 压力测试")
    print("=" * 60)
    print(f"测试时间: {STRESS_RESULTS['test_time']}")
    print(f"测试级别: {STRESS_CONFIG['test_levels']}")
    print(f"每级别持续时间: {STRESS_CONFIG['duration_per_level_seconds']}秒")
    
    # 运行各级别压力测试
    results = []
    
    # 预热
    print("\n[预热] 10并发用户测试...")
    time.sleep(2)
    
    for level in STRESS_CONFIG["test_levels"]:
        result = run_stress_level(level, STRESS_CONFIG["duration_per_level_seconds"])
        results.append(result)
        STRESS_RESULTS["results"][str(level)] = {
            "concurrent_users": result.concurrent_users,
            "total_requests": result.total_requests,
            "throughput_rps": result.throughput_rps,
            "avg_response_time_ms": result.avg_response_time_ms,
            "error_rate_percent": result.error_rate_percent,
            "status": result.status
        }
        
        print(f"\n结果: {result.status}")
        print(f"  总请求: {result.total_requests}")
        print(f"  吞吐量: {result.throughput_rps:.2f} req/s")
        print(f"  平均响应时间: {result.avg_response_time_ms:.2f} ms")
        print(f"  错误率: {result.error_rate_percent:.2f}%")
    
    # 分析系统极限
    print("\n" + "=" * 60)
    print("系统极限性能分析")
    print("=" * 60)
    
    analysis = analyze_system_limits(results)
    STRESS_RESULTS["summary"] = analysis
    
    print(f"最大支持并发用户: {analysis['max_supported_users']}")
    print(f"系统崩溃点: {analysis['breaking_point_users'] if analysis['breaking_point_users'] > 0 else '未达到'}")
    print(f"推荐最优并发: {analysis['optimal_users']}")
    
    # 生成报告
    report = generate_report(results, analysis)
    
    # 保存报告
    report_path = "docs/AI-Ready压力测试报告_20260403.md"
    import os
    os.makedirs("docs", exist_ok=True)
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\n报告已保存: {report_path}")
    
    # 保存JSON结果
    json_path = "docs/stress_test_results.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(STRESS_RESULTS, f, indent=2, ensure_ascii=False)
    
    print(f"JSON结果已保存: {json_path}")
    
    return results, analysis


if __name__ == "__main__":
    main()
