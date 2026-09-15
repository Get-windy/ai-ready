-- =====================================================
-- V2.0.0: 产品增加SKU、库存增加序列号/SKU、库存管理模式配置
-- =====================================================

-- 1. erp_product 增加 sku 字段
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS sku VARCHAR(100);

COMMENT ON COLUMN erp_product.sku IS 'SKU(库存单位编码)，用于SKU管理模式';
CREATE INDEX IF NOT EXISTS idx_product_sku ON erp_product(tenant_id, sku);

-- 2. erp_stock 增加序列号和SKU字段（用于SERIAL/SKU管理模式）
ALTER TABLE erp_stock
    ADD COLUMN IF NOT EXISTS serial_no VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sku VARCHAR(100);

COMMENT ON COLUMN erp_stock.serial_no IS '序列号（序列号管理模式使用）';
COMMENT ON COLUMN erp_stock.sku IS 'SKU（SKU管理模式使用）';
CREATE INDEX IF NOT EXISTS idx_stock_serial ON erp_stock(tenant_id, serial_no);
CREATE INDEX IF NOT EXISTS idx_stock_sku ON erp_stock(tenant_id, sku);

-- 3. 库存管理模式配置（默认BATCH）
INSERT INTO sys_project_config (tenant_id, config_key, config_value, config_type, config_group, description, status)
VALUES (0, 'inventory.mode', 'BATCH', 'string', 'inventory', '库存管理模式: BATCH-批次管理 SERIAL-序列号管理 SKU-SKU管理', 1)
ON CONFLICT DO NOTHING;
