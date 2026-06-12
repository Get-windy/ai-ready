# 配送管理系统 (DMS) — 架构设计文档

> 文档版本：v1.0.0
> 创建日期：2026-06-11
> 基于现有 ERP + WMS 系统架构扩展

---

## 一、系统定位

### 1.1 什么是 DMS

配送管理系统（Delivery Management System，简称 DMS）是与 ERP、WMS **平行的独立服务**，统一管理自有司机、众包运力、第三方配送平台（美团、达达、顺丰同城等）及社会车辆（出租车等），实现订单分配、运力调度、路线优化、配送执行、签收收款、费用结算的全链路管理。

### 1.2 与 ERP / WMS 的关系

```
┌────────────────────────────────────────────────────────────┐
│                     外部配送平台                            │
│      美团配送   达达   顺丰同城   社会车辆/出租车           │
└────────────────────┬───────────────────────────────────────┘
                     │ 公网回调接口
                     ▼
┌────────────────────────────────────────────────────────────┐
│              DMS 配送管理系统（独立服务）                   │
│                                                           │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ 运力池   │ │ 订单大厅  │ │ 调度引擎  │ │ 外部平台适配器│  │
│  │ 管理     │ │ 抢单/竞价 │ │ 自动/手动 │ │ 美团/达达/.. │  │
│  └─────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ 配送执行  │ │ 签收     │ │ 现场收款  │ │ 费用结算     │  │
│  │ 装车/路线 │ │ 拍照/签名 │ │ 二维码/现 │ │ 配送费/竞价  │  │
│  └─────────┘ └──────────┘ └──────────┘ └──────────────┘  │
└─────────┬─────────────────┬───────────────────────────────┘
          │ HTTP API         │ HTTP API
          ▼                  ▼
┌────────────────┐  ┌────────────────┐
│   ERP 系统     │  │   WMS 系统     │
│  (独立服务)    │  │  (独立服务)    │
│  订单/客户/    │  │  拣货/出库     │
│  商品/财务     │  │  库存异动      │
└────────────────┘  └────────────────┘
```

### 1.3 设计原则

| 原则 | 说明 |
|------|------|
| **独立部署** | DMS 作为一个独立 Spring Boot 应用运行，拥有独立进程和端口 |
| **不引入新框架** | 复用现有 Spring Boot 3.2.5 + MyBatis-Plus + Sa-Token 技术栈 |
| **不引入 MQ** | 沿用现有事件发件箱 + HTTP 回调模式，保证最终一致性 |
| **共享数据库服务器** | 使用独立 schema `dms_`，表设计严格遵循现有命名规范 |
| **适配器模式** | 每个外部配送平台独立 Adapter，实现统一接口，通过配置启用/禁用 |
| **租户隔离** | 所有表含 `tenant_id`，竞价开关等配置为租户级 |
| **前端嵌入** | PC 管理端页面可嵌入 ERP 菜单（微前端或链接跳转），移动端为独立 App |
| **质量自动评估** | 每个功能模块完成后按 95 分标准自评，不达标自动循环修复 |

---

## 二、技术选型

| 项 | 选择 | 理由 |
|----|------|------|
| 框架 | Spring Boot 3.2.5 + MyBatis-Plus | 与主项目一致 |
| 语言 | Java 17 | 与主项目一致 |
| 数据库 | PostgreSQL | 与主项目一致，独立 schema `dms_` |
| 权限 | Sa-Token 1.37.0 | `@SaCheckLogin` `@SaCheckPermission` |
| API 文档 | Knife4j (OpenAPI 3) | 与主项目一致 |
| 统一响应 | `ApiResponse<T>` + `Result<T>` | 与主项目一致 |
| 乐观锁 | `@Version` 注解 | 继承 BaseEntity 机制 |
| 模块间通信 | Spring `ApplicationEvent` | 轻量，无额外中间件依赖 |
| 跨系统集成 | 数据库事件表 + 定时任务 | 无 MQ 依赖，保证最终一致性 |
| 参数校验 | JSR-303 + 业务校验 | 与主项目一致 |
| 日志 | 操作日志 + trace_id 链路 | 与主项目一致 |
| 前端 PC | Vue 3 + Ant Design Vue | 与主项目一致，`apps/pc-admin` 扩展 |
| 前端移动 | Vue 3 + Vant 4 | 基于现有 `apps/driver-delivery` 扩展 |

---

## 三、服务部署

### 3.1 部署架构

```yaml
# 三服务并行部署示意
services:
  erp-service:
    port: 8080
    context: /api/erp

  wms-service:
    port: 8081
    context: /api/wms

  dms-service:
    port: 8082          # 独立端口
    context: /api/dms
    external-callback: /api/dms/callback/{channel}  # 公网回调
```

### 3.2 服务发现与配置

暂不引入注册中心，通过配置文件管理服务地址：

```yaml
# dms 配置 erp 和 wms 地址
dms:
  erp:
    base-url: http://erp-service:8080
  wms:
    base-url: http://wms-service:8081
```

### 3.3 外部平台回调

- 需公网可达，建议通过 Nginx/API 网关暴露 `/api/dms/callback/*`
- 安全验证：签名校验（HMAC-SHA256）+ IP 白名单
- 幂等处理：`traceId` 去重

---

## 四、数据库设计

### 4.1 命名规范

- 表前缀：`dms_`（Delivery Management System）
- 主键：`BIGSERIAL`
- 金额：`decimal(18,2)`
- 数量：`decimal(18,4)`
- 状态：`INTEGER` 或 `VARCHAR`
- 审计字段：`tenant_id`, `deleted`, `create_time`, `update_time`, `create_by`, `update_by`, `version`

### 4.2 表清单

#### 4.2.1 配送渠道（dms_channel）

配送渠道定义表，管理所有配送方式的基础配置。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| channel_code | VARCHAR(50) | 渠道编码（own/staff/meituan/dada/shunfeng/taxi） |
| channel_name | VARCHAR(200) | 渠道名称 |
| channel_type | INTEGER | 类型：1-自有员工 2-众包兼职 3-外部平台 4-社会车辆 |
| adapter_bean | VARCHAR(200) | 适配器Spring Bean名称 |
| config_json | TEXT | 渠道配置JSON（API密钥、回调URL等） |
| status | INTEGER | 状态：0-禁用 1-启用 |
| priority | INTEGER | 调度优先级（数字越小优先级越高） |
| sort_order | INTEGER | 排序 |
| remark | VARCHAR(500) | 备注 |
| deleted | INTEGER | 逻辑删除 |
| create/update_time | TIMESTAMP | 审计时间 |
| create/update_by | BIGINT | 审计人 |
| version | INTEGER | 乐观锁 |

#### 4.2.2 配送员（dms_rider）

配送员信息表，覆盖自有员工、众包骑手。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| user_id | BIGINT | 关联系统用户ID |
| rider_type | INTEGER | 类型：1-自有员工 2-众包兼职 3-外部平台骑手 4-社会车辆司机 |
| real_name | VARCHAR(100) | 真实姓名 |
| phone | VARCHAR(20) | 手机号 |
| id_card | VARCHAR(20) | 身份证号 |
| vehicle_type | VARCHAR(50) | 车辆类型（电动车/小货车/面包车等） |
| vehicle_no | VARCHAR(50) | 车牌号 |
| channel_id | BIGINT | 所属渠道ID |
| service_radius | DECIMAL(10,2) | 服务半径（公里） |
| current_lat | DECIMAL(10,6) | 当前位置纬度 |
| current_lng | DECIMAL(10,6) | 当前位置经度 |
| last_report_time | TIMESTAMP | 最后位置上报时间 |
| status | INTEGER | 接单状态：0-离线 1-空闲 2-忙碌 3-休息 |
| verify_status | INTEGER | 审核状态：0-待审核 1-已通过 2-已拒绝 |
| rating_score | DECIMAL(3,2) | 综合评分（1.00-5.00） |
| total_orders | INTEGER | 累计配送单数 |
| completed_orders | INTEGER | 完成单数 |
| deposit_amount | DECIMAL(18,2) | 保证金金额 |
| max_concurrent | INTEGER | 最大并行配送数 |
| remark | VARCHAR(500) | 备注 |
| deleted | INTEGER | 逻辑删除 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.3 配送任务（dms_task）

配送任务核心表，关联订单和配送员。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| task_no | VARCHAR(100) | 任务编号（生成规则：DMS+年月日+流水号） |
| order_id | BIGINT | ERP订单ID |
| order_no | VARCHAR(100) | ERP订单号 |
| order_type | INTEGER | 订单类型：1-销售配送 2-调拨 3-退货 |
| channel_id | BIGINT | 配送渠道ID |
| rider_id | BIGINT | 配送员ID |
| dispatch_type | INTEGER | 分配方式：1-自动分配 2-手动指派 3-抢单 4-竞价 |
| source_warehouse_id | BIGINT | 取货仓库ID |
| source_address | VARCHAR(500) | 取货地址 |
| source_lat | DECIMAL(10,6) | 取货坐标纬度 |
| source_lng | DECIMAL(10,6) | 取货坐标经度 |
| customer_id | BIGINT | 客户ID |
| customer_name | VARCHAR(200) | 客户名称 |
| customer_phone | VARCHAR(20) | 客户电话 |
| customer_address | VARCHAR(500) | 收货地址 |
| customer_lat | DECIMAL(10,6) | 收货坐标纬度 |
| customer_lng | DECIMAL(10,6) | 收货坐标经度 |
| total_items | INTEGER | 商品种类数 |
| total_quantity | DECIMAL(18,4) | 总数量 |
| total_weight | DECIMAL(18,4) | 总重量（kg） |
| total_volume | DECIMAL(18,4) | 总体积（m³） |
| goods_amount | DECIMAL(18,2) | 货品金额 |
| delivery_fee | DECIMAL(18,2) | 配送费 |
| collect_on_delivery | DECIMAL(18,2) | 到付金额 |
| estimated_distance | DECIMAL(10,2) | 预估距离（公里） |
| priority | INTEGER | 优先级：1-普通 2-紧急 3-加急 |
| status | INTEGER | 任务状态（见状态机） |
| urge_count | INTEGER | 催单次数 |
| urge_time | TIMESTAMP | 最后催单时间 |
| load_time | TIMESTAMP | 装车时间 |
| dispatch_time | TIMESTAMP | 分配时间 |
| pickup_time | TIMESTAMP | 取货时间 |
| delivery_time | TIMESTAMP | 送达时间 |
| completed_time | TIMESTAMP | 完成时间 |
| deadline_time | TIMESTAMP | 截止时间 |
| remark | VARCHAR(500) | 备注 |
| 审计字段 | ... | 标准审计字段 |

**任务状态机**：

```
待分配 ──► 已分配 ──► 已接单 ──► 取货中 ──► 配送中 ──► 已签收 ──► 已完成
  │           │          │          │           │
  └──► 已取消  └──► 已取消 └──► 异常  └──► 异常   └──► 异常
```

状态枚举值：0-待分配 1-已分配 2-已接单 3-取货中 4-配送中 5-已签收 6-已完成 7-已取消 8-异常

#### 4.2.4 订单大厅（dms_order_pool）

订单大厅表，管理待分配订单的抢单/竞价。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| task_id | BIGINT | 关联任务ID |
| delivery_fee | DECIMAL(18,2) | 配送费 |
| bid_enabled | INTEGER | 是否启用竞价：0-否 1-是 |
| bid_start_price | DECIMAL(18,2) | 起拍价 |
| bid_current_price | DECIMAL(18,2) | 当前最低出价 |
| bid_start_time | TIMESTAMP | 竞价开始时间 |
| bid_end_time | TIMESTAMP | 竞价截止时间 |
| bid_count | INTEGER | 出价人数 |
| pool_status | INTEGER | 大厅状态：0-待抢单 1-竞价中 2-已接单 3-已过期 4-已下架 |
| published_time | TIMESTAMP | 发布时间 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.5 竞价记录（dms_bid）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| pool_id | BIGINT | 大厅记录ID |
| task_id | BIGINT | 任务ID |
| rider_id | BIGINT | 配送员ID |
| rider_name | VARCHAR(100) | 配送员名称 |
| bid_price | DECIMAL(18,2) | 报价金额 |
| is_win | INTEGER | 是否中标：0-否 1-是 |
| bid_time | TIMESTAMP | 出价时间 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.6 位置轨迹（dms_tracking）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| rider_id | BIGINT | 配送员ID |
| task_id | BIGINT | 当前任务ID |
| lat | DECIMAL(10,6) | 纬度 |
| lng | DECIMAL(10,6) | 经度 |
| speed | DECIMAL(10,2) | 速度（km/h） |
| direction | DECIMAL(5,2) | 方向角 |
| report_time | TIMESTAMP | 上报时间 |
| source | INTEGER | 来源：1-APP上报 2-后台查询 |

#### 4.2.7 签收记录（dms_sign）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| task_id | BIGINT | 任务ID |
| sign_type | INTEGER | 签收类型：1-正常签收 2-部分签收 3-拒收 |
| photo_urls | TEXT | 照片URL列表（JSON数组） |
| signature_url | VARCHAR(500) | 手写签名图片URL |
| sign_lat | DECIMAL(10,6) | 签收位置纬度 |
| sign_lng | DECIMAL(10,6) | 签收位置经度 |
| customer_lat | DECIMAL(10,6) | 客户登记坐标纬度 |
| customer_lng | DECIMAL(10,6) | 客户登记坐标经度 |
| location_deviation | DECIMAL(10,2) | 定位偏差（米） |
| location_warning | INTEGER | 偏差超阈值警告：0-正常 1-超阈值 |
| new_customer_lat | DECIMAL(10,6) | 更新后的客户坐标纬度 |
| new_customer_lng | DECIMAL(10,6) | 更新后的客户坐标经度 |
| remark | VARCHAR(500) | 备注 |
| sign_time | TIMESTAMP | 签收时间 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.8 现场收款（dms_payment）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| task_id | BIGINT | 任务ID |
| payment_type | INTEGER | 收款方式：1-微信 2-支付宝 3-现金 4-其他 |
| amount | DECIMAL(18,2) | 收款金额 |
| qrcode_url | VARCHAR(500) | 收款二维码URL |
| external_order_no | VARCHAR(100) | 外部支付流水号 |
| pay_time | TIMESTAMP | 支付时间 |
| status | INTEGER | 状态：0-待支付 1-已支付 2-未付标记 |
| unpaid_remark | VARCHAR(500) | 未付原因备注 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.9 外部平台订单映射（dms_external_order）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| task_id | BIGINT | 内部任务ID |
| channel_id | BIGINT | 渠道ID |
| channel_order_no | VARCHAR(200) | 外部平台订单号 |
| channel_status | VARCHAR(50) | 外部平台状态 |
| estimated_fee | DECIMAL(18,2) | 平台预估费用 |
| actual_fee | DECIMAL(18,2) | 平台实结费用 |
| fee_difference | DECIMAL(18,2) | 差异金额 |
| callback_data | TEXT | 回调原始数据 |
| callback_time | TIMESTAMP | 回调时间 |
| status | INTEGER | 同步状态：0-待同步 1-已同步 2-异常 |
| retry_count | INTEGER | 重试次数 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.10 租户配置（dms_config）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| config_key | VARCHAR(200) | 配置键 |
| config_value | TEXT | 配置值（JSON格式） |
| config_desc | VARCHAR(500) | 配置说明 |
| scope | VARCHAR(50) | 生效范围：global/channel/rider |
| 审计字段 | ... | 标准审计字段 |

预定义配置键：

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `bid_enabled` | `false` | 竞价开关（租户级） |
| `bid_duration_minutes` | `30` | 竞价时限（分钟） |
| `bid_win_rule` | `lowest_price` | 中标规则：lowest_price/综合评分 |
| `dispatch_priority` | `["own","crowd","platform"]` | 调度优先级顺序 |
| `service_radius` | `20` | 默认服务半径（公里） |
| `location_deviation_threshold` | `500` | 签收定位偏差阈值（米） |
| `delivery_fee_per_km` | `2.0` | 每公里配送费 |
| `delivery_fee_base` | `5.0` | 基础配送费 |
| `auto_dispatch_enabled` | `true` | 自动分配开关 |
| `work_hours_start` | `"08:00"` | 上班时段-开始时间 |
| `work_hours_end` | `"18:00"` | 上班时段-结束时间 |
| `work_hours_enabled` | `true` | 上班时段控制开关 |

#### 4.2.11 自有车辆（dms_vehicle）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| vehicle_code | VARCHAR(100) | 车辆编码 |
| plate_no | VARCHAR(50) | 车牌号 |
| brand | VARCHAR(100) | 车辆品牌 |
| model | VARCHAR(100) | 车辆型号 |
| color | VARCHAR(50) | 车辆颜色 |
| vehicle_type | INTEGER | 车辆类型：1-电动车 2-小货车 3-面包车 4-厢式货车 5-冷藏车 6-三轮车 |
| vin | VARCHAR(50) | 车架号(VIN) |
| engine_no | VARCHAR(50) | 发动机号 |
| rated_load | DECIMAL(18,2) | 核定载质量(kg) |
| rated_passenger | INTEGER | 核定载客人数 |
| curb_weight | DECIMAL(18,2) | 整备质量(kg) |
| length_cm | DECIMAL(10,2) | 车长(cm) |
| width_cm | DECIMAL(10,2) | 车宽(cm) |
| height_cm | DECIMAL(10,2) | 车高(cm) |
| cargo_volume | DECIMAL(18,2) | 车厢容积(m³) |
| register_date | DATE | 注册日期 |
| operating_permit_no | VARCHAR(100) | 运营证号 |
| insurance_expire_date | DATE | 保险到期日 |
| inspection_expire_date | DATE | 年检到期日 |
| maintenance_interval_km | INTEGER | 保养周期(公里) |
| last_maintenance_km | INTEGER | 上次保养里程 |
| last_maintenance_date | DATE | 上次保养日期 |
| current_mileage | INTEGER | 当前里程(公里) |
| vehicle_manager_id | BIGINT | 车辆负责人ID |
| vehicle_manager_name | VARCHAR(100) | 车辆负责人姓名 |
| vehicle_manager_phone | VARCHAR(20) | 车辆负责人手机号 |
| current_rider_id | BIGINT | 当前配送驾驶员ID |
| current_rider_name | VARCHAR(100) | 当前配送驾驶员姓名 |
| ownership_type | INTEGER | 归属：1-公司自有 2-个人自带 3-租赁 |
| department | VARCHAR(100) | 所属部门 |
| status | INTEGER | 状态：0-空闲 1-使用中 2-维修中 3-已报废 |
| gps_enabled | INTEGER | 是否启用GPS：0-否 1-是 |
| gps_device_no | VARCHAR(100) | GPS设备编号 |
| remark | VARCHAR(500) | 备注 |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.12 车辆维保记录（dms_vehicle_maintenance）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| vehicle_id | BIGINT | 关联车辆ID |
| maint_type | INTEGER | 类型：1-保养 2-维修 3-年检 4-保险 5-事故 |
| maint_no | VARCHAR(100) | 维保单号 |
| maint_date | DATE | 维保日期 |
| maint_content | TEXT | 维保内容 |
| maint_cost | DECIMAL(18,2) | 维保费用 |
| maint_vendor | VARCHAR(200) | 维保厂商 |
| after_maint_mileage | INTEGER | 维保后里程 |
| next_maint_mileage | INTEGER | 下次维保里程 |
| next_maint_date | DATE | 下次维保日期 |
| attachment_urls | TEXT | 附件URLs |
| 审计字段 | ... | 标准审计字段 |

#### 4.2.13 事件发件箱（dms_event_outbox）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| trace_id | VARCHAR(100) | 链路追踪ID |
| event_type | VARCHAR(100) | 事件类型 |
| source | VARCHAR(50) | 来源：DMS/ERP/WMS |
| target | VARCHAR(50) | 目标：ERP/WMS |
| payload | TEXT | 请求体JSON |
| status | INTEGER | 状态：0-待发送 1-已发送 2-失败 |
| retry_count | INTEGER | 重试次数 |
| last_error | TEXT | 最后错误信息 |
| next_retry_time | TIMESTAMP | 下次重试时间 |
| 审计字段 | ... | 标准审计字段 |

---

## 五、模块架构

### 5.1 模块树

```
backend/dms/
├── pom.xml                         # 父 POM，聚合子模块
├── DMS-ARCHITECTURE.md             # 本架构文档
├── AGENTS.md                       # AI 辅助开发指南
│
├── dms-common/                     # 通用定义
│   ├── enums/                      # 任务状态、骑手类型、渠道类型枚举
│   ├── constant/                   # DMS 常量
│   └── exception/                  # 领域异常
│
├── dms-channel/                    # 配送渠道 + 外部平台适配器
│   ├── adapter/                    # 统一适配器接口 + 各平台实现
│   │   ├── DeliveryAdapter.java    # 统一接口：创建/查询/取消/预估/定位
│   │   ├── OwnStaffAdapter.java    # 自有员工适配器（内部调度）
│   │   ├── MeituanAdapter.java     # 美团配送适配器
│   │   ├── DadaAdapter.java        # 达达适配器
│   │   ├── ShunfengAdapter.java    # 顺丰同城适配器
│   │   └── SocialVehicleAdapter.java # 社会车辆模拟适配器
│   ├── entity/                     # DmsChannel
│   ├── service/                    # ChannelService（适配器管理）
│   └── controller/                 # /api/dms/channel
│
├── dms-rider/                      # 运力管理
│   ├── entity/                     # DmsRider
│   ├── service/                    # RiderService
│   ├── mapper/                     # DmsRiderMapper
│   └── controller/                 # /api/dms/rider
│
├── dms-task/                       # 配送任务核心
│   ├── entity/                     # DmsTask
│   ├── service/                    # TaskService
│   ├── mapper/                     # DmsTaskMapper
│   └── controller/                 # /api/dms/task
│
├── dms-order-pool/                 # 订单大厅
│   ├── entity/                     # DmsOrderPool, DmsBid
│   ├── service/                    # OrderPoolService, BidService
│   ├── mapper/                     # 对应 Mapper
│   └── controller/                 # /api/dms/order-pool
│
├── dms-dispatch/                   # 智能调度引擎
│   ├── service/                    # DispatchService（自动匹配/手动改派/围栏校验）
│   └── controller/                 # /api/dms/dispatch
│
├── dms-execution/                  # 配送执行
│   ├── entity/                     # 执行相关（可选）
│   ├── service/                    # ExecutionService（装车/路线/催单）
│   ├── mapper/
│   └── controller/                 # /api/dms/execution
│
├── dms-sign/                       # 签收
│   ├── entity/                     # DmsSign
│   ├── service/                    # SignService（定位比对）
│   ├── mapper/
│   └── controller/                 # /api/dms/sign
│
├── dms-payment/                    # 现场收款
│   ├── entity/                     # DmsPayment
│   ├── service/                    # PaymentService
│   ├── mapper/
│   └── controller/                 # /api/dms/payment
│
├── dms-vehicle/                    # 自有配送车辆管理
│   ├── entity/                     # DmsVehicle（车辆档案）、DmsVehicleMaintenance（维保记录）
│   ├── service/                    # VehicleService（档案/维保/保险/年检提醒）
│   ├── mapper/
│   ├── dto/                        # VehicleCreateDTO, VehicleVO, MaintenanceCreateDTO
│   └── controller/                 # /api/dms/vehicle
│
├── dms-route/                      # 路线规划与地图服务
│   ├── spi/                        # MapService 统一接口 + Amap / Tencent / Baidu 实现
│   ├── service/                    # RouteService（配送路线优化/地理编码/围栏）
│   ├── dto/                        # RoutePlanRequest/Response, GeocodeRequest/Response, ...
│   └── controller/                 # /api/dms/route
│
├── dms-tracking/                   # 位置追踪
│   ├── entity/                     # DmsTracking
│   ├── service/                    # TrackingService
│   ├── mapper/
│   └── controller/                 # /api/dms/tracking
│
├── dms-verification/               # 人车绑定与位置核验
│   ├── entity/                     # DmsRiderVehicleBinding（绑定记录）
│   │                                # DmsVehicleInspection（出车验车）
│   │                                # DmsPositionVerification（核验记录）
│   │                                # DmsVerificationAlert（异常告警）
│   ├── service/                    # VerificationService
│   │                                #   - 人车绑定/交车/解绑
│   │                                #   - 出车验车/收车验车
│   │                                #   - 位置核验/异常检测
│   │                                #   - 分离滞留告警
│   ├── enums/                      # BindingStatusEnum, AlertTypeEnum
│   ├── mapper/
│   ├── dto/
│   └── controller/                 # /api/dms/verification
│
├── dms-settlement/                 # 费用结算
│   ├── entity/                     # 结算相关
│   ├── service/                    # SettlementService
│   ├── mapper/
│   └── controller/                 # /api/dms/settlement
│
├── dms-config/                     # 租户配置
│   ├── entity/                     # DmsConfig
│   ├── service/                    # ConfigService
│   ├── mapper/
│   └── controller/                 # /api/dms/config
│
├── dms-event/                      # 事件集成
│   ├── entity/                     # DmsEventOutbox
│   ├── service/                    # EventService（重试/回调）
│   ├── mapper/
│   └── controller/                 # 回调入口
│
└── dms-bootstrap/                  # Spring Boot 启动入口
    ├── src/main/java/cn/aiedge/dms/
    │   ├── DmsApplication.java     # 主启动类
    │   └── config/                 # Spring 配置
    ├── src/main/resources/
    │   ├── application.yml         # 主配置
    │   ├── application-dev.yml     # 开发配置
    │   └── application-prod.yml    # 生产配置
    └── sql/
        └── V1.0.0__Create_DMS_Tables.sql  # 数据库初始化
```

### 5.2 统一适配器接口

```java
/**
 * 配送平台统一适配器接口
 * 每个外部平台独立实现此接口
 */
public interface DeliveryAdapter {
    /** 适配器唯一标识 */
    String getChannelCode();
    
    /** 创建配送单 */
    CreateResult createOrder(CreateRequest request);
    
    /** 查询配送状态 */
    QueryResult queryOrder(String channelOrderNo);
    
    /** 取消配送 */
    CancelResult cancelOrder(String channelOrderNo);
    
    /** 预估配送费用 */
    EstimateResult estimateFee(EstimateRequest request);
    
    /** 查询骑手位置 */
    LocationResult queryRiderLocation(String channelOrderNo);
}
```

### 5.3 事件交互流程

```
┌─────────┐                    ┌─────────┐                    ┌─────────┐
│   ERP   │                    │   DMS   │                    │   WMS   │
└────┬────┘                    └────┬────┘                    └────┬────┘
     │                              │                              │
     │ ① 推送待配送订单              │                              │
     │ ──────────────────────────►  │                              │
     │                              │                              │
     │                              │ ② 接收拣货完成事件            │
     │                              │◄──────────────────────────── │
     │                              │                              │
     │                              │ ③ 自动调度/进入订单大厅      │
     │                              │                              │
     │                              │ ④ 配送员接单                 │
     │                              │ ← 移动端                     │
     │                              │                              │
     │                              │ ⑤ 回推配送状态变更           │
     │ ◄──────────────────────────  │                              │
     │                              │                              │
     │                              │ ⑥ 签收提交 (定位比对)        │
     │                              │ ← 移动端                     │
     │                              │                              │
     │                              │ ⑦ 收款信息推送               │
     │ ◄──────────────────────────  │                              │
     │                              │                              │
     │ ⑧ 结算数据提供给财务模块      │                              │
     │ ◄──────────────────────────  │                              │
```

---

## 六、接口设计

### 6.1 与 ERP 交互接口

DMS 提供回调接口供 ERP 调用，同时调用 ERP 接口推送数据。

**① 接收 ERP 推送的待配送订单**

| 条目 | 内容 |
|------|------|
| 方向 | ERP → DMS |
| 方法 | POST |
| 路径 | `/api/dms/erp/sale-order` |
| 触发 | ERP 销售订单审批通过/出库完成 |
| 幂等 | traceId 去重 |
| Body | `{ traceId, source: "ERP", eventType: "SALE_ORDER_DISPATCH", payload: { orderId, orderNo, customerInfo, items[], warehouseInfo, ... }, timestamp }` |

**② 回推配送状态变更**

| 条目 | 内容 |
|------|------|
| 方向 | DMS → ERP |
| 方法 | POST |
| 路径 | `/api/erp/dms/delivery-status` |
| 触发 | DMS 任务状态变更 |
| 幂等 | traceId 去重（ERP 侧实现） |
| Body | `{ traceId, source: "DMS", eventType: "DELIVERY_STATUS_CHANGE", payload: { orderId, taskNo, status, riderInfo, time, ... }, timestamp }` |

**③ 同步基础数据**

| 条目 | 内容 |
|------|------|
| 方向 | ERP → DMS |
| 方法 | POST |
| 路径 | `/api/dms/erp/customer-sync` |
| 说明 | 客户信息变更时推送 |

**④ 推送收款信息**

| 条目 | 内容 |
|------|------|
| 方向 | DMS → ERP |
| 方法 | POST |
| 路径 | `/api/erp/dms/payment-info` |
| 说明 | 现场收款完成或未付款标记 |

### 6.2 与 WMS 交互接口

**① 接收 WMS 拣货完成事件**

| 条目 | 内容 |
|------|------|
| 方向 | WMS → DMS |
| 方法 | POST |
| 路径 | `/api/dms/wms/pick-complete` |
| Body | `{ traceId, source: "WMS", eventType: "PICK_COMPLETED", payload: { orderId, warehouseId, locationGuide, ... }, timestamp }` |

**② 查询拣货状态**

| 条目 | 内容 |
|------|------|
| 方向 | DMS → WMS |
| 方法 | GET |
| 路径 | `/api/wms/dms/pick-status?orderId={orderId}` |

### 6.3 内部接口

#### 6.3.1 订单大厅

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dms/order-pool/page` | 分页查询大厅订单列表 |
| GET | `/api/dms/order-pool/{id}` | 大厅订单详情 |
| POST | `/api/dms/order-pool/{id}/grab` | 抢单 |
| POST | `/api/dms/order-pool/{id}/bid` | 竞价出价 |
| GET | `/api/dms/order-pool/{id}/bid-list` | 竞价记录列表（对骑手隐藏金额） |
| POST | `/api/dms/order-pool/{id}/cancel-bid` | 取消出价 |

#### 6.3.2 调度

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/dispatch/auto` | 自动匹配运力 |
| POST | `/api/dms/dispatch/{taskId}/assign` | 手动指派 |
| GET | `/api/dms/dispatch/candidates` | 查询候选运力（围栏过滤） |
| POST | `/api/dms/dispatch/{taskId}/fence-check` | 围栏校验 |

#### 6.3.3 配送执行

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/execution/{taskId}/load` | 装车确认（扫描/勾选订单） |
| GET | `/api/dms/execution/{taskId}/route` | 获取优化路线 |
| PUT | `/api/dms/execution/{taskId}/route` | 手动调整路线 |
| POST | `/api/dms/execution/{taskId}/urge` | 催单 |
| POST | `/api/dms/execution/{taskId}/pickup` | 确认取货 |
| POST | `/api/dms/execution/{taskId}/arrive` | 到达确认 |

#### 6.3.4 签收

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/sign/submit` | 提交签收（照片+签名+坐标） |
| GET | `/api/dms/sign/{taskId}` | 查询签收记录 |

#### 6.3.5 收款

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/payment/qrcode` | 生成收款二维码 |
| POST | `/api/dms/payment/confirm` | 确认收款 |
| POST | `/api/dms/payment/mark-unpaid` | 标记未付款 |
| GET | `/api/dms/payment/{taskId}` | 查询收款记录 |

#### 6.3.6 运力管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/rider/register` | 配送员注册 |
| POST | `/api/dms/rider/{id}/approve` | 审核 |
| PUT | `/api/dms/rider/{id}` | 更新信息 |
| POST | `/api/dms/rider/{id}/status` | 更新接单状态 |
| POST | `/api/dms/rider/location` | 位置上报 |
| GET | `/api/dms/rider/{id}/track` | 轨迹查询 |
| GET | `/api/dms/rider/page` | 分页查询 |

#### 6.3.7 自有车辆管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dms/vehicle/page` | 分页查询车辆 |
| GET | `/api/dms/vehicle/{id}` | 车辆详情 |
| POST | `/api/dms/vehicle` | 新增车辆 |
| PUT | `/api/dms/vehicle/{id}` | 更新车辆 |
| PUT | `/api/dms/vehicle/{id}/status` | 更新车辆状态（空闲/使用中/维修/报废） |
| POST | `/api/dms/vehicle/{id}/bind-rider` | 绑定驾驶员 |
| PUT | `/api/dms/vehicle/{id}/mileage` | 更新里程 |
| GET | `/api/dms/vehicle/maintenance-due` | 待保养/年检/保险到期提醒 |
| DELETE | `/api/dms/vehicle/{id}` | 删除车辆 |
| GET | `/api/dms/vehicle/maintenance/page` | 分页查询维保记录 |
| POST | `/api/dms/vehicle/maintenance` | 新增维保记录 |
| DELETE | `/api/dms/vehicle/maintenance/{id}` | 删除维保记录 |

#### 6.3.8 路线规划与地图服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/route/plan` | 配送路线规划（多点最优） |
| GET | `/api/dms/route/geocode` | 地理编码：地址转坐标 |
| GET | `/api/dms/route/reverse-geocode` | 逆地理编码：坐标转地址 |
| POST | `/api/dms/route/distance` | 批量距离计算 |
| GET | `/api/dms/route/fence-check` | 电子围栏校验 |

#### 6.3.9 渠道配置

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dms/channel/page` | 渠道列表 |
| POST | `/api/dms/channel` | 新增渠道 |
| PUT | `/api/dms/channel/{id}` | 更新渠道 |
| PUT | `/api/dms/channel/{id}/status` | 启停渠道 |

#### 6.3.10 配置管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dms/config/{key}` | 获取租户配置 |
| PUT | `/api/dms/config/{key}` | 更新租户配置 |
| GET | `/api/dms/config/list` | 获取所有配置 |

### 6.4 外部平台回调接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/dms/callback/{channel}` | 统一回调入口，{channel} 标识平台编码 |

安全设计：
- 签名验证：`HMAC-SHA256(secret, payload)` 校验 `X-Signature` 请求头
- 时间戳：`X-Timestamp` 请求头，允许偏差 5 分钟
- IP 白名单：可配置
- 幂等：根据 `traceId` 或外部订单号去重

---

## 七、订单大厅设计

### 7.1 基础模式 - 抢单大厅

未被自动匹配的订单进入抢单大厅，配送员先到先得：

```
1. 系统自动分配未成功 → 订单进入大厅
2. 大厅订单展示：距离、商品、时效、配送费
3. 配送员点击【接单】
4. 系统校验：订单状态（未被抢）、配送员资格（围栏、负荷）
5. 锁定订单，从大厅移除，创建配送任务
6. 通知其他配送员该订单已接
```

### 7.2 高级模式 - 竞价大厅

后台配置开启后，部分订单进入竞价流程：

```
1. 自动分配未成功 → 订单进入大厅（标记为竞价）
2. 后台配置竞价参数：起拍价、竞价时限、中标规则
3. 配送员可见"已有X人竞价"（隐藏具体金额）
4. 竞价时限截止 → 按规则选择中标者
5. 无人竞价 → 退回普通抢单模式
6. 竞价期间普通抢单按钮隐藏
```

### 7.3 竞价配置开关

- 配置层级：租户级（`dms_config` 中的 `bid_enabled`）
- 默认关闭
- PC 后台可开启/关闭
- 控制订单大厅中是否显示竞价入口

---

## 八、智能调度引擎设计

### 8.1 自动分配流程

```
输入：待分配订单（含客户坐标、商品信息）
           │
           ▼
    ① 电子围栏过滤
    ─ 找出服务范围内（可配置半径）的空闲运力
    ─ 排除忙碌/离线/暂停接单的运力
           │
           ▼
    ② 按优先级排序
    ─ 自有员工 > 众包兼职 > 外部平台
    ─ 或按成本最低自动比价（可配置）
           │
           ▼
    ③ 负荷均衡
    ─ 优先分配给当前任务数最少的运力
    ─ 考虑 max_concurrent 上限
           │
           ▼
    ④ 自动分配或进入大厅
    ─ 匹配到最优运力 → 自动分配
    ─ 未匹配到 → 进入订单大厅
```

### 8.2 手动改派

- PC 管理端可查看配送员实时位置
- 支持将任务从当前配送员改派给其他人
- 改派记录留存

### 8.3 调度优先级配置

```
dms_config:
  dispatch_priority:
    - "own"        # 自有员工（成本可控、服务质量高）
    - "crowd"      # 众包兼职（灵活、成本适中）
    - "platform"   # 外部平台（覆盖面广、费用较高）
    - "taxi"       # 社会车辆（应急场景）
```

---

## 九、配送执行与监控

### 9.1 装车确认

- 司机在移动端扫描/勾选待配送订单
- 确认装车后，任务状态变更为"取货中"
- 支持批量装车

### 9.2 路线优化

- 自动根据订单地址规划最优路线
- 考虑距离、时效、交通状况
- 支持手动拖拽调整顺序
- 催单时动态调整优先级

### 9.3 催单

- 客户/客服可发起催单
- 记录催单次数和时间
- 高频催单可提升任务优先级
- 通知配送员（App 推送/短信）

### 9.4 签收

- 拍照签收：多张照片上传
- 手写签名上传
- **强制定位比对**：
  - 获取签收坐标
  - 与客户登记坐标计算偏差
  - 偏差超阈值（可配置，默认 500 米）警告
  - 允许更新客户收货坐标
- 异常情况：拒收、部分签收、地址错误、拍照备注

### 9.5 收款

- 生成微信/支付宝收款二维码（直接展示或转发给客户）
- 支持标记未付款并上报 ERP
- 收付款记录同步到 ERP 财务模块

---

## 十、人车绑定与位置核验

### 10.1 职责分离模型

| 角色 | 职责 | 绑定方式 |
|------|------|---------|
| **车辆负责人（车辆管理员）** | 长期固定负责车辆的日常维护、保养、年检、保险事务 | `dms_vehicle.vehicle_manager_id` 永久关联 |
| **配送驾驶员（临时绑定）** | 每次出车时临时绑定车辆，配送完成后交车解绑 | `dms_rider_vehicle_binding` 动态关联 |

> 车辆负责人与配送驾驶员**可能不是同一人**。例如：车辆负责人是车队管理员，配送驾驶员是当班骑手。负责人不变，驾驶员每次出车不同。

### 10.2 全生命周期流程

```
┌──────────────────────────────────────────────────────────┐
│                    出车流程（必须完整）                    │
│                                                          │
│  ① 配送员到达车场 → 核验身份（是否为主驾驶员/授权驾驶员） │
│  ② 出车验车：外观/轮胎/灯光/刹车/灭火器/警示牌           │
│     ├─ 验车通过 → 继续                                   │
│     └─ 验车不通过 → 禁止出车，通知车辆负责人维修           │
│  ③ 人车绑定：系统记录 binding 记录，状态=ACTIVE          │
│  ④ 开始配送 → 系统启动周期性位置核验                      │
│                                                          │
│                    配送中                                │
│  ⑤ 周期性核验人车位置（配GPS vs 车GPS）                  │
│     ├─ 位置一致（<阈值）→ 正常                           │
│     ├─ 位置分离（>阈值连续N次）→ 产生分离告警             │
│     └─ 车辆长时间不动（>阈值）→ 产生滞留告警              │
│                                                          │
│                    交车流程                              │
│  ⑥ 配送完成 → 收车验车（可选）                           │
│  ⑦ 交车登记：记录里程、位置、车况                        │
│  ⑧ 解绑：binding.status = HANDED_OVER                   │
│  ⑨ 停止位置核验 ✓                                       │
└──────────────────────────────────────────────────────────┘
```

**关键规则**：交车解绑后，系统立即停止对该记录的位置核验。配送员不再对车辆位置负责。

### 10.3 数据表

#### 10.3.1 人车绑定记录（dms_rider_vehicle_binding）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| tenant_id | BIGINT | 租户ID |
| rider_id | BIGINT | 配送员ID |
| rider_name | VARCHAR(100) | 配送员姓名 |
| rider_phone | VARCHAR(20) | 配送员手机号 |
| vehicle_id | BIGINT | 车辆ID |
| plate_no | VARCHAR(50) | 车牌号 |
| bind_time | TIMESTAMP | 绑定时间（出车） |
| bind_mileage | INTEGER | 绑定时的车辆里程 |
| inspection_id | BIGINT | 关联验车记录ID |
| handover_time | TIMESTAMP | 交车时间 |
| handover_mileage | INTEGER | 交车时的车辆里程 |
| handover_lat/lng | DECIMAL(10,6) | 交车位置坐标 |
| status | INTEGER | 0-绑定中 1-已交车 2-异常解绑 |
| bind_reason | VARCHAR(500) | 绑定原因/任务描述 |
| 审计字段 | ... | 标准审计字段 |

#### 10.3.2 出车验车记录（dms_vehicle_inspection）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| vehicle_id | BIGINT | 车辆ID |
| plate_no | VARCHAR(50) | 车牌号 |
| rider_id | BIGINT | 验车配送员 |
| inspection_type | INTEGER | 1-出车验车 2-收车验车 |
| exterior_status | INTEGER | 外观：0-正常 1-异常 |
| exterior_photos | TEXT | 外观照片URLs |
| tire_status | INTEGER | 轮胎：0-正常 1-异常 |
| light_status | INTEGER | 灯光：0-正常 1-异常 |
| brake_status | INTEGER | 刹车：0-正常 1-异常 |
| cleanliness_status | INTEGER | 清洁：0-正常 1-异常 |
| mileage | INTEGER | 验车时里程 |
| fuel_level | INTEGER | 油量/电量百分比 |
| fire_extinguisher | INTEGER | 灭火器：0-正常 1-缺失/过期 |
| warning_triangle | INTEGER | 警示牌：0-有 1-缺失 |
| result | INTEGER | 0-未检查 1-通过 2-不通过（禁止出车） |
| reviewer | VARCHAR(100) | 审核人（车管员） |
| review_time | TIMESTAMP | 审核时间 |
| binding_id | BIGINT | 关联绑定记录ID |
| 审计字段 | ... | 标准审计字段 |

#### 10.3.3 位置核验记录（dms_position_verification）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| binding_id | BIGINT | 绑定记录ID |
| rider_id | BIGINT | 配送员ID |
| vehicle_id | BIGINT | 车辆ID |
| rider_lat/lng | DECIMAL(10,6) | 配送员上报位置 |
| rider_report_time | TIMESTAMP | 配送员位置上报时间 |
| vehicle_lat/lng | DECIMAL(10,6) | 车辆GPS位置 |
| vehicle_report_time | TIMESTAMP | 车辆位置上报时间 |
| distance_meters | DECIMAL(10,2) | 人车距离（米） |
| threshold_meters | DECIMAL(10,2) | 偏差阈值 |
| is_abnormal | INTEGER | 0-正常 1-超阈值 |
| verify_time | TIMESTAMP | 核验时间 |
| 审计字段 | ... | 标准审计字段 |

#### 10.3.4 核验异常告警（dms_verification_alert）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGSERIAL | 主键 |
| binding_id | BIGINT | 绑定记录ID |
| alert_type | INTEGER | 1-人车分离 2-异常滞留 3-绑定超时 |
| alert_level | INTEGER | 1-提示 2-警告 3-严重 |
| rider_id/name | - | 配送员信息 |
| vehicle_id/plate_no | - | 车辆信息 |
| task_id | BIGINT | 涉及任务ID |
| rider_lat/lng | DECIMAL | 告警时配送员位置 |
| vehicle_lat/lng | DECIMAL | 告警时车辆位置 |
| distance_meters | DECIMAL(10,2) | 人车距离 |
| stay_duration | BIGINT | 滞留时长（秒） |
| alert_content | TEXT | 告警内容 |
| handle_status | INTEGER | 0-未处理 1-已确认 2-已忽略 3-已处理 |
| handler | VARCHAR(100) | 处理人 |
| handle_time | TIMESTAMP | 处理时间 |
| 审计字段 | ... | 标准审计字段 |

### 10.4 核验规则

#### 10.4.1 人车位置分离检测

```
触发条件：配送员GPS与车辆GPS距离 > 阈值（默认100米）
判定逻辑：连续 N 次（默认3次）核验异常 → 产生分离告警
频率：前台配送模式每 15-60 秒核验一次
```

#### 10.4.2 异常滞留检测

```
触发条件：车辆在半径50米内停留超过阈值时间（默认30分钟）
判定逻辑：检查最近N次核验记录的车辆位置变化
告警级别：提示级（不影响配送）
```

#### 10.4.3 绑定超时检测

```
触发条件：单次绑定超过阈值时间（默认12小时）未交车
判定逻辑：扫描所有绑定中记录，对比绑定时间
告警级别：提示级（可能忘记交车）
```

### 10.5 告警处理流

```
告警产生
  │
  ├─ PC管理端告警列表弹窗/标记
  │
  ├─ 车管员查看告警详情
  │    ├─ 位置分离 → 联系配送员确认情况
  │    ├─ 异常滞留 → 查看轨迹确认
  │    └─ 绑定超时 → 提醒配送员交车
  │
  └─ 处理操作
       ├─ 已确认（属实）
       ├─ 已忽略（误报）
       └─ 已处理（已解决）
```

### 10.6 车辆负责人管理

自有车辆 `dms_vehicle` 表通过 `vehicle_manager_id` 字段关联车辆负责人：

- 车辆负责人享有该车辆的**管理权限**：查看车况、维保记录、审批维修
- 车辆负责人不是固定驾驶员，配送员出车仍需执行绑定流程
- 更换负责人走车辆交接流程，更新 `vehicle_manager_id`

### 10.7 与现有系统的映射

| 功能 | 涉及模块 | 说明 |
|------|---------|------|
| 配送员信息 | dms-rider | 配送员基础信息与管理 |
| 车辆档案 | dms-vehicle | 车辆信息 + 车辆负责人 |
| 人车绑定 | dms-verification | 出车/交车记录 |
| 出车验车 | dms-verification | 安全检查记录 |
| 位置核验 | dms-verification + dms-tracking | 人车位置比对 |
| 告警管理 | dms-verification | 异常告警与处理 |
| 位置上报 | dms-tracking | 配送员GPS位置数据 |
| 车辆GPS | dms-tracking（扩展vehicle_position） | 车辆GPS设备数据 |

---

## 十一、费用与结算

### 11.1 配送费计算规则

| 规则 | 公式 | 说明 |
|------|------|------|
| 基础费 | `base_fee` | 固定起步价（可配置） |
| 距离费 | `per_km_rate * distance` | 按公里计费 |
| 重量费 | `per_kg_rate * weight` | 按重量计费（可选） |
| 件数费 | `per_item_rate * items` | 按件数计费（可选） |
| 时段附加 | `time_rate * fee` | 夜间/节假日附加 |
| 加急费 | `urgent_rate * fee` | 紧急订单加价 |

### 11.2 竞价管理

- 起拍价：系统根据费用规则计算
- 竞价时限：可配置（默认 30 分钟）
- 中标规则：最低价 / 综合评分（可配置）
- 综合评分 = 评分 × 权重 + 完成率 × 权重

### 11.3 外部平台费用

- 调用平台预估接口获取费用
- 记录预估与实结差异（`dms_external_order.fee_difference`）
- 差异异常时告警

### 11.4 结算数据

定期汇总以下数据提供给 ERP 财务模块：
- 配送员结算汇总（配送费、加价、扣款）
- 外部平台费用明细
- 收款对账数据

---

## 十二、位置上报与轨迹

### 12.1 上报控制策略

位置上报受 **上班时段** 和 **人车绑定状态** 双重控制，非持续上报。

#### 上报开关逻辑

```
上报状态 = (上班开始 OR 绑定车辆) AND (工作时间内)
上报停止 = 交车完成 OR 下班时间
```

- **开始上报条件**（任一满足即开始）：
  1. 配送员上班打卡（到达上班时段）
  2. 配送员绑定车辆（绑定后即开始上报，绑定优先于上班时段）
- **停止上报条件**（任一满足即停止）：
  1. 配送员交车完成（handover 结束绑定）
  2. 超出上班时段（下班时间到）

> 若配送员在下班前已绑定车辆，下班后即使绑定仍有效，位置上报**立即停止**，直到次日上班时段重新激活。

#### 上班时段配置

| 配置项 | 说明 | 默认值 | 配置位置 |
|--------|------|--------|----------|
| 上班时间 | 配送员默认上班开始时间 | 08:00 | `dms.work-hours.start` |
| 下班时间 | 配送员默认下班结束时间 | 18:00 | `dms.work-hours.end` |
| 上班时段 | 是否启用时段控制 | true | `dms.work-hours.enabled` |

上班时段为租户级可配置（`dms_config`），支持按天/按周设定不同时段。

### 12.2 上报频率

| 模式 | 频率 | 触发条件 |
|------|------|----------|
| 后台保活 | 每 5 分钟 | 已上班且已绑定，但无配送任务 |
| 配送中 | 每 10-30 秒 | 配送员正在执行配送任务（有活跃 Task） |
| 关键节点 | 即时 | 取货、到达、签收时强制上报 |

> 当上报条件不满足时（下班或交车后），客户端停止位置上报，不产生轨迹数据。

### 12.3 隐私合规

- 仅在满足上报条件时采集位置数据
- 配送员可选择暂停上报（休息模式），暂停期间视为非工作时段
- 轨迹数据保留 90 天后自动清理
- 下班后即时停止上报，不追踪非工作轨迹

---

## 十三、前端页面设计

### 13.1 PC 管理端（`apps/pc-admin` 扩展 `views/dms/`）

| 页面 | 路由 | 功能 |
|------|------|------|
| 配送仪表盘 | `/dms/dashboard` | 实时运力分布、待分配/异常订单统计 |
| 配送渠道配置 | `/dms/channel` | 外部平台凭证管理、启停 |
| 运力管理 | `/dms/rider` | 配送员列表、审核、评分、保证金、接单范围 |
| **自有车辆管理** | `/dms/vehicle` | **车辆档案、维保记录、保险年检提醒、绑定驾驶员** |
| **人车绑定与核验** | `/dms/verification` | **出车验车、人车绑定、核验记录、告警处理、交车** |
| **路线规划** | `/dms/route` | **路线优化、地理编码查询、电子围栏配置** |
| 订单调度中心 | `/dms/dispatch` | 地图模式查看订单和骑手，手动指派 |
| 订单大厅监控 | `/dms/order-pool` | 竞价情况查看、强制分配 |
| 配送规则配置 | `/dms/config` | 竞价开关、费用模板、调度优先级、围栏 |
| 轨迹回放 | `/dms/tracking` | 历史轨迹回放 |

### 13.2 移动端（基于 `apps/driver-delivery` 改造增强）

**自有员工与众包骑手功能差异**：

| 功能 | 自有员工 | 众包骑手 |
|------|---------|---------|
| 订单大厅（抢单） | 可选 | 主要来源 |
| 订单大厅（竞价） | 可选 | 可选 |
| 自动分配 | 主要来源 | 可选 |
| 装车确认 | ✅ | —（直接取货） |
| 路线优化 | ✅ | ✅ |
| 签收 | ✅ | ✅ |
| 收款 | ✅ | 可选 |
| 个人收益 | 工资单 | 按单结算 |
| 车辆信息 | 公司车辆 | 自备车辆 |

**页面列表**：

| 页面 | 说明 |
|------|------|
| 任务面板 | 今日汇总、待完成列表 |
| 订单大厅 | 订单列表/地图，接单/出价按钮 |
| 装车确认 | 扫描订单码 |
| 配送路线 | 地图导航、路线调整 |
| 配送详情 | 客户信息、催单高亮、到达确认 |
| 签收页 | 拍照/手写签名、定位比对、更新地址 |
| 收款页 | 二维码生成、标记未付 |
| 异常上报 | 拒收/部分签收/地址错误 |
| 个人中心 | 收益、评价、接单设置 |

---

## 十四、质量自动评估体系

每完成一个页面或功能模块，立即用清单自评（每项 0-10 分，满分 100）：

### 14.1 后端接口质量清单

| # | 检查项 | 分值 |
|---|--------|------|
| 1 | 参数校验完整（JSR-303 + 业务校验） | 10 |
| 2 | 异常处理统一（BusinessException + GlobalExceptionHandler） | 10 |
| 3 | 分页查询规范（Page 对象 + 排序） | 10 |
| 4 | 事务管理正确（@Transactional） | 10 |
| 5 | 并发安全（乐观锁 + 防重抢单） | 10 |
| 6 | 日志完整（操作日志 + trace_id 链路） | 10 |
| 7 | 权限控制（Sa-Token 注解） | 10 |
| 8 | API 文档完整（Knife4j 注解） | 10 |
| 9 | 响应格式统一（ApiResponse / Result） | 10 |
| 10 | 边界条件覆盖（空值/极限值/并发抢单） | 10 |

### 14.2 前端页面质量清单

| # | 检查项 | 分值 |
|---|--------|------|
| 1 | 数据状态全覆盖（加载态/空态/错误态/边界态） | 10 |
| 2 | 操作效率与密度（快捷键、批量、筛选记忆） | 10 |
| 3 | 反馈与防错（抢单防重、竞价确认、异常提示） | 10 |
| 4 | 视觉规范统一（与 ERP 风格协调、组件 small 尺寸、表格四边网格） | 10 |
| 5 | 导航与布局（固定表头/底栏、独立滚动、全屏覆盖） | 10 |
| 6 | 数据时效感（位置/状态实时更新、自动刷新） | 10 |
| 7 | 空状态引导（大厅无订单时说明、首次使用引导） | 10 |
| 8 | 权限预留感（竞价开关控制、按钮禁用态） | 10 |
| 9 | 工程化细节（防抖、loading、位置功耗优化、网络重连） | 10 |
| 10 | 整体专业感（单据集成打印、详情页全屏覆盖） | 10 |

**若总分 < 95，立即找出失分最多两项修复，重新自评，直到 ≥ 95 分。**

---

## 十五、实施路线

| 阶段 | 内容 | 预计产出 |
|------|------|---------|
| 1 | 数据库实现（所有 `dms_*` 表 SQL） | V1.0.0__Create_DMS_Tables.sql |
| 2 | dms-common + dms-bootstrap（基础框架 + 启动入口） | Spring Boot 可启动空服务 |
| 3 | dms-config（租户配置 CRUD + 竞价开关） | 配置管理 API |
| 4 | dms-channel（配送渠道 CRUD + 适配器接口定义） | 渠道管理 + 适配器框架 |
| 5 | dms-rider + dms-vehicle（运力管理 + 车辆管理 + 位置上报） | 配送员/车辆注册审核定位 |
| 6 | dms-task（配送任务核心 + 状态机） | 任务 CRUD + 状态流转 |
| 7 | dms-order-pool（订单大厅 + 抢单 + 竞价） | 抢单/竞价流程 |
| 8 | dms-dispatch（智能调度 + 围栏 + 分配引擎） | 自动/手动分配 |
| 9 | dms-route（路线规划 + 地图服务 SPI + 高德/腾讯/百度适配） | 路线规划 API |
| 10 | **dms-verification（人车绑定 + 出车验车 + 位置核验 + 异常告警 + 交车）** | 绑定核验 API |
| 11 | dms-execution（装车确认 + 路线） | 执行流程 |
| 12 | dms-sign + dms-payment（签收 + 收款） | 签收/收款 API |
| 13 | dms-tracking（位置追踪） | 轨迹 API |
| 14 | dms-settlement（费用结算） | 结算 API |
| 15 | dms-event（ERP/WMS 集成 + 事件引擎） | 集成接口 |
| 16 | PC 管理端页面（11 个页面含车辆管理 + 绑定核验） | 前端页面 |
| 17 | 移动端改造（driver-delivery 增强） | 移动端页面 |
| 18 | 端到端联调 | 全流程验证 |

---

## 十六、与现有代码的映射关系

### 16.1 复用现有组件

| 组件 | 位置 | 复用方式 |
|------|------|---------|
| `ApiResponse<T>` | `core/common/result/ApiResponse.java` | 直接依赖 |
| `Result<T>` sealed interface | `core/base/vo/Result.java` | 直接依赖 |
| `GlobalExceptionHandler` | `core/base/config/GlobalExceptionHandler.java` | 复制并调整 |
| Sa-Token 配置 | `core/base/config/SaTokenConfig.java` | 依赖 |
| `RestTemplateConfig` | `core/common/config/RestTemplateConfig.java` | 依赖 |
| MyBatis-Plus 配置 | `core/base/config/MyBatisPlusConfig.java` | 依赖 |
| `PageContainer` 组件 | frontend `packages/components` | 复用 |
| Ant Design Vue 组件库 | frontend 全域 | 复用 |
| Vant 组件库 | mobile `packages` | 复用 |

### 16.2 数据库规范遵循

- 审计字段：`tenant_id BIGINT`, `deleted INTEGER DEFAULT 0`, `create_time TIMESTAMP`, `update_time TIMESTAMP`, `create_by BIGINT`, `update_by BIGINT`, `version INTEGER DEFAULT 1`
- 金额类型：`decimal(18,2)`
- 数量类型：`decimal(18,4)`
- 表前缀：`dms_`

---

## 十七、安全与合规

### 17.1 接口安全

- 内部接口：Sa-Token 认证（`@SaCheckLogin` / `@SaCheckPermission`）
- 外部回调：HMAC-SHA256 签名校验 + IP 白名单
- 事件发件箱：保证最终一致性，失败重试，超过 3 次人工介入

### 17.2 数据合规

- 位置上报受**上班时段**和**人车绑定状态**双重控制，详见第十二章
- 下班或交车后即时停止上报，不追踪非工作轨迹
- 配送员可选择暂停上报（休息模式），暂停期间视为非工作时段
- 轨迹数据保留 90 天后自动清理

### 17.3 并发安全

- 抢单操作：数据库行锁 + 乐观锁
- 库存操作：`@Version` 乐观锁
- 事件幂等：`traceId` 去重

---

## 十八、待完善项

| 项 | 说明 | 优先级 |
|----|------|--------|
| 实际外部平台对接 | 当前使用模拟/Stub 适配器，需各自平台商户密钥 | 高 |
| 地图服务 | 路线规划和电子围栏依赖高德/百度地图 API | 高 |
| 推送服务 | 配送员 App 需 PUSH 通知（接单、催单） | 中 |
| 短信通知 | 客户催单/状态变更短信通知 | 中 |
| 完整竞价 UI | 竞价倒计时、出价引导 | 低 |
| 数据分析报表 | 配送效率、运力利用率、成本分析 | 低 |
| 国际化 | 多语言支持 | 低 |
