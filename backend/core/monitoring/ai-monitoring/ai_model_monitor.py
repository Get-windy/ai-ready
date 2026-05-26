#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI模型质量监控系统 - 主监控服务
集成性能监控、质量评估、漂移检测和异常检测功能
"""

import time
import json
import logging
import threading
from datetime import datetime, timedelta
from http.server import HTTPServer, BaseHTTPRequestHandler
from prometheus_client import Counter, Histogram, Gauge, CollectorRegistry, generate_latest
import numpy as np
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
import psutil
import os
import sys

# 添加当前目录到Python路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from scripts.model_evaluator import AIEvaluator

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('I:\\AI-Ready\\ai-monitoring\\logs\\ai_monitor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class AIModelMonitor:
    """AI模型监控主类"""
    
    def __init__(self, model_name="default_model"):
        self.model_name = model_name
        self.registry = CollectorRegistry()
        self.evaluator = AIEvaluator(model_name)
        
        # 初始化Prometheus指标
        self._init_metrics()
        
        # 监控数据存储
        self.monitoring_data = {
            'performance': [],
            'quality': [],
            'drift': [],
            'alerts': []
        }
        
        # 监控状态
        self.monitoring_enabled = True
        self.last_evaluation_time = None
        
        logger.info(f"AI模型监控器初始化完成 - 模型: {model_name}")
    
    def _init_metrics(self):
        """初始化Prometheus指标"""
        
        # 性能指标
        self.request_counter = Counter(
            'ai_model_requests_total',
            'AI模型总请求数',
            ['model_name', 'endpoint'],
            registry=self.registry
        )
        
        self.latency_histogram = Histogram(
            'ai_model_latency_seconds',
            'AI模型响应时间（秒）',
            ['model_name'],
            buckets=[0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0],
            registry=self.registry
        )
        
        self.throughput_gauge = Gauge(
            'ai_model_throughput_qps',
            'AI模型吞吐量（QPS）',
            ['model_name'],
            registry=self.registry
        )
        
        # 质量指标
        self.accuracy_gauge = Gauge(
            'ai_model_accuracy',
            'AI模型准确率',
            ['model_name'],
            registry=self.registry
        )
        
        self.precision_gauge = Gauge(
            'ai_model_precision',
            'AI模型精确率',
            ['model_name'],
            registry=self.registry
        )
        
        self.recall_gauge = Gauge(
            'ai_model_recall',
            'AI模型召回率',
            ['model_name'],
            registry=self.registry
        )
        
        self.f1_gauge = Gauge(
            'ai_model_f1_score',
            'AI模型F1分数',
            ['model_name'],
            registry=self.registry
        )
        
        # 资源指标
        self.cpu_usage_gauge = Gauge(
            'ai_model_cpu_usage_percent',
            'AI模型CPU使用率（百分比）',
            ['model_name'],
            registry=self.registry
        )
        
        self.memory_usage_gauge = Gauge(
            'ai_model_memory_usage_mb',
            'AI模型内存使用量（MB）',
            ['model_name'],
            registry=self.registry
        )
        
        # 漂移检测指标
        self.data_drift_gauge = Gauge(
            'ai_model_data_drift_score',
            'AI模型数据漂移分数',
            ['model_name'],
            registry=self.registry
        )
        
        self.concept_drift_gauge = Gauge(
            'ai_model_concept_drift_score',
            'AI模型概念漂移分数',
            ['model_name'],
            registry=self.registry
        )
        
        # 异常检测指标
        self.anomaly_score_gauge = Gauge(
            'ai_model_anomaly_score',
            'AI模型异常分数',
            ['model_name'],
            registry=self.registry
        )
        
        logger.info("Prometheus指标初始化完成")
    
    def record_request(self, endpoint="/predict", latency_seconds=0.1):
        """记录模型请求"""
        self.request_counter.labels(model_name=self.model_name, endpoint=endpoint).inc()
        self.latency_histogram.labels(model_name=self.model_name).observe(latency_seconds)
        
        # 更新吞吐量
        current_time = time.time()
        if hasattr(self, 'last_request_time'):
            time_diff = current_time - self.last_request_time
            if time_diff > 0:
                qps = 1 / time_diff
                self.throughput_gauge.labels(model_name=self.model_name).set(qps)
        self.last_request_time = current_time
        
        # 记录性能数据
        perf_data = {
            'timestamp': datetime.now().isoformat(),
            'endpoint': endpoint,
            'latency_seconds': latency_seconds,
            'cpu_percent': psutil.cpu_percent(),
            'memory_mb': psutil.Process().memory_info().rss / 1024 / 1024
        }
        self.monitoring_data['performance'].append(perf_data)
        
        # 保持数据量可控
        if len(self.monitoring_data['performance']) > 1000:
            self.monitoring_data['performance'] = self.monitoring_data['performance'][-1000:]
        
        logger.debug(f"记录请求: {endpoint}, 延迟: {latency_seconds:.3f}s")
    
    def update_quality_metrics(self, y_true=None, y_pred=None, y_prob=None):
        """更新质量指标"""
        if y_true is not None and y_pred is not None:
            try:
                # 计算质量指标
                accuracy = accuracy_score(y_true, y_pred)
                precision = precision_score(y_true, y_pred, average='weighted', zero_division=0)
                recall = recall_score(y_true, y_pred, average='weighted', zero_division=0)
                f1 = f1_score(y_true, y_pred, average='weighted', zero_division=0)
                
                # 更新Prometheus指标
                self.accuracy_gauge.labels(model_name=self.model_name).set(accuracy)
                self.precision_gauge.labels(model_name=self.model_name).set(precision)
                self.recall_gauge.labels(model_name=self.model_name).set(recall)
                self.f1_gauge.labels(model_name=self.model_name).set(f1)
                
                # 记录质量数据
                quality_data = {
                    'timestamp': datetime.now().isoformat(),
                    'accuracy': float(accuracy),
                    'precision': float(precision),
                    'recall': float(recall),
                    'f1_score': float(f1),
                    'sample_count': len(y_true)
                }
                self.monitoring_data['quality'].append(quality_data)
                
                # 保持数据量可控
                if len(self.monitoring_data['quality']) > 100:
                    self.monitoring_data['quality'] = self.monitoring_data['quality'][-100:]
                
                self.last_evaluation_time = datetime.now()
                logger.info(f"质量指标更新: 准确率={accuracy:.4f}, F1={f1:.4f}")
                
                return quality_data
            except Exception as e:
                logger.error(f"更新质量指标失败: {e}")
        
        return None
    
    def detect_drift(self, current_data, reference_data=None):
        """检测数据漂移和概念漂移"""
        try:
            # 这里实现漂移检测算法
            # 简化版本：使用KL散度或PSI作为漂移指标
            
            if reference_data is None:
                # 如果没有参考数据，使用历史平均值
                if len(self.monitoring_data['quality']) > 10:
                    recent_accuracy = [q['accuracy'] for q in self.monitoring_data['quality'][-10:]]
                    ref_accuracy = np.mean(recent_accuracy)
                else:
                    ref_accuracy = 0.9  # 默认参考值
            else:
                ref_accuracy = reference_data
            
            # 计算当前准确率
            current_accuracy = self.accuracy_gauge.labels(model_name=self.model_name)._value.get()
            if current_accuracy is None:
                current_accuracy = 0.9
            
            # 计算漂移分数（简化版）
            accuracy_diff = abs(current_accuracy - ref_accuracy)
            data_drift_score = min(accuracy_diff / 0.1, 1.0)  # 归一化到0-1
            concept_drift_score = min(accuracy_diff / 0.2, 1.0)  # 概念漂移阈值更高
            
            # 更新Prometheus指标
            self.data_drift_gauge.labels(model_name=self.model_name).set(data_drift_score)
            self.concept_drift_gauge.labels(model_name=self.model_name).set(concept_drift_score)
            
            # 记录漂移数据
            drift_data = {
                'timestamp': datetime.now().isoformat(),
                'data_drift_score': float(data_drift_score),
                'concept_drift_score': float(concept_drift_score),
                'current_accuracy': float(current_accuracy),
                'reference_accuracy': float(ref_accuracy)
            }
            self.monitoring_data['drift'].append(drift_data)
            
            # 检查是否需要告警
            if data_drift_score > 0.3:
                self._generate_alert('data_drift', f"数据漂移检测: 分数={data_drift_score:.3f}")
            
            if concept_drift_score > 0.5:
                self._generate_alert('concept_drift', f"概念漂移检测: 分数={concept_drift_score:.3f}")
            
            logger.info(f"漂移检测: 数据漂移={data_drift_score:.3f}, 概念漂移={concept_drift_score:.3f}")
            
            return drift_data
        except Exception as e:
            logger.error(f"漂移检测失败: {e}")
            return None
    
    def detect_anomalies(self):
        """检测模型异常"""
        try:
            # 这里实现异常检测算法
            # 简化版本：检查性能指标是否异常
            
            if len(self.monitoring_data['performance']) < 5:
                return None
            
            # 获取最近的性能数据
            recent_perf = self.monitoring_data['performance'][-5:]
            latencies = [p['latency_seconds'] for p in recent_perf]
            
            # 计算异常分数（基于延迟的Z-score）
            mean_latency = np.mean(latencies)
            std_latency = np.std(latencies) if len(latencies) > 1 else 0.1
            
            if std_latency > 0:
                current_latency = latencies[-1]
                z_score = abs((current_latency - mean_latency) / std_latency)
                anomaly_score = min(z_score / 3.0, 1.0)  # Z=3对应分数1.0
            else:
                anomaly_score = 0.0
            
            # 更新Prometheus指标
            self.anomaly_score_gauge.labels(model_name=self.model_name).set(anomaly_score)
            
            # 检查是否需要告警
            if anomaly_score > 0.8:
                self._generate_alert('anomaly', f"异常检测: 分数={anomaly_score:.3f}, 延迟异常")
            
            logger.debug(f"异常检测: 分数={anomaly_score:.3f}")
            
            return {'anomaly_score': float(anomaly_score), 'timestamp': datetime.now().isoformat()}
        except Exception as e:
            logger.error(f"异常检测失败: {e}")
            return None
    
    def _generate_alert(self, alert_type, message):
        """生成告警"""
        alert_data = {
            'timestamp': datetime.now().isoformat(),
            'type': alert_type,
            'severity': 'warning' if alert_type in ['data_drift', 'anomaly'] else 'critical',
            'message': message,
            'model': self.model_name,
            'acknowledged': False,
            'resolved': False
        }
        
        self.monitoring_data['alerts'].append(alert_data)
        
        # 保持告警数量可控
        if len(self.monitoring_data['alerts']) > 100:
            self.monitoring_data['alerts'] = self.monitoring_data['alerts'][-100:]
        
        logger.warning(f"生成告警: {alert_type} - {message}")
        
        # 这里可以添加告警通知逻辑（邮件、Slack等）
        self._send_alert_notification(alert_data)
    
    def _send_alert_notification(self, alert_data):
        """发送告警通知（示例实现）"""
        # 这里可以实现发送邮件、Slack消息等
        # 当前只是记录到日志
        logger.info(f"告警通知: {json.dumps(alert_data, ensure_ascii=False)}")
    
    def update_resource_metrics(self):
        """更新资源使用指标"""
        try:
            # 获取CPU使用率
            cpu_percent = psutil.cpu_percent()
            self.cpu_usage_gauge.labels(model_name=self.model_name).set(cpu_percent)
            
            # 获取内存使用量
            process = psutil.Process()
            memory_mb = process.memory_info().rss / 1024 / 1024
            self.memory_usage_gauge.labels(model_name=self.model_name).set(memory_mb)
            
            logger.debug(f"资源指标更新: CPU={cpu_percent}%, 内存={memory_mb:.2f}MB")
        except Exception as e:
            logger.error(f"更新资源指标失败: {e}")
    
    def get_metrics(self):
        """获取所有监控指标"""
        return {
            'model': self.model_name,
            'timestamp': datetime.now().isoformat(),
            'performance': self.monitoring_data['performance'][-10:] if self.monitoring_data['performance'] else [],
            'quality': self.monitoring_data['quality'][-5:] if self.monitoring_data['quality'] else [],
            'drift': self.monitoring_data['drift'][-5:] if self.monitoring_data['drift'] else [],
            'alerts': [a for a in self.monitoring_data['alerts'] if not a['resolved']][-10:],
            'status': 'active' if self.monitoring_enabled else 'inactive'
        }
    
    def generate_report(self, time_range_hours=24):
        """生成监控报告"""
        try:
            cutoff_time = datetime.now() - timedelta(hours=time_range_hours)
            
            # 过滤指定时间范围内的数据
            perf_data = [p for p in self.monitoring_data['performance'] 
                        if datetime.fromisoformat(p['timestamp'].replace('Z', '')) > cutoff_time]
            quality_data = [q for q in self.monitoring_data['quality']
                           if datetime.fromisoformat(q['timestamp'].replace('Z', '')) > cutoff_time]
            alert_data = [a for a in self.monitoring_data['alerts']
                         if datetime.fromisoformat(a['timestamp'].replace('Z', '')) > cutoff_time]
            
            # 计算统计数据
            report = {
                'model': self.model_name,
                'report_time': datetime.now().isoformat(),
                'time_range_hours': time_range_hours,
                'summary': {
                    'total_requests': len(perf_data),
                    'avg_latency_seconds': np.mean([p['latency_seconds'] for p in perf_data]) if perf_data else 0,
                    'avg_accuracy': np.mean([q['accuracy'] for q in quality_data]) if quality_data else 0,
                    'active_alerts': len([a for a in alert_data if not a['resolved']]),
                    'data_drift_detected': any(d['data_drift_score'] > 0.3 for d in self.monitoring_data['drift'][-10:]) if self.monitoring_data['drift'] else False
                },
                'recent_alerts': alert_data[-10:],
                'recommendations': self._generate_recommendations()
            }
            
            return report
        except Exception as e:
            logger.error(f"生成报告失败: {e}")
            return None
    
    def _generate_recommendations(self):
        """根据监控数据生成建议"""
        recommendations = []
        
        # 检查准确率
        if self.monitoring_data['quality']:
            recent_accuracy = [q['accuracy'] for q in self.monitoring_data['quality'][-5:]]
            avg_accuracy = np.mean(recent_accuracy)
            if avg_accuracy < 0.8:
                recommendations.append({
                    'type': 'quality',
                    'priority': 'high',
                    'message': f'模型准确率较低 ({avg_accuracy:.2%})，建议重新训练或调整参数'
                })
        
        # 检查响应时间
        if self.monitoring_data['performance']:
            recent_latency = [p['latency_seconds'] for p in self.monitoring_data['performance'][-10:]]
            avg_latency = np.mean(recent_latency)
            if avg_latency > 1.0:
                recommendations.append({
                    'type': 'performance',
                    'priority': 'medium',
                    'message': f'平均响应时间较高 ({avg_latency:.2f}s)，建议优化模型或增加资源'
                })
        
        # 检查告警
        active_alerts = [a for a in self.monitoring_data['alerts'] if not a['resolved']]
        if len(active_alerts) > 5:
            recommendations.append({
                'type': 'reliability',
                'priority': 'high',
                'message': f'有 {len(active_alerts)} 个未处理告警，建议立即处理'
            })
        
        return recommendations
    
    def stop_monitoring(self):
        """停止监控"""
        self.monitoring_enabled = False
        logger.info(f"停止AI模型监控 - 模型: {self.model_name}")


class MonitoringHTTPServer(BaseHTTPRequestHandler):
    """HTTP服务器，提供监控接口和Prometheus指标"""
    
    monitor = None  # 类变量，共享监控器实例
    
    def do_GET(self):
        """处理GET请求"""
        if self.path == '/metrics':
            # 提供Prometheus指标
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; version=0.0.4')
            self.end_headers()
            
            if self.monitor:
                # 更新资源指标
                self.monitor.update_resource_metrics()
                # 检测异常
                self.monitor.detect_anomalies()
                # 生成指标数据
                metrics_data = generate_latest(self.monitor.registry)
                self.wfile.write(metrics_data)
            else:
                self.wfile.write(b'# No monitor initialized\n')
        
        elif self.path == '/health':
            # 健康检查
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            response = {
                'status': 'healthy',
                'timestamp': datetime.now().isoformat(),
                'service': 'ai-model-monitor'
            }
            self.wfile.write(json.dumps(response).encode())
        
        elif self.path == '/metrics/json':
            # 获取JSON格式的监控数据
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            if self.monitor:
                metrics = self.monitor.get_metrics()
                self.wfile.write(json.dumps(metrics, indent=2).encode())
            else:
                self.wfile.write(json.dumps({'error': 'Monitor not initialized'}).encode())
        
        elif self.path == '/report':
            # 生成监控报告
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            if self.monitor:
                report = self.monitor.generate_report()
                self.wfile.write(json.dumps(report, indent=2).encode())
            else:
                self.wfile.write(json.dumps({'error': 'Monitor not initialized'}).encode())
        
        else:
            self.send_response(404)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({'error': 'Not found'}).encode())
    
    def do_POST(self):
        """处理POST请求"""
        if self.path == '/record-request':
            # 记录模型请求
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode())
            
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            if self.monitor:
                endpoint = data.get('endpoint', '/predict')
                latency = data.get('latency_seconds', 0.1)
                self.monitor.record_request(endpoint, latency)
                response = {'status': 'recorded', 'endpoint': endpoint}
            else:
                response = {'error': 'Monitor not initialized'}
            
            self.wfile.write(json.dumps(response).encode())
        
        elif self.path == '/update-quality':
            # 更新质量指标
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode())
            
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            if self.monitor:
                y_true = data.get('y_true')
                y_pred = data.get('y_pred')
                y_prob = data.get('y_prob')
                
                quality_data = self.monitor.update_quality_metrics(y_true, y_pred, y_prob)
                
                if quality_data:
                    response = {'status': 'updated', 'quality': quality_data}
                else:
                    response = {'error': 'Invalid data for quality update'}
            else:
                response = {'error': 'Monitor not initialized'}
            
            self.wfile.write(json.dumps(response).encode())
        
        elif self.path == '/check-drift':
            # 检查漂移
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            if self.monitor:
                drift_data = self.monitor.detect_drift(None)
                if drift_data:
                    response = {'status': 'checked', 'drift': drift_data}
                else:
                    response = {'error': 'Drift detection failed'}
            else:
                response = {'error': 'Monitor not initialized'}
            
            self.wfile.write(json.dumps(response).encode())
        
        else:
            self.send_response(404)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({'error': 'Not found'}).encode())
    
    def log_message(self, format, *args):
        """自定义日志格式"""
        logger.info(f"HTTP {self.address_string()} - {format % args}")


def run_monitor_server(port=8000, model_name="test_model"):
    """运行监控服务器"""
    # 创建监控器
    monitor = AIModelMonitor(model_name)
    MonitoringHTTPServer.monitor = monitor
    
    # 启动HTTP服务器
    server_address = ('', port)
    httpd = HTTPServer(server_address, MonitoringHTTPServer)
    
    logger.info(f"AI模型监控服务器启动，监听端口 {port}")
    logger.info(f"监控模型: {model_name}")
    logger.info("可用端点:")
    logger.info("  GET /metrics      - Prometheus指标")
    logger.info("  GET /health       - 健康检查")
    logger.info("  GET /metrics/json - JSON格式监控数据")
    logger.info("  GET /report       - 监控报告")
    logger.info("  POST /record-request - 记录模型请求")
    logger.info("  POST /update-quality - 更新质量指标")
    logger.info("  POST /check-drift - 检查漂移")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("收到停止信号，关闭服务器...")
        monitor.stop_monitoring()
        httpd.server_close()
        logger.info("服务器已关闭")


if __name__ == "__main__":
    # 运行监控服务器
    run_monitor_server(port=8000, model_name="ai_ready_model")