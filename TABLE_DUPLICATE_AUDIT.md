# 数据表重复审计（收尾盘点）

> 生成时间：2026-09-19
> 数据源：`devdb.public`，**553 张表**（含 1 张 Flyway 元数据表，已排除）
> 配套：孤儿页面/死代码见 `CLEANUP_SCOPE_20260919.md`；本文件只覆盖「表重复」这一维度。

---

## 0. 口径与可复现命令

```bash
python tools/audit-duplicate-tables.py        # 表名/结构相似分组 + 代码引用分档
python tools/audit-duplicate-tables-diff.py   # 候选组的结构差与数据比对
```

产物：`tools/audit-duplicate-tables.json`、`tools/audit-duplicate-tables-diff.md`。

**三路判定**：

| 路径 | 规则 |
|---|---|
| A 表名同类 | 去审计后缀（`_bak/_old/_copy/_tmp/_v1`…）、去模块前缀（`erp_/biz_/sys_/wms_`…）、去复数尾 `s` 后同名 |
| B 结构相似 | 两表列名集合 Jaccard ≥ 0.70 且列数 ≥ 4 |
| C 数据比对 | 按业务键（`name/code/config_key`…，采样 500）求交集判「是否同一批数据」 |

**引用分档**（判定表死活，逐档收紧）：

| 档 | 含义 |
|---|---|
| `mapper_xml` / `raw_sql` | 有真实查询代码（Mapper XML / 字符串字面量里的 DML） |
| `entity` | 有 `@TableName`（MyBatis-Plus）或 `@Table(name=)`（JPA）实体 |
| `ddl` / `seed` / `migration` | 只被建表脚本或 `schema.sql`/`data.sql` 提到，**不算业务引用** |
| `java_other` / `frontend_name` | 只作为同名标识符出现（`users`、`permissions` 变量），**不算引用** |

### 已知局限（勿把结论当铁证）

1. **行数**：`reltuples` 未 analyze 时恒为 0，脚本对 0 行表做了精确点算（402 张中 71 张实为有数据）。非 0 行仍是估算。
2. **`有实体但无查询`不等于死表**：MyBatis-Plus 的 `BaseMapper<T>` 靠实体 `@TableName` 工作，隐式 CRUD 不写 SQL。这一档必须看 Service/Controller 是否调用该 Mapper 才能定性（本轮未做）。
3. **不覆盖动态表名**：拼接生成的表名（`"t_" + type`）扫不到。
4. **`id` 刻意不作为比对依据**：自增 id 必然重叠，是伪证据。

---

## 1. 总量分档

| 档 | 数量 | 说明 |
|---|---|---|
| 有查询代码 | **230** | 活表 |
| 有实体但无任何查询 | **250** | 需二级甄别（见局限 2）；按前缀聚簇：`erp_stock_*` 16、`erp_product_*` 9、`hr*` 7、`erp_purchase_*` 6、`fixed_asset_*` 6、`mall*` 6 |
| 无实体无查询 | **72** | 孤儿表候选，见 §3 |

**核心结论：没有发现「同一批数据存两份」的铁证** —— 所有候选对按业务键比对均无交集。本项目的表重复是**「新旧两套实现并存」+「建了表没接线」**两种形态，不是数据双写。

---

## 2. A 类：同一业务两套实现（需裁定，9 组）

### A-01 部门主数据两套 —— **风险最高，优先处理**

| 表 | 行数 | 列数 | 实体 | 查询代码 |
|---|---|---|---|---|
| `sys_department` | 2 | 19 | `base/entity/Department.java`、`hr/ref/HrDepartmentRef.java` | `DepartmentMapper.xml`、财务收款统计 |
| `sys_dept` | **0** | 15 | `base/entity/SysDept.java` | `SysDeptMapper.java` 等 5 处 |

- 两表列重合 **18/19**（差 `create_by/update_by/ancestors/leader_name/version` vs `leader`）—— 是同一实体。
- **`sys_dept` 是空表，却仍被 5 处 SQL 引用**，其中两处会产生实际错误：
  - `erp-purchase/.../PurchaseOrderMapper.xml:76,198` —— 采购订单列表 `LEFT JOIN sys_dept d ON d.id = o.dept_id` ⇒ **部门名恒为 null**。
  - `erp-finance/.../AuxBalanceMapper.java:71` —— 辅助核算 `SELECT d.dept_code FROM sys_dept d WHERE d.dept_name = t.aux_value` ⇒ **核算项编码恒为 null**。
- 两套表都在活代码路径上（各自有 Mapper），不是单纯的死代码。
- **修正（2026-09-19 复核）**：`SysDeptMapper` 的引用面比初版记录更广 —— 除 5 处报表 SQL 外，还有
  **4 个 Java 注入方**：`WorkflowServiceImpl`（按部门找审批人）、`DataScopeAspect`（**数据权限切面**）、
  `RbacService.isDescendantDept`（部门层级判断）、`dms/RiderService`。
  所以收敛不是「改 5 处 SQL」，而是**连同数据权限链路一起迁移**。好消息是 `SysDeptMapper` 自定义方法
  实际只被调用了 `selectChildrenByParentId` 一处，其余是继承的 `selectById`/`selectList`，
  `DepartmentMapper` 都有对应能力（`selectByParentId` 等）。
- **处置建议（已下调风险等级）**：**暂不动代码**。理由有二 ——
  ① 迁移触及 `DataScopeAspect`（数据权限），属高风险区；
  ② 部门功能当前未启用（`sys_user.dept_id` 73 行全 NULL、`erp_purchase_order.dept_id` 11 单全 NULL），
  **改完无法端到端验证对错**，「无法验证的重构」比不改更危险。
  建议待部门功能启用、库里有真实部门数据后，再按「迁移 4 个 Java 消费方 → 改 5 处 SQL → 删 SysDept/SysDeptMapper
  → 观察期后 DROP TABLE」推进。当前只在 SysDept 类上加 `@Deprecated` 注释标记。
- **负责**：HR 模块 + ERP 采购/财务 + 数据权限负责人 + 业务确认

### A-02 费用报销两套（同模块两个包）

| 表 | 行数 | 实体所在包 |
|---|---|---|
| `erp_expense_item` | 53 | `erp/finance/expensedoc/entity/ExpenseItem.java` |
| `expense_item` | **0** | `erp/expense/model/ExpenseItem.java` |
| `erp_expense_approval` | 51 | `erp/finance/expensedoc/entity/ExpenseApproval.java` |
| `expense_approval` | **0** | `erp/expense/model/ExpenseApproval.java` |

- `expensedoc`（新，有数据）与 `expense`（旧，0 行）两套并存，列数差异大（16 vs 39、18 vs 20），**不是简单复制，是两次独立实现**。
- **修正（2026-09-19 复核）**：旧包（`erp/erp-finance/.../erp/expense/`，31 个 java 文件）**零外部调用** ——
  全仓只有 `AiReadyApplication.java:125` 的实体扫描清单列了 `cn.aiedge.erp.expense.model`，
  没有任何 Service/Controller 注入它的 Mapper。其 `expense/mapper/ExpenseItemMapper.java:15-27` 的 5 个
  `@Select` 确实查 0 行的 `expense_item`，但**无人调用，不产生运行时错误**。
  （初版报告写的「查询恒返回空」据此更正。）
- **处置建议（2026-09-19 再修正，勿整体删）**：旧包含 **6 个 Controller**，是 HTTP 入口，不受上面
  「零调用」结论覆盖。实测消费者分两类：
  - `/api/erp/expense/statistics/*`（`ExpenseAnalyticsController`）—— **仍在使用**：
    `frontend/apps/pc-admin/src/api/analytics-finance.ts:162-172`、`api/analytics.ts:760,832,836`
    在调，服务于「查费用（80454）」页面。**必须保留。**
  - `/api/erp/expense/{application,reimbursement,approval,payment,type}/*`（另 5 个 Controller）——
    消费者是 `api/erp/expense/index.ts`，而该文件只被 `views/erp/expense/{application,approval,reimbursement}/`
    这些**孤儿页**引用（正是 `CLEANUP_SCOPE` §2.2 第 14–18 项，且 payment/statistics 两页已在工作区删除）。
    费用类型 `/erp/expense/type/*` 前端零调用。
  - 因此正确姿势是**先删孤儿页，再删这 5 个 Controller 及其 Service/Mapper/Model**，
    顺序反了会留下 404 端点。**本轮不删**（依赖孤儿页批次先落地）。
- 与 `CLEANUP_SCOPE` Ｄ-07 相关（`finance/expense-apply|expense-pay|expense-reimburse` 三个孤儿页调不存在的端点）。
- **负责**：后端（erp-finance）

### A-03 供应商两套（同一模块内两个实体）

| 表 | 行数 | 列数 | 实体 |
|---|---|---|---|
| `supplier` | 0 | 13 | `erp-supplier-portal/.../supplier/entity/Supplier.java` |
| `erp_supplier` | 0 | 44 | `erp-supplier-portal/.../supplier/model/entity/SupplierEntity.java` |

- 同属 `erp-supplier-portal` 模块，两个实体、两张表，**都是 0 行**。
- 供应商数据实际可能在统一往来单位模型 `biz_party`（`CLEANUP_SCOPE` 的对标方向）。
- **处置建议**：确认实际数据落点（`biz_party` 还是哪张）后，两套表一起废弃。
- **负责**：后端（erp-partner / erp-supplier-portal）

### A-04 打印 v1 / v2 两套

| v1 表 | v2 表 | 实体 |
|---|---|---|
| `erp_print_task` (0行) | `sys_print_task` (0行) | `printing/entity/PrintTask.java` vs `printing/entity/v2/SysPrintTask.java` |
| `erp_print_template` (0行) | `sys_print_template` (0行) | `printing/entity/PrintTemplate.java` vs `.../v2/SysPrintTemplate.java` |

- `CLEANUP_SCOPE` §2.3 第 6–9 项列出 `printing/{chain,client,task,template}` 四页未挂菜单，与这里是同一件事。
- **处置建议**：随打印管理菜单（61205）补挂时一并定版，v1 整体删除。
- **负责**：后端 + 前端

### A-05 客户等级价两套（两个同名类）

| 表 | 行数 | 实体 |
|---|---|---|
| `biz_customer_grade_price` | 0 | `erp/party/entity/CustomerGradePrice.java` |
| `erp_customer_grade_price` | 18 | `erp/stock/entity/CustomerGradePrice.java` |

- **两个模块各有一个同名实体类**，各映射一张表。有数据的是 `erp-stock` 那套（属「商品价格管理-子标签3」）。
- **处置建议**：删除 `erp-party` 那套（含实体）。与 `CLEANUP_SCOPE` D-10（`priceLevelConfig.ts` 契约零引用）属同一片价格域。
- **负责**：后端（erp-partner）

### A-06 复数命名的旧 RBAC 五表（全部 0 行，无实体、无查询）

| 表 | 行数 | 列数 |
|---|---|---|
| `users` | 0 | 21 |
| `roles` | 0 | 13 |
| `permissions` | 0 | 16 |
| `user_roles` | 0 | 2 |
| `role_permissions` | 0 | 2 |

- 五张表互相有外键，只在 `V5.0.0__Consolidate_DDL_And_Seed_Data.sql` 建出，**没有任何实体、Mapper、查询**。
- 实际 RBAC 走 `sys_user`(75) / `sys_role`(5) / `sys_permission`(454) / `sys_user_role`(46) / `sys_role_permission`(490)。
- **处置建议**：**可直接列为删除候选**（这五张是本次新发现，`CLEANUP_SCOPE` 未收录）。
- **负责**：后端（core-base）

### A-07 两套 key-value 配置

| 表 | 行数 | 列数 | 定位 |
|---|---|---|---|
| `dms_config` | 97 | 12 | DMS 租户配置（键前缀 `dms.*`，`tenant_id=0` 全局 + 租户覆盖） |
| `sys_config` | 173 | 22 | 系统参数配置表（后台「系统参数」页维护） |

- 列集合**完全不重叠**（共有业务列 0），业务键无交集 ⇒ **不是表级重复**。
- 但两者都是「key-value + 分组 + 启用开关」的配置存储，属**能力重复**：后续新增参数不知道该进哪张表。
- **处置建议**：不删表；**出一份《参数落位规则》**（DMS 业务参数进 `dms_config`，系统级参数进 `sys_config`），并在开发规范里固化。
- 与 `CLEANUP_SCOPE` D-11/D-12 同属「文档与代码不一致」范畴。
- **负责**：架构 + 开发负责人

### A-08 仓库主数据两套

| 表 | 行数 | 列数 | 实体 |
|---|---|---|---|
| `erp_warehouse` | 105 | 19 | `erp/stock/entity/Warehouse.java` |
| `wms_warehouse` | 3 | 18 | `wms/entity/WmsWarehouse.java`（注释：仓库扩展信息） |

- `wms_warehouse` 有实体但**无任何查询代码**，3 行数据无法确认来源。
- **处置建议**：确认是否为「仓库扩展信息」的独立设计；若是半成品则删除，若确需扩展信息应改为 `erp_warehouse` 的 1:1 扩展表并明确写入路径。
- **负责**：后端（wms）

### A-09 事件发件箱两套

| 表 | 行数 | 实体 |
|---|---|---|
| `dms_event_outbox` | 282 | `dms/event/entity/DmsEventOutbox.java` |
| `wms_event_outbox` | 0 | `wms/entity/WmsEventOutbox.java`（注释：WMS→ERP 异步通知） |

- `wms_event_outbox` 有实体、无查询、0 行 ⇒ 未接线的半成品。
- **处置建议**：确认 WMS→ERP 通知是否已由别的机制承担（如 DMS 发件箱复用）；未实现则删表 + 删实体。
- **负责**：后端（wms）

---

## 3. B 类：孤儿表 72 张（无实体、无查询）

按簇归并（完整清单见 `tools/audit-duplicate-tables.json` 的 `bucket_tables.orphan`）：

| 簇 | 张数 | 说明 |
|---|---|---|
| `fee_*` | 7 | 费用报销老模型（`CLEANUP_SCOPE` D-13 已列） |
| `erp_marketing_*` | 6 | 营销老模型（D-13 已列） |
| `fin_*` | 12 | 财务报表/分析老模型（**本轮新增**） |
| `erp_kit_*` | 4 | 组装/拆分（D-13 已列） |
| `erp_partner_*` | 4 | 往来单位旧表簇（被 `biz_party` 统一模型取代） |
| `purchase_supplier_*` | 3 | 供应商寻源/评估（**本轮新增**） |
| `erp_supplier_*` | 3 | 供应商权益/通知/积分规则（**本轮新增**） |
| `dms_*` | 5 | `dms_logistics_ship`/`dms_return_receive`/`dms_ship_order`/`dms_purchase_receive`/`dms_dispatch_record`（**本轮新增**） |
| `sys_job*` | 4 | `sys_job`/`sys_job_log`/`sys_task_execute_log`/`sys_scheduled_task`（调度旧表，现用 `scheduled_task` 18 行） |
| 缓存类 | 4 | `batch_rule`/`batch_snapshot_cache`/`inventory_query_cache`/`serial_status_cache` |
| `permissions/roles/users/user_roles/role_permissions` | 5 | 见 A-06 |
| 其它 | 15 | `biz_party_address`、`crm_erp_customer_mapping`、`erp_finance_subject`、`finance_auxiliary_balance`、`mall_category`、`purchase_order_item`、`purchase_quote_*`、`traceability_log` 等 |

**处置**：与 `CLEANUP_SCOPE` D-13 一致 —— 删表属**格式决策**，须先备份 + 观察 1–2 个发布周期，**本轮只登记不删**。
**与 D-13 的差异**：D-13 记录 30 张，本轮口径更宽（把「只被建表脚本提到」也算孤儿），故为 72 张；两者不冲突，D-13 是本清单的子集。

**特别提醒（沿用 D-14）**：`mkt_presale_order`、`biz_party_transaction` **不在**本孤儿清单内 —— 它们被 `MarketingQueryMapper.java`、`PartyMapper.java` 的 raw SQL 引用 ⇒ **实体删除 ≠ 表可删**。

---

## 4. C 类：看似重复实则不同（明确保留，防误删）

以下几组结构相似度高（0.70–0.96）但业务不同，**不得合并**：

| 组 | 为什么不是重复 |
|---|---|
| `erp_stock_in_item` / `erp_stock_out_item` / `wms_borrow_order_item` | 入库明细 / 出库明细 / 借出明细，三个业务环节 |
| `erp_stock_damage` / `erp_stock_in` / `erp_stock_out` / `erp_stock_overflow` | 报损 / 入库 / 出库 / 报溢，独立单据类型 |
| `wms_pick_detail` / `wms_receipt_detail` / `wms_ship_detail` / `wms_move_detail` / `wms_putaway_detail` | 库内五环节明细，字段同构是设计使然 |
| `erp_pre_payment` / `erp_pre_receipt`（含 `_item`） | 预付 / 预收，方向相反 |
| `erp_payment_item` / `erp_receipt_item` | 付款 / 收款 |
| `finance_payable` / `finance_receivable` | 应付 / 应收 |
| `erp_purchase_exchange*` / `erp_sale_exchange*` | 采购换货 / 销售换货 |
| `erp_purchase_price_track` / `erp_sale_price_track` | 采购价跟踪 / 销售价跟踪 |
| `sys_role_menu` / `sys_tenant_menu` | 角色授权 / 租户授权 |
| `sys_role_permission` / `sys_user_role` | 都是 6 列关联表，结构必然相似 |
| `biz_party_category` / `erp_product_category` / `erp_warehouse_category` / `mall_category` | 分类树模式复用（但 `erp_partner_category` 0 行可单独议） |
| `erp_sale_return`(25行) / `erp_sale_return_doc`(1行) | **退货申请**（`V8.99.9__Create_Sale_Return_Apply_Tables`）与**退货单**（`V9.45.0__Create_Sale_Return_Doc_Tables`）是两个业务阶段，字段复制是历史原因，非重复 |

> `erp_sale_return` / `erp_sale_return_doc` 共 133/132 列且高度重合，虽非重复，但**字段复制到了维护危险的程度**：改一处漏一处。建议列入「字段收敛」专项（非本轮）。

---

## 5. 处理顺序与风险

**顺序不能反**（先定功能归属，再定表去留）：

```
1. A-01 部门表裁定（涉及采购/财务取数错误，优先级最高）
2. A-02 费用报销两套（旧 Mapper 查空表，属缺陷）
3. A-06 五张旧 RBAC 表（零引用，最安全的删除项）
4. A-03/A-04/A-05/A-08/A-09 逐组裁定（需业务确认）
5. B 类 72 张孤儿表 —— 备份 + 观察期后统一处理
6. A-07 出《参数落位规则》文档（不涉及删表）
```

**风险提示**：

- 删表前必须：① 备份；② 确认无外键指向（本轮已记录 `fk_in`）；③ 确认无视图/存储过程引用（已记录 `used_by_views`）；④ 有回滚手段。
- `sys_dept` 与 `sys_department` 都**不能先删**：必须先统一引用方，否则采购/财务取数会直接报错或全表扫空。
- A-06 五张表带互相外键，删除需按 `role_permissions`/`user_roles` → `roles`/`users`/`permissions` 的依赖顺序。
- 每笔删除落 Flyway 迁移，勿手工改库。

## 6. 待填写的决策记录

| 编号 | 事项 | 保留哪套 | 处置方式 | 确认人 | 日期 |
|---|---|---|---|---|---|
| A-01 | 部门主数据 | | | | |
| A-02 | 费用报销 | | | | |
| A-03 | 供应商 | | | | |
| A-04 | 打印 v1/v2 | | | | |
| A-05 | 客户等级价 | | | | |
| A-06 | 旧 RBAC 五表 | 删除全部 | | | |
| A-08 | 仓库扩展 | | | | |
| A-09 | WMS 事件发件箱 | | | | |

---

## 7. 执行记录（2026-09-19）

本轮只做「只动代码、不动表」的 A 批。实际执行过程中有两项原方案被证伪，已在上文就地修正。

| 事项 | 状态 | 说明 |
|---|---|---|
| A-01 部门表 | **暂停（方案已修正）** | 复核发现 `SysDeptMapper` 有 4 个 Java 注入方（含 `DataScopeAspect` 数据权限），且部门功能未启用 ⇒ 改完无法验证。改为「先在 `SysDept` 上标 `@Deprecated` 注释，待部门启用后再迁移」 |
| A-02 旧 expense 包 | **暂停（方案已修正）** | 复核发现旧包有 6 个 Controller，其中 `/api/erp/expense/statistics/*` 仍被「查费用 80454」页面调用 ⇒ 不可整体删。改为「先删孤儿页，再删其余 5 个 Controller」 |
| A-06 旧 RBAC 五表 | 未执行（属 B 批删表） | 无实体无查询，最安全，但删表需备份 + 观察期 |
| 其余 A 类 | 未执行 | 未取得业务确认 |

**教训（值得写进方法论文档）**：“某包无 Java 引用”**不能**推出“该包可删” —— Controller 是 Spring 扫描注册的 HTTP 入口，
不需要任何人 import 它。判定一个功能是否死掉，必须同时查「Java 引用」与「前端/外部实际调用」。

同时确认了一条重要的**判定纪律**：对涉及数据权限、且当前无数据可验证的重构，
「不改」优于「改了但无法验证」。
