-- Add frozen_quantity column to erp_stock table
ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS frozen_quantity DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_stock.frozen_quantity IS '冻结数量';
