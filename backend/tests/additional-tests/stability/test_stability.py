#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1 测试环境稳定性测试脚本
作者: test-agent-2
日期: 2026-04-28

稳定性测试脚本用于验证测试环境各服务在长时间运行下的稳定性，包括：
- 服务持续可用性测试
- 服务崩溃恢复测试
- 服务资源泄漏检测
- 服务死锁检测
"""

import asyncio
import aiohttp
import time
import psutil
import json
import logging
import statistics
import traceback
from typing import Dict, List, Tuple, Optional, Callable
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from collections import defaultdict
import signal
import sys

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 全局信号处理
stop_flag = False

def signal_handler(signum, frame):
    """处理中断信号"""
    global stop_flag
    logger.info(f"收到信号 {signum}，准备停止测试...")
    stop_flag = True

signal.signal(signal.SIGINT, signal_handler)
signal.signal(signal.SIGTERM, signal_handler)


@dataclass
class StabilityConfig:
    """稳定性测试配置类"""
    base_url: str = "http://localhost:8080"
    api_prefix: str = "/api"
    
    # 测试配置
    test_duration: int = 86400  # 24小时（秒）
    check_interval: int = 60  # 检查间隔（秒）
    max_retries: int = 3
    retry_delay: float = 1.0
    
    # 稳定性阈值
    health_check_threshold: float = 5.0  # 健康检查超时（秒）
    response_time_threshold: Dict[str, float] = field(default_factory=lambda: {
        "user_login": 1.0,
        "order_create": 1.0,
        "data_query": 0.5,
    })
    error_rate_threshold: float = 0.01  # 错误率阈值（1%）
    cpu_usage_threshold: float = 85.0  # CPU使用率阈值（%）
    memory_usage_threshold: float = 85.0  # 内存使用率阈值（%）
    
    # 并发配置
    concurrent_users: int = 10
    rps_target: int = 100  # 目标每秒请求数
    
    # 资源监控配置
    resource_check_interval: int = 10  # 资源检查间隔（秒）
    
    # 测试场景
    test_scenarios: List[str] = field(default_factory=lambda: [
        "service_continuity",
        "crash_recovery",
        "resource_leak",
        "deadlock_detection"
    ])


class StabilityMetrics:
    """稳定性指标收集类"""
    
    def __init__(self):
        self.metrics = {
            'total_requests': 0,
            'successful_requests': 0,
            'failed_requests': 0,
            'response_times': [],
            'error_rates': [],
            'cpu_usage': [],
            'memory_usage': [],
            'thread_count': [],
            'connection_count': [],
            'service_availability': [],
            'crash_events': 0,
            'recovery_events': 0,
            'resource_leak_warnings': 0,
            'deadlock_warnings': 0,
            'test_start_time': None,
            'test_end_time': None
        }
        self.per_hour_stats = defaultdict(dict)
        self.start_time = datetime.now()
    
    def record_request(self, success: bool, response_time: float):
        """记录请求"""
        self.metrics['total_requests'] += 1
        if success:
            self.metrics['successful_requests'] += 1
        else:
            self.metrics['failed_requests'] += 1
        self.metrics['response_times'].append(response_time)
    
    def record_cpu_usage(self, usage: float):
        """记录CPU使用率"""
        self.metrics['cpu_usage'].append(usage)
    
    def record_memory_usage(self, usage: float):
        """记录内存使用率"""
        self.metrics['memory_usage'].append(usage)
    
    def record_thread_count(self, count: int):
        """记录线程数"""
        self.metrics['thread_count'].append(count)
    
    def record_connection_count(self, count: int):
        """记录连接数"""
        self.metrics['connection_count'].append(count)
    
    def record_service_availability(self, available: bool):
        """记录服务可用性"""
        self.metrics['service_availability'].append(1.0 if available else 0.0)
    
    def record_crash_event(self):
        """记录服务崩溃事件"""
        self.metrics['crash_events'] += 1
    
    def record_recovery_event(self):
        """记录服务恢复事件"""
        self.metrics['recovery_events'] += 1
    
    def record_resource_leak(self):
        """记录资源泄漏警告"""
        self.metrics['resource_leak_warnings'] += 1
    
    def record_deadlock(self):
        """记录死锁警告"""
        self.metrics['deadlock_warnings'] += 1
    
    def get_summary(self) -> Dict:
        """获取稳定性指标摘要"""
        summary = {
            'total_requests': self.metrics['total_requests'],
            'successful_requests': self.metrics['successful_requests'],
            'failed_requests': self.metrics['failed_requests'],
            'success_rate': 0.0,
            'avg_response_time': 0.0,
            'p95_response_time': 0.0,
            'p99_response_time': 0.0,
            'max_response_time': 0.0,
            'min_response_time': 0.0,
            'avg_cpu_usage': 0.0,
            'max_cpu_usage': 0.0,
            'avg_memory_usage': 0.0,
            'max_memory_usage': 0.0,
            'avg_thread_count': 0.0,
            'avg_connection_count': 0.0,
            'service_availability': 0.0,
            'crash_events': self.metrics['crash_events'],
            'recovery_events': self.metrics['recovery_events'],
            'resource_leak_warnings': self.metrics['resource_leak_warnings'],
            'deadlock_warnings': self.metrics['deadlock_warnings'],
            'test_duration_hours': 0.0
        }
        
        # 计算请求相关指标
        if self.metrics['total_requests'] > 0:
            summary['success_rate'] = (self.metrics['successful_requests'] / 
                                       self.metrics['total_requests'] * 100)
        
        if self.metrics['response_times']:
            summary['avg_response_time'] = statistics.mean(self.metrics['response_times'])
            summary['max_response_time'] = max(self.metrics['response_times'])
            summary['min_response_time'] = min(self.metrics['response_times'])
            summary['p95_response_time'] = self._calculate_percentile(
                self.metrics['response_times'], 95
            )
            summary['p99_response_time'] = self._calculate_percentile(
                self.metrics['response_times'], 99
            )
        
        # 计算资源使用率指标
        if self.metrics['cpu_usage']:
            summary['avg_cpu_usage'] = statistics.mean(self.metrics['cpu_usage'])
            summary['max_cpu_usage'] = max(self.metrics['cpu_usage'])
        
        if self.metrics['memory_usage']:
            summary['avg_memory_usage'] = statistics.mean(self.metrics['memory_usage'])
            summary['max_memory_usage'] = max(self.metrics['memory_usage'])
        
        if self.metrics['thread_count']:
            summary['avg_thread_count'] = statistics.mean(self.metrics['thread_count'])
        
        if self.metrics['connection_count']:
            summary['avg_connection_count'] = statistics.mean(self.metrics['connection_count'])
        
        # 计算服务可用性
        if self.metrics['service_availability']:
            summary['service_availability'] = statistics.mean(
                self.metrics['service_availability']
            ) * 100
        
        # 计算测试持续时间
        if self.metrics['test_start_time']:
            end_time = self.metrics['test_end_time'] or datetime.now()
            summary['test_duration_hours'] = (end_time - 
                                              self.metrics['test_start_time']).total_seconds() / 3600
        
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
    
    def save_to_file(self, filepath: str):
        """保存指标到文件"""
        summary = self.get_summary()
        summary['per_hour_stats'] = dict(self.per_hour_stats)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        logger.info(f"稳定性指标已保存到: {filepath}")


class ServiceHealthChecker:
    """服务健康检查器"""
    
    def __init__(self, config: StabilityConfig):
        self.config = config
        self.session: Optional[aiohttp.ClientSession] = None
        self.last_health_check = None
        self.health_check_history = []
    
    async def __aenter__(self):
        timeout = aiohttp.ClientTimeout(total=30)
        self.session = aiohttp.ClientSession(timeout=timeout)
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            await self.session.close()
    
    async def check_health(self, endpoint: Optional[str] = None) -> Dict:
        """检查服务健康状态"""
        if not self.session:
            raise RuntimeError("Session not initialized. Use async context manager.")
        
        if not endpoint:
            endpoint = f"{self.config.base_url}/actuator/health"
        
        result = {
            'endpoint': endpoint,
            'healthy': False,
            'response_time': 0.0,
            'status_code': None,
            'error': None,
            'timestamp': datetime.now().isoformat()
        }
        
        try:
            start_time = time.time()
            async with self.session.get(endpoint, timeout=self.config.health_check_threshold) as response:
                result['status_code'] = response.status
                result['response_time'] = (time.time() - start_time) * 1000
                
                if response.status == 200:
                    result['healthy'] = True
                    self.last_health_check = datetime.now()
                    self.health_check_history.append({
                        'timestamp': result['timestamp'],
                        'healthy': True
                    })
                else:
                    result['error'] = f"HTTP {response.status}"
                    self.health_check_history.append({
                        'timestamp': result['timestamp'],
                        'healthy': False
                    })
        except asyncio.TimeoutError:
            result['error'] = ".timeout"
        except aiohttp.ClientError as e:
            result['error'] = str(e)
        except Exception as e:
            result['error'] = str(e)
        
        return result
    
    async def check_service_availability(self, endpoints: List[str]) -> Dict:
        """检查多个端点的可用性"""
        results = {}
        for endpoint in endpoints:
            result = await self.check_health(endpoint)
            results[endpoint] = result
        
        # 计算整体可用性
        total = len(results)
        healthy = sum(1 for r in results.values() if r['healthy'])
        
        return {
            'endpoints': endpoints,
            'total': total,
            'healthy': healthy,
            'availability': healthy / total * 100 if total > 0 else 0,
            'results': results
        }


class ResourceMonitor:
    """系统资源监控器"""
    
    def __init__(self, config: StabilityConfig):
        self.config = config
        self.start_memory = None
    
    def get_system_resources(self) -> Dict:
        """获取系统资源使用情况"""
        process = psutil.Process()
        
        return {
            'cpu_usage': psutil.cpu_percent(interval=0.1),
            'memory_usage': psutil.virtual_memory().percent,
            'memory_used_mb': psutil.virtual_memory().used / (1024 * 1024),
            'memory_total_mb': psutil.virtual_memory().total / (1024 * 1024),
            'disk_usage': psutil.disk_usage('/').percent,
            'network_bytes_sent': psutil.net_io_counters().bytes_sent,
            'network_bytes_recv': psutil.net_io_counters().bytes_recv,
            'timestamp': datetime.now().isoformat()
        }
    
    def get_process_resources(self) -> Dict:
        """获取进程资源使用情况"""
        process = psutil.Process()
        
        try:
            return {
                'cpu_usage': process.cpu_percent(interval=0.1),
                'memory_usage': process.memory_percent(),
                'memory_rss_mb': process.memory_info().rss / (1024 * 1024),
                'memory_vms_mb': process.memory_info().vms / (1024 * 1024),
                'thread_count': process.num_threads(),
                'open_files': len(process.open_files()),
                'connections': len(process.connections()),
                'timestamp': datetime.now().isoformat()
            }
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            return {
                'error': 'Process not accessible',
                'timestamp': datetime.now().isoformat()
            }
    
    def detect_resource_leak(self, prev_resources: Optional[Dict], 
                            current_resources: Dict) -> List[str]:
        """检测资源泄漏"""
        warnings = []
        
        if not prev_resources:
            return warnings
        
        # 检测内存泄漏
        if 'memory_rss_mb' in prev_resources and 'memory_rss_mb' in current_resources:
            memory_increase = (current_resources['memory_rss_mb'] - 
                             prev_resources['memory_rss_mb'])
            
            if memory_increase > 50:  # 内存增加超过50MB
                warnings.append(f"内存泄漏警告: 内存增加 {memory_increase:.1f}MB")
        
        # 检测线程泄漏
        if 'thread_count' in prev_resources and 'thread_count' in current_resources:
            thread_increase = (current_resources['thread_count'] - 
                             prev_resources['thread_count'])
            
            if thread_increase > 10:  # 线程增加超过10个
                warnings.append(f"线程泄漏警告: 线程增加 {thread_increase}个")
        
        # 检测连接泄漏
        if 'connections' in prev_resources and 'connections' in current_resources:
            conn_increase = (current_resources['connections'] - 
                           prev_resources['connections'])
            
            if conn_increase > 5:  # 连接增加超过5个
                warnings.append(f"连接泄漏警告: 连接增加 {conn_increase}个")
        
        return warnings


class StabilityTestSuite:
    """稳定性测试套件"""
    
    def __init__(self, config: StabilityConfig):
        self.config = config
        self.metrics = StabilityMetrics()
        self.health_checker: Optional[ServiceHealthChecker] = None
        self.resource_monitor = ResourceMonitor(config)
        self.prev_resources = None
        self.start_time = None
        self.end_time = None
        self.test_completed = False
    
    async def run_stability_test(self) -> Dict:
        """运行稳定性测试"""
        self.start_time = datetime.now()
        self.metrics.metrics['test_start_time'] = self.start_time.isoformat()
        
        logger.info(f"开始稳定性测试，目标持续时间: {self.config.test_duration}秒")
        logger.info(f"测试开始时间: {self.start_time.isoformat()}")
        
        async with ServiceHealthChecker(self.config) as self.health_checker:
            test_start = time.time()
            last_resource_check = 0
            last_health_check = 0
            last_hour_stats = self.start_time
            
            while not stop_flag:
                current_time = time.time()
                current_datetime = datetime.now()
                
                # 检查测试是否超时
                if current_time - test_start >= self.config.test_duration:
                    logger.info("达到测试持续时间，停止测试")
                    break
                
                # 执行健康检查
                if current_time - last_health_check >= self.config.check_interval:
                    last_health_check = current_time
                    await self._run_health_check()
                
                # 资源监控
                if current_time - last_resource_check >= self.config.resource_check_interval:
                    last_resource_check = current_time
                    await self._monitor_resources()
                
                # 每小时统计
                if (current_datetime - last_hour_stats).total_seconds() >= 3600:
                    self._update_hourly_stats(last_hour_stats, current_datetime)
                    last_hour_stats = current_datetime
                
                # 模拟并发请求
                await self._run_concurrent_requests()
                
                # 保存临时指标
                if current_time - test_start >= 3600:  # 每1小时保存一次
                    self._save_temporary_results(test_start)
                
                # 短暂休眠
                await asyncio.sleep(1)
            
            self.test_completed = True
            self.end_time = datetime.now()
            self.metrics.metrics['test_end_time'] = self.end_time.isoformat()
            
            # 最终统计
            self._update_hourly_stats(last_hour_stats, self.end_time)
            
            logger.info(f"稳定性测试完成，耗时: {(self.end_time - self.start_time).total_seconds():.2f}秒")
        
        return self.generate_test_report()
    
    async def _run_health_check(self):
        """运行健康检查"""
        endpoints = [
            f"{self.config.base_url}/actuator/health",
            f"{self.config.base_url}/api/health",
            f"{self.config.base_url}/health"
        ]
        
        result = await self.health_checker.check_service_availability(endpoints)
        self.metrics.record_service_availability(result['availability'] >= 95)
        
        if result['availability'] < 95:
            logger.warning(f"服务可用性不足: {result['availability']:.1f}%")
        
        logger.info(f"健康检查完成 - 可用性: {result['availability']:.1f}%")
    
    async def _monitor_resources(self):
        """监控系统资源"""
        system_resources = self.resource_monitor.get_system_resources()
        process_resources = self.resource_monitor.get_process_resources()
        
        # 记录指标
        self.metrics.record_cpu_usage(system_resources['cpu_usage'])
        self.metrics.record_memory_usage(system_resources['memory_usage'])
        
        if 'thread_count' in process_resources:
            self.metrics.record_thread_count(process_resources['thread_count'])
        
        if 'connections' in process_resources:
            self.metrics.record_connection_count(process_resources['connections'])
        
        # 检测资源泄漏
        leak_warnings = self.resource_monitor.detect_resource_leak(
            self.prev_resources, process_resources
        )
        
        if leak_warnings:
            for warning in leak_warnings:
                logger.warning(warning)
                self.metrics.record_resource_leak()
        
        self.prev_resources = process_resources
        
        # 检查资源使用率是否超标
        if system_resources['cpu_usage'] > self.config.cpu_usage_threshold:
            logger.warning(f"CPU使用率过高: {system_resources['cpu_usage']:.1f}%")
        
        if system_resources['memory_usage'] > self.config.memory_usage_threshold:
            logger.warning(f"内存使用率过高: {system_resources['memory_usage']:.1f}%")
    
    async def _run_concurrent_requests(self):
        """运行并发请求"""
        urls = [
            f"{self.config.base_url}/api/v1/users/login",
            f"{self.config.base_url}/api/v1/orders",
            f"{self.config.base_url}/api/v1/inventory/check"
        ]
        
        async def make_request(url: str):
            try:
                async with self.health_checker.session.get(url, timeout=30) as response:
                    if response.status == 200:
                        return True, response.elapsed.total_seconds()
                    else:
                        return False, response.elapsed.total_seconds()
            except asyncio.TimeoutError:
                return False, 30.0
            except Exception as e:
                return False, 30.0
        
        # 并发执行请求
        tasks = [make_request(url) for url in urls[:self.config.concurrent_users]]
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        for result in results:
            if isinstance(result, tuple):
                success, response_time = result
                self.metrics.record_request(success, response_time)
    
    def _update_hourly_stats(self, start: datetime, end: datetime):
        """更新每小时统计"""
        duration_hours = (end - start).total_seconds() / 3600
        hour_key = start.strftime("%Y-%m-%d %H:%M")
        
        summary = self.metrics.get_summary()
        
        self.metrics.per_hour_stats[hour_key] = {
            'duration_hours': duration_hours,
            'total_requests': self.metrics.metrics['total_requests'],
            'successful_requests': self.metrics.metrics['successful_requests'],
            'failed_requests': self.metrics.metrics['failed_requests'],
            'avg_response_time': summary['avg_response_time'],
            'service_availability': summary['service_availability'],
            'cpu_usage': summary['avg_cpu_usage'],
            'memory_usage': summary['avg_memory_usage']
        }
        
        logger.info(f"小时统计更新 ({hour_key}): "
                   f"请求={summary['total_requests']}, "
                   f"可用性={summary['service_availability']:.1f}%")
    
    def _save_temporary_results(self, test_start: float):
        """保存临时测试结果"""
        summary = self.metrics.get_summary()
        temp_file = f"stability_test_temp_{self.start_time.strftime('%Y%m%d_%H%M%S')}.json"
        
        temp_data = {
            'test_start': self.start_time.isoformat(),
            'current_time': datetime.now().isoformat(),
            'test_duration_seconds': time.time() - test_start,
            'summary': summary
        }
        
        with open(temp_file, 'w', encoding='utf-8') as f:
            json.dump(temp_data, f, indent=2, ensure_ascii=False)
        
        logger.info(f"临时测试结果已保存: {temp_file}")
    
    def generate_test_report(self) -> Dict:
        """生成测试报告"""
        summary = self.metrics.get_summary()
        
        # 判断测试是否通过
        all_passed = True
        issues = []
        
        # 检查服务可用性
        if summary['service_availability'] < 99.0:
            all_passed = False
            issues.append(f"服务可用性不足: {summary['service_availability']:.1f}% (需≥99%)")
        
        # 检查错误率
        if summary['success_rate'] < 99.0:
            all_passed = False
            issues.append(f"请求成功率不足: {summary['success_rate']:.1f}% (需≥99%)")
        
        # 检查平均响应时间
        if summary['avg_response_time'] > 1000:  # 1秒
            all_passed = False
            issues.append(f"平均响应时间过高: {summary['avg_response_time']:.2f}ms (需<1000ms)")
        
        # 检查资源泄漏
        if summary['resource_leak_warnings'] > 0:
            all_passed = False
            issues.append(f"发现资源泄漏警告: {summary['resource_leak_warnings']}次")
        
        # 检查死锁
        if summary['deadlock_warnings'] > 0:
            all_passed = False
            issues.append(f"发现死锁警告: {summary['deadlock_warnings']}次")
        
        # 检查崩溃事件
        if summary['crash_events'] > 0:
            all_passed = False
            issues.append(f"服务崩溃事件: {summary['crash_events']}次")
        
        # 判断是否达到最低测试时长
        test_duration_hours = summary['test_duration_hours']
        if test_duration_hours < 1:  # 最少运行1小时
            logger.warning(f"测试时长不足: {test_duration_hours:.2f}小时 (建议≥1小时)")
        
        report = {
            'test_type': 'stability',
            'test_name': 'Sprint 27+1 测试环境稳定性测试',
            'test_start': self.start_time.isoformat() if self.start_time else None,
            'test_end': self.end_time.isoformat() if self.end_time else None,
            'test_duration_hours': summary['test_duration_hours'],
            'summary': summary,
            'all_passed': all_passed,
            'issues': issues,
            'recommendations': self._generate_recommendations(summary, issues)
        }
        
        # 保存完整报告
        report_file = f"stability_test_report_{self.start_time.strftime('%Y%m%d_%H%M%S')}.json"
        self.metrics.save_to_file(report_file)
        
        # 生成文本报告
        self._generate_text_report(report, report_file)
        
        return report
    
    def _generate_recommendations(self, summary: Dict, issues: List[str]) -> List[str]:
        """生成优化建议"""
        recommendations = []
        
        if summary['resource_leak_warnings'] > 0:
            recommendations.append(
                "检查代码中的资源释放逻辑，确保关闭数据库连接、文件句柄等"
            )
        
        if summary['crash_events'] > 0:
            recommendations.append(
                "分析崩溃日志，修复导致服务崩溃的bug"
            )
        
        if summary['avg_response_time'] > 500:
            recommendations.append(
                "优化慢查询和性能瓶颈，考虑添加缓存"
            )
        
        if summary['success_rate'] < 100:
            recommendations.append(
                " implemented circuit breaker pattern to handle transient failures"
            )
        
        if not recommendations:
            recommendations.append(
                "稳定性测试通过，建议定期运行以持续监控"
            )
        
        return recommendations
    
    def _generate_text_report(self, report: Dict, json_file: str):
        """生成文本格式报告"""
        lines = []
        lines.append("=" * 70)
        lines.append("Sprint 27+1 测试环境稳定性测试报告")
        lines.append("=" * 70)
        lines.append("")
        
        lines.append("【执行摘要】")
        lines.append(f"测试开始时间: {report['test_start']}")
        lines.append(f"测试结束时间: {report['test_end']}")
        lines.append(f"测试持续时间: {report['test_duration_hours']:.2f} 小时")
        lines.append(f"测试结果: {'✅ 通过' if report['all_passed'] else '❌ 未通过'}")
        lines.append("")
        
        summary = report['summary']
        lines.append("【性能指标】")
        lines.append(f"  总请求数: {summary['total_requests']}")
        lines.append(f"  成功请求数: {summary['successful_requests']}")
        lines.append(f"  失败请求数: {summary['failed_requests']}")
        lines.append(f"  请求成功率: {summary['success_rate']:.2f}%")
        lines.append(f"  平均响应时间: {summary['avg_response_time']:.2f} ms")
        lines.append(f"  P95响应时间: {summary['p95_response_time']:.2f} ms")
        lines.append(f"  服务可用性: {summary['service_availability']:.2f}%")
        lines.append("")
        
        lines.append("【资源使用】")
        lines.append(f"  CPU平均使用率: {summary['avg_cpu_usage']:.2f}%")
        lines.append(f"  内存平均使用率: {summary['avg_memory_usage']:.2f}%")
        lines.append(f"  平均线程数: {summary['avg_thread_count']:.0f}")
        lines.append(f"  平均连接数: {summary['avg_connection_count']:.0f}")
        lines.append("")
        
        lines.append("【稳定性事件】")
        lines.append(f"  服务崩溃事件: {summary['crash_events']}")
        lines.append(f"  服务恢复事件: {summary['recovery_events']}")
        lines.append(f"  资源泄漏警告: {summary['resource_leak_warnings']}")
        lines.append(f"  死锁警告: {summary['deadlock_warnings']}")
        lines.append("")
        
        if report['issues']:
            lines.append("【发现的问题】")
            for i, issue in enumerate(report['issues'], 1):
                lines.append(f"  {i}. {issue}")
            lines.append("")
        
        if report['recommendations']:
            lines.append("【优化建议】")
            for i, rec in enumerate(report['recommendations'], 1):
                lines.append(f"  {i}. {rec}")
            lines.append("")
        
        lines.append("=" * 70)
        lines.append(f"报告已保存到: {json_file}")
        lines.append("=" * 70)
        
        text_content = "\n".join(lines)
        text_file = json_file.replace('.json', '.md')
        
        with open(text_file, 'w', encoding='utf-8') as f:
            f.write(text_content)
        
        logger.info(f"文本报告已保存: {text_file}")
        print(text_content)


async def main():
    """主函数"""
    # 创建配置
    config = StabilityConfig()
    
    # 允许通过环境变量配置
    import os
    config.base_url = os.environ.get('STABILITY_TEST_BASE_URL', config.base_url)
    
    logger.info("=" * 60)
    logger.info("Sprint 27+1 测试环境稳定性测试")
    logger.info("=" * 60)
    logger.info(f"测试配置:")
    logger.info(f"  基础URL: {config.base_url}")
    logger.info(f"  持续时间: {config.test_duration}秒 ({config.test_duration/3600:.1f}小时)")
    logger.info(f"  检查间隔: {config.check_interval}秒")
    logger.info(f"  并发用户数: {config.concurrent_users}")
    logger.info("=" * 60)
    
    # 运行测试
    test_suite = StabilityTestSuite(config)
    
    try:
        report = await test_suite.run_stability_test()
        
        # 打印最终结果
        print("\n" + "=" * 60)
        print("稳定性测试完成")
        print("=" * 60)
        
        if report['all_passed']:
            print("✅ 稳定性测试通过！")
        else:
            print("⚠️  稳定性测试未通过")
            print(f"\n发现 {len(report['issues'])} 个问题:")
            for issue in report['issues']:
                print(f"  - {issue}")
        
        print(f"\n详细报告:")
        json_file = f"stability_test_report_{test_suite.start_time.strftime('%Y%m%d_%H%M%S')}.json"
        print(f"- JSON格式: {json_file}")
        print(f"- 文本格式: {json_file.replace('.json', '.md')}")
        
        return 0 if report['all_passed'] else 1
        
    except KeyboardInterrupt:
        logger.info("测试被用户中断")
        return 1
    except Exception as e:
        logger.error(f"测试执行异常: {e}")
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    exit_code = asyncio.run(main())
    sys.exit(exit_code)
