"""
端到端测试运行器
统一运行所有端到端测试并生成报告
"""

import os
import sys
import json
import argparse
import subprocess
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Any


class E2ETestRunner:
    """端到端测试运行器"""
    
    def __init__(self):
        self.test_root = os.path.dirname(os.path.abspath(__file__))
        self.scenarios_dir = os.path.join(self.test_root, 'scenarios')
        self.report_dir = os.path.join(self.test_root, 'reports')
        self.results = []
        
        # 创建报告目录
        os.makedirs(self.report_dir, exist_ok=True)
    
    def discover_tests(self, category: str = None) -> List[str]:
        """发现测试文件"""
        test_files = []
        
        if category:
            # 搜索特定类别的测试
            category_dir = os.path.join(self.scenarios_dir, category)
            if os.path.exists(category_dir):
                for file in os.listdir(category_dir):
                    if file.startswith('test_') and file.endswith('.py'):
                        test_files.append(os.path.join(category_dir, file))
        else:
            # 搜索所有测试
            for root, dirs, files in os.walk(self.scenarios_dir):
                for file in files:
                    if file.startswith('test_') and file.endswith('.py'):
                        test_files.append(os.path.join(root, file))
        
        return sorted(test_files)
    
    def run_test_file(self, test_file: str) -> Dict[str, Any]:
        """运行单个测试文件"""
        print(f"\n{'='*60}")
        print(f"运行测试: {os.path.basename(test_file)}")
        print(f"{'='*60}")
        
        result = {
            'file': test_file,
            'start_time': datetime.now().isoformat(),
            'status': 'unknown',
            'output': '',
            'errors': ''
        }
        
        try:
            # 使用pytest运行测试
            cmd = [
                sys.executable, '-m', 'pytest',
                test_file,
                '-v',
                '--tb=short',
                '--json-report',
                '--json-report-file', os.path.join(self.report_dir, f'{os.path.basename(test_file)}.json')
            ]
            
            process = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300  # 5分钟超时
            )
            
            result['output'] = process.stdout
            result['errors'] = process.stderr
            result['return_code'] = process.returncode
            result['end_time'] = datetime.now().isoformat()
            
            if process.returncode == 0:
                result['status'] = 'passed'
                print(f"✅ 测试通过: {os.path.basename(test_file)}")
            else:
                result['status'] = 'failed'
                print(f"❌ 测试失败: {os.path.basename(test_file)}")
                if process.stderr:
                    print(f"错误信息: {process.stderr[:500]}")
            
        except subprocess.TimeoutExpired:
            result['status'] = 'timeout'
            result['errors'] = '测试执行超时'
            print(f"⏱️ 测试超时: {os.path.basename(test_file)}")
        except Exception as e:
            result['status'] = 'error'
            result['errors'] = str(e)
            print(f"💥 测试执行错误: {os.path.basename(test_file)} - {e}")
        
        return result
    
    def run_all_tests(self, category: str = None) -> Dict[str, Any]:
        """运行所有测试"""
        print(f"\n{'#'*60}")
        print(f"开始端到端测试执行")
        print(f"时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"{'#'*60}\n")
        
        # 发现测试
        test_files = self.discover_tests(category)
        print(f"发现 {len(test_files)} 个测试文件")
        
        if not test_files:
            print("未找到测试文件")
            return {'status': 'no_tests', 'total': 0}
        
        # 运行测试
        start_time = datetime.now()
        results = []
        
        for test_file in test_files:
            result = self.run_test_file(test_file)
            results.append(result)
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        # 统计结果
        passed = sum(1 for r in results if r['status'] == 'passed')
        failed = sum(1 for r in results if r['status'] == 'failed')
        errors = sum(1 for r in results if r['status'] == 'error')
        timeouts = sum(1 for r in results if r['status'] == 'timeout')
        
        summary = {
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat(),
            'duration_seconds': duration,
            'total_tests': len(test_files),
            'passed': passed,
            'failed': failed,
            'errors': errors,
            'timeouts': timeouts,
            'success_rate': (passed / len(test_files) * 100) if test_files else 0,
            'results': results
        }
        
        self.results = results
        
        # 打印摘要
        print(f"\n{'#'*60}")
        print(f"测试执行完成")
        print(f"{'#'*60}")
        print(f"总测试数: {len(test_files)}")
        print(f"通过: {passed} ✅")
        print(f"失败: {failed} ❌")
        print(f"错误: {errors} 💥")
        print(f"超时: {timeouts} ⏱️")
        print(f"成功率: {summary['success_rate']:.1f}%")
        print(f"总耗时: {duration:.1f}秒")
        print(f"{'#'*60}\n")
        
        return summary
    
    def generate_report(self, summary: Dict[str, Any]):
        """生成测试报告"""
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        
        # JSON报告
        json_report_path = os.path.join(self.report_dir, f'e2e_test_report_{timestamp}.json')
        with open(json_report_path, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        
        # Markdown报告
        md_report_path = os.path.join(self.report_dir, f'e2e_test_report_{timestamp}.md')
        with open(md_report_path, 'w', encoding='utf-8') as f:
            f.write("# 端到端测试报告\n\n")
            f.write(f"**执行时间**: {summary['start_time']}\n\n")
            f.write(f"**总耗时**: {summary['duration_seconds']:.1f}秒\n\n")
            f.write("## 执行摘要\n\n")
            f.write(f"- **总测试数**: {summary['total_tests']}\n")
            f.write(f"- **通过**: {summary['passed']} ✅\n")
            f.write(f"- **失败**: {summary['failed']} ❌\n")
            f.write(f"- **错误**: {summary['errors']} 💥\n")
            f.write(f"- **超时**: {summary['timeouts']} ⏱️\n")
            f.write(f"- **成功率**: {summary['success_rate']:.1f}%\n\n")
            
            f.write("## 详细结果\n\n")
            f.write("| 测试文件 | 状态 | 耗时 |\n")
            f.write("|---------|------|------|\n")
            
            for result in summary['results']:
                file_name = os.path.basename(result['file'])
                status = result['status']
                status_icon = '✅' if status == 'passed' else '❌' if status == 'failed' else '💥'
                f.write(f"| {file_name} | {status_icon} {status} | - |\n")
        
        print(f"报告已生成:")
        print(f"  - JSON: {json_report_path}")
        print(f"  - Markdown: {md_report_path}")
        
        return json_report_path, md_report_path


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='端到端测试运行器')
    parser.add_argument('--category', '-c', help='运行特定类别的测试 (user-flow, data-flow, business-flow, recovery-flow)')
    parser.add_argument('--report', '-r', action='store_true', help='生成报告')
    
    args = parser.parse_args()
    
    # 创建运行器
    runner = E2ETestRunner()
    
    # 运行测试
    summary = runner.run_all_tests(category=args.category)
    
    # 生成报告
    if args.report or True:  # 默认生成报告
        runner.generate_report(summary)
    
    # 返回状态码
    if summary.get('failed', 0) > 0 or summary.get('errors', 0) > 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())