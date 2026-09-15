-- ============================================================
-- V11.16.0: 4 张缺 tenant_id 的表补列并回填（消除 @InterceptorIgnore 规避点）
--
-- 背景：
--   全局 TenantLineInnerInterceptor 自动注入 tenant_id 条件，但下列表无该列，
--   查询直接 SQL 报错，此前靠多处 @InterceptorIgnore(tenantLine="true") 规避：
--   - erp_purchase_order_item  ← erp_purchase_order (order_id)
--   - erp_stock_damage_item    ← erp_stock_damage (damage_id)
--   - erp_stock_overflow_item  ← erp_stock_overflow (overflow_id)
--   - biz_party_follow         （无主表，系统当前仅租户 1，全部回填 1）
-- 孤儿明细（主表缺失）统一回填 tenant_id=1，避免 NULL 行对租户1不可见。
-- ============================================================

ALTER TABLE erp_purchase_order_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_stock_damage_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_stock_overflow_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE biz_party_follow ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

UPDATE erp_purchase_order_item i
SET tenant_id = po.tenant_id
FROM erp_purchase_order po
WHERE i.order_id = po.id AND i.tenant_id IS NULL;
UPDATE erp_purchase_order_item SET tenant_id = 1 WHERE tenant_id IS NULL;

UPDATE erp_stock_damage_item i
SET tenant_id = d.tenant_id
FROM erp_stock_damage d
WHERE i.damage_id = d.id AND i.tenant_id IS NULL;
UPDATE erp_stock_damage_item SET tenant_id = 1 WHERE tenant_id IS NULL;

UPDATE erp_stock_overflow_item i
SET tenant_id = o.tenant_id
FROM erp_stock_overflow o
WHERE i.overflow_id = o.id AND i.tenant_id IS NULL;
UPDATE erp_stock_overflow_item SET tenant_id = 1 WHERE tenant_id IS NULL;

UPDATE biz_party_follow SET tenant_id = 1 WHERE tenant_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_erp_purchase_order_item_tenant ON erp_purchase_order_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_erp_stock_damage_item_tenant ON erp_stock_damage_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_erp_stock_overflow_item_tenant ON erp_stock_overflow_item (tenant_id);
CREATE INDEX IF NOT EXISTS idx_biz_party_follow_tenant ON biz_party_follow (tenant_id);
