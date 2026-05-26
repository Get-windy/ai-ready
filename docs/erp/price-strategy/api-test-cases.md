# 价格策略API测试用例

**版本**: v1.0.0  
**最后更新**: 2026-05-01  
**适用对象**: QA测试人员、开发人员

## 📋 测试概述

本文档提供价格策略API的完整测试用例，包括功能测试、边界测试、性能测试和安全性测试。

## 🧪 测试环境要求

### 环境配置
- **API地址**: `http://localhost:8080/api/sale/pricing`
- **数据库**: MySQL 8.0+ 或 PostgreSQL 13+
- **认证方式**: Bearer Token
- **测试工具**: Postman, curl, JUnit

### 数据准备
```sql
-- 基础测试数据
INSERT INTO tenant (id, name) VALUES (1001, '测试租户');
INSERT INTO customer (id, tenant_id, name, level) VALUES (10001, 1001, '测试客户', 'VIP');
INSERT INTO product (id, name, base_price) VALUES (5001, '测试产品', 100.00);
```

## 🔧 功能测试用例

### 1. 策略管理功能

#### 1.1 创建价格策略
**测试用例ID**: TC-PRICE-001  
**测试目标**: 验证成功创建价格策略  
**前置条件**: 拥有`sale:pricing:create`权限

**测试步骤**:
1. 发送POST请求到 `/api/sale/pricing/strategies`
2. 请求体:
   ```json
   {
     "tenantId": 1001,
     "name": "功能测试策略",
     "strategyType": "customer_level",
     "customerLevel": "VIP",
     "discountRate": 0.10
   }
   ```

**预期结果**:
- 状态码: 200
- 返回的data字段包含策略ID
- 策略状态为"draft"
- 创建时间、更新时间正确

#### 1.2 创建失败 - 重复名称
**测试用例ID**: TC-PRICE-002  
**测试目标**: 验证名称重复时的错误处理

**测试步骤**:
1. 使用相同名称创建第二个策略

**预期结果**:
- 状态码: 400
- 错误信息包含"名称已存在"

#### 1.3 创建失败 - 必填字段缺失
**测试用例ID**: TC-PRICE-003  
**测试目标**: 验证必填字段校验

**测试步骤**:
1. 发送请求体缺少name字段
   ```json
   {
     "tenantId": 1001,
     "strategyType": "customer_level"
   }
   ```

**预期结果**:
- 状态码: 400
- 错误信息包含"name不能为空"

### 2. 查询功能

#### 2.1 分页查询
**测试用例ID**: TC-PRICE-010  
**测试目标**: 验证分页查询功能

**测试步骤**:
1. 创建10个测试策略
2. 发送GET请求: `/api/sale/pricing/strategies?pageNum=1&pageSize=5`

**预期结果**:
- 状态码: 200
- 返回5条记录
- total字段为10
- pages字段为2

#### 2.2 条件查询
**测试用例ID**: TC-PRICE-011  
**测试目标**: 验证按条件筛选

**测试步骤**:
1. 发送GET请求: `/api/sale/pricing/strategies?status=active&strategyType=customer_level`

**预期结果**:
- 只返回状态为active且类型为customer_level的策略

### 3. 价格计算功能

#### 3.1 基础价格计算
**测试用例ID**: TC-PRICE-020  
**测试目标**: 验证VIP客户折扣计算

**前置条件**: 创建VIP客户策略（折扣10%）

**测试步骤**:
1. 发送POST请求到 `/api/sale/pricing/calculate`
2. 请求体:
   ```json
   {
     "customerId": 10001,
     "customerLevel": "VIP",
     "items": [
       {
         "productId": 5001,
         "basePrice": 100.00,
         "quantity": 2
       }
     ]
   }
   ```

**预期结果**:
- 状态码: 200
- originalTotalAmount: 200.00
- discountedTotalAmount: 180.00
- totalDiscountAmount: 20.00
- appliedStrategies包含VIP策略

#### 3.2 多个策略叠加
**测试用例ID**: TC-PRICE-021  
**测试目标**: 验证多个策略同时适用

**前置条件**:
- 策略A: VIP客户折扣10%
- 策略B: 华北区域折扣5%

**测试步骤**:
1. 客户为VIP且位于华北区域
2. 计算价格

**预期结果**:
- 总折扣为14.5%（10% + 5% - 10%*5%）
- appliedStrategies包含两个策略

#### 3.3 策略优先级
**测试用例ID**: TC-PRICE-022  
**测试目标**: 验证策略优先级

**前置条件**:
- 策略A: 优先级1，折扣10%
- 策略B: 优先级2，折扣20%

**测试步骤**:
1. 两个策略同时适用
2. 计算价格

**预期结果**:
- 只应用策略A（优先级更高）
- 折扣为10%

### 4. 状态管理功能

#### 4.1 策略启用/停用
**测试用例ID**: TC-PRICE-030  
**测试目标**: 验证状态切换

**测试步骤**:
1. 创建策略（状态为draft）
2. 启用策略
3. 查询策略详情
4. 停用策略
5. 再次查询

**预期结果**:
- 启用后状态为active
- 停用后状态为inactive
- 状态变更记录正确

#### 4.2 已过期策略
**测试用例ID**: TC-PRICE-031  
**测试目标**: 验证过期策略处理

**前置条件**: 创建已过期的策略

**测试步骤**:
1. 计算价格时策略已过期

**预期结果**:
- 过期策略不参与价格计算
- 状态自动变更为expired

## 🎯 边界测试用例

### 1. 数值边界

#### 1.1 零值和负值
**测试用例ID**: TC-PRICE-BOUNDARY-001

**测试场景**:
- 基础价格为0
- 数量为0或负数
- 折扣率为0或负值

**预期结果**:
- 价格为0时返回错误
- 数量为0或负数时返回错误
- 折扣率为负值时返回错误

#### 1.2 极大值
**测试用例ID**: TC-PRICE-BOUNDARY-002

**测试场景**:
- 数量为Integer.MAX_VALUE
- 金额为BigDecimal最大值

**预期结果**:
- 正确处理或返回溢出错误
- 数据库约束正确

### 2. 时间边界

#### 2.1 时间重叠
**测试用例ID**: TC-PRICE-BOUNDARY-010

**测试场景**:
- 策略生效时间完全重叠
- 策略生效时间部分重叠

**预期结果**:
- 按优先级处理重叠策略
- 返回明确的冲突信息

#### 2.2 无效时间
**测试用例ID**: TC-PRICE-BOUNDARY-011

**测试场景**:
- 结束时间早于开始时间
- 时间为未来很远的时间
- 时间为过去的时间

**预期结果**:
- 时间逻辑校验正确
- 返回合适的错误信息

## ⚡ 性能测试用例

### 1. 接口响应时间

#### 1.1 单接口性能
**测试目标**: 验证单个接口的响应时间

**测试场景**:
- 创建策略: < 100ms
- 查询列表: < 50ms
- 计算价格: < 200ms

**测试方法**:
```bash
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
  -p calculate_data.json -T "application/json" \
  http://localhost:8080/api/sale/pricing/calculate
```

#### 1.2 并发性能
**测试目标**: 验证高并发下的稳定性

**测试场景**:
- 100并发价格计算
- 50并发策略创建

**预期结果**:
- 成功率 > 99%
- 平均响应时间 < 1s
- 无死锁或数据不一致

### 2. 数据量性能

#### 2.1 大数据量查询
**测试目标**: 验证大量数据下的查询性能

**测试场景**:
- 数据库中有10,000个策略
- 分页查询不同页码

**预期结果**:
- 第一页查询 < 100ms
- 最后一页查询 < 300ms

#### 2.2 复杂计算性能
**测试目标**: 验证复杂价格计算的性能

**测试场景**:
- 订单包含100个产品
- 同时适用10个策略

**预期结果**:
- 计算时间 < 500ms
- 内存使用合理

## 🔐 安全性测试用例

### 1. 认证授权

#### 1.1 未认证访问
**测试用例ID**: TC-PRICE-SEC-001

**测试步骤**:
1. 不提供Token访问接口

**预期结果**:
- 状态码: 401
- 错误信息: "未认证"

#### 1.2 权限不足
**测试用例ID**: TC-PRICE-SEC-002

**测试步骤**:
1. 使用只有查看权限的Token尝试创建策略

**预期结果**:
- 状态码: 403
- 错误信息: "权限不足"

### 2. 输入验证

#### 2.1 SQL注入
**测试用例ID**: TC-PRICE-SEC-010

**测试步骤**:
1. 在查询参数中注入SQL
   ```bash
   GET /api/sale/pricing/strategies?name=test' OR '1'='1
   ```

**预期结果**:
- 安全拦截或参数化查询
- 无数据泄露

#### 2.2 XSS攻击
**测试用例ID**: TC-PRICE-SEC-011

**测试步骤**:
1. 在策略名称中包含脚本
   ```json
   {
     "name": "<script>alert('xss')</script>"
   }
   ```

**预期结果**:
- 输入被过滤或转义
- 前端安全显示

## 📊 测试数据生成

### 1. 策略生成脚本
```python
# generate_test_strategies.py
import json
import random
from datetime import datetime, timedelta

strategies = []
for i in range(100):
    strategy = {
        "tenantId": 1001,
        "name": f"测试策略_{i}",
        "strategyType": random.choice(["customer_level", "region", "product_category"]),
        "discountRate": round(random.uniform(0.01, 0.30), 2),
        "effectiveStartTime": (datetime.now() + timedelta(days=random.randint(-30, 30))).isoformat(),
        "priority": random.randint(1, 10)
    }
    strategies.append(strategy)

with open('test_strategies.json', 'w') as f:
    json.dump(strategies, f, indent=2)
```

### 2. 计算请求生成
```python
# generate_calculation_requests.py
import json
import random

requests = []
for i in range(50):
    request = {
        "customerId": random.randint(10001, 10100),
        "customerLevel": random.choice(["VIP", "GOLD", "SILVER", "NORMAL"]),
        "items": [
            {
                "productId": random.randint(5001, 5100),
                "basePrice": round(random.uniform(10, 1000), 2),
                "quantity": random.randint(1, 100)
            } for _ in range(random.randint(1, 10))
        ]
    }
    requests.append(request)

with open('calculation_requests.json', 'w') as f:
    json.dump(requests, f, indent=2)
```

## 📈 测试报告模板

### 测试执行结果
```markdown
# 价格策略API测试报告
**测试日期**: 2026-05-01
**测试环境**: 开发环境
**测试人员**: QA Team

## 执行概况
- 总用例数: 50
- 通过数: 48
- 失败数: 2
- 通过率: 96%

## 失败用例分析
1. TC-PRICE-022: 策略优先级逻辑问题
   - 问题: 优先级相同时处理不正确
   - 建议: 增加优先级相同时的处理规则

2. TC-PRICE-BOUNDARY-002: 极大值处理
   - 问题: 金额超过BigDecimal范围时崩溃
   - 建议: 增加金额范围校验

## 性能测试结果
- 平均响应时间: 85ms
- 最大响应时间: 320ms
- 吞吐量: 1200 req/min
- 错误率: 0.1%

## 建议
1. 修复优先级逻辑问题
2. 增加输入边界校验
3. 优化大数据量查询性能
```

## 🔄 回归测试策略

### 每次发版必测
1. 基础CRUD功能
2. 价格计算核心逻辑
3. 权限控制
4. 数据一致性

### 每月全面测试
1. 所有功能测试用例
2. 性能基准测试
3. 安全扫描
4. 兼容性测试

---

**文档维护**: doc-writer  
**最后验证**: 2026-05-01  
**下一步**: 自动化测试脚本开发