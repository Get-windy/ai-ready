-- ============================================================================
-- V11.123.0 销售单据查询页金标准升级（2026-09-10）
--
-- 背景：销售单据查询页需要统一查询 4 类销售单据（销售订单 / 销售出库单 / 销售退货单 / 销售换货单），
--      但换货单实体（SaleExchange / SaleExchangeItem）声明的字段远多于建表列，
--      导致 MyBatis-Plus 生成 SELECT/INSERT 时命中不存在的列 → 接口直接 500
--      （实测 /api/sales/doc-query/page?documentType=EXCHANGE 及 /api/erp/sale/exchange/page 均 500）。
--
-- 处理：按实体声明补齐缺失列（实体为业务目标态，表为建表遗漏），全部 IF NOT EXISTS 幂等。
--      不作任何列删除/改名，对既有数据零影响。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. erp_sale_exchange（表头）补 57 列
-- ------------------------------------------------------------
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS sales_type VARCHAR(20);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS settle_status VARCHAR(20);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS print_count INT DEFAULT 0;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS attachment VARCHAR(500);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS summary VARCHAR(500);

-- 客户快照
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS customer_code VARCHAR(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS customer_level VARCHAR(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS contact_name VARCHAR(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS contact_address VARCHAR(255);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bank_account VARCHAR(64);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS tax_no VARCHAR(50);

-- 仓库快照
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS in_warehouse_id BIGINT;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS in_warehouse_name VARCHAR(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS out_warehouse_id BIGINT;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS out_warehouse_name VARCHAR(100);

-- 职员 / 部门快照
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS handler_name VARCHAR(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;

-- 收款 / 信用
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS payment_account VARCHAR(100);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS received_amount NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS prev_advance NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS use_advance NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS available_advance NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS advance_balance NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS receivable_increase NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS credit_limit NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS available_credit NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS prev_debt NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS current_debt NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS debt_balance NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS collection_deadline VARCHAR(50);

-- 金额计算链 / 数量汇总 / 物理属性
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS product_amount NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS settled_amount NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS in_quantity_total NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS out_quantity_total NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS total_weight NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS total_volume NUMERIC(18, 4);

-- 自定义字段
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num1 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num2 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num3 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num4 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_num5 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(255);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(255);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(255);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(255);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(255);

-- 制单人 / 审计字段（实体使用 create_by / update_by，表此前只有 created_by）
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS creator_id BIGINT;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS creator_name VARCHAR(50);
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE erp_sale_exchange ADD COLUMN IF NOT EXISTS update_by BIGINT;

-- 存量数据回填：表原有 created_by / created_by_name → 实体口径列
UPDATE erp_sale_exchange
SET creator_id = created_by,
    create_by  = created_by
WHERE creator_id IS NULL
  AND created_by IS NOT NULL;

UPDATE erp_sale_exchange
SET creator_name = created_by_name
WHERE creator_name IS NULL
  AND created_by_name IS NOT NULL;

-- ------------------------------------------------------------
-- 2. erp_sale_exchange_item（明细）补 68 列
-- ------------------------------------------------------------
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS warehouse_type INT;

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS specification VARCHAR(255);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS model VARCHAR(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS origin VARCHAR(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS brand VARCHAR(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS product_line_attr VARCHAR(50);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS image VARCHAR(500);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS location VARCHAR(100);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS area VARCHAR(100);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS available_stock NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS stock_converted NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS book_stock NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS batch_barcode VARCHAR(64);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS production_date DATE;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS shelf_life INT;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS expiry_date DATE;

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS quantity NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS conversion_rate NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS piece_scatter_qty NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS large_package NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS medium_package NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_package NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS recent_sale_date DATE;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS recent_sale_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS retail_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS wholesale_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS min_sale_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS unit_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS amount NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_unit VARCHAR(30);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_unit_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS small_unit_qty NUMERIC(18, 4);

-- 成本 / 毛利（销售单据查询页「成本金额 / 毛利」列的数据来源）
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS cost_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS cost_amount NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS discount NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS discount_price NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS volume NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS weight NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS is_gift BOOLEAN DEFAULT FALSE;

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level1 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level2 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level3 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level4 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level5 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level6 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level7 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS price_level8 NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num1 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num2 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num3 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num4 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num5 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num6 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num7 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num8 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num9 NUMERIC(18, 4);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_num10 NUMERIC(18, 4);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text1 VARCHAR(255);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text2 VARCHAR(255);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text3 VARCHAR(255);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(255);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(255);
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_text6 VARCHAR(255);

ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_partner BIGINT;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_staff BIGINT;
ALTER TABLE erp_sale_exchange_item ADD COLUMN IF NOT EXISTS ext_dept BIGINT;

-- ------------------------------------------------------------
-- 3. erp_sale_order_item 补自定义字段 1-10（实体已声明、建表遗漏）
-- ------------------------------------------------------------
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field1 NUMERIC(18, 4);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field2 NUMERIC(18, 4);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field3 NUMERIC(18, 4);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field4 VARCHAR(255);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field5 VARCHAR(255);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field6 NUMERIC(18, 4);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field7 NUMERIC(18, 4);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field8 BIGINT;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field9 BIGINT;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS custom_field10 BIGINT;

-- ------------------------------------------------------------
-- 4. 统一销售单据查询索引（4 类单据按日期/状态/客户分页）
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sale_exchange_date_status ON erp_sale_exchange (exchange_date, status);
CREATE INDEX IF NOT EXISTS idx_sale_exchange_no ON erp_sale_exchange (exchange_no);
CREATE INDEX IF NOT EXISTS idx_sale_exchange_customer ON erp_sale_exchange (customer_id);
CREATE INDEX IF NOT EXISTS idx_sale_exchange_item_doc ON erp_sale_exchange_item (exchange_id);
CREATE INDEX IF NOT EXISTS idx_sale_exchange_item_product ON erp_sale_exchange_item (product_id);
CREATE INDEX IF NOT EXISTS idx_sale_order_item_order ON erp_sale_order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_sale_return_doc_item_doc_cost ON erp_sale_return_doc_item (return_doc_id, ref_cost_amount);

COMMENT ON COLUMN erp_sale_exchange.settled_amount IS '已结算金额（统一销售单据查询/按单收款口径）';
COMMENT ON COLUMN erp_sale_exchange.settle_status IS '结算状态';
COMMENT ON COLUMN erp_sale_exchange_item.cost_amount IS '成本金额（销售单据查询「成本金额」列）';
