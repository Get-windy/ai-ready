"""
Sprint 27+1 测试环境自动化测试主脚本
执行所有模块的自动化测试
"""

import sys
import os
import logging
import time
from datetime import datetime
from typing import Dict, List, Any

# 添加框架路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from test_scripts.user_management_tests import run_user_management_tests
from test_scripts.order_management_tests import run_order_management_tests
from test_scripts.inventory_management_tests import run_inventory_management_tests

class TestExecutor:
    """测试执行器"""
    
    def __init__(self, test_env: str = "test"):
        self.test_env = test_env
        self.logger = self._setup_logging()
        self.results = {}
        
    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        log_dir = "logs"
        os.makedirs(log_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        log_file = os.path.join(log_dir, f"test_execution_{timestamp}.log")
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler()
            ]
        )
        
        return logging.getLogger(__name__)
    
    def run_user_management_tests(self) -> Dict[str, Any]:
        """运行用户管理测试"""
        self.logger.info("=" * 60)
        self.logger.info("开始执行用户管理模块测试")
        self.logger.info("=" * 60)
        
        try:
            start_time = time.time()
            results = run_user_management_tests()
            duration = time.time() - start_time
            
            self.results['user_management'] = {
                'results': results,
                'duration': duration,
                'status': 'completed'
            }
            
            self.logger.info(f"用户管理测试完成，耗时: {duration:.2f}秒")
            return results
            
        except Exception as e:
            self.logger.error(f"用户管理测试失败: {e}")
            self.results['user_management'] = {
                'error': str(e),
                'status': 'failed'
            }
            raise
    
    def run_order_management_tests(self) -> Dict[str, Any]:
        """运行订单管理测试"""
        self.logger.info("=" * 60)
        self.logger.info("开始执行订单管理模块测试")
        self.logger.info("=" * 60)
        
        try:
            start_time = time.time()
            results = run_order_management_tests()
            duration = time.time() - start_time
            
            self.results['order_management'] = {
                'results': results,
                'duration': duration,
                'status': 'completed'
            }
            
            self.logger.info(f"订单管理测试完成，耗时: {duration:.2f}秒")
            return results
            
        except Exception as e:
            self.logger.error(f"订单管理测试失败: {e}")
            self.results['order_management'] = {
                'error': str(e),
                'status': 'failed'
            }
            raise
    
    def run_inventory_management_tests(self) -> Dict[str, Any]:
        """运行库存管理测试"""
        self.logger.info("=" * 60)
        self.logger.info("开始执行库存管理模块测试")
        self.logger.info("=" * 60)
        
        try:
            start_time = time.time()
            results = run_inventory_management_tests()
            duration = time.time() - start_time
            
            self.results['inventory_management'] = {
                'results': results,
                'duration': duration,
                'status': 'completed'
            }
            
            self.logger.info(f"库存管理测试完成，耗时: {duration:.2f}秒")
            return results
            
        except Exception as e:
            self.logger.error(f"库存管理测试失败: {e}")
            self.results['inventory_management'] = {
                'error': str(e),
                'status': 'failed'
            }
            raise
    
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有测试"""
        self.logger.info("=" * 60)
        self.logger.info("开始执行Sprint 27+1测试环境自动化测试")
        self.logger.info(f"测试环境: {self.test_env}")
        self.logger.info(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self.logger.info("=" * 60)
        
        overall_start_time = time.time()
        
        try:
            # 运行所有模块测试
            self.run_user_management_tests()
            self.run_order_management_tests()
            self.run_inventory_management_tests()
            
            overall_duration = time.time() - overall_start_time
            
            # 生成总体报告
            report = self.generate_overall_report(overall_duration)
            
            self.logger.info("=" * 60)
            self.logger.info("所有测试执行完成")
            self.logger.info(f"总耗时: {overall_duration:.2f}秒")
            self.logger.info("=" * 60)
            
            # 保存报告
            self.save_report(report)
            
            return self.results
            
        except Exception as e:
            self.logger.error(f"测试执行过程中发生错误: {e}")
            overall_duration = time.time() - overall_start_time
            
            report = self.generate_overall_report(overall_duration, error=str(e))
            self.save_report(report)
            
            raise
    
    def generate_overall_report(self, total_duration: float, error: str = None) -> str:
        """生成总体测试报告"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        report = f"""
Sprint 27+1 测试环境自动化测试报告
==========================================
生成时间: {timestamp}
测试环境: {self.test_env}
总执行时间: {total_duration:.2f}秒

"""

        if error:
            report += f"执行状态: 失败\n错误信息: {error}\n\n"
        else:
            report += "执行状态: 成功\n\n"
        
        # 各模块测试结果汇总
        total_cases = 0
        passed_cases = 0
        failed_cases = 0
        error_cases = 0
        skipped_cases = 0
        
        report += "各模块测试结果:\n"
        report += "-" * 80 + "\n"
        
        for module, module_results in self.results.items():
            module_name = module.replace('_', ' ').title()
            
            if module_results.get('status') == 'completed':
                results = module_results['results']
                duration = module_results['duration']
                
                total = results.get('total_cases', 0)
                passed = results.get('passed_cases', 0)
                failed = results.get('failed_cases', 0)
                error = results.get('error_cases', 0)
                skipped = results.get('skipped_cases', 0)
                
                total_cases += total
                passed_cases += passed
                failed_cases += failed
                error_cases += error
                skipped_cases += skipped
                
                pass_rate = (passed / total * 100) if total > 0 else 0
                
                report += f"""
{module_name}:
  测试用例: {total}
  通过: {passed} ({pass_rate:.1f}%)
  失败: {failed}
  错误: {error}
  跳过: {skipped}
  耗时: {duration:.2f}秒
"""
            else:
                report += f"""
{module_name}:
  状态: 失败
  错误: {module_results.get('error', '未知错误')}
"""
        
        report += "-" * 80 + "\n"
        
        # 总体统计
        if total_cases > 0:
            overall_pass_rate = (passed_cases / total_cases * 100)
        else:
            overall_pass_rate = 0
        
        report += f"""
总体统计:
  总测试用例: {total_cases}
  总通过用例: {passed_cases} ({overall_pass_rate:.1f}%)
  总失败用例: {failed_cases}
  总错误用例: {error_cases}
  总跳过用例: {skipped_cases}
  总执行时间: {total_duration:.2f}秒

测试结论:
"""
        
        if error:
            report += "  测试执行失败，请检查错误信息。\n"
        elif failed_cases > 0 or error_cases > 0:
            report += "  测试未完全通过，存在失败或错误的测试用例。\n"
        elif overall_pass_rate < 100:
            report += "  测试未完全通过，存在跳过的测试用例。\n"
        else:
            report += "  所有测试用例执行通过！\n"
        
        report += "\n详细测试日志请查看日志文件。\n"
        
        return report
    
    def save_report(self, report: str):
        """保存测试报告到文件"""
        reports_dir = "reports"
        os.makedirs(reports_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = os.path.join(reports_dir, f"test_report_{timestamp}.txt")
        
        try:
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(report)
            
            self.logger.info(f"测试报告已保存到: {report_file}")
            
        except Exception as e:
            self.logger.error(f"保存测试报告失败: {e}")

def main():
    """主函数"""
    try:
        # 获取测试环境参数
        test_env = sys.argv[1] if len(sys.argv) > 1 else "test"
        
        # 创建测试执行器
        executor = TestExecutor(test_env)
        
        # 运行所有测试
        results = executor.run_all_tests()
        
        # 检查是否有失败
        failed_modules = [
            module for module, module_results in results.items()
            if module_results.get('status') != 'completed'
        ]
        
        if failed_modules:
            print(f"\n警告: 以下模块测试失败: {', '.join(failed_modules)}")
            sys.exit(1)
        else:
            print("\n所有测试执行完成！")
            sys.exit(0)
            
    except KeyboardInterrupt:
        print("\n测试被用户中断")
        sys.exit(130)
    except Exception as e:
        print(f"\n测试执行失败: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()