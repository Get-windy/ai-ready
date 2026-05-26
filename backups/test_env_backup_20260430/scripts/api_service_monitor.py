#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1 API服务启动监控脚本
监控API服务启动状态，准备连接池配置
作者: test-agent-2
日期: 2026-04-29
"""

import asyncio
import aiohttp
import time
import json
import logging
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from datetime import datetime
import statistics

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class ServiceEndpoint:
    """服务端点配置"""
    name: str
    url: str
    method: str = "GET"
    expected_status: int = 200
    timeout: int = 10
    required: bool = True
    health_check_path: str = "/actuator/health"
    api_path: str = "/api"


@dataclass
class ConnectionPoolConfig:
    """连接池配置"""
    max_connections: int = 100
    max_connections_per_host: int = 20
    keepalive_timeout: int = 30
    connection_timeout: int = 10
    read_timeout: int = 30
    retry_attempts: int = 3
    retry_delay: float = 1.0
    backoff_factor: float = 2.0


class APIServiceMonitor:
    """API服务监控器"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.endpoints = self._create_endpoints()
        self.connection_config = ConnectionPoolConfig()
        self.session: Optional[aiohttp.ClientSession] = None
        
    def _create_endpoints(self) -> List[ServiceEndpoint]:
        """创建监控端点"""
        return [
            ServiceEndpoint(
                name="API Gateway",
                url=f"{self.base_url}/actuator/health",
                health_check_path="/actuator/health",
                api_path="/api"
            ),
            ServiceEndpoint(
                name="User Service",
                url=f"{self.base_url}/api/users/health",
                health_check_path="/actuator/health",
                api_path="/api/users"
            ),
            ServiceEndpoint(
                name="Order Service",
                url=f"{self.base_url}/api/orders/health",
                health_check_path="/actuator/health",
                api_path="/api/orders"
            ),
            ServiceEndpoint(
                name="Inventory Service",
                url=f"{self.base_url}/api/inventory/health",
                health_check_path="/actuator/health",
                api_path="/api/inventory"
            ),
            ServiceEndpoint(
                name="Monitoring Service",
                url=f"{self.base_url}/api/monitor/health",
                health_check_path="/actuator/health",
                api_path="/api/monitor"
            )
        ]
    
    def create_connection_pool_config(self) -> Dict:
        """创建连接池配置"""
        return {
            "connector": {
                "limit": self.connection_config.max_connections,
                "limit_per_host": self.connection_config.max_connections_per_host,
                "keepalive_timeout": self.connection_config.keepalive_timeout
            },
            "timeout": aiohttp.ClientTimeout(
                total=self.connection_config.connection_timeout,
                connect=self.connection_config.connection_timeout,
                sock_read=self.connection_config.read_timeout
            ),
            "retry_options": {
                "attempts": self.connection_config.retry_attempts,
                "delay": self.connection_config.retry_delay,
                "backoff_factor": self.connection_config.backoff_factor
            },
            "headers": {
                "User-Agent": "Sprint27+1-Performance-Test/1.0",
                "Accept": "application/json",
                "Content-Type": "application/json"
            }
        }
    
    async def check_service_health(self, endpoint: ServiceEndpoint) -> Dict:
        """检查服务健康状态"""
        start_time = time.time()
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(
                    endpoint.url,
                    timeout=endpoint.timeout
                ) as response:
                    response_time = (time.time() - start_time) * 1000
                    
                    if response.status == endpoint.expected_status:
                        try:
                            data = await response.json()
                            return {
                                "name": endpoint.name,
                                "status": "healthy",
                                "response_time_ms": round(response_time, 2),
                                "http_status": response.status,
                                "data": data,
                                "url": endpoint.url
                            }
                        except:
                            return {
                                "name": endpoint.name,
                                "status": "healthy",
                                "response_time_ms": round(response_time, 2),
                                "http_status": response.status,
                                "url": endpoint.url
                            }
                    else:
                        return {
                            "name": endpoint.name,
                            "status": "unhealthy",
                            "response_time_ms": round(response_time, 2),
                            "http_status": response.status,
                            "error": f"Unexpected status: {response.status}",
                            "url": endpoint.url
                        }
        except asyncio.TimeoutError:
            return {
                "name": endpoint.name,
                "status": "timeout",
                "response_time_ms": endpoint.timeout * 1000,
                "error": f"Request timeout after {endpoint.timeout}s",
                "url": endpoint.url
            }
        except Exception as e:
            return {
                "name": endpoint.name,
                "status": "error",
                "response_time_ms": round((time.time() - start_time) * 1000, 2),
                "error": str(e),
                "url": endpoint.url
            }
    
    async def wait_for_service_startup(self, timeout: int = 300, interval: int = 5) -> Dict:
        """等待服务启动"""
        logger.info(f"等待API服务启动，超时时间: {timeout}秒，检查间隔: {interval}秒")
        
        start_time = time.time()
        check_count = 0
        results_history = []
        
        while time.time() - start_time < timeout:
            check_count += 1
            logger.info(f"第{check_count}次检查服务状态...")
            
            # 并发检查所有端点
            tasks = [self.check_service_health(endpoint) for endpoint in self.endpoints]
            results = await asyncio.gather(*tasks)
            
            # 分析结果
            healthy_count = sum(1 for r in results if r["status"] == "healthy")
            unhealthy_count = sum(1 for r in results if r["status"] in ["unhealthy", "error", "timeout"])
            required_services = [e for e in self.endpoints if e.required]
            required_healthy = all(
                any(r["name"] == e.name and r["status"] == "healthy" for r in results)
                for e in required_services
            )
            
            # 记录历史
            results_history.append({
                "timestamp": datetime.now().