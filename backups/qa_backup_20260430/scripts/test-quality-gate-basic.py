#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
质量门禁检查系统基本功能测试
版本: 1.0
创建日期: 2026-04-27
作者: qa-lead
"""

import os
import sys
import json
import subprocess
import tempfile
from datetime import datetime

def test_script_exists():
    """测试检查脚本是否存在"""
    print("测试1: 检查脚本是否存在")
    
    scripts = [
        "quality-gate-check-server-resources.py",
        "run-quality-gate-checks.py"
    ]
    
    all_exist = True
    for script in scripts:
        script_path = os.path.join(os.path.dirname(__file__), script)
        if os.path.exists(script_path):
            print(f"  [OK] {script} 存在")
        else:
            print(f"  [FAIL] {script} 不存在")
            all_exist = False
    
    return all_exist

def test_python_dependencies():
    """测试Python依赖"""
    print("\n测试2: 检查Python依赖")
    
    dependencies = ["psutil", "requests", "yaml"]
    
    all_installed = True
    for dep in dependencies:
        try:
            if dep == "yaml":
                import yaml
            elif dep == "psutil":
                import psutil
            elif dep == "requests":
                import requests
            print(f"  [OK] {dep} 已安装")
        except ImportError:
            print(f"  [WARN] {dep} 未安装")
            all_installed = False
    
    return all_installed

def test_server_resources_check():
    """测试服务器资源检查"""
    print("\n测试3: 运行服务器资源检查")
    
    script_path = os.path.join(os.path.dirname(__file__), "quality-gate-check-server-resources.py")
    
    if not os.path.exists(script_path):
        print("  [FAIL] 检查脚本不存在")
        return False
    
    try:
        # 创建临时输出文件
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as tmp:
            output_file = tmp.name
        
        # 运行检查脚本
        cmd = [sys.executable, script_path]
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=30
        )
        
        if result.returncode == 0:
            print("  [OK] 服务器资源检查执行成功")
            
            # 尝试解析输出（如果有JSON输出）
            try:
                if os.path.exists(output_file):
                    with open(output_file, 'r') as f:
                        data = json.load(f)
                    print(f"    检查项数: {len(data.get('results', []))}")
                    print(f"    通过数: {data.get('summary', {}).get('passed', 0)}")
            except:
                pass
                
            os.unlink(output_file)
            return True
        else:
            print(f"  [FAIL] 服务器资源检查失败，返回码: {result.returncode}")
            print(f"     错误输出: {result.stderr[:200]}")
            return False
            
    except subprocess.TimeoutExpired:
        print("  [FAIL] 服务器资源检查超时")
        return False
    except Exception as e:
        print(f"  [FAIL] 服务器资源检查异常: {e}")
        return False

def test_quality_gate_executor():
    """测试质量门禁执行器"""
    print("\n测试4: 测试质量门禁执行器")
    
    script_path = os.path.join(os.path.dirname(__file__), "run-quality-gate-checks.py")
    
    if not os.path.exists(script_path):
        print("  [FAIL] 执行器脚本不存在")
        return False
    
    try:
        # 运行快速检查
        cmd = [sys.executable, script_path, "--quick", "--html"]
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=60
        )
        
        if result.returncode in [0, 1]:  # 0=通过，1=有警告/失败
            print("  [OK] 质量门禁执行器运行成功")
            
            # 检查报告文件
            report_dir = "quality-gate-results"
            if os.path.exists(report_dir):
                reports = os.listdir(report_dir)
                print(f"    生成报告数: {len(reports)}")
                
                # 检查HTML报告
                html_reports = [f for f in reports if f.endswith('.html')]
                if html_reports:
                    print(f"    HTML报告: {html_reports[0]}")
                
                # 检查JSON报告
                json_reports = [f for f in reports if f.endswith('.json')]
                if json_reports:
                    print(f"    JSON报告: {json_reports[0]}")
            
            return True
        else:
            print(f"  [FAIL] 质量门禁执行器失败，返回码: {result.returncode}")
            print(f"     错误输出: {result.stderr[:200]}")
            return False
            
    except subprocess.TimeoutExpired:
        print("  [FAIL] 质量门禁执行器超时")
        return False
    except Exception as e:
        print(f"  [FAIL] 质量门禁执行器异常: {e}")
        return False

def test_documentation():
    """测试文档完整性"""
    print("\n测试5: 检查文档完整性")
    
    docs = [
        "../quality-checklists/sprint27-plus1-quality-gate-checklist.md",
        "../quality-checklists/quality-gate-installation-guide.md",
        "../test-reports/2026-04-27-sprint27-plus1-quality-gate-checklist-delivery.md"
    ]
    
    all_exist = True
    for doc in docs:
        doc_path = os.path.join(os.path.dirname(__file__), doc)
        if os.path.exists(doc_path):
            size = os.path.getsize(doc_path)
            print(f"  [OK] {doc} 存在 ({size} 字节)")
        else:
            print(f"  [FAIL] {doc} 不存在")
            all_exist = False
    
    return all_exist

def generate_test_report(results):
    """生成测试报告"""
    print("\n" + "=" * 60)
    print("质量门禁检查系统测试报告")
    print("=" * 60)
    
    total_tests = len(results)
    passed_tests = sum(1 for r in results if r["passed"])
    
    print(f"\n测试统计:")
    print(f"  总测试数: {total_tests}")
    print(f"  通过数: {passed_tests}")
    print(f"  失败数: {total_tests - passed_tests}")
    print(f"  通过率: {passed_tests/total_tests*100:.1f}%")
    
    print(f"\n详细结果:")
    for result in results:
        status = "[OK]" if result["passed"] else "[FAIL]"
        print(f"  {status} {result['name']}: {'通过' if result['passed'] else '失败'}")
        if not result["passed"] and "message" in result:
            print(f"     原因: {result['message']}")
    
    print(f"\n总体评估:")
    if passed_tests == total_tests:
        print("  [OK] 所有测试通过，质量门禁检查系统准备就绪")
        return 0
    elif passed_tests >= total_tests * 0.8:
        print("  [WARN] 大部分测试通过，系统基本可用")
        return 1
    else:
        print("  [FAIL] 测试失败较多，需要修复")
        return 2

def main():
    """主测试函数"""
    print("=" * 60)
    print("Sprint 27+1 质量门禁检查系统基本功能测试")
    print("=" * 60)
    print(f"测试时间: {datetime.now().isoformat()}")
    print(f"工作目录: {os.getcwd()}")
    print(f"Python版本: {sys.version}")
    print("=" * 60)
    
    # 执行所有测试
    test_results = []
    
    # 测试1: 脚本存在性
    test1_result = test_script_exists()
    test_results.append({
        "name": "脚本存在性测试",
        "passed": test1_result
    })
    
    # 测试2: 依赖检查
    test2_result = test_python_dependencies()
    test_results.append({
        "name": "Python依赖检查",
        "passed": test2_result
    })
    
    # 测试3: 服务器资源检查
    test3_result = test_server_resources_check()
    test_results.append({
        "name": "服务器资源检查测试",
        "passed": test3_result
    })
    
    # 测试4: 质量门禁执行器
    test4_result = test_quality_gate_executor()
    test_results.append({
        "name": "质量门禁执行器测试",
        "passed": test4_result
    })
    
    # 测试5: 文档完整性
    test5_result = test_documentation()
    test_results.append({
        "name": "文档完整性测试",
        "passed": test5_result
    })
    
    # 生成测试报告
    exit_code = generate_test_report(test_results)
    
    # 保存测试报告
    report_data = {
        "test_time": datetime.now().isoformat(),
        "environment": {
            "python_version": sys.version,
            "platform": sys.platform,
            "cwd": os.getcwd()
        },
        "results": test_results,
        "summary": {
            "total_tests": len(test_results),
            "passed": sum(1 for r in test_results if r["passed"]),
            "failed": sum(1 for r in test_results if not r["passed"])
        }
    }
    
    # 确保报告目录存在
    os.makedirs("test-reports", exist_ok=True)
    
    # 保存JSON报告
    report_file = f"test-reports/quality-gate-system-test-{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    with open(report_file, 'w', encoding='utf-8') as f:
        json.dump(report_data, f, ensure_ascii=False, indent=2)
    
    print(f"\n测试报告已保存到: {report_file}")
    print("=" * 60)
    
    sys.exit(exit_code)

if __name__ == "__main__":
    main()