-- 营销模块 → 商品促销（80312）/ 整单促销（80313）/ 特价（80314）/ 套餐（80315）/ 商城预售（80322）字段补全
--
-- 一、促销活动：复用既有 erp_promotion_activity（销售域 PromotionController + 分析域 SaleAnalysisMapper 同表），
--     不另建营销域促销表（同类型业务表全系统只此一张）。
--     对标 13 列 → 本表映射：
--       活动名称=name / 起始=start_time / 结束=end_time / 促销方式=type（PRODUCT 商品促销 / ORDER 整单促销 / SPECIAL_PRICE 特价）
--       促销商品=product_ids / 促销客户=customer_levels（显示串）+ customer_ids（id 串）/ 促销规则=description
--       组合促销·促销类型·促销模式·使用范围·制单人 为本次新增列
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS combo_promo INTEGER;
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS promo_type VARCHAR(64);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS promo_mode VARCHAR(64);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS promo_scope VARCHAR(32);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS customer_ids VARCHAR(1000);
ALTER TABLE erp_promotion_activity ADD COLUMN IF NOT EXISTS creator_name VARCHAR(64);

COMMENT ON COLUMN erp_promotion_activity.combo_promo IS '组合促销：1 是 / 0 否';
COMMENT ON COLUMN erp_promotion_activity.promo_type IS '促销类型：按商品数量 / 按商品金额 / 按客户等级 …';
COMMENT ON COLUMN erp_promotion_activity.promo_mode IS '促销模式：满赠 / 满减 / 打折 / 特价 …';
COMMENT ON COLUMN erp_promotion_activity.promo_scope IS '使用范围：线下使用 / 线上线下 / 商城使用';
COMMENT ON COLUMN erp_promotion_activity.customer_ids IS '促销客户 id 串（逗号分隔，用于「查看客户」）';
COMMENT ON COLUMN erp_promotion_activity.creator_name IS '制单人姓名（列表展示用）';

-- 二、套餐（对标「套餐」页：图片 / 套餐名称 / 套餐编号 / 套餐金额 / 套餐条码 / 捆绑销售 / 商品明细）
--     主体复用既有 erp_product_kit（kit_code/kit_name/kit_price + erp_product_kit_item 明细），仅补 3 列
ALTER TABLE erp_product_kit ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE erp_product_kit ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);
ALTER TABLE erp_product_kit ADD COLUMN IF NOT EXISTS bundle_sales INTEGER;

COMMENT ON COLUMN erp_product_kit.image_url IS '套餐图片（对标「图片」列）';
COMMENT ON COLUMN erp_product_kit.barcode IS '套餐条码（对标「套餐条码」列）';
COMMENT ON COLUMN erp_product_kit.bundle_sales IS '捆绑销售：1 是 / 0 否（对标「捆绑销售」列）';

-- 三、商城预售（对标「商品预售」Tab 12 列：活动名称/商品名称/商品图片/货号/规格/型号/预售价/起始/结束/是否支付订金/活动状态/创建人）
--     主体复用既有 mkt_presale，仅补 5 列
ALTER TABLE mkt_presale ADD COLUMN IF NOT EXISTS product_image VARCHAR(500);
ALTER TABLE mkt_presale ADD COLUMN IF NOT EXISTS product_spec VARCHAR(128);
ALTER TABLE mkt_presale ADD COLUMN IF NOT EXISTS product_model VARCHAR(128);
ALTER TABLE mkt_presale ADD COLUMN IF NOT EXISTS presale_price NUMERIC(18, 2);
ALTER TABLE mkt_presale ADD COLUMN IF NOT EXISTS deposit_required INTEGER;

COMMENT ON COLUMN mkt_presale.product_image IS '商品图片 URL';
COMMENT ON COLUMN mkt_presale.product_spec IS '规格';
COMMENT ON COLUMN mkt_presale.product_model IS '型号';
COMMENT ON COLUMN mkt_presale.presale_price IS '预售价';
COMMENT ON COLUMN mkt_presale.deposit_required IS '是否支付订金：1 是 / 0 否';
