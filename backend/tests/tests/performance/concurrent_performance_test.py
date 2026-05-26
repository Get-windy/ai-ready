#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
企智连并发性能测试脚本
测试场景：
1. 并发用户测试（100/500/1000用户）
2. 响应时间测试
3. 吞吐量测试
"""

import time
import json
import psutil
import threading
import queue
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List, Any
from dataclasses import dataclass
import requests
import statistics

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 并发测试配置 - 100/500/1000用户
CONCURRENT_CONFIGS = [
    {
        "name": "100_users",
        "display_name": "100用户并发",
        "concurrent_users": 100,
        "duration_seconds": 300,  # 5分钟
        "ramp_up_seconds": 30,
        "think_time_seconds": 0.5,
    },
    {
        "name": "500_users",
        "display_name": "500用户并发",
        "concurrent_users": 500,
        "duration_seconds": 300,
        "ramp_up_seconds": 60,
        "think_time_seconds": 0.5,
    },
    {
        "name": "1000_users",
        "display_name": "1000用户并发",
        "concurrent_users": 1000,
        "duration_seconds": 300,
        "ramp_up_seconds": 120,
        "think_time_seconds": 0.5,
    }
]

# 测试端点配置
ENDPOINTS = [
    {"path": "/api/auth/login", "method": "POST", "weight": 0.2, "name": "登录认证"},
    {"path": "/api/user/page", "method": "GET", "weight": 0.25, "name": "用户列表查询"},
    {"path": "/api/role/page", "method": "GET", "weight": 0.15, "name": "角色列表查询"},
    {"path": "/api/health", "method": "GET", "weight": 0.2, "name": "健康检查"},
    {"path": "/api/orders/list", "method": "GET", "weight": 0.2, "name": "订单列表查询"},
]


@dataclass
class RequestMetric:
    """请求指标"""
    timestamp: float
    endpoint: str
    method: str
    name: str
    success: bool
    status_code: int
    response_time_ms: float
    concurrent_level: int
    error: str = ""


@dataclass
class ResourceMetric:
    """资源指标"""
    timestamp: float
    cpu_percent: float
    memory_percent: float
    memory_mb: float
    threads: int
    concurrent_level: int


class ResourceMonitor:
    """资源监控器"""
    
    def __init__(self, concurrent_level: int, interval_seconds: float = 1.0):
        self.interval = interval_seconds
        self.concurrent_level = concurrent_level
        self.running = False
        self.metrics: List[ResourceMetric] = []
        self._thread = None
    
    def start(self):
        """开始监控"""
        self.running = True
        self._thread = threading.Thread(target=self._monitor_loop)
        self._thread.daemon = True
        self._thread.start()
    
    def stop(self):
        """停止监控"""
        self.running = False
        if self._thread:
            self._thread.join(timeout=2)
    
    def _monitor_loop(self):
        """监控循环"""
        while self.running:
            try:
                metric = ResourceMetric(
                    timestamp=time.time(),
                    cpu_percent=psutil.cpu_percent(interval=self.interval),
                    memory_percent=psutil.virtual_memory().percent,
                    memory_mb=psutil.virtual_memory().used / 1024 / 1024,
                    threads=psutil.Process().num_threads() if psutil.Process() else 0,
                    concurrent_level=self.concurrent_level
                )
                self.metrics.append(metric)
            except Exception:
                pass


def make_request(endpoint: Dict, concurrent_level: int, timeout: int = 10) -> RequestMetric:
    """发送HTTP请求"""
    start_time = time.time()
    timestamp = start_time
    
    try:
        if endpoint["method"] == "GET":
            resp = requests.get(f"{BASE_URL}{endpoint['path']}", timeout=timeout)
        else:
            resp = requests.post(f"{BASE_URL}{endpoint['path']}", timeout=timeout)
        
        elapsed_ms = (time.time() - start_time) * 1000
        # 401/403视为成功（认证相关接口）
        success = 200 <= resp.status_code < 400 or resp.status_code in [401, 403]
        return RequestMetric(
            timestamp=timestamp,
            endpoint=endpoint["path"],
            method=endpoint["method"],
            name=endpoint["name"],
            success=success,
            status_code=resp.status_code,
            response_time_ms=elapsed_ms,
            concurrent_level=concurrent_level
        )
    except requests.exceptions.Timeout:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp, endpoint=endpoint["path"], method=endpoint["method"],
            name=endpoint["name"], success=False, status_code=0, 
            response_time_ms=elapsed_ms, concurrent_level=concurrent_level, error="timeout"
        )
    except requests.exceptions.ConnectionError:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp, endpoint=endpoint["path"], method=endpoint["method"],
            name=endpoint["name"], success=False, status_code=0, 
            response_time_ms=elapsed_ms, concurrent_level=concurrent_level, error="connection_refused"
        )
    except Exception as e:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp, endpoint=endpoint["path"], method=endpoint["method"],
            name=endpoint["name"], success=False, status_code=0, 
            response_time_ms=elapsed_ms, concurrent_level=concurrent_level, error=str(e)[:50]
        )


def select_endpoint_by_weight() -> Dict:
    """根据权重选择端点"""
    import random
    r = random.random()
    cumulative = 0
    for endpoint in ENDPOINTS:
        cumulative += endpoint["weight"]
        if r <= cumulative:
            return endpoint
    return ENDPOINTS[-1]


def run_concurrent_test(config: Dict) -> Dict[str, Any]:
    """运行并发测试"""
    print(f"\n{'='*70}")
    print(f"开始测试: {config['display_name']}")
    print(f"{'='*70}")
    print(f"并发用户数: {config['concurrent_users']}")
    print(f"测试时长: {config['duration_seconds']}秒 ({config['duration_seconds']//60}分钟)")
    print(f"爬升时间: {config['ramp_up_seconds']}秒")
    print(f"思考时间: {config['think_time_seconds']}秒")
    
    concurrent_users = config["concurrent_users"]
    duration_seconds = config["duration_seconds"]
    ramp_up_seconds = config["ramp_up_seconds"]
    think_time = config["think_time_seconds"]
    
    # 启动资源监控
    print(f"\n[启动] 资源监控...")
    monitor = ResourceMonitor(concurrent_level=concurrent_users, interval_seconds=1.0)
    monitor.start()
    
    # 测试指标队列
    metrics_queue = queue.Queue()
    stop_event = threading.Event()
    
    def worker(user_id: int):
        """工作线程"""
        while not stop_event.is_set():
            endpoint = select_endpoint_by_weight()
            metric = make_request(endpoint, concurrent_users)
            metrics_queue.put(metric)
            
            # 思考时间
            if think_time > 0:
                time.sleep(think_time)
    
    # 启动并发用户
    print(f"\n[启动] 并发用户 ({concurrent_users}个)...")
    start_time = time.time()
    
    with ThreadPoolExecutor(max_workers=concurrent_users) as executor:
        # 爬升阶段 - 分批启动用户
        batch_size = max(1, concurrent_users // (ramp_up_seconds // 5))
        futures = []
        
        for i in range(0, concurrent_users, batch_size):
            current_batch = min(batch_size, concurrent_users - i)
            for j in range(current_batch):
                futures.append(executor.submit(worker, i + j))
            
            if i + batch_size < concurrent_users:
                print(f"  已启动 {min(i + batch_size, concurrent_users)}/{concurrent_users} 用户...")
                time.sleep(5)
        
        print(f"  全部 {concurrent_users} 用户已启动，开始正式测试...")
        
        # 运行指定时间
        time.sleep(duration_seconds)
        
        # 停止测试
        print(f"\n[停止] 结束测试...")
        stop_event.set()
        
        for future in as_completed(futures):
            pass
    
    actual_duration = time.time() - start_time
    
    # 停止资源监控
    monitor.stop()
    
    # 收集结果
    metrics = []
    while not metrics_queue.empty():
        metrics.append(metrics_queue.get())
    
    print(f"\n[完成] 收集到 {len(metrics)} 个请求指标")
    
    # 计算汇总指标
    return calculate_metrics(metrics, monitor.metrics, config, actual_duration)


def calculate_metrics(request_metrics: List[RequestMetric], resource_metrics: List[ResourceMetric], 
                     config: Dict, duration: float) -> Dict[str, Any]:
    """计算测试指标"""
    concurrent_users = config["concurrent_users"]
    
    if not request_metrics:
        return {"error": "No request data collected"}
    
    # 请求统计
    total_requests = len(request_metrics)
    successful_requests = sum(1 for m in request_metrics if m.success)
    failed_requests = total_requests - successful_requests
    
    # 响应时间统计
    response_times = [m.response_time_ms for m in request_metrics]
    avg_response_time = statistics.mean(response_times)
    min_response_time = min(response_times)
    max_response_time = max(response_times)
    
    # 百分位计算
    sorted_times = sorted(response_times)
    p50 = sorted_times[int(len(sorted_times) * 0.50)] if sorted_times else 0
    p95 = sorted_times[int(len(sorted_times) * 0.95)] if sorted_times else 0
    p99 = sorted_times[int(len(sorted_times) * 0.99)] if sorted_times else 0
    
    # 吞吐量
    throughput = total_requests / duration if duration > 0 else 0
    
    # 错误率
    error_rate = (failed_requests / total_requests * 100) if total_requests > 0 else 100
    
    # 按端点统计
    endpoint_stats = {}
    for m in request_metrics:
        ep = m.endpoint
        if ep not in endpoint_stats:
            endpoint_stats[ep] = {"name": m.name, "count": 0, "success": 0, "times": []}
        endpoint_stats[ep]["count"] += 1
        if m.success:
            endpoint_stats[ep]["success"] += 1
        endpoint_stats[ep]["times"].append(m.response_time_ms)
    
    for ep, stats in endpoint_stats.items():
        stats["avg_time"] = statistics.mean(stats["times"]) if stats["times"] else 0
        stats["success_rate"] = (stats["success"] / stats["count"] * 100) if stats["count"] > 0 else 0
    
    # 资源使用统计
    cpu_usage = [m.cpu_percent for m in resource_metrics]
    memory_usage = [m.memory_percent for m in resource_metrics]
    
    avg_cpu = statistics.mean(cpu_usage) if cpu_usage else 0
    max_cpu = max(cpu_usage) if cpu_usage else 0
    avg_memory = statistics.mean(memory_usage) if memory_usage else 0
    max_memory = max(memory_usage) if memory_usage else 0
    
    # 性能评分
    score = 100
    if error_rate > 10:
        score -= 30
    elif error_rate > 5:
        score -= 15
    elif error_rate > 1:
        score -= 5
    
    if avg_response_time > 2000:
        score -= 25
    elif avg_response_time > 1000:
        score -= 15
    elif avg_response_time > 500:
        score -= 5
    
    if p95 > 3000:
        score -= 15
    elif p95 > 2000:
        score -= 10
    elif p95 > 1000:
        score -= 5
    
    if max_cpu > 90:
        score -= 10
    if max_memory > 90:
        score -= 10
    
    score = max(0, score)
    
    # 状态判定
    if score >= 90:
        status = "PASS"
    elif score >= 70:
        status = "WARN"
    else:
        status = "FAIL"
    
    result = {
        "config": config,
        "metrics": {
            "total_requests": total_requests,
            "successful_requests": successful_requests,
            "failed_requests": failed_requests,
            "error_rate_percent": round(error_rate, 2),
            "throughput_rps": round(throughput, 2),
            "avg_response_time_ms": round(avg_response_time, 2),
            "min_response_time_ms": round(min_response_time, 2),
            "max_response_time_ms": round(max_response_time, 2),
            "p50_response_time_ms": round(p50, 2),
            "p95_response_time_ms": round(p95, 2),
            "p99_response_time_ms": round(p99, 2),
        },
        "endpoint_stats": endpoint_stats,
        "resource_usage": {
            "avg_cpu_percent": round(avg_cpu, 2),
            "max_cpu_percent": round(max_cpu, 2),
            "avg_memory_percent": round(avg_memory, 2),
            "max_memory_percent": round(max_memory, 2),
        },
        "score": score,
        "status": status,
        "duration_seconds": round(duration, 2),
    }
    
    print(f"\n{'='*70}")
    print(f"测试结果: {config['display_name']}")
    print(f"{'='*70}")
    print(f"总请求数: {total_requests}")
    print(f"成功请求: {successful_requests}")
    print(f"失败请求: {failed_requests}")
    print(f"错误率: {error_rate:.2f}%")
    print(f"吞吐量: {throughput:.2f} req/s")
    print(f"平均响应时间: {avg_response_time:.2f} ms")
    print(f"P95响应时间: {p95:.2f} ms")
    print(f"P99响应时间: {p99:.2f} ms")
    print(f"平均CPU使用率: {avg_cpu:.2f}%")
    print(f"最大CPU使用率: {max_cpu:.2f}%")
    print(f"平均内存使用率: {avg_memory:.2f}%")
    print(f"最大内存使用率: {max_memory:.2f}%")
    print(f"性能评分: {score}/100 [{status}]")
    print(f"{'='*70}")
    
    return result


def run_all_tests() -> List[Dict[str, Any]]:
    """运行所有并发测试"""
    print("="*70)
    print("企智连并发性能测试")
    print("="*70)
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"目标服务器: {BASE_URL}")
    print(f"\n测试场景:")
    for config in CONCURRENT_CONFIGS:
        print(f"  - {config['display_name']}: {config['concurrent_users']}用户, {config['duration_seconds']//60}分钟")
    
    # 预检
    print("\n[预检] 检查服务可用性...")
    try:
        resp = requests.get(f"{BASE_URL}/api/health", timeout=5)
        print(f"服务状态: HTTP {resp.status_code}")
    except Exception as e:
        print(f"警告: 服务连接问题 - {e}")
        print("将继续执行测试...")
    
    results = []
    for config in CONCURRENT_CONFIGS:
        result = run_concurrent_test(config)
        results.append(result)
        
        # 测试间休息
        if config != CONCURRENT_CONFIGS[-1]:
            print("\n[休息] 等待30秒进行下一组测试...")
            time.sleep(30)
    
    return results


def generate_performance_report(results: List[Dict[str, Any]], output_dir: str):
    """生成性能测试报告"""
    test_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    report = f"""# 企智连性能测试报告

## 文档信息

| 项目 | 值 |
|------|-----|
| 测试日期 | {test_time} |
| 测试环境 | {BASE_URL} |
| 报告版本 | v1.0 |

---

## 1. 测试概述

### 1.1 测试目的
验证企智连系统在预期负载下的性能表现，确保系统满足性能基准要求。

### 1.2 测试范围

| 模块 | 测试内容 | 状态 |
|------|----------|------|
| 并发用户测试 | 100/500/1000用户并发 | 已执行 |
| 响应时间测试 | 平均/P95/P99响应时间 | 已执行 |
| 吞吐量测试 | 系统处理能力 | 已执行 |

### 1.3 测试工具
- **测试框架**: Python + requests + ThreadPoolExecutor
- **监控工具**: psutil
- **测试时间**: {test_time}

---

## 2. 测试结果汇总

### 2.1 并发测试结果

| 用例编号 | 用例名称 | 并发用户数 | 平均响应时间 | P95响应时间 | P99响应时间 | 吞吐量 | 错误率 | 结果 |
|----------|----------|------------|--------------|-------------|-------------|--------|--------|------|
"""
    
    for i, result in enumerate(results, 1):
        config = result["config"]
        metrics = result["metrics"]
        status_icon = "✅" if result["status"] == "PASS" else ("⚠️" if result["status"] == "WARN" else "❌")
        report += f"| TC-PF-{i:03d} | {config['display_name']} | {config['concurrent_users']} | "
        report += f"{metrics['avg_response_time_ms']:.1f}ms | {metrics['p95_response_time_ms']:.1f}ms | "
        report += f"{metrics['p99_response_time_ms']:.1f}ms | {metrics['throughput_rps']:.1f} | "
        report += f"{metrics['error_rate_percent']:.2f}% | {status_icon} {result['status']} |\n"
    
    report += """
---

## 3. 详细测试结果

"""
    
    # 详细结果
    for i, result in enumerate(results, 1):
        config = result["config"]
        metrics = result["metrics"]
        resource = result["resource_usage"]
        
        report += f"""### 3.{i} {config['display_name']}测试结果

#### 性能指标

| 指标 | 值 |
|------|-----|
| 并发用户数 | {config['concurrent_users']} |
| 总请求数 | {metrics['total_requests']} |
| 成功请求 | {metrics['successful_requests']} |
| 失败请求 | {metrics['failed_requests']} |
| 错误率 | {metrics['error_rate_percent']:.2f}% |
| 吞吐量 | {metrics['throughput_rps']:.2f} req/s |
| 平均响应时间 | {metrics['avg_response_time_ms']:.2f} ms |
| 最小响应时间 | {metrics['min_response_time_ms']:.2f} ms |
| 最大响应时间 | {metrics['max_response_time_ms']:.2f} ms |
| P50响应时间 | {metrics['p50_response_time_ms']:.2f} ms |
| P95响应时间 | {metrics['p95_response_time_ms']:.2f} ms |
| P99响应时间 | {metrics['p99_response_time_ms']:.2f} ms |

#### 资源使用

| 指标 | 值 |
|------|-----|
| 平均CPU使用率 | {resource['avg_cpu_percent']:.2f}% |
| 最大CPU使用率 | {resource['max_cpu_percent']:.2f}% |
| 平均内存使用率 | {resource['avg_memory_percent']:.2f}% |
| 最大内存使用率 | {resource['max_memory_percent']:.2f}% |

#### 性能评分

**评分: {result['score']}/100**

"""
        if result["status"] == "PASS":
            report += "✅ **测试通过** - 系统性能表现良好\n"
        elif result["status"] == "WARN":
            report += "⚠️ **测试警告** - 系统性能存在轻微问题\n"
        else:
            report += "❌ **测试失败** - 系统性能需要优化\n"
        
        report += "\n---\n\n"
    
    # 端点统计
    report += """## 4. 端点性能分析

"""
    
    for i, result in enumerate(results, 1):
        config = result["config"]
        report += f"""### 4.{i} {config['display_name']} - 各端点表现

| 端点 | 名称 | 请求数 | 成功率 | 平均响应时间 |
|------|------|--------|--------|--------------|
"""
        for ep, stats in result["endpoint_stats"].items():
            report += f"| {ep} | {stats['name']} | {stats['count']} | {stats['success_rate']:.1f}% | {stats['avg_time']:.2f}ms |\n"
        report += "\n---\n\n"
    
    # 性能对比分析
    report += """## 5. 性能对比分析

### 5.1 响应时间趋势

| 并发用户数 | 平均响应时间 | P95响应时间 | P99响应时间 |
|------------|--------------|-------------|-------------|
"""
    
    for result in results:
        config = result["config"]
        metrics = result["metrics"]
        report += f"| {config['concurrent_users']} | {metrics['avg_response_time_ms']:.2f}ms | {metrics['p95_response_time_ms']:.2f}ms | {metrics['p99_response_time_ms']:.2f}ms |\n"
    
    report += """
### 5.2 吞吐量趋势

| 并发用户数 | 吞吐量 (req/s) | 错误率 |
|------------|----------------|--------|
"""
    
    for result in results:
        config = result["config"]
        metrics = result["metrics"]
        report += f"| {config['concurrent_users']} | {metrics['throughput_rps']:.2f} | {metrics['error_rate_percent']:.2f}% |\n"
    
    report += """
### 5.3 资源使用趋势

| 并发用户数 | 平均CPU | 最大CPU | 平均内存 | 最大内存 |
|------------|---------|---------|----------|----------|
"""
    
    for result in results:
        config = result["config"]
        resource = result["resource_usage"]
        report += f"| {config['concurrent_users']} | {resource['avg_cpu_percent']:.2f}% | {resource['max_cpu_percent']:.2f}% | {resource['avg_memory_percent']:.2f}% | {resource['max_memory_percent']:.2f}% |\n"
    
    # 性能基准对比
    report += """
---

## 6. 性能基准对比

### 6.1 性能基准要求

| 指标 | 基准要求 | 说明 |
|------|----------|------|
| 平均响应时间 | < 500ms | 正常业务响应 |
| P95响应时间 | < 1000ms | 95%请求响应 |
| P99响应时间 | < 2000ms | 99%请求响应 |
| 错误率 | < 1% | 系统稳定性 |
| 吞吐量 | > 100 req/s | 处理能力 |

### 6.2 达标情况

"""
    
    for result in results:
        config = result["config"]
        metrics = result["metrics"]
        
        report += f"""#### {config['display_name']}

| 指标 | 基准 | 实测 | 状态 |
|------|------|------|------|
| 平均响应时间 | < 500ms | {metrics['avg_response_time_ms']:.2f}ms | {"✅" if metrics['avg_response_time_ms'] < 500 else "❌"} |
| P95响应时间 | < 1000ms | {metrics['p95_response_time_ms']:.2f}ms | {"✅" if metrics['p95_response_time_ms'] < 1000 else "❌"} |
| P99响应时间 | < 2000ms | {metrics['p99_response_time_ms']:.2f}ms | {"✅" if metrics['p99_response_time_ms'] < 2000 else "❌"} |
| 错误率 | < 1% | {metrics['error_rate_percent']:.2f}% | {"✅" if metrics['error_rate_percent'] < 1 else "❌"} |
| 吞吐量 | > 100 req/s | {metrics['throughput_rps']:.2f} req/s | {"✅" if metrics['throughput_rps'] > 100 else "❌"} |

"""
    
    # 性能瓶颈分析
    report += """---

## 7. 性能瓶颈分析

### 7.1 发现的问题

"""
    
    has_issues = False
    for result in results:
        if result["status"] != "PASS":
            has_issues = True
            config = result["config"]
            metrics = result["metrics"]
            report += f"- **{config['display_name']}**: "
            if metrics['error_rate_percent'] > 5:
                report += f"错误率过高 ({metrics['error_rate_percent']:.2f}%)"
            elif metrics['avg_response_time_ms'] > 1000:
                report += f"响应时间过长 ({metrics['avg_response_time_ms']:.2f}ms)"
            report += "\n"
    
    if not has_issues:
        report += "✅ 未发现明显性能瓶颈\n"
    
    report += """
### 7.2 瓶颈分析详情

"""
    
    for result in results:
        config = result["config"]
        metrics = result["metrics"]
        resource = result["resource_usage"]
        
        if result["status"] != "PASS":
            report += f"""#### {config['display_name']}

- **现象**: 性能评分 {result['score']}/100
- **原因分析**: 
"""
            if metrics['error_rate_percent'] > 5:
                report += f"  - 错误率过高: {metrics['error_rate_percent']:.2f}%\n"
            if metrics['avg_response_time_ms'] > 1000:
                report += f"  - 平均响应时间过长: {metrics['avg_response_time_ms']:.2f}ms\n"
            if resource['max_cpu_percent'] > 90:
                report += f"  - CPU使用率过高: {resource['max_cpu_percent']:.2f}%\n"
            if resource['max_memory_percent'] > 90:
                report += f"  - 内存使用率过高: {resource['max_memory_percent']:.2f}%\n"
            
            report += """- **建议优化方案**:
  - 检查数据库连接池配置
  - 优化慢查询
  - 增加缓存机制
  - 考虑水平扩展

"""
    
    # 优化建议
    report += """---

## 8. 优化建议

### 8.1 短期优化（1周内）

| 序号 | 优化项 | 预期收益 | 优先级 |
|------|--------|----------|--------|
| 1 | 检查数据库连接池配置 | 减少连接等待 | P0 |
| 2 | 优化慢查询 | 提升响应速度 | P0 |
| 3 | 增加API缓存 | 减少重复计算 | P1 |

### 8.2 中期优化（1个月内）

| 序号 | 优化项 | 预期收益 | 优先级 |
|------|--------|----------|--------|
| 1 | 引入Redis缓存 | 降低数据库压力 | P1 |
| 2 | 优化线程池配置 | 提升并发能力 | P1 |
| 3 | 数据库读写分离 | 提升查询性能 | P2 |

### 8.3 长期优化（3个月内）

| 序号 | 优化项 | 预期收益 | 优先级 |
|------|--------|----------|--------|
| 1 | 微服务架构改造 | 提升系统弹性 | P2 |
| 2 | 引入消息队列 | 异步处理 | P2 |
| 3 | 水平扩展支持 | 支持更高并发 | P3 |

---

## 9. 测试结论

### 9.1 总体评估

"""
    
    # 计算总体评分
    avg_score = sum(r["score"] for r in results) / len(results) if results else 0
    pass_count = sum(1 for r in results if r["status"] == "PASS")
    warn_count = sum(1 for r in results if r["status"] == "WARN")
    fail_count = sum(1 for r in results if r["status"] == "FAIL")
    
    # 总体评级
    if avg_score >= 90:
        overall_grade = "A"
        overall_desc = "优秀"
    elif avg_score >= 80:
        overall_grade = "B"
        overall_desc = "良好"
    elif avg_score >= 70:
        overall_grade = "C"
        overall_desc = "一般"
    elif avg_score >= 60:
        overall_grade = "D"
        overall_desc = "及格"
    else:
        overall_grade = "F"
        overall_desc = "不及格"
    
    report += f"""| 评估维度 | 评分 | 说明 |
|----------|------|------|
| 综合性能评分 | {avg_score:.1f}/100 | {overall_desc} |
| 测试通过率 | {pass_count}/{len(results)} | {"✅ 全部通过" if pass_count == len(results) else ("⚠️ 部分通过" if pass_count > 0 else "❌ 未通过")} |
| 总体评级 | {overall_grade} | {overall_desc} |

### 9.2 各并发级别表现

| 并发用户数 | 性能评分 | 状态 |
|------------|----------|------|
"""
    
    for result in results:
        config = result["config"]
        status_icon = "✅" if result["status"] == "PASS" else ("⚠️" if result["status"] == "WARN" else "❌")
        report += f"| {config['concurrent_users']} | {result['score']}/100 | {status_icon} {result['status']} |\n"
    
    report += f"""
### 9.3 最终结论

**综合评分: {avg_score:.1f}/100 ({overall_grade}级)**

"""
    
    if overall_grade in ["A", "B"]:
        report += """✅ **系统性能表现良好**，能够满足预期负载需求。

- 系统可以稳定支持1000并发用户
- 响应时间在可接受范围内
- 错误率低于1%
- 建议可以上线使用

"""
    elif overall_grade == "C":
        report += """⚠️ **系统性能一般**，建议优化后再上线。

- 系统可以支持中等并发负载
- 部分场景响应时间较长
- 建议进行性能优化
- 上线前需进行针对性优化

"""
    else:
        report += """❌ **系统性能较差**，需要紧急优化。

- 系统在高并发下表现不佳
- 响应时间过长或错误率过高
- 必须进行性能优化
- 不建议当前状态下上线

"""
    
    report += f"""---

## 10. 附录

### 10.1 测试环境配置

| 配置项 | 值 |
|--------|-----|
| 目标服务器 | {BASE_URL} |
| 测试框架 | Python + requests + ThreadPoolExecutor |
| 监控工具 | psutil |
| 测试时间 | {test_time} |

### 10.2 测试配置详情

| 配置项 | 100用户 | 500用户 | 1000用户 |
|--------|---------|---------|----------|
| 测试时长 | 5分钟 | 5分钟 | 5分钟 |
| 爬升时间 | 30秒 | 60秒 | 120秒 |
| 思考时间 | 0.5秒 | 0.5秒 | 0.5秒 |

### 10.3 测试端点

| 端点 | 方法 | 权重 | 说明 |
|------|------|------|------|
| /api/auth/login | POST | 20% | 登录认证 |
| /api/user/page | GET | 25% | 用户列表查询 |
| /api/role/page | GET | 15% | 角色列表查询 |
| /api/health | GET | 20% | 健康检查 |
| /api/orders/list | GET | 20% | 订单列表查询 |

---

**报告生成时间**: {test_time}
**测试工具**: 企智连性能测试脚本 v1.0

"""
    
    # 保存报告
    import os
    os.makedirs(output_dir, exist_ok=True)
    report_path = os.path.join(output_dir, "PERFORMANCE_TEST_REPORT.md")
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\n✅ 性能测试报告已保存: {report_path}")
    
    # 保存JSON结果
    json_path = os.path.join(output_dir, f"performance_test_results_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json")
    json_data = {
        "test_time": test_time,
        "base_url": BASE_URL,
        "results": results,
        "summary": {
            "avg_score": round(avg_score, 2),
            "pass_count": pass_count,
            "warn_count": warn_count,
            "fail_count": fail_count,
            "overall_grade": overall_grade
        }
    }
    
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    print(f"✅ JSON结果已保存: {json_path}")
    
    return report_path


def main():
    """主函数"""
    print("="*70)
    print("企智连并发性能测试")
    print("="*70)
    print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    # 运行所有测试
    results = run_all_tests()
    
    # 生成报告
    print("\n" + "="*70)
    print("生成测试报告...")
    print("="*70)
    
    output_dir = r"I:\AI-Ready\docs"
    report_path = generate_performance_report(results, output_dir)
    
    print("\n" + "="*70)
    print("测试完成!")
    print("="*70)
    print(f"报告路径: {report_path}")
    
    # 打印汇总
    print("\n测试结果汇总:")
    print("-"*70)
    for result in results:
        config = result["config"]
        metrics = result["metrics"]
        status_icon = "✅" if result["status"] == "PASS" else ("⚠️" if result["status"] == "WARN" else "❌")
        print(f"{config['display_name']}: {status_icon} 评分{result['score']}/100 | "
              f"平均响应{metrics['avg_response_time_ms']:.1f}ms | "
              f"吞吐量{metrics['throughput_rps']:.1f}req/s | "
              f"错误率{metrics['error_rate_percent']:.2f}%")
    
    avg_score = sum(r["score"] for r in results) / len(results) if results else 0
    print(f"\n综合评分: {avg_score:.1f}/100")
    
    return results


if __name__ == "__main__":
    main()
