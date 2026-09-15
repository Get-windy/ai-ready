-- V9.51.0: 补充销售订单主表缺失字段
-- 这些字段在 Service/DTO 层已引用但未在数据库中创建

ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(100);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS receiver_phone VARCHAR(50);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS shipping_address VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS order_remark VARCHAR(500);
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS shipping_fee DECIMAL(18,4) DEFAULT 0;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(200);

-- 索引
CREATE INDEX IF NOT EXISTS idx_so_customer_name ON erp_sale_order(customer_name);
CREATE INDEX IF NOT EXISTS idx_so_receiver_name ON erp_sale_order(receiver_name);
