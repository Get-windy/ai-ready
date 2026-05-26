#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试运行器 - 支持并行测试和报告生成
"""

import os
import sys
import subprocess
import argparse
from pathlib import Path
from datetime import datetime


class TestRunner:
    """测试运行器"""
    
    def __init__(self):
        self.base_dir = Path(__file__).parent
        self.reports_dir = self.base_dir / "reports"
        self.reports_dir.mkdir(exist_ok=True)
        
    def run_tests(self, args):
        """运行测试"""
        # 构建pytest命令
        cmd = ["python", "-m", "pytest"]
        
        # 添加测试路径
        if args.path:
            cmd.extend(args.path)
        else:
            cmd.extend(["tests/api", "tests/e2e", "tests/integration", "tests/unit"])
        
        # 添加基本选项
        cmd.extend(["-v", "--tb=short", "--color=yes"])
        
        # 添加标记过滤
        if args.markers:
            cmd.extend(["-m", args.markers])
        
        # 添加并行测试支持
        if args.parallel:
            num_workers = args.workers or "auto"
            cmd.extend(["-n", str(num_workers), "--dist=loadfile"])
        
        # 添加超时设置
        if args.timeout:
            cmd.extend(["--timeout", str(args.timeout)])
        
        # 添加重试机制
        if args.rerun:
            cmd.extend(["--reruns", str(args.rerun), "--reruns-delay", "1"])
        
        # 添加报告生成
        if args.report:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            report_file = self.reports_dir / f"report_{timestamp}.html"
            cmd.extend([
                "--html", str(report_file),
                "--self-contained-html"
            ])
            
            # JSON报告
            json_file = self.reports_dir / f"report_{timestamp}.json"
            cmd.extend([
                "--json-report",
                "--json-report-file", str(json_file)
            ])
        
        # 添加覆盖率
        if args.coverage:
            cmd.extend([
                "--cov=src",
                "--cov-report=html:" + str(self.reports_dir / "coverage"),
                "--cov-report=xml:" + str(self.reports_dir / "coverage.xml"),
                "--cov-report=term-missing"
            ])
        
        # 添加环境变量
        env = os.environ.copy()
        env["TEST_ENV"] = args.env or "test"
        
        # 执行测试
        print(f"执行命令: {' '.join(cmd)}")
        print(f"环境: {env['TEST_ENV']}")
        
        result = subprocess.run(cmd, cwd=self.base_dir, env=env)
        
        return result.returncode
    
    def run_smoke_tests(self, args):
        """运行冒烟测试"""
        cmd = [
            "python", "-m", "pytest",
            "tests/api", "tests/e2e",
            "-v", "--tb=short",
            "-m", "smoke",
            "--color=yes"
        ]
        
        if args.parallel:
            cmd.extend(["-n", "auto"])
        
        env = os.environ.copy()
        env["TEST_ENV"] = args.env or "test"
        
        result = subprocess.run(cmd, cwd=self.base_dir, env=env)
        return result.returncode
    
    def run_regression_tests(self, args):
        """运行回归测试"""
        cmd = [
            "python", "-m", "pytest",
            "tests/api", "tests/e2e", "tests/integration",
            "-v", "--tb=short",
            "-m", "regression",
            "--color=yes"
        ]
        
        if args.parallel:
            num_workers = args.workers or 4
            cmd.extend(["-n", str(num_workers)])
        
        if args.report:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            report_file = self.reports_dir / f"regression_report_{timestamp}.html"
            cmd.extend(["--html", str(report_file), "--self-contained-html"])
        
        env = os.environ.copy()
        env["TEST_ENV"] = args.env or "test"
        
        result = subprocess.run(cmd, cwd=self.base_dir, env=env)
        return result.returncode
    
    def run_performance_tests(self, args):
        """运行性能测试"""
        cmd = [
            "python", "-m", "pytest",
            "tests/performance",
            "-v", "--tb=short",
            "-m", "performance",
            "--benchmark-only",
            "--benchmark-json", str(self.reports_dir / "benchmark.json"),
            "--color=yes"
        ]
        
        env = os.environ.copy()
        env["TEST_ENV"] = args.env or "test"
        
        result = subprocess.run(cmd, cwd=self.base_dir, env=env)
        return result.returncode
    
    def run_security_tests(self, args):
        """运行安全测试"""
        cmd = [
            "python", "-m", "pytest",
            "tests/security",
            "-v", "--tb=short",
            "-m", "security",
            "--color=yes"
        ]
        
        if args.report:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            report_file = self.reports_dir / f"security_report_{timestamp}.html"
            cmd.extend(["--html", str(report_file), "--self-contained-html"])
        
        env = os.environ.copy()
        env["TEST_ENV"] = args.env or "test"
        
        result = subprocess.run(cmd, cwd=self.base_dir, env=env)
        return result.returncode


def main():
    parser = argparse.ArgumentParser(description="测试运行器")
    parser.add_argument("mode", choices=["all", "smoke", "regression", "performance", "security"],
                       default="all", help="测试模式")
    parser.add_argument("--path", nargs="+", help="测试路径")
    parser.add_argument("--env", choices=["dev", "test", "staging", "prod"],
                       default="test", help="测试环境")
    parser.add_argument("--markers", help="标记过滤")
    parser.add_argument("--parallel", action="store_true", help="并行测试")
    parser.add_argument("--workers", type=int, help="并行工作进程数")
    parser.add_argument("--timeout", type=int, help="测试超时时间(秒)")
    parser.add_argument("--rerun", type=int, help="失败重试次数")
    parser.add_argument("--report", action="store_true", help="生成报告")
    parser.add_argument("--coverage", action="store_true", help="生成覆盖率报告")
    
    args = parser.parse_args()
    
    runner = TestRunner()
    
    if args.mode == "smoke":
        return runner.run_smoke_tests(args)
    elif args.mode == "regression":
        return runner.run_regression_tests(args)
    elif args.mode == "performance":
        return runner.run_performance_tests(args)
    elif args.mode == "security":
        return runner.run_security_tests(args)
    else:
        return runner.run_tests(args)


if __name__ == "__main__":
    sys.exit(main())
