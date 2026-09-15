-- ============================================================
-- V9.43.0: 销售出库单主表扩展字段
--
-- 对标生产级ERP系统，为 erp_sale_outbound 添加客户快照、
-- 收款、物流、会员、自定义等完整字段
-- ============================================================

ALTER TABLE erp_sale_outbound
    -- 客户快照扩展
    ADD COLUMN IF NOT EXISTS customer_code          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS customer_level         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS customer_remark        VARCHAR(500),
    -- 银行/税务
    ADD COLUMN IF NOT EXISTS bank_name              VARCHAR(200),
    ADD COLUMN IF NOT EXISTS bank_account           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tax_no                 VARCHAR(100),
    -- 摘要
    ADD COLUMN IF NOT EXISTS summary                VARCHAR(500),
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
    -- 物流扩展
    ADD COLUMN IF NOT EXISTS delivery_method        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS logistics_branch       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS freight_payer          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS freight                DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS waybill_no             VARCHAR(200),
    ADD COLUMN IF NOT EXISTS cod_amount             DECIMAL(18,2) DEFAULT 0,
    -- 收款账户（4个）
    ADD COLUMN IF NOT EXISTS payment_account1       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account2       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account3       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account4       VARCHAR(200),
    -- 金额扩展
    ADD COLUMN IF NOT EXISTS promo_discount         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS coupon_amount          DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS direct_discount        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS other_fee              DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rounding_amount        DECIMAL(18,2) DEFAULT 0,
    -- 统计
    ADD COLUMN IF NOT EXISTS total_weight           DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_volume           DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS return_quantity        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS return_amount          DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS box_count              INTEGER DEFAULT 0,
    -- 产生方式
    ADD COLUMN IF NOT EXISTS generation_method      VARCHAR(100),
    -- 会员/积分
    ADD COLUMN IF NOT EXISTS member_card_no         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS prev_points            DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_generated_points  DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_exchange_points   DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_used_points       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_points           DECIMAL(18,2) DEFAULT 0,
    -- 收款日/对账日
    ADD COLUMN IF NOT EXISTS payment_date           DATE,
    ADD COLUMN IF NOT EXISTS reconciliation_date    DATE,
    -- 表尾自定义字段
    ADD COLUMN IF NOT EXISTS footer_ext_text1       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS footer_ext_text2       VARCHAR(500),
    -- 记账时间
    ADD COLUMN IF NOT EXISTS bookkeeping_time       TIMESTAMP,
    -- 打印时间
    ADD COLUMN IF NOT EXISTS print_time             TIMESTAMP;

-- ══ 补充索引 ═══
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_customer_code ON erp_sale_outbound(customer_code);
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_settlement ON erp_sale_outbound(settlement_status, settlement_method);

-- ══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_outbound' AND column_name = 'customer_code';

    RAISE NOTICE '=== V9.43.0 销售出库单主表字段扩展 ===';
    RAISE NOTICE '总新增字段数: 45';
    RAISE NOTICE 'customer_code 列: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
