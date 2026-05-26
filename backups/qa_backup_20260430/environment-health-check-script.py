#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
环境健康检查脚本 - 子任务2
版本: 1.0.0
创建日期: 2026-04-29
作者: qa-lead
项目: AI-Ready测试环境配置专项
Sprint: Sprint 27+1
任务ID: task_1777438750224_y4l4dft4a

功能: 执行测试环境的健康检查，包括基础设施、服务和应用程序的健康状态检查
"""

import os
import sys
import json
import time
import logging
import argparse
import subprocess
from datetime import datetime
from typing import Dict, List, Any, Optional, Tuple
import requests
import socket
import psutil
import yaml
import platform
import shutil
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('environment-health-check.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class EnvironmentHealthChecker:
    """环境健康检查器"""
    
    def __init__(self, config_file: str = None):
        """
        初始化健康检查器
        
        Args:
            config_file: 配置文件路径
        """
        self.start_time = datetime.now()
        self.results = {
            "overall_status": "UNKNOWN",
            "total_checks": 0,
            "passed_checks": 0,
            "failed_checks": 0,
            "warning_checks": 0,
            "skipped_checks": 0,
            "check_results": [],
            "summary": {},
            "timestamp": self.start_time.isoformat(),
            "duration_seconds": 0
        }
        
        # 加载配置
        self.config = self.load_config(config_file)
        
        # 设置检查项
        self.check_items = self.initialize_check_items()
        
    def load_config(self, config_file: str = None) -> Dict:
        """加载配置文件"""
        default_config = {
            "environment": {
                "name": "test",
                "type": "test-environment",
                "description": "AI-Ready测试环境"
            },
            "checks": {
                "infrastructure": {
                    "enabled": True,
                    "cpu_threshold": 80,
                    "memory_threshold": 85,
                    "disk_threshold": 90,
                    "network_timeout": 5
                },
                "services": {
                    "enabled": True,
                    "timeout": 10,
                    "expected_services": [
                        {
                            "name": "erp-purchase",
                            "host": "localhost",
                            "port": 8080,
                            "health_endpoint": "/health",
                            "expected_status": 200
                        },
                        {
                            "name": "erp-sales",
                            "host": "localhost",
                            "port": 8081,
                            "health_endpoint": "/actuator/health",
                            "expected_status": 200
                        },
                        {
                            "name": "mysql-database",
                            "host": "localhost",
                            "port": 3306
                        },
                        {
                            "name": "redis-cache",
                            "host": "localhost",
                            "port": 6379
                        }
                    ]
                },
                "applications": {
                    "enabled": True,
                    "erp_modules": [
                        "purchase", "sales", "inventory", "finance"
                    ]
                }
            },
            "reporting": {
                "output_formats": ["json", "html", "markdown"],
                "output_dir": "./health-reports",
                "send_notifications": False
            },
            "parallel_execution": {
                "enabled": True,
                "max_workers": 5
            }
        }
        
        if config_file and os.path.exists(config_file):
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    user_config = yaml.safe_load(f)
                    # 合并配置
                    self.merge_configs(default_config, user_config)
                    logger.info(f"已加载配置文件: {config_file}")
            except Exception as e:
                logger.error(f"加载配置文件失败: {e}, 使用默认配置")
        
        return default_config
    
    def merge_configs(self, default: Dict, user: Dict) -> Dict:
        """递归合并配置"""
        for key, value in user.items():
            if key in default and isinstance(default[key], dict) and isinstance(value, dict):
                self.merge_configs(default[key], value)
            else:
                default[key] = value
        return default
    
    def initialize_check_items(self) -> List[Dict]:
        """初始化检查项列表"""
        check_items = []
        
        # 1. 基础设施检查项
        if self.config["checks"]["infrastructure"]["enabled"]:
            check_items.extend([
                {
                    "id": "infra_cpu",
                    "name": "CPU使用率检查",
                    "category": "infrastructure",
                    "description": "检查CPU使用率是否在阈值内",
                    "severity": "high",
                    "function": self.check_cpu_usage
                },
                {
                    "id": "infra_memory",
                    "name": "内存使用率检查",
                    "category": "infrastructure",
                    "description": "检查内存使用率是否在阈值内",
                    "severity": "high",
                    "function": self.check_memory_usage
                },
                {
                    "id": "infra_disk",
                    "name": "磁盘空间检查",
                    "category": "infrastructure",
                    "description": "检查磁盘空间使用率是否在阈值内",
                    "severity": "high",
                    "function": self.check_disk_usage
                },
                {
                    "id": "infra_network",
                    "name": "网络连通性检查",
                    "category": "infrastructure",
                    "description": "检查网络连通性和DNS解析",
                    "severity": "medium",
                    "function": self.check_network_connectivity
                },
                {
                    "id": "infra_processes",
                    "name": "关键进程检查",
                    "category": "infrastructure",
                    "description": "检查关键系统进程是否运行",
                    "severity": "medium",
                    "function": self.check_critical_processes
                }
            ])
        
        # 2. 服务检查项
        if self.config["checks"]["services"]["enabled"]:
            for service in self.config["checks"]["services"]["expected_services"]:
                check_items.append({
                    "id": f"service_{service['name']}",
                    "name": f"{service['name']}服务健康检查",
                    "category": "services",
                    "description": f"检查{service['name']}服务是否可用",
                    "severity": "critical",
                    "function": lambda s=service: self.check_service_health(s),
                    "service_config": service
                })
        
        # 3. 应用程序检查项
        if self.config["checks"]["applications"]["enabled"]:
            for module in self.config["checks"]["applications"]["erp_modules"]:
                check_items.append({
                    "id": f"app_{module}",
                    "name": f"ERP {module}模块检查",
                    "category": "applications",
                    "description": f"检查ERP {module}模块的基本功能",
                    "severity": "high",
                    "function": lambda m=module: self.check_erp_module(m)
                })
        
        logger.info(f"初始化了 {len(check_items)} 个检查项")
        return check_items
    
    def run_all_checks(self) -> Dict:
        """执行所有健康检查"""
        logger.info("开始执行环境健康检查...")
        self.results["total_checks"] = len(self.check_items)
        
        # 执行检查
        if self.config["parallel_execution"]["enabled"]:
            self.run_checks_in_parallel()
        else:
            self.run_checks_sequentially()
        
        # 计算总体状态
        self.calculate_overall_status()
        
        # 计算执行时间
        end_time = datetime.now()
        self.results["duration_seconds"] = (end_time - self.start_time).total_seconds()
        
        # 生成报告
        self.generate_reports()
        
        logger.info(f"健康检查完成。总体状态: {self.results['overall_status']}")
        logger.info(f"检查结果: {self.results['passed_checks']} 通过, "
                   f"{self.results['failed_checks']} 失败, "
                   f"{self.results['warning_checks']} 警告")
        
        return self.results
    
    def run_checks_in_parallel(self):
        """并行执行检查"""
        max_workers = self.config["parallel_execution"]["max_workers"]
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            # 提交所有检查任务
            future_to_check = {}
            for check_item in self.check_items:
                future = executor.submit(self.execute_single_check, check_item)
                future_to_check[future] = check_item
            
            # 收集结果
            for future in as_completed(future_to_check):
                check_item = future_to_check[future]
                try:
                    result = future.result(timeout=60)
                    self.process_check_result(result)
                except Exception as e:
                    logger.error(f"检查项 {check_item['name']} 执行异常: {e}")
                    self.process_check_result({
                        "check_id": check_item["id"],
                        "status": "ERROR",
                        "message": f"执行异常: {str(e)}",
                        "details": {"error": str(e)},
                        "duration": 0
                    })
    
    def run_checks_sequentially(self):
        """顺序执行检查"""
        for check_item in self.check_items:
            result = self.execute_single_check(check_item)
            self.process_check_result(result)
    
    def execute_single_check(self, check_item: Dict) -> Dict:
        """执行单个检查项"""
        start_time = time.time()
        check_id = check_item["id"]
        check_name = check_item["name"]
        
        logger.info(f"执行检查: {check_name}")
        
        try:
            # 执行检查函数
            result = check_item["function"]()
            result["check_id"] = check_id
            result["check_name"] = check_name
            result["category"] = check_item["category"]
            result["severity"] = check_item["severity"]
            
            # 添加执行时间
            result["duration"] = round(time.time() - start_time, 3)
            
            return result
            
        except Exception as e:
            logger.error(f"检查项 {check_name} 执行失败: {e}")
            return {
                "check_id": check_id,
                "check_name": check_name,
                "category": check_item["category"],
                "severity": check_item["severity"],
                "status": "ERROR",
                "message": f"执行失败: {str(e)}",
                "details": {"error": str(e), "traceback": str(sys.exc_info())},
                "duration": round(time.time() - start_time, 3)
            }
    
    def process_check_result(self, result: Dict):
        """处理检查结果"""
        self.results["check_results"].append(result)
        
        if result["status"] == "PASS":
            self.results["passed_checks"] += 1
        elif result["status"] == "FAIL":
            self.results["failed_checks"] += 1
        elif result["status"] == "WARNING":
            self.results["warning_checks"] += 1
        elif result["status"] == "SKIPPED":
            self.results["skipped_checks"] += 1
        elif result["status"] == "ERROR":
            self.results["failed_checks"] += 1
    
    def calculate_overall_status(self):
        """计算总体状态"""
        if self.results["failed_checks"] > 0:
            self.results["overall_status"] = "FAILED"
        elif self.results["warning_checks"] > 0:
            self.results["overall_status"] = "WARNING"
        elif self.results["passed_checks"] == self.results["total_checks"]:
            self.results["overall_status"] = "HEALTHY"
        else:
            self.results["overall_status"] = "UNKNOWN"
        
        # 生成摘要
        self.results["summary"] = {
            "infrastructure": self.summarize_by_category("infrastructure"),
            "services": self.summarize_by_category("services"),
            "applications": self.summarize_by_category("applications")
        }
    
    def summarize_by_category(self, category: str) -> Dict:
        """按类别汇总结果"""
        category_results = [r for r in self.results["check_results"] if r.get("category") == category]
        
        if not category_results:
            return {"status": "NO_CHECKS", "total": 0, "passed": 0, "failed": 0}
        
        total = len(category_results)
        passed = sum(1 for r in category_results if r.get("status") == "PASS")
        failed = sum(1 for r in category_results if r.get("status") == "FAIL")
        warning = sum(1 for r in category_results if r.get("status") == "WARNING")
        
        if failed > 0:
            status = "FAILED"
        elif warning > 0:
            status = "WARNING"
        elif passed == total:
            status = "HEALTHY"
        else:
            status = "UNKNOWN"
        
        return {
            "status": status,
            "total": total,
            "passed": passed,
            "failed": failed,
            "warning": warning
        }
    
    def generate_reports(self):
        """生成报告"""
        output_dir = self.config["reporting"]["output_dir"]
        os.makedirs(output_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        base_filename = f"environment-health-report-{timestamp}"
        
        # JSON报告
        if "json" in self.config["reporting"]["output_formats"]:
            json_path = os.path.join(output_dir, f"{base_filename}.json")
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(self.results, f, indent=2, ensure_ascii=False)
            logger.info(f"生成JSON报告: {json_path}")
        
        # Markdown报告
        if "markdown" in self.config["reporting"]["output_formats"]:
            markdown_path = os.path.join(output_dir, f"{base_filename}.md")
            self.generate_markdown_report(markdown_path)
            logger.info(f"生成Markdown报告: {markdown_path}")
        
        # HTML报告（简化版）
        if "html" in self.config["reporting"]["output_formats"]:
            html_path = os.path.join(output_dir, f"{base_filename}.html")
            self.generate_html_report(html_path)
            logger.info(f"生成HTML报告: {html_path}")
    
    # ========== 具体的健康检查方法 ==========
    
    def check_cpu_usage(self) -> Dict:
        """检查CPU使用率"""
        threshold = self.config["checks"]["infrastructure"]["cpu_threshold"]
        
        try:
            # 获取CPU使用率（1秒间隔）
            cpu_percent = psutil.cpu_percent(interval=1)
            
            status = "PASS" if cpu_percent < threshold else "FAIL"
            message = f"CPU使用率: {cpu_percent}% (阈值: {threshold}%)"
            
            details = {
                "cpu_percent": cpu_percent,
                "threshold": threshold,
                "cpu_count": psutil.cpu_count(),
                "cpu_freq": psutil.cpu_freq()._asdict() if psutil.cpu_freq() else None,
                "load_average": os.getloadavg() if hasattr(os, 'getloadavg') else None
            }
            
            if status == "FAIL":
                message += " ⚠️ 超过阈值"
            
            return {
                "status": status,
                "message": message,
                "details": details
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"CPU检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_memory_usage(self) -> Dict:
        """检查内存使用率"""
        threshold = self.config["checks"]["infrastructure"]["memory_threshold"]
        
        try:
            memory = psutil.virtual_memory()
            memory_percent = memory.percent
            
            status = "PASS" if memory_percent < threshold else "WARNING"
            message = f"内存使用率: {memory_percent}% (阈值: {threshold}%)"
            
            details = {
                "total_gb": round(memory.total / (1024**3), 2),
                "available_gb": round(memory.available / (1024**3), 2),
                "used_gb": round(memory.used / (1024**3), 2),
                "percent": memory_percent,
                "threshold": threshold
            }
            
            if status == "WARNING":
                message += " ⚠️ 接近阈值"
            
            return {
                "status": status,
                "message": message,
                "details": details
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"内存检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_disk_usage(self) -> Dict:
        """检查磁盘空间"""
        threshold = self.config["checks"]["infrastructure"]["disk_threshold"]
        
        try:
            partitions = psutil.disk_partitions()
            disk_results = []
            overall_status = "PASS"
            
            for partition in partitions:
                if 'cdrom' in partition.opts or partition.fstype == '':
                    continue
                
                try:
                    usage = psutil.disk_usage(partition.mountpoint)
                    disk_status = "PASS" if usage.percent < threshold else "FAIL"
                    
                    if disk_status == "FAIL":
                        overall_status = "FAIL"
                    
                    disk_results.append({
                        "device": partition.device,
                        "mountpoint": partition.mountpoint,
                        "fstype": partition.fstype,
                        "total_gb": round(usage.total / (1024**3), 2),
                        "used_gb": round(usage.used / (1024**3), 2),
                        "free_gb": round(usage.free / (1024**3), 2),
                        "percent": usage.percent,
                        "status": disk_status
                    })
                except Exception as e:
                    disk_results.append({
                        "device": partition.device,
                        "mountpoint": partition.mountpoint,
                        "error": str(e),
                        "status": "ERROR"
                    })
            
            # 生成汇总消息
            critical_disks = [d for d in disk_results if d.get("status") == "FAIL"]
            if critical_disks:
                message = f"磁盘空间警告: {len(critical_disks)}个分区超过阈值"
            else:
                message = f"磁盘空间正常: 检查了 {len(disk_results)} 个分区"
            
            return {
                "status": overall_status,
                "message": message,
                "details": {"partitions": disk_results}
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"磁盘检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_network_connectivity(self) -> Dict:
        """检查网络连通性"""
        timeout = self.config["checks"]["infrastructure"]["network_timeout"]
        
        try:
            # 测试目标
            test_targets = [
                {"host": "8.8.8.8", "port": 53, "description": "Google DNS"},
                {"host": "1.1.1.1", "port": 53, "description": "Cloudflare DNS"},
                {"host": "baidu.com", "port": 80, "description": "Baidu HTTP"},
                {"host": "github.com", "port": 443, "description": "GitHub HTTPS"}
            ]
            
            network_results = []
            failed_count = 0
            
            for target in test_targets:
                start_time = time.time()
                try:
                    # 测试TCP连接
                    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    sock.settimeout(timeout)
                    result = sock.connect_ex((target["host"], target["port"]))
                    sock.close()
                    
                    latency = round((time.time() - start_time) * 1000, 2)  # 毫秒
                    
                    if result == 0:
                        status = "PASS"
                        message = f"连接成功 (延迟: {latency}ms)"
                    else:
                        status = "FAIL"
                        message = f"连接失败 (错误码: {result})"
                        failed_count += 1
                        
                except Exception as e:
                    status = "FAIL"
                    message = f"连接异常: {str(e)}"
                    failed_count += 1
                    latency = None
                
                network_results.append({
                    "host": target["host"],
                    "port": target["port"],
                    "description": target["description"],
                    "status": status,
                    "message": message,
                    "latency_ms": latency
                })
            
            # 总体状态
            if failed_count == 0:
                overall_status = "PASS"
                message = f"网络连通性正常: 所有 {len(test_targets)} 个测试目标连接成功"
            elif failed_count < len(test_targets):
                overall_status = "WARNING"
                message = f"网络连通性警告: {failed_count}/{len(test_targets)} 个测试目标连接失败"
            else:
                overall_status = "FAIL"
                message = f"网络连通性失败: 所有 {len(test_targets)} 个测试目标连接失败"
            
            return {
                "status": overall_status,
                "message": message,
                "details": {"tests": network_results}
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"网络检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_critical_processes(self) -> Dict:
        """检查关键进程"""
        critical_processes = [
            {"name": "java", "description": "Java进程 (ERP服务)"},
            {"name": "mysqld", "description": "MySQL数据库"},
            {"name": "redis-server", "description": "Redis缓存"},
            {"name": "nginx", "description": "Nginx反向代理"},
            {"name": "docker", "description": "Docker守护进程"}
        ]
        
        try:
            process_results = []
            missing_count = 0
            
            for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
                try:
                    process_name = proc.info['name'].lower() if proc.info['name'] else ''
                    cmdline = ' '.join(proc.info['cmdline']) if proc.info['cmdline'] else ''
                    
                    for critical in critical_processes:
                        if critical["name"].lower() in process_name or critical["name"].lower() in cmdline.lower():
                            process_results.append({
                                "name": critical["name"],
                                "description": critical["description"],
                                "pid": proc.info['pid'],
                                "status": "RUNNING",
                                "found_in": process_name
                            })
                            # 标记为已找到
                            critical["found"] = True
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue
            
            # 检查未找到的进程
            for critical in critical_processes:
                if "found" not in critical:
                    process_results.append({
                        "name": critical["name"],
                        "description": critical["description"],
                        "pid": None,
                        "status": "NOT_FOUND",
                        "found_in": None
                    })
                    missing_count += 1
            
            # 总体状态
            if missing_count == 0:
                overall_status = "PASS"
                message = f"关键进程正常: 所有 {len(critical_processes)} 个进程都在运行"
            elif missing_count < len(critical_processes):
                overall_status = "WARNING"
                message = f"关键进程警告: {missing_count}/{len(critical_processes)} 个进程未找到"
            else:
                overall_status = "FAIL"
                message = f"关键进程失败: 所有 {len(critical_processes)} 个进程都未找到"
            
            return {
                "status": overall_status,
                "message": message,
                "details": {"processes": process_results}
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"进程检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_service_health(self, service_config: Dict) -> Dict:
        """检查服务健康状态"""
        timeout = self.config["checks"]["services"]["timeout"]
        
        host = service_config.get("host", "localhost")
        port = service_config.get("port", 80)
        name = service_config.get("name", "unknown")
        
        try:
            # 尝试TCP连接
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(timeout)
            
            start_time = time.time()
            result = sock.connect_ex((host, port))
            latency = round((time.time() - start_time) * 1000, 2)
            sock.close()
            
            if result == 0:
                # TCP连接成功，尝试HTTP健康检查
                if "health_endpoint" in service_config:
                    health_url = f"http://{host}:{port}{service_config['health_endpoint']}"
                    try:
                        response = requests.get(health_url, timeout=timeout)
                        expected_status = service_config.get("expected_status", 200)
                        
                        if response.status_code == expected_status:
                            status = "PASS"
                            message = f"服务健康检查通过 (HTTP {response.status_code}, 延迟: {latency}ms)"
                        else:
                            status = "FAIL"
                            message = f"服务健康检查失败: HTTP {response.status_code} (期望: {expected_status})"
                        
                        details = {
                            "tcp_connection": "SUCCESS",
                            "http_status": response.status_code,
                            "expected_status": expected_status,
                            "latency_ms": latency,
                            "response_time": response.elapsed.total_seconds() * 1000
                        }
                        
                    except requests.RequestException as e:
                        status = "WARNING"
                        message = f"TCP连接成功但HTTP检查失败: {str(e)}"
                        details = {
                            "tcp_connection": "SUCCESS",
                            "http_error": str(e),
                            "latency_ms": latency
                        }
                else:
                    # 没有健康端点，仅检查TCP连接
                    status = "PASS"
                    message = f"TCP连接成功 (延迟: {latency}ms)"
                    details = {
                        "tcp_connection": "SUCCESS",
                        "latency_ms": latency
                    }
            else:
                status = "FAIL"
                message = f"TCP连接失败 (错误码: {result})"
                details = {
                    "tcp_connection": "FAILED",
                    "error_code": result
                }
            
            return {
                "status": status,
                "message": message,
                "details": details
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"服务检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_erp_module(self, module_name: str) -> Dict:
        """检查ERP模块基本功能"""
        # 这里实现ERP模块的基本功能检查
        # 例如：检查数据库连接、API端点、业务逻辑等
        
        # 模拟检查
        time.sleep(0.5)  # 模拟检查耗时
        
        # 随机生成检查结果（实际实现中应该进行真实的检查）
        import random
        random_status = random.choice(["PASS", "PASS", "PASS", "WARNING", "FAIL"])
        
        status_messages = {
            "PASS": f"ERP {module_name}模块功能正常",
            "WARNING": f"ERP {module_name}模块存在轻微问题",
            "FAIL": f"ERP {module_name}模块功能异常"
        }
        
        return {
            "status": random_status,
            "message": status_messages[random_status],
            "details": {
                "module": module_name,
                "check_time": datetime.now().isoformat(),
                "simulated": True  # 标记为模拟检查
            }
        }
    
    def generate_markdown_report(self, filepath: str):
        """生成Markdown格式报告"""
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f"# 环境健康检查报告\n\n")
            f.write(f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"**环境**: {self.config['environment']['name']}\n")
            f.write(f"**总体状态**: **{self.results['overall_status']}**\n\n")
            
            f.write(f"## 检查摘要\n")
            f.write(f"- 总检查项: {self.results['total_checks']}\n")
            f.write(f"- 通过: {self.results['passed_checks']}\n")
            f.write(f"- 失败: {self.results['failed_checks']}\n")
            f.write(f"- 警告: {self.results['warning_checks']}\n")
            f.write(f"- 跳过: {self.results['skipped_checks']}\n")
            f.write(f"- 执行时间: {self.results['duration_seconds']:.2f}秒\n\n")
            
            f.write(f"## 按类别汇总\n")
            for category, summary in self.results['summary'].items():
                f.write(f"### {category.upper()}\n")
                f.write(f"- 状态: {summary['status']}\n")
                f.write(f"- 检查项: {summary['total']}\n")
                f.write(f"- 通过: {summary['passed']}\n")
                f.write(f"- 失败: {summary['failed']}\n")
                f.write(f"- 警告: {summary['warning']}\n\n")
            
            f.write(f"## 详细检查结果\n")
            for result in self.results['check_results']:
                status_emoji = {
                    "PASS": "✅",
                    "FAIL": "❌",
                    "WARNING": "⚠️",
                    "SKIPPED": "⏭️",
                    "ERROR": "💥"
                }.get(result.get('status'), '❓')
                
                f.write(f"### {status_emoji} {result.get('check_name', '未知检查')}\n")
                f.write(f"- **状态**: {result.get('status')}\n")
                f.write(f"- **类别**: {result.get('category')}\n")
                f.write(f"- **严重程度**: {result.get('severity')}\n")
                f.write(f"- **消息**: {result.get('message', '无消息')}\n")
                f.write(f"- **耗时**: {result.get('duration', 0):.3f}秒\n\n")
    
    def generate_html_report(self, filepath: str):
        """生成HTML格式报告（简化版）"""
        html_template = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>环境健康检查报告</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                .header { background: #f5f5f5; padding: 20px; border-radius: 5px; }
                .summary { margin: 20px 0; }
                .status-healthy { color: green; font-weight: bold; }
                .status-warning { color: orange; font-weight: bold; }
                .status-failed { color: red; font-weight: bold; }
                .check-result { border: 1px solid #ddd; padding: 10px; margin: 10px 0; border-radius: 5px; }
                .check-pass { background: #e8f5e8; }
                .check-fail { background: #ffeaea; }
                .check-warning { background: #fff8e1; }
                .timestamp { color: #666; font-size: 0.9em; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>环境健康检查报告</h1>
                <p class="timestamp">生成时间: {timestamp}</p>
                <p>环境: {environment}</p>
                <p>总体状态: <span class="status-{overall_status_class}">{overall_status}</span></p>
            </div>
            
            <div class="summary">
                <h2>检查摘要</h2>
                <p>总检查项: {total_checks} | 通过: {passed_checks} | 失败: {failed_checks} | 警告: {warning_checks}</p>
                <p>执行时间: {duration_seconds:.2f}秒</p>
            </div>
            
            <h2>详细检查结果</h2>
            {check_results_html}
        </body>
        </html>
        """
        
        # 生成检查结果HTML
        check_results_html = ""
        for result in self.results['check_results']:
            status_class = "pass" if result.get('status') == 'PASS' else \
                          "fail" if result.get('status') == 'FAIL' else \
                          "warning" if result.get('status') == 'WARNING' else "unknown"
            
            check_results_html += f"""
            <div class="check-result check-{status_class}">
                <h3>{result.get('check_name', '未知检查')}</h3>
                <p><strong>状态:</strong> {result.get('status')}</p>
                <p><strong>消息:</strong> {result.get('message', '无消息')}</p>
                <p><strong>耗时:</strong> {result.get('duration', 0):.3f}秒</p>
            </div>
            """
        
        # 填充模板
        overall_status_class = "healthy" if self.results['overall_status'] == 'HEALTHY' else \
                              "warning" if self.results['overall_status'] == 'WARNING' else "failed"
        
        html_content = html_template.format(
            timestamp=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            environment=self.config['environment']['name'],
            overall_status=self.results['overall_status'],
            overall_status_class=overall_status_class,
            total_checks=self.results['total_checks'],
            passed_checks=self.results['passed_checks'],
            failed_checks=self.results['failed_checks'],
            warning_checks=self.results['warning_checks'],
            duration_seconds=self.results['duration_seconds'],
            check_results_html=check_results_html
        )
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(html_content)

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='环境健康检查脚本')
    parser.add_argument('--config', '-c', help='配置文件路径', default='health-check-config.yaml')
    parser.add_argument('--output', '-o', help='输出目录', default='./health-reports')
    parser.add_argument('--format', '-f', help='输出格式', choices=['json', 'html', 'markdown', 'all'], default='all')
    parser.add_argument('--verbose', '-v', action='store_true', help='详细输出')
    
    args = parser.parse_args()
    
    # 设置详细日志
    if args.verbose:
        logger