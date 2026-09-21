# AI-Ready 全面收尾 · 主待办清单

> 建立日期：2026-09-20 · 本文是**唯一的总控清单**，其余四份审计报告降级为证据附件。
> 使用方式：每条任务有唯一编号（`模块-序号`），处理完把「状态」改为 ✅ 并在文末「执行日志」追加一行。
> 状态口径：⬜ 未开始 / 🔄 进行中 / ✅ 已完成 / ⏸ 待业务拍板 / 🚫 决定不做（写明理由）

## 0. 口径与证据来源

**证据附件（不重复劳动，直接引用）**

| 文件 | 覆盖 |
|---|---|
| `CLEANUP_SCOPE_20260919.md` | 三类遗留全量盘点 + 孤儿页 87 个分档 |
| `CLEANUP_AUDIT_REPORT.md` | 死代码逐项证据 |
| `TABLE_DUPLICATE_AUDIT.md` | 553 张表分档 + A 类 9 组两套实现 + 72 张孤儿表 |
| `FUNCTION_DUPLICATE_AUDIT.md` | 接口 78 组 / 页面 8 组 / 方法体 22 段重复 |
| `CLEANUP_DECISIONS_20260919.md` | 7 项裁决单 + 已修项 + 防再生门禁 |

**本轮（09-20）新增的两份专项审计结论已并入本文**：① 双层权限（系统级/租户级）审计；② 数据链路断点审计（含逐列非空率实测）。

**可复现脚本**：`tools/audit-*.py`（表/页面/接口/代码/权限码/鉴权覆盖/命中）、`tools/dbq2.py "SELECT ..."`（只读查库）、`tools/build-backend.sh`（clean 构建+重启）。

**两条铁律（血泪换来的，违反会造成线上事故）**
1. **补鉴权注解前必须先补权限码种子** —— 代码引用了库里没有的码，会让非超管全部 403。
2. **删任何文件必须 `mvn clean package`** —— 增量构建不清理 `target/` 旧产物（`.class` 会被组件扫描继续加载，`.xml` 会被打进 fat jar）。删 `*Controller`/`*Service` 前还要按**类名**搜 `@Pointcut`/`execution(`，切点是软引用，编译不报错、启动才炸。

---

## 1. 用户六条诉求 → 专项映射

| # | 诉求 | 对应专项 | 条目分布 |
|---|---|---|---|
| 1 | 死代码、死数据库表清理 | 专项 A（死代码）/ 专项 B（死表） | A-* / B-* / 各模块 `-CLEAN` 条目 |
| 2 | 同代码新旧多版本，需统一 | 专项 C（新旧并存） | C-* / 各模块 `-DUP` 条目 |
| 3 | 数据有没有打通、有没有断点 | **专项 D（数据断点）** | 各模块 `-BREAK` 条目 |
| 4 | 权限重构后的权限遗漏 | **专项 E（权限遗漏）** | E-* / 各模块 `-AUTHZ` 条目 |
| 5 | 按模块按功能逐个处理 | 第 3 节模块清单 | 全部 |
| 6 | 权限有系统级 + 租户级两层 | **专项 F（双层权限）** | F-* |

---

## 2. 进度看板

| 模块 | 断点 | 新旧重复 | 死代码/死表 | 权限 | 拍板项 | 状态 |
|---|---|---|---|---|---|---|
| 平台/系统（core-*、租户、用户） | 3 | 4 | 12 | 15 | 6 | 🔄 **`SysConfigService` 硬编码租户专项已闭环**（配置读写按租户隔离，28/28 真机验证）；剩 `ConfigChangeListener` 死缓存待清 |
| 财务域（erp-finance） | 5 | 2 | 3 | 4 | 2 | ⬜ |
| 库存仓储（erp-stock + wms） | 6 | 4 | 8 | 1 | 2 | 🔄 STK-BREAK-01/03 已闭环；-02 剩 2 处、-04/05 待做 |
| 销售域（erp-sales） | 4 | 3 | 2 | 3 | 2 | ⬜ |
| 采购域（erp-purchase） | 4 | 3 | 2 | 3 | 1 | 🔄 PUR-BREAK-01 已闭环；PUR-BREAK-03/04 待做 |
| **权限专项（E-01/E-04）** | — | — | — | 7 模块已补 | — | 🔄 97 控制器 / 709 端点已补；剩 crm/wms/b2b/analytics |
| 主数据（erp-partner / md-*） | 3 | 3 | 6 | 1 | 1 | ⬜ |
| DMS 配送 | 3 | 1 | 2 | 2 | 1 | ⬜ |
| 商城/营销（erp-mall / marketing） | 3 | 2 | 4 | 2 | 1 | ⬜ |
| HR | 1 | 1 | 2 | 1 | 1 | ⬜ |
| CRM | 1 | 1 | 2 | 4 | 0 | ⬜ |
| 报表分析 | 2 | 1 | 5 | 3 | 0 | ⬜ |
| 其余（打印/固定/预算/发票/observability） | 2 | 2 | 4 | 4 | 1 | ⬜ |
| 前端工程 / 构建 / 门禁 | — | 2 | 3 | — | 0 | ⬜ |

---

## 3. 模块清单（处理顺序 = 下表顺序）

---

### 3.1 平台 / 系统域（core-base、core-api、core-platform、租户、用户、权限）

#### 平台-BREAK-01 [P0] 租户上下文缺失 = 全租户可见可写（fail-open）✅ 已定位，待修

- **现象**：任何**已登录但 session 里没有 `tenantId`** 的会话，其所有租户表查询/写入都**不带租户条件**。
- **证据**：`AiReadyTenantLineInnerInterceptor.java:54-57` `shouldSkip()` = `getCurrentTenantIdValue() == null || isTenantScopeExempt()`；`MyBatisPlusConfig.java:125-144` 只认 ThreadLocal 与会话两来源。
- **四个漏写租户的登录入口**（登录成功但不写 `session.tenantId`）：
  - `backend/wms/.../PdaAuthController.java:42`（PDA 仓库端登录）
  - `backend/erp/erp-printing/.../v2/ClientAuthController.java:83`（打印客户端）
  - `backend/erp/erp-mall/.../b2b/service/impl/MallAuthServiceImpl.java:67`（商城 C 端，**且在白名单里**）
  - `AuthServiceImpl.java:103`（待确认是否有调用方）
- **代码里把 fail-open 论证成"上游 SaInterceptor 会拦未登录"** —— 但上游只判「是否登录」，不判「租户上下文是否存在」，这两件事在本项目不等价。
- **修法**：`shouldSkip()` 拆两语义：超管豁免（显式意图）保留；**上下文缺失对业务表 fail-closed**（抛异常或注入恒假条件）。登录链路对 `sys_user` 的查询用白名单式 `@InterceptorIgnore` 单独开口，而不是全局跳过。
- **依赖**：平台-BREAK-02 的 `insertFill` 问题同源，建议一并改。
- **状态**：⬜

#### 平台-BREAK-02 [P0] `insertFill` 无条件覆盖实体 tenantId ⇒ 跨租户写全部落错租户

- **证据**：`MyBatisPlusConfig.java:253-263` 用 `metaObject.setValue("tenantId", tenantId)` 而**非** `strictInsertFill`，不判断字段是否已有值。
- **项目自己已踩过**：`SysTenantMenuMapper.java:24-31` 注释记着「给租户 A 授权写到会话租户头上（2026-09-18 实踩）」，`SysTenantMenuMapper.xml:36-40` 因此改用标量 `#{tenantId}` 绕过。
- **数据佐证**：`sys_role_permission` 中 `role_tenant=1` 而关联行 `tenant_id=0` 的有 **16 行**、`=1` 的 518 行 —— 同一角色权限关联行租户标记不一致。
- **修法**：改为「仅当 `getValue("tenantId") == null` 时填充」+ 单测覆盖「显式指定 ≠ 会话租户」。
- **状态**：🔄 **代码已改（2026-09-20）** —— `MyBatisPlusConfig.java:253` 已加 `hasGetter` + `getValue(...) == null` 双重条件（并核实全仓无实体在自己初始化 `tenantId = 数字`，故无"显式 0 被跳过填充"风险）。**待办**：单测 + 编译验证。

#### 平台-BREAK-03 [P1] 51 行业务数据 `tenant_id IS NULL`，对任何租户都不可见

- **证据**（全库动态扫 494 张含 `tenant_id` 的表）：`budget_item` 21 / `annual_budget` 15 / `budget_execution_log` 7 / `finance_voucher_item` 6 / `erp_group_buy_activity` 1 / `finance_ledger` 1 = **51 行**。
- **成因**：`setTenantId(null)` 共 12 处（`HrAttendanceServiceImpl.java:297`、`MallKeywordServiceImpl.java:81`、`MallNoticeServiceImpl.java:83`、`CouponTemplateController.java:77` 等），依赖 `insertFill` 兜底，而**无上下文时 `insertFill` 是 no-op**。
- **修法**：补 `tenant_id NOT NULL DEFAULT 0` + 一次性回填（需确认归属）+ 修 12 处调用点。
- **状态**：⬜

#### 平台-BREAK-04 [P1] 租户初始化不写菜单授权 ⇒ 新租户管理员登录后看不到菜单

- **现状（`TenantRegistrationService.approve()` 实测阅读）**：租户审批通过时做了 6 步 —— 启用租户 → 启用 admin 并标记
  `isTenantAdmin` → `createDefaultAdminRole(tenantId)` 建默认管理员角色 → 给 admin 分配该角色 →
  **从「系统默认权限模板」（`PermissionTemplateService.getSystemTemplates()` + `applyTemplateToRole`）初始化角色权限** →
  清权限缓存。
- **断点**：初始化覆盖了 **`sys_role_permission`（接口权限）**，但**完全没有**写
  **`sys_role_menu`（角色菜单授权）** 与 **`sys_tenant_menu`（租户菜单授权）**。
- **证据链（DB 实测）**：`sys_role_menu` 全表仅 3 行（全属 SUPER_ADMIN）；租户 2 的 `E2E_T2_ADMIN`
  有 2 条 `sys_role_permission` 但 `sys_role_menu` **为 0**；`sys_tenant_menu` 的 tenant 1/2 均 0 行。
  而 `SysMenuServiceImpl.java:279-291` 要求 `sys_tenant_menu ∩ sys_role_menu` 取交集 ⇒ **交集恒空，菜单返回空数组**。
- **影响**：每个新批下来的租户，其管理员**能登录但导航为空**（靠前端静态路由兜底才没被当成故障）。
  这是平台-AUTHZ-01「非超管菜单授权为 0」的**根因与再生产机制**。
- **修法（二选一）**：
  - **A（推荐）**：把菜单可见性改为**从权限派生**（有 `xxx:list` 权限即显示对应菜单），从根本上不再维护第二套数据；
  - **B**：在 `approve()` 里补两步 —— 按默认模板写入 `sys_role_menu` 与 `sys_tenant_menu`，
    并把 `sys_permission_template`（现仅 2 行且无 UI、见 F-11）补成一个可维护的完整模板。
- **✅ 已拍板（2026-09-21，用户）**：选 **A —— 菜单可见性从权限派生**，租户初始化不再写第二套菜单授权表。
- **状态**：🔄 实施中（依赖权限码覆盖率，见下方「派生可行性实测」）

#### 平台-BREAK-05 [P1] 经营指标 / 会计期间查询**硬编码 `tenant_id = 1`**，多租户下取错数据

- **证据**：
  - `erp-observability/.../MetricsCalculationServiceImpl.java:39` `private static final long DEFAULT_TENANT_ID = 1L;`
    且 **30 余处** SQL 直接字符串拼接 `" ... AND tenant_id = " + DEFAULT_TENANT_ID`
    （:152/168/183/199/215/248/263/279/294/343/358/408/424/440/456/482 等）。
  - `erp-observability/.../MetricsServiceImpl.java:48` 同样 `DEFAULT_TENANT_ID = 1L`（:146/188 使用）。
  - `erp-finance/.../AccountingPeriodServiceImpl.java:35` `DEFAULT_TENANT_ID = 1L`，:42/:54 用于查会计期间。
- **影响**：**经营看板/指标对任何租户都只统计租户 1 的数据** —— 租户 2 的管理员看到的是平台租户的经营数据
  （既是数据不准，也是跨租户数据泄露）；会计期间同理（非租户 1 的租户查自己的期间会取到租户 1 的）。
- **性质**：与 F-01 同源（`tenant_id = 1` 被当成"全局"用），但这里是**在业务查询里写死**，
  绕过了多租户插件（字符串拼接的 SQL 插件也改不了）。
- **修法**：改为取**当前会话租户**（`MyBatisPlusConfig.getCurrentTenantIdValue()`），
  平台级汇总场景显式传参而非硬编码；`AccountingPeriodServiceImpl` 同期改。
- **状态**：✅ **已实施（2026-09-20），待运行验证** ——
  1. `MetricsCalculationServiceImpl`：常量 `DEFAULT_TENANT_ID` → `FALLBACK_TENANT_ID` + 新增
     `currentTenantId()`（`MyBatisPlusConfig.getCurrentTenantIdValue()` 优先，取不到才回落），
     **全部 30 余处字符串拼接的 `tenant_id = 1` 已替换**；
  2. `MetricsServiceImpl` 同样处理；
  3. `AccountingPeriodServiceImpl` 同样处理（此前写死 1 会让非平台租户的**月结/关账作用到租户 1**）；
  4. `MetricsScheduler.collectRealtimeMetrics()` 改为**逐租户采集**：定时任务没有会话上下文，
     原先两边叠加的结果是"所有指标只统计平台租户"；现在调度器 `listActiveTenantIds()`
     （查 `sys_tenant` 中 `status=1`）→ 逐租户 `setTempTenantId` → 采集 → **finally clear**
     （ThreadLocal + 线程池复用，不清理会串租户）。单租户失败不影响其余租户；租户列表查询失败则
     本轮跳过（fail-closed，而不是回落去采平台租户 —— 那会把"读不到租户"变成"所有看板都显示平台数据"）。
- **性能提示**：采集耗时变为「租户数 × 单租户」，当前租户量级可接受；租户显著增多时应改为按租户轮转。

#### 平台-DUP-01 [P1] 用户/角色/权限三套旧实现并存（v2 前缀 + 复数表 + 被遮蔽实现）

- **清单**：
  - `SysUserController`（新） vs `UserController`（`/api/v2/user/...` 旧前缀）
  - 5 张复数命名旧 RBAC 表 `users`/`roles`/`permissions`/`user_roles`/`role_permissions`（全 0 行、无实体无查询）→ 最安全的删除项（`TABLE_DUPLICATE_AUDIT` A-06）
  - `SearchServiceImpl`(@Primary) vs `AdvancedSearchServiceImpl`（**已删**，09-19）
  - `AuthServiceOptimizedImpl`(@Primary) vs `AuthServiceImpl`（**已删并改名**，09-19）
- **修法**：`/api/v2/user/*` 走弃用流程（先看访问日志）。
- **状态**：🔄 **A-06 五表已删除（本轮核验）** —— 迁移 `V11.433.0__Drop_Legacy_Permission_Tables.sql` 已执行成功（同时删掉 `sys_data_permission` 这套重复的数据权限实现），DB 实测 `users/roles/permissions/user_roles/role_permissions` 均不存在。`SearchServiceImpl`/`AuthServiceImpl` 两处遮蔽实现已于 09-19 删除。**剩余**：`/api/v2/user/*` 旧前缀走弃用观察流程。

#### 平台-SEC-01 [P0·安全] `PdaAuthController.login` 是桩实现：不校验密码，任意用户可登录

- **证据**：`backend/wms/src/main/java/cn/aiedge/wms/controller/PdaAuthController.java:37,42,68-80` —— `resolveUserId()` 用 `Math.abs(username.hashCode() % 10000) + 1` 造 userId，**只检查用户名≥2位、密码≥6位**，不做任何数据库查询与密码校验，随后直接 `StpUtil.login(userId)` 发 token。注释自己写着"临时实现…生产环境应替换为"。
- **影响**：任何人可对 PDA 端取得任意身份的有效 token（且该 token 又因 平台-BREAK-01 不带租户上下文 ⇒ 全租户可见可写）。属**认证绕过**。
- **消费方**：PDA 前端 `frontend/apps/pda-warehouse/src/api/index.ts:120-125` 定义了 `auth.login/logout`（需进一步确认页面是否真调用）；后端该控制器已被登记进 `core-api/src/test/resources/known-unauthorized-controllers.txt:337`。
- **⚠️ 本轮核验更正（2026-09-20 实测）**：该登录接口 **`/api/v1/warehouse/auth/login` 不在 `SaTokenConfig` 的任何白名单里**
  （两处 `excludePathPatterns` 均无它）⇒ 未登录调用会被 `SaInterceptor` 拦下返回 `{"code":401,"message":"请先登录"}`（已实测）。
  因此原判断需修正为两点：
  1. **原"认证绕过"漏洞实际不可被外部利用**（未登录根本进不到该端点；仅已登录用户可调用，且会把自己的会话重登为 hash userId）；
  2. 但反过来暴露一个真问题：**PDA 登录功能从来就是坏的**（接口不可达，前端 `api/index.ts:120` 定义了却必然 401）。
- **决策（用户 2026-09-20）**：**采用方案 A —— 改真实鉴权**。
- **实施（已完成）**：
  1. `PdaAuthController` 重写 —— 注入 `SysUserMapper` + `PasswordEncryptor`，按
     `selectByUsername(username, null)`（该方法带 `@InterceptorIgnore`，用户名全局唯一故不按租户过滤）查用户，
     依次校验密码（BCrypt）、`status == 1`、`tenantId != null`；登录后写 `session.tenantId` 与
     `tenantScopeExempt`，与主站登录完全同口径。删除了 `resolveUserId()` 桩方法。
  2. **`SaTokenConfig` 两处 `excludePathPatterns` 补 `/api/v1/warehouse/auth/login`**（否则登录接口不可达，
     见上面的核验更正）—— 只放行登录本身，`/api/v1/warehouse/**` 下业务接口仍需登录。
- **⚠️ 行为变更**：PDA 端必须改用**真实 `sys_user` 账号 + 密码**登录。若 PDA 演示环境此前依赖假账号，会立即失败 —— 这是预期。
- **✅ 验证通过（2026-09-20 重启实测）**：
  - 错误密码 → `{"code":401,"message":"用户名或密码错误"}`
  - 用户名过短 → `{"code":400,...}`
  - 未登录调用不再被拦成「请先登录」⇒ 白名单与真实鉴权同时生效。
- **状态**：✅ 已完成并验证

#### 平台-CLEAN-01 [P2] 13 个未装配功能包

- **清单**：`storage`/`report`/`search`/`knowledge`/`gateway`/`mq`/`recommendation`/`webhook`/`agent`/`assistant`/`feedback`/`runner`/`inventory.repository`。
- **推荐**（`CLEANUP_DECISIONS` §3）：多数删除；`agent`/`knowledge` 按 `AGENTS.md` 保留契约删实现。
- **前置**：逐包 `git log` 确认近期无人在动。
- **状态**：⬜

#### 平台-CLEAN-02 [P1] 两套 feature flag 前端实现

- **证据**：`composables/useFeatureFlag.ts` 与 `utils/featureFlags.ts` **都有引用方**（`composables/index.ts` / `main.ts`）。
- **修法**：收敛为一套。
- **状态**：⬜

---

### 3.2 专项 E：权限遗漏（跨模块）

> 完整分析见记忆 `permission-layering-gap-audit`（09-20 已修一大批：数据权限拦截器入插件链、表级自动模式、权限模拟接线、SoD 挂对入口、erp-finance 103 处 `@PreAuthorize` → `@SaCheckPermission`）。**以下为仍未闭环项。**

#### E-01 [P0] 243/397 控制器无任何访问控制注解（覆盖 2170 端点）

- **口径**：必须计入自定义注解 `@RequirePermission`（`PermissionAspect` 真实执行）等；**不**计入 `@DataPermission`（它管数据范围不管可达性）。
- **门禁已落地**：`AuthzAnnotationCoverageTest`（棘轮基线，新增违例即红）。
- **分批补（每批先补权限码种子）**：

| 批次 | 范围 | 理由 |
|---|---|---|
| 1 | `erp-finance.*` / `erp-payment.*` / `erp-invoice.*` / `erp-budget.*` | 涉及金额，越权后果最重（erp-finance 已由 09-20 修完 103 处，需复核余量） |
| 2 | `crm.*`（8 个控制器） | 整模块零鉴权，含客户主数据 |
| 3 | `erp.stock.*` | 采购/库存/销售单据 |
| 4 | `wms.*` / PDA | 需先定 PDA 设备鉴权策略 |
| 5 | `erp.marketing.*` / `erp.b2b.*` | 含 C 端公开接口，需逐个甄别 |

- **另**：`erp.pricing.controller.PriceApprovalController`（价格审批）单独补。
- **状态**：🔄 **批次 3（stock）与部分批次 1 已完成（2026-09-21）** ——
  本轮给 **7 个模块 / 97 个控制器 / 709 处端点**补上了 `@SaCheckPermission`：
  budget(54) + fixedasset(50) + stock/product/md(288) + invoice(42) + payment(95) +
  party(92) + marketing(142)。注解由 `tools/gen-module-permission-seed.py --apply`
  按同一套规则插入（手改 87 个文件必然漏改；漏改是静默 fail-open，改错是 403）。
  **覆盖率变化**：无 `@SaCheckPermission` 的控制器 **311(79%) → 218(55%)**；
  **无任何 `@SaCheck*` 的 224(57%) → 139(35%)**（端点 2028 → 1319）。
  **门禁同步**：`known-unauthorized-controllers.txt` 按棘轮规则删掉 **104 行**
  （85 行本轮 + 19 行历史过期，后者含 09-20 erp-finance 那批与已删的 FieldPermissionController）；
  `AuthzAnnotationCoverageTest` + `PointcutTargetExistenceTest` **4/4 通过**。
  **未做**：批次 2（crm 8 控制器，MD-AUTHZ-01 P0）、批次 4（wms/PDA）、批次 5（marketing/b2b 的 C 端甄别）、
  以及 `analytics`/`report` 零权限。

#### E-02 [P1] 僵尸权限码（能在矩阵勾选，勾了不生效）

- **实测口径（09-20）**：`474 = 312 生效 + 151 未生效 + 11 分组节点`；补 32 码后为 `506`，未生效 **144**。
- **⚠️ 数字已过时**：`V11.435.0__Remove_Legacy_Permission_Codes.sql` 已执行成功，DB 实测 `sys_permission` 现为 **500 行** ⇒ 需**重跑脚本取当前口径**再动手。
- **修法**：二选一 —— 接线，或从授权矩阵下线（前端 `v-permission` 302 处也是消费方，不能只扫后端）。
- **脚本**：`tools/gen-permission-effectivity.py` → `GET /api/permission/effectivity`；`tools/audit-permission-codes.py`。
- **状态**：🔄 **已重测 + 已逐条定性，剩"接线 or 删码"的施工（2026-09-21）**
  - **重测结果（当前口径，已修正）**：`994 = 873 生效 + 110 未生效 + 11 分组节点`。
    ⚠️ **原先报的 159 是工具误判**（见下方"根因"），修正后**真实僵尸码是 110**：
    **A 类 69**（有端点但无权限注解）+ **B 类 41**（端点不存在）。
    `permission-effectivity.json` 已重跑并写入（后端 `GET /api/permission/effectivity` 读的就是它）。
  - **🔴 根因：僵尸码被**虚增**了一半 —— 生效性扫描的正则漏了 `@RequiresPermission`（少一个 s）**
    本仓真正在用的注解是 core-base 的 **`@RequiresPermission`**（配 `PermissionAspect` 的
    `@Around("@annotation(...RequiresPermission)")` **真实拦截**），共 **15 个控制器 / 92 个码**；
    而 `tools/gen-permission-effectivity.py` 里的正则写的是 `@RequirePermission`（无 s）⇒
    **这些已被真实注解保护的码全被判成"没有消费方"**。hr 域那 35 个"僵尸码"全是这么来的
    （`HrController` 里就有 77 处 `@RequiresPermission("hr:…")`）。
    已在生效性脚本与分类脚本**两处**修正（两种拼写都收），重测：**159 → 110**（hr 归零）。
    教训：**判"僵尸码"的工具本身要先被验证**，否则会凭空指挥出一场不必要的"重建"。
  - **逐条定性（新增 `tools/classify-zombie-permissions.py`，产出 `tools/zombie-permissions.json`）**：把每条僵尸码判成「有端点但没注解」（**修正后 69 条**）还是「端点不存在」（**修正后 41 条**）。
    - **A 类 113 条按域**：`hr 35 / erp 13 / crm 12 / product 12 / md 7 / finance 6 / department 5 / permission 4 / position 4 / role 4 / tenant 4 / doc 2 / user 2 / purchase 1 / sale 1 / set 1`。**处置 = 补 `@SaCheckPermission`（这是 E-01 的活，不是 E-02 能"修"的）**；且**必须先确认码已在库**（本仓铁律：没有码就补注解 ⇒ 该接口对所有非超管一律 403）。
    - **B 类 46 条**：`data-permission:* 8 / system:dataimport:* 6 / system:dev:scheduler:* 5 / crm 3 / doc 3 / hr 3 / product 4(价格视图类) / finance 2 / 其余零散`。**⚠️ 这批不能照单删**：抽样即发现**假阴性** —— 例如 `system:dev:scheduler:*` 其实有 `SchedulerController` 系列控制器（路径 `/api/scheduler/*` 与 `system` 前缀无关，分类器按域前缀匹配不到），**照单删码会把本该接线的码删掉**。故 B 类清单只能当"待人工逐条确认"的工作单，**未做任何删除**。
  - **为什么这一轮不直接施工**：A 类 113 条的施工量等同 E-01 的剩余批次（hr/crm/core-api/erp-finance 四个模块约 10 个控制器目录），B 类 46 条需逐条人工确认；两者都不是"顺手改几行"，且误删/误注解的代价分别是"功能永久 403"与"删掉还要的码"。**这是本轮唯一没做完的项**，下一步是 E-01 批次（先补码后注解）+ B 类人工确认单。
  - **🔴 接上一条续做后的关键实测（2026-09-21）：生成器对这批「历史手写码」域不适用**
    `tools/gen-module-permission-seed.py` 已按上一轮结论**补进 7 个新模块**（hr / crm / coreapi /
    erpfinance / dms / sales / purchase，slot 9~15），并把 `scan`/`apply_annotations` 从
    `os.listdir`（**只扫一层**，导致 `crm/contract/controller`、`erp-finance/expensedoc/controller`
    整片扫不到）改成**递归**，`dirs` 因此只需给模块根；同时给 `--apply` 加了
    **铁律硬校验**（推导出的码必须存在于 `sys_permission`，否则**不插注解**并列 missing ——
    这条以前只靠人记）。
    **然后一测就发现根本问题**：把推导码与库中历史码逐条比对——
    | 模块 | 端点数 | 推导码 | **命中库中已有** | 推导出的新码 | 该域库中码 |
    |---|---|---|---|---|---|
    | hr | 92 | 18 | **8** | 10 | 40 |
    | crm | 185 | 67 | **8** | 59 | 57 |
    | core-api | 530 | 242 | **16** | 226 | 104 |
    | erp-finance | 405 | 216 | **85** | 131 | 96 |
    | sale / purchase / dms | 224/143/270 | 105/78/112 | **10/18/13** | 95/60/99 | 18/23/23 |
    根因：**历史码粒度更细**（`hr:leave:approve`、`crm:contract:batchapprove`），而路径推导只能给到
    `<域>:<资源>:<动作>`（`hr:update`、`crm:contract:update`）⇒ **两套命名不可自动对齐**。
    这也解释了为什么 E-01 上一批（7 模块）能成功：那批是 E-04 的**零码模块**，码由生成器现场
    创建，推导与库天然一致；**老模块没有这个前提**。
    ⇒ **不能用 `--apply` 修 A 类**：带着 DB 校验跑，只能命中 8~85 条、其余 700+ 端点照旧没注解；
    关掉校验跑，会插进 700 多个**库里没有的码** ⇒ 那些接口对**所有非超管 403**（正是铁律禁的事，
    已实测确认是 700+ 而不是几个）。
  - **⏸ 待拍板（A vs B，二选一，不能混着做）**：
    **(A) 以库为准**（尊重历史码）：逐个僵尸码人工/半自动找到它对应的端点再插注解。语义最准，
    但一个端点常对应多个码（`hr:leave` 的 create/update/delete/approve 打在同一组端点上），
    需要按 HTTP 方法细分或改成 `@SaCheckPermission(value={...}, mode=OR)`，工作量大且结果依赖人工判断。
    **(B) 以代码路径为准**（重建码库）：按生成器规则重写这几个域的码（迁移：删旧码 → 插新码 →
    迁移角色授权），再用 `--apply` 一次性补注解。可自动化、可复跑，但**会改变现有角色授权矩阵的语义**
    （各角色需要重新分配新码），属权限模型重构，风险集中在授权迁移那一步。
    **我倾向 (B)**：这批历史码本来就是"码与端点对不上"的产物（E-02 的定义），(A) 是逐条打补丁，
    下次加接口又会重新错位；(B) 一次对齐后生成器能在 CI 里持续守住。但它动授权，需要你点头。
  - **✅ 已拍板（2026-09-21，用户）：走 (B) —— 以代码路径为准重建码库，按模块推进，从 hr 开始。**
  - **⛔ 但 hr 这一批执行后**已主动回滚**（当日）**：动手后才发现上面那条"根因"——hr 根本不是
    无注解模块，它用 `@RequiresPermission` 保护得好好的，35 个"僵尸码"是工具误判。
    我为 (B) 生成的 `V11.451.0__Rebuild_Hr_Permissions.sql`（+27 码 / −3 码）**已按反向 SQL
    完整回滚**（删掉 27 个零引用新码、补回 3 个被误删且有真实引用的码），
    迁移文件也已删除，**未进 flyway_schema_history**；hr 码数回到 **40**、3 个引用码全部在位。
    **结论**：hr 不需要重建。(B) 是否还要做、对**哪些**域做，要等用修正后的口径重看 110 个僵尸码
    （A 69 / B 41）再定 —— 很可能只需要补那 69 个缺注解的端点，而不是重建任何码库。
  - **🔴 继续复核后又推翻一次：分类器给的"候选端点"不足以直接施工（当日）**
    拿最小的一组试手（`md:product-price:*` 共 7 个码，全部指向同一个控制器
    `ProductPriceController`），一读代码就发现：**这些端点早就被别的码保护着** ——
    `/page`、`/export`、`/brands`… 上写的是 `@SaCheckPermission("erp:product:list")`、
    `/batch-modify` 上是 `erp:product:price-batch`。也就是说 `md:product-price:*` 这 7 个码是
    **E-04 生成器"过度生成"的产物**（生成器按路径给端点造了码，而端点已有历史码在守），
    它们的正确处置是**删码**（端点已被保护，删掉这个没人用的码不改变任何行为），
    而**不是**按分类器的候选去插注解 —— 插了就会在同一方法上出现两个权限注解，
    且新码只被超管持有 ⇒ 其他角色当场 403。
    把 69 个 A 类按"候选端点是否已被别的权限注解保护"再切一刀：**冗余 22 条（可删）/
    真缺口 47 条（需人工判断后补注解）**。但**这 47 条同样不能照候选施工**：
    抽查 `md:product-price:list/view/export` 时分类器把它们的候选端点判成"无注解"，
    而实际那三行上方就写着 `erp:product:list` ⇒ **候选匹配（按路径子串猜）本身就不可靠**。
    **结论（方法论层面，写下来给后续所有人）**：这 110 条只能**逐条读控制器源码**来定
    "删码 / 补注解 / 补哪个码"，自动化只能用来**缩小范围**（把 110 缩到"疑似冗余 22 + 疑似缺口 47"），
    不能用来下结论。这也正是当初 E-01 那批（97 控制器）能成的道理 —— 那批是**零码模块**，
    端点与码一一对应、不存在"已有别的码在守"的歧义。
    **⚠️ 另需一并定夺**：本仓存在**两套注解机制**（Sa-Token 的 `@SaCheckPermission` 用在前 97 个控制器、
    core-base 的 `@RequiresPermission` 用在 15 个控制器），二者都有真实拦截 —— 属用户诉求 #2
    「同样功能的代码有新旧两套要统一」，应与 (B) 一起裁定收敛到哪一套。
  - **✅ 已拍板（2026-09-21，本次）：注解机制统一收敛到 Sa-Token 的 `@SaCheckPermission`**，理由：
    ① 它是框架原生注解（`SaInterceptor`/全局异常处理/通配 `*` 都由 Sa-Token 负责），
    本仓**已有 97 个控制器**在用，是绝对多数；② `@RequiresPermission` 是 core-base 的薄包装
    （`PermissionAspect` 内部同样调 `StpUtil.getPermissionList()` 再自己比），**不提供额外能力**，
    却多了一条 AOP 链路和一个自定义异常出口；③ 收敛方向选"少数改多数"才划算。
    **但 15 个控制器的 `@RequiresPermission` → `@SaCheckPermission` 的替换要单独一批做**，
    因为它会改变**异常类型与响应体**（`BusinessException` vs Sa-Token 的 `NotPermissionException`），
    需先确认前端/全局异常处理器对两者的响应码一致，属于"看起来机械、实际会动契约"的改动。
  - **为 (B) 先把生成器的推导质量补齐了（否则重建出来的码库比历史码更粗，等于降级）**：
    ① **资源回退**：类级路径没给资源时，改从**方法路径的首个非动作段**取资源。本仓 `HrController`
       的 base 就是 `/api/hr`（全部实体写在方法路径上），不做这层回退时 92 个端点只产出 18 个码，
       其中 `hr:update` 一个码覆盖 19 个端点 ⇒「能改职位的人也能改工资结构」，**粒度比历史码还粗**。
    ② **单复数对齐**：新资源名按该域**库中已有码的词表**保守对齐（`positions`→`position`），
       只在不含歧义时替换（不动 `status`/`statistics` 这类以 s 结尾的词）。目的：让新码与前端
       `v-permission` 里的旧字符串尽量同名，减少接线改动。
    ③ **补 PUT/POST 的动作规则**：本仓大量动作端点是 **PUT**（`PUT /hr/leave/{id}/approve`、
       `PUT /hr/performance/{id}/confirm`），原来只按 POST 写规则 ⇒ 这些全落到 `update` 兜底，
       把「审批」与「编辑」**合并成同一个码**（丢掉职责分离）。现补 approve/reject/audit、
       confirm/verify、generate、cancel/close/publish/execute、status（PUT）与
       generate/interview/clock（POST），动作词**全部来自库中已有历史码**，不是新造。
    **hr 域实测效果**：端点 92 → 码 **18 → 64**；与库中 40 个历史码的**交集 18 → 37**，
    需要删的 **18 → 3**（仅剩 `hr:attendance:rule` / `hr:candidate:interview` / `hr:leave:quota`
    这三个"子资源当动作"的历史写法，映射到各自的派生兄弟码）。
  - **hr 域 (B) 迁移的低风险依据（已核实）**：① 40 个旧 hr 码**全部只被「超级管理员」持有**
    （无任何租户角色受影响）；② 前端 `v-permission` 里 **hr:\* 零引用**（grep 无命中）⇒ 不需要改前端。
    故 (B) 在 hr 上是「元数据重建 + 注解补齐」，不改变任何角色的实际可访问性。
  - **顺带修掉生成器自身的两个缺陷（本轮实测踩到）**：
    ① **幂等判定只看前一行** ⇒ 本仓方法上注解有两种写法（`@Operation/@SaCheckPermission/@GetMapping`
    与 `@Operation/@PostMapping/@SaCheckPermission`），后一种被判成"没注解" ⇒ **重复插入同一条注解**
    （试跑销售域实测：一次 `--apply` 在 `SaleOrderController` 里插出 4 处重复）。现改为**双向窗口**
    （往前 + 往后各看若干行，遇空行/方法体边界即停）。
    ② **方法级幂等复用了"类级覆盖"的正则**（含 `@SaCheckLogin`）⇒ 会把"只要登录即可访问"当成
    "已有权限控制"而**整片跳过**（看起来跑了、实际什么都没补）。现拆成两个正则：
    类级覆盖用 `ANY_AUTH`（任一鉴权注解即可），方法级幂等只用 `PERM_ONLY`（`@SaCheckPermission`/`@RequirePermission`）。
    修完后在同一模块复跑：**插入数从 22 降到 4、重复注解 0**。

#### E-03 [P1] `sys_permission.api_path` 只填 238/474

- **修法**：补全映射（若依式「权限码↔接口」），或明确废弃该列。
- **状态**：🔄 **大部分已完成（2026-09-21）** —— 实测 **238/474 → 734/994（74%）**。
  本轮生成器在建码时**顺带写入** `api_path`/`method`（取该码扫描到的第一个代表端点），
  即「补 E-04 的同时把 E-03 一起解决」，没有另做一遍。
  **剩余 260 条已清点（2026-09-21）**：按域 `crm 53 / system 43 / finance 34 / dms 23 /
  platform 19 / datasource 16 / sale 10 / purchase 8 / data-permission 8 / erp 8 /
  permission-template 7 / doc 5 / product 4 / tenant 3 / log 3 / 其余零散`。
  **规律很清楚**：这 260 条集中在 `tools/gen-module-permission-seed.py` **从未覆盖的模块**
  （生成器的 MODULES 只有 budget/fixedasset/stock/marketing/party/invoice/payment 七个 +
  wms/b2b 仅出清单）⇒ 补全 = 把这几个模块补进生成器，再按「端点 → 码」回填 `api_path`。
  **本轮进度（2026-09-21 续做）**：把这 6 个域补进生成器后（见 E-02 的实测表），
  用推导结果**只回填能对齐的**（推导码必须已在库中，否则不写）⇒ 迁移
  `V11.450.0__Backfill_Permission_Api_Path_Part2.sql` 补 **47 条**，
  实测 `api_path` **734/994 → 781/994（79%）**。该迁移**纯元数据**：不新增码、不动角色授权、
  不动任何注解 ⇒ 不改变任何接口的可访问性（唯一消费方 `checkApiPermission` 无内部调用方且 fail-closed）。
  剩余 213 条**无法从推导对齐**（原因同 E-02：历史码粒度与路径推导不一致）——
  它们要么随 (B) 方案一起重建，要么由 (A) 方案逐条人工登记路径。

#### E-04 [P1] 9 个模块 0 权限码

- **清单**：stock / wms / marketing / b2b / payment / party / budget / invoice / fixedasset；另 analytics / report 零权限。
- **依赖**：补码是 E-01 的前置（铁律）。
- **状态**：🔄 **7/9 已完成并验证（2026-09-21）**，剩 wms / b2b 需先拍板：
  - **已完成**：budget(36) / fixedasset(40) / stock+product+md(202) / invoice(14) /
    payment+finance(37) / party+partner+md(59) / marketing(98) —— 共 **485 个新码**，
    迁移 `V11.441.0`（budget）+ `V11.442.0`~`V11.447.0`（六模块），全部 success。
  - **工具**：新增 `tools/gen-module-permission-seed.py` —— 按**类级路由路径**推导「域:资源:动作」
    （不能按模块目录推导：`erp-stock` 目录里装的是 `/api/erp/product/*`、`/api/erp/md/*` 控制器，
    `wms` 里混着 `/api/v1/warehouse/*`，`erp-mall` 里既有后台 `/api/erp/mall/admin/*` 又有 C 端
    `/api/v1/mall/*`）。`--apply` 可幂等地把注解插到映射注解之前。
  - **口径**：一码一「资源×动作」（对齐既有：`PurchaseOrderController` 16 端点→8 码），
    动作词复用库中已有的，不新造同义词。
  - **域命名规范（2026-09-21 定案，当场归一）**：域 = 类级路由路径去掉 `/api`、跳过 `erp`/`v1`
    容器段后的**第一段**（首段含 `-` 且前缀是已知域时拆开，如 `product-category` → `product:category`）。
    **不允许按模块目录推导** —— 二者在本仓不对应（`erp-stock` 目录里是 `/api/erp/product/*`、
    `/api/erp/md/*` 控制器；`wms` 里混 `/api/v1/warehouse/*`；`erp-mall` 里后台与 C 端混装）。
    同名不同物用**路径级覆盖**（`PATH_OVERRIDE`）解决，不用段级猜测。
    遗留的 `partner` 域与 `payment` 语义混用已由 `V11.448.0` 一并归一。
  - **未做（需拍板）**：`wms`（PDA 设备鉴权策略未定，设备端不能直接套员工账号口令模型）、
    `b2b`（`/api/v1/mall/**` 是 C 端公开接口，自动补注解会把商城顾客挡在门外，
    需逐个甄别哪些必须公开）。工具里已标记为 `manual_only`，不会误改。
  - **另**：`analytics` / `report` 零权限仍待补（本轮未覆盖）。
- **⚠️ 防僵尸码红线（本轮已验证守住）**：补码必须与补注解同批，否则就是凭空造僵尸码（加重 E-02）。
  实测：新增 485 码后，未生效数仅 144 → **159（+15）**，即新码约 **97% 真接线**。

#### E-05 [P2] 记录规则 `sys_record_rule` 无查询消费方

- **证据**：`buildDomainFilter` 仍无人调用；表 0 行。
- **状态**：⬜

#### E-06 [P1] 非超管角色的菜单授权为 0（与业务断点 7.1 同一件事，见 权限区域模块）

- **见** 平台-AUTHZ-01。

---

### 3.3 专项 F：双层权限（系统级 / 租户级）

> **核心结论：本项目不是两条清晰的授权链，而是「一个拦截器 + 一个会话角色码」的布尔开关。**

#### F-01 [P0] `tenant_id = 0` 与「平台租户 = 1」两个语义混用

- **证据**：系统级数据（`sys_menu` 416 行、`sys_config` 173 行、`sys_permission` 105 行）落在 `tenant_id = 0`；而代码硬编码 `SYSTEM_TENANT_ID = 1L`（`SysMenuServiceImpl.java:48`），DB 里 `id=1` 是 `tenant_code=SYSTEM / 系统租户`。
- **影响**：这是 F-02~F-06 多条问题的总根源。
- **✅ 已查清（2026-09-20 实测）**：
  - `admin` 用户 → `tenant_id=1`、`is_super_admin=true`、status=1；角色 `SUPER_ADMIN` 也在 `tenant_id=1`。
  - 租户 1 = `SYSTEM / 系统租户`（status=1）⇒ **代码硬编码 `SYSTEM_TENANT_ID=1L` 是正确的，不用改**。
  - 租户 0 = `tenant_mqd4cr9h`，**status=0 已禁用、无 admin_user_id** ⇒ **不是真租户，是注册测试残留**。
  - 所以 `tenant_id=0` 的真实身份是**「全局/平台共享数据」的容器**，不是租户。
- **`tenant_id=0` 上实际混着三类数据（实测分布）**：

  | 类别 | 表与行数 | 处置 |
  |---|---|---|
  | **设计上就该在 0 的全局数据** | `sys_menu` 416、`sys_config` 173、`sys_permission` 105、`dms_config` 57、`sys_tenant_package` 3、`sys_module` 6、`erp_product_grade` 8、`erp_member_level` 4、`sys_security_policy` 1、`sys_storage_config` 1、`sys_tenant_menu` 4 | **保留**，并把"0 = 全局"语义固化到代码/规范 |
  | **本应属于租户的数据** | `sys_login_log` 79、`workflow_node` 19 + `workflow_task` 15 + `workflow_instance` 5 + `workflow_definition` 4、`scheduled_task` 16 + `scheduled_task_log` 15、`sys_role_permission` **16**（正是"角色在租户 1、关联行在 0"那批）、`biz_party` 14 | **需按真实租户回填**（先定归属再改） |
  | **测试脏数据** | `sys_user` 3（`apitest_*`/`e2e_test*`，全 status=0）、`sys_department` 3（E2E） | **清理** |

- **语义定义（用户 2026-09-20 确认，本文以此为准）**：
  > `tenant_id = 0` = **「租户初始化数据容器」** —— 存放给新租户的默认数据：
  > 默认角色、以及租户初始化所需的其他信息。新租户从 0 号容器复制/派生自己的初始数据。

  实测现状与该定义**部分吻合**：已实现的"权限模板"（`sys_permission_template` → `applyTemplateToRole`）
  就是这个容器的一个实例（见 平台-BREAK-04）；但**菜单授权、租户菜单授权没有走模板**，是该定义下缺失的部分。
- **动作**：
  1. 作废/清理租户记录 0（`tenant_mqd4cr9h`）—— 它是 status=0 的注册残留，**不应作为租户存在**；
     但其中作为"初始化模板"的数据要保留（作废租户记录 ≠ 删 0 号数据）；
  2. 把语义写进规范：**`0` 表示"初始化模板/全局默认数据"，`1` 表示平台租户（SYSTEM，admin 所在）**；
     全局表读用 `@InterceptorIgnore` + 显式 `tenant_id = 0`；
  3. 按此语义补齐缺失的初始化项（菜单授权等，见 平台-BREAK-04）；
  4. 清理上面第二、三类数据（**先逐表确认归属，勿批量 UPDATE**）。
- **状态**：🔄 语义已定，数据清理与初始化补齐待逐表确认

#### F-02 [P0] `X-Tenant-Id` 头由客户端完全决定，后端无任何与会话比对

- **证据**：
  - 前端无条件注入且**默认回落 `'1'`**：`pc-admin/src/utils/request.ts:127-129`、`api/admin.ts:26`。
  - 后端 **21 个文件 / 104 处**消费该头，无任何全局校验。
  - 头优先于会话：`PrintTemplateV2Controller.java:39-52`、`SystemConfigServiceImpl.java:540-545`、`ReportScheduleController.java:197-200`。
  - 直接落库：`WorkflowController.java:105-107` → `WorkflowServiceImpl.java:126`（工作流四表在 ignore 清单，且实体不继承 BaseEntity 无 fill → **头值是唯一租户判据**）。
- **影响**：改 localStorage 或直接 curl 带头，即可读写别的租户（含平台租户 0/1）的流程定义、打印模板、报表、数据源、系统配置。
- **修法**：SaInterceptor 之后加全局 `HandlerInterceptor` 比对头与会话租户（超管豁免）；104 处消费点改从会话取；前端去掉 `'1'` 回落。
- **状态**：✅ **已实施（2026-09-20），待运行验证**
  - 新增 `cn.aiedge.base.security.TenantHeaderInterceptor`：头缺失/未登录/超管/无会话租户 → 放行；其余账号头值 ≠ 会话租户 → **403**（宁可拒绝也不静默改写，避免越权读取看起来成功）。
  - 注册进 `SaTokenConfig`，`order(2)`（排在鉴权之后）。
  - 前端 4 处去掉 `|| '1'` 回落：`utils/request.ts:127`、`api/admin.ts:26`、`api/marketing.ts:44`、`views/dms/realtime-tracking/index.vue:819`
    —— 拿不到租户时**不发该头**，交由后端使用会话租户。

#### F-03 [P0] `/api/config/**` 整表 `@InterceptorIgnore`，租户管理员带头 `X-Tenant-Id: 0` 即可改全局配置

- **证据**：`SysConfigMapper.java` 逐条 `@InterceptorIgnore(tenantLine="true")`（65/91/118/127/132/158/167/187/199/204 行）；`:187` 的 `UPDATE sys_config ... WHERE deleted=0 AND id=#{c.id}` **无 tenant_id 条件**；作用域全由头决定（`SystemConfigServiceImpl.java:540-545`）。`sys_config` 173 行**全在 `tenant_id=0`**。
- **修法**：`effectiveTenant` 改为「仅超管可用头，其余强制会话租户」（照抄 `TenantModuleController.java:76-92 resolveReadableTenantId`）；`updateConfig`/`logicDeleteById` 补 `tenant_id` 条件。
- **🔍 本轮深挖（与初判不同，更严重）**：`sys_config` 的唯一约束是 **`UNIQUE(param_key)`（不含 tenant_id）**，
  且 173 行**全在 `tenant_id=0`** ⇒ `selectByKey` 永远命中那唯一一行（`tenant_id IN (0,?)` 只能命中 0 行）
  ⇒ **租户管理员保存配置，实际改的就是全局配置**（影响所有租户），而不是"改自己租户"。
  也就是说：**当前表结构根本容不下"租户覆盖"**（mapper 注释也承认这点）。读路径的
  `tenant_id IN (0, #{tenantId})` 写法是对的，但受唯一约束限制，"覆盖"无从存在。
- **已完成（本轮）**：`effectiveTenant` 加固 —— 非超管**一律强制会话租户**，传入的头值忽略并告警；
  超管仍可用头（含 0 = 全局）。这一步堵住"任意指定租户"。
- **待拍板（完整实现"全局默认 + 租户覆盖"）**：
  1. 迁移去掉 `UNIQUE(param_key)`，改为 `UNIQUE(param_key, tenant_id) WHERE deleted = 0`（顺带修掉"软删后不能重建同键"）；
  2. `selectByKey` 排序改为**租户行优先**（现为 `ORDER BY tenant_id ASC` = 全局行优先，与覆盖语义相反）；
  3. `selectConfigs` 需按 `param_key` 去重（`DISTINCT ON`），否则同一键会出现两行；
  4. 写路径：租户管理员只写自己租户的行，超管写 0 行。
  **替代方案**：若判定"系统参数是平台专属功能"，则只需把 `system:config:*` 码收归平台角色，无需改表。
- **状态**：🔄 越权头已加固（✅）；租户覆盖待拍板（⏸）

#### F-04 [P1] 平台侧 `/api/tenant/**` 读接口可被任意租户用户调用

- **证据**：`TenantController.java:42-47` 类级只有 `@SaCheckLogin`；`getPage`(:59-87)/`getById`(:95-103)/`getConfig`(:229-246) 无权限注解；`sys_tenant` 在 ignore 清单 → 无租户条件。写接口反而有码（`:110/:125/:195`）。
- **影响**：任意登录用户可枚举全部租户档案（含联系方式、`admin_user_id`、到期时间）。
- **修法**：补 `system:tenant:list`/`query` 码；service 层非超管收敛为仅本租户。
- **状态**：✅ **已实施（2026-09-20），待运行验证**（顺序遵守「先种子后注解」铁律）：
  1. 迁移 `V11.438.0__Seed_Tenant_Query_Permissions.sql`：补 `system:tenant:list`(90066)、
     `system:tenant:query`(90067)，并**显式关联超管角色**（本仓超管权限不是硬编码放行，
     而是来自 `sys_role_permission`，不关联则平台管理员自己也会被拒）；
  2. `TenantController` 三个读接口补注解：`getPage` → `system:tenant:list`，
     `getById`、`getConfig` → `system:tenant:query`。
  **刻意未加**：`GET /current`（租户侧读自己公司信息，加了会让租户管理员看不到本企业档案）。

#### F-05 [P1] 平台/租户两层在权限码维度上无隔离，提权链完整

- **证据**：`sys_permission` / `sys_role_permission` **都在 ignore 清单**（全局表）；`SysRoleServiceImpl.assignPermissions`(:75-130) 对 `permissionIds` **不做任何白名单/归属校验**；DB 实证租户 2 的角色已持 `system:user:list/detail`（这些码落库时 `tenant_id=1`）。
- **提权链**：租户管理员拿到 `system:role:assign-permission` → 把 `tenant:menu:assign` 授给自己 → 调 `SysTenantMenuController` 改写**任意租户**菜单授权。**当前未被利用，但链是通的。**
- **修法**：给权限码引入"作用域"（平台专属 / 可授租户）字段，`assignPermissions` 按调用者身份过滤；或在平台侧接口加硬校验，不依赖权限码。
- **需拍板**：`system:*` 码是否允许授予租户角色？（迁移注释把"租户管理员自行勾选"当预期）
- **状态**：⏸ 部分需拍板

#### F-06 [P1] `SysTenantMenuController` 只认权限码，不认平台身份

- **证据**：四个端点的 `tenantId` 全部来自 `@PathVariable`，注解仅 `@SaCheckPermission("tenant:menu:*")`，类里**没有 import 任何身份判定工具**；写入 `SysTenantMenuMapper.xml:36-40` 用标量 `#{tenantId}`（刻意绕开 fill），删除 `:47-50` 是**物理删**全量。
- **修法**：service 入口加平台管理员硬校验（`isTenantScopeExempt()` 或 SUPER_ADMIN），非平台管理员 403。
- **状态**：✅ **已实施（2026-09-20），待运行验证** —— `SysTenantMenuController` 新增私有方法
  `assertPlatformAdmin()`（非 `isTenantScopeExempt()` 即抛 `BusinessException.forbidden`），
  四个端点（query/assign/remove/clear）全部前置调用。理由写在方法注释里：权限码可被授予租户角色，
  而 `sys_tenant_menu` 在忽略清单、写入走标量 `#{tenantId}`，只靠码挡不住。

#### F-07 [P1] 租户级菜单授权这一层数据为空，第二层形同虚设

- **证据（SQL）**：`sys_tenant_menu`：`tenant_id=0` → 4 行，`tenant 1/2` → **0 行**；`sys_role_menu`：`tenant_id=1` → **3 行**，其余 0。`sys_menu` 416 行在 `tenant_id=0`。
- **代码路径**：`SysMenuServiceImpl.java:279-291` 两级取交集；租户 2 交集为空 → `:302-304` 返回空菜单。
- **影响**：任何真实租户的"平台授权给租户的菜单"都没数据；结合平台-AUTHZ-01，租户用户导航只剩前端静态路由兜底。
- **✅ 已拍板（2026-09-21）**：随 平台-AUTHZ-01 一并由权限派生取代，`sys_tenant_menu` 退出菜单主链路
  （`tenantId` 归属之争随之作废，不再需要裁定 0 还是 1）。
- **状态**：🔄 实施中

#### F-08 [P1] 匿名 `/api/file/view/**` 无租户维度

- **证据**：白名单 `SaTokenConfig.java:48,90`；实现 `FileUploadController.java:108-126` 只做路径穿越防护，**路径里没有租户层级**。而做了租户校验的 `FileAccessController`（`/files/**`）**不在白名单**——安全的那份管不到曝光的那份。
- **影响**：未登录者凭 `yyyy/MM/dd/{uuid}.ext` 可读任意租户上传的证件/附件/头像（UUID 提供一定不可猜性）。
- **修法**：路径改 `{tenantId}/...` 并校验归属；或合并两份实现，统一到安全的那份。
- **需拍板**：是否接受"UUID 即权限"？
- **状态**：⏸ 需拍板

#### F-09 [P2] 其余加固清单（均有位置，未逐一实测）

- `@InterceptorIgnore(tenantLine="true")` 是第二张更隐蔽的忽略清单：**30 文件 / 76 处**（含 `SysUserMapper.java:24`、`SysMenuMapper.java:59,65`、`MailConfigMapper.java:27`、`SecurityPolicyMapper.java:27`、`SmsConfigMapper.java:17`、`DmsChannelMapper.java:46`）。
- `setTenantId(1L)` 字面量散落 stock/purchase/finance（`StockInServiceImpl.java:157`、`PurchaseInboundServiceImpl.java:294`、`PaymentController.java:128` 等），无会话上下文时写死平台租户。
- `SysTenantMenuMapper.deleteByMenuId`(:41 / XML :53-56) **无任何调用方** ⇒ 删菜单不清租户授权，遗留脏授权。
- `NotificationController`（core-notification）信任 `X-User-Id` 头（:31-35），可越权读写他人通知。
- **34 张表的 `tenant_id` 是 varchar**，拦截器却注入 `LongValue`：`budget_*`/`expense_*`/`invoice_*`/`fixed_asset_*`/`erp_supplier_*`/`erp_inquiry_quotation`。对 `erp_inquiry_quotation` 执行 `WHERE tenant_id = 0` 直接报 `operator does not exist: character varying = integer`，**PG 中会中止整个事务** ⇒ 表现为"整单 500"。
- **做对的地方**（勿动）：`TEMP_TENANT_ID` ThreadLocal 的 13 处 set 全部有 finally clear，未发现线程池串租户。
- **状态**：⬜

#### F-10 [P2] 前端"切换租户"只改 localStorage，不改会话租户

- **证据**：`BasicLayout.vue:849-865` 只做 `localStorage.setItem` + `router.push`，无后端调用；会话租户只在登录时写一次（`SysUserServiceImpl.java:119`）。另有多处 `|| 1` 硬编码回落（`dynamicRoutes.ts:1073`、`stores/user.ts:108,113,172`、`request.ts:358`、`tokenRefresher.ts:142`）。
- **影响**：对拦截器自动注入的 460 张表**完全无效**，只对 F-02 的 21 个头消费模块生效 → 行为不一致。
- **需拍板**：「切换租户」是切换会话身份（需重签 token，仅超管可切）还是只切换视图过滤？
- **状态**：⏸ 需拍板

#### F-11 [P1] SoD（职责分离）表空、权限模板无 UI 等

- `sys_sod_rule` 0 行（校验已挂对入口，规则为空时零影响）；`sys_permission_template` 2 行无 UI；`sys_field_permission` 0 行且 **core-platform 的 `FieldPermission` 实体列名与表不符**（`modelName/fieldName/groupId` vs 真库 `role_id/target_table/target_field`）⇒ 一旦被调用即报列不存在。
- **状态**：⬜

#### 平台-AUTHZ-01 [P1] 非超管角色的菜单授权为 0

- **证据（SQL）**：`sys_role_menu` 总共 **3 行**：SUPER_ADMIN(1) 有 3 条；`SYSTEM_ADMIN`/`DEPT_ADMIN`/`E2E_T2_ADMIN` **均为 0**。而接口权限 `sys_role_permission` 有 536 行（18/16/2 分布）。
- **代码**：`SysMenuServiceImpl.java:279-289` 普通租户走 `sys_tenant_menu ∩ sys_role_menu`，空交集 → `:299 log.warn("用户没有可访问的菜单")` 返回空数组；超管靠 `:270` 硬编码早退才拿到全菜单。
- **影响**：除超管外所有角色登录后菜单接口返回空，导航靠前端静态路由兜底（因此不易被发现）；新增角色/租户会立即复现。
- **根因**：**两套授权模型两处存、只维护一处**（接口权限有人维护、菜单授权无人维护）。
- **修法**：角色保存时同步写 `sys_role_menu`，或改由 `sys_permission` 派生菜单可见性；统一超管早退分支。
- **✅ 已拍板（2026-09-21，用户）**：**从 `sys_permission` 派生菜单可见性**，不再双维护 `sys_role_menu` / `sys_tenant_menu`。
- **🔬 派生可行性实测（2026-09-21）**：`sys_menu` **没有** `permission_code` 列（28 列已核）；
  307 个叶子菜单的 `menu_code` 里**只有 2 个**能精确命中权限码 —— 但**前缀规则成立**：
  菜单 `finance:other-income-doc` ↔ 权限 `finance:other-income-doc:view` 可派生。
  按前缀规则实测可见菜单数：SUPER_ADMIN 500 码 → 覆盖面广；SYSTEM_ADMIN(18 码) / DEPT_ADMIN(16 码) → **仅 3 个**；
  E2E_T2_ADMIN(2 码) → **0 个**。
  ⇒ **结论：「从权限派生」的瓶颈不是服务端逻辑，而是权限码库本身只覆盖 31/500 个 `erp:*`**
  （stock/wms/marketing/b2b/payment/party/budget/invoice/fixedasset 九个模块零权限码，见 E-04）。
  **正确顺序**：E-04 补码 → E-01 补注解 → 再切换菜单派生的默认口径。
- **未映射菜单的过渡口径（本轮实施）**：菜单码无对应权限时**保持可见**（后端 API 鉴权才是真正的访问控制点，
  菜单只是导航）；有对应权限的菜单则按权限显隐。这样既终止空菜单，又不因覆盖率不足而让导航"变少"。
- **状态**：🔄 实施中

---

### 3.4 财务域（erp-finance）

#### FIN-BREAK-01 [致命 P0] 总账虚增，利润表数字是假的

- **证据（同年度两套口径实测）**：

| 科目 | 凭证分录口径 `finance_voucher_item` | 总账口径 `finance_ledger` |
|---|---|---|
| 6001 主营业务收入（贷） | 2,500 | **18,230** |
| 1122 应收账款（借/贷） | 2,500 / 500 | **17,900 / 22,000** |
| 1403 库存商品（借/贷） | 255 / 0 | 4,815 / 3,120 |

  分期间：2026 期1 凭证 1,000 vs 总账 4,000（4 倍）；期9 2,666 vs 44,666（约 17 倍）。凭证借贷本身平衡（57/57）。
- **代码根因**：`finance_ledger` 唯一写入方 `LedgerServiceImpl.postToLedger()`（由 `VoucherServiceImpl.java:251` 过账调用）是**纯累加式**（`LedgerServiceImpl.java:448` `setPeriodDebit(existing + debit)`），**无按凭证重算/回滚入口**；唯一重算方法 `closePeriod()`（`:482`）**全仓无调用方**。凭证删了总账永久残留。
- **取数源分裂**：`TrialBalanceMapper.java:44` 读 `finance_voucher_item`（正确）；`FinancialReportServiceImpl.java:47,68,188,547,809,876` 读 `finance_ledger`（虚增）⇒ **利润表/资产负债表/总账账簿数字全错**。
- **修法**：① 新增"按凭证重算 `finance_ledger`"入口并执行一次；② `closePeriod` 接线或删除；③ 全报表统一到「凭证 → 总账」单向派生。
- **需拍板**：财务口径由谁确认；重算以凭证为准是否认可。
- **状态**：⏸ 修法明确，待财务确认后执行

#### FIN-BREAK-02 [P1] 凭证头 `summary` / `source_no` 永不写入（字段名对不上）

- **证据**：`finance_voucher` 69 行，`summary` **0/69**、`source_no` **0/69**、`handler_name`/`dept_name` 0/69；但**行级** `finance_voucher_item.source_no` 137/145 有值。
- **根因**：`BusinessAccountingServiceImpl.java:100-104` 组装时只 `setRemark(request.getSummary())`，**没有 `setSummary(...)` 也没有 `setSourceNo(...)`**；而 `VoucherServiceImpl.java:79,82` 读的是 `dto.getSummary()` / `dto.getSourceNo()` —— **remark ≠ summary**，来源单号整条漏传。
- **影响**：凭证列表"摘要/来源单据"列恒空；**无法从凭证反查业务单据**（`VoucherQuery.sourceNo` 条件永远筛不出）。而调用方 `ExpenseDocServiceImpl.java:289,466`、`CashTransferServiceImpl.java:381`、`ArApAdjustServiceImpl.java:339` 都老实 `setSourceNo(doc.getDocNo())` —— **白传**。
- **修法**：网关补 `setSummary`/`setSourceNo`；`finance_voucher.source_no` 用 `source_type + source_id` 反查回填。
- **状态**：✅ **已修（2026-09-20）** —— `BusinessAccountingServiceImpl.java:82` 后补 `setSummary(request.getSummary())` + `setSourceNo(request.getSourceNo())`；新增回填迁移 `V11.436.0__Backfill_Finance_Voucher_Summary_SourceNo.sql`（summary 从 remark 复制、source_no 从 `finance_voucher_item.source_no` 按 voucher_id 取 MIN 回填，幂等）。**待办**：启动后验证新产生的凭证两列有值。

#### FIN-BREAK-03 [P2] 财务多表业务列全空

- `finance_payable.invoice_no` 0/3、`finance_receivable.invoice_no` 0/6；`erp_expense_doc.pay_account2_id/2_name/3_id/4_id` **全 0/40**（多账户付款字段前端在传、后端不写）；`erp_pre_receipt` 的 `source_type/source_id/source_no/handler_id/dept_id/bookkeeper_id/payment_method/bank_account/transaction_no` 全 0/2；`erp_capital_flow` 的 `bank_account/bank_name/transaction_no/reconcile_at` 全 0/41。
- **影响**：发票核销、资金对账、多账户付款、预收款来源追溯全部无数据。
- **状态**：⬜

#### FIN-BREAK-04 [P2] 辅助核算项目字典近乎空

- `finance_auxiliary_item` 仅 1 行（E2E 临时项且已软删）、`finance_auxiliary_type` 6 行（1 条 E2E）。`AuxBalanceMapper.java` 的 DEPT 分支又反查 0 行的 `sys_dept` ⇒ 辅助核算余额表按核算项分行取数恒空。
- **状态**：⬜

#### FIN-DUP-01 [P1] 费用报销两套（旧 `erp/expense` 包）

- **证据**：新 `erp/finance/expensedoc`（`erp_expense_item` 53 行）vs 旧 `erp/expense`（`expense_item` **0 行**），31 个 java 文件、列数差异大，**两次独立实现**。
- **关键**：旧包有 **6 个 Controller**，是 HTTP 入口，不受"零 Java 引用"结论覆盖。实测：
  - `/api/erp/expense/statistics/*`（`ExpenseAnalyticsController`）**仍在使用**（`api/analytics-finance.ts:162-172`、`api/analytics.ts:760,832,836` 在调，服务「查费用 80454」页面）→ **必须保留**。
  - 另 5 个 `/application,reimbursement,approval,payment,type/*` 的消费者是 `api/erp/expense/index.ts`，只被**孤儿页**引用。
- **正确顺序**：**先删孤儿页，再删这 5 个 Controller 及 Service/Mapper/Model**（顺序反了留 404 端点）。
- **状态**：⬜ 依赖孤儿页批次

#### FIN-DUP-02 [P1] 应收/应付子分类账 vs 查应收/查应付（原判定有误，已纠正）

- **纠正**：`ReceivableController`/`PayableController` 是**完整子分类账**（新增/列表/账龄/核销/坏账/导出），不是只读报表，**不能删**。
- **推荐**：挂菜单到「财务」，与 `analytics/check-*`（查询分析）并存，命名区分：「应收账款管理」/「应付账款管理」。
- **前置**：核对 `finance/receivable/index.vue` 是否已接线 `PUT /{id}/write-off`、`PUT /{id}/bad-debt`，未接线先接线。
- **状态**：⏸ 待拍板

#### FIN-DUP-03 [P2] 核销中心 `finance/write-off` 只读

- 仅 4 个 GET；真正的核销在 `Receivable/PayableController`。**推荐保留挂菜单并标注"（查询）"**，批量核销列入后续迭代。
- **状态**：⏸ 待拍板

#### FIN-DUP-04 [P2] 资金流水 vs 查资金（80453）

- 两者同用 `capitalFlowApi`。**决策点**：财务是否需要"任意条件导出全部资金流水"的独立入口？要 → 挂台账；不要 → 删台账、导出并入分析页。
- **状态**：⏸ 待拍板

#### FIN-DUP-05 [P1] 预算两套（`budget/annual` vs 已挂 `finance/budget-plan` 80130）

- **推荐**：以 80130 为唯一编制入口。
- **状态**：⏸ 待拍板

---

### 3.5 库存 / 仓储域（erp-stock + wms）

#### STK-BREAK-01 [致命 P0] `erp_stock`（ERP 轨）与 `wms_inventory`（WMS 轨）实测漂移

- **证据**：`wms/.../InventoryServiceImpl.java:185-191` 扣减时镜像失败**只记 error 不回滚**（注释自认"不回滚本轨——避免历史漂移卡死仓库现场"）。实测：

| product_id | 仓库 | erp_stock 可用 | wms_inventory 可用 | 差 |
|---|---|---|---|---|
| 2073239284579586050 | 1 | 95 | 43 | **52** |
| 990000000000000003 | 1 | 152 | 152 | 0 |

- **影响**：`erp_stock` 是**所有库存类报表的唯一数据源**（`StockReportMapper.java:254`、`StockAlertQueryMapper.java:44`、`ProductMapper.java:29,54`、`ShortageReplenishMapper.java:127`、`ProductPriceQueryMapper.java:39`、`MetricsCalculationServiceImpl.java:247`）⇒ 可用库存/缺货预警/智能补货/经营看板全部与仓库现场作业依据不一致。已有 `InventoryReconcileService.reconcile()` 但**只读检测、无自动收敛**。
- **决策（用户 2026-09-20）**：**以 `erp_stock` 为准**（ERP 是系统核心）。
- **实施（已完成）**：
  1. **数据校准**：新增迁移 `V11.437.0__Reconcile_Wms_Inventory_To_Erp_Stock.sql` —— 按
     `(product_id, warehouse_id, batch_no)` 把 `wms_inventory` 的 quantity/available_quantity 对齐到 `erp_stock`；
     **仅处理单行 key**（同一 key 在 WMS 有多行/多库位时跳过，避免把仓库级汇总摊错），幂等。
  2. **停止新增漂移**：`InventoryServiceImpl.decrease()` 的镜像失败从"只记 error 不回滚"改为
     **抛 `WmsBusinessException` 回滚**（与 `increase` 对称）。这是此前两轨单向漂移的主因。
- **本轮实测差异**：**仅 1 行** —— `product_id=2073239284579586050, warehouse_id=1`：
  `erp_stock.available=95` vs `wms_inventory.available=43`（差 52），且该 WMS 行自身矛盾
  （`quantity=31 < available_quantity=43`，即 STK-BREAK-03）。
- **⚠️ 行为变更（需运行验证）**：收紧后，若 ERP 轨可用量不足，仓库出库作业会**直接失败**（此前会静默放过）。
  这是"以 erp_stock 为准"的必然结果；若现场出现卡单，说明 ERP 轨账本身有问题，应走对账而不是放宽。
- **状态**：🔄 代码 + 迁移已就绪，**待启动执行与验证**

#### STK-BREAK-02 [P0] 6 处代码绕过「库存唯一写入口」，方向不对称

- **设计约定**：`InventoryChangeEvent`/`InventoryService` 注释明确"ERP 侧严禁直写 `erp_stock`"。
- **实测违规点**（只写 `erp_stock` 不动 `wms_inventory`，全是**反向/取消**路径）：
  - `erp-purchase/.../PurchaseInboundServiceImpl.java:653`（取消入库回冲 —— 同文件 633 行注释刚说"不再直写"）；`reverseStock()` 还会因漂移抛异常**卡死"取消入库"**
  - `erp-purchase/.../PurchaseReturnServiceImpl.java:411`
  - `erp-sales/.../RetailOrderServiceImpl.java:396,402`
  - `erp-sales/.../SaleExchangeServiceImpl.java:364,366,389,391`
  - `erp-sales/.../SaleOrderServiceImpl.java:557`（发货扣减）
  - `erp-stock/.../StockController.java:89,102`（手工增减库存 API）
- **影响**：**正向走 WMS 双写、反向只写 ERP 单轨** ⇒ 每次取消/退回产生一次单向漂移，这正是 STK-BREAK-01 的成因。
- **修法**：全部改为发 `InventoryChangeEvent`。属缺陷修复，**不需拍板**。
- **状态**：🔄 **已完成 5 处（2026-09-20）**：
  - `PurchaseInboundServiceImpl#reverseStock`（取消入库回冲，改发 DECREASE）
  - `PurchaseReturnServiceImpl#updateStock`（采购退货出库，DECREASE）
  - `RetailOrderServiceImpl`（零售结算：退货 INCREASE / 正常 DECREASE）
  - `SaleExchangeServiceImpl`（换货过账 + 取消回滚，两处对称）
  **剩余 2 处需设计定稿（本轮未改）**：
  - `SaleOrderServiceImpl#confirmShipment` —— 它先 `unfreezeStock` 再 `decreaseStock`，而 **WMS 侧没有冻结/解冻概念**（`InventoryChangeEvent` 只有 INCREASE/DECREASE）。审批时冻结的是 `erp_stock.frozen_quantity`（`SaleOrderServiceImpl.java:2347`），WMS 的 `available_quantity` 从未被冻结 ⇒ 直接改事件会让 WMS 判"可用不足"而失败。**这是双轨制下"可用量"定义不一致的根问题，需先定冻结语义**。另该方法 `:558-560` 把库存异常**吞掉只记 error** 后仍标记发货，属独立缺陷。
  - `StockController` 的 `/increase`、`/decrease`、`/freeze`、`/unfreeze` 手工库存调整 API —— 管理员手工调账是否应作用于 WMS 轨，需业务定义。

#### STK-BREAK-03 [P1] `wms_inventory` 自身"可用量 > 总量"

- **证据**：`2079717784150593538` qty=31/avail=43；`9909046245377695746` qty=0/avail=152（2 行命中）。
- **影响**：超卖校验会放行不存在的库存。
- **修法**：加约束 + `increase/decrease/freeze/unfreeze` 补断言；先归零历史脏数据。
- **状态**：✅ **已完成并验证（2026-09-21）** ——
  1. 脏数据已由 `V11.437.0` 校准（那 2 行矛盾随「以 erp_stock 为准」对齐后消失）；
  2. 防再生落到**数据库约束**：迁移 `V11.440.0__Inventory_Quantity_Invariants.sql`
     给 `wms_inventory` 与 `erp_stock` 各加 4 条 CHECK：`available <= quantity`、`quantity >= 0`、
     `available >= 0`、`frozen >= 0`；用 `DO` + `pg_constraint` 判存在 ⇒ **幂等**（实测重跑通过）。
     干跑前实测两表 0 违规（wms 3 行 / erp_stock 4 行），加约束不会失败。
  3. **为何是约束而非断言**：`increase`/`decrease`/`freeze`/`unfreeze` 四条路径**本身都守恒**
     （同增同减，且 decrease/freeze 前置校验 `available >= quantity`）——历史违规全部来自
     **绕过 Service 的直写**（正是 STK-BREAK-02 那批反向路径）。约束是唯一能覆盖
     「Service + 手工 SQL + 运维脚本」全部写入方的收口点，故不再加冗余断言。

#### STK-BREAK-04 [P2] `erp_stock` 展示快照列 100% NULL

- **证据**：`erp_stock` 4 行，`product_name/product_code/warehouse_name/serial_no/batch_no/supplier_id/unit_price` **全 100% NULL**；唯一写入方 `StockServiceImpl.java:41-93` 只 set `productId/warehouseId/quantity*`（仅 `recordStockIn` 那一路会 set 快照，但**不被主链路调用**）。
- **影响**：`ProductPriceQueryMapper`、`StockReportMapper` 都 SELECT 了这些列 → 列表页该列为空。
- **状态**：✅ **已闭环（2026-09-21）**
  - **⚠️ 复核更正（原「影响」一句不准确）**：逐个查证后，绝大多数读路径**根本不依赖这些快照列** —— `ProductPriceQueryMapper` 取的是 JOIN 出来的 `p.product_code/p.product_name`；`StockReportMapper` 取的是单据明细/表头列；`StockAlertReplenishMapper:72,74,75`、`ShortageReplenishMapper:101,123` 都写了 `COALESCE(快照, 档案)`。全仓**只有 2 处**是"直接取快照、无回退"：`InitialStockQueryMapper:60`（仓库名）与 `:62`（期初单价），且它们只作用于 `is_initial = 1` 的期初页。所以这条**不是"列表页整列空白"**，而是"写入口口径不一致 + 唯一无回退的那列（期初仓库名）有风险"。
  - **写入侧根治**：`StockServiceImpl` 新增 `fillSnapshot()`（**只补空列、不覆盖调用方已给的值**，已完整的行零成本），挂到 **4 条会建行/改行的路径**：`increaseStock`（增改两支）、`checkStock`（盘点，凭空建行的高发路径）、`recordStockIn`（增改两支）、`saveInitialStock`。根因是「调用方自觉」不可靠：`increaseStock`/`checkStock` 只收 id，`wms.InventoryServiceImpl` 的 ERP 轨镜像与盘点调整**根本无从传名称**。
  - **存量回填**：迁移 `V11.449.0__Backfill_Erp_Stock_Snapshots.sql`（幂等，按 `tenant_id IS NOT DISTINCT FROM` 关联档案防跨租户取到别家商品名）。回填后 4 行的 `product_name/product_code/warehouse_name` 全部有值（前两行 `unit` 为空是因为商品档案本身 `unit` 就是 NULL，属正确行为）。
  - **读侧收口**：`InitialStockQueryMapper:60` 就地标注「本列直接取快照、依赖写入侧保证」，避免后人误以为有 COALESCE。
  - **验证**：新增 `tools/verify-stock-snapshot.cjs` —— **13/13 全绿**，三向断言：① 存量回填后无空行；② 走真实 `POST /erp/stock/increase`（只传 id）建出的**新行**快照有值（证明改在写入口而不是只靠迁移糊）；③ 人为清空快照后再写一次能被**自愈**补回、且数量未被补快照的动作改坏。脚本自清理测试行。
  - **⚠️ 仍未闭环（登记）**：`supplier_id` / `supplier_name` 这两列**任何写入路径都不写** —— 它们无法从商品/仓库档案推导，只能由单据传入，而当前**没有任何调用方传**（`StockOverflowServiceImpl` 传了商品/仓库/单位/金额，也没传供应商）。影响面：`ShortageReplenishMapper:123`、`SmartReplenishMapper:172`、`StockAlertReplenishMapper:120` 三处"补货建议的供应商列"依赖它（都有 COALESCE/回退到 lead price，故表现为兜底值或空，而非整列失效）。归入**独立待办**：需要决定「入库时从采购单带供应商」的链路改造。

#### STK-BREAK-05 [P2] 库存单据链大面积空表，报表 JOIN 空表

- **0 行表**：`erp_stock_out`/`_item`、`erp_stock_check`/`_item`、`erp_stock_take`/`_item`、`erp_stock_split`/`_item`、`erp_stock_replenishment`、`erp_stock_bom`/`_item`、`erp_stock_alert_config`；`erp_stock_in`/`_item` 各仅 1 行。
- **`StockReportMapper.java:114`、`InventoryAnalysisReportServiceImpl.java:143,160` JOIN 这些表** ⇒ 库存报表"出入库汇总/盘点"分区恒为空。
- **状态**：⬜

#### STK-DUP-01 [P1] 盘点两套（`wh/inventory-order` + `checkApi`/`/wms/check/*` vs 菜单 5003 盘点单）

- `wh/inventory-order` 能力是 `wms/check` 的**超集**（另有 `submitResult`/`cancelCheck`/`saveDetails`）。
- **建议落法**（对标 SAP EWM 三阶段）：5003 为单据层（审批+库存变动），`wh/inventory-order` 作业明细作为其执行阶段视图，复用 `checkApi` 数据、不迁数据；对齐后删重复入口。
- **绑定关系**：`wms/check/index.vue` 已删，`form.vue` 保留（被 `wh/inventory-order` 跳转）；删 `wh/inventory-order` 前必须先搬能力。
- **验收**：同一盘点单走完「准备 → 执行 → 审批」，`wms/check/form.vue` 仍可达，两入口不再并存。
- **状态**：⏸ 待拍板 / ⬜ 前端开发

#### STK-DUP-02 [P1] `wms_warehouse`(3 行) vs `erp_warehouse`(**105 行**) —— **两套都活着**

- **⚠️ 本轮核验修正（子代理初判"确认合并、可删"是错的）**：`wms_warehouse` **不是**无引用的死表 —— 它有完整消费者：`wms/controller/WarehouseController.java:26`、`wms/controller/LocationController.java:42`、`wms/receipt/service/impl/ReceiptServiceImpl.java:50` 三处注入 `cn.aiedge.wms.warehouse.service.WarehouseService`（该 Service 的 Mapper 就是 `WmsWarehouseMapper`）。另 `WMS-ARCHITECTURE.md:178` 明确其定位是「在 `erp_warehouse` 基础上扩展库区数量、类型、容量」。
- **数据实证（实测）**：`erp_warehouse` **105 行**、`wms_warehouse` 3 行，后者 id=1,2,3 与前者同 id 行字段一致。
- **真实性质**：这是 `FUNCTION_DUPLICATE_AUDIT` §1.1 的「`warehouse/*` 两套接口」（erp `WarehouseController` vs wms `WarehouseController`）—— 属**新旧/两套实现并存**（专项 C），不是死表。
- **修法（二选一，需裁定）**：① 承认扩展表设计，补齐 WMS 侧写入路径使其与 `erp_warehouse` 同步；② 收敛为一套（保留 `erp_warehouse`，把库区/类型/容量并入或另设 1:1 扩展表），删另一套的 Controller/Service。
- **状态**：⏸ 需裁定

> **⚠️ 口径教训（本轮第 4 次踩）**：判断"某表/某类无引用"**不能只搜 SQL 字符串或 `@TableName`** ——
> 必须搜 **Service/接口名**（`WarehouseService` 这类跨包引用不会出现在表名搜索里）。
> 子代理报告中的"无引用/可删"结论**一律需复核后才能落到删除动作**。

#### STK-CLEAN-01 [P2] 仓储域孤儿表

- `erp_kit_*`(4)、`batch_rule`/`batch_snapshot_cache`/`inventory_query_cache`/`serial_status_cache`(4)、`wms_event_outbox`(0 行，有实体无消费方)、`traceability_log` 等 → 走「备份 + 观察期 → 改名 `zz_deprecated_*` → 再观察 → drop」流程。
- **状态**：⬜

#### STK-AUTHZ-01 [P1] `wms.*` / PDA 零权限码

- 见 E-04；需先定 PDA 设备鉴权策略（`PdaAuthController` 本身还有 平台-BREAK-01 的漏写租户问题）。
- **状态**：⬜

---

### 3.6 销售域（erp-sales）

#### SAL-BREAK-01 [P1] 销售退货三段链路全断

- **证据**：`erp_sale_return` 25 行，`sale_order_id`/`sale_order_no` **0/25 非空**；`erp_sale_return_doc` 1 行，`return_apply_id`/`return_apply_no` **0/1 非空**；`salereturn` 包内**无任何 `saleReturnDocService` 引用**（grep 无输出）⇒ **申请审批通过后没有生成退货单的代码路径**。
- **影响**：退货申请永不关联原销售单（无法带出原单价格/数量校验），申请→退货单需人工重建；`wms_inventory_log` 里已有 25+ 条 `SALE_RETURN_APPLY`/`SALE_RETURN_DOC` 流水 ⇒ **两条链各自独立动库存，存在重复退货风险**。
- **修法**：保存申请时写 `sale_order_id/sale_order_no`；审批通过触发 `SaleReturnDocService` 生成草稿并回写 `return_apply_id`。
- **需拍板**：是否一申请一单？
- **状态**：⏸ 部分需拍板

#### SAL-BREAK-02 [P1] 销售订单明细只落 4 行，27 张单里 25 张无明细

- **证据**：`erp_sale_order` 27 行 vs `erp_sale_order_item` **仅 4 行**（分属 2 张单）；而 `erp_sale_order_promo_detail` 有 72 行、`min/max(order_id)` 精确落在那 24 张新建单上。下单路径 `SaleOrderServiceImpl.java:255,275,277` 确实 `itemMapper.insert` ⇒ 这 24 单是**在明细为空的情况下建出来的**（`total_amount` 也全 0）。
- **影响**：订单中心"明细 Tab"对 25/27 张单为空，无法出库/发货；促销明细有行而商品明细无行 = 典型"半截写入"。
- **修法**：`createOrder` 加最小校验（`items` 非空 + `billAmount` 与明细合计一致）。
- **需确认**：24 单无明细是 E2E 造数还是前端某入口漏传。
- **状态**：✅ **已闭环（2026-09-21），但结论与原判断不同 —— 原统计口径是错的**
  - **⚠️ 复核更正**：那 24 张 `XSDD-20260918-0001..0024` 的 `erp_sale_order.deleted` **全是 1（已逻辑删除）**。原统计「27 行里 25 张无明细」**没有过滤 `deleted`**，把 24 张**已删单**也算进去了；**活着的只有 3 张**（`QO202607232493` 2 行明细、`ZZT-QO-001` 2 行、`ZZT-QO-CANCEL` 0 行且 status=6 已取消）。这 24 张是 `tools/e2e-marketing.cjs` 跑出来的（金额 210/250/3 与 40/100/1 两两成对、共 12 对 = 12 次运行），脚本 `finally` 里删单 → 单据逻辑删除、明细物理删除，故留下"有单无明细"的残影。**"半截写入"不成立**（明细之所以不在，是被 `deleteOrder` 按设计删掉的）。
  - **据此不改 createOrder 的校验**：原「修法」是在错误前提下写的。硬加「明细非空才允许建单」会**打断合法的"先存空草稿"**（前端表单允许存草稿后再录明细），而没有任何证据表明有真实调用方在建空单。
  - **真正查出并修掉的缺陷（顺带）**：`SaleOrderServiceImpl.updateOrder` 原为「**无条件**删光旧明细 + **有条件**（`items != null`）重建」——删与建条件不一致 ⇒ 任何一次**不带明细的更新**（只改表头字段的保存、局部字段补写、脚本的部分字段 PUT）都会把该单**全部明细删掉且不补回**，而主表照常更新。已把删除并入同一条件，并把语义钉死：`items == null` → 不动明细；`items == []` → 显式清空；非空 → 先删后插。
  - **验证**：新增 `tools/verify-sale-pur-breaks.cjs`（与 PUR-BREAK-03 合并成一个脚本，**17/17 全绿**）：建单带 2 行 → 只改表头（不带 items）后**明细仍是 2 行**且 `summary` 确实被改（证明这次 PUT 真生效）→ 带 1 行更新后替换为 1 行 → 显式传 `[]` 清空。脚本自删测试单。

#### SAL-BREAK-03 [P2] 销售订单主表关键列 100% NULL + 第三条写入路径漏字段

- **证据**：`warehouse_id` 0/27、`salesman_id` 0/27、`dept_id` 0/27、`customer_name` 仅 5/27。
- **三条写入路径**：`SaleOrderServiceImpl.java:194-215`（正常录单，全字段 ✔）/ `SalePreOrderServiceImpl.java:515`（预订单转正式单）/ **`SaleOrderServiceImpl.java:1235 findOrCreateOrder()`（批量导入，只写 orderNo/date/type/status/generationMethod，完全不写 customer/warehouse/salesman/dept ✘）**。
- **影响**：`sales/order-center/index.vue:367,411,421,622,703,791` 用 `salesmanName` 做查询与列展示，恒空；`SaleOrderServiceImpl.java:1623` 的 `warehouse_id` 筛选恒无结果；导入订单无客户 ⇒ 出库/对账无法自动带往来单位。且服务端**无强校验**（27/27 为空）。
- **状态**：✅ **已闭环（2026-09-21）**
  - **⚠️ 口径更正**：`0/27` 这个分母包含了 24 张**已逻辑删除**的 E2E 单（见上条）。活着的 3 张单同为空 —— 但它们来自 seed（`ZZT-QO-*`）与早期造数，**没有任何来自 UI 录单的样本**，所以「正常录单路径漏字段」这个推论其实没有证据；`createOrder` 对这几个字段是**无条件 set(dto 值)**，UI 传什么就写什么。
  - **确认并修掉的唯一代码缺陷是批量导入那条路径**：`findOrCreateOrder` 此前只写单号/日期/类型/状态。现改为从该单**第一行**解析「客户 / 仓库 / 业务员 / 部门」并写入：客户按 `供应商/客户编码` 或 `名称` 查 `biz_party`（编码优先）、仓库按名称查 `erp_warehouse`、业务员按 `username` 或 `real_name` 查 `sys_user`、部门按名称查 `sys_dept`；新注入了 3 个 Mapper（`WarehouseMapper`/`SysUserMapper`/`SysDeptMapper`）。四个字段都是**可选列**：Excel 没这些表头、或名称解析不到，就留空**不报错**（不该因为一个仓库名拼错让整单导入失败）。表头别名支持英文驼峰与中文两套。
  - **仍未闭环（登记）**：`sys_dept` 本身 0 行（PUR-BREAK-02，已拍板冻结）⇒ 部门这一项即使解析也拿不到 id；`erp_sale_order.warehouse_id` 在现有数据里全空，属**历史数据**而非写路径问题，是否需要回填要业务定。

#### SAL-BREAK-04 [P2] 销售出库单 `order_id` 22/25 悬空

- **证据**：`erp_sale_outbound.order_id` 25 条非空，`LEFT JOIN erp_sale_order` 仅 **3 条命中**（按单号文本同样 3 条）；悬空值形如 `1789286438970`（疑 E2E 造数）。表间**无外键约束**。
- **影响**：出库单"源单"跳转/JOIN 取订单信息为空。
- **状态**：⬜

#### SAL-DUP-01 [P2] `erp_sale_return` / `erp_sale_return_doc` —— **不能合并**

- 退货**申请**与退货**单**是两个业务阶段（非重复）；但两者**当前无任何关联键**（见 SAL-BREAK-01）。**要修的是"补关联"，不是"合并表"。**
- 另注：两表 133/132 列高度重合，字段复制到维护危险的程度 → 列入「字段收敛」专项（见 ARCH-02）。
- **状态**：⬜

#### SAL-CLEAN-01 [P2] 销售域孤儿页

- `erp/sale/index.vue`、`erp/purchase/index.vue` 是**非孤儿**（`list_path` + `componentMap` 兜底解析，禁止删除，见 `CLEANUP_SCOPE` §2.1）。
- `trade/mall-return`、`erp/return`、`erp/sales-analysis`、`erp/shipment/index.vue`、`order-center/index.vue` 在删除/补能力清单内 → 见 SAL-CAP-01。
- **状态**：⬜

#### SAL-CAP-01 [P1] 15 项页面的写能力补齐（清单见 `CLEANUP_DECISIONS` §7.2）

- 与销售域相关：`erp/shipment/index.vue`（审核/出库 → `sales/outbound`）、`order-center/index.vue`（**采购侧 submit/approve/cancel/delete 整体缺失，工作量最大**）。
- **验收**：每补完一项，从旧页面删除对应入口；全部补完后旧页面方可删除。
- **状态**：⬜

---

### 3.7 采购域（erp-purchase）

#### PUR-BREAK-01 [P0] 采购合同：菜单在、页面在、接口 404、表还不存在（三重断）

- **证据链**：菜单 `sys_menu.id=81010`（采购合同）→ `views/erp/purchase-contract/index.vue:506` 调 `purchaseContractApi.page()` → `GET /erp/purchase/contract/page`；而 `PurchaseContractController.java:16` 的映射**没有 `/page`**（只有 `GET /{id}`、`/by-no/{}`、`/supplier/{}`、`/statistics` + 5 个 POST）；`PurchaseContractMapper.java:16-34` 全部 `FROM purchase_contract`，而该表**在 devdb 中不存在**（548 张表里没有）⇒ 即便有 `/page` 也是 500。
- **影响**：「采购合同」菜单点进去即 404/500，**整个功能不可用**。
- **修法**：二选一 ——（推荐）删菜单 81010 + 页面 + Controller/Service/Mapper（合同能力由 CRM `crm_contract` 承担）；或补建表 + 补 `/page`。
- **✅ 已拍板（2026-09-21，用户）**：**补建 `purchase_contract` 表 + 补 `GET /erp/purchase/contract/page` 接口**，
  保留 ERP 侧合同能力（不删菜单 81010、不并入 CRM）。
- **状态**：✅ **已完成并验证（2026-09-21）** —— 改动清单：
  1. 迁移 `V11.439.0__Create_Purchase_Contract.sql`：建 `purchase_contract`（含 `tenant_id` +
     `UNIQUE(tenant_id, contract_no)`）、`purchase_contract_item`、`purchase_contract_modification`；
     补 7 个权限码 `purchase:contract:{list,detail,create,update,delete,approve,export}`（id 90068-90074）
     并**显式关联超管角色**（铁律：先补码后补注解）。命名对齐同域既有约定 `purchase:<资源>:<动作>`
     （用 `detail`/`update`，**不是** `view`/`edit`，见 `purchase:order:*`）。
     干跑校验：事务内 3 表 + 7 码 + 7 关联，已回滚。
  2. `PurchaseContractController` 补 `/page`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`/export` 五个端点
     （导出为 CSV，带 BOM）；**全部端点补 `@SaCheckPermission`**；返回类型由裸实体
     `ResponseEntity<PurchaseContract>` 改为 `ApiResponse<T>`（前端类型本就是 `ApiResponse`，
     原返回形态下前端取不到 `data`）。
  3. `PurchaseContractService(+Impl)` 补 `pageContracts` / `createContract` / `updateContract` /
     `deleteContract`；新增 `PurchaseContractQueryDTO`。
  4. **两处连带缺陷一并修掉**：
     - `PurchaseContractMapper.findExpiringContracts` 原为 `end_date <= #{date}` 而参数是 `int days`
       → PG 抛 `operator does not exist: timestamp <= integer`，该查询从未成功执行过；改为「距今 days 天内到期」。
     - `ContractStatisticsDTO` 缺 `totalCount`/`draftCount`/`totalAmount`，列表页统计卡片恒为 0。
     - `PurchaseContractMapper.update` 只更新 7 列，用户改「供应商/质保期/合同附件」保存后**静默丢改动**；已补齐。
  5. **租户口径**：本 Mapper 是自定义 `@Insert` 注解 SQL，**不经过 `insertFill`** ⇒ Service 显式
     `setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue())`，INSERT 列表显式带 `tenant_id`。
     漏写不会报错，但会落成 `tenant_id=0` 的「谁都不看不见」数据（同 平台-BREAK-03 的成因）。
  6. 单元测试同步：`PurchaseContractControllerTest` 断言路径下沉到 `$.data.*`，并补 `/page` 用例。
     `./mvnw -o -pl erp/erp-purchase test-compile` **通过**。
  **验证**：新增 `tools/verify-purchase-contract.cjs` —— **17/17 全绿**（建→查→列→改→删→导出→鉴权，
  脚本自建自删不留残留）。其中「建完必须能回查/列表命中」是租户口径的关键断言：
  若 `tenant_id` 落成 0，多租户插件会注入 `tenant_id = 1` 使其不可见 —— 只断言「创建返回 200」会漏掉该 P0。

#### PUR-BREAK-02 [P1] `sys_dept`（0 行）被采购/财务活代码 JOIN —— 双重断点

- **证据**：`PurchaseOrderMapper.xml:76,198` `LEFT JOIN sys_dept d ON d.id = o.dept_id` → `d.dept_name` 恒空；**且** `erp_purchase_order.dept_id` 本身 **0/11 非空** ⇒ **即使 `sys_dept` 有数据仍为空**（双重断点）。
- **同链路受害者**：`AuxBalanceMapper.java:71`、`InboundNameLookupMapper.java:32`、`PurchaseAnalysisReportServiceImpl.java:201`、`WorkflowServiceImpl`（按部门找审批人）、`ExpenseApprovalServiceImpl.java:289,353`。
- **真实部门**在 `sys_department`（5 行：D9001/D9002 + 3 条 E2E）。
- **影响**：采购单据"部门"列、辅助核算 DEPT 编码、按部门筛选、按部门找审批人**全部失效**。
- **修法**：按 `TABLE_DUPLICATE_AUDIT` A-01 既定顺序 —— **暂不动代码**（触及 `DataScopeAspect` 数据权限 + 部门功能未启用，改完无法端到端验证）；先在 `SysDept` 标 `@Deprecated`，待部门启用后：迁 4 个 Java 消费方 → 改 5 处 SQL → 删 `SysDept`/`SysDeptMapper` → 观察期后 DROP。
- **✅ 已拍板（2026-09-21，用户）**：**启用部门维度，但先冻结不改代码** —— `SysDept` 标 `@Deprecated` 并登记
  A-01 迁移计划；待 `sys_user.dept_id` 有真实数据后再迁 4 处 Java + 5 处 SQL。理由：现在改完无法端到端验证。
- **状态**：🔄 仅标注冻结，不迁移

#### PUR-BREAK-03 [P2] 采购分析的供应商名/编码恒空

- **证据**：`PurchaseAnalysisReportServiceImpl.java:297,317` `LEFT JOIN erp_supplier s ON s.id = pr.supplier_id`，且 `:315` 用 `MAX(po.supplier_name) AS dimLabel`；实测 `erp_supplier` **0 行**、`supplier` **0 行**、`erp_purchase_order.supplier_name` **0/11 非空**（真实供应商在 `biz_party` 152 行）。
- 列表页靠 `erp_purchase_order_partner_snapshot`(10 行)兜住，**分析报表没有这层兜底** ⇒ "按供应商"维度标签与编码列为空，报表不可读。
- **状态**：✅ **已闭环（2026-09-21）**
  - **根因（比原判断更准确）**：`erp_supplier` 是个**没人写、没人维护的重复供应商档案表**（建表语句在 `DatabaseInitializer`，全仓消费方只有 `PurchaseAnalysisReportServiceImpl` 与 `SupplierSnapshotMapper` 两处）；系统真正在用的供应商主数据是 **`biz_party`**（按 `party_type` 区分角色），供应商管理页 `views/md/supplier` 走的也是 biz_party（`partnerApi`）。所以这不是"JOIN 写错了一个词"，是**两套供应商主数据**（专项 C）。第二处断点是 `MAX(po.supplier_name)` —— `PurchaseOrder` 实体**根本没有 supplierName 字段**，故该列**在代码层面就不可能被写入**。
  - **改法**：① `PurchaseAnalysisReportServiceImpl` 的两处 `erp_supplier` → `biz_party`（`supplierIdentity` 同步改用 `party_code`）；② `supplierCodeBlock` 的标签/编码改为**三级取值**「订单表冗余列 → 供应商快照 → biz_party 档案」，与采购列表页（`UnifiedPurchaseDocQueryServiceImpl` 用快照兜底）同口径；③ `SupplierSnapshotMapper`（为采购单据列表补「供应商编号/联系人/电话/地址」五列，读的也是 0 行的 `erp_supplier`）一并改指 `biz_party`：编号←`party_code`、联系人←`default_handler_name`（空则退 `legal_person`）、电话←`phone`（空则退 `legal_person_phone`）、地址←`address`、备注←`remark`。
  - **验证**：`tools/verify-sale-pur-breaks.cjs`（与 SAL-BREAK-02 合并）**17/17 全绿**，其中前提断言写明「`erp_supplier` 实测 0 行」「该供应商名称/编码**只存在于快照表**」，随后断言响应里出现 `E2EGYS001` 与 `E2E采购供应商`，且 `dimKey=2099000000000000901` 那行同时带 `dimLabel` 与 `supplierCode`（修复前两者为 null）；对照组「按商品」维度不带出供应商字段。
  - **仍未闭环（登记）**：① 分析页的**「供应商名称」筛选**仍 `w.like("po.supplier_name", ...)`（订单表恒空列）⇒ 按供应商名搜索恒无结果，要修得给三个聚合块都加 biz_party/快照 JOIN；② `erp_supplier` 这张 0 行表是否删除（还有 `PurchaseExchange` 实体的注释、`DatabaseInitializer` 建表语句引用它）—— 归专项 C「重复主数据」。

#### PUR-BREAK-04 [P2] 采购入库/退货的源单与主体断链

- `erp_purchase_inbound` 39 行中 `order_id` 仅 31 非空（**8 张 20260722 的单 `order_id`/`order_no` 全空却已 status=8**）；`erp_purchase_return` 仅 1 行且 `purchase_order_id`/`supplier_id` **全空** ⇒ `PurchaseOrderMapper.xml:88` 的退货聚合子查询永远匹配不上。
- 对比：入库单 `supplier_name` 36/39 有值，**订单表 0/11** —— 同一字段两表一写一不写。
- **状态**：🔄 **部分闭环（2026-09-21）—— 数据面判定为历史造数，代码面补掉一处**
  - **⚠️ 数据面逐条查证后不成立**：那 8 张 `PI202607220001..0008` 全是 2026-07-22 一批、其中 6 张 `supplier_id=100 / supplier_name='测试供应商'`，**是早期测试数据**而非活路径产物（`客户/仓库/主体` 全是测试值）；唯一那行 `PR202608280001` 同样 `supplier_name='测试供应商'`、`purchase_order_id` 为空 —— 它是走「直接新增退货单」建的，**本来就没有源单**，所以 `GROUP BY r.purchase_order_id` 匹配不上是**正确行为**，不是缺陷。此项若要"修数据"，属于**要不要清理历史测试单**的业务决定，不做静默改写。
  - **代码面补掉的真实缺口**：`POST /api/erp/purchase/return/from-order/{orderId}`（前端 `api/erp.ts:239` 已封装）对应的 `PurchaseReturnServiceImpl.createFromOrder` 此前**只写 `purchase_order_id`** —— 单号、供应商全不写，主体是空的。现补 `fillSourceOrder()`：写 `purchase_order_no` + `supplier_id` + `supplier_name`；名称**只从供应商快照取**（订单实体没有 supplierName 字段，取不到）。订单不存在时抛 404 而不是建出无源单。
  - **仍未闭环（登记）**：入库单没有类似 `fillSourceOrder` 的收口就建出 `order_id` 为空的单 —— 需要确认入库单是否允许"无源单直接入库"（业务上通常允许），若允许则应显式标注来源类型，而不是留空 order_id 让人误以为是断链。

#### PUR-DUP-01 [P1] 采购订单/入库/退货对称复制

- 与销售侧一一对应（`FUNCTION_DUPLICATE_AUDIT` §1.2：order 11 组 / exchange 14 组 / return 10 组），**本轮不动**（抽公共基类属架构改造）。
- **但**：任一侧改审批流/单号规则时**必须两侧同步改** → 写入 `ARCH-03 对称模块同步清单`。
- **状态**：🚫 本轮不做（记录规则）

---

### 3.8 主数据域（erp-partner、crm、md/*）

#### MD-DUP-01 [P1] 客户两套（接口 + 菜单）

- **接口**：`CustomerController`（crm） vs `MdCustomerController`（erp）—— `customer/page|list|import|export|batch|{}` 等 7 个同名端点。
- **菜单**：`80200 crm` vs `80510 md` **重名**（`CLEANUP_SCOPE` D-04）。
- **相关**：`crm_erp_customer_mapping` 表在孤儿清单里（0 引用）。
- **修法**：先定哪套是客户主数据的权威入口（记忆 `erp-vs-crm-customer`：CRM=公海客户，ERP=往来单位，**语义不同**）→ 据此改菜单名消歧、收敛接口。
- **状态**：⏸ 需拍板

#### MD-DUP-02 [P1] 供应商两套空壳表

- `supplier`(0 行/13 列) 与 `erp_supplier`(0 行/44 列) 同属 `erp-supplier-portal` 模块；真实供应商数据在 **`biz_party`(152 行)**。
- **修法**：确认数据落点后**两套表一起废弃**（不是合并入 `biz_party` —— 它们本就是零行空壳）。
- **状态**：⬜

#### MD-DUP-03 [P1] 客户等级价两套（两个同名类）

- `biz_customer_grade_price`(0 行，`erp/party/entity/CustomerGradePrice.java`) vs `erp_customer_grade_price`(**18 行**，`erp/stock/entity/CustomerGradePrice.java`，属「商品价格管理-子标签3」）。
- **修法**：删除 `erp-party` 那套（含实体）。与 `priceLevelConfig.ts` 契约零引用（D-10）同属价格域。
- **状态**：⬜

#### MD-DUP-04 [P2] 往来单位三个页面同源（`md/partner` / `md/supplier` / `md/customer`）

- `partner`(1136 行) 与 `supplier`(1165 行) 的 API 指纹**完全一致**（Jaccard 1.00），靠 `party_type` 区分；差异 779 行（67%）⇒ 同骨架 + 大量各自定制。
- **修法**：是否收敛为「一个页面的三个筛选视图」，**需业务裁定**。
- **状态**：⏸ 待拍板

#### MD-AUTHZ-01 [P0] `crm/*` 8 个控制器整模块零鉴权（含客户主数据）

- `CustomerController`（`crm/.../customer/controller/`）整个类**零鉴权注解**（pageList/create/update/delete/batchDelete/export/import）。
- 匿名白名单已在 09-19 移除 ⇒ 现为「登录后越权」（低权限用户直调高权限接口）。
- **修法**：E-01 批次 2（先补权限码种子）。
- **状态**：🔄 **部分完成**（2026-09-21）：E-02 批次 2 已给 `CustomerController` 的 `list/update/delete` 接上 `crm:customer:{list,update,delete}`（另接线 `crm:lead:view`、`crm:opportunity:{create,view}`，两向验证 18/18）；`create/batchDelete/export/import` 等**仍未接线**，待后续批次（其码已在库：`crm:customer:*`）。

#### CRM-CAP-01 [P2] CRM「售后阶段」（工单 / 售后）功能缺失 —— 用户口径：后期迭代补

- **背景（2026-09-21 用户口径）**：CRM 是**一条连续的客户维护链**——前期是销售（收集线索、维护客户、客情服务），**成交后是盯工单与售后**。所以「CRM 内部分两个阶段，但同属**客户服务管理**」⇒ 模块名定为「**客户服务**」，模块内部按阶段拆两个码子域：`crm:*`（售前）+ **`crm:service:*`（售后）**。业界同构：Salesforce = Sales Cloud + Service Cloud；Zoho = CRM + Desk；Odoo = CRM + Helpdesk。
- **实测缺口（三处都查过，非推测）**：`backend/crm` 现有 **10 个控制器全是售前**（Lead/Opportunity/Quotation+Template/Contract/Customer/CustomerFollowUp/CustomerPool/Visit/MarketingCampaign）；库里 **0 张** ticket / service / aftersale / complaint 表；菜单里**没有**工单/售后入口（仅系统监控的"服务状态"）。唯一沾边的是 `DatabaseInitializer` 里的 `customer_ticket` **字段**（VARCHAR，疑似"客户工单号"登记位），无实体、无接口。
- **影响**：客户服务链路只覆盖到成交，**成交之后的工单/售后无系统承接**；`crm:service:*` 码域为空 ⇒ 即便将来接线也无码可接。
- **修法（需新建，不是接线）**：工单实体 + 表 + 接口 + 菜单 + 权限码（`crm:service:*`）+ 与合同/客户/发货的关联。**量级不小，属独立功能项**，不要混进 E-02 接线批次（后者只是"把已有码接到已有接口上"）。
- **用户决议（2026-09-21）**：**后期迭代再补**，当期只计入本待办清单，不排期、不动手。
- **状态**：⬜ 待后期迭代（已登记，不阻塞当前接线与模块授权工作）

---

### 3.9 DMS 配送域

#### DMS-BREAK-01 [P1] `dms_task.order_id` 全空，DMS 与业务单据只有文本弱关联

- **证据**：`dms_task` 96 行，`order_id` **0/96** 非空；`order_no` 74 非空、`source_bill_no` 29 非空；按 `source_bill_no = erp_sale_outbound.outbound_no` 只有 **5/29 命中**。
- **影响**：DMS 任务无法与销售订单/出库单做 ID 级关联 ⇒ 订单跟踪、回单核销、配送结算只能靠字符串匹配，**单号一改即断链**。这也是 `dms_settlement` 全空的根因（结算需要单据 ID）。
- **修法**：派单时写入 `order_id`/`source_bill_id`；或补映射表。
- **状态**：⬜

#### DMS-BREAK-02 [P2] DMS 结算三表全空、支付表关键列全空

- `dms_settlement`/`_item`/`_rule` 全 0 行，而 `DmsSettlementItemMapper.java:25` 却 `JOIN dms_settlement` ⇒ 接口取数恒空；`SettlementService.java:614` 还向财务推送记账（`finance_voucher` 里一条自动凭证都没有）。
- `dms_payment` 仅 1 行且 `rider_id/audit_by/audit_time/pay_channel/trade_no/callback_time/finance_trace_id/qrcode_url` **全部 0**。
- **修法**：确认结算是否本期范围；不在范围则**删菜单 + 表**，避免"看起来能用"。
- **状态**：⏸ 需拍板

#### DMS-BREAK-03 [P2] 车辆主数据大面积空列

- `dms_vehicle` 45 列中 **26 列 100% NULL**（`vin/engine_no/rated_load/current_rider_id/owner_name/...`）；`dms_vehicle_maintenance` 的 `maint_cost/maint_vendor/next_maint_date` 全空；`dms_vehicle_energy_log.voucher_url` 全空；`dms_rider.id_card/entry_date/driver_license/settle_method` 全空。
- **影响**：「车辆台账/保养/证件」页面字段恒空。
- **状态**：⬜

#### DMS-CLEAN-01 [P2] DMS 孤儿表 5 张

- `dms_logistics_ship`/`dms_return_receive`/`dms_ship_order`/`dms_purchase_receive`/`dms_dispatch_record` → 走观察期流程。
- **状态**：⬜

---

### 3.10 商城 / 营销域（erp-mall、erp-marketing）

#### MKT-BREAK-01 [P0] 商城商品「新增写废弃表、列表读视图、删除删幽灵表」

- **证据**：`MallAdminController.java:226` 列表读视图 `v_mall_product`（实测 **6 行**）；`:245` 新建写已废弃 `mall_product`（**85 行 `deleted=0`**）；`:264` 删除也按 `mall_product` 定位与软删。
- **⚠️ 本轮核验修正（子代理初判"忘记改"，实为有意设计）**：`MallAdminServiceImpl.java:625-649` 有长篇注释说明原因 —— 「`erp_product` 是 ERP 商品主数据（product_code 唯一、含单位/分类/价格体系/审批与库存联动等必填约束），由商城弹窗仅凭「商品编码+名称+价格」INSERT 会造出半可用的主数据，且可能撞 product_code 唯一约束。**正确做法是新增时从 ERP 商品档案选取既有商品再上架**」。删除侧注释（`:699-705`）同样说明「改闭环需业务确认是软删 `erp_product` 还是仅下架」。
- **影响**：后台「新增商品」保存成功但列表看不到（用户视角 = 功能坏了），且持续往废表写脏数据；「删除」删幽灵表、列表行删不掉。
- **两个候选修法（需产品拍板）**：
  - **A（推荐，代码注释自述的正确做法）**：把「新增商品」改为「从 ERP 商品档案选取 → 上架」（后端 `createProduct` 按 `product_code` 查 `erp_product`，找到则置 `mall_shelf_status=1`；找不到明确报错提示先建商品档案）。需前端配合改交互。
  - **B**：允许商城新建商品，但补齐单位/分类/价格体系等必填字段后再写 `erp_product`。
  - 删除侧：先统一为「下架」（`mall_shelf_status=0`，可逆），是否允许软删主数据另议。
- **状态**：⏸ 需产品拍板（A/B）

#### MKT-BREAK-02 [P1] `mall_product` 是商品的第二份数据，且主键类型不兼容

- **证据**：`mall_product.product_id` 是 **`character varying`**（存 `product_code` 文本如 `'SP-20260704-040'`），`erp_product.id` 是 `bigint` ⇒ `LEFT JOIN erp_product e ON e.id = m.product_id` **直接报 `bigint = character varying` 类型错误**，永远无法用 ID 关联。
- 按 `product_code` 文本比对：`erp_product` 83 行全部能在 `mall_product` 找到同码行；但 `erp_product` 仅 6 行未删、`mall_product` 85 行全未删 ⇒ **商城在售的是 ERP 已删商品**。
- **修法**：**确认合并** —— `mall_product` 整体废弃，商城统一走 `v_mall_product`（源 `erp_product` + `erp_stock`）。
- **状态**：⬜

#### MKT-BREAK-03 [P2] `erp_product` 商城上架列全空

- `approval_status`/`approval_by`/`approval_time`/`mall_category_id`/`mall_category_name`/`mall_display_title`/`mall_description`/`video_url`/`rich_text_detail` **全 0/83** ⇒ 商品上架审核、商城详情页字段无数据来源。
- **状态**：⬜

#### MKT-CLEAN-01 [P2] 营销老模型孤儿表 6 张（`erp_marketing_*`）+ `mkt_presale_order` 注意

- `mkt_presale_order` **不在**孤儿清单内 —— 被 `MarketingQueryMapper.java` 的 raw SQL 引用 ⇒ **实体删除 ≠ 表可删**（D-14）。
- **状态**：⬜

#### MKT-AUTHZ-01 [P1] `erp.marketing.*` / `erp.b2b.*` 零权限码

- 含 C 端公开接口，需**逐个甄别**哪些必须公开、哪些是后台管理（`MallAdminController` 等后台接口必须补）。
- **状态**：⬜

---

### 3.11 HR 域

#### HR-BREAK-01 [P2] 部门/数据权限链路三处同时断

- `sys_user.dept_id` **0/75 非空**；`sys_dept` **0 行**（`DataScopeAspect`/`RbacService.isDescendantDept` 读它）；`sys_user_data_scope` 6 行**全部 `deleted=1`**（`target_ids` 都是 `9001`）。
- **影响**：按部门的数据行级隔离**完全未启用**；"按部门看数据"要么拿全量要么拿不到（越权风险）。
- **修法**：见 PUR-BREAK-02（同一件事，A-01 迁移顺序）。
- **✅ 已拍板（2026-09-21，用户）**：启用部门维度，先冻结不改代码（同 PUR-BREAK-02）。
- **状态**：🔄 冻结中

#### HR-AUTHZ-01 [P1] HR 模块 40 条僵尸权限码

- `E-02` 分布中 hr 占 40 条（集中在 crm 57 / hr 40 / finance 25 / system 25 / erp 23）。
- **状态**：⬜

---

### 3.12 其余模块（打印、固定资产、预算、发票、observability）

#### MISC-DUP-01 [P1] 打印 v1 / v2 两套（表 + 实体）

- `erp_print_task`/`erp_print_template`(0 行，`printing/entity/`) vs `sys_print_task`/`sys_print_template`(0 行，`printing/entity/v2/`)。
- **修法**：随打印管理菜单（61205）补挂时一并定版，**v1 整体删除**。菜单 61205 已挂 5 个页面（09-19 迁移 `V11.428.0`）。
- **状态**：⬜

#### MISC-DUP-02 [P2] 两套 key-value 配置（`dms_config` 97 行 vs `sys_config` 173 行）

- 列集合**完全不重叠**、业务键无交集 ⇒ **不是表级重复**，是**能力重复**（后续新参数不知进哪张表）。
- **修法**：**不删表**，出《参数落位规则》并写入开发规范。
- **状态**：⬜

#### MISC-AUTHZ-01 [P1] `erp-fixed-asset`(8) / `erp-budget`(7) / `erp.payment`(9) 控制器无鉴权

- 见 E-01 批次 1/3。
- **状态**：⬜

---

### 3.13 前端工程 / 架构 / 门禁

#### ARCH-01 [P2] 两个菜单一致性脚本应接进 CI（当前 CI 只跑 repo-hygiene）

- **口径问题已澄清（本轮实测）**：`CLEANUP_DECISIONS` §4 建议"先修 `check-menu-targets.py` 口径"，但实际上 **`tools/check-menu-list-paths.py` 已专门覆盖 `list_path` 链路**（`display_mode=1` 的菜单走它），两者分工明确、**无需再改口径**。
- **本轮实测结果（2026-09-20）**：
  - `check-menu-targets.py`：菜单指向组件不存在 **0 条**（说明 `V11.427.0` 的采购换货单修复**已生效**）；`menu_name` 重复 **3 组**；同一组件被多菜单引用 **5 个**。
  - `check-menu-list-paths.py`：58 条 `list_path` **全部可解析、0 悬空**。
- **待处理（源自上面实测）**：3 组重名中的「销售退货申请」70011 与 80091 **path 完全相同**（`sales/return-apply/form`）= 真重复挂载，按 D-04 应删 70011；另两组（其他收入 70542/80116、客户 80200/80510）是**同名不同功能**，改菜单名消歧。
- **CI 现状**：`.github/workflows/ci-optimized.yml` 只接了 `check-repo-hygiene.sh`；两个菜单脚本都还是手工跑。
- **建议**：给两个脚本加 `--ci` 开关（发现问题时 `exit 1`）后接入 CI。
- **⚠️ 注意**：修改 CI 流水线属需确认操作，**本轮未动**。
- **状态**：⬜（脚本已可跑；接 CI 待确认）

#### ARCH-02 [P2] 字段收敛专项：`erp_sale_return`(133 列) / `erp_sale_return_doc`(132 列)

- 高度重合、非重复但**字段复制到维护危险的程度**（改一处漏一处）。**单独立项**，不在本轮。
- **状态**：⬜

#### ARCH-03 [P1] 建立《对称模块同步清单》

- 采购/销售对称的 40 组接口（order 11 / exchange 14 / return 10 / contract 5）**逐个方法一一对应**，任一侧改审批流/单号规则必须两侧同步改。
- **产出**：写入 `AI_DEVELOPER_RULES.md` 或 `AGENTS.md`。
- **状态**：🔄 **部分完成**（2026-09-21 复核更正：此前标 ⬜ 是误标）
  - **已做**：`AI_DEVELOPER_RULES.md` §9.3「对称模块必须同步改（采购↔销售）」（该文件 09-20 23:22 版，L268-280 附近）已落地——写明 40 组分布（order 11 / exchange 14 / return 10 / contract 5）+ 同步规则 + 示例。
  - **未做**：要求的是「**逐个方法一一对应**」的清单（方法级），现存只有**分组建模 + 示例**，没有方法级映射表。差额就是这一张表。

#### ARCH-04 [P1] 把《参数落位规则》写入开发规范（对应 MISC-DUP-02）

- 内容：技术配置进 `sys_config`、DMS 业务参数进 `dms_config`、全局默认（`tenant_id=0`）+ 租户覆盖的读取姿势（`@InterceptorIgnore` + 显式租户）。
- **状态**：✅ **已完成**（2026-09-21 复核更正：此前标 ⬜ 是误标）
  - 证据：`AI_DEVELOPER_RULES.md` §9.2「参数落在哪张表」（同文件 L255-266 附近）已逐条写明 `sys_config` / `dms_config` 的分工、`tenant_id=0` 全局默认 + 租户覆盖的读取姿势，并与 §9.1 的 `@InterceptorIgnore` 口径衔接。

#### FE-CLEAN-01 [P2] 工作区运行产物

- `backend/*.log` **299MB**、`frontend/.._tool-results_*.png`、`backend/cols.tmp`、`*/**/.atcode` 285 个文件。
- 已有 `tools/archive-runtime-logs.sh` + `check-repo-hygiene.sh` 门禁。
- **状态**：⬜

#### FE-CLEAN-02 [P2] 文档与代码不一致

- `docs/.../存储配置开发文档.md:54,371-378` 断言 `/api/storage` 前缀"是活的"，实际 `cn.aiedge.storage` 从未进 `scanBasePackages`，整体 404（D-11）。
- `frontend/docs/ui-components/feedback-components-usage.md` 通篇用不存在的路径 `@/components/@ai-ready/common/components/feedback`（D-12）。
- **修法**：更正/删除。
- **状态**：🔄 **部分完成**（2026-09-21 复核更正）
  - **已做**：① 存储配置文档**已自我更正**（现明确写 `/api/storage/config`、`/test` 均 404，无需再动）；② `docs/.../设置模块/系统参数开发文档.md` 本轮更正 5 处（"`/api/config` 是内存 Map" 已过时、"系统配置页走另一套 `/api/system/config`" 是错的——两页共用 `/api/config`、路由行号 1788→98，另补 `/api/system/config` 无调用方且与 `/api/config` 共用权限码的说明）。
  - **未做**：① `feedback-components-usage.md:35,79,121` 仍在用**不存在的**路径 `@/components/@ai-ready/common/components/feedback`（该目录不存在）；② 同一份《系统参数开发文档》里仍有别的过期描述（例：称支付配置"写走 `/config/save-value` 只有内存"——那批 2026-09-21 已改为真实读写 `sys_project_config`）。**需一次专门的文档对齐**，属独立一轮。

---

## 4. 需求业务拍板的清单（汇总，共 15 项）

| # | 问题 | 影响条目 | 我的建议 |
|---|---|---|---|
| 1 | `tenant_id=0` 与「平台租户=1」谁是平台？0 号租户是真实租户吗 | F-01/03/07、平台-BREAK-03 | 统一为 0=平台全局，并加 `is_global` 显式建模 |
| 2 | 普通租户用户该不该看见 `tenant_id=0` 的数据 | F-01/07 | 不建议直接可见，靠显式"全局数据"标记 |
| 3 | `system:*` 权限码能否授予租户角色 | F-05 | 不接受 → 需引入"平台专属码"机制 |
| 4 | 「切换租户」的产品语义（切会话 vs 切视图） | F-10 | 切会话身份且仅超管可切 |
| 5 | `sys_tenant_menu` 的授权主体是平台还是租户管理员 | F-06/07 | 平台侧专属能力 |
| 6 | 文件访问是否接受"UUID 即权限" | F-08 | 不接受 → 路径加租户层级 |
| 7 | 授权模型：菜单可见性由 `sys_role_menu` 还是从权限派生 | 平台-AUTHZ-01 | 从权限派生，消除双维护 | ✅ **2026-09-21 决议：从权限派生**（分阶段，依赖 E-04 补码） |
| 8 | 部门维度是否启用（决定 A-01 迁移时机） | PUR-BREAK-02、HR-BREAK-01 | 启用后再迁移 | ✅ **2026-09-21 决议：启用，但先冻结不改代码** |
| 9 | 库存双轨以哪一轨为准 | STK-BREAK-01 | 以 WMS 为准（有流水可回溯） | ✅ **2026-09-20 决议：以 `erp_stock` 为准**（本行建议已过期，以决议为准） |
| 10 | 退货是否"一申请一单" | SAL-BREAK-01 | 是，并自动生成草稿退货单 | ⏸ 待拍板 |
| 11 | 采购合同保留 ERP 还是 CRM | PUR-BREAK-01 | 保留 CRM，删 ERP 菜单/页面/代码 | ✅ **2026-09-21 决议：保留 ERP —— 补建 `purchase_contract` 表 + 补 `/page` 接口**（与建议相反） |
| 12 | DMS 结算是否本期范围 | DMS-BREAK-02 | 不在则删菜单+表 |
| 13 | 财务口径确认：总账以凭证重算是否认可 | FIN-BREAK-01 | 认可，且统一"凭证→总账"单向派生 |
| 14 | 应收/应付是否挂菜单（子分类账） | FIN-DUP-02 | 挂菜单，与查应收/查应付并存 |
| 15 | 往来单位三页面是否收敛为一个页面三视图 | MD-DUP-04 | 建议收敛 |

**另需确认**：24 张无明细销售单 / 22 条悬空 `order_id` / DMS 24 条悬空 `source_bill_no` 是 E2E 造数还是代码缺陷（值形态都像测试数据，但**无法从只读数据判定**）。

---

## 5. 不需拍板、可直接动手的清单（按建议顺序）

| 顺序 | 条目 | 类型 | 预估改动面 |
|---|---|---|---|
| 1 | FIN-BREAK-02 凭证头 `summary`/`source_no` 漏写 | 缺陷 | 1 个网关类 + 1 条回填迁移 |
| 2 | MKT-BREAK-01 商城商品写废弃表 | 缺陷 | `MallAdminController` 2 个方法 |
| 3 | STK-BREAK-02 6 处绕过库存唯一写入口 | 缺陷 | 6 个 Service/Controller |
| 4 | 平台-BREAK-02 `insertFill` 覆盖 tenantId | 缺陷（跨租户写错位） | 1 个配置类 + 单测 |
| 5 | MD-DUP-03 删 `erp-party` 客户等级价 | 死代码 | 1 实体 + 引用 |
| 6 | STK-DUP-02 删 `wms_warehouse` + 实体 | 死表/死代码 | 1 实体 + 1 迁移 |
| 7 | 平台-BREAK-01 租户上下文 fail-closed | 加固 | 1 拦截器 + 4 个登录入口 |
| 8 | F-02 `X-Tenant-Id` 与会话比对 | 加固 | 新增 1 拦截器 + 逐步改 104 处 |
| 9 | A-06 5 张旧 RBAC 表删除 | 死表 | 1 迁移（需备份+观察期） |
| 10 | ARCH-03/04 两份规则文档 | 文档 | 2 处规范 |

---

## 6. 执行日志（追加式，最新在下）

| 日期 | 条目 | 动作 | 验证 |
|---|---|---|---|
| 2026-09-20 | 本文建立 | 汇总四份既有报告 + 两份新专项审计（双层权限、数据断点） | 新增 2 个子代理审计报告，均带 file:line/SQL 证据 |
| 2026-09-20 | FIN-BREAK-02 | 网关补 `setSummary`/`setSourceNo`；新增回填迁移 V11.436.0 | 待编译 + 启动验证 |
| 2026-09-20 | 平台-BREAK-02 | `insertFill` 加"仅在 tenantId 为空时填充" | 全仓 grep 确认无实体自初始化 tenantId |
| 2026-09-20 | STK-BREAK-02（5/7） | 采购入库回冲、采购退货、零售结算、换货过账与回滚改走 `InventoryChangeEvent` | 剩 2 处需设计定稿（已写清原因） |
| 2026-09-20 | 平台-BREAK-01（1/4） | `ClientAuthController` 登录补写 `session.tenantId` | 另 3 个入口：PDA 桩需拍板、商城 C 端需设计、`AuthServiceImpl` 疑似无调用方 |
| 2026-09-20 | 核验纠错 | 子代理报告 **4 处误判**已就地修正：MKT-BREAK-01（实为有意设计）、wms_warehouse（实为活的扩展实现，有 3 处消费者）、erp_warehouse 行数（105 非 3）、A-06 五表（早已由 V11.433.0 删除） | 教训：**"无引用/可删"结论必须复核后才能落到删除动作** |
| 2026-09-20 | 编译验证 | `./mvnw -o -DskipTests -pl core/base/core-base,erp/erp-finance,erp/erp-purchase,erp/erp-sales -am compile` | **BUILD SUCCESS**（EXIT=0），5 个改动文件全部通过 |
| 2026-09-20 | ⚠️ 未验证项 | `V11.436.0` 回填迁移与所有运行时行为**尚未在启动环境验证** —— 需一次后端重启（Flyway 执行迁移 + 打凭证/库存接口） | 重启后端属共享环境操作，**等待用户确认时机**，不擅自执行 |
| 2026-09-20 | 实测复核 | `check-menu-targets.py` / `check-menu-list-paths.py` 实跑 | 坏菜单 **0 条**（V11.427.0 已生效）、`list_path` 58 条 **0 悬空**；暴露出「销售退货申请」70011/80091 **path 完全重复**（真重复挂载）；CI 仅接 repo-hygiene |
| 2026-09-20 | 平台-SEC-01 | PDA 登录由桩改为真实鉴权（`SysUserMapper` + `PasswordEncryptor` + status/租户校验 + 写 session.tenantId） | wms 模块 **BUILD SUCCESS** |
| 2026-09-20 | STK-BREAK-01 | 按用户决策「以 erp_stock 为准」：新增迁移 `V11.437.0` 校准两轨 + `InventoryServiceImpl.decrease()` 镜像失败改为抛异常回滚 | wms 模块 **BUILD SUCCESS**；实测差异仅 1 行 |
| 2026-09-20 | F-01 | 查清 `tenant_id` 语义：admin 在租户 1（SYSTEM，硬编码正确）；租户 0 是已禁用的注册残留；0 上三类数据已按表列出 | 数据清理待逐表确认归属 |
| 2026-09-20 | **重启验证（第 1 轮）** | `clean install` 8 个模块 + 重启（3:24 构建 / 134s 启动，无启动错误） | ✅ 迁移 `V11.436.0`+`V11.437.0` 均 `success=true`；✅ 凭证回填 summary **0→69/69**、source_no **0→59/69**；✅ 库存两轨对齐 `95/95`、`152/152`（矛盾数据消失） |
| 2026-09-20 | 平台-SEC-01 复验 | 补 `SaTokenConfig` 白名单后重启，实测 PDA 登录 | ✅ 不再返回「请先登录」（白名单生效）；✅ 不存在账号与错误密码均被拒（真实鉴权生效） |
| 2026-09-20 | 夜间批次 A（安全加固） | F-02 新增 `TenantHeaderInterceptor` + 前端 4 处去 `\|\| '1'`；F-03 `effectiveTenant` 强制会话租户；F-04 补 2 个权限码 + 3 个读接口注解；F-06 `assertPlatformAdmin()`；平台-BREAK-05 三处写死 `tenant_id=1` 改为上下文租户 + 调度器逐租户采集 | 后端 **BUILD SUCCESS**（4 模块）；待重启验证 |
| 2026-09-21 | **重启验证（第 2 轮）** | `MODULES="core/base/core-base,core/platform/core-platform,erp/erp-finance,erp/erp-observability,erp/erp-partner,erp/erp-printing,erp/erp-purchase,erp/erp-sales,hr/hr-base,wms,core/api/core-api" tools/build-backend.sh`（clean install 11 模块 → 3:51 产物 → 140.7s 启动成功） | ✅ `V11.438.0` 迁移 `Successfully applied`；✅ 启动日志「数据权限拦截器已挂载…位置 1，链路共 4 个插件」 |
| 2026-09-21 | 批次 A 专项验证 | 新增 `tools/verify-tenant-hardening.cjs`（14 项，**拒绝路径 + 放行路径都测**） | ✅ **14/14 全绿**：非超管带跨租户头 → `403 无权访问其他租户的数据`；带头=本租户/不带头 → 200；超管带头 → 放行；`/tenant/page` 非超管 → `403 system:tenant:list`、超管 → 200；`/tenant-menu/2` 非平台管理员 → `403 tenant:menu:query`；回归 `/menu/tree`、`/permission/page` 均 200 |
| 2026-09-21 | 既有验证脚本复跑 | `tools/verify-permission-changes.cjs`（细粒度权限接线 23 项） | ✅ **23/23 全绿**（模拟态跨请求保持、生效性清单 351+144+11=506 自洽） |
| 2026-09-21 | 拍板落库 | 用户决议 4 项写入本文：菜单授权**改从权限派生**；采购合同**保留 ERP、补建表 + 补 `/page`**；部门维度**启用但先冻结**；库存双轨以 `erp_stock` 为准（09-20 已定，表格建议行已标注过期） | 见各条「已拍板」与第 4 节表格 |
| 2026-09-21 | 菜单派生可行性实测 | `sys_menu` 无 `permission_code` 列；307 个叶子菜单的 `menu_code` 仅 **2 个**精确命中权限码，但**前缀规则成立**（`finance:other-income-doc` ↔ `finance:other-income-doc:view`）。按前缀规则实测：SYSTEM_ADMIN(18 码)/DEPT_ADMIN(16 码) → 可见 **3 个**菜单；E2E_T2_ADMIN(2 码) → **0 个**。根因是权限码库只覆盖 31/500 个 `erp:*`（九个模块零码，见 E-04）⇒ **派生必须先补码** | 结论写入 平台-AUTHZ-01 条目 |
| 2026-09-21 | PUR-BREAK-01 | 补建 `purchase_contract` 等 3 表 + 7 权限码（V11.439.0）；补 `/page`、POST、PUT、DELETE、`/export` 五端点并补鉴权；返回类型改 `ApiResponse`；Service 补 CRUD。顺带修掉 3 个连带缺陷（`findExpiringContracts` 类型错误、统计 DTO 缺 3 字段、`update` 静默丢列） | ✅ 迁移 success；**17/17 验证全绿**（含租户口径回查断言） |
| 2026-09-21 | STK-BREAK-03 | 防再生落 DB：迁移 `V11.440.0` 给 `wms_inventory` + `erp_stock` 各加 4 条 CHECK（幂等 DO 块）。**不加冗余断言**——四条 Service 路径本身守恒，历史违规全来自绕过 Service 的直写（STK-BREAK-02 那批反向路径），约束才是覆盖全部写入方的收口点 | ✅ 8 条约束落库；回归 `verify-tenant-hardening` **14/14** + `verify-permission-changes` **23/23** |
| 2026-09-21 | **提交 + 推送** | 上一批 175 文件 / +13675 −5218 提交为 `ce0dd04a9`，推送 `origin`(gitee) `3db75f8a0..ce0dd04a9` | ✅ 本地与 `origin/localization` 同步。⚠️ `github` 远端 `localization` **落后 93 个提交**且长期未同步（最老缺失 `d40ac870`），**未推**，待确认是否废弃 |
| 2026-09-21 | **E-04 权限码（7/9）** | 新增 `tools/gen-module-permission-seed.py`（按类级路由路径推导「域:资源:动作」，`--apply` 幂等插注解）。生成并应用 `V11.441.0`~`V11.447.0` 七个迁移，**485 个新权限码**，全部带 `api_path`/`method` 并显式关联超管角色 | ✅ `sys_permission` **500 → 994**；`api_path` **238/474 → 734/994**；同码重复 0 组；迁移全部 `success`；每个迁移均事务内干跑 + 幂等重跑校验 |
| 2026-09-21 | **E-01 鉴权注解（7 模块）** | 给 budget / fixedasset / stock+product+md / invoice / payment / party / marketing 共 **97 控制器 / 709 端点**补 `@SaCheckPermission` | ✅ 新增 `tools/verify-module-authz.cjs`（**两向断言**：超管放行证明码在库 + 非超管被拒证明注解生效）**7 模块全绿**；无 `@SaCheck*` 控制器 **224(57%) → 139(35%)**；端点 2028 → 1319 |
| 2026-09-21 | 门禁棘轮同步 | `known-unauthorized-controllers.txt` 删除 **104 行**（85 本轮 + 19 历史过期：含 09-20 erp-finance 批量迁移与已删除的 `FieldPermissionController`） | ✅ `AuthzAnnotationCoverageTest` + `PointcutTargetExistenceTest` **4/4 通过**（含 `baselineMustNotBeStale`） |
| 2026-09-21 | 三个回归复跑 | `verify-tenant-hardening` / `verify-permission-changes` / `verify-purchase-contract` | ✅ **14/14、23/23、17/17** 全绿 |
| 2026-09-21 | **域前缀归一（本轮当场解决）** | 迁移 `V11.448.0`：① 往来单位 `partner:*` → `party:*`（同一业务对象被拆两个域；历史 `partner:merge` 实测零引用）；② `/api/erp/payment` 的 `payment:<动作>` → `finance:payment:*`（历史 `payment` 域指**第三方支付网关** `/api/payment`、`/refund`、`/reconciliation`，同名不同物）；历史 9 个 `payment:*` **保持不动**。用 `UPDATE permission_code` 改名（保留 id 与角色关联），并同步改 6 个控制器的注解字面量 | ✅ 迁移 `success`；干跑校验：`partner:*` 归零、`party:*` 45、`finance:payment:*` 9、历史 `payment:*` 仍 9、幂等；七模块鉴权 **40/40**；三回归 **14/14 + 23/23 + 17/17** |
| 2026-09-21 | 🔴 **新发现（商城支付回调被 401 挡住，且不能只加白名单）** | `SaTokenConfig` 白名单里**只有** `/api/v1/mall/auth/**`；实测 `POST /api/v1/mall/payments/callback` 未登录返回 `401 请先登录` —— 网关服务器**不带商城用户 token**，回调永远进不来 ⇒ 支付结果无法回写、订单停在待支付。**但直接加白名单会造出漏洞**：`MallPaymentServiceImpl.handleCallback` 只读 `out_trade_no` + `trade_status` 就改单，**零验签**（107 行代码无任何 sign/verify/hmac）⇒ 加白名单 = 任何人 POST 单号即可把订单标为已支付。**正确顺序：先验签、再加白名单**；另回调未找到订单时仍返回"处理成功"，应返回失败让网关重试 | 实测 8 条 C 端路径全部 401；`tools/probe-mall-public.cjs`。**待定**：接的是哪个支付渠道（决定验签算法） |
| 2026-09-21 | **商城游客浏览 + 价格开关接线** | 发现 `tenant_shop_config.allowGuest` / `guestShowPrice` **字段早已存在、前端开关早已就位，但后端从未消费**（两个空开关）。本轮接线：新增 `MallGuestAccess`（解析「逛的是哪家店」：会话租户 → 其次 `X-Tenant-Id` 头；再读该店配置）；`MallProductServiceImpl` 6 个读方法全部过 `requireShop()` 准入，价格按 `guestShowPrice` 决定是否下发；`SaTokenConfig` 放行 `/api/v1/mall/products/**`（放行可达性，数据范围仍由 requireShop 控制） | ✅ 新增 `tools/verify-mall-guest.cjs` —— **11/11 全绿**（NOT_ALLOW→403、ALLOW/HIDE→200 且价格为空、ALLOW/SHOW→价格有值、无店铺标识→400 明确报错、已登录不受开关影响），脚本自还原配置；回归 14/14 + 23/23 |
| 2026-09-21 | 📌 **产品规划（用户 2026-09-21）** | 当前是 **B2B 租户商城**（一租户一店），**后期要上线「区域商城」（对标美团外卖：一个区域多商家、消费者按位置浏览）**。影响：① `tenant_shop_config` 是 **1 租户 : 1 店铺** 的单行配置，区域商城需要「多店铺实体」；② C 端「我在哪」的入参届时要从 `tenantId` 换成 `regionId + shopId`（或按定位算区域）；③ B2B 客户等级价 与 零售门店价 是两套模型，**不可复用**；④ 区域商城是零售场景，售价是核心，而实测 `erp_product` **6 行商品 0 行有 `retail_price`**（批发价 6 行有值）⇒ 届时零售价体系需补齐 | 已把解析收口在 `MallGuestAccess` 一处，将来换粒度只改该类 |
| 2026-09-21 | **支付渠道抽象体检（发现全是桩）** | 用户明确「渠道由租户自选（微信/支付宝/都接），平台提供接口与方法」。核查 `core-payment`：`PaymentChannel` 接口**确实已有** `handleCallback(String)` 等 10 个方法，含微信/支付宝/银联/银行/现金五个实现 —— 但**实现全是桩**：`WechatChannel.handleCallback` 把报文包成 `PaymentRecord` 并**硬编码 `status=2`（成功）**、`AlipayChannel` 写着 `// TODO: 解析支付宝回调数据`、`createPayment`/`closePayment`/`createRefund` 同为桩。即**「回调成功」当前可以被凭空断言** | 逐文件读实现确认，非推测 |
| 2026-09-21 | **支付回调验签契约 + 商城回调重做** | ① 新增 `cn.aiedge.base.payment.PaymentCallbackVerifier`（契约放 core-base，实现留 core-payment —— 本仓既有方向：core-base 放接口、各模块放实现）+ `PaymentCallbackContext/Result/VerificationException`；② 商城回调改为 `POST /callback/{tenantId}/{channel}`：按租户+渠道分发验签，**无实现认领 ⇒ 401 fail-closed**；③ **删除**原 `handleCallback(Map)`「无验签即改单」路径；④ 订单改状态时**补上 `tenant_id` 条件**（原实现无此条件，多租户下等于跨租户改单），并加**金额校验**防改价重放；⑤ 应答体改由渠道决定（微信要 `SUCCESS`、支付宝要 `success`，原返回平台 JSON 会被网关当失败无限重推） | ✅ 新增 `tools/verify-payment-callback.cjs` **5/5**（旧路径 404、三渠道均 401 `UNSUPPORTED_CHANNEL`、库中伪造单号 0 条、商城已支付订单 0 条）；回归 11/11 + 14/14 + 17/17 |
| 2026-09-21 | ⚠️ **支付回调启用步骤（实为 4 步，顺序不能反）** | 动手时发现原先写的「三步」漏了前置一步：① **补凭据模型** —— `PaymentChannelParam` 原字段只有 appId/merchantNo/appSecret/notifyUrl/enabled，**没有验签需要的公钥或 APIv3 密钥**，verifier 无处读密钥；② 实现 `PaymentCallbackVerifier`；③ 为租户配齐凭据并使 `isConfigured(tenantId)=true`；④ **最后**才把 callback 路径加进 SaTokenConfig 白名单 | 类注释已按 4 步更正 |
| 2026-09-21 | **启用步骤 ① 完成：补验签凭据字段** | `PaymentChannelParam` 增 4 个可选字段（JSON 内扩展，向后兼容）：`alipayPublicKey`、`wechatApiV3Key`、`wechatPlatformSerial`、`wechatPlatformPublicKey`。刻意打破该类原「逐字对齐对标前端、不加字段」的约定并写明理由。**⚠️ 这些值与其它 `sys_project_config` 一样明文落库**，真实收款前应加密 | 编译通过 |
| 2026-09-21 | **启用步骤 ② 完成（支付宝）：`AlipayCallbackVerifier`** | RSA2/SHA256withRSA 验签，严格按支付宝规则：待验签串剔除 `sign`/`sign_type` 与空值、按参数名字典序升序、`k=v` 以 `&` 连接、用**解码后**的值；额外校验 `app_id`（签名只证明「来自支付宝」，不证明「发给本 app」）；凭据按租户读（回调无会话 ⇒ `setTempTenantId` + **finally 清理**）；应答纯文本 `success`/`failure`。**为什么先做支付宝**：只需一个公钥，无证书链，能最快把契约跑通并自证正确 | ✅ 新增 `AlipayCallbackVerifierTest` —— **12/12 通过**，测试用**自生成 RSA 密钥对**签名，**不需要真实支付宝凭据**。覆盖：合法签名通过、TRADE_FINISHED 算成功、WAIT_BUYER_PAY 不算、**篡改金额被拒**、**他人私钥签名被拒**、app_id 不符被拒、缺 sign 被拒、未配置/停用渠道被拒、应答体格式、待验签串规则 |
| 2026-09-21 | **启用步骤 ② 端到端确认** | 重建后实测回调端点行为**按渠道区分**：`POST /v1/mall/payments/callback/1/ALIPAY` → `401 failure`（verifier **已装配**、该租户未配凭据 ⇒ fail-closed 并返回渠道要求的失败应答）；`/WECHAT` → `401 UNSUPPORTED_CHANNEL`（尚未实现该渠道） | ✅ `verify-payment-callback.cjs` **6/6**；回归 14/14 + 11/11 + 17/17 + 14/14 |
| 2026-09-21 | **启用步骤 ② 补齐（微信 V3 + 银联）** | **微信 V3**（`WechatCallbackVerifier`）：① 签名字符串是 `时间戳\n随机串\n原始报文\n`（**结尾 \n 不能少**）；② `resource` 要 AES-256-GCM 解密（APIv3 密钥 + resource.nonce + associated_data）；③ **时间戳 ±5 分钟容差**（漏了则可无限重放截获的合法回调）；④ 证书轮换按 `Wechatpay-Serial` 查表；⑤ 金额 `amount.total` 单位是**分**须换元；⑥ 应答是 JSON 不是纯文本。<br>**银联**（`UnionPayCallbackVerifier`）：① 待验签串里 value 要 **URL 编码**（与支付宝「用解码后的值」**相反**，照抄支付宝写法会 100% 失败）；② `signMethod` 决定摘要（`01`=SHA1、`11`=SHA256，**未知取值直接拒绝**，不猜默认算法）；③ `certId` 选证书（同为轮换友好）；④ `txnAmt` 单位是分；⑤ `respCode=00` 为成功 | ✅ `WechatCallbackVerifierTest` **12/12**、`UnionPayCallbackVerifierTest` **13/13**（均用**自生成 RSA 密钥对** + 自加密 resource，不需要真实凭据）；三渠道合计 **37/37** |
| 2026-09-21 | **凭据字段在前端暴露**（用户要求） | 「设置 → 支付配置 → 支付方式 → 参数配置」抽屉按渠道**条件展示**验签凭据：支付宝 → 公钥；微信 → APIv3 密钥 + 平台证书表；银联 → 平台证书表。证书表用 textarea + **保存前校验**（非法 JSON / 非 PEM 一律拦住）—— 因为后端 `parseCerts` 遇非法 JSON 会静默当「未配」，表现是「保存成功但回调永远被拒」，比当场报错难查得多 | ✅ `vite build` 通过；`verify-payment-callback.cjs` **10/10**（三渠道应答形状各异且都与渠道规范一致，伪造回调的响应里不出现任何成功体）；回归 14/14 + 11/11 + 17/17 |
| 2026-09-21 | **「未配置 / 配置无效 ⇒ 不生效」落地**（用户要求） | ① 新增 `PaymentCredentialValidator`：**真解析**而非看字段非空 —— 半截公钥、把**私钥填进公钥字段**、丢 PEM 头尾、Base64 合法但内容非密钥，一律判无效；证书表要求**每张**都有效（混一张坏证书会「时好时坏」，比直接失败更难查）。② 三个 verifier 的 `isConfigured` 全部改走它。③ `PaymentChannelConfigVO.available` 口径变更：原为「渠道 Bean 的 `isAvailable()`」，而**所有渠道实现都无条件返回 true** ⇒ 没配任何凭据也显示「可用」；现改为 `渠道可用 && 凭据就绪`，并新增 `credentialReady` 字段区分「渠道不支持」与「你没配凭据」 | ✅ `PaymentCredentialValidationTest` 8/8；实测渠道列表：ALIPAY/WECHAT/UNIONPAY `available=false, credentialReady=false`；BANK/CASH `true`（不走回调，恒就绪）。前端列表新增「未配凭据」提示 |
| 2026-09-21 | 🔴 **「凭证只能本租户自用」——查出 3 个跨租户缺陷** | **① `SysConfigService` 硬编码租户**：`SysConfigServiceImpl` 里 `private static final Long CURRENT_TENANT_ID = 1L;`（注释自称「简化实现」），其 `getValue/getConfig/getConfigsByGroup/getAllConfigs` **一律读租户 1**，既不认会话租户也不认 `setTempTenantId`。我最初用它读渠道凭据 ⇒ **所有租户的回调都会拿租户 1 的凭据验签**。已改为新增 `TenantChannelCredentialReader`（显式传租户、**租户为 null 直接拒绝查询**、**不回落平台行**）。<br>**② 渠道参数读路径回落平台行**：`findRow(key, tenantId)` 逻辑是「本租户没有 → 取平台行」，租户没配时会**读到平台凭据**并显示为已配置。渠道参数改用 `findOwnRow`（严格本租户）。<br>**③ 渠道参数写路径覆盖平台行**：`upsert` 用 `findRow` 找「已存在行」⇒ 本租户没配过时会把**平台那一行**当成已存在并更新它，即**租户保存自己的参数 = 覆盖平台配置**。同样改用 `findOwnRow` | ✅ `TenantChannelCredentialReaderTest` 5/5（断言查询条件里的 tenant_id 就是传入值、**不是常量 1**；租户为 null 时**不查库**）。支付模块测试合计 **50/50** |
| 2026-09-21 | ⚠️ **跨模块影响登记（非支付专属）** | `SysConfigServiceImpl.CURRENT_TENANT_ID = 1L` 是**全局**的「简化实现」，凡是经由 `SysConfigService` 读配置的功能**都读租户 1**。这不是支付引入的问题，但影响面远超支付（`sys_project_config` 的租户级配置、菜单显隐相关读取等）。**待另立专项**：把该常量替换为「会话租户 → 显式传入」并给缓存 `configCache` 加租户维度（现缓存仅按 key，无租户维度，一旦支持多租户会立刻串值） | ✅ **已于同日闭环**（见下方「SysConfigService 租户专项」两行） |
| 2026-09-21 | 🚫 **`github` 远端：决定不再同步**（用户裁定） | 用户明确「github 远端直接备注为不同步，以后这个不同步了」。**唯一推送目标是 `origin` = gitee `https://gitee.com/CozyNook/ai-ready.git`**；`github` 远端（`https://github.com/Get-windy/ai-ready.git`）保留在本地配置里但**永不推送**（本轮**不动 remote 本身**：删 remote 会一并删掉 `refs/remotes/github/*`，那是与历史 commit 比对的基准）。<br>**连带须知**：`.github/workflows/ci-optimized.yml`（`repo-hygiene` 等作业）因此**不会再在本项目触发**（它依赖 GitHub Actions）⇒ 该工作流已变成「本地可跑但无人在跑」。后续二选一：迁到 Gitee Go 流水线，或明确降级为手工脚本（`tools/check-repo-hygiene.sh`）并删掉工作流文件 | 🚫 决定不做（不同步）。若将来要彻底删远端：`git remote remove github`（本轮未执行） |
| 2026-09-21 | ⏭ **启用步骤 ③④ 未做（需业务输入）** | ③ 需**真实凭据**：支付宝（公钥 + appId）、微信（APIv3 密钥 + 平台证书）、银联（平台证书 + certId）。前端字段已就位，可直接在「支付配置」页填；④ 白名单必须在③之后（现在端点外部不可达）。<br>**另注**：银联应答体我按最常见的 `ok`/`fail` 实现，**上线前需对照你们签约版本的通知规范确认**（改 `ackBody` 一处即可）；现金（CASH）无网关回调，`UNSUPPORTED_CHANNEL` 是正确行为而非缺口 | 待用户提供凭据 |
| 2026-09-21 | **SysConfigService 租户专项 ① 侦察（决议依据）** | 逐个读全部 7 个 `SysConfigService` 消费点 + 真库数据，确认**没有一个键是"平台级共享"**：`inventory.mode`（控制器注释自称「支持租户级配置」）、`expense.approval.level1~3.approverId`（**费用审批人会被派到别的租户的人**）、`mall.product.default.sort`、`stock.alert.comparison`、`marketing.autoCampaign.globalFreq*`、`set:menu-config:hidden`、`user_page_config:*` 全是租户级。真库 `sys_project_config` **只有租户 1 的 21 行**；`sys_tenant` 里 id=0 是**已软删的注册残留**（不是"平台租户"），id=1 才是「系统租户」。另发现 `MenuVisibilityServiceImpl` 早已是正确写法（显式传 `tenantId`），从来不受该常量影响 | 结论：**读=会话租户 → 回落平台行(0)；写=严格本租户**，不需要"平台级键白名单" |
| 2026-09-21 | **SysConfigService 租户专项 ② 改造 + 验证** | ① 删掉 `CURRENT_TENANT_ID = 1L`，改 `scopedTenantId()`（会话租户 → 平台行 0）；② `configCache` 键由「配置键」改为「**租户ID + `\0` + 配置键**」（原缓存无租户维度，多租户一开就串值）；③ 写/删走新的 `SysProjectConfigMapper.selectRowByTenant`（**严格本租户、绝不回落**）——否则本租户没配过时会把**平台行**当自己的更新掉；④ Redis 历史键加租户段（`sys:config:history:<tenantId>:<key>`，原键共用 ⇒ 租户 A 能翻到、并回滚成租户 B 的旧值）；⑤ 变更事件 `sys:config:change` 报文补 `tenantId`；⑥ `getConfigValue` 补 `ORDER BY id DESC LIMIT 1`（`MenuVisibilityServiceImpl` 注释已指出该表无唯一索引、可能存在重复行 ⇒ 原来会抛 `TooManyResultsException`）；⑦ **同批修掉另一处同源硬编码**：`UserPageConfigController` 的 `config.setTenantId(1L)`（所有租户的页面配置都写进租户 1）<br>**顺带查证**：存量 15 行 `user_page_config` 的键尾 userId **全部属于租户 1 的用户**，故加租户条件不会让任何人的配置"消失" | ✅ 新增 `SysConfigServiceImplTest` **16/16**（会话租户、平台回落、**不猜租户 1**、缓存按租户隔离、写不更新平台行、删只用本租户行、历史键含租户、事件含 tenantId）；新增 `tools/verify-config-tenant.cjs` **28/28**（真机双租户交叉：租户 2 写的行 `tenant_id=2`、租户 1 读同一键 `null`、租户 2 回落读到平台行 `PLAT`、租户 2 保存同名键**平台行仍是 PLAT**、租户 2 删除后平台行仍 `deleted=0`、配置 Map 不含租户 1 独有键、页面配置落会话租户、存量键仍可读）；回归 `verify-tenant-hardening` **14/14**、`verify-payment-callback` **10/10**、支付模块 **50/50**；库中残留 0 行、临时授权已撤销 |
| 2026-09-21 | 🔎 **原判「`api_path` 指错控制器」经复核后订正：不是数据错，是两控制器共用一套码** | 原发现：`SysConfigController` 路由是 `/api/system/config/**`，而它用的码 `system:config:*` 在库里的 `api_path` 是 `/api/config/list`、`/api/config/save`、`/api/config/{configKey}` —— 另一个控制器 `SystemConfigController`（`/api/config`，读写 `sys_config`）的路径。<br>**逐项复核后的订正**：① 两个控制器**确实共用**这 6 个码（`system:config:{list,query,create,update,delete,export}`）；② 只有**超管**持有它们（`超级管理员` 6 个），无其它角色受影响；③ `api_path` 的**唯一消费方**是 `PermissionServiceImpl.checkApiPermission`（只经 `PermissionController` 暴露，无内部调用方；且它 fail-closed，异常/无匹配一律 false），全库 `api_path 非空 ∧ permission_type ≠ 3` 的**错配为 0**；④ 真正有前端调用方的是 `/api/config`（`frontend/.../api/config.ts` 全部指向 `/config/*`，`views/set/sys-params/index.vue` 与 `views/system/config/index.vue` **两个页面都 import 它**），`/api/system/config` **零前端引用**。<br>**结论**：`api_path` 记的是"唯一有调用方的那个控制器"，**它没填错**；错的是"一套码被两个控制器共用"。已在 `SysConfigController` 类注释写明这件事，并**禁止**在拍板前给它单独造码（那是给无调用方的接口造码） | ⏸ **待拍板**：**(A) 删掉 `SysConfigController`**（12 端点闲置，但它是 history/rollback/compare 的唯一实现，删了就没了）或 **(B) 把它的 history/rollback/compare 接到活的 `views/system/config` 页上**（`docs/.../系统参数开发文档.md` §10 也是这个建议）。**本轮不改数据、不造码** |
| 2026-09-21 | ✅ **「`ConfigChangeListener` 死缓存」已删除** | `cn.aiedge.base.config.ConfigChangeListener` 里的静态 `LOCAL_CACHE` + `getCachedValue` + `updateCache` + 只打 debug 日志的 `triggerCallbacks` **整段删除**（`grep -rn "getCachedValue\|updateCache\|LOCAL_CACHE" backend` 除本类外**零命中**，确认无外部/反射调用）；顺带删掉该类里从未被使用的 `@Autowired StringRedisTemplate` 字段。保留本类作为 `sys:config:change` 的唯一订阅入口（报文已带 `tenantId`，日志里一并打印）。<br>⚠️ 同时在类注释里写明**仍未实现的缺口**：它**并没有**去失效 `SysConfigServiceImpl.configCache`（进程内缓存），多实例部署下 A 实例改的配置 B 实例要重启/`refresh` 才可见 —— 单实例无影响，但别误以为已经有跨实例失效。<br>**重启后发现更彻底的事实**：启动日志打的是「Redis未配置，配置变更监听器以本地模式运行」（`redisContainer` 为 null）⇒ 本环境里**它连订阅都没建立**，`publishConfigChange` 的 `convertAndSend` 会被自己 catch 掉、只留一条 warn。即整条 `sys:config:change` 链路当前是**空转**的：既发不出去、也收不到。删掉的那个静态缓存不只是"没人读"，而是**整个类在运行期完全不起作用** | ✅ 编译通过；第 4 轮重启（147.0s 启动成功）后回归 `verify-config-tenant` **28/28**、`verify-tenant-hardening` **14/14**、`verify-payment-callback` **10/10** |
| 2026-09-21 | 🔧 **FE-CLEAN-02 局部修正**（本次发现连带的文档错） | `docs/.../设置模块/系统参数开发文档.md`：① 原写「本页读写 `/api/config` 的**内存 Map**」→ 自 2026-09-18 落库改造（V11.393.0）后**已不成立**，现在真实读写 `sys_config`；② 原写「系统模块『系统配置』页走**另一套** `/api/system/config`，与本页无任何交集」→ **错**，该页 import 的也是 `@/api/config`，**两页共用同一套后端**；③ 路由行号 `dynamicRoutes.ts:1788` → 实际 `:98`。均已就地更正并标注日期 | ⚠️ **该文档仍有其它过期处未清**（例：§5.4 附近称支付配置「写走 `/config/save-value` 只有内存」——那批已在 2026-09-21 改为真实读写 `sys_project_config`）。归入 **FE-CLEAN-02**，需一次专门的文档对齐，不在本轮做完 |
| 2026-09-21 | 🔎 **新发现（死代码）：`ConfigChangeListener.LOCAL_CACHE` 只写不读** | `cn.aiedge.base.config.ConfigChangeListener` 订阅 `sys:config:change` 维护 `LOCAL_CACHE`，但全仓 **`getCachedValue` 零调用**（`grep -rn ConfigChangeListener` 只在该类自身与 `MapKeyResolver` 的一句注释里出现）⇒ 这个"本地缓存"从未被任何读取方使用，纯粹是内存泄漏式的死代码。另外它的缓存键**没有租户维度**，与本次修复前的 `SysConfigServiceImpl.configCache` 属同一缺陷类 | ✅ **同日已闭环**：确认零外部引用后整段删除（见上方「`ConfigChangeListener` 死缓存已删除」一行）。原计划的措辞是"待办"，实际当轮就做了，因为复核零引用只花了两次 grep |
| 2026-09-21 | **同批修掉第 3 处「猜租户」：`DocQueryController`** | `currentTenantId()` 原为「Session 里取不到 tenantId ⇒ 回退默认租户 1」，注释还写着「使用默认租户 1」。本类全部端点 `@SaCheckLogin`，一旦解析不出租户（旧 token、登录链路漏存 tenantId），**回退 1 = 把租户 1 的经营历程/待审批/业务草稿单据展示给另一个租户的用户**。改为抛 `BusinessException(401, "无法确定当前租户，请重新登录")`，与 `SetAppCenterController`/`SetRebuildController`/`TenantController` 的「解析不到就拒绝」口径一致 | ✅ 编译通过；运行回归见下方「重启验证（第 3 轮）」 |
| 2026-09-21 | ⚠️ **登记（未修）：平台配置四件套 `*ConfigService` 的租户口径没想清楚** | `SmsConfigService`/`MailConfigService`/`StorageConfigService`/`SecurityPolicyService` 四个平台配置服务各有两处 `if (tenantId == null) tenantId = 1L;`，外加一处**不带租户条件**的兜底查询 `selectOne(new LambdaQueryWrapper<>().last("LIMIT 1"))`。<br>**实测修正了我最初的两个判断**：① `= 1L` 分支**当前不可达**（四个控制器都显式传 `0L`）；② 那条无条件的 `LIMIT 1` **并不会读到别的租户的行** —— 这四张表都不在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 里，租户插件会把它注入成 `AND tenant_id = <会话租户>`（超管会话经 `isTenantScopeExempt()` 整体豁免注入）。<br>**真正的结构性问题是**：正因为会被注入，非超管会话下 `getConfig(0L)` 实际执行的是 `tenant_id = 0 AND tenant_id = 2` ⇒ **永远 0 行**，只能靠那条兜底去读"会话租户自己那一行"。也就是说**这四个平台配置页对非超管会话根本读不到平台行**（读到的是本租户的行，页面却叫"平台配置"）；超管会话因为被豁免注入才正常。实测：`/api/sms/config` 超管 → 返回 `tenant_id=0` 的 aliyun 行 | ⬜ **待业务拍板后再动**，因为两条修法语义完全不同：<br>**(A) 这四张表是平台级** → 加进 `IGNORE_TENANT_TABLES`（或对这几个 Mapper 加 `@InterceptorIgnore(tenantLine="true")`），并删掉那条兜底；<br>**(B) 将来要支持每租户短信/邮件/存储配置** → 控制器不能再传 `0L`，要传会话租户，`= 1L` 兜底一并去掉。<br>**我先按 (A) 做过一版、已主动回滚**：理由是当初的判断（"那条兜底会读到别的租户的行"）被上面这次实测**证伪**，基于已被证伪的前提改 4 个文件不诚实。`sys_*_config` 目前各只有 1 行 `tenant_id = 0`，所以两种修法都不会造成数据迁移 |
| 2026-09-21 | **重启验证（第 3 轮）· 租户专项闭环** | `tools/build-backend.sh`（clean install 3 模块 → 1:43 产物 → 134.9s 启动成功）后跑全套回归 | ✅ `verify-config-tenant` **28/28**、`verify-tenant-hardening` **14/14**、`verify-payment-callback` **10/10**、支付模块单测 **50/50**、`SysConfigServiceImplTest` **16/16**；`/docquery/{business-history,pending-docs,draft-docs}` 超管均 **200 且有真实数据**（`/business-history/page` 返回 `list` 非空，销售出库单 XSCK202609140005 等）、未登录 **401**（异常改动没有把 `@SaCheckLogin` 顶掉）；DB 残留复核：`sys_project_config` 仍是 **租户 1 的 21 行**、`e2e.cfgtenant.*`/`user_page_config:e2e-verify:*` **0 行**、租户 2 角色权限数回落 **2**、`sys_sms_config` 回到 **1 行 tenant_id=0**（探针数据已清） |

| 2026-09-21 | **8 项待办逐项复核（用户点名，结论全部回到代码/DB 实证，不采信清单标记）** | ① **SAL-BREAK-02**：`erp_sale_order` 27 单 / `erp_sale_order_item` 仅 **4 行（2 单有明细）**，`total_amount=0` 的 **26 单**；`SaleOrderServiceImpl:272-279` 仍只是 `if (items != null)` 循环插入、**无「明细非空」校验** ⇒ ⬜ 属实。<br>② **SAL-BREAK-03**：主表 `warehouse_id/salesman_id/dept_id` **0/27**、`customer_name` **5/27**；`:1229-1250 findOrCreateOrder()` 仍只 set orderNo/orderDate/saleType/status/generationMethod + 三个 0 ⇒ ⬜ 属实。<br>③ **PUR-BREAK-03**：`erp_supplier` **0 行**、`erp_purchase_order.supplier_name` **0/11**，`PurchaseAnalysisReportServiceImpl:297,317` 仍写死 `LEFT JOIN erp_supplier` ⇒ 恒空，⬜ 属实。<br>④ **PUR-BREAK-04**：`erp_purchase_return.purchase_order_id` **0/1**，`PurchaseOrderMapper.xml` 的 `ret_agg` 仍 `GROUP BY r.purchase_order_id`（该列 NULL ⇒ 聚合永空）⇒ ⬜ 属实。<br>⑤ **MD-DUP-03**：两个 `CustomerGradePrice.java`（erp-partner / erp-stock）**都在**，且 erp-party 版仍被 `party/mapper`、`party/service(+Impl)`、**`core-agent/PartyCapabilityProvider`** 引用 ⇒ ⬜ 属实（照原计划直接删会碰 core-agent，属**删不掉**而非"忘记删"）。<br>⑥ **E-02**：离线产物 `permission-effectivity.json`（09-21 08:31）summary = db **994** / effective **824** / **ineffective 159** / groupNodes 11 ⇒ 僵尸码由 144 涨到 **159**，**修复未做**（但"待重测"口径已过时，清单是新的）。<br>⑦ **E-03**：`api_path` 非空 **734/994（73.9%）**、空 260 条 ⇒ 与清单逐字吻合，🔄 属实（**剩余 260 条的补全没做**）。<br>⑧ **E-01 批次 2（crm）**：`backend/crm` 全模块 grep `SaCheckPermission/PreAuthorize` **只命中 `CrmPermissions.java`（常量文件）**，`CustomerController` 注解 **0 处** ⇒ 未做，🔄 属实。<br>⑨ **MISC-DUP-01（打印 v1）**：v1 实体 `printing/entity/PrintTask\|PrintTemplate.java` 与 v2 `entity/v2/Sys*` 并存，v1 控制器 `PrintTaskController:22`、`PrintTemplateController:18` 仍在（`/api/v1/print/*`）⇒ ⬜ 属实。<br>⑩ **前端 feature flag 两套**：该条目编号是 **平台-CLEAN-02**（不是 FE-*），`composables/useFeatureFlag.ts` 与 `utils/featureFlags.ts` 都在且都被 `main.ts:9,35,54` 引用 ⇒ ⬜ 属实。<br>⑪ **STK-BREAK-04**：`erp_stock` 4 行、`product_name/product_code/warehouse_name/serial_no/batch_no/supplier_id/unit_price` **全部 0/4（100% NULL）**；无任何回填迁移，`V11.440.0` **只加 CHECK 约束**（那是 STK-BREAK-03，**不能当成本条已做**）；`StockServiceImpl.recordStockIn():115-126` 是唯一 set 快照的路径，主链路 `increase/decrease` 仍不写 ⇒ ⬜ 属实。 | **反向发现（清单标 ⬜ 但实际已做/大半已做）**：**ARCH-04 ✅ 已完成**（`AI_DEVELOPER_RULES.md` §9.2 已落地，见该条已更正）；**ARCH-03 🔄 部分完成**（§9.3 有 40 组分布与同步规则，**缺方法级一一对应表**，已更正）；**FE-CLEAN-02 🔄 部分完成**（存储文档已自我更正 + 本轮更正《系统参数开发文档》5 处，**剩 `feedback-components-usage.md` 三处错误路径**，已更正）。其余 8 项清单标记与实测一致，**未发现「标 ✅ 但代码没做」的反向情况** |
| 2026-09-21 | **E-02 批次 1 完成：`erp:expense:*` 11 处接线（两向验证 32/32）** | 逐条读控制器源码后定处置，**不是**照分类器的候选施工（候选已证明不可靠）。实际接线：<br>· `ExpensePaymentController` 5 处（`payment:{list,query,create,confirm,cancel}` ← `/page`、`/{id}`、`POST`、`/{id}/confirm`、`/{id}/cancel`，一码一端点，逐字对得上）；<br>· `ExpenseController` 3 处（`application:list`←`/application/page`、`application:query`←`/application/{id}`、`statistics:list`←`/statistics/page`）；<br>· `ExpenseApprovalController` 2 处（`approval:list`←`/records` 与 `/pending` —— 同一个码守两个"列表"端点，符合"一码=资源×动作"）；<br>· `ExpenseReimbursementController` 1 处（`reimbursement:query`←`/{id}`）。<br>这 4 个控制器此前**只有 `@SaCheckLogin`、没有任何权限注解**，故属真缺口。 | ✅ 新增 `tools/verify-authz-batch1.cjs` **32/32**：① 10 个码全部在库且**只被超管持有**；② 超管 11 条探针全非 403（200/400/404，证明注解没把接口锁死）；③ 非超管（租户 2）**11 条全部 403**（证明注解真生效）。写类端点用"必然被拒的入参"打，鉴权在参数校验之前 ⇒ 无副作用。effectivity：未生效 **110 → 100**。 |
| 2026-09-21 | ⏸ **批次 1 剩余 3 码待删（无对应端点）** | `erp:expense:application:approve`（真实端点是 `/api/erp/expense/{id}/approve`，路径上并没有 `application` 段）、`erp:expense:approval:query`（`/approval` 下只有 `/process`、`/records`、`/pending`，没有 `/{id}`）、`erp:expense:statistics:refresh`（`/statistics` 下无 refresh 端点）。三者都无对应端点 ⇒ 按 (A) 口径应**删码**。**未删**，归入"B 类删除批次"统一处理（删除也要一次性验一遍引用）。 | ⬜ 待删除批次 |
| 2026-09-21 | **E-02 批次 2 完成：crm 6 处接线（两向验证 18/18）** | crm 四个控制器（`ContractController`/`CustomerController`/`CustomerFollowUpController`/`CustomerOpportunityController`）此前**零权限注解**（只有登录校验），属真缺口。本批接线 6 条（逐条读源码后定的落点）：`crm:customer:list`←`GET /customer/page`、`crm:customer:update`←`PUT /customer/{id}`、`crm:customer:delete`←`DELETE /customer/{id}`、`crm:lead:view`←`GET /crm/followUp/lead/{leadId}`、`crm:opportunity:create`←`POST /crm/opportunity`、`crm:opportunity:view`←`GET /crm/opportunity/statistics`。 | ✅ `tools/verify-authz-batch2.cjs` **18/18**（6 码在库且只被超管持有 / 超管 6 条非 403 / 非超管 6 条全 403）。effectivity：未生效 **100 → 94**。 |
| 2026-09-21 | ⏸ **批次 2 剩余 6 码待删 + 1 个顺带发现** | **待删（无对应端点）**：`crm:contract:batchapprove`（`/contract` 下只有 `/batch`(DELETE) 与 `/{id}/approve`，没有 batch-approve）、`crm:contract:refresh`、`crm:create`（两段码、无资源，指代不明）、`crm:opportunity:detailrefresh`、`crm:opportunity:reset`、`crm:refresh` —— 均无对应端点，按 (A) 口径应删，归入 B 类删除批次。<br>**顺带发现**：`GET /crm/opportunity/statistics` 对超管返回 **500**（非本次改动引入，鉴权通过后业务报错）⇒ 该端点的统计查询有 bug，单独登记。 | ⬜ 待删除批次 / ⬜ 新 bug |

---

## 附：权限授权的双层模型（2026-09-21 用户口述，**这是本仓权限口径的权威描述**）

> 「我们所有的权限除了系统模块的是授权给超管和超管所在的系统租户的。其他模块就看超管和系统租户是否授权给某个租户，只要是授权给某个租户，租户所属的系统管理员就拥有了租户内的最高权限，租户内的部门管理员权限由租户所在系统管理员给与配置。当然系统模块的权限由系统超管授权给系统租户所属的用户和部门管理员。」

读法（落到本仓的表上）：

| 层 | 表 | 谁操作 | 含义 |
|---|---|---|---|
| 系统模块 | `sys_permission`（`system:*`、`platform:*` 等） + `sys_role_permission` | **系统超管** | 直接授给「系统租户」下的用户/部门管理员角色 |
| 业务模块 | 同上，但**必须先由平台把码授给该租户** | **超管/系统租户** | 「授权给某租户」= **启用该模块给该租户** |
| 租户内 | `sys_role_permission`（角色属租户：`sys_role.tenant_id`） | **该租户的系统管理员** | 被启用后，租户系统管理员在本租户内拥有**最高权限**（= 模块级全权） |
| 租户内下级 | 同上 | 租户系统管理员 | 部门管理员的权限由租户系统管理员自行配置 |

**⚠️ 对本轮 E-02 接线工作的直接结论**：
1. **接线（补 `@SaCheckPermission`）与授权（`sys_role_permission`）是两件事**。
   接线是把"这个接口受哪个码管"写进代码；**是否给某个租户启用该模块是平台侧的产品决定**，
   不能由脚本代做。所以本轮把码接到端点上、**不**顺手授权，是符合模型的做法。
2. 因此当前「已接线但只被超管持有」的码 = **"模块已具备管控能力，但尚未启用给任何租户"**，
   这是一个**合法且明确的中间态**，不是缺陷。
3. 但要注意：在启用之前，那些端点对**非超管的租户用户是 403**（而接线前是"登录即可访问"）。
   ⇒ **接线等于"把模块交给平台来开关"**。每批接线后都应登记"待启用的模块"，让平台决定何时/给谁开。

### 待启用的模块清单（接线已完成，尚未授权给任何租户）

| 批次 | 模块 | 码数 | 涉及端点 | 启用方式 |
|---|---|---|---|---|
| E-01（09-20/21 那 7 个模块） | budget / fixedasset / stock+product+md / invoice / payment / party / marketing | 485 | 709 | 见下方 SQL |
| E-02 批次 1 | `erp:expense:*`（费用申请/支付/审批/统计） | 11 | 11 | 同上 |
| E-02 批次 2 | `crm:*`（客户/商机/跟进） | 6 | 6 | 同上 |

**⚠️ 先分清「租户 1 = 系统租户」（2026-09-21 用户确认 + 实测核对）**：

```
sys_tenant: id=1 系统租户 SYSTEM（status=1）｜id=2 E2E验收租户2 E2E_T2｜id=0 是软删的注册残留
sys_role  : 超级管理员(1) / 系统管理员(2065122951570362369) / 部门管理员(2065122951620694018)
            —— 三者 tenant_id **都是 1** ⇒ 它们都是**系统租户（平台自身）**下的角色，
            不是某个普通租户的角色
```

所以「授权给谁」要分三类（本附录上一版把前两类混为一谈了，已更正）：

| 授什么 | 授给谁 | 本仓对象 |
|---|---|---|
| **系统模块**权限（`system:*` / `platform:*` …） | **系统租户**下的用户与部门管理员 | 上面那三个角色（均属租户 1） |
| **业务模块**权限（`erp:*` / `crm:*` / `sale:*` …），启用给某租户 | **该租户**的系统管理员 | 例：租户 2 → `E2E租户2管理员`(2099000000000009031) |
| 业务模块，**系统租户自己也要用** | 系统租户的系统管理员 | 现状即如此：租户 1 的系统管理员持有 `finance:month-closing:*`/`finance:period:*`/`finance:reconciliation:*`/`finance:other-income-doc:*` —— 即"平台作为一家租户在使用业务模块" |

**启用某个模块给某个租户 = 把该模块的码授给「该租户」的系统管理员角色**，SQL 形态（`role_id` 换成目标租户的系统管理员）：

```sql
-- 例：把费用模块启用给租户 1 的系统管理员（先确认角色 id 与码前缀）
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9592000 + row_number() OVER (ORDER BY p.id), 2065122951570362369, p.id, 1, now()
  FROM sys_permission p
 WHERE p.permission_code LIKE 'erp:expense:%'
   AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                    WHERE rp.role_id = 2065122951570362369 AND rp.permission_id = p.id);
```
（`id` 用新号段 `959xxxx`，避开已用号段；`NOT EXISTS` 保证幂等。）

---

## 附二：模块授权（entitlement）与权限（RBAC）的分层 —— 业界做法 vs 本仓现状（2026-09-21）

**问题**：业务模块「启用给某个租户」到底该由谁、在哪一层做？

**业界共识（调研结论）**：把「权益 entitlement」与「授权 RBAC」当成**两个独立层**，各自回答不同问题、由不同角色拥有：

| 层 | 回答的问题 | 谁维护 | 变更频率 |
|---|---|---|---|
| AuthN | 你是谁 | IdP | — |
| 租户隔离 | 这份数据属于谁 | 平台（安全底线，永不变） | — |
| **授权 RBAC** | *这个用户*能不能做这件事 | 租户内管理员调角色 | 高 |
| **权益 Entitlement** | *这个租户*有没有买/被启用这个模块 | 平台侧销售/运营/超管 | 低（随合同） |

三条关键实践：
1. **两道门都要在服务端把住**，判定顺序**先授权后权益**（权益是"组织"属性，用户连操作权都没有时不必报"套餐不包含"）；报错语义要分开：「你没这个权限」vs「你的租户没启用这个模块，请升级/申请开通」。
2. **导航按权益裁剪（零可见性原则）**：没买的模块不要出现在菜单里，避免"点了才 403"。
3. **不要把权益抄成权限行**（反模式）：若"某租户启用了财务模块"靠往 `sys_role_permission` 里塞一笔笔码来实现，那么每次销售改套餐都要改权限数据，且无法表达"启用了但某人还没被授权"这一合法中间态。套餐/配额/用量（metering）应由权益侧自己承载，权限侧只管"人能不能做"。

**本仓现状（实测，比预想的好得多 —— 这套东西你们已经有了）**：

| 已有 | 证据 |
|---|---|
| 权益数据模型 | `sys_module`(6 个模块) / `sys_tenant_module`(租户×模块，含 `purchase_type`，租户 1、2 各有 5 个模块) / `sys_tenant_package`(基础·专业·企业版 + 价格 + max_users) / `sys_tenant_quota`(max_* 与 **used_*** 用量列) |
| 平台端授权入口 | `TenantModuleController` + `TenantModuleService`；前端 `views/admin/tenant/module-auth/index.vue` + `api/tenantModule.ts` |
| 前端按权益裁剪导航 | `stores/user.ts` 的 `hasValidModule()` / `validModuleCodes` |

**唯一的缺口（也正是本轮暴露出来的）**：**后端不校验模块权益** —— `TenantModuleService.hasModuleAccess(tenantId, moduleCode)` **零调用方**。
⇒ 权益当前只决定"看不看得见"，不决定"调不调得通"。于是出现本轮这种自相矛盾的状态：
**租户 1、2 在 `sys_tenant_module` 里都启用了 `crm`，但 `crm:*` 码只授给了超级管理员** ⇒ 前端给租户 2 的管理员显示 CRM 菜单，点进去后端 403。

**结论 / 建议（对应上面三条实践）**：
1. **修缺口的正确位置是"加一道后端权益门"**（把已存在但没人调的 `hasModuleAccess()` 接进请求链：`@SaCheckPermission` 之后、业务之前），而**不是**把码逐个授给各租户的角色。
2. 之前附录里「启用模块 = 把码授给该租户的系统管理员角色」这条**是权宜之计、不是模型做法**：它把权益抄成了权限行，违反第 3 条实践。保留为"临时让某租户能用起来"的运维手段可以，但**不要**当成长期机制。
3. 因此「接线（补注解）」这一步**与权益无关、不需要等平台决定**：码与端点的绑定是代码事实；"哪个租户能调"由权益门决定。

---

## 附三：模块口径（2026-09-21 用户口述 —— **这是权威定义**）

> 「销售是销售（和采购一样都是企业货流物流的一个销售出库、收款的方向）；交易是交易（交易更多是针对网上订货、网络销售一类的）；仓储和资料模块分开；crm 是客服模块；dms 是配送；hr 是人力资源；系统（系统平台级的）；设置（租户级的）；分析单独一个；工作台只是一个页面，页面显示的字段内容和登录用户的其他权限有关（比如普通员工没有利润权限就看不到当日毛利润、没有汇总权限就看不到当天营业额），不应该工作台单独是一个模块。」

### 定下来的模块清单（14 个，其中 1 个平台级不对租户开关）

| # | 模块码（建议） | 名称 | 覆盖的一级菜单 | 覆盖的权限码域 | 层级 |
|---|---|---|---|---|---|
| 1 | `sale` | 销售 | 销售 | `sale` | 租户 |
| 2 | `purchase` | 采购 | 采购 | `purchase` | 租户 |
| 3 | `warehouse` | **仓储** | 仓储 | `stock` `wms` | 租户 |
| 4 | `master-data` | **资料**（与仓储分开） | 资料（商品管理/往来单位/仓库管理/配送管理/财务账户/支付管理） | `product` `md` `party` | 租户 |
| 5 | `trade` | **交易**（网上订货/网络销售） | 交易（商城订单/支付结算/商城管理/商城设置/外部平台） | `mall`(`b2b`) 及后续 `trade` | 租户 |
| 6 | `crm` | **客服**（原叫"客户关系"，需改名） | CRM（外勤拜访/客户/线索/商机/报价/合同/发票/CRM报表） | `crm` | 租户 |
| 7 | `dms` | 配送 | 配送 | `dms` | 租户 |
| 8 | `hr` | 人力资源 | 人力资源 | `hr` | 租户 |
| 9 | `finance` | 财务 | 财务 | `finance` `payment` `budget` `fixed-asset` `invoice`(?) | 租户 |
| 10 | `marketing` | 营销 | 营销 | `marketing` | 租户 |
| 11 | `analytics` | **分析**（独立模块） | 分析（综合单据/采销分析/仓配分析/提成分析/财务分析/营销分析） | 无专属码（只读数据，权益门按模块本身判定） | 租户 |
| 12 | `settings` | 设置 | 设置（系统配置/数据录入/账套操作/财务设置/打印管理/工作流/审批） | `set` `workflow` `print` | **租户级** |
| 13 | `system` | 系统 | 系统（租户管理/模块管理/系统监控/数据管理/开发工具/平台设置/系统管理） | `system` `platform` `tenant` `permission*` `role` `user` `department` `position` `log` `datasource` | **平台级（不对租户开关）** |
| — | ~~`dashboard`~~ | ~~工作台~~ | 工作台 | — | **不是模块**，见下 |

**与现状的差异（要改的）**：
1. `sys_module` 现有 6 条（sale/purchase/warehouse/finance/crm/marketing）⇒ 需**补 7 条**：`master-data`(资料) / `trade`(交易) / `dms`(配送) / `hr`(人力资源) / `analytics`(分析) / `settings`(设置) / `system`(系统)。
2. `crm` 的名称由「客户关系」改为「**客服**」。
3. 存量 `warehouse` 的语义要**收窄到仓储作业**（`stock`+`wms`），把 `md`/`product`/`party` 划给新的 `资料` 模块 —— 这会改变租户已启用的模块覆盖面，属**权限面变更**，需一次性迁移并复核。

### ⚠️ 工作台不是模块，它的"按权限显示字段"是**字段级/数据级权限**，另一个专项

用户的要求（普通员工无利润权限 ⇒ 看不到当日毛利润；无汇总权限 ⇒ 看不到营业额）**不属于模块权益**，而是**同一页面上按权限裁剪字段/指标**。本仓已有的相关件：
`sys_permission` 的 `data-permission` 域（8 条）、前端页面配置（`user_page_config:*` 列显隐）、以及 `FieldPermission*`（此前已在死代码清理中评估过）。
⇒ 单独立项：**工作台指标级权限**（哪些指标对哪些角色可见），不要塞进模块/权益体系。

### 附三之补：模块划分与成熟系统的对照核验（2026-09-21 调研）

| 用户口径 | 成熟系统是否如此 | 依据 |
|---|---|---|
| 销售（出库/收款方向）与采购对称 | ✅ 是 | SAP：**SD**（Sales & Distribution：订单/发货/开票）与 **MM**（Materials Management）对称，同属 Logistics |
| 「交易」（网上订货/网络销售）独立于销售 | ✅ 是，且这正是两派的分野 | **Odoo 把 eCommerce 当一等公民 App**（与 Sales/Inventory/Website 原生打通）；**SAP 里 ecommerce 只评"basic"**、靠外围系统。本仓是"租户商城 + 平台交易"，更接近 Odoo 式 ⇒ 独立成模块成立 |
| 仓储与资料分开 | ✅ 是，共识 | SAP：物料主数据/分类/批次在 **LO（General Logistics）** 或专门的 **Master Data Management**，与 **EWM/WM** 仓库作业分离；Odoo：Products（主数据）与 Inventory（作业）是两个 App |
| dms 配送独立 | ✅ 是 | SAP **TM / LE**；Odoo Delivery |
| hr 独立 | ✅ 是 | SAP HCM/SuccessFactors；Odoo HR |
| 系统（平台级）与设置（租户级）分开 | ✅ 是，且是硬要求 | 平台管理（租户/模块/监控）与租户配置**必须分层**，否则租户管理员能触及平台级配置。本仓码数分布也印证：`system` 104 条 vs `set` 10 条 |
| 分析独立 | ✅ 是 | SAP **BW/Analytics**；Odoo Reporting |
| **工作台不是模块**，字段随用户权限裁剪 | ✅ **完全正确，且是关键洞察** | SAP Fiori Launchpad 的 tile 按角色裁剪；Odoo 仪表盘按权限隐藏指标。工作台是"门户页面"，其可见性属**字段/指标级权限** |
| **「crm 是客服」** | ⚠️ **需澄清** | 成熟系统里 **CRM（线索/商机/报价 → 销售过程）** 与 **Service/Helpdesk（工单/售后 → 客服）** 是**两个不同 App**（Odoo 有 CRM + Helpdesk；SAP 有 CRM/CS）。本仓 CRM 菜单现为「外勤拜访/客户/线索/商机/报价/合同/发票/CRM报表」⇒ **语义上是 CRM（销售过程），不是客服**。若真要"客服"，内容对不上（缺工单/售后），应另立模块；若只是"给客服部门用"，那是**使用部门**表述而非模块语义 |

**结论**：用户口径 8/9 与成熟系统一致，且"工作台不算模块""仓储与资料分开""交易独立"三条比本仓现状更贴近业界。**唯一要澄清的是 `crm` 到底是 CRM（销售过程）还是客服（Service/Helpdesk）** —— 这决定它是改名还是新增一个模块。

### 附三之补二：CRM = 「客户服务」一个模块、内部两个阶段（2026-09-21 用户口径 + 实测）

**用户口径**：「crm 是一个连续的维护客户的过程，前期是销售，同时要收集线索、维护客户、客情服务；成交以后肯定是盯着工单和售后了。CRM 内部可能分两个阶段，但都属于客户服务管理。」

**业界核验：成立**。Salesforce = **Sales Cloud（售前）+ Service Cloud（售后）**同属客户域；Zoho = CRM + Desk；Odoo = CRM + Helpdesk。**模块层面一个「客户域」，功能层面两个集**，是主流做法。

**本仓实测（差异很大）**：

| 阶段 | 现状 |
|---|---|
| 售前 | **齐**：`backend/crm` 10 个控制器（`CustomerLead` 线索 / `CustomerOpportunity` 商机 / `Quotation`+`QuotationTemplate` 报价 / `Contract` 合同 / `Customer` 客户 / `CustomerFollowUp` 跟进 / `CustomerPool` 客户池 / `Visit` 外勤拜访 / `MarketingCampaign`），权限码 `crm:*` 57 条，菜单 8 项 |
| **售后** | **基本空缺**：全库**没有任何** ticket / service / aftersale / complaint **表**；仅 `DatabaseInitializer` 里有一个 `customer_ticket` **字段**（VARCHAR(20)/(200)，疑似"客户工单号"的登记位），**无实体、无接口、无菜单** |

**结论与建议**：
1. 模块定义：`crm` 名称定为「**客户服务**」（不是"客户关系"，也不是单独的"客服"）。
2. 模块内部按阶段拆两个码子域：`crm:*`（售前，已存在 57 条）与 **`crm:service:*`（售后，待建）**；菜单侧对应两个分组（售前 8 项目前齐全，售后需新增）。
3. ⚠️ **「CRM 售后阶段（工单/售后）」是一条真正的功能缺口**，不是接线能解决的：需要新建工单实体 + 表 + 接口 + 菜单 + 权限码。量级不小，应作为独立功能项排期，不要混进 E-02 接线批次。
