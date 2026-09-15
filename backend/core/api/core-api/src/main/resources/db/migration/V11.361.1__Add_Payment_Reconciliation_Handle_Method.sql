-- ============================================================================
-- 每日对账 · 差异处理方式落库
-- 背景：开发文档《每日对账开发文档》待完善 #3 —— 前端「处理差异」提交 {method, remark}，
--       后端 handleDifference(id, remark) 只收 remark，处理方式（手工调账/忽略差异/重新对账）未落库。
-- 说明：新增列 payment_reconciliation.handle_method，值域 MANUAL / IGNORE / REPROCESS（对应前端 method）。
-- ============================================================================

ALTER TABLE payment_reconciliation
    ADD COLUMN IF NOT EXISTS handle_method VARCHAR(20);

COMMENT ON COLUMN payment_reconciliation.handle_method IS '差异处理方式: MANUAL手工调账, IGNORE忽略差异, REPROCESS重新对账';
