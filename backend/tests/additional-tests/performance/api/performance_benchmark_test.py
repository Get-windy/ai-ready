#!/usr/bin/env python3
"""
性能基准测试脚本
用于测试AI-Ready项目API接口的性能基准
"""

import time
import json
import random
import statistics
import threading
import concurrent.futures
from datetime import datetime
from typing import Dict, List, Tuple, Optional
import requests
import numpy as np
from dataclasses import dataclass, asdict
import matplotlib.pyplot as plt
import pandas as pd

@dataclass
class PerformanceResult:
    """性能测试结果类"""
    endpoint: str
    total_requests: int
    successful_requests: int
    failed_requests: int
    success_rate: float
    min_response_time: float
    max_response_time: float
    avg_response_time: float
    p50_response_time: float
    p90_response_time: float
    p95_response_time: float
    p99_response_time: float
    throughput: float  # 请求/秒
    test_duration: float
    concurrent_users: int

class PerformanceBenchmark:
    """性能基准测试类"""
    
    def __init__(self, base_url: str = "http://localhost:8080", timeout: int = 30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({
            'Content-Type': 'application/json',
            'User-Agent': 'PerformanceBenchmark/1.0'
        })
        self.test_results: Dict[str, PerformanceResult] = {}
        
    def make_request(self, method: str, endpoint: str, data: Optional[dict] = None) -> Tuple[bool, float]:
        """发送HTTP请求并记录响应时间"""
        url = f"{self.base_url}{endpoint}"
        start_time = time.time()
        
        try:
            if method == 'GET':
                response = self.session.get(url, timeout=self.timeout)
            elif method == 'POST':
                response = self.session.post(url, json=data, timeout=self.timeout)
            else:
                raise ValueError(f"Unsupported method: {method}")
            
            response_time = (time.time() - start_time) * 1000  # 转换为毫秒
            success = response.status_code == 200
            return success, response_time
            
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            return False, response_time
    
    def test_user_login(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试用户登录性能"""
        print(f"开始测试用户登录性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        endpoint = "/api/v1/users/login"
        request_data = {
            "username": f"test_user_{random.randint(1, 1000)}",
            "password": "test_password"
        }
        
        return self._run_concurrent_test(
            endpoint_name="user_login",
            endpoint=endpoint,
            method="POST",
            data=request_data,
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def test_user_register(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试用户注册性能"""
        print(f"开始测试用户注册性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        endpoint = "/api/v1/users/register"
        
        return self._run_concurrent_test(
            endpoint_name="user_register",
            endpoint=endpoint,
            method="POST",
            data_generator=lambda: {
                "username": f"user_{random.randint(10000, 99999)}_{int(time.time())}",
                "password": "password123",
                "email": f"test{random.randint(10000, 99999)}@example.com",
                "phone": f"138{random.randint(10000000, 99999999)}"
            },
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def test_order_create(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试订单创建性能"""
        print(f"开始测试订单创建性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        endpoint = "/api/v1/orders"
        
        return self._run_concurrent_test(
            endpoint_name="order_create",
            endpoint=endpoint,
            method="POST",
            data_generator=lambda: {
                "user_id": random.randint(1000, 9999),
                "items": [
                    {
                        "product_id": random.randint(1, 100),
                        "quantity": random.randint(1, 5),
                        "price": random.randint(10, 1000)
                    }
                    for _ in range(random.randint(1, 3))
                ],
                "shipping_address": {
                    "address": f"测试地址{random.randint(1, 100)}号",
                    "city": "测试市",
                    "province": "测试省",
                    "postal_code": str(random.randint(100000, 999999))
                }
            },
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def test_order_query(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试订单查询性能"""
        print(f"开始测试订单查询性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        return self._run_concurrent_test(
            endpoint_name="order_query",
            endpoint_generator=lambda: f"/api/v1/orders/{random.randint(1000, 9999)}",
            method="GET",
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def test_inventory_check(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试库存检查性能"""
        print(f"开始测试库存检查性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        endpoint = "/api/v1/inventory/check"
        
        return self._run_concurrent_test(
            endpoint_name="inventory_check",
            endpoint=endpoint,
            method="POST",
            data_generator=lambda: {
                "product_id": random.randint(1, 100),
                "warehouse_id": random.randint(1, 5)
            },
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def test_inventory_deduct(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试库存扣减性能"""
        print(f"开始测试库存扣减性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        endpoint = "/api/v1/inventory/deduct"
        
        return self._run_concurrent_test(
            endpoint_name="inventory_deduct",
            endpoint=endpoint,
            method="POST",
            data_generator=lambda: {
                "product_id": random.randint(1, 100),
                "quantity": random.randint(1, 10),
                "order_id": random.randint(1000, 9999),
                "deduct_type": "order"
            },
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def test_ai_predict(self, concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """测试AI预测性能"""
        print(f"开始测试AI预测性能，并发用户数：{concurrent_users}，持续时间：{duration_seconds}秒")
        
        endpoint = "/api/v1/ai/predict"
        
        return self._run_concurrent_test(
            endpoint_name="ai_predict",
            endpoint=endpoint,
            method="POST",
            data_generator=lambda: {
                "model_name": "mock_ai_model",
                "input_data": {
                    "text": "这是一个测试文本，用于AI预测性能测试。" * random.randint(1, 5),
                    "features": [random.random() for _ in range(10)]
                },
                "parameters": {
                    "temperature": random.uniform(0.1, 1.0),
                    "max_tokens": random.randint(50, 200)
                }
            },
            concurrent_users=concurrent_users,
            duration_seconds=duration_seconds
        )
    
    def _run_concurrent_test(self, endpoint_name: str, endpoint: str = None, 
                            endpoint_generator: callable = None, method: str = "POST",
                            data: dict = None, data_generator: callable = None,
                            concurrent_users: int = 10, duration_seconds: int = 60) -> PerformanceResult:
        """运行并发性能测试"""
        start_time = time.time()
        end_time = start_time + duration_seconds
        response_times = []
        successful_requests = 0
        total_requests = 0
        
        def worker():
            nonlocal successful_requests, total_requests
            thread_start = time.time()
            
            while time.time() < end_time:
                try:
                    # 生成端点（如果提供了生成器）
                    current_endpoint = endpoint
                    if endpoint_generator:
                        current_endpoint = endpoint_generator()
                    
                    # 生成数据（如果提供了生成器）
                    current_data = data
                    if data_generator:
                        current_data = data_generator()
                    
                    # 发送请求
                    success, response_time = self.make_request(method, current_endpoint, current_data)
                    
                    # 记录结果
                    response_times.append(response_time)
                    total_requests += 1
                    if success:
                        successful_requests += 1
                        
                except Exception as e:
                    response_times.append(0)  # 记录失败请求
                    total_requests += 1
                
                # 添加随机延迟，模拟真实用户行为
                time.sleep(random.uniform(0.01, 0.1))
        
        # 创建并启动工作线程
        threads = []
        for i in range(concurrent_users):
            thread = threading.Thread(target=worker, name=f"Worker-{i}")
            thread.daemon = True
            threads.append(thread)
            thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
        
        # 计算性能指标
        test_duration = time.time() - start_time
        failed_requests = total_requests - successful_requests
        success_rate = successful_requests / total_requests if total_requests > 0 else 0
        
        if response_times:
            response_times_sorted = sorted(response_times)
            min_response_time = min(response_times)
            max_response_time = max(response_times)
            avg_response_time = statistics.mean(response_times)
            p50_response_time = np.percentile(response_times, 50)
            p90_response_time = np.percentile(response_times, 90)
            p95_response_time = np.percentile(response_times, 95)
            p99_response_time = np.percentile(response_times, 99)
            throughput = total_requests / test_duration
        else:
            min_response_time = max_response_time = avg_response_time = 0
            p50_response_time = p90_response_time = p95_response_time = p99_response_time = 0
            throughput = 0
        
        # 创建结果对象
        result = PerformanceResult(
            endpoint=endpoint_name,
            total_requests=total_requests,
            successful_requests=successful_requests,
            failed_requests=failed_requests,
            success_rate=success_rate,
            min_response_time=min_response_time,
            max_response_time=max_response_time,
            avg_response_time=avg_response_time,
            p50_response_time=p50_response_time,
            p90_response_time=p90_response_time,
            p95_response_time=p95_response_time,
            p99_response_time=p99_response_time,
            throughput=throughput,
            test_duration=test_duration,
            concurrent_users=concurrent_users
        )
        
        # 保存结果
        self.test_results[endpoint_name] = result
        
        return result
    
    def run_comprehensive_test_suite(self, concurrent_users: int = 10, duration_seconds: int = 30) -> Dict[str, PerformanceResult]:
        """运行完整的性能测试套件"""
        print(f"开始运行综合性能测试套件，并发用户数：{concurrent_users}，每个测试持续时间：{duration_seconds}秒")
        
        test_suite = [
            ("用户登录", self.test_user_login, concurrent_users, duration_seconds),
            ("用户注册", self.test_user_register, concurrent_users, duration_seconds),
            ("订单创建", self.test_order_create, concurrent_users, duration_seconds),
            ("订单查询", self.test_order_query, concurrent_users, duration_seconds),
            ("库存检查", self.test_inventory_check, concurrent_users, duration_seconds),
            ("库存扣减", self.test_inventory_deduct, concurrent_users, duration_seconds),
            ("AI预测", self.test_ai_predict, concurrent_users, duration_seconds),
        ]
        
        all_results = {}
        
        for test_name, test_func, users, duration in test_suite:
            print(f"\n{'='*60}")
            print(f"执行测试: {test_name}")
            print(f"{'='*60}")
            
            try:
                result = test_func(users, duration)
                all_results[test_name] = result
                
                # 打印测试结果摘要
                self._print_result_summary(result)
                
                # 短暂休息，避免服务过载
                time.sleep(5)
                
            except Exception as e:
                print(f"测试 {test_name} 失败: {e}")
                continue
        
        return all_results
    
    def _print_result_summary(self, result: PerformanceResult):
        """打印测试结果摘要"""
        print(f"\n测试结果摘要 - {result.endpoint}:")
        print(f"  总请求数: {result.total_requests:,}")
        print(f"  成功请求: {result.successful_requests:,} ({result.success_rate*100:.1f}%)")
        print(f"  失败请求: {result.failed_requests:,}")
        print(f"  测试时长: {result.test_duration:.1f}秒")
        print(f"  吞吐量: {result.throughput:.1f} 请求/秒")
        print(f"  响应时间统计:")
        print(f"    最小值: {result.min_response_time:.1f}ms")
        print(f"    平均值: {result.avg_response_time:.1f}ms")
        print(f"    最大值: {result.max_response_time:.1f}ms")
        print(f"    P50: {result.p50_response_time:.1f}ms")
        print(f"    P90: {result.p90_response_time:.1f}ms")
        print(f"    P95: {result.p95_response_time:.1f}ms")
        print(f"    P99: {result.p99_response_time:.1f}ms")
        
        # 根据性能基准评估结果
        self._evaluate_performance(result)
    
    def _evaluate_performance(self, result: PerformanceResult):
        """根据性能基准评估测试结果"""
        # 性能基准标准（根据需求文档）
        performance_benchmarks = {
            "user_login": {
                "avg_rt": 200,
                "p95_rt": 500,
                "success_rate": 99.0
            },
            "user_register": {
                "avg_rt": 300,
                "p95_rt": 800,
                "success_rate": 98.0
            },
            "order_create": {
                "avg_rt": 300,
                "p95_rt": 800,
                "success_rate": 99.0
            },
            "order_query": {
                "avg_rt": 200,
                "p95_rt": 500,
                "success_rate": 99.0
            },
            "inventory_check": {
                "avg_rt": 100,
                "p95_rt": 300,
                "success_rate": 99.0
            },
            "inventory_deduct": {
                "avg_rt": 200,
                "p95_rt": 500,
                "success_rate": 99.9
            },
            "ai_predict": {
                "avg_rt": 500,
                "p95_rt": 2000,
                "success_rate": 96.0
            }
        }
        
        benchmark = performance_benchmarks.get(result.endpoint)
        if benchmark:
            print(f"\n性能评估:")
            
            avg_rt_ok = result.avg_response_time <= benchmark["avg_rt"]
            p95_rt_ok = result.p95_response_time <= benchmark["p95_rt"]
            success_rate_ok = result.success_rate * 100 >= benchmark["success_rate"]
            
            print(f"  平均响应时间: {result.avg_response_time:.1f}ms {'✅' if avg_rt_ok else '❌'} (标准: ≤{benchmark['avg_rt']}ms)")
            print(f"  P95响应时间: {result.p95_response_time:.1f}ms {'✅' if p95_rt_ok else '❌'} (标准: ≤{benchmark['p95_rt']}ms)")
            print(f"  成功率: {result.success_rate*100:.1f}% {'✅' if success_rate_ok else '❌'} (标准: ≥{benchmark['success_rate']}%)")
            
            if avg_rt_ok and p95_rt_ok and success_rate_ok:
                print(f"  ✅ 性能达标")
            else:
                print(f"  ⚠️  性能未达标，需要优化")
        else:
            print(f"  ⚠️  无对应的性能基准配置")
    
    def generate_report(self, results: Dict[str, PerformanceResult]) -> str:
        """生成性能测试报告"""
        print(f"\n{'='*80}")
        print(f"性能测试报告")
        print(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'='*80}")
        
        # 汇总统计
        total_requests = sum(r.total_requests for r in results.values())
        total_success = sum(r.successful_requests for r in results.values())
        total_duration = sum(r.test_duration for r in results.values())
        overall_success_rate = total_success / total_requests if total_requests > 0 else 0
        
        print(f"\n汇总统计:")
        print(f"  总测试时长: {total_duration:.1f}秒")
        print(f"  总请求数: {total_requests:,}")
        print(f"  总成功请求: {total_success:,}")
        print(f"  整体成功率: {overall_success_rate*100:.1f}%")
        
        # 详细结果表
        print(f"\n详细性能结果:")
        print(f"{'-'*120}")
        print(f"{'API端点':<20} {'总请求数':>10} {'成功率':>8} {'平均RT':>10} {'P95RT':>10} {'吞吐量':>10} {'并发数':>8} {'评估':>10}")
        print(f"{'-'*120}")
        
        for test_name, result in results.items():
            avg_rt = result.avg_response_time
            p95_rt = result.p95_response_time
            success_rate = result.success_rate * 100
            throughput = result.throughput
            
            # 简单评估
            performance_benchmarks = {
                "用户登录": {"avg_rt": 200, "p95_rt": 500, "success_rate": 99.0},
                "用户注册": {"avg_rt": 300, "p95_rt": 800, "success_rate": 98.0},
                "订单创建": {"avg_rt": 300, "p95_rt": 800, "success_rate": 99.0},
                "订单查询": {"avg_rt": 200, "p95_rt": 500, "success_rate": 99.0},
                "库存检查": {"avg_rt": 100, "p95_rt": 300, "success_rate": 99.0},
                "库存扣减": {"avg_rt": 200, "p95_rt": 500, "success_rate": 99.9},
                "AI预测": {"avg_rt": 500, "p95_rt": 2000, "success_rate": 96.0},
            }
            
            benchmark = performance_benchmarks.get(test_name)
            if benchmark:
                assessment = "✅" if (avg_rt <= benchmark["avg_rt"] and 
                                     p95_rt <= benchmark["p95_rt"] and 
                                     success_rate >= benchmark["success_rate"]) else "⚠️"
            else:
                assessment = "N/A"
            
            print(f"{test_name:<20} {result.total_requests:>10,} {success_rate:>7.1f}% "
                  f"{avg_rt:>9.1f}ms {p95_rt:>9.1f}ms {throughput:>9.1f} "
                  f"{result.concurrent_users:>8} {assessment:>10}")
        
        print(f"{'-'*120}")
        
        # 生成JSON报告
        report_data = {
            "report_generated_at": datetime.now().isoformat(),
            "test_environment": {
                "base_url": self.base_url,
                "timeout": self.timeout
            },
            "summary": {
                "total_requests": total_requests,
                "total_successful_requests": total_success,
                "overall_success_rate": overall_success_rate,
                "total_test_duration": total_duration
            },
            "detailed_results": {
                test_name: asdict(result) for test_name, result in results.items()
            },
            "performance_assessment": {
                "benchmarks": {
                    "user_login": {"avg_rt": 200, "p95_rt": 500, "success_rate": 99.0},
                    "user_register": {"avg_rt": 300, "p95_rt": 800, "success_rate": 98.0},
                    "order_create": {"avg_rt": 300, "p95_rt": 800, "success_rate": 99.0},
                    "order_query": {"avg_rt": 200, "p95_rt": 500, "success_rate": 99.0},
                    "inventory_check": {"avg_rt": 100, "p95_rt": 300, "success_rate": 99.0},
                    "inventory_deduct": {"avg_rt": 200, "p95_rt": 500, "success_rate": 99.9},
                    "ai_predict": {"avg_rt": 500, "p95_rt": 2000, "success_rate": 96.0},
                }
            }
        }
        
        return report_data
    
    def save_report(self, report_data: dict, filename: str = None):
        """保存测试报告到文件"""
        if filename is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"I:\\AI-Ready\\tests\\performance\\reports\\performance_report_{timestamp}.json"
        
        # 确保目录存在
        import os
        os.makedirs(os.path.dirname(filename), exist_ok=True)
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(report_data, f, ensure_ascii=False, indent=2)
        
        print(f"\n测试报告已保存到: {filename}")
        
        # 同时保存为markdown格式
        md_filename = filename.replace('.json', '.md')
        self._save_markdown_report(report_data, md_filename)
    
    def _save_markdown_report(self, report_data: dict, filename: str):
        """保存Markdown格式的测试报告"""
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(f"# API性能测试报告\n\n")
            f.write(f"**生成时间**: {report_data['report_generated_at']}\n\n")
            f.write(f"**测试环境**: {report_data['test_environment']['base_url']}\n\n")
            
            # 汇总统计
            f.write(f"## 📊 汇总统计\n\n")
            summary = report_data['summary']
            f.write(f"- **总请求数**: {summary['total_requests']:,}\n")
            f.write(f"- **成功请求数**: {summary['total_successful_requests']:,}\n")
            f.write(f"- **整体成功率**: {summary['overall_success_rate']*100:.1f}%\n")
            f.write(f"- **总测试时长**: {summary['total_test_duration']:.1f}秒\n\n")
            
            # 详细结果
            f.write(f"## 📈 详细性能结果\n\n")
            f.write(f"| API端点 | 总请求数 | 成功率 | 平均RT(ms) | P95 RT(ms) | 吞吐量(请求/秒) | 并发数 | 评估 |\n")
            f.write(f"|---------|----------|--------|------------|------------|----------------|--------|------|\n")
            
            for test_name, result in report_data['detailed_results'].items():
                avg_rt = result['avg_response_time']
                p95_rt = result['p95_response_time']
                success_rate = result['success_rate'] * 100
                throughput = result['throughput']
                
                # 评估
                benchmark = report_data['performance_assessment']['benchmarks'].get(test_name)
                if benchmark:
                    assessment = "✅ 达标" if (avg_rt <= benchmark['avg_rt'] and 
                                              p95_rt <= benchmark['p95_rt'] and 
                                              success_rate >= benchmark['success_rate']) else "⚠️ 未达标"
                else:
                    assessment = "N/A"
                
                f.write(f"| {test_name} | {result['total_requests']:,} | {success_rate:.1f}% | {avg_rt:.1f} | {p95_rt:.1f} | {throughput:.1f} | {result['concurrent_users']} | {assessment} |\n")
            
            # 性能基准
            f.write(f"\n## 🎯 性能基准\n\n")
            f.write(f"| API端点 | 平均RT标准 | P95 RT标准 | 成功率标准 |\n")
            f.write(f"|---------|------------|------------|------------|\n")
            
            for endpoint, benchmark in report_data['performance_assessment']['benchmarks'].items():
                f.write(f"| {endpoint} | ≤{benchmark['avg_rt']}ms | ≤{benchmark['p95_rt']}ms | ≥{benchmark['success_rate']}% |\n")
            
            # 结论和建议
            f.write(f"\n## 📋 结论与建议\n\n")
            f.write(f"### 测试结论\n\n")
            
            # 统计达标情况
            passed_tests = 0
            total_tests = len(report_data['detailed_results'])
            
            for test_name, result in report_data['detailed_results'].items():
                benchmark = report_data['performance_assessment']['benchmarks'].get(test_name)
                if benchmark:
                    if (result['avg_response_time'] <= benchmark['avg_rt'] and
                        result['p95_response_time'] <= benchmark['p95_rt'] and
                        result['success_rate'] * 100 >= benchmark['success_rate']):
                        passed_tests += 1
            
            pass_rate = (passed_tests / total_tests) * 100 if total_tests > 0 else 0
            f.write(f"1. **总体通过率**: {passed_tests}/{total_tests} ({pass_rate:.1f}%)\n")
            f.write(f"2. **整体性能评估**: {'✅ 性能达标' if pass_rate >= 80 else '⚠️ 需要优化'}\n")
            f.write(f"3. **系统吞吐量**: {summary['total_requests']/summary['total_test_duration']:.1f} 请求/秒\n\n")
            
            f.write(f"### 建议\n\n")
            if pass_rate >= 90:
                f.write(f"1. ✅ 系统性能表现良好，满足业务需求\n")
                f.write(f"2. ✅ 可考虑进一步优化用户体验\n")
                f.write(f"3. ✅ 建议定期进行性能监控\n")
            elif pass_rate >= 70:
                f.write(f"1. ⚠️ 系统基本满足性能要求，但存在优化空间\n")
                f.write(f"2. ⚠️ 建议关注未达标的API接口\n")
                f.write(f"3. ⚠️ 建议进行针对性优化\n")
            else:
                f.write(f"1. ❌ 系统性能未达标，需要重点优化\n")
                f.write(f"2. ❌ 建议进行性能瓶颈分析\n")
                f.write(f"3. ❌ 建议优化高延迟API接口\n")
        
        print(f"Markdown报告已保存到: {filename}")

def main():
    """主函数"""
    print("="*80)
    print("AI-Ready Sprint 27+1 测试环境API性能基准测试")
    print("="*80)
    
    # 创建性能测试器
    benchmark = PerformanceBenchmark(base_url="http://localhost:8080")
    
    try:
        # 测试服务连接
        print("测试服务连接...")
        try:
            health_response = requests.get("http://localhost:8080/actuator/health", timeout=5)
            if health_response.status_code == 200:
                print("✅ Mock API Gateway 服务正常")
            else:
                print("⚠️  Mock API Gateway 服务可能有问题")
        except:
            print("❌  无法连接到 Mock API Gateway 服务")
            print("请先启动 Mock 服务: python mock_api_gateway.py")
            return
        
        # 运行综合性能测试
        print("\n开始综合性能测试...")
        results = benchmark.run_comprehensive_test_suite(
            concurrent_users=5,  # 使用较少的并发用户进行mock测试
            duration_seconds=15  # 较短的测试时间
        )
        
        # 生成报告
        if results:
            report_data = benchmark.generate_report(results)
            benchmark.save_report(report_data)
        else:
            print("❌ 没有有效的测试结果")
            
    except KeyboardInterrupt:
        print("\n测试被用户中断")
    except Exception as e:
        print(f"测试过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
    
    print("\n" + "="*80)
    print("性能测试完成")
    print("="*80)

if __name__ == "__main__":
    main()