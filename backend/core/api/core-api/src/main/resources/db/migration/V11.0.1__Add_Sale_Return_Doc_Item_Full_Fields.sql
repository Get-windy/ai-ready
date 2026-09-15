-- ============================================================
-- V11.0.1: 销售退货单明细表完整字段扩展
--
-- 对标生产级ERP系统，为 erp_sale_return_doc_item 补齐页面
-- 配置所需的全部字段（从 ~18 个扩展到 ~70 个），对应文档 67 列
-- ============================================================

ALTER TABLE erp_sale_return_doc_item
    -- ═══ 标准租户/审计字段 ═══
    ADD COLUMN IF NOT EXISTS tenant_id              BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS line_no                INTEGER,
    ADD COLUMN IF NOT EXISTS create_by              BIGINT,
    ADD COLUMN IF NOT EXISTS create_time            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS update_by              BIGINT,
    ADD COLUMN IF NOT EXISTS update_time            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted                INTEGER DEFAULT 0,

    -- ═══ 商品扩展信息 ═══
    ADD COLUMN IF NOT EXISTS image_url              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS product_spec           VARCHAR(200),
    ADD COLUMN IF NOT EXISTS product_unit           VARCHAR(50),
    ADD COLUMN IF NOT EXISTS storage_location       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS area                   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS model_no               VARCHAR(200),
    ADD COLUMN IF NOT EXISTS origin_place           VARCHAR(200),
    ADD COLUMN IF NOT EXISTS brand                  VARCHAR(200),

    -- ═══ 库存信息 ═══
    ADD COLUMN IF NOT EXISTS available_stock        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS available_stock_converted DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS book_stock             DECIMAL(18,4),

    -- ═══ 批次信息 ═══
    ADD COLUMN IF NOT EXISTS batch_barcode          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS production_date        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS shelf_life             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS expiry_date            TIMESTAMP,

    -- ═══ 价格体系 ═══
    ADD COLUMN IF NOT EXISTS last_sale_date         DATE,
    ADD COLUMN IF NOT EXISTS last_sale_price        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS retail_price           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS wholesale_price        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS min_sale_price         DECIMAL(18,4),

    -- ═══ 小单位 ═══
    ADD COLUMN IF NOT EXISTS small_unit             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS small_unit_price       DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS small_unit_quantity    DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS conversion_result      DECIMAL(18,4),

    -- ═══ 成本 ═══
    ADD COLUMN IF NOT EXISTS ref_cost_price         DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS ref_cost_amount        DECIMAL(18,2),

    -- ═══ 折扣 ═══
    ADD COLUMN IF NOT EXISTS discount_rate          DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discounted_price       DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS discounted_amount      DECIMAL(18,2) DEFAULT 0,

    -- ═══ 积分/礼品 ═══
    ADD COLUMN IF NOT EXISTS exchange_gift          VARCHAR(200),
    ADD COLUMN IF NOT EXISTS exchange_points        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generated_points       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS used_points            DECIMAL(18,2) DEFAULT 0,

    -- ═══ 价格等级（8个标准化产品价格等级） ═══
    -- 50:餐饮店 51:食堂团餐 52:外围餐饮店 53:自助vip
    -- 54:大团餐 55:重点|vip01 56:连锁|vip 57:特价客户
    ADD COLUMN IF NOT EXISTS price_level1           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level2           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level3           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level4           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level5           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level6           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level7           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level8           DECIMAL(18,4),

    -- ═══ 物理属性 ═══
    ADD COLUMN IF NOT EXISTS weight                 DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS volume                 DECIMAL(18,4),

    -- ═══ 行属性 ═══
    ADD COLUMN IF NOT EXISTS is_gift                BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS product_line_attr      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS received_quantity      DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS terminated_quantity    DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS terminated_amount      DECIMAL(18,2) DEFAULT 0,

    -- ═══ 明细备注 ═══
    ADD COLUMN IF NOT EXISTS item_remark            VARCHAR(500),

    -- ═══ 单据自定义字段（数字1-7） ═══
    ADD COLUMN IF NOT EXISTS ext_num1               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num2               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num3               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num4               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num5               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num6               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num7               DECIMAL(18,2),

    -- ═══ 单据自定义字段（文本1-2） ═══
    ADD COLUMN IF NOT EXISTS ext_text1              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text2              VARCHAR(500),

    -- ═══ 单据自定义字段（关联） ═══
    ADD COLUMN IF NOT EXISTS ext_partner            BIGINT,
    ADD COLUMN IF NOT EXISTS ext_staff              BIGINT,
    ADD COLUMN IF NOT EXISTS ext_dept               BIGINT;

-- ═══ 明细表索引 ═══
CREATE INDEX IF NOT EXISTS idx_return_doc_item_tenant ON erp_sale_return_doc_item(tenant_id);
CREATE INDEX IF NOT EXISTS idx_return_doc_item_line_no ON erp_sale_return_doc_item(return_doc_id, line_no);
CREATE INDEX IF NOT EXISTS idx_return_doc_item_brand ON erp_sale_return_doc_item(brand);
CREATE INDEX IF NOT EXISTS idx_return_doc_item_product_attr ON erp_sale_return_doc_item(product_line_attr);

-- ═══ 列注释 ═══
COMMENT ON COLUMN erp_sale_return_doc_item.line_no IS '行号';
COMMENT ON COLUMN erp_sale_return_doc_item.product_spec IS '产品规格(冗余)';
COMMENT ON COLUMN erp_sale_return_doc_item.product_unit IS '产品单位(冗余)';
COMMENT ON COLUMN erp_sale_return_doc_item.storage_location IS '货位';
COMMENT ON COLUMN erp_sale_return_doc_item.available_stock IS '可用库存';
COMMENT ON COLUMN erp_sale_return_doc_item.book_stock IS '账面库存';
COMMENT ON COLUMN erp_sale_return_doc_item.batch_barcode IS '批次条码';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level1 IS '餐饮店价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level2 IS '食堂团餐价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level3 IS '外围餐饮店价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level4 IS '自助vip价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level5 IS '大团餐价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level6 IS '重点|vip01价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level7 IS '连锁|vip价格';
COMMENT ON COLUMN erp_sale_return_doc_item.price_level8 IS '特价客户价格';
COMMENT ON COLUMN erp_sale_return_doc_item.is_gift IS '是否赠品';
COMMENT ON COLUMN erp_sale_return_doc_item.exchange_gift IS '兑换礼品';

-- ═══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return_doc_item';

    RAISE NOTICE '=== V11.0.1 销售退货单明细表完整字段扩展 ===';
    RAISE NOTICE '明细表总字段数: %', col_count;
END $$;
