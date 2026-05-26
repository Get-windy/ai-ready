#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready JMeter 性能测试执行器
支持本地和远程执行JMeter测试脚本
"""

import os
import subprocess
import time
from datetime import datetime
from pathlib import Path

# 配置
JMETER_HOME = os.environ.get("JMETER_HOME", r"C:\apache-jmeter-5.6.3")
BASE_DIR = Path(__file__).parent
TEST_PLAN = BASE_DIR / "ai-ready-api-performance-test.jmx"
RESULTS_DIR = BASE_DIR / "results"

# 测试场景配置
TEST_SCENARIOS = {
    "light": {
        "threads": 50,
        "rampup": 10,
        "duration": 60,
        "description": "轻量级负载测试 - 50并发用户"
    },
    "medium": {
        "threads": 100,
        "rampup": 20,
        "duration": 120,
        "description": "中等级负载测试 - 100并发用户"
    },
    "heavy": {
        "threads": 200,
        "rampup": 30,
        "duration": 180,
        "description": "重量级负载测试 - 200并发用户"
    },
    "stress": {
        "threads": 500,
        "rampup": 60,
        "duration": 300,
        "description": "压力测试 - 500并发用户"
    }
}


def check_jmeter():
    """检查JMeter是否可用"""
    jmeter_bin = os.path.join(JMETER_HOME, "bin", "jmeter.bat" if os.name == "nt" else "jmeter")
    if not os.path.exists(jmeter_bin):
        print(f"[ERROR] JMeter not found at {jmeter_bin}")
        print("Please set JMETER_HOME environment variable")
        return None
    return jmeter_bin


def prepare_results_dir():
    """创建结果目录"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    result_dir = RESULTS_DIR / timestamp
    result_dir.mkdir(parents=True, exist_ok=True)
    return result_dir


def run_jmeter_test(jmeter_bin, scenario="medium", base_url="localhost", port="8080"):
    """执行JMeter测试"""
    if scenario not in TEST_SCENARIOS:
        print(f"[ERROR] Unknown scenario: {scenario}")
        return None
    
    config = TEST_SCENARIOS[scenario]
    print(f"\n{'='*60}")
    print(f"AI-Ready JMeter Performance Test")
    print(f"{'='*60}")
    print(f"Scenario: {scenario} - {config['description']}")
    print(f"Base URL: {base_url}:{port}")
    print(f"Threads: {config['threads']}, Ramp-up: {config['rampup']}s, Duration: {config['duration']}s")
    print(f"{'='*60}\n")
    
    # 准备结果目录
    result_dir = prepare_results_dir()
    
    # 输出文件
    jtl_file = result_dir / "results.jtl"
    log_file = result_dir / "jmeter.log"
    report_dir = result_dir / "html-report"
    
    # 构建JMeter命令
    cmd = [
        jmeter_bin,
        "-n",  # 非GUI模式
        "-t", str(TEST_PLAN),  # 测试计划文件
        "-l", str(jtl_file),  # 结果文件
        "-j", str(log_file),  # 日志文件
        "-e",  # 生成HTML报告
        "-o", str(report_dir),  # HTML报告目录
        f"-Jthreads={config['threads']}",
        f"-Jrampup={config['rampup']}",
        f"-Jduration={config['duration']}",
        f"-JBASE_URL={base_url}",
        f"-JPORT={port}",
    ]
    
    print(f"Executing: {' '.join(cmd)}")
    print(f"Results will be saved to: {result_dir}")
    
    start_time = time.time()
    
    try:
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True
        )
        
        # 实时输出
        for line in process.stdout:
            print(line, end="")
        
        process.wait()
        elapsed = time.time() - start_time
        
        print(f"\n{'='*60}")
        print(f"Test completed in {elapsed:.2f} seconds")
        print(f"Results saved to: {result_dir}")
        print(f"HTML Report: {report_dir / 'index.html'}")
        print(f"{'='*60}")
        
        return result_dir
        
    except Exception as e:
        print(f"[ERROR] Failed to run JMeter test: {e}")
        return None


def run_all_scenarios(jmeter_bin, base_url="localhost", port="8080"):
    """运行所有测试场景"""
    results = {}
    for scenario in TEST_SCENARIOS:
        print(f"\n\n{'#'*60}")
        print(f"# Running scenario: {scenario}")
        print(f"{'#'*60}")
        result = run_jmeter_test(jmeter_bin, scenario, base_url, port)
        results[scenario] = result
        time.sleep(10)  # 场景间休息
    
    return results


def main():
    """主函数"""
    import argparse
    
    parser = argparse.ArgumentParser(description="AI-Ready JMeter Performance Test Runner")
    parser.add_argument("--scenario", "-s", default="medium", 
                        choices=list(TEST_SCENARIOS.keys()),
                        help="Test scenario to run")
    parser.add_argument("--all", "-a", action="store_true",
                        help="Run all scenarios")
    parser.add_argument("--url", "-u", default="localhost",
                        help="Base URL (default: localhost)")
    parser.add_argument("--port", "-p", default="8080",
                        help="Port number (default: 8080)")
    
    args = parser.parse_args()
    
    # 检查JMeter
    jmeter_bin = check_jmeter()
    if not jmeter_bin:
        return 1
    
    # 确保测试计划存在
    if not TEST_PLAN.exists():
        print(f"[ERROR] Test plan not found: {TEST_PLAN}")
        return 1
    
    # 创建结果目录
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    
    # 执行测试
    if args.all:
        run_all_scenarios(jmeter_bin, args.url, args.port)
    else:
        run_jmeter_test(jmeter_bin, args.scenario, args.url, args.port)
    
    return 0


if __name__ == "__main__":
    exit(main())
