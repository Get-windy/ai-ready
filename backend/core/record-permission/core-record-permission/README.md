# 记录级权限过滤和字段级权限控制模块

## 概述

记录级权限过滤和字段级权限控制是Odoo的核心安全特性，实现精细化的数据访问控制。

## 功能特性

### 记录级权限过滤
- ✅ **规则定义管理**：创建、编辑、删除记录规则
- ✅ **Domain过滤**：支持Odoo Domain格式的过滤条件
- ✅ **多维度规则**：支持全局规则、用户规则、角色规则
- ✅ **权限类型**：读取、写入、创建、删除四种权限
- ✅ **规则组合**：支持AND、OR、NOT逻辑组合

### 字段级权限控制
- ✅ **权限定义管理**：创建、编辑、删除字段权限
- ✅ **字段可见性**：控制字段是否可见
- ✅ **字段可编辑性**：控制字段是否可编辑
- ✅ **字段必填性**：控制字段是否必填
- ✅ **字段隐藏**：控制字段是否完全隐藏
- ✅ **多维度权限**：支持用户权限、角色权限

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **缓存**：Redis
- **API文档**：Knife4j

## API接口

### 记录级权限 `/api/permission/record`
- `POST` - 创建记录规则
- `PUT /{id}` - 更新规则
- `GET /{id}` - 获取规则详情
- `GET /model/{modelName}` - 获取模型的所有规则
- `GET /user/{userId}` - 获取用户的所有规则
- `GET /group/{groupId}` - 获取角色的所有规则
- `GET /list` - 规则列表查询
- `DELETE /{id}` - 删除规则
- `POST /{id}/activate` - 激活规则
- `POST /{id}/deactivate` - 停用规则
- `POST /build-domain` - 构建Domain过滤条件
- `POST /check-access` - 检查记录访问权限
- `POST /filter-records` - 过滤记录列表

### 字段级权限 `/api/permission/field`
- `POST` - 创建字段权限
- `PUT /{id}` - 更新权限
- `GET /{id}` - 获取权限详情
- `GET /model/{modelName}` - 获取模型的所有字段权限
- `GET /model/{modelName}/user/{userId}` - 获取用户在模型上的字段权限
- `GET /model/{modelName}/group/{groupId}` - 获取角色在模型上的字段权限
- `GET /list` - 权限列表查询
- `DELETE /{id}` - 删除权限
- `POST /get-all` - 获取用户在模型上的所有字段权限
- `POST /get-single` - 获取单个字段权限
- `POST /check-readable` - 检查字段是否可读
- `POST /check-writable` - 检查字段是否可写
- `POST /filter-fields` - 过滤记录字段
- `POST /apply` - 应用字段权限

## 使用示例

### 1. 创建记录规则
```json
POST /api/permission/record
{
  "ruleName": "个人记录规则",
  "modelName": "Party",
  "domain": "[[\"create_by\", \"=\", user_id]]",
  "permRead": true,
  "permWrite": true,
  "permCreate": true,
  "permDelete": true
}
```

### 2. Domain格式示例
```json
// 简单条件
["field", "operator", "value"]

// 组合条件（OR）
["|", ["field1", "=", "value1"], ["field2", "=", "value2"]]

// 组合条件（AND）
["&", ["field1", "=", "value1"], ["field2", "=", "value2"]]

// 否定条件
["!", ["field", "=", "value"]]
```

### 3. 创建字段权限
```json
POST /api/permission/field
{
  "modelName": "Party",
  "fieldName": "credit_limit",
  "readable": true,
  "writable": true,
  "required": false,
  "hidden": false
}
```

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_permission_tables.sql`

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问地址
- 服务地址：http://localhost:8100
- API文档：http://localhost:8100/doc.html

## 许可证

Apache License 2.0