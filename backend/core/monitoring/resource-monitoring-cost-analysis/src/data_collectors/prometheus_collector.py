"""
Prometheus数据采集器
从Prometheus收集监控指标数据
"""

import asyncio
import logging
from typing import Dict, List, Optional, Any, Union
from datetime import datetime, timedelta
import httpx
import pandas as pd
import numpy as np
from dataclasses import dataclass
from enum import Enum

logger = logging.getLogger(__name__)


class MetricType(Enum):
    """指标类型枚举"""
    GAUGE = "gauge"
    COUNTER = "counter"
    HISTOGRAM = "histogram"
    SUMMARY = "summary"


@dataclass
class MetricQuery:
    """指标查询定义"""
    name: str
    query: str
    metric_type: MetricType
    description: str
    labels: List[str] = None
    aggregation: str = "avg"
    step: str = "1m"
    timeout: int = 30


class PrometheusCollector:
    """Prometheus数据采集器"""
    
    def __init__(
        self,
        prometheus_url: str = "http://localhost:9090",
        timeout: int = 30,
        max_retries: int = 3
    ):
        """
        初始化Prometheus采集器
        
        Args:
            prometheus_url: Prometheus服务器URL
            timeout: 请求超时时间（秒）
            max_retries: 最大重试次数
        """
        self.prometheus_url = prometheus_url.rstrip('/')
        self.timeout = timeout
        self.max_retries = max_retries
        self.client = None
        self._queries = self._get_default_queries()
        
    def _get_default_queries(self) -> Dict[str, MetricQuery]:
        """获取默认的指标查询"""
        return {
            # 基础设施指标
            "node_cpu_usage": MetricQuery(
                name="node_cpu_usage",
                query='100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)',
                metric_type=MetricType.GAUGE,
                description="节点CPU使用率 (%)",
                labels=["instance", "job"]
            ),
            "node_memory_usage": MetricQuery(
                name="node_memory_usage",
                query='(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100',
                metric_type=MetricType.GAUGE,
                description="节点内存使用率 (%)",
                labels=["instance", "job"]
            ),
            "node_disk_usage": MetricQuery(
                name="node_disk_usage",
                query='(node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes * 100',
                metric_type=MetricType.GAUGE,
                description="节点磁盘使用率 (%)",
                labels=["instance", "job", "mountpoint", "fstype"]
            ),
            "node_network_receive": MetricQuery(
                name="node_network_receive",
                query='rate(node_network_receive_bytes_total[5m])',
                metric_type=MetricType.GAUGE,
                description="节点网络接收速率 (bytes/s)",
                labels=["instance", "job", "device"]
            ),
            "node_network_transmit": MetricQuery(
                name="node_network_transmit",
                query='rate(node_network_transmit_bytes_total[5m])',
                metric_type=MetricType.GAUGE,
                description="节点网络发送速率 (bytes/s)",
                labels=["instance", "job", "device"]
            ),
            
            # 容器指标
            "container_cpu_usage": MetricQuery(
                name="container_cpu_usage",
                query='rate(container_cpu_usage_seconds_total[5m]) * 100',
                metric_type=MetricType.GAUGE,
                description="容器CPU使用率 (%)",
                labels=["container", "pod", "namespace", "instance"]
            ),
            "container_memory_usage": MetricQuery(
                name="container_memory_usage",
                query='container_memory_working_set_bytes',
                metric_type=MetricType.GAUGE,
                description="容器内存使用量 (bytes)",
                labels=["container", "pod", "namespace", "instance"]
            ),
            "container_memory_limit": MetricQuery(
                name="container_memory_limit",
                query='container_spec_memory_limit_bytes',
                metric_type=MetricType.GAUGE,
                description="容器内存限制 (bytes)",
                labels=["container", "pod", "namespace", "instance"]
            ),
            
            # 应用指标
            "http_requests_total": MetricQuery(
                name="http_requests_total",
                query='rate(http_requests_total[5m])',
                metric_type=MetricType.COUNTER,
                description="HTTP请求速率 (requests/s)",
                labels=["method", "handler", "code", "instance"]
            ),
            "http_request_duration_seconds": MetricQuery(
                name="http_request_duration_seconds",
                query='rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m])',
                metric_type=MetricType.HISTOGRAM,
                description="HTTP请求平均持续时间 (seconds)",
                labels=["method", "handler", "instance"]
            ),
            "jvm_memory_used": MetricQuery(
                name="jvm_memory_used",
                query='jvm_memory_used_bytes',
                metric_type=MetricType.GAUGE,
                description="JVM内存使用量 (bytes)",
                labels=["area", "instance"]
            ),
            
            # 数据库指标
            "postgres_connections": MetricQuery(
                name="postgres_connections",
                query='pg_stat_database_numbackends',
                metric_type=MetricType.GAUGE,
                description="PostgreSQL连接数",
                labels=["datname", "instance"]
            ),
            "mysql_connections": MetricQuery(
                name="mysql_connections",
                query='mysql_global_status_threads_connected',
                metric_type=MetricType.GAUGE,
                description="MySQL连接数",
                labels=["instance"]
            ),
            "redis_memory_used": MetricQuery(
                name="redis_memory_used",
                query='redis_memory_used_bytes',
                metric_type=MetricType.GAUGE,
                description="Redis内存使用量 (bytes)",
                labels=["instance"]
            ),
            
            # 消息队列指标
            "rabbitmq_queue_messages": MetricQuery(
                name="rabbitmq_queue_messages",
                query='rabbitmq_queue_messages',
                metric_type=MetricType.GAUGE,
                description="RabbitMQ队列消息数",
                labels=["queue", "vhost", "instance"]
            ),
            "kafka_topic_messages": MetricQuery(
                name="kafka_topic_messages",
                query='kafka_topic_partition_current_offset - kafka_consumer_group_current_offset',
                metric_type=MetricType.GAUGE,
                description="Kafka主题未消费消息数",
                labels=["topic", "partition", "consumer_group", "instance"]
            )
        }
    
    async def _get_client(self) -> httpx.AsyncClient:
        """获取HTTP客户端"""
        if self.client is None:
            self.client = httpx.AsyncClient(
                timeout=self.timeout,
                limits=httpx.Limits(max_connections=100, max_keepalive_connections=20)
            )
        return self.client
    
    async def query_prometheus(
        self,
        query: str,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None,
        step: str = "1m"
    ) -> Dict[str, Any]:
        """
        查询Prometheus
        
        Args:
            query: PromQL查询语句
            start_time: 开始时间
            end_time: 结束时间
            step: 查询步长
            
        Returns:
            查询结果
        """
        client = await self._get_client()
        
        params = {
            "query": query
        }
        
        if start_time and end_time:
            params.update({
                "start": start_time.isoformat(),
                "end": end_time.isoformat(),
                "step": step
            })
        
        for retry in range(self.max_retries):
            try:
                response = await client.get(
                    f"{self.prometheus_url}/api/v1/query",
                    params=params
                )
                response.raise_for_status()
                return response.json()
            except httpx.RequestError as e:
                if retry == self.max_retries - 1:
                    logger.error(f"查询Prometheus失败: {e}")
                    raise
                await asyncio.sleep(2 ** retry)  # 指数退避
            except httpx.HTTPStatusError as e:
                logger.error(f"Prometheus返回错误: {e.response.status_code} - {e.response.text}")
                raise
    
    async def query_range_prometheus(
        self,
        query: str,
        start_time: datetime,
        end_time: datetime,
        step: str = "1m"
    ) -> Dict[str, Any]:
        """
        查询Prometheus时间范围数据
        
        Args:
            query: PromQL查询语句
            start_time: 开始时间
            end_time: 结束时间
            step: 查询步长
            
        Returns:
            查询结果
        """
        client = await self._get_client()
        
        params = {
            "query": query,
            "start": start_time.isoformat(),
            "end": end_time.isoformat(),
            "step": step
        }
        
        for retry in range(self.max_retries):
            try:
                response = await client.get(
                    f"{self.prometheus_url}/api/v1/query_range",
                    params=params
                )
                response.raise_for_status()
                return response.json()
            except httpx.RequestError as e:
                if retry == self.max_retries - 1:
                    logger.error(f"查询Prometheus范围数据失败: {e}")
                    raise
                await asyncio.sleep(2 ** retry)
            except httpx.HTTPStatusError as e:
                logger.error(f"Prometheus返回错误: {e.response.status_code} - {e.response.text}")
                raise
    
    async def collect_metric(
        self,
        metric_name: str,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None,
        step: str = "1m"
    ) -> pd.DataFrame:
        """
        收集特定指标数据
        
        Args:
            metric_name: 指标名称
            start_time: 开始时间
            end_time: 结束时间
            step: 查询步长
            
        Returns:
            指标数据DataFrame
        """
        if metric_name not in self._queries:
            raise ValueError(f"未知的指标: {metric_name}")
        
        metric_query = self._queries[metric_name]
        
        if start_time and end_time:
            result = await self.query_range_prometheus(
                metric_query.query,
                start_time,
                end_time,
                step
            )
        else:
            result = await self.query_prometheus(metric_query.query)
        
        return self._parse_prometheus_result(result, metric_query)
    
    def _parse_prometheus_result(
        self,
        result: Dict[str, Any],
        metric_query: MetricQuery
    ) -> pd.DataFrame:
        """
        解析Prometheus查询结果
        
        Args:
            result: Prometheus查询结果
            metric_query: 指标查询定义
            
        Returns:
            解析后的DataFrame
        """
        if result["status"] != "success":
            raise ValueError(f"Prometheus查询失败: {result.get('error', 'Unknown error')}")
        
        data = result["data"]
        
        if "result" not in data:
            return pd.DataFrame()
        
        rows = []
        
        for item in data["result"]:
            metric = item["metric"]
            values = item.get("values", [])
            
            # 单点查询
            if not values:
                value = item.get("value")
                if value:
                    timestamp, val = value
                    row = {
                        "timestamp": datetime.fromtimestamp(timestamp),
                        "value": float(val) if val != "NaN" else np.nan,
                        "metric_name": metric_query.name
                    }
                    # 添加标签
                    for label in metric_query.labels or []:
                        row[label] = metric.get(label, "")
                    rows.append(row)
            
            # 范围查询
            else:
                for timestamp, val in values:
                    row = {
                        "timestamp": datetime.fromtimestamp(timestamp),
                        "value": float(val) if val != "NaN" else np.nan,
                        "metric_name": metric_query.name
                    }
                    # 添加标签
                    for label in metric_query.labels or []:
                        row[label] = metric.get(label, "")
                    rows.append(row)
        
        df = pd.DataFrame(rows)
        
        if not df.empty:
            df = df.sort_values("timestamp")
            df = df.reset_index(drop=True)
        
        return df
    
    async def collect_all_metrics(
        self,
        start_time: Optional[datetime] = None,
        end_time: Optional[datetime] = None,
        step: str = "1m",
        metric_names: Optional[List[str]] = None
    ) -> Dict[str, pd.DataFrame]:
        """
        收集所有监控指标
        
        Args:
            start_time: 开始时间
            end_time: 结束时间
            step: 查询步长
            metric_names: 要收集的指标名称列表，None表示收集所有
            
        Returns:
            指标数据字典
        """
        metrics_to_collect = metric_names or list(self._queries.keys())
        
        tasks = []
        for metric_name in metrics_to_collect:
            if metric_name in self._queries:
                task = self.collect_metric(metric_name, start_time, end_time, step)
                tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        collected_metrics = {}
        for metric_name, result in zip(metrics_to_collect, results):
            if isinstance(result, Exception):
                logger.error(f"收集指标 {metric_name} 失败: {result}")
                continue
            collected_metrics[metric_name] = result
        
        return collected_metrics
    
    async def get_metric_names(self) -> List[str]:
        """获取所有可用的指标名称"""
        client = await self._get_client()
        
        try:
            response = await client.get(f"{self.prometheus_url}/api/v1/label/__name__/values")
            response.raise_for_status()
            result = response.json()
            
            if result["status"] == "success":
                return result["data"]
            else:
                return []
        except Exception as e:
            logger.error(f"获取指标名称失败: {e}")
            return list(self._queries.keys())
    
    async def get_targets(self) -> List[Dict[str, Any]]:
        """获取监控目标状态"""
        client = await self._get_client()
        
        try:
            response = await client.get(f"{self.prometheus_url}/api/v1/targets")
            response.raise_for_status()
            result = response.json()
            
            if result["status"] == "success":
                return result["data"]["activeTargets"]
            else:
                return []
        except Exception as e:
            logger.error(f"获取监控目标失败: {e}")
            return []
    
    async def get_alerts(self) -> List[Dict[str, Any]]:
        """获取当前告警"""
        client = await self._get_client()
        
        try:
            response = await client.get(f"{self.prometheus_url}/api/v1/alerts")
            response.raise_for_status()
            result = response.json()
            
            if result["status"] == "success":
                return result["data"]["alerts"]
            else:
                return []
        except Exception as e:
            logger.error(f"获取告警失败: {e}")
            return []
    
    async def close(self):
        """关闭HTTP客户端"""
        if self.client:
            await self.client.aclose()
            self.client = None
    
    async def __aenter__(self):
        """异步上下文管理器入口"""
        await self._get_client()
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """异步上下文管理器出口"""
        await self.close()


# 使用示例
async def example_usage():
    """使用示例"""
    collector = PrometheusCollector(prometheus_url="http://localhost:9090")
    
    try:
        # 收集单个指标
        cpu_df = await collector.collect_metric("node_cpu_usage")
        print(f"CPU使用率数据: {len(cpu_df)} 行")
        
        # 收集多个指标
        end_time = datetime.now()
        start_time = end_time - timedelta(hours=1)
        
        metrics = await collector.collect_all_metrics(
            start_time=start_time,
            end_time=end_time,
            step="5m"
        )
        
        for metric_name, df in metrics.items():
            if not df.empty:
                print(f"{metric_name}: {len(df)} 行数据")
        
        # 获取监控目标
        targets = await collector.get_targets()
        print(f"监控目标数: {len(targets)}")
        
        # 获取告警
        alerts = await collector.get_alerts()
        print(f"当前告警数: {len(alerts)}")
        
    finally:
        await collector.close()


if __name__ == "__main__":
    import asyncio
    asyncio.run(example_usage())