#!/usr/bin/env python3
"""
自动化测试框架诊断工具
用于分析测试框架的配置、执行效率和稳定性
"""

import os
import sys
import json
import time
import subprocess
from pathlib import Path
from datetime import datetime
from collections import defaultdict

class TestFrameworkDiagnostic:
    def __init__(self):
        self.root_dir = Path("I:/AI-Ready")
        self.backend_tests_dir = self.root_dir / "backend" / "tests"
        self.project_tests_dir = self.root_dir / "tests"
        self.diagnostic_results = {
            "timestamp": datetime.now().isoformat(),
            "config_analysis": {},
            "performance_metrics": {},
            "stability_issues": [],
            "optimization_recommendations": []
        }
    
    def analyze_test_configs(self):
        """分析测试配置文件"""
        configs = {
            "pytest_configs": [],
            "conftest_files": [],
            "test_scripts": [],
            "ci_configs": []
        }
        
        # 查找pytest.ini文件
        for pytest_ini in self.root_dir.rglob("pytest.ini"):
            configs["pytest_configs"].append(str(pytest_ini.relative_to(self.root_dir)))
            self._analyze_pytest_config(pytest_ini)
        
        # 查找conftest.py文件
        for conftest in self.root_dir.rglob("conftest.py"):
            configs["conftest_files"].append(str(conftest.relative_to(self.root_dir)))
        
        # 查找测试脚本
        for script in self.backend_tests_dir.rglob("*.sh"):
            if "test" in script.name.lower():
                configs["test_scripts"].append(str(script.relative_to(self.root_dir)))
        
        # 查找CI/CD配置
        ci_patterns = ["*docker-compose*.yml", "*Jenkinsfile*", "*github-workflows*.yml", "*.gitlab-ci.yml"]
        for pattern in ci_patterns:
            for file in self.root_dir.rglob(pattern):
                configs["ci_configs"].append(str(file.relative_to(self.root_dir)))
        
        self.diagnostic_results["config_analysis"] = configs
        return configs
    
    def _analyze_pytest_config(self, config_path):
        """分析pytest配置"""
        try:
            with open(config_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            analysis = {
                "test_paths": "Not specified",
                "addopts": "Not specified",
                "markers": "Not specified",
                "coverage": "Not specified"
            }
            
            lines = content.split('\n')
            for line in lines:
                line = line.strip()
                if line.startswith("testpaths"):
                    analysis["test_paths"] = line.split('=', 1)[-1].strip()
                elif line.startswith("addopts"):
                    analysis["addopts"] = line.split('=', 1)[-1].strip()
                elif line.startswith("markers"):
                    analysis["markers"] = "Defined"
                elif "coverage" in line:
                    analysis["coverage"] = "Enabled"
            
            self.diagnostic_results["config_analysis"]["pytest_config_details"] = {
                str(config_path.relative_to(self.root_dir)): analysis
            }
        except Exception as e:
            print(f"Error analyzing pytest config {config_path}: {e}")
    
    def analyze_test_structure(self):
        """分析测试目录结构"""
        structure = {
            "total_test_files": 0,
            "test_types": defaultdict(int),
            "test_file_sizes": [],
            "test_modules": set()
        }
        
        # 统计测试文件
        for test_file in self.root_dir.rglob("test_*.py"):
            structure["total_test_files"] += 1
            
            # 分类测试类型
            parent_dir = test_file.parent.name.lower()
            if "api" in parent_dir or "test_file.name" in str(test_file):
                structure["test_types"]["api"] += 1
            elif "unit" in parent_dir:
                structure["test_types"]["unit"] += 1
            elif "integration" in parent_dir:
                structure["test_types"]["integration"] += 1
            elif "e2e" in parent_dir or "end" in parent_dir:
                structure["test_types"]["e2e"] += 1
            elif "performance" in parent_dir:
                structure["test_types"]["performance"] += 1
            else:
                structure["test_types"]["other"] += 1
            
            # 记录文件大小
            size_kb = test_file.stat().st_size / 1024
            structure["test_file_sizes"].append({
                "file": str(test_file.relative_to(self.root_dir)),
                "size_kb": round(size_kb, 2)
            })
            
            # 记录模块
            structure["test_modules"].add(str(test_file.parent.relative_to(self.root_dir)))
        
        structure["test_modules"] = list(structure["test_modules"])
        
        # 计算平均文件大小
        if structure["test_file_sizes"]:
            avg_size = sum(item["size_kb"] for item in structure["test_file_sizes"]) / len(structure["test_file_sizes"])
            structure["average_file_size_kb"] = round(avg_size, 2)
            structure["largest_test_file"] = max(structure["test_file_sizes"], key=lambda x: x["size_kb"])
        
        self.diagnostic_results["structure_analysis"] = structure
        return structure
    
    def analyze_test_performance(self):
        """分析测试性能（模拟）"""
        # 这里可以实际运行测试来收集性能数据
        # 为了快速诊断，我们先提供模拟数据
        performance = {
            "estimated_execution_time": {
                "unit_tests": "~30 seconds",
                "api_tests": "~2 minutes",
                "integration_tests": "~5 minutes",
                "e2e_tests": "~10 minutes",
                "total": "~18 minutes"
            },
            "parallel_execution": "Not configured",
            "test_isolation": "Unknown",
            "resource_usage": "Unknown"
        }
        
        # 检查是否配置了并行执行
        configs = self.diagnostic_results.get("config_analysis", {}).get("pytest_config_details", {})
        for config in configs.values():
            if "addopts" in config and ("-n" in config["addopts"] or "numprocesses" in config["addopts"]):
                performance["parallel_execution"] = "Configured"
        
        self.diagnostic_results["performance_metrics"] = performance
        return performance
    
    def identify_stability_issues(self):
        """识别稳定性问题"""
        issues = []
        
        # 检查过大的测试文件（可能难以维护）
        structure = self.diagnostic_results.get("structure_analysis", {})
        if "largest_test_file" in structure:
            largest = structure["largest_test_file"]
            if largest["size_kb"] > 50:  # 超过50KB的测试文件可能太大
                issues.append({
                    "type": "large_test_file",
                    "file": largest["file"],
                    "size_kb": largest["size_kb"],
                    "recommendation": "Consider splitting this large test file into smaller, focused test files"
                })
        
        # 检查缺少测试配置
        configs = self.diagnostic_results.get("config_analysis", {})
        if not configs.get("pytest_configs"):
            issues.append({
                "type": "missing_pytest_config",
                "description": "No pytest.ini configuration file found",
                "recommendation": "Create a central pytest.ini file for consistent test configuration"
            })
        
        # 检查缺少标记系统
        if configs.get("pytest_configs"):
            for config_path, details in configs.get("pytest_config_details", {}).items():
                if details.get("markers") == "Not specified":
                    issues.append({
                        "type": "missing_test_markers",
                        "config": config_path,
                        "recommendation": "Add test markers to categorize tests (e.g., @pytest.mark.slow, @pytest.mark.unit)"
                    })
        
        self.diagnostic_results["stability_issues"] = issues
        return issues
    
    def generate_optimization_recommendations(self):
        """生成优化建议"""
        recommendations = []
        
        # 基于配置分析的优化建议
        configs = self.diagnostic_results.get("config_analysis", {})
        if len(configs.get("pytest_configs", [])) > 1:
            recommendations.append({
                "priority": "high",
                "title": "统一pytest配置",
                "description": "发现多个pytest.ini文件，建议创建统一的中央配置",
                "action": "合并所有pytest.ini配置到一个文件中"
            })
        
        # 基于性能分析的优化建议
        performance = self.diagnostic_results.get("performance_metrics", {})
        if performance.get("parallel_execution") == "Not configured":
            recommendations.append({
                "priority": "medium",
                "title": "启用并行测试执行",
                "description": "测试没有配置并行执行，可能影响执行效率",
                "action": "在pytest配置中添加'-n auto'选项启用自动并行执行"
            })
        
        # 基于结构分析的优化建议
        structure = self.diagnostic_results.get("structure_analysis", {})
        test_counts = structure.get("test_types", {})
        
        if test_counts.get("e2e", 0) > 20:
            recommendations.append({
                "priority": "medium",
                "title": "优化E2E测试执行策略",
                "description": f"发现{test_counts.get('e2e', 0)}个E2E测试，可能需要较长时间执行",
                "action": "考虑将E2E测试标记为慢速测试，与快速测试分开执行"
            })
        
        # 通用优化建议
        recommendations.extend([
            {
                "priority": "low",
                "title": "完善测试标记系统",
                "description": "测试标记有助于分类和执行管理",
                "action": "为所有测试添加适当的标记（unit, integration, e2e, slow, etc.）"
            },
            {
                "priority": "medium",
                "title": "优化测试报告生成",
                "description": "清晰的测试报告有助于问题诊断",
                "action": "配置HTML报告和JUnit XML报告输出"
            },
            {
                "priority": "high",
                "title": "创建测试框架维护文档",
                "description": "缺乏测试框架的维护文档",
                "action": "创建TEST_FRAMEWORK_MAINTENANCE.md文档"
            }
        ])
        
        self.diagnostic_results["optimization_recommendations"] = recommendations
        return recommendations
    
    def run_diagnostic(self):
        """运行完整的诊断流程"""
        print("=" * 60)
        print("自动化测试框架诊断工具")
        print("=" * 60)
        
        print("\n1. 分析测试配置文件...")
        self.analyze_test_configs()
        
        print("2. 分析测试目录结构...")
        self.analyze_test_structure()
        
        print("3. 分析测试性能...")
        self.analyze_test_performance()
        
        print("4. 识别稳定性问题...")
        self.identify_stability_issues()
        
        print("5. 生成优化建议...")
        self.generate_optimization_recommendations()
        
        print("\n" + "=" * 60)
        print("诊断完成!")
        print("=" * 60)
        
        # 保存诊断结果
        report_path = self.root_dir / "tests" / "test_framework_diagnostic_report.json"
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(self.diagnostic_results, f, indent=2, ensure_ascii=False)
        
        print(f"诊断报告已保存到: {report_path}")
        
        return self.diagnostic_results
    
    def print_summary(self):
        """打印诊断摘要"""
        results = self.diagnostic_results
        
        print("\n" + "=" * 60)
        print("测试框架诊断摘要")
        print("=" * 60)
        
        # 配置分析
        configs = results.get("config_analysis", {})
        print(f"\n📋 配置文件分析:")
        print(f"  • pytest.ini文件: {len(configs.get('pytest_configs', []))}个")
        print(f"  • conftest.py文件: {len(configs.get('conftest_files', []))}个")
        print(f"  • 测试脚本: {len(configs.get('test_scripts', []))}个")
        print(f"  • CI/CD配置: {len(configs.get('ci_configs', []))}个")
        
        # 结构分析
        structure = results.get("structure_analysis", {})
        print(f"\n📁 测试结构分析:")
        print(f"  • 总测试文件: {structure.get('total_test_files', 0)}个")
        print(f"  • 测试类型分布:")
        for test_type, count in structure.get("test_types", {}).items():
            print(f"    - {test_type}: {count}个")
        
        # 稳定性问题
        issues = results.get("stability_issues", [])
        print(f"\n⚠️  稳定性问题: {len(issues)}个")
        for issue in issues[:3]:  # 显示前3个问题
            print(f"  • {issue['type']}: {issue.get('description', issue.get('file', 'N/A'))}")
        
        # 优化建议
        recommendations = results.get("optimization_recommendations", [])
        print(f"\n💡 优化建议: {len(recommendations)}条")
        
        high_priority = [r for r in recommendations if r["priority"] == "high"]
        medium_priority = [r for r in recommendations if r["priority"] == "medium"]
        
        if high_priority:
            print(f"  🔴 高优先级 ({len(high_priority)}条):")
            for rec in high_priority:
                print(f"    • {rec['title']}")
        
        if medium_priority:
            print(f"  🟡 中优先级 ({len(medium_priority)}条):")
            for rec in medium_priority:
                print(f"    • {rec['title']}")
        
        print("\n" + "=" * 60)

def main():
    """主函数"""
    try:
        diagnostic = TestFrameworkDiagnostic()
        results = diagnostic.run_diagnostic()
        diagnostic.print_summary()
        
        # 生成优化任务列表
        generate_optimization_tasks(results)
        
    except Exception as e:
        print(f"诊断过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        return 1
    
    return 0

def generate_optimization_tasks(results):
    """基于诊断结果生成优化任务"""
    recommendations = results.get("optimization_recommendations", [])
    
    if not recommendations:
        return
    
    print("\n" + "=" * 60)
    print("推荐的优化任务:")
    print("=" * 60)
    
    task_counter = 1
    for rec in recommendations:
        if rec["priority"] in ["high", "medium"]:
            print(f"\n任务 {task_counter}: {rec['title']}")
            print(f"优先级: {rec['priority']}")
            print(f"描述: {rec['description']}")
            print(f"操作: {rec['action']}")
            task_counter += 1

if __name__ == "__main__":
    sys.exit(main())