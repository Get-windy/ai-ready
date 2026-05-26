#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI模型指标收集服务
提供HTTP接口供Prometheus采集指标
"""

import time
import random
import json
import psutil
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime
from prometheus_client import Counter, Histogram, Gauge, CollectorRegistry, generate_latest
from threading import Thread
import numpy as np

class AIMetricsCollector:
    def __init__(self):
        self.registry = CollectorRegistry()
        
        # 创建Prometheus指标
        self.request_counter = Counter(
            'ai_model_requests',
            'AI模型请求总数',
            ['model_name', 'status'],
            registry=self.registry
        )
        
        self.error_counter = Counter(
            'ai_model_errors',
            'AI模型错误数',
            ['model_name', 'error_type'],
            registry=self.registry
        )
        
        self.prediction_latency = Histogram(
            'ai_model_prediction_latency_ms',
            'AI模型预测延迟（毫秒）',
            ['model_name'],
            buckets=[10, 50, 100, 200, 500, 1000, 2000],
            registry=self.registry
        )
        
        self.accuracy_gauge = Gauge(
            'ai_model_accuracy',
            'AI模型准确率',
            ['model_name'],
            registry=self.registry
        )
        
        self.p95_latency_gauge = Gauge(
            'ai_model_p95_latency',
            'AI模型P95延迟',
            ['model_name'],
            registry=self.registry
        )
        
        self.throughput_gauge = Gauge(
            'ai_model_throughput',
            'AI模型吞吐量（QPS）',
            ['model_name'],
            registry=self.registry
        )
        
        self.cpu_usage_gauge = Gauge(
            'ai_model_cpu_usage',
            'AI模型CPU使用率',
            registry=self.registry
        )
        
        self.memory_usage_gauge = Gauge(
            'ai_model_memory_usage',
            'AI模型内存使用率',
            registry=self.registry
        )
        
        self.drift_score_gauge = Gauge(
            'ai_data_drift_score',
            'AI数据漂移分数',
            ['model_name'],
            registry=self.registry
        )
        
        self.concept_drift_gauge = Gauge(
            'ai_concept_drift_score',
            'AI概念漂移分数',
            ['model_name'],
            registry=self.registry
        )
        
        # 模拟数据
        self.models = ['recommendation', 'chatbot', 'search', 'classification']
        self.simulated_accuracy = {model: 0.9 for model in self.models}
        
    def simulate_metrics(self):
        """模拟指标更新"""
        while True:
            # 模拟请求和响应
            for model_name in self.models:
                # 随机生成请求
                for _ in range(random.randint(10, 50)):
                    status = 'success' if random.random() > 0.02 else 'error'
                    self.request_counter.labels(model_name=model_name, status=status).inc()
                    
                    if status == 'error':
                        self.error_counter.labels(model_name=model_name, error_type='prediction').inc()
                    
                    # 模拟延迟
                    latency = random.uniform(50, 500)
                    self.prediction_latency.labels(model_name=model_name).observe(latency)
                
                # 模拟准确率波动
                accuracy_change = random.uniform(-0.02, 0.02)
                self.simulated_accuracy[model_name] = max(0.7, min(0.98, 
                    self.simulated_accuracy[model_name] + accuracy_change))
                self.accuracy_gauge.labels(model_name=model_name).set(
                    self.simulated_accuracy[model_name])
                
                # 模拟P95延迟
                p95_latency = random.uniform(100, 600)
                self.p95_latency_gauge.labels(model_name=model_name).set(p95_latency)
                
                # 模拟吞吐量
                throughput = random.randint(50, 200)
                self.throughput_gauge.labels(model_name=model_name).set(throughput)
                
                # 模拟漂移分数
                drift_score = random.uniform(0.1, 0.4)
                self.drift_score_gauge.labels(model_name=model_name).set(drift_score)
                
                concept_drift = random.uniform(0.05, 0.3)
                self.concept_drift_gauge.labels(model_name=model_name).set(concept_drift)
            
            # 模拟资源使用
            cpu_usage = psutil.cpu_percent(interval=1) / 100
            self.cpu_usage_gauge.set(cpu_usage)
            
            memory_usage = psutil.virtual_memory().percent / 100
            self.memory_usage_gauge.set(memory_usage)
            
            time.sleep(10)  # 每10秒更新一次

class MetricsHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/metrics':
            # 返回Prometheus格式指标
            self.send_response(200)
            self.send_header('Content-Type', 'text/plain; version=0.0.4')
            self.end_headers()
            
            metrics_data = generate_latest(collector.registry)
            self.wfile.write(metrics_data)
        
        elif self.path == '/health':
            # 健康检查
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            health_status = {
                "status": "healthy",
                "timestamp": datetime.now().isoformat(),
                "version": "1.0.0",
                "models": collector.models
            }
            self.wfile.write(json.dumps(health_status).encode('utf-8'))
        
        elif self.path == '/api/models':
            # 返回模型列表
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            
            models_info = []
            for model in collector.models:
                models_info.append({
                    "name": model,
                    "accuracy": collector.simulated_accuracy[model],
                    "status": "active"
                })
            
            self.wfile.write(json.dumps(models_info).encode('utf-8'))
        
        else:
            self.send_response(404)
            self.end_headers()

def run_metrics_server():
    """运行指标服务器"""
    server = HTTPServer(('localhost', 8000), MetricsHandler)
    print(f"AI模型指标收集服务启动在 http://localhost:8000")
    print(f"指标端点: /metrics")
    print(f"健康检查: /health")
    print(f"模型列表: /api/models")
    
    # 启动指标模拟线程
    simulator_thread = Thread(target=collector.simulate_metrics, daemon=True)
    simulator_thread.start()
    
    server.serve_forever()

if __name__ == "__main__":
    collector = AIMetricsCollector()
    run_metrics_server()