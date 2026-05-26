#!/usr/bin/env python3
"""
erp-purchase模块结构检查脚本
检查采购询价/报价功能的关键文件存在性
"""

import os
import json
from datetime import datetime

def check_file_exists(file_path):
    """检查文件是否存在"""
    exists = os.path.exists(file_path)
    status = "[PASS]" if exists else "[FAIL]"
    print(f"检查文件: {file_path} {status}")
    return exists, file_path

def analyze_module_structure(project_path):
    """分析模块结构"""
    print("=" * 80)
    print("erp-purchase采购询价/报价功能模块结构检查")
    print("=" * 80)
    
    results = {
        "check_date": datetime.now().isoformat(),
        "project_path": project_path,
        "modules": {},
        "missing_files": [],
        "summary": {
            "total_checked": 0,
            "found": 0,
            "missing": 0,
            "coverage": 0
        }
    }
    
    # 关键类文件列表
    key_classes = [
        "src/main/java/cn/aiedge/erp/purchase/controller/PurchaseInquiryController.java",
        "src/main/java/cn/aiedge/erp/purchase/controller/PurchaseQuoteController.java",
        "src/main/java/cn/aiedge/erp/purchase/controller/PurchaseOrderController.java",
        "src/main/java/cn/aiedge/erp/purchase/service/PurchaseInquiryService.java",
        "src/main/java/cn/aiedge/erp/purchase/service/PurchaseQuoteService.java",
        "src/main/java/cn/aiedge/erp/purchase/service/PurchaseOrderService.java",
        "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseInquiry.java",
        "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseSupplierQuote.java",
        "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseOrder.java",
        "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteComparison.java",
        "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteContract.java"
    ]
    
    # 测试文件列表
    test_classes = [
        "src/test/java/cn/aiedge/erp/purchase/service/PurchaseInquiryServiceTest.java",
        "src/test/java/cn/aiedge/erp/purchase/service/PurchaseQuoteServiceTest.java",
        "src/test/java/cn/aiedge/erp/purchase/service/PurchaseOrderServiceTest.java",
        "src/test/java/cn/aiedge/erp/purchase/service/PurchaseQuoteComparisonTest.java",
        "src/test/java/cn/aiedge/erp/purchase/service/PurchaseContractServiceTest.java",
        "src/test/java/cn/aiedge/erp/purchase/integration/PurchaseIntegrationTest.java"
    ]
    
    print("\n1. 检查关键业务类文件:")
    print("-" * 40)
    
    business_files = []
    for file_rel_path in key_classes:
        file_path = os.path.join(project_path, file_rel_path)
        exists, _ = check_file_exists(file_path)
        business_files.append({
            "path": file_rel_path,
            "exists": exists
        })
        results["summary"]["total_checked"] += 1
        if exists:
            results["summary"]["found"] += 1
        else:
            results["summary"]["missing"] += 1
            results["missing_files"].append(file_rel_path)
    
    print("\n2. 检查测试类文件:")
    print("-" * 40)
    
    test_files = []
    for file_rel_path in test_classes:
        file_path = os.path.join(project_path, file_rel_path)
        exists, _ = check_file_exists(file_path)
        test_files.append({
            "path": file_rel_path,
            "exists": exists
        })
        results["summary"]["total_checked"] += 1
        if exists:
            results["summary"]["found"] += 1
        else:
            results["summary"]["missing"] += 1
            results["missing_files"].append(file_rel_path)
    
    # 计算覆盖率
    if results["summary"]["total_checked"] > 0:
        results["summary"]["coverage"] = round(
            (results["summary"]["found"] / results["summary"]["total_checked"]) * 100, 2
        )
    
    results["modules"]["business_files"] = business_files
    results["modules"]["test_files"] = test_files
    
    return results

def generate_report(results):
    """生成检查报告"""
    print("\n" + "=" * 80)
    print("检查报告")
    print("=" * 80)
    
    print(f"\n检查日期: {results['check_date']}")
    print(f"项目路径: {results['project_path']}")
    
    print(f"\n检查统计:")
    print(f"  总检查文件数: {results['summary']['total_checked']}")
    print(f"  找到文件数: {results['summary']['found']}")
    print(f"  缺失文件数: {results['summary']['missing']}")
    print(f"  文件存在率: {results['summary']['coverage']}%")
    
    if results["missing_files"]:
        print(f"\n缺失的关键文件 ({len(results['missing_files'])}个):")
        for missing_file in results["missing_files"]:
            print(f"  • {missing_file}")
    
    # 功能模块完整性分析
    print("\n" + "=" * 80)
    print("功能模块完整性分析")
    print("=" * 80)
    
    modules = {
        "询价管理模块": [
            "PurchaseInquiryController.java",
            "PurchaseInquiryService.java", 
            "PurchaseInquiry.java",
            "PurchaseInquiryServiceTest.java"
        ],
        "报价管理模块": [
            "PurchaseQuoteController.java",
            "PurchaseQuoteService.java",
            "PurchaseSupplierQuote.java",
            "PurchaseQuoteServiceTest.java"
        ],
        "订单管理模块": [
            "PurchaseOrderController.java",
            "PurchaseOrderService.java",
            "PurchaseOrder.java",
            "PurchaseOrderServiceTest.java"
        ],
        "比价分析模块": [
            "PurchaseQuoteComparison.java",
            "PurchaseQuoteComparisonTest.java"
        ],
        "合同管理模块": [
            "PurchaseQuoteContract.java",
            "PurchaseContractServiceTest.java"
        ],
        "集成测试模块": [
            "PurchaseIntegrationTest.java"
        ]
    }
    
    for module_name, file_list in modules.items():
        found_count = 0
        total_count = len(file_list)
        
        print(f"\n{module_name}:")
        for file_name in file_list:
            # 检查文件是否存在
            exists = False
            for file_info in results["modules"]["business_files"] + results["modules"]["test_files"]:
                if file_name in file_info["path"]:
                    exists = file_info["exists"]
                    break
            
            status = "✓" if exists else "✗"
            found_count += 1 if exists else 0
            print(f"  {status} {file_name}")
        
        coverage = (found_count / total_count) * 100 if total_count > 0 else 0
        status = "完整" if coverage >= 90 else "部分完整" if coverage >= 50 else "缺失"
        print(f"  完整性: {coverage:.1f}% ({status})")
    
    # 总体评估
    print("\n" + "=" * 80)
    print("总体评估")
    print("=" * 80)
    
    overall_coverage = results["summary"]["coverage"]
    if overall_coverage >= 90:
        assessment = "✅ 优秀 - 模块结构完整，符合P0发布标准"
    elif overall_coverage >= 80:
        assessment = "⚠️ 良好 - 模块结构基本完整，需要补充部分文件"
    elif overall_coverage >= 70:
        assessment = "⚠️ 一般 - 模块结构存在缺失，需要重大改进"
    else:
        assessment = "❌ 不合格 - 模块结构不完整，不满足P0要求"
    
    print(f"\n{assessment}")
    
    # 建议
    print("\n建议:")
    if results["summary"]["missing"] > 0:
        print("1. 立即创建缺失的关键文件")
    if overall_coverage < 80:
        print("2. 补充模块结构，确保功能完整性")
    print("3. 运行单元测试验证业务逻辑")
    print("4. 执行集成测试验证模块协作")
    
    return assessment

def save_results(results, output_dir="module-check-results"):
    """保存检查结果"""
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    
    # 保存JSON结果
    json_file = os.path.join(output_dir, f"erp-purchase-module-check-{timestamp}.json")
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)
    
    # 保存文本报告
    txt_file = os.path.join(output_dir, f"erp-purchase-module-report-{timestamp}.txt")
    
    with open(txt_file, 'w', encoding='utf-8') as f:
        f.write("=" * 80 + "\n")
        f.write("erp-purchase采购询价/报价功能模块结构检查报告\n")
        f.write("=" * 80 + "\n\n")
        
        f.write(f"检查日期: {results['check_date']}\n")
        f.write(f"项目路径: {results['project_path']}\n\n")
        
        f.write("检查统计:\n")
        f.write(f"  总检查文件数: {results['summary']['total_checked']}\n")
        f.write(f"  找到文件数: {results['summary']['found']}\n")
        f.write(f"  缺失文件数: {results['summary']['missing']}\n")
        f.write(f"  文件存在率: {results['summary']['coverage']}%\n\n")
        
        if results["missing_files"]:
            f.write(f"缺失的关键文件 ({len(results['missing_files'])}个):\n")
            for missing_file in results["missing_files"]:
                f.write(f"  • {missing_file}\n")
            f.write("\n")
    
    print(f"\n检查结果已保存:")
    print(f"  JSON文件: {json_file}")
    print(f"  文本报告: {txt_file}")
    
    return json_file, txt_file

def main():
    # 设置项目路径
    project_path = r"I:\AI-Ready\backend\erp\erp-purchase"
    
    if not os.path.exists(project_path):
        print(f"错误: 项目路径不存在: {project_path}")
        return
    
    # 分析模块结构
    results = analyze_module_structure(project_path)
    
    # 生成报告
    assessment = generate_report(results)
    
    # 保存结果
    json_file, txt_file = save_results(results)
    
    # 返回结果
    print("\n" + "=" * 80)
    print("检查完成")
    print("=" * 80)
    
    return results

if __name__ == "__main__":
    main()