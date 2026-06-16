-- V6.11.0: Fix erp_business_metric.id sequence and add missing columns to sys_print_chain_item

-- 1. Fix erp_business_metric: id column lacks auto-increment sequence
CREATE SEQUENCE IF NOT EXISTS erp_business_metric_id_seq;
ALTER TABLE erp_business_metric ALTER COLUMN id SET DEFAULT nextval('erp_business_metric_id_seq');
SELECT setval('erp_business_metric_id_seq', COALESCE((SELECT MAX(id) FROM erp_business_metric), 0) + 1, false);

-- 2. Add missing columns to sys_print_chain_item
ALTER TABLE sys_print_chain_item ADD COLUMN IF NOT EXISTS screenshot_mode VARCHAR(20) NOT NULL DEFAULT 'DISABLED';
ALTER TABLE sys_print_chain_item ADD COLUMN IF NOT EXISTS screenshot_confirm_timeout INTEGER DEFAULT 300;
COMMENT ON COLUMN sys_print_chain_item.screenshot_mode IS '截图模式: DISABLED / MANUAL_CONFIRM / AUTO_CONFIRM';
COMMENT ON COLUMN sys_print_chain_item.screenshot_confirm_timeout IS '截图确认超时时间(秒)';
