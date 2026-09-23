# 人力资源模块 · 开发文档集

> **对应系统**：来肯企汇 ql361 v2.2（`22stable.ql361.com`）**没有人力资源模块** —— 本模块是**本系统独有扩展**。
> **撰写口径**：按**业界成熟生产级 HR 系统**（Odoo 18、SAP SuccessFactors / S/4HANA HCM、金蝶 s-HR / 星瀚 HR、用友 YonSuite 人力云，并引 Workday / BambooHR）的标准能力设计，**不照搬任何一家界面**。
> **事实来源**：① 本系统源码（引用必须带 `文件:行号`）；② devdb 实测（`sys_menu` 菜单树、`hr_*` / `sys_*` 表结构与行数，2026-09-18 直连查询）；③ 业界官方文档（标题 + URL + 章节 + 查阅日期留痕）。
> **通用规范**：见《ql361对标/对标开发技术参考文档》第 4、5 章；布局金标准施工手册见《交易模块/_开发指南-金标准.md》（三条路线 A/A′/B、模板、陷阱、自检清单）；同结构模块可参照《CRM模块/README.md》。

> ## 🟢 最新状态（2026-09-18 金标准实施完成）
>
> **本文下面 §3「当前实现现状」与 §7「全局缺口汇总」是实施前的基线快照，保留作历史对照。**
> **实施后的真实状态见文末 §8「本轮落地情况（2026-09-18）」**，各页细节见每篇页面文档末尾的
> 「附：本轮落地情况（2026-09-18）」章节。
>
> 一句话：**6 条 P0 全修、10 个菜单页全部升级到路线 A 金标准、模块级 E2E 171/171 通过。**
> 关键修复：`/hr` 裸前缀 → `/api/hr`（6 组端点全 404）、10 个 Mapper 无 SQL 绑定、
> `sys_user_position` 缺列、HR 权限码 0 条、`SysUser.password` 外泄、招聘孤儿菜单；
> 另收尾修掉**跨租户越权**（`sys_user` 查询无租户条件 → 租户管理员可翻到全库账号）——
> 口径为「**超管豁免、其余限本租户**」，故 `admin` 的验证方式不受影响；
> 新增能力：人事异动留痕、考勤规则、假期额度、累计预扣个税、绩效系数联动、
> 招聘→入职→员工档案闭环、请假↔考勤联动。

---

## 1. 模块定位与边界

人力资源 = **组织 · 人 · 时间 · 钱 · 评价** 五件事的闭环：

```
组织架构(部门) ─┬─▶ 岗位(编制) ─▶ 员工档案 ─┬─▶ 考勤记录 ───┐
                │                          ├─▶ 请假管理 ───┼─▶ 薪资管理(核算+发放)
                │                          └─▶ 绩效考核 ───┘
                └─▶ 招聘管理(需求→候选人)──────────▶ 入职 ──▶ 员工档案
```

**边界铁律**：
- 本模块**不负责系统账号与角色权限**（那是系统管理域）；但菜单里确实挂了「职员管理」三页（`md:staff-*`），**归属存疑**，见 §2.4。
- 「职位管理」的 `hr_position`（HR 岗位）与系统管理的 `sys_position`（系统岗位）是**两套主数据**，当前互不相通（P0，见 §7）。
- 部门主数据归**系统管理**（`sys_department` + `DepartmentController`），HR 仅**引用** `dept_id`。

---

## 2. 模块菜单结构与文档清单（`sys_menu` 实测 2026-09-18）

### 2.1 菜单树（devdb 直查，递归展开）

```
60014  人力资源                                (mega:hr, sort=1400)
├─ 61401  员工管理                              (mega:hr:employee, sort=100)
│   └─ 90001  员工列表    hr:employee           单入口  hr/employee  → views/hr/employee/list.vue
├─ 61402  考勤管理                              (mega:hr:attendance, sort=200)
│   ├─ 90002  考勤记录    hr:attendance         单入口  hr/attendance → views/hr/attendance/list.vue
│   └─ 90003  请假管理    hr:leave              单入口  hr/leave      → views/hr/leave/list.vue
├─ 61403  薪资管理                              (mega:hr:salary, sort=300)
│   ├─ 90004  薪资管理    hr:salary             单入口  hr/salary     → views/hr/salary/list.vue
│   └─ 90005  绩效考核    hr:performance        单入口  hr/performance→ views/hr/performance/list.vue
├─ 61404  组织管理                              (mega:hr:organization, sort=400)
│   └─ 90006  职位管理    hr:position           单入口  hr/organization/position → views/hr/organization/position-list.vue
└─ 61405  职员管理                              (mega:hr:staff, sort=500)
    ├─ 80530  职员部门    md:staff-dept         单入口  md/staff-dept → views/md/staff-dept/index.vue
    ├─ 80531  岗位权限    md:staff-role         单入口  md/staff-role → views/md/staff-role/index.vue
    └─ 80532  全部操作员    md:staff-all        单入口  md/staff-all  → views/md/staff-all/index.vue

（游离）907  招聘管理      hr-recruitment        单入口  /hr/recruitment → views/hr/recruitment/list.vue
       ├─ 9071  新增      hr-recruitment:add    （按钮型菜单 menu_type=3）
       ├─ 9072  编辑      hr-recruitment:edit
       └─ 9073  删除      hr-recruitment:delete
```

**菜单数**：1 个顶级 + 5 个分组 + **10 个页面项**。

### 2.2 页面 ↔ 文档对照表

| 分组 | 页面 | 菜单ID | menu_code | 路由 | 组件 | 文档 |
|------|------|:-----:|-----------|------|------|------|
| 员工管理 | 员工列表 | 90001 | `hr:employee` | `hr/employee` | `views/hr/employee/list.vue`（518 行） | [员工列表开发文档](./员工列表开发文档.md) |
| 考勤管理 | 考勤记录 | 90002 | `hr:attendance` | `hr/attendance` | `views/hr/attendance/list.vue`（70 行） | [考勤记录开发文档](./考勤记录开发文档.md) |
| | 请假管理 | 90003 | `hr:leave` | `hr/leave` | `views/hr/leave/list.vue`（112 行） | [请假管理开发文档](./请假管理开发文档.md) |
| 薪资管理 | 薪资管理 | 90004 | `hr:salary` | `hr/salary` | `views/hr/salary/list.vue`（163 行） | [薪资管理开发文档](./薪资管理开发文档.md) |
| | 绩效考核 | 90005 | `hr:performance` | `hr/performance` | `views/hr/performance/list.vue`（75 行） | [绩效考核开发文档](./绩效考核开发文档.md) |
| 组织管理 | 职位管理 | 90006 | `hr:position` | `hr/organization/position` | `views/hr/organization/position-list.vue`（222 行） | [职位管理开发文档](./职位管理开发文档.md) |
| 职员管理 | 职员部门 | 80530 | `md:staff-dept` | `md/staff-dept` | `views/md/staff-dept/index.vue`（320 行） | [职员部门开发文档](./职员部门开发文档.md) |
| | 岗位权限 | 80531 | `md:staff-role` | `md/staff-role` | `views/md/staff-role/index.vue`（316 行） | [岗位权限开发文档](./岗位权限开发文档.md) |
| | 全部操作员 | 80532 | `md:staff-all` | `md/staff-all` | `views/md/staff-all/index.vue`（325 行） | [全部操作员开发文档](./全部操作员开发文档.md) |
| （游离） | 招聘管理 | 907 | `hr-recruitment` | `/hr/recruitment` | `views/hr/recruitment/list.vue`（421 行） | [招聘管理开发文档](./招聘管理开发文档.md) |
| — | （无菜单）组织管理 | — | — | `hr/organization/index` | `views/hr/organization/index.vue`（273 行） | [组织管理开发文档](./组织管理开发文档.md) |

> **10 个菜单页 ↔ 11 篇文档**：其中 10 篇对应菜单页，另 1 篇（`组织管理开发文档`）记录**无菜单的孤儿页**与部门主数据的真实归属。

### 2.3 🔴 招聘管理是**孤儿菜单**（P0，用户点不到）

菜单 `907 招聘管理` 的 `parent_id = 900`，但 **`sys_menu` 中根本不存在 id = 900 的记录**（`select * from sys_menu where id between 900 and 999` 只返回 907 一行）。

- 根因：迁移 `V9.6.1:27` 删除了 901–906 却**遗漏 907**；`V11.36.0:152` 又把它写成 `parent_id=900`。
- 后果：**父节点不存在 → 菜单树上不可达 → 招聘管理页面用户无法进入**。
- 修法：把 907 挂到已存在的分组（建议新建 `61406 招聘管理` 分组，或暂挂 `61401 员工管理`），并同步修正 `menu_level`。
- 附带：其三个按钮型子菜单 `9071/9072/9073` 声明了权限码 `hr-recruitment:add|edit|delete`，但**这三条权限码在 `sys_permission` 中不存在** → 修好菜单后按钮仍可能被隐藏。

### 2.4 「职员管理」三页的归属存疑（需裁定）

| 事实 | 说明 |
|------|------|
| 菜单挂在 **人力资源 → 职员管理** 下 | 中文名看着像 HR |
| 但 `menu_code` 前缀是 `md:`，组件在 `views/md/` 下 | 与 HR 的 `hr:*` / `views/hr/` 命名体系**不一致** |
| 实际功能是**系统管理域**内容 | 部门 / 角色权限 / 操作员账号，与人事概念（编制、异动、考勤）无关 |
| 存在**功能更强的同主题页面但无菜单** | `views/system/department/index.vue`(1138 行)、`views/system/position/index.vue`(1301 行)、`views/system/user/index.vue`(918 行)、`views/system/role/index.vue`(931 行) —— **全部无 `sys_menu` 绑定、用户不可达** |

> **用户在 HR 菜单下看到的，恰是功能最弱的那一套（简化版）**；完整版躺在 `views/system/` 下没有入口。
> **建议**：裁定 `md:staff-*` 三页与 `views/system/*` 四页的收敛关系（保留一套、另一套下线或做重定向），并统一 `menu_code` 前缀。

> 📌 **2026-09-23 第三轮审计后的实际状态**（本表上半部分已过期，保留作历史对照）：
> - `V11.422.0` 已把 80532 → `views/system/user/index.vue`、`V11.429.0` 把 80531 → `views/system/role/index.vue`
>   ⇒ **user / role 两页已有菜单**，不再是"不可达"；
> - `views/system/department/index.vue` **已被删除**（只剩空目录 `views/system/department/`）；
> - `views/system/position/index.vue` 现为 **1601 行且失去入口**（V11.429.0 把它从 80531 换下来之后）——
>   它的去留**仍需裁定**：本文档 §5.5 的裁定是「改指该页、废弃 md/staff-role」，与 V11.429.0 的
>   「该页配不了权限、名实不符」相反，两条结论至今未合并；
> - 三页行数实测为 user **1661** / position **1601** / role **2563**（本表写的是 918/1301/931）。
> - `md:staff-*` 三条 `menu_code` **保持原样未动**：它们没有对应的权限码 ⇒ 菜单派生 fail-open 恒可见。
>   要对齐前缀得先裁定"这三页算系统管理域还是 HR 域"，否则对齐后**只会对超管可见**
>   （实测 `tenant-admin:department` / `tenant-admin:role` 两组权限码只有 SUPER_ADMIN 持有）。

---

## 3. 本模块当前实现现状（2026-09-18 源码 + devdb 实测）

> ⚠️ **本节是「实施前」的基线快照（历史对照用）**，其中「整体未达金标准 / 浏览器中基本不可用」
> 等结论**已被 §8 的本轮实施推翻**。查当前状态请看 §8，以及各页文档末尾的「附：本轮落地情况」。
>
> **重要**：本模块**整体未达金标准**，且**当前在浏览器中基本不可用**（见 §3.2 的 404 链）。逐页实情见各页文档「实现差异说明」，此处汇总：

### 3.1 页面外壳符合度

| 页面 | 当前外壳 | 目标（金标准） | 差距 |
|------|---------|---------------|------|
| 员工列表 | 原生 `a-table` + `ErrorBoundary` | 路线 A | 整体重写 |
| 考勤记录 | 原生 `a-table` | 路线 A | 整体重写 |
| 请假管理 | 原生 `a-table` | 路线 A | 整体重写 |
| 薪资管理 | 原生 `a-table` + `ErrorBoundary` | 路线 A | 整体重写 |
| 绩效考核 | 原生 `a-table` | 路线 A | 整体重写 |
| 职位管理 | 原生 `a-table` | 路线 A | 整体重写 |
| 职员部门 / 岗位权限 / 全部操作员 | `ARReportPage`（路线 B） | 路线 A | 整体重写 |
| 招聘管理 | 原生 `a-table` + `Modal` | 路线 A | 整体重写 |
| 组织管理（无菜单） | 原生 `a-table` | 路线 A | 整体重写 |

**全模块 0 个页面使用** `CategoryListLayout` / `BillTableList` / `BillDetailTable` / `StandardPagination` / `PageConfigPanel` / `BillFormPage`。

### 3.2 🔴 全模块共性问题（逐页文档不再重复，此处集中列）

**P0-1 · 路径前缀不一致 → 6 组端点全部 404（模块级致命）**

| 控制器 | 类级 `@RequestMapping` | 评价 |
|--------|----------------------|------|
| `HrController` | **`/hr`**（**缺 `/api`**） | ❌ **错** |
| `HrRecruitmentController` | `/api/hr/recruitment` | ✅ |
| `HrCandidateController` | `/api/hr/candidate` | ✅ |
| `DepartmentController` / `PositionController` / `UserController` / `RoleController` | `/api/...` | ✅ |

前端 `utils/request.ts` 的 `baseURL = '/api'`，而 `api/hr/index.ts` 写 `request.get('/hr/employees/page')` → 实际请求 `/api/hr/employees/page`。
**已验证**：全仓**无** servlet `context-path`（各 profile 都是 `/`）、**无** `addPathPrefix`（`HealthCheckConfig.configurePathMatch` 是空实现）、vite proxy `'/api'` **无 rewrite**；全仓裸前缀控制器**只有 `/hr` 一个**（另两个是 fallback）。

→ **员工 / 考勤 / 请假 / 薪资 / 绩效 / 岗位 六组端点全部 404**，页面在浏览器里完全打不开。
→ **修法**：`HrController` 改为 `@RequestMapping("/api/hr")`。

**P0-2 · 招聘页双前缀 → `/api/api/...`（另一条 404）**

`views/hr/recruitment/list.vue:292/368/371/383` 直接写 `request.get('/api/hr/recruitment/page')` 等**绝对路径**（未走 `api/hr/index.ts`），
叠加 `baseURL='/api'` → 合成 **`/api/api/hr/recruitment/page`** → 404。
> 这是金标准陷阱 11（`/api` 双前缀）的又一实例。**修好 P0-1 也不能修这个**，两处要分别改。

**P0-3 · 10 个 Mapper 方法无 SQL 绑定 → `Invalid bound statement (not found)`**

`hr-base` 模块**没有 `src/main/resources` 目录、没有任何 mapper XML**，接口方法也**没有 `@Select` 注解**：

| Mapper 方法 | 调用方 |
|-------------|--------|
| `HrEmployeeMapper.selectByDeptId` / `selectByEmployeeNo` / `updateStatus` | `PUT /hr/employees/{id}/status` |
| `HrPositionMapper.selectByDeptId` | 按部门查岗位 |
| `HrContractMapper.selectByEmployeeId` | `GET /hr/employees/{id}/contracts` |
| `HrAttendanceMapper.selectByEmployeeAndMonth` | **月度薪资生成** |
| `HrLeaveRequestMapper.selectByEmployeeId` | 员工请假查询 |
| `HrSalaryStructureMapper.selectEffectiveByEmployeeId` | **月度薪资生成（核心查询）** |
| `HrSalaryPaymentMapper.selectByEmployeeId` | 员工薪资历史 |
| `HrPerformanceMapper.selectByEmployeeId` | 员工绩效历史 |

→ 至少 **8 个端点/链路调用即抛异常**；其中月度薪资生成还会被 `try-catch` **吞掉异常**，**静默产出 0 条记录**（报喜不报忧）。
> 已确认全仓库**没有任何 XML** 的 namespace 是 `cn.aiedge.hr.mapper.*`。

**P0-4 · `sys_user_position` 缺两列 → 岗位列表也 500**

`UserPositionMapper` 的 3 个 `@Select` 都写了 `AND deleted = 0`（`UserPositionMapper.java:23,29,35`），但库表
**既没有 `deleted` 列、也没有 `tenant_id` 列**，且不在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 白名单。
→ 双重报错。且 `PositionServiceImpl.convertToVO` **无条件调用** `countUsersByPositionId` →
**`GET /api/position/page` 也会 500**（不只是写操作）。当前该表 0 行，缺陷处于**潜伏态**：**新增第一个岗位后列表页必挂**。

**P0-5 · 薪资数据无任何权限保护**

- `HrController` **无任何鉴权注解**（无 `@SaCheckLogin`、无 `@RequiresPermission`），而 `HrRecruitmentController`/`HrCandidateController` **有** `@SaCheckLogin`。
- **`sys_permission` 中 HR 权限码 0 条**（全表 169 行，前缀只有 `crm(5)/department(6)/erp(23)/finance(24)/log(6)/permission(5)/position(7)/purchase(9)/role(5)/sale(9)/stock(2)/system(58)/tenant(5)/user(5)`）。
- 唯一被声明的 HR 权限码是**菜单里挂的** `hr-recruitment:add|edit|delete` 三条，**权限表中不存在**。
→ **薪资保密是 HR 系统的基本要求**，当前任何登录用户可直接调 `/hr/salary/payment/page` 拉全员工资。**这条必须最先修**。

**P0-6 · 账号接口返回密码字段**

`SysUser.password`（`core/base/core-base/src/main/java/cn/aiedge/base/entity/SysUser.java:40`）**没有 `@JsonIgnore`**（该文件 `JsonIgnore` 出现 0 次），
而 `GET /api/user/page` 直接返回裸 `SysUser` → **响应体带出密码**（加密后的哈希，但仍属敏感数据泄露）。
→ 修法：字段加 `@JsonIgnore`，或返回专用 VO。

**其它 P1 级共性问题**

| # | 问题 | 说明 |
|---|------|------|
| 7 | **无 DTO / 无 VO / 无参数校验** | hr 侧接口直接收发实体（`@RequestBody HrEmployee` 等），无 `@Valid` |
| 8 | **无导入 / 无导出 / 无统计端点** | hr 模块 3 个控制器都没有 export/import/statistics |
| 9 | **`hr_employee` 没有 `user_id` 字段** | 员工与系统账号 `sys_user` **完全无关联** → 离职不会封号，无员工自助 |
| 10 | **`hr_position.current_count`（在岗人数）从不维护** | 全代码库无写入 → 编制管控形同虚设 |
| 11 | **`MetaObjectHandler` 只填时间与租户** | `MyBatisPlusConfig.java:172-198` → `create_by`/`update_by` **恒 NULL** |
| 12 | **无任何 `@Scheduled`** | 无合同到期提醒、无试用期到期提醒、无考勤月结、无薪资自动发放 |
| 13 | **课程/异动能力完全缺失** | **无 `hr_transfer` 类异动表、无异动类型枚举、无生效日期概念** —— 入职/转正/调岗/调薪/晋升/离职 只能覆盖式改主档，**改完查不到历史** |
| 14 | **菜单 `menu_level` 未正确回填** | HR 各页为 `0`，而 CRM 的叶子菜单是 `3` |
| 15 | **所有 HR 表 0 行** | 仅 `sys_department` 有 2 行种子（财务部 D9001 / 市场部 D9002，均 `status=0` 禁用）→ **任何 UI 验收必须先造数** |
| 16 | **无模块级 E2E、无验收截图** | `tools/` 下无 `e2e-hr*.cjs`；截图资产目录不存在 |

### 3.3 后端能力盘点

| 域 | 控制器 | 端点 | 表（列数 / 行数） |
|----|--------|:----:|------------------|
| 岗位 | `HrController` | 5 | `hr_position`(17 / 0) |
| 员工 | `HrController` | 6 | `hr_employee`(29 / 0) |
| 合同 | （仅 1 个读端点） | 1 | `hr_contract`(17 / 0) |
| 考勤 | `HrController` | 3 | `hr_attendance`(16 / 0) |
| 请假 | `HrController` | 4 | `hr_leave_request`(19 / 0) |
| 薪资 | `HrController` | 5 | `hr_salary_structure`(21 / 0)、`hr_salary_payment`(21 / 0) |
| 绩效 | `HrController` | 3 | `hr_performance`(21 / 0) |
| 招聘 | `HrRecruitmentController` | 6 | `hr_recruitment`(28 / 0) |
| 候选人 | `HrCandidateController` | 6 | `hr_candidate`(29 / 0) |
| 部门 | `DepartmentController`（**系统管理域**） | 11 | `sys_department`(19 / **2**) |
| 系统岗位 | `PositionController`（系统管理域） | ~15 | `sys_position`(16 / 0)、`sys_position_category`(14 / 0)、`sys_user_position`(5 / 0) |

**合计 HR 侧 38 个端点**，全部无 DTO、无权限注解、无统计、无导入导出。

---

## 4. 与其它模块的接线点

| 方向 | 接线点 | 状态 |
|------|--------|------|
| HR 岗位 → 系统账号 | `hr_position` ↔ `sys_user_position` | ❌ **无关联表/字段** |
| 员工 → 系统账号 | `hr_employee.user_id` | ❌ **字段不存在** |
| 招聘 → 员工 | 候选人 `status=7 已入职` → 创建 `hr_employee` | ❌ **完全断开**（`applicant_count`/`hired_count` 也从不维护） |
| 请假 → 考勤 | 批准后写入 `hr_attendance` 标记休假 | ❌ **无联动** → 请假期间可能被当 `ABSENT` **误扣工资** |
| 考勤 → 薪资 | `selectByEmployeeAndMonth` | ⚠️ 链路存在，但 mapper **无绑定** + 考勤表恒空 |
| 绩效 → 薪资 | `performanceAmount` 取薪资结构**固定基数** | ⚠️ **与绩效结果完全无关**（绩效形同虚设） |
| 部门 → HR | `hr_employee.dept_id` / `hr_position.dept_id` / `hr_recruitment.dept_id` | ⚠️ 仅逻辑引用，**无外键**；部门种子仅 2 条且禁用 |
| 请假审批 → 工作流引擎 | `hr_leave_request.workflow_instance_id` | ❌ 字段存在但**全代码库无任何赋值/读取** |

> ⚠️ **本表是 2026-09-18 实施前的基线**。其中 5 处「❌ 完全断开」已于第二轮打通，
> 2026-09-23 第三轮审计又逐条复核 —— **当前口径以 §9.5 为准**（本表保留作历史对照）。

---

## 5. 业界对标口径（本模块无对标页）

### 5.1 为什么不能照 ql361 抄

ql361（来肯企汇）是**快消品进销存 + 商城**系统，**没有人资域** —— 它连"员工"都只是系统操作员。本模块**必须**按业界成熟 HR 产品的能力集建模。

### 5.2 参照系统与主责能力

| 系统 | 官方文档入口 | 本模块主要参照点 |
|------|-------------|-----------------|
| **Odoo 18** | `https://www.odoo.com/documentation/18.0/` | 员工主档字段级模型、部门、岗位、人事合同、考勤与加班审批、休假三件套（额度/累积/审批）、薪资规则引擎、招聘与应聘者、绩效评估 |
| **SAP SuccessFactors / S/4HANA HCM** | `https://help.sap.com/` | **Job/Position/Employee 三层模型**、编制管控与超编拦截、**人事措施（Personnel Action）**、时间管理与假期额度、Payroll Area/Wage Type/**Payroll Control Record**/追溯计算、RBP 权限、Recruiting、Performance & Goals/Calibration |
| **金蝶 s-HR / 云·星瀚 HR** | `https://vip.kingdee.com/` | **人事快速异动**（入职/转正/调动）、组织/岗位/编制、薪资项目与公式、脱敏开关 `desensitize_enable` |
| **用友 YonSuite 人力云 / YonBIP** | `https://success.yonyou.com/` | 调动办理、转正办理、**编制管理（维度编制 + 控编方式）**、审批流挂接 |
| （补充）**Workday / BambooHR** | `https://doc.workday.com/`、`https://help.bamboohr.com/` | Job management vs Position management、假期累积与结转、考勤异常处理、招聘全流程、目标管理（⚠️ **"补卡单"与 Calibration 本次未取到官方佐证**，见 §5.5） |

### 5.3 借什么、不借什么（沿用 2026-09-15 已裁定口径）

1. **借**：做法、字段、交互形态（状态机、卡片式布局、步骤条、向导弹窗、二次确认、幂等约束、重试分流、启停/测试态切换）。**卡片式也是交互，要借鉴。**
2. **实现**：能用现成组件就用现成组件；现成组件不满足**可以新建**；但**颜色、样式、字体必须与本系统现有代码统一**。
3. **留痕**：官方文档引用保留 —— 标题 + URL + 章节 + 查阅日期，并标注口径来源是**官方文档**还是**行业实践**。
4. **不借**：业界界面的**视觉样式**（配色/圆角/阴影/字体/组件外观皮肤）；**文档中不使用业界系统截图**。
5. **不足处按业界补齐**：无对标页 → 按业界成熟经验做，但**不发明本系统无法支撑的字段**（无数据源的能力如实标注为缺口，不造假数据）。

### 5.4 对标取证底稿（本模块的"事实源"）

> **底稿**：本目录 **[`人力资源-业界做法调研.md`](./人力资源-业界做法调研.md)**（2026-09-18 调研）
> —— 含 **Odoo 18 / SAP / 金蝶 / 用友 / Workday / BambooHR** 五块共 **7 章**：
> `0 结论摘要(10条)` · `1 Odoo 18` · `2 SAP` · `3 金蝶` · `4 用友` · `5 通用 HR 工程实践` · `6 未找到官方文档佐证的能力清单` · `7 来源汇总（按系统分组）`。
> 各页文档「业界对标」章的**参考来源行只写官方文档根地址**（避免深链失效），**逐条精确出处以底稿为准**。

**证据可信度分级**（底稿通用口径，引用时必须带级别）：

| 级别 | 含义 |
|:----:|------|
| **A** | 厂商官方产品文档 / 官方源码（含官方文档源文件、官方 OpenAPI/SDK 文档） |
| **B** | 厂商官方社区 / 官网解决方案文章 / 授权服务商 |
| **C** | 第三方教程 / 博客 |
| **D** | 推论或未证实（**文档中一律不得作为"业界标准"引用**） |

**底稿结论摘要（10 条，逐条可溯源）**：

| # | 结论 | 级别 |
|:-:|------|:----:|
| 1 | **三层模型是分水岭**：SAP/Workday 把「职务 Job → 职位 Position（编制单元）→ 员工 Employee」拆成三个对象；Odoo 只有「岗位 `hr.job` + 员工 `hr.employee`」两层，**没有 Position 层** | A |
| 2 | **编制是"职位上的 FTE 容量 + 校验规则"**，不是员工表上的一个数字：SAP 用 `targetFTE` + `positionControlled` 做超编拦截；用友/金蝶用「组织编制/维度编制/控编方式(允许/不允许超编)」做事前+事中管控 | A/B |
| 3 | **人事异动被建模成一等公民**：SAP 叫 **Personnel Action**（一个动作 = 一组预定义 infotype 的连续维护，写入 `Actions infotype (0000)`）；用友叫「变动类型+变动原因+生效日期+申请单→审批→回写任职记录」；金蝶叫「人事快速异动」 | A/B |
| 4 | **"生效日期 + 历史版本"是 HR 数据的通用底座**：SAP EC 的 MDF 对象带 `effectiveStartDate`/`effectiveEndDate`/`mdfSystemVersionId`；用友调动单必须选生效日期；金蝶提供**修订(Revise)**与历史版本查询 | A/B |
| 5 | **考勤是"打卡明细 + 异常清单"两件事**：Odoo 的考勤错误定义为「签到后 24 小时内未签退」或「单次签到超过 16 小时」，异常标红且需有权限者修改/删除后才能进入薪资 | A |
| 6 | **加班带审批状态**：`overtime_hours` → `overtime_status`(To Approve) → `validated_overtime_hours`，即「加班申报→审批→计入」 | A |
| 7 | **假期额度三件套**：额度分配(Allocation) + 累积计划(Accrual) + 审批方式(Validation)；Odoo 明确区分「结转时点」「是否按工时累积」「结转规则（不结转/上限/封顶）」；SAP 2006 区分「**有效期**」与「**扣减期**」并支持**负额度** | A |
| 8 | **薪资通行架构**：结构类型 → 结构 → 规则 → 参数 → 输入项；规则带类别（基本/津贴/扣款/社保/个税）、序号、条件、计算方式，并把**借贷科目挂在规则上**；各国差异由**本地化包**承载而非写死 | A |
| 9 | **SAP 薪资发放有"控制记录"闸门**：`Payroll Control Record` 持 `Payroll area/status/Period/Retroactive Accounting Limit`，按 `Release → Check → Release for Correction → Exit` 流转；发布时**期间号 +1 并锁住影响过去的主数据**，出错人员进 **Matchcode W** 只做更正运行且**期间不变** | A |
| 10 | **薪资保密是显式配置项**：Odoo 用字段级 `groups=` 限制 `hr.group_hr_user`；金蝶有 `desensitize_enable`（脱敏开关） | A |

> **⚠️ 与本模块最相关的三条差距**（对照上表）：
> ① 本系统**没有 Position 层**，且 `quota_count`/`current_count` 无人维护 → 编制管控缺失（对应结论 1/2）；
> ② 本系统**完全没有异动模型与生效日期** → 改主档即覆盖，无历史（对应结论 3/4）；
> ③ 本系统**没有假期额度体系**（无 allocation/accrual/结转）与**薪资规则引擎**（公式写死在 Java）（对应结论 7/8）。

### 5.5 ⚠️ 底稿明确列出的「未找到官方文档佐证」清单（**15 项，引用时必须避让**）

> 摘自底稿 §6。**凡涉及下列能力，文档中不得以"业界标准如此"的口径描述**；需要写时按「本系统需求」或「本次未取到证据」如实标注。

| # | 未能佐证的对象 | 对本模块的影响 |
|:-:|---------------|---------------|
| 1–2 | **北森 / Moka 官方能力文档** | 仅营销站，**底稿完全不引用** |
| 3 | **金蝶 s-HR 的考勤 / 薪资 / 绩效 / 招聘官方文档**（`help.kingdee.com` 连接失败） | **不得**把金蝶写成这几块的出处；底稿 §3 只覆盖组织/岗位/编制/人事异动 |
| 4 | **金蝶薪资项目 / 公式 / 个税 / 社保公积金**官方文档 | 《薪资管理开发文档》的薪资规则对标**只能引 Odoo/SAP** |
| 5–6 | **用友 DHR 独立产品线官方文档**（底稿 §4 全部是 **YonSuite 人力云**） | 引用时须写「YonSuite 人力云」，**不得**写「用友 DHR」 |
| 7–9 | SAP 的 Effective Dating 独立主题页 / RBP 字段级权限 / SuccessFactors Time Account & Accrual 字段 | 「生效日期+版本」「薪资字段级权限」「假期累积规则字段」在 SAP 侧**本次无字段级证据** |
| 10–11 | **Odoo Payroll 的 Enterprise 源码字段清单** / `hr.payslip` 状态机 | 薪资规则引擎的字段名**只能引官方文档正文**；**不得**写 payslip 状态枚举 |
| 12 | **Workday Calibration（校准）官方主题页** | 绩效校准内容**只能写 Goals 部分** |
| 13 | BambooHR 试用期/合同到期提醒 | — |
| **14** | **「补卡单 / 忘打卡补登」的独立单据对象** | 🔴 **Odoo / SAP / Workday 三家官方文档中均未核验到** —— **已在《考勤记录开发文档》中就地订正**为「本系统需求侧建议」，不得称"业界通行做法" |
| 15 | **「薪资保密」的完整权限矩阵** | 只有 Odoo 字段级 `groups=`（A，源码）与金蝶 `desensitize_enable`（A）两点可引；**不得**展开写"业界通用的薪资可见范围矩阵" |

---

## 6. 写作与开发原则

- **两类配置严格分开**：查询条件/功能按钮 →「页面配置」；默认列 + 全量可配置列 →「数据表格列配置」。
- **不重复通用规范**：统一指向《对标开发技术参考文档》与《_开发指南-金标准.md》。
- **不发明字段**：列名/按钮逐字取自本系统源码；业界补充项**必须单独标注为"业界建议、本系统未实现"**。
- **页面组件优先复用**、**功能/模块不重复开发**（列配置、分页、上传、地区级联、审批流等一律复用现成组件）。
- **阶段结束前回写开发文档**。
- **【金标准】模块级 E2E** 命名 `tools/e2e-hr.cjs`（当前**尚无**）。
- **【金标准】启动进程随手关**：见 `AI_DEVELOPER_RULES.md` 7.3；按端口/PID 精确 kill，勿用文件名批量匹配。

---

## 7. 全局缺口汇总（P0 → P2）

> ⚠️ **本节是「实施前」的缺口清单（历史对照用）**。逐条的处理结果见 §8.1；仍在的缺口见 §8.5。
> 2026-09-23 第三轮审计新增的两个 P0（**前端菜单可见性口径**、**HR 权限只授超管**）不在本表内
> —— 它们是「实施后」才暴露的，见 §9.1 / §9.2。

| 级别 | 缺口 | 影响 | 处理建议 |
|:----:|------|------|---------|
| **P0** | `HrController` 用裸前缀 `/hr`（其余控制器都是 `/api/...`） | **员工/考勤/请假/薪资/绩效/岗位 6 组端点全部 404**，页面完全打不开 | 改 `@RequestMapping("/api/hr")` |
| **P0** | 招聘页写 `request.get('/api/hr/...')` 叠加 `baseURL='/api'` | 合成 `/api/api/hr/...` → **404** | 去掉视图内的 `/api` 前缀，统一走 `api/hr/index.ts` |
| **P0** | 10 个 Mapper 方法无 SQL 绑定（无 XML、无 `@Select`） | 8 条链路调用即抛 `Invalid bound statement`；**月度薪资静默产出 0 条** | 逐个补 `@Select` 或 XML |
| **P0** | `sys_user_position` 缺 `deleted` 与 `tenant_id` 两列 | `GET /api/position/page` 500（新增第一个岗位后必现） | 补列或改 SQL / 加忽略表 |
| **P0** | `HrController` 无鉴权注解 + HR 权限码 0 条 | **全员工资数据任何人可拉** | 补 `@SaCheckLogin` + `hr:*` 权限码种子 |
| **P0** | `SysUser.password` 无 `@JsonIgnore`，`/api/user/page` 返回裸实体 | **密码哈希随接口外泄** | 字段加 `@JsonIgnore` 或返回专用 VO |
| **P0** | 招聘管理是孤儿菜单（父 900 不存在） | **页面用户不可达** | 挂到存在的分组并修 `menu_level` |
| **P1** | **无异动模型 / 无生效日期 / 无历史版本** | 入职/转正/调岗/调薪/离职 只能覆盖式改主档，**改完查不到历史** | 新建 `hr_transfer`（异动单：类型/原因/生效日期/前后值/审批） |
| **P1** | **无假期额度体系**（无 allocation / accrual / 结转 / 余额校验） | 请假 `days` 靠手填，无余额概念 | 补 `hr_leave_type` + `hr_leave_allocation` + 累积规则 |
| **P1** | **薪资无规则引擎**，公式与比例全写死在 Java（社保 10.5% / 公积金 12% / 起征点 5000 / 加班 1.5 倍） | 加一个工资项就要改表改代码；政策一变全改 | 补 `hr_salary_rule`（类别/序号/条件/计算方式/借贷科目） |
| **P1** | **个税用"月度单独计税"口径** | 与现行**累计预扣法**不符 | 按累计预扣法重写（注：分段数组本身是级距宽度，算术无误，**口径错**） |
| **P1** | **绩效与薪资无联动**：`performance_amount` 取薪资结构固定基数 | 绩效形同虚设 | 绩效等级 → 绩效工资系数 |
| **P1** | **请假与考勤无联动** | 请假期间可能被判 `ABSENT` **误扣工资** | 批准请假时写入考勤的"休假"标记 |
| **P1** | **招聘 → 入职 → 员工档案断开**；候选人 `status=7` 不创建员工 | HR 核心闭环断裂 | 补「转入职」端点，一并写 `hr_employee` |
| **P1** | `hr_position.quota_count`/`current_count` 无人维护 | 编制管控形同虚设；超编不拦截 | 员工增删改时同步维护 + 超编校验 |
| **P1** | 员工与系统账号无关联（`hr_employee` 无 `user_id`） | 离职不封号，无员工自助 | 补 `user_id` 映射 + 生命周期联动 |
| **P1** | 考勤不计算 `late_minutes`/`early_minutes`/`work_hours`，且无考勤规则/工作日历 | 永远判不出迟到早退 | 补班次/工作日历/打卡规则 |
| **P1** | 请假审批单级且审批人无规则；`workflow_instance_id` 未接线 | 无部门主管判定、无多级 | 接入已有工作流引擎 |
| **P1** | 无 DTO/VO/参数校验；无导入导出；无统计端点 | 接口质量与可用性 | 按需补 |
| **P2** | `md:staff-*` 三页与 `views/system/*` 四页**重复实现**，且后者无菜单 | 用户只能用功能最弱的版本 | 裁定收敛关系并统一 `menu_code` 前缀 |
| **P2** | `hr_position`（HR 岗位）与 `sys_position`（系统岗位）两套主数据 | 同一概念两处维护 | 收敛或明确职责边界 |
| **P2** | `hr_contract` 无写端点（`createContract` 未接线） | 合同只能读不能建 | 补 CRUD |
| **P2** | `metaObjectHandler` 不填 `create_by`/`update_by` | 审计字段恒 NULL | 统一审计列填充 |
| **P2** | 无 `@Scheduled` | 无合同到期/试用期到期提醒 | 接入调度任务模块 |
| **P2** | 菜单 `menu_level` 未正确回填 | 层级数据不一致 | 出迁移订正 |
| **P2** | 无模块级 E2E、无验收截图、所有 HR 表 0 行 | 无法回归 | 新建 `tools/e2e-hr.cjs`；验收前先造数 |

---

## 8. 本轮落地情况（2026-09-18 · 金标准实施）

> 施工依据：本目录 10 篇页面开发文档 + 上文 §7 缺口清单。
> 验收：`node tools/e2e-hr.cjs` → **171/171 通过**（端点可达性 / 接口语义 / 真库对账 / 状态机 / 跨模块联动 / 安全断言 / 10 页 Playwright 巡检与截屏）。
> 产物：`tool-results/e2e-hr/`（10 张页面截屏 + `e2e-hr-result.txt`）。

### 8.1 P0 逐条处置（§7 全表复核）

| # | 原缺口 | 处置 | 证据 |
|:-:|---|---|---|
| 1 | `HrController` 用裸前缀 `/hr` → 6 组端点全 404 | ✅ 改 `@RequestMapping("/api/hr")` | E2E 第 0 节 24 端点全 200 |
| 2 | 招聘页写 `/api/hr/...` 叠加 baseURL → `/api/api/...` | ✅ 全部改走 `@/api/hr`，视图内零 `'/api/'` 字面量 | E2E 双前缀自查 + UI 页无 4xx |
| 3 | 10 个 Mapper 无 SQL 绑定 | ✅ **删除全部自定义方法**，改 Service 层 `LambdaQueryWrapper` / `UpdateWrapper`（多租户与逻辑删除才会生效） | 月度薪资不再静默 0 条 |
| 4 | `sys_user_position` 缺 `deleted`/`tenant_id` → 岗位列表 500 | ✅ `V11.380.0` 补两列（不加进忽略表，避免丢租户隔离） | E2E 真库核列 + `/api/position/page` 200 |
| 5 | `HrController` 无鉴权 + HR 权限码 0 条 → 工资裸奔 | ✅ `@SaCheckLogin` + 方法级 `@RequiresPermission("hr:*")`；补 **42 条**权限码 | E2E 第 9 节 |
| 6 | `SysUser.password` 无 `@JsonIgnore` | ✅ 已加；`/api/user/page` 不再返回密码 | E2E 断言响应体无 `"password"` |
| 7 | 招聘管理孤儿菜单（父 900 不存在） | ✅ `V11.380.0` 建分组 `61406` 挂接；**`V11.384.0` 补 `client_type`** | E2E 第 9/11 节 |
| 8 | **收尾发现**：`sys_user` 在租户忽略表内且查询**无租户条件** → 任意租户管理员可翻到全库所有租户账号（含按 id 读/改/删/重置密码） | ✅ 口径＝**超管豁免、其余强制限会话租户**：`SysUserServiceImpl.resolveScopedTenantId` + `SysUserController.assertSameTenant` + `/api/v2` 分页补条件 | E2E 第 9 节 7 项断言（超管仍见全部、非超管看不到第二租户、显式传 `tenantId` 也被忽略、按 id 越权 403）。详见《全部操作员开发文档》附节 |

> ⚠️ 第 8 条要特别留意**不要一刀切**：本库 `admin` 是 `is_super_admin=true` + 角色 `SUPER_ADMIN`（`tenant_id=1` = 系统租户 = 平台自身），
> 按「超管豁免」处理后 **`admin` 的可见范围与验证开发的方式完全不变**；被堵住的是「客户 A 的管理员翻到客户 B 的账号」。

> ⚠️ 第 7 条的**二阶陷阱**（本轮实踩，值得记入方法论）：新建菜单只写 `path`/`component`/`parent_id` 是不够的，
> `SysMenuServiceImpl.getUserMegaMenus` 还会按 **`client_type`** 过滤 —— 该列为 NULL 时整支菜单从树上消失，
> **症状与孤儿菜单完全一致**（用户点不到、直连 URL 落 404）。新建菜单必须写 `client_type='tenant-admin'`。

### 8.2 页面外壳（10 个菜单页 → 路线 A）

| 页面 | 原形态 | 现形态 | 列配置 key |
|---|---|---|---|
| 员工列表 | 原生 `a-table` | 路线 A + **左部门树** | `hr-employee-table-columns` |
| 考勤记录 | 原生 `a-table` | 路线 A | `hr-attendance-table-columns` |
| 请假管理 | 原生 `a-table`（无 catch、无 ErrorBoundary → 打不开即白屏） | 路线 A | `hr-leave-table-columns` |
| 薪资管理 | 原生 `a-table` | 路线 A（**双 Tab**） | `hr-salary-{payment,structure}-table-columns` |
| 绩效考核 | 原生 `a-table` | 路线 A | `hr-performance-table-columns` |
| 职位管理 | 原生 `a-table` | 路线 A + **左部门树** | `hr-position-table-columns` |
| 职员部门 | `ARReportPage`（路线 B） | 路线 A + 左部门树 | `md-staff-dept-table-columns` |
| 岗位权限 | `ARReportPage`（路线 B） | 路线 A | `md-staff-role-table-columns` |
| 全部操作员 | `ARReportPage`（路线 B） | 路线 A | `md-staff-all-columns` |
| 招聘管理 | 原生 `a-table` + `Modal` | 路线 A（**双 Tab**） | `hr-recruitment-` / `hr-candidate-table-columns` |

机械自检（10/10 全过）：`ErrorBoundary` + `PageContainer(full-height)` + `CategoryListLayout` + `BillDetailTable|BillTableList` + `StandardPagination(classic)` + `PageConfigPanel`（含 `:default-*-config`）、无 `:max-height`、有 `.table-area` 弹性链、列配置 key 与页面配置 key 不同值。

### 8.3 本轮新增的后端能力

| 能力 | 载体 | 说明 |
|---|---|---|
| **人事异动留痕** | 新表 `hr_employee_change` + `GET /employee-changes/page` | 建档/转正/调岗/调薪/离职自动写前后快照 JSON。对标 SAP Personnel Action / 金蝶「人事快速异动」。**填补了 §3.2 P1「改完查不到历史」** |
| **考勤规则** | 新表 `hr_attendance_rule` + `GET/PUT /attendance/rule` | 上下班时间 / 迟到早退宽限 / 标准日工时。打卡与重算据此计算 `late_minutes`/`early_minutes`/`work_hours` |
| **假期额度** | 新表 `hr_leave_quota` + `GET/PUT /leave/quota`、`GET /leave/balance` | 对标 Odoo 假期三件套中的 **Allocation**；已用天数为实时聚集 |
| **请假 ↔ 考勤联动** | `hr_attendance.leave_request_id` + `leave` 服务 | 批准写入 `LEAVE` 标记、撤销/拒绝还原；薪资只把 `ABSENT` 计缺勤扣款 |
| **累计预扣个税** | `hr_salary_payment.gross_amount` + 重写算法 | 口径从「按月单独计税」订正为**累计预扣预缴法**；累计减除费用按「本单位任职受雇月份数」而非日历月份 |
| **绩效 → 薪资联动** | `hr_performance.performance_coefficient` | `绩效工资 = 绩效基数 × 系数`（未填按 1.0） |
| **招聘 → 入职 → 员工档案** | `POST /candidate/{id}/hire` | 一条动作同时建档 + 置状态 7；`applicant_count`/`hired_count` 自动维护。**填补 §4 的断裂闭环** |
| **工号 / 合同号 / 岗位编码号段** | `biz_number_sequence` 种子 `EMP`/`HT`/`HRPOS` | 杜绝「空工号第二条撞 UNIQUE」 |
| **编制管控** | `HrPositionServiceImpl.refreshCurrentCount` | 员工建档/转正/调岗/离职自动重算 `current_count`；有在岗员工时删除岗位被拒 |
| **状态机 + 字段白名单** | 员工试用(2)→在职(1)→离职(0)；合同/绩效/请假/发放各有状态机 | 非法迁移一律拒绝；表单不能直改状态类字段 |
| **统计 / 导出端点** | `*/stat` 6 个 + 列表扩参 | 员工/岗位/考勤/请假/薪资/绩效/招聘各有统计 |
| **候选人删除端点** | `DELETE /api/hr/candidate/{id}` + `hr:candidate:delete`（`V11.386.0`） | ⚠️ **收尾补漏**：前端候选人 Tab 行内「删除」调的是 `hrCandidateApi.remove`，而该封装**根本不存在** → 点击必抛 TypeError；同时后端也无该端点。已一并补齐（已入职候选人不可删，删除时应聘人数同步 −1） |

### 8.4 数据模型变更

- **迁移**：`V11.380.0__HR_Module_Gold_Standard.sql`（补列 + 3 张新表 + 号段种子 + 42 条权限码 + 招聘菜单挂接）、`V11.384.0__Fix_HR_Recruitment_Menu_ClientType.sql`（补 `client_type`）。
- **新增列**：`hr_employee.user_id/regular_date/resign_type/resign_reason`；`hr_contract.trial_date_end/terminate_reason`；`hr_attendance.leave_request_id`；`hr_salary_payment.gross_amount`；`hr_performance.performance_coefficient`；`sys_user_position.deleted/tenant_id`。
- **新增表**：`hr_employee_change`、`hr_attendance_rule`、`hr_leave_quota`。

### 8.5 仍未闭环（如实登记，勿当成已实现）

> ⚠️ 本清单为 2026-09-18 快照。2026-09-23 第三轮审计复核后的订正：**第 13 条（无业务索引）已过期**
> （实测 39 个索引）；**第 5 条第 9 条仍然成立**（`hr_employee.user_id` 列已建但全仓 0 读 0 写；
> `MetaObjectHandler` 依旧只填 `createTime/updateTime/version/tenantId`，**审计人仍恒 NULL**）。
> 逐条订正见 **§9.4**。

1. **无异动审批流**：`hr_employee_change` 由业务动作自动写入，**没有**「申请 → 审批 → 生效」单据形态；`workflow_instance_id` 字段仍未接线。
2. **假期额度只有 Allocation**：**无 Accrual（按月/按工龄累积）**、**无 Rollover（结转规则）**、**无有效期与扣减期分离**（SAP IT2006）。
3. **无薪资规则引擎**：社保 10.5% / 公积金 12% / 起征点 5000 / 加班 1.5 倍**写死在 Java 常量**；`/salary/setting`、`payroll-run`、`period-control` 未实现。
4. **无考勤班次/工作日历/月结**：考勤规则只有「一条默认班次」，无节假日与调休，无期间锁定；考勤机导入、补卡申请审批闭环未实现。
5. **`sys_user ↔ hr_employee` 仍无映射入口**：`user_id` 列已建，但**无维护界面**，故 ESS（员工自助）与「离职封号」仍无从谈起，考勤页也未做「签到/签退」按钮。
6. **绩效无方案/模板/自评/校准**，且 `hr_performance` **无唯一约束**（同员工同周期可重复提交）。
7. **三组重复实现未收敛**：`md:staff-*` 三页 vs `views/system/{department,position,user,role}` 四页（后者功能更强但**全部无菜单**）；`hr_employee` 的 `list.vue` / `index.vue` / `form.vue` 三套（后两个不可达）；`hr/organization/index.vue` 孤儿页。**本轮按「只重写有菜单的那一页」处理，未动 `views/system/`。**
8. **`hr_position` vs `sys_position` 两套岗位主数据**并存，职责边界仍待裁定。
9. **审计人恒 NULL**：`MetaObjectHandler` 仍只填 `createTime`/`updateTime`/`tenantId`。
10. **无 `@Scheduled`**：合同到期、试用期到期均无自动提醒（`GET /contracts/expiring` 已提供数据，缺调度与推送）。
11. **薪资无字段级脱敏**：靠 `hr:salary:*` 权限码做粗粒度收口。
12. **菜单 `menu_level` 未统一回填**（HR 叶子页为 0，CRM 部分为 3）—— 实测 CRM 自身也混用 0 与 3，故未强行订正，避免引入新的不一致。
13. **无业务索引**：`hr_*` 各表除主键/唯一键外基本无索引；`hr_performance` 无唯一约束。

### 8.6 本模块 E2E 与验收账号

```bash
# 1) 专用验收账号（不要用 admin —— 并行会话同账号二次登录会 sa-token 互踢，
#    表现为「页面跑一半跳登录页 / 接口莫名 401」，极易误判成页面缺陷）
python tools/dbq.py "$(cat tools/e2e-hr-user.sql)"      # e2e_hr / admin123

# 2) 后端（dev profile，默认 5655）；UI 节另需前端 dev server（5656）
#    打包前务必先停掉占用 fat jar 的进程，并 clean install hr-base：
#    否则 IDE 写进 target/classes 的 ECJ 残缺类会被打进 jar，
#    运行时报 java.lang.Error: Unresolved compilation problem
mvn -o -pl hr/hr-base clean install -DskipTests
mvn -o -pl core/api/core-api package -DskipTests

# 3) 跑
node tools/e2e-hr.cjs            # 期望 171/171 通过，退出码 0
SKIP_UI=1 node tools/e2e-hr.cjs  # 只跑接口 + 真库
```

> E2E 内置两条守卫，防止复发：① 扫 `hr-base` jar 的字节码，出现 `Unresolved compilation` 标记即失败（ECJ 残缺类）；② 断言 `/user/page` 响应体不含 `"password"`。
>
> **自愈设计**：脚本第 **-1 节**会在任何断言之前，按 `E2EHR` 前缀清一遍上次异常中断留下的残骸
> —— 脚本跑到一半被中断时第 12 节不会执行，实测跨轮累计出过 27 名员工并污染断言。
> 第 12 节清理分两条通道：**API 删除**（业务允许删的）与 **SQL 兜底**（已确认的考核、已入职的候选人
> 这类设计上拒绝删除的自造数据），跑完实测残留可视行数为 0（其余为逻辑删除行，符合预期）。

### 8.7 深度收口：`sys_user` 移出全局忽略表（2026-09-18 第二轮）

§8.1 第 8 条的修复最初是「逐处补租户条件」（`pageUsers` + 控制器 `assertSameTenant`）。本轮做完**彻底收口**，
改为上游 RuoYi 的做法 —— **`sys_user` 移出 `IGNORE_TENANT_TABLES`，只在登录链路局部放开**：

- 登录发生在认证之前（无会话、无临时租户）→ `AiReadyTenantLineInnerInterceptor.shouldSkip()` 天然返回 true
  → 登录/注册/启动任务照常跨租户可查；**认证之后的常规查询自动带 `tenant_id`**，不必逐处补。
- 新增「会话整体豁免」：`isTenantScopeExempt()` 读 Sa-Token Session 标记，`login` 时写入。
  **平台超管豁免**，故 `admin`（`tenant_id=1` = 系统租户 = 平台自身）的可见范围与验证方式**完全不变**。

🔴 **两个实踩的坑**（详见《全部操作员开发文档》该节）：
1. **拦截器里绝不能实时算角色** —— `StpUtil.hasRole()` → 查角色 SQL → 又过拦截器 → **无限递归**，
   症状是登录直接 `StackOverflowError` / 前端只显示「系统异常」。豁免标记必须**登录时算好写进 Session**。
   **副作用**：旧 token 无此标记，会被按会话租户收敛，**重新登录一次**即恢复。
2. **超管必须整体豁免**，否则给表开了自动注入后 `admin` 也会被收敛到租户 1。

**行为对照（E2E 实测）**：`admin`/`e2e_hr` → 全部租户（不变）；`e2e_hr_ta`(租户1) → 仅租户 1；
`e2e_hr_t2`(租户2) → 仅租户 2 自己那一行且 `/department/tree` 为空。
**回归**：4 账号 × 19 跨模块端点零 5xx；HR E2E **171/171**。

> ⚠️ **执行 `tools/e2e-hr-user.sql` 后若后端在运行，请重启再跑 E2E**：
> 角色/权限有两级缓存（L1 Caffeine 30s + **L2 Redis**），新建角色会被「空角色列表」缓存住，
> 表现为 `/auth/userinfo` 返回 `"roles":[]`、受控端点 403。实测只清 L1 或调
> `/api/user-permission/cache/refresh/{id}` 都不够，**重启后端才干净**。

---

## 9. 第三轮收口：全栈审计与修复（2026-09-23）

> 审计报告：`I:/AI-Ready/HR_MODULE_AUDIT_20260923.md`（含 8 个只读脚本与全部证据）。
> 本节只记**与模块文档口径有关**的结论；§7 / §8.5 里已过期的条目在此一并订正。

### 9.1 🔴 本节要记的第一个坑：菜单可见性**只能有一处实现**

审计发现（运行时实测）：**非超管用户看不到任何 HR 菜单** —— 后端 `/menu/user/mega/...` 已下发，
前端 `MegaMenuPanel` 里还有一份 `userStore.hasPermission(item.menuCode)` 的**全等匹配**副本，
而后端 `MenuPermissionDeriver` 是**前缀派生**（权限码 `hr:employee:list` 会展开出 `hr:employee`）。
两套口径不同源 ⇒ **全库 306 条叶子菜单里只有 4 条的 menu_code 恰好等于某条权限码**，
其余 302 条（含 HR 的 10 条）对非超管**后端已下发、前端又隐藏**，悬停一级菜单只剩列标题。

- **修法**：删掉前端那份副本，**可见性由后端单点决定**；连带删除另两处零调用的同款失效实现
  （`utils/permission.ts#canAccessMenu`、`composables/usePermission#useMenuPermission`，它们查的是
  `menu:${menuCode}` 这种库里根本不存在的码）。
- **教训**：**「权限码 → 菜单码」的口径只允许存在于 `MenuPermissionDeriver` 一处**。
  前端若要再判一次，必须复用同一套前缀规则，否则必然漂移。
- 回归脚本：`node tools/verify-menu-visibility.cjs`（真机，非超管 4 条 / 超管 10 条）。

### 9.2 🔴 第二个坑：HR 权限码此前**只授给了超管**

实测 `hr:*` 40 条**只有 SUPER_ADMIN 持有**（SYSTEM_ADMIN / DEPT_ADMIN / E2E_T2_ADMIN 全 0）
⇒ 非超管即使菜单可见，调任何 HR 接口都是 403。
**历次 E2E 全绿是因为验收账号 `e2e_hr` 挂在 SUPER_ADMIN 上、走 `*` 通配** —— 这是「假绿」的典型形态。

- **修法**：`tools/grant-hr-permissions.py`（幂等、带授权前备份）：
  系统管理员 **37** 条（**排除 `hr:salary:*`**，依据 V11.380.0 权限种子自己的注释
  「薪资保密：与其余 HR 权限分开授予」）；部门管理员 **8** 条只读（同样排除薪资）。
- ⚠️ **改完必须等权限缓存过期（L1 30s + L2 Redis 5min）或重启后端**，否则验证会看到旧结果（§8.7 尾注同理）。

### 9.3 第三个坑：多租户下**建档链路整体不可用**

1. `biz_number_sequence` **全表只有 `tenant_id = 1` 的行**（72 行 / 45 个 biz_type），
   而 `nextNumber` 查不到就抛「未配置编号序列」⇒ 非 1 号租户**员工/合同/岗位都建不出来**。
   **修法**：查不到时按同 `bizType` 的既有行**补种一行**（`seedMissingSequence`，`@InterceptorIgnore` +
   `ON CONFLICT DO NOTHING`，幂等）；`nextNumber(bizType)` 由写死租户 1 改为**会话租户**
   （此前非 1 号租户的销售订单/发票/批次号同样生成不出来）。
2. `hr_employee` 的工号唯一约束是 `UNIQUE (employee_no)`，**不含 tenant_id**，
   而号段是 per-tenant、按天重置、前缀相同 ⇒ 两个租户同一天各建第一个员工都会得到
   `EMP-YYYYMMDD-0001` 撞约束。
   **修法**（迁移 `V11.502.0`）：改 `UNIQUE (tenant_id, employee_no) WHERE deleted = 0`
   —— 顺带与逻辑删除口径对齐（旧索引不含 `deleted`，软删后同工号会被数据库拒、被应用层放行）。

### 9.4 §7 / §8.5 的过期条目（按审计实测订正）

| 原记录 | 实测 | 现结论 |
|---|---|---|
| §8.3「统计 / 导出端点」已交付 | `/stat` 8 个后端已建但**前端 10 个页面 0 处调用**；导出**没有后端端点**（全在前端拼 CSV，见 `employee/list.vue` 等） | 改为「后端能力就绪，前端未展示」 |
| §8.5#13「`hr_*` 各表基本无索引」 | 实测 **39 个索引**，含 `dept_id`/`tenant_id`/业务日期，`hr_leave_quota` 有 `(tenant_id, leave_type, year) WHERE deleted = 0` 部分唯一索引 | 已补齐，删掉该条 |
| §4 接线表 5 处「❌ 完全断开」 | 招聘→入职→档案、请假↔考勤、绩效→薪资、考勤→薪资**均已打通** | 见下方 9.5 |
| §4「`hr_employee.user_id` 字段不存在」 | 字段已建（V11.380.0），但**全仓 0 读 0 写** | 改为「列已建，无维护入口」 |
| §2.4「`views/system/{department,position,user,role}` 四页全部无菜单」 | role/user 已被 80531/80532 指向；`department` 页**已删除**（只剩空目录）；`position` 页 1601 行**失去入口**（V11.429.0 改指角色页后） | 重写该表；`position` 页去留**待裁定** |
| §2.4 行数（user 918 / position 1301 / role 931） | 实际 **1661 / 1601 / 2563** | 更新 |
| `views/md/staff-dept/index.vue` 注释称「后端删除不校验部门引用（P0）」 | `DepartmentServiceImpl.delete` **已实现** `sys_user.dept_id` / `sys_position.dept_id` / `hr_employee.dept_id` 三类引用校验 | 该注释已删除 |

### 9.5 接线状态（审计复核后的真实口径）

| 方向 | 状态 |
|---|---|
| 招聘 → 入职 → 员工档案 | ✅ `POST /api/hr/candidate/{id}/hire` 一个事务内建档 + 置状态 7 + 维护 `applicant_count`/`hired_count` |
| 请假 ↔ 考勤 | ✅ 批准写 `LEAVE` 标记、撤销/拒绝还原（`markLeave`/`unmarkLeave`） |
| 绩效 → 薪资 | ✅ `绩效工资 = 绩效基数 × performance_coefficient`（未填按 1.0） |
| 考勤 → 薪资 | ✅ 缺勤扣款**只统计 `ABSENT`**，`LEAVE` 不扣 |
| 编制管控 | ✅ 建档/转正/调岗/离职自动重算 `hr_position.current_count` |
| 部门 → HR | ✅ HR 侧只读引用 `sys_department`（`HrDepartmentRef`）；反向有删除引用校验 |
| `hr_employee.user_id` → 系统账号 | ❌ 列已建，**无读写入口**（ESS / 离职封号仍无从谈起） |
| 请假审批 → 工作流引擎 | ❌ `workflow_instance_id` 无赋值/读取 |
| 薪资 → 财务 | ❌ 确认发放不生成凭证/应付 |
| 行级数据权限 | ❌ 全站未生效（`sys_data_scope` 0 行、`@DataPermission` 全仓 0 引用）—— 非 HR 单模块问题 |

### 9.6 本轮其它清理

- 删死页面 `views/hr/attendance/index.vue` + 僵尸组件键 `hr/attendance/index`
  （该页签到写死 `clockIn(0)`，会给 employeeId=0 建考勤记录）。
- 删 5 个无调用方的 `listForExport`（考勤/员工/请假/绩效/薪资；招聘那条由 `GET /recruitment/list` 在用，保留）、
  `HrLookupHelper#fillEmployeeInfo`、`HrEmployeeService#getByEmployeeNo`。
- 迁 `V11.502.0`：`907.menu_code` `hr-recruitment` → `hr:recruitment`（与接口鉴权码同源）；
  软删 9071/9072/9073（指向 V11.435.0 已删除的历史权限码）；
  80530/80531/80532/907 的 `menu_level` 由 3 → 0（非系统租户的租户管理员此前看不到这 4 页）。
- 新增权限码 `hr:salary:delete` / `hr:performance:delete`，`HrController` 3 个删除端点改用删除码
  （此前挂 `:update`，导致角色页权限矩阵的**「删除」列永远为空**）。
- `views/system/user/index.vue` 刷新按钮的 `v-permission` 由 `system:user:query`（**库中不存在**，
  fail-closed 导致除超管外所有人看不到该按钮）改为 `tenant-admin:user:list`。

### 9.7 落地后的回归（2026-09-23 真机）

| 回归项 | 结果 |
|---|---|
| 前端单测 `npx vitest run` | **244 passed / 8 skipped** |
| 后端 `mvn -o -pl core/base/core-base install` + `-pl hr/hr-base install` + `-pl core/api/core-api package` | BUILD SUCCESS，fat jar 无 ECJ 残缺类 |
| 模块 E2E `node tools/e2e-hr.cjs` | **174/174 通过，0 失败** |
| 菜单真机 `node tools/verify-menu-visibility.cjs` | **5 PASS / 0 FAIL**（非超管 9 条 / 超管 10 条） |
| 权限真机 `node tools/verify-hr-permissions.cjs` | **6 PASS / 0 FAIL** |

> ⚠️ E2E 顺带订正：`e2e-hr.cjs` 原先对 10 个页面统一断言「`CategoryListLayout` + `.ss-grid` 齿轮」，
> 但 80531/80532 自 V11.422.0/V11.429.0 起指向的是**系统域页面**（那两个 `.vue` 里
> `CategoryListLayout` 出现 0 次）⇒ 这 4 条断言**从 2026-09-19 起就是恒失败的假失败**。
> 现已按页面形态区分 `category` / `system` 两种期望。

> ⚠️ 运行时环境坑（本轮实踩）：我启动的后端实例在运行中被**并行会话重写了同一个 fat jar**
> ⇒ 之后 `/api/auth/login` 全部 500，堆栈 `NoClassDefFoundError: ...logback...ThrowableProxy`。
> **重启即愈，别去查依赖冲突**（见 `fatjar-swap-while-running`）。
