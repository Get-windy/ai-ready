# API监控开发文档

> **对标状态：⚠️ 无对标**——ql361 无「配送」一级菜单，无此页面。本页按**金标准**（无桩无模拟 / 真实数据）+ **本系统现状** + **业界开放平台/网关监控实践**撰写。
> 本系统菜单：**配送 → API监控 → API监控**（菜单ID `90107`，menu_code `trade:api-monitor`）
> 路由：`trade/api-monitor` ｜ 组件：`views/trade/api-monitor/list.vue`
> **验收：`node tools/e2e-api-monitor.cjs` —— 119/119 通过**（2026-09-14，接口与 DB 逐项对账 + UI + 沙箱 + 重试 + 告警）
> 关联文档：《配送模块（DMS）开发文档》；外部渠道能力见《渠道配置》《外部订单》《数据同步》（交易模块）。
> 通用规范见《对标开发技术参考文档》第 4、5 章。

## 1. 页面概述

- **接口监控与联调自检页**：展示对**外部渠道接口**（开放接口 / 渠道调用）的健康状态、调用量与失败情况，
  提供**依赖健康逐项探测**、**快速联调沙箱**、**异常告警**与**库存同步记录**闭环（重试 / 分类 / 导出）。
- 业务定位：配送/多平台库存协同的运维入口——「第三方渠道接口是否通、同步是否成功、失败能不能一键重试」。

## 2. 本系统现状（2026-09-14 实测 · 全部真实数据）

### 2.1 统计卡片（8 张，全部后端聚合 `GET /api/trade/api-monitor/stat`）

| 卡片 | 指标 | 数据来源 | 状态 |
|------|------|---------|------|
| API状态 | 服务状态（UP/DOWN） | `/api/open/health` | ✅ 真实 |
| 今日接口调用量 | `count(api_access_log, direction IN/OUT, 今日)` | `api_access_log` | ✅ 真实 |
| 接口成功率 | 成功数 / 总数 ×100%（保留 2 位） | 同上，`status='SUCCESS'` | ✅ 真实 |
| 平均耗时 | `AVG(response_time)` | 同上 | ✅ 真实 |
| **P95 耗时** | 分位耗时（升序第 95% 位） | 同上 | ✅ 真实 |
| 今日失败次数 | 总量 − 成功数 | 同上 | ✅ 真实 |
| 待处理订单 | 待处理数量（笔）+「查看」跳 `/trade/external-order` | `/api/open/order/pending-count` | ✅ 真实 |
| 库存同步失败数 | `sync_status = 2` 计数 | `inventory_sync_record` | ✅ 真实 |

> **调用日志口径**：埋点底表为 `api_access_log`，三类方向——
> **IN** 外部调用我方开放接口（`/api/open/**` 拦截器）、**OUT** 我方调用外部渠道（库存推送/改价）、
> **SANDBOX** 页面「快速联调」自检。**指标只计 IN + OUT**（联调自检不计入调用量，避免自证式虚高）。
> **无数据约定**：`successRate` / `avgCostMs` / `p95CostMs` / `maxCostMs` / `syncFailedCount` 在无数据时返回 `null`
> （前端显示 0 或「-」），**严禁写死常量**（原「今日查询 1234 次」「同步成功率 98.5%」已彻底删除，并有 E2E 断言把关）。
> **实现落点**：后端 `cn.aiedge.trade.monitor.*`（core-base）；前端 `views/trade/api-monitor/list.vue`。

### 2.2 依赖健康面板（`GET /api/trade/api-monitor/deps`，逐依赖分级）

| 依赖 | 探测方式 | 缺失/异常行为 |
|------|---------|--------------|
| 数据库 | `SELECT 1`（真实往返 + 耗时） | DOWN + 错误摘要 |
| Redis 缓存 | `PING/PONG` | DOWN + 错误摘要 |
| 消息中间件 | **TCP 连通性探测** `spring.rabbitmq.host:port`（1.5s 超时，不做 AMQP 握手，如实标注） | 未配置 → NOT_CONFIGURED；不可达 → DOWN |
| 地图服务 | 配置中心 `map.*` + 应用配置 `dms.map.*` 的 Key 来源（环境变量 / 配置中心 / 无） | 无 Key → NOT_CONFIGURED（提示降级为直线模式） |
| 第三方渠道 | `external_channel_config` **启用渠道逐项** + 近 24 小时成功/失败 + 最近错误 | 近 24h 全失败 → DOWN + 最近错误；无启用渠道 → 不返回该项 |

> 面板顶部汇总「共 N 项 · 异常 x · 未配置 y」；每项可 hover 查看说明与最近错误。

### 2.3 快速联调沙箱（真实回环调用，非模拟）

- 接口目录 `GET /api/trade/api-monitor/calls/endpoints`：**5 组 12 个**开放接口（订单接入 4 / 库存查询 4 / 库存同步 2 / 商品同步 1 / 服务健康 1），
  含中文名、方法、路径模板、**参数定义**（位置 path/query/body、类型、必填、示例值）。
- 发送 `POST /api/trade/api-monitor/sandbox/invoke`：服务端**回环 HTTP 调用本实例真实接口**
  （`http://127.0.0.1:{port}`，透传联调人的登录态、显式 `Accept: application/json`），
  返回 HTTP 状态 / 业务码 / 耗时 / 响应体（脱敏截断）/ **请求号**。
- **联调历史 = 调用日志中 `direction = SANDBOX` 的分页**（同一张表，不另建历史表）；可按请求号在「接口调用日志」Tab 追溯。

### 2.4 三 Tab 明细台账（金标准骨架）

| Tab | 列（默认显示 + 隐藏） | 说明 |
|-----|--------------------|------|
| 接口调用日志 | 14 列（默认 8 + 隐藏 6：响应码/请求号/错误码/调用方IP…） | `calls/page`，支持 渠道/接口/方向/状态/关键字/时间 过滤 |
| 库存同步记录 | 12 列（默认 10 + 隐藏 2） | `inventory-sync/page`，支持 渠道/SKU/类型/状态/失败分类/时间 过滤；行内**重试** |
| 异常告警 | 9 列 | 阈值判定结果（类型/级别/明细/当前值/阈值/事件外发状态/处置建议） |

- 骨架：`CategoryListLayout`（三 Tab）+ `BillTableList`（表头齿轮列配置，`storage-key` 逐 Tab 独立
  `trade-api-monitor-calls` / `-sync` / `-alerts`）+ `PageConfigPanel`（12 个查询条件 + 4 个功能按钮）。
- 工具栏：`页面配置` ｜ `刷新(F5)` ｜ `导出`（calls / sync 为**真实 xlsx**；alerts 为实时派生数据，按钮置灰）｜ `自动刷新开关`（间隔读配置中心，默认 30s，**默认关闭**）。

### 2.5 库存同步增强（重试 / 分类 / 导出）

- **失败原因分类**（`error_category`）：`NETWORK / AUTH / PARAM / RATE_LIMIT / BIZ_REJECT / UNKNOWN`，由错误信息关键字推导（`ErrorCategory`），
  在写入同步记录与重试回写时统一落库；`GET /api/trade/api-monitor/sync/stat` 返回状态分布 + 分类计数（带中文标签）。
- **重试** `POST /api/trade/api-monitor/sync/{id}/retry`：**复用原记录**（不新增一条同步记录，避免重复记账），
  真实调用渠道适配器并回写 `retry_count / last_retry_time / sync_status / error_msg / error_category`；
  已成功的记录拒绝重复重试（幂等保护）。
- **导出** `GET /api/trade/inventory-sync/export`（真实 xlsx，10 列）。

### 2.6 告警（阈值判定 + 静默期 + 事件外发）

| 告警类型 | 触发条件（阈值来自配置中心） | 级别 |
|---------|---------------------------|------|
| `ERROR_RATE` | 今日错误率 > `monitor.threshold.error-rate`（默认 5%） | 超 2 倍 → CRITICAL |
| `P95_LATENCY` | 今日 P95 > `monitor.threshold.p95-ms`（默认 2000ms） | WARN |
| `FAIL_COUNT` | 今日失败数 > `monitor.threshold.fail-count`（默认 20） | WARN |
| `SYNC_FAILED` | 库存同步失败数 > `monitor.threshold.sync-fail-count`（默认 0 = 有失败即告警） | WARN |
| `DEPENDENCY` | 任一依赖项 DOWN | CRITICAL |

- **静默期**：同类型告警在 `monitor.threshold.silence-minutes`（默认 30 分钟）内只**外发一次事件**，页面仍持续展示（标注「静默期内 / 已外发事件」）。
- **事件通道**：core-base 发布 `ApiMonitorAlertEvent`，由 DMS 侧 `ApiMonitorAlertBridgeListener` 写入 `dms_event_outbox`
  （`event_type = API_MONITOR_ALERT`）——保持模块单向依赖（core-base 不反向依赖 DMS），与《签收管理》同一条外发通道。
- **留存**：`monitor.log.retention-days`（默认 30 天）；每日 03:50 按租户逐个清理 + 手动入口 `POST /clean-expired`（0 = 不清理）。

## 3. 后端接口（金标准 · 已全部落地）

| 接口 | 说明 |
|------|------|
| `GET /api/trade/api-monitor/stat` | 统计卡片聚合（8 项 + 口径说明 + 统计区间） |
| `GET /api/trade/api-monitor/deps` | 依赖健康逐项（DB / Redis / MQ / 地图 / 渠道） |
| `GET /api/trade/api-monitor/calls/page` | 接口调用日志分页（渠道/接口/方向/状态/关键字/时间） |
| `GET /api/trade/api-monitor/calls/stat` | 分维度统计（`groupBy = channel / api / direction`，含成功率） |
| `GET /api/trade/api-monitor/calls/trend` | 调用量按小时趋势（total / fail） |
| `GET /api/trade/api-monitor/calls/endpoints` | 开放接口目录（联调分组树 + 参数定义） |
| `GET /api/trade/api-monitor/calls/export` | 调用日志导出 xlsx（最多 5000 条） |
| `POST /api/trade/api-monitor/sandbox/invoke` | 快速联调（真实回环调用 + 落 SANDBOX 日志） |
| `GET /api/trade/api-monitor/alerts` | 异常告警（阈值判定 + 静默期 + 事件外发） |
| `GET /api/trade/api-monitor/thresholds` | 当前生效阈值 + 逐键来源（TENANT/GLOBAL/DEFAULT） |
| `GET /api/trade/api-monitor/sync/stat` | 同步记录统计（状态分布 + 失败原因分类） |
| `POST /api/trade/api-monitor/sync/{id}/retry` | 同步失败重试（复用原记录回写） |
| `POST /api/trade/api-monitor/clean-expired` | 按保留策略清理调用日志 |
| `GET /api/trade/inventory-sync/page` | 同步记录分页（渠道/SKU/类型/状态/失败分类/时间） |
| `GET /api/trade/inventory-sync/export` | 同步记录导出 xlsx |

## 4. 数据模型与迁移

| 迁移 | 内容 |
|------|------|
| `V11.360.0` | ① **复用并扩展 `api_access_log`**（V8.14.0 建成的「限流监控」表，此前 0 行 0 引用）补齐 `api_name / request_id / direction / status / error_code / error_msg` + 3 个索引——**不新建 `api_call_log`**（避免同义表双轨）；② `inventory_sync_record` 补 `retry_count / last_retry_time / error_category`，并放开 `product_id NOT NULL`（同步记录按 SKU 记账）；③ 配置中心预置 `monitor.*` 7 键（全局 + 系统租户双行） |
| `V11.360.1` | 幂等补种 `monitor.threshold.sync-fail-count`（原因见 §6.3：共享库已被并行实例抢先应用 V11.360.0 的早期版本，Flyway `repair-on-migrate` 后同号迁移不会重跑） |
| `V11.360.2` | 补 `external_order_raw.deleted` 列 —— 实体有 `@TableLogic` 而表缺列，导致**开放接口订单回调 500**（由本页联调沙箱回环调用暴露的真实缺陷），口径与 `inventory_sync_record`（V11.213.0）一致：补列对齐实体 |

- **埋点方式**：入站统一下沉在 `ApiCallLogInterceptor`（挂 `/api/open/**`，排在认证拦截器之后 → 只记通过认证的调用；
  `api_path` 记 URI 模板、`request_id` 回写响应头 `X-Request-Id`、查询串**脱敏**、**不落请求/响应体**）；
  出站在 `InventorySyncServiceImpl` 的渠道调用点写日志；无租户上下文时跳过落库（避免 `tenant_id` NOT NULL 与脏数据）。

## 5. 实现差异说明（现状 → 金标准：全部达成）

| 项 | 目标 | 现状 |
|----|------|------|
| 统计卡片 | 全部后端聚合，无写死 | ✅ 8 张全真实（含 P95），E2E 断言卡片值与接口严格一致 |
| 健康检查 | 逐依赖健康面板 | ✅ `/deps`：DB/Redis/MQ/地图/渠道逐项 + 最近失败 + 建议 |
| 调用日志 | 日志表 + 分页/统计 + 埋点 | ✅ 复用扩展 `api_access_log` + 拦截器/出站埋点 + page/stat/trend/export |
| 联调工具 | 接口分组 + 参数表单 + 结果 + 历史 | ✅ 5 组 12 接口 + 参数表单 + 真实回环 + JSON 结果 + 耗时/请求号 + 历史（SANDBOX 日志） |
| 同步记录 | 补重试/导出/错误分类 | ✅ 重试（复用原记录 + 幂等）+ 6 类失败分类 + 真实 xlsx + 富查询 |
| 告警 | 错误率/延迟阈值 + 静默期 | ✅ 5 类告警（配置化 5 个阈值）+ Redis 静默期 + `dms_event_outbox` 事件外发 |
| 页面骨架 | `CategoryListLayout` + 表头齿轮 + 页面配置 | ✅ 三 Tab + 逐 Tab `storage-key` + 12 查询项/4 按钮配置弹窗 + F5 + 自动刷新 + 经典分页 |

**实现文件**
- 后端：`cn.aiedge.trade.monitor`（controller / service / mapper / entity / dto / event / `ApiCallLogInterceptor` / `ApiCallLogRecorder` / `OpenApiCatalog` / `ErrorCategory` / `XlsxExporter` / `TimeParsers` / `ApiMonitorLogRetentionJob`）
- 前端：`views/trade/api-monitor/list.vue`；API 封装 `api/trade/index.ts`（`apiMonitorApi` / `inventorySyncApi`）
- 迁移：`V11.360.0` / `V11.360.1` / `V11.360.2`；E2E：`tools/e2e-api-monitor.cjs`（119 项）

## 6. 开发要点与踩坑（复用价值）

1. **埋点复用既有空表**：`api_access_log` 早已存在（V8.14.0，0 行 0 引用），直接扩展它比新建 `api_call_log` 更符合「同类型能力只实现一次」——否则两张同义表必然分叉。
2. **`a-statistic` 的字符串值陷阱**：把「-」直接传给 `a-statistic` 的 `value`，其内部正则 `^(-?)(\d*)(\.(\d+))?$` 会把「-」解析成「负号 + 空整数 → 0」，**渲染成 `-0`**；字符串展示一律走 `:formatter`（原样输出），E2E 已加断言把关。
3. **Flyway 同号迁移改了不重跑**：dev 档 `spring.flyway.repair-on-migrate=true` 会按磁盘改写 checksum，已被应用的版本**不会重跑**；共享库里 V11.360.0 早被并行实例应用 → 后加的配置键只能用 **新号迁移**（V11.360.1）补种。**改已应用的迁移等于没改。**
4. **实体 `@TableLogic` 必须与表列一致**：`external_order_raw` 缺 `deleted` 列 → 该表所有查询 500（与 `inventory_sync_record` V11.213.0 是同一类问题）。新增实体务必核对列；**手写 SQL 必须自带 `deleted = 0`**。
5. **联调必须显式 `Accept: application/json`**：本平台类路径上有 `jackson-dataformat-xml`，`Accept: */*` 时内容协商会返回 **XML**，联调结果就不是对接方看到的报文。
6. **依赖「是否接入」要看真实探测**：`spring.rabbitmq.host` 有配置 ≠ Broker 可用（本机 MQ 未启动 → 面板如实显示「异常」，并触发 DEPENDENCY 告警）；不做假 UP。
7. **UI E2E 的稳健性**：页面较长时 sticky 表头的齿轮可能被容器拦截 → 脚本先常规点击、失败回退 JS 触发；等待改为 `waitForSelector` 而非死等秒数（本机多实例并行时响应慢）。

---

## 配置落位（2026-09-13 增补 · 2026-09-14 落地）

| 配置项 | 落位 | 生效 | 缺失行为 |
|--------|------|------|---------|
| 接口监控阈值（错误率 / P95 / 失败数 / 同步失败数 / 静默期 / 自动刷新间隔 / 日志保留天数） | ✅ 已落**配置中心** `dms_config`（`monitor.*`，配送参数页「API监控」分组可改；全局默认 + 租户覆盖） | 保存即热生效（每次取用读配置中心） | 代码默认值：5% / 2000ms / 20 次 / 0 条 / 30 分钟 / 30s / 30 天 |
| 被监控接口清单 | 服务注册/埋点表，非配置文件（`OpenApiCatalog` 人工镜像 + 拦截器按 URI 模板记录） | — | 未登记接口仍埋点，名称回退为方法名 |
| 外部服务依赖健康检查（含地图服务） | 地图读配置中心 `map.*` + `dms.map.*`；MQ 读 `spring.rabbitmq.*` 做 TCP 探测；渠道读 `external_channel_config` | — | 无 Key → 显示降级；无启用渠道 → 不返回渠道项 |
| 告警事件去向 | `dms_event_outbox`（`event_type = API_MONITOR_ALERT`，由 DMS 侧桥接监听器写入） | 触发即写 | 写入失败仅告警日志，不影响页面展示 |

> 统一口径见《对标开发技术参考文档》§5.6。
