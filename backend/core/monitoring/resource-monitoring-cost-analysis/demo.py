#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
资源监控与成本分析模块演示脚本
展示模块的核心功能
"""

import sys
import os

# 添加当前目录到Python路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from datetime import datetime, timedelta
import pandas as pd
import numpy as np
from decimal import Decimal

print("=" * 70)
print("   资源监控与成本分析模块演示")
print("   Sprint 27+1 测试环境")
print("=" * 70)
print()

# 演示1: 创建模拟监控数据
print("1. 创建模拟监控数据")
print("-" * 40)

# 生成时间序列
dates = pd.date_range(
    start=datetime.now() - timedelta(days=7),
    end=datetime.now(),
    freq='h'  # 每小时一个数据点
)

# 模拟CPU使用率 (20-80%)
cpu_data = pd.DataFrame({
    'timestamp': dates,
    'value': np.random.uniform(20, 80, len(dates)),
    'instance': ['test-server-01'] * len(dates),
    'job': ['node-exporter'] * len(dates)
})

# 模拟内存使用率 (40-90%)
memory_data = pd.DataFrame({
    'timestamp': dates,
    'value': np.random.uniform(40, 90, len(dates)),
    'instance': ['test-server-01'] * len(dates),
    'job': ['node-exporter'] * len(dates)
})

# 模拟磁盘使用率 (30-85%)
disk_data = pd.DataFrame({
    'timestamp': dates,
    'value': np.random.uniform(30, 85, len(dates)),
    'instance': ['test-server-01'] * len(dates),
    'job': ['node-exporter'] * len(dates),
    'mountpoint': ['/'] * len(dates)
})

metrics_data = {
    'node_cpu_usage': cpu_data,
    'node_memory_usage': memory_data,
    'node_disk_usage': disk_data
}

print(f"生成 {len(metrics_data)} 个监控指标:")
for name, df in metrics_data.items():
    print(f"  {name}: {len(df)} 个数据点")
    print(f"    时间范围: {df['timestamp'].min()} 到 {df['timestamp'].max()}")
    print(f"    平均值: {df['value'].mean():.1f}%")
print()

# 演示2: 成本计算
print("2. 成本计算演示")
print("-" * 40)

# 简单的成本计算函数
def calculate_simple_cost(metrics_data):
    """简单的成本计算"""
    costs = {}
    
    # CPU成本: 假设t3.medium实例，$0.0416/小时
    if 'node_cpu_usage' in metrics_data:
        cpu_avg = metrics_data['node_cpu_usage']['value'].mean()
        cpu_hours = len(metrics_data['node_cpu_usage']) / 24  # 数据点数量转换为天数再转换为小时
        cpu_cost = Decimal('0.0416') * Decimal(str(cpu_hours)) * Decimal(str(cpu_avg / 100))
        costs['cpu'] = cpu_cost
    
    # 内存成本: 假设16GB内存，类似SSD存储成本
    if 'node_memory_usage' in metrics_data:
        memory_avg = metrics_data['node_memory_usage']['value'].mean()
        memory_gb = 16
        memory_cost = Decimal('0.08') * Decimal(str(memory_gb)) * Decimal(str(memory_avg / 100))
        costs['memory'] = memory_cost
    
    # 磁盘成本: 假设100GB gp2存储
    if 'node_disk_usage' in metrics_data:
        disk_avg = metrics_data['node_disk_usage']['value'].mean()
        disk_gb = 100
        disk_cost = Decimal('0.10') * Decimal(str(disk_gb)) * Decimal(str(disk_avg / 100))
        costs['disk'] = disk_cost
    
    return costs

costs = calculate_simple_cost(metrics_data)
total_cost = sum(costs.values())

print(f"7天资源成本估算:")
for resource, cost in costs.items():
    print(f"  {resource}: ${cost:.2f}")
print(f"  总成本: ${total_cost:.2f}")
print()

# 演示3: 资源分析
print("3. 资源分析演示")
print("-" * 40)

def analyze_resources(metrics_data):
    """分析资源使用情况"""
    analysis = {}
    
    for name, df in metrics_data.items():
        values = df['value']
        
        analysis[name] = {
            'average': float(values.mean()),
            'max': float(values.max()),
            'min': float(values.min()),
            'std': float(values.std()),
            'trend': 'stable'  # 简化版本，实际应该计算趋势
        }
        
        # 简单异常检测
        threshold_high = values.mean() + 2 * values.std()
        anomalies = values[values > threshold_high]
        if len(anomalies) > 0:
            analysis[name]['anomalies'] = len(anomalies)
            analysis[name]['anomaly_percentage'] = len(anomalies) / len(values) * 100
    
    return analysis

analysis = analyze_resources(metrics_data)

for metric, stats in analysis.items():
    print(f"{metric}:")
    print(f"  平均值: {stats['average']:.1f}%")
    print(f"  最大值: {stats['max']:.1f}%")
    print(f"  最小值: {stats['min']:.1f}%")
    print(f"  标准差: {stats['std']:.1f}%")
    
    if 'anomalies' in stats:
        print(f"  异常检测: {stats['anomalies']} 个异常点 ({stats['anomaly_percentage']:.1f}%)")
    print()

# 演示4: 优化建议
print("4. 优化建议")
print("-" * 40)

def generate_recommendations(metrics_data, costs):
    """生成优化建议"""
    recommendations = []
    
    # CPU优化建议
    if 'node_cpu_usage' in metrics_data:
        cpu_avg = metrics_data['node_cpu_usage']['value'].mean()
        if cpu_avg < 30:
            recommendations.append({
                'resource': 'CPU',
                'issue': '使用率过低',
                'current': f'{cpu_avg:.1f}%',
                'recommendation': '考虑使用更小的实例类型',
                'potential_savings': '20-50%'
            })
        elif cpu_avg > 80:
            recommendations.append({
                'resource': 'CPU',
                'issue': '使用率过高',
                'current': f'{cpu_avg:.1f}%',
                'recommendation': '考虑升级实例类型或优化应用',
                'potential_savings': '提升性能'
            })
    
    # 内存优化建议
    if 'node_memory_usage' in metrics_data:
        memory_avg = metrics_data['node_memory_usage']['value'].mean()
        if memory_avg > 85:
            recommendations.append({
                'resource': '内存',
                'issue': '使用率过高',
                'current': f'{memory_avg:.1f}%',
                'recommendation': '增加内存或优化应用内存使用',
                'potential_savings': '避免内存不足'
            })
    
    # 磁盘优化建议
    if 'node_disk_usage' in metrics_data:
        disk_avg = metrics_data['node_disk_usage']['value'].mean()
        if disk_avg > 90:
            recommendations.append({
                'resource': '磁盘',
                'issue': '使用率过高',
                'current': f'{disk_avg:.1f}%',
                'recommendation': '清理无用文件或增加存储容量',
                'potential_savings': '避免磁盘空间不足'
            })
    
    # 成本优化建议
    if total_cost > Decimal('50'):
        recommendations.append({
            'resource': '成本',
            'issue': '成本较高',
            'current': f'${total_cost:.2f}',
            'recommendation': '考虑使用预留实例或节省计划',
            'potential_savings': '30-60%'
        })
    
    return recommendations

recommendations = generate_recommendations(metrics_data, costs)

if recommendations:
    print("优化建议:")
    for i, rec in enumerate(recommendations, 1):
        print(f"{i}. [{rec['resource']}] {rec['issue']} ({rec['current']})")
        print(f"   建议: {rec['recommendation']}")
        print(f"   潜在节省: {rec['potential_savings']}")
else:
    print("当前资源配置合理，无需优化")
print()

# 演示5: 部署说明
print("5. 部署和使用说明")
print("-" * 40)

print("部署步骤:")
print("1. 初始化环境: ./deploy.sh --init")
print("2. 部署监控栈: ./deploy.sh --deploy")
print("3. 启动服务: ./start.sh")
print()
print("访问地址:")
print("  • Prometheus: http://localhost:9090")
print("  • Grafana: http://localhost:3000 (admin/admin)")
print("  • 监控服务: http://localhost:8000")
print("  • 监控API: http://localhost:8000/api/v1/metrics/resource")
print()
print("功能特性:")
print("  ✓ 实时资源监控 (CPU, 内存, 磁盘, 网络)")
print("  ✓ 成本分析和优化建议")
print("  ✓ 异常检测和告警")
print("  ✓ 可视化仪表板")
print("  ✓ 容器化部署")

print()
print("=" * 70)
print("演示完成！")
print("=" * 70)