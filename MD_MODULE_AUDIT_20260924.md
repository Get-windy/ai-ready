# 资料模块 · 全栈审计报告（2026-09-24）

> **范围**：前端 `frontend/apps/pc-admin/src/views/md/**`（22 个目录 / 29 个 `.vue` / 27,342 行）
> ＋ 被资料菜单引用的 `views/erp/product/**`、`views/erp/pricing/approval/**`、`views/erp/batch/index.vue`；
> 后端命中 **35 个 Controller / 231 个端点**（跨 `erp-partner`、`erp-stock`、`erp-pricing`、`erp-finance`、`wms`、`core-base`）；
> 数据库 **49 张相关表**；菜单 `sys_menu` 资料子树（根 `60011`）**30 条 / 24 个叶子**；代码中在用权限码 **161 个**。
>
> **对标依据**：`docs/Yh-Spec/手动整理对标开发文档/资料模块/`（19 篇）
> ＋ `.../系统菜单设计与管理/`（5 篇）。
>
> **本次审计为只读**：未改动任何业务代码、未改动任何库表数据。新增的只有审计脚本与产物（见 §7）。

---

## 0. 结论摘要

| 维度 | 结论 |
|---|---|
| 菜单连通性 | ✅ **24/24 叶子的 component 与 5 条双入口的 list_path 全部解析到真实 `.vue`**；资料模块内 `menu_code` 无重复 |
| 双入口配置 | ✅ 5 条「添加」型（70501/80510/80511/80512/80513）与《41条双入口菜单的正确配置》规定完全一致 |
| 接口接线 | ⚠️ 19 处前端调用后端不存在；其中**3 处有真实消费方**（客户建档 2 处 + 批次删除 1 处），其余 16 处挂在已失去菜单入口的旧页面上 |
| 主数据导入 | 🔴 客户列表的「导入」走**只校验不落库**的通用端点 `/import/v2/excel/{dataType}`，却提示「导入成功」；同仓已有真实导入端点且**供应商/其他往来单位已切过去**，只有客户页没切 |
| 接口鉴权 | ✅ 被资料页面调用的 **231 个端点 0 个缺鉴权注解**（全仓少数几个 100% 覆盖域之一） |
| 桩代码 | ✅ `views/md/**` 19 页 **0 处** TODO / mock / `Math.random` / 假数据；⚠️ 但 `views/erp/product/form.vue:1934` 有 **1 处占位按钮**（「商品字段及规则配置」点了只弹 `message.info('…待完善')`） |
| **非超管可用性** | ⚠️ 21/24 页接口 403（161 个权限码里 128 个只授 SUPER_ADMIN）。**2026-09-27 按用户澄清的权限模型修正定性**：三层链路（平台授模块权 → 租户超管自动全权 → 租户超管给角色分配）在代码里均已实现，故这不是系统缺陷，而是"租户侧尚未分配"的状态；见 §10.1 |
| **多租户可用性** | 🔴 **5 条核心主数据菜单 `menu_level=3`**，非 1 号租户的菜单接口会强制过滤掉（客户/供应商/物流公司/其他往来单位/仓库规划） |
| **客户建档链路** | ⚠️ 每次新增客户会发 **2 个注定 404 的请求**并被 `catch {}` 静默吞掉；**经复核数据未丢失**（角色/银行信息已随主档写 `biz_party.roles`/`bank_name`/`bank_account`），但死封装与静默吞错会掩盖真实故障 → 已由 P0 下调为 **P1-0** |
| 行级数据权限 | 🔴 「按往来单位」取数仍查**历史空表 `erp_partner`**（0 行），主数据实际在 `biz_party`（153 行）→ 候选列表恒空 |
| 删除引用保护 | 🔴 客户/商品删除**不做任何引用校验**；`hasTransactions` 接口前端零调用，且它查的 `biz_party_transaction` 也是空表 |
| 重复实现 | ⚠️ 「支付账户(80552)」与「银行账户(70540)」共用同一 `finance_account` 表**与同一 `BankAccountService`**（属有意复用的两套视图，非重复代码），但**权限码却有两套**（`finance:account:*` vs `finance:bank-account:*`）→ 授了一个另一个仍 403 |
| 历史遗留 | ⚠️ **6 张 0 行历史表**（`erp_partner*`）＋ 旧往来单位页 `views/erp/partner/{index,detail}.vue`（1,377 行）失去菜单入口却仍被工作台快捷入口指向 |
| 前端死封装 | ⚠️ 资料模块 api 封装 **230 个方法零消费方**（`api/erp/product.ts` 占大头：商品属性 / SKU 规则 / 推荐商品 / 单位管理整块没接 UI） |
| 按钮级权限 | ⚠️ `views/md/**` 19 个列表页 **0 处 `v-permission`**（同仓 `erp/batch` 7 处、`erp/pricing/approval` 3 处） |

**P0 四条、P1 十一条、P2 六条**（另有 §6 由 4 路文档对照产出的增量项 **D-1~D-18** 与文档回写项 **W-1~W-8**），逐条见下。

> ⚠️ **本报告经过一轮自我复核并下调了一条结论**：初判「客户建档两处 404 导致角色/银行数据丢失」为 P0，
> 经回溯 `buildPayload` 与 `MdCustomerController:1456-1459/1508-1510` 后确认 ——
> 角色与银行信息**已包含在客户主档 payload 中随 `partnerApi.create` 落库**（`biz_party.roles` / `biz_party.bank_name` / `biz_party.bank_account` 三列确实存在且被写入），
> 因此**数据不丢**，该问题降为 P1-0（死封装 + 静默吞错）。这条也说明：本仓审计结论必须逐条回溯源码，不能只看断链清单。

---

## 1. P0

### P0-1 🔴 非超管用户下资料模块 21/24 个页面完全不可用（第五例同型缺陷）

> ⚠️ **2026-09-27 定性修正**：本条初判为"系统缺陷（第五例同型）"，经用户澄清权限模型后重新核实 ——
> 正确的模型是「平台给租户授模块使用权 → 租户超管自动持有全部权限 → 租户超管给本租户的角色/员工分配」。
> 三层链路**都已实现**（证据见 §10.1）。因此 `SYSTEM_ADMIN`/`DEPT_ADMIN` 没有资料模块权限，
> 原因是**没有任何人给它们分配过**（本仓只有 `SUPER_ADMIN` 会在启动时被自动重建为全权，见
> `PermissionInitializationConfig.assignAllPermissionsToSuperAdmin()`）—— 不是代码缺陷，是配置状态。
> 下方实测数据仍然有效，只是**结论**从"必须改代码"变为"需要分配，或由产品决定是否预置默认角色权限"。

**权限码授权实况**（`python tools/audit-md-permission.py`）：

| 项 | 数量 |
|---|---|
| 资料模块 24 页实际用到的权限码 | **161** |
| 其中**只授予 `SUPER_ADMIN`** | **128（80%）** |
| 有非超管角色持有 | 33（`md:route-master:*` 9 + `wms:*` 21 + `stock:inventory-mode:*` 3） |

**真机实测对照**（`python tools/audit-md-probe.py`，租户 1）：

| 账号 | 角色 | 结果 |
|---|---|---|
| `e2e_hr_ta` | SYSTEM_ADMIN（非超管） | **200 × 3 / 403 × 21** |
| `e2e_product` | SUPER_ADMIN | **200 × 24 / 403 × 0** |

非超管命中 403 的典型报文（后端明确回报权限码）：

```
[403] 70510 客户        /api/erp/md/customer/page      msg=无权限访问: md:customer:list
[403] 70505 图片管理    /api/erp/md/image/page         msg=无权限访问: md:image:list
[403] 70540 银行账户    /api/erp/finance/bank-account/page  msg=无权限访问: finance:bank-account:list
[403] 70543 会计科目    /api/erp/finance/subject/tree  msg=无权限访问: finance:subject:view
```

只有 3 个接口放行：`/erp/inventory-mode`（`stock:inventory-mode:*`）、`/erp/warehouse/page`（`wms:warehouse:view`）、`/erp/md/route/page`（`md:route-master:list`）。

**这意味着**：全仓 `sys_user` 有效 73 行中 42 行是超管（57%），所以历史 E2E 全绿**完全不能**证明资料模块对真实租户管理员可用。

**与仓储/财务/DMS/HR 四次审计同型**（第五例），修法一致：
1. 新增 `tools/grant-md-permissions.py`，给 `SYSTEM_ADMIN` 授只读+常用写子集、`DEPT_ADMIN` 授只读子集；
2. 或把这些权限码并入「资料域」权限组，交由租户管理员自助分配。

**第二条受影响链路：菜单也会一起消失。** 菜单可见性由「权限码前缀」派生（`SysMenuServiceImpl.java:334-342`）：

```java
if (menuCode != null && !menuCode.isEmpty()
    && knownMenuCodes.contains(menuCode)        // 权限码库里存在以它前缀命中的码
    && !heldMenuCodes.contains(menuCode)) {     // 但当前用户不持有
    blockedByPermission++; continue;            // → 该菜单被隐藏
}
```

`md:*` 权限码只覆盖 9 个业务对象（`customer / expense-type / image / linked-account / other-income /
payment-channel / payment-method / product-price / route-master`），**不含** `md:product`、`md:supplier`、
`md:logistics`、`md:partner`、`md:warehouse-plan`。于是出现**同组内不一致**：

| 菜单 | `menu_code` | 能否命中权限码 | 非超管可见性 |
|---|---|---|---|
| 80510 客户 | `md:customer` | ✅ 命中 `md:customer:*` | **被隐藏** |
| 70505 图片管理 | `md:image` | ✅ | **被隐藏** |
| 70513 互联账号 | `md:linked-account` | ✅ | **被隐藏** |
| 80511 供应商 | `md:supplier` | ❌ 无对应权限码 | fail-open **可见** |
| 80512 物流公司 | `md:logistics` | ❌ | fail-open **可见** |
| 80513 其他往来单位 | `md:partner` | ❌ | fail-open **可见** |
| 80520 仓库规划 | `md:warehouse-plan` | ❌ | fail-open **可见** |

即：**「有权限体系的菜单」被拦、「没权限体系的菜单」反而放行** —— 修 P0-1 之后这条不一致会自然消失，
但 `menu_level`（P0-2）不会 —— 两条链路叠加，客户/供应商等页面在非系统租户下**先**被 `menu_level` 拦掉。

---

### P0-2 🔴 5 条核心主数据菜单 `menu_level=3`，非 1 号租户整块看不到

**实况**（`python tools/audit-md-menu.py`）：

| 菜单 | id | 归属 | `menu_level` |
|---|---|---|---|
| 客户 | 80510 | 资料 → 往来单位 | **3** |
| 供应商 | 80511 | 资料 → 往来单位 | **3** |
| 物流公司 | 80512 | 资料 → 往来单位 | **3** |
| 其他往来单位 | 80513 | 资料 → 往来单位 | **3** |
| 仓库规划 | 80520 | 资料 → 仓库管理 | **3** |

**过滤逻辑**（`backend/core/base/core-base/src/main/java/cn/aiedge/base/service/impl/SysMenuServiceImpl.java`）：

```java
49:   private static final Long SYSTEM_TENANT_ID = 1L;
253:  boolean isSystemTenant = SYSTEM_TENANT_ID.equals(tenantId);
277:  if (!isSystemTenant && !isSuperAdmin) {
278:      wrapper.eq(SysMenu::getMenuLevel, 0);   // ← 非 1 号租户的非超管：只下发 menu_level=0
279:  }
```

**后果**：任何 `tenant_id ≠ 1` 的租户，其租户管理员与普通用户**收不到这 5 条菜单**；而它们恰好是「往来单位」与「仓库管理」两个分组下的核心主数据页（`61102` 分组下 5 个叶子有 4 个命中，整组近乎清空）。

本机 dev 环境的 `admin` 同时满足「系统租户 + 超管」双重豁免（`SysMenuServiceImpl:259` 早退分支），因此本地永远看不到这个现象。

**注**：这与 P0-1 是**叠加**关系 —— 即使补齐了权限码，`menu_level=3` 仍会让非系统租户看不到菜单入口。

---

---

### P0-3 🔴 行级数据权限「按往来单位」取数仍指向历史空表 `erp_partner`

**代码**（`backend/core/base/core-base/src/main/java/cn/aiedge/base/service/impl/UserDataScopeServiceImpl.java`）：

```java
139:  case "partner":
140:      sql = "SELECT t.id AS id, t.partner_name AS name, t.partner_code AS code,"
141:          + " t.partner_category_id ... LEFT JOIN erp_partner_category c ..."
142:          + " FROM erp_partner t "
143:          + " WHERE t.deleted = 0 AND t.tenant_id = ?";
```

**表实况**：

| 表 | 行数 | 说明 |
|---|---|---|
| `erp_partner` | **0** | 历史表，已无实体类、无写入 |
| `biz_party` | **153** | 客户/供应商主数据现落于此（`Party` 实体 `@TableName("biz_party")`） |

同文件 `case "product"` 用的 `erp_product` 是**活表**（83 行），说明这是**单点遗漏**而非整体设计。

**佐证**：`erp-partner` 模块内 `SystemRebuildService.java:318` 已写明「客户/供应商主数据现落在 biz_party（erp_partner 为历史表，已无写入）」。

**后果**：数据权限规则配置页选「按往来单位」维度时，**候选列表恒空**，该维度无法配置。

---

### P0-4 🔴 客户列表的「导入」是**只校验不落库**的假导入，却提示「导入成功」

**前端**（`frontend/apps/pc-admin/src/views/md/customer/index.vue:2685`）：

```js
const res = await request.post('/import/v2/excel/customer', formData, {...})
message.success('导入成功')     // ← 无论有没有落库都提示成功
```

**后端链路**：`DataImportController.importExcel`（`/api/import/v2/excel/{dataType}`）→ `DataImportServiceImpl.importExcel`。
通读该方法（`DataImportServiceImpl.java:82-160`）后确认：它**只做了「读表头 → 逐行读值 → 必填校验 → 返回 `ImportResult.success(totalRows, successCount)`」**，
**全程没有任何 `insert`/`save`/`mapper` 调用** —— 校验通过即报成功，数据一行都不会进库。

**三重后果**：
1. 用户上传 Excel → 提示「导入成功」→ 列表里没有任何新数据（**静默失败**，与 P1-0 同型）；
2. 该端点另有 `@SaCheckPermission("system:import:create")`（`DataImportController.java:45`），非超管会先吃 403；
3. **同仓已有真实导入实现却没用上**：`MdCustomerController.java:1068 POST /api/erp/md/customer/import-excel`（含 `:1012` 模板下载端点），
   且**供应商、其他往来单位已切到真实导入**（`supplier/index.vue:404-410`、`partner/index.vue:423` 走 `BaseDataImportWizard`）—— 只有客户页没切。

**建议**：客户页改用 `BaseDataImportWizard` + `/erp/md/customer/import-excel`（与供应商同口径），
并把通用导入端点（`DataImportController`）在未被任何页面使用前标注为「仅校验」以免误用。

---

## 2. P1

### P1-0 客户建档：每次新增都发 2 个注定 404 的请求，且被 `catch {}` 静默吞掉

**前端调用点**（`frontend/apps/pc-admin/src/views/md/customer/form.vue`）：

```js
1121: async function submitRoles(partnerId: number) {
1122:   const allRoles = ['CUSTOMER', ...otherRoleOptions.value.filter(o => o.checked).map(o => o.value)]
1123:   for (const roleType of allRoles) {
1124:     try { await partnerRoleApi.addRole(partnerId, roleType, roleType === 'CUSTOMER') } catch {}
1125:   }
1126: }
1128: async function submitBankAccount(partnerId: number) {
1130:   try { await partnerBankAccountApi.create({ partnerId, accountName: form.partnerName, ... }) } catch {}
1131: }
```

**后端实况**（`backend/erp/erp-partner/src/main/java/cn/aiedge/erp/party/controller/PartyRoleController.java`）：
只有 `/page`、`/list`、`/{id}`、`POST`、`PUT /{id}`、`DELETE /{id}`、`PUT /{id}/status` ——
**没有** `/by-party/{partyId}`、`/{partyId}/add`、`/{partyId}/remove/{roleId}`；
全仓也**没有**任何 `partner/bank-accounts` 控制器。

**真机 404 铁证**：

```
[404] 客户建档 · 绑角色    POST /api/erp/partner/roles/1/add        {"code":404,"message":"接口不存在: api/erp/partner/roles/1/add"}
[404] 客户建档 · 银行账户  POST /api/erp/partner/bank-accounts      {"code":404,"message":"接口不存在: api/erp/partner/bank-accounts"}
```

**✅ 复核结论：数据并未丢失（这是本次审计中被自我推翻的一条初判）**

| 用户填的 | 走的落库路径 | 证据 |
|---|---|---|
| 「其他角色」勾选 | `buildPayload()` 拼成 `roles` 字符串 → `partnerApi.create` → `biz_party.roles` 列 | `customer/form.vue:923`（`roles`）、`MdCustomerController.java:1508-1510`（`party.setRoles(roles)`）、`Party.java:128`、`biz_party.roles` 列存在 |
| 开户行 / 银行账号 | 同上，`bankName` / `bankAccount` 字段 | `customer/form.vue:944-945`、`MdCustomerController.java:1456-1459`、`Party.java:60/63`、`biz_party.bank_name/bank_account` 列存在 |

**因此真正的缺陷是三条（而非「数据丢失」）**：
1. 每次新增客户固定发出 **2 个 404 请求**（`submitRoles` 还会按角色数循环发多次）—— 噪音污染后端日志与前端错误监控；
2. `catch {}` 让失败彻底不可见，**同类写法若被复制到真正会丢数据的地方，将无人察觉**；
3. `partnerRoleApi`（4 个方法）与 `partnerBankAccountApi`（4 个方法）**整族是死封装**，
   且 `erp_partner_role` 关联表设计被「`biz_party.roles` 逗号字符串」绕过 —— 关联表 0 行，
   与 `biz_party_role`（角色字典，同样 0 行）一起构成「设计了但没启用」的中间态。

**建议**：删掉这 2 个调用与对应封装（或后端补端点后接回），并把空 `catch` 改为显式告警。

### P1-1 批次管理「删除」必失败（405）

`frontend/apps/pc-admin/src/views/erp/batch/index.vue:1815` 调 `DELETE /erp/batch-sn/batches/{id}`；
`BatchNumberController` 只有 `GET /{id}`、`PUT /{id}`、`PATCH /status`、`POST /inbound` 等，**无 DELETE**。

真机：`[405] DELETE /api/erp/batch-sn/batches/999999 → "请求方法不支持，请使用 PUT,GET"`。

### P1-2 客户/商品删除无引用保护（校验接口存在但没人用，且查的是空表）

- `MdCustomerController.java:457 delete()` 直接 `partyService.removeById(id)`，**无任何前置校验**；
- 校验能力其实存在：`PartyController.java:154 /{id}/has-transactions` → `PartyMapper.hasTransactions`；
- 但**前端零调用**（`grep -rn "has-transactions" frontend/` 零命中）；
- 而且它查的是 `biz_party_transaction` —— **0 行的空表**，就算调用也恒返回 false。

**后果**：已被销售单引用的客户可以被直接删除，产生孤儿引用。

### P1-3 「支付账户」与「银行账户」两套端点共用同一 service，但权限码却是两套

**先修正一个容易误判的点**：这**不是**两份重复实现。`FinanceAccountController` 的所有写操作都**显式委派**给 `BankAccountService`（源码注释直书「复用《银行账户》写入口」）：

```java
100: @PostMapping   create(...)  { return Result.success("新增成功", bankAccountService.create(dto)); }
111: @PutMapping("/{id}") update(...) { ... bankAccountService.update(id, dto); }
122: @DeleteMapping("/{id}") delete(...) { bankAccountService.delete(id); }
```

| 菜单 | 前端页 | 端点前缀 | service | 表 | 权限码 |
|---|---|---|---|---|---|
| 80552 支付账户 | `views/md/payment-account/index.vue` | `/api/erp/finance/account`（10 端点） | `BankAccountService`（同一份） | `finance_account` | `finance:account:*`（7 个） |
| 70540 银行账户 | `views/md/bank-account/index.vue` | `/api/erp/finance/bank-account`（9 端点） | `BankAccountService` | `finance_account` | `finance:bank-account:*`（8 个） |

**真正的问题只在权限码**：同一实体、同一写入口，却有 `finance:account:*` 与 `finance:bank-account:*` **两套语义重叠的权限码**，
在权限分配界面会出现两个看起来都很像的权限组 —— 授了一个另一个仍 403（P0-1 的成因之一）。
建议合并为一族（保留 `finance:bank-account:*` 为唯一口径，`finance:account:*` 作为兼容别名或直接废弃）。

其余同类「口径视图」关系经复核**均属有意设计**（见 §6）：

| 关系 | 事实 |
|---|---|
| 会计科目 / 费用类型 / 其他收入 | 同一张 `finance_account_subject`，后两者是 `subject_type=5` + `direction=1/2` 的口径视图，薄门面委托 `AccountSubjectService` |
| 支付方式 / 支付渠道 | 两张独立档案表，`md_payment_channel.method_id → md_payment_method.id` |
| 重复建表 | **无**（迁移目录 grep `create table ... bank_account|payment_account|expense_type|accounting_subject` 命中 0） |

### P1-4 六张 0 行历史表 `erp_partner*`

| 表 | 行数 | 代码引用 |
|---|---|---|
| `erp_partner` | 0 | 仅 `UserDataScopeServiceImpl:140`（见 P0-4）+ `DatabaseInitializer:813` 建表 |
| `erp_partner_category` | 0 | 仅 `UserDataScopeServiceImpl:141` + `DatabaseInitializer:833` |
| `erp_partner_contact` | 0 | **零引用** |
| `erp_partner_grade` | 0 | `ProductPriceQueryMapper:176`（见 P1-5）+ `DatabaseInitializer:841` |
| `erp_partner_role` | 0 | **零引用** |
| `erp_partner_logistics_ext` | 0 | 仅 `SetAppCenterController:216` 注释提及 |

### P1-5 客户级别下拉的 UNION 第二支查空表

`backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/mapper/ProductPriceQueryMapper.java:170-179`
（`selectCustomerGrades`）：第一支 `biz_party.party_level`（有数据）∪ 第二支 `erp_partner_grade`（0 行）。
当前不会整体恒空，但**第二支是死支**，且 `erp_customer_level` 也是 0 行 —— 「客户级别主数据」实际没落库。

### P1-6 37 个后端端点前端从未调用（含 5 组「真实导入」能力）

`python tools/audit-md-deadcode.py` B 段列出 37 条，典型：

| 端点 | 位置 | 意义 |
|---|---|---|
| `POST /api/erp/md/customer/import-excel` | `MdCustomerController.java:1068` | 客户**真实导入**（落库） |
| `GET  /api/erp/md/customer/import-template` | `:1012` | 客户导入模板 |
| `POST /api/erp/md/product-price/import-excel` | `ProductPriceController.java:402` | 商品价格真实导入 |
| `POST /api/erp/md/payment-channel/import-excel` | `MdPaymentChannelController.java:173` | 支付渠道真实导入 |
| `POST /api/erp/md/route/import-excel` | `RouteMasterController.java:185` | 线路真实导入 |
| `POST /api/erp/batch-sn/batches/{inbound,outbound,transfer,inventory}` | `BatchNumberController.java:172/194/287/319` | 批次出入库/移库能力整块无 UI |

**同时**：客户页的「导入」按钮走的是通用 `POST /api/import/v2/excel/customer`（`DataImportController`，`dataType` 模板端点）。
两条导入链路并存，需按文档裁定保留哪一条。

### P1-7 前端 230 个 api 封装零消费方（`api/erp/product.ts` 为重灾区）

`api/erp/product.ts` 中 `productApi.create/getById/getByCode/batchUpdatePrices/approval`、
`productUnitApi.*`、`productBarcodeApi.*`、`productAttributeApi.*`（10 个）、
`productSkuRuleApi.*`（6 个）、`productRecommendApi.*`、`productRelatedApi.*` 等**全域无引用**。

对应的功能表也全部是空表：`erp_product_attribute_def / _option / _value`、`erp_product_sku_rule`、
`erp_product_recommend`、`erp_product_related`、`erp_product_attachment` 全为 0 行。
→ 「商品属性、SKU 规则、推荐商品、商品关联、商品附件」是**建了表与封装、没有界面**的能力。

（商品新增实际走 `productFormApi.batchCreate/batchUpdate`，故不构成功能缺失，属**死封装**。）

### P1-8 `views/md/**` 19 个列表页 0 处按钮级权限控制

后端为资料模块定义了细粒度权限码（`create/update/delete/export/import/status` 齐全，动作分布见 §5），
但前端 `views/md/**` 下 **`v-permission` 用量为 0**（对照：`erp/batch/index.vue` 7 处、`erp/pricing/approval/index.vue` 3 处）。
→ 非超管看到全部按钮，点了才 403。

### P1-9 旧「往来单位」页面失去菜单入口却仍在（1,971 行）

- `views/erp/partner/index.vue`（821 行）、`views/erp/partner/detail.vue`（556 行）
  ＋ 4 个 Panel 组件（594 行）—— componentMap 里有键（`dynamicRoutes.ts:72-73`）、有隐藏静态路由（`:1396/1402/1408`），
  但**全库没有任何菜单的 `component`/`list_path` 会解析到它们**（僵尸键判定）；
- 工作台仍有两个快捷入口指向 `/erp/partner`（`views/erp/dashboard/index.vue:218/226`）；
- 这 4 个 Panel 依赖的 `partnerAddressApi` / `partnerBankAccountApi` / `partnerTagApi` 全部是**断链封装**（见 §3）。

→ 旧页 ↔ 新页（`md/customer`、`md/supplier`、`md/partner`）功能重叠，须按文档裁定收敛。

### P1-10 `erp_product` 与 `biz_party` 的有效数据量异常偏低

| 表 | 总行数 | `deleted=0` |
|---|---|---|
| `erp_product` | 83 | **6** |
| `biz_party` | 153 | **19** |

资料模块的「商品」「客户」列表页对超管只会显示 6 个商品、19 个往来单位。
这与「商品条码 103 行、商品单位 187 行」等子表规模明显不匹配，需确认是**清理脚本误删**还是**逻辑删标记异常**（本次只读审计未追因）。

---

## 3. P2

| # | 项 | 证据 |
|---|---|---|
| P2-1 | 70504「商品辅助资料」`display_mode=0` 却残留 `list_path=md/product-supplement`（双入口已关闭，字段未清） | `tools/audit-md-menu.py` ② 段 |
| P2-2 | `erp_route_point`（221 行）**无 `deleted` 列**，与同域其余表不一致，`erp_route`/`erp_route_area` 都有 | `tools/audit-md-db.py` |
| P2-3 | `biz_party_address` / `biz_party_role` / `biz_party_grade_relation` / `biz_party_transaction` **无 `tenant_id`**（在租户忽略清单内，属既定设计但需文档化） | 同上 |
| P2-4 | 权限码命名空间分散在 **9 个**（`md` 50 / `product` 31 / `party` 20 / `wms` 20 / `finance` 15 / `erp` 10 / `pricing` 6 / `mall` 5 / `stock` 4）；动作词 **`view`(18) 与 `detail`(21) 混用**、**`update`(21) 与 `edit`(1，`md:payment-channel:edit`) 不一致** | `tools/audit-md-permission.py` |
| P2-5 | 后端 8 处空 `catch`（`MdCustomerController` ×4、`CustomerRegionController` ×2、`PartyCategoryController` ×2），均为 `NumberFormatException` 静默吞（非法入参直接落默认值） | 审计脚本 A 段 |
| P2-6 | 库内残留 E2E 造数：`finance_account` 7 行中 4 行是 `E2E交付账户/E2EUI支付账户...` 测试数据；`erp_linked_account`（互联账号）**0 行**、`erp_route_area` 0 行 | `tools/audit-md-db.py` |

---

## 4. 零缺陷项（本次核实通过的）

| 项 | 证据 |
|---|---|
| 24/24 叶子菜单 component 解析成功（含 `erp/product/index`、`erp/batch/index` 等跨目录引用） | `audit-md-menu.py` ① |
| 5 条双入口 `list_path` 全部解析成功（`md/product/form` 是 componentMap 别名 → `erp/product/form.vue`） | `audit-md-menu.py` ①b |
| 资料模块内 `menu_code` **0 重复**（全库 8 组重复均不在资料模块） | `audit-md-menu.py` ③ |
| 5 条「添加」型双入口配置与《41条双入口菜单的正确配置》**完全一致**（主菜单→`/index`、标签→`/form`、`tag_label=添加`） | 文档 §「配置规则」 + 库实况 |
| 被资料页面调用的 **231 个端点全部有鉴权注解**（0 个裸端点） | `audit-md-permission.py` |
| 161 个在用权限码**全部存在于 `sys_permission`**（0 个缺失，符合「补注解前必须先补种子」铁律） | 同上 |
| `views/md/**` 19 个页面 **0 处桩代码**：无 TODO/FIXME、无 mock、无 `Math.random`、无占位文案 | `grep` 全量扫描 |
| （更正）`views/erp/product/**` 有 **1 处**占位按钮，已在 P1-11 单列 | `erp/product/form.vue:1934` |
| 16/19 个 `views/md` 列表页使用统一的 `CategoryListLayout` 布局（符合资料模块「左分类树+右数据表」规约） | 布局组件扫描 |
| 菜单渲染不再有前端二次权限过滤（`permissions.includes(menu_code)` 口径已清理） | `grep` 全站仅剩按钮级用法 |

---

## 5. 跨模块关系与主数据引用完整性

资料模块是**主数据提供方**，其正确性通过下游业务单据体现。本节对 5 类核心主数据做了外键命中率实测（`python tools/audit-md-refs.py`）。

| 引用关系 | 命中 / 总数 | 判定 |
|---|---|---|
| `erp_stock.product_id` → `erp_product` | 4 / 4 | ✅ |
| `erp_product_barcode.product_id` → `erp_product` | 32 / 32 | ✅ |
| `erp_product_unit.product_id` → `erp_product` | 91 / 91 | ✅ |
| `erp_product_image.product_id` → `erp_product` | 1 / 1 | ✅ |
| `erp_product_location.product_id` → `erp_product` | 1 / 1 | ✅ |
| `erp_stock.warehouse_id` → `erp_warehouse` | 4 / 4 | ✅ |
| `wms_location.warehouse_id` → `erp_warehouse` | 2 / 2 | ✅ |
| `biz_party_contact.party_id` → `biz_party` | 62 / 71 | ⚠️ 9 条孤儿 |
| `erp_sale_order.customer_id` → `biz_party` | 1 / 3 | ⚠️ 2 条孤儿 |
| `erp_sale_order_item.product_id` → `erp_product` | 2 / 4 | ⚠️ 2 条孤儿 |
| `erp_purchase_order_item.product_id` → `erp_product` | 8 / 12 | ⚠️ 4 条孤儿 |
| `erp_purchase_order.supplier_id` → `biz_party` / `party` | 0 / 10 | 🔴 全部孤儿 |
| `erp_sale_order_item.warehouse_id` → `erp_warehouse` / `wms_warehouse` | 0 / 4 | 🔴 全部孤儿 |

**口径说明（务必按此理解）**：本次库内业务单据的样本量是**个位数**（销售单 3、采购单 10、库存 4、明细 4~12 行），
因此上表**只能证明「引用约束不存在」**（删主数据不会拦、也不会级联），**不足以量化业务影响**。
其中 `erp_purchase_order.supplier_id` 命中 0 与 2026-09-23 采购审计的结论一致（「数据不可测」，非新增代码缺陷）。

**结构性结论**：

1. **主数据删除无引用保护**（见 P1-2）：`MdCustomerController.delete` / 商品删除均无前置校验，
   `hasTransactions` 接口无人调用且查的是空表 —— 上表的孤儿行就是这么产生的。
2. **仓库主数据双源仍未收敛**：`erp_warehouse` **105** 行 vs `wms_warehouse` **3** 行。
   本次实测 `erp_stock.warehouse_id` 同时命中两张表（4/4 + 4/4，说明两表 ID 空间重叠），
   资料模块（仓库规划 80520）走 `erp_warehouse`，WMS 侧另有 3 行 —— 与 2026-09-23 仓储审计的 P0 结论一致，此处仅从资料侧复现。
3. **菜单授权与权限码授权解耦**：`sys_role_menu` 全库仅 **3 行**，资料模块子树 **0 行** ——
   资料模块菜单的可见性**完全**由「权限码 → 菜单码前缀」派生（`SysMenuServiceImpl:285-300`）。
   因此 P0-1（权限码只授超管）会**同一条链路上同时**导致「接口 403」与「菜单不渲染」。

---

## 6. 与开发文档的验收差距

本节由 4 路并行调研产出（往来单位域 / 商品域 / 财务账户域 / 菜单设计），**关键结论均已回溯源码复核**，
复核中推翻了 2 条初判（已体现在 §1–§3 的定级里）。行号均为 `文件:行` 形式，可逐条复查。

### 6.1 逐子域符合度总表

| 子域 | 页面数 | 符合度 | 结论 |
|---|---|---|---|
| 商品 | 4（商品/条码/价格管理/辅助资料/图片/货位/仓库规划） | 高 | 列数、查询项、按钮、导出、真实 xlsx、唯一性校验**逐项吻合**；差距集中在「已建后端无前端入口」 |
| 往来单位 | 5（客户/供应商/物流公司/其他往来单位/互联账号） | 高 | 5 子标签、26 列、页面配置、客商合并（`MdCustomerController:839-959`）均落地；差距见 D-8~D-10 |
| 财务账户 | 7（银行账户/会计科目/费用类型/其他收入/支付方式/支付渠道/支付账户） | 高 | **0 个 P0**、7 页按钮全真调后端、菜单全指向真实组件、无重复建表；差距见 D-11~D-15 |
| 菜单设计 | — | 高 | 5 条双入口**与文档逐字一致**；`menu-consolidation-v2` 的所有裁定**已全部落地**（61105 已物理删除、80530-80532 已移 61405、61107 已建） |

### 6.2 增量缺陷清单（D 系列，§1–§3 未覆盖的部分）

| # | 级别 | 差异 | 证据 |
|---|---|---|---|
| D-1 | P1 | 商品表单「商品字段及规则配置」是**占位按钮**，点击只弹 `message.info('商品字段配置功能待完善')` | `views/erp/product/form.vue:1933-1935`（触发点 `:153-159`） |
| D-2 | P1 | 商品表单图片 Tab 直传走 `/file/upload` 只写 `form.imageUrl`，**不落 `erp_product_image`** → 图片管理页看不到，违反「图片单一口径」（该口径就是本模块图片管理页 `/api/erp/md/image/view/{id}`） | `views/erp/product/form.vue:1672-1677`；对照 `api/erp/productImage.ts:76-120` |
| D-3 | P1 | 商品屏蔽（`erp_product_shield`，33 行）**无任何下游消费方**：销售开单、商城上架都不过滤 | 全后端 grep `ProductShield` 仅命中 `erp-stock` 自身 6 个文件 |
| D-4 | P1 | 套餐 Tab 的「新增」按钮**不建立套餐档案**：套餐页只读，`productKitApi` 只有 `page/getById/getItems` | `views/erp/product/index.vue:99`；`api/erp/product.ts:236-246` |
| D-5 | P1 | **商品属性 / SKU 规则 / 商品附件 / 商品关联**四块：后端 Controller 已实现、前端**无任何页面/路由/菜单入口**（对应表全 0 行） | `ProductAttributeController`/`ProductSkuRuleController`/`ProductAttachmentController`/`ProductRelatedController`；前端 grep `attribute\|skuRule\|productAttachment\|productRelated` 零调用 |
| D-6 | P1 | 「推荐货位」无下游消费：销售出库/采购入库明细不按「商品+仓库」带出推荐货位 | 全后端 grep `erp_product_location` 仅命中 `erp-stock` 自身 + `SystemRebuildService` |
| D-7 | P1 | 客户级别**数据源三分裂**：子标签 CRUD 写 `erp_customer_level`，表单/筛选下拉读 `biz_customer_grade`（`CustomerGrade` 实体），价格查询又 UNION `erp_partner_grade` —— **三张表全为 0 行** | `views/md/customer/index.vue:2062/2431/2433` vs `customer/form.vue:887`；`CustomerLevel.java:16`、`CustomerGrade.java:12`、`ProductPriceQueryMapper.java:176` |
| D-8 | P1 | 客户表单**必填校验只有 `partnerName`**，而文档要求 客户编号*/所属分类*/客户级别*/默认经手人*/手机* | 客户开发文档 L177；`views/md/customer/form.vue:882-885`（`formRules` 仅一项） |
| D-9 | P1 | 支付方式的**第三方支付凭据**（商户号/密钥/API 证书/appid/公钥）未落位 `md_payment_method`，无加密存储、无脱敏展示 | 支付方式开发文档 §配置落位 L249-259；`V11.29.0__Create_Payment_Method_Channel_Tables.sql:15-43`；`views/md/payment-method/index.vue:248-322` |
| D-10 | P1 | 费用类型页**可创建出「损益类·贷方」科目**（后端只在 `direction==null` 时兜底借方，传值即生效），突破「费用类=损益类·借方」的口径 | `ExpenseTypeSubjectServiceImpl.java:73-76`（`if (dto.getDirection() == null) setDirection(DEBIT)`）；其他收入页同型 `OtherIncomeSubjectServiceImpl.java:73-76` |
| D-11 | P2 | 银行账号在列表与导出中**明文无掩码** | `md/bank-account/index.vue:353`、`md/payment-account/index.vue:336`、`BankAccountController.java:197` |
| D-12 | P2 | 银行账户 `PUT` **无法清空**开户行/账号/户主名/收款码（null 被跳过） | `BankAccountServiceImpl.java:359-375` |
| D-13 | P2 | 会计科目 `level` 无 1–4 级上限校验 | `AccountSubjectServiceImpl.java:127`；文档规定 1-4 级 |
| D-14 | P2 | 商品货位设置查询区「仓库」文档要求**必选且默认不加载**，代码默认「全部仓库」并直接加载（多出一个文档未写的聚合视角） | `md/location/index.vue:414-421,491` |
| D-15 | P2 | 银行账户表单多出 **币种/状态/排序/备注** 4 个文档未列字段；后端 `accountLevel/currency/mallTransferEnabled` 过滤条件前端未使用 | `md/bank-account/index.vue:364/420/433/445`；`BankAccountServiceImpl.java:173-181` |
| D-16 | P2 | 客户「客户级别」的**表单下拉值与列表筛选值都按 `gradeName` 字符串匹配**，编辑回填时 `partnerGradeId: undefined` 靠名称反查 —— 级别一旦改名，历史数据的级别关联即断 | `views/md/customer/form.vue:925/1012-1016` |
| D-17 | P2 | **约定不一致三处**：(a) `generateCode.ts:9` 把物流前缀映射为 `WLGS`，但物流表单实际用独立逻辑取 `WuLiu`（`logistics/form.vue:305`），该映射对物流是死代码；(b) 菜单 `component` 短式/全式混用（70501 与 80510-80513 用短式，其余用 `views/xxx/index.vue`，映射齐全故不影响加载）；(c) 列配置 storage-key 命名不一致：`payment-channel` 的 `global-config-key` 与 `storage-key` 同名，而 `payment-method` 用 `-global` 后缀 | `generateCode.ts:9`、`logistics/form.vue:305`；`dynamicRoutes.ts:592-599`；`payment-channel/index.vue:155-156` vs `payment-method/index.vue:139-140` |
| D-18 | 环境事实 | **Flyway 迁移脚本的实际位置是 `backend/core/api/core-api/src/main/resources/db/migration/`**，仓库根的 `db/migration/` 只有 3 个 archive 文件 —— 后续审计/排查找迁移时勿走错 | 本次四路调研中 3 路都先找错了目录 |

### 6.3 文档侧需要回写/定稿的地方（不是代码缺陷，但会误导后续开发）

| # | 文档问题 | 证据 |
|---|---|---|
| W-1 | **`menu_level` 语义两份文档互相冲突**：`mega-menu-redesign.md` §5.2/§6.1/§6.3/§6.7 定义「0=租户级 / 1=系统级」；而 `系统菜单开发文档-营销模块菜单新增.md:36` 定义成「层级（顶级 0 → 分组 0 → 页面 3）」。全库 **65 条** `tenant-admin` 叶子取值 3 正是这份冲突文档的数据化后果 | 对照两份文档 + `SELECT client_type, menu_level, count(*)` |
| W-2 | 「商品价格管理开发文档」头部与「业务规范」「业界对标参考」仍称本页为「⚠️ 占位桩页 / 后端未实现 / 冗余占位」，**与已全栈实现冲突** | 该文档 L3 / L172 / L178-183 vs `views/md/product-price/index.vue:820-825`、`api/md-product-price.ts:127-199` |
| W-3 | `mega-menu-redesign.md` §2.10/§7/统计汇总未同步：缺「其他往来单位」（资料双入口记 4、实为 5）、列 3 仍写「仓库管理」（实为「批次管理」81014）、列 1 缺 70506/81008/81009 | 该文档 L487-505 / L2112-2116 / L2227 |
| W-4 | `menu-consolidation-v2.md` §十.5 规定支付管理叶子用 `views/common/placeholder/index.vue`，实际早已落地真实页面 | 该文档 L342/L346 vs `views/md/payment-{method,channel,account}/index.vue` |
| W-5 | `41条双入口菜单的正确配置.md` 的「数据库核对」章节只维护了仓储/采购条目，**未登记资料模块 5 条双入口**；表格也无 `component` 列（导致 70501 的短式写法无文档可对照） | 该文档 L111-121、L84-100 |
| W-6 | 客户/供应商文档对「证件信息分区」表述矛盾（客户文档称四类统一接入、供应商文档称未加），代码按客户文档实现 | 供应商文档 L265 vs 客户文档 L276；`views/md/supplier/form.vue:337` |
| W-7 | 其他往来单位文档同页两处表名不一致（L102 `erp_partner_attachment` vs L137 `biz_party_attachment`），代码用前者 | `PartyAttachment.java:16` |
| W-8 | 互联账号 API 层缺 `export()` 声明，页面绕过 API 层直连 `request.get`（破坏「页面只调 api 层」约定） | `api/erp/linkedAccount.ts` 全文无 export；`views/md/linked-account/index.vue:566` |

---

## 7. 审计脚本与产物

本次新增 **8 个只读脚本**（全部落在 `tools/`，符合脚本落位规约），产物在 `tool-results/md-audit/`：

| 脚本 | 作用 | 产物 |
|---|---|---|
| `tools/audit-md-menu.py` | 菜单子树递归 + `component`/`list_path` 解析 + 可见性/级别/双入口异常 + `menu_code` 重复 | `md-menu-audit.json` |
| `tools/audit-md-contract.py` | 资料相关 api 封装 + 页面内联调用 ↔ 全后端 3098 个端点契约比对 | `contract-compare.md` |
| `tools/audit-md-api-usage.py` | api 封装方法 → 页面消费方归属（区分「真断链」与「死封装」） | `api-usage.json` |
| `tools/audit-md-permission.py` | 231 个命中端点的鉴权注解覆盖率 + 161 个权限码与 `sys_permission`/`sys_role_permission` 对账 | `permission-audit.json` |
| `tools/audit-md-db.py` | 49 张表 ↔ 39 个实体逐列对账 + `tenant_id`/`deleted`/忽略清单/行数 | `db-audit.json` |
| `tools/audit-md-deadcode.py` | 前端孤儿页面/僵尸键 + 后端零调用端点 + 零消费方封装 | `deadcode.json` |
| `tools/audit-md-refs.py` | 5 类主数据的外键命中率 | `refs-audit.json` |
| `tools/audit-md-probe.py` | **真机**验证：非超管 vs 超管对照 + 断链端点实际响应 | stdout |

**两个脚本级的坑（已修，供后续复用者参考）**：

1. `audit-md-contract.py` 首版把 `/import/v2/excel/customer` 误判为断链 ——
   后端是模板端点 `/import/v2/excel/{dataType}`，前端把路径变量**写死**了。
   修法：比对时额外尝试「任一段替换为 `{}`」的候选（`match()` 函数）。
2. `audit-md-db.py` 首版把实体里的 VO 字段（`@TableField(exist = false)`）算成表列，
   误报出一大批「实体有、库中无」；且把没有 `@TableName` 的实体（如 `Party` → `biz_party`）漏掉。
   修法：解析字段上方的注解块跳过 `exist = false`，并支持按类名推导表名。

---

## 8. 建议的修复顺序

**第 1 批（P0，直接影响可用性，互不耦合）**

1. `tools/grant-md-permissions.py` —— 按角色补授资料模块权限码（对标 `grant-storage-permissions.py` 的形态）；
   注意 `finance:bank-account:*` / `finance:account:*` 涉及资金信息，`DEPT_ADMIN` 只授 `*:list/view/detail`。
2. `sys_menu` 5 条 `menu_level=3` → `0`（**改库前须按用户规约先出方案获批**）。
3. 客户列表「导入」切到真实链路：`BaseDataImportWizard` + `/erp/md/customer/import-excel`
   （供应商/其他往来单位已是这个口径），并把「导入成功」提示改为按后端真实计数显示。
4. 客户建档：**删掉 `submitRoles` / `submitBankAccount` 两个调用与对应的 8 个死封装**
   （数据已随主档落库，无需补端点），并把空 `catch` 改为显式告警。
5. `UserDataScopeServiceImpl:139-145` 的 `erp_partner` → `biz_party`（字段名 `partner_name/partner_code` → `party_name/party_code`，`partner_category_id` → 对应新列）。

**第 2 批（P1，收敛与清理）**

6. 商品域四块「有后端无前端」能力（商品属性 / SKU 规则 / 商品附件 / 商品关联）——
   **先定去留**：补 UI 还是连表带 Controller 一起删（对应表全 0 行，从未被使用）。
7. 商品表单图片直传改为落 `erp_product_image`（D-2，违反图片单一口径）。
8. 商品字段配置占位按钮（`erp/product/form.vue:1934`）与套餐「新增」空按钮（D-1/D-4）。
9. 客户级别三表分裂收敛为单一数据源（D-7，三表全 0 行，是**改表结构的窗口期**）。
10. 客户表单必填校验补齐（D-8）；费用类型/其他收入的 `direction` 强制口径（D-10）。
11. 批次管理删除：要么补 `DELETE /erp/batch-sn/batches/{id}`，要么前端去掉删除按钮。
12. 「支付账户 vs 银行账户」两套权限码收敛（**改库前先查 `docs/Yh-Spec/.../` 的收敛裁定**）。
13. 6 张 `erp_partner*` 空表 + `erp_partner_grade` 死支的处置；`views/erp/partner/**`（1,971 行）旧页面去留（连带工作台 2 个快捷入口）。
14. 零消费方端点/封装清理（**先按「谁在调用」逐条判定**，禁用 grep 结果直接删 —— 参见 2026-09-19「44 项错 15 项」教训）。

**第 3 批（P2，规范化）**

15. 权限码命名统一（`view`/`detail`、`update`/`edit`）与业务域分组；
16. `v-permission` 补齐（19 个资料列表页）；
17. 支付凭据落位与账号掩码（D-9/D-11）、银行账户 PUT 清空语义（D-12）、科目层级校验（D-13）；
18. 清理 E2E 造数、补 `erp_route_point.deleted`、核实 `erp_product`/`biz_party` 有效行数偏低的成因；
19. 文档回写（§6.3 的 W-1~W-8，尤其 **W-1 `menu_level` 语义定稿**——它决定全库 65 条菜单怎么改）。

**改库类操作的规约提醒**：第 1 批的 2（`sys_menu`）、第 2 批的 9/12/13 都涉及系统配置数据表，
按既定规约**须先出方案获批**再动库；只读排查不受限。

---


## 9. 修复进度（2026-09-26）

按用户 2026-09-26 指示「客户级别从基础上根本性解决 + D-17 按推断根本性解决」执行，**本轮为改动代码/迁移脚本**，
未在运行中的实例上执行迁移（见 §9.3 的重启说明）。

### 9.1 客户级别：三数据源收敛为 `biz_customer_grade`（对应 D-7，根因消除）

| # | 改动 | 位置 |
|---|---|---|
| 1 | **删除整套旧实现**（5 个文件）：`CustomerLevelController` / `CustomerLevel` / `CustomerLevelService` / `CustomerLevelServiceImpl` / `CustomerLevelMapper` | `backend/erp/erp-partner/src/main/java/cn/aiedge/erp/customer/**`（整包删除，含 `target/classes` 残类） |
| 2 | 级别 Controller 增强：新增 `/page`（分页+关键字+状态）、`list` 改为**只返回启用**、创建/更新补**编码唯一**校验、删除补**引用保护**（被客户使用中不允许删，提示改用停用） | `party/controller/CustomerGradeController.java`、`party/mapper/CustomerGradeMapper.java`（新增 `countUsedByGradeName`）、`party/service/CustomerGradeService.java`、`party/service/impl/CustomerGradeServiceImpl.java` |
| 3 | 数据权限「客户级别」维度改查新表 | `base/service/impl/UserDataScopeServiceImpl.java:164-172` |
| 4 | 去掉商品价格查询里指向 `erp_partner_grade` 的**死 UNION 支** | `erp-stock/mapper/ProductPriceQueryMapper.java:169-180` |
| 5 | 移除 `DatabaseInitializer` 中会被反复重建的 `erp_partner_grade` 建表语句 | `base/config/DatabaseInitializer.java:840` |
| 6 | **前端**：客户页「客户级别」子标签的 4 处调用（page/post/put/delete）改走 `partnerGradeApi`，字段口径 `levelName/levelCode/enabled` → `gradeName/gradeCode/status`；`PartnerGrade` 类型去掉已无对应列的 `gradeType`、补 `discountRate`；`partnerGradeApi` 新增 `page()`；**全站 6 处 `list('CUSTOMER')` 调用改无参**，注释里的 `?gradeType=CUSTOMER` 口径一并清理 | `views/md/customer/index.vue`、`api/erp/partner.ts`、`views/analytics/mall-customer-list`、`views/mall/buyer-account`、`views/mall/shop-config`、`views/md/customer/form.vue`、`api/erp/mall.ts` |
| 7 | 迁移 `V11.503.0__Drop_Customer_Level_Dual_Source.sql`：删 `party:customer-level:*` 的角色授权并软删 7 个权限码（其端点已不存在，属幽灵码）、DROP `erp_customer_level` 与 `erp_partner_grade` | `backend/core/api/core-api/src/main/resources/db/migration/` |
| 8 | 文档回写：客户开发文档「客户级别 子标签」补收敛说明 | `docs/Yh-Spec/手动整理对标开发文档/资料模块/客户开发文档.md:149` |

### 9.2 D-17 三处约定不一致（根治）

| # | 问题 | 处理 |
|---|---|---|
| a | `generateCode.ts` 里物流前缀映射 `WLGS`/`LOG` 是**被对标实测否定**的初稿猜测（物流文档 L186 明确「实际为 `WuLiu`，而非初稿的 WLGS/LOG」），且物流表单另写了一份取号实现 | 从前缀映射中**删除 logistics 项**（类型收窄为 `customer\|supplier\|partner`）；取号逻辑收敛为 `fetchNextSeq` 单点；新增 `generateLogisticsCodeAsync()`（`WuLiu` + 3 位补零，**对标记号不变**）；物流表单删除自实现的 `CODE_PREFIX` 与 next-seq 调用，改用该函数（顺带清掉随之失效的 `request` import） |
| b | 菜单 `component` 短式/全式混用（资料模块 5 条短式 vs 19 条全式） | 迁移 `V11.505.0` 把 5 条统一为全式；顺带清理 70504 残留的 `list_path`（`display_mode=0` 却留着双入口字段）。**全库另有 25 条短式属其他模块，未越界改动** |
| c | 列配置 `storage-key` 与 `global-config-key` **同名** —— 二者分别落 localStorage 与后端用户配置表，同名会互相覆盖 | 统一为 `global-config-key = storage-key + '-global'`，共改 **11 处 / 8 个页面**（bank-account、customer、partner 用模板串动态拼接；expense-type、location、payment-account、payment-channel、product-price ×4 为静态串）。对照正确样例：`image`、`payment-method` |

### 9.3 验证状态

| 项 | 结果 |
|---|---|
| 后端编译（全仓 32 模块） | ✅ `./mvnw compile` **BUILD SUCCESS** |
| 前端构建 | ✅ `npx vite build` 成功产出 |
| 旧引用残留自检 | ✅ `erp_customer_level` / `erp_partner_grade` / `WLGS` 在**可执行代码**中已归零（仅剩历史迁移脚本与说明性注释） |
| **迁移执行与真机验证** | ⏸ **未执行** —— `V11.503.0`/`V11.505.0` 需重启后端才会跑；而 5655 端口当前由**并行会话**的实例占用（`logs/backend-hr-verify-2.log` 16:48 仍在写入），重启会打断其验证。**待确认重启窗口后执行**：`tools/build-backend.sh`（内含 `clean package`，可一并清掉已删类的 `target/` 残类） |

> ⚠️ 注意：`erp-partner` 的 `target/classes/cn/aiedge/erp/customer/**` 残类已**手工删除**，
> 但最终仍须走一次 `clean package` —— 增量构建不会清理「已删源文件」的旧产物（MNG-5039），
> 否则 fat jar 里会残留 `CustomerLevelController`，让「已下线」变成假象。

## 10. 权限模型核对（2026-09-27 · 按用户澄清修正）

用户澄清的模型：**「除系统模块是系统级外，其他模块都是租户级；平台给租户授予模块使用权；租户内的租户超管可以给自己的员工和角色做权限控制。」**
据此逐层核实，**三层链路在代码里均已实现**。

### 10.1 三层链路的实现证据

| 层 | 职责 | 实现与实测 |
|---|---|---|
| ① 平台 → 租户：模块使用权（entitlement） | 租户"买没买" | 表 `sys_tenant_module`（`module_code` + `status` + `expire_time`）。实测租户 1、2 都已持 13 个模块，**含 `master-data`（资料）** |
| ② 租户 → 租户超管：自动全权 | 超管"什么都能做" | `PermissionInitializationConfig implements ApplicationRunner`，**每次启动**执行 `assignAllPermissionsToSuperAdmin()`：先 `DELETE` 该角色的旧关联再按 `permissionService.list()` 全量重建。所以 `SUPER_ADMIN` 永远是全权，**不需要也不会靠迁移授权** |
| ③ 租户超管 → 本租户角色/员工：自助分配 | 员工"能做什么" | 权限树取数口径 = **平台级(`tenant_id=0`) + 本租户**（`SysPermissionServiceImpl.java:186-191` 与 `:128-137`）；分配接口 `POST /api/role/{id}/permissions`（`SysRoleController.java:68`）。**实测**：租户 1 超管调 `/api/permission/tree?tenantId=1` 返回 **1821 个节点**，其中资料域命名空间 **239 个码**，资料模块 161 码全部可见 |

### 10.2 现状（分配状态，非缺陷）

| 租户 | 角色 | 可分配权限的码 | 持有资料模块码 |
|---|---|---|---|
| 1 | `SUPER_ADMIN`（租户超管） | 有 | 161（启动时自动重建） |
| 1 | `SYSTEM_ADMIN` | 有 | 161（**由 V11.513.0 预置**，此前为 41） |
| 1 | `DEPT_ADMIN` | 无 | 80（**由 V11.513.0 预置**，此前为 21） |
| 2 | `E2E_T2_ADMIN`（唯一角色，E2E 测试建） | 有 1 个 | **0** |

- 租户 2 拥有 `master-data` 模块权、其管理员角色**也有**分配权限的能力，但**还没有给任何人分配过** —— 这正是"非超管 403"的直接原因，属**租户侧配置未完成**。
- `DEPT_ADMIN`/`ROLE_MQD2X2DI` 不持有"可分配权限"的码，是**正确的职责分离**（普通角色不该能改权限）。

### 10.3 本次已执行授权的定位与处置选项

`V11.513.0` 的授权部分（`SYSTEM_ADMIN` +120、`DEPT_ADMIN` +59）在本模型下是**「替租户超管预置了一份默认分配」**，
不是绕过机制 —— 租户超管之后仍可在界面上自由增删。两种处置：

- **保留（当前状态）**：租户 1 开箱即可用，与既有 `grant-storage-permissions.py`、`V11.509.0`（CRM）、`V11.510.0`（设置）的做法一致。
- **回滚**：`DELETE FROM sys_role_permission WHERE id BETWEEN 9800000 AND 9899999;`

### 10.4 建议（本次核实中新发现的产品缺口）

**租户开通流程没有预置角色与权限**：`POST /api/tenant`（`TenantController.java:110`）只建租户本身，
未创建"租户超管"角色、也未授任何权限码。后果是新租户开通后**没有可用的管理员角色**，
必须靠人工补（租户 2 现在就是这么个中间态）。

若产品要求"开通即用"，应在租户开通/审批流程中：
① 建 `tenant_admin`（或复用 `SUPER_ADMIN` 语义的租户级角色）；② 按 `sys_tenant_module` 已授权模块预置只读权限模板；
③ 建首个管理员账号并绑定该角色。**此项属产品决策，本次未实施。**
