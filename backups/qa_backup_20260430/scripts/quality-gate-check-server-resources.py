#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sprint 27+1质量门禁检查脚本 - 服务器资源检查
版本: 1.0
创建日期: 2026-04-27
作者: qa-lead
"""

import os
import sys
import json
import platform
import psutil
import subprocess
import time
from datetime import datetime
from typing import Dict, List, Any, Tuple

class ServerResourceChecker:
    """服务器资源检查器"""
    
    def __init__(self):
        self.check_results = {
            "check_time": datetime.now().isoformat(),
            "check_type": "server_resources",
            "results": [],
            "summary": {
                "total_checks": 0,
                "passed": 0,
                "failed": 0,
                "warning": 0
            },
            "system_info": {}
        }
        
    def get_system_info(self) -> Dict[str, Any]:
        """获取系统信息"""
        info = {
            "platform": platform.system(),
            "platform_release": platform.release(),
            "platform_version": platform.version(),
            "architecture": platform.machine(),
            "processor": platform.processor(),
            "hostname": platform.node()
        }
        return info
    
    def check_cpu_cores(self) -> Dict[str, Any]:
        """检查CPU核心数"""
        check_id = "cpu_cores"
        requirement = "≥ 8核"
        
        try:
            cpu_count = psutil.cpu_count(logical=True)
            physical_cores = psutil.cpu_count(logical=False)
            
            passed = cpu_count >= 8
            status = "PASSED" if passed else "FAILED"
            
            result = {
                "check_id": check_id,
                "check_name": "CPU核心数检查",
                "requirement": requirement,
                "actual_value": f"{cpu_count}核 (物理核心: {physical_cores})",
                "status": status,
                "passed": passed,
                "details": {
                    "logical_cores": cpu_count,
                    "physical_cores": physical_cores
                }
            }
            
            return result
            
        except Exception as e:
            return {
                "check_id": check_id,
                "check_name": "CPU核心数检查",
                "requirement": requirement,
                "actual_value": "检查失败",
                "status": "FAILED",
                "passed": False,
                "error": str(e)
            }
    
    def check_memory(self) -> Dict[str, Any]:
        """检查内存容量"""
        check_id = "memory_capacity"
        requirement = "≥ 32GB"
        
        try:
            memory = psutil.virtual_memory()
            total_gb = memory.total / (1024**3)
            
            passed = total_gb >= 32
            status = "PASSED" if passed else "FAILED"
            
            result = {
                "check_id": check_id,
                "check_name": "内存容量检查",
                "requirement": requirement,
                "actual_value": f"{total_gb:.2f} GB",
                "status": status,
                "passed": passed,
                "details": {
                    "total_bytes": memory.total,
                    "total_gb": total_gb,
                    "available_gb": memory.available / (1024**3),
                    "percent_used": memory.percent
                }
            }
            
            return result
            
        except Exception as e:
            return {
                "check_id": check_id,
                "check_name": "内存容量检查",
                "requirement": requirement,
                "actual_value": "检查失败",
                "status": "FAILED",
                "passed": False,
                "error": str(e)
            }
    
    def check_disk_space(self) -> List[Dict[str, Any]]:
        """检查磁盘空间"""
        results = []
        
        # 检查系统盘
        system_disk_check = {
            "check_id": "system_disk_space",
            "check_name": "系统盘空间检查",
            "requirement": "≥ 200GB"
        }
        
        # 检查数据盘
        data_disk_check = {
            "check_id": "data_disk_space",
            "check_name": "数据盘空间检查",
            "requirement": "≥ 500GB"
        }
        
        try:
            partitions = psutil.disk_partitions(all=False)
            
            for partition in partitions:
                if platform.system() == "Windows":
                    # Windows系统
                    if partition.device == "C:\\":
                        usage = psutil.disk_usage(partition.mountpoint)
                        free_gb = usage.free / (1024**3)
                        
                        passed = free_gb >= 200
                        system_disk_check.update({
                            "actual_value": f"{free_gb:.2f} GB可用",
                            "status": "PASSED" if passed else "FAILED",
                            "passed": passed,
                            "details": {
                                "mountpoint": partition.mountpoint,
                                "total_gb": usage.total / (1024**3),
                                "used_gb": usage.used / (1024**3),
                                "free_gb": free_gb,
                                "percent_used": usage.percent
                            }
                        })
                        
                    elif partition.device in ["D:\\", "E:\\", "F:\\"]:
                        # 假设数据盘是D/E/F盘
                        usage = psutil.disk_usage(partition.mountpoint)
                        free_gb = usage.free / (1024**3)
                        
                        passed = free_gb >= 500
                        data_disk_check.update({
                            "actual_value": f"{free_gb:.2f} GB可用",
                            "status": "PASSED" if passed else "FAILED",
                            "passed": passed,
                            "details": {
                                "mountpoint": partition.mountpoint,
                                "total_gb": usage.total / (1024**3),
                                "used_gb": usage.used / (1024**3),
                                "free_gb": free_gb,
                                "percent_used": usage.percent
                            }
                        })
                        
                else:
                    # Linux/Unix系统
                    if partition.mountpoint == "/":
                        usage = psutil.disk_usage(partition.mountpoint)
                        free_gb = usage.free / (1024**3)
                        
                        passed = free_gb >= 200
                        system_disk_check.update({
                            "actual_value": f"{free_gb:.2f} GB可用",
                            "status": "PASSED" if passed else "FAILED",
                            "passed": passed,
                            "details": {
                                "mountpoint": partition.mountpoint,
                                "total_gb": usage.total / (1024**3),
                                "used_gb": usage.used / (1024**3),
                                "free_gb": free_gb,
                                "percent_used": usage.percent
                            }
                        })
                        
                    elif partition.mountpoint in ["/data", "/mnt/data", "/var"]:
                        usage = psutil.disk_usage(partition.mountpoint)
                        free_gb = usage.free / (1024**3)
                        
                        passed = free_gb >= 500
                        data_disk_check.update({
                            "actual_value": f"{free_gb:.2f} GB可用",
                            "status": "PASSED" if passed else "FAILED",
                            "passed": passed,
                            "details": {
                                "mountpoint": partition.mountpoint,
                                "total_gb": usage.total / (1024**3),
                                "used_gb": usage.used / (1024**3),
                                "free_gb": free_gb,
                                "percent_used": usage.percent
                            }
                        })
            
            # 如果没有找到数据盘，标记为失败
            if "actual_value" not in data_disk_check:
                data_disk_check.update({
                    "actual_value": "未找到数据盘",
                    "status": "FAILED",
                    "passed": False,
                    "details": {"error": "未找到符合要求的数据盘"}
                })
            
            results.append(system_disk_check)
            results.append(data_disk_check)
            
        except Exception as e:
            system_disk_check.update({
                "actual_value": "检查失败",
                "status": "FAILED",
                "passed": False,
                "error": str(e)
            })
            data_disk_check.update({
                "actual_value": "检查失败",
                "status": "FAILED",
                "passed": False,
                "error": str(e)
            })
            results.append(system_disk_check)
            results.append(data_disk_check)
        
        return results
    
    def check_network_bandwidth(self) -> Dict[str, Any]:
        """检查网络带宽（简化版）"""
        check_id = "network_bandwidth"
        requirement = "≥ 100Mbps"
        
        try:
            # 这里使用简单的网络测试，实际生产环境可能需要更复杂的测试
            import socket
            import time
            
            # 测试连接到公共DNS服务器
            test_host = "8.8.8.8"
            test_port = 53
            
            start_time = time.time()
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(5)
                sock.connect((test_host, test_port))
                sock.close()
                latency = (time.time() - start_time) * 1000  # 转换为毫秒
            except:
                latency = 999  # 连接失败
            
            # 简化检查：假设延迟小于100ms表示网络良好
            # 实际应该进行带宽测试
            passed = latency < 100
            status = "PASSED" if passed else "WARNING"
            
            result = {
                "check_id": check_id,
                "check_name": "网络带宽检查",
                "requirement": requirement,
                "actual_value": f"延迟: {latency:.2f}ms (简化检查)",
                "status": status,
                "passed": passed,
                "details": {
                    "test_host": test_host,
                    "test_port": test_port,
                    "latency_ms": latency,
                    "note": "这是简化检查，建议使用专业工具测试实际带宽"
                }
            }
            
            return result
            
        except Exception as e:
            return {
                "check_id": check_id,
                "check_name": "网络带宽检查",
                "requirement": requirement,
                "actual_value": "检查失败",
                "status": "WARNING",
                "passed": False,
                "error": str(e)
            }
    
    def check_current_resource_usage(self) -> List[Dict[str, Any]]:
        """检查当前资源使用情况"""
        results = []
        
        try:
            # CPU使用率
            cpu_percent = psutil.cpu_percent(interval=1)
            cpu_check = {
                "check_id": "current_cpu_usage",
                "check_name": "当前CPU使用率",
                "requirement": "< 80%",
                "actual_value": f"{cpu_percent:.1f}%",
                "status": "PASSED" if cpu_percent < 80 else "WARNING",
                "passed": cpu_percent < 80,
                "details": {"cpu_percent": cpu_percent}
            }
            results.append(cpu_check)
            
            # 内存使用率
            memory = psutil.virtual_memory()
            memory_check = {
                "check_id": "current_memory_usage",
                "check_name": "当前内存使用率",
                "requirement": "< 85%",
                "actual_value": f"{memory.percent:.1f}%",
                "status": "PASSED" if memory.percent < 85 else "WARNING",
                "passed": memory.percent < 85,
                "details": {
                    "percent": memory.percent,
                    "used_gb": memory.used / (1024**3),
                    "available_gb": memory.available / (1024**3)
                }
            }
            results.append(memory_check)
            
            # 磁盘使用率（检查根分区）
            try:
                disk = psutil.disk_usage('/' if platform.system() != "Windows" else "C:\\")
                disk_check = {
                    "check_id": "current_disk_usage",
                    "check_name": "当前磁盘使用率",
                    "requirement": "< 90%",
                    "actual_value": f"{disk.percent:.1f}%",
                    "status": "PASSED" if disk.percent < 90 else "WARNING",
                    "passed": disk.percent < 90,
                    "details": {
                        "percent": disk.percent,
                        "used_gb": disk.used / (1024**3),
                        "free_gb": disk.free / (1024**3)
                    }
                }
                results.append(disk_check)
            except:
                pass
            
        except Exception as e:
            error_check = {
                "check_id": "resource_usage_error",
                "check_name": "资源使用检查",
                "requirement": "正常检查",
                "actual_value": "检查失败",
                "status": "WARNING",
                "passed": False,
                "error": str(e)
            }
            results.append(error_check)
        
        return results
    
    def run_all_checks(self) -> Dict[str, Any]:
        """运行所有检查"""
        print("开始执行服务器资源检查...")
        print("=" * 60)
        
        # 收集系统信息
        self.check_results["system_info"] = self.get_system_info()
        
        # 执行各项检查
        checks = [
            self.check_cpu_cores,
            self.check_memory,
            lambda: self.check_disk_space(),
            self.check_network_bandwidth,
            lambda: self.check_current_resource_usage()
        ]
        
        for check_func in checks:
            result = check_func()
            if isinstance(result, list):
                for r in result:
                    self.check_results["results"].append(r)
            else:
                self.check_results["results"].append(result)
        
        # 更新统计信息
        for result in self.check_results["results"]:
            self.check_results["summary"]["total_checks"] += 1
            if result.get("passed", False):
                self.check_results["summary"]["passed"] += 1
            elif result.get("status") == "WARNING":
                self.check_results["summary"]["warning"] += 1
            else:
                self.check_results["summary"]["failed"] += 1
        
        return self.check_results
    
    def print_report(self):
        """打印检查报告"""
        print("\n" + "=" * 60)
        print("服务器资源检查报告")
        print("=" * 60)
        
        # 系统信息
        print(f"\n系统信息:")
        print(f"  平台: {self.check_results['system_info'].get('platform', 'N/A')}")
        print(f"  版本: {self.check_results['system_info'].get('platform_release', 'N/A')}")
        print(f"  主机名: {self.check_results['system_info'].get('hostname', 'N/A')}")
        print(f"  架构: {self.check_results['system_info'].get('architecture', 'N/A')}")
        
        # 检查结果
        print(f"\n检查结果:")
        for result in self.check_results["results"]:
            status_icon = "✅" if result.get("passed") else "⚠️" if result.get("status") == "WARNING" else "❌"
            print(f"  {status_icon} {result.get('check_name', 'N/A')}")
            print(f"     要求: {result.get('requirement', 'N/A')}")
            print(f"     实际: {result.get('actual_value', 'N/A')}")
            print(f"     状态: {result.get('status', 'N/A')}")
            
            # 显示详细信息（如果有）
            details = result.get('details')
            if details and isinstance(details, dict):
                for key, value in details.items():
                    if key not in ['error']:
                        print(f"     {key}: {value}")
            
            # 显示错误信息（如果有）
            error = result.get('error')
            if error:
                print(f"     错误: {error}")
            
            print()
        
        # 统计信息
        summary = self.check_results["summary"]
        print(f"\n统计信息:")
        print(f"  总检查项: {summary['total_checks']}")
        print(f"  通过: {summary['passed']}")
        print(f"  失败: {summary['failed']}")
        print(f"  警告: {summary['warning']}")
        print(f"  通过率: {summary['passed']/summary['total_checks']*100:.1f}%" if summary['total_checks'] > 0 else "  通过率: N/A")
        
        # 总体评估
        if summary['failed'] == 0 and summary['warning'] == 0:
            print(f"\n总体评估: ✅ 所有检查通过")
        elif summary['failed'] == 0:
            print(f"\n总体评估: ⚠️ 有警告项，需要关注")
        else:
            print(f"\n总体评估: ❌ 有失败项，需要修复")
        
        print(f"\n检查时间: {self.check_results['check_time']}")
        print("=" * 60)
    
    def save_report(self, output_file: str = None):
        """保存检查报告到文件"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"server-resources-check-{timestamp}.json"
        
        # 确保输出目录存在
        os.makedirs(os.path.dirname(os.path.abspath(output_file)), exist_ok=True)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(self.check_results, f, ensure_ascii=False, indent=2)
        
        print(f"检查报告已保存到: {output_file}")
        return output_file

def main():
    """主函数"""
    try:
        # 检查依赖
        try:
            import psutil
        except ImportError:
            print("错误: 缺少psutil库，请安装: pip install psutil")
            sys.exit(1)
        
        # 创建检查器并运行检查
        checker = ServerResourceChecker()
        results = checker.run_all_checks()
        
        # 打印报告
        checker.print_report()
        
        # 保存报告
        report_dir = "reports"
        os.makedirs(report_dir, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = os.path.join(report_dir, f"server-resources-check-{timestamp}.json")
        checker.save_report(report_file)
        
        # 根据检查结果返回退出码
        if results["summary"]["failed"] > 0:
            print("\n❌ 有失败项，服务器资源不满足Sprint 27+1质量门禁要求")
            sys.exit(1)
        elif results["summary"]["warning"] > 0:
            print("\n⚠️ 有警告项，建议检查并优化")
            sys.exit(0)
        else:
            print("\n✅ 所有检查通过，服务器资源满足Sprint 27+1质量门禁要求")
            sys.exit(0)
            
    except KeyboardInterrupt:
        print("\n检查被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"检查过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()