#!/usr/bin/env python3
"""
Sprint 27+1 测试环境安全配置验证脚本 - 主程序
开发Sprint 27+1测试环境的安全配置验证脚本，确保测试环境的安全合规性
"""

import os
import sys
import json
import yaml
import logging
import argparse
from datetime import datetime
from typing import Dict, List, Any, Optional

# 添加模块路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 导入自定义模块
try:
    from config_validation.security_config_validator import SecurityConfigValidator
    from vulnerability_scanning.vulnerability_scanner import VulnerabilityScanner
    from compliance_checks.compliance_checker import ComplianceChecker
    from reporting.security_report_generator import SecurityReportGenerator
except ImportError as e:
    print(f"模块导入失败: {e}")
    print("请确保所有子模块都已正确创建")
    sys.exit(1)

class SecurityValidationMain:
    """安全验证主程序"""
    
    def __init__(self, test_env_root: str = None):
        """
        初始化安全验证主程序
        
        Args:
            test_env_root: 测试环境根目录，如果为None则使用当前脚本所在目录的父目录
        """
        if test_env_root is None:
            test_env_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        
        self.test_env_root = test_env_root
        self.results = {}
        self.setup_logging()
        
        # 初始化各验证模块
        self.config_validator = SecurityConfigValidator(test_env_root)
        self.vulnerability_scanner = VulnerabilityScanner(test_env_root)
        self.compliance_checker = ComplianceChecker(test_env_root)
        self.report_generator = SecurityReportGenerator(test_env_root)
    
    def setup_logging(self):
        """设置日志"""
        log_dir = os.path.join(self.test_env_root, "logs", "security")
        os.makedirs(log_dir, exist_ok=True)
        
        log_file = os.path.join(log_dir, f"security_validation_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")
        
        logging.basicConfig(
            level=logging.INFO,
            format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            handlers=[
                logging.FileHandler(log_file, encoding='utf-8'),
                logging.StreamHandler(sys.stdout)
            ]
        )
        
        self.logger = logging.getLogger(__name__)
        self.logger.info(f"安全验证程序启动，测试环境根目录: {self.test_env_root}")
        self.logger.info(f"日志文件: {log_file}")
    
    def run_all_checks(self) -> Dict[str, Any]:
        """
        运行所有安全检查
        
        Returns:
            包含所有检查结果的字典
        """
        self.logger.info("开始执行安全验证...")
        
        try:
            # 1. 安全配置验证
            self.logger.info("执行安全配置验证...")
            config_results = self.config_validator.validate_all_configs()
            self.results['config_validation'] = config_results
            
            # 2. 漏洞扫描
            self.logger.info("执行漏洞扫描...")
            vulnerability_results = self.vulnerability_scanner.scan_all()
            self.results['vulnerability_scanning'] = vulnerability_results
            
            # 3. 合规检查
            self.logger.info("执行合规检查...")
            compliance_results = self.compliance_checker.check_all_compliance()
            self.results['compliance_checks'] = compliance_results
            
            # 4. 生成报告
            self.logger.info("生成安全报告...")
            report_results = self.report_generator.generate_all_reports(self.results)
            self.results['reporting'] = report_results
            
            # 5. 汇总结果
            self.logger.info("汇总验证结果...")
            overall_result = self.summarize_results()
            self.results['overall'] = overall_result
            
            self.logger.info(f"安全验证完成，总体结果: {overall_result['status']}")
            
        except Exception as e:
            self.logger.error(f"安全验证过程中发生错误: {e}", exc_info=True)
            self.results['error'] = str(e)
        
        return self.results
    
    def summarize_results(self) -> Dict[str, Any]:
        """汇总所有检查结果"""
        summary = {
            'total_checks': 0,
            'passed_checks': 0,
            'failed_checks': 0,
            'warnings': 0,
            'status': 'UNKNOWN',
            'timestamp': datetime.now().isoformat()
        }
        
        # 统计各模块结果
        for module_name, module_results in self.results.items():
            if module_name in ['config_validation', 'vulnerability_scanning', 'compliance_checks']:
                if isinstance(module_results, dict):
                    if 'summary' in module_results:
                        module_summary = module_results['summary']
                        summary['total_checks'] += module_summary.get('total_checks', 0)
                        summary['passed_checks'] += module_summary.get('passed_checks', 0)
                        summary['failed_checks'] += module_summary.get('failed_checks', 0)
                        summary['warnings'] += module_summary.get('warnings', 0)
        
        # 确定总体状态
        if summary['failed_checks'] > 0:
            summary['status'] = 'FAILED'
        elif summary['warnings'] > 0:
            summary['status'] = 'WARNING'
        elif summary['passed_checks'] > 0:
            summary['status'] = 'PASSED'
        else:
            summary['status'] = 'NO_CHECKS'
        
        return summary
    
    def save_results(self, output_file: str = None):
        """保存验证结果到文件"""
        if output_file is None:
            results_dir = os.path.join(self.test_env_root, "reports", "security")
            os.makedirs(results_dir, exist_ok=True)
            output_file = os.path.join(results_dir, f"security_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
        
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                # 将datetime对象转换为字符串
                def json_serializer(obj):
                    if isinstance(obj, datetime):
                        return obj.isoformat()
                    raise TypeError(f"Type {type(obj)} not serializable")
                
                json.dump(self.results, f, indent=2, default=json_serializer, ensure_ascii=False)
            
            self.logger.info(f"验证结果已保存到: {output_file}")
            return output_file
        except Exception as e:
            self.logger.error(f"保存验证结果失败: {e}")
            return None
    
    def print_summary(self):
        """打印验证结果摘要"""
        if 'overall' not in self.results:
            print("尚未运行验证，请先调用 run_all_checks()")
            return
        
        summary = self.results['overall']
        
        print("\n" + "="*60)
        print("安全验证结果摘要")
        print("="*60)
        print(f"总体状态: {summary['status']}")
        print(f"总检查项: {summary['total_checks']}")
        print(f"通过项: {summary['passed_checks']}")
        print(f"失败项: {summary['failed_checks']}")
        print(f"警告项: {summary['warnings']}")
        print(f"时间戳: {summary['timestamp']}")
        print("="*60)
        
        # 打印各模块状态
        for module_name in ['config_validation', 'vulnerability_scanning', 'compliance_checks']:
            if module_name in self.results:
                module_results = self.results[module_name]
                if isinstance(module_results, dict) and 'summary' in module_results:
                    module_summary = module_results['summary']
                    print(f"\n{module_name.replace('_', ' ').title()}:")
                    print(f"  状态: {module_summary.get('status', 'UNKNOWN')}")
                    print(f"  检查项: {module_summary.get('total_checks', 0)}")
                    print(f"  通过: {module_summary.get('passed_checks', 0)}")
                    print(f"  失败: {module_summary.get('failed_checks', 0)}")

def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='测试环境安全配置验证脚本')
    parser.add_argument('--test-env', default=None, help='测试环境根目录')
    parser.add_argument('--output', default=None, help='结果输出文件')
    parser.add_argument('--quick', action='store_true', help='快速模式，只运行关键检查')
    
    args = parser.parse_args()
    
    try:
        # 创建验证主程序
        validator = SecurityValidationMain(args.test_env)
        
        # 运行检查
        validator.run_all_checks()
        
        # 保存结果
        if args.output:
            validator.save_results(args.output)
        else:
            validator.save_results()
        
        # 打印摘要
        validator.print_summary()
        
        # 根据结果返回退出码
        if 'overall' in validator.results:
            overall_status = validator.results['overall']['status']
            if overall_status == 'FAILED':
                return 1
            elif overall_status == 'WARNING':
                return 2
        
        return 0
        
    except Exception as e:
        print(f"程序执行失败: {e}", file=sys.stderr)
        return 3

if __name__ == "__main__":
    sys.exit(main())