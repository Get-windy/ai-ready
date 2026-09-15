# 配送模块（DMS）开发文档 · 模块总览

> **对标状态：⚠️ ql361 无此模块**——2026-09-12 实测：ql361 侧边栏一级菜单为 `销售 / 采购 / 仓储 / 配发收 / 财务 / 营销 / 商城 / 分析 / 驾驶舱 / 资料 / 设置`，**没有「配送」一级菜单**。本模块为本系统新增，按**本系统自主建模 + 业界标准（即时配送 TMS/DMS）**设计，不得臆造 ql361 字段。
> **本文为模块级总览**（数据模型 / 边界 / 公共能力 / 实施路线）；**逐页文档见《配送模块 README》索引**。
> 通用规范见《对标开发技术参考文档》第 4、5 章。

## 1. 模块边界

| | 配发收（对标 ql361，5 页） | 配送 DMS（本系统新增，6 页） |
|---|---|---|
| 职责 | 单次配送动作：拣货 → 发货 → 出库 → 配送任务查询 → 退货收货 | 配送**能力体系**：路线执行、调度、跟踪、参数、监控 |
| 数据 | 销售订单 / 出库单 / 退货单 / 采购入库单 | `dms_task`、`dms_rider`、`dms_vehicle`、`dms_tracking`、`dms_config`、`erp_delivery_route` |
| 边界 | 「一次配送」 | 「配送资源与调度执行」 |

> 两者通过**配送任务（`DmsTask`）**衔接：配发收产生配送需求 → 《配送单》承载任务 → DMS 调度资源并跟踪执行。

## 2. 实际页面清单（sys_menu 实测 · 8 列 11 项）

> 权威口径见《配送模块 README》§1.1（含菜单ID / path / 组件 / 接口）。此处只做本文档的导航：

| 列 | 页面 | 文档 |
|----|------|------|
| 线路管理 | 线路列表 | 《线路列表开发文档》 |
| 配送业务 | 配送查询 / 配送单 `[历史]` | 《配送查询开发文档》 / 《配送单开发文档》 |
| 物流配送 | 物流发货 / 发货查询 | 《物流发货开发文档》 / 《发货查询开发文档》 |
| 收货业务 | 物流退货收货 / 采购订货收货 | 《物流退货收货开发文档》 / 《采购订货收货开发文档》 |
| 调度管理 | 调度任务 | 《调度任务开发文档》 |
| 配送跟踪 | 实时跟踪 | 《实时跟踪开发文档》 |
| 配送配置 | 配送参数 | 《配送参数开发文档》 |
| API监控 | API监控 | 《API监控开发文档》 |

**菜单归属补充**：`线路`（`md:route`，70530）挂在 `资料 → 配送管理`，业务属配送线路**档案**，见《线路开发文档》。
`60502 人车管理` 无子菜单项，UI 不渲染（车辆/配送员页面当前**无菜单挂载**，属待补入口）。

## 3. 数据模型总览

| 表 / 实体 | 归属 | 用途 |
|-----------|------|------|
| `dms_task`（`DmsTask`） | task | 配送任务（状态 0–8，见《调度任务开发文档》§5） |
| `dms_rider`（`DmsRider`） | rider | 配送员档案（类型 `RiderTypeEnum`） |
| `dms_vehicle` / `dms_vehicle_maintenance` | vehicle | 车辆档案 / 维护记录 |
| `erp_delivery_route` + `erp_route_point` | route（erp-delivery-route） | 配送路线执行单 + 点位（见《线路列表开发文档》） |
| `erp_route` + `erp_route_area` | md | 线路**档案**（见《线路开发文档》，红线区分） |
| `dms_tracking`（`DmsTracking`） | tracking | 轨迹点（配送员/任务/位置/速度/方向） |
| `dms_sign`（`DmsSign`） | sign | 签收记录 |
| `dms_order_pool` / `dms_bid` | orderpool | 订单池 / 抢单竞价 |
| `dms_channel`（`DmsChannel`） | channel | 配送渠道 + 适配器（达达/美团/顺丰/自有员工/社会运力） |
| `dms_channel_order`（`DmsChannelOrder`） | channel | **渠道外部单台账**（幂等键 `taskNo:channelId` 唯一；承载外部单号/单状态/尝试次数/失败原因，见《渠道管理开发文档》§5.2） |
| `dms_channel_callback_log`（`DmsChannelCallbackLog`） | channel | **渠道回调日志**（验签结果/重放标记/处理结果/原始报文；`(tenant_id, channel_id, nonce)` 唯一防重放） |
| `dms_config`（`DmsConfig`） | config | 配送参数（见《配送参数开发文档》） |
| `dms_route_rider` | dispatch | **区域分包绑定**（线路档案 `erp_route` × 配送员，智能调度 AREA 策略消费） |
| `dms_payment`（`DmsPayment`） | payment | 配送费/代收货款结算 |
| `dms_position_verification` / `dms_rider_vehicle_binding` / `dms_vehicle_inspection` / `dms_verification_alert` | verification | 实名认证/人车绑定/年检/告警 |
| `dms_event_outbox` | event | 领域事件外发 |
| （建议新增）`api_call_log`、`dms_config_history`、`dms_task_log` | — | 接口调用日志、参数变更审计、调度操作日志 |

## 4. 后端模块结构

`backend/dms/src/main/java/cn/aiedge/dms/`：

```
channel(渠道+6适配器)  common(常量/枚举/异常/工具)  config(参数)  dispatch(调度)
event(事件外发)  execution(执行)  orderpool(订单池/竞价)  payment(结算)
rider(配送员)  route(地理编码/距离/路径规划)  sign(签收)  task(配送任务)
tracking(轨迹)  vehicle(车辆)  verification(实名认证)
```

关键枚举：`TaskStatusEnum`(0–8) / `DispatchTypeEnum`(自动/手动/抢单/竞价) / `RiderTypeEnum` / `ChannelTypeEnum` / `PoolStatusEnum`。

## 5. 跨页公共能力（金标准统一下沉）

1. **列表骨架统一**：`CategoryListLayout` + `BillTableList`（序号列齿轮列配置 + `storage-key`）+ `PageConfigPanel`（查询条件/功能按钮 Tab）。禁止页面自造表格与列配置入口。
2. **状态口径统一**：执行态（`TaskStatusEnum` 9 态）只在执行侧展示；**对外台账**（《配送查询》）统一映射为 `待配送 / 配送中 / 已配送` 三值。
3. **单据编号统一**：`DmsTask.taskNo` 须接入 `sys_number_rule` + `next-no`（现状前端时间戳，见《配送单开发文档》§4.1）；建议格式 `PSD-YYYYMMDD-序号`。
4. **选择器统一**：配送员/车辆/线路/客户/商品一律**选择器**（可搜可新增），禁止手输 ID（现状《调度任务》《线路列表》查询为手输 ID）。
5. **地图能力**：复用 `dms/route` 的地理编码/逆编码/距离/路径规划/围栏服务，前端接入地图 SDK（见《实时跟踪开发文档》）。
   - 🔑 **地图 Key 配置落位**：环境变量 `AMAP_API_KEY` → core-api `application.yml` 的 `dms.map.*`/`amap.key` → 《配送参数》配置中心 `map.*`（保存即热生效）；统一口径见《对标开发技术参考文档》**§5.6**，本模块逐页落位见《配送模块 README》§7 与《路线规划开发文档》§8。
6. **事件与配置**：参数变更、任务指派、签收等经 `dms_event_outbox` 外发；消费方监听刷新缓存。
7. **导出/打印**：导出真实 xlsx；打印接 `PrintDialog`（F8 快捷键），不得输出 JSON 或浏览器默认打印。
8. **审计列**：创建人/创建时间/修改人/修改时间按 `BaseEntity` 统一。

## 6. 跨模块联动

| 方向 | 联动点 |
|------|--------|
| 销售 → 配送 | 销售出库单 `XSCKD-` → 配送任务；配送费/代收货款回链财务 |
| 采购 → 配送 | 采购入库单收货（《采购订货收货》） |
| 仓储 ↔ 配送 | 出库环节扣库存；配送不改库存 |
| 财务 ← 配送 | 配送费结算（`dms_payment`）→ 收款核销 |
| 资料 → 配送 | 线路档案（`erp_route`）、物流公司、客户/供应商、商品、车辆/配送员 |
| 司机端 ↔ 配送 | `frontend/apps/driver-delivery` 接单/取货/配送/签收回写任务状态与轨迹 |

## 7. 模块级实施路线（阶段 C）

> 逐页任务书见各页文档「待完善（实施清单）」。模块级优先级如下：

**P0（阻断验收：桩/假数据/口径错）**
1. 《API监控》：删除两张硬编码假数据卡片（1234 次 / 98.5%），改后端统计。
2. 《配送单》：列表 `apiUrl=/dispatch/dispatch-order/page` 与后端 `/api/dms/task` 不匹配 → 打通接口；编号改统一号段。
3. 《线路列表》《调度任务》《配送查询》查询条件中的**手输 ID** 改选择器。
4. 全局：`物流发货/发货查询/物流退货收货/采购订货收货` 的「直达型」入口须复用目标视图组件（《订单处理中心》拣货/发货、《销售退货申请-历史》、《收货处理》），不得各写一套。

**P1（金标准骨架与配置）**
5. 11 页统一换 `CategoryListLayout` + `BillTableList`（序号齿轮）+ `PageConfigPanel`，`storage-key` 逐页命名。
6. 列定义按各页文档「金标准目标设计」重建（默认列 + `defaultHidden` 隐藏列）。
7. 批量操作（批量发货/批量收货/批量指派/批量打印）与经典分页栏。
8. 导出真实 xlsx、打印接 `PrintDialog`。

**P2（能力补齐）**
9. 地图能力：实时跟踪地图 + 轨迹回放 + 地图派单。
10. 参数中心：元数据驱动 + 类型化控件 + 变更审计 + 热生效。
11. 接口监控：`api_call_log` 埋点 + 依赖健康分级 + 告警阈值。
12. 调度策略：负载上限、超时升级、区域分包、抢单/竞价与订单池闭环。✅ **2026-09-13 全部落地**——负载硬门控 + 超时升级（《调度任务》E2E 138/138）、**区域分包**（`dms_route_rider` 按线路档案绑定配送员，严格/偏好两档）、《订单池》抢单竞价闭环（E2E 129/129，中标与任务指派同事务）。
13. 菜单治理：`人车管理`（车辆/配送员）补菜单入口；`线路列表` 更名「配送路线单」消除命名撞车。
