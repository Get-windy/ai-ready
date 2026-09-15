-- =====================================================
-- V8.1.0: 新增商品表单全栈补全
-- - erp_product 补充缺失字段
-- - erp_product_unit 补充定价列
-- - 新建 erp_product_recommend (推荐商品)
-- =====================================================

-- 1. erp_product 补充字段
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS industry_category VARCHAR(100),
    ADD COLUMN IF NOT EXISTS near_expiry_days INTEGER,
    ADD COLUMN IF NOT EXISTS model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS product_code_alias VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_batch_expiry_managed INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_standard_product INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS use_coupon INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS default_sales_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS default_purchase_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS default_stock_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS rich_text_detail TEXT,
    ADD COLUMN IF NOT EXISTS mall_display_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS mall_description TEXT,
    ADD COLUMN IF NOT EXISTS mall_tags VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS mall_shelf_status INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS mall_sort_order INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mall_min_order_qty INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mall_purchase_limit INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);

COMMENT ON COLUMN erp_product.industry_category IS '所属行业类别';
COMMENT ON COLUMN erp_product.near_expiry_days IS '近效期天数';
COMMENT ON COLUMN erp_product.model IS '型号';
COMMENT ON COLUMN erp_product.product_code_alias IS '货号(别名)';
COMMENT ON COLUMN erp_product.is_batch_expiry_managed IS '是否启用保质期/批次号 0=否 1=是';
COMMENT ON COLUMN erp_product.is_standard_product IS '标品认定 1=标品 0=非标品';
COMMENT ON COLUMN erp_product.use_coupon IS '使用优惠券 0=否 1=是';
COMMENT ON COLUMN erp_product.default_sales_unit_id IS '销售常用单位ID';
COMMENT ON COLUMN erp_product.default_purchase_unit_id IS '采购常用单位ID';
COMMENT ON COLUMN erp_product.default_stock_unit_id IS '库存单位ID';
COMMENT ON COLUMN erp_product.rich_text_detail IS '商品详情页富文本(商城)';
COMMENT ON COLUMN erp_product.mall_display_title IS '商城显示标题';
COMMENT ON COLUMN erp_product.mall_description IS '商城商品描述';
COMMENT ON COLUMN erp_product.mall_tags IS '商品标签(逗号分隔)';
COMMENT ON COLUMN erp_product.mall_shelf_status IS '商城上架状态 0=下架 1=上架';
COMMENT ON COLUMN erp_product.mall_sort_order IS '商城排序值';
COMMENT ON COLUMN erp_product.mall_min_order_qty IS '商城起订量';
COMMENT ON COLUMN erp_product.mall_purchase_limit IS '商城限购量(0=不限)';
COMMENT ON COLUMN erp_product.video_url IS '主图视频URL';

-- 2. erp_product_unit 表(如不存在则创建)
CREATE TABLE IF NOT EXISTS erp_product_unit (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL DEFAULT 0,
    product_id        BIGINT NOT NULL,
    unit_name         VARCHAR(50) NOT NULL,
    is_base_unit      INTEGER DEFAULT 0,
    conversion_rate   DECIMAL(20,6) DEFAULT 1.000000,
    barcode           VARCHAR(100),
    sort_order        INTEGER DEFAULT 0,
    deleted           INTEGER DEFAULT 0,
    create_by         BIGINT,
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT,
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_unit IS '产品多单位换算表';
COMMENT ON COLUMN erp_product_unit.unit_name IS '单位名称';
COMMENT ON COLUMN erp_product_unit.is_base_unit IS '是否基础单位 0=否 1=是';
COMMENT ON COLUMN erp_product_unit.conversion_rate IS '换算比率(相对基础单位)';
COMMENT ON COLUMN erp_product_unit.barcode IS '单位条码';
COMMENT ON COLUMN erp_product_unit.sort_order IS '排序号';

-- 2b. erp_product_unit 补充定价列(多场景价格)
ALTER TABLE erp_product_unit
    ADD COLUMN IF NOT EXISTS unit_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS preset_purchase_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS reference_cost DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS recent_purchase_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS wholesale_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS retail_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS min_sale_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS min_discount DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS restaurant_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS canteen_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS outer_restaurant_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS self_vip_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS group_meal_price DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS key_vip_price DECIMAL(20,2);

COMMENT ON COLUMN erp_product_unit.unit_type IS '单位类型: SMALL=小单位 MEDIUM=中单位 LARGE=大单位';
COMMENT ON COLUMN erp_product_unit.preset_purchase_price IS '预设进价';
COMMENT ON COLUMN erp_product_unit.reference_cost IS '参考成本';
COMMENT ON COLUMN erp_product_unit.recent_purchase_price IS '最近进价';
COMMENT ON COLUMN erp_product_unit.wholesale_price IS '批发价';
COMMENT ON COLUMN erp_product_unit.retail_price IS '零售价';
COMMENT ON COLUMN erp_product_unit.min_sale_price IS '最低售价';
COMMENT ON COLUMN erp_product_unit.min_discount IS '最低折扣(%)';
COMMENT ON COLUMN erp_product_unit.restaurant_price IS '餐饮店价格';
COMMENT ON COLUMN erp_product_unit.canteen_price IS '食堂团餐价格';
COMMENT ON COLUMN erp_product_unit.outer_restaurant_price IS '外围餐饮店价格';
COMMENT ON COLUMN erp_product_unit.self_vip_price IS '自助vip价格';
COMMENT ON COLUMN erp_product_unit.group_meal_price IS '大团餐价格';
COMMENT ON COLUMN erp_product_unit.key_vip_price IS '重点vip价格';

-- 3. 推荐商品表(独立表,支持最多12条,有序排列)
CREATE TABLE IF NOT EXISTS erp_product_recommend (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT NOT NULL DEFAULT 0,
    product_id           BIGINT NOT NULL REFERENCES erp_product(id),
    recommend_product_id BIGINT NOT NULL REFERENCES erp_product(id),
    sort_order           INTEGER NOT NULL DEFAULT 0,
    deleted              INTEGER NOT NULL DEFAULT 0,
    create_by            BIGINT,
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by            BIGINT,
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE erp_product_recommend IS '推荐商品关联表';
COMMENT ON COLUMN erp_product_recommend.product_id IS '主商品ID';
COMMENT ON COLUMN erp_product_recommend.recommend_product_id IS '推荐商品ID';
COMMENT ON COLUMN erp_product_recommend.sort_order IS '排序序号(1-12)';

CREATE INDEX IF NOT EXISTS idx_precommend_product ON erp_product_recommend(product_id);
CREATE INDEX IF NOT EXISTS idx_precommend_related ON erp_product_recommend(recommend_product_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_precommend_unique ON erp_product_recommend(product_id, recommend_product_id);
