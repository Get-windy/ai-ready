-- V10.0.1: 预订货单明细表添加计价单位字段(pricing_unit)
-- 修复pricingUnit数据被unit覆盖的问题

ALTER TABLE erp_sale_pre_order_item ADD COLUMN pricing_unit VARCHAR(50);

COMMENT ON COLUMN erp_sale_pre_order_item.pricing_unit IS '计价单位';
