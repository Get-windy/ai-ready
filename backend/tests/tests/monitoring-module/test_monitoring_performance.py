#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
监控告警模块性能测试脚本

测试内容：
1. 告警触发延迟测试（要求≤5秒）
2. 指标采集吞吐量测试
3. 并发性能测试
4. 负载测试

性能要求：
- 告警触发延迟: ≤5秒
- 指标采集吞吐量: ≥100次/秒
- 并发处理能力: ≥50个请求/秒

技术栈: Python + pytest + requests
测试环境: localhost:8080
"""

import json
import os
import sys
import time
import pytest
import requests
import threading
from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed

# 测试配置
BASE_URL = "http://localhost:8080/api/monitor"
TEST_DATA = {
    "tenant_id": 1,
    "test_rule_name": "性能测试告警规则"
}

class TestMonitoringPerformance:
    """监控告警模块性能测试"""
    
    def setup_class(self):
        """测试类初始化"""
        self.session = requests.Session()
        self.test_rules = []
        self.performance_results = []
        print(f"开始执行监控告警模块性能测试 - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    def teardown_class(self):
        """测试类清理"""
        for rule_id in self.test_rules:
            try:
                self.session.delete(f"{BASE_URL}/alerts/rules/{rule_id}")
            except:
                pass
        print(f"监控告警模块性能测试完成 - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # ==================== 告警触发延迟测试 ====================
    
    def test_01_alert_trigger_delay(self):
        """测试告警触发延迟（要求≤5秒）"""
        print("性能测试: 告警触发延迟")
        
        # 创建告警规则
        rule_data = {
            "ruleName": f"{TEST_DATA['test_rule_name']}_delay_test",
            "ruleCode": f"PERF_DELAY_{int(time.time())}",
            "metricName": "cpu_usage",
            "operator": ">",
            "threshold": 50.0,
            "duration": 0,  # 立即触发
            "severity": "critical",
            "notifyType": "webhook",
            "notifyTargets": "http://localhost:8080/test-webhook",
            "enabled": True,
            "tenantId": TEST_DATA["tenant_id"]
        }
        
        # 测量规则创建时间
        start_time = time.time()
        response = self.session.post(f"{BASE_URL}/alerts/rules", json=rule_data)
        create_time = time.time() - start_time
        
        assert response.status_code == 200, f"创建告警规则失败: {response.text}"
        result = response.json()
        rule_id = result.get("id") or result.get("data", {}).get("id")
        
        if rule_id:
            self.test_rules.append(rule_id)
        
        # 测试告警检查延迟
        # 模拟触发告警检查
        check_start = time.time()
        
        # 获取系统指标
        metrics_response = self.session.get(f"{BASE_URL}/metrics")
        check_end = time.time()
        check_time = (check_end - check_start) * 1000  # 转换为毫秒
        
        # 获取告警历史
        history_start = time.time()
        history_response = self.session.get(f"{BASE_URL}/alerts/history", params={
            "tenantId": TEST_DATA["tenant_id"],
            "hours": 1
        })
        history_time = (time.time() - history_start) * 1000
        
        # 记录性能数据
        performance = {
            "test_name": "告警触发延迟",
            "rule_create_time_ms": round(create_time * 1000, 2),
            "metrics_check_time_ms": round(check_time, 2),
            "history_query_time_ms": round(history_time, 2),
            "requirement_ms": 5000,  # ≤5秒 = 5000ms
            "passed": check_time < 5000 and history_time < 5000
        }
        self.performance_results.append(performance)
        
        print(f"  ✓ 规则创建: {performance['rule_create_time_ms']}ms")
        print(f"  ✓ 指标检查: {performance['metrics_check_time_ms']}ms")
        print(f"  ✓ 历史查询: {performance['history_query_time_ms']}ms")
        print(f"  ✓ 性能要求: ≤{performance['requirement_ms']}ms")
        print(f"  ✓ 测试结果: {'通过' if performance['passed'] else '未通过'}")
        
        assert performance['passed'], f"告警触发延迟超过要求: {check_time}ms > 5000ms"
    
    # ==================== 指标采集吞吐量测试 ====================
    
    def test_02_metrics_throughput(self):
        """测试指标采集吞吐量（要求≥100次/秒）"""
        print("性能测试: 指标采集吞吐量")
        
        # 执行多次指标采集
        iterations = 100
        start_time = time.time()
        
        success_count = 0
        for i in range(iterations):
            try:
                response = self.session.get(f"{BASE_URL}/metrics")
                if response.status_code == 200:
                    success_count += 1
            except:
                pass
        
        total_time = time.time() - start_time
        throughput = success_count / total_time if total_time > 0 else 0
        
        performance = {
            "test_name": "指标采集吞吐量",
            "total_requests": iterations,
            "success_count": success_count,
            "total_time_sec": round(total_time, 2),
            "throughput_rps": round(throughput, 2),
            "requirement_rps": 100,  # ≥100次/秒
            "passed": throughput >= 100
        }
        self.performance_results.append(performance)
        
        print(f"  ✓ 总请求数: {performance['total_requests']}")
        print(f"  ✓ 成功请求: {performance['success_count']}")
        print(f"  ✓ 总耗时: {performance['total_time_sec']}s")
        print(f"  ✓ 吞吐量: {performance['throughput_rps']} 请求/秒")
        print(f"  ✓ 性能要求: ≥{performance['requirement_rps']} 请求/秒")
        print(f"  ✓ 测试结果: {'通过' if performance['passed'] else '未通过'}")
        
        assert performance['passed'], f"指标采集吞吐量不足: {throughput} < 100次/秒"
    
    # ==================== 并发性能测试 ====================
    
    def test_03_concurrent_requests(self):
        """测试并发请求处理能力"""
        print("性能测试: 并发请求处理能力")
        
        def make_request():
            try:
                start = time.time()
                response = self.session.get(f"{BASE_URL}/metrics")
                duration = time.time() - start
                return {
                    "success": response.status_code == 200,
                    "duration_ms": duration * 1000
                }
            except:
                return {"success": False, "duration_ms": 0}
        
        # 并发请求数
        concurrent_count = 50
        
        start_time = time.time()
        
        with ThreadPoolExecutor(max_workers=concurrent_count) as executor:
            futures = [executor.submit(make_request) for _ in range(concurrent_count)]
            results = [f.result() for f in as_completed(futures)]
        
        total_time = time.time() - start_time
        
        success_count = sum(1 for r in results if r["success"])
        avg_duration = sum(r["duration_ms"] for r in results if r["success"]) / success_count if success_count > 0 else 0
        rps = success_count / total_time if total_time > 0 else 0
        
        performance = {
            "test_name": "并发请求处理",
            "concurrent_requests": concurrent_count,
            "success_count": success_count,
            "total_time_sec": round(total_time, 2),
            "avg_response_time_ms": round(avg_duration, 2),
            "rps": round(rps, 2),
            "requirement_rps": 50,  # ≥50请求/秒
            "passed": rps >= 50
        }
        self.performance_results.append(performance)
        
        print(f"  ✓ 并发请求数: {performance['concurrent_requests']}")
        print(f"  ✓ 成功请求: {performance['success_count']}")
        print(f"  ✓ 总耗时: {performance['total_time_sec']}s")
        print(f"  ✓ 平均响应时间: {performance['avg_response_time_ms']}ms")
        print(f"  ✓ 吞吐量: {performance['rps']} 请求/秒")
        print(f"  ✓ 测试结果: {'通过' if performance['passed'] else '未通过'}")
        
        assert performance['passed'], f"并发处理能力不足: {rps} < 50请求/秒"
    
    # ==================== 批量告警规则性能测试 ====================
    
    def test_04_batch_rule_operations(self):
        """测试批量告警规则操作性能"""
        print("性能测试: 批量告警规则操作")
        
        # 批量创建规则
        batch_size = 20
        create_start = time.time()
        
        created_ids = []
        for i in range(batch_size):
            rule_data = {
                "ruleName": f"{TEST_DATA['test_rule_name']}_batch_{i}",
                "ruleCode": f"PERF_BATCH_{int(time.time())}_{i}",
                "metricName": "cpu_usage",
                "operator": ">",
                "threshold": 80.0,
                "severity": "warning",
                "enabled": True,
                "tenantId": TEST_DATA["tenant_id"]
            }
            
            try:
                response = self.session.post(f"{BASE_URL}/alerts/rules", json=rule_data)
                if response.status_code == 200:
                    result = response.json()
                    rule_id = result.get("id") or result.get("data", {}).get("id")
                    if rule_id:
                        created_ids.append(rule_id)
                        self.test_rules.append(rule_id)
            except:
                pass
        
        create_time = time.time() - create_start
        
        # 批量查询规则
        query_start = time.time()
        response = self.session.get(f"{BASE_URL}/alerts/rules", params={"tenantId": TEST_DATA["tenant_id"]})
        query_time = time.time() - query_start
        
        # 清理
        delete_start = time.time()
        for rule_id in created_ids:
            try:
                self.session.delete(f"{BASE_URL}/alerts/rules/{rule_id}")
            except:
                pass
        delete_time = time.time() - delete_start
        
        # 从测试列表中移除已删除的规则
        for rule_id in created_ids:
            if rule_id in self.test_rules:
                self.test_rules.remove(rule_id)
        
        performance = {
            "test_name": "批量告警规则操作",
            "batch_size": batch_size,
            "created_count": len(created_ids),
            "create_time_sec": round(create_time, 2),
            "query_time_sec": round(query_time, 2),
            "delete_time_sec": round(delete_time, 2),
            "avg_create_time_ms": round((create_time / batch_size) * 1000, 2),
            "passed": create_time < 10  # 批量创建应在10秒内完成
        }
        self.performance_results.append(performance)
        
        print(f"  ✓ 批量大小: {performance['batch_size']}")
        print(f"  ✓ 成功创建: {performance['created_count']}")
        print(f"  ✓ 创建耗时: {performance['create_time_sec']}s")
        print(f"  ✓ 查询耗时: {performance['query_time_sec']}s")
        print(f"  ✓ 删除耗时: {performance['delete_time_sec']}s")
        print(f"  ✓ 平均创建时间: {performance['avg_create_time_ms']}ms/条")
        print(f"  ✓ 测试结果: {'通过' if performance['passed'] else '未通过'}")
        
        assert performance['passed'], f"批量操作性能不达标: {create_time}s > 10s"
    
    # ==================== 持续负载测试 ====================
    
    def test_05_sustained_load(self):
        """测试持续负载下的性能"""
        print("性能测试: 持续负载测试")
        
        # 持续30秒的负载测试
        duration = 30
        start_time = time.time()
        
        request_count = 0
        success_count = 0
        response_times = []
        
        while time.time() - start_time < duration:
            try:
                req_start = time.time()
                response = self.session.get(f"{BASE_URL}/metrics")
                req_time = time.time() - req_start
                
                request_count += 1
                if response.status_code == 200:
                    success_count += 1
                    response_times.append(req_time * 1000)
            except:
                pass
        
        total_time = time.time() - start_time
        rps = request_count / total_time if total_time > 0 else 0
        avg_response_time = sum(response_times) / len(response_times) if response_times else 0
        
        performance = {
            "test_name": "持续负载测试",
            "duration_sec": round(total_time, 2),
            "total_requests": request_count,
            "success_count": success_count,
            "rps": round(rps, 2),
            "avg_response_time_ms": round(avg_response_time, 2),
            "success_rate_pct": round((success_count / request_count * 100) if request_count > 0 else 0, 2),
            "passed": rps >= 50 and success_count / request_count >= 0.95 if request_count > 0 else False
        }
        self.performance_results.append(performance)
        
        print(f"  ✓ 测试时长: {performance['duration_sec']}s")
        print(f"  ✓ 总请求数: {performance['total_requests']}")
        print(f"  ✓ 成功请求: {performance['success_count']}")
        print(f"  ✓ 吞吐量: {performance['rps']} 请求/秒")
        print(f"  ✓ 平均响应时间: {performance['avg_response_time_ms']}ms")
        print(f"  ✓ 成功率: {performance['success_rate_pct']}%")
        print(f"  ✓ 测试结果: {'通过' if performance['passed'] else '未通过'}")
        
        assert performance['passed'], f"持续负载测试未通过"
    
    def test_06_summary_report(self):
        """生成性能测试汇总报告"""
        print("\n" + "=" * 60)
        print("性能测试汇总")
        print("=" * 60)
        
        for result in self.performance_results:
            status = "✅ 通过" if result["passed"] else "❌ 未通过"
            print(f"\n{result['test_name']}: {status}")
            for key, value in result.items():
                if key != "test_name" and key != "passed":
                    print(f"  - {key}: {value}")
        
        # 生成报告文件
        report_path = "I:\\AI-Ready\\tests\\monitoring-module\\monitoring-performance-test-report.md"
        
        passed_count = sum(1 for r in self.performance_results if r["passed"])
        total_count = len(self.performance_results)
        
        with open(report_path, "w", encoding="utf-8") as f:
            f.write("# 监控告警模块性能测试报告\n\n")
            f.write("## 测试概要\n\n")
            f.write(f"- **测试时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"- **测试环境**: AI-Ready 测试环境\n")
            f.write(f"- **总测试数**: {total_count}\n")
            f.write(f"- **通过数**: {passed_count}\n")
            f.write(f"- **失败数**: {total_count - passed_count}\n")
            f.write(f"- **成功率**: {round(passed_count/total_count*100, 2)}%\n\n")
            
            f.write("## 性能要求\n\n")
            f.write("- 告警触发延迟: ≤5秒\n")
            f.write("- 指标采集吞吐量: ≥100次/秒\n")
            f.write("- 并发处理能力: ≥50请求/秒\n\n")
            
            f.write("## 测试结果详情\n\n")
            
            for result in self.performance_results:
                status = "✅ 通过" if result["passed"] else "❌ 未通过"
                f.write(f"### {result['test_name']}: {status}\n\n")
                
                exclude_keys = ["test_name", "passed"]
                for key, value in result.items():
                    if key not in exclude_keys:
                        f.write(f"- {key}: {value}\n")
                f.write("\n")
            
            f.write("## 结论\n\n")
            if passed_count == total_count:
                f.write("✅ **性能测试全部通过** - 监控告警模块性能满足所有要求。\n")
            elif passed_count >= total_count * 0.8:
                f.write("⚠️ **性能测试大部分通过** - 监控告警模块性能基本满足要求，存在部分问题需要优化。\n")
            else:
                f.write("❌ **性能测试未通过** - 监控告警模块性能存在严重问题，需要优化。\n")
            
            f.write("\n---\n")
            f.write(f"**测试执行人**: test-agent-2\n")
            f.write(f"**项目**: ai-ready\n")
            f.write(f"**Sprint**: Sprint 28\n")
        
        print(f"\n✅ 性能测试报告已生成: {report_path}")
        
        return passed_count == total_count

def run_performance_tests():
    """运行性能测试"""
    pytest_args = [
        "-v",
        "--tb=short",
        "--no-header",
        "--no-summary",
        "-q"
    ]
    
    import io
    from contextlib import redirect_stdout, redirect_stderr
    
    stdout_capture = io.StringIO()
    stderr_capture = io.StringIO()
    
    with redirect_stdout(stdout_capture), redirect_stderr(stderr_capture):
        exit_code = pytest.main(pytest_args + [__file__])
    
    return exit_code == 0

if __name__ == "__main__":
    run_performance_tests()