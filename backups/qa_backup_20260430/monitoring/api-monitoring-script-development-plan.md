# 【Sprint 27+1】测试环境API接口监控脚本开发方案

## 1. 方案概述

### 1.1 目标
为Sprint 27+1测试环境开发API接口监控脚本，确保API服务正常运行，及时发现和解决接口问题。

### 1.2 范围
- API接口可用性监控
- API响应时间监控
- API错误率监控
- API性能指标监控
- 监控数据收集和告警

### 1.3 监控对象
- 用户服务API (user-service:8085)
- 订单服务API (order-service:8086)
- 库存服务API (inventory-service:8082)
- CRM服务API (crm-service:8087)
- ERP服务API (erp-service:8088)

## 2. 监控指标体系

### 2.1 可用性指标
| 指标 | 描述 | 监控频率 | 告警阈值 |
|------|------|---------|---------|
| API可用性 | 接口HTTP状态码 | 30秒 | 连续3次失败 |
| 服务健康检查 | Spring Boot Actuator健康端点 | 30秒 | health状态不为UP |
| 服务发现 | 服务注册中心状态 | 30秒 | 服务未注册 |

### 2.2 性能指标
| 指标 | 描述 | 监控频率 | 告警阈值 |
|------|------|---------|---------|
| 响应时间P95 | 95%请求响应时间 | 30秒 | > 500ms |
| 响应时间P99 | 99%请求响应时间 | 30秒 | > 1000ms |
| 吞吐量 | 每秒请求数(QPS) | 30秒 | < 10 或 > 1000 |
| 并发数 | 同时处理的请求数 | 30秒 | > 100 |

### 2.3 错误指标
| 指标 | 描述 | 监控频率 | 告警阈值 |
|------|------|---------|---------|
| 错误率 | HTTP错误码比例 | 30秒 | > 1% |
| 4xx错误率 | 客户端错误比例 | 30秒 | > 0.5% |
| 5xx错误率 | 服务器错误比例 | 30秒 | > 0.1% |
| 超时率 | 请求超时比例 | 30秒 | > 0.5% |

### 2.4 业务指标
| 指标 | 描述 | 监控频率 | 告警阈值 |
|------|------|---------|---------|
| 关键接口成功率 | 登录、下单等关键接口 | 30秒 | < 99% |
| 数据一致性 | 跨服务数据一致性检查 | 5分钟 | 不一致 |
| 业务流水 | 关键业务流水监控 | 1分钟 | 异常波动 |

## 3. 监控脚本设计

### 3.1 脚本架构
```
API监控脚本架构：
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  监控目标配置   │    │  监控执行引擎   │    │  数据收集器     │
│  - 服务列表     │    │  - 并发执行     │    │  - 指标计算     │
│  - 接口列表     │    │  - 超时控制     │    │  - 数据存储     │
│  - 检查频率     │    │  - 重试机制     │    │  - 数据格式化   │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         └──────────┬───────────┴──────────────────────┘
                    │
                    ▼
           ┌─────────────────┐    ┌─────────────────┐
           │  告警引擎       │    │  报告生成器     │
           │  - 规则匹配     │    │  - 日报/周报    │
           │  - 告警发送     │    │  - 性能报告     │
           │  - 告警抑制     │    │  - 趋势分析     │
           └─────────────────┘    └─────────────────┘
```

### 3.2 技术选型
- **开发语言**: Python 3.8+ (兼容性好，库丰富)
- **HTTP客户端**: requests + aiohttp (同步+异步)
- **配置管理**: YAML配置文件
- **数据存储**: JSON文件 + Prometheus Pushgateway
- **任务调度**: schedule库 + systemd/cron
- **日志记录**: logging模块 + JSON格式

### 3.3 目录结构
```
scripts/monitoring/api/
├── config/                    # 配置文件
│   ├── services.yaml         # 服务配置
│   ├── endpoints.yaml        # 接口端点配置
│   ├── alerts.yaml           # 告警规则配置
│   └── prometheus.yaml       # Prometheus配置
├── src/                      # 源代码
│   ├── monitor.py            # 主监控脚本
│   ├── checker.py            # 检查器模块
│   ├── collector.py          # 数据收集器
│   ├── alert.py              # 告警模块
│   ├── reporter.py           # 报告生成器
│   └── utils.py              # 工具函数
├── tests/                    # 测试代码
│   ├── test_monitor.py
│   ├── test_checker.py
│   └── test_data/
├── logs/                     # 日志文件
│   ├── monitor.log
│   ├── error.log
│   └── access.log
├── data/                     # 监控数据
│   ├── metrics/
│   ├── alerts/
│   └── reports/
├── requirements.txt          # Python依赖
├── docker-compose.yml        # Docker部署
└── README.md                 # 使用文档
```

## 4. 监控脚本实现

### 4.1 服务配置 (config/services.yaml)
```yaml
services:
  user-service:
    name: "用户服务"
    host: "user-service"
    port: 8085
    health_endpoint: "/actuator/health"
    metrics_endpoint: "/actuator/prometheus"
    endpoints:
      - name: "用户登录"
        path: "/api/v1/auth/login"
        method: "POST"
        expected_status: 200
        timeout: 5
        check_interval: 30
        alert_threshold: 3
        
      - name: "用户信息查询"
        path: "/api/v1/users/{id}"
        method: "GET"
        expected_status: 200
        timeout: 3
        check_interval: 30
        alert_threshold: 3
        
      - name: "用户列表"
        path: "/api/v1/users"
        method: "GET"
        expected_status: 200
        timeout: 3
        check_interval: 30
        alert_threshold: 3

  order-service:
    name: "订单服务"
    host: "order-service"
    port: 8086
    health_endpoint: "/actuator/health"
    metrics_endpoint: "/actuator/prometheus"
    endpoints:
      - name: "创建订单"
        path: "/api/v1/orders"
        method: "POST"
        expected_status: 201
        timeout: 10
        check_interval: 30
        alert_threshold: 3
        
      - name: "订单查询"
        path: "/api/v1/orders/{id}"
        method: "GET"
        expected_status: 200
        timeout: 3
        check_interval: 30
        alert_threshold: 3
        
      - name: "订单列表"
        path: "/api/v1/orders"
        method: "GET"
        expected_status: 200
        timeout: 5
        check_interval: 30
        alert_threshold: 3

  inventory-service:
    name: "库存服务"
    host: "inventory-service"
    port: 8082
    health_endpoint: "/actuator/health"
    metrics_endpoint: "/actuator/prometheus"
    endpoints:
      - name: "库存查询"
        path: "/api/v1/inventory/{productId}"
        method: "GET"
        expected_status: 200
        timeout: 3
        check_interval: 30
        alert_threshold: 3
        
      - name: "库存更新"
        path: "/api/v1/inventory/{productId}"
        method: "PUT"
        expected_status: 200
        timeout: 5
        check_interval: 30
        alert_threshold: 3

  crm-service:
    name: "CRM服务"
    host: "crm-service"
    port: 8087
    health_endpoint: "/actuator/health"
    metrics_endpoint: "/actuator/prometheus"
    endpoints:
      - name: "客户查询"
        path: "/api/v1/customers/{id}"
        method: "GET"
        expected_status: 200
        timeout: 3
        check_interval: 30
        alert_threshold: 3
        
      - name: "客户列表"
        path: "/api/v1/customers"
        method: "GET"
        expected_status: 200
        timeout: 5
        check_interval: 30
        alert_threshold: 3

  erp-service:
    name: "ERP服务"
    host: "erp-service"
    port: 8088
    health_endpoint: "/actuator/health"
    metrics_endpoint: "/actuator/prometheus"
    endpoints:
      - name: "采购订单查询"
        path: "/api/v1/purchase-orders/{id}"
        method: "GET"
        expected_status: 200
        timeout: 3
        check_interval: 30
        alert_threshold: 3
        
      - name: "采购订单列表"
        path: "/api/v1/purchase-orders"
        method: "GET"
        expected_status: 200
        timeout: 5
        check_interval: 30
        alert_threshold: 3

# 全局配置
global:
  check_interval: 30  # 默认检查间隔(秒)
  timeout: 10         # 默认超时时间(秒)
  retry_times: 2      # 默认重试次数
  alert_threshold: 3  # 默认告警阈值(连续失败次数)
  
  # 告警配置
  alert:
    enabled: true
    channels:
      - type: "prometheus"
        endpoint: "http://prometheus:9090"
      - type: "webhook"
        endpoint: "http://alertmanager:9093/api/v1/alerts"
      - type: "email"
        smtp_server: "smtp.example.com"
        smtp_port: 587
        sender: "monitor@ai-ready.com"
        receivers:
          - "admin@ai-ready.com"
          - "devops@ai-ready.com"
          
  # 日志配置
  logging:
    level: "INFO"
    format: "json"
    file: "/var/log/api-monitor/monitor.log"
    max_size_mb: 100
    backup_count: 10
    
  # 数据存储
  storage:
    metrics_dir: "/var/lib/api-monitor/metrics"
    alerts_dir: "/var/lib/api-monitor/alerts"
    reports_dir: "/var/lib/api-monitor/reports"
    retention_days: 30
```

### 4.2 主监控脚本 (src/monitor.py)
```python
#!/usr/bin/env python3
"""
API接口监控脚本主程序
版本: 1.0
创建日期: 2026-04-28
"""

import asyncio
import logging
import signal
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any

import yaml
from aiohttp import ClientSession, ClientTimeout

from checker import APIChecker
from collector import MetricsCollector
from alert import AlertManager
from reporter import ReportGenerator
from utils import setup_logging, load_config, validate_config

class APIMonitor:
    """API监控主类"""
    
    def __init__(self, config_path: str):
        """初始化监控器"""
        self.config_path = config_path
        self.config = None
        self.checker = None
        self.collector = None
        self.alert_manager = None
        self.reporter = None
        self.running = False
        self.logger = logging.getLogger(__name__)
        
    def setup(self):
        """设置监控器"""
        try:
            # 加载配置
            self.config = load_config(self.config_path)
            validate_config(self.config)
            
            # 初始化组件
            self.checker = APIChecker(self.config)
            self.collector = MetricsCollector(self.config)
            self.alert_manager = AlertManager(self.config)
            self.reporter = ReportGenerator(self.config)
            
            self.logger.info("API监控器初始化完成")
            return True
            
        except Exception as e:
            self.logger.error(f"监控器初始化失败: {e}")
            return False
    
    async def run_checks(self):
        """执行API检查"""
        try:
            start_time = time.time()
            
            # 执行健康检查
            health_results = await self.checker.check_health()
            
            # 执行端点检查
            endpoint_results = await self.checker.check_endpoints()
            
            # 收集指标
            metrics = self.collector.collect(health_results, endpoint_results)
            
            # 检查告警
            alerts = self.alert_manager.check_alerts(metrics)
            
            # 发送告警
            if alerts:
                await self.alert_manager.send_alerts(alerts)
            
            # 记录指标
            self.collector.store_metrics(metrics)
            
            # 记录执行时间
            duration = time.time() - start_time
            self.logger.info(f"检查完成，耗时: {duration:.2f}秒")
            
            return metrics, alerts
            
        except Exception as e:
            self.logger.error(f"检查执行失败: {e}")
            return None, None
    
    async def run(self):
        """运行监控器主循环"""
        self.running = True
        self.logger.info("API监控器开始运行")
        
        # 设置信号处理
        loop = asyncio.get_event_loop()
        for sig in (signal.SIGTERM, signal.SIGINT):
            loop.add_signal_handler(sig, self.stop)
        
        try:
            while self.running:
                # 执行检查
                metrics, alerts = await self.run_checks()
                
                # 生成报告（每小时一次）
                current_hour = datetime.now().hour
                if current_hour != getattr(self, 'last_report_hour', -1):
                    if metrics:
                        report = self.reporter.generate_hourly_report(metrics, alerts)
                        self.reporter.save_report(report)
                    self.last_report_hour = current_hour
                
                # 等待下次检查
                check_interval = self.config['global']['check_interval']
                await asyncio.sleep(check_interval)
                
        except asyncio.CancelledError:
            self.logger.info("监控器任务被取消")
        except Exception as e:
            self.logger.error(f"监控器运行异常: {e}")
        finally:
            self.cleanup()
    
    def stop(self):
        """停止监控器"""
        self.logger.info("收到停止信号，正在停止监控器...")
        self.running = False
    
    def cleanup(self):
        """清理资源"""
        self.logger.info("清理监控器资源...")
        if self.checker:
            self.checker.cleanup()
        if self.alert_manager:
            self.alert_manager.cleanup()
        self.logger.info("监控器已停止")

async def main():
    """主函数"""
    # 设置日志
    setup_logging()
    logger = logging.getLogger(__name__)
    
    # 解析命令行参数
    import argparse
    parser = argparse.ArgumentParser(description='API接口监控脚本')
    parser.add_argument('--config', '-c', default='config/services.yaml',
                       help='配置文件路径')
    parser.add_argument('--daemon', '-d', action='store_true',
                       help='以守护进程方式运行')
    args = parser.parse_args()
    
    # 创建监控器
    monitor = APIMonitor(args.config)
    
    if not monitor.setup():
        logger.error("监控器设置失败，退出")
        sys.exit(1)
    
    try:
        # 运行监控器
        await monitor.run()
    except KeyboardInterrupt:
        logger.info("收到键盘中断信号")
    finally:
        monitor.cleanup()

if __name__ == "__main__":
    asyncio.run(main())
```

### 4.3 API检查器模块 (src/checker.py)
```python
"""
API检查器模块
负责执行具体的API检查
"""

import asyncio
import logging
from datetime import datetime
from typing import Dict, List, Any, Optional
from urllib.parse import urljoin

import aiohttp
from aiohttp import ClientSession, ClientTimeout, ClientResponse

logger = logging.getLogger(__name__)

class APIChecker:
    """API检查器"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化检查器"""
        self.config = config
        self.services = config['services']
        self.session = None
        self.timeout = ClientTimeout(total=config['global']['timeout'])
        
    async def __aenter__(self):
        """异步上下文管理器入口"""
        await self.setup()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """异步上下文管理器出口"""
        await self.cleanup()
    
    async def setup(self):
        """设置检查器"""
        self.session = ClientSession(timeout=self.timeout)
        
    async def cleanup(self):
        """清理检查器"""
        if self.session:
            await self.session.close()
    
    async def check_health(self) -> Dict[str, Any]:
        """检查服务健康状态"""
        health_results = {}
        
        tasks = []
        for service_id, service_config in self.services.items():
            task = self._check_service_health(service_id, service_config)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        for service_id, result in zip(self.services.keys(), results):
            if isinstance(result, Exception):
                health_results[service_id] = {
                    'status': 'error',
                    'error': str(result),
                    'timestamp': datetime.now().isoformat()
                }
            else:
                health_results[service_id] = result
        
        return health_results
    
    async def _check_service_health(self, service_id: str, service_config: Dict[str, Any]) -> Dict[str, Any]:
        """检查单个服务健康状态"""
        try:
            url = self._build_url(service_config, service_config['health_endpoint'])
            
            async with self.session.get(url) as response:
                status = response.status
                response_data = await response.json() if response.content_type == 'application/json' else {}
                
                result = {
                    'service_id': service_id,
                    'service_name': service_config['name'],
                    'status': 'healthy' if status == 200 and response_data.get('status') == 'UP' else 'unhealthy',
                    'http_status': status,
                    'response_data': response_data,
                    'response_time': response.elapsed.total_seconds(),
                    'timestamp': datetime.now().isoformat()
                }
                
                logger.debug(f"服务健康检查: {service_id} - {result['status']}")
                return result
                
        except asyncio.TimeoutError:
            return {
                'service_id': service_id,
                'service_name': service_config['name'],
                'status': 'timeout',
                'http_status': 0,
                'response_time': self.timeout.total,
                'error': '请求超时',
                'timestamp': datetime.now().isoformat()
            }
        except Exception as e:
            return {
                'service_id': service_id,
                'service_name': service_config['name'],
                'status': 'error',
                'http_status': 0,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    async def check_endpoints(self) -> Dict[str, List[Dict[str, Any]]]:
        """检查所有端点"""
        endpoint_results = {}
        
        for service_id, service_config in self.services.items():
            endpoints = service_config.get('endpoints', [])
            if not endpoints:
                continue
            
            service_results = await self._check_service_endpoints(service_id, service_config, endpoints)
            endpoint_results[service_id] = service_results
        
        return endpoint_results
    
    async def _check_service_endpoints(self, service_id: str, service_config: Dict[str, Any], 
                                      endpoints: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """检查单个服务的所有端点"""
        tasks = []
        for endpoint_config in endpoints:
            task = self._check_endpoint(service_id, service_config, endpoint_config)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        service_results = []
        for endpoint_config, result in zip(endpoints, results):
            if isinstance(result, Exception):
                service_results.append({
                    'endpoint_name': endpoint_config['name'],
                    'status': 'error',
                    'error': str(result),
                    'timestamp': datetime.now().isoformat()
                })
            else:
                service_results.append(result)
        
        return service_results
    
    async def _check_endpoint(self, service_id: str, service_config: Dict[str, Any], 
                             endpoint_config: Dict[str, Any]) -> Dict[str, Any]:
        """检查单个端点"""
        try:
            # 构建URL（处理路径参数）
            path = endpoint_config['path']
            # 这里可以添加路径参数替换逻辑
            url = self._build_url(service_config, path)
            
            # 准备请求
            method = endpoint_config['method'].lower()
            timeout = endpoint_config.get('timeout', self.config['global']['timeout'])
            endpoint_timeout = ClientTimeout(total=timeout)
            
            # 执行请求
            start_time = datetime.now()
            
            async with aiohttp.ClientSession(timeout=endpoint_timeout) as session:
                if method == 'get':
                    async with session.get(url) as response:
                        return await self._process_response(service_id, service_config, endpoint_config, response, start_time)
                elif method == 'post':
                    # 这里可以添加请求体
                    async with session.post(url) as response:
                        return await self._process_response(service_id, service_config, endpoint_config, response, start_time)
                elif method == 'put':
                    async with session.put(url) as response:
                        return await self._process_response(service_id, service_config, endpoint_config, response, start_time)
                elif method == 'delete':
                    async with session.delete(url) as response:
                        return await self._process_response(service_id, service_config, endpoint_config, response, start_time)
                else:
                    raise ValueError(f"不支持的HTTP方法: {method}")
                    
        except asyncio.TimeoutError:
            return {
                'service_id': service_id,
                'service_name': service_config['name'],
                'endpoint_name': endpoint_config['name'],
                'endpoint_path': endpoint_config['path'],
                'method': endpoint_config['method'],
                'status': 'timeout',
                'http_status': 0,
                'response_time': timeout,
                'error': '请求超时',
                'timestamp': datetime.now().isoformat()
            }
        except Exception as e:
            return {
                'service_id': service_id,
                'service_name': service_config['name'],
                'endpoint_name': endpoint_config['name'],
                'endpoint_path': endpoint_config['path'],
                'method': endpoint_config['method'],
                'status': 'error',
                'http_status': 0,
                'error': str(e),
                'timestamp': datetime.now().isoformat()
            }
    
    async def _process_response(self, service_id: str, service_config: Dict[str, Any], 
                               endpoint_config: Dict[str, Any], response: ClientResponse, 
                               start_time: datetime) -> Dict[str, Any]:
        """处理HTTP响应"""
        end_time = datetime.now()
        response_time = (end_time - start_time).total_seconds()
        
        expected_status = endpoint_config.get('expected_status', 200)
        status_match = response.status == expected_status
        
        # 读取响应体（如果内容类型是JSON）
        response_data = None
        if response.content_type == 'application/json':
            try:
                response_data = await response.json()
            except:
                response_data = await response.text()
        
        result = {
            'service_id': service_id,
            'service_name': service_config['name'],
            'endpoint_name': endpoint_config['name'],
            'endpoint_path': endpoint_config['path'],
            'method': endpoint_config['method'],
            'status': 'success' if status_match else 'failed',
            'http_status': response.status,
            'expected_status': expected_status,
            'response_time': response_time,
            'response_size': response.content_length or 0,
            'response_data': response_data,
            'timestamp': end_time.isoformat()
        }
        
        logger.debug(f"端点检查: {service_id}/{endpoint_config['name']} - {result['status']}")
        return result
    
    def _build_url(self, service_config: Dict[str, Any], path: str) -> str:
        """构建完整的URL"""
        base_url = f"http://{service_config['host']}:{service_config['port']}"
        return urljoin(base_url, path)
```

### 4.4 数据收集器模块 (src/collector.py)
```python
"""
数据收集器模块
负责收集和存储监控指标
"""

import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any
from collections import defaultdict

logger = logging.getLogger(__name__)

class MetricsCollector:
    """指标收集器"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化收集器"""
        self.config = config
        self.storage_config = config['global']['storage']
        self.metrics_dir = Path(self.storage_config['metrics_dir'])
        self.metrics_dir.mkdir(parents=True, exist_ok=True)
        
        # 初始化指标缓存
        self.metrics_cache = defaultdict(list)
        
    def collect(self, health_results: Dict[str, Any], endpoint_results: Dict[str, List[Dict[str, Any]]]) -> Dict[str, Any]:
        """收集监控指标"""
        timestamp = datetime.now()
        metrics = {
            'timestamp': timestamp.isoformat(),
            'health_metrics': {},
            'endpoint_metrics': {},
            'summary_metrics': {}
        }
        
        # 收集健康指标
        for service_id, health_data in health_results.items():
            metrics['health_metrics'][service_id] = self._extract_health_metrics(health_data)
        
        # 收集端点指标
        for service_id, endpoint_list in endpoint_results.items():
            service_metrics = []
            for endpoint_data in endpoint_list:
                service_metrics.append(self._extract_endpoint_metrics(endpoint_data))
            metrics['endpoint_metrics'][service_id] = service_metrics
        
        # 计算汇总指标
        metrics['summary_metrics'] = self._calculate_summary_metrics(metrics)
        
        # 缓存指标
        self._cache_metrics(metrics)
        
        return metrics
    
    def _extract_health_metrics(self, health_data: Dict[str, Any]) -> Dict[str, Any]:
        """提取健康指标"""
        return {
            'status': health_data.get('status', 'unknown'),
            'http_status': health_data.get('http_status', 0),
            'response_time': health_data.get('response_time', 0),
            'timestamp': health_data.get('timestamp', datetime.now().isoformat())
        }
    
    def _extract_endpoint_metrics(self, endpoint_data: Dict[str, Any]) -> Dict[str, Any]:
        """提取端点指标"""
        return {
            'endpoint_name': endpoint_data.get('endpoint_name', 'unknown'),
            'endpoint_path': endpoint_data.get('endpoint_path', ''),
            'method': endpoint_data.get('method', 'GET'),
            'status': endpoint_data.get('status', 'unknown'),
            'http_status': endpoint_data.get('http_status', 0),
            'expected_status': endpoint_data.get('expected_status', 200),
            'response_time': endpoint_data.get('response_time', 0),
            'response_size': endpoint_data.get('response_size', 0),
            'timestamp': endpoint_data.get('timestamp', datetime.now().isoformat())
        }
    
    def _calculate_summary_metrics(self, metrics: Dict[str, Any]) -> Dict[str, Any]:
        """计算汇总指标"""
        summary = {
            'total_services': len(metrics['health_metrics']),
            'healthy_services': 0,
            'unhealthy_services': 0,
            'total_endpoints': 0,
            'successful_endpoints': 0,
            'failed_endpoints': 0,
            'average_response_time': 0,
            'total_errors': 0
        }
        
        # 统计服务健康状态
        for service_id, health_metric in metrics['health_metrics'].items():
            if health_metric['status'] == 'healthy':
                summary['healthy_services'] += 1
            else:
                summary['unhealthy_services'] += 1
                summary['total_errors'] += 1
        
        # 统计端点状态
        response_times = []
        for service_id, endpoint_list in metrics['endpoint_metrics'].items():
            summary['total_endpoints'] += len(endpoint_list)
            
            for endpoint_metric in endpoint_list:
                if endpoint_metric['status'] == 'success':
                    summary['successful_endpoints'] += 1
                else:
                    summary['failed_endpoints'] += 1
                    summary['total_errors'] += 1
                
                if endpoint_metric['response_time'] > 0:
                    response_times.append(endpoint_metric['response_time'])
        
        # 计算平均响应时间
        if response_times:
            summary['average_response_time'] = sum(response_times) / len(response_times)
        
        # 计算成功率
        if summary['total_endpoints'] > 0:
            summary['success_rate'] = summary['successful_endpoints'] / summary['total_endpoints'] * 100
        else:
            summary['success_rate'] = 0
        
        if summary['total_services'] > 0:
            summary['health_rate'] = summary['healthy_services'] / summary['total_services'] * 100
        else:
            summary['health_rate'] = 0
        
        return summary
    
    def _cache_metrics(self, metrics: Dict[str, Any]):
        """缓存指标"""
        timestamp = metrics['timestamp']
        date_key = timestamp[:10]  # YYYY-MM-DD
        
        # 添加到缓存
        self.metrics_cache[date_key].append(metrics)
        
        # 限制缓存大小
        max_cache_size = 1000
        if len(self.metrics_cache[date_key]) > max_cache_size:
            self.metrics_cache[date_key] = self.metrics_cache[date_key][-max_cache_size:]
    
    def store_metrics(self, metrics: Dict[str, Any]):
        """存储指标到文件"""
        try:
            timestamp = metrics['timestamp']
            date_key = timestamp[:10]
            hour_key = timestamp[:13]  # YYYY-MM-DDTHH
            
            # 按日期存储
            date_file = self.metrics_dir / f"{date_key}.json"
            
            # 读取现有数据
            existing_data = []
            if date_file.exists():
                try:
                    with open(date_file, 'r') as f:
                        existing_data = json.load(f)
                except (json.JSONDecodeError, IOError):
                    existing_data = []
            
            # 添加新数据
            existing_data.append(metrics)
            
            # 保存数据
            with open(date_file, 'w') as f:
                json.dump(existing_data, f, indent=2, default=str)
            
            # 按小时存储（用于快速查询）
            hour_dir = self.metrics_dir / date_key
            hour_dir.mkdir(exist_ok=True)
            
            hour_file = hour_dir / f"{hour_key}.json"
            hour_data = []
            if hour_file.exists():
                try:
                    with open(hour_file, 'r') as f:
                        hour_data = json.load(f)
                except (json.JSONDecodeError, IOError):
                    hour_data = []
            
            hour_data.append(metrics)
            
            with open(hour_file, 'w') as f:
                json.dump(hour_data, f, indent=2, default=str)
            
            logger.debug(f"指标已存储: {date_file}, {hour_file}")
            
        except Exception as e:
            logger.error(f"存储指标失败: {e}")
    
    def get_metrics(self, start_time: datetime, end_time: datetime) -> List[Dict[str, Any]]:
        """获取指定时间范围内的指标"""
        result = []
        
        try:
            # 遍历日期范围
            current_date = start_time.date()
            end_date = end_time.date()
            
            while current_date <= end_date:
                date_key = current_date.isoformat()
                date_file = self.metrics_dir / f"{date_key}.json"
                
                if date_file.exists():
                    try:
                        with open(date_file, 'r') as f:
                            date_metrics = json.load(f)
                        
                        # 过滤时间范围
                        for metric in date_metrics:
                            metric_time = datetime.fromisoformat(metric['timestamp'])
                            if start_time <= metric_time <= end_time:
                                result.append(metric)
                    except (json.JSONDecodeError, IOError) as e:
                        logger.warning(f"读取指标文件失败 {date_file}: {e}")
                
                current_date = current_date.replace(day=current_date.day + 1)
            
        except Exception as e:
            logger.error(f"获取指标失败: {e}")
        
        return result
    
    def cleanup_old_metrics(self):
        """清理旧的指标数据"""
        try:
            retention_days = self.storage_config.get('retention_days', 30)
            cutoff_date = datetime.now().replace(day=datetime.now().day - retention_days).date()
            
            for metric_file in self.metrics_dir.glob("*.json"):
                # 从文件名提取日期
                try:
                    file_date = datetime.strptime(metric_file.stem, "%Y-%m-%d").date()
                    if file_date < cutoff_date:
                        metric_file.unlink()
                        logger.info(f"删除旧指标文件: {metric_file}")
                except ValueError:
                    continue
            
            # 清理小时目录
            for date_dir in self.metrics_dir.iterdir():
                if date_dir.is_dir():
                    try:
                        dir_date = datetime.strptime(date_dir.name, "%Y-%m-%d").date()
                        if dir_date < cutoff_date:
                            import shutil
                            shutil.rmtree(date_dir)
                            logger.info(f"删除旧指标目录: {date_dir}")
                    except ValueError:
                        continue
                        
        except Exception as e:
            logger.error(f"清理旧指标失败: {e}")
```

### 4.5 告警模块 (src/alert.py)
```python
"""
告警模块
负责检查告警条件和发送告警
"""

import json
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from collections import defaultdict

logger = logging.getLogger(__name__)

class AlertManager:
    """告警管理器"""
    
    def __init__(self, config: Dict[str, Any]):
        """初始化告警管理器"""
        self.config = config
        self.alert_config = config['global']['alert']
        self.storage_config = config['global']['storage']
        
        self.alerts_dir = Path(self.storage_config['alerts_dir'])
        self.alerts_dir.mkdir(parents=True, exist_ok=True)
        
        # 告警状态缓存
        self.alert_state = defaultdict(dict)
        self.active_alerts = {}
        
        # 告警抑制
        self.suppressed_alerts = set()
        
    def check_alerts(self, metrics: Dict[str, Any]) -> List[Dict