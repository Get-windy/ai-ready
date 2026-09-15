-- 会计凭证金标准增强
-- 1. finance_voucher 补充凭证头字段：单据类型/摘要/经手人/部门/来源单据编号/打印次数
-- 2. finance_voucher_item 补充明细科目（辅助核算项）
-- 说明：凭证号 KJPZ- 前缀由前端/后端生成，无新增列。

ALTER TABLE finance_voucher ADD COLUMN IF NOT EXISTS voucher_type varchar(50) DEFAULT 'manual';
ALTER TABLE finance_voucher ADD COLUMN IF NOT EXISTS summary varchar(500);
ALTER TABLE finance_voucher ADD COLUMN IF NOT EXISTS handler_name varchar(255);
ALTER TABLE finance_voucher ADD COLUMN IF NOT EXISTS dept_name varchar(255);
ALTER TABLE finance_voucher ADD COLUMN IF NOT EXISTS source_no varchar(100);
ALTER TABLE finance_voucher ADD COLUMN IF NOT EXISTS print_count int DEFAULT 0;

ALTER TABLE finance_voucher_item ADD COLUMN IF NOT EXISTS detail_subject varchar(255);
