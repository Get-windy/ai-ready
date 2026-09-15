-- ============================================================
-- V9.38.0: 补充销售退货申请表字段
--
-- 为 erp_sale_return 表添加列表页和表单页所需字段
-- 为 erp_sale_return_item 表添加明细表格所需字段
-- ============================================================

-- ═══ 主表 erp_sale_return 补充字段 ═══
ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS customer_code     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS customer_level    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contact_name      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS contact_phone     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS contact_address   VARCHAR(500),
    ADD COLUMN IF NOT EXISTS customer_remark   VARCHAR(500);

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS warehouse_id      BIGINT,
    ADD COLUMN IF NOT EXISTS warehouse_name    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS handler_id        BIGINT,
    ADD COLUMN IF NOT EXISTS handler_name      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dept_name         VARCHAR(100);

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS order_date        TIMESTAMP,
    ADD COLUMN IF NOT EXISTS expected_receive_date VARCHAR(50),
    ADD COLUMN IF NOT EXISTS auditor           VARCHAR(100);

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS print_count       INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS generate_type     VARCHAR(50),
    ADD COLUMN IF NOT EXISTS settle_status     VARCHAR(50);

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS current_debt      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prev_debt         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS debt_balance      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS collection_deadline VARCHAR(50),
    ADD COLUMN IF NOT EXISTS source_order      VARCHAR(100);

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(200),
    ADD COLUMN IF NOT EXISTS logistics_no      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS shipping_fee      DECIMAL(18,2) DEFAULT 0;

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS member_card_no    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS member_name       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS member_discount   DECIMAL(18,2) DEFAULT 100;

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS creator_name      VARCHAR(100);

-- 为 order_date 建索引，加速列表页按日期查询
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_order_date ON erp_sale_return(order_date);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_customer_name ON erp_sale_return(customer_name);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_handler_name ON erp_sale_return(handler_name);
CREATE INDEX IF NOT EXISTS idx_erp_sale_return_status ON erp_sale_return(status);

-- ═══ 明细表 erp_sale_return_item 补充字段 ══
ALTER TABLE erp_sale_return_item
    ADD COLUMN IF NOT EXISTS barcode             VARCHAR(200),
    ADD COLUMN IF NOT EXISTS specification       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS unit                VARCHAR(50),
    ADD COLUMN IF NOT EXISTS conversion_relation VARCHAR(100),
    ADD COLUMN IF NOT EXISTS available_stock     DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS piece_quantity      DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS big_pack            DECIMAL(18,0) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mid_pack            DECIMAL(18,0) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS small_pack          DECIMAL(18,0) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tax_rate            DECIMAL(18,4) DEFAULT 13,
    ADD COLUMN IF NOT EXISTS exchange_gift       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS exchange_points     DECIMAL(18,2) DEFAULT 0;

-- ══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
    item_col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return' AND column_name = 'warehouse_id';

    SELECT COUNT(*) INTO item_col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return_item' AND column_name = 'barcode';

    RAISE NOTICE '=== V9.38.0 销售退货申请表字段补充 ===';
    RAISE NOTICE '主表 warehouse_id 列: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
    RAISE NOTICE '明细表 barcode 列: %', CASE WHEN item_col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
