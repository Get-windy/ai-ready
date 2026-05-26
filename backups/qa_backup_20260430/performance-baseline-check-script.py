#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
性能基线检查脚本 - 子任务4
版本: 1.0.0
创建日期: 2026-04-29
作者: qa-lead
项目: AI-Ready测试环境配置专项
Sprint: Sprint 27+1
任务ID: task_1777438750224_y4l4dft4a

功能: 执行测试环境的性能基线检查，建立性能基准并监控性能变化
"""

import os
import sys
import json
import yaml
import time
import logging
import argparse
import statistics
import subprocess
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Any, Tuple, Optional
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
import psutil
import requests
import numpy as np
from dataclasses import dataclass, asdict
from enum import Enum

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('performance-baseline-check.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

class PerformanceMetric(Enum):
    """性能指标枚举"""
    CPU_USAGE = "cpu_usage"
    MEMORY_USAGE = "memory_usage"
    DISK_IO = "disk_io"
    NETWORK_IO = "network_io"
    RESPONSE_TIME = "response_time"
    THROUGHPUT = "throughput"
    ERROR_RATE = "error_rate"
    CONCURRENT_USERS = "concurrent_users"

@dataclass
class PerformanceSample:
    """性能采样数据"""
    timestamp: datetime
    metric: str
    value: float
    unit: str
    tags: Dict[str, str]
    
    def to_dict(self):
        """转换为字典"""
        return {
            "timestamp": self.timestamp.isoformat(),
            "metric": self.metric,
            "value": self.value,
            "unit": self.unit,
            "tags": self.tags
        }

@dataclass
class PerformanceBaseline:
    """性能基线数据"""
    metric: str
    unit: str
    samples: List[float]
    mean: float
    median: float
    p95: float
    p99: float
    std_dev: float
    min_value: float
    max_value: float
    created_at: datetime
    updated_at: datetime
    
    @classmethod
    def from_samples(cls, metric: str, unit: str, samples: List[float]):
        """从采样数据创建基线"""
        if not samples:
            raise ValueError("采样数据不能为空")
        
        samples_sorted = sorted(samples)
        n = len(samples)
        
        return cls(
            metric=metric,
            unit=unit,
            samples=samples,
            mean=statistics.mean(samples),
            median=statistics.median(samples),
            p95=np.percentile(samples, 95) if n >= 5 else samples[-1],
            p99=np.percentile(samples, 99) if n >= 10 else samples[-1],
            std_dev=statistics.stdev(samples) if n >= 2 else 0,
            min_value=min(samples),
            max_value=max(samples),
            created_at=datetime.now(),
            updated_at=datetime.now()
        )
    
    def to_dict(self):
        """转换为字典"""
        return {
            "metric": self.metric,
            "unit": self.unit,
            "statistics": {
                "mean": self.mean,
                "median": self.median,
                "p95": self.p95,
                "p99": self.p99,
                "std_dev": self.std_dev,
                "min": self.min_value,
                "max": self.max_value,
                "sample_count": len(self.samples)
            },
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat()
        }
    
    def is_within_baseline(self, value: float, threshold: float = 2.0) -> Tuple[bool, float]:
        """
        检查值是否在基线范围内
        
        Args:
            value: 要检查的值
            threshold: 标准差倍数阈值（默认2倍标准差）
        
        Returns:
            (是否在基线内, 偏差百分比)
        """
        if self.std_dev == 0:
            deviation_pct = abs((value - self.mean) / self.mean * 100) if self.mean != 0 else 0
            within_baseline = deviation_pct <= 10  # 如果标准差为0，允许10%的偏差
        else:
            z_score = abs(value - self.mean) / self.std_dev
            deviation_pct = (value - self.mean) / self.mean * 100 if self.mean != 0 else 0
            within_baseline = z_score <= threshold
        
        return within_baseline, deviation_pct

class PerformanceBaselineChecker:
    """性能基线检查器"""
    
    def __init__(self, config_file: str = None):
        """
        初始化性能基线检查器
        
        Args:
            config_file: 配置文件路径
        """
        self.start_time = datetime.now()
        self.results = {
            "overall_status": "UNKNOWN",
            "baseline_checks": 0,
            "within_baseline": 0,
            "outside_baseline": 0,
            "warning_checks": 0,
            "check_results": [],
            "performance_summary": {},
            "timestamp": self.start_time.isoformat(),
            "duration_seconds": 0
        }
        
        # 加载配置
        self.config = self.load_config(config_file)
        
        # 加载基线数据
        self.baselines = self.load_baselines()
        
        # 设置检查项
        self.check_items = self.initialize_check_items()
        
        # 性能采样器
        self.samples_collector = PerformanceSamplesCollector(self.config)
        
    def load_config(self, config_file: str = None) -> Dict:
        """加载配置文件"""
        default_config = {
            "environment": {
                "name": "test",
                "type": "test-environment",
                "performance_profile": "balanced"
            },
            "baseline": {
                "collection_duration": 300,  # 采样持续时间（秒）
                "sampling_interval": 5,      # 采样间隔（秒）
                "min_samples": 10,           # 最小采样数
                "z_score_threshold": 2.0,    # Z分数阈值
                "deviation_threshold": 20,   # 偏差百分比阈值
                "auto_update_baseline": False  # 是否自动更新基线
            },
            "checks": {
                "enabled_metrics": [
                    "cpu_usage",
                    "memory_usage",
                    "disk_io_read",
                    "disk_io_write",
                    "network_io_in",
                    "network_io_out",
                    "api_response_time",
                    "api_throughput"
                ],
                "service_endpoints": [
                    {
                        "name": "erp-purchase-api",
                        "url": "http://localhost:8080/api/purchase/health",
                        "method": "GET",
                        "expected_status": 200
                    },
                    {
                        "name": "erp-sales-api",
                        "url": "http://localhost:8081/api/sales/health",
                        "method": "GET",
                        "expected_status": 200
                    }
                ]
            },
            "reporting": {
                "output_formats": ["json", "html", "markdown"],
                "output_dir": "./performance-reports",
                "generate_trend_charts": True,
                "store_samples": True
            },
            "monitoring": {
                "enable_real_time": False,
                "alert_on_deviation": True,
                "alert_threshold": 30  # 偏差超过30%告警
            }
        }
        
        if config_file and os.path.exists(config_file):
            try:
                with open(config_file, 'r', encoding='utf-8') as f:
                    user_config = yaml.safe_load(f)
                    # 合并配置
                    self.merge_configs(default_config, user_config)
                    logger.info(f"已加载配置文件: {config_file}")
            except Exception as e:
                logger.error(f"加载配置文件失败: {e}, 使用默认配置")
        
        return default_config
    
    def load_baselines(self) -> Dict[str, PerformanceBaseline]:
        """加载性能基线数据"""
        baselines = {}
        baseline_file = self.config.get("baseline", {}).get("baseline_file", "performance-baselines.json")
        
        if os.path.exists(baseline_file):
            try:
                with open(baseline_file, 'r', encoding='utf-8') as f:
                    baseline_data = json.load(f)
                
                for metric, data in baseline_data.items():
                    baselines[metric] = PerformanceBaseline(
                        metric=data["metric"],
                        unit=data["unit"],
                        samples=data.get("samples", []),
                        mean=data["statistics"]["mean"],
                        median=data["statistics"]["median"],
                        p95=data["statistics"]["p95"],
                        p99=data["statistics"]["p99"],
                        std_dev=data["statistics"]["std_dev"],
                        min_value=data["statistics"]["min"],
                        max_value=data["statistics"]["max"],
                        created_at=datetime.fromisoformat(data["created_at"]),
                        updated_at=datetime.fromisoformat(data["updated_at"])
                    )
                
                logger.info(f"已加载 {len(baselines)} 个性能基线")
            except Exception as e:
                logger.warning(f"加载基线数据失败: {e}, 将创建新的基线")
        else:
            logger.info("未找到基线数据文件，将创建新的性能基线")
        
        return baselines
    
    def merge_configs(self, default: Dict, user: Dict) -> Dict:
        """递归合并配置"""
        for key, value in user.items():
            if key in default and isinstance(default[key], dict) and isinstance(value, dict):
                self.merge_configs(default[key], value)
            else:
                default[key] = value
        return default
    
    def initialize_check_items(self) -> List[Dict]:
        """初始化检查项列表"""
        check_items = []
        enabled_metrics = self.config["checks"]["enabled_metrics"]
        
        # 基础设施性能检查
        if "cpu_usage" in enabled_metrics:
            check_items.append({
                "id": "perf_cpu_usage",
                "name": "CPU使用率性能检查",
                "category": "infrastructure",
                "metric": "cpu_usage",
                "unit": "percent",
                "description": "检查CPU使用率是否在性能基线范围内",
                "severity": "high",
                "function": self.check_cpu_performance
            })
        
        if "memory_usage" in enabled_metrics:
            check_items.append({
                "id": "perf_memory_usage",
                "name": "内存使用率性能检查",
                "category": "infrastructure",
                "metric": "memory_usage",
                "unit": "percent",
                "description": "检查内存使用率是否在性能基线范围内",
                "severity": "high",
                "function": self.check_memory_performance
            })
        
        if "disk_io_read" in enabled_metrics or "disk_io_write" in enabled_metrics:
            check_items.append({
                "id": "perf_disk_io",
                "name": "磁盘IO性能检查",
                "category": "infrastructure",
                "metric": "disk_io",
                "unit": "MB/s",
                "description": "检查磁盘读写性能是否在基线范围内",
                "severity": "medium",
                "function": self.check_disk_io_performance
            })
        
        if "network_io_in" in enabled_metrics or "network_io_out" in enabled_metrics:
            check_items.append({
                "id": "perf_network_io",
                "name": "网络IO性能检查",
                "category": "infrastructure",
                "metric": "network_io",
                "unit": "MB/s",
                "description": "检查网络吞吐量是否在基线范围内",
                "severity": "medium",
                "function": self.check_network_io_performance
            })
        
        # 应用程序性能检查
        if "api_response_time" in enabled_metrics:
            for endpoint in self.config["checks"]["service_endpoints"]:
                check_items.append({
                    "id": f"perf_api_{endpoint['name']}",
                    "name": f"{endpoint['name']} API响应时间检查",
                    "category": "application",
                    "metric": "api_response_time",
                    "unit": "ms",
                    "description": f"检查{endpoint['name']} API响应时间是否在基线范围内",
                    "severity": "critical",
                    "function": lambda e=endpoint: self.check_api_performance(e),
                    "endpoint_config": endpoint
                })
        
        if "api_throughput" in enabled_metrics:
            check_items.append({
                "id": "perf_api_throughput",
                "name": "API吞吐量性能检查",
                "category": "application",
                "metric": "api_throughput",
                "unit": "requests/sec",
                "description": "检查API吞吐量是否在基线范围内",
                "severity": "high",
                "function": self.check_api_throughput_performance
            })
        
        logger.info(f"初始化了 {len(check_items)} 个性能基线检查项")
        return check_items
    
    def run_all_checks(self) -> Dict:
        """执行所有性能基线检查"""
        logger.info("开始执行性能基线检查...")
        
        # 收集当前性能样本
        logger.info("收集性能样本...")
        current_samples = self.samples_collector.collect_samples()
        
        # 如果没有基线数据，先建立基线
        if not self.baselines:
            logger.info("未找到性能基线，正在建立新的性能基线...")
            self.establish_baselines(current_samples)
        
        # 执行检查
        self.results["baseline_checks"] = len(self.check_items)
        
        for check_item in self.check_items:
            result = self.execute_single_check(check_item, current_samples)
            self.process_check_result(result)
        
        # 计算总体状态
        self.calculate_overall_status()
        
        # 计算执行时间
        end_time = datetime.now()
        self.results["duration_seconds"] = (end_time - self.start_time).total_seconds()
        
        # 生成报告
        self.generate_reports(current_samples)
        
        # 如果需要，更新基线数据
        if self.config["baseline"]["auto_update_baseline"]:
            self.update_baselines(current_samples)
        
        logger.info(f"性能基线检查完成。总体状态: {self.results['overall_status']}")
        logger.info(f"检查结果: {self.results['within_baseline']} 在基线内, "
                   f"{self.results['outside_baseline']} 超出基线, "
                   f"{self.results['warning_checks']} 警告")
        
        return self.results
    
    def establish_baselines(self, samples: List[PerformanceSample]):
        """建立性能基线"""
        logger.info("正在建立性能基线...")
        
        # 按指标分组样本
        samples_by_metric = {}
        for sample in samples:
            if sample.metric not in samples_by_metric:
                samples_by_metric[sample.metric] = []
            samples_by_metric[sample.metric].append(sample.value)
        
        # 为每个指标创建基线
        for metric, values in samples_by_metric.items():
            if len(values) >= self.config["baseline"]["min_samples"]:
                try:
                    baseline = PerformanceBaseline.from_samples(
                        metric=metric,
                        unit=self.get_unit_for_metric(metric),
                        samples=values
                    )
                    self.baselines[metric] = baseline
                    logger.info(f"为指标 {metric} 建立了性能基线: mean={baseline.mean:.2f}, std={baseline.std_dev:.2f}")
                except Exception as e:
                    logger.error(f"为指标 {metric} 建立基线失败: {e}")
        
        # 保存基线数据
        self.save_baselines()
    
    def update_baselines(self, new_samples: List[PerformanceSample]):
        """更新性能基线"""
        logger.info("正在更新性能基线...")
        
        updated = False
        for sample in new_samples:
            metric = sample.metric
            if metric in self.baselines:
                # 将新样本加入基线数据
                baseline = self.baselines[metric]
                baseline.samples.append(sample.value)
                
                # 限制样本数量，保留最近N个样本
                max_samples = 1000
                if len(baseline.samples) > max_samples:
                    baseline.samples = baseline.samples[-max_samples:]
                
                # 重新计算统计信息
                try:
                    baseline = PerformanceBaseline.from_samples(
                        metric=baseline.metric,
                        unit=baseline.unit,
                        samples=baseline.samples
                    )
                    self.baselines[metric] = baseline
                    updated = True
                    logger.info(f"更新了指标 {metric} 的性能基线")
                except Exception as e:
                    logger.error(f"更新指标 {metric} 基线失败: {e}")
        
        if updated:
            self.save_baselines()
    
    def save_baselines(self):
        """保存性能基线数据"""
        baseline_file = self.config.get("baseline", {}).get("baseline_file", "performance-baselines.json")
        
        baseline_data = {}
        for metric, baseline in self.baselines.items():
            baseline_data[metric] = baseline.to_dict()
        
        try:
            with open(baseline_file, 'w', encoding='utf-8') as f:
                json.dump(baseline_data, f, indent=2, ensure_ascii=False)
            logger.info(f"已保存性能基线数据到: {baseline_file}")
        except Exception as e:
            logger.error(f"保存性能基线数据失败: {e}")
    
    def execute_single_check(self, check_item: Dict, current_samples: List[PerformanceSample]) -> Dict:
        """执行单个性能检查项"""
        start_time = time.time()
        check_id = check_item["id"]
        check_name = check_item["name"]
        
        logger.info(f"执行性能基线检查: {check_name}")
        
        try:
            # 执行检查函数
            result = check_item["function"]()
            result["check_id"] = check_id
            result["check_name"] = check_name
            result["category"] = check_item["category"]
            result["severity"] = check_item["severity"]
            
            # 添加当前性能数据
            metric = check_item["metric"]
            current_values = [s.value for s in current_samples if s.metric == metric]
            
            if current_values:
                result["current_value"] = statistics.mean(current_values)
                result["current_samples"] = len(current_values)
            
            # 添加基线数据
            if metric in self.baselines:
                baseline = self.baselines[metric]
                result["baseline"] = baseline.to_dict()
                
                # 检查是否在基线范围内
                if "current_value" in result:
                    within_baseline, deviation_pct = baseline.is_within_baseline(
                        result["current_value"],
                        self.config["baseline"]["z_score_threshold"]
                    )
                    
                    result["within_baseline"] = within_baseline
                    result["deviation_percent"] = deviation_pct
                    
                    if within_baseline:
                        result["status"] = "WITHIN_BASELINE"
                    elif abs(deviation_pct) <= self.config["baseline"]["deviation_threshold"]:
                        result["status"] = "WARNING"
                        result["message"] = f"轻微超出基线: {deviation_pct:.1f}%"
                    else:
                        result["status"] = "OUTSIDE_BASELINE"
                        result["message"] = f"显著超出基线: {deviation_pct:.1f}%"
                else:
                    result["status"] = "NO_DATA"
                    result["message"] = "无法获取当前性能数据"
            else:
                result["status"] = "NO_BASELINE"
                result["message"] = "未找到性能基线数据"
            
            # 添加执行时间
            result["duration"] = round(time.time() - start_time, 3)
            
            return result
            
        except Exception as e:
            logger.error(f"性能检查项 {check_name} 执行失败: {e}")
            return {
                "check_id": check_id,
                "check_name": check_name,
                "category": check_item["category"],
                "severity": check_item["severity"],
                "status": "ERROR",
                "message": f"执行失败: {str(e)}",
                "details": {"error": str(e)},
                "duration": round(time.time() - start_time, 3)
            }
    
    def process_check_result(self, result: Dict):
        """处理检查结果"""
        self.results["check_results"].append(result)
        
        if result["status"] == "WITHIN_BASELINE":
            self.results["within_baseline"] += 1
        elif result["status"] == "OUTSIDE_BASELINE":
            self.results["outside_baseline"] += 1
        elif result["status"] == "WARNING":
            self.results["warning_checks"] += 1
    
    def calculate_overall_status(self):
        """计算总体状态"""
        if self.results["outside_baseline"] > 0:
            self.results["overall_status"] = "PERFORMANCE_DEGRADED"
        elif self.results["warning_checks"] > 0:
            self.results["overall_status"] = "PERFORMANCE_WARNING"
        elif self.results["within_baseline"] == self.results["baseline_checks"]:
            self.results["overall_status"] = "PERFORMANCE_NORMAL"
        else:
            self.results["overall_status"] = "UNKNOWN"
        
        # 生成性能摘要
        self.results["performance_summary"] = {
            "baseline_coverage": self.calculate_baseline_coverage(),
            "performance_trend": self.analyze_performance_trend(),
            "recommendations": self.generate_recommendations()
        }
    
    def calculate_baseline_coverage(self) -> Dict:
        """计算基线覆盖率"""
        total_metrics = len(self.check_items)
        metrics_with_baseline = sum(1 for item in self.check_items 
                                  if item["metric"] in self.baselines)
        
        coverage_rate = (metrics_with_baseline / total_metrics * 100) if total_metrics > 0 else 0
        
        return {
            "total_metrics": total_metrics,
            "metrics_with_baseline": metrics_with_baseline,
            "coverage_rate": round(coverage_rate, 2),
            "status": "GOOD" if coverage_rate >= 80 else "POOR"
        }
    
    def analyze_performance_trend(self) -> Dict:
        """分析性能趋势"""
        # 这里可以实现更复杂的趋势分析
        # 暂时返回简化的分析结果
        
        deviations = []
        for result in self.results["check_results"]:
            if "deviation_percent" in result:
                deviations.append(abs(result["deviation_percent"]))
        
        if deviations:
            avg_deviation = statistics.mean(deviations)
            max_deviation = max(deviations)
            
            if avg_deviation <= 10:
                trend = "STABLE"
            elif avg_deviation <= 20:
                trend = "SLIGHT_DEGRADATION"
            else:
                trend = "SIGNIFICANT_DEGRADATION"
        else:
            avg_deviation = 0
            max_deviation = 0
            trend = "UNKNOWN"
        
        return {
            "average_deviation": round(avg_deviation, 2),
            "maximum_deviation": round(max_deviation, 2),
            "trend": trend,
            "samples_analyzed": len(deviations)
        }
    
    def generate_recommendations(self) -> List[str]:
        """生成性能优化建议"""
        recommendations = []
        
        # 分析检查结果，生成针对性建议
        for result in self.results["check_results"]:
            if result["status"] == "OUTSIDE_BASELINE":
                metric = result.get("metric", "")
                deviation = result.get("deviation_percent", 0)
                
                if "cpu" in metric:
                    recommendations.append(
                        f"CPU使用率超出基线 {deviation:.1f}%，建议检查应用程序CPU使用情况或考虑增加计算资源"
                    )
                elif "memory" in metric:
                    recommendations.append(
                        f"内存使用率超出基线 {deviation:.1f}%，建议优化内存使用或增加内存资源"
                    )
                elif "disk" in metric:
                    recommendations.append(
                        f"磁盘IO性能超出基线 {deviation:.1f}%，建议检查磁盘健康状况或优化IO密集型操作"
                    )
                elif "network" in metric:
                    recommendations.append(
                        f"网络IO性能超出基线 {deviation:.1f}%，建议检查网络带宽或优化网络通信"
                    )
                elif "api" in metric:
                    recommendations.append(
                        f"API性能超出基线 {deviation:.1f}%，建议优化API实现或增加应用服务器资源"
                    )
        
        # 如果没有问题，添加常规建议
        if not recommendations:
            recommendations.append("所有性能指标均在基线范围内，系统性能正常")
            recommendations.append("建议定期运行性能基线检查以监控性能变化")
        
        return recommendations
    
    def get_unit_for_metric(self, metric: str) -> str:
        """根据指标名称获取单位"""
        unit_map = {
            "cpu_usage": "percent",
            "memory_usage": "percent",
            "disk_io_read": "MB/s",
            "disk_io_write": "MB/s",
            "network_io_in": "MB/s",
            "network_io_out": "MB/s",
            "api_response_time": "ms",
            "api_throughput": "requests/sec"
        }
        return unit_map.get(metric, "unknown")
    
    # ========== 具体的性能检查方法 ==========
    
    def check_cpu_performance(self) -> Dict:
        """检查CPU性能"""
        try:
            # 收集一段时间内的CPU使用率
            cpu_samples = []
            duration = 10  # 采样10秒
            interval = 1
            
            for _ in range(duration // interval):
                cpu_percent = psutil.cpu_percent(interval=interval)
                cpu_samples.append(cpu_percent)
            
            avg_cpu = statistics.mean(cpu_samples)
            
            return {
                "metric": "cpu_usage",
                "unit": "percent",
                "current_samples": cpu_samples,
                "message": f"平均CPU使用率: {avg_cpu:.1f}%"
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"CPU性能检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_memory_performance(self) -> Dict:
        """检查内存性能"""
        try:
            memory = psutil.virtual_memory()
            memory_percent = memory.percent
            
            return {
                "metric": "memory_usage",
                "unit": "percent",
                "current_value": memory_percent,
                "message": f"内存使用率: {memory_percent:.1f}%",
                "details": {
                    "total_gb": round(memory.total / (1024**3), 2),
                    "available_gb": round(memory.available / (1024**3), 2),
                    "used_gb": round(memory.used / (1024**3), 2)
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"内存性能检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_disk_io_performance(self) -> Dict:
        """检查磁盘IO性能"""
        try:
            # 获取磁盘IO统计
            disk_io_start = psutil.disk_io_counters()
            time.sleep(2)  # 等待2秒
            disk_io_end = psutil.disk_io_counters()
            
            # 计算读写速度
            read_bytes = disk_io_end.read_bytes - disk_io_start.read_bytes
            write_bytes = disk_io_end.write_bytes - disk_io_start.write_bytes
            duration = 2  # 2秒
            
            read_mbps = read_bytes / duration / (1024**2)
            write_mbps = write_bytes / duration / (1024**2)
            
            return {
                "metric": "disk_io",
                "unit": "MB/s",
                "current_value": read_mbps + write_mbps,
                "message": f"磁盘IO: 读{read_mbps:.1f} MB/s, 写{write_mbps:.1f} MB/s",
                "details": {
                    "read_mbps": read_mbps,
                    "write_mbps": write_mbps,
                    "total_mbps": read_mbps + write_mbps
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"磁盘IO性能检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_network_io_performance(self) -> Dict:
        """检查网络IO性能"""
        try:
            # 获取网络IO统计
            net_io_start = psutil.net_io_counters()
            time.sleep(2)  # 等待2秒
            net_io_end = psutil.net_io_counters()
            
            # 计算网络速度
            bytes_recv = net_io_end.bytes_recv - net_io_start.bytes_recv
            bytes_sent = net_io_end.bytes_sent - net_io_start.bytes_sent
            duration = 2  # 2秒
            
            recv_mbps = bytes_recv / duration / (1024**2) * 8  # 转换为Mbps
            sent_mbps = bytes_sent / duration / (1024**2) * 8
            
            return {
                "metric": "network_io",
                "unit": "Mbps",
                "current_value": recv_mbps + sent_mbps,
                "message": f"网络IO: 接收{recv_mbps:.1f} Mbps, 发送{sent_mbps:.1f} Mbps",
                "details": {
                    "receive_mbps": recv_mbps,
                    "send_mbps": sent_mbps,
                    "total_mbps": recv_mbps + sent_mbps
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"网络IO性能检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_api_performance(self, endpoint_config: Dict) -> Dict:
        """检查API性能"""
        try:
            url = endpoint_config["url"]
            method = endpoint_config.get("method", "GET")
            expected_status = endpoint_config.get("expected_status", 200)
            
            # 执行多次请求以获取准确的响应时间
            response_times = []
            successful_requests = 0
            
            for _ in range(10):  # 执行10次请求
                start_time = time.time()
                try:
                    response = requests.request(
                        method=method,
                        url=url,
                        timeout=10
                    )
                    elapsed = time.time() - start_time
                    
                    if response.status_code == expected_status:
                        response_times.append(elapsed * 1000)  # 转换为毫秒
                        successful_requests += 1
                    else:
                        logger.warning(f"API请求返回非预期状态码: {response.status_code}")
                except Exception as e:
                    logger.warning(f"API请求失败: {e}")
            
            if response_times:
                avg_response_time = statistics.mean(response_times)
                p95_response_time = np.percentile(response_times, 95) if len(response_times) >= 5 else response_times[-1]
                
                return {
                    "metric": "api_response_time",
                    "unit": "ms",
                    "current_value": avg_response_time,
                    "message": f"API平均响应时间: {avg_response_time:.1f}ms (P95: {p95_response_time:.1f}ms)",
                    "details": {
                        "endpoint": url,
                        "method": method,
                        "success_rate": successful_requests / 10 * 100,
                        "response_times": response_times,
                        "average_ms": avg_response_time,
                        "p95_ms": p95_response_time
                    }
                }
            else:
                return {
                    "status": "ERROR",
                    "message": "所有API请求都失败",
                    "details": {"endpoint": url, "method": method}
                }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"API性能检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def check_api_throughput_performance(self) -> Dict:
        """检查API吞吐量性能"""
        try:
            # 这里实现吞吐量测试
            # 暂时返回模拟数据
            throughput = 150  # 模拟150请求/秒
            
            return {
                "metric": "api_throughput",
                "unit": "requests/sec",
                "current_value": throughput,
                "message": f"API吞吐量: {throughput} 请求/秒",
                "details": {
                    "note": "这是模拟数据，实际实现需要执行压力测试",
                    "simulated": True
                }
            }
            
        except Exception as e:
            return {
                "status": "ERROR",
                "message": f"API吞吐量检查失败: {str(e)}",
                "details": {"error": str(e)}
            }
    
    def generate_reports(self, current_samples: List[PerformanceSample]):
        """生成性能报告"""
        output_dir = self.config["reporting"]["output_dir"]
        os.makedirs(output_dir, exist_ok=True)
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        base_filename = f"performance-baseline-report-{timestamp}"
        
        # JSON报告
        if "json" in self.config["reporting"]["output_formats"]:
            json_path = os.path.join(output_dir, f"{base_filename}.json")
            with open(json_path, 'w', encoding='utf-8') as f:
                # 包含采样数据
                full_report = self.results.copy()
                if self.config["reporting"]["store_samples"]:
                    full_report["current_samples"] = [s.to_dict() for s in current_samples]
                json.dump(full_report, f, indent=2, ensure_ascii=False)
            logger.info(f"生成JSON性能报告: {json_path}")
        
        # Markdown报告
        if "markdown" in self.config["reporting"]["output_formats"]:
            markdown_path = os.path.join(output_dir, f"{base_filename}.md")
            self.generate_markdown_report(markdown_path, current_samples)
            logger.info(f"生成Markdown性能报告: {markdown_path}")
        
        # 保存采样数据
        if self.config["reporting"]["store_samples"]:
            samples_file = os.path.join(output_dir, f"performance-samples-{timestamp}.json")
            samples_data = [s.to_dict() for s in current_samples]
            with open(samples_file, 'w', encoding='utf-8') as f:
                json.dump(samples_data, f, indent=2, ensure_ascii=False)
            logger.info(f"保存性能采样