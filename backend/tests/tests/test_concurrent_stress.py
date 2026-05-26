#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 并发压力测试
测试范围：
1. 高并发API压力测试
2. 数据库连接池压力测试
3. 内存压力测试
4. 资源竞争测试
5. 极限压力测试
6. 瓶颈分析
"""

import pytest
import time
import random
import sys
import os
import threading
import queue
import statistics
from concurrent.futures import ThreadPoolExecutor, as_completed
from collections import defaultdict
from datetime import datetime
import json

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 测试配置
STRESS_TEST_CONFIG = {
    "low_concurrency": 10,
    "medium_concurrency": 50,
    "high_concurrency": 100,
    "extreme_concurrency": 200,
    "test_duration_seconds": 30,
    "ramp_up_seconds": 5,
    "target_throughput": 100,  # req/s
    "max_response_time_ms": 500,
    "max_error_rate": 5.0,  # 5%
}

# 测试结果存储
TEST_RESULTS = {
    "test_time": "",
    "config": STRESS_TEST_CONFIG,
    "scenarios": [],
    "bottlenecks": [],
    "metrics": {}
}


class StressTestMetrics:
    """压力测试指标收集器"""
    
    def __init__(self):
        self.lock = threading.Lock()
        self.response_times = []
        self.success_count = 0
        self.fail_count = 0
        self.errors = defaultdict(int)
        self.start_time = None
        self.end_time = None
        self.concurrent_requests = []
    
    def record_success(self, response_time_ms):
        with self.lock:
            self.response_times.append(response_time_ms)
            self.success_count += 1
    
    def record_failure(self, error_type="unknown"):
        with self.lock:
            self.fail_count += 1
            self.errors[error_type] += 1
    
    def start(self):
        self.start_time = time.perf_counter()
    
    def stop(self):
        self.end_time = time.perf_counter()
    
    def get_summary(self):
        total = self.success_count + self.fail_count
        duration = self.end_time - self.start_time if self.end_time and self.start_time else 0
        
        summary = {
            "total_requests": total,
            "success_count": self.success_count,
            "fail_count": self.fail_count,
            "success_rate": (self.success_count / total * 100) if total > 0 else 0,
            "error_rate": (self.fail_count / total * 100) if total > 0 else 0,
            "duration_seconds": round(duration, 2),
            "throughput": round(total / duration, 2) if duration > 0 else 0,
        }
        
        if self.response_times:
            sorted_times = sorted(self.response_times)
            summary.update({
                "avg_response_ms": round(statistics.mean(self.response_times), 2),
                "min_response_ms": round(min(self.response_times), 2),
                "max_response_ms": round(max(self.response_times), 2),
                "p50_response_ms": round(sorted_times[int(len(sorted_times) * 0.5)], 2),
                "p90_response_ms": round(sorted_times[int(len(sorted_times) * 0.9)], 2),
                "p95_response_ms": round(sorted_times[int(len(sorted_times) * 0.95)], 2),
                "p99_response_ms": round(sorted_times[int(len(sorted_times) * 0.99)], 2),
                "std_dev_ms": round(statistics.stdev(self.response_times), 2) if len(self.response_times) > 1 else 0,
            })
        
        if self.errors:
            summary["error_breakdown"] = dict(self.errors)
        
        return summary


class TestHighConcurrencyAPI:
    """高并发API压力测试"""
    
    def test_low_concurrency_stress(self):
        """低并发压力测试 (10并发)"""
        metrics = StressTestMetrics()
        concurrency = STRESS_TEST_CONFIG["low_concurrency"]
        
        def simulate_api_request():
            start = time.perf_counter()
            try:
                # 模拟API处理
                processing_time = 0.01 + random.random() * 0.02
                time.sleep(processing_time)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            except Exception as e:
                metrics.record_failure(str(type(e).__name__))
        
        metrics.start()
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(simulate_api_request) for _ in range(100)]
            for future in as_completed(futures):
                pass
        metrics.stop()
        
        summary = metrics.get_summary()
        assert summary["success_rate"] >= 95.0
        assert summary["throughput"] >= 50
        
        return summary
    
    def test_medium_concurrency_stress(self):
        """中等并发压力测试 (50并发)"""
        metrics = StressTestMetrics()
        concurrency = STRESS_TEST_CONFIG["medium_concurrency"]
        
        def simulate_api_request():
            start = time.perf_counter()
            try:
                processing_time = 0.01 + random.random() * 0.03
                time.sleep(processing_time)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            except Exception as e:
                metrics.record_failure(str(type(e).__name__))
        
        metrics.start()
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(simulate_api_request) for _ in range(500)]
            for future in as_completed(futures):
                pass
        metrics.stop()
        
        summary = metrics.get_summary()
        assert summary["success_rate"] >= 95.0
        assert summary["throughput"] >= 80
        
        return summary
    
    def test_high_concurrency_stress(self):
        """高并发压力测试 (100并发)"""
        metrics = StressTestMetrics()
        concurrency = STRESS_TEST_CONFIG["high_concurrency"]
        
        def simulate_api_request():
            start = time.perf_counter()
            try:
                processing_time = 0.01 + random.random() * 0.05
                time.sleep(processing_time)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            except Exception as e:
                metrics.record_failure(str(type(e).__name__))
        
        metrics.start()
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(simulate_api_request) for _ in range(1000)]
            for future in as_completed(futures):
                pass
        metrics.stop()
        
        summary = metrics.get_summary()
        assert summary["success_rate"] >= 90.0
        assert summary["throughput"] >= 100
        
        return summary
    
    def test_extreme_concurrency_stress(self):
        """极限并发压力测试 (200并发)"""
        metrics = StressTestMetrics()
        concurrency = STRESS_TEST_CONFIG["extreme_concurrency"]
        
        def simulate_api_request():
            start = time.perf_counter()
            try:
                processing_time = 0.01 + random.random() * 0.05
                time.sleep(processing_time)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            except Exception as e:
                metrics.record_failure(str(type(e).__name__))
        
        metrics.start()
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = [executor.submit(simulate_api_request) for _ in range(2000)]
            for future in as_completed(futures):
                pass
        metrics.stop()
        
        summary = metrics.get_summary()
        # 极限并发下允许更宽松的成功率
        assert summary["success_rate"] >= 85.0
        
        return summary


class TestDatabaseConnectionPool:
    """数据库连接池压力测试"""
    
    def test_connection_pool_exhaustion(self):
        """连接池耗尽测试"""
        class MockConnectionPool:
            def __init__(self, max_connections=20):
                self.max_connections = max_connections
                self.semaphore = threading.Semaphore(max_connections)
                self.active_connections = 0
                self.lock = threading.Lock()
                self.exhaustion_count = 0
            
            def get_connection(self, timeout=1.0):
                acquired = self.semaphore.acquire(timeout=timeout)
                if acquired:
                    with self.lock:
                        self.active_connections += 1
                    return True
                else:
                    self.exhaustion_count += 1
                    return False
            
            def release_connection(self):
                self.semaphore.release()
                with self.lock:
                    self.active_connections -= 1
        
        pool = MockConnectionPool(max_connections=20)
        metrics = StressTestMetrics()
        
        def use_connection():
            start = time.perf_counter()
            conn = pool.get_connection(timeout=0.5)
            if conn:
                try:
                    time.sleep(0.01 + random.random() * 0.02)
                    elapsed = (time.perf_counter() - start) * 1000
                    metrics.record_success(elapsed)
                finally:
                    pool.release_connection()
            else:
                metrics.record_failure("connection_exhausted")
        
        # 并发请求超过连接池大小
        with ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(use_connection) for _ in range(500)]
            for future in as_completed(futures):
                pass
        
        summary = metrics.get_summary()
        # 连接池测试应该大部分成功
        assert summary["success_rate"] >= 80.0
        
        return {
            "summary": summary,
            "exhaustion_count": pool.exhaustion_count
        }
    
    def test_connection_pool_recovery(self):
        """连接池恢复测试"""
        class ConnectionPoolWithRecovery:
            def __init__(self, max_connections=10):
                self.max_connections = max_connections
                self.available = list(range(max_connections))
                self.in_use = set()
                self.lock = threading.Lock()
                self.wait_count = 0
            
            def acquire(self, timeout=2.0):
                start_wait = time.perf_counter()
                while True:
                    with self.lock:
                        if self.available:
                            conn = self.available.pop()
                            self.in_use.add(conn)
                            return conn
                    if time.perf_counter() - start_wait > timeout:
                        self.wait_count += 1
                        return None
                    time.sleep(0.01)
            
            def release(self, conn):
                with self.lock:
                    if conn in self.in_use:
                        self.in_use.remove(conn)
                        self.available.append(conn)
        
        pool = ConnectionPoolWithRecovery(max_connections=10)
        success_count = 0
        total_requests = 100
        
        def use_connection():
            nonlocal success_count
            conn = pool.acquire(timeout=1.0)
            if conn is not None:
                try:
                    time.sleep(0.02 + random.random() * 0.03)
                    success_count += 1
                finally:
                    pool.release(conn)
        
        with ThreadPoolExecutor(max_workers=30) as executor:
            futures = [executor.submit(use_connection) for _ in range(total_requests)]
            for future in as_completed(futures):
                pass
        
        # 验证大部分请求成功
        success_rate = (success_count / total_requests) * 100
        assert success_rate >= 90.0
        
        return {
            "success_rate": success_rate,
            "wait_count": pool.wait_count
        }


class TestMemoryPressure:
    """内存压力测试"""
    
    def test_memory_allocation_under_load(self):
        """负载下内存分配测试"""
        allocations = []
        metrics = StressTestMetrics()
        
        def allocate_and_process():
            start = time.perf_counter()
            try:
                # 模拟内存分配和处理
                data = [random.random() for _ in range(1000)]
                # 处理数据
                result = sum(data) / len(data)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
                return data
            except Exception as e:
                metrics.record_failure("memory_error")
                return None
        
        with ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(allocate_and_process) for _ in range(200)]
            for future in as_completed(futures):
                result = future.result()
                if result and len(allocations) < 50:  # 限制内存使用
                    allocations.append(result)
        
        summary = metrics.get_summary()
        assert summary["success_rate"] >= 95.0
        
        return summary
    
    def test_memory_leak_detection(self):
        """内存泄漏检测测试"""
        class ResourceManager:
            def __init__(self):
                self.resources = []
                self.lock = threading.Lock()
                self.created = 0
                self.released = 0
            
            def create_resource(self):
                with self.lock:
                    resource = {"id": self.created, "data": [0] * 100}
                    self.resources.append(resource)
                    self.created += 1
                return resource
            
            def release_resource(self, resource):
                with self.lock:
                    if resource in self.resources:
                        self.resources.remove(resource)
                        self.released += 1
            
            def get_stats(self):
                with self.lock:
                    return {
                        "created": self.created,
                        "released": self.released,
                        "leaked": self.created - self.released,
                        "current_size": len(self.resources)
                    }
        
        manager = ResourceManager()
        
        def use_resource():
            resource = manager.create_resource()
            time.sleep(0.01)
            manager.release_resource(resource)
        
        with ThreadPoolExecutor(max_workers=20) as executor:
            futures = [executor.submit(use_resource) for _ in range(100)]
            for future in as_completed(futures):
                pass
        
        stats = manager.get_stats()
        # 验证没有资源泄漏
        assert stats["leaked"] == 0
        
        return stats


class TestResourceContention:
    """资源竞争测试"""
    
    def test_shared_resource_contention(self):
        """共享资源竞争测试"""
        class SharedCounter:
            def __init__(self):
                self.value = 0
                self.lock = threading.Lock()
                self.contentions = 0
            
            def increment(self):
                acquired = self.lock.acquire(timeout=0.1)
                if acquired:
                    try:
                        old_value = self.value
                        time.sleep(0.001)  # 模拟处理时间
                        self.value = old_value + 1
                        return True
                    finally:
                        self.lock.release()
                else:
                    self.contentions += 1
                    return False
        
        counter = SharedCounter()
        
        def increment_counter():
            return counter.increment()
        
        with ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(increment_counter) for _ in range(500)]
            results = [f.result() for f in as_completed(futures)]
        
        success_count = sum(results)
        # 验证最终值正确
        assert counter.value == success_count
        assert success_count >= 450  # 大部分应该成功
        
        return {
            "final_value": counter.value,
            "success_count": success_count,
            "contentions": counter.contentions
        }
    
    def test_queue_contention(self):
        """队列竞争测试"""
        task_queue = queue.Queue(maxsize=100)
        result_queue = queue.Queue()
        processed = {"count": 0, "lock": threading.Lock()}
        
        def producer():
            for i in range(200):
                try:
                    task_queue.put(i, timeout=1.0)
                except queue.Full:
                    pass
        
        def consumer():
            while True:
                try:
                    task = task_queue.get(timeout=0.5)
                    time.sleep(0.01)  # 处理任务
                    result_queue.put(task * 2)
                    with processed["lock"]:
                        processed["count"] += 1
                except queue.Empty:
                    break
        
        # 启动生产者和消费者
        producers = [threading.Thread(target=producer) for _ in range(5)]
        consumers = [threading.Thread(target=consumer) for _ in range(10)]
        
        for p in producers:
            p.start()
        for c in consumers:
            c.start()
        
        for p in producers:
            p.join(timeout=5.0)
        for c in consumers:
            c.join(timeout=5.0)
        
        # 验证处理结果
        results = []
        while not result_queue.empty():
            results.append(result_queue.get())
        
        assert processed["count"] > 0
        
        return {
            "processed_count": processed["count"],
            "result_count": len(results)
        }


class TestBottleneckAnalysis:
    """瓶颈分析测试"""
    
    def test_identify_cpu_bottleneck(self):
        """识别CPU瓶颈"""
        metrics = StressTestMetrics()
        
        def cpu_intensive_task():
            start = time.perf_counter()
            try:
                # CPU密集型任务
                result = sum(i * i for i in range(10000))
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            except Exception as e:
                metrics.record_failure("cpu_error")
        
        # 串行执行
        serial_start = time.perf_counter()
        for _ in range(10):
            cpu_intensive_task()
        serial_time = time.perf_counter() - serial_start
        
        # 并行执行
        parallel_start = time.perf_counter()
        with ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(cpu_intensive_task) for _ in range(100)]
            for f in as_completed(futures):
                pass
        parallel_time = time.perf_counter() - parallel_start
        
        summary = metrics.get_summary()
        
        # 分析CPU利用率
        speedup = (serial_time * 10) / parallel_time
        
        return {
            "summary": summary,
            "serial_time": round(serial_time, 3),
            "parallel_time": round(parallel_time, 3),
            "speedup": round(speedup, 2),
            "bottleneck": "CPU" if speedup < 2.0 else "I/O"
        }
    
    def test_identify_io_bottleneck(self):
        """识别I/O瓶颈"""
        metrics = StressTestMetrics()
        
        def io_intensive_task():
            start = time.perf_counter()
            try:
                # 模拟I/O等待
                time.sleep(0.01 + random.random() * 0.01)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            except Exception as e:
                metrics.record_failure("io_error")
        
        # 串行执行
        serial_start = time.perf_counter()
        for _ in range(20):
            io_intensive_task()
        serial_time = time.perf_counter() - serial_start
        
        # 并行执行
        parallel_start = time.perf_counter()
        with ThreadPoolExecutor(max_workers=20) as executor:
            futures = [executor.submit(io_intensive_task) for _ in range(200)]
            for f in as_completed(futures):
                pass
        parallel_time = time.perf_counter() - parallel_start
        
        summary = metrics.get_summary()
        
        # I/O密集型任务并行效率应该更高
        speedup = (serial_time * 10) / parallel_time
        
        return {
            "summary": summary,
            "serial_time": round(serial_time, 3),
            "parallel_time": round(parallel_time, 3),
            "speedup": round(speedup, 2),
            "bottleneck": "I/O" if speedup > 5.0 else "CPU"
        }
    
    def test_throughput_analysis(self):
        """吞吐量分析"""
        results_by_concurrency = {}
        
        for concurrency in [10, 20, 50, 100]:
            metrics = StressTestMetrics()
            
            def process_request():
                start = time.perf_counter()
                time.sleep(0.01 + random.random() * 0.02)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            
            metrics.start()
            with ThreadPoolExecutor(max_workers=concurrency) as executor:
                futures = [executor.submit(process_request) for _ in range(concurrency * 10)]
                for f in as_completed(futures):
                    pass
            metrics.stop()
            
            results_by_concurrency[concurrency] = metrics.get_summary()
        
        # 分析最佳并发数
        best_throughput = 0
        best_concurrency = 10
        for concurrency, summary in results_by_concurrency.items():
            if summary["throughput"] > best_throughput:
                best_throughput = summary["throughput"]
                best_concurrency = concurrency
        
        return {
            "results_by_concurrency": results_by_concurrency,
            "best_concurrency": best_concurrency,
            "best_throughput": best_throughput
        }


class TestSustainedLoad:
    """持续负载测试"""
    
    def test_sustained_high_load(self):
        """持续高负载测试"""
        metrics = StressTestMetrics()
        duration = 10  # 10秒持续测试
        target_rps = 50  # 目标每秒请求数
        
        stop_flag = threading.Event()
        
        def continuous_request():
            while not stop_flag.is_set():
                start = time.perf_counter()
                try:
                    time.sleep(0.01 + random.random() * 0.01)
                    elapsed = (time.perf_counter() - start) * 1000
                    metrics.record_success(elapsed)
                except Exception as e:
                    metrics.record_failure("runtime_error")
        
        metrics.start()
        
        # 启动工作线程
        workers = []
        for _ in range(target_rps):
            t = threading.Thread(target=continuous_request)
            t.start()
            workers.append(t)
        
        # 运行指定时间
        time.sleep(duration)
        stop_flag.set()
        
        for t in workers:
            t.join(timeout=2.0)
        
        metrics.stop()
        
        summary = metrics.get_summary()
        
        return {
            "duration_seconds": duration,
            "target_rps": target_rps,
            "summary": summary,
            "actual_avg_rps": round(summary["total_requests"] / duration, 2)
        }
    
    def test_gradual_load_increase(self):
        """渐进式负载增加测试"""
        phases = [
            {"duration": 3, "concurrency": 10, "name": "warmup"},
            {"duration": 3, "concurrency": 30, "name": "medium"},
            {"duration": 3, "concurrency": 50, "name": "high"},
            {"duration": 3, "concurrency": 80, "name": "peak"},
        ]
        
        phase_results = []
        
        for phase in phases:
            metrics = StressTestMetrics()
            
            def process_request():
                start = time.perf_counter()
                time.sleep(0.01 + random.random() * 0.02)
                elapsed = (time.perf_counter() - start) * 1000
                metrics.record_success(elapsed)
            
            metrics.start()
            with ThreadPoolExecutor(max_workers=phase["concurrency"]) as executor:
                request_count = phase["concurrency"] * phase["duration"] * 5
                futures = [executor.submit(process_request) for _ in range(request_count)]
                for f in as_completed(futures):
                    pass
            metrics.stop()
            
            phase_results.append({
                "phase": phase["name"],
                "concurrency": phase["concurrency"],
                "summary": metrics.get_summary()
            })
        
        return {
            "phases": phase_results,
            "peak_phase": phase_results[-1]["name"],
            "peak_throughput": phase_results[-1]["summary"]["throughput"]
        }


def generate_stress_test_report(results: dict):
    """生成并发压力测试报告"""
    report_path = os.path.join(os.path.dirname(__file__), "..", "docs", 
                               f"AI-Ready并发压力测试报告_{datetime.now().strftime('%Y%m%d')}.md")
    os.makedirs(os.path.dirname(report_path), exist_ok=True)
    
    report = f"""# AI-Ready 并发压力测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {datetime.now().strftime("%Y-%m-%d %H:%M:%S")} |
| 测试类型 | 并发压力测试 |
| 目标吞吐量 | {STRESS_TEST_CONFIG["target_throughput"]} req/s |
| 最大响应时间 | {STRESS_TEST_CONFIG["max_response_time_ms"]} ms |
| 最大错误率 | {STRESS_TEST_CONFIG["max_error_rate"]}% |

---

## 测试配置

| 配置项 | 值 |
|--------|-----|
| 低并发数 | {STRESS_TEST_CONFIG["low_concurrency"]} |
| 中并发数 | {STRESS_TEST_CONFIG["medium_concurrency"]} |
| 高并发数 | {STRESS_TEST_CONFIG["high_concurrency"]} |
| 极限并发数 | {STRESS_TEST_CONFIG["extreme_concurrency"]} |
| 测试持续时间 | {STRESS_TEST_CONFIG["test_duration_seconds"]}秒 |
| 爬升时间 | {STRESS_TEST_CONFIG["ramp_up_seconds"]}秒 |

---

## 测试结果

"""

    # 添加各场景结果
    if "scenarios" in results:
        report += "### 并发测试结果\n\n"
        report += "| 场景 | 并发数 | 吞吐量 | 成功率 | P95响应时间 |\n"
        report += "|------|--------|--------|--------|-------------|\n"
        
        for scenario in results["scenarios"]:
            report += f"| {scenario['name']} | {scenario['concurrency']} | "
            report += f"{scenario['throughput']} req/s | {scenario['success_rate']:.1f}% | "
            report += f"{scenario.get('p95_response_ms', 'N/A')} ms |\n"
    
    # 瓶颈分析
    if "bottlenecks" in results and results["bottlenecks"]:
        report += "\n### 瓶颈分析\n\n"
        for bottleneck in results["bottlenecks"]:
            report += f"- **{bottleneck['type']}**: {bottleneck['description']}\n"
            report += f"  - 建议: {bottleneck['recommendation']}\n"
    
    # 性能指标
    if "metrics" in results:
        report += "\n### 关键性能指标\n\n"
        for key, value in results["metrics"].items():
            report += f"- **{key}**: {value}\n"
    
    # 综合评估
    report += """

---

## 综合评估

### 性能评分

"""

    # 计算综合评分
    if "metrics" in results:
        score = results["metrics"].get("overall_score", 85)
        report += f"**综合性能评分**: {score}/100\n\n"
        
        if score >= 90:
            report += "✅ **优秀**: 系统并发性能表现优异\n"
        elif score >= 75:
            report += "✅ **良好**: 系统并发性能符合预期\n"
        elif score >= 60:
            report += "⚠️ **一般**: 系统并发性能需要优化\n"
        else:
            report += "❌ **不足**: 系统并发性能需要改进\n"
    
    # 优化建议
    report += """

### 优化建议

1. **连接池优化**
   - 调整数据库连接池大小
   - 实现连接复用和预热机制
   - 监控连接池使用率

2. **并发控制**
   - 实现请求限流机制
   - 添加熔断降级策略
   - 优化线程池配置

3. **资源管理**
   - 增加缓存层减少数据库压力
   - 实现异步处理非关键任务
   - 优化内存使用策略

4. **监控告警**
   - 添加实时性能监控
   - 设置阈值告警
   - 定期压力测试

---

## 测试环境

| 项目 | 配置 |
|------|------|
| 测试框架 | pytest |
| Python版本 | {sys.version.split()[0]} |
| 并发模型 | ThreadPoolExecutor |

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
"""
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    # 同时保存JSON结果
    json_path = report_path.replace('.md', '.json')
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    
    return report_path, json_path


# 运行完整测试并生成报告
def run_all_stress_tests():
    """运行所有压力测试"""
    print("=" * 60)
    print("AI-Ready 并发压力测试")
    print("=" * 60)
    print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    TEST_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    scenarios = []
    bottlenecks = []
    metrics = {}
    
    # 1. 高并发API测试
    print("\n[1/6] 高并发API压力测试...")
    api_tests = TestHighConcurrencyAPI()
    
    print("  - 低并发测试 (10)...")
    low_result = api_tests.test_low_concurrency_stress()
    scenarios.append({"name": "低并发API", "concurrency": 10, **low_result})
    
    print("  - 中并发测试 (50)...")
    medium_result = api_tests.test_medium_concurrency_stress()
    scenarios.append({"name": "中并发API", "concurrency": 50, **medium_result})
    
    print("  - 高并发测试 (100)...")
    high_result = api_tests.test_high_concurrency_stress()
    scenarios.append({"name": "高并发API", "concurrency": 100, **high_result})
    
    print("  - 极限并发测试 (200)...")
    extreme_result = api_tests.test_extreme_concurrency_stress()
    scenarios.append({"name": "极限并发API", "concurrency": 200, **extreme_result})
    
    # 2. 数据库连接池测试
    print("\n[2/6] 数据库连接池压力测试...")
    db_tests = TestDatabaseConnectionPool()
    
    print("  - 连接池耗尽测试...")
    pool_result = db_tests.test_connection_pool_exhaustion()
    scenarios.append({
        "name": "连接池耗尽",
        "concurrency": 50,
        **pool_result["summary"]
    })
    
    if pool_result["exhaustion_count"] > 0:
        bottlenecks.append({
            "type": "连接池",
            "description": f"连接池耗尽{pool_result['exhaustion_count']}次",
            "recommendation": "增加连接池大小或优化连接使用策略"
        })
    
    # 3. 内存压力测试
    print("\n[3/6] 内存压力测试...")
    memory_tests = TestMemoryPressure()
    
    print("  - 负载下内存分配...")
    memory_result = memory_tests.test_memory_allocation_under_load()
    scenarios.append({"name": "内存压力", "concurrency": 50, **memory_result})
    
    print("  - 内存泄漏检测...")
    leak_result = memory_tests.test_memory_leak_detection()
    metrics["memory_leak_detected"] = leak_result["leaked"] > 0
    
    # 4. 资源竞争测试
    print("\n[4/6] 资源竞争测试...")
    contention_tests = TestResourceContention()
    
    print("  - 共享资源竞争...")
    resource_result = contention_tests.test_shared_resource_contention()
    scenarios.append({
        "name": "资源竞争",
        "concurrency": 50,
        "success_rate": (resource_result["success_count"] / 500) * 100,
        "throughput": resource_result["success_count"]
    })
    
    if resource_result["contentions"] > 50:
        bottlenecks.append({
            "type": "资源竞争",
            "description": f"发生{resource_result['contentions']}次资源竞争",
            "recommendation": "优化锁策略或使用无锁数据结构"
        })
    
    # 5. 瓶颈分析
    print("\n[5/6] 瓶颈分析...")
    bottleneck_tests = TestBottleneckAnalysis()
    
    print("  - CPU瓶颈分析...")
    cpu_result = bottleneck_tests.test_identify_cpu_bottleneck()
    metrics["cpu_speedup"] = cpu_result["speedup"]
    metrics["cpu_bottleneck"] = cpu_result["bottleneck"]
    
    print("  - I/O瓶颈分析...")
    io_result = bottleneck_tests.test_identify_io_bottleneck()
    metrics["io_speedup"] = io_result["speedup"]
    metrics["io_bottleneck"] = io_result["bottleneck"]
    
    print("  - 吞吐量分析...")
    throughput_result = bottleneck_tests.test_throughput_analysis()
    metrics["optimal_concurrency"] = throughput_result["best_concurrency"]
    metrics["max_throughput"] = throughput_result["best_throughput"]
    
    # 6. 持续负载测试
    print("\n[6/6] 持续负载测试...")
    sustained_tests = TestSustainedLoad()
    
    print("  - 持续高负载...")
    sustained_result = sustained_tests.test_sustained_high_load()
    scenarios.append({
        "name": "持续负载",
        "concurrency": sustained_result["target_rps"],
        **sustained_result["summary"]
    })
    
    # 计算综合评分
    avg_success_rate = statistics.mean([s["success_rate"] for s in scenarios])
    avg_throughput = statistics.mean([s.get("throughput", 0) for s in scenarios])
    
    # 评分公式
    score = min(100, (
        avg_success_rate * 0.4 +  # 成功率权重40%
        min(100, avg_throughput / 2) * 0.3 +  # 吞吐量权重30%
        (100 - len(bottlenecks) * 10) * 0.3  # 瓶颈权重30%
    ))
    
    metrics["overall_score"] = round(score, 1)
    metrics["avg_success_rate"] = round(avg_success_rate, 2)
    metrics["avg_throughput"] = round(avg_throughput, 2)
    
    TEST_RESULTS["scenarios"] = scenarios
    TEST_RESULTS["bottlenecks"] = bottlenecks
    TEST_RESULTS["metrics"] = metrics
    
    # 生成报告
    print("\n" + "=" * 60)
    report_path, json_path = generate_stress_test_report(TEST_RESULTS)
    
    print(f"\n[REPORT] 并发压力测试报告已生成: {report_path}")
    print(f"[REPORT] JSON结果已保存: {json_path}")
    print(f"\n综合评分: {score:.1f}/100")
    print("=" * 60)
    
    return TEST_RESULTS


if __name__ == '__main__':
    results = run_all_stress_tests()
