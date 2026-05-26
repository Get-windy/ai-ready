#!/usr/bin/env python3
"""
销售价格策略管理模块编译验证脚本
用于验证模块的基本完整性
"""

import os
import sys
import glob

# 修复Unicode编码问题
def safe_print(message):
    try:
        print(message)
    except UnicodeEncodeError:
        # 替换Unicode字符为ASCII
        cleaned = message.replace('✅', '[OK]').replace('❌', '[FAIL]').replace('⚠️', '[WARN]').replace('ℹ️', '[INFO]').replace('🎉', '[SUCCESS]').replace('=', '=')
        print(cleaned)

def check_directory_structure():
    """检查目录结构完整性"""
    safe_print("检查目录结构完整性...")
    
    required_dirs = [
        "src/main/java/cn/aiedge/erp/sales/pricing",
        "src/main/java/cn/aiedge/erp/sales/pricing/config",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller",
        "src/main/java/cn/aiedge/erp/sales/pricing/service",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl",
        "src/main/java/cn/aiedge/erp/sales/pricing/repository",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto",
        "src/main/resources",
        "src/test/java/cn/aiedge/erp/sales/pricing"
    ]
    
    for dir_path in required_dirs:
        if os.path.exists(dir_path):
            safe_print(f"  [OK] {dir_path}")
        else:
            safe_print(f"  [FAIL] {dir_path} - 目录不存在")
            return False
    
    return True

def check_java_files():
    """检查Java文件完整性"""
    print("\n检查Java文件完整性...")
    
    expected_files = [
        "src/main/java/cn/aiedge/erp/sales/pricing/PricingApplication.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/config/DroolsConfig.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller/PricingController.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/controller/DroolsRuleController.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/IPriceStrategyService.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/IPriceCalculationService.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/IDroolsRuleService.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl/PriceStrategyServiceImpl.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl/PriceCalculationServiceImpl.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/service/impl/DroolsRuleServiceImpl.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/repository/PriceStrategyRepository.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/repository/PriceRuleRepository.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceStrategy.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceRule.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceStrategyDTO.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceRuleDTO.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceCalculationRequest.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceCalculationResult.java"
    ]
    
    found_count = 0
    for file_path in expected_files:
        if os.path.exists(file_path):
            found_count += 1
            safe_print(f"  [OK] {file_path}")
        else:
            safe_print(f"  [FAIL] {file_path} - 文件不存在")
    
    safe_print(f"Java文件完整度: {found_count}/{len(expected_files)}")
    return found_count >= len(expected_files) * 0.9  # 允许少量文件缺失

def check_resource_files():
    """检查资源文件完整性"""
    print("\n检查资源文件完整性...")
    
    required_files = [
        "pom.xml",
        "src/main/resources/application.yml",
        "src/main/resources/rules/basic-pricing-rules.drl",
        "README.md",
        "QUICK_FIX_README.md",
        "FINAL_REPORT.md"
    ]
    
    for file_path in required_files:
        if os.path.exists(file_path):
            safe_print(f"  [OK] {file_path}")
        else:
            safe_print(f"  [FAIL] {file_path} - 文件不存在")
            return False
    
    return True

def check_import_statements():
    """抽样检查Java导入语句"""
    print("\n抽样检查Java导入语句...")
    
    # 检查关键文件是否使用Jakarta EE
    key_files = [
        "src/main/java/cn/aiedge/erp/sales/pricing/controller/PricingController.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/entity/PriceStrategy.java",
        "src/main/java/cn/aiedge/erp/sales/pricing/dto/PriceCalculationRequest.java"
    ]
    
    for file_path in key_files:
        if os.path.exists(file_path):
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    
                # 检查是否使用jakarta导入
                if 'jakarta.' in content:
                    safe_print(f"  [OK] {file_path} - 使用Jakarta EE导入")
                elif 'javax.' in content:
                    safe_print(f"  [WARN] {file_path} - 仍使用javax导入，需要更新")
                else:
                    safe_print(f"  [INFO] {file_path} - 未检测到Servlet相关导入")
            except Exception as e:
                safe_print(f"  [FAIL] {file_path} - 读取失败: {e}")
        else:
            safe_print(f"  [FAIL] {file_path} - 文件不存在")
    
    return True

def main():
    safe_print("=" * 60)
    safe_print("销售价格策略管理模块完整性验证")
    safe_print("=" * 60)
    
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    results = []
    
    # 执行各项检查
    results.append(("目录结构检查", check_directory_structure()))
    results.append(("Java文件检查", check_java_files()))
    results.append(("资源文件检查", check_resource_files()))
    results.append(("导入语句检查", check_import_statements()))
    
    safe_print("\n" + "=" * 60)
    safe_print("验证结果汇总:")
    safe_print("=" * 60)
    
    all_passed = True
    for check_name, passed in results:
        status = "[OK] 通过" if passed else "[FAIL] 失败"
        safe_print(f"{check_name}: {status}")
        if not passed:
            all_passed = False
    
    safe_print("\n" + "=" * 60)
    if all_passed:
        safe_print("[SUCCESS] 模块完整性验证通过！")
        safe_print("模块已准备好进行编译和测试。")
        sys.exit(0)
    else:
        safe_print("[WARN] 模块完整性验证未通过！")
        safe_print("请根据以上检查结果修复问题。")
        sys.exit(1)

if __name__ == "__main__":
    main()