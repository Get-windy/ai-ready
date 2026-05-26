#!/usr/bin/env python3
"""
Sprint 27+1 测试环境部署前验证脚本
用于验证目标环境是否满足部署质量门禁要求

作者: QA-Lead (qa-lead)
创建日期: 2026-05-01
"""

import json
import subprocess
import sys
import time
import socket
import psutil
import requests
from datetime import datetime
from typing import Dict, List, Tuple, Optional


class EnvironmentValidator:
    """测试环境验证器"""
    
    def __init__(self, environment: str = "sprint27-plus1"):
        self.environment = environment
        self.results = {
            "validation_time": datetime.now().isoformat(),
            "environment": environment,
            "overall_status": "PASS",
            "total_checks": 0,
            "passed_checks": 0,
            "failed_checks": 0,
            "warnings": 0,
            "check_results": []
        }
    
    def add_result(self, check_name: str, status: str, message: str, details: Dict = None):
        """添加检查结果"""
        self.results["total_checks"] += 1
        
        result = {
            "check_name": check_name,
            "status": status,
            "message": message,
            "timestamp": datetime.now().isoformat()
        }
        
        if details:
            result["details"] = details
        
        if status == "PASS":
            self.results["passed_checks"] += 1
        elif status == "FAIL":
            self.results["failed_checks"] += 1
            self.results["overall_status"] = "FAIL"
        elif status == "WARNING":
            self.results["warnings"] += 1
        
        self.results["check_results"].append(result)
    
    def check_system_resources(self) -> bool:
        """检查系统资源"""
        print("🔍 检查系统资源...")
        
        try:
            # 检查CPU核心数
            cpu_count = psutil.cpu_count(logical=True)
            min_cpu = 2
            if cpu_count >= min_cpu:
                self.add_result(
                    "CPU_资源检查",
                    "PASS",
                    f"CPU核心数满足要求: {cpu_count}核 (最低要求: {min_cpu}核)",
                    {"cpu_count": cpu_count, "min_required": min_cpu}
                )
            else:
                self.add_result(
                    "CPU_资源检查",
                    "FAIL",
                    f"CPU核心数不足: {cpu_count}核 (最低要求: {min_cpu}核)",
                    {"cpu_count": cpu_count, "min_required": min_cpu}
                )
            
            # 检查内存
            memory = psutil.virtual_memory()
            memory_gb = memory.total / (1024**3)
            min_memory_gb = 4
            if memory_gb >= min_memory_gb:
                self.add_result(
                    "内存资源检查",
                    "PASS",
                    f"内存满足要求: {memory_gb:.1f}GB (最低要求: {min_memory_gb}GB)",
                    {"total_memory_gb": memory_gb, "min_required_gb": min_memory_gb}
                )
            else:
                self.add_result(
                    "内存资源检查",
                    "FAIL",
                    f"内存不足: {memory_gb:.1f}GB (最低要求: {min_memory_gb}GB)",
                    {"total_memory_gb": memory_gb, "min_required_gb": min_memory_gb}
                )
            
            # 检查磁盘空间
            disk = psutil.disk_usage('/')
            disk_gb = disk.free / (1024**3)
            min_disk_gb = 20
            if disk_gb >= min_disk_gb:
                self.add_result(
                    "磁盘空间检查",
                    "PASS",
                    f"磁盘空间满足要求: {disk_gb:.1f}GB可用 (最低要求: {min_disk_gb}GB)",
                    {"free_disk_gb": disk_gb, "min_required_gb": min_disk_gb}
                )
            else:
                self.add_result(
                    "磁盘空间检查",
                    "FAIL",
                    f"磁盘空间不足: {disk_gb:.1f}GB可用 (最低要求: {min_disk_gb}GB)",
                    {"free_disk_gb": disk_gb, "min_required_gb": min_disk_gb}
                )
            
            return True
            
        except Exception as e:
            self.add_result(
                "系统资源检查",
                "FAIL",
                f"检查系统资源时出错: {str(e)}",
                {"error": str(e)}
            )
            return False
    
    def check_network_connectivity(self) -> bool:
        """检查网络连通性"""
        print("🔍 检查网络连通性...")
        
        # 需要检查的服务端点
        endpoints = [
            ("PostgreSQL", "localhost", 5432),
            ("Redis", "localhost", 6379),
            ("Kafka", "localhost", 9092),
            ("MinIO", "localhost", 9000),
            ("Prometheus", "localhost", 9090),
            ("Grafana", "localhost", 3000),
        ]
        
        for service_name, host, port in endpoints:
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(5)
                result = sock.connect_ex((host, port))
                sock.close()
                
                if result == 0:
                    self.add_result(
                        f"网络连通性_{service_name}",
                        "PASS",
                        f"{service_name} 服务可达: {host}:{port}"
                    )
                else:
                    self.add_result(
                        f"网络连通性_{service_name}",
                        "WARNING" if service_name in ["Prometheus", "Grafana"] else "FAIL",
                        f"{service_name} 服务不可达: {host}:{port}"
                    )
                
            except Exception as e:
                self.add_result(
                    f"网络连通性_{service_name}",
                    "FAIL",
                    f"检查 {service_name} 连通性时出错: {str(e)}"
                )
        
        return True
    
    def check_port_availability(self, ports: List[int]) -> bool:
        """检查端口可用性"""
        print("🔍 检查端口可用性...")
        
        required_ports = [8080, 8081, 8082, 8083, 9000, 3000, 9090, 9200]
        
        for port in required_ports:
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(2)
                result = sock.connect_ex(('localhost', port))
                sock.close()
                
                if result == 0:
                    self.add_result(
                        f"端口可用性_{port}",
                        "FAIL",
                        f"端口 {port} 已被占用",
                        {"port": port, "status": "occupied"}
                    )
                else:
                    self.add_result(
                        f"端口可用性_{port}",
                        "PASS",
                        f"端口 {port} 可用",
                        {"port": port, "status": "available"}
                    )
                    
            except Exception as e:
                self.add_result(
                    f"端口可用性_{port}",
                    "FAIL",
                    f"检查端口 {port} 时出错: {str(e)}"
                )
        
        return True
    
    def check_docker_availability(self) -> bool:
        """检查Docker可用性"""
        print("🔍 检查Docker可用性...")
        
        try:
            result = subprocess.run(
                ['docker', '--version'],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if result.returncode == 0:
                self.add_result(
                    "Docker可用性检查",
                    "PASS",
                    f"Docker可用: {result.stdout.strip()}",
                    {"docker_version": result.stdout.strip()}
                )
            else:
                self.add_result(
                    "Docker可用性检查",
                    "FAIL",
                    "Docker不可用或未安装",
                    {"error": result.stderr}
                )
            
            # 检查Docker Compose
            result = subprocess.run(
                ['docker-compose', '--version'],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            if result.returncode == 0:
                self.add_result(
                    "Docker_Compose可用性检查",
                    "PASS",
                    f"Docker Compose可用: {result.stdout.strip()}",
                    {"docker_compose_version": result.stdout.strip()}
                )
            else:
                self.add_result(
                    "Docker_Compose可用性检查",
                    "FAIL",
                    "Docker Compose不可用或未安装",
                    {"error": result.stderr}
                )
            
            return True
            
        except subprocess.TimeoutExpired:
            self.add_result(
                "Docker可用性检查",
                "FAIL",
                "检查Docker可用性超时"
            )
            return False
        except Exception as e:
            self.add_result(
                "Docker可用性检查",
                "FAIL",
                f"检查Docker可用性时出错: {str(e)}"
            )
            return False
    
    def check_dependency_services(self) -> bool:
        """检查依赖服务"""
        print("🔍 检查依赖服务...")
        
        services = [
            {
                "name": "PostgreSQL",
                "check_command": ["pg_isready", "-h", "localhost", "-p", "5432"],
                "timeout": 10
            },
            {
                "name": "Redis",
                "check_command": ["redis-cli", "ping"],
                "expected_output": "PONG",
                "timeout": 5
            },
            {
                "name": "MinIO",
                "check_url": "http://localhost:9000/minio/health/live",
                "timeout": 10
            }
        ]
        
        for service in services:
            service_name = service["name"]
            
            try:
                if "check_command" in service:
                    result = subprocess.run(
                        service["check_command"],
                        capture_output=True,
                        text=True,
                        timeout=service.get("timeout", 10)
                    )
                    
                    if result.returncode == 0:
                        if "expected_output" in service:
                            if service["expected_output"] in result.stdout:
                                self.add_result(
                                    f"依赖服务_{service_name}",
                                    "PASS",
                                    f"{service_name} 服务正常"
                                )
                            else:
                                self.add_result(
                                    f"依赖服务_{service_name}",
                                    "FAIL",
                                    f"{service_name} 服务响应异常"
                                )
                        else:
                            self.add_result(
                                f"依赖服务_{service_name}",
                                "PASS",
                                f"{service_name} 服务正常"
                            )
                    else:
                        self.add_result(
                            f"依赖服务_{service_name}",
                            "FAIL",
                            f"{service_name} 服务不可用"
                        )
                
                elif "check_url" in service:
                    response = requests.get(
                        service["check_url"],
                        timeout=service.get("timeout", 10)
                    )
                    
                    if response.status_code == 200:
                        self.add_result(
                            f"依赖服务_{service_name}",
                            "PASS",
                            f"{service_name} 服务正常"
                        )
                    else:
                        self.add_result(
                            f"依赖服务_{service_name}",
                            "FAIL",
                            f"{service_name} 服务异常，状态码: {response.status_code}"
                        )
                
            except requests.exceptions.RequestException as e:
                self.add_result(
                    f"依赖服务_{service_name}",
                    "FAIL",
                    f"{service_name} 服务连接失败: {str(e)}"
                )
            except subprocess.TimeoutExpired:
                self.add_result(
                    f"依赖服务_{service_name}",
                    "FAIL",
                    f"检查 {service_name} 服务超时"
                )
            except Exception as e:
                self.add_result(
                    f"依赖服务_{service_name}",
                    "FAIL",
                    f"检查 {service_name} 服务时出错: {str(e)}"
                )
        
        return True
    
    def generate_report(self) -> Dict:
        """生成验证报告"""
        report = {
            "summary": {
                "environment": self.environment,
                "validation_time": self.results["validation_time"],
                "overall_status": self.results["overall_status"],
                "total_checks": self.results["total_checks"],
                "passed_checks": self.results["passed_checks"],
                "failed_checks": self.results["failed_checks"],
                "warnings": self.results["warnings"],
                "pass_rate": round((self.results["passed_checks"] / self.results["total_checks"]) * 100, 2)
                if self.results["total_checks"] > 0 else 0
            },
            "detailed_results": self.results["check_results"],
            "recommendations": []
        }
        
        # 生成建议
        if self.results["failed_checks"] > 0:
            report["recommendations"].append({
                "priority": "HIGH",
                "action": "修复失败的检查项后再继续部署",
                "failed_checks": self.results["failed_checks"]
            })
        
        if self.results["warnings"] > 0:
            report["recommendations"].append({
                "priority": "MEDIUM",
                "action": "建议修复警告项以提升环境质量",
                "warning_checks": self.results["warnings"]
            })
        
        return report
    
    def print_summary(self):
        """打印验证摘要"""
        print("\n" + "="*60)
        print("🏁 环境验证结果摘要")
        print("="*60)
        
        print(f"📅 验证时间: {self.results['validation_time']}")
        print(f"🌍 目标环境: {self.environment}")
        print(f"📊 总体状态: {self.results['overall_status']}")
        print(f"✅ 通过检查: {self.results['passed_checks']}/{self.results['total_checks']}")
        print(f"❌ 失败检查: {self.results['failed_checks']}")
        print(f"⚠️  警告检查: {self.results['warnings']}")
        
        if self.results['total_checks'] > 0:
            pass_rate = (self.results['passed_checks'] / self.results['total_checks']) * 100
            print(f"📈 通过率: {pass_rate:.1f}%")
        
        print("\n" + "="*60)
        
        # 打印失败项
        if self.results["failed_checks"] > 0:
            print("❌ 失败检查项:")
            for result in self.results["check_results"]:
                if result["status"] == "FAIL":
                    print(f"  • {result['check_name']}: {result['message']}")
        
        # 打印警告项
        if self.results["warnings"] > 0:
            print("\n⚠️  警告检查项:")
            for result in self.results["check_results"]:
                if result["status"] == "WARNING":
                    print(f"  • {result['check_name']}: {result['message']}")
    
    def run_all_checks(self) -> bool:
        """运行所有检查"""
        print(f"🚀 开始验证 {self.environment} 环境...")
        print("-"*60)
        
        # 执行所有检查
        checks = [
            ("系统资源检查", self.check_system_resources),
            ("网络连通性检查", self.check_network_connectivity),
            ("端口可用性检查", lambda: self.check_port_availability([])),
            ("Docker可用性检查", self.check_docker_availability),
            ("依赖服务检查", self.check_dependency_services),
        ]
        
        for check_name, check_func in checks:
            print(f"正在执行: {check_name}")
            try:
                check_func()
            except Exception as e:
                self.add_result(
                    check_name,
                    "FAIL",
                    f"执行检查时出错: {str(e)}"
                )
        
        return self.results["overall_status"] == "PASS"


def main():
    """主函数"""
    print("="*60)
    print("🏗️  Sprint 27+1 测试环境部署前验证工具")
    print("="*60)
    
    # 解析命令行参数
    environment = "sprint27-plus1"
    output_file = None
    
    if len(sys.argv) > 1:
        environment = sys.argv[1]
    
    if len(sys.argv) > 2:
        output_file = sys.argv[2]
    
    # 创建验证器
    validator = EnvironmentValidator(environment)
    
    # 运行所有检查
    success = validator.run_all_checks()
    
    # 打印摘要
    validator.print_summary()
    
    # 生成报告
    report = validator.generate_report()
    
    # 保存报告到文件
    if output_file:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        print(f"\n📄 报告已保存到: {output_file}")
    
    # 根据结果退出
    if success:
        print("\n🎉 环境验证通过，可以继续部署！")
        return 0
    else:
        print("\n❌ 环境验证失败，请修复问题后再试！")
        return 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\n\n⚠️  用户中断执行")
        sys.exit(130)
    except Exception as e:
        print(f"\n❌ 程序执行出错: {str(e)}")
        sys.exit(1)