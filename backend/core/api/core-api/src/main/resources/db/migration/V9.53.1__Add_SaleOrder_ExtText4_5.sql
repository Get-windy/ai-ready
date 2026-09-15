-- 销售订单扩展字段 extText4/extText5
-- 对应前端的自定义搜索字段，与 extText1-3 同模式 (VARCHAR(500))
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_text4 VARCHAR(500) DEFAULT NULL;
ALTER TABLE erp_sale_order ADD COLUMN IF NOT EXISTS ext_text5 VARCHAR(500) DEFAULT NULL;

COMMENT ON COLUMN erp_sale_order.ext_text4 IS '自定义字段6(文本)';
COMMENT ON COLUMN erp_sale_order.ext_text5 IS '自定义字段7(文本)';
