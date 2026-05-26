# 自动化规则引擎模块

## 概述

自动化规则引擎是Odoo的核心特性，允许自动触发业务动作，减少人工操作，提升业务效率。

## 功能特性

### 核心功能
- ✅ **规则定义管理**：创建、编辑、删除自动化规则
- ✅ **多种触发类型**：创建时、更新时、删除时、定时、字段变更、状态变更
- ✅ **多种动作类型**：更新字段、发送消息、发送通知、设置状态、创建活动等
- ✅ **条件评估**：支持复杂的触发条件配置
- ✅ **执行日志**：完整的执行记录和错误追踪
- ✅ **优先级控制**：按优先级顺序执行规则

### 支持的触发类型
| 类型 | 说明 | 触发时机 |
|------|------|----------|
| ON_CREATE | 创建时触发 | 新记录创建时 |
| ON_WRITE | 更新时触发 | 记录更新时 |
| ON_DELETE | 删除时触发 | 记录删除时 |
| ON_TIME | 定时触发 | 按计划执行 |
| ON_CHANGE | 字段变更触发 | 特定字段变更时 |
| ON_STATE_CHANGE | 状态变更触发 | 状态字段变更时 |

### 支持的动作类型
| 类型 | 说明 |
|------|------|
| WRITE | 更新字段值 |
| CREATE | 创建新记录 |
| DELETE | 删除记录 |
| SEND_EMAIL | 发送邮件 |
| SEND_MESSAGE | 发送消息 |
| SEND_NOTIFICATION | 发送通知 |
| EXECUTE_METHOD | 执行方法 |
| CREATE_ACTIVITY | 创建活动 |
| ADD_FOLLOWER | 添加关注者 |
| SET_STATE | 设置状态 |
| WEBHOOK | 调用Webhook |

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **缓存**：Redis
- **API文档**：Knife4j

## API接口

### 规则管理 `/api/automation`
- `POST` - 创建自动化规则
- `PUT /{id}` - 更新规则
- `GET /{id}` - 获取规则详情
- `GET /model/{modelName}` - 获取模型的所有规则
- `GET /model/{modelName}/trigger/{triggerType}` - 获取特定触发类型的规则
- `GET /list` - 规则列表查询
- `DELETE /{id}` - 删除规则
- `POST /{id}/activate` - 激活规则
- `POST /{id}/deactivate` - 停用规则

### 触发执行 `/api/automation/trigger`
- `POST /create` - 手动触发创建规则
- `POST /write` - 手动触发更新规则
- `POST /delete` - 手动触发删除规则
- `POST /scheduled` - 执行定时规则

## 使用示例

### 1. 创建自动化规则
```json
POST /api/automation
{
  "ruleName": "新客户欢迎通知",
  "modelName": "Party",
  "triggerType": "ON_CREATE",
  "triggerCondition": "{\"type\": \"field_value\", \"field\": \"partyType\", \"operator\": \"=\", \"value\": \"CUSTOMER\"}",
  "actionType": "SEND_NOTIFICATION",
  "actionConfig": "{\"template\": \"welcome_customer\", \"channels\": [\"email\", \"sms\"]}",
  "active": true,
  "priority": 10
}
```

### 2. 触发条件配置示例
```json
// 字段值条件
{"type": "field_value", "field": "amount", "operator": ">=", "value": 10000}

// 字段变更条件
{"type": "field_change", "field": "state"}

// 状态变更条件
{"type": "state_change", "from_state": "DRAFT", "to_state": "CONFIRMED"}
```

### 3. 动作配置示例
```json
// 更新字段
{"fields": {"grade": "VIP", "discount": 0.1}}

// 发送通知
{"template": "order_confirmed", "recipients": ["customer", "sales_person"], "channels": ["email", "sms"]}
```

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_automation_tables.sql`

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问地址
- 服务地址：http://localhost:8099
- API文档：http://localhost:8099/doc.html

## 许可证

Apache License 2.0