#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
配置合规性检查脚本 - 子任务3
版本: 1.0.0
创建日期: 2026-04-29
作者: qa-lead
项目: AI-Ready测试环境配置专项
Sprint: Sprint 27+1
任务ID: task_1777438750224_y4l4dft4a

功能: 执行测试环境的配置合规性检查，确保环境配置符合标准和最佳实践
"""

import os
import sys
import json
import yaml
import re
import logging
import argparse
import subprocess
import platform
import socket
import hashlib
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Tuple, Optional
import psutil
import requests

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('config-compliance-check.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class ConfigComplianceChecker:
    """配置合规性检查器"""
    
    def __init__(self, config_file: str = None):
        """
        初始化配置合规性检查器
        
        Args:
            config_file: 配置文件路径
        """
        self.start_time = datetime.now()
        self.results = {
            "overall_compliance": "UNKNOWN",
            "total_checks": 0,
            "compliant_checks": 0,
            "non_compliant_checks": 0,
            "warning_checks": 0,
            "check_results": [],
            "compliance_summary": {},
            "timestamp": self.start_time.isoformat(),
            "duration_seconds": 0
        }
        
        # 加载配置
        self.config = self.load_config(config_file)
        
        # 加载合规规则
        self.compliance_rules = self.load_compliance_rules()
        
        # 设置检查项
        self.check_items = self.initialize_check_items()
        
    def load_config(self, config_file: str = None) -> Dict:
        """加载配置文件"""
        default_config = {
            "environment": {
                "name": "test",
                "type": "test-environment",
                "compliance_standard": "enterprise-baseline"
            },
            "compliance": {
                "enabled_categories": [
                    "os_config",
                    "security_config",
                    "network_config",
                    "application_config",
                    "database_config"
                ],
                "strict_mode": False,
                "auto_remediation": False
            },
            "reporting": {
                "output_formats": ["json", "html", "markdown"],
                "output_dir": "./compliance-reports",
                "generate_remediation_plan": True
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
    
    def load_compliance_rules(self) -> Dict:
        """加载合规规则"""
        # 这里可以加载外部规则文件，暂时使用内置规则
        return {
            "os_config": {
                "rules": [
                    {
                        "id": "os_user_password_policy",
                        "name": "用户密码策略检查",
                        "description": "检查密码策略是否符合企业安全标准",
                        "severity": "high",
                        "check_method": "command",
                        "command": "net accounts",
                        "expected_patterns": [
                            r"Minimum password age.*: (\d+) days",
                            r"Minimum password length.*: (\d+) characters",
                            r"Password history.*: (\d+) remembered"
                        ],
                        "expected_values": {
                            "Minimum password age": {"min": 1, "max": 7},
                            "Minimum password length": {"min": 8, "max": 20},
                            "Password history": {"min": 5, "max": 24}
                        }
                    },
                    {
                        "id": "os_audit_policy",
                        "name": "审计策略检查",
                        "description": "检查系统审计策略是否启用",
                        "severity": "medium",
                        "check_method": "file",
                        "file_path": "/etc/audit/audit.rules",
                        "expected_patterns": [
                            r"-w /etc/passwd -p wa -k identity",
                            r"-w /etc/shadow -p wa -k identity",
                            r"-w /var/log/auth.log -p wa -k authentication"
                        ]
                    }
                ]
            },
            "security_config": {
                "rules": [
                    {
                        "id": "security_firewall_status",
                        "name": "防火墙状态检查",
                        "description": "检查防火墙是否启用",
                        "severity": "critical",
                        "check_method": "service_status",
                        "service_name": "firewalld",
                        "expected_status": "active"
                    },
                    {
                        "id": "security_ssh_config",
                        "name": "SSH配置检查",
                        "description": "检查SSH服务的安全配置",
                        "severity": "high",
                        "check_method": "file",
                        "file_path": "/etc/ssh/sshd_config",
                        "expected_patterns": [
                            r"^PermitRootLogin no",
                            r"^PasswordAuthentication no",
                            r"^X11Forwarding no",
                            r"^MaxAuthTries \d+"
                        ]
                    }
                ]
            },
            "application_config": {
                "rules": [
                    {
                        "id": "app_log_config",
                        "name": "应用程序日志配置检查",
                        "description": "检查应用程序日志配置是否正确",
                        "severity": "medium",
                        "check_method": "file_exists",
                        "file_path": "/var/log/application.log",
                        "expected": True,
                        "permissions": "644"
                    },
                    {
                        "id": "app_env_config",
                        "name": "环境变量配置检查",
                        "description": "检查关键环境变量是否设置",
                        "severity": "medium",
                        "check_method": "env_variable",
                        "variables": [
                            "JAVA_HOME",
                            "PATH",
                            "ERP_DB_HOST",
                            "ERP_DB_PASSWORD"
                        ],
                        "expected": "not_empty"
                    }
                ]
            }
        }
    
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
        
        # 根据启用的类别加载检查项
        enabled_categories = self.config["compliance"]["enabled_categories"]
        
        for category in enabled_categories:
            if category in self.compliance_rules:
                for rule in self.compliance_rules[category]["rules"]:
                    check_items.append({
                        "id": rule["id"],
                        "name": rule["name"],
                        "category": category,
                        "description": rule["description"],
                        "severity": rule["severity"],
                        "rule_config": rule,
                        "function": self.create_check_function(rule)
                    })
        
        logger.info(f"初始化了 {len(check_items)} 个配置合规性检查项")
        return check_items
    
    def create_check_function(self, rule: Dict):
        """根据规则创建检查函数"""
        check_method = rule.get("check_method", "command")
        
        if check_method == "command":
            return lambda r=rule: self.check_by_command(r)
        elif check_method == "file":
            return lambda r=rule: self.check_by_file(r)
        elif check_method == "file_exists":
            return lambda r=rule: self.check_file_exists(r)
        elif check_method == "service_status":
            return lambda r=rule: self.check_service_status(r)
        elif check_method == "env_variable":
            return lambda r=rule: self.check_env_variables(r)
        elif check_method == "port_listening":
            return lambda r=rule: self.check_port_listening(r)
        else:
            return lambda r=rule: self.check_generic(r)
    
    def run_all_checks(self) -> Dict:
        """执行所有配置合规性检查"""
        logger.info("开始执行配置合规性检查...")
        self.results["total_checks"] = len(self.check_items)
        
        # 执行检查
        for check_item in self.check_items:
            result = self.execute_single_check(check_item)
            self.process_check_result(result)
        
        # 计算总体合规性
        self.calculate_overall_compliance()
        
        # 计算执行时间
        end_time = datetime.now()
        self.results["duration_seconds"] = (end_time - self.start_time).total_seconds()
        
        # 生成报告
        self.generate_reports()
        
        # 生成修复建议（如果启用）
        if self.config["reporting"]["generate_remediation_plan"]:
            self.generate_remediation_plan()
        
        logger.info(f"配置合规性检查完成。总体合规性: {self.results['overall_compliance']}")
        logger.info(f"检查结果: {self.results['compliant_checks']} 合规, "
                   f"{self.results['non_compliant_checks']} 不合规, "
                   f"{self.results['warning_checks']} 警告")
        
        return self.results
    
    def execute_single_check(self, check_item: Dict) -> Dict:
        """执行单个检查项"""
        start_time = time.time()
        check_id = check_item["id"]
        check_name = check_item["name"]
        
        logger.info(f"执行配置合规性检查: {check_name}")
        
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
                "details": {"error": str(e)},
                "duration": round(time.time() - start_time, 3)
            }
    
    def process_check_result(self, result: Dict):
        """处理检查结果"""
        self.results["check_results"].append(result)
        
        if result["status"] == "COMPLIANT":
            self.results["compliant_checks"] += 1
        elif result["status"] == "NON_COMPLIANT":
            self.results["non_compliant_checks"] += 1
        elif result["status"] == "WARNING":
            self.results["warning_checks"] += 1
        elif result["status"] == "ERROR":
            self.results["non_compliant_checks"] += 1
    
    def calculate_overall_compliance(self):
        """计算总体合规性"""
        if self.results["non_compliant_checks"] > 0:
            self.results["overall_compliance"] = "NON_COMPLIANT"
        elif self.results["warning_checks"] > 0:
            self.results["overall_compliance"] = "WARNING"
        elif self.results["compliant_checks"] == self.results["total_checks"]:
            self.results["overall_compliance"] = "FULLY_COMPLIANT"
        else:
            self.results["overall_compliance"] = "UNKNOWN"
        
        # 按类别生成摘要
        for category in set([r.get("category") for r in self.results["check_results"]]):
            category_results = [r for r in self.results["check_results"] if r.get("category") == category]
            
            compliant = sum(1 for r in category_results if r.get("status") == "COMPLIANT")
            non_compliant = sum(1 for r in category_results if r.get("status") == "NON_COMPLIANT")
            warning = sum(1 for r in category_results if r.get("status") == "WARNING")
            total = len(category_results)
            
            if non_compliant > 0:
                status = "NON_COMPLIANT"
            elif warning > 0:
                status = "WARNING"
            elif compliant == total:
                status = "FULLY_COMPLIANT"
            else:
                status = "UNKNOWN"
            
            self.results["compliance_summary"][category] = {
                "status": status,
                "total": total,
                "compliant": compliant,
                "non_compliant": non_compliant,
                "warning": warning,
                "compliance_rate": round(compliant / total * 100, 2) if total > 0 else 0
            }
    
    # ========== 具体的检查方法 ==========
    
    def check_by_command(self, rule: Dict) -> Dict:
        """通过命令执行检查"""
        try:
            command = rule.get("command", "")
            expected_patterns = rule.get("expected_patterns", [])
            expected_values = rule.get("expected_values", {})
            
            # 执行命令
            result = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True,
                timeout=30
            )
            
            output = result.stdout
            error = result.stderr
            
            if result.returncode != 0:
                return {
                    "status": "NON_COMPLIANT",
                    "message": f"命令执行失败: {error}",
                    "details": {
                        "command": command,
                        "return_code": result.returncode,
                        "stderr": error
                    }
                }
            
            # 检查预期模式
            findings = []
            all_patterns_matched = True
            
            for pattern in expected_patterns:
                match = re.search(pattern, output, re.IGNORECASE)
                if match:
                    findings.append({
                        "pattern": pattern,
                        "matched": True,
                        "value": match.group(1) if match.groups() else match.group(0)
                    })
                else:
                    findings.append({
                        "pattern": pattern,
                        "matched": False,
                        "value": None
                    })
                    all_patterns_matched = False
            
            # 检查预期值范围
            value_findings = []
            for key, expected_range in expected_values.items():
                # 在输出中查找键值对
                key_pattern = rf"{re.escape(key)}.*: (\d+)"
                match = re.search(key_pattern, output, re.IGNORECASE)
                
                if match:
                    value = int(match.group(1))
                    min_val = expected_range.get("min")
                    max_val = expected_range.get("max")
                    
                    if (min_val is not None and value < min_val) or (max_val is not None and value > max_val):
                        value_findings.append({
                            "key": key,
                            "value": value,
                            "expected_min": min_val,
                            "expected_max": max_val,
                            "compliant": False
                        })
                        all_patterns_matched = False
                    else:
                        value_findings.append({
                            "key": key,
                            "value": value,
                            "expected_min": min_val,
                            "expected_max": max_val,
                            "compliant": True
                        })
                else:
                    value_findings.append({
                        "key": key,
                        "value": None,
                        "error": "未找到"
                    })
                    all_patterns_matched = False
            
            if all_patterns_matched and all(v.get("compliant", True) for v in value_findings):
                status = "COMPLIANT"
                message = "所有配置项符合预期"
            else:
                status = "NON_COMPLIANT"
                message = "部分配置项不符合预期"
            
            return {
                "status": status,
                "message": message,
                "details": {
                    "command": command,
                    "output": output,
                    "pattern_findings": findings,
                    "value_findings": value_findings
                }
            }
            
        except subprocess.TimeoutExpired:
            return {
                "status": "ERROR",
                "message": "命令执行超时",
                "details": {"command": rule.get("command"), "timeout": 30}
            }
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"命令检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_by_file(self, rule: Dict) -> Dict:
        """通过文件内容检查"""
        try:
            file_path = rule.get("file_path", "")
            expected_patterns = rule.get("expected_patterns", [])
            
            if not os.path.exists(file_path):
                return {
                    "status": "NON_COMPLIANT",
                    "message": f"文件不存在: {file_path}",
                    "details": {"file_path": file_path, "exists": False}
                }
            
            # 读取文件内容
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            # 检查文件权限
            file_stat = os.stat(file_path)
            permissions = oct(file_stat.st_mode)[-3:]
            
            # 检查预期模式
            findings = []
            all_patterns_matched = True
            
            for pattern in expected_patterns:
                match = re.search(pattern, content, re.MULTILINE)
                if match:
                    findings.append({
                        "pattern": pattern,
                        "matched": True,
                        "line": match.group(0)
                    })
                else:
                    findings.append({
                        "pattern": pattern,
                        "matched": False,
                        "line": None
                    })
                    all_patterns_matched = False
            
            # 检查文件权限（如果指定）
            expected_permissions = rule.get("permissions")
            permissions_compliant = True
            if expected_permissions:
                permissions_compliant = (permissions == expected_permissions)
                all_patterns_matched = all_patterns_matched and permissions_compliant
            
            if all_patterns_matched:
                status = "COMPLIANT"
                message = f"文件内容符合预期 (权限: {permissions})"
            else:
                status = "NON_COMPLIANT"
                message = f"文件内容或权限不符合预期"
            
            return {
                "status": status,
                "message": message,
                "details": {
                    "file_path": file_path,
                    "exists": True,
                    "permissions": permissions,
                    "expected_permissions": expected_permissions,
                    "permissions_compliant": permissions_compliant,
                    "pattern_findings": findings
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"文件检查失败: {str(e)}",
                "details": {"error": str(e), "file_path": rule.get("file_path")}
            }
    
    def check_file_exists(self, rule: Dict) -> Dict:
        """检查文件是否存在"""
        try:
            file_path = rule.get("file_path", "")
            expected = rule.get("expected", True)
            
            exists = os.path.exists(file_path)
            
            if exists == expected:
                status = "COMPLIANT"
                message = f"文件存在性符合预期: {'存在' if exists else '不存在'}"
            else:
                status = "NON_COMPLIANT"
                message = f"文件存在性不符合预期: 期望{'存在' if expected else '不存在'}, 实际{'存在' if exists else '不存在'}"
            
            # 检查权限（如果文件存在）
            permissions = None
            if exists:
                file_stat = os.stat(file_path)
                permissions = oct(file_stat.st_mode)[-3:]
                
                expected_permissions = rule.get("permissions")
                if expected_permissions and permissions != expected_permissions:
                    status = "NON_COMPLIANT"
                    message += f" (权限不符: 实际{permissions}, 期望{expected_permissions})"
            
            return {
                "status": status,
                "message": message,
                "details": {
                    "file_path": file_path,
                    "exists": exists,
                    "expected": expected,
                    "permissions": permissions,
                    "expected_permissions": rule.get("permissions")
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"文件存在性检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_service_status(self, rule: Dict) -> Dict:
        """检查服务状态"""
        try:
            service_name = rule.get("service_name", "")
            expected_status = rule.get("expected_status", "active")
            
            # 根据操作系统使用不同的命令
            if platform.system() == "Windows":
                command = f"sc query {service_name}"
            else:
                command = f"systemctl is-active {service_name}"
            
            result = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            actual_status = result.stdout.strip().lower()
            
            if result.returncode == 0 and actual_status == expected_status.lower():
                status = "COMPLIANT"
                message = f"服务状态符合预期: {actual_status}"
            else:
                status = "NON_COMPLIANT"
                message = f"服务状态不符合预期: 期望{expected_status}, 实际{actual_status or '未找到'}"
            
            return {
                "status": status,
                "message": message,
                "details": {
                    "service_name": service_name,
                    "expected_status": expected_status,
                    "actual_status": actual_status,
                    "command_output": result.stdout,
                    "command_error": result.stderr,
                    "return_code": result.returncode
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"服务状态检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_env_variables(self, rule: Dict) -> Dict:
        """检查环境变量"""
        try:
            variables = rule.get("variables", [])
            expected = rule.get("expected", "not_empty")
            
            findings = []
            all_compliant = True
            
            for var in variables:
                value = os.environ.get(var)
                
                if expected == "not_empty":
                    compliant = bool(value)
                    finding = {
                        "variable": var,
                        "value": value,
                        "expected": "not_empty",
                        "compliant": compliant
                    }
                elif expected == "empty":
                    compliant = not bool(value)
                    finding = {
                        "variable": var,
                        "value": value,
                        "expected": "empty",
                        "compliant": compliant
                    }
                else:
                    # 期望特定值
                    compliant = (value == expected)
                    finding = {
                        "variable": var,
                        "value": value,
                        "expected": expected,
                        "compliant": compliant
                    }
                
                findings.append(finding)
                if not compliant:
                    all_compliant = False
            
            if all_compliant:
                status = "COMPLIANT"
                message = f"所有环境变量符合预期 ({len(variables)}个)"
            else:
                status = "NON_COMPLIANT"
                non_compliant_vars = [f["variable"] for f in findings if not f["compliant"]]
                message = f"部分环境变量不符合预期: {', '.join(non_compliant_vars)}"
            
            return {
                "status": status,
                "message": message,
                "details": {"variable_findings": findings}
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"环境变量检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_port_listening(self, rule: Dict) -> Dict:
        """检查端口监听状态"""
        try:
            port = rule.get("port")
            protocol = rule.get("protocol", "tcp")
            expected = rule.get("expected", True)
            
            if not port:
                return {
                    "status": "ERROR",
                    "message": "未指定端口号",
                    "details": {"rule": rule}
                }
            
            # 检查端口是否在监听
            listening = False
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(2)
                result = sock.connect_ex(('localhost', port))
                sock.close()
                listening = (result == 0)
            except:
                listening = False
            
            if listening == expected:
                status = "COMPLIANT"
                message = f"端口监听状态符合预期: 端口{port} {'在监听' if listening else '未监听'}"
            else:
                status = "NON_COMPLIANT"
                message = f"端口监听状态不符合预期: 期望{'监听' if expected else '不监听'}, 实际{'监听' if listening else '不监听'}"
            
            return {
                "status": status,
                "message": message,
                "details": {
                    "port": port,
                    "protocol": protocol,
                    "expected": expected,
                    "actual": listening
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"端口检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_generic(self, rule: Dict) -> Dict:
        """通用检查方法"""
        try:
            # 这里可以实现自定义检查逻辑
            check_type = rule.get("type", "custom")
            
            if check_type == "custom":
                # 执行自定义检查逻辑
                custom_check = rule.get("custom_check")
                if custom_check:
                    # 这里可以执行自定义脚本或函数
                    # 暂时返回模拟结果
                    return {
                        "status": "WARNING",
                        "message": "自定义检查未实现",
                        "details": {"rule": rule, "note": "需要实现自定义检查逻辑"}
                    }
            
            return {
                "status": "WARNING",
                "message": f"未实现的检查类型: {check_type}",
                "details": {"rule": rule}
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"通用检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def generate_reports(self):
        """生成合规性报告"""
        output_dir = self.config["reporting"]["output_dir"]
        os.makedirs(output_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        base_filename = f"config-compliance-report-{timestamp}"
        
        # JSON报告
        if "json" in self.config["reporting"]["output_formats"]:
            json_path = os.path.join(output_dir, f"{base_filename}.json")
            with open(json_path, 'w', encoding='utf-8') as f:
                json.dump(self.results, f, indent=2, ensure_ascii=False)
            logger.info(f"生成JSON合规性报告: {json_path}")
        
        # Markdown报告
        if "markdown" in self.config["reporting"]["output_formats"]:
            markdown_path = os.path.join(output_dir, f"{base_filename}.md")
            self.generate_markdown_report(markdown_path)
            logger.info(f"生成Markdown合规性报告: {markdown_path}")
        
        # HTML报告
        if "html" in self.config["reporting"]["output_formats"]:
            html_path = os.path.join(output_dir, f"{base_filename}.html")
            self.generate_html_report(html_path)
            logger.info(f"生成HTML合规性报告: {html_path}")
    
    def generate_markdown_report(self, filepath: str):
        """生成Markdown格式合规性报告"""
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f"# 配置合规性检查报告\n\n")
            f.write(f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"**环境**: {self.config['environment']['name']}\n")
            f.write(f"**合规标准**: {self.config['environment']['compliance_standard']}\n")
            f.write(f"**总体合规性**: **{self.results['overall_compliance']}**\n\n")
            
            f.write(f"## 检查摘要\n")
            f.write(f"- 总检查项: {self.results['total_checks']}\n")
            f.write(f"- 合规项: {self.results['compliant_checks']}\n")
            f.write(f"- 不合规项: {self.results['non_compliant_checks']}\n")
            f.write(f"- 警告项: {self.results['warning_checks']}\n")
            f.write(f"- 执行时间: {self.results['duration_seconds']:.2f}秒\n\n")
            
            f.write(f"## 按类别合规率\n")
            for category, summary in self.results['compliance_summary'].items():
                f.write(f"### {category.upper()}\n")
                f.write(f"- 合规状态: {summary['status']}\n")
                f.write(f"- 合规率: {summary['compliance_rate']}%\n")
                f.write(f"- 检查项: {summary['total']}\n")
                f.write(f"- 合规: {summary['compliant']}\n")
                f.write(f"- 不合规: {summary['non_compliant']}\n")
                f.write(f"- 警告: {summary['warning']}\n\n")
            
            f.write(f"## 详细检查结果\n")
            for result in self.results['check_results']:
                status_emoji = {
                    "COMPLIANT": "✅",
                    "NON_COMPLIANT": "❌",
                    "WARNING": "⚠️",
                    "ERROR": "💥"
                }.get(result.get('status'), '❓')
                
                f.write(f"### {status_emoji} {result.get('check_name', '未知检查')}\n")
                f.write(f"- **状态**: {result.get('status')}\n")
                f.write(f"- **类别**: {result.get('category')}\n")
                f.write(f"- **严重程度**: {result.get('severity')}\n")
                f.write(f"- **消息**: {result.get('message', '无消息')}\n")
                f.write(f"- **耗时**: {result.get('duration', 0):.3f}秒\n\n")
    
    def generate_html_report(self, filepath: str):
        """生成HTML格式合规性报告"""
        html_template = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>配置合规性检查报告</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; background: #f9f9f9; }
                .header { background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .summary { background: #fff; padding: 20px; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .compliance-summary { display: flex; flex-wrap: wrap; gap: 20px; margin: 20px 0; }
                .category-card { background: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); flex: 1; min-width: 200px; }
                .check-result { background: #fff; padding: 15px; margin: 10px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); border-left: 4px solid #ccc; }
                .check-compliant { border-left-color: #4CAF50; }
                .check-non-compliant { border-left-color: #F44336; }
                .check-warning { border-left-color: #FF9800; }
                .check-error { border-left-color: #9E9E9E; }
                .status-badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 0.9em; font-weight: bold; }
                .status-compliant { background: #4CAF50; color: white; }
                .status-non-compliant { background: #F44336; color: white; }
                .status-warning { background: #FF9800; color: white; }
                .status-error { background: #9E9E9E; color: white; }
                .severity-critical { color: #F44336; font-weight: bold; }
                .severity-high { color: #FF9800; }
                .severity-medium { color: #2196F3; }
                .severity-low { color: #4CAF50; }
                .timestamp { color: #666; font-size: 0.9em; }
                h1, h2, h3 { color: #333; }
                .progress-bar { height: 20px; background: #e0e0e0; border-radius: 10px; overflow: hidden; margin: 10px 0; }
                .progress-fill { height: 100%; background: #4CAF50; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>配置合规性检查报告</h1>
                <p class="timestamp">生成时间: {timestamp}</p>
                <p>环境: {environment} | 合规标准: {compliance_standard}</p>
                <p>总体合规性: <span class="status-badge status-{overall_status_class}">{overall_compliance}</span></p>
            </div>
            
            <div class="summary">
                <h2>检查摘要</h2>
                <p>总检查项: {total_checks} | 合规: {compliant_checks} | 不合规: {non_compliant_checks} | 警告: {warning_checks}</p>
                <p>执行时间: {duration_seconds:.2f}秒</p>
                
                <div class="progress-bar">
                    <div class="progress-fill" style="width: {compliance_rate}%"></div>
                </div>
                <p>总体合规率: {compliance_rate}%</p>
            </div>
            
            <h2>按类别合规率</h2>
            <div class