# 前后端 API 适配检查报告

> 生成日期: 2026-06-04
> 最后更新: 2026-06-04 (第二轮修复完成)
> 检查范围: 前端 30 个 API 文件 ↔ 后端 185+ 个 Controller

---

## 一、已修复问题

### 1.1 双重 `/api` 前缀（Critical Bug）— 已修复

| 文件 | 路径 | 修复前 | 修复后 |
|------|------|--------|--------|
| `src/api/crm.ts` (contractApi) | 所有路径 | `/api/crm/contract/*` | `/crm/contract/*` |
| `src/api/crm.ts` (invoiceApi) | 所有路径 | `/api/erp/invoice/*` | `/erp/invoice/*` |

**原因**: axios 实例的 `baseURL` 已设为 `/api`，调用 `request.get('/api/...')` 会生成 `POST /api/api/crm/contract/page`，导致 404。

### 1.2 HTTP 方法不匹配（PATCH → PUT）— 已修复

前端 6 个文件中的 9 个状态更新接口已从 `request.patch()` 改为 `request.put()`，匹配后端的 `@PutMapping`。

### 1.3 缺失的后端端点 — 已修复

| 前端调用 | 后端状态 | 修复 |
|----------|----------|------|
| `GET /user/list` | SysUserController 缺失 | 已添加 |
| `GET /customer/list` | CustomerController 缺失 | 已添加 |
| `PATCH /customer/{id}/status` | CustomerController 缺失 | 已添加 |
| `POST /customer/import` | CustomerController 缺失 | 已添加 |
| `GET /customer/download-template` | CustomerController 缺失 | 已添加 |

### 1.4 直接使用 fetch() 绕过 Axios — 已修复

**文件**: `src/views/finance/accounts-receivable/index.vue` — 已改用 `receivableV1Api`。

### 1.5 路径分隔符不匹配（连字符 → 斜杠）— 本轮修复

| 后端 Controller | 原路径 | 修复后路径 | 匹配前端 |
|----------------|--------|-----------|---------|
| `PurchaseInboundController` | `/api/erp/purchase-inbound` | `/api/erp/purchase/inbound` | `/erp/purchase/inbound/*` ✓ |
| `PurchaseReturnController` | `/api/erp/purchase-return` | `/api/erp/purchase/return` | `/erp/purchase/return/*` ✓ |
| `SaleOutboundController` | `/api/erp/sale-outbound` | `/api/erp/sale/outbound` | `/erp/sale/outbound/*` ✓ |

### 1.6 PurchaseExchangeController 缺失 `/api` 前缀 — 本轮修复

**后端**: `PurchaseExchangeController` 原 `@RequestMapping("/erp/purchase/exchange")` — 缺少 `/api` 前缀。
**修复**: 改为 `@RequestMapping("/api/erp/purchase/exchange")`，与前端的 `/erp/purchase/exchange/*` 完全匹配。

### 1.7 Batch API 路径不一致 — 本轮修复

**后端**: `BatchNumberController` 原 `@RequestMapping("/api/erp/batch-sn/batches")`（复数）
**前端**: 调用 `/erp/batch-sn/batch/page`
**修复**: 后端改为 `@RequestMapping("/api/erp/batch-sn/batch")`（单数），匹配前端路径。

### 1.8 ReceiptController 路径缺失 `/sale/` 段 — 本轮修复

**后端**: `ReceiptController` 原 `@RequestMapping("/api/erp/receipt")`
**前端**: 调用 `/erp/sale/receipt/page`
**修复**: 后端改为 `@RequestMapping("/api/erp/sale/receipt")`，匹配前端路径。

### 1.9 ConfigController 路径不匹配 — 本轮修复

| 前端调用 | 问题 | 修复 |
|---------|------|------|
| `GET /config/page` | 后端只有 `/list` | 添加 `@GetMapping({"/list", "/page"})` |
| `GET /config/{id}` | 缺失 | 新增 `@GetMapping("/{id}")` 按 ID 查询 |
| `POST /config` | 后端只有 `/save` | 新增 `@PostMapping` 和 `@PostMapping("/create")` |
| `PUT /config/{id}` | 缺失 | 新增 `@PutMapping("/{id}")` 按 ID 更新 |
| `DELETE /config/{id}` | 只有按 key 删除 | 新增 `@DeleteMapping("/{id}")`，原接口改为 `/key/{configKey}` |

### 1.10 DictType/DictItem 路径不匹配 — 本轮修复

| 前端调用 | 问题 | 修复 |
|---------|------|------|
| `GET /dict/type/page` | 后端只有 `/list` | 改为 `@GetMapping({"/list", "/page"})` |
| `PUT /dict/type/{id}` | 后端只有无路径参数 PUT | 新增 `@PutMapping("/{id}")` |
| `GET /dict/item/list/{dictTypeId}` | 后端使用 `/type/{dictTypeId}` | 新增 `@GetMapping("/list/{dictTypeId}")` |
| `PUT /dict/item/{id}` | 后端只有无路径参数 PUT | 新增 `@PutMapping("/{id}")` |

### 1.11 新增 DictOptionController（Options API）— 本轮新增

前端选项 API 调用 `GET /dict/{dictCode}` 和 `POST /dict/batch` 但后端缺乏对应端点。

**修复**: 新建 `DictOptionController` 于 `cn.aiedge.dict.controller`：
- `@GetMapping("/api/dict/{dictCode}")` — 按编码返回 `{dictCode, dictName, items: [{value, label}]}` 格式
- `@PostMapping("/api/dict/batch")` — 批量查询返回数组

### 1.12 QuotationController 缺失端点 — 本轮修复

| 前端调用 | 问题 | 修复 |
|---------|------|------|
| `DELETE /crm/quotation/{id}` | 后端缺失 | 新增 `@DeleteMapping("/{id}")` |
| `POST /crm/quotation/{id}/convert-to-order` | 后端只有 `/convert` | 新增映射 `{"/{id}/convert", "/{id}/convert-to-order"}` |

### 1.13 新增 ProductController — 本轮新增

前端调用 `GET /product/list` 但后端缺失。

**修复**: 新建 `ProductController` 于 `erp-stock` 模块，`@RequestMapping("/api/product")`，从 Stock 表去重提取产品数据。

### 1.14 新增 WarehouseController — 本轮新增

前端调用 `GET /warehouse/list` 但后端缺失。

**修复**: 新建 `WarehouseController` 于 `erp-stock` 模块，`@RequestMapping("/api/warehouse")`，从 Stock 表去重提取仓库数据。

---

## 二、已验证的正确匹配

### 2.1 ERP 路径匹配

| 模块 | 后端路径 | 前端路径（含 /api 基路径） | 匹配 |
|------|---------|--------------------------|------|
| 采购订单 | `/api/erp/purchase/order/*` | `/api/erp/purchase/order/*` | ✓ |
| 采购询价 | `/api/erp/purchase/inquiry/*` | `/api/erp/purchase/inquiry/*` | ✓ |
| 采购入库 | `/api/erp/purchase/inbound/*` | `/api/erp/purchase/inbound/*` | ✓（已修复） |
| 采购退货 | `/api/erp/purchase/return/*` | `/api/erp/purchase/return/*` | ✓（已修复） |
| 采购换货 | `/api/erp/purchase/exchange/*` | `/api/erp/purchase/exchange/*` | ✓（已修复） |
| 销售订单 | `/api/erp/sale/order/*` | `/api/erp/sale/order/*` | ✓ |
| 销售出库 | `/api/erp/sale/outbound/*` | `/api/erp/sale/outbound/*` | ✓（已修复） |
| 销售退货 | 无 Controller | `/api/erp/sale/return/*` | ❌ 模块无源码 |
| 销售收款 | `/api/erp/sale/receipt/*` | `/api/erp/sale/receipt/*` | ✓（已修复） |
| 付款管理 | `/api/erp/payment/*` | `/api/erp/payment/*` | ✓ |
| 库存管理 | `/api/erp/stock/*` | `/api/erp/stock/*` | ✓ |
| 库存盘点 | `/api/erp/stock/check/*` | `/api/erp/stock/check/*` | ✓ |
| 库存调拨 | `/api/erp/stock/transfer/*` | `/api/erp/stock/transfer/*` | ✓ |
| 批次管理 | `/api/erp/batch-sn/batch/*` | `/api/erp/batch-sn/batch/*` | ✓（已修复） |

### 2.2 CRM 路径匹配

| 模块 | 后端路径 | 前端路径 | 匹配 |
|------|---------|---------|------|
| 线索 | `/api/crm/lead/*` | `/crm/lead/*` | ✓ |
| 商机 | `/api/crm/opportunity/*` | `/crm/opportunity/*` | ✓ |
| 合同 | `/api/crm/contract/*` | `/crm/contract/*` | ✓（已修双重 /api） |
| 报价 | `/api/crm/quotation/*` | `/crm/quotation/*` | ✓ |
| 发票 | `/api/erp/invoice/*` | `/erp/invoice/*` | ✓（已修双重 /api） |

### 2.3 系统管理路径匹配

| 模块 | 后端路径 | 前端路径 | 匹配 |
|------|---------|---------|------|
| 用户 | `/api/user/*` | `/user/*` | ✓ |
| 角色 | `/api/role/*` | `/role/*` | ✓ |
| 部门 | `/api/dept/*` | `/dept/*` | ✓ |
| 职位 | `/api/position/*` | `/position/*` | ✓ |
| 租户 | `/api/tenant/*` | `/tenant/*` | ✓ |
| 字典类型 | `/api/dict/type/*` | `/dict/type/*` | ✓（已修复） |
| 字典项 | `/api/dict/item/*` | `/dict/item/*` | ✓（已修复） |
| 系统配置 | `/api/config/*` | `/config/*` | ✓（已修复） |
| 菜单 | `/api/menu/*` | `/menu/*` | ✓ |
| 客户 | `/api/customer/*` | `/customer/*` | ✓ |
| 供应商 | `/api/supplier/*` | `/supplier/*` | ✓ |

---

## 三、仍存在的问题

### 3.1 销售退货模块（erp-sale-return）无源码

`erp-sale-return` 模块是一个空骨架 Maven 模块（仅有 `pom.xml` 和 `application.yml`），无任何 Java 源码。前端调用的 `/erp/sale/return/*` 端点全部返回 404。

**建议**: 实现销售退货完整模块（Controller、Service、Entity、Mapper）或至少提供占位端点。

### 3.2 产品/仓库数据源基于 Stock 表

ProductController 和 WarehouseController 从 `erp_stock` 表去重查询产品/仓库数据。这意味着：
- 如果没有库存记录，下拉选项为空
- 无法管理产品/仓库的独立信息（如价格、联系方式等）
- 长期应考虑创建独立的产品和仓库表

### 3.3 字典状态字段类型不一致

| 字段 | 前端类型 | 后端类型 | 兼容性 |
|------|---------|---------|--------|
| `DictType.status` | `number` (0/1) | `String` ("ENABLED"/"DISABLED") | △ 需前端转换 |
| `DictItem` 字段名 | `itemCode`/`itemName` | `itemValue`/`itemText` | △ 需前端适配 |

前端 `DictOptionController` 已做转换处理，但管理端页面（`dict.ts` 直接调用的 CRUD）可能存在字段不匹配。

### 3.4 响应包装不一致

CustomerController 返回裸对象而非 `Result<T>` 包装，前端拦截器的"无 wrapper 透传"机制可兼容但较脆弱。

---

## 四、修复统计

| 轮次 | 修复项 | 影响范围 |
|------|--------|---------|
| 第一轮 | 双重 /api 前缀、PATCH→PUT、缺失端点、fetch() 替换 | 8 个文件 |
| 第二轮 | 路径分隔符、缺失 /api 前缀、Batch 复数→单数、Receipt 路径、Config RESTful、Dict RESTful、DictOption、Quotation、Product、Warehouse | 10+ 个后端文件 |

### 本轮修复清单

| # | 修复内容 | 文件 | 严重程度 |
|---|---------|------|---------|
| 1 | PurchaseInbound 路径连字符→斜杠 | `PurchaseInboundController.java` | High |
| 2 | PurchaseReturn 路径连字符→斜杠 | `PurchaseReturnController.java` | High |
| 3 | SaleOutbound 路径连字符→斜杠 | `SaleOutboundController.java` | High |
| 4 | PurchaseExchange 添加 /api 前缀 | `PurchaseExchangeController.java` | Critical |
| 5 | BatchNumber 复数→单数 | `BatchNumberController.java` | High |
| 6 | Receipt 添加 /sale/ 段 | `ReceiptController.java` | High |
| 7 | Config RESTful 端点 | `SystemConfigController.java` | High |
| 8 | DictType 添加 /page 和 PUT /{id} | `DictTypeController.java` | Medium |
| 9 | DictItem 添加 /list/{id} 和 PUT /{id} | `DictItemController.java` | Medium |
| 10 | DictOption 控制器（options API） | `DictOptionController.java`（新建） | Medium |
| 11 | Quotation 添加 DELETE /{id} 和 convert-to-order | `QuotationController.java` | Medium |
| 12 | Product 控制器 | `ProductController.java`（新建） | Low |
| 13 | Warehouse 控制器 | `WarehouseController.java`（新建） | Low |
