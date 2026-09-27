# 设置模块 · 全栈审计报告（2026-09-24）

> **范围**：前端 `views/set/**`（13 页）+ `views/workflow/**`（4 页）+ `views/printing/**`（5 页，随 `61205 打印管理` 挂在本模块子树下）；
> 后端 `core/base`、`core/api`、`core/payment`、`erp/erp-printing`、`erp/erp-finance`、`erp/erp-stock`、工作流相关；
> 数据库 `sys_menu` 设置子树（60012，实测 **44 行 = 1 顶级 + 8 分组 + 26 叶子 + 9 按钮**）+ `sys_permission` 中 `set:%`/`workflow:%`/`print:%` + 本模块涉及的 35 张表。
> **对标依据**：`docs/Yh-Spec/手动整理对标开发文档/设置模块/`（19 篇）与 `.../系统菜单设计与管理/`（5 篇）。
>
> **本次审计为只读**：未改动任何业务代码、未改动任何库表数据（所有 SQL 均为 SELECT）。审计过程中未在仓库留下临时文件。
> 证据口径：`file:line` 为审计时刻实测行号；SQL 均可用 `psycopg2` 连 `localhost:5432/devdb`（devuser）复现。

---

## 0. 结论摘要

| 维度 | 结论 |
|---|---|
| 接口接线（前端 → 后端） | ⚠️ 设置 13 页 **1 条真断链**（会计期间「新增期间」尾斜杠）；工作流 25 条路径 **0 断链**；打印 4 页 REST 路径 **0 断链**（但权限码体系错位，见 P0-2） |
| 菜单连通性 | ✅ 26 个叶子的 `component` **全部解析到真实 `.vue`，0 孤儿**（含跨域复用的 3 个）；空分组 **0 个**（`61205` 已有 5 子菜单） |
| 数据落库真实性 | ✅ 文档点名的「写内存/写缓存」两处（系统参数、支付配置）**确已改真落库**；DB 实测 `sys_config=173`、`sys_tenant_profile=3`、`sys_tenant_module=28`、`set_print_config=1` |
| 后端鉴权覆盖 | ✅ 设置模块**在用**控制器的写端点 **100% 带 `@SaCheckPermission`**，未发现裸奔写端点 |
| 桩代码 | ✅ 设置 13 页 + 工作流 4 页 **无 TODO/mock/写死假数据/`console.log`**，无「catch 吞异常仍提示成功」 |
| **非超管可用性** | 🔴 **形同虚设**：26 个页面中**仅「会计期间」1 个可用**（三重障碍：10 条菜单 `menu_level=3` 被过滤 + 10 条 `fail-closed` 菜单消失 + 16 条可见但 403）—— **P0-1** → **✅ 已修（2026-09-26）**，见 §6.5 |
| **打印管理组** | 🔴 前端 18 处权限码 `printing:*` 在库中 **0 行**（后端实为 `print:*`）→ 非超管**操作按钮全隐 + 接口 403** —— **P0-2** → **✅ 已修（并行会话，本次复核确认）** |
| 跨模块关系 | 🔴 **4 处「配置了没人读」**：企业信息档案 16 列、财务期初、`sys_config` 系统参数、支付配置多数键 —— **P1-2~P1-6** |
| 死代码 / 冗余 | ⚠️ 配置中心 ×2、日志实现 ×5、任务控制器 ×2、打印 v1/v2 并存；工作流 `workflowDefinitionApi` 整对象 + 4 个枚举 + 1 个孤儿 `.vue.test` + 1 个无派发方的跨页事件；后端 11 条无引用端点 |
| 与开发文档的差异 | ⚠️ **文档快照已被后续迁移大面积推翻**：README §2.1 记 7 组/18 页 vs 实测 8 组/26 叶；§10.5-3 记 printing 5 页菜单 0 行 vs 实测 4 个菜单已挂出 |

**P0 三条、P1 十条、P2 十八条**，逐条见下。另有正面结论 §4（可作回归基线）与文档回写清单 §5。

---

## 1. P0

### P0-1 🔴 设置模块对非超管几乎完全不可用（26 页中 25 页打不开）

**现象**：用 `SYSTEM_ADMIN`（385 条权限）或 `DEPT_ADMIN`（175 条权限）登录，进入「设置」域，26 个页面里只有「会计期间」能用。

**根因：菜单可见性派生规则 × 权限码只发超管，两条规则叠出两种坏结果**

菜单可见性自 2026-09-21 起由权限码派生（`SysMenuServiceImpl.getUserMegaMenus`，`core-base/.../service/impl/SysMenuServiceImpl.java:288-350`），命中规则是前缀匹配（`MenuPermissionDeriver.java:99-120`）：

```java
// SysMenuServiceImpl.java:339-348  过渡口径原文
if (menuCode != null && !menuCode.isEmpty()
        && knownMenuCodes.contains(menuCode)      // ① 权限码库"覆盖"了这个菜单码
        && !heldMenuCodes.contains(menuCode)) {   // ② 但该用户不持有
    blockedByPermission++;
    continue;                                     // → 隐藏
}
```

对 26 个叶子逐一判定（`knownMenuCodes` = 全库有效权限码的前缀展开集合）：

| 分桶 | 数量 | 非超管看到的结果 |
|---|:-:|---|
| **① 被覆盖**（`menu_code` 恰是某权限码的前缀）→ fail-closed | **10** | **菜单从导航消失** |
| **② 未覆盖** → fail-open 保持可见 | **16** | **菜单在，点进去接口 403** |

**① 菜单消失的 10 个**（`menu_code` ∈ 权限前缀集，而 `set:*` 全部只授 `role_id=1`）：

```
80620 菜单配置 set:menu-config      80624 企业信息 set:company-info      80625 应用中心 set:app-center
70550 库存期初 set:initial-stock    70551 财务期初 set:initial-finance    70560 系统重建 set:rebuild
70561 系统任务 set:system-task      80930 打印设置 set:print-config      80146 辅助核算 finance:auxiliary
81016 协议列表 agreement
```

**② 可见但 403 的 16 个**（`menu_code` 与权限码前缀错配，例如菜单 `801 workflow-definition` 而权限码是 `workflow:definition:*`（冒号式）；菜单 `80621 set:sys-params` 而实际接口要 `system:config:list`）：

```
80621 系统参数  80622 审核设置  80623 支付配置  80626 外链同步  80630 操作日志  70570 会计期间*
81000-81003 打印模板/链路/客户端/任务
801 流程定义  80610 流程设计  80611 流程分析  802 流程实例  803 我的待办  804 我的已办
```
> `*` 会计期间是唯一例外：`finance:period:*` 恰好授给了 SYSTEM_ADMIN / DEPT_ADMIN，故「可见 + 接口放行」= **唯一可用的页面**。

**③ 还有第三重障碍：10 条菜单 `menu_level=3`，普通租户根本取不到**（本节为 2026-09-26 补记）

`getUserMegaMenus` 对「非系统租户且非超管」还有一行硬过滤：`wrapper.eq(SysMenu::getMenuLevel, 0)`（`SysMenuServiceImpl.java:275-278`）。而设置子树 26 个叶子里**恰好有 10 条是 `menu_level=3`**：

```
80610 流程设计  80620 菜单配置  80621 系统参数  80622 审核设置  80623 支付配置
80624 企业信息  80625 应用中心  80626 外链同步  80630 操作日志  80930 打印设置
```

⇒ 对**非系统租户**（`tenant_id ≠ 1`）的租户管理员，这 10 页**无论授什么码都不会下发**。这是记忆里反复出现的同型问题（分析 29 条 V11.499.0 / CRM V11.500.0 / 配送 20 条 V11.501.0 / HR 4 条 V11.502.0），**设置模块是漏网的一组**。`dev` 的 admin 同时命中「系统租户 + 超管」双豁免，本地永远看不出来。

**权限侧证据**（devdb 实测）：

```sql
select r.role_code, count(rp.id) from sys_role r
  left join sys_role_permission rp on rp.role_id = r.id group by 1;
-- SUPER_ADMIN 1835 | SYSTEM_ADMIN 385 | DEPT_ADMIN 175 | E2E_T2_ADMIN 4 | ROLE_MQD2X2DI 0

select case when p.permission_code like 'set:%' then 'set' else 'workflow' end k,
       string_agg(distinct rp.role_id::text, ',') roles, count(*)
  from sys_permission p left join sys_role_permission rp on rp.permission_id = p.id
 where p.permission_code like 'set:%' or p.permission_code like 'workflow%' group by 1;
-- set      | 1 | 18
-- workflow | 1 | 19        ← 37 条码全部只授 role_id=1

-- SYSTEM_ADMIN / DEPT_ADMIN 在设置模块范围内的相关权限码：
--   仅 finance:period:create / :update / :view 三条，其余为 0
```

**为什么这是本次的头号问题**：开发文档 §10.3 把「`workflow:*` 权限码 0 行」当作已修（落库 + 授 role 1 即算完成），但**只授超管**这一条，叠加菜单可见性派生后，等于把整个设置域收归超管。收尾阶段若客户环境用租户管理员账号，设置模块（含刚做完的企业信息、应用中心、系统重建、系统任务、期初、打印设置）**全部不可见或不可用**。

**处置建议（二选一，需拍板）**：
- **A（推荐）**：给 `SYSTEM_ADMIN` / `DEPT_ADMIN` 补授设置域应有的码（按页勾选，不是全给）。同时把 6 个连字符式 `menu_code` 改成与权限码同前缀（见 P1-1 的错配清单），使「可见 = 可用」。
- **B**：若产品口径就是「设置域只给超管」，则应把 16 个 `未覆盖 → fail-open` 的菜单显式置 `visible=0`（或补一个 `set:*` 前缀码使其 fail-closed），避免「看得见、点开 403」的坏体验。

> ⚠️ 本报告只给裁定建议，**未改任何库数据**（`sys_menu`/`sys_role_permission` 属系统配置数据，按项目纪律须先出方案获批）。

---

### P0-2 🔴 打印管理组 18 处权限码是「幽灵码」，非超管操作按钮全隐

**范围**：`views/printing/{template,chain,client,task,designer}/index.vue`（菜单 81000-81003 挂在设置 → `61205 打印管理` 下，属本模块）。

**事实**：前端 18 处 `v-permission="'printing:xxx'"`，而

```sql
select count(*) from sys_permission where permission_code like 'printing:%';  -- 0
select count(*) from sys_permission where permission_code like 'print:%';      -- 54
```
- 后端 `erp-printing` 的真实权限码是 **`print:` 前缀**（`print:template:publish`、`print:client:update`、`print:task:cancel`…）；
- 权限指令对未持有的码置 `display:none`（`directives/permission.ts:49`），仅 `'*'`（超管）放行；
- 这 18 个 `printing:*` 码在 `sys_permission` 里**不存在**，因此在权限管理页**根本建不出、也无从授予**。

**后果**：非超管在打印 4 页上「菜单可见（fail-open）、表格加载 403、**所有行内与工具栏操作按钮全部不显示**」；未加 `v-permission` 的「新增模板 / 新增打印链 / 新增客户端」按钮虽可见，点开提交必 403。

**逐条映射**（前端码 → 后端真实码，端点均存在）：

| 前端 `v-permission`（文件:行） | 后端真实码 | 端点 |
|---|---|---|
| `views/printing/template/index.vue:113/122/131/139` | `print:template:update/publish/copy/delete` | `PrintTemplateV2Controller:111/202/217/188` |
| `views/printing/chain/index.vue:83/98` | `print:chain:update/delete` | `PrintChainController:78/130` |
| `views/printing/client/index.vue:191/199` | `print:client:update/delete` | `PrintClientController:119/110` |
| `views/printing/task/index.vue:54/231/240/250/259` | `print:task:execute/detail/cancel/confirm`、`print:screenshot:retry` | `PrintTaskV2Controller:32/43/82/90`、`ScreenshotController:73` |
| `views/printing/designer/index.vue:80/89/95` | `print:format:view`、`print:template:create/update/publish` | `FormatController:44`、`PrintTemplateV2Controller:71/111/202` |

**定性**：17 条是**纯前端笔误**（后端端点与权限码都齐备），1 条（`printing:client:copy`）是纯前端剪贴板动作、本不需要后端码。**修法极简：把 18 处 `printing:` 批量改为 `print:`**（并给业务角色授 `print:*`，否则仍是超管专属）。

> 这是文档 §7.1 P0-16「`workflow:*` 权限码 0 行」的**同类漏网**：当时只扫了工作流前缀，没扫打印前缀。

---

### P0-3 🔴 会计期间「新增期间」请求带尾斜杠 → 永远 404

**证据**：

```ts
// frontend/apps/pc-admin/src/api/finance/index.ts:1116-1120
/** 新增会计期间（后端 @PostMapping("/")，需带尾部斜杠） */
create: (data: { periodYear: number; periodMonth: number; remark?: string }) =>
  request.post('/erp/finance/period/', data),      // ← 带尾斜杠
```
```java
// backend/erp/erp-finance/.../AccountingPeriodController.java:28,55-56
@RequestMapping("/api/erp/finance/period")
...
@PostMapping                                        // ← 无 value，真实路径不带尾斜杠
public Result<AccountingPeriodDTO> create(@Valid @RequestBody AccountingPeriodDTO dto) {
```
- 请求拼接不做尾斜杠规整：`const url = \`${config.baseURL || ''}${config.url || ''}\``（`utils/request.ts:107`）→ 实际发出 `/api/erp/finance/period/`；
- Spring Boot 3.2.5 默认**不做**尾斜杠匹配（全仓无 `setUseTrailingSlashMatch`，仅 `HealthCheckConfig.java:25-27` 空实现）；
- 运行期探针佐证：`GET /api/erp/finance/period` → 405（命中映射，提示改用 POST）；`GET /api/erp/finance/period/` → 401（未命中控制器）。

**后果**：`accounting-period/index.vue:461` 的「新增期间」提交匹配不到控制器 → 404，`:466` 的「会计期间创建成功」永远走不到。**该按钮不可用**。

**修法**：`request.post('/erp/finance/period', data)`（去掉尾斜杠），并订正那行错误注释。建议同时加一条静态门禁：扫全仓 `request.*('/.../')` 的尾斜杠写法（全 api 层仅 2 处，另一处 `api/finance/index.ts:507` 是字符串拼接，无风险）。

---

## 2. P1

### P1-1 ⚠️ 打印设置的行为配置对非超管整体失效，且每次弹「拒绝访问」

`PrintConfigController.getBehaviorConfig()`（`backend/erp/erp-printing/.../PrintConfigController.java:116-118`）：

```java
@SaCheckPermission("set:print-config:view")   // ← 实际要求管理权限
@GetMapping("/behavior")
public ResponseEntity<Map<String, Object>> getBehaviorConfig() {
```

而同一个文件的 Javadoc（`:45`、`:104-110`）与前端（`components/PrintDialog/printBehavior.ts:52-54`、`api/set/print-config.ts:90-91`）**三处都声称该端点「无需管理权限、任何已登录用户可读」**——Javadoc 里甚至专门论证了「若带权限，普通账号会 403 并被拦截器弹成全局提示，配置对这些人永远不生效」。

**实际后果**（与该设计意图完全相反）：
1. `set:print-config:view` 只授 `role_id=1` → 非超管调 `/behavior` 必 403；
2. `GlobalExceptionHandler.java:64-68` 返回 403 → `utils/request.ts:331-332` 执行 `message.error('拒绝访问')` → **非超管每 5 分钟（缓存 TTL）打开任一含 `PrintDialog` 的页面都会被弹一次**；
3. `printBehavior.ts:71-75` 静默降级为 `null`，调用方按「沿用改造前行为」处理 ⇒ **5 项配置全部不生效**：`remoteEnabled=false` 被无视（`PrintDialog/index.vue:234`）、草稿打印门控失效、小数位格式化与「打印内容」不注入、助手跳过预览失效。

**修法**：删掉 `:118` 那行注解（与 Javadoc/前端假定一致）。该端点的读租户来自服务端会话、只回行为字段，去掉管理码不引入越权。

**附带 P2**：`resolveTenantId()` 无会话时回落 `1L`（`PrintConfigController.java:199-201`）；`GET` 走 `getOrCreate` 导致**读操作会建行**（`:98`、`:123`）。

---

### P1-2 ⚠️ 企业信息档案「配了没人读」——16 个字段零跨模块消费

`sys_tenant_profile` 的 16 个业务列（LOGO、统一社会信用代码、法人、注册地址、开户行、税号…）全仓读取点只有一处：`CompanyProfileVO.java:163-178`（设置页把自己写的数据读回来）。

```bash
# 引用 SysTenantProfile / sys_tenant_profile 的后端文件（全部 6 个）
TenantController.java  CompanyProfileVO.java  TenantProfileService.java
SysTenant.java  SysTenantProfile.java  SysTenantProfileMapper.java
# grep logoUrl/logo_url 后端：仅 CompanyProfileDTO/CompanyProfileVO/TenantProfileService/SysTenantProfile
```

- **被消费的只有主表 `tenant_name`**：应用中心（`SetAppCenterController.java:114`）、协议当事人（`AgreementMapper.java:44`）、打印客户端登录（`ClientAuthController.java:49`）、登录态（`AuthController.java:245`）；
- **未消费**：打印模板抬头、单据页眉、报表页眉、导出。打印模板里的 `{{companyName}}` 实为**客户**名称（`views/printing/seed-templates.ts:1129`）。

⇒ 用户在「企业信息」里配的 LOGO、税号、开户行**不会出现在任何单据或打印输出上**。文档 §10.5-11 只把 LOGO 记为「已做（能上传能落库）」，未指出**落库后无消费方**。

---

### P1-3 ⚠️ 财务期初是孤岛：只落库，不进任何账务报表

`erp_initial_finance_subject` / `erp_initial_finance_partner`（两表均 **0 行**）的唯一读取方是录入页自己的试算平衡（`InitialFinanceServiceImpl.java:447-451`）。

| 下游报表 | 实际取数来源 | 是否 JOIN 期初表 |
|---|---|---|
| 试算平衡 | `finance_voucher_item JOIN finance_voucher`（`TrialBalanceMapper.java:38-39`） | ❌ |
| 资产负债表 | `finance_ledger`（`FinancialReportServiceImpl.java:264-275`） | ❌ |
| 往来账 | （`PartnerLedgerServiceImpl.java:36-38`） | ❌ |

⇒ 财务期初录入的数据**不产生任何账务效果**。文档 §11.3 记「与库存期初对平已做」（属实，`InitialFinanceServiceImpl.java:491-508`），但未指出**期初余额不进报表**这一更大的缺口。

---

### P1-4 ⚠️ `sys_config`（系统参数页）无任何业务消费方

**存在两套互不相通的配置中心**：

| 表 | 服务 | 入口 | 业务消费方 |
|---|---|---|---|
| `sys_config`（173 行，8 个导航分组） | core-api `SystemConfigService` | `/api/config`（设置 → 系统参数，**在用**） | **仅自身包内 5 个文件**（controller/service/mapper/model），**无任何 `erp-*` 模块注入** |
| `sys_project_config`（22 行） | core-base `SysConfigService`（`@Primary`，`SysConfigServiceImpl.java:66`） | `/api/system/config`（**死接口**，零调用方） | 仅 2 处：费用审批人（`ExpenseApprovalServiceImpl.java:318`）、营销频控（`AutoCampaignServiceImpl.java:244-245`） |

`SystemConfigService` 全仓引用只有自身包内 5 处；种子键 `industry.*` 亦无消费者。⇒ **系统参数页改任何值都不影响任何业务行为**（真实生效的少量开关躺在另一张表里）。

**同族 P2**：core-base 的 `SysConfigController`（`/api/system/config`）是**无调用方的死接口**，且两套共用 `system:config:*` 权限码，而 `sys_permission.api_path` 只登记了 `/api/config` —— 即便被调用也过不了权限矩阵。

---

### P1-5 ⚠️ 会计期间校验覆盖面不足 + 口径不一致

`AccountingPeriod` 的消费**只在 `erp-finance` 内部**：

| 动作 | 是否校验关账期间 | 证据 |
|---|:-:|---|
| 凭证新增 | ✅ | `VoucherServiceImpl.java:83` |
| 凭证修改 | ✅ | `:147` |
| 凭证红冲 | ✅ | `:291` |
| **凭证审核** | ❌ | `:225`（无 `assertPeriodOpen`） |
| **凭证过账** | ❌ | `:248`（无 `assertPeriodOpen`） |
| 库存/出入库/采购/销售单据 | ❌ **零引用** | `grep AccountingPeriod backend/erp/{erp-stock,erp-purchase,erp-sales}` → **0 命中** |

**口径不一致**（同一「已关账」在不同入口判定不同）：凭证按 `fiscal_year + fiscal_period`→单期间（`:123-130`）；财务期初按 `periodYear` **整年**统计（`InitialFinanceServiceImpl.java:607/618`）；`currentYear` 按日期区间（`:563`）；**期间行不存在时静默放行**（`VoucherServiceImpl.java:129` 的 `ifPresent`）；租户取法亦不同（`AccountingPeriodServiceImpl.java:41` 写死回落 1 vs `InitialFinanceServiceImpl.java:639` 走租户插件）。

**同族 P2**：`LedgerServiceImpl.java:482` 的 `closePeriod` 不写 `fin_accounting_period.status`，且全仓无调用方（死代码）——存在两套「结账」。

---

### P1-6 ⚠️ 支付配置多数键无人读；发起支付仍是桩

- **真实消费方只有一条链**：`TenantChannelCredentialReader.java:49-61` 直查 `sys_project_config` → 供三个回调验签器（`AlipayCallbackVerifier.java:60/136`、`WechatCallbackVerifier.java:66/248`、`UnionPayCallbackVerifier.java:69/248`）；
- **无消费方**：`payment.wechat.appId/appSecret`、`payment.scene.*`、`payment.refund.autoRefundEnabled`；
- **发起支付未实现**：`MallPaymentServiceImpl.java:64-72` 的 `createPayment` 只打日志；`AlipayChannel.java:26-30` 明写 `// TODO 调用支付宝/微信 API`。

⇒ 「支付配置」页填的渠道参数**只在回调验签时被用到**，不影响发起支付（因为发起支付本身还没接）。

---

### P1-7 ⚠️ 设置页 `v-permission` 覆盖严重不足（8/13 页零覆盖）

零 `v-permission` 的页面：`menu-config`、`sys-params`（3 个文件）、`audit-config`（2 个文件）、`initial-stock`、`initial-finance`、`app-center`。

**缓解事实（重要，避免误判为越权）**：菜单可见性由权限码派生（见 P0-1），**能看见菜单者必持有对应 `*:view/list` 码**；风险限于「持有 `list` 码但无 `update` 码的用户仍看到写按钮 → 点一次 403」，**不是整页越权**。后端写端点注解 100% 覆盖，未发现真正的越权写。

**对照**：`payment-config`（5 处）、`operation-log`、`system-task`、`accounting-period`、`print-config` 是正确的正面样例。

---

### P1-8 ⚠️ 平行/冗余实现堆积

| 能力 | 实现数 | 明细 | 在用 |
|---|:-:|---|---|
| 系统配置 | **2** | core-api `SystemConfigService`（`/api/config`）vs core-base `SysConfigService`（`/api/system/config`） | 前者 |
| 操作日志 | **5** | `/api/log`（活）、`/api/system/log`、`/api/logs`、`/api/system/log/advanced`、`/api/system/log/structured` | 仅 `/api/log` |
| 打印模板 | **2** | `sys_print_template`(v2) vs `erp_print_template`(v1) | 两表均 0 行 |
| 任务 | **2** | `SystemTaskController`（台账，`/api/set/system-task`）vs `ScheduledTaskController`（配置，`/api/scheduler/task`） | 均活，命名易混 |
| 部门 | **2** | `sys_department`(5 行) vs `sys_dept`(0 行) | 前者 |
| 结账 | **2** | `MonthClosingServiceImpl` vs `LedgerServiceImpl.closePeriod`（死） | 前者 |

除日志有「保留并登记」的裁定（`LogManageController` 类注释）外，其余均无裁定，属可清理债务。**`LogManageController` 自身仍硬编码 `DEFAULT_TENANT_ID = 1L`**（`:40`），一旦被复用即跨租户。

---

### P1-9 ⚠️ 工作流：契约与 UI 的「看起来有、其实没有」

1. **审批模式（会签/或签）是未完成的实现骨架**：前端 `APPROVE_MODE_MAP` 提供「会签（全部通过）」（`api/workflow/index.ts:387-391`），但后端 `approveMode` **全仓仅 1 处 `setVariable`、0 处读取**（`WorkflowEngineImpl.java:134` 把它塞进 `ExecutionContext` 后无人消费；`workflow_node` 表亦**无 `approve_mode` 列**——它只存在于流程定义的 JSON 里）。`WorkflowServiceImpl.approve()`（`:405-457`）在 `completeUserTask` 之后直接 `resolveNextNode` 推进，**不检查同节点是否还有 PENDING 兄弟任务** ⇒ 实际恒为「或签」。UI 宣称的「会签」不成立。（2026-09-26 复核：论断成立，证据已精确化）
2. **审批人类型 4 vs 6**：后端支持 `user/role/applicant_self/leader/dept_leader/dept`（`WorkflowServiceImpl.java:1620-1626`），前端只给 4 项 ⇒ `leader`、`dept` **无 UI 入口**。
3. **流程定义台账的筛选/分页是客户端做的**：`designer/index.vue:703-709` 只对已拉取列表按 `name/enabled` 过滤，分页在页面侧切（`:715`）——只有 `type` 真正下发后端。功能可用，但「筛选透传后端」的说法不成立。
4. **优先级筛选功能性死亡**：`workflow_task.priority` 30 行**全为 NULL**（`V11.405.0` 加的可空列），后端按严格 `eq` 过滤（`WorkflowServiceImpl.java:880-882`）⇒ 选「高/中/低」恒返回 0 行。文档 §10.5-18 已如实登记（未闭环），核实为**真**。
5. ~~`task/stat` 短路分支缺键~~ **【2026-09-26 复核：子代理误判，已证伪】** 实读 `WorkflowServiceImpl.java:787-789`，短路分支遍历的正是 `TASK_STAT_KEYS`，而该常量（`:833-838`）**已含全部 19 个键**（含 `actionApprove/actionReject/actionReturn/actionTransfer/...`）⇒ 该分支同样会写出全部 action 键，**不存在「四张卡恒显 0」**。此条撤销。

**正面**：文档 §10.3 P0-14（804 进错 Tab）、P0-15（801/80610 同一界面）**代码层已真修**——`task-management.vue:686-703` 与 `designer/index.vue:623-637` 均按路由分流，标题与 Tab 初值随之变化。工作流 25 条 API 路径 **0 断链**，36 个端点权限注解 **100% 覆盖**。

> ⚠️ **分流无回归护栏**：801/803/804 的形态分流**依赖 `sys_menu.route_name` 保持为 NULL**（即 `route.name === menu_code`）。一旦有人在菜单配置页写入 `route_name`，三处会静默落到错误形态，且无单测/断言保护。

---

### P1-10 ⚠️ 开发文档快照已被后续迁移大面积推翻（需回写）

| 文档位置 | 文档声称 | 实测 | 判定 |
|---|---|---|---|
| §2.1 | 7 个分组 / 18 个页面项 | **8 个分组 / 26 个叶子**（新增 80626 外链同步、80146 辅助核算、81000-81003 打印×4、81016 协议列表 + 新分组 61208） | **过期** |
| §10.5-3 / §11.7 | `printing/{template,chain,client,task,designer}` 5 页在 `sys_menu` **0 行** | 81000-81003 **4 个菜单已挂出且 active**；仅 `designer` 仍无菜单 | **过期**（据此写「不造假入口」的结论已不成立） |
| §7.1 P0-16 / §10.3 | `workflow:*` 权限码 **13 个** / 0 行 | **19 行**（`V11.398/403/404/405/417/464`）；`sys_permission` 全表 **1922 行**（文档记 264） | **数字过期** |
| §11.1 | `GET /behavior` **无权限注解** | 实际**有** `@SaCheckPermission("set:print-config:view")`（`:118`） | **与代码相反** |
| §10.5-17 | 「退回只把目标节点写进备注，真正回退未实现」 | `returnTask` 已实现真实节点回退（`WorkflowServiceImpl.java:1166-1223`） | **过期** |
| §7.3 | `workflow:create` 跨页事件「全组 3 页监听」 | 现仅 **1 页**监听（`process-analysis.vue:529`），仍无派发方 | **描述过期** |
| §7.3 | `componentMap` 冗余注册在 `dynamicRoutes.ts:119-120` | 行号已漂移；冗余现象转移到 `:1481`/`:1500` 的 `requiredRoutes` | **行号漂移** |
| §2.1 问题#2 | `61201` 的 sort=3 空缺 | 现由 **80626 外链同步**（sort=3）占位 | **已消失** |
| §2.1 问题#1 | `61205` 空分组 | 已有 5 个子菜单 | **已消失（文档本身也标了已修）** |

**建议**：README 加一节「文档快照与迁移的对账时点」，或（更好）把 §2.1/§2.2/§7 标为「历史快照，以 devdb 实测为准」，避免后续验收按过期清单漏验 7 个页面。

---

## 3. P2

| # | 项 | 证据 |
|:-:|---|---|
| P2-1 | **`61203` 内 `sort` 重号**：`70560 系统重建` 与 `80630 操作日志` 均 `sort=2` | `group by parent_id,sort having count(*)>1` → `61203\|2\|2` |
| P2-2 | **`804 我的已办` component 带 `.vue` 后缀**（同组 802/803 不带） | DB 实测；归一化规则能兜住，不影响渲染。~~「`80610 path` 无前导斜杠」系文档记反~~ —— 全库 301 条有值 path 中仅 5 条带前导斜杠（801/802/803/804/80611），**无前导斜杠才是本项目主流写法**，故不改；`80610 menu_level=3` 已并入 P0-1 ③ 处理 |
| P2-3 | **`menu_code` 命名不规范 6/26**：`workflow-definition`、`workflow-instance`、`my-task`、`my-done`、`agreement`（无域前缀）、`finance:auxiliary`（前缀与所属「财务设置」组不符）。规范见 `mega-menu-redesign.md:617`（`域:短横线名词`） | 这 6 个正是 P0-1 中「fail-open 可见但 403」的成因 |
| P2-4 | **`804` / `802` / `803` 的 menu_code 与权限码错配**：菜单 `my-task`，接口要 `workflow:task:view` | 同 P0-1 ② 桶 |
| P2-5 | **`6130701 菜单管理` 是 `tenant_id=1` 的死数据**：`SysMenuServiceImpl.java:198/212/244` 硬编码 `.eq(SysMenu::getTenantId, 0L)`，该行永不被返回；且 `client_type='tenant-admin'` 却挂在 system-admin 域 | 文档 §10.4-2 记它会污染菜单配置页分组面板（已加排除） |
| P2-6 | **`sys_permission` 实体缺 3 列**：表有 `create_by`/`update_by`/`remark`，`SysPermission.java:25-108` 无对应字段 | 新建/更新权限时这 3 列恒 NULL |
| P2-7 | **`sys_role_menu` 仅 3 行**且全属 role 1 / tenant 1：授权配置点（角色管理页仍写它）与生效点（权限码派生）**不一致** | `select * from sys_role_menu` → 3 行；用户改了不生效 |
| P2-8 | **9 张空表**：`sys_print_template`、`erp_print_template`、`sys_print_chain`、`sys_print_chain_item`、`sys_print_client`、`sys_print_task`、`sys_audit_rule`、`erp_initial_finance_subject`、`erp_initial_finance_partner`。前 6 张对应的菜单已挂出 → 用户点到空页 | 打印 4 页有新增入口（能落库），属空基线而非「不落库」；审核设置/财务期初同理 |
| P2-9 | **死代码（前端/后端）** | 见下 §3.1 |
| P2-10 | **应用中心 Javadoc 与代码自相矛盾**：类注释（`SetAppCenterController.java:46-58`）整段论证「为什么不加 `@SaCheckPermission`」，而 3 个端点实际都有该注解 | 注释引用的历史事实（`set:%` 0 行）已被 `V11.471.0` 推翻 |
| P2-11 | **错误处理口径分裂**：业务校验失败走 `Result.fail`（HTTP 200 + body code）；`requireTenantId()` 等抛 `RuntimeException` 被 `GlobalExceptionHandler`(:249-256) 兜底成 **HTTP 500**。设置模块至少 3 处（`SetMenuConfigController:183`、`SetAppCenterController:367`、`SetRebuildController:291`）。另：文档称 `intervene` 返回 400，实为 **HTTP 200 + body 400** |  |
| P2-12 | **双入口 / 跨域复用**：`views/agreement/index.vue` 同时挂 `62506 平台协议`(system-admin 域) 与 `81016 协议列表`(设置域)；`views/workflow/designer/index.vue` 挂 801 + 80610；`80626 外链同步` 复用了 `views/system/data-import/index.vue` | `group by component having count(*)>1` → 3 组 |
| P2-13 | **权益闸门（`sys_tenant_menu`）实际从未生效**：全表 4 行（菜单 80601-80604）**全部 `deleted=1`** → `SysMenuServiceImpl.java:322-333` 取到空集 → 走注释里的「平台尚未配置」fail-open 放行。即 P0-1 的可见性只由「权限码派生」一层决定 | `select * from sys_tenant_menu` → 4 行全 `deleted=1` |
| P2-14 | **迁移文件声明的权限 id 与库中实际不一致**：`V11.398.0__Audit_Config_Rules_And_Permission_Seeds.sql:71-72` 声明 `workflow:audit:list/update` 为 **91351/91352**，库中实际是 **91321/91322**（文件注释自陈改过两次号，Flyway checksum 已对应上） | 重建库会得到与现网不同的 id；`V11.402.0:111` 的注释亦佐证（称 91321 已被 V11.398.0 占用） |
| P2-15 | **权限码 `sort` 重号**：`workflow:task:transfer`(91392) 与 `workflow:definition:list`(91370) 同为 **406** | `select sort,count(*) from sys_permission where permission_code like 'workflow%' group by 1 having count(*)>1` |
| P2-16 | **陈旧注释**：`WorkflowController.java:180-184` 称部分端点「仅需登录」，实际 36 个端点**全部**带 `@SaCheckPermission`（与 P2-10 应用中心 Javadoc 同类：**注释比代码旧**） | 逐端点核对表见 §4 正面结论 |
| P2-17 | **`workflow_callback_log` 无 `deleted` 列**（同模块其余 4 张工作流表都有） | 逻辑删除列口径不统一，与 `fin_accounting_period` 用 `deleted_flag` 同类 |
| P2-18 | **`task/transfer` 的 `targetUser` 走 `Long.parseLong`**（`WorkflowController.java:749`）：传非数字用户名 → NPE 被捕获 → 400。当前前端传 `u.id`（`task-management.vue:1126`）故正常，但契约脆弱 | 字段名 `targetUser` 暗示可传用户名，实为 id |

### 3.1 死代码清单（删前请先按项目纪律查开发文档裁定）

| 项 | 位置 | 说明 |
|---|---|---|
| `workflowDefinitionApi` **整个对象 7 个方法** | `api/workflow/index.ts:41-66` | 全仓零引用（仅注释自指） |
| `workflowInstanceApi.getById/.start/.withdraw` + `interface WorkflowInstance` | `api/workflow/index.ts:186-216` | 零引用 |
| 4 个死枚举导出 | `api/workflow/index.ts:552/561/571/588` | `PROCESS_TYPE_MAP`、`DEFINITION_STATUS_MAP`、`TASK_ACTION_MAP`、数值版 `INSTANCE_STATUS_MAP` |
| 跨页事件 `workflow:create` | 监听 `process-analysis.vue:529`；**全仓 0 处派发** | 死事件 |
| 孤儿文件 `views/workflow/process-analysis.vue.test` | 18KB，旧版整页副本，全仓无引用 | 建议删除 |
| 后端 11 条无引用端点 | `/instances/{id}/status`、`/pending`、`/approved`、`/my-applications`、`/pending/count`、`/{id}/approve|reject|transfer|cancel`、`/callback-log/page|{id}/retry` | 其中 `/pending`、`/approved`、`/pending/count` 与 803/804 的 `/task/page` **能力重复（两套实现）** |
| `componentMap` 冗余键 | `dynamicRoutes.ts:124-125`（`workflow/definition`、`set/workflow-designer`） | DB 中已无对应 component，靠动态 import 兜底 |
| `TenantController.getConfig/updateConfig` | `:232-259` | 返回硬编码 Map / 桩实现 `return Result.ok(true)`，视图层零引用（死桩；**非在用假保存**） |
| `PrintConfigController` 读端点走 `getOrCreate` | `:98`、`:123` | 读操作建行 |
| `print:draft` 悬空权限码 | `sys_permission` 存在，无端点消费 |  |
| `LedgerServiceImpl.closePeriod` | `:482` | 无调用方，且不写期间状态（见 P1-5） |

---

## 4. 正面结论（可作回归基线）

以下均为本次**实测通过**的项，后续改动请勿回退：

1. **文档 §10.3 / §11 声称的 P0 修复，逐条核实基本为真**（这是本次最重要的正面结论）。带 DB 实测的三条硬证据：
   - `sys_config` **173 行**（§11.2 记 18→173）✓，`nav_group` 分布 17/14/33/8/5/10/62/24 逐项吻合；
   - `sys_tenant` **15 列** / `sys_tenant_profile` **24 列**、两表各 **3 行**（§11.4 拆表）✓；
   - `sys_tenant_module` **28 行**（§11.5 回填，原为 0 行）✓、`set_print_config` 1 行、`sys_config` 中 `locked` 折算逻辑在 `SystemConfigServiceImpl.java:455-528` ✓。
2. **菜单树连通性满分**：26 个叶子的 `component` **全部解析到真实 `.vue`**（含 3 个跨域复用页与靠动态 import 兜底的 801/80610）；空分组 **0 个**；`deleted<>0` 残留 **0 行**；子树内 `status/visible` 异常 **0 行**。
3. **设置 13 页接口接线 0 断链**（除 P0-3）：逐页把 api 层路径与后端 `@*Mapping` 归一化比对，全部命中。
4. **后端鉴权覆盖完整**：设置模块在用控制器（`SetMenuConfigController`、`SystemConfigController`、`SysLogStubController`、`SetAppCenterController`、`SetRebuildController`、`SystemTaskController`、`TenantController`、`PrintConfigController`、`PaymentConfigController`、`InitialStockController`、`InitialFinanceController`、`AccountingPeriodController`、`WorkflowController` 36/36 方法）**写端点 100% 带 `@SaCheckPermission`**，未发现裸奔写端点。
5. **表存在性 0 缺口**：代码 `@TableName` 全集（456 张）对照 `information_schema`，**无「引用但缺表」**（唯一缺失 `tenant_fill_probe` 是测试探针，预期不存在）。
6. **无桩代码**：设置 13 页 + 工作流 4 页全量 grep **零** `TODO|FIXME|mock|写死`（仅命中注释与 `TODO_COLUMNS` 常量名）；无 `console.log`；无「catch 吞异常仍提示成功」。唯一「静态目录」是 `AuditRuleCatalog`（枚举常量，注释已说明不落库理由）。
7. **金标准达成度良好**：横向自适应查询网格 `repeat(auto-fill,minmax(190px,1fr))` **恰 8 页齐备**（即全部有查询区的页面），**无纵向单列**；`PageConfigPanel`/`storage-key`/`StandardPagination` 在适用的页面均已具备（不适用者均有注释说明口径）。
8. **危险操作有二次确认**：系统重建（空选拦截 → 密码弹窗 → `Modal.confirm` **三重**，服务端 BCrypt 校验 + 审计）、会计期间启停（`a-popconfirm`）、菜单配置批量显隐（`Modal.confirm`）。
9. **真落库闭环**：系统参数保存后回读才提示成功（`sys-params/index.vue:446-466`）；支付配置保存后强制 GET 回读（`payment-config/index.vue:925`）；打印设置 PUT 后 `await load()` 回读（`print-config/index.vue:460-475`）。
10. **企业信息拆表数据保全**：`sys_tenant` 3 行 / `sys_tenant_profile` 3 行，无缺档；提交体 21 字段与后端 DTO 的 21 个 `@JsonProperty` **逐字全命中**。

---

## 5. 文档需回写的清单

| 文档位置 | 回写内容 |
|---|---|
| `设置模块/README.md` §2.1 / §2.2 | 菜单树 **7 组 18 页 → 8 组 26 叶**；补录 `80626 外链同步`、`80146 辅助核算`、`81000-81003`、`81016 协议列表` 与分组 `61208`；标注「以 devdb 实测为准」 |
| 同上 §2.1「三处结构问题」 | #1（61205 空分组）、#2（61201 sort=3 空缺）**均已消失**，只余 #3（同组件双菜单） |
| 同上 §10.5-3 / §11.7 | printing 5 页菜单「0 行」→ **81000-81003 已挂出**，仅 `designer` 无菜单 |
| 同上 §11.1 | `GET /behavior`「无权限注解」**与代码相反**（实际带 `set:print-config:view`）→ 应记为「有注解、与设计意图矛盾」（本报告 P1-1） |
| 同上 §7.1 P0-16 / §10.3 | `workflow:*` 权限码 **13 → 19**；`sys_permission` **264 → 1922 行**；并补记「37 条码只授 role 1 → 非超管全不可用」（本报告 P0-1） |
| 同上 §10.5-17 | 「退回只写备注」**已过期**（`returnTask` 已实现真实回退） |
| 同上 §7.3 | `workflow:create` 监听方「3 页 → 1 页」 |
| 各页文档的「二轮收口」节 | 补记本报告的 P1-2~P1-6（跨模块消费缺口）——这些**此前从未登记** |
| 打印相关文档 | 补记 P0-2（`printing:*` 幽灵码） |

---

## 6. 建议的处置次序

> 原则：**先修「非超管完全不可用」（P0-1/P0-2/P0-3），再补跨模块消费缺口（P1-2~P1-6），最后清冗余与死代码**。
> 前三条都是**小改动、大收益**（改 1 行注解 / 批量替换权限码前缀 / 去一个尾斜杠），建议优先。

| 批次 | 内容 | 涉及 | 需人工拍板 |
|:-:|---|---|:-:|
| **一（P0）** | ① 会计期间 `create` 去尾斜杠（P0-3，改 1 行）<br>② 打印 18 处 `printing:` → `print:`（P0-2）<br>③ `PrintConfigController:118` 删注解（P1-1） | 前端 2 文件 + 后端 1 行 | 否 |
| **二（P0-1）** | 设置域权限码授予方案：给 `SYSTEM_ADMIN`/`DEPT_ADMIN` 补授哪些码，或改为超管专属（隐藏 16 个 fail-open 菜单） | `sys_role_permission` / `sys_menu` | ✅ **必须**（系统配置数据，按纪律先出方案获批） |
| **三（P1）** | 跨模块消费缺口：企业信息 → 单据/打印抬头；财务期初 → 报表；`sys_config` 的消费方或下线；支付配置收敛 | 多个模块 | 部分需产品口径 |
| **四（P1/P2）** | 死代码清理（§3.1）、平行实现收敛（§2 P1-8）、菜单数据订正（sort 重号、`.vue` 后缀、`menu_code` 命名） | 多文件 + 菜单数据 | 菜单数据需拍板 |
| **五** | 文档回写（§5） | README 等 | 否 |

---

## 6.5 本轮已执行的修复（2026-09-26）

> 用户「解决你发现的问题」后落地。**代码改动 4 个文件 + 新增 1 个迁移**，迁移已应用到 devdb 并验证。

| # | 条目 | 处置 | 落点 | 验证 |
|:-:|---|---|---|---|
| 1 | **P0-1 ③ menu_level** | 10 条设置菜单 `menu_level` 3 → 0 | `V11.510.0` 第一节 | 实测设置子树 26 条**全部为 0** ✓ |
| 2 | **P0-1 ① ② 权限** | 给 `SYSTEM_ADMIN` 授 115 条、`DEPT_ADMIN` 授 13 条设置域码 | `V11.510.0` 第四节 | 实测 `SUPER_ADMIN=118 / SYSTEM_ADMIN=115 / DEPT_ADMIN=13`，差值 3 = 刻意排除的 `set:rebuild:execute` + `workflow:callback-log:*`(2) ✓ |
| 3 | **P0-3 尾斜杠** | `POST /erp/finance/period/` → 去尾斜杠，并订正错误注释 | `api/finance/index.ts:1118-1123` | 后端 `@PostMapping`（无 value），路径现与之一致 |
| 4 | **P1-1 `/behavior` 注解** | 删除 `@SaCheckPermission("set:print-config:view")`，与 Javadoc/前端假定一致 | `PrintConfigController.java:116` | 该类其余 3 个端点注解不动；同批订正 Javadoc 中已过期的「只授超管」表述 |
| 5 | **P1-9-4 时间线词表** | `RECORD_ACTION_MAP` 补 `return: 退回` | `instance-monitor.vue:557` | 后端 `WorkflowConverter.taskActionToString(8)="return"`，前端 `TASK_ACTION_TEXT_MAP` 同口径 |
| 6 | **P2-1 sort 重号** | `80630 操作日志` sort 2 → 4 | `V11.510.0` 第二节 | 实测 `61203` 已不在全库 sort 重号组中 ✓ |
| 7 | **P2-2 `.vue` 后缀** | `804` component 去 `.vue` | `V11.510.0` 第三节 | 与同组 802/803 写法一致 |

**修复后的可见性重算**（按 `MenuPermissionDeriver` + `SysMenuServiceImpl` 规则实算）：

| 角色 | 26 叶中因权限码被隐藏 | 说明 |
|---|:-:|---|
| `SUPER_ADMIN` | **0** | 全可见 |
| `SYSTEM_ADMIN` | **2**：系统重建、协议列表 | 系统重建**刻意保留超管**（危险操作）；协议列表属 `agreement:*`（27 条仍只授超管，见下） |
| `DEPT_ADMIN` | **9** | 均为**刻意设计**（部门管理员不做设置项管理） |

**真机验证（2026-09-26，账号 `e2e_hr_ta` = SYSTEM_ADMIN / 租户 1，后端 `localhost:5655`）**：

| 验证项 | 调用 | 结果 |
|---|---|---|
| **P0-1** 设置域菜单下发 | `GET /api/menu/user/mega/tenant-admin?tenantId=1` | **24 页 / 8 个分组**（修复前该账号仅「会计期间」1 页可用） |
| **P1-1** 打印行为配置 | `GET /api/set/print-config/behavior` | **HTTP 200** + 真实配置体（修复前非超管 **403**） |
| **P0-3** 会计期间新增 | `POST /api/erp/finance/period` | **HTTP 400「会计年度不能为空」** ⇒ 端点已命中（修复前请求发的是带尾斜杠的路径，永远 404） |
| **P0-3 反证** | `POST /api/erp/finance/period/` | **HTTP 404「接口不存在: api/erp/finance/period」** ⇒ 实测确认尾斜杠不匹配，即原 bug 成立 |
| 各设置页接口可用性 | 7 个分页/详情端点 | **全部 200**：`/api/config/page`、`/api/set/initial-stock/page`、`/api/workflow/task/page`、`/api/workflow/instance/page`、`/api/set/print-config`、`/api/set/system-task/page`、`/api/erp/finance/period/page` |

菜单下发的两处「缺失」与设计一致：**系统重建**（刻意保留超管）与**协议列表**（`agreement:*` 仍只授超管）。⇒ P0-1 / P0-3 / P1-1 三条**均有真机证据**，不再是静态推导。

**同批核实但未执行的项（附理由）**：

| 条目 | 为何不执行 |
|---|---|
| **P0-2 `printing:*` 幽灵码** | **已由并行会话修复**（本次复核：`views/printing/*/index.vue` 现全部为 `print:*`，`printing:` 计数 0），无需重复改 |
| **P1-2~P1-6 跨模块消费缺口** | 属**功能开发**（让企业信息进单据抬头、财务期初进报表、`sys_config` 找消费方），非缺陷修复，需产品口径与排期 |
| **P1-7 v-permission 缺失** | P0-1 修复后**风险面已收窄**为「持 `:list` 码但无 `:update` 码的角色」——当前 5 个角色中 `SYSTEM_ADMIN` 拿到全量码、`DEPT_ADMIN` 看不到这些页，**无此组合**；前端加错码反而会隐藏本该显示的按钮，故不改 |
| **P1-9-1 会签不生效** | `approveMode` 是**未完成的实现骨架**（0 处消费），补全会签判定属审批核心逻辑的功能开发，且当前无数据触发（`workflow_node` 无 `approve_mode` 列），宜单独排期 + E2E |
| **P1-9-5 `task/stat` 短路缺键** | **复核证伪，撤销**（`TASK_STAT_KEYS` 已含全部 19 键） |
| **P2-2 `80610 path` 前导斜杠** | 复核判定**文档记反**：无前导斜杠是主流写法（301 条中仅 5 条带），**不改** |
| **81016 协议列表可见性** | 依赖 `agreement:*` 授权策略（27 条仍只授超管），属**协议模块**自己的范围（其菜单 62506 同样受影响），登记不越界改 |
| **P1-8 平行实现收敛 / §3.1 死代码** | 涉及跨模块资产，按项目纪律「处置重复实现前必须先查开发文档裁定」，宜另起一轮专项（本报告 §3.1 已备清单） |

---

## 7. 审计方法与产物

**方法（全部只读）**：
1. **源码静态比对**：逐页读前端 + 逐 Controller 读后端，把前端 api 层路径归一化后与后端 `@*Mapping` 做双向差集；
2. **数据库直查**：`psycopg2` 连 `localhost:5432/devdb`（devuser）执行 SELECT——菜单递归树、权限码/角色授权分布、表结构与行数、实体列对账；
3. **运行期探针**：对 `localhost:5655` 发**未授权只读**请求，用「405（命中映射）vs 401（未命中）」区分尾斜杠匹配行为（P0-3）；
4. **并行分工**：4 个方向（set 页面 / 工作流页面 / 后端 / 数据库+菜单权限）+ 1 个补充方向（打印组+跨模块关系）并行审计后交叉复核，高影响力结论（P0-1/P0-2/P0-3/P1-1）由主流程亲自复验。

**产物**：本报告（`SETTINGS_MODULE_AUDIT_20260924.md`）。本次**未新增脚本文件、未改动任何代码或库数据**。

**已知局限（如实登记）**：
- **未做登录态端到端复验**：P0-1 的「非超管 25/26 不可用」是按 `MenuPermissionDeriver` + `SysMenuServiceImpl:288-350` 的规则**静态推导**（规则原文已引、O(1) 可复核），并以 `sys_role_permission` 实测数据佐证；**未用非超管账号真机登录**（避免与并行会话抢 Sa-Token 会话，`sa-token.is-concurrent=false`）。
- **未跑 E2E**：`tools/e2e-settings.cjs` 等未运行，文档声称的 E2E 断言（如「804 与 803 落点不同」）未复验，仅做代码层核实。
- **财务期初/打印组的「空表」影响面**：相关表 0 行，P1-3 的报表缺口与 P2-8 的空页体验按代码路径成立，但无真实数据触发。
- **`sys_permission` 1922 行中设置模块之外的幽灵码**：仅对 `set:`/`workflow:`/`printing:` 做了「常量定义 + 注解 + 前端 `v-permission`」三路合并复核；其余前缀（如 `erp:fixed-asset:*`、`wms:wave:*`）未逐条复核，**不予定级**。

---

## 附：设置子树菜单完整实测（`deleted=0`，2026-09-24）

```
60012 设置 (mega:settings, sort=1500, client_type=tenant-admin, tenant_id=0)
├─ 61201 系统配置 (100)
│   ├─ 80620 菜单配置  set:menu-config      set/menu-config        views/set/menu-config/index.vue      [覆盖→隐藏]
│   ├─ 80621 系统参数  set:sys-params       set/sys-params         views/set/sys-params/index.vue       [fail-open→403]
│   ├─ 80626 外链同步  set:external-sync    set/external-sync      views/system/data-import/index.vue   [fail-open→403] ★跨域复用
│   ├─ 80622 审核设置  set:audit-config     set/audit-config       views/set/audit-config/index.vue     [fail-open→403]
│   ├─ 80623 支付配置  set:payment-config   set/payment-config     views/set/payment-config/index.vue   [fail-open→403]
│   ├─ 80624 企业信息  set:company-info     set/company-info       views/set/company-info/index.vue     [覆盖→隐藏]
│   └─ 80625 应用中心  set:app-center       set/app-center         views/set/app-center/index.vue       [覆盖→隐藏]
├─ 61202 数据录入 (200)
│   ├─ 70550 库存期初  set:initial-stock    set/initial-stock      views/set/initial-stock/index.vue    [覆盖→隐藏]
│   └─ 70551 财务期初  set:initial-finance  set/initial-finance    views/set/initial-finance/index.vue  [覆盖→隐藏]
├─ 61203 账套操作 (300)   ⚠ sort 重号（70560 与 80630 均 =2）
│   ├─ 70560 系统重建  set:rebuild          set/rebuild            views/set/rebuild/index.vue          [覆盖→隐藏]
│   ├─ 80630 操作日志  set:operation-log    set/operation-log      views/set/operation-log/index.vue    [fail-open→403]
│   └─ 70561 系统任务  set:system-task      set/system-task        views/set/system-task/index.vue      [覆盖→隐藏]
├─ 61204 财务设置 (400)
│   ├─ 70570 会计期间  set:accounting-period set/accounting-period views/set/accounting-period/index.vue [fail-open→✅唯一可用]
│   └─ 80146 辅助核算  finance:auxiliary    finance/auxiliary      views/finance/auxiliary/index.vue    [覆盖→隐藏] ★跨域复用
├─ 61205 打印管理 (500)
│   ├─ 80930 打印设置  set:print-config     set/print-config       views/set/print-config/index.vue     [覆盖→隐藏]
│   ├─ 81000 打印模板  set:print-template   printing/template      views/printing/template/index.vue    [fail-open→403+按钮全隐]
│   ├─ 81001 打印链路  set:print-chain      printing/chain         views/printing/chain/index.vue       [同上]
│   ├─ 81002 打印客户端 set:print-client    printing/client        views/printing/client/index.vue      [同上]
│   └─ 81003 打印任务  set:print-task       printing/task          views/printing/task/index.vue        [同上]
├─ 61206 工作流 (600)
│   ├─ 801   流程定义  workflow-definition  /workflow/definition      views/workflow/designer/index.vue    [fail-open→403]
│   ├─ 80610 流程设计  set:workflow-designer set/workflow-designer(无前导斜杠) 同一组件                 [fail-open→403]
│   └─ 80611 流程分析  set:workflow-analysis /workflow/process-analysis views/workflow/process-analysis.vue [fail-open→403]
├─ 61207 审批 (610)
│   ├─ 802   流程实例  workflow-instance   /workflow/instance     views/workflow/instance-monitor      [fail-open→403]
│   ├─ 803   我的待办  my-task             /workflow/task         views/workflow/task-management       [fail-open→403]
│   └─ 804   我的已办  my-done             /workflow/done         views/workflow/task-management.vue   [fail-open→403]
└─ 61208 协议契约 (620)
    └─ 81016 协议列表  agreement           agreement              views/agreement/index.vue            [覆盖→隐藏] ★与 62506 双入口

按钮型菜单（menu_type=2，无 component/path，不生成路由）：8011-8015（挂 801）、8031-8034（挂 803）
↳ 其中 8033 转交 / 8034 撤回 的 menu_code 是「连字符式」，与任何权限码都不相等 ⇒ 纯死数据（P2）
```

**统计**：44 行 = 1 顶级 + 8 分组 + 26 叶子 + 9 按钮；`component` 解析失败 **0**；空分组 **0**；覆盖桶 10 / fail-open 桶 16。

---

*审计时间：2026-09-24　·　审计方式：只读（源码静态比对 + devdb SELECT + 未授权运行期探针）　·　未改动任何代码与库数据*
