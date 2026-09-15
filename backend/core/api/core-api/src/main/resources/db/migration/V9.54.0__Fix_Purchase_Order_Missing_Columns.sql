-- 修复 erp_purchase_order 表与 PurchaseOrder 实体字段不匹配问题
-- 问题：实体 PurchaseOrder 定义了 purchase_order_no 等字段，但数据库表缺少对应列
-- 错误：org.postgresql.util.PSQLException: 错误: 字段 "purchase_order_no" 不存在

-- 添加实体中存在但表中缺失的字段
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS purchase_order_no VARCHAR(50);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS buyer_id BIGINT;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS buyer_name VARCHAR(100);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS purchase_type INTEGER;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS source INTEGER;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS final_amount DECIMAL(18,2);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS currency VARCHAR(10);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS expected_delivery_date TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS actual_delivery_date TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS payment_terms INTEGER;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS payment_terms_desc VARCHAR(200);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS payment_due_date TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS incoterms VARCHAR(20);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS shipping_method VARCHAR(50);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS approval_level INTEGER;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS current_approver_id BIGINT;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS current_approver_name VARCHAR(100);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS submit_for_approval_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS approval_complete_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS supplier_confirmation_no VARCHAR(100);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS supplier_confirmation_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS supplier_confirmation_remark VARCHAR(500);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS tracking_no VARCHAR(100);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(100);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS estimated_arrival_date TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS actual_arrival_date TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS quality_check_result VARCHAR(500);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS invoice_no VARCHAR(100);
-- paid_amount 已存在于表中，无需添加
-- received_quantity 已存在于表中
-- received_amount 已存在于表中
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS payment_complete_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS order_complete_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS cancel_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS close_reason VARCHAR(500);
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS close_time TIMESTAMP;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

-- 为关键字段添加索引
CREATE INDEX IF NOT EXISTS idx_purchase_order_purchase_no ON erp_purchase_order(purchase_order_no);
CREATE INDEX IF NOT EXISTS idx_purchase_order_buyer ON erp_purchase_order(buyer_id);
