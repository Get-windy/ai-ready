# 看板视图模块

## 概述

看板视图是Odoo的核心视图类型，用于卡片式任务管理，支持拖拽排序、分组显示。

## 功能特性

### 核心功能
- ✅ **卡片分组显示**：按状态、优先级等字段分组
- ✅ **卡片拖拽排序**：支持拖拽移动卡片到不同列
- ✅ **卡片快速编辑**：点击卡片快速编辑
- ✅ **列管理**：添加、编辑、删除列
- ✅ **卡片搜索**：搜索卡片标题和描述
- ✅ **优先级显示**：高/中/低优先级颜色标识
- ✅ **负责人显示**：显示卡片负责人头像
- ✅ **截止日期**：显示卡片截止日期
- ✅ **标签管理**：支持多标签

## 技术栈

- **前端**：Vue 3 + Ant Design Vue
- **后端**：Spring Boot 3.x + MyBatis-Plus
- **数据库**：PostgreSQL

## API接口

### 看板数据 `/api/kanban`
- `GET /{modelName}/data` - 获取看板数据
- `GET /{modelName}/columns` - 获取模型的列
- `POST /{modelName}/sync` - 同步模型数据到看板

### 列管理 `/api/kanban/column`
- `POST` - 创建列
- `PUT /{id}` - 更新列
- `DELETE /{id}` - 删除列
- `GET /{columnId}/cards` - 获取列的卡片

### 卡片管理 `/api/kanban/card`
- `POST` - 创建卡片
- `PUT /{id}` - 更新卡片
- `DELETE /{id}` - 删除卡片
- `POST /move` - 移动卡片
- `GET /list` - 卡片列表查询
- `GET /{modelName}/record/{recordId}/card` - 获取记录的卡片

## 使用示例

### 1. 获取看板数据
```
GET /api/kanban/SaleOrder/data?groupField=state
```

### 2. 创建卡片
```json
POST /api/kanban/card
{
  "modelName": "SaleOrder",
  "recordId": 123,
  "columnId": 1,
  "title": "订单#123",
  "priority": "HIGH",
  "assigneeId": 1,
  "dueDate": "2026-05-30",
  "tags": ["urgent", "vip"]
}
```

### 3. 移动卡片
```json
POST /api/kanban/card/move
{
  "cardId": 1,
  "fromColumnId": 1,
  "toColumnId": 2
}
```

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_kanban_tables.sql`

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 访问地址
- 服务地址：http://localhost:8101
- API文档：http://localhost:8101/doc.html

## 许可证

Apache License 2.0