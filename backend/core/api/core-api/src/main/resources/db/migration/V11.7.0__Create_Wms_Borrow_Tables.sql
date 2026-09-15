-- ==============================================================
-- WMS 借进借出管理 - 建表脚本
-- Flyway Migration: V11.7.0__Create_Wms_Borrow_Tables.sql
--
-- 表清单:
--   wms_borrow_order       借进借出单（头表）
--   wms_borrow_order_item  借进借出单明细
--   wms_borrow_return      归还记录（头表）
--   wms_borrow_return_item 归还记录明细
-- ==============================================================

-- ==================== 1. 借进借出单（头表） ====================

CREATE TABLE IF NOT EXISTS wms_borrow_order (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    order_no VARCHAR(100) NOT NULL,                  -- 单号 JJ/JC+yyyyMMdd+3位流水
    direction INTEGER NOT NULL,                      -- 方向 1-借进 2-借出
    partner_id BIGINT,                               -- 往来单位ID
    partner_name VARCHAR(200),                       -- 往来单位名称
    warehouse_id BIGINT,                             -- 仓库ID
    warehouse_name VARCHAR(200),                     -- 仓库名称
    borrow_date DATE,                                -- 借出/借进日期
    expected_return_date DATE,                       -- 预计归还日期
    status INTEGER DEFAULT 0,                        -- 0-草稿 1-待审批 2-已审批 3-部分归还 4-已归还 5-已取消
    total_quantity DECIMAL(18,4) DEFAULT 0,          -- 总数量
    returned_quantity DECIMAL(18,4) DEFAULT 0,       -- 已归还数量
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version INTEGER DEFAULT 1
);
COMMENT ON TABLE wms_borrow_order IS '借进借出单';
COMMENT ON COLUMN wms_borrow_order.direction IS '1-借进 2-借出';
COMMENT ON COLUMN wms_borrow_order.status IS '0-草稿 1-待审批 2-已审批 3-部分归还 4-已归还 5-已取消';

CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_borrow_order_no ON wms_borrow_order(order_no);
CREATE INDEX IF NOT EXISTS idx_wms_borrow_order_partner ON wms_borrow_order(partner_id);
CREATE INDEX IF NOT EXISTS idx_wms_borrow_order_warehouse ON wms_borrow_order(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_wms_borrow_order_status ON wms_borrow_order(status);

-- ==================== 2. 借进借出单明细 ====================

CREATE TABLE IF NOT EXISTS wms_borrow_order_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    order_id BIGINT NOT NULL,                        -- 关联 wms_borrow_order.id
    line_no INTEGER DEFAULT 0,                       -- 行号
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    product_spec VARCHAR(200),
    unit VARCHAR(50),                                -- 单位
    quantity DECIMAL(18,4) DEFAULT 0,                -- 借进/借出数量
    returned_quantity DECIMAL(18,4) DEFAULT 0,       -- 已归还数量
    price DECIMAL(18,4) DEFAULT 0,                   -- 单价
    amount DECIMAL(18,2) DEFAULT 0,                  -- 金额
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_borrow_order_item IS '借进借出单明细';

CREATE INDEX IF NOT EXISTS idx_wms_borrow_item_order ON wms_borrow_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_wms_borrow_item_product ON wms_borrow_order_item(product_id);

-- ==================== 3. 归还记录（头表） ====================

CREATE TABLE IF NOT EXISTS wms_borrow_return (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    order_id BIGINT NOT NULL,                        -- 关联 wms_borrow_order.id
    return_date DATE,                                -- 归还日期
    operator_id BIGINT,                              -- 操作人ID
    operator_name VARCHAR(100),                      -- 操作人姓名
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_borrow_return IS '借进借出归还记录';

CREATE INDEX IF NOT EXISTS idx_wms_borrow_return_order ON wms_borrow_return(order_id);

-- ==================== 4. 归还记录明细 ====================

CREATE TABLE IF NOT EXISTS wms_borrow_return_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    return_id BIGINT NOT NULL,                       -- 关联 wms_borrow_return.id
    order_item_id BIGINT,                            -- 关联 wms_borrow_order_item.id
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    quantity DECIMAL(18,4) DEFAULT 0,                -- 本次归还数量
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);
COMMENT ON TABLE wms_borrow_return_item IS '借进借出归还记录明细';

CREATE INDEX IF NOT EXISTS idx_wms_borrow_return_item_return ON wms_borrow_return_item(return_id);
