# 测试环境API接口文档

**版本**: 1.0.0  
**创建日期**: 2026-04-27  
**作者**: doc-writer  
**最后更新**: 2026-04-27

## 目录

1. [概述](#概述)
2. [API基础规范](#api基础规范)
3. [认证授权](#认证授权)
4. [用户管理API](#用户管理api)
5. [订单管理API](#订单管理api)
6. [库存管理API](#库存管理api)
7. [支付管理API](#支付管理api)
8. [监控告警API](#监控告警api)
9. [测试专用API](#测试专用api)
10. [API错误处理](#api错误处理)
11. [API版本管理](#api版本管理)
12. [API测试指南](#api测试指南)

---

## 概述

### 文档目标

本文档旨在为测试环境提供完整的API接口规范，包括：

1. **接口定义**: 所有API的详细说明
2. **使用示例**: 实际调用示例和代码片段
3. **测试数据**: 测试环境专用数据和配置
4. **错误处理**: 统一的错误响应格式
5. **最佳实践**: API使用的最佳实践建议

### 环境信息

| 项目 | 测试环境 | 说明 |
|------|----------|------|
| **基础URL** | `http://test-env.ai-ready.local:8080` | API网关地址 |
| **API版本** | v1 | 当前API版本 |
| **环境标识** | test | 测试环境标识 |
| **数据隔离** | 独立数据库 | 与生产环境隔离 |
| **认证方式** | JWT + API Key | 双重认证可选 |

### 快速开始

#### 1. 获取访问令牌
```bash
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tester@ai-ready.local",
    "password": "Tester@2024"
  }'
```

#### 2. 调用API接口
```bash
# 使用获取的Token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 调用用户信息接口
curl -X GET "http://test-env.ai-ready.local:8080/api/v1/users/me" \
  -H "Authorization: Bearer $TOKEN"
```

#### 3. 查看API文档
```
# Swagger UI文档
http://test-env.ai-ready.local:8080/swagger-ui.html

# OpenAPI规范
http://test-env.ai-ready.local:8080/v3/api-docs
```

## API基础规范

### 请求规范

#### 1. 请求头
```http
# 标准请求头
Content-Type: application/json
Accept: application/json
Authorization: Bearer <jwt_token>
X-API-Key: <api_key>  # 可选，用于自动化测试
X-Request-ID: <request_id>  # 请求追踪
X-Test-Environment: test  # 环境标识

# 测试专用头
X-Test-User-ID: user_001  # 测试用户ID
X-Test-Scenario: load_test  # 测试场景标识
X-Bypass-Cache: true  # 绕过缓存
```

#### 2. 请求方法
| 方法 | 用途 | 幂等性 |
|------|------|--------|
| **GET** | 获取资源 | 是 |
| **POST** | 创建资源 | 否 |
| **PUT** | 更新资源（全量） | 是 |
| **PATCH** | 更新资源（部分） | 否 |
| **DELETE** | 删除资源 | 是 |
| **HEAD** | 获取头部信息 | 是 |
| **OPTIONS** | 获取支持方法 | 是 |

#### 3. URL规范
```http
# 基础URL模式
{protocol}://{host}:{port}/api/{version}/{resource}[/{id}][/{sub-resource}]

# 示例
GET /api/v1/users
GET /api/v1/users/123
GET /api/v1/users/123/orders
POST /api/v1/users/123/orders

# 查询参数
GET /api/v1/orders?status=completed&page=1&size=20&sort=created_at,desc
```

#### 4. 请求体格式
```json
{
  "field1": "value1",
  "field2": "value2",
  "nested": {
    "field3": "value3"
  },
  "arrayField": [
    "item1",
    "item2"
  ]
}
```

### 响应规范

#### 1. 响应状态码
| 状态码 | 含义 | 说明 |
|--------|------|------|
| **200 OK** | 成功 | 请求成功 |
| **201 Created** | 创建成功 | 资源创建成功 |
| **204 No Content** | 无内容 | 成功但无响应体 |
| **400 Bad Request** | 请求错误 | 参数验证失败 |
| **401 Unauthorized** | 未授权 | 认证失败 |
| **403 Forbidden** | 禁止访问 | 权限不足 |
| **404 Not Found** | 资源不存在 | 请求的资源不存在 |
| **409 Conflict** | 冲突 | 资源状态冲突 |
| **429 Too Many Requests** | 请求过多 | 限流触发 |
| **500 Internal Server Error** | 服务器错误 | 服务器内部错误 |
| **503 Service Unavailable** | 服务不可用 | 服务维护或过载 |

#### 2. 响应头
```http
# 标准响应头
Content-Type: application/json
Content-Length: 1234
X-Request-ID: <request_id>
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1633024800
X-Trace-ID: trace_123456
X-Response-Time: 150ms

# 分页相关
X-Page: 1
X-Per-Page: 20
X-Total: 100
X-Total-Pages: 5
```

#### 3. 响应体格式

##### 成功响应
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {
    // 实际数据
  },
  "timestamp": "2026-04-27T10:30:45.123Z",
  "requestId": "req_123456"
}
```

##### 分页响应
```json
{
  "success": true,
  "code": 200,
  "message": "查询成功",
  "data": {
    "items": [
      // 数据项数组
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "pages": 5
    }
  },
  "timestamp": "2026-04-27T10:30:45.123Z"
}
```

##### 错误响应
```json
{
  "success": false,
  "code": 400,
  "message": "请求参数验证失败",
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空",
      "code": "VALIDATION_ERROR"
    },
    {
      "field": "email",
      "message": "邮箱格式不正确",
      "code": "INVALID_EMAIL"
    }
  ],
  "timestamp": "2026-04-27T10:30:45.123Z",
  "requestId": "req_123456",
  "path": "/api/v1/users",
  "traceId": "trace_123456"
}
```

### 分页和排序

#### 分页参数
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | Integer | 1 | 页码，从1开始 |
| `size` | Integer | 20 | 每页大小，最大100 |
| `offset` | Integer | 0 | 偏移量，与page互斥 |
| `limit` | Integer | 20 | 限制数量，与size相同 |

#### 排序参数
```http
# 单字段排序
GET /api/v1/users?sort=created_at,desc

# 多字段排序
GET /api/v1/users?sort=last_name,asc&sort=first_name,asc

# 复杂排序
GET /api/v1/orders?sort=status,asc&sort=created_at,desc
```

#### 分页示例
```bash
# 第一页，每页20条
curl "http://test-env.ai-ready.local:8080/api/v1/users?page=1&size=20"

# 使用偏移量
curl "http://test-env.ai-ready.local:8080/api/v1/users?offset=40&limit=20"

# 带排序
curl "http://test-env.ai-ready.local:8080/api/v1/users?page=1&size=20&sort=created_at,desc"
```

### 字段选择和过滤

#### 字段选择
```http
# 选择特定字段
GET /api/v1/users?fields=id,username,email

# 排除特定字段
GET /api/v1/users?exclude=password,salt

# 嵌套字段选择
GET /api/v1/users/123/orders?fields=id,amount,user{id,username}
```

#### 过滤条件
```http
# 等于过滤
GET /api/v1/users?status=active

# 不等于过滤
GET /api/v1/users?status[ne]=inactive

# 大于小于过滤
GET /api/v1/users?created_at[gt]=2026-01-01&created_at[lt]=2026-12-31

# IN查询
GET /api/v1/users?status[in]=active,pending

# LIKE查询
GET /api/v1/users?username[like]=admin%

# 复合条件
GET /api/v1/orders?status=completed&amount[gt]=100&created_at[gte]=2026-01-01
```

## 认证授权

### 认证方式

#### 1. JWT认证（推荐）
```bash
# 获取JWT令牌
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "tester@ai-ready.local",
    "password": "Tester@2024"
  }'

# 响应示例
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshToken": "refresh_token_123",
    "user": {
      "id": "user_001",
      "username": "tester@ai-ready.local",
      "roles": ["ROLE_TESTER"]
    }
  }
}
```

#### 2. API Key认证
```bash
# 使用API Key
curl -X GET "http://test-env.ai-ready.local:8080/api/v1/users" \
  -H "X-API-Key: test_api_key_123456"

# 生成API Key
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/api-keys" \
  -H "Authorization: Bearer <jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "自动化测试",
    "permissions": ["user:read", "order:read"]
  }'
```

#### 3. Basic认证（仅测试）
```bash
# Basic认证（不推荐生产环境）
curl -u "tester:Tester@2024" \
  "http://test-env.ai-ready.local:8080/api/v1/users"
```

### 测试环境专用认证

#### 测试用户账户
| 角色 | 用户名 | 密码 | 权限 | 用途 |
|------|--------|------|------|------|
| **管理员** | admin@ai-ready.local | Admin@2024 | 所有权限 | 环境管理 |
| **测试工程师** | tester@ai-ready.local | Tester@2024 | 测试相关 | 功能测试 |
| **开发工程师** | developer@ai-ready.local | Developer@2024 | 开发相关 | 开发调试 |
| **自动化测试** | auto_test@ai-ready.local | AutoTest@2024 | 只读权限 | 自动化测试 |
| **性能测试** | perf_test@ai-ready.local | PerfTest@2024 | 读写权限 | 性能测试 |

#### 预置API Key
| Key名称 | API Key | 权限 | 用途 |
|---------|---------|------|------|
| **自动化测试** | `test_auto_key_123` | 只读 | 自动化回归测试 |
| **性能测试** | `test_perf_key_456` | 读写 | 性能压力测试 |
| **安全测试** | `test_security_key_789` | 受限 | 安全漏洞测试 |
| **监控测试** | `test_monitor_key_012` | 只读 | 监控告警测试 |

### 权限控制

#### 权限层级
```yaml
# 权限定义
permissions:
  # 用户管理权限
  user:
    read: "读取用户信息"
    write: "创建/更新用户"
    delete: "删除用户"
    
  # 订单管理权限
  order:
    read: "读取订单信息"
    write: "创建/更新订单"
    delete: "删除订单"
    approve: "审核订单"
    
  # 库存管理权限
  inventory:
    read: "读取库存信息"
    adjust: "调整库存"
    alert: "管理库存告警"
    
  # 支付管理权限
  payment:
    read: "读取支付信息"
    process: "处理支付"
    refund: "退款处理"
    
  # 系统管理权限
  system:
    config: "系统配置"
    monitor: "监控查看"
    admin: "管理员权限"
```

#### 角色定义
```yaml
# 角色权限映射
roles:
  ROLE_ADMIN:
    - user:*
    - order:*
    - inventory:*
    - payment:*
    - system:*
    
  ROLE_TESTER:
    - user:read
    - order:read
    - order:write
    - inventory:read
    - payment:read
    
  ROLE_DEVELOPER:
    - user:read
    - order:*
    - inventory:read
    - payment:read
    - system:monitor
    
  ROLE_AUTO_TEST:
    - user:read
    - order:read
    - inventory:read
    
  ROLE_PERF_TEST:
    - user:*
    - order:*
    - inventory:*
    - payment:*
```

### 刷新令牌

#### 令牌刷新流程
```bash
# 1. 使用刷新令牌获取新访问令牌
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "refresh_token_123"
  }'

# 2. 响应示例
{
  "success": true,
  "code": 200,
  "message": "令牌刷新成功",
  "data": {
    "token": "new_access_token_456",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshToken": "new_refresh_token_789"
  }
}
```

#### 令牌失效
```bash
# 注销当前令牌
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/logout" \
  -H "Authorization: Bearer <current_token>"

# 强制注销所有令牌
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/logout-all" \
  -H "Authorization: Bearer <current_token>"
```

## 用户管理API

### 用户注册

#### 注册新用户
```http
POST /api/v1/users/register
Content-Type: application/json

{
  "username": "newuser@test.com",
  "password": "Password123!",
  "email": "newuser@test.com",
  "fullName": "测试用户",
  "phone": "13800138000",
  "avatar": "https://example.com/avatar.jpg"
}
```

#### 响应示例
```json
{
  "success": true,
  "code": 201,
  "message": "用户注册成功",
  "data": {
    "id": "user_1001",
    "username": "newuser@test.com",
    "email": "newuser@test.com",
    "fullName": "测试用户",
    "status": "ACTIVE",
    "createdAt": "2026-04-27T10:30:45.123Z",
    "updatedAt": "2026-04-27T10:30:45.123Z"
  }
}
```

#### 测试环境专用注册
```bash
# 快速注册测试用户（跳过邮箱验证）
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/users/register-test" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser_001",
    "password": "Test123!",
    "role": "ROLE_TESTER"
  }'
```

### 用户登录

#### 标准登录
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "tester@ai-ready.local",
  "password": "Tester@2024"
}
```

#### 测试环境快速登录
```bash
# 使用测试专用账户（无需密码）
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/auth/test-login" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test_user_001",
    "role": "ROLE_TESTER"
  }'
```

### 用户信息管理

#### 获取当前用户信息
```http
GET /api/v1/users/me
Authorization: Bearer <token>
```

#### 获取指定用户信息
```http
GET /api/v1/users/{userId}
Authorization: Bearer <token>
```

#### 更新用户信息
```http
PUT /api/v1/users/{userId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "更新后的姓名",
  "phone": "13900139000",
  "avatar": "https://example.com/new-avatar.jpg"
}
```

#### 批量查询用户
```http
GET /api/v1/users
Authorization: Bearer <token>

# 查询参数
?page=1&size=20&sort=created_at,desc&status=ACTIVE&username[like]=test%
```

### 用户角色和权限

#### 获取用户角色
```http
GET /api/v1/users/{userId}/roles
Authorization: Bearer <token>
```

#### 分配用户角色
```http
POST /api/v1/users/{userId}/roles
Authorization: Bearer <token>
Content-Type: application/json

{
  "roles": ["ROLE_TESTER", "ROLE_DEVELOPER"]
}
```

#### 获取用户权限
```http
GET /api/v1/users/{userId}/permissions
Authorization: Bearer <token>
```

### 测试环境专用API

#### 批量创建测试用户
```bash
# 创建10个测试用户
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/test/users/batch-create" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 10,
    "role": "ROLE_TESTER",
    "prefix": "test_user_"
  }'
```

#### 重置测试用户密码
```bash
# 重置所有测试用户密码
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/test/users/reset-passwords" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "ROLE_TESTER",
    "newPassword": "Test123!"
  }'
```

#### 清理测试用户数据
```bash
# 清理指定时间前的测试用户
curl -X DELETE "http://test-env.ai-ready.local:8080/api/v1/test/users/cleanup" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "createdBefore": "2026-04-20T00:00:00Z",
    "role": "ROLE_TESTER"
  }'
```

## 订单管理API

### 订单创建

#### 创建新订单
```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "user_001",
  "items": [
    {
      "productId": "prod_001",
      "quantity": 2,
      "unitPrice": 99.99
    },
    {
      "productId": "prod_002", 
      "quantity": 1,
      "unitPrice": 199.99
    }
  ],
  "shippingAddress": {
    "recipient": "张三",
    "phone": "13800138000",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detail": "某某街道123号"
  },
  "paymentMethod": "ALIPAY",
  "remarks": "测试订单，请优先处理"
}
```

#### 响应示例
```json
{
  "success": true,
  "code": 201,
  "message": "订单创建成功",
  "data": {
    "id": "order_202604270001",
    "orderNumber": "ORD202604270001",
    "userId": "user_001",
    "totalAmount": 299.97,
    "status": "PENDING",
    "items": [
      {
        "productId": "prod_001",
        "productName": "测试产品A",
        "quantity": 2,
        "unitPrice": 99.99,
        "subtotal": 199.98
      },
      {
        "productId": "prod_002",
        "productName": "测试产品B",
        "quantity": 1,
        "unitPrice": 199.99,
        "subtotal": 199.99
      }
    ],
    "createdAt": "2026-04-27T10:30:45.123Z",
    "updatedAt": "2026-04-27T10:30:45.123Z"
  }
}
```

### 订单查询

#### 查询订单列表
```http
GET /api/v1/orders
Authorization: Bearer <token>

# 查询参数
?userId=user_001&status=PENDING&page=1&size=20&sort=created_at,desc
```

#### 查询订单详情
```http
GET /api/v1/orders/{orderId}
Authorization: Bearer <token>
```

#### 查询用户订单
```http
GET /api/v1/users/{userId}/orders
Authorization: Bearer <token>
```

### 订单状态管理

#### 更新订单状态
```http
PATCH /api/v1/orders/{orderId}/status
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "PROCESSING",
  "remark": "开始处理订单"
}
```

#### 订单状态流转
```yaml
# 订单状态定义
status_flow:
  PENDING: 待处理
  PROCESSING: 处理中
  SHIPPED: 已发货
  DELIVERED: 已送达
  COMPLETED: 已完成
  CANCELLED: 已取消
  REFUNDED: 已退款
```

### 订单支付

#### 创建支付订单
```http
POST /api/v1/orders/{orderId}/payment
Authorization: Bearer <token>
Content-Type: application/json

{
  "paymentMethod": "ALIPAY",
  "paymentAmount": 299.97
}
```

#### 查询支付状态
```http
GET /api/v1/orders/{orderId}/payment
Authorization: Bearer <token>
```

#### 支付回调处理
```http
POST /api/v1/payments/callback/{gateway}
Content-Type: application/json

{
  "orderId": "order_202604270001",
  "paymentId": "pay_123456",
  "status": "SUCCESS",
  "amount": 299.97,
  "paidAt": "2026-04-27T10:35:00.000Z",
  "gatewayData": {
    // 支付网关返回的原始数据
  }
}
```

### 测试环境专用API

#### 批量创建测试订单
```bash
# 创建100个测试订单
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/test/orders/batch-create" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "count": 100,
    "userId": "user_001",
    "statusDistribution": {
      "PENDING": 30,
      "PROCESSING": 40,
      "SHIPPED": 20,
      "COMPLETED": 10
    }
  }'
```

#### 模拟订单状态流转
```bash
# 模拟订单状态变化（用于测试工作流）
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/test/orders/simulate-flow" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order_202604270001",
    "steps": [
      {"status": "PROCESSING", "delay": 5000},
      {"status": "SHIPPED", "delay": 10000},
      {"status": "DELIVERED", "delay": 15000},
      {"status": "COMPLETED", "delay": 20000}
    ]
  }'
```

#### 清理测试订单数据
```bash
# 清理测试订单
curl -X DELETE "http://test-env.ai-ready.local:8080/api/v1/test/orders/cleanup" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "createdBefore": "2026-04-20T00:00:00Z",
    "keepRecent": 1000
  }'
```

## 库存管理API

### 库存查询

#### 查询产品库存
```http
GET /api/v1/inventory/products/{productId}
Authorization: Bearer <token>
```

#### 批量查询库存
```http
GET /api/v1/inventory/products
Authorization: Bearer <token>

# 查询参数
?productIds=prod_001,prod_002,prod_003&warehouseId=wh_001
```

#### 查询库存历史
```http
GET /api/v1/inventory/products/{productId}/history
Authorization: Bearer <token>

# 查询参数
?startDate=2026-04-01&endDate=2026-04-27&page=1&size=50
```

### 库存调整

#### 调整库存数量
```http
POST /api/v1/inventory/adjust
Authorization: Bearer <token>
Content-Type: application/json

{
  "productId": "prod_001",
  "warehouseId": "wh_001",
  "quantity": -10,
  "adjustType": "SALE",
  "referenceId": "order_202604270001",
  "remark": "销售出库"
}
```

#### 批量调整库存
```http
POST /api/v1/inventory/batch-adjust
Authorization: Bearer <token>
Content-Type: application/json

{
  "adjustments": [
    {
      "productId": "prod_001",
      "quantity": -5,
      "adjustType": "SALE"
    },
    {
      "productId": "prod_002",
      "quantity": 100,
      "adjustType": "PURCHASE"
    }
  ],
  "warehouseId": "wh_001",
  "remark": "批量库存调整"
}
```

### 库存预警

#### 设置库存预警
```http
POST /api/v1/inventory/alerts
Authorization: Bearer <token>
Content-Type: application/json

{
  "productId": "prod_001",
  "warehouseId": "wh_001",
  "threshold": 50,
  "alertType": "LOW_STOCK",
  "notificationChannels": ["EMAIL", "WECHAT"],
  "recipients": ["manager@ai-ready.local"]
}
```

#### 查询库存预警
```http
GET /api/v1/inventory/alerts
Authorization: Bearer <token>

# 查询参数
?status=ACTIVE&alertType=LOW_STOCK&page=1&size=20
```

### 库存盘点

#### 创建盘点任务
```http
POST /api/v1/inventory/stocktakes
Authorization: Bearer <token>
Content-Type: application/json

{
  "warehouseId": "wh_001",
  "productIds": ["prod_001", "prod_002", "prod_003"],
  "plannedDate": "2026-04-28",
  "assignee": "user_002",
  "remark": "月度盘点"
}
```

#### 提交盘点结果
```http
POST /api/v1/inventory/stocktakes/{stocktakeId}/results
Authorization: Bearer <token>
Content-Type: application/json

{
  "results": [
    {
      "productId": "prod_001",
      "actualQuantity": 95,
      "systemQuantity": 100,
      "difference": -5,
      "remark": "销售未及时出库"
    },
    {
      "productId": "prod_002",
      "actualQuantity": 150,
      "systemQuantity": 150,
      "difference": 0,
      "remark": "库存准确"
    }
  ]
}
```

### 测试环境专用API

#### 初始化测试库存
```bash
# 初始化测试库存数据
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/test/inventory/init" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "products": [
      {"id": "prod_001", "name": "测试产品A", "initialStock": 1000},
      {"id": "prod_002", "name": "测试产品B", "initialStock": 500},
      {"id": "prod_003", "name": "测试产品C", "initialStock": 200}
    ],
    "warehouseId": "wh_001"
  }'
```

#### 模拟库存操作
```bash
# 模拟库存操作（用于测试并发）
curl -X POST "http://test-env.ai-ready.local:8080/api/v1/test/inventory/simulate" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod_001",
    "operations": [
      {"type": "sale", "quantity": -10, "delay": 100},
      {"type": "purchase", "quantity": 50, "delay": 200},
      {"type": "return", "quantity": 5, "delay": 150}
    ],
    "concurrent": 10
  }'
```

#### 生成库存报告
```bash
# 生成库存测试报告
curl -X GET "http://test-env.ai-ready.local:8080/api/v1/test/inventory/report" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "startDate": "2026-04-01",
    "endDate": "2026-04-27",
    "format": "pdf"
  }'
```

## 支付管理API

### 支付创建

#### 创建支付订单
```http
POST /api/v1/payments
Authorization: Bearer <token>
Content-Type: application/json

{
  "orderId": "order_202604270001",
  "amount": 299.97,
  "currency": "CNY",
  "paymentMethod": "ALIPAY",
  "payerInfo": {
    "userId": "user_001",
    "name": "张三",
    "email": "zhangsan@test.com"
  },
  "callbackUrl": "http://test-env.ai-ready.local:8080/api/v1/payments/callback",
  "returnUrl": "http://test-env.ai-ready.local:8080/orders/success"
}
```

#### 响应示例
```json
{
  "success": true,
  "code": 201,
  "message": "支付订单创建成功",
  "data": {
    "paymentId": "pay_123456",
    "orderId": "order_202604270001",
    "amount": 299.97,
    "currency": "CNY",
    "status": "PENDING",
    "paymentUrl": "https://alipay.com/pay?order=123456",
    "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg...",
    "expiresAt": "2026-04-27T11:30:45.123Z",
    "createdAt": "2026-04-27T10:30:45.123Z"
  }
}
```

### 支付查询

#### 查询支付状态
```http
GET /api/v1/payments/{paymentId}
Authorization: Bearer <token>
```

#### 查询订单支付记录
```http
GET /api/v1/orders/{orderId}/payments
Authorization: Bearer <token>
```

#### 查询用户支付历史
```http
GET /api/v1/users/{userId}/payments
Authorization: Bearer <token>

# 查询参数
?startDate=2026-04-01&endDate=2026-04-27&status=SUCCESS&page=1&size=20
```

### 支付回调

#### 支付成功回调
```http
POST /api/v1/payments/callback/alipay
Content-Type: application/json

{
  "trade_no": "202604271030001",
  "out_trade_no": "pay_123456",
  "trade_status": "TRADE_SUCCESS",
  "total_amount": "299.97",
  "receipt_amount": "299.97",
  "buyer_pay_amount": "299.97",
  "gmt_payment": "2026-04-27 10:35:00",
  "sign": "signature_string",
  "sign_type": "RSA2"
}
```

#### 支付失败回调
```http
POST /api/v1/payments/callback/alipay
Content-Type: application/json

{
  "trade_no": "202604271030001",
  "out_trade_no": "pay_123456",
  "trade_status": "TRADE_CLOSED",
  "total_amount": "299.97",
  "gmt_close": "2026-04-27 10:40:00",
  "sign": "signature_string",
  "sign_type": "RSA2"
}
```

### 退款管理

#### 创建退款申请
```http
POST /api/v1/payments/{paymentId}/refunds
Authorization: Bearer <token>
Content-Type: application/json

{
  "refundAmount": 299.97,
  "refundReason": "用户取消