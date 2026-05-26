#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 自动化测试执行器
统一执行API和UI自动化测试
"""

import os
import sys
import subprocess
import argparse
from datetime import datetime
from pathlib import Path


# 项目路径
PROJECT_ROOT = Path(__file__).parent.parent.parent
AUTOMATION_DIR = Path(__file__).parent
RESULTS_DIR = AUTOMATION_DIR / "results"


def run_pytest(test_type="all", markers=None, verbose=True, coverage=True):
    """执行pytest测试"""
    
    # 确保结果目录存在
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_file = RESULTS_DIR / f"test_report_{timestamp}.html"
    coverage_dir = RESULTS_DIR / f"coverage_{timestamp}"
    
    # 构建pytest命令
    cmd = ["pytest"]
    
    # 测试类型
    if test_type == "api":
        cmd.append("test_api_automation.py")
    elif test_type == "ui":
        cmd.append("test_ui_automation.py")
    else:
        cmd.append(".")
    
    # 标记过滤
    if markers:
        cmd.extend(["-m", markers])
    
    # 详细输出
    if verbose:
        cmd.append("-v")
    
    # 测试报告
    cmd.extend(["--html", str(report_file), "--self-contained-html"])
    
    # 覆盖率报告
    if coverage:
        cmd.extend([
            "--cov=.",
            "--cov-report", f"html:{coverage_dir}",
            "--cov-report", "term-missing"
        ])
    
    # 短格式错误
    cmd.append("--tb=short")
    
    # 并行执行
    cmd.extend(["-n", "auto"])
    
    print(f"执行命令: {' '.join(cmd)}")
    print(f"工作目录: {AUTOMATION_DIR}")
    
    # 执行测试
    result = subprocess.run(
        cmd,
        cwd=str(AUTOMATION_DIR),
        capture_output=True,
        text=True
    )
    
    print(result.stdout)
    if result.stderr:
        print(f"错误输出:\n{result.stderr}")
    
    return result.returncode, report_file


def run_smoke_tests():
    """执行冒烟测试"""
    print("\n" + "="*60)
    print("执行冒烟测试 (Smoke Tests)")
    print("="*60 + "\n")
    return run_pytest(markers="smoke", coverage=False)


def run_regression_tests():
    """执行回归测试"""
    print("\n" + "="*60)
    print("执行回归测试 (Regression Tests)")
    print("="*60 + "\n")
    return run_pytest(markers="regression")


def run_api_tests():
    """执行API自动化测试"""
    print("\n" + "="*60)
    print("执行API自动化测试")
    print("="*60 + "\n")
    return run_pytest(test_type="api")


def run_ui_tests():
    """执行UI自动化测试"""
    print("\n" + "="*60)
    print("执行UI自动化测试")
    print("="*60 + "\n")
    return run_pytest(test_type="ui")


def run_all_tests():
    """执行所有自动化测试"""
    print("\n" + "="*60)
    print("执行所有自动化测试")
    print("="*60 + "\n")
    return run_pytest(test_type="all")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="AI-Ready自动化测试执行器")
    parser.add_argument("--type", "-t", default="all", 
                        choices=["all", "api", "ui", "smoke", "regression"],
                        help="测试类型")
    parser.add_argument("--markers", "-m", help="pytest标记过滤")
    parser.add_argument("--no-coverage", action="store_true", help="不生成覆盖率报告")
    parser.add_argument("--quiet", "-q", action="store_true", help="简洁输出")
    
    args = parser.parse_args()
    
    # 根据类型执行测试
    if args.type == "smoke":
        code, report = run_smoke_tests()
    elif args.type == "regression":
        code, report = run_regression_tests()
    elif args.type == "api":
        code, report = run_api_tests()
    elif args.type == "ui":
        code, report = run_ui_tests()
    else:
        code, report = run_all_tests()
    
    if code == 0:
        print(f"\n✅ 测试通过！报告: {report}")
    else:
        print(f"\n❌ 测试失败！报告: {report}")
    
    return code


if __name__ == "__main__":
    sys.exit(main())