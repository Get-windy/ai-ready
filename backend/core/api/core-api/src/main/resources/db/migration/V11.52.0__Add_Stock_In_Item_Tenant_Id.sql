-- ═══════════════════════════════════════════════════════════════════
-- V11.52.0 其他入库/出库明细增加租户列
-- 多租户插件 TenantLineInnerInterceptor 会在 INSERT 时自动注入 tenant_id，
-- erp_stock_in_item / erp_stock_out_item 建表时遗漏该列，此处补齐。
-- ═══════════════════════════════════════════════════════════════════

ALTER TABLE erp_stock_in_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE erp_stock_out_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
