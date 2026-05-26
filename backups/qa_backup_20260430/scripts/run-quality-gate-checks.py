#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1质量门禁检查执行脚本
版本: 1.0
创建日期: 2026-04-27
作者: qa-lead
描述: 执行完整的质量门禁检查流程
"""

import os
import sys
import json
import subprocess
import time
import argparse
from datetime import datetime
from typing import Dict, List, Any, Optional
import shutil

class QualityGateExecutor:
    """质量门禁检查执行器"""
    
    def __init__(self):
        self.check_scripts = {
            "server_resources": "quality-gate-check-server-resources.py",
            "os_security": "quality-gate-check-os-security.py",
            "network_connectivity": "quality-gate-check-network-connectivity.py",
            "runtime_versions": "quality-gate-check-runtime-versions.py",
            "database_services": "quality-gate-check-database-services.py",
            "service_status": "quality-gate-check-service-status.py",
            "core_functionality": "quality-gate-test-core-functionality.py"
        }
        
        self.results_dir = "quality-gate-results"
        self.reports_dir = os.path.join(self.results_dir, "reports")
        self.summary_file = os.path.join(self.results_dir, "quality-gate-summary.json")
        
        # 创建结果目录
        os.makedirs(self.reports_dir, exist_ok=True)
        
        self.execution_results = {
            "execution_id": datetime.now().strftime("%Y%m%d_%H%M%S"),
            "start_time": datetime.now().isoformat(),
            "end_time": None,
            "environment": self.get_environment_info(),
            "checks": [],
            "summary": {
                "total_checks": 0,
                "completed": 0,
                "passed": 0,
                "failed": 0,
                "skipped": 0,
                "warning": 0
            },
            "overall_status": "NOT_STARTED"
        }
    
    def get_environment_info(self) -> Dict[str, Any]:
        """获取环境信息"""
        import platform
        import socket
        
        return {
            "hostname": socket.gethostname(),
            "platform": platform.system(),
            "platform_release": platform.release(),
            "platform_version": platform.version(),
            "architecture": platform.machine(),
            "python_version": platform.python_version(),
            "working_directory": os.getcwd(),
            "execution_time": datetime.now().isoformat()
        }
    
    def check_script_exists(self, script_name: str) -> bool:
        """检查脚本是否存在"""
        script_path = os.path.join(os.path.dirname(__file__), script_name)
        return os.path.exists(script_path)
    
    def run_check_script(self, check_name: str, script_name: str, args: List[str] = None) -> Dict[str, Any]:
        """运行检查脚本"""
        if args is None:
            args = []
        
        script_path = os.path.join(os.path.dirname(__file__), script_name)
        
        if not os.path.exists(script_path):
            return {
                "check_name": check_name,
                "script_name": script_name,
                "status": "SKIPPED",
                "error": f"脚本不存在: {script_path}",
                "start_time": datetime.now().isoformat(),
                "end_time": datetime.now().isoformat(),
                "duration_seconds": 0
            }
        
        try:
            # 构建命令
            cmd = [sys.executable, script_path] + args
            
            # 生成报告文件名
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            report_file = os.path.join(self.reports_dir, f"{check_name}_{timestamp}.json")
            
            # 添加输出文件参数（如果脚本支持）
            if "--output" not in args and "-o" not in args:
                cmd.extend(["--output", report_file])
            
            start_time = time.time()
            print(f"开始执行检查: {check_name}")
            print(f"命令: {' '.join(cmd)}")
            
            # 执行命令
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300  # 5分钟超时
            )
            
            end_time = time.time()
            duration = end_time - start_time
            
            # 解析结果
            check_result = {
                "check_name": check_name,
                "script_name": script_name,
                "command": ' '.join(cmd),
                "status": "COMPLETED",
                "return_code": result.returncode,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "start_time": datetime.fromtimestamp(start_time).isoformat(),
                "end_time": datetime.fromtimestamp(end_time).isoformat(),
                "duration_seconds": duration,
                "report_file": report_file if os.path.exists(report_file) else None
            }
            
            # 根据返回码判断检查结果
            if result.returncode == 0:
                check_result["check_status"] = "PASSED"
                print(f"✅ 检查通过: {check_name} (耗时: {duration:.2f}秒)")
            elif result.returncode == 1:
                check_result["check_status"] = "FAILED"
                print(f"❌ 检查失败: {check_name} (耗时: {duration:.2f}秒)")
            else:
                check_result["check_status"] = "ERROR"
                print(f"⚠️ 检查错误: {check_name} (返回码: {result.returncode}, 耗时: {duration:.2f}秒)")
            
            # 保存详细输出
            log_file = os.path.join(self.reports_dir, f"{check_name}_{timestamp}.log")
            with open(log_file, 'w', encoding='utf-8') as f:
                f.write(f"=== 检查: {check_name} ===\n")
                f.write(f"开始时间: {check_result['start_time']}\n")
                f.write(f"结束时间: {check_result['end_time']}\n")
                f.write(f"耗时: {duration:.2f}秒\n")
                f.write(f"返回码: {result.returncode}\n")
                f.write(f"状态: {check_result['check_status']}\n\n")
                f.write("=== 标准输出 ===\n")
                f.write(result.stdout)
                f.write("\n\n=== 标准错误 ===\n")
                f.write(result.stderr)
            
            check_result["log_file"] = log_file
            
            return check_result
            
        except subprocess.TimeoutExpired:
            end_time = time.time()
            return {
                "check_name": check_name,
                "script_name": script_name,
                "status": "TIMEOUT",
                "error": "检查超时 (300秒)",
                "start_time": datetime.fromtimestamp(start_time).isoformat(),
                "end_time": datetime.fromtimestamp(end_time).isoformat(),
                "duration_seconds": end_time - start_time
            }
        except Exception as e:
            return {
                "check_name": check_name,
                "script_name": script_name,
                "status": "ERROR",
                "error": str(e),
                "start_time": datetime.now().isoformat(),
                "end_time": datetime.now().isoformat(),
                "duration_seconds": 0
            }
    
    def run_checks(self, check_list: List[str] = None, skip_list: List[str] = None) -> Dict[str, Any]:
        """运行指定的检查列表"""
        if check_list is None:
            check_list = list(self.check_scripts.keys())
        
        if skip_list is None:
            skip_list = []
        
        print("=" * 70)
        print("Sprint 27+1 质量门禁检查")
        print("=" * 70)
        print(f"执行ID: {self.execution_results['execution_id']}")
        print(f"开始时间: {self.execution_results['start_time']}")
        print(f"环境: {self.execution_results['environment']['hostname']}")
        print(f"工作目录: {self.execution_results['environment']['working_directory']}")
        print(f"检查数量: {len(check_list)}")
        print("=" * 70)
        
        # 执行检查
        for check_name in check_list:
            if check_name in skip_list:
                print(f"跳过检查: {check_name}")
                continue
                
            if check_name not in self.check_scripts:
                print(f"未知检查: {check_name}")
                continue
                
            script_name = self.check_scripts[check_name]
            result = self.run_check_script(check_name, script_name)
            
            # 更新统计信息
            self.execution_results["summary"]["total_checks"] += 1
            
            if result["status"] == "COMPLETED":
                self.execution_results["summary"]["completed"] += 1
                if result.get("check_status") == "PASSED":
                    self.execution_results["summary"]["passed"] += 1
                elif result.get("check_status") == "FAILED":
                    self.execution_results["summary"]["failed"] += 1
                else:
                    self.execution_results["summary"]["warning"] += 1
            elif result["status"] == "SKIPPED":
                self.execution_results["summary"]["skipped"] += 1
            else:
                self.execution_results["summary"]["warning"] += 1
            
            self.execution_results["checks"].append(result)
        
        # 完成执行
        self.execution_results["end_time"] = datetime.now().isoformat()
        
        # 计算总体状态
        if self.execution_results["summary"]["failed"] > 0:
            self.execution_results["overall_status"] = "FAILED"
        elif self.execution_results["summary"]["warning"] > 0:
            self.execution_results["overall_status"] = "WARNING"
        elif self.execution_results["summary"]["passed"] == self.execution_results["summary"]["total_checks"]:
            self.execution_results["overall_status"] = "PASSED"
        else:
            self.execution_results["overall_status"] = "PARTIAL"
        
        return self.execution_results
    
    def generate_summary_report(self):
        """生成摘要报告"""
        summary = self.execution_results["summary"]
        total = summary["total_checks"]
        
        print("\n" + "=" * 70)
        print("质量门禁检查摘要")
        print("=" * 70)
        
        print(f"执行ID: {self.execution_results['execution_id']}")
        print(f"开始时间: {self.execution_results['start_time']}")
        print(f"结束时间: {self.execution_results['end_time']}")
        
        if self.execution_results["end_time"] and self.execution_results["start_time"]:
            start = datetime.fromisoformat(self.execution_results["start_time"])
            end = datetime.fromisoformat(self.execution_results["end_time"])
            duration = (end - start).total_seconds()
            print(f"总耗时: {duration:.2f}秒")
        
        print("\n检查统计:")
        print(f"  总检查项: {total}")
        print(f"  已完成: {summary['completed']}")
        print(f"  已跳过: {summary['skipped']}")
        print(f"  通过: {summary['passed']}")
        print(f"  失败: {summary['failed']}")
        print(f"  警告: {summary['warning']}")
        
        if total > 0:
            completion_rate = summary['completed'] / total * 100
            pass_rate = summary['passed'] / summary['completed'] * 100 if summary['completed'] > 0 else 0
            print(f"\n完成率: {completion_rate:.1f}%")
            print(f"通过率: {pass_rate:.1f}%")
        
        print(f"\n总体状态: {self.execution_results['overall_status']}")
        
        # 显示状态图标
        if self.execution_results["overall_status"] == "PASSED":
            print("\n✅ 所有质量门禁检查通过")
            print("   测试环境符合Sprint 27+1质量要求")
        elif self.execution_results["overall_status"] == "WARNING":
            print("\n⚠️ 质量门禁检查有警告")
            print("   测试环境基本符合要求，但需要关注警告项")
        elif self.execution_results["overall_status"] == "FAILED":
            print("\n❌ 质量门禁检查失败")
            print("   测试环境不符合Sprint 27+1质量要求")
            print("   需要修复失败项后重新检查")
        else:
            print("\nℹ️ 质量门禁检查部分完成")
            print("   需要检查未完成的检查项")
        
        print("\n详细报告:")
        print(f"  摘要文件: {self.summary_file}")
        print(f"  报告目录: {self.reports_dir}")
        
        # 列出检查结果
        print("\n检查结果详情:")
        for check in self.execution_results["checks"]:
            status_icon = "✅" if check.get("check_status") == "PASSED" else \
                         "❌" if check.get("check_status") == "FAILED" else \
                         "⚠️" if check.get("status") in ["WARNING", "ERROR"] else \
                         "⏭️" if check.get("status") == "SKIPPED" else "❓"
            
            print(f"  {status_icon} {check['check_name']}: {check.get('check_status', check['status'])}")
            if check.get("duration_seconds"):
                print(f"     耗时: {check['duration_seconds']:.2f}秒")
            if check.get("error"):
                print(f"     错误: {check['error']}")
        
        print("=" * 70)
    
    def save_summary(self):
        """保存摘要到文件"""
        with open(self.summary_file, 'w', encoding='utf-8') as f:
            json.dump(self.execution_results, f, ensure_ascii=False, indent=2)
        
        print(f"质量门禁检查摘要已保存到: {self.summary_file}")
    
    def generate_html_report(self):
        """生成HTML报告"""
        try:
            html_file = os.path.join(self.results_dir, "quality-gate-report.html")
            
            html_content = f"""
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sprint 27+1 质量门禁检查报告</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: Arial, sans-serif; line-height: 1.6; color: #333; background-color: #f5f5f5; }}
        .container {{ max-width: 1200px; margin: 0 auto; padding: 20px; }}
        .header {{ background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 10px; margin-bottom: 30px; }}
        .header h1 {{ font-size: 2.5em; margin-bottom: 10px; }}
        .header .subtitle {{ font-size: 1.2em; opacity: 0.9; }}
        .summary-card {{ background: white; border-radius: 10px; padding: 25px; margin-bottom: 30px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }}
        .summary-card h2 {{ color: #333; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 2px solid #f0f0f0; }}
        .stats {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }}
        .stat-item {{ text-align: center; padding: 20px; border-radius: 8px; }}
        .stat-item.passed {{ background: #d4edda; color: #155724; }}
        .stat-item.failed {{ background: #f8d7da; color: #721c24; }}
        .stat-item.warning {{ background: #fff3cd; color: #856404; }}
        .stat-item.skipped {{ background: #e2e3e5; color: #383d41; }}
        .stat-value {{ font-size: 2.5em; font-weight: bold; margin-bottom: 5px; }}
        .stat-label {{ font-size: 1em; }}
        .overall-status {{ text-align: center; padding: 20px; margin: 20px 0; border-radius: 8px; font-size: 1.5em; font-weight: bold; }}
        .overall-status.passed {{ background: #d4edda; color: #155724; }}
        .overall-status.failed {{ background: #f8d7da; color: #721c24; }}
        .overall-status.warning {{ background: #fff3cd; color: #856404; }}
        .check-results {{ background: white; border-radius: 10px; padding: 25px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }}
        .check-results h2 {{ color: #333; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 2px solid #f0f0f0; }}
        .check-table {{ width: 100%; border-collapse: collapse; }}
        .check-table th, .check-table td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #ddd; }}
        .check-table th {{ background-color: #f8f9fa; font-weight: bold; }}
        .check-table tr:hover {{ background-color: #f5f5f5; }}
        .status-badge {{ padding: 4px 8px; border-radius: 4px; font-size: 0.9em; font-weight: bold; }}
        .status-passed {{ background: #d4edda; color: #155724; }}
        .status-failed {{ background: #f8d7da; color: #721c24; }}
        .status-warning {{ background: #fff3cd; color: #856404; }}
        .status-skipped {{ background: #e2e3e5; color: #383d41; }}
        .status-error {{ background: #f8d7da; color: #721c24; }}
        .footer {{ margin-top: 40px; text-align: center; color: #666; font-size: 0.9em; padding: 20px; border-top: 1px solid #ddd; }}
        .timestamp {{ color: #999; font-size: 0.8em; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📊 Sprint 27+1 质量门禁检查报告</h1>
            <div class="subtitle">执行ID: {self.execution_results['execution_id']}</div>
            <div class="subtitle">测试环境质量门禁检查</div>
        </div>
        
        <div class="summary-card">
            <h2>📈 检查概览</h2>
            <div class="stats">
                <div class="stat-item passed">
                    <div class="stat-value">{self.execution_results['summary']['passed']}</div>
                    <div class="stat-label">通过</div>
                </div>
                <div class="stat-item failed">
                    <div class="stat-value">{self.execution_results['summary']['failed']}</div>
                    <div class="stat-label">失败</div>
                </div>
                <div class="stat-item warning">
                    <div class="stat-value">{self.execution_results['summary']['warning']}</div>
                    <div class="stat-label">警告</div>
                </div>
                <div class="stat-item skipped">
                    <div class="stat-value">{self.execution_results['summary']['skipped']}</div>
                    <div class="stat-label">跳过</div>
                </div>
            </div>
            
            <div class="overall-status {self.execution_results['overall_status'].lower()}">
                总体状态: {self.execution_results['overall_status']}
            </div>
            
            <div class="environment-info">
                <h3>环境信息</h3>
                <p><strong>主机名:</strong> {self.execution_results['environment']['hostname']}</p>
                <p><strong>平台:</strong> {self.execution_results['environment']['platform']} {self.execution_results['environment']['platform_release']}</p>
                <p><strong>架构:</strong> {self.execution_results['environment']['architecture']}</p>
                <p><strong>开始时间:</strong> {self.execution_results['start_time']}</p>
                <p><strong>结束时间:</strong> {self.execution_results['end_time']}</p>
            </div>
        </div>
        
        <div class="check-results">
            <h2>📋 检查结果详情</h2>
            <table class="check-table">
                <thead>
                    <tr>
                        <th>检查名称</th>
                        <th>状态</th>
                        <th>耗时(秒)</th>
                        <th>开始时间</th>
                        <th>详细报告</th>
                    </tr>
                </thead>
                <tbody>
            """
            
            for check in self.execution_results["checks"]:
                status_class = ""
                status_text = check.get("check_status", check.get("status", "UNKNOWN"))
                
                if status_text == "PASSED":
                    status_class = "status-passed"
                elif status_text == "FAILED":
                    status_class = "status-failed"
                elif status_text in ["WARNING", "ERROR"]:
                    status_class = "status-warning"
                elif status_text == "SKIPPED":
                    status_class = "status-skipped"
                else:
                    status_class = "status-error"
                
                duration = check.get("duration_seconds", 0)
                report_link = f"<a href='reports/{os.path.basename(check.get('log_file', ''))}' target='_blank'>查看日志</a>" if check.get("log_file") else "-"
                
                html_content += f"""
                    <tr>
                        <td><strong>{check['check_name']}</strong></td>
                        <td><span class="status-badge {status_class}">{status_text}</span></td>
                        <td>{duration:.2f}</td>
                        <td>{check.get('start_time', '-')}</td>
                        <td>{report_link}</td>
                    </tr>
                """
            
            html_content += """
                </tbody>
            </table>
        </div>
        
        <div class="footer">
            <p>生成时间: <span class="timestamp">""" + datetime.now().isoformat() + """</span></p>
            <p>© 2026 AI-Ready项目 - Sprint 27+1 质量保障团队</p>
        </div>
    </div>
</body>
</html>
            """
            
            with open(html_file, 'w', encoding='utf-8') as f:
                f.write(html_content)
            
            print(f"HTML报告已生成: {html_file}")
            return html_file
            
        except Exception as e:
            print(f"生成HTML报告时出错: {e}")
            return None

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Sprint 27+1质量门禁检查执行脚本')
    parser.add_argument('--checks', nargs='+', help='指定要执行的检查名称')
    parser.add_argument('--skip', nargs='+', help='指定要跳过的检查名称')
    parser.add_argument('--all', action='store_true', help='执行所有检查')
    parser.add_argument('--html', action='store_true', help='生成HTML报告')
    parser.add_argument('--quick', action='store_true', help='快速检查模式（只执行关键检查）')
    
    args = parser.parse_args()
    
    # 确定要执行的检查
    executor = QualityGateExecutor()
    
    if args.checks:
        check_list = args.checks
    elif args.quick:
        # 快速检查模式：只执行关键检查
        check_list = ["server_resources", "service_status", "core_functionality"]
    elif args.all:
        check_list = list(executor.check_scripts.keys())
    else:
        # 默认执行所有检查
        check_list = list(executor.check_scripts.keys())
    
    # 运行检查
    try:
        results = executor.run_checks(check_list, args.skip)
        
        # 生成报告
        executor.generate_summary_report()
        executor.save_summary()
        
        if args.html:
            executor.generate_html_report()
        
        # 根据总体状态返回退出码
        if results["overall_status"] == "PASSED":
            print("\n🎉 质量门禁检查通过！测试环境符合Sprint 27+1要求。")
            sys.exit(0)
        elif results["overall_status"] == "WARNING":
            print("\n⚠️ 质量门禁检查有警告，请查看详细报告。")
            sys.exit(1)
        else:
            print("\n❌ 质量门禁检查失败，请修复问题后重新检查。")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n检查被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"执行质量门禁检查时出错: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()