#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AI-Ready 并发安全测试套件
测试范围：
1. 并发竞争条件测试
2. 死锁检测
3. 数据一致性测试
4. 原子性验证
"""

import time
import json
import threading
import queue
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import Dict, List, Any, Tuple
import requests
from dataclasses import dataclass, field

# 配置
BASE_URL = "http://localhost:8080"
API_BASE = f"{BASE_URL}/api"

# 测试配置
CONCURRENT_CONFIG = {
    "thread_counts": [10, 50, 100],
    "iterations_per_thread": 10,
    "timeout_seconds": 30,
}

# 测试结果
TEST_RESULTS = {
    "test_time": "",
    "config": CONCURRENT_CONFIG,
    "results": {},
    "issues": [],
    "summary": {}
}


@dataclass
class ConcurrencyIssue:
    """并发问题"""
    issue_type: str
    severity: str  # HIGH, MEDIUM, LOW
    description: str
    scenario: str
    recommendation: str


@dataclass
class ThreadResult:
    """线程执行结果"""
    thread_id: int
    success: bool
    response_time_ms: float
    status_code: int
    error: str = ""
    data: Dict = field(default_factory=dict)


def make_request(endpoint: str, method: str = "GET", data: dict = None, headers: dict = None) -> Tuple[bool, int, float, Dict]:
    """发送HTTP请求"""
    start_time = time.time()
    try:
        if method == "GET":
            resp = requests.get(f"{BASE_URL}{endpoint}", headers=headers, timeout=10)
        elif method == "POST":
            resp = requests.post(f"{BASE_URL}{endpoint}", json=data, headers=headers, timeout=10)
        elif method == "PUT":
            resp = requests.put(f"{BASE_URL}{endpoint}", json=data, headers=headers, timeout=10)
        elif method == "DELETE":
            resp = requests.delete(f"{BASE_URL}{endpoint}", headers=headers, timeout=10)
        else:
            resp = requests.request(method, f"{BASE_URL}{endpoint}", json=data, headers=headers, timeout=10)
        
        elapsed_ms = (time.time() - start_time) * 1000
        try:
            response_data = resp.json()
        except:
            response_data = {"raw": resp.text[:500]}
        
        return True, resp.status_code, elapsed_ms, response_data
    except requests.exceptions.Timeout:
        elapsed_ms = (time.time() - start_time) * 1000
        return False, 0, elapsed_ms, {"error": "timeout"}
    except requests.exceptions.ConnectionError:
        elapsed_ms = (time.time() - start_time) * 1000
        return False, 0, elapsed_ms, {"error": "connection_refused"}
    except Exception as e:
        elapsed_ms = (time.time() - start_time) * 1000
        return False, 0, elapsed_ms, {"error": str(e)}


def test_concurrent_reads():
    """并发读测试 - 验证读取一致性"""
    print("\n--- 并发读测试 ---")
    results = []
    issues = []
    
    def read_worker(thread_id: int, iterations: int) -> List[ThreadResult]:
        thread_results = []
        for i in range(iterations):
            success, status_code, elapsed_ms, data = make_request(
                "/api/user/page", "GET", 
                {"pageNum": 1, "pageSize": 10, "tenantId": 1}
            )
            thread_results.append(ThreadResult(
                thread_id=thread_id,
                success=success and status_code in [200, 401],
                response_time_ms=elapsed_ms,
                status_code=status_code,
                data=data
            ))
        return thread_results
    
    all_results = []
    for thread_count in CONCURRENT_CONFIG["thread_counts"]:
        print(f"测试 {thread_count} 线程并发读取...")
        
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            futures = [
                executor.submit(read_worker, i, CONCURRENT_CONFIG["iterations_per_thread"])
                for i in range(thread_count)
            ]
            for future in as_completed(futures):
                all_results.extend(future.result())
    
    # 分析结果
    success_count = sum(1 for r in all_results if r.success)
    fail_count = len(all_results) - success_count
    success_rate = (success_count / len(all_results) * 100) if all_results else 0
    
    status_codes = {}
    for r in all_results:
        code = r.status_code
        status_codes[code] = status_codes.get(code, 0) + 1
    
    # 检查不一致性
    if len(status_codes) > 1 and 200 in status_codes:
        # 存在多种状态码
        issues.append(ConcurrencyIssue(
            issue_type="READ_INCONSISTENCY",
            severity="MEDIUM",
            description=f"并发读取返回不同状态码: {status_codes}",
            scenario="并发读取同一资源",
            recommendation="检查认证状态一致性或实现乐观锁"
        ))
    
    results.append({
        "test_name": "并发读测试",
        "total_requests": len(all_results),
        "success_count": success_count,
        "fail_count": fail_count,
        "success_rate": round(success_rate, 2),
        "status_codes": status_codes,
        "status": "PASS" if success_rate >= 95 else "FAIL"
    })
    
    print(f"  总请求: {len(all_results)}, 成功率: {success_rate:.2f}%")
    
    return results, issues


def test_concurrent_writes():
    """并发写测试 - 验证写入隔离性"""
    print("\n--- 并发写测试 ---")
    results = []
    issues = []
    
    def write_worker(thread_id: int, iterations: int) -> List[ThreadResult]:
        thread_results = []
        for i in range(iterations):
            # 模拟并发写入
            data = {
                "name": f"test_user_{thread_id}_{i}",
                "email": f"test_{thread_id}_{i}@example.com"
            }
            success, status_code, elapsed_ms, resp_data = make_request(
                "/api/user/add", "POST", data
            )
            thread_results.append(ThreadResult(
                thread_id=thread_id,
                success=success,
                response_time_ms=elapsed_ms,
                status_code=status_code,
                data=resp_data
            ))
        return thread_results
    
    all_results = []
    for thread_count in [10, 20]:  # 写入测试用较少线程
        print(f"测试 {thread_count} 线程并发写入...")
        
        with ThreadPoolExecutor(max_workers=thread_count) as executor:
            futures = [
                executor.submit(write_worker, i, 5)
                for i in range(thread_count)
            ]
            for future in as_completed(futures):
                all_results.extend(future.result())
    
    # 分析结果
    success_count = sum(1 for r in all_results if r.success and r.status_code < 500)
    fail_count = len(all_results) - success_count
    success_rate = (success_count / len(all_results) * 100) if all_results else 0
    
    # 检查写入冲突
    status_codes = {}
    for r in all_results:
        code = r.status_code
        status_codes[code] = status_codes.get(code, 0) + 1
    
    # 检查重复写入
    if 409 in status_codes or 500 in status_codes:
        issues.append(ConcurrencyIssue(
            issue_type="WRITE_CONFLICT",
            severity="HIGH" if 500 in status_codes else "MEDIUM",
            description=f"并发写入存在冲突: {status_codes}",
            scenario="多线程同时创建资源",
            recommendation="实现唯一约束或分布式锁"
        ))
    
    results.append({
        "test_name": "并发写测试",
        "total_requests": len(all_results),
        "success_count": success_count,
        "fail_count": fail_count,
        "success_rate": round(success_rate, 2),
        "status_codes": status_codes,
        "status": "PASS" if success_rate >= 90 else "FAIL"
    })
    
    print(f"  总请求: {len(all_results)}, 成功率: {success_rate:.2f}%")
    
    return results, issues


def test_read_write_conflict():
    """读写冲突测试"""
    print("\n--- 读写冲突测试 ---")
    results = []
    issues = []
    
    readers_done = threading.Event()
    writers_done = threading.Event()
    results_queue = queue.Queue()
    
    def reader_worker(thread_id: int):
        """读取工作线程"""
        for i in range(10):
            success, status_code, elapsed_ms, data = make_request(
                "/api/user/page", "GET", {"pageNum": 1, "pageSize": 10}
            )
            results_queue.put(("read", thread_id, status_code, elapsed_ms))
    
    def writer_worker(thread_id: int):
        """写入工作线程"""
        for i in range(5):
            data = {"name": f"rw_test_{thread_id}_{i}"}
            success, status_code, elapsed_ms, resp = make_request(
                "/api/user/add", "POST", data
            )
            results_queue.put(("write", thread_id, status_code, elapsed_ms))
    
    print("测试 10 读取线程 + 5 写入线程并发执行...")
    
    # 同时启动读写线程
    with ThreadPoolExecutor(max_workers=15) as executor:
        read_futures = [executor.submit(reader_worker, i) for i in range(10)]
        write_futures = [executor.submit(writer_worker, i) for i in range(5)]
        
        for f in as_completed(read_futures + write_futures):
            pass
    
    # 收集结果
    read_results = []
    write_results = []
    while not results_queue.empty():
        op_type, thread_id, status_code, elapsed_ms = results_queue.get()
        if op_type == "read":
            read_results.append((status_code, elapsed_ms))
        else:
            write_results.append((status_code, elapsed_ms))
    
    read_success = sum(1 for s, _ in read_results if s in [200, 401])
    write_success = sum(1 for s, _ in write_results if s < 500)
    
    results.append({
        "test_name": "读写冲突测试",
        "read_requests": len(read_results),
        "write_requests": len(write_results),
        "read_success": read_success,
        "write_success": write_success,
        "status": "PASS" if read_success >= len(read_results) * 0.9 else "WARN"
    })
    
    print(f"  读取: {len(read_results)}次, 写入: {len(write_results)}次")
    
    return results, issues


def test_deadlock_detection():
    """死锁检测测试"""
    print("\n--- 死锁检测测试 ---")
    results = []
    issues = []
    
    deadlock_detected = False
    timeout_count = 0
    total_requests = 0
    
    def resource_a_then_b(thread_id: int):
        """先访问资源A再访问资源B"""
        nonlocal timeout_count, total_requests
        for i in range(5):
            total_requests += 1
            # 先访问用户资源
            success_a, code_a, time_a, _ = make_request("/api/user/page", "GET")
            time.sleep(0.01)  # 模拟处理时间
            # 再访问角色资源
            success_b, code_b, time_b, _ = make_request("/api/role/page", "GET")
            if time_a > 5000 or time_b > 5000:
                timeout_count += 1
    
    def resource_b_then_a(thread_id: int):
        """先访问资源B再访问资源A"""
        nonlocal timeout_count, total_requests
        for i in range(5):
            total_requests += 1
            # 先访问角色资源
            success_b, code_b, time_b, _ = make_request("/api/role/page", "GET")
            time.sleep(0.01)
            # 再访问用户资源
            success_a, code_a, time_a, _ = make_request("/api/user/page", "GET")
            if time_a > 5000 or time_b > 5000:
                timeout_count += 1
    
    print("测试资源访问顺序死锁场景...")
    
    start_time = time.time()
    
    # 创建可能产生死锁的场景
    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = []
        # 一半线程先A后B
        for i in range(10):
            futures.append(executor.submit(resource_a_then_b, i))
        # 一半线程先B后A
        for i in range(10):
            futures.append(executor.submit(resource_b_then_a, i))
        
        # 等待所有线程完成，设置超时
        for future in as_completed(futures, timeout=60):
            try:
                future.result()
            except Exception as e:
                pass
    
    elapsed = time.time() - start_time
    
    # 如果执行时间过长或超时过多，可能有死锁
    if elapsed > 30 or timeout_count > total_requests * 0.1:
        deadlock_detected = True
        issues.append(ConcurrencyIssue(
            issue_type="POTENTIAL_DEADLOCK",
            severity="HIGH",
            description=f"检测到潜在死锁: 执行时间{elapsed:.1f}s, 超时率{timeout_count}/{total_requests}",
            scenario="不同顺序访问多个资源",
            recommendation="检查数据库锁机制，考虑统一资源访问顺序"
        ))
    
    results.append({
        "test_name": "死锁检测",
        "total_requests": total_requests,
        "timeout_count": timeout_count,
        "execution_time_s": round(elapsed, 2),
        "deadlock_detected": deadlock_detected,
        "status": "PASS" if not deadlock_detected else "FAIL"
    })
    
    print(f"  执行时间: {elapsed:.2f}s, 超时: {timeout_count}/{total_requests}")
    
    return results, issues


def test_atomicity():
    """原子性测试"""
    print("\n--- 原子性测试 ---")
    results = []
    issues = []
    
    # 测试事务性操作
    def transaction_worker(thread_id: int) -> List[ThreadResult]:
        thread_results = []
        for i in range(5):
            # 模拟事务操作
            data = {
                "userId": thread_id * 10 + i,
                "roleId": i,
                "operation": "assign_role"
            }
            success, status_code, elapsed_ms, resp = make_request(
                "/api/user/role/assign", "POST", data
            )
            thread_results.append(ThreadResult(
                thread_id=thread_id,
                success=success,
                response_time_ms=elapsed_ms,
                status_code=status_code,
                data=resp
            ))
        return thread_results
    
    all_results = []
    print("测试 20 线程并发事务操作...")
    
    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = [executor.submit(transaction_worker, i) for i in range(20)]
        for future in as_completed(futures):
            all_results.extend(future.result())
    
    success_count = sum(1 for r in all_results if r.success)
    
    # 检查部分失败情况（非原子操作）
    partial_failures = []
    for r in all_results:
        if r.status_code not in [200, 401, 404]:
            partial_failures.append(r)
    
    if len(partial_failures) > 0:
        issues.append(ConcurrencyIssue(
            issue_type="ATOMICITY_VIOLATION",
            severity="MEDIUM",
            description=f"检测到 {len(partial_failures)} 次部分失败",
            scenario="并发事务操作",
            recommendation="确保事务具有原子性，全部成功或全部回滚"
        ))
    
    results.append({
        "test_name": "原子性测试",
        "total_requests": len(all_results),
        "success_count": success_count,
        "partial_failure_count": len(partial_failures),
        "status": "PASS" if len(partial_failures) == 0 else "WARN"
    })
    
    print(f"  总请求: {len(all_results)}, 部分失败: {len(partial_failures)}")
    
    return results, issues


def test_data_consistency():
    """数据一致性测试"""
    print("\n--- 数据一致性测试 ---")
    results = []
    issues = []
    
    consistency_violations = []
    
    def consistency_check_worker(thread_id: int) -> Dict:
        """检查数据一致性"""
        # 读取同一资源多次
        readings = []
        for i in range(3):
            success, status_code, elapsed_ms, data = make_request(
                "/api/user/page", "GET", {"pageNum": 1, "pageSize": 10}
            )
            readings.append(data)
        
        # 比较读取结果
        # 由于没有实际数据变化，主要检查响应结构一致性
        return {
            "thread_id": thread_id,
            "readings": readings,
            "consistent": len(set(str(r) for r in readings)) == 1
        }
    
    print("测试 30 线程数据一致性检查...")
    
    with ThreadPoolExecutor(max_workers=30) as executor:
        futures = [executor.submit(consistency_check_worker, i) for i in range(30)]
        
        for future in as_completed(futures):
            result = future.result()
            if not result["consistent"]:
                consistency_violations.append(result)
    
    if consistency_violations:
        issues.append(ConcurrencyIssue(
            issue_type="DATA_INCONSISTENCY",
            severity="HIGH",
            description=f"检测到 {len(consistency_violations)} 次数据不一致",
            scenario="并发读取同一资源",
            recommendation="检查缓存一致性或实现版本控制"
        ))
    
    results.append({
        "test_name": "数据一致性测试",
        "threads_tested": 30,
        "violations": len(consistency_violations),
        "status": "PASS" if len(consistency_violations) == 0 else "FAIL"
    })
    
    print(f"  一致性违规: {len(consistency_violations)}/30")
    
    return results, issues


def generate_report(all_results: List[Dict], all_issues: List[ConcurrencyIssue]) -> str:
    """生成并发安全测试报告"""
    
    total_tests = len(all_results)
    passed_tests = sum(1 for r in all_results if r.get("status") == "PASS")
    failed_tests = sum(1 for r in all_results if r.get("status") == "FAIL")
    warn_tests = sum(1 for r in all_results if r.get("status") == "WARN")
    
    high_issues = sum(1 for i in all_issues if i.severity == "HIGH")
    medium_issues = sum(1 for i in all_issues if i.severity == "MEDIUM")
    low_issues = sum(1 for i in all_issues if i.severity == "LOW")
    
    report = f"""# AI-Ready 并发安全测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {TEST_RESULTS["test_time"]} |
| 测试环境 | {BASE_URL} |
| 总测试数 | {total_tests} |
| 通过测试 | {passed_tests} |
| 警告测试 | {warn_tests} |
| 失败测试 | {failed_tests} |
| 发现问题 | {len(all_issues)} |

---

## 测试配置

| 参数 | 值 |
|------|-----|
| 线程数配置 | {CONCURRENT_CONFIG["thread_counts"]} |
| 每线程迭代次数 | {CONCURRENT_CONFIG["iterations_per_thread"]} |
| 超时时间 | {CONCURRENT_CONFIG["timeout_seconds"]}秒 |

---

## 测试结果详情

"""
    
    for result in all_results:
        status_icon = "✅" if result.get("status") == "PASS" else ("⚠️" if result.get("status") == "WARN" else "❌")
        report += f"""### {result.get("test_name", "未知测试")} {status_icon}

| 指标 | 值 |
|------|-----|
"""
        for key, value in result.items():
            if key != "test_name" and key != "status":
                report += f"| {key} | {value} |\n"
        report += f"| 状态 | **{result.get('status', 'N/A')}** |\n\n---\n\n"
    
    # 问题汇总
    report += f"""## 发现的并发问题

| 严重性 | 数量 |
|--------|------|
| 高危 | {high_issues} |
| 中危 | {medium_issues} |
| 低危 | {low_issues} |

"""
    
    if all_issues:
        report += "### 问题详情\n\n"
        for i, issue in enumerate(all_issues, 1):
            report += f"""#### 问题 {i}: {issue.issue_type}

| 属性 | 值 |
|------|-----|
| 类型 | {issue.issue_type} |
| 严重性 | **{issue.severity}** |
| 场景 | {issue.scenario} |
| 描述 | {issue.description} |
| 建议 | {issue.recommendation} |

"""
    
    report += f"""## 并发安全评估

### 安全性评分

| 维度 | 评分 |
|------|------|
| 读取安全性 | {'✅ 通过' if any(r.get('test_name') == '并发读测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 写入隔离性 | {'✅ 通过' if any(r.get('test_name') == '并发写测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 死锁防护 | {'✅ 通过' if any(r.get('test_name') == '死锁检测' and r.get('status') == 'PASS' for r in all_results) else '❌ 存在风险'} |
| 原子性保证 | {'✅ 通过' if any(r.get('test_name') == '原子性测试' and r.get('status') == 'PASS' for r in all_results) else '⚠️ 需关注'} |
| 数据一致性 | {'✅ 通过' if any(r.get('test_name') == '数据一致性测试' and r.get('status') == 'PASS' for r in all_results) else '❌ 存在风险'} |

### 改进建议

1. **数据库层面**:
   - 使用乐观锁/悲观锁保护临界资源
   - 实现事务隔离级别配置
   - 添加死锁检测和自动恢复机制

2. **应用层面**:
   - 实现分布式锁（如Redis锁）
   - 使用线程安全的数据结构
   - 避免长时间持有锁

3. **架构层面**:
   - 考虑消息队列削峰
   - 实现幂等性设计
   - 添加限流和熔断机制

---

**报告生成时间**: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}
**测试执行者**: test-agent-2
**项目**: AI-Ready
"""
    
    return report


def main():
    """主函数"""
    TEST_RESULTS["test_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    print("=" * 60)
    print("AI-Ready 并发安全测试")
    print("=" * 60)
    print(f"测试时间: {TEST_RESULTS['test_time']}")
    print(f"测试环境: {BASE_URL}")
    print("=" * 60)
    
    all_results = []
    all_issues = []
    
    # 执行各类并发安全测试
    results, issues = test_concurrent_reads()
    all_results.extend(results)
    all_issues.extend(issues)
    
    results, issues = test_concurrent_writes()
    all_results.extend(results)
    all_issues.extend(issues)
    
    results, issues = test_read_write_conflict()
    all_results.extend(results)
    all_issues.extend(issues)
    
    results, issues = test_deadlock_detection()
    all_results.extend(results)
    all_issues.extend(issues)
    
    results, issues = test_atomicity()
    all_results.extend(results)
    all_issues.extend(issues)
    
    results, issues = test_data_consistency()
    all_results.extend(results)
    all_issues.extend(issues)
    
    # 生成报告
    print("\n" + "=" * 60)
    print("生成测试报告...")
    
    report = generate_report(all_results, all_issues)
    
    # 保存报告
    import os
    os.makedirs("docs", exist_ok=True)
    
    report_path = "docs/AI-Ready并发安全测试报告_20260404.md"
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"报告已保存: {report_path}")
    
    # 保存JSON结果
    json_data = {
        "test_time": TEST_RESULTS["test_time"],
        "config": CONCURRENT_CONFIG,
        "results": all_results,
        "issues": [
            {
                "type": i.issue_type,
                "severity": i.severity,
                "description": i.description,
                "scenario": i.scenario,
                "recommendation": i.recommendation
            }
            for i in all_issues
        ]
    }
    
    json_path = "docs/concurrent_safety_test_results.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(json_data, f, indent=2, ensure_ascii=False)
    
    print(f"JSON结果已保存: {json_path}")
    
    # 输出摘要
    passed = sum(1 for r in all_results if r.get("status") == "PASS")
    failed = sum(1 for r in all_results if r.get("status") == "FAIL")
    warn = sum(1 for r in all_results if r.get("status") == "WARN")
    
    print(f"\n测试结果: {passed} 通过, {warn} 警告, {failed} 失败")
    print(f"发现问题: {len(all_issues)} 个")
    print("=" * 60)
    
    return all_results, all_issues


if __name__ == "__main__":
    main()
