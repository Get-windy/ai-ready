# 移动端订单管理API文档

## 概述
本文档描述了AI-Ready系统移动端的订单管理相关API接口，包括订单查询、创建、更新、删除、状态管理等功能。

## 基础信息
- **基础路径**: `/api/order`
- **认证方式**: JWT Token (Bearer Token)
- **响应格式**: JSON
- **权限要求**: 不同操作需要不同的权限

## API接口列表

### 1. 分页查询订单
**GET** `/api/order/page`

**描述**: 分页查询订单列表，支持多种查询条件。

**权限要求**: `order:view`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pageNum | integer | 否 | 页码，默认1 |
| pageSize | integer | 否 | 每页大小，默认10 |
| tenantId | long | 是 | 租户ID |
| orderNo | string | 否 | 订单编号模糊查询 |
| customerName | string | 否 | 客户名称模糊查询 |
| orderType | integer | 否 | 订单类型（0-销售，1-采购，2-退货） |
| status | integer | 否 | 订单状态（0-待审核，1-已审核，2-已发货，3-已完成，4-已取消） |
| saleId | long | 否 | 销售员ID |
| startDate | string | 否 | 开始日期（格式：yyyy-MM-dd HH:mm:ss） |
| endDate | string | 否 | 结束日期（格式：yyyy-MM-dd HH:mm:ss） |

**请求头**:
```
Authorization: Bearer {accessToken}
```

**响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "id": 1,
        "orderNo": "SO202604270001",
        "customerName": "测试客户",
        "customerPhone": "13800138000",
        "customerAddress": "北京市朝阳区",
        "orderType": 0,
        "status": 1,
        "totalAmount": 1999.99,
        "paidAmount": 0.00,
        "saleName": "销售员张三",
        "createTime": "2026-04-27 10:00:00",
        "updateTime": "2026-04-27 10:00:00",
        "items": [
          {
            "productId": 1,
            "productName": "测试商品",
            "productCode": "P001",
            "quantity": 2,
            "unitPrice": 999.99,
            "totalPrice": 1999.98
          }
        ]
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10,
    "pages": 10
  }
}
```

**响应字段说明**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| list | array | 订单列表 |
| total | integer | 总记录数 |
| current | integer | 当前页码 |
| size | integer | 每页大小 |
| pages | integer | 总页数 |

### 2. 获取订单详情
**GET** `/api/order/{id}`

**描述**: 根据订单ID获取订单详细信息。

**权限要求**: `order:view`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 订单ID |

**请求头**:
```
Authorization: Bearer {accessToken}
```

**响应**:
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "orderNo": "SO202604270001",
    "customerId": 1,
    "customerName": "测试客户",
    "customerPhone": "13800138000",
    "customerAddress": "北京市朝阳区",
    "orderType": 0,
    "status": 1,
    "totalAmount": 1999.99,
    "paidAmount": 0.00,
    "remark": "测试订单备注",
    "saleId": 1,
    "saleName": "销售员张三",
    "createTime": "2026-04-27 10:00:00",
    "updateTime": "2026-04-27 10:00:00",
    "items": [
      {
        "id": 1,
        "orderId": 1,
        "productId": 1,
        "productName": "测试商品",
        "productCode": "P001",
        "quantity": 2,
        "unitPrice": 999.99,
        "totalPrice": 1999.98,
        "remark": "商品备注"
      }
    ],
    "paymentRecords": [
      {
        "id": 1,
        "orderId": 1,
        "paymentAmount": 1000.00,
        "paymentMethod": 0,
        "paymentTime": "2026-04-27 11:00:00",
        "remark": "首付款"
      }
    ],
    "deliveryInfo": {
      "deliveryMethod": 0,
      "deliveryAddress": "北京市朝阳区",
      "deliveryTime": "2026-04-28 10:00:00",
      "deliveryStatus": 0
    }
  }
}
```

### 3. 创建订单
**POST** `/api/order`

**描述**: 创建新的订单。

**权限要求**: `order:create`

**请求体**:
```json
{
  "tenantId": 1,
  "customerId": 1,
  "orderType": 0,
  "remark": "测试订单",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "unitPrice": 999.99,
      "remark": "商品备注"
    }
  ],
  "deliveryInfo": {
    "deliveryMethod": 0,
    "deliveryAddress": "北京市朝阳区",
    "deliveryTime": "2026-04-28 10:00:00"
  }
}
```

**请求头**:
```
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": 1
}
```

### 4. 更新订单
**PUT** `/api/order/{id}`

**描述**: 更新订单信息。

**权限要求**: `order:update`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 订单ID |

**请求体**:
```json
{
  "customerId": 1,
  "remark": "更新后的备注",
  "items": [
    {
      "productId": 1,
      "quantity": 3,
      "unitPrice": 999.99,
      "remark": "更新后的商品备注"
    }
  ]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "更新成功",
  "data": null
}
```

### 5. 删除订单
**DELETE** `/api/order/{id}`

**描述**: 删除订单（仅限草稿状态的订单）。

**权限要求**: `order:delete`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 订单ID |

**响应**:
```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 6. 更新订单状态
**PUT** `/api/order/{id}/status`

**描述**: 更新订单状态。

**权限要求**: `order:update`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 订单ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | integer | 是 | 新状态（0-待审核，1-已审核，2-已发货，3-已完成，4-已取消） |

**响应**:
```json
{
  "code": 200,
  "message": "状态更新成功",
  "data": null
}
```

### 7. 审核订单
**PUT** `/api/order/{id}/audit`

**描述**: 审核订单。

**权限要求**: `order:audit`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 订单ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | integer | 是 | 审核状态（1-通过，2-拒绝） |
| auditRemark | string | 否 | 审核备注 |

**响应**:
```json
{
  "code": 200,
  "message": "审核成功",
  "data": null
}
```

### 8. 订单收款
**PUT** `/api/order/{id}/receive`

**描述**: 记录订单收款。

**权限要求**: `order:receive`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | long | 是 | 订单ID |

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| amount | decimal | 是 | 收款金额 |

**响应**:
```json
{
  "code": 200,
  "message": "收款成功",
  "data": null
}
```

## 订单状态流转

### 状态定义
| 状态码 | 状态名称 | 说明 |
|--------|----------|------|
| 0 | 待审核 | 订单已创建，等待审核 |
| 1 | 已审核 | 订单已审核通过 |
| 2 | 已发货 | 订单已发货 |
| 3 | 已完成 | 订单已完成（已收货） |
| 4 | 已取消 | 订单已取消 |
| 5 | 已关闭 | 订单已关闭（系统自动关闭） |

### 状态流转规则
1. 订单创建 → 待审核（0）
2. 待审核 → 已审核（1）或 已取消（4）
3. 已审核 → 已发货（2）或 已取消（4）
4. 已发货 → 已完成（3）
5. 所有状态 → 已关闭（5）（30天未操作自动关闭）

## 移动端专用功能

### 1. 离线创建订单
移动端支持离线创建订单，数据缓存在本地，网络恢复后自动同步到服务器。

### 2. 订单拍照上传
支持拍照上传订单相关凭证（收据、发票等）。

### 3. 地理位置标记
创建订单时可自动获取当前位置信息。

### 4. 订单二维码
每个订单生成唯一二维码，方便扫描查看。

### 5. 订单推送通知
订单状态变更时，向移动端推送实时通知。

## 错误处理

### 常见错误码
| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| 200 | 成功 | 操作成功 |
| 400 | 参数错误 | 检查请求参数 |
| 401 | 未授权 | 检查Token是否有效 |
| 403 | 权限不足 | 检查用户权限 |
| 404 | 订单不存在 | 检查订单ID是否正确 |
| 409 | 状态冲突 | 检查订单当前状态 |
| 500 | 服务器错误 | 联系管理员 |

### 错误响应示例
```json
{
  "code": 409,
  "message": "订单当前状态不允许删除",
  "data": null,
  "timestamp": "2026-04-27T16:40:00Z"
}
```

## 测试用例

### 测试1：查询订单列表
```bash
curl -X GET "http://test-ai-ready.example.com/api/order/page?pageNum=1&pageSize=10&tenantId=1" \
  -H "Authorization: Bearer {accessToken}"
```

### 测试2：创建订单
```bash
curl -X POST "http://test-ai-ready.example.com/api/order" \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "customerId": 1,
    "orderType": 0,
    "remark": "测试订单",
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "unitPrice": 999.99
      }
    ]
  }'
```

### 测试3：更新订单状态
```bash
curl -X PUT "http://test-ai-ready.example.com/api/order/1/status?status=1" \
  -H "Authorization: Bearer {accessToken}"
```

## 移动端SDK集成

### Android示例
```kotlin
// 订单服务初始化
val orderService = OrderService.Builder()
    .baseUrl("http://test-ai-ready.example.com")
    .authTokenProvider { getAccessToken() }
    .offlineStorageEnabled(true)
    .build()

// 查询订单列表
orderService.getOrderList(pageNum = 1, pageSize = 10, tenantId = 1) { result ->
    when (result) {
        is Success -> {
            val orders = result.data.list
            // 显示订单列表
        }
        is Failure -> {
            // 处理查询失败
        }
    }
}

// 创建订单（支持离线）
val order = Order(
    tenantId = 1,
    customerId = 1,
    orderType = 0,
    items = listOf(
        OrderItem(productId = 1, quantity = 2, unitPrice = 999.99)
    )
)

orderService.createOrder(order, offline = true) { result ->
    when (result) {
        is Success -> {
            val orderId = result.data
            // 订单创建成功
        }
        is Failure -> {
            // 处理创建失败
        }
    }
}
```

### iOS示例
```swift
// 订单服务初始化
let orderService = OrderService(
    baseURL: URL(string: "http://test-ai-ready.example.com")!,
    tokenProvider: { getAccessToken() },
    offlineStorageEnabled: true
)

// 查询订单列表
orderService.getOrderList(pageNum: 1, pageSize: 10, tenantId: 1) { result in
    switch result {
    case .success(let response):
        let orders = response.list
        // 显示订单列表
    case .failure(let error):
        // 处理查询失败
    }
}

// 创建订单（支持离线）
let order = Order(
    tenantId: 1,
    customerId: 1,
    orderType: 0,
    items: [
        OrderItem(productId: 1, quantity: 2, unitPrice: 999.99)
    ]
)

orderService.createOrder(order, offline: true) { result in
    switch result {
    case .success(let orderId):
        // 订单创建成功
    case .failure(let error):
        // 处理创建失败
    }
}
```

## 性能优化

### 1. 分页查询优化
- 支持游标分页（Cursor-based Pagination）
- 数据压缩传输
- 图片懒加载

### 2. 缓存策略
- 订单列表缓存（5分钟）
- 订单详情缓存（10分钟）
- 缓存自动刷新

### 3. 离线模式
- 本地数据库存储
- 冲突解决机制
- 批量同步优化

## 注意事项
1. 移动端订单创建支持离线模式，但审核、发货等操作需要在线
2. 订单删除仅限于草稿状态
3. 订单状态变更会触发推送通知
4. 敏感操作需要二次确认
5. 支持订单导出功能（PDF/Excel）

---
*文档最后更新：2026-04-27*
*API版本：v1.0.0*
*适用于：Sprint 27+1 测试环境移动端开发*