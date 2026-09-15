-- =============================================
-- 采购换货单表扩展脚本
-- 版本: V11.44.0
-- 说明: 扩展erp_purchase_exchange和erp_purchase_exchange_item表字段，支持双仓库换货
-- =============================================

-- =============================================
-- 1. 创建/扩展明细表 erp_purchase_exchange_item
-- =============================================
-- 兜底：全新库直接创建完整结构；已存在的旧表由下方 ALTER 补齐新列
CREATE TABLE IF NOT EXISTS erp_purchase_exchange_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    exchange_id BIGINT NOT NULL,
    warehouse_type INTEGER DEFAULT 1,
    product_id BIGINT,
    product_name VARCHAR(200),
    product_code VARCHAR(100),
    barcode VARCHAR(100),
    specification VARCHAR(200),
    model VARCHAR(200),
    origin VARCHAR(200),
    brand VARCHAR(200),
    unit VARCHAR(50),
    image VARCHAR(500),
    location VARCHAR(100),
    area VARCHAR(100),
    available_stock DECIMAL(18,4) DEFAULT 0,
    stock_converted DECIMAL(18,4) DEFAULT 0,
    book_stock DECIMAL(18,4) DEFAULT 0,
    batch_barcode VARCHAR(100),
    production_date DATE,
    shelf_life INTEGER,
    expiry_date DATE,
    quantity DECIMAL(18,4) DEFAULT 0,
    conversion_rate DECIMAL(18,4) DEFAULT 1,
    piece_scatter_qty DECIMAL(18,4) DEFAULT 0,
    large_package DECIMAL(18,4) DEFAULT 0,
    medium_package DECIMAL(18,4) DEFAULT 0,
    small_package DECIMAL(18,4) DEFAULT 0,
    recent_purchase_date DATE,
    retail_price DECIMAL(18,2) DEFAULT 0,
    wholesale_price DECIMAL(18,2) DEFAULT 0,
    unit_price DECIMAL(18,2) DEFAULT 0,
    amount DECIMAL(18,2) DEFAULT 0,
    small_unit VARCHAR(50),
    small_unit_price DECIMAL(18,2) DEFAULT 0,
    small_unit_qty DECIMAL(18,4) DEFAULT 0,
    cost_price DECIMAL(18,2) DEFAULT 0,
    cost_amount DECIMAL(18,2) DEFAULT 0,
    discount DECIMAL(5,2) DEFAULT 100,
    discount_price DECIMAL(18,2) DEFAULT 0,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    volume DECIMAL(18,4) DEFAULT 0,
    weight DECIMAL(18,4) DEFAULT 0,
    is_gift BOOLEAN DEFAULT FALSE,
    remark VARCHAR(500),
    price_level_1 DECIMAL(18,2) DEFAULT 0,
    price_level_2 DECIMAL(18,2) DEFAULT 0,
    price_level_3 DECIMAL(18,2) DEFAULT 0,
    price_level_4 DECIMAL(18,2) DEFAULT 0,
    price_level_5 DECIMAL(18,2) DEFAULT 0,
    price_level_6 DECIMAL(18,2) DEFAULT 0,
    price_level_7 DECIMAL(18,2) DEFAULT 0,
    price_level_8 DECIMAL(18,2) DEFAULT 0,
    ext_num1 DECIMAL(18,4),
    ext_num2 DECIMAL(18,4),
    ext_num3 DECIMAL(18,4),
    ext_num4 DECIMAL(18,4),
    ext_num5 DECIMAL(18,4),
    ext_num6 DECIMAL(18,4),
    ext_num7 DECIMAL(18,4),
    ext_num8 DECIMAL(18,4),
    ext_num9 DECIMAL(18,4),
    ext_num10 DECIMAL(18,4),
    ext_text1 VARCHAR(500),
    ext_text2 VARCHAR(500),
    ext_text3 VARCHAR(500),
    ext_text4 VARCHAR(500),
    ext_text5 VARCHAR(500),
    ext_text6 VARCHAR(500),
    ext_partner BIGINT,
    ext_staff BIGINT,
    ext_dept BIGINT,
    deleted INTEGER DEFAULT 0,
    create_by BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 旧的明细表（V9.34.0）缺少新模型列，逐个补齐
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS warehouse_type INTEGER DEFAULT 1;
COMMENT ON COLUMN erp_purchase_exchange_item.warehouse_type IS '仓库类型: 1=换入, 2=换出';
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS barcode VARCHAR(100);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS specification VARCHAR(200);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS model VARCHAR(200);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS origin VARCHAR(200);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS brand VARCHAR(200);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS image VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS location VARCHAR(100);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS area VARCHAR(100);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS available_stock DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS stock_converted DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS book_stock DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS batch_barcode VARCHAR(100);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS production_date DATE;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS shelf_life INTEGER;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS quantity DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS conversion_rate DECIMAL(18,4) DEFAULT 1;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS piece_scatter_qty DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS large_package DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS medium_package DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS small_package DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS recent_purchase_date DATE;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS retail_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS wholesale_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS unit_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(50);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS small_unit_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS small_unit_qty DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS cost_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS cost_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS discount DECIMAL(5,2) DEFAULT 100;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS discount_price DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS volume DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS weight DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS is_gift BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_1 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_2 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_3 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_4 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_5 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_6 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_7 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS price_level_8 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num1 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num2 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num3 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num4 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num5 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num6 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num7 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num8 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num9 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_num10 DECIMAL(18,4);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_text6 VARCHAR(500);
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_partner BIGINT;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_staff BIGINT;
ALTER TABLE erp_purchase_exchange_item ADD COLUMN IF NOT EXISTS ext_dept BIGINT;

-- =============================================
-- 2. 扩展主表 erp_purchase_exchange
-- =============================================

-- 供应商快照字段（来源: erp_supplier）
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(50);
COMMENT ON COLUMN erp_purchase_exchange.supplier_code IS '供应商编号';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS supplier_remark VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.supplier_remark IS '供应商备注';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100);
COMMENT ON COLUMN erp_purchase_exchange.contact_name IS '联系人';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50);
COMMENT ON COLUMN erp_purchase_exchange.contact_phone IS '联系电话';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS contact_address VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.contact_address IS '联系地址';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS bank_name VARCHAR(200);
COMMENT ON COLUMN erp_purchase_exchange.bank_name IS '开户行';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100);
COMMENT ON COLUMN erp_purchase_exchange.bank_account IS '银行账号';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS tax_no VARCHAR(100);
COMMENT ON COLUMN erp_purchase_exchange.tax_no IS '税号';

-- 仓库快照字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS in_warehouse_id BIGINT;
COMMENT ON COLUMN erp_purchase_exchange.in_warehouse_id IS '换入仓库ID';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS in_warehouse_name VARCHAR(200);
COMMENT ON COLUMN erp_purchase_exchange.in_warehouse_name IS '换入仓库名称';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS out_warehouse_id BIGINT;
COMMENT ON COLUMN erp_purchase_exchange.out_warehouse_id IS '换出仓库ID';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS out_warehouse_name VARCHAR(200);
COMMENT ON COLUMN erp_purchase_exchange.out_warehouse_name IS '换出仓库名称';

-- 职员/部门快照字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS handler_id BIGINT;
COMMENT ON COLUMN erp_purchase_exchange.handler_id IS '经手人ID';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS handler_name VARCHAR(100);
COMMENT ON COLUMN erp_purchase_exchange.handler_name IS '经手人';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS dept_id BIGINT;
COMMENT ON COLUMN erp_purchase_exchange.dept_id IS '部门ID';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS dept_name VARCHAR(200);
COMMENT ON COLUMN erp_purchase_exchange.dept_name IS '部门名称';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);
COMMENT ON COLUMN erp_purchase_exchange.bookkeeper_name IS '记账人';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
COMMENT ON COLUMN erp_purchase_exchange.bookkeeping_time IS '记账时间';

-- 付款/结算字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS payment_account VARCHAR(100);
COMMENT ON COLUMN erp_purchase_exchange.payment_account IS '付款账户';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.paid_amount IS '付款金额';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS more_accounts VARCHAR(200);
COMMENT ON COLUMN erp_purchase_exchange.more_accounts IS '更多账户';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS prev_prepaid DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.prev_prepaid IS '此前预付';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS use_prepaid DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.use_prepaid IS '使用预付款';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS prepaid_balance DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.prepaid_balance IS '预付余额';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS prev_debt DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.prev_debt IS '此前欠款';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS current_debt DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.current_debt IS '本次欠款';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS debt_balance DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.debt_balance IS '欠款余额';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS payment_deadline VARCHAR(50);
COMMENT ON COLUMN erp_purchase_exchange.payment_deadline IS '付款期限';

-- 金额计算链字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS product_amount DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.product_amount IS '商品金额';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.discount_amount IS '折扣金额';

-- 数量汇总字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS in_quantity_total DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.in_quantity_total IS '换入数量合计';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS out_quantity_total DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.out_quantity_total IS '换出数量合计';

-- 物理属性汇总字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS total_weight DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.total_weight IS '重量合计(kg)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS total_volume DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.total_volume IS '体积合计(m³)';

-- 结算字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS settle_status VARCHAR(20);
COMMENT ON COLUMN erp_purchase_exchange.settle_status IS '结算状态';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.settled_amount IS '已结金额';

-- 其他业务字段
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_purchase_exchange.print_count IS '打印次数';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS attachment TEXT;
COMMENT ON COLUMN erp_purchase_exchange.attachment IS '附件';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.summary IS '摘要';

-- 自定义字段（表头-数字）
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_num1 DECIMAL(18,4);
COMMENT ON COLUMN erp_purchase_exchange.ext_num1 IS '自定义字段1(数字)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_num2 DECIMAL(18,4);
COMMENT ON COLUMN erp_purchase_exchange.ext_num2 IS '自定义字段2(数字)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_num3 DECIMAL(18,4);
COMMENT ON COLUMN erp_purchase_exchange.ext_num3 IS '自定义字段3(数字)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_num4 DECIMAL(18,4);
COMMENT ON COLUMN erp_purchase_exchange.ext_num4 IS '自定义字段4(数字)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_num5 DECIMAL(18,4);
COMMENT ON COLUMN erp_purchase_exchange.ext_num5 IS '自定义字段5(数字)';

-- 自定义字段（表头-文本）
ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.ext_text1 IS '自定义字段1(文本)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.ext_text2 IS '自定义字段2(文本)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.ext_text3 IS '自定义字段3(文本)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.ext_text4 IS '自定义字段4(文本)';

ALTER TABLE erp_purchase_exchange ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(500);
COMMENT ON COLUMN erp_purchase_exchange.ext_text5 IS '自定义字段5(文本)';

-- =============================================
-- 3. 创建索引（优化查询性能）
-- =============================================

-- 主表索引
CREATE INDEX IF NOT EXISTS idx_exchange_supplier ON erp_purchase_exchange(supplier_id);
CREATE INDEX IF NOT EXISTS idx_exchange_date ON erp_purchase_exchange(exchange_date);
CREATE INDEX IF NOT EXISTS idx_exchange_status ON erp_purchase_exchange(status);
CREATE INDEX IF NOT EXISTS idx_exchange_in_warehouse ON erp_purchase_exchange(in_warehouse_id);
CREATE INDEX IF NOT EXISTS idx_exchange_out_warehouse ON erp_purchase_exchange(out_warehouse_id);

-- 明细表索引
CREATE INDEX IF NOT EXISTS idx_exchange_item_exchange ON erp_purchase_exchange_item(exchange_id);
CREATE INDEX IF NOT EXISTS idx_exchange_item_product ON erp_purchase_exchange_item(product_id);
CREATE INDEX IF NOT EXISTS idx_exchange_item_warehouse_type ON erp_purchase_exchange_item(warehouse_type);

-- =============================================
-- 完成
-- =============================================
