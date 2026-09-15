-- 明细账金标准增强
-- 1. finance_voucher_item 补「对账标记」——账簿逐笔明细可标记对账状态并按此过滤
-- 2. finance_voucher_item 补核算项三列（核算单位/核算部门/核算职员）——科目挂核算项时按核算项列示，
--    保证往来明细可查（对应开发文档 P1 核算项）。
-- 说明：单据日期取自 finance_voucher.voucher_date，记账日期取自 finance_voucher.post_at，
--       单据编号取自 finance_voucher.voucher_no，均为联查字段，不新增冗余列。

ALTER TABLE finance_voucher_item ADD COLUMN IF NOT EXISTS reconcile_flag smallint DEFAULT 0;
ALTER TABLE finance_voucher_item ADD COLUMN IF NOT EXISTS aux_unit varchar(255);
ALTER TABLE finance_voucher_item ADD COLUMN IF NOT EXISTS aux_dept varchar(255);
ALTER TABLE finance_voucher_item ADD COLUMN IF NOT EXISTS aux_staff varchar(255);

COMMENT ON COLUMN finance_voucher_item.reconcile_flag IS '对账标记 0-未对账 1-已对账';
COMMENT ON COLUMN finance_voucher_item.aux_unit IS '核算单位（往来单位核算项）';
COMMENT ON COLUMN finance_voucher_item.aux_dept IS '核算部门';
COMMENT ON COLUMN finance_voucher_item.aux_staff IS '核算职员';

CREATE INDEX IF NOT EXISTS idx_fvi_reconcile_flag ON finance_voucher_item (reconcile_flag);
CREATE INDEX IF NOT EXISTS idx_fvi_subject_code ON finance_voucher_item (subject_code);
