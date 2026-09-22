# 销售模块全面审计报告（2026-09-22）

**范围**：销售域前端（`frontend/apps/pc-admin/src/views/sales/**`、`views/erp/sale/**`）、后端（`backend/erp/erp-sales`）、数据库（devdb 销售域 29 张表），对照 `docs/Yh-Spec/手动整理对标开发文档/销售模块/*.md`（10 篇）与 `系统菜单设计与管理/*.md`。
**维度**：可用性 · 规范性 · 冗余 · 死代码 · 功能接通 · 跨模块关系 · 权限配置。
**口径**：所有结论均给出可复现证据（SQL 原文 / `文件:行号` / 实跑脚本）。区分「代码缺陷」与「E2E 造数残留」——后者不计为缺陷。

---

## 〇、摘要

| 级别 | 数量 | 代表问题 |
|---|---|---|
| **P0（已修）** | 1 类 8 条 | 销售退货单/出库单/退货申请/订单详情的按钮跳转**全部落到 404 页** |
| **P0（待拍板）** | 4 | 出库完成双写凭证 · 取消不冲应收 · 库存静默漏扣 · DMS 只有单号字符串关联 |
| **P1（已修）** | 2 类 | 3 处配置弹窗调用不存在接口（选项恒空）· 2 处批量打印文案与实现不符 |
| **P1（待处理）** | 5 | 部门维度断链 · 订单明细双列集 · 预订单 NOT NULL 必 500 · 销售页零 v-permission · 退货申请库存流水成孤儿 |
| **P2（待处理）** | 12 | 死 API/死菜单/僵尸权限码/模块归属错位/三套打印与三套查询方案持久化等 |
| **已删除死代码** | 4 文件 | 指向不存在的 `t_sale_*` 表的实体与 Mapper |

**整体判断**：销售模块的**功能骨架是通的**——18 个菜单入口页 100% 可正常打开且无接口报错；后端 224 个端点 100% 有鉴权注解、110 个权限码 100% 在库并已关联角色。问题集中在三类：① 前后端路径约定漂移（菜单 `client_type`/`visible` 与前端硬编码不匹配）；② 卖到财务/库存/DMS 的**跨界链路半通**；③ 同域内三套实现并存（打印、查询方案、列配置持久化）。

---

## 一、已修复（本轮，附验证）

### 1.1 【P0】8 条页面跳转死链 —— 销售退货单整页不可操作

**症状**：销售退货单列表的「新增 / 详情 / 编辑 / 打印」、退货申请列表「新增」、订单中心「详情」、各表单「历史」按钮，点下去内容变成 404 页（URL 不变，比被弹回工作台更隐蔽）。
**根因**：这批路径的菜单记录确实存在（`sys_menu.id` 80601/80602/80603/80604/80092），但配置为 `client_type='pc-admin'` 且 `visible=0`；而前端 `dynamicRoutes.ts` 顶部硬编码 `CLIENT_TYPE = 'tenant-admin'`，服务端 `SysMenuServiceImpl.getUserMegaMenus` 又强制 `client_type = ? AND visible = 1` ⇒ **这些菜单永远不下发，依赖菜单树注册的路由永远不存在**。

**修复**：在 `dynamicRoutes.ts` 的 `getRequiredRoutes()` 集中补齐 8 条路由（与既有 pre-order/retail 别名补齐的做法一致，页面代码零改动）：

| 路径 | 组件 | 来源 |
|---|---|---|
| `sales/return-doc/create` | return-doc/form.vue | 退货单列表「新增」 |
| `sales/return-doc/form/:id` | return-doc/form.vue | 列表「详情/编辑/打印」 |
| `sales/return-doc` | return-doc/index.vue | 表单「历史」（文档约定地址） |
| `sales/outbound/create` | outbound/form.vue | 单据查询「复制出库单」 |
| `sales/outbound` | outbound/index.vue | 出库单表单「历史」 |
| `sales/return-apply/create` | return-apply/form.vue | 退货申请列表「新增」 |
| `sales/return-apply` | return-apply/index.vue | 表单「历史」（文档约定地址） |
| `sales/order/form/:id` | erp/sale/form.vue | 订单中心「详情」、退货申请「跳源订单」 |

**验证**：`node tools/verify-sales-routes.cjs` → **22/22 通过**（修复前 14/22）。
> 该脚本第一版只比较 URL 是否被守卫弹回，得出"22/22 通过"的**错误结论**——因为 `dynamicRoutes.ts` 在 Layout 下注册了 catch-all `/:pathMatch(.*)*`，未注册路径 URL 同样不变、只是渲染 404 页。已把判定改为「URL + `.not-found` 元素 + 正文文案 + 组件未找到提示」四重检查。

### 1.2 【P1】3 处配置弹窗调用不存在的后端接口 —— 选项永远是空的

**证据**（修复前实测：全部 404）：

| 位置 | 原调用 | 结果 |
|---|---|---|
| `views/sales/outbound/form.vue:991` | `/md/logistics/list` | 404（后端无 `/api/md/logistics` 控制器） |
| `views/erp/column-config/SaleOutboundFormConfig.vue:371,374,377,380` | `/api/erp/partner/customer/list`、`/api/wms/warehouse/list`、`/api/system/user/list`、`/md/logistics/list` | 全部 404 |
| `views/erp/column-config/SaleReturnApplyFormConfig.vue:337,340,343,353` | 同上 | 全部 404 |

后两处还有额外的**双 `/api` 前缀**问题：`request` 的 `baseURL` 已是 `/api`，写成 `/api/erp/...` 会拼成 `/api/api/erp/...`。
异常被 `catch { selectorOptions.value = [] }` 吞掉 ⇒ 表现为「录单默认值」快速选择弹窗**永远是空列表、无任何报错**。

**修复后实测**（同一脚本，仅列关键行）：

```
200  /api/erp/md/customer/list?partnerType=LOGISTICS&...   数组1条
200  /api/erp/md/customer/list?partnerType=CUSTOMER&...    数组5条
200  /api/wms/warehouse/list-all                           数组3条
200  /api/user/list?status=1&pageSize=5                    数组5条
404  /api/md/logistics/list                                （原写法）
404  /api/api/erp/partner/customer/list                    （原写法）
```

### 1.3 【P1】2 处「批量打印」文案承诺 N 张、实际只打第 1 张

`views/sales/return-doc/index.vue:965`、`views/sales/return-apply/index.vue:1104`：弹窗写「将依次打开选中的 N 张单据进行打印」，`onOk` 只取 `selectedRowKeys.value[0]`，其余 N-1 张被静默丢弃。
**修复**：文案与实际对齐（单张 → 直接询问；多张 → 明确告知"本页一次只打印第一张，可返回列表继续"）。
> **真正的批量打印范本**在同模块 `views/sales/exchange/index.vue:814-830`（列表直接挂 `PrintDialog` + `printData.docs` 多张渲染 + 打印成功后调 `/batch-print` 回写次数）。退货单/退货申请列表未挂该组件，登记为待完善。

### 1.4 【P2】删除 4 个死代码文件

| 文件 | 证据 |
|---|---|
| `sale/entity/SaleExchange.java` | `@TableName("t_sale_exchange")`，该表**不存在**（`information_schema` 查无此表；全后端 `t_sale_` 仅出现在这两个实体里） |
| `sale/entity/SaleReturnDoc.java` | 同上，`@TableName("t_sale_return_doc")` |
| `sale/mapper/SaleExchangeMapper.java` | 全后端**零引用、零注入点** |
| `sale/mapper/SaleReturnDocMapper.java` | 同上 |

活实现分别在 `sale/saleexchange/entity/`、`sale/returnDoc/entity/`（`@TableName("erp_sale_exchange")` / `erp_sale_return_doc`）。
**已排除误删**：`sale/callback/SaleApprovalCallback.java` 虽是 0 处类名引用，但它是 `@Component implements ApprovalCallback`，由框架分发器按接口调用，**不可删**。
**验证**：`./mvnw -o -pl erp/erp-sales test-compile` → BUILD SUCCESS。

---

## 二、待拍板（P0，涉及财务口径，改动前需确认保留哪一套）

### 2.1 销售出库「完成」同时写两张收入凭证

`SaleOutboundServiceImpl.complete()` 在同一次调用里执行两条记账路径：

```java
// :1311  本地凭证（voucherService.create → 永远 draft）
generateAccountingVoucher(outbound);
// :1330  业财直调（BusinessAccountingService → 自动 post）
salesAccountingService.createReceivableOnShipment(...);
```

两者**分录语义重叠**——都做「借 应收账款 / 贷 主营业务收入」，且本地那份额外含成本段（6401/1405）。
**SQL 实证**（`finance_voucher`，每张单 3 张凭证）：

| voucher_no | status | remark | 金额 |
|---|---|---|---|
| KJPZ-20260910-047 | **draft** | 销售出库自动生成 - XSCK202609100002 | 320.00 |
| KJPZ-20260910-047 | **posted** | 销售出库凭证 - XSCK202609100002 | 200.00 |
| KJPZ-20260910-049 | **draft** | 取消出库自动红冲 - XSCK202609100002 | 200.00 |

**后果**：① 收入重复（一张悬挂草稿，一旦被人过账即翻倍）；② **成本结转只停在草稿**，总账里销售成本从未结转。
**建议**：保留业财直调（posted）为唯一收入入口，把本地 `generateAccountingVoucher` 降级为"仅成本结转"或整体移除；需先定"销售成本由谁结转"。

### 2.2 取消已完成出库单不冲销应收

`cancel()`（`SaleOutboundServiceImpl.java:1475`）只做 `reverseStock` + `generateReverseVoucher` + 客户信用更新，**没有任何冲销 `finance_receivable` 的代码**（销售侧 `receivable` 只在 `:1330` 出现一处写入）。
**SQL 实证**：`finance_receivable` 6 行全部 `source_type='SALE_SHIPMENT'`、`status='normal'`，而对应出库单状态均为 `12`（已取消）⇒ **已取消单据的应收永久挂账**。

### 2.3 红冲凭证永不过账

`generateReverseVoucher()`（`:1591`）走 `voucherService.create` 后无 `audit`/`post`，6 张「取消出库自动红冲」**全部 draft**，且金额只有 200（未含成本段）。⇒ 取消单据在总账上收入仍成立。

### 2.4 发货时库存可能被静默漏扣

`SaleOutboundServiceImpl.ship() → :1775` 发布 `InventoryChangeEvent(DECREASE)` 前有两处静默分支：
- `warehouse_id` 为空 → 抛「出库单未指定发货仓库」（有拦截）；
- **明细 `product_id` 为空 → `continue`，只打一行 `log.warn("无有效出库明细，未产生库存变动")`** ⇒ 单据正常发货、库存不动。
`wms_inventory_log` 佐证：`erp_sale_outbound` 有 3 行已发货 + 2 行已完成，但只有 6 张产生流水；`E2E-XSCKD-0001/0002` 的明细 `product_id` 全为 NULL。
**建议**：至少升级为业务异常或写入异常记账标记（现有「仅显示异常记账单据」筛选可复用）。

### 2.5 DMS 配送只有单号字符串关联，结算空表

- `dms_task.order_id` **96 行全空**；`dms_task.source_bill_no` 20 个 distinct 值中**只有 3 个**能对上 `erp_sale_outbound.outbound_no`；写入侧还把多单号**逗号拼接**成一个字段（`TaskService.java:514`）⇒ 即使单号不改，IN 精确匹配也永远匹配不上。
- `dms_settlement` / `dms_settlement_item` / `dms_dispatch_record` **全 0 行**；`SettlementService.generate` 是人工触发的接口，从未被调用。
- 影响面：配送台账、"发货查询"的配送状态/线路固定项、配送结算三处同时失真。

---

## 三、待处理（P1）

### 3.1 部门维度断链：销售用的是 0 行的那张部门表
`SaleOrderServiceImpl.java:91,1350-1356` 用 `SysDeptMapper` → `sys_dept`（**0 行**），真实部门在 `sys_department`（5 行）。
现状：`sys_user.dept_id` 75 行全 NULL、`erp_sale_order.dept_id` 28 行全 NULL ⇒ 部门在销售全链路是**纯文本快照、ID 恒空**，按部门的数据权限/辅助核算/报表归集全部失效。
> 与 `MASTER_TODO` PUR-BREAK-02 同一问题，已拍板「冻结不改、待部门启用后统一迁移」。

### 3.2 `erp_sale_order_item` 双列集：明细自定义字段写进去读不出来
表里同时存在 `custom_field1..10`（无下划线，实体按 MP 驼峰规则读写）与 `custom_field_1..10`（有下划线，`SaleOrderItemMapper.xml:50-59` 读）。
⇒ 实体写入 `custom_field1`，列表/详情从 `custom_field_1` 取值，**永远读不到**。
同款问题此前已在 `erp_sale_exchange` 收口过（`V11.130.0` 注释明确写了这个坑），`erp_sale_order_item` 漏了。

### 3.3 `erp_sale_pre_order` 四个 NOT NULL 无默认列 → 插入必 500
`order_no / customer_id / customer_name / order_date` 均 `NOT NULL` 且无默认值；`SalePreOrderController.java:445-463` 对每列"有才 set"，`toLocalDate` 对空值返回 null ⇒ 前端不传 `orderDate` 时直接 NOT NULL 违例。

### 3.4 销售页面零按钮级权限
后端备好了 110 个 `sale:*` 权限码（含 `:create/:approve/:delete/:export`），但 `views/sales/**` **0 处** `v-permission` / `hasPermission`；而全站其它模块有 89 个文件在用。
⇒ 无权限用户照常看到按钮，点下去才被后端 403。

### 3.5 退货申请的库存流水成孤儿且无生产者
`SaleReturnServiceImpl.java:370-393` 审核路径已明确不写库存（入库下沉到退货单收货），全仓无任何地方发布 `SALE_RETURN_APPLY`(INCREASE)；但 **`SALE_RETURN_APPLY_CANCEL`(DECREASE) 的生产者仍在**（`:458`），而申请侧没有任何代码给 `bookkeeping_time` 赋值 ⇒ 死路径 + 潜在误扣。
（`wms_inventory_log` 中 7 条 `SALE_RETURN_APPLY` 全部指向已不存在的单据。）

### 3.6 退货单/退货申请列表「批量打印」功能待补
见 §1.3，文案已修正，真正实现需挂 `PrintDialog`（范本：`sales/exchange/index.vue`）。

---

## 四、待处理（P2）

| # | 问题 | 证据 |
|---|---|---|
| 1 | **前端死 API** | `api/erp.ts:1378 returnOrderApi`（6 个方法全指向不存在的 `/erp/return/*`，零引用）；`shipmentApi` 零引用；`saleOrderApi.print` → `GET /erp/sale/order/{id}/print` 后端不存在 |
| 2 | **2 个订单中心页面并存** | `views/order-center/index.vue`（1950 行）仍在 `componentMap` 可直达；现行是 `views/sales/order-center/index.vue`（1489 行，菜单 80050）。`MASTER_TODO` 已列入删除清单 |
| 3 | **6 条 `client_type='pc-admin'` 菜单永不生效** | 80091/80092/80601-80604；无任何前端消费方（`CLIENT_TYPE` 硬编码 `tenant-admin`） |
| 4 | **双入口 70011 / 80091 同名同 path** | 文档《41条双入口菜单的正确配置》说明为有意双入口，但 `client_type` 差异使其中一条恒不可见 |
| 5 | **3 个僵尸权限码** | `sale:discount:edit`、`sale:manage`、`sale:settle:force`（库中有、代码零引用） |
| 6 | **权限码 `view`/`detail` 语义重复** | 11 个资源同时存在（如 `sale:order:view` 14 处 + `sale:order:detail` 3 处）；且 `sys_permission.api_path` 元数据与实际挂载点不符（`sale:order:view` 声明 `/api/erp/sale/order/stats`，实际挂在 page/export 等 14 个端点） |
| 7 | **权限名中英混杂** | `sale:*` 105 条中 92 条 permission_name 含英文资源名（如「销售exchange审批」「销售outbound查看」），与「配置界面文案用业务白话」的既定要求不符 |
| 8 | **模块归属错位** | `erp-sales` 内含 `PromotionController`（映射 `/api/sale/promotion`，消费方是营销前端）、`AccountDeliveryController`（`/api/erp/finance/account-delivery`）、`ProductKitController`（`/api/erp/product-kit`） |
| 9 | **打印实现三套并存** | ① `PrintDialog`（exchange/return-doc/form/doc-query/order-center）② 手写 `window.open`（retail、price-track、visit-*）③ 仅后端计数+提示（outbound 表单/列表：点「打印」只弹"打印成功"，无任何打印输出） |
| 10 | **查询方案三套持久化** | localStorage 单键 JSON（doc-query、exchange）/ localStorage 一方案一键 + 全表扫描（detail-query）/ 后端 `userPageConfigApi`（order-center） |
| 11 | **零售单配置三处存储 key** | 面板写 `retail-page-config`、页面读写 `retail-order-page-config`、后端 `userPageConfigApi` 又一处 |
| 12 | **"保存后立即打印"死开关** | return-doc/form.vue:1225 `maybePrintAfterSubmit` 定义后从未调用；retail/form.vue:398 `printAfterSubmit` 无消费点 —— 但配置面板都向操作员暴露了该勾选框 |
| 13 | **订单明细页自定义字段/死列** | `erp_sale_return` 的 `applicant_id/apply_time/sale_order_id/sale_order_no` 全空且实体无字段（退货申请无法记录申请人/来源订单）；`erp_sale_order` 13 列实体不映射 |
| 14 | **迁移与 Flyway 漂移** | `flyway_schema_history` 有 13 个 version 重复（同 script 同 checksum 不同 installed_on）；`V8.100.0` 已应用但仓库中不存在；125 处重复 `ADD COLUMN`（均带 `IF NOT EXISTS`，不阻断） |
| 15 | **主单据表单号无唯一索引** | `erp_sale_order/outbound/exchange/return_doc` 的单号列只有主键索引，重复单号仅靠生成器约束 |
| 16 | **`api/erp.ts` 对象字面量内重复键** | `outboundApi` 里 `print`（360 行）与 `batchPrint`（362 行）被 376/383 行的同名键静默覆盖，前两条属不可达代码 |
| 17 | **零售单「积分规则（每元积分数）」配置无效** | `retail/form.vue:350` 的 `retailSettings.pointPerYuan` 默认 1、可配置，但全文件仅出现在定义与默认值两处，实际积分按 `Math.floor(payableAmount)` 硬编码（`:1461`） |
| 18 | **「打印配置」Tab 在多页形同虚设** | `PageConfigPanel` 的打印配置 Tab 默认渲染，但 `return-doc/return-apply/retail/pre-order/visit-*` 列表页既不隐藏也不读取 `printConfig`；`exchange/index.vue` 未传 `:always-last-template` → 该勾选无效 |
| 19 | **全局列配置未落后端** | `BillDetailTable` 仅在传 `globalConfigKey` 时才存后端；`sales/order-center/index.vue:68`、`sales/retail/index.vue:167` 只传 `storage-key` ⇒ 「全局配置」在这两页只写 localStorage，跨浏览器不生效 |
| 20 | **`erp/sales-analysis/` 空目录残留** | `index.vue` 已删，目录只剩 `.`/`..` |
| 21 | **换货单制单人历史断档** | `erp_sale_exchange` 前 4 行写的是 `created_by/created_by_name`（当前代码树已无写入方），后 4 行才是实体的 `creator_id/creator_name` ⇒ 老数据的制单人列读不出来 |
| 22 | **`tenant_id` 默认值 0/1 不一致** | `erp_sale_return_doc(_item)`、`erp_sale_order_promo_detail` 为 `NOT NULL DEFAULT 0`，其余为 `DEFAULT 1`；走无会话路径时会落到 tenant 0（对所有租户不可见）。当前库未能复现，登记待观察 |
| 23 | **重复语义的迁移文件** | `V11.1.0` 与 `V11.2.0` 同名 `Fix_Budget_FixedAsset_Invoice_Metrics_Schema.sql`；`V9.41.0` 与 `V9.42.0` 同名 `Add_Sale_Outbound_Item_Fields.sql` |

---

## 四之二、与采购的对称性核查（P1）

销售与采购在本仓是**对称模块**（`AI_DEVELOPER_RULES.md` §9.3 要求「对称接口必须同步改」）。抽查订单/退货/换货/合同四组后，**三类均有单侧缺失**：

| 维度 | 销售 | 采购 |
|---|---|---|
| 单号生成 | 走集中式 `BizNumberGeneratorService.nextSaleOrderNo()` | **本地手搓** `"CG"+yyyyMMdd`（`PurchaseOrderServiceImpl:282`，内含 `Integer.parseInt` 无并发保护）；集中式 `nextPurchaseOrderNo()` 是**死代码** |
| 单号格式 | `前缀-yyyyMMdd-4位`（`XSDD/XSTHSQD/XSTHD/XSHHD`），出库 `XSCK`+8 位是唯一例外 | `字母前缀+yyyyMMdd` **无连字符** ⇒ 靠前缀解析单号做跨模块路由的代码会单侧失效 |
| 状态枚举 | **无** `SaleOrderStatus`，散落魔法数字（`SaleOrderServiceImpl:217/427/482/524/556`） | 有完整 `OrderStatus` 枚举，但 `:249` 取消写 `status=4`（枚举 4=IN_PROGRESS，CANCELLED=7），前端又定义 `4:'已取消'` ⇒ **同一状态三套口径** |
| 审批/快照流水 | `erp_sale_order_audit_trail` 3 行；`partner_snapshot` **0 行** | 15 行；10 行 |
| 合同能力 | **完全没有销售合同模块**（无表、无类、无控制器） | `purchase_contract` + 完整审批链（提交/审批/生效/终止/归档） |
| 换货 | `erp_sale_exchange` 9 行（代码+数据都在） | Controller/Service/号段齐备但 **0 行**（代码存在、运行时不存在） |

**销售有、采购缺**：订单的 11 个履约看板端点（`/ship`、`/payment`、`/pending`、`/customer-credit`、`/center/*` …）与审批时的**库存预检 + 冻结**（`checkAndFreezeStock`）。
**采购有、销售缺**：销售退货无 `statistics` / `batch-print` / `order/{orderId}` / `from-order/{orderId}` / 明细行 CRUD。
**唯一真正对称做对的是财务预检模式**：`SaleOutboundServiceImpl:1296` ↔ `PurchaseInboundServiceImpl:495`，两者都刻意放在记账 try/catch **之外**。

> 本节多数根因在采购侧，但「销售无状态枚举」「销售无合同模块」是销售侧的缺口，且**单号格式不对称**会直接影响任何按前缀解析单号的串联逻辑。

---

## 五、检查方法与复现

| 脚本 | 用途 | 本次结果 |
|---|---|---|
| `tools/audit-sales-module.py`（新增） | 前端调用 ↔ 后端端点双向比对、端点鉴权覆盖 | 端点 224、断链 7、冗余候选 18、无鉴权端点 **0** |
| `tools/verify-sales-routes.cjs`（新增） | 逐条打开页面内跳转目标，判定路由是否存在 | 修复前 14/22 → 修复后 **22/22** |
| `tools/verify-sales-pages.cjs`（新增） | 打开 18 个菜单入口，收集失败请求与 console 错误 | **0/18 有问题** |
| `tools/check-menu-targets.py`（既有） | 菜单组件文件存在性 / 重名 / 一组件多菜单 | 组件缺失 0；重名 3 组 |
| `tools/scan-unguarded-controllers.py`（既有） | 整类零权限注解的控制器 | 销售域 0 |

运行时前提：后端 `5655`、前端 `5656`（`vite`）在跑；测试账号 `sra_e2e / admin123`（系统租户）。
> ⚠️ 会话陷阱：`sa-token` 单端登录，脚本**必须全程共用一个 context**。第一版 `verify-sales-pages.cjs` 每页重新 `login()`，第二次登录把上一次踢下线，导致从被踢那页起刷出成片的 `500 /api/auth/captcha`、`500 /api/auth/logout` 与业务接口 500——全是会话失效的连锁反应，会让整份报告失真。

---

## 六、可确认无问题的项

- **后端鉴权覆盖**：`erp-sales` 224 个端点 **100%** 带 `@SaCheckPermission`（0 个裸端点）。
- **权限码闭环**：代码引用的 110 个权限码**全部**存在于 `sys_permission`（deleted=0）且**全部**已关联角色 ⇒ 非超管不会因缺码被 403。
- **软删/租户**：20 张含 `deleted` 列的表对应实体均有 `@TableLogic`；9 张无该列的子表实体也无——无「实体逻辑删除但表无列」组合。
- **主键**：销售域在用的实体均为 `IdType.ASSIGN_ID`（雪花 ID），不存在"缺主键序列致建档必 400"。
- **CRM 口径**：销售订单/出库单客户来自 `biz_party`（152 行），全仓 `grep "crm_customer" erp/` 零命中——与既定裁定「CRM=公海客户、ERP=往来单位」一致。
- **迁移版本号**：`V*.sql` 无重复版本号，无 `V11.x` 号段冲突。
- **前端残留**：sales 下无 `.bak/.old/.orig`；无成段注释掉的死代码；所有动态 `import()` 路径均指向存在的文件。

---

## 七、修复进度（2026-09-23 补齐）

### 已完成并验证

| 批次 | 内容 | 验证 | 提交 |
|---|---|---|---|
| P0 路由 | 8 条跳转死链补齐 | `verify-sales-routes.cjs` 22/22 | `6bd2f494` |
| P0 财务 | 出库完成双写凭证收口（收入+成本同凭证、自动过账）· cancel 补作废应收 · 红冲改业财直发含成本段 | `test-compile` SUCCESS + `SalesAccountingServiceTest` 8/8 | `9b8a3044` |
| P0 库存 | 发货时「无可过账明细」由 log.warn 放行改为抛业务异常 | 同上 | `9b8a3044` |
| P1 数据 | 订单明细 XML 读列 `custom_field_1..10` → `custom_field1..10` · 预订单 order_no/order_date/customerName 兜底 | 同上 | `9b8a3044` |
| P1 权限 | 9 个销售页面接入 `v-permission`（与后端同一套码） | `vite build` SUCCESS | `44e73749` |
| P1 接口 | 3 处配置弹窗调用不存在的端点（客户/仓库/职员/物流公司） | 实测 404 → 200 有数据 | `6bd2f494` |
| P2 前端 | 删 3 处死 API + `outboundApi` 重复键 + 5 个未使用符号 + 空目录 | `vite build` SUCCESS | `9b8a3044` |

### 复核后**判定为非缺陷**（不改，附理由）

| 项 | 原判 | 复核结论 |
|---|---|---|
| 退货申请 `applyStockDecrease` 是"死路径 + 潜在误扣" | P1 | **有意保留的兼容分支**：方法注释明确写「仅用于兼容改造前已过账的存量单据」，且 `bookkeepingTime != null` 时取消单据冲回库存是**正确**行为。当前库 25 行 `erp_sale_return` 的 `bookkeeping_time` 全为 null，故不触发 —— 属防御历史数据，非缺陷。 |
| `SaleApprovalCallback` 零引用 | 死代码候选 | `@Component implements ApprovalCallback`，由框架分发器按**接口**调用，类名 grep 必然为 0。**不可删**。 |

### 仍未处理（连同理由）

| 项 | 级别 | 为什么不在这批做 |
|---|---|---|
| 退货单/退货申请列表「真实批量打印」 | P2 | 需给两个列表挂 `PrintDialog` + 后端 `batch-print` 端点（现无），是**功能开发**而非补漏；范本见 `sales/exchange/index.vue` |
| 「保存后立即打印」死开关（return-doc / retail） | P2 | 该页保存后即 `router.push` 跳列表，接打印需改 `useBillForm` 的 `redirectPath` 契约（影响多个页面），收益仅一个默认关闭的开关 |
| 零售单「每元积分数」配置无效 | P2 | 需改积分计算口径（现为 `Math.floor(应收)` 硬编码），属业务规则变更 |
| 零售单 storage-key 三套并存 | P2 | 统一会**迁移已有用户的配置**，需先定哪套是权威 |
| `order-center`/`retail` 全局列配置未落后端 · 打印配置 Tab 形隐 | P2 | 低风险但纯体验项，可随下批一起 |
| 打印三套实现 / 查询方案三套持久化 / 模块归属错位 | P2 | **架构收敛**，需先定统一方案（文档裁定 + 影响面评估），不宜审计中顺手改 |
| 6 条 `pc-admin` 死菜单 · 3 个僵尸权限码 · 92 条权限名中英混杂 · view/detail 码重复 | P2 | 全部是 `sys_menu` / `sys_permission` **系统配置数据**，按既定纪律须先出方案获批再改库（见 §八） |
| 部门维度断链（`sys_dept` 0 行） | P1 | 已在 `MASTER_TODO` PUR-BREAK-02 拍板「冻结不改、待部门启用后统一迁移」 |
| DMS 单号级关联 / 采购对称性缺口 | P0/P1 | 跨模块架构改造，超出销售模块收尾范围 |

---

## 八、需拍板的库数据变更（未执行）

以下都是「改一行数据就能修好、但会影响所有租户可见性」的项，按纪律**先给方案不动库**：

1. **6 条 `client_type='pc-admin'` 菜单**（80091/80092/80601-80604）：前端只请求 `tenant-admin` 菜单 ⇒ 这 6 条永不生效。
   *建议*：把 80091 改为 `tenant-admin`（与 70011 构成真正的双入口）或删除；80601-80604/80092 本就是隐藏路由，路由已在前端 `getRequiredRoutes()` 补齐，**可直接删除这 5 条**。
2. **3 个僵尸权限码**：`sale:discount:edit`、`sale:manage`、`sale:settle:force`（代码与前端全仓零引用）。
   *建议*：确认非预留后删除，或补上消费方。
3. **权限名中英混杂**（`sale:*` 105 条中 92 条）：如「销售exchange审批」「销售outbound查看」。
   *建议*：批量改成业务白话（「销售换货单-审批」），系全库 976/1835 条的共性问题，宜按模块分批。
4. **`view`/`detail` 语义重复**：11 个资源同时存在两套「查看」码。
   *建议*：先定统一的读码（本仓 `purchase:*` 用 `detail`），再逐资源合并；合并涉及角色授权数据迁移。
5. **`finance_account_subject` 里 1403 与 1405 同名「库存商品」**：凭证两侧各用一个编码（退货侧曾是 1403，已统一到 1405）。
   *建议*：把 1403 更名为「原材料」（准则口径）或删除，并核对历史凭证。
