-- ============================================================
-- V11.69.0: 移库单补来源关联字段
--
-- 背景：
--   移库单（wms_move_task）此前无来源类型/来源单号，无法按来源单据检索。
--   为完善「无来源关联」缺口，新增 source_type / source_no 两列，
--   支持按来源单据（盘点差异/补货/其他）追踪与检索。
-- ============================================================

ALTER TABLE wms_move_task ADD COLUMN IF NOT EXISTS source_type INTEGER DEFAULT 0;
ALTER TABLE wms_move_task ADD COLUMN IF NOT EXISTS source_no VARCHAR(100);
COMMENT ON COLUMN wms_move_task.source_type IS '来源类型 0-手动 1-盘点差异 2-补货 3-其他';
COMMENT ON COLUMN wms_move_task.source_no IS '来源单号';
