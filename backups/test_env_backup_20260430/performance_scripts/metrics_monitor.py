"""
Sprint 27+1 测试环境性能指标监控脚本
实时监控系统性能指标并生成报告
"""

import time
import threading
import statistics
import logging
import json
from typing import Dict, List, Any, Optional
from datetime import datetime, timedelta
import requests
import psutil
import os

class MetricsMonitor:
    """性能指标监控类"""
    
    def __init__(self, base_url: str, monitor_duration: int = 3600):
        self.base_url = base_url
        self.monitor_duration = monitor_duration
        self.metrics = {
            'response_times': [],
            'status_codes': {},
            'error_rates': [],
            'resource_usage': [],
            'throughput': [],
            'availability': []
        }
        self.lock = threading.Lock()
        self.stop_event = threading.Event()
        self.start_time = None
        self.logger = self._setup_logging()
        
        # 监控端点
        self.monitor_endpoints = [
            {
                'name': 'user_api',
                'url': f"{base_url}/api/users",
                'method': 'GET',
                'interval': 10,  # 每10秒检查一次
                'critical': True
            },
            {
                'name': 'order_api',
                'url': f"{base_url}/api/orders",
                'method': 'GET',
                'interval': 15,
                'critical': True
            },
            {
                'name': 'inventory_api',
                'url': f"{base_url}/api/inventory/products",
                'method': 'GET',
                'interval': 20,
                'critical': True
            },
            {
                'name': 'health_check',
                'url': f"{base_url}/health",
                'method': 'GET',
                'interval': 30,
                'critical': True
            }
        ]
        
        # 性能阈值
        self.thresholds = {
            'response_time_warning': 1000,  # 1秒警告
            'response_time_critical': 3000,  # 3秒严重
            'error_rate_warning': 5,  # 5%警告
            'error_rate_critical': 10,  # 10%严重
            'cpu_warning': 80,  # 80%警告
            'cpu_critical': 90,  # 90%严重
            'memory_warning': 80,
            'memory_critical': 90,
            'availability_warning': 99,  # 99%警告
            'availability_critical': 95  # 95%严重
        }
        
        # 告警记录
        self.alerts = []
    
    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        log_dir = "metrics_logs"
        os.makedirs(log_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = os.path.join(log_dir, f"metrics_monitor_{timestamp}.log")
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        
        return logging.getLogger(__name__)
    
    def check_endpoint(self, endpoint: Dict[str, Any]) -> Dict[str, Any]:
        """检查单个端点"""
        start_time = time.time()
        success = False
        status_code = 0
        error = None
        response_time = 0
        
        try:
            if endpoint['method'] == 'GET':
                response = requests.get(endpoint['url'], timeout=10)
            else:
                response = requests.post(endpoint['url'], timeout=10)
            
            status_code = response.status_code
            success = 200 <= status_code < 300
            
            if not success:
                error = f"HTTP {status_code}"
                
        except requests.exceptions.Timeout:
            error = "请求超时"
        except requests.exceptions.ConnectionError:
            error = "连接错误"
        except Exception as e:
            error = str(e)
        
        end_time = time.time()
        response_time = (end_time - start_time) * 1000
        
        result = {
            'endpoint': endpoint['name'],
            'success': success,
            'response_time_ms': response_time,
            'status_code': status_code,
            'error': error,
            'timestamp': datetime.now().isoformat()
        }
        
        # 检查阈值并触发告警
        self._check_thresholds(result, endpoint)
        
        return result
    
    def _check_thresholds(self, result: Dict[str, Any], endpoint: Dict[str, Any]):
        """检查性能阈值"""
        # 响应时间告警
        if result['response_time_ms'] > self.thresholds['response_time_critical']:
            alert = {
                'level': 'critical',
                'type': 'response_time',
                'endpoint': endpoint['name'],
                'value': result['response_time_ms'],
                'threshold': self.thresholds['response_time_critical'],
                'timestamp': result['timestamp'],
                'message': f"端点 {endpoint['name']} 响应时间严重超时: {result['response_time_ms']:.0f}ms > {self.thresholds['response_time_critical']}ms"
            }
            self.alerts.append(alert)
            self.logger.error(alert['message'])
            
        elif result['response_time_ms'] > self.thresholds['response_time_warning']:
            alert = {
                'level': 'warning',
                'type': 'response_time',
                'endpoint': endpoint['name'],
                'value': result['response_time_ms'],
                'threshold': self.thresholds['response_time_warning'],
                'timestamp': result['timestamp'],
                'message': f"端点 {endpoint['name']} 响应时间警告: {result['response_time_ms']:.0f}ms > {self.thresholds['response_time_warning']}ms"
            }
            self.alerts.append(alert)
            self.logger.warning(alert['message'])
        
        # 错误告警
        if not result['success'] and endpoint['critical']:
            alert = {
                'level': 'critical',
                'type': 'endpoint_error',
                'endpoint': endpoint['name'],
                'error': result['error'],
                'timestamp': result['timestamp'],
                'message': f"关键端点 {endpoint['name']} 错误: {result['error']}"
            }
            self.alerts.append(alert)
            self.logger.error(alert['message'])
    
    def monitor_endpoint(self, endpoint: Dict[str, Any]):
        """监控单个端点"""
        while not self.stop_event.is_set():
            try:
                result = self.check_endpoint(endpoint)
                
                with self.lock:
                    # 记录响应时间
                    self.metrics['response_times'].append({
                        'endpoint': endpoint['name'],
                        'response_time': result['response_time_ms'],
                        'timestamp': result['timestamp']
                    })
                    
                    # 记录状态码
                    status_code = result['status_code']
                    if status_code not in self.metrics['status_codes']:
                        self.metrics['status_codes'][status_code] = 0
                    self.metrics['status_codes'][status_code] += 1
                    
                    # 记录可用性
                    self.metrics['availability'].append({
                        'endpoint': endpoint['name'],
                        'available': result['success'],
                        'timestamp': result['timestamp']
                    })
                
                # 等待指定间隔
                time.sleep(endpoint['interval'])
                
            except Exception as e:
                self.logger.error(f"端点监控错误 {endpoint['name']}: {e}")
                time.sleep(endpoint['interval'])
    
    def monitor_resources(self, interval: int = 30):
        """监控系统资源"""
        while not self.stop_event.is_set():
            try:
                # CPU使用率
                cpu_percent = psutil.cpu_percent(interval=1)
                
                # 内存使用率
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
                
                # 网络IO
                net_io = None
                try:
                    net_io = psutil.net_io_counters()
                except:
                    pass
                
                resource_data = {
                    'timestamp': datetime.now().isoformat(),
                    'cpu_percent': cpu_percent,
                    'memory_percent': memory_percent,
                    'memory_used_gb': memory_used_gb,
                    'disk_usage': disk_usage,
                    'net_io': net_io
                }
                
                with self.lock:
                    self.metrics['resource_usage'].append(resource_data)
                
                # 资源使用率告警
                if cpu_percent > self.thresholds['cpu_critical']:
                    alert = {
                        'level': 'critical',
                        'type': 'cpu_usage',
                        'value': cpu_percent,
                        'threshold': self.thresholds['cpu_critical'],
                        'timestamp': resource_data['timestamp'],
                        'message': f"CPU使用率严重过高: {cpu_percent:.1f}% > {self.thresholds['cpu_critical']}%"
                    }
                    self.alerts.append(alert)
                    self.logger.error(alert['message'])
                elif cpu_percent > self.thresholds['cpu_warning']:
                    alert = {
                        'level': 'warning',
                        'type': 'cpu_usage',
                        'value': cpu_percent,
                        'threshold': self.thresholds['cpu_warning'],
                        'timestamp': resource_data['timestamp'],
                        'message': f"CPU使用率警告: {cpu_percent:.1f}% > {self.thresholds['cpu_warning']}%"
                    }
                    self.alerts.append(alert)
                    self.logger.warning(alert['message'])
                
                if memory_percent > self.thresholds['memory_critical']:
                    alert = {
                        'level': 'critical',
                        'type': 'memory_usage',
                        'value': memory_percent,
                        'threshold': self.thresholds['memory_critical'],
                        'timestamp': resource_data['timestamp'],
                        'message': f"内存使用率严重过高: {memory_percent:.1f}% > {self.thresholds['memory_critical']}%"
                    }
                    self.alerts.append(alert)
                    self.logger.error(alert['message'])
                elif memory_percent > self.thresholds['memory_warning']:
                    alert = {
                        'level': 'warning',
                        'type': 'memory_usage',
                        'value': memory_percent,
                        'threshold': self.thresholds['memory_warning'],
                        'timestamp': resource_data['timestamp'],
                        'message': f"内存使用率警告: {memory_percent:.1f}% > {self.thresholds['memory_warning']}%"
                    }
                    self.alerts.append(alert)
                    self.logger.warning(alert['message'])
                
                time.sleep(interval)
                
            except Exception as e:
                self.logger.error(f"资源监控错误: {e}")
                time.sleep(interval)
    
    def calculate_metrics(self):
        """计算性能指标"""
        with self.lock:
            # 计算错误率
            total_checks = len(self.metrics['availability'])
            successful_checks = sum(1 for a in self.metrics['availability'] if a['available'])
            
            if total_checks > 0:
                error_rate = ((total_checks - successful_checks) / total_checks * 100)
                self.metrics['error_rates'].append({
                    'timestamp': datetime.now().isoformat(),
                    'error_rate': error_rate,
                    'total_checks': total_checks,
                    'failed_checks': total_checks - successful_checks
                })
            
            # 计算吞吐量（每分钟）
            now = datetime.now()
            one_minute_ago = now - timedelta(minutes=1)
            
            recent_checks = [
                a for a in self.metrics['availability']
                if datetime.fromisoformat(a['timestamp'].replace('Z', '+00:00')) >= one_minute_ago
            ]
            
            if recent_checks:
                throughput = len(recent_checks)  # 每分钟检查次数
                self.metrics['throughput'].append({
                    'timestamp': now.isoformat(),
                    'throughput': throughput
                })
    
    def generate_dashboard(self) -> str:
        """生成实时监控仪表板"""
        with self.lock:
            # 计算关键指标
            total_checks = len(self.metrics['availability'])
            successful_checks = sum(1 for a in self.metrics['availability'] if a['available'])
            availability = (successful_checks / total_checks * 100) if total_checks > 0 else 100
            
            # 平均响应时间
            if self.metrics['response_times']:
                avg_response_time = statistics.mean([r['response_time'] for r in self.metrics['response_times']])
                p95_response_time = statistics.quantiles(
                    [r['response_time'] for r in self.metrics['response_times']], 
                    n=20
                )[-1] if len(self.metrics['response_times']) >= 20 else avg_response_time
            else:
                avg_response_time = p95_response_time = 0
            
            # 当前资源使用率
            current_resources = self.metrics['resource_usage'][-1] if self.metrics['resource_usage'] else {}
            cpu_percent = current_resources.get('cpu_percent', 0)
            memory_percent = current_resources.get('memory_percent', 0)
            
            # 告警统计
            critical_alerts = len([a for a in self.alerts if a['level'] == 'critical'])
            warning_alerts = len([a for a in self.alerts if a['level'] == 'warning'])
            
            # 最近告警
            recent_alerts = sorted(self.alerts, key=lambda x: x['timestamp'], reverse=True)[:5]
            
            # 生成仪表板
            dashboard = f"""
实时性能监控仪表板
===================
时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
监控地址: {self.base_url}
运行时间: {((time.time() - self.start_time) / 60):.1f}分钟

关键指标:
  📊 可用性: {availability:.2f}%
  ⏱️  平均响应时间: {avg_response_time:.0f}ms
  📈 P95响应时间: {p95_response_time:.0f}ms
  🔄 总检查次数: {total_checks}
  ✅ 成功检查: {successful_checks}
  ❌ 失败检查: {total_checks - successful_checks}

资源使用:
  💻 CPU使用率: {cpu_percent:.1f}%
  🧠 内存使用率: {memory_percent:.1f}%

告警状态:
  🔴 严重告警: {critical_alerts}
  🟡 警告告警: {warning_alerts}
  🟢 正常

端点状态:
"""
            
            # 各端点状态
            for endpoint in self.monitor_endpoints:
                endpoint_checks = [a for a in self.metrics['availability'] if a['endpoint'] == endpoint['name']]
                if endpoint_checks:
                    endpoint_success = sum(1 for a in endpoint_checks if a['available'])
                    endpoint_availability = (endpoint_success / len(endpoint_checks) * 100) if endpoint_checks else 0
                    
                    endpoint_response_times = [r for r in self.metrics['response_times'] if r['endpoint'] == endpoint['name']]
                    if endpoint_response_times:
                        endpoint_avg_response = statistics.mean([r['response_time'] for r in endpoint_response_times])
                    else:
                        endpoint_avg_response = 0
                    
                    status_icon = "🟢" if endpoint_availability >= 99 else "🟡" if endpoint_availability >= 95 else "🔴"
                    
                    dashboard += f"  {status_icon} {endpoint['name']}: {endpoint_availability:.1f}% | {endpoint_avg_response:.0f}ms\n"
            
            # 最近告警
            if recent_alerts:
                dashboard += "\n最近告警:\n"
                for alert in recent_alerts:
                    icon = "🔴" if alert['level'] == 'critical' else "🟡"
                    time_str = datetime.fromisoformat(alert['timestamp'].replace('Z', '+00:00')).strftime('%H:%M:%S')
                    dashboard += f"  {icon} [{time_str}] {alert['message'][:60]}...\n"
            
            dashboard += f"\n监控日志: metrics_logs/\n"
            
            return dashboard
    
    def run_monitor(self):
        """运行监控"""
        self.logger.info("=" * 60)
        self.logger.info("开始性能指标监控")
        self.logger.info(f"监控地址: {self.base_url}")
        self.logger.info(f"监控时长: {self.monitor_duration}秒")
        self.logger.info("=" * 60)
        
        self.start_time = time.time()
        self.stop_event.clear()
        
        # 启动端点监控线程
        endpoint_threads = []
        for endpoint in self.monitor_endpoints:
            thread = threading.Thread(target=self.monitor_endpoint, args=(endpoint,))
            thread.daemon = True
            thread.start()
            endpoint_threads.append(thread)
        
        # 启动资源监控线程
        resource_thread = threading.Thread(target=self.monitor_resources, args=(30,))
        resource_thread.daemon = True
        resource_thread.start()
        
        self.logger.info(f"已启动 {len(endpoint_threads)} 个端点监控线程")
        self.logger.info("性能监控运行中...")
        
        # 主循环：定期输出仪表板
        last_dashboard_time = time.time()
        last_metrics_time = time.time()
        
        while time.time() - self.start_time < self.monitor_duration and not self.stop_event.is_set():
            current_time = time.time()
            
            # 每分钟计算一次指标
            if current_time - last_metrics_time >= 60:
                self.calculate_metrics()
                last_metrics_time = current_time
            
            # 每30秒输出一次仪表板
            if current_time - last_dashboard_time >= 30:
                dashboard = self.generate_dashboard()
                print("\n" + "="*60)
                print(dashboard)
                print("="*60)
                last_dashboard_time = current_time
            
            time.sleep(1)
        
        # 停止监控
        self.stop_event.set()
        self.logger.info("停止性能监控...")
        
        # 等待线程结束
        for thread in endpoint_threads:
            thread.join(timeout=5)
        
        resource_thread.join(timeout=5)
        
        self.logger.info("性能监控完成")
        
        # 生成最终报告
        report = self.generate_report()
        self.logger.info("\n" + report)
        
        # 保存结果
        self.save_results()
        
        return report
    
    def generate_report(self) -> str:
        """生成监控报告"""
        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        with self.lock:
            # 计算总体指标
            total_checks = len(self.metrics['availability'])
            successful_checks = sum(1 for a in self.metrics['availability'] if a['available'])
            overall_availability = (successful_checks / total_checks * 100) if total_checks > 0 else 0
            
            # 响应时间统计
            if self.metrics['response_times']:
                response_times = [r['response_time'] for r in self.metrics['response_times']]
                avg_response_time = statistics.mean(response_times)
                p95_response_time = statistics.quantiles(response_times, n=20)[-1] if len(response_times) >= 20 else avg_response_time
                max_response_time = max(response_times)
                min_response_time = min(response_times)
            else:
                avg_response_time = p95_response_time = max_response_time = min_response_time = 0
            
            # 资源使用统计
            if self.metrics['resource_usage']:
                cpu_values = [r['cpu_percent'] for r in self.metrics['resource_usage']]
                memory_values = [r['memory_percent'] for r in self.metrics['resource_usage']]
                
                cpu_avg = statistics.mean(cpu_values) if cpu_values else 0
                cpu_max = max(cpu_values) if cpu_values else 0
                memory_avg = statistics.mean(memory_values) if memory_values else 0
                memory_max = max(memory_values) if memory_values else 0
            else:
                cpu_avg = cpu_max = memory_avg = memory_max = 0
            
            # 告警统计
            critical_alerts = len([a for a in self.alerts if a['level'] == 'critical'])
            warning_alerts = len([a for a in self.alerts if a['level'] == 'warning'])
            
            # 状态码分布
            status_codes = self.metrics['status_codes']
        
        report = f"""
性能监控报告
=============
监控时间: {timestamp}
监控地址: {self.base_url}
监控时长: {self.monitor_duration}秒

总体性能指标:
  📊 整体可用性: {overall_availability:.2f}%
  ⏱️  平均响应时间: {avg_response_time:.0f}ms
  📈 P95响应时间: {p95_response_time:.0f}ms
  ⬆️  最大响应时间: {max_response_time:.0f}ms
  ⬇️  最小响应时间: {min_response_time:.0f}ms
  🔄 总检查次数: {total_checks}
  ✅ 成功检查: {successful_checks}
  ❌ 失败检查: {total_checks - successful_checks}

资源使用统计:
  💻 CPU使用率: 平均{cpu_avg:.1f}%, 最大{cpu_max:.1f}%
  🧠 内存使用率: 平均{memory_avg:.1f}%, 最大{memory_max:.1f}%

告警统计:
  🔴 严重告警: {critical_alerts}次
  🟡 警告告警: {warning_alerts}次

状态码分布:
"""
        
        for code, count in sorted(status_codes.items()):
            percentage = (count / total_checks * 100) if total_checks > 0 else 0
            report += f"  HTTP {code}: {count}次 ({percentage:.1f}%)\n"
        
        # 各端点性能
        report += "\n各端点性能:\n"
        
        for endpoint in self.monitor_endpoints:
            with self.lock:
                endpoint_checks = [a for a in self.metrics['availability'] if a['endpoint'] == endpoint['name']]
                endpoint_response_times = [r for r in self.metrics['response_times'] if r['endpoint'] == endpoint['name']]
            
            if endpoint_checks:
                endpoint_success = sum(1 for a in endpoint_checks if a['available'])
                endpoint_availability = (endpoint_success / len(endpoint_checks) * 100)
                
                if endpoint_response_times:
                    endpoint_avg = statistics.mean([r['response_time'] for r in endpoint_response_times])
                    endpoint_p95 = statistics.quantiles(
                        [r['response_time'] for r in endpoint_response_times], 
                        n=20
                    )[-1] if len(endpoint_response_times) >= 20 else endpoint_avg
                else:
                    endpoint_avg = endpoint_p95 = 0
                
                report += f"""
  {endpoint['name']}:
    可用性: {endpoint_availability:.2f}%
    平均响应时间: {endpoint_avg:.0f}ms
    P95响应时间: {endpoint_p95:.0f}ms
    检查次数: {len(endpoint_checks)}
"""
        
        # 性能评估
        report += "\n性能评估:\n"
        report += "-" * 60 + "\n"
        
        if overall_availability >= 99.9:
            report += "✅ 可用性: 优秀 (≥99.9%)\n"
        elif overall_availability >= 99:
            report += "✅ 可用性: 良好 (≥99%)\n"
        elif overall_availability >= 95:
            report += "⚠️  可用性: 一般 (≥95%)\n"
        else:
            report += "❌ 可用性: 差 (<95%)\n"
        
        if avg_response_time <= 500:
            report += "✅ 响应时间: 优秀 (≤500ms)\n"
        elif avg_response_time <= 1000:
            report += "✅ 响应时间: 良好 (≤1s)\n"
        elif avg_response_time <= 2000:
            report += "⚠️  响应时间: 一般 (≤2s)\n"
        else:
            report += "❌ 响应时间: 差 (>2s)\n"
        
        if critical_alerts == 0:
            report += "✅ 系统稳定性: 优秀 (无严重告警)\n"
        elif critical_alerts <= 3:
            report += "✅ 系统稳定性: 良好 (≤3次严重告警)\n"
        elif critical_alerts <= 10:
            report += "⚠️  系统稳定性: 一般 (≤10次严重告警)\n"
        else:
            report += "❌ 系统稳定性: 差 (>10次严重告警)\n"
        
        if cpu_max <= 70:
            report += "✅ CPU使用率: 优秀 (≤70%)\n"
        elif cpu_max <= 80:
            report += "✅ CPU使用率: 良好 (≤80%)\n"
        elif cpu_max <= 90:
            report += "⚠️  CPU使用率: 一般 (≤90%)\n"
        else:
            report += "❌ CPU使用率: 差 (>90%)\n"
        
        # 改进建议
        report += "\n改进建议:\n"
        report += "-" * 60 + "\n"
        
        if overall_availability < 99:
            report += "  1. 提升系统整体可用性\n"
        
        if avg_response_time > 1000:
            report += "  2. 优化响应时间，特别是慢查询\n"
        
        if critical_alerts > 0:
            report += "  3. 解决严重告警问题\n"
        
        if cpu_max > 80:
            report += "  4. 优化CPU使用率\n"
        
        if memory_max > 80:
            report += "  5. 优化内存使用\n"
        
        report += f"\n详细数据已保存到结果文件。\n"
        
        return report
    
    def save_results(self):
        """保存监控结果到文件"""
        import os
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results_dir = "metrics_monitor_results"
        os.makedirs(results_dir, exist_ok=True)
        
        # 保存详细结果
        results_file = os.path.join(results_dir, f"metrics_monitor_results_{timestamp}.json")
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump({
                'config': {
                    'base_url': self.base_url,
                    'monitor_duration': self.monitor_duration,
                    'start_time': self.start_time
                },
                'metrics': self.metrics,
                'alerts': self.alerts,
                'thresholds': self.thresholds
            }, f, ensure_ascii=False, indent=2)
        
        # 保存报告
        report = self.generate_report()
        report_file = os.path.join(results_dir, f"metrics_monitor_report_{timestamp}.txt")
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report)
        
        self.logger.info(f"监控结果已保存到: {results_file}")
        self.logger.info(f"监控报告已保存到: {report_file}")

def main():
    """主函数"""
    import sys
    
    # 默认参数
    base_url = "http://localhost:8080"
    monitor_duration = 3600  # 1小时
    
    # 解析命令行参数
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    if len(sys.argv) > 2:
        try:
            monitor_duration = int(sys.argv[2])
        except ValueError:
            print(f"无效的监控时长: {sys.argv[2]}")
            return
    
    # 运行性能监控
    try:
        print(f"开始性能指标监控: {monitor_duration}秒")
        print(f"监控地址: {base_url}")
        print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 60)
        
        monitor = MetricsMonitor(base_url, monitor_duration)
        report = monitor.run_monitor()
        
        print("\n" + "="*60)
        print("性能监控完成!")
        print("="*60)
        print(report)
        
    except KeyboardInterrupt:
        print("\n性能监控被用户中断")
    except Exception as e:
        print(f"性能监控失败: {e}")
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
        main()  # 仍然运行监控，只是不监控资源