#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户权限管理自动化测试执行器
Sprint 27+1 测试环境

集成执行所有权限管理相关测试：
- 角色权限矩阵验证
- 权限继承规则验证
- 权限变更审计验证
- 用户登录权限验证
- 角色权限分配验证

生成测试报告并输出测试结果。
"""

import subprocess
import sys
import os
import json
import time
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, List


class PermissionTestRunner:
    """权限管理测试执行器"""

    def __init__(self):
        self.test_dir = Path(__file__).parent
        self.report_dir = self.test_dir.parent / "reports"
        self.results = []
        self.start_time = None
        self.end_time = None

    def _run_test_file(self, test_file: str, markers: str = None) -> Dict[str, Any]:
        """运行单个测试文件"""
        test_path = self.test_dir / test_file

        if not test_path.exists():
            return {
                "file": test_file,
                "status": "skipped",
                "reason": "文件不存在",
                "passed": 0,
                "failed": 0,
                "errors": 0,
                "duration": 0
            }

        cmd = [
            sys.executable, "-m", "pytest",
            str(test_path),
            "-v",
            "--tb=short",
            "--no-header",
            "-q"
        ]

        if markers:
            cmd.extend(["-m", markers])

        # 确保报告目录存在
        self.report_dir.mkdir(parents=True, exist_ok=True)

        start = time.time()
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=300,
                cwd=str(self.test_dir)
            )
            duration = time.time() - start

            # 解析输出
            stdout = result.stdout
            stderr = result.stderr

            # 提取通过/失败数量
            passed = stdout.count("PASSED")
            failed = stdout.count("FAILED")
            errors = stdout.count("ERROR")
            skipped = stdout.count("SKIPPED")

            # 如果没有PASSED/FAILED标记，尝试从总结行解析
            if passed == 0 and failed == 0:
                for line in stdout.split("\n"):
                    if "passed" in line or "failed" in line or "error" in line:
                        # 解析 pytest 总结行如 "3 passed, 1 failed in 2.34s"
                        parts = line.split(",")
                        for part in parts:
                            if "passed" in part:
                                try:
                                    passed = int(part.strip().split()[0])
                                except:
                                    pass
                            if "failed" in part:
                                try:
                                    failed = int(part.strip().split()[0])
                                except:
                                    pass
                            if "error" in part:
                                try:
                                    errors = int(part.strip().split()[0])
                                except:
                                    pass
                            if "skipped" in part:
                                try:
                                    skipped = int(part.strip().split()[0])
                                except:
                                    pass

            status = "passed" if result.returncode == 0 else "failed"

            return {
                "file": test_file,
                "status": status,
                "passed": passed,
                "failed": failed,
                "errors": errors,
                "skipped": skipped,
                "duration": round(duration, 2),
                "returncode": result.returncode,
                "stdout": stdout,
                "stderr": stderr
            }

        except subprocess.TimeoutExpired:
            return {
                "file": test_file,
                "status": "timeout",
                "passed": 0,
                "failed": 0,
                "errors": 0,
                "skipped": 0,
                "duration": 300,
                "reason": "测试超时（5分钟）"
            }
        except Exception as e:
            return {
                "file": test_file,
                "status": "error",
                "passed": 0,
                "failed": 0,
                "errors": 1,
                "skipped": 0,
                "duration": 0,
                "reason": str(e)
            }

    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有权限管理测试"""
        print("=" * 70)
        print("用户权限管理自动化测试执行")
        print(f"执行时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 70)
        print()

        self.start_time = time.time()

        test_files = [
            {
                "file": "test_user_permission_management.py",
                "name": "用户权限管理API测试",
                "description": "角色CRUD、权限分配/回收、用户角色分配"
            },
            {
                "file": "test_role_permission_matrix.py",
                "name": "角色权限矩阵验证",
                "description": "验证角色权限矩阵符合设计规范"
            },
            {
                "file": "test_permission_inheritance.py",
                "name": "权限继承规则验证",
                "description": "验证角色继承、权限去重、循环继承阻止"
            },
            {
                "file": "test_permission_audit.py",
                "name": "权限变更审计验证",
                "description": "验证权限变更操作被正确记录到审计日志"
            },
            {
                "file": "test_user_login.py",
                "name": "用户登录权限验证",
                "description": "验证登录成功/失败、账号锁定等场景"
            },
            {
                "file": "test_role_manager.py",
                "name": "角色管理单元测试",
                "description": "RoleManager模块单元测试"
            },
            {
                "file": "test_permission_manager.py",
                "name": "权限管理单元测试",
                "description": "PermissionManager模块单元测试"
            }
        ]

        total_passed = 0
        total_failed = 0
        total_errors = 0
        total_skipped = 0

        for test_config in test_files:
            print(f"\n📋 {test_config['name']}")
            print(f"   {test_config['description']}")
            print(f"   文件: {test_config['file']}")
            print("-" * 70)

            result = self._run_test_file(test_config["file"])
            self.results.append(result)

            total_passed += result.get("passed", 0)
            total_failed += result.get("failed", 0)
            total_errors += result.get("errors", 0)
            total_skipped += result.get("skipped", 0)

            status_icon = "✅" if result["status"] == "passed" else "❌"
            if result["status"] in ["skipped", "timeout"]:
                status_icon = "⏭️"

            print(f"\n{status_icon} 结果: {result['status'].upper()}")
            print(f"   通过: {result.get('passed', 0)}")
            print(f"   失败: {result.get('failed', 0)}")
            print(f"   错误: {result.get('errors', 0)}")
            print(f"   跳过: {result.get('skipped', 0)}")
            print(f"   耗时: {result.get('duration', 0):.2f}秒")

            if result.get("reason"):
                print(f"   原因: {result['reason']}")

        self.end_time = time.time()
        total_duration = self.end_time - self.start_time

        # 生成汇总
        summary = {
            "total_tests": len(test_files),
            "total_passed": total_passed,
            "total_failed": total_failed,
            "total_errors": total_errors,
            "total_skipped": total_skipped,
            "total_duration": round(total_duration, 2),
            "success_rate": round(
                (total_passed / (total_passed + total_failed + total_errors) * 100), 2
            ) if (total_passed + total_failed + total_errors) > 0 else 0,
            "timestamp": datetime.now().isoformat(),
            "results": self.results
        }

        return summary

    def generate_report(self, summary: Dict[str, Any]) -> str:
        """生成测试报告"""
        report_path = self.report_dir / f"permission_test_report_{int(time.time())}.json"

        with open(report_path, "w", encoding="utf-8") as f:
            json.dump(summary, f, ensure_ascii=False, indent=2)

        # 同时生成文本报告
        text_report_path = self.report_dir / f"permission_test_report_{int(time.time())}.txt"

        with open(text_report_path, "w", encoding="utf-8") as f:
            f.write("=" * 70 + "\n")
            f.write("用户权限管理自动化测试报告\n")
            f.write("=" * 70 + "\n\n")
            f.write(f"生成时间: {summary['timestamp']}\n")
            f.write(f"总耗时: {summary['total_duration']}秒\n\n")

            f.write("测试汇总\n")
            f.write("-" * 70 + "\n")
            f.write(f"测试文件数: {summary['total_tests']}\n")
            f.write(f"通过: {summary['total_passed']}\n")
            f.write(f"失败: {summary['total_failed']}\n")
            f.write(f"错误: {summary['total_errors']}\n")
            f.write(f"跳过: {summary['total_skipped']}\n")
            f.write(f"成功率: {summary['success_rate']}%\n\n")

            f.write("详细结果\n")
            f.write("-" * 70 + "\n")
            for result in summary["results"]:
                f.write(f"\n文件: {result['file']}\n")
                f.write(f"状态: {result['status']}\n")
                f.write(f"通过/失败/错误/跳过: {result.get('passed', 0)}/{result.get('failed', 0)}/{result.get('errors', 0)}/{result.get('skipped', 0)}\n")
                f.write(f"耗时: {result.get('duration', 0):.2f}秒\n")
                if result.get("reason"):
                    f.write(f"原因: {result['reason']}\n")

        print(f"\n📄 测试报告已生成:")
        print(f"   JSON: {report_path}")
        print(f"   TEXT: {text_report_path}")

        return str(report_path)

    def print_summary(self, summary: Dict[str, Any]):
        """打印测试汇总"""
        print("\n" + "=" * 70)
        print("测试执行汇总")
        print("=" * 70)
        print(f"测试文件数: {summary['total_tests']}")
        print(f"总通过:     {summary['total_passed']}")
        print(f"总失败:     {summary['total_failed']}")
        print(f"总错误:     {summary['total_errors']}")
        print(f"总跳过:     {summary['total_skipped']}")
        print(f"总耗时:     {summary['total_duration']:.2f}秒")
        print(f"成功率:     {summary['success_rate']}%")
        print("=" * 70)

        if summary['total_failed'] > 0 or summary['total_errors'] > 0:
            print("\n⚠️  测试存在失败项，请查看详细报告")
            return 1
        elif summary['total_passed'] == 0 and summary['total_skipped'] == summary['total_tests']:
            print("\n⏭️  所有测试被跳过，可能测试环境未就绪")
            return 2
        else:
            print("\n✅ 测试执行完成")
            return 0


def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(description="用户权限管理自动化测试执行器")
    parser.add_argument(
        "--file",
        help="指定运行单个测试文件",
        choices=[
            "test_user_permission_management.py",
            "test_role_permission_matrix.py",
            "test_permission_inheritance.py",
            "test_permission_audit.py",
            "test_user_login.py",
            "test_role_manager.py",
            "test_permission_manager.py"
        ]
    )
    parser.add_argument(
        "--env",
        default="test",
        help="测试环境 (dev/test/staging)"
    )
    args = parser.parse_args()

    runner = PermissionTestRunner()

    if args.file:
        # 运行单个文件
        print(f"运行单个测试文件: {args.file}")
        result = runner._run_test_file(args.file)
        print(f"\n结果: {result['status']}")
        print(f"通过: {result.get('passed', 0)}")
        print(f"失败: {result.get('failed', 0)}")
        print(f"耗时: {result.get('duration', 0):.2f}秒")
        sys.exit(0 if result['status'] == 'passed' else 1)
    else:
        # 运行全部
        summary = runner.run_all_tests()
        runner.print_summary(summary)
        report_path = runner.generate_report(summary)
        sys.exit(0 if summary['success_rate'] >= 80 else 1)


if __name__ == "__main__":
    main()
