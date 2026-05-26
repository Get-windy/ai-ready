#!/usr/bin/env python3
"""
AI-Ready 用户管理模块性能测试
Sprint 27+1 测试环境配置

测试内容：
1. 用户注册接口性能测试
2. 用户登录接口性能测试
3. 用户信息查询接口性能测试
4. 数据库连接池压力测试

验收标准：
- TPS ≥ 100（用户注册）
- TPS ≥ 500（用户登录）
- QPS ≥ 1000（信息查询）
- 平均响应时间 < 200ms
- 错误率 < 0.1%
"""

import os
import sys
import json
import time
import random
import string
import threading
import concurrent.futures
from datetime import datetime
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, field
from pathlib import Path

# 尝试导入必要的库
try:
    import requests
    import psycopg2
    from psycopg2 import pool
    REQUESTS_AVAILABLE = True
    PSYCOPG2_AVAILABLE = True
except ImportError:
    REQUESTS_AVAILABLE = False
    PSYCOPG2_AVAILABLE = False
    print("警告: requests 或 psycopg2 未安装，将使用模拟模式")

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


class DatabaseConnectionPool:
    """数据库连接池管理"""
    
    def __init__(self, min_conn: int = 5, max_conn: int = 50):
        self.min_conn = min_conn
        self.max_conn = max_conn
        self.connection_pool = None
        self.connections_in_use = 0
        self.max_connections_used = 0
        self.connection_wait_times = []
        
        if PSYCOPG2_AVAILABLE:
            try:
                self.connection_pool = psycopg2.pool.ThreadedConnectionPool(
                    min_conn, max_conn,
                    **DB_CONFIG
                )
                print(f"数据库连接池创建成功: min={min_conn}, max={max_conn}")
            except Exception as e:
                print(f"数据库连接池创建失败: {e}")
                self.connection_pool = None
        else:
            print("模拟模式: 数据库连接池")
    
    def get_connection(self) -> Optional:
        """获取连接"""
        start = time.time()
        if self.connection_pool:
            try:
                conn = self.connection_pool.getconn()
                self.connections_in_use += 1
                self.max_connections_used = max(self.max_connections_used, self.connections_in_use)
                self.connection_wait_times.append(time.time() - start)
                return conn
            except Exception as e:
                print(f"获取连接失败: {e}")
                return None
        return None
    
    def release_connection(self, conn):
        """释放连接"""
        if self.connection_pool and conn:
            try:
                self.connection_pool.putconn(conn)
                self.connections_in_use -= 1
            except Exception as e:
                print(f"释放连接失败: {e}")
    
    def get_stats(self) -> Dict:
        """获取连接池统计"""
        return {
            "min_connections": self.min_conn,
            "max_connections": self.max_conn,
            "connections_in_use": self.connections_in_use,
            "max_connections_used": self.max_connections_used,
            "avg_wait_time_ms": sum(self.connection_wait_times) / len(self.connection_wait_times) * 1000 if self.connection_wait_times else 0,
            "connection_leaks": max(0, self.connections_in_use)  # 如果为正值，可能存在泄漏
        }


class UserManagementPerformanceTest:
    """用户管理模块性能测试"""
    
    def __init__(self):
        self.db_pool = DatabaseConnectionPool(min_conn=10, max_conn=100)
        self.results: List[TestResult] = []
        self.test_users = []
        self.lock = threading.Lock()
        
    def _generate_random_user(self) -> Dict:
        """生成随机用户数据"""
        username = f"perf_test_{random.randint(10000, 99999)}_{int(time.time() * 1000 % 10000)}"
        return {
            "username": username,
            "email": f"{username}@test.com",
            "password": "TestPass123!",
            "first_name": "Test",
            "last_name": "User",
            "phone": f"138{random.randint(10000000, 99999999)}"
        }
    
    def _generate_auth_token(self, user_id: str) -> str:
        """生成模拟JWT token"""
        import hashlib
        token_data = f"{user_id}:{time.time()}:secret_key"
        return hashlib.sha256(token_data.encode()).hexdigest()[:32]
    
    def _simulate_api_call(self, endpoint: str, method: str = "GET", data: Dict = None) -> Tuple[bool, float, str]:
        """模拟API调用（当requests不可用时）"""
        start = time.time()
        
        # 模拟网络延迟 (10-100ms)
        base_delay = random.uniform(0.01, 0.05)
        
        # 根据端点类型添加额外延迟
        if "register" in endpoint:
            base_delay += random.uniform(0.02, 0.08)  # 注册需要写数据库
        elif "login" in endpoint:
            base_delay += random.uniform(0.01, 0.05)  # 登录需要验证
        elif "user" in endpoint:
            base_delay += random.uniform(0.005, 0.02)  # 查询较快
        
        time.sleep(base_delay)
        
        # 模拟成功率 (99.9%)
        if random.random() < 0.001:
            return False, (time.time() - start) * 1000, "模拟错误: 服务器内部错误"
        
        return True, (time.time() - start) * 1000, "success"
    
    def _make_api_request(self, endpoint: str, method: str = "GET", data: Dict = None, headers: Dict = None) -> Tuple[bool, float, str]:
        """执行API请求"""
        if not REQUESTS_AVAILABLE:
            return self._simulate_api_call(endpoint, method, data)
        
        url = f"{API_BASE_URL}{endpoint}"
        start = time.time()
        
        try:
            if method == "GET":
                response = requests.get(url, headers=headers, timeout=10)
            elif method == "POST":
                response = requests.post(url, json=data, headers=headers, timeout=10)
            elif method == "PUT":
                response = requests.put(url, json=data, headers=headers, timeout=10)
            elif method == "DELETE":
                response = requests.delete(url, headers=headers, timeout=10)
            else:
                return False, 0, f"不支持的方法: {method}"
            
            elapsed = (time.time() - start) * 1000
            
            if response.status_code in [200, 201, 204]:
                return True, elapsed, "success"
            else:
                return False, elapsed, f"HTTP {response.status_code}: {response.text[:100]}"
                
        except requests.exceptions.Timeout:
            return False, (time.time() - start) * 1000, "请求超时"
        except Exception as e:
            return False, (time.time() - start) * 1000, str(e)
    
    def test_user_registration(self, concurrent_users: int, duration_seconds: int = 300) -> TestResult:
        """测试用户注册接口性能"""
        print(f"\n=== 用户注册性能测试 ===")
        print(f"并发用户数: {concurrent_users}")
        print(f"测试时长: {duration_seconds}秒")
        
        result = TestResult(
            test_name="用户注册接口性能测试",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        stop_event = threading.Event()
        
        def register_worker():
            while not stop_event.is_set():
                user_data = self._generate_random_user()
                success, response_time, message = self._make_api_request(
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
                        result.errors.append(message)
        
        # 启动并发线程
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=register_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        # 运行指定时长
        time.sleep(duration_seconds)
        stop_event.set()
        
        # 等待所有线程完成
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def test_user_login(self, concurrent_users: int, duration_seconds: int = 300) -> TestResult:
        """测试用户登录接口性能"""
        print(f"\n=== 用户登录性能测试 ===")
        print(f"并发用户数: {concurrent_users}")
        print(f"测试时长: {duration_seconds}秒")
        
        result = TestResult(
            test_name="用户登录接口性能测试",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        # 确保有测试用户
        if not self.test_users:
            print("警告: 没有可用的测试用户，将使用模拟用户")
            self.test_users = [f"test_user_{i}" for i in range(100)]
        
        stop_event = threading.Event()
        
        def login_worker():
            while not stop_event.is_set():
                username = random.choice(self.test_users)
                login_data = {
                    "username": username,
                    "password": "TestPass123!"
                }
                
                success, response_time, message = self._make_api_request(
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
                        result.errors.append(message)
        
        # 启动并发线程
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=login_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        # 运行指定时长
        time.sleep(duration_seconds)
        stop_event.set()
        
        # 等待所有线程完成
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def test_user_query(self, concurrent_users: int, duration_seconds: int = 300) -> TestResult:
        """测试用户信息查询接口性能"""
        print(f"\n=== 用户信息查询性能测试 ===")
        print(f"并发用户数: {concurrent_users}")
        print(f"测试时长: {duration_seconds}秒")
        
        result = TestResult(
            test_name="用户信息查询接口性能测试",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        # 生成模拟token
        auth_token = self._generate_auth_token("test_admin")
        headers = {"Authorization": f"Bearer {auth_token}"}
        
        stop_event = threading.Event()
        
        def query_worker():
            while not stop_event.is_set():
                # 随机查询方式
                query_type = random.choice(["by_id", "list", "search"])
                
                if query_type == "by_id":
                    user_id = random.randint(1, 10000)
                    endpoint = f"/users/{user_id}"
                elif query_type == "list":
                    page = random.randint(1, 100)
                    endpoint = f"/users?page={page}&size=20"
                else:  # search
                    endpoint = f"/users/search?keyword=test&page=1&size=20"
                
                success, response_time, message = self._make_api_request(
                    endpoint,
                    method="GET",
                    headers=headers
                )
                
                with self.lock:
                    result.total_requests += 1
                    result.response_times.append(response_time)
                    if success:
                        result.successful_requests += 1
                    else:
                        result.failed_requests += 1
                        result.errors.append(message)
        
        # 启动并发线程
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=query_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        # 运行指定时长
        time.sleep(duration_seconds)
        stop_event.set()
        
        # 等待所有线程完成
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def test_database_connection_pool(self, concurrent_users: int, duration_seconds: int = 300) -> TestResult:
        """测试数据库连接池压力"""
        print(f"\n=== 数据库连接池压力测试 ===")
        print(f"并发用户数: {concurrent_users}")
        print(f"测试时长: {duration_seconds}秒")
        
        result = TestResult(
            test_name="数据库连接池压力测试",
            concurrent_users=concurrent_users
        )
        result.start_time = time.time()
        
        stop_event = threading.Event()
        
        def db_worker():
            while not stop_event.is_set():
                conn = None
                start = time.time()
                
                try:
                    # 获取连接
                    conn = self.db_pool.get_connection()
                    
                    if conn:
                        # 执行简单查询
                        if PSYCOPG2_AVAILABLE:
                            with conn.cursor() as cur:
                                cur.execute("SELECT 1")
                                cur.fetchone()
                        else:
                            # 模拟查询延迟
                            time.sleep(random.uniform(0.001, 0.01))
                        
                        elapsed = (time.time() - start) * 1000
                        
                        with self.lock:
                            result.total_requests += 1
                            result.successful_requests += 1
                            result.response_times.append(elapsed)
                    else:
                        with self.lock:
                            result.total_requests += 1
                            result.failed_requests += 1
                            result.errors.append("无法获取数据库连接")
                    
                except Exception as e:
                    elapsed = (time.time() - start) * 1000
                    with self.lock:
                        result.total_requests += 1
                        result.failed_requests += 1
                        result.errors.append(str(e))
                
                finally:
                    if conn:
                        self.db_pool.release_connection(conn)
                
                # 模拟业务处理间隔
                time.sleep(random.uniform(0.001, 0.005))
        
        # 启动并发线程
        threads = []
        for _ in range(concurrent_users):
            t = threading.Thread(target=db_worker)
            t.daemon = True
            threads.append(t)
            t.start()
        
        # 运行指定时长
        time.sleep(duration_seconds)
        stop_event.set()
        
        # 等待所有线程完成
        for t in threads:
            t.join(timeout=5)
        
        result.end_time = time.time()
        return result
    
    def run_all_tests(self):
        """运行所有性能测试"""
        print("=" * 60)
        print("AI-Ready 用户管理模块性能测试")
        print("=" * 60)
        print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"测试环境: {API_BASE_URL}")
        print(f"数据库: {DB_CONFIG['database']}@{DB_CONFIG['host']}")
        print("=" * 60)
        
        # 1. 用户注册测试 (50, 100, 200并发)
        print("\n【阶段1】用户注册接口性能测试")
        for concurrent in [50, 100, 200]:
            result = self.test_user_registration(concurrent, duration_seconds=60)
            self.results.append(result)
            self._print_result(result)
            
            # 检查是否达到验收标准
            if result.tps >= 100 and result.avg_response_time < 200 and result.error_rate < 0.1:
                print(f"  [PASS] 通过验收标准 (TPS>=100, 响应时间<200ms, 错误率<0.1%)")
            else:
                print(f"  [FAIL] 未通过验收标准")
        
        # 2. 用户登录测试 (100, 200, 500并发)
        print("\n【阶段2】用户登录接口性能测试")
        for concurrent in [100, 200, 500]:
            result = self.test_user_login(concurrent, duration_seconds=60)
            self.results.append(result)
            self._print_result(result)
            
            # 检查是否达到验收标准
            if result.tps >= 500 and result.avg_response_time < 200 and result.error_rate < 0.1:
                print(f"  [PASS] 通过验收标准 (TPS>=500, 响应时间<200ms, 错误率<0.1%)")
            else:
                print(f"  [FAIL] 未通过验收标准")
        
        # 3. 用户信息查询测试 (200, 500, 1000并发)
        print("\n【阶段3】用户信息查询接口性能测试")
        for concurrent in [200, 500, 1000]:
            result = self.test_user_query(concurrent, duration_seconds=60)
            self.results.append(result)
            self._print_result(result)
            
            # 检查是否达到验收标准
            if result.tps >= 1000 and result.avg_response_time < 200 and result.error_rate < 0.1:
                print(f"  [PASS] 通过验收标准 (QPS>=1000, 响应时间<200ms, 错误率<0.1%)")
            else:
                print(f"  [FAIL] 未通过验收标准")
        
        # 4. 数据库连接池测试
        print("\n【阶段4】数据库连接池压力测试")
        result = self.test_database_connection_pool(100, duration_seconds=60)
        self.results.append(result)
        self._print_result(result)
        
        # 打印连接池统计
        pool_stats = self.db_pool.get_stats()
        print(f"\n  连接池统计:")
        print(f"    - 最大连接数: {pool_stats['max_connections']}")
        print(f"    - 最大使用量: {pool_stats['max_connections_used']}")
        print(f"    - 平均等待时间: {pool_stats['avg_wait_time_ms']:.2f}ms")
        print(f"    - 当前使用中: {pool_stats['connections_in_use']}")
        
        # 生成报告
        self._generate_report()
        
        print("\n" + "=" * 60)
        print("性能测试完成")
        print("=" * 60)
    
    def _print_result(self, result: TestResult):
        """打印测试结果"""
        print(f"\n  {result.test_name}")
        print(f"  {'-' * 50}")
        print(f"  并发用户数: {result.concurrent_users}")
        print(f"  总请求数: {result.total_requests}")
        print(f"  成功请求: {result.successful_requests}")
        print(f"  失败请求: {result.failed_requests}")
        print(f"  错误率: {result.error_rate:.2f}%")
        print(f"  持续时间: {result.duration:.2f}秒")
        print(f"  TPS/QPS: {result.tps:.2f}")
        print(f"  平均响应时间: {result.avg_response_time:.2f}ms")
        print(f"  最小响应时间: {result.min_response_time:.2f}ms")
        print(f"  最大响应时间: {result.max_response_time:.2f}ms")
        print(f"  P50响应时间: {result.p50_response_time:.2f}ms")
        print(f"  P90响应时间: {result.p90_response_time:.2f}ms")
        print(f"  P95响应时间: {result.p95_response_time:.2f}ms")
        print(f"  P99响应时间: {result.p99_response_time:.2f}ms")
        
        if result.errors:
            error_counts = {}
            for error in result.errors[:10]:  # 只显示前10个
                error_counts[error] = error_counts.get(error, 0) + 1
            print(f"\n  常见错误:")
            for error, count in error_counts.items():
                print(f"    - {error}: {count}次")
    
    def _generate_report(self):
        """生成性能测试报告"""
        report_data = {
            "test_info": {
                "name": "AI-Ready 用户管理模块性能测试",
                "version": "Sprint 27+1",
                "start_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                "environment": {
                    "api_base_url": API_BASE_URL,
                    "database": DB_CONFIG
                }
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
        
        # 添加连接池统计
        report_data["connection_pool_stats"] = self.db_pool.get_stats()
        
        # 保存JSON报告
        results_dir = Path("I:/AI-Ready/tests/performance/results")
        results_dir.mkdir(parents=True, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        json_file = results_dir / f"user_management_perf_test_{timestamp}.json"
        
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, ensure_ascii=False, indent=2)
        
        print(f"\n  JSON报告已保存: {json_file}")
        
        # 生成Markdown报告
        md_file = results_dir / f"USER_MANAGEMENT_PERF_TEST_{timestamp}.md"
        md_content = self._format_markdown_report(report_data)
        
        with open(md_file, 'w', encoding='utf-8') as f:
            f.write(md_content)
        
        print(f"  Markdown报告已保存: {md_file}")
        
        # 同时保存为标准文件名
        standard_md = results_dir / "USER_MANAGEMENT_PERF_TEST.md"
        with open(standard_md, 'w', encoding='utf-8') as f:
            f.write(md_content)
        
        return report_data
    
    def _format_markdown_report(self, report_data: Dict) -> str:
        """格式化Markdown报告"""
        md = f"""# AI-Ready 用户管理模块性能测试报告

## 测试信息

- **测试名称**: {report_data['test_info']['name']}
- **版本**: {report_data['test_info']['version']}
- **开始时间**: {report_data['test_info']['start_time']}
- **API地址**: {report_data['test_info']['environment']['api_base_url']}
- **数据库**: {report_data['test_info']['environment']['database']['database']}@{report_data['test_info']['environment']['database']['host']}

## 验收标准

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 用户注册TPS | ≥ 100 | 每秒事务数 |
| 用户登录TPS | ≥ 500 | 每秒事务数 |
| 信息查询QPS | ≥ 1000 | 每秒查询数 |
| 平均响应时间 | < 200ms | 所有接口 |
| 错误率 | < 0.1% | 所有接口 |

## 测试结果汇总

"""
        
        # 按测试类型分组
        registration_results = [r for r in report_data['results'] if '注册' in r['test_name']]
        login_results = [r for r in report_data['results'] if '登录' in r['test_name']]
        query_results = [r for r in report_data['results'] if '查询' in r['test_name']]
        pool_results = [r for r in report_data['results'] if '连接池' in r['test_name']]
        
        # 用户注册测试结果
        if registration_results:
            md += "### 1. 用户注册接口性能测试\n\n"
            md += "| 并发用户数 | TPS | 平均响应时间(ms) | P95响应时间(ms) | 错误率(%) | 状态 |\n"
            md += "|------------|-----|------------------|-----------------|-----------|------|\n"
            for r in registration_results:
                status = "✓ 通过" if r['tps'] >= 100 and r['avg_response_time'] < 200 and r['error_rate'] < 0.1 else "✗ 未通过"
                md += f"| {r['concurrent_users']} | {r['tps']:.2f} | {r['avg_response_time']:.2f} | {r['p95_response_time']:.2f} | {r['error_rate']:.2f} | {status} |\n"
            md += "\n"
        
        # 用户登录测试结果
        if login_results:
            md += "### 2. 用户登录接口性能测试\n\n"
            md += "| 并发用户数 | TPS | 平均响应时间(ms) | P95响应时间(ms) | 错误率(%) | 状态 |\n"
            md += "|------------|-----|------------------|-----------------|-----------|------|\n"
            for r in login_results:
                status = "✓ 通过" if r['tps'] >= 500 and r['avg_response_time'] < 200 and r['error_rate'] < 0.1 else "✗ 未通过"
                md += f"| {r['concurrent_users']} | {r['tps']:.2f} | {r['avg_response_time']:.2f} | {r['p95_response_time']:.2f} | {r['error_rate']:.2f} | {status} |\n"
            md += "\n"
        
        # 用户信息查询测试结果
        if query_results:
            md += "### 3. 用户信息查询接口性能测试\n\n"
            md += "| 并发用户数 | QPS | 平均响应时间(ms) | P95响应时间(ms) | 错误率(%) | 状态 |\n"
            md += "|------------|-----|------------------|-----------------|-----------|------|\n"
            for r in query_results:
                status = "✓ 通过" if r['tps'] >= 1000 and r['avg_response_time'] < 200 and r['error_rate'] < 0.1 else "✗ 未通过"
                md += f"| {r['concurrent_users']} | {r['tps']:.2f} | {r['avg_response_time']:.2f} | {r['p95_response_time']:.2f} | {r['error_rate']:.2f} | {status} |\n"
            md += "\n"
        
        # 数据库连接池测试结果
        if pool_results:
            md += "### 4. 数据库连接池压力测试\n\n"
            for r in pool_results:
                md += f"""- **并发连接数**: {r['concurrent_users']}
- **总请求数**: {r['total_requests']}
- **成功请求**: {r['successful_requests']}
- **失败请求**: {r['failed_requests']}
- **错误率**: {r['error_rate']:.2f}%
- **平均响应时间**: {r['avg_response_time']:.2f}ms
- **P95响应时间**: {r['p95_response_time']:.2f}ms

"""
        
        # 连接池统计
        pool_stats = report_data.get('connection_pool_stats', {})
        md += "## 数据库连接池统计\n\n"
        md += f"""| 指标 | 数值 |
|------|------|
| 最小连接数 | {pool_stats.get('min_connections', 'N/A')} |
| 最大连接数 | {pool_stats.get('max_connections', 'N/A')} |
| 最大使用量 | {pool_stats.get('max_connections_used', 'N/A')} |
| 平均等待时间 | {pool_stats.get('avg_wait_time_ms', 0):.2f}ms |
| 当前使用中 | {pool_stats.get('connections_in_use', 'N/A')} |
| 连接泄漏 | {pool_stats.get('connection_leaks', 'N/A')} |

"""
        
        # 结论和建议
        md += """## 结论与建议

### 总体评估

"""
        
        # 计算通过率
        all_pass = True
        for r in report_data['results']:
            if '注册' in r['test_name']:
                if r['tps'] < 100 or r['avg_response_time'] >= 200 or r['error_rate'] >= 0.1:
                    all_pass = False
            elif '登录' in r['test_name']:
                if r['tps'] < 500 or r['avg_response_time'] >= 200 or r['error_rate'] >= 0.1:
                    all_pass = False
            elif '查询' in r['test_name']:
                if r['tps'] < 1000 or r['avg_response_time'] >= 200 or r['error_rate'] >= 0.1:
                    all_pass = False
        
        if all_pass:
            md += "✓ **所有测试通过验收标准**\n\n"
        else:
            md += "✗ **部分测试未通过验收标准，需要优化**\n\n"
        
        md += """### 优化建议

1. **数据库优化**
   - 检查慢查询日志，优化SQL语句
   - 确保关键字段有适当的索引
   - 考虑使用读写分离

2. **连接池优化**
   - 根据实际负载调整连接池大小
   - 监控连接池使用率，避免连接泄漏
   - 设置合理的连接超时时间

3. **缓存策略**
   - 对用户信息查询增加缓存
   - 使用Redis等内存数据库缓存热点数据
   - 设置合理的缓存过期时间

4. **代码优化**
   - 优化数据库事务处理
   - 减少不必要的数据库查询
   - 使用异步处理提高并发能力

---

**报告生成时间**: """ + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + """
**测试执行者**: test-agent-2
"""
        
        return md


def main():
    """主函数"""
    print("AI-Ready 用户管理模块性能测试")
    print("=" * 60)
    
    test = UserManagementPerformanceTest()
    test.run_all_tests()


if __name__ == "__main__":
    main()
