-- ============================================================
-- V11.0.0: 销售退货单主表完整字段扩展
--
-- 对标生产级ERP系统，为 erp_sale_return_doc 补齐页面配置和
-- 列表所需的全部字段（从 ~25 个扩展到 ~100 个）
-- ============================================================

ALTER TABLE erp_sale_return_doc
    -- ═══ 单据基本信息 ═══
    ADD COLUMN IF NOT EXISTS sales_type             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS generate_type          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS settle_status          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS attachment             VARCHAR(500),

    -- ═══ 银行/税务快照 ═══
    ADD COLUMN IF NOT EXISTS bank_name              VARCHAR(200),
    ADD COLUMN IF NOT EXISTS bank_account           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tax_no                 VARCHAR(100),
    ADD COLUMN IF NOT EXISTS customer_ticket        VARCHAR(50),

    -- ═══ 部门/区域 ═══
    ADD COLUMN IF NOT EXISTS dept_id                BIGINT,
    ADD COLUMN IF NOT EXISTS region                 VARCHAR(200),

    -- ═══ 收货信息快照 ═══
    ADD COLUMN IF NOT EXISTS receiver_name          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS receiver_phone         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS shipping_address       VARCHAR(500),

    -- ═══ 金额计算链 ═══
    ADD COLUMN IF NOT EXISTS product_amount         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS promo_discount         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS coupon_amount          DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS direct_discount        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_amount        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS other_fee              DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bill_amount            DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount_bill_amount   DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settled_amount         DECIMAL(18,2) DEFAULT 0,

    -- ═══ 数量汇总 ═══
    ADD COLUMN IF NOT EXISTS return_quantity_total  DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS product_line_count     INTEGER DEFAULT 0,

    -- ═══ 物理属性汇总 ═══
    ADD COLUMN IF NOT EXISTS total_weight           DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_volume           DECIMAL(18,4) DEFAULT 0,

    -- ═══ 付款/预收/信用 ═══
    ADD COLUMN IF NOT EXISTS payment_account1       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account2       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account3       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS payment_account4       VARCHAR(200),
    ADD COLUMN IF NOT EXISTS prev_advance           DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS return_advance         DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_advance      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS advance_balance        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS receivable_reduce      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS credit_limit           DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_credit       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prev_debt              DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_debt           DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS debt_balance           DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS collection_deadline    VARCHAR(50),

    -- ═══ 结算日期 ═══
    ADD COLUMN IF NOT EXISTS payment_date           TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reconciliation_date    TIMESTAMP,
    ADD COLUMN IF NOT EXISTS bookkeeping_time       TIMESTAMP,

    -- ═══ 物流信息 ═══
    ADD COLUMN IF NOT EXISTS delivery_method        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_route         VARCHAR(200),
    ADD COLUMN IF NOT EXISTS delivery_route_id      BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_order_no      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_no            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS waybill_no             VARCHAR(200),
    ADD COLUMN IF NOT EXISTS logistics_company      VARCHAR(200),
    ADD COLUMN IF NOT EXISTS shipping_fee           DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS freight_payer          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cod_amount             DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS driver_name            VARCHAR(100),
    ADD COLUMN IF NOT EXISTS driver_id              BIGINT,
    ADD COLUMN IF NOT EXISTS delivery_vehicle       VARCHAR(100),

    -- ═══ 会员/积分 ═══
    ADD COLUMN IF NOT EXISTS member_card_no         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS member_name            VARCHAR(200),
    ADD COLUMN IF NOT EXISTS member_discount        DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS prev_points            DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_generated_points  DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_exchange_points   DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS member_used_points       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS current_points           DECIMAL(18,2) DEFAULT 0,

    -- ═══ 源单关联 ═══
    ADD COLUMN IF NOT EXISTS source_order           VARCHAR(200),
    ADD COLUMN IF NOT EXISTS source_order_id        BIGINT,

    -- ═══ 表头自定义字段（数字1-5） ═══
    ADD COLUMN IF NOT EXISTS ext_num1               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num2               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num3               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num4               DECIMAL(18,2),
    ADD COLUMN IF NOT EXISTS ext_num5               DECIMAL(18,2),

    -- ═══ 表头自定义字段（文本1-5） ═══
    ADD COLUMN IF NOT EXISTS ext_text1              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text2              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text3              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text4              VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ext_text5              VARCHAR(500),

    -- ═══ 表头自定义字段（关联） ═══
    ADD COLUMN IF NOT EXISTS ext_partner            BIGINT,
    ADD COLUMN IF NOT EXISTS ext_staff              BIGINT,
    ADD COLUMN IF NOT EXISTS ext_dept               BIGINT,

    -- ═══ 表尾自定义字段 ═══
    ADD COLUMN IF NOT EXISTS footer_ext_text1       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS footer_ext_text2       VARCHAR(500),

    -- ═══ 备注/摘要 ═══
    ADD COLUMN IF NOT EXISTS summary                VARCHAR(500),
    ADD COLUMN IF NOT EXISTS product_line_attr      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS internal_note          VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS buyer_remark           VARCHAR(500),

    -- ═══ 审批/审核 ═══
    ADD COLUMN IF NOT EXISTS approved_note          VARCHAR(500),
    ADD COLUMN IF NOT EXISTS auditor                VARCHAR(100),
    ADD COLUMN IF NOT EXISTS auditor_id             BIGINT,
    ADD COLUMN IF NOT EXISTS auditor_name           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS audit_time             TIMESTAMP,

    -- ═══ 提交相关 ═══
    ADD COLUMN IF NOT EXISTS submit_by              BIGINT,
    ADD COLUMN IF NOT EXISTS submit_time            TIMESTAMP,

    -- ═══ 打印时间 ═══
    ADD COLUMN IF NOT EXISTS print_time             TIMESTAMP,

    -- ═══ 标准审计字段 ═══
    ADD COLUMN IF NOT EXISTS create_by              BIGINT,
    ADD COLUMN IF NOT EXISTS creator_name           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS update_by              BIGINT,
    ADD COLUMN IF NOT EXISTS version                INTEGER DEFAULT 0;

-- ═══ 主表索引 ═══
CREATE INDEX IF NOT EXISTS idx_return_doc_dept_id ON erp_sale_return_doc(dept_id);
CREATE INDEX IF NOT EXISTS idx_return_doc_settle_status ON erp_sale_return_doc(settle_status);
CREATE INDEX IF NOT EXISTS idx_return_doc_sales_type ON erp_sale_return_doc(sales_type);
CREATE INDEX IF NOT EXISTS idx_return_doc_generate_type ON erp_sale_return_doc(generate_type);
CREATE INDEX IF NOT EXISTS idx_return_doc_create_by ON erp_sale_return_doc(create_by);

-- ═══ 列注释 ═══
COMMENT ON COLUMN erp_sale_return_doc.sales_type IS '销售类型: 正常销售/退货';
COMMENT ON COLUMN erp_sale_return_doc.generate_type IS '产生方式: 手工/退货申请/销售出库';
COMMENT ON COLUMN erp_sale_return_doc.settle_status IS '结算状态: 未结算/部分结算/已结算';
COMMENT ON COLUMN erp_sale_return_doc.bill_amount IS '本单金额';
COMMENT ON COLUMN erp_sale_return_doc.discount_bill_amount IS '折后金额';
COMMENT ON COLUMN erp_sale_return_doc.product_line_count IS '商品行数';
COMMENT ON COLUMN erp_sale_return_doc.return_quantity_total IS '退货总数量';
COMMENT ON COLUMN erp_sale_return_doc.source_order IS '源单名称';
COMMENT ON COLUMN erp_sale_return_doc.source_order_id IS '源单ID';
COMMENT ON COLUMN erp_sale_return_doc.creator_name IS '制单人姓名';
COMMENT ON COLUMN erp_sale_return_doc.auditor_name IS '审核人姓名';
COMMENT ON COLUMN erp_sale_return_doc.summary IS '摘要';

-- ═══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return_doc';

    RAISE NOTICE '=== V11.0.0 销售退货单主表完整字段扩展 ===';
    RAISE NOTICE '主表总字段数: %', col_count;
END $$;
