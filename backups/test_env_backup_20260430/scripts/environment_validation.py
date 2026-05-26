#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1 测试环境验证脚本
用于验证测试环境是否就绪
作者: test-agent-2
日期: 2026-04-29
"""

import asyncio
import aiohttp
import socket
import time
import json
import logging
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class ServiceStatus(Enum):
    """服务状态"""
    HEALTHY = "healthy"
    DEGRADED = "degraded"
    UNHEALTHY = "unhealthy"
    UNKNOWN = "unknown"


@dataclass
class ServiceCheck:
    """服务检查配置"""
    name: str
    host: str
    port: int
    check_type: str = "tcp"  # tcp, http, https
    check_path: str = "/"
    timeout: int = 5
    expected_status: int = 200
    required: bool = True  # 是否为必需服务


@dataclass
class ServiceResult:
    """服务检查结果"""
    name: str
    status: ServiceStatus
    response_time: float
    error_message: str = ""
    details: Dict = field(default_factory=dict)


class EnvironmentValidator:
    """环境验证器"""
    
    def __init__(self):
        self.services = [
            # API服务
            ServiceCheck("API Gateway", "localhost", 8080, "http", "/", 10, 200, True),
            
            # 微服务
            ServiceCheck("User Service 1", "localhost", 8081, "http", "/actuator/health", 5, 200, True),
            ServiceCheck("User Service 2", "localhost", 8082, "http", "/actuator/health", 5, 200, False),
            ServiceCheck("Order Service 1", "localhost", 8083, "http", "/actuator/health", 5, 200, True),
            ServiceCheck("Order Service 2", "localhost", 8084, "http", "/actuator/health", 5, 200, False),
            ServiceCheck("Inventory Service", "localhost", 8085, "http", "/actuator/health", 5, 200, True),
            ServiceCheck("Monitoring Service", "localhost", 8086, "http", "/actuator/health", 5, 200, True),
            
            # 数据库服务
            ServiceCheck("PostgreSQL", "localhost", 5432, "tcp", timeout=5, required=True),
            ServiceCheck("Redis", "localhost", 6379, "tcp", timeout=5, required=True),
            
            # 消息队列
            ServiceCheck("Kafka", "localhost", 9092, "tcp", timeout=5, required=False),
            ServiceCheck("ZooKeeper", "localhost", 2181, "tcp", timeout=5, required=False),
            
            # 基础设施
            ServiceCheck("Nacos", "localhost", 8848, "http", "/nacos/v1/ns/operator/health", 5, 200, False),
            ServiceCheck("Prometheus", "localhost", 9090, "http", "/-/healthy", 5, 200, False),
            ServiceCheck("Grafana", "localhost", 3000, "http", "/api/health", 5, 200, False),
            ServiceCheck("Jaeger", "localhost", 16686, "http", "/api/services", 5, 200, False),
        ]
        self.results: List[ServiceResult] = []
    
    async def check_tcp_port(self, host: str, port: int, timeout: int = 5) -> Tuple[bool, float, str]:
        """检查TCP端口"""
        start_time = time.time()
        try:
            reader, writer = await asyncio.wait_for(
                asyncio.open_connection(host, port),
                timeout=timeout
            )
            writer.close()
            await writer.wait_closed()
            response_time = (time.time() - start_time) * 1000
            return True, response_time, ""
        except asyncio.TimeoutError:
            return False, timeout * 1000, "Connection timeout"
        except Exception as e:
            return False, (time.time() - start_time) * 1000, str(e)
    
    async def check_http_endpoint(self, host: str, port: int, path: str, 
                                  timeout: int = 5, expected_status: int = 200) -> Tuple[bool, float, str, Dict]:
        """检查HTTP端点"""
        url = f"http://{host}:{port}{path}"
        start_time = time.time()
        
        try:
            async with aiohttp.ClientSession() as session:
                async with asyncio.timeout(timeout):
                    async with session.get(url) as response:
                        response_time = (time.time() - start_time) * 1000
                        
                        if response.status == expected_status:
                            try:
                                data = await response.json()
                                return True, response_time, "", data
                            except:
                                return True, response_time, "", {"status": response.status}
                        else:
                            return False, response_time, f"Unexpected status: {response.status}", {"status": response.status}
        except asyncio.TimeoutError:
            return False, timeout * 1000, "HTTP request timeout", {}
        except Exception as e:
            return False, (time.time() - start_time) * 1000, str(e), {}
    
    async def check_service(self, service: ServiceCheck) -> ServiceResult:
        """检查单个服务"""
        logger.info(f"检查服务: {service.name} ({service.host}:{service.port})")
        
        if service.check_type == "tcp":
            success, response_time, error = await self.check_tcp_port(
                service.host, service.port, service.timeout
            )
            
            if success:
                return ServiceResult(
                    name=service.name,
                    status=ServiceStatus.HEALTHY,
                    response_time=response_time,
                    details={"port": service.port}
                )
            else:
                return ServiceResult(
                    name=service.name,
                    status=ServiceStatus.UNHEALTHY,
                    response_time=response_time,
                    error_message=error,
                    details={"port": service.port}
                )
        
        elif service.check_type in ["http", "https"]:
            success, response_time, error, data = await self.check_http_endpoint(
                service.host, service.port, service.check_path,
                service.timeout, service.expected_status
            )
            
            if success:
                return ServiceResult(
                    name=service.name,
                    status=ServiceStatus.HEALTHY,
                    response_time=response_time,
                    details={"status": data.get("status", "OK"), "url": f"http://{service.host}:{service.port}{service.check_path}"}
                )
            else:
                return ServiceResult(
                    name=service.name,
                    status=ServiceStatus.UNHEALTHY,
                    response_time=response_time,
                    error_message=error,
                    details={"url": f"http://{service.host}:{service.port}{service.check_path}"}
                )
        
        else:
            return ServiceResult(
                name=service.name,
                status=ServiceStatus.UNKNOWN,
                response_time=0,
                error_message=f"Unknown check type: {service.check_type}"
            )
    
    async def validate_environment(self) -> Dict:
        """验证整个环境"""
        logger.info("开始测试环境验证...")
        
        validation_results = {
            "timestamp": datetime.now().isoformat(),
            "environment": "test",
            "services": {},
            "summary": {
                "total": 0,
                "healthy": 0,
                "degraded": 0,
                "unhealthy": 0,
                "unknown": 0,
                "required_healthy": True
            }
        }
        
        # 并发检查所有服务
        tasks = [self.check_service(service) for service in self.services]
        self.results = await asyncio.gather(*tasks)
        
        # 处理结果
        required_services_healthy = True
        
        for result in self.results:
            validation_results["services"][result.name] = {
                "status": result.status.value,
                "response_time_ms": round(result.response_time, 2),
                "error": result.error_message,
                "details": result.details
            }
            
            validation_results["summary"]["total"] += 1
            
            if result.status == ServiceStatus.HEALTHY:
                validation_results["summary"]["healthy"] += 1
            elif result.status == ServiceStatus.DEGRADED:
                validation_results["summary"]["degraded"] += 1
            elif result.status == ServiceStatus.UNHEALTHY:
                validation_results["summary"]["unhealthy"] += 1
                # 检查是否是必需服务
                service_config = next((s for s in self.services if s.name == result.name), None)
                if service_config and service_config.required:
                    required_services_healthy = False
            else:
                validation_results["summary"]["unknown"] += 1
        
        validation_results["summary"]["required_healthy"] = required_services_healthy
        
        # 生成报告
        self._generate_report(validation_results)
        
        return validation_results
    
    def _generate_report(self, results: Dict):
        """生成验证报告"""
        report_file = f"environment_validation_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        with open(report_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        
        logger.info(f"环境验证报告已保存到: {report_file}")
        
        # 生成文本报告
        summary_file = f"environment_validation_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"
        with open(summary_file, 'w', encoding='utf-8') as f:
            f.write("=" * 80 + "\n")
            f.write("Sprint 27+1 测试环境验证报告\n")
            f.write("=" * 80 + "\n\n")
            
            f.write(f"验证时间: {results['timestamp']}\n")
            f.write(f"环境: {results['environment']}\n\n")
            
            f.write("服务状态:\n")
            f.write("-" * 80 + "\n")
            f.write(f"{'服务名称':<30} {'状态':<12} {'响应时间':<12} {'错误信息':<30}\n")
            f.write("-" * 80 + "\n")
            
            for service_name, service_data in results['services'].items():
                status = service_data['status']
                response_time = f"{service_data['response_time_ms']:.2f}ms"
                error = service_data['error'][:30] if service_data['error'] else ""
                
                status_icon = {
                    "healthy": "✅",
                    "degraded": "⚠️",
                    "unhealthy": "❌",
                    "unknown": "❓"
                }.get(status, "❓")
                
                f.write(f"{service_name:<30} {status_icon} {status:<10} {response_time:<12} {error:<30}\n")
            
            f.write("\n" + "=" * 80 + "\n")
            f.write("验证摘要:\n")
            f.write("-" * 40 + "\n")
            summary = results['summary']
            f.write(f"总服务数: {summary['total']}\n")
            f.write(f"健康服务: {summary['healthy']}\n")
            f.write(f"降级服务: {summary['degraded']}\n")
            f.write(f"异常服务: {summary['unhealthy']}\n")
            f.write(f"未知状态: {summary['unknown']}\n")
            f.write(f"\n必需服务状态: {'✅ 全部健康' if summary['required_healthy'] else '❌ 存在异常'}\n")
            
            f.write("\n结论:\n")
            f.write("-" * 40 + "\n")
            if summary['required_healthy']:
                f.write("✅ 测试环境验证通过，可以开始测试！\n")
            else:
                f.write("❌ 测试环境验证未通过，请修复以下必需服务:\n")
                for service_name, service_data in results['services'].items():
                    if service_data['status'] == 'unhealthy':
                        service_config = next((s for s in self.services if s.name == service_name), None)
                        if service_config and service_config.required:
                            f.write(f"  - {service_name}: {service_data['error']}\n")
        
        logger.info(f"环境验证摘要已保存到: {summary_file}")


async def main():
    """主函数"""
    print("=" * 80)
    print("Sprint 27+1 测试环境验证")
    print("=" * 80)
    
    validator = EnvironmentValidator()
    results = await validator.validate_environment()
    
    print("\n验证完成！")
    print(f"总服务数: {results['summary']['total']}")
    print(f"健康服务: {results['summary']['healthy']}")
    print(f"异常服务: {results['summary']['unhealthy']}")
    print(f"\n必需服务状态: {'✅ 通过' if results['summary']['required_healthy'] else '❌ 未通过'}")
    
    if not results['summary']['required_healthy']:
        print("\n❌ 以下必需服务异常:")
        for service_name, service_data in results['services'].items():
            if service_data['status'] == 'unhealthy':
                service_config = next((s for s in validator.services if s.name == service_name), None)
                if service_config and service_config.required:
                    print(f"  - {service_name}: {service_data['error']}")
    
    print(f"\n详细报告已生成:")
    print(f"- JSON报告: environment_validation_report_*.json")
    print(f"- 文本摘要: environment_validation_summary_*.txt")


if __name__ == "__main__":
    asyncio.run(main())
