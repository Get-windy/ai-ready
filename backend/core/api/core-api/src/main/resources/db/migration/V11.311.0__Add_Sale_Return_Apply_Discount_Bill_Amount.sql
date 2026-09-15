-- 销售退货申请「折后金额」列：对齐销售退货单口径（erp_sale_return_doc.discount_bill_amount）
-- 折后金额 = 商品金额 − 优惠金额；本单金额 = 折后金额 + 运费 + 其他费用
-- 背景：《物流退货收货》按单据列表含「金额 / 折后金额 / 本单金额」三列，此前折后金额无真实来源。
ALTER TABLE erp_sale_return ADD COLUMN IF NOT EXISTS discount_bill_amount NUMERIC(18, 4);

COMMENT ON COLUMN erp_sale_return.discount_bill_amount IS '折后金额（商品金额-优惠金额）';
