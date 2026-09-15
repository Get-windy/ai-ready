-- V11.71.0: 质检单补仓库维度 warehouse_id/warehouse_name
-- 前置：WMS 冻结/放行监听器需按 仓库+批次+数量 锁定库存，质检单必须先有仓库维度
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(100);

COMMENT ON COLUMN quality_inspection.warehouse_id IS '仓库ID(质检/冻结放行维度)';
COMMENT ON COLUMN quality_inspection.warehouse_name IS '仓库名称';

CREATE INDEX IF NOT EXISTS idx_quality_inspection_warehouse ON quality_inspection(tenant_id, warehouse_id);
