# API文档开发文档

> **⚠️ 实施状态（2026-09-19）**：本页已按本文档实施并真机验证（脚本 `node tools/e2e-system.cjs`，67/67 通过）。**本文档 §9「实现差异说明」/ §12「剩余缺口」记录的是实施前的状态**；凡与《README.md》**§10「2026-09-19 实施结果」**冲突，一律以 README §10 为准。

> **文档类型：** 本系统独有页（ql361 无对标）→ 按 **OpenAPI Specification 3.1.0** 建模：本页是**平台研发支撑**里的「接口文档门户」，`client_type = system-admin`
> **本系统路由：** `admin/dev/api-doc`（**单入口**，无表单页、无 `list_path`）
> **菜单：** ID `62403`｜`menu_code = system:dev:api-doc`｜`client_type = system-admin`｜`path = admin/dev/api-doc`｜`component = views/admin/dev/api-doc`｜父菜单 `61305 开发工具`（上溯 `60013 系统`）｜`sort = 300`
> **对标页：** **ql361 无对标页（系统模块是平台级，ql361 是租户级产品）** —— 详 §2、§8
> **数据来源：** 本系统源码逐处核对（2026-09-18）＋ devdb 实测（`sys_menu` / `information_schema.tables`，2026-09-18 直连 psql）＋ 运行态只读探测（后端 :5655，2026-09-18）
> **通用规范：** 《ql361对标/对标开发技术参考文档》第 4、5 章；布局金标准见《交易模块/_开发指南-金标准.md》；模块全景见《系统模块/README.md》§1 三条红线、§3.4 **P0-13**、§7.1 第 13 条

---

## 1. 页面概述

API文档页是**平台研发支撑里的「接口文档门户」**：一张 `a-card` 里放一段静态说明（「API 文档已集成」）+ 两个跳转按钮（Swagger UI / Knife4j）+ 一张 4 列「模块清单」表格。它是 `61305 开发工具` 组的第 2 页（`sort=300`）。

**本页当前是整页桩**：表格 4 列的内容 **100% 由前端写死**（`api-doc/index.vue:146-153`，6 条固定记录），唯一调用的 `GET /api/monitor/info` **后端不存在**（运行态实测 404），两个跳转按钮在开发环境**打开的是 SPA 自身**。详见 §3.5、§6、§9。

| 项 | 值 | 证据 |
|----|-----|------|
| 页面 | `admin/dev/api-doc`（单入口） | `sys_menu.id=62403`，`menu_type=1`，`display_mode=0`，`list_path` 为空（psql 实测） |
| 组件 | `frontend/apps/pc-admin/src/views/admin/dev/api-doc/index.vue`（**160 行**） | `sys_menu.component=views/admin/dev/api-doc` |
| 外壳 | `PageContainer(full-height)` + `#header` 插槽 + `a-card` + 裸 `a-table`（**无布局组件**） | `api-doc/index.vue:2`、`:3-32`、`:34`、`:90-105` |
| 后端 | **未找到**：本页唯一端点 `GET /api/monitor/info` 在 `SystemMonitorController` 中**不存在** | §6.2、§6.3 |
| 数据表 | **无**：本页不读写任何表（全库表名含 `api_doc` 的表 **0 张**，psql 实测） | §7 |
| 权限码 | 前端**零 `v-permission`**（全文 0 处）；本页无对应后端写端点 | §5.4 |
| 数据真实性 | **整页桩**（表格 100% 前端写死；唯一真实分支指向不存在的接口） | `api-doc/index.vue:136-153` |
| 页面配置 | **无 `storage-key`**、无 `PageConfigPanel`、无列配置弹窗、无表头齿轮 | §3.6 |

### 1.1 与相邻页的分工（`61305 开发工具` 组 4 页极易混淆）

| 页面 | 菜单ID | 回答的问题 | 本页与其关系 |
|------|:-----:|-----------|-------------|
| 模板管理 | 62402 | 「导入/导出/打印模板长什么样、有几套」 | 无关；模板数据存后端内存（README §3.3） |
| **API文档（本页）** | 62403 | 「系统提供哪些接口、怎么浏览和试调」——**文档门户** | 本页 |
| API测试 | 62404 | 「发一个具体请求、看返回」——**调试台** | 与本页是两个页面；本页只「跳转」不「调试」（README §3.4 P0-11） |
| 定时任务 | 62405 | 「哪些任务在跑、跑得怎么样」 | 无关 |

> **一句话分工**：本页是**开发工具组的门面页**（只做说明 + 两处外链 + 一张模块清单）；真正能返回「真实端点清单」的能力在**交易模块的 API 监控页**（`ApiMonitorController`，见 §6.4），本页**未接**。
> **同一后端端点的跨页复用（登记）**：接口监控页 62203 也调用 `/monitor/dashboard` 与 `/monitor/info`（README §2.5、§3.4 P0-3）—— **同一个不存在的端点被两个页面共用**，属同一根因。

### 1.2 本页不提供的能力（如实登记）

| 缺什么 | 现状 | 级别 |
|--------|------|:----:|
| **真实接口清单** | 表格数据 100% 前端写死（`:146-153`）；**不来自 springdoc / OpenAPI 扫描**，也不来自任何后端接口 | **P0** |
| **OpenAPI spec 展示** | 无 spec 渲染、无 `operationId` / `tags` / `parameters` / `responses` 任何字段的呈现（4 列只有 `name` / `basePath` / `count` / `description`） | **P0** |
| **按模块 / 按权限过滤** | 无查询条件、无筛选、无搜索框 | P1 |
| **接口详情 / 参数定义** | 无详情页、无抽屉、无行点击 | P1 |
| **「在线试调」** | 无（该能力在 API测试页 62404，且其 url/method 被 `buildConfig` 静默丢弃，README §3.4 P0-11） | P1 |
| **列配置（表头齿轮）** | 无 `BillDetailTable`、无 `defaultHidden` / `slotName` / `type` | P1 |
| **页面配置弹窗** | 无 `PageConfigPanel` | P1 |
| **经典分页栏** | `:pagination="false"`（`:94`），**表格无分页** | P1 |
| **导出 / 打印 / 快捷键** | 全无 | P2 |
| **验收截图 / 模块级 E2E** | `tools/` 与 `tools/acceptance/` 下不存在 `e2e-system-*.cjs` / `e2e-admin-*.cjs` | P2 |

---

## 2. 页面截图

**N/A —— 本模块无对标系统截图。**

**原因（本模块的通用前提，本页严格遵守）**：ql361（来肯企汇 v2.2，`22stable.ql361.com`）是**租户级进销存产品**，它对客户暴露的只有「设置」域（`client_type = tenant-admin`）。本页属于**平台控制台**（`client_type = system-admin`，psql 实测 `sys_menu.id=62403` 的 `client_type='system-admin'`）——「平台研发方自己怎么看自己的接口」。因此：

- ql361 中**不存在**「API文档」这个页面，**无可对标截图**；
- **不使用 Odoo / SAP / 金蝶 / 用友的界面截图**（口径见《系统模块/README.md》§5.3：本模块按业界**能力模型**建模，**不照搬任何一家的界面**）；
- §8 只写**业界能力对标**（本页对标 OpenAPI Specification 3.1.0），逐条标来源与可信度级别。

**本系统截图**：当前无本系统验收截图（`tools/` 与 `tools/acceptance/` 下不存在 `e2e-system-*.cjs` / `e2e-admin-*.cjs`）→ **P2 缺口**（README §7.3）。

**数据现状（devdb + 运行态实测 2026-09-18）**：

```
-- 全库表名含 api_doc 的表（information_schema.tables，public schema）
select count(*) from information_schema.tables
 where table_schema='public' and table_name like '%api_doc%';
 count = 0            -- 0 张

-- 菜单行（sys_menu，逐列）
 id=62403 | API文档 | system:dev:api-doc | admin/dev/api-doc | views/admin/dev/api-doc
 client_type=system-admin | status=1 | visible=1 | sort=300 | deleted=0 | tenant_id=0
 display_mode=0 | menu_type=1 | parent_id=61305
```

**结论（本页数据现状的核心）**：**本页不读写任何表**，**表格内容 100% 由前端写死**（6 条固定记录，`api-doc/index.vue:146-153`）。因此「表格有没有数据」与数据库**无任何关系** —— 库空、库满、库被删，表格都渲染同样 6 行。这一点决定了本页 §10 的验收必须以「**证明它是桩**」为主线（见 §10.1-D 组）。

---

## 3. 列表页配置（`views/admin/dev/api-doc/index.vue`，160 行）

### 3.1 页面外壳

```
PageContainer full-height(:2)  ← 闭合 :107
└─ #header 插槽(:3-32)
   ├─ .page-header > .page-header-left(:4-18)
   │   ├─ a-breadcrumb：首页(router-link to "/") / 系统管理 / API文档(:6-14)
   │   └─ h2.page-header-title：API文档(:15-17)
   └─ .page-header-right(:19-30) → 「刷新」按钮(:20-29，ReloadOutlined，:loading="loading")
└─ a-card(:bordered="false")(:34-106)
   ├─ #title：接口文档(:35-37)
   ├─ #extra：a-space → 「Swagger UI」(:40-47) + 「Knife4j」(:48-55)
   ├─ 灰底空态区 div(:59-86)：a-empty description="API 文档已集成"(:60)
   │   ├─ #image：FileTextOutlined 64px #1890ff(:61-63)
   │   ├─ 说明文案两行(:64-67)
   │   └─ 「打开 Swagger UI」(:69-77) + 「打开 Knife4j」(:78-83)
   ├─ a-divider(:88)
   └─ 裸 a-table(:90-105)
       ├─ :data-source="modules" / :columns="moduleColumns" / row-key="name"(:91-93)
       ├─ :pagination="false"(:94)、size="small"(:95)
       └─ #bodyCell：column.key === 'count' → a-badge :overflow-count="999"(:97-104)
```

| 项 | 当前实现 | 金标准（路线 A） | 差距 |
|----|----------|-----------------|------|
| 外壳顶层 | `PageContainer(full-height)`（`:2`） | `ErrorBoundary > PageContainer(full-height)` | 缺页面级 `ErrorBoundary`（全局包裹在 `App.vue`） |
| 布局组件 | **无**（`a-card` 直接包内容） | 按页面形态选骨架（本页是「说明 + 清单表」，最接近 `DocCenterLayout` 或 `CategoryListLayout`） | 整体重写 |
| 表格 | 裸 `a-table`（`:90-105`） | `BillTableList` / `BillDetailTable`（含列配置） | **无列配置能力** |
| 分页 | **无**（`:pagination="false"`，`:94`） | `StandardPagination variant="classic"` | 形态不符（但本页 6 条固定数据，分页意义有限） |
| 列配置 | **无表头齿轮**（列数组只有 `title/dataIndex/key/width/ellipsis`，`:118-123`） | 表头齿轮（个人 / 全局） | 无 |
| 页面配置 | **无 `PageConfigPanel`**、无 `storage-key` / `global-config-key`（全文零命中） | `PageConfigPanel` | 无 |
| 行选择 / 批量 | **无** | — | 本页无写动作，属合理缺失 |
| 页内 Tab | **无** | — | 本页内容少，属合理缺失 |
| 图表 / 统计卡 | **无** | — | 本页无此诉求 |
| 打印 / 导出 | **无** | — | P2 |

> **判定**：本页外壳是「**裸 `PageContainer` + `a-card` + 裸 `a-table`**」，**未达金标准路线 A 的任何一条**，与 README §3.1「30 页全部未达金标准」口径一致。
> **但优先级提醒**：本页真正的 P0 **不是外壳**，而是「**表格数据是写死的**」（§3.5）。换外壳不解决「用户看的是假清单」这个问题。

### 3.2 页内 Tab

**本页无页内 Tab**：全文未使用 `a-tabs`（grep 零命中）。

### 3.3 查询条件

**本页无查询条件**：无查询表单、无 filter 区、无搜索框、无排序参数、无分页参数（全文零命中）。

| 项 | 现状 | 目标规格（★ = 需后端配合） |
|----|------|---------------------------|
| 查询项数量 | **0 项** | 建议 ★按模块名（对应 OpenAPI `tags`）、★按路径前缀（对应 `servers` / `paths` 前缀）、★按权限码可见性过滤 |
| 排序 | 无 | ★按 `name` / ★按 `count` |
| 分页 | 无（`:pagination="false"`，`:94`） | 接入真实 spec 后需分页（本系统路径数量远大于 6） |
| 视图形态 | 单视图（无 Tab、无树） | 建议 ★「按模块 / 按标签 / 按权限」三视图 |

### 3.4 功能按钮（**全页共 5 个**）

| 位置 | 按钮 | 权限码 | 接线状态 | 行号 |
|------|------|--------|---------|------|
| 页头右侧 | `刷新`（`size="small"` + `ReloadOutlined`，`:loading="loading"`） | **无** | **调接口但接口不存在**：`refreshAll`（`:133-157`）→ `request.get('/monitor/info')`（`:136`）→ 必然 404 → 走 `catch` 写死兜底（`:146-153`） | `:20-29` |
| 卡片右上（`#extra`） | `Swagger UI`（`type="primary" ghost`） | **无** | `openSwagger`（`:125-127`）→ `window.open('/swagger-ui/index.html', '_blank')`（`:126`） | `:40-47` |
| 卡片右上（`#extra`） | `Knife4j`（`type="primary" ghost`） | **无** | `openKnife4j`（`:129-131`）→ `window.open('/doc.html', '_blank')`（`:130`） | `:48-55` |
| 空态区内 | `打开 Swagger UI`（`type="primary"` + `FileTextOutlined`） | **无** | 同一个 `openSwagger` | `:69-77` |
| 空态区内 | `打开 Knife4j`（`FileTextOutlined`） | **无** | 同一个 `openKnife4j` | `:78-83` |

**桩按钮清单（本页共 1 个）**：

| 桩按钮 | 桩的形态 | 证据 |
|--------|---------|------|
| `刷新` | **调真实接口但接口不存在**：请求路径 `/monitor/info` 在 `SystemMonitorController`（17 个端点）中**无对应 mapping**；失败被 `catch {}` 静默吞掉后**立即填 6 条写死数据**，页内**不提示、不标记** | `api-doc/index.vue:136`、`:145-153`；§6.3 |

> **另 4 个按钮不是「桩」但「不可用」**（性质不同，必须区分）：
> - `Swagger UI`（2 处）与 `Knife4j`（2 处）是**真实跳转**（`window.open`），但在**开发环境两处都打不开文档页** —— 因为 dev server 未代理 `/swagger-ui/**` 与 `/doc.html`（§5.5）。**这不是桩，是配置缺失。**

**权限缺陷**：全页**无任何 `v-permission`**、无权限码常量、无 `usePermission`（全文零命中）。本页也没有任何后端写端点，因此无「按钮可见但 403」的问题；但**「本页对谁可见」完全由菜单 `client_type` 决定**（§5.4）。

### 3.5 表格列（**4 列**，`moduleColumns` `:118-123`）

| # | title | dataIndex | key | 宽 | 渲染 | 行 |
|:-:|-------|-----------|-----|:--:|------|----|
| 1 | 模块名称 | `name` | `name` | 无 | 原值 | `:119` |
| 2 | 基础路径 | `basePath` | `basePath` | 无 | 原值 | `:120` |
| 3 | 接口数量 | `count` | `count` | 100 | `#bodyCell` → `a-badge :count="record.count"`（`:97-104`） | `:121` |
| 4 | 描述 | `description` | `description` | 无（`ellipsis: true`） | 原值 | `:122` |

**列定义核查**：

| 项 | 实测结论 |
|----|---------|
| `type` / `defaultHidden` / `slotName` | **全部未指定**（无列配置能力） |
| `formatter` | **未使用**；因此不存在 `formatter: ({row}) => ...` 对象解构缺陷写法 |
| 表格分页 | **无**（`:pagination="false"`，`:94`） |
| 行操作列 | **无** |
| `row-key` | `"name"`（`:93`） —— 因数据是写死的 6 条、`name` 唯一，未见冲突 |

**表格数据来源判定（本页最关键的一节）**：

| 判定项 | 结论 | 证据 |
|--------|------|------|
| 真实分支 | 只在 `res?.endpoints` 存在时执行（`:136-144`） | `api-doc/index.vue:136-144` |
| 真实分支可达性 | **永不可达** —— 它依赖的 `GET /api/monitor/info` 后端不存在，**必然 404**（运行态实测） | `:136`；§6.3 |
| 兜底分支 | `catch` 里**写死 6 条**（`:146-153`），因此**表格 100% 来自兜底** | `:145-153` |
| 兜底数据原文 | 见下方代码块 | `api-doc/index.vue:146-153` |

```js
    modules.value = [
      { name: '认证授权', basePath: '/api/auth', count: 8, description: '登录、登出、Token验证' },
      { name: '菜单管理', basePath: '/api/menu', count: 12, description: '菜单CRUD、角色菜单授权' },
      { name: '用户管理', basePath: '/api/user', count: 15, description: '用户CRUD、角色分配' },
      { name: '角色管理', basePath: '/api/role', count: 10, description: '角色CRUD、权限分配' },
      { name: '基础数据', basePath: '/api/base', count: 25, description: '字典、参数、配置' },
      { name: '文件管理', basePath: '/api/file', count: 6, description: '文件上传、下载、预览' },
    ]
```

**6 条的 `count` 固定值**：`8 / 12 / 15 / 10 / 25 / 6`（合计 76）。**这 6 个数字与后端任何接口的返回值都无关系**，是**纯字面量**。

> **用户可感知的后果（本页最严重的体验问题）**：`catch {}` 静默吞掉 404 后**立即填兜底数据**（`:145-153`）—— 页内**不提示、不标记**。用户唯一能看到异常的信号是全局响应拦截器弹的**一次 404 toast**（`utils/request.ts:329-331`，`message.error('请求资源不存在')`），随后表格**照样显示 6 条写死数据**。
> → **用户无法从表格内容区分「真取到」与「兜底写死」**。这是本页必须写进 §3 的桩特征。

### 3.6 行操作

**无**：无操作列、无行点击、无行内链接。表格纯展示（且展示的是假数据）。

### 3.7 弹窗 / 抽屉 / 表单

**全无**：无 `a-modal`、无 `a-drawer`、无 `a-form`（全文零命中）。

### 3.8 刷新与资源清理（无泄漏）

| 项 | 实测 |
|----|------|
| 页面加载拉取 | `onMounted(refreshAll)`（`:159`），**只在挂载时拉一次** |
| 轮询 / 定时器 | **无**：全文 `setInterval` / `setTimeout` **0 命中** |
| 事件监听 | **无**（无 `addEventListener`） |
| `onUnmounted` / `onBeforeUnmount` | **无** |
| 内存泄漏检查 | **无任何需要清理的资源**（无定时器、无监听器），**不存在泄漏** |
| 重复点击风险 | `刷新` 按钮 `:loading="loading"`（`:22`）有防重入；`openSwagger` / `openKnife4j` 无节流（可开出多个标签页，P2） |

---

## 4. 表单页配置

**本页 N/A —— 无表单页。**

- 菜单 `display_mode = 0`（单入口）、`list_path` 为空（psql 实测），本页自身即全部界面；
- 页内**无弹窗、无抽屉、无表单字段**（§3.7）。

**目标规格（若本页按 OpenAPI 改造，需补的「交互面」，★ 标需新增能力）**：

| ★ 目标交互 | 内容 | 对应 OpenAPI 对象 |
|-----------|------|-------------------|
| ★ **接口详情抽屉** | 展示 `summary` / `description` / `operationId` / `deprecated` 与参数表 | `Operation Object` |
| ★ **参数定义表** | `name` + `in`（`path`/`query`/`header`/`cookie`）+ `required` + `schema` | `Parameter Object` |
| ★ **请求体 / 响应体视图** | `requestBody.content` 必填；响应码与 `default` | `RequestBody` / `Responses Object` |
| ★ **安全标注展示** | 展示该操作生效的 `securityRequirements` | `Security Requirement Object` |
| ★ **在线试调** | **不在本页做** —— 复用 API测试页 62404，且必须先按 OWASP SSRF 清单加固（README §3.4 P0-11） | — |

> **不发明字段承诺**：以上 ★ 项**本系统均未实现**，落地前须先让后端具备「生成并暴露 OpenAPI spec」的能力（§11.1）；**本页当前的 4 个列字段不动**（见 §8.5）。

---

## 5. 业务规范

### 5.1 状态值域

**本页 N/A —— 无状态字段、无下拉选项、无枚举**（全文零命中）。

| 项 | 现状 |
|----|------|
| 状态字段 | 无 |
| 下拉选项 | 无 |
| 与后端枚举对照 | **不适用**（无枚举） |

### 5.2 流转规则

**本页 N/A —— 无状态机、无写动作。**

```
（无状态）── 本页无任何会产生副作用的写操作 ──▶ （无状态）
```

| 动作 | 后端 | 前置校验 | 失败文案 | 副作用 | 行号 |
|------|------|:--------:|---------|--------|----|
| `刷新` | **不存在**（`GET /api/monitor/info`） | 无 | 页内**无**（只有全局 404 toast） | **无**（仅重置 `modules` 为写死的 6 条） | `:133-157` |
| `Swagger UI`（2 处） | 非 XHR，`window.open` | 无 | 无 | 打开一个新标签页 | `:125-127` |
| `Knife4j`（2 处） | 非 XHR，`window.open` | 无 | 无 | 打开一个新标签页 | `:129-131` |

### 5.3 取值口径（表格内容从哪来）

| 数据项 | 口径 | 证据 |
|--------|------|------|
| `name`（模块名称） | **前端字面量** | `:147-152` |
| `basePath`（基础路径） | **前端字面量**（且**是虚构的** —— `/api/auth`、`/api/user`、`/api/role`、`/api/base` 在本系统后端**不存在**，见 §6.5） | `:147-152` |
| `count`（接口数量） | **前端字面量**（`8 / 12 / 15 / 10 / 25 / 6`，合计 76） | `:147-152` |
| `description`（描述） | **前端字面量** | `:147-152` |
| 表格总行数 | 恒为 **6 行**（除接口异常时报错被吞，仍回填同一组 6 条） | `:146-153` |

> **本页 4 列全部是「字面量口径」，无任何一列来自数据库或后端。** 这一点是本页与系统模块其它页（如《租户列表》至少读 `sys_tenant`）的**根本区别**。

### 5.4 权限口径

| 层 | 现状 | 证据 |
|----|------|------|
| 前端 | **零权限控制**（无 `v-permission`、无权限码常量、无 `usePermission`，全文零命中） | `api-doc/index.vue` 全文 |
| 后端 | **无对应端点** → **无任何权限注解可查** | §6.2 |
| 菜单可见性 | 由 `sys_menu.client_type='system-admin'` 决定：**普通租户登录时本菜单不在菜单树中**（README §1.1） | psql 实测 62403 行 |
| 权限码落库 | 本页**不需要权限码**（无写端点）；但 `sys_permission` 中 `system:dev%` 相关码的现状见 §11.3 | devdb 实测 |
| 实际后果 | `admin` 可访问本页；**本页对任何账号都不产生 403**（无端点可 403） | §10.1-G 组 |

### 5.5 多租户口径（本模块红线②，本页须写清「为何 N/A」）

**本页不读库 → 多租户插件对本页无作用对象**，但口径必须写清以免误判：

| 事实 | 证据 |
|------|------|
| 本页**无任何表读写**，因此**不存在被注入 `tenant_id` 的 SQL** | §7（0 张表） |
| **超管会话有 `tenantScopeExempt` 整体豁免**（2026-09-18 实测确认）：写入点 = 登录时 `StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"))` | `SysUserServiceImpl.java:88` |
| 读取点 = `AiReadyTenantLineInnerInterceptor.shouldSkip()` → `MyBatisPlusConfig.isTenantScopeExempt()` | `AiReadyTenantLineInnerInterceptor.java:41-44`；`MyBatisPlusConfig.java:157-166`（`StpUtil.getSession().get("tenantScopeExempt")` 在 `:162`） |
| **订正后的准确口径**：「平台表查回 0 行」**只发生在「旧 token」或「非超管账号」两种情况下**，**不是**「超管默认看不到」 | README §1.3 红线②（已订正） |
| 对本页的意义 | **无**（本页不读表）；但**若将来本页改为「后端生成 spec 并读缓存表」，则必须按红线②处理该缓存表的租户豁免** | §8.5、§11.3 |

> **修法（新写平台级查询时仍推荐显式声明，不依赖会话豁免）**：读全局用 `@InterceptorIgnore(tenantLine = "true")`；写目标租户行前 `MyBatisPlusConfig.setTempTenantId(tid)`（`MyBatisPlusConfig.java:101-103`）。理由：会话豁免依赖 Sa-Token Session，对**定时任务 / 无会话上下文**的链路无效。

### 5.6 文档跳转地址的口径（本页特有的「外链契约」）

本页有两条外链，各自的口径如下：

| 按钮 | 打开的地址 | 后端是否可达 | 开发环境是否可达 | 证据 |
|------|-----------|:---:|:---:|------|
| `Swagger UI`（2 处） | `/swagger-ui/index.html`（相对路径，`window.open`） | **不可达（401）** —— **不在 Sa-Token 放行清单** | **不可达（未代理）** | `:126`；`SaTokenConfig.java:60-64`、`:100-105`；`vite.config.ts:109-137`；运行态实测 401 |
| `Knife4j`（2 处） | `/doc.html`（相对路径，`window.open`） | **可达（200）** —— 在放行清单内 | **不可达（未代理）** | `:130`；`SaTokenConfig.java:60`、`:101`；运行态实测 200 |

**结论**：「Swagger UI」按钮**双重不可用**（401 + 未代理）；「Knife4j」按钮**后端可达（200）但开发环境仍不可达**（未代理）。→ **两个按钮在 dev 环境都打开 SPA 自身**（`index.html`），而不是文档页。

---

## 6. 后端接口清单

### 6.1 本页用到的端点（**1 个，且不存在**）

| # | 方法 | 路径 | 入参 | 返回 | 前端封装 | 行号 |
|:-:|------|------|------|------|---------|------|
| 1 | GET | `/api/monitor/info` | 无 | 期望 `{endpoints: {...}}`（前端读 `res?.endpoints`，`:137`） | **无 api 封装**，直接 `import request from '@/utils/request'`（`:113`）后用 `request.get('/monitor/info')`（`:136`） | 前端 `:136`；axios `baseURL: '/api'` (`utils/request.ts:92-98`) |

> **必答项：前端在调、后端不存在** —— 这是**必然 404**。
> **运行态实测（2026-09-18，后端 :5655 / PID 67196，fat jar `C:/Users/Administrator/AppData/Local/Temp/hr-api.jar` 构建于 14:55、进程启动 15:02；用超管 `admin` 登录后带 token 请求）**：
> ```
> GET /api/monitor/info → 404  {"code":404,"message":"接口不存在: api/monitor/info"}
> ```

### 6.2 非 XHR 的两个外部地址

| 地址 | 触发按钮 | 行号 |
|------|---------|------|
| `/swagger-ui/index.html` | `Swagger UI` / `打开 Swagger UI` | `:126` |
| `/doc.html` | `Knife4j` / `打开 Knife4j` | `:130` |

### 6.3 为什么「后端无 `/info`」——`SystemMonitorController` 全端点逐条对照（**17 个，无 `/info`**）

控制器：`backend/core/api/core-api/src/main/java/cn/aiedge/monitor/controller/SystemMonitorController.java`，类上 `@RequestMapping("/api/monitor")`（`:26`），类级 `@SaCheckLogin`（`:29`），**无逐端点权限码**。

| # | 方法 路径 | 行号 | # | 方法 路径 | 行号 |
|:-:|----------|------|:-:|----------|------|
| 1 | `GET /metrics` | `:37` | 10 | `PUT /alerts/rules` | `:103` |
| 2 | `GET /metrics/history` | `:43` | 11 | `DELETE /alerts/rules/{ruleId}` | `:109` |
| 3 | `GET /metrics/trend/{metricName}` | `:50` | 12 | `GET /alerts/rules/{ruleId}` | `:116` |
| 4 | `GET /overview` | `:58` | 13 | `GET /alerts/rules` | `:122` |
| 5 | `GET /health` | `:64` | 14 | `POST /alerts/rules/{ruleId}/enable` | `:129` |
| 6 | `GET /jvm` | `:70` | 15 | `POST /alerts/rules/{ruleId}/disable` | `:136` |
| 7 | `GET /threads` | `:76` | 16 | `GET /alerts/history` | `:143` |
| 8 | `GET /memory` | `:82` | 17 | — （以上即全部） | — |
| 9 | `POST /gc` | `:88` | | **`/info`：无** | **无** |

> **17 个端点中没有任何 `/info` / `info` 映射** —— 这是「后端无 `/info`」的**第一条证明**（静态）。
> **第二条证明（更根本）**：该控制器所属的 `cn.aiedge.monitor` 包**不在 `AiReadyApplication.java:21-96` 的 `scanBasePackages`**（README §2.3，2026-09-18 复核 `grep` 命中 **0**）→ **这 17 个端点即使存在也不会被装配，运行期同样 404**。
> → **本页调用的 `/info` 属于「包没装配 + 路径也不存在」双 404。**

### 6.4 后端有、本页未用的相关端点（**1 个关键项**）

| # | 端点 | 能力 | 前端调用点 | 与 API文档页的关系 |
|:-:|------|------|-----------|-------------------|
| 1 | `GET /api/trade/api-monitor/calls/endpoints`（`ApiMonitorController.java:130-133`） | 返回**真实端点清单**（联调分组树 + 参数定义，`OpenApiCatalog.java:114`） | `frontend/apps/pc-admin/src/api/trade/index.ts:379`（交易 → API监控模块） | **本页应当复用它，但本页未调用**（见「不重复开发」原则，README §6） |

> **这是一条「能力已存在但没接」的证据**：全系统**已有**可用的端点清单实现（且该模块有 E2E 119/119 记录；`api_access_log` 表 63 行，README §3.5），本页却**另起一套前端写死的假清单** —— 违反 README §6「功能/模块不重复开发」与「页面组件优先复用」。

### 6.5 后端缺口（本页相关）

| 缺口 | 说明 | 级别 |
|------|------|:----:|
| **无 `/api/monitor/info`** | 前端唯一的真实调用指向不存在的端点 | **P0** |
| **无 OpenAPI spec 暴露端点** | 全库无任何返回 spec 的控制器；`/v3/api-docs` 属 springdoc 框架自带（§11.1），**未实测** | **P0** |
| **无接口清单聚合端点（本页专用）** | 已有能力在 `ApiMonitorController`，本页未接 | **P0** |
| **假 `basePath` 未被纠正** | 写死的 `/api/auth`、`/api/user`、`/api/role`、`/api/base` 在本系统后端**零命中**（本系统的真实前缀形如 `/api/menu`、`/api/config`、`/api/file` 等，且 `/api/auth` 不在 `SaTokenConfig` 的放行路径中作为独立控制器出现） | **P0**（属 §3.5 假数据的一部分） |
| **无审计** | 本页无写操作，**无审计诉求**（与 README 红线③无关） | N/A |

---

## 7. 数据模型

### 7.1 表

**本页不读写任何表。** 实测（psql，2026-09-18）：

```sql
select count(*) from information_schema.tables
 where table_schema='public' and table_name like '%api_doc%';
-- count = 0
```

| 项 | 实测值 |
|----|--------|
| 表名含 `api_doc` 的表 | **0 张** |
| 本页读写的表 | **0 张** |
| 本页依赖的实体 | **无** |
| `@TableName` 位置 | **不适用** |

> 对照：全库表名含 `template` / `import` 的表有 **14 张**（属模板管理页 62402 的领域），**与本页无关**。

### 7.2 本页触碰的「非业务数据」（仅菜单行，只读）

| 数据 | 来源 | 值 |
|------|------|-----|
| 菜单行 | `sys_menu.id = 62403` | `menu_name=API文档`、`menu_code=system:dev:api-doc`、`path=admin/dev/api-doc`、`component=views/admin/dev/api-doc`、`client_type=system-admin`、`status=1`、`visible=1`、`sort=300`、`deleted=0`、`tenant_id=0`、`display_mode=0`、`menu_type=1`、`parent_id=61305` |

- `sys_menu` 全表实测 **381 行**（README §3.5，其中 `tenant_id=0` 380 行、`tenant_id=1` 1 行）；
- 本页**只读取该行用于路由/菜单渲染**，**不在页面上展示、不修改**。

### 7.3 多租户插件对本页的作用

**N/A** —— 无 SQL、无表 → 无 `tenant_id` 注入点。口径见 §5.5。

---

## 8. 业界对标

> **本页在 ql361 无对标页**（§2 已说明），因此**本章只有「业界对标」一段，没有「ql361 对标规格」**。
> 来源取自《业界对标-系统模块-20260918.md》§5.2（第 487-497 行）、§8.7（第 876-884 行）、§9.1 第 14 条（第 923 行）与附章三条硬提示（第 1081-1085 行）。**不引用业界界面截图。**

### 8.1 业界叫法

- **OpenAPI Specification（OAS）**，业界通称 **Swagger**（工具链）；
- Spring Boot 生态常用 **springdoc-openapi**（**本次未取证该库文档，属 D 级提示，实施前须核其官网**——底稿 §9.1 第 89 项明列「Spring 生态的 OpenAPI 生成库（springdoc-openapi）为业界常用（未取证其文档）」）。

### 8.2 业界做法要点（逐条引用 OpenAPI 3.1.0 官方规范，A 级）

1. **规范定位原文**：「The OpenAPI Specification (OAS) defines a standard, programming language-agnostic interface description for HTTP APIs」。
2. **根对象（OpenAPI Object）必填 `openapi` 与 `info`**，可选 `jsonSchemaDialect`、`servers`、`paths`、`webhooks`、`components`、`security`、`tags`、`externalDocs`；且**文档至少要包含 `paths`、`components`、`webhooks` 之一**。
3. **操作（Operation Object）字段**：`tags`、`summary`、`description`、`externalDocs`、`operationId`、`parameters`、`requestBody`、`responses`、`callbacks`、`deprecated`、`security`、`servers`。其中 **`operationId` 区分大小写且唯一**；`parameters` 按 **name + in 组合唯一**；`in` 取值 `path`/`query`/`header`/`cookie`；`RequestBody Object` 的 `content` 必填；`Responses Object` 至少一个响应码并可用 `default`。
4. **可复用组件（components）**：`schemas`、`responses`、`parameters`、`examples`、`requestBodies`、`headers`、`securitySchemes`、`links`、`callbacks`、`pathItems`（共 10 类）。
5. **安全是数组且可被操作级覆盖**：`security` 是 `Security Requirement Object` 数组，「可被操作级覆盖，**空对象表示安全可选**」。安全方案类型含 `apiKey`、`http`、`oauth2`、`openIdConnect`（**该页在 `Security Scheme Object` 处被截断，`http` / `openIdConnect` 的字段说明未取到 —— 必须如实标注「未取到」**）。

**来源（标题 + URL + 章节 + 查阅日期 + 级别）**：
- OpenAPI Specification v3.1.0 `https://spec.openapis.org/oas/v3.1.0.html`（章节：OpenAPI Object / Paths Object / Operation Object / Components Object / Security Requirement Object），2026-09-18，**A**

**另：底稿 §8.7 对本页主题的原文口径**：
> **API 文档**：OpenAPI 3.1.0 官方规范（**A**），结构见 §5.2。
> **API 调试台的安全边界**：OWASP SSRF 清单（**A**），要点见 §5.3。**官方该页明确不含"响应处理"与"API 调试工具"的指导**，因此「调试台如何避免凭据外泄」**必须自建，且不得声称有业界官方标准**（§9）。

> **写作纪律（底稿附章第 3 条）**：凡底稿标 **D 级**的方案，本页必须写「**本系统设计**」而非「业界标准」；凡底稿 §9.1 的 18 项能力（含第 89 项 springdoc-openapi 未取证），本页**不得出现「业界通行做法是……」的表述**。

### 8.3 能力对照表

| 能力 | 业界做法 | 本系统当前 | 差距 / 建议 |
|------|---------|-----------|------------|
| **接口描述格式** | OpenAPI 3.1.0：根对象必填 `openapi` + `info`；至少含 `paths` / `components` / `webhooks` 之一（A） | **无任何 spec**：表格 4 列（`name`/`basePath`/`count`/`description`）是前端字面量 | **P0**：先让后端产出并暴露 spec；本页改为消费 spec |
| **操作级元数据** | `Operation Object` 有 `operationId`（唯一、区分大小写）、`tags`、`summary`、`description`、`deprecated`、`security` 等 12 个字段（A） | 表格**只有 4 列**，无 `operationId` / `tags` / `deprecated` / `security` 任何一项 | **P0**（需前端列语义重定义，见 §8.5） |
| **参数建模** | `parameters` 按 `name` + `in` 组合唯一；`in ∈ {path, query, header, cookie}`（A） | **无参数展示**（无详情页、无参数表） | P1 |
| **请求体 / 响应体** | `RequestBody.content` 必填；`Responses Object` 至少一个响应码，可用 `default`（A） | **无** | P1 |
| **可复用组件** | `components` 10 类：`schemas` / `responses` / `parameters` / `examples` / `requestBodies` / `headers` / `securitySchemes` / `links` / `callbacks` / `pathItems`（A） | **无** | P1 |
| **安全标注** | `security` 是 `Security Requirement Object` 数组，**可被操作级覆盖，空对象表示安全可选**；方案类型含 `apiKey` / `http` / `oauth2` / `openIdConnect`（A；`http`/`openIdConnect` 字段说明**未取到**） | **无 security 标注**；本系统**已有权限码体系**（`sys_permission` 共 264 行），但**未映射到 spec** | P1：把权限码映射为 `apiKey` / `oauth2` 描述（属**本系统设计**） |
| **按模块 / 按权限过滤** | 底稿 §5.2「落地」建议：平台级 API 文档页应能**按模块 / 按权限过滤**，且必须支持 `security` 标注（**该建议为底稿自建，非规范条文**） | **无过滤、无查询条件**（§3.3） | P1（**本系统设计**） |
| **spec 生成工具** | springdoc-openapi 在 Spring Boot 生态常用（**D 级：未取证该库文档**） | 依赖**已在**（`springdoc-openapi-starter-webmvc-ui`，`core-api/pom.xml:372-373`）；**yml 未声明开关**（§11.1） | 实施前**须核 springdoc 官网**，不得称「业界标准」 |
| **文档 UI** | Swagger UI / Knife4j 是常见渲染前端（工具链通称） | 两个按钮**在 dev 环境都打开 SPA 自身**（§5.6） | **P0**：修代理 + 修放行清单，或改为站内渲染 |
| **「前端写死模块清单」** | **无任何厂商这样做** —— 该模式**不属业界做法**，是本页缺陷 | **本页当前正是这样**（`:146-153`） | **P0**：必须消除 |

### 8.4 明确不借鉴

| 不借鉴 | 理由 |
|--------|------|
| **照搬 Swagger UI 的默认视觉** | 本模块口径：只学**能力模型**，**不照搬界面**（README §5.3）。且 Swagger UI 首屏在本系统**连登录都过不去**（401，§5.6） |
| **「前端写死模块清单」这种伪文档** | 这是本页**当前缺陷**，不是业界做法；任何情况下都不得保留 |
| **把 API 调试台并入本页** | 调试能力属 API测试页 62404；且其 url/method 透传存在 **SSRF + 凭据外泄 + 请求头注入**三重风险（README §3.4 P0-11），**未加固前不得扩大暴露面** |
| **按 OWASP SSRF 清单给本页加「调试」入口** | 底稿 §9.1 第 14 条：OWASP SSRF Cheat Sheet **完全没有**针对「API 测试/调试工具」的内容 → 「调试台剥离平台内部凭据 + 响应大小/超时上限」属 **D 级自建安全设计**，**不得写成业界标准** |
| **宣称 springdoc-openapi 是「业界标准」** | 底稿 §9.1 第 89 项明列**未取证其文档**；只能说「Spring Boot 生态常用（D 级提示）」 |

### 8.5 不发明字段承诺

本页建议涉及的字段与表，逐项声明：

| 项 | 现状 | 落地性质 |
|----|------|---------|
| `name` / `basePath` / `count` / `description` | **已在 `moduleColumns` `:118-123` 定义**，但值是前端字面量 | **不新增字段**；若按 OpenAPI 改造，属**前端列语义重定义**（对齐 `tags` / `servers.url` / `paths` 计数 / `info.description`），**不新增后端实体** |
| `operationId` / `tags` / `parameters` / `responses` / `security` 等展示字段 | **本系统不存在** | 来自 spec 对象本身，**不是实体字段**；需后端先产出 spec |
| 缓存 spec 的表 | **不存在** | ★**需新增表**（建议名 `api_spec_snapshot`，用于存放 spec 快照/版本），**必须显式标注「需新增表」**，且落地时须按 §5.5 红线②处理租户豁免 |
| 权限码 → security 方案的映射 | **不存在** | **本系统设计**（非业界标准）：把 `sys_permission.permission_code` 映射为 `apiKey` / `oauth2` 描述 |

---

## 9. 实现差异说明（当前实现 vs 目标规格）

### 9.0 本轮（2026-09-18）处置结果

**本轮无代码改动，以下为现状核实。** 本页所有结论均来自 `views/admin/dev/api-doc/index.vue`（160 行）、`SystemMonitorController.java`、`SaTokenConfig.java`、`vite.config.ts`、`ApiMonitorController.java` 的逐行核对、devdb 实测与运行态只读探测。**未改的绝不写成已修。**

### 9.1 已完成（可保留）

| # | 事实 | 证据 |
|:-:|------|------|
| 1 | **无泄漏**：无定时器、无监听器，无需清理资源（`setInterval` / `setTimeout` / `addEventListener` 全文 0 命中） | `api-doc/index.vue` 全文 |
| 2 | **无匿名 mock 服务、无 `setTimeout` 假进度**（`TODO` / `FIXME` / `mock` 全文 0 命中） | 同上 |
| 3 | 表格列定义**无 `formatter` 对象解构缺陷写法**（该项目通病在本页**未出现**） | `:118-123` |
| 4 | `刷新` 按钮有 `:loading` 防重入 | `:22` |
| 5 | 面包屑层级与标题**与菜单命名一致**（「首页 / 系统管理 / API文档」，标题「API文档」） | `:6-17` |
| 6 | 两个跳转用**相对路径** `window.open`，未硬编码域名（换环境不用改代码） | `:126`、`:130` |

### 9.2 缺陷（按优先级，**均已核实、本轮未修**）

| 级别 | 缺陷 | 位置 | 修法 |
|:----:|------|------|------|
| **P0** | **整页桩**：表格 4 列数据 100% 前端写死（6 条固定记录，`:146-153`）；唯一「真实分支」指向不存在的 `GET /api/monitor/info` → **永不可达** | `:136-153` | 改为消费真实 OpenAPI spec（或先复用 `ApiMonitorController` 的 `/calls/endpoints`） |
| **P0** | **调用的端点不存在**：`GET /api/monitor/info` 在 `SystemMonitorController`（17 端点）中**无 `/info`**；且 `cn.aiedge.monitor` 包**未装配** → 双 404 | `:136`；§6.3 | 补端点（或复用已有能力） |
| **P0** | **假 `basePath`**：写死的 `/api/auth`、`/api/user`、`/api/role`、`/api/base` 在本系统后端为零命中/不存在 | `:147-152` | 随 spec 化一并消除 |
| **P0** | **错误被静默吞掉后填假数据**：`catch {}` 不提示、不标记，用户**无法区分「真取到」与「兜底写死」**（仅全局 404 toast 一次） | `:145-153`；`utils/request.ts:329-331` | `catch` 内给出**显式错误态**（空态 + 重试按钮），不得回填业务数据 |
| **P0** | **两个跳转按钮在开发环境打开 SPA 自身**：`/swagger-ui/**`、`/doc.html` **未被 vite 代理**（proxy 仅 `/api`、`/api/erp/mall`、`/ws`）；且 `/swagger-ui/index.html` **不在 Sa-Token 放行清单**（实测 401） | `:126`、`:130`；`vite.config.ts:109-137`；`SaTokenConfig.java:60-64`、`:100-105` | 补 vite 代理 + 把 `/swagger-ui/**` 加入放行清单（或在 `SaTokenConfig` 中放行后由前端站内渲染） |
| **P1** | **已有真实能力未接入**：`GET /api/trade/api-monitor/calls/endpoints`（`ApiMonitorController.java:130-133`）能返回真实端点清单，本页**不调用** → 违反「不重复开发」 | §6.4；`api/trade/index.ts:379` | 复用该端点，删除写死清单 |
| **P1** | **空态文案是静态说明、非实时探测**（「API 文档已集成」，但后端 spec 实际不可从本页到达） | `:60`、`:64-67` | 改为真实探测结果驱动的状态文案 |
| **P1** | **`springdoc` / `knife4j` 的开关未在 core-api yml 声明**，行为靠依赖默认值（`/doc.html` 实测 200 说明 Knife4j 默认开启） | `core-api/pom.xml:372-373`；`wms/pom.xml:48`；核心 yml 零命中（§11.1） | 显式声明开关与路径，避免默认值漂移 |
| **P1** | **未达金标准路线 A**：无 `ErrorBoundary`、无列配置齿轮、无 `PageConfigPanel`、无经典分页 | `:2`、`:34`、`:90-105`、`:118-123` | 整体重写为路线 A |
| **P2** | **无查询 / 无排序 / 无 Tab**（0 项） | §3.3 | 接入 spec 后补 |
| **P2** | **`openSwagger` / `openKnife4j` 无节流**，连点可开多个标签页 | `:125-131` | 补节流或改为站内路由 |
| **P2** | **列宽未指定**（4 列中 3 列无 `width`），列内容短，视觉尚可但布局不稳 | `:119`、`:120`、`:122` | 补 `width` 或改列配置化 |
| **P2** | **无验收截图 / 无模块级 E2E** | README §7.3 | 随系统模块整体补 `tools/e2e-system.cjs` |

---

## 10. 验收标准

> **前置**：本模块**尚无 E2E**（`tools/` 与 `tools/acceptance/` 下无 `e2e-system-*.cjs` / `e2e-admin-*.cjs`）。建议落地 `tools/e2e-system.cjs`（接口 + 直连 DB 对账 + 写库后复原 + Playwright UI 截屏，参照 `tools/e2e-crm.cjs`）。
> **权限账号**：`admin`（`SUPER_ADMIN`，`tenant_id=1`）用作 200/404 基线；另需一个**非超管账号**（建议持 `SYSTEM_ADMIN` 或 `DEPT_ADMIN` 角色，二者均**无** `*` 通配）做对照。
> **本页验收主线**：本页**整页桩**，验收的首要目标**不是「能取到数据」，而是「能证明它是桩」** —— 即断言表格内容**等于前端源码里的字面量**，而**不等于后端任何接口的返回值**（因为后端根本没有对应接口）。这一组断言在 §10.1-D 与 §10.2 中单列。
> **运行态基线（2026-09-18 实测，后端 :5655）**：`GET /api/monitor/info` → **404**；`GET /swagger-ui/index.html` → **401**；`GET /doc.html` → **200**；`GET /api/v3/api-docs` → **404**；阳性对照 `GET /api/config/list` → **200**；`GET /api/menu/tree`（无参数）→ **400「缺少必要参数: tenantId」**；**未挂 token 时全部 401**。

### 10.1 接口验收（**24 项**）

**A. 可达性与基线（①–④）**

① `GET /api/monitor/info`（带 `admin` 的 `Authorization: Bearer <token>`）→ **期望 404**，响应体 `{"code":404,"message":"接口不存在: api/monitor/info"}`（与 §6.1 运行态实测逐字一致）。**这是本页桩性的第一证据**。
② 同一路径**不带 `Authorization`** → **期望 401**（全局 Sa-Token 拦截器 `StpUtil.checkLogin()`，`SaTokenConfig.java:70`）。
③ **阳性对照**：`GET /api/config/list`（带 token）→ **期望 200** —— 用同一次会话证明「token 有效、后端可达」，从而排除「404 是因为没登录」的可能。
④ `GET /api/menu/tree`（带 token、**不带参数**）→ **期望 400**，消息含 `缺少必要参数: tenantId`；**带 `tenantId=1`** → **期望 200** —— 登记「菜单树依赖 tenantId」这一会话口径。

**B. 「后端无 `/info`」的双重证明（⑤–⑦）**

⑤ **静态断言（第一条证明）**：读 `SystemMonitorController.java`，断言全类 `@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@PatchMapping` 共 **17 个**（行号 `:37/43/50/58/64/70/76/82/88/97/103/109/116/122/129/136/143`），且**无任何一个路径含 `info`**。断言方式：`grep -c` 计数 = 17 且 `grep -n "info"` 零命中。
⑥ **静态断言（第二条证明，更根本）**：读 `AiReadyApplication.java:21-96`，断言 `scanBasePackages` 中**不包含** `cn.aiedge.monitor`（`grep -c "cn.aiedge.monitor"` = **0**）→ 上述 17 个端点运行期同样 404。
⑦ **运行态对照（未实测项，如实标注）**：`GET /api/monitor/health` → **未实测**；**预期 404**（与 ⑥ 一致）。修复 `scanBasePackages` 后应改为断言 200，并可据此复核 ⑤ 的路径清单。

**C. 文档跳转能力的实测（⑧–⑫）**

⑧ `GET /swagger-ui/index.html`（带 token）→ **期望 401**（**不在 Sa-Token 放行清单**；与 §5.6 实测一致）。断言响应**不含** Swagger UI 的 HTML 特征（如 `swagger-ui` 关键字）。
⑨ `GET /doc.html`（带 token）→ **期望 200**（**在放行清单内**，`SaTokenConfig.java:60`、`:101`）→ 证明 **Knife4j 后端可达**。
⑩ **静态断言放行清单内容**：读 `SaTokenConfig.java:60-64` 与 `:100-105`，断言清单**含** `/doc.html`、`/webjars/**`、`/swagger-resources/**`、`/v3/api-docs/**`、`/favicon.ico`、`/error`；**不含** `/swagger-ui/index.html` 或 `/swagger-ui/**`。
⑪ `GET /api/v3/api-docs`（带 token）→ **期望 404**（前端 axios `baseURL: '/api'` 会把 spec 路径拼歪；springdoc 的真实路径是 `/v3/api-docs`，**不带 `/api` 前缀**）。`GET /v3/api-docs` → **未实测**（**本轮未探测**）；**预期 200**（在放行清单 `SaTokenConfig.java:63` 内）。**必须如实登记为「未实测」**。
⑫ **静态断言 vite 代理键**：读 `frontend/apps/pc-admin/vite.config.ts:109-137`，断言 `server.proxy` 的键**恰好只有 3 个**：`/api/erp/mall`（`:110`）、`/api`（`:121`）、`/ws`（`:133`）→ 因此 `/swagger-ui/**` 与 `/doc.html` **未被代理**。

**D. 桩性判别（本页核心，⑬–⑰）**

⑬ **静态断言写死内容**：读 `api-doc/index.vue:146-153`，断言数组恰为 **6 条**，且 `count` 依次为 **8 / 12 / 15 / 10 / 25 / 6**（合计 76），`name` 依次为 `认证授权 / 菜单管理 / 用户管理 / 角色管理 / 基础数据 / 文件管理`。
⑭ **UI 断言（关键判别断言）**：页面表格**渲染 6 行**，且每行 4 列的内容**逐字等于 §10.1-⑬ 的 6 条字面量**。
    **断言写法必须是「与前端源码字面量比对」，而**不是**「与后端接口返回值比对」** —— 因为后端**没有**这个接口，任何「与接口返回值一致」的断言都无从建立。**这一条即「证明它是桩」的核心**。
⑮ **反证 1（接口断了照样有数据）**：在 ① 已断言 `GET /api/monitor/info` = **404** 的前提下，断言表格**仍然是 6 行**（而不是空表）→ 证明数据来自 `catch` 兜底而非后端。
⑯ **反证 2（真实分支不可达）**：静态断言 `:136-144` 的赋值分支**以 `res?.endpoints` 为条件**，而唯一能提供 `endpoints` 的接口不存在 → 断言该分支在生产路径上**永不可达**（可在源码中加临时 `console.log` 复核，**复核后必须移除**，不得留痕）。
⑰ **静默失败断言**：点「刷新」→ 断言**页内无任何错误提示元素**（无 `a-alert`、无错误空态、`modules` 未被清空）；同时断言**恰好出现一次**全局 404 toast（`utils/request.ts:329-331`），且**表格内容不变**。

**E. 已有真实能力未接入（⑱–⑲）**

⑱ **静态断言**：`grep -c "api-monitor"` 与 `grep -c "calls/endpoints"` 在 `api-doc/index.vue` 中均为 **0** → 本页**未调用**全系统已存在的端点清单能力。
⑲ **运行态对照**：`GET /api/trade/api-monitor/calls/endpoints`（带 token）→ **期望 200** 且返回**非空**端点清单（该能力由 `ApiMonitorController.java:130-133` + `OpenApiCatalog.java:114` 提供；前端调用点 `api/trade/index.ts:379`）→ 证明「真实能力已存在，只是本页没接」。**修复后本页断言应改为「本页表格条数 = 该接口返回条数」。**

**F. 数据层（⑳–㉒）**

⑳ **DB 直查对账**：`select count(*) from information_schema.tables where table_schema='public' and table_name like '%api_doc%'` → 断言 **= 0**（证明本页不落库、无表可查）。
㉑ **DB 直查菜单行**：`select id,menu_code,path,component,client_type,status,visible,sort,deleted,tenant_id,display_mode,menu_type,parent_id from sys_menu where id=62403` → 断言逐列等于 §7.2 记录值（`system-admin` / `1` / `1` / `300` / `0` / `0` / `0` / `1` / `61305`）。
㉒ **「改库 → 回读 → 按原值复原」标准动作（本页以 `sys_menu` 为对象）**：
    - 记录原值：`sort = 300`（须先 `select` 落盘）；
    - 改库：`UPDATE sys_menu SET sort = 301 WHERE id = 62403`；
    - 回读：刷新页面 → 断言菜单排序发生变化（**证明菜单来自 DB**）；
    - **同时断言表格内容仍是同一组 6 条、`count` 仍是 8/12/15/10/25/6**（**证明表格不来自 DB** —— 这是本页桩性的第三条反证）；
    - **按原值复原**：`UPDATE sys_menu SET sort = 300 WHERE id = 62403`；再回读断言 `sort = 300`。
    **注意**：这是本页**唯一**可执行的「改库」动作；**不得**对其它表做写操作。

**G. 权限断言（㉓–㉔）**

㉓ 用 **`admin`（`SUPER_ADMIN`）** 调 `GET /api/monitor/info` → **期望 404**（**不是 403**）—— 因为端点在**路由层**就不存在，权限链根本不参与。这条断言用于防止把「404」误判为「权限问题」。
㉔ 用**非超管账号**（无 `*` 通配）调同一路径 → **期望同样是 404**（同理）；**对照组**：非超管调 `GET /api/menu/tree?tenantId=1` → **期望 403**（`system:menu:*` 6 个权限码在 `sys_permission` 中 **0 行**，README §3.4 P0-16）。
    **必须写清**：本页**自身不产生 403**（无写端点、无权限码），403 只出现在**对照组**上。

### 10.2 UI 验收（**17 项**）

① 菜单 `系统 → 开发工具 → API文档`（`62403`）点开直达本页（**单入口**，无标签跳转、无 `list_path`）。
② 面包屑逐字为 `首页 / 系统管理 / API文档`（`:6-14`），页面标题（`h2.page-header-title`）逐字 `API文档`（`:15-17`）。
③ 页头右侧有且仅有 1 个按钮 `刷新`（`:20-29`）。
④ 卡片 `#title` 逐字 `接口文档`（`:35-37`）；`#extra` 区有且仅有 2 个按钮，文案逐字 `Swagger UI`（`:46`）与 `Knife4j`（`:54`）。
⑤ 空态区（灰底）存在；`a-empty` 的 `description` 逐字 `API 文档已集成`（`:60`）；图标为 `FileTextOutlined`、字号 64px、颜色 `#1890ff`（`:61-63`）。
⑥ 空态正文两行逐字：`本系统通过标准工具提供完整的 API 文档` / `可使用 Swagger UI 或 Knife4j 浏览和测试接口`（`:65-66`）。
⑦ 空态区内按钮 2 个，文案逐字 `打开 Swagger UI`（`:76`）与 `打开 Knife4j`（`:82`）。
⑧ 分隔线（`a-divider`，`:88`）下方表格的 **4 个列头**逐字为 `模块名称 / 基础路径 / 接口数量 / 描述`（`:118-123`）。
⑨ **表格恒渲染 6 行**，`模块名称` 列逐行为 `认证授权 / 菜单管理 / 用户管理 / 角色管理 / 基础数据 / 文件管理`（`:147-152`）。
⑩ **「接口数量」列用 `a-badge` 渲染**（`:97-104`），断言可见数字为 **8 / 12 / 15 / 10 / 25 / 6**，且**不含**其它值（`:overflow-count="999"` 未触发）。
⑪ **表格无分页控件**（`:pagination="false"`，`:94`）—— 断言页面无「共 N 条 / 每页 N 条」等分页元素。
⑫ **桩判别（UI 侧核心）**：点「刷新」→ **观察**：① 出现一次 404 toast「请求资源不存在」；② **表格仍为同一组 6 行**；③ **页内无错误提示**。三条同时成立 = 桩性证据。
⑬ 点 `Swagger UI`（`:40-47`）→ 新标签打开；断言新标签 URL 为**当前站点**的 `/swagger-ui/index.html`，且**渲染的是 SPA 自身**（不是 Swagger UI 页面）→ 开发环境未代理的证据。
⑭ 点 `Knife4j`（`:48-55`）→ 同上，URL 为**当前站点**的 `/doc.html`，仍渲染 SPA 自身。
⑮ 空态区的 `打开 Swagger UI`（`:69-77`）与 `打开 Knife4j`（`:78-83`）行为与 ⑬/⑭ 一致（同一函数）。
⑯ **权限/可见性断言**：`admin` 登录时该菜单可见、可进入本页；**普通租户账号登录时 `系统` 顶级菜单不出现**（`client_type='system-admin'`，README §1.1）→ 手动输入路径 `#/admin/dev/api-doc` 亦不应进入。
⑰ **页面控制台无 `error`**（含 `TypeError` / `ReferenceError`）；并用 Network 面板断言：**整页只有 1 个 XHR**（`GET /api/monitor/info`，**状态 404**），且**无其它 4xx/5xx**。参见记忆中的「重构搬迁后必须做运行时验证」。

---

## 11. 配置落位

> 口径见《对标开发技术参考文档》§5.6 的四条落位通道：**① 环境变量 / ② core-api 的 Spring 配置（`application.yml` + profile yml）/ ③ 配置中心（数据库配置表）/ ④ 业务档案表字段**。
> **本页的特殊性**：本页**不读表**，所以「通道 3 / 通道 4」在本页**基本落空**；真正的落位点在**通道 2（yml）**与**代码常量（硬编码放行清单）**、以及**构建期配置（vite proxy）**。**本系统没有的配置项一律写「无配置项（未实现）」并标明建议落位，绝不编造 yml key。**

### 11.1 API 文档来源与 OpenAPI 工具链

| 配置项 | 落位 | 生效 | 缺失行为 |
|--------|------|------|---------|
| **本系统 API 文档的来源** | **无配置项（未实现）** | **不生效** | **表格 100% 前端写死**（6 条字面量，`api-doc/index.vue:146-153`）；后端无 `/info` → 必然 404 → 走兜底；用户看到的是**假清单** |
| **springdoc 是否启用 / 是否暴露 spec** | **通道 2 core-api yml —— 但未声明**（`backend/core/api/core-api/src/main/resources/` 下 `application*.yml` 全部 `grep springdoc|knife4j|swagger` **零命中**，2026-09-18 复核） | 依赖**已在**（`springdoc-openapi-starter-webmvc-ui`，`backend/core/api/core-api/pom.xml:372-373`）→ 行为**取决于框架默认值** | 未声明即**行为不受控**：`/api/v3/api-docs` 实测 404（前端 baseURL 拼错）；`/v3/api-docs` **未实测**。建议落位（**本系统设计**）：在 core-api yml 增加显式开关与 `springdoc.api-docs.path` / `springdoc.swagger-ui.path` —— **具体 key 名须先核 springdoc 官网（底稿 §9.1 第 89 项：未取证），本文不编造** |
| **knife4j 是否启用** | **通道 2 —— 未在 core-api yml 声明**（同上零命中） | 依赖经 **`backend/wms/pom.xml:48`**（`knife4j-openapi3-jakarta-spring-boot-starter`）→ **`core-api/pom.xml:126`**（依赖 `wms`）**传递到 core-api** | 传递存在 → `/doc.html` **实测 200**（后端可达）。但**无显式开关**＝无法按环境关闭文档端点（**生产环境不应暴露文档**，属 P1）。建议落位（**本系统设计**）：按 profile 显式关闭 |
| **API 文档页的模块清单** | **无配置项（未实现）** | **不生效** | 无任何配置可改这 6 条；改动需**改前端源码 + 重新构建**（`api-doc/index.vue:146-153`） |

### 11.2 鉴权与网络通道

| 配置项 | 落位 | 生效 | 缺失行为 |
|--------|------|------|---------|
| **Sa-Token 放行清单** | **未落位（硬编码常量）**：`backend/core/base/core-base/src/main/java/cn/aiedge/base/config/SaTokenConfig.java:60-64`（`SaInterceptor` 注册前的 `excludePathPatterns`）与 `:100-105`（`SaInterceptor` 注册）—— **Java 字符串清单，不是 yml 配置项** | **改代码 + 重启** | 清单**含** `/doc.html`（`:60`、`:101`）、`/webjars/**`、`/swagger-resources/**`、`/v3/api-docs/**`、`/favicon.ico`、`/error`；**不含 `/swagger-ui/index.html`（或 `/swagger-ui/**`）** → **Swagger UI 首屏被登录拦截（运行态实测 401）** |
| **前端 vite 代理** | **构建/开发期配置**：`frontend/apps/pc-admin/vite.config.ts:109-137` —— `server.proxy` 仅 3 个键：`/api/erp/mall`（`:110`）、`/api`（`:121`）、`/ws`（`:133`） | **dev server 启动时生效**（改后需重启 dev server） | `/swagger-ui/**` 与 `/doc.html` **未被代理** → 开发环境 `window.open` 打开的是 **SPA 自身**（`index.html`）而非文档页。**两个跳转按钮在 dev 环境都不可达** |
| **后端服务地址（代理 target）** | **通道 2 / 构建期配置**：`vite.config.ts` 内**硬编码** `http://localhost:5655`（`:111`、`:122`、`:134`） | dev server 启动时生效 | 后端换端口/换主机需**改前端配置并重启**；且**无环境变量覆盖** |

### 11.3 平台侧多租户豁免与权限码

| 配置项 | 落位 | 生效 | 缺失行为 |
|--------|------|------|---------|
| **平台侧多租户豁免** | **本页 N/A** —— 本页**不读库**（0 张表，§7.1），**无 SQL 可被注入 `tenant_id`** | — | 无缺失行为。**但若将来新增「spec 快照表」**，该表须按红线②处置：读全局用 `@InterceptorIgnore(tenantLine = "true")`；写目标租户行前 `MyBatisPlusConfig.setTempTenantId(tid)`（`MyBatisPlusConfig.java:101-103`） |
| **超管会话豁免标记（供理解，非本页配置）** | **运行时 Session（代码写入）**：写入点 `SysUserServiceImpl.java:88`（`StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"))`）；读取点 `AiReadyTenantLineInnerInterceptor.java:41-44` → `MyBatisPlusConfig.java:157-166`（`:162` 读 Session） | **登录时写入，当次会话生效** | **旧 token 无此标记** → 需重新登录才恢复全局视野（`MyBatisPlusConfig.java:150-152` 注释）。**本页不受影响**（不读库） |
| **本页可见性（菜单域）** | **通道 4 业务档案表**：`sys_menu.client_type`（`62403` 行实测 `system-admin`） | 登录构建菜单树时生效 | 若被误改为 `tenant-admin`，**普通租户也能看到平台页**（红线①同源风险：平台菜单不得混进租户套餐） |
| **本页权限码** | **无配置项（未实现）** —— 本页**无写端点**，不需要权限码 | — | 无缺失行为。对照：`sys_permission` 中 `system:dev%` 相关码的现状见 README §3.3（`system:dev:scheduler:*` 整组在库中但 30 页完全没用） |

### 11.4 落位缺口（登记）

| 缺口 | 建议 |
|------|------|
| **spec 的生成与暴露无任何配置** | 建议落位**通道 2**（core-api yml）：显式声明是否启用、spec 路径、UI 路径；**key 名须先核 springdoc 官方文档**（底稿 §9.1 第 89 项：未取证） |
| **放行清单是代码常量**，改一次要重发版 | 建议**保持硬编码**（属安全边界，不该运行时可改），但**必须补齐 `/swagger-ui/**`** 或明确「本系统不提供 Swagger UI，仅用 Knife4j」并在文案上同步（本页文案当前同时宣传两者，与能力不符） |
| **生产环境文档端点的开关** | 建议**通道 2**：prod profile 关闭 `/doc.html` 与 spec 端点（当前无开关，**生产会暴露**） |
| **前端代理 target 硬编码** | 建议**通道 1 环境变量**（如 vite 的 `loadEnv`）覆盖后端地址，避免多实例/多环境改代码 |
| **模块清单无配置** | **不应做成配置项** —— 正确做法是**消费真实 spec**，而不是把假数据配置化 |

---

## 12. 剩余缺口（P0 / P1 / P2）

> 口径与《系统模块/README.md》§7 对齐：**P0** = 页面不可用 / 数据不落库 / 越权 / 红线；**P1** = 与业界能力差距大但页面能开；**P2** = 体验与一致性。
> 本页对应 README §3.4 **P0-13** 与 §7.1 第 **13** 条「API文档页整页桩」。

### P0（5 条）

① **整页桩：表格数据 100% 前端写死（README §7.1-13 的本体）**：4 列内容与 `count`（8/12/15/10/25/6）全部是字面量（`api-doc/index.vue:146-153`）；唯一「真实分支」以 `res?.endpoints` 为条件（`:136-144`），而提供该字段的接口不存在 → **永不可达**。用户看到的是**与系统实际接口无关的假清单**。建议：改为消费真实 OpenAPI spec；短期可先复用 `GET /api/trade/api-monitor/calls/endpoints`。
② **调用的端点不存在（双 404）**：`GET /api/monitor/info` 在 `SystemMonitorController` 的 17 个端点中**无 `/info`**（`:37/43/50/58/64/70/76/82/88/97/103/109/116/122/129/136/143`）；且 `cn.aiedge.monitor` **不在 `scanBasePackages`**（README §2.3）→ **包没装配 + 路径也不存在**。运行态实测 404 `{"code":404,"message":"接口不存在: api/monitor/info"}`。**同一端点也被接口监控页 62203 调用（README §3.4 P0-3），属同一根因。**
③ **错误被静默吞掉后立即填假数据**：`catch {}`（`:145-153`）不提示、不标记，页内无任何错误态；仅全局拦截器弹一次 404 toast（`utils/request.ts:329-331`）。**用户无法从表格内容区分「真取到」与「兜底写死」** —— 这是本页最严重的可感知缺陷。建议：`catch` 内给出显式错误态（含重试），**不得回填业务数据**。
④ **两个文档跳转按钮在开发环境打开 SPA 自身**：`/swagger-ui/**` 与 `/doc.html` **未被 vite 代理**（`vite.config.ts:109-137` 仅 `/api`、`/api/erp/mall`、`/ws`）；且 `/swagger-ui/index.html` **不在 Sa-Token 放行清单**（`SaTokenConfig.java:60-64`、`:100-105`），**运行态实测 401**。→ 「Swagger UI」**双重不可用**；「Knife4j」**后端可达（200）但 dev 环境仍不可达**。建议：补 vite 代理 + 把 `/swagger-ui/**` 加入放行清单（或在文案中明确「本系统仅用 Knife4j」）。
⑤ **假 `basePath` 与后端不符**：写死的 `/api/auth`、`/api/user`、`/api/role`、`/api/base` 在本系统后端**零命中/不存在**（§6.5）→ 即使有人照着本页去找接口，也会扑空。建议：随 spec 化一并消除。

### P1（6 条）

⑥ **已有真实能力未接入（违反「不重复开发」）**：`GET /api/trade/api-monitor/calls/endpoints`（`ApiMonitorController.java:130-133` + `OpenApiCatalog.java:114`，前端调用点 `api/trade/index.ts:379`）能返回真实端点清单，本页**不调用**，另起一套写死清单。建议：复用（README §6「功能/模块不重复开发」）。
⑦ **空态文案是静态说明、非实时探测**：「API 文档已集成」（`:60`）、两行说明（`:64-67`）在 spec 实际不可达时**仍然显示为「已集成」**，属误导。建议：改为由真实探测结果驱动的状态文案。
⑧ **OpenAPI 工具链无显式配置**：core-api yml **零命中** `springdoc` / `knife4j` / `swagger`；行为靠依赖默认值（`/doc.html` 实测 200 说明 Knife4j 默认开启）。**生产环境无开关可关文档端点**。建议：按 profile 显式声明（**key 名须核 springdoc 官网，属 D 级提示**）。
⑨ **跳转按钮无节流**：`openSwagger` / `openKnife4j`（`:125-131`）连点可开多个标签页。建议：补节流或改站内路由。
⑩ **无按模块 / 按权限过滤**（§3.3 查询项 0 项）：底稿 §5.2「落地」建议平台级 API 文档页应能**按模块 / 按权限过滤**（**该建议为底稿自建，非规范条文**）。建议按 OpenAPI `tags` + 本系统权限码实现（**本系统设计**）。
⑪ **未达金标准路线 A**：无 `ErrorBoundary`、无列配置齿轮、无 `PageConfigPanel`、无经典分页（`:2`、`:34`、`:90-105`、`:118-123`）—— 与 README §7.2「30 页全部无 `PageConfigPanel` / 无 `storage-key` / 无经典分页 / 无列配置齿轮」一致。**注意优先级**：外壳升级**不解决** P0-①（假数据）。

### P2（6 条）

⑫ **无查询 / 无排序 / 无页内 Tab**（0 项，§3.3）。
⑬ **3 列未指定 `width`**（`:119`、`:120`、`:122`），布局稳定性差。
⑭ **`row-key="name"`** 对写死数据无冲突，但接入真实 spec 后 `name` 未必唯一，**需改为更稳的键**（如 `basePath` 或组合键）。
⑮ **无导出 / 无打印 / 无快捷键**（全文）。
⑯ **无本系统验收截图、无模块级 E2E**（README §7.3 末条）：建议随系统模块整体落地 `tools/e2e-system.cjs`。
⑰ **本页文案与实际能力不符**：同时宣传 `Swagger UI` 与 `Knife4j`，而前者在本系统**任何环境都不可用**（401），后者**仅后端可达**。建议二者取一并在文案中说明。
