#!/usr/bin/env python3
"""
测试框架配置验证工具
验证测试框架的配置正确性和完整性
"""

import os
import sys
import json
import yaml
import pytest
from pathlib import Path
from datetime import datetime

class TestFrameworkValidator:
    def __init__(self):
        self.root_dir = Path("I:/AI-Ready")
        self.tests_dir = self.root_dir / "backend" / "tests"
        self.validation_results = {
            "timestamp": datetime.now().isoformat(),
            "checks": [],
            "issues": [],
            "recommendations": []
        }
    
    def check_pytest_config(self):
        """检查pytest配置"""
        config_path = self.tests_dir / "pytest.ini"
        
        check = {
            "name": "pytest_config",
            "status": "unknown",
            "details": {}
        }
        
        if not config_path.exists():
            check["status"] = "failed"
            check["details"]["error"] = "pytest.ini文件不存在"
            self.validation_results["issues"].append({
                "type": "missing_pytest_config",
                "severity": "high",
                "description": "缺少主pytest配置文件",
                "recommendation": "创建backend/tests/pytest.ini文件"
            })
        else:
            try:
                with open(config_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                check["status"] = "passed"
                check["details"]["file_size"] = len(content)
                check["details"]["lines"] = len(content.split('\n'))
                
                # 检查关键配置项
                required_sections = ["pytest", "tool:pytest"]
                required_options = ["addopts", "markers", "testpaths"]
                
                for section in required_sections:
                    if f"[{section}]" in content:
                        check["details"][f"has_{section}"] = True
                    else:
                        check["details"][f"has_{section}"] = False
                
                for option in required_options:
                    if option in content:
                        check["details"][f"has_{option}"] = True
                    else:
                        check["details"][f"has_{option}"] = False
                        self.validation_results["issues"].append({
                            "type": "missing_pytest_option",
                            "severity": "medium",
                            "description": f"pytest配置缺少{option}选项",
                            "recommendation": f"在pytest.ini中添加{option}配置"
                        })
                
                # 检查并行执行配置
                if "-n" in content or "numprocesses" in content:
                    check["details"]["has_parallel"] = True
                else:
                    check["details"]["has_parallel"] = False
                    self.validation_results["recommendations"].append({
                        "priority": "medium",
                        "description": "pytest配置未启用并行执行",
                        "action": "在addopts中添加'-n auto'选项"
                    })
                
            except Exception as e:
                check["status"] = "error"
                check["details"]["error"] = str(e)
        
        self.validation_results["checks"].append(check)
        return check
    
    def check_test_structure(self):
        """检查测试目录结构"""
        check = {
            "name": "test_structure",
            "status": "unknown",
            "details": {}
        }
        
        required_dirs = [
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
        
        existing_dirs = []
        missing_dirs = []
        
        for dir_name in required_dirs:
            dir_path = self.tests_dir / dir_name
            if dir_path.exists() and dir_path.is_dir():
                existing_dirs.append(dir_name)
            else:
                missing_dirs.append(dir_name)
        
        check["details"]["existing_dirs"] = existing_dirs
        check["details"]["missing_dirs"] = missing_dirs
        
        if not missing_dirs:
            check["status"] = "passed"
        else:
            check["status"] = "warning"
            for missing_dir in missing_dirs:
                self.validation_results["issues"].append({
                    "type": "missing_test_directory",
                    "severity": "low",
                    "description": f"缺少测试目录: {missing_dir}",
                    "recommendation": f"创建backend/tests/{missing_dir}目录"
                })
        
        self.validation_results["checks"].append(check)
        return check
    
    def check_test_scripts(self):
        """检查测试脚本"""
        check = {
            "name": "test_scripts",
            "status": "unknown",
            "details": {}
        }
        
        scripts_dir = self.tests_dir / "scripts"
        required_scripts = [
            "optimized-test-runner.sh",
            "test-execution-script.sh"
        ]
        
        existing_scripts = []
        missing_scripts = []
        
        if scripts_dir.exists():
            for script_name in required_scripts:
                script_path = scripts_dir / script_name
                if script_path.exists():
                    existing_scripts.append(script_name)
                    
                    # 检查脚本权限
                    try:
                        stat = script_path.stat()
                        check["details"][f"{script_name}_size"] = stat.st_size
                        check["details"][f"{script_name}_executable"] = bool(stat.st_mode & 0o111)
                    except Exception as e:
                        check["details"][f"{script_name}_error"] = str(e)
                else:
                    missing_scripts.append(script_name)
        else:
            missing_scripts = required_scripts
        
        check["details"]["existing_scripts"] = existing_scripts
        check["details"]["missing_scripts"] = missing_scripts
        
        if not missing_scripts:
            check["status"] = "passed"
        else:
            check["status"] = "warning"
            for missing_script in missing_scripts:
                self.validation_results["issues"].append({
                    "type": "missing_test_script",
                    "severity": "medium",
                    "description": f"缺少测试脚本: {missing_script}",
                    "recommendation": f"创建backend/tests/scripts/{missing_script}文件"
                })
        
        self.validation_results["checks"].append(check)
        return check
    
    def check_test_documentation(self):
        """检查测试文档"""
        check = {
            "name": "test_documentation",
            "status": "unknown",
            "details": {}
        }
        
        docs_dir = self.tests_dir / "docs"
        required_docs = [
            "TEST_FRAMEWORK_MAINTENANCE.md",
            "api_automation_test_template.md"
        ]
        
        existing_docs = []
        missing_docs = []
        
        if docs_dir.exists():
            for doc_name in required_docs:
                doc_path = docs_dir / doc_name
                if doc_path.exists():
                    existing_docs.append(doc_name)
                    
                    # 检查文档大小
                    try:
                        size = doc_path.stat().st_size
                        check["details"][f"{doc_name}_size"] = size
                        check["details"][f"{doc_name}_valid"] = size > 100  # 至少100字节
                    except Exception as e:
                        check["details"][f"{doc_name}_error"] = str(e)
                else:
                    missing_docs.append(doc_name)
        else:
            missing_docs = required_docs
        
        check["details"]["existing_docs"] = existing_docs
        check["details"]["missing_docs"] = missing_docs
        
        if not missing_docs:
            check["status"] = "passed"
        else:
            check["status"] = "warning"
            for missing_doc in missing_docs:
                self.validation_results["issues"].append({
                    "type": "missing_test_documentation",
                    "severity": "low",
                    "description": f"缺少测试文档: {missing_doc}",
                    "recommendation": f"创建backend/tests/docs/{missing_doc}文件"
                })
        
        self.validation_results["checks"].append(check)
        return check
    
    def check_test_reports(self):
        """检查测试报告配置"""
        check = {
            "name": "test_reports",
            "status": "unknown",
            "details": {}
        }
        
        reports_dir = self.tests_dir / "reports"
        
        if reports_dir.exists():
            check["details"]["reports_dir_exists"] = True
            
            # 检查报告目录是否可写
            try:
                test_file = reports_dir / ".write_test"
                test_file.write_text("test")
                test_file.unlink()
                check["details"]["reports_dir_writable"] = True
            except Exception as e:
                check["details"]["reports_dir_writable"] = False
                check["details"]["write_error"] = str(e)
                self.validation_results["issues"].append({
                    "type": "reports_directory_not_writable",
                    "severity": "medium",
                    "description": "测试报告目录不可写",
                    "recommendation": "检查backend/tests/reports目录权限"
                })
        else:
            check["details"]["reports_dir_exists"] = False
            self.validation_results["issues"].append({
                "type": "missing_reports_directory",
                "severity": "medium",
                "description": "缺少测试报告目录",
                "recommendation": "创建backend/tests/reports目录"
            })
        
        check["status"] = "passed" if check["details"].get("reports_dir_exists") and check["details"].get("reports_dir_writable", False) else "failed"
        
        self.validation_results["checks"].append(check)
        return check
    
    def check_test_execution(self):
        """检查测试执行能力"""
        check = {
            "name": "test_execution",
            "status": "unknown",
            "details": {}
        }
        
        try:
            # 尝试导入pytest
            import pytest as _pytest
            check["details"]["pytest_available"] = True
            check["details"]["pytest_version"] = _pytest.__version__
        except ImportError:
            check["details"]["pytest_available"] = False
            self.validation_results["issues"].append({
                "type": "pytest_not_installed",
                "severity": "high",
                "description": "pytest未安装",
                "recommendation": "安装pytest: pip install pytest"
            })
        
        # 检查测试文件是否存在
        test_files = list(self.tests_dir.rglob("test_*.py"))
        check["details"]["test_files_count"] = len(test_files)
        
        if len(test_files) > 0:
            check["details"]["has_test_files"] = True
            
            # 检查一个示例测试文件
            sample_test = test_files[0]
            check["details"]["sample_test"] = str(sample_test.relative_to(self.root_dir))
            
            try:
                with open(sample_test, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 检查测试文件内容
                check["details"]["sample_test_size"] = len(content)
                check["details"]["sample_test_has_import"] = "import pytest" in content or "from pytest" in content
                check["details"]["sample_test_has_test_function"] = "def test_" in content
            except Exception as e:
                check["details"]["sample_test_error"] = str(e)
        else:
            check["details"]["has_test_files"] = False
            self.validation_results["issues"].append({
                "type": "no_test_files",
                "severity": "high",
                "description": "未找到测试文件",
                "recommendation": "创建test_*.py测试文件"
            })
        
        # 综合判断
        if (check["details"].get("pytest_available", False) and 
            check["details"].get("has_test_files", False)):
            check["status"] = "passed"
        else:
            check["status"] = "failed"
        
        self.validation_results["checks"].append(check)
        return check
    
    def run_validation(self):
        """运行完整的验证流程"""
        print("=" * 60)
        print("测试框架配置验证工具")
        print("=" * 60)
        
        checks = [
            ("检查pytest配置", self.check_pytest_config),
            ("检查测试目录结构", self.check_test_structure),
            ("检查测试脚本", self.check_test_scripts),
            ("检查测试文档", self.check_test_documentation),
            ("检查测试报告配置", self.check_test_reports),
            ("检查测试执行能力", self.check_test_execution)
        ]
        
        for check_name, check_func in checks:
            print(f"\n{check_name}...")
            result = check_func()
            status_icon = "✅" if result["status"] == "passed" else "⚠️" if result["status"] == "warning" else "❌"
            print(f"  {status_icon} {result['status'].upper()}")
        
        # 生成验证报告
        self.generate_validation_report()
        
        return self.validation_results
    
    def generate_validation_report(self):
        """生成验证报告"""
        report_path = self.tests_dir / "reports" / "framework_validation_report.json"
        
        # 确保报告目录存在
        report_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(self.validation_results, f, indent=2, ensure_ascii=False)
        
        # 生成Markdown摘要
        md_report = self.tests_dir / "reports" / "framework_validation_summary.md"
        self.generate_markdown_summary(md_report)
        
        print(f"\n验证报告已保存到: {report_path}")
        print(f"摘要报告已保存到: {md_report}")
    
    def generate_markdown_summary(self, output_path):
        """生成Markdown格式的验证摘要"""
        results = self.validation_results
        
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write("# 测试框架配置验证报告\n\n")
            f.write(f"**验证时间**: {results['timestamp']}\n\n")
            
            # 检查结果汇总
            f.write("## 检查结果汇总\n\n")
            
            passed = sum(1 for check in results["checks"] if check["status"] == "passed")
            warning = sum(1 for check in results["checks"] if check["status"] == "warning")
            failed = sum(1 for check in results["checks"] if check["status"] in ["failed", "error"])
            total = len(results["checks"])
            
            f.write(f"- ✅ 通过: {passed}/{total}\n")
            f.write(f"- ⚠️  警告: {warning}/{total}\n")
            f.write(f"- ❌ 失败: {failed}/{total}\n\n")
            
            # 详细检查结果
            f.write("## 详细检查结果\n\n")
            for check in results["checks"]:
                status_icon = "✅" if check["status"] == "passed" else "⚠️" if check["status"] == "warning" else "❌"
                f.write(f"### {status_icon} {check['name']} ({check['status'].upper()})\n\n")
                
                for key, value in check["details"].items():
                    if isinstance(value, (list, dict)):
                        value_str = json.dumps(value, ensure_ascii=False, indent=2)
                    else:
                        value_str = str(value)
                    f.write(f"- **{key}**: {value_str}\n")
                
                f.write("\n")
            
            # 问题列表
            if results["issues"]:
                f.write("## 发现的问题\n\n")
                
                # 按严重程度分组
                high_issues = [issue for issue in results["issues"] if issue["severity"] == "high"]
                medium_issues = [issue for issue in results["issues"] if issue["severity"] == "medium"]
                low_issues = [issue for issue in results["issues"] if issue["severity"] == "low"]
                
                if high_issues:
                    f.write("### 🔴 高优先级问题\n\n")
                    for issue in high_issues:
                        f.write(f"**{issue['type']}**\n")
                        f.write(f"- 描述: {issue['description']}\n")
                        f.write(f"- 建议: {issue['recommendation']}\n\n")
                
                if medium_issues:
                    f.write("### 🟡 中优先级问题\n\n")
                    for issue in medium_issues:
                        f.write(f"**{issue['type']}**\n")
                        f.write(f"- 描述: {issue['description']}\n")
                        f.write(f"- 建议: {issue['recommendation']}\n\n")
                
                if low_issues:
                    f.write("### 🟢 低优先级问题\n\n")
                    for issue in low_issues:
                        f.write(f"**{issue['type']}**\n")
                        f.write(f"- 描述: {issue['description']}\n")
                        f.write(f"- 建议: {issue['recommendation']}\n\n")
            
            # 建议列表
            if results["recommendations"]:
                f.write("## 优化建议\n\n")
                
                high_recs = [rec for rec in results["recommendations"] if rec["priority"] == "high"]
                medium_recs = [rec for rec in results["recommendations"] if rec["priority"] == "medium"]
                low_recs = [rec for rec in results["recommendations"] if rec["priority"] == "low"]
                
                if high_recs:
                    f.write("### 🔴 高优先级建议\n\n")
                    for rec in high_recs:
                        f.write(f"- {rec['description']}\n")
                        f.write(f"  **操作**: {rec['action']}\n\n")
                
                if medium_recs:
                    f.write("### 🟡 中优先级建议\n\n")
                    for rec in medium_recs:
                        f.write(f"- {rec['description']}\n")
                        f.write(f"  **操作**: {rec['action']}\n\n")
                
                if low_recs:
                    f.write("### 🟢 低优先级建议\n\n")
                    for rec in low_recs:
                        f.write(f"- {rec['description']}\n")
                        f.write(f"  **操作**: {rec['action']}\n\n")
            
            # 总结
            f.write("## 验证结论\n\n")
            
            if failed == 0 and warning == 0:
                f.write("✅ **测试框架配置完整，所有检查都通过。**\n")
                f.write("测试框架已准备好使用。\n")
            elif failed == 0:
                f.write("⚠️ **测试框架配置基本完整，但有警告需要关注。**\n")
                f.write("建议修复警告中提到的问题以获得最佳体验。\n")
            else:
                f.write("❌ **测试框架配置存在问题，需要修复。**\n")
                f.write("请优先修复高优先级问题，然后重新验证。\n")
            
            f.write("\n## 后续步骤\n\n")
            f.write("1. 根据问题列表修复配置问题\n")
            f.write("2. 运行测试验证修复效果\n")
            f.write("3. 定期运行此验证工具确保配置正确\n")
    
    def print_summary(self):
        """打印验证摘要"""
        results = self.validation_results
        
        print("\n" + "=" * 60)
        print("验证摘要")
        print("=" * 60)
        
        # 统计结果
        passed = sum(1 for check in results["checks"] if check["status"] == "passed")
        warning = sum(1 for check in results["checks"] if check["status"] == "warning")
        failed = sum(1 for check in results["checks"] if check["status"] in ["failed", "error"])
        total = len(results["checks"])
        
        print(f"\n检查结果: {passed}通过, {warning}警告, {failed}失败 (共{total}项)")
        
        # 显示问题摘要
        if results["issues"]:
            print("\n发现的问题:")
            
            high_issues = [issue for issue in results["issues"] if issue["severity"] == "high"]
            medium_issues = [issue for issue in results["issues"] if issue["severity"] == "medium"]
            low_issues = [issue for issue in results["issues"] if issue["severity"] == "low"]
            
            if high_issues:
                print(f"  🔴 高优先级: {len(high_issues)}个")
                for issue in high_issues[:3]:  # 显示前3个
                    print(f"    • {issue['type']}: {issue['description']}")
            
            if medium_issues:
                print(f"  🟡 中优先级: {len(medium_issues)}个")
                for issue in medium_issues[:2]:  # 显示前2个
                    print(f"    • {issue['type']}: {issue['description']}")
        
        # 显示建议摘要
        if results["recommendations"]:
            print("\n优化建议:")
            
            high_recs = [rec for rec in results["recommendations"] if rec["priority"] == "high"]
            medium_recs = [rec for rec in results["recommendations"] if rec["priority"] == "medium"]
            
            if high_recs:
                print(f"  🔴 高优先级: {len(high_recs)}条")
                for rec in high_recs[:2]:  # 显示前2个
                    print(f"    • {rec['description']}")
            
            if medium_recs:
                print(f"  🟡 中优先级: {len(medium_recs)}条")
                for rec in medium_recs[:2]:  # 显示前2个
                    print(f"    • {rec['description']}")
        
        print("\n" + "=" * 60)

def main():
    """主函数"""
    try:
        validator = TestFrameworkValidator()
        results = validator.run_validation()
        validator.print_summary()
        
        # 返回适当的退出代码
        failed_checks = sum(1 for check in results["checks"] if check["status"] in ["failed", "error"])
        if failed_checks > 0:
            return 1
        else:
            return 0
            
    except Exception as e:
        print(f"验证过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        return 2

if __name__ == "__main__":
    sys.exit(main())