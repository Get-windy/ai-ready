-- ==============================================================
-- WMS 仓储管理模块 - 数据库初始化脚本
-- Flyway Migration: V6.0.0__Create_WMS_Tables.sql
--
-- 遵循现有命名规范:
--   - BIGSERIAL 主键
--   - 所有金额 decimal(18,2)
--   - 所有数量 decimal(18,4)
--   - 状态用 INTEGER
--   - 包含 tenant_id, deleted, create_time, update_time 审计字段
--   - 逻辑删除 deleted INTEGER DEFAULT 0
-- ==============================================================

-- ==================== 1. 仓库扩展表 ====================

-- 仓库扩展信息（在 erp_warehouse 基础上扩展 WMS 管理字段）
CREATE TABLE IF NOT EXISTS wms_warehouse (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    warehouse_id BIGINT NOT NULL,                   -- 关联 erp_warehouse.id
    warehouse_code VARCHAR(50),
    warehouse_name VARCHAR(200),
    warehouse_type INTEGER DEFAULT 1,                -- 1-普通 2-冷库 3-危险品 4-保税
    zone_count INTEGER DEFAULT 0,                   -- 库区数量
    location_count INTEGER DEFAULT 0,               -- 货位数量
    total_capacity DECIMAL(18,4) DEFAULT 0,         -- 总容量(m³)
    used_capacity DECIMAL(18,4) DEFAULT 0,           -- 已用容量
    is_wms_enabled INTEGER DEFAULT 0,               -- 是否启用WMS管理 0-否 1-是
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_warehouse IS '仓库扩展信息（WMS）';
COMMENT ON COLUMN wms_warehouse.warehouse_type IS '1-普通 2-冷库 3-危险品 4-保税';
COMMENT ON COLUMN wms_warehouse.is_wms_enabled IS '0-否 1-是';

-- ==================== 2. 库区/货位表 ====================

CREATE TABLE IF NOT EXISTS wms_location (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    warehouse_id BIGINT NOT NULL,                    -- 关联 erp_warehouse.id
    location_code VARCHAR(100) NOT NULL,             -- 货位编码（层级，如 A-01-01）
    location_name VARCHAR(200),
    location_type INTEGER DEFAULT 1,                 -- 1-存储位 2-暂存位 3-拣货位 4-收货区 5-发货区 6-报废区
    location_level INTEGER DEFAULT 1,                -- 层级（1-库区 2-巷道 3-货架 4-货位）
    parent_id BIGINT,                                -- 父级ID
    path VARCHAR(500),                               -- 层级路径（如 /A/A-01/A-01-01）
    max_capacity DECIMAL(18,4) DEFAULT 0,            -- 最大容量
    used_capacity DECIMAL(18,4) DEFAULT 0,           -- 已用容量
    max_weight DECIMAL(18,2) DEFAULT 0,              -- 最大载重(kg)
    length_cm DECIMAL(10,2) DEFAULT 0,               -- 长(cm)
    width_cm DECIMAL(10,2) DEFAULT 0,                -- 宽(cm)
    height_cm DECIMAL(10,2) DEFAULT 0,               -- 高(cm)
    status INTEGER DEFAULT 1,                        -- 1-空闲 2-占用 3-冻结 4-维修
    is_pickable INTEGER DEFAULT 1,                   -- 是否可拣货 0-否 1-是
    is_receivable INTEGER DEFAULT 1,                 -- 是否可收货 0-否 1-是
    sort_order INTEGER DEFAULT 0,                    -- 排序
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_location IS '库区/货位';
COMMENT ON COLUMN wms_location.location_type IS '1-存储位 2-暂存位 3-拣货位 4-收货区 5-发货区 6-报废区';
COMMENT ON COLUMN wms_location.location_level IS '1-库区 2-巷道 3-货架 4-货位';
COMMENT ON COLUMN wms_location.status IS '1-空闲 2-占用 3-冻结 4-维修';

CREATE INDEX idx_wms_location_warehouse ON wms_location(warehouse_id);
CREATE INDEX idx_wms_location_code ON wms_location(location_code);
CREATE INDEX idx_wms_location_parent ON wms_location(parent_id);

-- ==================== 3. 收货任务表 ====================

CREATE TABLE IF NOT EXISTS wms_receipt_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_no VARCHAR(100) NOT NULL,                   -- 任务单号（RC+年月日+流水号）
    source_type INTEGER DEFAULT 1,                   -- 来源类型 1-采购入库 2-退货入库 3-调拨入库 4-盘盈入库
    source_order_id BIGINT,                          -- 来源单据ID（如 erp_purchase_inbound.id）
    source_order_no VARCHAR(100),                    -- 来源单据号
    warehouse_id BIGINT,                             -- 仓库
    warehouse_name VARCHAR(200),
    supplier_id BIGINT,                              -- 供应商ID
    supplier_name VARCHAR(200),
    total_items INTEGER DEFAULT 0,                   -- 总商品数
    total_quantity DECIMAL(18,4) DEFAULT 0,          -- 总应收数量
    received_quantity DECIMAL(18,4) DEFAULT 0,       -- 已收数量
    status INTEGER DEFAULT 0,                        -- 0-待收货 1-收货中 2-已完成 3-已取消 4-异常
    priority INTEGER DEFAULT 1,                      -- 1-普通 2-紧急 3-加急
    expected_time TIMESTAMP,                         -- 预期到货时间
    completed_time TIMESTAMP,
    assignee_id BIGINT,                              -- 分配操作人
    assignee_name VARCHAR(100),
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_receipt_task IS '收货任务';
COMMENT ON COLUMN wms_receipt_task.source_type IS '1-采购入库 2-退货入库 3-调拨入库 4-盘盈入库';
COMMENT ON COLUMN wms_receipt_task.status IS '0-待收货 1-收货中 2-已完成 3-已取消 4-异常';
COMMENT ON COLUMN wms_receipt_task.priority IS '1-普通 2-紧急 3-加急';

CREATE INDEX idx_wms_receipt_task_no ON wms_receipt_task(task_no);
CREATE INDEX idx_wms_receipt_task_status ON wms_receipt_task(status);
CREATE INDEX idx_wms_receipt_task_source ON wms_receipt_task(source_order_id);

-- ==================== 4. 收货明细细表 ====================

CREATE TABLE IF NOT EXISTS wms_receipt_detail (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_id BIGINT NOT NULL,                         -- 关联 wms_receipt_task.id
    line_no INTEGER DEFAULT 0,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    expected_quantity DECIMAL(18,4) DEFAULT 0,       -- 应收数量
    received_quantity DECIMAL(18,4) DEFAULT 0,       -- 实收数量
    putaway_quantity DECIMAL(18,4) DEFAULT 0,        -- 已上架数量
    location_id BIGINT,                               -- 上架货位
    location_code VARCHAR(100),
    batch_no VARCHAR(100),                           -- 批次号
    production_date TIMESTAMP,                       -- 生产日期
    validity_date TIMESTAMP,                         -- 有效期至
    status INTEGER DEFAULT 0,                        -- 0-待收货 1-已收货 2-已上架
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_receipt_detail IS '收货明细';
COMMENT ON COLUMN wms_receipt_detail.status IS '0-待收货 1-已收货 2-已上架';

CREATE INDEX idx_wms_receipt_detail_task ON wms_receipt_detail(task_id);

-- ==================== 5. 上架任务表 ====================

CREATE TABLE IF NOT EXISTS wms_putaway_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_no VARCHAR(100) NOT NULL,                   -- 任务单号（PA+年月日+流水号）
    source_type INTEGER DEFAULT 1,                   -- 来源 1-收货上架 2-移库上架
    source_id BIGINT,                                -- 来源单据ID
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    total_items INTEGER DEFAULT 0,
    total_quantity DECIMAL(18,4) DEFAULT 0,
    putaway_quantity DECIMAL(18,4) DEFAULT 0,
    status INTEGER DEFAULT 0,                        -- 0-待上架 1-上架中 2-已完成 3-已取消
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_putaway_task IS '上架任务';
COMMENT ON COLUMN wms_putaway_task.source_type IS '1-收货上架 2-移库上架';
COMMENT ON COLUMN wms_putaway_task.status IS '0-待上架 1-上架中 2-已完成 3-已取消';

CREATE INDEX idx_wms_putaway_task_no ON wms_putaway_task(task_no);
CREATE INDEX idx_wms_putaway_task_status ON wms_putaway_task(status);

-- ==================== 6. 上架明细细表 ====================

CREATE TABLE IF NOT EXISTS wms_putaway_detail (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 0,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    quantity DECIMAL(18,4) DEFAULT 0,                -- 上架数量
    from_location_id BIGINT,                         -- 源货位（暂存位）
    from_location_code VARCHAR(100),
    to_location_id BIGINT NOT NULL,                  -- 目标货位
    to_location_code VARCHAR(100) NOT NULL,
    batch_no VARCHAR(100),
    production_date TIMESTAMP,
    validity_date TIMESTAMP,
    status INTEGER DEFAULT 0,                        -- 0-待上架 1-已上架
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_putaway_detail IS '上架明细';
COMMENT ON COLUMN wms_putaway_detail.status IS '0-待上架 1-已上架';

CREATE INDEX idx_wms_putaway_detail_task ON wms_putaway_detail(task_id);

-- ==================== 7. 拣货波次表 ====================

CREATE TABLE IF NOT EXISTS wms_pick_wave (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    wave_no VARCHAR(100) NOT NULL,                   -- 波次号（WV+年月日+流水号）
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    order_count INTEGER DEFAULT 0,                   -- 订单数
    item_count INTEGER DEFAULT 0,                    -- 商品项数
    total_quantity DECIMAL(18,4) DEFAULT 0,          -- 总拣货数量
    picked_quantity DECIMAL(18,4) DEFAULT 0,
    status INTEGER DEFAULT 0,                        -- 0-待分配 1-拣货中 2-已完成 3-已取消
    priority INTEGER DEFAULT 1,                      -- 1-普通 2-紧急 3-加急
    wave_type INTEGER DEFAULT 1,                     -- 1-按单波次 2-按商品聚合 3-按路线
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_pick_wave IS '拣货波次';
COMMENT ON COLUMN wms_pick_wave.status IS '0-待分配 1-拣货中 2-已完成 3-已取消';
COMMENT ON COLUMN wms_pick_wave.priority IS '1-普通 2-紧急 3-加急';
COMMENT ON COLUMN wms_pick_wave.wave_type IS '1-按单波次 2-按商品聚合 3-按路线';

CREATE INDEX idx_wms_pick_wave_no ON wms_pick_wave(wave_no);
CREATE INDEX idx_wms_pick_wave_status ON wms_pick_wave(status);

-- ==================== 8. 拣货任务表 ====================

CREATE TABLE IF NOT EXISTS wms_pick_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_no VARCHAR(100) NOT NULL,                   -- 任务单号（PK+年月日+流水号）
    wave_id BIGINT,                                  -- 关联 wms_pick_wave.id
    source_type INTEGER DEFAULT 1,                   -- 来源 1-销售出库 2-退货出库 3-调拨出库 4-盘亏出库
    source_order_id BIGINT,                          -- 来源单据ID
    source_order_no VARCHAR(100),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    customer_id BIGINT,
    customer_name VARCHAR(200),
    total_items INTEGER DEFAULT 0,
    total_quantity DECIMAL(18,4) DEFAULT 0,
    picked_quantity DECIMAL(18,4) DEFAULT 0,
    status INTEGER DEFAULT 0,                        -- 0-待拣货 1-拣货中 2-已完成 3-缺货 4-已取消
    priority INTEGER DEFAULT 1,
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    expect_ship_time TIMESTAMP,                      -- 期望发货时间
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_pick_task IS '拣货任务';
COMMENT ON COLUMN wms_pick_task.source_type IS '1-销售出库 2-退货出库 3-调拨出库 4-盘亏出库';
COMMENT ON COLUMN wms_pick_task.status IS '0-待拣货 1-拣货中 2-已完成 3-缺货 4-已取消';

CREATE INDEX idx_wms_pick_task_no ON wms_pick_task(task_no);
CREATE INDEX idx_wms_pick_task_wave ON wms_pick_task(wave_id);
CREATE INDEX idx_wms_pick_task_status ON wms_pick_task(status);

-- ==================== 9. 拣货明细细表 ====================

CREATE TABLE IF NOT EXISTS wms_pick_detail (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 0,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    location_id BIGINT,                              -- 拣货货位
    location_code VARCHAR(100),
    expected_quantity DECIMAL(18,4) DEFAULT 0,       -- 应拣数量
    picked_quantity DECIMAL(18,4) DEFAULT 0,         -- 实拣数量
    shortage_quantity DECIMAL(18,4) DEFAULT 0,       -- 缺货数量
    batch_no VARCHAR(100),
    serial_no VARCHAR(100),
    status INTEGER DEFAULT 0,                        -- 0-待拣货 1-已拣货 2-缺货
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_pick_detail IS '拣货明细';
COMMENT ON COLUMN wms_pick_detail.status IS '0-待拣货 1-已拣货 2-缺货';

CREATE INDEX idx_wms_pick_detail_task ON wms_pick_detail(task_id);
CREATE INDEX idx_wms_pick_detail_location ON wms_pick_detail(location_id);

-- ==================== 10. 发货复核任务表 ====================

CREATE TABLE IF NOT EXISTS wms_ship_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_no VARCHAR(100) NOT NULL,                   -- 任务单号（SH+年月日+流水号）
    pick_task_id BIGINT,                             -- 关联拣货任务
    source_order_id BIGINT,                          -- 关联出库单
    source_order_no VARCHAR(100),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    customer_id BIGINT,
    customer_name VARCHAR(200),
    total_items INTEGER DEFAULT 0,
    total_quantity DECIMAL(18,4) DEFAULT 0,
    scanned_quantity DECIMAL(18,4) DEFAULT 0,        -- 已扫描数量
    status INTEGER DEFAULT 0,                        -- 0-待复核 1-复核中 2-已发货 3-已取消
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    carrier_name VARCHAR(200),                       -- 承运商
    tracking_no VARCHAR(100),                        -- 运单号
    ship_time TIMESTAMP,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_ship_task IS '发货复核任务';
COMMENT ON COLUMN wms_ship_task.status IS '0-待复核 1-复核中 2-已发货 3-已取消';

CREATE INDEX idx_wms_ship_task_no ON wms_ship_task(task_no);
CREATE INDEX idx_wms_ship_task_status ON wms_ship_task(status);

-- ==================== 11. 发货复核明细表 ====================

CREATE TABLE IF NOT EXISTS wms_ship_detail (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    ship_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 0,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    expected_quantity DECIMAL(18,4) DEFAULT 0,       -- 应发数量
    scanned_quantity DECIMAL(18,4) DEFAULT 0,         -- 扫描数量
    confirmed_quantity DECIMAL(18,4) DEFAULT 0,       -- 确认数量
    batch_no VARCHAR(100),
    serial_no VARCHAR(100),
    location_id BIGINT,
    location_code VARCHAR(100),
    status INTEGER DEFAULT 0,                        -- 0-待扫描 1-已扫描 2-已确认
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_ship_detail IS '发货复核明细';
COMMENT ON COLUMN wms_ship_detail.status IS '0-待扫描 1-已扫描 2-已确认';

CREATE INDEX idx_wms_ship_detail_ship ON wms_ship_detail(ship_id);

-- ==================== 12. 实时库存表（WMS 明细维度） ====================

CREATE TABLE IF NOT EXISTS wms_inventory (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    warehouse_id BIGINT NOT NULL,
    warehouse_name VARCHAR(200),
    location_id BIGINT,                              -- 货位ID
    location_code VARCHAR(100),
    batch_no VARCHAR(100),                           -- 批次号
    serial_no VARCHAR(100),                          -- 序列号
    quantity DECIMAL(18,4) DEFAULT 0,                -- 总数量
    available_quantity DECIMAL(18,4) DEFAULT 0,      -- 可用量（quantity - frozen_quantity）
    frozen_quantity DECIMAL(18,4) DEFAULT 0,         -- 冻结量
    unit_cost DECIMAL(18,6) DEFAULT 0,               -- 单位成本
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    production_date TIMESTAMP,
    validity_date TIMESTAMP,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_inventory IS '实时库存（WMS 明细维度：商品+仓库+货位+批次）';

CREATE UNIQUE INDEX idx_wms_inventory_uk ON wms_inventory(product_id, warehouse_id, COALESCE(location_id, 0), COALESCE(batch_no, ''));
CREATE INDEX idx_wms_inventory_product ON wms_inventory(product_id);
CREATE INDEX idx_wms_inventory_warehouse ON wms_inventory(warehouse_id);
CREATE INDEX idx_wms_inventory_location ON wms_inventory(location_id);
CREATE INDEX idx_wms_inventory_batch ON wms_inventory(batch_no);

-- ==================== 13. 库存异动日志表 ====================

CREATE TABLE IF NOT EXISTS wms_inventory_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    trace_id VARCHAR(64),                            -- 链路追踪ID
    product_id BIGINT NOT NULL,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    location_id BIGINT,
    location_code VARCHAR(100),
    batch_no VARCHAR(100),
    serial_no VARCHAR(100),
    change_type INTEGER NOT NULL,                    -- 变动类型 1-入库 2-出库 3-冻结 4-解冻 5-盘盈 6-盘亏 7-移库 8-调整
    direction INTEGER NOT NULL,                      -- 方向 1-入库(增加) -1-出库(减少)
    quantity DECIMAL(18,4) NOT NULL,                 -- 变动数量
    before_quantity DECIMAL(18,4) DEFAULT 0,         -- 变动前数量
    after_quantity DECIMAL(18,4) DEFAULT 0,          -- 变动后数量
    source_type VARCHAR(50),                         -- 来源单据类型（receipt/pick/ship/check/move/adjust）
    source_id BIGINT,                                -- 来源单据ID
    source_no VARCHAR(100),                          -- 来源单据号
    operator_id BIGINT,                              -- 操作人
    operator_name VARCHAR(100),
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE wms_inventory_log IS '库存异动日志';

CREATE INDEX idx_wms_inventory_log_product ON wms_inventory_log(product_id);
CREATE INDEX idx_wms_inventory_log_warehouse ON wms_inventory_log(warehouse_id);
CREATE INDEX idx_wms_inventory_log_time ON wms_inventory_log(create_time);
CREATE INDEX idx_wms_inventory_log_trace ON wms_inventory_log(trace_id);
CREATE INDEX idx_wms_inventory_log_source ON wms_inventory_log(source_type, source_id);

-- ==================== 14. 移库任务表 ====================

CREATE TABLE IF NOT EXISTS wms_move_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_no VARCHAR(100) NOT NULL,                   -- 任务单号（MV+年月日+流水号）
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    from_location_id BIGINT,                         -- 源货位
    from_location_code VARCHAR(100),
    to_location_id BIGINT,                           -- 目标货位
    to_location_code VARCHAR(100),
    total_items INTEGER DEFAULT 0,
    total_quantity DECIMAL(18,4) DEFAULT 0,
    moved_quantity DECIMAL(18,4) DEFAULT 0,
    status INTEGER DEFAULT 0,                        -- 0-待移库 1-移库中 2-已完成 3-已取消
    move_type INTEGER DEFAULT 1,                     -- 1-库内移库 2-补货移库 3-整理移库
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_move_task IS '移库任务';
COMMENT ON COLUMN wms_move_task.status IS '0-待移库 1-移库中 2-已完成 3-已取消';
COMMENT ON COLUMN wms_move_task.move_type IS '1-库内移库 2-补货移库 3-整理移库';

CREATE INDEX idx_wms_move_task_no ON wms_move_task(task_no);
CREATE INDEX idx_wms_move_task_status ON wms_move_task(status);

-- ==================== 15. 移库明细表 ====================

CREATE TABLE IF NOT EXISTS wms_move_detail (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 0,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    quantity DECIMAL(18,4) DEFAULT 0,               -- 移库数量
    batch_no VARCHAR(100),
    serial_no VARCHAR(100),
    from_location_id BIGINT,
    from_location_code VARCHAR(100),
    to_location_id BIGINT,
    to_location_code VARCHAR(100),
    status INTEGER DEFAULT 0,                        -- 0-待移库 1-已移库
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_move_detail IS '移库明细';
COMMENT ON COLUMN wms_move_detail.status IS '0-待移库 1-已移库';

CREATE INDEX idx_wms_move_detail_task ON wms_move_detail(task_id);

-- ==================== 16. 盘点任务表 ====================

CREATE TABLE IF NOT EXISTS wms_check_task (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_no VARCHAR(100) NOT NULL,                   -- 任务单号（CK+年月日+流水号）
    warehouse_id BIGINT,
    warehouse_name VARCHAR(200),
    check_type INTEGER DEFAULT 1,                    -- 1-明盘（显示账面数） 2-盲盘（不显示账面数）
    scope_type INTEGER DEFAULT 1,                    -- 盘点范围 1-全库盘点 2-指定货位 3-指定商品 4-指定批次
    total_items INTEGER DEFAULT 0,
    checked_items INTEGER DEFAULT 0,
    diff_items INTEGER DEFAULT 0,
    status INTEGER DEFAULT 0,                        -- 0-待盘点 1-盘点中 2-已完成 3-已审核 4-已取消
    lock_location INTEGER DEFAULT 1,                 -- 盘点期间是否锁定库位 0-否 1-是
    assignee_id BIGINT,
    assignee_name VARCHAR(100),
    checker_id BIGINT,                               -- 复盘人
    checker_name VARCHAR(100),
    approved_by BIGINT,
    approved_time TIMESTAMP,
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_check_task IS '盘点任务';
COMMENT ON COLUMN wms_check_task.check_type IS '1-明盘 2-盲盘';
COMMENT ON COLUMN wms_check_task.scope_type IS '1-全库盘点 2-指定货位 3-指定商品 4-指定批次';
COMMENT ON COLUMN wms_check_task.status IS '0-待盘点 1-盘点中 2-已完成 3-已审核 4-已取消';

CREATE INDEX idx_wms_check_task_no ON wms_check_task(task_no);
CREATE INDEX idx_wms_check_task_status ON wms_check_task(status);

-- ==================== 17. 盘点结果表 ====================

CREATE TABLE IF NOT EXISTS wms_check_result (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    task_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 0,
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    product_unit VARCHAR(50),
    location_id BIGINT,
    location_code VARCHAR(100),
    batch_no VARCHAR(100),
    book_quantity DECIMAL(18,4) DEFAULT 0,           -- 账面数量
    actual_quantity DECIMAL(18,4) DEFAULT 0,         -- 实盘数量
    diff_quantity DECIMAL(18,4) DEFAULT 0,           -- 差异数量
    diff_type INTEGER DEFAULT 0,                     -- 差异类型 0-正常 1-盘盈 2-盘亏
    unit_cost DECIMAL(18,6) DEFAULT 0,
    diff_amount DECIMAL(18,2) DEFAULT 0,            -- 差异金额
    check_status INTEGER DEFAULT 0,                  -- 0-待盘点 1-已盘点 2-已确认 3-已调整
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_check_result IS '盘点结果';
COMMENT ON COLUMN wms_check_result.diff_type IS '0-正常 1-盘盈 2-盘亏';
COMMENT ON COLUMN wms_check_result.check_status IS '0-待盘点 1-已盘点 2-已确认 3-已调整';

CREATE INDEX idx_wms_check_result_task ON wms_check_result(task_id);
CREATE INDEX idx_wms_check_result_location ON wms_check_result(location_id);

-- ==================== 18. 事件发件箱表 ====================

CREATE TABLE IF NOT EXISTS wms_event_outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    trace_id VARCHAR(64) NOT NULL,                   -- 追踪ID
    event_type VARCHAR(100) NOT NULL,                 -- 事件类型
    source_system VARCHAR(50) DEFAULT 'WMS',          -- 来源系统
    target_system VARCHAR(50) DEFAULT 'ERP',          -- 目标系统
    payload TEXT,                                     -- 事件内容（JSON）
    status INTEGER DEFAULT 0,                         -- 0-待发送 1-发送成功 2-发送失败
    retry_count INTEGER DEFAULT 0,                    -- 已重试次数
    max_retry INTEGER DEFAULT 3,
    last_error TEXT,                                  -- 最后一次错误
    next_retry_time TIMESTAMP,                        -- 下次重试时间
    completed_time TIMESTAMP,                         -- 完成时间
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE wms_event_outbox IS '事件发件箱（WMS→ERP 异步通知）';
COMMENT ON COLUMN wms_event_outbox.status IS '0-待发送 1-发送成功 2-发送失败';

CREATE INDEX idx_wms_event_outbox_status ON wms_event_outbox(status, next_retry_time);
CREATE INDEX idx_wms_event_outbox_trace ON wms_event_outbox(trace_id);
