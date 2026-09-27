# 配送模块（DMS）全面审计报告

> **审计日期：** 2026-09-23
> **审计范围：** 前端 `views/{dms, dispatch, md/route, trade/api-monitor}` + 司机端 `apps/driver-delivery`；后端 `dms` 模块 + `erp-delivery-route` + `core-base(trade)` + `erp-sales(SaleLogisticsController)`；数据库配送域 47 张表；`sys_menu` 配送子树 38 条 + `资料→配送管理→线路`(70530)；`sys_permission` 配送域 146 个权限码
> **对标依据：** `docs/Yh-Spec/手动整理对标开发文档/配送模块/`（28 篇）+ `系统菜单设计与管理/`（5 篇）
> **审计维度：** 可用性 / 规范性 / 冗余 / 死代码 / 功能接通 / 跨模块关系 / 权限合规

---

## 0. 审计方法与可复现脚本

全部结论来自可复现的**只读**脚本或直连 devdb 的 SQL，脚本已落在 `tools/`：

| 脚本 | 作用 | 产物 |
|------|------|------|
| `tools/audit-dms-menu.py` | 模拟 `dynamicRoutes.getComponent` 解析规则，校验配送菜单可达性 / 双入口自洽 / 可见性 / sort | 控制台 |
| `tools/audit-dms-frontend.py` | 配送域 55 个 `.vue` × 路由引用 × 菜单引用 × 源码引用 | `tool-results/dms-frontend-audit.json` |
| `tools/audit-dms-backend.py` | 31 个控制器 / 369 端点 / 鉴权注解 / 权限码提取 | `tool-results/dms-backend-audit.json` |
| `tools/audit-dms-api-wiring.py` | 前端 `src/api/**` + `src/views/**` 直调 ↔ 后端端点双向比对 | `tool-results/dms-api-wiring.json` |
| `tools/audit-dms-table-usage.py` | 47 张表 × 实体 × Mapper/Service/Controller 消费方 × 真实行数 | `tool-results/dms-table-usage.json` |
| `tools/audit-dms-deadcode.py` | 后端非容器管理类引用计数（已排除 Spring Bean 误报） | `tool-results/dms-deadcode.json` |
| `tools/audit-dms-recheck.py` | **稳健性复核**：全后端端点集判断链 + 正确注解窗口重扫裸端点（见附录 §6-7） | `tool-results/dms-recheck.json` |
| `tools/grant-dms-permissions.py` | 配送域权限授权（写操作，执行前自动备份） | `tool-results/role_perm_backup_before_dms_grant_20260923.json` |
| `tools/audit-permission-codes.py`（既有） | 权限码 注解 ↔ 种子 对账 | `tools/audit-permission-codes.json` |

**规模基线：**
- 前端：配送自有域 **48 个页面**（`dms` 38 + `dispatch` 8 + `md/route` 1 + `trade/api-monitor` 1），另 7 个「入口直达型」复用目标页（`order-center`、`sales/return-apply`×2、`sales/return-doc`×2、`erp/purchase`×2）；含 16 个子组件，合计 47,119 行
- 后端：`dms` 274 个 Java 文件；控制器 **31 个 / 端点 369 个**（配送自有路径 357 个）
- 菜单：配送子树 **38 条 = 1 顶级 + 10 分组 + 27 叶子**，另加跨域挂载的 `70530 线路`
- 数据库：配送相关表 **47 张**

---

## 1. 结论摘要

| # | 级别 | 问题 | 影响 |
|---|------|------|------|
| P0-1 | 阻断 | 配送功能对非超管不可用。**根因不是「平台没授权」，而是两层授权链路的第二层未启用**：模块门已开（租户 1/2 均开 `dms`），但除超管外**没有任何角色持有 `tenant-admin:role:*`** ⇒ 租户管理员无法自助配权限 | 非超管看得见菜单、点进去全部 403（§4.1，**2026-09-26 定性修正**）<br>临时手段：自有域 126 码已授系统管理员(126)/部门管理员(68) —— §11.2 |
| P0-2 | 阻断 | **司机端 App 的登录页 + 3 个 Tab 指向不存在的后端端点** | 默认首页 `/order`、地图 Tab、我的→历史/统计 全部空态或 TypeError（§3.1） |
| P0-3 | 阻断 | **20 条配送自建菜单 `menu_level=3`（非法值）**，非系统租户的非超管**整块看不到 DMS 自建体系** | 与 P0-1 叠加 = 看不见 + 调不了（§4.6）<br>✅ **已修**：`V11.501.0` + devdb 手工执行（20 条 3→0） —— §11.2 |
| P1-1 | 高 | 配送域前端 **`v-permission` 用量 = 0**（全站 380 处） | 无权限的用户看见所有按钮，点击才 403（§4.3） |
| P1-2 | 高 | **14 个叶子菜单的 `menu_code` 在权限码表里找不到对应**（命名体系断裂） | 菜单派生 fail-open，该 14 页对所有人可见（§4.2） |
| P1-3 | 高 | `OpenApiController` 12 个端点**无任何鉴权注解**，自称「签名校验」但 `signature` 参数从未使用，含 3 处 TODO 桩 | 对外 API 既不可用（未放行白名单）又有越权面（§3.2）<br>⚠️ **与交易模块 P0-3 同一处代码，处置并入那边，勿两处各改一遍** |
| P1-4 | 中 | **5 张僵尸表**（无实体 / 无 Mapper / 无 SQL / 零数据） | 早期建模残留（§6.1） |
| P2-1 | 中 | 14 张表**零业务数据**，配送多条链路从未真实跑通 | 静态结论未经运行时验证（§5.3） |
| P2-2 | 中 | **10 张表列数超开发规范**（`dms_task` 61 列，规范 ≤25） | 存量宽表（§5.1） |
| P2-3 | 中 | 车辆档案 `ratedLoad`/`cargoVolume` **未接入派单约束**（调度用的是全局配置上限） | 配置口径 ≠ 车型口径（§7.2） |
| P2-4 | 低 | 订单池过期扫描、结算周期生成**无定时任务**；配送员绩效字段**无回写方** | 文档已自述，本次确认（§7.3） |
| P2-5 | 低 | 配送业务分组下 3 个叶子 `sort` 并列 | 展示顺序不稳定（§2.2）<br>✅ **已修**（改为 1/2/3） —— §11.2 |
| P2-6 | 低 | 前端 `any` 类型 1005 处（`dms` 728 + `dispatch` 235） | 规范偏离（§8.2） |
| P2-7 | 低 | `scheduled_task` 表 12 条**垃圾演示数据**（`job_key` 为空，重复两份） | 脏数据（§5.4） |
| P2-8 | 低 | 3 处文档失实记载（项数、菜单挂载、孤儿页结论） | 文档已过期（§9.7） |

**总体判断：** 配送模块是本次系列审计中**代码卫生最好**的模块——菜单连通性 0 缺陷、前端 0 孤儿页、前后端接线 0 断点、后端死代码 0、权限码 0 缺失、dms 模块端点鉴权覆盖 100%、无桩代码、不越界写库存。但它有**三个结构性缺口**：

1. **权限只授超管，且前端零权限控制** —— 与采购、仓储模块同源（见 §4）；
2. **司机端只完成了「配送/路线」两个 Tab**，登录与订单/地图/我的仍是原型桩（§3.1）；
3. **命名体系与口径的两处分裂**：菜单码 ↔ 权限码（§4.2）、配置上限 ↔ 车型档案（§7.2）。

---

## 2. 菜单与路由连通性

### 2.1 连通性校验结论：**0 缺陷**

按前端 `getComponent()` 的真实解析规则（去 `views/` 前缀与 `.vue` 后缀 → 精确命中 → 追加 `/index` → 去 `/index` → 磁盘兜底）逐条模拟：

- **27 个叶子**（含跨域的 `70530 线路`）的 `component` **全部**解析到磁盘真实存在的 `.vue`：**0 条坏菜单**
- 3 个双入口菜单（`80760 配送单`、`80770 车辆管理`、`80790 配送员管理`）的 `list_path` **全部可解析**：**0 条打不开的标签**
- 双入口自洽性：`display_mode=1` 的 3 条**均有** `list_path` + `tag_label`；`display_mode=0` 的**无一条**残留 `list_path`
- 可见性/状态/客户端：27 个叶子全部 `status=1`、`visible=1`、`client_type=tenant-admin`，**0 异常**

### 2.2 规范性瑕疵（唯一一条）

| 问题 | 证据 | 建议 |
|------|------|------|
| `60401 配送业务` 下 3 个叶子 `sort` 并列 | 配送查询#1、配送单#1、配送仪表盘#2 | 改为 1/2/3 |

其余分组（配送路线、人车管理、物流配送、收货业务、调度管理、配送跟踪、配送配置、结算收款、API监控）的叶子 `sort` 均无冲突。**10 个分组自身**在顶级 `60005` 下也有两组并列（`60401/60501` 同为 100、`60402/60403` 同为 300、`60504/60505` 同为 400、`60506/61506`＝500/600），属轻微微瑕。

### 2.3 文档与库不一致（3 处，均以库为准）

| 文档 | 文档记载 | 数据库实际 | 判定 |
|------|---------|-----------|------|
| `配送模块DMS开发文档.md` §2 | 「实际页面清单（sys_menu 实测 · **8 列 11 项**）」，表格只列 8 行 | **10 分组 27 叶子**（38 条） | 文档写于 `V11.168/169.0` 之前，**已过期** |
| 同上 §2 附注 | 「`60502 人车管理` 无子菜单项，UI 不渲染（车辆/配送员页面当前**无菜单挂载**，属待补入口）」 | `V11.168.0` 已补 5 个子菜单（车辆管理/车辆维护/配送员管理/用车管理/人员核验），全部可见可达 | **已修复，文档未回写** |
| `README.md` §1.1 标题 | 「本系统配送模块实际菜单（sys_menu 实测 · **10 分组 25 项**）」 | 表格实为 **27 行** | 标题数字错误 |

**另有一处文档已被证伪**：`线路开发文档.md`「重复性研判」称 `views/dms/route/index.vue` **「在 componentMap 注册但 sys_menu 无菜单挂载 → 不可达死页」**——实测 `V11.169.0` 已挂菜单 **80830 路线规划**，该页可达且是金标准页（E2E 186/186）。**该结论已完全失效**。

---

## 3. 前后端功能接通

### 3.1 P0：司机端 App 的登录与 3 个 Tab 指向不存在的端点

司机端 `frontend/apps/driver-delivery` 共 5 个底部 Tab（`App.vue`）+ 登录页。真实契约迁移只覆盖了其中 2 个：

| 入口 | 页面 | 调用 | 后端 | 判定 |
|------|------|------|------|------|
| **登录页** `/login` | `views/login/index.vue:41` | `api.auth.login()` → `POST /api/v1/delivery/auth/login` | **不存在** | ✗ 登录不可用 |
| **订单 Tab**（默认首页 `/` → `/order`） | `views/order/index.vue:53` | `api.order.getList()` → `GET /api/v1/delivery/orders` | **不存在** | ✗ 空态 |
| 订单详情 `/order/:id` | `views/order/detail.vue:16` | `api.order.getDetail` / `startDelivery` | **不存在** | ✗ |
| **配送 Tab** `/delivery` | `views/delivery/*` | `@/api/dms`（`/api/dms/task/page`、`/dms/sign/submit`…） | ✅ 存在 | ✅ 已接真实契约 |
| **路线 Tab** `/my-route` | `views/route/my-route.vue` | `@/api/delivery-route`（`/api/delivery/route/*`） | ✅ 存在 | ✅ |
| **地图 Tab** `/map` | `views/map/index.vue:32` | `api.delivery.getMapPoints()` | **方法在 `api/index.ts` 中根本不存在** | ✗ 必然 `TypeError` |
| 导航 `/navigation` | `views/map/navigation.vue:19` | `api.order.getDetail(orderId)` | 不存在 | ✗ |
| 路线优化 `/route` | `views/route/index.vue:53` | `api.order.getList({status:'pending'})` | 不存在 | ✗ |
| 我的→历史 `/user/history` | `views/user/history.vue:47` | `api.delivery.getHistory(...)` | **方法不存在**（`api/index.ts` 只有 `api.user.getHistory`） | ✗ `TypeError` |
| 我的→统计 `/user/statistics` | `views/user/statistics.vue:56` | `api.delivery.getStatistics()` | **方法不存在** | ✗ `TypeError` |
| **我的 Tab** `/user` | `views/user/index.vue` | 仅读 `stores/user.ts` 本地状态 | — | 静态可渲染 |

**根因**：`frontend/apps/driver-delivery/src/api/index.ts` 的 `order` / `delivery` / `map` / `user` / `auth` **五组共 25 个端点**全部基于 `baseURL = '/api/v1/delivery'`，而后端**没有任何控制器**注册在该前缀下（全仓仅 `tests/tests/delivery-test-cases/.../DeliveryModuleTest.java:25` 把这个字符串当作测试常量）。真实契约的迁移只做了 `delivery`/`sign`/`payment`/`verification`/`tracking`/`route` 六个域（见 `api/dms.ts`、`api/delivery-route.ts`）。

**文档早已自述**：《签收管理开发文档》§7.5 明确写——「司机端登录仍打不存在的 `/api/v1/delivery/auth/login`；司机端『订单』Tab 仍为原型假数据」。**即：这是已知遗留，代码未修。**

> **附带问题**：`views/login/index.vue:19` 的品牌 logo 指向外部第三方图片生成服务
> `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=配送员logo图标&image_size=square`
> —— 原型期产物，属**运行时外部依赖 + 信息外泄**（把中文 prompt 发往第三方），生产必须换为本地静态资源。

### 3.2 P1：`OpenApiController` 的鉴权与桩（`core-base/trade`，被「API监控」菜单邻接）

```
文件：backend/core/base/core-base/src/main/java/cn/aiedge/trade/controller/OpenApiController.java
映射：@RequestMapping("/api/open")   —— 12 个端点
鉴权：类与方法上【无任何】@SaCheckPermission / @SaCheckLogin / @SaIgnore
白名单：SaTokenConfig:33-80 / 86-133 两份 excludePathPatterns 均【不含】/api/open/**
```

三条结论：

1. **对外 API 事实上不可用**：类注释自称「用于接收外部平台（电商、ERP等）的订单和库存请求」，但 `/api/open/**` 不在放行名单，`SaInterceptor` 对 `/**` 强制 `StpUtil.checkLogin()` → **外部无会话的系统必然 401**。
2. **但登录用户可无权限码调用**：任何已登录用户（无需任何 `trade:*` 权限）即可 `POST /api/open/order/callback/{channelCode}` 写入伪造订单、`GET /api/open/inventory/query` 查任意 SKU 库存、`POST /api/open/inventory/lock` 触发「锁定」。
3. **自称的安全校验是空头支票**：类注释第 4 条「安全验证 - 签名校验」，而 `orderCallback` 第 49 行接收的 `signature` 参数**在方法体内从未被使用**（`grep` 全文无第二处出现）。

另有 **3 处 TODO 桩**（均在方法体内、只打日志或直接返回成功）：

| 端点 | 行 | 桩内容 |
|------|----|--------|
| `POST /order/status/{internalOrderId}` | 79 | `// TODO: 实现订单状态推送` → 只 `log.info` |
| `POST /inventory/lock` | 111 | `// TODO: 实现库存锁定逻辑` → 只做余量比较，未真正锁定 |
| `POST /inventory/release` | 124 | `// TODO: 实现库存释放逻辑` → 直接 `Result.success()` |

**功能已被取代**：前端 `api/trade/index.ts:292` 注释写明「原调开放接口 `/api/open/order/pending-count`（供外部平台回调使用），管理端页面**已切回本域端点**」。即 `ExternalOrderController`(`/api/trade/external-order/*`，有 `trade:external-order:list` 类注解) 是它的替代品。

> **建议（需拍板）**：二选一 ——（a）**下线** `OpenApiController`（12 端点 + 3 桩），把对外能力统一收敛到 `trade/external-order` 并补 API Key/签名机制；（b）保留但补 `@SaIgnore` + 真实签名校验 + 白名单，并明确谁在调用。**当前形态（不可用 + 无鉴权 + 假校验 + 桩）三者兼有，是本次审计风险最集中的单点。**
>
> **⚠️ 交叉引用（勿重复处置）**：本条与 **交易模块审计**（`TRADE_MODULE_AUDIT_20260923.md` §P0-3「外部平台空壳」）**是同一处代码**——那边同时记录了 `ExternalOrderServiceImpl.receiveCallback` 的验签代码被注释掉、`TaobaoChannelAdapter.pullOrders` 返回硬编码假数据。
> `OpenApiController` 的归属是 **trade 域**（不在 `dms` 模块内），本报告从「被配送菜单邻接」的角度确认其状态，**处置应并入交易模块的 P0-3 一并决策**，不要在两处各改一遍。

### 3.3 接线比对结论：**前端调了后端没有 = 0**

配电域 338 组 `(verb, path)` 前端调用 ↔ 357 个后端端点双向比对：

- **① 前端调用了但后端没有：0 个** ✅
- **② 后端有端点但前端未调用：19 个**，逐条归因后**全部合理**：

| 端点 | 归属 | 判定 |
|------|------|------|
| `/dms/execution/{id}/{arrive,load,pickup,urge,route}`（5） | 配送执行（司机端语义） | 司机端未接 —— 与 §3.1 同源 |
| `/dms/task/rider/{id}`、`/rider/{id}/active-count`（2） | 司机端任务 | 同上 |
| `/dms/verification/me/rider` | 司机端身份 | 同上 |
| `/dms/tracking/stream` | SSE | 前端用 `EventSource`（非 `request.*`），脚本口径外 |
| `/dms/channel/callback` | 渠道回调 | 外部平台调用，正常 |
| `/dms/event/pending`、`/dms/event/{id}/retry`（2） | `dms_event_outbox` | **无任何前端消费方**，见 §6.2 |
| `/dms/rider/{import-template,import-excel}`（2） | 配送员导入 | 由 `BaseDataImportWizard` 通用组件消费（URL 走 props，脚本首轮漏报，已修正口径 —— 见 §6.2） ✅ |
| `/erp/md/route/{import-template,import-excel}`（2） | 线路导入 | 同上 ✅ |
| `/delivery/route/list`、`/active/{id}`（2） | 配送路线单 | 页面用的是 `/page`；`/active` 供司机端 |

> 说明：脚本口径为 `src/api/**` + `src/views/**` 中的 `request.*` 直调，不含 `EventSource` 与司机端应用（`apps/driver-delivery` 单独核验，见 §3.1）。

---

## 4. 权限配置

### 4.1 P0：配送权限的完整链路 —— **模块门已开，缺的是租户侧自助配置能力**

> **⚠️ 2026-09-26 定性修正**：本条初版写成「146 个权限码只授超管 ⇒ 非超管不可用」，**定性不完整**。
> 本仓授权是**两层**（见 `ModuleEntitlementService` 类注释）：
> **① 模块授权**（平台方决定某租户**有没有**这个模块，`sys_tenant_module`）
> **② 权限**（租户内管理员决定某角色**能不能做**某件事，`sys_role_permission`）
> 逐层核验后，真正的缺口在**第 ② 层的配置能力**上，而不是"平台忘了授权"。

```sql
-- 角色 × 配送域权限码（dms:* / dispatch:* / delivery:* / md:route* / trade:*）
SELECT r.role_name, count(*) FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
JOIN sys_role r ON r.id = rp.role_id
WHERE p.permission_code LIKE 'dms:%' OR p.permission_code LIKE 'dispatch:%'
   OR p.permission_code LIKE 'delivery:%' OR p.permission_code LIKE 'md:route%'
   OR p.permission_code LIKE 'trade:%'
GROUP BY r.role_name;
```

| 角色 | 配送域权限码授权数 |
|------|:---:|
| 超级管理员（role_id=1） | **146** |
| 系统管理员 | 0 |
| 部门管理员 | 0 |
| E2E租户2管理员 | 0 |
| role_mqd2x2di | 0 |

```sql
-- 角色 × 配送菜单（38 条）授权数：全部为 0（含超管 —— 超管走通配 ["*"] 全量下发）
```

#### 4.1.1 第一层（模块门）：**配送模块已给租户开通**

```
sys_module            : dms「配送」status=1（已注册）
sys_module_permission : dms → 'dms:'      （调度 / 派单 / 骑手车辆 / 履约核销 / 结算）
                        dms → 'delivery:' （配送路线单执行单，与 dms:route:* 分开登记）
sys_tenant_module     : tenant 1 = dms、tenant 2 = dms，**均 status=0（0=正常，1=停用）**
```

`ModuleEntitlementInterceptor` 对每次请求做「接口所需权限码 → 归属模块 → 该模块须已给本租户开通」的判定；**租户 1、2 均已开通 dms，这一层不构成阻断**。

> 口径说明：`SysTenantModule.status` = **0=正常 / 1=停用**（实体注释），与 `sys_menu` 的「1=启用」**相反** —— 又一处「status 逐表不同」。
> `dispatch:*`（7 条菜单码）**未登记**在 `sys_module_permission`：它只作菜单码用（`dispatch:` 开头的权限码为 0 条），不参与模块门判定。

**用户侧分布**：47 个用户中 **43 个是超级管理员**，非超管仅 4 个（系统管理员 2 / E2E租户2管理员 1 / 其他 1）。

**传导链**：
1. `UnifiedPermissionCacheService.getPermissions()` 对超管返回 `["*"]`（通配）→ 超管不受影响；
2. 非超管走 `userService.getUserPermissionCodes()` → **不含任何配送权限码** → `@SaCheckPermission("dms:task:list")` 等**一律 403**；
3. 菜单侧（见 §4.2）：`MenuPermissionDeriver` 对「不在权限码前缀集合里」的菜单码按过渡口径 **fail-open 保持可见**；
4. **净效果：非超管用户看得见配送菜单，点进去每个接口都 403。**

> 与 `PURCHASE_AUDIT_REPORT_20260922.md`（采购）、`STORAGE_MODULE_AUDIT_20260923.md`（仓储）**完全同源**，是跨模块的系统性问题，不是配送模块独有缺陷。但配送是本系列第三个确认的模块，建议**一次性出全局授权方案**，不要再逐模块打补丁。

#### 4.1.2 第二层（租户侧配置能力）：**当前除超管外无人持有**

| 角色 | 权限码总数 | `tenant-admin:` 角色/权限管理类 |
|------|:---:|---|
| 超级管理员 | 通配符 `*` | 全部 |
| 系统管理员 | 724 | **仅 2 个**（`tenant-admin:user:detail` / `:list`） |
| 部门管理员 | 281 | **0** |
| E2E租户2管理员 | **4** | **0**（仅 `crm:contract:view/create` + `user:detail/list`） |

能「给角色分配权限」的接口（`POST /api/role/{id}/permissions` 需 `tenant-admin:role:update`；可分配清单 `GET /api/permission/tree` 需 `tenant-admin:permission:list`）—— **除超管外没有任何角色可用**。

**完整链条**：

```
租户已开通 dms 模块 ✓
  └─ 但租户管理员角色（E2E_T2_ADMIN）没有 tenant-admin:role:update / permission:list
       └─ 无法自助给本租户角色勾选 dms: 权限码
            └─ 该租户下所有角色都拿不到 dms 权限码
                 └─ 接口 403（@SaCheckPermission）
```

**因此本条的正确定性是**：「**平台开模块 → 租户自助配权限**」这条两层设计链路，**第二层目前处于未启用状态**（能配置的角色不存在）。这是**全站性问题**（不止配送），`SETTINGS_MODULE_AUDIT_20260924` / `SYSTEM_MODULE_AUDIT_20260924` 均有同型记录（「SYSTEM_ADMIN 角色名不副实」）。

> **正确修法（修订版）**：不是继续给业务角色硬编码授权（那只是绕过第二层），而是**给平台/租户管理员角色补齐 `tenant-admin:role:*` / `tenant-admin:permission:*`**，让设计链路真正跑起来。
> §11.2-3 已执行的那次授权是**临时手段**（让系统租户管理员在链路打通前可用），不改变上述判断。

### 4.2 P1：14 个叶子菜单的 `menu_code` 与权限码命名体系断裂

`MenuPermissionDeriver` 的命中规则是：菜单码 `C` 算「有对应权限码」，当且仅当存在权限码 `P` 满足 `P = C` 或 `P` 以 `C + ":"` 开头。逐条核验 27 个叶子：

**命中（13 个）**：配送仪表盘、车辆管理、车辆维护、配送员管理、人员核验、智能调度、订单池、配送跟踪、签收管理、渠道管理、配送配置、配送结算、收款管理、API监控

**未命中（14 个）** —— 权限码库里找不到 `menu_code` 本身或其合法前缀：

| 菜单 | menu_code | 实际使用的权限码域 |
|------|-----------|------------------|
| 70150 配送查询 | `dispatch:query` | `dms:task:view` |
| 80760 配送单 | `dispatch:dispatch-order` | `dms:task:*` |
| 70155 物流发货 | `dispatch:logistics-ship` | `erp:sale:*` / 无专用码 |
| 70156 发货查询 | `dispatch:ship-query` | `erp:sale:outbound:*` |
| 70162 物流运费对账 | `dispatch:freight-reconcile` | `erp:sale:logistics:*` |
| 70160 物流退货收货 | `dispatch:return-receive` | `erp:sale:return:*` |
| 70161 采购订货收货 | `dispatch:purchase-receive` | `erp:purchase:order:*` |
| 80700 配送路线单 | `dms:route-list` | **`delivery:route:*`** |
| 80830 路线规划 | `dms:route-plan` | **`dms:route:*`** |
| 80730 调度任务 | `dms:dispatch-task` | **`dms:task:*`** |
| 80740 实时跟踪 | `dms:realtime-tracking` | **`dms:tracking:*`** |
| 80750 配送参数 | `dms:config-params` | **`dms:config:*`** |
| 80840 用车管理 | `dms:vehicle-usage` | `dms:vehicle-energy:*`（+ `dms:verification:*`） |
| 70530 线路 | `md:route` | **`md:route-master:*`** |

**影响**：`MenuPermissionDeriver` 判定这 14 个菜单「权限码库尚未覆盖」→ 按 fail-open 全部保持可见。也就是说，**即使将来给角色补了权限，这 14 个菜单的可见性也无法从权限码派生**，必须继续依赖 `sys_role_menu`（而该表配送域 0 行）。

**修法（需拍板）**：对齐命名 —— 把 `menu_code` 改成与权限码同源（如 `80730 dms:dispatch-task` → `dms:task`；`80700 dms:route-list` → `delivery:route`；`70530 md:route` → `md:route-master`），或在权限码库补一组与菜单码同名的「导航权限码」。**前者更干净，但要同步改 14 条 `sys_menu` 数据。**

### 4.3 P1：配送域前端零权限控制

```bash
$ grep -rn "v-permission" views/dms views/dispatch views/md/route views/trade/api-monitor | wc -l
0
$ grep -rn "v-permission" views | wc -l
380
$ grep -rn "hasPermission|usePermission|checkPermission" views/dms views/dispatch ... 
（无任何命中）
```

**全站 380 处 `v-permission` 分布在 18 个目录**（erp 13 文件 / fixed-asset 10 / system 9 / sales 9 / agreement 9 / finance 8 / crm 8 / set 7 …），**配送域 0 处**，也没有使用任何等价的指令式判定。该模块与《权限分层》既有结论对齐：**前端 `v-permission` 是权限码的消费方之一，配送域缺这一层**。

**后果**：新增 / 删除 / 审核 / 派单 / 结算 / 推送财务 等所有按钮对任何能进入页面的用户都可见可点，点击后才由后端 403 拒绝 —— 不符合「最小权限 + 界面即权限」的行业惯例。

### 4.4 P0：20 条配送自建菜单的 `menu_level=3`（非法值）导致整块不可见

**字段语义**：`SysMenu.menuLevel` 实体注释与《mega-menu-redesign.md》都只有 **`0=租户级 / 1=系统级`** —— 3 是**无人承认的取值**。

**过滤逻辑**：

```java
// SysMenuServiceImpl.getUserMegaMenus:259-283
if (isSystemTenant && isSuperAdmin) {
    return buildMenuTree(excludeTenantHidden(getAllMenus(), tenantId), 0L);  // ← 早退：拿全量
}
...
// 普通租户：只返回租户级菜单（menuLevel=0），但超管不受限制
if (!isSystemTenant && !isSuperAdmin) {
    wrapper.eq(SysMenu::getMenuLevel, 0);      // ← 硬过滤：level=3 全部消失
}
```

**受影响的 20 条**（全是本系统自建的 DMS 页面；配发收那 6 页是 `level=0`，不受影响）：

```
80760 配送单   80820 配送仪表盘   80700 配送路线单   80830 路线规划
80770 车辆管理 80780 车辆维护     80790 配送员管理   80840 用车管理   80847 人员核验
80730 调度任务 80850 智能调度     80860 订单池
80740 实时跟踪 80870 配送跟踪     80900 签收管理
80750 配送参数 80880 渠道管理     80890 配送配置
80910 配送结算 80920 收款管理
```

**根因（两层）**：

1. **源头 = `V6.22.0__Fill_Mega_Menu_All_Columns.sql`** —— 这是一个**批量占位迁移**，用 `views/common/placeholder/index.vue` 一次性插入大量菜单占位行，**每一条的 `menu_level` 都硬编码为 `3`**（同批的仓储 `80010/80011/80012`、销售 `80050` 同样是 3）。配送的 `80730/80740/80750/80760` 就在这一批里。
   > 这也解释了**全库为何有 124 条** level=3 —— 它是批量脚本的产物，不是个别笔误。
2. **沿用 = `V11.168.0` / `V11.169.0` / `V11.170.0` / `V11.201.0`** 后续补录菜单时沿用了同一写法（也写 3）；其中 `V11.169.0` 等用 `ON CONFLICT (id) DO UPDATE SET menu_level = EXCLUDED.menu_level`（**重放会覆盖回 3**），而 `V6.22.0` 用的是 `DO NOTHING`（不覆盖）。

> **对其他模块的价值**：`V11.499.0` 注释列出的 marketing 19 / finance 17 / mall 13 / set 10 / md 8 / crm 6 等同类条目，**根因大概率同是这个 V6.22.0 批量模板**，各模块出迁移时可一并按此追溯。

**为什么历次 E2E 全绿也没发现**：dev 的 `admin` 账号**同时命中** `isSystemTenant` 与 `isSuperAdmin` → 走 `:259` 早退分支取全量，**本地永远看不出问题**。必须用一个「普通租户 + 非超管」的账号请求菜单接口才会暴露。

> **⚠️ 陷阱**：修这条时若按 `path LIKE 'dms/%'` 批量更新，会**漏掉 `80760 配送单`** —— 它的 path 是 `dispatch/dispatch-order/form`，不含 `dms/` 前缀。（`V11.499.0` 的注释里把 dms 记为「19 条」，正是按前缀统计所致。）

> **交叉引用**：`V11.499.0`（分析模块 29 条）与 `V11.500.0`（CRM）是**同期另两个会话**对同一类问题的修复，其注释明确写「全库 menu_level=3 另有 dms 19 / marketing 19 / finance 17 / mall 13 / set 10 / md 8 / crm 6…，属各自模块的审计范围」——**配送这 20 条正是被留给本报告的**。修法已按该模式落地（§11.2）。

### 4.5 接口鉴权覆盖率：**dms 模块 100%**

`dms / erp-delivery-route / core-base(trade) / erp-sales(logistics)` 共 **31 个控制器 / 369 个端点**：

| 模块 | 端点数 | 无任何鉴权注解 |
|------|:---:|:---:|
| `dms` | 313 | **0** |
| `erp-delivery-route` | 39 | **0** |
| `core-base/trade`（除 OpenApiController） | 33 | **0** |
| `core-base/trade` — **OpenApiController** | 12 | **12**（见 §3.2） |
| `erp-sales` — SaleLogisticsController | 12 | **0** |

> **口径修正说明**：本脚本首轮把 `ChannelController` 的 12 个端点误报为「无鉴权」——该文件的注解风格是 `@PostMapping` 在**上**、`@SaCheckPermission` 在**下**，而脚本只向上扫描。修正为「向上 + 向下至方法签名」双向后，误报归零。**该控制器实际上每个端点都有 `@SaCheckPermission`。**

**权限码对账（全仓）**：代码引用 1715 个，库中 1836 个，**全仓仅 2 个缺失**（`crm:contract:refresh`、`set:menu-config:view|update`），**配送域 146 个权限码零缺失**。

**粒度**：配送域权限码到 `:list/:view/:detail/:create/:update/:delete/:export/:approve/:audit/:execute/:cancel/:complete/:sign/:assign/:reassign/:auto/:config/:fence/:status/:report/:submit/:retry/:clear` —— **粒度符合行业标准**（CRUD + 业务动作分离）。

**唯一的刻意例外**：`POST /api/dms/channel/callback` 用 `@SaIgnore` 免登录，且在 `SaTokenConfig` 白名单中，安全由 **HMAC 验签 + 时间戳容差 + nonce 防重放**保证（代码注释与《渠道管理开发文档》§3.4 一致）—— 这是**正确设计**，不是缺口。

### 4.6 行级数据权限

按既有记录（`permission-layering-gap-audit`），`DataPermissionInterceptor` 已挂入插件链、`@DataScope` 与表级自动模式生效。本轮未发现配送域新增缺口。`配送仪表盘开发文档` §7 自述「角色化视图（接入 `DataScope` 行级权限）待完善」，属**已知未做**。

---

## 5. 数据库

### 5.1 列数超开发规范（10 张）

规范（《开发技术规范》）：**主表 ≤ 25 列，明细表 ≤ 35 列**。

| 表 | 列数 | 类型 |
|---|:---:|---|
| `dms_task` | **61** | 主表（超 36 列） |
| `dms_vehicle` | 45 | 主表 |
| `dms_rider` | 43 | 主表 |
| `erp_delivery_route` | 40 | 主表 |
| `dms_vehicle_inspection` | 36 | 主表（超 11 列） |
| `dms_payment` | 33 | 主表 |
| `dms_sign` | 30 | 主表 |
| `dms_verification_alert` | 27 | 主表 |
| `dms_settlement` / `dms_settlement_rule` | 26 | 主表 |
| `dms_rider_verification` / `dms_vehicle_energy_log` / `dms_rider_vehicle_binding` | 25 | 主表（边界） |

> 判定说明：`dms_payment` 33 列**文档已自述**「超出《开发技术规范》『单表 ≤ 25 列』上限——属历史遗留」。`dms_task` 61 列是本次最突出的宽表（含大量执行态/金额/坐标/审计列），建议登记技术债拆分（如把坐标与执行态拆到 `dms_task_exec`），**不建议本轮动**。

### 5.2 表冗余与双轨

| 主题 | 并存实现 | 行数 | 判定 |
|------|---------|:---:|---|
| 线路 | `erp_route` + `erp_route_area`（**档案**） | 38 / 0 | **活**：`RouteMasterController` 维护；`erp_route_area`（配送区域子表）**零数据** |
| 线路 | `erp_delivery_route` + `erp_route_point`（**执行单**） | 147 / 221 | **活**：`delivery:route:*` 维护，与档案严格区分 ✅ |
| 渠道 | `dms_channel`（配送自有渠道档案） | 30 | **活** ✅ |
| 渠道 | `external_channel_config`（core-base 交易域渠道配置） | — | **活**：`ChannelConfigController` 维护，属「交易→外部平台」菜单（非配送菜单） |
| 结算 | `dms_settlement` / `_item` / `_rule`（配送结算） | 0 / 0 / 0 | **活**（代码齐备，未跑通） |
| 收款 | `dms_payment` / `_flow` / `_collection` | 1 / 0 / 0 | **活** |
| 调度审计 | `dms_task_log`（633 行） | 633 | **活** ✅ |
| 调度审计 | `dms_dispatch_record` | 0 | **僵尸表**（§6.1） |
| 事件 | `dms_event_outbox` | 295 | **活**（写入方）但**无消费方**（§6.2） |

> **注意**：`dms_channel`（配送）与 `external_channel_config`（交易）是**两个域的渠道档案**，不是重复实现 —— 前者是「配送运力渠道」（达达/美团/自有），后者是「外部平台对接配置」。二者当前**无同步关系**，存在口径分叉风险（渠道管理页写 `dms_channel`，交易域页面写 `external_channel_config`）。

### 5.3 数据现状提示（P2）

**14 张表零业务数据**：

```
dms_dispatch_record  dms_logistics_ship  dms_payment_collection  dms_payment_flow
dms_purchase_receive dms_return_receive  dms_settlement          dms_settlement_item
dms_settlement_rule  dms_ship_order      erp_freight_rule        erp_route_area
erp_shipment_notify  （erp_balance_log，非配送域）
```

其中 `dms_settlement*`（3 张）、`dms_payment_flow`、`erp_freight_rule`、`erp_shipment_notify` 属**代码与页面齐备、但从未真实跑通**；`dms_dispatch_record` 等 5 张为僵尸表。

> **结论：配送模块的「结算 / 对账 / 运费 / ASN / 收款流水」五条链路，在当前环境中从未被真实数据验证过。** 本轮所有「接线正确性」结论均为静态分析结论。

### 5.4 脏数据：`scheduled_task` 12 条演示数据

```sql
SELECT count(*) FROM scheduled_task WHERE job_key IS NULL;   -- 12（两批重复的 6 条）
```

6 条为一组、**完整重复两遍**，「历史演示数据：未绑定 job_key，已停用」。真任务只有 5 条（配送 1 条 + 营销 4 条）。

---

## 6. 冗余与死代码

### 6.1 P1：5 张僵尸表（无实体 / 无 Mapper / 无 SQL / 零数据）

| 表 | 列数 | 建表来源 | 功能已被谁取代 |
|---|:---:|---|---|
| `dms_dispatch_record` | 16 | `V9.10.1__Add_Visit_CostSharing_Dispatch_Tables.sql` | `dms_task_log`（调度审计，633 行） |
| `dms_logistics_ship` | 15 | 同上 | 物流发货（复用《订单处理中心》拣货/发货） |
| `dms_purchase_receive` | 16 | 同上 | 采购订货收货（复用采购订单 doc-query） |
| `dms_return_receive` | 15 | 同上 | 物流退货收货（复用销售退货申请） |
| `dms_ship_order` | 16 | 同上 | 发货查询（复用销售出库单） |

**证据**：`tools/audit-dms-table-usage.py` 对每张表在 `backend/` 全量源码中检索 `@TableName("表名")` 与手写 SQL 的 `FROM/JOIN/INTO/UPDATE`，**五张表命中数均为 0**；直连 devdb 查 `count(*) = 0`。

**根因**：`V9.10.1`（早期批次）按「配送自建单据表」建模，后续实际走了**「复用存量单据」路线**（发货复用订单处理中心、收货复用销售/采购单据），这五张表被整套抛弃，但**从未删表、也未删迁移**。

**既有处置**：`TABLE_DUPLICATE_AUDIT.md` 第 212 行已登记该 5 张表，处置为「与 `CLEANUP_SCOPE` D-13 一致 —— 删表属**格式决策**，须先备份 + 观察 1–2 个发布周期，**本轮只登记不删**」。**本次审计确认该结论成立并在报告内固化证据。**

### 6.2 无消费方的后端能力（1 组）

| 端点 | 类 | 消费方 | 判定 |
|------|----|--------|------|
| `GET /dms/event/pending`、`POST /dms/event/{id}/retry` | `EventController` | 前端无调用；`dms_event_outbox` 295 行由 `EventService` 写入、`@Scheduled(fixedRate=10000)` 派发 | **人工运维端点**，保留合理 |

> `dms_event_outbox` 有 295 行数据且 `EventService` 每 10 秒派发 —— 需确认下游是否有消费方（本次未见配送域外的事件监听器除 `ApiMonitorAlertBridgeListener` 外）。

> **⚠️ 此处曾误报「配送员导入未接线」**：`/dms/rider/import-template`、`/dms/rider/import-excel` 一度被列为功能缺口，实为**脚本口径漏报** —— 《配送员管理》页（`views/dms/rider/index.vue:641-648`）**早已接线**，只是走通用组件 `BaseDataImportWizard`，其 URL 由 props 传入（组件内部才 `request.get(props.templateUrl)`），字面量正则抓不到。
> 已修正脚本口径（`tools/audit-dms-api-wiring.py` 增加组件属性 URL 提取）后重跑：未调用端点 **19 → 15**，四条导入接口（配送员 2 + 线路 2）确认均**已被消费**。**配送域无功能缺口。**

### 6.3 后端死代码：**0**

按「排除 Spring 容器管理类（`@RestController/@Service/@Component/@Mapper/@Configuration/@Entity/@TableName/@Scheduled` 等）后的引用计数」扫描 `dms` + `erp-delivery-route`：

```
目标模块类总数（非容器管理）: 154
完全无引用（死代码）: 0
仅被测试引用: 0
```

**这是本次系列审计中唯一一个后端死代码为 0 的模块**（对比：仓储 11 个文件、销售 4 个死文件）。

### 6.4 前端死代码：**0 孤儿页面**

配送域 55 个 `.vue` 全部能被 `componentMap` 或菜单或源码引用命中：

```
配送域前端页面总数: 55，总行数 47119
孤儿候选（无路由键/无菜单/无源码引用）: 0
```

**入口直达型（薄壳复用）设计验证通过**：

| 薄壳页面 | 行数 | 复用目标 |
|---------|:---:|---|
| `dispatch/logistics-ship/index` | 27 | `order-center/index`（1950 行） |
| `dispatch/return-receive/index` | 17 | `sales/return-apply/*`（1255+1331 行） |
| `dms/vehicle/usage/index` | 16 | `dms/verification/components/VerificationTabs`（2159 行） |
| `dms/verification/index` | 16 | 同上 |

符合《配送模块 README》§4「入口直达现象……必须复用目标视图组件，不得重复造页面」的要求 ✅

### 6.5 桩代码检查：**后端干净，司机端有原型桩**

- **后端 `dms` / `erp-delivery-route`**：`TODO/FIXME/模拟/未实现` 命中 15 处，**逐条核实全部是刻意设计**——`DeliveryAdapter` 及 4 个平台适配器（达达/美团/顺丰/社会车辆）统一「未对接该平台 → **如实失败，不返回模拟数据**」，这是金标准红线（「无桩、无模拟」），**不是缺陷** ✅
- **前端 PC 端**：仅 3 处解释性注释（`dashboard`「不使用占位符」、`ship-query`「用不存在单号占位保证无数据」、`api-monitor`「无桩无模拟」），**无实际桩** ✅
- **司机端**：`api/index.ts` 25 个端点全为原型契约 + 3 处调用不存在的方法（§3.1）❌
- **`OpenApiController`**：3 处 TODO 桩（§3.2）❌

---

## 7. 与其他模块的关系

### 7.1 正确接通的跨模块关系（验证通过）

| 关系 | 实现方式 | 结论 |
|------|---------|------|
| 配送 ↔ 库存 | `dms` 模块内 `grep 'erp_stock\|wms_inventory'` **零命中** | ✅ **严格不越界**（README §6 红线「配送不改库存」） |
| 配送 → 财务 | `SettlementService` 直接注入 `erp-finance` 的 `BusinessAccountingService` / `PayableService` / `VoucherItemMapper`，推送生成凭证 + 外部运力应付，**真正的记账** | ✅ 非 outbox 空壳 |
| 配送 → 财务（收款） | `PaymentService` 推财务幂等（`finance_push_status` / `finance_trace_id` 列） | ✅ |
| 资料 → 配送 | 线路档案 `erp_route` 由 `erp-delivery-route` 的 `RouteMasterController` 维护，配送侧**只读引用**（`RouteLookupMapper`、`RouteServiceImpl`） | ✅ |
| 司机端 ↔ 配送 | `/dms/task/page`、`/dms/sign/submit`、`/dms/payment/confirm`、`/dms/tracking/report`、`/dms/verification/me/rider` 等 | ✅ **仅配送/路线/签收/收款域**（订单/地图/我的未接，§3.1） |
| 仓储/销售 → 配送 | `OutboundDeliveryFilterController` 按配送状态/线路**反查**销售出库单号，供《发货查询》固定项过滤 | ✅ 反向依赖，设计合理 |
| **营销 ← 配送** | `erp-marketing` 的 `CommissionAnalyticsServiceImpl` 读 `dms_task` / `dms_task_doc`（提成矩阵、业绩概览/明细的行源） | ✅ **README 未记录的跨模块依赖**，建议补入 §6 联动表 |

### 7.2 P2：车辆档案的载重/容积未接入派单约束（口径分裂）

**两套并存的口径**：

```java
// ① 调度侧实际使用：全局配置上限（dms_config）
// DispatchService.java:80-86
CFG_MAX_LOAD_KG   = "dms.dispatch.max.load.kg";
CFG_MAX_VOLUME_M3 = "dms.dispatch.max.volume.m3";
// :404-416  assertAssignable 内按「任务总重量/体积 vs 配置上限」硬门控 ✅

// ② 车辆档案的车型维度字段：完全未参与派单
// DispatchService 内 grep 'ratedLoad|cargoVolume' → 命中 0
```

**《车辆管理开发文档》§5 第 7 项已自述**：「载重/容积接入派单约束 —— 🛠 待接：`ratedLoad`/`cargoVolume` 已入库并随 `options` 返回，**智能调度侧尚未读取做超限校验**」，本次审计**确认属实**。

> 注意区分：`README` §1.1 说的「负载硬门控（并接上限·载重·容积）」指的是 **①配置上限**（已实现，E2E 175/175 验证过）；**②车型档案载重**确实未接。两者都对，但**容易互相掩盖**，建议在调度文档中显式区分。

### 7.3 P2：三条「能力已建、驱动未接」的链路（文档已自述，本次确认）

| 链路 | 现状 | 证据 |
|------|------|------|
| **订单池过期扫描** | `POST /dms/order-pool/expire-scan` 端点存在，**无 `@Scheduled`、无 job_key** | `@Scheduled` 在 dms 模块仅 5 处（事件外发/合规清理/SSE/核验×2），无订单池 |
| **结算周期自动生成** | `dms_settlement_rule.settle_cycle` 字段已落库，**无定时任务消费** | `scheduled_task` 表无对应 job_key |
| **配送员绩效回写** | `dms_rider.total_orders / today_orders / punctual_rate / rating_score` **只在排序中读取，无任何写入方** | `grep` 命中仅 `RiderService.java:165-167`（orderBy） |
| **超时升级定时** | `DmsDispatchEscalateJob` 已实现（`job_key = dms.dispatch.escalateOverdue`，Cron `0 */10 * * * ?`），**默认 `enabled=0`** | 设计如此（安全闸门：默认停用 + Redis 锁 + dryRun）✅ |

> 另：`调度任务开发文档` §配置落位提到「定时调度：`xxl.job.*`（⚠️ 未在 core-api yml 声明，需补）」—— 本次核实实际使用的是 `scheduled_task` 表 + `JobHandler` SPI 方案（**非** xxl-job），该文档注记为**过时**。

### 7.4 配送任务的创建入口：**仅人工建单**

```java
// 全仓搜索「创建 dms_task」的唯一入口
TaskController.java:282   return ApiResponse.ok(taskService.create(task));
```

`erp-sales` 模块内 `grep 'DmsTask|dms_task'` 仅命中 1 处**注释**（`SaleOutboundQueryDTO.java:107`），无任何创建逻辑。

**即：销售出库单不会自动生成配送任务**，配送单与上游单据是**人工关联**关系（`dms_task_doc`，`doc_type`：1 销售出库单 / 2 销售退货单 / 3 调拨单），选中后由上游聚合表头数量/金额/重量/体积。

> **文档口径提示**：`配送模块DMS开发文档` §6 的「销售 → 配送：销售出库单 `XSCKD-` → 配送任务」**易被读成自动链路**。建议回写为「人工在《配送单》选择来源上游单据（`dms_task_doc`）」以免后续误解。**这一点也可能是设计缺口**（若业务期望自动生成，则属 P1 未实现），需拍板。

---

## 8. 规范性

### 8.1 后端（良好）

- **注释**：`DispatchService`、`ChannelController`、`MenuPermissionDeriver`、`OpenApiController` 等均有详尽中文注释，含设计决策与历史背景（如 `DispatchService:128` 明确引用开发文档 §3.6 工程约束）；
- **鉴权**：dms 模块 313 端点 **100% 有注解**，是本次系列审计中最高；
- **控制器粒度**：5–29 端点/控制器，仅 `VerificationController`(29) / `PaymentController`(25) / `TaskController`(23) 偏大，未见上帝类；
- **异常处理**：统一 `DmsBusinessException` + `DmsGlobalExceptionHandler`；
- **接口口径**：`ApiResponse` 统一包装；分页统一 `Page<T>`。

### 8.2 前端（规范性偏离）

| 目录 | `: any` / `as any` 处数 |
|------|:---:|
| `views/dms` | **728** |
| `views/dispatch` | **235** |
| `api/dms` | 28 |
| `views/trade/api-monitor` | 21 |
| `views/md/route` | 13 |
| **合计** | **1025** |

- **页面组件复用**：统一使用 `PageContainer` + `CategoryListLayout` + `BillTableList` / `BillDetailTable` / `PageConfigPanel`，**复用度良好** ✅
- **导出/打印**：导出走 `responseType: blob` 且**校验返回是否为 JSON 错误体**（`tracking/index.vue:697`、`sign/index.vue:863` 先判 `application/json` 再建 blob）——实现质量高于一般水平 ✅；10 个页面接入 `PrintDialog` / F8 ✅
- **权限指令**：**0 处**（§4.3）❌
- **`dms/shared.ts`** 提供模块内共享逻辑，符合「下沉公共能力」要求 ✅

---

## 9. 开发文档对照（28 篇）

> 方法：派子代理逐篇提取「菜单配置 / 接口路径 / 单号前缀 / 表名 / 文档自标注未完成项 / 自述缺陷」，再与代码与数据库事实逐条对照。

### 9.1 菜单配置一致性：**核心一致**

| 文档 | 记载 | 数据库实际 | 判定 |
|------|------|-----------|------|
| 配送单 | `80760`，`list_path=dispatch/dispatch-order/index`，双入口 | 完全一致 | ✅ |
| 车辆管理 | `80770`，`path=dms/vehicle`，`list_path=dms/vehicle/form`，`tag=添加` | 完全一致 | ✅ |
| 配送员管理 | `80790`，`path=dms/rider`，`list_path=dms/rider/form`，`tag=添加` | 完全一致 | ✅ |
| 线路 | `70530`，`md:route`，`md/route`，`display_mode=0` | 完全一致 | ✅ |
| 其余 23 篇 | 仅写各自菜单ID（或只写 ql361 的 menuId） | 菜单ID 全部一致 | ✅ |

### 9.2 接口路径一致性：**抽样 100% 存在**

逐条核验的完整路径全部在后端存在：配送查询（`/task/page`、`/{id}`、`/{id}/print`、`/{id}/cancel`、`/export`、`/filter-options`）、配送单（`/task/{next-no,page,page-detail,save,export,audit,unaudit,print}`）、配送路线单（22 条 `/api/delivery/route/*`）、订单池（14 条）、渠道管理（14 条）、签收（9 条）、收款（25 条）、结算（18 条）、智能调度（15 条）、路线规划（围栏 8 条 + LBS 9 条）、配送跟踪（8 条）、实时跟踪（9 条）、API监控（13 条 + inventory-sync 4 条）、车辆/维保/配送员/参数/仪表盘 —— **无一条失效**。

> 与 §3.3 的自动化比对结论（前端调了后端没有 = 0）互相印证。

### 9.3 单号前缀一致性：**7/7 一致**

| 业务 | 文档 | 代码 | 判定 |
|------|------|------|------|
| 配送单 | `PSD-YYYYMMDD-序号` | ✅ | ✅ |
| 配送路线单 | `PSXL-YYYYMMDD-4位序号` | ✅ | ✅ |
| 结算单 | `JSD-YYYYMMDD-序号` | `SettlementService.java:676-678` | ✅ |
| 线路档案 | `XL` + 3 位（`XL001`） | ✅ | ✅ |
| 车辆编码 | `VH` + 3 位 | ✅ | ✅ |
| 配送员编号 | `PSY` + 4 位 | ✅ | ✅ |
| 维保工单 | `WBD` + 4 位 | ✅ | ✅ |

### 9.4 文档标注「待完善」但**代码已实现**（文档滞后，需回写）

| # | 文档标注 | 代码事实 |
|---|---------|---------|
| 1 | `配送模块DMS开发文档` §7 P0-1：《API监控》删除两张硬编码假数据卡片（1234 次 / 98.5%） | 已改后端真实聚合（8 KPI 含 P95），E2E 119/119 |
| 2 | 同上 P0-2：《配送单》列表 `apiUrl` 与后端不匹配 → 打通 | 已打通（§3.3 双向比对 0 断点） |
| 3 | 同上 P0-3：《线路列表》《调度任务》《配送查询》查询条件手输 ID 改选择器 | 三页均已用选择器 |
| 4 | 同上 P2-13：`人车管理` 补菜单入口 | `V11.168.0` 已补 5 个子菜单（§2.3） |
| 5 | 同上 P2-13：`线路列表` 更名「配送路线单」 | `V11.168.0` 已更名 |
| 6 | `线路开发文档`：`views/dms/route/index.vue` 无菜单挂载 → 不可达死页 | `V11.169.0` 已挂 `80830 路线规划`，**该结论已失效** |
| 7 | `配送单开发文档` §5.3：编号为前端时间戳 | 已接号段 `GET /api/dms/task/next-no` |
| 8 | `配送配置开发文档` §5：与《配送参数》重复 | ✅ 已按「只读总览 vs 唯一可写」分工落地 |

### 9.5 文档标注「待完善」且**代码确实未实现**（真实缺口）

| # | 文档 | 缺口 | 级别 |
|---|------|------|------|
| 1 | `签收管理开发文档` §7.5 | 司机端登录打不存在的 `/api/v1/delivery/auth/login`；「订单」Tab 仍原型假数据 | **P0**（§3.1，本次已扩展到 3 个 Tab + 登录页） |
| 2 | `车辆管理开发文档` §5-7 | `ratedLoad`/`cargoVolume` 未接入派单约束 | **P2**（§7.2，已确认） |
| 3 | `配送员管理开发文档` §5.1-2 | 绩效回写（`total_orders` 等）无写入方 | P2（§7.3，已确认） |
| 4 | `订单池开发文档` §4.3-2 | 定时过期未加 `@Scheduled` | P2（§7.3，已确认） |
| 5 | `配送结算开发文档` §6.7-3 | 结算周期自动调度未接定时任务 | P2（§7.3，已确认） |
| 6 | `渠道管理开发文档` §7-1 | 4 个外部平台适配器未真实对接 | 设计如此（如实失败，不伪造） |
| 7 | `配送路线单开发文档` §8-2/3 | 打印未接 `PrintDialog`；短信通道 `sms.enabled=false` | P2（能力已闭环，环境未开通） |
| 8 | `实时跟踪开发文档` §6-1 | 司机端上报策略（5–10s/位移 50m）与 Redis 最新位置缓存未接 | P2 |
| 9 | `收款管理开发文档` §6.9-4 | 司机端收款未采集 `actual_quantity`/备注 | P2（与 §3.1 同源） |
| 10 | `配送路线单开发文档` §8-4 | 与配送任务（`DmsTask`）未合并 | 设计选择 |
| 11 | `配送跟踪开发文档`（遗留） | 逆地理编码待接 `dms/route` | P2 |
| 12 | `配送仪表盘开发文档` §7-4 | 告警下钻的 query 参数页面未消费 | P2 |

### 9.6 文档自述缺陷的复核：**均已修复**

`车辆管理`（6 处接口不匹配）、`配送员管理`（枚举口径冲突/假删除）、`订单池`（二轮 8 处）、`物流发货`（5 处）、`采购订货收货`（6 处）、`路线规划`（5 处）、`智能调度`（4 处）、`渠道管理`（6 处）、`签收管理`（5 处司机端契约）、`发货查询`（4 处）、`物流退货收货`（行键重复）—— 逐条抽样核验，**文档记录的修复状态与代码一致**。

### 9.7 文档失实/过时（3 处 + 1 处口径提示）

见 §2.3（菜单项数、人车管理、线路孤儿页）与 §7.4（销售→配送链路表述）。

---

## 10. 修复建议与优先级

### 10.1 建议立即处理（P0）

| # | 动作 | 涉及文件 | 风险 |
|---|------|---------|------|
| 1 | ~~给业务角色补配送权限授权~~ ✅ **已完成**（§11.2）；**剩余**：统一 14 条菜单码与权限码命名体系（§4.2） | `sys_menu.menu_code` | 改系统配置数据，会连带改变菜单可见性，**需单独回归** |
| 2 | 司机端：登录页与「订单/地图/我的」三 Tab —— 二选一：**(a) 按 `api/dms.ts` 真实契约接线**；**(b) 暂时下架这些 Tab 与登录页**，只保留已通的「配送/路线」 | `apps/driver-delivery/src/{api,views,App.vue,router}` | 中（涉及产品决策：司机端是否本轮交付完整） |
| 3 | `OpenApiController`：下线（推荐）或补 `@SaIgnore` + 真实签名校验 + 白名单；同时清掉 3 处 TODO 桩 | `core-base/.../trade/controller/OpenApiController.java` | 中（需确认是否已有外部调用方） |
| 4 | 配送域补 `v-permission` 按钮级权限（对齐全站 380 处的既定做法） | `views/{dms,dispatch}/**` 关键按钮 | 低-中（前端改动，需先定权限码映射） |

### 10.2 建议本轮收尾处理（P1）

| # | 动作 | 说明 |
|---|------|------|
| ~~5~~ | ~~配送员管理页接线导入~~ | ❌ **本条为误报，已排除**（页面早已接线，见 §6.2） |
| 6 | ~~回写失实文档~~ | ✅ **已完成**（§11.3，5 处） |
| 7 | ~~修 `60401 配送业务` 下 3 个叶子的 `sort` 并列~~ | ✅ **已完成**（§11.2） |
| 8 | 清理 `scheduled_task` 12 条垃圾演示数据 | 需备份，待执行 |

### 10.3 建议登记为技术债（P2）

9. 5 张僵尸表的删除（**须先备份 + 观察 1–2 个发布周期**，既有 `TABLE_DUPLICATE_AUDIT` 已定此处置）
10. `dms_task`(61 列) 等 10 张超规范宽表的拆分
11. 车辆档案载重/容积接入派单约束（§7.2）
12. 订单池过期扫描 / 结算周期生成接入定时任务（§7.3）
13. 配送员绩效字段接入回写（§7.3）
14. 司机端位置上报策略与 Redis 最新位置缓存（`实时跟踪` 文档 §6-1）
15. 前端 `any` 类型治理，优先 `views/dms`（728 处）
16. 补一轮真实单据运行验证（结算/对账/运费/ASN 五链路 0 数据）
17. 明确 `dms_channel`（配送运力渠道）与 `external_channel_config`（交易对接配置）的分工与是否需要同步

---

## 11. 本次已执行的修复（2026-09-23）

### 11.1 原则

只做**零争议、可静态验证**的动作。凡涉及**代码删除、数据库改动、菜单/权限数据、产品决策**的，一律保留在 §10 待拍板，本轮**未动**。

### 11.2 已执行：数据库修复（**仅配送域**，执行前已备份）

| # | 动作 | 影响行数 | 验证 |
|---|------|:---:|------|
| 1 | **20 条配送自建菜单 `menu_level` 3→0**（§4.6）—— 新迁移 `V11.501.0__Fix_Dms_Menu_Level_And_Sort.sql` + 同一份 SQL 在 devdb 手工执行（`WHERE menu_level = 3` 幂等） | 20 | 回查配送 38 条菜单 **全部 `menu_level=0`**；`tools/audit-dms-menu.py` 复跑 **0 回归** |
| 2 | **`60401 配送业务` 下 3 个叶子 `sort` 并列** → 1 / 2 / 3 | 2 | 菜单审计 §⑤「同目录 sort 并列」输出**已为空** |
| 3 | **配送自有域权限授权**（`tools/grant-dms-permissions.py`，矩阵与仓储 `grant-storage-permissions.py` 一致） | +194 | 系统管理员 **126** 条（域内全部）、部门管理员 **68** 条（只读 `:list/:view/:detail/:query/:export`）；超管仍 126 |

**备份产物**（执行前生成，可完整回滚）：
- `tool-results/menu_backup_before_dms_fix_20260923.json`（39 行菜单）
- `tool-results/role_perm_backup_before_dms_grant_20260923.json`（受影响角色 321 行授权）

#### 11.2.1 20 条菜单数据查重（2026-09-26）

对 20 条逐项核验，**全部干净**：

| 检查项 | 结果 |
|--------|------|
| 20 条内部 `menu_code` / `route_name` / `path` / `component` 重复 | **无** |
| 与全库其他菜单的 `menu_code` / `path` 冲突 | **无** |
| `path` 在 `dynamicRoutes.ts` 中另有静态注册（会互相覆盖） | **无** |
| 字段自洽 | `menu_level=0`、`tenant_id=0`、`client_type=tenant-admin`、`visible=1`、`status=1` 全部正常 |

> `route_name` **全部为空** ⇒ 前端路由名回落到 `menu_code`（规则 `route_name || menu_code`）。因 20 条 `menu_code` 互不重复、且与全库无冲突，**不存在 `erp-menu-code-route-collision` 那类静默 404**。
> （另：全库 `menu_code` 重复仍有 3 组 —— `purchase:analytics` / `purchase:order` / `stock:replenishment`，**均不在配送域**，由 `V11.499.0` 以补 `route_name` 的方式处理。）

#### 11.2.2 运行期验证（真机三账号对照）

脚本 `tools/audit-dms-menu-runtime.cjs`（只读）：

| 账号 | 可见「配送自建页」 | 说明 |
|------|:---:|------|
| `admin`（系统租户 + 超管） | **20/20** | 双豁免走 `:259` 早退分支取全量 —— **本地永远看不出问题** |
| `e2e_hr_ta`（系统租户 + SYSTEM_ADMIN） | **20/20** | `isSystemTenant=true` ⇒ 不加 `menuLevel` 过滤 |
| **`e2e_hr_t2`（租户2 + E2E_T2_ADMIN）** | **7/20** | ★关键：**修复前为 0/20** |

**修复已生效。** `e2e_hr_t2` 能看到的 7 条（`80760/80700/80830/80840/80730/80740/80750`）正是 §4.2 里「`menu_code` 在权限码表找不到对应」的 **fail-open** 那一批；其余 13 条按权限码派生规则**正确隐藏**（该账号只持有 4 个权限码，且 `E2E_T2_ADMIN` 刻意未授权 —— 见 §11.2「刻意未做②」）。**两级过滤（menu_level 硬过滤 + 权限码派生）均按设计工作。**

#### 11.2.3 代码修复：平台级权限码对租户不可见（**已改 + 编译通过，待构建生效**）

**这是「租户自助配权限」链路的最后一环，也是本次追加排查的真根因。**

```java
// core-base SysPermissionServiceImpl —— 修复前
private List<SysPermission> listAllPermissions(Long tenantId) {
    wrapper.eq(SysPermission::getTenantId, tenantId)   // ← 只查精确 tenantId
}
```

平台定义的全部权限码（含 `dms:*` / `delivery:*`，共 **1464 条**）`tenant_id` 恒为 **0**，
而租户管理员传 `tenantId=2` ⇒ **可分配清单恒为空** ⇒ 即使已把 `tenant-admin:role:assign-permission`
授给它（§11.2 已完成），打开「角色 → 分配权限」仍是**空的**，配不了任何权限。

**修法**：`listAllPermissions` 与 `getChildrenPermissions` 改为「平台级(0) + 本租户」。
（`sys_permission` 已登记在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`（"权限定义系统级"），
故该 `eq` 是唯一过滤条件，改 `in` 即生效。**只增不减**，`tenantId=0` 时行为不变。）

| 视角 | 修复前 | 修复后 |
|------|:---:|:---:|
| 租户 0（平台/超管） | 1464 | 1464（**不变**） |
| 租户 1 | 356 | 1820 |
| **租户 2** | **0** | **1464**（含 `dms:` / `delivery:` 94 个） |

**验证状态：✅ 已真机验证通过（2026-09-27，5655 新构建实例）**

`tools/audit-dms-tenant-flow.cjs` —— **两步授权链路的端到端闭环**（用租户管理员账号，非超管）：

| 步骤 | 结果 |
|------|------|
| ① 赋权前 `GET /dms/vehicle/page` | **403** |
| ② `GET /role/{id}/permissions` | **200**（租户管理员可读本租户角色权限） |
| ③ `GET /permission/tree?tenantId=2` | **200，1465 个权限码**（含 `dms:` 80 个）← **修复前为 0** |
| ④ `POST /role/{id}/permissions`（自助赋权） | **200** |
| ⑤ 重登后 `GET /dms/vehicle/page` | **200**（**403 消除**） |

**⇒「平台开模块 → 租户管理员自助配权限 → 模块接口可用」整条设计链路打通。**

同期接口探测（`tools/audit-dms-menu-runtime.cjs`）：**超管 20/20 接口全部 200**，
此前 13 个因 fat jar 被替换导致的 500 **全部消失**；系统管理员 19/20（唯一 403 是交易域码的 API监控，见 §11.2「刻意未做②」）。

> ⚠️ **踩坑记录（2026-09-26/27）**
> 1. **首次验证打到了错误的实例**：5655 短暂下线期间，验证脚本误指向 5671 —— 那是一个
>    23:14 启动、跑 `core-api-verify-run.jar` 旧副本的实例 ⇒ `/permission/tree` 仍返回 0，
>    一度误判为「改动没生效」。**判据**：先确认目标端口由**哪个 PID** 监听、该 PID 的
>    **启动时间**与 **jar 构建时间**的先后（`netstat -ano` + `Get-CimInstance Win32_Process`）。
> 2. **验证脚本自身的雪花 ID 精度坑**：`const T2_ROLE_ID = 2099000000000009031` 写成数字字面量，
>    超过 `Number.MAX_SAFE_INTEGER` 被舍入为 `2099000000000009000` ⇒ 打错角色 ⇒ 500。
>    已改字符串；权限 id 也统一 `String()` 比对。*（与 `js-bigint-precision-fix` 记忆同源。）*
> 3. **Flyway 并发冲突**：起第二实例时与另一会话的实例同时执行 `V11.512.0`，双方都用
>    `MAX(id)+row_number()` 算主键 ⇒ 撞 `sys_role_permission_pkey`，我的实例启动失败。
>    与本次改动无关（该迁移随后成功）；`flyway_schema_history` 失败记录 = 0。
>    ⇒ 新坑已记入记忆 `flyway-concurrent-migration-conflict`：**本仓同一时刻只应有一个实例跑 Flyway**。
> 4. 观察：`V11.504/509/510/512/513/514/515` 是**其他会话并行做同类的「menuLevel + 角色授权」修复**；
>    经核对**未覆盖配送、也未与本次授权重复**（`tenant-admin` 授权为净新增 11 条）。

> ⚠️ **该轮接口探测中的 13 个 500 是环境问题，不是配送代码缺陷**（**2026-09-27 已随新构建恢复：超管 20/20 全 200**）——
> 错误日志为 `NoClassDefFoundError: cn/aiedge/dms/...` +
> `Zip 'Central Directory File Header Record' not found at position 1197092`；
> fat jar mtime `20:29:48` **晚于** 进程启动 `17:00:31`（报错发生在 20:43）。
> 即**运行中的 fat jar 被替换**（`fatjar-swap-while-running` 坑），**重启后端即愈**，与本次改动无关。7 个正常返回 200 的接口（路线单/车辆/智能调度/签收/渠道/结算/API监控）可反证代码本身无系统性缺陷。

**刻意未做 ①：`menu_code` 改名（14 条）**　理由见 §10.1-1 —— `menu_code` 参与菜单可见性派生，改名会让非超管从「fail-open 可见」变成「不可见」，必须与「先授权（已完成）、后改码、再验证」的顺序绑定，并单独回归。且 `V11.499.0` 已明确记录过「刻意不改 `menu_code`」的同类理由。

**刻意未做 ②：`trade:%` 的 20 个权限码授权**　属交易模块审计范围（避免与并行会话在同一批数据上冲突）。**受此影响：《API监控》页（90107，挂在配送菜单下）对非超管仍不可用** —— 待交易模块处置后一并解决。

#### 11.2.4 第 3~5 项：按用户拍板执行（2026-09-27）

**① 僵尸表 + 脏数据：直接删除**（用户拍板）

- 迁移 `V11.517.0__Drop_Dms_Zombie_Tables_And_Clean_Scheduled_Task.sql`
  + devdb 手工执行：**DROP 5 张表**（`dms_dispatch_record` / `dms_logistics_ship` /
  `dms_purchase_receive` / `dms_return_receive` / `dms_ship_order`，均 0 行）
- `scheduled_task` **删除 12 行** `job_key` 为空的演示数据（剩 4 条真任务）
- 备份：`tool-results/scheduled_task_backup_before_clean_20260927.json`

**② `menu_code` 对齐（方案 B：只改 7 条纯配送自有）**（用户拍板）

- 迁移 `V11.518.0__Align_Dms_Menu_Codes_With_Permission_Prefixes.sql` + devdb 手工执行

| 菜单 | `menu_code` 改为 | 同时补 `route_name` |
|---|---|---|
| 80730 调度任务 | `dms:task` | `DmsDispatchTask` |
| 80740 实时跟踪 | `dms:tracking` | `DmsRealtimeTracking` |
| 80750 配送参数 | `dms:config` | `DmsConfigParams` |
| 80830 路线规划 | `dms:route` | `DmsRoutePlan` |
| 80700 配送路线单 | `delivery:route` | `DmsRouteList` |
| 80840 用车管理 | `dms:vehicle-energy`（双前缀选主） | `DmsVehicleUsage` |
| 70530 线路 | `md:route-master` | `MdRouteMaster` |
| 80870 配送跟踪（同码伙伴） | 不变 | `DmsTracking` |
| 80890 配送配置（同码伙伴） | 不变 | `DmsConfig` |

⚠️ 必须给 80870/80890 一并补 `route_name`：改名后与 80740/80750 **同 `menu_code`**，
前端路由名 `route_name || menu_code` 会撞名、后者覆盖前者（`erp-menu-code-route-collision`）。

**效果**：7 条**全部派生命中**；配送域 27 个叶子的派生命中数 **13 → 20**；菜单连通性 **0 回归**。
可见性变化（按派生规则模拟）：超管 27/27、系统/部门管理员 26/27、
**租户2管理员 7 → 8/27**（那 6 条从「fail-open 白看」变为「需租户超管配权限才可见」——正是本方案目的）。
5 条跨域复用页 + 70150/80760 保持 fail-open 不动（方案 B）。
备份：`tool-results/menu_backup_before_code_align_20260927.json`

**③ `OpenApiController` 补齐（方案 B）**（用户拍板）

| 改动 | 文件 |
|---|---|
| 新增 HMAC-SHA256 验签拦截器（`X-Api-Key`/`X-Timestamp`/`X-Sign` + 时间戳容差 + 恒定时间比较；`/api/open/health` 放行） | `core-base/.../trade/open/OpenApiAuthInterceptor.java`（新） |
| 注册拦截器（`order=50`，在 Sa-Token 之后、埋点之前） | `core-base/.../trade/open/OpenApiWebConfig.java`（新） |
| **两处**白名单加 `/api/open/**` | `SaTokenConfig.java` |
| 按 `appId` 跨租户查密钥（`@InterceptorIgnore`，无租户上下文时租户插件会让查询恒空） | `ExternalChannelConfigMapper.selectSecretByAppId`（新） |
| 3 个 TODO 桩 → **如实失败**（`Result.fail(501, …)`），不再 `return success()` 假装成功 | `OpenApiController` 的 `order/status`、`inventory/lock`、`inventory/release` |
| 类注释订正：真实鉴权口径 + `signature` 参数已由 `X-Sign` 取代 | `OpenApiController` |

**收益**：堵住「任何登录用户可无权限码调用开放 API（写订单 / 查任意 SKU 库存）」的越权面；
外部平台从此**可调**（此前必然 401）。\n**当前仍不可用属运营配置**：`external_channel_config` 0 行 ⇒ 无 appId/secret ⇒ 一律 401 并给出可读原因（这是**正确**行为）。
编译：`BUILD SUCCESS`。**待下次构建 + 重启生效。**

### 11.3 已执行：5 处文档失实订正（纯事实订正，零代码影响）

| # | 文件 | 原文 | 订正 |
|---|------|------|------|
| 1 | `配送模块/README.md` §1.1 标题 | 「10 分组 **25 项**」（与表内实为 27 行不符） | 「10 分组 **27 项**」+ 附复审口径说明（38 条 = 1+10+27，另加 `70530`，合计 28 页） |
| 2 | `配送模块/配送模块DMS开发文档.md` §2 标题 | 「实际页面清单（sys_menu 实测 · **8 列 11 项**）」 | 「10 分组 **27 项**」+ 标注该节写于 `V11.168/169.0` 之前、**已过期**，并指向 README §1.1 |
| 3 | 同上 §2 附注 | 「`60502 人车管理` 无子菜单项，UI 不渲染（车辆/配送员页面当前**无菜单挂载**，属待补入口）」 | 标注 **已修复**：`V11.168.0` 补齐 5 个子菜单，全部可达（连通性 0 缺陷） |
| 4 | 同上 §6 跨模块联动表 | 「销售 → 配送：销售出库单 `XSCKD-` → 配送任务」 | 澄清为**人工关联**（`dms_task_doc`），并注明**无自动生成链路**（唯一创建入口是 `TaskController.create`） |
| 5 | `配送模块/线路开发文档.md` §跨模块检查 + §重复性研判 | 「`views/dms/route/index.vue` **无菜单挂载** → 不可达孤儿页，建议删除或补菜单」 | 标注 **已失效**：`V11.169.0` 已挂菜单 `80830 路线规划`，页面可达且为金标准页（E2E 186/186）；复审 **0 孤儿页面** |

> 选择订正而非删除原文，是为了保留文档演进痕迹（用 `~~删除线~~` + 并列订正说明），与仓储审计 §11 的做法一致。

### 11.4 已固化：8 个可复现审计脚本

| 脚本 | 作用 |
|------|------|
| `tools/audit-dms-{menu,frontend,backend,api-wiring,table-usage,deadcode}.py` | 六维盘点（菜单/前端/后端/接线/表消费方/死代码） |
| `tools/audit-dms-recheck.py` | **稳健性复核**：断链判定改用「全后端端点集」、鉴权注解窗口改用「max(上一映射, 上一签名)」口径重跑（见附录 §7-8） |
| `tools/audit-permission-codes.py`（既有） | 权限码 注解 ↔ 种子 对账 |

全部**只读**，可随时重跑复现本报告结论。

### 11.5 本轮**未**执行（需拍板）

| 项 | 原因 |
|---|------|
| 改 14 条 `menu_code` | 会**连带改变菜单可见性**（从 fail-open 可见 → 需持码才可见），须单独回归（§10.1-1、§11.2「刻意未做 ①」）——**权限授权本身已完成** |
| 司机端登录页与 3 个 Tab 的接线/下架 | **产品决策**：司机端本轮是否交付完整（§10.1-2） |
| `OpenApiController` 下线 / 补鉴权 | **并入交易模块 P0-3 处置**，勿两处各改一遍（§3.2 交叉引用） |
| 配送域补 `v-permission` | 需先定权限码 ↔ 按钮的映射（§10.1-4） |
| 配送员管理页接线导入 | 端点已就绪，属前端新增功能（§10.2-5） |
| 删除 5 张僵尸表 | 既有 `TABLE_DUPLICATE_AUDIT` 已定「备份 + 观察 1–2 个发布周期」；**本轮只登记**（§10.3-9） |
| 清 `scheduled_task` 12 条垃圾数据 | 改共享数据库，需先备份（§10.2-8） |
| 宽表拆分、绩效回写、定时任务补齐 | 存量技术债（§10.3） |

---

## 附：判定口径说明（防误判）

本报告在采集过程中**修正了 3 处自身误报**，记录在此以免后续复核踩同样的坑：

1. **「前端调了但后端没有」先在采集侧修口径，再定性**
   首轮脚本用 `request\.(get|post)\s*<[^>]*>\(\s*['"]` 提取调用，**泛型嵌套**（`request.get<Result<Record<string, any>>>('/x')`）会匹配失败 → 假报 15 条失效调用。改为「跳过可嵌套 `<...>` 后取第一个引号串」后，**失效调用归零**。同批次还把 `erp-sales` 加入后端扫描范围，消除了另一类假阳性。

2. **注解写在 `@XxxMapping` 下方时，只向上扫描会漏**
   首轮把 `ChannelController` 的 12 个端点误报为「无鉴权」。实际该文件风格是
   `@PostMapping` → `@SaCheckPermission` → 方法签名（注解在 mapping **之下**）。修正为「向上 12 行 + 向下至方法签名」双向扫描后误报归零。**Java 注解顺序不影响 Sa-Token 语义，功能是正常的** —— 若照首轮结论去「补」注解，就是在正常代码上做无用改动。

3. **`components/` 子组件不算孤儿**
   首轮用「全路径字符串是否被引用」判定，相对导入（`./components/X.vue`）不命中 → 误报 15 个孤儿组件。加入 basename 相对路径匹配后 **0 孤儿**。

4. **「无实体无 Mapper」才是僵尸表判据，`live=0` 不是**
   `dms_settlement*`、`dms_payment_flow`、`erp_freight_rule`、`erp_shipment_notify` 同样零数据，但它们**有实体、有 Mapper、有 Controller、有前端页面**，属「已建成未跑通」而非死表（§5.3）。只有 `dms_dispatch_record` 等 5 张**四者皆无**的才是僵尸表（§6.1）。

5. **「后端有端点、前端未调用」≠ 死接口**
   19 条中有 8 条是**司机端**消费（`apps/driver-delivery` 不在 `pc-admin` 扫描范围内）、1 条是**外部平台回调**、1 条是 **SSE**（走 `EventSource` 而非 `request.*`）、2 条走**通用导入组件**。逐条归因后无一条是真死接口（§3.3）。

6. **URL 由 props 传入的通用组件，字面量正则抓不到**
   `BaseDataImportWizard`（导入三步向导）的 `template-url` / `import-url` 是**组件属性**，组件内部才 `request.get(props.templateUrl)`。只扫 `request.*` 字面量会把**所有走该组件的导入接口**误报成「前端无消费方」——实测误报了配送员导入 2 条 + 线路导入 2 条，并一度被写成本报告的「功能缺口」。
   修正口径（提取组件属性 URL）后重跑：**未调用端点 19 → 15，配送域无功能缺口**。*（此坑与 `endpoint-scan-three-false-positives` 的「坑 2 后半」同类：调用池不补齐就没有资格下结论。）*

7. **断链判定必须用「全后端端点集」，不能用模块集**
   首轮 `audit-dms-api-wiring.py` 的后端扫描限定在 4 个模块目录 → **跨模块调用会被误报成断链**（实测误报 15 条 `/erp/sale/logistics/*`，实际由 `erp-sales` 的 `SaleLogisticsController` 提供）。
   按 `tools/audit-dms-recheck.py` 的正确口径（全后端 403 个控制器）重跑：**断链仍为 0**，结论稳健；同时量化了「只用模块集会误报 15 条假断链」。
   *（与 `endpoint-scan-three-false-positives` 记忆的「坑 2」同源。）*

8. **鉴权注解窗口：`back_start = max(上一个映射注解行, 上一个方法签名行)`**
   这里比「坑 1」更严格一层：只把窗口止于「上一个 `@XxxMapping`」**仍然不够** —— 当上一个方法把注解写在映射**之后**时，那段注解依旧落在窗口里、会漏给下一个方法。
   按正确口径重跑：dms 模块的裸端点**仍是 12 条且全部是 `OpenApiController`**，结论稳健。

9. **文档自述「未完成」需回代码验证，可能已过时**
   `配送模块DMS开发文档` §2 的「人车管理无子菜单」、`线路开发文档` 的「孤儿页」两条，**代码早已修复**（`V11.168/169.0`），文档未回写（§9.4）。反之 `车辆管理`/`配送员管理`/`订单池`/`结算` 自述的 4 条未完成项，**回代码验证后确实未做**（§9.5）。**两个方向都会错，必须逐条验。**
