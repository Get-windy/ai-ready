-- ============================================================
-- V6.31.0: Enhance Sale Outbound Tables
-- 补充 erp_sale_outbound 缺失字段 + 创建 erp_sale_outbound_item
-- ============================================================

-- 1. 为 erp_sale_outbound 补充缺失字段
ALTER TABLE erp_sale_outbound
    ADD COLUMN IF NOT EXISTS customer_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS customer_level VARCHAR(100),
    ADD COLUMN IF NOT EXISTS customer_remark VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS bank_account VARCHAR(200),
    ADD COLUMN IF NOT EXISTS tax_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS location VARCHAR(100),
    ADD COLUMN IF NOT EXISTS region VARCHAR(100),
    ADD COLUMN IF NOT EXISTS generation_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS summary VARCHAR(500),
    ADD COLUMN IF NOT EXISTS promo_discount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS coupon_amount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS direct_discount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS other_fee DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rounding_amount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_weight DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_volume DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS return_quantity DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS return_amount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS box_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settlement_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settlement_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_account1 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account2 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account3 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account4 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(200),
    ADD COLUMN IF NOT EXISTS logistics_branch VARCHAR(200),
    ADD COLUMN IF NOT EXISTS freight_payer VARCHAR(50),
    ADD COLUMN IF NOT EXISTS freight DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(200),
    ADD COLUMN IF NOT EXISTS waybill_no VARCHAR(200),
    ADD COLUMN IF NOT EXISTS cod_amount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS expected_ship_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS actual_ship_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS picking_by BIGINT,
    ADD COLUMN IF NOT EXISTS picking_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS packing_by BIGINT,
    ADD COLUMN IF NOT EXISTS packing_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS shipped_by BIGINT,
    ADD COLUMN IF NOT EXISTS shipped_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS approved_by BIGINT,
    ADD COLUMN IF NOT EXISTS approved_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS approved_note VARCHAR(500),
    ADD COLUMN IF NOT EXISTS completed_by BIGINT,
    ADD COLUMN IF NOT EXISTS completed_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS member_card_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS prev_points DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_generated_points DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_exchange_points DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_used_points DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_points DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS payment_date DATE,
    ADD COLUMN IF NOT EXISTS reconciliation_date DATE,
    ADD COLUMN IF NOT EXISTS internal_note VARCHAR(500),
    ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS print_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ext_num1 DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num2 DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num3 DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num4 DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num5 DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_partner BIGINT,
    ADD COLUMN IF NOT EXISTS ext_staff BIGINT,
    ADD COLUMN IF NOT EXISTS ext_dept BIGINT,
    ADD COLUMN IF NOT EXISTS footer_ext_text1 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS footer_ext_text2 VARCHAR(500),
    -- 预收款
    ADD COLUMN IF NOT EXISTS advance_payment_amount DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prev_advance_payment DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS used_advance_payment DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS order_deposit DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_advance_payment DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS advance_payment_balance DECIMAL(18,2) DEFAULT 0,
    -- 信用
    ADD COLUMN IF NOT EXISTS credit_limit DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_credit DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prev_arrears DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_arrears DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS arrears_balance DECIMAL(18,2) DEFAULT 0,
    -- 物流扩展
    ADD COLUMN IF NOT EXISTS delivery_order_no VARCHAR(200),
    ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 0;

-- 2. 创建 erp_sale_outbound_item 明细表
CREATE TABLE IF NOT EXISTS erp_sale_outbound_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    outbound_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 0,

    -- 产品快照
    product_id BIGINT,
    product_code VARCHAR(100),
    product_name VARCHAR(200),
    image_url VARCHAR(500),
    barcode VARCHAR(100),
    small_unit_barcode VARCHAR(100),
    specification VARCHAR(200),
    product_spec VARCHAR(200),
    model VARCHAR(100),
    origin VARCHAR(100),
    brand VARCHAR(100),
    product_unit VARCHAR(50),
    product_attribute VARCHAR(100),

    -- 货位/区域
    location VARCHAR(100),
    region VARCHAR(100),
    warehouse_location_id BIGINT,
    warehouse_location_code VARCHAR(100),

    -- 来源订单
    order_item_id BIGINT,
    order_no VARCHAR(100),
    order_quantity DECIMAL(18,2) DEFAULT 0,

    -- 数量
    quantity DECIMAL(18,2) DEFAULT 0,
    outbound_quantity DECIMAL(18,2) DEFAULT 0,
    pending_quantity DECIMAL(18,2) DEFAULT 0,
    piece_quantity DECIMAL(18,2) DEFAULT 0,

    -- 包装
    big_pack DECIMAL(18,2) DEFAULT 0,
    mid_pack DECIMAL(18,2) DEFAULT 0,
    small_pack DECIMAL(18,2) DEFAULT 0,

    -- 单位换算
    conversion_relation VARCHAR(100),
    conversion_result DECIMAL(18,2) DEFAULT 0,
    small_unit VARCHAR(50),
    small_unit_quantity DECIMAL(18,2) DEFAULT 0,

    -- 价格快照
    unit_price DECIMAL(18,4) DEFAULT 0,
    line_amount DECIMAL(18,2) DEFAULT 0,
    original_price DECIMAL(18,4) DEFAULT 0,
    small_unit_price DECIMAL(18,4) DEFAULT 0,
    tax_rate DECIMAL(10,2) DEFAULT 0,

    -- 折扣
    discount_rate DECIMAL(10,2) DEFAULT 0,
    discounted_amount DECIMAL(18,2) DEFAULT 0,
    discounted_price DECIMAL(18,4) DEFAULT 0,
    favorable_discount_rate DECIMAL(10,2) DEFAULT 0,
    favorable_unit_price DECIMAL(18,4) DEFAULT 0,
    favorable_amount DECIMAL(18,2) DEFAULT 0,

    -- 参考成本
    cost_price DECIMAL(18,4) DEFAULT 0,
    cost_amount DECIMAL(18,2) DEFAULT 0,
    gross_profit DECIMAL(18,2) DEFAULT 0,

    -- 市场价格快照
    retail_price DECIMAL(18,4) DEFAULT 0,
    wholesale_price DECIMAL(18,4) DEFAULT 0,
    min_sale_price DECIMAL(18,4) DEFAULT 0,
    last_sale_price DECIMAL(18,4) DEFAULT 0,
    last_sale_date DATE,

    -- 价格等级（8级，对标odoo/SAP标准产品价格等级）
    price_level1 DECIMAL(18,4) DEFAULT 0,
    price_level2 DECIMAL(18,4) DEFAULT 0,
    price_level3 DECIMAL(18,4) DEFAULT 0,
    price_level4 DECIMAL(18,4) DEFAULT 0,
    price_level5 DECIMAL(18,4) DEFAULT 0,
    price_level6 DECIMAL(18,4) DEFAULT 0,
    price_level7 DECIMAL(18,4) DEFAULT 0,
    price_level8 DECIMAL(18,4) DEFAULT 0,

    -- 库存快照
    available_stock DECIMAL(18,2) DEFAULT 0,
    available_stock_converted DECIMAL(18,2) DEFAULT 0,
    book_stock DECIMAL(18,2) DEFAULT 0,

    -- 批次/保质期
    shelf_life VARCHAR(50),
    batch_no VARCHAR(100),
    production_date TIMESTAMP,
    validity_date TIMESTAMP,
    expiry_date DATE,

    -- 体积/重量
    volume DECIMAL(18,4) DEFAULT 0,
    weight DECIMAL(18,4) DEFAULT 0,

    -- 赠品/兑换/积分
    gift BOOLEAN DEFAULT FALSE,
    gift_item VARCHAR(200),
    exchange_points DECIMAL(18,2) DEFAULT 0,
    used_points DECIMAL(18,2) DEFAULT 0,
    generated_points DECIMAL(18,2) DEFAULT 0,

    -- 箱号
    box_no VARCHAR(100),
    customer_ticket VARCHAR(200),

    -- 拣货/打包流程
    picking_status INTEGER DEFAULT 0,
    picking_by BIGINT,
    picking_time TIMESTAMP,
    packing_status INTEGER DEFAULT 0,
    packing_by BIGINT,
    packing_time TIMESTAMP,

    -- 备注
    remark VARCHAR(500),

    -- 表体自定义字段（数字 1-7）
    ext_num1 DECIMAL(18,2),
    ext_num2 DECIMAL(18,2),
    ext_num3 DECIMAL(18,2),
    ext_num4 DECIMAL(18,2),
    ext_num5 DECIMAL(18,2),
    ext_num6 DECIMAL(18,2),
    ext_num7 DECIMAL(18,2),

    -- 表体自定义字段（文本 1-2）
    ext_text1 VARCHAR(500),
    ext_text2 VARCHAR(500),

    -- 表体自定义字段（往来单位/职员/部门）
    ext_partner BIGINT,
    ext_staff BIGINT,
    ext_dept BIGINT,

    -- 系统字段
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

-- 3. 创建外键索引
CREATE INDEX IF NOT EXISTS idx_outbound_item_outbound_id ON erp_sale_outbound_item(outbound_id);
CREATE INDEX IF NOT EXISTS idx_outbound_item_product_id ON erp_sale_outbound_item(product_id);
CREATE INDEX IF NOT EXISTS idx_outbound_outbound_no ON erp_sale_outbound(outbound_no);
CREATE INDEX IF NOT EXISTS idx_outbound_order_id ON erp_sale_outbound(order_id);
CREATE INDEX IF NOT EXISTS idx_outbound_customer_id ON erp_sale_outbound(customer_id);
CREATE INDEX IF NOT EXISTS idx_outbound_status ON erp_sale_outbound(status);
CREATE INDEX IF NOT EXISTS idx_outbound_date ON erp_sale_outbound(outbound_date);
