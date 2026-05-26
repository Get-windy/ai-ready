#!/usr/bin/env python3
"""
数据库性能测试主程序
用于Sprint 27+1测试环境数据库性能优化与压力测试
"""

import os
import sys
import time
import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional

# 添加项目根目录到Python路径
sys.path.append(str(Path(__file__).parent.parent.parent.parent))

from performance_test_utils import (
    DatabaseAnalyzer,
    PerformanceTester,
    StressTester,
    ReportGenerator,
    DatabaseConfig
)

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/app/logs/performance_test.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

class DatabasePerformanceTestRunner:
    """数据库性能测试运行器"""
    
    def __init__(self):
        self.test_id = f"performance_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        self.results_dir = Path("/app/results") / self.test_id
        self.results_dir.mkdir(parents=True, exist_ok=True)
        
        # 数据库配置
        self.db_configs = self._load_database_configs()
        
        # 测试工具
        self.analyzer = DatabaseAnalyzer()
        self.performance_tester = PerformanceTester()
        self.stress_tester = StressTester()
        self.report_generator = ReportGenerator()
    
    def _load_database_configs(self) -> Dict[str, DatabaseConfig]:
        """加载数据库配置"""
        configs = {}
        
        # 主数据库配置
        configs["main"] = DatabaseConfig(
            name="主数据库",
            host=os.getenv("POSTGRES_MAIN_HOST", "sprint-postgres-main"),
            port=int(os.getenv("POSTGRES_MAIN_PORT", "5432")),
            database=os.getenv("POSTGRES_MAIN_DB", "sprint_gateway_db"),
            username=os.getenv("POSTGRES_MAIN_USER", "sprint_gateway_user"),
            password=os.getenv("POSTGRES_MAIN_PASSWORD", "sprint_gateway_pass_2026")
        )
        
        # 库存数据库配置
        configs["inventory"] = DatabaseConfig(
            name="库存数据库",
            host=os.getenv("POSTGRES_INVENTORY_HOST", "sprint-postgres-inventory"),
            port=int(os.getenv("POSTGRES_INVENTORY_PORT", "5432")),
            database=os.getenv("POSTGRES_INVENTORY_DB", "sprint_inventory_db"),
            username=os.getenv("POSTGRES_INVENTORY_USER", "sprint_inventory_user"),
            password=os.getenv("POSTGRES_INVENTORY_PASSWORD", "sprint_inventory_pass_2026")
        )
        
        # 财务数据库配置
        configs["finance"] = DatabaseConfig(
            name="财务数据库",
            host=os.getenv("POSTGRES_FINANCE_HOST", "sprint-postgres-finance"),
            port=int(os.getenv("POSTGRES_FINANCE_PORT", "5432")),
            database=os.getenv("POSTGRES_FINANCE_DB", "sprint_finance_db"),
            username=os.getenv("POSTGRES_FINANCE_USER", "sprint_finance_user"),
            password=os.getenv("POSTGRES_FINANCE_PASSWORD", "sprint_finance_pass_2026")
        )
        
        # AI数据库配置
        configs["ai"] = DatabaseConfig(
            name="AI数据库",
            host=os.getenv("POSTGRES_AI_HOST", "sprint-postgres-ai"),
            port=int(os.getenv("POSTGRES_AI_PORT", "5432")),
            database=os.getenv("POSTGRES_AI_DB", "sprint_ai_db"),
            username=os.getenv("POSTGRES_AI_USER", "sprint_ai_user"),
            password=os.getenv("POSTGRES_AI_PASSWORD", "sprint_ai_pass_2026")
        )
        
        return configs
    
    def run_database_analysis(self) -> Dict[str, Any]:
        """运行数据库性能分析"""
        logger.info("开始数据库性能分析...")
        
        analysis_results = {}
        
        for db_name, config in self.db_configs.items():
            logger.info(f"分析数据库: {config.name}")
            
            try:
                # 连接数据库
                if not self.analyzer.connect(config):
                    logger.warning(f"无法连接到数据库: {config.name}")
                    continue
                
                # 执行分析
                analysis = self.analyzer.analyze_performance(config)
                analysis_results[db_name] = analysis
                
                # 保存分析结果
                analysis_file = self.results_dir / f"{db_name}_analysis.json"
                with open(analysis_file, 'w', encoding='utf-8') as f:
                    json.dump(analysis, f, indent=2, ensure_ascii=False)
                
                logger.info(f"数据库分析完成: {config.name}")
                
            except Exception as e:
                logger.error(f"数据库分析失败 {config.name}: {str(e)}")
                analysis_results[db_name] = {"error": str(e)}
        
        return analysis_results
    
    def run_performance_tests(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """运行性能测试"""
        logger.info("开始数据库性能测试...")
        
        performance_results = {}
        
        for db_name, config in self.db_configs.items():
            logger.info(f"性能测试数据库: {config.name}")
            
            try:
                # 连接数据库
                if not self.performance_tester.connect(config):
                    logger.warning(f"无法连接到数据库进行性能测试: {config.name}")
                    continue
                
                # 获取分析结果中的瓶颈信息
                bottlenecks = analysis_results.get(db_name, {}).get("bottlenecks", [])
                
                # 执行性能测试
                performance = self.performance_tester.run_tests(
                    config=config,
                    bottlenecks=bottlenecks
                )
                performance_results[db_name] = performance
                
                # 保存性能测试结果
                performance_file = self.results_dir / f"{db_name}_performance.json"
                with open(performance_file, 'w', encoding='utf-8') as f:
                    json.dump(performance, f, indent=2, ensure_ascii=False)
                
                logger.info(f"性能测试完成: {config.name}")
                
            except Exception as e:
                logger.error(f"性能测试失败 {config.name}: {str(e)}")
                performance_results[db_name] = {"error": str(e)}
        
        return performance_results
    
    def run_stress_tests(self) -> Dict[str, Any]:
        """运行压力测试"""
        logger.info("开始数据库压力测试...")
        
        stress_results = {}
        
        for db_name, config in self.db_configs.items():
            logger.info(f"压力测试数据库: {config.name}")
            
            try:
                # 连接数据库
                if not self.stress_tester.connect(config):
                    logger.warning(f"无法连接到数据库进行压力测试: {config.name}")
                    continue
                
                # 执行压力测试
                stress_test = self.stress_tester.run_stress_test(config)
                stress_results[db_name] = stress_test
                
                # 保存压力测试结果
                stress_file = self.results_dir / f"{db_name}_stress.json"
                with open(stress_file, 'w', encoding='utf-8') as f:
                    json.dump(stress_test, f, indent=2, ensure_ascii=False)
                
                logger.info(f"压力测试完成: {config.name}")
                
            except Exception as e:
                logger.error(f"压力测试失败 {config.name}: {str(e)}")
                stress_results[db_name] = {"error": str(e)}
        
        return stress_results
    
    def generate_reports(self, analysis_results: Dict[str, Any], 
                        performance_results: Dict[str, Any],
                        stress_results: Dict[str, Any]) -> Dict[str, str]:
        """生成测试报告"""
        logger.info("生成测试报告...")
        
        report_files = {}
        
        try:
            # 生成综合报告
            summary_report = self.report_generator.generate_summary_report(
                analysis_results=analysis_results,
                performance_results=performance_results,
                stress_results=stress_results,
                output_dir=self.results_dir
            )
            report_files["summary"] = summary_report
            
            # 生成详细报告
            detailed_report = self.report_generator.generate_detailed_report(
                analysis_results=analysis_results,
                performance_results=performance_results,
                stress_results=stress_results,
                output_dir=self.results_dir
            )
            report_files["detailed"] = detailed_report
            
            # 生成优化建议报告
            optimization_report = self.report_generator.generate_optimization_report(
                analysis_results=analysis_results,
                performance_results=performance_results,
                output_dir=self.results_dir
            )
            report_files["optimization"] = optimization_report
            
            # 生成性能测试报告
            performance_report = self.report_generator.generate_performance_report(
                performance_results=performance_results,
                output_dir=self.results_dir
            )
            report_files["performance"] = performance_report
            
            logger.info("测试报告生成完成")
            
        except Exception as e:
            logger.error(f"报告生成失败: {str(e)}")
            report_files["error"] = str(e)
        
        return report_files
    
    def save_test_summary(self, all_results: Dict[str, Any]) -> str:
        """保存测试摘要"""
        summary = {
            "test_id": self.test_id,
            "timestamp": datetime.now().isoformat(),
            "database_count": len(self.db_configs),
            "databases": list(self.db_configs.keys()),
            "results_dir": str(self.results_dir),
            "summary": {}
        }
        
        # 添加测试结果摘要
        for db_name in self.db_configs.keys():
            summary["summary"][db_name] = {
                "analyzed": db_name in all_results.get("analysis", {}),
                "performance_tested": db_name in all_results.get("performance", {}),
                "stress_tested": db_name in all_results.get("stress", {})
            }
        
        # 保存摘要
        summary_file = self.results_dir / "test_summary.json"
        with open(summary_file, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        
        return str(summary_file)
    
    def run_all_tests(self) -> Dict[str, Any]:
        """运行所有测试"""
        logger.info(f"开始数据库性能测试套件 (ID: {self.test_id})")
        
        all_results = {}
        
        try:
            # 1. 数据库性能分析
            start_time = time.time()
            analysis_results = self.run_database_analysis()
            all_results["analysis"] = analysis_results
            analysis_time = time.time() - start_time
            logger.info(f"数据库分析完成，耗时: {analysis_time:.2f}秒")
            
            # 2. 性能测试
            start_time = time.time()
            performance_results = self.run_performance_tests(analysis_results)
            all_results["performance"] = performance_results
            performance_time = time.time() - start_time
            logger.info(f"性能测试完成，耗时: {performance_time:.2f}秒")
            
            # 3. 压力测试
            start_time = time.time()
            stress_results = self.run_stress_tests()
            all_results["stress"] = stress_results
            stress_time = time.time() - start_time
            logger.info(f"压力测试完成，耗时: {stress_time:.2f}秒")
            
            # 4. 生成报告
            start_time = time.time()
            report_files = self.generate_reports(analysis_results, performance_results, stress_results)
            all_results["reports"] = report_files
            report_time = time.time() - start_time
            logger.info(f"报告生成完成，耗时: {report_time:.2f}秒")
            
            # 5. 保存测试摘要
            summary_file = self.save_test_summary(all_results)
            all_results["summary_file"] = summary_file
            
            # 添加测试统计信息
            all_results["statistics"] = {
                "total_time": analysis_time + performance_time + stress_time + report_time,
                "analysis_time": analysis_time,
                "performance_time": performance_time,
                "stress_time": stress_time,
                "report_time": report_time,
                "total_databases": len(self.db_configs),
                "successful_analysis": sum(1 for r in analysis_results.values() if "error" not in r),
                "successful_performance": sum(1 for r in performance_results.values() if "error" not in r),
                "successful_stress": sum(1 for r in stress_results.values() if "error" not in r)
            }
            
            logger.info("所有测试完成！")
            
        except Exception as e:
            logger.error(f"测试套件执行失败: {str(e)}")
            all_results["error"] = str(e)
        
        return all_results

def main():
    """主函数"""
    try:
        # 创建测试运行器
        runner = DatabasePerformanceTestRunner()
        
        # 运行所有测试
        results = runner.run_all_tests()
        
        # 输出测试结果摘要
        if "statistics" in results:
            stats = results["statistics"]
            print(f"\n{'='*60}")
            print("数据库性能测试结果摘要")
            print(f"{'='*60}")
            print(f"测试ID: {runner.test_id}")
            print(f"总耗时: {stats['total_time']:.2f}秒")
            print(f"数据库数量: {stats['total_databases']}")
            print(f"成功分析: {stats['successful_analysis']}/{stats['total_databases']}")
            print(f"成功性能测试: {stats['successful_performance']}/{stats['total_databases']}")
            print(f"成功压力测试: {stats['successful_stress']}/{stats['total_databases']}")
            print(f"结果目录: {runner.results_dir}")
            
            if "reports" in results:
                print("\n生成的报告:")
                for report_name, report_path in results["reports"].items():
                    if isinstance(report_path, str):
                        print(f"  - {report_name}: {report_path}")
            
            print(f"{'='*60}")
        
        # 检查测试结果
        if "error" in results:
            print(f"\n❌ 测试执行失败: {results['error']}")
            sys.exit(1)
        
        # 检查是否有失败的测试
        failed_tests = 0
        for db_name in runner.db_configs:
            if "analysis" in results and db_name in results["analysis"] and "error" in results["analysis"][db_name]:
                print(f"❌ 数据库分析失败 {db_name}: {results['analysis'][db_name]['error']}")
                failed_tests += 1
        
        if failed_tests > 0:
            print(f"\n❌ 有 {failed_tests} 个测试失败")
            sys.exit(1)
        
        print("\n✅ 所有测试执行成功！")
        
    except KeyboardInterrupt:
        print("\n⏹️ 测试被用户中断")
        sys.exit(130)
    except Exception as e:
        print(f"\n❌ 测试执行失败: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()