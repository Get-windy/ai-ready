-- ============================================================
-- V10.2.0: Add delivery_driver to erp_sale_outbound
-- 补充销售出库配送司机字段
-- ============================================================

ALTER TABLE erp_sale_outbound
    ADD COLUMN IF NOT EXISTS delivery_driver VARCHAR(100);
