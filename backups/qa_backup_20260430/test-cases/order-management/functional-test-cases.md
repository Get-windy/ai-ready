# 订单管理模块功能测试用例

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: test-agent-2  
**最后更新**: 2026-04-27

---

## 目录

1. [测试概述](#测试概述)
2. [测试用例 - 订单创建](#测试用例---订单创建)
3. [测试用例 - 订单查询](#测试用例---订单查询)
4. [测试用例 - 订单修改](#测试用例---订单修改)
5. [测试用例 - 订单状态管理](#测试用例---订单状态管理)
6. [测试数据准备](#测试数据准备)
7. [测试环境配置](#测试环境配置)

---

## 测试概述

### 测试目标

验证Sprint 27+1测试环境中订单管理模块的功能正确性和完整性。

### 测试范围

| 功能模块 | 测试重点 | 优先级 |
|---------|---------|-------|
| 订单创建 | 正常/异常订单创建 | 高 |
| 订单查询 | 按条件查询订单 | 高 |
| 订单修改 | 订单信息修改 | 高 |
| 订单状态 | 状态流转管理 | 高 |

### 测试环境

- **服务地址**: http://test-ai-ready.example.com
- **API前缀**: /api/v1/order
- **测试工具**: pytest + requests
- **数据库**: MySQL 8.0

---

## 测试用例 - 订单创建

### TC-CREATE-001: 正常订单创建

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-CREATE-001 |
| **用例名称** | 正常订单创建 |
| **测试目的** | 验证正常订单创建流程 |
| **优先级** | P0 |
| **前置条件** | 用户已登录，商品库存充足 |

**测试步骤**:
1. 调用POST /api/v1/order/create
2. 传入正确的订单数据（商品ID、数量、地址等）
3. 验证返回结果

**输入数据**:
```json
{
  "userId": 1,
  "items": [
    {
      "productId": "SKU001",
      "quantity": 2,
      "price": 99.99
    }
  ],
  "shippingAddress": {
    "province": "广东省",
    "city": "深圳市",
    "district": "南山区",
    "address": "科技园"
  },
  "remark": "测试订单"
}
```

**预期结果**:
- 返回状态码: 200
- 返回订单号（格式: ORD + 8位数字）
- 订单状态: PENDING
- 订单金额计算正确

---

### TC-CREATE-002: 订单创建-库存不足

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-CREATE-002 |
| **用例名称** | 订单创建-库存不足 |
| **测试目的** | 验证库存不足时的错误处理 |
| **优先级** | P1 |

**测试步骤**:
1. 选择库存为0的商品
2. 调用订单创建API
3. 验证错误返回

**预期结果**:
- 返回状态码: 400
- 错误信息: "库存不足"
- 订单未创建

---

### TC-CREATE-003: 订单创建-参数缺失

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-CREATE-003 |
| **用例名称** | 订单创建-参数缺失 |
| **测试目的** | 验证必填参数缺失时的处理 |
| **优先级** | P1 |

**测试步骤**:
1. 调用订单创建API，不传userId
2. 验证错误返回

**预期结果**:
- 返回状态码: 400
- 错误信息: "用户ID不能为空"

---

### TC-CREATE-004: 订单创建-金额校验

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-CREATE-004 |
| **用例名称** | 订单创建-金额校验 |
| **测试目的** | 验证订单金额计算正确性 |
| **优先级** | P1 |

**测试步骤**:
1. 创建包含多个商品的订单
2. 验证总金额 = 商品1价格×数量 + 商品2价格×数量
3. 验证运费计算

**预期结果**:
- 订单金额计算准确
- 运费计算符合规则

---

## 测试用例 - 订单查询

### TC-QUERY-001: 按订单号查询

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-QUERY-001 |
| **用例名称** | 按订单号查询 |
| **测试目的** | 验证按订单号查询功能 |
| **优先级** | P0 |

**测试步骤**:
1. 调用GET /api/v1/order/{orderNo}
2. 传入有效订单号
3. 验证返回结果

**预期结果**:
- 返回状态码: 200
- 返回订单完整信息
- 订单号匹配

---

### TC-QUERY-002: 按用户查询订单列表

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-QUERY-002 |
| **用例名称** | 按用户查询订单列表 |
| **测试目的** | 验证分页查询功能 |
| **优先级** | P0 |

**测试步骤**:
1. 调用GET /api/v1/order/list?userId=1&page=1&size=10
2. 验证返回结果

**预期结果**:
- 返回状态码: 200
- 返回订单列表
- 分页参数正确
- 总数量准确

---

### TC-QUERY-003: 按状态查询订单

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-QUERY-003 |
| **用例名称** | 按状态查询订单 |
| **测试目的** | 验证按状态筛选功能 |
| **优先级** | P1 |

**测试步骤**:
1. 调用GET /api/v1/order/list?status=PENDING
2. 验证返回结果

**预期结果**:
- 返回状态码: 200
- 所有订单状态为PENDING

---

### TC-QUERY-004: 订单不存在

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-QUERY-004 |
| **用例名称** | 订单不存在 |
| **测试目的** | 验证查询不存在订单的处理 |
| **优先级** | P1 |

**测试步骤**:
1. 调用GET /api/v1/order/ORD99999999
2. 验证错误返回

**预期结果**:
- 返回状态码: 404
- 错误信息: "订单不存在"

---

## 测试用例 - 订单修改

### TC-UPDATE-001: 修改订单地址

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-UPDATE-001 |
| **用例名称** | 修改订单地址 |
| **测试目的** | 验证订单地址修改功能 |
| **优先级** | P0 |
| **前置条件** | 订单状态为PENDING |

**测试步骤**:
1. 调用PUT /api/v1/order/{orderNo}/address
2. 传入新地址
3. 验证修改结果

**输入数据**:
```json
{
  "province": "北京市",
  "city": "北京市",
  "district": "朝阳区",
  "address": "国贸大厦"
}
```

**预期结果**:
- 返回状态码: 200
- 地址信息已更新

---

### TC-UPDATE-002: 修改已发货订单

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-UPDATE-002 |
| **用例名称** | 修改已发货订单 |
| **测试目的** | 验证已发货订单不可修改 |
| **优先级** | P1 |

**测试步骤**:
1. 选择状态为SHIPPED的订单
2. 尝试修改订单信息
3. 验证错误返回

**预期结果**:
- 返回状态码: 400
- 错误信息: "已发货订单不可修改"

---

### TC-UPDATE-003: 取消订单

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-UPDATE-003 |
| **用例名称** | 取消订单 |
| **测试目的** | 验证订单取消功能 |
| **优先级** | P0 |

**测试步骤**:
1. 调用PUT /api/v1/order/{orderNo}/cancel
2. 验证订单状态

**预期结果**:
- 返回状态码: 200
- 订单状态变为CANCELLED
- 库存已恢复

---

## 测试用例 - 订单状态管理

### TC-STATUS-001: 状态流转-PENDING到PAID

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-STATUS-001 |
| **用例名称** | 状态流转-PENDING到PAID |
| **测试目的** | 验证支付后状态变更 |
| **优先级** | P0 |

**测试步骤**:
1. 创建订单（状态PENDING）
2. 模拟支付回调
3. 查询订单状态

**预期结果**:
- 订单状态变为PAID
- 支付时间已记录

---

### TC-STATUS-002: 状态流转-PAID到SHIPPED

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-STATUS-002 |
| **用例名称** | 状态流转-PAID到SHIPPED |
| **测试目的** | 验证发货后状态变更 |
| **优先级** | P0 |

**测试步骤**:
1. 创建已支付订单
2. 调用发货API
3. 查询订单状态

**预期结果**:
- 订单状态变为SHIPPED
- 物流信息已记录

---

### TC-STATUS-003: 状态流转-SHIPPED到COMPLETED

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-STATUS-003 |
| **用例名称** | 状态流转-SHIPPED到COMPLETED |
| **测试目的** | 验证确认收货后状态变更 |
| **优先级** | P0 |

**测试步骤**:
1. 创建已发货订单
2. 调用确认收货API
3. 查询订单状态

**预期结果**:
- 订单状态变为COMPLETED
- 完成时间已记录

---

### TC-STATUS-004: 非法状态流转

| 属性 | 内容 |
|-----|------|
| **用例ID** | TC-STATUS-004 |
| **用例名称** | 非法状态流转 |
| **测试目的** | 验证非法状态流转被阻止 |
| **优先级** | P1 |

**测试步骤**:
1. 创建PENDING订单
2. 尝试直接调用确认收货API
3. 验证错误返回

**预期结果**:
- 返回状态码: 400
- 错误信息: "非法状态流转"

---

## 测试数据准备

### 1. 测试用户

| 用户ID | 用户名 | 角色 | 状态 |
|-------|-------|-----|------|
| 1 | test_user_1 | 普通用户 | 启用 |
| 2 | test_user_2 | 普通用户 | 启用 |
| 3 | test_admin | 管理员 | 启用 |

### 2. 测试商品

| 商品ID | 商品名称 | 价格 | 库存 |
|-------|---------|-----|------|
| SKU001 | 测试商品1 | 99.99 | 100 |
| SKU002 | 测试商品2 | 199.99 | 0 |
| SKU003 | 测试商品3 | 299.99 | 50 |

### 3. 测试订单

| 订单号 | 用户ID | 状态 | 用途 |
|-------|-------|------|-----|
| ORD00000001 | 1 | PENDING | 正常测试 |
| ORD00000002 | 1 | PAID | 支付测试 |
| ORD00000003 | 1 | SHIPPED | 发货测试 |
| ORD00000004 | 1 | COMPLETED | 完成测试 |
| ORD00000005 | 1 | CANCELLED | 取消测试 |

---

## 测试环境配置

### 1. 服务配置

```yaml
# application-test.yml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_ready_test?useSSL=false
    username: test_user
    password: test_password
  
  redis:
    host: localhost
    port: 6379
    database: 1

# 订单服务配置
order:
  # 订单号前缀
  orderNoPrefix: ORD
  # 订单超时时间（分钟）
  timeoutMinutes: 30
  # 最大订单金额
  maxAmount: 100000
```

### 2. 数据库初始化脚本

```sql
-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS ai_ready_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 订单表
CREATE TABLE IF NOT EXISTS `order` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_no` VARCHAR(20) NOT NULL UNIQUE,
  `user_id` BIGINT NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `shipping_address` JSON,
  `remark` VARCHAR(500),
  `paid_time` DATETIME,
  `shipped_time` DATETIME,
  `completed_time` DATETIME,
  `cancelled_time` DATETIME,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单商品表
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `product_id` VARCHAR(50) NOT NULL,
  `product_name` VARCHAR(200),
  `quantity` INT NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `subtotal` DECIMAL(10,2) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`order_id`) REFERENCES `order`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 测试执行计划

### 阶段1: 测试准备（30分钟）

| 时间 | 任务 | 负责人 |
|-----|-----|-------|
| 0-10分钟 | 检查测试环境 | test-agent-2 |
| 10-20分钟 | 初始化测试数据 | test-agent-2 |
| 20-30分钟 | 验证测试数据 | test-agent-2 |

### 阶段2: 功能测试执行（2小时）

| 时间 | 测试模块 | 用例数量 |
|-----|---------|---------|
| 0-30分钟 | 订单创建 | 4 |
| 30-60分钟 | 订单查询 | 4 |
| 60-90分钟 | 订单修改 | 3 |
| 90-120分钟 | 订单状态管理 | 4 |

### 阶段3: 报告生成（30分钟）

| 时间 | 任务 | 负责人 |
|-----|-----|-------|
| 0-15分钟 | 汇总测试结果 | test-agent-2 |
| 15-30分钟 | 生成测试报告 | test-agent-2 |

---

## 附录

### 附录A: 状态机定义

```
[PENDING] --支付--> [PAID] --发货--> [SHIPPED] --确认收货--> [COMPLETED]
    |                    |                      |
    |--取消--> [CANCELLED]  |--取消--> [CANCELLED]  |
                           |--退款--> [REFUNDED]
```

### 附录B: 错误码定义

| 错误码 | 描述 | HTTP状态码 |
|-------|-----|----------|
| ORDER001 | 订单不存在 | 404 |
| ORDER002 | 库存不足 | 400 |
| ORDER003 | 参数错误 | 400 |
| ORDER004 | 状态非法 | 400 |
| ORDER005 | 订单不可修改 | 400 |

---

**最后更新**: 2026-04-27  
**文档版本**: 1.0.0  
**项目**: AI-Ready Sprint 27+1