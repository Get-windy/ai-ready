#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 接口压力测试脚本
"""

import requests
import time
import statistics
import json
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from dataclasses import dataclass, field, asdict
from typing import Dict, List, Any
import os
import sys
import random

# 配置
BASE_URL = "http://localhost:8080"

# 测试配置
TEST_CONFIG = {
    "base_url": BASE_URL,
    "timeout_seconds": 10,
    "test_scenarios": [
        {"name": "baseline", "concurrent_users": 1, "requests_per_user": 20},
        {"name": "low_concurrent", "concurrent_users": 50, "requests_per_user": 10},
        {"name": "medium_concurrent", "concurrent_users": 100, "requests_per_user": 10},
        {"name": "high_concurrent", "concurrent_users": 200, "requests_per_user": 5},
        {"name": "stress_test", "concurrent_users": 500, "requests_per_user": 3},
    ]
}

# API端点
API_ENDPOINTS = [
    {"path": "/", "method": "GET", "name": "root"},
    {"path": "/api/user/page", "method": "GET", "name": "user_page"},
    {"path": "/api/role/page", "method": "GET", "name": "role_page"},
    {"path": "/api/health", "method": "GET", "name": "health"},
]


@dataclass
class RequestResult:
    endpoint: str
    success: bool
    status_code: int
    response_time_ms: float
    error: str = ""


@dataclass
class ScenarioResult:
    scenario_name: str
    concurrent_users: int
    total_requests: int
    successful_requests: int
    failed_requests: int
    avg_response_time_ms: float
    min_response_time_ms: float
    max_response_time_ms: float
    p50_response_time_ms: float
    p95_response_time_ms: float
    p99_response_time_ms: float
    throughput_rps: float
    error_rate_percent: float
    duration_seconds: float
    status: str = "PASS"


class PerformanceMetrics:
    def __init__(self):
        self.results: List[RequestResult] = []
        self.lock = threading.Lock()
    
    def record(self, result: RequestResult):
        with self.lock:
            self.results.append(result)
    
    def get_summary(self) -> Dict[str, Any]:
        if not self.results:
            return {"error": "No data"}
        
        times = [r.response_time_ms for r in self.results]
        successes = sum(1 for r in self.results if r.success)
        failures = len(times) - successes
        sorted_times = sorted(times)
        total_time = sum(times) / 1000
        
        return {
            "total_requests": len(times),
            "successful_requests": successes,
            "failed_requests": failures,
            "avg_response_time_ms": statistics.mean(times),
            "min_response_time_ms": min(times),
            "max_response_time_ms": max(times),
            "p50_response_time_ms": sorted_times[int(len(sorted_times) * 0.50)],
            "p95_response_time_ms": sorted_times[int(len(sorted_times) * 0.95)],
            "p99_response_time_ms": sorted_times[int(len(sorted_times) * 0.99)],
            "throughput_rps": len(times) / total_time if total_time > 0 else 0,
            "error_rate_percent": failures / len(times) * 100,
        }


def make_request(endpoint: Dict, timeout: int = 10) -> RequestResult:
    url = f"{BASE_URL}{endpoint['path']}"
    name = endpoint.get('name', endpoint['path'])
    
    start_time = time.perf_counter()
    try:
        resp = requests.get(url, timeout=timeout)
        elapsed = (time.perf_counter() - start_time) * 1000
        success = 200 <= resp.status_code < 400 or resp.status_code in [401, 403]
        return RequestResult(endpoint=name, success=success, status_code=resp.status_code, response_time_ms=elapsed)
    except requests.exceptions.Timeout:
        elapsed = (time.perf_counter() - start_time) * 1000
        return RequestResult(endpoint=name, success=False, status_code=0, response_time_ms=elapsed, error="timeout")
    except requests.exceptions.ConnectionError:
        elapsed = (time.perf_counter() - start_time) * 1000
        return RequestResult(endpoint=name, success=False, status_code=0, response_time_ms=elapsed, error="connection_refused")
    except Exception as e:
        elapsed = (time.perf_counter() - start_time) * 1000
        return RequestResult(endpoint=name, success=False, status_code=0, response_time_ms=elapsed, error=str(e))


def run_scenario(scenario: Dict) -> ScenarioResult:
    name = scenario["name"]
    users = scenario["concurrent_users"]
    requests_per_user = scenario["requests_per_user"]
    
    print(f"\n{'='*60}")
    print(f"Scenario: {name}")
    print(f"Concurrent users: {users}, Requests per user: {requests_per_user}")
    print(f"{'='*60}")
    
    metrics = PerformanceMetrics()
    start_time = time.time()
    
    def worker():
        for _ in range(requests_per_user):
            endpoint = random.choice(API_ENDPOINTS)
            result = make_request(endpoint, TEST_CONFIG["timeout_seconds"])
            metrics.record(result)
    
    with ThreadPoolExecutor(max_workers=users) as executor:
        futures = [executor.submit(worker) for _ in range(users)]
        for future in as_completed(futures):
            pass
    
    duration = time.time() - start_time
    summary = metrics.get_summary()
    
    error_rate = summary["error_rate_percent"]
    avg_time = summary["avg_response_time_ms"]
    
    if error_rate < 5 and avg_time < 500:
        status = "PASS"
    elif error_rate < 20 and avg_time < 1000:
        status = "WARN"
    else:
        status = "FAIL"
    
    result = ScenarioResult(
        scenario_name=name,
        concurrent_users=users,
        total_requests=summary["total_requests"],
        successful_requests=summary["successful_requests"],
        failed_requests=summary["failed_requests"],
        avg_response_time_ms=summary["avg_response_time_ms"],
        min_response_time_ms=summary["min_response_time_ms"],
        max_response_time_ms=summary["max_response_time_ms"],
        p50_response_time_ms=summary["p50_response_time_ms"],
        p95_response_time_ms=summary["p95_response_time_ms"],
        p99_response_time_ms=summary["p99_response_time_ms"],
        throughput_rps=summary["throughput_rps"],
        error_rate_percent=summary["error_rate_percent"],
        duration_seconds=duration,
        status=status
    )
    
    print(f"\nResult: [{status}]")
    print(f"  Total requests: {result.total_requests}")
    print(f"  Success/Failure: {result.successful_requests}/{result.failed_requests}")
    print(f"  Avg response time: {result.avg_response_time_ms:.2f}ms")
    print(f"  P95 response time: {result.p95_response_time_ms:.2f}ms")
    print(f"  Throughput: {result.throughput_rps:.2f} req/s")
    print(f"  Error rate: {result.error_rate_percent:.2f}%")
    print(f"  Duration: {result.duration_seconds:.2f}s")
    
    return result


def analyze_performance(results: List[ScenarioResult]) -> Dict[str, Any]:
    analysis = {
        "overall_status": "PASS",
        "max_supported_concurrent": 0,
        "performance_degradation": [],
        "bottlenecks": [],
        "recommendations": []
    }
    
    for result in results:
        if result.status == "PASS":
            analysis["max_supported_concurrent"] = max(analysis["max_supported_concurrent"], result.concurrent_users)
        if result.status == "FAIL":
            analysis["overall_status"] = "WARN"
    
    baseline = results[0] if results else None
    if baseline:
        for result in results[1:]:
            degradation = {
                "scenario": result.scenario_name,
                "response_time_increase_percent": ((result.avg_response_time_ms - baseline.avg_response_time_ms) / baseline.avg_response_time_ms * 100) if baseline.avg_response_time_ms > 0 else 0,
            }
            analysis["performance_degradation"].append(degradation)
    
    for result in results:
        if result.avg_response_time_ms > 500:
            analysis["bottlenecks"].append(f"{result.scenario_name}: avg response time {result.avg_response_time_ms:.0f}ms exceeds 500ms threshold")
        if result.error_rate_percent > 5:
            analysis["bottlenecks"].append(f"{result.scenario_name}: error rate {result.error_rate_percent:.1f}% exceeds 5% threshold")
    
    if analysis["max_supported_concurrent"] >= 200:
        analysis["recommendations"].append("System performance is excellent, supports 200+ concurrent users")
    elif analysis["max_supported_concurrent"] >= 100:
        analysis["recommendations"].append("System performance is good, optimize for higher concurrency")
    else:
        analysis["recommendations"].append("System performance needs optimization")
    
    return analysis


def generate_report(results: List[ScenarioResult], analysis: Dict[str, Any], output_dir: str) -> str:
    test_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    report = f"""# AI-Ready API Pressure Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | {test_time} |
| Test Environment | {BASE_URL} |
| Total Scenarios | {len(results)} |
| Overall Status | {analysis['overall_status']} |

---

## Test Configuration

### Test Scenarios

| Scenario | Concurrent Users | Requests Per User |
|----------|------------------|-------------------|
"""
    
    for scenario in TEST_CONFIG["test_scenarios"]:
        report += f"| {scenario['name']} | {scenario['concurrent_users']} | {scenario['requests_per_user']} |\n"
    
    report += """
### API Endpoints

| Endpoint | Method | Name |
|----------|--------|------|
"""
    
    for ep in API_ENDPOINTS:
        report += f"| {ep['path']} | {ep['method']} | {ep['name']} |\n"
    
    report += """
---

## Test Results

### Summary

| Scenario | Concurrent | Total Req | Success Rate | Avg Response | P95 Response | Throughput | Error Rate | Status |
|----------|------------|-----------|--------------|--------------|--------------|------------|------------|--------|
"""
    
    for result in results:
        success_rate = 100 - result.error_rate_percent
        report += f"| {result.scenario_name} | {result.concurrent_users} | {result.total_requests} | "
        report += f"{success_rate:.1f}% | {result.avg_response_time_ms:.1f}ms | {result.p95_response_time_ms:.1f}ms | "
        report += f"{result.throughput_rps:.1f}r/s | {result.error_rate_percent:.1f}% | {result.status} |\n"
    
    report += """
### Detailed Results

"""
    
    for result in results:
        report += f"""#### {result.scenario_name} [{result.status}]

| Metric | Value |
|--------|-------|
| Concurrent Users | {result.concurrent_users} |
| Total Requests | {result.total_requests} |
| Successful Requests | {result.successful_requests} |
| Failed Requests | {result.failed_requests} |
| Success Rate | {100 - result.error_rate_percent:.2f}% |
| Error Rate | {result.error_rate_percent:.2f}% |
| Avg Response Time | {result.avg_response_time_ms:.2f} ms |
| Min Response Time | {result.min_response_time_ms:.2f} ms |
| Max Response Time | {result.max_response_time_ms:.2f} ms |
| P50 Response Time | {result.p50_response_time_ms:.2f} ms |
| P95 Response Time | {result.p95_response_time_ms:.2f} ms |
| P99 Response Time | {result.p99_response_time_ms:.2f} ms |
| Throughput | {result.throughput_rps:.2f} req/s |
| Duration | {result.duration_seconds:.2f} s |

---

"""
    
    report += f"""## Performance Analysis

### System Capacity

| Metric | Value |
|--------|-------|
| Max Supported Concurrent Users | {analysis['max_supported_concurrent']} |
| Overall Status | {analysis['overall_status']} |

### Performance Degradation

| Scenario | Response Time Increase |
|----------|----------------------|
"""
    
    for deg in analysis["performance_degradation"]:
        report += f"| {deg['scenario']} | {deg['response_time_increase_percent']:+.1f}% |\n"
    
    if analysis["bottlenecks"]:
        report += "\n### Identified Bottlenecks\n\n"
        for bottleneck in analysis["bottlenecks"]:
            report += f"- {bottleneck}\n"
    
    report += """
### Recommendations

"""
    for rec in analysis["recommendations"]:
        report += f"- {rec}\n"
    
    report += f"""
---

## Performance Benchmarks

| Level | Avg Response Time | Error Rate | Description |
|-------|-------------------|------------|-------------|
| Excellent | < 100ms | < 1% | Excellent performance |
| Good | < 300ms | < 5% | Good performance |
| Fair | < 500ms | < 10% | Fair performance, needs attention |
| Poor | < 1000ms | < 20% | Poor performance, needs optimization |
| Critical | >= 1000ms | >= 20% | Critical, urgent optimization needed |

---

## Conclusion

"""
    
    if analysis["overall_status"] == "PASS":
        report += "**System performance is good**, all test scenarios passed performance benchmarks.\n\n"
    else:
        report += "**System needs attention**, some test scenarios did not meet performance benchmarks.\n\n"
    
    report += f"""- Max supported concurrent users: **{analysis['max_supported_concurrent']}**
- Recommended production concurrency: **{int(analysis['max_supported_concurrent'] * 0.7)}** (70% load)

---

**Report Generated**: {test_time}
**Test Tool**: Python + requests + ThreadPoolExecutor
"""
    
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, f"API_PRESSURE_TEST_REPORT_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nReport saved: {report_path}")
    
    json_path = os.path.join(output_dir, f"api_pressure_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    json_results = {
        "test_time": test_time,
        "config": TEST_CONFIG,
        "results": [asdict(r) for r in results],
        "analysis": analysis
    }
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_results, f, indent=2, ensure_ascii=False, default=str)
    
    print(f"JSON results saved: {json_path}")
    
    return report_path


def main():
    print("=" * 60)
    print("AI-Ready API Pressure Test")
    print("=" * 60)
    print(f"Test Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Target Server: {BASE_URL}")
    
    print("\n[Pre-check] Checking service availability...")
    try:
        resp = requests.get(f"{BASE_URL}/", timeout=5)
        print(f"Service Status: HTTP {resp.status_code}")
    except Exception as e:
        print(f"Warning: Service connection issue - {e}")
    
    results = []
    for scenario in TEST_CONFIG["test_scenarios"]:
        result = run_scenario(scenario)
        results.append(result)
    
    print("\n" + "=" * 60)
    print("Performance Analysis")
    print("=" * 60)
    
    analysis = analyze_performance(results)
    
    print(f"Overall Status: {analysis['overall_status']}")
    print(f"Max Supported Concurrent: {analysis['max_supported_concurrent']}")
    
    if analysis['bottlenecks']:
        print("\nIdentified Bottlenecks:")
        for b in analysis['bottlenecks']:
            print(f"  - {b}")
    
    output_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "docs")
    generate_report(results, analysis, output_dir)
    
    print("\n" + "=" * 60)
    print("Test Complete")
    print("=" * 60)
    
    return results, analysis


if __name__ == "__main__":
    main()
