# ERP批次管理API使用指南

## 概述

ERP批次管理API提供完整的批次管理功能，包括批次的增删改查、状态变更、流转操作等。本文档详细介绍了API的使用方法、参数说明、调用示例和最佳实践。

## 快速开始

### 环境要求
- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 项目启动

1. **克隆项目**
```bash
cd I:\AI-Ready\backend\erp\batch-management-service
```

2. **配置数据库**
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS erp_batch_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建数据库用户（如果不存在）
CREATE USER IF NOT EXISTS 'batch_user'@'%' IDENTIFIED BY 'batch_password';
GRANT ALL PRIVILEGES ON erp_batch_dev.* TO 'batch_user'@'%';
FLUSH PRIVILEGES;
```

3. **修改配置文件**
修改 `src/main/resources/application-dev.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/erp_batch_dev
    username: batch_user
    password: batch_password
```

4. **启动应用**
```bash
mvn spring-boot:run
```

5. **访问API文档**
打开浏览器访问：http://localhost:8082/batch/swagger-ui.html

## API接口概览

### 1. 批次管理接口

#### 1.1 创建批次
- **端点**: `POST /batches`
- **描述**: 创建一个新的批次
- **请求体**: `BatchCreationDTO`
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 1.2 获取批次详情
- **端点**: `GET /batches/{id}`
- **描述**: 根据批次ID获取批次详细信息
- **路径参数**: `id` - 批次ID
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 1.3 更新批次
- **端点**: `PUT /batches/{id}`
- **描述**: 更新批次信息
- **路径参数**: `id` - 批次ID
- **请求体**: `BatchUpdateDTO`
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 1.4 删除批次
- **端点**: `DELETE /batches/{id}`
- **描述**: 删除批次（逻辑删除）
- **路径参数**: `id` - 批次ID
- **响应**: `ApiResponse<Void>`

### 2. 批次状态管理接口

#### 2.1 激活批次
- **端点**: `POST /batches/{id}/activate`
- **描述**: 将批次状态改为激活
- **路径参数**: `id` - 批次ID
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 2.2 停用批次
- **端点**: `POST /batches/{id}/deactivate`
- **描述**: 将批次状态改为停用
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 2.3 锁定批次
- **端点**: `POST /batches/{id}/lock`
- **描述**: 锁定批次，防止修改
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 2.4 完成批次
- **端点**: `POST /batches/{id}/complete`
- **描述**: 标记批次为已完成
- **响应**: `ApiResponse<BatchDetailDTO>`

### 3. 批次操作接口

#### 3.1 批次转移
- **端点**: `POST /batches/{id}/transfer`
- **描述**: 将批次从一个仓库/货位转移到另一个
- **请求参数**: `targetWarehouseId`, `targetLocationId`
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 3.2 批次质检
- **端点**: `POST /batches/{id}/quality-check`
- **描述**: 执行批次质量检查
- **请求参数**: `inspectionResult`, `inspectionNotes`
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 3.3 批次拆分
- **端点**: `POST /batches/{id}/split`
- **描述**: 将一个批次拆分成多个子批次
- **请求参数**: `splitQuantities` (拆分数量列表)
- **响应**: `ApiResponse<List<BatchDetailDTO>>`

#### 3.4 批次合并
- **端点**: `POST /batches/{id}/merge`
- **描述**: 将多个批次合并为一个批次
- **请求参数**: `sourceBatchIds` (源批次ID列表)
- **响应**: `ApiResponse<BatchDetailDTO>`

### 4. 批次流转接口

#### 4.1 批次入库
- **端点**: `POST /batches/{id}/inbound`
- **描述**: 执行批次入库操作
- **请求参数**: `warehouseId`, `locationId`, `quantity`
- **响应**: `ApiResponse<BatchDetailDTO>`

#### 4.2 批次出库
- **端点**: `POST /batches/{id}/outbound`
- **描述**: 执行批次出库操作
- **请求参数**: `quantity`, `targetOrderId`
- **响应**: `ApiResponse<BatchDetailDTO>`

### 5. 批次查询接口

#### 5.1 搜索批次
- **端点**: `GET /batches/search`
- **描述**: 根据条件搜索批次
- **查询参数**: 
  - `batchNo` - 批次号
  - `productId` - 产品ID
  - `status` - 批次状态
  - `warehouseId` - 仓库ID
  - `page` - 页码
  - `size` - 每页大小
- **响应**: `ApiResponse<Page<BatchDetailDTO>>`

#### 5.2 按状态查询批次
- **端点**: `GET /batches/status/{status}`
- **描述**: 根据状态查询批次列表
- **路径参数**: `status` - 批次状态
- **查询参数**: `page`, `size`
- **响应**: `ApiResponse<Page<BatchDetailDTO>>`

#### 5.3 获取批次预警
- **端点**: `GET /batches/warnings`
- **描述**: 获取批次预警信息（过期、库存不足等）
- **查询参数**: `warningType`, `page`, `size`
- **响应**: `ApiResponse<Page<BatchWarningDTO>>`

## 调用示例

### 1. 创建批次 (cURL)
```bash
curl -X POST "http://localhost:8082/batch/batches" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "productId": 1001,
    "batchNo": "BATCH202501150001",
    "productionDate": "2025-01-15",
    "expirationDate": "2026-01-15",
    "totalQuantity": 1000.00,
    "unit": "件",
    "supplierId": 2001,
    "warehouseId": 3001,
    "remark": "测试批次"
  }'
```

### 2. 获取批次详情 (cURL)
```bash
curl -X GET "http://localhost:8082/batch/batches/1" \
  -H "Authorization: Bearer <token>"
```

### 3. 更新批次 (cURL)
```bash
curl -X PUT "http://localhost:8082/batch/batches/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "remark": "更新批次备注信息",
    "extraInfo": "{\"qualityLevel\": \"A\", \"storageTemp\": \"2-8°C\"}"
  }'
```

### 4. 批次转移 (cURL)
```bash
curl -X POST "http://localhost:8082/batch/batches/1/transfer" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "targetWarehouseId": 3002,
    "targetLocationId": 4001,
    "transferReason": "仓库调整"
  }'
```

### 5. 搜索批次 (cURL)
```bash
curl -X GET "http://localhost:8082/batch/batches/search?productId=1001&status=ACTIVE&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

## 请求/响应格式

### 统一响应格式
所有API接口都返回统一的响应格式：
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    // 实际数据
  },
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### 错误响应格式
```json
{
  "success": false,
  "code": "BATCH_NOT_FOUND",
  "message": "批次不存在",
  "data": null,
  "timestamp": "2025-01-15T10:30:00Z"
}
```

## 错误码说明

### 通用错误码
| 错误码 | 描述 | HTTP状态码 |
|--------|------|------------|
| SUCCESS | 操作成功 | 200 |
| BAD_REQUEST | 请求参数错误 | 400 |
| UNAUTHORIZED | 未授权访问 | 401 |
| FORBIDDEN | 权限不足 | 403 |
| NOT_FOUND | 资源不存在 | 404 |
| INTERNAL_ERROR | 服务器内部错误 | 500 |
| SERVICE_UNAVAILABLE | 服务不可用 | 503 |

### 业务错误码
| 错误码 | 描述 | HTTP状态码 |
|--------|------|------------|
| BATCH_NOT_FOUND | 批次不存在 | 404 |
| BATCH_ALREADY_EXISTS | 批次已存在 | 409 |
| BATCH_STATUS_INVALID | 批次状态无效 | 400 |
| INSUFFICIENT_QUANTITY | 批次数量不足 | 400 |
| BATCH_LOCKED | 批次已锁定 | 423 |
| EXPIRED_BATCH | 批次已过期 | 400 |
| QUALITY_CHECK_FAILED | 质检不通过 | 400 |

## 认证与授权

### 1. Bearer Token认证
所有API接口（除了公共接口）都需要Bearer Token认证：
```
Authorization: Bearer <jwt_token>
```

### 2. 权限控制
- **管理员**: 可以执行所有操作
- **仓库管理员**: 可以执行批次流转、转移操作
- **质检员**: 可以执行质检操作
- **普通用户**: 只能查看批次信息

## 性能最佳实践

### 1. 批量操作
- 使用分页查询避免一次性加载大量数据
- 批量操作时考虑使用异步处理

### 2. 缓存策略
- 频繁访问的批次详情可以使用Redis缓存
- 缓存过期时间建议设置为5-10分钟

### 3. 并发控制
- 重要操作（如出库、转移）使用乐观锁
- 高并发场景考虑使用分布式锁

### 4. 监控与告警
- 监控API响应时间、错误率
- 设置批次过期预警、库存预警

## 数据模型

### BatchCreationDTO (创建批次)
```json
{
  "productId": 1001,
  "batchNo": "BATCH202501150001",
  "productionDate": "2025-01-15",
  "expirationDate": "2026-01-15",
  "totalQuantity": 1000.00,
  "unit": "件",
  "supplierId": 2001,
  "warehouseId": 3001,
  "remark": "备注信息"
}
```

### BatchDetailDTO (批次详情)
```json
{
  "id": 1,
  "batchNo": "BATCH202501150001",
  "productId": 1001,
  "productName": "产品A",
  "productionDate": "2025-01-15",
  "expirationDate": "2026-01-15",
  "totalQuantity": 1000.00,
  "availableQuantity": 800.00,
  "reservedQuantity": 200.00,
  "batchStatus": "ACTIVE",
  "qualityStatus": "PASSED",
  "warehouseName": "中央仓库",
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

## 常见问题

### Q1: 如何生成批次号？
A: 批次号由系统自动生成，格式为：BATCH + 年月日时分秒 + 序列号

### Q2: 批次状态有哪些？
A: 批次状态包括：CREATED, ACTIVE, INACTIVE, LOCKED, COMPLETED, CANCELLED, ARCHIVED

### Q3: 如何查询即将过期的批次？
A: 使用预警接口：`GET /batches/warnings?warningType=EXPIRATION`

### Q4: 批次拆分后原批次如何处理？
A: 批次拆分后，原批次数量减少，生成新的子批次，原批次状态保持不变

### Q5: 如何恢复已删除的批次？
A: 已删除的批次可以通过系统管理员在数据库中恢复

## 技术支持

- **文档**: https://docs.aiedge.cn/erp/batch
- **Git仓库**: https://github.com/aiedge/erp-batch-service
- **问题反馈**: erp-support@aiedge.cn
- **API监控**: https://monitor.aiedge.cn/erp/batch

## 版本历史

| 版本 | 日期 | 描述 |
|------|------|------|
| 1.0.0 | 2025-01-15 | 初始版本，包含完整的批次管理API |
| 1.1.0 | 2025-02-01 | 新增批次预警功能 |
| 1.2.0 | 2025-03-15 | 优化性能，添加缓存支持 |

---

**最后更新**: 2025-01-15  
**文档版本**: 1.0.0  
**维护团队**: ERP开发团队