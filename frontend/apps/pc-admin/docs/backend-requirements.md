# 后端 API 需求清单

> 生成日期：2026-06-04
> 项目：ai-ready-admin（智企连.AI-Ready 管理前端）

---

## 一、API 响应格式规范

### 1.1 标准响应包装

所有 API 应统一返回以下格式：

```typescript
interface ApiResponse<T> {
  code: number        // 0=成功, 非0=错误
  message: string     // 成功/错误消息
  data: T             // 业务数据
  timestamp: number   // 时间戳
}
```

### 1.2 分页响应格式

```typescript
interface PageResponse<T> {
  records: T[]        // 数据列表
  total: number       // 总记录数
  current: number     // 当前页码
  size: number        // 每页条数
  pages?: number      // 总页数
}
```

### 1.3 分页请求参数

**统一使用 `pageNum / pageSize`**（前端的标准）：

```typescript
interface PageQuery {
  pageNum: number     // 当前页码，从1开始
  pageSize: number    // 每页条数，默认20
}
```

---

## 二、已知问题清单

### P0 - 阻塞性问题（需立即修复）

| # | 问题 | 影响范围 | 说明 |
|---|------|---------|------|
| B-01 | **分页参数不一致** | purchase.ts、permission.ts、purchase-exchange.ts 使用 `current/size`，其余使用 `pageNum/pageSize` | 后端需统一接收 `pageNum/pageSize`，或前端统一发送 |
| B-02 | **API 重复定义** | 采购订单 API 定义在 3 个文件（purchase.ts、order.ts、erp.ts）指向不同端点 | 需确定标准端点并废弃重复定义 |
| B-03 | **导出方法签名不统一** | 4 种不同模式：`Promise<Blob>`、`async+cast`、`ApiResponse<Blob>`、`exportData` 命名 | 后端统一响应头支持 Blob 下载 |

### P1 - 高优先级（需在 Sprint 内修复）

| # | 问题 | 涉及文件 | 说明 |
|---|------|---------|------|
| B-04 | `crm.ts` 返回类型缺少 `ApiResponse` 包装 | leadApi、opportunityApi、contractApi、invoiceApi | `page()` 返回类型为 `PageResponse<T>` 但实际是 `ApiResponse<PageResponse<T>>` |
| B-05 | `erp.ts` 返回类型缺少 `ApiResponse` 包装 | 所有子 API | `getPage()` 返回类型为 `PageResult<T>` 但实际是 `ApiResponse<PageResult<T>>` |
| B-06 | `supplier.ts` 返回类型缺少 `ApiResponse` 包装 | `page()`、`getById()`、`create()`、`update()` | 所有方法返回类型都遗漏了外层包装 |
| B-07 | `options.ts` 返回类型缺少 `ApiResponse` 包装 | `getSuppliers()`、`getDict()` | 返回 `Promise<OptionItem[]>` 但实际是 `ApiResponse<OptionItem[]>` |
| B-08 | **缺少 `batchDelete` 端点** | 13 个 API 文件无批量删除 | 见下方完整清单 |

### P2 - 中优先级

| # | 问题 | 涉及文件 | 说明 |
|---|------|---------|------|
| B-09 | **缺少导出端点** | 12 个 API 文件无导出方法 | 见下方完整清单 |
| B-10 | `finance.ts` 无 TypeScript 类型 | 全部使用 `any` | 需要添加完整的请求/响应类型 |
| B-11 | `fixed-asset.ts` 无 TypeScript 类型 | 全部使用 `any` | 需要添加完整的请求/响应类型 |
| B-12 | 方法命名不统一 | `page()` vs `getPage()` vs `getList()` 混用 | 统一为 `page()` |
| B-13 | 单条查询方法不统一 | `getById(id)` vs `get(id)` | 统一为 `getById(id)` |

### P3 - 低优先级（待规划）

| # | 问题 | 说明 |
|---|------|------|
| B-14 | 部分 stub 页面缺少真实 API（导出、批量操作） | finance 各页面的导出为 `message.info` stub |
| B-15 | `erp/` 模块与独立模块的重复页面需合并 | erp/purchase、erp/sale 与独立 purchase/、sale/ 功能重叠 |
| B-16 | 工作流模块无 API 文件 | `src/api/workflow.ts` 不存在 |

---

## 三、缺少的 API 端点清单

### 3.1 缺少 `export`（导出）端点的 API

| # | API 模块 | 后端端点需添加 |
|---|---------|--------------|
| EX-01 | `crm/leadApi` | `GET /crm/lead/export` |
| EX-02 | `crm/opportunityApi` | `GET /crm/opportunity/export` |
| EX-03 | `crm/contractApi` | `GET /crm/contract/export` |
| EX-04 | `crm/invoiceApi` | `GET /crm/invoice/export` |
| EX-05 | `crm/supplierApi` | `GET /crm/supplier/export` (CRM 供应商) |
| EX-06 | `budget/budgetTemplateApi` | `GET /budget/template/export` |
| EX-07 | `supplier/supplierApi` | `GET /supplier/export` |
| EX-08 | `permissionApi` | `GET /permission/export` |
| EX-09 | `roleApi` | `GET /role/export` |
| EX-10 | `configApi` | `GET /config/export` |
| EX-11 | `dictApi` | `GET /dict/type/export`, `GET /dict/item/export` |
| EX-12 | `notificationApi` | `GET /notification/export` |
| EX-13 | `userApi` | `GET /user/export` |
| EX-14 | `fixedAssetApi` | `GET /fixed-asset/export` |
| EX-15 | `finance/accountSubjectApi` | `GET /finance/account-subject/export` |
| EX-16 | `finance/voucherApi` | `GET /finance/voucher/export` |
| EX-17 | `finance/receivableApi` | `GET /finance/receivable/export` |
| EX-18 | `finance/payableApi` | `GET /finance/payable/export` |
| EX-19 | `finance/reportApi` | `GET /finance/report/export` |

### 3.2 缺少 `batchDelete` 端点的 API

| # | API 模块 | 后端端点需添加 |
|---|---------|--------------|
| BD-01 | `purchase/purchaseOrderApi` | `DELETE /purchase/order/batch` |
| BD-02 | `crm/leadApi` | `DELETE /crm/lead/batch` |
| BD-03 | `crm/opportunityApi` | `DELETE /crm/opportunity/batch` |
| BD-04 | `crm/contractApi` | `DELETE /crm/contract/batch` |
| BD-05 | `crm/invoiceApi` | `DELETE /crm/invoice/batch` |
| BD-06 | `budget/templateApi` | `DELETE /budget/template/batch` |
| BD-07 | `supplier/supplierApi` | `DELETE /supplier/batch` |
| BD-08 | `roleApi` | `DELETE /role/batch` |
| BD-09 | `dict/typeApi` + `dict/itemApi` | `DELETE /dict/type/batch`, `DELETE /dict/item/batch` |
| BD-10 | `fixedAssetApi` | `DELETE /fixed-asset/batch` |
| BD-11 | `finance/accountSubjectApi` | `DELETE /finance/account-subject/batch` |
| BD-12 | `finance/voucherApi` | `DELETE /finance/voucher/batch` |
| BD-13 | `finance/receivableApi` | `DELETE /finance/receivable/batch` |
| BD-14 | `finance/payableApi` | `DELETE /finance/payable/batch` |
| BD-15 | `erp/inboundApi` | `DELETE /erp/stock/inbound/batch` |
| BD-16 | `erp/purchaseReturnApi` | `DELETE /erp/purchase/return/batch` |
| BD-17 | `erp/paymentApi` | `DELETE /erp/purchase/payment/batch` |
| BD-18 | `purchase-exchange/purchaseExchangeApi` | `DELETE /purchase-exchange/batch` |

### 3.3 Stub 实现（需补充真实逻辑）

| # | 页面 | 当前实现 | 需求 |
|---|------|---------|------|
| SB-01 | `finance/accounts-receivable/index.vue` | `message.info('导出应收账款')` | 对接真实 API |
| SB-02 | `finance/accounts-payable/index.vue` | `message.info('导出应付账款')` | 对接真实 API |
| SB-03 | `finance/trial-balance/index.vue` | `message.success('导出功能开发中')` | 对接真实 API |
| SB-04 | `finance/accounts-receivable/payment-record.vue` | `message.info('导出收款记录')` | 对接真实 API |
| SB-05 | `finance/accounts-receivable/collection-reminder.vue` | `message.info('导出催收列表')` | 对接真实 API |
| SB-06 | `finance/reports/components/BalanceSheet.vue` | `message.info('导出资产负债表')` | 对接真实 API |
| SB-07 | `finance/reports/components/CashFlowStatement.vue` | `message.info('导出现金流量表')` | 对接真实 API |
| SB-08 | `finance/reports/components/ProfitStatement.vue` | `message.info('导出利润表')` | 对接真实 API |
| SB-09 | `supplier/index.vue` - 批量激活门户 | `message.success` 无 API 调用 | `POST /supplier/batch/activate-portal` |
| SB-10 | `fixed-asset/asset/index.vue` - 批量删除 | 注释 `// In production, implement` | `DELETE /fixed-asset/batch` |

---

## 四、批量操作规范

### 4.1 批量删除

```
DELETE /{resource}/batch
Content-Type: application/json

请求体: { "ids": [1, 2, 3] }
响应: ApiResponse<{ successCount: number, failCount: number }>
```

### 4.2 批量导出

```
GET /{resource}/export?pageNum=1&pageSize=-1
Accept: text/csv

响应: Blob (Content-Type: text/csv; charset=utf-8)
响应头: Content-Disposition: attachment; filename="{文件名}_{日期}.csv"
```

### 4.3 批量审批

```
POST /{resource}/batch/approve
Content-Type: application/json

请求体: { "ids": [1, 2, 3], "approved": true, "remark": "审批通过" }
响应: ApiResponse<{ successCount: number, failCount: number }>
```

---

## 五、枚举值规范

需后端统一以下枚举定义（前端已在代码中使用）：

| 枚举 | 定义 | 使用模块 |
|------|------|---------|
| `ExchangeStatus` | `DRAFT=0, PENDING_APPROVAL=1, APPROVED=2, EXCHANGING=3, COMPLETED=4, REJECTED=5, CANCELLED=6` | 换货管理 |
| `InvoiceStatus` | `draft, issued, sent, received, cancelled` | CRM 发票 |
| `OpportunityStage` | `qualification,需求分析,方案,谈判,成交` | CRM 商机 |
| `CooperationStatus` | `1=正常, 2=暂停, 3=终止, 4=潜在` | 供应商 |
| `PortalStatus` | `0=未激活, 1=已激活, 2=已禁用` | 供应商门户 |

---

## 六、建议的修复优先级

| 优先级 | 批次 | 内容 |
|--------|------|------|
| **Sprint 1** | 统一分页参数 | 后端确认 `pageNum/pageSize` 为标准，前端统一发送 |
| **Sprint 1** | 修复返回类型 | `crm.ts`、`erp.ts`、`supplier.ts`、`options.ts` 补全 `ApiResponse` 包装 |
| **Sprint 1** | 导出签名标准化 | 统一导出为 `GET /{resource}/export` 返回 Blob |
| **Sprint 2** | 添加 `batchDelete` | 优先采购、CRM、供应商模块 |
| **Sprint 2** | 消灭重复 API | purchase/order/erp 的采购订单三合一 |
| **Sprint 3** | 补全 TypeScript 类型 | `finance.ts`、`fixed-asset.ts` |
| **Sprint 3** | Finance 模块后端对接 | 替换 8 个 stub 导出为真实 API |
| **待规划** | 工作流 API | 创建 `api/workflow.ts` |
