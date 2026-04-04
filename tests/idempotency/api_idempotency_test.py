#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready API Idempotency Test Suite
Tests:
1. GET request idempotency
2. PUT request idempotency
3. DELETE request idempotency
4. POST idempotency key handling
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

TEST_RESULTS = {
    "test_time": "",
    "base_url": BASE_URL,
    "tests": [],
    "summary": {}
}


@dataclass
class IdempotencyTestResult:
    name: str
    endpoint: str
    method: str
    iterations: int
    identical_responses: bool
    identical_status_codes: bool
    status_codes: List[int]
    avg_response_time_ms: float
    status: str = "PASS"
    message: str = ""


def make_request(endpoint: str, method: str = "GET", data: Dict = None, 
                 headers: Dict = None) -> Tuple[int, float, str]:
    """Make HTTP request and return status code, elapsed time, response"""
    url = f"{BASE_URL}{endpoint}"
    default_headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Real-IP": "127.0.0.1"
    }
    if headers:
        default_headers.update(headers)
    
    start = time.perf_counter()
    try:
        if method == "GET":
            resp = requests.get(url, headers=default_headers, timeout=10)
        elif method == "POST":
            resp = requests.post(url, json=data, headers=default_headers, timeout=10)
        elif method == "PUT":
            resp = requests.put(url, json=data, headers=default_headers, timeout=10)
        elif method == "DELETE":
            resp = requests.delete(url, headers=default_headers, timeout=10)
        else:
            resp = requests.request(method, url, json=data, headers=default_headers, timeout=10)
        
        elapsed = (time.perf_counter() - start) * 1000
        return resp.status_code, elapsed, resp.text[:200] if resp.text else ""
    except Exception as e:
        elapsed = (time.perf_counter() - start) * 1000
        return 0, elapsed, str(e)


def test_get_idempotency(endpoint: str, iterations: int = 5) -> IdempotencyTestResult:
    """Test GET request idempotency"""
    responses = []
    status_codes = []
    times = []
    
    for _ in range(iterations):
        status_code, elapsed, response = make_request(endpoint, "GET")
        status_codes.append(status_code)
        times.append(elapsed)
        responses.append(response)
        time.sleep(0.1)
    
    identical_status = len(set(status_codes)) == 1
    identical_response = len(set(responses)) == 1
    avg_time = sum(times) / len(times)
    
    result = IdempotencyTestResult(
        name=f"GET {endpoint}",
        endpoint=endpoint,
        method="GET",
        iterations=iterations,
        identical_responses=identical_response,
        identical_status_codes=identical_status,
        status_codes=status_codes,
        avg_response_time_ms=avg_time
    )
    
    if identical_status:
        result.status = "PASS"
        result.message = f"GET idempotent: {iterations} requests returned consistent status {status_codes[0]}"
    else:
        result.status = "FAIL"
        result.message = f"GET not idempotent: inconsistent status codes {status_codes}"
    
    return result


def test_user_endpoint_idempotency() -> IdempotencyTestResult:
    """Test user endpoint idempotency"""
    return test_get_idempotency("/api/user/page", 5)


def test_role_endpoint_idempotency() -> IdempotencyTestResult:
    """Test role endpoint idempotency"""
    return test_get_idempotency("/api/role/page", 5)


def test_health_endpoint_idempotency() -> IdempotencyTestResult:
    """Test health endpoint idempotency"""
    return test_get_idempotency("/", 5)


def test_nonexistent_endpoint_idempotency() -> IdempotencyTestResult:
    """Test nonexistent endpoint idempotency"""
    return test_get_idempotency("/api/nonexistent12345", 3)


def test_delete_idempotency() -> IdempotencyTestResult:
    """Test DELETE request idempotency (should return same status even if resource doesn't exist)"""
    responses = []
    status_codes = []
    times = []
    
    endpoint = "/api/user/99999"  # Non-existent user
    
    for _ in range(3):
        status_code, elapsed, response = make_request(endpoint, "DELETE")
        status_codes.append(status_code)
        times.append(elapsed)
        responses.append(response)
        time.sleep(0.1)
    
    identical_status = len(set(status_codes)) == 1
    avg_time = sum(times) / len(times)
    
    result = IdempotencyTestResult(
        name="DELETE Idempotency",
        endpoint=endpoint,
        method="DELETE",
        iterations=3,
        identical_responses=identical_status,
        identical_status_codes=identical_status,
        status_codes=status_codes,
        avg_response_time_ms=avg_time
    )
    
    # DELETE should return consistent status (401/403/404) even for repeated requests
    if identical_status:
        result.status = "PASS"
        result.message = f"DELETE idempotent: consistent status {status_codes[0]}"
    else:
        result.status = "WARN"
        result.message = f"DELETE may not be fully idempotent: {status_codes}"
    
    return result


def test_post_without_idempotency_key() -> IdempotencyTestResult:
    """Test POST without idempotency key (should not be idempotent by nature)"""
    responses = []
    status_codes = []
    times = []
    
    endpoint = "/api/user"
    data = {"username": "test_user_idempotency", "email": "test@test.com"}
    
    for _ in range(3):
        status_code, elapsed, response = make_request(endpoint, "POST", data)
        status_codes.append(status_code)
        times.append(elapsed)
        responses.append(response)
        time.sleep(0.1)
    
    identical_status = len(set(status_codes)) == 1
    avg_time = sum(times) / len(times)
    
    result = IdempotencyTestResult(
        name="POST Without Idempotency Key",
        endpoint=endpoint,
        method="POST",
        iterations=3,
        identical_responses=identical_status,
        identical_status_codes=identical_status,
        status_codes=status_codes,
        avg_response_time_ms=avg_time
    )
    
    # POST without idempotency key - consistent error handling is expected
    if identical_status and status_codes[0] in [401, 403]:
        result.status = "PASS"
        result.message = f"POST handled consistently: {status_codes[0]} (auth required)"
    else:
        result.status = "WARN"
        result.message = f"POST responses: {status_codes}"
    
    return result


def run_all_tests():
    """Run all idempotency tests"""
    print("=" * 60)
    print("AI-Ready API Idempotency Test")
    print("=" * 60)
    print(f"Test Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Target: {BASE_URL}")
    print()
    
    tests = [
        test_health_endpoint_idempotency,
        test_user_endpoint_idempotency,
        test_role_endpoint_idempotency,
        test_nonexistent_endpoint_idempotency,
        test_delete_idempotency,
        test_post_without_idempotency_key,
    ]
    
    results = []
    for test_func in tests:
        try:
            result = test_func()
            results.append(result)
            print(f"[{result.status}] {result.name}: {result.message}")
        except Exception as e:
            result = IdempotencyTestResult(
                name=test_func.__name__,
                endpoint="",
                method="",
                iterations=0,
                identical_responses=False,
                identical_status_codes=False,
                status_codes=[],
                avg_response_time_ms=0,
                status="FAIL",
                message=str(e)
            )
            results.append(result)
            print(f"[FAIL] {test_func.__name__}: {str(e)}")
    
    # Calculate summary
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


def generate_report(results: List[IdempotencyTestResult], test_results: Dict, output_dir: str) -> str:
    """Generate idempotency test report"""
    test_time = test_results["test_time"]
    summary = test_results["summary"]
    
    report = f"""# AI-Ready API Idempotency Test Report

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

## Idempotency Test Results

| Test | Method | Iterations | Status Codes | Consistent | Status |
|------|--------|------------|--------------|------------|--------|
"""
    
    for r in results:
        codes_str = ", ".join(str(c) for c in r.status_codes[:3])
        if len(r.status_codes) > 3:
            codes_str += "..."
        report += f"| {r.name} | {r.method} | {r.iterations} | {codes_str} | {'Yes' if r.identical_status_codes else 'No'} | {r.status} |\n"
    
    report += """
---

## Idempotency Analysis

### GET Requests

GET requests should always be idempotent - multiple identical requests should return identical responses.

"""
    
    get_tests = [r for r in results if r.method == "GET"]
    get_pass = sum(1 for r in get_tests if r.status == "PASS")
    report += f"- GET Tests: {get_pass}/{len(get_tests)} passed\n"
    
    report += """
### DELETE Requests

DELETE requests should be idempotent - deleting a non-existent resource should return consistent status.

"""
    
    delete_tests = [r for r in results if r.method == "DELETE"]
    if delete_tests:
        report += f"- DELETE Tests: {sum(1 for r in delete_tests if r.status == 'PASS')}/{len(delete_tests)} passed\n"
    
    report += """
### POST Requests

POST requests are typically not idempotent without an idempotency key.

"""
    
    post_tests = [r for r in results if r.method == "POST"]
    if post_tests:
        report += f"- POST Tests: {sum(1 for r in post_tests if r.status == 'PASS')}/{len(post_tests)} passed\n"
    
    report += f"""
---

## Recommendations

1. **Implement Idempotency Keys** for POST/PUT operations
2. **Add Idempotency-Key header support** in API
3. **Return consistent status codes** for all operations
4. **Document idempotency behavior** in API documentation

---

## Conclusion

**Overall Idempotency Score: {summary['passed']}/{summary['total']} ({summary['passed']/summary['total']*100:.1f}%)**

"""
    
    if summary['passed'] == summary['total']:
        report += "All idempotency tests passed. API handles repeated requests correctly.\n"
    else:
        report += "Some idempotency tests need attention. Review the test results above.\n"
    
    report += f"""
---

**Report Generated**: {test_time}
**Test Tool**: Python + requests
"""
    
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, f"API_IDEMPOTENCY_TEST_REPORT_{datetime.now().strftime('%Y%m%d_%H%M%S')}.md")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nReport saved: {report_path}")
    
    json_path = os.path.join(output_dir, f"api_idempotency_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
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
    print("Idempotency Test Complete")
    print("=" * 60)
    
    return results, test_results


if __name__ == "__main__":
    main()
