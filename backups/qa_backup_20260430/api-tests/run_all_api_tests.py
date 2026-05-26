"""
Sprint 27+1 测试环境API接口自动化测试主运行脚本
运行所有API测试：RESTful, GraphQL, WebSocket, 文件上传/下载
"""

import sys
import os
import time
import json
import subprocess
from datetime import datetime
from typing import Dict, List, Any, Optional

# 添加当前目录到路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from config import config
from test_utils import logger


class APITestRunner:
    """API测试运行器"""
    
    def __init__(self):
        self.test_results = {}
        self.start_time = None
        self.end_time = None
        
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有API测试"""
        self.start_time = time.time()
        logger.log_test_start("Sprint 27+1 完整API测试套件")
        
        print("=" * 70)
        print("🚀 Sprint 27+1 - 测试环境API接口自动化测试")
        print("=" * 70)
        print(f"测试环境: {config.base_url}")
        print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"测试目录: {os.path.dirname(os.path.abspath(__file__))}")
        print()
        
        # 运行各个API测试
        test_modules = [
            {
                "name": "RESTful API测试",
                "script": "restful/test_restful_api.py",
                "description": "测试RESTful API接口功能、性能和错误处理"
            },
            {
                "name": "GraphQL API测试", 
                "script": "graphql/test_graphql_api.py",
                "description": "测试GraphQL API查询、变更、订阅和性能"
            },
            {
                "name": "WebSocket API测试",
                "script": "websocket/test_websocket_api.py", 
                "description": "测试WebSocket连接、消息交换和实时通信"
            },
            {
                "name": "文件上传/下载API测试",
                "script": "file-upload/test_file_upload_api.py",
                "description": "测试文件上传、下载、大小限制和类型验证"
            }
        ]
        
        overall_results = {
            "project": "Sprint 27+1 - AI-Ready 测试环境",
            "environment": config.base_url,
            "start_time": datetime.now().isoformat(),
            "test_modules": [],
            "summary": {
                "total_modules": 0,
                "completed_modules": 0,
                "passed_modules": 0,
                "failed_modules": 0,
                "total_tests": 0,
                "passed_tests": 0,
                "failed_tests": 0,
                "warning_tests": 0
            }
        }
        
        for module in test_modules:
            print(f"🔧 开始运行: {module['name']}")
            print(f"   {module['description']}")
            
            module_result = self.run_test_module(module)
            overall_results["test_modules"].append(module_result)
            
            # 更新统计信息
            overall_results["summary"]["total_modules"] += 1
            overall_results["summary"]["completed_modules"] += 1
            
            if module_result.get("execution_status") == "completed":
                module_summary = module_result.get("results", {}).get("summary", {})
                overall_results["summary"]["total_tests"] += module_summary.get("total", 0)
                overall_results["summary"]["passed_tests"] += module_summary.get("passed", 0)
                overall_results["summary"]["failed_tests"] += module_summary.get("failed", 0)
                overall_results["summary"]["warning_tests"] += module_summary.get("warnings", 0)
                
                if module_summary.get("failed", 0) == 0:
                    overall_results["summary"]["passed_modules"] += 1
                else:
                    overall_results["summary"]["failed_modules"] += 1
            
            print()
        
        self.end_time = time.time()
        overall_results["end_time"] = datetime.now().isoformat()
        overall_results["total_duration_seconds"] = round(self.end_time - self.start_time, 2)
        
        # 保存总体报告
        self.save_overall_report(overall_results)
        
        # 生成总结报告
        self.generate_summary_report(overall_results)
        
        return overall_results
    
    def run_test_module(self, module: Dict) -> Dict[str, Any]:
        """运行单个测试模块"""
        module_result = {
            "module_name": module["name"],
            "script": module["script"],
            "description": module["description"],
            "start_time": datetime.now().isoformat(),
            "execution_status": "failed",
            "error": None,
            "results": None
        }
        
        script_path = os.path.join(os.path.dirname(__file__), module["script"])
        
        if not os.path.exists(script_path):
            module_result["error"] = f"测试脚本不存在: {script_path}"
            logger.log_test_fail(module["name"], module_result["error"])
            return module_result
        
        try:
            # 运行测试脚本
            env = os.environ.copy()
            env["PYTHONPATH"] = os.path.dirname(os.path.abspath(__file__)) + os.pathsep + env.get("PYTHONPATH", "")
            
            result = subprocess.run(
                [sys.executable, script_path],
                capture_output=True,
                text=True,
                env=env,
                timeout=300  # 5分钟超时
            )
            
            module_result["end_time"] = datetime.now().isoformat()
            module_result["exit_code"] = result.returncode
            module_result["stdout"] = result.stdout
            module_result["stderr"] = result.stderr
            
            # 尝试从输出中提取JSON结果
            json_result = self.extract_json_result(result.stdout)
            if json_result:
                module_result["results"] = json_result
                module_result["execution_status"] = "completed"
                logger.log_test_pass(module["name"], f"测试完成，退出码: {result.returncode}")
            else:
                module_result["execution_status"] = "partial"
                module_result["error"] = "无法从输出中提取JSON结果"
                logger.log_test_warning(module["name"], module_result["error"])
            
        except subprocess.TimeoutExpired:
            module_result["execution_status"] = "timeout"
            module_result["error"] = "测试执行超时（5分钟）"
            logger.log_test_fail(module["name"], module_result["error"])
        except Exception as e:
            module_result["execution_status"] = "error"
            module_result["error"] = str(e)
            logger.log_test_fail(module["name"], module_result["error"])
        
        return module_result
    
    def extract_json_result(self, output: str) -> Optional[Dict]:
        """从输出中提取JSON结果"""
        try:
            # 查找JSON开始和结束位置
            lines = output.split('\n')
            json_start = -1
            json_end = -1
            
            for i, line in enumerate(lines):
                if line.strip().startswith('{'):
                    json_start = i
                    break
            
            if json_start == -1:
                return None
            
            # 查找匹配的结束大括号
            brace_count = 0
            for i in range(json_start, len(lines)):
                line = lines[i]
                brace_count += line.count('{')
                brace_count -= line.count('}')
                
                if brace_count == 0:
                    json_end = i
                    break
            
            if json_end == -1:
                return None
            
            # 提取并解析JSON
            json_text = '\n'.join(lines[json_start:json_end + 1])
            return json.loads(json_text)
        except:
            return None
    
    def save_overall_report(self, results: Dict[str, Any]):
        """保存总体测试报告"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_filename = f"api_tests_overall_report_{timestamp}.json"
        report_path = os.path.join(config.reports_dir, report_filename)
        
        try:
            os.makedirs(config.reports_dir, exist_ok=True)
            
            with open(report_path, 'w', encoding='utf-8') as f:
                json.dump(results, f, indent=2, ensure_ascii=False)
            
            logger.log_test_pass("保存总体测试报告", f"报告已保存到: {report_path}")
            print(f"📊 总体测试报告已保存: {report_path}")
            
            # 同时保存为易读的文本格式
            txt_report_path = os.path.join(config.reports_dir, f"api_tests_summary_{timestamp}.txt")
            self.save_text_report(results, txt_report_path)
            
        except Exception as e:
            logger.log_test_fail("保存总体测试报告", str(e))
            print(f"❌ 保存总体测试报告失败: {str(e)}")
    
    def save_text_report(self, results: Dict[str, Any], filepath: str):
        """保存文本格式的报告"""
        try:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write("=" * 70 + "\n")
                f.write("Sprint 27+1 - API接口自动化测试总结报告\n")
                f.write("=" * 70 + "\n\n")
                
                f.write(f"项目: {results.get('project', 'N/A')}\n")
                f.write(f"测试环境: {results.get('environment', 'N/A')}\n")
                f.write(f"开始时间: {results.get('start_time', 'N/A')}\n")
                f.write(f"结束时间: {results.get('end_time', 'N/A')}\n")
                f.write(f"总耗时: {results.get('total_duration_seconds', 0):.2f} 秒\n\n")
                
                summary = results.get('summary', {})
                f.write("📊 测试统计:\n")
                f.write(f"   测试模块: {summary.get('total_modules', 0)} 个\n")
                f.write(f"   完成模块: {summary.get('completed_modules', 0)} 个\n")
                f.write(f"   通过模块: {summary.get('passed_modules', 0)} 个\n")
                f.write(f"   失败模块: {summary.get('failed_modules', 0)} 个\n")
                f.write(f"   总测试数: {summary.get('total_tests', 0)} 个\n")
                f.write(f"   通过测试: {summary.get('passed_tests', 0)} 个\n")
                f.write(f"   失败测试: {summary.get('failed_tests', 0)} 个\n")
                f.write(f"   警告测试: {summary.get('warning_tests', 0)} 个\n\n")
                
                f.write("🔧 各模块详情:\n")
                for module in results.get('test_modules', []):
                    f.write(f"\n  {module.get('module_name', 'N/A')}:\n")
                    f.write(f"    状态: {module.get('execution_status', 'N/A')}\n")
                    
                    if module.get('results'):
                        module_results = module['results']
                        module_summary = module_results.get('summary', {})
                        f.write(f"    测试数: {module_summary.get('total', 0)}\n")
                        f.write(f"    通过: {module_summary.get('passed', 0)}\n")
                        f.write(f"    失败: {module_summary.get('failed', 0)}\n")
                        f.write(f"    警告: {module_summary.get('warnings', 0)}\n")
                        f.write(f"    耗时: {module_results.get('duration', 0):.2f} 秒\n")
                    
                    if module.get('error'):
                        f.write(f"    错误: {module['error']}\n")
                
                f.write("\n" + "=" * 70 + "\n")
                f.write("报告结束\n")
                f.write("=" * 70 + "\n")
            
            print(f"📝 文本总结报告已保存: {filepath}")
            
        except Exception as e:
            print(f"❌ 保存文本报告失败: {str(e)}")
    
    def generate_summary_report(self, results: Dict[str, Any]):
        """生成并显示总结报告"""
        print("\n" + "=" * 70)
        print("📋 Sprint 27+1 - API接口自动化测试总结")
        print("=" * 70)
        
        summary = results.get('summary', {})
        
        # 计算通过率
        total_tests = summary.get('total_tests', 0)
        passed_tests = summary.get('passed_tests', 0)
        failed_tests = summary.get('failed_tests', 0)
        warning_tests = summary.get('warning_tests', 0)
        
        if total_tests > 0:
            pass_rate = (passed_tests / total_tests) * 100
        else:
            pass_rate = 0
        
        print(f"\n📊 总体统计:")
        print(f"   测试模块: {summary.get('total_modules', 0)} 个")
        print(f"   完成模块: {summary.get('completed_modules', 0)} 个")
        print(f"   通过模块: {summary.get('passed_modules', 0)} 个 ✅")
        print(f"   失败模块: {summary.get('failed_modules', 0)} 个 ❌")
        
        print(f"\n🔬 测试详情:")
        print(f"   总测试数: {total_tests} 个")
        print(f"   通过测试: {passed_tests} 个 ✅")
        print(f"   失败测试: {failed_tests} 个 ❌")
        print(f"   警告测试: {warning_tests} 个 ⚠️")
        print(f"   通过率: {pass_rate:.1f}%")
        
        print(f"\n⏱️  执行时间:")
        print(f"   开始时间: {results.get('start_time', 'N/A')}")
        print(f"   结束时间: {results.get('end_time', 'N/A')}")
        print(f"   总耗时: {results.get('total_duration_seconds', 0):.2f} 秒")
        
        print(f"\n🔧 各模块状态:")
        for module in results.get('test_modules', []):
            status_icon = {
                'completed': '✅',
                'partial': '⚠️',
                'failed': '❌',
                'error': '❌',
                'timeout': '⏰'
            }.get(module.get('execution_status', ''), '❓')
            
            module_name = module.get('module_name', 'N/A')
            status = module.get('execution_status', 'unknown').upper()
            
            if module.get('results'):
                module_results = module['results']
                module_summary = module_results.get('summary', {})
                details = f"测试: {module_summary.get('total', 0)}, 通过: {module_summary.get('passed', 0)}, 失败: {module_summary.get('failed', 0)}"
            else:
                details = module.get('error', '无结果')
            
            print(f"   {status_icon} {module_name}: {status} - {details}")
        
        print("\n" + "=" * 70)
        
        # 给出总体评估
        if failed_tests == 0 and summary.get('failed_modules', 0) == 0:
            print("🎉 所有测试通过！API接口功能正常。")
            print("建议: 可以继续进行集成测试和性能测试。")
        elif failed_tests > 0:
            print("⚠️  存在失败的测试，需要检查API接口功能。")
            print("建议: 检查失败的测试详情，修复相关问题。")
        else:
            print("ℹ️  测试完成，但存在警告。")
            print("建议: 检查警告信息，优化API接口。")
        
        print("=" * 70 + "\n")


def check_dependencies():
    """检查依赖包"""
    print("🔍 检查依赖包...")
    
    required_packages = [
        "requests",
        # "websockets",  # WebSocket测试需要，但不是必须的
    ]
    
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package.replace('-', '_'))
            print(f"   ✅ {package}")
        except ImportError:
            missing_packages.append(package)
            print(f"   ❌ {package} (未安装)")
    
    if missing_packages:
        print(f"\n⚠️  缺少以下依赖包:")
        for package in missing_packages:
            print(f"    pip install {package}")
        print("\n部分测试可能无法正常运行。")
    
    return len(missing_packages) == 0


def main():
    """主函数"""
    print("\n" + "=" * 70)
    print("🚀 Sprint 27+1 - 测试环境API接口自动化测试")
    print("=" * 70)
    
    # 检查依赖
    if not check_dependencies():
        print("\n⚠️  继续运行测试可能会出错。")
        response = input("是否继续? (y/N): ")
        if response.lower() != 'y':
            print("测试已取消。")
            return 1
    
    print("\n" + "=" * 70)
    
    try:
        # 创建并运行测试
        runner = APITestRunner()
        results = runner.run_all_tests()
        
        # 根据结果返回退出码
        summary = results.get('summary', {})
        if summary.get('failed_tests', 0) == 0 and summary.get('failed_modules', 0) == 0:
            return 0  # 成功
        else:
            return 1  # 有失败
        
    except KeyboardInterrupt:
        print("\n\n⏹️  测试被用户中断。")
        return 130
    except Exception as e:
        print(f"\n❌ 测试运行异常: {str(e)}")
        import traceback
        traceback.print_exc()
        return 1


if __name__ == "__main__":
    exit_code = main()
    sys.exit(exit_code)