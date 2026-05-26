#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试环境质量检查自动化框架 - 主集成脚本
版本: 1.0.0
创建日期: 2026-04-29
作者: qa-lead
项目: AI-Ready测试环境配置专项
Sprint: Sprint 27+1
任务ID: task_1777438750224_y4l4dft4a

功能: 集成所有质量检查子任务，提供统一的自动化框架
包含：环境健康检查、配置合规检查、性能基线检查
"""

import os
import sys
import json
import yaml
import time
import logging
import argparse
import schedule
import threading
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Any, Optional
from enum import Enum
import subprocess
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('quality-automation-framework.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class CheckType(Enum):
    """检查类型枚举"""
    HEALTH_CHECK = "health_check"
    COMPLIANCE_CHECK = "compliance_check"
    PERFORMANCE_CHECK = "performance_check"
    ALL_CHECKS = "all_checks"

class AutomationScheduler:
    """自动化调度器"""
    
    def __init__(self, config_file: str = None):
        """
        初始化自动化调度器
        
        Args:
            config_file: 配置文件路径
        """
        self.config = self.load_config(config_file)
        self.is_running = False
        self.scheduler_thread = None
        
        # 结果存储
        self.results_history = []
        self.max_history_size = 100
        
        # 初始化检查模块
        self.check_modules = self.initialize_check_modules()
        
    def load_config(self, config_file: str = None) -> Dict:
        """加载配置文件"""
        default_config = {
            "framework": {
                "name": "Test Environment Quality Automation Framework",
                "version": "1.0.0",
                "description": "自动化测试环境质量检查框架"
            },
            "scheduler": {
                "enabled": True,
                "mode": "cron",  # cron, interval, manual
                "cron_expression": "0 */2 * * *",  # 每2小时执行一次
                "check_interval_minutes": 30,
                "start_time": "09:00",
                "end_time": "18:00",
                "weekdays_only": True
            },
            "checks": {
                "enabled_checks": [
                    "health_check",
                    "compliance_check",
                    "performance_check"
                ],
                "health_check_config": "health-check-config.yaml",
                "compliance_check_config": "compliance-config.yaml",
                "performance_check_config": "performance-config.yaml",
                "parallel_execution": True,
                "timeout_minutes": 30
            },
            "reporting": {
                "output_dir": "./quality-reports",
                "consolidated_reports": True,
                "generate_dashboard": True,
                "retention_days": 30,
                "notification": {
                    "enabled": True,
                    "channels": ["email", "log"],
                    "email": {
                        "smtp_server": "smtp.example.com",
                        "smtp_port": 587,
                        "sender": "quality-check@example.com",
                        "recipients": ["qa-team@example.com"],
                        "on_failure": True,
                        "on_warning": True,
                        "daily_summary": True
                    }
                }
            },
            "monitoring": {
                "enable_prometheus": False,
                "prometheus_pushgateway": "http://localhost:9091",
                "enable_elasticsearch": False,
                "elasticsearch_hosts": ["http://localhost:9200"]
            }
        }
        
        if config_file and os.path.exists(config_file):
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    user_config = yaml.safe_load(f)
                    self.merge_configs(default_config, user_config)
                    logger.info(f"已加载配置文件: {config_file}")
            except Exception as e:
                logger.error(f"加载配置文件失败: {e}")
        
        return default_config
    
    def merge_configs(self, default: Dict, user: Dict) -> Dict:
        """递归合并配置"""
        for key, value in user.items():
            if key in default and isinstance(default[key], dict) and isinstance(value, dict):
                self.merge_configs(default[key], value)
            else:
                default[key] = value
        return default
    
    def initialize_check_modules(self) -> Dict[str, Any]:
        """初始化检查模块"""
        modules = {}
        
        # 动态导入检查模块
        try:
            # 环境健康检查模块
            if "health_check" in self.config["checks"]["enabled_checks"]:
                modules["health_check"] = {
                    "script": "environment-health-check-script.py",
                    "config": self.config["checks"]["health_check_config"],
                    "description": "环境健康检查"
                }
            
            # 配置合规检查模块
            if "compliance_check" in self.config["checks"]["enabled_checks"]:
                modules["compliance_check"] = {
                    "script": "config-compliance-check-script.py",
                    "config": self.config["checks"]["compliance_check_config"],
                    "description": "配置合规检查"
                }
            
            # 性能基线检查模块
            if "performance_check" in self.config["checks"]["enabled_checks"]:
                modules["performance_check"] = {
                    "script": "performance-baseline-check-script.py",
                    "config": self.config["checks"]["performance_check_config"],
                    "description": "性能基线检查"
                }
            
            logger.info(f"初始化了 {len(modules)} 个检查模块")
            
        except Exception as e:
            logger.error(f"初始化检查模块失败: {e}")
        
        return modules
    
    def run_checks(self, check_types: List[str] = None) -> Dict:
        """
        执行检查
        
        Args:
            check_types: 要执行的检查类型列表，None表示执行所有启用检查
        
        Returns:
            检查结果字典
        """
        start_time = datetime.now()
        logger.info(f"开始执行质量检查，时间: {start_time}")
        
        if check_types is None:
            check_types = list(self.check_modules.keys())
        
        results = {
            "execution_id": f"exec_{int(start_time.timestamp())}",
            "start_time": start_time.isoformat(),
            "check_types": check_types,
            "check_results": {},
            "overall_status": "UNKNOWN",
            "summary": {},
            "duration_seconds": 0
        }
        
        try:
            # 执行检查
            if self.config["checks"]["parallel_execution"] and len(check_types) > 1:
                results["check_results"] = self.run_checks_in_parallel(check_types)
            else:
                results["check_results"] = self.run_checks_sequentially(check_types)
            
            # 分析结果
            self.analyze_results(results)
            
            # 生成报告
            self.generate_reports(results)
            
            # 发送通知
            self.send_notifications(results)
            
            # 存储结果
            self.store_results(results)
            
        except Exception as e:
            logger.error(f"执行检查时发生错误: {e}")
            results["error"] = str(e)
            results["overall_status"] = "ERROR"
        
        # 计算执行时间
        end_time = datetime.now()
        results["end_time"] = end_time.isoformat()
        results["duration_seconds"] = (end_time - start_time).total_seconds()
        
        logger.info(f"质量检查执行完成，总体状态: {results['overall_status']}")
        logger.info(f"执行时间: {results['duration_seconds']:.2f}秒")
        
        return results
    
    def run_checks_in_parallel(self, check_types: List[str]) -> Dict[str, Dict]:
        """并行执行检查"""
        import concurrent.futures
        
        results = {}
        max_workers = min(len(check_types), 4)  # 最多4个并行
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
            # 提交检查任务
            future_to_check = {}
            for check_type in check_types:
                if check_type in self.check_modules:
                    future = executor.submit(self.execute_single_check, check_type)
                    future_to_check[future] = check_type
            
            # 收集结果
            for future in concurrent.futures.as_completed(future_to_check):
                check_type = future_to_check[future]
                try:
                    result = future.result(timeout=600)  # 10分钟超时
                    results[check_type] = result
                except Exception as e:
                    logger.error(f"检查 {check_type} 执行失败: {e}")
                    results[check_type] = {
                        "status": "ERROR",
                        "message": f"执行失败: {str(e)}",
                        "error": str(e)
                    }
        
        return results
    
    def run_checks_sequentially(self, check_types: List[str]) -> Dict[str, Dict]:
        """顺序执行检查"""
        results = {}
        
        for check_type in check_types:
            if check_type in self.check_modules:
                try:
                    result = self.execute_single_check(check_type)
                    results[check_type] = result
                except Exception as e:
                    logger.error(f"检查 {check_type} 执行失败: {e}")
                    results[check_type] = {
                        "status": "ERROR",
                        "message": f"执行失败: {str(e)}",
                        "error": str(e)
                    }
        
        return results
    
    def execute_single_check(self, check_type: str) -> Dict:
        """执行单个检查"""
        module_info = self.check_modules.get(check_type)
        if not module_info:
            return {
                "status": "SKIPPED",
                "message": f"检查类型 {check_type} 未配置或未启用"
            }
        
        script_path = module_info["script"]
        config_path = module_info["config"]
        
        if not os.path.exists(script_path):
            return {
                "status": "ERROR",
                "message": f"检查脚本不存在: {script_path}"
            }
        
        logger.info(f"执行检查: {module_info['description']}")
        
        try:
            # 构建命令
            cmd = [
                sys.executable,  # 使用当前Python解释器
                script_path,
                "--config", config_path,
                "--output", self.config["reporting"]["output_dir"]
            ]
            
            # 添加其他参数
            if check_type == "performance_check":
                cmd.extend(["--full"])  # 性能检查执行完整流程
            
            # 执行命令
            start_time = time.time()
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=self.config["checks"]["timeout_minutes"] * 60
            )
            duration = time.time() - start_time
            
            # 解析输出
            output = result.stdout
            error = result.stderr
            
            if result.returncode == 0:
                # 尝试从输出中解析JSON结果
                try:
                    # 查找JSON输出
                    json_start = output.find('{')
                    json_end = output.rfind('}') + 1
                    if json_start >= 0 and json_end > json_start:
                        json_output = output[json_start:json_end]
                        check_result = json.loads(json_output)
                        check_result["execution_time"] = duration
                        return check_result
                except json.JSONDecodeError:
                    pass
                
                # 如果无法解析JSON，返回基本结果
                return {
                    "status": "COMPLETED",
                    "message": "检查执行完成",
                    "execution_time": duration,
                    "return_code": result.returncode,
                    "output": output[:1000]  # 截断输出
                }
            else:
                return {
                    "status": "FAILED",
                    "message": f"检查执行失败，返回码: {result.returncode}",
                    "execution_time": duration,
                    "return_code": result.returncode,
                    "error": error,
                    "output": output[:1000]
                }
                
        except subprocess.TimeoutExpired:
            return {
                "status": "TIMEOUT",
                "message": f"检查执行超时 ({self.config['checks']['timeout_minutes']}分钟)",
                "execution_time": self.config["checks"]["timeout_minutes"] * 60
            }
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"检查执行异常: {str(e)}",
                "error": str(e),
                "execution_time": 0
            }
    
    def analyze_results(self, results: Dict):
        """分析检查结果"""
        check_results = results["check_results"]
        
        # 统计状态
        status_counts = {
            "COMPLETED": 0,
            "WITHIN_BASELINE": 0,
            "COMPLIANT": 0,
            "HEALTHY": 0,
            "WARNING": 0,
            "FAILED": 0,
            "ERROR": 0,
            "TIMEOUT": 0,
            "SKIPPED": 0
        }
        
        for check_type, result in check_results.items():
            status = result.get("status", "UNKNOWN")
            if status in status_counts:
                status_counts[status] += 1
            else:
                status_counts[status] = 1
        
        # 确定总体状态
        if status_counts.get("FAILED", 0) > 0 or status_counts.get("ERROR", 0) > 0:
            overall_status = "FAILED"
        elif status_counts.get("TIMEOUT", 0) > 0:
            overall_status = "TIMEOUT"
        elif status_counts.get("WARNING", 0) > 0:
            overall_status = "WARNING"
        elif all(status in ["COMPLETED", "WITHIN_BASELINE", "COMPLIANT", "HEALTHY"] 
                for status in [r.get("status", "") for r in check_results.values()]):
            overall_status = "HEALTHY"
        else:
            overall_status = "UNKNOWN"
        
        # 计算成功率
        total_checks = len(check_results)
        successful_checks = sum(status_counts.get(s, 0) for s in 
                              ["COMPLETED", "WITHIN_BASELINE", "COMPLIANT", "HEALTHY"])
        success_rate = (successful_checks / total_checks * 100) if total_checks > 0 else 0
        
        results["overall_status"] = overall_status
        results["summary"] = {
            "total_checks": total_checks,
            "status_counts": status_counts,
            "success_rate": round(success_rate, 2),
            "failed_checks": status_counts.get("FAILED", 0) + status_counts.get("ERROR", 0),
            "warning_checks": status_counts.get("WARNING", 0)
        }
    
    def generate_reports(self, results: Dict):
        """生成报告"""
        output_dir = self.config["reporting"]["output_dir"]
        os.makedirs(output_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        execution_id = results["execution_id"]
        
        # JSON报告
        json_report_path = os.path.join(output_dir, f"quality-report-{execution_id}.json")
        with open(json_report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        logger.info(f"生成JSON报告: {json_report_path}")
        
        # Markdown报告
        if self.config["reporting"]["consolidated_reports"]:
            markdown_report_path = os.path.join(output_dir, f"quality-report-{execution_id}.md")
            self.generate_markdown_report(markdown_report_path, results)
            logger.info(f"生成Markdown报告: {markdown_report_path}")
        
        # HTML仪表盘
        if self.config["reporting"]["generate_dashboard"]:
            dashboard_path = os.path.join(output_dir, "dashboard.html")
            self.generate_html_dashboard(dashboard_path, results)
            logger.info(f"生成HTML仪表盘: {dashboard_path}")
        
        # 更新最新报告链接
        latest_link = os.path.join(output_dir, "latest-report.json")
        if os.path.exists(latest_link):
            os.remove(latest_link)
        os.symlink(json_report_path, latest_link)
    
    def generate_markdown_report(self, filepath: str, results: Dict):
        """生成Markdown格式报告"""
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f"# 测试环境质量检查报告\n\n")
            f.write(f"**执行ID**: {results['execution_id']}\n")
            f.write(f"**执行时间**: {results['start_time']}\n")
            f.write(f"**持续时间**: {results['duration_seconds']:.2f}秒\n")
            f.write(f"**总体状态**: **{results['overall_status']}**\n\n")
            
            f.write(f"## 检查摘要\n")
            summary = results["summary"]
            f.write(f"- 总检查项: {summary['total_checks']}\n")
            f.write(f"- 成功率: {summary['success_rate']}%\n")
            f.write(f"- 失败项: {summary['failed_checks']}\n")
            f.write(f"- 警告项: {summary['warning_checks']}\n\n")
            
            f.write(f"## 详细检查结果\n")
            for check_type, result in results["check_results"].items():
                module_info = self.check_modules.get(check_type, {})
                check_name = module_info.get("description", check_type)
                
                status = result.get("status", "UNKNOWN")
                status_emoji = {
                    "COMPLETED": "✅",
                    "WITHIN_BASELINE": "✅",
                    "COMPLIANT": "✅",
                    "HEALTHY": "✅",
                    "WARNING": "⚠️",
                    "FAILED": "❌",
                    "ERROR": "💥",
                    "TIMEOUT": "⏰",
                    "SKIPPED": "⏭️"
                }.get(status, "❓")
                
                f.write(f"### {status_emoji} {check_name}\n")
                f.write(f"- **状态**: {status}\n")
                f.write(f"- **消息**: {result.get('message', '无消息')}\n")
                
                if "execution_time" in result:
                    f.write(f"- **执行时间**: {result['execution_time']:.2f}秒\n")
                
                if "details" in result:
                    f.write(f"- **详情**: {json.dumps(result['details'], indent=2)}\n")
                
                f.write("\n")
            
            f.write(f"## 建议措施\n")
            if results["overall_status"] == "HEALTHY":
                f.write("✅ 所有检查通过，测试环境质量良好\n")
                f.write("建议：继续保持当前配置和监控\n")
            elif results["overall_status"] == "WARNING":
                f.write("⚠️ 存在警告项，需要关注\n")
                f.write("建议：检查警告项详情，优化相关配置\n")
            elif results["overall_status"] == "FAILED":
                f.write("❌ 存在失败项，需要立即处理\n")
                f.write("建议：查看失败项详情，优先修复关键问题\n")
            else:
                f.write("🔍 状态未知，需要进一步检查\n")
                f.write("建议：查看详细日志，确认问题原因\n")
    
    def generate_html_dashboard(self, filepath: str, results: Dict):
        """生成HTML仪表盘"""
        html_template = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>测试环境质量检查仪表盘</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
                .header { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: bold; }
                .status-healthy { background: #4CAF50; color: white; }
                .status-warning { background: #FF9800; color: white; }
                .status-failed { background: #F44336; color: white; }
                .summary-cards { display: flex; flex-wrap: wrap; gap: 20px; margin: 20px 0; }
                .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); flex: 1; min-width: 200px; }
                .check-results { margin-top: 20px; }
                .check-item { background: white; padding: 15px; margin: 10px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .check-success { border-left: 4px solid #4CAF50; }
                .check-warning { border-left: 4px solid #FF9800; }
                .check-failed { border-left: 4px solid #F44336; }
                .progress-bar { height: 20px; background: #e0e0e0; border-radius: 10px; overflow: hidden; }
                .progress-fill { height: 100%; background: #4CAF50; }
                h1, h2, h3 { color: #333; }
                .timestamp { color: #666; font-size: 0.9em; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>测试环境质量检查仪表盘</h1>
                <p class="timestamp">更新时间: {timestamp}</p>
                <p>执行ID: {execution_id} | 总体状态: <span class="status-badge status-{overall_status_class}">{overall_status}</span></p>
            </div>
            
            <div class="summary-cards">
                <div class="card">
                    <h3>检查摘要</h3>
                    <p>总检查项: {total_checks}</p>
                    <p>成功率: {success_rate}%</p>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: {success_rate}%"></div>
                    </div>
                </div>
                
                <div class="card">
                    <h3>状态分布</h3>
                    <p>成功: {successful_checks}</p>
                    <p>失败: {failed_checks}</p>
                    <p>警告: {warning_checks}</p>
                </div>
                
                <div class="card">
                    <h3>执行信息</h3>
                    <p>开始时间: {start_time}</p>
                    <p>持续时间: {duration_seconds:.2f}秒</p>
                </div>
            </div>
            
            <h2>详细检查结果</h2>
            <div class="check-results">
                {check_results_html}
            </div>
            
            <script>
                // 自动刷新（每5分钟）
                setTimeout(function() {
                    location.reload();
                }, 5 * 60 * 1000);
            </script>
        </body>
        </html>
        """
        
        # 准备数据
        overall_status_class = "healthy" if results["overall_status"] == "HEALTHY" else \
                              "warning" if results["overall_status"] == "WARNING" else "failed"
        
        summary = results["summary"]
        success_rate = summary["success_rate"]
        total_checks = summary["total_checks"]
        failed_checks = summary["failed_checks"]
        warning_checks = summary["warning_checks"]
        successful_checks = total_checks - failed_checks - warning_checks
        
        # 生成检查结果HTML
        check_results_html = ""
        for check_type, result in results["check_results"].items():
            module_info = self.check_modules.get(check_type, {})
            check_name = module_info.get("description", check_type)
            
            status = result.get("status", "UNKNOWN")
            status_class = "success" if status in ["COMPLETED", "WITHIN_BASELINE", "COMPLIANT", "HEALTHY"] else \
                          "warning" if status == "WARNING" else "failed"
            
            check_results_html += f"""
            <div class="check-item check-{status_class}">
                <h3>{check_name}</h3>
                <p><strong>状态:</strong> {status}</p>
                <p><strong>消息:</strong> {result.get('message', '无消息')}</p>
                {f"<p><strong>执行时间:</strong> {result.get('execution_time', 0):.2f}秒</p>" if 'execution_time' in result else ''}
            </div>
            """
        
        # 填充模板
        html_content = html_template.format(
            timestamp=datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            execution_id=results["execution_id"],
            overall_status=results["overall_status"],
            overall_status_class=overall_status_class,
            total_checks=total_checks,
            success_rate=success_rate,
            successful_checks=successful_checks,
            failed_checks=failed_checks,
            warning_checks=warning_checks,
            start_time=results["start_time"],
            duration_seconds=results["duration_seconds"],
            check_results_html=check_results_html
        )
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(html_content)
    
    def send_notifications(self, results: Dict):
        """发送通知"""
        notification_config = self.config["reporting"]["notification"]
        
        if not notification_config["enabled"]:
            return
        
        # 检查是否满足发送条件
        overall_status = results["overall_status"]
        send_notification = False
        
        if overall_status == "FAILED" and notification_config.get("on_failure", True):
            send_notification = True
            subject = f"🚨 测试环境质量检查失败 - {results['execution_id']}"
            priority = "high"
        elif overall_status == "WARNING" and notification_config.get("on_warning", True):
            send_notification = True
            subject = f"⚠️ 测试环境质量检查警告 - {results['execution_id']}"
            priority = "medium"
        else:
            # 每日摘要
            if notification_config.get("daily_summary", True):
                now = datetime.now()
                if now.hour == 9 and now.minute < 5:  # 每天9点发送摘要
                    send_notification = True
                    subject = f"📊 测试环境质量检查每日摘要 - {now.strftime('%Y-%m-%d')}"
                    priority = "low"
        
        if not send_notification:
            return
        
        # 构建通知内容
        message = self.build_notification_message(results, subject)
        
        # 发送到各个渠道
        channels = notification_config.get("channels", ["log"])
        
        for channel in channels:
            try:
                if channel == "email":
                    self.send_email_notification(subject, message, priority)
                elif channel == "log":
                    self.send_log_notification(subject, message)
                elif channel == "slack":
                    self.send_slack_notification(subject, message, priority)
                elif channel == "dingtalk":
                    self.send_dingtalk_notification(subject, message, priority)
            except Exception as e:
                logger.error(f"发送 {channel} 通知失败: {e}")
    
    def build_notification_message(self, results: Dict, subject: str) -> str:
        """构建通知消息"""
        summary = results["summary"]
        
        message = f"{subject}\n\n"
        message += f"执行时间: {results['start_time']}\n"
        message += f"总体状态: {results['overall_status']}\n"
        message += f"检查项数: {summary['total_checks']}\n"
        message += f"成功率: {summary['success_rate']}%\n"
        message += f"失败项: {summary['failed_checks']}\n"
        message += f"警告项: {summary['warning_checks']}\n\n"
        
        message += "详细结果:\n"
        for check_type, result in results["check_results"].items():
            module_info = self.check_modules.get(check_type, {})
            check_name = module_info.get("description", check_type)
            status = result.get("status", "UNKNOWN")
            status_icon = "✅" if status in ["COMPLETED", "WITHIN_BASELINE", "COMPLIANT", "HEALTHY"] else \
                         "⚠️" if status == "WARNING" else "❌"
            
            message += f"{status_icon} {check_name}: {status}\n"
            if result.get("message"):
                message += f"   消息: {result['message']}\n"
        
        message += f"\n查看详细报告: {self.config['reporting']['output_dir']}/latest-report.json"
        
        return message
    
    def send_email_notification(self, subject: str, message: str, priority: str = "normal"):
        """发送邮件通知"""
        email_config = self.config["reporting"]["notification"]["email"]
        
        if not email_config:
            return
        
        try:
            # 构建邮件
            msg = MIMEMultipart()
            msg['From'] = email_config["sender"]
            msg['To'] = ', '.join(email_config["recipients"])
            msg['Subject'] = subject
            msg['X-Priority'] = '1' if priority == "high" else '3' if priority == "low" else '2'
            
            # 添加文本内容
            msg.attach(MIMEText(message, 'plain', 'utf-8'))
            
            # 发送邮件
            with smtplib.SMTP(email_config["smtp_server"], email_config["smtp_port"]) as server:
                server.starttls()
                # 如果需要认证
                # server.login(username, password)
                server.send_message(msg)
            
            logger.info(f"邮件通知已发送给 {len(email_config['recipients'])} 个收件人")
            
        except Exception as e:
            logger.error(f"发送邮件通知失败: {e}")
    
    def send_log_notification(self, subject: str, message: str):
        """发送日志通知"""
        logger.info(f"通知: {subject}")
        logger.info(f"消息内容: {message}")
    
    def send_slack_notification(self, subject: str, message: str, priority: str):
        """发送Slack通知（待实现）"""
        # TODO: 实现Slack Webhook集成
        pass
    
    def send_dingtalk_notification(self, subject: str, message: str, priority: str):
        """发送钉钉通知（待实现）"""
        # TODO: 实现钉钉机器人集成
        pass
    
    def store_results(self, results: Dict):
        """存储检查结果"""
        self.results_history.append(results)
        
        # 限制历史记录大小
        if len(self.results_history) > self.max_history_size:
            self.results_history = self.results_history[-self.max_history_size:]
        
        # 保存到文件
        history_file = os.path.join(self.config["reporting"]["output_dir"], "execution-history.json")
        try:
            with open(history_file, 'w', encoding='utf-8') as f:
                json.dump(self.results_history, f, indent=2, ensure_ascii=False)
        except Exception as e:
            logger.error(f"保存执行历史失败: {e}")
    
    def start_scheduler(self):
        """启动调度器"""
        if not self.config["scheduler"]["enabled"]:
            logger.info("调度器已禁用")
            return
        
        scheduler_config = self.config["scheduler"]
        mode = scheduler_config["mode"]
        
        logger.info(f"启动自动化调度器，模式: {mode}")
        
        if mode == "cron":
            self.start_cron_scheduler()
        elif mode == "interval":
            self.start_interval_scheduler()
        elif mode == "manual":
            logger.info("手动模式，不会自动调度")
            return
        else:
            logger.error(f"不支持的调度模式: {mode}")
            return
        
        self.is_running = True
        
        # 启动调度线程
        self.scheduler_thread = threading.Thread(target=self._scheduler_loop)
        self.scheduler_thread.daemon = True
        self.scheduler_thread.start()
        
        logger.info("自动化调度器已启动")
    
    def start_cron_scheduler(self):
        """启动Cron调度器"""
        cron_expression = self.config["scheduler"]["cron_expression"]
        
        # 解析Cron表达式
        # 这里简化处理，实际应该使用croniter或类似库
        # 暂时使用schedule库的简化版本
        
        import schedule
        schedule.every(2).hours.do(self.run_checks)  # 简化：每2小时
        
        logger.info(f"配置Cron调度: {cron_expression}")
    
    def start_interval_scheduler(self):
        """启动间隔调度器"""
        interval_minutes = self.config["scheduler"]["check_interval_minutes"]
        
        import schedule
        schedule.every(interval_minutes).minutes.do(self.run_checks)
        
        logger.info(f"配置间隔调度: 每{interval_minutes}分钟")
    
    def _scheduler_loop(self):
        """调度器主循环"""
        import schedule
        import time
        
        while self.is_running:
            try:
                schedule.run_pending()
                time.sleep(60)  # 每分钟检查一次
            except Exception as e:
                logger.error(f"调度器循环错误: {e}")
                time.sleep(60)
    
    def stop_scheduler(self):
        """停止调度器"""
        self.is_running = False
        if self.scheduler_thread:
            self.scheduler_thread.join(timeout=10)
        
        logger.info("自动化调度器已停止")
    
    def get_execution_history(self, limit: int = 10) -> List[Dict]:
        """获取执行历史"""
        return self.results_history[-limit:] if self.results_history else []
    
    def get_status_summary(self) -> Dict:
        """获取状态摘要"""
        if not self.results_history:
            return {"status": "NO_HISTORY", "message": "无执行历史"}
        
        latest_result = self.results_history[-1]
        
        return {
            "latest_execution": latest_result["execution_id"],
            "latest_status": latest_result["overall_status"],
            "latest_time": latest_result["start_time"],
            "total_executions": len(self.results_history),
