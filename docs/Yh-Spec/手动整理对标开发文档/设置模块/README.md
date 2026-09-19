# 设置模块 · 开发文档集

> **对应系统**：来肯企汇 ql361 v2.2（`22stable.ql361.com`）**有完整的「设置」一级菜单**，共 4 个分组 13 个页面。
> **本模块口径**：设置 = **租户级的设置管理**（`client_type = tenant-admin`）—— 即"我这个租户自己怎么用这套系统"。平台级（"我怎么管所有租户"）另立《系统模块》，见 `../系统模块/README.md`。
> **事实来源**：① 本系统源码（引用必须带 `文件:行号`）；② devdb 实测（`sys_menu` 菜单树、各业务表结构与行数，2026-09-18 直连查询）；③ ql361 实测抓取（`tool-results/ql361/设置-live/`，2026-09-18 登录后逐页抓列配置 + 截图）。
> **通用规范**：见《ql361对标/对标开发技术参考文档》第 4、5 章；布局金标准施工手册见《交易模块/_开发指南-金标准.md》（三条路线 A/A′/B、模板、陷阱、自检清单）。
> **模块定位一句话**：设置 = **租户自助的"系统怎么跑"配置台**（参数 / 打印 / 审核规则 / 支付 / 企业档案 / 应用开通 / 期初录入 / 账套维护 / 会计期间 / 操作日志）**＋租户自己的审批流引擎**（流程定义·设计·实例·待办·已办）。

---

## 1. 模块定位与边界（最重要的一节，先读这里）

### 1.1 「设置」与「系统」的边界（本系统特有，最易混）

本系统的菜单按 `sys_menu.client_type` 分成**两套互不可见的域**（`SysMenuServiceImpl.getMenuTree → listAllMenus(0L)` 只取 `tenant_id = 0` 的菜单，再按登录人的 `client_type` 过滤）：

| | **设置模块**（本文档集） | **系统模块**（`../系统模块/`） |
|---|---|---|
| 顶级菜单 | `60012 设置`（`mega:settings`, sort=1500） | `60013 系统`（`mega:system`, sort=1600） |
| `client_type` | **`tenant-admin`** | **`system-admin`** |
| 谁能看见 | **租户管理员**（我们日常登录的就是这一侧） | **仅平台运营方**（普通租户登录**完全看不到**，菜单树里不出现） |
| 管什么 | **租户自己怎么用系统**：参数、打印、审核规则、支付、企业信息、应用开通、期初、账套、会计期间、操作日志、本租户的审批流 | **平台方怎么管所有租户**：租户生命周期、模块授权、配额、系统监控、数据管理、开发工具、平台参数 |
| 作用域 | **单租户内**（`tenant_id = 当前租户`） | **跨租户**（`tenant_id = 0` 全局 或 指定租户） |
| 对标 | ql361 **有**（租户级系统，13 页） | ql361 **无**（厂商不会把自己的 SaaS 控制台给客户看）→ 按业界生产级平台建模 |

> **红线**：
> ① 设置模块**只改本租户**的数据，任何"影响其它租户"的能力（套餐、授权、配额、跨租户监控）都属于系统模块，**不得**在设置模块里实现；
> ② 两个模块**不共用页面组件**（同一功能在两侧是不同页面，如"菜单"：设置侧是 `set/menu-config`「菜单配置」= 租户改自己菜单显隐；系统侧是 `system/menu`「菜单管理」= 平台定义全局菜单与权限）；
> ③ 设置模块的写端点**一律要求租户隔离**（MyBatis-Plus 多租户插件已全局生效，见 `MyBatisPlusConfig`）。

### 1.2 域边界（设置模块内部 7 个分组各管什么）

```
设置
├─ 系统配置   租户级开关与档案：菜单显隐 / 系统参数 / 打印设置 / 审核规则 / 支付 / 企业信息 / 应用开通
├─ 数据录入   开账前的期初数：库存期初（按商品）、财务期初（银行现金/往来/固定资产/资产负债）
├─ 账套操作   账套级运维：系统重建（清数据）/ 系统任务（异步任务台账）/ 操作日志（审计）
├─ 财务设置   会计期间（12 期起止日期与结账）
├─ 打印管理   ★ 空分组（ql361 的「打印设置」应落这里，见 §2.3）
├─ 工作流     流程定义（台账+设计器）/ 流程设计（同一设计器）
└─ 审批       流程实例（监控台）/ 我的待办 / 我的已办
```

> **一句话区分四个易混页**：
> - **系统参数**（80621）= 租户级的**开关与枚举**（批次效期、成本规则、税额…），对标"设置项"；
> - **平台参数**（系统模块 62501）= **平台级**参数（跨租户的全局键值）；
> - **菜单配置**（80620）= **租户**改自己**已授权菜单的显隐**；
> - **菜单管理**（系统模块 6130701）= **平台**定义**全局菜单树 + 权限码 + 路由/组件**。

---

## 2. 模块菜单结构与文档清单（`sys_menu` 实测 2026-09-18）

### 2.1 菜单树（devdb 直查，递归展开；`deleted=0`）

```
60012  设置                                 (mega:settings, sort=1500, client_type=tenant-admin)
├─ 61201  系统配置                          (mega:set:sys-config, sort=100)
│   ├─ 80620  菜单配置   set:menu-config     单入口  set/menu-config       ★有对标
│   ├─ 80621  系统参数   set:sys-params      单入口  set/sys-params        ★有对标
│   ├─ 80622  审核设置   set:audit-config    单入口  set/audit-config      ★有对标
│   ├─ 80623  支付配置   set:payment-config  单入口  set/payment-config    ★有对标
│   ├─ 80624  企业信息   set:company-info    单入口  set/company-info      ★有对标
│   └─ 80625  应用中心   set:app-center      单入口  set/app-center        ★有对标
├─ 61202  数据录入                          (mega:set:data-entry, sort=200)
│   ├─ 70550  库存期初   set:initial-stock   单入口  set/initial-stock     ★有对标
│   └─ 70551  财务期初   set:initial-finance 单入口  set/initial-finance   ★有对标
├─ 61203  账套操作                          (mega:set:account, sort=300)
│   ├─ 70560  系统重建   set:rebuild         单入口  set/rebuild           ★有对标
│   ├─ 70561  系统任务   set:system-task     单入口  set/system-task       ★有对标
│   └─ 80630  操作日志   set:operation-log   单入口  set/operation-log     ★有对标
├─ 61204  财务设置                          (mega:set:fin-config, sort=400)
│   └─ 70570  会计期间   set:accounting-period 单入口 set/accounting-period ★有对标
├─ 61205  打印管理                          (mega:set:printing, sort=500)
│   └─ 80930  打印设置   set:print-config   单入口 set/print-config     ★有对标（2026-09-18 建，见 §2.3）
├─ 61206  工作流                            (mega:set:workflow, sort=600)
│   ├─ 801    流程定义   workflow-definition  单入口 /workflow/definition  ✗无对标（本系统独有）
│   └─ 80610  流程设计   set:workflow-designer 单入口 set/workflow-designer ✗无对标（同一组件）
└─ 61207  审批                              (mega:set:approval, sort=610)
    ├─ 802    流程实例   workflow-instance    单入口 /workflow/instance    ✗无对标
    ├─ 803    我的待办   my-task              单入口 /workflow/task        ✗无对标
    └─ 804    我的已办   my-done              单入口 /workflow/done        ✗无对标（同一组件）
```

**菜单数**：1 个顶级 + 7 个分组 + **18 个页面项**（2026-09-18 建「打印设置」80930 后 `61205` 不再是空分组）。

**⚠️ 三处菜单结构问题（写文档/做验收时必须知道）**：

| # | 问题 | 实测 |
|:-:|------|------|
| 1 | ~~**`61205 打印管理` 是空分组**~~ → **已落地（2026-09-18）** | 建页前：该分组 `sort=500`，**无任何 `parent_id=61205` 的子菜单**（含已删除也没有）→ 前端渲染出一个**点不开任何页面**的菜单组。现已有子菜单 **80930 打印设置**（迁移 V11.402.0，详见 §2.3） |
| 2 | **`61201 系统配置` 的 `sort=3` 空缺** | 现有 sort = 1/2/4/5/6/7（`80620~80625`），**没有 sort=3**；且 ql361 该组是 **7 页**（多一个「打印设置」）→ 位置与 ql361 的 7 页制式吻合，推测原本预留给了打印设置 |
| 3 | **`801/80610` 同组件、`803/804` 同组件** | `801 流程定义` 与 `80610 流程设计` 的 `component` **逐字相同**（`views/workflow/designer/index.vue`）；`803 我的待办` 与 `804 我的已办` 归一化后**同一 key**（`task-management` vs `task-management.vue`，见 `router/dynamicRoutes.ts:922` 的归一化规则）→ **两两进入的是 100% 同一界面**，详见 §7 的 P0 |

### 2.2 页面 ↔ 文档对照表（**18 篇：17 个菜单页 + 1 个应有而未建的页**）

| 分组 | 页面 | 菜单ID | menu_code | 路由 | 组件 | 对标 | 文档 |
|------|------|:-----:|-----------|------|------|:----:|------|
| 系统配置 | 菜单配置 | 80620 | `set:menu-config` | `set/menu-config` | `views/set/menu-config/index.vue` | ✅ | [菜单配置开发文档](./菜单配置开发文档.md) |
| | 系统参数 | 80621 | `set:sys-params` | `set/sys-params` | `views/set/sys-params/index.vue` | ✅ | [系统参数开发文档](./系统参数开发文档.md) |
| | 审核设置 | 80622 | `set:audit-config` | `set/audit-config` | `views/set/audit-config/index.vue` | ✅ | [审核设置开发文档](./审核设置开发文档.md) |
| | 支付配置 | 80623 | `set:payment-config` | `set/payment-config` | `views/set/payment-config/index.vue` | ✅ | [支付配置开发文档](./支付配置开发文档.md) |
| | 企业信息 | 80624 | `set:company-info` | `set/company-info` | `views/set/company-info/index.vue` | ✅ | [企业信息开发文档](./企业信息开发文档.md) |
| | 应用中心 | 80625 | `set:app-center` | `set/app-center` | `views/set/app-center/index.vue` | ✅ | [应用中心开发文档](./应用中心开发文档.md) |
| 数据录入 | 库存期初 | 70550 | `set:initial-stock` | `set/initial-stock` | `views/set/initial-stock/index.vue` | ✅ | [库存期初开发文档](./库存期初开发文档.md) |
| | 财务期初 | 70551 | `set:initial-finance` | `set/initial-finance` | `views/set/initial-finance/index.vue` | ✅ | [财务期初开发文档](./财务期初开发文档.md) |
| 账套操作 | 系统重建 | 70560 | `set:rebuild` | `set/rebuild` | `views/set/rebuild/index.vue` | ✅ | [系统重建开发文档](./系统重建开发文档.md) |
| | 系统任务 | 70561 | `set:system-task` | `set/system-task` | `views/set/system-task/index.vue` | ✅ | [系统任务开发文档](./系统任务开发文档.md) |
| | 操作日志 | 80630 | `set:operation-log` | `set/operation-log` | `views/set/operation-log/index.vue` | ✅ | [操作日志开发文档](./操作日志开发文档.md) |
| 财务设置 | 会计期间 | 70570 | `set:accounting-period` | `set/accounting-period` | `views/set/accounting-period/index.vue` | ✅ | [会计期间开发文档](./会计期间开发文档.md) |
| **打印管理** | 打印设置 | 80930 | `set:print-config` | `set/print-config` | `views/set/print-config/index.vue` | ✅ | [打印设置开发文档](./打印设置开发文档.md) ★2026-09-18 建（迁移 V11.402.0） |
| 工作流 | 流程定义 | 801 | `workflow-definition` | `/workflow/definition` | `views/workflow/designer/index.vue` | — | [流程定义开发文档](./流程定义开发文档.md) |
| | 流程设计 | 80610 | `set:workflow-designer` | `set/workflow-designer` | `views/workflow/designer/index.vue` | — | [流程设计开发文档](./流程设计开发文档.md) |
| 审批 | 流程实例 | 802 | `workflow-instance` | `/workflow/instance` | `views/workflow/instance-monitor.vue` | — | [流程实例开发文档](./流程实例开发文档.md) |
| | 我的待办 | 803 | `my-task` | `/workflow/task` | `views/workflow/task-management.vue` | — | [我的待办开发文档](./我的待办开发文档.md) |
| | 我的已办 | 804 | `my-done` | `/workflow/done` | `views/workflow/task-management.vue` | — | [我的已办开发文档](./我的已办开发文档.md) |

**13 个页面在 ql361「设置」域有 1:1 对标**（12 个现存菜单页 + 「打印设置」——后者已于 2026-09-18 建出，菜单 80930）；**5 个工作流/审批页为本系统独有**（ql361 无工作流域，其"待审批单据/业务草稿/经营历程"挂在**分析域**的"综合单据"组，见 §5.1）。

### 2.3 `61205 打印管理` 空分组 —— 本模块最大的**结构缺口**（**已落地**，2026-09-18）

**事实（devdb 实测 2026-09-18，建页前）**：

```sql
SELECT id, parent_id, menu_name, menu_code, sort, status FROM sys_menu WHERE parent_id = 61205 AND deleted = 0;
-- 0 行
SELECT id, parent_id, menu_name, deleted FROM sys_menu WHERE parent_id = 61205;
-- 0 行（含已逻辑删除的也没有）
```

即该分组**从未有过子菜单**，是一个纯壳。前端渲染树时该分组会出现在菜单里但**点不出任何页面**。

**与对标的关系**：ql361「设置 → 系统配置」组实测 **7 页**，我们只有 6 页，少的那一页正是 **`打印设置`**（实测截图 `tool-results/ql361/设置-live/打印设置.png`，页面内容见《[打印设置开发文档](./打印设置开发文档.md)》）。而我们的 `61201 系统配置` 组 `sort` 恰好缺 `3`（现有 1/2/4/5/6/7），与"原本预留了一位"吻合。

**结论（已裁定并落地）**：`61205 打印管理` **应承接 ql361 的「打印设置」页**，而不是继续空着或挂到 `61201` 下。这既解释了空分组，也补齐了对标缺页。本模块文档集已为它单列一篇《打印设置开发文档》（§2.2 表格末行）。

**落地结果（2026-09-18，迁移 `V11.402.0__Add_Set_Print_Config.sql`）**：

| 项 | 落地内容 |
|---|---|
| 菜单 | 新建 **`80930 打印设置`**（`parent_id = 61205`、`menu_code = set:print-config`、`path = set/print-config`、`component = views/set/print-config/index.vue`、`menu_type = 1`、`tenant_id = 0`、`client_type = tenant-admin`、`deleted = 0`、`visible = 1`、`status = 1`、`sort = 1`）→ **空分组已消灭**（`61205` 现有 1 个子菜单） |
| 表 | 新建 `set_print_config`（一行一租户，唯一索引 `uk_set_print_config_tenant`，16 列） |
| 后端 | `PrintConfigController`（`cn.aiedge.erp.printing`，前缀 `/api/set/print-config`：GET / PUT / GET `assistant`）+ `PrintConfigService(Impl)` + `SetPrintConfigMapper` |
| 前端 | `views/set/print-config/index.vue`（路线 A′，复用 `components/SettingsLayout`）+ `api/set/print-config.ts` + `router/dynamicRoutes.ts` 的 `componentMap` 两条映射 |
| 权限码 | 新增 `set:print-config:view` / `set:print-config:update`（授权 `role_id = 1`） |
| **仍未落地（如实登记）** | ① §6.3 的「5 个打印视图无菜单」**未处理**（本页的 6 个模板类目只做口径说明，无跳转）；② 7 个配置项在打印链路上**尚未被消费**（`PrintDialog` 仍强校验先预览、远程分支仍是桩、无小数位格式化）→ **本页「存得下、读得回」，但还不会改变打印行为**；③ ql361 的 3 个下拉完整选项集与 `?` 气泡正文仍未补抓 |

### 2.4 无菜单的孤儿实现与不可达路由（必须如实标注，不得当作"已完成"）

| 项 | 位置 | 实情 |
|---|---|---|
| ~~**流程分析**~~ **已闭环（2026-09-18 二轮，`V11.417.0`）** | `views/workflow/process-analysis.vue` + `router/dynamicRoutes.ts` | 原状态：**菜单孤儿但路由可达**（`sys_menu` 里 `component LIKE '%process-analysis%'` 0 行；历史上三次挂过菜单均已删除），该页首屏最多 30 秒展示写死的 2024 年 mock 数据。**现状态**：`V11.417.0` 补菜单 **`80611 流程分析`**（挂 `61206 工作流`，`menu_code = set:workflow-analysis`、`component = views/workflow/process-analysis.vue`、`menu_level = 0`）+ 权限码 `workflow:analysis:view`（`WorkflowController` 的 `/analysis/refresh`、`/analysis/report` 均加 `@SaCheckPermission`）；页面写死的 2024 mock 已清除（E2E 有专项断言）。→ 详见《流程实例开发文档》。 |
| **审核设置编辑器** | `views/set/audit-config/form.vue`（214 行） | `sys_menu` 中**无该 component 的菜单**；同为"活文件、无入口"，且其编辑态保存只 `message.info` 不提交。→ 详见《[审核设置开发文档](./审核设置开发文档.md)》。 |
| **`componentMap` 冗余注册** | `router/dynamicRoutes.ts:119-120` | `'workflow/definition'` 与 `'set/workflow-designer'` 两条映射**对任何现存菜单都不成立**（801/80610 的 DB `component` 都是 `views/workflow/designer/index.vue`）→ 靠 `:940` 的动态导入兜底才渲染出来。 |
| **`/api/system/log` 平行实现** | `core-base` `LogManageController`（14 端点，权限注解齐全） | **无任何页面引用**；而收户真正在用是 `SysLogStubController`（`/api/log`，6 端点，**零权限注解 + 租户硬编码为 1**）。→ 详见《[操作日志开发文档](./操作日志开发文档.md)》。 |

---

## 3. 本模块实现现状（**施工前快照** · 2026-09-18 源码核对）

> 🔴 **本节是施工前的静态取证快照，用于记录"改之前长什么样"，其中的缺口已在本轮施工中全部处置。**
> **当前实际状态请看 §10「施工收口」**（含 164/164 端到端验收证据）。本节保留原文不动，是为了让后来者能对照出"改了什么、为什么改"。

> 本节所有结论来自 `tool-results/docgen/evidence/设置模块/*.md`（12 页 + 5 工作流页，逐页 `文件:行号` 取证），未经运行期验证的项已单独标注。

### 3.1 页面外壳符合度（**施工前**：全线未达金标准；施工后见 §10）

| 页面 | 当前外壳（实测） | 路线判定 | 状态 |
|------|-----------------|:--------:|------|
| 菜单配置（80620） | `ErrorBoundary > PageContainer(title)` **未传 `full-height`** + 裸 `a-table` | ✗ 非 A/A′/B | ❌ |
| 系统参数（80621） | 同上 | ✗ | ❌ |
| 审核设置（80622） | 同上（+1 个未注册的 `form.vue`） | ✗ | ❌ |
| 支付配置（80623） | 同上 + 3 个裸 `a-table` + `a-tabs`(2) | ✗ | ❌ |
| 企业信息（80624） | 同上 + **纯表单无表** | ✗（配置页，应走 A′） | ❌ |
| 应用中心（80625） | 同上 + `a-card` 栅格（**0 个按钮**） | ✗（配置页，应走 A′） | ❌ |
| 库存期初（70550） | 同上 + 裸 `a-table` | ✗ | ❌ |
| 财务期初（70551） | 同上 + 裸 `a-table` | ✗ | ❌ |
| 系统重建（70560） | 同上 + `BillTableList`（**传了不存在的 prop**） | ✗ | ❌ |
| 系统任务（70561） | 同上 + `BillTableList`（**同上**） | ✗ | ❌ |
| 操作日志（80630） | **裸 `div.page-container`**（无 `ErrorBoundary`、无 `PageContainer`）+ `a-card` + 裸 `a-table` | ✗ | ❌ |
| 会计期间（70570） | **`ARReportPage`（路线 B，已明令不再新增）** | B | ❌ |
| 流程定义 / 流程设计（801/80610） | 设计器自绘（`ErrorBoundary` + `PageContainer` + 画布） | ✗ | ❌ |
| 流程实例（802） | `BillTableList` + 自绘统计卡 | ✗ | ❌ |
| 我的待办 / 我的已办（803/804） | 同上 | ✗ | ❌ |

**金标准自检 8 项的实测通过情况（12 个系统配置/录入/账套页）**：

| # | 检查项 | 通过 | 说明 |
|:-:|--------|:----:|------|
| 1 | `ErrorBoundary` + `PageContainer(full-height)` | **0/12** | 12 页均用了 `ErrorBoundary`（操作日志除外），但**无一传 `full-height`** |
| 2 | `CategoryListLayout` | **0/12** | 全无（见 §4 的形态缺口讨论） |
| 3 | `BillDetailTable` 或 `BillTableList` | **2/12** | 仅系统重建/系统任务用了 `BillTableList`，且用法错误 |
| 4 | 经典分页 `StandardPagination` | **0/12** | `grep StandardPagination FE/views/set/` → 0 命中 |
| 5 | `PageConfigPanel`（含两个 default 配置） | **0/12** | 且**全模块零 `DEFAULT_QUERY_FIELDS`/`DEFAULT_FUNCTION_BUTTONS`** |
| 6 | `storage-key` / `global-config-key` | **0/12** | `grep -rn "storage-key\|storageKey\|global-config-key" FE/views/set/` → **0 命中** |
| 7 | 打印（对标有打印才强制） | 待逐页判 | ql361 设置域多数页**无打印按钮**（仅操作日志/系统任务有），逐页判 N/A |
| 8 | 导出 | 部分 | 操作日志有导出（但是**假导出**）、系统任务对标有导出 |

> **一句话**：设置模块的 12 个对标页，**没有一页达到金标准路线 A**。这是本模块最大的施工量。

### 3.2 全模块共性缺陷（可整段引用，证据见各页文档）

1. **页面配置能力全缺**：12 页**均无** `PageConfigPanel`、**均无** `DEFAULT_QUERY_FIELDS` / `DEFAULT_FUNCTION_BUTTONS` 常量、**均无** `storage-key`/`global-config-key`。
2. **前端权限门控全缺**：`grep -rn "v-permission\|hasPermission" FE/views/set/` → **零命中**；而后端对应写端点**多数要求权限码**（如 `system:menu:*`、`system:config:*`、`system:tenant:update`）→ 非超管用户点按钮**预期 403**。
3. **列配置不可持久化**：所有列数组都是硬编码常量，无表头齿轮（无序号列 `rowNo`）；`defaultHidden` **全模块零使用**（"默认列 / 全量列"在 12 页**完全相等**）。
4. **表格组件三种并存**：裸 `a-table`（7 页）、`BillTableList`（2 页）、`ARReportPage` 内置表（1 页）。
5. **模块内几乎无页内 Tab**：仅「支付配置」有 `a-tabs`（2 个：支付请求 / 支付记录），且两 Tab **不共享列配置、无独立 storage-key、切换不重新加载**。
6. **查询区未采用横向自适应网格**：有查询区的页面一律 `a-form layout="inline"` 单行流式（与《查询区必须横向网格》规范不符）。
7. **`formatter` 缺陷模式在本模块不存在**：12 页**均未使用 `formatter`**（全走 `#bodyCell` 插槽或 `ARReportPage` 的 `dataIndex` 判定）→ **未发现** `({row}) =>` 对象解构的签名错配。但发现**同类的插槽判定口径分裂**：多数页用 `column.key`，`ARReportPage`（会计期间）用 `column.dataIndex`。
8. **数据侧普遍无数据**：`sys_tenant_module` **0 行**、`payment_request` **0 行**、`payment_record` **0 行**、`erp_stock(is_initial=1)` **0 行**；`%initial%`/`%rebuild%`/`%system_task%` 相关表**一个都不存在**。

### 3.3 后端能力盘点（**两极分化：有的很全，有的完全没有**）

| 页面 | 控制器（相对 `BE/`） | 类前缀 | 端点数 | 权限注解 |
|---|---|---|---|---|
| 菜单配置 | `core/base/core-base/.../base/controller/SysMenuController.java` | `/api/menu` | 16 | ✅ 逐个 `system:menu:create/update/delete/list/detail/update-status` |
| 系统参数 | `core/api/core-api/.../config/controller/SystemConfigController.java` | `/api/config` | 15 | ⚠️ 多数有 `system:config:*`；`/types`/`/groups`/`/map`/`/value/{key}` **无** |
| 审核设置 | `core/api/core-api/.../workflow/controller/WorkflowController.java` | `/api/workflow` | **32** | ❌ **仅类级 `@SaCheckLogin`，无任何方法级权限码** |
| 支付配置 | `core/payment/core-payment/.../payment/controller/PaymentController.java` | `/api/payment` | 11 | ❌ 全部无权限注解 |
| 企业信息 | `core/api/core-api/.../tenant/controller/TenantController.java` | `/api/tenant` | 12 | ⚠️ 类级 `@SaCheckLogin` + 写端点 `system:tenant:update` |
| 应用中心 | `core/api/core-api/.../tenant/controller/TenantModuleController.java` | `/api/tenant-module` | 2 | ⚠️ `system:tenant:query`；`/valid-codes` 无 |
| 应用中心 | `core/api/core-api/.../tenant/controller/TenantPackageController.java` | `/api/tenant-package` | 6 | ✅ `platform:tenant-package:*` |
| 库存期初 | `erp/erp-stock/.../stock/controller/initial/InitialStockController.java` | `/api/set/initial-stock` | 4 | ❌ 仅 `@SaCheckLogin` |
| **财务期初** | **未找到** | — | **0** | — |
| **系统重建** | **未找到** | — | **0** | — |
| **系统任务** | **未找到** | — | **0** | — |
| 操作日志 | `core/api/core-api/.../audit/controller/SysLogStubController.java` | `/api/log` | 6 | ❌ 全部无权限注解，且租户硬编码 `DEFAULT_TENANT_ID = 1L` |
| 操作日志（平行，无人调用） | `core/base/core-base/.../base/controller/LogManageController.java` | `/api/system/log` | 14 | ✅ `log:oper:*` / `log:login:*` 齐全 |
| 会计期间 | `erp/erp-finance/.../finance/controller/AccountingPeriodController.java` | `/api/erp/finance/period` | 4 | ✅ `finance:period:view/create/update` |
| 工作流 5 页 | `core/api/core-api/.../workflow/controller/WorkflowController.java` | `/api/workflow` | 32 | ❌ 仅类级 `@SaCheckLogin` |

> **注意**：本模块**只有一个** `WorkflowController`（32 端点）。前端 `src/api/workflow/index.ts` 里另有 16 条路径（`workflowDefinitionApi` 7 条、`workflowTaskApi` 5 条、`workflowInstanceApi` 4 条）**与后端全部对不上**（详见各页文档「接口清单」）。

---

## 4. 【重要】设置模块的页面形态：**两类页面，两种壳**（ql361 实测）

> 这一节是本模块独有的施工前提，**不要**照搬交易/资料的形态结论。

### 4.1 ql361 设置域实测：两种形态并存

**① 参数/表单类页（系统参数、打印设置、企业信息、审核设置、菜单配置…）→「左列纵向标签 + 右栏内容」两栏式**

以「系统参数」实测为例（截图 `tool-results/ql361/设置-live/系统参数.png`）：

```
左列（8 个纵向标签按钮）        右栏（分组卡片）
行业设置  ←选中                □ 商品规格属性                 关
流程启用                       ■ 批次、批号管理               开
单据设置                          ☑ 保质期管理
库存设置                          □ 批号管理 / □ 批号启用大小写
财务设置                          批次条码规则生成 [无]+[无]+[无]
数据权限                          批次商品成本规则 [按移动加权平均]
消息提醒                          批次保质期商品默认出库规则 [近效先出]
其他                              ☑ 订单启用批次保质期
                                □ 序列号管理                   关
                                          [保存]
```

- **开关是「一组卡片 + 右对齐 toggle」**，每张卡带 **`?` 帮助气泡**与**温馨提示**（如"此配置被商品启用后不能更改，请慎重选择"）；
- 底部**一个**「保存」；
- 同一形态也见于「打印设置」（左列：箱号模板/物流模板/条码模板/套餐条码/批次条码/货位码）。

**② 列表类页（操作日志、系统任务、库存期初、会计期间…）→ 标准列表页**

以「操作日志」实测为例（截图 `tool-results/ql361/设置-live/操作日志.png`）：

```
顶部 Tab：系统日志 | 登录日志
查询区：登录日期(起)* 登录日期(止)*  登录类型[下拉]  操作员[输入]  [查询]
工具栏：刷新 | 打印(F8) | 导出
数据表：序号 | 时间 | 操作员 | 姓名 | 内容
分页：首页 上页 第(1/43)页 下页 尾页 跳转到[1]页 共 855 条记录 每页显示[20]行
```

**③ 矩阵类页（审核设置、会计期间、系统重建、财务期初）→ 无表头表格 / 多 Tab 表格**

- **审核设置**：`单据 | 审核设置 | 摘要` 三列表格，**每行一个「设置」按钮**打开配置；ql361 实测含 16 类单据（销售出库单/销售订单/销售退货申请单/商城订单取消/采购入库单/采购订单/费用单/收款单/付款单/预收款单/预付款单/会计凭证/预订货单/调拨单/费用合同/调拨申请单）；
- **会计期间**：5 列（期间号/起始日期/结账日期/天数）+ 12 行（1期~12期）+ 保存；
- **系统重建**：2 列（选项/描述）+ 12 个复选项 + **必须输入登录密码**才能确定；
- **财务期初**：5 个 Tab（银行现金期初 / 应付期初 / 应收期初 / 固定资产期初 / 资产负债期初）。

### 4.2 与 `CategoryListLayout`（客户列表页用的壳）的关系：**神似而不同**

| | 客户列表页 `md/customer` | ql361 设置页（参数类） |
|---|---|---|
| 外壳 | **`CategoryListLayout`**（已有组件） | 无专用组件（自定义两栏） |
| Tab 位置 | **顶部**（全部客户/会员管理/全部联系人/客户级别/区域管理） | **左侧纵向**（行业设置/流程启用/…） |
| 左栏内容 | **分类树**（可折叠、有层级、带"显示层次结构"） | **扁平标签按钮**（无层级、无折叠、无搜索） |
| 右栏内容 | 数据表 | 表单卡片 |
| 共同点 | 只有「左导航 + 右内容」这个两栏壳 | 同左 |

**结论（施工口径）**：
- **列表类设置页**（操作日志/系统任务/库存期初/会计期间…）→ 直接走**路线 A**（`ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable`），与其它模块一致；
- **参数类设置页**（系统参数/打印设置/企业信息…）→ 走**路线 A′（配置页形态）**：`ErrorBoundary > PageContainer(full-height)` + 自绘「左列纵向视图选择器 + 右栏分组卡片表单」，**复用现有 `a-tabs` 的纵向模式或新建一个通用 `SettingsLayout` 组件**；
- **本系统现状：全站没有「左列纵向标签」的通用组件**（`grep -rln "settings-nav|vertical-tab|left-tab" components/ views/` → 0 命中）→ 若要复刻，**需新建一个通用组件**（按《页面组件优先复用》，先确认确无对应再建，且建成后归入 `components/` 供设置模块内多页复用）。

---

## 5. 业界对标口径

### 5.1 ql361 设置域实测菜单结构（2026-09-18，`tool-results/ql361/设置-live/_menu.json`）

| 分组 | 页面（逐字） |
|---|---|
| **系统配置**（7） | 菜单配置 · 系统参数 · **打印设置** · 审核设置 · 支付配置 · 企业信息 · 应用中心 |
| **期初录入**（2） | 库存期初 · 财务期初 |
| **账套操作**（3） | 系统重建 · 操作日志 · 系统任务 |
| **财务设置**（1） | 会计期间 |

**与我们菜单的对照**：

| ql361 | 我们 | 差异 |
|---|---|---|
| 系统配置 · 菜单配置 | 80620 菜单配置 | ✅ |
| 系统配置 · 系统参数 | 80621 系统参数 | ✅ |
| 系统配置 · **打印设置** | **80930** | ✅ **已建**（2026-09-18，挂 `61205 打印管理`，见 §2.3） |
| 系统配置 · 审核设置 | 80622 审核设置 | ✅ |
| 系统配置 · 支付配置 | 80623 支付配置 | ✅ |
| 系统配置 · 企业信息 | 80624 企业信息 | ✅ |
| 系统配置 · 应用中心 | 80625 应用中心 | ✅ |
| **期初录入** · 库存期初 / 财务期初 | **数据录入** 70550 / 70551 | ⚠️ 分组名不同（我们叫"数据录入"） |
| 账套操作 · 系统重建 / 操作日志 / 系统任务 | 70560 / 80630 / 70561 | ✅（顺序不同：我们 sort=2/2/3 有重号） |
| 财务设置 · 会计期间 | 70570 会计期间 | ✅ |
| —（ql361 无） | 工作流 801 / 80610、审批 802 / 803 / 804 | ➕ **本系统独有 5 页** |

> **ql361 无工作流域**：它的"待审批单据 / 业务草稿 / 经营历程"挂在**分析域 → 综合单据**组（见 `tool-results/ql361/menus-full.json`），是**只读台账**性质的（查审批中的单据），**不是**可配置的审批流引擎。因此本系统的工作流/审批 5 页**没有对标页**，按 §5.3 口径建模。

### 5.2 ql361 设置域 13 页逐页实测摘要（文档引用的原始证据）

| 页面 | 形态 | 实测要点（列/字段/按钮） | 证据文件 |
|---|---|---|---|
| 菜单配置 | 左分组 + 右开关列表 | 左侧分组（外勤拜访/订货业务/销售业务/…），右侧每页面一行「开/关」 | `菜单配置.json` / `.png` |
| 系统参数 | 左纵向 8 标签 + 右卡片 | 8 个标签：行业设置·流程启用·单据设置·库存设置·财务设置·数据权限·消息提醒·其他；卡片式开关/下拉 + `?` 帮助 + 温馨提示 + 底部保存 | `系统参数.json` / `.png` |
| 打印设置 | 左纵向 6 标签 + 右表单 | 标签：箱号模板·物流模板·条码模板·套餐条码·批次条码·货位码；表单：允许打印草稿·属性商品汇总打印·批次效期商品汇总打印·打印内容·单据打印小数位数（数量/单价各 N 位）·助手打印·远程打印 | `打印设置.json` / `.png` |
| 审核设置 | 3 列表 + 行内设置 | 列：单据 / 审核设置 / 摘要；16 类单据；摘要文案如"商品低于成本价时提交给[杨生淮]审核;…" | `审核设置.json` / `.png` |
| 支付配置 | 4 Tab | 微信公众号配置 / 支付方式 / 场景配置 / 在线退款 | `支付配置.json` / `.png` |
| 企业信息 | 2 Tab | 企业信息 / 纳税人信息 | `企业信息.json` / `.png` |
| 应用中心 | 信息卡 + 功能开关 | 公司名称·剩余短信 100 条·物流查询 1000 次·到期日期 2027-07-23（剩余 308 天）·购买记录；功能模块：聚合支付(开)·电子面单(关)·腾讯微企付(关)·硬件服务(商米L2)；短信及其他：短信包·企汇存储空间·物流查询·智能排线·客户服务·WMS实施费·数据恢复 | `应用中心.json` / `.png` |
| 库存期初 | 分类树 + 11 列表 | 列：商品名称·货号·条码·规格·型号·产地·小单位·期初数量·期初成本单价·期初金额；左树"当前路径:全部商品"；空态文案"还没有内容哦 请输入查询条件点击查询吧！" | `库存期初.json` / `.png` |
| 财务期初 | 5 Tab 表格 | 银行现金期初(科目编号/科目名称/期初金额) · 应付期初(供应商编号/名称/应付金额/预付金额) · 应收期初(客户编号/名称/默认经手人/应收金额/预收金额) · 固定资产期初(科目编号/名称/期初金额) · 资产负债期初(科目编号/名称/借贷方向/期初金额) | `财务期初.json` / `.png` |
| 系统重建 | 2 列表 + 复选 + 密码 | 12 个选项：业务草稿·库存期初·买家账号·往来期初·银行现金期初·商品·仓库区域·货位信息·往来单位·职员·操作员·发票；**必输登录密码**；顶部警告"系统重建将清除 所有单据、订单 和以下选项数据且 不能恢复！" | `系统重建.json` / `.png` |
| 系统任务 | 9 列列表 | 列：任务ID·任务类型·任务名称·任务状态·创建人·任务创建时间·任务开始时间·任务结束时间·结果查看；行内「下载处理结果」；实测共 235 条 | `系统任务.json` / `.png` |
| 操作日志 | 2 Tab 列表 | Tab：系统日志 / 登录日志；列：时间·操作员·姓名·内容 ｜ 时间·操作员·姓名·登录类型；工具栏 刷新·打印(F8)·导出；实测共 855 条、每页 20 行 | `操作日志.json` / `.png` |
| 会计期间 | 5 列表 + 保存 | 列：期间号·起始日期·结账日期·天数；12 行（1期~12期，实测 2026 全年） | `会计期间.json` / `.png` |

**抓取方法**：`NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-domain-crawl.cjs 设置`（不带页面名=抓全部 13 页）。产物 `tool-results/ql361/设置-live/<页>.json` + `.png` + `shots/<页>-列配置弹窗.png`。
> ⚠️ **抓取自检**：每页 JSON 的 `desktopTab` 必须等于页名，否则该页不可信（ql361 是 MDI，已打开页面的 DOM 会残留）。本次 13 页 `desktopTab` **全部等于页名**，结果可信。

### 5.3 借什么、不借什么（沿用 2026-09-15 已裁定口径）

1. **借**：做法、字段、交互形态（状态机、卡片式布局、分组配置、`?` 帮助气泡、温馨提示、二次确认、密码二次验证、幂等约束）。**卡片式也是交互，要借鉴。**
2. **实现**：能用现成组件就用现成组件；现成组件不满足**可以新建**；但**颜色、样式、字体必须与本系统现有代码统一**（主题配色照现有样式，不照抄 ql361 的橙色/圆角/阴影）。
3. **留痕**：官方文档引用保留 —— 标题 + URL + 章节 + 查阅日期，并标注口径来源是**官方文档**还是**行业实践**。
4. **不借**：业界界面的**视觉样式**（配色/圆角/阴影/字体/组件外观皮肤）。
5. **不足处按业界补齐**：无对标页（工作流/审批 5 页、打印设置以外的扩展）→ 按业界成熟经验做，但**不发明本系统无法支撑的字段**（无数据源的能力如实标注为缺口，不造假数据）。

### 5.4 工作流/审批 5 页的业界参照

ql361 无工作流引擎，本系统的手写引擎（无 Flowable/Activiti/Camunda，见 `_工作流汇总.md` §八）按下列业界产品参照：

| 系统 | 官方文档入口 | 本模块主要参照点 |
|------|-------------|-----------------|
| **Odoo 17/18** | `https://www.odoo.com/documentation/18.0/` | 审批（Approvals）与自动化规则（Automation Rules）、`mail.activity` 计划任务、审批人与职责分离 |
| **SAP** | `https://help.sap.com/` | **Release Strategy**（按金额/条件解析审批层级与审批人集合）、工作流 `SWDD` 定义、`SWI1` 工作项收件箱 |
| **金蝶云·星空 / 苍穹** | `https://vip.kingdee.com/`、`https://help.kingdee.com/` | 工作流设计器、审批节点（会签/或签/条件分支）、流程版本与发布 |
| **用友 YonSuite** | `https://success.yonyou.com/`、`https://fwq.yonyou.com/` | 协同云统一审批流配置（固定流/自由流/参与人/分支条件）、多级审批 |
| **Flowable / Camunda**（开源引擎参照） | `https://documentation.flowable.com/`、`https://docs.camunda.org/` | BPMN 2.0 建模、任务认领/转办/委派、Misfire 与超时升级 |

> **⚠️ 写作纪律**：凡引用上表能力，**必须在文档中标注来源级别（A/B/C/D）**；**D 级（推论/未证实）一律不得写成"业界标准"**。

---

## 6. 写作与开发原则

- **两类配置严格分开**：查询条件/功能按钮 →「页面配置」；默认列 + 全量可配置列 →「数据表格列配置」。
- **不重复通用规范**：统一指向《对标开发技术参考文档》与《交易模块/_开发指南-金标准.md》。
- **不发明字段**：列名/按钮逐字取自本系统源码；业界补充项**必须单独标注为"业界建议、本系统未实现"**。
- **页面组件优先复用**、**功能/模块不重复开发**（列配置、分页、上传、科目选择器、下拉等一律复用现成组件）。
- **阶段结束前回写开发文档**：把「实现差异说明」更新为当前代码实际实现情况。
- **模块级 E2E（已落地）**：`tools/e2e-settings.cjs` —— **208 项全过**（退出码 0；第一轮 164 → 二轮 201 → 三轮 208）。覆盖 ① 菜单完整性（26 项，含 `80930 打印设置` 挂 `61205`、`client_type` 校验）② 接口探针 27 条 ③ 只读 DB 对账 14 条 ④ 写库→三级回读→按原值复原（系统参数/支付配置/打印设置/企业信息/会计期间/菜单配置/财务期初）⑤ 工作流权限码与真聚合统计 **⑤b 未闭环项收口验证（33 条断言）** ⑥ 页面外壳金标准静态自检 ⑦ Playwright 16 页渲染 + 逐页内容断言 + 截图 **⑦d 「双击行」入口真机验证 + ⑦e 改造后可达页回归巡查**（详见 §11.8）。
  - 用法：`node tools/e2e-settings.cjs`；`SET_PORT=5691 FE_URL=http://localhost:5657` 可指向本会话自建实例；`SET_ONLY=api` 跳过 UI 节。
  - **必须配专用账号**：先执行 `tools/e2e-settings-user.sql`（建 `e2e_settings`）。本项目 `sa-token.is-concurrent=false`（新登录踢旧会话），多会话并行时共用 `admin` 会**互相顶替**，UI 侧表现为「每个页面都跳登录页」的假失败。
  - 产物：`tool-results/e2e-settings/`（`ui-*.png` 16 张 + 运行日志）。
- **【金标准】启动进程随手关**：见 `AI_DEVELOPER_RULES.md` 7.3 会话收尾纪律；按端口/PID 精确 kill，勿用文件名批量匹配。

---

## 7. 全局缺口汇总（P0 → P2）—— **施工前清单**

> 🔴 **本节是施工前的缺口清单（历史快照）**。每条的实际处置结果见 §10.3「缺口逐条处置表」。
> 级别口径：**P0** = 页面不可用/数据不落库/越权；**P1** = 与对标不符或能力缺失但页面能用；**P2** = 体验/一致性。
> 下列结论来自静态源码比对（`tool-results/docgen/evidence/设置模块/`）；**本机 5655/8080 当时无监听进程，未做运行期复验**，凡"预期 404/400"处均已标注。

### 7.1 P0（14 条）

| # | 页面 | 缺口 | 证据 | 影响 |
|:-:|------|------|------|------|
| 1 | 系统重建（70560） | **表格永远为空、查询/重置完全无效，且后端零控制器、库零表** | 给 `BillTableList` 传了**不存在的 prop `apiUrl`/`params`** 并调 **未暴露的 `reload()`**（`components/BillTableList/BillTableList.vue:283-315` 无这两 prop、`:432-435` 无 `reload`）；`views/set/rebuild/index.vue:46,47,109,117` | 整页假死；且与 ql361 的「系统重建」完全不是一回事（对标是"清数据 + 输密码"的危险操作页） |
| 2 | 系统任务（70561） | **同上**（表格恒空 + 零后端零表） | `views/set/system-task/index.vue:63,64,127,136` | 整页假死；对标是"异步任务台账（实测 235 条，可下载结果）" |
| 3 | 财务期初（70551） | **后端零控制器、库零表**；前端 218 行的查/增/改/删全打空 | 无 `%initial%` 表；前端 `views/set/initial-finance/index.vue:148,161,191,194,206` 调 4 个端点全无对应 | 整页空壳 |
| 4 | 财务期初（70551） | **科目下拉请求路径写错** | 前端 `/finance/subject/list`，正确为 `/erp/finance/subject/list`（`erp-finance/.../AccountSubjectController.java:36,50-53`） | 科目永远加载不出 |
| 5 | 库存期初（70550） | **产品下拉永远为空 → 必填项无法满足 → 无法提交** | `productOptions` 从未被赋值（`loadOptions` 只加载仓库，`views/set/initial-stock/index.vue:136,181-186`） | 页面不可用 |
| 6 | 库存期初（70550） | **3 个写端点全部不存在** | 前端调 `POST /set/initial-stock`、`PUT /{id}`、`DELETE /{id}`；后端只有 `POST /save`、`PUT /update`、`GET /export`（`InitialStockController.java:30-83`） | 写操作全 404 |
| 7 | 库存期初（70550） | **「规格」「金额」两列恒空** | VO/DTO 与表 `erp_stock` 均无 `specification`/`amount`（`InitialStockVO.java:10-16`） | 与对标 11 列中的 2 列对不上 |
| 8 | 系统参数（80621） | **整页数据来自 JVM 内存，所有写操作都是假的** | `SystemConfigServiceImpl` 用 `static final Map BUILTIN_CONFIGS`（12 条）实现全部读写；"分页"是假分页（`pages` 恒 1、`pageNum/pageSize` 被忽略）；`deleteConfigByKey` 对内置信恒 `false`（页面无视返回值提示"删除成功"）；`batchDelete`/`deleteConfig`/`refreshCache` **空实现**；DB 的 `sys_config` **8 行永远读不到**（键名两套完全对不上）。`config/service/impl/SystemConfigServiceImpl.java:26,30-46,49-61,104-114,129-143,168-171,187-194,237-245` | 配置**存不下**，重启即失 |
| 9 | 系统参数（80621） | **内置配置从不 `setId` → 全部行 `id` 为 null** | 同上；表格 `row-key="id"` 失效，新增的配置**也不会出现在列表中** | 列表行错乱 + 新增不可见 |
| 10 | 支付配置（80623） | **渠道参数「假保存」** | 走 `/config/save-value` **写缓存不落库**，回读走 `/config/list` 只返回内置 12 条 → **保存后重新打开抽屉永远为空**，却仍提示"保存成功"。`views/set/payment-config/index.vue:265,284,289`；`api/payment/index.ts:43,47-74` | 配置白填 |
| 11 | 操作日志（80630） | **取数路径错误致列表恒空** | `result.data.records`（`utils/request.ts:207-221` 已拆包）→ `TypeError` 被吞成"查询失败"。`views/set/operation-log/index.vue:249,250` | **整页永远查不出数据**（`sys_oper_log` 实有 **13465 行**） |
| 12 | 操作日志（80630） | **假导出 + 清空语义不符 + 零权限注解 + 租户硬编码** | 导出拿到 Blob 后**无任何下载处理**却提示"导出成功"（`:273-285`）；"清空"实际只删 90 天前（`SysLogStubController.java:126-136`）；6 个端点**零权限注解**；`DEFAULT_TENANT_ID = 1L` 忽略请求头租户（`:31`） | 数据造假提示 + **越权可清日志** + 跨租户口径错误 |
| 13 | 企业信息（80624） | **15 个字段中 11 个永远空、连"企业名称"都存不上** | `SysTenant` 实体只有 15 列，页面用 `companyName/creditCode/companyType/legalPerson/registeredCapital/industry/companyScale/establishDate/businessScope/contactPersonPhone/contactPersonEmail` 等**实体不存在的字段**；「企业名称」以 `companyName` 提交而实体收 `tenantName` → 静默丢弃。`views/set/company-info/index.vue:255-271,288` | 企业档案存不下来 |
| 14 | 我的已办（804） | **进入后显示的是「待办」** | 803/804 归一化后**同一 key**（`router/dynamicRoutes.ts:922`）；组件 `task-management.vue` **零路由读取**（`useRoute`/`query.`/`defineProps` 命中 0），Tab 初值**硬编码** `ref('todo')`（`:508`） | 菜单语义与落点相反，用户不手点第二个 Tab 永远看不到已办 |

**另 3 条同源的 P0（工作流组，与上表并列）**：

| # | 页面 | 缺口 | 证据 |
|:-:|------|------|------|
| 15 | 流程定义 801 / 流程设计 80610 | **两菜单 100% 同一界面，页内标题恒为「流程设计」** | 两菜单 DB `component` 逐字相同；`designer/index.vue:4` `title="流程设计"`；组件零路由读取 |
| 16 | 流程实例 802 / 待办 803 / 已办 804 | **6 个 `workflow:*` 权限码在 `sys_permission` 中全部不存在**（`SELECT ... LIKE 'workflow%'` → **0 行**，表共 264 行） | 前端用 `workflow:instance:view/diagram/intervene`（`instance-monitor.vue:178/186/195`）、`workflow:task:view/approve/transfer`（`task-management.vue:203/212/221`）；指令 `directives/permission.ts:49` 置 `display:none` → **非超管用户看不到任何行内操作** |
| 17 | 我的待办/已办 803/804 | **任务优先级恒为「中」（硬编码桩）** | `WorkflowServiceImpl.java:1497` `record.put("priority", "medium")` → "高优先级"统计卡恒 0 |

### 7.2 P1（与对标不符 / 能力缺失，页面尚能打开）

| 页面 | 缺口 | 证据 |
|------|------|------|
| 全模块 12 页 | **无 `PageConfigPanel`、无 `storage-key`、无列配置齿轮、无 `defaultHidden`** —— 金标准自检第 5/6 项全数不通过 | §3.1 |
| 全模块 | **无 `StandardPagination` 经典分页** | `grep StandardPagination FE/views/set/` → 0 |
| 全模块 | **查询区未采用横向自适应网格**（一律 `a-form layout="inline"` 单行流式） | 各页证据 §3 |
| 全模块 | **前端零 `v-permission`**，而后端写端点多数要求权限码 → 非超管预期 403 | 各页证据 §4 |
| 菜单配置（80620） | **`/menu/tree` 少传必填 `tenantId`** → 预期 400 | 后端 `SysMenuController.java:83` `@RequestParam Long tenantId` 必填；前端裸调不带参（`views/set/menu-config/index.vue:249`；`utils/request.ts:127-129` 只注入请求头） |
| 菜单配置（80620） | **「权限标识（`perms`）」无落库位置** | `SysMenu` 实体与 `sys_menu` 表**均无 `perms` 列**（实际列名是 `menu_code`）→ 该列编辑后静默丢弃（`:240,187-192`） |
| 菜单配置（80620） | **列名用 `sort` 而非实际列 `sort`… 且未传 `tenantId`** | 详见《菜单配置开发文档》§11；另 `views/system/menu/index.vue` 的 `perms` 同样不存在（见《系统模块 README》） |
| 审核设置（80622） | **页面顶部 alert 与后端事实相反** —— 声称"更新、停用、删除接口尚未开放"，实际 `WorkflowController.java:92/106/119/132` **已全部实现** | `views/set/audit-config/index.vue:4-10` |
| 审核设置（80622） | **编辑器 `form.vue` 无菜单/路由入口**；其编辑态保存只 `message.info` 不提交 | `views/set/audit-config/form.vue:187-188,166,193` |
| 应用中心（80625） | **核心区恒空 + 整页 0 个按钮** | `sys_tenant_module` 实测 **0 行** → "已开通模块"永远 `a-empty`；套餐区依赖 `platform:tenant-package:list` 权限，无权限时**静默隐藏**（`views/set/app-center/index.vue:108-111,195-198,205-208`） |
| 操作日志（80630） | **存在功能重叠的第二套 `/api/system/log`（权限注解齐全）却无人引用** | `core-base/.../LogManageController.java`（14 端点） |
| 会计期间（70570） | **走路线 B（`ARReportPage`）**，而该路线已明令不再新增 | `views/set/accounting-period/index.vue:3-51` |
| 流程实例/待办（802/803） | **多个查询条件静默失效**：`priority`/`startDate`/`endDate` 前端下发、后端签名**无这三个参数**；实例页 `startDate`/`endDate` 后端**收到但未传给 service** | `WorkflowController.java:294-301`、`:203-209`；`task-management.vue:615-619` |
| 流程设计（801/80610） | **启停流程会意外递增版本**：开关用 `create`（POST 新建）传 `{...detail, enabled}`，后端见 `definitionId` 即转 update，每次 +版本 | `designer/index.vue:640`；`WorkflowServiceImpl.java:103/128` |
| 流程设计（801/80610） | **前端 `workflowDefinitionApi` 7 条路径与后端不符**（`/definitions/page` 被路径变量吞为 `definitionId="page"`；publish/disable 方法 PUT vs 后端 POST；delete 未接线） | `api/workflow/index.ts:21-43` |
| 流程实例（802） | **前端 `workflowInstanceApi` 4 条路径与后端不符**（`/instances/{id}/history` 不存在） | `api/workflow/index.ts:64-77` |
| 我的待办/已办（803/804） | **前端 `workflowTaskApi` 5 条路径后端全部不存在**（`/workflow/tasks/**`） | `api/workflow/index.ts:96-112` |
| 我的待办/已办（803/804） | **统计卡口径错误**（当页条数冒充全量）；**已办无「处理时间/审批结果/审批意见」列**（后端已返回） | `task-management.vue:522-524,531-540`；`WorkflowServiceImpl.java:1504-1505` |
| 流程实例（802） | **统计卡口径错误**（当页条数冒充全量）；**实例状态前端仅 4 值 vs 后端 7 值**（`rejected`/`withdrawn`/`cancelled` 无法筛选，列表显示英文原文） | `instance-monitor.vue:416-418,116-127,601-609`；`WorkflowConverter.java:36-57` |
| 流程分析（无菜单） | **首屏最多 30 秒展示写死的 2024 年 mock 数据**；另有 2 处"图表组件开发中…"占位；无菜单入口 | `process-analysis.vue:329-334/345-374/392-396/410-474`、`:546-557`、`:279,288` |

### 7.3 P2（体验与一致性）

| 缺口 | 说明 |
|------|------|
| ~~`61205 打印管理` 是**空分组**~~ | **已修**（2026-09-18）：已挂子菜单 80930 打印设置（§2.3）；原症状为前端渲染出一个点不开任何页面的菜单组 |
| `61203 账套操作` 内 **`sort` 重号** | `70560 系统重建` 与 `80630 操作日志` 都是 `sort=2` → 显示顺序不确定 |
| `80610 流程设计` 的 `path` **无前导斜杠**（`set/workflow-designer`） | 与同组 `801` 的 `/workflow/definition` 不一致 |
| `804 我的已办` 的 `component` **带 `.vue` 后缀** | 与全站写法不一致（`task-management.vue` vs `task-management`） |
| 跨页事件 `workflow:create` **全组 3 页监听、无任何页面派发** | 死代码（`instance-monitor.vue:631`、`task-management.vue:812`、`process-analysis.vue:555`） |
| `TaskTransferRequest.type` 后端接收但**未使用**（"委托"与"转办"共用端点与字段） | `WorkflowController.java:599`、`:340-359` |
| 「退回」被实现为 `reject`，只把目标节点写进备注，**真正的回退流转未实现** | `WorkflowServiceImpl.java:884-889` |
| 前端 `TASK_ACTION_MAP` 仅 3 值 vs 后端 7 值 | DB 实测 `action=4`(12 行)、`7`(4 行) 未被覆盖（`api/workflow/index.ts:206-209`） |
| 「流程信息」弹窗 `@ok` **仅关闭不提交**，易误解为保存 | `designer/index.vue:190` |
| `componentMap` 两条**不可达冗余注册** | `router/dynamicRoutes.ts:119-120` |
| **无模块级 E2E、无验收截图资产** | 见 §6 |

---

## 8. 本模块的施工建议（路线选择）

> 施工顺序建议：**先把"数据落库"修好（§7.1），再做外壳金标准（§3.1），最后补能力（§7.2）**。理由：外壳换掉但数据仍不落库，验收一样不过。

| 批次 | 内容 | 覆盖 |
|:----:|------|------|
| **B0** | 修 P0：系统重建/系统任务（补后端+表 or 按对标重做）、财务期初（补后端+表）、库存期初（补 VO/端点/下拉）、系统参数（改真落库 `sys_config`）、支付配置（改真落库）、操作日志（修取数 + 权限 + 租户）、企业信息（补列或改字段名）、804（读路由）、workflow 权限码种子 | 9 页 |
| **B1** | 外壳升路线 A：列表类 6 页（操作日志/系统任务/库存期初/会计期间/菜单配置/审核设置） | 6 页 |
| **B2** | 外壳升路线 A′（配置页形态）：参数类 5 页（系统参数/打印设置/企业信息/支付配置/应用中心）+ 评估是否新建 `SettingsLayout` 通用组件 | 5 页 |
| **B3** | 工作流/审批 5 页：统一读路由、补权限码、补缺失列、修 API 路径 | 5 页 |
| **B4** | 补 `打印设置` 页（菜单挂 `61205`）+ 收敛 `操作日志` 双实现 + 处置 `process-analysis` 孤儿 | 3 项 |

---

## 9. 文档集清单

```
docs/Yh-Spec/手动整理对标开发文档/设置模块/
  README.md                  （本文件）
  菜单配置开发文档.md         80620
  系统参数开发文档.md         80621
  审核设置开发文档.md         80622
  支付配置开发文档.md         80623
  企业信息开发文档.md         80624
  应用中心开发文档.md         80625
  库存期初开发文档.md         70550
  财务期初开发文档.md         70551
  系统重建开发文档.md         70560
  系统任务开发文档.md         70561
  操作日志开发文档.md         80630
  会计期间开发文档.md         70570
  打印设置开发文档.md         ★2026-09-18 已建（菜单 80930，挂 61205 打印管理；配置消费待接线）
  流程定义开发文档.md         801
  流程设计开发文档.md         80610
  流程实例开发文档.md         802
  我的待办开发文档.md         803
  我的已办开发文档.md         804
```

**取证素材**（供复核）：`tool-results/docgen/evidence/设置模块/`（19 个 `.md`，含 `_汇总.md`、`_工作流汇总.md`）；
**对标素材**：`tool-results/ql361/设置-live/`（13 页 JSON + PNG + `shots/` 弹窗截图 + `_menu.json`）。

---

## 10. 施工收口（2026-09-18 本轮 · 18 页全量）

> 本节是**当前实际状态**的唯一权威描述。§3 / §7 保留的是施工前快照，如有冲突以本节为准。

### 10.1 验收结论

> **二轮收口后，本节的三处数字已被 §11 取代**（164 → **201** 项；覆盖新增 ⑤b/⑦d 两节）。本表保留第一轮快照。

| 项 | 结果 |
|---|---|
| 模块级 E2E | **`tools/e2e-settings.cjs` 164 项全过**（退出码 0，0 失败 0 跳过）→ **二轮 201 → 三轮 208 项全过，见 §11** |
| 覆盖 | 26 条菜单完整性 · 27 条接口探针 · 14 条只读 DB 对账 · 7 组「写库→回读→复原」 · 工作流权限码与真聚合 · 16 页外壳静态自检 · 16 页 Playwright 渲染 + 逐页内容断言 |
| 证据产物 | `tool-results/e2e-settings/`（16 张页面截图 + 逐轮运行日志，末端为 `final.txt`） |
| 专用账号 | `tools/e2e-settings-user.sql` → `e2e_settings`（**必须用**：`sa-token.is-concurrent=false` 下共用 `admin` 会与并行会话互相顶替，UI 表现为「每页都跳登录」的假失败） |

### 10.2 页面外壳：18 页全部达标

| 路线 | 页面 | 形态 |
|---|---|---|
| **A（列表页）** | 菜单配置 80620 · 审核设置 80622 · 支付配置 80623 · 库存期初 70550 · 财务期初 70551 · 系统重建 70560 · 系统任务 70561 · 操作日志 80630 · 会计期间 70570 · 流程定义 801 / 流程设计 80610 · 流程实例 802 · 我的待办 803 / 我的已办 804 | `ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable/BillTableList` + 表头 rowNo 齿轮列配置（`storage-key` = `global-config-key`）+ 经典分页 `StandardPagination` |
| **A′（配置页）** | 系统参数 80621 · 企业信息 80624 · 应用中心 80625 · 打印设置 80930 | `ErrorBoundary > PageContainer(full-height)` + 分区卡片表单 / 左列纵向标签 + 右栏内容 |
| **查询区** | 8 个有查询区的页面 | 一律横向自适应网格 `repeat(auto-fill, minmax(190px,1fr))` + `useAutoGridSpan`，**无纵向单列** |

**新建的通用组件**：`frontend/apps/pc-admin/src/components/SettingsLayout/`（左列纵向标签 + 右栏内容插槽），供设置模块参数类页面复用（系统参数 / 打印设置已在用）。

### 10.3 缺口逐条处置表（对应 §7）

| §7 条目 | 处置 |
|---|---|
| P0-1/2 系统重建、系统任务「传了不存在的 prop `apiUrl`/`params`、调未暴露的 `reload()`」 | ✅ 均已按对标重做（重建=危险操作页；任务=复用 `scheduled_task_log` 的台账） |
| P0-3/4 财务期初「零控制器零表 + 科目路径写错」 | ✅ `V11.399.0` 建两表 + 5 权限码；科目路径改 `/erp/finance/subject/list` |
| P0-5/6/7 库存期初「产品下拉恒空 + 3 写端点不存在 + 规格/金额恒空」 | ✅ `InitialStockQueryMapper`（JOIN 商品 + 递归分类）+ 补 `DELETE /{id}`；顺带修「先清空再插入误删他人期初」 |
| P0-8/9 系统参数「读 JVM 内存假分页假增删改 + 内置行 id 为 null」 | ✅ `SystemConfigServiceImpl` 删内存 Map 改真实读写 `sys_config`（`V11.393.0` 补列 + seed）；`/config/page` 真分页，id 非空 |
| P0-10 支付配置「渠道参数假保存」 | ✅ 真落 `sys_project_config`（`config_group='payment'`），保存→回读闭环 |
| P0-11/12 操作日志「取数路径错误致列表恒空 + 假导出 + 零权限 + 租户硬编码」 | ✅ 重写 `SysLogStubController`：修取数、真导出、补 6 权限码（`V11.394.0`）、改会话租户口径 |
| P0-13 企业信息「15 字段 11 个无列、企业名称存不上」 | ✅ `V11.396.0` 给 `sys_tenant` 补 15 个可空列；`tenantName` 绑定修复；`GET/PUT /tenant/current` 往返闭环 |
| P0-14 我的已办「进入后显示待办」 | ✅ 组件改读路由（`route.name` + `route.path` 兜底）决定初始 Tab 与标题；E2E 逐页断言两页落点不同 |
| P0-15 流程定义/流程设计「两菜单 100% 同一界面」 | ✅ 读路由分流（801 台账 / 80610 设计器），标题、空态同步 |
| P0-16 `workflow:*` 权限码 0 行 | ✅ 13 个码全部落库（`V11.398.0` / `V11.403.0` / `V11.404.0` / `V11.405.0`）+ 方法级 `@SaCheckPermission` + 前端 `v-permission` |
| P0-17 任务优先级硬编码 `"medium"` | ✅ `V11.405.0` 加**可空列** `priority`，如实回传 NULL（前端显示 `-`），**不造假**；筛选真生效 |
| P1 全模块无 `PageConfigPanel`/`storage-key`/`StandardPagination`/横向网格/`v-permission` | ✅ 逐页补齐（对标无「页面配置」的页按裁定**不加**，如审核设置/系统任务） |
| P1 菜单配置 `/menu/tree` 缺 `tenantId` → 400 | ✅ 不再调它，改走新端点 `/set/menu-config/list`（租户取会话） |
| P1 菜单配置「`perms` 列无落库位置」 | ✅ 按真实列名 `menu_code` 订正（表里没有 `perms`） |
| P1 审核设置「顶部 alert 与后端事实相反」 | ✅ alert 已删除；`form.vue` 收敛为真实保存的配置弹窗 |
| P1 应用中心「核心区恒空 + 0 按钮 + 套餐区静默隐藏」 | ✅ `V11.397.0` 按真实模块回填 `sys_tenant_module`；无权限改为**显式提示块** |
| P1 操作日志「`/api/system/log` 平行实现无人引用」 | ✅ 裁定保留 `SysLogStubController` 为唯一在用实现，`LogManageController` 不删并在类注释登记为平行实现 |
| P1 会计期间「走路线 B」 | ✅ 升级路线 A（12 期固定矩阵，`:min-rows="12"`），新增 `PUT /erp/finance/period/batch-dates` |
| P1 流程实例「统计卡口径错 + 状态 4 vs 7 + 日期未透传」 | ✅ `GET /workflow/instance/stat` 真聚合；7 状态映射；`startDate`/`endDate` 透传；详情改 `GET /workflow/instance/detail` |
| P1 我的待办/已办「统计卡口径错 + 已办缺 3 列 + API 路径全不存在」 | ✅ `GET /workflow/task/stat`；已办补「审批结果/审批意见/处理时间」；`workflowTaskApi` 路径逐条订正 |
| P1 打印设置「应有而未建」 | ✅ `V11.402.0` 建 `set_print_config` + 菜单 **80930**；前端 `views/set/print-config/index.vue`（路线 A′） |
| P2 `804` 的 `component` 带 `.vue` 后缀 / `80610` 的 `path` 无前导斜杠 / `sort` 重号 | ⚠️ 未改（不影响渲染；`component` 归一化规则本身能兜住）。如需统一，走单独一次菜单数据订正 |

### 10.4 本轮的两处**组件级**修复（影响全站，勿回退）

1. **`MyBatisPlusConfig.metaObjectHandler()` 补上 `version` 的 INSERT 填充**。
   `BaseEntity.version` 带 `@TableField(fill = FieldFill.INSERT)`，该注解会让 MyBatis-Plus **把 version 列写进 INSERT**，而处理器此前没有填充它 ⇒ 显式插入 NULL ⇒ 凡是 `version NOT NULL` 的表一律 `null value in column "version" violates not-null constraint`。本轮 `set_print_config` 实机踩到（GET 直接 400）。修法是统一填 `0`（= 初始版本，与 `@Version` 语义一致；DB 默认值也是 0）。
2. **`SetMenuConfigController` 排除「挂不到一级域」的页面菜单**。
   `6130701 菜单管理` 是一行 `client_type='tenant-admin'` 却挂在 **system-admin** 域（`61307 系统`）下的异类菜单，会让「菜单配置」左分组面板多出一个 `domainId=null` 的空组。现在挂不到一级域的行直接不进本页。

### 10.5 未闭环项逐条处置（**二轮收口后状态**，2026-09-19）

> 本节原先登记 **12 条**未闭环项。二轮已把其中大部分**真正解决**，下表为**处置后**状态。
> ✅ = 已闭环（有代码/迁移证据）；⚠️ = **仍未闭环，如实保留**（不假装已完成）。
> 逐条代码级证据见各页文档的 `## 二轮收口（2026-09-19）` 节。

| # | 页面 | 未闭环项 | 二轮处置 |
|:-:|------|---------|---------|
| 1 | **打印设置** | 7 个配置项在打印链路上尚未被消费 | ✅ **已接线**（见 §11.1 逐项表） |
| 2 | **打印设置** | 「打印内容」下拉只有 1 个候选值 | ✅ **已补齐为 ql361 实测 3 项**（逐字：`批号 *数量` / `生产日期 *数量` / `批号 生产日期~到期日期 *数量`） |
| 3 | 打印设置 | 6 个模板类目无真实载体 | ⚠️ **仍未闭环**：`sys_print_template` 有 `page_code` 但**无 billtype/类目列**、`erp_print_template` 连 `page_code` 都没有（只有 `template_code`/`template_type`），且**两表均 0 行**；`printing/{template,chain,client,task,designer}` 5 页在 `sys_menu` **0 行**（实测）→ 保留「口径说明」，不造假入口 |
| 4 | **系统参数** | 8 个视图里只有「行业设置」「其他」有配置项 | ✅ **已补齐**：`V11.421.0` 落 6 视图 seed，`sys_config` **18 → 173 行**（8 个 `nav_group` 分布 17/14/33/8/5/10/62/24，devdb 实测） |
| 5 | **系统参数** | `locked` 全 false（缺商品引用判定链路） | ✅ **已实现**：读取时按 `erp_product` 引用计数折算 `locked` + `lockedReason`（**不落库**），前端禁用 + 悬浮说明 |
| 6 | 审核设置 | 16 类单据只 3 类有适用条件证据；ql361 弹窗无截图证据 | ⚠️ **仍未闭环**（如实保留） |
| 7 | **财务期初** | 年度取会计年 / 会计期间联动 / 与库存期初对平 | ✅ **三项均已做**（**未新增迁移**，见下 §11.3） |
| 8 | **财务期初** | 应付/应收下拉实际源是 `biz_party` | ✅ **已订正说明（不改数据源）**：`erp_partner` 0 行、`biz_party` 152 行 |
| 9 | **企业信息** | `sys_tenant` 30 列超规范 | ✅ **已拆表**：`V11.420.0` 30 → **15 列** + 新建 1:1 子表 `sys_tenant_profile`（**24 列**） |
| 10 | **企业信息** | 纳税人识别号列名 `tax_number` vs 文档 §4 的 `taxpayer_id` | ✅ **裁定保留 `tax_number`**（全库统一词根；`taxpayer_id` 是拟名、无列名证据） |
| 11 | **企业信息** | LOGO 未做 | ✅ **已做**：上传能力 `POST /api/file/upload` 早已存在；`logo_url` 落子表；页面 Tab① 新增「企业标识」分区，**未改任何共享组件** |
| 12 | 企业信息 | Tab②「公司名称」只读 / 无变更审计 / `level`·套餐未暴露 | ⚠️ **仍未闭环**（如实保留） |
| 13 | 系统重建 | `biz_party_address`/`biz_party_role`/`shop_user_party_link` 无 `tenant_id` 故未清；未建异步任务台账 | ⚠️ **部分闭环**：`SystemRebuildService` 增 `delByParent()`，范围 9「往来单位」已纳入 **`biz_party_address`** 与 **`shop_user_party_link`**（条件 `... WHERE party_id IN (SELECT id FROM biz_party WHERE tenant_id = ?)`；**count 预估与 DELETE 共用同一 WHERE**，单事务、先子后主；`preflight` 增校验「关联列存在 + 父表有 tenant_id」）。<br>**仍未闭环**：`biz_party_role` 实测**不挂 `biz_party`**（只有 `role_name`/`role_code`，而 `biz_party.roles` 是字符串）→ 无法限租户，**保留不删**（宁可不删，不越权）；异步任务台账仍未建（当前为同步单事务） |
| 14 | 系统任务 | 「创建人」对既有历史行恒为 `-` | ✅ **写入侧已闭环**：`V11.416.0` 补 `scheduled_task_log.create_by`；`TaskExecutor` 在**调用线程**解析发起人（`real_name→nickname→username`，线程池切换会丢 Sa-Token 上下文）后透传，有会话记姓名、无会话记「定时调度」，**绝不空**；前端改插槽渲染（空值 `-` + tooltip）。<br>⚠️ **历史 15 行仍为 `-`**：该列本轮才建、姓名列从无写入来源，**无依据可回填，不编造** |
| 15 | 应用中心 | `expire_time` 全租户 NULL；短信/物流查询用量「未接入」 | ⚠️ **部分闭环**：短信已接真实来源（`mkt_sms_setting` + `mkt_sms_record`，`SetAppCenterController` 用 JdbcTemplate 带租户条件，含表缺失降级）；**物流查询仍「未接入」**（全库无计量表、无轨迹查询实现），但文案已说清缺什么；`expire_time` 三租户全 NULL → 保持「长期有效」，**不用 `create_time`+套餐时长推算** |
| 16 | 流程定义 | 台账默认会话下仍为空（种子 4 行 `tenant_id=0`，查询严格 `eq`） | ✅ **已闭环**：`getWorkflowDefinitions` 改为 **`tenant_id = :租户 OR tenant_id = 0`**（全局默认模板 + 租户覆盖，与菜单树 / `sys_config` 同口径）；**写入路径未动**（新建仍落当前租户，不得写全局行）。devdb 实测接口返回 **4 条**（`订单审批流程/请假审批流程/报销审批流程`…） |
| 17 | 流程实例 | 干预理由无后端 400 硬校验；统计卡 4 张；`BillTableList` 未透出 `cell-dblclick` | ✅ **三项均已闭环**：① `intervene` 端点在 `reason` 空白时返回 **400 +「请填写干预理由」**；② 补第 5 张「已挂起」卡（`PauseCircleOutlined`，统计卡改 5 列 CSS Grid）；③ 「双击行开详情」改为**页面侧自行实现**（新增 `composables/useRowDblclick.ts` + `BillDetailTable` 的惰性 `data-row-key`；**共享组件事件契约保持原状**），并已**真机验证**"双击确实打开该行详情"。**方案经过一次返工，详见 §11.8** |
| 18 | 我的待办/已办 | 「高优先级」卡恒 0；退回无法定位目标节点时降级为驳回 | ⚠️ **仍未闭环**（已加 tooltip 说明） |
| 19 | **流程分析**（孤儿页） | 无菜单入口 + 首屏展示写死的 2024 mock 数据 | ✅ **已闭环**：`V11.417.0` 补菜单 **`80611 流程分析`**（挂 `61206`）+ 清 mock + 权限码 `workflow:analysis:view`（详情归《流程实例开发文档》） |

### 10.6 迁移清单（本轮新增，均已应用到 devdb）

| 迁移 | 内容 |
|---|---|
| `V11.393.0` | `sys_config` 补 5 列（17→22）+ 18 行 seed（系统参数真落库） |
| `V11.394.0` | 6 个 `log:*` 权限码 + 授权 `role_id=1`（操作日志） |
| `V11.395.0` | 8 个 `payment:*` 权限码 + 授权（支付配置） |
| `V11.396.0` | `sys_tenant` 补 15 个可空列 + `set:company-info:save` 权限（企业信息） |
| `V11.397.0` | 按真实模块回填 `sys_tenant_module`（应用中心） |
| `V11.398.0` | 建 `sys_audit_rule` + `workflow:audit:*` 两码（审核设置） |
| `V11.399.0` | 建 `erp_initial_finance_subject` / `erp_initial_finance_partner` + 5 权限码（财务期初） |
| `V11.400.0` | `set:rebuild:execute` 权限码（系统重建） |
| `V11.401.0` | `scheduled_task_log` 补 5 列 + `set:system-task:*` 两码（系统任务） |
| `V11.402.0` | 建 `set_print_config` + **菜单 80930 打印设置**（挂 61205）+ 两权限码 |
| `V11.403.0` | 5 个 `workflow:definition:*` 权限码（流程定义/设计） |
| `V11.404.0` | 3 个 `workflow:instance:*` 权限码（流程实例） |
| `V11.405.0` | `workflow_task.priority` 可空列 + 3 个 `workflow:task:*` 权限码（我的待办/已办） |

### 二轮收口新增迁移（2026-09-19，**均已应用到 devdb**）

| 迁移 | 内容 |
|---|---|
| `V11.416.0` | `scheduled_task_log` 补 `create_by`（发起人用户 id，可空；历史行不编造）+ 写入侧 `TaskExecutor` 记录发起人（系统任务） |
| `V11.417.0` | 补菜单 **`80611 流程分析`**（挂 `61206`）+ 权限码 `workflow:analysis:view`（流程分析孤儿页收口） |
| `V11.420.0` | **企业信息拆表**：建 `sys_tenant_profile`（1:1，24 列）+ 搬 15 列数据 + `sys_tenant` 删回 15 列 + 新增 `logo_url`（见下） |
| `V11.421.0` | **系统参数 6 视图 seed**：`sys_config` 18 → **173 行**；并补 `V11.393.0` seed 的 10 行空 `help_text`（只填 NULL，不改键） |

> **`V11.420.0` 细节**：迁移内带**删列前强制断言**（有租户搬不到子表即 `RAISE EXCEPTION`，宁可起不来也不丢档案）与
> **完整回滚脚本（注释形式）**；已在 devdb 用「事务内执行 → ROLLBACK」干跑校验（3 行 → 3 行、缺口 0、复跑幂等、回滚后无残留）。
> **现已在 devdb 正式应用**：`flyway_schema_history` 四条（416/417/420/421）`success = true`，实测 `sys_tenant` **15 列**、
> `sys_tenant_profile` **24 列 / 3 行**、`sys_tenant` **3 行**（`sys_config` 173 行）。

> ⚠️ **并行会话抢注主键的教训**：本轮 `V11.402.0` 初稿的权限 id（`91320/91321`）与已应用的 `V11.398.0` 撞主键（`91321`），会让 **Flyway 在下一次启动时整体中止**。**新增固定 id 的种子前，必须先用 `python tools/dbq.py "SELECT ..."` 实测该 id 空闲**，不要凭注释里的"已核空闲"（那是写入时刻的快照，随时会被并行会话占用）。
> ⚠️ `V11.398.0` 已应用后又改过盘上文件（id 91321/91322 → 91351/91352），**dev 靠 `application-dev.yml` 的 `repair-on-migrate: true` 容忍 checksum 漂移**；生产 profile 没有该开关，需注意（新环境按盘上文件走 `91351/91352`，与 dev 库现状不同号，但 `WHERE NOT EXISTS(permission_code)` 保证语义一致）。
> ⚠️ **迁移「抢号」的教训（再记一次）**：二轮 `V11.420.0` 被**两个 agent 同时取号**（一个先写，另一个发现后改用 `V11.421.0`）。**取号前必须实测**当前最大号（`ls backend/core/api/core-api/src/main/resources/db/migration/ | sort -V | tail -3`），**文档里那句「当前最高 Vx」永远是过期的**，不可作为取号依据。

### 10.7 施工顺序回顾（供同类模块复用）

1. **先修数据落库（P0）**，再做外壳金标准 —— 外壳换掉但数据仍不落库，验收一样不过。
2. **每页一篇文档 + 一次真机验收**，不要"编译通过 + 静态核对通过"就交付。
3. **共享环境三件套**：① 自建实例用**独立端口**（本轮 5691）并从**私有 jar 副本**启动 —— 直接在 `target/` 里跑，会被并行会话的重建**就地换掉 jar**，运行中进程随即 `NoClassDefFoundError`（本轮实踩）；② E2E 用**专用账号**；③ 写库断言一律「改 → 三级回读 → 按原值复原/物理清理」。

---

## 11. 二轮收口（2026-09-19）

> 本节是**二轮收口的实际实现**，取代 §10.5 的旧结论（原表已在上文重写为「处置后状态」）。
> **未新增表结构变更**（除 §11.4 拆表迁移外，本轮其余改动不涉及新表）；**未改任何共享组件的对外契约**。

### 11.1 打印设置：7 个配置项的接线状态（§10.5-1/-2 关闭）

**新增读路径**（供打印组件、免管理权限）：

- 后端 `GET /api/set/print-config/behavior`（`PrintConfigController#getBehaviorConfig`，**无权限注解**，只回行为字段；配置要作用于所有人的打印）。
- 前端 `frontend/apps/pc-admin/src/components/PrintDialog/printBehavior.ts`（**带 5 分钟缓存**；无权限/接口失败/超时一律返回 `null` → 调用方**完全走改造前行为**，绝不让「配置读不到把打印弄挂」）。

| 配置项 | 状态 | 落点 |
|---|:--:|---|
| 允许打印草稿 `allowDraftPrint` | ✅ 已接 | `printBehavior.isDraftDocument` + `PrintDialog.open()` 拦草稿态并提示 |
| 单据打印小数位数 `decimalEnabled` + `qtyDecimal`/`priceDecimal` | ✅ 已接 | `applyPrintBehavior` → 渲染前**数量列/单价列**定长格式化（**金额/税额列不动**） |
| 打印内容 `printContent` | ✅ 已接 | 明细行派生 `batchEffectiveText` |
| 助手打印 `assistantEnabled` | ✅ 已接 | 开启后点「打印」**直接渲染打印**，不再要求先预览 |
| 远程打印 `remoteEnabled` | ✅ 已接 | 关闭 → 远程模式禁用；开启 → 按 `pageCode` 取 **ACTIVE 链路**提交 `POST /v2/print/tasks/by-chain`；**无链路/无客户端时明确拒绝且不建任务** |
| 属性商品汇总打印 `attrSummaryPrint` | ❌ **未接** | `FormatEngineImpl.renderTable` 逐行直出，**无汇总行能力**（不硬造，页面与代码注释已如实标注） |
| 批次效期商品汇总打印 `batchSummaryPrint` | ❌ **未接** | 同上 |

**「打印内容」候选值**：`PrintConfigController.PRINT_CONTENT_OPTIONS` 已补齐为 ql361 实测 **3 项（逐字）**：`批号 *数量` / `生产日期 *数量` / `批号 生产日期~到期日期 *数量`。

### 11.2 系统参数：6 视图 seed + `locked` 折算（§10.5-4/-5 关闭）

**证据来源**：本轮**真实登录 ql361 抓取**（8/8 标签、69/69 个 `?` 气泡、23/23 下拉完整选项集），脚本 `tools/ql361-settings-probe.cjs`（可复跑），报告 `tool-results/ql361/设置-deep/_summary.md`（1314 行）。**这是设置模块第一次**把系统参数 8 标签 / 审核设置 16 单据弹窗 / 打印设置 3 选项采全。

**迁移 `V11.421.0`**：`sys_config` **18 → 173 行**，8 个 `nav_group` 分布（devdb 实测）：

| nav_group | industry | flow | bill | stock | finance | data_perm | notify | other |
|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| 行数 | 17 | 14 | 33 | 8 | 5 | 10 | 62 | 24 |

每行带逐字 `config_name`、`help_text`（`?` 气泡原文）、`tip_text`（温馨提示）、`parent_key`（三层嵌套，消息提醒最深）。同时把 `V11.393.0` seed 的 10 行**空 `help_text` 补上**（只填 NULL，不改键）。

**下拉选项集**：`GET /config/value-options` 合并返回两张表 —— `VALUE_OPTIONS` **23 个单键** + `SEGMENT_OPTIONS` **1 个多段键**（`industry.batch.barcodeRule` 的 3 段各自取值域）= 共 24 个键。

**前端渲染**：新增 `value_type = 'group'`（分组行无控件）与 `parent_key` 递归渲染（`views/set/sys-params/ParamNodeRow.vue`）。

**`locked` 折算**（读取时实时算，**不落库**）：真实引用链路 = `erp_product.is_batch_expiry_managed`（商品表单「保质期/批次号」勾选框）→ 锁 `industry.batch.shelfLife`（devdb 实测 1 个商品勾选 → `locked = 1` 行）；`is_batch_managed`/`is_serial_managed` → 批号管理/序列号管理；`erp_product_attribute_def` 有行 → 商品规格属性。前端禁用 + 悬浮说明；批量保存整批只查一次并跳过锁定项。

### 11.3 财务期初：会计年 / 会计期间联动 / 存货对平（§10.5-7/-8 关闭，**未新增迁移**）

- **年度取会计年**：新增 `GET /api/erp/finance/initial/current-year`，回退链 = `status=1` 且今天落在 `start..end` 的年度（`OPEN_PERIOD`）→ 该租户最大 `period_year`（`MAX_PERIOD_YEAR`）→ 系统年 + hint。devdb 实测返回 `{periodYear:2026, source:"OPEN_PERIOD"}`。
- **会计期间联动**：新增 `GET /api/erp/finance/initial/period-status` —— **该年度存在任一开启期间即允许**（全部关闭才禁止；两篇文档均无裁定 → 宽松口径，已写入注释与文档）；后端 `assertPeriodEditable` 拦新增/修改/删除（400 + 统一文案），前端置灰并给原因。
- **与库存期初对平**：并入 `inventoryCheck` —— 财务侧 = `subject_code` 前缀 `140`（沿用 `FinancialReportServiceImpl.ASSET_LINE_DEFS` 既有口径）；库存侧 = `SUM(quantity * unit_price) FROM erp_stock WHERE deleted = 0 AND is_initial = 1`（新增 `initial/mapper/InitialStockOpeningMapper.java`，租户条件与库存期初页用同一个 `getCurrentTenantIdValue()`）。**只检查提示、不改写数据、不阻断保存**。
- **应收/应付数据源订正**：实测 `erp_partner` **0 行**、`biz_party` **152 行**；`MdCustomerController` / `SystemRebuildService` 均以 `biz_party` 为现役 → **只订正代码注释与文档口径，不改数据源**。

### 11.4 企业信息：拆表 + LOGO（§10.5-9/-10/-11 关闭，`V11.420.0`）

- `sys_tenant` **30 → 15 列**；新建 1:1 子表 `sys_tenant_profile`（**24 列** = 15 个档案列逐字沿用原名 + `logo_url` + 8 个审计/隔离列），已 devdb 应用（列数 15/24、行数 3/3）。
- 数据保全：`INSERT ... SELECT`（含 `tenant_id`）→ 删列前 `DO` 块**强制断言「每租户都有子表行」**，不满足即 `RAISE EXCEPTION`；迁移头含**完整回滚脚本**。
- 新增 `SysTenantProfile` + mapper + `TenantProfileService`（同事务写两张表，逐列 `set` 保「清空」语义）。**`GET/PUT /api/tenant/current` 接口契约与响应字段名保持不变**（改为读写子表，响应 `CompanyProfileVO`）。
- **LOGO**：`logo_url` 落子表；页面 Tab① 新增「企业标识」分区（上传/重新上传/预览/移除，限图片且 ≤2MB），复用 `POST /api/file/upload`，**未改任何共享组件**；移除提交空串 → 后端归一为 NULL。
- **纳税人识别号**：裁定保留 `tax_number`（`biz_party`/`erp_supplier`/`crm_customer`/`sys_tenant_profile` 全库统一词根）。

### 11.5 应用中心：短信用量接入真实来源（§10.5-15 部分闭环）

- **短信**：真实来源 `mkt_sms_setting`（`quota_total`/`quota_used`）+ `mkt_sms_record`（本月发送行数）；`SetAppCenterController#smsUsage` 用 `JdbcTemplate` **显式带租户条件**（JdbcTemplate 不走租户拦截器）接上，含**表缺失降级**（`available=false` + reason，不让概览 500）。
- **物流查询**：全库无计量表、无轨迹查询实现 → **仍「未接入」**，但 `logisticsQueryUsage()` 返回的 reason 已说清缺什么（列出真实存在的物流表，说明它们不记录查询动作）。
- **到期日期**：`sys_tenant.expire_time` 三租户**全 NULL** → 保持「长期有效」，**不用 `create_time` + 套餐时长推算，不造假到期日**。

### 11.6 验收与取证资产（二轮）

| 项 | 内容 |
|---|---|
| 模块级 E2E | `tools/e2e-settings.cjs` **208 项全过**（0 失败 0 跳过）；第一轮 164 → 二轮 201 → 三轮 208 |
| 新增验证节 | **「5b 未闭环项收口验证」33 条断言**；**「7d 共享组件改动覆盖面巡查」**（`cell-dblclick` 组件级改动后跨页无回归） |
| 运行证据 | `tool-results/e2e-settings/`（201 与 208 两轮运行日志 + 16 张页面截图） |
| 对标取证资产（**新增**） | `tools/ql361-settings-probe.cjs`（可复跑）+ `tool-results/ql361/设置-deep/`（**81 个 JSON + `shots/` 下 53 张 PNG + `_summary.md` 1314 行**） |
| 迁移 | `V11.416.0` / `V11.417.0` / `V11.420.0` / `V11.421.0`（均已应用到 devdb，见 §10.6） |

> **取证覆盖面**：本轮是设置模块**第一次**把①系统参数 8 个标签的全部卡片/控件、②审核设置 16 类单据的配置弹窗、③打印设置的 3 个下拉完整选项 —— 三者同时采全。`_summary.md` 里未取到的项（如消息提醒中归属不明的勾选框、收件人演示文本）在 `V11.421.0` 中**逐条登记为「不落库/未取到」**，不补造。

### 11.7 二轮仍未闭环（**汇总，禁止当作已完成**）

以 §10.5 处置表 ⚠️ 行为准（**该表已按二轮实际结果重写**）：

| 仍未闭环 | 为什么不能"解决" |
|---|---|
| 打印设置 6 个模板类目无真实载体 | `sys_print_template` 无类目列且 0 行；5 个 printing 页在 `sys_menu` 无菜单项 → 跳转必 404。**造入口 = 造假入口** |
| 打印设置「属性商品汇总 / 批次效期汇总」未接 | `FormatEngineImpl.renderTable` 逐行直出，**引擎没有汇总行能力**，页面已如实标注 |
| 审核设置：16 类单据只 3 类有适用条件证据、弹窗无截图 | 本轮已抓 16/16 弹窗（`_summary.md` §二），但**各单据"适用哪些条件"的裁剪规则**仍未取到 → 保持 7 类条件对 16 类统一可选 |
| 系统重建 `biz_party_role` 未清 | 该表**不挂 `biz_party`**，无法限租户 → 宁可不删，不越权 |
| 系统重建未建异步任务台账 | 当前为同步单事务（大库可能超时）；建台账属新能力，需先裁定 |
| 系统任务历史 15 行创建人仍 `-` | 该列本轮才建、姓名列从无写入来源，**无依据可回填** |
| 应用中心物流查询用量未接入 | 全库无计量表、无轨迹查询实现 → 只把文案改到说清缺什么 |
| 应用中心 `expire_time` 全 NULL | 无套餐时长来源可推 → 保持「长期有效」，**不编造到期日** |
| 我的待办/已办「高优先级」卡恒 0 | `workflow_task.priority` 无任何业务来源（可空列，如实回传 NULL）→ **编一个优先级规则就是发明业务** |
| 我的待办/已办「退回」无法定位目标节点时降级为驳回 | 目标节点解析失败时的兜底路径，原因写进审批意见 |
| 企业信息 Tab②「公司名称」只读 / 无变更审计 / `level`·套餐未暴露 | 产品未裁定；审计需新表 |
| 流程分析页 `analysis/*` 忽略入参 `tenantId` | 工作流四表整体豁免多租户插件，页面为全局口径；改动有清空现有数据的风险 |

### 11.8 「双击行」入口：**页面侧自行实现**，共享表格组件事件契约保持原状

> **本节经过一次方案返工，最终口径以本节为准**（下面的"为什么改方案"是最重要的部分）。

#### 最终实现（2026-09-19）

为满足「流程实例双击行打开详情」，**没有**在共享表格组件上增加全局事件派发，而是：

| 落点 | 内容 |
|---|---|
| **共享组件（`BillTableList` / `BillDetailTable`）** | **事件契约完全保持原状** —— `defineEmits` 里没有 `cell-dblclick`，组件自身也没有任何 `@dblclick` 监听 |
| **`BillDetailTable` 的 `<tr>`** | 新增**惰性属性 `data-row-key`**（值 = `record[rowKey] ?? record.id`，与它自身 `:key` 同口径）。与既有的 `<td data-col-key>` 同一范式，**纯属性、零行为影响**；占位空行不带该属性 → 天然不会被命中 |
| **新增页面侧工具** | `frontend/apps/pc-admin/src/composables/useRowDblclick.ts` —— 事件委托实现：从事件目标反查 `tr[data-row-key]` → 用 `rows()` 建索引还原记录 → 回调页面；内置**交互控件白名单**（button/a/input/select/ant-select/ant-picker/.ss-checkbox 上的双击一律忽略，避免与既有交互抢事件） |
| **各页面** | 谁要双击谁在页面侧自己接：容器加 `ref="tableWrap"` + `useRowDblclick(tableWrap, () => rows.value, handleView, '<rowKey>')` |

#### 为什么改方案（重要，勿回退）

最初的做法是**在组件上透出 `cell-dblclick`**。核查后发现两个事实，促使返工：

1. **这批绑定本来就是坏的**：39 个页面早就写了 `@cell-dblclick="handleView"`，而组件**从未派发**该事件 → 它们**一直是死绑定，双击从来没有过任何反应**。所以这不是"新增能力"，而是"要不要一次性改变 211 处调用方的行为"。
2. **爆炸半径全站**：`<BillTableList>` 被引用 **211 处 / 170 个视图文件**。在组件上补派发 = 一次改动让 39 个页面的行为同时从"无反应"变成"打开详情/进编辑"，且**无法逐页评估与签署**。

裁定：**共享表格组件的事件契约保持不变，双击入口下沉到页面侧**；同一段委托逻辑用 composable 收敛，避免复制 39 份。

> 反对的替代方案：① "把 39 个页面的绑定删掉了事" —— 属于删功能（"双击行打开详情"是明文规格）；
> ② "加个 prop 才生效" —— 调用方已按正确契约写好，加开关等于让缺陷继续存在、只是换个名字（且属向后兼容垫片）。

#### 39 处死绑定的实际处置

| 处置 | 数量 | 说明 |
|---|---|---|
| **改造为页面侧实现** | **38** | 删掉 `<BillTableList>` 上的 `@cell-dblclick`，改为容器 ref + `useRowDblclick`；处理函数体一律未改（签名本就是 `(record)`，正好匹配） |
| **保留原样** | **1** | `erp/batch/index.vue` 的 `@cell-dblclick="({ row }) => handleDetail(row)"` 绑的是 **`vxe-table`**（非本次涉及的两个组件）。已查证 `vxe-table@4.19.10`：`es/table/src/emits.js:31` 声明该事件、`table.js` 真实 dispatch、载荷是对象 `{row,...}` → **该绑定本来就是好的**，保留才对 |
| 误报 | 1 | `workflow/task-management.vue` 的 grep 命中其实是**模板注释**（原绑定已被上游删除）；按其真实处理函数 `handleViewDetail` 迁移为页面侧接入 |

#### 顺带修复的真缺陷

- **`crm/supplier/index.vue` 模板被伪注释破坏**：一段 `<!-- ... --` 把 `@add` / `@refresh` / `@search` / `@export` 四个属性吞成了注释文本，产物里出现非法属性名 `"<!--"`（若被 `setAttribute` 会抛 `InvalidCharacterError`）→ **该页工具栏按钮此前点了没反应**。已还原为规范属性。

#### 真机验证（本轮补做的，也是本次返工的触发点）

`tools/e2e-settings.cjs` 新增 **7d 节**，把它固化为永久断言（不再是"只断言渲染无 error"）：

- 惰性 `data-row-key` 只挂在真实数据行：**数据行 12 / 空行带 key 0**；
- **双击流程实例行 → 打开详情抽屉，且详情内容含该行 `row-key`**（证明是"开着这一行"，不是随便开了个东西）；
- 双击行内按钮 → 新增 console error **0 条**（控件白名单生效）；
- 改造后其它可达页回归：`/system/config/index`、`/system/dict/index`、`/md/supplier/index`、`/sales/order-center` 全部 `errors=0`。

验收：`tools/e2e-settings.cjs` **208/208 全过**（上一轮 201）。

#### 仍未闭环（如实登记）

- **4 个页面的 `handleView` 是空函数体**（`system/dict`、`system/position`、`system/role`、`system/user` 的 `handleView = (record:any) => {}`）→ 双击接上了，但**仍无动作**。属预存桩，需业务先定义"看什么详情"，**本轮不代拟**。
- `views/purchase/exchange/index.vue.bak` 是历史备份文件（含同样的旧绑定），非 `.vue`、不参与构建，未删。
- `views/sales/outbound/index.vue` 存在且 `componentMap` 有映射，但 `sys_menu` 只有 `sales/outbound/form` → `/sales/outbound/index` **无路由可达**（同「流程分析」症的孤儿页）。已在 `tools/e2e-billdetailtable-regression.cjs` 的失败项里暴露，建议销售模块侧按同一口径处置。
- `tools/e2e-billdetailtable-regression.cjs` **15/16**（唯一失败项就是上面那条 `/sales/outbound/index` 期望路径过期，与本改动无关）。
