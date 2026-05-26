"""
报告生成器类
用于生成数据库性能测试报告
"""

import json
import os
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Any, Optional
import logging

logger = logging.getLogger(__name__)

class ReportGenerator:
    """报告生成器"""
    
    def __init__(self):
        self.template_dir = Path(__file__).parent / "templates"
        self.template_dir.mkdir(exist_ok=True)
    
    def generate_summary_report(self, analysis_results: Dict[str, Any], 
                               performance_results: Dict[str, Any],
                               stress_results: Dict[str, Any],
                               output_dir: Path) -> str:
        """生成综合报告"""
        report_path = output_dir / "summary_report.md"
        
        try:
            report_content = self._create_summary_content(
                analysis_results, performance_results, stress_results
            )
            
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            logger.info(f"综合报告已生成: {report_path}")
            return str(report_path)
            
        except Exception as e:
            logger.error(f"生成综合报告失败: {str(e)}")
            raise
    
    def generate_detailed_report(self, analysis_results: Dict[str, Any], 
                                performance_results: Dict[str, Any],
                                stress_results: Dict[str, Any],
                                output_dir: Path) -> str:
        """生成详细报告"""
        report_path = output_dir / "detailed_report.md"
        
        try:
            report_content = self._create_detailed_content(
                analysis_results, performance_results, stress_results
            )
            
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            logger.info(f"详细报告已生成: {report_path}")
            return str(report_path)
            
        except Exception as e:
            logger.error(f"生成详细报告失败: {str(e)}")
            raise
    
    def generate_optimization_report(self, analysis_results: Dict[str, Any], 
                                    performance_results: Dict[str, Any],
                                    output_dir: Path) -> str:
        """生成优化建议报告"""
        report_path = output_dir / "optimization_report.md"
        
        try:
            report_content = self._create_optimization_content(
                analysis_results, performance_results
            )
            
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            logger.info(f"优化建议报告已生成: {report_path}")
            return str(report_path)
            
        except Exception as e:
            logger.error(f"生成优化建议报告失败: {str(e)}")
            raise
    
    def generate_performance_report(self, performance_results: Dict[str, Any],
                                   output_dir: Path) -> str:
        """生成性能测试报告"""
        report_path = output_dir / "performance_report.md"
        
        try:
            report_content = self._create_performance_content(performance_results)
            
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(report_content)
            
            logger.info(f"性能测试报告已生成: {report_path}")
            return str(report_path)
            
        except Exception as e:
            logger.error(f"生成性能测试报告失败: {str(e)}")
            raise
    
    def _create_summary_content(self, analysis_results: Dict[str, Any], 
                               performance_results: Dict[str, Any],
                               stress_results: Dict[str, Any]) -> str:
        """创建综合报告内容"""
        content = f"""# 数据库性能测试综合报告

## 测试概述
- **测试时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **测试范围**: 数据库性能分析、性能测试、压力测试
- **测试环境**: Sprint 27+1 测试环境

## 数据库概览
"""
        
        # 添加数据库信息
        for db_name in analysis_results.keys():
            if db_name in analysis_results and "performance_score" in analysis_results[db_name]:
                score = analysis_results[db_name]["performance_score"]
                content += f"\n### {db_name.capitalize()} 数据库\n"
                content += f"- **性能评分**: {score:.1f}/100\n"
                
                if "bottlenecks" in analysis_results[db_name]:
                    bottlenecks = analysis_results[db_name]["bottlenecks"]
                    content += f"- **发现瓶颈**: {len(bottlenecks)} 个\n"
                
                if db_name in performance_results and "summary" in performance_results[db_name]:
                    perf_summary = performance_results[db_name]["summary"]
                    content += f"- **性能评估**: {perf_summary.get('overall_assessment', 'N/A')}\n"
        
        # 添加性能测试摘要
        content += "\n## 性能测试摘要\n"
        
        for db_name in performance_results.keys():
            if db_name in performance_results and "summary" in performance_results[db_name]:
                perf_summary = performance_results[db_name]["summary"]
                content += f"\n### {db_name.capitalize()} 性能测试\n"
                content += f"- **性能评分**: {perf_summary.get('performance_score', 'N/A')}\n"
                
                if "issues_found" in perf_summary:
                    issues = perf_summary["issues_found"]
                    content += f"- **发现问题**: {len(issues)} 个\n"
        
        # 添加压力测试摘要
        content += "\n## 压力测试摘要\n"
        
        for db_name in stress_results.keys():
            if db_name in stress_results and "summary" in stress_results[db_name]:
                stress_summary = stress_results[db_name]["summary"]
                content += f"\n### {db_name.capitalize()} 压力测试\n"
                content += f"- **最大并发数**: {stress_summary.get('max_concurrency_tested', 'N/A')}\n"
                content += f"- **最大吞吐量**: {stress_summary.get('max_throughput_rps', 'N/A'):.1f} RPS\n"
                content += f"- **总体评估**: {stress_summary.get('overall_assessment', 'N/A')}\n"
        
        # 添加关键发现
        content += "\n## 关键发现\n"
        
        all_issues = []
        for db_name in analysis_results.keys():
            if db_name in analysis_results and "bottlenecks" in analysis_results[db_name]:
                bottlenecks = analysis_results[db_name]["bottlenecks"]
                for bottleneck in bottlenecks:
                    if bottleneck.get("severity") == "high":
                        all_issues.append(f"- {db_name}: {bottleneck.get('description', 'N/A')}")
        
        if all_issues:
            content += "\n### 高优先级问题\n"
            content += "\n".join(all_issues[:5])  # 只显示前5个高优先级问题
        else:
            content += "\n未发现高优先级问题\n"
        
        # 添加建议
        content += "\n## 总体建议\n"
        
        recommendations = []
        for db_name in analysis_results.keys():
            if db_name in analysis_results and "recommendations" in analysis_results[db_name]:
                recs = analysis_results[db_name]["recommendations"]
                recommendations.extend(recs[:3])  # 每个数据库取前3条建议
        
        if recommendations:
            for rec in set(recommendations)[:10]:  # 去重并取前10条
                content += f"- {rec}\n"
        else:
            content += "- 数据库性能良好，建议定期监控\n"
        
        # 添加测试结论
        content += f"""
## 测试结论

### 整体评估
根据本次测试结果，数据库性能总体{'良好' if self._calculate_overall_score(analysis_results) >= 70 else '需要优化'}。

### 建议行动
1. **立即处理**: 解决所有高优先级瓶颈问题
2. **短期优化**: 实施性能测试中发现的问题修复
3. **长期规划**: 根据压力测试结果规划容量扩展
4. **持续监控**: 建立数据库性能监控体系

### 后续步骤
1. 与开发团队沟通测试结果
2. 制定性能优化计划
3. 安排优化方案实施
4. 定期进行性能回归测试

---
*报告生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
*测试环境: Sprint 27+1*
"""
        
        return content
    
    def _create_detailed_content(self, analysis_results: Dict[str, Any], 
                                performance_results: Dict[str, Any],
                                stress_results: Dict[str, Any]) -> str:
        """创建详细报告内容"""
        content = f"""# 数据库性能测试详细报告

## 测试信息
- **测试时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **测试环境**: Sprint 27+1 测试环境
- **报告版本**: 1.0

## 目录
1. 数据库配置分析
2. 性能测试结果
3. 压力测试结果
4. 瓶颈分析
5. 优化建议
6. 附录

---

## 1. 数据库配置分析
"""
        
        # 数据库配置分析
        for db_name, analysis in analysis_results.items():
            content += f"\n### 1.{list(analysis_results.keys()).index(db_name)+1} {db_name.capitalize()} 数据库配置\n"
            
            if "configuration" in analysis:
                config = analysis["configuration"]
                content += "#### 主要配置参数\n"
                content += "| 参数 | 值 | 说明 |\n"
                content += "|------|-----|------|\n"
                
                config_mapping = {
                    "max_connections": ("最大连接数", "建议根据实际负载调整"),
                    "shared_buffers": ("共享缓冲区", "通常设置为内存的25%"),
                    "effective_cache_size": ("有效缓存大小", "通常设置为内存的50-75%"),
                    "work_mem": ("工作内存", "每个查询操作可用的内存"),
                    "maintenance_work_mem": ("维护工作内存", "维护操作可用的内存")
                }
                
                for key, (display_name, description) in config_mapping.items():
                    value = config.get(key, "N/A")
                    content += f"| {display_name} | {value} | {description} |\n"
            
            if "performance_metrics" in analysis:
                metrics = analysis["performance_metrics"]
                content += "\n#### 性能指标\n"
                
                if "connections" in metrics:
                    conn = metrics["connections"]
                    content += f"- 总连接数: {conn.get('total', 'N/A')}\n"
                    content += f"- 活跃连接数: {conn.get('active', 'N/A')}\n"
                    content += f"- 空闲连接数: {conn.get('idle', 'N/A')}\n"
                
                if "cache_hit_ratio" in metrics:
                    cache = metrics["cache_hit_ratio"]
                    content += f"- 堆缓存命中率: {cache.get('heap', 0):.1%}\n"
                    content += f"- 索引缓存命中率: {cache.get('index', 0):.1%}\n"
        
        # 性能测试结果
        content += "\n---\n\n## 2. 性能测试结果\n"
        
        for db_name, performance in performance_results.items():
            content += f"\n### 2.{list(performance_results.keys()).index(db_name)+1} {db_name.capitalize()} 性能测试\n"
            
            if "tests" in performance:
                tests = performance["tests"]
                
                if "basic_queries" in tests:
                    basic = tests["basic_queries"]
                    content += "#### 基础查询性能\n"
                    content += "| 查询类型 | 响应时间(ms) | 评估 |\n"
                    content += "|----------|-------------|------|\n"
                    
                    query_types = [
                        ("simple_query_ms", "简单查询", 10),
                        ("complex_query_ms", "复杂查询", 100),
                        ("aggregate_query_ms", "聚合查询", 50)
                    ]
                    
                    for key, display_name, threshold in query_types:
                        value = basic.get(key, 0)
                        assessment = "✅ 良好" if value <= threshold else "⚠️ 需要优化"
                        content += f"| {display_name} | {value:.2f} | {assessment} |\n"
                
                if "index_performance" in tests:
                    idx = tests["index_performance"]
                    if "index_improvement_percent" in idx:
                        improvement = idx["index_improvement_percent"]
                        content += f"\n#### 索引性能提升\n"
                        content += f"- 索引查询性能提升: {improvement:.1f}%\n"
                        content += f"- 评估: {'✅ 良好' if improvement >= 50 else '⚠️ 需要优化'}\n"
        
        # 压力测试结果
        content += "\n---\n\n## 3. 压力测试结果\n"
        
        for db_name, stress in stress_results.items():
            content += f"\n### 3.{list(stress_results.keys()).index(db_name)+1} {db_name.capitalize()} 压力测试\n"
            
            if "test_scenarios" in stress:
                scenarios = stress["test_scenarios"]
                content += "#### 测试场景结果\n"
                content += "| 并发数 | 成功率 | 平均响应时间(ms) | 吞吐量(RPS) | 评估 |\n"
                content += "|--------|--------|-----------------|-------------|------|\n"
                
                for scenario in scenarios:
                    if "statistics" in scenario:
                        stats = scenario["statistics"]
                        concurrency = scenario["concurrency"]
                        success_rate = stats.get("success_rate", 0) * 100
                        avg_response = stats.get("avg_response_time_ms", 0)
                        throughput = stats.get("throughput_rps", 0)
                        
                        # 评估标准
                        if success_rate >= 99 and avg_response <= 100:
                            assessment = "✅ 优秀"
                        elif success_rate >= 95 and avg_response <= 200:
                            assessment = "⚠️ 良好"
                        else:
                            assessment = "❌ 需要优化"
                        
                        content += f"| {concurrency} | {success_rate:.1f}% | {avg_response:.1f} | {throughput:.1f} | {assessment} |\n"
        
        # 瓶颈分析
        content += "\n---\n\n## 4. 瓶颈分析\n"
        
        for db_name, analysis in analysis_results.items():
            if "bottlenecks" in analysis:
                bottlenecks = analysis["bottlenecks"]
                if bottlenecks:
                    content += f"\n### {db_name.capitalize()} 数据库瓶颈\n"
                    
                    for i, bottleneck in enumerate(bottlenecks, 1):
                        severity = bottleneck.get("severity", "unknown")
                        description = bottleneck.get("description", "N/A")
                        recommendation = bottleneck.get("recommendation", "N/A")
                        
                        severity_icon = {
                            "high": "🔴",
                            "medium": "🟡", 
                            "low": "🟢"
                        }.get(severity, "⚪")
                        
                        content += f"\n#### {severity_icon} 瓶颈 {i}: {severity.upper()} 优先级\n"
                        content += f"- **问题描述**: {description}\n"
                        content += f"- **优化建议**: {recommendation}\n"
        
        # 优化建议
        content += "\n---\n\n## 5. 优化建议\n"
        
        all_recommendations = []
        for db_name, analysis in analysis_results.items():
            if "recommendations" in analysis:
                all_recommendations.extend(analysis["recommendations"])
        
        if all_recommendations:
            content += "### 总体优化建议\n"
            unique_recs = list(set(all_recommendations))
            
            # 分类建议
            immediate = []
            short_term = []
            long_term = []
            
            for rec in unique_recs:
                rec_lower = rec.lower()
                if any(keyword in rec_lower for keyword in ["立即", "紧急", "必须", "尽快"]):
                    immediate.append(rec)
                elif any(keyword in rec_lower for keyword in ["建议", "考虑", "优化", "改进"]):
                    short_term.append(rec)
                else:
                    long_term.append(rec)
            
            if immediate:
                content += "\n#### 🔴 立即处理\n"
                for rec in immediate[:5]:
                    content += f"- {rec}\n"
            
            if short_term:
                content += "\n#### 🟡 短期优化\n"
                for rec in short_term[:10]:
                    content += f"- {rec}\n"
            
            if long_term:
                content += "\n#### 🟢 长期规划\n"
                for rec in long_term[:5]:
                    content += f"- {rec}\n"
        else:
            content += "\n未发现需要优化的关键问题。\n"
        
        # 附录
        content += f"""
---

## 6. 附录

### 测试方法说明
1. **数据库分析**: 通过系统表和统计信息分析数据库配置和性能
2. **性能测试**: 执行标准查询测试，测量响应时间和吞吐量
3. **压力测试**: 模拟高并发场景，测试系统极限性能

### 评估标准
- **优秀**: 性能评分 ≥ 90，无高优先级瓶颈
- **良好**: 性能评分 ≥ 70，有少量中低优先级问题
- **需要优化**: 性能评分 < 70，存在高优先级瓶颈

### 后续步骤建议
1. **立即行动**: 解决所有高优先级问题
2. **一周内**: 实施短期优化建议
3. **一个月内**: 完成长期规划项目
4. **持续**: 建立性能监控和定期测试机制

### 联系方式
- **测试负责人**: 运维工程师
- **测试时间**: {datetime.now().strftime('%Y-%m-%d')}
- **环境信息**: Sprint 27+1测试环境

---
*报告版本: 1.0*
*生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
*© AI-Ready 项目团队*
"""
        
        return content
    
    def _create_optimization_content(self, analysis_results: Dict[str, Any], 
                                    performance_results: Dict[str, Any]) -> str:
        """创建优化建议内容"""
        content = f"""# 数据库优化建议报告

## 报告概述
- **生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **适用范围**: Sprint 27+1 测试环境
- **目标**: 提供数据库性能优化指导

## 优化优先级矩阵
| 优先级 | 说明 | 处理时限 |
|--------|------|----------|
| 🔴 高 | 严重影响性能，必须立即处理 | 24小时内 |
| 🟡 中 | 影响用户体验，建议尽快处理 | 1周内 |
| 🟢 低 | 优化空间，可规划处理 | 1个月内 |

## 详细优化建议
"""
        
        # 按数据库组织优化建议
        for db_name, analysis in analysis_results.items():
            content += f"\n## {db_name.capitalize()} 数据库\n"
            
            # 当前性能状况
            score = analysis.get("performance_score", 0)
            content += f"### 当前性能评分: {score:.1f}/100\n"
            
            if score >= 90:
                content += "**评估**: 性能优秀，建议保持当前配置\n\n"
            elif score >= 70:
                content += "**评估**: 性能良好，有优化空间\n\n"
            elif score >= 50:
                content += "**评估**: 性能一般，需要重点优化\n\n"
            else:
                content += "**评估**: 性能较差，急需全面优化\n\n"
            
            # 瓶颈和建议
            if "bottlenecks" in analysis:
                bottlenecks = analysis["bottlenecks"]
                if bottlenecks:
                    content += "### 发现的问题\n"
                    
                    for bottleneck in bottlenecks:
                        severity = bottleneck.get("severity", "unknown")
                        description = bottleneck.get("description", "N/A")
                        
                        severity_display = {
                            "high": "🔴 高优先级",
                            "medium": "🟡 中优先级",
                            "low": "🟢 低优先级"
                        }.get(severity, "⚪ 未分类")
                        
                        content += f"- **{severity_display}**: {description}\n"
            
            # 优化建议
            if "recommendations" in analysis:
                recommendations = analysis["recommendations"]
                if recommendations:
                    content += "\n### 具体优化建议\n"
                    
                    for i, rec in enumerate(recommendations[:10], 1):  # 最多显示10条
                        content += f"{i}. {rec}\n"
            
            # 性能测试相关建议
            if db_name in performance_results and "summary" in performance_results[db_name]:
                perf_summary = performance_results[db_name]["summary"]
                
                if "recommendations" in perf_summary:
                    perf_recs = perf_summary["recommendations"]
                    if perf_recs:
                        content += "\n### 性能测试建议\n"
                        for rec in perf_recs[:5]:  # 最多显示5条
                            content += f"- {rec}\n"
        
        # 总体优化策略
        content += """
---

## 总体优化策略

### 1. 配置优化
#### 立即执行
1. 调整连接池配置
2. 优化缓存设置
3. 更新统计信息

#### 短期计划  
1. 调整数据库参数
2. 优化存储配置
3. 设置自动维护任务

### 2. 查询优化
#### SQL优化
1. 分析慢查询日志
2. 优化复杂查询
3. 减少全表扫描

#### 索引优化
1. 添加缺失索引
2. 删除冗余索引
3. 优化复合索引

### 3. 架构优化
#### 数据模型
1. 规范化设计审查
2. 分区策略优化
3. 数据归档规划

#### 高可用
1. 主从复制配置
2. 读写分离实现
3. 故障转移策略

### 4. 监控运维
#### 监控体系
1. 性能指标监控
2. 告警规则设置
3. 容量规划预测

#### 维护流程
1. 定期备份策略
2. 版本升级计划
3. 安全加固措施

---

## 实施计划

### 第一阶段 (1-3天)
1. 解决所有高优先级问题
2. 调整关键配置参数
3. 建立基础监控

### 第二阶段 (1-2周)
1. 实施查询优化
2. 完善索引策略
3. 建立告警机制

### 第三阶段 (1个月)
1. 架构优化实施
2. 高可用部署
3. 自动化运维

### 第四阶段 (持续)
1. 性能调优迭代
2. 新技术评估
3. 最佳实践推广

---

## 成功指标

### 性能指标
1. **查询响应时间**: 减少30%以上
2. **系统吞吐量**: 提升50%以上
3. **连接池使用率**: 控制在80%以下
4. **缓存命中率**: 达到95%以上

### 业务指标
1. **用户满意度**: 提升20%以上
2. **系统可用性**: 达到99.9%以上
3. **故障恢复时间**: 控制在5分钟以内

### 运维指标
1. **监控覆盖率**: 达到100%
2. **自动化程度**: 达到80%以上
3. **告警准确率**: 达到95%以上

---

## 风险与应对

### 技术风险
1. **优化不兼容**: 回滚方案，分阶段实施
2. **性能回退**: 性能基线，A/B测试
3. **数据丢失**: 完整备份，验证流程

### 业务风险
1. **服务中断**: 维护窗口，灰度发布
2. **用户体验**: 用户反馈，性能监控
3. **数据一致**: 数据校验，事务控制

### 管理风险
1. **资源不足**: 资源规划，优先级管理
2. **时间延误**: 里程碑检查，风险预警
3. **沟通不畅**: 定期同步，文档共享

---

## 总结

数据库性能优化是一个持续的过程，需要结合监控、分析和调优的循环。
本报告提供的建议应结合实际情况进行调整和实施。

**关键成功因素**:
1. 领导支持和资源保障
2. 团队协作和技术能力
3. 数据驱动和持续改进
4. 风险管理和应急预案

**建议下一步**:
1. 召开优化方案评审会
2. 制定详细实施计划
3. 组建优化专项小组
4. 建立效果评估机制

---
*报告版本: 1.0*
*生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
*© AI-Ready 项目团队 - 运维工程部*
"""
        
        return content
    
    def _create_performance_content(self, performance_results: Dict[str, Any]) -> str:
        """创建性能测试内容"""
        content = f"""# 数据库性能测试报告

## 测试概况
- **测试时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- **测试环境**: Sprint 27+1 测试环境
- **测试目的**: 评估数据库性能，识别优化机会

## 测试方法

### 1. 测试类型
#### 基础查询测试
- 简单查询: SELECT 1
- 复杂查询: 多表关联查询
- 聚合查询: COUNT, SUM等聚合操作

#### 索引性能测试
- 无索引查询性能
- 有索引查询性能
- 索引性能提升对比

#### 事务性能测试
- 单事务性能
- 多事务并发性能
- 事务隔离级别测试

#### 连接池测试
- 连接建立时间
- 连接复用效率
- 连接池容量测试

### 2. 测试指标
- **响应时间**: 查询执行时间(ms)
- **吞吐量**: 每秒处理查询数(QPS)
- **成功率**: 成功请求比例
- **资源使用**: CPU、内存、连接数

## 测试结果
"""
        
        # 测试结果详情
        for db_name, performance in performance_results.items():
            content += f"\n## {db_name.capitalize()} 数据库\n"
            
            if "summary" in performance:
                summary = performance["summary"]
                content += f"### 测试摘要\n"
                content += f"- **性能评分**: {summary.get('performance_score', 'N/A')}\n"
                content += f"- **总体评估**: {summary.get('overall_assessment', 'N/A')}\n"
                
                if "issues_found" in summary:
                    issues = summary["issues_found"]
                    content += f"- **发现问题**: {len(issues)} 个\n"
            
            if "tests" in performance:
                tests = performance["tests"]
                content += "\n### 详细测试结果\n"
                
                # 基础查询
                if "basic_queries" in tests:
                    basic = tests["basic_queries"]
                    content += "#### 基础查询性能\n"
                    
                    query_results = [
                        ("simple_query_ms", "简单查询", "SELECT 1"),
                        ("complex_query_ms", "复杂查询", "多表查询"),
                        ("aggregate_query_ms", "聚合查询", "COUNT统计")
                    ]
                    
                    content += "| 查询类型 | SQL示例 | 响应时间(ms) | 评估 |\n"
                    content += "|----------|---------|-------------|------|\n"
                    
                    for key, display_name, sql_example in query_results:
                        value = basic.get(key, 0)
                        threshold = 10 if key == "simple_query_ms" else 100 if key == "complex_query_ms" else 50
                        assessment = "✅ 通过" if value <= threshold else "⚠️ 需优化"
                        content += f"| {display_name} | `{sql_example}` | {value:.2f} | {assessment} |\n"
                
                # 索引性能
                if "index_performance" in tests:
                    idx = tests["index_performance"]
                    content += "\n#### 索引性能测试\n"
                    
                    if "no_index_query_ms" in idx and "with_index_query_ms" in idx:
                        no_idx = idx["no_index_query_ms"]
                        with_idx = idx["with_index_query_ms"]
                        improvement = idx.get("index_improvement_percent", 0)
                        
                        content += "| 测试场景 | 响应时间(ms) | 性能对比 |\n"
                        content += "|----------|-------------|----------|\n"
                        content += f"| 无索引查询 | {no_idx:.2f} | 基准 |\n"
                        content += f"| 有索引查询 | {with_idx:.2f} | {improvement:.1f}% 提升 |\n"
                        
                        if improvement >= 50:
                            content += "\n**评估**: ✅ 索引效果显著\n"
                        else:
                            content += "\n**评估**: ⚠️ 索引效果有限，建议优化\n"
                
                # 事务性能
                if "transaction_performance" in tests:
                    trans = tests["transaction_performance"]
                    if "transaction_times_ms" in trans:
                        times = trans["transaction_times_ms"]
                        content += "\n#### 事务性能测试\n"
                        
                        content += "| 统计指标 | 值(ms) | 说明 |\n"
                        content += "|----------|--------|------|\n"
                        content += f"| 平均时间 | {times.get('avg', 0):.2f} | 事务平均执行时间 |\n"
                        content += f"| 最长时间 | {times.get('max', 0):.2f} | 事务最长执行时间 |\n"
                        content += f"| P95时间 | {times.get('p95', 0):.2f} | 95%事务在此时间内完成 |\n"
                        
                        avg_time = times.get('avg', 0)
                        if avg_time <= 100:
                            content += "\n**评估**: ✅ 事务性能优秀\n"
                        elif avg_time <= 500:
                            content += "\n**评估**: ⚠️ 事务性能良好\n"
                        else:
                            content += "\n**评估**: ❌ 事务性能需要优化\n"
        
        # 性能对比分析
        content += """
---

## 性能对比分析

### 数据库性能排名
"""
        
        # 计算性能排名
        db_scores = {}
        for db_name, performance in performance_results.items():
            if "summary" in performance:
                score = performance["summary"].get("performance_score", 0)
                db_scores[db_name] = score
        
        if db_scores:
            sorted_dbs = sorted(db_scores.items(), key=lambda x: x[1], reverse=True)
            
            content += "| 排名 | 数据库 | 性能评分 | 等级 |\n"
            content += "|------|--------|----------|------|\n"
            
            for i, (db_name, score) in enumerate(sorted_dbs, 1):
                if score >= 90:
                    grade = "A+ (优秀)"
                elif score >= 80:
                    grade = "A (良好)"
                elif score >= 70:
                    grade = "B (一般)"
                elif score >= 60:
                    grade = "C (需要优化)"
                else:
                    grade = "D (急需优化)"
                
                content += f"| {i} | {db_name.capitalize()} | {score:.1f} | {grade} |\n"
        
        # 关键发现
        content += """
---

## 关键发现与建议

### 1. 优势项目
"""
        
        # 找出性能优秀的项目
        excellent_dbs = []
        for db_name, performance in performance_results.items():
            if "summary" in performance:
                score = performance["summary"].get("performance_score", 0)
                if score >= 90:
                    excellent_dbs.append(db_name)
        
        if excellent_dbs:
            for db_name in excellent_dbs:
                content += f"- **{db_name.capitalize()}数据库**: 性能优秀，配置合理，可作为参考模板\n"
        else:
            content += "- 暂无性能特别优秀的数据库\n"
        
        content += """
### 2. 主要问题
"""
        
        # 找出共性问题
        common_issues = {}
        for db_name, performance in performance_results.items():
            if "summary" in performance and "issues_found" in performance["summary"]:
                issues = performance["summary"]["issues_found"]
                for issue in issues:
                    common_issues[issue] = common_issues.get(issue, 0) + 1
        
        if common_issues:
            sorted_issues = sorted(common_issues.items(), key=lambda x: x[1], reverse=True)
            for issue, count in sorted_issues[:5]:  # 最多显示5个共性问题
                content += f"- **{issue}** ({count}个数据库存在此问题)\n"
        else:
            content += "- 未发现共性问题\n"
        
        content += """
### 3. 优化优先级
"""
        
        # 根据性能评分确定优化优先级
        optimization_priority = []
        for db_name, performance in performance_results.items():
            if "summary" in performance:
                score = performance["summary"].get("performance_score", 0)
                if score < 70:
                    optimization_priority.append((db_name, score, "高"))
                elif score < 80:
                    optimization_priority.append((db_name, score, "中"))
                elif score < 90:
                    optimization_priority.append((db_name, score, "低"))
        
        if optimization_priority:
            optimization_priority.sort(key=lambda x: x[1])  # 按评分升序排序
            content += "| 数据库 | 性能评分 | 优化优先级 | 建议行动 |\n"
            content += "|--------|----------|------------|----------|\n"
            
            for db_name, score, priority in optimization_priority:
                if priority == "高":
                    action = "立即优化"
                elif priority == "中":
                    action = "近期优化"
                else:
                    action = "规划优化"
                
                content += f"| {db_name.capitalize()} | {score:.1f} | {priority} | {action} |\n"
        else:
            content += "- 所有数据库性能良好，无需紧急优化\n"
        
        # 测试结论
        content += f"""
---

## 测试结论

### 总体评估
根据本次性能测试结果，数据库性能总体{'良好' if self._calculate_overall_performance_score(performance_results) >= 70 else '需要优化'}。

### 主要成就
1. **性能达标**: {len([p for p in performance_results.values() if p.get('summary', {}).get('performance_score', 0) >= 70])}/{len(performance_results)} 个数据库性能达标
2. **稳定性好**: 所有测试场景均成功执行
3. **数据完整**: 测试过程中未发生数据丢失

### 待改进项
1. **性能优化**: 需要优化 {len([p for p in performance_results.values() if p.get('summary', {}).get('performance_score', 0) < 70])} 个数据库
2. **监控完善**: 需要建立更完善的性能监控体系
3. **文档更新**: 需要更新数据库配置文档

### 后续建议
1. **立即行动**: 解决高优先级优化项
2. **短期计划**: 完善性能监控和告警
3. **长期规划**: 建立性能优化长效机制

### 风险提示
1. **性能风险**: 高并发场景下可能出现性能瓶颈
2. **容量风险**: 数据增长可能导致性能下降
3. **运维风险**: 缺乏自动化运维工具

---

## 附录

### A. 测试环境配置
- **操作系统**: Linux
- **数据库版本**: PostgreSQL 14
- **测试工具**: 自定义Python测试框架
- **监控工具**: Prometheus