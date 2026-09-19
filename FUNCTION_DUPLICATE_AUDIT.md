# 功能重复开发审计（收尾盘点）

> 生成时间：2026-09-19
> 范围：接口级 / 页面级 / 服务方法与工具函数级三条线
> 配套：孤儿页面与死代码见 `CLEANUP_SCOPE_20260919.md`；数据表重复见 `TABLE_DUPLICATE_AUDIT.md`

---

## 0. 口径与可复现命令

```bash
python tools/audit-duplicate-apis.py    # 接口级：同资源尾部被 >=2 个 Controller 实现
python tools/audit-duplicate-pages.py   # 页面级：页面调用的 API 集合 Jaccard >= 0.6
python tools/audit-duplicate-code.py    # 代码级：方法体归一化后完全相同 / utils 同名导出
```

产物：`tools/audit-duplicate-apis.json`、`audit-duplicate-pages.json`、`audit-duplicate-code.json`。

### 判定口径

| 层级 | 判据 | 为什么这样定 |
|---|---|---|
| 接口 | 去掉 `/api`、模块前缀后的「资源尾部」相同 | 路径前缀不同但资源动作相同，就是同一业务能力被实现两次 |
| 页面 | 页面 `@/api/*` 的 `模块#方法` 集合 Jaccard ≥ 0.6 且共同调用 ≥ 3 个 | 不用标题（中文重名常见）、不用字段（同构表单会误配） |
| 代码 | 方法体去注释/去字符串/去数字后**完全相同**，且长度 ≥ 300 字符 | 跨类**同名**方法绝大多数是「实现同一接口」，那是好设计不是重复 |

### 已知局限

1. 接口级不区分 HTTP 方法，`GET /x` 与 `DELETE /x` 会被归到同一 tail（实际输出里已按 verb 分开列示）。
2. 页面级只看 API 调用集：**共用 API 但业务不同**（如采购订单/销售订单都用同一套 CRUD 形态）会被判相似，必须人工二次裁定。
3. 代码级只抓**完全一致**的方法体，变量名不同的近似复制抓不到（阈值收紧是为了零误报）。

---

## 1. 接口级重复：78 组

### 1.1 同业务两套实现（需要处理）

| 资源 | 实现 A | 实现 B | 判断 |
|---|---|---|---|
| `check/page`、`check/{}` | `CheckController`（wms） | `StockCheckController`（erp） | **两套盘点**，与 `CLEANUP_SCOPE` §2.6 第 5 项「`wh/inventory-order` vs 盘点单（5003）」是同一件事，与表 `erp_stock_check_item` vs `wms_check_result` 对应 |
| `config/list\|page\|export\|batch\|{}` | `ConfigController`（dms） | `SystemConfigController`（config） | **两套配置中心**，与 `TABLE_DUPLICATE_AUDIT` A-07（`dms_config` vs `sys_config`）是同一件事的两面 |
| `customer/page\|list\|import\|export\|batch\|{}` 等 7 个 | `CustomerController`（customer） | `MdCustomerController`（erp） | **客户两套**，对应 `CLEANUP_SCOPE` D-04「客户（80200 crm vs 80510 md）重名菜单」 |
| `dashboard/stats`、`dashboard/trend` | `DashboardController`（dashboard） | `DashboardController`（dms） | **两个同名类**，聚合口径可能不一致 |
| `user/page\|batch\|{}` 等 6 个 | `SysUserController`（user） | `UserController`（v2） | 新旧两套用户接口（`/api/v2/user/...` 是旧前缀，`CLEANUP_SCOPE` §1.3 提过它的 `status` 写反修复） |
| `warehouse/page\|save\|update\|{}` | `WarehouseController`（erp） | `WarehouseController`（wms） | 与 `TABLE_DUPLICATE_AUDIT` A-08（`erp_warehouse` vs `wms_warehouse`）同一件事 |
| `receipt/page\|{}\\|next-no\|page-detail` | `ReceiptController`（erp） | `ReceiptController`（wms） | 收货单两套，与 `CLEANUP_SCOPE` §2.2 第 27 项（`wms/receipt/index.vue` → `wh/receiving-order`）呼应 |
| `reconciliation/{}` | `ReconciliationController`（erp） | `ReconciliationController`（reconciliation） | 对账两套 |
| `supplier-portal/*` | `SupplierPortalController`（v1） | `SupplierPortalInquiryController`（supplier-portal） | 供应商门户 v1 与新实现 |
| `payment/page\|export\|{}\|{}/write-off` | `PaymentController`（dms） | `PaymentController`（erp） | **需裁定**：DMS 配送付款与 ERP 付款单若语义不同则保留 |

### 1.2 业务对称复制（结构重复，非功能重复）

以下 4 组是**采购/销售对称**或**合同双方对称**，业务上是两件事，保留：

- `order/*` 11 组：`PurchaseOrderController` ↔ `SaleOrderController`
- `exchange/*` 14 组：`PurchaseExchangeController` ↔ `SaleExchangeController`
- `return/*` 10 组：`PurchaseReturnController` ↔ `SaleReturnController`
- `contract/*` 5 组：`ContractController`（CRM 合同）↔ `PurchaseContractController`（采购合同）

**但要指出**：这 4 组接口**逐个方法一一对应**（如 `exchange` 的 14 个方法：`page/{}/approve/{}/reject/{}/submit/{}/cancel/{}/complete/{}/print/{}/items/{}/tracking/{}/approval-records/batch-print/export/next-no` 全部同名同形），对应表结构相似度 0.70–0.79。这是**结构性复制**：采购与销售两侧各维护一份几乎相同的代码。

- 处置：**本轮不动**。抽公共基类属架构改造，风险高于收益，且不是「收尾」该做的事。
- 但若后续任一侧改了审批流/单号规则，**必须两侧同步改** —— 建议在开发规范里写明这条「对称模块同步清单」。

---

## 2. 页面级重复

扫描 64 个 API 调用 ≥ 3 的页面，得到 8 组候选。**逐组人工裁定后，6 组是误报，2 组是真问题**。

### 2.1 真问题

#### P-01 司机端两个签收页 —— **同一端、两个入口、两套实现**

| 页面 | 路由 | 标题 | 行数 |
|---|---|---|---|
| `driver-delivery/src/views/delivery/sign.vue` | `/delivery/:id/sign` | 签收 | 335 |
| `driver-delivery/src/views/sign/index.vue` | `/sign` | 签收确认 | 452 |

- 两个页面**都在路由表里活着**（`router/index.ts:36` 与 `:72`），入口分别是配送详情/路线页（`views/route/index.vue:145` 跳 `/sign`）。
- 两文件差异 666 行 —— **不是复制，是两次独立实现**。
- 两处都有 2026-09-13 的独立修复注释：`delivery/sign.vue` 改为直连 `POST /api/dms/sign/submit`（四要素采集）；`sign/index.vue` 也改了「假成功」问题。
- **风险**：一次修复只落到一边，两个入口行为就不一致（这正是 `dms-sign-driver-dual-end` 记录过的坑）。
- **处置**：合并为一个页面（保留四要素完整的那套），另一条路由重定向。**本轮最该处理的页面级重复。**

#### P-02 往来单位三个页面同源（`md/partner` / `md/supplier` / `md/customer`）

| 页面 | 菜单 | 行数 | API 集合 |
|---|---|---|---|
| `md/partner/index.vue` | 其他往来单位（80513） | 1136 | `partnerApi` + 附件 + 分类 |
| `md/supplier/index.vue` | 供应商（80511） | 1165 | **与 partner 完全相同**（Jaccard 1.00） |
| `md/customer/index.vue` | 客户（80510） | 2943 | 同前 3 个接口（Jaccard 0.60） |

- `partner` 与 `supplier` 的 API 指纹**完全一致**，结构同源，靠 `party_type` 区分（partner 页注释写明 `biz_party.party_type=4`）。
- 差异 779 行（占 1165 行的 67%）⇒ **不是简单复制，是同一骨架 + 大量各自定制**。
- **处置**：不是「删一个」的问题，而是**是否把这三种往来单位收敛为一个页面的三个筛选视图**。需业务裁定。本轮只登记。
- 与 `TABLE_DUPLICATE_AUDIT` A-03/A-05（供应商、等级价两套表）同属往来单位域。

### 2.2 误报（明确保留）

| 组 | 为什么不是重复 |
|---|---|
| `crm/opportunity`(2129) vs `crm/opportunity-stage`(677) | 商机列表 vs 商机阶段配置，体量差 3 倍，差异 1862 行 —— 阶段配置页引用商机 API 是应有之义 |
| `sales/visit-exec`(1017) vs `sales/visit-plan`(825) | 巡访执行 vs 巡访计划，差异 570 行 |
| `mall/freight-config`(1072) vs `mall/shop-config`(1023) | 运费配置 vs 店铺配置，差异 1511 行（远超两文件体量） |
| `mall/basic-config` vs `mall/shop-config` | 同上 |
| `erp/column-config/PreOrderFormConfig` vs `SaleReturnDocFormConfig` | 都是列配置，共用 `getCustomers/getUsers/getWarehouses` 是列配置的固有需求 |
| `wh/borrow-in/form` vs `wh/borrow-out/form` | 借入/借出方向相反 |

---

## 3. 服务方法与工具函数级重复

扫描 4125 段方法体（归一化后 ≥ 300 字符），**22 段在不同文件中完全重复**。

### 3.1 被 `@Primary` 遮蔽的整套重复实现 —— **确凿的「新旧并存」**

| 实现 | 状态 | 证据 |
|---|---|---|
| `SearchServiceImpl`(474行) **`@Primary`** / `AdvancedSearchServiceImpl`(734行) | 后者**零引用** | `SearchServiceImpl.java:21` 有 `@Primary`；`AdvancedSearchServiceImpl` 全仓无任何注入点；两者 `getSearchHistory`、`deleteSearchHistory`、`buildSearchResult` **三个方法体完全相同** |
| `AuthServiceOptimizedImpl`(301行) **`@Primary`** / `AuthServiceImpl`(230行) | 后者被遮蔽 | `AuthServiceOptimizedImpl.java:29` 有 `@Primary`；`WorkflowServiceImpl.java:1725` 注释明写「已被 `@Primary` 的 AuthServiceOptimizedImpl 遮蔽的 AuthServiceImpl」；两者 `getCurrentUserInfo` 方法体相同 |

- 这是**功能重复最严重的一种**：旧实现没删、仍参与编译、仍可能被误注入，且**两份代码都会被人当参考去改**。
- **处置**：确认无 `@Qualifier` 直接引用后，删除被遮蔽的 `AdvancedSearchServiceImpl`、`AuthServiceImpl`。属低风险死代码清理（与 `CLEANUP_SCOPE` §3 死代码清理同批）。

### 3.2 重复的工具方法（应抽公共）

| 方法 | 处数 | 位置 | 建议 |
|---|---|---|---|
| `getClientIp` | 4 | `AgentInvokeController`、`RequestLoggingFilter`、`RateLimitInterceptor`、`PrintTaskController` | 抽到 `common/utils`，这是最典型的「各写一遍」 |
| `extractParameter` | 4 | `erp-pricing` 4 个定价策略类 | 上提到策略抽象基类 |
| `parse` / `parseTime` | 3 | `trade/monitor/TimeParsers`、`PaymentServiceImpl`、`RefundServiceImpl` | 统一用 `TimeParsers` |
| `sortBy` | 3 | `PreOrderAnalysisReportServiceImpl`、`SaleAnalysisReportServiceImpl`、`InventoryAnalysisReportServiceImpl` | 抽到分析报表公共基类 |
| `send` / `sendDelayed` | 2 | `EnhancedMessageProducer` vs `MessageProducer` | 与 3.1 同类：新旧两套 producer |
| `getNullPropertyNames` | 2 | `core-base/common/utils/BeanCopyUtils` vs `erp-supplier-portal/common/core/utils/BeanUtils` | **两个同功能工具类，且都在被使用**（dict 模块用前者、供应商服务用后者）⇒ 合并为一处 |
| `buildJdbcUrl` | 2 | `DataSourceServiceImpl`、`ReportDataSourceServiceImpl` | 抽公共 |
| `tokenize` | 2 | `InvertedIndex`、`SearchQueryBuilder` | 抽公共 |
| `pageResult` | 2 | `PurchaseAnalysisReportServiceImpl`、`SaleAnalysisReportServiceImpl` | 属对称复制，可随对称模块一起处理 |
| `page` | 2 | `ExpenseTypeSubjectServiceImpl`、`OtherIncomeSubjectServiceImpl` | 同上 |
| `toTrialBalanceDTO` | 2 | `FinancialReportServiceImpl`、`LedgerServiceImpl` | 抽公共 |
| `calculateBulkDiscount` | 2 | `PromotionalDiscountCalculator`、`StandardDiscountCalculator` | 上提基类 |
| `currentUserName` | 2 | `SaleExchangeServiceImpl`、`SaleReturnServiceImpl` | 抽公共 |
| `toLongList` | 2 | `ProductController`、`ProductShieldController` | 抽公共 |

### 3.3 前端 utils 重复

| 名称 | 位置 | 说明 |
|---|---|---|
| `useFeatureFlag` / `useFeatureFlags` | `composables/useFeatureFlag.ts` 与 `utils/featureFlags.ts` | **两套 feature flag 实现**，且**都有引用方**（`composables/index.ts` 导出前者、`main.ts` 引后者）⇒ 真重复，需收敛 |

> 前端只扫出 2 组，说明前端工具函数的复用纪律明显好于后端。

---

## 4. 处理优先级

```
1. P-01 司机端双签收页        —— 合并，防行为不一致（有实际业务风险）
2. §3.1 @Primary 遮蔽的两套实现 —— 删除被遮蔽方（低风险，与死代码清理同批）
3. §1.1 同业务两套接口          —— 逐组裁定（与 TABLE_DUPLICATE_AUDIT 的 A 类一一对应，需一起定）
4. §3.3 feature flag 两套      —— 收敛
5. §3.2 工具方法抽公共         —— 常规重构，可分批
6. §1.2 对称模块结构性复制      —— 本轮不动，写入「对称模块同步清单」
```

## 5. 与已有清单的交叉关系

| 本审计发现 | 已有条目 |
|---|---|
| `check/*` 两套盘点接口 | `CLEANUP_SCOPE` §2.6 第 5 项（待裁决）、`TABLE_DUPLICATE_AUDIT` C 类 |
| `config/*` 两套配置接口 | `TABLE_DUPLICATE_AUDIT` A-07（出《参数落位规则》） |
| `customer/*` 客户两套 | `CLEANUP_SCOPE` D-04（菜单重名）、`TABLE_DUPLICATE_AUDIT` A-03 |
| `warehouse/*` 仓库两套 | `TABLE_DUPLICATE_AUDIT` A-08 |
| `receipt/*` 收货两套 | `CLEANUP_SCOPE` §2.2 第 27 项 |
| 司机端双签收页 | 记忆条目 `dms-sign-driver-dual-end`（双端坑） |
| 嵌套方法体重复 | `CLEANUP_SCOPE` §3.1「被取代的平行实现 46 个」的延续 |

**与 D-09（接口冗余 1118 个前端未调用）的区别**：D-09 是「没人调」，本审计是「两个都在调/两处都有实现」。两者互补，处理顺序上**应先解决重复（本文件），再走弃用观察流程（D-09）**。

## 6. 待填写决策记录

| 编号 | 事项 | 保留 | 处置 | 确认人 | 日期 |
|---|---|---|---|---|---|
| P-01 | 司机端签收页 | | | | |
| 3.1a | Search 两实现 | `SearchServiceImpl` | | | |
| 3.1b | Auth 两实现 | `AuthServiceOptimizedImpl` | | | |
| P-02 | 往来单位三页面 | | | | |
| 3.3 | feature flag | | | | |
| 1.1 | 同业务两套接口（9 组） | | | | |

---

## 7. 执行记录（2026-09-19）

本轮执行了「只动代码」的部分。

### 已完成

| 事项 | 动作 | 验证 |
|---|---|---|
| §3.1 搜索重复实现 | 删除 `AdvancedSearchServiceImpl.java`（734 行） | 删前确认：无按 bean 名 `advancedSearchService` 的注入（全仓 25 处 `@Qualifier` 全是 RestTemplate/Queue/DataSource 等基础设施 bean）；`ElasticsearchSearchServiceImpl`/`SemanticSearchServiceImpl` 实现的是**另外两个接口**（条件装配 / 被 Controller 使用），不在删除范围 |
| §3.1 鉴权重复实现 | 删除被遮蔽的 `AuthServiceImpl.java`；把 `AuthServiceOptimizedImpl` 改名为 `AuthServiceImpl`（去掉 "Optimized" 历史包袱） | 删前确认无按 bean 名注入；`AuthService` 接口保留 |
| §3.1 关联注释 | 更新 `WorkflowServiceImpl.java:1724` 提及旧类名的注释 | — |
| P-01 司机端双签收 | `/sign` 改为重定向到 `/delivery/:id/sign`（`orderId`→`:id` 参数映射）；删除 `views/sign/index.vue` | 确认 `delivery/sign.vue` 从 `route.params.id` 取 taskId；确认全仓仅 `router/index.ts` 引用被删页面 |
| 编译验证 | 后端 `./mvnw -o -DskipTests test-compile` | **BUILD SUCCESS**，7 分 19 秒，全模块通过 |
| 构建验证 | 司机端 `npx vite build` | **✓ built in 12.48s**；产物只有一个 sign chunk，确认旧页面已移除 |

### 未执行 / 推后

- §1.1「同业务两套接口」9 组：全部需要业务确认，且与 `TABLE_DUPLICATE_AUDIT` 的 A 类一一对应，应合并裁定。
- §3.2 工具方法抽公共：常规重构，不属收尾清理。
- §2.1 P-02 往来单位三页面收敛：需业务先定「是否收敛为一个页面的三个筛选视图」。
- §1.2 对称模块结构性复制：明确不动。

### 修正的判定

原 §1.1 表中把「旧 `expense` 包」判为可整体删除是**错的** —— 该包有 6 个 Controller，
其中 `/api/erp/expense/statistics/*` 仍被「查费用（80454）」页面调用。详见 `TABLE_DUPLICATE_AUDIT.md` A-02 的修正记录。
