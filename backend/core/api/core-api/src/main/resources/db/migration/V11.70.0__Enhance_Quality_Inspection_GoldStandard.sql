-- ============================================================
-- V11.69.0: 质检单金标准升级（对齐报损/报溢单）
-- 补主表列：质检单号 quality_no、单据状态 status、制单人 creator_name，
-- 以及 BaseEntity 审计列 create_by/update_by/version（V8.13.0 建表缺失，
-- 否则 MyBatis-Plus INSERT/UPDATE 因填充字段触发 BadSqlGrammar）。
-- ============================================================

ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS quality_no VARCHAR(64);
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS status INT DEFAULT 0;
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE quality_inspection ADD COLUMN IF NOT EXISTS version INT DEFAULT 0;

COMMENT ON COLUMN quality_inspection.quality_no IS '质检单号';
COMMENT ON COLUMN quality_inspection.status IS '单据状态: 0待检, 1已完成, 2已作废';
COMMENT ON COLUMN quality_inspection.creator_name IS '制单人姓名';

CREATE INDEX IF NOT EXISTS idx_quality_inspection_no ON quality_inspection(quality_no);
CREATE INDEX IF NOT EXISTS idx_quality_inspection_status ON quality_inspection(tenant_id, status);
