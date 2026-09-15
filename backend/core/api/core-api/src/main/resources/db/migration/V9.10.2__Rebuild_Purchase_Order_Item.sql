-- V9.10.0 重建采购订单明细表（生产级完整字段版）
-- 对标 SaleOrderItem 结构，从 17 列扩展到 70+ 列
-- 原表 purchase_order_item 废弃，新建 erp_purchase_order_item

-- ============================================================
-- 采购订单明细表（生产级完整字段）
-- ============================================================
CREATE TABLE IF NOT EXISTS erp_purchase_order_item (
    id                          BIGSERIAL       PRIMARY KEY,
    order_id                    BIGINT          NOT NULL,
    line_no                     INTEGER,

    -- 商品信息快照
    product_id                  BIGINT,
    product_code                VARCHAR(50),
    product_name                VARCHAR(200),
    image                       VARCHAR(500),
    item_code                   VARCHAR(100),
    barcode                     VARCHAR(100),
    small_unit_barcode          VARCHAR(100),
    specification               VARCHAR(200),
    model                       VARCHAR(100),
    origin                      VARCHAR(100),
    brand                       VARCHAR(100),
    shelf_life                  VARCHAR(50),
    unit                        VARCHAR(50),
    pricing_unit                VARCHAR(50),
    small_unit                  VARCHAR(50),
    line_attribute              VARCHAR(50),
    area                        VARCHAR(100),
    location                    VARCHAR(100),

    -- 批次信息
    batch_code                  VARCHAR(100),
    production_date             DATE,
    expiry_date                 DATE,

    -- 包装/数量
    quantity                    DECIMAL(18,2)   DEFAULT 0,
    big_pack                    DECIMAL(18,2)   DEFAULT 0,
    mid_pack                    DECIMAL(18,2)   DEFAULT 0,
    small_pack                  DECIMAL(18,2)   DEFAULT 0,
    small_unit_quantity         DECIMAL(18,2)   DEFAULT 0,
    conversion_relation         VARCHAR(100),

    -- 库存快照
    available_stock             DECIMAL(18,2)   DEFAULT 0,
    available_stock_converted   DECIMAL(18,2)   DEFAULT 0,
    book_stock                  DECIMAL(18,2)   DEFAULT 0,
    unshipped_quantity          DECIMAL(18,2)   DEFAULT 0,
    received_quantity_detail    DECIMAL(18,2)   DEFAULT 0,
    terminated_quantity         DECIMAL(18,2)   DEFAULT 0,
    terminated_amount           DECIMAL(18,2)   DEFAULT 0,

    -- 价格信息
    small_unit_price            DECIMAL(18,2)   DEFAULT 0,
    latest_purchase_date        DATE,
    latest_purchase_price       DECIMAL(18,2)   DEFAULT 0,
    retail_price                DECIMAL(18,2)   DEFAULT 0,
    wholesale_price             DECIMAL(18,2)   DEFAULT 0,
    unit_price                  DECIMAL(18,2)   DEFAULT 0,
    cost_price                  DECIMAL(18,2)   DEFAULT 0,
    cost_amount                 DECIMAL(18,2)   DEFAULT 0,

    -- 折扣
    discount_rate               DECIMAL(10,2)   DEFAULT 0,
    discounted_unit_price       DECIMAL(18,2)   DEFAULT 0,
    discounted_amount           DECIMAL(18,2)   DEFAULT 0,
    original_price              DECIMAL(18,2)   DEFAULT 0,

    -- 物理属性
    volume                      DECIMAL(18,4)   DEFAULT 0,
    weight                      DECIMAL(18,4)   DEFAULT 0,

    -- 赠品
    gift                        BOOLEAN         DEFAULT FALSE,

    -- 备注
    remark                      VARCHAR(500),

    -- 仓库
    warehouse_id                BIGINT,

    -- 自定义字段 1~10
    custom_field_1              DECIMAL(18,2),
    custom_field_2              DECIMAL(18,2),
    custom_field_3              DECIMAL(18,2),
    custom_field_4              VARCHAR(255),
    custom_field_5              VARCHAR(255),
    custom_field_6              DECIMAL(18,2),
    custom_field_7              DECIMAL(18,2),
    custom_field_8              BIGINT,
    custom_field_9              BIGINT,
    custom_field_10             BIGINT,

    -- 计算/冗余字段
    amount                      DECIMAL(18,2)   DEFAULT 0,
    tax_rate                    DECIMAL(10,2)   DEFAULT 0,
    tax_amount                  DECIMAL(18,2)   DEFAULT 0,
    unit_price_with_tax         DECIMAL(18,2)   DEFAULT 0,
    amount_with_tax             DECIMAL(18,2)   DEFAULT 0,
    discount_amount             DECIMAL(18,2)   DEFAULT 0,
    received_quantity           DECIMAL(18,2)   DEFAULT 0,

    -- 系统字段
    create_time                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time                 TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_item_order ON erp_purchase_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_item_product ON erp_purchase_order_item(product_id);

COMMENT ON TABLE erp_purchase_order_item IS '采购订单明细(生产级完整字段版)';
COMMENT ON COLUMN erp_purchase_order_item.custom_field_8 IS '单据自定义8(往来单位ID)';
COMMENT ON COLUMN erp_purchase_order_item.custom_field_9 IS '单据自定义9(职员ID)';
COMMENT ON COLUMN erp_purchase_order_item.custom_field_10 IS '单据自定义10(部门ID)';
