-- ============================================================
-- V11.69.0: 收货差异处理 —— 明细补破损数量列
--
-- 业务：超收/短收/破损 记录差异。破损数量不入库（净入库 = 实收 - 破损），
--       超收(received>expected)/短收(received<expected) 由差异推导，前端展示。
-- ============================================================

ALTER TABLE wms_receipt_detail ADD COLUMN IF NOT EXISTS broken_quantity DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN wms_receipt_detail.broken_quantity IS '破损数量（不入库，需红冲/调整）';
