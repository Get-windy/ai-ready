# 价格策略API接口文档

**版本**: v1.0.0  
**最后更新**: 2026-05-01  
**维护者**: 文档专家 (doc-writer)

## 📋 概述

本文档详细描述了ERP系统的价格策略管理模块API接口。价格策略模块负责管理销售过程中的价格规则、策略配置和价格计算功能，支持灵活的定价策略，如客户等级定价、区域定价、产品类别定价等。

### 主要功能
- 价格策略的增删改查
- 价格规则的配置管理
- 实时价格计算
- 价格模拟与验证
- 策略生效状态管理

## 🔗 接口概览

所有价格策略API均位于 `/api/sale/pricing` 路径下。

## 🛠️ 接口详情

### 1. 价格策略管理

#### 1.1 创建价格策略

**接口**: `POST /api/sale/pricing/strategies`

**权限要求**: `sale:pricing:create`

**请求体**:
```json
{
  "tenantId": 1001,
  "name": "VIP客户价格策略",
  "description": "适用于VIP客户的专属价格策略",
  "strategyType": "customer_level",
  "customerLevel": "VIP",
  "basePrice": 100.00,
  "discountRate": 0.10,
  "effectiveStartTime": "2026-05-01T00:00:00",
  "effectiveEndTime": "2026-12-31T23:59:59",
  "priority": 1,
  "rules": [
    {
      "name": "订单满1000元打9折",
      "ruleType": "amount_discount",
      "conditionExpression": "orderAmount >= 1000",
      "discountRate": 0.10,
      "minAmount": 1000
    }
  ]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": 12345
}
```

**字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 策略ID（创建时由系统生成） |
| tenantId | Long | 是 | 租户ID |
| name | String | 是 | 策略名称 |
| description | String | 否 | 策略描述 |
| strategyType | String | 否 | 策略类型：customer_level/region/product_category/time/composite |
| customerLevel | String | 否 | 适用客户等级 |
| regionCode | String | 否 | 适用区域编码 |
| productCategoryId | Long | 否 | 适用产品类别ID |
| basePrice | BigDecimal | 否 | 基础价格 |
| priceFactor | BigDecimal | 否 | 价格系数 |
| discountRate | BigDecimal | 否 | 折扣率（0-1之间） |
| discountAmount | BigDecimal | 否 | 折扣金额 |
| minQuantity | Integer | 否 | 最小数量门槛 |
| formulaConfig | String | 否 | 公式配置（JSON格式） |
| effectiveStartTime | LocalDateTime | 否 | 生效开始时间 |
| effectiveEndTime | LocalDateTime | 否 | 生效结束时间 |
| priority | Integer | 否 | 优先级（数值越小优先级越高） |
| status | String | 否 | 状态：draft/active/inactive/expired |
| rules | List<PriceRuleDTO> | 否 | 关联规则列表 |

#### 1.2 更新价格策略

**接口**: `PUT /api/sale/pricing/strategies/{id}`

**权限要求**: `sale:pricing:update`

**路径参数**:
- `id`: 策略ID

**请求体**: 与创建接口相同

**响应**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

#### 1.3 删除价格策略

**接口**: `DELETE /api/sale/pricing/strategies/{id}`

**权限要求**: `sale:pricing:delete`

**路径参数**:
- `id`: 策略ID

**响应**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

#### 1.4 获取策略详情

**接口**: `GET /api/sale/pricing/strategies/{id}`

**权限要求**: `SaCheckLogin`（登录即可）

**路径参数**:
- `id`: 策略ID

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 12345,
    "tenantId": 1001,
    "name": "VIP客户价格策略",
    "description": "适用于VIP客户的专属价格策略",
    "strategyType": "customer_level",
    "customerLevel": "VIP",
    "basePrice": 100.00,
    "discountRate": 0.10,
    "effectiveStartTime": "2026-05-01T00:00:00",
    "effectiveEndTime": "2026-12-31T23:59:59",
    "priority": 1,
    "status": "active",
    "createTime": "2026-04-28T10:30:00",
    "updateTime": "2026-04-28T10:30:00",
    "rules": [
      {
        "id": 1,
        "name": "订单满1000元打9折",
        "ruleType": "amount_discount",
        "conditionExpression": "orderAmount >= 1000",
        "discountRate": 0.10,
        "minAmount": 1000,
        "status": "active"
      }
    ]
  }
}
```

#### 1.5 分页查询策略列表

**接口**: `GET /api/sale/pricing/strategies`

**权限要求**: `SaCheckLogin`（登录即可）

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认为1 |
| pageSize | Integer | 否 | 每页大小，默认为10 |
| tenantId | Long | 否 | 租户ID过滤 |
| name | String | 否 | 策略名称模糊搜索 |
| status | String | 否 | 状态过滤 |
| strategyType | String | 否 | 策略类型过滤 |

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 12345,
        "name": "VIP客户价格策略",
        "strategyType": "customer_level",
        "status": "active",
        "effectiveStartTime": "2026-05-01T00:00:00",
        "effectiveEndTime": "2026-12-31T23:59:59"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

#### 1.6 查询生效中的策略

**接口**: `GET /api/sale/pricing/strategies/active`

**权限要求**: `SaCheckLogin`（登录即可）

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 12345,
      "name": "VIP客户价格策略",
      "strategyType": "customer_level",
      "customerLevel": "VIP",
      "discountRate": 0.10
    }
  ]
}
```

#### 1.7 启用策略

**接口**: `POST /api/sale/pricing/strategies/{id}/activate`

**权限要求**: `sale:pricing:activate`

**路径参数**:
- `id`: 策略ID

**响应**:
```json
{
  "code": 200,
  "message": "启用成功",
  "data": null
}
```

#### 1.8 停用策略

**接口**: `POST /api/sale/pricing/strategies/{id}/deactivate`

**权限要求**: `sale:pricing:activate`

**路径参数**:
- `id`: 策略ID

**响应**:
```json
{
  "code": 200,
  "message": "停用成功",
  "data": null
}
```

#### 1.9 复制策略

**接口**: `POST /api/sale/pricing/strategies/{id}/copy`

**权限要求**: `sale:pricing:create`

**路径参数**:
- `id`: 策略ID

**响应**:
```json
{
  "code": 200,
  "message": "复制成功",
  "data": 12346
}
```

#### 1.10 验证策略配置

**接口**: `POST /api/sale/pricing/strategies/{id}/validate`

**权限要求**: `sale:pricing:validate`

**路径参数**:
- `id`: 策略ID

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

### 2. 价格计算

#### 2.1 计算价格

**接口**: `POST /api/sale/pricing/calculate`

**权限要求**: `SaCheckLogin`（登录即可）

**请求体**:
```json
{
  "customerId": 10001,
  "customerLevel": "VIP",
  "regionCode": "CN-BJ",
  "items": [
    {
      "productId": 5001,
      "productName": "智能手表",
      "productCategoryId": 101,
      "basePrice": 299.00,
      "quantity": 2
    }
  ]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "originalTotalAmount": 598.00,
    "discountedTotalAmount": 538.20,
    "finalTotalAmount": 538.20,
    "totalDiscountAmount": 59.80,
    "itemResults": [
      {
        "productId": 5001,
        "productName": "智能手表",
        "basePrice": 299.00,
        "quantity": 2,
        "subtotal": 598.00,
        "discountedUnitPrice": 269.10,
        "discountedSubtotal": 538.20,
        "discountAmount": 59.80,
        "appliedRules": ["VIP客户折扣10%"]
      }
    ],
    "appliedStrategies": [
      {
        "strategyId": 12345,
        "strategyName": "VIP客户价格策略",
        "strategyType": "customer_level",
        "discountAmount": 59.80
      }
    ]
  }
}
```

**请求体字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customerId | Long | 是 | 客户ID |
| customerLevel | String | 否 | 客户等级 |
| regionCode | String | 否 | 区域编码 |
| items | List<PriceCalculationItem> | 是 | 订单明细列表 |
| promotionIds | List<Long> | 否 | 指定促销ID列表（可选） |

**PriceCalculationItem字段**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productId | Long | 是 | 产品ID |
| productName | String | 否 | 产品名称 |
| productCategoryId | Long | 否 | 产品类别ID |
| basePrice | BigDecimal | 否 | 基础单价 |
| quantity | Integer | 否 | 数量 |

#### 2.2 模拟价格计算

**接口**: `POST /api/sale/pricing/simulate`

**权限要求**: `SaCheckLogin`（登录即可）

**说明**: 与计算价格接口类似，但不会影响实际库存和订单状态，仅用于价格模拟。

**请求体**: 与计算价格接口相同

**响应**: 与计算价格接口相同

### 3. 价格规则管理

#### 3.1 价格规则数据结构

**PriceRuleDTO字段说明**:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 否 | 规则ID |
| name | String | 否 | 规则名称 |
| description | String | 否 | 规则描述 |
| strategyId | Long | 否 | 所属策略ID |
| ruleType | String | 否 | 规则类型 |
| conditionExpression | String | 否 | 条件表达式 |
| calculationExpression | String | 否 | 计算表达式 |
| discountRate | BigDecimal | 否 | 折扣率 |
| discountAmount | BigDecimal | 否 | 折扣金额 |
| priceFactor | BigDecimal | 否 | 价格系数 |
| fixedAmount | BigDecimal | 否 | 固定金额 |
| minQuantity | Integer | 否 | 最小数量 |
| maxQuantity | Integer | 否 | 最大数量 |
| minAmount | BigDecimal | 否 | 最小金额 |
| maxAmount | BigDecimal | 否 | 最大金额 |
| sortOrder | Integer | 否 | 排序号 |
| status | String | 否 | 状态 |

## 📊 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或Token失效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 🔐 权限说明

价格策略模块的权限分为以下几个等级：

1. **查看权限** (`SaCheckLogin`): 登录用户即可查看策略列表和详情
2. **操作权限** (`sale:pricing:create/update/delete`): 创建、更新、删除价格策略
3. **激活权限** (`sale:pricing:activate`): 启用或停用价格策略
4. **验证权限** (`sale:pricing:validate`): 验证策略配置

## 🚀 使用示例

### 完整流程示例

#### 步骤1: 创建价格策略
```bash
curl -X POST "http://localhost:8080/api/sale/pricing/strategies" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "tenantId": 1001,
    "name": "华东区域价格策略",
    "strategyType": "region",
    "regionCode": "CN-SH",
    "discountRate": 0.05
  }'
```

#### 步骤2: 查询策略列表
```bash
curl -X GET "http://localhost:8080/api/sale/pricing/strategies?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer {token}"
```

#### 步骤3: 计算价格
```bash
curl -X POST "http://localhost:8080/api/sale/pricing/calculate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "customerId": 10001,
    "customerLevel": "VIP",
    "items": [
      {
        "productId": 5001,
        "quantity": 3
      }
    ]
  }'
```

## 📝 注意事项

1. **时间格式**: 所有日期时间字段使用ISO 8601格式: `YYYY-MM-DDTHH:mm:ss`
2. **金额精度**: 所有金额字段使用BigDecimal类型，保留2位小数
3. **优先级**: 策略优先级数值越小优先级越高
4. **策略冲突**: 当多个策略同时适用时，按优先级执行
5. **生效时间**: 策略的生效时间为闭区间，包括开始和结束时间点

## 🔄 更新记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0.0 | 2026-05-01 | 初始版本，包含完整的API接口文档 |

## 📞 技术支持

如果在使用过程中遇到任何问题，请联系：

- **技术支持**: backend-dev@aiedge.cn
- **业务咨询**: product-analyst@aiedge.cn
- **文档维护**: doc-writer@aiedge.cn