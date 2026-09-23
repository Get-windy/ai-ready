# 仓储模块全面审计报告

> **审计日期：** 2026-09-23
> **审计范围：** 前端 `views/{erp/stock-*, wh, wms, quality}`、后端 `erp/erp-stock` + `wms`、数据库仓储相关 55 张表、`sys_menu` 仓储域菜单树与权限配置
> **对标依据：** `docs/Yh-Spec/手动整理对标开发文档/仓储模块/`（24 篇）+ `系统菜单设计与管理/`（5 篇）
> **审计维度：** 可用性 / 规范性 / 冗余 / 死代码 / 功能接通 / 跨模块关系 / 权限合规

---

## 0. 审计方法与可复现脚本

全部结论均来自可复现的只读脚本或直连 devdb 的 SQL，脚本已落在 `tools/`：

| 脚本 | 作用 | 产物 |
|------|------|------|
| `tools/audit-storage-frontend.py` | 仓储 61 个前端页面 × 路由引用 × 菜单引用 × 源码引用 | `tool-results/storage-frontend-audit.json` |
| `tools/audit-storage-menu.py` | 模拟 `dynamicRoutes.getComponent` 解析规则，校验仓储菜单可达性 | 控制台 |
| `tools/audit-storage-backend.py` | `erp-stock` + `wms` 控制器/端点/鉴权注解/权限码提取 | `tool-results/storage-backend-audit.json` |
| `tools/audit-storage-api-wiring.py` | 前端 `src/api/**` 调用 ↔ 后端端点双向比对 | `tool-results/storage-api-wiring.json` |
| `tools/audit-storage-deadcode.py` | 后端非容器管理类的引用计数（已排除 Spring Bean 误报） | `tool-results/storage-deadcode.json` |
| `tools/check-menu-targets.py`（既有） | 全站菜单组件存在性 | — |
| `tools/audit-permission-codes.py`（既有） | 权限码 注解 ↔ 种子 对账 | `tools/audit-permission-codes.json` |

**规模基线：** 前端页面 61 个；后端 `erp-stock` 361 + `wms` 116 个 Java 文件；控制器 65 个 / 端点 553 个（仓储域路径 368 个）；仓储菜单 34 条（8 个目录 + 26 个叶子）；仓储相关表 55 张。

---

## 1. 结论摘要

| # | 级别 | 问题 | 影响 |
|---|------|------|------|
| P0-1 | 阻断 | 仓储 203 个权限码**只授予超级管理员**，非超管角色零授权 | 非超管用户能看见菜单但所有接口 403（详见 §4.1） |
| P0-2 | 阻断 | 发货单「取消发货」后端接口不存在 | 活页面按钮必然 404（§3.1） |
| P0-3 | 阻断 | WMS→ERP 的 HTTP 内部调用**全部必然 401** | 事件回传永远发不出；**收货确认直接回滚、功能不可用**（§7.1） |
| P0-4 | 严重 | ERP 轨直写 `erp_stock`，绕过 WMS 唯一写入口 | 两轨库存必然漂移（§7.2） |
| ~~P0-5~~ | ~~严重~~ → **P2** | **仓库主数据双源**（已更正定性）：两表 `deleted=0` 各 3 行、当前**一致**；缺的是同步机制 | 新建仓库不会进下拉（§7.5 已更正） |
| P1-1 | 高 | 同一「盘点」存在**三套实现**，两套无消费方 | 维护面 ×3、口径不一致（§6.1） |
| P1-2 | 高 | 7 个**无路由/无入口的孤岛页面**（2700+ 行） | 用户不可达的"活代码"（§6.2） |
| P1-3 | 中 | 前端 3 个死 API 对象 + 若干死方法指向不存在的端点 | 误用即 404（§6.3） |
| P1-4 | 中 | 后端 12 个文件/类全无引用（含 3 个 AI 服务） | 死代码（§6.4） |
| P1-5 | 中 | 成本调价单记账**不回写库存成本** | 调价后出库成本/毛利仍按旧成本算（§9.4-2） |
| P2-1 | 中 | 31 张表列数超开发规范（明细≤35 / 主表≤25） | 规范偏离（§5.1） |
| P2-2 | 低 | 菜单 component 格式不统一、残留 list_path、sort 并列 | 规范性问题（§2.2） |
| P2-3 | 低 | 菜单文档与库不一致 3 处（以库为准，需回写文档） | 文档失真（§2.3） |
| P2-4 | 低 | `erp-stock` 模块混装商品/商城/营销/期初库存控制器 | 模块边界不清（§8.1） |
| P2-5 | 低 | `any` 类型泛滥（wh 489 / quality 110 / stocktake 81 处） | 规范偏离（§8.2） |

**总体判断：** 仓储模块的**代码本身完成度高**（菜单连通性 0 缺陷、鉴权注解覆盖率 553 端点中仅 10 个裸端点、权限码 100% 在种子库、无桩代码残留），但存在**「权限只授超管」「两个取消功能从未实现」「事件回传必然 401」「库存双写口径被销售模块绕过」**四类实质缺陷，以及**三套盘点、多个孤岛页面、批量死代码**的收尾残留。

---

## 2. 菜单与路由连通性

### 2.1 连通性校验结论：0 缺陷

按前端 `getComponent()` 的真实解析规则（`dynamicRoutes.ts:904-931`：去 `views/` 前缀与 `.vue` 后缀 → 精确命中 → 追加 `/index` → 去 `/index` → 磁盘兜底）逐条模拟：

- 26 个叶子菜单的 `component` **全部**解析到磁盘真实存在的 `.vue`：**0 条坏菜单**
- 双入口菜单的 `list_path` **全部**可解析：**0 条打不开的标签**
- 菜单树与《仓储模块菜单变更》§七 的树形结构**逐节点一致**；文档判定的删除项（80017 盘点作业单、5012 补货管理、60306–60310 五个作业列组）经查 `sys_menu` **已物理删除且无同名残留**

### 2.2 规范性瑕疵

| 问题 | 证据 | 建议 |
|------|------|------|
| `component` 字段两种风格混用 | 一类 `erp/stock-in/form`（无前缀无后缀），一类 `views/erp/alert-query/index.vue`（完整路径）。涉及 5017、80010、80011、80018、90203、81005、81012、81013、80012–80016 | 统一为 `模块/页面/form` 风格 |
| 5013「预警设置」残留 `list_path` | `display_mode=0` 却仍保留 `list_path=erp/stock-alert-config/index` | 清空（无副作用） |
| 80012–80016 五单 `sort` 全为 1 | 同目录并列排序，展示顺序不稳定 | 改为 1..5 |
| 双入口 `path` 与 `component` 语义分离 | 如 80012：`path=wms/receive/form`、`component=views/wh/receiving-order/form/index.vue`（依赖 `componentMap` 别名 `'wms/receive/form'` 桥接） | 属刻意设计（路由别名→`wh/*`），保留；但别名链是隐性依赖，删键即断 |

### 2.3 文档与库不一致（3 处，均以库为准）

| 菜单 | 文档《41条》记载 | 数据库实际 | 判定 |
|------|-----------------|-----------|------|
| 5014 生产模板 | `wh/production-template/form` ⧉ `erp/stock-bom/index` | `erp/stock-bom/form` ⧉ `erp/stock-bom/index` | 库为准（无 `wh/production-template` 页面），**文档待回写** |
| 80010 借进单 | `wms/borrow-in/form` | `wh/borrow-in/form` | 库为准（`componentMap` 仅注册 `wh/borrow-in/form`） |
| 80011 借出单 | `wms/borrow-out/form` | `wh/borrow-out/form` | 同上 |

---

## 3. 前后端功能接通

### 3.1 真实功能断点（P0）

**① 发货单「取消发货」** — 后端从未实现

```
前端定义  api/wms/ship.ts:82-84         POST /wms/ship/cancel
前端调用  views/wh/shipping-order/form/index.vue:636  shipApi.cancelShip(formData.id, 'PC端取消发货')
后端      ShipController(/api/wms/ship) 仅 12 个端点：task/save、task/update、task/{id}、task/page、
          task/query、task/page-detail、task/{id}(DELETE)、start、scan、confirm、details/{shipId}、detail/save
          —— 无 /cancel；ShipService 亦无 cancel 方法
权限码    wms:ship:cancel 不在 sys_permission（库中只有 borrow/move/pick/putaway/receipt 五个 :cancel）
```

**对照证据**：同域其它五单都有取消能力 —— `ReceiptController:137` `wms:receipt:cancel`、`MoveController:128` `wms:move:cancel`，`wms:pick:cancel`、`wms:putaway:cancel`、`wms:borrow:cancel` 均在库。**结论：发货单取消是遗漏，不是刻意裁剪。**

> 修法建议（待拍板）：`ShipController` 补 `@PostMapping("/cancel")` + `wms:ship:cancel` 权限码，服务层参考 `ReceiptController.cancel`（校验状态 → 置取消 → 回滚已占用库存）实现。

**② 盘点作业单「取消盘点」** — 同类缺口

```
前端定义  api/wms/check.ts:81-83        POST /wms/check/cancel
前端调用  views/wh/inventory-order/form/index.vue:343  checkApi.cancelCheck(form.id, 'PC端取消盘点')
后端      CheckController(/api/wms/check) 10 个端点中无 /cancel；CheckService 无 cancel 方法
```

> 该页本身是孤岛（见 §6.2），修法与「这套页面是否保留」的决策绑定。

### 3.2 失效调用（仅存在于死代码中，不会被用户触发）

以下 18 条"前端调了、后端没有"的调用，逐条回溯到导出对象后确认**全部位于无人引用的死 API 对象中**（详见 §6.3），因此不构成线上故障，但属于**误用即 404 的陷阱**：

- `/erp/stocktake/{page,{id},submit,approve}`（`stocktakeOrderApi`）—— 后端真实路径是 `/erp/stock/take/*`
- `/erp/stock/check/{page,{id},items,start,complete,submit,cancel}`（`stockCheckApi`）
- `/erp/stock-alert-config/export`（`stockAlertConfigApi`）
- `DELETE /erp/stock/assemble/{id}`、`DELETE /erp/stock/split/{id}` —— 后端两个控制器**确实没有** DELETE 端点（其它五单都有）
- `GET /erp/stock/{assemble,bom,cost-adjust,split}/export` —— 后端只有 `StockCheckController:152` 有 `export`

### 3.3 后端有端点、前端无消费方（67 个，分类）

| 类别 | 数量 | 判定 |
|------|------|------|
| `/v1/warehouse/**`（PDA 端） | 38 | **正常** —— 由 `apps/pda-warehouse` 消费，本次只扫了 pc-admin |
| `/api/wms/erp/**`、`/api/erp/wms/**`（ERP↔WMS 集成） | 8 | **死接口**，见 §7.1 |
| `/erp/stock/check/**`（盘点作业） | 6 | **死接口**，见 §6.1 |
| `/erp/stock/{increase,decrease,freeze,unfreeze}` | 4 | 无前端消费；`decrease/unfreeze/freeze` 被销售模块内部调用（见 §7.2） |
| `/erp/stock/{by-product/{id},analytics/page,inventory/reconcile,init-from-erp}` 等 | 11 | 部分无消费方，建议逐条复核 |

---

## 4. 权限配置

### 4.1 P0：仓储权限只授超级管理员

```sql
-- 角色 × 仓储权限码 授权数
SELECT rp.role_id, count(*) FROM sys_role_permission rp
JOIN sys_permission p ON p.id=rp.permission_id
WHERE p.permission_code LIKE 'stock:%' OR p.permission_code LIKE 'wms:%'
GROUP BY rp.role_id;
-- 结果：只有 role_id=1（SUPER_ADMIN）→ 203 条；其余角色 0 条

-- 角色 × 仓储菜单 授权数
SELECT r.id, r.role_name, count(rm.menu_id) FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id=r.id AND rm.menu_id IN (仓储菜单 34 个 id)
GROUP BY r.id;
-- 结果：全部为 0（含超管）
```

**用户侧分布**：46 个用户中 **43 个是超级管理员**，非超管仅 3 个（系统管理员 18 个权限码 / 部门管理员 16 个 / E2E租户2管理员 4 个），**三者均不含任何 `stock:*` 或 `wms:*`**。

**传导链**：
1. `UnifiedPermissionCacheService.getPermissions():84` —— 超管角色返回 `["*"]`（通配），所以超管不受影响；
2. 非超管走 `userService.getUserPermissionCodes()` → 无仓储权限码 → `@SaCheckPermission("stock:in:list")` 等**一律 403**；
3. 菜单侧：`MenuPermissionDeriver`（2026-09-22 拍板）把菜单可见性改为从 `sys_permission` 派生，规则是 `menu_code` 等于权限码或为其 `:` 前缀。仓储菜单码是 `erp:stock-in` / `wms:receipt` 体系，而权限码是 `stock:in:*` / `wms:receipt:*` 体系，**两套命名根本对不上** → 菜单码不在派生集合中 → 按过渡口径 **fail-open 保持可见**；
4. **净效果：非超管用户看得见仓储菜单，点进去每个接口都 403。**

**根因（两条，都要处理）**：
- 授权数据缺失：`sys_role_permission` 从未给业务角色授过仓储权限（与 `PURCHASE_AUDIT_REPORT_20260922.md` 记录的采购模块问题同源）；
- **命名体系断裂**：菜单码（`erp:stock-in`、`erp:stocktake`、`erp:alert-query`、`mega:*`）与权限码（`stock:in:*`、`stock:take:*`、`stock:alert:*`）不同源，导致菜单派生规则对仓储域**整体失效**（既判不出"该用户有没有这个菜单"，也判不出"库里有没有覆盖"）。

> **结论：仓储模块目前在权限上是"单角色可用"的。** 任何非超管岗位（仓管员、库管主管）都无法使用，与行业标准（角色-权限矩阵 + 最小权限）差距最大的一环。

### 4.2 接口鉴权覆盖率（良好）

`erp-stock` + `wms` 共 65 个控制器 / 553 个端点，**无任何鉴权注解的仅 10 个（1.8%）**：

| 端点 | 类 | 判定 |
|------|----|------|
| `POST /api/erp/wms/{receipt-complete,ship-complete,inventory-change,check-diff}` | `ErpCallbackController` | 见 §7.1（需登录 + 同进程自调） |
| `POST /api/wms/erp/{purchase-order,sale-order,product-sync,check-command}` | `ErpIntegrationController` | 同上，且无消费方 |
| `POST /api/v1/warehouse/auth/login` | `PdaAuthController` | **正常**（已在 `SaTokenConfig` 白名单，登录接口不放行即死锁） |
| `POST /api/v1/warehouse/auth/logout` | `PdaAuthController` | 正常（登出需持 token） |

- 权限码对账（全仓）：代码引用 1720 个，库中 1835 个，**仓储域零缺失**；全仓仅 2 个缺失且均不在仓储域；
- 权限码分组按业务域（`stock:*`、`wms:*`、`erp:stock-*`）划分，粒度到 `:list/:create/:update/:delete/:submit/:approve/:execute/:cancel/:export`，**粒度符合行业标准**。

### 4.3 行级数据权限

按既有记录（`permission-layering-gap-audit`），`DataPermissionInterceptor` 已挂入插件链、`@DataScope` 与表级自动模式生效；本轮未发现仓储域新增缺口。

---

## 5. 数据库

### 5.1 列数超规范（P2，31 张）

规范（`docs` 既有《开发技术规范》）：**主表 ≤ 25 列，明细表 ≤ 35 列**。超出者：

| 超 35 列（明细表违规） | 列数 | | 26–35 列（主表违规） | 列数 |
|---|---|---|---|---|
| `erp_stock_transfer_item` | 64 | | `erp_stock` | 29 |
| `wms_borrow_order_item` | 63 | | `erp_stock_alert_config` | 29 |
| `erp_stock_assemble` | 54 | | `erp_stock_bom` | 26 |
| `erp_stock_split` | 52 | | `erp_stock_bom_item` | 29 |
| `erp_stock_in_item` / `erp_stock_out_item` / `erp_stock_split_item` | 51 | | `erp_stock_replenishment` | 28 |
| `erp_stock_assemble_item` | 50 | | `wms_inventory` / `wms_inventory_log` / `wms_location` | 28–29 |
| `erp_stock_take_item` | 44 | | `quality_inspection` | 30 |
| `erp_stock_damage_item` / `erp_stock_overflow_item` | 43 | | | |
| `erp_stock_in` / `erp_stock_out` | 42 | | | |
| `erp_stock_damage` / `erp_stock_overflow` / `erp_stock_take` / `erp_stock_transfer` / `wms_borrow_order` | 41–43 | | | |
| `erp_stock_cost_adjust` | 39 | | | |
| `erp_stock_check` / `erp_stock_check_item` / `erp_stock_cost_adjust_item` | 32–34 | | | |

判定说明：单据主表/明细表字段多是业务自然结果，但 **>`erp_stock_transfer_item` 64 列** 这一档已明显超出"单据明细"的合理范围，且规范里明确"跨模块数据必须拆分子表（物流、支付、会员、审核等）"。属**存量宽表**，建议登记为技术债逐步拆，不建议本轮动。

### 5.2 表冗余与双轨

| 主题 | 并存实现 | 行数 | 判定 |
|------|---------|------|------|
| 库存 | `erp_stock`（仓库级汇总，"ERP 轨"） | 4 | **刻意双轨**，WMS 过账时镜像；但被销售模块绕过（§7.2） |
| 库存 | `wms_inventory`（库位级明细，"WMS 轨"） | 3 | 同上 |
| 仓库主数据 | `erp_warehouse`（18 列） | 105 | **活**：`/erp/warehouse/*` 的消费方是「仓库规划」页 |
| 仓库主数据 | `wms_warehouse`（18 列） | 3 | **活但数据缺项**：`/wms/warehouse/list-all` 读它，被 `api/options.ts:getWarehouses()` 全站消费（§7.5）。注意：WMS 侧 `WarehouseService` 确有多处消费者，**不是死表**，问题在两表数据不同步 |
| 货位 | `wms_location`（61 行） | 61 | **活**（`warehousePlan.ts` 头注释明确"沿用 WMS 货位表，不另建"） |
| 货位 | `erp_product_location`（推荐货位绑定） | — | 活，语义不同 |
| 盘点 | `erp_stock_take` / `erp_stock_check` / `wms_check_task` | 0 / 0 / 0 | **三套并存**（§6.1） |
| 库存余额日志 | `erp_balance_log`（实体 `BalanceLog` + `BalanceLogMapper` 全无引用） | — | **无消费方**（§6.4） |

### 5.3 数据现状提示

仓储执行类表 `wms_receipt_task/putaway/pick/ship/move/check` 与全部 `erp_stock_*` 单据表 **live=0**（devdb 中无业务数据）。这说明：**WMS 五单与仓储单据链路在当前环境从未被真实跑通过**——本轮所有"接线正确性"结论均为静态分析结论，未获运行时验证。收尾阶段建议至少制造一轮真实单据，验证库存过账与事件回传。

---

## 6. 冗余与死代码

### 6.1 P1：同一「盘点」三套实现

| # | 前端页面 | 后端 | 表 | 消费方 | 判定 |
|---|---------|------|----|--------|------|
| 1 | `views/erp/stocktake/{index,form}.vue`（菜单 5003「盘点单」，双入口） | `StockTakeController` `/api/erp/stock/take`（11 端点） | `erp_stock_take` / `_item` | **在用**（页面调 `stockTakeApi`） | **保留** |
| 2 | 无页面（前端 `stockCheckApi` 已死） | `StockCheckController` `/api/erp/stock/check`（16 端点）+ `StockCheckService(Impl)` | `erp_stock_check` / `_item` | **仅单元测试**（`StockCheckServiceImplTest`） | **死链路** |
| 3 | `views/wh/inventory-order/{index,form}.vue`（无菜单）+ `views/wms/check/form.vue` | `CheckController` `/api/wms/check`（10 端点） | `wms_check_task` / `wms_check_result` | 仅上述孤岛页互跳 | **孤岛 + 与 1 功能重复** |

**文档裁定证据**：《仓储模块菜单变更》§4.5 已明确 80017「盘点作业单」**因与 5003 盘点单功能重复而硬删除**。菜单删了，但页面（`wh/inventory-order` 597 行）、路由、后端 `CheckController`、表全部保留。

### 6.2 P1：无菜单入口的孤岛页面（9 个）

判定口径：能被 `dynamicRoutes.ts` 解析（非纯孤儿）**但** ① 无任何 `sys_menu` 引用、② 无任何页面 `router.push`/`<router-link>` 指向 —— 即**用户从 UI 上无法到达**。

| 页面 | 行数 | 路由 | 说明 |
|------|------|------|------|
| `views/wh/inventory-order/index.vue` | 103 | **无** | 盘点作业单列表；`router.push('/wms/check/form')`（目标同样无路由） |
| `views/wh/inventory-order/form/index.vue` | 494 | **无** | 盘点作业单表单 |
| `views/wms/check/form.vue` | 169 | **无** | `redirectPath='/wms/check'`（该路由不存在），与上一行构成**互相指的闭环孤岛** |
| `views/wms/receipt/form.vue` | 157 | **无** | `redirectPath='/wms/receipt'`；`componentMap` 注释称"被菜单 list_path 使用"，但菜单早已改指 `wh/receiving-order` |
| `views/wms/wave/form.vue` | 132 | **无** | `redirectPath='/wms/wave'`（实际菜单注册的路由是 `/wms/wave/index`） |
| `views/wms/warehouse/index.vue` | 495 | **无** | WMS 侧仓库管理页（维护 `wms_warehouse` 的 WMS 专属属性）；后端 `/api/wms/warehouse/*` 仍被全站仓库下拉复用（§7.5），**页面无入口但接口有用** |
| `views/erp/stock/replenishment/index.vue` | 1191 | 有 | 有路由、无菜单、无任何页内跳转 |

> 合计 **2741 行**。判定依据：`componentMap` 只提供组件映射、**不产生路由**；上述 6 个 `wms/*` 页面在 `dynamicRoutes.ts` 中既无 `path:` 静态注册、也无 `sys_menu` 行，连路由都不存在。
>
> **已排除的疑似项（经复核有入口，非孤岛）**：`views/erp/stock/index.vue`（ERP 仪表盘 81015 跳 `/erp/stock`）、`views/erp/serial/{index,detail}.vue`（批次管理页 `erp/batch/index.vue:1472` 跳 `/erp/serial`）；`views/erp/column-config/*` 被 `sales/*/form.vue` 以组件方式正常复用。
>
> 但 `views/erp/stock/index.vue` 仍有一处缺陷：静态路由 `path:'stock'`（`dynamicRoutes.ts:1555`）的 `meta.title` 写成 **「销售出库」**，组件却是库存页 —— 复制粘贴残留。

### 6.3 前端死 API 对象（**已清理**）

`frontend/apps/pc-admin/src/api/erp.ts` 中四个导出对象**零引用**，已删除：

| 对象 | 说明 | 状态 |
|------|------|------|
| `stockCheckApi`（+`StockCheck`/`StockCheckItem`） | 对应已死的 `StockCheckController` | ✅ 已删 |
| `stocktakeOrderApi`（+`StocktakeOrder`） | 路径 `/erp/stocktake/*` 与后端 `/erp/stock/take` 不一致（双重失效） | ✅ 已删 |
| `stockAlertConfigApi` | 预警设置页实际用 `request` 直调，未走该对象 | ✅ 已删 |
| `warehouseStockInApi`（+`WarehouseStockInOrder`） | **与在用的 `stockInApi` 指向完全相同的 `/erp/stock/in/*` 端点** —— 同功能的第二份定义 | ✅ 已删 |

**死方法**（对象活着、方法从无调用且后端无这类端点）已删除：`stockAssembleApi.delete/export`、`stockSplitApi.delete/export`、`stockBomApi.export`、`stockCostAdjustApi.export`（`stockBomApi.delete` 与 `stockCostAdjustApi.delete` **保留** —— 后端有端点且页面在调用）。

> 另有 2 个**非仓储域**的零引用对象仍在：`saleStatsApi`（`/erp/sale/order/stats`）、`purchaseStatsApi`（`/erp/purchase/order/stats`）—— 属销售/采购域，留给对应模块审计处置。

### 6.4 后端死代码

按"排除 Spring 容器管理类后的引用计数"扫描（184 个非 Bean 类），**4 个类全无引用**：

| 类 | 文件 | 判定 |
|----|------|------|
| `BatchAnomalyDetectionService` | `erp-stock/batchsn/ai/service/` | 死代码 |
| `BatchQualityPredictionService` | 同上 | 死代码 |
| `IntelligentSearchService` | 同上 | 死代码 |
| `BalanceLogMapper`（+ 实体 `BalanceLog`） | `erp-stock/stock/mapper/` | 死代码，表 `erp_balance_log` 无消费方 |

连带：`batchsn/ai/dto/` 下 6 个 DTO 仅被上述三个死服务引用，同属死代码（共 **11 个文件**）。

**另有 8 个文件属"有实现无消费方"**（Controller 是活 Bean，但无人调用其端点）：
`ErpCallbackController`、`ErpIntegrationController`、`ErpCallbackService(Impl)`（见 §7.1）、`StockCheckController` + `StockCheckService(Impl)`（见 §6.1）。

### 6.5 桩代码检查：干净

- 前端仓储 61 个页面：**零** TODO/FIXME/mock/桩/占位（仅 2 处解释性注释提到"占位行"）；
- 后端仓储代码：仅 3 处注释命中，其中 `StockInServiceImpl:268`、`StockOutServiceImpl:268` 的 `TODO-P0 库存收敛` **已过时**（下方即 `publishEvent(InventoryChangeEvent)`，库存收敛已落地），建议更新注释；`PdaAuthController:40` 是修复说明。

---

## 7. 与其他模块的关系

### 7.1 P0：WMS → ERP 的 HTTP 内部调用全部必然 401（两条链路）

**共同根因**：WMS 侧用裸 `java.net.http.HttpClient` 调用 ERP 的 REST 端点，**不带任何 Authorization 头**；而 `SaTokenConfig:84` 的 `SaInterceptor` 对 `/**` 强制 `StpUtil.checkLogin()`，白名单（同文件 33–80 行）只放行了 `/api/v1/warehouse/auth/login`（PDA 登录）等，**没有** `/api/erp/wms/**`、`/api/erp/purchase/**`。同时二者**本就在同一个 JVM 里**（同一 fat jar），HTTP 自调是多余设计。

#### 7.1.1 收货确认回写采购订单 —— **直接阻断功能（最严重）**

```java
// wms/.../ReceiptServiceImpl.java
54:  private static final String ERP_RECEIPT_BACKFILL_URL = "/api/erp/purchase/order/received";
56:  @Value("${wms.erp.base-url:http://localhost:5655}") private String erpBaseUrl;
343: if (task.getSourceOrderId() != null) { callErpReceiveBackfill(task, details); }   // confirmReceipt 内
372: HttpRequest request = HttpRequest.newBuilder()
373:     .uri(URI.create(erpBaseUrl + ERP_RECEIPT_BACKFILL_URL))
374:     .header("Content-Type", "application/json")
375:     .header("X-Trace-Id", "RECEIPT-" + task.getTaskNo())      // ← 没有 Authorization
379: if (response.statusCode() != 200 && response.statusCode() != 201) {
380:     throw new WmsBusinessException("调用ERP收货回写失败: HTTP " + response.statusCode() + ...);
```

**接收端确实存在**（我最初误判为"端点不存在"，复核后更正）：
`PurchaseOrderController`（`@RequestMapping("/api/erp/purchase/order")`，第 38 行）+ `@PostMapping("/received")`（第 51 行）+ `@SaCheckPermission("purchase:order:list")`（第 50 行）→ 需要登录态。

**后果链**：401 → `callErpReceiveBackfill` 抛 `WmsBusinessException` → `confirmReceipt` 标注 `@Transactional(rollbackFor = Exception.class)` → **整个收货确认事务回滚**。即：**凡 `sourceOrderId` 非空（采购订单下推生成）的收货单，「确认收货」100% 失败**；手工新建（sourceOrderId 为空）的收货单不受影响。

补充：该端点用 `@SaCheckPermission("purchase:order:list")` 作鉴权码，语义也不对（用"列表查询"权限守护"写回已收数量"），建议新增 `purchase:order:receive-backfill` 之类的专用码。

#### 7.1.2 事件回传（outbox）—— 静默失败

**链路**：`EventServiceImpl.processPendingEvents()`（`@Scheduled(fixedDelay=10000)`）扫描 `wms_event_outbox`，用同一个裸 `HttpClient` POST 到 `http://localhost:5655` + 端点：

```
RECEIPT_COMPLETED → /api/erp/wms/receipt-complete
SHIP_COMPLETED    → /api/erp/wms/ship-complete
INVENTORY_CHANGE  → /api/erp/wms/inventory-change
CHECK_DIFF        → /api/erp/wms/check-diff
```

其中第 1 条（无 Authorization）同 7.1.1；此外还有：

2. **同一进程内自我 HTTP 调用**：`ErpCallbackController` 就在 `wms` 模块（同一个 fat jar），WMS 通过 HTTP 回调"自己进程里的端点"，属多余的跨进程设计；
3. **端口硬编码风险**：`wms.erp.base-url` 默认 `http://localhost:5655`，与后端实际监听端口不一致时直接连接失败。

**文档早已记录过这个坑**：《移库单开发文档》§5 写明 —— *"盘点联动…**未用 HTTP（`/api/wms` 未在鉴权放行，会 401），改用进程内 Spring 事件解耦**"*。移库→盘点链路因此绕开了问题，但上述**两条** HTTP 调用没有同步改造。

**当前影响**：`wms_event_outbox` live=0，说明尚无事件产生（上游 WMS 单也未跑通），**属"首次真实使用即失败"的潜伏缺陷**。

> 建议：二选一 ——（a）改为进程内 `ApplicationEventPublisher` 直发（推荐，单体架构下最简且同事务）；（b）保留 outbox 但把 `/api/erp/wms/**` 加入白名单并用内部签名头（`X-Internal-Sign` + 时间戳）校验来源。

### 7.2 P0：ERP 轨库存被销售模块直写，绕过唯一写入口

**声明侧（红线）**：`InventoryChangeEvent` 类注释明确 —— *"ERP 侧业务动作（其他出入库/采购入库等）不再直接写 `erp_stock`…库存变动只能走 `InventoryService`（唯一写入口，双写 `wms_inventory` + 镜像 `erp_stock`）"*。

**实现侧（违约）**：

```java
// erp/erp-sales/.../SaleOrderServiceImpl.java
584:  stockService.unfreezeStock(item.getProductId(), warehouseId, item.getQuantity());
585:  stockService.decreaseStock(item.getProductId(), warehouseId, item.getQuantity());
2468: boolean frozen = stockService.freezeStock(...);

// erp/erp-stock/.../StockServiceImpl.java  —— 直写 erp_stock，不经 WMS
 91: public boolean decreaseStock(...) { ... return this.updateById(stock); }
```

**后果**：销售出库只扣 `erp_stock`（ERP 轨），**不产生 `wms_inventory` 变动、不写 `wms_inventory_log`、不触发 WMS 事件** → 两轨余额单向漂移，且 WMS 出货作业看不到已扣减。

> 说明：此项与 `SALES_MODULE_AUDIT_20260922.md` 记录的销售模块 4 个未修 P0（含"库存静默漏扣"）同源，**属已被识别但尚未修复项**，本报告从"仓储库存唯一口径"角度再次确认。

### 7.3 正确接通的跨模块关系（验证通过）

| 关系 | 实现方式 | 结论 |
|------|---------|------|
| 采购入库 → 库存 | 采购入库单审核/执行 → `InventoryChangeEvent` → `InventoryChangeEventListener` → `InventoryService.increase` | 正确 |
| 其他出入库 → 库存 | `StockInServiceImpl:273` / `StockOutServiceImpl:273` `publishEvent(InventoryChangeEvent)` | 正确 |
| 质检 → 库存 | `QualityInspectionServiceImpl:127` 发布 `QualityInspectionCompletedEvent`，"由 wms/erp-stock 监听驱动库存放行/冻结"；`QualityGateListener`（wms）+ `QualityDefectDispositionService`（core-api） | 正确 |
| 盘点 → 移库 | `wms/listener/StocktakeMoveListener` | 存在，未运行时验证 |
| 仓库主数据 | `erp_warehouse` 为全站唯一口径（销售/采购/仓储/财务统一引用） | 正确 |

### 7.4 模块边界问题（P2）

`erp-stock` 模块（361 文件）实际承载了**四个域**：

- 仓储：`erp/stock/{controller,service}` 的 stock-* 全家桶 ✅
- 商品主数据：`Product*` 22 个控制器（品牌/分类/条码/单位/属性/SKU/价格/图片/货位…）
- 商城/营销：`MallTagController`、`GroupBuyController`
- 批次序列号：`batchsn/**`（含 3 个死 AI 服务）
- 期初库存：`InitialStockController`

这与"模块划分按业务域"的规范不符，建议后续拆分（存量债，非本次必修）。

### 7.5 P0：仓库主数据双源 —— 规划的仓库在下拉里选不到

**两条互不相通的链路**：

```
① 维护链路（仓库规划页）   资料 → 仓库管理 → 仓库规划
   api/erp/warehousePlan.ts  →  /erp/warehouse/*  →  erp_warehouse      （105 行，活跃维护）
   该文件头注释自称："仓库主数据：erp_warehouse（全系统唯一口径，销售/采购/仓储/财务统一引用）"

② 消费链路（全站下拉）     api/options.ts:95  getWarehouses() → /wms/warehouse/list-all
   WarehouseController:78 → WarehouseServiceImpl.pageWarehouse() → WmsWarehouseMapper
   →  wms_warehouse                                                      （3 行：WH001/WH002/WH003）
```

**关键代码证据**：`WarehouseServiceImpl` 同时注入了 `WmsWarehouseMapper`（第 31 行）与 `WarehouseMapper`（ERP 侧，第 33 行），但 `pageWarehouse()`（第 53–74 行，即 `list-all` 的实现）用的是 **`warehouseMapper` = `WmsWarehouseMapper`**。

**影响**：在「仓库规划」里新建/停用的仓库，**不会**自动出现在任何业务单据的"仓库"下拉里（销售出库单、库存预警查询、查库存、采购准备、批次查询等 10+ 处页面均消费 `getWarehouses()`）。

> **⚠️ 2026-09-23 更正**：初版报告写的"两表行数差 35 倍（105 : 3）"是**未过滤 `deleted` 造成的误判** —— `erp_warehouse` 全表 105 行里 102 行是 E2E 软删数据，`deleted=0` 的实际只有 3 行，与 `wms_warehouse` 的 3 行**完全一致**（id 也对齐）。
> 因此本条的真实性质是「**缺少同步机制，一旦新建仓库就会分裂**」，而**不是**「当前数据已经不一致」。
> 严重度由 P0 下调为 **P2**（仍应修复，已在 D7 落地）。

> **⚠️ 定性前必须先查既有设计**：`wms_warehouse` 并非"无人用的重复表"——`wms` 模块的 `WarehouseController`、`LocationController`、`ReceiptServiceImpl` 均注入 `wms.warehouse.service.WarehouseService`，且既有架构记录把它描述为 `erp_warehouse` 的「扩展设计」（WMS 侧仓库携带 `is_wms_enabled`、`warehouse_type` 等 WMS 专属属性）。**因此本条的定性是「两张表的数据不一致导致下拉缺项」，不是「其中一张该删」。**
>
> **建议（需拍板，二选一）**：(a) 保持双表，补一条同步机制（在仓库规划页保存时同步 upsert `wms_warehouse`），最小改动；(b) 收敛为单一口径，把 `list-all` 切到 `erp_warehouse` 并按 `status`/租户过滤 —— 此路必须先核对 `wms_location.warehouse_id` 的取值来源与历史数据 id 对应关系，否则货位会挂空。

---

## 8. 规范性

### 8.1 后端

- **注释**：仓储核心类注释详尽且为中文，含设计决策与历史背景（如 `InventoryServiceImpl` 的双轨镜像决策、`MenuPermissionDeriver` 的命中规则），**高于项目平均水平**；
- **鉴权**：553 端点中仅 10 个裸端点，且均为合理例外（§4.2）；
- **控制器粒度**：11–22 个端点/控制器，未见上帝类；
- **异常处理**：统一 `WmsBusinessException` / `BusinessException` + `ApiExceptionHandler`。

### 8.2 前端

- **`any` 类型泛滥**（规范要求"禁止 any"）：

| 目录 | `: any` / `as any` 处数 |
|------|------|
| `views/wh` | 489 |
| `views/quality` | 110 |
| `views/erp/stocktake` | 81 |
| `views/erp/stock-assemble` / `stock-split` | 77 / 77 |
| `views/erp/stock-transfer` | 70 |
| `views/erp/stock-cost-adjust` | 66 |
| `views/erp/stock-in` | 63 |
| `views/erp/stock-overflow` | 61 |
| 其余页面 | 14–51 |

- **页面组件复用**：仓储页面统一使用 `PageContainer` + `CategoryListLayout` + `BillTableList` / `BillDetailTable` / `PageConfigPanel` + `useAutoGridSpan`，**复用度良好**；
- **直调 `request`**：多个页面绕过 `api/` 层直接 `request.get/post`（如 `erp/stock-alert-config/index.vue:318-592` 共 7 处、`erp/alert-query/index.vue:246`、`erp/stock-transfer/index.vue:870`），削弱了 API 层的统一治理能力，建议逐步收口。

---

## 9. 开发文档对照（24 篇）

> 方法：逐篇提取文档中的「菜单配置 / 接口路径 / 单号前缀 / 建表 / 业务规则 / 文档自标注未完成项」，再与代码与数据库事实逐条对照。逐篇提取原文见 `tool-results/doc-extract-1c6613.md`（10 篇单据）与 `doc-extract-1d17fd.md`（14 篇作业/质量）。

### 9.1 菜单配置一致性：**20/20 一致**（7 条路径写法漂移）

| 文档 | 文档记载 | 数据库实际 | 判定 |
|------|---------|-----------|------|
| 5001/5002/5003/5008/5009/5010/5011/5015/5016 | `erp/xxx/form` ⧉ `erp/xxx/index` ⧉ 历史 | **完全一致** | ✅ |
| 5014 生产模板 | `erp/stock-bom/form` ⧉ `erp/stock-bom/index` ⧉ **列表** | **完全一致** | ✅ |
| 90201 质检单 / 90202 质量标准 | 双入口，历史 | **完全一致** | ✅ |
| 5013 / 5017 / 80018 / 90203 / 81005 | 单入口（`display_mode=0`） | **完全一致** | ✅ |
| **80010–80016**（借进/借出/收货/上架/拣货/发货/移库） | 主路径 `wh/xxx/form`，标签路径 `wms/xxx` | 库中为 `path=wms/xxx/form`、`list_path=wh/xxx/index` | ⚠️ **写法互换，功能等价**（靠 `componentMap` 别名桥接），需回写文档 |

> 注：《41条双入口菜单的正确配置》中的 5014/80010/80011 三行是**旧版记载**；各单据自己的开发文档（如生产模板、借进单）与库一致，**以单据文档为准**。

### 9.2 接口路径一致性

- **盘点单**：文档给出的 3 条真实路径 `/api/erp/stock/take/{id}/process`、`/unchecked-products`、`/next-no` → 后端 `StockTakeController` **全部存在** ✅
- **收货单**：`/next-no`、`/page`、`/page-detail`、`/detail/save`、`/start`、`/confirm`、`/cancel` → `ReceiptController` **全部存在** ✅
- **移库单**：`/cancel`、`/detail/save`、`/next-no`、`/batch`、`/page`、`/page-detail` → `MoveController` **全部存在** ✅
- **拣货单**：`/task/cancel`、`/detail/save`、`/next-no`、`/page-detail` → `PickController`（22 端点）**全部存在** ✅
- **发货单**：文档工作流明确要求 `cancelShip(id,reason)` → **后端不存在**（见 §3.1 P0-2）❌
- 其余 8 篇文档只列方法名、未给完整路径，无法逐条比对路径，已按方法语义核对存在性。

### 9.3 文档标注「待完善」但代码**已实现**（文档滞后，需回写）

| # | 文档标注 | 代码事实 |
|---|---------|---------|
| 1 | 其他入库/出库、调拨、报损、报溢、组装单均写「列配置弹窗/页面配置弹窗**待实现**」 | 仓储 10 个单据页面**全部**已接入 `ColumnConfigPanel` + `PageConfigPanel` + 3Tab 配置弹窗 |
| 2 | 其他出库单 §2.7-1：「`views/erp/stock-out/` 下**缺 index.vue**；`componentMap` 中 `erp/stock-out/index` **错误映射到 `stock-transfer/index.vue`**」 | `views/erp/stock-out/index.vue` 存在（843 行）；`dynamicRoutes.ts:142` `'erp/stock-out/index' → @/views/erp/stock-out/index.vue` **已修正** |
| 3 | 报溢单 §5.7-1：「记账 `execute` 全系统各单据均只更新状态+记账人，**未写入库存**」 | `StockInServiceImpl:273`、`StockOutServiceImpl:273`、`StockOverflowServiceImpl` 等均已 `publishEvent(InventoryChangeEvent)`，由 `InventoryChangeEventListener` 过账 |
| 4 | 生产模板 §10.7-1：「`wh/production-template` 旧页面保留」 | 该目录**已删除**，`views/wh/` 下无 `production-template` |

### 9.4 文档标注「待完善」且代码**确实未实现**（真实缺口）

| # | 文档 | 缺口 | 级别 |
|---|------|------|------|
| 1 | 发货单 §2「工作流」 | `cancelShip` 后端无端点（§3.1） | **P0** |
| 2 | 成本调价单 §7.7-1 | 记账 `execute` **只改单据/明细的状态与金额，不回写库存成本**（`StockCostAdjustServiceImpl.java:253-281` 实证：仅 `updateById`，无任何库存表写入）→ 调价后出库成本、毛利仍按旧成本计算 | **P1** |
| 3 | 拣货单 §7-④ | `waveCreate` 前端调用签名不匹配；`detailConfirm`/`detailShortage` 后端存在但页面未用 | P2 |
| 4 | 质量证书 §7-① | 证书编号后端 create 未生成；`page` 未接收 `status` 筛选 | P2 |
| 5 | 借进借出查询 §7-③⑤ | 「查询方案」下拉为占位（无多方案持久化）；「库存换算结果/浮动数量」无口径数据恒为 0 | P2 |
| 6 | 借进/借出单 §7-① | 借转采购/借转销售**仅写台账字段，不生成下游单据** | P2 |
| 7 | 移库单 §7-③ | `freeze/unfreeze` 无调用（已用悲观锁替代，属有意设计） | — |
| 8 | 盘点单 §6.7-2 | 盘盈/盘亏生成的报损/报溢单为草稿态，送审/记账需人工接管（跨模块闭环未自动） | P2 |
| 9 | 预警设置 §7-③ | 「批量取消」语义为清除勾选，非删除阈值配置 | P2 |
| 10 | 质检单 §7-④ | `complete` 仅提示、未实际回写入库/批次 | P2 |

### 9.5 单号前缀一致性：10/10 一致

文档前缀与代码实现逐条核对一致：`QTRKD-`、`QTCKD-`、`BSD-`、`BYD-`、`KCPDD-`（序列 `PDD/KCPDD`）、`CBTJD-`、`ZZD-`、`CXD-`、`RC/PK/SH/MV/PA`（WMS 五单前端 `genTaskNo`）、`JJD-/JCD-`、`ZJD-`（质检单）、`QSTD-yyyyMMdd-UUID6`（质量标准）。**未发现前缀冲突或实现偏离**。

### 9.6 表格/表名一致性

文档仅 1 篇（盘点单）给出明确表名 `erp_stock_take` / `erp_stock_take_item` → 与库中一致 ✅；WMS 五单文档给出 `wms_receipt_task`/`wms_putaway_task`/`wms_pick_task`/`wms_ship_task`/`wms_move_task` → 与库中一致 ✅；质量三表 `quality_inspection`/`quality_standard`/`quality_certificate`/`quality_defect_handle` → 一致 ✅。

---

## 10. 修复建议与优先级

### 10.1 建议立即处理（P0）

| # | 动作 | 涉及文件 | 风险 |
|---|------|---------|------|
| 1 | 给业务角色补仓储权限授权，并**统一菜单码与权限码命名体系** | `sys_role_permission`、`sys_menu.menu_code` | 改系统配置数据，**需先出方案获批** |
| 2 | 补 `wms:ship:cancel` 权限码 + `ShipController./cancel` + `ShipService.cancelShip` | wms 模块 | 中（涉及状态机与库存回滚，需按 `ReceiptController.cancel` 范式） |
| 3 | WMS→ERP 的两条 HTTP 内部调用改为进程内调用（Spring 事件 / 直接注入 Service），删除裸 HttpClient | `ReceiptServiceImpl:353-387`、`EventServiceImpl:72-116` | 中（**改完须实测"确认收货"全链路**） |
| 4 | 销售出库改走 `InventoryChangeEvent`，不再直写 `erp_stock` | `SaleOrderServiceImpl` | 高（跨模块，建议与销售模块 P0 一并处理） |
| 5 | 统一仓库主数据源：`/wms/warehouse/list-all` 改读 `erp_warehouse` | `WarehouseServiceImpl:53-74` | 中（**须先核对 `wms_location` 的 warehouse_id 外键指向**） |

### 10.2 建议本轮收尾处理（P1）

| # | 动作 | 说明 |
|---|------|------|
| 5 | 清理盘点三套实现：保留 `erp/stock/take`（菜单 5003 在用），移除 `wh/inventory-order` + `wms/check/form` 孤岛页与 `erp/stock/check` 死链路 | 依据文档 §4.5 对 80017 的裁定；**删前需确认无外部调用** |
| 6 | 删除 3 个前端死 API 对象 + 6 个死方法 | 零引用，纯删 |
| 7 | 删除后端 `batchsn/ai/**`（11 文件）与 `BalanceLog(+Mapper)` | 零引用，纯删；`erp_balance_log` 表是否保留待定 |
| 8 | 处理其余孤岛页：`wms/{check,receipt,wave}/form`、`wms/warehouse`、`erp/stock/replenishment` | 逐页判定"补菜单入口"还是"删除"；`wms/warehouse` 与 §10.1-5 联动 |
| 9 | 修 `dynamicRoutes.ts:1555` 静态路由 `path:'stock'` 的错误标题（「销售出库」→「库存管理」） | 单行修正，零风险 |
| 10 | 成本调价单 `execute` 补库存成本回写 | `StockCostAdjustServiceImpl:253-281`；需先定"成本回写到 `erp_stock` 还是 WMS 轨" |

### 10.3 建议登记为技术债（P2）

9. 清理菜单脏数据：5013 残留 `list_path`、80012–80016 `sort` 并列、`component` 格式统一；
10. 回写《41条双入口菜单的正确配置》中 3 处与库不一致的记载；
11. 拆分 `erp-stock` 模块（商品/商城/营销/批次序列号）；组织拆分 `erp_stock_transfer_item`（64 列）等超宽表；
12. 前端 `any` 类型治理，优先 `views/wh`（489 处）；
13. 更新 `StockInServiceImpl:268` / `StockOutServiceImpl:268` 的过时 TODO 注释；
14. 补一轮真实单据运行验证（当前 WMS 全链路 0 数据）。

---

## 11. 本次已执行的修复（2026-09-23）

只做**零争议、可静态验证**的三类；凡涉及菜单/配置数据、业务语义、模块边界的，一律保留在 §10 待拍板。

| # | 修复 | 文件 | 验证 |
|---|------|------|------|
| 1 | 删除 4 个零引用 API 对象 + 6 个死方法（含配套 interface），净减约 160 行 | `frontend/apps/pc-admin/src/api/erp.ts` | 全仓 grep 无残留引用；`eslint` **0 error**（128 个 warning 均为既有 `any` 类） |
| 2 | 修正静态路由 `path:'stock'` 的错误标题「销售出库」→「库存管理」，图标 `ExportOutlined`→`ContainerOutlined` | `frontend/apps/pc-admin/src/router/dynamicRoutes.ts:1509` | `eslint` 通过；该 error（`1000:5` 的 ASI 保护分号）经核对为**既有**代码，非本次引入 |
| 3 | 两处**过时 TODO 注释**更正（`TODO-P0 库存收敛` → 已落地说明，其下代码即 `publishEvent(InventoryChangeEvent)`） | `erp-stock/.../StockInServiceImpl.java:268`、`StockOutServiceImpl.java:268` | 纯注释改动，未触碰逻辑 |

### 11.2 用户拍板后执行（D1–D10，2026-09-23）

| # | 动作 | 关键文件 | 验证 |
|---|------|---------|------|
| **D1.1** | 收货回写采购订单：**删掉裸 HttpClient 调用**，改为发布进程内事件 `PurchaseReceiptBackfillEvent`，由 core-api 的 `PurchaseReceiptBackfillListener` 同事务回写 | `wms/.../ReceiptServiceImpl.java`、`core-base/.../PurchaseReceiptBackfillEvent.java`、`core-api/.../PurchaseReceiptBackfillListener.java` | ✅ core-base / wms / core-api 编译通过 |
| **D1.2** | 事件 outbox 的 HTTP 派发整体移除：删 `ErpCallbackController`/`ErpCallbackService(Impl)`（**空壳**，四个方法只打日志）、删 `processPendingEvents`/`retryFailedEvents`/`handleFailure`、删 `EventController./outbox/process` 端点与前端 `processPending`，保留 outbox 的写入/查询/手动重置 | `wms/controller/ErpCallbackController.java`(删)、`wms/event/**`、`wms/controller/EventController.java`、前端 `api/wms/event.ts` | ✅ wms 编译通过 |
| **D2** | 补齐发货单取消：`ShipService.cancelShip` + `ShipController./cancel` + 权限码 `wms:ship:cancel`（幂等迁移 V11.496.0） | `wms/ship/**`、`wms/controller/ShipController.java`、`db/migration/V11.496.0__Add_Wms_Ship_Cancel_Permission.sql` | ✅ wms 编译通过 |
| **D3** | 仓储权限授权：系统管理员授**全部** 203 个、部门管理员授**只读** 83 个（脚本 `tools/grant-storage-permissions.py`，执行前已备份） | `sys_role_permission` | ✅ 回查 role_id=1/2065…369 各 203、…018 为 83 |
| **D4** | 删除盘点死链路：`StockCheckController`/`Service`/`ServiceImpl`/`StockCheck(+Item)`/`StockCheckStatus`/两个 Mapper/单测，共 **9 个文件** | `erp-stock/.../stock/**` | ✅ erp-stock 编译通过；全仓无残留引用（表 `erp_stock_check` 保留） |
| **D5** | 删除 7 个孤岛页面（2741 行）及 `componentMap` 僵尸键、静态路由；连带清理 `api/wms/check.ts`（整文件）与 `whTask.CHECK_STATUS_MAP` | `views/{wh/inventory-order,wms/{check,receipt,wave,warehouse},erp/stock/replenishment}`、`router/dynamicRoutes.ts` | ✅ 菜单连通性复跑 **0 失败**；前端审计 61→54 页、0 孤儿 |
| **D6** | 成本调价单 `execute` 回写库存成本（`erp_stock.unit_price`，按 商品×仓库 定位） | `erp-stock/.../StockCostAdjustServiceImpl.java` | ✅ erp-stock 编译通过 |
| **D7** | 仓库主数据同步：erp 侧 `WarehouseServiceImpl` 的增/改/停/删发布 `WarehouseChangedEvent`，wms 侧 `WarehouseSyncListener` upsert `wms_warehouse`（id 与 `warehouse_id` 均取 erp 侧 id，与现有种子一致） | `erp-stock/.../WarehouseServiceImpl.java`、`core-base/.../WarehouseChangedEvent.java`、`wms/warehouse/listener/WarehouseSyncListener.java` | ✅ 两侧编译通过 |
| **D8** | 菜单数据：5013 清空残留 `list_path`；80012–80016 `sort` 改为 1..5（改前备份 `tool-results/menu_backup_before_D8_20260923.json`） | `sys_menu` | ✅ 已回查 |
| ~~D8-3~~ | **未做**：菜单 `component` 格式统一。理由：全站 **278 条**用 `views/xxx` 形式，仓储那 12 条才是少数派 —— 改哪边都零功能收益，且属全站性变更 | — | 建议单独立项 |
| D9 | 保留 `batchsn/ai`（AI 能力骨架）与 `BalanceLog` | — | 按拍板保留 |
| D10 | 销售直写 `erp_stock` | — | **未动**，与销售模块 4 个 P0 同批处理 |

**编译验证附加说明**：全量 `mvn -pl core/api/core-api -am compile` 在本机**反复随机失败在不同模块**（erp-partner → erp-marketing → …，且同一模块单独编译又成功），形态是「无法访问 xxx」「找不到符号」在 import 行 —— 与 IDE 的 Java 语言服务器（ECJ）并发写 `target/classes` 的特征一致。最终采用「先 `mvn -pl erp/erp-purchase -am install` 装依赖到本地仓库，再 `mvn -pl core/api/core-api compile`」绕开，**结果 BUILD SUCCESS**。四个受影响的模块（core-base / erp-stock / wms / core-api）均已单独编译通过。

### 经复核后**决定不动**的死代码

| 对象 | 行数 | 不动的原因 |
|------|------|-----------|
| `erp-stock/batchsn/ai/**`（3 个 service + 6 个 DTO） | 1581 | **不是"废弃代码"，是"只有接口、没有实现"的 AI 能力骨架**（`BatchAnomalyDetectionService` 等均为 interface 且无 Impl）。删除等于放弃这项产品规划，须拍板 |
| `BalanceLog` + `BalanceLogMapper`（表 `erp_balance_log`） | ~60 | 实体/Mapper 齐备但无 Service 消费 —— 形态更像**预留的库存余额流水**，同属规划性代码 |

---

## 附：判定口径说明（防误判）

1. **"无引用" ≠ 可删**：`@Controller/@Service/@Component/@Mapper` 等容器管理类在 Java 源码中本无引用，死代码扫描已排除；剩下的 4 个类经人工二次确认。
2. **"前端调了后端没有" ≠ 线上故障**：18 条中 16 条位于零引用死对象内，只有 §3.1 的两条（发货/盘点取消）挂在被真实调用的方法上。
3. **行数为 0 ≠ 死表**：`wms_*` 执行表 live=0 是**环境无数据**所致（上游链路未跑通），不能据此判定表冗余；冗余判定改以"是否有消费方代码"为准。
4. **菜单不可达的判定**：同时满足"无 `sys_menu` 引用"+"无源码 `router.push`/`router-link` 指向"，排除 `componentMap` 别名兜底造成的假阴性。
