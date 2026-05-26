# ERP远程打印模块

## 概述

远程打印/多端打印模块是企智连ERP系统的核心业务场景之一，支持异地打印、远程打印、多端打印功能。

## 功能特性

### 核心功能
- ✅ **打印模板管理**：模板创建、编辑、预览、复制、删除
- ✅ **打印任务管理**：任务创建、队列管理、状态查询、批量打印、取消、重试
- ✅ **打印机管理**：打印机注册、状态管理、分组管理
- ✅ **打印日志**：日志查询、统计、导出

### 业务场景支持
- 销售订单打印
- 采购订单打印
- 发票打印
- 送货单打印
- 拣货单打印
- 入库单/出库单打印
- 标签打印

## 技术栈

- **框架**：Spring Boot 3.x
- **数据库**：PostgreSQL + MyBatis-Plus
- **消息队列**：RabbitMQ（打印任务队列）
- **缓存**：Redis
- **API文档**：Knife4j

## API接口

### 打印模板管理 `/api/v1/print/templates`
- `POST` - 创建模板
- `PUT /{id}` - 更新模板
- `GET /{id}` - 获取模板详情
- `GET` - 模板列表查询
- `DELETE /{id}` - 删除模板
- `POST /{id}/copy` - 复制模板
- `POST /{id}/preview` - 预览模板

### 打印任务管理 `/api/v1/print/tasks`
- `POST` - 创建打印任务
- `POST /batch` - 批量打印
- `GET /{id}` - 获取任务详情
- `GET` - 任务列表查询
- `GET /queue` - 获取打印队列
- `POST /{id}/cancel` - 取消任务
- `POST /{id}/retry` - 重试任务
- `GET /history` - 打印历史查询

### 打印机管理 `/api/v1/print/printers`
- `POST` - 注册打印机
- `PUT /{id}` - 更新打印机
- `GET /{id}` - 获取打印机详情
- `GET` - 打印机列表查询
- `DELETE /{id}` - 删除打印机
- `GET /{id}/status` - 获取打印机状态
- `PUT /{id}/status` - 更新打印机状态
- `POST /groups` - 创建打印机分组
- `GET /groups` - 分组列表
- `POST /groups/{groupId}/assign` - 分配打印机到分组

### 打印日志 `/api/v1/print/logs`
- `GET` - 打印记录查询
- `GET /statistics` - 打印统计
- `GET /export` - 打印日志导出

## 部署指南

### 1. 数据库配置
执行 `src/main/resources/db/migration/V1__create_printing_tables.sql`

### 2. RabbitMQ配置
确保RabbitMQ服务运行，并创建 `print.queue` 队列

### 3. 启动服务
```bash
mvn spring-boot:run
```

### 4. 访问地址
- 服务地址：http://localhost:8095
- API文档：http://localhost:8095/doc.html

## 许可证

Apache License 2.0