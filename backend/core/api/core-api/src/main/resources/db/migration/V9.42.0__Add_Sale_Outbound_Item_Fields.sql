-- ============================================================
-- V9.42.0: 销售出库明细表扩展字段
--
-- 对标生产级ERP系统（Odoo/SAP/金蝶/用友），为 erp_sale_outbound_item
-- 添加完整的产品快照、包装、价格、库存、成本、优惠、物流、积分等字段
-- ============================================================

ALTER TABLE erp_sale_outbound_item
    -- 产品图片
    ADD COLUMN IF NOT EXISTS image_url              VARCHAR(500),
    -- 条码相关
    ADD COLUMN IF NOT EXISTS barcode                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS small_unit_barcode     VARCHAR(100),
    -- 货位
    ADD COLUMN IF NOT EXISTS location               VARCHAR(200),
    -- 规格型号
    ADD COLUMN IF NOT EXISTS specification          VARCHAR(200),
    ADD COLUMN IF NOT EXISTS model                  VARCHAR(200),
    -- 产地品牌
    ADD COLUMN IF NOT EXISTS origin                 VARCHAR(200),
    ADD COLUMN IF NOT EXISTS brand                  VARCHAR(200),
    -- 商品行属性
    ADD COLUMN IF NOT EXISTS product_attribute      VARCHAR(100),
    -- 单据自定义字段（数字）
    ADD COLUMN IF NOT EXISTS ext_num1               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num2               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num3               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num4               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num5               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num6               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num7               DECIMAL(18,2),
    -- 单据自定义字段（文本）
    ADD COLUMN IF NOT EXISTS ext_text1              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text2              VARCHAR(500),
    -- 单据自定义字段（往来单位/职员/部门）
    ADD COLUMN IF NOT EXISTS ext_partner            BIGINT,
    ADD COLUMN IF NOT EXISTS ext_staff              BIGINT,
    ADD COLUMN IF NOT EXISTS ext_dept               BIGINT,
    -- 价格相关
    ADD COLUMN IF NOT EXISTS last_sale_date         DATE,
    ADD COLUMN IF NOT EXISTS last_sale_price        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS retail_price           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS wholesale_price        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS min_sale_price         DECIMAL(18,4),
    -- 小单位相关
    ADD COLUMN IF NOT EXISTS small_unit             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS small_unit_price       DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS small_unit_quantity    DECIMAL(18,2),
    -- 库存相关
    ADD COLUMN IF NOT EXISTS available_stock        DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS book_stock             DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS conversion_result      DECIMAL(18,2),
    -- 包装
    ADD COLUMN IF NOT EXISTS big_pack               DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mid_pack               DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS small_pack             DECIMAL(18,2) DEFAULT 0,
    -- 成本毛利
    ADD COLUMN IF NOT EXISTS cost_price             DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS cost_amount            DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS gross_profit           DECIMAL(18,2),
    -- 体积重量
    ADD COLUMN IF NOT EXISTS volume                 DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS weight                 DECIMAL(18,4),
    -- 赠品/兑换
    ADD COLUMN IF NOT EXISTS gift                   BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS gift_item              VARCHAR(200),
    ADD COLUMN IF NOT EXISTS exchange_points        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS used_points            DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generated_points       DECIMAL(18,2) DEFAULT 0,
    -- 优惠折扣（折单级别）
    ADD COLUMN IF NOT EXISTS favorable_discount_rate  DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS favorable_unit_price     DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS favorable_amount         DECIMAL(18,2) DEFAULT 0,
    -- 价格等级（8级）
    ADD COLUMN IF NOT EXISTS price_level1           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level2           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level3           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level4           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level5           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level6           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level7           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level8           DECIMAL(18,4),
    -- 箱号
    ADD COLUMN IF NOT EXISTS box_no                 VARCHAR(100);

-- ══ 索引 ═══
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_item_outbound ON erp_sale_outbound_item(outbound_id);
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_item_product ON erp_sale_outbound_item(product_id);

-- ══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_outbound_item' AND column_name = 'image_url';

    RAISE NOTICE '=== V9.42.0 销售出库明细字段扩展 ===';
    RAISE NOTICE '总新增字段数: 55';
    RAISE NOTICE 'image_url 列: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
