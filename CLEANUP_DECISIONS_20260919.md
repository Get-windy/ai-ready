# 收尾裁决：业界对照与落地方案

> 2026-09-19 · 配套 `CLEANUP_SCOPE_20260919.md`（三类遗留盘点）
> 本文只解决两件事：① 本轮已直接修掉的；② 需要裁决的每一项 —— 业界怎么做、我推荐什么、怎么落地。

---

## 0. 本轮已直接修复（无需裁决，已完成）

| 编号 | 问题 | 改动 | 为什么可以直接修 |
|---|---|---|---|
| D-01 | 菜单 70063「采购换货单」`component` 指向已不存在的 `views/erp/purchase-exchange/form.vue`，新增/编辑显示「页面组件未找到」 | 新增迁移 `V11.427.0__Fix_Purchase_Exchange_Menu_Component.sql`，改为 `views/purchase/exchange/form.vue`（`componentMap:407` 已有该映射） | 纯路径修正，一行 UPDATE，幂等且可回滚 |
| DC-01 | `DatabaseInitializer` 内置 133 条旧菜单种子（ID 1000–14004），与 Flyway 菜单体系并行，且 `MERGE INTO ... KEY(id)` 在 PG 上必失败 | 删除菜单种子段与两个 `appendMenu` 重载，`ensureMenuData()` 收敛为只调 `ensureRoleMenuAssociations()`（文件 1768 → 1539 行，编译通过） | 该路径现网永不触发（tenant-admin 菜单非空），删除零行为变化 |
| D-03 | 授权取数路径只过滤 `deleted`、不过滤 `status` ⇒ 禁用角色/权限/菜单不生效（fail-open） | 见下方 8 处 | 今天**零行为变化**（三表数据方向与新增条件一致），属纯加固 |

### D-03 的 8 处改动明细

| # | 位置 | 补的条件 | 该表 `status` 实证语义 |
|---|---|---|---|
| 1 | `SysUserMapper.xml` `selectRoleCodesByUserId` | `r.status = 0` | `sys_role`：**0=启用**（5 行全 0） |
| 2 | `SysUserMapper.xml` `selectPermissionCodesByUserId` | `p.status = 0` + `r.status = 0` | `sys_permission`：**0=正常**（454 行全 0） |
| 3 | `SysUserMapper.xml` `selectMenuIdsByUserId` | `m.status = 1` + `r.status = 0` | `sys_menu`：**1=启用**（389 行全 1） |
| 4 | `SysMenuMapper.xml` `selectMenusByIds` | `status = 1` | 同上（用户菜单树下发） |
| 5 | `SysRoleMapper.xml` `selectRolesByUserId` | `sr.status = 0` | `sys_role`（参与数据权限判定） |
| 6 | `SysRoleMapper.xml` `selectPermissionIdsByRoleId` | `sp.status = 0` | `sys_permission`（角色继承有效权限计算） |
| 7 | `RoleMapper.xml` `selectByRoleCode` | `status = 0` | `sys_role`（工作流审批人解析） |
| 8 | `SysPermissionMapper.java` ×2 | `p.status = 1` → `= 0`（**方向写反，恒空**） | `sys_permission` |

> **⚠️ 为什么不能"统一加 `= 0`"**：`sys_menu` 是 **1=启用**，与 `sys_role`/`sys_permission` 的 0=启用**相反**；而三个实体类的 Javadoc 都写着「0-正常 1-禁用」——**注释是错的**。若照注释统一加 `= 0`，389 条菜单会被全部过滤掉，全站菜单消失。落地前逐表 `SELECT status, count(*) ... GROUP BY 1` 取证，是这次没踩坑的唯一原因。
>
> **刻意未改的**：`PermissionMapper.selectByRoleId` / `selectByParentId`、`RoleMapper.selectByParentId` —— 这些是**管理端配置视图**（角色/权限管理页），必须能看到已禁用项才能重新启用；加了 `status` 过滤反而会让管理员无法恢复。这与"运行时授权取数必须过滤"是两种不同用途，不能一刀切。

### 顺带修掉的 3 个真缺陷（由子代理全局审计发现）

| 位置 | 问题 | 修法 |
|---|---|---|
| `ClientAuthController`（打印客户端登录） | 只校验密码与租户归属，**完全不校验 `user.getStatus()`** ⇒ 已停用账号仍能登录取到 Sa-Token | 补 `if (user.getStatus() == null \|\| user.getStatus() != 1)` 拒绝，与主站 `SysUserServiceImpl#login` 口径一致 |
| `WorkflowServiceImpl`（部门审批人解析） | `.eq(SysUser::getStatus, 0)` —— 按实证 `sys_user.status` 是 **1=启用**，原写法会把**已停用账号**选成审批人（fail-open） | 改为 `.eq(..., 1)`，并更正了引用错误来源（`AuthServiceImpl`）的注释 |
| `SysPermissionMapper` ×2 | `p.status = 1` 方向反了（同上 8-#8） | 改为 `= 0` |

**用户表的 `status` 语义已实证为「1=启用，0=禁用/待审批」**（依据：`SysUserServiceImpl#login` 用 `!= 1` 拒绝；`TenantRegistrationService` 注册时 `setStatus(0)`、审批通过 `setStatus(1)`）。注意它与 `sys_role`/`sys_permission` 相反。

### 验证状态
- `core-base` 模块编译：BUILD SUCCESS（子代理实测）。
- 全量 `mvn -o -DskipTests test-compile`：本轮已发起，结果见文末「验证记录」。
- 新增门禁测试与审计脚本见 §2。

---

## 1. 需要你裁决的 7 项 —— 业界对照与推荐

### 1.1 ⚠️ 应收 / 应付（原判定有误，必须先纠正）

**我原先的判定错了。** 我把它归为「第 1 类冗余删除，被 `analytics/check-receivable`(80457) / `check-payable`(80458) 取代」。实际核查后端后：

```
ReceivableController:  POST /        新增（手工立账）
                       GET  /list    列表
                       GET  /aging   账龄分析
                       PUT  /{id}/write-off   核销
                       PUT  /{id}/bad-debt    坏账标记
                       DELETE /batch  GET /export
PayableController:     同上（除 bad-debt）
```

**这是完整的应收/应付子分类账，不是只读报表。** 而 `analytics/check-receivable` 只是查询视图。

**业界对照**

| 系统 | 做法 |
|---|---|
| SAP FI-AR/AP | AR/AP 是**独立子分类账（subledger）**，管理 customer-to-cash 全周期：主数据、开票、信用检查、收款、催收（dunning）、对账。标准事务：`FB70` 客户发票（手工立账）、`FB75` 客户贷项凭证、`F-28` 收款清账、**Write Off Items**（支持完全/部分核销，生成核销凭证且可冲销） |
| SAP 高级形态 | `AR Workplace`：实时余额、余额分类、手工核销的发起/取消/审批、批量审批、**按公司/科目/账龄配置自动核销**、期末账龄 |
| NetSuite | 坏账两种方法：直接核销（日记账借坏账费用/贷 AR）与备抵法（Allowance for Bad Debts 科目）；核销后需用零金额收款把发票移出账龄表 |
| Odoo | 账龄直接建在 `account_move` 上（`amount_residual`、`payment_state`），按 **due date** 分桶、区间可配置 |

**结论**：手工立账（应收/应付单）、坏账准备/核销、账龄分析，在成熟 ERP 里都是**应收应付模块的核心业务事务**，不可能由只读查询页替代。

**推荐**：**改判第 2 类，挂菜单** —— 挂到「财务 → 收入支出 / 往来」下，与 `analytics/check-*` 并存（后者是查询分析，前者是业务处理）。

**落地步骤**
1. 核对 `finance/receivable/index.vue` 是否已调用 `PUT /{id}/write-off` 与 `PUT /{id}/bad-debt`（两个动作都在后端就绪）——若页面未接线，先接线再挂菜单；
2. 菜单命名建议区分职责：「应收账款管理」「应付账款管理」（对应子分类账）vs 已挂的「查应收」「查应付」（对应查询）；
3. 写 Flyway 迁移挂载，验证：新增一笔应收 → 账龄出现该笔 → 核销后从账龄消失。

**若你决定不挂**：则必须同时接受「系统没有手工立账与坏账管理能力」这一事实，并在文档里写明由外部总账系统承担 —— 不要留着页面又不挂菜单。

---

### 1.2 核销中心（`finance/write-off`）

**现状**：`WriteOffController` 只有 4 个 GET（`/page`、`/{id}`、`/receipt/{receiptId}`、`/payment/{paymentId}`）——**只读**。真正的核销动作在 `ReceivableController`/`PayableController` 的 `PUT /{id}/write-off`。

**业界对照**：SAP 里核销有两种并存形态 —— **单据内清账**（F-28 收款时逐笔清账）与**独立工作台**（AR Workplace 的批量核销/审批/自动核销规则）。两者不是二选一：逐笔走单据、批量与跨单据走工作台。

**推荐**：**保留并挂菜单**，定位为「收付款核销中心（查询 + 批量入口）」。

**理由**：只读并不意味着无价值 —— 财务需要按收款单/付款单反查核销明细、需要批量视角。但**要诚实标注它的能力边界**：当前它不能发起核销，只能查。若要名副其实，应补批量核销端点。

**落地**：挂到「财务 → 收付款」下；页面或菜单名加注「（查询）」，避免用户以为能在此发起核销。补批量核销列入后续迭代。

---

### 1.3 资金流水（`finance/capital-flow`）vs 查资金（`analytics/check-fund` 80453）

**现状**：两者都用 `capitalFlowApi`；前者带导出，后者偏分析。

**业界对照**：SAP 里「资金流水」是**子分类账明细行项目**（`FBL5N` 类行的现金/银行行项目），「资金分析」是报表层。两者是**明细与视图**的关系，不是重复。

**推荐**：**保留两者但明确分工** —— 资金流水台账（明细，带导出）挂菜单；查资金（分析视图）保持现状。若两者的查询条件与列高度重合，则删台账页、把导出能力并入查资金。

**决策依据（你只需回答一个问题）**：财务是否需要一个"可按任意条件导出全部资金流水"的入口？要 → 挂台账；不要 → 删台账、导出并入分析页。

---

### 1.4 盘点两套（`wh/inventory-order` vs 盘点单 5003）

**现状**：两套独立实现 —— `wh/inventory-order`（`checkApi`，`/wms/check/*`，标题「盘点作业单列表」，可 `router.push('/wms/check/form')`）vs 菜单 5003「盘点单」（`stockTakeApi`）。功能重叠。

**业界对照**（SAP EWM 是最清晰的参照）

| SAP EWM 的三种计数方法 | 用途 |
|---|---|
| Logistics Area Count | 年度全盘（多数国家的法定要求），覆盖所有货位 |
| Product Count | **周期/循环盘点**（高价值、快周转），按设定间隔 |
| Stock Adjustment | 计划外的单点纠正，无计数任务 |

三阶段流程：**Preparation → Execution → Approval**。关键设计：**计数任务只记录结果，库存与财务的更新只在获得授权批准后发生**；支持复盘（recount）、拒绝、跳过；可设独立于执行日期的**关键日期（Stichtag）**用于追溯过账。

Odoo 侧的对应关系：`stock.quant` 上的 cycle count vs 年度全盘；最佳实践强调「周期盘点 / 年度全盘 / 计划外调整」三者**职责分离、各自独立权限**。

**结论**：**「盘点作业单」与「盘点单」不是重复，而是同一流程的两个阶段** —— 作业单是执行层（谁在哪个货位盘了什么、差异多少），盘点单是单据层（审批、库存变动、财务过账）。成熟系统的做法是**一个模块两个阶段**，而不是两套并行实现。

**推荐**：**不删任何一套，做整合**：
1. 以菜单 5003「盘点单」为单据层（审批 + 库存变动）；
2. 把 `wh/inventory-order` 的作业明细（点位/明细/差异）作为其**执行阶段视图**（一个 Tab 或子页），复用 `checkApi` 的数据；
3. 整合后删除重复的列表入口与冗余接口。

**落地风险**：两套都有真实数据与 E2E。建议先做**只读对齐**（把作业明细挂进盘点单页面的 Tab），确认无功能缺失后再删冗余入口，不要一次性合并。

---

### 1.5 预算两套（`budget/annual` vs 已挂 `finance/budget-plan` 80130）

**现状**：两者同走 `/erp/budget/annual` 后端。`budget/annual/index.vue` 1366 行；`finance/budget-plan/` 是 index(930) + form(973) = 1903 行且**已挂菜单 80130「预算编制」**。

**业界对照**：预算在 ERP 里通常分层 —— **预算编制（planning/budgeting）**、**预算调整（adjustment）**、**预算执行与控制（execution/control）**、**预算报表**。同一实体（年度预算）在不同层有不同视图，但**编制入口只应有一个**，否则会出现两处都能改同一份预算、口径不一致。

**推荐**：**以已挂的 80130 为唯一编制入口**。
- `budget/index.vue`（总览仪表盘）、`budget/template`、`budget/adjustment`、`budget/report` → 第 2 类，挂 60608 下（它们对应调整/报表/模板，与编制不重复）；
- `budget/annual/index.vue` → **删除或降级为只读列表**，避免第二个可写入口。

**决策依据**：两页是否都能创建/修改年度预算？若是 → 必须砍掉一个（推荐保留 80130，它是金标准且有配套 form.vue）；若 `budget/annual` 只是列表视图 → 降级为只读或直接删。

---

### 1.6 `erp/stock/index`（`stock/detail/:id` 隐藏路由）

**现状**：被 `getRequiredRoutes` 注册为 `stock/detail/:id` 的组件，但仓内找不到 `router.push` 入口。

**业界做法**：详情页作为**隐藏路由**（不出现在导航中）是标准形态 —— 详情页从来不该有菜单项。

**推荐**：**保留为隐藏详情路由，不挂菜单、不删除**。成本为零；删了反而可能打断外部收藏链接或打印模板里的引用（`printing/seed-templates.ts` 提到过该 pageCode）。

---

### 1.7 `member/profile`（会员档案）

**现状**：`ARReportPage` + `memberApi`（走 `/erp/party/*`）完整 CRUD，仅被 `points-history` 以 `window.open('/member/profile?id=')` 打开；菜单 80300「会员管理」已挂 `views/marketing/member-manage`。

**业界对照**：CRM/会员系统普遍有 **360 视图**（会员档案 = 基本信息 + 积分 + 消费 + 等级 + 标签），它通常**不是一个独立菜单**，而是从会员列表点进去的详情页；也可能作为"会员档案查询"独立页供客服使用。

**推荐**：**改判第 3 类 —— 作为 80300 的详情路由**（保持隐藏路由，从会员管理页点入）。
理由：`points-history` 已经用 `window.open('/member/profile?id=')` 这样用了，说明**现有设计意图就是详情页**；把它再挂一个菜单会造成同一功能两个入口。

### 1.8 ⚠️ 第 1 类「冗余删除」清单的复核结果：**15 项不能删**

**这是本轮最重要的一次自我纠错。** 我最初的判据是"新旧页面调用同一批 api + 功能名相似 ⇒ 旧页冗余"。**判据本身是错的**：`api/*.ts` 的函数是**模块级共享**的 —— 新页面引用了同一个 api 文件的**读方法**（如 `receiptApi.page()`），不代表它调用了**写方法**（如 `receiptApi.complete()`）。

对 44 项逐条核对"旧页面的写操作是否被新页面覆盖"后，**15 项有独有写能力，删了就是删功能**：

| 旧页面 | 独有写能力 | 后端是否真实存在 | 建议 |
|---|---|---|---|
| `finance/receipt/index.vue` | **完成收款 `complete`** | ✅ `ReceiptController:241 POST /{id}/complete` | 新页补入口，或改判挂菜单 |
| `finance/payment/index.vue` | **完成付款 `complete`** | ✅ `PaymentController:225` | 同上 |
| `finance/pre-payment/index.vue` | **预付转付款/冲抵 `offsetToPayment`** | ✅ `/erp/pre-payment/{id}/offset-to-payment` | 新页补入口 |
| `erp/expense/application/index.vue` | 删除草稿费用申请 | ✅ `DELETE /erp/expense/application/{id}` | api 层已有 `expenseDocApi.delete`，新页接线即可 |
| `erp/expense/reimbursement/index.vue` | 删除草稿报销单 | ✅ `DELETE /erp/expense/reimbursement/{id}` | 同上 |
| `erp/expense/approval/index.vue` | **审批退回（RETURN）** | 需扩枚举 | 新 api 类型只允许 `APPROVE\|REJECT`，要加 RETURN |
| `hr/attendance/index.vue` | 签到/签退 `clockIn/clockOut` | ✅ | 低危：旧实现硬编码 `employeeId=0` **写脏数据**，新页已注明待后端按登录人解析员工 |
| `wms/warehouse/index.vue` | `/wms/warehouse` 主数据 CRUD | ✅ 但**是另一套后端资源**（`wms/WarehouseController` ≠ `erp/WarehouseController`） | 需先拍板 `wms_warehouse` 是否并入 `erp_warehouse` |
| `wms/location/index.vue` | 货位主数据「单条手动 save」 | ✅（批量 generate 在 `md/warehouse-plan`） | 取代页标注要改为 `md/warehouse-plan`(80520) |
| `erp/mall/product/index.vue` | **删除商品（单个 + 批量）** | ✅ | `mall/product-shelf` 全页无 delete，需补 |
| `erp/shipment/index.vue` | **审核 `approve`、出库 `ship`** | ✅ `SaleOutboundController:138/198` | api 方法已存在，只差 UI 挂载 |
| `order-center/index.vue` | **采购单 submit/approve/cancel/delete/exportData**；销售单 submit/batchDelete | ✅ | 新页只做销售单，**采购侧整体缺失** |
| `erp/partner/index.vue` | 地址簿 CRUD/设默认、银行账户 CRUD、标签、多联系人 CRUD | ✅ | 新页只有单主联系人 `savePrimaryContact` |
| `erp/stock/replenishment/index.vue` | **生成建议 / 忽略 / 创建采购单** | ✅ `StockReplenishmentController:38/45/55` | 新页只读列表 |
| `erp/stock/index.vue` | **冻结 / 解冻库存** | ✅ `StockController:58/71` | 两新页均纯查询 |
| `supplier/index.vue` | **激活/禁用供应商门户** | ✅ `/api/supplier/{id}/activate-portal` | 新页无 portal 能力 |

**另外 3 处"取代页选错了"**（不丢功能，但标注必须更正，否则下次复核会重犯同样的误判）：
- `hr/organization/index.vue` → 部门 CRUD 的真取代页是 `md/staff-dept`(80530)，**不是** `position-list`
- `wms/location/index.vue` → 货位 CRUD 的真取代页是 `md/warehouse-plan`(80520) 的"货位"Tab，**不是** `md/location`(70520)
- `wms/check/index.vue` → 能力由 `wh/inventory-order` 超集覆盖，**但 `wh/inventory-order` 自己也是待处理孤儿**；两者去留必须绑定决策（见 §1.4）

**结论**：第 1 类可删清单从 **46 项收缩到 29 项**。这 15 项的正确处置不是"删"，而是二选一：
- **a)** 把独有写能力搬到已挂菜单的新页面 → 然后删旧页（推荐，尤其 `complete`/`approve`/`ship`/`delete` 这类高频动作）；
- **b)** 旧页确有独立价值（如 `finance/receipt` 的完整收款流程）→ 改判第 2 类挂菜单。

**这也说明一件事**：孤儿页面清理不能只看"功能名相似 + 同 api"。判据必须是**写操作集合的包含关系**。

---

## 2. 鉴权覆盖率治理方案（D-02）

### 2.1 问题规模（已重建权威口径）

| 指标 | 数值 |
|---|---|
| Controller | 396 |
| 端点方法 | 3563 |
| 无 `@SaCheckPermission` | 328（83%），覆盖 2905 端点 |
| **无任何访问控制注解** | **242（61%），覆盖 2162 端点** |

> 口径修正：**必须把自定义注解算进去**，否则会高估缺口。本项目除 `@SaCheck*` 外还有 `@RequirePermission`(16 处)/`@RequireRole`/`@RequiresPermission`/`@RequireFilePermission`，其中 `@RequirePermission` 由 core-api 的 `PermissionAspect` 通过 AOP **真实执行**。计入后从 253 降到 242。
> 刻意不计入：`@DataPermission`/`@DataPermissionCheck`/`@BusinessPermissionCheck` —— 它们做数据范围过滤，不决定接口可达性。

### 2.2 业界做法：注解式 + 架构门禁 + 棘轮基线

调研结论很一致：**用架构测试（ArchUnit）强制"每个端点必须有授权注解"**，并采用**渐进式强制（gradual enforcement）**：

```java
// 业界标准写法（ArchUnit）
methods().that().areMetaAnnotatedWith(RequestMapping.class)
         .and().areDeclaredInClassesThat().areMetaAnnotatedWith(Controller.class)
         .should().beAnnotatedWith(PreAuthorize.class)
         .orShould().beDeclaredInClassesThat().areAnnotatedWith(PreAuthorize.class);
```

关键点：
- `areMetaAnnotatedWith` 一次覆盖 `@GetMapping/@PostMapping/...` 全部派生注解；
- `.orShould()` 允许类级或方法级任一位置标注；
- 在**测试阶段**失败，早于部署，不依赖用户踩坑；
- **渐进式**：存量先记录不阻塞，逐步清理后再纳入严格校验。

### 2.3 本项目的落地方案（已落地，非提案）

**① 审计脚本** `tools/audit-authz-coverage.py`
- 口径与门禁一致；`--baseline` 可重新生成基线；输出按包分组，便于分批治理。

**② 门禁测试** `core-api/src/test/java/cn/aiedge/architecture/AuthzAnnotationCoverageTest.java`
- 三条断言：**① 未注解且不在基线 → 失败**；**② 基线过期（已补注解/类已删）→ 失败**；**③ 扫描数 < 300 → 失败**（防空跑）。
- 与既有的 `ComponentScanCoverageTest`（装配门禁）同风格、同模块、同"棘轮"语义。

**③ 基线清单** `core-api/src/test/resources/known-unauthorized-controllers.txt`
- 242 个类，按业务域分组并标注优先级；文件头写明维护规则与"只许缩小"。

**实现注记**：判定没有用 ArchUnit —— 本仓库以 `mvn -o`（离线）构建，给 core-api 新增 test 依赖有失败风险；也没有用反射（`getDeclaredMethods()` 遇 classpath 缺失会抛 `NoClassDefFoundError`，把与鉴权无关的类误判成"无法判定"），改用**直接搜索 class 文件常量池里的注解描述符**（`Lcn/dev33/satoken/annotation/SaCheckPermission;`）。类级与方法级注解都写在同一个 class 文件里，因此一次字节搜索同时覆盖两者，零依赖、零类加载。未使用的 import 不写入 class 文件，注解也不会因"未使用"被优化掉 —— 不存在这两类误判。

**④ 门禁的牙齿验证（实测，不是"跑通了"）**

**第一次运行就抓到一个真实违例**，同时暴露出我的审计脚本有一个假阴性：

| 项 | 结果 |
|---|---|
| 门禁测试 | 3 个断言中 2 通过、1 **失败**：`cn.aiedge.erp.pricing.controller.PriceApprovalController` 无任何访问控制注解且不在基线内 → **这正是门禁该有的行为** |
| 该类的真实情况 | 类 Javadoc:42-43 明确写着「前端该页未使用任何 v-permission 码，故此处不加 `@SaCheckPermission`，仅依赖全局登录校验（口径与页面对齐）」——**刻意豁免**。已在基线中注明理由，并标记为「价格审批涉及定价，建议后续补鉴权」 |
| **顺带发现脚本 bug** | `tools/audit-authz-coverage.py` 按**文本**搜索注解名，被这个类 Javadoc 里提到的 `{@code @SaCheckPermission}` 欺骗，把它误判成"已鉴权"（**假阴性**，漏报一个真实的无鉴权接口）。字节码方案不受影响 —— 注释不写进 class 文件。已给脚本加 `strip_comments()` 对齐口径 |
| 修复后一致性 | 脚本与门禁均报 **243**（原 242 + `PriceApprovalController`）；交叉验证「实际无鉴权集合 == 基线集合」，无其他误报 |

> 教训：**文本级审计与字节码级审计必须对齐口径**。一个会被注释骗、一个不会，两边数字对不上时很难判断谁对。本项目的口径以**门禁测试（字节码）**为准，脚本只作辅助排查。

### 2.4 分批补注解的顺序（**铁律：先补种子，再补注解**）

> 历史事故：代码引用的 243 个权限码中曾有 90 个在 `sys_permission` 里不存在，导致非超管**一律 403**。`@SaCheckPermission` 引用了库里没有的码 = 把能用的功能直接锁死。现已修复（`missing_in_db: 0`），但这条顺序不能破。

每批的固定流程：
1. `python tools/audit-permission-codes.py` 确认本批要用的权限码**已入库且已关联角色**；缺则先补种子（注意 `sys_permission.id` 无序列默认值，须显式给 id 并先实测区间）；
2. 给目标控制器加 `@SaCheckPermission`；
3. 从基线文件删除对应行；
4. 用**非超管**账号实测：有权限可访问、无权限 403（只测"拒绝路径"会漏掉放行路径的 P0）；
5. 跑 `AuthzAnnotationCoverageTest` 确认棘轮前进且无新违例。

建议批次（按业务风险，而非按包名字母序）：

| 批次 | 范围 | 理由 |
|---|---|---|
| 1 | `erp.finance.*`、`erp.payment.*`、`erp.invoice.*`、`erp.budget.*` | 涉及金额与账务，越权后果最重 |
| 2 | `crm.*`（8 个控制器） | 整模块零鉴权，且含客户主数据 |
| 3 | `erp.stock.*`（采购/库存/销售单据） | 单据量大，影响库存与成本 |
| 4 | `wms.*` / PDA | 需先定 PDA 设备的鉴权策略（设备登录 or 用户登录） |
| 5 | `erp.marketing.*` / `erp.b2b.*` | 含 C 端公开接口，需逐个甄别，放最后 |

**每批结束的验收**：基线行数下降 + 非超管实测 + 门禁绿。

---

## 3. 未装配功能包（D-05）裁决方案

13 个包「代码在、表建了、控制器没装配、没有菜单、没有前端页面」。业界对标 **Spring Modulith** 的处理方式：

- Modulith 用 `@ApplicationModule` + `ApplicationModules.verify()` 做架构适应度测试，并支持 `Type.OPEN` 用于**渐进式迁移的传统项目**；
- 但官方文档明确指出：在完全模块化的应用里用 OPEN 模块「通常暗示次优的模块化」；
- 生产实践建议 4 阶段渐进推进，并**警告不要用 OPEN 模块来强行让测试通过**。

关键是：**必须有明确的三选一裁决，且"要"的那一项必须配一条能打到端点的冒烟测试** —— 没有冒烟就不算"要"。

| 包 | 我的推荐 | 依据 |
|---|---|---|
| `cn.aiedge.storage.*` | **删除** | 与 `platform.StorageConfigController` + `common.file.FileUploadController` 重复，前端走的是后者 |
| `cn.aiedge.report.*` | **删除** | 无前端、无文档；注意 `base.log` 的 `ErrorReportController` 是另一条活的链 |
| `cn.aiedge.search.*` | **删除** | 前后端都没接线 |
| `cn.aiedge.gateway.*` | **删除** | 微服务网关方案已废弃，`GatewayAutoConfiguration` 已被主应用 `exclude` |
| `cn.aiedge.mq.*` | **删除** | `@ConditionalOnProperty(mq.rabbit.enabled)` 未开启 |
| `cn.aiedge.webhook.*` | **删除** | 无消费者、无菜单 |
| `cn.aiedge.assistant.*` | **删除** | 与 `core-agent` 定位重叠 |
| `cn.aiedge.feedback.*` | **删除**或补齐 mobile 反馈页 | mobile-admin 只 `push('/feedback')`，路由不存在 |
| `cn.aiedge.recommendation.*` | **删除** | 无前端、无文档，产品未提需求 |
| `cn.aiedge.knowledge.*` | **保留契约、删实现** | `AGENTS.md` 声明"AI 只保留接口"，按该口径在 `core-agent` 保留契约 |
| `cn.aiedge.agent.*` | **保留契约、删实现** | 同上，需与 `AGENTS.md` 的预留范围对齐 |

**执行前提**：删除前对每个包做一次 `git log` 确认是否近期有人在动（避免删掉并行会话正在开发的代码）。

---

## 4. 防再生机制（比删代码更重要）

本轮沉淀的守卫，都在 CI 上，且都做了"该红时确实红"的验证：

| 守卫 | 文件 | 拦什么 |
|---|---|---|
| 装配门禁 | `ComponentScanCoverageTest` | 控制器所在包漏配 `scanBasePackages`（"表能建、点不动"的根因） |
| **鉴权门禁（本轮新增）** | `AuthzAnnotationCoverageTest` | 新增控制器漏加访问控制注解 |
| 产物门禁 | `tools/check-repo-hygiene.sh` + CI `repo-hygiene` 作业 | `.log/.bak/.tmp/__pycache__/playwright-report` 等入库 |
| 接口命中审计 | `tools/audit-endpoint-hits.py` | 用访问日志区分"源码没搜到"与"生产真没人调" |
| 权限码一致性 | `tools/audit-permission-codes.py` | 代码引用了库里不存在的权限码（会锁死功能） |
| 菜单一致性 | `tools/check-menu-targets.py` | 菜单指向不存在的组件（D-01 那类） |
| 孤儿页面 | `tools/audit-orphan-pages.py` + `classify-orphan-pages.py` | 前端有页面、菜单无记录 |

**建议补的最后一块**：把 `check-menu-targets.py` 接进 CI（它现在是手工跑的）。**注意要先修好它的判定口径** —— 它只检查 `component` 字段、不检查 `list_path`，而 `display_mode=1` 的菜单走的是 `list_path` 那条链路（这正是它把 `erp/sale`、`erp/purchase` 误报成孤儿的原因）。直接接入会产生假阳性。

---

## 5. 修订后的收尾范围

### 5.1 已完成的（本轮）
- D-01 坏菜单修复（迁移已写，待启动验证）
- DC-01 旧菜单种子清理（编译通过）
- D-03 授权路径 status 加固（8 处，零行为变化）
- 顺带修复 3 个真缺陷（打印客户端登录、部门审批人解析、权限查询方向）
- 鉴权覆盖率门禁落地（测试 + 基线 + 审计脚本）

### 5.2 待你拍板的（本文 §1，共 7 项）

| # | 决策点 | 我的推荐 | 你只需回答 |
|---|---|---|---|
| 1 | 应收/应付 | **挂菜单**（原判定有误，已纠正） | 同意挂？页面若未接核销/坏账动作，是否本轮补接线？ |
| 2 | 核销中心 | **挂菜单**，标注"仅查询" | 是否本轮补批量核销端点，还是先只读上线？ |
| 3 | 资金流水 vs 查资金 | 视需求二选一 | 是否需要"任意条件导出全部资金流水"的独立入口？ |
| 4 | 盘点两套 | **整合为一个模块两阶段**，先做只读对齐 | 同意先对齐再合并？还是本轮只挂一个、另一个先隐藏？ |
| 5 | 预算 `budget/annual` | **以已挂 80130 为唯一编制入口** | 两页是否都能改预算？是则砍 `budget/annual` |
| 6 | `stock/detail/:id` | **保留隐藏路由** | 无异议即执行 |
| 7 | `member/profile` | **降级为 80300 详情路由** | 同意？ |

### 5.3 孤儿页面「第 1 类删除」的最终范围：**29 项**（原 46 项）

经 §1.8 的写能力复核，范围大幅收缩：

| 处置 | 数量 | 说明 |
|---|---|---|
| **可删** | **29** | 新旧写能力已对齐（含 3 项需先更正"取代页"标注） |
| **不可删，需先补能力或改判挂菜单** | **15** | 详见 §1.8 表格 |
| 改判第 2 类（应收/应付） | 2 | 详见 §1.1 |
| 合计 | 46 | |

**29 项可删清单**（写能力已核对）：
`finance/pre-receipt`、`finance/subject`、`finance/expense-apply`、`finance/expense-pay`、`finance/expense-reimburse`、`finance/report`、`finance/reports`、`finance/capital-flow`、`erp/expense/payment`、`erp/expense/statistics`、`hr/leave`、`hr/performance`、`hr/employee`、`hr/salary`、`wms/receipt`、`wms/inventory`、`erp/mall/banner`、`erp/mall/config`、`erp/mall/order`、`erp/mall/user-audit`、`system/department`、`trade/mall-return`、`charts`、`erp/product/price-batch`、`erp/return`、`erp/sales-analysis`
＋ 3 项需更正标注的：`hr/organization`、`wms/location`、`wms/check`（**前提**：`wh/inventory-order` 的去留先按 §1.4 拍板）

**删除前必做（逐项，不可批量跳过）**：
1. `wms/*` 保留 `form.vue`（仍被在用页面跳转）；
2. **接口层全部保留** —— `api/wms/*`、`supplierApi`、`inventoryApi`、`warehouseApi`、`receivableApi` 等仍被在用的页面复用，删页面不删 api；
3. `wms/check` 的删除与 `wh/inventory-order` 绑定，二者不能同时删；
4. 删除后跑：前端构建 + 相关页面 console 无 error（**搬迁后必须做运行时验证**，这是本项目踩过的坑）+ `AuthzAnnotationCoverageTest`。

**第 4 类（2 项）维持删除**：`erp/pricing/tiers`（后端无 `/erp/pricing/tiers` 映射）、`common/placeholder`（纯占位）。

### 5.4 后移项
D-05 的 13 包裁决、D-06 契约控制器、D-08 权限码冗余、D-09 接口弃用、D-10/D-11/D-12 契约与文档、D-13 孤儿表 30 张、D-15 运行产物 299MB、D-16 历史瘦身、D-17 支付渠道 TODO。

**后移理由**：全部需要外部观察窗口（访问日志 2–4 周）或会改写共享状态（数据库表、git 历史），属"先观察、再标记、后删除"流程，不得与代码删除同批。

---

## 6. 验证记录

| 项 | 命令 | 结果 |
|---|---|---|
| core-base 编译 | `mvn -o -DskipTests -pl core/base/core-base -am compile` | BUILD SUCCESS（子代理实测，356 文件） |
| `DatabaseInitializer` 清理 | `grep -c appendMenu` / `grep -c "MERGE INTO sys_menu"` | 0 处调用、0 处旧种子（仅 Javadoc 提及历史） |
| 文件行数 | `wc -l` | 1768 → 1539 |
| 鉴权审计 | `python tools/audit-authz-coverage.py` | 396 控制器 / 3563 端点 / 242 无鉴权 / 2162 端点 |
| 全量编译 | `mvn -o -DskipTests test-compile` | **BUILD SUCCESS**，6:46，23 个模块全绿 |
| 鉴权门禁（首次运行） | `mvn -o -pl core/api/core-api -am test -Dtest=AuthzAnnotationCoverageTest` | **1/3 失败** → 抓到 `PriceApprovalController` 真实违例，并暴露审计脚本的注释假阴性（见 §2.3 ④） |
| 鉴权门禁（修复后） | 同上 | **Tests run: 3, Failures: 0, Errors: 0** ✅ |
| 基线一致性 | 脚本 `--baseline` 输出 vs 基线文件交叉比对 | 243 == 243，完全一致 |
| 改动 XML 格式 | ElementTree 解析 5 个 mapper | 全部合法 |
| 加固条件核对 | 正则提取 `status` 条件 | 与预期逐条吻合（`m.*`=1，`p./sr./sp./r.`=0） |
| 孤儿页面删除 | 代理执行 34 + 2 个文件；`npx vite build` | **EXIT=0**（6m13s），构建日志中 `Could not resolve` **0 次**；`dynamicRoutes.ts` 清理 48 条 componentMap + 4 条别名改指 |
| 删后菜单完整性 | `check-menu-targets.py` | 坏菜单仍只有 1 条（70063，已由 V11.427.0 修复，待启动生效）——**删除未产生新的坏菜单** |
| 删后 `list_path` 完整性 | 自建脚本复刻 `getComponent` 三级兜底 | 58 条 `list_path` **全部可解析，0 条悬空** |
| 挂载迁移 `V11.428.0` | 事务中试跑 28 条 INSERT 后 ROLLBACK | **28/28 成功**；字段分布 `(status=1, client_type=tenant-admin, menu_type=1)` ×28；60607 资产管理 0→1、61205 打印管理 1→5、60608 预算管理 2→6 |

> 挂载迁移刻意**未手工执行**，devdb 保持原状，实际生效由应用启动时的 Flyway 完成 —— 遵守项目的迁移管理流程，避免 devdb 与 `flyway_schema_history` 脱节。

### 6.1 仍需在启动环境验证（编译与静态检查覆盖不到）

1. `V11.427.0` 生效后：采购换货单「新增/编辑」页面正常渲染、console 无 error；
2. `V11.428.0` 生效后：上述 28 个菜单以 tenant-admin 角色登录可见、可打开、无「页面组件未找到」；重点看 60607 资产管理（原先完全无入口）与 61205 打印管理；
3. 授权加固后：以**非超管**账号登录，菜单/权限/角色显示与加固前一致（预期零变化）；
4. 打印客户端用**已停用账号**登录 → 应被拒（403）。

---

## 7. 本轮未完成、需后续开发的任务

以下四项**不是清理工作，是功能开发**，因此本轮只登记、不动手。每项都给了验收标准，可直接派工。

### 7.1 盘点两套的整合（对应 §1.4，前端开发）

**现状**：`wh/inventory-order`（`checkApi`，`/wms/check/*`，含点位/差异明细、可跳 `wms/check/form`）与菜单 5003「盘点单」（`stockTakeApi`）是两套并行实现。经复核，`wh/inventory-order` 的能力是 `wms/check` 的**超集**（另有 `submitResult`/`cancelCheck`/`saveDetails`）。

**为什么本轮没做**：这是页面合并，会改动正在被使用的功能，风险高于"删冗余"。

**建议落法（对标 SAP EWM 的三阶段）**：
1. 以菜单 5003「盘点单」为**单据层**（审批 + 库存变动）；
2. 把 `wh/inventory-order` 的作业明细作为其**执行阶段视图**（Tab 或子页），复用 `checkApi` 的数据，不做数据迁移；
3. 对齐后删除重复的列表入口与冗余接口（`api/wms/check.ts` 若仍被 form 使用则保留）。

**验收**：同一张盘点单能走完「准备（生成作业）→ 执行（录入实盘/复盘）→ 审批（库存变动 + 可追溯审计）」；`wms/check/form.vue` 仍可达；两处入口不再并存。

> ⚠️ **`wh/inventory-order` 与 `wms/check/index.vue` 的去留是绑定的**：`wms/check/index.vue` 已被删除，但 `wms/check/form.vue` 被保留（仍被 `wh/inventory-order` 跳转）。若后续决定连 `wh/inventory-order` 一起删，必须先把它的能力搬到 5003。

### 7.2 15 项页面的写能力补齐（对应 §1.8，前端开发）

这些页面**本轮刻意未删**（删了会丢功能）。二选一：把独有写能力搬到已挂菜单的新页面（推荐），或改判第 2 类挂菜单。

| # | 旧页面 | 要补的能力 | 补到哪 | 备注 |
|---|---|---|---|---|
| 1 | `finance/receipt/index.vue` | 完成收款 `complete` | `finance/receipt-doc` | 后端 `POST /{id}/complete` 已存在 |
| 2 | `finance/payment/index.vue` | 完成付款 `complete` | `finance/payment-doc` | 同上 |
| 3 | `finance/pre-payment/index.vue` | 预付转付款 `offsetToPayment` | `finance/advance-payment` | 状态文案已有「已冲抵」，缺入口 |
| 4 | `erp/expense/application/index.vue` | 删除草稿 | `finance/expense-doc` | api 层已有 `expenseDocApi.delete` |
| 5 | `erp/expense/reimbursement/index.vue` | 删除草稿 | `finance/expense-doc` | 同上 |
| 6 | `erp/expense/approval/index.vue` | 审批**退回**（RETURN） | `finance/expense-approval` | 需先扩 api 枚举（现仅 APPROVE/REJECT） |
| 7 | `erp/mall/product/index.vue` | 删除商品（单个 + 批量） | `mall/product-shelf` | 该页现无任何 delete |
| 8 | `erp/shipment/index.vue` | 审核 `approve`、出库 `ship` | `sales/outbound` | api 方法已存在，只差 UI 挂载 |
| 9 | `order-center/index.vue` | 采购单 `submit/approve/cancel/delete`；销售单 `submit/batchDelete` | `sales/order-center` | **采购侧整体缺失**，工作量最大 |
| 10 | `erp/partner/index.vue` | 地址簿/银行账户/标签/多联系人 CRUD | `md/partner` | 现仅单主联系人 |
| 11 | `erp/stock/replenishment/index.vue` | 生成建议/忽略/创建采购单 | `purchase/smart-replenish` | 后端三个端点齐全，新页只读 |
| 12 | `erp/stock/index.vue` | 冻结/解冻库存 | `analytics/check-stock` | 后端 `StockController:58/71` |
| 13 | `supplier/index.vue` | 激活/禁用供应商门户 | `md/supplier` | 后端两端点齐全 |
| 14 | `wms/warehouse/index.vue` | `/wms/warehouse` 主数据 CRUD | 待定 | ⚠️ **需业务先拍板**：`wms_warehouse` 与 `erp_warehouse` 是否两套并存（是两套独立后端资源） |
| 15 | `hr/attendance/index.vue` | 签到/签退 | `hr/attendance/list.vue` | 低危：旧实现硬编码 `employeeId=0` **写脏数据**，新页已注明"待后端按登录人解析员工"。建议先修后端再迁 UI |

**验收**：每补完一项，从其旧页面删除对应入口；全部补完后，旧页面方可删除。

### 7.3 鉴权注解分批补（对应 §2.4）

门禁与基线已就位（243 行棘轮）。后续按批次推进，**每批必须先补权限码种子再补注解**：

| 批次 | 范围 | 理由 |
|---|---|---|
| 1 | `erp.finance.*` / `erp.payment.*` / `erp.invoice.*` / `erp.budget.*` | 涉及金额，越权后果最重 |
| 2 | `crm.*`（8 个控制器） | 整模块零鉴权，含客户主数据 |
| 3 | `erp.stock.*` | 采购/库存/销售单据 |
| 4 | `wms.*` / PDA | 需先定 PDA 设备鉴权策略 |
| 5 | `erp.marketing.*` / `erp.b2b.*` | 含 C 端公开接口，需逐个甄别 |

**另需单独处理 1 项**：`erp.pricing.controller.PriceApprovalController`（价格审批）—— 类注释声明是"口径与页面对齐"而刻意不加，但**价格审批涉及定价，建议补**。补前须建权限码种子。

### 7.4 未装配 13 包的裁决执行（对应 §3）

推荐已给出（多数删除、`agent`/`knowledge` 保留契约删实现）。执行前对每个包做一次 `git log` 确认近期无人在动。

### 7.5 后端 30 张孤儿表（D-13）

流程：观察 `pg_stat_user_tables` 1–2 个发布周期 → 改名 `zz_deprecated_*` → 再观察一个周期 → drop。**前置：确认备份可用**。数据不可回滚，代码可回滚。

> 注意与并行会话的 `TABLE_DUPLICATE_AUDIT.md`（72 张孤儿表 / 553 张表分档）交叉核对后再动 —— 两份清单口径不同，取并集前先对齐。

**仍需在启动环境验证的**（编译无法覆盖）：
1. `V11.427.0` 迁移生效后，采购换货单「新增/编辑」页面正常且 console 无 error；
2. 授权加固后，以**非超管**账号登录，菜单/权限/角色显示与加固前一致（预期零变化）；
3. 打印客户端用**已停用账号**登录 → 应被拒（403）。
