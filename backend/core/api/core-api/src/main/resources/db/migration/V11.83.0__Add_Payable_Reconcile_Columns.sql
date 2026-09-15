-- 按单付款：应付(按单付款核销工作台)追加对账标记字段
-- 对账(√/否) 标记与供应商对账，记录最后对账标记时间/人
ALTER TABLE finance_payable ADD COLUMN IF NOT EXISTS reconcile_flag SMALLINT DEFAULT 0;
COMMENT ON COLUMN finance_payable.reconcile_flag IS '对账标记：0-否 1-√';
ALTER TABLE finance_payable ADD COLUMN IF NOT EXISTS reconcile_by_name VARCHAR(255);
COMMENT ON COLUMN finance_payable.reconcile_by_name IS '最后对账标记人';
ALTER TABLE finance_payable ADD COLUMN IF NOT EXISTS reconcile_at TIMESTAMP;
COMMENT ON COLUMN finance_payable.reconcile_at IS '最后对账标记时间';
