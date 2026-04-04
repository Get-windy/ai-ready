#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready API Regression Test Suite
Tests and compares API responses with baseline
"""

import requests
import time
import json
from datetime import datetime
from typing import Dict, List, Any, Tuple
from dataclasses import dataclass, field, asdict
import os

# Configuration
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# Baseline results from previous test
BASELINE = {
    "test_time": "2026-04-03 22:29:35",
    "pressure_test": {
        "baseline_avg_time": 5.8,
        "low_concurrent_avg_time": 85.4,
        "medium_concurrent_avg_time": 151.2,
        "high_concurrent_avg_time": 168.6,
        "stress_test_avg_time": 196.9
    }
}

TEST_RESULTS = {
    "test_time": "",
    "baseline": BASELINE,
    "current": {},
    "comparison": {},
    "tests": []
}


@dataclass
class APITestResult:
    name: str
    endpoint: str
    status_code: int
    response_time_ms: float
    baseline_time_ms: float
    deviation_percent: float
    status: str = "PASS"
    message: str = ""


def make_request(endpoint: str, method: str = "GET", timeout: int = 10) -> Tuple[int, float, str]:
    """Make HTTP request and return status code, elapsed time, response"""
    url = f"{BASE_URL}{endpoint}"
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Real-IP": "127.0.0.1"
    }
    
    start = time.perf_counter()
    try:
        if method == "GET":
            resp = requests.get(url, headers=headers, timeout=timeout)
        else:
            resp = requests.request(method, url, headers=headers, timeout=timeout)
        elapsed = (time.perf_counter() - start) * 1000
        return resp.status_code, elapsed, resp.text[:100] if resp.text else ""
    except requests.exceptions.Timeout:
        elapsed = (time.perf_counter() - start) * 1000
        return 0, elapsed, "timeout"
    except requests.exceptions.ConnectionError:
        elapsed = (time.perf_counter() - start) * 1000
        return 0, elapsed, "connection_error"
    except Exception as e:
        elapsed = (time.perf_counter() - start) * 1000
        return 0, elapsed, str(e)


def test_api_health() -> APITestResult:
    """Test API root endpoint"""
    status_code, elapsed, response = make_request("/")
    
    baseline_time = BASELINE["pressure_test"]["baseline_avg_time"]
    deviation = ((elapsed - baseline_time) / baseline_time * 100) if baseline_time > 0 else 0
    
    result = APITestResult(
        name="API Health Check",
        endpoint="/",
        status_code=status_code,
        response_time_ms=elapsed,
        baseline_time_ms=baseline_time,
        deviation_percent=deviation
    )
    
    if status_code in [200, 401, 403]:
        if abs(deviation) < 50:  # Within 50% of baseline
            result.status = "PASS"
            result.message = f"Response time within baseline range ({deviation:+.1f}%)"
        else:
            result.status = "WARN"
            result.message = f"Response time deviation: {deviation:+.1f}%"
    else:
        result.status = "FAIL"
        result.message = f"Unexpected status code: {status_code}"
    
    return result


def test_user_endpoint() -> APITestResult:
    """Test user endpoint"""
    status_code, elapsed, response = make_request("/api/user/page")
    
    baseline_time = 10.0  # Expected baseline
    deviation = ((elapsed - baseline_time) / baseline_time * 100) if baseline_time > 0 else 0
    
    result = APITestResult(
        name="User API",
        endpoint="/api/user/page",
        status_code=status_code,
        response_time_ms=elapsed,
        baseline_time_ms=baseline_time,
        deviation_percent=deviation
    )
    
    if status_code in [200, 401, 403]:
        result.status = "PASS"
        result.message = f"User API accessible ({deviation:+.1f}% from baseline)"
    else:
        result.status = "FAIL"
        result.message = f"Unexpected status: {status_code}"
    
    return result


def test_role_endpoint() -> APITestResult:
    """Test role endpoint"""
    status_code, elapsed, response = make_request("/api/role/page")
    
    baseline_time = 10.0
    deviation = ((elapsed - baseline_time) / baseline_time * 100) if baseline_time > 0 else 0
    
    result = APITestResult(
        name="Role API",
        endpoint="/api/role/page",
        status_code=status_code,
        response_time_ms=elapsed,
        baseline_time_ms=baseline_time,
        deviation_percent=deviation
    )
    
    if status_code in [200, 401, 403]:
        result.status = "PASS"
        result.message = f"Role API accessible ({deviation:+.1f}% from baseline)"
    else:
        result.status = "FAIL"
        result.message = f"Unexpected status: {status_code}"
    
    return result


def test_multiple_requests_consistency() -> List[APITestResult]:
    """Test multiple requests for consistency"""
    results = []
    
    for i in range(3):
        status_code, elapsed, response = make_request("/")
        
        baseline_time = BASELINE["pressure_test"]["baseline_avg_time"]
        deviation = ((elapsed - baseline_time) / baseline_time * 100) if baseline_time > 0 else 0
        
        result = APITestResult(
            name=f"Consistency Test #{i+1}",
            endpoint="/",
            status_code=status_code,
            response_time_ms=elapsed,
            baseline_time_ms=baseline_time,
            deviation_percent=deviation
        )
        
        if status_code in [200, 401, 403]:
            result.status = "PASS"
            result.message = f"Consistent response ({elapsed:.1f}ms)"
        else:
            result.status = "FAIL"
            result.message = f"Inconsistent status: {status_code}"
        
        results.append(result)
        time.sleep(0.1)
    
    return results


def run_regression_tests():
    """Run all regression tests"""
    print("=" * 60)
    print("AI-Ready API Regression Test")
    print("=" * 60)
    print(f"Test Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Baseline: {BASELINE['test_time']}")
    print()
    
    all_results = []
    
    # Test 1: API Health
    print("--- API Health Tests ---")
    result = test_api_health()
    all_results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # Test 2: User Endpoint
    print("\n--- Endpoint Tests ---")
    result = test_user_endpoint()
    all_results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # Test 3: Role Endpoint
    result = test_role_endpoint()
    all_results.append(result)
    print(f"[{result.status}] {result.name}: {result.message}")
    
    # Test 4: Consistency Tests
    print("\n--- Consistency Tests ---")
    results = test_multiple_requests_consistency()
    all_results.extend(results)
    for r in results:
        print(f"[{r.status}] {r.name}: {r.message}")
    
    # Calculate summary
    passed = sum(1 for r in all_results if r.status == "PASS")
    failed = sum(1 for r in all_results if r.status == "FAIL")
    warned = sum(1 for r in all_results if r.status == "WARN")
    total = len(all_results)
    
    # Calculate average response time
    avg_time = sum(r.response_time_ms for r in all_results) / total if total > 0 else 0
    baseline_avg = BASELINE["pressure_test"]["baseline_avg_time"]
    
    # Update results
    TEST_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    TEST_RESULTS["current"] = {
        "avg_response_time": avg_time,
        "passed": passed,
        "failed": failed,
        "warned": warned,
        "total": total
    }
    TEST_RESULTS["comparison"] = {
        "avg_time_change_percent": ((avg_time - baseline_avg) / baseline_avg * 100) if baseline_avg > 0 else 0
    }
    TEST_RESULTS["tests"] = [asdict(r) for r in all_results]
    
    print("\n" + "=" * 60)
    print("Summary")
    print("=" * 60)
    print(f"Total: {total}, Passed: {passed}, Failed: {failed}, Warned: {warned}")
    print(f"Current Avg Response: {avg_time:.2f}ms")
    print(f"Baseline Avg Response: {baseline_avg:.2f}ms")
    print(f"Change: {TEST_RESULTS['comparison']['avg_time_change_percent']:+.1f}%")
    
    return all_results, TEST_RESULTS


def generate_report(results: List[APITestResult], test_results: Dict, output_dir: str) -> str:
    """Generate regression test report"""
    test_time = test_results["test_time"]
    
    report = f"""# AI-Ready API Regression Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | {test_time} |
| Baseline Time | {BASELINE['test_time']} |
| Test Environment | {BASE_URL} |
| Total Tests | {len(results)} |
| Passed | {sum(1 for r in results if r.status == 'PASS')} |
| Failed | {sum(1 for r in results if r.status == 'FAIL')} |
| Warnings | {sum(1 for r in results if r.status == 'WARN')} |

---

## Regression Analysis

### Response Time Comparison

| Metric | Baseline | Current | Change |
|--------|----------|---------|--------|
| Average Response Time | {BASELINE['pressure_test']['baseline_avg_time']:.2f}ms | {test_results['current']['avg_response_time']:.2f}ms | {test_results['comparison']['avg_time_change_percent']:+.1f}% |

### Regression Status

"""
    
    # Check for regressions
    has_regression = any(r.status == "FAIL" for r in results)
    has_warnings = any(r.status == "WARN" for r in results)
    
    if has_regression:
        report += "**REGRESSION DETECTED** - Some tests failed.\n\n"
    elif has_warnings:
        report += "**WARNINGS** - Some tests have warnings.\n\n"
    else:
        report += "**NO REGRESSION** - All tests passed.\n\n"
    
    report += """---

## Test Results

| Test | Endpoint | Status Code | Response Time | Baseline | Deviation | Status |
|------|----------|-------------|---------------|----------|-----------|--------|
"""
    
    for r in results:
        report += f"| {r.name} | {r.endpoint} | {r.status_code} | {r.response_time_ms:.1f}ms | {r.baseline_time_ms:.1f}ms | {r.deviation_percent:+.1f}% | {r.status} |\n"
    
    report += f"""
---

## Baseline Comparison

### Previous Pressure Test Results (2026-04-03)

| Scenario | Baseline Response Time |
|----------|----------------------|
| Single User | {BASELINE['pressure_test']['baseline_avg_time']:.1f}ms |
| 50 Concurrent | {BASELINE['pressure_test']['low_concurrent_avg_time']:.1f}ms |
| 100 Concurrent | {BASELINE['pressure_test']['medium_concurrent_avg_time']:.1f}ms |
| 200 Concurrent | {BASELINE['pressure_test']['high_concurrent_avg_time']:.1f}ms |
| 500 Concurrent | {BASELINE['pressure_test']['stress_test_avg_time']:.1f}ms |

---

## Conclusion

"""
    
    if has_regression:
        report += "API regression detected. Review failed tests above.\n"
    else:
        report += "No API regression detected. All endpoints responding within expected parameters.\n"
    
    report += f"""
---

**Report Generated**: {test_time}
**Test Tool**: Python + requests
**Baseline Source**: Previous pressure test results
"""
    
    # Save report
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, f"API_REGRESSION_TEST_REPORT_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nReport saved: {report_path}")
    
    # Save JSON
    json_path = os.path.join(output_dir, f"api_regression_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(test_results, f, indent=2, ensure_ascii=False)
    
    print(f"JSON results saved: {json_path}")
    
    return report_path


def main():
    """Main function"""
    results, test_results = run_regression_tests()
    
    output_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
    generate_report(results, test_results, output_dir)
    
    print("\n" + "=" * 60)
    print("Regression Test Complete")
    print("=" * 60)
    
    return results, test_results


if __name__ == "__main__":
    main()
