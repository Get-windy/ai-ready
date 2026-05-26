# 批次/序列号管理模块 API 文档

## 模块概述

批次/序列号管理模块提供完整的批次追踪和单品序列号管理能力，支持从采购入库到销售出库的全生命周期管理。

**模块路径**: `/api/erp/batch-sn`
**版本**: v1.0
**最后更新**: 2026-04-29

---

## 批次管理 API

### 基础端点: `/api/erp/batch-sn/batches`

#### 1. 创建批次

**请求**: `POST /api/erp/batch-sn/batches`

**请求体**:
```json
{
  "productId": 1,
  "productCode": "PROD-001",
  "productName": "示例产品",
  "productionDate": "2026-04-29",
  "expirationDate": "2027-04-29",
  "totalQuantity": 1000,
  "sourceType": "PURCHASE",
  "warehouseId": 1,
  "warehouseName": "主仓库"
}
```

**响应**:
```json
{
  "id": 1,
  "batchNo": "B202604290001",
  "productId": 1,
  "productCode": "PROD-001",
  "batchStatus": "ACTIVE",
  "totalQuantity": 1000,
  "availableQuantity": 1000,
  "createdAt": "2026-04-29T10:00:00"
}
```

#### 2. 查询批次详情

**请求**: `GET /api/erp/batch-sn/batches/{id}`

**响应**:
```json
{
  "id": 1,
  "batchNo": "B202604290001",
  "productId": 1,
  "productCode": "PROD-001",
  "productName": "示例产品",
  "productionDate": "2026-04-29",
  "expirationDate": "2027-04-29",
  "batchStatus": "ACTIVE",
  "qualityStatus": "NORMAL",
  "totalQuantity": 1000,
  "availableQuantity": 800,
  "reservedQuantity": 200
}
```

#### 3. 查询批次列表

**请求**: `GET /api/erp/batch-sn/batches?page=1&size=20&productCode=PROD-001&status=ACTIVE`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认20 |
| batchNo | string | 否 | 批次号模糊查询 |
| productCode | string | 否 | 产品编码 |
| status | string | 否 | 批次状态: ACTIVE/EXPIRED/QUARANTINED/CANCELLED |
| sourceType | string | 否 | 来源类型: PURCHASE/PRODUCTION/SALE_RETURN |

**响应**:
```json
{
  "data": [
    {
      "id": 1,
      "batchNo": "B202604290001",
      "productCode": "PROD-001",
      "batchStatus": "ACTIVE",
      "totalQuantity": 1000
    }
  ],
  "total": 100,
  "page": 1,
  "size": 20
}
```

#### 4. 更新批次

**请求**: `PUT /api/erp/batch-sn/batches/{id}`

**请求体**:
```json
{
  "productName": "更新后的产品名称",
  "warehouseId": 2
}
```

#### 5. 批量更新批次状态

**请求**: `PATCH /api/erp/batch-sn/batches/status`

**请求体**:
```json
{
  "batchIds": [1, 2, 3],
  "newStatus": "QUARANTINED"
}
```

#### 6. 批次入库

**请求**: `POST /api/erp/batch-sn/batches/inbound`

**请求体**:
```json
{
  "productId": 1,
  "productCode": "PROD-001",
  "totalQuantity": 500,
  "warehouseId": 1,
  "warehouseName": "主仓库",
  "locationId": 101
}
```

#### 7. 批次出库

**请求**: `POST /api/erp/batch-sn/batches/outbound`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| batchId | long | 是 | 批次ID |
| quantity | decimal | 是 | 出库数量 |
| warehouseId | long | 是 | 仓库ID |
| locationId | long | 是 | 库位ID |

#### 8. 批次质检

**请求**: `POST /api/erp/batch-sn/batches/{id}/quality-inspection`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | string | 是 | 质量状态: NORMAL/QUARANTINED/DEFECTIVE |
| inspectorId | string | 是 | 质检员ID |
| inspectorName | string | 是 | 质检员姓名 |

#### 9. 查询临期批次

**请求**: `GET /api/erp/batch-sn/batches/expiring-warning?warningDays=30`

**响应**: 返回即将在指定天数内过期的批次列表

#### 10. 查询库存汇总

**请求**: `GET /api/erp/batch-sn/batches/stock-summary`

**响应**:
```json
{
  "data": [
    {
      "productCode": "PROD-001",
      "productName": "示例产品",
      "totalBatches": 5,
      "totalQuantity": 5000,
      "availableQuantity": 4500
    }
  ]
}
```

#### 11. 验证批次号唯一性

**请求**: `GET /api/erp/batch-sn/batches/validate-batch-no?batchNo=B202604290001`

**响应**: `true` (可用) / `false` (已存在)

---

## 序列号管理 API

### 基础端点: `/api/erp/batch-sn/serials`

#### 1. 创建序列号

**请求**: `POST /api/erp/batch-sn/serials`

**请求体**:
```json
{
  "serialNo": "SN202604290001",
  "productId": 1,
  "productCode": "PROD-001",
  "batchId": 1,
  "batchNo": "B202604290001",
  "manufacturingDate": "2026-04-29",
  "warrantyPeriod": 12,
  "warrantyStartDate": "2026-04-29",
  "warrantyEndDate": "2027-04-29"
}
```

#### 2. 查询序列号详情

**请求**: `GET /api/erp/batch-sn/serials/{id}`

**响应**:
```json
{
  "id": 1,
  "serialNo": "SN202604290001",
  "productCode": "PROD-001",
  "batchNo": "B202604290001",
  "snStatus": "AVAILABLE",
  "snStage": "WAREHOUSE",
  "warrantyEndDate": "2027-04-29",
  "maintenanceCount": 0
}
```

#### 3. 查询序列号列表

**请求**: `GET /api/erp/batch-sn/serials?page=1&size=20&productCode=PROD-001&status=AVAILABLE`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |
| serialNo | string | 否 | 序列号 |
| productCode | string | 否 | 产品编码 |
| status | string | 否 | 状态: AVAILABLE/IN_USE/INSERVICE/MAINTAINED/SCRAP |
| batchNo | string | 否 | 批次号 |

#### 4. 更新序列号

**请求**: `PUT /api/erp/batch-sn/serials/{id}`

#### 5. 序列号入库

**请求**: `POST /api/erp/batch-sn/serials/inbound`

#### 6. 序列号出库

**请求**: `POST /api/erp/batch-sn/serials/outbound`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| serialId | long | 是 | 序列号ID |
| saleOrderId | long | 是 | 销售订单ID |
| saleOrderNo | string | 是 | 销售订单号 |
| warehouseId | long | 是 | 仓库ID |
| locationId | long | 是 | 库位ID |

#### 7. 更新序列号状态

**请求**: `PATCH /api/erp/batch-sn/serials/{id}/status`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | string | 是 | 新状态 |
| stage | string | 是 | 新阶段 |

#### 8. 查询质保临期序列号

**请求**: `GET /api/erp/batch-sn/serials/warranty-warning?warningDays=30`

#### 9. 查询序列号流转历史

**请求**: `GET /api/erp/batch-sn/serials/{id}/full-history`

**响应**:
```json
{
  "data": [
    {
      "flowType": "INBOUND",
      "fromStatus": "null",
      "toStatus": "AVAILABLE",
      "fromStage": "null",
      "toStage": "WAREHOUSE",
      "createdAt": "2026-04-29T10:00:00"
    },
    {
      "flowType": "OUTBOUND",
      "fromStatus": "AVAILABLE",
      "toStatus": "IN_USE",
      "fromStage": "WAREHOUSE",
      "toStage": "EOF_CUSTOMER",
      "createdAt": "2026-05-01T14:30:00"
    }
  ]
}
```

#### 10. 验证序列号唯一性

**请求**: `GET /api/erp/batch-sn/serials/validate-serial-no?serialNo=SN202604290001`

---

## 数据模型

### 批次状态 (BatchStatus)

| 状态码 | 说明 |
|--------|------|
| ACTIVE | 活跃中，可正常使用 |
| EXPIRED | 已过期，不可使用 |
| QUARANTINED | 隔离中，待处理 |
| CANCELLED | 已取消 |

### 序列号状态 (SnStatus)

| 状态码 | 说明 |
|--------|------|
| AVAILABLE | 可用，待分配 |
| IN_USE | 使用中 |
| INSERVICE | 服务中 |
| MAINTAINED | 维修中 |
| SCRAP | 已报废 |

### 序列号阶段 (SnStage)

| 阶段码 | 说明 |
|--------|------|
| WAREHOUSE | 仓库中 |
| IN_TRANSIT | 在途 |
| EOF_CUSTOMER | 客户手中 |
| IN_SERVICE | 服务中 |
| SCRAPPED | 已报废 |

### 质量状态 (QualityStatus)

| 状态码 | 说明 |
|--------|------|
| NORMAL | 正常 |
| QUARANTINED | 待检 |
| DEFECTIVE | 不合格 |

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 资源冲突（如批次号已存在） |
| 422 | 业务逻辑错误（如库存不足） |
| 500 | 服务器内部错误 |

---

## 使用示例

### 场景1: 采购入库创建批次

```bash
# 1. 创建批次
curl -X POST http://api.example.com/api/erp/batch-sn/batches \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "productCode": "PROD-001",
    "productName": "示例产品",
    "productionDate": "2026-04-29",
    "expirationDate": "2027-04-29",
    "totalQuantity": 1000,
    "sourceType": "PURCHASE",
    "warehouseId": 1,
    "warehouseName": "主仓库"
  }'

# 2. 执行入库
curl -X POST "http://api.example.com/api/erp/batch-sn/batches/inbound?warehouseId=1&warehouseName=主仓库&locationId=101" \
  -H "Content-Type: application/json" \
  -d '{"batchNo": "B202604290001", ...}'
```

### 场景2: 销售出库使用序列号

```bash
# 1. 创建序列号
curl -X POST http://api.example.com/api/erp