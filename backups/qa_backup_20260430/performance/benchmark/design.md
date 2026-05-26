# 测试环境性能基准测试设计

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: test-agent-2  
**最后更新**: 2026-04-27

---

## 目录

1. [概述](#概述)
2. [性能测试范围](#性能测试范围)
3. [性能测试场景](#性能测试场景)
4. [性能测试用例](#性能测试用例)
5. [测试数据准备方案](#测试数据准备方案)
6. [测试脚本设计方案](#测试脚本设计方案)
7. [性能基准指标](#性能基准指标)
8. [测试执行计划](#测试执行计划)
9. [风险与应对](#风险与应对)

---

## 概述

### 测试目标

为Sprint 27+1测试环境建立完整的性能基准数据，为后续性能优化提供基准参考。

### 测试范围

本次性能测试覆盖以下4个核心服务：

| 服务名称 | 服务端口 | 测试重点 |
|---------|---------|---------|
| 用户管理服务 | 8081 | 认证、查询、批量操作 |
| 订单管理服务 | 8082 | 订单创建、查询、修改 |
| 库存管理服务 | 8083 | 库存查询、更新、锁定 |
| API网关服务 | 8080 | 路由、认证、限流 |

### 测试环境

- **环境名称**: Sprint 27+1测试环境
- **基础URL**: http://test-ai-ready.example.com
- **测试工具**: JMeter / Python requests / curl
- **监控工具**: Prometheus + Grafana

---

## 性能测试场景

### 场景1: 用户管理服务性能测试

| 场景名称 | 测试目的 | 并发用户 | 请求次数 | 预期指标 |
|---------|---------|---------|---------|---------|
| 用户登录性能 | 验证认证接口性能 | 100 | 1000 | P95 < 500ms, QPS ≥ 200 |
| 用户查询性能 | 验证分页查询性能 | 50 | 500 | P95 < 500ms, QPS ≥ 150 |
| 批量操作性能 | 验证批量删除性能 | 20 | 100 | P95 < 1000ms, QPS ≥ 50 |

### 场景2: 订单管理服务性能测试

| 场景名称 | 测试目的 | 并发用户 | 请求次数 | 预期指标 |
|---------|---------|---------|---------|---------|
| 订单创建性能 | 验证订单创建性能 | 50 | 500 | P95 < 500ms, QPS ≥ 100 |
| 订单查询性能 | 验证订单查询性能 | 30 | 300 | P95 < 500ms, QPS ≥ 80 |
| 订单修改性能 | 验证订单修改性能 | 20 | 200 | P95 < 500ms, QPS ≥ 60 |

### 场景3: 库存管理服务性能测试

| 场景名称 | 测试目的 | 并发用户 | 请求次数 | 预期指标 |
|---------|---------|---------|---------|---------|
| 库存查询性能 | 验证库存查询性能 | 50 | 500 | P95 < 300ms, QPS ≥ 150 |
| 库存更新性能 | 验证库存更新性能 | 30 | 300 | P95 < 500ms, QPS ≥ 100 |
| 库存锁定性能 | 验证库存锁定性能 | 20 | 200 | P95 < 400ms, QPS ≥ 80 |

### 场景4: API网关性能测试

| 场景名称 | 测试目的 | 并发用户 | 请求次数 | 预期指标 |
|---------|---------|---------|---------|---------|
| 路由性能 | 验证API路由性能 | 200 | 2000 | P95 < 100ms, QPS ≥ 500 |
| 认证性能 | 验证JWT验证性能 | 100 | 1000 | P95 < 50ms, QPS ≥ 300 |
| 限流性能 | 验证限流策略性能 | 500 | 5000 | 限流生效正确 |

---

## 性能测试用例

### 认证管理测试用例

| 用例ID | 测试用例 | 并发用户 | 请求次数 | 预期P95 | 预期QPS | 状态 |
|-------|---------|---------|---------|---------|---------|------|
| TP-AUTH-001 | 用户登录 | 100 | 1000 | 320ms | 200 | 待执行 |
| TP-AUTH-002 | 用户登出 | 50 | 500 | 150ms | 150 | 待执行 |
| TP-AUTH-003 | JWT验证 | 200 | 2000 | 50ms | 300 | 待执行 |

### 用户管理测试用例

| 用例ID | 测试用例 | 并发用户 | 请求次数 | 预期P95 | 预期QPS | 状态 |
|-------|---------|---------|---------|---------|---------|------|
| TP-USER-001 | 分页查询用户(10条) | 50 | 500 | 250ms | 150 | 待执行 |
| TP-USER-002 | 分页查询用户(100条) | 30 | 300 | 500ms | 100 | 待执行 |
| TP-USER-003 | 创建用户 | 20 | 200 | 300ms | 80 | 待执行 |
| TP-USER-004 | 批量删除用户(100个) | 10 | 100 | 800ms | 50 | 待执行 |

### 订单管理测试用例

| 用例ID | 测试用例 | 并发用户 | 请求次数 | 预期P95 | 预期QPS | 状态 |
|-------|---------|---------|---------|---------|---------|------|
| TP-ORDER-001 | 创建订单 | 50 | 500 | 400ms | 100 | 待执行 |
| TP-ORDER-002 | 查询订单 | 30 | 300 | 300ms | 80 | 待执行 |
| TP-ORDER-003 | 修改订单 | 20 | 200 | 350ms | 60 | 待执行 |
| TP-ORDER-004 | 查询订单列表 | 20 | 200 | 400ms | 50 | 待执行 |

### 库存管理测试用例

| 用例ID | 测试用例 | 并发用户 | 请求次数 | 预期P95 | 预期QPS | 状态 |
|-------|---------|---------|---------|---------|---------|------|
| TP-STOCK-001 | 查询库存 | 50 | 500 | 200ms | 150 | 待执行 |
| TP-STOCK-002 | 更新库存 | 30 | 300 | 300ms | 100 | 待执行 |
| TP-STOCK-003 | 库存锁定 | 20 | 200 | 250ms | 80 | 待执行 |
| TP-STOCK-004 | 库存释放 | 20 | 200 | 250ms | 80 | 待执行 |

### API网关测试用例

| 用例ID | 测试用例 | 并发用户 | 请求次数 | 预期P95 | 预期QPS | 状态 |
|-------|---------|---------|---------|---------|---------|------|
| TP-GW-001 | API路由 | 200 | 2000 | 80ms | 500 | 待执行 |
| TP-GW-002 | JWT认证 | 100 | 1000 | 40ms | 300 | 待执行 |
| TP-GW-003 | 限流测试 | 500 | 5000 | N/A | 限流生效 | 待执行 |

---

## 测试数据准备方案

### 1. 用户数据准备

```sql
-- 管理员用户
INSERT INTO sys_user (username, password, real_name, email, phone, status, gender, dept_id, create_time) 
VALUES ('admin', '加密密码', '管理员', 'admin@example.com', '13800138000', 1, 1, 1, NOW());

-- 测试用户（批量生成10000个）
-- 使用测试数据工厂生成
```

### 2. 订单数据准备

```sql
-- 待处理订单（1000个）
INSERT INTO order (order_no, user_id, total_amount, status, create_time) 
SELECT 
  CONCAT('ORD', LPAD(seq, 8, '0')),
  FLOOR(RAND() * 10000) + 1,
  ROUND(RAND() * 1000, 2),
  'PENDING',
  NOW()
FROM (SELECT @row := @row + 1 as seq FROM information_schema.columns, (SELECT @row := 0) r LIMIT 1000) t;
```

### 3. 库存数据准备

```sql
-- 库存记录（10000个SKU）
INSERT INTO stock (sku_id, quantity, reserved_quantity, version) 
SELECT 
  LPAD(seq, 8, '0'),
  FLOOR(RAND() * 1000),
  0,
  0
FROM (SELECT @row := @row + 1 as seq FROM information_schema.columns, (SELECT @row := 0) r LIMIT 10000) t;
```

### 4. 测试数据管理

| 数据类型 | 准备方式 | 清理方式 | 轮换周期 |
|---------|---------|---------|---------|
| 用户数据 | 批量生成 | 删除测试用户 | 每次测试 |
| 订单数据 | 批量生成 | 删除测试订单 | 每次测试 |
| 库存数据 | 批量生成 | 重置库存 | 每次测试 |
| JWT Token | 动态生成 | 不需要清理 | 每次请求 |

---

## 测试脚本设计方案

### 1. JMeter脚本设计

#### 用户登录脚本 (login.jmx)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="用户登录性能测试">
      <elementProp name="TestPlan.user_defined_variables">
        <elementProp name="BASE_URL" elementPropType="HTTPSamplerProxy" testname="BASE_URL">
          <stringProp name="HTTPSampler.path"/>
          <stringProp name="HTTPSampler.domain">test-ai-ready.example.com</stringProp>
          <stringProp name="HTTPSampler.port">80</stringProp>
          <stringProp name="HTTPSampler.protocol">http</stringProp>
        </elementProp>
      </elementProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="登录线程组">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="用户登录">
          <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
          <stringProp name="HTTPSampler.port">80</stringProp>
          <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <elementProp name="HTTPsampler.Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="username" elementPropType="HTTPArgument">
                <stringProp name="Argument.value">admin</stringProp>
              </elementProp>
              <elementProp name="password" elementPropType="HTTPArgument">
                <stringProp name="Argument.value">[REDACTED]</stringProp>
              </elementProp>
            </collectionProp>
          </elementProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

#### Python脚本设计 (test_user_management.py)

```python
#!/usr/bin/env python
# -*- coding: utf-8 -*-

import requests
import time
import concurrent.futures
import json

BASE_URL = "http://test-ai-ready.example.com"

def login():
    """用户登录"""
    url = f"{BASE_URL}/api/auth/login"
    payload = {
        "username": "admin",
        "password": "[REDACTED]"
    }
    headers = {"Content-Type": "application/json"}
    
    start_time = time.time()
    response = requests.post(url, json=payload, headers=headers)
    elapsed = time.time() - start_time
    
    return {
        "status_code": response.status_code,
        "elapsed_ms": elapsed * 1000,
        "data": response.json() if response.status_code == 200 else None
    }

def test_login_performance(concurrency=100, iterations=1000):
    """测试登录性能"""
    results = []
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(login) for _ in range(iterations)]
        
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())
    
    # 计算性能指标
    elapsed_times = [r["elapsed_ms"] for r in results if r["status_code"] == 200]
    
    print(f"完成次数: {len(elapsed_times)}/{iterations}")
    print(f"平均响应时间: {sum(elapsed_times)/len(elapsed_times):.2f}ms")
    print(f"P95响应时间: {sorted(elapsed_times)[int(len(elapsed_times)*0.95)]:.2f}ms")
    print(f"QPS: {len(elapsed_times)/300:.2f}")  # 假设总时长300秒

if __name__ == "__main__":
    test_login_performance()
```

### 2. 脚本目录结构

```
qa/performance/benchmark/scripts/
├── login.jmx                        # 用户登录性能测试(JMeter)
├── user-management.jmx              # 用户管理性能测试(JMeter)
├── order-management.jmx             # 订单管理性能测试(JMeter)
├── stock-management.jmx             # 库存管理性能测试(JMeter)
├── api-gateway.jmx                  # API网关性能测试(JMeter)
├── test_user_management.py          # 用户管理性能测试(Python)
├── test_order_management.py         # 订单管理性能测试(Python)
├── test_stock_management.py         # 库存管理性能测试(Python)
├── test_api_gateway.py              # API网关性能测试(Python)
├── run_all_benchmarks.sh            # 一键运行所有基准测试
└── analyze_results.sh               # 分析测试结果
```

---

## 性能基准指标

### 1. 响应时间指标

| 服务 | 接口 | P50 | P95 | P99 | 超时阈值 |
|-----|-----|-----|-----|-----|---------|
| 用户管理 | 登录 | 100ms | 320ms | 500ms | 1000ms |
| 用户管理 | 查询 | 200ms | 400ms | 650ms | 1000ms |
| 用户管理 | 批量操作 | 500ms | 800ms | 1500ms | 2000ms |
| 订单管理 | 创建 | 200ms | 400ms | 600ms | 1000ms |
| 订单管理 | 查询 | 150ms | 300ms | 500ms | 1000ms |
| 订单管理 | 修改 | 200ms | 400ms | 600ms | 1000ms |
| 库存管理 | 查询 | 100ms | 200ms | 300ms | 500ms |
| 库存管理 | 更新 | 150ms | 300ms | 500ms | 1000ms |
| 库存管理 | 锁定 | 100ms | 250ms | 400ms | 600ms |
| API网关 | 路由 | 20ms | 80ms | 150ms | 300ms |
| API网关 | 认证 | 10ms | 40ms | 80ms | 200ms |

### 2. 吞吐量指标

| 服务 | 场景 | QPS | 并发用户 | 错误率阈值 |
|-----|-----|-----|---------|---------|
| 用户管理 | 登录 | ≥200 | 100 | <1% |
| 用户管理 | 查询 | ≥150 | 50 | <1% |
| 用户管理 | 批量操作 | ≥50 | 20 | <2% |
| 订单管理 | 创建 | ≥100 | 50 | <1% |
| 订单管理 | 查询 | ≥80 | 30 | <1% |
| 订单管理 | 修改 | ≥60 | 20 | <2% |
| 库存管理 | 查询 | ≥150 | 50 | <1% |
| 库存管理 | 更新 | ≥100 | 30 | <1% |
| 库存管理 | 锁定 | ≥80 | 20 | <2% |
| API网关 | 路由 | ≥500 | 200 | <0.5% |
| API网关 | 认证 | ≥300 | 100 | <1% |

### 3. 系统资源指标

| 资源项 | CPU使用率 | 内存使用率 | 磁盘I/O | 网络I/O |
|-------|----------|----------|--------|--------|
| 用户管理服务 | ≤70% | ≤80% | ≤50% | ≤60% |
| 订单管理服务 | ≤70% | ≤80% | ≤50% | ≤60% |
| 库存管理服务 | ≤70% | ≤80% | ≤50% | ≤60% |
| API网关服务 | ≤60% | ≤70% | ≤30% | ≤50% |
| 数据库服务 | ≤80% | ≤85% | ≤70% | ≤60% |

---

## 测试执行计划

### 阶段1: 准备阶段（1小时）

| 时间 | 任务 | 负责人 | 交付物 |
|-----|-----|-------|-------|
| 0-15分钟 | 环境检查 | test-agent-2 | 环境检查报告 |
| 15-30分钟 | 数据准备 | test-agent-2 | 测试数据脚本 |
| 30-60分钟 | 脚本准备 | test-agent-2 | JMeter/Python脚本 |

### 阶段2: 执行阶段（2小时）

| 时间 | 测试场景 | 并发用户 | 预计时长 |
|-----|---------|---------|---------|
| 0-30分钟 | 用户管理服务 | 100 | 30分钟 |
| 30-60分钟 | 订单管理服务 | 50 | 30分钟 |
| 60-90分钟 | 库存管理服务 | 50 | 30分钟 |
| 90-120分钟 | API网关服务 | 200 | 30分钟 |

### 阶段3: 分析阶段（1.5小时）

| 时间 | 任务 | 负责人 | 交付物 |
|-----|-----|-------|-------|
| 0-30分钟 | 数据收集 | test-agent-2 | 性能数据集 |
| 30-90分钟 | 性能分析 | test-agent-2 | 性能瓶颈报告 |
| 90-120分钟 | 优化建议 | test-agent-2 | 性能优化建议 |

### 阶段4: 报告阶段（1小时）

| 时间 | 任务 | 负责人 | 交付物 |
|-----|-----|-------|-------|
| 0-30分钟 | 生成报告 | test-agent-2 | 性能基准报告 |
| 30-60分钟 | 报告评审 | test-agent-2 | 评审通过报告 |

---

## 风险与应对

### 1. 测试环境不可用

| 风险描述 | 概率 | 影响 | 应对措施 |
|---------|-----|------|---------|
| 服务未部署 | 高 | 高 | 提前检查服务状态，准备回滚方案 |
| 服务崩溃 | 中 | 高 | 准备快速重启脚本，记录重启日志 |
| 网络问题 | 低 | 中 | 准备本地测试方案，使用curl测试 |

### 2. 测试数据问题

| 风险描述 | 概率 | 影响 | 应对措施 |
|---------|-----|------|---------|
| 数据不足 | 中 | 中 | 准备数据生成脚本，批量生成测试数据 |
| 数据污染 | 低 | 高 | 每次测试前清空测试数据，测试后验证 |

### 3. 测试脚本问题

| 风险描述 | 概率 | 影响 | 应对措施 |
|---------|-----|------|---------|
| 脚本错误 | 中 | 中 | 先执行小规模预测试，验证脚本正确性 |
| 性能瓶颈 | 低 | 高 | 使用多个测试节点负载均衡，监控测试工具资源 |

### 4. 结果分析问题

| 风险描述 | 概率 | 影响 | 应对措施 |
|---------|-----|------|---------|
| 数据不准确 | 中 | 中 | 多次测试取平均值，验证数据一致性 |
| 分析错误 | 低 | 高 | 使用自动化分析工具，交叉验证结果 |

---

## 附录

### 附录A: 性能测试 checklist

```bash
# 环境检查
- [ ] 所有服务已部署并健康
- [ ] 测试数据库已准备
- [ ] 网络连接正常

# 脚本检查
- [ ] JMeter脚本语法正确
- [ ] Python脚本语法正确
- [ ] 测试数据生成脚本正确

# 执行检查
- [ ] 监控工具已启动
- [ ] 日志收集正常
- [ ] 性能数据存储就绪

# 分析检查
- [ ] 数据完整
- [ ] 分析方法正确
- [ ] 结论合理
```

### 附录B: 性能优化checklist

```bash
# 数据库优化
- [ ] 索引优化
- [ ] 查询优化
- [ ] 连接池配置

# 应用优化
- [ ] 缓存策略
- [ ] 异步处理
- [ ] 并发控制

# 网络优化
- [ ] 连接复用
- [ ] 压缩传输
- [ ] CDN加速
```

---

**最后更新**: 2026-04-27  
**文档版本**: 1.0.0  
**项目**: AI-Ready Sprint 27+1