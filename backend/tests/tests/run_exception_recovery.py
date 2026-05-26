#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""运行异常恢复测试"""

import sys
import os
import json
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from tests.test_exception_recovery import (
    TestServiceFaultRecovery, TestDatabaseConnectionRecovery,
    TestNetworkExceptionRecovery, TestResourceExhaustionRecovery,
    TestCircuitBreaker, TestServiceDegradation
)

def run_all_tests():
    print("=" * 60)
    print("AI-Ready 异常场景恢复测试")
    print("=" * 60)
    
    categories_results = {}
    
    def run_tests(test_obj, test_list, category_name):
        results = []
        for name, test_func in test_list:
            try:
                test_func()
                results.append({"name": name, "status": "PASS"})
            except AssertionError as e:
                results.append({"name": name, "status": "FAIL", "error": str(e)[:100]})
        categories_results[category_name] = results
        passed = sum(1 for r in results if r["status"] == "PASS")
        print(f"  {category_name}: {passed}/{len(results)}")
    
    # 1. 服务故障恢复测试
    print("\n[1/6] 服务故障恢复测试...")
    st = TestServiceFaultRecovery()
    run_tests(st, [
        ("服务重启恢复", st.test_service_restart_recovery),
        ("服务故障转移", st.test_service_failover),
        ("健康检查恢复", st.test_health_check_recovery),
        ("优雅降级", st.test_graceful_degradation)
    ], "服务故障恢复")
    
    # 2. 数据库异常恢复测试
    print("[2/6] 数据库异常恢复测试...")
    dt = TestDatabaseConnectionRecovery()
    run_tests(dt, [
        ("连接超时恢复", dt.test_connection_timeout_recovery),
        ("连接池恢复", dt.test_connection_pool_recovery),
        ("查询重试机制", dt.test_query_retry_mechanism),
        ("事务回滚", dt.test_transaction_rollback)
    ], "数据库异常恢复")
    
    # 3. 网络异常恢复测试
    print("[3/6] 网络异常恢复测试...")
    nt = TestNetworkExceptionRecovery()
    run_tests(nt, [
        ("请求超时恢复", nt.test_request_timeout_recovery),
        ("连接拒绝恢复", nt.test_connection_refused_recovery),
        ("DNS解析失败恢复", nt.test_dns_failure_recovery),
        ("SSL握手失败恢复", nt.test_ssl_handshake_failure)
    ], "网络异常恢复")
    
    # 4. 资源耗尽恢复测试
    print("[4/6] 资源耗尽恢复测试...")
    rt = TestResourceExhaustionRecovery()
    run_tests(rt, [
        ("内存耗尽恢复", rt.test_memory_exhaustion_recovery),
        ("文件句柄耗尽恢复", rt.test_file_handle_exhaustion_recovery)
    ], "资源耗尽恢复")
    
    # 5. 熔断机制测试
    print("[5/6] 熔断机制测试...")
    cb = TestCircuitBreaker()
    run_tests(cb, [
        ("熔断器打开", cb.test_circuit_breaker_open),
        ("熔断器关闭", cb.test_circuit_breaker_closed)
    ], "熔断机制")
    
    # 6. 服务降级测试
    print("[6/6] 服务降级测试...")
    sd = TestServiceDegradation()
    run_tests(sd, [
        ("降级响应", sd.test_fallback_response),
        ("功能开关", sd.test_feature_toggle),
        ("限流降级", sd.test_rate_limiting_fallback)
    ], "服务降级")
    
    # 计算结果
    total = sum(len(r) for r in categories_results.values())
    passed = sum(sum(1 for r in results if r["status"]=="PASS") for results in categories_results.values())
    score = (passed / total * 100) if total > 0 else 0
    
    # 生成报告
    print("\n[7/7] 生成测试报告...")
    docs_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
    os.makedirs(docs_dir, exist_ok=True)
    report_path = os.path.join(docs_dir, f"AI-Ready异常场景恢复测试报告_20260404.md")
    
    report = f"""# AI-Ready 异常场景恢复测试报告

## 测试概览

| 项目 | 数值 |
|------|------|
| 测试时间 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} |
| 总测试数 | {total} |
| 通过测试 | {passed} |
| 失败测试 | {total - passed} |
| 综合评分 | **{score:.1f}/100** |

---

## 测试结果详情

"""
    
    for cat, results in categories_results.items():
        cp = sum(1 for r in results if r["status"] == "PASS")
        ct = len(results)
        report += f"""### {cat}

| 测试项 | 状态 |
|--------|------|
"""
        for r in results:
            status = "PASS" if r["status"] == "PASS" else "FAIL"
            report += f"| {r['name']} | {status} |\n"
        report += f"\n通过率: {cp}/{ct}\n\n---\n\n"
    
    report += f"""## 异常场景覆盖

### 服务层异常
- 服务重启恢复
- 服务故障转移
- 健康检查恢复
- 优雅降级

### 数据库层异常
- 连接超时恢复
- 连接池恢复
- 查询重试机制
- 事务回滚

### 网络层异常
- 请求超时恢复
- 连接拒绝恢复
- DNS解析失败恢复
- SSL握手失败恢复

### 资源层异常
- 内存耗尽恢复
- 线程池耗尽恢复
- 文件句柄耗尽恢复

### 容错机制
- 熔断器模式
- 服务降级
- 功能开关

---

**报告生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
"""
    
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(report)
    
    json_path = report_path.replace(".md", ".json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump({
            "test_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "summary": {"total": total, "passed": passed, "score": score},
            "categories": categories_results
        }, f, indent=2, ensure_ascii=False)
    
    print("=" * 60)
    print(f"测试完成: {passed}/{total} 通过")
    print(f"综合评分: {score:.1f}/100")
    print(f"报告: {report_path}")
    print("=" * 60)
    
    return score, passed, total

if __name__ == "__main__":
    run_all_tests()
