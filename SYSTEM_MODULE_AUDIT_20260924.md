# 系统模块全栈审计报告

> **日期**：2026-09-24　**范围**：系统模块（平台控制台，菜单 `60013` 子树 / 30 篇开发文档 / `client_type=system-admin`）
> **对照基准**：`docs/Yh-Spec/手动整理对标开发文档/系统模块/`（30 篇 + README）、`.../系统菜单设计与管理/`（5 篇）
> **审计对象**：后端 `cn.aiedge.{module,monitor,platform,datasource,devtool,export,tenant,permission,audit,scheduler,cache,dict,config,storage,base,...}`、前端 `frontend/apps/pc-admin/src/views/{admin,system}/**`、devdb 全部系统级表
> **方法**：8 个新增自动化脚本 + 全量 DB 实测 + 真机登录实测 + 2 个子代理代码通读（结论均已逐条复核）+ 重跑既有验收脚本
> **审计性质**：只读。本报告不修改任何代码；修复批次见 §7（待裁决）

---

## 0. 结论摘要

**总体判断：架构与工程规范质量高，"接通率"显著优于其它模块（财务/交易/仓储/分析），但存在 3 类系统性缺口。**

| 维度 | 结论 | 最严重问题 |
|---|---|---|
| **可用性** | 后端良好 / **前端有 P0** | 接口全通、E2E 66/67；但 **`views/system/**` 6 页 `res.data` 死判空致表格/下拉恒空**；慢查询页恒空、告警永不触发 |
| **前端（专项）** | **较大缺口** | **19+ 处 `res.data` 死判空（admin 域已修、system 域漏修）**；6 处假成功（忽略后端 `success:false`）；1 处空按钮；权限页筛选条件完全无效 |
| **规范性** | 良好 | 菜单 `component` 1 处错配靠别名兜底；E2E 断言过期未回写；注释与代码 1 处不一致 |
| **冗余/死代码** | **中等缺口** | **4 套日志后端（38 端点 0 使用）、2 套配置后端、导入导出 15 端点 0 使用**；`cn.aiedge.storage` 整包未装配 |
| **功能接通** | **较大缺口** | **导入"报成功不落库"、导出"下载空文件"、告警规则配了永不触发、租户配置读写皆占位** |
| **跨模块关系** | 良好 | entitlement 门已真接线、5 个业务模块依赖 `SysConfigService` 正常；`/api/import/v2/excel` 是资料模块导入的真实断点 |
| **权限配置** | **较大缺口** | **150 个 `system:*` 权限码 100% 只授超管（非超管全 403）**；**菜单码覆盖率仅 35.7%（196/305 菜单的可见性不受权限控制）**；菜单接口 `tenantId` 默认值=1 是跨租户口径缺陷 |

**若按顺序只做 4 件事**：
1. 🚨 **止血：阻断"保存即清空角色/用户权限"**（`role/index.vue:1747`、`user/index.vue:1184` 先加"回显未成功禁止提交"）— 这是**唯一会破坏存量数据**的缺陷（§2.7）
2. **修 `views/system/**` 的 `res.data` 死判空**（6 个页面现在不可用；修好第 1 条的回显也依赖它）
3. **导入落库**（资料模块客户/往来单位导入会骗过使用者）— `DataImportServiceImpl.java:82-158`
4. **权限只授超管**（系统模块对任何非超管角色不可用，"系统管理员"角色名不副实）

> 另有**零成本**立即项：`system:config:*` / `tenant-admin:user:*` 前后端权限码不一致（§7.5）、6 个空目录可删（P2-14）。

---

## 1. 审计方法与证据链

### 1.1 新增审计脚本（全部可重跑，只读）

| 脚本 | 作用 | 产出 |
|---|---|---|
| `tools/audit-system-spec.py` | 从 30 篇文档抽取「规格头」（路由/菜单ID/menu_code/component/后端前缀/权限码/表） | `tools/system-spec.json` |
| `tools/audit-system-db.py` | devdb 采集：菜单树、权限码、授权、表行数、表结构 | `tools/system-db.json` |
| `tools/audit-system-contract.py` | 后端端点全集 × 前端调用全集对照（处理 baseURL/模板常量/字符串拼接） | `tools/system-contract.json` |
| `tools/audit-system-backend.py` | 系统模块端点清单、未调用端点、无鉴权端点、权限码引用 | `tools/system-backend.json` |
| `tools/audit-system-menu.py` | 菜单 `component` → 前端文件存在性、孤儿页 | `tools/system-menu.json` |
| `tools/audit-system-assembly.py` | `@RestController` 包 vs `scanBasePackages` 覆盖检查 | `tools/system-assembly.json` |
| `tools/verify-system-tenancy.cjs` | **真机实测**：租户2 非超管登录 → 菜单可见性 | stdout |
| `tools/system-peek.py` | 上述 JSON 的查询小工具 | — |

### 1.2 复用的既有资产
- `tools/e2e-system.cjs`（30 页模块级 E2E，写后复原）— 重跑结果见 §2.4
- `tools/audit-api-usage.py`（全仓接口使用率）

### 1.3 关键口径声明（避免误读）
- **前端 axios `baseURL=/api`**：api 封装里写 `/menu/tree` 实为 `/api/menu/tree`，对照时已归一化。
- **`menu_code` ≠ `permission_code`**：前者是菜单编码（`sys_menu.menu_code`），后者是接口权限码（`sys_permission.permission_code`），两者仅约定"前缀可对应"，本报告分别统计。
- **`status` 语义逐表不同**：`sys_permission.status=0` 是"正常"，`sys_menu.status=1` 是"启用"（实体注释相反，历史踩坑）。
- **超管 43 人**：devdb 中 `SUPER_ADMIN` 角色挂 43 个用户 —— 任何"只有超管能过"的验证都会被超管账号伪装成通过，本次已用**非超管租户账号**单独验证（§6.1）。

---

## 2. 维度一：可用性

### 2.1 页面与菜单：35 个叶子菜单全部可达 ✅

菜单树实测（`tools/audit-system-db.py`）：

```
60013 系统 (system-admin, sort=1600, 唯一 system-admin 一级菜单)
├─ 61301 租户管理   62001 租户列表 62002 租户审批 62003 租户套餐 62004 模块授权 62005 配额管理
├─ 61302 模块管理   62101 模块列表 62102 模块版本 62103 模块发布 62104 使用统计
├─ 61303 系统监控   62201 服务状态 62202 性能监控 62203 接口监控 62204 系统日志 62205 操作审计 62206 缓存管理
├─ 61304 数据管理   62301 连接管理 62302 慢查询 62303 备份管理 62304 同步任务 62305 清理规则
├─ 61305 开发工具   62402 模板管理 62403 API文档 62404 API测试 62405 定时任务
├─ 61306 平台设置   62501 平台参数 62502 邮件配置 62503 短信配置 62504 存储配置 62505 安全策略
├─ 61307 系统管理   6130701 菜单管理 6130702 数据字典 6130703 系统配置 6130704 权限配置
└─ 61308 协议契约   62506 平台协议 62507 条款字典维护
```
共 **44 行**（1 顶级 + 8 分组 + 35 叶子）。

- **30 篇开发文档对应的页面 100% 有菜单**（逐条比对 `system-spec.json`）。
- **5 个菜单无对应文档**：`6130702 数据字典`、`6130703 系统配置`、`6130704 权限配置`（系统模块原有页）、`62506 平台协议`、`62507 条款字典维护`（协议模块页挂在系统模块菜单下）→ 建议补文档或在下轮文档中登记。

### 2.2 菜单 → 组件：34/35 直连，1 处靠别名 ✅（有遗留）

| 菜单 | component | 状态 |
|---|---|---|
| 34 个叶子 | `views/admin/**`、`views/system/**` | ✅ 文件存在 |
| **62204 系统日志** | `views/admin/monitor/log`（**文件不存在**） | ⚠️ 靠 `router/dynamicRoutes.ts:240` 别名 `'admin/monitor/log' → views/system/log/index.vue` 兜底 |

> 别名处注释完备（记录了 2026-09-19 删除幽灵文件的过程），**功能不受影响**，但属于"菜单表数据与实现不一致"的技术债。建议把 `sys_menu.component` 改为 `views/system/log/index.vue`，消除对别名的依赖。

**一级菜单结构符合《菜单聚合改造方案 V2》**：实际 14 个（13 业务域 + 工作台 `50010`），被删除的 `60004 配发收 / 60009 商城 / 60016 质量 / 60017 支付` 均已不存在 ✅。

### 2.3 接口连通：系统模块页面域**零断链** ✅

`tools/audit-system-contract.py` 对照结果（method+path 归一化后）：

| 指标 | 数值 |
|---|---|
| 后端端点 | 3562（路径 3101） |
| 前端调用点 | 2780（路径 2166） |
| **系统模块页面域断链** | **0** |

> 注：全站另有 364 条断链，集中在其它 app（`driver-delivery`/`pda-warehouse`/`mobile-*` 打 `/api/v1/delivery/**`、`/api/v1/warehouse/**` 等不存在的路径）—— **不属本次范围**，但与 2026-09-23 DMS 审计记录一致，建议单独立项。

### 2.4 既有验收脚本重跑：66/67 ✅（唯一失败是断言过期）

```
node tools/e2e-system.cjs  →  通过 66 / 失败 1
❌ 菜单数 = 1 顶级 + 7 分组 + 33 页 = 41 — 实际 42
```

原因：菜单后来新增了 `61308 协议契约` 分组 + 2 个协议页，**断言未同步回写**。属脚本维护问题，非产品缺陷。

**其它已验证为真机通过的项**（脚本内断言）：
- 系统日志 `total` 与 DB 一致（api=15276 db=15276）
- 缓存概览为真实 Redis 数据（`totalKeys=67 regions=6`）
- 清理规则新增不再 500 + 真实落库 + 已复原
- 平台参数分页 total 与 DB 一致（173/173）
- 安全策略保存落库（全局 `tenant_id=0`，未产生 `tenant_id=1` 影子行）
- 定时任务分页 total 一致（16/16）
- 权限码种子：`system:menu:*` 7 / `system:module:*` 4 / `datasource:*` 16 / `tenant:menu:*` 3

### 2.5 可用性缺陷（见 §5 清单）
最突出的是**两个"页面能打开但永远没有数据/永远不会响"的功能**：
- **慢查询（62302）**：查询实现真实，**全仓无任何写入方**（`sys_slow_query` 无 INSERT）→ 页面恒空
- **告警（62203 接口监控内的告警区）**：`checkAndAlert` 全仓无调用方 → 规则配了永不触发，`sys_alert_history` 恒空

### 2.6 🔴 `views/system/**` 6 个页面残留 `res.data` 死判空 → 表格/下拉恒空（**前端侧最严重发现，已复核**）

**根因**：`utils/request.ts:198-226` 的响应拦截器**已经拆掉 `Result` 外层**：
```ts
if (isOk) { return (data !== undefined ? data : resData) as any }   // 返回 data 本身，不是 {code,message,data}
```
所以调用方拿到的 `res` **就是业务数据本身**，`res.data` 恒为 `undefined`。

**残留位置**（`grep -rn "res\.data" views/system --include=*.vue` 共 30 命中，其中 2 处是已修的说明注释）：

| 页面 | 行 | 恒不执行的分支 → 后果 |
|---|---|---|
| `system/config/index.vue` | 428、448 | **配置表格永不加载**（`tableData` 恒空）、分组下拉恒空 |
| `system/dict/index.vue` | 838、899、929 | 字典项映射恒空（字典项无法回显） |
| `system/permission/index.vue` | 592、659 | 权限定义树恒空、「上级权限」树恒空 |
| `system/position/index.vue` | 704、893、914、926、1111 | 岗位级别 / 分类 / 部门下拉**恒空**（新增岗位时无从选择） |
| `system/role/index.vue` | 974、1549、1560 | 角色类型下拉、已勾选菜单、单据类型恒空 |
| `system/user/index.vue` | 924、993 | 用户类型下拉、租户列表恒空 |
| `system/log/index.vue` | 10、569 | ✅ 仅注释（该页**已修**，并留了警示） |

**这是"同类修复只做了一半"的典型**：`views/admin/**` 只有 5 处 `res.data`，且**全部是注释或防御性写法**（如 `views/admin/tenant/approval/index.vue:314` 注释原文：「所以这里拿到的 res 已经是数组本身，不能再写 res.data（旧代码写 res.data 恒为 undefined）」）—— **项目自己知道这个坑，admin 域修了、system 域漏了**。

**为什么 E2E 没发现**：`tools/e2e-system.cjs` 直连后端 API 对账，**不经过前端页面**，对纯前端缺陷是盲区。

> 同类（前端不看后端返回的 `success` 而恒弹成功）：`system/config/index.vue:509-527,531-549`（后端 `SystemConfigServiceImpl.java:309-323` 对「删内置配置/删不存在的键」**显式返回 false**，前端照样弹「删除成功」）、`system/dict`、`system/position`、`system/role`、`system/permission`、`system/menu` 各有一处。

### 2.7 🔴🔴 **数据破坏级：回显恒空 + 后端全量覆盖 = 保存即清空已有权限**（已双向复核）

这是 `res.data` 死判空的**最严重后果** —— 不只是"看不到"，而是"**改坏**"：

**路径 A｜角色权限**：`views/system/role/index.vue`
```js
// :1522  回显（读）——res 已是 Result 拆包后的数组，res.data 恒 undefined
const authorized = (res?.data || []) as unknown as Array<string|number>
permCheckedIds.value = new Set(authorized.map(id => String(id)))   // → 恒为空集
// :1747  提交（写）——全量覆盖语义
await roleApi.assignPermissions(roleId, Array.from(permCheckedIds.value))   // → 传空数组
```
```java
// SysRoleServiceImpl.java:80-91（后端）
if (permissionIds == null || permissionIds.isEmpty()) {
    rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
            .eq(SysRolePermission::getRoleId, roleId));     // ← 清空该角色【全部】权限
    ...
    return;
}
```
> **后果**：管理员打开「角色管理 → 功能权限」Tab → 界面显示**一个都没勾**（其实原有权限都在）→ 点保存 → **该角色全部功能权限被静默清空**。同文件 `:1549`（菜单权限 Tab）、`:1560`（单据类型）为同一模式。

**路径 B｜用户角色**：`views/system/user/index.vue`
```js
// :1176-1177  回显
const userRes = await userApi.getById(record.id)
targetRoleKeys.value = (userRes.data as any)?.roleIds ? ... : []    // userRes 是 SysUser，无 data、无 roleIds 字段 → 恒 []
// :1184  提交
await userApi.assignRoles(currentUserId.value, targetRoleKeys.value.map(Number))   // → 传空数组
```
```java
// SysUserServiceImpl.java:358-363（后端）
if (roleIds == null || roleIds.isEmpty()) {
    userRoleMapper.deleteByUserId(userId);   // ← 清空该用户【全部】角色 → 用户失去所有权限
    return;
}
```
> **后果**：管理员打开「用户管理 → 分配角色」→ 复选框全空（原有角色不显示）→ 点确定 → **该用户的全部角色被清空**。

**判定**：这两条是本次审计中**唯一会破坏存量数据**的缺陷，严重度高于"导入不落库"（后者只是不生效，前者是**破坏已有配置**）。且因为回显是空的，操作者**看不出自己删了什么**。

**另一处同类**：`system/user/index.vue:841,869-870`（数据范围列恒显示「未设置」+ 弹窗候选列表恒空 → 该功能完全不可用）、`system/data-import/index.vue:1120-1122`（字段映射静默空表 + `:1171` 保存为全量覆盖 → 点保存清空服务端映射）。

**为什么测试没拦住**（子代理发现，方法论价值高）：`views/system/position/index.test.ts:64-81` 的 mock **凭空造出了真实拦截器永远不会产出的 `data` 字段**（`{ records: [], total: 0, data: true }`、`{ data: [], code: 200 }`），让 `if (res.data)` 分支在测试里"恰好"通过；且断言全是 `toHaveBeenCalled()`，从不校验渲染结果 —— **测试反向掩盖了 bug**。

---

## 3. 维度二：规范性

### 3.1 装配（`scanBasePackages`）：系统模块 5 个包已补齐 ✅

历史 P0（19 控制器/126 端点未装配）已于 2026-09-18 修复。当前 `scanBasePackages` 共 73 条，系统模块相关 `module / monitor / platform / datasource / export / devtool / config` 均在列 ✅。

**仍有 14 个包未装配**（`tools/audit-system-assembly.py`）：

| 未装配包 | 控制器数 | 与系统模块的关系 |
|---|---|---|
| `cn.aiedge.storage.controller` / `.chunk` | 3 | ⚠️ `FileStorageController`(`/api/storage`)、`ChunkUploadController`、`FileAccessController`(`/files/**`) → **16 端点运行期 404**。存储配置页(62504)配的存储后端**没有消费方** |
| `cn.aiedge.report.controller` | 3 | 报表模块（无前端页面） |
| `cn.aiedge.mq.controller` | 2 | 消息队列管理（连接管理页未接） |
| `cn.aiedge.webhook.controller` | 1 | `sys_webhook`/`sys_webhook_log` 表存在但控制器未装配 |
| `cn.aiedge.agent` / `assistant` / `knowledge` / `recommendation` / `search` / `feedback` / `gateway` | 10 | AI 相关（本系统未上线该域） |

> **修正一条旧结论**：`/api/file/**` 的通用上传**并未缺失** —— 由 `cn.aiedge.common.file.FileUploadController` 独立实现（`common/file/FileUploadController.java:74-104`），不依赖未装配的 storage 包。storage 包是**另一套未被采用的实现**。

### 3.2 表结构规范：仅 4 张表超 25 列 ✅

系统级表共 79 张（`sys_*`/`sync_*`），仅 4 张列数 >25：

| 表 | 列数 | 主键 |
|---|---|---|
| `sys_user` | 29 | id |
| `sys_print_task` | 28 | task_id |
| `sys_menu` | 28 | id |
| `sys_system_log` | 26 | id |

全部有主键，无"无主键表"。对照《开发技术规范》「数据库 ≤25 列」，属轻微超标（可接受）。

### 3.3 代码注释与代码不一致（1 处，P2）

`core/base/core-base/src/main/java/cn/aiedge/base/controller/SetMenuConfigController.java:38` 注释写：
```java
* {@code @SaCheckPermission("set:menu-config:view|update")}，并**同批次**补上这两个权限码
```
实际代码用的是 `set:menu-config:list`（:83）与 `set:menu-config:update`（:149）。两个真实权限码**都在库**（status=0）✅ —— 无功能影响，但注释会误导后续排查（本次自动化扫描就把它当成"引用了不存在的权限码"报了 1 条，人工复核后排除）。

### 3.4 文档回写纪律：部分滞后（P2）

- 30 篇文档头部统一标注「已按本文档实施并真机验证（`node tools/e2e-system.cjs`，67/67）」—— 本次重跑为 **66/67**，因菜单新增未同步断言。
- `README.md §10` 有逐页状态表，但未登记 2026-09-19 之后新增的 `61308 协议契约` 分组与 2 个协议页。

---

## 4. 维度三：冗余与死代码

### 4.1 后端端点使用率（`tools/audit-system-backend.py`）

系统模块相关包内 **568 端点 / 490 路径**，其中：

| 分类 | 数量 |
|---|---|
| 被前端调用 | 326 |
| **未被前端调用** | **242** |
| └ 疑似内部/动作/导出类（不应要求前端调用） | 79 |
| └ **冗余候选（需逐个判定）** | **163** |

### 4.2 已确认的重复实现组

| # | 重复组 | 在用 | 未用（死代码候选） | 证据 |
|---|---|---|---|---|
| **A** | **日志后端 4 套** | `SysLogStubController`（`/api/log`，9 端点） | `LogManageController`(`/api/system/log`,13) + `AdvancedLogController`(`/advanced/*`,12) + `StructuredLogController`(`/structured/*`,3) + `SystemLogController`(`/api/logs/system/*`,10) = **38 端点** | `SysLogStubController.java:33-60` 类注释已裁定保留 `LogManageController`「不接线」；另 3 套无任何说明 |
| **B** | 配置后端 2 套 | `SystemConfigController`(`/api/config`，16 端点，全在用) | `SysConfigController`(`/api/system/config`，13 端点，**12 未用**) | 两套操作**两张不同的表**：`sys_config` vs `sys_project_config` |
| **C** | 导入导出 3 套 | `DataImportController`(`/api/import/v2`，仅 `excel/{dataType}` 在用) | `DataExportController`(5) + `DataExportControllerExt`(4) + `BatchImportController`(6) = **15 端点** | 前端仅 2 处调用：`views/md/customer/index.vue:2685`、`views/md/components/PartnerListPage.vue:1111` |
| **D** | 字典后端 3 个控制器 | `DictController`/`DictItemController`/`DictTypeController` 各有在用端点 | 13 个端点未用（如 `/dict/{id}`、`/dict/batch`） | 需逐个判定 |
| **E** | 文件存储 | `cn.aiedge.common.file.FileUploadController`（`/api/file/**`，在用） | `cn.aiedge.storage` 整包（16 端点，**未装配 → 404**） | `AiReadyApplication.java:21-108` 不含该包 |

> **A 组的影响面**：`/api/log`（在用）与 `/api/system/log`（未用）**前缀仅差一段**，且都叫"日志"。新同事极易改错文件（改死代码上的 bug 不会有任何效果）。建议按 `SysLogStubController` 类注释的既定裁定执行：**删除/归档后 3 套**，或至少加 `@Deprecated` + 明确注释。
>
> **B 组的影响面**：`sys_config`(173 行) 与 `sys_project_config`(23 行) 语义重叠，且 `SysConfigController`（未用那套）正是 `SetMenuConfigController`/`UserPageConfigController`/`MenuVisibilityServiceImpl` 在读的那张表的写入方 —— **若要清理 B 组，必须先确认 `sys_project_config` 由谁写**（当前只看到读方，写方可能就是这套"未用"端点）。

### 4.3 前端孤儿页

`tools/audit-system-menu.py` 报 `views/{admin,system}` 下 41 个页面中 9 个"未被 60013 菜单指向"，逐个核实后**全部另有归属，无真孤儿**：

| 页面 | 实际归属 |
|---|---|
| `system/log/index` | 菜单 62204（component 错配，靠别名兜底，见 §2.2） |
| `system/user/index` | 菜单 `80532 全部操作员`（`client_type=tenant-admin`） |
| `system/role/index` | 菜单 `80533 权限配置`、`80531 岗位权限` |
| `system/data-import/index` | 菜单 `80626 外链同步` |
| `system/position/index` | 菜单在 tenant-admin 域（不在 60013 子树） |
| `system/permission/components/SodRulePanel`<br>`system/role/components/*` | 子组件，非页面 |

---

## 5. 维度四：功能是否真接通

> 本节基于逐 Service 实现通读（子代理产出，**关键结论已由我逐条复核**，复核记录见 §5.4）。

### 5.1 真桩清单（功能完全不产生实际效果）— 13 项

| # | 功能 | 位置 | 一句话 |
|---|---|---|---|
| 1 | **导出 Excel** | `export/controller/DataExportController.java:36-49` | 只设响应头，方法体空，`// TODO: 实现真实Excel导出逻辑` **（已复核）** |
| 2 | **导出 CSV** | 同上 `:52-63` | 同上 **（已复核）** |
| 3 | 批量导出 Excel / PDF | `export/controller/DataExportControllerExt.java:110-114` | `fetchData()` 恒 `return List.of()` → 下载文件只有表头 |
| 4 | **导入 Excel/CSV（主链路）** | `export/service/impl/DataImportServiceImpl.java:82-158` | 解析/表头映射/必填校验齐全，**零 insert**，却返回 `ImportResult.success(total, success)` **（已复核）** |
| 5 | 导入 Excel/CSV（旧链路） | `export/controller/DataExportController.java:76-79` | 解析后全部计成功、不落库 |
| 6 | 批量导入错误文件 | `export/service/impl/BatchImportServiceImpl.java:275-281` | 只返回文件名字符串，**从不生成文件** |
| 7 | 依赖服务健康探测 | `monitor/controller/HealthMonitorController.java:253-262` | Redis/MQ 恒 `false`→DOWN，外部 API 恒 `true`→UP（写死常量，唯 DB 是真探测） |
| 8 | 测试通知渠道 | `monitor/controller/AlertManagementController.java:240-254` | 只 `log.info`，回执"测试通知已发送" |
| 9 | **慢查询页** | `datasource/service/impl/SlowQueryServiceImpl.java:22-33` | 查询真实，但**全仓无任何写入方**（`sys_slow_query` 0 行）→ 页面恒空 **（已复核）** |
| 10 | 日志聚合服务 | `monitor/service/LogAggregationService.java`（205 行） | 全仓 0 调用方，读写入口都不存在（死代码） |
| 11 | 租户配置读 | `tenant/controller/TenantController.java:239-247` | 返回硬编码 map（`maxUsers` 恒 100、`themeColor` 恒 `#1890ff`）**（已复核）** |
| 12 | 租户配置写 | 同上 `:257-259` | `// 配置存储暂未实现，返回成功占位` → `Result.ok(true)` **（已复核）** |
| 13 | **告警自动触发** | `monitor/service/impl/AlertRuleServiceImpl.java:91` | `checkAndAlert` 全仓**无任何调用方** → 规则配了永不触发 **（已复核）** |

### 5.2 半桩清单（部分真实）— 10 项

| # | 功能 | 位置 | 边界 |
|---|---|---|---|
| 1 | 告警邮件/短信通知 | `AlertRuleServiceImpl.java:195-203` | email/sms 分支只 `log.info`；仅 webhook 真外发 |
| 2 | 通知渠道配置 | `AlertManagementController.java:33` | `ConcurrentHashMap` 内存态，重启即丢 |
| 3 | 告警规则列表筛选 | 同上 `:82-92` | `wrapper` 构造后未使用 + 内部固定 `enabled=true` → **禁用规则不可见** |
| 4 | 性能监控历史/趋势/预测 | `SystemMonitorServiceImpl.java:28-30,70-74`；`PerformanceMetricsController.java:31-32` | 历史仅进程内且只在访问 `/realtime` 时写入；重启清零、多实例不共享、`hours` 参数被忽略 |
| 5 | 综合健康 components | `HealthMonitorController.java:59-63` | `healthDetails` 取到未用，components 恒空 map |
| 6 | 网络统计 | `InfrastructureMonitorController.java:230-256` | 只有采集时间 + 网卡计数，无吞吐指标 |
| 7 | 安全策略 5 字段 | `base/util/PasswordPolicy.java:23-29` | `lockThreshold`/`lockDuration`/`sessionTimeout`/`ipWhitelist`/`singleDevice` 自述"仍无消费方" |
| 8 | 租户配额 | `tenant/service/impl/TenantQuotaServiceImpl.java:21-57` | CRUD 真落库，但**无任何校验消费**（建用户不查上限）→ 纯展示 |
| 9 | 批量导入进度 | `BatchImportServiceImpl.java:91-144` | 进度真写 Redis，但从不写业务表；且对同一 `InputStream` 读两次 |
| 10 | 文件存储包 | `AiReadyApplication.java:21-108` | `/api/storage/**` 运行期 404（见 §3.1） |

### 5.3 已确认真实（历史问题已修，勿重复排查）

备份管理（真 `pg_dump`/`pg_restore` + 状态机 + 幂等闸门）、同步任务（真投递 + 如实回执"已投递≠已同步"）、清理规则（真分批 `DELETE` + 白名单/正则/存在性/列类型四重校验）、缓存管理（真 Redis `INFO`/`SCAN`，不再返假数据）、接口监控（真 `api_access_log` 分页聚合 + 采集拦截器）、API 文档（真 `/v3/api-docs`）、权限模拟（真接 `StpInterfaceImpl`）、平台参数（真读写 `sys_config`，不再是 JVM 内存假分页）、模块管理（真落库 + entitlement 拦截）、邮件/短信/存储连通测试（真 SMTP / TCP / 写盘探测）、调度任务（真线程池 + `JobHandler` 白名单）、操作日志（真分页/导出/清理）、模板管理（真读写 `dev_template`）、模块 CRUD/版本/发布/使用统计（真聚合）、租户档案/注册/系统重建/过期冻结（真实）。

> **14 项历史桩已被真修** —— 这是本模块与其它模块（财务/交易/仓储）最显著的差别。

### 5.4 我的复核记录（子代理结论不可直接采信）

| 复核项 | 方法 | 结果 |
|---|---|---|
| 导出空实现 | 直接读 `DataExportController.java:34-64` | ✅ 属实（`TODO` 与空方法体俱在） |
| `checkAndAlert` 无调用方 | `grep -rn checkAndAlert` 全后端 | ✅ 属实（仅接口声明 + 实现自身） |
| `sys_slow_query` 无写入方 | `grep -rn sys_slow_query\|SlowQueryMapper` 排除读取侧 | ✅ 属实（0 命中） |
| 租户配置硬编码 | 读 `TenantController.java:228-260` | ✅ 属实 |
| 导入不落库 | 独立读 `DataImportServiceImpl.java:82-158` + grep `insert\|save` | ✅ 属实（**双人双路独立确认**） |
| "权限码缺失 1 种" | 追查 `set:menu-config:view\|update` 出处 | ❌ **我的脚本误报** —— 该串只出现在 javadoc，真实注解是 `set:menu-config:list/update`，两码均在库 |

---

## 6. 维度五：与其他模块的关系

### 6.1 系统模块被其他模块依赖（反向依赖）✅

| 依赖方 | 被依赖方 | 位置 |
|---|---|---|
| 财务·费用审批 | `SysConfigService` | `erp/finance/expensedoc/service/impl/ExpenseApprovalServiceImpl.java:74` |
| 营销·自动化活动 | `SysConfigService` | `erp/marketing/service/impl/AutoCampaignServiceImpl.java:42` |
| 仓储·库存模式 | `SysConfigService` | `erp/stock/controller/InventoryModeController.java:29` |
| 仓储·商品 | `SysConfigService` | `erp/stock/controller/ProductController.java:44` |
| 仓储·库存预警设置 | `SysConfigService` | `erp/stock/service/impl/StockAlertConfigServiceImpl.java:37` |

→ 系统模块（平台参数）是 5 个业务模块的配置来源，**这条链路是活的**（`sys_config` 173 行有数据）。

### 6.2 模块权益门（entitlement）✅ 已真接线

```
sys_tenant_module（平台授权租户可用模块）
   ↓ ModuleEntitlementService（权限码 → sys_module_permission 最长前缀 → 模块）
   ↓ ModuleEntitlementInterceptor（WebMvcConfig.java:45-50，order=3）
@SaCheckPermission 判定之后拦截，未开通 → 403「模块未开通：xxx，请联系平台管理员」
```
- 已挂载，非空壳 ✅（对比 2026-09-19 前的"只声明未入链"）
- 映射表 `sys_module_permission` **仅 47 行**，靠**最长前缀**匹配 → 覆盖 `@SaCheckPermission` 1438 端点 + `@RequirePermission` 18 端点
- 实测数据：`sys_tenant_module` 中**租户1、租户2 各 14 个模块全开**（`sys_module` 共 10 个模块 + `analytics/settings/system/agreement`）

> **注意口径**：`sys_module_permission` 里 **没有 `system` 之外的平台模块映射缺口问题**，但 `system` 模块本身只有 **8 条**权限码映射 —— 意味着系统模块的大部分端点**不受模块门约束**（无映射=放行）。这是"平台功能天然只给平台方"的合理设计，不是缺陷，但需知晓。

### 6.3 跨模块真实断点（1 处，重要）

**`/api/import/v2/excel/{dataType}` 是资料模块导入的实际入口，但它不落库**：

```
views/md/customer/index.vue:2685          ─┐
views/md/components/PartnerListPage.vue:1111┘→ POST /api/import/v2/excel/{customer|partner}
                                                  → DataImportService.importExcel()
                                                  → 解析+校验 → return success(N)  ← 一行未写
```
→ **资料模块（客户/往来单位）的"导入"按钮会提示成功但数据不进库**。这是本次审计中**最容易被使用者当作已完成工作**的缺陷（跨模块影响）。

### 6.4 模块边界与菜单结构 ✅

- 一级菜单 14 个 = 《菜单聚合改造方案 V2》规划的 13 业务域 + 工作台 ✅
- `60013 系统` 是**唯一** `client_type=system-admin` 的一级菜单 ✅（`system-admin` 43 行 / `tenant-admin` 370 行）
- **异常 1 处**：`6130701 菜单管理` 的 `client_type='tenant-admin'`（却挂在 `system-admin` 的 `61307` 下）、`tenant_id=1`（`sys_menu` 中唯一非 0 行）。文档已登记为 P0-16：**超管能看到它，但它管不到自己**（`GET /api/menu/tree` 强制 `eq(tenant_id,0)`）→ 无法在本页编辑/删除该菜单自身。
- **系统模块不在 `sys_module` 清单中**（`sys_module` 只有 10 个业务模块）→ 系统模块**无模块级开关**，永远可用（对平台方而言合理）。

---

## 7. 维度六：权限配置（本次最严重的系统性缺口）

### 7.1 系统模块 150 个权限码 **100% 只授给超级管理员** 🔴

`tools/audit-system-db.py` + `sys_role_permission` 实测：

| 角色 | 权限码总数 | 其中 `system:*` | 用户数 |
|---|---|---|---|
| `SUPER_ADMIN` 超级管理员 | 1835 | **150（全部）** | **43 人** |
| `SYSTEM_ADMIN` 系统管理员 | 385（stock/dms/wms/hr/finance/delivery/md） | **0** | 2 人 |
| `DEPT_ADMIN` 部门管理员 | 175（dms/stock/wms/finance/hr/delivery/md） | **0** | 0 人 |
| 其它 | 0~4 | 0 | — |

**结论**：
1. **任何非超管账号访问系统模块页面接口都会 403** —— 系统模块实际上只有"超管"一个可用身份。
2. **`SYSTEM_ADMIN`（系统管理员）角色名不副实**：它有 385 个业务权限，却没有一个系统模块权限 → 无法承担任何"系统管理"职责。这是角色命名与权限设计的语义错位。
3. **43/47 个用户是超管** —— 与 2026-09-22 采购模块审计、09-23 DMS/分析/财务审计发现的"权限只授超管致 E2E 假绿"是**同一个系统性问题**，本次是第 N 次复现（系统模块 150 码）/ 第五个模块（采购 203、DMS 146、分析 13、交易 91、HR）。

> **行业标准对照**：RBAC 的要求是"角色 = 职责集合，权限按职责分配给角色"。当前实现是"所有平台权限塞给唯一超管角色"，**违反最小权限原则**（无法给运维/客服分配只读的租户查看权），也让审计追溯失去意义（所有平台操作都记在同一个人头上）。

### 7.2 菜单可见性不受权限控制的比例：64.3% 🔴

机制：`SysMenuServiceImpl.getUserMegaMenus` 的过滤① —— 非超管用户按"权限码覆盖的菜单码前缀"过滤菜单（`MenuPermissionDeriver`，2026-09-22 平台-AUTHZ-01），但采用**过渡口径**：

> 菜单码若**不在**权限码库覆盖集合内 ⇒ 视为"权限码库尚未覆盖" ⇒ **保持可见（fail-open）**

实测（`sys_menu` × `sys_permission` 前缀展开）：

| 指标 | 数值 |
|---|---|
| 叶子菜单总数（`menu_type=1`） | 305 |
| `menu_code` 在权限码库中有对应 | **109（35.7%）** |
| **无对应 → 可见性不受权限控制** | **196（64.3%）** |
| 系统模块 35 个叶子中有对应 | **10（28.6%）** |

**后果**：RBAC 的"菜单显示层"对 2/3 的菜单失效。用户可能看到菜单但点进去 403（因为接口鉴权是独立的、真实的）—— **安全上没有越权，但体验与合规上不一致**：行业审计要求"用户可见资源 ⊆ 用户有权限资源"，当前是"可见资源 ⊋ 有权限资源"。

**这不是越权漏洞**（真正的访问控制点在 `@SaCheckPermission`），而是**导航层与授权层不一致**。已由设计者有意选择（注释言明"宁可多显示、不可少显示"），但从规范性/行业标准角度必须登记为缺口。

### 7.3 菜单接口的 `tenantId` 默认值 = 1（跨租户口径缺陷）🟠

```java
// core/base/core-base/.../controller/SysMenuController.java:198-206
@GetMapping("/user/mega/{clientType}")
public Result<List<SysMenu>> getUserMegaMenus(
        @PathVariable String clientType,
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "1") Long tenantId) {      // ← 默认值 1
```
而服务层的 `menu_level` 过滤正是以 `tenantId` 判定"是否系统租户"：
```java
// SysMenuServiceImpl.java:253,277
boolean isSystemTenant = SYSTEM_TENANT_ID.equals(tenantId);        // SYSTEM_TENANT_ID = 1L
if (!isSystemTenant && !isSuperAdmin) { wrapper.eq(SysMenu::getMenuLevel, 0); }
```

**两个后果**：
1. **前端不传 `tenantId` 时，菜单按"系统租户"口径返回** —— 普通租户用户看到的是系统租户视角的菜单（含 `menu_level=1/3` 的行）。本应取**会话租户**。
2. 与 `sys_menu` 中 **65 条 `menu_level=3` 的租户级菜单**叠加后形成**潜在 P0**：一旦此默认值被修正为会话租户，这 65 个页面（**财务 17 页、营销 19 页、商城 13 页、资料 5 页、设置 10 页、交易 1 页**）将对普通租户**整块消失**。

**真机实测**（`tools/verify-system-tenancy.cjs`，租户2 非超管 `e2e_hr_t2`）：

| 账号 | 租户 | 菜单节点数 |
|---|---|---|
| `admin`（超管，系统租户） | 1 | 413 |
| `e2e_hr_t2`（非超管） | 2 | **273** |

65 条 `menu_level=3` 中，租户2 **看不到 12 条**，其中：
- 11 条因**权限码过滤**（该用户权限不覆盖，`menu_code` 在库中有对应码）→ **过滤逻辑正确工作**
- 剩余 53 条因**权限码库无覆盖** → fail-open 可见（§7.2 的同一根因）

**看不到的 12 条**（均为租户级核心页）：
```
财务：80103 提现存现转款  80115 费用单  80116 其他收入  80117 应收应付调整
      80120 会计凭证      80121 月结    80122 对账
资料：80510 客户
设置：80620 菜单配置      80624 企业信息  80625 应用中心  80930 打印设置
```
> 这 12 条**无孪生入口**（每页只有一条菜单），即该租户**完全无法到达**这些页面 —— 除非修正其权限码或菜单可见性口径。

### 7.4 端点鉴权覆盖率：39/568 无任何注解（93.1% 覆盖）

`tools/audit-system-backend.py` 对系统模块 568 个端点扫描结果：**39 个无 `@SaCheckPermission`/`@RequirePermission`/`@SaCheckLogin` 等任何注解**。

逐个判定（全部在 `AuthController`/`ProfileController`/`NotificationController`/`SessionController`/`SysRegionController`/`DictController`/`UserPageConfigController`/`ErrorReportController`/`SseNotificationController`）：

| 端点组 | 数量 | 判定 |
|---|---|---|
| `/auth/**`（login/logout/captcha/refresh/userinfo/tenants/check/login-history） | 9 | ✅ 合理（匿名或仅登录） |
| `/profile*`（资料/密码/偏好/头像） | 6 | ✅ 合理 |
| `/notification/*`（含 `DELETE /notification/{id}`） | 8 | ⚠️ 合理（自助通知），但**需确认按 userId 收敛**（防 IDOR） |
| `/session/current` | 1 | ✅ |
| `/sys/region/tree`、`/children` | 2 | ✅ 公开行政区划 |
| `/dict/{id}`、`/dict/batch` | 2 | ✅ 合理（前端启动加载字典） |
| `/user-permission/current/*` | 4 | ✅ 取自己的权限 |
| `/tenant-registration/register` | 1 | ✅ 租户注册 |
| `/system/user-config/{userId}/{pageKey}`（GET/POST） | 2 | ⚠️ **需确认读写的 userId 与登录人一致**（当前由路径传入） |
| `/error-report`、`/error-report/recent|query|statistics` | 4 | ⚠️ 前端错误上报（合理），但 `/query`、`/statistics` 是管理视角 → 建议加码 |
| `/files/**`、`/storage/*` | — | 未装配（§3.1） |
| `/user/login`、`/user/logout` | 2 | ⚠️ **与 `/auth/login`、`/auth/logout` 重复的登录端点**（两套登录） |

> 建议：`/system/user-config/{userId}/**`、`/notification/**` 两处做一次越权实测（本次未做，列入 §9 待办）。
> `/user/login` 与 `/auth/login` 重复属**冗余**，建议确认后收敛为一套。

### 7.5 前端权限码对账：38 个码全部在库 ✅，但发现 1 组前后端码不一致 🔴

`tools/audit-system-fe-perm.py`：

| 指标 | 数值 |
|---|---|
| `v-permission` / `hasPermission` 引用点 | 50 处 / 10 个文件 |
| 去重后权限码 | 38 个 |
| **库中不存在的** | **0** ✅ |
| 系统模块 38 个页面中使用按钮级权限控制的 | **10 个**（`views/system/**` 9 个 + `admin/dev/scheduler`） |

**发现：`system:config:*` 组前后端命名不一致**

| | 前端使用 | 后端注解使用 | 库中存在 |
|---|---|---|---|
| 查询 | `system:config:query` | `system:config:list`（6 处） | 两者都在 |
| 新增 | `system:config:create` | **无对应注解** | 在 |
| 修改 | `system:config:update` | `system:config:update` | 在 |
| 删除 | `system:config:delete` | **无对应注解** | 在 |

位置：`views/system/config/index.vue:31,140,201,209` vs `config/controller/SystemConfigController.java:189,218,236,249,258,269,284`

**后果**：对非超管角色，**必须同时授 `query` 和 `list`** 才能既看到按钮又调通接口；只授其一会出现"按钮可见但 403"或"接口通但按钮不可见"。属**权限码设计不一致**（P1）。

**同类不一致另确认 3 处**（前端门禁用码 ≠ 后端注解用码，**已逐条双向复核**）：

| 按钮 | 前端码 | 后端真实码 |
|---|---|---|
| 用户·重置密码 | `tenant-admin:user:update`（`user/index.vue:237`） | `tenant-admin:user:reset-password`（`SysUserController.java:194`） |
| 用户·停用/启用 | `tenant-admin:user:update`（`:250`） | `tenant-admin:user:update-status`（`SysUserController.java:256`） |
| 用户·分配角色 | `tenant-admin:role:assign`（`:220`） | `tenant-admin:user:assign-role`（`SysUserController.java:225`） |

**反向缺口（该守的没守）**：`system/log/index.vue:29-34` 导出按钮**无 `v-permission`**，而后端 `/log/export` 要求 `log:oper:export` → 无权限用户可见可点、必然 403；`system/data-import` 全页无按钮门禁（后端要求 `system:dataimport:*`）。

> **口径说明**：`backend/core/base/.../permission-effectivity.json` 中 `system:config:query` / `system:config:create` 的 `refCounts = {backend: 0, frontend: 1}` —— 即这两个码**没有任何后端消费方**，是 `V11.419.0__Register_System_Admin_Orphan_Pages.sql:83-95` 专门为"让按钮可见"补的库记录（该迁移注释也承认后端要的是 `list`/`update`）。

> 对照：`system:dev:scheduler:*` 组**完全一致** ✅（后端用常量 `PERM_LIST = "system:dev:scheduler:list"`，与前端 `PERM.LIST` 同值）—— 这是正确的做法，可作为整改样板。

---

## 8. 缺陷清单

### P0（数据正确性 / 功能完全不可用）

| # | 缺陷 | 位置 | 影响 |
|---|---|---|---|
| **P0-1** | **导入"报成功不落库"** | `export/service/impl/DataImportServiceImpl.java:82-158`（Excel）、`:161+`（CSV） | 资料模块（客户/往来单位）导入提示成功但**数据一行未写**；`export/controller/DataExportController.java:76-79` 旧链路同样全计成功 |
| **P0-2** | **导出"下载空文件"** | `export/controller/DataExportController.java:36-49,52-63`（方法体空）；`DataExportControllerExt.java:110-114`（`fetchData` 恒空列表） | 用户拿到只有表头的 xlsx/csv/pdf，无报错 |
| **P0-3** | **告警规则永不触发** | `monitor/service/impl/AlertRuleServiceImpl.java:91` `checkAndAlert` 全仓 0 调用方 | 告警能力整体不可用；`sys_alert_history` 恒空 |
| **P0-4** | **慢查询页恒空** | `datasource/service/impl/SlowQueryServiceImpl.java:22-33` | 无任何写入方（`sys_slow_query` 0 行），页面永无数据 |
| **P0-5** | **150 个 `system:*` 权限码 100% 只授超管** | `sys_role_permission` 实测 | 非超管访问系统模块全 403；`SYSTEM_ADMIN` 角色无一个系统权限（名不副实） |
| **P0-6** | **租户配置读写皆占位** | `tenant/controller/TenantController.java:239-259` | 读返回硬编码 `maxUsers=100`，写直接 `Result.ok(true)` |
| **P0-7** | 🚨 **保存即清空角色/用户权限**（回显恒空 + 后端全量覆盖） | `views/system/role/index.vue:1522→1747`；`views/system/user/index.vue:1176→1184`（后端 `SysRoleServiceImpl.java:80-91`、`SysUserServiceImpl.java:358-363`） | **唯一会破坏存量数据的缺陷**：管理员点一次保存 → 角色全部功能权限/用户全部角色被**静默清空**，且界面看不出删了什么 |
| **P0-8** | **`res.data` 死判空致 6 个页面表格/下拉恒空** | `views/system/{config,dict,permission,position,role,user}/*.vue`（19+ 处，明细见 §2.6） | 系统配置页/岗位列表/权限定义抽屉/字典项**整块恒空**；数据范围功能完全不可用。**纯前端缺陷，后端 E2E 完全覆盖不到** |

### P1（可用性 / 规范性 / 安全边界）

| # | 缺陷 | 位置 |
|---|---|---|
| P1-1 | 菜单接口 `tenantId` 默认值 `1`（应为会话租户） | `base/controller/SysMenuController.java:203` |
| P1-2 | 65 条租户级菜单 `menu_level=3`（修正 P1-1 后将对普通租户整块消失）；**其中 12 条已实际不可见**（§7.3） | `sys_menu` 数据 |
| P1-3 | 菜单码覆盖率仅 35.7%（196/305 菜单可见性不受权限控制，fail-open） | `MenuPermissionDeriver` 过渡口径 |
| P1-4 | `system:config:*` 前后端权限码不一致（`query` vs `list`，create/delete 后端无注解） | §7.5 |
| P1-5 | 4 套日志后端（38 端点 0 使用） | §4.2-A |
| P1-6 | 2 套配置后端（`SysConfigController` 13 端点 12 未用） | §4.2-B |
| P1-7 | 租户配额只存不校验（无任何 maxUsers/存储上限消费方） | `TenantQuotaServiceImpl.java:21-57` |
| P1-8 | 安全策略 5 字段（锁定阈值/时长/会话超时/IP 白名单/单设备）无消费方 | `base/util/PasswordPolicy.java:23-29` |
| P1-9 | 依赖健康探测硬编码（Redis/MQ 恒 DOWN、外部 API 恒 UP） | `HealthMonitorController.java:253-262` |
| P1-10 | 告警通知 email/sms 只打日志；通知渠道配置仅内存态 | `AlertRuleServiceImpl.java:195-203`、`AlertManagementController.java:33` |
| P1-11 | 告警规则列表**禁用项不可见**（`wrapper` 构造后未用 + 内部固定 `enabled=true`） | `AlertManagementController.java:82-92` |
| P1-12 | 性能监控历史仅进程内（重启清零、多实例不共享、`hours` 参数被忽略） | `SystemMonitorServiceImpl.java:28-30,70-74` |
| P1-13 | `cn.aiedge.storage` 整包未装配（16 端点 404），存储配置配了无消费方 | `AiReadyApplication.java:21-108` |
| P1-14 | `sys_menu` 中 `6130701 菜单管理` 的 `client_type/tenant_id` 异常 → **本页管不到自己** | 文档 P0-16，未修 |
| P1-15 | 批量导入：同一 `InputStream` 读两次（必然失败）+ 错误文件从不生成 | `BatchImportServiceImpl.java:107-114,275-281` |
| P1-16 | **假成功**：前端忽略后端 `{success:false}` 恒弹"删除成功"（配置/字典/岗位/角色/权限/菜单各 1 处） | `system/config/index.vue:509-549` 等 |
| P1-17 | 「编辑」按钮函数体为空（点击无反应） | `system/data-import/index.vue:1155-1157`（**已复核属实**） |
| P1-18 | 权限定义抽屉的 3 个筛选条件完全无效（`defSearchForm` 只写不读） | `system/permission/index.vue:577-624` |
| P1-19 | 字段映射加载失败静默清空 + 提交是**全量覆盖** → 可能把服务端映射清空 | `system/data-import/index.vue:966-970`、`:1171-1193` |
| P1-20 | 使用统计页翻页是空操作（`handlePageChange` 不重新取数） | `admin/module/usage/index.vue:134-141` |

### P2（工程债 / 文档一致性）

| # | 缺陷 | 位置 |
|---|---|---|
| P2-1 | `62204 系统日志` 菜单 `component` 指向不存在的文件（靠别名兜底） | `sys_menu.component` vs `router/dynamicRoutes.ts:240` |
| P2-2 | `tools/e2e-system.cjs` 菜单数断言过期（期望 41，实际 42） | `tools/e2e-system.cjs:74` |
| P2-3 | `SetMenuConfigController.java:38` 注释与代码不一致（`view\|update` vs `list/update`） | javadoc |
| P2-4 | 5 个菜单无开发文档（数据字典/系统配置/权限配置/平台协议/条款字典维护） | `docs/.../系统模块/` |
| P2-5 | README §10 未登记 2026-09-19 后新增的协议分组与 2 页 | `系统模块/README.md` |
| P2-6 | 两套登录端点并存（`/auth/login` 与 `/user/login`） | `AuthController` / `SysUserController` |
| P2-7 | `sys_dict_type` / `sys_dict_item` 均 0 行（数据字典页无数据） | devdb |
| P2-8 | 33 张系统级表 0 行（含 `sys_data_scope`/`sys_field_permission`/`sys_sod_rule`/`sys_job`/`sys_job_log`/`sys_mail_config`/`sys_sms_config`/`sys_storage_config`/`sys_security_policy`） | devdb |
| P2-9 | **测试反向掩盖 bug**：mock 造出真实拦截器永不产出的 `data` 字段，让 `if (res.data)` 在测试中"恰好"通过；断言只 `toHaveBeenCalled()` | `views/system/position/index.test.ts:64-81,149-157` |
| P2-10 | 删除失败的"回滚"用 `password:'123456'` **重建**用户（语义错 + 弱口令 + 新 id 与原行不同） | `system/user/index.vue:1083,1102` |
| P2-11 | 新增同步配置表单默认 `baseUrl: 'https://www.ql361.com'`（写死第三方地址） | `system/data-import/index.vue:723` |
| P2-12 | 租户 ID 回落 `1`（平台租户）——`utils/request.ts:125-134` 已删同类回落，此处是遗留 | `system/permission/index.vue:591,658` |
| P2-13 | 提交时若回显未成功**应先禁止提交**（否则修 P0-7 前每次保存都在清空） | `role/index.vue:1747`、`user/index.vue:1184` |
| P2-14 | 6 个空目录残留（幽灵文件/孤儿页删除后的壳） | `views/admin/monitor/log`、`admin/sys/permissions`、`admin/tenant/permissions`、`system/department`、`system/tenant`、`system/tenant-approval`（已核实无文件、无路由键 → 删除零风险） |

---

## 9. 建议的修复批次（待裁决，本次未动代码）

| 批次 | 内容 | 风险 | 建议 |
|---|---|---|---|
| **批 0a｜🚨 止血（最优先）** | **P0-7 保存即清空权限**：先给 `role/index.vue:1747`、`user/index.vue:1184` 加"回显未成功则禁止提交"的保护，再修回显 | 低（纯前端） | **应立即做**：在做任何其它事之前，先阻断"点一次保存清空一个角色/用户权限"的路径 |
| **批 0b｜前端静默失效（最低风险、最快见效）** | P0-8 `res.data` 死判空（6 页 19+ 处）、P1-16 假成功、P1-17 空按钮、P1-18 筛选无效、P2-9 测试掩盖 | 低（纯前端） | 改动局部、无后端依赖，做完 6 个页面立刻可用 |
| **批 1｜数据正确性** | P0-1 导入落库、P0-2 导出接真实取数、P0-3 告警触发接线、P0-4 慢查询采集方 | 中（写路径） | 优先，逐项配 E2E 断言"写入后回读 DB" |
| **批 2｜权限体系** | P0-5 为 `SYSTEM_ADMIN` 等角色补系统权限（或新增"平台运维/只读"角色）；P1-4 统一 `system:config:*` 码 | 低（种子数据） | 补码铁律：**先在 `sys_permission` 补码、再改注解**（`permission_code` 无唯一约束，须显式给 id） |
| **批 3｜菜单与租户口径** | P1-1 `tenantId` 改会话租户；P1-2 65 条 `menu_level` 归 0 | 中（影响全平台租户菜单） | **必须先做真机回归**（租户2 账号），并同步 `tools/e2e-system.cjs` 断言 |
| **批 4｜死代码收敛** | P1-5/P1-6/P1-13：3 套日志后端、1 套配置后端、storage 包的处置（删除 or 加 `@Deprecated` + 说明） | 低 | 处置前确认无外部调用方（storage 包需先决定"是否采用"） |
| **批 5｜工程债** | P2-1~P2-8 | 低 | 随手可做，建议合并为一次清理提交 |

> **不建议做的事**：在没有真机回归的前提下修改 `menu_level` 或 `tenantId` 默认值 —— 会瞬间改变所有租户的可见菜单。

---

## 10. 附：审计脚本用法

```bash
cd I:/AI-Ready
python tools/audit-system-spec.py        # 文档规格提取
python tools/audit-system-db.py          # DB 事实采集
python tools/audit-system-contract.py    # 后端端点 × 前端调用对照
python tools/audit-system-backend.py     # 端点使用率 / 鉴权覆盖 / 权限码
python tools/audit-system-menu.py        # 菜单 → 组件 → 孤儿页
python tools/audit-system-assembly.py    # scanBasePackages 覆盖
python tools/audit-system-fe-perm.py     # 前端权限码对账
node   tools/verify-system-tenancy.cjs   # 真机：租户菜单可见性
node   tools/e2e-system.cjs              # 既有 30 页模块级 E2E
```

**配套的两份子代理专项报告**（逐行代码通读，结论已由本报告作者复核）：
- `tools/system-audit-backend-report.md` — 后端"真接通"审计（真桩 13 项 / 半桩 10 项 / 已修 14 项）
- `tools/system-audit-frontend-report.md` — 前端可用性审计（假数据 / 未接线 / 静默失效 / 假分页）

**已知解析限制**（不影响结论，但重跑时需知道）：
- 前端路径解析已处理 `baseURL=/api`、模板常量 `${API_BASE}`、字符串拼接、`mutate('post', ...)` 自建封装；**仍无法解析**跨行参数中的路径拼接（如 `/cache/region/` + `encodeURIComponent(x)` + `/keys`），会产出 3 条已知假断链。
- 后端鉴权扫描不解析**常量形式的权限码**（如 `@SaCheckPermission(PERM_LIST)`），会漏计 —— 核对 `scheduler` 包时已人工补齐。

---

## 11. 本轮修复记录（2026-09-26）

> 验证：后端 `./mvnw -pl core/api/core-api -am compile` 通过；前端 `vite build` 通过；`vitest run src/views/system/position/index.test.ts` 13 passed。

### 11.1 批 0a · 止血（数据破坏路径）

| 文件 | 改动 |
|---|---|
| `views/system/role/index.vue` | ① 回显改用 `unwrap()` + `normalizeIdList()`（原先读 `res.data` 恒空）；② 新增 `permTabLoaded/menuTabLoaded/billTypeTabLoaded` 三个「回显是否成功」标记；③ 每次打开弹窗重置标记；④ 加载失败时清空并置 false；⑤ **提交前守卫**：任一维度未加载成功即拒绝保存并提示 |
| `views/system/user/index.vue` | ① 回显改走 `GET /permission/user/{id}/role-ids`（原先读 `SysUser.roleIds`，该字段不存在）；② 新增 `roleModalLoaded`；③ **提交前守卫**：未成功回显则拒绝 `assignRoles` |
| `api/permission.ts` | 新增 `getUserRoleIds(userId)` 封装 |

### 11.2 批 0b · 前端静默失效

| 文件 | 改动 |
|---|---|
| `views/system/config/index.vue` | 表格/分组两个 `res.data` 死判空修复；删除/批量删除改为**按后端返回值判定成败** |
| `views/system/dict/index.vue` | 3 处 `res.data` 死判空修复；删除改为按返回值判定 |
| `views/system/permission/index.vue` | 权限定义树 / 上级树 2 处修复；**新增 `filteredDefData` 本地过滤**（3 个查询条件原先完全无效）；删除改为按返回值判定 |
| `views/system/position/index.vue` | 5 处修复（级别/分类/部门下拉、列表、分类分页）；删除与批量删除改为按返回值判定 |
| `views/system/role/index.vue` | 角色类型下拉修复；删除改为按返回值判定 |
| `views/system/user/index.vue` | 用户类型/租户列表/数据范围摘要/数据范围候选 4 处修复 |
| `views/system/menu/index.vue` | 删除改为按返回值判定 |
| `views/system/data-import/index.vue` | 移除空的「编辑」按钮及其空函数；源系统/单据类型/字段映射加载失败改为**显式报错**；新增 `mappingLoaded` 守卫（未加载成功禁止全量覆盖提交） |
| `views/admin/module/usage/index.vue` | 假分页修复（全量 + `applyPage()` 切片），翻页真实生效 |
| `utils/writeResult.ts` | **新增**共用判定 `isWriteFailed(res)`（兼容裸 boolean 与 `{success:false}` 两种后端表达） |
| `views/system/position/index.test.ts` | mock 改为回放**拦截器拆包后的真实形态**（删掉运行时不会出现的 `data` 字段）—— 此后该测试能真正捕获 `res.data` 回归 |

### 11.3 批 1 · 后端真实功能（部分）

| 项 | 改动 |
|---|---|
| **导入不再骗人** | `views/md/customer/index.vue`、`views/md/components/PartnerListPage.vue` 改调资料模块的**真实**端点 `POST /erp/md/customer/import-excel?partnerType=xxx`（编码生成 + 编号查重 + 真落 `erp_partner`），并展示成功/失败明细；顺带修正旧映射「供应商/物流公司全落成客户类型」的错误 |
| **后端假导入改为明确失败** | `DataImportServiceImpl.importExcel/importCsv` 不再 `return success(N)`，改为返回失败并说明；`DataExportController` 的 `/excel/import`、`/csv/import` 同样改为 `badRequest` |
| **导出不再给空文件** | `DataExportController./excel/export`、`/csv/export` 与 `DataExportControllerExt.fetchData` 改为明确失败（导出数据源 `dataType → 查询实现` 尚未接线） |

### 11.4 仍未处理（待下一轮）

| 项 | 说明 |
|---|---|
| P0-3 告警规则永不触发 | 需在指标采集处接线 `checkAndAlert` |
| P0-4 慢查询无采集方 | 需实现采集（`pg_stat_statements` 或日志） |
| P0-5 权限只授超管 | 需为平台角色补权限种子（**补码铁律：先补 `sys_permission` 再改注解**） |
| P1-1/P1-2 菜单 `tenantId` 默认值 / 65 条 `menu_level=3` | 改动影响所有租户菜单，**需先真机回归** |
| P1-4 前后端权限码不一致（5 组） | 需同步前端门禁码与后端注解码 |
| P1-5/6/13 死代码（3 套日志后端、1 套配置后端、storage 包） | 处置前需确认无外部调用方 |
| P2-9~P2-14 工程债 | 6 个空目录、测试、硬编码等 |

---

## 12. 补做记录（2026-09-26 第二轮）

> 验证：后端 `./mvnw -pl core/api/core-api,core/base/core-base -am compile` 通过；前端此前 `vite build` 通过。

### 12.1 前后端权限码不一致 · 5 组全部对齐 ✅

**修复原则**：以后端注解为准（接口鉴权才是权威），改前端门禁码。

| 按钮 | 原前端码 | 改为（= 后端注解） | 位置 |
|---|---|---|---|
| 系统配置·刷新/查询 | `system:config:query` | `system:config:list` | `views/system/config/index.vue:31` |
| 系统配置·新增 | `system:config:create` | `system:config:update` | 同上 `:109,140`（`POST /config/save` 后端用 update） |
| 用户·重置密码 | `tenant-admin:user:update` | `tenant-admin:user:reset-password` | `views/system/user/index.vue:237` |
| 用户·停用/启用 | `tenant-admin:user:update` | `tenant-admin:user:update-status` | 同上 `:250` |
| 用户·分配角色 | `tenant-admin:role:assign` | `tenant-admin:user:assign-role` | 同上 `:220` |

> 「用户·编辑」按钮保留 `tenant-admin:user:update` —— 与后端 `PUT /user/{id}` 一致，**本来就是对的**，未动。
>
> **验证**（`tools/audit-system-fe-perm.py` + 后端注解全量扫描）：前端引用的 **39 个码全部在库**，
> 且**全部能在后端 `@SaCheckPermission`/`@RequirePermission` 中找到消费方**
> （唯 5 个 `system:dev:scheduler:*` 是常量形式 `PERM_LIST` 等，已人工核对常量值一致）。
> → 前后端权限码现已 **100% 一致**。

### 12.2 死代码收敛 · 采用「标注」而非「删除」

**核查结果**（全仓 `grep`，排除自身与 `target/`）：

| 控制器 | 前缀 | 被 Java 代码引用 | 前端调用 | 处置 |
|---|---|---|---|---|
| `LogManageController` | `/api/system/log` | **0**（3 处命中全是 javadoc） | 0 | 加零调用方标注 |
| `AdvancedLogController` | `/api/system/log/advanced` | 0 | 0 | 加零调用方标注 |
| `StructuredLogController` | `/api/system/log/structured` | 0 | 0 | 加零调用方标注 |
| `SystemLogController` | `/api/logs/system` | 0 | 0 | 加零调用方标注 |
| `SysConfigController` | `/api/system/config` | 0 | 12/13 未用 | **已有完整注释**（标注「无调用方 / 待拍板 / 不要给它造权限码」，见 `MASTER_TODO_20260920.md`） |
| `cn.aiedge.storage` 整包 | `/api/storage`、`/files/**` | 不在 `scanBasePackages` | 0 | `storage/README.md` 顶部加**未装配**警示 + 决策项 |

**为什么标注而不删除**：这 4 个日志控制器的 REST 能力本身可用，且**可能存在本仓之外的调用方**
（客户自建集成、运维脚本）—— 删除是不可逆操作，收益（少 38 个端点）远小于风险。
标注解决的是**真正的危害**：新人改错文件（"改在死实现上，bug 不会消失"）。
已在每个类注释中写明「改日志逻辑前请先确认改的是不是本类」，并指向在用的 `SysLogStubController`。

> 若后续确认无外部调用方，可安全删除这 4 个类（`git revert` 可回滚）。

### 12.3 工程债清理

- **删除 7 个空目录**（幽灵文件 / 孤儿页删除后的空壳，均已核实 0 文件、无路由键）：
  `views/admin/monitor/log`、`views/admin/sys`、`views/admin/sys/permissions`、
  `views/admin/tenant/permissions`、`views/system/department`、`views/system/tenant`、`views/system/tenant-approval`
- `views/{admin,system}` 下现已**无空目录残留**

### 12.4 仍未处理（下一轮，需先拍板或需功能开发）

| 项 | 阻塞原因 |
|---|---|
| P0-3 告警规则永不触发 | 需在指标采集处接线 `checkAndAlert`（功能开发） |
| P0-4 慢查询无采集方 | 需实现采集链路（功能开发） |
| P0-5 权限只授超管 | 需为平台角色补权限种子（**先补 `sys_permission` 再改注解**） |
| P1-1/P1-2 菜单 `tenantId` 默认值 + 65 条 `menu_level=3` | 影响所有租户的可见菜单，**必须先真机回归**（脚本 `tools/verify-system-tenancy.cjs` 已就绪） |
| storage 包 | 需决策：接入并收敛 `/api/file/**`，还是整体删除 |
| SysConfigController | 项目既有待拍板项（`MASTER_TODO_20260920.md`） |

---

## 13. 真机验证结果（2026-09-26 23:xx，后端已换新 jar）

> 验证前提：确认运行中的 `core-api-0.3.24-exec.jar`（22:12 构建 / 23:14 启动）**确含本轮改动**
> （反编译检查 `DataImportServiceImpl`/`DataExportController`/`DataExportControllerExt` 三个类，改动标记均为 True）。

### 13.1 回归：无退化 ✅

```
node tools/e2e-system.cjs → 通过 66 / 失败 1
```
与修复前**完全一致**（唯一失败仍是已知的菜单数断言过期，`tools/e2e-system.cjs:74`）。本轮改动未破坏系统模块。

### 13.2 旧假导入已改为明确失败 ✅（**本轮核心目标达成**）

真机调用 `POST /api/import/v2/excel/customer`（表头正确的测试文件）：
```json
{"success":false,"successCount":0,"totalCount":0,
 "message":"该导入通道尚未实现数据落库，为避免「提示成功但未写入」，本次已中止（文件解析通过 1 条）。资料档案类导入请使用对应的「导入」入口…",
 "failureCount":0}
```
修复前该接口会回执 `success:true` 让使用者以为导入成功。**现已明确拒绝**。验证脚本：`tools/verify-import-real.cjs`。

### 13.3 ⚠️ 新发现：资料模块自带的"真实导入端点"**实测也不落库**

把前端导入指向 `POST /erp/md/customer/import-excel?partnerType=customer` 后实测：

| 检查项 | 结果 |
|---|---|
| HTTP 响应 | 200，`{"total":1,"success":1,"failure":0,"errors":[]}` |
| 库中是否有新行 | **无**（`biz_party` 行数 0→0，按名称/按 id/按 create_time 三种查法均查不到） |
| 用业务 API 反查 | `GET /erp/md/customer/page?keyword=审计验证` → 0 条 |
| 表本身可写吗 | **可写**（psycopg2 手工 INSERT 成功、查回、清理均正常） |
| 源码与运行 jar 是否同源 | 是（源码 09-21，jar 内 `erp-partner-0.3.24.jar` 09-26 23:06 打包） |

**结论**：`MdCustomerController.importExcelOther` 返回"成功 1 条"但数据库没有新行 —— 现象客观存在，**根因未定位**
（代码路径 `partyService.save()` 返回 true 且未被 catch 记为错误，与"未落库"矛盾；需查后端 stdout 的 SQL/事务日志）。
**注意**：该端点在 `erp-partner` 模块，**本轮未改动该模块任何代码**。

### 13.4 报告更正

- §11.3 与 §6.3 中写的"真落 `erp_partner`"**有误**：`Party` 实体 `@TableName("biz_party")`，
  数据实际在 **`biz_party`**（185 行）。`erp_partner` 是 **0 行的废弃表** ——
  `MdCustomerController` 的 `@Tag` 注释已写明"替换旧版 `PartnerController(erp_partner)`"。
- 因此"`erp_partner` 表 0 行"不是缺陷，是**已完成的新旧表切换**。

### 13.5 待办（真机验证暴露的新问题）

| # | 问题 | 说明 |
|---|---|---|
| V-1 | **资料模块导入端点不落库** | 需后端加日志或断点定位（`MdCustomerController.importExcelOther`）；**这是"客户导入真能用"的最后一道坎** |
| V-2 | 前端导入入口已指向真实端点 | 后端修好后**无需再改前端**；但在此之前，用户点导入会看到"导入成功 1 条"而数据没进去 —— 与修复前的危害**同类** |
| V-3 | `MdCustomerController.fromBody` 对 `partnerType` 强制 `(String)` 转型 | 传数字（外部集成常见）会 500 `Integer cannot be cast to String`（已在 `backend/logs/errors/2026-09-26.jsonl` 留证）。建议改为兼容 String/Number |
| V-4 | 新增客户接口参数校验 | 缺 `partnerCode` 等必填时返回 400「请求数据不完整或存在冲突」，文案未指明缺哪个字段 |

---

## 14. V-1 / V-3 处理结果（2026-09-27 凌晨）

### 14.1 V-1「导入不落库」→ **根因查明：非导入功能本身，而是并行会话的 party 双写改造**

**证据链**：

| 时间 | 事实 |
|---|---|
| 09-26 22:10 | 提交 `3b8ccc4a`：**"feat(party): 双写落地 —— biz_party 的写入同步到 party/party_tenant（阶段 3 方案 B 第 2 步）"** |
| 09-26 23:31 | 提交 `c188d5e3`：双写实机验收脚本 |
| 23:14–23:4x | 后端实例 A（PID 47992）：导入端点返回 **200 + `success:1`**，但 `biz_party` 无新行 |
| 09-27 00:00 | target jar **被重建**（含双写改造） |
| 23:47 | 后端实例 B（PID 73868）启动 |
| 之后复测 | 导入端点与新增端点**双双 500**「系统异常」；`pg_stat_user_tables.n_tup_ins` 未变化 ⇒ **INSERT 根本没执行** |

**结论**：`erp-partner` 模块正处于「biz_party → party/party_tenant 双写」改造的**进行中状态**，导入/建档链路因此异常。
这**不是系统模块的问题，也不是本轮改动引入的** —— 本轮我未改 `erp-partner` 任何代码（`MdCustomerController` 源码 mtime 仍是 09-21）。

**建议**：与那个会话协调；在其双写改造收尾后，用 `tools/verify-import-real.cjs` 复验一次即可（脚本会自动清理测试数据）。
**在此之前**，前端导入入口指向的端点不可用 —— 见 §13.3 的风险提示。

### 14.2 V-3 强制转型 500 — **已修复** ✅

`MdCustomerController.fromBody` 等处共 **34 处** `(String) body.get(...)` 强制转型；
外部集成若把「编号」等字段传成数字，会抛 `ClassCastException` → 500
（`backend/logs/errors/2026-09-26.jsonl` 有留证：`class java.lang.Integer cannot be cast to class java.lang.String`）。

**改动**：全部改用**项目自带的** `str(Object)` helper（`MdCustomerController:696`，`v == null ? null : v.toString()`），
**未新增任何方法**。替换后残留强制转型 0 处；`mvn -pl erp/erp-partner -am compile` 通过。

> 注：该文件同时是并行会话双写改造的邻近区域，但其 mtime 为 09-21（未被对方改动），故本次改动无冲突。
