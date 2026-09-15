-- V8.0.0: 创建销售订单主表和明细表
-- erp_sale_order / erp_sale_order_item
-- 配套后端实体: SaleOrder.java / SaleOrderItem.java

-- ============================================================
-- 1. 销售订单主表
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_sale_order (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    order_no            VARCHAR(64) NOT NULL,
    customer_id         BIGINT,
    customer_name       VARCHAR(200),
    order_date          TIMESTAMP,
    expected_ship_date  TIMESTAMP,
    sale_type           INTEGER DEFAULT 1,
    status              INTEGER DEFAULT 0,
    total_amount        DECIMAL(18,2) DEFAULT 0,
    tax_amount          DECIMAL(18,2) DEFAULT 0,
    total_amount_with_tax DECIMAL(18,2) DEFAULT 0,
    received_amount     DECIMAL(18,2) DEFAULT 0,
    salesman_id         BIGINT,
    salesman_name       VARCHAR(100),
    dept_id             BIGINT,
    warehouse_id        BIGINT,
    shipping_address    VARCHAR(500),
    receiver_name       VARCHAR(100),
    receiver_phone      VARCHAR(50),
    payment_account_id  BIGINT,
    logistics_company   VARCHAR(200),
    logistics_no        VARCHAR(200),
    shipping_fee        DECIMAL(18,2) DEFAULT 0,
    member_card_no      VARCHAR(100),
    member_name         VARCHAR(100),
    member_discount     INTEGER DEFAULT 100,
    order_remark        VARCHAR(1000),
    buyer_remark        VARCHAR(1000),
    remark              VARCHAR(1000),
    ext_info            TEXT,
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE erp_sale_order IS '销售订单主表';
COMMENT ON COLUMN erp_sale_order.sale_type IS '销售类型：1-正常销售 2-样品销售 3-促销销售';
COMMENT ON COLUMN erp_sale_order.status IS '状态：0-草稿 1-待审批 2-已审批 3-部分出库 4-完成 5-取消';
COMMENT ON COLUMN erp_sale_order.member_discount IS '会员折扣（百分比，100=无折扣）';

CREATE INDEX IF NOT EXISTS idx_so_tenant ON erp_sale_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_so_order_no ON erp_sale_order(order_no);
CREATE INDEX IF NOT EXISTS idx_so_customer ON erp_sale_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_so_status ON erp_sale_order(status);
CREATE INDEX IF NOT EXISTS idx_so_date ON erp_sale_order(order_date);
CREATE INDEX IF NOT EXISTS idx_so_salesman ON erp_sale_order(salesman_id);

-- ============================================================
-- 2. 销售订单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_sale_order_item (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    line_no             INTEGER,
    product_id          BIGINT,
    product_code        VARCHAR(100),
    product_name        VARCHAR(300),
    barcode             VARCHAR(200),
    specification       VARCHAR(200),
    location            VARCHAR(200),
    unit                VARCHAR(50),
    line_attribute      VARCHAR(200),
    batch_code          VARCHAR(200),
    production_date     TIMESTAMP,
    shelf_life          VARCHAR(100),
    expiry_date         TIMESTAMP,
    big_pack            DECIMAL(18,2) DEFAULT 0,
    mid_pack            DECIMAL(18,2) DEFAULT 0,
    small_pack          DECIMAL(18,2) DEFAULT 0,
    quantity            DECIMAL(18,2) DEFAULT 0,
    shipped_quantity    DECIMAL(18,2) DEFAULT 0,
    unit_price          DECIMAL(18,2) DEFAULT 0,
    tax_rate            DECIMAL(8,2) DEFAULT 0,
    unit_price_with_tax DECIMAL(18,2) DEFAULT 0,
    amount              DECIMAL(18,2) DEFAULT 0,
    tax_amount          DECIMAL(18,2) DEFAULT 0,
    amount_with_tax     DECIMAL(18,2) DEFAULT 0,
    discount_rate       DECIMAL(8,2) DEFAULT 0,
    discount_amount     DECIMAL(18,2) DEFAULT 0,
    warehouse_id        BIGINT,
    remark              VARCHAR(500),
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_sale_order_item IS '销售订单明细表';
COMMENT ON COLUMN erp_sale_order_item.line_attribute IS '商品行属性';
COMMENT ON COLUMN erp_sale_order_item.batch_code IS '批次条码';
COMMENT ON COLUMN erp_sale_order_item.big_pack IS '大包装数量';
COMMENT ON COLUMN erp_sale_order_item.mid_pack IS '中包装数量';
COMMENT ON COLUMN erp_sale_order_item.small_pack IS '小包装数量';

CREATE INDEX IF NOT EXISTS idx_soi_order ON erp_sale_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_soi_product ON erp_sale_order_item(product_id);

-- ============================================================
-- 3. 验证
-- ============================================================
SELECT COUNT(*) AS table_count FROM information_schema.tables
WHERE table_name IN ('erp_sale_order', 'erp_sale_order_item');
