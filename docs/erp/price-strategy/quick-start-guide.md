# 价格策略API快速开始指南

**目标读者**: 开发人员、测试人员  
**所需时间**: 10分钟

## 🚀 快速体验

### 环境准备
确保你已经满足以下条件：
- ERP系统已启动并运行在 `http://localhost:8080`
- 拥有有效的用户账号和权限
- 已获取访问Token

### 步骤1: 获取访问Token

使用你的账号登录系统，获取访问Token。Token通常通过登录接口返回，格式如下：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 7200
  }
}
```

### 步骤2: 创建第一个价格策略

使用以下命令创建一个简单的价格策略：

```bash
curl -X POST "http://localhost:8080/api/sale/pricing/strategies" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "tenantId": 1001,
    "name": "快速测试策略",
    "description": "用于API测试的价格策略",
    "strategyType": "customer_level",
    "customerLevel": "VIP",
    "discountRate": 0.15,
    "effectiveStartTime": "2026-05-01T00:00:00",
    "effectiveEndTime": "2026-12-31T23:59:59",
    "priority": 5
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": 12345
}
```

保存返回的策略ID，后续操作会用到。

### 步骤3: 查询策略列表

查看系统中所有价格策略：

```bash
curl -X GET "http://localhost:8080/api/sale/pricing/strategies" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 步骤4: 进行价格计算

使用刚创建的策略计算价格：

```bash
curl -X POST "http://localhost:8080/api/sale/pricing/calculate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "customerId": 10001,
    "customerLevel": "VIP",
    "items": [
      {
        "productId": 5001,
        "productName": "测试产品",
        "basePrice": 100.00,
        "quantity": 5
      }
    ]
  }'
```

**预期响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "originalTotalAmount": 500.00,
    "discountedTotalAmount": 425.00,
    "finalTotalAmount": 425.00,
    "totalDiscountAmount": 75.00,
    "itemResults": [
      {
        "productId": 5001,
        "productName": "测试产品",
        "basePrice": 100.00,
        "quantity": 5,
        "subtotal": 500.00,
        "discountedUnitPrice": 85.00,
        "discountedSubtotal": 425.00,
        "discountAmount": 75.00,
        "appliedRules": ["VIP客户折扣15%"]
      }
    ]
  }
}
```

## 📚 核心概念

### 1. 策略类型
系统支持多种策略类型：
- **customer_level**: 客户等级定价（VIP、金卡、银卡等）
- **region**: 区域定价（华北、华东、华南等）
- **product_category**: 产品类别定价
- **time**: 时间定价（季节性、节假日等）
- **composite**: 组合定价

### 2. 规则配置
每个策略可以包含多个规则，规则按顺序执行：
```json
{
  "rules": [
    {
      "name": "满减规则",
      "ruleType": "amount_discount",
      "conditionExpression": "orderAmount >= 1000",
      "discountAmount": 100.00
    },
    {
      "name": "数量折扣",
      "ruleType": "quantity_discount",
      "conditionExpression": "quantity >= 10",
      "discountRate": 0.05
    }
  ]
}
```

### 3. 优先级机制
- 优先级数字越小，优先级越高（1 > 2 > 3）
- 当多个策略同时适用时，按优先级执行
- 同一策略内的规则按顺序执行

## 🔧 常用操作

### 1. 启用/停用策略
```bash
# 启用策略
curl -X POST "http://localhost:8080/api/sale/pricing/strategies/12345/activate" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# 停用策略
curl -X POST "http://localhost:8080/api/sale/pricing/strategies/12345/deactivate" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 2. 复制策略
```bash
curl -X POST "http://localhost:8080/api/sale/pricing/strategies/12345/copy" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 3. 验证策略配置
```bash
curl -X POST "http://localhost:8080/api/sale/pricing/strategies/12345/validate" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 🧪 测试用例

### 基础测试用例

**用例1: 创建策略**
```bash
# 测试数据
{
  "tenantId": 1001,
  "name": "测试策略",
  "strategyType": "customer_level",
  "customerLevel": "TEST"
}

# 验证点
- 返回状态码200
- 返回策略ID
- 策略状态为draft
```

**用例2: 计算VIP价格**
```bash
# 测试数据
{
  "customerLevel": "VIP",
  "items": [{"productId": 1, "basePrice": 100, "quantity": 2}]
}

# 验证点
- 返回状态码200
- 折扣后价格为180（VIP折扣10%）
- 应用的策略包含VIP策略
```

**用例3: 无效策略验证**
```bash
# 测试数据 - 缺少必填字段
{
  "name": "无效策略"
}

# 验证点
- 返回状态码400
- 错误信息包含必填字段提示
```

### 边界测试

**数量边界测试**:
```bash
{
  "items": [{"productId": 1, "basePrice": 100, "quantity": 0}]  # 数量为0
}
```

**金额边界测试**:
```bash
{
  "items": [{"productId": 1, "basePrice": 0, "quantity": 1}]  # 价格为0
}
```

## 📋 调试技巧

### 1. 查看请求日志
开启DEBUG日志级别查看详细的请求处理过程：
```yaml
logging:
  level:
    cn.aiedge.erp.sale.controller.PricingController: DEBUG
    cn.aiedge.erp.sale.service.impl: DEBUG
```

### 2. 使用Swagger UI
访问 `http://localhost:8080/swagger-ui.html` 可以：
- 查看所有API接口
- 在线测试接口
- 查看请求/响应模型

### 3. 常见问题排查

**问题1: 权限不足**
```json
{
  "code": 403,
  "message": "没有权限"
}
```
**解决方案**: 检查用户角色和权限配置

**问题2: Token过期**
```json
{
  "code": 401,
  "message": "Token已过期"
}
```
**解决方案**: 重新登录获取新Token

**问题3: 参数格式错误**
```json
{
  "code": 400,
  "message": "请求参数格式错误"
}
```
**解决方案**: 检查JSON格式和字段类型

## 🚀 进阶使用

### 批量操作
虽然系统未提供专门的批量接口，但可以通过脚本实现批量操作：

```bash
#!/bin/bash
# 批量创建策略
for i in {1..10}; do
  curl -X POST "http://localhost:8080/api/sale/pricing/strategies" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"name\": \"批量策略$i\", \"tenantId\": 1001}" &
done
```

### 性能测试
```bash
# 使用Apache Bench进行压力测试
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
  -p test_data.json -T "application/json" \
  http://localhost:8080/api/sale/pricing/calculate
```

## 📞 获取帮助

如果在快速开始过程中遇到问题：

1. **查看完整文档**: `api-documentation.md`
2. **查看错误日志**: 系统日志文件
3. **联系技术支持**: backend-dev@aiedge.cn
4. **提交问题**: GitHub Issues

---

**完成快速开始后，你已经可以：**
✅ 创建和管理价格策略  
✅ 执行价格计算  
✅ 处理常见错误  
✅ 进行基本测试  

下一步建议阅读完整的API文档了解所有功能和高级用法。