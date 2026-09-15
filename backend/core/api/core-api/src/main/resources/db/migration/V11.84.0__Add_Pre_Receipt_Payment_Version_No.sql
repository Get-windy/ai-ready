-- ============================================================
-- V11.84.0: 补预收款单/预付款单 version_no 列
--
-- 背景：PreReceipt/PrePayment 实体（金标准改造）含 @Version versionNo 字段，
--   MyBatisPlus 生成 SELECT 时会引用该列；但 erp_pre_receipt / erp_pre_payment
--   建表 SQL 未提供 version_no 列，导致 next-no 等查询报
--   "字段 version_no 不存在"（BadSqlGrammarException 500）。
--   此处按 PreReceiptItem 设表范式补列（version_no INT，乐观锁）。
-- ============================================================

ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS version_no INT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS version_no INT;

COMMENT ON COLUMN erp_pre_receipt.version_no IS '版本号(乐观锁)';
COMMENT ON COLUMN erp_pre_payment.version_no IS '版本号(乐观锁)';
