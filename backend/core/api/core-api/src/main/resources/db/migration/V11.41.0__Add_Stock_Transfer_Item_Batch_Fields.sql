-- 调拨明细：补对标"调拨价系列/批次/包装/条码"列
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS barcode character varying(100);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS shelf_life character varying(50);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS conversion_relation character varying(50);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS piece_quantity numeric(18,4);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS big_pack numeric(18,4);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS mid_pack numeric(18,4);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS small_pack numeric(18,4);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS transfer_price numeric(18,4);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS transfer_amount numeric(18,4);
ALTER TABLE public.erp_stock_transfer_item ADD COLUMN IF NOT EXISTS transfer_diff numeric(18,4);

-- 调拨单主表：补单据日期
ALTER TABLE public.erp_stock_transfer ADD COLUMN IF NOT EXISTS bill_date date;
