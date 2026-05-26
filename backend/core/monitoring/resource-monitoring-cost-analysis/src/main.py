"""
资源监控与成本分析主应用程序
集成所有模块，提供完整的监控和成本分析功能
"""

import asyncio
import logging
import sys
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
import pandas as pd
from dataclasses import dataclass
import json

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('resource_monitoring.log')
    ]
)

logger = logging.getLogger(__name__)

# 导入各个模块
try:
    from data_collectors.prometheus_collector import PrometheusCollector, MetricType
    from cost_calculators.resource_cost_calculator import (
        ResourceCostCalculator, ResourceType, PricingModel,
        ResourceSpec, CostItem, CostSummary
    )
    from analyzers.resource_analyzer import ResourceAnalyzer
    from visualizers.dashboard_generator import DashboardGenerator
    from alerts.intelligent_alerter import IntelligentAlerter
except ImportError as e:
    logger.error(f"导入模块失败: {e}")
    logger.info("请确保所有依赖模块已正确安装")
    sys.exit(1)


@dataclass
class MonitoringConfig:
    """监控配置"""
    prometheus_url: str = "http://localhost:9090"
    collection_interval: int = 300  # 数据收集间隔（秒）
    retention_days: int = 30  # 数据保留天数
    cost_calculation_interval: int = 3600  # 成本计算间隔（秒）
    alert_check_interval: int = 60  # 告警检查间隔（秒）
    
    # 要监控的指标
    metrics_to_monitor: List[str] = None
    
    def __post_init__(self):
        if self.metrics_to_monitor is None:
            self.metrics_to_monitor = [
                "node_cpu_usage",
                "node_memory_usage",
                "node_disk_usage",
                "node_network_receive",
                "node_network_transmit",
                "container_cpu_usage",
                "container_memory_usage",
                "http_requests_total",
                "http_request_duration_seconds"
            ]


class ResourceMonitoringApp:
    """资源监控与成本分析应用程序"""
    
    def __init__(self, config: MonitoringConfig):
        """
        初始化应用程序
        
        Args:
            config: 监控配置
        """
        self.config = config
        self.is_running = False
        
        # 初始化各个组件
        self.prometheus_collector = PrometheusCollector(
            prometheus_url=config.prometheus_url
        )
        self.cost_calculator = ResourceCostCalculator()
        self.resource_analyzer = ResourceAnalyzer()
        self.dashboard_generator = DashboardGenerator()
        self.alerter = IntelligentAlerter()
        
        # 数据存储
        self.metrics_data: Dict[str, pd.DataFrame] = {}
        self.cost_data: List[CostItem] = []
        self.analysis_results: Dict[str, Any] = {}
        
        logger.info("资源监控与成本分析应用程序初始化完成")
    
    async def collect_metrics(self):
        """收集监控指标"""
        logger.info("开始收集监控指标...")
        
        try:
            # 收集最近1小时的数据
            end_time = datetime.now()
            start_time = end_time - timedelta(hours=1)
            
            metrics = await self.prometheus_collector.collect_all_metrics(
                start_time=start_time,
                end_time=end_time,
                step="1m",
                metric_names=self.config.metrics_to_monitor
            )
            
            # 合并到历史数据
            for metric_name, df in metrics.items():
                if metric_name in self.metrics_data:
                    # 合并数据，去除重复
                    combined_df = pd.concat([self.metrics_data[metric_name], df])
                    combined_df = combined_df.drop_duplicates(subset=['timestamp'], keep='last')
                    combined_df = combined_df.sort_values('timestamp')
                    
                    # 应用数据保留策略
                    cutoff_time = datetime.now() - timedelta(days=self.config.retention_days)
                    combined_df = combined_df[combined_df['timestamp'] >= cutoff_time]
                    
                    self.metrics_data[metric_name] = combined_df
                else:
                    self.metrics_data[metric_name] = df
            
            logger.info(f"成功收集 {len(metrics)} 个指标的数据")
            
            # 记录收集的指标统计
            for metric_name, df in metrics.items():
                if not df.empty:
                    logger.debug(f"  {metric_name}: {len(df)} 行数据，时间范围: {df['timestamp'].min()} 到 {df['timestamp'].max()}")
            
            return True
            
        except Exception as e:
            logger.error(f"收集监控指标失败: {e}")
            return False
    
    async def calculate_costs(self):
        """计算资源成本"""
        logger.info("开始计算资源成本...")
        
        try:
            # 从监控指标计算成本
            new_cost_items = self.cost_calculator.calculate_costs_from_metrics(
                metrics_data=self.metrics_data,
                period_hours=1  # 计算最近1小时的成本
            )
            
            # 添加到成本数据
            self.cost_data.extend(new_cost_items)
            
            # 应用数据保留策略
            cutoff_time = datetime.now() - timedelta(days=self.config.retention_days)
            self.cost_data = [
                item for item in self.cost_data 
                if item.period_end >= cutoff_time
            ]
            
            # 生成成本汇总
            cost_summary = self.cost_calculator.summarize_costs(self.cost_data)
            
            logger.info(f"成本计算完成: 总成本 ${cost_summary.total_cost}")
            logger.info(f"  按资源类型: { {k.value: f'${v}' for k, v in cost_summary.cost_by_resource_type.items()} }")
            logger.info(f"  按服务: { {k: f'${v}' for k, v in cost_summary.cost_by_service.items()} }")
            
            return cost_summary
            
        except Exception as e:
            logger.error(f"计算资源成本失败: {e}")
            return None
    
    async def analyze_resources(self):
        """分析资源使用情况"""
        logger.info("开始分析资源使用情况...")
        
        try:
            analysis_results = {}
            
            # 分析CPU使用
            if "node_cpu_usage" in self.metrics_data:
                cpu_analysis = self.resource_analyzer.analyze_cpu_usage(
                    self.metrics_data["node_cpu_usage"]
                )
                analysis_results["cpu"] = cpu_analysis
                logger.info(f"CPU分析: 平均使用率 {cpu_analysis.get('average_usage', 0):.1f}%")
            
            # 分析内存使用
            if "node_memory_usage" in self.metrics_data:
                memory_analysis = self.resource_analyzer.analyze_memory_usage(
                    self.metrics_data["node_memory_usage"]
                )
                analysis_results["memory"] = memory_analysis
                logger.info(f"内存分析: 平均使用率 {memory_analysis.get('average_usage', 0):.1f}%")
            
            # 分析磁盘使用
            if "node_disk_usage" in self.metrics_data:
                disk_analysis = self.resource_analyzer.analyze_disk_usage(
                    self.metrics_data["node_disk_usage"]
                )
                analysis_results["disk"] = disk_analysis
                logger.info(f"磁盘分析: 平均使用率 {disk_analysis.get('average_usage', 0):.1f}%")
            
            # 分析网络使用
            if "node_network_transmit" in self.metrics_data:
                network_analysis = self.resource_analyzer.analyze_network_usage(
                    self.metrics_data["node_network_transmit"]
                )
                analysis_results["network"] = network_analysis
                logger.info(f"网络分析: 总传输量 {network_analysis.get('total_transfer_gb', 0):.2f} GB")
            
            # 检测异常
            anomalies = self.resource_analyzer.detect_anomalies(self.metrics_data)
            if anomalies:
                analysis_results["anomalies"] = anomalies
                logger.warning(f"检测到 {len(anomalies)} 个异常")
            
            self.analysis_results = analysis_results
            return analysis_results
            
        except Exception as e:
            logger.error(f"分析资源使用失败: {e}")
            return {}
    
    async def generate_dashboards(self):
        """生成监控仪表板"""
        logger.info("生成监控仪表板...")
        
        try:
            # 生成资源使用仪表板
            resource_dashboard = self.dashboard_generator.generate_resource_dashboard(
                metrics_data=self.metrics_data,
                analysis_results=self.analysis_results
            )
            
            # 生成成本分析仪表板
            cost_summary = self.cost_calculator.summarize_costs(self.cost_data)
            cost_dashboard = self.dashboard_generator.generate_cost_dashboard(
                cost_data=self.cost_data,
                cost_summary=cost_summary
            )
            
            # 生成优化建议仪表板
            optimization_dashboard = self.dashboard_generator.generate_optimization_dashboard(
                metrics_data=self.metrics_data,
                cost_data=self.cost_data,
                analysis_results=self.analysis_results
            )
            
            dashboards = {
                "resource": resource_dashboard,
                "cost": cost_dashboard,
                "optimization": optimization_dashboard
            }
            
            # 保存仪表板到文件
            for name, dashboard in dashboards.items():
                filename = f"dashboard_{name}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.html"
                with open(filename, 'w', encoding='utf-8') as f:
                    f.write(dashboard)
                logger.info(f"仪表板已保存到: {filename}")
            
            return dashboards
            
        except Exception as e:
            logger.error(f"生成仪表板失败: {e}")
            return {}
    
    async def check_alerts(self):
        """检查告警"""
        logger.debug("检查告警...")
        
        try:
            alerts = self.alerter.check_alerts(
                metrics_data=self.metrics_data,
                cost_data=self.cost_data,
                analysis_results=self.analysis_results
            )
            
            if alerts:
                for alert in alerts:
                    logger.warning(f"告警: {alert['severity']} - {alert['message']}")
                    
                    # 发送告警通知
                    await self.alerter.send_alert_notification(alert)
            
            return alerts
            
        except Exception as e:
            logger.error(f"检查告警失败: {e}")
            return []
    
    async def run_single_cycle(self):
        """运行单个监控周期"""
        logger.info("=" * 60)
        logger.info(f"开始监控周期: {datetime.now()}")
        logger.info("=" * 60)
        
        # 1. 收集监控指标
        collection_success = await self.collect_metrics()
        if not collection_success:
            logger.error("监控指标收集失败，跳过本周期")
            return False
        
        # 2. 计算资源成本
        cost_summary = await self.calculate_costs()
        if cost_summary:
            logger.info(f"周期成本汇总: ${cost_summary.total_cost}")
        
        # 3. 分析资源使用
        analysis_results = await self.analyze_resources()
        
        # 4. 检查告警
        alerts = await self.check_alerts()
        
        # 5. 定期生成仪表板（每6小时一次）
        current_hour = datetime.now().hour
        if current_hour % 6 == 0:  # 每6小时生成一次
            await self.generate_dashboards()
        
        # 6. 生成周期报告
        await self.generate_cycle_report(cost_summary, analysis_results, alerts)
        
        logger.info(f"监控周期完成: {datetime.now()}")
        return True
    
    async def generate_cycle_report(
        self,
        cost_summary: Optional[CostSummary],
        analysis_results: Dict[str, Any],
        alerts: List[Dict[str, Any]]
    ):
        """生成周期报告"""
        report = {
            "timestamp": datetime.now().isoformat(),
            "metrics_collected": len(self.metrics_data),
            "cost_summary": {
                "total_cost": str(cost_summary.total_cost) if cost_summary else "0",
                "currency": cost_summary.currency if cost_summary else "USD"
            },
            "resource_analysis": {
                "metrics_analyzed": len(analysis_results)
            },
            "alerts": {
                "total": len(alerts),
                "by_severity": {}
            },
            "data_statistics": {
                metric_name: {
                    "rows": len(df),
                    "time_range": {
                        "start": df['timestamp'].min().isoformat() if not df.empty else None,
                        "end": df['timestamp'].max().isoformat() if not df.empty else None
                    }
                }
                for metric_name, df in self.metrics_data.items()
            }
        }
        
        # 统计告警严重程度
        for alert in alerts:
            severity = alert['severity']
            report["alerts"]["by_severity"][severity] = report["alerts"]["by_severity"].get(severity, 0) + 1
        
        # 保存报告
        filename = f"cycle_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)
        
        logger.info(f"周期报告已保存到: {filename}")
    
    async def run_continuous(self):
        """持续运行监控"""
        self.is_running = True
        logger.info("开始持续监控...")
        
        cycle_count = 0
        
        try:
            while self.is_running:
                cycle_count += 1
                logger.info(f"第 {cycle_count} 个监控周期")
                
                # 运行单个周期
                success = await self.run_single_cycle()
                
                if not success:
                    logger.error("监控周期失败，等待重试...")
                    await asyncio.sleep(60)  # 失败后等待1分钟
                    continue
                
                # 等待下一个周期
                logger.info(f"等待 {self.config.collection_interval} 秒后开始下一个周期...")
                await asyncio.sleep(self.config.collection_interval)
                
        except KeyboardInterrupt:
            logger.info("收到停止信号，正在停止监控...")
        except Exception as e:
            logger.error(f"监控运行失败: {e}")
        finally:
            self.is_running = False
            await self.cleanup()
    
    async def cleanup(self):
        """清理资源"""
        logger.info("清理资源...")
        
        try:
            # 关闭Prometheus采集器
            await self.prometheus_collector.close()
            
            # 生成最终报告
            await self.generate_final_report()
            
            logger.info("资源清理完成")
            
        except Exception as e:
            logger.error(f"清理资源失败: {e}")
    
    async def generate_final_report(self):
        """生成最终报告"""
        logger.info("生成最终监控报告...")
        
        try:
            # 汇总所有成本数据
            final_cost_summary = self.cost_calculator.summarize_costs(self.cost_data)
            
            # 分析资源使用趋势
            resource_trends = {}
            for metric_name, df in self.metrics_data.items():
                if not df.empty:
                    # 计算每日平均值
                    df['date'] = df['timestamp'].dt.date
                    daily_avg = df.groupby('date')['value'].mean()
                    
                    resource_trends[metric_name] = {
                        "daily_averages": {
                            str(date): float(avg) for date, avg in daily_avg.items()
                        },
                        "overall_average": float(df['value'].mean()),
                        "overall_max": float(df['value'].max()),
                        "overall_min": float(df['value'].min())
                    }
            
            # 生成最终报告
            final_report = {
                "report_type": "final_monitoring_report",
                "generated_at": datetime.now().isoformat(),
                "monitoring_period": {
                    "start": min([df['timestamp'].min() for df in self.metrics_data.values() if not df.empty]).isoformat(),
                    "end": max([df['timestamp'].max() for df in self.metrics_data.values() if not df.empty]).isoformat()
                },
                "cost_summary": {
                    "total_cost": str(final_cost_summary.total_cost),
                    "currency": final_cost_summary.currency,
                    "by_resource_type": {
                        k.value: str(v) for k, v in final_cost_summary.cost_by_resource_type.items()
                    },
                    "by_service": {
                        k: str(v) for k, v in final_cost_summary.cost_by_service.items()
                    }
                },
                "resource_trends": resource_trends,
                "data_statistics": {
                    "total_metrics_collected": len(self.metrics_data),
                    "total_data_points": sum(len(df) for df in self.metrics_data.values()),
                    "metrics_details": {
                        metric_name: {
                            "data_points": len(df),
                            "time_range": {
                                "start": df['timestamp'].min().isoformat() if not df.empty else None,
                                "end": df['timestamp'].max().isoformat() if not df.empty else None
                            }
                        }
                        for metric_name, df in self.metrics_data.items()
                    }
                },
                "recommendations": self._generate_recommendations(final_cost_summary, resource_trends)
            }
            
            # 保存最终报告
            filename = f"final_monitoring_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(final_report, f, indent=2, ensure_ascii=False)
            
            logger.info(f"最终报告已保存到: {filename}")
            
            # 同时生成HTML版本
            html_report = self.dashboard_generator.generate_final_report_html(final_report)
            html_filename = f"final_monitoring_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.html"
            with open(html_filename, 'w', encoding='utf-8') as f:
                f.write(html_report)
            
            logger.info(f"HTML最终报告已保存到: {html_filename}")
            
        except Exception as e:
            logger.error(f"生成最终报告失败: {e}")
    
    def _generate_recommendations(
        self,
        cost_summary: CostSummary,
        resource_trends: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """生成优化建议"""
        recommendations = []
        
        # 成本优化建议
        total_cost = cost_summary.total_cost
        if total_cost > Decimal("1000"):  # 如果总成本超过1000美元
            recommendations.append({
                "category": "cost_optimization",
                "priority": "high",
                "title": "考虑使用预留实例",
                "description": "总成本较高，建议使用预留实例或节省计划以降低长期成本",
                "estimated_savings": "30-60%",
                "action_items": [
                    "分析工作负载模式",
                    "评估预留实例选项",
                    "制定预留实例采购计划"
                ]
            })
        
        # CPU使用率优化建议
        if "node_cpu_usage" in resource_trends:
            cpu_avg = resource_trends["node_cpu_usage"]["overall_average"]
            if cpu_avg < 30:  # CPU使用率过低
                recommendations.append({
                    "category": "resource_optimization",
                    "priority": "medium",
                    "title": "CPU资源过剩",
                    "description": f"平均CPU使用率仅为{cpu_avg:.1f}%，考虑使用更小的实例类型",
                    "estimated_savings": "20-50%",
                    "action_items": [
                        "评估降级实例类型的可行性",
                        "测试更小实例类型的性能",
                        "制定实例类型调整计划"
                    ]
                })
            elif cpu_avg > 80:  # CPU使用率过高
                recommendations.append({
                    "category": "performance_optimization",
                    "priority": "high",
                    "title": "CPU资源紧张",
                    "description": f"平均CPU使用率达到{cpu_avg:.1f}%，考虑升级实例类型或优化应用",
                    "estimated_savings": "N/A (需要投资)",
                    "action_items": [
                        "分析CPU使用高峰",
                        "优化应用代码",
                        "考虑自动扩展"
                    ]
                })
        
        # 内存使用率优化建议
        if "node_memory_usage" in resource_trends:
            memory_avg = resource_trends["node_memory_usage"]["overall_average"]
            if memory_avg > 85:  # 内存使用率过高
                recommendations.append({
                    "category": "performance_optimization",
                    "priority": "high",
                    "title": "内存资源紧张",
                    "description": f"平均内存使用率达到{memory_avg:.1f}%，存在内存不足风险",
                    "estimated_savings": "N/A (需要投资)",
                    "action_items": [
                        "分析内存使用模式",
                        "优化应用内存配置",
                        "考虑增加内存或使用内存优化实例"
                    ]
                })
        
        # 存储优化建议
        if "node_disk_usage" in resource_trends:
            disk_avg = resource_trends["node_disk_usage"]["overall_average"]
            if disk_avg > 90:  # 磁盘使用率过高
                recommendations.append({
                    "category": "storage_optimization",
                    "priority": "high",
                    "title": "磁盘空间紧张",
                    "description": f"平均磁盘使用率达到{disk_avg:.1f}%，需要清理或扩容",
                    "estimated_savings": "N/A (需要投资)",
                    "action_items": [
                        "分析磁盘使用情况",
                        "清理无用文件",
                        "考虑增加存储容量"
                    ]
                })
        
        return recommendations


async def main():
    """主函数"""
    # 创建配置
    config = MonitoringConfig(
        prometheus_url="http://localhost:9090",
        collection_interval=300,  # 5分钟
        retention_days=7,
        cost_calculation_interval=3600,  # 1小时
        alert_check_interval=60  # 1分钟
    )
    
    # 创建应用程序
    app = ResourceMonitoringApp(config)
    
    try:
        # 运行持续监控
        await app.run_continuous()
    except KeyboardInterrupt:
        logger.info("应用程序被用户中断")
    except Exception as e:
        logger.error(f"应用程序运行失败: {e}")
        return 1
    
    return 0


if __name__ == "__main__":
    # 运行应用程序
    exit_code = asyncio.run(main())
    sys.exit(exit_code)