-- ───────────────────────────────────────────────────────────
-- V11.77.0 缺陷 8D/CAPA 与多步处置剩余量
-- 缺陷主表/处理历史补 纠正措施 corrective_action、预防措施 preventive_action
-- 支撑 CAPA 闭环；剩余量校验在 Service 层基于处理历史累加实现（无需表变更）。
-- ───────────────────────────────────────────────────────────
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS corrective_action VARCHAR(1000);
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS preventive_action VARCHAR(1000);
ALTER TABLE quality_defect_handle_history ADD COLUMN IF NOT EXISTS corrective_action VARCHAR(1000);
ALTER TABLE quality_defect_handle_history ADD COLUMN IF NOT EXISTS preventive_action VARCHAR(1000);

COMMENT ON COLUMN quality_defect_handle.corrective_action IS '纠正措施(CAPA)';
COMMENT ON COLUMN quality_defect_handle.preventive_action IS '预防措施(CAPA)';
COMMENT ON COLUMN quality_defect_handle_history.corrective_action IS '该步处置的纠正措施';
COMMENT ON COLUMN quality_defect_handle_history.preventive_action IS '该步处置的预防措施';
