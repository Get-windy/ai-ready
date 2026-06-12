# WMS 仓储管理模块 — 架构设计文档

> 基于 2026-06-11 架构评审会议结论，第一性原理驱动设计，无历史包袱。

---

## 一、设计前提

### 1.1 租户分级：WMS 作为增值服务

WMS 是 ERP 系统的**增值服务模块**，非必选。租户不购买 WMS 时仍可完成完整的库存业务流程，只是操作模式不同：

| 操作 | 基础版（无 WMS） | WMS 增值版 |
|------|-----------------|-----------|
| 入库 | ERP 采购入库单 → 直接增加 `erp_stock` | 采购入库单 → WMS 收货任务 → PDA 收货 → 上架 → 确认入库 |
| 出库 | ERP 销售出库单 → 直接扣减 `erp_stock` | 销售出库单 → WMS 拣货任务 → PDA 拣货 → 复核 → 确认出库 |
| 盘点 | ERP 盘点单 → 手工录入实盘数 → 调整库存 | WMS 盘点任务 → PDA 逐库位扫描 → 差异报告 → 调整 |
| 货位 | 不管理货位 | 完整货位层级 + 上架策略推荐 |
| PDA | 无 | 全流程 PDA 作业 |
| 波次策略 | 无 | 按优先级/区域聚合生成波次 |

**技术实现方式**：通过租户特性开关（Feature Flag）控制，不涉及两套代码分支。

```java
// 租户服务判断是否启用 WMS
boolean wmsEnabled = tenantService.hasFeature(tenantId, "wms");

// 基础版走 erp-stock 直接操作
if (!wmsEnabled) {
    stockService.decreaseStock(productId, warehouseId, quantity);
    return;
}

// 增值版走 WMS 作业流程
pickService.createPickTask(saleOrder);
```

**设计原则**：
- 数据层面不隔离：所有租户共用 `erp_stock` 表，WMS 增值版写入更细粒度的 `wms_inventory_log`
- 流程层面分层：基础版走 ERP 直接操作，增值版走 WMS 作业流程
- 界面层面控制：PC 菜单根据租户特性显示/隐藏 WMS 页面，PDA 端仅 WMS 租户可访问

### 1.2 现有 erp-stock 模块的拆解结论

`erp-stock` 模块存在严重的职责混乱问题，包含大量不属于库存的实体。由于租户分级需求，**erp-stock 不做完全拆解，而是分层保留**：

| 分类 | 内容 | 处理方式 |
|------|------|---------|
| **基础库存核心** | Stock、StockService.increase/decrease/freeze/unfreeze | **保留在 erp-stock**，供基础版租户使用 |
| **WMS 增强逻辑** | 收货任务、拣货波次、上架策略、货位管理、PDA 接口 | 全部在 WMS 模块中新建 |
| **仓库扩展** | Warehouse、仓库 CRUD | 迁入 WMS，erp-stock 保留只读查询接口 |
| **库存写入路径** | 基础版直接写 `erp_stock`，增值版通过 WMS 写 | 两条路径最终都更新 `erp_stock` 表 |
| **杂质实体** | Product、Partner、Marketing、Balance 等 | 原地标注 `@deprecated`，不迁移 |

### 1.3 写入路径（关键）

```
基础版租户：
  ERP 模块 ──► erp-stock.StockService ──► erp_stock 表

增值版租户：
  ERP 模块 ──► WMS 作业流程 ──► wms_inventory_log ──► erp_stock 表
                                    + 异步事件回调 ERP
```

- **erp-stock 的 crud 写入完全保留给基础版租户使用**
- **WMS 增值版通过 wms_inventory_log 做异动流水，同时更新 erp_stock**
- **两种方式最终写入同一张 erp_stock 表**，不存在数据分裂

### 1.4 依赖方改造

针对"已购买 WMS"的租户，改造引用方：

| 模块 | 基础版行为 | WMS 增值版行为 |
|------|-----------|---------------|
| `erp/sales` | 直接 `stockService.decreaseStock()` | 通过事件通知 WMS 创建拣货任务，WMS 完成后回调扣减 |
| `core/agent` | 依赖 `erp-stock` | 改为依赖 `wms-inventory` |
| `core/api` | 通过 `erp-stock` | WMS 独立包扫描 |

> 改造工作仅在 WMS 上线后、针对已购买 WMS 的租户生效，不影响基础版租户的现有流程。

### 1.5 数据主权划分

| 数据 | 所属域 | ERP 视角 | WMS 视角 |
|------|--------|---------|---------|
| 商品主数据 | ERP | 增删改 | 只读引用（product_id） |
| 客户/供应商 | ERP | 增删改 | 只读引用 |
| 销售订单 | ERP | 增删改 | 读取生成拣货任务 |
| 采购订单 | ERP | 增删改 | 读取生成收货任务 |
| 仓库 | WMS | 只读查询 | 增删改 |
| 货位 | WMS | 只读查询 | 增删改 |
| 库存实物（`erp_stock`） | 共享 | 增删改（基础版直接操作，增值版由 WMS 代理写入） | 写入（增值版） |

---

## 二、模块架构

### 2.1 模块树

```
backend/wms/
├── pom.xml
├── WMS-ARCHITECTURE.md
├── AGENTS.md
│
├── wms-common/              # 通用定义
│   ├── enums/               # 任务状态、作业类型枚举
│   ├── constant/            # WMS 常量
│   └── exception/           # 领域异常
│
├── wms-warehouse/           # 仓库/货位管理
│   ├── entity/              # WmsWarehouse, WmsLocation
│   ├── service/             # LocationService（上架策略推荐）
│   └── controller/          # /api/wms/warehouse, /api/wms/location
│
├── wms-receipt/             # 收货任务
│   ├── entity/              # ReceiptTask, ReceiptDetail
│   ├── service/             # ReceiptService（关联采购订单）
│   └── controller/          # /api/wms/receipt
│
├── wms-putaway/             # 上架任务
│   ├── entity/              # PutawayTask, PutawayDetail
│   ├── service/             # PutawayService（上架策略引擎）
│   └── controller/          # /api/wms/putaway
│
├── wms-pick/                # 拣货任务/波次
│   ├── entity/              # PickTask, PickDetail, PickWave
│   ├── service/             # PickService（波次策略引擎）
│   └── controller/          # /api/wms/pick
│
├── wms-ship/                # 发货复核
│   ├── entity/              # ShipTask, ShipDetail
│   ├── service/             # ShipService
│   └── controller/          # /api/wms/ship
│
├── wms-inventory/           # 库存/异动
│   ├── entity/              # WmsInventory, WmsInventoryLog
│   ├── service/             # InventoryService（冻结/解冻/扣减）
│   └── controller/          # /api/wms/inventory
│
├── wms-move/                # 库内移库
│   ├── entity/              # MoveTask, MoveDetail
│   ├── service/             # MoveService
│   └── controller/          # /api/wms/move
│
├── wms-check/               # 盘点
│   ├── entity/              # CheckTask, CheckResult
│   ├── service/             # CheckService（明盘/盲盘）
│   └── controller/          # /api/wms/check
│
└── wms-event/               # 事件集成
    ├── entity/              # EventOutbox（事件发件箱）
    ├── service/             # EventService（重试/回调）
    └── controller/          # /api/wms/erp（ERP 回调接口）
```

### 2.2 技术选型

| 项 | 选择 | 理由 |
|----|------|------|
| 框架 | Spring Boot 3.2.5 + MyBatis-Plus | 与主项目一致 |
| 模块间通信 | Spring `ApplicationEvent` | 轻量，无额外中间件依赖 |
| 跨系统集成 | 数据库事件表 + 定时任务 | 无 MQ 依赖，保证最终一致性 |
| 乐观锁 | `@Version` 注解 | 继承 BaseEntity 机制 |
| API 文档 | Knife4j (OpenAPI 3) | 与主项目一致 |
| 统一响应 | `Result<T>` sealed interface | 复用 core-base |

---

## 三、数据库模型

### 3.1 新表清单

所有表使用 `wms_` 前缀，PostgreSQL，遵循现有命名规范：

| 表名 | 用途 | 核心字段 |
|------|------|---------|
| `wms_warehouse` | 仓库扩展 | 在 `erp_warehouse` 基础上扩展库区数量、类型、容量 |
| `wms_location` | 库区/货位 | 层级编码(A-01-01)、容量、状态(空闲/占用/冻结)、载重 |
| `wms_receipt_task` | 收货任务 | 关联采购单号、供应商、仓库、状态、预期到货时间 |
| `wms_receipt_detail` | 收货明细 | 商品ID、应收数、实收数、上架货位、批次 |
| `wms_putaway_task` | 上架任务 | 关联收货单、状态、操作人 |
| `wms_putaway_detail` | 上架明细 | 商品、源货位(暂存)、目标货位、数量 |
| `wms_pick_wave` | 拣货波次 | 波次号、订单数、商品数、状态、优先级 |
| `wms_pick_task` | 拣货任务 | 关联波次、出库单、仓库、状态 |
| `wms_pick_detail` | 拣货明细 | 货位、商品、应拣数、实拣数、缺货标记 |
| `wms_ship_task` | 发货复核 | 关联拣货任务/出库单、状态 |
| `wms_ship_detail` | 复核明细 | 商品、扫描数、确认数 |
| `wms_inventory` | 实时库存 | 商品+仓库+货位+批次维度，数量、可用量、冻结量 |
| `wms_inventory_log` | 库存异动 | 来源单据类型/ID、变动量、变动前/后、方向(入库/出库)、操作人 |
| `wms_move_task` | 移库任务 | 源货位、目标货位、状态 |
| `wms_move_detail` | 移库明细 | 商品、数量 |
| `wms_check_task` | 盘点任务 | 盘点类型(明盘/盲盘)、仓库、状态、锁定库位 |
| `wms_check_result` | 盘点结果 | 商品、账面数、实盘数、差异、原因 |
| `wms_event_outbox` | 事件发件箱 | trace_id、事件类型、请求体、状态(待发送/成功/失败)、重试次数 |

### 3.2 与 erp_stock 表的关系

```
erp_stock (ERP 库存快照)               wms_inventory (WMS 明细库存)
  product_id, warehouse_id,               product_id, warehouse_id,
  quantity, available_quantity,            location_id, batch_no,
  frozen_quantity                          quantity, available_qty

WMS 写操作: 写 wms_inventory + 写 wms_inventory_log + 同步更新 erp_stock
ERP 读操作: 只读 erp_stock（兼容现有查询）
```

**不搞两套数据！** WMS 的每一次库存变动同时写 `wms_inventory_log`（流水）和 `erp_stock`（汇总快照），`erp_stock` 表作为兼容层供 ERP 查询。

---

## 四、ERP 集成设计

### 4.1 交互流程

```
┌─────────┐                    ┌─────────┐                    ┌─────────┐
│   ERP   │                    │   WMS   │                    │   PDA   │
└────┬────┘                    └────┬────┘                    └────┬────┘
     │                              │                              │
     │ ① 下发采购入库单              │                              │
     │ ──────────────────────────►  │                              │
     │                              │ ② 生成收货任务               │
     │                              │ ③ PDA 领取收货任务           │
     │                              │◄──────────────────────────── │
     │                              │ ④ 扫码收货 → 确认           │
     │                              │◄──────────────────────────── │
     │                              │ ⑤ 生成上架任务               │
     │                              │ ⑥ PDA 上架确认               │
     │                              │◄──────────────────────────── │
     │ ⑦ 回写入库完成 + 库存变动    │                              │
     │◄──────────────────────────── │                              │
     │                              │                              │
     │ ⑧ 下发销售出库单              │                              │
     │ ──────────────────────────►  │                              │
     │                              │ ⑨ 生成拣货波次/任务          │
     │                              │ ⑩ PDA 领取拣货任务           │
     │                              │◄──────────────────────────── │
     │                              │ ⑪ 扫码拣货 → 缺货标记       │
     │                              │◄──────────────────────────── │
     │                              │ ⑫ 生成发货复核任务           │
     │                              │ ⑬ PDA 复核确认               │
     │                              │◄──────────────────────────── │
     │ ⑭ 回发出库完成 + 库存扣减    │                              │
     │◄──────────────────────────── │                              │
```

### 4.2 接口契约

所有交互接口统一格式：

```json
{
  "traceId": "uuid",
  "source": "ERP|WMS",
  "eventType": "PURCHASE_ORDER_CREATED|RECEIPT_COMPLETED|...",
  "payload": {},
  "timestamp": "2026-06-11T10:00:00Z"
}
```

| 方向 | 接口 | 说明 |
|------|------|------|
| ERP → WMS | `POST /api/wms/erp/purchase-order` | 采购订单下达 |
| ERP → WMS | `POST /api/wms/erp/sale-order` | 销售订单下达 |
| ERP → WMS | `POST /api/wms/erp/product-sync` | 商品主数据同步 |
| ERP → WMS | `POST /api/wms/erp/check-command` | 盘点指令下发 |
| WMS → ERP | `POST /api/erp/wms/receipt-complete` | 收货完成回调 |
| WMS → ERP | `POST /api/erp/wms/ship-complete` | 发货完成回调 |
| WMS → ERP | `POST /api/erp/wms/inventory-change` | 库存异动通知 |
| WMS → ERP | `POST /api/erp/wms/check-diff` | 盘点差异回传 |

### 4.3 最终一致性保证

- 使用 `wms_event_outbox` 表持久化事件
- 定时任务每 10 秒扫描待发送事件
- 失败自动重试 3 次，间隔递增（10s, 30s, 60s）
- 超过 3 次标记为 `failed`，人工介入
- 幂等：ERP 侧根据 `traceId` 去重

---

## 五、PDA 接口设计

PDA 调用基础路径：`/api/v1/warehouse`

PDA 侧已有前端框架，以下为需要实现的后端接口：

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 仓库人员登录 |
| POST | `/auth/logout` | 登出 |

### 任务
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tasks` | 任务列表 |
| GET | `/tasks/{id}` | 任务详情 |
| PUT | `/tasks/{id}/start` | 领取任务 |
| PUT | `/tasks/{id}/complete` | 完成任务 |

### 收货
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/receive` | 收货任务列表 |
| GET | `/receive/{id}` | 收货详情 |
| POST | `/receive/{id}/scan` | 扫码商品 |
| POST | `/receive/{id}/confirm` | 确认收货 |
| POST | `/receive/{id}/exception` | 异常报告 |

### 上架
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/putaway` | 上架任务列表 |
| GET | `/putaway/{id}` | 上架详情 |
| POST | `/putaway/{id}/scan` | 扫码商品 |
| POST | `/putaway/{id}/confirm` | 确认上架 |

### 拣货
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/pick` | 拣货任务列表 |
| GET | `/pick/{id}` | 拣货详情 |
| POST | `/pick/{id}/verify` | 扫码验证 |
| PUT | `/pick/item/{itemId}` | 确认拣货数量 |
| PUT | `/pick/{id}/complete` | 完成拣货 |
| POST | `/pick/{id}/shortage` | 缺货标记 |

### 复核
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ship` | 复核任务列表 |
| GET | `/ship/{id}` | 复核详情 |
| POST | `/ship/{id}/scan` | 扫码复核 |
| PUT | `/ship/{id}/confirm` | 确认发货 |

### 盘点
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/check` | 盘点任务列表 |
| GET | `/check/{id}` | 盘点详情 |
| POST | `/check/{id}/scan-location` | 扫描库位 |
| POST | `/check/{id}/scan-product` | 扫码录入实盘数 |
| PUT | `/check/{id}/submit` | 提交盘点结果 |

### 库存
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/inventory/query` | 库存查询（扫码查库存） |

### 移库
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/move` | 创建移库任务 |
| GET | `/move/{id}` | 移库详情 |
| POST | `/move/{id}/execute` | 执行移库 |

---

## 六、质量要求

### 6.1 后端接口质量清单（每项 0-10，满分 100）

1. 参数校验完整（JSR-303 + 业务校验）
2. 异常处理统一（BusinessException + GlobalExceptionHandler）
3. 分页查询规范（Page 对象 + 排序）
4. 事务管理正确（@Transactional）
5. 并发安全（@Version 乐观锁）
6. 日志完整（操作日志 + trace_id 链路）
7. 权限控制（Sa-Token 注解）
8. API 文档完整（Knife4j 注解）
9. 响应格式统一（Result<T> sealed interface）
10. 边界条件覆盖（空值/极限值/并发）

### 6.2 前端页面质量清单（每项 0-10，满分 100）

见主需求文档第 6 节 95 分评估体系。

---

## 七、实施路线

| 阶段 | 内容 | 依赖 |
|------|------|------|
| 1 | 数据库实现（所有 wms_* 表） | - |
| 2 | wms-common + wms-warehouse（基础 CRUD） | 阶段 1 |
| 3 | wms-inventory（库存核心 + 异动日志） | 阶段 2 |
| 4 | wms-receipt + wms-putaway（收货上架） | 阶段 3 |
| 5 | wms-pick + wms-ship（拣货发货） | 阶段 3 |
| 6 | wms-check + wms-move（盘点移库） | 阶段 3 |
| 7 | wms-event（ERP 集成 + 事件引擎） | 阶段 4/5/6 |
| 8 | PC 管理端页面（10 页面） | 阶段 2-6 |
| 9 | PDA 后端接口实现 + 前端补充 | 阶段 2-6 |
| 10 | ERP 引用方改造 + 端到端联调 | 阶段 7 |
