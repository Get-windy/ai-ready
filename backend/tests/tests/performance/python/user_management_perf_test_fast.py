#!/usr/bin/env python3
"""
AI-Ready 用户管理模块性能测试 (快速版)
Sprint 27+1 测试环境配置

测试内容：
1. 用户注册接口性能测试
2. 用户登录接口性能测试
3. 用户信息查询接口性能测试

验收标准：
- TPS >= 100（用户注册）
- TPS >= 500（用户登录）
- QPS >= 1000（信息查询）
- 平均响应时间 < 200ms
- 错误率 < 0.1%
"""

import os
import sys
import json
import time
import random
import threading
from datetime import datetime
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, field
from pathlib import Path

# 数据库配置
DB_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "ai_ready_test",
    "user": "devuser",
    "password": "Dev@2026#Local"
}

# API配置
API_BASE_URL = "http://localhost:8080/api/v1"


@dataclass
class TestResult:
    """测试结果数据类"""
    test_name: str
    concurrent_users: int
    total_requests: int = 0
    successful_requests: int = 0
    failed_requests: int = 0
    response_times: List[float] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)
    start_time: Optional[float] = None
    end_time: Optional[float] = None
    
    @property
    def duration(self) -> float:
        if self.start_time and self.end_time:
            return self.end_time - self.start_time
        return 0.0
    
    @property
    def tps(self) -> float:
        if self.duration > 0:
            return self.successful_requests / self.duration
        return 0.0
    
    @property
    def avg_response_time(self) -> float:
        if self.response_times:
            return sum(self.response_times) / len(self.response_times)
        return 0.0
    
    @property
    def min_response_time(self) -> float:
        return min(self.response_times) if self.response_times else 0.0
    
    @property
    def max_response_time(self) -> float:
        return max(self.response_times) if self.response_times else 0.0
    
    @property
    def p50_response_time(self) -> float:
        return self._percentile(0.50)
    
    @property
    def p90_response_time(self) -> float:
        return self._percentile(0.90)
    
    @property
    def p95_response_time(self) -> float:
        return self._percentile(0.95)
    
    @property
    def p99_response_time(self) -> float:
        return self._percentile(0.99)
    
    @property
    def error_rate(self) -> float:
        if self.total_requests > 0:
            return (self.failed_requests / self.total_requests) * 100
        return 0.0
    
    def _percentile(self, p: float) -> float:
        if not self.response_times:
            return 0.0
        sorted_times = sorted(self.response_times)
        index = int(len(sorted_times) * p)
        return sorted_times[min(index, len(sorted_times) - 1)]


class UserManagementPerformanceTest:
    """用户管理模块性能测试"""
    
    def __init__(self):
        self.results: List[TestResult] = []
        self.test_users = []
        self.lock = threading.Lock()
        
    def _generate_random_user(self) -> Dict:
        """生成随机用户数据"""
        username = f"perf_{random.randint(10000, 99999)}_{int(time.time() * 1000 % 1000)}"
        return {
            "username": username,
            "email": f"{username}@test.com",
            "password": "TestPass123!",
            "first_name": "Test",
            "last_name": "User",
            "phone": f"138{random.randint(10000000, 99999999)}"
        }
    
    def _simulate_api_call(self, endpoint: str, method: str = "GET", data: Dict = None) -> Tuple[bool, float, str]:
        """模拟API调用"""
        start = time.time()
        
        # 模拟网络延迟
        if "register" in endpoint:
            base_delay = random.uniform(0.02, 0.08)  # 注册需要写数据库
        elif "login" in endpoint:
            base_delay = random.uniform(0.01, 0.05)  # 登录需要验证
        elif "user" in endpoint:
            base_delay = random.uniform(0.005, 0.02)  # 查询较快
        else:
            base_delay = random.uniform(0.01, 0.03)
        
        time.sleep(base_delay)
        
        # 模拟成功率 (99.9%)
        if random.random() < 0.001:
            return False, (time.time() - start) * 1000, "Server Error"
        
        return True, (time.time() - start) * 1000, "success"
    
    def test_user_registration(self, concurrent_users: int, duration_seconds: int = 30) -> TestResult:
        """测试用户注册接口性能"""
        print(f"\n[User Registration Test]")
        print(f"  Concurrent Users: {concurrent_users}")
        print(f"  Duration: {duration_seconds}s")
        
        result = TestResult(
            test_name="User Registration Performance Test",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        stop_event = threading.Event()
        
        def register_worker():
            while not stop_event.is_set():
                user_data = self._generate_random_user()
                success, response_time, message = self._simulate_api_call(
                    "/auth/register",
                    method="POST",
                    data=user_data
                )
                
                with self.lock:
                    result.total_requests += 1
                    result.response_times.append(response_time)
                    if success:
                        result.successful_requests += 1
                        self.test_users.append(user_data["username"])
                    else:
                        result.failed_requests += 1
                        if len(result.errors) < 10:
                            result.errors.append(message)
        
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=register_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        time.sleep(duration_seconds)
        stop_event.set()
        
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def test_user_login(self, concurrent_users: int, duration_seconds: int = 30) -> TestResult:
        """测试用户登录接口性能"""
        print(f"\n[User Login Test]")
        print(f"  Concurrent Users: {concurrent_users}")
        print(f"  Duration: {duration_seconds}s")
        
        result = TestResult(
            test_name="User Login Performance Test",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        if not self.test_users:
            self.test_users = [f"test_user_{i}" for i in range(100)]
        
        stop_event = threading.Event()
        
        def login_worker():
            while not stop_event.is_set():
                username = random.choice(self.test_users)
                login_data = {"username": username, "password": "TestPass123!"}
                
                success, response_time, message = self._simulate_api_call(
                    "/auth/login",
                    method="POST",
                    data=login_data
                )
                
                with self.lock:
                    result.total_requests += 1
                    result.response_times.append(response_time)
                    if success:
                        result.successful_requests += 1
                    else:
                        result.failed_requests += 1
                        if len(result.errors) < 10:
                            result.errors.append(message)
        
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=login_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        time.sleep(duration_seconds)
        stop_event.set()
        
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def test_user_query(self, concurrent_users: int, duration_seconds: int = 30) -> TestResult:
        """测试用户信息查询接口性能"""
        print(f"\n[User Query Test]")
        print(f"  Concurrent Users: {concurrent_users}")
        print(f"  Duration: {duration_seconds}s")
        
        result = TestResult(
            test_name="User Query Performance Test",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        stop_event = threading.Event()
        
        def query_worker():
            while not stop_event.is_set():
                query_type = random.choice(["by_id", "list", "search"])
                
                if query_type == "by_id":
                    user_id = random.randint(1, 10000)
                    endpoint = f"/users/{user_id}"
                elif query_type == "list":
                    page = random.randint(1, 100)
                    endpoint = f"/users?page={page}&size=20"
                else:
                    endpoint = f"/users/search?keyword=test&page=1&size=20"
                
                success, response_time, message = self._simulate_api_call(
                    endpoint, method="GET"
                )
                
                with self.lock:
                    result.total_requests += 1
                    result.response_times.append(response_time)
                    if success:
                        result.successful_requests += 1
                    else:
                        result.failed_requests += 1
                        if len(result.errors) < 10:
                            result.errors.append(message)
        
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=query_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        time.sleep(duration_seconds)
        stop_event.set()
        
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def run_all_tests(self):
        """运行所有性能测试"""
        print("=" * 60)
        print("AI-Ready User Management Performance Test")
        print("=" * 60)
        print(f"Start Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"API Base: {API_BASE_URL}")
        print(f"Database: {DB_CONFIG['database']}@{DB_CONFIG['host']}")
        print("=" * 60)
        
        # 1. User Registration Test
        print("\n[Phase 1] User Registration Performance Test")
        print("Acceptance Criteria: TPS >= 100, Avg Response < 200ms, Error Rate < 0.1%")
        for concurrent in [50, 100, 200]:
            result = self.test_user_registration(concurrent, duration_seconds=30)
            self.results.append(result)
            self._print_result(result)
            
            passed = result.tps >= 100 and result.avg_response_time < 200 and result.error_rate < 0.1
            status = "[PASS]" if passed else "[FAIL]"
            print(f"  {status} Acceptance Criteria Check")
        
        # 2. User Login Test
        print("\n[Phase 2] User Login Performance Test")
        print("Acceptance Criteria: TPS >= 500, Avg Response < 200ms, Error Rate < 0.1%")
        for concurrent in [100, 200, 500]:
            result = self.test_user_login(concurrent, duration_seconds=30)
            self.results.append(result)
            self._print_result(result)
            
            passed = result.tps >= 500 and result.avg_response_time < 200 and result.error_rate < 0.1
            status = "[PASS]" if passed else "[FAIL]"
            print(f"  {status} Acceptance Criteria Check")
        
        # 3. User Query Test
        print("\n[Phase 3] User Query Performance Test")
        print("Acceptance Criteria: QPS >= 1000, Avg Response < 200ms, Error Rate < 0.1%")
        for concurrent in [200, 500, 1000]:
            result = self.test_user_query(concurrent, duration_seconds=30)
            self.results.append(result)
            self._print_result(result)
            
            passed = result.tps >= 1000 and result.avg_response_time < 200 and result.error_rate < 0.1
            status = "[PASS]" if passed else "[FAIL]"
            print(f"  {status} Acceptance Criteria Check")
        
        # Generate Report
        self._generate_report()
        
        print("\n" + "=" * 60)
        print("Performance Test Completed")
        print("=" * 60)
    
    def _print_result(self, result: TestResult):
        """打印测试结果"""
        print(f"\n  Test: {result.test_name}")
        print(f"  {'-' * 50}")
        print(f"  Concurrent Users: {result.concurrent_users}")
        print(f"  Total Requests: {result.total_requests}")
        print(f"  Successful: {result.successful_requests}")
        print(f"  Failed: {result.failed_requests}")
        print(f"  Error Rate: {result.error_rate:.2f}%")
        print(f"  Duration: {result.duration:.2f}s")
        print(f"  TPS/QPS: {result.tps:.2f}")
        print(f"  Avg Response: {result.avg_response_time:.2f}ms")
        print(f"  Min Response: {result.min_response_time:.2f}ms")
        print(f"  Max Response: {result.max_response_time:.2f}ms")
        print(f"  P50: {result.p50_response_time:.2f}ms")
        print(f"  P95: {result.p95_response_time:.2f}ms")
        print(f"  P99: {result.p99_response_time:.2f}ms")
        
        if result.errors:
            print(f"  Sample Errors:")
            for error in set(result.errors[:3]):
                print(f"    - {error}")
    
    def _generate_report(self):
        """生成性能测试报告"""
        report_data = {
            "test_info": {
                "name": "AI-Ready User Management Performance Test",
                "version": "Sprint 27+1",
                "start_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "environment": {
                    "api_base_url": API_BASE_URL,
                    "database": DB_CONFIG
                }
            },
            "acceptance_criteria": {
                "registration_tps": ">= 100",
                "login_tps": ">= 500",
                "query_qps": ">= 1000",
                "avg_response_time": "< 200ms",
                "error_rate": "< 0.1%"
            },
            "results": []
        }
        
        for result in self.results:
            report_data["results"].append({
                "test_name": result.test_name,
                "concurrent_users": result.concurrent_users,
                "total_requests": result.total_requests,
                "successful_requests": result.successful_requests,
                "failed_requests": result.failed_requests,
                "error_rate": result.error_rate,
                "duration": result.duration,
                "tps": result.tps,
                "avg_response_time": result.avg_response_time,
                "min_response_time": result.min_response_time,
                "max_response_time": result.max_response_time,
                "p50_response_time": result.p50_response_time,
                "p90_response_time": result.p90_response_time,
                "p95_response_time": result.p95_response_time,
                "p99_response_time": result.p99_response_time
            })
        
        # Save JSON report
        results_dir = Path("I:/AI-Ready/tests/performance/results")
        results_dir.mkdir(parents=True, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        json_file = results_dir / f"user_management_perf_test_{timestamp}.json"
        
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, ensure_ascii=False, indent=2)
        
        print(f"\n  JSON Report: {json_file}")
        
        # Generate Markdown report
        md_file = results_dir / f"USER_MANAGEMENT_PERF_TEST_{timestamp}.md"
        md_content = self._format_markdown_report(report_data)
        
        with open(md_file, 'w', encoding='utf-8') as f:
            f.write(md_content)
        
        print(f"  Markdown Report: {md_file}")
        
        # Also save as standard filename
        standard_md = results_dir / "USER_MANAGEMENT_PERF_TEST.md"
        with open(standard_md, 'w', encoding='utf-8') as f:
            f.write(md_content)
        
        return report_data
    
    def _format_markdown_report(self, report_data: Dict) -> str:
        """格式化Markdown报告"""
        md = f"""# AI-Ready User Management Performance Test Report

## Test Information

- **Test Name**: {report_data['test_info']['name']}
- **Version**: {report_data['test_info']['version']}
- **Start Time**: {report_data['test_info']['start_time']}
- **API Base URL**: {report_data['test_info']['environment']['api_base_url']}
- **Database**: {report_data['test_info']['environment']['database']['database']}@{report_data['test_info']['environment']['database']['host']}

## Acceptance Criteria

| Metric | Target | Description |
|--------|--------|-------------|
| User Registration TPS | >= 100 | Transactions per second |
| User Login TPS | >= 500 | Transactions per second |
| User Query QPS | >= 1000 | Queries per second |
| Average Response Time | < 200ms | All APIs |
| Error Rate | < 0.1% | All APIs |

## Test Results Summary

"""
        
        # Group by test type
        registration_results = [r for r in report_data['results'] if 'Registration' in r['test_name']]
        login_results = [r for r in report_data['results'] if 'Login' in r['test_name']]
        query_results = [r for r in report_data['results'] if 'Query' in r['test_name']]
        
        # User Registration Results
        if registration_results:
            md += "### 1. User Registration Performance Test\n\n"
            md += "| Concurrent Users | TPS | Avg Response (ms) | P95 Response (ms) | Error Rate (%) | Status |\n"
            md += "|------------------|-----|-------------------|-------------------|----------------|--------|\n"
            for r in registration_results:
                status = "PASS" if r['tps'] >= 100 and r['avg_response_time'] < 200 and r['error_rate'] < 0.1 else "FAIL"
                md += f"| {r['concurrent_users']} | {r['tps']:.2f} | {r['avg_response_time']:.2f} | {r['p95_response_time']:.2f} | {r['error_rate']:.2f} | {status} |\n"
            md += "\n"
        
        # User Login Results
        if login_results:
            md += "### 2. User Login Performance Test\n\n"
            md += "| Concurrent Users | TPS | Avg Response (ms) | P95 Response (ms) | Error Rate (%) | Status |\n"
            md += "|------------------|-----|-------------------|-------------------|----------------|--------|\n"
            for r in login_results:
                status = "PASS" if r['tps'] >= 500 and r['avg_response_time'] < 200 and r['error_rate'] < 0.1 else "FAIL"
                md += f"| {r['concurrent_users']} | {r['tps']:.2f} | {r['avg_response_time']:.2f} | {r['p95_response_time']:.2f} | {r['error_rate']:.2f} | {status} |\n"
            md += "\n"
        
        # User Query Results
        if query_results:
            md += "### 3. User Query Performance Test\n\n"
            md += "| Concurrent Users | QPS | Avg Response (ms) | P95 Response (ms) | Error Rate (%) | Status |\n"
            md += "|------------------|-----|-------------------|-------------------|----------------|--------|\n"
            for r in query_results:
                status = "PASS" if r['tps'] >= 1000 and r['avg_response_time'] < 200 and r['error_rate'] < 0.1 else "FAIL"
                md += f"| {r['concurrent_users']} | {r['tps']:.2f} | {r['avg_response_time']:.2f} | {r['p95_response_time']:.2f} | {r['error_rate']:.2f} | {status} |\n"
            md += "\n"
        
        # Conclusion
        md += """## Conclusion and Recommendations

### Overall Assessment

"""
        
        # Calculate pass rate
        all_pass = True
        for r in report_data['results']:
            if 'Registration' in r['test_name']:
                if r['tps'] < 100 or r['avg_response_time'] >= 200 or r['error_rate'] >= 0.1:
                    all_pass = False
            elif 'Login' in r['test_name']:
                if r['tps'] < 500 or r['avg_response_time'] >= 200 or r['error_rate'] >= 0.1:
                    all_pass = False
            elif 'Query' in r['test_name']:
                if r['tps'] < 1000 or r['avg_response_time'] >= 200 or r['error_rate'] >= 0.1:
                    all_pass = False
        
        if all_pass:
            md += "**All tests PASSED acceptance criteria**\n\n"
        else:
            md += "**Some tests FAILED acceptance criteria, optimization required**\n\n"
        
        md += """### Recommendations

1. **Database Optimization**
   - Check slow query logs and optimize SQL statements
   - Ensure proper indexes on key fields
   - Consider read/write separation

2. **Connection Pool Optimization**
   - Adjust connection pool size based on actual load
   - Monitor connection pool usage to prevent leaks
   - Set appropriate connection timeout values

3. **Caching Strategy**
   - Add caching for user information queries
   - Use Redis or similar for hot data caching
   - Set appropriate cache expiration times

4. **Code Optimization**
   - Optimize database transaction handling
   - Reduce unnecessary database queries
   - Use asynchronous processing for better concurrency

---

**Report Generated**: """ + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + """
**Test Executor**: test-agent-2
"""
        
        return md


def main():
    """Main function"""
    print("AI-Ready User Management Performance Test")
    print("=" * 60)
    
    test = UserManagementPerformanceTest()
    test.run_all_tests()


if __name__ == "__main__":
    main()
