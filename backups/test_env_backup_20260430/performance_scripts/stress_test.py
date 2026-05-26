"""
Sprint 27+1 测试环境压力测试脚本
测试系统在极限负载下的性能表现和稳定性
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

class StressTest:
    """压力测试类"""
    
    def __init__(self, base_url: str, max_users: int = 100, duration: int = 300):
        self.base_url = base_url
        self.max_users = max_users
        self.duration = duration
        self.results = []
        self.metrics = {
            'throughput': 0,
            'error_rate': 0,
            'response_times': [],
            'status_codes': {},
            'resource_usage': []
        }
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.logger = self._setup_logging()
        
        # API端点配置
        self.endpoints = [
            {
                'name': 'user_list',
                'url': f"{base_url}/api/users",
                'method': 'GET',
                'weight': 3
            },
            {
                'name': 'user_create',
                'url': f"{base_url}/api/users",
                'method': 'POST',
                'weight': 1
            },
            {
                'name': 'order_list',
                'url': f"{base_url}/api/orders",
                'method': 'GET',
                'weight': 4
            },
            {
                'name': 'order_create',
                'url': f"{base_url}/api/orders",
                'method': 'POST',
                'weight': 2
            },
            {
                'name': 'inventory_list',
                'url': f"{base_url}/api/inventory/products",
                'method': 'GET',
                'weight': 5
            },
            {
                'name': 'inventory_check',
                'url': f"{base_url}/api/inventory/availability",
                'method': 'POST',
                'weight': 1
            }
        ]
        
        # 测试数据模板
        self.test_data_templates = {
            'user_create': {
                "username": "stress_user_{id}",
                "email": "stress_{id}@test.com",
                "phone": "138{id:08d}",
                "age": 25,
                "active": True
            },
            'order_create': {
                "customer_id": "CUST_STRESS_{id}",
                "items": [
                    {
                        "product_id": "PROD_STRESS_{item_id}",
                        "quantity": random.randint(1, 5),
                        "price": random.randint(10, 100)
                    }
                    for item_id in range(1, 4)
                ],
                "shipping_address": "压力测试地址",
                "contact_phone": "13800138000"
            },
            'inventory_check': {
                "items": [
                    {
                        "product_id": "PROD_STRESS_{item_id}",
                        "quantity": random.randint(1, 3)
                    }
                    for item_id in range(1, 4)
                ]
            }
        }
    
    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        log_dir = "performance_logs"
        import os
        os.makedirs(log_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = os.path.join(log_dir, f"stress_test_{timestamp}.log")
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        
        return logging.getLogger(__name__)
    
    def generate_test_data(self, template_name: str, user_id: int) -> Dict[str, Any]:
        """生成测试数据"""
        template = self.test_data_templates.get(template_name, {})
        
        if not template:
            return {}
        
        # 深拷贝模板
        import copy
        data = copy.deepcopy(template)
        
        # 替换模板变量
        if isinstance(data, dict):
            for key, value in data.items():
                if isinstance(value, str):
                    data[key] = value.format(id=user_id, user_id=user_id)
                elif isinstance(value, list):
                    for i, item in enumerate(value):
                        if isinstance(item, dict):
                            for sub_key, sub_value in item.items():
                                if isinstance(sub_value, str):
                                    data[key][i][sub_key] = sub_value.format(
                                        id=user_id, 
                                        user_id=user_id,
                                        item_id=i+1
                                    )
        
        return data
    
    def make_request(self, endpoint: Dict[str, Any], user_id: int) -> Dict[str, Any]:
        """发送HTTP请求"""
        start_time = time.time()
        success = False
        status_code = 0
        error = None
        response_size = 0
        
        try:
            # 准备请求数据和参数
            data = None
            params = None
            headers = {"Content-Type": "application/json"}
            
            if endpoint['method'] == 'GET':
                params = {"page": 1, "size": random.randint(5, 20)}
            elif endpoint['method'] == 'POST':
                data = self.generate_test_data(endpoint['name'], user_id)
                if data:
                    data = json.dumps(data)
            
            # 发送请求
            if endpoint['method'] == 'GET':
                response = requests.get(
                    endpoint['url'],
                    params=params,
                    headers=headers,
                    timeout=15
                )
            elif endpoint['method'] == 'POST':
                response = requests.post(
                    endpoint['url'],
                    data=data,
                    headers=headers,
                    timeout=15
                )
            else:
                raise ValueError(f"不支持的HTTP方法: {endpoint['method']}")
            
            status_code = response.status_code
            response_size = len(response.content)
            
            # 判断请求是否成功
            success = 200 <= status_code < 300
            
            if not success:
                error = f"HTTP {status_code}"
                
        except requests.exceptions.Timeout:
            error = "请求超时 (15秒)"
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
            'timestamp': datetime.now().isoformat()
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
        
        return self.endpoints[0]  # 默认返回第一个
    
    def stress_user(self, user_id: int, ramp_up_time: int = 30):
        """压力测试用户线程"""
        # 等待逐步增加用户数
        time.sleep(random.uniform(0, ramp_up_time))
        
        request_count = 0
        
        while not self.stop_event.is_set():
            try:
                # 选择API端点
                endpoint = self.select_endpoint()
                
                # 发送请求
                result = self.make_request(endpoint, user_id)
                request_count += 1
                
                # 记录结果
                with self.lock:
                    self.results.append(result)
                    
                    # 更新状态码统计
                    status_code = result['status_code']
                    if status_code not in self.metrics['status_codes']:
                        self.metrics['status_codes'][status_code] = 0
                    self.metrics['status_codes'][status_code] += 1
                    
                    # 记录成功的响应时间
                    if result['success']:
                        self.metrics['response_times'].append(result['response_time_ms'])
                
                # 随机等待时间，模拟真实用户行为
                think_time = random.uniform(0.1, 2.0)
                time.sleep(think_time)
                
            except Exception as e:
                self.logger.error(f"用户 {user_id} 发生错误: {e}")
                time.sleep(1)  # 错误后等待
    
    def monitor_resources(self, interval: int = 5):
        """监控资源使用率（模拟）"""
        import psutil
        import os
        
        while not self.stop_event.is_set():
            try:
                # CPU使用率
                cpu_percent = psutil.cpu_percent(interval=1)
                
                # 内存使用率
                memory = psutil.virtual_memory()
                memory_percent = memory.percent
                memory_used_gb = memory.used / (1024**3)
                
                # 进程内存（如果可能）
                process = psutil.Process(os.getpid())
                process_memory_mb = process.memory_info().rss / (1024**2)
                
                # 磁盘使用率（主要分区）
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
                
                resource_data = {
                    'timestamp': datetime.now().isoformat(),
                    'cpu_percent': cpu_percent,
                    'memory_percent': memory_percent,
                    'memory_used_gb': memory_used_gb,
                    'process_memory_mb': process_memory_mb,
                    'disk_usage': disk_usage
                }
                
                with self.lock:
                    self.metrics['resource_usage'].append(resource_data)
                
                time.sleep(interval)
                
            except Exception as e:
                self.logger.error(f"资源监控错误: {e}")
                time.sleep(interval)
    
    def calculate_metrics(self):
        """计算性能指标"""
        if not self.results:
            return
        
        total_requests = len(self.results)
        successful_requests = sum(1 for r in self.results if r['success'])
        failed_requests = total_requests - successful_requests
        
        # 吞吐量（请求/秒）
        if self.results:
            start_time = min(r['timestamp'] for r in self.results)
            end_time = max(r['timestamp'] for r in self.results)
            
            try:
                start_dt = datetime.fromisoformat(start_time.replace('Z', '+00:00'))
                end_dt = datetime.fromisoformat(end_time.replace('Z', '+00:00'))
                duration_seconds = (end_dt - start_dt).total_seconds()
                
                if duration_seconds > 0:
                    self.metrics['throughput'] = total_requests / duration_seconds
            except:
                self.metrics['throughput'] = total_requests / self.duration
        
        # 错误率
        if total_requests > 0:
            self.metrics['error_rate'] = (failed_requests / total_requests) * 100
        
        # 响应时间统计
        successful_times = [r['response_time_ms'] for r in self.results if r['success']]
        if successful_times:
            self.metrics['response_times'] = successful_times
    
    def run_test(self, ramp_up_time: int = 30, monitor_resources: bool = True):
        """运行压力测试"""
        self.logger.info("=" * 60)
        self.logger.info("开始压力测试")
        self.logger.info(f"测试地址: {self.base_url}")
        self.logger.info(f"最大并发用户: {self.max_users}")
        self.logger.info(f"测试时长: {self.duration}秒")
        self.logger.info(f"用户增加时间: {ramp_up_time}秒")
        self.logger.info("=" * 60)
        
        # 重置结果
        self.results = []
        self.metrics = {
            'throughput': 0,
            'error_rate': 0,
            'response_times': [],
            'status_codes': {},
            'resource_usage': []
        }
        self.stop_event.clear()
        
        # 启动资源监控线程
        monitor_thread = None
        if monitor_resources:
            monitor_thread = threading.Thread(target=self.monitor_resources, args=(5,))
            monitor_thread.daemon = True
            monitor_thread.start()
        
        # 创建并启动用户线程
        threads = []
        start_time = time.time()
        
        for i in range(self.max_users):
            thread = threading.Thread(
                target=self.stress_user,
                args=(i, ramp_up_time)
            )
            thread.daemon = True
            thread.start()
            threads.append(thread)
            
            # 分批创建用户，避免瞬间压力
            if i % 10 == 0 and i > 0:
                time.sleep(0.1)
        
        self.logger.info(f"已启动 {len(threads)} 个虚拟用户线程")
        
        # 运行指定时间
        self.logger.info(f"压力测试运行中... (将持续{self.duration}秒)")
        
        # 定时输出进度
        progress_interval = 30
        last_progress_time = start_time
        
        while time.time() - start_time < self.duration:
            current_time = time.time()
            elapsed = current_time - start_time
            
            if current_time - last_progress_time >= progress_interval:
                with self.lock:
                    total_req = len(self.results)
                    success_req = sum(1 for r in self.results if r['success'])
                
                success_rate = (success_req / total_req * 100) if total_req > 0 else 0
                
                self.logger.info(
                    f"进度: {elapsed:.0f}/{self.duration}秒 | "
                    f"请求数: {total_req} | "
                    f"成功率: {success_rate:.1f}%"
                )
                last_progress_time = current_time
            
            time.sleep(1)
        
        # 停止测试
        self.stop_event.set()
        self.logger.info("停止压力测试...")
        
        # 等待线程结束
        for thread in threads:
            thread.join(timeout=5)
        
        if monitor_thread:
            self.stop_event.set()
            monitor_thread.join(timeout=5)
        
        # 计算指标
        self.calculate_metrics()
        
        self.logger.info("压力测试完成")
        
        # 生成报告
        report = self.generate_report()
        self.logger.info("\n" + report)
        
        # 保存结果
        self.save_results()
        
        return report
    
    def generate_report(self) -> str:
        """生成压力测试报告"""
        if not self.results:
            return "没有测试结果"
        
        total_requests = len(self.results)
        successful_requests = sum(1 for r in self.results if r['success'])
        failed_requests = total_requests - successful_requests
        success_rate = (successful_requests / total_requests * 100) if total_requests > 0 else 0
        
        # 响应时间统计
        successful_times = self.metrics['response_times']
        if successful_times:
            avg_response_time = statistics.mean(successful_times)
            p50_response_time = statistics.median(successful_times)
            p90_response_time = statistics.quantiles(successful_times, n=10)[-1]
            p95_response_time = statistics.quantiles(successful_times, n=20)[-1]
            p99_response_time = statistics.quantiles(successful_times, n=100)[-1] if len(successful_times) >= 100 else p95_response_time
            max_response_time = max(successful_times)
            min_response_time = min(successful_times)
            std_dev = statistics.stdev(successful_times) if len(successful_times) > 1 else 0
        else:
            avg_response_time = p50_response_time = p90_response_time = p95_response_time = p99_response_time = 0
            max_response_time = min_response_time = std_dev = 0
        
        # 吞吐量
        throughput = self.metrics['throughput']
        
        # 错误率
        error_rate = self.metrics['error_rate']
        
        # 按端点统计
        endpoints_stats = {}
        for result in self.results:
            endpoint = result['endpoint']
            if endpoint not in endpoints_stats:
                endpoints_stats[endpoint] = {
                    'total': 0,
                    'success': 0,
                    'response_times': []
                }
            
            endpoints_stats[endpoint]['total'] += 1
            if result['success']:
                endpoints_stats[endpoint]['success'] += 1
                endpoints_stats[endpoint]['response_times'].append(result['response_time_ms'])
        
        # 状态码分布
        status_codes = self.metrics['status_codes']
        
        # 资源使用统计
        resource_stats = {}
        if self.metrics['resource_usage']:
            cpu_values = [r['cpu_percent'] for r in self.metrics['resource_usage']]
            memory_values = [r['memory_percent'] for r in self.metrics['resource_usage']]
            
            resource_stats = {
                'cpu_avg': statistics.mean(cpu_values) if cpu_values else 0,
                'cpu_max': max(cpu_values) if cpu_values else 0,
                'memory_avg': statistics.mean(memory_values) if memory_values else 0,
                'memory_max': max(memory_values) if memory_values else 0
            }
        
        # 生成报告
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        report = f"""
压力测试报告
===============
测试时间: {timestamp}
测试配置:
  测试地址: {self.base_url}
  最大并发用户: {self.max_users}
  测试时长: {self.duration}秒
  总请求数: {total_requests}

性能指标摘要:
  ✅ 成功率: {success_rate:.2f}%
  📊 吞吐量: {throughput:.2f} 请求/秒
  ⚠️  错误率: {error_rate:.2f}%
  ⏱️  平均响应时间: {avg_response_time:.2f}ms
  📈 P50响应时间: {p50_response_time:.2f}ms
  📈 P90响应时间: {p90_response_time:.2f}ms
  📈 P95响应时间: {p95_response_time:.2f}ms
  📈 P99响应时间: {p99_response_time:.2f}ms
  ⬆️  最大响应时间: {max_response_time:.2f}ms
  ⬇️  最小响应时间: {min_response_time:.2f}ms
  📐 标准差: {std_dev:.2f}ms

状态码分布:
"""
        
        for code, count in sorted(status_codes.items()):
            percentage = (count / total_requests * 100) if total_requests > 0 else 0
            report += f"  HTTP {code}: {count}次 ({percentage:.1f}%)\n"
        
        report += "\n各端点性能统计:\n"
        
        for endpoint, stats in endpoints_stats.items():
            endpoint_total = stats['total']
            endpoint_success = stats['success']
            endpoint_success_rate = (endpoint_success / endpoint_total * 100) if endpoint_total > 0 else 0
            
            if stats['response_times']:
                endpoint_avg = statistics.mean(stats['response_times'])
                endpoint_p95 = statistics.quantiles(stats['response_times'], n=20)[-1] if len(stats['response_times']) >= 20 else endpoint_avg
            else:
                endpoint_avg = endpoint_p95 = 0
            
            report += f"""
  {endpoint}:
    请求数: {endpoint_total}
    成功率: {endpoint_success_rate:.2f}%
    平均响应时间: {endpoint_avg:.2f}ms
    P95响应时间: {endpoint_p95:.2f}ms
"""
        
        # 资源使用统计
        if resource_stats:
            report += f"""
资源使用统计:
  💻 CPU使用率: 平均{resource_stats['cpu_avg']:.1f}%, 最大{resource_stats['cpu_max']:.1f}%
  🧠 内存使用率: 平均{resource_stats['memory_avg']:.1f}%, 最大{resource_stats['memory_max']:.1f}%
"""
        
        # 错误分析
        errors = {}
        for result in self.results:
            if not result['success'] and result['error']:
                error_type = result['error']
                if error_type not in errors:
                    errors[error_type] = 0
                errors[error_type] += 1
        
        if errors:
            report += "\n错误分析:\n"
            for error_type, count in errors.items():
                percentage = (count / failed_requests * 100) if failed_requests > 0 else 0
                report += f"  {error_type}: {count}次 ({percentage:.1f}%)\n"
        
        # 性能评估和建议
        report += "\n性能评估:\n"
        
        # 成功率评估
        if success_rate >= 99.9:
            report += "  ✅ 成功率优秀 (≥99.9%)\n"
        elif success_rate >= 99:
            report += "  ✅ 成功率良好 (≥99%)\n"
        elif success_rate >= 95:
            report += "  ⚠️  成功率一般 (≥95%)\n"
        else:
            report += "  ❌ 成功率差 (<95%)\n"
        
        # 响应时间评估
        if avg_response_time <= 100:
            report += "  ✅ 平均响应时间优秀 (≤100ms)\n"
        elif avg_response_time <= 500:
            report += "  ✅ 平均响应时间良好 (≤500ms)\n"
        elif avg_response_time <= 1000:
            report += "  ⚠️  平均响应时间一般 (≤1s)\n"
        else:
            report += "  ❌ 平均响应时间差 (>1s)\n"
        
        # P95响应时间评估
        if p95_response_time <= 500:
            report += "  ✅ P95响应时间优秀 (≤500ms)\n"
        elif p95_response_time <= 1000:
            report += "  ✅ P95响应时间良好 (≤1s)\n"
        elif p95_response_time <= 2000:
            report += "  ⚠️  P95响应时间一般 (≤2s)\n"
        else:
            report += "  ❌ P95响应时间差 (>2s)\n"
        
        # 吞吐量评估
        if throughput >= 100:
            report += f"  ✅ 吞吐量优秀 (≥100 请求/秒)\n"
        elif throughput >= 50:
            report += f"  ✅ 吞吐量良好 (≥50 请求/秒)\n"
        elif throughput >= 20:
            report += f"  ⚠️  吞吐量一般 (≥20 请求/秒)\n"
        else:
            report += f"  ❌ 吞吐量差 (<20 请求/秒)\n"
        
        # 系统稳定性评估
        if error_rate <= 0.1:
            report += "  ✅ 系统稳定性优秀 (错误率≤0.1%)\n"
        elif error_rate <= 1:
            report += "  ✅ 系统稳定性良好 (错误率≤1%)\n"
        elif error_rate <= 5:
            report += "  ⚠️  系统稳定性一般 (错误率≤5%)\n"
        else:
            report += "  ❌ 系统稳定性差 (错误率>5%)\n"
        
        # 综合建议
        report += "\n改进建议:\n"
        
        if success_rate < 95:
            report += "  1. 优化系统稳定性，减少错误率\n"
        
        if avg_response_time > 1000:
            report += "  2. 优化响应时间，特别是慢查询\n"
        
        if p95_response_time > 2000:
            report += "  3. 优化P95响应时间，提升用户体验\n"
        
        if throughput < 20:
            report += "  4. 提升系统吞吐量，考虑水平扩展\n"
        
        if error_rate > 5:
            report += "  5. 加强错误处理和监控\n"
        
        if resource_stats and resource_stats['cpu_max'] > 80:
            report += "  6. CPU使用率过高，考虑优化代码或增加资源\n"
        
        if resource_stats and resource_stats['memory_max'] > 80:
            report += "  7. 内存使用率过高，检查内存泄漏\n"
        
        report += "\n详细数据已保存到结果文件。\n"
        
        return report
    
    def save_results(self):
        """保存测试结果到文件"""
        import json
        import os
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_dir = "stress_test_results"
        os.makedirs(results_dir, exist_ok=True)
        
        # 保存原始结果
        results_file = os.path.join(results_dir, f"stress_test_results_{timestamp}.json")
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump({
                'config': {
                    'base_url': self.base_url,
                    'max_users': self.max_users,
                    'duration': self.duration
                },
                'results': self.results,
                'metrics': self.metrics
            }, f, ensure_ascii=False, indent=2)
        
        # 保存报告
        report = self.generate_report()
        report_file = os.path.join(results_dir, f"stress_test_report_{timestamp}.txt")
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        self.logger.info(f"测试结果已保存到: {results_file}")
        self.logger.info(f"测试报告已保存到: {report_file}")

def main():
    """主函数"""
    import sys
    
    # 默认参数
    base_url = "http://localhost:8080"
    max_users = 100
    duration = 300
    ramp_up_time = 30
    
    # 解析命令行参数
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    if len(sys.argv) > 2:
        try:
            max_users = int(sys.argv[2])
        except ValueError:
            print(f"无效的最大用户数: {sys.argv[2]}")
            return
    
    if len(sys.argv) > 3:
        try:
            duration = int(sys.argv[3])
        except ValueError:
            print(f"无效的测试时长: {sys.argv[3]}")
            return
    
    if len(sys.argv) > 4:
        try:
            ramp_up_time = int(sys.argv[4])
        except ValueError:
            print(f"无效的用户增加时间: {sys.argv[4]}")
            return
    
    # 运行压力测试
    try:
        stress_test = StressTest(base_url, max_users, duration)
        report = stress_test.run_test(ramp_up_time)
        
        print("\n" + "="*60)
        print("压力测试完成!")
        print("="*60)
        print(report)
        
    except KeyboardInterrupt:
        print("\n压力测试被用户中断")
    except Exception as e:
        print(f"压力测试失败: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()