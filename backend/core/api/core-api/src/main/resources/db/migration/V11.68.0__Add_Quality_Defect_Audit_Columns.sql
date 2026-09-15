-- ───────────────────────────────────────────────────────────
-- V11.68.0 缺陷记录审计列补齐
-- quality_defect_handle 实体继承 BaseEntity（含 createBy/updateBy/
-- version 字段，由 MetaObjectHandler 自动填充），但 V8.13.0 建表
-- 时缺少这三列，导致 insert 生成的 SQL 引用不存在的列而报
-- BadSqlGrammar。补齐以与其它业务表（如 wms_event_outbox）及
-- BaseEntity 保持一致。
-- ───────────────────────────────────────────────────────────
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS create_by BIGINT;
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS update_by BIGINT;
ALTER TABLE quality_defect_handle ADD COLUMN IF NOT EXISTS version INT;

COMMENT ON COLUMN quality_defect_handle.create_by IS '创建人ID';
COMMENT ON COLUMN quality_defect_handle.update_by IS '更新人ID';
COMMENT ON COLUMN quality_defect_handle.version IS '乐观锁版本号';
