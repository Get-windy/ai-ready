-- ============================================================================
-- V11.361.5 交易模块金标准补齐：v_mall_product 视图追加规格 / 单位两列
-- ============================================================================
-- 背景：
--   购物车（trade/cart）金标准列定义含「规格 specification」「单位 unitName」两列，
--   商品上架（mall/product-shelf）同样需要展示规格与单位。二者数据源均为
--   v_mall_product 视图（`SELECT ... FROM erp_product`），但该视图此前未暴露
--   erp_product.spec / erp_product.unit，导致前端两列恒为空。
--
-- 处理：
--   以 CREATE OR REPLACE VIEW 重建视图，在原列之后**追加**两列（PostgreSQL 仅允许
--   在视图末尾追加列，故不调整既有列顺序与类型，向后兼容）。
--     新列 == erp_product.spec  → specification（规格）
--     新列 == erp_product.unit  → unit_name    （单位）
--
-- 幂等性：
--   CREATE OR REPLACE VIEW 可重复执行；不新增任何物理表/列，不修改既有列。
--   视图定义与 V9.32.0__Create_All_Missing_Tables.sql 保持一致，仅末尾追加两列。
-- ============================================================================

CREATE OR REPLACE VIEW v_mall_product AS
SELECT
    p.id,
    p.tenant_id,
    CAST(p.product_code AS TEXT)    AS product_id,
    p.product_code,
    p.product_name,
    p.image_url,
    p.retail_price                  AS sale_price,
    p.wholesale_price               AS market_price,
    CAST(COALESCE(s.total_available, 0) AS INTEGER) AS stock_quantity,
    CAST(p.category_id AS TEXT)     AS category_id,
    COALESCE(p.mall_category_name, p.category) AS category_name,
    CAST(CASE WHEN p.mall_shelf_status = 1 THEN 'ON_SHELF' ELSE 'INACTIVE' END AS TEXT) AS status,
    p.mall_description              AS description,
    CAST(COALESCE(p.mall_sales_count, 0) AS INTEGER) AS sales_count,
    p.deleted,
    CAST(p.create_by AS BIGINT)     AS create_by,
    p.create_time,
    CAST(p.update_by AS BIGINT)     AS update_by,
    p.update_time,
    -- ↓↓↓ V11.361.5 追加列（必须位于末尾） ↓↓↓
    p.spec                          AS specification,
    p.unit                          AS unit_name
FROM erp_product p
LEFT JOIN (
    SELECT product_id, SUM(COALESCE(available_quantity, 0)) AS total_available
    FROM erp_stock
    WHERE deleted = 0
    GROUP BY product_id
) s ON s.product_id = p.id
WHERE p.deleted = 0;

COMMENT ON VIEW v_mall_product IS '商城商品只读视图（数据源 erp_product + erp_stock 实时可用库存）；V11.361.5 起追加 specification/unit_name 两列供购物车与商品上架使用';
