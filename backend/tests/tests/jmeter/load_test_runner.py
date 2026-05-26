#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 负载测试执行器
使用Python多线程模拟高并发负载测试
"""

import os
import sys
import json
import time
import threading
import requests
import statistics
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 测试配置
BASE_URL = "http://localhost:8080"
RESULTS_DIR = Path(__file__).parent / "results"

# 负载测试场景配置
TEST_SCENARIOS = {
    "normal": {
        "name": "正常负载测试",
        "concurrent_users": 50,
        "ramp_up_time": 10,
        "duration": 60,
        "requests_per_user": 100,
        "description": "模拟正常业务负载 - 50并发用户"
    },
    "peak": {
        "name": "峰值负载测试",
        "concurrent_users": 100,
        "ramp_up_time": 20,
        "duration": 120,
        "requests_per_user": 200,
        "description": "模拟峰值业务负载 - 100并发用户"
    },
    "stress": {
        "name": "压力测试",
        "concurrent_users": 200,
        "ramp_up_time": 30,
        "duration": 180,
        "requests_per_user": 300,
        "description": "系统压力测试 - 200并发用户"
    },
    "spike": {
        "name": "突发负载测试",
        "concurrent_users": 300,
        "ramp_up_time": 5,
        "duration": 60,
        "requests_per_user": 50,
        "description": "模拟突发流量 - 300并发用户"
    }
}

# API端点配置
ENDPOINTS = [
    {
        "name": "用户列表查询",
        "path": "/api/user/page",
        "method": "GET",
        "params": {"pageNum": 1, "pageSize": 10, "tenantId": 1},
        "weight": 0.4,
        "expected_max_time": 200
    },
    {
        "name": "角色列表查询",
        "path": "/api/role/page",
        "method": "GET",
        "params": {"pageNum": 1, "pageSize": 10},
        "weight": 0.3,
        "expected_max_time": 100
    },
    {
        "name": "健康检查",
        "path": "/api/health",
        "method": "GET",
        "params": {},
        "weight": 0.3,
        "expected_max_time": 50
    }
]


class LoadTestResult:
    """负载测试结果类"""
    def __init__(self):
        self.start_time = None
        self.end_time = None
        self.total_requests = 0
        self.successful_requests = 0
        self.failed_requests = 0
        self.response_times = []
        self.errors = []
        self.endpoint_stats = {}
        self.status_codes = {}
        self._lock = threading.Lock()
        
    def add_result(self, endpoint_name, response_time, success, status_code, error=None):
        with self._lock:
            self.total_requests += 1
            if success:
                self.successful_requests += 1
                self.response_times.append(response_time)
            else:
                self.failed_requests += 1
                if error:
                    self.errors.append(error)
            
            if endpoint_name not in self.endpoint_stats:
                self.endpoint_stats[endpoint_name] = {"total": 0, "success": 0, "failed": 0, "response_times": []}
            self.endpoint_stats[endpoint_name]["total"] += 1
            if success:
                self.endpoint_stats[endpoint_name]["success"] += 1
                self.endpoint_stats[endpoint_name]["response_times"].append(response_time)
            else:
                self.endpoint_stats[endpoint_name]["failed"] += 1
            
            if status_code not in self.status_codes:
                self.status_codes[status_code] = 0
            self.status_codes[status_code] += 1
    
    def get_summary(self):
        with self._lock:
            if not self.response_times:
                return {"total_requests": self.total_requests, "successful_requests": self.successful_requests,
                        "failed_requests": self.failed_requests, "success_rate": 0, "avg_response_time": 0,
                        "min_response_time": 0, "max_response_time": 0, "p50_response_time": 0,
                        "p95_response_time": 0, "p99_response_time": 0, "throughput": 0}
            
            sorted_times = sorted(self.response_times)
            total_time = (self.end_time - self.start_time).total_seconds() if self.end_time else 1
            
            return {
                "total_requests": self.total_requests,
                "successful_requests": self.successful_requests,
                "failed_requests": self.failed_requests,
                "success_rate": round(self.successful_requests / self.total_requests * 100, 2),
                "avg_response_time": round(statistics.mean(self.response_times), 2),
                "min_response_time": round(min(self.response_times), 2),
                "max_response_time": round(max(self.response_times), 2),
                "p50_response_time": round(sorted_times[len(sorted_times) // 2], 2),
                "p95_response_time": round(sorted_times[int(len(sorted_times) * 0.95)], 2),
                "p99_response_time": round(sorted_times[int(len(sorted_times) * 0.99)], 2),
                "throughput": round(self.total_requests / total_time, 2)
            }


def select_endpoint():
    import random
    weights = [e["weight"] for e in ENDPOINTS]
    return random.choices(ENDPOINTS, weights=weights)[0]


def make_request(endpoint):
    url = f"{BASE_URL}{endpoint['path']}"
    start_time = time.time()
    try:
        if endpoint["method"] == "GET":
            response = requests.get(url, params=endpoint.get("params", {}), timeout=30)
        else:
            response = requests.post(url, json=endpoint.get("params", {}), timeout=30)
        elapsed = (time.time() - start_time) * 1000
        success = response.status_code == 200
        return {"success": success, "response_time": elapsed, "status_code": response.status_code,
                "error": None if success else f"HTTP {response.status_code}"}
    except requests.exceptions.Timeout:
        return {"success": False, "response_time": 30000, "status_code": 0, "error": "Timeout"}
    except Exception as e:
        return {"success": False, "response_time": 0, "status_code": 0, "error": str(e)}



def user_session(user_id, scenario_config, result_collector, stop_event):
    requests_made = 0
    max_requests = scenario_config["requests_per_user"]
    while requests_made < max_requests and not stop_event.is_set():
        endpoint = select_endpoint()
        result = make_request(endpoint)
        result_collector.add_result(endpoint["name"], result["response_time"], result["success"],
                                    result["status_code"], result["error"])
        requests_made += 1
        time.sleep(0.1)


def run_load_test(scenario_name, scenario_config):
    print(f"\n{'='*70}")
    print(f"开始执行: {scenario_config['name']}")
    print(f"描述: {scenario_config['description']}")
    print(f"并发用户: {scenario_config['concurrent_users']}")
    print(f"持续时间: {scenario_config['duration']}秒")
    print(f"{'='*70}\n")
    
    result = LoadTestResult()
    result.start_time = datetime.now()
    stop_event = threading.Event()
    
    with ThreadPoolExecutor(max_workers=scenario_config["concurrent_users"]) as executor:
        futures = []
        ramp_up_delay = scenario_config["ramp_up_time"] / scenario_config["concurrent_users"]
        
        for user_id in range(scenario_config["concurrent_users"]):
            future = executor.submit(user_session, user_id, scenario_config, result, stop_event)
            futures.append(future)
            time.sleep(ramp_up_delay)
        
        time.sleep(scenario_config["duration"])
        stop_event.set()
    
    result.end_time = datetime.now()
    return result


def save_results(scenario_name, result, output_dir):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    result_dir = output_dir / f"{scenario_name}_{timestamp}"
    result_dir.mkdir(parents=True, exist_ok=True)
    
    summary = result.get_summary()
    
    result_file = result_dir / "result.json"
    with open(result_file, "w", encoding="utf-8") as f:
        json.dump({
            "scenario": scenario_name,
            "timestamp": timestamp,
            "summary": summary,
            "endpoint_stats": result.endpoint_stats,
            "status_codes": result.status_codes,
            "errors": result.errors[:100]
        }, f, ensure_ascii=False, indent=2)
    
    return result_dir, summary


def print_summary(scenario_name, summary):
    print(f"\n{'='*70}")
    print(f"负载测试结果: {scenario_name}")
    print(f"{'='*70}")
    print(f"总请求数: {summary['total_requests']}")
    print(f"成功请求: {summary['success']}
    print(f"失败请求: {summary['failed_requests']}")
    print(f"成功率: {summary['success_rate']}%")
    print(f"\n响应时间 (ms):")
    print(f"  平均: {summary['avg_response_time']}")
    print(f"  最小: {summary['min_response_time']}")
    print(f"  最大: {summary['max_response_time']}")
    print(f"  P50:  {summary['p50_response_time']}")
    print(f"  P95:  {summary['p95_response_time']}")
    print(f"  P99:  {summary['p99_response_time']}")
    print(f"\n吞吐量: {summary['throughput']} req/s")
    print(f"{'='*70}\n")


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="AI-Ready Load Test Runner")
    parser.add_argument("--scenario", "-s", default="normal", choices=list(TEST_SCENAR<think>The user wants me to execute the load testing task. Let me continue working on this task. I need to complete the load test task and report to supervisor.