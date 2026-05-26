#!/usr/bin/env python3
"""
API接口监控脚本 - 简化版本
版本: 1.0
创建日期: 2026-04-28
用途: 监控测试环境API接口可用性和性能
"""

import asyncio
import json
import logging
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any
import aiohttp
import yaml

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('api_monitor.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class APIMonitor:
    """API监控器"""
    
    def __init__(self, config_file: str = "config.yaml"):
        """初始化监控器"""
        self.config_file = config_file
        self.config = self.load_config()
        self.results_dir = Path("results")
        self.results_dir.mkdir(exist_ok=True)
        
    def load_config(self) -> Dict[str, Any]:
        """加载配置文件"""
        config = {
            "services": {
                "user-service": {
                    "name": "用户服务",
                    "host": "user-service",
                    "port": 8085,
                    "endpoints": [
                        {"name": "健康检查", "path": "/actuator/health", "method": "GET", "expected_status": 200},
                        {"name": "用户登录", "path": "/api/v1/auth/login", "method": "POST", "expected_status": 200}
                    ]
                },
                "order-service": {
                    "name": "订单服务",
                    "host": "order-service",
                    "port": 8086,
                    "endpoints": [
                        {"name": "健康检查", "path": "/actuator/health", "method": "GET", "expected_status": 200},
                        {"name": "订单列表", "path": "/api/v1/orders", "method": "GET", "expected_status": 200}
                    ]
                },
                "inventory-service": {
                    "name": "库存服务",
                    "host": "inventory-service",
                    "port": 8082,
                    "endpoints": [
                        {"name": "健康检查", "path": "/actuator/health", "method": "GET", "expected_status": 200},
                        {"name": "库存查询", "path": "/api/v1/inventory/1", "method": "GET", "expected_status": 200}
                    ]
                }
            },
            "monitoring": {
                "check_interval": 30,  # 检查间隔(秒)
                "timeout": 10,         # 请求超时(秒)
                "retry_times": 2       # 重试次数
            }
        }
        
        # 如果存在配置文件，则加载
        config_path = Path(self.config_file)
        if config_path.exists():
            try:
                with open(config_path, 'r') as f:
                    user_config = yaml.safe_load(f)
                    config.update(user_config)
                logger.info(f"配置文件加载成功: {self.config_file}")
            except Exception as e:
                logger.warning(f"配置文件加载失败，使用默认配置: {e}")
        
        return config
    
    async def check_service(self, service_id: str, service_config: Dict[str, Any]) -> Dict[str, Any]:
        """检查单个服务"""
        results = {
            "service_id": service_id,
            "service_name": service_config["name"],
            "timestamp": datetime.now().isoformat(),
            "endpoints": [],
            "overall_status": "healthy"
        }
        
        for endpoint in service_config.get("endpoints", []):
            endpoint_result = await self.check_endpoint(service_config, endpoint)
            results["endpoints"].append(endpoint_result)
            
            # 如果任何端点失败，则服务状态为不健康
            if endpoint_result["status"] != "success":
                results["overall_status"] = "unhealthy"
        
        return results
    
    async def check_endpoint(self, service_config: Dict[str, Any], endpoint: Dict[str, Any]) -> Dict[str, Any]:
        """检查单个端点"""
        url = f"http://{service_config['host']}:{service_config['port']}{endpoint['path']}"
        
        result = {
            "name": endpoint["name"],
            "url": url,
            "method": endpoint["method"],
            "expected_status": endpoint.get("expected_status", 200),
            "status": "unknown",
            "http_status": 0,
            "response_time": 0,
            "error": None,
            "timestamp": datetime.now().isoformat()
        }
        
        timeout = aiohttp.ClientTimeout(total=self.config["monitoring"]["timeout"])
        
        try:
            start_time = time.time()
            
            async with aiohttp.ClientSession(timeout=timeout) as session:
                if endpoint["method"].upper() == "GET":
                    async with session.get(url) as response:
                        result["http_status"] = response.status
                        result["response_time"] = time.time() - start_time
                        
                        if response.status == result["expected_status"]:
                            result["status"] = "success"
                        else:
                            result["status"] = "failed"
                            result["error"] = f"HTTP状态码不匹配: {response.status}"
                            
                elif endpoint["method"].upper() == "POST":
                    # 对于POST请求，可以添加请求体
                    async with session.post(url, json={}) as response:
                        result["http_status"] = response.status
                        result["response_time"] = time.time() - start_time
                        
                        if response.status == result["expected_status"]:
                            result["status"] = "success"
                        else:
                            result["status"] = "failed"
                            result["error"] = f"HTTP状态码不匹配: {response.status}"
                else:
                    result["status"] = "failed"
                    result["error"] = f"不支持的HTTP方法: {endpoint['method']}"
                    
        except asyncio.TimeoutError:
            result["status"] = "timeout"
            result["error"] = f"请求超时 ({self.config['monitoring']['timeout']}秒)"
            result["response_time"] = self.config["monitoring"]["timeout"]
            
        except Exception as e:
            result["status"] = "error"
            result["error"] = str(e)
        
        logger.info(f"端点检查: {service_config['name']}/{endpoint['name']} - {result['status']}")
        return result
    
    async def run_checks(self) -> Dict[str, Any]:
        """执行所有检查"""
        logger.info("开始执行API检查...")
        
        tasks = []
        for service_id, service_config in self.config["services"].items():
            task = self.check_service(service_id, service_config)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks)
        
        summary = self.generate_summary(results)
        
        # 保存结果
        self.save_results(results, summary)
        
        return {"results": results, "summary": summary}
    
    def generate_summary(self, results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """生成检查摘要"""
        total_endpoints = 0
        successful_endpoints = 0
        failed_endpoints = 0
        total_response_time = 0
        healthy_services = 0
        
        for service_result in results:
            if service_result["overall_status"] == "healthy":
                healthy_services += 1
            
            for endpoint in service_result["endpoints"]:
                total_endpoints += 1
                if endpoint["status"] == "success":
                    successful_endpoints += 1
                    total_response_time += endpoint.get("response_time", 0)
                else:
                    failed_endpoints += 1
        
        avg_response_time = total_response_time / successful_endpoints if successful_endpoints > 0 else 0
        success_rate = (successful_endpoints / total_endpoints * 100) if total_endpoints > 0 else 0
        health_rate = (healthy_services / len(results) * 100) if results else 0
        
        return {
            "timestamp": datetime.now().isoformat(),
            "total_services": len(results),
            "healthy_services": healthy_services,
            "unhealthy_services": len(results) - healthy_services,
            "total_endpoints": total_endpoints,
            "successful_endpoints": successful_endpoints,
            "failed_endpoints": failed_endpoints,
            "success_rate": round(success_rate, 2),
            "health_rate": round(health_rate, 2),
            "avg_response_time": round(avg_response_time, 3)
        }
    
    def save_results(self, results: List[Dict[str, Any]], summary: Dict[str, Any]):
        """保存检查结果"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 保存详细结果
        results_file = self.results_dir / f"api_results_{timestamp}.json"
        with open(results_file, 'w') as f:
            json.dump({
                "timestamp": datetime.now().isoformat(),
                "results": results,
                "summary": summary
            }, f, indent=2, default=str)
        
        # 保存摘要到独立文件（便于查看）
        summary_file = self.results_dir / f"api_summary_{timestamp}.json"
        with open(summary_file, 'w') as f:
            json.dump(summary, f, indent=2, default=str)
        
        logger.info(f"结果已保存: {results_file}, {summary_file}")
        
        # 生成简单的控制台报告
        self.print_report(summary)
    
    def print_report(self, summary: Dict[str, Any]):
        """打印控制台报告"""
        print("\n" + "="*60)
        print("API监控检查报告")
        print("="*60)
        print(f"检查时间: {summary['timestamp']}")
        print(f"服务总数: {summary['total_services']}")
        print(f"健康服务: {summary['healthy_services']} ({summary['health_rate']}%)")
        print(f"端点总数: {summary['total_endpoints']}")
        print(f"成功端点: {summary['successful_endpoints']} ({summary['success_rate']}%)")
        print(f"失败端点: {summary['failed_endpoints']}")
        print(f"平均响应时间: {summary['avg_response_time']}秒")
        print("="*60)
    
    async def run_continuous(self):
        """持续运行监控"""
        logger.info("启动持续监控模式")
        
        try:
            while True:
                await self.run_checks()
                
                # 等待下一次检查
                interval = self.config["monitoring"]["check_interval"]
                logger.info(f"等待 {interval} 秒后进行下一次检查...")
                await asyncio.sleep(interval)
                
        except KeyboardInterrupt:
            logger.info("收到停止信号，监控停止")
        except Exception as e:
            logger.error(f"监控运行异常: {e}")

async def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description='API接口监控脚本')
    parser.add_argument('--config', '-c', default='config.yaml', help='配置文件路径')
    parser.add_argument('--once', '-o', action='store_true', help='只运行一次检查')
    parser.add_argument('--continuous', '-C', action='store_true', help='持续运行监控')
    
    args = parser.parse_args()
    
    monitor = APIMonitor(args.config)
    
    if args.once:
        # 单次运行模式
        await monitor.run_checks()
    elif args.continuous:
        # 持续运行模式
        await monitor.run_continuous()
    else:
        # 默认单次运行
        await monitor.run_checks()

if __name__ == "__main__":
    asyncio.run(main())