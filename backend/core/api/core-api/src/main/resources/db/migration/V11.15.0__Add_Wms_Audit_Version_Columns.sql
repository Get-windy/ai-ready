-- ============================================================
-- V11.15.0: WMS 域批量补齐实体审计列与乐观锁列
--
-- 背景：
--   WMS 模块实体统一继承含 create_by/update_by/@Version 的 BaseEntity，
--   下列表为旧版结构缺列，insert/update 时报"字段不存在"。
--   - wms_inventory_log：缺 create_by/update_by/version（借进审批库存日志实测暴露）
--   - wms_check_result / wms_*_detail：缺 version
-- ============================================================

ALTER TABLE wms_inventory_log ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE wms_inventory_log ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE wms_inventory_log ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;

ALTER TABLE wms_check_result ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_receipt_detail ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_putaway_detail ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_pick_detail ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_ship_detail ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE wms_move_detail ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
