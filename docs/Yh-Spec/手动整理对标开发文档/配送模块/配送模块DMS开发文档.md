# 配送模块（DMS）开发文档

> **对标状态：⚠️ ql361 无此模块**——2026-09-12 实测：ql361 侧边栏一级菜单为 `销售 / 采购 / 仓储 / 配发收 / 财务 / 营销 / 商城 / 分析 / 驾驶舱 / 资料 / 设置`，**没有「配送」一级菜单**，亦无车辆/骑手/调度/跟踪等配送资源页面。本模块为本系统新增，按**本系统自主建模 + 业界标准（即时配送 TMS/DMS）**设计，不得臆造 ql361 字段。
> 本系统菜单：侧边栏 **配送**（mega:delivery），6 列 / 17 项。
> 关联文档：《配送单开发文档》（配送任务单据）、《配送查询开发文档》（台账）、《线路开发文档》（线路主数据）。
> 通用规范见《对标开发技术参考文档》第 4、5 章。

## 1. 模块定位与边界

| | 配发收（对标 ql361） | 配送 DMS（本系统新增） |
|---|---|---|
| 职责 | 单次配送动作：订单拣货 → 发货 → 出库 → 配送任务查询 → 收货 | 配送**能力体系**：线路/车辆/骑手/调度/跟踪/配置 |
| 数据 | 销售订单、出库单、退货单 | 配送任务（DmsTask）、司机、车辆、渠道、跟踪、签收 |
| 边界 | 「一次配送」 | 「配送资源与调度执行」 |

> 二者通过 **配送任务（DmsTask）** 衔接：配发收产生配送需求，《配送单》承载任务，DMS 负责指派资源与执行跟踪。

## 2. 菜单结构与页面清单

实测本系统菜单（mega:delivery，6 列 17 项）：

| 列 | 菜单项 | 本系统路由 | 前端视图 | 后端模块 |
|----|--------|-----------|---------|---------|
| 1 线路管理 | 线路列表 | `dms/route-list` | `views/dms/route-list` | `route`（地理） |
| | 配送路线 | `dms/route` | `views/dms/route` | `erp-delivery-route`（执行单 `erp_delivery_route`） |
| 2 车辆管理 | 车辆列表 | `dms/vehicle` | `views/dms/vehicle` | `vehicle`（`DmsVehicle`） |
| | 车辆管理 [添加](双) | `dms/vehicle` | `views/dms/vehicle` | `vehicle`（+ `DmsVehicleMaintenance` 维护） |
| 3 骑手管理 | 骑手列表 | `dms/rider` | `views/dms/rider` | `rider`（`DmsRider`） |
| | 骑手管理 [添加](双) | `dms/rider` | `views/dms/rider` | `rider` |
| 4 调度管理 | 调度任务 | `dms/dispatch-task` | `views/dms/dispatch-task` | `dispatch`（`DispatchController`） |
| | 配送仪表盘 | `dms/dashboard` | `views/dms/dashboard` | `dashboard` |
| | 智能调度 | `dms/dispatch` | `views/dms/dispatch` | `dispatch`（`DispatchTypeEnum` 自动/手动/抢单/竞价） |
| | 订单池 | `dms/order-pool` | `views/dms/order-pool` | `orderpool`（`DmsOrderPool` + `DmsBid`） |
| 5 配送跟踪 | 实时跟踪 | `dms/realtime-tracking` | `views/dms/realtime-tracking` | `tracking`（`DmsTracking`） |
| | 配送跟踪 | `dms/tracking` | `views/dms/tracking` | `tracking` + `sign`（`DmsSign`） |
| 6 配送配置 | 配送参数 | `dms/config-params` | `views/dms/config-params` | `config`（`DmsConfig`） |
| | 渠道管理 | `dms/channel` | `views/dms/channel` | `channel`（`DmsChannel` + 6 适配器） |
| | 实名认证 | `dms/verification` | `views/dms/verification` | `verification`（4 实体） |
| | 配送配置 | `dms/config` | `views/dms/config` | `config` |

> 双入口项（车辆管理、骑手管理）标 `[添加](双)`：主菜单 → 列表，标签「添加」→ 表单（见《41条双入口菜单的正确配置》）。

## 3. 各页面要素（自研设计）

### 3.1 线路管理

- **线路列表**：配送线路档案（区别于《线路》主数据——本页是 DMS 视角的线路资源）。
- **配送路线**：配送执行单，实体 `erp_delivery_route` + 明细 `erp_route_point`；状态 `PLANNING / READY / IN_PROGRESS / COMPLETED / CANCELLED`；接口 `/api/delivery/route`（`/plan`、`/list`、`/{routeId}`、`/active`、`/{routeId}/start|complete|cancel`、`/navigation`、`/reoptimize`）。
- 与《线路开发文档》关系：线路（`md/route`）为**全局基础数据**，配送路线为**执行单据**，严禁另建重复线路主数据表。

### 3.2 车辆管理

- 实体 `DmsVehicle`（车辆档案：车牌/车型/载重/容积/状态）+ `DmsVehicleMaintenance`（维护记录）。
- 能力：车辆列表、新增/编辑（双入口）、维护记录、（`DmsVehicleInspection` 年检）。

### 3.3 骑手管理

- 实体 `DmsRider`（骑手：姓名/电话/类型 `RiderTypeEnum` /状态）。
- 能力：骑手列表、新增/编辑（双入口）、与车辆绑定（`DmsRiderVehicleBinding`）。

### 3.4 调度管理

- **调度任务**：配送任务指派视图（`DmsTask.dispatchType`：1 自动 / 2 手动 / 3 抢单 / 4 竞价）。
- **配送仪表盘**：运营概览（任务量/完成率/时效/异常）。
- **智能调度**：自动派单策略（`DispatchService`）。
- **订单池**：待调度任务池 `DmsOrderPool` + 抢单/竞价 `DmsBid`。

### 3.5 配送跟踪

- **实时跟踪**：`DmsTracking` 位置上报与轨迹（`RouteController` 提供地理编码/距离/路径规划）。
- **配送跟踪**：任务全链路状态跟踪 + 签收 `DmsSign`（签收人/时间/方式/凭证）。

### 3.6 配送配置

- **配送参数 / 配送配置**：`DmsConfig` 键值配置（派单策略、时效阈值、费用规则）。
- **渠道管理**：`DmsChannel` + 适配器 `DadaAdapter / MeituanAdapter / ShunfengAdapter / OwnStaffAdapter / SocialVehicleAdapter`（第三方运力接入）。
- **实名认证**：`DmsPositionVerification / DmsRiderVehicleBinding / DmsVehicleInspection / DmsVerificationAlert`（骑手/车辆资质与告警）。

### 3.7 支撑模块（无独立菜单）

- **支付**：`DmsPayment`（代收货款/配送费结算）。
- **执行**：`ExecutionController`（任务执行动作）。
- **事件**：`DmsEventOutbox`（领域事件外发）。

## 4. 业务规范

- **单据编号**：配送任务 `DmsTask.taskNo`——现状前端时间戳生成，**须改走统一号段**（见《配送单开发文档》§4.1）。
- **状态机**：`DmsTask.status` 0–8（待分配/已分配/已接单/取货中/配送中/已签收/已完成/已取消/异常），流转由后端校验；对外台账映射为「待配送/配送中/已配送」三值（见《配送查询》§5）。
- **司机端**：`frontend/apps/driver-delivery` 为配送执行端，与 DMS 任务/跟踪/签收联动。
- **与财务**：配送费/代收货款经 `DmsPayment` 结算，回链财务收款。
- **与仓储**：配送任务的货源为出库单（`XSCKD-`），库存扣减在出库环节完成，配送不改库存。

## 5. 实现差异说明（本系统现状）

| 项 | 现状 | 差距 |
|----|------|------|
| 后端 | `backend/dms` 模块完整：task/rider/vehicle/route/channel/config/orderpool/tracking/sign/verification/payment/execution/event | 与 ERP 主数据（客户/商品/仓库/线路）打通程度待复核；号段未接入 |
| 前端 | `views/dms/*` 14 个视图 + `api/dms/*` 11 个 API 模块 | 需按金标准统一骨架（列表 `CategoryListLayout`+`BillDetailTable`+`PageConfigPanel`；表单 `BillFormPage`） |
| 菜单 | mega-menu 已设计 6 列 17 项 | 需核对 `sys_menu` 实际配置（路由/双入口/权限） |
| 线路 | 存在两处「线路」：`md/route`（主数据，桩页）；`erp_delivery_route`（执行单） | 见《线路开发文档》红线：主数据唯一，勿重复建表 |
| 号段 | `taskNo` 前端 `PS+时间戳` | 接入 `sys_number_rule` + `next-no` |
| 页面配置 | DMS 页面未接双配置弹窗 | 按金标准补列/页面配置 |

### 待完善（对齐金标准）

1. **逐页金标准化**：6 列 17 项按「列表金标准（`CategoryListLayout` + `BillDetailTable` 序号列齿轮 + `PageConfigPanel`）/ 表单金标准（`BillFormPage` + `BillDetailTable` 明细 + 配置 3 Tab）」逐页升级，`storage-key` 按页面命名。
2. **菜单核对**：`sys_menu` 中 mega:delivery 6 列 17 项的实际落位、双入口（车辆管理/骑手管理）、权限码。
3. **号段统一**：配送任务编号接入统一号段。
4. **数据打通**：DMS 任务 ↔ 销售出库单/《配送单》↔ 司机端 ↔ 财务收款，形成闭环。
5. **地图能力**：`route` 模块的地理编码/路径规划服务配置（密钥/配额）需在生产环境核对。
6. **对标补位**：ql361 无对标，字段与交互以**业界标准 + 本系统现状**为准，不强行挂靠对标；后续若对标系统上线配送模块，再补实测。
