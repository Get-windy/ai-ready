-- 营销模块：储值卡资金闭环（开卡/充值/消费/退款 → 会计凭证）
--
-- 背景：此前储值卡只落营销台账，**不进总账**——收钱不记预收、消费不结收入，属"资金不闭环"。
-- 业界口径（生产级预付卡记账）：
--   · 开卡/充值（企业收钱、形成负债）：借 1001 库存现金 / 1002 银行存款，贷 2203 预收账款
--   · 消费（负债转收入）：            借 2203 预收账款，贷 6001 主营业务收入
--   · 退款（冲回负债、退钱）：        借 2203 预收账款，贷 1001/1002
--   · **赠送额不进凭证**：赠送不产生现金流；其成本体现为"消费结转收入 > 实收金额"的差额，
--     具体会计处理由财务政策决定（系统只保证实收金额入账），页面与文档均已如实标注。
--
-- 幂等：`mkt_stored_card_flow.voucher_no` 记录已生成凭证号，重试/重复提交时非空即跳过。
ALTER TABLE mkt_stored_card_flow ADD COLUMN IF NOT EXISTS voucher_no VARCHAR(64);
ALTER TABLE mkt_stored_card_flow ADD COLUMN IF NOT EXISTS settle_account VARCHAR(16);

COMMENT ON COLUMN mkt_stored_card_flow.voucher_no IS '已生成的会计凭证号（幂等标记：非空即不再重复生成）';
COMMENT ON COLUMN mkt_stored_card_flow.settle_account IS '结算账户：CASH 库存现金 / BANK 银行存款（开卡·充值·退款用）';

ALTER TABLE mkt_stored_card ADD COLUMN IF NOT EXISTS settle_account VARCHAR(16);

COMMENT ON COLUMN mkt_stored_card.settle_account IS '开卡时的默认结算账户（CASH/BANK），后续充值可在流水上单独指定';
