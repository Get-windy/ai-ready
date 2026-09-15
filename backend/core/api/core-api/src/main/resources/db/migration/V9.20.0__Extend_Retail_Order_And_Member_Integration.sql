-- ============================================================
-- V9.20.0 零售单扩展：明细表、会员融合、默认散客
-- 行业最佳实践：B2B企业客户(party_level=ENTERPRISE)
--              B2C个人会员(party_level=MEMBER)
--              共用biz_party统一往来单位体系
-- ============================================================

-- 1. 零售单明细表
CREATE TABLE IF NOT EXISTS erp_retail_order_item (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    product_id          BIGINT,
    product_name        VARCHAR(200),
    item_code           VARCHAR(100),
    barcode             VARCHAR(100),
    unit                VARCHAR(20),
    available_stock     NUMERIC(18,2),
    batch_code          VARCHAR(100),
    production_date     DATE,
    shelf_life          VARCHAR(50),
    expiry_date         DATE,
    quantity            NUMERIC(18,2)   DEFAULT 0,
    unit_price          NUMERIC(18,2)   DEFAULT 0,
    amount              NUMERIC(18,2)   DEFAULT 0,
    big_pack            INTEGER         DEFAULT 0,
    mid_pack            INTEGER         DEFAULT 0,
    small_pack          INTEGER         DEFAULT 0,
    remark              VARCHAR(500),
    deleted             INTEGER         DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_retail_order_item_order ON erp_retail_order_item(order_id);

-- 2. 零售单主表扩展字段（POS收款、会员、仓库等）
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS warehouse_id     BIGINT;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS warehouse_name   VARCHAR(100);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS handler_id       BIGINT;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS handler_name     VARCHAR(100);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS order_date       DATE;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS sale_type        VARCHAR(20) DEFAULT 'NORMAL';
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS member_card_no   VARCHAR(50);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS member_name      VARCHAR(100);
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS direct_discount  NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS coupon_discount  NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS promo_discount   NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prev_points      INTEGER     DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS payable_amount   NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS cash_amount      NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS card_amount      NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prepaid_amount   NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS transfer_amount  NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS combined_payment BOOLEAN     DEFAULT false;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS change_amount    NUMERIC(18,2) DEFAULT 0;
ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prepaid_balance  NUMERIC(18,2) DEFAULT 0;

-- 3. biz_party 增加会员扩展字段（个人客户/会员专用）
--    企业客户 party_level=NULL 或 'ENTERPRISE'
--    个人会员 party_level='MEMBER'
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS phone         VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS member_card_no VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS birthday      DATE;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS points        INTEGER DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_biz_party_member_card ON biz_party(member_card_no) WHERE party_level = 'MEMBER';
CREATE INDEX IF NOT EXISTS idx_biz_party_phone ON biz_party(phone) WHERE party_level = 'MEMBER';

-- 4. 插入默认散客（Walk-in Customer）
--    用于零售单无指定会员时自动下账
INSERT INTO biz_party (party_code, party_name, short_name, party_type, party_level, status, tenant_id, remark)
SELECT 'WALKIN', '散客（零售默认）', '散客', 1, 'MEMBER', 1, 0,
       '系统预置散客，用于零售单无会员时自动关联。不可删除。'
WHERE NOT EXISTS (
    SELECT 1 FROM biz_party WHERE party_code = 'WALKIN' AND party_type = 1 AND deleted = 0
);
