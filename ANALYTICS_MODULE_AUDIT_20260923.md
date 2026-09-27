# 分析模块全面审计报告（2026-09-23）

**范围**
- 前端：`frontend/apps/pc-admin/src/views/analytics/**`（31 个页面 + `shared/` 5 个复用件）+ `src/api/analytics{,-finance,-sales,-supply}.ts` 及这些页面 import 的其它 api 文件
- 后端：**分析模块没有独立模块**，代码分散在 6 处 —— `core-api` 的 `docquery/**` 与 `report/**`、`erp-finance` 的 `finance/analytics/**` 与 `expense/analytics/**`、`erp-sales` 的 `sale/analytics/**` + `sale/controller/SaleAnalysisReportController` + `sale/preorder/analytics/**`、`erp-stock` 的 `stock/analytics/**`、`erp-purchase` 的 `purchase/analytics/**`、`erp-marketing` 的 `marketing/analytics/**`
- 数据库：`devdb`（PostgreSQL）`sys_menu` / `sys_permission` / `sys_role_permission` 及分析域取数涉及的业务表
- 对照基准：`docs/Yh-Spec/手动整理对标开发文档/分析模块/*.md`（**31 篇 + README + _开发指南-金标准**）、`系统菜单设计与管理/*.md`

**维度**：可用性 · 规范性 · 冗余 · 死代码 · 功能接通 · 跨模块关系 · 权限配置
**口径**：结论分三级证据 —— **【实测】**（本轮真机跑过，附命令与输出）· **【读码】**（逐一打开文件核对过行号）· **【清点】**（机械扫描，标注方法，未逐条复核）。所有数字给出可复现来源。

> ⚠️ 本仓反复踩过的坑，本次已规避并写进方法：
> ① **未注册路由被 catch-all 承接，URL 不变但渲染 404 页** —— 本轮「采购分析」正是此症，故判定页面可用性一律用四重检查（URL + 404 文案 + 主内容 + 表格元素）；
> ② **sa-token 单端互踢会伪装成「页面未渲染」** —— 首次 E2E 即因此整节失效，改用模块专用账号 `e2e_analytics` 后复现为 0；
> ③ **判断「无引用」不能只搜类名** —— 死代码结论一律标注搜索方法与置信度，且对子代理结论逐条复核（本轮复核 **7 条**：前端 5 条 + 后端 2 条，**7 条均属实**）。

---

## 〇、摘要

| 级别 | 数量 | 代表问题 |
|---|---|---|
| **P0 致命** | 5 类 | **「采购分析」页 100% 打不开（404）** · **非系统租户看不到分析模块全部 31 页** · **非超管下 15/18 分析接口 403** · 商城用户接口 `mall/admin/user/page` **500**（两个分析页受影响） · **费用申请单分支跨租户 + 软删泄漏**（经营历程/待审批/草稿会看到全部租户的费用申请单） |
| **P1 严重** | 9 类 | 菜单权限派生**完全失效**（`ana:` 命名空间与接口权限码对不上）· 费用申请单「删除/提交」被误发成**驳回** · 零行级数据权限 + 零按钮级权限 · 业绩提成中心「页面配置」是死开关 · 提成链路无生产者（表 0 行）· `ReportAnalyticsController` 整块死岛（含**伪造的趋势数据**）· 旧回款统计端点死重复 · 业务草稿删除按钮未按能力矩阵渲染 · **租户解析失败时"静默回落租户 1"与"静默返回空表"两种相反错误处置并存** |
| **P2 一般** | 12+ 类 | 7 个 `ApiResponse` 响应契约分裂 · 分页参数两套命名 · 多视图两套实现 · 页面间大段重复代码（27 页 F8 逐字相同）· 14 个死 API 方法 + 同名对象 · `.atcode` 污染 175 文件已入库 · 9 张零引用表 · `menu_code` 无唯一约束 · 31 页 component 无迁移落库 · `finance_account_subject` join 无租户（多租户下**金额成倍放大**）· 状态码文案两处不一致 · 「方案汇总提成」视图退化为 1 行 |
| **确认无问题的项** | 8 项 | 31 页骨架 10 项**全过** · 29/31 页逐 Tab 列数与对标**完全一致** · 无孤儿页 · 无 TODO/桩/console.log 残留 · 权限码 100% 在库 · 无断链（契约比对仅 1 处且属他模块） · 综合单据口径深检全过 · **后端 37 个端点 0 个裸端点** |

**整体判断**

分析模块的**页面做工是全仓最好的之一**：31 页统一走金标准路线 A/A′，逐 Tab 独立列配置，E2E 203 项通过、**29/31 页在真机上完全干净（0 失败请求、0 console 错误）**，列数与 ql361 对标**逐 Tab 一一吻合**，无孤儿页、无 TODO 残留、无硬编码假数据。这份底子值得肯定。

但**「能不能用」被五件事拦住了**，且都在页面之外：

1. **一个菜单的 `menu_code` 写重复了**，导致前端路由被同名覆盖 —— **「采购分析」页点进去就是 404**，而 E2E 与人工巡检都只会看到「URL 正常、无报错」。同样的写法在全库还有 2 组，已经让**「库存预警补货」「缺货补货」两个页面也打不开**。
2. **29 条菜单的 `menu_level=3`**，而服务端对「非系统租户」强制只返回 `menu_level=0` —— **换任何一个非 1 号租户登录，分析模块 31 页在导航里全部消失**。开发与验收一直用 `admin`（系统租户+超管，走早退分支不过滤），所以从未暴露。
3. **分析模块的 14 个核心权限码，13 个只授给了 SUPER_ADMIN** —— 非超管角色（含租户管理员）登录后，除「进销存/查库存/库存明细」3 页外**其余全部 403**。

再加上两条：

4. **一个稳定性缺陷**：商城用户分页接口因**包装类型三元拆箱**对 `null` 拆箱抛 NPE，使「营销推广分析 / 推广客户列表」与「商城客户列表」两页的取数直接 **500**。
5. **一处安全隐患**：`DocQueryService` 的「费用申请单」分支是全模块 13 类单据里**唯一不带租户过滤、也不排除软删**的，会让每个租户的经营历程/待审批/草稿看到**全部租户**的费用申请单（代码缺陷确凿；当前该表 0 行，故尚未实际泄漏）。

**后端侧**：37 个端点**零裸端点**（全部带 `@SaCheckLogin` 或 `@SaCheckPermission`），README §4.1 声称的 11 组聚合端点**逐字存在**，手写 SQL 的租户条件在 12 类单据 + 13 段库存 UNION 上**逐条带租户**，注释里也没有 TODO 残留 —— 骨架是扎实的。真正的问题集中在两类：**租户条件的若干处漏拼**（5 处，其中 1 处可放大金额）与**租户解析失败后的错误处置不一致**（一半回落租户 1、一半返回空表），以及 `ReportAnalytics` 包内**编造趋势数据**。

---

## 〇·补、本轮修复记录（2026-09-23，已执行并验证）

> 经确认后按 §九 第一批执行。所有改动均**先读码定位、后改动、再实测**。

| # | 问题（对应前文） | 改动 | 验证结果 |
|---|---|---|---|
| 1 | **「采购分析」等 3 页静默 404**（§2.1） | 新增迁移 `V11.499.0__Fix_Menu_RouteName_Collision_And_Analytics_MenuLevel.sql`：给 80421/80422/70051/70052/70053/70060/70071 **补唯一的 `route_name`**（大驼峰，沿用库中既有 `SystemMenu` 风格）。**刻意不改 `menu_code`** —— 它参与权限码前缀派生，且 80091/70011 这类真双入口本就有意共用 | `node tools/verify-menu-route-collision.cjs` → **9/9 全部可达**（修复前 3 条 404）；E2E 复跑「未改造 **0**」（原为 1） |
| 2 | **非系统租户看不到分析模块**（§2.2） | 同一迁移：分析模块 **29 条** `menu_level` 由 3 改回 0 | `node tools/verify-analytics-fixes.cjs` → tenant 2 菜单返回 analytics 页面 **29 条**（修复前 **0**），全场 menuLevel 分布 `{0:217}` |
| 3 | **费用申请单分支跨租户 + 软删泄漏**（§2.5） | `DocQueryService` 的 EXPENSE 分支 `baseWhere` 由 `"1=1"` 改为 `"tenant_id = ?::text AND deleted = false"`、`hasTenant` 由 `false` 改 `true`（该表 tenant_id 是 **varchar**，故显式 `::text`），并订正了原「JPA 表无 tenant_id/deleted 列」的错误注释 | 经营历程/待审批/草稿三接口 **HTTP 200**（`total=85/3/61`），证明 SQL 可执行且未误伤其余 12 类分支 |
| 4 | **同类问题再次静默发生的风险**（§2.1 制度根因） | `router/dynamicRoutes.ts` 新增 `assertNoDuplicateRouteName`：注册前递归检查路由名冲突并 `console.error` 输出「谁覆盖了谁 + 修法」。**只告警不抛错**（避免一条脏菜单让整站路由注册失败） | 读码确认接入点在 `routes = [layoutRoute]` 之前 |
| 5 | **`mall/admin/user/page` 500**（§2.4） | 由**并行会话**在 `MallAdminServiceImpl:232-234` 以相同方式修复（`Integer.valueOf(ShopUserTenant.ENABLED_YES)`） | ⏳ **待生效**：该修复在 `erp-mall` 模块，需该模块重新构建进 fat jar；本次仅构建了核心 3 模块（当时另有会话正在构建 erp-mall，避免并发 clean 互删 target） |

**未执行的两条（说明理由）**

- **§九 第 2 条「给 `sys_menu.menu_code` 加唯一约束」—— 放弃**。复核发现全库 4 组重复中，`80091`/`70011`（销售退货申请，path 相同、`display_mode=1`）是**有意为之的真双入口**；加唯一约束会破坏该设计。改以「补 `route_name` + 前端路由名冲突告警」覆盖同类风险。
- **其余 `menu_level=3` 的 95 条**（dms 19 / marketing 19 / finance 17 / mall 13 / set 10 / md 8 / crm 6 …）不属本次审计范围，已在迁移注释中登记。**其中 CRM 的同类修复由并行会话在同日以 `V11.500.0` 完成**。

**回归**：`ANALYTICS_ONLY="采购分析,营销推广分析,商城客户列表,待审批单据" node tools/e2e-analytics.cjs` → **通过 68 / 失败 1 / 未改造 0**（唯一失败仍是上述第 5 条待生效项）。

### 第二轮修复记录（2026-09-26，P1 明确缺陷 —— 不含需拍板项）

> 第 5 条（`mall/admin/user/page`）在 `erp-mall` 重新构建后**已验证通过**（`verify-analytics-fixes.cjs` 5/5）。

| # | 问题（对应前文） | 改动 | 验证结果 |
|---|---|---|---|
| 6 | **费用申请单「删除/提交」被误发成驳回**（§3.2） | `shared/docActions.ts:32` 的 EXPENSE 分支加 `kind` 判断：只有 `approve`/`reject` 走 JPA 审批流，`submit`/`remove` 回落通用分支（`POST /erp/expense/application/{id}/submit`、`DELETE /erp/expense/application/{id}` —— 已核实两端点均存在） | E2E 覆盖页 0 error |
| 7 | **业绩提成中心「页面配置」是死开关**（§3.4） | 解构 `isQueryVisible` 并与 Tab 判断相与（＝ 配置 ∧ Tab），面板取消勾选后查询区真实响应 | E2E「业绩提成中心」6 Tab 全过、0 error |
| 8 | **能力矩阵不一致**（§3.8） | `draft` 删除按钮加 `canDo(record.docTypeCode,'remove')` 门控（与相邻的「复制」一致）；`pending-approval` 批量过滤由硬编码 `!== 'STOCK_DAMAGE'` 改为 `canDo(r.docTypeCode, action)`，并修正提示文案 | E2E「业务草稿/待审批单据」0 error |
| 9 | **租户解析失败处置不一致**（§3.10） | **6 处统一**：`AnalyticsSupport`、`CommissionAnalyticsServiceImpl` 去掉「静默回落租户 1」；`PurchaseAnalysisReport`、`PreOrderAnalysisReport`、`InventoryAnalysisReport`、`PromotionFunnelReport`、`SaleAnalysisReport` 五个服务的 `tenantId()` 去掉「把 null 传进 SQL」（原会静默返回 0 行），一律抛 `BusinessException(401,"无法确定当前租户，请重新登录")` | 编译 + 启动通过；三接口 200 |
| 10 | **费用矩阵金额可成倍放大**（§4.4 #1） | `ExpenseAnalyticsServiceImpl` 的公共 FROM 补 `AND s.tenant_id = d.tenant_id AND s.deleted_flag = 0`（按"与单据同租户"关联，比传参更稳） | 「查费用」4 Tab E2E 全过、0 error |
| 11 | **回款统计编号串号**（§4.4 相关） | `codeExpr` 改为相关子查询绑定外层主表别名（`u.tenant_id = <alias>.tenant_id`），三处调用分别传 `"r"`/`"pr"`/`"spo"` | 「回款统计」接口 200 |

**经复核后**不修改**的 3 类（说明理由，避免无意义改动）**：`PreOrderAnalysisReportServiceImpl` 的 `biz_party_category` / `biz_party_contact` join，以及 `InventoryAnalysisReportServiceImpl` 的 13 段明细 join —— 均为**按主键 id 关联**（id 全局唯一），不产生跨租户行；改动的收益为零而回归风险非零。已在 `backend-inventory.md §4.1` 登记为"低危、写法待统一"。

**顺手修复的两个发布阻塞（非本次审计范围，但它们让所有人都起不来）**

1. **`tools/check-stale-classes.py` 的 GBK 编码 bug**：脚本输出含 emoji（✅/❌/⚠️），Windows 控制台默认 GBK 时 `print` 抛 `UnicodeEncodeError` 并以非 0 退出 —— 被 `build-backend.sh` 误判成「产物里有 ECJ 残缺类，拒绝启动」。**实测崩在成功分支**（`print("✅ 未发现 ECJ 残缺类")`），即产物本来是干净的。已加 `sys.stdout/stderr.reconfigure(encoding='utf-8', errors='replace')`。
2. **`V11.513.0__Fix_MasterData_MenuLevel_And_Grant_Permissions.sql`（并行会话新增）的两处 SQL 语法错误**：`IN (...)` 列表**尾随逗号**（第 109、148 行），报 `语法错误 在 ")" 或附近` ⇒ **整个应用无法启动**。已删逗号；并清理了该迁移首次失败时已提交的 **128 行残留**（`DELETE FROM sys_role_permission WHERE id BETWEEN 9800000 AND 9899999` —— 即该迁移注释里自带的回滚语句），否则重跑必然主键冲突。

**回归**：`ANALYTICS_ONLY="采购分析,业绩提成中心,业务草稿,待审批单据,查费用,回款统计" node tools/e2e-analytics.cjs` → **通过 98 / 失败 0 / 未改造 0**。

### 第三轮修复记录（2026-09-27，分析码补授给「租户超管」）

> 口径确认：本系统的「租户超管」= **`SYSTEM_ADMIN`（系统管理员）**。
> 佐证：`tools/e2e-hr-user.sql` 里 `e2e_hr_ta` 的昵称即「**租户管理员**验收账号」，其角色正是 SYSTEM_ADMIN。
> **不含 `DEPT_ADMIN`**（部门管理员权限面应更窄，是否授分析码属单独决策）。

**迁移**：`V11.515.0__Grant_Analytics_Permissions_To_System_Admin.sql`（`sql` 见 §8.4）

**授予 14 个码**（此前**仅 SUPER_ADMIN 持有**）：

| 类别 | 权限码 |
|---|---|
| 分析模块自有（9） | `doc:docquery:list` · `erp:expense:statistics:list` · `finance:analytics-collection-stats:list` · `finance:analytics-collection-stats:view` · `finance:analytics-invoice-stats:list` · `finance:analytics-partner-balance:reconcile` · `finance:partner-balance:view` · `purchase:analytics:list` · `sale:pre-order-analysis:list` |
| 销售分析组（2） | `sale:analysis:list` · `sale:analysis-promotion-funnel:list` |
| **分析页跨域依赖（3）** | `finance:collection-stats:view`（回款统计） · `finance:report:view`（查应收） · `mall:trade-analysis:view`（交易分析） |

> 为什么要带上最后 3 个非 analytics 命名空间的码：分析页的取数是**跨域调用**，权限码按"被调接口所属域"命名 —— 只授 analytics 自身的码，这几个页面仍会 403。这 3 个都是租户内的只读/分析能力，给租户超管不越界。
> `tenant_id` 取**角色自身的 tenant_id**（`r.tenant_id`），不硬编码 1（本仓曾有 `sys_role_permission` 租户标记不一致的 16 行实证）。

**验证**（`node tools/verify-analytics-authz.cjs`，账号 `e2e_hr_ta` = tenant 1 + SYSTEM_ADMIN）：

| | 授权前 | 授权后 |
|---|---|---|
| 18 个代表接口中被拒 | **15** | **0** ✅ |

**附带修正**：该脚本的探针原写 `/erp/marketing/commission/analytics/overview` —— 该端点**不存在**（属探针自身错误，会误报 404），已改为前端真实调用的 `/erp/marketing/commission/analytics/rider-matrix/page`。

---



## 一、已确认可用的部分（正面结论）

这些是本次检查中**验证通过**的项，供收尾阶段放心：

| # | 结论 | 证据 |
|---|---|---|
| 1 | **31 页骨架 10 项全过**：ErrorBoundary → PageContainer(`full-height` 静态) → CategoryListLayout → BillDetailTable → StandardPagination(`variant="classic"`)、无 `:max-height` | E2E §3 `31/31`；独立复算 `_skeleton.cjs` 结果逐页一致 |
| 2 | **29/31 页真机完全干净** | E2E 三次合并：0 失败请求、0 pageerror、0 console.error（例外见 §2.1、§2.4） |
| 3 | **逐 Tab 列数与 ql361 对标完全一致**（含 8 Tab 的销售分析、6 Tab 的业绩提成中心/营销推广分析） | E2E §5 全部 `✔`；两处差额已按 `override` 显式登记来源 |
| 4 | **31 个页面目录 ↔ 31 条菜单 path ↔ 31 条 component，三方一一对应，无孤儿页** | 目录清单 + `dynamicRoutes.ts:675-736` componentMap + `sys_menu` 三源交叉 |
| 5 | **无 TODO/FIXME 残留、无 `console.log`/`debugger`、无硬编码假数据集** | 全模块 grep；`rows.value = []` 仅出现在 catch 分支的错误重置 |
| 6 | **代码引用的分析权限码 100% 在库且启用**（`sys_permission.status=0` 语义为启用） | 18 个代表端点逐一比对，无「代码引用但库里没有」 |
| 7 | **前后端契约基本一致**：分析页面 import 的 9 个 api 文件、120 个调用点中，**断链仅 1 处**（且属营销模块的 `POST /erp/marketing/coupon`，与页面实际调用无关）；双 `/api` 前缀 **0** 处 | `tools/audit-analytics-contract.py` 输出 `contract-compare.md` |
| 8 | **综合单据（经营历程/待审批/草稿）口径深检全过**：合计行按全量 SUM（非当前页求和）、排序真实在后端生效、红冲开关只增不减、日期区间过滤、账期比较符、`docId` 按字符串返回（不丢精度）、13 类待审批计数、5 个行级动作端点可达 | E2E §6 全部 `✔` |
| 9 | **页面配置 key 与列配置 key 全域无冲突**（31 页逐页比对） | `_scan.cjs` 抽全部 key 字面量并解引用后比对 |

---

## 二、P0 致命问题

### 2.1 「采购分析」页 100% 打不开 —— menu_code 重复致前端路由互相覆盖【实测】

**现象**（`tools/diag-analytics-purchase-analysis.cjs`，复现 2 次，与后端状态无关）：

```
url          : http://localhost:5656/analytics/purchase-analysis   ← URL 正常
hasTableArea : false    hasSsGrid : false
bodyText     : "… 分析 资料 交易 人力资源 设置 … 工作台 采购分析 404 抱歉，您访问的页面不存在 返回首页"
logs         : 无 4xx/5xx、无 pageerror、无 console.error          ← 静默失败
```

⇒ 渲染的是 **catch-all 404 页**。`tools/e2e-analytics.cjs` 因此两次把它判为「未改造（页面未渲染 BillDetailTable）」。

**根因**（读码定位到行）：

`frontend/apps/pc-admin/src/router/dynamicRoutes.ts:966`
```js
name: menu.routeName || menu.menuCode,     // 路由名 = route_name，为空则取 menu_code
```
80421/80422 的 `route_name` **均为 null**，而 `menu_code` **都是 `purchase:analytics`**：

| id | menu_name | menu_code | route_name | path |
|---|---|---|---|---|
| 80421 | 采购分析 | `purchase:analytics` | null | `analytics/purchase-analysis` |
| 80422 | 采购/准备 | `purchase:analytics` | null | `analytics/purchase-prep` |

两条菜单生成**同名路由**，Vue Router 对同名路由是「后者覆盖前者」（`dynamicRoutes.ts:1216-1222` 逐条 `router.addRoute`）⇒ 80421 的路由被挤掉。

**已排除的其它可能**：菜单接口**正常返回**了 80421（超管 `GET /menu/user/mega/tenant-admin` → 416 节点、含 `analytics/purchase-analysis`）；`componentMap:697` 也有该条目；`component` 指向的文件存在 ⇒ **问题只在路由名冲突这一处**。

**影响面：不止分析模块**（全库 4 组 menu_code 重复，其中 3 组 path 不同）

`node tools/verify-menu-route-collision.cjs`（脚本已落位 `tools/`）：

| 菜单ID | 菜单名 | path | 实测 | 说明 |
|---|---|---|---|---|
| **80421** | **采购分析** | `/analytics/purchase-analysis` | ❌ **404** | **本模块** |
| 80422 | 采购/准备 | `/analytics/purchase-prep` | ✅ 正常 | 覆盖者 |
| 70051 | 库存预警补货 | `/purchase/alert-replenish` | ❌ **404** | 跨模块 |
| 70052 | 缺货补货 | `/purchase/shortage-replenish` | ❌ **404** | 跨模块 |
| 70053 | 智能补货 | `/purchase/smart-replenish` | ✅ 正常 | 覆盖者 |
| 70071 | 采购明细查询 | `/purchase/detail-query` | ✅ 正常 | 幸存者 |
| 70060 | 采购订单 | `/purchase/order/form` | ⚠️ 表面正常 | 路由同样被覆盖，但 URL 恰好被 `getRequiredRoutes()` 的参数路由 `purchase/order/:id`（`:1304`）兜住、渲染同一个 `views/erp/purchase/form.vue` ⇒ **看不出异常**（疑似同源问题） |
| 80091 / 70011 | 销售退货申请 | `/sales/return-apply/form` | ✅ 正常 | 两条 **path 相同**、`display_mode=1` ⇒ 真双入口，覆盖无害 |

**制度根因**：`sys_menu.menu_code` **没有唯一约束**（实测可插入重复值）；`系统菜单设计与管理/41条双入口菜单的正确配置.md` 只覆盖 `display_mode=1` 这一种形态，**「多条不同 path 的菜单共用 menu_code」没有任何规范禁止**。

### 2.2 非系统租户看不到分析模块全部 31 页（menu_level=3）【实测】

**代码**（`SysMenuServiceImpl.getUserMegaMenus`，core-base）：
```java
// :276-279
if (!isSystemTenant && !isSuperAdmin) {
    wrapper.eq(SysMenu::getMenuLevel, 0);      // 非系统租户只返回 menu_level=0
}
```
**前端**（`router/dynamicRoutes.ts:1072,1131`）：
```js
const tenantId = userStore.tenantId || 1
const res = await request.get(`/menu/user/mega/${CLIENT_TYPE}`, { userId, tenantId })
```
**接口**（`SysMenuController.java:201`）：`@RequestParam(defaultValue = "1") Long tenantId`

**实测**（`tools/verify-analytics-authz.cjs`，账号 `e2e_hr_t2`：tenant 2、非超管）：

| 请求 | 返回节点 | 其中 analytics 菜单 |
|---|---|---|
| 不带 tenantId（服务端默认 1） | 286 | 29 |
| **`?tenantId=2`（前端真实行为）** | 188 | **0** |
| `?tenantId=1` | 286 | 29 |

`?tenantId=2` 返回的菜单**全部 menuLevel=0**（分布 `{0:188}`），分析目录下实际子节点：

```
[61001] 综合单据 子节点 0 个      [61002] 采销分析 子节点 1 个 → 81011:销售报表（属别的模块）
[61003] 仓配分析 0 个             [61004] 提成分析 0 个
[61005] 财务分析 0 个             [61006] 营销分析 0 个
```

⇒ **非系统租户登录后，分析模块在导航里整体消失**（29 条叶子为 `menu_level=3`；仅有的 2 条 `menu_level=0`——80421/80422——另因权限码过滤被挡，见 §3.1）。
> 全库 `menu_level` 分布：`0→252 / 1→43 / 3→124 / 4→3`（**没有 2**，却有一个前端菜单管理页不支持的 **3**；`mega-menu-redesign.md` 的字段注释也只承认 `0=租户级 / 1=系统级`）。
> 开发与验收一直用 `admin`（**系统租户 + 超管**，走 `:259` 早退分支不受 `menu_level` 限制）⇒ 从未暴露。**与财务模块审计（`FINANCE_MODULE_AUDIT_20260923.md` §3.1）完全同构**。

### 2.3 非超管下 15/18 分析接口 403【实测】—— ✅ 已修（2026-09-27，见「第三轮修复记录」）

`tools/verify-analytics-authz.cjs`，账号 `e2e_hr_ta`（tenant 1，SYSTEM_ADMIN，持 222 个权限）：

```
✘ doc:docquery:list                      经营历程 / 待审批单据      403
✘ sale:analysis:list                     销售分析 / 客户活跃分析     403
✘ sale:analysis-promotion-funnel:list    推广分析                  403
✘ sale:pre-order-analysis:list           预订货查询                 403
✘ purchase:analytics:list                采购分析                  403
✔ stock:analytics:list                   进销存分析 / 查库存 / 库存明细  200
✘ marketing:commission:list              业务员提成                 403
✘ finance:collection-stats:view          回款统计                  403
✘ invoice:view                           发票统计                  403
✘ finance:partner-balance:view           往来余额表                 403
✘ finance:report:view                    查应收                    403
✘ erp:expense:statistics:list            查费用                    403
✘ mall:trade-analysis:view               交易分析                  403
→ 被拒 15/18
```
超管对照（`e2e_hr`）：17/18 返回 200。

**授权现状**（`sys_role_permission` 实测）——14 个核心码中 **13 个只有 SUPER_ADMIN 持有**：

| 权限码 | SUPER_ADMIN | SYSTEM_ADMIN | DEPT_ADMIN |
|---|:--:|:--:|:--:|
| `stock:analytics:list` | ✅ | ✅ | ✅ |
| 其余 13 个 | ✅ | ❌ | ❌ |

角色权限总数：`SUPER_ADMIN 1836 / SYSTEM_ADMIN 222 / DEPT_ADMIN 99 / E2E_T2_ADMIN 4`。

### 2.4 商城用户接口 500 —— 包装类型三元拆箱 NPE（两个分析页受影响）【实测 + 读码】

**现象**（E2E 第三次运行，唯一失败项）：
```
[HTTP 500] GET /api/erp/mall/admin/user/page?pageNum=1&pageSize=20
```
影响页面：**「营销推广分析 / 推广客户列表」** 与 **「商城客户列表」**（二者都调该端点）。

**后端异常**（`logs/analytics-audit-backend.log:32661`）：
```
java.lang.NullPointerException: Cannot invoke "java.lang.Integer.intValue()" because "status" is null
	at cn.aiedge.erp.b2b.service.impl.MallAdminServiceImpl.pageUsers(MallAdminServiceImpl.java:225)
	at cn.aiedge.erp.b2b.controller.MallAdminController.pageUsers(MallAdminController.java:70)
```

**根因**（读码确认，教科书式拆箱）：
```java
// ShopUserTenant.java:53
public static final int ENABLED_YES = 1;        // ← 基本类型 int

// MallAdminServiceImpl.java:225
Integer enabledFilter = Boolean.FALSE.equals(showDisabled) ? ShopUserTenant.ENABLED_YES : status;
//                                                                  ↑ int              ↑ Integer
```
三元表达式两个分支类型不同（`int` / `Integer`）⇒ Java 按**二进制数值提升**把整个表达式定为 `int` ⇒ `status` 被**拆箱**；而前端不传 `status` 时它是 `null` ⇒ **NPE**（异常消息里的变量名正是 `status`）。
> 与本仓财务模块审计发现的 `Integer != Integer` 引用比较属**同一类包装类型陷阱**。
> **触发条件**：`showDisabled` 非 `Boolean.FALSE`（即 true 或 null）且 `status == null` —— 正是前端的默认调用姿势。

### 2.5 费用申请单分支**跨租户 + 软删泄漏** —— 代码注释与库结构不符【读码 + 实测 · 已复核】

**代码**（`core-api/.../docquery/service/DocQueryService.java:194-212`，13 类单据里唯一的例外）：
```java
// JPA 表：无 tenant_id/deleted/create_by 列          ← 注释断言
new DocBranch("EXPENSE", "费用申请单",
    "SELECT … FROM expense_application",
    "1=1", false,                                     // ← baseWhere 恒真、hasTenant=false
    …)
```
其余 **12 个分支**的 `baseWhere` 都是 `"tenant_id = ? AND deleted = 0"`（`:84,95,103,…,193`）。

**实测库结构**（`information_schema`）：
```
table_name=expense_application | column=deleted   | type=boolean
table_name=expense_application | column=tenant_id | type=character varying
```
⇒ **该表两列都存在**，注释断言不成立。

**后果**：`EXPENSE` 分支既不按租户过滤、也不排除软删 ⇒ **每个租户的「经营历程 / 待审批单据 / 业务草稿」都会列出全部租户的费用申请单**，且已删除的单据也会出现。修复时注意该表 `tenant_id` 是 **varchar**（其余表为 bigint），条件需写 `tenant_id = ?::text`。
> ⚠️ **当前影响被数据现状掩盖**：`expense_application` **0 行**（本轮实测），故暂无实际泄漏。但代码缺陷确凿，一旦该旧审批链启用即为跨租户数据泄漏。

---

## 三、P1 严重问题

### 3.1 菜单权限派生对分析模块**完全失效**【读码 + 实测】

`getUserMegaMenus` 的权限过滤（`:339-350`）是 **fail-open**：
```java
if (menuCode != null && !menuCode.isEmpty()
        && knownMenuCodes.contains(menuCode)      // 权限库里存在以该 menu_code 为前缀的权限码
        && !heldMenuCodes.contains(menuCode)) {   // 且用户不持有
    blockedByPermission++; continue;              // 才隐藏
}
```
而**权限码库里没有任何 `ana:` 前缀的权限码**（`SELECT * FROM sys_permission WHERE permission_code LIKE 'ana:%'` → **0 行**）。分析模块 31 条菜单的 `menu_code` 与真实接口权限码**分属两套命名空间**：

| 菜单 menu_code | 实际生效的接口权限码 |
|---|---|
| `ana:pending-approval` / `ana:draft` / `ana:business-history` | `doc:docquery:list` |
| `ana:sales-*` / `ana:customer-active` | `sale:analysis:list` |
| `ana:check-stock` / `ana:check-batch` / `ana:inventory-analysis` / `ana:stock-detail` | `stock:analytics:list` |
| `ana:business-analysis` / `ana:check-fund` / `ana:check-expense` / `ana:invoice-stats` / `ana:check-receivable` / `ana:check-payable` / `ana:ar-balance-sheet` | `finance:collection-stats:view` / `invoice:view` / `finance:report:view` / `erp:expense:statistics:list` / `finance:partner-balance:view` … |
| `ana:staff-commission` / `ana:commission-center` | `marketing:commission:list` / `marketing:commission-analytics:list` |
| `ana:trade-analysis` / `ana:mall-customer-list` | `mall:trade-analysis:view` … |
| **`purchase:analytics`**（80421/80422） | `purchase:analytics:list` ← **唯独这两条能前缀命中** |

**两个后果**：
1. 29 条 `ana:` 菜单**永远命中不上** `knownMenuCodes` ⇒ 永远 fail-open 保持可见 ⇒ **菜单可见性与接口权限完全脱节**（菜单看得见、点进去 403）。
2. 唯独 80421/80422 的 `menu_code` 是 `purchase:analytics`，能命中 `purchase:analytics:list` ⇒ **这两条反而受权限控制**，实测在 tenant 2 下被隐藏。**同一模块内两套语义**。

### 3.2 费用申请单的「删除 / 提交」被误发成**驳回**【读码 · 已复核属实】

`views/analytics/shared/docActions.ts:26-32`：
```ts
if (record.docTypeCode === 'EXPENSE') {
  // 费用申请单走 JPA 审批流
  await docActionApi.expenseApproval(
    String(record.docId),
    kind === 'approve' ? 'APPROVE' : 'REJECT',   // ← 只二分 approve / 其余，未判 kind
    kind === 'approve' ? payload?.note : payload?.reason
  )
} else if (kind === 'remove') { ... }
```
该分支**先于** `kind === 'remove' / 'submit' / 'copy' / 'cancel'` 判断，且只按 `kind === 'approve'` 二分。而 `docTypes.ts:77` 给 EXPENSE 登记的能力是 `acts('submit','approve','reject','remove')` ⇒ EXPENSE 会进入此分支的动作有 4 种，其中 **`submit` 与 `remove` 都会被发成 `REJECT`**。

**真实触发路径**：`draft/index.vue:379 → 365`（`pickCapable('remove')` 用 `canDo` 过滤，EXPENSE 通过）⇒ 用户点「删除草稿」⇒ 实际调用 `/erp/expense/approval/process` 且 `action=REJECT`；`draft/index.vue:339 → 347`（`pickCapable('submit')`）同理。

> **影响分级说明**：代码逻辑错误**确凿**；实际危害受限于该审批流的数据现状 —— 它走的是 **旧 JPA 费用审批链**（`expense_application` 等 6 张表**全 0 行**，见财务审计 §4.4），故当前更可能表现为「操作失败」而非真的改了状态。但一旦该链路启用，即为**越权改单据状态**。

### 3.3 零行级数据权限 + 零按钮级权限【读码 + 实测】

| 项 | 现状 | 证据 |
|---|---|---|
| **行级数据权限** | **全仓未生效**。注解式 `@DataScope` 三件套已于 2026-09-20 删除（`MyBatisPlusConfig:315-319` 注释自述「全仓零业务引用，整条链从未生效过」）；替代的 `@DataPermission` **全仓零业务使用**（仅拦截器自身与测试提及）；`sys_data_scope` / `sys_field_permission` **0 行** | grep + SQL |
| **分析域使用情况** | 6 个 analytics 包 + `docquery` **无一使用** `@DataPermission`/`@DataScope` | grep 零命中 |
| **按钮级权限** | `views/analytics/**` 内 `v-permission` / `hasPermission` **0 次** | grep 零命中 |

**含义**：分析模块是与「数据权限」最相关的域（销售业绩、业务员提成、查应收/应付天然应按职员/部门隔离），但当前**任何持权限的用户都能看到全部职员、全部客户的汇总数据**；页面上的行级动作按钮也无按钮级权限控制。这与用友/金蝶/管家婆等同类产品的通行做法（业务员仅见本人业绩、部门主管仅见本部门）存在**明确差距**。

### 3.4 业绩提成中心「页面配置 → 查询条件显隐」是死开关【读码 · 已复核】

`commission-center/index.vue:527-534` 的 `queryVisible(key)` **只按 `activeTab` 判断、完全不读页面配置**；composable 解构时（`:517-520`）**没有取 `isQueryVisible`**。模板 5 处 `v-if="queryVisible(...)"`（`:81/92/103/114/161`）因此与配置无关。

**交叉验证**：全模块 21 页接了 `PageConfigPanel`，其中 **20 页**都用了 `isQueryVisible`，**唯一缺失的正是 `commission-center`**（`grep -rl isQueryVisible */index.vue`）。⇒ 用户在该页取消勾选任一查询项，查询区毫无变化。

### 3.5 提成链路缺生产者 —— 提成相关页面恒空【读码 + SQL】

- `erp_commission_record` **0 行**、`erp_commission_rule` **0 行**、`erp_commission_settlement_config` **0 行**；
- 全仓对 `erp_commission_record` 的引用只有**读**（`CommissionAnalyticsServiceImpl` 6 处 SELECT）与**一处 UPDATE**（`:911` 结算改状态），**没有任何 INSERT** ⇒ **全库无写入者**；
- 「业务员提成」「业绩提成中心」两页因此恒为空（README §4.2 亦如实登记）。

### 3.6 `ReportAnalyticsController` 是一整块死岛【清点 · 高置信】

`core-api` 的 `report/controller/ReportAnalyticsController.java`（`/api/report/analytics/**`，**9 个端点**）+ 其 Service/Impl（577 行）：
- **6 个前端 App（pc-admin / mobile-admin / mobile-mall / driver-delivery / pda-warehouse / print-client）全部零引用**（`grep -rn "report/analytics" frontend/apps/*/src/` → 0）；
- 后端仅自身 Service/Impl + 单元测试引用；
- 数据库无对应菜单（`sys_menu` 中 `path/component/menu_code` 含 `report` 的 6 条**均不指向它**）；
- 权限码 `system:report-analytics:view` 只授给 SUPER_ADMIN。
⇒ 该功能**没有任何 HTTP 或内部入口**。与财务模块的「发票匹配死岛」同类。

### 3.7 旧「回款统计」端点与新端点重复且前端零有效调用【清点】

| | 旧 | 新 |
|---|---|---|
| 类 | `erp-finance/.../finance/controller/CollectionStatsController` | `.../finance/analytics/controller/CollectionStatsAnalyticsController` |
| 路径 | `GET /api/erp/finance/collection-stats` | `GET /api/erp/finance/analytics/collection-stats/page`、`/detail` |
| 前端消费 | **0 个页面**（仅两处死封装：`api/analytics.ts:498` `financeAnalyticsApi.collectionStats`、`api/finance/index.ts:928` `collectionStatsApi.getStats`，二者均无调用方） | 「回款统计」页实际使用（`collection-stats/index.vue:189`） |

⇒ 旧 Controller 为**死端点**，且与新 Controller 功能重叠。
> ⚠️ 附带：`tools/e2e-analytics.cjs` 第 4 节的「回款统计」探针**探的是旧端点**（`/erp/finance/collection-stats`），与页面真实取数不符 ⇒ 属测试覆盖错位。

### 3.8 业务草稿行级「删除」按钮未按能力矩阵渲染【读码 · 已复核】

`draft/index.vue:142` 的「删除」**无条件渲染**，而紧邻的「复制」（`:143`）写了 `v-if="canDo(record.docTypeCode,'copy')"`。
按 `docTypes.ts:51-67`，`SALE_OUTBOUND`、`PURCHASE_INBOUND`、`PURCHASE_RETURN`、`RECEIPT`、`PAYMENT` **都没有 `remove`** ⇒ 这些类型的草稿行点「删除」只会弹一句「不支持该操作」（`docActions.ts:22`），属「点了没反应」类体验缺陷。

**同源**：`pending-approval/index.vue:386` 的批量能力过滤是**硬编码** `r.docTypeCode !== 'STOCK_DAMAGE'`，未用 `canDo()` ⇒ `SALE_PRE_ORDER`（能力为 `submit/approve/remove`，**无 reject**）在「批量驳回」时不会被过滤，被计入「N 张失败」而非「跳过」。硬编码名单后续必然与能力矩阵漂移。

### 3.9 查库存：切 Tab 后查询项显隐 / 按钮启停沿用上一个 Tab 的配置【读码 · 置信度高】

`check-stock/index.vue` 只建了**一份** `useAnalyticsPageConfig`（`:628-636`，`storageKey: 'analytics-check-stock-page-config'`），而模板按 Tab 传**不同的 key**（`:165` 的 `analytics-check-stock-page-config-${activeTab}`）；`onTabChange`（`:595-597`）只改 `activeTab`、**不重新加载配置** ⇒ 切到「按属性 / 库存分布」后，查询项显隐与按钮启停仍是「当前库存」Tab 的状态，直到手动重开一次弹窗才纠正。
> **对照（做对了的 4 页）**：`purchase-prep:791-798`（`cfgByTab`）、`sales-debt:509/514/519`、`sales-fulfillment:524/529`、`sales-performance:572/577` —— 每 Tab 一份 composable 实例，因此免疫。

### 3.10 租户解析失败时，**两种相反的错误处置并存**【读码 · 已复核】

本仓已确立口径（`DocQueryController.java:73-87` 的注释把它定性为要根除的高危写法）：
> 「Session 里取不到 tenantId ⇒ 回退默认租户 1 … 等于**把租户 1 的数据展示给另一个租户的用户**；解析不出时应当明确报『请重新登录』。」

分析模块的两侧做法**互相矛盾**：

| 写法 | 位置 | 后果 |
|---|---|---|
| **静默回落租户 1** | `finance/analytics/support/AnalyticsSupport.java:25-29`（注释还写「与既有分析模块实现一致」）；`CommissionAnalyticsServiceImpl.java:76-79`（`return t == null ? 1L : t`） | 覆盖 **9 个端点**：回款统计 page/detail、发票统计、往来余额表 page/detail/reconcile/reconcile-history、查费用 matrix/detail/partner、业绩提成中心全部 7 个 ⇒ session 缺租户时**静默显示系统租户的数据** |
| **静默返回空表** | `PurchaseAnalysisReportServiceImpl:120-122`、`PreOrderAnalysisReportServiceImpl:54-56`、`InventoryAnalysisReportServiceImpl:70-72`、`PromotionFunnelReportServiceImpl:54-56`、`SaleAnalysisReportServiceImpl:342-344` | `tenantId()` 把 null 直接传进 `tenant_id = ?` ⇒ **0 行**（"数据凭空少了"且无任何报错） |

⇒ 同一模块内**既有"看到别人的数据"、也有"看不到自己的数据"**，且都没有按本仓口径报「请重新登录」。

### 3.11 `ReportAnalytics` 的**趋势数据是编造的**【读码 · 已复核】

`ReportAnalyticsServiceImpl.java:444-478` 方法注释自认：`// 模拟按粒度分组（实际应从数据中解析日期）`（`:449`）；实现把报表行**按顺序**塞进连续日期格子：
```java
double value = getDoubleValue(rows.get(dataIndex % rows.size()).get(field));   // :469
```
⇒ ① 行的真实日期维度被忽略；② 行数少于格子数时 `% rows.size()` **循环复用** ⇒ `trend` / `slope` / `r2` / `forecast` 全部建立在编造序列上。
**同处另有**：`rankings` 恒 `Collections.emptyList()`（`:378`）、单字段异常被 `catch` 吞成缺字段（`:355-357`）。

> 该控制器前端**零引用**（§3.6），故当前不误导用户；但一旦接线，输出的是**看似真实的假趋势**。

### 3.12 已登记的 disabled 占位（**不是**静默假功能）

13 处控件渲染为 `disabled` + `title="…后端端点无该条件，待补"`，属"如实留空"：`business-analysis`（部门/经手人/包含手工凭证）、`check-fund`（日期类型/单据类型/账户/往来单位/经手人/显示红冲）、`check-payable`（日期类型/部门/经手人）、`check-receivable`（日期类型/结款方式/部门/经手人/包含未记账）。
**恒空但有设计说明**：「商品批次跟踪」Tab（后端无「单据×批次」流水端点，`check-batch:463`）；营销推广分析 6 个 Tab 的漏斗指标列（`EMPTY = () => '-'`，`:346`）。

---

## 四、P2 一般问题

### 4.1 规范性

| # | 问题 | 证据 |
|---|---|---|
| 1 | **响应契约分裂：全仓 7 个 `ApiResponse` 类**，其中 2 个 `code` 为 `String`（`erp-fixed-asset/.../dto/ApiResponse.java`、`erp-mall/.../b2b/dto/ApiResponse.java`），其余为 `int`；标准 `Result` 又是另一套结构 | `find . -name ApiResponse.java` + 逐个读字段 |
| 2 | **前端为规避契约差异绕开统一拦截器**：`api/analytics.ts:751-775` 的 `expenseAnalyticsApi.statisticsPage` 用**原生 axios + 手动 token**，注释理由是「后端 code 为字符串 "200"」——**该理由已过时**：实测 `GET /erp/expense/statistics/page` 返回 `code` 为**数字 200**（`{"success":true,"code":200,...}`）。绕开拦截器的代价是**丢失 token 自动刷新、401 统一处理、错误提示**；`api/marketing.ts:25` 同模式 | 实测 + 读码 |
| 3 | **分页参数两套命名**：分析页混用 `page/size`（`/docquery/*`、`/erp/sale/analysis/*`、`/erp/purchase/analytics`、`/erp/stock/analytics`）与 `pageNum/pageSize`（`/erp/stock/page`、`/erp/finance/auxiliary/balance/page`、`/erp/finance/partner-balance/page`、`/erp/mall/admin/user/page`） | `audit-analytics-contract.py` + 前端 api 文件 |
| 4 | **同一模块两种多视图实现**：销售分析/采购准备用**独立路径**（`sales-performance/`、`sales-analysis/`…），采购分析/进销存/预订货/查库存等用 **`?tab=` 参数**；两种风格并存 | `analytics-supply.ts` vs `analytics-sales.ts` |
| 5 | **`api/analytics-finance.ts:159` 与 `api/analytics.ts:757` 同名导出 `expenseAnalyticsApi`**，语义不同（前者 `matrix/detail/partner` 在用，后者仅 `statisticsPage` 且零引用） | 读码 |
| 6 | **31 页的 `component` 映射没有任何 Flyway 迁移落库**：全仓 `.sql` 搜 `views/analytics` 仅一条注释命中；唯一种子 `V6.22.0__Fill_Mega_Menu_All_Columns.sql` 写的仍是占位组件 `views/common/placeholder/index.vue`（**该占位文件实际不存在**）。库中映射显然为手工 UPDATE ⇒ **按迁移重建库，31 页会全部指向不存在的组件** | 子代理取证 + 复核命令见 `frontend-inventory.md §2.4` |
| 7 | **`.atcode/` 工具产物污染已入版本库**：`git ls-files | grep -c '\.atcode/'` → **175 个文件**（分布在 `views/sales`、`views/purchase`、`views/analytics`、`src/router`、`src/utils` 等 20+ 目录），内容为 IDE 工作流 JSON；`.gitignore:114` 虽已写 `**/.atcode/`，但**已跟踪的文件不会被忽略规则移除** | git 实测 |
| 8 | **`sys_menu.menu_code` 无唯一约束**（§2.1 的制度根因） | 实测可插入重复值 |
| 9 | **3 个 Tab 多接了「页面配置」弹窗**（引入非对标功能）：查库存「库存分布」、查费用「按明细」、业绩提成中心「业绩概览/业绩明细」—— ql361 对标 `pageConfig.found=false`，本系统却对全部 Tab 渲染面板（无 `v-if` 门控） | `frontend-inventory.md §1.4`；**正确样板**见 `purchase-prep` 的 `hasPageConfig` 门控 |
| 10 | **E2E 的「查询方案占位」判据恒不触发**：判据是 `src 含 '--查询方案--' 且不含 QuerySchemeBar`，但该文案只存在于组件内部（`shared/QuerySchemeBar.vue:20`），页面源码里不出现 ⇒ **这是一条永远通过的假判据** | `frontend-inventory.md §1.5` |
| 11 | **E2E 第 4 节探针探的是死接口**：`回款统计` 探 `/erp/finance/collection-stats`（旧死端点）、`推广分析` 探 `/erp/sale/promotion/analysis`、`查费用` 探 `by-department/by-type` —— 4 个探针端点对应的 api 方法**均零页面引用**，页面实际用的是另一些端点 ⇒ 给出「接口通过」的假信心 | `frontend-inventory.md §2.2 注` |

### 4.2 页面间重复代码（可直接抽 `shared/`）

| 重复块 | 完全相同 | 涉及 | 说明 |
|---|:--:|---|---|
| `handleF8Key(e)` | **27 页逐字相同** | ar-balance-sheet:480 … stock-detail:477 | 连同 27 处 `addEventListener/removeEventListener` 样板，应抽 `shared/useF8Print.ts` |
| `handleExport()` | 13 页 | ar-balance-sheet:502 … sales-performance:659 | 均为 `executeExport({fileName, headers, total, fetchAll, mapToRows, fallbackRows})`，仅 `fileName` 不同 |
| `handlePrint()` | 10 + 3 + 3 页 | check-expense:481 … | `window.open` + 拼 HTML 表格 |
| `fmtMoney()` 本地副本 | **21 页** | ar-balance-sheet:224 … | 与 `shared/docActions.ts:108` 的 `formatMoney` **函数体逐字相同**，却只有 9 页选择 import 共享版；命名还分 `fmtMoney`/`formatMoney` |
| `formatNumber()` / `fmtNum()` | 7 + 14 页 | check-batch:546 … | 同义不同名 |

### 4.3 数据库层【实测】

| 项 | 明细 |
|---|---|
| **零引用的分析相关表（9 张）** | `erp_commission_settlement_config` · `fee_statistics` · `fin_analysis_report` · `fin_data_analysis` · `fin_financial_report` · `fin_report_instance` · `fin_report_template` · `inventory_report_items` · `inventory_reports` —— 后端 Java/XML **0 引用**，且全部 **0 行**（死表候选，处置需按"观察期→改名→drop"流程） |
| **有引用但 0 行的提成表** | `erp_commission_record` / `erp_commission_rule`（见 §3.5） |
| **分析页数据源的行数快照** | `erp_stock` **4** · `erp_sale_pre_order` **0** · `mall_order` **0** · `finance_receivable` **6** · `finance_payable` **3** · `finance_ledger` **22** · `finance_voucher` **69** · `erp_sale_order` **29** · `erp_sale_outbound` **35** · `erp_purchase_order` **11** · `erp_purchase_inbound` **39** · `biz_party` **152** · `finance_account_subject` **107** · `erp_product` **83** ⇒ 「预订货查询」「交易分析」「提成两页」在 dev 库下**必然空白**（部分与 README §4.2 的登记一致） |
| **`sys_dept` 0 行 / `sys_department` 5 行** | 与财务审计同一处断点（部门维度取数可能读错表） |
| **`finance_account_subject` 的 `subject_code` 有 4 组重复** | 均为 **E2E 测试脏数据**（`T9001`×16 / `T9001.01`×14 / `ET9001`×3 / `ET9001.01`×3，全部 `tenant_id=1`，名称含「E2E测试科目」「重复编号」）⇒ 该表被验收脚本反复写入同名科目污染 |

### 4.4 后端取数与实现的规范性问题（详见 `backend-inventory.md`）

| # | 问题 | 影响 | 证据 |
|---|---|---|---|
| 1 | **费用矩阵 join 不带租户 ⇒ 金额可成倍放大**：`ExpenseAnalyticsServiceImpl.java:51` 的 `LEFT JOIN finance_account_subject s ON s.subject_code = i.subject_code` 既无 `tenant_id` 也无软删条件，而该表**无 `subject_code` 唯一约束**（全仓迁移 0 命中） | 一个 code 匹配多行 ⇒ `SUM(i.amount)` 虚增，「查费用」页金额偏大。**本轮实测该表确有 4 组重复 code**（见上表，虽源于测试数据）；多租户下同一 code 每租户各一份会**稳定触发** | 读码 + SQL 实测 |
| 2 | **同一状态码两处文案不一致**：`SaleAnalysisReportServiceImpl:1277-1289` 把 `erp_sale_order.status` 的 `2→待付款`、`3→待发货`；而 `DocQueryService:75-76` 对同一 status 映射为 `2→待发货`、`3→部分发货` | 同一单据在「经营历程」与「销售分析」显示不同状态名 | 读码 |
| 3 | **`dutyText()` 把脏值冒充为「企业员工」**：`CommissionAnalyticsServiceImpl.java:164-172`，`riderType` 非数字时默认返回「企业员工」 | 伪造值而非留空，且角色过滤（`:227-237`、`:771-776`）跟着错 | 读码 |
| 4 | **「方案汇总提成」视图退化**：`CommissionAnalyticsServiceImpl.java:396-420` 的 SQL 无 `GROUP BY`、4 个方案列恒 NULL，只剩一条 `HAVING SUM(...) <> 0` 的全表总计行 ⇒ 对标 5 列 5/5 的 Tab 实际只有 1 行数字 + 4 个空列 | 页面"能开但没内容" | 读码 |
| 5 | **两个派生列互为相反数**：`SaleAnalysisReportServiceImpl.java:946-947` 的 `orderFloatQty = orderQty - saleQty` 与 `saleFloatQty = saleQty - orderQty`，而类注释 `:47` 明写「本系统无独立浮动数量列」（列为缺口） | 注释与实现自相矛盾，展示即得两个等值反号的伪列 | 读码 |
| 6 | **权限码 ↔ `api_path` 反查失效**：`sys_permission.api_path` 一个码只有一列，而本模块多个端点共用一个码（`doc:docquery:list` 3 个 / `sale:analysis:list` 5 个 / `marketing:commission-analytics:list` 5 个 / `system:report-analytics:view` 9 个）⇒ 基于 `api_path` 的**兜底反查**（`RbacService.hasApiPermission`）命中不到未登记端点 | 方法级 `@SaCheckPermission` 不受影响，兜底层失效 | 读码（`SysConfigController:29-32` 已把该机制写进注释） |
| 7 | **类注释过期**：`PartnerLedgerController.java:32-36` 称 `POST /reconcile`「仍只校验登录」，实际 `:64` 已挂 `finance:analytics-partner-balance:reconcile`（且该码已落库） | 评审易误判为裸端点 | 读码 |

> **正面**：`PurchaseAnalysisReportServiceImpl:197-203` 的仓库/部门子查询**显式带租户**，是全模块子查询写法最规范的一处，可作为整改样板。

### 4.5 性能与并发（7 处同型，均为"全量装入内存 + 内存分页"）

| # | 问题 | 证据 |
|---|---|---|
| 1 | **内存分页且无 `pageSize` 上限**（7 处私有实现） | `AnalyticsSupport:87-100`（finance/expense 4 服务）· `CommissionAnalyticsServiceImpl:106-119` · `PurchaseAnalysisReportServiceImpl:558-571` · `SaleAnalysisReportServiceImpl:1027-1040` · `InventoryAnalysisReportServiceImpl:492-506` · `PreOrderAnalysisReportServiceImpl:319-333` · `PromotionFunnelReportServiceImpl:104-116`。**对照**：`DocQueryService:271` 有 `Math.min(...,100)` 上限，其余**没有** ⇒ 传 `size=1000000` 即可让单请求返回整表 |
| 2 | **综合单据一次请求把 13 表 UNION 跑 2~3 遍** | `DocQueryService:265-267`（count+sum 聚合）与 `:277-290`（分页）各执行一次完整 `sourceSql`；`/pending-docs/page` 再多执行一次（`pendingSummary`，`:312-315`）⇒ **3 次 13 表全扫 + 3 次 `LEFT JOIN sys_user`**，且 UNION 结果上的 `ORDER BY … LIMIT` 无索引可用、无缓存 |
| 3 | **履约分析全量入内存 + 超长 IN** | `SaleAnalysisReportServiceImpl:1179-1191` 把 `erp_sale_order` **全量（无 LIMIT）**装入内存 → `:1195-1201` 收集全部 orderId → `:1237-1243`/`:1261-1265` 用 `IN (?,?,…)` 拼**全部 id**；PG 参数上限 65535 ⇒ **超过即报错** |
| 4 | **N+1** | `PartnerLedgerServiceImpl:101` 的 `page()` 对**每一行**结算单位调 `partnerCode(name)`，后者单发 SQL（`:196-205`）⇒ 1000 个往来单位 = 1001 次查询 |
| 5 | **清账单号并发竞态** | `PartnerLedgerServiceImpl:367-383` 用 `MAX(source_no)+1` 生成 `QZ-YYYYMMDD-00X`，**无锁、无唯一约束兜底** ⇒ 并发清账可重号 |
| 6 | 报表分析 `/overview` 一次请求串行触发 4 次全量生成 | `ReportAnalyticsController:206-209`（yoy+mom+trend+anomaly）—— 该控制器无前端消费，见 §3.6 |

### 4.6 后端死代码（方法级，置信度高）

`SqlWhere.eq:29-35`（0 调用）· `DocQueryService.concat:354-358`（private，0 调用）· `PromotionFunnelReportServiceImpl.byNum:150-154`（自标 `@SuppressWarnings("unused")`，注释称"保留以避免子类重复实现"而该类语义上无子类）· **12 个死 DTO 字段**（`AnalyticsQuery` 6 个、`CommissionAnalyticsQuery` 3 个、`SaleAnalysisReportQueryDTO` 3 个，全后端 getter 零命中）。

> ⚠️ **三个"看似无引用实则内用"的反例（勿误删）**：`AnalyticsSupport.nvl`（被同类 `round()`/`ratio()` 调用）、`numericId`（`PartnerLedgerServiceImpl:215`）、`tenantIdText`（`InvoiceStatsServiceImpl:124`）。

---

## 五、冗余与死代码清单

| 类别 | 数量 | 说明 |
|---|---|---|
| **死 API 方法**（`api/analytics.ts`） | **14** | `stockReportApi.invSummaryPage/prepAnalysis/alertList`、`financeAnalyticsApi.collectionStats`、`saleAnalyticsApi.promotionAnalysis`、`commissionApi.rulePage/recordPage`、`purchaseAnalyticsApi.orderStatistics/inboundPage/docPage`、`expenseAnalyticsApi.statisticsPage`、`invoiceAnalyticsApi.dateRangeList`、`expenseStatisticsApi.byDepartment/byType` —— 全部**零页面引用**（方法 A 全 `src` 遍历 + 方法 B 仓库级二次搜索，置信度高） |
| **同名冲突对象** | 2 | 两个 `expenseAnalyticsApi`（§4.1 #5） |
| **死后端端点** | 11+ | `ReportAnalyticsController` 10 个（§3.6） · 旧 `GET /erp/finance/collection-stats`（§3.7） · `/erp/stock/inv-summary/page`、`/erp/stock/prep-analysis`、`/erp/stock/alert`、`/erp/marketing/commission/rule/page` 等（对应上述死方法） |
| **死表** | 9 | §4.3 |
| **导出面无外部消费者** | 2 | `shared/docTypes.ts` 的 `DOC_TYPES`、`getDocType`（仅同目录内部使用）⇒ **应保留但不必导出**，不是删除 |
| **未使用 import** | 1 | `sales-debt/index.vue:145` 导入 `QUICK_DATES` 后未使用（全模块唯一一处） |
| **后端方法级死代码** | 3 + 12 | `SqlWhere.eq`、`DocQueryService.concat`、`PromotionFunnelReportServiceImpl.byNum` + 12 个死 DTO 字段 —— §4.6（附 3 个"看似无引用实则内用"的反例） |
| **空目录** | 1 | `views/common/placeholder/`（空，但被 117 条菜单的**旧种子**引用 ⇒ 见 §4.1 #6） |

**已明确排除的误报**（避免整改跑偏）：
- `storage-key` 与 `global-config-key` **同值是本模块金标准的明确要求**（《_开发指南-金标准》§九.6），不是缺陷；真正要查的是「页面配置 key 是否等于列 key」——31 页**无一冲突**。
- 31 个 `analytics/*` 目录**无孤儿页**（三源交叉确认）。
- `views/analytics/**` 内**无** `console.log`/`debugger`/TODO/硬编码假数据。

---

## 六、跨模块关系

### 6.1 分析模块是纯「消费方」：读 6 个域的接口，不产出数据

31 页共 import **9 个 api 文件**，跨 6 个模块：

| 依赖来源 | 被分析页使用的对象 | 对应后端模块 |
|---|---|---|
| `api/analytics*.ts`（本模块自有封装） | `docQueryApi`/`docActionApi`/`stockReportApi`/`saleAnalyticsApi`/`mallAnalyticsApi`/`commissionApi`/`crmMarketingApi`/`financeAnalyticsApi`/`shopUserApi` 等 | 分散在 6 个模块（见范围） |
| `api/finance/index.ts` | `auxiliaryBalanceApi`、`expenseDocApi` | erp-finance |
| `api/erp/partner.ts` | `customerRegionApi`、`partnerCategoryApi`、`partnerGradeApi` | erp-partner（资料） |
| `api/erp/product.ts` | `productApi`、`productCategoryApi` | erp-partner / erp-sales |
| `api/marketing.ts` | `couponApi`、`groupBuyApi`、`flashSaleApi` | erp-marketing |
| `api/department.ts` | `departmentApi` | core-base（系统） |

**分析模块自身不产生数据、不写业务表**（除三处**受控动作**：待审批/草稿的单据动作、往来余额表的 `reconcile`、业绩提成中心的 `settle`）—— 这三处均按「调各域既有端点、不直改状态列」的原则实现（`docActions.ts:10-13` 注释明示），**设计方向正确**。

### 6.2 上游链路现状（决定页面是否有数据）

| 分析页 | 上游 | 现状 |
|---|---|---|
| 待审批 / 草稿 / 经营历程 | 13 类单据表 UNION | ✅ 有数据（经营历程 85 条） |
| 销售分析 / 业绩 / 履约 / 欠款 | 销售单据 | ✅ 有数据 |
| 采购分析 / 采购准备 | 采购单据 + 库存 | ✅ 有数据 |
| 查库存 / 进销存 / 库存明细 | `erp_stock` | ⚠️ 仅 **4** 行（与仓储模块审计的「库存双轨漂移」相关） |
| **预订货查询** | `erp_sale_pre_order` | ❌ **0 行** |
| **业务员提成 / 业绩提成中心** | `erp_commission_record` | ❌ **0 行且无写入者**（§3.5） |
| **交易分析** | `mall_order` | ❌ **0 行** |
| 商城客户列表 / 推广客户列表 | `shop_user` + `shop_user_tenant` | ❌ **接口 500**（§2.4） |
| 往来余额表 | 辅助核算 / 凭证 | ✅ 有数据（`finance_voucher` 69 / `finance_ledger` 22） |

### 6.3 上游接口故障会直接瘫痪分析页 —— 本轮实证

`/api/erp/mall/admin/user/page`（**erp-mall 模块**的 `MallAdminController`）的拆箱 NPE（§2.4）直接让**分析模块的 2 个页面**取数 500。分析域全部取数都依赖别的模块的接口，**没有独立的容错或降级**。

---

## 七、与开发文档的验收基准对照

**34 个 md 文件** = 31 篇页面文档 + `README.md` + `_开发指南-金标准.md` + `跨模块建模决策-20260918.md`；另有菜单侧 5 篇（`系统菜单设计与管理/`）。

### 7.1 README §4「31 页已全部完成金标准改造」—— **属实**（本轮独立验证）

| 声称 | 本轮验证 | 结论 |
|---|---|---|
| 31 页统一走路线 A/A′ | E2E §3 骨架 10 项 **31/31 全过**；独立复算 `_skeleton.cjs` 逐页一致 | ✅ 属实 |
| 多视图 Tab 页逐 Tab 独立一套列定义/查询/`storage-key` | E2E §5 逐 Tab 列数断言**全部与对标吻合**；key 命名逐页核对无冲突 | ✅ 属实 |
| 对标有页面配置弹窗的页接 `PageConfigPanel` | 页级 21/21 正确（10 页对标无面板的都没接） | ✅ 属实（Tab 级有 3 页多接，见 §4.1 #9） |
| 上一版 README 写的「模块级 E2E 当前**尚无**」 | 现已有 `tools/e2e-analytics.cjs`（七节），本轮跑通 | ✅ 已补齐 |

> ⚠️ **但这句话容易误读**：它证明的是**页面做工达标**，与「功能能否使用」是两件事。本轮 P0 的 4 条问题（路由 404、租户不可见、非超管 403、接口 500）**都不在这句话的射程内**，也不影响 E2E 的 203 项通过 —— 因为 E2E 用的是**系统租户的超管账号**。

### 7.2 README §4.2「仍然成立的缺口」—— 抽查均属实

| README 登记 | 本轮验证 |
|---|---|
| `erp_commission_record` 全库无写入者 → 提成三 Tab 恒 0 | ✅ 复核属实（无 INSERT，见 §3.5） |
| 交易分析 / 商城客户列表各 3 列无数据源 | ✅ 复核属实（`mall_order` 0 行、无访问埋点表），E2E 用 `override` 显式登记差额 |
| 对标列清单不可得（采购分析「按时间」Tab、业务员提成） | ✅ E2E SPEC 中对应项 `count: null`，**未做列数断言**（未编造） |
| 查询方案为本机具名方案（localStorage） | ✅ `shared/QuerySchemeBar.vue` 实现一致 |

### 7.3 菜单文档侧的两处规范空白（与本模块 P0 直接相关）

逐字核对 `系统菜单设计与管理/` 5 篇后确认，以下规则**全部"未提及"**：
- **`menu_code` 的命名与唯一性规则**（§2.1 的根因就是它没有约束）
- **`menu_level` 的取值域**（文档与前端 UI 只承认 `0=租户级 / 1=系统级`，而库中存在 124 条 `3`，分析模块占 29 条 ⇒ §2.2）
- **权限码从 `menu_code` 的派生规则**（§3.1 的根因：两套命名空间并存且无人规定它们要对应）
- 排序字段规范、SoD 职责分离、数据权限要求（后三项与财务模块审计结论一致）

> ⇒ **§2.1 / §2.2 / §3.1 三条都是"制度上没有规则"的直接后果**，不是某个人写错了某个值。

---

## 八、检查方法与复现

### 8.1 本轮新增的脚本（均已落位 `tools/`）

| 脚本 | 用途 | 本轮结果 |
|---|---|---|
| `tools/audit-analytics-contract.py`（新增） | 分析模块前后端契约双向比对（自动发现页面 import 的 api 文件 → 按**对象**收窄调用 → 与全仓 3101 个端点比对） | 120 个调用点、**断链 1**、方法不匹配 1、双前缀 0、动态路径 7 |
| `tools/verify-analytics-authz.cjs`（新增） | 超管/非超管逐接口权限实测 + 非系统租户菜单过滤实测 | 非超管 **15/18 被拒**；tenant2 菜单 **analytics 0 条** |
| `tools/verify-menu-route-collision.cjs`（新增） | 重复 `menu_code` 组的路由可达性实测 | **3 个页面 404** |
| `tools/diag-analytics-purchase-analysis.cjs`（新增） | 单页 404 的 DOM/console/网络取证 | 确认渲染 catch-all 404，无报错 |
| `tools/e2e-analytics.cjs`（既有） | 模块级 E2E 七节 | 三次运行合并：**203 通过 / 1 失败 / 1 未改造** |

### 8.2 运行时前提与陷阱（本轮实踩）

- 后端 `5655`、前端 `5656`（vite）；账号：`e2e_analytics` / `e2e_hr` / `e2e_hr_ta` / `e2e_hr_t2`（密码均 `admin123`，由 `tools/e2e-hr-user.sql` 等建立）。
- ⚠️ **sa-token 单端互踢**：首次用 `admin` 跑 E2E，第 5 节整节被并行会话踢成「页面未渲染」，**极易误判为页面缺陷**。必须用模块专用账号。
- ⚠️ **后端中途退出**：E2E 第二次跑到第 13 页时 `ECONNREFUSED`（`core-api-0.3.24-exec.jar` 进程消失，场上两个 9/18 启动的僵尸进程并未监听）。已自行重启并补齐剩余 17 页。
- ⚠️ **`sys_menu.menu_level` 取值域与文档不符**、**`sys_permission.status=0` 才是启用**（与直觉相反）—— 本轮查询口径均已按实测校准。

### 8.3 分工与复核

- 前端逐页盘点（骨架/key/引用矩阵/接口清单/重复代码）由子代理产出 `tool-results/analytics-audit/frontend-inventory.md`（含 8 个可重跑脚本）；
- 后端端点与实现质量详见 `tool-results/analytics-audit/backend-inventory.md`；
- **子代理结论一律复核**：本轮抽查 **7 条** —— 前端 5 条（EXPENSE 动作错发、draft 删除按钮未按矩阵、commission-center 死开关、`isQueryVisible` 覆盖率 21 vs 20、31 页无孤儿页）+ 后端 2 条（`DocQueryService` EXPENSE 分支的跨租户泄漏、`ExpenseAnalyticsServiceImpl` join 缺租户及 `subject_code` 重复实测），**7 条均复核属实**；另有 1 条（`storage-key` 与 `global-config-key` 同名）被子代理**主动纠正为本仓规范要求**、未计入缺陷。

### 8.4 本轮产出的证据文件（`tool-results/analytics-audit/`）

| 文件 | 内容 |
|---|---|
| `db-menu-permission-findings.md` | 菜单/权限/路由冲突的原始取证（含 M-1/M-2/P-1/P-2/R-1 编号发现） |
| `frontend-inventory.md`（66 KB） | 31 页骨架 / key / 引用矩阵 / 接口清单 / 重复代码 / 桩 |
| `backend-inventory.md` | 后端端点清单 / 裸端点 / 假数据 / 取数正确性 / 性能 / 死代码 |
| `contract-compare.md` | 前后端契约比对原始结果 |
| `route-collision.md` | 路由冲突实测表 |
| `authz-probe.json` | 权限与菜单实测原始数据 |
| `e2e-result.md` | E2E 三次运行记录与合并口径 |

---

## 九、建议的处理顺序

**第一批（P0，建议立即处理）** —— ✅ **已于 2026-09-23 本轮执行完毕，详见「〇·补、本轮修复记录」**

1. ~~修「采购分析」页 404~~ → ✅ **已完成**（迁移 V11.499.0 补 7 条 route_name；并新增前端重名路由告警 `assertNoDuplicateRouteName`）；**同时**修 70051/70052 两条（同类问题已实测 404）—— §2.1
2. ~~给 `sys_menu.menu_code` 加唯一约束~~ → ❌ **经复核放弃**：会破坏 `80091`/`70011` 这类**真双入口**（path 相同、有意共用 menu_code）；改由「补 route_name + 前端冲突告警」覆盖。—— §2.1 / §7.3
3. ~~修 `mall/admin/user/page` 的拆箱 NPE~~ → ✅ **代码已修**（`MallAdminServiceImpl:232-234`，由并行会话完成），⏳ 待 `erp-mall` 重新构建后生效——2 个分析页随之恢复。—— §2.4
4. ~~29 条菜单 `menu_level` 由 3 改回 0~~ → ✅ **已完成**（同迁移；tenant 2 实测可见 29 条，原为 0）—— §2.2
5. ~~修 `DocQueryService` 的 EXPENSE 分支~~ → ✅ **已完成**（补 `tenant_id = ?::text AND deleted = false`，注意该表 tenant_id 是 varchar）—— §2.5

**第二批（P1）** —— 明确缺陷**已于 2026-09-26 执行完毕**（详见「第二轮修复记录」）；带 ⏸ 的需产品拍板，本轮未动

6. ~~权限码补授给功能角色~~ → ✅ **已完成**（2026-09-27，`V11.515.0` 把 14 个码授给 **SYSTEM_ADMIN = 租户超管**；实测被拒接口 **15 → 0**）。菜单码与权限码的对应规则（§3.1）仍未定 —— 但那是**菜单可见性**的事，与本次授权无关 —— §3.1
7. ~~修 EXPENSE 动作分发~~ → ✅ **已完成**（`docActions.ts` 加 `kind` 判断）—— §3.2
8. ⏸ **评估是否需要行级数据权限**：至少「销售业绩 / 业务员提成 / 查应收应付」建议按职员/部门隔离 —— §3.3（**属产品决策**）
9. ~~修「业绩提成中心」页面配置联动~~ → ✅ **已完成**（补 `isQueryVisible`）—— §3.4
10. ⏸ **决定提成链路走向**：接《业务员提成》落库（补生产者 + `rule_id`），或下线提成页 —— §3.5（**属产品决策**）
11. ⏸ **处置两处死岛**：`ReportAnalyticsController`（9 端点 + 编造趋势）、旧 `CollectionStatsController` —— §3.6 / §3.7 / §3.11（**需确认是否下线**）
12. ~~修「业务草稿删除按钮」与「待审批批量能力」的矩阵一致性~~ → ✅ **已完成** —— §3.8
13. ~~统一租户解析失败的错误处置~~ → ✅ **已完成**（6 处统一为「请重新登录」）—— §3.10
14. ~~修 `ExpenseAnalyticsServiceImpl:51` 的 join~~ → ✅ **已完成**（补 `tenant_id` + 软删）。`finance_account_subject.subject_code` 的租户内唯一约束**未加**（需先清测试脏数据，见 §4.3）—— §4.4 #1
15. ~~补其余手写 SQL 的租户条件~~ → ✅ **部分完成**：`codeExpr` 子查询、`bp2` 分类子查询**已补**；`biz_party_category`/`biz_party_contact` 与库存明细的 13 段 join 经复核为**按主键 id 关联**（不产生跨租户行），**有意不改** —— `backend-inventory.md §4.1`

**第三批（P2 + 收尾）**

16. 抽 `shared/` 复用件（F8 打印 / 导出 / 打印 / 金额格式化），消除 27+21+13+10 份重复 —— §4.2
17. 清理 14 个死 API 方法、9 张死表（按"观察期 → 改名 `zz_deprecated_*` → drop"流程）
18. 清 `.atcode/` 污染（`git rm -r --cached`，`.gitignore` 已有规则）—— §4.1 #7
19. 统一分页参数命名与多视图实现方式；处置 3 个 Tab 多接的面板 —— §4.1 #3/#4/#9
20. 修 E2E 的两条假判据（查询方案占位判据、第 4 节探针指向死端点）—— §4.1 #10/#11
21. 补 31 页 component 的 Flyway 迁移（使库可从迁移重建）—— §4.1 #6
22. 清理 dev 库中的 E2E 测试脏数据（`finance_account_subject` 的 4 组重复科目等）—— §4.3
23. **给 7 处内存分页补 `pageSize` 上限**（对齐 `DocQueryService:271` 的 `Math.min(...,100)`）—— §4.5 #1；履约分析的"全量入内存 + 超长 IN"建议改为游标/分批 —— §4.5 #3

---

*报告生成：2026-09-23 · 所有【实测】结论均附可复现命令，【读码】结论附 `文件:行号`，【清点】结论附扫描方法。*


