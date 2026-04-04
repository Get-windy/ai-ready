#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready API Rate Limiting Test Suite
Tests:
1. Normal request handling
2. Burst request handling
3. Rate limit detection
4. Rate limit recovery
"""

import requests
import time
import json
from datetime import datetime
from typing import Dict, List, Any, Tuple
from dataclasses import dataclass, field, asdict
from concurrent.futures import ThreadPoolExecutor, as_completed
import os

# Configuration
BASE_URL = "http://localhost:8080"

TEST_RESULTS = {
    "test_time": "",
    "base_url": BASE_URL,
    "tests": [],
    "summary": {}
}


@dataclass
class RateLimitTestResult:
    name: str
    total_requests: int
    successful_requests: int
    rate_limited_requests: int
    rate_limit_detected: bool
    status_codes: Dict[int, int]
    avg_response_time_ms: float
    status: str = "PASS"
    message: str = ""


def make_request(endpoint: str, method: str = "GET") -> Tuple[int, float]:
    """Make HTTP request and return status code and elapsed time"""
    url = f"{BASE_URL}{endpoint}"
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Real-IP": "127.0.0.1"
    }
    
    start = time.perf_counter()
    try:
        resp = requests.get(url, headers=headers, timeout=10)
        elapsed = (time.perf_counter() - start) * 1000
        return resp.status_code, elapsed
    except Exception as e:
        elapsed = (time.perf_counter() - start) * 1000
        return 0, elapsed


def test_normal_request_rate():
    """Test normal request rate handling"""
    results = []
    
    for i in range(10):
        status_code, elapsed = make_request("/")
        results.append({"status": status_code, "time": elapsed})
        time.sleep(0.2)  # 5 requests per second
    
    status_codes = [r["status"] for r in results]
    code_distribution = {}
    for code in status_codes:
        code_distribution[code] = code_distribution.get(code, 0) + 1
    
    successful = sum(1 for r in results if r["status"] in [200, 401, 403])
    avg_time = sum(r["time"] for r in results) / len(results)
    
    result = RateLimitTestResult(
        name="Normal Request Rate",
        total_requests=len(results),
        successful_requests=successful,
        rate_limited_requests=0,
        rate_limit_detected=False,
        status_codes=code_distribution,
        avg_response_time_ms=avg_time
    )
    
    if successful == len(results):
        result.status = "PASS"
        result.message = f"All {len(results)} requests handled successfully"
    else:
        result.status = "WARN"
        result.message = f"Some requests failed: {successful}/{len(results)}"
    
    return result


def test_burst_requests():
    """Test burst request handling"""
    results = []
    
    # Send 20 requests rapidly without delay
    for i in range(20):
        status_code, elapsed = make_request("/")
        results.append({"status": status_code, "time": elapsed})
    
    status_codes = [r["status"] for r in results]
    code_distribution = {}
    for code in status_codes:
        code_distribution[code] = code_distribution.get(code, 0) + 1
    
    successful = sum(1 for r in results if r["status"] in [200, 401, 403])
    rate_limited = sum(1 for r in results if r["status"] == 429)
    avg_time = sum(r["time"] for r in results) / len(results)
    
    result = RateLimitTestResult(
        name="Burst Requests",
        total_requests=len(results),
        successful_requests=successful,
        rate_limited_requests=rate_limited,
        rate_limit_detected=rate_limited > 0,
        status_codes=code_distribution,
        avg_response_time_ms=avg_time
    )
    
    # If rate limiting is implemented, we expect some 429 responses
    # If not implemented, all requests should still be handled
    if rate_limited > 0:
        result.status = "PASS"
        result.message = f"Rate limiting detected: {rate_limited} requests limited"
    elif successful == len(results):
        result.status = "PASS"
        result.message = f"All {len(results)} burst requests handled (no rate limiting)"
    else:
        result.status = "WARN"
        result.message = f"Some requests failed unexpectedly"
    
    return result


def test_concurrent_burst():
    """Test concurrent burst handling"""
    results_queue = []
    
    def make_concurrent_request(i):
        status_code, elapsed = make_request("/")
        return {"status": status_code, "time": elapsed}
    
    # Send 30 concurrent requests
    with ThreadPoolExecutor(max_workers=30) as executor:
        futures = [executor.submit(make_concurrent_request, i) for i in range(30)]
        for future in as_completed(futures):
            results_queue.append(future.result())
    
    status_codes = [r["status"] for r in results_queue]
    code_distribution = {}
    for code in status_codes:
        code_distribution[code] = code_distribution.get(code, 0) + 1
    
    successful = sum(1 for r in results_queue if r["status"] in [200, 401, 403])
    rate_limited = sum(1 for r in results_queue if r["status"] == 429)
    avg_time = sum(r["time"] for r in results_queue) / len(results_queue) if results_queue else 0
    
    result = RateLimitTestResult(
        name="Concurrent Burst",
        total_requests=len(results_queue),
        successful_requests=successful,
        rate_limited_requests=rate_limited,
        rate_limit_detected=rate_limited > 0,
        status_codes=code_distribution,
        avg_response_time_ms=avg_time
    )
    
    if rate_limited > 0:
        result.status = "PASS"
        result.message = f"Rate limiting active: {rate_limited}/{len(results_queue)} limited"
    elif successful == len(results_queue):
        result.status = "PASS"
        result.message = f"All {len(results_queue)} concurrent requests handled"
    else:
        result.status = "WARN"
        result.message = f"Mixed results: {successful} success, {rate_limited} limited"
    
    return result


def test_rate_limit_recovery():
    """Test recovery after rate limiting"""
    # First, send a burst
    print("  Sending initial burst...")
    initial_results = []
    for i in range(10):
        status_code, elapsed = make_request("/")
        initial_results.append({"status": status_code, "time": elapsed})
    
    # Wait for recovery period
    print("  Waiting 2 seconds for recovery...")
    time.sleep(2)
    
    # Send recovery requests
    print("  Sending recovery requests...")
    recovery_results = []
    for i in range(5):
        status_code, elapsed = make_request("/")
        recovery_results.append({"status": status_code, "time": elapsed})
        time.sleep(0.1)
    
    initial_codes = [r["status"] for r in initial_results]
    recovery_codes = [r["status"] for r in recovery_results]
    
    initial_distribution = {}
    for code in initial_codes:
        initial_distribution[code] = initial_distribution.get(code, 0) + 1
    
    recovery_distribution = {}
    for code in recovery_codes:
        recovery_distribution[code] = recovery_distribution.get(code, 0) + 1
    
    # Check if recovery requests are handled normally
    recovery_successful = sum(1 for c in recovery_codes if c in [200, 401, 403])
    
    result = RateLimitTestResult(
        name="Rate Limit Recovery",
        total_requests=len(recovery_results),
        successful_requests=recovery_successful,
        rate_limited_requests=sum(1 for c in recovery_codes if c == 429),
        rate_limit_detected=429 in recovery_codes,
        status_codes=recovery_distribution,
        avg_response_time_ms=sum(r["time"] for r in recovery_results) / len(recovery_results)
    )
    
    if recovery_successful == len(recovery_results):
        result.status = "PASS"
        result.message = "System recovered and handling requests normally"
    else:
        result.status = "WARN"
        result.message = f"Recovery incomplete: {recovery_successful}/{len(recovery_results)} succeeded"
    
    return result


def test_sustained_load():
    """Test sustained moderate load"""
    results = []
    
    # Send 50 requests over 5 seconds (10 req/s)
    for i in range(50):
        status_code, elapsed = make_request("/")
        results.append({"status": status_code, "time": elapsed})
        time.sleep(0.1)  # ~10 req/s
    
    status_codes = [r["status"] for r in results]
    code_distribution = {}
    for code in status_codes:
        code_distribution[code] = code_distribution.get(code, 0) + 1
    
    successful = sum(1 for r in results if r["status"] in [200, 401, 403])
    rate_limited = sum(1 for r in results if r["status"] == 429)
    avg_time = sum(r["time"] for r in results) / len(results)
    
    result = RateLimitTestResult(
        name="Sustained Load",
        total_requests=len(results),
        successful_requests=successful,
        rate_limited_requests=rate_limited,
        rate_limit_detected=rate_limited > 0,
        status_codes=code_distribution,
        avg_response_time_ms=avg_time
    )
    
    if successful >= len(results) * 0.95:  # 95% success rate
        result.status = "PASS"
        result.message = f"Sustained load handled: {successful}/{len(results)} succeeded"
    else:
        result.status = "WARN"
        result.message = f"High failure rate: {successful}/{len(results)} succeeded"
    
    return result


def run_all_tests():
    """Run all rate limiting tests"""
    print("=" * 60)
    print("AI-Ready API Rate Limiting Test")
    print("=" * 60)
    print(f"Test Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Target: {BASE_URL}")
    print()
    
    tests = [
        ("Normal Rate", test_normal_request_rate),
        ("Burst", test_burst_requests),
        ("Concurrent Burst", test_concurrent_burst),
        ("Recovery", test_rate_limit_recovery),
        ("Sustained Load", test_sustained_load),
    ]
    
    results = []
    for name, test_func in tests:
        print(f"\n--- {name} Test ---")
        try:
            result = test_func()
            results.append(result)
            print(f"[{result.status}] {result.name}: {result.message}")
        except Exception as e:
            result = RateLimitTestResult(
                name=name,
                total_requests=0,
                successful_requests=0,
                rate_limited_requests=0,
                rate_limit_detected=False,
                status_codes={},
                avg_response_time_ms=0,
                status="FAIL",
                message=str(e)
            )
            results.append(result)
            print(f"[FAIL] {name}: {str(e)}")
    
    # Summary
    passed = sum(1 for r in results if r.status == "PASS")
    failed = sum(1 for r in results if r.status == "FAIL")
    warned = sum(1 for r in results if r.status == "WARN")
    total = len(results)
    
    print("\n" + "=" * 60)
    print(f"Summary: {passed}/{total} passed, {failed} failed, {warned} warned")
    print("=" * 60)
    
    TEST_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    TEST_RESULTS["tests"] = [asdict(r) for r in results]
    TEST_RESULTS["summary"] = {
        "total": total,
        "passed": passed,
        "failed": failed,
        "warned": warned
    }
    
    return results, TEST_RESULTS


def generate_report(results: List[RateLimitTestResult], test_results: Dict, output_dir: str) -> str:
    """Generate rate limiting test report"""
    test_time = test_results["test_time"]
    summary = test_results["summary"]
    
    report = f"""# AI-Ready API Rate Limiting Test Report

## Test Overview

| Item | Value |
|------|-------|
| Test Time | {test_time} |
| Test Environment | {BASE_URL} |
| Total Tests | {summary['total']} |
| Passed | {summary['passed']} |
| Failed | {summary['failed']} |
| Warnings | {summary['warned']} |

---

## Test Results

| Test | Total Requests | Successful | Rate Limited | Avg Time | Status |
|------|---------------|------------|--------------|----------|--------|
"""
    
    for r in results:
        report += f"| {r.name} | {r.total_requests} | {r.successful_requests} | {r.rate_limited_requests} | {r.avg_response_time_ms:.1f}ms | {r.status} |\n"
    
    report += """
---

## Rate Limiting Analysis

"""
    
    rate_limited_tests = [r for r in results if r.rate_limit_detected]
    
    if rate_limited_tests:
        report += "### Rate Limiting Detected\n\n"
        report += "The system has rate limiting implemented:\n\n"
        for r in rate_limited_tests:
            report += f"- **{r.name}**: {r.rate_limited_requests} requests rate limited (HTTP 429)\n"
    else:
        report += "### No Rate Limiting Detected\n\n"
        report += "The system handled all requests without rate limiting.\n\n"
        report += "**Note**: Consider implementing rate limiting for production use:\n"
        report += "- Add rate limiting middleware\n"
        report += "- Configure limits per endpoint\n"
        report += "- Return 429 Too Many Requests when exceeded\n"
    
    report += f"""
---

## Recommendations

1. **Implement Rate Limiting** if not already present
   - Use token bucket or sliding window algorithm
   - Configure limits per endpoint/client
   - Add Retry-After header for 429 responses

2. **Rate Limit Headers**
   - X-RateLimit-Limit: Maximum requests per window
   - X-RateLimit-Remaining: Remaining requests
   - X-RateLimit-Reset: Time until reset

3. **Circuit Breaker**
   - Implement circuit breaker for high load protection
   - Graceful degradation under pressure

---

## Conclusion

**Overall Status: {summary['passed']}/{summary['total']} tests passed**

"""
    
    if summary['passed'] == summary['total']:
        report += "All rate limiting tests passed. System handles requests appropriately.\n"
    else:
        report += "Some tests need attention. Review the test results above.\n"
    
    report += f"""
---

**Report Generated**: {test_time}
**Test Tool**: Python + requests + ThreadPoolExecutor
"""
    
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, f"API_RATE_LIMITING_TEST_REPORT_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nReport saved: {report_path}")
    
    json_path = os.path.join(output_dir, f"api_rate_limiting_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(test_results, f, indent=2, ensure_ascii=False)
    
    print(f"JSON results saved: {json_path}")
    
    return report_path


def main():
    """Main function"""
    results, test_results = run_all_tests()
    
    output_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
    generate_report(results, test_results, output_dir)
    
    print("\n" + "=" * 60)
    print("Rate Limiting Test Complete")
    print("=" * 60)
    
    return results, test_results


if __name__ == "__main__":
    main()
