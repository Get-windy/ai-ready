#!/usr/bin/env python3
"""
数据库性能监控服务
提供实时性能指标和健康检查端点
"""

import os
import time
import json
import logging
import threading
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Dict, List, Any, Optional
import psutil

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/app/logs/performance_monitor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class PerformanceMonitor:
    """性能监控器"""
    
    def __init__(self):
        self.metrics = {
            "system": {},
            "database": {},
            "test": {},
            "timestamp": datetime.now().isoformat()
        }
        self.running = True
        self.monitor_thread = None
    
    def start_monitoring(self):
        """启动监控"""
        logger.info("启动性能监控...")
        self.monitor_thread = threading.Thread(target=self._monitor_loop)
        self.monitor_thread.daemon = True
        self.monitor_thread.start()
    
    def stop_monitoring(self):
        """停止监控"""
        logger.info("停止性能监控...")
        self.running = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5)
    
    def _monitor_loop(self):
        """监控循环"""
        while self.running:
            try:
                # 收集系统指标
                self._collect_system_metrics()
                
                # 收集数据库指标
                self._collect_database_metrics()
                
                # 收集测试指标
                self._collect_test_metrics()
                
                # 更新时间戳
                self.metrics["timestamp"] = datetime.now().isoformat()
                
                # 每5秒收集一次
                time.sleep(5)
                
            except Exception as e:
                logger.error(f"监控收集失败: {str(e)}")
                time.sleep(10)
    
    def _collect_system_metrics(self):
        """收集系统指标"""
        try:
            # CPU使用率
            cpu_percent = psutil.cpu_percent(interval=1)
            
            # 内存使用
            memory = psutil.virtual_memory()
            
            # 磁盘使用
            disk = psutil.disk_usage('/')
            
            # 网络IO
            net_io = psutil.net_io_counters()
            
            self.metrics["system"] = {
                "cpu_percent": cpu_percent,
                "memory": {
                    "total": memory.total,
                    "available": memory.available,
                    "percent": memory.percent,
                    "used": memory.used,
                    "free": memory.free
                },
                "disk": {
                    "total": disk.total,
                    "used": disk.used,
                    "free": disk.free,
                    "percent": disk.percent
                },
                "network": {
                    "bytes_sent": net_io.bytes_sent,
                    "bytes_recv": net_io.bytes_recv,
                    "packets_sent": net_io.packets_sent,
                    "packets_recv": net_io.packets_recv
                },
                "process_count": len(psutil.pids()),
                "uptime": time.time() - psutil.boot_time()
            }
            
        except Exception as e:
            logger.warning(f"系统指标收集失败: {str(e)}")
            self.metrics["system"] = {"error": str(e)}
    
    def _collect_database_metrics(self):
        """收集数据库指标"""
        # 这里可以添加实际的数据库连接和指标收集
        # 目前使用模拟数据
        self.metrics["database"] = {
            "connections": {
                "active": 10,
                "idle": 5,
                "total": 15
            },
            "queries": {
                "total": 1000,
                "slow": 5,
                "avg_time_ms": 25.5
            },
            "cache": {
                "hit_rate": 0.95,
                "miss_rate": 0.05
            }
        }
    
    def _collect_test_metrics(self):
        """收集测试指标"""
        # 检查测试结果目录
        results_dir = "/app/results"
        if os.path.exists(results_dir):
            test_dirs = [d for d in os.listdir(results_dir) 
                        if os.path.isdir(os.path.join(results_dir, d))]
            
            self.metrics["test"] = {
                "total_tests": len(test_dirs),
                "latest_test": max(test_dirs) if test_dirs else None,
                "status": "running" if self.running else "stopped"
            }
        else:
            self.metrics["test"] = {
                "total_tests": 0,
                "latest_test": None,
                "status": "no_results"
            }
    
    def get_metrics(self) -> Dict[str, Any]:
        """获取当前指标"""
        return self.metrics
    
    def get_health_status(self) -> Dict[str, Any]:
        """获取健康状态"""
        system_ok = "system" in self.metrics and "error" not in self.metrics["system"]
        
        return {
            "status": "healthy" if system_ok else "unhealthy",
            "timestamp": datetime.now().isoformat(),
            "components": {
                "system_monitoring": system_ok,
                "database_monitoring": "database" in self.metrics,
                "test_monitoring": "test" in self.metrics
            },
            "uptime": self.metrics.get("system", {}).get("uptime", 0)
        }

class MonitorRequestHandler(BaseHTTPRequestHandler):
    """HTTP请求处理器"""
    
    def __init__(self, monitor: PerformanceMonitor, *args, **kwargs):
        self.monitor = monitor
        super().__init__(*args, **kwargs)
    
    def do_GET(self):
        """处理GET请求"""
        try:
            if self.path == "/":
                self._send_response(200, {"message": "Performance Monitor API", "status": "running"})
            
            elif self.path == "/health":
                health_status = self.monitor.get_health_status()
                self._send_response(200, health_status)
            
            elif self.path == "/metrics":
                metrics = self.monitor.get_metrics()
                self._send_response(200, metrics)
            
            elif self.path == "/status":
                status = {
                    "running": self.monitor.running,
                    "timestamp": datetime.now().isoformat(),
                    "endpoints": [
                        "/health - 健康检查",
                        "/metrics - 性能指标",
                        "/status - 服务状态"
                    ]
                }
                self._send_response(200, status)
            
            else:
                self._send_response(404, {"error": "Endpoint not found", "path": self.path})
                
        except Exception as e:
            logger.error(f"请求处理失败: {str(e)}")
            self._send_response(500, {"error": str(e)})
    
    def do_POST(self):
        """处理POST请求"""
        try:
            if self.path == "/start-test":
                content_length = int(self.headers.get('Content-Length', 0))
                post_data = self.rfile.read(content_length)
                test_config = json.loads(post_data.decode('utf-8'))
                
                response = {
                    "message": "Test started",
                    "config": test_config,
                    "timestamp": datetime.now().isoformat()
                }
                self._send_response(200, response)
            
            elif self.path == "/stop-test":
                response = {
                    "message": "Test stopped",
                    "timestamp": datetime.now().isoformat()
                }
                self._send_response(200, response)
            
            else:
                self._send_response(404, {"error": "Endpoint not found", "path": self.path})
                
        except Exception as e:
            logger.error(f"POST请求处理失败: {str(e)}")
            self._send_response(500, {"error": str(e)})
    
    def _send_response(self, status_code: int, data: Dict[str, Any]):
        """发送HTTP响应"""
        self.send_response(status_code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()
        
        response_json = json.dumps(data, indent=2, ensure_ascii=False)
        self.wfile.write(response_json.encode('utf-8'))
    
    def log_message(self, format, *args):
        """自定义日志消息格式"""
        logger.info(f"{self.address_string()} - {format % args}")

def run_monitor_server(host: str = "0.0.0.0", port: int = 8000):
    """运行监控服务器"""
    monitor = PerformanceMonitor()
    monitor.start_monitoring()
    
    def handler_factory(*args, **kwargs):
        return MonitorRequestHandler(monitor, *args, **kwargs)
    
    server_address = (host, port)
    httpd = HTTPServer(server_address, handler_factory)
    
    logger.info(f"性能监控服务启动在 http://{host}:{port}")
    logger.info("可用端点:")
    logger.info("  GET /health     - 健康检查")
    logger.info("  GET /metrics    - 性能指标")
    logger.info("  GET /status     - 服务状态")
    logger.info("  POST /start-test - 启动测试")
    logger.info("  POST /stop-test  - 停止测试")
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logger.info("收到中断信号，停止服务...")
    finally:
        monitor.stop_monitoring()
        httpd.server_close()
        logger.info("服务已停止")

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="数据库性能监控服务")
    parser.add_argument("--host", default="0.0.0.0", help="监听主机")
    parser.add_argument("--port", type=int, default=8000, help="监听端口")
    
    args = parser.parse_args()
    
    run_monitor_server(host=args.host, port=args.port)