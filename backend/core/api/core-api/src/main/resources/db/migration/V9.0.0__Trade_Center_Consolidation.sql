-- =====================================================
-- V9.0.0: 交易中心数据整合 - 消除 B2B 商城数据冗余
-- =====================================================
-- 目标:
-- 1. mall_product → 由 erp_product 直接提供,不再维护独立冗余
-- 2. mall_order/mall_order_item → 由 erp_sale_order 替代(order_source=2)
-- 3. 保留 mall 表用于历史数据,新增数据写入 erp_* 表
-- =====================================================

-- =====================================================
-- Part 1: erp_product 补全商城所需字段
-- =====================================================
ALTER TABLE erp_product
    ADD COLUMN IF NOT EXISTS mall_sales_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mall_category_id BIGINT,
    ADD COLUMN IF NOT EXISTS mall_category_name VARCHAR(100);

COMMENT ON COLUMN erp_product.mall_sales_count IS '商城累计销量';
COMMENT ON COLUMN erp_product.mall_category_id IS '商城分类ID';
COMMENT ON COLUMN erp_product.mall_category_name IS '商城分类名称';

-- =====================================================
-- Part 2: 同步已有 mall_product 数据到 erp_product
-- =====================================================
UPDATE erp_product ep
SET
    mall_sales_count   = COALESCE(mp.sales_count, 0),
    mall_category_id   = NULLIF(mp.category_id, '')::BIGINT,
    mall_category_name = mp.category_name
FROM mall_product mp
WHERE ep.product_code = mp.product_id
  AND mp.deleted = 0;

-- 同步未关联 mall_product 的 erp_product 的 mall_shelf_status
-- 默认已有数据视为上架
UPDATE erp_product
SET mall_shelf_status = COALESCE(mall_shelf_status, 1)
WHERE mall_shelf_status IS NULL;

-- =====================================================
-- Part 3: 创建 v_mall_product 视图(向后兼容)
-- =====================================================
-- 该视图模拟 mall_product 的结构,数据来源为 erp_product + erp_stock
-- 供 B2B 商城前端 API 查询使用,避免改动物品层代码
CREATE OR REPLACE VIEW v_mall_product AS
SELECT
    ep.id               AS id,
    ep.tenant_id        AS tenant_id,
    ep.product_code     AS product_id,       -- 对应 mall_product.product_id
    ep.product_code     AS product_code,
    ep.product_name     AS product_name,
    ep.image_url        AS image_url,
    COALESCE(ep.retail_price, ep.standard_price, 0) AS sale_price,
    COALESCE(ep.wholesale_price, 0)          AS market_price,
    COALESCE(st.available_qty, 0)::INTEGER   AS stock_quantity,
    ep.mall_category_id::VARCHAR(50)         AS category_id,
    ep.mall_category_name                    AS category_name,
    CASE WHEN ep.mall_shelf_status = 1
         THEN 'ON_SHELF' ELSE 'INACTIVE'
    END                                      AS status,
    COALESCE(ep.mall_description, '')        AS description,
    ep.mall_sales_count                      AS sales_count,
    ep.deleted                               AS deleted,
    ep.create_by                             AS create_by,
    ep.create_time                           AS create_time,
    ep.update_by                             AS update_by,
    ep.update_time                           AS update_time
FROM erp_product ep
LEFT JOIN LATERAL (
    SELECT
        COALESCE(SUM(s.quantity - COALESCE(s.frozen_quantity, 0)), 0) AS available_qty
    FROM erp_stock s
    WHERE s.product_id = ep.id
      AND s.deleted = 0
) st ON true
WHERE ep.deleted = 0;

COMMENT ON VIEW v_mall_product IS '商城商品视图(替代 mall_product 表,数据源为 erp_product+erp_stock)';

-- =====================================================
-- Part 4: 创建商城缺省分类表
-- =====================================================
-- 原 mall_product 的分类是字符串类型,现建立商城专用分类体系
-- 与 ERP 产品分类(erp_product_category)可关联但不强制
CREATE TABLE IF NOT EXISTS mall_category (
    id              BIGINT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    category_name   VARCHAR(100) NOT NULL,
    parent_id       BIGINT DEFAULT 0,
    sort_order      INTEGER DEFAULT 0,
    icon_url        VARCHAR(500),
    status          INTEGER DEFAULT 1,
    deleted         INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE mall_category IS '商城商品分类';
COMMENT ON COLUMN mall_category.category_name IS '分类名称';
COMMENT ON COLUMN mall_category.parent_id IS '父分类ID(0=顶级)';
COMMENT ON COLUMN mall_category.sort_order IS '排序';
COMMENT ON COLUMN mall_category.icon_url IS '分类图标';

CREATE INDEX IF NOT EXISTS idx_mall_cat_tenant ON mall_category(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mall_cat_parent ON mall_category(parent_id);

-- =====================================================
-- Part 5: erp_sale_order 补齐商城订单所需字段
-- =====================================================
-- order_source, payment_method, payment_status, delivery_status, consignee, consignee_phone
-- 已在 V8.11.0 中添加,此处仅补充商城特定字段
ALTER TABLE erp_sale_order
    ADD COLUMN IF NOT EXISTS consignee_address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(500);

COMMENT ON COLUMN erp_sale_order.consignee_address IS '收货详细地址';
COMMENT ON COLUMN erp_sale_order.buyer_remark IS '买家备注';

-- =====================================================
-- Part 6: 创建同步触发器,保持 erp_product ↔ mall_product 一致
-- =====================================================

-- 6a. 当 erp_product 更新时,同步 mall_product
CREATE OR REPLACE FUNCTION sync_erp_product_to_mall()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO mall_product (
        id, tenant_id, product_id, product_code, product_name,
        image_url, sale_price, market_price, stock_quantity,
        category_id, category_name, status, description, sales_count,
        deleted, create_by, create_time, update_by, update_time
    ) VALUES (
        NEW.id, COALESCE(NEW.tenant_id, 0), NEW.product_code, NEW.product_code, NEW.product_name,
        NEW.image_url,
        COALESCE(NEW.retail_price, NEW.standard_price, 0),
        COALESCE(NEW.wholesale_price, 0),
        0,
        NEW.mall_category_id::VARCHAR(50),
        NEW.mall_category_name,
        CASE WHEN COALESCE(NEW.mall_shelf_status, 1) = 1 THEN 'ON_SHELF' ELSE 'INACTIVE' END,
        COALESCE(NEW.mall_description, ''),
        NEW.mall_sales_count,
        NEW.deleted, NEW.create_by::BIGINT, NEW.create_time, NEW.update_by::BIGINT, NEW.update_time
    )
    ON CONFLICT (id) DO UPDATE SET
        product_name    = EXCLUDED.product_name,
        image_url       = EXCLUDED.image_url,
        sale_price      = EXCLUDED.sale_price,
        market_price    = EXCLUDED.market_price,
        category_id     = EXCLUDED.category_id,
        category_name   = EXCLUDED.category_name,
        status          = EXCLUDED.status,
        description     = EXCLUDED.description,
        sales_count     = EXCLUDED.sales_count,
        product_code    = EXCLUDED.product_code,
        product_id      = EXCLUDED.product_id,
        update_by       = EXCLUDED.update_by,
        update_time     = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 创建触发器(仅在不存在时)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_erp_product_sync_mall') THEN
        CREATE TRIGGER trg_erp_product_sync_mall
        AFTER INSERT OR UPDATE ON erp_product
        FOR EACH ROW EXECUTE FUNCTION sync_erp_product_to_mall();
    END IF;
END;
$$;

-- 6b. 当 erp_product.deleted 更新时,同步到 mall_product
-- （利用通用触发器的 ON CONFLICT DO UPDATE 处理软删除同步）

-- =====================================================
-- Part 7: 创建索引优化视图查询性能
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_erp_product_mall_shelf ON erp_product(mall_shelf_status);
CREATE INDEX IF NOT EXISTS idx_erp_product_mall_category ON erp_product(mall_category_id);

-- =====================================================
-- Part 8: 初始触发同步——已有数据同步到 mall_product
-- =====================================================
-- 触发器不会对已有数据生效,此处手动触发一次
UPDATE erp_product SET update_time = NOW() WHERE update_time IS NULL;
-- 再次执行确保所有产品同步
INSERT INTO mall_product (
    id, tenant_id, product_id, product_code, product_name,
    image_url, sale_price, market_price, stock_quantity,
    category_id, category_name, status, description, sales_count,
    deleted, create_by, create_time, update_by, update_time
)
SELECT
    ep.id, COALESCE(ep.tenant_id, 0), ep.product_code, ep.product_code, ep.product_name,
    ep.image_url,
    COALESCE(ep.retail_price, ep.standard_price, 0),
    COALESCE(ep.wholesale_price, 0),
    0,
    ep.mall_category_id::VARCHAR(50),
    ep.mall_category_name,
    CASE WHEN COALESCE(ep.mall_shelf_status, 1) = 1 THEN 'ON_SHELF' ELSE 'INACTIVE' END,
    COALESCE(ep.mall_description, ''),
    ep.mall_sales_count,
    ep.deleted, ep.create_by::BIGINT, ep.create_time, ep.update_by::BIGINT, ep.update_time
FROM erp_product ep
WHERE ep.deleted = 0
ON CONFLICT (id) DO UPDATE SET
    product_name    = EXCLUDED.product_name,
    image_url       = EXCLUDED.image_url,
    sale_price      = EXCLUDED.sale_price,
    market_price    = EXCLUDED.market_price,
    status          = EXCLUDED.status,
    description     = EXCLUDED.description,
    sales_count     = EXCLUDED.sales_count,
    update_time     = NOW();

-- =====================================================
-- Part 9: 标记废弃表(添加注释,便于后续清理)
-- =====================================================
COMMENT ON TABLE mall_product IS '[已废弃] 由 v_mall_product 视图替代,数据源为 erp_product';
COMMENT ON TABLE mall_order IS '[已废弃] 新订单写入 erp_sale_order(order_source=2)';
COMMENT ON TABLE mall_order_item IS '[已废弃] 新订单明细写入 erp_sale_order_item';
