# 企智连性能测试方案

## 文档信息
- **项目**: 企智连 (AI-Ready)
- **版本**: v1.0
- **编写日期**: 2026-04-15
- **测试负责人**: test-agent-1

---

## 1. 测试概述

### 1.1 测试目标
验证企智连系统在预期负载下的性能表现，确保系统满足以下性能基准：
- **响应时间**: < 2秒 (P95)
- **并发支持**: 1000+ 用户
- **吞吐量**: ≥ 500 TPS
- **错误率**: < 0.1%

### 1.2 测试范围
| 模块 | 测试类型 | 优先级 |
|------|----------|--------|
| 订单管理 | 业务流程性能 | P0 |
| 库存管理 | 查询性能 | P0 |
| 报表系统 | 批量数据处理 | P1 |
| 用户认证 | 并发登录 | P0 |
| 数据库 | 查询优化 | P0 |
| API接口 | 响应时间 | P0 |

### 1.3 测试工具
- **JMeter 5.6+**: 主要性能测试工具
- **Java 11+**: 自定义测试插件
- **MySQL 8.0**: 数据库性能测试
- **Prometheus + Grafana**: 系统资源监控

---

## 2. 核心业务流程性能测试

### 2.1 订单创建流程测试

#### 测试用例 TC-PF-001: 订单创建性能
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-PF-001 |
| 用例名称 | 订单创建流程性能测试 |
| 测试类型 | 业务流程性能 |
| 优先级 | P0 |
| 预置条件 | 用户已登录，库存充足 |

**测试步骤**:
1. 发起创建订单请求
2. 验证订单创建成功
3. 记录响应时间
4. 重复执行 100 次

**性能指标**:
| 指标 | 目标值 | 测试方法 |
|------|--------|----------|
| 平均响应时间 | < 500ms | JMeter Summary Report |
| P95响应时间 | < 1000ms | JMeter Aggregate Report |
| P99响应时间 | < 2000ms | JMeter Aggregate Report |
| 成功率 | 100% | JMeter Summary Report |

**JMeter配置**:
```xml
<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="订单创建测试">
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">10</stringProp>
  <boolProp name="ThreadGroup.scheduler">true</boolProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>
```

---

#### 测试用例 TC-PF-002: 批量订单创建
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-PF-002 |
| 用例名称 | 批量订单创建性能测试 |
| 测试类型 | 批量数据处理 |
| 优先级 | P1 |
| 预置条件 | 用户已登录，库存充足 |

**测试步骤**:
1. 创建包含 50 个商品的订单
2. 验证订单创建成功
3. 记录响应时间
4. 并发执行 200 用户

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 2000ms |
| P95响应时间 | < 3000ms |
| 成功率 | ≥ 99% |

---

### 2.2 库存查询流程测试

#### 测试用例 TC-PF-003: 单商品库存查询
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-PF-003 |
| 用例名称 | 单商品库存查询性能测试 |
| 测试类型 | 查询性能 |
| 优先级 | P0 |
| 预置条件 | 商品数据已预置 |

**测试步骤**:
1. 发起商品库存查询请求
2. 验证返回结果正确
3. 记录响应时间
4. 并发 500 用户执行

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 100ms |
| P95响应时间 | < 200ms |
| 吞吐量 | ≥ 1000 TPS |

**JMeter配置**:
```xml
<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="库存查询测试">
  <stringProp name="ThreadGroup.num_threads">500</stringProp>
  <stringProp name="ThreadGroup.ramp_time">30</stringProp>
  <boolProp name="ThreadGroup.scheduler">true</boolProp>
  <stringProp name="ThreadGroup.duration">600</stringProp>
</ThreadGroup>
```

---

#### 测试用例 TC-PF-004: 批量库存查询
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-PF-004 |
| 用例名称 | 批量库存查询性能测试 |
| 测试类型 | 批量查询性能 |
| 优先级 | P1 |
| 预置条件 | 100个商品数据已预置 |

**测试步骤**:
1. 发起批量库存查询请求（100商品）
2. 验证返回结果完整
3. 记录响应时间
4. 并发 100 用户执行

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 500ms |
| P95响应时间 | < 1000ms |
| 吞吞吐量 | ≥ 200 TPS |

---

### 2.3 报表生成流程测试

#### 测试用例 TC-PF-005: 日报表生成
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-PF-005 |
| 用例名称 | 日报表生成性能测试 |
| 测试类型 | 报表性能 |
| 优先级 | P1 |
| 预置条件 | 当日订单数据已存在 |

**测试步骤**:
1. 发起日报表生成请求
2. 验证报表数据正确
3. 记录响应时间
4. 执行 50 次

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 3000ms |
| P95响应时间 | < 5000ms |
| 报表完整率 | 100% |

---

#### 测试用例 TC-PF-006: 月度报表生成
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-PF-006 |
| 用例名称 | 月度报表生成性能测试 |
| 测试类型 | 大数据报表性能 |
| 优先级 | P1 |
| 预置条件 | 月度订单数据约10万条 |

**测试步骤**:
1. 发起月度报表生成请求
2. 验证报表数据完整
3. 记录响应时间
4. 执行 20 次

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 10000ms |
| P95响应时间 | < 15000ms |
| 内存占用 | < 2GB |

---

## 3. 并发用户场景测试

### 3.1 登录并发测试

#### 测试用例 TC-CON-001: 用户并发登录
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-CON-001 |
| 用例名称 | 1000用户并发登录测试 |
| 测试类型 | 并发登录 |
| 优先级 | P0 |
| 预置条件 | 用户账号已预置1000个 |

**测试步骤**:
1. 1000用户同时发起登录请求
2. 验证登录成功
3. 记录响应时间分布
4. 持续执行5分钟

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 500ms |
| P95响应时间 | < 1000ms |
| P99响应时间 | < 2000ms |
| 登录成功率 | ≥ 99.9% |
| 最大并发数 | 1000 |

**JMeter配置**:
```xml
<ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="并发登录测试">
  <stringProp name="ThreadGroup.num_threads">1000</stringProp>
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>
  <boolProp name="ThreadGroup.scheduler">true</boolProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>
```

---

### 3.2 业务并发测试

#### 测试用例 TC-CON-002: 订单创建并发测试
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-CON-002 |
| 用例名称 | 500用户并发创建订单 |
| 测试类型 | 业务并发 |
| 优先级 | P0 |
| 预置条件 | 用户已登录，库存充足 |

**测试步骤**:
1. 500用户同时发起订单创建请求
2. 验证订单创建无冲突
3. 记录响应时间和错误率
4. 持续执行10分钟

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 平均响应时间 | < 1000ms |
| P95响应时间 | < 2000ms |
| 错误率 | < 0.1% |
| 数据一致性 | 100% |

---

#### 测试用例 TC-CON-003: 混合业务并发测试
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-CON-003 |
| 用例名称 | 混合业务并发测试 |
| 测试类型 | 综合并发 |
| 优先级 | P0 |
| 预置条件 | 系统正常运行 |

**业务比例配置**:
| 业务类型 | 占比 | 并发数 |
|----------|------|--------|
| 商品查询 | 40% | 400 |
| 库存查询 | 30% | 300 |
| 订单创建 | 20% | 200 |
| 用户登录 | 10% | 100 |

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 总吞吐量 | ≥ 500 TPS |
| 平均响应时间 | < 1000ms |
| 错误率 | < 0.5% |
| CPU使用率 | < 80% |
| 内存使用率 | < 70% |

---

### 3.3 压力测试

#### 测试用例 TC-CON-004: 系统压力极限测试
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-CON-004 |
| 用例名称 | 系统压力极限测试 |
| 测试类型 | 压力测试 |
| 优先级 | P1 |
| 预置条件 | 系统正常运行 |

**测试步骤**:
1. 从500用户开始，每分钟增加100用户
2. 持续监控系统指标
3. 记录系统崩溃点
4. 分析瓶颈原因

**性能指标**:
| 指标 | 目标值 | 告警阈值 |
|------|--------|----------|
| 最大并发数 | 1000+ | - |
| CPU使用率 | < 80% | 90% |
| 内存使用率 | < 70% | 85% |
| 响应时间 | < 2s | 5s |
| 错误率 | < 0.1% | 1% |

---

## 4. 数据库性能测试

### 4.1 查询性能测试

#### 测试用例 TC-DB-001: 单表查询性能
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-DB-001 |
| 用例名称 | 单表查询性能测试 |
| 测试类型 | 数据库查询 |
| 优先级 | P0 |
| 预置条件 | 表数据量100万条 |

**测试SQL**:
```sql
-- 主键查询
SELECT * FROM orders WHERE id = ?;

-- 索引查询
SELECT * FROM orders WHERE user_id = ? AND status = ?;

-- 范围查询
SELECT * FROM orders WHERE create_time BETWEEN ? AND ?;
```

**性能指标**:
| 查询类型 | 目标响应时间 |
|----------|--------------|
| 主键查询 | < 5ms |
| 索引查询 | < 50ms |
| 范围查询 | < 200ms |

---

#### 测试用例 TC-DB-002: 多表关联查询性能
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-DB-002 |
| 用例名称 | 多表关联查询性能测试 |
| 测试类型 | 数据库关联查询 |
| 优先级 | P0 |
| 预置条件 | 表数据量100万条 |

**测试SQL**:
```sql
-- 订单-商品关联查询
SELECT o.*, p.name, p.price
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE o.user_id = ?;

-- 订单-用户关联查询
SELECT o.*, u.name, u.phone
FROM orders o
JOIN users u ON o.user_id = u.id
WHERE o.status = ? AND o.create_time > ?;
```

**性能指标**:
| 查询类型 | 目标响应时间 |
|----------|--------------|
| 两表关联 | < 100ms |
| 三表关联 | < 200ms |
| 四表关联 | < 500ms |

---

### 4.2 索引效果测试

#### 测试用例 TC-DB-003: 索引效果对比测试
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-DB-003 |
| 用例名称 | 索引效果对比测试 |
| 测试类型 | 索引性能 |
| 优先级 | P0 |
| 预置条件 | 表数据量100万条 |

**测试步骤**:
1. 执行无索引查询，记录响应时间
2. 创建索引
3. 执行相同查询，记录响应时间
4. 计算性能提升比例

**测试索引**:
```sql
-- 创建索引
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_orders_create_time ON orders(create_time);
CREATE INDEX idx_order_items_product ON order_items(product_id);
```

**性能指标**:
| 场景 | 无索引 | 有索引 | 提升比例 |
|------|--------|--------|----------|
| user_id查询 | ~500ms | < 50ms | ≥ 10倍 |
| 状态+时间查询 | ~1000ms | < 200ms | ≥ 5倍 |
| 商品统计查询 | ~300ms | < 50ms | ≥ 6倍 |

---

### 4.3 事务性能测试

#### 测试用例 TC-DB-004: 事务提交性能
| 字段 | 值 |
|------|-----|
| 用例编号 | TC-DB-004 |
| 用例名称 | 事务提交性能测试 |
| 测试类型 | 事务性能 |
| 优先级 | P1 |
| 预置条件 | 数据库连接池已配置 |

**测试步骤**:
1. 执行订单创建事务
2. 记录事务提交时间
3. 并发执行100事务
4. 验证数据一致性

**性能指标**:
| 指标 | 目标值 |
|------|--------|
| 单事务时间 | < 100ms |
| 事务吞吐量 | ≥ 100 TPS |
| 事务成功率 | ≥ 99.9% |
| 死锁发生率 | 0 |

---

## 5. API接口响应时间测试

### 5.1 接口性能测试矩阵

| 接口 | 用例编号 | 目标时间 | 并发数 |
|------|----------|----------|--------|
| /api/auth/login | TC-API-001 | < 500ms | 1000 |
| /api/orders/create | TC-API-002 | < 1000ms | 500 |
| /api/orders/list | TC-API-003 | < 200ms | 200 |
| /api/products/query | TC-API-004 | < 100ms | 500 |
| /api/inventory/check | TC-API-005 | < 100ms | 500 |
| /api/reports/daily | TC-API-006 | < 3000ms | 50 |
| /api/reports/monthly | TC-API-007 | < 10000ms | 20 |
| /api/users/profile | TC-API-008 | < 200ms | 300 |
| /api/inventory/update | TC-API-009 | < 500ms | 200 |
| /api/orders/cancel | TC-API-010 | < 500ms | 200 |

---

### 5.2 详细接口测试用例

#### TC-API-001: 登录接口性能
**请求示例**:
```json
POST /api/auth/login
{
  "username": "test_user",
  "password": "encrypted_password"
}
```

**性能指标**:
| 并发数 | 平均响应时间 | P95 | P99 |
|--------|--------------|-----|-----|
| 100 | < 200ms | < 300ms | < 500ms |
| 500 | < 400ms | < 600ms | < 1000ms |
| 1000 | < 500ms | < 800ms | < 1500ms |

---

#### TC-API-002: 订单创建接口性能
**请求示例**:
```json
POST /api/orders/create
{
  "userId": "user_001",
  "products": [
    {"productId": "p001", "quantity": 2},
    {"productId": "p002", "quantity": 1}
  ],
  "addressId": "addr_001"
}
```

**性能指标**:
| 商品数量 | 目标响应时间 |
|----------|--------------|
| 1-5个 | < 500ms |
| 5-20个 | < 1000ms |
| 20-50个 | < 2000ms |

---

## 6. 系统资源监控指标

### 6.1 监控指标定义

| 指标类别 | 监控指标 | 正常范围 | 告警阈值 |
|----------|----------|----------|----------|
| **CPU** | 使用率 | 0-70% | > 80% |
| **CPU** | 系统负载 | < 核数 | > 2倍核数 |
| **内存** | 使用率 | 0-70% | > 85% |
| **内存** | JVM堆使用 | < 2GB | > 2.5GB |
| **磁盘** | I/O等待 | < 10ms | > 50ms |
| **磁盘** | 使用率 | < 80% | > 90% |
| **网络** | 连接数 | < 5000 | > 10000 |
| **网络** | 带宽使用 | < 70% | > 90% |
| **数据库** | 连接数 | < 100 | > 200 |
| **数据库** | 查询时间 | < 100ms | > 500ms |

---

### 6.2 监控配置

**Prometheus 配置**:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'qizhilian-app'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'mysql'
    static_configs:
      - targets: ['localhost:9104']

  - job_name: 'node'
    static_configs:
      - targets: ['localhost:9100']
```

**Grafana Dashboard 指标**:
```
# CPU使用率
rate(process_cpu_seconds_total[1m]) * 100

# 内存使用率
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# HTTP请求响应时间P95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))

# 数据库连接池使用率
(hikaricp_connections_active / hikaricp_connections_max) * 100
```

---

## 7. JMeter测试脚本结构

### 7.1 目录结构
```
I:\AI-Ready\tests\performance\
├── scripts/
│   ├── OrderCreationTest.jmx          # 订单创建测试
│   ├── InventoryQueryTest.jmx         # 库存查询测试
│   ├── ConcurrentLoginTest.jmx        # 并发登录测试
│   ├── MixedBusinessTest.jmx          # 混合业务测试
│   ├── StressTest.jmx                 # 压力测试
│   └── APITestSuite.jmx               # API接口测试套件
├── data/
│   ├── users.csv                      # 测试用户数据
│   ├── products.csv                   # 商品数据
│   └── orders.csv                     # 订单数据
├── results/
│   └── *.jtl                          # 测试结果文件
├── plugins/
│   └── CustomSampler.jar              # 自定义采样器
├── PERFORMANCE_TEST_PLAN.md           # 本文档
└── PERFORMANCE_TEST_REPORT.md         # 测试报告模板
```

---

## 8. 性能基准汇总

### 8.1 核心指标基准

| 模块 | 指标 | 基准值 | 测试用例 |
|------|------|--------|----------|
| 登录 | 响应时间P95 | < 1000ms | TC-CON-001 |
| 订单创建 | 响应时间P95 | < 2000ms | TC-PF-001 |
| 库存查询 | 响应时间P95 | < 200ms | TC-PF-003 |
| 报表生成 | 响应时间 | < 5000ms | TC-PF-005 |
| 系统并发 | 最大并发数 | 1000+ | TC-CON-004 |
| 数据库 | 查询时间 | < 100ms | TC-DB-001 |
| API接口 | 平均响应 | < 1000ms | TC-API-* |

---

## 9. 执行计划

### 9.1 测试执行顺序

| 阶段 | 测试内容 | 预计时长 |
|------|----------|----------|
| 第1天 | 单接口性能测试 | 4h |
| 第2天 | 并发登录测试 | 2h |
| 第2天 | 业务流程性能测试 | 4h |
| 第3天 | 数据库性能测试 | 3h |
| 第3天 | 压力测试 | 3h |
| 第4天 | 结果分析与报告 | 2h |

---

## 附录A: 性能测试报告模板

```markdown
# 企智连性能测试报告

## 1. 测试概述
- 测试日期: YYYY-MM-DD
- 测试环境: [环境描述]
- 测试工具: JMeter 5.6

## 2. 测试结果汇总
| 用例编号 | 用例名称 | 平均响应时间 | P95响应时间 | 成功率 | 结果 |
|----------|----------|--------------|-------------|--------|------|
| TC-PF-001 | 订单创建 | XXXms | XXXms | XX% | PASS/FAIL |

## 3. 性能瓶颈分析
[详细分析]

## 4. 优化建议
[优化建议列表]

## 5. 结论
[测试结论]
```

---

**文档版本历史**:
| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|----------|
| v1.0 | 2026-04-15 | test-agent-1 | 初版创建 |