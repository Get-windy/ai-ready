-- ============================================================
-- V11.22.0: 采购订单子表补 tenant_id 列
--
-- 背景：V9.10.3 创建的 6 张采购订单子表缺 tenant_id，
-- 实体均有 tenantId，MP 全字段 insert 报"tenant_id 字段不存在"。
-- 回填策略：按 order_id 关联主表回填，孤儿行兜底 1。
-- ============================================================

ALTER TABLE erp_purchase_order_partner_snapshot ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_purchase_order_settlement ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_purchase_order_logistics ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_purchase_order_deposit ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_purchase_order_audit_trail ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
ALTER TABLE erp_purchase_order_ext_info ADD COLUMN IF NOT EXISTS tenant_id BIGINT;

UPDATE erp_purchase_order_partner_snapshot i SET tenant_id = o.tenant_id FROM erp_purchase_order o WHERE i.order_id = o.id AND i.tenant_id IS NULL;
UPDATE erp_purchase_order_settlement i SET tenant_id = o.tenant_id FROM erp_purchase_order o WHERE i.order_id = o.id AND i.tenant_id IS NULL;
UPDATE erp_purchase_order_logistics i SET tenant_id = o.tenant_id FROM erp_purchase_order o WHERE i.order_id = o.id AND i.tenant_id IS NULL;
UPDATE erp_purchase_order_deposit i SET tenant_id = o.tenant_id FROM erp_purchase_order o WHERE i.order_id = o.id AND i.tenant_id IS NULL;
UPDATE erp_purchase_order_audit_trail i SET tenant_id = o.tenant_id FROM erp_purchase_order o WHERE i.order_id = o.id AND i.tenant_id IS NULL;
UPDATE erp_purchase_order_ext_info i SET tenant_id = o.tenant_id FROM erp_purchase_order o WHERE i.order_id = o.id AND i.tenant_id IS NULL;

UPDATE erp_purchase_order_partner_snapshot SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE erp_purchase_order_settlement SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE erp_purchase_order_logistics SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE erp_purchase_order_deposit SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE erp_purchase_order_audit_trail SET tenant_id = 1 WHERE tenant_id IS NULL;
UPDATE erp_purchase_order_ext_info SET tenant_id = 1 WHERE tenant_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_po_snapshot_tenant ON erp_purchase_order_partner_snapshot(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_settlement_tenant ON erp_purchase_order_settlement(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_logistics_tenant ON erp_purchase_order_logistics(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_deposit_tenant ON erp_purchase_order_deposit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_audit_trail_tenant ON erp_purchase_order_audit_trail(tenant_id);
CREATE INDEX IF NOT EXISTS idx_po_ext_info_tenant ON erp_purchase_order_ext_info(tenant_id);
