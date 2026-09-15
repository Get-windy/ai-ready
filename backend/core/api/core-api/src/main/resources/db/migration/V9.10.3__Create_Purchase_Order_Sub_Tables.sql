-- V9.10.1 创建采购订单子表（对标 SaleOrder 7 子表模式）
-- 主表 PurchaseOrder 从 89 字段瘦身至 ≤25 字段

-- ============================================================
-- 1. 供应商快照 (1:1)
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_partner_snapshot (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    supplier_name       VARCHAR(200),
    supplier_code       VARCHAR(50),
    contact_name        VARCHAR(100),
    contact_phone       VARCHAR(50),
    contact_address     VARCHAR(500),
    bank_name           VARCHAR(200),
    bank_account        VARCHAR(100),
    tax_no              VARCHAR(100),
    supplier_remark     VARCHAR(500),
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_po_partner_snapshot_order ON erp_purchase_order_partner_snapshot(order_id);
COMMENT ON TABLE erp_purchase_order_partner_snapshot IS '采购订单供应商快照(1:1)';

-- ============================================================
-- 2. 结算信息 (1:1)
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_settlement (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    payment_method_id   BIGINT,
    payment_method_name VARCHAR(50),
    settle_method_id    BIGINT,
    settle_date         TIMESTAMP,
    cash_discount       VARCHAR(200),
    require_provide     VARCHAR(200),
    deposit_account_1   VARCHAR(100),
    deposit_amount_1    DECIMAL(18,2)   DEFAULT 0,
    more_accounts       VARCHAR(500),
    prev_prepaid        DECIMAL(18,2)   DEFAULT 0,
    prepaid_balance     DECIMAL(18,2)   DEFAULT 0,
    prev_debt           DECIMAL(18,2)   DEFAULT 0,
    payment_term        VARCHAR(200),
    other_expense       DECIMAL(18,2)   DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_po_settlement_order ON erp_purchase_order_settlement(order_id);
COMMENT ON TABLE erp_purchase_order_settlement IS '采购订单结算信息(1:1)';

-- ============================================================
-- 3. 物流信息 (1:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_logistics (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    logistics_company   VARCHAR(200),
    tracking_number     VARCHAR(100),
    freight             DECIMAL(18,2)   DEFAULT 0,
    estimated_arrival   TIMESTAMP,
    shipped             BOOLEAN         DEFAULT FALSE,
    ship_time           TIMESTAMP,
    received            BOOLEAN         DEFAULT FALSE,
    receive_time        TIMESTAMP,
    received_quantity   DECIMAL(18,2)   DEFAULT 0,
    quality_check_result VARCHAR(500),
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_po_logistics_order ON erp_purchase_order_logistics(order_id);
COMMENT ON TABLE erp_purchase_order_logistics IS '采购订单物流信息(1:N)';

-- ============================================================
-- 4. 订金账户 (1:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_deposit (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    account_name        VARCHAR(100),
    account_no          VARCHAR(100),
    amount              DECIMAL(18,2)   DEFAULT 0,
    used_amount         DECIMAL(18,2)   DEFAULT 0,
    status              INTEGER         DEFAULT 0,
    sequence_no         INTEGER         DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_po_deposit_order ON erp_purchase_order_deposit(order_id);
COMMENT ON TABLE erp_purchase_order_deposit IS '采购订单订金账户(1:N)';

-- ============================================================
-- 5. 审核流水 (1:N)
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_audit_trail (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    action              VARCHAR(50),
    operator_id         BIGINT,
    operator_name       VARCHAR(100),
    level_no            INTEGER,
    comment             VARCHAR(500),
    operate_time        TIMESTAMP,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_po_audit_trail_order ON erp_purchase_order_audit_trail(order_id);
COMMENT ON TABLE erp_purchase_order_audit_trail IS '采购订单审核流水(1:N)';

-- ============================================================
-- 6. 扩展信息 (1:1)
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_ext_info (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    summary             VARCHAR(500),
    attachment          VARCHAR(1000),
    ext_num_1           DECIMAL(18,2),
    ext_num_2           DECIMAL(18,2),
    ext_text_1          VARCHAR(255),
    ext_text_2          VARCHAR(255),
    ext_text_3          VARCHAR(255),
    print_count         INTEGER         DEFAULT 0,
    ext_json            TEXT,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_po_ext_info_order ON erp_purchase_order_ext_info(order_id);
COMMENT ON TABLE erp_purchase_order_ext_info IS '采购订单扩展信息(1:1)';

-- ============================================================
-- 主表瘦身：添加新字段（为 PurchaseOrder 实体缩减做准备）
-- ============================================================
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS product_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS bill_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS submitter_id BIGINT;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS submit_time TIMESTAMP;
