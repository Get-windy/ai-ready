-- ============================================================================
-- V11.361.6 交易模块金标准补齐：v_mall_product 视图追加「商品上架」对标列
-- ============================================================================
-- 背景：
--   商品上架（mall/product-shelf，菜单 80360）金标准列配置共 48 列，其中
--   条码/规格/型号/产地/品牌/单位/批发价/预设进价/排序/排序值/起订量/商品积分/
--   备注/关键字 + 8 个客户类型价格列 + 20 个食品分类列此前在 v_mall_product
--   视图中均不存在，前端只能以 slot 渲染 '-'（不做假数据）。
--   V11.361.5 已追加 specification（← erp_product.spec）与 unit_name
--   （← erp_product.unit）两列，本次在其后继续追加。
--
-- 处理：
--   以 CREATE OR REPLACE VIEW 重建视图。PostgreSQL 仅允许在视图**末尾追加**列，
--   故既有 21 列（id..unit_name）的顺序、名称与类型完全保持不变，仅在末尾追加。
--   既有 CAST(p.create_by AS BIGINT) / CAST(p.update_by AS BIGINT) 原样保留
--   （erp_product.create_by/update_by 建表为 VARCHAR(50)，视图显式 CAST，勿改）。
--
-- 映射关系（视图列名 ← 数据源；每列均已核对「真正生效」的建表/加列迁移）：
--   1) 直接映射 erp_product
--      barcode                 ← p.barcode            （V1.5.0）
--      model_no                ← p.model              （V8.1.0）
--      origin_place            ← p.origin             （V3.0.0）
--      brand                   ← p.brand              （V3.0.0）
--      wholesale_price         ← p.wholesale_price    （V1.5.0，与既有 market_price
--                                同源；market_price 为商城历史「市场价」口径，保留不动）
--      preset_cost_price       ← COALESCE(p.purchase_price, p.cost_price, 0)
--                                （purchase_price V3.0.0 / cost_price V1.5.0）
--      sort                    ← p.mall_sort_type     （V11.139.0）
--      sort_value              ← p.mall_sort_order    （V8.1.0）
--      min_order_quantity      ← p.mall_min_order_qty （V8.1.0）
--      product_points          ← p.mall_points        （V11.139.0）
--      remark                  ← p.remark             （V1.1.9 建表）
--      keyword                 ← p.keywords           （V11.139.0）
--      use_coupon              ← p.use_coupon         （V8.1.0，0/1 原始列）
--      coupon_used             ← p.use_coupon = 1     （BOOLEAN，便于前端直接判定）
--      product_type            ← p.product_type       （V1.5.0，SINGLE/KIT/SERVICE）
--      visible_status          ← p.status             （V1.1.9 建表，ENABLED/DISABLED）
--      product_tag             ← p.mall_tags          （V8.1.0，逗号分隔标准槽位
--                                TAG_1..TAG_20，见 V11.161.0）
--      industry_category       ← p.industry_category  （V8.1.0）
--   2) 客户类型价格 8 列（对标列 15-22）
--      grade_price_1 .. grade_price_8 ← MAX(erp_product_unit.grade_price_N)
--        口径：erp_product_unit 为「商品 × 单位」的明细表，等级价按单位维护
--        （V11.139.0:10-15 明确：erp_product_unit.grade_price_N = 单位标准等级价，
--         商品表单维护；erp_product_grade_price = 客户指定价/级别指定价，两者口径不同）。
--        列序与「商品上架」页 8 列一一对应（V9.23.0 的旧列名映射）：
--          1 餐饮店 / 2 食堂团餐 / 3 外围餐饮店 / 4 自助vip
--          5 大团餐 / 6 重点|vip01 / 7 连锁|vip / 8 特价客户
--        聚合用 MAX 的理由：一个商品可挂多个单位，而列表页只有一行，
--        取最大值即「该商品在该价格等级上是否配了价」的保守判定；
--        前端按 > 0 渲染 √/×。
--        注：grade_price_1..8 由 V9.23.0 建立、V9.29.0 曾 DROP、
--        V11.139.0 又 ADD COLUMN IF NOT EXISTS 恢复（DEFAULT 0），当前存在。
--      customer_grade_codes    ← erp_customer_grade_price.grade_name 去重升序聚合
--                                （V11.158.0 建表；grade_name 为「级别指定价设置」
--                                 里用户自定义的等级昵称，作为 8 列的补充佐证）
--   3) 未追加的列（如实保留为缺口，不做假数据）：
--      - 20 个食品分类列（早餐面点/酒席宴席/…/馒头馍馍）没有独立物理列，也没有
--        食品分类标记表（erp_product.industry_category 是 VARCHAR 单值，非 20 个布尔位）；
--        现有唯一等效数据源只有 erp_product.mall_tags（标准槽位 TAG_N），前端按槽位序
--        映射到 20 列，且该映射依赖标签昵称约定，属弱约束。
--
-- 已修正（编写期发现并删除，避免 Flyway 启动失败）：
--   - 曾计划追加 product_grade_codes ← erp_product_grade_price.grade_code。
--     经核对，erp_product_grade_price 真正生效的建表是 V1.5.0:88
--     （列为 id/tenant_id/product_id/product_grade_id/price/min_order_qty/
--      is_active/effective_date/expire_date/remark/deleted/create_by/create_time/
--      update_by/update_time），V8.18.0:66 的同名 CREATE TABLE IF NOT EXISTS
--      **整表被跳过**，其 grade_code/grade_name/... 列从未建立，
--      V9.29.0/V9.30.0/V9.31.0 也只加了 unit_id/contact_partner_id/benefit_*/
--      date_start/date_end/min_quantity。故该列不存在，已整列删除。
--
-- 幂等性：
--   CREATE OR REPLACE VIEW 天然可重复执行；不新增/修改/删除任何物理表或物理列，
--   不改动既有视图列的顺序、名称与类型（仅在末尾追加），向后兼容。
--   聚合子查询均为相关子查询 + 标量聚合，商品无单位/无等级价时返回 NULL
--   （前端渲染 '-' 或 ×）。
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
    -- ↓↓↓ V11.361.5 追加列（顺序不可调整，勿移动） ↓↓↓
    p.spec                          AS specification,
    p.unit                          AS unit_name,
    -- ↓↓↓ V11.361.6 追加列（必须位于末尾） ↓↓↓
    p.barcode                       AS barcode,
    p.model                         AS model_no,
    p.origin                        AS origin_place,
    p.brand                         AS brand,
    p.wholesale_price               AS wholesale_price,
    COALESCE(p.purchase_price, p.cost_price, 0) AS preset_cost_price,
    p.mall_sort_type                AS sort,
    p.mall_sort_order               AS sort_value,
    p.mall_min_order_qty            AS min_order_quantity,
    p.mall_points                   AS product_points,
    p.remark                        AS remark,
    p.keywords                      AS keyword,
    p.use_coupon                    AS use_coupon,
    CAST(p.use_coupon = 1 AS BOOLEAN) AS coupon_used,
    p.product_type                  AS product_type,
    p.status                        AS visible_status,
    p.mall_tags                     AS product_tag,
    p.industry_category             AS industry_category,
    -- 客户类型价格 8 列（对标列 15-22）：MAX 聚合 erp_product_unit.grade_price_N
    (SELECT MAX(u.grade_price_1) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_1,
    (SELECT MAX(u.grade_price_2) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_2,
    (SELECT MAX(u.grade_price_3) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_3,
    (SELECT MAX(u.grade_price_4) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_4,
    (SELECT MAX(u.grade_price_5) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_5,
    (SELECT MAX(u.grade_price_6) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_6,
    (SELECT MAX(u.grade_price_7) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_7,
    (SELECT MAX(u.grade_price_8) FROM erp_product_unit u
      WHERE u.product_id = p.id AND u.deleted = 0) AS grade_price_8,
    -- 客户等级昵称集合（级别指定价设置，补充佐证；昵称为用户自定义）
    (SELECT string_agg(DISTINCT cg.grade_name, ',' ORDER BY cg.grade_name)
       FROM erp_customer_grade_price cg
      WHERE cg.product_id = p.id
        AND cg.tenant_id = p.tenant_id
        AND cg.deleted = 0
        AND cg.status = 1
        AND cg.grade_name IS NOT NULL
        AND cg.grade_name <> '')    AS customer_grade_codes
FROM erp_product p
LEFT JOIN (
    SELECT product_id, SUM(COALESCE(available_quantity, 0)) AS total_available
    FROM erp_stock
    WHERE deleted = 0
    GROUP BY product_id
) s ON s.product_id = p.id
WHERE p.deleted = 0;

-- ── 视图/列注释（CREATE OR REPLACE VIEW 不会清除未改动列的注释，此处仅补新列） ──
COMMENT ON VIEW v_mall_product IS '商城商品只读视图（数据源 erp_product + erp_stock 实时可用库存）；V11.361.5 起追加 specification/unit_name，V11.361.6 起追加商品上架金标准 48 列所需字段';

COMMENT ON COLUMN v_mall_product.barcode              IS '条形码（erp_product.barcode）';
COMMENT ON COLUMN v_mall_product.model_no             IS '型号（erp_product.model）';
COMMENT ON COLUMN v_mall_product.origin_place         IS '产地（erp_product.origin）';
COMMENT ON COLUMN v_mall_product.brand                IS '品牌（erp_product.brand）';
COMMENT ON COLUMN v_mall_product.wholesale_price      IS '批发价（erp_product.wholesale_price，与 market_price 同源，market_price 为历史商城口径保留）';
COMMENT ON COLUMN v_mall_product.preset_cost_price    IS '预设进价（COALESCE(erp_product.purchase_price, erp_product.cost_price, 0)）';
COMMENT ON COLUMN v_mall_product.sort                 IS '商城排序方式（erp_product.mall_sort_type：DEFAULT/SALES/MANUAL）';
COMMENT ON COLUMN v_mall_product.sort_value           IS '商城排序值（erp_product.mall_sort_order）';
COMMENT ON COLUMN v_mall_product.min_order_quantity   IS '商城起订量（erp_product.mall_min_order_qty）';
COMMENT ON COLUMN v_mall_product.product_points       IS '商品积分（erp_product.mall_points）';
COMMENT ON COLUMN v_mall_product.remark               IS '备注（erp_product.remark）';
COMMENT ON COLUMN v_mall_product.keyword              IS '商城检索关键字（erp_product.keywords）';
COMMENT ON COLUMN v_mall_product.use_coupon           IS '使用优惠券原始值（erp_product.use_coupon：0=否 1=是）';
COMMENT ON COLUMN v_mall_product.coupon_used          IS '使用优惠券布尔值（erp_product.use_coupon = 1），供前端「使用优惠券」查询与列展示直接判定';
COMMENT ON COLUMN v_mall_product.product_type         IS '商品类型（erp_product.product_type：SINGLE单品/KIT套件/SERVICE服务）';
COMMENT ON COLUMN v_mall_product.visible_status       IS '显示状态（erp_product.status：ENABLED启用/DISABLED停用），非商城上架状态';
COMMENT ON COLUMN v_mall_product.product_tag          IS '商品标签（erp_product.mall_tags，逗号分隔的标准槽位编码 TAG_1..TAG_20，见 V11.161.0）';
COMMENT ON COLUMN v_mall_product.industry_category    IS '所属行业类别（erp_product.industry_category）';
COMMENT ON COLUMN v_mall_product.grade_price_1        IS '价格等级1-餐饮店（MAX(erp_product_unit.grade_price_1)，该商品多单位取最大值；>0 即视为已配价）';
COMMENT ON COLUMN v_mall_product.grade_price_2        IS '价格等级2-食堂团餐（MAX(erp_product_unit.grade_price_2)）';
COMMENT ON COLUMN v_mall_product.grade_price_3        IS '价格等级3-外围餐饮店（MAX(erp_product_unit.grade_price_3)）';
COMMENT ON COLUMN v_mall_product.grade_price_4        IS '价格等级4-自助vip（MAX(erp_product_unit.grade_price_4)）';
COMMENT ON COLUMN v_mall_product.grade_price_5        IS '价格等级5-大团餐（MAX(erp_product_unit.grade_price_5)）';
COMMENT ON COLUMN v_mall_product.grade_price_6        IS '价格等级6-重点|vip01（MAX(erp_product_unit.grade_price_6)）';
COMMENT ON COLUMN v_mall_product.grade_price_7        IS '价格等级7-连锁|vip（MAX(erp_product_unit.grade_price_7)）';
COMMENT ON COLUMN v_mall_product.grade_price_8        IS '价格等级8-特价客户（MAX(erp_product_unit.grade_price_8)）';
COMMENT ON COLUMN v_mall_product.customer_grade_codes IS '已配置级别指定价的客户等级昵称集合（erp_customer_grade_price.grade_name 去重升序逗号串，昵称为用户自定义；作为 8 个 grade_price_N 的补充佐证）';
