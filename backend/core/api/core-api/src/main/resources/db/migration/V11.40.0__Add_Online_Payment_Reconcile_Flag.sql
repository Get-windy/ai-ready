-- 在线支付对账：资金流水增加对账标记（0-未对账 1-已对账）
ALTER TABLE public.erp_capital_flow ADD COLUMN IF NOT EXISTS reconcile_flag integer NOT NULL DEFAULT 0;

-- 对账人（非必须，便于追溯）
ALTER TABLE public.erp_capital_flow ADD COLUMN IF NOT EXISTS reconcile_by character varying(50);
ALTER TABLE public.erp_capital_flow ADD COLUMN IF NOT EXISTS reconcile_at timestamp without time zone;
