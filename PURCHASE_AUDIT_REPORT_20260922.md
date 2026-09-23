# 采购模块全栈审计报告

> **修复进度（2026-09-23 更新）**：P0-2/3/4/5/6/7/8/9/11 与「逻辑删除未过滤（新发现）」已落地代码；
> 详见文末「附录 A：修复落地清单」。P0-1（权限只授超管）与 P0-10（退货/换货财务库存闭环）
> 仍待拍板/单独排期，原因见附录 A。

**审计日期**：2026-09-22
**审计范围**：`backend/erp/erp-purchase`（148 个 main 源文件）、`frontend/apps/pc-admin/src/views/purchase/`（13 个目录）、`views/erp/purchase/`（2 页）、`api/purchase*.ts`、采购相关 33 张表与 36 个迁移脚本、采购域 `sys_menu`/`sys_permission`/`sys_role_permission`
**对标依据**：`docs/Yh-Spec/手动整理对标开发文档/采购模块/`（12 篇）+ `系统菜单设计与管理/`（5 篇）
**方法**：5 路并行静态审计 + 真库 `information_schema` 核对 + **真实登录会话打接口实测**（后端 5655）
**边界**：只读。未修改任何代码、配置或数据库。

---

## 结论摘要

采购模块的**代码骨架完成度很高**（143 个端点 100% 带 `@SaCheckPermission`、列/查询项与文档逐项对齐、单据号迁移无重号、种子数据 tenant_id 规范），但存在**一条从权限到数据闭环的系统性断裂**，且这条断裂被"所有测试账号都是超管"这一事实完整掩盖。

一句话结论：**采购模块当前只对超管可用；一旦给业务角色授权，会立刻暴露出 4 个 P0。**

| 级别 | 数量 | 代表问题 |
|---|---|---|
| P0 | 11 | 权限只授超管（实测全 403）· 订单元数据接口不存在（实测 400）· 换货明细列名不匹配（实测 SQL 报错）· 进价跨租户泄露 · 硬编码租户 · 已收数量口径错 · 退货/换货无财务闭环 · 智能补货假成功 · 导出空心 |
| P1 | 14 | 单据号唯一索引缺租户/软删条件 · 采购询价无菜单入口 · 页面配置按钮开关不生效 · 表单页缺页面配置 · 打印只加计数 |
| P2 | 20+ | 317 列僵尸表 · 11 条僵尸权限码 · 重复服务实现 · `.atcode` 目录污染 66 处 |

### 最需要先知道的三件事

1. **采购权限只授给了 `SUPER_ADMIN`（92 条），`SYSTEM_ADMIN` / `DEPT_ADMIN` 一条都没有。** 实测以 `SYSTEM_ADMIN` 登录后，`order/doc-query`、`inquiry`、`inbound`、`price-track` 四个接口**全部 403**。这不是权限码缺失（码都在库里），而是**没有业务角色承接**。
2. **库内 46 个用户里有 43 个是 `SUPER_ADMIN`**（其余 2 个 `SYSTEM_ADMIN`、1 个 `E2E_T2_ADMIN`），超管又被租户拦截器和权限校验双重豁免 → **历次 E2E 全绿并不代表真实租户用户可用**。
3. **换货单明细读不出来。** `erp_purchase_exchange_item` 的 8 个 `price_level_1..8` 列，实体写成 `priceLevel1..8` 且无 `@TableField`，MyBatis-Plus 推导出 `price_level1`（数字前不补下划线）。实测 PostgreSQL 直接报错并提示「您也许需要增加明确的类型转换 / `erp_purchase_exchange_item.price_level_1`」。该表当前 0 行，所以是**潜伏炸弹**——第一张真实换货单录进去就会炸。

---

## 一、权限与鉴权

### P0-1 采购权限只授给超管，其他角色全线 403（实测）

**证据（真库）**
```sql
SELECT r.role_code, count(*) FROM sys_role_permission rp
  JOIN sys_role r ON r.id=rp.role_id JOIN sys_permission p ON p.id=rp.permission_id
 WHERE p.permission_code LIKE 'purchase:%' AND p.deleted=0 GROUP BY 1;
-- 结果：SUPER_ADMIN | 92     （仅此一行；SYSTEM_ADMIN / DEPT_ADMIN = 0）
```

**实测（以 `SYSTEM_ADMIN` 账号 `e2e_hr_ta` 登录 5655）**
```
GET /api/erp/purchase/order/doc-query/page → 403 {"message":"无权限访问: purchase:order:list"}
GET /api/erp/purchase/inquiry/page         → 403 {"message":"无权限访问: purchase:inquiry:list"}
GET /api/purchase/price-track/page         → 403 {"message":"无权限访问: purchase:price:edit"}
GET /api/erp/purchase/inbound/page         → 403 {"message":"无权限访问: purchase:inbound:list"}
```

**影响**：采购模块对任何非超管用户完全不可用（列表、单据、查询页全部打不开）。这是本次审计**最严重且最容易被漏掉**的问题——因为所有验收账号都是超管。

**修复顺序（必须遵守，否则会制造新的全站 403）**：① 先建采购角色（采购员/采购主管/收货员/应付会计）→ ② `sys_role_permission` 授权 → ③ 才谈调整注解。缺任何一步都会重演 2026-09-19 那次「243 处引用、153 条在库 → 非超管全 403」。

### P0-2 进价台账跨租户泄露（实测 SQL 无租户条件）

`mapper/PurchasePriceTrackMapper.java:32,78` 两个查询都标了 `@InterceptorIgnore(tenantLine = "true")`，而 SQL 里**没有自己补 `tenant_id`**：

```java
@InterceptorIgnore(tenantLine = "true")
@Select("<script>SELECT ... FROM erp_purchase_price_track t WHERE t.deleted = 0 ...")
IPage<PurchasePriceTrack> selectTrackPage(...);
```

真库核对：`erp_purchase_price_track` **确有 `tenant_id` 列**（V11.48.0 建表含 `tenant_id`）。所以这不是"表没有列只能忽略"，而是**主动关掉了隔离却没补条件**。

**影响**：任何持有 `purchase:price:edit` 的登录用户，在 `/page` 与 `/trend` 上会读到**全部租户**的最近采购价——这是采购最敏感的成本数据。
**修复**：删掉 `@InterceptorIgnore`，或在 SQL 中显式 `AND t.tenant_id = #{tenantId}`。同文件的 `:88,:98` 两处也要一并核。

### P0-3 新建单据硬编码 `tenant_id = 1`（4 处）

```
inbound/controller/PurchaseInboundController.java:99        inbound.setTenantId(1L);
inbound/service/impl/PurchaseInboundServiceImpl.java:294    inbound.setTenantId(1L);   // 导入路径
purchasereturn/controller/PurchaseReturnController.java:106 ret.setTenantId(1L);
purchaseexchange/service/impl/PurchaseExchangeServiceImpl.java:176  exchange.setTenantId(1L);
```

**机理**：MyBatis-Plus 的租户插件只在 INSERT 列**不含** `tenant_id` 时注入；列已存在则保留原值。所以这里写死 1，租户 2 的用户新建的单据会落成 `tenant_id=1`——**自己列表里查不到，却污染了租户 1 的数据**。
**修复**：删掉硬编码，交给拦截器注入，或显式取 `MyBatisPlusConfig.getCurrentTenantIdValue()`。顺带查 `PurchaseExchangeServiceImpl.java:203,228` 的 `... : 1L` 兜底。

### P0-4 四张询价/报价表无 `tenant_id` 列——当前被 403 掩盖，修完权限立刻爆

真库核对：

| 表 | 列数 | 有 tenant_id？ |
|---|---|---|
| `purchase_inquiry` | 27 | **无** |
| `purchase_inquiry_item` | 17 | **无** |
| `purchase_supplier_quote` | 34 | **无** |
| `purchase_quote_item` | 19 | **无** |

这四张表**都不在** `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 清单内（该清单已逐项核对，无 `purchase*`）。拦截器 `AiReadyTenantLineInnerInterceptor.shouldSkip()` 对"已登录"会话**不跳过**（2026-09-21 平台-BREAK-01 收紧为 fail-closed），会注入 `tenant_id = ?` → PostgreSQL 报「字段不存在」→ 整块 500。

**当前为什么没暴露**：非超管 403 根本进不来，超管被 `isTenantScopeExempt()` 豁免。**所以这是"修好权限就立刻炸"的连带风险，必须与 P0-1 同批处理**。
**修复**（照 V11.16.0 / V11.22.0 给订单子表补列的先例）：补 `tenant_id` 列并回填，或给 Mapper 加 `@InterceptorIgnore` 并自行按业务键隔离。

### 做得好的部分

- **143 个端点 100% 带 `@SaCheckPermission`**，无裸奔端点；`@PreAuthorize` 全仓仅出现在注释里（且已说明 sa-token 不填充 SecurityContext、恒抛 AccessDeniedException），采购侧已无这个历史隐患。
- 权限码命名首段用 `purchase:`（业务域），未使用 `erp:` 这类无业务含义的命名空间。
- 未发现"前端传 userId/tenantId 直接查"的越权读取（`PurchaseOrderController.java:216,220,274` 有 `tenantId` 入参，但会被拦截器 AND 上会话租户，不构成越权）。

### 权限设计的行业差距（细分）

| 差距 | 现状 | 业界标准 |
|---|---|---|
| **无业务角色** | 只有 SUPER_ADMIN / SYSTEM_ADMIN / DEPT_ADMIN | U8 预置采购员/采购主管/采购会计；SAP 用 Activity 组 |
| **无职责分离** | `submit`/`approve`/`batch-approve` 三个码可由同一角色持有 | SAP Release Strategy、U8 审批流要求"制单人 ≠ 审批人" |
| **无金额分级审批** | `purchase:order:approve` 单一码 | ≤X 万主管批、>X 万总监批 |
| **进价无字段级隔离** | 只有粗码 `purchase:price:edit`，读端点也用它 | Odoo `group_purchase_manager` 区分"可见进价/仅售价" |
| **无数据级权限** | 有 `sys_data_scope` 框架，采购控制器零使用 | SAP 按采购组织、Odoo 按多公司隔离 |
| **SoD 规则未配置** | `sys_sod_rule` 表存在，采购域无规则 | — |

另：`PurchasePriceTrackController.java:49,87` 的两个**读**端点挂的是**写**权限码 `purchase:price:edit`（代码注释自认是"临时口径，已登记 MASTER_TODO"），导致读写权限无法分离。

---

## 二、可用性（功能是否真的能用）

### P0-5 采购订单分页端点根本不存在（实测 400）

**实测**
```
GET /api/erp/purchase/order/page?pageNum=1&pageSize=5
→ 400 {"code":400,"message":"参数[id]格式不正确"}
```

**根因**：`PurchaseOrderController`（`@RequestMapping("/api/erp/purchase/order")`）**没有 `/page` 映射**，请求落到了 `@GetMapping("/{id}")`（`PurchaseOrderController.java:103`），Spring 试图把字符串 `"page"` 转成 `Long` 失败 → 400。
（注：不是子代理报告的 404。诊断口径以此为准。）

**调用方（6 处，全废）**

| 位置 | 用户可见后果 |
|---|---|
| `views/purchase/inbound/form.vue:432` | **入库单选源单永久失败**，无法从采购订单下推入库 |
| `views/purchase/return/form.vue:259` | **退货单选源单永久失败**，无法从采购订单下推退货 |
| `views/order-center/index.vue:1045`（经 `api/order.ts:263`） | 订单中心「采购」tab 列表打不开 |
| `api/purchase.ts:39`、`api/erp.ts:1898` | 同名导出，同上 |

旁证：权限种子 `PermissionInitializationConfig.java:256` 已登记 `GET /api/erp/purchase/order/page` → `purchase:order:list`，说明这是**约定的入口**，只是 Controller 漏写。
**修复**：补 `@GetMapping("/page")`（或在 `/{id}` 之前加更精确映射），或统一收敛到 `doc-query/page` 并改前端。二选一，不要两套并存。

### P0-6 换货单明细列名不匹配 → 必 500（实测）

```java
// purchaseexchange/entity/PurchaseExchangeItem.java:99-106  —— 无 @TableField
private BigDecimal priceLevel1;   // MyBatis-Plus 推导 → price_level1
private BigDecimal priceLevel8;
```
```sql
-- 真库实际列（V11.47.0__Extend_Purchase_Exchange_Tables.sql:58-65）
price_level_1 ... price_level_8
```
**实测**：`SELECT price_level1 FROM erp_purchase_exchange_item` → PostgreSQL 报错并**主动提示应为 `price_level_1`**。

**同类字段在别处都已显式修正**，说明这是漏改而非设计：
- `entity/PurchaseOrderSettlement.java:47,51` → `@TableField("deposit_account_1")` / `("deposit_amount_1")`
- `entity/PurchaseOrderExtInfo.java:35,47` → `@TableField("ext_num_1")` / `("ext_text_1")`
- `entity/PurchaseOrderItem.java:157-184` → `@TableField("custom_field_1..10")`

**影响**：`PurchaseExchangeItemMapper` 是纯 `BaseMapper`（无自定义 SQL），`PurchaseExchangeServiceImpl:360` 用 `selectList` 取明细 → 全列查询 → 必 500。换货单列表 0 行才没暴露。
**修复**：补 8 个 `@TableField("price_level_N")`；并在 CI 加一条"实体字段 → 列名 vs 迁移 DDL"的比对用例（这类事故在订单、换货上各出现过一次，值得一次性根治）。

### P0-7 三个列表页「导出」是空心按钮

| 位置 | 现行为 |
|---|---|
| `views/purchase/return/index.vue:373-377` | `request.get('/erp/purchase/return/export')` 后只 `message.success('已导出 N 条')`，无 Blob、无下载 |
| `views/purchase/inbound/index.vue:419-423` | 同上 |
| `views/purchase/exchange/index.vue:328-334` | 同上 |

对比正确写法：`api/purchase.ts:88` 用 `responseType:'blob'`。
**影响**：点导出只弹提示，磁盘上不会有文件。

### P0-8 智能补货「生成采购订单」是假成功

```java
// erp-stock/.../StockReplenishmentServiceImpl.java:141
eventPublisher.publishEvent(new PurchaseOrderCreateEvent(this, suggestion.getId(), supplierId, ...));
// :151
String tempOrderNo = "PO-" + System.currentTimeMillis();
suggestion.setCreatedOrderNo(tempOrderNo);
```
全仓搜索 `PurchaseOrderCreateEvent`：**只有定义处（`event/PurchaseOrderCreateEvent.java:19`）与发布处，没有任何监听器**。

**影响**：用户点"生成采购订单"→ 接口返回成功、建议状态变 `ORDERED`、写上一个假单号 `PO-<时间戳>`——**但没有订单落库**。且建议已被置为已处理，无法重试。这是**数据假象**，比报错更危险。
**修复**：补监听器真实建单（走 `PurchaseOrderService.createOrder + submitForApproval`），或改为同步调用；同时去掉假单号。

### 其他未接通项（P1）

| 问题 | 位置 | 说明 |
|---|---|---|
| 打印只加计数 | `PurchaseInboundServiceImpl.java:317-331`、`PurchaseOrderServiceImpl.java:660-677`、`PurchaseExchangeServiceImpl.java:338-347` | 全是 `printCount+1` + log；`erp-purchase/pom.xml` 无 `erp-printing` 依赖，无 `PrintTask` 调用 → 用户点打印只看到次数+1 |
| 「复制单据」无接收方 | `views/purchase/doc-query/index.vue:703` 传 `query:{copyFrom: id}` | 全前端 grep `copyFrom` **仅此一处**，入库/退货/换货表单都不读该参数 |
| Excel 导入半成品 | `PurchaseOrderServiceImpl.java:603-657` | `:632` 读到仓库列却恒定 `setWarehouseId(0L)`；`:617-619` 未设 `supplierId`；`:635` 明细无 `productId`；`:643-648` 逐行 catch 吞异常不回报失败行 → 导入的订单后续无法收货 |
| 询价页无法新建 | `views/purchase/inquiry/index.vue:198-203` | 工具栏只有「刷新/配置」，无「新增」；路由也只有详情页。后端 `PurchaseInquiryController` 有 POST → **有后端无入口** |
| 快速查询未实现 | `return/form.vue:828`、`inbound/form.vue:1158` | `message.info('... 快速查询功能暂不可用')`（明确的自认桩） |
| 明细查询范围过滤是表面功夫 | `detail-query/index.vue:441` | 注释自认"后端暂不精确过滤，传参无害" |

---

## 三、数据库与迁移

### 迁移卫生：良好

- **488 个迁移脚本，零重复版本号**（含数值化比对），2026-09-19 那次「重号致启动死锁」的 P0 未复现。
- 采购相关种子数据的 `tenant_id` **全部显式声明**（`V11.439.0:126`、`V11.460.0:32`、`V11.478.0:43` 均为 `0`；`erp_purchase_price_track` 用 `COALESCE(h.tenant_id, p.tenant_id, 0)`）→ 未复现「INSERT 列清单无 tenant_id → 落 0」的坑。
- 采购相关 36 个脚本**无一含 `${}` 字面量** → 无 Flyway 占位符替换致启动失败的风险。

### P0-9 单据号唯一索引不设防（实测）

| 表 | 单据号列 | 实测唯一索引 | 问题 |
|---|---|---|---|
| `erp_purchase_order` | `order_no` | `uk_erp_purchase_order_no(order_no)` | 全局唯一、不含 `tenant_id`、不含 `WHERE deleted=0` |
| `erp_purchase_return` | `return_no` | `uk_erp_purchase_return_no(return_no)` | 同上 |
| `erp_purchase_inbound` | `inbound_no` | `uk_erp_purchase_inbound_no(inbound_no)` | 同上 |
| `erp_purchase_exchange` | `exchange_no` | **只有 pkey(id)，无单据号唯一索引** | 完全无约束 |

两个后果：
1. **软删占号**：单据逻辑删除后单号永久占用，同号补录 → `duplicate key`。
2. **跨租户撞号**：`next-no` 若按租户各自编号（合同表迁移 `V11.439.0:74` 的注释明确写了"不同租户可以各自出现相同合同号"），租户 B 会被租户 A 占号 → **建档必失败**。三个表的索引口径与 `purchase_contract`（`uk_purchase_contract_no_tenant(tenant_id, contract_no)`）不一致。
3. `erp_purchase_exchange.exchange_no` 连唯一约束都没有 → `next-no` 号段并发生成/重试会**静默写入重复单号**。

**修复**：统一为 `CREATE UNIQUE INDEX ... ON t(tenant_id, 单号) WHERE deleted = 0`（换货表先补）。

### P0-10 采购退货无财务闭环 / 换货无任何库存影响

```java
// erp-finance/.../PaymentBusinessIntegrationService.java:82  供应商退款收款单
public Receipt createReceiptFromPurchaseReturn(Long returnId, ...) { ... }
```
全仓搜索调用方：**只有该方法自己的定义与注释**（`:26` 的类注释还写着"采购退货出库时调用"）。采购侧 `PurchaseReturnServiceImpl.java:336-350` 完成后只发库存事件。

- **采购退货**：减了库存、减了金额，但**无红字应付、无退款收款单、结算状态恒 0**（`:191`）→ **应付账款虚增**，退货金额永久挂在应付上。
- **采购换货**：`PurchaseExchangeServiceImpl.java:316-334` 的 `complete()` 只做 `setStatus(4) + updateById`，全类**无任何** `InventoryChangeEvent` / `StockService` / 财务引用（已 grep 确认）→ **换货对库存与财务零影响**，换入/换出数量仅用于页面统计。

### P0-11 订单「已收/未收数量」用错了列（实测）

```xml
<!-- mapper/PurchaseOrderMapper.xml:24-25 -->
COALESCE(o.received_amount, 0) AS received_quantity,
GREATEST(COALESCE(o.total_quantity,0) - COALESCE(o.received_amount,0), 0) AS unreceive_quantity,
```
- 用**金额列** `received_amount` 冒充**数量列** `received_quantity`。
- 而 `received_amount` **没有任何写入方**：`PurchaseOrderMapper.java:29` 的 `updateReceivedAmount` 全仓只有声明，零调用。
- 对比明细 Tab（同文件 `:169-170`）用的是正确的 `i.received_quantity`。

**影响**：订单列表「已收数量」恒为 0、「未收数量」恒等于订货量；两个 Tab 对同一事实给出不同数字。
**修复**：改用 `received_quantity`（`PurchaseInboundServiceImpl.java:249` 的注释说明《采购明细查询》已是这个口径），并补上唯一的写入方。

### 其他数据库问题

| 级别 | 问题 | 证据 |
|---|---|---|
| P1 | **317 列僵尸表** | 8 张表建了但全仓零引用：`purchase_supplier_evaluation`(90)、`purchase_supplier_quotation`(87)、`purchase_supplier_candidate`(51)、`purchase_order_item`(20，被 `erp_purchase_order_item` 取代)、`erp_purchase_sales_driven`(19)、`erp_purchase_cost_sharing`(18，活表是 `erp_cost_sharing`)、`purchase_quote_comparison`(17)、`purchase_quote_contract`(15) |
| P1 | **数量精度不统一** | 订单明细 `quantity DECIMAL(18,2)` vs 入库/退货明细 `DECIMAL(18,4)` vs 换货 `numeric(19,4)` → 称重类商品下单→入库链路产生舍入误差，订单数量 ≠ 入库数量累加 |
| P1 | 订单表缺日期/状态索引 | `erp_purchase_order` 只有 `order_no`/`supplier_id`/`tenant_id`，列表页高频的「日期区间 + 状态」只能全表扫 |
| P2 | **超 25 列红线** | 规范见《开发技术规范》（`erp_purchase_order` **119 列**，其中 **90 列无实体字段**，抽查 11 个如 `sale_order_no`/`tran_status`/`cash_discount` 全仓 0 命中；`erp_purchase_exchange_item` 87 列含 17 个无映射、9 个 V11.47.0 重构后已废弃的列） |
| P2 | 两套自定义字段命名并存 | `erp_purchase_order_item.custom_field_N`（带下划线）vs `erp_purchase_return_item.custom_fieldN`（不带）——各自自洽，但同一业务域两套口径极易在下次复制时踩坑 |
| P2 | 6 个实体无 `@TableName`/`@TableId` | `PurchaseContract`、`PurchaseContractItem`、`PurchaseInquiry`、`PurchaseInquiryItem`、`PurchaseQuoteItem`、`PurchaseSupplierQuote`——靠类名推导恰好正确，改类名即静默指向别的表 |
| P2 | 全采购域 0 外键 | 属本仓全局既定风格（`erp_sale_*`/`crm_*` 同样），非采购独有 |

---

## 四、跨模块关系

### 闭环检查表

| # | 流程 | 结论 | 证据 / 断点 |
|---|---|---|---|
| 1 | 订单 → 入库：**已收数量回写** | **断点 P0** | 唯一写方是 WMS 的 HTTP 回调 `PurchaseOrderController.java:49`；ERP 侧 `PurchaseInboundServiceImpl.java:499-503` 确认**不回写** → 同一订单可反复生成入库单 → **库存与应付双计** |
| 2 | 订单列表「已收/未收」 | **断点 P0** | 见 P0-11 |
| 3 | 订单「终止数量/终止金额」 | **未实现** | `PurchaseOrderItem.java:104-107` 有字段，全仓无 `setTerminatedQuantity/Amount` 写入 |
| 4 | 入库 → 库存 | **闭环（数量）/ 断点（成本）** | 数量：`PurchaseInboundServiceImpl.java:636-653` → `InventoryChangeEventListener`（真实监听器，已装配）。**成本断链**：`InventoryChangeEvent`（`core-base`）**无 unitCost 字段** → 采购单价不进库存成本，移动加权/先进先出无数据来源 |
| 5 | 入库 → 应付 | **创建闭环 / 结算断点** | 创建：`PurchaseInboundServiceImpl.java:507-517` → `PurchaseAccountingService.java:48-102`（走 `BusinessAccountingService` 接口，规范性好）。结算：正常付款走 `PayableServiceImpl.writeOff()` **不回写入库单** → 「结算状态/已结金额」与付款事实脱节 |
| 6 | 退货 → 库存 | **闭环** | `PurchaseReturnServiceImpl.java:445-466` 发 DECREASE 事件 |
| 7 | 退货 → 应付冲减 | **断点 P0** | 见 P0-10 |
| 8 | 换货 → 库存双向 | **断点 P0** | 见 P0-10 |
| 9 | 订单 → 订金/预付款 | **断点 P0** | `erp-purchase` 全模块**无 `PrePayment` 引用**；订金只做子表 CRUD → 预付款账户余额不扣减、取消订单不回滚 |
| 10 | 费用分摊 → 入库成本 | **半闭环** | `CostSharingServiceImpl.java:326-363` 写了明细 `unit_cost`，但 (a) 不重算入库单头金额、**不更新应付**；(b) `cancel()`（`:387`）**不回滚** `unit_cost` |
| 11 | 价格跟踪 → 订单单价 | **断点 P1** | 台账无消费方；下单单价来源是 `erp_product.purchase_price`（`SalesDrivenServiceImpl.java:301-304`）→ 下单价与跟踪价可任意不一致 |
| 12 | 以销定购 → 采购订单 | **闭环** | `SalesDrivenServiceImpl.java:204-206` 真实 `createOrder + submitForApproval`，且有防重 `:171-173` |
| 13 | 智能补货 → 采购订单 | **断点 P0** | 见 P0-8 |
| 14 | 缺货补货 / 库存预警补货 | **只读，无落单能力** | 控制器只有 `/list /generate /{id}/create-order /ignore`；两个页面只有页面无落库 |

### 对账口径不一致（会导致"同一问题两个数字"）

| 冲突 | 证据 |
|---|---|
| **已收数量两套口径** | 订单 Tab 用 `o.received_amount`（`PurchaseOrderMapper.xml:24`），明细 Tab 用 `i.received_quantity`（`:169`） |
| **结算状态 vs 已结金额** | 筛选用 `settle_status` 列，列表金额用付款单实时聚合（`UnifiedPurchaseDocQueryServiceImpl.java:334-465`）；列只在手工补核销时写（`UnifiedPurchaseDocQueryController.java:199-203`）→ **筛「已结算」与显示的已结金额互相矛盾** |
| **单据查询默认隐藏了待收货单** | `UnifiedPurchaseDocQueryServiceImpl.java:207,227,256` 三处 `ne("status", 3)` 注释写"隐藏红冲"，实际入库单 `InboundStatus.java:11` 的 3 = **待收货** → 默认**看不到待收货单据** |
| **采购总额三种算法** | 订单统计取 `totalAmount`、单据查询展示 `bill_amount`、订单列表用 `productAmount - discount + otherExpense` |
| **分析口径与列表不同** | 分析只算 `status IN (4..9)`，单据查询计任意状态（含草稿） |
| **换货/退货金额语义分叉** | 换货 `totalAmount = discountAmount`（`PurchaseExchangeServiceImpl.java:442`），退货是行金额合计（`PurchaseReturnServiceImpl.java:372`）→ 合并分页时同列不同义 |

### 架构耦合

| 级别 | 问题 |
|---|---|
| P2 | `service/impl/UnifiedPurchaseDocQueryServiceImpl.java:52-55` **直接注入 `erp-finance` 的 `PayableMapper` 与 `erp-payment` 的 `PaymentItemMapper/PaymentMapper`** 并手写 QueryWrapper 查别人的表，绕过 `PayableService` 边界。同模块的 `PurchaseAccountingService` 是正确示范（走接口）。 |
| P2 | `erp-purchase/pom.xml` 声明了 `erp-stock` 但模块内两处注入的 `StockService` **从未使用**（`PurchaseInboundServiceImpl.java:45`、`PurchaseReturnServiceImpl.java:38`）→ 死依赖；库存实际走 WMS 事件。同时**未声明**却实际依赖 `erp-partner`、`erp-printing`、`quality`（`PurchaseInboundServiceImpl.java:20,48` 直接 import `cn.aiedge.quality.*`）→ 靠 core-api 聚合传递依赖偶然成立，**模块单独编译/测试即断**。 |
| P2 | `PurchaseExchangeServiceImpl.java:28-32` 用 `@Autowired` 字段注入，同模块其余服务统一 `@RequiredArgsConstructor` 构造注入。 |

---

## 五、冗余与死代码

### 死代码（判定均已复核引用方式）

| 级别 | 目标 | 证据 |
|---|---|---|
| P2 | **整个「采购需求分析」服务无入口** | `service/impl/PurchaseDemandAnalysisServiceImpl.java`（~950 行）+ 接口 + `PurchaseDemandMapper` + `PurchaseDemand` 实体：全仓引用仅命中自身，**无 Controller、无前端** |
| P2 | **比价服务只被测试引用** | `PurchaseQuoteComparisonServiceImpl` 非测试引用仅自身；`PurchaseQuoteController.java:157` 的 `/compare` 走的是 `quoteService.compareQuotes()`，不是它 |
| P2 | **采购报价后端 12 端点 + 前端零调用** | `PurchaseQuoteController`（12 端点）；全前端 grep `purchase/quote|purchaseQuote|supplierQuote|quoteApi` 为空，`views/purchase/` 下无 quote 目录 |
| P2 | **Mapper 里一个无 SQL 无调用的重载** | `PurchaseContractMapper.java:141` `int updateExecutionProgress(Long id, PurchaseContract contract);` 无 `@Update`、无 XML、无调用 → 一旦被调必抛 `InvalidBoundStatementException` |
| P2 | 前端死方法（后端不存在） | `api/erp.ts:1904,1908,1909,1930`（`batchDelete`/`close`/`print`/`stats`）、`api/purchase-exchange.ts:277,296`（`getItemsByWarehouseType`/`batchApprove`）——views 内零调用，后端也无对应映射 |

### 重复实现（按「写操作包含关系」严格判定）

| 结论 | 对象 | 依据 |
|---|---|---|
| **可删（严格子集）** | `views/purchase/detail/inbound/InboundDetail.vue` | 写操作只有 `approve`+打印；`inbound/form.vue:1219-1363` 覆盖 `submit/approve/reject/receive/qualityCheck/confirmWarehouse/complete/cancel` → **严格超集**。且无用户可见入口（列表页查看/编辑都跳 `form?id=`） |
| **不能删，需人工裁定** | `alert-replenish` / `shortage-replenish` / `smart-replenish` 三页 | 三者**均无写操作**（只读 + 勾选后 `router.push` 到采购订单表单），不存在"写操作包含关系"，按判据**不能判定冗余**。数据源不同（`/erp/stock/{alert,shortage,smart}-replenish/page`），业务侧重可能不同 → 需产品裁定 |
| **非重复** | `doc-query`（按单据 37 列）vs `detail-query`（按明细 58 列） | 粒度不同 |
| **路由层重复（可合并）** | `views/erp/purchase/index.vue` 被 4 处路由挂成不同路径 | `router/dynamicRoutes.ts:102,155,309,1528`，同一份组件。建议保留规范入口，其余 redirect |

### 组件复用违规（用户强偏好：同类问题一律改共享组件一次覆盖）

| 级别 | 问题 | 已有可复用组件 |
|---|---|---|
| P1 | `alert-replenish/index.vue:127-176` **手写 220px 分类树面板**（`a-tree` + 折叠 + count 插槽） | `CategoryListLayout` 已被 `smart-replenish:4-14`、`shortage-replenish:4-14` 正确复用 → 同一能力在采购内写了 2 套 |
| P2 | 导出逻辑各写一遍（5 份几乎相同的 CSV 拼装） | `alert-replenish:411`、`smart-replenish:381`、`shortage-replenish:528`、`sales-driven:739`、`cost-sharing/index:302` |
| P2 | 采购订单表单页**手写** `a-modal + a-table` 页面配置（`erp/purchase/form.vue:107-198`） | `PageConfigPanel` 已被 9 个采购页面复用；手写版还缺打印配置/恢复默认 |
| P2 | 采购订单列表用 `BillDetailTable` 承载列表，其余列表统一 `BillTableList`（`erp/purchase/index.vue:48`） | `BillTableList`（自带表头齿轮） |
| P2 | 同一 API 对象三处重复导出 | `purchaseOrderApi` 于 `api/purchase.ts:36`、`api/order.ts:258`、`api/erp.ts:1896`；`replenishmentApi` 重名于 `api/purchase.ts:420` 与 `api/erp.ts:842` |

### 仓库卫生

**`.atcode/workflows/bundled/*.json` 目录污染 66 处**（全仓），其中采购相关 9 处：`views/purchase/`、`views/purchase/{detail-query,exchange}`、`views/erp/purchase/`，以及后端 `erp-purchase/src/main/java/cn/aiedge/erp/purchase/{analytics,entity,inbound/dto,purchaseexchange/entity}`。这些是 agent 运行残留，混在源码树里，建议清理并加入 `.gitignore`。

---

## 六、菜单与路由

### 采购菜单树（真库实测，23 个节点，状态均正常）

```
采购 (60002, mega:purchase)
├── 采购准备 (60201)
│   ├── 70051 库存预警补货   purchase/alert-replenish
│   ├── 70052 缺货补货       purchase/shortage-replenish
│   ├── 70053 智能补货       purchase/smart-replenish
│   └── 70054 以销定购       purchase/sales-driven
├── 采购业务 (60202)
│   ├── 70060 采购订单       display_mode=1, list_path=purchase/order/index
│   ├── 70061 采购入库单     display_mode=1
│   ├── 70062 采购退货单     display_mode=1
│   ├── 70063 采购换货单     display_mode=1
│   ├── 70064 采购费用分摊   display_mode=1
│   └── 81010 采购合同
└── 采购查询 (60203)
    ├── 70070 采购单据查询 / 70071 采购明细查询 / 70072 采购价格跟踪
    ├── 81006 供应商询价 / 81007 供应商绩效评估
    └── (缺) 采购询价
```

**做得对的**：菜单全部 `tenant_id=0`、`status=1`（`sys_menu` 的 1=启用语义正确，实体注释 `SysMenu.java:107` 写反了，**不要照注释改**）；无孤立节点；双入口菜单（70060-70064，`display_mode=1`）与《41条双入口菜单的正确配置》对齐。

### 问题

| 级别 | 问题 | 证据 |
|---|---|---|
| **P1** | **采购询价页是孤儿** | 页面、静态路由、13 个后端端点、10 个 `purchase:inquiry:*` 权限码**全都在**，唯独 `sys_menu` 无对应行（`SELECT ... component LIKE '%purchase/inquiry%'` 返回空）→ **功能齐全但用户找不到入口** |
| **P1** | **`menu_code` 与权限码前缀错配 6 处** | 补货三页（`purchase:alert/shortage/smart-replenish` 前缀在库中**不存在**）→ 菜单 fail-open 对所有人可见，点进去却要 `stock:replenishment:*` → **能看见、点进去 403**；`70071` 同理（要 `purchase:order:list`）；`81006/81007` 的 menu_code 是 `purchase:supplier-*` 而实际权限码是 `supplier:*`；`80421/80422` 是 `ana:purchase-*` 而实际要 `purchase:analytics:list` |
| **P1** | `menu_level=3` 异常值 | 全库 `menu_level` 分布 `(0,249)(1,43)(3,126)(4,3)`，采购域只有 `80421`/`80422` 两条非 0。`SysMenuServiceImpl.java:277-278` 对「非系统租户且非超管」强制 `eq(menuLevel,0)` → **租户普通用户看不到「采购分析/采购准备」** |
| P2 | 权限码「名实不符」 | `PurchasePriceTrackController.java:50-88` 六个端点全用粗粒度 `purchase:price:edit`，库里为它种的却是 `purchase:price-track:*` 5 个码 → 后者成僵尸码；且持有真正生效码的用户**菜单反被隐藏**（`70072` 的 menu_code 是 `purchase:price-track`） |
| P2 | 11 条僵尸权限码 | `purchase:contract:submit/view`、`purchase:manage`、`purchase:order:export/import/print`、`purchase:price-track:{create,delete,list,update,view}` —— 库里有、代码零引用 |
| P2 | component 格式不统一 | 70061 `purchase/inbound/form`、70064 `purchase/cost-sharing/form` 是相对路径，兄弟节点是 `views/xxx/index.vue` 全路径。当前靠 `dynamicRoutes.ts:904-931` 的兜底解析能工作，但格式应统一 |
| — | 孤儿页判定的假阳性复核 | `70060` 的 `list_path=purchase/order/index` 磁盘无此文件，但 `dynamicRoutes.ts:309` 有别名 `'purchase/order': () => import('@/views/erp/purchase/index.vue')`，经 `getComponent` 的「去 /index」分支命中 → **实际可用，不是白屏**。已实测确认。 |

---

## 七、页面 ↔ 文档 对齐情况

对齐度整体很好——**17 个页面里 11 个逐项达标**。

| 页面 | 文档要求(列/查询/按钮) | 实际 | 结论 |
|---|---|---|---|
| 采购订单 按单据 | 39 / 17 / 7 | 39 / 17 / 7 | ✅ |
| 采购订单 按明细 | 59 / 13 / 5 | 59 / 13 / 5 | ✅ |
| 采购订单表单 | 明细57列 / 页配35 | 57 / 35 | ✅（但页配用内联手写弹窗，非 `PageConfigPanel`） |
| 采购入库单列表 | 31 / 21 / 7 | 31 / 21 / 7 | ✅ |
| 采购入库单表单 | 明细59 / 页配37 | 62 / 42 | ❌ 多出 `批次号`/`税率%`/`质检状态`/`质检说明` 4 列；页配字段 42 vs 37 |
| 采购退货单列表 | 35 / 17 / 6 | 35 / 18 / 6 | ⚠️ 多一个「仅显示已选中」复选框 |
| **采购退货单表单** | 明细57 / 页配35 | 57 / **无页配弹窗** | ❌ P1：注释写"页面配置35字段"（`:627`）但文件内无配置弹窗 |
| 采购换货单列表 | 32 / 13 / 6 | 32 / 13 / 6 | ✅ |
| **采购换货单表单** | 换入57 / 换出57 / 页配36 | 55 / 55 / **无页配** | ❌ P1 |
| 采购费用分摊列表 | 13 / 8 / 4 | 13 / 8 / 4 | ✅ |
| 采购费用分摊表单 | 19 / 12 / 页配10 | 9 / 12 / 10 | ✅ |
| 采购单据查询 | 37 / 18 / — | 37 / 18 / 5 | ✅ |
| 采购明细查询 | 58 / 26 / — | 58 / 26 / 6 | ✅ |
| 采购价格跟踪 | 11 列 | 11 | ✅ |
| 以销定购 | 42 / 30 / 6 | 42 / 28 / 6 | ⚠️ 查询项少 2（缺「优惠券」独立项、"发货日期起/止"合并） |
| 缺货补货 | 16 / 10 | 16 / 10 | ✅ |
| 智能补货 | 25 列 | 25 | ✅ |
| 库存预警补货 | 25 列 | 25 | ✅（但未复用 `CategoryListLayout`） |
| **采购询价** | **无对标文档** | 13 / 3 / 2 | ⚠️ `docs/.../采购模块/` 只有 12 篇，**缺《采购询价开发文档》** |

### 规范性细节（对项目既定规范）

| 级别 | 问题 |
|---|---|
| **P1** | **页面配置的「功能按钮」开关全模块形同虚设**：`DocCenterLayout` 只有静态 `toolbarConfig` 入参，**没有任何"功能按钮启用态"入参**（`DocCenterLayout.vue:351-399`），`functionButtonConfig` 只传给 `PageConfigPanel`、从未过滤工具栏。涉及 `erp/purchase/index.vue:241-258`、`inbound/index.vue:194-204`、`return/index.vue:176-185`、`exchange/index.vue:152-161`、`cost-sharing/index.vue:137-144`、`inquiry/index.vue:198-203` |
| **P1** | **查询条件显隐未接线**：`doc-query:563`、`sales-driven:656`、`shortage-replenish:392` 的 `handlePageConfigChange` 是空实现 → 用户勾选无效果 |
| P2 | 查询区版式违规：`alert-replenish:479-494` 用 `display:flex` + 固定宽度 `140px`，违反"查询条件一律横向自适应网格" |
| P2 | 图标按钮悬停提示缺 `placement="bottom"`：`doc-query:43`、`sales-driven:43`、`shortage-replenish:54` |
| P2 | `erp/purchase/index.vue:526` 仍保留文档明示已移除的「单价状态」查询项 |
| P2 | 查询语义被弱化：`return/index.vue:247-251` 把 12 个文本查询条件拼成单个 `keyword`；`inquiry:241-250` 同 |
| P2 | `exchange/form.vue:854` 提示"请使用底部Tab中的列配置功能"，但页面无该 Tab |

---

## 八、需要人工裁定 / 无法确认

**不要代做决定的事项**（按项目纪律，先出方案获批再动）：

1. **给采购建哪些业务角色、授哪些权限** —— 建议采购员/采购主管/收货员/应付会计四个，但角色粒度与授权范围属业务裁定。
2. **`purchase:price-track:*`(5 条) 与 `purchase:price:edit` 保留哪一套** —— 删僵尸码会影响 `70072` 菜单的派生隐藏，改动前必须同步改 Controller 注解 + 角色授权。
3. **三个补货页是否冗余** —— 按写操作包含判据三者均无写操作，无法证明谁可删。
4. **采购询价菜单挂哪里** —— 功能齐全只缺菜单；建议挂「采购→采购查询」（与 `81006/81007` 同层）或供应商域，需裁定。
5. **`80421/80422` 的 `menu_level=3` 是笔误还是语义** —— 全库 3/4 级共 129 条，无文档说明含义。
6. **317 列僵尸表是否 DROP** —— 涉及迁移，需确认无外部系统依赖。
7. **`purchase:inquiry` 等四表补 `tenant_id` 还是加 `@InterceptorIgnore`** —— 涉及数据模型语义。
8. **`return/form.vue` / `exchange/form.vue` 缺页面配置是遗漏还是有意简化** —— 两文件注释都写了字段数，倾向是遗漏，但需回写文档确认。

**无法从静态分析确认的**：

- `biz_number_sequence` 号段生成器的租户语义 —— 决定 P0-9「跨租户撞号」是实锤还是理论。**建议先查这个再动手改索引**，否则可能改错方向。
- WMS 与 ERP 是否同 JVM。若同进程且存在内部调用绕行拦截器，则「WMS 收货回写被 401 拦死」的结论需修正（静态证据显示：`ReceiptServiceImpl.java:373-378` 的 HttpClient 只带 `Content-Type`/`X-Trace-Id`、无 token，而 `/api/erp/purchase/order/received` 不在 `SaTokenConfig` 白名单 → 指向 401）。
- `wx_inventory` 系列是否有别处补写成本 —— 我只读了 `InventoryServiceImpl.increase` 主路径。

---

## 附：已复核并更正的子代理结论

按项目铁律（子代理审计报告必须复核），以下 3 处结论经实测**推翻或修正**，不应进报告正文：

| 子代理原结论 | 复核结果 | 证据 |
|---|---|---|
| `erp_purchase_exchange` 无主键、`erp_purchase_exchange_item` 主键失效（P0） | **误判**。两表都有主键 | `pg_index` 实测：`erp_purchase_exchange_pkey(id)`、`erp_purchase_exchange_item_pkey(id)`。子代理是被迁移脚本里的 `CREATE TABLE IF NOT EXISTS` 误导，未查库 |
| `GET /erp/purchase/order/page` 返回 404 | **修正为 400**。请求被 `@GetMapping("/{id}")` 捕获，`"page"` 转 `Long` 失败 | 实测 `400 {"code":400,"message":"参数[id]格式不正确"}`。影响相同，但排查方向不同 |
| `purchase_inquiry` 等表缺 `tenant_id` 会导致「询价页整体 500」（当前故障） | **降级为潜伏风险**。当前非超管根本进不来（403），超管被豁免 → 不是当前活跃故障 | 实测超管访问 `inquiry/page` 返回 200；非超管返回 403 |

---

*报告生成：2026-09-22 · 只读审计阶段未改动代码/配置/数据库*

---

# 附录 A：修复落地清单（2026-09-23）

## 已修复并已编译通过

| 编号 | 问题 | 改动 |
|---|---|---|
| P0-3 | 4 处 `setTenantId(1L)` 硬编码 | `PurchaseInboundController:99`、`PurchaseInboundServiceImpl:294`、`PurchaseReturnController:106`、`PurchaseExchangeServiceImpl:176` 一律改为 `MyBatisPlusConfig.getCurrentTenantIdValue()`；`PurchaseExchangeServiceImpl` 两处 `: 1L` 兜底同样改会话租户 |
| P0-6 | 换货明细 `price_level_N` 列名不匹配 | `PurchaseExchangeItem` 8 个 `priceLevelN` 补 `@TableField("price_level_N")` |
| P0-5 | `GET /erp/purchase/order/page` 不存在（落到 `/{id}` → 400） | 新增 `@GetMapping("/page")`（声明在 `/{id}` 之前）+ `PurchaseOrderService.pageOrders()`，复用「按单据」39 列 SQL。兼容两套调用方入参（`orderNo/supplierName` 与 `keyword/startDate/endDate`），租户由会话决定 |
| P0-11 | 订单「已收/未收数量」用金额列冒充 | `PurchaseOrderMapper.xml` 改为按明细聚合 `SUM(item.received_quantity)`，口径与「按明细 Tab」一致 |
| P0-2 | 进价台账跨租户泄露 | `PurchasePriceTrackMapper` 的 `selectTrackPage` / `selectTrend` 移除 `@InterceptorIgnore`，租户条件交回拦截器注入。（同文件另两处 `selectProductSnapshot`/`selectPartnerSnapshot` 是按 id 查单行、非列表越权，本次未动） |
| P0-7 | 三个列表页导出空心 | 新增共享工具 `utils/exportCsv.ts#exportCsvFromColumns`（列定义→CSV，自动跳过序号/选择/操作/图片列，支持插槽列 resolvers），退货/入库/换货三页接入，真实产出 CSV |
| P0-8 | 智能补货「生成采购订单」假成功 | 删除无人监听的 `PurchaseOrderCreateEvent`；改为同步 SPI `ReplenishmentOrderGateway`（定义在 erp-stock、实现在 erp-purchase，避免反向依赖成环）。`StockReplenishmentServiceImpl.createOrder` 拿到**真实订单号**后才置 ORDERED；采购模块未装配时**显式报错**，不再写 `PO-<时间戳>` 假单号 |
| P0-9 | 单据号唯一索引不设防 | 迁移 `V11.495.0`：`uk_erp_purchase_*_no` → `(tenant_id, 单号) WHERE deleted = 0`；换货单补 `uk_erp_purchase_exchange_no_tenant` |
| P0-4 | 询价/报价 4 表无 `tenant_id` | 同上迁移补列 + 索引（4 表当前均 0 行，无回填风险） |
| 新发现 | 自定义 SQL 未过滤逻辑删除 | `selectDocListWithNames` 缺 `deleted = 0`，devdb 实测有 1 张已删订单会被列出。在 `PurchaseDocQueryServiceImpl` 与 `PurchaseOrderServiceImpl.pageOrders` 的 wrapper 显式补条件（`@TableLogic` 只作用于 BaseMapper 生成的 SQL） |

**验证到哪一步**：① 后端 `mvn -pl erp/erp-purchase -am compile` 通过；② 迁移 SQL 用「事务内执行 + 回滚」跑通，回滚后已确认无残留；③ 前端 4 个文件 `eslint` 0 error。
**未验证**：端到端运行验证（共享 5655/5656 实例当时已停、且并行会话正在重打 fat jar，未重启以免影响他人）。**下次启动后需复跑：`/erp/purchase/order/page` 返回分页、换货单表单能打开、三个列表页导出能下载、智能补货能真实建单。**

## 待拍板（未动）

| 编号 | 事项 | 为什么没直接做 |
|---|---|---|
| **P0-1** | 采购权限只授超管 → 非超管全 403 | **全系统一致如此**：`sale` 105 / `stock` 106 / `finance` 138 / `dms` 103 / `wms` 97 / `purchase` 92 条权限**全部只授 `SUPER_ADMIN`**；`SYSTEM_ADMIN`/`DEPT_ADMIN` 仅有 finance 16 条。给采购单独建角色会造成模块间不一致，且角色粒度属业务设计。建议矩阵：采购员（建单/提交/查看/导出）、采购主管（+审批/驳回/取消/终止）、收货员（入库单全流程 + 订单只读）、应付会计（查询页 + 进价只读）。**确认后写一条种子迁移即可** |
| **P0-10** | 退货无应付冲减、换货不影响库存、订单已收数量无写入方、费用分摊不回滚 | 均为跨模块数据闭环，改动面涉及 erp-finance / wms，需单独排期与回归 |
| P1 | 采购询价无菜单入口 | 属 `sys_menu` 变更，按纪律需先批方案 |
| P1 | 6 处 `menu_code` 与权限码前缀错配 | 同上（含菜单 fail-open 可见但点进去 403） |
| P1 | 页面配置「功能按钮」开关全模块无效、查询条件显隐未接线 | 需改共享组件 `DocCenterLayout`，影响面覆盖全部列表页，应作为独立专项 |
| P2 | 317 列僵尸表、11 条僵尸权限码、重复服务实现 | 删除类操作需逐个确认无外部依赖 |

## ⚠️ 与并行会话的冲突风险（重要）

1. **Flyway 版本号**：本修复占用 `V11.495.0`；`V11.494.0`（商城）是并行会话刚加的**未跟踪**文件。并行会话若同样取 11.495，启动会因「重号」或 checksum 不符而失败。**合并前先确认版本号归属。**
2. 本次编译期间出现过一次 `erp-stock` 报「`cn.aiedge.base.config` 不存在」的**瞬时失败**，重跑即成功 —— 是并行会话并发执行 Maven（重打 fat jar）导致 `target/classes` / `~/.m2` 中途不一致，不是代码缺陷。

---

# 附录 B：第二轮修复（2026-09-23 续）

用户裁定：**角色是租户自己的事，平台不做特别预置和细微角色设计** —— 故 P0-1 不再是"待办"，而是**刻意不做**：
平台只负责把权限码种好、把菜单/接口打通，租户自行建角色授权。原「建议四个采购角色」的提议作废。

## 本轮已修复（编译通过）

| 编号 | 问题 | 改动 |
|---|---|---|
| A3 | 订单「已收数量」只有 WMS 一条写入口 → 从来源订单下推的入库单确认后订单已收纹丝不动，同单可反复下推（库存/应付双计） | `PurchaseInboundServiceImpl` 新增 `writeBackOrderReceivedQuantity()`，确认入库时按 `orderItemId`（退化用 订单+商品）回写；取消入库时**对称回冲**（与 `reverseStock` 同处调用）。`addReceivedQuantity` 加 `GREATEST(...,0)` 下界，回冲多扣不会写出负数 |
| A1 | 采购退货不发红字应付、无凭证、结算状态恒 0 → 退货金额永久挂在应付上 | `PurchaseAccountingService.createPayableReversalOnReturn()`：负数应付（sourceType `PURCHASE_RETURN`）+ 红字凭证 Dr 2202 / Cr 1403，与入库记账严格对称；退货完成后置 `settleStatus=2`；按 `existsBySource` 幂等；记账失败不阻断单据（同入库语义） |
| A2 | 换货完成对库存零影响 | `PurchaseExchangeServiceImpl.postInventoryChanges()`：按 `warehouseType`（1 换入 / 2 换出）配 `inWarehouseId` / `outWarehouseId` 发 `InventoryChangeEvent` 走 WMS 唯一过账口；仓库缺失**显式抛错**而非静默跳过 |
| A5 | 取消费用分摊不回滚已回写的 `unit_cost` | `CostSharingServiceImpl.rollbackAllocatedCost()`：取消时把入库明细 `unit_cost` 还原为 `unit_price`，定位方式与 complete() 一一对称 |
| P1 | **页面配置「功能按钮」开关全模块形同虚设** | 组件级修复：`DocCenterLayout` 新增 `functionButtonsConfig` 入参并在 `visibleToolbarButtons` 里过滤（未列出的按钮 fail-open）。6 个采购列表页（含采购订单）各加一行绑定即生效 |
| P1 | shortage-replenish 查询条件显隐未接线 | 该页加 `queryFieldVisible(key)` + 10 个字段 `v-show`；`handlePageConfigChange` 真正应用配置；挂载时从 localStorage 还原（PageConfigPanel 只在被打开时才读存档，不会主动同步） |
| P1 | 采购询价功能齐全却无菜单入口 | 迁移 `V11.496.0` 新增 70073「采购询价」挂「采购→采购查询」 |
| P1 | 7 条菜单 `menu_code` 与真实权限码前缀不匹配（菜单人人可见、点进去 403） | 同迁移：70071→`purchase:order`、70051/52/53→`stock:replenishment`、81006/81007→`supplier:*`、80421/80422→`purchase:analytics` 且 `menu_level` 3→0 |
| P2 | 价格跟踪六个端点用粗粒度遗留码 `purchase:price:edit`，导致已种子的 `purchase:price-track:*` 成僵尸码、且持真实码的人看不到菜单 | `PurchasePriceTrackController` 按资源:动作拆开：list / view / create / update / delete（五个码均已授超管，实测无 403 风险） |

## 本轮**复核后推翻**的审计结论（不修，属误判或非缺陷）

| 原结论 | 复核结果 |
|---|---|
| 70070 采购单据查询 `menu_code` 与权限码错配 | **误判**。该页调 `/api/purchase/doc-query/page`，`UnifiedPurchaseDocQueryController` 用的就是 `purchase:doc-query:list`，menu_code 正确 |
| 订金未联动预付款账户（P0） | **非缺陷**。销售侧 `SaleOrderDeposit` 同样只存子表、不建预收款单 —— 是平台一致设计：订单订金是计划/引用，资金实动由财务单据承接。单方给采购接线反而造成模块间不对称 |
| 费用分摊「不重算入库单头金额、不更新应付」（P0） | **部分不成立**。`CostSharing` 自带 `expenseType`/`totalAmount`，是独立费用单（费用本身已承载资金），把分摊再并入入库头/应付会与该费用重复计量。真正缺的只是**取消不回滚**，已修 |

## 本轮未做（附精确原因与配方，供下一轮直接开工）

| 事项 | 为什么没做 / 怎么修 |
|---|---|
| doc-query、sales-driven 两页查询条件显隐 | **不能直接套 `v-show`**：这两页 `queryFieldsConfig` 里 `visible:false` 的语义是「折叠进『更多条件』区」，不是「隐藏」——直接绑 `v-show` 会让这些字段在展开「更多条件」后也永远不显示（回归）。正确修法：把「默认 2 排 / 更多条件」的分区由配置驱动（`visible:true` → 主网格；`false` → 更多条件区内且展开可见），并把那些字段的默认值改成 `true`（语义改为「该条件是否启用」） |
| 打印只加 `printCount+1`，无真实打印 | 需接 `erp-printing`（`erp-purchase/pom.xml` 目前未声明该依赖）。属跨模块功能接线，建议与打印链路一起排期 |
| 「复制单据」无接收方 | `doc-query/index.vue:703` 传 `copyFrom`，入库/退货/换货表单都不读。修法：表单页 `onMounted` 读 `route.query.copyFrom` 并回填明细 |
| 采购询价页无「新增」按钮、无建单表单路由 | 后端 13 个端点齐备，缺前端表单页 + `purchase/inquiry/form` 路由。属新建页面，非接线 |
| 退货/换货表单页缺「页面配置」弹窗 | 文档要求 35 / 36 字段。需按 `PageConfigPanel` 契约补弹窗（可参照入库表单页） |
| Excel 导入半成品（仓库恒 0、无 `supplierId`、明细无 `productId`、失败行静默丢弃） | `PurchaseOrderServiceImpl.importOrders` / `PurchaseInboundServiceImpl.importOrders` 需解析列并写真实 ID + 汇总失败行返回 |
| P2 清理：317 列僵尸表、11 条僵尸权限码、重复服务实现（`PurchaseDemandAnalysisService` 无入口 / `PurchaseQuoteComparisonService` 只被测试引用 / `PurchaseContractMapper.updateExecutionProgress` 无 SQL 无调用）、前端死方法、`.atcode` 目录污染 66 处 | 均为删除类操作，需逐项确认无外部依赖后单独一批处理 |

## 运行时验证结果（2026-09-23，真实登录 admin 打 5655）

并行会话在 06:18 重启了共享实例，Flyway 顺带把 `V11.495.0` 应用到了 devdb —— 于是本轮修复**得到了真实验证**，不再是纸面结论：

| 验证项 | 结果 |
|---|---|
| `V11.495.0` 索引是否建成 | ✅ 6 个全部存在（4 个 `(tenant_id, 单号) WHERE deleted=0` + `idx_erp_purchase_order_tenant_date` + `idx_erp_purchase_inbound_item_product`）；旧的全局唯一索引 `uk_erp_purchase_order_no` / `uk_erp_purchase_return_no` **已删除** |
| 询价/报价 4 表补列 | ✅ `purchase_inquiry` / `purchase_inquiry_item` / `purchase_supplier_quote` / `purchase_quote_item` 均有 `tenant_id` |
| **P0-5** `GET /api/erp/purchase/order/page` | ✅ 由 `400 参数[id]格式不正确` → **200**，返回真实 `records`（orderNo / supplierName / supplierCode / contactName…），`total=10` |
| **P0-11** 已收/未收数量口径 | ✅ 逐单不同且为**数量**：`E2E-CGD-0003` 总5/已收5/未收0、`E2E-CGD-0002` 总8/已收0/未收8、`E2E-CGD-0001` 总10/已收4/未收6（修复前已收恒 0、未收恒等于订货量） |
| 逻辑删除过滤（新发现项） | ✅ `total=10`，与 `deleted=0` 的实际行数一致（库中另有 1 行 `deleted=1`，修复前会被列出） |
| 询价接口（补列后） | ✅ 200，未出现期望中的「字段 tenant_id 不存在」 |
| `V11.498.0` 菜单迁移 | 事务内执行 + 回滚，可执行且零残留（尚未应用到库，等下次重启） |

---

# 附录 C：第三轮（2026-09-23 续，按"未做清单"逐项执行）

## 已完成

| 项 | 改动 | 验证 |
|---|---|---|
| **「复制单据」无接收方** | 在 `useBillForm`（入库/退货/换货等单据表单共用）里新增 `loadCopySource()`：读 `?copyFrom=` 带出源单内容，但清空 `id`/`status`/重新取号/明细行去主键——作为**新单**保存。组件级修复，一处覆盖所有用该 composable 的表单页 | 已核对 `UnifiedPurchaseConverter` 的 `id` 就是源实体主键、`documentType`∈{INBOUND,RETURN,EXCHANGE} 与前端路由映射一致 |
| **Excel 导入半成品** | 新增 `PurchaseImportLookupMapper`（名称→主数据主键回查）；订单/入库两个 `importOrders` 重写：解析供应商/仓库/商品的**真实 ID**、写 `productId`/`warehouseId`/`supplierId`，失败行**逐行回报行号+原因**（返回值由 `int` 改为 `{count, failed, errors}`）；前端新增共享 `utils/importResult.ts` 统一展示 | 编译通过；`erp-purchase` 无相关测试故签名变更不阻塞 test-compile |
| **采购询价无建单入口** | 新增 `views/purchase/inquiry/form.vue`（分区卡片 + 两列栅格 + 行内校验）+ 静态路由 `purchase/inquiry/form` + 列表页「新增」按钮；并修正 `goEdit` 原本与 `goDetail` 指向同一只读页的缺陷 | eslint 0 error |
| **查询条件显隐（doc-query / sales-driven）** | 按上一轮给出的配方实施：把 `visible` 语义统一为「该条件是否启用」（折叠区字段默认值 false→true，展开/收起仍由 showMoreConditions 控制），18 + 28 个字段全部 `v-show` 绑定，弹窗 change 与挂载时 localStorage 还原 | `v-show` 计数 18/28 与字段数一致；eslint 0 error |
| **打印按钮是假成功** | 4 个采购列表页：保留打印次数台账 + `window.print()` 产出真实单据，提示语改为与事实相符 | 见下"为什么没接 erp-printing" |
| **P2 清理** | ① 删 87 个嵌套 `.atcode` 污染目录（170 个被 git 跟踪的重复 workflow 文件，与仓库根 `.atcode` **逐字节相同**，已 `diff` 核对）；② 删前端死方法（`purchaseOrderApi.batchDelete/close/print`、`purchaseStatsApi`+`PurchaseStats`、换货的 `getItemsByWarehouseType/batchApprove`——后端均无对应端点）；③ 删 `PurchaseContractMapper` 无 SQL 无调用的 `updateExecutionProgress` 二参重载（一旦被调必抛 `InvalidBoundStatementException`）；④ **僵尸权限码 11 → 0** | 编译通过；僵尸码用脚本按"库中码 × 代码引用"重算为 0 |

**僵尸权限码的修法说明**：这 5 条（合同 submit/view、订单 export/import/print）不是"该删的码"，而是**端点挂了更粗的码**（如 `/{id}/submit` 挂 `contract:update`）。故按与价格跟踪同一口径处理——把端点绑回已种子的精确码，而不是删码。删码会让粒度永久退化。

## 仍然没做（附理由）

| 项 | 理由 |
|---|---|
| 退货/换货表单页「页面配置」弹窗 | **我上一轮的配方是错的**：入库表单页并未使用 `PageConfigPanel`（它是自写内联弹窗），而 `PageConfigPanel` 的页签是**列表页专用**。正确做法是新建共享的「单据表单页配置面板」+ `useFormPageConfig` 组合式（把 inbound 那套抽取出来）再接回 3 个表单页 —— 属跨 4 页的组件抽取迁移，会动到最重的表单页，且当前环境无法跑 E2E，不宜在收尾阶段硬上 |
| 317 列僵尸表 DROP | 破坏性操作，且需确认无外部系统依赖，按纪律先出方案 |
| 重复服务实现（`PurchaseDemandAnalysisService` ~950 行无入口、`PurchaseQuoteComparisonService` 只被测试引用） | 属"这个功能产品还要不要"的裁定，不代为删除；我复核过引用面（确实只有自身文件），删起来安全，但删的是**功能**不是死代码 |

## ⚠️ 另一会话已把我的改动提交了（须知悉）

提交 `0004c656 chore(release): 0.3.24 —— 财务审计修复 + 仓储/采购重构 + 死代码清理` 由**并行会话**创建，其中**顺带提交了本轮采购修复**：`PURCHASE_AUDIT_REPORT_20260922.md`、迁移 `V11.495.0` 与 `V11.498.0`、以及采购/换货/退货/记账/费用分摊等 Java 改动。

影响：
- 该提交把这些改动与**财务/仓储/商城**的工作混在一个 release commit 里，提交信息并未反映采购内容。
- 我是在编辑过程中才发现文件已不在 `git status` 里；**这不是我提交的**。
- 若需要拆分或补提交信息，请在合并前处理。我后续改动（附录 C 的这批）尚未提交。

## ⚠️ Flyway 重号（已处置）

本轮结束前发现并行会话也取了 `V11.496.0`（`Add_Wms_Ship_Cancel_Permission`），**两条 11.496.0 并存会让后端启动直接失败**（正是项目历史上「重号迁移致启动死锁」那一类）。

处置：我的菜单迁移**让位**改名为 `V11.498.0__Fix_Purchase_Menu_Codes_And_Inquiry_Entry.sql`（11.497.0 也已被他方占用）。当前全量 493 条迁移**零重号**。

⚠️ **但根因未解**：两个会话并行抢号，随时可能再次撞号。合并前请务必复核 `ls | sed -E 's/^(V[0-9.]+)__.*/\1/' | sort | uniq -d` 为空。
（`application-dev.yml` 配了 `out-of-order: true`，dev 下跳号安全；但 `application.yml`/`application-prod.yml` 未配 —— 即**生产环境跳号仍有风险**，所以不能靠"取个大号避开"来解决，只能靠协调。）

另注：**`V11.495.0` 已被应用，其内容已冻结**，不要再修改该文件（改内容会导致 checksum 不符、启动失败）。

## 环境阻塞记录

本轮编译反复失败于 `core-base/target/classes` 中类文件被并发删除（`InventoryChangeEvent$ChangeType.class`、`cn.aiedge.base.vo`、`cn.aiedge.common.exception` 轮流"不存在"）。重试循环第 3 次 **BUILD SUCCESS**，全部改动编译通过。**这是并行会话并发执行 Maven 造成的 `target/` 竞争，不是代码缺陷。**
`V11.496.0` 与 `V11.495.0` 两条迁移均已用「事务内执行 + 回滚」验证可执行且零残留。
