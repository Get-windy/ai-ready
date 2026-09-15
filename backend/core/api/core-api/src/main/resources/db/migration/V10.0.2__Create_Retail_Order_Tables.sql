-- ============================================================
-- V10.0.2 创建零售单模块基础表
-- 零售单主表 + 明细表
-- ============================================================

-- ═══════════════════════════════════════════════════════════════
-- 1. 零售单主表 erp_retail_order
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS erp_retail_order (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,

    -- 单据基本信息
    retail_no           VARCHAR(50)     NOT NULL,
    order_date          DATE            NOT NULL,
    sale_type           VARCHAR(20)     DEFAULT 'NORMAL',
    status              INTEGER         NOT NULL DEFAULT 0,

    -- 客户快照
    customer_id         BIGINT,
    customer_name       VARCHAR(200),

    -- 仓库/经手人
    warehouse_id        BIGINT,
    warehouse_name      VARCHAR(200),
    handler_id          BIGINT,
    handler_name        VARCHAR(100),

    -- 会员信息
    member_card_no      VARCHAR(50),
    member_name         VARCHAR(100),

    -- 数量/金额
    amount              NUMERIC(18,2)   DEFAULT 0,
    direct_discount     NUMERIC(18,2)   DEFAULT 0,
    coupon_discount     NUMERIC(18,2)   DEFAULT 0,
    promo_discount      NUMERIC(18,2)   DEFAULT 0,
    payable_amount      NUMERIC(18,2)   DEFAULT 0,

    -- 收款方式（汇总）
    payment_method      VARCHAR(50),
    cash_amount         NUMERIC(18,2)   DEFAULT 0,
    card_amount         NUMERIC(18,2)   DEFAULT 0,
    prepaid_amount      NUMERIC(18,2)   DEFAULT 0,
    transfer_amount     NUMERIC(18,2)   DEFAULT 0,
    total_received      NUMERIC(18,2)   DEFAULT 0,
    combined_payment    BOOLEAN         DEFAULT false,
    change_amount       NUMERIC(18,2)   DEFAULT 0,

    -- 备注
    remark              VARCHAR(500),

    -- 系统字段
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE erp_retail_order IS '零售单主表';
COMMENT ON COLUMN erp_retail_order.retail_no IS '零售单号（LSD-YYYYMMDD-NNNN）';
COMMENT ON COLUMN erp_retail_order.order_date IS '单据日期';
COMMENT ON COLUMN erp_retail_order.sale_type IS '销售类型（NORMAL/RETURN）';
COMMENT ON COLUMN erp_retail_order.status IS '状态：0=草稿 1=已完成 2=挂单 3=已作废';
COMMENT ON COLUMN erp_retail_order.customer_id IS '客户ID → biz_party.id';
COMMENT ON COLUMN erp_retail_order.customer_name IS '客户名称（快照）';
COMMENT ON COLUMN erp_retail_order.warehouse_id IS '仓库ID';
COMMENT ON COLUMN erp_retail_order.handler_id IS '经手人ID';
COMMENT ON COLUMN erp_retail_order.amount IS '商品总金额（优惠前）';
COMMENT ON COLUMN erp_retail_order.payable_amount IS '应付金额';

CREATE INDEX IF NOT EXISTS idx_retail_order_retail_no ON erp_retail_order(retail_no);
CREATE INDEX IF NOT EXISTS idx_retail_order_customer ON erp_retail_order(customer_id);

-- ═══════════════════════════════════════════════════════════════
-- 2. 零售单明细表 erp_retail_order_item
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS erp_retail_order_item (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL DEFAULT 1,
    order_id            BIGINT          NOT NULL,

    -- 商品信息
    product_id          BIGINT,
    product_name        VARCHAR(200),
    barcode             VARCHAR(100),
    unit                VARCHAR(20),

    -- 数量/金额
    quantity            NUMERIC(18,2)   NOT NULL DEFAULT 0,
    unit_price          NUMERIC(18,2)   NOT NULL DEFAULT 0,
    amount              NUMERIC(18,2)   NOT NULL DEFAULT 0,

    -- 备注
    remark              VARCHAR(500),

    -- 系统字段
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT
);

COMMENT ON TABLE erp_retail_order_item IS '零售单明细表';
COMMENT ON COLUMN erp_retail_order_item.order_id IS '所属零售单ID';
COMMENT ON COLUMN erp_retail_order_item.product_id IS '商品ID';
COMMENT ON COLUMN erp_retail_order_item.product_name IS '商品名称（快照）';
COMMENT ON COLUMN erp_retail_order_item.barcode IS '条码';
COMMENT ON COLUMN erp_retail_order_item.quantity IS '数量';
COMMENT ON COLUMN erp_retail_order_item.unit_price IS '单价';
COMMENT ON COLUMN erp_retail_order_item.amount IS '金额';

CREATE INDEX IF NOT EXISTS idx_retail_order_item_order ON erp_retail_order_item(order_id);
