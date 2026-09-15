-- ============================================================
-- V9.53.0: 销售退货申请表完整字段扩展
--
-- 对标生产级ERP系统（Odoo/SAP/金蝶/用友），为 erp_sale_return
-- 和 erp_sale_return_item 补齐页面配置和列表所需的全部字段
-- ============================================================

-- ═══════════════════════════════════════════════════════════
-- 主表 erp_sale_return 补充字段
-- ═══════════════════════════════════════════════════════════

ALTER TABLE erp_sale_return
    -- 银行/税务快照
    ADD COLUMN IF NOT EXISTS bank_name              VARCHAR(200),
    ADD COLUMN IF NOT EXISTS bank_account           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tax_no                 VARCHAR(100),
    -- 客户一票通
    ADD COLUMN IF NOT EXISTS customer_ticket        VARCHAR(50),
    -- 部门ID（dept_name已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS dept_id                BIGINT,
    -- 区域
    ADD COLUMN IF NOT EXISTS region                 VARCHAR(200),
    -- 收货信息快照
    ADD COLUMN IF NOT EXISTS receiver_name          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS receiver_phone         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS shipping_address       VARCHAR(500),
    -- 金额计算链
    ADD COLUMN IF NOT EXISTS product_amount         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS promo_discount         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS coupon_amount          DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS direct_discount        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_amount        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS other_fee              DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bill_amount            DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settled_amount         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS freight_payer          VARCHAR(100),
    -- 数量汇总
    ADD COLUMN IF NOT EXISTS ordered_quantity       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS received_quantity      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unreceived_quantity    DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS return_quantity_total  DECIMAL(18,2) DEFAULT 0,
    -- 物理属性汇总
    ADD COLUMN IF NOT EXISTS total_weight           DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_volume           DECIMAL(18,4) DEFAULT 0,
    -- 结算方式（settle_status已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS settlement_method      VARCHAR(100),
    -- 收款账户
    ADD COLUMN IF NOT EXISTS payment_account1       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account2       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account3       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account4       VARCHAR(200),
    -- 信用额度（current_debt/prev_debt/debt_balance已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS credit_limit           DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_credit       DECIMAL(18,2) DEFAULT 0,
    -- 物流扩展（logistics_company/logistics_no/shipping_fee已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS delivery_method        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_route         VARCHAR(200),
    ADD COLUMN IF NOT EXISTS delivery_route_id      BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_order_no      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS waybill_no             VARCHAR(200),
    ADD COLUMN IF NOT EXISTS cod_amount             DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS driver_name            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS driver_id              BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_vehicle       VARCHAR(100),
    -- 会员/积分扩展（member_card_no已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS prev_points            DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_generated_points  DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_exchange_points   DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_used_points       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_points           DECIMAL(18,2) DEFAULT 0,
    -- 源单关联（source_order已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS source_order_id        BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_order_id      BIGINT,
    -- 表头自定义字段（数字）
    ADD COLUMN IF NOT EXISTS ext_num1               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num2               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num3               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num4               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num5               DECIMAL(18,2),
    -- 表头自定义字段（文本）
    ADD COLUMN IF NOT EXISTS ext_text1              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text2              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text3              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text4              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text5              VARCHAR(500),
    -- 表头自定义字段（往来单位/职员/部门）
    ADD COLUMN IF NOT EXISTS ext_partner            BIGINT,
    ADD COLUMN IF NOT EXISTS ext_staff              BIGINT,
    ADD COLUMN IF NOT EXISTS ext_dept               BIGINT,
    -- 表尾自定义字段
    ADD COLUMN IF NOT EXISTS footer_ext_text1       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS footer_ext_text2       VARCHAR(500),
    -- 备注
    ADD COLUMN IF NOT EXISTS internal_note          VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS buyer_remark           VARCHAR(500),
    -- 摘要
    ADD COLUMN IF NOT EXISTS summary                VARCHAR(500),
    -- 销售类型
    ADD COLUMN IF NOT EXISTS sales_type             VARCHAR(100),
    -- 商品行属性（默认值）
    ADD COLUMN IF NOT EXISTS product_line_attr      VARCHAR(100),
    -- 配送单
    ADD COLUMN IF NOT EXISTS delivery_no            VARCHAR(100),
    -- 提交/审核扩展（approved_by/approved_time已在V9.39.0添加）
    ADD COLUMN IF NOT EXISTS submit_by              BIGINT,
    ADD COLUMN IF NOT EXISTS submit_time            TIMESTAMP,
    ADD COLUMN IF NOT EXISTS auditor_id             BIGINT,
    ADD COLUMN IF NOT EXISTS auditor_name           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS audit_time             TIMESTAMP,
    -- 收款日/对账日
    ADD COLUMN IF NOT EXISTS payment_date           DATE,
    ADD COLUMN IF NOT EXISTS reconciliation_date    DATE,
    -- 记账/打印时间
    ADD COLUMN IF NOT EXISTS bookkeeping_time       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS print_time             TIMESTAMP;

-- ═══ 主表索引 ═══
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_dept_id ON erp_sale_return(dept_id);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_settlement ON erp_sale_return(settle_status, settlement_method);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_sales_type ON erp_sale_return(sales_type);

-- ═══════════════════════════════════════════════════════════
-- 明细表 erp_sale_return_item 补充字段
-- ═══════════════════════════════════════════════════════════

ALTER TABLE erp_sale_return_item
    -- 产品图片
    ADD COLUMN IF NOT EXISTS image_url              VARCHAR(500),
    -- 区域/型号/产地/品牌
    ADD COLUMN IF NOT EXISTS area                   VARCHAR(200),
    ADD COLUMN IF NOT EXISTS model_no               VARCHAR(200),
    ADD COLUMN IF NOT EXISTS origin_place           VARCHAR(200),
    ADD COLUMN IF NOT EXISTS brand                  VARCHAR(200),
    -- 库存扩展（available_stock已在V9.38.0添加）
    ADD COLUMN IF NOT EXISTS available_stock_converted  DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS book_stock             DECIMAL(18,2),
    -- 价格体系
    ADD COLUMN IF NOT EXISTS last_sale_date         DATE,
    ADD COLUMN IF NOT EXISTS last_sale_price        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS retail_price           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS wholesale_price        DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS min_sale_price         DECIMAL(18,4),
    -- 小单位
    ADD COLUMN IF NOT EXISTS small_unit             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS small_unit_price       DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS small_unit_quantity    DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS conversion_result      DECIMAL(18,2),
    -- 折扣
    ADD COLUMN IF NOT EXISTS discount_rate          DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discounted_price       DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS discounted_amount      DECIMAL(18,2) DEFAULT 0,
    -- 成本
    ADD COLUMN IF NOT EXISTS ref_cost_price         DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS ref_cost_amount        DECIMAL(18,2),
    -- 物理属性
    ADD COLUMN IF NOT EXISTS weight                 DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS volume                 DECIMAL(18,4),
    -- 收货/终止
    ADD COLUMN IF NOT EXISTS received_quantity      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS terminated_quantity    DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS terminated_amount      DECIMAL(18,2) DEFAULT 0,
    -- 行属性
    ADD COLUMN IF NOT EXISTS is_gift                BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS product_line_attr      VARCHAR(100),
    -- 明细备注（remark已在V9.12.0添加）
    ADD COLUMN IF NOT EXISTS item_remark            VARCHAR(500),
    -- 价格等级（8个标准化产品价格等级，对标销售出库单）
    ADD COLUMN IF NOT EXISTS price_level1           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level2           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level3           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level4           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level5           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level6           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level7           DECIMAL(18,4),
    ADD COLUMN IF NOT EXISTS price_level8           DECIMAL(18,4),
    -- 单据自定义字段（数字1-7）
    ADD COLUMN IF NOT EXISTS ext_num1               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num2               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num3               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num4               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num5               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num6               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num7               DECIMAL(18,2),
    -- 单据自定义字段（文本1-2）
    ADD COLUMN IF NOT EXISTS ext_text1              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text2              VARCHAR(500),
    -- 单据自定义字段（往来单位/职员/部门）
    ADD COLUMN IF NOT EXISTS ext_partner            BIGINT,
    ADD COLUMN IF NOT EXISTS ext_staff              BIGINT,
    ADD COLUMN IF NOT EXISTS ext_dept               BIGINT;

-- ═══ 明细表索引 ═══
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_item_brand ON erp_sale_return_item(brand);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_item_product_attr ON erp_sale_return_item(product_line_attr);

-- ═══════════════════════════════════════════════════════════
-- 验证
-- ═══════════════════════════════════════════════════════════

DO $$
DECLARE
    main_count INTEGER;
    item_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO main_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return' AND column_name = 'bank_name';

    SELECT COUNT(*) INTO item_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return_item' AND column_name = 'image_url';

    RAISE NOTICE '=== V9.53.0 销售退货申请表完整字段扩展 ===';
    RAISE NOTICE '主表 bank_name 列: %', CASE WHEN main_count > 0 THEN 'OK' ELSE 'MISSING' END;
    RAISE NOTICE '明细表 image_url 列: %', CASE WHEN item_count > 0 THEN 'OK' ELSE 'MISSING' END;

    SELECT COUNT(*) INTO main_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return';
    SELECT COUNT(*) INTO item_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return_item';

    RAISE NOTICE '主表总字段数: %', main_count;
    RAISE NOTICE '明细表总字段数: %', item_count;
END $$;
