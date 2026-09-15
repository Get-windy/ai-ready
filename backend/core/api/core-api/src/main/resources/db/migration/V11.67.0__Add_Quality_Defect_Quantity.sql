-- ───────────────────────────────────────────────────────────
-- V11.66.0 缺陷记录页对齐
-- quality_defect_handle 补「缺陷数量 defect_quantity」列（缺陷
-- 记录自身的属性，与处置时的「处理数量 handle_quantity」独立），
-- 并补「来源单号 biz_no」列（缺陷创建时带出质检单的业务单号），
-- 同时更新处理方式列注释补充 CONCESSION（让步接收）。
-- 消除前端缺陷数量/来源单号列表读取空缺、处置弹窗「处理数量」
-- 默认值取错，以及 create 缺陷数量错写入 handle_quantity 的错位。
-- ───────────────────────────────────────────────────────────
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS defect_quantity DECIMAL(12,2);
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS biz_no VARCHAR(50);

COMMENT ON COLUMN quality_defect_handle.defect_quantity IS '缺陷数量';
COMMENT ON COLUMN quality_defect_handle.biz_no IS '来源单号(质检单的业务单号)';
COMMENT ON COLUMN quality_defect_handle.handle_type IS '处理方式: RETURN退货, CONCESSION让步接收, REWORK返工, SCRAP报废, SPECIAL_RELEASE特采';
