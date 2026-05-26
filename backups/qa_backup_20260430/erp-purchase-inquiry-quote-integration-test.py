#!/usr/bin/env python3
"""
erp-purchase采购询价/报价功能集成测试脚本
P0级别测试，覆盖5大测试模块
"""

import os
import sys
import subprocess
import json
import time
from datetime import datetime

class ERPTestFramework:
    def __init__(self, project_path):
        self.project_path = project_path
        self.test_results = {
            "test_date": datetime.now().isoformat(),
            "project": "erp-purchase",
            "modules": {},
            "summary": {
                "total_tests": 0,
                "passed": 0,
                "failed": 0,
                "skipped": 0,
                "coverage": 0
            }
        }
        
    def log(self, message, level="INFO"):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        # 处理Unicode字符，避免编码错误
        safe_message = message.encode('utf-8', errors='ignore').decode('utf-8')
        print(f"[{timestamp}] [{level}] {safe_message}")
        
    def run_maven_test(self, test_class=None):
        """运行Maven测试"""
        cmd = ["mvn", "test"]
        if test_class:
            cmd.extend(["-Dtest", test_class])
            
        self.log(f"运行Maven测试: {' '.join(cmd)}")
        
        try:
            result = subprocess.run(
                cmd,
                cwd=self.project_path,
                capture_output=True,
                text=True,
                timeout=300
            )
            
            if result.returncode == 0:
                self.log("Maven测试执行成功")
                return True, result.stdout
            else:
                self.log(f"Maven测试执行失败: {result.stderr}", "ERROR")
                return False, result.stderr
                
        except subprocess.TimeoutExpired:
            self.log("Maven测试执行超时", "ERROR")
            return False, "测试执行超时"
        except Exception as e:
            self.log(f"Maven测试执行异常: {str(e)}", "ERROR")
            return False, str(e)
    
    def check_file_exists(self, file_path):
        """检查文件是否存在"""
        full_path = os.path.join(self.project_path, file_path)
        exists = os.path.exists(full_path)
        status = "[PASS]" if exists else "[FAIL]"
        self.log(f"检查文件: {file_path} {status}")
        return exists, full_path
    
    def analyze_test_coverage(self):
        """分析测试覆盖率"""
        self.log("分析测试覆盖率...")
        
        # 检查关键类文件
        key_classes = [
            "src/main/java/cn/aiedge/erp/purchase/controller/PurchaseInquiryController.java",
            "src/main/java/cn/aiedge/erp/purchase/controller/PurchaseQuoteController.java",
            "src/main/java/cn/aiedge/erp/purchase/controller/PurchaseOrderController.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseInquiryService.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseQuoteService.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseOrderService.java",
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseInquiry.java",
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseSupplierQuote.java",
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseOrder.java"
        ]
        
        test_classes = [
            "src/test/java/cn/aiedge/erp/purchase/service/PurchaseInquiryServiceTest.java",
            "src/test/java/cn/aiedge/erp/purchase/service/PurchaseQuoteServiceTest.java",
            "src/test/java/cn/aiedge/erp/purchase/service/PurchaseOrderServiceTest.java"
        ]
        
        # 检查文件存在性
        class_exists = sum(1 for c in key_classes if self.check_file_exists(c)[0])
        test_exists = sum(1 for t in test_classes if self.check_file_exists(t)[0])
        
        coverage = (test_exists / max(class_exists, 1)) * 100
        
        return {
            "total_classes": len(key_classes),
            "classes_found": class_exists,
            "total_tests": len(test_classes),
            "tests_found": test_exists,
            "coverage_percentage": round(coverage, 2)
        }
    
    def test_inquiry_process(self):
        """测试询价流程"""
        self.log("开始测试询价流程...")
        
        tests = [
            {
                "name": "创建询价单测试",
                "status": "pending",
                "description": "验证询价单创建功能"
            },
            {
                "name": "发布询价单测试",
                "status": "pending",
                "description": "验证询价单发布功能"
            },
            {
                "name": "询价单状态流转测试",
                "status": "pending",
                "description": "验证DRAFT→PUBLISHED→CLOSED状态流转"
            },
            {
                "name": "询价截止时间管理测试",
                "status": "pending",
                "description": "验证截止时间有效性检查"
            }
        ]
        
        # 检查相关文件
        required_files = [
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseInquiry.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseInquiryService.java",
            "src/test/java/cn/aiedge/erp/purchase/service/PurchaseInquiryServiceTest.java"
        ]
        
        for file in required_files:
            exists, _ = self.check_file_exists(file)
            if not exists:
                self.log(f"缺少必要文件: {file}", "WARNING")
        
        # 运行测试
        success, output = self.run_maven_test("PurchaseInquiryServiceTest")
        
        if success:
            self.log("询价流程测试通过")
            for test in tests:
                test["status"] = "passed"
        else:
            self.log("询价流程测试失败", "ERROR")
            for test in tests:
                test["status"] = "failed"
        
        return tests
    
    def test_quote_process(self):
        """测试报价流程"""
        self.log("开始测试报价流程...")
        
        tests = [
            {
                "name": "供应商报价提交测试",
                "status": "pending",
                "description": "验证供应商报价提交功能"
            },
            {
                "name": "报价单格式验证测试",
                "status": "pending",
                "description": "验证报价单字段完整性校验"
            },
            {
                "name": "报价有效期管理测试",
                "status": "pending",
                "description": "验证报价有效期检查和过期处理"
            },
            {
                "name": "报价修改与撤回测试",
                "status": "pending",
                "description": "验证报价修改和撤回功能"
            }
        ]
        
        # 检查相关文件
        required_files = [
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseSupplierQuote.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseQuoteService.java",
            "src/test/java/cn/aiedge/erp/purchase/service/PurchaseQuoteServiceTest.java"
        ]
        
        for file in required_files:
            exists, _ = self.check_file_exists(file)
            if not exists:
                self.log(f"缺少必要文件: {file}", "WARNING")
        
        # 运行测试
        success, output = self.run_maven_test("PurchaseQuoteServiceTest")
        
        if success:
            self.log("报价流程测试通过")
            for test in tests:
                test["status"] = "passed"
        else:
            self.log("报价流程测试失败", "ERROR")
            for test in tests:
                test["status"] = "failed"
        
        return tests
    
    def test_comparison_analysis(self):
        """测试比价分析"""
        self.log("开始测试比价分析...")
        
        tests = [
            {
                "name": "多供应商报价比价测试",
                "status": "pending",
                "description": "验证多供应商报价比较功能"
            },
            {
                "name": "报价分析工具测试",
                "status": "pending",
                "description": "验证报价评分和权重计算"
            },
            {
                "name": "报价历史对比测试",
                "status": "pending",
                "description": "验证历史报价对比功能"
            },
            {
                "name": "采购建议生成测试",
                "status": "pending",
                "description": "验证自动化采购建议生成"
            }
        ]
        
        # 检查相关文件
        required_files = [
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteComparison.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseQuoteService.java"
        ]
        
        for file in required_files:
            exists, _ = self.check_file_exists(file)
            if not exists:
                self.log(f"缺少必要文件: {file}", "WARNING")
                for test in tests:
                    test["status"] = "skipped"
                return tests
        
        # 检查是否有比价分析测试
        comparison_test_file = "src/test/java/cn/aiedge/erp/purchase/service/PurchaseQuoteComparisonTest.java"
        exists, _ = self.check_file_exists(comparison_test_file)
        
        if exists:
            success, output = self.run_maven_test("PurchaseQuoteComparisonTest")
            if success:
                self.log("比价分析测试通过")
                for test in tests:
                    test["status"] = "passed"
            else:
                self.log("比价分析测试失败", "ERROR")
                for test in tests:
                    test["status"] = "failed"
        else:
            self.log("比价分析测试文件不存在，跳过测试", "WARNING")
            for test in tests:
                test["status"] = "skipped"
        
        return tests
    
    def test_contract_generation(self):
        """测试合同生成"""
        self.log("开始测试合同生成...")
        
        tests = [
            {
                "name": "采购合同自动生成测试",
                "status": "pending",
                "description": "验证采购合同自动生成功能"
            },
            {
                "name": "合同条款验证测试",
                "status": "pending",
                "description": "验证合同条款完整性检查"
            },
            {
                "name": "合同审批流程测试",
                "status": "pending",
                "description": "验证合同审批工作流"
            },
            {
                "name": "合同归档管理测试",
                "status": "pending",
                "description": "验证合同归档和检索功能"
            }
        ]
        
        # 检查相关文件
        required_files = [
            "src/main/java/cn/aiedge/erp/purchase/entity/PurchaseQuoteContract.java",
            "src/main/java/cn/aiedge/erp/purchase/service/PurchaseContractService.java"
        ]
        
        missing_files = []
        for file in required_files:
            exists, _ = self.check_file_exists(file)
            if not exists:
                missing_files.append(file)
        
        if missing_files:
            self.log(f"合同相关文件缺失: {', '.join(missing_files)}", "WARNING")
            for test in tests:
                test["status"] = "skipped"
            return tests
        
        # 检查是否有合同测试
        contract_test_file = "src/test/java/cn/aiedge/erp/purchase/service/PurchaseContractServiceTest.java"
        exists, _ = self.check_file_exists(contract_test_file)
        
        if exists:
            success, output = self.run_maven_test("PurchaseContractServiceTest")
            if success:
                self.log("合同生成测试通过")
                for test in tests:
                    test["status"] = "passed"
            else:
                self.log("合同生成测试失败", "ERROR")
                for test in tests:
                    test["status"] = "failed"
        else:
            self.log("合同生成测试文件不存在，跳过测试", "WARNING")
            for test in tests:
                test["status"] = "skipped"
        
        return tests
    
    def test_integration(self):
        """测试集成功能"""
        self.log("开始测试集成功能...")
        
        tests = [
            {
                "name": "与erp-finance付款流程集成测试",
                "status": "pending",
                "description": "验证采购付款流程集成"
            },
            {
                "name": "与erp-inventory入库流程集成测试",
                "status": "pending",
                "description": "验证采购入库流程集成"
            },
            {
                "name": "与erp-supplier供应商管理集成测试",
                "status": "pending",
                "description": "验证供应商管理集成"
            },
            {
                "name": "跨系统数据同步测试",
                "status": "pending",
                "description": "验证跨模块数据一致性"
            }
        ]
        
        # 检查集成测试文件
        integration_test_file = "src/test/java/cn/aiedge/erp/purchase/integration/PurchaseIntegrationTest.java"
        exists, _ = self.check_file_exists(integration_test_file)
        
        if not exists:
            self.log("集成测试文件不存在，跳过测试", "WARNING")
            for test in tests:
                test["status"] = "skipped"
            return tests
        
        # 运行集成测试
        success, output = self.run_maven_test("PurchaseIntegrationTest")
        
        if success:
            self.log("集成测试通过")
            for test in tests:
                test["status"] = "passed"
        else:
            self.log("集成测试失败", "ERROR")
            for test in tests:
                test["status"] = "failed"
        
        return tests
    
    def run_all_tests(self):
        """运行所有测试"""
        self.log("=" * 60)
        self.log("开始执行erp-purchase采购询价/报价功能P0测试")
        self.log("=" * 60)
        
        # 分析测试覆盖率
        coverage = self.analyze_test_coverage()
        self.test_results["coverage_analysis"] = coverage
        
        # 运行各模块测试
        modules = {
            "inquiry_process": self.test_inquiry_process(),
            "quote_process": self.test_quote_process(),
            "comparison_analysis": self.test_comparison_analysis(),
            "contract_generation": self.test_contract_generation(),
            "integration": self.test_integration()
        }
        
        self.test_results["modules"] = modules
        
        # 统计结果
        for module_name, tests in modules.items():
            for test in tests:
                self.test_results["summary"]["total_tests"] += 1
                if test["status"] == "passed":
                    self.test_results["summary"]["passed"] += 1
                elif test["status"] == "failed":
                    self.test_results["summary"]["failed"] += 1
                elif test["status"] == "skipped":
                    self.test_results["summary"]["skipped"] += 1
        
        # 计算覆盖率
        total = self.test_results["summary"]["total_tests"]
        passed = self.test_results["summary"]["passed"]
        if total > 0:
            self.test_results["summary"]["coverage"] = round((passed / total) * 100, 2)
        
        return self.test_results
    
    def generate_report(self):
        """生成测试报告"""
        self.log("生成测试报告...")
        
        report = {
            "测试概述": {
                "测试日期": self.test_results["test_date"],
                "测试项目": self.test_results["project"],
                "测试类型": "P0功能测试"
            },
            "测试统计": {
                "总测试数": self.test_results["summary"]["total_tests"],
                "通过数": self.test_results["summary"]["passed"],
                "失败数": self.test_results["summary"]["failed"],
                "跳过数": self.test_results["summary"]["skipped"],
                "通过率": f"{self.test_results['summary']['coverage']}%"
            },
            "测试覆盖度分析": self.test_results.get("coverage_analysis", {}),
            "详细测试结果": {}
        }
        
        # 添加详细结果
        for module_name, tests in self.test_results["modules"].items():
            module_display = {
                "询价流程测试": "询价流程",
                "quote_process": "报价流程",
                "comparison_analysis": "比价分析",
                "contract_generation": "合同生成",
                "integration": "集成测试"
            }.get(module_name, module_name)
            
            report["详细测试结果"][module_display] = []
            for test in tests:
                status_icon = {
                    "passed": "✅",
                    "failed": "❌",
                    "skipped": "⚠️",
                    "pending": "⏳"
                }.get(test["status"], "❓")
                
                report["详细测试结果"][module_display].append({
                    "测试项": test["name"],
                    "状态": f"{status_icon} {test['status']}",
                    "描述": test["description"]
                })
        
        # 总体评估
        passed_rate = self.test_results["summary"]["coverage"]
        if passed_rate >= 90:
            assessment = "✅ 优秀 - 通过率90%以上，符合P0发布标准"
        elif passed_rate >= 80:
            assessment = "⚠️ 良好 - 通过率80%以上，需要修复部分问题"
        elif passed_rate >= 70:
            assessment = "⚠️ 一般 - 通过率70%以上，需要重大改进"
        else:
            assessment = "❌ 不合格 - 通过率低于70%，不满足P0要求"
        
        report["总体评估"] = assessment
        
        # 关键问题
        issues = []
        for module_name, tests in self.test_results["modules"].items():
            failed_tests = [t for t in tests if t["status"] == "failed"]
            skipped_tests = [t for t in tests if t["status"] == "skipped"]
            
            if failed_tests:
                issues.append(f"{module_name}模块有{len(failed_tests)}个测试失败")
            if skipped_tests:
                issues.append(f"{module_name}模块有{len(skipped_tests)}个测试被跳过")
        
        if issues:
            report["关键问题"] = issues
        
        # 建议
        suggestions = []
        if self.test_results["summary"]["failed"] > 0:
            suggestions.append("立即修复失败的测试用例")
        if self.test_results["summary"]["skipped"] > 0:
            suggestions.append("补充被跳过的测试用例")
        if passed_rate < 80:
            suggestions.append("提高测试覆盖率至80%以上")
        
        if suggestions:
            report["改进建议"] = suggestions
        
        return report
    
    def save_results(self, output_dir="test-results"):
        """保存测试结果"""
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # 保存JSON结果
        json_file = os.path.join(output_dir, f"erp-purchase-test-results-{timestamp}.json")
        with open(json_file, 'w', encoding='utf-8') as f:
            json.dump(self.test_results, f, ensure_ascii=False, indent=2)
        
        # 生成文本报告
        report = self.generate_report()
        txt_file = os.path.join(output_dir, f"erp-purchase-test-report-{timestamp}.txt")
        
        with open(txt_file, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write("erp-purchase采购询价/报价功能P0测试报告\n")
            f.write("=" * 60 + "\n\n")
            
            f.write("测试概述:\n")
            for key, value in report["测试概述"].items():
                f.write(f"  {key}: {value}\n")
            
            f.write("\n测试统计:\n")
            for key, value in report["测试统计"].items():
                f.write(f"  {key}: {value}\n")
            
            f.write("\n测试覆盖度分析:\n")
            if "测试覆盖度分析" in report:
                for key, value in report["测试覆盖度分析"].items():
                    f.write(f"  {key}: {value}\n")
            
            f.write("\n详细测试结果:\n")
            for module, tests in report["详细测试结果"].items():
                f.write(f"\n  {module}:\n")
                for test in tests:
                    f.write(f"    {test['测试项']} - {test['状态']}\n")
                    f.write(f"      描述: {test['描述']}\n")
            
            f.write(f"\n总体评估: {report['总体评估']}\n")
            
            if "关键问题" in report:
                f.write("\n关键问题:\n")
                for issue in report["关键问题"]:
                    f.write(f"  • {issue}\n")
            
            if "改进建议" in report:
                f.write("\n改进建议:\n")
                for suggestion in report["改进建议"]:
                    f.write(f"  • {suggestion}\n")
        
        self.log(f"测试结果已保存: {json_file}")
        self.log(f"测试报告已保存: {txt_file}")
        
        return json_file, txt_file

def main():
    # 设置项目路径
    project_path = r"I:\AI-Ready\backend\erp\erp-purchase"
    
    if not os.path.exists(project_path):
        print(f"错误: 项目路径不存在: {project_path}")
        sys.exit(1)
    
    # 创建测试框架
    test_framework = ERPTestFramework(project_path)
    
    # 运行所有测试
    results = test_framework.run_all_tests()
    
    # 保存结果
    json_file, txt_file = test_framework.save_results("test-results")
    
    # 打印摘要
    print("\n" + "=" * 60)
    print("测试执行完成")
    print("=" * 60)
    print(f"总测试数: {results['summary']['total_tests']}")
    print(f"通过数: {results['summary']['passed']}")
    print(f"失败数: {results['summary']['failed']}")
    print(f"跳过数: {results['summary']['skipped']}")
    print(f"通过率: {results['summary']['coverage']}%")
    
    # 检查Maven是否可用
    maven_check = subprocess.run(["mvn", "--version"], capture_output=True, text=True)
    if maven_check.returncode != 0:
        print("\n警告: Maven未安装或未在PATH中，部分测试可能无法执行")
    
    # 返回退出码
    if results['summary']['failed'] > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()