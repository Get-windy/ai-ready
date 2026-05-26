# Webhook机制模块

## 概述

Webhook是事件推送通知机制，允许外部系统订阅系统事件并接收通知。

## 功能特性

### 核心功能
- ✅ **Webhook定义管理**：创建、编辑、删除Webhook
- ✅ **事件订阅**：订阅特定模型的事件
- ✅ **事件触发**：当事件发生时自动触发Webhook
- ✅ **调用日志**：记录Webhook调用历史
- ✅ **重试机制**：失败时自动重试
- ✅ **签名验证**：HMAC-SHA256签名验证
- ✅ **异步执行**：异步触发，不影响主流程

### 支持的触发事件
| 事件 | 说明 |
|------|------|
| ON_CREATE | 创建时触发 |
| ON_UPDATE | 更新时触发 |
| ON_DELETE | 删除时触发 |
| ON_STATE_CHANGE | 状态变更触发 |
| ON_FIELD_CHANGE | 字段变更触发 |
| ON_SCHEDULED | 定时触发 |

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **HTTP客户端**：Hutool HttpUtil
- **签名算法**：HMAC-SHA256

## API接口

### Webhook管理 `/api/webhook`
- `POST` - 创建Webhook
- `PUT /{id}` - 更新Webhook
- `GET /{id}` - 获取Webhook详情
- `GET /model/{modelName}` - 获取模型的所有Webhook
- `GET /model/{modelName}/event/{triggerEvent}` - 获取特定事件的Webhook
- `GET /list` - Webhook列表查询
- `DELETE /{id}` - 删除Webhook
- `POST /{id}/activate` - 激活Webhook
- `POST /{id}/deactivate` - 停用Webhook

### 触发与日志 `/api/webhook`
- `POST /trigger` - 手动触发Webhook
- `GET /{webhookId}/logs` - 获取Webhook日志
- `GET /logs/list` - 日志列表查询
- `POST /retry` - 重试失败的Webhook
- `POST /verify-signature` - 验证签名

## 使用示例

### 1. 创建Webhook
```json
POST /api/webhook
{
  "webhookName": "订单创建通知",
  "modelName": "SaleOrder",
  "triggerEvent": "ON_CREATE",
  "targetUrl": "https://example.com/webhook/order-created",
  "method": "POST",
  "secretKey": "your-secret-key",
  "active": true,
  "maxRetry": 3,
  "timeout": 30000
}
```

### 2. Webhook Payload格式
```json
{
  "webhookCode": "WH00001",
  "modelName": "SaleOrder",
  "triggerEvent": "ON_CREATE",
  "recordId": 123,
  "data": {
    "orderNo": "SO20260001",
    "amount": 10000,
    "customerName": "张三"
  },
  "timestamp": "2026-05-25T10:00:00",
  "signature": "a1b2c3d4..."
}
```

### 3. 签名验证
外部系统收到Webhook后，可使用secretKey验证签名：
```java
String expectedSignature = SecureUtil.hmacSha256(secretKey).digestHex(payloadJson);
boolean valid = expectedSignature.equals(signature);
```

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_webhook_tables.sql`

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问地址
- 服务地址：http://localhost:8102
- API文档：http://localhost:8102/doc.html

## 许可证

Apache License 2.0