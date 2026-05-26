#!/usr/bin/env python3
"""
销售价格策略管理模块验证脚本
用于验证模块的基本结构和完整性
"""

import os
import sys

def check_directory_structure(base_path):
    """检查目录结构完整性"""
    required_dirs = [
        "src/main/java/cn/aiedge/erp/sales/pricing",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller",
        "src/main/java/cn/aiedge/erp/sales/pricing/service",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto",
        "src/main/java/cn/aiedge/erp/sales/pricing/repository",
        "src/main/java/cn/aiedge/erp/sales/pricing/config",
        "src/main/resources",
        "src/main/resources/rules",
        "src/test/java/cn/aiedge/erp/sales/pricing",
        "src/test/java/cn/aiedge/erp/sales/pricing/service",
    ]
    
    missing_dirs = []
    for dir_path in required_dirs:
        full_path = os.path.join(base_path, dir_path)
        if not os.path.exists(full_path):
            missing_dirs.append(dir_path)
    
    if missing_dirs:
        print(f"[ERROR] Missing directories: {missing_dirs}")
        return False
    else:
        print("[OK] Directory structure complete")
        return True

def check_java_files(base_path):
    """检查核心Java文件"""
    required_files = [
        "src/main/java/cn/aiedge/erp/sales/pricing/PricingApplication.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/config/DroolsConfig.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller/PricingController.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller/DroolsRuleController.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceStrategy.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceRule.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceStrategyDTO.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceRuleDTO.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceCalculationRequest.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceCalculationResult.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/IPriceStrategyService.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/IPriceCalculationService.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/IDroolsRuleService.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl/PriceStrategyServiceImpl.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl/PriceCalculationServiceImpl.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl/DroolsRuleServiceImpl.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/repository/PriceStrategyRepository.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/repository/PriceRuleRepository.java",
    ]
    
    missing_files = []
    for file_path in required_files:
        full_path = os.path.join(base_path, file_path)
        if not os.path.exists(full_path):
            missing_files.append(file_path)
    
    if missing_files:
        print(f"[ERROR] Missing Java files: {len(missing_files)}")
        for file in missing_files[:5]:  # Show first 5 only
            print(f"   - {file}")
        if len(missing_files) > 5:
            print(f"   ... and {len(missing_files)-5} more files")
        return False
    else:
        print(f"[OK] Java files complete ({len(required_files)} files)")
        return True

def check_resource_files(base_path):
    """检查资源文件"""
    required_resources = [
        "pom.xml",
        "README.md",
        "src/main/resources/application.yml",
        "src/main/resources/rules/basic-pricing-rules.drl",
    ]
    
    missing_resources = []
    for resource in required_resources:
        full_path = os.path.join(base_path, resource)
        if not os.path.exists(full_path):
            missing_resources.append(resource)
    
    if missing_resources:
        print(f"[ERROR] Missing resource files: {missing_resources}")
        return False
    else:
        print(f"[OK] Resource files complete ({len(required_resources)} files)")
        return True

def check_test_files(base_path):
    """检查测试文件"""
    test_files = [
        "src/test/java/cn/aiedge/erp/sales/pricing/PricingApplicationTest.java",
        "src/test/java/cn/aiedge/erp/sales/pricing/service/PriceStrategyServiceTest.java",
        "src/test/java/cn/aiedge/erp/sales/pricing/service/DroolsRuleServiceTest.java",
    ]
    
    missing_tests = []
    for test_file in test_files:
        full_path = os.path.join(base_path, test_file)
        if not os.path.exists(full_path):
            missing_tests.append(test_file)
    
    if missing_tests:
        print(f"[WARN] Missing test files: {len(missing_tests)}")
        return False
    else:
        print(f"[OK] Test framework established ({len(test_files)} files)")
        return True

def validate_imports(base_path):
    """验证Java导入语句"""
    print("\n[CHECK] Validating Java imports (key files)...")
    
    # 检查关键文件的导入是否正确
    files_to_check = [
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceStrategy.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceRule.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceCalculationRequest.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceStrategyDTO.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller/PricingController.java",
    ]
    
    import_issues = []
    
    for file_path in files_to_check:
        full_path = os.path.join(base_path, file_path)
        if not os.path.exists(full_path):
            continue
            
        try:
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
                # 检查是否还有javax.persistence导入
                if 'import javax.persistence' in content:
                    import_issues.append(f"{file_path}: Contains javax.persistence import")
                if 'import javax.validation' in content:
                    import_issues.append(f"{file_path}: Contains javax.validation import")
        except Exception as e:
            import_issues.append(f"{file_path}: Read failed - {str(e)}")
    
    if import_issues:
        print(f"[ERROR] Import issues: {len(import_issues)}")
        for issue in import_issues[:3]:
            print(f"   - {issue}")
        return False
    else:
        print("[OK] Key file imports correct (Jakarta EE standard)")
        return True

def calculate_module_metrics(base_path):
    """计算模块指标"""
    print("\n[METRICS] Module statistics:")
    
    # 统计Java文件数量
    java_count = 0
    for root, dirs, files in os.walk(os.path.join(base_path, "src/main/java")):
        java_count += len([f for f in files if f.endswith('.java')])
    
    # 统计测试文件数量
    test_count = 0
    for root, dirs, files in os.walk(os.path.join(base_path, "src/test/java")):
        test_count += len([f for f in files if f.endswith('.java')])
    
    # 统计资源文件数量
    resource_count = 0
    for root, dirs, files in os.walk(os.path.join(base_path, "src/main/resources")):
        resource_count += len(files)
    
    print(f"   Java source files: {java_count}")
    print(f"   Test files: {test_count}")
    print(f"   Resource files: {resource_count}")
    print(f"   Total files: {java_count + test_count + resource_count + 5}")  # +5 for pom.xml, etc
    
    return True

def main():
    """主函数"""
    print("=" * 60)
    print("Sales Price Strategy Module Integrity Validation")
    print("=" * 60)
    
    # 获取当前目录
    base_path = os.path.dirname(os.path.abspath(__file__))
    print(f"Validation directory: {base_path}\n")
    
    # 执行验证
    all_checks = []
    
    all_checks.append(check_directory_structure(base_path))
    all_checks.append(check_java_files(base_path))
    all_checks.append(check_resource_files(base_path))
    all_checks.append(check_test_files(base_path))
    all_checks.append(validate_imports(base_path))
    all_checks.append(calculate_module_metrics(base_path))
    
    # 汇总结果
    success_count = sum(all_checks)
    total_count = len(all_checks)
    
    print("\n" + "=" * 60)
    print(f"Validation result: {success_count}/{total_count} checks passed")
    
    if success_count == total_count:
        print("[SUCCESS] Module integrity validation passed!")
        return 0
    elif success_count >= total_count * 0.8:
        print("[WARNING] Module mostly complete, some minor issues")
        return 1
    else:
        print("[ERROR] Module integrity issues need fixing")
        return 2

if __name__ == "__main__":
    sys.exit(main())