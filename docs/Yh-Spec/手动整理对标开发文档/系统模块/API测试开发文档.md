# API测试开发文档

> **⚠️ 实施状态（2026-09-19）**：本页已按本文档实施并真机验证（脚本 `node tools/e2e-system.cjs`，67/67 通过）。**本文档 §9「实现差异说明」/ §12「剩余缺口」记录的是实施前的状态**；凡与《README.md》**§10「2026-09-19 实施结果」**冲突，一律以 README §10 为准。

> **文档类型：** 本系统独有页（ql361 无对标）→ 建模口径：本页是**平台运营方的 API 调试台**，其**核心不是 UI，而是安全边界** —— 按 **OWASP《Server-Side Request Forgery Prevention Cheat Sheet》** 定义边界，「受限的 API 测试台」为**本系统设计**，`client_type = system-admin`
> **本系统路由：** `admin/dev/api-test`（**单入口**，无表单页、无 `list_path`）
> **菜单：** ID `62404`｜`menu_code = system:dev:api-test`｜`client_type = system-admin`｜`path = admin/dev/api-test`｜`component = views/admin/dev/api-test`｜父菜单 `61305 开发工具`（上溯 `60013 系统`）｜`sort = 400`
> **对标页：** **ql361 无对标页（系统模块是平台级，ql361 是租户级产品）** —— 详 §2、§8
> **数据来源：** 本系统源码逐处核对（2026-09-18）＋ devdb 实测（`sys_menu` / `sys_permission`，2026-09-18 直连 psql）＋ 只读 GET 探测（`localhost:5655`）
> **通用规范：** 《ql361对标/对标开发技术参考文档》第 4、5 章；布局金标准见《交易模块/_开发指南-金标准.md》；模块全景见《系统模块/README.md》§1 三条红线、§3.4 **P0-11**、§7.1 第 **11** 条

---

## 1. 页面概述

API 测试页是**平台运营方的 API 调试台**：左栏填「请求方法 / 请求 URL / 请求头 / 请求体」，右栏展示「耗时 / 状态码 / 响应正文」。它是 `61305 开发工具` 组的第 3 页（`sort=400`），使用者是**平台研发与运营人员**。

**【注意】本页当前是「部分桩 + 功能失效」页**（README §3.2 分类）：它确实调用真实 axios，但请求目标（URL / 方法 / 请求体）被 `utils/request.ts` 的 `buildConfig` **静默丢弃**，实际只会发 `GET /api`；成功分支的状态码被**写死为 200**。详见 §5.2。

| 项 | 值 | 证据 |
|----|-----|------|
| 页面 | `admin/dev/api-test`（单入口） | `sys_menu.id=62404`，`menu_type=1`，`display_mode=0`（psql 实测） |
| 组件 | `frontend/apps/pc-admin/src/views/admin/dev/api-test/index.vue`（**170 行**） | `sys_menu.component=views/admin/dev/api-test` |
| 路由注册 | `admin/dev/api-test` → 该组件 | `router/dynamicRoutes.ts:269` |
| 外壳 | `PageContainer(full-height)` + `#header` 插槽 + `a-row`（左 `a-col:8` / 右 `a-col:16`），**无布局组件、无表格** | `api-test/index.vue:2`、`:3-20`、`:22`、`:23`、`:81` |
| 后端 | **未找到**（本页**不调用任何专用后端端点**；直接用当前用户 token 打通用 axios 实例） | 全后端 `grep "api-test"` 仅命中测试框架文档，无 Java 控制器 |
| 数据表 | **无**（本页不读写任何表） | 全文无接口封装、无 `api/*.ts` 引用 |
| 权限码 | 前端**零权限控制**（无 `v-permission` / `hasPermission`）；后端无对应端点 | `api-test/index.vue` 全文；`sys_permission` 实测 0 行命中 |
| 数据真实性 | **部分桩 + 功能失效**：真实发 axios，但目标被丢弃；成功状态码写死 200 | `api-test/index.vue:144-145`；`utils/request.ts:375-400` |

**1.1 与相邻页的分工（`61305 开发工具` 组 4 页）**

| 页面 | 菜单ID | 回答的问题 | 后端前缀 | 本页与其关系 |
|------|:-----:|-----------|---------|-------------|
| 模板管理 | 62402 | 「导入/导出模板长什么样、怎么维护」 | `/api/import-templates` | 无接线 |
| **API文档** | 62403 | 「系统有哪些接口、怎么调」——**接口目录（读）** | **未找到**（整页桩） | **同组相邻页，且共用同一个不存在的默认地址**：`api-doc/index.vue:136` 也打 `/monitor/info` |
| **API测试（本页）** | 62404 | 「我现在就发一个请求试试」——**调用台（写动作）** | **无专用端点** | 本页 |
| 定时任务 | 62405 | 「哪些任务在跑、跑成没跑成」 | `/api/scheduler/task` | 无接线 |

> **一句话分工**：本组四页里，**API 文档是"读目录"**（可复用 OpenAPI），**API 测试是"直接发请求"** —— 后者是**唯一一个"用户输入什么、系统就往哪发"**的页面，因此**它是本模块风险最高的功能**（README §7.2「开发工具」条）。
> **【注意】本页与 API 文档（62403）共享同一个失效默认值**：两页的默认/唯一目标都是 **`/api/monitor/info`**，而后端**不存在**该端点（§5.5 运行态实测 404）—— 这不是本页独有的偶发 bug，而是同一批"接口监控"遗留代码的连带。

**1.2 本页不提供的能力（如实登记）**

| 缺什么 | 现状 | 级别 |
|--------|------|:----:|
| **URL 真正生效** | 用户填的 `url` 被 `buildConfig` 丢弃（`utils/request.ts:375-400`）→ 恒发 `GET /api` | **P0** |
| **请求方法真正生效** | 用户选的 `method` 被丢弃 → 恒为 `GET`（axios 默认值） | **P0** |
| **请求体真正生效** | `config.data` 被复制，但方法为 GET → **请求体不发送**；且仅 `POST`/`PUT` 才赋值 | **P0** |
| **真实的响应状态码** | 成功分支写死 `statusCode.value = 200`（`:145`），不读真实响应码 | **P0** |
| **URL / 方法白名单** | **均无**（无任何校验代码、无后端代理层） | **P0** |
| **内网网段屏蔽 / 重定向跟随控制** | **均无**（无 IP 解析、无 denylist、无 `maxRedirects`；浏览器侧由 XHR 自动跟随） | **P0** |
| **审计流水** | **无**（不发审计、不落表；`sys_audit_log` 实测 **0 行**） | **P0** |
| **响应大小上限 / 超时上限（本页可控）** | **均无**：响应体原样 `JSON.stringify` 后整段塞进 `pre`；超时仅有通用 axios 实例的 `timeout: 30000`（`utils/request.ts:94`），本页**不可配** | P1 |
| **响应体不落盘** | 【符合】本来就不落盘（无下载/无历史），但**也无"响应历史"** —— 刷新即丢 | — |
| **请求历史 / 保存的请求集合 / 环境变量 / 认证管理** | **均无**（无 storage、无表；对比 Postman 类工具的 Environments / Auth 面板） | P1 |
| **响应格式化（JSON 折叠、语法高亮、搜索）** | **无** —— 只有 `pre` 原样文本（`:97`，样式 `:157-170`） | P2 |
| **复制响应 / 下载响应 / 清空响应** | **无**（页头无按钮，右栏 `#extra` 只有两个只读标签） | P2 |
| **请求头增删行 UI / Curl 代码片段生成** | **均无**：请求头只有一个 3 行文本域，按行 `:` 解析（`:129-138`），无键值对表格；无 Curl 生成 | P2 |
| **非法 JSON 的独立错误提示** | **无** —— `JSON.parse` 落入同一 `catch`，被显示成「500 + SyntaxError」 | P1 |

---

## 2. 页面截图

**N/A —— 本模块无对标系统截图。**

**原因（本模块的通用前提，本页严格遵守）**：ql361（来肯企汇 v2.2，`22stable.ql361.com`）是**租户级进销存产品**，它对客户暴露的只有「设置」域（`client_type = tenant-admin`）。本页属于**平台控制台**（`client_type = system-admin`）——「平台方自己的研发调试工具」——**任何厂商都不会把自己的 SaaS 控制台（含内部调试台）给客户看**。因此：

- ql361 中**不存在**「API测试」这个页面（`sys_menu` 里 `client_type='system-admin'` 的行不会出现在普通租户登录的菜单树中），**无可对标截图**；
- **不使用 Odoo / SAP / 金蝶 / 用友的界面截图**（口径见《系统模块/README.md》§5.3：本模块按业界**能力模型**建模，**不照搬任何一家的界面**）；
- §8 只写**业界能力对标**，逐条标来源与可信度级别。

**本系统截图**：当前无本系统验收截图（`tools/` 与 `tools/acceptance/` 下不存在 `e2e-system-*.cjs` / `e2e-admin-*.cjs`）→ **P2 缺口**（README §7.3）。

**数据现状（2026-09-18 实测）**：

```
本页不读写任何表：无接口封装文件、组件内零 useRequest / 零 onMounted / 零表格数据源；
  唯一数据来源 = 用户点击「发送请求」后的 axios 返回值（渲染到 pre）

默认目标地址不存在（运行态实测）：
  带超管 token GET /api/monitor/info → 404 {"code":404,"message":"接口不存在: api/monitor/info"}
  阳性对照：GET /api/config/list → 200；GET /api/menu/tree → 400「缺少必要参数: tenantId」
  无 token 探测（本机 5655 端口，本轮复核）：任意路径（含明显不存在的路径）
    → 一律 HTTP 401 {"code":401,"message":"请先登录"}
    （Sa-Token 拦截器先于路由匹配生效 → 不登录无法凭状态码判定端点是否存在）
```

- **静态佐证**：`SystemMonitorController`（`backend/core/api/core-api/src/main/java/cn/aiedge/monitor/controller/SystemMonitorController.java`，类级 `@RequestMapping("/api/monitor")` 见 `:26`）实测 **17 个端点**中**无 `/info`**（端点行号 `:37/43/50/58/64/70/76/82/88/97/103/109/116/122/129/136/143`）；
- **连带**：同组《API文档开发文档》的唯一真实分支也指向该地址（`api-doc/index.vue:136`），接口监控页同样打 `/monitor/info`（`views/admin/monitor/api/index.vue:251`）→ 三页共用一个死链（README §3.4 **P0-3**、**P0-13**）；**即使 URL 透传修好，本页默认填充值仍是死链**（§5.5）。

---

## 3. 列表页配置

**本页无列表、无表格、无查询条件、无分页、无行操作、无弹窗。** 本节按本页形态（**「请求配置」+「响应结果」左右两栏**）改写，逐项给出字段清单与行号。

### 3.1 页面外壳（`api-test/index.vue`，170 行）

```
PageContainer(full-height)(:2)                      ← 唯一外壳，闭合 :101
└─ #header 插槽(:3-20)：a-breadcrumb(:6-14)「首页(:7-11) / 系统管理(:12) / API测试(:13)」
          + h2.page-header-title「API测试」(:15-17)（**页头右侧无任何按钮**）
└─ a-row :gutter="16"(:22)
   ├─ a-col :span="8"(:23) → a-card「请求配置」(:24-79)：:bordered="false"(:25)、title(:26)
   │   └─ a-form layout="vertical"(:28-78)
   │       ├─ a-select「请求方法」(:29-44)｜a-input「请求URL」(:45-50)
   │       ├─ a-textarea :rows="3"「请求头」(:51-57)｜a-textarea :rows="6"「请求体」(:58-64)
   │       └─ a-button「发送请求」(:65-77)
   └─ a-col :span="16"(:81) → a-card「响应结果」(:82-98)：:bordered="false"(:82)、title(:83)
       ├─ #extra(:86-96)：a-tag 耗时「{{ responseTime }}ms」(:87-89)
       │                + a-tag 状态码(:90-95，:color="statusCode < 400 ? 'green' : 'red'")
       └─ pre.response-body：{{ response }}(:97)
```

| 项 | 当前实现 | 金标准（路线 A/A′） | 差距 |
|----|----------|-----------------|------|
| 外壳顶层 | `PageContainer(full-height)`（`:2`） | `ErrorBoundary > PageContainer(full-height)` | 【不符合】**缺页面级 `ErrorBoundary`**（全局包裹在 `App.vue:2-10`） |
| 布局组件 | **无**（`a-row` + 两张 `a-card`） | 本页是**工具页**（无列表/无单据），路线 A 的 `CategoryListLayout` / `BillDetailTable` **形态不适用**；应走「左右分栏工具页」形态 | 【不适用】形态特殊，**不得强套列表骨架** |
| 表格 / 分页 / 列配置齿轮 / 行选择 / 行操作 | **全部无**（不使用任何 `columns` 数组；`formatter` / `defaultHidden` / `slotName` **均不适用**） | 不适用 | — |
| 页面配置弹窗 / 页内 Tab / 统计卡 / 图表 | **全部无**（无 `PageConfigPanel`、无 `storage-key` / `global-config-key`、无 `a-tabs`、无图表、无 `ARReportPage`；全文零命中） | 不适用 | — |
| 页头按钮 | **无** | 建议补「清空响应 / 复制响应 / 请求历史」（P2） | P2 |

> **判定**：本页外壳是「**裸 `PageContainer` + `a-row` 两栏 `a-card`**」，**不适用路线 A 的列表骨架**（无表格、无列表数据），与 README §3.1「30 页全部未达金标准路线 A」口径一致 —— 但本页的**核心缺口不在外壳，而在 §5.2 的请求链路失效与安全边界**。

### 3.2 页内 Tab

**本页无页内 Tab**：全文未使用 `a-tabs`（grep 零命中）。

### 3.3 左栏「请求配置」字段清单（**4 个输入项，全部非必填**）

| # | label（逐字） | 控件 | 绑定 | `required` / 校验 | 默认值 | 行号 |
|:-:|--------------|------|------|:-----------------:|--------|------|
| 1 | `请求方法` | `a-select` | `v-model:value="method"`（`:30`） | **无** | `GET`（`:109`） | `:29-44` |
| 2 | `请求URL` | `a-input` | `v-model:value="url"`（`:47`） | **无** | `/api/monitor/info`（`:110`） | `:45-50`（placeholder `/api/...` `:48`） |
| 3 | `请求头` | `a-textarea :rows="3"` | `v-model:value="headers"`（`:53`） | **无** | 空串（`:111`） | `:51-57`（placeholder 两行文本 `:55`） |
| 4 | `请求体` | `a-textarea :rows="6"` | `v-model:value="body"`（`:60`） | **无** | 空串（`:112`） | `:58-64`（placeholder `{"key": "value"}` `:62`） |

- **表单无 `:model`、无 `:rules`**（`:28` 只有 `layout="vertical"`）→ **零校验**；`sendRequest` 里唯一的守卫是 `if (!url.value) return`（`:119`，且**无任何提示**，用户会以为按钮没反应）。
- **「请求方法」下拉选项逐字**（`:30-43`）：`GET`（`:31-33`）、`POST`（`:34-36`）、`PUT`（`:37-39`）、`DELETE`（`:40-42`）——**4 个值，无 `PATCH`、无 `HEAD`、无 `OPTIONS`**。
- **「请求头」解析口径**（`:129-138`）：按 `\n` 切行 → 每行 `indexOf(':')` → `idx > 0` 时以 `key.trim()` / `value.trim()` 写入对象 → 整体赋给 `config.headers`。**无格式校验**（无 `:` 的行被静默忽略）。
- **「请求体」解析口径**（`:140-142`）：仅当 `method === 'POST' || method === 'PUT'` **且** `body` 非空时 `JSON.parse(body.value)` 赋给 `config.data`。**`DELETE` 选中的请求体被丢弃**；**非法 JSON 无独立 `try/catch`**（§5.2 风险表末行）。

### 3.4 右栏「响应结果」展示字段清单（**3 项**）

| # | 展示项 | 控件 | 取值 | 行号 |
|:-:|--------|------|------|------|
| 1 | 耗时 | `a-tag`（`v-if="responseTime"`） | `{{ responseTime }}ms`；`Date.now()` 差值（`:121` 起 / `:151` 止），**非定时器** | `:87-89` |
| 2 | 状态码 | `a-tag`（`v-if="statusCode"`），`:color="statusCode < 400 ? 'green' : 'red'"` | 成功分支**写死 200**（`:145`）；异常分支 `e?.response?.status \|\| 500`（`:148`）；初值 `0`（`:115`） | `:90-95` |
| 3 | 响应正文 | `pre.response-body` | `JSON.stringify(res, null, 2)`（成功 `:146`）/ `JSON.stringify(e?.response?.data \|\| e.message \|\| '请求失败', null, 2)`（失败 `:149`） | `:97`（样式 `:157-170`：底色 `#1e1e1e`、`min-height: 400px`、`max-height: 600px`、`overflow: auto`、`font-size: 13px`） |

- **初始占位文案与初值**（`:114-116`，逐字）：
  ```
  const response = ref('点击"发送请求"查看响应')
  const statusCode = ref(0)
  const responseTime = ref(0)
  ```
  → 初次进入页面，右栏 `#extra` **两个 `a-tag` 都不渲染**（`statusCode=0` 与 `responseTime=0` 均为假值），正文显示「点击"发送请求"查看响应」，**但按钮与文案不一致（按钮上写「发送请求」，文案写「发送请求」同字，属巧合一致）**。
- **无复制按钮、无清空按钮、无下载按钮、无折叠/高亮** —— 响应只是纯文本 `pre`。

### 3.5 功能按钮（**全页仅 1 个**）

| 位置 | 按钮 | 权限码 | 接线状态 | 行号 |
|------|------|--------|---------|------|
| 左栏表单末行 | `发送请求`（`type="primary"` `:loading="sending"` `block`，图标 `SendOutlined`） | **无** | **真实发起 axios 请求**（`request.request(config)`，`:144`），但**用户填的 URL / 方法 / 请求体被静默丢弃** | `:65-77`（`@click="sendRequest"` `:70`；图标 `:72-74`；函数体 `:118-154`） |

**桩按钮清单**：**无**。本页**唯一按钮是真实发请求**（不是弹提示的桩）—— 缺陷在**参数被丢弃**，不在"按钮是假的"。

> **【注意】纪律提示（本页专属，必须随文档传递）**：**编写/执行任何验收脚本时，不得真的点击「发送请求」** —— 该按钮会**带当前用户 token** 打真实请求（§5.2）。只允许在**受控只读端点**上执行（§10.1 的安全前置）。

### 3.6 表格列

**N/A —— 本页无表格。** 本页不使用任何 `columns` 数组，`formatter` / `defaultHidden` / `slotName` / `type` **均不适用**；也不存在 `BillDetailTable` / `BillTableList`。

### 3.7 行操作

**N/A —— 本页无行、无行操作。**

### 3.8 弹窗 / 抽屉

**N/A —— 本页无 `a-modal`、无 `a-drawer`。** 所有输入都在左栏**内联表单**里（§3.3），所有输出都在右栏**内联 `pre`** 里（§3.4）。

### 3.9 自动刷新与资源清理

| 项 | 结论 | 证据 |
|----|------|------|
| `setInterval` / `setTimeout` | **0 处**（全文 grep 零命中）；耗时用 `Date.now()` 差值，**不是定时器** | `:121`、`:151` |
| `onMounted` / `onUnmounted` / `onBeforeUnmount` | **全部无** | 全文 |
| 内存泄漏 | **无定时器、无监听器、无观察者**，不存在需清理的资源 → **无泄漏** | 同上 |
| 请求取消 | **无 `AbortController` / 无 `CancelToken`** → 请求发出后**无法取消**；重复点击「发送请求」会**并发发出多个请求**（`sending` 只控 `:loading` 视觉，不阻断再次点击） | `:66-76`、`:144` |

---

## 4. 表单页配置

**本页无表单页**：菜单 `display_mode = 0`（单入口）、`list_path` 为空（devdb 实测），本页自身即全部界面。

**页内为「内联请求表单」而非档案/单据表单**，4 个字段全部非必填、零校验（清单见 §3.3），**唯一提交动作**是「发送请求」（§3.5）。

**提交行为（`sendRequest`，`:118-154`）**：

| 阶段 | 行为 | 行号 |
|------|------|------|
| 前置守卫 | `if (!url.value) return`（**无提示**） | `:119` |
| 置加载态 | `sending.value = true` | `:120` |
| 计时起点 | `const startTime = Date.now()` | `:121` |
| 组装 config | `const config: any = { method: method.value, url: url.value }` | `:124-127` |
| 请求头 | 文本域按行解析 → `config.headers`（仅 `headers.value` 非空时） | `:129-138` |
| 请求体 | 仅 `POST`/`PUT` 且 `body` 非空 → `JSON.parse` → `config.data` | `:140-142` |
| 发起 | `const res = await request.request(config)` | `:144` |
| 成功分支 | `statusCode.value = 200`（**写死**）+ `response.value = JSON.stringify(res, null, 2)` | `:145-146` |
| 失败分支 | `statusCode.value = e?.response?.status \|\| 500` + `JSON.stringify(e?.response?.data \|\| e.message \|\| '请求失败', null, 2)` | `:147-149` |
| 收尾 | `responseTime.value = Date.now() - startTime`；`sending.value = false` | `:150-153` |

**目标规格（本页缺、业界调试台常见，★ 标注）**：
- ★**请求历史持久化**（当前刷新即丢）
- ★**保存的请求集合 / 文件夹**（对标 Postman Collections）
- ★**环境变量与变量替换**（`{{baseUrl}}` 之类）—— **本系统口径：明确不借鉴**（见 §8.5）
- ★**认证面板**（Bearer / API Key / Basic 的结构化输入，替代手写文本域）
- ★**Curl / 代码片段生成**（把当前请求导出为可复用命令）
- ★**响应体折叠、语法高亮、大小与耗时明细**

> **不发明字段承诺**：以上 ★ 项**本系统一个都没有实现**，且**本页无后端实体、无表** —— 若要落地历史/集合，**必须新增表**（§7.3、§11.4 已显式标注）。
> **表单布局选型提醒**：本页是**工具页内联表单**，既非「单据」也非「档案（主数据）」，因此《表单布局选型：单据 vs 档案》的分区卡片规则**不适用**；`layout="vertical"` 的单列表单在此形态下是可接受的。

---

## 5. 业务规范

### 5.1 状态值域

| 场景 | 选项（名 / 值） | 位置 |
|------|----------------|------|
| 「请求方法」下拉 | `GET` / `POST` / `PUT` / `DELETE`（**4 值**） | `api-test/index.vue:31-42` |
| 状态码 Tag 颜色 | `statusCode < 400` → `green`；否则 → `red` | `:92` |
| 状态码初值 | `0`（`a-tag` 因 `v-if="statusCode"` 不渲染） | `:115`、`:90-95` |
| 耗时初值 | `0`（`a-tag` 因 `v-if="responseTime"` 不渲染） | `:116`、`:87-89` |
| 响应初值 | 字符串 `点击"发送请求"查看响应` | `:114` |

**无其它状态字段**：本页无单据状态、无审批状态、无启用/禁用开关。

**【注意】值域与后端枚举无对照关系**：本页方法值域是**前端字面量**（`a-select-option value="..."`），**没有对应后端枚举**（本页无后端端点），因此不存在"前端枚举与后端枚举列数不一致"这类问题。

### 5.2 ★ P0-11-① 请求链路：URL / 方法 / 请求体被 `buildConfig` 静默丢弃

**结论：用户填的 URL、方法、请求体三者全部无效；页面唯一生效的输入是「请求头」。实际永远请求 `GET /api`。**

**(1) 页面发送逻辑原文（`api-test/index.vue:118-154`，照录）**

```js
async function sendRequest() {
  if (!url.value) return
  sending.value = true
  const startTime = Date.now()

  try {
    const config: any = {
      method: method.value,
      url: url.value,
    }

    if (headers.value) {
      const headerObj: Record<string, string> = {}
      headers.value.split('\n').forEach(line => {
        const idx = line.indexOf(':')
        if (idx > 0) {
          headerObj[line.slice(0, idx).trim()] = line.slice(idx + 1).trim()
        }
      })
      config.headers = headerObj
    }

    if ((method.value === 'POST' || method.value === 'PUT') && body.value) {
      config.data = JSON.parse(body.value)
    }

    const res = await request.request(config)
    statusCode.value = 200
    response.value = JSON.stringify(res, null, 2)
  } catch (e: any) {
    statusCode.value = e?.response?.status || 500
    response.value = JSON.stringify(e?.response?.data || e.message || '请求失败', null, 2)
  } finally {
    responseTime.value = Date.now() - startTime
    sending.value = false
  }
}
```

**(2) 链路证据（逐跳，全部带行号）**

| 跳 | 位置 | 事实 |
|:--:|------|------|
| ① | `api-test/index.vue:144` | 调 `request.request(config)`，`config` 含 `method` / `url` / `headers?` / `data?` |
| ② | `utils/request.ts:473-477` | `request.request()` 先经 `const finalConfig = buildConfig(config)`（`:474`），再 `return service.request(finalConfig)`（`:476`） |
| ③ | `utils/request.ts:375-400` | **`buildConfig` 只复制 6 个键**：`retryConfig`（`:380-390`）、`headers`（`:393`）、`responseType`（`:394`）、`params`（`:395`）、`data`（`:396`）、`_skipAuthRefresh`（`:397`）—— **`url` 与 `method` 均未复制**（函数体 `:375-400` 全文可核） |
| ④ | `utils/request.ts:92-98` | `service = axios.create({ baseURL: '/api'（`:93`）, timeout: 30000（`:94`）, headers: {'Content-Type':'application/json'}（`:95-97`） })` |
| ⑤ | axios 1.16.1 `axios.cjs:2080-2088` / `:2098-2100` / `:2112-2118` | `isAbsoluteURL(undefined)` → `typeof url !== 'string'`（`:2084-2086`）→ **`false`**；`buildFullPath` 中 `isRelativeUrl = true`（`:2113`）→ 走 `combineURLs(baseURL, requestedURL)`（`:2114-2115`）；`combineURLs('/api', undefined)` 因 `relativeURL` 为假值 → **返回 `baseURL` 本身，即 `/api`**（`:2099`） |
| ⑧ | 结论 | 最终请求 = **`GET /api`**（`method` 未传 → axios 默认 `get`）；**请求体因方法为 GET 而不发送**（axios 对 GET 会丢弃/序列化 `data`） |

> **本轮已实读源码核实**：`axios.cjs` 的 `isAbsoluteURL`（`:2080-2088`）、`combineURLs`（`:2098-2100`）、`buildFullPath`（`:2112-2118`）三处行号与语义**均已逐行核对**（`frontend/apps/pc-admin/node_modules/axios/package.json:3` 实测版本 `"version": "1.16.1"`），**非推论**。

**(3) 唯一生效的输入是请求头**

`headers` 被 `buildConfig` 原样透传（`utils/request.ts:393`）→ 用户在文本域写的**非 `Authorization` 头**会真的随请求发出（例：`X-Forwarded-For`、`X-Real-IP`、自定义业务头）。**`Authorization` 会被请求拦截器覆盖**（`utils/request.ts:121-123`）→ 用户手写的 `Authorization` 无效。

### 5.3 ★ P0-11-② 安全风险（逐项给证据）

| 风险项 | 现状 | 证据 |
|---|---|---|
| **是否带当前用户 token** | **带**。请求拦截器**无条件注入** `Authorization: Bearer <token>`（只要 `getToken()` 有值） | `utils/request.ts:121-123`；另注入 `tenantId`（`:128`）与 `X-Tenant-Id`（`:129`），值取 `localStorage.getItem('tenantId') \|\| '1'`（`:127`） |
| **任意 URL 请求 / SSRF 面** | **当前代码下不成立**（`url` 被 `buildConfig` 丢弃，见 §5.2）；**但设计面成立**：文本框可填绝对 URL，若 `url` 透传生效，axios 的 `isAbsoluteURL` 会判定为绝对地址并**绕过 `baseURL` 直连任意域**（含浏览器可达的内网地址，仅受 CORS 约束） | `api-test/index.vue:45-50`（输入框无限制）；`axios.cjs:2080-2088`（`isAbsoluteURL`）、`:2112-2118`（`buildFullPath` 对绝对 URL 原样返回 `requestedURL`） |
| **凭据外泄向量** | **潜在**：若 `url` 透传生效，跨域绝对 URL 会连带把 `Authorization: Bearer` 发给第三方域（攻击者只需在自己的 CORS 响应头里放行该自定义头，浏览器即真实发出） | `api-test/index.vue:125-126`（用户可填任意 URL）+ `utils/request.ts:121-123`（token 无条件注入） |
| **请求头可注入** | **是**：文本域按行解析、`:` 分割成对象后**整体**作为 `config.headers`（`:129-138`），`buildConfig` 原样透传（`utils/request.ts:393`）。`Authorization` 会被拦截器覆盖（`:121-123`），但**任意其它头**（如 `X-Forwarded-For`、`X-Real-IP`、框架内部头）会随请求发往后端 | `api-test/index.vue:129-138`；`utils/request.ts:393` |
| **方法可选** | 是，`GET`/`POST`/`PUT`/`DELETE`（`:30-43`）；**因 `method` 被丢弃，当前恒为 `GET`** | `api-test/index.vue:30-43`；`utils/request.ts:375-400` |
| **放行范围** | **无 URL 白名单、无方法白名单、无域名限制、无网段屏蔽、无重定向控制、无审计日志**。生效形态等于「**用当前用户会话 token 直接打任意 `/api/**`**」，绕过 UI 层的能力门槛（**唯一门槛是菜单 `client_type='system-admin'` 决定谁看得见本页**） | 全页 grep：**0 处** `v-permission` / `hasPermission` / 权限码常量；**无后端控制器**（全后端 `grep "api-test"` 仅命中测试框架文档） |
| **响应泄露** | 任意接口响应体**原样** `JSON.stringify` 打印到页面（`:146`、`:149`），**无字段脱敏、无大小上限、无"敏感接口"标记** | `:146`、`:149`、`:97` |
| **状态码真实性** | 成功分支把状态码**写死为 200**（`:145`）→ **并非真实响应码**；用户无法从状态码 Tag 判断真实 HTTP 状态 | `:144-145` |
| **异常处理误导** | `JSON.parse(body.value)`（`:141`）**无独立 `try/catch`** → 非法 JSON 会落入同一个 `catch`，显示为「**500 + SyntaxError 文案**」，**误导用户以为是服务端错误** | `:140-149`（`:141` 抛错 → `:147-149` 兜底 `e?.response?.status \|\| 500`） |
| **无审计** | 本页不写任何审计流水，`sys_audit_log` 实测 **0 行** → 「谁在什么时候拿什么 token 打了哪个接口」**事后无法还原** | README §1.3 红线③、§3.5；devdb 实测 |

> **一句话定性**：本页当前是「**功能失效但相对安全**」（因为 `url` 被丢弃，请求只会落在同源 `/api`）；**一旦按直觉"修好透传"，它会立刻变成「带当前用户 token 的任意 URL 请求器」** —— 即 **SSRF + 凭据外泄 + 请求头注入**三重风险（README §3.4 **P0-11** 原文口径）。**加固必须先于透传修复落地**（§8.4）。

### 5.4 取值口径

| 项 | 口径 | 证据 |
|----|------|------|
| 「请求方法」/「请求URL」取值 | 前端字面量 / 自由文本，直传 `config.method` / `config.url`（**两者当前均被丢弃**） | `:30-43`、`:46-48`、`:125-126` |
| 「请求头」取值 | 文本域 → 按行 `:` 分割 → 对象；**重复键后者覆盖前者**（对象赋值语义） | `:129-138` |
| 「请求体」取值 | **仅** `method === 'POST' \|\| 'PUT'` 且非空时 `JSON.parse` | `:140-142` |
| 耗时 | `Date.now()` 差值，含 axios 请求 + 响应处理全程；**无分阶段计时**（无 DNS/TTFB/下载细分） | `:121`、`:151` |
| 状态码 | 成功分支**常量 200**（不读真实值）；失败分支取 `e?.response?.status` | `:145`、`:148` |
| 响应正文 | 成功 = axios 返回体（经 `request.ts` 响应拦截器处理后的形态）；失败 = `e.response.data` 或 `e.message` 或 `'请求失败'` | `:146`、`:149` |

### 5.5 默认填充值与死链（实测）

| 项 | 值 | 证据 |
|----|-----|------|
| 默认方法 | `GET` | `api-test/index.vue:109` |
| 默认 URL | `/api/monitor/info` | `api-test/index.vue:110` |
| 该地址是否存在 | **不存在** —— 带超管 token 运行态实测 **404** `{"code":404,"message":"接口不存在: api/monitor/info"}`（2026-09-18）；静态佐证：`SystemMonitorController.java:26` 类级 `/api/monitor`，**17 个端点无 `/info`**（`:37/43/50/58/64/70/76/82/88/97/103/109/116/122/129/136/143`） | 见 §2「数据现状」 |
| 结论 | **即使 URL 透传修好，默认填充值仍是死链** —— 与同组《API文档》（`api-doc/index.vue:136`）共享同一失效目标 | 同上 |

> **探测方法说明（诚实登记）**：无 token 时本机 5655 端口对**任意路径**都返回 `401 {"code":401,"message":"请先登录"}`（Sa-Token 拦截器先于路由匹配）→ **未登录探测无法判定端点是否存在**；上表的 404 结论来自**带超管 token** 的运行态实测记录。**本轮未执行任何写端点，也未点击页面「发送请求」。**

### 5.6 权限口径

| 层 | 现状 | 证据 |
|----|------|------|
| 前端 / 后端 | 前端**零权限控制**（无 `v-permission`、无 `hasPermission`、无权限码常量、无 `usePermission`）；后端**无本页专用端点** → **无任何权限注解可生效** | `api-test/index.vue` 全文（170 行）；全后端 `grep "api-test"` 零命中（仅测试框架文档） |
| 权限码落库 | `sys_permission` 实测 **264 行**；`system:dev%` 命中 **5 行**且**全部是 `system:dev:scheduler:*`**；**`system:dev:api-test%` 命中 0 行** | devdb 实测 |
| **实际门槛** | **菜单可见性**：`sys_menu.id=62404` 的 `client_type='system-admin'`（psql 实测）→ 只有平台侧超管会话能拿到该菜单（`SysMenuServiceImpl.java:250` 判系统租户、`:255` 判 `SUPER_ADMIN`、`:259-265` 走超管分支、`:331-339` 的 Wrapper 为 `client_type IN ('tenant-admin','system-admin')` 且 `status=1`、`visible=1`、`deleted=0`） | 同上 |
| 前端请求的 client_type | 前端**固定** `CLIENT_TYPE = 'tenant-admin'`（`router/dynamicRoutes.ts:7`），请求 `/menu/user/mega/tenant-admin?userId=…&tenantId=…`（`:1150`）→ **平台菜单是靠后端在系统租户 + 超管分支下"多返回一批"而出现的，不是前端另开一套控制台** | `dynamicRoutes.ts:7`、`:1150` |
| **实际后果** | 超管（系统租户）可见并可打开本页；**非超管账号在菜单树里看不到本页**（无 `sys_role_menu` 绑定 → `finalMenuIds` 不含 62404）。**本页发出请求后，后端只校验登录态（Sa-Token），不校验任何 API 测试动作权限** | `SysMenuServiceImpl.java:266-272`、`:289-292` |

### 5.7 多租户口径（本模块红线②，本页形态特殊）

**本页不直接读任何库表**，因此「平台表被租户插件过滤 → 查回 0 行」在这里**不会表现为空表**；但**它发出的请求要经过通用 axios 通道**，该通道**无条件注入 `tenantId` / `X-Tenant-Id`**（`utils/request.ts:127-129`，值 = `localStorage.getItem('tenantId') || '1'`）→ 因此：

| 事实 | 证据 |
|------|------|
| 本页**无表可查**，红线②在本页**不直接适用** → 写「N/A（本页不直接读库）」 | 本页全文无 DB 访问 |
| 但走通用通道时**会带上 `tenantId` 头**，后端据此决定会话租户 → 用户"打的接口别人能不能看到"受此影响 | `utils/request.ts:127-129` |
| **超管整体豁免已接线并生效**（本页请求因此对平台表也能读到数据）：写入点 `SysUserServiceImpl.java:88` → `StpUtil.getSession().set("tenantScopeExempt", StpUtil.hasRole("SUPER_ADMIN"))`；读取点 `AiReadyTenantLineInnerInterceptor.java:41-44` → `MyBatisPlusConfig.java:157-166` 判定豁免时**直接 return，不注入 `tenant_id`** | 同上 |
| **【注意】红线本体仍在**：**非超管**的平台运营账号仍被注入 `tenant_id = <会话租户>`；且**旧 token**（改动前已签发）无 `tenantScopeExempt` 标记 → 需**重新登录一次**（`MyBatisPlusConfig.java:150-152` 注释明写）。表现形态是**页面空白 / 查不到数据，而不是报错** | 同上 |
| **对本页的具体含义** | 本页若用于调试平台表接口，**超管新 token 下能看到数据、旧 token 下会看到空** —— 用户会误以为「接口坏了」，实为租户过滤（**这是本页最容易误判的失败模式**） | 同上 |

**修法（与本模块一致）**：本页**不新增表**，无需豁免；但**调试目标接口若为平台表**，验收必须写死「用超管 + 重新登录」（§10.1）。读全局数据用 `@InterceptorIgnore(tenantLine = "true")`；写目标租户行前 `MyBatisPlusConfig.setTempTenantId(tid)`。

### 5.8 与其它页的关系

| 方向 | 关系 | 证据 |
|------|------|------|
| API测试（62404）→ API文档（62403）/ 接口监控（62203） | **三页共用同一个失效目标 `/api/monitor/info`**；后两者分别是「半桩页」与「整页桩」（README §3.4 P0-3、P0-13） | `api-test/index.vue:110`；`api-doc/index.vue:136`；`views/admin/monitor/api/index.vue:251` |
| API测试（62404）→ 交易模块 API 监控 | **零关系**：后者走 `/api/trade/api-monitor/*` 13 端点 + `api_access_log` 表（63 行），是**另一套完全不同的实现**（README §2.5） | README §2.5 |
| API测试（62404）→ 全站任何页 | **无接线**：本页不发审计、不写表、不影响任何业务状态 | 全文 |

---

## 6. 后端接口清单

**结论：本页无专用后端端点 —— 它通过通用 axios 通道直接把请求打出去。**

### 6.1 本页已调用的接口（**1 个，且路径由 axios 默认值决定**）

| # | 方法 | 路径 | 入参 | 返回 | 前端封装 | 行号 |
|:-:|------|------|------|------|---------|------|
| 1 | **GET** | **`/api`** | `headers`（被 `buildConfig` 保留的部分，含拦截器注入的 `Authorization` / `tenantId` / `X-Tenant-Id`） | 任意（由命中的路由决定；`/api` 本身 404） | `request.request(config)`（`utils/request.ts:473-477`） | 调用点 `api-test/index.vue:144`；链路 `utils/request.ts:474`→`:476`→`:375-400` |

> **说明**：`config` 里虽有 `method` / `url`，但二者在 `buildConfig`（`utils/request.ts:375-400`）阶段**未被复制** → 落到 axios 实例默认值（`baseURL: '/api'` `:93`、默认方法 `get`）。**因此该行"路径"栏写的是链路推导结果，不是用户输入值**。

### 6.2 前端"声明了、但当前永不生效"的输入（**3 个**）

| # | 输入 | 声明位置 | 期望语义 | 当前结局 | 级别 |
|:-:|------|---------|---------|---------|:----:|
| 1 | `请求URL` 文本值 | `api-test/index.vue:110`（默认 `/api/monitor/info`）、`:47`（`v-model`） | 作为 `config.url` 发请求 | **被 `buildConfig` 丢弃** → 恒发 `/api` | **P0** |
| 2 | `请求方法` 下拉值 | `:109`（默认 `GET`）、`:30` | 作为 `config.method` | **被丢弃** → 恒为 `GET` | **P0** |
| 3 | `请求体` 文本值 | `:112`（空）、`:60` | 作为 `config.data` | `data` **被复制了**，但方法为 GET → **不发请求体**；且仅 `POST`/`PUT` 才赋值 | **P0** |

> **另登记一条**：默认填值 `/api/monitor/info` 本身**在后端不存在**（§5.5）→ **即使 #1 修好，该默认值仍是 404**。

### 6.3 后端有、本页未用的端点（本页相关的近邻）

| # | 端点族 | 数量 | 本页为何未用 | 建议 |
|:-:|--------|:---:|-------------|------|
| 1 | `SystemMonitorController`（`/api/monitor`） | **17** | 本页**不调任何专用端点**；该控制器是「接口监控」页的设计目标 | 【注意】该控制器所在包 `cn.aiedge.monitor` **不在 `scanBasePackages`**（`AiReadyApplication.java:21-96`）→ **运行期全 404**（README §3.4 **P0-1**）。**本页若真去调它，一样 404** |
| 2 | 平台侧通用端点族（`/api/config`、`/api/menu`、`/api/scheduler/task` 等） | — | 本页**不预置任何示例请求**，用户需自己手填 URL（且当前填了也不生效） | 建议首版「受限 API 测试台」内置**只读端点下拉**（§8.4） |

### 6.4 前端在调、后端不存在（**1 个**）

| # | 前端位置 | 调用的路径 | 后方是否存在 | 后果 |
|:-:|---------|-----------|:-----------:|------|
| 1 | `api-test/index.vue:110`（默认填值） | `/api/monitor/info` | **不存在**（`SystemMonitorController` 17 端点无 `/info`） | 若 `url` 透传修好 → **必然 404**（运行态实测已确认：带超管 token → `{"code":404,"message":"接口不存在: api/monitor/info"}`） |

> **注意**：严格说这条**当前不是"前端在调"**（因为 `url` 被丢弃，实际请求的是 `/api`）。列在这里的意义是：**它是本页声明的目标，且一旦修好透传就会立刻成为必然 404** —— 属"**修链路时必须同步修正的既有缺陷**"。

### 6.5 后端缺口（本页相关）

| 缺口 | 说明 | 级别 |
|------|------|:----:|
| 【不符合】**无代理 / 网关端点** | 业界安全的 API 调试台应有**服务端代理**（在服务端做 allowlist 校验、剥离凭据、限流限大小）——本页**直接在浏览器里发请求**，**没有服务端代理层** | **P0** |
| 【不符合】**无白名单校验 / 无网段屏蔽** | 无 URL / 方法 / 域名白名单，无 IP 解析与私网/链路本地地址拦截（前后端均无） | **P0** |
| 【不符合】**无审计写入** | 不写任何审计流水（`sys_audit_log` 实测 **0 行**） | **P0** |
| 【不符合】**无响应大小上限 / 无本页级超时 / 无权限码** | 仅通用通道 `timeout: 30000`（`utils/request.ts:94`）；`system:dev:api-test%` 在 `sys_permission` 中 **0 行** | P1 |

---

## 7. 数据模型

### 7.1 本页数据表：**无**

**N/A —— 本页不读写任何表。**

| 判定项 | 实测结论 | 依据 |
|--------|---------|------|
| 是否有实体 / `@TableName` / Service / Mapper | **无** | 本页无后端控制器、无 Service、无 Mapper |
| 是否有接口封装文件 / 是否调用 `api/*.ts` | **均无** | 组件直接 `import request from '@/utils/request'`（`api-test/index.vue:107`），全文无第二处 import 业务模块 |
| 是否有 `localStorage` / `sessionStorage` 写入 | **无** | 全文无 storage 调用 |
| 是否写审计 | **无** | `sys_audit_log` 实测 **0 行** |

### 7.2 与本页唯一相关的两处库数据（**均为只读的元数据，页面本身不查询它们**）

| 表 | 实测值 | 与本页关系 |
|----|--------|-----------|
| `sys_menu` | 全表 **381 行**；`id=62404` 一行：`menu_name=API测试`、`menu_code=system:dev:api-test`、`path=admin/dev/api-test`、`component=views/admin/dev/api-test`、`client_type=system-admin`、`menu_type=1`、`display_mode=0`、`status=1`、`visible=1`、`sort=400`、`deleted=0`、`tenant_id=0`、`parent_id=61305`（psql 实测 2026-09-18） | 决定**谁看得见本页**（§5.6） |
| `sys_permission` | 全表 **264 行**；`system:dev%` **5 行**（全为 `system:dev:scheduler:create/delete/execute/list/update`）；**`system:dev:api-test%` 0 行** | 本页**无权限码**（§5.6、§11.3） |

**父链实测**：`62404 → 61305 开发工具（mega:sys:devtools, sort=500, parent_id=60013）→ 60013 系统（mega:system, sort=1600, parent_id=0）`。

### 7.3 加固方案如需落库：**必须新增表**（本系统设计，尚未存在）

| 建议表 | 用途 | 状态 |
|--------|------|------|
| `sys_api_test_audit` | 记录「谁 / 何时 / 对哪个接口 / 用什么方法 / 什么参数 / 结果」 | ★ **建议新增（本系统设计，非业界字段承诺）** |
| `sys_api_test_allowlist` | 存储允许调试的 URL / 域名白名单（替代硬编码） | ★ **建议新增（本系统设计）** |

> **【注意】不发明字段承诺**：以上两表**在当前库中不存在**（本页无任何表）。本页 §8.4 的加固方案**不需要新增表即可落地的最小集**是：**只允许本系统自身基地址 + 剥离凭据 + 响应/超时上限 + 全量审计**（审计若要落库才需新增 `sys_api_test_audit`）。**本页与后端实体无映射**。

---

## 8. 业界对标

> **本页在 ql361 无对标页**（§2 已说明），因此**本章只有「业界对标」一段，没有「ql361 对标规格」**。
> 来源**只能**取自《业界对标-系统模块-20260918.md》§5.3（第 499-526 行）、§8.7（第 876-884 行）、§9.1 第 14 条（第 923 行）；每条按「标题 + URL + 章节 + 查阅日期 + 可信度级别」留痕。**不引用业界界面截图**。
> **本页核心不是 UI，是安全边界** —— 因此本章以 OWASP 官方原文为准，逐条保留底稿引用。

### 8.1 业界叫法与本系统坐标

| 厂商 / 来源 | 叫法 | 与本页的对应 | 级别 |
|------------|------|-------------|:----:|
| **OWASP** | **《Server-Side Request Forgery Prevention Cheat Sheet》** —— API 调试台类功能的**业界共识安全边界** | 本页的安全规格**唯一外部依据** | **A** |
| **SAP** | 有 **API 测试能力**，但通过 **cockpit 权限控制** | 本页 ≈ SAP 的"受权限控制的 API 测试入口"，但**本系统无权限码**（§5.6） | **B**（底稿口径） |
| **Odoo** | **无** API 测试/调试台 | 不适用 | **A**（底稿口径） |
| Postman / Swagger UI 类工具 | 「API 调试台」的产品形态（环境变量、集合、认证面板） | **本系统不照搬其"环境变量注入密钥"模型**（§8.5） | **D**（底稿未取官方文档，属工具形态通识，**不作为规格依据**） |

### 8.2 业界做法要点（OWASP 官方原文，逐条保留底稿引用）

1. **allowlist 优先，denylist 兜底**：官方原文「**Deny-lists are bypass-prone. Prefer allow-lists.**」；allowlist 适用于「应用只与已识别的可信系统通信」的场景；对开放目标（如 webhook）才用 denylist。**A**
2. **「自己构造请求」而不是「透传用户 URL」**：原文「**Match the host against an allowlist, and build the request yourself.**」**A**
3. **解析器不一致要视为拒绝**：若 URL 字符串在下游被重新解析，不同解析器可能得出不同 host —— 原文「**Treat parser disagreement as a rejection.**」**A**
4. **必须屏蔽的最小网段/地址清单（官方表格）**：
   - AWS IMDS：`169.254.169.254`、`metadata.amazonaws.com`
   - GCP Metadata：`metadata.google.internal`、`169.254.169.254`
   - Azure IMDS：`169.254.169.254`
   - Localhost：`127.0.0.0/8`、`0.0.0.0/8`、`::1/128`
   - RFC1918 私网：`10.0.0.0/8`、`172.16.0.0/12`、`192.168.0.0/16`
   - Multicast：`224.0.0.0/4`、`ff00::/8`
   - 相关建议：AWS 环境应迁移到 **IMDSv2** 并禁用 IMDSv1 作为纵深防御。**A**
5. **防 DNS Rebinding**：内部 DNS 应优先解析本组织域名；**同时解析 A 与 AAAA 记录并校验每个结果都是公网可路由的**（不是私网/链路本地），然后**用这个结果发请求**；并对 allowlist 里的域名做监控（官方给出了 `dns.resolver` + `ipaddress` 脚本思路）。**A**
6. **必须关闭重定向跟随**：官方要求「**Disable redirect following in the web client so validation cannot be stepped around**」，并关联到 SSRF Bible 里的 "Unsafe redirect" 绕过。**A**
7. **开放目标场景的额外要求**：需要一个随机 token 证明调用是合法发起的、**仅允许 POST**、做 IP/域名合法性校验、协议限定为 HTTP/HTTPS、参数名与 token 使用固定字符集。**A**
8. **纵深防御**：「harden both the application layer and the network layer (firewall rules, network segregation)」。**A**
9. **★ 本页官方未覆盖的部分（必须显式写明）**：OWASP 该页**没有**关于「响应处理（过滤 / 大小限制）」的指导，也**完全没有**针对「API 测试 / 调试工具」的内容（唯一提到的工具是静态分析 Semgrep）。**A**（缺位结论）

### 8.3 来源（标题 + URL + 章节 + 查阅日期 + 级别）

- **OWASP Cheat Sheet Series《Server-Side Request Forgery Prevention Cheat Sheet》**
  `https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html`
  （章节：**Denylist vs allowlist** / **Minimum ranges to block** / **DNS rebinding** / **Redirect handling**），2026-09-18，**A**
- 底稿自述的覆盖缺口（用于第 9 条）：《业界对标-系统模块-20260918.md》**§9.1 第 14 条**（第 923 行），2026-09-18，**A**（检索结论）
- SAP / Odoo 的"有无 API 测试能力"口径：同底稿 **§5.3**（第 501 行），2026-09-18，**B**

### 8.4 能力对照表

| 能力 | 业界做法（OWASP，A 级） | 本系统当前 | 差距 / 建议 |
|------|----------------------|-----------|------------|
| **目标地址来源** | **allowlist 优先**（原文「Deny-lists are bypass-prone. Prefer allow-lists.」）；**自己构造请求**（原文「Match the host against an allowlist, and build the request yourself.」） | **无 allowlist、无 denylist** —— 用户填什么就发什么（当前因 `url` 被丢弃而未真的发出，见 §5.2） | **P0**：首版改为「**只允许对本系统自身 API 基地址发请求**」，**不提供任意 URL 输入**（§8.4-落地） |
| **内网/元数据屏蔽** | 官方给出最小屏蔽清单（AWS/GCP/Azure IMDS、`127.0.0.0/8`、`0.0.0.0/8`、`::1/128`、RFC1918、Multicast） | **无任何屏蔽** | **P0**：若最终要支持外部 URL，须**逐条实现该清单**（不是"参考"，是**照清单实现**） |
| **DNS Rebinding 防护** | **A/AAAA 双解析 + 每个结果都必须是公网可路由 + 用解析结果发请求** | **无**（无解析、无校验） | **P0**（若支持外部 URL） |
| **重定向跟随** | 官方要求**关闭**重定向跟随，否则校验可被绕过 | **无控制**（浏览器 XHR 默认行为） | **P0**（若支持外部 URL） |
| **凭据处理** | 【注意】**OWASP 该页未覆盖**（§9.1 第 14 条）→ **无业界标准可引** | **无条件带上当前用户 token**（`utils/request.ts:121-123`） | **本系统设计（D 级）**：**剥离平台内部凭据**，只使用当前平台操作员身份；**不得声称有业界官方标准** |
| **响应处理（大小/超时/不落盘）** | 【注意】**OWASP 该页没有"响应处理（过滤/大小限制）"的指导**（§5.3 第 9 条）→ **无业界标准可引** | 无大小上限；仅通用通道 `timeout: 30000` | **本系统设计（D 级）**：加**响应大小上限 + 超时上限 + 响应体不落盘** |
| **审计** | 【注意】OWASP SSRF 篇**未涉及审计**；本模块《README》红线③要求平台侧写操作必须留痕 | **零审计**（`sys_audit_log` 实测 **0 行**） | **P0**：**全量审计**（谁 / 何时 / 对哪个接口 / 什么参数 / 结果）——**需新增表**（§7.3） |
| **开放目标（任意 URL）** | 若确要支持：**随机 token + 仅 POST + IP/域名合法性校验 + 限 HTTP/HTTPS + 固定字符集** | 不适用（无该能力） | **明确不借鉴**（§8.5：成本远高于收益） |
| **纵深防御** | 应用层 + 网络层（防火墙、网络隔离）双加固 | **仅应用层且未加固** | 建议：网络层由部署侧收敛（不在本页范围，登记为运行前提）；**【注意】另注：API 测试/调试工具的专门安全指南在 OWASP 全文不存在**（Semgrep 是唯一被提到的工具）→ **必须自建并标注为「本系统设计」（D 级）** |

**★ 落地（本页是风险最高的功能，必须写清哪些是业界依据、哪些是本系统设计）**

【注意】建议首版降级为「**受限的 API 测试台**」——**以下为「本系统设计」**，其中**「剥离平台凭据 + 响应大小/超时上限」在 OWASP 该页无对应内容，属 D 级自建安全设计，不得称业界标准**：

1. **只允许对本系统自身的 API 基地址发请求**（天然 allowlist = `utils/request.ts:93` 的 `baseURL: '/api'`），**不提供任意 URL**；
2. 代理请求时**剥离 / 不携带平台的内部凭据**，只使用当前平台操作员的身份（本系统已有 sa-token 体系）；
3. 加**响应大小上限**、**超时上限**、**响应体不落盘**；
4. **全量审计**（谁在什么时候对哪个接口发了什么参数）；
5. 若确实要支持外部 URL：**按 OWASP 清单实现**（allowlist + 屏蔽网段 + 关闭重定向 + A/AAAA 双校验 + 仅 POST）；
6. **必须随文档传递的风险提示**：**官方未提供「API 调试台的凭据外泄防护」专门文档**（底稿 §9.1 第 14 条），**本项为自建安全设计（D 级）**。

### 8.5 明确不借鉴

| 不借鉴 | 理由 |
|--------|------|
| **「任意 URL 调试」** | 业界开放目标场景要求「随机 token + 仅 POST + IP/域名双校验 + 固定字符集」，**实现成本远高于本页收益**；本系统场景（调试自己的平台接口）**不需要外部目标** |
| **Postman 类工具的「环境变量注入密钥」模型** | 该模型把密钥放在客户端可替换的变量里，**与本系统"凭据不外泄"的方向相反**；且底稿**未取得其官方文档佐证**（D 级）→ **不作为规格依据** |
| **业界的界面样式**（调试台布局、历史面板、环境选择器） | 本模块口径：只学**能力模型与安全边界**，**不照搬界面**（README §5.3） |
| **浏览器直连目标**（当前实现形态） | 安全校验必须在**服务端**做（浏览器侧校验可绕过）；本页**应向"服务端代理"演进** |
| **无上限地展示响应体** | 大响应会导致页面卡死；且响应中可能含其他租户数据 |

### 8.6 不发明字段承诺

本页 §8.4 / §8.4-落地 提出的所有建议中：

- **无需新增表即可落地的**：①只允许本系统基地址（改前端 + 若走代理则加后端）；②剥离凭据；③响应大小/超时上限；④修正默认填值（`/api/monitor/info` → 存在的只读端点）；
- **必须新增表的（★ 显式标注）**：★`sys_api_test_audit`（审计）、★`sys_api_test_allowlist`（若要把白名单做成可配置）——**两表当前均不存在**；
- **必须新增配置项的（★ 显式标注，§11.4）**：★URL 白名单 / ★方法白名单 / ★网段屏蔽清单 / ★响应大小上限 / ★超时上限 / ★审计开关 ——**当前全部无配置项**；
- **本轮不改任何代码**（§9.0），以上全部为**规格建议**，**不得当作已实现引用**；
- **不得声称有业界官方标准**：凭据剥离与响应处理两项，**OWASP 该页无对应内容**（底稿 §9.1 第 14 条），只能写「**本系统设计（D 级）**」。

---

## 9. 实现差异说明（当前实现 vs 目标规格）

### 9.0 本轮（2026-09-18）处置结果

**本轮无代码改动，以下为现状核实。** 本页所有结论均来自 `views/admin/dev/api-test/index.vue`（170 行）、`utils/request.ts`（`:92-98`、`:104-144`、`:375-400`、`:473-477`）、`axios.cjs`（`:2080-2088`、`:2098-2100`、`:2112-2118`）、`router/dynamicRoutes.ts`（`:7`、`:269`、`:1150`）、`SysMenuServiceImpl.java`（`:250-265`、`:331-339`）的逐行核对，devdb 实测（`sys_menu` / `sys_permission`），以及只读 GET 探测。**未改的绝不写成已修。**

### 9.1 已完成（可保留）

| # | 事实 | 证据 |
|:-:|------|------|
| 1 | **按钮不是桩**：`发送请求` 真实调用 axios（`request.request(config)`） | `:144` |
| 2 | **无定时器、无监听器、无观察者** → 不存在需清理的资源（**无泄漏**） | 全文 grep：`setInterval`/`setTimeout` 0 命中；无 `onMounted`/`onUnmounted` |
| 3 | **响应体不落盘、不下发下载** → 天然满足"响应体不落盘"这一条 | `:97`（仅 `pre` 渲染） |
| 4 | `:loading="sending"` 在请求期间禁用按钮视觉（避免视觉上的重复提交误导） | `:68`、`:120`、`:152` |
| 5 | 失败分支**保留了后端原因**（`e?.response?.data \|\| e.message`） | `:149` |
| 6 | 失败分支**读真实状态码**（`e?.response?.status`）—— 缺陷只在**成功分支** | `:148` vs `:145` |
| 7 | 请求头解析对**无 `:` 的行做静默忽略**（`idx > 0` 守卫，不会产生空键），且对键值**两端 `trim()`** | `:133`、`:134` |

### 9.2 缺陷（按优先级，**均已核实、本轮未修**）

| 级别 | 缺陷 | 位置 | 修法 |
|:----:|------|------|------|
| **P0** | **URL / 方法 / 请求体被 `buildConfig` 静默丢弃 → 实际恒发 `GET /api`**（本页核心功能完全失效） | `utils/request.ts:375-400`（未复制 `url` / `method`）；`api-test/index.vue:144` | 二选一：①在 `buildConfig` 中透传 `url`/`method`（**必须先完成 §8.4 安全加固**）；②本页不再走 `request.request`，改走**专用的服务端代理端点**（推荐） |
| **P0** | **修好透传即产生 SSRF + 凭据外泄 + 请求头注入三重风险**（token 无条件注入 `utils/request.ts:121-123`，且无任何白名单/屏蔽/审计） | `utils/request.ts:121-123`、`api-test/index.vue:45-50`、`:129-138` | **加固先于透传**：allowlist + 剥离凭据 + 关闭重定向 + 屏蔽网段 + 全量审计（§8.4） |
| **P0** | **成功状态码写死 200**，不读真实响应码 | `api-test/index.vue:144-145` | 改读 `res.status` / 响应包装中的真实状态码 |
| **P0** | **零审计**：本页的每次调用都不留痕；`sys_audit_log` 实测 **0 行** | 全文；devdb 实测 | 落 `sys_api_test_audit`（**需新增表**，§7.3） |
| **P1** | **默认填值是死链**：`/api/monitor/info` 后端不存在（运行态实测 404）→ 即使链路修好仍是 404 | `api-test/index.vue:110`；`SystemMonitorController.java:26`+17 端点 | 改为**存在的只读端点**（如 `/api/menu/tree`）或从 OpenAPI 动态拉取 |
| **P1** | **非法 JSON 被显示成「500 + SyntaxError」**，误导为服务端错误 | `:141`（无独立 `try/catch`）→ `:147-149` | 把 `JSON.parse` 包进独立 `try/catch`，提示「请求体不是合法 JSON」 |
| **P1** | **`DELETE` 选中的请求体被丢弃**（仅 `POST`/`PUT` 赋值） | `:140` | 明确各方法的请求体语义（`DELETE` 是否允许 body） |
| **P1** | **无请求取消**，重复点击会并发发出多个请求 | `:66-76`、`:144` | 加 `AbortController`，或请求中禁用按钮（`sending` 只控视觉） |
| **P1** | **无响应大小上限**，大响应整段塞进 `pre`（`max-height: 600px` 只裁剪显示，不裁剪内存） | `:97`、`:157-170` | 截断 + 提示「响应过大已截断」 |
| **P1** | **无 URL/方法白名单、无网段屏蔽、无重定向控制** | 全页 | §8.4 加固清单 |
| **P1** | **未达金标准**：无页面级 `ErrorBoundary`、无页头操作按钮、无请求历史 | `:2`、`:3-20` | 按需补（本页形态**不适用**列表骨架，见 §3.1） |
| **P2** | **「请求URL」为空时静默返回**，无任何提示 | `:119` | 补 `message.warning` |
| **P2** | **无清空响应 / 复制响应 / 下载响应 / 历史记录** | 右栏 `#extra`（`:86-96`） | 按需补 |
| **P2** | **`Authorization` 手写值被拦截器覆盖**（用户以为自定义头生效） | `utils/request.ts:121-123` | 在 UI 上明示「认证头由平台注入」 |
| **P2** | 无 `PATCH` / `HEAD` / `OPTIONS` 选项 | `:31-42` | 按需补 |
| **P2** | 无响应格式化（折叠/高亮/搜索）、无 curl 生成、无环境变量 | `:97` | 按需补 |

### 9.3 2026-09-19 后端安全兜底（本轮新增）

**一句话**：把「路径白名单 / 禁重定向 / 超时 / 响应上限」从**前端**搬到**服务端**并补齐 **allowlist / 屏蔽内网网段 / 凭据剥离 / 调用审计 / 限流** —— 前端校验可以被开发者工具、改 JS 或直接调接口绕过，只有服务端成立的边界才算边界。

**新增/改动文件**

| 文件 | 说明 |
|------|------|
| `backend/core/api/core-api/src/main/java/cn/aiedge/devtool/controller/DevApiTestController.java` | **新增**：`POST /api/dev/api-test/send`（受控出站请求）、`GET /api/dev/api-test/policy`（返回服务端**实际生效**的安全边界，供页面如实展示） |
| `.../devtool/security/ApiTestTargetGuard.java` | **新增**：SSRF 安全闸门（协议 / allowlist / 内网网段 / 钉 IP） |
| `.../devtool/service/ApiTestOutboundService.java` | **新增**：受控出站执行（禁跳转 / 超时 / 流式截断 / 凭据剥离 / 响应头脱敏） |
| `.../devtool/config/ApiTestProperties.java` | **新增**：`app.api-test.*` 配置（前缀 `app` 沿用本项目 `app.data-maintenance` 口径） |
| `.../devtool/dto/ApiTestSendRequest.java` / `ApiTestSendResult.java` | **新增**：入参（`headers`/`body` 标 `WRITE_ONLY`，不写审计表）与结果 |
| `AiReadyApplication.java` | **改动**：`scanBasePackages` 增补 `cn.aiedge.devtool`（漏配 = 端点 404，与本模块此前 5 个包同症） |
| `core-api/src/main/resources/application.yml` | **改动**：新增 `app.api-test.*` 配置块（含生产下线开关 `enabled`） |
| `frontend/apps/pc-admin/src/views/admin/dev/api-test/index.vue` | **改动**：改为调后端端点（不再 `fetch` 直发）；安全说明改为读取 `/policy` 的实际值；被拒时如实展示服务端原因；默认地址由死链 `/api/monitor/info` 改为**真实且免登**的 `/api/auth/check` |
| `db/migration/V11.418.0__Seed_Api_Test_Send_Permission.sql` | **新增迁移**：权限码 `system:dev:api-test:send`（id 6240401，菜单号派生，避开抢号严重的 9xxxx 段） |

**10 条闸门的落点（逐条）**

| # | 闸门 | 做了 | 落在哪 |
|:-:|------|:----:|--------|
| 1 | 协议白名单（仅 http/https，拒 file:/ftp:/gopher:/jar:/data:/dict:/ldap:） | ✅ | `ApiTestTargetGuard.validate`（绝对 URL 分支，scheme 判定） |
| 2 | 目标 allowlist（默认仅同源；外部须服务端配置，**不接受前端传入**） | ✅ | `ApiTestTargetGuard.validate` + `ApiTestProperties.selfBaseUrl / allowExternal / allowedHosts`；同源判定用 `scheme+host+有效端口` **全等**（不拿 `Host`/`X-Forwarded-Host` 推导） |
| 3 | 屏蔽内网网段（127/8、10/8、172.16/12、192.168/16、169.254/16 含云元数据、::1、fc00::/7、fe80::/10、0.0.0.0；另加 100.64/10、192.0.0.0/24、224/4、240/4、IPv4-mapped） | ✅ | `ApiTestTargetGuard.describeBlockedAddress / isNeverAllowed`（**同源豁免**仅豁免「内网网段」一类，`0.0.0.0`/组播/广播任何时候都拒） |
| 4 | 防 DNS Rebinding：先解析 → 校验 → **用该 IP 发起连接** | ✅ | `ApiTestTargetGuard.pinnedDnsResolver` 把校验过的 `InetAddress` 钉进 `PoolingHttpClientConnectionManager(dnsResolver)`；解析目标与已校验主机不一致直接 `UnknownHostException` |
| 5 | 禁跟随重定向 | ✅ | `ApiTestOutboundService.execute`：`disableRedirectHandling()` + `setRedirectsEnabled(false)` + `setMaxRedirects(0)`；3xx 一律只回显不跟随 |
| 6 | 连接超时 + 读取超时（各 5s，可配） | ✅ | `RequestConfig.setConnectTimeout / setSocketTimeout / setConnectionRequestTimeout` |
| 7 | 响应体上限（**流式**截断，非先读满再截） | ✅ | `ApiTestOutboundService.readCapped`（读满 256KB 即停，不使用 `EntityUtils.toByteArray`） |
| 8 | 剥离平台凭据 + 逐跳头丢弃 | ✅ | `validatedHeaders`：**入站请求头从不参与**（控制器签名里没有 `HttpServletRequest`），只取请求体 `headers`；黑名单含 Host/Content-Length/Connection/Transfer-Encoding/Upgrade/TE/Trailer/Expect/Proxy-*/Cookie/Set-Cookie/X-Tenant-Id/tenantId/X-User-Id/X-Forwarded-*/X-Real-IP/Sa-Token；`Authorization` 默认**也拒**（`app.api-test.allow-user-authorization=false`）；头名走 RFC 7230 token 校验、头值禁换行 |
| 9 | 鉴权（权限码 + 落库） | ✅ | `@SaCheckPermission("system:dev:api-test:send")` + Flyway **V11.418.0**（`sys_permission.id=6240401`，并授权超管 role_id=1） |
| 10 | 限流 | ✅（有边界，见下） | `DevApiTestController.RateLimiter`：单实例内存滑动窗口 60s，默认 30 次/分钟/人（`app.api-test.rate-limit-per-minute`） |

**审计**：`@OperationLog(module="开发工具-API测试", type="OTHER")` → `sys_oper_log`，记 谁（userId/username）、何时、入站 URI、耗时、是否异常，以及**目标 URL 与请求方法**（随参数落库）。请求体与请求头在 DTO 上标了 `@JsonProperty(WRITE_ONLY)`，**不写审计表**（审计要「谁打了哪个地址」，不需要把业务数据与使用者的凭据抄一份）—— 已实测：序列化结果只有 `[{"url":"/api/auth/check","method":"GET"}]`。

**「本系统安全设计」的措辞纪律**：协议白名单 / allowlist / 禁跳转 / 屏蔽内网网段四项**有 OWASP 依据**（《Server-Side Request Forgery Prevention Cheat Sheet》）；而「调试台剥离平台凭据 + 响应大小/超时上限」OWASP **没有**对应章节，属**本系统自建安全设计（D 级）**，代码注释与页面文案**均未**把它写成业界标准。

**已验证（只做拒绝路径，不含任何对真实内网/云元数据服务的访问）**：一次性自检脚手架对 `ApiTestTargetGuard` + `ApiTestOutboundService` 跑了 **75 项拒绝断言，75 通过 / 0 失败**，覆盖：`file:`/`ftp:`/`gopher:`/`jar:`/`dict:`/`ldap:`/`data:` 协议、`//host` 协议相对地址、`user:pass@host` userinfo 混淆、`/api/../` 与 `/api/%2e%2e/` 目录跳转、`127.0.0.1`/`10.x`/`172.16-31.x`/`192.168.x`/`169.254.169.254`/`100.64.x`/`0.0.0.0`/`::1`/`fc00::1`/`fe80::1`/`::ffff:127.0.0.1`/`224.0.0.1`/`255.255.255.255`（**把字面量 IP 写进白名单使其通过 allowlist 关，从而单独验证内网闸门**）、白名单未命中/后缀伪装/端口不匹配、逐跳头与平台内部头注入、头名与头值换行注入、`Authorization` 默认拒绝、`TRACE` 方法、GET 带体、请求体超上限。**该脚手架是临时脚本，验证后已删除，未入库。**

**仍未闭环（如实登记）**

| 项 | 现状 | 级别 |
|----|------|:----:|
| 限流是**单实例内存**计数 | 多实例部署时每实例各算一份、重启清零；要严格口径需换 Redis 计数 | P1 |
| 被拒原因**未进 `sys_oper_log`** | `@OperationLog` 的 `status` 只区分「是否抛异常」，业务级拒绝以 `success=false` 返回 → 拒绝**原因**只进了应用日志（`log.warn`），DB 审计里只有「目标 URL + 调用者」 | P1 |
| 未新增**专用**调试台审计表 | 复用通用 `sys_oper_log`（§7.3 曾设想 `sys_api_test_audit`）；未做「响应体是否留痕」的独立策略 | P2 |
| 无「只读端点清单」 | allowlist 只按 origin 放行，未细化到「该 origin 下哪些端点可调试」 | P2 |
| 平台凭据不可用 → 需鉴权接口恒 401 | 这是**凭据剥离的必然结果**（页面已明文说明）。要让使用者自带凭据，需运维显式开启 `app.api-test.allow-user-authorization`；页面端未提供 Authorization 输入（保持原白名单，不给前端新增凭据通道） | 设计取舍 |
| 生产环境未强制下线 | 提供 `app.api-test.enabled=false` 开关，但**默认 true**、需运维显式关闭 | P1 |
| 请求历史 / 保存的集合 / curl 生成 / 响应格式化 | 仍全部未做（§3.1、§12 的 P2 项） | P2 |

---

## 10. 验收标准

> **【重点】本页验收的绝对前置（安全红线，任何脚本都必须遵守）**：
> 1. **编写/执行任何验收脚本时，不得真的点击页面「发送请求」按钮** —— 它会**带当前用户 token** 打真实请求（§5.2、§5.3）。
> 2. **只允许对只读端点执行"发送请求类"断言**，示例：`http://127.0.0.1:5655/api/menu/tree`、`/api/config/list`；**绝不测试任意外网 URL、绝不执行任何写端点**。
> 3. 每次此类断言**必须同时断言"请求头不落到第三方域"**（监听全部请求，断言 host 恒为本机/同源）。
> 4. **不得**为了验证"URL 是否透传"而故意填外网地址去发（它当前不会被发送；一旦透传修好即为**危险动作**）——该结论用**静态断言**（§10.1-③④）与**请求监听**（§10.1-⑤）证明即可。
> **前置（本模块通用）**：本模块**尚无 E2E**（`tools/` 与 `tools/acceptance/` 下无 `e2e-system-*.cjs` / `e2e-admin-*.cjs`）。建议落地 `tools/e2e-system.cjs`（接口 + 直连 DB 对账 + 写库后复原 + Playwright UI 截屏，参照 `tools/e2e-crm.cjs`）。
> **权限账号**：`admin`（`SUPER_ADMIN`，`sys_user.is_super_admin=t`）用作 **200/可见** 基线；另需一个**非超管账号**（建议持 `SYSTEM_ADMIN` 或 `DEPT_ADMIN` 角色，二者均**无** `*` 通配）做**菜单不可见**对账。
> **红线②专项（本页形态）**：本页**不直接读表**，因此**无"空表断言"**；但若用本页调试**平台级表接口**，必须写死「**用超管 + 重新登录后的新 token**」——旧 token 会按会话租户收敛而**查回 0 行且不报错**（`MyBatisPlusConfig.java:150-152`、`:157-166`）。

### 10.1 接口验收（**24 项**）

**静态链路断言（①–⑧，**不发出任何请求**，纯源码核对）**

① **`buildConfig` 复制键清单断言**：读 `utils/request.ts:375-400`，断言其**恰好复制这 6 个键** —— `retryConfig`（`:380-390`）、`headers`（`:393`）、`responseType`（`:394`）、`params`（`:395`）、`data`（`:396`）、`_skipAuthRefresh`（`:397`）；**断言其中不含 `url`、不含 `method`**。
② **`buildConfig` 负向断言**：全文检索 `utils/request.ts:375-400` 函数体，断言**0 处**出现 `options.url` / `options.method` / `config.url =` / `config.method =`。
③ **丢弃断言（本页核心缺陷的可执行证据）**：断言 `api-test/index.vue:144` 传入的 `config`（`:124-127` 含 `method`/`url`）在到达 `service.request(finalConfig)`（`:476`）时**已不含这两个键** → 结论：**实际恒为 `GET /api`**。
④ **axios 语义断言**：读 `axios.cjs:2080-2088`，断言 `isAbsoluteURL(undefined) === false`（`typeof url !== 'string'` 分支）；读 `:2098-2100`，断言 `combineURLs('/api', undefined) === '/api'`；读 `:2112-2118`，断言 `buildFullPath('/api', undefined, ...)` 走 `:2114-2115` 返回 `/api`。**三处行号已本轮实读核实**。
⑤ **成功状态码写死断言**：读 `api-test/index.vue:144-145`，断言 `statusCode.value` 在成功分支被赋**字面量 `200`**，且**不读** `res.status` / `res.data.code`。
⑥ **请求头注入断言（静态）**：读 `utils/request.ts:121-123`，断言 `config.headers.Authorization = \`Bearer ${token}\`` **无条件执行**（仅由 `if (token)` 守卫）；读 `:127-129`，断言**同时**注入 `config.headers.tenantId` 与 `config.headers['X-Tenant-Id']`，值 = `localStorage.getItem('tenantId') || '1'`。
⑦ **无白名单/无审计断言**：全文检索 `api-test/index.vue`（170 行），断言 **0 处** `v-permission` / `hasPermission` / `permission` / `allowlist` / `whitelist` / `audit`；断言无任何后端控制器（全后端 `grep "api-test"` 仅命中测试框架 `.md`/`.yml`/`.py`）。
⑧ **默认填值断言**：断言 `api-test/index.vue:109` 的 `method` 初值为字符串 `'GET'`、`:110` 的 `url` 初值为字符串 `'/api/monitor/info'`。

**端点存在性与基线（⑨–⑫，只读 GET）**

⑨ **默认目标不存在专项**：带 `admin` token 调 `GET /api/monitor/info` → **期望 404** `{"code":404,"message":"接口不存在: api/monitor/info"}`（运行态实测值）。
⑩ **阳性对照**：带 `admin` token 调 `GET /api/config/list` → **期望 200**（证明"404 不是全局拦截器造成的"）。
⑪ **参数缺失对照**：带 `admin` token 调 `GET /api/menu/tree`（不带 `tenantId`）→ **期望 400**「缺少必要参数: tenantId」（证明该端点存在且参数校验生效）。
⑫ **未登录对照**：不带 `Authorization` 调上述任一路径 → **期望 401** `{"code":401,"message":"请先登录"}`（Sa-Token 拦截器先于路由匹配 → **登记：未登录探测无法判定端点是否存在**）。

**运行态请求断言（⑬–⑯，**仅限只读同源端点**）**

⑬ **实际请求恒为 `GET /api` 专项（可执行）**：用 Playwright 打开本页并**监听 `page.on('request')`**，在**不修改源码**的前提下（当前 `url` 被丢弃，用户输入不影响目标），断言点击「发送请求」后发出的唯一新请求为 **`method === 'GET'`** 且 **URL 恒以同源 `/api` 结尾**（不含用户输入的任何路径片段）。**执行前置**：该请求落在同源 `/api`，**不会外发第三方域**（这正是当前代码的"安全副作用"）。
⑭ **请求头注入断言（运行态）**：对⑬监听到的请求断言其 `headers` **含 `Authorization: Bearer <token>`**（token 非空）、**含 `tenantId`**、**含 `X-Tenant-Id`**（对应 `utils/request.ts:121-129`）。
⑮ **不落到第三方域专项（安全断言，必须随⑬⑭一起执行）**：断言本次操作产生的**全部**请求 host 都属于本机/同源（`127.0.0.1:5655` 或前端 dev 源），**0 条**发往任意外部域 —— **这是"当前未发生凭据外泄"的负向证据**。
⑯ **绝对 URL 不能绕出专项**：在「请求URL」填绝对 URL（**只填不发，或用本地无害值**），按⑬的监听方式执行一次，断言**实际请求仍是同源 `/api`**、**未发生域名绕过**；同时在文档/测试报告中**登记设计风险**：**一旦 `url` 透传修好（`buildConfig` 补 `url`/`method`），同一操作会立刻绕出到该绝对 URL**（`axios.cjs:2112-2118` 对绝对 URL 原样返回）→ 因此**加固必须先于透传**。

**库数据对账（⑰–⑲）**

⑰ **菜单行对账**：`SELECT id, menu_code, path, component, client_type, status, visible, sort, deleted, tenant_id, parent_id FROM sys_menu WHERE id = 62404` → 断言逐列等于：`system:dev:api-test` / `admin/dev/api-test` / `views/admin/dev/api-test` / `system-admin` / `1` / `1` / `400` / `0` / `0` / `61305`（**均为本轮实测值**）。
⑱ **权限码缺失对账**：`SELECT count(*) FROM sys_permission WHERE permission_code LIKE 'system:dev:api-test%'` → 断言 **0**；同时 `SELECT count(*) FROM sys_permission WHERE permission_code LIKE 'system:dev%'` → 断言 **5**（全为 `system:dev:scheduler:*`）；`SELECT count(*) FROM sys_permission` → 断言 **264**（本轮实测；**验收请用「≥」而非「==」**，并行会话可能新增）。
⑲ **审计缺失对账**：`SELECT count(*) FROM sys_audit_log` → 断言 **0**（本轮实测）→ 登记「本页调用零留痕」。
⑳ **「改库 → 回读 → 按原值复原」标准动作**：取 `sys_menu.id = 62404`，记录原值 `visible = 1`（实测）→ **改库**置 `visible = 0` → 用超管调 `/menu/user/mega/tenant-admin?userId=…&tenantId=1` **回读**，断言返回的菜单树中**不含 62404** → **按原值复原** `visible = 1` → 再次回读，断言菜单树中**含 62404** → 最后 `SELECT visible FROM sys_menu WHERE id = 62404` 断言回到 **1**。（**全程记录改动前后值；复原失败即验收不通过**。）

**权限断言（㉑–㉒）**

㉑ 用 `admin`（SUPER_ADMIN，系统租户 `tenantId=1`）登录 → 调 `/menu/user/mega/tenant-admin?userId=<adminId>&tenantId=1` → **期望 200 且返回的菜单树含 62404**（走 `SysMenuServiceImpl.java:259-265` 超管分支 → `:331-339` 的 `client_type IN ('tenant-admin','system-admin')`）。
㉒ 用**非超管账号**（无 `*` 通配）登录 → 同一接口 → **期望 200**（端点本身只要登录，**不是 403**）**但返回的菜单树不含 62404**（走 `:266-272` 的 `finalMenuIds = roleMenuIds`，实测 `sys_role_menu` 未把 62404 授给非超管角色）→ **这就是本页唯一的权限门槛**；**同时登记 P1：本页自身发出的请求不带任何权限码**（后端只校验登录态）。

**加固后应新增的断言（㉓–㉔，当前为"未实现"登记）**

㉓ **响应大小上限**：当前无上限 → 断言大响应（如故意调一个返回超大体的只读端点）**会被完整渲染**（登记 P1）；加固后改断言「超过阈值即截断并提示」。
㉔ **超时上限（本页可控）**：当前仅通用通道 `timeout: 30000`（`utils/request.ts:94`），本页**不可配** → 断言源码中本页**无** timeout 参数（`api-test/index.vue` 全文 0 处 `timeout`）；加固后改断言「本页可配超时且默认 ≤ N 秒」。

### 10.2 UI 验收（**17 项**）

① 以 `admin` 登录 → 菜单 `系统 → 开发工具 → API测试`（`62404`）点开直达本页（**单入口**，无标签跳转、无 `list_path`）。
② 面包屑逐字为 `首页 / 系统管理 / API测试`（`api-test/index.vue:6-14`），页面标题逐字 `API测试`（`:15-17`）。
③ **页头无任何按钮**：断言 `#header` 插槽内（`:3-20`）只有 `a-breadcrumb` 与 `h2`，**0 个 `a-button`**。
④ **左栏卡片**：标题逐字 `请求配置`（`:26`），`:bordered="false"`（`:25`），`a-col :span="8"`（`:23`）。
⑤ **右栏卡片**：标题逐字 `响应结果`（`:83`），`:bordered="false"`（`:82`），`a-col :span="16"`（`:81`）。
⑥ **左栏 4 个字段逐字**：`请求方法` / `请求URL` / `请求头` / `请求体`（`:29`、`:45`、`:51`、`:58`）；`a-form` 为 `layout="vertical"`（`:28`）。
⑦ **默认值断言**：「请求方法」默认显示 `GET`（`:109`）；**「请求URL」默认显示 `/api/monitor/info`**（`:110`）；「请求头」「请求体」为**空**（`:111-112`）。
⑧ **placeholder 逐字**：「请求URL」= `/api/...`（`:48`）；「请求头」= `Content-Type: application/json` 换行 `Authorization: Bearer ...`（`:55`）；「请求体」= `{"key": "value"}`（`:62`）。
⑨ **右栏初始态**：响应正文逐字显示 `点击"发送请求"查看响应`（`:114`）；**两个 `a-tag` 都不渲染**（`statusCode=0`/`responseTime=0` 为假值，`:90-95`、`:87-89`）。
⑩ **按钮存在与文案**：左栏表单末行有且仅有 1 个按钮，文案逐字 `发送请求`（`:66-76`），`type="primary"`、`block`、带 `SendOutlined` 图标（`:67`、`:69`、`:72-74`）。
⑪ **【重点】实际请求恒为 `GET /api`（运行态可执行，须遵守 §10 前置）**：打开 Playwright 请求监听 → 不修改源码、不填外网 URL → 点击「发送请求」→ **断言新请求 method = `GET`、URL 恒为同源 `/api`**（不回显用户输入的 `/api/monitor/info`）→ 这是 §9.2 **P0①** 的可执行 UI 证据。
⑫ **【重点】请求头注入断言**：对⑪的请求断言含 `Authorization: Bearer <token>`、`tenantId`、`X-Tenant-Id`（`utils/request.ts:121-129`）。
⑬ **【重点】不落到第三方域断言（安全）**：断言⑪过程中**0 条**请求发往外部域（负向证据，登记为"当前未发生凭据外泄"）。
⑭ **【重点】状态码写死断言**：点击「发送请求」后，无论真实响应如何，右栏状态码 `a-tag` 显示 **`200`** 且为**绿色**（`statusCode < 400`，`:92`、`:145`）→ 这是 §9.2 **P0③** 的可执行 UI 证据。
⑮ **请求头文本域解析断言**：在「请求头」输入 `X-Real-IP: 1.2.3.4` 换行 `BadLineNoColon` → 触发一次（遵守前置，只对只读同源端点）→ 断言请求头**含 `X-Real-IP`**、**不含 `BadLineNoColon`**（无 `:` 的行被 `idx > 0` 守卫静默忽略，`:133`）。
⑯ **非法 JSON 误导行为断言**：「请求方法」选 `POST`、「请求体」填 `{bad json` → 点「发送请求」→ 断言右栏显示**红色 Tag `500`** 且正文含 `SyntaxError` 文案（**不是**服务端错误，登记 §9.2 **P1**）；**前置**：该场景下 `JSON.parse`（`:141`）在发请求**之前**抛错 → **不会真的发出请求**（安全）。
⑰ **页面控制台无 `error`**（含 `TypeError` / `ReferenceError`）+ **结构断言**：无 `a-tabs` / 无 `a-table` / 无 `a-modal` / 无分页 / 无图表；并断言 Network 面板中**除预期的 `/api` 404 外无 4xx/5xx**（若出现 401，即 §5.3「token 未注入」或会话失效的可执行证据）。

---

## 11. 配置落位

> 口径见《对标开发技术参考文档》§5.6 的四条落位通道：**① 环境变量 / ② core-api 的 Spring 配置（`application.yml` + profile yml）/ ③ 配置中心（数据库配置表）/ ④ 业务档案表字段**。
> **本页必须显式区分「已存在的配置项」与「本页缺失、仅为本系统建议的配置项」** —— 后者一律写「**无配置项（未实现）**」并标明**建议落位**，**绝不编造 yml key**。

### 11.1 已存在的配置项（本页实际使用的）

| 配置项 | 落位 | 存储位置 / 格式 | 生效方式 | 缺失行为 |
|--------|------|----------------|---------|---------|
| **请求基地址** `baseURL: '/api'` | **未落位（代码硬编码）** | `utils/request.ts:92-98` 的 `axios.create({ baseURL: '/api' })`（`:93`）；**无对应 yml key**（全文无第二处定义） | **改代码 + 重新构建前端** | 无（硬编码常量，不会缺失）；**这也是本页唯一的"天然 allowlist"** → 加固方案应复用它（§8.4） |
| **请求超时** `timeout: 30000` / **默认请求头** `Content-Type: application/json` | **未落位（代码硬编码）** | `utils/request.ts:94`、`:95-97`（**通用通道**，非本页专属） | 改代码 + 重新构建 | 无（硬编码） |
| **默认填充 URL** `/api/monitor/info` 与**默认请求方法** `GET` | **未落位（前端硬编码）** | `api-test/index.vue:110` 的 `const url = ref('/api/monitor/info')`；`:109` | 改代码 + 重新构建 | 无；**URL 该值本身是死链**（§5.5） |
| **「请求方法」可选值域** | **未落位（前端硬编码）** | `api-test/index.vue:31-42`（4 个字面量） | 改代码 + 重新构建 | 无 |
| **tenantId 请求头来源** | **通道 4（浏览器 localStorage）**，带默认值 `'1'` | `utils/request.ts:127`：`localStorage.getItem('tenantId') \|\| '1'` | 读取即生效（每次请求） | **回落为 `'1'`** → 会话租户被判为系统租户 |
| **超管角色码**（决定 `tenantScopeExempt` 与 `*` 通配，间接决定"谁能看到本页"与"本页能读到什么"） | **通道 2 core-api yml** | `system.super-admin.role-codes`（《对标开发技术参考文档》§5.6.3 登记） | 重启 | 用内置默认值 `SUPER_ADMIN` |

### 11.2 本页缺失的配置项（**全部为「无配置项（未实现）」**）

| 配置项 | 落位 | 现状 | 生效方式 | 缺失行为 |
|--------|------|------|---------|---------|
| **URL 白名单** | **无配置项（未实现）** | 代码中不存在该概念（全页 0 处 `allowlist`/`whitelist`）；无后端代理层 | — | **任何 URL 均无限制**（当前因 `url` 被丢弃而未真的生效；透传修好后**立即生效为"任意 URL"**） |
| **方法白名单** | **无配置项（未实现）** | 前端下拉只是可选值集合，**不是白名单校验**（无校验代码） | — | 任意方法（当前恒为 `GET`） |
| **网段屏蔽清单** | **无配置项（未实现）** | 无 IP 解析、无 `169.254.169.254` / RFC1918 / localhost 拦截（OWASP 清单一条未实现） | — | **内网与云元数据地址可被访问**（透传修好后；当时受浏览器 CORS 约束，但不是安全边界） |
| **响应大小上限** | **无配置项（未实现）** | 无截断逻辑；响应整段渲染（`:97`） | — | **大响应全量渲染** → 页面卡顿/内存压力 |
| **超时上限（本页级） / 重定向跟随开关** | **均无配置项（未实现）** | 本页不可配超时（仅通用通道 `timeout: 30000`，`utils/request.ts:94`）；无 `maxRedirects` / 无 `followRedirect` 控制 | — | 固定 30 秒；重定向跟随默认行为（浏览器侧） |
| **审计开关 / 审计落库** | **无配置项（未实现）** | 本页零审计调用；`sys_audit_log` 实测 **0 行** | — | **调用零留痕**（红线③） |

**★ 建议落位（标明为「本系统设计」，非业界字段承诺；**落地前须新增配置项/新表**）**：

| 建议配置项 | 建议落位通道 | 建议形态 | 备注 |
|-----------|-------------|---------|------|
| **URL 白名单** | **通道 3（配置中心 DB）** | ★ 建议新增表 `sys_api_test_allowlist`（**逐行记录，非 JSON**） | **本系统设计（D 级）**；也可先硬编码为「仅 `baseURL`」 |
| **方法白名单** | **通道 2 yml** 或 **通道 3** | 逗号分隔字符串 / 逐行记录 | **本系统设计（D 级）** |
| **网段屏蔽清单** | **通道 2 yml** | 逗号分隔 CIDR 列表（默认内联 OWASP 清单） | **本系统设计（D 级）**；OWASP 清单是 A 级**依据**，但"写成哪个配置键"是本系统决定 |
| **响应大小上限 / 超时上限** | **通道 2 yml** | 整数（字节 / 毫秒） | **本系统设计（D 级）** —— OWASP 该页**无对应指导**（§8.2-9） |
| **审计落库** | **通道 3（新表）** | ★ 建议新增表 `sys_api_test_audit` | **本系统设计（D 级）** |

### 11.3 权限码落位（通道 3，实测 0 行）

| 配置项 | 落位 | 存储位置 / 格式 | 生效方式 | 缺失行为 |
|--------|------|----------------|---------|---------|
| `system:dev:api-test:*`（**建议新增**） | **通道 3（数据库权限表）** | `sys_permission.permission_code`（**逐行记录，非 JSON**）；**实测 0 行**（全表 264 行，`system:dev%` 仅 5 行且全为 `scheduler`） | 授权后即时（缓存 TTL 300s / 本地 30s） | **本页目前无任何权限码可校验** —— 唯一门槛是菜单 `client_type='system-admin'`（§5.6）；**建议补**：`system:dev:api-test:execute`（调试动作）与 `system:dev:api-test:external`（允许外部 URL，默认不授） |

### 11.4 落位缺口（登记）

| 缺口 | 建议 |
|------|------|
| **本页全部安全配置项均不存在** | 按 §11.2「建议落位」逐项补齐；**其中审计与 allowlist 需新增表**（★ 标注），**不得当作已有配置引用** |
| **`baseURL` 是代码常量** | 建议**保持不变**（它属"天然 allowlist"，是安全边界的一部分，**不应做成运行时可改**）；若要做多环境，走前端构建期环境变量（通道 1） |
| **默认填充 URL 是死链** | 建议改为**存在的只读端点**，或从 OpenAPI spec 动态拉取（与 API文档 62403 的落地方向一致） |
| **超时/大小上限落在通用通道，本页不可控** | 建议**本页专属**的代理端点携带自己的上限，避免影响全站其他请求 |

---

## 12. 剩余缺口（P0 / P1 / P2）

> 口径与《系统模块/README.md》§7 对齐：**P0** = 页面不可用 / 数据不落库 / 越权 / 红线；**P1** = 与业界能力差距大但页面能开；**P2** = 体验与一致性。
> README **§3.4 P0-11**、**§7.1 第 11 条**、§1.3 红线③、§7.2「200 行内未达金标准 / 无审计」在本页的具体化见下。

### P0（4 条）

① **URL / 方法 / 请求体被 `buildConfig` 静默丢弃，实际恒发 `GET /api`（README §7.1 第 11 条的前半段）**：`utils/request.ts:375-400` 只复制 `retryConfig`/`headers`/`responseType`/`params`/`data`/`_skipAuthRefresh` **6 个键**，**未复制 `url` 与 `method`** → 用户填写的三项输入**全部无效**，页面唯一生效的输入是「请求头」；请求体因方法为 GET 而不发送。**后果：本页核心功能完全失效**。建议：**先完成 §8.4 安全加固**，再决定是「在 `buildConfig` 透传 `url`/`method`」还是「改走专用服务端代理端点」（**推荐后者**）。
② **修好透传即产生 SSRF + 凭据外泄 + 请求头注入三重风险（README §7.1 第 11 条的后半段，也是本页最高风险项）**：`Authorization: Bearer <token>` 被**无条件注入**（`utils/request.ts:121-123`），且**无 URL 白名单、无方法白名单、无网段屏蔽、无重定向控制、无审计**（`api-test/index.vue` 全文 0 处权限/白名单代码）。**当前"未出事"只是因为 `url` 被丢弃**。建议：按 OWASP 清单（**A 级**）实现安全边界 —— allowlist + 屏蔽最小网段 + 关闭重定向 + A/AAAA 双校验；「**剥离平台凭据 + 响应大小/超时上限**」为**本系统设计（D 级，OWASP 该页无对应内容，底稿 §9.1 第 14 条）**，**不得称业界标准**。
③ **成功状态码写死 200**：`api-test/index.vue:144-145` → 无论真实响应是什么，成功分支一律展示 `200`（绿色 Tag）→ **用户无法从状态码判断真实 HTTP 状态**。建议：改读真实响应的状态码。
④ **平台侧零审计（README §1.3 红线③、§7.1 P0-17 的具体化）**：本页每次调用**不留任何流水**；`sys_audit_log` 实测 **0 行**。后果：「谁在什么时候拿什么 token 打了哪个接口、传了什么参数」**事后无法还原**。建议：按「时间 + 主体 + 来源 IP + 目标接口 + 方法 + 参数 + 结果」落审计 → **需新增表 `sys_api_test_audit`**（★ 本系统设计）。

### P1（8 条）

⑤ **默认填充值是死链**：`api-test/index.vue:110` 的 `/api/monitor/info` 在后端不存在（**运行态实测 404**；`SystemMonitorController.java:26` + 17 端点无 `/info`）→ **即使链路修好仍是 404**；且与《API文档》62403（`api-doc/index.vue:136`）、接口监控 62203（`views/admin/monitor/api/index.vue:251`）**三页共用同一死链**。
⑥ **非法 JSON 被显示成「500 + SyntaxError」**：`:141` 的 `JSON.parse` 无独立 `try/catch` → 落入 `:147-149` 的业务异常分支，**误导用户以为是服务端错误**。
⑦ **无响应大小上限**：响应体整段渲染（`:97`；`max-height: 600px` 只裁剪**显示**，不裁剪内存）→ 大响应会拖慢/卡死页面。
⑧ **无请求取消机制**：无 `AbortController`；`sending` 只控 `:loading` 视觉（`:68`）→ **重复点击会并发发出多个请求**。
⑨ **`DELETE` 选中的请求体被丢弃**：`:140` 仅对 `POST`/`PUT` 赋 `config.data`。
⑩ **无权限码**：`system:dev:api-test%` 在 `sys_permission`（264 行）中 **0 行**；本页**零 `v-permission`** → 唯一门槛是菜单 `client_type='system-admin'`，**动作层无任何鉴权**。建议补 `system:dev:api-test:execute` 与 `system:dev:api-test:external`（默认不授）。
⑪ **未达金标准**：无页面级 `ErrorBoundary`（全局包裹在 `App.vue:2-10`）、页头 0 个操作按钮、无请求历史、无响应复制/下载 —— 与 README §7.2「30 页全部无 `PageConfigPanel` / 无 `storage-key`」一致（本页形态**不适用**列表骨架，故不作骨架级重写要求）。
⑫ **仅有一个 3 行文本域输入请求头，且 `Authorization` 手写值会被拦截器覆盖**（`utils/request.ts:121-123`）→ 用户易误以为自定义认证头生效。

### P2（4 条）

⑬ **「请求URL」为空时静默 `return`**（`:119`，无任何提示）→ 用户以为按钮失效。
⑭ **无响应格式化能力，方法下拉仅 4 值**：无 JSON 折叠、无语法高亮、无搜索、无 curl 片段生成（`:97` 仅 `pre` 原样文本）；方法仅 `GET`/`POST`/`PUT`/`DELETE`（`:31-42`），无 `PATCH`/`HEAD`/`OPTIONS`。
⑮ **无请求历史 / 无保存的请求集合 / 无环境变量** → 刷新页面即丢（当前无任何持久化，**若要落地需新增表**，§7.3）。
⑯ **无本系统验收截图、无模块级 E2E**（README §7.3 末条）→ 本页**尤其需要**（因其唯一可执行的安全断言必须靠 Playwright 请求监听完成，§10.2-⑪⑫⑬）。
