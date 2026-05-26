#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
资源监控与成本分析模块简单演示
"""

import sys
import os
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from decimal import Decimal

print("资源监控与成本分析模块演示")
print("=" * 50)

# 1. 创建模拟数据
print("\n1. 创建模拟监控数据")
dates = pd.date_range(
    start=datetime.now() - timedelta(days=7),
    end=datetime.now(),
    freq='h'
)

cpu_data = pd.DataFrame({
    'timestamp': dates,
    'value': np.random.uniform(20, 80, len(dates)),
    'instance': ['test-server'] * len(dates)
})

memory_data = pd.DataFrame({
    'timestamp': dates,
    'value': np.random.uniform(40, 90, len(dates)),
    'instance': ['test-server'] * len(dates)
})

metrics = {
    'cpu': cpu_data,
    'memory': memory_data
}

print(f"生成 {len(metrics)} 个监控指标，共 {len(dates)} 小时数据")

# 2. 成本计算
print("\n2. 成本计算示例")

def calculate_cost(metrics):
    """计算资源成本"""
    costs = {}
    
    # CPU成本
    if 'cpu' in metrics:
        cpu_avg = metrics['cpu']['value'].mean()
        # t3.medium实例: $0.0416/小时
        cpu_cost = Decimal('0.0416') * Decimal(str(len(dates))) * Decimal(str(cpu_avg / 100))
        costs['cpu'] = cpu_cost
    
    # 内存成本
    if 'memory' in metrics:
        memory_avg = metrics['memory']['value'].mean()
        # 16GB内存，类似SSD存储成本
        memory_cost = Decimal('0.08') * Decimal('16') * Decimal(str(memory_avg / 100))
        costs['memory'] = memory_cost
    
    return costs

costs = calculate_cost(metrics)
total_cost = sum(costs.values())

print("7天资源成本估算:")
for resource, cost in costs.items():
    print(f"  {resource}: ${float(cost):.2f}")
print(f"  总成本: ${float(total_cost):.2f}")

# 3. 资源分析
print("\n3. 资源使用分析")

for name, df in metrics.items():
    values = df['value']
    print(f"\n{name}使用情况:")
    print(f"  平均值: {values.mean():.1f}%")
    print(f"  最大值: {values.max():.1f}%")
    print(f"  最小值: {values.min():.1f}%")
    
    # 简单异常检测
    if values.max() > 80:
        print(f"  警告: 检测到高使用率峰值")

# 4. 优化建议
print("\n4. 优化建议")

recommendations = []

# CPU建议
cpu_avg = metrics['cpu']['value'].mean()
if cpu_avg < 30:
    recommendations.append("CPU使用率较低，考虑使用更小的实例类型")
elif cpu_avg > 70:
    recommendations.append("CPU使用率较高，考虑优化应用或升级实例")

# 内存建议
memory_avg = metrics['memory']['value'].mean()
if memory_avg > 80:
    recommendations.append("内存使用率较高，考虑增加内存或优化应用")

# 成本建议
if float(total_cost) > 50:
    recommendations.append("总成本较高，考虑使用预留实例或节省计划")

if recommendations:
    print("建议:")
    for i, rec in enumerate(recommendations, 1):
        print(f"  {i}. {rec}")
else:
    print("当前资源配置合理")

# 5. 部署说明
print("\n5. 快速开始")
print("部署命令:")
print("  ./deploy.sh --init     # 初始化环境")
print("  ./deploy.sh --deploy   # 部署监控栈")
print("  ./start.sh             # 启动服务")
print("\n访问地址:")
print("  Prometheus: http://localhost:9090")
print("  Grafana:    http://localhost:3000")
print("  监控服务:   http://localhost:8000")

print("\n" + "=" * 50)
print("演示完成！")