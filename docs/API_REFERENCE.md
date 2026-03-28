# 智企连·AI-Ready API 接口文档

**版本**: v1.0.0  
**最后更新**: 2026-03-28  
**维护者**: team-member

---

## 目录

1. [概述](#概述)
2. [认证授权](#认证授权)
3. [用户管理API](#用户管理api)
4. [采购管理API](#采购管理api)
5. [销售管理API](#销售管理api)
6. [线索管理API](#线索管理api)
7. [错误码说明](#错误码说明)

---

## 概述

### 基础URL

```
开发环境: http://localhost:8080/api
测试环境: http://test.ai-ready.cn/api
生产环境: https://api.ai-ready.cn/api
```

### 请求格式

- Content-Type: `application/json`
- 字符编码: `UTF-8`

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1711612800000
}
```

---

## 认证授权

### 登录

**POST** `/user/login`

**请求体**:
```json
{
  "username": "admin",
  "password": "123456",
  "tenantId": 1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "timestamp": 1711612800000
}
```

### 登出

**POST** `/user/logout`

**请求头**:
```
Authorization: Bearer {token}
```

---

## 用户管理API

### 创建用户

**POST** `/user`

**权限**: `user:create`

**请求体**:
```json
{
  "tenantId": 1,
  "username": "zhangsan",
  "password": "123456",
  "nickname": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "userType": 1,
  "deptId": 100
}
```

### 分页查询用户

**GET** `/user/page`

**权限**: `user:list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | Long | 否 | 当前页，默认1 |
| size | Long | 否 | 每页大小，默认10 |
| tenantId | Long | 是 | 租户ID |
| username | String | 否 | 用户名（模糊查询） |
| status | Integer | 否 | 状态 |
| deptId | Long | 否 | 部门ID |

---

## 采购管理API

### 创建采购订单

**POST** `/erp/purchase/order`

**权限**: `purchase:order:create`

**请求体**:
```json
{
  "tenantId": 1,
  "supplierId": 100,
  "supplierName": "供应商A",
  "orderDate": "2026-03-28T10:00:00",
  "expectedDate": "2026-04-05T10:00:00",
  "warehouseId": 1,
  "remark": "备注信息"
}
```

### 分页查询采购订单

**GET** `/erp/purchase/order/page`

**权限**: `purchase:order:list`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| current | Long | 否 | 当前页 |
| size | Long | 否 | 每页大小 |
| tenantId | Long | 是 | 租户ID |
| orderNo | String | 否 | 订单号 |
| supplierId | Long | 否 | 供应商ID |
| status | Integer | 否 | 状态 |

### 提交审批

**POST** `/erp/purchase/order/{id}/submit`

**权限**: `purchase:order:submit`

### 审批通过

**POST** `/erp/purchase/order/{id}/approve`

**权限**: `purchase:order:approve`

---

## 销售管理API

### 创建销售订单

**POST** `/erp/sale/order`

**权限**: `sale:order:create`

**请求体**:
```json
{
  "tenantId": 1,
  "customerId": 200,
  "customerName": "客户A",
  "orderDate": "2026-03-28T10:00:00",
  "expectedShipDate": "2026-03-30T10:00:00",
  "warehouseId": 1,
  "shippingAddress": "北京市朝阳区xxx",
  "receiverName": "李四",
  "receiverPhone": "13900139000"
}
```

---

## 线索管理API

### 创建线索

**POST** `/api/crm/lead`

**权限**: `crm:lead:create`

**请求体**:
```json
{
  "tenantId": 1,
  "name": "线索名称",
  "companyName": "公司名称",
  "contactName": "联系人",
  "phone": "13800138000",
  "email": "contact@example.com",
  "source": 1,
  "industry": "IT",
  "region": "北京"
}
```

### 分配线索

**POST** `/api/crm/lead/{id}/assign`

**权限**: `crm:lead:assign`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ownerId | Long | 是 | 负责人ID |

### 转化为客户

**POST** `/api/crm/lead/{id}/convert`

**权限**: `crm:lead:convert`

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

**文档维护**: team-member  
**最后更新**: 2026-03-28