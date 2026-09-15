-- ============================================================
-- V11.35.0: erp_cost_sharing_item / erp_sale_order_item 补 tenant_id 列
--
-- 背景：
--   MyBatis-Plus 多租户插件自动向 INSERT 追加 tenant_id 字段，
--   但 erp_cost_sharing_item / erp_sale_order_item 建表时缺少此列，
--   create 报 "tenant_id 字段不存在" 500 错。
-- ============================================================

ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
