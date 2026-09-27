# 系统模块（平台控制台）前端审计报告

审计范围：`frontend/apps/pc-admin/src/views/admin/**`、`src/views/system/**` 及其 `src/api/**` 依赖。只读调研，未修改任何文件。

---

## 0. 最重要的发现（一句话）

`views/admin/**`（系统模块文档已覆盖的那批页）此前已被逐页修过，**假绿/假数据基本清零**；但 `views/system/**`（config / dict / position / permission / role / user，共 6 页）**整体漏修**，残留 **22 处 `if (res.data)` 死判空**——响应拦截器 `utils/request.ts:203-226` 已把 `Result`/`Page` 拆包，`res.data` 恒 `undefined`，于是这些 `if` 分支永不执行、`catch` 也不触发，**页面静默空表且无任何报错**。这是历史上「系统日志整页恒空」同一类缺陷（`docs/Yh-Spec/.../系统模块/系统日志开发文档.md:602` 有同源取证），`views/system/log/index.vue:570` 已修好并留了警示注释，但同类代码在其余 6 页仍在。

---

## ① 假数据 / 假绿

### 1.1 已修复（读到的，可作为对照基线）
`views/admin/monitor/health/index.vue:508-512` 已改为 `ref<string|null>(null)`，并在 `:657` 明确注释「不做 `|| 'UP'` / `|| 100` 兜底」；`admin/data/*`、`admin/dev/*`、`admin/monitor/*`、`admin/platform/*`、`admin/tenant/*` 共 30 处 `catch` 已改为「清空 + message.error + 页面级错误横幅」，全仓搜索无 `mock/假数据/桩数据` 残留（除注释外）。

### 1.2 假成功（前端不看后端 `success`，恒弹成功）— **确认 bug**

`system/config/index.vue:509-527` 单删：
```js
async onOk() {
  try {
    await configApi.delete(record.configKey)
    message.success('删除成功')      // ← 从不读返回值
```
后端 `SystemConfigController.java:319-328` / `SystemConfigServiceImpl.java:309-323` **显式返回 false**：
```java
if (row == null) { log.warn("删除配置失败：配置键不存在..."); return false; }
if (Boolean.TRUE.equals(row.getSystemConfig())) { log.warn("不能删除内置配置: {}", configKey); return false; }
```
即：**删内置参数 / 删不存在的键 → 后端什么都没删，前端弹「删除成功」**。后端注释本身写着「旧实现恒 true 是假成功」，说明后端已改诚实，前端没跟上。

同类（`batchDelete`，`system/config/index.vue:531-549`）：后端 `SystemConfigServiceImpl.java:358-363` 在 `deletable.isEmpty()` 时 `return false`、并要求「删满请求条数」才 true，前端同样忽略：
```js
await configApi.batchDelete(ids)
message.success('批量删除成功')
```

同类未逐一读后端语义、但已确认「后端返回 `{success:boolean}` 且前端忽略」的位置：
- `system/dict/index.vue:726-733`（类型）、`:894-905`（字典项）← 后端 `DictItemController.java:62-68` 返回 `ApiResponse.success(Map.of("success", result))`
- `system/position/index.vue:1053-1060`、`:1074-1085`
- `system/role/index.vue:1224-1230`
- `system/permission/index.vue:715-722`、`system/menu/index.vue:1255-1262`

（对照：`admin/platform/params/index.vue:666`、`admin/data/cleanup/index.vue:651`、`admin/data/sync/index.vue:819` 都做了**回读校验**并在未生效时提示 —— 系统模块这几页没有这个动作。）

### 1.3 占位默认值（可接受，已显式标注）
`admin/platform/security/index.vue:438-465`：表单初值是 DB DDL 默认常量，但 `loadError` 非空时禁用保存 + 逐区红标（`:464` `saveDisabledReason`）。**结论：不构成假绿**，是本仓允许的做法。

---

## ② 按钮未接线 — **确认 1 处**

`system/data-import/index.vue:414-422`（模板）/ `:1155-1157`（方法）：
```html
<a-button type="link" size="small" @click="editMapping(record)">编辑</a-button>
```
```js
function editMapping(record: any) {
  // 行内编辑，无需弹窗     ← 函数体只有注释，点击无任何反应
}
```
全量脚本扫描（33 个 .vue）确认：**这是唯一一个空函数体按钮**。另外核查「调用了 api 里不存在的方法」——418 处 `xxxApi.method()` 调用全部能解析到 `src/api/**` 的定义，**无此问题**。

---

## ③ 查询条件静默失效

### 3.1 **确认**：权限定义抽屉的 3 个筛选条件完全无效
`system/permission/index.vue:577-583`（筛选字段定义）/ `:585` / `:620-624`：
```js
const defSearchForm = ref({ permissionName: '', permissionType: ..., status: ... })
const handleDefFilterChange = (filters) => {
  if (Object.keys(filters).length === 0) Object.assign(defSearchForm.value, {...})
  else Object.assign(defSearchForm.value, filters)
  fetchDefData()          // ← fetchDefData 只传 tenantId，defSearchForm 全文件再无第二次出现
}
```
`fetchDefData`（`:591`）调用 `permissionApi.getTree(userStore.tenantId || 1)`，**不接受任何筛选参数**；`defSearchForm` 除赋值外零读取（`grep defSearchForm` 仅 4 处，全是写）。用户在「权限名称 / 权限类型 / 状态」三个框里输入 → 无任何效果、无任何提示。

### 3.2 静默失败的加载（无提示，UI 表现为「空」）
| 位置 | 失败后表现 | 影响 |
|---|---|---|
| `system/data-import/index.vue:966-970` | `mappingRows.value = []`，无 message | **危险**：`batchSaveMappings`（`:1171-1193`）是**全量覆盖**提交，静默空表后点保存 → 把服务端映射清空 |
| `system/data-import/index.vue:1262-1272` | `historyRows.value = []`，无 message | 同步历史看起来「无记录」 |
| `system/data-import/index.vue:972-980` | 仅 `console.error(e)` | 源系统下拉恒空 |
| `system/data-import/index.vue:982-987` | 仅 `console.error(e)` | 单据类型勾选项恒空 |
| `system/permission/index.vue:656-661` | 仅 `console.warn` | 新增权限的「上级」树恒空 |
| `system/position/index.vue:701-710` / `:913-918` / `:925-930` | 仅 `console.warn` | 岗位级别 / 分类 / 部门下拉恒空 |
| `system/role/index.vue:1546-1552` | 仅 `console.warn` | 菜单权限回显恒空（另见 ⑤/①） |
| `system/user/index.vue:1174-1179` | 仅 `console.warn` | 角色回显恒空（另见 ⑦） |

### 3.3 UI 提示不一致（P2）
`system/menu/index.vue:802-805` / `:1165` 三条件全部本地过滤（`filteredMenuTree`），功能是好的，但查询区标签/placeholder（`menuName`「请输入菜单名称」等）**没有任何「本地过滤」提示**；对照 `admin/data/backup/index.vue:68` 明确写「请输入备份名称（本地过滤）」。同一仓两套做法。

---

## ④ 假分页 / 假 CRUD

### 4.1 **假分页（确认代码，运行效果为推断）**
`admin/module/usage/index.vue:134-141`（页脚分页器）/ `:279-283`：
```js
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  // 没有 fetchData()，也没有对 tableData 切片
}
```
`StandardPagination` 绑定 `pagination.total`（来自后端 `{records,total}`），而 `BillDetailTable`（`:100`）的 `v-model:data-source="tableData"` 是**全量记录**、其自带分页被 `showPagination` 默认 `false` 关掉（`components/BillFormPage/BillDetailTable/index.vue:490,582`）。推断后果：翻页只改页码高亮，表格行不变（`api/admin.ts:392-395` 的 `moduleApi.usage(days)` 确实不接收分页参数）。**标注：`handlePageChange` 是空操作是「读到的」；「翻页行不变」是「推测的」，需运行复验。**

### 4.2 无分页的大表
- `system/data-import/index.vue:502` 同步历史 `:pagination="false"`，`/v1/sync-history/config/{id}` 无分页参数 → 行数无上限。
- `system/data-import/index.vue:317` 字段映射 `:pagination="false"`（条数受模板限制，风险低）。

### 4.3 本地增删改（**均有落库出口，不算假 CRUD**）
`system/data-import/index.vue:1159-1165`（`splice` 本地删行）、`system/role/components/RoleDataScopeTab.vue:336/348`、`RoleFieldPermissionTab.vue:418/432`（本地 push/splice）——前者由 `batchSaveMappings` 全量提交、后两者由 `role/index.vue:1766-1776` 的底部「保存」全量覆盖提交。已核对存在提交路径，**不认定为假 CRUD**。

---

## ⑤ 权限码清单与不一致

### 5.1 系统模块各页使用的权限码全集（38 个，去重，标注出处）

| 权限码 | 出现文件:行 |
|---|---|
| `system:config:query` | system/config/index.vue:31 |
| `system:config:create` | system/config/index.vue:109,140 |
| `system:config:update` | system/config/index.vue:110,201 |
| `system:config:delete` | system/config/index.vue:111,209 |
| `system:dict:create` | system/dict/index.vue:131,199 |
| `system:dict:update` | system/dict/index.vue:175,232 |
| `system:dict:delete` | system/dict/index.vue:183,240 |
| `system:log` → `log:oper:delete` | system/log/index.vue:42 |
| `system:menu:create` | system/menu/index.vue:19,285 |
| `system:menu:update` | system/menu/index.vue:295 |
| `system:menu:update-status` | system/menu/index.vue:225 |
| `system:menu:delete` | system/menu/index.vue:321 |
| `tenant-admin:role:assign-menu` | system/menu/index.vue:311 |
| `tenant-admin:permission:create` | system/permission/index.vue:45,56,209,297 |
| `tenant-admin:permission:update` | system/permission/index.vue:305 |
| `tenant-admin:permission:delete` | system/permission/index.vue:313 |
| `tenant-admin:sod-rule:create` | system/permission/components/SodRulePanel.vue:19 |
| `tenant-admin:sod-rule:update` | system/permission/components/SodRulePanel.vue:69 |
| `tenant-admin:sod-rule:delete` | system/permission/components/SodRulePanel.vue:77 |
| `tenant-admin:position:create` | system/position/index.vue:123 |
| `tenant-admin:position:edit` | system/position/index.vue:124,183,191,208 |
| `tenant-admin:position:delete` | system/position/index.vue:125,215 |
| `tenant-admin:role:create` | system/role/index.vue:109 |
| `tenant-admin:role:update` | system/role/index.vue:166 |
| `tenant-admin:permission:assign` | system/role/index.vue:176 |
| `tenant-admin:role:delete` | system/role/index.vue:184 |
| `tenant-admin:field-permission:assign` | system/role/index.vue:1011 |
| `tenant-admin:data-scope:assign` | system/role/index.vue:1012 |
| `tenant-admin:record-rule:update` | system/role/index.vue:1014 |
| `tenant-admin:user:list` | system/user/index.vue:35 |
| `tenant-admin:user:create` | system/user/index.vue:114,134 |
| `tenant-admin:user:update` | system/user/index.vue:212,237,250 |
| `tenant-admin:role:assign` | system/user/index.vue:220 |
| `system:simulate` | system/user/index.vue:244 |
| `tenant-admin:user:delete` | system/user/index.vue:257 |
| `system:dev:scheduler:list` | admin/dev/scheduler/index.vue:34,186（常量表 :392-398） |
| `system:dev:scheduler:create` | admin/dev/scheduler/index.vue:19 |
| `system:dev:scheduler:update` | admin/dev/scheduler/index.vue:194 |
| `system:dev:scheduler:delete` | admin/dev/scheduler/index.vue:206 |
| `system:dev:scheduler:execute` | admin/dev/scheduler/index.vue:178 |

（`views/admin` 其余 27 页、`system/data-import` **完全不用权限码**，按钮无任何前端门禁。）

### 5.2 **确认不一致 5 处**（前端门禁用码 ≠ 后端 `@SaCheckPermission` 用码）

| # | 按钮 | 前端码 / 位置 | 后端真实码 / 位置 |
|---|---|---|---|
| a | 系统配置·查询 | `system:config:query` `system/config/index.vue:31` | `system:config:list` `SystemConfigController.java:189,218…` |
| b | 系统配置·新增 | `system:config:create` `system/config/index.vue:109,140` | `system:config:update` `SystemConfigController.java:284,295`（`POST /config/save`） |
| c | 用户·重置密码 | `tenant-admin:user:update` `system/user/index.vue:237` | `tenant-admin:user:reset-password` `SysUserController.java:194` |
| d | 用户·停用/启用 | `tenant-admin:user:update` `system/user/index.vue:250` | `tenant-admin:user:update-status` `SysUserController.java:256` |
| e | 用户·分配角色 | `tenant-admin:role:assign` `system/user/index.vue:220` | `tenant-admin:user:assign-role` `SysUserController.java:225` |

后果双向：只授「后端真码」的人**看不到按钮**；只授「前端码」的人**看得到按钮但点下去 403**。旁证：`backend/core/base/.../permission-effectivity.json` 中 `system:config:query` 与 `system:config:create` 的 `refCounts = {backend: 0, frontend: 1}`，即这两个码**无任何后端消费方**（`V11.419.0__Register_System_Admin_Orphan_Pages.sql:83-95` 是专为「让按钮可见」补的库记录，注释也承认后端要的是 `list`/`update`）。

### 5.3 反向缺口（该守的没守）
- `system/log/index.vue:29-34` 导出按钮**无 `v-permission`**，后端 `/log/export` 要求 `log:oper:export`（`log.ts:167`）→ 无权限用户可见可点、必然 403。
- `system/menu/index.vue` 查询/刷新按钮无 `system:menu:list` 门禁（`SysMenuController.java:82`）；`system/data-import` 全页无门禁（后端要求 `system:dataimport:*`，`admin.ts` 未声明）。

---

## ⑥ 重复实现 / 死文件

### 6.1 **确认可删的死文件：0 个 .vue**
`views/admin/**` + `views/system/**` 共 **42 个 .vue**（38 个 `index.vue` + 4 个 `components/*.vue`），逐个反查引用后**全部被引用**：
- 29 个 admin 页 → `router/dynamicRoutes.ts:112-267` 的 `componentMap` 各有键；
- 4 个组件用相对路径引入：`system/permission/index.vue:535`、`system/role/index.vue:919-921`。

**结论：本仓的 `views/admin/monitor/log/index.vue` 幽灵文件（历史已知项）已被删除**，`router/dynamicRoutes.ts:236-240` 留有别名注释（`'admin/monitor/log' → views/system/log/index.vue`）说明原因。权限页三份孤儿（`admin/sys/permissions`、`admin/tenant/permissions`）也已删。

### 6.2 **确认可删的空目录：6 个**（0 文件，无路由引用）
```
views/admin/monitor/log          ← 幽灵文件删除后残留
views/admin/sys/permissions      ← 权限孤儿页删除后残留
views/admin/tenant/permissions   ← 同上
views/system/department          ← 无页面、无 componentMap 键
views/system/tenant              ← 重复实现删除后残留（V11.419.0 注释：能力已被菜单 62001 覆盖）
views/system/tenant-approval     ← 同上（菜单 62002）
```
证明：`find views/admin views/system -type d -empty` 得到以上 6 个；`grep -n "system/department\|tenant-approval\|admin/sys/permissions" router/dynamicRoutes.ts` **0 命中**。仅为目录，删除零风险。

### 6.3 无未调用但已定义的工具函数
逐文件扫描「函数体为空 / 只有 message / 只有 console / 只有 return」的函数，命中仅 `system/data-import/index.vue:1155 editMapping`（已列入 ②）与各页 `handleError`（ErrorBoundary 回调，正当）。`system/role/index.vue:1911 unwrap()` 确认在 `:2040,2069,2076,2077` 被调用（不是死代码）。

---

## ⑦ 硬编码

| # | 位置 | 内容 | 判断 |
|---|---|---|---|
| a | `system/user/index.vue:1083`、`:1102` | `password: '123456'` | **【高】**「删除失败回滚」时用 `userApi.create({...savedRecord, password:'123456'})` **重建用户**。语义错（不是恢复，是新建）+ 密码被改成弱口令 `123456`，且新建的行 id/关联关系与原行不同。两个位置都是 `executeOptimistic` 的 rollback 回调 |
| b | `system/data-import/index.vue:723` | `baseUrl: 'https://www.ql361.com'` | 新增配置表单默认值写死第三方 URL（`defaultConfigForm`） |
| c | `system/data-import/index.vue:716`、`:1036` | `selectedBillTypes = ['601','604','504','801']` | 写死业务单据类型默认勾选 |
| d | `system/permission/index.vue:591`、`:658` | `permissionApi.getTree(userStore.tenantId \|\| 1)` | 租户 ID 回落 `1`（平台租户）。`utils/request.ts:125-134` 已明确删除同类回落（理由：「回落到平台租户身份」），此处是遗留 |
| e | `system/user/index.vue:1176-1177` | `(userRes.data as any)?.roleIds` | 硬编码了一个**后端不存在的字段**：`/user/{id}` 返回 `SysUser`，`SysUser.java` 34 个字段中无 `roleIds`；后端其实有现成端点 `GET /api/permission/user/{userId}/role-ids`（`PermissionController.java:85-90`），前端 `src/api/**` 从未引用 |
| f | `system/data-import/index.vue:746-753` | `domainCategories` 单据类型分组常量 | 业务值域表，可接受 |

---

## ⑧ 可用性缺陷（P0 / P1 / P2）

> 统一根因：**`res.data` 死判空 22 处**（6 个文件）。成因：`utils/request.ts:203-226` 对 `Array` / `{records,total}` / `Result{code,data}` 三种形态**全部拆包**，运行时 `res` 就是数据本体，`res.data` 恒 `undefined`；`if (res.data)` 为假 → 不赋值、不报错、不设 `hasError`，页面呈现为「业务上真的没有数据」。

### P0（功能/数据正确性，用户必然踩到）
| # | 位置 | 代码 | 实际后果 |
|---|---|---|---|
| P0-1 | `system/config/index.vue:428` vs 后端 `SystemConfigController.java:207-215`（`ResponseEntity.ok(Map.of("records",…))`，**无 code 字段**） | `if (res.data) { tableData.value = res.records; pagination.total = res.total }` | **系统配置页整页恒空**；同一 `if` 里读的正是 `res.records`，说明作者知道取数口径，缺陷只在守卫 |
| P0-2 | `system/position/index.vue:893` | 同上一行模式 | **岗位管理列表恒空**（`positionApi.getPage` → `ApiResponse<PageResult>` 拆包后无 `data`）。同类页 `system/user/index.vue:1001`、`system/role/index.vue:1148` 写的是正确形态 `if (res) { res.records }` |
| P0-3 | `system/permission/index.vue:592,604-606` | `if (res.data) { fixIds(res.data); permissionDefData.value = res.data }` | **权限定义抽屉表格恒空**；`defHasError` 保持 false → 显示「暂无数据」而不是错误态 |
| P0-4 | `system/dict/index.vue:838,899,929` | `if (res.data) { dictItemMap[typeId] = res.data }` | **字典类型展开后字典项恒空**；删除/保存后的刷新也失效。同文件 `:1037` 对**同一接口**写的是正确形态 `Array.isArray(res) ? res : (res?.data ?? [])` |
| P0-5 | `system/user/index.vue:1176-1177` | `targetRoleKeys.value = (userRes.data as any)?.roleIds ? … : []` | **「分配角色」弹窗永不回显已分配角色**（所有复选框空）。保存走 `POST /user/{id}/roles` **全量覆盖**（`SysUserController.java:224`）→ 管理员只勾一个新角色保存，**原有角色被静默清空** |
| P0-6 | `system/role/index.vue:1522` | `const authorized = (res?.data \|\| [])` | **功能权限矩阵勾选恒空**；`:1747` `roleApi.assignPermissions(roleId, Array.from(permCheckedIds))` 为全量覆盖 → 保存即清空该角色全部功能权限。同文件 `:2076-2077` 用 `unwrap()` 正确读取同一族接口 |
| P0-7 | `system/role/index.vue:1549,1560` | `checkedMenuKeys.value = res.data \|\| []` / `billTypeData.value = res.data \|\| []` | 菜单权限、单据类型权限 Tab 回显恒空；`:1754,1761` 同样全量覆盖提交 |
| P0-8 | `system/user/index.vue:841`（列表摘要）、`:869-870`（设置弹窗） | `const map = res?.data \|\| {}` / `scopeTargets = targetsRes.data \|\| []` | **「数据范围」列恒显示「未设置」**；`handleOpenScope` 弹窗的候选对象列表恒空（模板 `:428` 显示 `scopeTargets.length`、`:485` 按 `scopeChecked` 勾选）→ 该功能完全不可用。后端 `UserDataScopeController.java:49,92` 返回 `Result<…>`，拆包后无 `data` |
| P0-9 | `system/data-import/index.vue:1120-1122` | `catch (e) { mappingRows.value = [] }` 无提示 | 字段映射加载失败 → 静默空表；`:1171` 保存为全量覆盖 → **点保存即清空服务端映射** |

### P1（假成功 / 静默失败 / 假分页）
| # | 位置 | 问题 |
|---|---|---|
| P1-1 | `system/config/index.vue:520,540` | 后端返回 `success:false`（内置/不存在）仍弹「删除成功」「批量删除成功」（见 ① 1.2，两端代码均已核对） |
| P1-2 | `system/dict/index.vue:730,896`、`system/position/index.vue:1057,1078`、`system/role/index.vue:1228`、`system/permission/index.vue:718`、`system/menu/index.vue:1257` | 同类「不看 `success` 就弹成功」 |
| P1-3 | `system/user/index.vue:1083,1102` | 删除回滚用 `password:'123456'` **重建**用户（见 ⑦a）：语义错 + 弱口令 |
| P1-4 | `admin/module/usage/index.vue:279-283` | `handlePageChange` 空操作 + 表格数据源是全量 → 假分页（运行效果待复验） |
| P1-5 | `system/data-import/index.vue:979,986,1120,1268` | 四处 `catch` 只有 `console.error` 或设空数组，**零用户提示** |
| P1-6 | `system/permission/index.vue:620-624` | 3 个筛选条件写入 `defSearchForm` 后从不读取 → 查询按钮视觉生效、实际无效 |
| P1-7 | `system/permission/index.vue:599-600` | `item.id = --dupCounter` **把重复雪花 ID 改写成负数**并塞进 `_rawId`；`:718` 删除时用 `record._rawId ?? record.id`。这是为了绕前端精度问题的本地 ID 伪造，副作用是编辑/删除的目标 ID 有歧义（两行可能共享同一负数 id） |
| P1-8 | `system/log/index.vue:29-34` | 导出按钮无权限码（后端需 `log:oper:export`）→ 必然 403 |

### P2（体验 / 一致性）
| # | 位置 | 问题 |
|---|---|---|
| P2-1 | `views/system/position/index.test.ts:64-81` | **测试反向掩盖 bug**：mock 把 `getPage` 造成 `{ records: [], total: 0, data: true }`、`getCategoryList` 造成 `{ data: [], code: 200 }`——凭空造出真实拦截器永远不会产出的 `data` 字段，让 `if (res.data)` 分支在测试里「恰好」通过；且断言全是 `toHaveBeenCalled()`（`:149-157`），从不断言渲染结果。**这就是 P0-2 能长期存活的原因** |
| P2-2 | `system/config/index.vue:637`、`system/dict/index.vue:1104`、`system/position/index.vue:1404`、`system/menu/index.vue:1219`、`system/role/index.vue:2111`、`system/user/index.vue:1356` | 6 页共用一套「`:min-empty-rows="12"` ghost 占位行 + `data-row-key` 反查双击行」的复制粘贴实现（每页 10+ 行注释重复），未抽公共 composable |
| P2-3 | `system/menu/index.vue` 查询区 | 本地过滤无 UI 提示（见 ③ 3.3） |
| P2-4 | `system/data-import/index.vue:502` | 同步历史表无分页（`:pagination="false"`） |
| P2-5 | 表格宽度 | `system/menu`、`system/role` 等页列定义 20+ 列、多列 `fixed:'right'`+`scroll.x`，窄屏横向滚动严重（`system/role/index.vue` 列定义段） |

---

## 补充：我读到的 vs 我推测的

**我读到的（源码 + 后端源码双向核对）**：①②③⑤ 全部条目；P0-1~P0-9 的「`res.data` 恒 `undefined`」结论——对每个接口都读了后端返回类型（`ResponseEntity.ok(Map.of("records"…))` / `ApiResponse<T>` / `Result<T>` 三种）与 `utils/request.ts:203-226` 的三个拆包分支，两两组合的每一种都推出 `res.data === undefined`；`system/log` 的同源缺陷有官方文档与已修代码双重佐证。

**我推测的（未运行复验）**：
- P1-4「翻页后表格行不变」——`handlePageChange` 空操作是读到的，但表格是否另有隐式切片只做了 `BillDetailTable` 的 `showPagination=false` 检查，未运行验证；
- ① 1.2 中 dict/position/role/permission/menu 五处「假成功」——后端返回 `{success:boolean}` 已读实，但**未逐页确认各 service 在何种输入下真的返回 false**（config 那处两侧都已读实）；
- ⑥ 空目录「可删」——无文件、无路由键、无 import 均已核对，但未检查构建配置是否有按目录 glob 的逻辑（`vite.config.ts` 未读）。

**建议修复序**：P0-1~P0-9 全部改成与 `system/log/index.vue:570` / `system/dict/index.vue:1037` / `system/role/index.vue:2040` 一致的写法（直接读 `res.records`，或统一用 `system/role/index.vue:1911 unwrap()`），并同步补 `hasError`/`message.error`；P0-5/P0-6/P0-7 需**同时**给 `assign*` 加「未回显成功前禁止提交」的保护，否则修好回显前每次保存都在清空权限。