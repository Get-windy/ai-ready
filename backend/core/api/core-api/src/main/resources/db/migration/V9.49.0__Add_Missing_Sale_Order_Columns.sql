-- V9.49.0: 补全 erp_sale_order 表缺失列
-- SaleOrder 实体定义了大量生产级字段，但之前的迁移未添加对应的数据库列
-- 导致 MyBatis-Plus SELECT 时报 "column does not exist" 500 错误

-- ═══ 客户快照字段 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS customer_code VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS customer_remark VARCHAR(1000);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS customer_level VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS customer_ticket VARCHAR(20);

-- ═══ 银行/税务 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS bank_name VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS tax_no VARCHAR(100);

-- ═══ 仓库/经手人/部门 名称快照 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS salesman_name VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS dept_name VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS promoter_id BIGINT;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS promoter_name VARCHAR(100);

-- ═══ 收货信息补充 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS pickup_address VARCHAR(500);

-- ═══ 结款/配送 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS settlement_method VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS delivery_route VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS delivery_route_id BIGINT;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS driver_id BIGINT;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS driver_name VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS delivery_vehicle VARCHAR(50);

-- ═══ 物流补充 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS freight_payer VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS waybill_no VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS cod_amount DECIMAL(18,2) DEFAULT 0;

-- ═══ 金额汇总 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS product_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS promo_discount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS coupon_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS direct_discount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS other_fee DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS bill_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,2) DEFAULT 0;

-- ═══ 订金/预收 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deposit_account VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deposit_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS prev_advance DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS advance_balance DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deposit_account1 VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deposit_account2 VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deposit_account3 VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS deposit_account4 VARCHAR(100);

-- ═══ 信用额度 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS credit_limit DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS available_credit DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS prev_debt DECIMAL(18,2) DEFAULT 0;

-- ═══ 收款日/对账日 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS payment_date DATE;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS reconciliation_date DATE;

-- ═══ 会员/积分 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS prev_points DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS sale_points DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS return_points DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS exchange_points DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS used_points DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS current_points DECIMAL(18,2) DEFAULT 0;

-- ═══ 数量汇总 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS total_quantity DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS shipped_quantity DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS unshipped_quantity DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS return_quantity DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS return_amount DECIMAL(18,2) DEFAULT 0;

-- ═══ 物理属性汇总 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS total_weight DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS total_volume DECIMAL(18,6) DEFAULT 0;

-- ═══ 备注/摘要 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS region VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS attachment TEXT;

-- ═══ 自定义字段（表头） ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_num1 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_num2 DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(500);

-- ═══ 表尾自定义字段 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS footer_ext_text1 VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS footer_ext_text2 VARCHAR(500);

-- ═══ 审核信息 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS auditor_id BIGINT;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS audit_time TIMESTAMP;

-- ═══ 提交信息 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS submitter_id BIGINT;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS submitter_name VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS submit_time TIMESTAMP;

-- ═══ 制单/打印 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100);

-- ═══ 第三方/来源 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS third_party_order_no VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS source_order VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS product_brand VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS industry_category VARCHAR(200);

-- ═══ 补单/履约 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS supplement_type VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS supplement_status VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS generation_method VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS original_order_id BIGINT;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS original_order_no VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS shipped_order_no VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS original_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS remaining_unshipped_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS original_discount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS original_item_count INTEGER DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS unshipped_item_count INTEGER DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS original_quantity DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS unshipped_quantity_items DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS fulfillment_rate DECIMAL(8,2) DEFAULT 0;

-- ═══ 拣货仓库/集货位 ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS picking_warehouse VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS collection_location VARCHAR(200);

-- ═══ 预计发货时间(新命名) ═══
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS expected_ship_time TIMESTAMP;

-- ═══ 已收金额(新命名, 原received_amount保留) ═══
-- received_amount 已在 V8.0.0 中存在, 不需要再添加
