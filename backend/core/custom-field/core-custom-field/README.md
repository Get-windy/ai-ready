# 动态字段系统模块

## 概述

动态字段系统是Odoo的核心特性，允许用户在运行时动态添加自定义字段，无需修改代码或数据库结构。

## 功能特性

### 核心功能
- ✅ **字段定义管理**：创建、编辑、删除自定义字段
- ✅ **字段类型支持**：字符串、整数、浮点数、布尔值、日期、文本、选择字段、JSON等
- ✅ **字段分组**：按分组组织字段，便于UI展示
- ✅ **字段值存储**：支持多种数据类型的值存储
- ✅ **字段搜索**：支持按自定义字段搜索业务记录
- ✅ **多租户隔离**：字段定义和值按租户隔离

### 支持的字段类型
| 类型 | 说明 | 存储字段 |
|------|------|----------|
| STRING | 字符串 | value_string |
| INTEGER | 整数 | value_integer |
| LONG | 长整数 | value_long |
| DOUBLE | 浮点数 | value_double |
| BOOLEAN | 布尔值 | value_boolean |
| DATE | 日期 | value_date |
| DATETIME | 日期时间 | value_date |
| TEXT | 长文本 | value_text |
| SELECTION | 选择字段 | value_string |
| MULTI_SELECTION | 多选字段 | value_json |
| JSON | JSON对象 | value_json |
| FILE | 文件 | value_string |
| IMAGE | 图片 | value_string |

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **缓存**：Redis
- **API文档**：Knife4j

## API接口

### 字段定义管理 `/api/custom-field`
- `POST` - 创建自定义字段
- `PUT /{id}` - 更新字段
- `GET /{id}` - 获取字段详情
- `GET /model/{modelName}` - 获取模型的所有字段
- `GET /model/{modelName}/group/{groupCode}` - 获取模型分组的字段
- `GET /list` - 字段列表查询
- `DELETE /{id}` - 删除字段
- `POST /{id}/activate` - 激活字段
- `POST /{id}/deactivate` - 停用字段
- `GET /model/{modelName}/searchable` - 获取可搜索字段
- `GET /validate` - 验证字段名称

### 字段分组 `/api/custom-field/group`
- `GET /model/{modelName}` - 获取字段分组
- `POST` - 创建字段分组
- `DELETE /{id}` - 删除字段分组

### 字段值管理 `/api/custom-field-value`
- `POST /save` - 保存单个字段值
- `POST /save-batch` - 批量保存字段值（按字段ID）
- `POST /save-by-name` - 批量保存字段值（按字段名）
- `GET /get` - 获取单个字段值
- `GET /get-by-name` - 按字段名获取字段值
- `GET /all` - 获取记录的所有字段值
- `DELETE /delete` - 删除记录的所有字段值
- `GET /search` - 按字段搜索记录
- `POST /copy` - 复制字段值

## 使用示例

### 1. 创建自定义字段
```json
POST /api/custom-field
{
  "modelName": "Party",
  "fieldName": "custom_grade",
  "fieldType": "STRING",
  "fieldLabel": "自定义等级",
  "searchable": true,
  "sortable": true
}
```

### 2. 保存字段值
```json
POST /api/custom-field-value/save-by-name?modelName=Party&recordId=123
{
  "custom_grade": "A级",
  "credit_limit": 50000.00,
  "is_vip": true
}
```

### 3. 获取字段值
```
GET /api/custom-field-value/all?modelName=Party&recordId=123
```

### 4. 搜索记录
```
GET /api/custom-field-value/search?modelName=Party&fieldName=custom_grade&value=A级
```

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_custom_field_tables.sql`

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问地址
- 服务地址：http://localhost:8098
- API文档：http://localhost:8098/doc.html

## 许可证

Apache License 2.0