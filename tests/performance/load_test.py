#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 负载测试套件
执行持续负载测试，监控系统资源使用，分析系统稳定性
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

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 负载测试配置
LOAD_CONFIG = {
    "duration_minutes": 5,
    "concurrent_users": 50,
    "ramp_up_seconds": 30,
    "think_time_seconds": 1,
    "endpoints": [
        ("/api/user/page", "GET", 0.4),
        ("/api/role/page", "GET", 0.3),
        ("/api/health", "GET", 0.3),
    ],
}

# 测试结果
LOAD_RESULTS = {
    "test_time": "",
    "config": LOAD_CONFIG,
    "metrics": [],
    "resource_usage": [],
    "summary": {}
}


@dataclass
class RequestMetric:
    """请求指标"""
    timestamp: float
    endpoint: str
    method: str
    success: bool
    status_code: int
    response_time_ms: float
    error: str = ""


@dataclass
class ResourceMetric:
    """资源指标"""
    timestamp: float
    cpu_percent: float
    memory_percent: float
    memory_mb: float
    threads: int


class ResourceMonitor:
    """资源监控器"""
    
    def __init__(self, interval_seconds: float = 1.0):
        self.interval = interval_seconds
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
                    threads=psutil.Process().num_threads() if psutil.Process() else 0
                )
                self.metrics.append(metric)
            except Exception:
                pass


def make_request(endpoint: str, method: str = "GET") -> RequestMetric:
    """发送HTTP请求"""
    start_time = time.time()
    timestamp = start_time
    
    try:
        if method == "GET":
            resp = requests.get(f"{BASE_URL}{endpoint}", timeout=10)
        else:
            resp = requests.post(f"{BASE_URL}{endpoint}", timeout=10)
        
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp,
            endpoint=endpoint,
            method=method,
            success=200 <= resp.status_code < 400 or resp.status_code in [401, 403],
            status_code=resp.status_code,
            response_time_ms=elapsed_ms
        )
    except requests.exceptions.Timeout:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp, endpoint=endpoint, method=method,
            success=False, status_code=0, response_time_ms=elapsed_ms, error="timeout"
        )
    except requests.exceptions.ConnectionError:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp, endpoint=endpoint, method=method,
            success=False, status_code=0, response_time_ms=elapsed_ms, error="connection_refused"
        )
    except Exception as e:
        elapsed_ms = (time.time() - start_time) * 1000
        return RequestMetric(
            timestamp=timestamp, endpoint=endpoint, method=method,
            success=False, status_code=0, response_time_ms=elapsed_ms, error=str(e)[:50]
        )


def run_load_test() -> Dict[str, Any]:
    """运行负载测试"""
    LOAD_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready 负载测试")
    print("=" * 60)
    print(f"测试时间: {LOAD_RESULTS['test_time']}")
    print(f"持续时间: {LOAD_CONFIG['duration_minutes']}分钟")
    print(f"并发用户: {LOAD_CONFIG['concurrent_users']}")
    print(f"爬升时间: {LOAD_CONFIG['ramp_up_seconds']}秒")
    
    # 启动资源监控
    print("\n[启动] 资源监控...")
    monitor = ResourceMonitor(interval_seconds=1.0)
    monitor.start()
    
    # 测试指标队列
    metrics_queue = queue.Queue()
    stop_event = threading.Event()
    
    def worker(user_id: int):
        """工作线程"""
        while not stop_event.is_set():
            for endpoint, method, weight in LOAD_CONFIG["endpoints"]:
                if stop_event.is_set():
                    break
                metric = make_request(endpoint, method)
                metrics_queue.put(metric)
                
                # 思考时间
                if LOAD_CONFIG["think_time_seconds"] > 0:
                    time.sleep(LOAD_CONFIG["think_time_seconds"])
    
    # 启动并发用户
    print(f"\n[启动] {LOAD_CONFIG['concurrent_users']} 并发用户...")
    duration_seconds = LOAD_CONFIG["duration_minutes"] * 60
    ramp_up = LOAD_CONFIG["ramp_up_seconds"]
    
    with ThreadPoolExecutor(max_workers=LOAD_CONFIG["concurrent_users"]) as executor:
        # 爬升阶段
        users_per_batch = max(1, LOAD_CONFIG["concurrent_users"] // (ramp_up // 5))
        futures = []
        
        for batch in range(0, LOAD_CONFIG["concurrent_users"], users_per_batch):
            batch_size = min(users_per_batch, LOAD_CONFIG["concurrent_users"] - batch)
            for i in range(batch_size):
                futures.append(executor.submit(worker, batch + i))
            time.sleep(5)  # 每5秒增加一批用户
        
        print(f"[运行] 负载测试执行中 ({duration_seconds}秒)...")
        
        # 运行指定时间
        time.sleep(duration_seconds)
        
        # 停止测试
        print("\n[停止] 结束负载测试...")
        stop_event.set()
        
        for future in as_completed(futures):
            pass
    
    # 停止资源监控
    monitor.stop()
    
    # 收集结果
    metrics = []
    while not metrics_queue.empty():
        metrics.append(metrics_queue.get())
    
    LOAD_RESULTS["metrics"] = [
        {
            "timestamp": m.timestamp,
            "endpoint": m.endpoint,
            "method": m.method,
            "success": m.success,
            "status_code": m.status_code,
            "response_time_ms": m.response_time_ms,
            "error": m.error
        }
        for m in metrics
    ]
    
    LOAD_RESULTS["resource_usage"] = [
        {
            "timestamp": m.timestamp,
            "cpu_percent": m.cpu_percent,
            "memory_percent": m.memory_percent,
            "memory_mb": m.memory_mb,
            "threads": m.threads
        }
        for m in monitor.metrics
    ]
    
    # 计算汇总
    total_requests = len(metrics)
    successful_requests = sum(1 for m in metrics if m.success)
    failed_requests = sum(1 for m in metrics if not m.success)
    
    response_times = [m.response_time_ms for m in metrics]
    avg_response_time = sum(response_times) / len(response_times) if response_times else 0
    
    # 计算百分位
    sorted_times = sorted(response_times)
    p50 = sorted_times[int(len(sorted_times) * 0.5)] if sorted_times else 0
    p95 = sorted_times[int(len(sorted_times) * 0.95)] if sorted_times else 0
    p99 = sorted_times[int(len(sorted_times) * 0.99)] if sorted_times else 0
    
    # 吞吐量
    test_duration = duration_seconds
    throughput = total_requests / test_duration if test_duration > 0 else 0
    
    # 错误率
    error_rate = (failed_requests / total_requests * 100) if total_requests > 0 else 100
    
    # 资源使用
    cpu_usage = [m.cpu_percent for m in monitor.metrics]
    memory_usage = [m.memory_percent for m in monitor.metrics]
    
    avg_cpu = sum(cpu_usage) / len(cpu_usage) if cpu_usage else 0
    max_cpu = max(cpu_usage) if cpu_usage else 0
    avg_memory = sum(memory_usage) / len(memory_usage) if memory_usage else 0
    max_memory = max(memory_usage) if memory_usage else 0
    
    # 系统稳定性评分
    stability_score = 100
    if error_rate > 10:
        stability_score -= 30
    elif error_rate > 5:
        stability_score -= 15
    if avg_response_time > 2000:
        stability_score -= 20
    elif avg_response_time > 1000:
        stability_score -= 10
    if max_cpu > 90:
        stability_score -= 15
    if max_memory > 90:
        stability_score -= 15
    stability_score = max(0, stability_score)
    
    LOAD_RESULTS["summary"] = {
        "total_requests": total_requests,
        "successful_requests": successful_requests,
        "failed_requests": failed_requests,
        "error_rate_percent": round(error_rate, 2),
        "throughput_rps": round(throughput, 2),
        "avg_response_time_ms": round(avg_response_time, 2),
        "p50_response_time_ms": round(p50, 2),
        "p95_response_time_ms": round(p95, 2),
        "p99_response_time_ms": round(p99, 2),
        "avg_cpu_percent": round(avg_cpu, 2),
        "max_cpu_percent": round(max_cpu, 2),
        "avg_memory_percent": round(avg_memory, 2),
        "max_memory_percent": round(max_memory, 2),
        "stability_score": stability_score
    }
    
    print("\n" + "=" * 60)
    print("负载测试完成")
    print("=" * 60)
    print(f"总请求数: {total_requests}")
    print(f"成功请求: {successful_requests}")
    print(f"失败请求: {failed_requests}")
    print(f"错误率: {error_rate:.2f}%")
    print(f"吞吐量: {throughput:.2f} req/s")
    print(f"平均响应时间: {avg_response_time:.2f} ms")
    print(f"平均CPU使用率: {avg_cpu:.2f}%")
    print(f"平均内存使用率: {avg_memory:.2f}%")
    print(f"稳定性评分: {stability_score}/100")
    
    return LOAD_RESULTS


def generate_report(results: Dict[str, Any], output_path: str):
    """生成负载测试报告"""
    summary = results["summary"]
    
    report = f"""# AI-Ready 负载测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {results["test_time"]} |
| 测试环境 | {BASE_URL} |
| 持续时间 | {LOAD_CONFIG["duration_minutes"]}分钟 |
| 并发用户 | {LOAD_CONFIG["concurrent_users"]} |
| 爬升时间 | {LOAD_CONFIG["ramp_up_seconds"]}秒 |
| 稳定性评分 | **{summary["stability_score"]}/100** |

---

## 测试配置

| 参数 | 值 |
|------|-----|
| 持续时间 | {LOAD_CONFIG["duration_minutes"]}分钟 |
| 并发用户 | {LOAD_CONFIG["concurrent_users"]} |
| 爬升时间 | {LOAD_CONFIG["ramp_up_seconds"]}秒 |
| 思考时间 | {LOAD_CONFIG["think_time_seconds"]}秒 |

### 测试端点

| 端点 | 方法 | 权重 |
|------|------|------|
| /api/user/page | GET | 40% |
| /api/role/page | GET | 30% |
| /api/health | GET | 30% |

---

## 性能指标

### 请求统计

| 指标 | 值 |
|------|-----|
| 总请求数 | {summary["total_requests"]} |
| 成功请求 | {summary["successful_requests"]} |
| 失败请求 | {summary["failed_requests"]} |
| 错误率 | {summary["error_rate_percent"]}% |
| 吞吐量 | {summary["throughput_rps"]} req/s |

### 响应时间

| 指标 | 值 |
|------|-----|
| 平均响应时间 | {summary["avg_response_time_ms"]} ms |
| P50响应时间 | {summary["p50_response_time_ms"]} ms |
| P95响应时间 | {summary["p95_response_time_ms"]} ms |
| P99响应时间 | {summary["p99_response_time_ms"]} ms |

---

## 资源使用

### CPU使用率

| 指标 | 值 |
|------|-----|
| 平均CPU使用率 | {summary["avg_cpu_percent"]}% |
| 最大CPU使用率 | {summary["max_cpu_percent"]}% |

### 内存使用率

| 指标 | 值 |
|------|-----|
| 平均内存使用率 | {summary["avg_memory_percent"]}% |
| 最大内存使用率 | {summary["max_memory_percent"]}% |

---

## 系统稳定性分析

### 稳定性评分: {summary["stability_score"]}/100

"""

    if summary["stability_score"] >= 90:
        report += "✅ **系统稳定性优秀** - 能够稳定处理预期负载\n"
    elif summary["stability_score"] >= 70:
        report += "⚠️ **系统稳定性良好** - 存在轻微性能问题\n"
    elif summary["stability_score"] >= 50:
        report += "⚠️ **系统稳定性一般** - 需要优化\n"
    else:
        report += "❌ **系统稳定性较差** - 需要紧急优化\n"
    
    report += """
---

## 性能基线对比

| 指标 | 基线值 | 实际值 | 状态 |
|------|--------|--------|------|
| 平均响应时间 | < 100ms | {avg_rt}ms | {rt_status} |
| P95响应时间 | < 200ms | {p95}ms | {p95_status} |
| 错误率 | < 1% | {err}% | {err_status} |
| 吞吐量 | > 100 req/s | {tput} req/s | {tput_status} |

""".format(
        avg_rt=summary["avg_response_time_ms"],
        p95=summary["p95_response_time_ms"],
        err=summary["error_rate_percent"],
        tput=summary["throughput_rps"],
        rt_status="✅" if summary["avg_response_time_ms"] < 100 else ("⚠️" if summary["avg_response_time_ms"] < 500 else "❌"),
        p95_status="✅" if summary["p95_response_time_ms"] < 200 else ("⚠️" if summary["p95_response_time_ms"] < 1000 else "❌"),
        err_status="✅" if summary["error_rate_percent"] < 1 else ("⚠️" if summary["error_rate_percent"] < 5 else "❌"),
        tput_status="✅" if summary["throughput_rps"] > 100 else ("⚠️" if summary["throughput_rps"] > 50 else "❌")
    )
    
    report += """
---

## 改进建议

"""
    
    if summary["error_rate_percent"] > 5:
        report += "### 🔴 高优先级\n\n"
        report += f"- 错误率过高 ({summary['error_rate_percent']}%)，需检查服务稳定性\n"
        report += "- 检查API服务是否正常运行\n"
        report += "- 检查数据库连接池配置\n\n"
    
    if summary["avg_response_time_ms"] > 500:
        report += "### 🟠 中优先级\n\n"
        report += f"- 平均响应时间偏高 ({summary['avg_response_time_ms']}ms)\n"
        report += "- 考虑增加缓存机制\n"
        report += "- 优化数据库查询\n\n"
    
    if summary["max_cpu_percent"] > 80:
        report += "### 🟡 资源优化\n\n"
        report += f"- CPU使用率较高 ({summary['max_cpu_percent']}%)\n"
        report += "- 考虑水平扩展\n\n"
    
    report += f"""
---

## 测试结论

本次负载测试在 {LOAD_CONFIG['concurrent_users']} 并发用户下运行 {LOAD_CONFIG['duration_minutes']} 分钟。

"""
    
    if summary["stability_score"] >= 70:
        report += "系统整体表现稳定，能够满足预期负载需求。\n"
    else:
        report += "系统在当前负载下存在稳定性问题，建议进行优化后再上线。\n"
    
    report += f"""
---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\n报告已保存: {output_path}")


if __name__ == "__main__":
    results = run_load_test()
    
    report_path = r"I:\AI-Ready\docs\AI-Ready负载测试报告_20260403.md"
    import os
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    generate_report(results, report_path)
    
    # 保存JSON结果
    json_path = r"I:\AI-Ready\docs\load_test_results.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    print(f"JSON结果已保存: {json_path}")
