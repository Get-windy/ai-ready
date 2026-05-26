#!/usr/bin/env python3
"""
简化版测试配置检查
"""

import os
import json
from pathlib import Path

def check_test_framework():
    """检查测试框架配置"""
    root = Path("I:/AI-Ready")
    tests_dir = root / "backend" / "tests"
    
    results = {
        "config_files": [],
        "test_directories": [],
        "scripts": [],
        "documents": [],
        "issues": [],
        "status": "unknown"
    }
    
    # 检查配置文件
    config_files = [
        "pytest.ini",
        "conftest.py"
    ]
    
    for config in config_files:
        config_path = tests_dir / config
        if config_path.exists():
            results["config_files"].append({
                "name": config,
                "exists": True,
                "size": config_path.stat().st_size
            })
        else:
            results["config_files"].append({
                "name": config,
                "exists": False
            })
            results["issues"].append(f"缺少配置文件: {config}")
    
    # 检查目录结构
    test_dirs = [
        "api",
        "unit",
        "integration",
        "e2e",
        "performance",
        "reports",
        "results",
        "scripts",
        "docs"
    ]
    
    for dir_name in test_dirs:
        dir_path = tests_dir / dir_name
        if dir_path.exists() and dir_path.is_dir():
            results["test_directories"].append({
                "name": dir_name,
                "exists": True
            })
        else:
            results["test_directories"].append({
                "name": dir_name,
                "exists": False
            })
            results["issues"].append(f"缺少测试目录: {dir_name}")
    
    # 检查脚本文件
    script_files = [
        "optimized-test-runner.sh",
        "test-execution-script.sh"
    ]
    
    scripts_dir = tests_dir / "scripts"
    for script in script_files:
        script_path = scripts_dir / script
        if script_path.exists():
            results["scripts"].append({
                "name": script,
                "exists": True,
                "size": script_path.stat().st_size
            })
        else:
            results["scripts"].append({
                "name": script,
                "exists": False
            })
            results["issues"].append(f"缺少脚本文件: {script}")
    
    # 检查文档
    doc_files = [
        "TEST_FRAMEWORK_MAINTENANCE.md",
        "api_automation_test_template.md"
    ]
    
    docs_dir = tests_dir / "docs"
    for doc in doc_files:
        doc_path = docs_dir / doc
        if doc_path.exists():
            results["documents"].append({
                "name": doc,
                "exists": True,
                "size": doc_path.stat().st_size
            })
        else:
            results["documents"].append({
                "name": doc,
                "exists": False
            })
            results["issues"].append(f"缺少文档: {doc}")
    
    # 确定状态
    if len(results["issues"]) == 0:
        results["status"] = "passed"
    elif any("缺少配置文件" in issue for issue in results["issues"]):
        results["status"] = "failed"
    else:
        results["status"] = "warning"
    
    return results

def main():
    """主函数"""
    print("测试框架配置检查")
    print("=" * 50)
    
    results = check_test_framework()
    
    # 打印结果
    print(f"\n配置文件:")
    for config in results["config_files"]:
        status = "[OK]" if config["exists"] else "[X] "
        size_info = f" ({config['size']} bytes)" if config.get("size") else ""
        print(f"  {status} {config['name']}{size_info}")
    
    print(f"\n测试目录:")
    for dir_info in results["test_directories"]:
        status = "[OK]" if dir_info["exists"] else "[X] "
        print(f"  {status} {dir_info['name']}")
    
    print(f"\n脚本文件:")
    for script in results["scripts"]:
        status = "[OK]" if script["exists"] else "[X] "
        size_info = f" ({script['size']} bytes)" if script.get("size") else ""
        print(f"  {status} {script['name']}{size_info}")
    
    print(f"\n文档文件:")
    for doc in results["documents"]:
        status = "[OK]" if doc["exists"] else "[X] "
        size_info = f" ({doc['size']} bytes)" if doc.get("size") else ""
        print(f"  {status} {doc['name']}{size_info}")
    
    # 打印问题
    if results["issues"]:
        print(f"\n发现的问题:")
        for issue in results["issues"]:
            print(f"  - {issue}")
    
    # 总结
    print(f"\n" + "=" * 50)
    print(f"状态: {results['status'].upper()}")
    
    # 保存结果
    report_path = Path("I:/AI-Ready/tests/test_config_check_report.json")
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print(f"报告已保存到: {report_path}")
    
    return 0 if results["status"] == "passed" else 1

if __name__ == "__main__":
    import sys
    sys.exit(main())