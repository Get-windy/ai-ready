#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 29监控告警模块性能测试脚本
作者: qa-lead
日期: 2026-04-27
"""

import asyncio
import aiohttp
import time
import statistics
import json
import logging
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from datetime import datetime, timedelta
import matplotlib.pyplot as plt
import pandas as pd

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@dataclass
class TestConfig:
    """测试配置类"""
    base_url: str = "http://localhost:8080"
    api_prefix: str = "/api"
    monitor_path: str = "/monitor"
    alert_path: str = "/alerts"
    
    # 性能指标阈值
    page_load_threshold: float = 3.0  # 页面加载时间阈值(秒)
    data_refresh_threshold: float = 5.0  # 数据刷新延迟阈值(秒)
    alert_trigger_threshold: float = 10.0  # 告警触发延迟阈值(秒)
    notification_threshold: float = 5.0  # 通知发送延迟阈值(秒)
    api_response_threshold: float = 0.5  # API响应时间阈值(秒)
    
    # 并发测试配置
    concurrent_users: int = 1000
    qps_target: int = 100
    test_duration: int = 300  # 测试持续时间(秒)


class PerformanceMetrics:
    """性能指标收集类"""
    
    def __init__(self):
        self.metrics = {
            'page_load_times': [],
            'api_response_times': [],
            'data_refresh_times': [],
            'alert_trigger_times': [],
            'notification_times': [],
            'concurrent_users': 0,
            'qps': 0,
            'error_count': 0,
            'total_requests': 0
        }
    
    def add_page_load_time(self, time_ms: float):
        """添加页面加载时间"""
        self.metrics['page_load_times'].append(time_ms)
    
    def add_api_response_time(self, time_ms: float):
        """添加API响应时间"""
        self.metrics['api_response_times'].append(time_ms)
    
    def add_data_refresh_time(self, time_ms: float):
        """添加数据刷新时间"""
        self.metrics['data_refresh_times'].append(time_ms)
    
    def add_alert_trigger_time(self, time_ms: float):
        """添加告警触发时间"""
        self.metrics['alert_trigger_times'].append(time_ms)
    
    def add_notification_time(self, time_ms: float):
        """添加通知发送时间"""
        self.metrics['notification_times'].append(time_ms)
    
    def increment_error(self):
        """增加错误计数"""
        self.metrics['error_count'] += 1
    
    def increment_request(self):
        """增加请求计数"""
        self.metrics['total_requests'] += 1
    
    def set_concurrent_users(self, users: int):
        """设置并发用户数"""
        self.metrics['concurrent_users'] = users
    
    def set_qps(self, qps: float):
        """设置QPS"""
        self.metrics['qps'] = qps
    
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
            success_rate = (self.metrics['total_requests'] - self.metrics['error_count']) / self.metrics['total_requests']
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


class PerformanceTestSuite:
    """性能测试套件"""
    
    def __init__(self, config: TestConfig):
        self.config = config
        self.metrics = PerformanceMetrics()
        self.session: Optional[aiohttp.ClientSession] = None
    
    async def __aenter__(self):
        self.session = aiohttp.ClientSession()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            await self.session.close()
    
    async def test_page_load_performance(self) -> bool:
        """测试页面加载性能 (PCT-001 到 PCT-009)"""
        logger.info("开始页面加载性能测试...")
        
        test_cases = [
            ("监控大盘页面", f"{self.config.api_prefix}{self.config.monitor_path}/dashboard"),
            ("告警规则列表", f"{self.config.api_prefix}{self.config.alert_path}/list"),
            ("告警详情页面", f"{self.config.api_prefix}{self.config.alert_path}/detail/1"),
            ("配置页面", f"{self.config.api_prefix}{self.config.monitor_path}/config")
        ]
        
        all_passed = True
        
        for page_name, url in test_cases:
            logger.info(f"测试 {page_name}: {url}")
            
            # 测试单用户访问
            start_time = time.time()
            try:
                async with self.session.get(f"{self.config.base_url}{url}") as response:
                    if response.status == 200:
                        load_time = (time.time() - start_time) * 1000  # 转换为毫秒
                        self.metrics.add_page_load_time(load_time)
                        
                        # 检查是否满足性能要求
                        if load_time > self.config.page_load_threshold * 1000:
                            logger.warning(f"{page_name} 加载时间 {load_time:.2f}ms 超过阈值 {self.config.page_load_threshold*1000:.2f}ms")
                            all_passed = False
                        else:
                            logger.info(f"{page_name} 加载时间: {load_time:.2f}ms (符合要求)")
                    else:
                        logger.error(f"{page_name} 请求失败: HTTP {response.status}")
                        self.metrics.increment_error()
                        all_passed = False
            except Exception as e:
                logger.error(f"{page_name} 测试异常: {e}")
                self.metrics.increment_error()
                all_passed = False
            
            await asyncio.sleep(0.5)  # 避免请求过于频繁
        
        return all_passed
    
    async def test_data_refresh_performance(self) -> bool:
        """测试数据刷新性能 (PCT-010 到 PCT-017)"""
        logger.info("开始数据刷新性能测试...")
        
        all_passed = True
        
        # 测试实时数据刷新
        logger.info("测试实时数据刷新延迟...")
        for i in range(10):
            start_time = time.time()
            try:
                async with self.session.get(
                    f"{self.config.base_url}{self.config.api_prefix}{self.config.monitor_path}/metrics/latest"
                ) as response:
                    if response.status == 200:
                        refresh_time = (time.time() - start_time) * 1000
                        self.metrics.add_data_refresh_time(refresh_time)
                        
                        if refresh_time > self.config.data_refresh_threshold * 1000:
                            logger.warning(f"数据刷新延迟 {refresh_time:.2f}ms 超过阈值 {self.config.data_refresh_threshold*1000:.2f}ms")
                            all_passed = False
            except Exception as e:
                logger.error(f"数据刷新测试异常: {e}")
                self.metrics.increment_error()
                all_passed = False
            
            await asyncio.sleep(0.1)
        
        # 测试手动刷新
        logger.info("测试手动刷新响应时间...")
        start_time = time.time()
        try:
            async with self.session.post(
                f"{self.config.base_url}{self.config.api_prefix}{self.config.monitor_path}/refresh"
            ) as response:
                if response.status == 200:
                    refresh_time = (time.time() - start_time) * 1000
                    if refresh_time > 2000:  # 2秒阈值
                        logger.warning(f"手动刷新响应时间 {refresh_time:.2f}ms 超过阈值 2000ms")
                        all_passed = False
                    else:
                        logger.info(f"手动刷新响应时间: {refresh_time:.2f}ms")
        except Exception as e:
            logger.error(f"手动刷新测试异常: {e}")
            all_passed = False
        
        return all_passed
    
    async def test_alert_processing_performance(self) -> bool:
        """测试告警处理性能 (PCT-018 到 PCT-025)"""
        logger.info("开始告警处理性能测试...")
        
        all_passed = True
        
        # 测试告警规则创建性能
        logger.info("测试告警规则创建性能...")
        alert_data = {
            "name": f"性能测试告警_{int(time.time())}",
            "metricType": "CPU_USAGE",
            "threshold": 80.0,
            "operator": ">",
            "severity": "WARNING",
            "durationSeconds": 300,
            "notificationChannels": "EMAIL",
            "enabled": True
        }
        
        start_time = time.time()
        try:
            async with self.session.post(
                f"{self.config.base_url}{self.config.api_prefix}{self.config.alert_path}/create",
                json=alert_data
            ) as response:
                if response.status == 201:
                    create_time = (time.time() - start_time) * 1000
                    self.metrics.add_alert_trigger_time(create_time)
                    
                    if create_time > self.config.alert_trigger_threshold * 1000:
                        logger.warning(f"告警创建时间 {create_time:.2f}ms 超过阈值 {self.config.alert_trigger_threshold*1000:.2f}ms")
                        all_passed = False
                    else:
                        logger.info(f"告警创建时间: {create_time:.2f}ms")
                else:
                    logger.error(f"告警创建失败: HTTP {response.status}")
                    all_passed = False
        except Exception as e:
            logger.error(f"告警创建测试异常: {e}")
            all_passed = False
        
        # 测试告警列表查询性能
        logger.info("测试告警列表查询性能...")
        start_time = time.time()
        try:
            async with self.session.get(
                f"{self.config.base_url}{self.config.api_prefix}{self.config.alert_path}/list?page=1&size=20"
            ) as response:
                if response.status == 200:
                    query_time = (time.time() - start_time) * 1000
                    self.metrics.add_api_response_time(query_time)
                    
                    if query_time > self.config.api_response_threshold * 1000:
                        logger.warning(f"告警列表查询时间 {query_time:.2f}ms 超过阈值 {self.config.api_response_threshold*1000:.2f}ms")
                        all_passed = False
                    else:
                        logger.info(f"告警列表查询时间: {query_time:.2f}ms")
                else:
                    logger.error(f"告警列表查询失败: HTTP {response.status}")
                    all_passed = False
        except Exception as e:
            logger.error(f"告警列表查询测试异常: {e}")
            all_passed = False
        
        return all_passed
    
    async def test_concurrent_performance(self) -> bool:
        """测试系统并发性能 (PCT-026 到 PCT-033)"""
        logger.info("开始系统并发性能测试...")
        
        all_passed = True
        test_start_time = time.time()
        request_count = 0
        
        # 创建并发测试任务
        async def make_request(request_id: int):
            nonlocal request_count
            try:
                async with self.session.get(
                    f"{self.config.base_url}{self.config.api_prefix}{self.config.monitor_path}/health"
                ) as response:
                    request_count += 1
                    if response.status != 200:
                        logger.warning(f"请求 {request_id} 失败: HTTP {response.status}")
                        self.metrics.increment_error()
                        return False
                    return True
            except Exception as e:
                logger.warning(f"请求 {request_id} 异常: {e}")
                self.metrics.increment_error()
                return False
        
        # 执行并发测试
        logger.info(f"执行 {self.config.concurrent_users} 并发用户测试，持续 {self.config.test_duration} 秒...")
        
        tasks = []
        for i in range(self.config.concurrent_users):
            task = asyncio.create_task(make_request(i))
            tasks.append(task)
        
        # 等待测试完成
        await asyncio.sleep(self.config.test_duration)
        
        # 取消所有任务
        for task in tasks:
            task.cancel()
        
        # 计算QPS
        elapsed_time = time.time() - test_start_time
        qps = request_count / elapsed_time
        self.metrics.set_qps(qps)
        self.metrics.set_concurrent_users(self.config.concurrent_users)
        
        logger.info(f"并发测试完成: {request_count} 请求, {elapsed_time:.2f} 秒, QPS: {qps:.2f}")
        
        # 检查QPS是否达标
        if qps < self.config.qps_target:
            logger.warning(f"系统QPS {qps:.2f} 低于目标 {self.config.qps_target}")
            all_passed = False
        else:
            logger.info(f"系统QPS {qps:.2f} 达到目标 {self.config.qps_target}")
        
        return all_passed
    
    async def run_all_tests(self) -> Dict:
        """运行所有性能测试"""
        logger.info("开始执行Sprint 29监控告警模块性能测试套件...")
        
        test_results = {
            "test_suite": "Sprint 29监控告警模块性能测试",
            "start_time": datetime.now().isoformat(),
            "config": {
                "base_url": self.config.base_url,
                "page_load_threshold": self.config.page_load_threshold,
                "data_refresh_threshold": self.config.data_refresh_threshold,
                "alert_trigger_threshold": self.config.alert_trigger_threshold,
                "notification_threshold": self.config.notification_threshold,
                "api_response_threshold": self.config.api_response_threshold,
                "concurrent_users": self.config.concurrent_users,
                "qps_target": self.config.qps_target
            },
            "results": {}
        }
        
        # 执行各个测试
        tests = [
            ("页面加载性能测试", self.test_page_load_performance),
            ("数据刷新性能测试", self.test_data_refresh_performance),
            ("告警处理性能测试", self.test_alert_processing_performance),
            ("系统并发性能测试", self.test_concurrent_performance)
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
        """生成性能测试报告"""
        report_file = f"performance_test_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        # 保存JSON报告
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(test_results, f, indent=2, ensure_ascii=False)
        
        logger.info(f"性能测试报告已保存到: {report_file}")
        
        # 生成文本摘要
        summary_file = f"performance_test_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(summary_file, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write("Sprint 29监控告警模块性能测试报告\n")
            f.write("=" * 60 + "\n\n")
            
            f.write(f"测试时间: {test_results['start_time']} - {test_results['end_time']}\n")
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
            if metrics.get('api_response_times_avg'):
                f.write(f"API响应平均时间: {metrics['api_response_times_avg']:.2f}ms\n")
                f.write(f"API响应P95时间: {metrics['api_response_times_p95']:.2f}ms\n")
            if metrics.get('data_refresh_times_avg'):
                f.write(f"数据刷新平均时间: {metrics['data_refresh_times_avg']:.2f}ms\n")
            if metrics.get('alert_trigger_times_avg'):
                f.write(f"告警触发平均时间: {metrics['alert_trigger_times_avg']:.2f}ms\n")
            if metrics.get('qps'):
                f.write(f"系统QPS: {metrics['qps']:.2f}\n")
            if metrics.get('success_rate'):
                f.write(f"请求成功率: {metrics['success_rate']:.2f}%\n")
            if metrics.get('concurrent_users'):
                f.write(f"并发用户数: {metrics['concurrent_users']}\n")
            
            f.write("\n结论:\n")
            f.write("-" * 40 + "\n")
            all_passed = all(r.get('passed', False) for r in test_results['results'].values())
            if all_passed:
                f.write("✅ 所有性能测试通过，监控告警模块性能符合Sprint 29要求。\n")
            else:
                f.write("⚠️  部分性能测试未通过，需要进一步优化。\n")
        
        logger.info(f"性能测试摘要已保存到: {summary_file}")


async def main():
    """主函数"""
    # 加载测试配置
    config = TestConfig()
    
    # 运行性能测试套件
    async with PerformanceTestSuite(config) as test_suite:
        results = await test_suite.run_all_tests()
    
    # 打印最终结果
    print("\n" + "=" * 60)
    print("Sprint 29监控告警模块性能测试完成")
    print("=" * 60)
    
    all_passed = all(r.get('passed', False) for r in results['results'].values())
    if all_passed:
        print("✅ 所有性能测试通过！")
    else:
        print("⚠️  部分测试未通过，请查看详细报告。")
    
    print(f"\n详细报告已生成:")
    print(f"- JSON格式报告: performance_test_report_*.json")
    print(f"- 文本摘要: performance_test_summary_*.txt")


if __name__ == "__main__":
    asyncio.run(main())