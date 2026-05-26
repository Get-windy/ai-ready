#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试运行脚本 - 统一入口
"""

import subprocess
import sys
import argparse
from pathlib import Path


def run_tests(args):
    """运行测试"""
    cmd = ['pytest']
    
    # 基本选项
    cmd.extend(['-v', '--tb=short'])
    
    # 环境选项
    if args.env:
        cmd.extend(['--env', args.env])
    
    # 测试类型过滤
    if args.smoke:
        cmd.extend(['-m', 'smoke'])
    elif args.api:
        cmd.extend(['-m', 'api'])
    elif args.ui:
        cmd.extend(['-m', 'ui'])
    elif args.integration:
        cmd.extend(['-m', 'integration'])
    elif args.performance:
        cmd.extend(['-m', 'performance'])
    elif args.security:
        cmd.extend(['-m', 'security'])
    
    # 并行执行
    if args.parallel:
        cmd.extend(['-n', str(args.parallel)])
    
    # 报告选项
    if args.html:
        cmd.extend(['--html', args.html, '--self-contained-html'])
    
    if args.allure:
        cmd.extend(['--alluredir', args.allure])
    
    if args.coverage:
        cmd.extend(['--cov', 'src', '--cov-report=html:reports/coverage'])
    
    # 特定测试文件/目录
    if args.tests:
        cmd.extend(args.tests)
    
    print(f"运行命令: {' '.join(cmd)}")
    
    result = subprocess.run(cmd, cwd=Path(__file__).parent.parent.parent)
    
    return result.returncode


def main():
    parser = argparse.ArgumentParser(description='AI-Ready测试运行脚本')
    
    parser.add_argument('--env', default='dev', help='测试环境')
    parser.add_argument('--smoke', action='store_true', help='运行冒烟测试')
    parser.add_argument('--api', action='store_true', help='运行API测试')
    parser.add_argument('--ui', action='store_true', help='运行UI测试')
    parser.add_argument('--integration', action='store_true', help='运行集成测试')
    parser.add_argument('--performance', action='store_true', help='运行性能测试')
    parser.add_argument('--security', action='store_true', help='运行安全测试')
    parser.add_argument('--parallel', type=int, default=0, help='并行进程数')
    parser.add_argument('--html', default='', help='HTML报告路径')
    parser.add_argument('--allure', default='', help='Allure报告目录')
    parser.add_argument('--coverage', action='store_true', help='生成覆盖率报告')
    parser.add_argument('tests', nargs='*', help='特定测试文件/目录')
    
    args = parser.parse_args()
    
    sys.exit(run_tests(args))


if __name__ == '__main__':
    main()