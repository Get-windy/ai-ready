"""
Sprint 27+1 测试环境稳定性测试脚本
测试系统在长时间运行下的稳定性表现
"""

import time
import threading
import statistics
import logging
import random
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
import requests
import json
import psutil
import os

class StabilityTest:
    """稳定性测试类"""
    
    def __init__(self, base_url: str, duration_hours: int = 8, concurrent_users: int = 20):
        self.base_url = base_url
        self.duration_hours = duration_hours
        self.duration_seconds = duration_hours * 3600
        self.concurrent_users = concurrent_users
        
        self.results = []
        self.metrics = {
            'hourly_stats': {},
            'resource_usage': [],
            'error_trends': [],
            'performance_degradation': []
        }
        
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.start_time = None
        self.logger = self._setup_logging()
        
        # API端点配置
        self.endpoints = [
            {
                'name': 'user_list',
                'url': f"{base_url}/api/users",
                'method': 'GET',
                'weight': 3,
                'critical': True
            },
            {
                'name': 'user_create',
                'url': f"{base_url}/api/users",
                'method': 'POST',
                'weight': 1,
                'critical': False
            },
            {
                'name': 'order_list',
                'url': f"{base_url}/api/orders",
                'method': 'GET',
                'weight': 4,
                'critical': True
            },
            {
                'name': 'order_create',
                'url': f"{base_url}/api/orders",
                'method': 'POST',
                'weight': 2,
                'critical': False
            },
            {
                'name': 'inventory_list',
                'url': f"{base_url}/api/inventory/products",
                'method': 'GET',
                'weight': 3,
                'critical': True
            },
            {
                'name': 'inventory_check',
                'url': f"{base_url}/api/inventory/availability",
                'method': 'POST',
                'weight': 1,
                'critical': False
            }
        ]
    
    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        log_dir = "stability_logs"
        os.makedirs(log_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = os.path.join(log_dir, f"stability_test_{timestamp}.log")
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        
        return logging.getLogger(__name__)
    
    def generate_test_data(self, endpoint_name: str, user_id: int) -> Dict[str, Any]:
        """生成测试数据"""
        templates = {
            'user_create': {
                "username": f"stability_user_{user_id}_{int(time.time()) % 10000}",
                "email": f"stability_{user_id}_{int(time.time()) % 10000}@test.com",
                "phone": f"138{user_id:04d}{int(time.time()) % 10000:04d}",
                "age": random.randint(18, 60),
                "active": random.choice([True, False])
            },
            'order_create': {
                "customer_id": f"CUST_STABILITY_{user_id:04d}",
                "items": [
                    {
                        "product_id": f"PROD_STABILITY_{i:03d}",
                        "quantity": random.randint(1, 5),
                        "price": random.randint(10, 100)
                    }
                    for i in range(1, random.randint(2, 5))
                ],
                "shipping_address": "稳定性测试地址",
                "contact_phone": "13800138000",
                "notes": f"稳定性测试订单 {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
            },
            'inventory_check': {
                "items": [
                    {
                        "product_id": f"PROD_STABILITY_{i:03d}",
                        "quantity": random.randint(1, 3)
                    }
                    for i in range(1, random.randint(2, 4))
                ]
            }
        }
        
        return templates.get(endpoint_name, {})
    
    def make_request(self, endpoint: Dict[str, Any], user_id: int) -> Dict[str, Any]:
        """发送HTTP请求"""
        start_time = time.time()
        success = False
        status_code = 0
        error = None
        response_size = 0
        
        try:
            # 准备请求
            data = None
            params = None
            headers = {"Content-Type": "application/json"}
            
            if endpoint['method'] == 'GET':
                params = {
                    "page": random.randint(1, 3),
                    "size": random.randint(5, 20)
                }
            elif endpoint['method'] == 'POST':
                data = self.generate_test_data(endpoint['name'], user_id)
                if data:
                    data = json.dumps(data)
            
            # 发送请求
            timeout = 30  # 稳定性测试使用较长超时时间
            
            if endpoint['method'] == 'GET':
                response = requests.get(
                    endpoint['url'],
                    params=params,
                    headers=headers,
                    timeout=timeout
                )
            elif endpoint['method'] == 'POST':
                response = requests.post(
                    endpoint['url'],
                    data=data,
                    headers=headers,
                    timeout=timeout
                )
            else:
                raise ValueError(f"不支持的HTTP方法: {endpoint['method']}")
            
            status_code = response.status_code
            response_size = len(response.content)
            
            # 判断请求是否成功
            success = 200 <= status_code < 300
            
            if not success:
                error = f"HTTP {status_code}: {response.text[:100]}"
                
        except requests.exceptions.Timeout:
            error = f"请求超时 ({timeout}秒)"
        except requests.exceptions.ConnectionError:
            error = "连接错误"
        except requests.exceptions.RequestException as e:
            error = f"请求异常: {str(e)}"
        except Exception as e:
            error = f"未知错误: {str(e)}"
        
        end_time = time.time()
        response_time = (end_time - start_time) * 1000  # 转换为毫秒
        
        return {
            'user_id': user_id,
            'endpoint': endpoint['name'],
            'method': endpoint['method'],
            'success': success,
            'response_time_ms': response_time,
            'status_code': status_code,
            'response_size_bytes': response_size,
            'error': error,
            'timestamp': datetime.now().isoformat(),
            'elapsed_hours': (time.time() - self.start_time) / 3600 if self.start_time else 0
        }
    
    def select_endpoint(self) -> Dict[str, Any]:
        """根据权重选择API端点"""
        total_weight = sum(endpoint['weight'] for endpoint in self.endpoints)
        random_value = random.randint(1, total_weight)
        
        current_weight = 0
        for endpoint in self.endpoints:
            current_weight += endpoint['weight']
            if random_value <= current_weight:
                return endpoint
        
        return self.endpoints[0]
    
    def stability_user(self, user_id: int):
        """稳定性测试用户线程"""
        request_count = 0
        error_count = 0
        
        while not self.stop_event.is_set():
            try:
                # 选择API端点
                endpoint = self.select_endpoint()
                
                # 发送请求
                result = self.make_request(endpoint, user_id)
                request_count += 1
                
                if not result['success']:
                    error_count += 1
                
                # 记录结果
                with self.lock:
                    self.results.append(result)
                
                # 随机等待时间，模拟真实用户行为
                think_time = random.uniform(1.0, 5.0)  # 稳定性测试使用较长思考时间
                time.sleep(think_time)
                
                # 每100个请求输出一次状态
                if request_count % 100 == 0:
                    self.logger.debug(
                        f"用户 {user_id}: {request_count}请求, "
                        f"{error_count}错误, "
                        f"错误率: {error_count/request_count*100:.2f}%"
                    )
                
            except Exception as e:
                self.logger.error(f"用户 {user_id} 线程错误: {e}")
                time.sleep(5)  # 错误后等待较长时间
    
    def monitor_resources(self, interval: int = 60):
        """监控资源使用率"""
        while not self.stop_event.is_set():
            try:
                # 系统资源
                cpu_percent = psutil.cpu_percent(interval=1)
                memory = psutil.virtual_memory()
                memory_percent = memory.percent
                memory_used_gb = memory.used / (1024**3)
                
                # 磁盘使用率
                disk_usage = None
                try:
                    disk = psutil.disk_usage('/')
                    disk_percent = disk.percent
                    disk_used_gb = disk.used / (1024**3)
                    disk_usage = {
                        'percent': disk_percent,
                        'used_gb': disk_used_gb
                    }
                except:
                    pass
                
                # 网络IO（如果可用）
                net_io = None
                try:
                    net_io_start = psutil.net_io_counters()
                    time.sleep(1)
                    net_io_end = psutil.net_io_counters()
                    
                    net_io = {
                        'bytes_sent_per_sec': (net_io_end.bytes_sent - net_io_start.bytes_sent),
                        'bytes_recv_per_sec': (net_io_end.bytes_recv - net_io_start.bytes_recv),
                        'packets_sent_per_sec': (net_io_end.packets_sent - net_io_start.packets_sent),
                        'packets_recv_per_sec': (net_io_end.packets_recv - net_io_start.packets_recv)
                    }
                except:
                    pass
                
                resource_data = {
                    'timestamp': datetime.now().isoformat(),
                    'elapsed_hours': (time.time() - self.start_time) / 3600 if self.start_time else 0,
                    'cpu_percent': cpu_percent,
                    'memory_percent': memory_percent,
                    'memory_used_gb': memory_used_gb,
                    'disk_usage': disk_usage,
                    'net_io': net_io
                }
                
                with self.lock:
                    self.metrics['resource_usage'].append(resource_data)
                
                # 资源使用率警告
                if cpu_percent > 80:
                    self.logger.warning(f"CPU使用率过高: {cpu_percent}%")
                if memory_percent > 80:
                    self.logger.warning(f"内存使用率过高: {memory_percent}%")
                if disk_usage and disk_usage['percent'] > 80:
                    self.logger.warning(f"磁盘使用率过高: {disk_usage['percent']}%")
                
                time.sleep(interval)
                
            except Exception as e:
                self.logger.error(f"资源监控错误: {e}")
                time.sleep(interval)
    
    def calculate_hourly_stats(self):
        """计算每小时统计"""
        if not self.results:
            return
        
        current_hour = int((time.time() - self.start_time) / 3600) if self.start_time else 0
        
        # 获取最近一小时的结果
        one_hour_ago = time.time() - 3600
        recent_results = [
            r for r in self.results 
            if datetime.fromisoformat(r['timestamp'].replace('Z', '+00:00')).timestamp() >= one_hour_ago
        ]
        
        if not recent_results:
            return
        
        total_requests = len(recent_results)
        successful_requests = sum(1 for r in recent_results if r['success'])
        failed_requests = total_requests - successful_requests
        success_rate = (successful_requests / total_requests * 100) if total_requests > 0 else 0
        
        # 响应时间统计
        successful_times = [r['response_time_ms'] for r in recent_results if r['success']]
        if successful_times:
            avg_response_time = statistics.mean(successful_times)
            p95_response_time = statistics.quantiles(successful_times, n=20)[-1] if len(successful_times) >= 20 else avg_response_time
            max_response_time = max(successful_times)
        else:
            avg_response_time = p95_response_time = max_response_time = 0
        
        # 吞吐量
        if recent_results:
            timestamps = [datetime.fromisoformat(r['timestamp'].replace('Z', '+00:00')).timestamp() for r in recent_results]
            start_time = min(timestamps)
            end_time = max(timestamps)
            duration = end_time - start_time if end_time > start_time else 3600
            
            throughput = total_requests / duration if duration > 0 else 0
        else:
            throughput = 0
        
        # 错误分析
        errors = {}
        for result in recent_results:
            if not result['success'] and result['error']:
                error_type = result['error']
                if error_type not in errors:
                    errors[error_type] = 0
                errors[error_type] += 1
        
        hourly_stats = {
            'hour': current_hour,
            'total_requests': total_requests,
            'successful_requests': successful_requests,
            'failed_requests': failed_requests,
            'success_rate': success_rate,
            'avg_response_time': avg_response_time,
            'p95_response_time': p95_response_time,
            'max_response_time': max_response_time,
            'throughput': throughput,
            'errors': errors,
            'timestamp': datetime.now().isoformat()
        }
        
        with self.lock:
            self.metrics['hourly_stats'][current_hour] = hourly_stats
        
        # 检测性能下降
        if current_hour >= 1:
            prev_hour_stats = self.metrics['hourly_stats'].get(current_hour - 1)
            if prev_hour_stats:
                # 检查成功率下降
                if success_rate < prev_hour_stats['success_rate'] - 5:
                    degradation = {
                        'hour': current_hour,
                        'metric': 'success_rate',
                        'from': prev_hour_stats['success_rate'],
                        'to': success_rate,
                        'change': success_rate - prev_hour_stats['success_rate'],
                        'timestamp': datetime.now().isoformat()
                    }
                    self.metrics['performance_degradation'].append(degradation)
                    self.logger.warning(f"第{current_hour}小时检测到成功率下降: {degradation['from']:.2f}% → {degradation['to']:.2f}%")
                
                # 检查响应时间上升
                if avg_response_time > prev_hour_stats['avg_response_time'] * 1.5:  # 增长50%
                    degradation = {
                        'hour': current_hour,
                        'metric': 'avg_response_time',
                        'from': prev_hour_stats['avg_response_time'],
                        'to': avg_response_time,
                        'change': avg_response_time - prev_hour_stats['avg_response_time'],
                        'timestamp': datetime.now().isoformat()
                    }
                    self.metrics['performance_degradation'].append(degradation)
                    self.logger.warning(f"第{current_hour}小时检测到响应时间上升: {degradation['from']:.2f}ms → {degradation['to']:.2f}ms")
        
        return hourly_stats
    
    def run_test(self):
        """运行稳定性测试"""
        self.logger.info("=" * 60)
        self.logger.info("开始稳定性测试")
        self.logger.info(f"测试地址: {self.base_url}")
        self.logger.info(f"测试时长: {self.duration_hours}小时")
        self.logger.info(f"并发用户: {self.concurrent_users}")
        self.logger.info(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self.logger.info("=" * 60)
        
        # 重置结果
        self.results = []
        self.metrics = {
            'hourly_stats': {},
            'resource_usage': [],
            'error_trends': [],
            'performance_degradation': []
        }
        self.stop_event.clear()
        self.start_time = time.time()
        
        # 启动资源监控线程
        monitor_thread = threading.Thread(target=self.monitor_resources, args=(300,))  # 5分钟间隔
        monitor_thread.daemon = True
        monitor_thread.start()
        
        # 创建并启动用户线程
        threads = []
        for i in range(self.concurrent_users):
            thread = threading.Thread(target=self.stability_user, args=(i,))
            thread.daemon = True
            thread.start()
            threads.append(thread)
        
        self.logger.info(f"已启动 {len(threads)} 个虚拟用户线程")
        self.logger.info(f"稳定性测试运行中... (将持续{self.duration_hours}小时)")
        
        # 主循环：每小时输出统计
        last_hourly_check = time.time()
        last_status_output = time.time()
        
        while time.time() - self.start_time < self.duration_seconds and not self.stop_event.is_set():
            current_time = time.time()
            elapsed_hours = (current_time - self.start_time) / 3600
            
            # 每小时计算统计
            if current_time - last_hourly_check >= 3600:  # 1小时
                self.logger.info(f"第{int(elapsed_hours)}小时统计...")
                stats = self.calculate_hourly_stats()
                if stats:
                    self.logger.info(
                        f"  请求数: {stats['total_requests']}, "
                        f"成功率: {stats['success_rate']:.2f}%, "
                        f"平均响应时间: {stats['avg_response_time']:.2f}ms"
                    )
                last_hourly_check = current_time
            
            # 每30分钟输出状态
            if current_time - last_status_output >= 1800:  # 30分钟
                with self.lock:
                    total_req = len(self.results)
                    success_req = sum(1 for r in self.results if r['success'])
                
                success_rate = (success_req / total_req * 100) if total_req > 0 else 0
                
                self.logger.info(
                    f"运行状态: {elapsed_hours:.1f}/{self.duration_hours}小时 | "
                    f"总请求数: {total_req} | "
                    f"成功率: {success_rate:.1f}%"
                )
                last_status_output = current_time
            
            time.sleep(60)  # 每分钟检查一次
        
        # 测试完成
        self.stop_event.set()
        self.logger.info("停止稳定性测试...")
        
        # 等待线程结束
        for thread in threads:
            thread.join(timeout=10)
        
        # 计算最终统计
        self.calculate_hourly_stats()
        
        self.logger.info("稳定性测试完成")
        
        # 生成报告
        report = self.generate_report()
        self.logger.info("\n" + report)
        
        # 保存结果
        self.save_results()
        
        return report
    
    def generate_report(self) -> str:
        """生成稳定性测试报告"""
        if not self.results:
            return "没有测试结果"
        
        total_requests = len(self.results)
        successful_requests = sum(1 for r in self.results if r['success'])
        failed_requests = total_requests - successful_requests
        overall_success_rate = (successful_requests / total_requests * 100) if total_requests > 0 else 0
        
        # 总体响应时间统计
        successful_times = [r['response_time_ms'] for r in self.results if r['success']]
        if successful_times:
            overall_avg_response_time = statistics.mean(successful_times)
            overall_p95_response_time = statistics.quantiles(successful_times, n=20)[-1] if len(successful_times) >= 20 else overall_avg_response_time
            overall_max_response_time = max(successful_times)
            overall_min_response_time = min(successful_times)
        else:
            overall_avg_response_time = overall_p95_response_time = 0
            overall_max_response_time = overall_min_response_time = 0
        
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        report = f"""
稳定性测试报告
================
测试时间: {timestamp}
测试配置:
  测试地址: {self.base_url}
  测试时长: {self.duration_hours}小时
  并发用户: {self.concurrent_users}
  总请求数: {total_requests}

总体性能指标:
  ✅ 总成功率: {overall_success_rate:.2f}%
  ⏱️  平均响应时间: {overall_avg_response_time:.2f}ms
  📈 P95响应时间: {overall_p95_response_time:.2f}ms
  ⬆️  最大响应时间: {overall_max_response_time:.2f}ms
  ⬇️  最小响应时间: {overall_min_response_time:.2f}ms
  📊 成功请求: {successful_requests}
  ❌ 失败请求: {failed_requests}

每小时性能趋势:
"""
        
        # 每小时统计
        hours = sorted(self.metrics['hourly_stats'].keys())
        if hours:
            report += "\n小时 | 请求数 | 成功率(%) | 平均响应时间(ms) | P95响应时间(ms) | 吞吐量(请求/秒)\n"
            report += "-" * 90 + "\n"
            
            for hour in hours:
                stats = self.metrics['hourly_stats'][hour]
                report += f"{hour:^4} | {stats['total_requests']:^8} | {stats['success_rate']:^10.2f} | {stats['avg_response_time']:^16.2f} | {stats['p95_response_time']:^16.2f} | {stats['throughput']:^16.2f}\n"
        
        # 性能稳定性分析
        report += "\n性能稳定性分析:\n"
        report += "-" * 60 + "\n"
        
        if hours and len(hours) >= 2:
            first_hour_stats = self.metrics['hourly_stats'][hours[0]]
            last_hour_stats = self.metrics['hourly_stats'][hours[-1]]
            
            # 成功率稳定性
            success_rate_change = last_hour_stats['success_rate'] - first_hour_stats['success_rate']
            if abs(success_rate_change) <= 2:
                report += f"✅ 成功率稳定性: 优秀 (变化{success_rate_change:+.2f}%)\n"
            elif abs(success_rate_change) <= 5:
                report += f"✅ 成功率稳定性: 良好 (变化{success_rate_change:+.2f}%)\n"
            elif abs(success_rate_change) <= 10:
                report += f"⚠️  成功率稳定性: 一般 (变化{success_rate_change:+.2f}%)\n"
            else:
                report += f"❌ 成功率稳定性: 差 (变化{success_rate_change:+.2f}%)\n"
            
            # 响应时间稳定性
            response_time_change_pct = ((last_hour_stats['avg_response_time'] - first_hour_stats['avg_response_time']) / first_hour_stats['avg_response_time'] * 100) if first_hour_stats['avg_response_time'] > 0 else 0
            if abs(response_time_change_pct) <= 10:
                report += f"✅ 响应时间稳定性: 优秀 (变化{response_time_change_pct:+.1f}%)\n"
            elif abs(response_time_change_pct) <= 20:
                report += f"✅ 响应时间稳定性: 良好 (变化{response_time_change_pct:+.1f}%)\n"
            elif abs(response_time_change_pct) <= 50:
                report += f"⚠️  响应时间稳定性: 一般 (变化{response_time_change_pct:+.1f}%)\n"
            else:
                report += f"❌ 响应时间稳定性: 差 (变化{response_time_change_pct:+.1f}%)\n"
        
        # 性能下降检测
        if self.metrics['performance_degradation']:
            report += "\n检测到的性能下降事件:\n"
            for degradation in self.metrics['performance_degradation']:
                report += f"  第{degradation['hour']}小时: {degradation['metric']} "
                report += f"从{degradation['from']:.2f}到{degradation['to']:.2f} "
                report += f"(变化{degradation['change']:+.2f})\n"
        else:
            report += "\n✅ 未检测到明显的性能下降事件\n"
        
        # 错误分析
        report += "\n错误分析:\n"
        report += "-" * 60 + "\n"
        
        # 收集所有错误
        all_errors = {}
        for result in self.results:
            if not result['success'] and result['error']:
                error_type = result['error'].split(':')[0] if ':' in result['error'] else result['error']
                if error_type not in all_errors:
                    all_errors[error_type] = 0
                all_errors[error_type] += 1
        
        if all_errors:
            report += "主要错误类型:\n"
            for error_type, count in sorted(all_errors.items(), key=lambda x: x[1], reverse=True)[:10]:
                percentage = (count / failed_requests * 100) if failed_requests > 0 else 0
                report += f"  • {error_type}: {count}次 ({percentage:.1f}%)\n"
            
            # 错误趋势分析
            error_hours = {}
            for result in self.results:
                if not result['success']:
                    hour = int(result['elapsed_hours'])
                    if hour not in error_hours:
                        error_hours[hour] = 0
                    error_hours[hour] += 1
            
            if error_hours:
                report += "\n错误时间分布:\n"
                for hour in sorted(error_hours.keys()):
                    report += f"  第{hour}小时: {error_hours[hour]}次错误\n"
        else:
            report += "✅ 测试期间未出现系统错误\n"
        
        # 资源使用分析
        if self.metrics['resource_usage']:
            report += "\n资源使用分析:\n"
            report += "-" * 60 + "\n"
            
            cpu_values = [r['cpu_percent'] for r in self.metrics['resource_usage']]
            memory_values = [r['memory_percent'] for r in self.metrics['resource_usage']]
            
            if cpu_values:
                cpu_avg = statistics.mean(cpu_values)
                cpu_max = max(cpu_values)
                cpu_std = statistics.stdev(cpu_values) if len(cpu_values) > 1 else 0
                
                report += f"CPU使用率:\n"
                report += f"  平均: {cpu_avg:.1f}%\n"
                report += f"  最大: {cpu_max:.1f}%\n"
                report += f"  标准差: {cpu_std:.1f}%\n"
                
                if cpu_max > 80:
                    report += f"  ⚠️  检测到CPU使用率峰值过高\n"
            
            if memory_values:
                memory_avg = statistics.mean(memory_values)
                memory_max = max(memory_values)
                memory_std = statistics.stdev(memory_values) if len(memory_values) > 1 else 0
                
                report += f"\n内存使用率:\n"
                report += f"  平均: {memory_avg:.1f}%\n"
                report += f"  最大: {memory_max:.1f}%\n"
                report += f"  标准差: {memory_std:.1f}%\n"
                
                if memory_max > 80:
                    report += f"  ⚠️  检测到内存使用率峰值过高\n"
        
        # 系统稳定性评估
        report += "\n系统稳定性评估:\n"
        report += "-" * 60 + "\n"
        
        stability_score = 100
        
        # 成功率评估
        if overall_success_rate >= 99.9:
            report += "✅ 成功率: 优秀 (≥99.9%)\n"
        elif overall_success_rate >= 99:
            report += "✅ 成功率: 良好 (≥99%)\n"
            stability_score -= 5
        elif overall_success_rate >= 95:
            report += "⚠️  成功率: 一般 (≥95%)\n"
            stability_score -= 15
        else:
            report += "❌ 成功率: 差 (<95%)\n"
            stability_score -= 30
        
        # 响应时间稳定性评估
        if hours and len(hours) >= 2:
            first_avg = self.metrics['hourly_stats'][hours[0]]['avg_response_time']
            last_avg = self.metrics['hourly_stats'][hours[-1]]['avg_response_time']
            change_pct = ((last_avg - first_avg) / first_avg * 100) if first_avg > 0 else 0
            
            if abs(change_pct) <= 10:
                report += "✅ 响应时间稳定性: 优秀 (变化≤10%)\n"
            elif abs(change_pct) <= 20:
                report += "✅ 响应时间稳定性: 良好 (变化≤20%)\n"
                stability_score -= 5
            elif abs(change_pct) <= 50:
                report += "⚠️  响应时间稳定性: 一般 (变化≤50%)\n"
                stability_score -= 10
            else:
                report += "❌ 响应时间稳定性: 差 (变化>50%)\n"
                stability_score -= 20
        
        # 错误率评估
        error_rate = (failed_requests / total_requests * 100) if total_requests > 0 else 0
        if error_rate <= 0.1:
            report += "✅ 错误率: 优秀 (≤0.1%)\n"
        elif error_rate <= 1:
            report += "✅ 错误率: 良好 (≤1%)\n"
            stability_score -= 5
        elif error_rate <= 5:
            report += "⚠️  错误率: 一般 (≤5%)\n"
            stability_score -= 10
        else:
            report += "❌ 错误率: 差 (>5%)\n"
            stability_score -= 25
        
        # 性能下降事件评估
        if self.metrics['performance_degradation']:
            degradation_count = len(self.metrics['performance_degradation'])
            report += f"⚠️  性能下降事件: {degradation_count}次\n"
            stability_score -= degradation_count * 5
        else:
            report += "✅ 性能下降事件: 无\n"
        
        # 总体稳定性评分
        report += f"\n总体稳定性评分: {stability_score:.1f}/100\n"
        
        if stability_score >= 90:
            report += "🎉 系统稳定性: 优秀\n"
        elif stability_score >= 80:
            report += "✅ 系统稳定性: 良好\n"
        elif stability_score >= 70:
            report += "⚠️  系统稳定性: 一般\n"
        else:
            report += "❌ 系统稳定性: 差\n"
        
        # 改进建议
        report += "\n改进建议:\n"
        report += "-" * 60 + "\n"
        
        if overall_success_rate < 95:
            report += "  1. 提升系统整体成功率，优化错误处理\n"
        
        if self.metrics['performance_degradation']:
            report += "  2. 分析性能下降原因，优化长时间运行性能\n"
        
        if error_rate > 5:
            report += "  3. 减少系统错误率，加强监控和告警\n"
        
        if cpu_values and max(cpu_values) > 80:
            report += "  4. 优化CPU使用率，考虑代码优化或资源扩展\n"
        
        if memory_values and max(memory_values) > 80:
            report += "  5. 优化内存使用，检查内存泄漏\n"
        
        report += f"\n详细数据已保存到结果文件。\n"
        
        return report
    
    def save_results(self):
        """保存测试结果到文件"""
        import json
        import os
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_dir = "stability_test_results"
        os.makedirs(results_dir, exist_ok=True)
        
        # 保存详细结果
        results_file = os.path.join(results_dir, f"stability_test_results_{timestamp}.json")
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump({
                'config': {
                    'base_url': self.base_url,
                    'duration_hours': self.duration_hours,
                    'concurrent_users': self.concurrent_users,
                    'start_time': self.start_time
                },
                'results': self.results[:10000],  # 只保存前10000条记录
                'metrics': self.metrics
            }, f, ensure_ascii=False, indent=2)
        
        # 保存报告
        report = self.generate_report()
        report_file = os.path.join(results_dir, f"stability_test_report_{timestamp}.txt")
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        self.logger.info(f"测试结果已保存到: {results_file}")
        self.logger.info(f"测试报告已保存到: {report_file}")

def main():
    """主函数"""
    import sys
    
    # 默认参数
    base_url = "http://localhost:8080"
    duration_hours = 8
    concurrent_users = 20
    
    # 解析命令行参数
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    if len(sys.argv) > 2:
        try:
            duration_hours = int(sys.argv[2])
        except ValueError:
            print(f"无效的测试时长: {sys.argv[2]}")
            return
    
    if len(sys.argv) > 3:
        try:
            concurrent_users = int(sys.argv[3])
        except ValueError:
            print(f"无效的并发用户数: {sys.argv[3]}")
            return
    
    # 运行稳定性测试
    try:
        print(f"开始稳定性测试: {duration_hours}小时")
        print(f"测试地址: {base_url}")
        print(f"并发用户: {concurrent_users}")
        print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)
        
        stability_test = StabilityTest(base_url, duration_hours, concurrent_users)
        report = stability_test.run_test()
        
        print("\n" + "="*60)
        print("稳定性测试完成!")
        print("="*60)
        print(report)
        
    except KeyboardInterrupt:
        print("\n稳定性测试被用户中断")
    except Exception as e:
        print(f"稳定性测试失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # 检查psutil是否可用
    try:
        import psutil
        main()
    except ImportError:
        print("警告: psutil 未安装，资源监控功能不可用")
        print("安装命令: pip install psutil")
        main()  # 仍然运行测试，只是不监控资源