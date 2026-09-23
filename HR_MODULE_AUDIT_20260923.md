# 人力资源模块 · 全栈审计报告（2026-09-23）

> 范围：前端 `views/hr/**`（9 页）+ `views/md/staff-dept` + 被 HR 菜单引用的 `views/system/{role,user}`；
> 后端 `backend/hr/hr-base`（52 个 java / 6840 行）；数据库 13 张 `hr_*` 表 + `sys_menu` 人力资源子树（17 条）+ `sys_permission`（40 条 HR 权限码）；
> 对标依据：`docs/Yh-Spec/手动整理对标开发文档/人力资源模块/`（11 篇）与 `.../系统菜单设计与管理/`（5 篇）。
>
> **本次审计为只读**：未改动任何业务代码、未改动任何库表数据。新增的只有审计脚本与产物（见 §7）。

---

## 0. 结论摘要

| 维度 | 结论 |
|---|---|
| 菜单连通性 | ✅ **0 缺陷**（10 个叶子的 component 全部解析到真实 `.vue`） |
| 接口接线 | ✅ **0 断链**（前端 93 处调用 ↔ 后端 92 个端点全对得上；92 个端点 0 个无鉴权注解） |
| 数据层隔离 | ✅ 13 张 `hr_*` 表都有 `tenant_id` + `deleted`，且都不在 `IGNORE_TENANT_TABLES` 内 → 租户隔离/逻辑删除是**真生效**的 |
| 实体 ↔ 表 | ✅ 14 个实体逐列对账，**零缺列** |
| 桩代码 | ✅ 10 个页面**无桩**（35 处 `console.*` 全是错误日志，无 mock/假数据/TODO） |
| **非超管可用性** | 🔴 **完全不可用**（P0-1 + P0-2：菜单渲染不出来，接口又全 403）→ **已修**，见 §6.5 |
| **多租户可用性** | 🔴 非 1 号租户**建档必失败**（P0-3 号段未初始化 + 工号唯一约束不含租户）→ **已修**，见 §6.5 |
| 行级数据权限 | 🔴 **零生效**（`@DataPermission` 全仓 0 处引用、`sys_data_scope` 0 行）—— 全站性问题，未修 |
| 死代码 / 冗余 | ⚠️ 1 个死页面（`hr/attendance/index.vue`，且内含写死 `employeeId=0` 的打卡）+ 1 个失去入口的 1601 行页面 + 5 个死方法 + 20 个无页面调用的前端 API 封装 + 3 个孤儿按钮菜单 + 1 个空目录 |
| 能力已建界面未接 | ⚠️ 8 个 `/stat` 端点、合同到期提醒、`hr_employee.user_id` 全无前端入口 |
| 与其它模块的关系 | ✅ 招聘→入职→档案、请假↔考勤、绩效→薪资、考勤→薪资、编制联动、部门删除引用校验**均已打通**；❌ 薪资→财务、请假→工作流仍未接线 |

**P0 三条、P1 六条、P2 五条**，逐条见下。

> 📌 **本报告已按用户 2026-09-23「按你的推荐执行」落地了其中的 10 条**（P0×3、P1×3、P2×2），
> 逐条落点与验证证据见 **§6.5**；未执行项与理由也列在同一节。

---

## 1. P0

### P0-1 🔴 非超管用户看不到任何业务菜单（系统级缺陷，HR 全部 10 页命中）

**运行时实测证据**（`node tools/audit-hr-menu-visibility.cjs`，账号 `e2e_hr_ta` = 租户 1 非超管 / 角色 SYSTEM_ADMIN / 222 条权限码）：

```
后端 /menu/user/mega/tenant-admin?tenantId=1 返回顶层菜单数=13
全站抽样：后端下发叶子菜单 190 个，其中前端全等匹配不中 190 个   ← 190/190 全部不命中
人力资源相关：后端下发 4 个叶子（职员部门/岗位权限/全部操作员/招聘管理），前端判定 4 个全隐藏
```

**根因：前后端两套判定口径不同源**

| 侧 | 位置 | 规则 |
|---|---|---|
| 后端 | `MenuPermissionDeriver.toMenuCodePrefixes` | **前缀匹配**：权限码 `hr:employee:list` 展开出 `hr:employee`、`hr`；菜单码落在集合里即命中 |
| 前端 | `stores/user.ts:205 hasPermission` | **全等匹配**：`this.permissions.includes(menuCode)` |

后端 `permissions` 来自 `/auth/userinfo` → `StpUtil.getPermissionList()`，里面**只有权限码、没有菜单码**（已核全仓无任何位置往 `permissions` 里塞菜单码）。

**规模**（devdb 实测）：`menu_type=1` 且启用的菜单 **306 条**，其 `menu_code` **恰好等于**某条启用权限码的只有 **4 条**（且全是平台侧）：

```
62101 模块列表       menu_code=system:module:list
62001 租户列表       menu_code=system:tenant:list
62002 租户审批       menu_code=system:tenant:approve
62506 平台协议       menu_code=agreement:view
```

→ **其余 302 条叶子菜单对任何非超管用户都不会渲染**。因为超管 `permissions=['*']`（`hasPermission` 里有 `'*'` 短路），这个缺陷被长期掩盖
（devdb 实测：`sys_user` 有效 73 行，其中 **42 行**挂了 `SUPER_ADMIN` 角色，占 57%）。

**症状**：桌面端悬停「人力资源」→ 面板只显示 6 个列标题（员工管理/考勤管理/薪资管理/组织管理/职员管理/招聘管理），**每列下面 0 个条目**；只能靠顶部全局搜索 / 直连 URL / 折叠态以外的移动端抽屉进入。

**建议修法**：
1. 前端 `hasPermission` 改为与后端同口径的**前缀匹配**（或改为「menuCode ∈ 权限码前缀集合」的一次性判定）；
2. 同类失效口径还有 `utils/permission.ts:99 canAccessMenu` 与 `composables/usePermission.ts:166 canAccessMenu` —— 它们查的是 `menu:${menuCode}`（如 `menu:hr:employee`），而库里**没有任何 `menu:` 前缀的权限码**，同样是恒 false；
3. 顺带把「菜单可见性」收敛成**单一函数**（现在有三处各写一遍，改一处必漏两处）。

---

### P0-2 🔴 HR 权限码只授给超管 → 非超管调 HR 接口全 403

`sys_role_permission` 实测：

| 角色 | 租户 | 持有的 `hr:*` 权限码 |
|---|---|---|
| SUPER_ADMIN（角色 1） | 1 | **40 / 40** |
| SYSTEM_ADMIN | 1 | 0 |
| DEPT_ADMIN | 1 | 0 |
| ROLE_MQD2X2DI | 1 | 0 |
| E2E_T2_ADMIN | 2 | 0 |

**双重后果**：
- 后端 `MenuPermissionDeriver` 按前缀过滤 → SYSTEM_ADMIN **连 员工/考勤/请假/薪资/绩效/岗位编制 6 个菜单都收不到**（实测：那 4 个分组下 0 个叶子）；
- 即使补了权限，`@RequiresPermission("hr:xxx")` 也会把接口全判 403。

**这是与仓储/财务审计同型的复核结论**：E2E 用超管跑 → `SUPER_ADMIN` 走 `*` 通配 → 全绿，但真实租户管理员一条都进不去。**HR E2E 171/171 的「绿」不能作为非超管可用性的证据。**

**建议**：新增 `tools/grant-hr-permissions.py`（对标 `tools/grant-storage-permissions.py`），系统管理员授全部 40 条、部门管理员授只读子集（`*:list` + `hr:leave:create` 等）。**注意**：`hr:salary:*` 属薪资保密数据，不应无脑全授。

---

### P0-3 🔴 多租户下 HR 建档链路直接失败

**① 号段未初始化 → 必抛异常**

`biz_number_sequence` 实测：**全表 72 行、45 个 biz_type，`tenant_id` 只有 1**。
`BizNumberGeneratorService.nextNumber(bizType, tenantId)` 查不到行就抛
`IllegalArgumentException("未配置编号序列: bizType=EMP, ..., tenantId=2，请先在 biz_number_sequence 表中初始化")`。

→ 非 1 号租户「新增员工（EMP）/ 新增合同（HT）/ 新增岗位（HRPOS）」直接失败。全仓 Java **没有任何位置**在租户创建时初始化号段（`grep -rn biz_number_sequence backend/**/*.java` 只命中 Mapper 自身）。

> 这是**全站号段体系的通病**（XSDD/CGDD 等同样只有租户 1），HR 只是其中之一；但对 HR 的后果最硬：员工建档是模块的入口动作。

**② 工号唯一约束不含租户 → 即使补了号段也会跨租户撞**

```
hr_employee_employee_no_key  UNIQUE (employee_no)     ← 已实测确认不含 tenant_id
```

而号段是 **per-tenant** 的（`selectForUpdateWithLocale` 带 `tenant_id = #{tenantId}`），按天重置、前缀同为 `EMP`
→ 两个租户**同一天**各建第一个员工都会生成 `EMP-YYYYMMDD-0001` → **撞唯一约束报 400**。

**建议**：`uk` 改 `UNIQUE (tenant_id, employee_no)`（现有 0 行数据，改约束零风险）；号段改为「查不到则按默认前缀/长度 upsert 一行」而不是抛异常，或随租户创建初始化。

---

## 2. P1

### P1-1 ⚠️ 8 个统计端点零前端消费

后端 `/stat` 端点 8 个（position / employee / attendance / leave / salary / performance / recruitment / candidate），
权限码齐备、E2E 有断言，但：

```
grep -rn "\.stat(" views/hr views/md/staff-dept | wc -l   →  0
```

**10 个页面里 0 处调用**（`tools/audit-hr-frontend-api.py` 也把 8 个 `xxxApi.stat` 全部标为无页面调用）。

《人力资源模块 README》§8.3 把「统计 / 导出端点」列为已交付能力 —— 实际是**后端已建、界面未接**，文档口径需订正为「后端能力就绪，前端未展示」。

---

### P1-2 ⚠️ 5 个 `listForExport` 是无调用方的死方法

| 方法 | 调用方 |
|---|---|
| `HrRecruitmentService.listForExport` | ✅ `HrRecruitmentController:66`（`GET /hr/recruitment/list`） |
| `HrAttendanceService.listForExport` | ❌ 0 |
| `HrEmployeeService.listForExport` | ❌ 0 |
| `HrLeaveRequestService.listForExport` | ❌ 0 |
| `HrPerformanceService.listForExport` | ❌ 0 |
| `HrSalaryPaymentService.listForExport` | ❌ 0 |

原因：**HR 的导出全部在浏览器侧实现**（用 `xxxApi.page({pageNum:1, pageSize:10000})` 拉全量、前端拼 CSV 加 BOM），
见 `views/hr/employee/list.vue:1562`、`views/hr/attendance/list.vue:859`、`views/hr/leave/list.vue:1100` 等。
即 HR **没有任何后端导出端点**，这 5 个方法是从未接线的残留。

另两个死方法：
- `HrLookupHelper.fillEmployeeInfo`（152 行文件里的泛型助手，**0 调用方**）；
- `HrEmployeeService.getByEmployeeNo`（**0 调用方**，实现里还带 `last("limit 1")`）。

---

### P1-3 ⚠️ 死页面 / 僵尸路由键 / 目录污染

| 对象 | 状态 | 证据 |
|---|---|---|
| `views/hr/attendance/index.vue`（137 行） | **不可达** | `componentMap['hr/attendance/index']` 有键，但全库没有任何菜单的 `component`/`list_path` 能解析到它（菜单 90002 指向 `list.vue`） |
| 同一文件内的打卡按钮 | 🔴 **脏数据写入** | `handleClockIn()` 调 `hrAttendanceApi.clockIn(0)` —— **employeeId 写死为 0**；一旦走到就会给「员工 0」建一条考勤记录并提示"签到成功" |
| `views/system/position/index.vue`（1601 行） | **失去入口** | `V11.422.0` 把 80531 指过来，`V11.429.0` 又改指角色页 → 该页只剩 `componentMap['system/position/index']` 僵尸键 + 一个单测引用；V11.422.0 注释里「该页保留」的口径已过期 |
| `views/system/department/` | **空目录** | 文档 §2.4 称该页 1138 行「无菜单不可达」，实际文件**已被删除**，目录（0 个文件）与文档记录都需清理 |

---

### P1-4 ⚠️ 3 个按钮型菜单指向已删除的权限码

```
9071 新增  menu_type=3  menu_code=hr-recruitment:add
9072 编辑  menu_type=3  menu_code=hr-recruitment:edit
9073 删除  menu_type=3  menu_code=hr-recruitment:delete
```

这三条权限码已由 `V11.435.0__Remove_Legacy_Permission_Codes.sql` 从 `sys_permission` **删除**
（`SELECT ... WHERE permission_code LIKE 'hr%' AND NOT LIKE 'hr:%'` → 0 行）→ 三个按钮菜单成了孤儿配置。

**同时暴露一个命名断裂**：菜单 907 的 `menu_code = 'hr-recruitment'`（**连字符**），而权限码是 `hr:recruitment:*`（**冒号**）。
两端派生都失效：
- 后端 `MenuPermissionDeriver`：`hr-recruitment` 不在权限码前缀集合里 → 走 fail-open，**对该用户恒可见**（哪怕没有 `hr:recruitment:list`）；
- 前端全等匹配：又恒不中 → 见 P0-1。

**建议**：`menu_code` 改 `hr:recruitment`（`menu_code` 不是权限码前缀映射的来源，改它不断授权；但要同步 `MenuPermissionDeriver` 的大概率不变验证），并清掉 9071–9073 或改指真实权限码。

---

### P1-5 🔴 无行级数据权限（薪资/员工全租户可见）

```
@DataScope 注解：已于 2026-09-20 整链删除（MyBatisPlusConfig:315 注释）
@DataPermission 注解：全仓 0 处使用（grep 命中的 5 处全是注释/定义/测试说明）
sys_data_scope（表级自动模式的配置源）：0 行
sys_user_data_scope（ql361 7 类数据权限）：6 行，且 deleted 全为 1（测试残留）
```

→ **行级数据权限在全站（含 HR）零生效**。任何拿到 `hr:salary:list` 的用户可拉**全租户**员工工资；
`hr:employee:list` 同理。页面侧的 `applyDeptScope` 只在**显式传 `deptId`** 时生效 —— 那是筛选，不是权限。

**行业标准对照**：SAP HCM / 用友 / 金蝶的薪资与员工档案都要求「部门主管仅本部门、本人仅本人」；
ql361 也把「全部操作员」的 7 类数据权限（`sys_user_data_scope` 已建但**无任何业务侧消费方**）做成一等公民。
本模块在这条上明显低于业界基线。

---

### P1-6 ⚠️ 权限码粒度与动作语义不齐（矩阵列会失真）

| 端点 | 实际动作 | 所挂权限码 |
|---|---|---|
| `DELETE /salary/payment/{id}` | **删除** | `hr:salary:update` |
| `DELETE /salary/structure/{id}` | **删除** | `hr:salary:update` |
| `DELETE /performance/{id}` | **删除** | `hr:performance:update` |
| `POST /attendance` | **新增** | `hr:attendance:update` |
| `PUT /employees/{id}/status` `/regularize` `/resign` | 三类动作 | 共用 `hr:employee:status` |

后果：`views/system/role/index.vue` 的权限矩阵按「动作段 → 6 列 + 允许」映射
（`ACTION_COLUMNS`，见该文件 1265 行注释），删除类动作因为码是 `:update` 会落进「修改」列
→ **薪资/绩效的「删除」列永远是空的**，而「修改」列同时管增删改，矩阵语义失真。

**建议**：补 `hr:salary:delete`、`hr:performance:delete`、`hr:attendance:create`（或把删除动作并入 `hr:salary:update` 的**语义说明**里并在矩阵 tooltip 标注），二者择一，但要一致。

---

## 3. P2

### P2-1 `system:user:query` 权限码不存在 → 「全部操作员」刷新按钮永久隐藏
`views/system/user/index.vue:31` 用 `v-permission="'system:user:query'"`，而该码**不在 `sys_permission`**（实测）。
`v-permission` 是全等匹配 + fail-closed（隐藏 DOM）→ 除超管（`'*'`）外**所有人看不到「刷新」按钮**。
（HR 相关 11 个页面共引用 12 个权限码，其余 11 个均存在且启用。）

### P2-2 `menu_level=3` 在非系统租户被过滤
`SysMenuServiceImpl.getUserMegaMenus` 对 `!isSystemTenant && !isSuperAdmin` 追加 `menu_level = 0` 条件。
80530/80531/80532/907 **menu_level 全是 3**（V11.422.0 明确「跟随实测值取 3」）
→ **非系统租户的租户管理员看不到这 4 个页面**。（财务模块同样有 17 条 `menu_level=3`，属同类问题。）
建议：把 HR 这 4 条订正为 0，或把「menuLevel 过滤」改为按父链判定。

### P2-3 菜单码命名断裂（`hr:` / `md:staff-*` / `hr-recruitment` 三套混用）
- `md:staff-dept` / `md:staff-role` / `md:staff-all` 是 `V6.22.0` 建在「资料 > 职员权限」下的**搬迁痕迹**（`V9.6.0` 整组改挂 61405）；
- 这三条 menu_code 在 `sys_permission` 中**一条对应权限码都没有**（`SELECT ... LIKE 'md:staff%'` → 0 行）
  → 它们的菜单派生永远 fail-open，「有权限才能看」的语义对这三页失效；
- 权限矩阵按 `permission_code` 第一段分域（该页 1238 行注释），`md:staff-*` 会与资料域 `md:*` 混在一起。

### P2-4 既有缺口复核（文档已登记，本次逐条复验仍存在）

| 缺口 | 复验证据 |
|---|---|
| `hr_employee.user_id` 未接线 | 列与实体字段都在，全仓 **0 读 0 写**（`grep -rn "setUserId\|getUserId" backend/hr` → 0） |
| 无 `@Scheduled` | `backend/hr` 内 0 处；`scheduled_task` 16 行里**没有 HR 任务** |
| 合同到期提醒无入口 | `GET /contracts/expiring` 存在，但 `hrContractApi.expiring` **0 调用**；无独立合同列表页（`hrContractApi.page` 也 0 调用；合同 CRUD 只在员工行内「合同」弹窗里可用） |
| `hr_performance` 无唯一约束 | `pg_constraint` 实测只有 PK，**无 `(employee_id, review_period)` uk** → 同员工同周期可重复提交 |
| 无薪资规则引擎 | 社保 10.5% / 公积金 12% / 起征点 5000 / 加班 1.5 倍仍是 `HrSalaryPaymentServiceImpl` 顶部常量；加班按「全月实际工时 − 全月标准工时」，标准工时只数周一至周五（无节假日日历） |
| 假期额度只有 Allocation | `hr_leave_quota` 只有 `quota_days`，无 Accrual / Rollover |
| 请假审批未接工作流 | `hr_leave_request.workflow_instance_id` 存在但全代码库无赋值/读取 |
| 薪资→财务零接线 | `HrSalaryPaymentServiceImpl.confirmPayment` 只写 `hr_salary_payment`，**不生成凭证/应付** |
| 租户数据重建遗漏 | `SystemRebuildService` 只清 `hr_employee`，其余 12 张 `hr_*` 表不清（代码注释已如实登记为保守取舍）→ 重建后考勤/请假/薪资/绩效/合同/异动/招聘/候选人残留 |
| 前端零 `v-permission` | 10 个 HR 页面 0 处（40 条权限码只由后端端点消费，按钮点了才 403） |
| 薪资无字段级脱敏 | 靠 `hr:salary:*` 粗粒度收口（`HrEmployee.phone/email/idCard` 有 `@DataMask`，薪资金额没有） |

### P2-6 🔴 业务校验错误一律返回 HTTP 500（真机实测，收尾时才发现）

HR 的 service 里 **98 处**用的是 `new BusinessException("...")`，而该构造器的默认 `code = 500`
（`BusinessException.java:23`），`GlobalExceptionHandler:215-229` 按 code 原样映射成 HTTP 状态
⇒ **「请选择员工」「考核记录不存在」「非法的状态迁移」这类正常的业务拒绝，客户端收到的是 500。**

真机证据（`tools/verify-hr-permissions.cjs`）：
```
DELETE /api/hr/performance/999999999  →  status=500  code=500  msg=考核记录不存在
```

**为什么这不是「只是个状态码」**：
1. 前端 `utils/request.ts:83` 的 `retryCondition` 是 `status >= 500`，且 `shouldRetry` 只放行 GET
   ⇒ **凡是 GET 端点上的业务校验失败（如 `?month=2026-13`），前端会自动重试 2 次**（指数退避 ~0.8s/1.6s），
   用户要多等 2 秒才看到本来立刻就该看到的错误提示；
2. `GlobalExceptionHandler` 对 `code >= 500` 走 `log.error` 且**写入错误日志服务**
   ⇒ 用户填错日期这种正常操作被记成服务端故障，污染错误监控。

**修法**（本仓已有现成口径，1390 处在用）：改调用工厂方法
`BusinessException.badRequest(...)`（400，占绝大多数：参数为空/非法/状态不允许）、
`notFound(...)`（404，"XX 不存在"）。
**诊断侧对照**：`department` 模块用的是 `BusinessException.notFound/badRequest`（正确示范）。

**本轮未改**：98 处逐一分类属于「改 API 契约语义」的判断，且会影响所有 HR 端点的返回码，
不宜在未确认前批量替换 —— 已登记，待拍板。

### P2-5 违反《开发技术规范》（2026-07-13 版，全仓性，非 HR 独有）

| 规范 | HR 实测 |
|---|---|
| 主表 ≤ 25 列 | ❌ `hr_employee` **33 列**、`hr_candidate` **29 列**、`hr_recruitment` **28 列** |
| Service 类 ≤ 500 行 | ❌ `HrSalaryPaymentServiceImpl` **532 行**（另有 `HrAttendanceServiceImpl` 491、`HrEmployeeServiceImpl` 466 接近上限） |
| 禁止 `any` 类型 | ❌ HR 8 个页面 + `staff-dept` 共 **284 处** `any`（`recruitment/list.vue` 单页 63 处） |
| 列表页列数 ≤ 20 | ⚠️ `hr/salary/list.vue` 的 `paymentColumns` 共 21 列（默认可见 18，3 列 `defaultHidden`），贴线 |

---

## 4. 正面结论（可作回归基线）

1. **菜单连通性 0 缺陷**：10 个 HR 叶子菜单 + 6 个分组 + 1 个顶级，`component` 全部能按 `getComponent` 规则
   （`exact` 命中）落到磁盘真实 `.vue`；无 `display_mode=1` 配置不一致；`client_type` 全为 `tenant-admin`、
   `visible=1`、`status=1`。（`tools/audit-hr-menu.py`）
2. **后端鉴权全覆盖**：`HrController` / `HrRecruitmentController` / `HrCandidateController` 共 **92 个端点，
   0 个无鉴权注解**；类级 `@SaCheckLogin` + 方法级 `@RequiresPermission`；
   40 条权限码 **100% 在种子库且 `status=0`（启用）**；代码引用与库中权限码**零缺口**
   （唯一被扫出的 `hr:xxx` 是 `HrController` 类注释里的示例文本，非真实注解）。
3. **接口双向 0 断链**：前端 `api/hr/index.ts` 93 处调用 ↔ 后端 92 个端点全部对得上（`tools/audit-hr-api-wiring.py`）。
4. **数据层隔离扎实**：13 张 `hr_*` 表全部有 `tenant_id` + `deleted`，且**都不在 `IGNORE_TENANT_TABLES`**
   → 多租户拦截器自动注入 `tenant_id`、`@TableLogic` 自动过滤软删。
   服务层里的 `eq(tenantId != null, ...)` 是双保险，不是唯一防线。
5. **实体 ↔ 表逐列一致**：14 个实体（含 `HrDepartmentRef` → `sys_department`）对账**零缺列**
   （`tools/audit-hr-db.py` §②）。
6. **索引齐备**：13 张表共 39 个索引，覆盖 `employee_id` / `dept_id` / `tenant_id` / 业务日期；
   `hr_leave_quota` 有 `(tenant_id, leave_type, year) WHERE deleted = 0` 部分唯一索引、
   `hr_attendance_rule` 有 per-tenant 唯一索引。（文档 §8.5#13「无业务索引」**已过期，应订正**。）
7. **敏感字段脱敏**：`HrEmployee.phone/email/idCard` 带 `@DataMask`，由 `DataMaskSerializer` 消费。
8. **前端零桩**：10 个页面 35 处 `console.*` 全部是 `[模块] xxx失败` 形态的错误日志，
   无 mock / 假数据 / TODO 占位；E2E 10/10 页断言「金标准骨架 + 表格 + 列配置齿轮 + 控制台无 error」全过
   （`tool-results/e2e-hr/e2e-hr-result.txt`：**171 PASS / 0 FAIL**）。
9. **跨模块闭环已打通**（doc §4 的「❌ 完全断开」清单已基本翻案）：
   - 招聘 → 入职 → 员工档案：`POST /candidate/{id}/hire` 一个事务内建档 + 置状态 7 + 维护 `applicant_count`/`hired_count`；
   - 请假 ↔ 考勤：批准写 `LEAVE` 标记、撤销/拒绝还原（`markLeave`/`unmarkLeave`）；
   - 绩效 → 薪资：`绩效工资 = 绩效基数 × performance_coefficient`（未填按 1.0）；
   - 考勤 → 薪资：缺勤扣款**只统计 `ABSENT`**（`LEAVE` 不扣，避免请假被误扣）；
   - 编制管控：建档/转正/调岗/离职自动重算 `hr_position.current_count`，有在岗员工时拒删岗位；
   - 部门主数据：HR 侧只读引用（`HrDepartmentRef` 的类注释明确「不得本模块增删改」）；
     **反方向** `DepartmentServiceImpl.delete` 会校验 `sys_user.dept_id` / `sys_position.dept_id` / `hr_employee.dept_id` 三类引用。
10. **租户重建登记**：`hr_employee` 已列入 `SystemRebuildService` 的「10 职员」范围。
11. **状态机与字段白名单**：员工 试用(2)→在职(1)→离职(0)（离职为终态）、合同/绩效/请假/发放各有状态机；
    状态类字段（`status`/`leaveDate`/`regularDate`/`currentCount`/`applicantCount`）**不接受表单直改**。
12. **无死类**：`hr-base` 52 个 Java 类逐个做引用扫描（跳过 Spring 按接口装配的 `*Impl`），
    **无任何类处于"零引用"状态**（`tools/audit-hr-deadcode.py` → 0）。死代码集中在**方法级**（见 P1-1/P1-2）。

---

## 5. 与开发文档的差异（文档需回写）

| # | 文档位置 | 文档说法 | 实际 | 处置 |
|:-:|---|---|---|---|
| 1 | README §8.3「统计 / 导出端点」 | 已交付 | `/stat` 8 个后端已建但**前端零消费**；导出**无后端端点**（全在前端拼 CSV） | 改成「后端能力就绪，前端未展示」 |
| 2 | README §8.5#13 | 「`hr_*` 各表除主键/唯一键外基本无索引」 | 实测 39 个索引，含 dept/tenant/日期/部分唯一索引 | 订正为已补齐 |
| 3 | README §4 接线表 | 「❌ 完全断开」×5 | 招聘→入职、请假→考勤、绩效→薪资、考勤→薪资均已打通 | 按 §4-9 回写 |
| 4 | README §4 | `hr_employee.user_id`「字段不存在」 | 字段已建（V11.380.0），仍**无读写入口** | 改为「列已建，未接线」 |
| 5 | README §2.4 | `views/system/{department,position,user,role}` 四页「全部无菜单」 | role/user 已被 80531/80532 指向；position **失去入口**（P1-3）；department 页**已被删除**，只剩空目录 | 重写该表 |
| 6 | `md/staff-dept/index.vue:463` 注释 | 「后端删除不校验 …引用（P0）」 | `DepartmentServiceImpl:245-273` **已实现**三类引用校验 | 删掉该注释 |
| 7 | README §2.4 | `views/system/user/index.vue` 918 行 / `position` 1301 行 / `role` 931 行 | 实际 1661 / 1601 / 2563 行 | 更新行数 |
| 8 | README §8.3 末行 | 「候选人删除端点 V11.386.0 补齐」 | ✅ 属实（`hr:candidate:delete` 在库、`hrCandidateApi.remove` 已被调用） | 保持 |
| 9 | 全部 11 篇页面文档 | 未提及 P0-1（前端菜单可见性口径） | 本次新发现 | 追加到 README §7 |

---

## 6. 建议的处置次序

**第一批（P0，建议本轮就做）**
1. 前端 `hasPermission` 口径统一（改前缀匹配）+ `canAccessMenu` 两处死口径收敛 → 一次修复覆盖全站 302 条菜单；
2. `hr_employee` 唯一约束改 `(tenant_id, employee_no)`；号段查不到时 upsert 默认行而非抛异常；
3. 出 `tools/grant-hr-permissions.py`（系统管理员全授、部门管理员只读子集，**排除 `hr:salary:*`**）——按既定纪律，改授权数据前先给方案获批。

**第二批（P1，清理与接线）**
4. 删 `views/hr/attendance/index.vue` + `componentMap['hr/attendance/index']`；
5. 裁定 `views/system/position/index.vue`（1601 行）的去留：要么给回菜单入口，要么删页 + 删僵尸键 + 删单测；
6. 删 5 个死 `listForExport` + `HrLookupHelper.fillEmployeeInfo` + `HrEmployeeService.getByEmployeeNo`；
7. 8 个 `/stat` 端点：要么在前端落地统计卡，要么删除（不要留"已建未接"）；
8. 清理 9071–9073 孤儿按钮菜单，`907.menu_code` 改 `hr:recruitment`；
9. 补 `hr:salary:delete` / `hr:performance:delete`（或统一语义），使权限矩阵列不再失真。

**第三批（P2，规范与文档）**
10. 订正 80530/80531/80532/907 的 `menu_level` → 0；
11. `md:staff-*` 菜单码与权限码前缀对齐（或明确登记为"无权限码、恒可见"的例外）；
12. 补 `system:user:query` 权限码，或把该按钮改用 `system:user:list`；
13. 行级数据权限：先给「员工档案 / 薪资」两张表在 `sys_data_scope` 里配规则 + 打 `@DataPermission`，验证生效后再推广；
14. 回写 README §4/§7/§8 与相关页面文档（见 §5 清单）。

---

## 6.5 本轮已执行的修复（2026-09-23，用户「按推荐执行」后落地）

| # | 条目 | 落点 | 验证 |
|:-:|---|---|---|
| 1 | **P0-1** 前端菜单可见性口径统一 | `components/MegaMenuPanel`：删掉「全等匹配」的第二份判定（可见性由后端单点决定）；连带删除零调用的 `utils/permission.ts#canAccessMenu`、`composables/usePermission#useMenuPermission`（canAccessMenu/canAccessAnyMenu/filterMenus）及其单测 | `node tools/verify-menu-visibility.cjs` → **5 PASS / 0 FAIL**：非超管面板 **0 条 → 9 条**（唯缺「薪资管理」，见 P0-2 注）、超管 10 条 |
| 2 | **P0-3** 工号唯一约束补租户维度 | 迁移 `V11.502.0` §一：`UNIQUE(employee_no)` → `UNIQUE(tenant_id, employee_no) WHERE deleted = 0`（同时与逻辑删除口径对齐） | 事务内实测 4 条断言：跨租户同工号 ✓ 允许、同租户重复 ✗ 拒绝、软删后可复用 ✓ |
| 3 | **P0-3** 号段按模板自愈 | `BizNumberSequenceMapper#seedMissingSequence`（`@InterceptorIgnore` + `ON CONFLICT DO NOTHING`）+ `BizNumberGeneratorService` 查不到时补种；`nextNumber(bizType)` 由写死租户 1 改为**会话租户** | 事务内实测：租户 2 的 `EMP` 号段补种成功（1 行，前缀/长度沿用模板）、重复执行 0 行（幂等） |
| 4 | **P0-2** HR 权限授权 | `tools/grant-hr-permissions.py`（含授权前备份）：SYSTEM_ADMIN **37** 条（排除 `hr:salary:*`）、DEPT_ADMIN **8** 条只读（同样排除薪资） | 脚本回查输出；误授的 `hr:salary:list` 已撤销 |
| 5 | **P1-2** 死方法 | 删 5 个 `listForExport`（考勤/员工/请假/绩效/薪资，接口+实现）、`HrLookupHelper#fillEmployeeInfo`、`HrEmployeeService#getByEmployeeNo` | `mvn -o -pl core/base/core-base install` + `-pl hr/hr-base compile` + `-pl core/api/core-api compile` 全部 BUILD SUCCESS |
| 6 | **P1-3** 死页面 | 删 `views/hr/attendance/index.vue` + `componentMap['hr/attendance/index']`（连带消灭写死 `clockIn(0)` 的脏数据入口） | 全仓 grep 零残留引用 |
| 7 | **P1-4** 菜单码与孤儿按钮 | 迁移 `V11.502.0` §三/§四：`907.menu_code` `hr-recruitment` → `hr:recruitment`；软删 9071/9072/9073 | DB 回查 |
| 8 | **P1-6** 删除类权限码 | 迁移 §五 新增 `hr:salary:delete`(91207) / `hr:performance:delete`(91208)，`HrController` 3 处注解同步改掉 | DB 回查 + 编译通过 |
| 9 | **P2-1** 悬空权限码 | `views/system/user/index.vue` 的刷新按钮 `system:user:query` → `tenant-admin:user:list`（库中不存在→不存在悬空） | 复扫：11 个页面 12 个权限码，**悬空 0** |
| 10 | **P2-2** `menu_level` | 迁移 §二：80530/80531/80532/907 由 3 → 0 | DB 回查 |

> ⚠️ **授权矩阵的一个刻意副作用（请确认是否符合预期）**：本脚本按权限种子自己的注释
> 「薪资保密：与其余 HR 权限分开授予」，**没有**把 `hr:salary:*` 给系统管理员/部门管理员
> ⇒ 「薪资管理」菜单目前**只有超管可见**（与改造前一致，不是回归），要开放需另建薪酬角色
> 或把 `PLANS` 里系统管理员那行的过滤条件去掉。其余 6 个 HR 页面已对系统管理员开放。

**未执行 / 需另行裁定**：

| 条目 | 为什么不做 |
|---|---|
| `views/system/position/index.vue`（1601 行，失去入口）的去留 | **文档裁定与后续迁移相互矛盾**：《岗位权限开发文档》§5.5 的裁定是「菜单 80531 改指该页、废弃本页」，而 `V11.429.0` 以「该页无 role/permission 引用、配不了权限、名实不符」为由改指了角色页。删 / 留都等于替产品拍板，按「处置重复实现前必须先查文档裁定」的纪律**留待裁定**（现状：文件 + `componentMap` 僵尸键都还在）。 |
| `md:staff-*` 三条 menu_code 对齐权限码前缀 | 对齐后这三页会**只对超管可见** —— 实测 `tenant-admin:department`(6 条) 与 `tenant-admin:role`(12 条) **只有 SUPER_ADMIN 持有**，非超管一个都没有。要一起做，得先裁定「这三页算系统管理域还是 HR 域」并配套授权；只做菜单码会变成「菜单凭空消失」。故本轮**按例外登记**（无对应权限码 ⇒ fail-open 恒可见），并写入 §5 待办。 |
| 8 个 `/stat` 端点接前端 | 属**新增界面**而非修复，需要产品确认要在哪些页面展示哪些指标；本轮只登记「后端已建、前端未接」，不动端点也不删。 |
| 行级数据权限（P1-5） | 全站性问题（`sys_data_scope` 0 行 + `@DataPermission` 全仓 0 引用），不是 HR 单模块能收口的；需先做全站方案。 |
| **P2-5 技术规范债**（`hr_employee` 33 列 > 25、`HrSalaryPaymentServiceImpl` 532 行 > 500、284 处 `any`） | 属**重构**而非修复：拆表/拆类会动到已通过 174 项 E2E 的既有实现，收尾阶段不宜做；`any` 是全仓普遍现象，单独改 HR 会造成风格分裂。已登记为规范债。 |
| **P2-6 业务错误返回 500**（98 处 `new BusinessException`） | 会改变**全部 HR 端点**的返回码语义（500 → 400/404），且 98 处要逐条判断是 400 还是 404；在未确认前不做批量替换（改完要重跑 E2E + 前端回归）。 |

### 6.6 落地后的全量回归（2026-09-23 真机）

| 回归项 | 命令 | 结果 |
|---|---|---|
| 前端单测 | `cd frontend/apps/pc-admin && npx vitest run` | **244 passed / 8 skipped（10 文件）** |
| 前端 lint（改动 4 个文件） | `npx eslint src/components/MegaMenuPanel/MegaMenuPanel.vue src/utils/permission.ts src/composables/usePermission.ts` | **0 error**（余 5 条 `any` warning 为存量） |
| 后端编译 | `mvn -o -pl core/base/core-base install` → `-pl hr/hr-base install` → `-pl core/api/core-api package` | **BUILD SUCCESS**；fat jar 内 `hr-base`/`core-base` 已含改动，且**无 ECJ 残缺类** |
| 模块 E2E | `node tools/e2e-hr.cjs` | **174/174 通过，0 失败**（含本轮新增 5 条回归断言） |
| 菜单真机 | `node tools/verify-menu-visibility.cjs` | **5 PASS / 0 FAIL** |
| 权限真机 | `node tools/verify-hr-permissions.cjs` | **6 PASS / 0 FAIL**（员工查询放行 / 绩效删除放行 / 薪资三项仍 403 / 超管对照） |

**E2E 顺带修掉的一处「断言没跟着菜单改指走」**：
`e2e-hr.cjs` 对 10 个页面统一套「`CategoryListLayout` + `.ss-grid` 齿轮」断言，
但 80531/80532 在 `V11.422.0`/`V11.429.0` 之后指向的是**系统域页面**
（`views/system/{role,user}/index.vue`，实测这两个文件里 `CategoryListLayout` 出现 **0 次**）
⇒ 这两页的 4 条断言**自 2026-09-19 起恒失败**，是断言过期而非页面缺陷。
现已按 `PAGES` 的第三列区分 `category`/`system` 两种期望形态。

**运行时环境坑（本轮实踩，务必记住）**：我启动的后端实例（22:44:31）在运行中被**并行会话重写了同一个
fat jar**（jar mtime 22:48:38）⇒ 之后所有 `/api/auth/login` 返回 500，堆栈是
`NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy`（懒加载随机 CNFE）。
**重启即愈，不要去查依赖冲突** —— 与 `fatjar-swap-while-running` 记录的是同一个坑。

**两处自我更正**（审计报告本身的口径修正）：

1. **`.atcode/` 目录不是误放**：`views/hr/organization/.atcode/workflows/` 曾被列为「目录污染」，实际 `.atcode/` 在 `.gitignore:114` 里，且 `views/` 下几十个目录都有 —— 它是本地工具状态，不入库。本报告已删掉该条。
2. **权限缓存会假装「修复没生效」**：授权完立刻验证会看到旧结果（L1 Caffeine 30s + L2 Redis 5min，实测重启前 `e2e_hr_ta` 的角色列表都可能是空的）。复跑验证前先等缓存过期或重启后端。

---

## 7. 审计脚本与产物（本次新增，均为只读）

| 脚本 | 作用 | 产物 |
|---|---|---|
| `tools/audit-hr-menu.py` | HR 菜单连通性 / 双入口自洽 / client_type / menu_level | stdout |
| `tools/audit-hr-backend.py` | HR 控制器端点 + 鉴权注解 + 权限码抽取 | `tool-results/hr-backend-audit.json` |
| `tools/audit-hr-db.py` | `hr_*` 表 / 实体对账 / 权限码对账 / 角色授权 / 用户角色 | `tool-results/hr-db-audit.json` |
| `tools/audit-hr-frontend.py` | HR 页面清单 / 孤儿 / 僵尸键 / 桩扫描 | `tool-results/hr-frontend-audit.json` |
| `tools/audit-hr-api-wiring.py` | 前后端接口双向差集 | stdout |
| `tools/audit-hr-frontend-api.py` | `api/hr` 方法级消费方扫描 | stdout |
| `tools/audit-hr-deadcode.py` | HR 后端类引用扫描 | stdout |
| `tools/audit-hr-menu-visibility.cjs` | **运行时**：登录真实账号，对账「后端下发的菜单」vs「前端 MegaMenuPanel 判定」 | stdout |

一键复跑（后端需在 5655、前端 dev server 需在 5656）：

```bash
cd i:/AI-Ready
python tools/audit-hr-menu.py
python tools/audit-hr-backend.py && python tools/audit-hr-db.py && python tools/audit-hr-api-wiring.py
python tools/audit-hr-frontend.py && python tools/audit-hr-frontend-api.py && python tools/audit-hr-deadcode.py
E2E_USER=e2e_hr_ta node tools/audit-hr-menu-visibility.cjs   # 非超管视角（P0-1 的证据）
E2E_USER=e2e_hr    node tools/audit-hr-menu-visibility.cjs   # 超管视角（对照组：全部显示）
```

> 审计期间 dev 后端（5655）曾被并行会话停止又重启；P0-1 的两组观测（超管全显示 / 非超管全隐藏）
> 都是在**重启后的同一实例**上取得的，互为对照组，说明这不是缓存或实例状态问题。
> 另注：`e2e_hr_ta` 在重启前那次登录拿到的角色列表为空（角色/权限有 L1 Caffeine + L2 Redis 两级缓存），
> 重启后正常返回 `SYSTEM_ADMIN` / 222 条权限码 —— 复跑前若结果异常，先怀疑缓存与实例状态。
