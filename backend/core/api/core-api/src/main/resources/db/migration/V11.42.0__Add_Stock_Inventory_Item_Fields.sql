-- 报损明细：补批次/包装/换算/货位/条码列
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS barcode character varying(100);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS location character varying(100);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS shelf_life character varying(50);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS validity_date date;
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS conversion_relation character varying(50);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS piece_quantity numeric(18,4);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS big_pack numeric(18,4);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS mid_pack numeric(18,4);
ALTER TABLE public.erp_stock_damage_item ADD COLUMN IF NOT EXISTS small_pack numeric(18,4);

-- 报溢明细：补批次/包装/换算/货位/条码列
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS barcode character varying(100);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS location character varying(100);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS shelf_life character varying(50);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS validity_date date;
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS conversion_relation character varying(50);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS piece_quantity numeric(18,4);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS big_pack numeric(18,4);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS mid_pack numeric(18,4);
ALTER TABLE public.erp_stock_overflow_item ADD COLUMN IF NOT EXISTS small_pack numeric(18,4);

-- 成本调价明细：补换算/价格/金额/批次/货位列
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS barcode character varying(100);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS location character varying(100);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS shelf_life character varying(50);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS validity_date date;
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS conversion_relation character varying(50);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS conversion_result character varying(50);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS piece_quantity numeric(18,4);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS wholesale_price numeric(18,4);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS retail_price numeric(18,4);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS old_amount numeric(18,4);
ALTER TABLE public.erp_stock_cost_adjust_item ADD COLUMN IF NOT EXISTS new_amount numeric(18,4);
