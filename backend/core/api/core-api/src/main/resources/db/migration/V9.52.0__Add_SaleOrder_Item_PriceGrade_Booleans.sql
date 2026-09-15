-- V9.52.0: 为销售订单明细表增加8个标准化价格等级布尔字段
-- 这些字段对应用户自定义的价格等级昵称（餐饮店/食堂团餐/自助vip/大团餐/特价客户/外围餐饮店/重点vip01/连锁vip）
-- 实际业务含义为标准化产品价格等级，通过客户等级关联

ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS restaurant BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS canteen BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS vip_self BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS large_group BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS special_customer BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS out_restaurant BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS vip_level1 BOOLEAN DEFAULT FALSE;
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS vip_level2 BOOLEAN DEFAULT FALSE;
