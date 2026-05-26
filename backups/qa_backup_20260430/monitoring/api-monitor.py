#!/usr/bin/env python3
"""
API接口监控脚本
用途: 监控测试环境API服务的可用性、性能和错误率
版本: 1.0
集成: Prometheus + Grafana
"""

import os
import sys
import json
import time
import argparse
import logging
import requests
import threading
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional, Callable
from dataclasses import dataclass, asdict
from collections import deque
import statistics

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('api-monitor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


@dataclass
class APIMetric:
    """API指标数据类"""
    endpoint: str
    method: str
    status_code: int
    response_time: float  # milliseconds
    timestamp: datetime
    error: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'endpoint': self.endpoint,
            'method': self.method,
            'status_code': self.status_code,
            'response_time': self.response_time,
            'timestamp': self.timestamp.isoformat(),
            'error': self.error
        }


@dataclass
class AlertRule:
    """告警规则"""
    name: str
    condition: str  # 'response_time > 1000' or 'error_rate > 0.05'
    threshold: float
    duration: int  # seconds
    severity: str  # 'warning' or 'critical'
    message: str


class PrometheusExporter:
    """Prometheus指标导出器"""
    
    def __init__(self, port: int = 9091):
        self.port = port
        self.metrics = {}
        self.lock = threading.Lock()
    
    def update_gauge(self, name: str, value: float, labels: Dict[str, str] = None):
        """更新Gauge指标"""
        with self.lock:
            if name not in self.metrics:
                self.metrics[name] = {'type': 'gauge', 'values': []}
            
            self.metrics[name]['values'].append({
                'value': value,
                'labels': labels or {},
                'timestamp': datetime.now().isoformat()
            })
    
    def update_counter(self, name: str, value: float = 1, labels: Dict[str, str] = None):
        """更新Counter指标"""
        with self.lock:
            if name not in self.metrics:
                self.metrics[name] = {'type': 'counter', 'value': 0}
            
            self.metrics[name]['value'] += value
    
    def get_metrics_text(self) -> str:
        """获取Prometheus格式的指标文本"""
        lines = []
        
        with self.lock:
            for name, metric in self.metrics.items():
                if metric['type'] == 'gauge':
                    lines.append(f"# HELP {name} API metric")
                    lines.append(f"# TYPE {name} gauge")
                    for value in metric['values'][-10:]:  # 最近10个值
                        labels_str = ','.join([f'{k}="{v}"' for k, v in value['labels'].items()])
                        if labels_str:
                            lines.append(f'{name}{{{labels_str}}} {value["value"]}')
                        else:
                            lines.append(f'{name} {value["value"]}')
                
                elif metric['type'] == 'counter':
                    lines.append(f"# HELP {name} API counter")
                    lines.append(f"# TYPE {name} counter")
                    lines.append(f"{name} {metric['value']}")
        
        return '\n'.join(lines)
    
    def save_to_file(self, filepath: str = 'api-metrics.prom'):
        """保存指标到文件"""
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(self.get_metrics_text())


class APIMonitor:
    """API监控器"""
    
    def __init__(self, base_url: str = 'http://localhost:8080', 
                 prometheus_port: int = 9091,
                 check_interval: int = 30):
        self.base_url = base_url.rstrip('/')
        self.check_interval = check_interval
        self.metrics_history = deque(maxlen=1000)
        self.alert_rules = []
        self.active_alerts = []
        self.prometheus = PrometheusExporter(port=prometheus_port)
        self.running = False
        self.monitor_thread = None
        
        # 默认API端点配置
        self.endpoints = [
            {'path': '/api/health', 'method': 'GET', 'name': 'health_check'},
            {'path': '/api/user/login', 'method': 'POST', 'name': 'user_login'},
            {'path': '/api/user/info', 'method': 'GET', 'name': 'user_info'},
            {'path': '/api/order/list', 'method': 'GET', 'name': 'order_list'},
            {'path': '/api/order/detail', 'method': 'GET', 'name': 'order_detail'},
            {'path': '/api/product/list', 'method': 'GET', 'name': 'product_list'},
            {'path': '/api/inventory/status', 'method': 'GET', 'name': 'inventory_status'},
            {'path': '/api/partner/list', 'method': 'GET', 'name': 'partner_list'},
        ]
        
        # 默认告警规则
        self.setup_default_alerts()
    
    def setup_default_alerts(self):
        """设置默认告警规则"""
        self.alert_rules = [
            AlertRule(
                name='high_response_time',
                condition='response_time > 1000',
                threshold=1000,
                duration=60,
                severity='warning',
                message='API响应时间超过1秒'
            ),
            AlertRule(
                name='critical_response_time',
                condition='response_time > 3000',
                threshold=3000,
                duration=30,
                severity='critical',
                message='API响应时间超过3秒'
            ),
            AlertRule(
                name='high_error_rate',
                condition='error_rate > 0.05',
                threshold=0.05,
                duration=120,
                severity='warning',
                message='API错误率超过5%'
            ),
            AlertRule(
                name='critical_error_rate',
                condition='error_rate > 0.20',
                threshold=0.20,
                duration=60,
                severity='critical',
                message='API错误率超过20%'
            ),
            AlertRule(
                name='service_unavailable',
                condition='availability < 0.95',
                threshold=0.95,
                duration=60,
                severity='critical',
                message='API可用性低于95%'
            )
        ]
    
    def add_endpoint(self, path: str, method: str = 'GET', name: str = None, 
                     headers: Dict = None, body: Dict = None):
        """添加监控端点"""
        endpoint = {
            'path': path,
            'method': method,
            'name': name or path.replace('/', '_'),
            'headers': headers or {},
            'body': body or {}
        }
        self.endpoints.append(endpoint)
    
    def check_endpoint(self, endpoint: Dict) -> APIMetric:
        """检查单个API端点"""
        url = f"{self.base_url}{endpoint['path']}"
        method = endpoint['method']
        headers = endpoint.get('headers', {})
        body = endpoint.get('body', {})
        
        start_time = time.time()
        
        try:
            if method == 'GET':
                response = requests.get(url, headers=headers, timeout=10)
            elif method == 'POST':
                response = requests.post(url, json=body, headers=headers, timeout=10)
            elif method == 'PUT':
                response = requests.put(url, json=body, headers=headers, timeout=10)
            elif method == 'DELETE':
                response = requests.delete(url, headers=headers, timeout=10)
            else:
                response = requests.request(method, url, headers=headers, timeout=10)
            
            response_time = (time.time() - start_time) * 1000
            
            metric = APIMetric(
                endpoint=endpoint['path'],
                method=method,
                status_code=response.status_code,
                response_time=response_time,
                timestamp=datetime.now()
            )
            
            # 更新Prometheus指标
            self.prometheus.update_gauge(
                'api_response_time_ms',
                response_time,
                {'endpoint': endpoint['name'], 'method': method}
            )
            
            if response.status_code >= 400:
                self.prometheus.update_counter(
                    'api_errors_total',
                    1,
                    {'endpoint': endpoint['name'], 'status_code': str(response.status_code)}
                )
            else:
                self.prometheus.update_counter(
                    'api_requests_total',
                    1,
                    {'endpoint': endpoint['name'], 'method': method}
                )
            
            return metric
            
        except requests.exceptions.Timeout:
            response_time = (time.time() - start_time) * 1000
            return APIMetric(
                endpoint=endpoint['path'],
                method=method,
                status_code=0,
                response_time=response_time,
                timestamp=datetime.now(),
                error='Timeout'
            )
        
        except requests.exceptions.ConnectionError:
            response_time = (time.time() - start_time) * 1000
            return APIMetric(
                endpoint=endpoint['path'],
                method=method,
                status_code=0,
                response_time=response_time,
                timestamp=datetime.now(),
                error='Connection Error'
            )
        
        except Exception as e:
            response_time = (time.time() - start_time) * 1000
            return APIMetric(
                endpoint=endpoint['path'],
                method=method,
                status_code=0,
                response_time=response_time,
                timestamp=datetime.now(),
                error=str(e)
            )
    
    def check_all_endpoints(self) -> List[APIMetric]:
        """检查所有API端点"""
        metrics = []
        
        for endpoint in self.endpoints:
            metric = self.check_endpoint(endpoint)
            metrics.append(metric)
            self.metrics_history.append(metric)
            
            status = '✅' if metric.status_code < 400 and metric.error is None else '❌'
            logger.info(f"{status} {metric.method} {metric.endpoint} - "
                       f"{metric.status_code} ({metric.response_time:.2f}ms)")
        
        return metrics
    
    def calculate_statistics(self, window_seconds: int = 300) -> Dict[str, Any]:
        """计算统计信息"""
        cutoff_time = datetime.now() - timedelta(seconds=window_seconds)
        recent_metrics = [m for m in self.metrics_history if m.timestamp > cutoff_time]
        
        if not recent_metrics:
            return {}
        
        # 按端点分组统计
        endpoint_stats = {}
        for metric in recent_metrics:
            key = f"{metric.method} {metric.endpoint}"
            if key not in endpoint_stats:
                endpoint_stats[key] = []
            endpoint_stats[key].append(metric)
        
        stats = {}
        for endpoint, metrics in endpoint_stats.items():
            response_times = [m.response_time for m in metrics]
            error_count = sum(1 for m in metrics if m.status_code >= 400 or m.error)
            total_count = len(metrics)
            
            stats[endpoint] = {
                'total_requests': total_count,
                'error_count': error_count,
                'error_rate': error_count / total_count if total_count > 0 else 0,
                'availability': (total_count - error_count) / total_count if total_count > 0 else 0,
                'avg_response_time': statistics.mean(response_times) if response_times else 0,
                'min_response_time': min(response_times) if response_times else 0,
                'max_response_time': max(response_times) if response_times else 0,
                'p50_response_time': statistics.median(response_times) if response_times else 0,
                'p95_response_time': sorted(response_times)[int(len(response_times) * 0.95)] if response_times else 0,
                'p99_response_time': sorted(response_times)[int(len(response_times) * 0.99)] if response_times else 0,
            }
        
        return stats
    
    def check_alerts(self) -> List[Dict[str, Any]]:
        """检查告警规则"""
        alerts = []
        stats = self.calculate_statistics(window_seconds=300)
        
        for rule in self.alert_rules:
            # 解析条件
            if 'response_time' in rule.condition:
                for endpoint, endpoint_stats in stats.items():
                    if endpoint_stats['avg_response_time'] > rule.threshold:
                        alert = {
                            'rule': rule.name,
                            'severity': rule.severity,
                            'message': f"{rule.message} - {endpoint}: "
                                      f"{endpoint_stats['avg_response_time']:.2f}ms",
                            'timestamp': datetime.now().isoformat(),
                            'value': endpoint_stats['avg_response_time']
                        }
                        alerts.append(alert)
            
            elif 'error_rate' in rule.condition:
                for endpoint, endpoint_stats in stats.items():
                    if endpoint_stats['error_rate'] > rule.threshold:
                        alert = {
                            'rule': rule.name,
                            'severity': rule.severity,
                            'message': f"{rule.message} - {endpoint}: "
                                      f"{endpoint_stats['error_rate']*100:.2f}%",
                            'timestamp': datetime.now().isoformat(),
                            'value': endpoint_stats['error_rate']
                        }
                        alerts.append(alert)
            
            elif 'availability' in rule.condition:
                for endpoint, endpoint_stats in stats.items():
                    if endpoint_stats['availability'] < rule.threshold:
                        alert = {
                            'rule': rule.name,
                            'severity': rule.severity,
                            'message': f"{rule.message} - {endpoint}: "
                                      f"{endpoint_stats['availability']*100:.2f}%",
                            'timestamp': datetime.now().isoformat(),
                            'value': endpoint_stats['availability']
                        }
                        alerts.append(alert)
        
        self.active_alerts = alerts
        return alerts
    
    def start_monitoring(self):
        """开始监控"""
        self.running = True
        logger.info(f"🚀 API监控已启动 - 检查间隔: {self.check_interval}秒")
        
        def monitor_loop():
            while self.running:
                try:
                    # 检查所有端点
                    metrics = self.check_all_endpoints()
                    
                    # 检查告警
                    alerts = self.check_alerts()
                    if alerts:
                        for alert in alerts:
                            if alert['severity'] == 'critical':
                                logger.error(f"🚨 CRITICAL: {alert['message']}")
                            else:
                                logger.warning(f"⚠️ WARNING: {alert['message']}")
                    
                    # 保存Prometheus指标
                    self.prometheus.save_to_file()
                    
                    # 等待下一次检查
                    time.sleep(self.check_interval)
                    
                except Exception as e:
                    logger.error(f"监控循环异常: {e}")
                    time.sleep(self.check_interval)
        
        self.monitor_thread = threading.Thread(target=monitor_loop, daemon=True)
        self.monitor_thread.start()
    
    def stop_monitoring(self):
        """停止监控"""
        self.running = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5)
        logger.info("🛑 API监控已停止")
    
    def generate_report(self, output_file: str = 'api-monitor-report.json') -> Dict[str, Any]:
        """生成监控报告"""
        stats = self.calculate_statistics()
        alerts = self.check_alerts()
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'base_url': self.base_url,
            'summary': {
                'total_endpoints': len(self.endpoints),
                'total_checks': len(self.metrics_history),
                'active_alerts': len(alerts)
            },
            'statistics': stats,
            'alerts': alerts,
            'endpoints': [
                {
                    'path': ep['path'],
                    'method': ep['method'],
                    'name': ep['name']
                }
                for ep in self.endpoints
            ]
        }
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        logger.info(f"📊 监控报告已生成: {output_file}")
        return report


def parse_args():
    """解析命令行参数"""
    parser = argparse.ArgumentParser(description='API接口监控工具')
    parser.add_argument('--base-url', default='http://localhost:8080',
                       help='API基础URL')
    parser.add_argument('--interval', type=int, default=30,
                       help='检查间隔(秒)')
    parser.add_argument('--prometheus-port', type=int, default=9091,
                       help='Prometheus指标端口')
    parser.add_argument('--config', default='api-monitor-config.json',
                       help='配置文件路径')
    parser.add_argument('--report', default='api-monitor-report.json',
                       help='报告输出文件')
    parser.add_argument('--daemon', action='store_true',
                       help='后台运行模式')
    parser.add_argument('--once', action='store_true',
                       help='只执行一次检查')
    parser.add_argument('--verbose', action='store_true',
                       help='显示详细日志')
    return parser.parse_args()


def main():
    """主函数"""
    args = parse_args()
    
    # 设置详细日志
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # 创建监控器
    monitor = APIMonitor(
        base_url=args.base_url,
        prometheus_port=args.prometheus_port,
        check_interval=args.interval
    )
    
    # 加载配置文件
    if os.path.exists(args.config):
        with open(args.config, 'r', encoding='utf-8') as f:
            config = json.load(f)
            
            # 加载自定义端点
            for endpoint in config.get('endpoints', []):
                monitor.add_endpoint(
                    path=endpoint['path'],
                    method=endpoint.get('method', 'GET'),
                    name=endpoint.get('name'),
                    headers=endpoint.get('headers', {}),
                    body=endpoint.get('body', {})
                )
            
            logger.info(f"📋 已加载 {len(config.get('endpoints', []))} 个端点配置")
    
    if args.once:
        # 单次检查模式
        logger.info("🔍 执行单次API检查...")
        metrics = monitor.check_all_endpoints()
        alerts = monitor.check_alerts()
        report = monitor.generate_report(args.report)
        
        # 输出结果
        print("\n" + "=" * 80)
        print("API监控检查结果:")
        print("=" * 80)
        
        for endpoint, stats in report['statistics'].items():
            status = "✅" if stats['error_rate'] == 0 else "❌"
            print(f"{status} {endpoint}")
            print(f"   请求数: {stats['total_requests']}, "
                  f"错误率: {stats['error_rate']*100:.2f}%, "
                  f"平均响应: {stats['avg_response_time']:.2f}ms")
        
        if alerts:
            print("\n⚠️ 活动告警:")
            for alert in alerts:
                print(f"   [{alert['severity'].upper()}] {alert['message']}")
        
        print(f"\n📊 报告已保存: {args.report}")
        
    elif args.daemon:
        # 后台运行模式
        logger.info("🚀 启动API监控守护进程...")
        monitor.start_monitoring()
        
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            logger.info("收到停止信号...")
        finally:
            monitor.stop_monitoring()
    
    else:
        # 默认模式: 执行一次并生成报告
        logger.info("🔍 执行API监控检查...")
        metrics = monitor.check_all_endpoints()
        report = monitor.generate_report(args.report)
        
        print("\n" + "=" * 80)
        print("API监控完成!")
        print("=" * 80)
        print(f"总端点数: {report['summary']['total_endpoints']}")
        print(f"总检查数: {report['summary']['total_checks']}")
        print(f"活动告警: {report['summary']['active_alerts']}")
        print(f"报告文件: {args.report}")


if __name__ == '__main__':
    main()
