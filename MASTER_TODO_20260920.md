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
| 平台/系统（core-*、租户、用户） | 3 | 4 | 12 | 15 | 6 | 🔄 **`SysConfigService` 硬编码租户专项已闭环**；**平台-BREAK-02 ✅ 闭环**（单测 4/4 + 反向验证）；**平台-BREAK-01 ✅ 已实机验证**（fail-open → fail-closed，回归 37 项全绿）；**平台-MODULE-01 entitlement 门 ✅ 已实现**（模块开关从"纯展示"变成真拦截）；剩 `AuthServiceImpl` 死代码；**模块开通/停用写入口 ✅ 已闭环（2026-09-22，`verify-module-assign` 31/31）** |
| 财务域（erp-finance） | 5 | 2 | 3 | 4 | 2 | ⬜ |
| 库存仓储（erp-stock + wms） | 6 | 4 | 8 | 1 | 2 | 🔄 STK-BREAK-01/03 已闭环；-02 剩 2 处、-04/05 待做；**E-01 wms 批次 ✅**（20 控制器/162 端点补注解 + 86 新码，`verify-module-authz wms` 22/22）；STK-AUTHZ-01 ✅ 闭环 |
| 销售域（erp-sales） | 4 | 3 | 2 | 3 | 2 | ⬜ |
| 采购域（erp-purchase） | 4 | 3 | 2 | 3 | 1 | 🔄 PUR-BREAK-01 已闭环；PUR-BREAK-03/04 待做 |
| **权限专项（E-01/E-04/E-02）** | — | — | — | **E-01 已收口：裸控制器 197 → 27、裸端点 1765 → 126**，剩 27 个全部是**有据可依的有意排除** | — | 🔄 **E-02 ✅ 闭环**；**E-07 ✅ 已修**；**E-01 ✅ 本轮完成 14 个批次**（见下方「E-01 收口总表」）；**E-08 记分牌**已从 202 控制器 / 1794 端点收敛到 **27 / 126** |
| 主数据（erp-partner / md-*） | 3 | 3 | 6 | 1 | 1 | ⬜ |
| DMS 配送 | 3 | 1 | 2 | 2 | 1 | ⬜ |
| 商城/营销（erp-mall / marketing） | 3 | 2 | 4 | 2 | 1 | ⬜ |
| HR | 1 | 1 | 2 | 1 | 1 | ⬜ |
| CRM | 2 | 1 | 2 | 4 | 1 | 🔄 **CRM-BREAK-02 硬编码租户/异常口径已修**（crm 域 7 处同类一次收口，实机 4/4）；**CRM-BREAK-03 新立（软删后重号 400，需拍板修法）**；该类 40 端点鉴权注解仍待做 |
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
- **状态**：✅ **代码已改并实机验证（2026-09-21）**，回归 37 项全绿
  （`verify-tenant-hardening` 14/14 + `verify-permission-changes` 23/23）。
  **但拒绝分支（已登录却无租户）当前无法用 API 触达**，属纵深防御，详见末段"验证边界"。
  做法比原"修法"更省：**没有**给 `sys_user`
  的登录查询加 `@InterceptorIgnore`，因为登录查询发生在 `StpUtil.login()` **之前**，天然属于
  「未登录」分支，本来就会跳过。改动只有三处：
  1. `AiReadyTenantLineInnerInterceptor#shouldSkip()` 拆成三支（顺序即优先级）：
     ① 超管豁免 → 跳过；② 有会话租户 → 注入；③ 无租户上下文 → **只有未登录才跳过**，
     「已登录却没租户」不再跳过 ⇒ 父类注入 `tenant_id = null`（PG 中恒 UNKNOWN）＝ **fail-closed**。
  2. `MallAuthServiceImpl#login` 在 `StpUtil.login()` 之后**立刻**写 `session.tenantId`
     （必须早于同方法内的 `buildIdentities`，它要查 `biz_party` / `shop_user_party_link`）。
  3. `MallAuthServiceImpl#refreshToken` 同上补写 —— `StpUtil.login()` 会**重建会话**，
     不补写则「刷新过 token 的会话」又退化成无租户上下文。
- **全仓 `StpUtil.login()` 只有 6 处**，已逐一核对：主站 `SysUserServiceImpl:115`、
  `AuthController:227`（走前者）、PDA `PdaAuthController:57`、打印端 `ClientAuthController:83`、
  商城 `MallAuthServiceImpl:67`（本轮补）、`AuthServiceImpl:103`（**死代码**，见下）。前四处原本已写租户。
- **新发现（未处理，需裁决）**：`core/base/.../service/impl/AuthServiceImpl.java` 与接口
  `cn.aiedge.base.service.AuthService` **全仓零引用/零注入**（实测 grep），是死代码，且它同样漏写
  `session.tenantId`。建议随「死代码清理」批次整对删除（属 平台-CLEAN 系列）。
- **新发现（未处理，需裁决）**：`MallAuthServiceImpl#register` 造 `ShopUser` 时**没有** tenantId，
  而注册发生在未登录态 ⇒ `insertFill` 不填、拦截器也不注入；实测 `shop_user.tenant_id`
  是 **NOT NULL 且无默认值** ⇒ **商城注册当前必然插入失败**。租户只能来自请求头 `X-Tenant-Id`
  （`MallGuestAccess#currentShopTenantId` 已有现成口径），但 `frontend/apps/mobile-mall` **不发这个头**
  （实测 grep），属前后端一起定的产品口径，故本轮不动手。
- **代价（须知悉）**：漏写租户的登录入口今后表现为「登录成功但列表全空」，而不是「看到别人的数据」。
  反过来说，**任何新增登录入口都必须在 `StpUtil.login()` 后立刻写 `session.tenantId`**，
  已写进拦截器类注释。
- **⚠️ 验证边界（不要把它读成"已验证完毕"）**：本轮实机只能验证**两条放行分支**
  （未登录 → 跳过；有租户 → 注入），因为**拒绝分支已无法用 API 触达** ——
  全仓 `StpUtil.login()` 仅 6 处，除死代码 `AuthServiceImpl` 外 5 处全部写租户，
  即"已登录却无租户"的会话在现有数据下**造不出来**（商城 C 端 `shop_user` 表 0 行）。
  该分支的收益是**纵深防御**：将来若有第 7 个登录入口漏写租户，
  失败方向是"看不到数据"而非"全租户可见可写"。**要真正两向验证，需先有可造的租户缺失会话**
  （例如新增登录入口时，在 e2e 脚本里刻意注入一个无租户 token）。

#### 平台-BREAK-02 [P0] `insertFill` 无条件覆盖实体 tenantId ⇒ 跨租户写全部落错租户

- **证据**：`MyBatisPlusConfig.java:253-263` 用 `metaObject.setValue("tenantId", tenantId)` 而**非** `strictInsertFill`，不判断字段是否已有值。
- **项目自己已踩过**：`SysTenantMenuMapper.java:24-31` 注释记着「给租户 A 授权写到会话租户头上（2026-09-18 实踩）」，`SysTenantMenuMapper.xml:36-40` 因此改用标量 `#{tenantId}` 绕过。
- **数据佐证**：`sys_role_permission` 中 `role_tenant=1` 而关联行 `tenant_id=0` 的有 **16 行**、`=1` 的 518 行 —— 同一角色权限关联行租户标记不一致。
- **修法**：改为「仅当 `getValue("tenantId") == null` 时填充」+ 单测覆盖「显式指定 ≠ 会话租户」。
- **状态**：✅ **已闭环（2026-09-21）** —— `MyBatisPlusConfig.java:256-257` 的 `hasGetter` +
  `getValue(...) == null` 双重条件保留，并补上单测
  `core/base/core-base/src/test/java/cn/aiedge/base/config/TenantInsertFillTest.java`（4 例：
  显式租户保留 / 按会话租户填充 / 无上下文保持 null / 不影响其它公共字段）。
  **反向验证**：把守卫改回无条件覆盖后重跑，2 例失败且报
  `expected: <7> but was: <9>`（正是"给租户 A 建数据却落到会话租户头上"），恢复后 4/4 通过。
  全仓 grep 已确认无实体自初始化 `tenantId = 数字`，故无"显式 0 被跳过填充"风险。

#### 平台-BREAK-03 [P1] 51 行业务数据 `tenant_id IS NULL`，对任何租户都不可见

- **证据**（全库动态扫 494 张含 `tenant_id` 的表）：`budget_item` 21 / `annual_budget` 15 / `budget_execution_log` 7 / `finance_voucher_item` 6 / `erp_group_buy_activity` 1 / `finance_ledger` 1 = **51 行**。
- **成因**：`setTenantId(null)` 共 12 处（`HrAttendanceServiceImpl.java:297`、`MallKeywordServiceImpl.java:81`、`MallNoticeServiceImpl.java:83`、`CouponTemplateController.java:77` 等），依赖 `insertFill` 兜底，而**无上下文时 `insertFill` 是 no-op**。
- **修法**：补 `tenant_id NOT NULL DEFAULT 0` + 一次性回填（需确认归属）+ 修 12 处调用点。
- **同类但方向相反的实证（2026-09-21 补，见 平台-BREAK-01）**：`shop_user.tenant_id` 是
  **NOT NULL 且无默认值**，而商城注册链同样不填 ⇒ 不是"落 NULL 行"，而是**注册直接插入失败**。
  即同一根因（注册/建单链路没有租户上下文）在两种列定义下表现为两种故障：
  **可空列 → 造出谁都看不见的行；NOT NULL 列 → 功能整条不可用**。修的时候要一起看。
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
- **⚠️ 实测标尺（2026-09-21，以 E-08 为准）**：**202 个控制器 / 1794 处端点**整类零权限注解
  （E-01 原先按 2170 端点估的口径只算到"类内一个注解都没有"的那部分，两者不矛盾，
  但**扫描脚本可复跑**，以后报告进度请用它的数字）。按模块：
  `wms 23/172`、`erp-mall 13/113`、`crm 7/144`、`erp-pricing 6/50`、`erp-marketing 1/7`，
  其余散落在 core-* 与 DMS/配送。
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
- **状态**：✅ **已完成（2026-09-21）** —— 批次 1~5 全部施工完毕，`934 = 902 生效 + 21 未生效 + 11 分组节点`，
  僵尸码 **159(误判) → 110 → 21**，且**剩下的 21 条逐条都有裁定记录**（20 条"功能未建"+ 1 条待拍板），
  不再有"未定性的僵尸"。全过程与三个工具盲点见下方"✅ 闭环"块。
  施工前的定性过程（保留，因为里面的两个工具误判教训还要复用）：
  <details><summary>展开：定性过程与被推翻的数字</summary>
- （原）🔄 已重测 + 已逐条定性，剩"接线 or 删码"的施工（2026-09-21）
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
  </details>

  - **✅ 闭环（2026-09-21 续做：批次 3/4/5 全部施工完毕）** —— 僵尸码
    **159(误判) → 110 → 100(批次1) → 94(批次2) → 75(批次3) → 68(修工具) → 70(修工具) → 36(批次4) → 21(批次5)**，
    库中 `sys_permission` **994 → 934**（删 60 条），`GET /api/permission/effectivity` 现报
    **`934 = 902 生效 + 21 未生效 + 11 分组节点`**。剩的 21 条**全部已逐条裁定**，不再是"未定性的僵尸"。
    - **⚠️ 过程中又挖出三个同类工具盲点（都先修工具再动手，这是本仓的硬规矩）**：
      ① **权限码写在常量里**：`@SaCheckPermission(PERM_VIEW)` + `private static final String PERM_VIEW = "set:system-task:view"`，
         扫描器只认字面量 ⇒ **7 个码被误判为僵尸**（`system:dev:scheduler:*` 6 个 + `set:system-task:view`）。
         现按同文件常量表解析，解析不出来的**显式列出**（不再静默漏判）。**75 → 68**。
      ② **测试文件被当成消费方**：`pc-admin/src/__tests__/permission.test.ts` 里的 `'user:create'` 只是断言假数据，
         却让这些码被判「生效」⇒ 掩盖真僵尸。现测试路径不计消费方，单独列出。**68 → 70**（`user:create/delete` 现形）。
      ③ **显式调用式校验**：crm 子模块只依赖 core-base、拿不到 core-api 的切面，用
         `CrmPermissions.require("crm:contract:edit")` 做校验（见 `crm/common/CrmPermissions.java` 类注释），
         注解式扫描看不见。现用保守模式 `\w*Permissions?\.require("码")` 收（**不用** `\brequire\("` 这种宽模式，
         宁可漏认也不要把无关调用当消费方）。
    - **批次 3（product/md 19 条，全部删除）**：核到 `product:*`（**无 `erp:` 前缀**）是 E-04 造出的**平行命名空间** ——
      子实体控制器（attributes/category/brand/grade/units…）真在用，但顶层 8 条 CRUD 与
      `product:barcodes:{list,export}`、`product:shield:{list,create}` 零消费方；真正把门的是
      **`erp:product:`\* 8 条**（`ProductController`）。`md:product-price:*` 7 条同理，端点已被
      `erp:product:list` / `erp:product:price-batch` 守着。迁移 `V11.451.0`。
    - **批次 4（核心域 32 条删除 + 5 控制器 20 端点接线）**：
      · **删**：裸域重影 `permission:* / role:* / user:* / tenant:*`（16 条，正主是 `system:*` 那套）
        + 同族多余动作码 16 条（`erp:expense:{application:approve,statistics:refresh,approval:query}`、
        `finance:receivable:{list,query,export,edit}`、`finance:balance:view`、`doc:{date:edit,unapprove}`、crm 6 条）。迁移 `V11.452.0`。
      · **接线（全部用库里已有的码，不造新码）**：`PartnerLedgerController` 3 读 ← `finance:partner-balance:view`；
        `ExpenseAnalyticsController` 3 读 ← `erp:expense:statistics:list`；`ExpenseApprovalController#/process`
        ← **`erp:expense:approval:process`**（前端审批页按钮一直在查这个码，后端却没挂 ⇒ "前端藏了按钮、后端敞着门"）；
        `PurchasePriceTrackController` 6 端点 ← `purchase:price:edit`；`SalesPriceTrackController` 7 端点 ← `sale:price:edit`。
        ⚠️ 价格跟踪的**读**也用写码：库里没有读码，而"最近采购价"是成本敏感数据，
        只保护写等于让未授权用户照样翻到全量进价 —— 权衡后读写同码，待该页单独设计码族时替换（已登记）。
      · **有意保留 1 条**：`finance:other-income-doc:approve`（端点只有 `confirm`，用的是 `...:update`），
        但它**被「部门管理员」「系统管理员」真实持有**（种子有意授权）⇒ 不盲删，
        究竟该"删码"还是"补一个审批环节"属产品决策，**留待拍板**，仍显示为"未生效（标灰）"。
    - **批次 5（9 条重影删除 + `SyncConfigController` 9 端点接线）**：
      · **删**：`data-permission:*` 8 条（整族重影，正主是 `system:data-scope:*` —— `SysDataScopeController`
        + 前端 `RoleDataScopeTab.vue` 真在用）+ `tenant:config`（正主 `system:tenant:query`）。迁移 `V11.453.0`。
      · **接线**：`SyncConfigController`（`/api/v1/sync-config`，9 端点）此前只有 `@SaCheckLogin`，
        而 `system:dataimport:{list,create,update,delete,test,sync}` **6 个码早在库里躺着**，
        且 `sys_permission.api_path` 回填的正是 `/api/v1/sync-config*` ⇒ 属"码在等接口"，两侧对齐即可。
      · **剩下的 21 条 = 20 条「已定义未实现」+ 1 条待拍板**，**不删**：它们各自是一个**尚未实现的独立能力**
        （`crm:contract:{download,renewapply}`、`crm:opportunity:search`、`doc:{reverse,void,draft:view-others}`、
        `party:merge`、`print:draft`、`sale:{discount:edit,settle:force}`、`payment:account:select`、
        `receipt:account:select`、`finance:receivable:{analysis,payment}`、`system:{permission,role}:export`、
        `product:{cost,purchase-price,retail-price,wholesale-price}:view`），
        不是别人的重影。删掉等于销毁路线图；保留则在矩阵里显示「未生效（标灰）」——**那正是它们此刻的真实状态**。
        ⚠️ 其中 `product:cost:view` / `product:*price:view` 四条是**字段级可见性**（"没有利润权限就看不到毛利"），
        与工作台字段级权限专项是同一件事，**应当由那个专项统一设计**，别在这里随手删。
    - **🔴 断掉"删码被重启撤销"的回路（独立缺陷，本轮实测发现）**：首批删完重启后，**24 条码全部复活**。
      根因在 `PermissionInitializationConfig#savePermissions`：它按 `permission_code` 查一次、
      查到就更新查不到就插入，而 `SysPermission.deleted` 带 `@TableLogic` ⇒ 条件构造器被自动追加
      `deleted = 0`，**看不见墓碑** ⇒ 「被有意删掉的码」被当成「从未种过的码」，**每重启一次插一行新的**；
      随后 `assignAllPermissionsToSuperAdmin()` 又把新活行全量授给超管，把清理整体撤销。
      后果两层：① 管理端在权限矩阵删掉的权限，重启就回来（删除形同虚设）；② 库里同码两行（墓碑 + 活行）。
      **修法**：`SysPermissionMapper.countDeletedByCode`（`@Select` 直写 SQL 绕开 @TableLogic）+ 初始化器
      查到墓碑即**跳过不复活**；同时把已删的 24 条从初始化器的静态清单里摘掉（否则清单还在"想要"它们）。
      **实证**：临时软删 `log:audit:stats` 后重启，日志出现"已被有意删除（存在墓碑行），初始化器跳过不复活"，
      活行 0 / 墓碑 1、全库同码双行 **0 组**；测试后已原样恢复。
    - **🔴 顺带发现：鉴权门禁自批次 1 起就是红的（已修）**。`AuthzAnnotationCoverageTest` 的棘轮规则要求
      「补了注解就必须从 `known-unauthorized-controllers.txt` 删掉对应行」，而批次 1/2 接线后基线**没同步收缩**，
      实测有 **7 行过期**（`CustomerController`/`CustomerFollowUpController`/`CustomerOpportunityController`
      + `erp.expense.controller` 四个）。也就是说 `baselineMustNotBeStale` 这条**从批次 1 起就一直在报红**，
      只是当时没人跑这个测试。现已删掉 7 行并**真跑门禁**：`AuthzAnnotationCoverageTest`(3) +
      `PointcutTargetExistenceTest`(1) **4/4 通过**。教训：接线批次必须把「跑门禁测试」写进收尾清单，
      否则棘轮会腐烂成一份"永远豁免"的名册。
    - **验证**：新增 `tools/verify-authz-batch45.cjs` **53/53 全绿**（三向 + 三个缺陷钉子）；
      批次 1 `32/32`、批次 2 `18/18` 复跑无回归；`GET /api/permission/effectivity` 与 DB 对账一致
      （`902 + 21 + 11 = 934`）。新增 `tools/scan-unguarded-controllers.py`（E-01 的记分牌）、
      `tools/refs-of-codes.py`（删码前全仓引用核对）。
    - **⚠️ 新的可量化结论（给 E-01 用）**：裸端点扫描实测 **202 个控制器 / 1794 处端点零权限注解**
      （全局拦截器 `SaTokenConfig` 只做 `StpUtil.checkLogin()`，**不做权限校验**）。
      实测用例：租户 2 的 HR 用户（无任何财务/采购/销售码）打
      `/erp/finance/expense-approval/pending`、`/erp/finance/expense-doc/page`、
      `/erp/finance/analytics/partner-balance/page`、`/purchase/price-track/page`、
      `/sales/price-track/page` **全部 200**。⇒ E-01 的真实规模远大于原估，
      且**"整类零注解的控制器"**是最危险的一档（不是"漏了某个端点"）。

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

#### E-07 [P1] 权限拒绝被吞成 HTTP 500「系统异常，请稍后重试」（本轮实测发现并已修）

- **现象**：非超管打 `GET /api/user-permission/user/1/permissions`（`PermissionController`，走
  `cn.aiedge.permission.annotation.RequirePermission` + `PermissionAspect`）拿到的是
  **500 `{"code":500,"message":"系统异常，请稍后重试"}`**，而超管同一路径 200。
  即**权限不足与服务器故障对调用方完全无法区分**：前端会把它报成"系统故障"，
  运维会被误导去查服务，而两向验证里「非超管必须 403」这条断言也会被误判为失败。
- **根因（精确）**：`PermissionDeniedException` 原来直接 `extends RuntimeException`，
  而 core-base 的 `GlobalExceptionHandler` 有一个 `@ExceptionHandler(RuntimeException.class)` 兜底
  （返回 500「系统异常，请稍后重试」）。`@RestControllerAdvice` **两个都没写 `@Order`**，
  按注册顺序兜底那个先命中 ⇒ 专治权限拒绝的 `PermissionExceptionHandler`（返回 403）**根本没被调用**。
  是"注册顺序碰运气"埋的雷，不是处理器没写。
- **修法（双保险，三层都改）**：
  ① `PermissionDeniedException extends BusinessException`（code=**403**）——
  即便仍被 core-base 的兜底 advice 先命中，也会走 `handleBusinessException` 按 code 映射成 HTTP 403；
  ② `PermissionExceptionHandler` 加 `@Order(Ordered.HIGHEST_PRECEDENCE)`，确定性地先命中，
  并/把响应体从自造的三字段 `{code,message,success}` 改成全局统一的 `Result.fail(403, msg)`；
  ③ 登录类走 Sa-Token 的 `NotPermissionException`（`GlobalExceptionHandler` 里已有 403 映射），不受影响。
- **验证**：`tools/verify-authz-batch45.cjs` §④ 钉住 —— 非超管 `→ 403`（不是 500）、超管 `非 403`；4/4 通过。
- **状态**：✅ **已修（2026-09-21）**

#### E-08 [P0] 裸端点全量盘点：202 控制器 / 1794 端点零权限注解（本轮实测，为 E-01 立标尺）

- **口径**：类内 `(Get|Post|Put|Delete|Patch)Mapping` 数 > 0，且
  `@SaCheckPermission` / `@RequiresPermission` / `@RequirePermission` 数 == 0。
  脚本 `tools/scan-unguarded-controllers.py`（输出同时区分「仅 `@SaCheckLogin`」与「连类级校验都没有」）。
- **实测**：**202 个控制器 / 1794 处端点**。全局拦截器只有
  `SaTokenConfig` 里 `new SaInterceptor(handle -> StpUtil.checkLogin())` —— **只校验登录，不校验权限**。
- **实证用例**（租户 2 的 HR 用户 `e2e_hr_t2`，不持有任何财务/采购/销售码）：
  `/erp/finance/expense-approval/pending`、`/erp/finance/expense-doc/page`、
  `/erp/finance/analytics/partner-balance/page`、`/purchase/price-track/page`、`/sales/price-track/page`
  **全部 200**。其中前两类是**金额类**端点，属越权后果最重的一档。
- **⚠️ 与 E-01 的关系**：E-01 原先按"控制器数/端点数"估的是 2170 端点；
  本次扫描给出**可复跑的下限 1794**（只算"整类零注解"，不含"类内有注解但漏了某些方法"的那部分）。
  **"整类零注解"是最危险的一档** —— 它的存在说明权限不是"漏了个别端点"，而是**整个功能面没设防**。
- **⚠️ 也有豁免项**（扫出来不等于都要补）：公开接口（商城 `/api/v1/mall/**`）、
  只读本租户数据且租户 id 取自会话的（`SetAppCenterController`，类注释里有裁定）、
  纯转发/健康检查。故本脚本**只负责列出来，不负责定罪**。
- **修法**：并入 E-01 的分批（每批仍需先确认码在库 —— 本仓铁律）。
- **状态**：⬜ 待 E-01 分批施工（记分牌已就位）

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

#### 平台-MODULE-01 [P0] 模块目录（entitlement 的"名册"）只有 6 条，且与权限码没有对应关系

- **背景**：本仓授权是两层 —— ① 模块授权（平台方决定"某租户有没有这个模块"）；
  ② 权限（租户内管理员决定"某角色能不能做某件事"）。业界同构（Salesforce/Zoho/Odoo = 订阅开关 + 租户内角色）。
- **实测缺口（两处，都查过）**：
  ① `sys_module` 只有 **6 条**（sale/purchase/warehouse/finance/crm/marketing），
     而系统实际有 13 个模块（用户 2026-09-21 定稿）⇒ 一半模块**在名册上不存在**，
     "给某租户开资料模块"这件事在数据上无从表达；
  ② **模块与权限码之间没有任何对应关系** —— 谁也不知道"关掉仓储模块"到底该关掉哪些码。
     且 `TenantModuleService.hasModuleAccess()` / `getValidModuleCodes()` **只有读接口调用**
     （`TenantModuleController`、`SetAppCenterController`），**没有一处参与请求拦截**
     ⇒ 后端**不按模块授权做任何拦截**，模块开关目前纯属展示。
- **✅ 已完成（2026-09-21，迁移 `V11.454.0`）**：
  · 模块目录 **6 → 13**：`crm` 改名「**客户服务**」（售前+售后两阶段一个模块）、
    `warehouse` 语义收窄（只留 `stock:` + `wms:`，商品档案/往来单位划出）、
    新增 资料/交易/配送/人力资源/分析/设置(租户级)/系统(平台级)；
  · 新建 **`sys_module_permission`**（模块 → 权限码前缀），37 条前缀覆盖全部 12 个有码模块；
    归属口径 = **最长前缀优先**，同长取 sort 小；
  · 回填既有两个租户的开通记录（口径：**现状不降级** —— 迁移不替业务做减法，
    先按"今天实际能用的全都开"，再由平台职员按合同主动关）。
- **验证**：新增 `tools/verify-module-mapping.cjs` **35/35** —— 13 模块名逐条比对、
  映射无孤儿模块码、**923/923 在役码 100% 有归属**、无同长前缀歧义、
  `analytics` 映射数为 0（已知缺口，断言为 0 以防被塞假码）、两租户各 13 条开通记录。
- **✅ entitlement 门已实现（2026-09-21）** —— 新增 4 个文件 + 2 处登记：
  | 文件 | 作用 |
  |---|---|
  | `core-base/.../entity/SysModulePermission.java` + `mapper/SysModulePermissionMapper.java` | `sys_module_permission` 的实体/Mapper（此前只有表、没有代码入口） |
  | `core-api/.../module/service/ModuleEntitlementService.java` | 码→模块解析（**最长前缀优先、同长取 sort 小**）+ 租户开通集合缓存（30s）+ 前缀规则缓存（5min） |
  | `core-api/.../module/interceptor/ModuleEntitlementInterceptor.java` | 请求链判定，未开通 → `BusinessException.forbidden("模块未开通：<名>（<码>），请联系平台管理员")` |
  | `core-api/.../config/WebMvcConfig.java` | 注册 **order 3**（core-base 不能反向依赖 core-api，故注册点只能在这里） |
  · `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 增加 `sys_module_permission`（平台级参考数据，
    否则租户会话读它会被注入 `AND tenant_id=<会话租户>` → 一行读不到 → **这道门静默失效**）。
  · `TenantModuleService` 新增 `getValidModuleCodesStrict()`（**不吞异常**），
    原方法改为「调用它 + try/catch」以保持行为不变；目的是让门能区分
    「真没开通」与「读库失败」——两者都判成 403，会把一次 DB 抖动放大成整租户不可用。
- **判定链与三条「有意不拦」**（都写进了类注释）：码无归属前缀 → 不拦（`analytics` 整域无码；
  把"查不到归属"当"未开通"会让任何新码当场 403）；超管 → 不拦；**读库失败 → 不拦**（fail-open + ERROR 日志）。
  第三条与 `AiReadyTenantLineInnerInterceptor` 的 fail-closed 口径**故意相反**，理由不同：
  数据边界必须 fail-closed，而模块门只表达"平台方卖没卖"，一次 DB 抖动不该让整租户全站 403。
- **顺序已反编译确认**：`SaInterceptor`(order 1) 的 `preHandle` 内部先调
  `SaStrategy.checkMethodAnnotation`（做 `@SaCheckPermission`）再跑 auth 函数，
  故 order 3 天然在其之后 —— 无权限时报的仍是「无权限访问: xxx」而非「模块未开通」。
  这个顺序**只会影响文案、不会报错**，所以必须显式断言，见验证脚本第 ② 组。
- **覆盖口径（实测端点计数）**：认两种注解 —— `@SaCheckPermission` **1438 个端点**（主流）
  与 `@RequirePermission` **18 个**。两者默认语义**相反**，已分别取值：
  `SaCheckPermission.mode` 默认 **AND**（反编译 `AnnotationDefault: SaMode.AND` 确认），
  `RequirePermission.logical` 默认 **OR** ⇒ OR 语义下"第一个码的模块没开通"不足以拦人
  （用户可能靠第二个码进得来），必须全不满足才拦。**这个参数不能省。**
- **两处已知不覆盖/顺序例外**（都写进了类注释）：
  ① `CrmPermissions.require(code)` 这类**程序化**校验共 3 处（在 crm 子模块，因为它只依赖
     core-base 拿不到注解）—— 没有可枚举的注解，模块门看不见它们。不是权限被放开，
     是模块门对那 3 个接口不生效；要收口需改成 `@SaCheckPermission`（sa-token 注解任何模块都能用）。
  ② `@RequirePermission` 由 **AOP 切面** `PermissionAspect` 处理，而 AOP 切的是方法调用、
     永远晚于所有拦截器 `preHandle` ⇒ 这 18 个端点上模块门会**先于**权限检查说话。
     两者都是 403 且都真实成立，故未为此把那批判定搬进切面（代价是同类拒绝文案不同）。
- **⚠️ 遗留（这是下一段活的全部内容）**：
  ① **模块开通/停用没有写入入口**：`TenantModuleService.assignModule()` / `removeModule()`
     **全仓零调用方**（实测 grep），`SetAppCenterController` 与 `TenantModuleController` 都只读，
     前端 `views/admin/tenant/module-auth` 也只有查询。
     ⇒ 现在这道门**只能拦、不能放**：平台方无法从界面上给租户开关模块（只能改库）。
     原「开通/停用时自动派生/回收该租户的授权」因此**无法开工**，属要先补的产品能力（写接口 + 码 + 页面）。
  ② **分析模块整域没有权限码**（`analytics%` 查询 0 行，`/views/analytics/**` 只有登录校验，
     见 E-08）⇒ 该模块的映射先建成空表，码族要单独设计；
  ③ ~~平台级「系统」模块该开给谁~~ **✅ 已裁定（2026-09-21 用户）**：
     **「系统」模块只开给系统租户**（"系统模块权限由超管授权给系统租户的用户"）；
     **「设置」才是租户级的管理设置模块**。落实在迁移 `V11.455.0`（软删非系统租户的
     `system` 开通记录 + 补系统租户那条），实测租户 1 = 13 个模块、租户 2 = 12 个（无「系统」）。
- **✅ 该裁定顺带暴露的冲突已解决（2026-09-21，用户裁定：③ 另立租户管理码族）**：
  冲突是——租户内"人/角色/权限"的码前缀都是 `system:`、按映射全归「系统」模块，
  而「系统」已收紧为**只开给系统租户** ⇒ 业务租户将再无码可管理自己的用户与角色；
  但用户口述同时说"租户内的部门管理员权限由租户所在系统管理员给与配置"。两者对不上。
  **裁定：另立「租户管理」码族 `tenant-admin:`，归「设置」（租户级）模块。**
  落实为 `V11.456.0` + `V11.457.0` 两个迁移与一次**跨端重命名**：
  · **7 个子域从 `system:` 剥离**：`system:<fam>:<act>` → `tenant-admin:<fam>:<act>`
    （user / role / permission / data-scope / field-permission / record-rule / sod-rule）；
  · **5 个原裸前缀子域并入同一族**：`<fam>:<act>` → `tenant-admin:<fam>:<act>`
    （department / position / permission-template / role-inheritance / **data-scope**）；
  · 合计 **72 条码**，映射表新增 `tenant-admin:` 前缀挂到「设置」，并清掉「系统」模块里
    已迁走的 5 条前缀。
  **⚠️ 这类"改码文本"的活比命名本身危险得多**：本仓铁律是「码必须先在库里存在，注解才能挂」，
  改一半（库改了注解没改）的症状是**所有非超管一律 403**，且不报编译错、不报启动错。
  故写了 `tools/rename-permission-prefix.py`：**从真库读码 → 整码精确匹配 → 跨后端注解 +
  前端 v-permission + 种子配置一起改 → 改完复扫断言旧码零残留**。实测 **225 处引用 / 24 个文件**。
  · **该脚本第一版按"前缀"替换，踩了两个坑，都已修并写进脚本注释**：
    ① 把打印引擎里的 CSS `position:relative;` / `position:absolute;`（4 处）当成权限码；
    ② 漏了「同一个子域里混着两种写法」的情况 —— `data-scope` 有 3 条在 `system:` 下、
       另 2 条是裸前缀，第一版只搬了 system: 那半，结果那 2 条**变成无模块归属**。
       暴露靠的不是人肉复查，而是 `verify-module-mapping.cjs` 里那条
       **"在役码必须 100% 能被某个前缀接住"** 的断言（直接报 `未归属 2 条`）。
  · **另一个顺带修掉的工具坑**：`tools/build-backend.sh` 在"没有实例在跑"时会
    **完全静默地 exit 1**（`grep -E '^[0-9]+$'` 无匹配 → `pipefail` → `set -e`，
    且退出发生在任何 echo 之前）。上一次启动失败之后本来就没实例，再跑构建就什么都看不到。
    已加 `|| true`，空结果会走"没有正在运行的后端实例"分支。
- **状态**：🔄 名册与映射表 ✅（`verify-module-mapping.cjs` **43/43**）；
  「系统」只开系统租户 ✅（`V11.455.0`）；**租户管理码族 ✅（`V11.456.0` + `V11.457.0`）**；
  **entitlement 门 ✅ 已实现并实机验证**（`tools/verify-module-entitlement.cjs`，
  拒绝/放行/顺序/恢复四组）；**剩「模块开通/停用写入入口」缺失 ⇒ 这道门目前只能拦不能放**。

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

#### E-01 wms 批次 [P0] 172 个裸端点补注解 —— ✅ 已完成（2026-09-21）

- **做了什么**：wms 模块 **20 个控制器 / 162 个端点**补上 `@SaCheckPermission`（共 **162 处**），
  新增迁移 `V11.459.0__Seed_Wms_Permissions.sql`（**86 个新码**，`wms:` 前缀码 11 → 97）；
  基线 `known-unauthorized-controllers.txt` 同步收缩 wms 20 行 + crm 10 行（棘轮规则要求）。
  裸控制器 **197 → 170**、裸端点 **1765 → 1459**。
- **⚠️ 前缀陷阱（本条最容易搞错的地方）**：库里原本就有 **11 条 `wms:*` 码**，容易被当成"已有码可直接用"——
  实测它们的 `api_path` **全部是 `/api/erp/warehouse*`**（erp-stock 的仓库/仓库分类控制器，见 V11.443.0），
  与 `backend/wms` 的 172 个端点**零交集**；复用会让"改 ERP 仓库档案"和"改 WMS 仓库作业"共用一个开关。
  ⇒ **wms 实为零码模块**，必须现场建码。
- **PDA 口径已定稿（解除原 `manual_only` 的阻塞）**：**PDA 复用后台同名资源的码**，不建 `wms:pda:*`。
  依据是代码实测（非推测）：PDA 用真实 `sys_user` + BCrypt 登录，且与后台**共用同一套 Sa-Token
  会话/token/权限链路**（`PdaAuthController` 直接 `StpUtil`；`SaTokenJwtConfig` 整文件被注释、
  无自定义 StpLogic；token-name 同为 `Authorization`）⇒ **"设备令牌"在本仓不存在、也不需要**，
  设备身份是**角色策略**问题（给设备角色勾哪些码）。
  另：PDA 收货控制器类名是 `receive`、后台是 `receipt`，本批**统一到 `receipt`**
  （同一业务对象两种拼写不该变成两套码）。
- **显式排除的 10 个端点（绝不能加权限注解）**，已写进生成器配置并在 `--apply` 报告里逐条显示：
  · `POST /api/v1/warehouse/auth/login`（`SaTokenConfig` 两处白名单，未登录必须可达）
  · `POST /api/v1/warehouse/auth/logout`（加码 ⇒ 没该码的用户无法登出）
  · `ErpCallbackController`（`/api/erp/wms/**`，4）+ `ErpIntegrationController`（`/api/wms/erp/**`，4）
- **🔴 顺带查实一条独立缺陷（未修，需拍板）**：上面那 8 个 ERP 集成端点
  **既不在白名单里、代码里也没有任何验签实现**（`ErpCallbackServiceImpl` 四个方法全是 log + `return ok`），
  而 `EventServiceImpl` 的内部 outbox 投递**不带任何认证头** ⇒ **该回调链路按代码推断是断的**
  （实测未登录调用返回 `401 请先登录`）。**修法只能是补服务间鉴权（验签/内部令牌），
  不能靠加白名单** —— 加白名单等于把库存变更接口变成匿名可达。属独立决策项。
- **生成器本轮的增强**（都写进了工具注释）：新增 `base_overrides`（按**类级路径**指定资源 ——
  PDA 八个控制器推导出的「域:资源」全是 `wms`，用资源当键会互相覆盖）、
  `skip_bases` / `skip_methods`（安全阀，排除登录端点与无会话接口）、
  以及一批**状态/生命周期类 POST 的通用动作规则**（start/cancel/confirm/complete/pause/resume/retry/post/reconcile），
  此前它们一律落到 `create` 兜底，把"启动拣货任务""取消入库"算成"新建单据"。
- **验证（全绿）**：`verify-module-authz.cjs wms` **22/22**（11 资源 × 超管放行/非超管 403）；
  `AuthzAnnotationCoverageTest` **3/3**（含"基线不得过期"）；
  回归 `verify-tenant-hardening` **14/14**、`verify-permission-changes` **23/23**、
  `verify-module-mapping` **43/43**、`verify-module-entitlement` **8/8**、
  `verify-module-authz crm` **18/18**、`verify-crm-tenant-fix` **6/6**；
  另实测被排除端点行为未变：PDA 登录仍可达（错密码 → `401 用户名或密码错误`，非"请先登录"）、
  ERP 回调仍是 `401 请先登录`（改前就这样，本轮未触碰）。
- **⚠️ 后果（须知悉）**：与 crm 批次同理 —— 新码只挂在超管，**租户/设备角色必须被授予 wms 码
  才能用仓储作业**（否则整块功能对非超管 403）。

#### MKT-MODEL-01 [P0] 商城顾客的归属模型：**系统顾客 + 租户关联** 双层（2026-09-22 用户裁定）

- **用户口径（原话）**：「商城顾客应该又是**系统顾客**！商城顾客是属于租户的，系统顾客是属于系统的，
  **一个系统顾客可以属于多个租户，前提是租户添加该顾客**」。
- **模型解读（与本仓既有形态对齐）**：这就是 `sys_user` + `sys_user_tenant` 那一套的镜像 ——
  · **系统顾客**：一个全局身份，不预设某一个租户（相当于 `sys_user`）；
  · **租户关联**：租户通过"添加该顾客"建立归属（相当于 `sys_user_tenant` 的 `user_id × tenant_id` + 状态）；
  · 因此同一顾客可被多个租户分别添加、各自维护自己的业务数据（订单/购物车/收货址）。
- **⚠️ 与现状的差距（三点，都已实测）**：
  ① `shop_user.tenant_id` 是 **NOT NULL 的单租户列**（`information_schema` 已核）⇒ 今天的模型是
     「一个商城账号属于且只属于一个租户」，与"一个系统顾客可属多个租户"**直接冲突**；
  ② **没有系统级顾客实体**，也**没有"顾客 × 租户"关联表**（对照 `sys_user_tenant` 45 行；
     `shop_user` 0 行、`shop_user_party_link` 0 行）；
  ③ 没有"**租户添加该顾客**"这条流程 —— 而它正是用户口径里的**授权动作**（谁把顾客纳入本租户）。
- **对 E-01 的影响（口径要跟着改）**：我在 E-01 里把 `/api/v1/mall/**` 整块排除，当时的理由是
  「`shop_user` 会话查不到 `sys_role_permission` 的码」。按本裁定，**理由要改成**：
  C 端顾客是**系统级身份、本就不该套员工 RBAC 码**；它的访问边界应当由
  **"该租户是否已添加该顾客"这类归属校验**来保证 —— 也就是说，
  **被排除不等于不需要访问控制**，只是控制形态不是权限码，而是归属关系。
  （现状是：`MallGuestAccess` 只判"哪家店 + 是否允许游客"，**没有任何"这个顾客属不属于本租户"的校验**。）
- **落地形态（建议，待确认）**：
  · 新增 `shop_user_tenant`（`shop_user_id, tenant_id, status, is_default, source, create_time…`，
    `UNIQUE(shop_user_id, tenant_id)`），照抄 `sys_user_tenant` 的形状；
  · `shop_user.tenant_id` → 语义降级为"**注册来源租户**"或直接改为允许 0/可空（系统级身份）；
  · 商城登录：身份 = 系统顾客；**租户上下文按请求解析**（`X-Tenant-Id` / 域名），
    并校验该 `(shop_user, tenant)` 关联存在且 `status=正常`，否则拒绝；
  · "租户添加顾客" = 落一行关联（可带审核，参照 `shop_user_party_link` 已有的
    `status/applied_phone/tenant_approved_by/...` 字段与审批流）。
- **好消息**：`shop_user` / `shop_user_party_link` **都是 0 行** ⇒ 改模型**无历史数据迁移风险**，
  可以按目标形态直接建，不必写回填脚本。
- **状态**：⏸ **已裁定模型，待拍板"是否现在实施 + 落地形态"**（它会动商城登录、租户上下文解析、
  以及 C 端的访问控制形态，属独立功能项，不是几行改动）。

#### MKT-MODEL-02 [P0] 用户 / 往来单位 / 租户 三主体模型（2026-09-22 讨论中，未定稿）

> ⚠️ 与 `MKT-MODEL-01` 是同一件事的两层：01 是"商城顾客"那一层（已裁定的最小改动），
> 02 是它背后的完整主体模型。**先定 02，再决定 01 的两张表要不要按 02 的形态重排。**

**核心判断**：本仓把**两个系统级主体**硬塞进了"租户内"，所有错位都是这一条派生的。
1. **自然人**（谁在操作）：现在割裂成 `sys_user`（员工，带 tenant_id）/ `shop_user`（顾客）/
   `hr_employee`（0 行，tenant_id + user_id）—— 同一个真人 2~3 个账号，互不认识。
2. **往来单位**（替谁做事 / 谁在和我做生意）：`biz_party` **带 `tenant_id`** ⇒ 被切成"每个租户一份"，
   同一真单位在 E、F 是两条互不相识的记录，且无法表达"B 既是 E 的客户、自己又是租户"。
3. **租户**：`sys_tenant` 与往来单位**无任何关联**。

**已实测的四条"混在一起"的证据**：
· `biz_party.tenant_id`（把"关系"压成了"归属"）；
· `biz_party.party_level='MEMBER'` + `member_card_no/points/member_level` ⇒ **个人会员被当成一条往来单位**；
· `biz_party_contact`（72 行）带 `open_mall_account/linkman/contact_id` ⇒ 是 R1 的雏形，但长得像通讯录字段；
· `shop_user_party_link` 已有完整审批流（`status/applied_phone/enterprise_approved_by/tenant_approved_by`）
  ⇒ **R1 的正规形态其实已经写好**，只是 0 行、只服务商城侧。

**模型 = 4 条边 + 1 个维度**：

| 边 | 含义 | 现状 |
|---|---|---|
| R1 自然人 × 往来单位 | 任职/代表（员工、法人、采购、财务…），多对多 + 审批 + 角色 | `shop_user_party_link` ✅ 结构已有 |
| R2 往来单位 × 租户 | "这个单位在我店里是客户" + 等级/信用/账期 | 被 `biz_party.tenant_id` 压成归属 ⚠️ 要拆 |
| R3 自然人 × 租户 | "这个人在这家店注册过" + 审核 | `shop_user_tenant`（V11.484.0）✅ |
| R4 租户 × 往来单位 | "这家店属于哪个单位" | ❌ 不存在（`sys_tenant` 无 party 关联） |
| 维度 | 单据上的「**代谁**」`acting_party_id`（空=个人自购） | ❌ 无（`mall_order` 只有单值 `customer_id`） |

**唯一一条授权推导规则**：
> **一个人在某租户能代表哪个单位 = （他所属的单位 R1）∩（该租户的客户 R2）**

用户给的五种场景全部由它推出（E 处为 B/C 下单、F 处为自己与为 C、C 在 B 店下单、**C 自己开店**）。

**R4 有两种产生方式（2026-09-22 补充）**：
· **平台开具**（"单位 B 又是一个租户"）；
· **自助申请**——"客户 C 在 E 处下单觉得商城好，**也可以申请开店成为一个租户自行开店**"。
  **仓库里已有现成骨架**：`TenantRegistrationController`（自助注册 → 系统管理员审批 → 自动初始化租户环境 → 驳回 → 待审核列表）。
  **缺的只是**：申请要**关联一个往来单位**（开店主体是单位，不是自然人）、审批通过时落一条 **R4** 关联、
  以及租户初始化的**商店配置/管理员绑定**。⚠️ 该接口不在 `SaTokenConfig` 白名单（注释却写"公开接口"）——见既有缺陷表。

**身份叠加不冲突（这正是"主体系统级 + 关系租户内"的价值）**：C 在 E 处是**客户**（E 的一条 R2 + C 的 R3），
同时 C 自己是**租户**（一条 R4）——同一个 C 在两处并存，不需要复制身份。

**身份选择器有两个轴**（照此实现，现有 `IdentityDTO`/`activePartyId`/`switchIdentity` 已是雏形）：
① **入口决定进哪家店**（tenant：域名 / 路由 / `X-Tenant-Id`）；
② **在这家店以什么身份** = 个人 ∪ (R1 ∩ R2) ∪ 店内角色（老板/员工）。
所以 C 在 E 的商城只能选"客户"，在**自己的**商城是"老板/管理员"。

**落地三步（每步可独立上线）**：
① 自然人侧：`shop_user` 升系统级（✅ V11.484.0）+ `shop_user_tenant` + 登录入店校验 + 租户审核；
② 往来单位升系统级：`biz_party.tenant_id` 拆成 `party`(主档, 键 = `unified_code`) + `party_tenant`(客户关系)，
   **存量 152 行需归并策略**；③ 加 R4（含"申请开店"流程）+ 单据 `acting_party_id`。

**待拍板（6 点）**：① `sys_user`/`shop_user`/`hr_employee.user_id` 是否物理合并（建议先不合并）；
② 存量 152 行往来单位按 `unified_code` 怎么归并；③ R1 审批人是谁（单位管理员 / 租户管理员 / 双审）；
④ `acting_party_id` 与 `person_id` 是否分离（**建议必须分离**：谁买 vs 谁付）；
⑤ 个人顾客与单位客户是否同一张档案（**建议分开**：`person_tenant` 与 `party_tenant` 各一张）；
⑥ **一个单位能开几个店**（建议 R4 为 0..N，租户→单位则为 0..1，空=平台自营）、**个人能否开店**。

**R2 必须是「双向关系 + 角色集合」，不是单向归档（2026-09-22 补充）**：
用户给出——**租户 B 与租户 F 可以互为客户和供应商**（B 向 F 卖鸡米花、F 向 B 卖面包）。
这一条纠正了三处：
① **角色是"关系"的属性**（在 `(租户, 主体)` 这条边上），不是主体的属性；
   且**可并存**（同一对主体既是客户又是供应商）——本仓 `biz_party.roles` 现在是**单值** `CUSTOMER`/`SUPPLIER`，
   不够用；现实里还有"今天供货明天采购"，所以角色只能挂在关系上、不能挂在主体上。
② **方向由单据决定，不由档案决定**：B 的销售单 `seller=B(P), buyer=F(P)`；F 的销售单 `seller=F(P), buyer=B(P)`。
   ⇒ 单据上必须有明确的 **seller / buyer 主体**（本仓 `erp_sale_order` 之类只有 `customer_id` 这种单值对方，
   表达不了"互为"）。**建议统一成 `seller_party_id` + `buyer_party_id`**。
③ **R4 与 R2 是两条不同的边**：R4 = ownership（这家店属于哪个单位），R2 = 贸易关系（谁在我店里是客户/供应商）。
   「B 与 F 互为客户供应商」= B、F 各自有 R4 指向自己那个主体，再加两条方向相反的 R2。
④ **由此自然产生第三层需求：跨租户对账/净额结算**（我卖你 10 万、你卖我 8 万 ⇒ 净结 2 万）。
   它直接影响 FIN 域应收应付口径，**要单独拍板**（见下 ⑫）。

**我上一条问的"投单时买方主体是租户还是往来单位"，被这条回答了：必须是往来单位（主体）**——
因为"互为买卖"要求双方都被识别为**同一个系统级主体**，租户只是"在哪里操作"。
⇒ `inter_tenant_doc_link` 存的是**主体对 + 租户对 + 两张单据**，不能只存租户对。

**状态**：🔄 **设计文档 v1 已落地：`docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md`**（403 行，2026-09-22）。
文档含：6 个真实场景 → 三主体 + 四条边 + 一个维度 → DDL 级表结构 → 四套状态机 →
跨租户单据协同（含可见性边界与"不许跨租户直写"三条铁律）→ 三步迁移路线 → 决策记录（13+2 条）。
**已认同 8 条（2026-09-22 用户）**：
⑪ 单据统一 `seller_party_id`/`buyer_party_id`；⑫ 要做跨租户对账/净额结算；
⑬ 同一对主体在同一家店的角色可并存；**② 存量 152 行往来单位是开发模拟数据、不需人工确认**（但要补唯一约束闸门）；
**⑥ 开店与营业执照挂钩：一店一照为默认，同照再开需审批**（"多开几个店你就多办几个营业执照"；
"国家打击一店多开，但**这个能力要保留**"）；**⑦ 个人可开店但须符合国家法规**（须有执照/个体户登记）；
**⑧ 推单，不是镜像**；⑨ 卖方确认制 + 确认后改走变更单。
**📌 设计原则（用户强调）**：「用户/往来单位/租户的场景是**符合当下真实世界交易真实场景**的」
⇒ 现实怎么发生就怎么建模，**不接受用技术便利简化真实商业关系**。
**并已认同**：⑯ **代销/寄售要建模**（S7 一件代发 + S9 经销中转 + S8 厂家推单）；
⑱ **S7/S8/S9 复用"推单"机制**，不新造机制。
**🔑 由 S7/S8/S9 提炼出的第三个正交维度**：**「交易链（钱怎么走）」与「履约路径（货怎么走）」必须分开建模** ——
S7 与 S9 的**交易链完全相同**（客户→F、F→B），差别只在货**跳不跳过 F**：
直发 = 1 段运输、F 不动库存；中转 = 2 段运输（B→F、F→客户）、**货权在中途转移**、F 先增后减库存。
同（一份分销协议可能两种履约方式都用；**同一张订单的不同行也可不同**）。
**又认同 3 条**：
**⑲（账期）「F 给客户 30 天、B 只给 F 10 天或现结，这是两个不同维度的问题，F 给客户的承诺不能变成 B 给 F 的承诺」**
⇒ 账期/信用/价格是**有向边**的属性，`party_tenant` 必须带 `direction`（SALE/PURCHASE）一行一个方向；
一张客户订单产生**两条独立应收应付**（F 对客户 30 天、F 对 B 10 天），**20 天差额是 F 自己的垫资**，
系统要如实呈现、**不得对齐**。**（同时又更正了文档里早先"角色集合 + 单值账期"那处自相矛盾）**
**⑳（取消/退货/责任划分）「客户取消一张已推给 B 且 B 已备货的单，这要求 F 和 B 达成代销合作的时候确定」**
⇒ 属**协议条款**，系统不做全局规则；分销关系因此升级为**协议（条款层）+ 商品明细（价格层）两层**。
**由此推出的通用原则**：**凡是"双方怎么分责任"的问题，答案都在双方约定的协议里，不在系统全局规则里**；
系统只负责 ① 提供条款的结构化载体、② 如实执行**当时生效**的那份协议（故协议需生效区间 + 版本）。
**自检结论（文档 §七）**：把"一笔真实生意从头到尾走一遍"后，**「商品与单据」主干较完整，但「钱」与「逆向」两块基本空白**（16 条缺口已登记）。
**✅ 2026-09-22 用户批复「其他都按你的推荐执行」⇒ 讨论过的 40 条全部裁定完毕**，文档定稿 v1（`docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md`，1070 行，v2.4）：
· §6.1 **已明确清单 40 条**（标注来源：用户裁定 / 派生结论 / 按推荐采纳）；
· §6.2 **五条通用原则**（层层收紧，第五条"协议自治有边界"最高）；
· §6.3 平台角色三句话；§6.4 **我撤回过的 4 条推荐留痕**（防止后人再踩）；
· §七 缺口清单（对照真实交易自检 16 条）；§八 决策归档 + 后续新议题登记处。
**两处带附加条件（实施必须照做）**：★G4 资金**默认直连收单**（规避"二清"）但 `fund_flow_mode` 要做成平台级可配、**保留代收代付扩展位**（未来开平台级商城时切 ESCROW 不需重构）；⑳a 可售量 = **额度 ∧ 供货方真实可用库存**（双重校验，**硬要求不得出现"无货发单"**，对外只暴露"充足/少量/无货"，跨租户锁库存走事件投递）。
**下一步**：按 §5 的三步路线开工——第 1 步（自然人侧）收尾 ⇒ 第 2 步（往来单位升系统级）⇒ 第 3 步（R4 + 单据四主体 + 代谁）。⑳-余 可售量取"共享实时可用"还是"额度"、中转入库要不要在途状态 · ㉑ 开票与售后归属 ·
㉒ 订单路由规则由谁定 · ㉓ 代销协议是否并入 `purchase_contract` · ㉔ 逆向链做到什么程度 · ㉕ 平台抽成怎么落（含"二清"合规）。
**定稿前不要**按 02 去改 `shop_user_tenant`；第 1 步（自然人侧）只做 §5.2 那三件、可先行收尾。

#### 本轮顺带查出/修掉的既有缺陷（E-01 之外的意外收获）

| # | 缺陷 | 影响 | 处置 |
|---|---|---|---|
| 1 | `PurchaseInquiryMapper` 把 `deleted = 0` 写在**布尔列**上；且 `#{x} IS NULL` 的裸占位符 PG 无法推断类型 | **采购询价单整块 500**（列表/详情/统计全废） | ✅ 已修（`deleted = false/true` + `jdbcType=VARCHAR`） |
| 2 | `SaleOrderServiceImpl:129` 用 `Map.of(...)`，而 `customerId`/`status` 是可选筛选（null 值 → `Map.of` 抛 NPE） | **销售订单列表不带筛选条件必然 500** | ✅ 已修（改 `HashMap`） |
| 3 | `sys_notification_record` **无 `tenant_id` 列**，却未登记进 `IGNORE_TENANT_TABLES` | 通知接口对**任何租户会话**整块 500（超管豁免故未暴露） | ✅ 已修（登记忽略 + 注释） |
| 4 | `NotificationController` 身份取自可伪造的 `X-User-Id` 头 | 任何登录用户改头即可读写**他人**通知；且前端从不发该头 ⇒ userId 恒 null、功能恒空 | ✅ 已修（改会话取 id）。**但服务层仍是内存桩**，见 #5 |
| 5 | `NotificationServiceImpl` 用**内存 Map** 存通知（`records`/`userNotifications`），不落库 | 通知不持久化；`markAsRead`/`deleteNotification` **只按 id 不校验归属**（IDOR） | ⬜ 未修 —— 属"整个模块是桩实现"，要按功能项做，不是改几行 |
| 6 | 供应商积分 Mapper 与表结构漂移：`SupplierPointsRecordMapper.java:14/17/20` 用 `create_time`/`points`/`points_type`，真库列是 `created_time`/`points_amount`/`record_type` | 供应商积分记录/汇总/看板全 500 | ⬜ 未修 |
| 7 | `POST /api/v2/print/client/auth/login` **不在 `SaTokenConfig` 任何白名单** | 打印客户端（Electron）**登不进来**，整个客户端不可用 | ⬜ 未修（与 09-20 修的 PDA 登录同类） |
| 8 | `BatchNumberMapper#selectByBatchNo/selectExpiringBatches/selectStockSummary`、`SerialNumberMapper#selectWarrantyExpiring` 既无 `@Select` 也无 mapper XML（`erp-stock/src/main/resources` 目录不存在） | 批次/序列号 4 个查询必然 `Invalid bound statement` 500 | ⬜ 未修 |
| 9 | `OpenApiController.java:49` 收了 `signature` 参数但 `:51` **没把它传给业务**（类注释却写着"签名校验"） | 开放接口的签名校验形同虚设 | ⬜ 未修（安全项） |
| 10 | `verify-permission-changes.cjs:31` 默认读**固定文件名**的日志（`backend-restart-20260920.log`） | 它校对的可能是几天前那次启动，容易误判"已生效" | ⬜ 未修（工具项：应改成取最新日志） |
| 11 | b2b 商城后台侧此前**只被全局 `checkLogin()` 保护** | **任何 C 端买家（shop_user 会话）都能调商城后台接口** | ✅ 本批补码顺带收掉 |

**两处被推翻的"刻意豁免"（已改写注释声明，如需恢复需"注解 + 码一并撤"）**：
`PriceApprovalController`（原注释：前端该页无 v-permission 码故不加）、
`SetMenuConfigController`（原注释：`sys_permission` 无 `set:` 码 —— 该前提本身也不成立）。

#### E-01 收口总表（2026-09-21 夜 ~ 09-22 凌晨）

**成果**：整类零权限注解的控制器 **197 → 27**，裸端点 **1765 → 126**。
共 **14 个批次**、**16 个模块**、新增 **18 个迁移**（`V11.458.0` ~ `V11.483.0`）。

| 批次 | 模块 | 控制器 | 注解数 | 迁移 | 实机验证 |
|---|---|---|---|---|---|
| 1 | crm | 10 | 179 | V11.458.0（+36 码） | 18/18 |
| 2 | wms | 20 | 162 | V11.459.0（+86 码） | 22/22 |
| 3 | purchase | 10 | 97 | V11.460.0（+69 码） | 22/22 |
| 4 | sales | 19 | 197 | V11.461.0（+95 码） | 28/28 |
| 5 | dms | 20 | 210 | V11.462.0（+80 码） | 32/32 |
| 6 | erpfinance | 18 | 124 | V11.463.0（+72 码） | 22/22 |
| 7 | coreapi | 38 | 302 | V11.464.0（+97 码） | 36/36 |
| 8 | erpprinting / erppricing / erpobserv / erpbatchsn | 25 | 176 | V11.465.0~468.0（+111 码） | 26/14/8/8 |
| 9 | corebase / coreplatform / corenotify / corepayment / coreagent | 30 | 195 | V11.471.0~476.0（+108 码） | 34/12/6/8/— |
| 10 | b2b（仅后台侧）/ supplier / deliveryroute / partnerlevel / mktanalytics / stockextra | 14 | 170 | V11.477.0~483.0（+112 码） | 24/8/8/4/4/4 |
| 修正 | `V11.469.0`（补 7 码）+ `V11.470.0` / `V11.475.0` / `V11.483.0`（补模块前缀映射） | | | | |

**剩余 27 个控制器 / 126 端点 —— 全部是有意排除，不是待办**（逐类理由）：
① **未登录必须可达**：`AuthController`、`NotificationController`(core-base)、`SysRegionController`(下拉)、
`UserPageConfigController`、`SseNotificationController`、`PdaAuthController`、`ClientApiController`；
② **C 端顾客**：`/api/v1/mall/**` 全部（`MallAuthController`/`MallCartController`/`MallOrderController`/
`MallProductController`/`MallPartyLinkController`/`MallUserController`/`MallNoticeController`/`MallPaymentController`）
—— 登录主体是 `shop_user` 而非 `sys_user`，本仓权限体系查不到它的码，补注解 = 全部顾客 403；
③ **无用户会话的机器对机器**：`OpenApiController`、`ErpCallbackController`、`ErpIntegrationController`、
`AgentInvokeController`、`FileAccessController`；
④ **前端必然调用的自省/自助**：`ProfileController`、`DashboardController`、`DictController`、
`ErpBasicController`、`FileUploadController`、`DataFixController`（后者另有既有缺陷，见下）；
⑤ **未装配包**：`webhook` 等（运行时 404）。

**过程中的工具缺陷（都已修，且都是"会静默做错事"的那类）**：
1. `--sql` 的端点抽取**看不见方法上已有的注解** ⇒ 给已挂码的控制器造**永不使用的僵尸码**（改成与 `--apply` 同源）。
2. 类级覆盖判据把 `@SaCheckLogin` 当成"已管" ⇒ **类上只写登录校验的控制器被整片跳过**（改为只认权限类注解）。
3. `PERM_ONLY` 不认 `RequiresPermission`（core-base 的注解，由切面真实执行）⇒ 28 个已门禁方法被判成裸端点。
4. 判"是否已有注解"时**不剥注释** ⇒ Javadoc 里的 `{@code @SaCheckPermission}` 让整个控制器被跳过
   （实测 `PriceApprovalController` 8 个端点漏补）。
5. 路径换行写在下一行时的兜底会**误抓下一行的 `@Parameter(description="…")`** 当路径 ⇒ 产出 `dms:task任务信息:create` 这类垃圾码。
6. `--apply` 的 `@动作词` 简写会绕过 `base_overrides`/`resource_overrides`（已在配置注释里写明规避方式）。
7. `manual_only` 只是 `--list` 的展示标记，`build()`/`apply_annotations()` 都不读它 ⇒ 当年"b2b 不自动补注解"只是流程约定，跑一次 `--apply` 拦不住。

#### E-01 第 8/9 批（coreapi + erp 小模块）[P0] —— ✅ 已完成（2026-09-21 夜）

- **core-api**：38 个控制器 / **302 处**注解，迁移 `V11.464.0` 补 **97 个码**（复用 21 个既有码）；
  `verify-module-authz coreapi` **36/36**。
- **erp-printing / erp-pricing / erp-observability / erp-stock(batchsn)**：
  25 个控制器 / **176 处**注解，迁移 `V11.465.0`~`V11.468.0` 补 **111 个码**；
  四个模块的 `verify-module-authz` 分别 **26/26、14/14、8/8、8/8**。
- **E-01 累计进度**：裸控制器 **197 → 64**、裸端点 **1765 → 499**。
- **🔴 修掉一个会造成"静默漏补"的工具缺陷（3 处判据）**：判"类/方法是否已有权限注解"时
  **没有剥注释** —— 本仓 Javadoc 会写 `{@code @SaCheckPermission}` 来解释设计，
  于是 `PriceApprovalController` 被整类判成"已有注解"而跳过，**8 个端点（含审批通过/拒绝）漏补**，
  且 `--apply` 只打印「跳过（类级已有权限注解）」，不报错不告警。修法是先 `strip_comments` 再匹配。
  **同类还修了两处**：① `PERM_ONLY` 不认 `RequiresPermission`（core-base 的注解，由 PermissionAspect 真实执行）
  ⇒ 28 个早已被门禁的方法被判成裸端点；② `--sql` 用的端点抽取看不见方法上已有的注解
  ⇒ 给已挂码的控制器造**永远不会被插入的僵尸码**。
- **⚠️ 一处与"既有刻意豁免"的冲突（已按 E-01 口径处理，但需你知悉）**：
  `PriceApprovalController` 的类 Javadoc 原本写明「前端该页未使用任何 v-permission 码，
  故**刻意不加** `@SaCheckPermission`」。本轮按 E-01 的既定口径（每个端点都要有码、
  再由管理员通过矩阵授予角色）给它补了 `pricing:approval:*`，**并已改写该 Javadoc** 说明改动原因与副作用。
  若你希望恢复旧口径，需**注解与这批码一并撤**（只撤一边 = 僵尸码或漏补）。
- **新增 6 个权限前缀的模块归属**（迁移 `V11.470.0`）：`signature:→设置`、`metrics:/monitor:→系统`、
  `pricing:→资料`、`erp:batch:/erp:serial:→仓储`。不补的话
  `verify-module-mapping.cjs` 的「在役码 100% 有归属」断言会红，且模块门对这批码不生效。
  **刻意不把这些码归到 `analytics`** —— 该脚本有一条硬断言「analytics 映射数必须为 0」（已知缺口）。
- **🔴 两条既有缺陷（未修，只记录）**：
  ① **打印客户端登不进来**：`POST /api/v2/print/client/auth/login` **不在 `SaTokenConfig` 的任何白名单里**，
     而 Electron 客户端正是调它登录（`frontend/apps/print-client/src/main/services/auth.ts:62`）
     ⇒ 该客户端今天实际不可用（与 2026-09-20 修复的 PDA 登录是同一类问题）。
  ② **批次/序列号 4 个查询必然 500**：`BatchNumberMapper#selectByBatchNo/selectExpiringBatches/selectStockSummary`、
     `SerialNumberMapper#selectWarrantyExpiring` 既无 `@Select` 也没有 mapper XML
     （`erp-stock/src/main/resources` 目录不存在）⇒ `Invalid bound statement (not found)`。
     与本轮改动无关（无注解时同样 500）。

#### E-01 purchase / sales 批次 [P0] —— ✅ 已完成（2026-09-21 夜）

- **purchase**：10 个控制器 / **97 处**注解，迁移 `V11.460.0` 补 **69 个码**（purchase 码 23 → 92）。
  `verify-module-authz purchase` **22/22**。
- **sales**：19 个控制器 / **197 处**注解，迁移 `V11.461.0` 补 **95 个码**（sale 前缀 18 → 105）。
  `verify-module-authz sales` **28/28**。
- **两批共关闭 306 个裸端点**（本项目累计：裸控制器 **197 → 143**、裸端点 **1765 → 1186**）。
- **🔴 顺带查出并修掉 3 个真实缺陷（都不是本轮改动引入的）**：
  ① **采购询价单整块功能 500**（`PurchaseInquiryMapper`）：`deleted = 0` 写在**布尔列**上
     （`purchase_inquiry.deleted` 是 boolean）⇒ 该控制器所有查询报
     `操作符不存在: boolean = integer`；改 `deleted = false/true` 后又暴露第二个：
     `#{keyword} IS NULL` 里的裸占位符 PG **无法推断类型** ⇒ 补 `jdbcType=VARCHAR`。
     **即"没有参数就没有询价单列表"整页不可用。**
     全库共 **20 张表的 `deleted` 是 boolean**（含 budget/expense/fixed-asset 全族），
     已逐表扫过：只有 `purchase_inquiry` 用手写 SQL 拿了它当整数（其余走 MyBatis-Plus `@TableLogic`）。
  ② **销售订单列表不带筛选条件必然 500**（`SaleOrderServiceImpl:129`）：
     `Map.of(...)` 对 **null 值直接抛 NPE**，而 `customerId`/`status` 是可选筛选 ⇒
     无参调用 `/erp/sale/order/page` 必 NPE。改用允许 null 的 `HashMap`。
  ③ **工具自身的僵尸码缺陷**：`--sql` 用的端点抽取正则**看不见方法上已有的注解**，
     会把已挂码的控制器（如 `SalesPriceTrackController` 早已全挂 `sale:price:edit`）
     再算成"需要建码"⇒ 造出**永远不会被插入的僵尸码**。现已与 `--apply` 共用同一套
     逐行解析并标记 `annotated`：**只为"确实会被插注解"的端点建码**
     （sales 224 个端点里 27 个已注解，只有 197 个参与建码）。
- **另一个工具修正**：类级覆盖判据原用「任一鉴权注解」（含 `@SaCheckLogin`），
  导致**类上只写了 `@SaCheckLogin` 的控制器被整片跳过** —— 实测
  `UnifiedPurchaseDocQueryController` 因此漏补，其 `/api/purchase/doc-query/page`
  对非超管仍 200。现改为只认权限类注解（`@SaCheckPermission`/`@RequirePermission`）。

#### STK-AUTHZ-01 [P1] `wms.*` / PDA 零权限码

- 见 E-04；需先定 PDA 设备鉴权策略（`PdaAuthController` 本身还有 平台-BREAK-01 的漏写租户问题）。
- **状态**：✅ **已闭环（2026-09-21，见上方「E-01 wms 批次」）** ——
  20 个控制器 / 162 端点已补 `@SaCheckPermission`，新增迁移 `V11.459.0`（86 个新码），
  实机 `verify-module-authz.cjs wms` **22/22**。
  **原"必须先定的口径"已定**：PDA **复用用户会话与后台同名资源的码**（代码实测 PDA 已用
  真实 `sys_user` + BCrypt 登录、与后台共用同一套 Sa-Token 会话/token/权限链路）
  ⇒ **不需要设备令牌**；设备身份是"给设备角色勾哪些码"的角色策略问题。
  **余下 3 个控制器 10 个端点是"有意不加码"**：PDA 登录/登出 +
  `ErpCallback`/`ErpIntegration`（无会话的机器对机器接口，且**代码里没有任何验签**，见上方该条缺陷）。

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
  **实测剩余规模（2026-09-21 E-08 扫描）**：`crm` 域仍有 **7 个裸控制器 / 144 端点** ——
  批次 2 只覆盖了 6 个端点，**这个模块的活才开头**（`ContractController` 一个类就 40 端点、
  且它里面还有 `contract.setTenantId(1L)` 的硬编码租户，见 CRM 域条目）。

#### CRM-BREAK-02 [P0] `ContractController` 新建合同**硬编码 `tenant_id = 1`**，且异常口径错

- **证据（2026-09-21 读源发现）**：`backend/crm/.../contract/controller/ContractController.java`
  的 `create()` 里写死 `contract.setTenantId(1L);` —— **任何租户建合同都落进租户 1（系统租户）**。
  与 平台-BREAK-02（`insertFill` 无条件覆盖 tenantId）是同一类"跨租户写错位"，
  但这里是**控制器里手写常量**，`insertFill` 的修复救不了它。
- **同文件另一处**：`getById()` 用 `throw new RuntimeException("合同不存在")` —— 会被
  `GlobalExceptionHandler` 兜底成 **HTTP 500「系统异常，请稍后重试」**，把"单据不存在（404）"
  报成"服务故障"。本仓已有 `BusinessException.notFound(msg)`（→404），应改用它。
  （与刚修完的 E-07 是同一类"异常语义错位"，只是这次在业务代码里。）
- **影响**：跨租户数据污染（租户 2 建的合同出现在租户 1），且排查时会被 500 误导。
- **修法**：去掉硬编码，租户取当前会话（`SecurityUtils.getCurrentTenantId()` / `insertFill` 自动填充）；
  `RuntimeException` → `BusinessException.notFound`。**与该类的 `@SaCheckPermission` 补注解同批做**
  （它现在整类 40 端点零鉴权，见 MD-AUTHZ-01）。
- **状态**：🔄 **硬编码租户与异常口径已修（2026-09-21），鉴权注解仍待做**。实际改动比本条原描述更大——
  **同类硬编码在 crm 域共 7 处**（读源实测），已一次性收口：
  | 文件 | 原写法 | 现写法 |
  |---|---|---|
  | `ContractController#create` | `contract.setTenantId(1L)` | 删除，走 `insertFill` |
  | `QuotationController#create` | `quotation.setTenantId(1L)` | 删除 |
  | `QuotationTemplateController#create` | `template.setTenantId(1L)` | 删除 |
  | `MarketingCampaignController#create` | `campaign.setTenantId(1L)` | 删除 |
  | `CustomerPoolServiceImpl#putToPool` / `#returnToPool` | `pool.setTenantId(1L)` | 删除 |
  | `CustomerPoolMapper#countAvailable` | SQL 里写死 `AND tenant_id = #{tenantId}` + 调用方传 `1L` | 删条件与入参，交给租户拦截器 |
  **异常口径**：crm 域 4 处 `throw new RuntimeException("xx不存在")` 全部改 `BusinessException.notFound`
  （`ContractController:59`、`QuotationController:55`、`QuotationTemplateController:41`、
  `MarketingCampaignController:55`）。
  **验证**：`./mvnw -o -DskipTests -pl crm -am compile` **BUILD SUCCESS**。
  **仍待办 ①**：该类 40 端点的鉴权注解（见 MD-AUTHZ-01 / E-08）。
  **仍待办 ②（本轮有意不动）**：死代码里还剩 2 处同款硬编码租户 1，**均无任何调用方**（grep 实测）：
  `ContractServiceImpl#listByStatus`（`baseMapper.selectByStatus(status, 1L)`）与
  `MarketingCampaignServiceImpl#listByStatus`（同写法），对应 Mapper 是
  `WHERE ... AND tenant_id = #{tenantId}`。它们不是活缺陷，但一旦被调用就是「查别的租户」——
  建议随「死代码清理」批次**整方法删除**（属 平台-CLEAN / MD-DUP 同类），而不是改一改继续留着。

#### E-01 crm 批次 [P0] 144 个裸端点补注解 —— ✅ 已完成（2026-09-21）

- **做了什么**：crm 域 **10 个控制器 / 185 个端点**全部补上 `@SaCheckPermission`（共 **179 处**），
  裸控制器 **197 → 190 个**、裸端点 **1765 → 1621 处**；新增迁移
  `V11.458.0__Seed_Crm_Permissions.sql`（**补 36 个码，crm 码 57 → 93**），
  全部新码关联超管角色。
- **为什么不是简单 `--apply`**：生成器通用推导出的 69 个码与库中 57 个历史码**只交集 8 条** ——
  历史动作词是 `view`/`edit`（不是 `list`/`detail`/`update`），且通用规则的 POST 兜底是
  `create`，会把 `/{id}/sign`、`/{id}/terminate`、`/mark-expired` 全判成"新建合同"。
  故在生成器里给 crm 加了**显式映射**（新增 `resource_overrides` + `code_rules` 两套机制，
  按控制器作用域，避免 `/{id}/convert` 在 lead 与 quotation 之间错配），
  能复用历史码的一律复用（`export`→`download`/`downloadpdf`、`/convert`→`lead:convert`…），
  **只在历史词表确实没有时才建新码**。
- **零码资源**（跟进 / 公海池 / 营销活动 / 报价模板 / 拜访，库里一个码都没有）现场按
  `<域>:<资源>:<动作>` 建码；其中"状态/生命周期类 POST"统一归 `update` 而不是通用兜底的
  `create`（"能建活动的人才能取消活动"说不通）。
- **顺带清掉一处历史包袱**：crm 曾用 `CrmPermissions.require(code)` 做程序化校验（3 处，
  因为该子模块只依赖 core-base），**这类写法模块 entitlement 门看不见**。本轮 3 处全部
  改成 `@SaCheckPermission`，`CrmPermissions` 类已无引用、**已删除**。
- **验证（全绿）**：`verify-module-authz.cjs crm` **18/18**（新增 9 个资源探针，
  超管放行＝码在库、非超管 403＝注解生效）；`verify-crm-tenant-fix.cjs` **6/6**；
  回归 `verify-tenant-hardening` **14/14**、`verify-permission-changes` **23/23**、
  `verify-module-mapping` **43/43**、`verify-module-entitlement` **8/8**、
  `verify-module-authz budget` **14/14**。
- **⚠️ 后果（须知悉）**：补注解后 crm 的码**只在超管手上**，所以**租户角色必须被授予 crm 码
  才能用 CRM**（且按平台-AUTHZ-01「菜单从权限派生」的决议，未授权时 CRM 菜单也会消失）。
  测试夹具 `E2E_T2_ADMIN` 已补授 `crm:contract:create` / `crm:contract:view` 两条，
  否则 `verify-crm-tenant-fix.cjs` 会变 403（那是预期行为，不是脚本坏了）。
- **仍待办**：合同的 `terminate/cancel/effective/complete` 目前都归 `crm:contract:edit`
  （历史词表里没有这 4 个动作码）—— 若将来要按职责分离拆码，属独立裁定。

#### CRM-BREAK-03 [P0] 软删单据后再新建 ⇒ 撞唯一索引报 400（当天最后一张的号会被复用）

- **证据（2026-09-21 实机复现，非推测）**：`crm_contract` 上
  `uk_crm_contract_no UNIQUE(contract_no)` **不含 `deleted`**（真库 `pg_indexes` 已核），
  而 `ContractServiceImpl#generateContractNo()` 统计的是
  `likeRight(contractNo, 前缀).eq(deleted, 0).orderByDesc(...).last("LIMIT 1")`
  ⇒ **软删行不参与计数，但占着号**。把当天最后一张合同软删掉之后，
  下一次生成又拿回同一个号，插入直接撞唯一索引，前端看到的是
  **400「请求数据不完整或存在冲突」**（`DataIntegrityViolationException` 的兜底文案），
  而不是任何与"合同号冲突"有关的提示。实测：软删 `CT202609210001` 后新建即复现。
- **波及面**：crm 域共有 **12 个 `uk_crm_*` 唯一索引**都是「业务单号 UNIQUE、不含 deleted」
  （contract / quotation / quotation_template / lead / opportunity / customer /
  follow_up / campaign / content / channel / visit_plan / contract_change），
  且 `QuotationServiceImpl#generateQuotationNo`、`QuotationTemplateServiceImpl#generateTemplateCode`
  用的是**同一套"查最大号 +1"算法** ⇒ 同一缺陷同构存在，不是合同独有。
- **为什么容易踩**：列表页"删除"走的是软删（`deleted=1`），单据在界面上"没了"，
  但号码还在库里 —— 用户会认为是新单据没保存上。
- **修法（三选一，需拍板）**：
  ① `generateContractNo` 去掉 `.eq(deleted, 0)`（把软删行也算进序号）—— 一行改动，最小风险，
     代价是号段有洞但绝不重号；
  ② 唯一索引改为**部分唯一索引** `WHERE deleted = 0` —— 语义最正确，但 12 张表都要改，
     且**存量软删行会立刻产生同号冲突**，必须先清洗数据；
  ③ 单号改为「日期 + 随机/序列」不再"查最大+1" —— 改动最大，能顺带解决并发下的
     "两个请求算出同一个号"（现算法天然有竞态）。
- **状态**：⬜ **本轮只做到"定位 + 复现 + 不被它挡住"**（验证脚本
  `tools/verify-crm-tenant-fix.cjs` 因此改用**硬删除**清场，并在文件头写明了原因）。
  修法需要拍板，未擅自改。

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
- **状态**：🔄 **一半已完成（2026-09-21 实测）** ——
  · **marketing 侧 ✅ 基本收口**：E-01 那批已给 marketing 补 142 处注解，
    实测剩余裸控制器 **1 个 / 7 端点**（另有 E-04 已补的 103 条 `marketing:*` 码）。
  · **b2b / 商城侧 ⬜ 未做**：实测 `erp-mall` 仍有 **13 个裸控制器 / 113 端点**，
    其中 `MallAdminController`（40 端点，后台管理）**必须补**；
    C 端公开接口（`MallProductController` 等，走 `/api/v1/mall/**`）需按"确定公开"登记进基线，
    不许用基线当"懒得甄别"的筐。

---

### 3.11 HR 域

#### HR-BREAK-01 [P2] 部门/数据权限链路三处同时断

- `sys_user.dept_id` **0/75 非空**；`sys_dept` **0 行**（`DataScopeAspect`/`RbacService.isDescendantDept` 读它）；`sys_user_data_scope` 6 行**全部 `deleted=1`**（`target_ids` 都是 `9001`）。
- **影响**：按部门的数据行级隔离**完全未启用**；"按部门看数据"要么拿全量要么拿不到（越权风险）。
- **修法**：见 PUR-BREAK-02（同一件事，A-01 迁移顺序）。
- **✅ 已拍板（2026-09-21，用户）**：启用部门维度，先冻结不改代码（同 PUR-BREAK-02）。
- **状态**：🔄 冻结中

#### HR-AUTHZ-01 [P1] HR 模块 40 条僵尸权限码 —— ❌ **本条是误判，已关闭**

- 原记录：`E-02` 分布中 hr 占 40 条（集中在 crm 57 / hr 40 / finance 25 / system 25 / erp 23）。
- **❌ 该 40 条并非僵尸码，是工具误判**：`tools/gen-permission-effectivity.py` 的正则原先只认
  `@RequirePermission`（**少一个 s**），而本仓 hr 域真正在用的是 core-base 的
  `@RequiresPermission`（配 `PermissionAspect` 真实拦截，`HrController` 里就有 77 处）。
  正则一修，hr 域的"僵尸码"**归零**，E-02 的规模也从 159 缩到 110（后续批次清理到 21）。
- **实证**：E-02 闭环后，`GET /api/permission/effectivity` 的 `ineffective` 清单里
  **hr 域 0 条**（见 `verify-authz-batch45.cjs` 第⑦段钉住的 21 条）。
- **状态**：✅ **已关闭（2026-09-21，随 E-02 闭环；结论是"不存在这个工作"）**

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
- **状态**：✅ **已完成（2026-09-21 实测更正：此前标 ⬜ 是过期）** ——
  E-01 的「7 模块 / 97 控制器 / 709 端点」那批已包含 fixedasset(50) / budget(54) / payment(95)，
  注解由 `tools/gen-module-permission-seed.py --apply` 统一插入。
  **实测核对**：`tools/scan-unguarded-controllers.py` 里
  `erp-fixedasset` / `erp-budget` / `erp-payment` **裸控制器均为 0 个**。

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
| 1 | `tenant_id=0` 与「平台租户=1」谁是平台？0 号租户是真实租户吗 | F-01/03/07、平台-BREAK-03 | ✅ **2026-09-23 决议：`0 = 默认共享租户`（共享层，非真实租户）；`1 = 系统平台租户`（`sys_tenant` 里只有它，`SYSTEM`）** ⇒ 旧建议"0=平台全局 + 加 `is_global` 列"**作废**（不需要那个列）。详见 `docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md` **§6.5** |
| 2 | 普通租户用户该不该看见 `tenant_id=0` 的数据 | F-01/07 | ✅ **2026-09-23 随 #1 解决：看得见** —— `tenant_id=0` 就是"共享层"（平台字典/散客档案/平台默认值），"共享"的本义就是各租户可见。真正要防的是"看见别的**租户**的数据"，与 0 无关；把 0 一并挡掉属额外收紧，需单独论证（见 §6.5 推论③） |
| 3 | `system:*` 权限码能否授予租户角色 | F-05 | ✅ **不接受，且机制已存在**：平台专属能力走**独立码族 + 前缀归「系统」模块**（按 V11.455.0 只开给系统租户）。本轮 `agreement:platform:*`（含 `compliance:read`）就是这套的现成实现 ⇒ **照抄它，不再造第二套**（见 §6.5 推论②） |
| 4 | 「切换租户」的产品语义（切会话 vs 切视图） | F-10 | 切会话身份且仅超管可切 |
| 5 | `sys_tenant_menu` 的授权主体是平台还是租户管理员 | F-06/07 | 平台侧专属能力 |
| 6 | 文件访问是否接受"UUID 即权限" | F-08 | ✅ **2026-09-23 决议：不接受** ⇒ 存储路径加**租户层级**（并保留 UUID 作为不可猜的二级随机段） |
| 7 | 授权模型：菜单可见性由 `sys_role_menu` 还是从权限派生 | 平台-AUTHZ-01 | 从权限派生，消除双维护 | ✅ **2026-09-21 决议：从权限派生**（分阶段，依赖 E-04 补码） |
| 8 | 部门维度是否启用（决定 A-01 迁移时机） | PUR-BREAK-02、HR-BREAK-01 | 启用后再迁移 | ✅ **2026-09-21 决议：启用，但先冻结不改代码** |
| 9 | 库存双轨以哪一轨为准 | STK-BREAK-01 | 以 WMS 为准（有流水可回溯） | ✅ **2026-09-20 决议：以 `erp_stock` 为准**（本行建议已过期，以决议为准） |
| 10 | 退货是否"一申请一单" | SAL-BREAK-01 | ✅ **2026-09-23 决议：是**，并自动生成草稿退货单（申请与退货单一一对应，可追溯） |
| 11 | 采购合同保留 ERP 还是 CRM | PUR-BREAK-01 | 保留 CRM，删 ERP 菜单/页面/代码 | ✅ **2026-09-21 决议：保留 ERP —— 补建 `purchase_contract` 表 + 补 `/page` 接口**（与建议相反） |
| 12 | DMS 结算是否本期范围 | DMS-BREAK-02 | ✅ **2026-09-23 决议：在范围内**（本条**原建议已过期**——`dms/settlement` 早已实现：后端 `dms.settlement` 包 + 菜单 `80910 配送结算` + 二轮 E2E 87/87）⇒ 不再删菜单/表 |
| 13 | 财务口径确认：总账以凭证重算是否认可 | FIN-BREAK-01 | ✅ **2026-09-23 决议：认可**，并统一为「**凭证 → 总账**」**单向派生**（财务审计实测"总账≠凭证分录差 44,130"的根因就是两套口径并存 ⇒ 单向派生可一次性消除） |
| 14 | 应收/应付是否挂菜单（子分类账） | FIN-DUP-02 | ✅ **2026-09-23 决议：挂菜单**，与「查应收 / 查应付」**并存**（子分类账与查询页职责不同，不属重复页面） |
| 15 | 往来单位三页面是否收敛为一个页面三视图 | MD-DUP-04 | ✅ **2026-09-23 决议：收敛**；⚠️ 收敛**按 `docs/Yh-Spec` 里已有的收敛裁定**执行，**不按"谁有菜单谁在用"的直觉判**（本仓多份重复实现都有成文裁定，且常与直觉相反） |

**另需确认**：24 张无明细销售单 / 22 条悬空 `order_id` / DMS 24 条悬空 `source_bill_no` 是 E2E 造数还是代码缺陷（值形态都像测试数据，但**无法从只读数据判定**）。
- ✅ **2026-09-23 用户拍板：24 张无明细销售单按测试数据处理并「补齐明细」**。**执行时核实：对象已不复存在** ——
  · 「24 张无明细销售单」现在**只剩 1 张**：`erp_sale_order.ZZT-QO-CANCEL`（客户「验收测试客户」、`total_amount=0.00`、`status=6` 取消态），
    它本身就是**"取消"用例的造数**；给一张取消态零额单补明细行 = **凭空造假数据**，故**不做**（宁可留空也不编）。
  · 「22 条悬空 `order_id`」现在**销售/采购明细均为 0 条**。
  · 「DMS 24 条悬空 `source_bill_no`」**确认是 E2E 造数**（`dms_task.source_bill_no = XSCKD-E2E-01..24`），不是代码缺陷 ⇒ 无处置。
  ⇒ **三条一致结论：都是历史 E2E 造数，且大部分已被清理；不需要动数据。**（若你要的是"补一张给 `ZZT-QO-CANCEL`"，说一句我再补。）

---

## 4.1 本轮（2026-09-23）随拍板一并定下的小口径

| 事项 | 决议 | 依据 |
|---|---|---|
| 货到付款（COD）的到期日 | **取业务日**（不接"到货日"） | COD 是极短期在途敞口，账龄差异可忽略；接"到货日"要改销售出库+配送链路，收益不匹配。结算方式已随 `CreditTermResult#getSettlementType()` 带出，**不丢信息**，将来要收紧只需改 `dueDateFrom` 一处 |
| 档案的「固定账期日」（`fixed_payment_day`/`fixed_credit_day`） | **不接** | 文档没有"算哪个月、遇节假日怎么办"的口径，不臆造；等真有业务用再定 |
| 租户内档案写"非现结"却没填有效天数 | **不拒单**，落回现款现结 | 协议侧同类情形**拒单**是对的（那是双方约定，缺项=没谈完）；档案是**租户单方政策**且 `settlement_days` 默认 0，从严会大面积误伤 |
| 快照「统一写入工具」 | **不做**，等第一张新单据 | 规范 §11.6 已定；现在没有消费方，先造工具即过度设计 |
| `finance_tax_declaration` 有税额无税率 | 保留在快照缺口**棘轮**里 | 申报表口径待定 |
| `ComponentScanCoverageTest` 只校验控制器包 | ✅ **要补 mapper 包校验**（带棘轮基线） | 本轮"整仓起不来"的根因正是这个盲区（`@MapperScan` 只覆盖 `*.mapper`/`*.dao`，放错包的 mapper 即使有 `@Mapper` 也不注册） |

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
| 2026-09-21 | CRM-BREAK-02 | crm 域硬编码租户 **7 处**一次收口（4 个 Controller 的 `setTenantId(1L)`、`CustomerPoolServiceImpl` 2 处、`CustomerPoolMapper#countAvailable` 的 SQL 条件与入参）；异常口径 4 处 `RuntimeException` → `BusinessException.notFound`。新增 `tools/verify-crm-tenant-fix.cjs` | ✅ 编译 `BUILD SUCCESS`；**实机 4/4**：租户 2 建合同落库 `tenant_id = 2`（DB 直查两条探针行均 =2，探针行已 `deleted=1` 回收）、查不存在合同 → **404「合同不存在」**（修复前为 500「系统异常」） |
| 2026-09-21 | 平台-BREAK-02 | 补单测 `TenantInsertFillTest`（core-base，4 例） | ✅ **4/4**，且**反向验证**：改回无条件覆盖后 2 例失败并报 `expected: <7> but was: <9>` |
| 2026-09-21 | 平台-BREAK-01 | `shouldSkip()` 语义收紧（已登录无租户 → fail-closed）；`MallAuthServiceImpl#login`/`#refreshToken` 补写会话租户；同步订正 PDA / 打印端两处已过期的 fail-open 注释 | 编译 **BUILD SUCCESS**（core-base + erp-mall + wms + crm，6 模块）；**实机回归 3 套全绿**：`verify-tenant-hardening` **14/14**、`verify-permission-changes` **23/23**、`verify-module-mapping` **43/43**；未登录白名单链路（登录本身即跨租户查 `sys_user`）与游客商城入口均正常 |
| 2026-09-21 | ⚠️ 验证边界声明 | 平台-BREAK-01 的**拒绝分支（已登录却无租户）当前无法用 API 触达** —— 全仓 6 处 `StpUtil.login()` 已有 5 处写租户、第 6 处是死代码，即"漏洞入口"已不存在 | 该分支属**纵深防御**：价值在于将来新增的第 7 个登录入口漏写时**失败方向是"看不到"而不是"全租户可见"**。已用 37 项回归覆盖其两条放行分支，拒绝分支只有代码级论证 |
| 2026-09-21 | STK-BREAK-01 复核 | DB 实测两轨对齐：`erp_stock` 4 行 / `wms_inventory` 3 行，共同 key 全部相等（`0/0`、`152/152`、`95/95`，原 95 vs 43 漂移已消失） | ⚠️ 残留 1 行"ERP 有、WMS 无"：`product_id=2073239284579586050, warehouse_id=2, qty=0`（2026-09-10 起未再变动）。**零数量、无价值漂移**，但说明"ERP 侧可存在 WMS 无对应行的库存行"，对账脚本应按 key 而非按行数比对 |
| 2026-09-21 | **平台-MODULE-01 entitlement 门** | 新增 `SysModulePermission` 实体/Mapper、`ModuleEntitlementService`（码→模块 + 双缓存）、`ModuleEntitlementInterceptor`（order 3）、`WebMvcConfig` 注册；`IGNORE_TENANT_TABLES` 增 `sys_module_permission`；`TenantModuleService` 增 `getValidModuleCodesStrict()` | ✅ 编译 **BUILD SUCCESS**；新增 `tools/verify-module-entitlement.cjs` **8/8**：超管豁免 200、有码+模块开通 200、**停用模块后同一人同一接口 403「模块未开通：设置（settings）」**、恢复后回到 200（全程 `status` 复原核对 = 0） |
| 2026-09-21 | 模块门 · 顺序断言 | 用「租户 2 调 `/tenant/page`」做**唯一可证伪**的排序证据：该用户既无 `system:tenant:list` 码、租户也没开 `system` 模块 | ✅ 文案是 `无权限访问: system:tenant:list` 而**不是**「模块未开通」⇒ 证明模块门（order 3）确实排在 `@SaCheckPermission`（`SaInterceptor` order 1 内部）之后 |
| 2026-09-21 | 模块门 · 全量回归 | 后端重启（模块门是全局行为改动，必须回归） | ✅ 四套全绿：`verify-tenant-hardening` **14/14**、`verify-permission-changes` **23/23**、`verify-module-mapping` **43/43**、`verify-module-entitlement` **8/8** |
| 2026-09-21 | ⚠️ 模块门的已知缺口 | ① `analytics` 整域无权限码 ⇒ 该模块映射到 0 条码，门对它不生效（与映射脚本"断言为 0"一致）；② 码无归属前缀时**放行**（不是拦），否则任何新写的码会当场 403；③ 端口缓存 30s ⇒ 平台侧改开关后最迟 30s 生效 | ① ② 已写进 `ModuleEntitlementService` 类注释；③ 由 `evictTenant()` 兜底（供将来的开通/停用接口调用），跨实例场景需另接 Redis 失效广播 |
| 2026-09-21 | 模块门 · 覆盖与语义补正 | 实测两种权限注解 **`@SaCheckPermission` 1438 端点（默认 AND，已反编译确认）+ `@RequirePermission` 18 端点（默认 OR）**，语义相反故分别取值；OR 下"全不满足才拦"，否则会误伤 | ✅ 编译通过；仍 **8/8 + 14/14 + 23/23 + 43/43** 全绿。另记录两处例外：`CrmPermissions.require()` 程序化校验 3 处不被覆盖；`@RequirePermission` 走 AOP 故模块门会先于权限检查说话（两边都 403、都真实成立） |
| 2026-09-21 | **CRM-BREAK-03（本轮新发现）** | `uk_crm_contract_no UNIQUE(contract_no)` 不含 `deleted`，而 `generateContractNo()` 只数 `deleted=0` 的行 ⇒ **软删当天最后一张合同后再新建必然 400「请求数据不完整或存在冲突」**。crm 域同构的 `uk_crm_*` 唯一索引共 **12 个**，报价单/报价模板用的是同一套算法 | 实机复现（软删 `CT202609210001` 后新建即撞）；已在验证脚本改用硬删除清场并写明原因。**修法需拍板**，未擅自改 |
| 2026-09-21 | **E-01 crm 批次** | 给生成器加 `resource_overrides` + `code_rules`（按控制器作用域）；10 控制器 / 185 端点补 **179 处**注解；新增 `V11.458.0` 补 **36 个码**（57 → 93）；删除已无引用的 `CrmPermissions` 类（3 处程序化校验改为注解，该写法模块门看不见）；基线收缩 10 行 | ✅ 迁移 applied；`verify-module-authz crm` **18/18**、`verify-crm-tenant-fix` **6/6**；回归 14/23/43/8 全绿 |
| 2026-09-21 | **E-01 wms 批次** | 给生成器加 `base_overrides`（按类级路径定资源）+ `skip_bases`/`skip_methods`（安全阀）+ 一批状态类 POST 动作规则；20 控制器 / 162 端点补 **162 处**注解；新增 `V11.459.0` 补 **86 个码**（11 → 97）；基线收缩 wms 20 行 | ✅ 迁移 applied；`verify-module-authz wms` **22/22**；`AuthzAnnotationCoverageTest` **3/3**（含"基线不得过期"）；被排除端点行为未变（PDA 登录仍可达） |
| 2026-09-21 | **E-08 标尺更新** | 本轮两批合计关闭 **27 个控制器 / 306 个端点** | 裸控制器 **197 → 170**、裸端点 **1765 → 1459** |
| 2026-09-21 | **🔴 新发现（未修）** | `ErpCallbackController`（`/api/erp/wms/**`）+ `ErpIntegrationController`（`/api/wms/erp/**`）**不在白名单、且代码里没有任何验签**（`ErpCallbackServiceImpl` 全是 log + return ok），而内部 outbox 投递不带认证头 ⇒ **回调链路按代码推断是断的** | 实测未登录 → `401 请先登录`。**修法只能补服务间鉴权，不能加白名单**（加了 = 库存变更接口匿名可达）。需拍板 |
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
| 2026-09-21 | **E-02 批次 3 完成：product/md 19 条全部删码（迁移 `V11.451.0`）** | 逐条读源后定处置 —— **结论是"全删"而非"补注解"**：`product:*`（**无 `erp:` 前缀**）是 E-04 造出的平行命名空间，子实体控制器（attributes/category/brand/grade/units…）真在用，但顶层 8 条 CRUD 与 `product:barcodes:{list,export}`、`product:shield:{list,create}` 零消费方；真正把门的是 `erp:product:*` **8 条**（`ProductController`）。`md:product-price:*` 7 条的端点已被 `erp:product:list` / `erp:product:price-batch` 守着。删前核对：19 条均为叶子节点、仅超管持有、菜单零引用。 | ✅ effectivity 未生效 **94 → 75**（精确 -19）。迁移含三段自检（19 条必须下架 / `erp:product:*` 8 条必须仍在 / 无同码多行）。 |
| 2026-09-21 | **修工具：生效性扫描器三个同类盲点（先修工具再动手）** | ① **常量写法**：`@SaCheckPermission(PERM_VIEW)` 这类写法看不见 ⇒ 7 码误判；现按同文件常量表解析 + 未解析的显式列出。② **测试文件当消费方**：`__tests__/*.test.ts` 里的码是断言假数据，却让 `user:create/delete` 被判"生效"；现测试路径不计消费方并单独列出。③ **显式调用式校验**：crm 用 `CrmPermissions.require("...")`（模块依赖限制，见其类注释），注解式扫描看不见；现用保守模式收（不用宽模式，宁可漏认也不把无关调用当消费方）。 | ✅ 未生效 **75 → 68（-7，与预测一致）→ 70（+2，`user:create/delete` 现形）**；0 处未解析。三个盲点全部写进脚本注释。 |
| 2026-09-21 | **E-02 批次 4 完成：核心域 32 条删码 + 5 控制器 20 端点接线（迁移 `V11.452.0`）** | **删**：裸域重影 `permission:* / role:* / user:* / tenant:*`（16 条，正主是 `system:*`）+ 同族多余动作码 16 条。**接线（只用库里已有的码）**：`PartnerLedgerController` 3 读←`finance:partner-balance:view`；`ExpenseAnalyticsController` 3 读←`erp:expense:statistics:list`；`ExpenseApprovalController#/process`←`erp:expense:approval:process`（**前端按钮一直在查这个码、后端却没挂**）；`PurchasePriceTrackController` 6 端点←`purchase:price:edit`；`SalesPriceTrackController` 7 端点←`sale:price:edit`。<br>**有意保留 1 条**：`finance:other-income-doc:approve` —— 被「部门管理员」「系统管理员」真实持有（种子有意授权），删码会静默回收授权 ⇒ 留待拍板。 | ✅ 新增 `tools/verify-authz-batch45.cjs`（三向 + 三个缺陷钉子）。effectivity 未生效 **70 → 36**。 |
| 2026-09-21 | **E-02 批次 5 完成：9 条重影删码 + `SyncConfigController` 9 端点接线（迁移 `V11.453.0`）** | **删**：`data-permission:*` 8 条（整族重影，正主 `system:data-scope:*` —— `SysDataScopeController` + 前端 `RoleDataScopeTab.vue` 真在用）+ `tenant:config`（正主 `system:tenant:query`）。**接线**：`SyncConfigController`（`/api/v1/sync-config`）此前只有登录校验，而 `system:dataimport:{list,create,update,delete,test,sync}` **6 码早在库里**、`api_path` 回填的正是该路径 ⇒ "码在等接口"。<br>**剩 21 条不删**：20 条是「已定义未实现」的独立能力（不是重影），1 条待拍板；它们在矩阵里显示"未生效（标灰）"就是真实状态。 | ✅ effectivity 未生效 **36 → 21**；`GET /api/permission/effectivity` 与 DB 对账一致（`902 + 21 + 11 = 934`）。 |
| 2026-09-21 | **🔴 独立缺陷：删掉的权限码"重启就复活"（已修 + 实证）** | 首批删完重启后**24 条码全部复活**。根因在 `PermissionInitializationConfig#savePermissions`：按 `permission_code` 查一次、查到更新查不到插入，而 `SysPermission.deleted` 带 `@TableLogic` ⇒ 条件构造器被自动追加 `deleted = 0`，**看不见墓碑** ⇒ 「被有意删掉的码」被当成「从未种过的码」，每重启插一行新的；随后 `assignAllPermissionsToSuperAdmin()` 又把新活行全量授给超管，把清理整体撤销。**两层后果**：① 管理端在矩阵删掉的权限重启就回来（删除形同虚设）；② 库里同码两行。<br>**修法**：`SysPermissionMapper.countDeletedByCode`（`@Select` 绕开 @TableLogic）+ 初始化器查到墓碑即**跳过不复活**；并把已删的 24 条从初始化器静态清单里摘掉。 | ✅ **实证**：临时软删 `log:audit:stats` 后重启，日志出现"已被有意删除（存在墓碑行），初始化器跳过不复活"，活行 0 / 墓碑 1、全库同码双行 **0 组**；测试后原样恢复。 |
| 2026-09-21 | **🔴 独立缺陷：权限拒绝被吞成 500（已修）→ E-07** | 非超管打 `GET /api/user-permission/user/1/permissions` 拿到 **500「系统异常，请稍后重试」**，超管 200。根因：`PermissionDeniedException extends RuntimeException`，被 core-base `GlobalExceptionHandler` 的 `@ExceptionHandler(RuntimeException.class)` 兜底吞掉；两个 `@RestControllerAdvice` 都没写 `@Order`，兜底那个按注册顺序先命中 ⇒ 专治权限拒绝的 403 处理器根本没被调用。**权限不足与服务器故障对调用方无法区分**。<br>**修法**：异常改继承 `BusinessException(403)`（双保险）+ 处理器加 `@Order(HIGHEST_PRECEDENCE)` + 响应体统一成 `Result.fail(403, msg)`。 | ✅ 新增断言钉住：非超管 `→ 403`（不是 500）、超管 `非 403`，4/4 通过。 |
| 2026-09-21 | **🔴 E-08 新立标尺：裸端点盘点 202 控制器 / 1794 端点** | 新增 `tools/scan-unguarded-controllers.py`。实测**整类零权限注解**的控制器 **202 个 / 1794 处端点**；全局拦截器 `SaTokenConfig` 只有 `StpUtil.checkLogin()`，**不校验权限**。**实证**：租户 2 的 HR 用户（无任何财务/采购/销售码）打 `/erp/finance/expense-approval/pending`、`/erp/finance/expense-doc/page`、`/erp/finance/analytics/partner-balance/page`、`/purchase/price-track/page`、`/sales/price-track/page` **全部 200**（前两类是金额端点）。<br>结论：权限不是"漏了个别端点"，而是**整个功能面没设防**；E-01 的真实规模大于原估。 | ⬜ 待 E-01 分批施工（记分牌已就位；脚本只列不罪，公开接口/本租户只读类属豁免） |
| 2026-09-21 | **E-02 验收：批次 1/2/4/5 全量复跑** | `verify-authz-batch45.cjs` **53/53**（① 11 码在库且只被超管持有 ② 18 条超管探针全非 403 ③ 18 条非超管全 403 ④ 403 钉 4/4 ⑤ 60 条已删码不得复活 ⑥ 未生效清单必须正好 21 条）；批次 1 **32/32**、批次 2 **18/18** 无回归。新增 `tools/refs-of-codes.py`（删码前全仓引用核对：B 类 35 条的引用只落在种子 SQL / 文档 / 审计 JSON，无一处活跃代码）。 | ✅ 全绿；构建 `MODULES=core-base,core-platform,core-api,erp-finance,erp-purchase,erp-sales` clean install 成功，启动 137s |
| 2026-09-21 | **🔴 鉴权门禁（棘轮）自批次 1 起一直是红的（已修）** | `AuthzAnnotationCoverageTest#baselineMustNotBeStale` 的规则是"补了注解就必须从基线清单删行"。批次 1/2 接线后**基线没同步收缩**，实测 **7 行过期**（`crm.customer.controller.CustomerController` / `CustomerFollowUpController` / `CustomerOpportunityController` + `erp.expense.controller` 的 ExpenseApproval/Expense/ExpensePayment/ExpenseReimbursement），加上本轮新补的 `PurchasePriceTrackController` / `SalesPriceTrackController` 共 9 行。 | ✅ 删掉 9 行（并按文件维护规则补注释说明出处），`known-unauthorized-controllers.txt` **139 → 130** 行；`./mvnw -pl core/api/core-api test -Dtest=AuthzAnnotationCoverageTest,PointcutTargetExistenceTest` **4/4 通过**（控制器总数 394 / 端点 3533；无任何 `@SaCheck*` 130 个 = 33%，覆盖端点 1231）。<br>⚠️ 教训：**接线批次的收尾清单里必须有"跑门禁测试"这一步**，否则棘轮会腐烂成"永远豁免"名册 |
| 2026-09-21 | **A 方案完成：模块目录 6 → 13 + 建「模块→码」映射表（迁移 `V11.454.0`）** | 用户定稿的 13 模块落到数据上：`crm` 改名「**客户服务**」（售前+售后两阶段同属一个模块；业界同构 Salesforce Sales+Service Cloud / Zoho CRM+Desk / Odoo CRM+Helpdesk）、`warehouse` 语义收窄（只留 `stock:`+`wms:`，商品档案/往来单位划给新的「资料」）、新增 资料/交易/配送/人力资源/分析/设置(租户级)/系统(平台级)。<br>新建 **`sys_module_permission`**（模块 → 权限码前缀），**37 条前缀**覆盖 12 个有码模块；归属口径 = **最长前缀优先、同长取 sort 小**。<br>回填租户 1/2 的开通记录（口径：**现状不降级** —— 迁移不替业务做减法，先按"今天能用的全开"，再由平台职员按合同关）。 | ✅ 迁移 Flyway `success`；新增 `tools/verify-module-mapping.cjs` **35/35** —— 13 模块名逐条比对、无孤儿模块码、**923/923 在役码 100% 有归属**、无同长前缀歧义、`analytics` 映射数为 0（断言为 0 以防被塞假码）、两租户各 13 条开通记录。<br>⬜ 遗留：entitlement 门本身（`hasModuleAccess()` 仍是零调用方）、分析模块码族、系统模块开给谁的口径（见 平台-MODULE-01） |
| 2026-09-21 | **🔴 工具坑：`build-backend.sh` 在"无实例在跑"时静默 exit 1（已修）** | 脚本是 `set -euo pipefail`，而 `find_backend_pids` 末尾的 `grep -E '^[0-9]+$'` 在"没有实例"时无匹配返回 1 ⇒ pipeline 返 1 ⇒ 赋值语句触发 `set -e` ⇒ **立刻退出**；因为退出发生在任何 `echo` 之前，现场表现是**零输出 + exit 1**（极易被误判成"构建卡住/环境坏了"）。触发路径很普通：上一次启动失败（迁移报错）之后本来就没有实例，再跑构建就什么都看不到 —— 本轮连续踩了两次。已加 `\|\| true`，空结果会走到"==> 没有正在运行的后端实例"分支。 | ✅ 修复后连续三次构建重启均正常打印 |
| 2026-09-21 | **用户裁定：另立「租户管理」码族 `tenant-admin:`（迁移 `V11.456.0` + `V11.457.0`）** | 冲突：租户内"人/角色/权限"的码前缀都是 `system:`、按映射全归「系统」模块，而「系统」已收紧为只开给系统租户 ⇒ 业务租户再无码可管理自己的用户与角色；但用户口述又说"部门管理员权限由租户的系统管理员配置"。**裁定 ③：另立码族，归「设置」（租户级）模块。**<br>· 7 个子域从 `system:` 剥离：user / role / permission / data-scope / field-permission / record-rule / sod-rule<br>· 5 个原裸前缀子域并入同族：department / position / permission-template / role-inheritance / **data-scope**<br>· 合计 **72 条码**；映射表加 `tenant-admin:` → 「设置」，清掉「系统」里已迁走的 5 条前缀。<br>新增 `tools/rename-permission-prefix.py`（**从真库读码 → 整码精确匹配 → 跨后端注解/前端指令/种子一起改 → 复扫断言零残留**），实测 **225 处 / 24 文件**，改动是干净的 1:1 替换。 | ✅ 两个迁移 Flyway `success`；`verify-module-mapping.cjs` **43/43**；`verify-authz-batch45.cjs` **66/66**（新增第⑥段：7 个新码在库且旧码不存在 + 6 个端点「超管非 403 / 非超管 403」+ 一条正向对照 `tenant-admin:user:list` 被租户侧角色真实持有，故 `/user/page` 对非超管放行是**正确行为**）；**生效性数字一字未变**（`934 = 902 + 21 + 11`，后端 1615 处 / 前端 324 处）⇒ 证明改名左右对称、没有码丢失消费方。<br>⚠️ 脚本第一版的两个坑（CSS `position:relative` 误伤、`data-scope` 半搬）已修并写进脚本注释；后者是靠"在役码 100% 有归属"断言抓出来的 |
| 2026-09-21 | **用户裁定：「系统」模块只开给系统租户；「设置」是租户级管理设置模块（迁移 `V11.455.0`）** | 上个迁移（V11.454.0）为"不替业务做减法"把 13 个模块全量补给了两个租户，并在末尾登记了这个待裁定项。现按裁定收紧：**软删非系统租户的 `system` 开通记录**（回收的是开通记录，不是模块本身，可回滚），系统租户缺则补上。本迁移**只动 entitlement 名册，不动权限码、不动鉴权注解** —— `hasModuleAccess()` 仍是零调用方，故**不改变任何接口今天的可达性**。<br>🔴 **顺带暴露新问题（已登记，等裁定）**：租户内的用户/角色/权限管理码前缀都是 `system:`，按映射全归「系统」模块 ⇒ 收紧后**业务租户再无码可管理自己的用户与角色**（「设置」现有码不含 user/role/dept/position）；而用户口述又说"部门管理员权限由租户的系统管理员配置"。两者对不上，**不猜、不动映射表**，否则 entitlement 门一上租户 2 连配角色都做不到。 | ✅ Flyway `success`；`verify-module-mapping.cjs` **39/39**（新增第⑥⑦段钉住裁定：`system` 开通租户**恰好只有 1**、系统租户 13 个模块、业务租户 12 个、业务租户必须有「设置」、`sys_module` 里模块仍在、无悬空开通行） |
| 2026-09-21 | **bump version 0.3.21 → 0.3.22（用户指令"更新版本"）** | 版本号只存在于两处、必须同改：`backend/pom.xml` 的 `<revision>`（全 30 个模块经 `${revision}` 继承）+ `tools/build-backend.sh` 里写死的 jar 名（已重构成 `VERSION` 常量，并同步 powershell 匹配串与注释）。<br>**⚠️ 这一轮又踩出三个版本相关的坑，全部已修并写进脚本注释**：<br>① **bump 后必须做【全仓】构建**：版本一变，本地 Maven 仓库里那批 `xxx:0.3.21` 的 jar 全部对不上，只构建核心模块时 core-api 直接报 **20 个依赖 absent**（`core-agent/core-notification/crm/erp-*/hr-base/wms/dms-delivery`…）。正确顺序：先 `cd backend && ./mvnw clean install -DskipTests -B`（不写 `-pl`），再跑本脚本。<br>② **bump 前必须先停掉旧版本实例**：`stop_backend` 按 jar 名匹配进程，版本号一改就匹配不到正在跑的旧实例 ⇒ 端口仍被占、clean 删不掉旧 jar。<br>③ **并发跑两次构建会把 jar 从运行中实例的脚下换掉**：实测第一个实例启动到第 10 秒时报 `NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy`（jar 完整性检查却是 OK），而**该 exec jar 的时间戳正好是实例崩溃那一刻** —— 是并发的第二次 `clean install` 重写了它。表象极具误导性（像是"jar 坏了/依赖缺失"），实际是构建并发冲突。<br>④ **另发现环境冲突（非本仓问题，已如实记录）**：`tool-results/system-module/core-api-mine.jar` 有**另一个会话的副本实例在抢 5655 端口**（构建脚本头注释早就警告过"别动 tool-results 里其它会话的副本"）。故本轮重启期间出现过"端口被占/实例身份不清"的干扰，验证结论均以**确认 PID 属于本单位实例**的结果为准。 | ✅ 全仓构建 **BUILD SUCCESS（30 模块 / 7:24）**；0.3.22 产物实测能起（`logs/backend-20260921-215102.log`：`Starting AiReadyApplication v0.3.22` + 134.6s 起完）；bump 后四个验证脚本全绿：`verify-module-mapping` **43/43**、`verify-authz-batch45` **66/66**、`verify-authz-batch1` **32/32**、`verify-authz-batch2` **18/18** |

| 2026-09-22 | **三条战线集成验证（重启实测）** | `tools/build-backend.sh`（停服 → clean install 3 模块 → 启动 141.4s）。⚠️ 前置踩坑：直接跑全仓 `clean install` 会在 core-api 的 `clean` 阶段失败 —— **运行中的实例占着 `core-api-0.3.22-exec.jar`**，Windows 下删不掉（`.mvnw` 报 `Failed to delete ...exec.jar`）。必须走本脚本的先停服流程。另实测脚本**一次停掉了两个实例**（PID 19368 + 10456），印证 `find_backend_pids` 返回全部 PID 那条修正是必要的 | ✅ `V11.484.0` / `V11.485.0` 均 `success`；启动**零错误**；两个拦截器都挂上（数据权限位置 1、**记录规则位置 2 且在分页插件之前**，链路 5 插件） |
| 2026-09-22 | **平台-MODULE-01 遗留① 闭环**：模块开通/停用写入口 | 新增 `POST /api/tenant-module/assign`、`DELETE /api/tenant-module/remove`；`assignModule` 修「盲插第二行」（该表实测**无 (tenant_id,module_code) 唯一索引** ⇒ 盲插不报错、只静默重复）改为**查含墓碑行的主记录、命中即复活**；写成功后调 `ModuleEntitlementService.evictTenant()` ⇒ **停用立即生效**（不再等 30s 缓存）；`moduleCode` 必须存在于 `sys_module` 目录；`assertPlatformAdmin` 硬校验（不靠租户隔离豁免）；`remove(1,'system')` 被拒（V11.455.0 不变式）。新增 `tools/verify-module-assign.cjs` | ✅ **31/31**。关键断言：墓碑复活后**行数仍为 1 且主键不变**（正对原缺陷）；脚本**刻意不 sleep** ⇒ "改完立刻生效"本身就是被验证的行为；`remove(1,'system')` → 400 且拒绝后库中「系统」仍开通；探针行逐字段还原后可重复运行。**这道门从此"能开能关"** |
| 2026-09-22 | **平台-AUTHZ-01 / E-06 / F-07 闭环**：菜单可见性改从权限派生 | 新增 `MenuPermissionDeriver`（权限码按 `:` 边界展开成前缀集合 **U**，菜单只做两次集合查找，60s 缓存，读库失败 fail-open）；`SysMenuServiceImpl#getUserMegaMenus` 用派生结果替代 `sys_role_menu` 一级，超管早退逐字保留。**过渡口径**：`menu_code` 为空或不在 U 中 ⇒ **保持可见**（只有"存在对应权限码但用户不持有"才隐藏）。⚠️ 实测**真瓶颈不是 `sys_role_menu` 而是 `sys_tenant_menu`**：它只有 **4 行且 tenant_id 全是 0**，租户 1/2 都是 0 行 ⇒ 按"0 行 = 平台尚未配置 ⇒ 放行 + WARN"处理 | ✅ **33/33**。非超管菜单从 **空数组** 变为 193/295 节点；`e2e_hr_ta` 返回节点集与派生期望集**逐 id 相等**（多出 0、缺失 0）；无码菜单 193 个仍可见（防导航缩水）；有码未持有 75 个确实隐藏且**同菜单对超管可见**（证伪"全放开"）；超管 **14 顶级 / 410 节点与改动前一致**；`sys_tenant_menu`/`sys_role_menu` 行数未变 |
| 2026-09-22 | **E-05 闭环**：记录规则接上查询消费方 | 新增独立 `RecordRuleInterceptor` + `RecordRuleInterceptorConfig`（**插在分页插件之前**，与 `DataPermissionInterceptor` 并列而非合并 —— 理由：那条链字段固定按表 data_scope 驱动，而记录规则是任意字段 domain 且按 model 查另一张表，合入会让已有 10 用例兜底的类同时背两套口径，静默漏数据时无法二分定位）；`RecordRuleServiceImpl` 增 `buildReadDomainFilter` / `getEnabledRecordRuleModels`（租户键控 30s 缓存 + `AtomicBoolean` 防重入闸 + 增删改即时失效）。**三条血泪教训**（拦截器自递归 / `Map.of()` 不收 null / 注册顺序）逐条写进类注释并落到实现 | ✅ `RecordRuleSqlRewriteTest` **15/15**；合并既有用例 **25/25**（`DataPermissionSqlRewriteTest` 10/10 无回归）。**零行为变化已钉住**：`sys_record_rule` 0 行 ⇒ 不解析 SQL、不取会话、不注入，用例断言返回 null + `verify(ruleService, never()).buildReadDomainFilter(...)` |
| 2026-09-22 | 三条战线验收 + 全局回归 | 串行跑（共用账号并发会 sa-token 互踢，伪装成假失败）+ 门禁测试 | ✅ `verify-module-assign` **31/31**、`verify-menu-derivation` **33/33**、`verify-module-mapping` **43/43**、`verify-module-entitlement` **8/8**、`verify-tenant-hardening` **14/14**、`verify-permission-changes` **23/23**；`AuthzAnnotationCoverageTest` **3/3**（含棘轮 `baselineMustNotBeStale`）、`PointcutTargetExistenceTest` **1/1**。前端 `ai-ready-admin` vite build **通过**（模块授权开关页 + 权限模拟横幅改为显示用户名） |
| 2026-09-22 | **协议模块（阶段 A）代码完成，待重启验证** | **用户裁定「协议还是独立成模块」** ⇒ 新建独立 Maven 模块 `backend/agreement`（`cn.aiedge.agreement`），与 crm/wms/dms/hr 同构；四处装配线全接（根 pom `<modules>` + `dependencyManagement` + core-api 依赖 + `scanBasePackages`）。设计依据写入 `docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md` **§十二**（六条裁定 + 分期 + 与既有物边界）。4 张表（`agreement` / `agreement_version` / `agreement_term` / `agreement_term_option`，**刻意无 `default_option` 列**）；10 条码族；迁移 `V11.486.0`（建表）+ `V11.487.0`（模块名册 + 前缀映射 + 码族 + 4 行菜单）。**裁定⑥**（主档系统级 `tenant_id=0` + 进 `IGNORE_TENANT_TABLES`，可见性由两端 tenant 显式判定并**收敛到 `AgreementVisibility` 唯一一处**）—— 这是①+②的必然推论，漏了乙方就查不到协议 | 🔄 编译通过、单测 **31/31**（14 不变量 + 10 查询范围 + 7 状态机）、两迁移事务内 dry-run 通过（已回滚，库未改）。
**菜单挂载点（用户 2026-09-22 再裁定）**：「租户级的协议从**设置-协议契约**分组进，系统级的协议从**系统-协议契约**分组进；后端独立模块与前端分组并不矛盾」⇒ **不新建一级菜单**，5 行分挂两棵树（**模块=权益/代码边界 ≠ 菜单挂载位置=导航边界**）：
`61208` 设置(60012)→协议契约(目录,tenant-admin) · `81016` →协议列表(`agreement`) · `61308` 系统(60013)→协议契约(目录,**system-admin**) · `62506` →平台协议(`agreement/platform`) · `62507` →条款字典维护(`agreement/term-option/index`)。
⚠️ `client_type` 是硬隔离：`getAllMenusForSystemAdmin()` 查 `client_type IN ('tenant-admin','system-admin')` ⇒ 超管看两棵树、普通租户只看 tenant-admin 那棵 ⇒ 平台协议页有**两道独立的门**（树的分流 + `agreement:platform:*` 码归属）。目录层 menu_code 命名随树不同（设置 `mega:set:*` / 系统 `mega:sys:*`）。
**待办**：① 上述 5 行菜单**待用户批准后**才应用；② 重启跑 `tools/verify-agreement.cjs`（含"第三方租户看不到别人协议"这条防线）；③ `tools/verify-module-mapping.cjs` 的模块数断言已同步 **13→14**（协议是第 14 个模块） |

| 2026-09-22 | **协议模块落库 + 全量验证（用户批准后执行）** | `MODULES="agreement,core/base/core-base,core/platform/core-platform,core/api/core-api" tools/build-backend.sh`（reactor 含新模块，clean install 1:54 + 启动 137.9s）。⚠️ 前置踩坑：直接跑全仓 `clean install` 会在 core-api 的 `clean` 阶段失败 —— **运行中的实例占着 `core-api-0.3.22-exec.jar`**，Windows 删不掉；必须走本脚本的先停服流程 | ✅ **步 1-7 全通过**：① `V11.486.0~V11.489.0` 四条 `success`；② 启动 **0 ERROR**，记录规则拦截器位置 2（分页前）；③ 12 表 / 20 码全授超管 / 5 行菜单无租户串号 / 21 个设定字段 `consumer_point` 无空缺 / **0 个 `default*` 列**；④ `verify-agreement` **55/55**（含裁定⑥ 防线：第三方读 404 且**该行在库里确实存在** + 是有权端能读的反向对照）；⑤ 菜单检查与基线一致；⑥ `verify-module-mapping` **45/45**（新模块使断言 43→45）、`verify-module-entitlement` 8/8、`verify-tenant-hardening` 14/14、`verify-permission-changes` 23/23；⑦ 协议单测 **56/56**、门禁 `AuthzAnnotationCoverageTest`+`PointcutTargetExistenceTest` **4/4** |
| 2026-09-22 | 🔴 **验证脚本自身的一串缺陷（已修，值得记住）** | `verify-agreement.cjs` 此前**从未跑通过**（协议表当时还不存在，其 ⓪ 前置必然不成立）⇒ 首次真机执行暴露 4 处脚本缺陷：**① 雪花 id 过 `Number()` 静默截断**（`partyAId`/`partyBId` 复用同一主体被新增校验拒；更致命的是 `/api/role/2099000000000009000/permissions` —— 真实角色 id 是 `…9031`，后端抛「角色不存在」被兜底吞成 **500「系统异常」**，把"脚本传错 id"伪装成"服务故障"；**且还原会写入不存在的 permission_id，等于删掉租户 2 的真实权限**）；**② ⑤ 的"篡改"载荷本身非法**（给条款选了不属于该 term_code 的选项 ⇒ 被"选项不在字典"先拦下，等于**白验**了"已生效不可写"这条不变量）；**③ 空 id 直接拼 `WHERE id=`** ⇒ 底层 PSQL 栈掩盖真实失败；**④ 自清理只删内存里记得的 id** ⇒ 进程被 kill 留下的行永远清不掉，却报「残留=[]」 | ✅ 修后 **55/55 且连跑两次全绿**。四条通用教训：**(a) 本仓所有 id 一律字符串**（前端早有同名教训）；**(b) 验证"不变量"时必须用合法载荷**，否则拦下它的可能是另一个前置校验；**(c) 探针清理要按"可识别的前缀"扫库删**，不能只依赖内存；**(d) 脚本要能识别环境冲突**（sa-token `is-concurrent=false`，同账号并发登录会顶掉会话，把几十条断言整齐染成 401）——识别成环境问题而不是伪装成系统缺陷 |

| 2026-09-22 | **协议模块 · 内容层 + 成立过程 + 终止（两次落库，均已真机验证）** | **第 2 次落库**：`MODULES="agreement,core/base/core-base,core/platform/core-platform,core/api/core-api" tools/build-backend.sh`（97.4s 启动）。⚠️ 停服阶段一次停掉 **6 个**实例（PID 33408/50472/11088/24100/63220/52532）—— 之前几轮重启留了僵尸 JVM，印证"kill 全部匹配 PID"那条修正的必要性。**累计**：协议表 **15 张**、权限码 **26 条**、`agreement_version` 5 个协商/变更列 | ✅ **全绿**：① `V11.490.0`/`V11.491.0` success；② 启动 0 ERROR；③ 库内核对 3 表/26 码/6 新码授超管/**终止表 0 个"免责"语义列**（§13.7"终止≠免责"的**结构反证**在真库成立）+ 5 个必填要素 NOT NULL；④ `verify-agreement` **55/55**；⑤ 全局回归 **45/8/14/23**；⑥ 协议单测 **94/94** + 门禁 **4/4** |
| 2026-09-22 | **内容层要点**（DOMAIN-MODEL §13.1~13.3/13.9/13.10） | `agreement_setting`(+`_def` **21 项，每项都有 `consumer_point`**，迁移内断言"为空即回滚") / `agreement_narrative`（文字版，接口带 `autoExecutable=false`+`manualNotice`）/ `agreement_fulfillment_mode`（**集合可并存**）/ `agreement_template*`（4 表，PLATFORM/TENANT 两级 + **平台合规抽查读**）/ **`AgreementRuntime`**：`resolve(..., 业务时点)`，三态 `AGREED/UNDECLARED/UNDEFINED`，`require()` 未约定抛中文异常；**模板不是默认值**靠**反射断言**（Runtime 的字段/参数/返回类型不含任何 `*Template*`）保证 | ⚠️ **关键缺口（下一步的核心）**：`AgreementRuntime` **目前还没有下游消费方** —— 订单路由/发货/定价/结算都还没调它。即"字段设定版改变物流·钱流·货流"这条**尚未打通**，只是备好了入口。这正是用户强调的重点，属阶段 B（价格与额度 + 履约方式接线） |
| 2026-09-22 | **成立过程与终止要点**（§13.4~13.7） | 唯一送达 `agreement_invite`：token **五绑定**（目标主体/租户/版本/时效/一次性），只存 SHA-256 + `token_hint`/`invite_code` 供运维排查；转发的第三方一律只得「这份契约不是发给你的」；协商=一串 DRAFT 版本（`proposed_by_side/person` + `proposal_note` + 逐条 diff），**谈成前现行版本继续有效**；签署 `agreement_signature` = **自然人代表主体**（`authority_basis` NOT NULL，回答"谁签的、凭什么代表这家公司"）；终止 `agreement_termination` 五种来源 + 主张违约/异议留痕 | 🔄 **已知变通（子代理如实申报）**：① `sign` 先调既有 `confirm` 再落签署行，**未把"必须有签署行"设为 activate 硬门**（否则破坏既有 56 用例与真机脚本链路）；② 仓里**无二维码库**（`qr-scanner` 是 pda 扫码解析、不能生成）⇒ 只返回短链+邀请码，渲染交前端，未装新依赖；③ 未做到期提醒/自然到期定时登记；④ 平台清退**仅对"平台是其一端"的协议**可用 |
| 2026-09-22 | **协议前端整套界面（构建通过）** | 新增 8 个文件（内容层编辑 / 唯一送达面板 / 协商 diff / 签署 / 终止 / 发起向导 / 模板管理 / 受邀方打开）+ 改 `api/agreement.ts`、列表页、详情页 Tab、`dynamicRoutes.ts`。**未新增菜单行**（入口=列表页按钮 + 隐藏路由），故未触发"改 sys_menu 需批准" | ✅ `vite build` 通过（6492 模块 / 4m7s）。独立复核：前端用到的 agreement 码 **25 个全部 ⊆ 26**（无越界）；后端 `AgreementInviteToken.shortLink()` 返回 `/agreement/invite?token=xxx` 与前端注册的隐藏路由**逐字一致** |
| 2026-09-22 | **结算方式线 + 三层结算口径落地（DOMAIN-MODEL §13.3 补充口径 3 / §7.7 附）** | 迁移 `V11.492.0` 加字段字典 `SETTLEMENT_TYPE`（扁平五项 `CASH_PREPAY`/`CASH_SPOT`/`CASH_ON_DELIVERY`/`CREDIT`/`ROLLING`，**非必填**——"没约定结算方式"是合法状态）+ 五条自检；新增 `SettlementType`（协议侧与财务侧**共用一套编码**，`requiresCreditDays()` 写在枚举语义上）；`CreditTermResult` **三态→四态**（新增 `NO_CREDIT_TERM` = 本笔无账期，**正常业务结论**，到期日 = 业务日）；`AgreementCreditTermProvider` 改**两层判定**（先结算方式 → 再决定是否问天数；未约定/取值认不出一律 `NO_AGREEMENT`，绝不就此给双方记赊账）；`BusinessAccountingServiceImpl` **三层优先级链收敛到一处**（跨租户→协议 · 无协议→现款现结 · 租户内→`biz_party` 档案 · 都没有→现款现结）；新增 `PartySettlementProfile(+Mapper)` 把此前**零消费方**的 `biz_party.settlement_type`/`credit_days`/`payment_days` 接上；采购/销售 `PLATFORM_DEFAULT_CREDIT_DAYS = 30` **移除**（回退 = 业务日当天结清） | ✅ **真机全绿**：`V11.492.0` success；`verify-agreement` 55/55；`verify-credit-term` 按两层判定重写后全绿；单测 协议 107/107 · 财务 20/20 · 销售/采购记账接线各自全过。⚠️ **旧断言必须拆**：`verify-credit-term.cjs` 第 ⑤ 组原断言「未约定 ⇒ 留空 + 不阻断」已按新裁定**拆成 ⑤-A / ⑤-B** —— 拿旧断言判新行为，会把正确的拒单判成红的 |
| 2026-09-22 | 🔴 **"拒单"最容易假实现的一处（新增纪律）** | `BusinessAccountingServiceImpl.createReceivableFromBusiness` 的调用方**是 catch-and-continue**（源码注释就写着"记账失败不阻断单据"）⇒ **在记账方法里抛业务异常 = 拒单理由被吞掉 = 等于没拒**。落地方式：新增 `precheckReceivableDueDate`/`precheckPayableDueDate` **只读预检**，并在**销售出库 `complete` / 采购入库 `confirmWarehouse` 的记账 try/catch 之外、状态变更之前**调用（位置选在状态变更前，拒单不留痕迹）。**通用判据**：任何"必须拦住单据"的校验，先问一句「**这个出口的异常会不会被调用方吞掉**」 | ✅ 出库/入库两条主链路已接；验收按**两向**断言：提交被拒（HTTP 400）+ 库里**没有**该单据 |
| 2026-09-22 | **本轮已知取舍（不许当成已完成）** | ① **货到付款的到期日仍取业务日**（§13.3 第 2 层表格要求"到货日"，但记账链路拿不到到货日；结算方式已随结果带出，待链路补输入后在 `dueDateFrom` **一处**收紧）；② 档案口径只接 `settlement_type` + 天数，`fixed_payment_day`/`fixed_credit_day`（固定账期日）**未接**；③ 租户内档案写"非现结"却没填有效天数时**不拒单**，落回现款现结（档案是租户自己的政策，且 `settlement_days` 默认 0，从严会大面积误伤）；④ 协议正本的**平台合规抽查读权限**仍未做 | ⬜ ①②③ 待后续；④ 见 DOMAIN-MODEL §8.2 |
| 2026-09-22 | **`biz_party.settlement_type` 是历史两值编码（易混）** | 它在 `V9.14.0` 被从字符串改成**整数**：`0 = 现结`、非 0 = 有账期；**与协议字段字典的 `SETTLEMENT_TYPE`（五项字符串枚举）不是同一个东西**，同名不同义。档案口径的读法只有「0 ⇒ 无账期；非 0 ⇒ 看 `credit_days`(应收) / `payment_days`(应付)」 | ✅ 已在 `PartySettlementProfile` 类注释与映射器里写明，避免后人拿两套枚举互转 |
| 2026-09-22 | **单据快照规范定稿（DOMAIN-MODEL §11.6 · 阶段 2 / U2）** | 唯一"晚做就要全表回填"的规范，落在动任何单据表之前：① **与旧规「禁止冗余名称字段」的判据**（"这个值会不会随时间变、且历史单据要认当时的账"）+ 快照是**列举式白名单**（不是把主档搬过来）；② **必快照清单→规范列名**一律沿用既有惯例（`<role>_name`/`_code`/**`tax_no`**/`_level`+`_grade_code`/`_grade_name`/`contact_*`/**`receiver_*`**/`bank_*`/**`tax_rate`**/**`agreement_version_no`**），且**"对方档案的联系人"与"本单收货人"必须分开**；③ **表形态两种**（默认行内列；击穿 25 列红线才拆 `<bill>_partner_snapshot`，已有 2 例），**禁止拿 JSON 大字段兜"几个字段的快照"**；④ **业务日期**沿用域内语义名（不做全表改名）、≠ 凭证日期、别认错同表上的多个日期列；⑤ 生效/过账那一刻写一次、之后**只读**（改主数据/改协议不回写历史单）；⑥ **存量不回改** + 两张**只许缩不许涨**的清单（豁免 13 项 / 已知缺口 7 项）；⑦ **登记制**：`§11.6.8` 登记 **32 张单据主表**（`agreement` 刻意不在内 —— 协议是契约，时点是有效期区间、快照是整版 `snapshot_json`） | ✅ `tools/audit-snapshot-spec.cjs` 跑通 exit 0（R1~R4 + 棘轮：只有**清单之外**的新违规才失败）。**未做**：没给任何表**新增**快照列（按裁定存量不回改、新表立即执行）；**统一快照写入工具等第一张新单据落地时再做**（现在没有消费方） |
| 2026-09-22 | ⚠️ **写审计脚本撞到的两个"静默骗人"（值得记住）** | ① **`tools/sql.cjs` 硬上限 500 行且静默截断** —— 第一版"一次把全库 表→列 拉回来"，14146 行被截成前 500 行，只看到 **29 张表（真实 568）** 却报"覆盖率 100%"。对策：查询必须收窄（`GROUP BY table_name` / `VALUES` 收窄），且**命中上限直接抛错**。② **按"有 `_no` 列"自动判定"单据表" ⇒ 真库判出 80+ 张**，明细表/台账/缓存/日志全被卷进来（`wms_pick_detail`、`serial_status_cache`…）—— **一条 80% 误报的检查比没有检查更糟**（会训练所有人忽略它）。对策：受约束的表**由人显式登记**（登记制），机器只做判定 | ✅ 两条都已固化进 `audit-snapshot-spec.cjs` 的文件头与实现 |
| 2026-09-22 | **协议「平台合规抽查读」独立权限码（DOMAIN-MODEL §13.9 / §8.2 待办关闭）** | 迁移 `V11.493.0` 加独立码 **`agreement:platform:compliance:read`**（id 111147，已授超管；前缀 `agreement:platform:` 归「系统」模块 ⇒ 按 V11.455.0 **天然只有平台侧能拿**）；新开 `AgreementComplianceController`（`GET /api/agreement/platform/compliance/page` 与 `/{id}`）+ 两条 `@OperLog` 留痕。⚠️ **刻意不写进菜单**（改 `sys_menu` 需另行批准，且合规抽查是稽核动作不走业务导航）。放宽可见性的那一句写在 **`AgreementVisibility#applyPlatformCompliance`**（"唯一构造处"纪律不破），列表复用同一个 `applyTypeScope` | ✅ `verify-agreement` **55 → 61** 全绿：⑨ 组两向验（平台侧读到"自己不是任一端"的协议 → 200 且 id 对得上、列表口径一致；租户会话**有租户级码无平台码** → 403；同一会话走租户级端点仍 200 证明 403 只来自平台码）。回归 mapping 45/45 · entitlement 8/8 · tenant-hardening 14/14 · permission-changes 23/23 · credit-term 39/39 · 协议单测 107/107 · 鉴权门禁 4/4 |
| 2026-09-22 | ⚠️ **"平台能力"不要靠"把租户级码授给平台"来兜** | §13.9 落地前的现状是：协议所有读端点复用租户级 `agreement:view`，而可见性只放行两端 ⇒ 平台想读别人的协议，唯一办法是**把租户级码授给平台侧**——那等于把"读任意租户协议"的能力散给**所有租户管理员**，比缺功能更糟。**通用判据**：给平台开的能力，**必须是独立码族 + 独立端点**（前缀归「系统」模块天然隔离），并**留痕**；放宽可见性的那一句**只能写在可见性的唯一构造处** | ✅ 已按此落地，并在 `AgreementComplianceController` 的类注释里写明"分开写是显式声明，不是重复代码" |
| 2026-09-22 | **阶段 1 自然人侧收尾（R3）施工完成（DOMAIN-MODEL §11.3）** | ① **登录入店校验**（`MallAuthServiceImpl#login`）：先解析"在登哪家店"（会话租户 → 头 `X-Tenant-Id`，**不接受请求体**），再查 `shop_user_tenant`；四种状态分开说（没注册/待审核/已拒绝带理由/已解除），未知状态 fail-closed；**入店校验放在密码验证之后**（反了会泄露"本店有没有这个顾客"）。② **注册两分支**：没有 ⇒ 建系统顾客（`tenant_id=0`）+ 本店关联；已有 ⇒ **必须原密码自证**（防冒名绑定），通过后只补关联、`source=system_reuse`，且"已拒绝/已解除"的行**就地改**不重插（部分唯一索引）。③ **同意/拒绝/默认同意**按**本店** `reg_audit_required`（修掉原来 `LIMIT 1` 取全表第一条配置）。④ **审核改挂关联表**：后台 `pageUsers/approveUser/rejectUser` 走 `shop_user_tenant`，归属守卫统一为 `requireShopLink`（toggle/delete/update 一并接上） | ✅ `tools/verify-mall-shop-entry.cjs` **25/25**（两向）+ `MallAuthServiceImplTest` 7/7。回归：agreement 61/61 · credit-term 39/39 · mapping 45/45 · entitlement 8/8 · tenant-hardening 14/14 · permission-changes 23/23 |
| 2026-09-23 | **阶段 1 遗留项：启用/停用下沉到关联表** | 迁移 `V11.494.0` 给 `shop_user_tenant` 加 **`enabled`**（1 启用 / 0 停用，NOT NULL DEFAULT 1 + 四条自检）。**刻意不复用 `status`**：`status` 是**准入审核**（待审/正常/拒绝/解除），"停用"是**准入之后的运营动作**，两者正交（挤一列会让"这列在说什么"变模糊——本仓最贵的一类错）。**两个"停用"的分工写死**：`shop_user.status` = 平台级账号开关（封号，影响所有店）；`shop_user_tenant.enabled` = 本店开关；登录**两道都要过**。改到的消费方：入店校验（审核通过后再判一道）、后台 `toggleUserStatus`、`pageUsers` 的启用/停用过滤与回填（否则 A 店停用会在 B 店列表显示成停用） | ✅ `verify-mall-shop-entry` **25 → 32**：⑨ 组两向验（A 店停用 ⇒ A 店进不去、**B 店照常能进**；重新启用 ⇒ 立刻能进）+ 单测 7 → 9。回归：agreement 61/61 · credit-term 39/39 · mapping 45/45 · entitlement 8/8 · tenant-hardening 14/14 · permission-changes 23/23 |
| 2026-09-23 | 🔴 **并行会话下的两处环境级拦截（与本轮业务无关，但能让所有人验不了）** | ① **两个会话同时 `mvn clean` 会互删 `target/classes`** ⇒ 报"找不到符号: 类 PasswordEncryptor"这类**像真错的假编译失败**（源码明明同包）。判据：**错误集合每次都在变** + 系统里有 ≥2 个 maven 进程 ⇒ 等对方停手再构建。② **mapper 接口必须住在 `mapper`/`dao` 命名的包里**：本仓用**显式** `@MapperScan({"cn.aiedge.**.mapper","cn.aiedge.**.dao",…})`，而**存在显式 `@MapperScan` 时，MyBatis 自动扫 `@Mapper` 接口会退让** ⇒ 放在别的包里的 mapper 即使有 `@Mapper` 也**不注册成 Bean** ⇒ 依赖它的组件起不来 ⇒ **整个应用启动失败**（实测 `cn.aiedge.erp.purchase.replenishment.ReplenishmentProductMapper`） | ✅ 已在 `AiReadyApplication` 显式登记该包并写明理由（**这一行动了并行会话正在写的文件，理由是它让整仓起不来**）。⚠️ **`ComponentScanCoverageTest` 只校验控制器包、不校验 mapper 包 = 该门禁的盲区，待补** |
| 2026-09-22 | 🔴 **"表已就位但消费方没改"的三处实证（本仓典型缺陷形态）** | 阶段 1 一上手就连撞三处，**都不是新写的代码错**，而是"拆分做了、消费方没跟上"：**(a)** 后台顾客页按 `shop_user.tenant_id` 过滤，而顾客升系统级后该列**恒 0** ⇒ **列表永远空、审核永远 404**（静默，不报错）；**(b)** `shop_user_party_link` **无 `tenant_id` 列**却未登记进 `IGNORE_TENANT_TABLES` ⇒ 拦截器注入 `tenant_id` ⇒ SQL 报「字段不存在」⇒ **商城登录整条链路 500**（因为库里从来没有商城顾客，这条路**从未被走通**，所以一直没暴露）；**(c)** `identities.stream()...findFirst()` 在"未关联 `biz_party` 的顾客"（虚拟会员身份 `partyId=null`）上 `Optional.of(null)` ⇒ **每个新注册顾客的首次登录都 500**。**通用判据**：凡是"把某列语义搬走（升系统级 / 改挂关联表）"，必须**同时**盘一遍消费方：查询条件、归属守卫、以及"没有该列的表有没有登记进忽略清单" | ✅ 三处已修；(b) 在 `MyBatisPlusConfig` 加了带完整理由的登记注释。(c) 的教训：**空库跑不通的路，等于从没验证过** —— 本轮靠"真机 E2E 从零造数据"才暴露 |

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

---

## 6. 本轮（2026-09-23）拍板后的执行记录

| 事项 | 状态 | 证据 / 说明 |
|---|---|---|
| 租户编号口径（§4 第 1/2 条） | ✅ 已落地 | `docs/DOMAIN-MODEL-USER-PARTY-TENANT-v1.md` **§6.5** + 变更记录 v3.22：`0 = 默认共享租户`（共享层哨兵，`sys_tenant` 无该行）、`1 = 系统平台租户`（`SYSTEM`）。旧建议"0=平台全局 + `is_global` 列"**作废** |
| 平台专属码机制（§4 第 3 条） | ✅ 直接复用现成实现 | `agreement:platform:*`（前缀归「系统」模块 ⇒ 按 V11.455.0 只开给系统租户）；后续平台能力照抄，不造第二套 |
| 文件访问"UUID 即权限"（§4 第 6 条） | ✅ 决议：不接受 | 存储路径加**租户层级**（实施项，登记在此） |
| 退货一申请一单（§4 第 10 条） | ✅ 决议：是 + 自动生成草稿退货单 | 实施项 |
| DMS 结算范围（§4 第 12 条） | ✅ 决议：在范围内（原建议已过期） | `dms.settlement` 包 + 菜单 `80910` + 二轮 E2E 87/87 |
| 总账以凭证重算（§4 第 13 条） | ✅ 决议：认可 + 统一"凭证→总账"单向派生 | 与财务审计"总账≠凭证分录差 44,130"配套 |
| 应收/应付挂菜单（§4 第 14 条） | ✅ 决议：挂菜单（与查询页并存） | 实施项 |
| 往来单位三页面收敛（§4 第 15 条） | ✅ 决议：收敛 | ⚠️ 按 `docs/Yh-Spec` 的收敛裁定执行，不按"谁有菜单谁在用"判 |
| 三条口径小项 | ✅ 已定 | 货到付款取业务日 · 固定账期日不接 · 档案缺天数不拒单（见 §4.1） |
| **装配门禁补 mapper 包校验** | ✅ **已完成** | `ComponentScanCoverageTest` 新增 `allMapperInterfacesAreMapperScanned`：**从 `@MapperScan` 注解本身读**包模式（不写死副本，否则门禁会变装饰），并修掉扫描器"跳过接口"的缺陷（父类默认要求 `isConcrete`，控制器都是具体类所以过去没暴露）。**首次运行即查出真违规**：`cn.aiedge.storage.permission.FilePermissionMapper`（同模块的 `storage.mapper.FileInfoMapper` 能注册、它不能 ⇒ 将来谁把 storage 接进 `scanBasePackages`，`FilePermissionService` 立刻起不来）。**已按门禁给的首选修法移包**到 `cn.aiedge.storage.mapper`。门禁 4/4 绿 |
| 无明细销售单 / 悬空 order_id / DMS 悬空 source_bill_no | ✅ 已核实（见 §4「另需确认」） | 24→**1**（取消态零额，不补）· 22→**0** · DMS 24 条 = `XSCKD-E2E-*` **E2E 造数** |

---

## 7. 阶段 3+5 施工方案（2026-09-23，D1(c) 产出）

**方案文档**：`docs/PHASE-3-5-MIGRATION-PLAN-v1.md`（⬜ 待评审，通过后才开工）

三条必须先知道的实测结论：
1. **`biz_party` 未删只有 19 行**（总 152 含 133 行软删残留）—— "存量 152 行"是含软删的计数；
2. 🔴 **`unified_code` 152 行全为空** ⇒ **唯一合法识别键（统一社会信用代码）在存量里根本没被填过**，
   而按名称归并是裁定④**明令禁止**的 ⇒ **"自动归并即可"（裁定②的前提）当前不可执行**；
   方案给三条出路，推荐 **I：先一照一档地搬（19→19），归并留给 `merge_request` 流程** —— 请拍板；
3. **引用面 100 张表**（含 `customer_id`/`supplier_id` 的 64 张），且 `partner_id`（25 张）**未必指向 `biz_party.id`**
   ⇒ **阶段 3 的第一步是"引用面判定"（出 `refsurface.csv`），不是建表**；按列名猜引用关系会改错数据且不可逆。

其它要点：并存期取 **B（新增表 + 双写 + 读走视图）**；**不可逆操作（删列/删表/删归并行）一律单独一次发布、排在观察期之后**；
阶段 5 加主体列时**必须同批加快照列**（64 张表，拆两次就是二次回填）。

**第 0 步已执行（2026-09-23）**：`tools/refsurface.cjs` → `tools/refsurface.csv`（候选 111 处，两列证据留空待填）。
结果：数据侧确认指向 `biz_party` **14 处**；**3 处同时命中 `biz_party`/`sys_user`（不可区分）**；
**2 处实际指向 `biz_party_contact`**（`erp_purchase_price_track.partner_id`、`finance_payable.supplier_id`）；
✅ **`erp_purchase_inbound.supplier_id` 命中 `biz_party` = 0 已查清：不是代码缺陷，是 dev 库采购单据"用假 id 造"（实测 `2099000000000000901`/`100`，且无名称快照）** ⇒ **数据侧 0 命中 ≠ 代码错**；连带记下：应付侧"三层结算口径按 `biz_party.id` 取档案"那条链路**用现有数据验不了**，要验必须先造"供应商指向真实 `biz_party` 行"的入库单（验收夹具问题）。

**序 1 裁定与执行（2026-09-23）**：用户选 **I（先不归并，一照一档）**，并授权「当前是平台测试的模拟数据，
直接把信用代码模拟上」⇒ 已执行 `tools/seed-mock-unified-code.cjs`，给 **19 行**未删 `biz_party` 补
**模拟**统一社会信用代码（GB 32100 格式合法 + 前缀 `91999999FAKE` 一眼可辨 + 幂等可 `--revert`），
回查 **19/19 有值、19 个不同值、全表无重复**。⚠️ 每行代码不同 ⇒ **本步不产生归并**（这就是 I 的形态）。

**序 2 第一批已执行（2026-09-23）**：`V11.497.0` 建 `party`（共享层，`tenant_id` 恒 0）+ `party_tenant`（R2 有向边）
+ 两个唯一索引 + 一照一档回填（19 行 → 19 档 + **18 条边**：16 SALE / 2 PURCHASE）。
⚠️ **方向不猜**：`party_type=3`（承运商＝第三方服务主体）那 1 行**只进 party、不建边**，
自检里显式断言"在 party 里但不在 party_tenant 里"以证明是"不猜"而非漏搬。
同批 `party` 进 `IGNORE_TENANT_TABLES`（**否则租户会话一件都读不到**，与 `shop_user` 同形态）、`party_tenant` 不进。
迁移在事务内跑通后回滚验证（DDL + 回填 + 5 条自检全过、**无残留**），**待下次重启由 Flyway 正式应用**。
