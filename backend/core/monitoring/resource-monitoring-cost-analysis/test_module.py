#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
资源监控与成本分析模块测试脚本
用于验证各个模块的功能
"""

import asyncio
import logging
import sys
from datetime import datetime, timedelta
import pandas as pd
import numpy as np

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)


def test_data_collector():
    """测试数据采集器"""
    logger.info("测试数据采集器...")
    
    try:
        # 模拟监控数据
        dates = pd.date_range(start='2026-04-01', end='2026-04-28', freq='H')
        
        # CPU使用率数据
        cpu_data = pd.DataFrame({
            'timestamp': dates,
            'value': np.random.uniform(20, 80, len(dates)),
            'instance': ['server-01'] * len(dates),
            'job': ['node-exporter'] * len(dates)
        })
        
        # 内存使用率数据
        memory_data = pd.DataFrame({
            'timestamp': dates,
            'value': np.random.uniform(40, 90, len(dates)),
            'instance': ['server-01'] * len(dates),
            'job': ['node-exporter'] * len(dates)
        })
        
        # 磁盘使用率数据
        disk_data = pd.DataFrame({
            'timestamp': dates,
            'value': np.random.uniform(30, 85, len(dates)),
            'instance': ['server-01'] * len(dates),
            'job': ['node-exporter'] * len(dates),
            'mountpoint': ['/'] * len(dates)
        })
        
        metrics_data = {
            'node_cpu_usage': cpu_data,
            'node_memory_usage': memory_data,
            'node_disk_usage': disk_data
        }
        
        logger.info(f"生成模拟数据: {len(metrics_data)} 个指标")
        for name, df in metrics_data.items():
            logger.info(f"  {name}: {len(df)} 行数据，时间范围: {df['timestamp'].min()} 到 {df['timestamp'].max()}")
        
        return metrics_data
        
    except Exception as e:
        logger.error(f"测试数据采集器失败: {e}")
        return {}


def test_cost_calculator(metrics_data):
    """测试成本计算器"""
    logger.info("测试成本计算器...")
    
    try:
        from cost_calculators.resource_cost_calculator import (
            ResourceCostCalculator, ResourceType, PricingModel,
            ResourceSpec, CostItem
        )
        
        calculator = ResourceCostCalculator()
        
        # 测试实例成本计算
        instance_cost = calculator.calculate_instance_cost(
            instance_type="t3.medium",
            usage_hours=720,  # 30天
            provider="aws"
        )
        logger.info(f"t3.medium实例30天成本: ${instance_cost}")
        
        # 测试存储成本计算
        storage_cost = calculator.calculate_storage_cost(
            storage_gb=100,
            storage_type="gp2",
            provider="aws",
            duration_days=30
        )
        logger.info(f"100GB gp2存储30天成本: ${storage_cost}")
        
        # 测试从监控指标计算成本
        cost_items = calculator.calculate_costs_from_metrics(
            metrics_data=metrics_data,
            period_hours=24
        )
        logger.info(f"从监控指标生成 {len(cost_items)} 个成本项")
        
        # 汇总成本
        if cost_items:
            cost_summary = calculator.summarize_costs(cost_items)
            logger.info(f"成本汇总: 总成本 ${cost_summary.total_cost}")
            
            for resource_type, cost in cost_summary.cost_by_resource_type.items():
                logger.info(f"  {resource_type.value}: ${cost}")
        
        return cost_items
        
    except ImportError as e:
        logger.error(f"导入成本计算器失败: {e}")
        return []
    except Exception as e:
        logger.error(f"测试成本计算器失败: {e}")
        return []


def test_resource_analyzer(metrics_data):
    """测试资源分析器"""
    logger.info("测试资源分析器...")
    
    try:
        from analyzers.resource_analyzer import ResourceAnalyzer
        
        analyzer = ResourceAnalyzer()
        
        analysis_results = {}
        
        # 分析CPU使用
        if "node_cpu_usage" in metrics_data:
            cpu_analysis = analyzer.analyze_cpu_usage(metrics_data["node_cpu_usage"])
            analysis_results["cpu"] = cpu_analysis
            logger.info(f"CPU分析结果:")
            logger.info(f"  平均使用率: {cpu_analysis.get('average_usage', 0):.1f}%")
            logger.info(f"  峰值使用率: {cpu_analysis.get('peak_usage', 0):.1f}%")
            logger.info(f"  使用趋势: {cpu_analysis.get('trend', 'unknown')}")
        
        # 分析内存使用
        if "node_memory_usage" in metrics_data:
            memory_analysis = analyzer.analyze_memory_usage(metrics_data["node_memory_usage"])
            analysis_results["memory"] = memory_analysis
            logger.info(f"内存分析结果:")
            logger.info(f"  平均使用率: {memory_analysis.get('average_usage', 0):.1f}%")
            logger.info(f"  峰值使用率: {memory_analysis.get('peak_usage', 0):.1f}%")
        
        # 检测异常
        anomalies = analyzer.detect_anomalies(metrics_data)
        if anomalies:
            analysis_results["anomalies"] = anomalies
            logger.warning(f"检测到 {len(anomalies)} 个异常")
            for anomaly in anomalies[:3]:  # 显示前3个异常
                logger.warning(f"  异常: {anomaly.get('metric')} - {anomaly.get('description')}")
        
        return analysis_results
        
    except ImportError as e:
        logger.error(f"导入资源分析器失败: {e}")
        return {}
    except Exception as e:
        logger.error(f"测试资源分析器失败: {e}")
        return {}


def test_dashboard_generator(metrics_data, cost_items, analysis_results):
    """测试仪表板生成器"""
    logger.info("测试仪表板生成器...")
    
    try:
        from visualizers.dashboard_generator import DashboardGenerator
        
        generator = DashboardGenerator()
        
        # 生成资源使用仪表板
        resource_dashboard = generator.generate_resource_dashboard(
            metrics_data=metrics_data,
            analysis_results=analysis_results
        )
        
        # 生成成本分析仪表板
        from cost_calculators.resource_cost_calculator import ResourceCostCalculator
        calculator = ResourceCostCalculator()
        cost_summary = calculator.summarize_costs(cost_items) if cost_items else None
        
        cost_dashboard = generator.generate_cost_dashboard(
            cost_data=cost_items,
            cost_summary=cost_summary
        )
        
        logger.info(f"仪表板生成完成:")
        logger.info(f"  资源仪表板长度: {len(resource_dashboard)} 字符")
        logger.info(f"  成本仪表板长度: {len(cost_dashboard)} 字符")
        
        # 保存示例仪表板
        with open("test_resource_dashboard.html", "w", encoding="utf-8") as f:
            f.write(resource_dashboard)
        logger.info("资源仪表板已保存到: test_resource_dashboard.html")
        
        with open("test_cost_dashboard.html", "w", encoding="utf-8") as f:
            f.write(cost_dashboard)
        logger.info("成本仪表板已保存到: test_cost_dashboard.html")
        
        return True
        
    except ImportError as e:
        logger.error(f"导入仪表板生成器失败: {e}")
        return False
    except Exception as e:
        logger.error(f"测试仪表板生成器失败: {e}")
        return False


def test_alerts(metrics_data, cost_items, analysis_results):
    """测试告警系统"""
    logger.info("测试告警系统...")
    
    try:
        from alerts.intelligent_alerter import IntelligentAlerter
        
        alerter = IntelligentAlerter()
        
        # 检查告警
        alerts = alerter.check_alerts(
            metrics_data=metrics_data,
            cost_data=cost_items,
            analysis_results=analysis_results
        )
        
        logger.info(f"生成 {len(alerts)} 个告警")
        
        for alert in alerts[:5]:  # 显示前5个告警
            logger.warning(f"告警 [{alert.get('severity')}]: {alert.get('message')}")
        
        return alerts
        
    except ImportError as e:
        logger.error(f"导入告警系统失败: {e}")
        return []
    except Exception as e:
        logger.error(f"测试告警系统失败: {e}")
        return []


async def test_main_app():
    """测试主应用程序"""
    logger.info("测试主应用程序...")
    
    try:
        from src.main import MonitoringConfig, ResourceMonitoringApp
        
        # 创建配置
        config = MonitoringConfig(
            prometheus_url="http://localhost:9090",
            collection_interval=60,  # 1分钟（测试用）
            retention_days=1
        )
        
        # 创建应用程序
        app = ResourceMonitoringApp(config)
        
        # 模拟数据
        metrics_data = test_data_collector()
        
        # 运行单个周期
        logger.info("运行应用程序单个周期...")
        
        # 收集指标
        collection_success = await app.collect_metrics()
        logger.info(f"指标收集: {'成功' if collection_success else '失败'}")
        
        # 计算成本
        cost_summary = await app.calculate_costs()
        if cost_summary:
            logger.info(f"成本计算: 总成本 ${cost_summary.total_cost}")
        
        # 分析资源
        analysis_results = await app.analyze_resources()
        logger.info(f"资源分析: {len(analysis_results)} 个分析结果")
        
        # 检查告警
        alerts = await app.check_alerts()
        logger.info(f"告警检查: {len(alerts)} 个告警")
        
        # 清理
        await app.cleanup()
        
        logger.info("主应用程序测试完成")
        return True
        
    except ImportError as e:
        logger.error(f"导入主应用程序失败: {e}")
        return False
    except Exception as e:
        logger.error(f"测试主应用程序失败: {e}")
        return False


def run_all_tests():
    """运行所有测试"""
    logger.info("=" * 60)
    logger.info("开始运行资源监控与成本分析模块测试")
    logger.info("=" * 60)
    
    # 测试数据采集器
    metrics_data = test_data_collector()
    if not metrics_data:
        logger.error("数据采集器测试失败，中止后续测试")
        return False
    
    # 测试成本计算器
    cost_items = test_cost_calculator(metrics_data)
    
    # 测试资源分析器
    analysis_results = test_resource_analyzer(metrics_data)
    
    # 测试仪表板生成器
    dashboard_success = test_dashboard_generator(metrics_data, cost_items, analysis_results)
    
    # 测试告警系统
    alerts = test_alerts(metrics_data, cost_items, analysis_results)
    
    # 测试主应用程序
    main_app_success = asyncio.run(test_main_app())
    
    logger.info("=" * 60)
    logger.info("测试结果汇总:")
    logger.info(f"  数据采集器: {'✓' if metrics_data else '✗'}")
    logger.info(f"  成本计算器: {'✓' if cost_items is not None else '✗'}")
    logger.info(f"  资源分析器: {'✓' if analysis_results else '✗'}")
    logger.info(f"  仪表板生成器: {'✓' if dashboard_success else '✗'}")
    logger.info(f"  告警系统: {'✓' if alerts is not None else '✗'}")
    logger.info(f"  主应用程序: {'✓' if main_app_success else '✗'}")
    logger.info("=" * 60)
    
    all_passed = all([
        bool(metrics_data),
        cost_items is not None,
        bool(analysis_results),
        dashboard_success,
        alerts is not None,
        main_app_success
    ])
    
    if all_passed:
        logger.info("🎉 所有测试通过！")
        logger.info("")
        logger.info("下一步:")
        logger.info("1. 运行 ./start.sh 初始化环境")
        logger.info("2. 运行 ./start.sh 选择部署选项")
        logger.info("3. 访问 http://localhost:3000 查看监控仪表板")
        logger.info("4. 访问 http://localhost:8000 使用监控API")
    else:
        logger.error("❌ 部分测试失败")
        logger.info("")
        logger.info("建议:")
        logger.info("1. 检查依赖安装: pip install -r requirements.txt")
        logger.info("2. 检查模块导入路径")
        logger.info("3. 查看详细错误日志")
    
    return all_passed


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description="资源监控与成本分析模块测试工具")
    parser.add_argument("--all", action="store_true", help="运行所有测试")
    parser.add_argument("--collector", action="store_true", help="测试数据采集器")
    parser.add_argument("--calculator", action="store_true", help="测试成本计算器")
    parser.add_argument("--analyzer", action="store_true", help="测试资源分析器")
    parser.add_argument("--dashboard", action="store_true", help="测试仪表板生成器")
    parser.add_argument("--alerts", action="store_true", help="测试告警系统")
    parser.add_argument("--main", action="store_true", help="测试主应用程序")
    
    args = parser.parse_args()
    
    if not any(vars(args).values()):
        args.all = True
    
    if args.all:
        success = run_all_tests()
        sys.exit(0 if success else 1)
    else:
        if args.collector:
            test_data_collector()
        
        if args.calculator:
            metrics_data = test_data_collector()
            test_cost_calculator(metrics_data)
        
        if args.analyzer:
            metrics_data = test_data_collector()
            test_resource_analyzer(metrics_data)
        
        if args.dashboard:
            metrics_data = test_data_collector()
            cost_items = test_cost_calculator(metrics_data)
            analysis_results = test_resource_analyzer(metrics_data)
            test_dashboard_generator(metrics_data, cost_items, analysis_results)
        
        if args.alerts:
            metrics_data = test_data_collector()
            cost_items = test_cost_calculator(metrics_data)
            analysis_results = test_resource_analyzer(metrics_data)
            test_alerts(metrics_data, cost_items, analysis_results)
        
        if args.main:
            asyncio.run(test_main_app())


if __name__ == "__main__":
    main()