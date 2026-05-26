#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1 增强版性能测试脚本
包含边界条件测试、压力测试和模拟测试支持
作者: test-agent-2
日期: 2026-04-29
"""

import asyncio
import aiohttp
import time
import statistics
import json
import logging
from typing import Dict, List, Tuple, Optional, Any
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
import random
import string

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class TestMode(Enum):
    """测试模式"""
    REAL = "real"           # 真实环境测试
    MOCK = "mock"           # 模拟环境测试
    HYBRID = "hybrid"       # 混合模式（部分真实，部分模拟）


class BoundaryType(Enum):
    """边界条件类型"""
    MIN = "minimum"         # 最小值
    MAX = "maximum"         # 最大值
    EMPTY = "empty"         # 空值
    NULL = "null"           # 空值
    SPECIAL = "special"     # 特殊字符
    OVERFLOW = "overflow"   # 溢出值
    NEGATIVE = "negative"   # 负值


@dataclass
class TestConfig:
    """增强版测试配置类"""
    # 基础配置
    base_url: str = "http://localhost:8080"
    api_prefix: str = "/api"
    test_mode: TestMode = TestMode.REAL
    
    # 性能指标阈值
    page_load_threshold: float = 3.0
    data_refresh_threshold: float = 5.0
    alert_trigger_threshold: float = 10.0
    notification_threshold: float = 5.0
    api_response_threshold: float = 0.5
    
    # 并发测试配置
    concurrent_users: int = 100
    qps_target: int = 100
    test_duration: int = 300
    ramp_up_time: int = 30
    
    # 边界条件测试配置
    enable_boundary_tests: bool = True
    boundary_test_cases: List[BoundaryType] = field(default_factory=lambda: [
        BoundaryType.MIN, BoundaryType.MAX, BoundaryType.EMPTY, 
        BoundaryType.SPECIAL, BoundaryType.OVERFLOW
    ])
    
    # 压力测试配置
    enable_stress_tests: bool = True
    stress_test_levels: List[int] = field(default_factory=lambda: [200, 500, 1000])
    stress_test_duration: int = 60
    
    # 稳定性测试配置
    enable_stability_tests: bool = True
    stability_test_duration: int = 3600  # 1小时
    stability_interval: int = 60
    
    # Mock配置
    mock_response_delay: float = 0.1
    mock_error_rate: float = 0.05
    mock_timeout_rate: float = 0.02


@dataclass
class BoundaryTestCase:
    """边界条件测试用例"""
    name: str
    boundary_type: BoundaryType
    input_data: Any
    expected_result: str
    description: str


class MockResponseGenerator:
    """模拟响应生成器"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.request_count = 0
    
    def generate_response(self, endpoint: str, method: str = "GET") -> Dict:
        """生成模拟响应"""
        self.request_count += 1
        
        # 模拟错误率
        if random.random() < self.config.mock_error_rate:
            return self._generate_error_response(endpoint)
        
        # 模拟超时
        if random.random() < self.config.mock_timeout_rate:
            time.sleep(self.config.mock_response_delay * 3)
            return self._generate_timeout_response(endpoint)
        
        # 正常响应延迟
        time.sleep(random.uniform(0.01, self.config.mock_response_delay))
        
        # 根据端点生成不同的响应
        if "dashboard" in endpoint:
            return self._generate_dashboard_response()
        elif "alert" in endpoint:
            return self._generate_alert_response()
        elif "metrics" in endpoint:
            return self._generate_metrics_response()
        elif "health" in endpoint:
            return self._generate_health_response()
        else:
            return self._generate_default_response()
    
    def _generate_error_response(self, endpoint: str) -> Dict:
        """生成错误响应"""
        return {
            "status": "error",
            "code": random.choice([500, 502, 503, 504]),
            "message": f"Mock error for {endpoint}",
            "timestamp": datetime.now().isoformat()
        }
    
    def _generate_timeout_response(self, endpoint: str) -> Dict:
        """生成超时响应"""
        return {
            "status": "timeout",
            "code": 408,
            "message": f"Request timeout for {endpoint}",
            "timestamp": datetime.now().isoformat()
        }
    
    def _generate_dashboard_response(self) -> Dict:
        """生成监控大盘响应"""
        return {
            "status": "success",
            "data": {
                "cpu_usage": random.uniform(20, 95),
                "memory_usage": random.uniform(30, 90),
                "disk_usage": random.uniform(40, 85),
                "network_in": random.uniform(100, 10000),
                "network_out": random.uniform(100, 10000),
                "active_connections": random.randint(10, 1000),
                "qps": random.uniform(50, 500)
            },
            "timestamp": datetime.now().isoformat()
        }
    
    def _generate_alert_response(self) -> Dict:
        """生成告警响应"""
        return {
            "status": "success",
            "data": {
                "alerts": [
                    {
                        "id": f"ALERT-{random.randint(1000, 9999)}",
                        "severity": random.choice(["CRITICAL", "WARNING", "INFO"]),
                        "message": f"Mock alert message {i}",
                        "timestamp": datetime.now().isoformat()
                    }
                    for i in range(random.randint(0, 5))
                ]
            },
            "timestamp": datetime.now().isoformat()
        }
    
    def _generate_metrics_response(self) -> Dict:
        """生成指标响应"""
        return {
            "status": "success",
            "data": {
                "metrics": [
                    {
                        "name": f"metric_{i}",
                        "value": random.uniform(0, 100),
                        "unit": random.choice(["ms", "bytes", "count", "percentage"]),
                        "timestamp": datetime.now().isoformat()
                    }
                    for i in range(random.randint(5, 20))
                ]
            },
            "timestamp": datetime.now().isoformat()
        }
    
    def _generate_health_response(self) -> Dict:
        """生成健康检查响应"""
        return {
            "status": "UP",
            "components": {
                "database": {"status": "UP"},
                "redis": {"status": "UP"},
                "diskSpace": {"status": "UP"}
            },
            "timestamp": datetime.now().isoformat()
        }
    
    def _generate_default_response(self) -> Dict:
        """生成默认响应"""
        return {
            "status": "success",
            "data": {"message": "Mock response"},
            "timestamp": datetime.now().isoformat()
        }


class EnhancedPerformanceMetrics:
    """增强版性能指标收集类"""
    
    def __init__(self):
        self.metrics = {
            'page_load_times': [],
            'api_response_times': [],
            'data_refresh_times': [],
            'alert_trigger_times': [],
            'notification_times': [],
            'boundary_test_times': [],
            'stress_test_times': [],
            'stability_test_times': [],
            'concurrent_users': 0,
            'qps': 0,
            'error_count': 0,
            'timeout_count': 0,
            'total_requests': 0,
            'boundary_tests_passed': 0,
            'boundary_tests_failed': 0,
            'stress_tests_passed': 0,
            'stress_tests_failed': 0
        }
    
    def add_metric(self, metric_name: str, value: float):
        """添加指标"""
        if metric_name in self.metrics:
            if isinstance(self.metrics[metric_name], list):
                self.metrics[metric_name].append(value)
        else:
            logger.warning(f"Unknown metric: {metric_name}")
    
    def increment_counter(self, counter_name: str):
        """增加计数器"""
        if counter_name in self.metrics:
            self.metrics[counter_name] += 1
    
    def get_summary(self) -> Dict:
        """获取性能指标摘要"""
        summary = {}
        
        for metric_name, values in self.metrics.items():
            if isinstance(values, list) and values:
                summary[f'{metric_name}_avg'] = statistics.mean(values)
                summary[f'{metric_name}_p95'] = self._calculate_percentile(values, 95)
                summary[f'{metric_name}_p99'] = self._calculate_percentile(values, 99)
                summary[f'{metric_name}_max'] = max(values)
                summary[f'{metric_name}_min'] = min(values)
                summary[f'{metric_name}_count'] = len(values)
            else:
                summary[metric_name] = values
        
        # 计算成功率
        if self.metrics['total_requests'] > 0:
            success_rate = (self.metrics['total_requests'] - self.metrics['error_count'] - self.metrics['timeout_count']) / self.metrics['total_requests']
            summary['success_rate'] = success_rate * 100
        
        return summary
    
    def _calculate_percentile(self, values: List[float], percentile: int) -> float:
        """计算百分位数"""
        if not values:
            return 0.0
        sorted_values = sorted(values)
        index = (percentile / 100) * (len(sorted_values) - 1)
        if index.is_integer():
            return sorted_values[int(index)]
        else:
            lower = sorted_values[int(index)]
            upper = sorted_values[int(index) + 1]
            return lower + (upper - lower) * (index - int(index))


class EnhancedPerformanceTestSuite:
    """增强版性能测试套件"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.metrics = EnhancedPerformanceMetrics()
        self.session: Optional[aiohttp.ClientSession] = None
        self.mock_generator: Optional[MockResponseGenerator] = None
        
        if config.test_mode in [TestMode.MOCK, TestMode.HYBRID]:
            self.mock_generator = MockResponseGenerator(config)
    
    async def __aenter__(self):
        if self.config.test_mode == TestMode.REAL:
            self.session = aiohttp.ClientSession()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            await self.session.close()
    
    async def _make_request(self, method: str, endpoint: str, **kwargs) -> Tuple[bool, Dict, float]:
        """发送请求（支持真实和模拟模式）"""
        start_time = time.time()
        
        try:
            if self.config.test_mode == TestMode.MOCK:
                # 模拟模式
                response_data = self.mock_generator.generate_response(endpoint, method)
                response_time = (time.time() - start_time) * 1000
                
                if response_data.get('status') == 'error':
                    self.metrics.increment_counter('error_count')
                    return False, response_data, response_time
                elif response_data.get('status') == 'timeout':
                    self.metrics.increment_counter('timeout_count')
                    return False, response_data, response_time
                else:
                    return True, response_data, response_time
            else:
                # 真实模式
                url = f"{self.config.base_url}{endpoint}"
                async with self.session.request(method, url, **kwargs) as response:
                    response_time = (time.time() - start_time) * 1000
                    
                    if response.status >= 400:
                        self.metrics.increment_counter('error_count')
                        return False, {"status": response.status}, response_time
                    
                    try:
                        data = await response.json()
                        return True, data, response_time
                    except:
                        return True, {"status": response.status}, response_time
        except asyncio.TimeoutError:
            self.metrics.increment_counter('timeout_count')
            return False, {"error": "Timeout"}, (time.time() - start_time) * 1000
        except Exception as e:
            self.metrics.increment_counter('error_count')
            return False, {"error": str(e)}, (time.time() - start_time) * 1000
    
    def _generate_boundary_test_cases(self) -> List[BoundaryTestCase]:
        """生成边界条件测试用例"""
        test_cases = []
        
        if BoundaryType.MIN in self.config.boundary_test_cases:
            test_cases.append(BoundaryTestCase(
                name="最小值测试",
                boundary_type=BoundaryType.MIN,
                input_data={"page": 1, "size": 1},
                expected_result="success",
                description="测试最小分页参数"
            ))
        
        if BoundaryType.MAX in self.config.boundary_test_cases:
            test_cases.append(BoundaryTestCase(
                name="最大值测试",
                boundary_type=BoundaryType.MAX,
                input_data={"page": 999999, "size": 10000},
                expected_result="error",
                description="测试最大分页参数"
            ))
        
        if BoundaryType.EMPTY in self.config.boundary_test_cases:
            test_cases.append(BoundaryTestCase(
                name="空值测试",
                boundary_type=BoundaryType.EMPTY,
                input_data={"name": "", "description": ""},
                expected_result="error",
                description="测试空字符串参数"
            ))
        
        if BoundaryType.SPECIAL in self.config.boundary_test_cases:
            test_cases.append(BoundaryTestCase(
                name="特殊字符测试",
                boundary_type=BoundaryType.SPECIAL,
                input_data={"name": "<script>alert('xss')</script>", "description": "'; DROP TABLE users; --"},
                expected_result="error",
                description="测试特殊字符和SQL注入"
            ))
        
        if BoundaryType.OVERFLOW in self.config.boundary_test_cases:
            test_cases.append(BoundaryTestCase(
                name="溢出值测试",
                boundary_type=BoundaryType.OVERFLOW,
                input_data={"amount": float('inf'), "count": 2**63},
                expected_result="error",
                description="测试溢出值"
            ))
        
        return test_cases
    
    async def test_page_load_performance(self) -> bool:
        """测试页面加载性能（增强版）"""
        logger.info("开始页面加载性能测试（增强版）...")
        
        test_cases = [
            ("监控大盘页面", f"{self.config.api_prefix}/monitor/dashboard"),
            ("告警规则列表", f"{self.config.api_prefix}/alerts/list"),
            ("告警详情页面", f"{self.config.api_prefix}/alerts/detail/1"),
            ("配置页面", f"{self.config.api_prefix}/monitor/config")
        ]
        
        all_passed = True
        
        for page_name, url in test_cases:
            logger.info(f"测试 {page_name}: {url}")
            
            success, data, load_time = await self._make_request("GET", url)
            
            if success:
                self.metrics.add_metric('page_load_times', load_time)
                
                if load_time > self.config.page_load_threshold * 1000:
                    logger.warning(f"{page_name} 加载时间 {load_time:.2f}ms 超过阈值")
                    all_passed = False
                else:
                    logger.info(f"{page_name} 加载时间: {load_time:.2f}ms")
            else:
                logger.error(f"{page_name} 请求失败")
                all_passed = False
            
            await asyncio.sleep(0.5)
        
        return all_passed
    
    async def test_boundary_conditions(self) -> bool:
        """测试边界条件"""
        if not self.config.enable_boundary_tests:
            logger.info("边界条件测试已禁用")
            return True
        
        logger.info("开始边界条件测试...")
        
        test_cases = self._generate_boundary_test_cases()
        all_passed = True
        
        for test_case in test_cases:
            logger.info(f"测试: {test_case.name} - {test_case.description}")
            
            start_time = time.time()
            success, data, response_time = await self._make_request(
                "POST", 
                f"{self.config.api_prefix}/alerts/create",
                json=test_case.input_data
            )
            
            test_time = (time.time() - start_time) * 1000
            self.metrics.add_metric('boundary_test_times', test_time)
            
            if test_case.expected_result == "success" and success:
                logger.info(f"✓ {test_case.name} 通过")
                self.metrics.increment_counter('boundary_tests_passed')
            elif test_case.expected_result == "error" and not success:
                logger.info(f"✓ {test_case.name} 通过（预期错误）")
                self.metrics.increment_counter('boundary_tests_passed')
            else:
                logger.warning(f"✗ {test_case.name} 失败")
                self.metrics.increment_counter('boundary_tests_failed')
                all_passed = False
            
            await asyncio.sleep(0.2)
        
        return all_passed
    
    async def test_stress_performance(self) -> bool:
        """压力测试"""
        if not self.config.enable_stress_tests:
            logger.info("压力测试已禁用")
            return True
        
        logger.info("开始压力测试...")
        
        all_passed = True
        
        for level in self.config.stress_test_levels:
            logger.info(f"执行压力测试 - {level} 并发用户...")
            
            start_time = time.time()
            success_count = 0
            error_count = 0
            
            async def stress_request(request_id: int):
                nonlocal success_count, error_count
                success, data, response_time = await self._make_request(
                    "GET", 
                    f"{self.config.api_prefix}/monitor/health"
                )
                if success:
                    success_count += 1
                else:
                    error_count += 1
            
            # 创建并发请求
            tasks = [stress_request(i) for i in range(level)]
            await asyncio.gather(*tasks, return_exceptions=True)
            
            elapsed_time = time.time() - start_time
            qps = level / elapsed_time if elapsed_time > 0 else 0
            
            self.metrics.add_metric('stress_test_times', elapsed_time * 1000)
            
            success_rate = success_count / level * 100 if level > 0 else 0
            
            logger.info(f"压力测试 {level} 用户: QPS={qps:.2f}, 成功率={success_rate:.2f}%")
            
            if success_rate < 95:
                logger.warning(f"压力测试 {level} 用户未通过（成功率低于95%）")
                self.metrics.increment_counter('stress_tests_failed')
                all_passed = False
            else:
                logger.info(f"✓ 压力测试 {level} 用户通过")
                self.metrics.increment_counter('stress_tests_passed')
            
            await asyncio.sleep(2)
        
        return all_passed
    
    async def test_stability_performance(self) -> bool:
        """稳定性测试"""
        if not self.config.enable_stability_tests:
            logger.info("稳定性测试已禁用")
            return True
        
        logger.info(f"开始稳定性测试（持续{self.config.stability_test_duration}秒）...")
        
        start_time = time.time()
        test_count = 0
        error_count = 0
        
        while time.time() - start_time < self.config.stability_test_duration:
            success, data, response_time = await self._make_request(
                "GET", 
                f"{self.config.api_prefix}/monitor/health"
            )
            
            test_count += 1
            if not success:
                error_count += 1
            
            self.metrics.add_metric('stability_test_times', response_time)
            
            await asyncio.sleep(self.config.stability_interval)
        
        elapsed_time = time.time() - start_time
        error_rate = error_count / test_count * 100 if test_count > 0 else 0
        
        logger.info(f"稳定性测试完成: {test_count}次请求, 错误率={error_rate:.2f}%")
        
        return error_rate < 5  # 错误率低于5%视为通过
    
    async def run_all_tests(self) -> Dict:
        """运行所有增强版性能测试"""
        logger.info("开始执行Sprint 27+1增强版性能测试套件...")
        
        test_results = {
            "test_suite": "Sprint 27+1增强版性能测试",
            "test_mode": self.config.test_mode.value,
            "start_time": datetime.now().isoformat(),
            "config": {
                "base_url": self.config.base_url,
                "test_mode": self.config.test_mode.value,
                "concurrent_users": self.config.concurrent_users,
                "enable_boundary_tests": self.config.enable_boundary_tests,
                "enable_stress_tests": self.config.enable_stress_tests,
                "enable_stability_tests": self.config.enable_stability_tests
            },
            "results": {}
        }
        
        # 执行各个测试
        tests = [
            ("页面加载性能测试", self.test_page_load_performance),
            ("边界条件测试", self.test_boundary_conditions),
            ("压力测试", self.test_stress_performance),
            ("稳定性测试", self.test_stability_performance)
        ]
        
        for test_name, test_func in tests:
            logger.info(f"=== 开始执行: {test_name} ===")
            try:
                passed = await test_func()
                test_results["results"][test_name] = {
                    "passed": passed,
                    "timestamp": datetime.now().isoformat()
                }
                logger.info(f"=== {test_name} {'通过' if passed else '失败'} ===\n")
            except Exception as e:
                logger.error(f"{test_name} 执行异常: {e}")
                test_results["results"][test_name] = {
                    "passed": False,
                    "error": str(e),
                    "timestamp": datetime.now().isoformat()
                }
        
        # 收集最终性能指标
        test_results["metrics"] = self.metrics.get_summary()
        test_results["end_time"] = datetime.now().isoformat()
        
        # 生成测试报告
        self._generate_report(test_results)
        
        return test_results
    
    def _generate_report(self, test_results: Dict):
        """生成增强版测试报告"""
        report_file = f"enhanced_performance_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(test_results, f, indent=2, ensure_ascii=False)
        
        logger.info(f"增强版性能测试报告已保存到: {report_file}")
        
        # 生成文本摘要
        summary_file = f"enhanced_performance_test_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(summary_file, 'w', encoding='utf-8') as f:
            f.write("=" * 80 + "\n")
            f.write("Sprint 27+1增强版性能测试报告\n")
            f.write("=" * 80 + "\n\n")
            
            f.write(f"测试时间: {test_results['start_time']} - {test_results['end_time']}\n")
            f.write(f"测试模式: {test_results['test_mode']}\n")
            f.write(f"测试环境: {test_results['config']['base_url']}\n\n")
            
            f.write("测试结果:\n")
            f.write("-" * 40 + "\n")
            for test_name, result in test_results['results'].items():
                status = "✓ 通过" if result.get('passed', False) else "✗ 失败"
                f.write(f"{test_name:30} {status}\n")
            
            f.write("\n性能指标:\n")
            f.write("-" * 40 + "\n")
            metrics = test_results['metrics']
            
            if metrics.get('page_load_times_avg'):
                f.write(f"页面加载平均时间: {metrics['page_load_times_avg']:.2f}ms\n")
                f.write(f"页面加载P95时间: {metrics['page_load_times_p95']:.2f}ms\n")
            
            if metrics.get('boundary_test_times_avg'):
                f.write(f"边界测试平均时间: {metrics['boundary_test_times_avg']:.2f}ms\n")
                f.write(f"边界测试通过: {metrics.get('boundary_tests_passed', 0)}\n")
                f.write(f"边界测试失败: {metrics.get('boundary_tests_failed', 0)}\n")
            
            if metrics.get('stress_test_times_avg'):
                f.write(f"压力测试平均时间: {metrics['stress_test_times_avg']:.2f}ms\n")
                f.write(f"压力测试通过: {metrics.get('stress_tests_passed', 0)}\n")
                f.write(f"压力测试失败: {metrics.get('stress_tests_failed', 0)}\n")
            
            if metrics.get('success_rate'):
                f.write(f"请求成功率: {metrics['success_rate']:.2f}%\n")
            
            f.write("\n结论:\n")
            f.write("-" * 40 + "\n")
            all_passed = all(r.get('passed', False) for r in test_results['results'].values())
            if all_passed:
                f.write("✅ 所有增强版性能测试通过！\n")
            else:
                f.write("⚠️  部分测试未通过，需要进一步优化。\n")
        
        logger.info(f"增强版测试摘要已保存到: {summary_file}")


async def main():
    """主函数"""
    # 加载测试配置（使用模拟模式进行独立测试）
    config = TestConfig(
        test_mode=TestMode.MOCK,  # 使用模拟模式
        enable_boundary_tests=True,
        enable_stress_tests=True,
        enable_stability_tests=False  # 简化测试，不执行长时间稳定性测试
    )
    
    # 运行增强版性能测试套件
    async with EnhancedPerformanceTestSuite(config) as test_suite:
        results = await test_suite.run_all_tests()
    
    # 打印最终结果
    print("\n" + "=" * 80)
    print("Sprint 27+1增强版性能测试完成")
    print("=" * 80)
    
    all_passed = all(r.get('passed', False) for r in results['results'].values())
    if all_passed:
        print("✅ 所有增强版性能测试通过！")
    else:
        print("⚠️  部分测试未通过，请查看详细报告。")
    
    print(f"\n详细报告已生成:")
    print(f"- JSON报告: enhanced_performance_test_report_*.json")
    print(f"- 文本摘要: enhanced_performance_test_summary_*.txt")


if __name__ == "__main__":
    asyncio.run(main())
