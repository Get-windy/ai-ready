-- V10.0.0: 创建预订货单主表和明细表
-- 预订货单(SalePreOrder)用于订货会订货、预订货等业务场景

-- ═══════════════════════════════════════════
-- 预订货单主表
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_pre_order (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    order_no        VARCHAR(50)     NOT NULL,
    order_date      DATE            NOT NULL,
    status          INT             NOT NULL DEFAULT 0,
    sale_type       INT             NOT NULL DEFAULT 0,

    -- 客户/往来单位
    customer_id     BIGINT          NOT NULL,
    customer_name   VARCHAR(200)    NOT NULL,
    customer_code   VARCHAR(50),
    customer_level  VARCHAR(50),
    customer_ticket VARCHAR(100),
    customer_remark TEXT,

    -- 银行信息
    bank_name       VARCHAR(200),
    bank_account    VARCHAR(100),
    tax_no          VARCHAR(50),

    -- 仓库
    warehouse_id    BIGINT,
    warehouse_name  VARCHAR(200),

    -- 收货信息
    receiver_name   VARCHAR(100),
    receiver_phone  VARCHAR(50),
    shipping_address VARCHAR(500),
    region          VARCHAR(100),

    -- 经手人/部门
    handler_id      BIGINT,
    handler_name    VARCHAR(100),
    dept_id         BIGINT,
    dept_name       VARCHAR(100),

    -- 金额
    total_amount        DECIMAL(18,2) DEFAULT 0,
    discounted_amount   DECIMAL(18,2) DEFAULT 0,
    order_amount        DECIMAL(18,2) DEFAULT 0,

    -- 结算
    settlement_status   INT DEFAULT 0,
    received_deposit    DECIMAL(18,2) DEFAULT 0,
    unreceived_deposit  DECIMAL(18,2) DEFAULT 0,
    deposit_balance     DECIMAL(18,2) DEFAULT 0,

    -- 预订金账户
    deposit_account1    VARCHAR(100),
    deposit_account2    VARCHAR(100),
    deposit_account3    VARCHAR(100),
    deposit_account4    VARCHAR(100),
    deposit_amount      DECIMAL(18,2) DEFAULT 0,

    -- 信用
    credit_limit        DECIMAL(18,2) DEFAULT 0,
    deposit_deadline    DATE,

    -- 数量汇总
    pre_order_quantity  DECIMAL(18,4) DEFAULT 0,
    ordered_quantity    DECIMAL(18,4) DEFAULT 0,
    un_ordered_quantity DECIMAL(18,4) DEFAULT 0,
    shipped_quantity    DECIMAL(18,4) DEFAULT 0,
    un_shipped_quantity DECIMAL(18,4) DEFAULT 0,

    -- 体积重量
    total_weight    DECIMAL(18,4) DEFAULT 0,
    total_volume    DECIMAL(18,4) DEFAULT 0,

    -- 表头自定义字段
    ext_num1        DECIMAL(18,2),
    ext_num2        DECIMAL(18,2),
    ext_text1       VARCHAR(500),
    ext_text2       VARCHAR(500),
    ext_text3       VARCHAR(500),

    -- 表尾自定义字段
    footer_ext_text1 VARCHAR(500),
    footer_ext_text2 VARCHAR(500),

    -- 扩展信息
    summary         TEXT,
    remark          TEXT,
    attachment      TEXT,
    ext_info        TEXT,

    -- 审核流
    create_by       BIGINT,
    creator_name    VARCHAR(100),
    submit_by       BIGINT,
    submitter_name  VARCHAR(100),
    submit_time     TIMESTAMP,
    approved_by     BIGINT,
    auditor_name    VARCHAR(100),
    approved_time   TIMESTAMP,

    -- 系统字段
    print_count     INT DEFAULT 0,
    version_no      INT DEFAULT 0,
    deleted         INT DEFAULT 0,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,

    CONSTRAINT pk_erp_sale_pre_order PRIMARY KEY (id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_spo_tenant ON erp_sale_pre_order(tenant_id);
CREATE INDEX IF NOT EXISTS idx_spo_customer ON erp_sale_pre_order(customer_id);
CREATE INDEX IF NOT EXISTS idx_spo_order_no ON erp_sale_pre_order(order_no);
CREATE INDEX IF NOT EXISTS idx_spo_date ON erp_sale_pre_order(order_date);
CREATE INDEX IF NOT EXISTS idx_spo_status ON erp_sale_pre_order(status);
CREATE INDEX IF NOT EXISTS idx_spo_warehouse ON erp_sale_pre_order(warehouse_id);

COMMENT ON TABLE erp_sale_pre_order IS '预订货单主表';
COMMENT ON COLUMN erp_sale_pre_order.status IS '单据状态: 0=草稿 1=审核中 2=待订货 3=部分订货 4=已订货 5=已完成 -1=已取消';
COMMENT ON COLUMN erp_sale_pre_order.settlement_status IS '结算状态: 0=未结算 1=部分结算 2=已结算';
COMMENT ON COLUMN erp_sale_pre_order.sale_type IS '销售类型: 0=正常销售 1=样品销售 2=促销销售';

-- ═══════════════════════════════════════════
-- 预订货单明细表
-- ═══════════════════════════════════════════
CREATE TABLE IF NOT EXISTS erp_sale_pre_order_item (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    order_id        BIGINT          NOT NULL,
    line_no         INT             NOT NULL,

    -- 商品信息
    product_id      BIGINT,
    image_url       VARCHAR(500),
    product_name    VARCHAR(200),
    product_code    VARCHAR(50),
    barcode         VARCHAR(100),
    specification   VARCHAR(200),
    model           VARCHAR(200),
    origin          VARCHAR(100),
    brand           VARCHAR(100),

    -- 单位与换算
    unit            VARCHAR(50),
    small_unit      VARCHAR(50),
    small_unit_quantity DECIMAL(18,4) DEFAULT 0,
    conversion_relation VARCHAR(100),
    conversion_result   DECIMAL(18,4) DEFAULT 0,

    -- 库存
    region              VARCHAR(100),
    location            VARCHAR(100),
    available_stock     DECIMAL(18,4) DEFAULT 0,
    available_stock_conversion DECIMAL(18,4) DEFAULT 0,
    book_stock          DECIMAL(18,4) DEFAULT 0,

    -- 数量
    quantity            DECIMAL(18,4) DEFAULT 0,
    piece_quantity      DECIMAL(18,4) DEFAULT 0,
    big_pack            DECIMAL(18,4) DEFAULT 0,
    mid_pack            DECIMAL(18,4) DEFAULT 0,
    small_pack          DECIMAL(18,4) DEFAULT 0,
    ordered_quantity    DECIMAL(18,4) DEFAULT 0,
    un_ordered_quantity DECIMAL(18,4) DEFAULT 0,
    shipped_quantity    DECIMAL(18,4) DEFAULT 0,
    un_shipped_quantity DECIMAL(18,4) DEFAULT 0,
    terminate_quantity  DECIMAL(18,4) DEFAULT 0,
    terminate_amount    DECIMAL(18,2) DEFAULT 0,

    -- 价格
    last_sale_date      DATE,
    retail_price        DECIMAL(18,2) DEFAULT 0,
    wholesale_price     DECIMAL(18,2) DEFAULT 0,
    min_sale_price      DECIMAL(18,2) DEFAULT 0,
    unit_price          DECIMAL(18,2) DEFAULT 0,
    amount              DECIMAL(18,2) DEFAULT 0,
    small_unit_price    DECIMAL(18,2) DEFAULT 0,

    -- 折扣
    discount_rate       DECIMAL(8,2) DEFAULT 0,
    discounted_price    DECIMAL(18,2) DEFAULT 0,
    discounted_amount   DECIMAL(18,2) DEFAULT 0,

    -- 成本与毛利
    cost_price          DECIMAL(18,2) DEFAULT 0,
    cost_amount         DECIMAL(18,2) DEFAULT 0,
    gross_profit        DECIMAL(18,2) DEFAULT 0,

    -- 体积重量
    volume              DECIMAL(18,4) DEFAULT 0,
    weight              DECIMAL(18,4) DEFAULT 0,

    -- 属性
    product_attribute   VARCHAR(100),
    gift                BOOLEAN DEFAULT FALSE,
    remark              TEXT,

    -- 价格等级(8个)
    price_level1        DECIMAL(18,2) DEFAULT 0,
    price_level2        DECIMAL(18,2) DEFAULT 0,
    price_level3        DECIMAL(18,2) DEFAULT 0,
    price_level4        DECIMAL(18,2) DEFAULT 0,
    price_level5        DECIMAL(18,2) DEFAULT 0,
    price_level6        DECIMAL(18,2) DEFAULT 0,
    price_level7        DECIMAL(18,2) DEFAULT 0,
    price_level8        DECIMAL(18,2) DEFAULT 0,

    -- 表体自定义字段
    ext_num1        DECIMAL(18,2),
    ext_num2        DECIMAL(18,2),
    ext_num3        DECIMAL(18,2),
    ext_num4        DECIMAL(18,2),
    ext_num5        DECIMAL(18,2),
    ext_text1       VARCHAR(500),
    ext_text2       VARCHAR(500),
    ext_partner     BIGINT,
    ext_staff       BIGINT,
    ext_dept        BIGINT,

    -- 系统字段
    deleted         INT DEFAULT 0,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,

    CONSTRAINT pk_erp_sale_pre_order_item PRIMARY KEY (id)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_spoi_tenant ON erp_sale_pre_order_item(tenant_id);
CREATE INDEX IF NOT EXISTS idx_spoi_order ON erp_sale_pre_order_item(order_id);
CREATE INDEX IF NOT EXISTS idx_spoi_product ON erp_sale_pre_order_item(product_id);

COMMENT ON TABLE erp_sale_pre_order_item IS '预订货单明细表';
