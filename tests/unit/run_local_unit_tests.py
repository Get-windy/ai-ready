#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 本地单元测试执行脚本
用于本地开发环境执行所有单元测试
"""

import pytest
import os
import sys
import json
from datetime import datetime
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent.parent
sys.path.insert(0, str(project_root))


def run_unit_tests():
    """执行所有单元测试"""
    
    print("=" * 60)
    print("AI-Ready 本地单元测试")
    print(f"执行时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    # 测试目录
    unit_test_dir = Path(__file__).parent
    
    # 收集测试文件
    test_files = list(unit_test_dir.glob("test_*.py"))
    print(f"\n发现测试文件: {len(test_files)} 个")
    for f in test_files:
        print(f"  - {f.name}")
    
    # pytest参数
    pytest_args = [
        str(unit_test_dir),
        "-v",  # 详细输出
        "-m", "unit",  # 只运行unit标记的测试
        "--tb=short",  # 简短的traceback
        "--strict-markers",
        "-q",  # 简洁输出
        f"--junitxml={unit_test_dir.parent / 'reports' / 'junit-unit.xml'}",
        f"--html={unit_test_dir.parent / 'reports' / 'unit-test-report.html'}",
        "--self-contained-html",
    ]
    
    # 执行测试
    exit_code = pytest.main(pytest_args)
    
    return exit_code


def generate_summary_report():
    """生成测试摘要报告"""
    
    report_dir = Path(__file__).parent.parent / 'reports'
    report_dir.mkdir(exist_ok=True)
    
    summary = {
        "test_run": {
            "timestamp": datetime.now().isoformat(),
            "environment": "local",
            "test_type": "unit"
        },
        "test_suites": [
            {
                "name": "用户模块单元测试",
                "file": "test_user_unit.py",
                "test_classes": [
                    "TestUserModel",
                    "TestUserAuthentication", 
                    "TestUserValidation",
                    "TestUserStatus",
                    "TestUserTenant"
                ]
            },
            {
                "name": "权限模块单元测试",
                "file": "test_permission_unit.py",
                "test_classes": [
                    "TestRoleModel",
                    "TestPermissionCheck",
                    "TestMenuPermission",
                    "TestMenuModel",
                    "TestRoleAssignment",
                    "TestDataScope"
                ]
            },
            {
                "name": "ERP模块单元测试",
                "file": "test_erp_unit.py",
                "test_classes": [
                    "TestProductModel",
                    "TestStockModel",
                    "TestWarehouseModel",
                    "TestAccountModel",
                    "TestPurchaseOrder",
                    "TestDepreciation",
                    "TestERPReport"
                ]
            },
            {
                "name": "CRM模块单元测试",
                "file": "test_crm_unit.py",
                "test_classes": [
                    "TestCustomerModel",
                    "TestLeadModel",
                    "TestOpportunityModel",
                    "TestActivityModel",
                    "TestCRMReport",
                    "TestCRMValidation",
                    "TestCRMIntegration"
                ]
            }
        ],
        "mock_services": [
            "MockDatabase",
            "MockAuthService",
            "MockPermissionService",
            "MockERPService",
            "MockCRMService",
            "MockNotificationService"
        ],
        "test_coverage": {
            "user_module": "创建、查询、更新、删除、认证、验证、状态、租户隔离",
            "permission_module": "角色管理、权限检查、菜单权限、数据权限",
            "erp_module": "产品、库存、仓库、会计科目、采购订单、折旧计算",
            "crm_module": "客户、线索、商机、活动、报表、验证、集成流程"
        },
        "dependencies": {
            "external_services": "None - 所有外部依赖已Mock",
            "database": "SQLite内存数据库/Mock",
            "api": "Mock服务"
        }
    }
    
    # 保存摘要
    summary_path = report_dir / 'unit-test-summary.json'
    with open(summary_path, 'w', encoding='utf-8') as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    
    print(f"\n测试摘要已保存: {summary_path}")
    
    return summary


def print_usage():
    """打印使用说明"""
    print("\n使用说明:")
    print("-" * 40)
    print("1. 执行所有单元测试:")
    print("   python run_local_unit_tests.py")
    print("")
    print("2. 执行单个测试文件:")
    print("   pytest tests/unit/test_user_unit.py -v")
    print("")
    print("3. 执行特定测试类:")
    print("   pytest tests/unit/test_user_unit.py::TestUserModel -v")
    print("")
    print("4. 执行带标记的测试:")
    print("   pytest tests/unit -m unit -v")
    print("")
    print("5. 生成覆盖率报告:")
    print("   pytest tests/unit --cov=. --cov-report=html")
    print("-" * 40)


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='AI-Ready本地单元测试')
    parser.add_argument('--summary', action='store_true', help='只生成摘要报告')
    parser.add_argument('--usage', action='store_true', help='显示使用说明')
    
    args = parser.parse_args()
    
    if args.usage:
        print_usage()
        sys.exit(0)
    
    if args.summary:
        generate_summary_report()
        sys.exit(0)
    
    # 执行测试
    exit_code = run_unit_tests()
    
    # 生成摘要
    generate_summary_report()
    
    print("\n" + "=" * 60)
    if exit_code == 0:
        print("✅ 所有测试通过!")
    else:
        print("❌ 存在测试失败，请检查报告")
    print("=" * 60)
    
    sys.exit(exit_code)