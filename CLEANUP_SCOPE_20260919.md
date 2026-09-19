# AI-Ready 开发收尾盘点与范围裁定

> 生成时间：2026-09-19
> 范围：开发收尾（不含上线动作）。三类遗留 = 缺陷清单 / 孤儿页面清单 / 死代码清单。
> 配套：死代码细节见 `CLEANUP_AUDIT_REPORT.md`（第二轮，2026-09-19）；本文件聚焦「三类遗留全量盘点 + 本轮做什么」。

---

## 0. 盘点口径与可复现命令

| 维度 | 数据来源 | 复现命令 |
|---|---|---|
| 菜单/组件一致性 | PostgreSQL `sys_menu`（389 行，deleted=0）+ `dynamicRoutes.ts` + `views/` 磁盘文件 | `python tools/check-menu-targets.py` |
| 孤儿页面 | 同上三方交叉 | `python tools/audit-orphan-pages.py` + `python tools/classify-orphan-pages.py` |
| 后端鉴权覆盖率 | `backend/` 全量 `@RestController` 扫描（排除 target/test） | 见 §1 D-02 口径 |
| 接口冗余 | 前端 6 端请求 × 后端 mapping | `python tools/audit-api-usage.py` |
| 权限码 | 代码注解 × `sys_permission` | `python tools/audit-permission-codes.py` |
| 装配完整性 | `scanBasePackages` 闭包 × 控制器包 | `ComponentScanCoverageTest`（已落地门禁） |

菜单表分布：`tenant-admin` 343 / `system-admin` 40 / `pc-admin` 6。

---

## 1. 缺陷清单（按阻塞分级）

### 1.1 阻塞上线（P0，必须本轮修）

#### D-01 坏菜单：采购换货单「新增/编辑」不可用
- **证据**：菜单 `id=70063`「采购换货单」`component='views/erp/purchase-exchange/form.vue'`，该文件**不存在**（目录 `views/erp/purchase-exchange/` 已无）。git 历史显示实现在 `2e08cce53` 时位于该目录，其后搬迁到 `views/purchase/exchange/`，菜单未同步。
- **影响**：菜单本身能点开（`display_mode=1`，走 `list_path='purchase/exchange/index'`，该键在 `componentMap:405-406` 有映射）；点「新增/编辑」时 `getComponent` 解析失败，落入 `dynamicRoutes.ts:946-953` 的兜底，页面显示「页面组件未找到: erp/purchase-exchange/form」。**采购换货单无法录入**。
- **修复**：`componentMap:407` 已有 `'purchase/exchange/form'` 映射，故只需一行 SQL：
  ```sql
  UPDATE sys_menu SET component = 'purchase/exchange/form' WHERE id = 70063;
  ```
  落成 Flyway 迁移（勿手工改库）。
- **验证**：以 tenant-admin 登录 → 采购 → 采购换货单 → 新增，页面正常渲染且无 console error。
- **负责**：后端（迁移）+ 前端（回归）

#### D-02 后端接口鉴权覆盖率缺口（最大一块）
- **实测口径**：`backend/` 下 `@RestController`/`@Controller` 文件，排除 `target/`、`src/test/`；判定单位是「文件内是否出现任何访问控制注解」（类级注解覆盖整类）。
- **实测数字（2026-09-19 复核口径）**：

  | 指标 | 数值 |
  |---|---|
  | Controller 总数 | 396 |
  | 接口方法总数 | 3563 |
  | 无 `@SaCheckPermission` | **330（83%）**，覆盖 2916 个端点 |
  | 无任何访问控制注解 | **243（61%）**，覆盖 2170 个端点 |

  > 口径已精化：**必须把自定义注解计入**（`@RequirePermission` 16 处、`@RequireRole`、
  > `@RequiresPermission`、`@RequireFilePermission`），其中 `@RequirePermission` 由 core-api 的
  > `PermissionAspect` 通过 AOP **真实执行**。不计入它们会把缺口从 243 高估到 253。
  > 也**刻意不计入** `@DataPermission`/`@DataPermissionCheck`/`@BusinessPermissionCheck`
  > —— 它们做数据范围过滤，不决定接口可达性。
  > 落地与分批计划见 `CLEANUP_DECISIONS_20260919.md` §2；门禁已落地（首次运行即抓到 1 个真实违例）。

- **性质**：鉴权主要靠前端菜单遮挡 + 登录态（`SaInterceptor` 的 `checkLogin` 生效）。**登录后的越权**（低权限用户直调高权限接口）无后端拦截。
- **重灾区**（无任何 `@SaCheck*` 的业务 CRUD 控制器，完整清单可由 §0 口径复跑）：`crm/*`（8）、`erp-finance/*`（约 30，含收款/付款/凭证/总账/月结/科目）、`erp-fixed-asset/*`（8）、`erp-budget/*`（7）、`erp-mall/*`（多个后台管理）、`quality/*`（4）、`erp/payment/*`（9）。
- **不是缺陷的部分（需排除，勿误报）**：登录/验证码/注册、`GlobalExceptionHandler`、`PermissionExceptionHandler`、`HealthMonitorController`、`SseNotificationController`（内部自行校验 token）、`OpenApiController`/`ExternalOrderController`/`GatewayFallbackController`（对外契约）、商城 C 端公开接口。
- **处置**：**不能一次性批量加注解**。补注解前必须先补权限种子（历史教训：代码引用 243 个权限码曾缺失导致非超管全 403，已于本轮前修复，见 §1.3）。建议按模块分批：`erp-finance` → `crm` → `erp-fixed-asset` → `erp-budget`。
- **负责**：后端 + 开发负责人裁决

#### D-03 授权路径不校验 `status`（fail-open，待裁决）
- **证据**：真实鉴权链路 `SysUserMapper.selectPermissionCodesByUserId`（`SysUserMapper.xml:48-59`）只过滤 `p.deleted = 0` 与租户条件，**无 `p.status`** ⇒ 把权限置为「禁用」不会真正收回授权。
- **当前行为无变化**：`sys_permission` 364 行 status 全为 0（0=正常），故今天是零行为差异，属**加固**。
- **注意**：`status` 语义**逐表不同**（`sys_permission`/`sys_role` 是 0=启用，`sys_department` 注释是 0=禁用），**不得**看到 `status=1` 就批量改 0。
- **处置**：补 `AND p.status = 0`，但它在授权路径上，需产品/负责人确认后动。
- **负责**：后端 + 开发负责人裁决

### 1.2 不阻塞（P1，本轮需决策或处理）

| 编号 | 缺陷 | 证据 | 处置建议 |
|---|---|---|---|
| D-04 | 4 组菜单重名 | `check-menu-targets.py`：其他收入（70542 `md/other-income` vs 80116 `finance/other-income-doc/form`）、客户（80200 crm vs 80510 md）、权限配置（80533 role vs 6130704 permission）、销售退货申请（70011 vs 80091，且路径完全相同） | 前三组是**同名不同功能**，改菜单名消歧；第 4 组是真重复挂载，删 70011 |
| D-05 | 13 个功能包「代码在、表建了、点不动」 | 不在 `scanBasePackages` 闭包内：`storage/report/search/knowledge/gateway/mq/recommendation/webhook/agent/assistant/feedback/runner/inventory.repository` | 逐包三选一裁决（要/不要/暂缓），见 `CLEANUP_AUDIT_REPORT.md` §⑤ |
| D-06 | 11 个无调用方但有对外契约的控制器 | `BusinessAccountingController`（注释声明供其它模块调用）、`IntegrationController`（接收 WebHook）、`SignatureController`/`RatingController`（疑公网）、`wms/ErpCallbackController` 等 | **不可轻删**。用 `tools/audit-endpoint-hits.py` 对访问日志观察 2–4 周后再定 |
| D-07 | 孤儿页面调用不存在的后端接口 | 已验证：`finance/expense-apply` → `/finance/expense-apply/page`、`finance/expense-pay`、`finance/expense-reimburse`、`trade/mall-return` → `/erp/mall/admin/return/*`、`erp/pricing/tiers` → `/erp/pricing/tiers/*`，均全库无 mapping | 这 5 页全部落在「第 1/4 类删除」清单内，**删页即解决**（见 §2） |
| D-08 | 权限码冗余 | `sys_permission` 454 个，代码仅用 243 个 ⇒ 211 个代码中未使用 | 记录后移；权限码清理需同步角色关联 |
| D-09 | 接口冗余 | 前端从未调用 1118 个（普通）+ 104 个（内部）；仅 `/api/open/**` 有运行期日志覆盖 | 用 `audit-endpoint-hits.py` 走弃用流程，**本轮不删** |
| D-10 | 契约文件零引用 | `pc-admin/src/utils/priceLevelConfig.ts` 自称「全系统唯一来源」但零引用；各 api 文件内联了价格等级字段 | 改为全站唯一来源（收尾后单独立项） |
| D-11 | 文档与代码不一致 | `docs/.../存储配置开发文档.md:54,371-378` 断言 `/api/storage` 前缀「是活的」，实际 `cn.aiedge.storage` 从未进 `scanBasePackages`，整体 404 | 更正文档 |
| D-12 | 设计文档描述不存在的组件库 | `frontend/docs/ui-components/feedback-components-usage.md` 通篇用不存在的路径 `@/components/@ai-ready/common/components/feedback`，`ARSkeleton` 无文件 | 删除或重写 |

### 1.3 已关闭（本轮前已修复，勿重复排查）

| 缺陷 | 关闭证据（2026-09-19 复测） |
|---|---|
| 90 个权限码缺失导致非超管全 403 | `audit-permission-codes.json`：`missing_in_db: 0`（代码引用 243，库中 454） |
| 匿名跨租户 CRUD（`/api/customer|supplier|crm` 白名单） | `SaTokenConfig.java:31-67,73-109` 已移除这三个前缀，并加注释说明 |
| `status=1` 写反（RoleMapper/PermissionMapper×2/RbacService） | 已改 `= 0`，`/api/v2/user/1` 恢复返回角色 |
| 完全死页面文件（D1） | `classify-orphan-pages.py`：**D1 = 0**（第二轮清理生效） |
| `PasswordResetRunner` 启动重置 admin 密码后门 | 已删除 |

### 1.4 明确后移（P2，本轮只登记不处理）

- D-13 **孤儿数据库表 30 张**（`fee_*`、`erp_marketing_*` 老模型、`erp_kit_*`、`batch_rule` 等）—— 删表属格式决策，须先观察 1–2 个发布周期 + 确认备份可用。
- D-14 **废弃表仍在使用但易误判**：`mkt_presale_order`、`biz_party_transaction` 被 raw SQL 引用 ⇒ **实体删除 ≠ 表可删**。
- D-15 **工作区运行产物**：`backend/*.log` 299MB、`frontend/.._tool-results_*.png`、`backend/cols.tmp`、`*/**/.atcode` 285 个文件。
- D-16 **git 历史瘦身**（`git filter-repo` 改写历史会让所有克隆失效）—— 必须单独立项、全团队知情窗口执行。
- D-17 **TODO/FIXME 37 处**，其中 22 处集中在第三方支付渠道适配器（`TaobaoChannelAdapter` 12、`AlipayChannel` 6、`WechatChannel` 2、`UnionPayChannel` 2），属「未实现完」而非遗留，**保持现状**。
- D-18 **并行会话把 121 个删除混入功能提交** `6acdb6374` —— 建议后续拆分提交习惯，不追溯。

---

## 2. 孤儿页面清单（全量）

**定义**：前端存在页面文件，但 `sys_menu` 无对应记录（component / list_path 双向匹配，含 `/index` 变体兜底）。

**总量**：候选 94 个（D2 仅 URL 可达 24 + D3 仅映射 61 + D4 仅 push 可达 9），剔除误判 7 个 ⇒ **真孤儿 87 个**。

### 2.1 非孤儿（误判，已剔除 7 个，禁止删除）

| 文件 | 为什么不是孤儿 |
|---|---|
| `erp/sale/index.vue` | 菜单 70010「销售订单」`list_path='sales/order/index'`，经 `componentMap['sales/order']`（`dynamicRoutes.ts:322`）解析 —— 它就是列表页 |
| `erp/purchase/index.vue` | 菜单 70060「采购订单」同上（`dynamicRoutes.ts:323`） |
| `admin/monitor/log` | 目录已空，该 componentMap 键是菜单 62204「系统日志」的别名 → `system/log/index.vue`（`dynamicRoutes.ts:252-256`）。**键不可删** |
| `notification/index.vue` | 顶栏铃铛入口（`useNotification.ts` push `/notification`），布局直达，设计上无菜单 |
| `profile/index.vue` | 头像下拉入口（`BasicLayout.vue`），布局直达，设计上无菜单 |
| `dms/order-pool/bid-detail.vue` | 订单池「查看竞标」隐藏详情路由，从列表点入 |
| `erp/column-config/SaleOrderItemColumnConfig.vue` | 销售订单明细列配置，从销售订单页 push 进入的隐藏配置路由 |

### 2.2 第 1 类 · 冗余删除（46 个）

判定依据：**已被某个已挂菜单的新页面取代**（同 API + 同功能，或旧端点根本不存在）。

| # | 删除对象 | 被谁取代（菜单 id） | 关键证据 |
|---|---|---|---|
| 1 | `finance/pre-receipt/index.vue` | advance-receipt（80102） | 页面自证：`router.replace('/finance/advance-receipt/index')` |
| 2 | `finance/receipt/index.vue` | receipt-doc（80101） | 同一 `receiptApi` |
| 3 | `finance/payment/index.vue` | payment-doc（80111） | 同一 `paymentApi` |
| 4 | `finance/pre-payment/index.vue` | advance-payment（80112） | 同一 `prePaymentApi` |
| 5 | `finance/subject/index.vue` | md/accounting-subject（70543） | 同一 `accountSubjectApi` |
| 6 | `finance/expense-apply/index.vue` | expense-doc（80115） | 端点 `/finance/expense-apply/page` 不存在 |
| 7 | `finance/expense-pay/index.vue` | payment-doc（80111） | 端点 `/finance/expense-pay/page` 不存在 |
| 8 | `finance/expense-reimburse/index.vue` | expense-doc（80115） | 端点不存在 |
| 9 | `finance/report/index.vue` | balance-sheet（70222）/balance-report（70230）/profit-report（70231） | 三个 Tab 各有已挂对应页 |
| 10 | `finance/reports/index.vue` | 同上 | 现金流量表端点 `/finance/statement/cash-flow-statement/generate` 不存在 |
| 11 | `finance/receivable/index.vue` | 查应收（80457）+ 往来余额表（80459） | ⚠️ 见 §2.6 存疑 |
| 12 | `finance/payable/index.vue` | 查应付（80458）+ 往来余额表（80459） | ⚠️ 见 §2.6 存疑 |
| 13 | `finance/capital-flow/index.vue` | check-fund（80453） | 同一 `capitalFlowApi`；⚠️ 见 §2.6 |
| 14 | `erp/expense/application/index.vue` | expense-doc（80115） | 旧菜单种子残留页 |
| 15 | `erp/expense/reimbursement/index.vue` | expense-doc（80115） | 同上 |
| 16 | `erp/expense/approval/index.vue` | expense-approval（70242） | 同上 |
| 17 | `erp/expense/payment/index.vue` | payment-doc（80111） | 同上 |
| 18 | `erp/expense/statistics/index.vue` | expense-stats（70244） | 同上；且趋势图用 `Math.random()` 造假数 |
| 19 | `hr/attendance/index.vue` | attendance/list.vue（90002） | 137 行旧版 vs 964 行已挂版 |
| 20 | `hr/leave/index.vue` | leave/list.vue（90003） | 218 vs 1209 行 |
| 21 | `hr/performance/index.vue` | performance/list.vue（90005） | 204 vs 1068 行 |
| 22 | `hr/organization/index.vue` | organization/position-list.vue（90006） | 273 vs 892 行 |
| 23 | `hr/employee/index.vue`（+`form.vue`） | employee/list.vue（90001） | 仅被自身 `form.vue` 反向 push |
| 24 | `hr/salary/index.vue`（+`form.vue`） | salary/list.vue（90004） | 仅被自身 `form.vue` 反向 push |
| 25 | `wms/warehouse/index.vue` | md/warehouse-plan（80520） | 同一 `warehouseApi` 语义 |
| 26 | `wms/location/index.vue` | md/location（70520）/ md/warehouse-plan（80520） | 同一 `locationApi` |
| 27 | `wms/receipt/index.vue`（**保留 form**） | wh/receiving-order（80012） | 同一 `receiptApi` |
| 28 | `wms/inventory/index.vue` | check-stock（80431）/ stock-detail（80434） | ⚠️ `inventoryApi` 本身仍被 `wh/borrow-out` 使用，**接口不能删** |
| 29 | `wms/check/index.vue`（**保留 form**） | wh/inventory-order（60302）/ 盘点单（5003） | `wms/check/form.vue` 仍被 `wh/inventory-order` 跳转 |
| 30 | `erp/mall/banner/index.vue` | mall/shop-decoration（80373） | 同一 `shopBannerApi` |
| 31 | `erp/mall/config/index.vue` | mall/basic-config（80370）/ shop-config（80371） | 同一 `shopConfigApi` |
| 32 | `erp/mall/order/index.vue` | mall/order-process（80350）/ trade/mall-order（90102） | 同一 `mallOrderApi` |
| 33 | `erp/mall/product/index.vue` | mall/product-shelf（80360） | 同一 `mallProductApi` |
| 34 | `erp/mall/user-audit/index.vue` | 买家申请管理（80362）/ 买家账号（80363） | 同一 `shopUserApi` |
| 35 | `erp/shipment/index.vue` | sales/outbound（70021） | 全页走 `/erp/sale/outbound/*` |
| 36 | `order-center/index.vue` | sales/order-center（80050） | 80050 为在用版且被物流发货复用 |
| 37 | `system/department/index.vue` | md/staff-dept（80530） | 同一 `departmentApi` |
| 38 | `trade/mall-return/index.vue` | 退货申请处理（80351） | 后端无 `/erp/mall/admin/return` 控制器 |
| 39 | `charts/index.vue` | analytics/sales-analysis（80413） | 文件内容实为销售分析页 |
| 40 | `erp/partner/index.vue` | md/partner（80513）/ 客户（80200）/ 供应商（80511） | 同一 `partnerApi` |
| 41 | `erp/product/price-batch.vue` | md/product-price（70503） | 该页含「批量修改」Tab |
| 42 | `erp/return/index.vue` | sales/return-doc（70022） | 无独立 api import |
| 43 | `erp/sales-analysis/index.vue` | analytics/sales-analysis（80413） | 重复实现 |
| 44 | `erp/stock/replenishment/index.vue` | purchase/smart-replenish（70053） | 重复实现 |
| 45 | `erp/stock/index.vue` | check-stock（80431）/ stock-detail（80434） | ⚠️ 见 §2.6 存疑 |
| 46 | `supplier/index.vue` | md/supplier（80511） | 旧 `/api/supplier/*`；`supplierApi` 仍被 9 处使用，**接口保留** |

**删除前必做**：确认无模板/配置引用、无外部链接、无 `router.push` 指向；每笔可 `git checkout` 回滚。

### 2.3 第 2 类 · 有价值但未挂菜单 → 补挂（28 个）

| # | 页面 | 后端是否真实 | 建议菜单位置 |
|---|---|---|---|
| 1 | `finance/index.vue` | ✅ `reportApi.getDashboard()` | 财务（60006）首页 |
| 2 | `finance/offset/index.vue` | ✅ `OffsetController` | 财务 → 往来对冲 |
| 3 | `finance/deposit/index.vue` | ✅ `DepositConditionController` | 财务 → 定金押金 |
| 4 | `finance/auxiliary/index.vue` | ✅ `FinanceAuxiliaryController` | 财务设置（61204）辅助核算 |
| 5 | `fixed-asset/index.vue`（容器） | ✅ 8 个 `FixedAsset*Controller` | **财务 → 资产管理（60607，当前 0 子菜单）** |
| 6-9 | `printing/{chain,client,task,template}/index.vue` | ✅ `/api/v2/print/*` 四个控制器 | **系统设置 → 打印管理（61205，当前仅 1 子菜单）** |
| 10 | `budget/index.vue` | ✅ | 财务 → 预算管理（60608）总览 |
| 11 | `budget/template/index.vue` | ✅ `BudgetTemplateController` | 60608 预算模板 |
| 12 | `budget/annual/index.vue` | ✅ `AnnualBudgetController` | 60608 年度预算 ⚠️ 与 80130 重叠，见 §2.6 |
| 13 | `budget/adjustment/index.vue` | ✅ | 60608 预算调整 |
| 14 | `budget/report/index.vue` | ✅ `BudgetReportController` | 60608 预算报表 |
| 15 | `erp/dashboard/index.vue` | ✅ 6 个真实聚合接口 | 工作台（50011）或新建 ERP 仪表盘 |
| 16 | `member/points-history/index.vue` | ✅ `pointsApi` | 会员中心（60801）积分流水 |
| 17 | `quality/certificate/list.vue` | ✅ `qualityCertificateApi` | 质量管理（60311）质量证书 |
| 18 | `supplier/inquiry/index.vue` | ✅ `supplierApi` | 采购查询（60203）供应商询价 |
| 19 | `supplier/performance/index.vue` | ✅ `supplierApi` | 60203 供应商绩效评估 |
| 20 | `wh/inventory-order/index.vue` | ✅ `checkApi` | ⚠️ 与盘点单（5003）重叠，见 §2.6 |
| 21 | `wms/event/index.vue` | ✅ `EventController` | 系统监控（61303）/ 库内作业（60312）事件监控 |
| 22 | `wms/wave/index.vue` | ✅ `WmsPickWave` | 60312 波次管理 |
| 23 | `erp/batch/index.vue` | ✅ `batchApi` | 仓库管理（61103）/ 60312 批次管理 |
| 24 | `erp/product/grade.vue` | ✅ `productGradeApi` | 商品管理（61101）价格等级 |
| 25 | `erp/product/inventory-mode.vue` | ✅ `inventoryModeApi` | 61101 库存管理模式 |
| 26 | `erp/purchase-contract/index.vue` | ✅ `@/api/purchase-contract` | 采购业务（60202）采购合同 |
| 27 | `erp/sales-report/index.vue` | ✅ `salesReportApi` | 采销分析（61002）销售报表 |
| 28 | `erp/serial/index.vue` | ✅ `serialApi` | ⚠️ 与 `erp/batch` 序列号 Tab 重叠，建议合并后只留一个 |

**补挂流程**：确认页面可用 → 定菜单位置/名称/排序/可见角色 → 写 Flyway 迁移 → 补 `componentMap`（若缺）→ 对应角色登录验证。

### 2.4 第 3 类 · 整合进其它页（11 个）

| # | 页面 | 整合方式 |
|---|---|---|
| 1 | `finance/write-off/index.vue` | 核销工作台 → 作为「核销」Tab 并入收款单（80101）/付款单（80111）；⚠️ 见 §2.6 |
| 2-9 | `fixed-asset/{asset,category,depreciation,purchase,transfer,disposal,inventory,report}/index.vue` | 均为 `fixed-asset/index.vue` 容器的 Tab（`VALID_TABS`），随容器一起挂 60607 即可，**不单独挂菜单** |
| 10 | `printing/designer/index.vue` | 打印模板设计器 → 作为「打印模板」的编辑子页，不单独挂菜单 |
| 11 | `member/profile/index.vue` | 会员档案 → 作为会员管理（80300）的详情路由；⚠️ 见 §2.6 |

### 2.5 第 4 类 · 无意义删除（2 个）

| # | 页面 | 证据 |
|---|---|---|
| 1 | `erp/pricing/tiers/index.vue` | 前端调 `/erp/pricing/tiers/*`，全库无 mapping（`PricingController` 只有 configs/resolve/price-memory；层级在 `/api/v1/price-engine/tiers`） |
| 2 | `common/placeholder/index.vue` | 941B，仅 `<a-result title="功能开发中">`，零 api、零业务 |

### 2.6 存疑，需人工裁决（6 项，**裁决前不动**）

| # | 事项 | 两种可能 |
|---|---|---|
| 1 | `finance/receivable` / `finance/payable` | 这两页有「新增应收/应付、坏账标记、账龄分析」管理动作，而 `analytics/check-receivable|payable` 偏只读查询。若业务需手工立账/坏账管理 → 改判**第 2 类挂菜单** |
| 2 | `budget/annual` vs 已挂 `finance/budget-plan`（80130） | 两者同走 `/erp/budget/annual` 后端。需确认以哪套为准，否则重复 |
| 3 | `finance/write-off` | 若业务要保留独立「收付款核销中心」→ 改判第 2 类 |
| 4 | `finance/capital-flow` vs `analytics/check-fund`（80453） | 前者带导出、后者偏分析。是否重复需产品确认 |
| 5 | `wh/inventory-order` vs 盘点单（5003） | 两套盘点实现（`checkApi` vs `stockTakeApi`）。裁定后才能定 `wms/check/index.vue` 的去留 |
| 6 | `erp/stock/index.vue` | 作为 `stock/detail/:id` 组件被 `getRequiredRoutes` 注册，但仓内找不到 push 入口。若外部有历史链接 → 保留为第 3 类详情页 |
| 7 | `member/profile` | 独立会员档案（第 1 类）vs 会员管理详情路由（第 3 类） |

---

## 3. 死代码清单

> 第二轮（2026-09-19）已删除 **330 个文件**（后端 258 / 前端 72），方法论与逐项证据见 `CLEANUP_AUDIT_REPORT.md`。此处只列**尚未处理**的部分。

### 3.1 已清理（摘要）
- 零引用末端类型 161 个；被取代的平行实现 46 个（`Fee*` 整套、`pricing.execution`、装配/拆分簇）；废弃模块级启动入口 24 个；未接线簇 7 个；前端 72 个；安全后门 `PasswordResetRunner` 1 个。
- 门禁：`ComponentScanCoverageTest`（装配）、`tools/check-repo-hygiene.sh` + CI `repo-hygiene` 作业（产物）、`tools/audit-endpoint-hits.py`（运行期命中）。

### 3.2 本轮新发现的死代码

#### DC-01 `DatabaseInitializer` 的旧菜单种子（133 条 `appendMenu`）
- **证据**：`backend/core/base/core-base/src/main/java/cn/aiedge/base/config/DatabaseInitializer.java:1503-1702`，`ensureMenuData()` 用 `MERGE INTO sys_menu (...) KEY(id) VALUES` 种一套 **ID 1000–14004** 的旧菜单（`1000 工作台`、`2000 系统管理`、`3000 采购管理`、`6000 财务管理`、`7000 CRM`、`8000 供应商管理` …）。
- **三点定性**：
  1. 与现网 Flyway 种出的菜单体系（6xxxx/7xxxx/8xxxx，343 条 tenant-admin）**完全并行**，是第二套事实来源；
  2. 执行条件是 `tenant-admin` 菜单数为 0（`DatabaseInitializer.java:1507-1508`），现网永不触发；
  3. `MERGE INTO ... KEY(id)` 是 H2/Oracle 语法，**在 PostgreSQL 上执行会语法报错** ⇒ 即使触发也是失败路径。
- **危害**：全新环境（Flyway 未跑/被禁）会走这条失败路径；且它是 §3 那批旧菜单页面的来源，留着会持续误导后人认为这些页面「有菜单」。
- **处置**：删除 `ensureMenuData()` 的菜单种子段与其调用，保留 `ensureRoleMenuAssociations` 等无争议逻辑。**列为本轮清理项**。

#### DC-02 装配门禁基线内的 13 个包 / 19 个控制器
见 D-05。处置依赖 §1.2 的逐包裁决（a 补扫描 / b 删代码 / c 收进 experimental）。

#### DC-03 其它（沿用已有报告结论，本轮不动）
- 30 张孤儿表、`priceLevelConfig.ts` 契约未采用、`frontend/docs/ui-components/*` 描述的组件库、`_probe_*.js` 之外的探针、遗留运行产物 299MB。

---

## 4. 本轮收尾范围裁定

### 4.1 必须修（阻塞，本轮关闭）

| 编号 | 事项 | 动作 | 验证 |
|---|---|---|---|
| D-01 | 坏菜单 70063 | Flyway 迁移改 `component='purchase/exchange/form'` | 采购换货单新增/编辑可用，console 无 error |
| D-02 | 鉴权覆盖率 | **本轮只做第一批**：`erp-finance` 的收款/付款/凭证/月结/科目补 `@SaCheckPermission`（须先补权限种子） | 非超管账号：有权限可访问、无权限 403 |
| D-03 | `status` fail-open | 提裁决单；批准后补 `AND p.status = 0` | 权限置禁用后确实失效 |

> D-02 不做全量（257 个控制器一次性补注解会大面积锁死功能）。分批推进，本轮完成财务第一批并沉淀「补注解 checklist」。

### 4.2 孤儿页面本轮处理

| 类别 | 数量 | 本轮动作 |
|---|---|---|
| 第 1 类 冗余删除 | 46 | **本轮删除**（删前逐项确认无 push/无配置引用；`wms/*` 保留 `form.vue`；`api/wms/*`、`supplierApi`、`inventoryApi`、`warehouseApi` 等**接口全部保留**） |
| 第 4 类 无意义删除 | 2 | **本轮删除** |
| 第 2 类 补挂菜单 | 28 | **本轮出方案**（位置/名称/排序/角色）→ 负责人批准 → 迁移挂载；其中 `fixed-asset`（60607）与 `printing`（61205）是明确的菜单洞，优先 |
| 第 3 类 整合 | 11 | **随第 2 类一起**：`fixed-asset` 8 个 Tab 随容器挂 60607；`printing/designer` 随模板挂 61205；`finance/write-off` 与 `member/profile` 待 §2.6 裁决 |
| 存疑 | 7 | **本轮只出裁决单，不动代码** |

### 4.3 死代码本轮清理

| 编号 | 事项 | 动作 |
|---|---|---|
| DC-01 | `DatabaseInitializer` 旧菜单种子（133 条） | **本轮删除**（删后启动一次验证 `ensureRoleMenuAssociations` 仍正常） |
| — | 第二轮已完成的 330 个文件 | 已入库/待提交部分按 §3.1，本轮补齐提交与验证记录 |

### 4.4 明确后移（本轮不动）

D-05 的 13 个功能包裁决、D-06 契约控制器、D-08 权限码冗余、D-09 接口弃用、D-10/D-11/D-12 契约与文档、D-13 孤儿表、D-15 运行产物、D-16 历史瘦身、D-17 支付渠道 TODO、D-18 提交拆分。
**后移理由**：均需外部观察窗口（访问日志 2–4 周）或会改写共享状态（历史、数据库表），属「先观察、再标记、后删除」流程，不得与代码删除同批。

---

## 5. 每批清理的通用检查项

- [ ] 引用关系已确认：无动态调用、无 `router.push`、无模板/配置引用
- [ ] 无外部系统调用依赖（公网链接、第三方回调、移动端旧版本）
- [ ] 后端删除已确认不在 `@EntityScan`/`@MapperScan`/`scanBasePackages` 中（或已同步更新基线）
- [ ] 通道产物验证：`mvn -o -DskipTests test-compile` + `npx vite build`（各端）
- [ ] 运行期验证：相关页面 console 无 error（`refactor-verify-runtime` 教训：搬迁后必须运行时验证）
- [ ] 清理动作可回滚（受 git 跟踪）
- [ ] 决策记录与验证结果已填写

## 6. 完成标准

1. D-01/D-02 第一批/D-03 全部关闭并有验证记录。
2. 孤儿页面：第 1、4 类删除完毕；第 2、3 类挂载完毕且对应角色可见可打开；存疑 7 项有书面裁决。
3. 死代码：DC-01 清理完毕；第二轮 330 个文件的删除全部入库且各自有验证记录。
4. 每批清理有验证记录（自动化测试 + 手工回归），无回归。
5. 后移项均带 owner 与观察期，无「停在中间」的模糊态。
