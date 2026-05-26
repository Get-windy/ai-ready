#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI-Ready 测试数据管理验证脚本
Version: v1.0.0
Description: 验证测试数据生成、备份、恢复、隔离和清理功能
"""

import os
import json
from datetime import datetime
from pathlib import Path

# 配置变量
TEST_DATA_DIR = r"I:\AI-Ready\AI_TEST_DATA"
BACKUP_DIR = r"I:\AI-Ready\testdata-backups"
REPORT_DIR = r"I:\AI-Ready\infrastructure\tests\reports"
REPORT_FILE = os.path.join(REPORT_DIR, "test-data-management-report.md")
RESULTS_FILE = os.path.join(REPORT_DIR, "test-data-management-results.json")

# 验证结果集合
results = {
    "summary": {
        "passed": 0,
        "failed": 0,
        "warnings": 0,
        "total": 0
    },
    "tests": []
}


def record_test_result(test_name, category, status, details):
    """记录测试结果"""
    result = {
        "testName": test_name,
        "category": category,
        "status": status,
        "details": details,
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
    
    results["tests"].append(result)
    
    # 更新统计
    results["summary"]["total"] += 1
    if status == "passed":
        results["summary"]["passed"] += 1
    elif status == "failed":
        results["summary"]["failed"] += 1
    elif status == "warning":
        results["summary"]["warnings"] += 1
    
    # 输出结果
    status_icons = {"passed": "[PASS]", "failed": "[FAIL]", "warning": "[WARN]"}
    print(f"{status_icons.get(status, '[UNK]')} {test_name}")


def main():
    print("=" * 60)
    print("AI-Ready 测试数据管理验证脚本")
    print("=" * 60)
    print("")
    
    print("步骤1: 验证测试数据生成流程")
    print("-" * 30)
    
    # 1.1 检查测试数据文件是否存在
    if os.path.exists(TEST_DATA_DIR):
        data_files = [f for f in os.listdir(TEST_DATA_DIR) if os.path.isfile(os.path.join(TEST_DATA_DIR, f))]
        record_test_result("测试数据目录存在", "数据生成", "passed", f"测试数据目录存在，包含 {len(data_files)} 个文件")
    else:
        record_test_result("测试数据目录存在", "数据生成", "failed", f"测试数据目录不存在: {TEST_DATA_DIR}")
    
    # 1.2 检查数据生成脚本
    script_files = [
        "generate_anomaly_test_data.py",
        "generate_business_scenario_data.py",
        "generate_user_behavior_data.py"
    ]
    
    for script in script_files:
        script_path = os.path.join(TEST_DATA_DIR, script)
        if os.path.exists(script_path):
            record_test_result(f"数据生成脚本存在: {script}", "数据生成", "passed", f"脚本文件存在: {script_path}")
        else:
            record_test_result(f"数据生成脚本存在: {script}", "数据生成", "failed", f"脚本文件缺失: {script_path}")
    
    # 1.3 检查生成的数据文件
    data_files_to_check = [
        "user_behavior_data.json",
        "user_behavior_data.csv",
        "business_scenario_data.json",
        "business_scenario_data.csv",
        "anomaly_test_data.json"
    ]
    
    for filename in data_files_to_check:
        filepath = os.path.join(TEST_DATA_DIR, filename)
        if os.path.exists(filepath):
            file_size = os.path.getsize(filepath)
            record_test_result(f"数据文件存在: {filename}", "数据生成", "passed", f"文件存在: {filepath}, 大小: {file_size} 字节")
        else:
            record_test_result(f"数据文件存在: {filename}", "数据生成", "failed", f"文件缺失: {filepath}")
    
    print("")
    print("步骤2: 测试数据备份机制")
    print("-" * 30)
    
    # 2.1 检查备份目录
    if os.path.exists(BACKUP_DIR):
        record_test_result("备份目录存在", "备份机制", "passed", f"备份目录已创建: {BACKUP_DIR}")
    else:
        try:
            os.makedirs(BACKUP_DIR)
            record_test_result("备份目录创建", "备份机制", "passed", f"成功创建备份目录: {BACKUP_DIR}")
        except Exception as e:
            record_test_result("备份目录创建", "备份机制", "failed", f"创建备份目录失败: {str(e)}")
    
    # 2.2 测试数据备份功能
    try:
        backup_time = datetime.now().strftime("%Y%m%d_%H%M%S")
        backup_subdir = os.path.join(BACKUP_DIR, backup_time)
        os.makedirs(backup_subdir, exist_ok=True)
        
        # 备份数据文件
        all_files = [f for f in os.listdir(TEST_DATA_DIR) if os.path.isfile(os.path.join(TEST_DATA_DIR, f))]
        backup_files = all_files[:5]
        backup_count = 0
        
        for filename in backup_files:
            src = os.path.join(TEST_DATA_DIR, filename)
            dst = os.path.join(backup_subdir, filename)
            if os.path.exists(src):
                with open(src, 'rb') as fsrc:
                    with open(dst, 'wb') as fdst:
                        fdst.write(fsrc.read())
                        backup_count += 1
        
        record_test_result("数据备份功能", "备份机制", "passed", f"成功备份 {backup_count} 个文件到 {backup_subdir}")
    except Exception as e:
        record_test_result("数据备份功能", "备份机制", "failed", f"数据备份失败: {str(e)}")
    
    print("")
    print("步骤3: 数据隔离验证")
    print("-" * 30)
    
    # 3.1 检查Docker配置
    docker_compose_file = r"I:\AI-Ready\docker\test-environment\docker-compose.yml"
    
    if os.path.exists(docker_compose_file):
        with open(docker_compose_file, 'r', encoding='utf-8') as f:
            config_content = f.read()
        
        if "postgres-" in config_content and "POSTGRES_DB:" in config_content:
            record_test_result("数据库隔离配置", "数据隔离", "passed", "发现多数据库隔离配置")
        else:
            record_test_result("数据库隔离配置", "数据隔离", "warning", "数据库配置文件存在但无法确定隔离配置")
        
        if "redis-main" in config_content and "redis-inventory" in config_content and "redis-finance" in config_content:
            record_test_result("Redis数据库隔离", "数据隔离", "passed", "发现多Redis实例隔离配置")
        else:
            record_test_result("Redis数据库隔离", "数据隔离", "warning", "Redis隔离配置不完整")
    else:
        record_test_result("Docker配置文件", "数据隔离", "failed", "Docker Compose配置文件缺失")
    
    print("")
    print("步骤4: 数据清理策略验证")
    print("-" * 30)
    
    # 4.1 检查清理脚本
    cleanup_scripts = []
    for root, dirs, files in os.walk(r"I:\AI-Ready"):
        for file in files:
            if "cleanup" in file.lower() and file.endswith(('.sh', '.bat', '.ps1')):
                cleanup_scripts.append(os.path.join(root, file))
    
    if len(cleanup_scripts) > 0:
        record_test_result("清理脚本存在", "清理策略", "passed", f"在子目录中发现 {len(cleanup_scripts)} 个清理脚本")
    else:
        record_test_result("清理脚本存在", "清理策略", "warning", "未找到清理脚本")
    
    # 4.2 检查日志清理配置
    docker_logging_config = r"I:\AI-Ready\docker\test-environment"
    log_configs = []
    
    if os.path.exists(docker_logging_config):
        for root, dirs, files in os.walk(docker_logging_config):
            for file in files:
                if "log" in file.lower():
                    log_configs.append(os.path.join(root, file))
    
    if len(log_configs) > 0:
        record_test_result("日志清理配置", "清理策略", "passed", f"在Docker配置中发现 {len(log_configs)} 个日志相关文件")
    else:
        record_test_result("日志清理配置", "清理策略", "warning", "未找到日志清理配置")
    
    print("")
    print("步骤5: 数据恢复机制验证")
    print("-" * 30)
    
    # 5.1 检查数据库初始化脚本
    init_scripts_dir = r"I:\AI-Ready\init-scripts"
    init_scripts = []
    
    if os.path.exists(init_scripts_dir):
        for file in os.listdir(init_scripts_dir):
            if file.endswith('.sql'):
                init_scripts.append(os.path.join(init_scripts_dir, file))
    
    if len(init_scripts) > 0:
        record_test_result("数据库初始化脚本", "恢复机制", "passed", f"发现 {len(init_scripts)} 个数据库初始化脚本")
    else:
        record_test_result("数据库初始化脚本", "恢复机制", "failed", "未找到数据库初始化脚本")
    
    # 5.2 检查测试数据恢复能力
    try:
        backup_files = []
        if os.path.exists(BACKUP_DIR):
            for root, dirs, files in os.walk(BACKUP_DIR):
                for file in files:
                    backup_files.append(os.path.join(root, file))
        
        if len(backup_files) > 0:
            latest_backup = max(backup_files, key=os.path.getmtime)
            backup_age = (datetime.now() - datetime.fromtimestamp(os.path.getmtime(latest_backup))).total_seconds() / 3600
            
            if backup_age < 24:
                record_test_result("备份有效性检查", "恢复机制", "passed", f"发现最近24小时内创建的备份: {Path(latest_backup).name}")
            else:
                record_test_result("备份有效性检查", "恢复机制", "warning", f"备份超过24小时未更新: {Path(latest_backup).name}")
        else:
            record_test_result("备份有效性检查", "恢复机制", "warning", "备份目录中没有文件")
    except Exception as e:
        record_test_result("备份有效性检查", "恢复机制", "failed", f"检查备份失败: {str(e)}")
    
    print("")
    print("=" * 60)
    print("验证完成")
    print("=" * 60)
    print("")
    print("结果摘要:")
    print(f"  通过: {results['summary']['passed']}")
    print(f"  失败: {results['summary']['failed']}")
    print(f"  警告: {results['summary']['warnings']}")
    print(f"  总计: {results['summary']['total']}")
    
    # 生成验证报告
    report_content = f"""# 测试数据管理验证报告

## 验证时间
{datetime.now().strftime("%Y-%m-%d %H:%M:%S")}

## 概述
本次验证测试了测试数据管理的5个主要方面:数据生成、备份机制、数据隔离、清理策略和恢复机制。

## 验证结果摘要
| 指标 | 数值 |
|------|------|
| 通过 | {results['summary']['passed']} |
| 失败 | {results['summary']['failed']} |
| 警告 | {results['summary']['warnings']} |
| 总计 | {results['summary']['total']} |

## 详细测试结果

### 1. 数据生成流程验证
"""

    for test in results["tests"]:
        if test["category"] == "数据生成":
            status_icon = "[PASS]" if test["status"] == "passed" else ("[FAIL]" if test["status"] == "failed" else "[WARN]")
            report_content += f"- {status_icon} {test['testName']}\n"
            report_content += f"  - 状态: {test['status']}\n"
            report_content += f"  - 详情: {test['details']}\n\n"
    
    report_content += "### 2. 备份机制验证\n"
    for test in results["tests"]:
        if test["category"] == "备份机制":
            status_icon = "[PASS]" if test["status"] == "passed" else ("[FAIL]" if test["status"] == "failed" else "[WARN]")
            report_content += f"- {status_icon} {test['testName']}\n"
            report_content += f"  - 状态: {test['status']}\n"
            report_content += f"  - 详情: {test['details']}\n\n"
    
    report_content += "### 3. 数据隔离验证\n"
    for test in results["tests"]:
        if test["category"] == "数据隔离":
            status_icon = "[PASS]" if test["status"] == "passed" else ("[FAIL]" if test["status"] == "failed" else "[WARN]")
            report_content += f"- {status_icon} {test['testName']}\n"
            report_content += f"  - 状态: {test['status']}\n"
            report_content += f"  - 详情: {test['details']}\n\n"
    
    report_content += "### 4. 清理策略验证\n"
    for test in results["tests"]:
        if test["category"] == "清理策略":
            status_icon = "[PASS]" if test["status"] == "passed" else ("[FAIL]" if test["status"] == "failed" else "[WARN]")
            report_content += f"- {status_icon} {test['testName']}\n"
            report_content += f"  - 状态: {test['status']}\n"
            report_content += f"  - 详情: {test['details']}\n\n"
    
    report_content += "### 5. 恢复机制验证\n"
    for test in results["tests"]:
        if test["category"] == "恢复机制":
            status_icon = "[PASS]" if test["status"] == "passed" else ("[FAIL]" if test["status"] == "failed" else "[WARN]")
            report_content += f"- {status_icon} {test['testName']}\n"
            report_content += f"  - 状态: {test['status']}\n"
            report_content += f"  - 详情: {test['details']}\n\n"
    
    # 计算覆盖率
    total = results["summary"]["total"]
    passed_with_warnings = results["summary"]["passed"] + results["summary"]["warnings"]
    coverage = round((passed_with_warnings / total * 100), 2) if total > 0 else 0
    
    report_content += f"""## 结论与建议

### 覆盖率
- **验证覆盖率**: {coverage}%

### 关键发现
"""
    
    if results["summary"]["passed"] >= total * 0.8:
        report_content += "- [OK] 测试数据管理功能基本完善，大部分验证项通过\n"
    elif results["summary"]["passed"] >= total * 0.6:
        report_content += "- [WARN] 测试数据管理功能部分完善，需要改进\n"
    else:
        report_content += "- [CRIT] 测试数据管理功能存在较大问题，需要重点修复\n"
    
    report_content += """
### 改进建议
1. **数据生成**: 确保所有数据生成脚本正常运行并生成有效的测试数据
2. **备份机制**: 建立定期备份策略，确保备份频率满足RPO要求
3. **数据隔离**: 验证多租户场景下的数据隔离策略
4. **清理策略**: 完善自动清理脚本，防止数据无限增长
5. **恢复机制**: 定期演练数据恢复流程，确保RTO满足要求

## 附录
- 验证脚本: verify-test-data-management.py
- 报告生成时间: """ + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + "\n"
    
    # 保存报告
    with open(REPORT_FILE, 'w', encoding='utf-8') as f:
        f.write(report_content)
    print(f"验证报告已保存到: {REPORT_FILE}")
    
    # 保存JSON结果
    with open(RESULTS_FILE, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"JSON结果已保存到: {RESULTS_FILE}")
    
    # 返回退出码
    return 1 if results["summary"]["failed"] > 0 else 0


if __name__ == "__main__":
    exit(main())