#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
性能样本收集器
版本: 1.0.0
创建日期: 2026-04-29
作者: qa-lead
功能: 收集系统性能样本数据，用于建立性能基线
"""

import time
import threading
from datetime import datetime
from typing import List, Dict, Any
import psutil
import statistics

class PerformanceSamplesCollector:
    """性能样本收集器"""
    
    def __init__(self, config: Dict):
        """
        初始化性能样本收集器
        
        Args:
            config: 配置字典
        """
        self.config = config
        self.samples = []
        self.is_collecting = False
        self.collection_thread = None
        
        # 从配置获取参数
        baseline_config = config.get("baseline", {})
        self.collection_duration = baseline_config.get("collection_duration", 300)  # 秒
        self.sampling_interval = baseline_config.get("sampling_interval", 5)  # 秒
        self.min_samples = baseline_config.get("min_samples", 10)
        
    def collect_samples(self) -> List['PerformanceSample']:
        """
        收集性能样本
        
        Returns:
            性能样本列表
        """
        logger.info(f"开始收集性能样本，持续时间: {self.collection_duration}秒，采样间隔: {self.sampling_interval}秒")
        
        self.samples = []
        start_time = time.time()
        sample_count = 0
        
        while time.time() - start_time < self.collection_duration:
            try:
                # 收集各种性能指标
                self.collect_cpu_samples()
                self.collect_memory_samples()
                self.collect_disk_samples()
                self.collect_network_samples()
                
                sample_count += 1
                logger.debug(f"已收集 {sample_count} 组性能样本")
                
                # 等待下一个采样间隔
                time.sleep(self.sampling_interval)
                
            except Exception as e:
                logger.error(f"收集性能样本时出错: {e}")
                time.sleep(self.sampling_interval)  # 出错后继续
        
        logger.info(f"性能样本收集完成，共收集 {len(self.samples)} 个样本")
        return self.samples
    
    def collect_cpu_samples(self):
        """收集CPU使用率样本"""
        try:
            # 获取CPU使用率（百分比）
            cpu_percent = psutil.cpu_percent(interval=1)
            
            self.samples.append(PerformanceSample(
                timestamp=datetime.now(),
                metric="cpu_usage",
                value=cpu_percent,
                unit="percent",
                tags={"type": "system", "source": "psutil"}
            ))
            
            # 获取每个CPU核心的使用率
            cpu_percent_per_core = psutil.cpu_percent(interval=1, percpu=True)
            for i, core_percent in enumerate(cpu_percent_per_core):
                self.samples.append(PerformanceSample(
                    timestamp=datetime.now(),
                    metric=f"cpu_core_{i}_usage",
                    value=core_percent,
                    unit="percent",
                    tags={"type": "core", "core_id": str(i), "source": "psutil"}
                ))
                
        except Exception as e:
            logger.error(f"收集CPU样本失败: {e}")
    
    def collect_memory_samples(self):
        """收集内存使用样本"""
        try:
            memory = psutil.virtual_memory()
            
            # 内存使用率
            self.samples.append(PerformanceSample(
                timestamp=datetime.now(),
                metric="memory_usage",
                value=memory.percent,
                unit="percent",
                tags={"type": "system", "source": "psutil"}
            ))
            
            # 内存使用量（GB）
            memory_used_gb = memory.used / (1024**3)
            self.samples.append(PerformanceSample(
                timestamp=datetime.now(),
                metric="memory_used",
                value=memory_used_gb,
                unit="GB",
                tags={"type": "system", "source": "psutil"}
            ))
            
            # 可用内存（GB）
            memory_available_gb = memory.available / (1024**3)
            self.samples.append(PerformanceSample(
                timestamp=datetime.now(),
                metric="memory_available",
                value=memory_available_gb,
                unit="GB",
                tags={"type": "system", "source": "psutil"}
            ))
            
        except Exception as e:
            logger.error(f"收集内存样本失败: {e}")
    
    def collect_disk_samples(self):
        """收集磁盘IO样本"""
        try:
            # 获取磁盘IO统计
            disk_io_counters = psutil.disk_io_counters()
            if disk_io_counters:
                # 读取速度（MB/s）
                read_mbps = disk_io_counters.read_bytes / (1024**2)
                self.samples.append(PerformanceSample(
                    timestamp=datetime.now(),
                    metric="disk_read_mbps",
                    value=read_mbps,
                    unit="MB/s",
                    tags={"type": "io", "operation": "read", "source": "psutil"}
                ))
                
                # 写入速度（MB/s）
                write_mbps = disk_io_counters.write_bytes / (1024**2)
                self.samples.append(PerformanceSample(
                    timestamp=datetime.now(),
                    metric="disk_write_mbps",
                    value=write_mbps,
                    unit="MB/s",
                    tags={"type": "io", "operation": "write", "source": "psutil"}
                ))
                
        except Exception as e:
            logger.error(f"收集磁盘样本失败: {e}")
    
    def collect_network_samples(self):
        """收集网络IO样本"""
        try:
            # 获取网络IO统计
            net_io_counters = psutil.net_io_counters()
            if net_io_counters:
                # 接收速度（Mbps）
                bytes_recv_mbps = net_io_counters.bytes_recv / (1024**2) * 8
                self.samples.append(PerformanceSample(
                    timestamp=datetime.now(),
                    metric="network_receive_mbps",
                    value=bytes_recv_mbps,
                    unit="Mbps",
                    tags={"type": "network", "direction": "receive", "source": "psutil"}
                ))
                
                # 发送速度（Mbps）
                bytes_sent_mbps = net_io_counters.bytes_sent / (1024**2) * 8
                self.samples.append(PerformanceSample(
                    timestamp=datetime.now(),
                    metric="network_send_mbps",
                    value=bytes_sent_mbps,
                    unit="Mbps",
                    tags={"type": "network", "direction": "send", "source": "psutil"}
                ))
                
        except Exception as e:
            logger.error(f"收集网络样本失败: {e}")
    
    def start_continuous_collection(self):
        """开始持续收集性能样本"""
        if self.is_collecting:
            logger.warning("性能样本收集已在运行中")
            return
        
        self.is_collecting = True
        self.collection_thread = threading.Thread(target=self._continuous_collection_loop)
        self.collection_thread.daemon = True
        self.collection_thread.start()
        logger.info("已启动持续性能样本收集")
    
    def stop_continuous_collection(self):
        """停止持续收集性能样本"""
        self.is_collecting = False
        if self.collection_thread:
            self.collection_thread.join(timeout=10)
        logger.info("已停止持续性能样本收集")
    
    def _continuous_collection_loop(self):
        """持续收集循环"""
        while self.is_collecting:
            try:
                self.collect_samples()
                time.sleep(self.sampling_interval)
            except Exception as e:
                logger.error(f"持续收集性能样本时出错: {e}")
                time.sleep(self.sampling_interval)
    
    def get_recent_samples(self, metric: str, minutes: int = 5) -> List['PerformanceSample']:
        """
        获取最近一段时间的特定指标样本
        
        Args:
            metric: 指标名称
            minutes: 时间范围（分钟）
        
        Returns:
            符合条件的样本列表
        """
        cutoff_time = datetime.now().timestamp() - (minutes * 60)
        
        recent_samples = [
            sample for sample in self.samples
            if sample.metric == metric and sample.timestamp.timestamp() > cutoff_time
        ]
        
        return recent_samples
    
    def get_statistics(self, metric: str, minutes: int = 5) -> Dict[str, Any]:
        """
        获取特定指标的统计信息
        
        Args:
            metric: 指标名称
            minutes: 时间范围（分钟）
        
        Returns:
            统计信息字典
        """
        recent_samples = self.get_recent_samples(metric, minutes)
        
        if not recent_samples:
            return {
                "metric": metric,
                "sample_count": 0,
                "message": "没有可用的样本数据"
            }
        
        values = [sample.value for sample in recent_samples]
        
        return {
            "metric": metric,
            "sample_count": len(values),
            "mean": statistics.mean(values),
            "median": statistics.median(values),
            "min": min(values),
            "max": max(values),
            "std_dev": statistics.stdev(values) if len(values) >= 2 else 0,
            "latest_value": values[-1],
            "time_range_minutes": minutes,
            "first_sample_time": recent_samples[0].timestamp.isoformat(),
            "last_sample_time": recent_samples[-1].timestamp.isoformat()
        }

# 性能样本数据类
class PerformanceSample:
    """性能采样数据"""
    
    def __init__(self, timestamp, metric, value, unit, tags=None):
        self.timestamp = timestamp
        self.metric = metric
        self.value = value
        self.unit = unit
        self.tags = tags or {}
    
    def to_dict(self):
        """转换为字典"""
        return {
            "timestamp": self.timestamp.isoformat(),
            "metric": self.metric,
            "value": self.value,
            "unit": self.unit,
            "tags": self.tags
        }
    
    @classmethod
    def from_dict(cls, data):
        """从字典创建实例"""
        return cls(
            timestamp=datetime.fromisoformat(data["timestamp"]),
            metric=data["metric"],
            value=data["value"],
            unit=data["unit"],
            tags=data.get("tags", {})
        )

# 日志配置
import logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

if __name__ == "__main__":
    # 测试代码
    config = {
        "baseline": {
            "collection_duration": 30,  # 30秒测试
            "sampling_interval": 5,
            "min_samples": 5
        }
    }
    
    collector = PerformanceSamplesCollector(config)
    samples = collector.collect_samples()
    
    print(f"收集到 {len(samples)} 个性能样本")
    
    # 显示前几个样本
    for i, sample in enumerate(samples[:5]):
        print(f"样本 {i+1}: {sample.metric} = {sample.value} {sample.unit}")