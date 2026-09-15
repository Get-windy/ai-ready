-- V9.14.0 biz_party 列重构：精简冗余列 + 类型修正 + 地址子表
-- 从 69 列精简到 30 列，删除所有孤儿列和冗余列

-- ============================================================
-- 1. 删除地址列（迁移到 biz_party_address）
-- ============================================================
ALTER TABLE biz_party DROP COLUMN IF EXISTS registered_address;
ALTER TABLE biz_party DROP COLUMN IF EXISTS business_address;

-- ============================================================
-- 2. 删除业务联系人/财务联系人（使用 biz_party_contact）
-- ============================================================
ALTER TABLE biz_party DROP COLUMN IF EXISTS business_contact;
ALTER TABLE biz_party DROP COLUMN IF EXISTS business_contact_phone;
ALTER TABLE biz_party DROP COLUMN IF EXISTS finance_contact;
ALTER TABLE biz_party DROP COLUMN IF EXISTS finance_contact_phone;

-- ============================================================
-- 3. 删除交易统计列（从订单表实时计算）
-- ============================================================
ALTER TABLE biz_party DROP COLUMN IF EXISTS first_trade_date;
ALTER TABLE biz_party DROP COLUMN IF EXISTS last_trade_date;
ALTER TABLE biz_party DROP COLUMN IF EXISTS trade_count;
ALTER TABLE biz_party DROP COLUMN IF EXISTS trade_amount;

-- ============================================================
-- 4. 删除 V8.7.0 遗留孤儿列
-- ============================================================
ALTER TABLE biz_party DROP COLUMN IF EXISTS company_full_name;
ALTER TABLE biz_party DROP COLUMN IF EXISTS unified_social_code;
ALTER TABLE biz_party DROP COLUMN IF EXISTS tax_id;
ALTER TABLE biz_party DROP COLUMN IF EXISTS registered_capital;
ALTER TABLE biz_party DROP COLUMN IF EXISTS company_phone;
ALTER TABLE biz_party DROP COLUMN IF EXISTS company_email;
ALTER TABLE biz_party DROP COLUMN IF EXISTS company_website;
ALTER TABLE biz_party DROP COLUMN IF EXISTS industry;
ALTER TABLE biz_party DROP COLUMN IF EXISTS source_channel;
ALTER TABLE biz_party DROP COLUMN IF EXISTS country;
ALTER TABLE biz_party DROP COLUMN IF EXISTS province;
ALTER TABLE biz_party DROP COLUMN IF EXISTS city;
ALTER TABLE biz_party DROP COLUMN IF EXISTS district;
ALTER TABLE biz_party DROP COLUMN IF EXISTS detail_address;
ALTER TABLE biz_party DROP COLUMN IF EXISTS credit_days;
ALTER TABLE biz_party DROP COLUMN IF EXISTS tax_rate;
ALTER TABLE biz_party DROP COLUMN IF EXISTS settle_type;
ALTER TABLE biz_party DROP COLUMN IF EXISTS opening_balance;
ALTER TABLE biz_party DROP COLUMN IF EXISTS opening_prepaid;
ALTER TABLE biz_party DROP COLUMN IF EXISTS default_warehouse_id;
ALTER TABLE biz_party DROP COLUMN IF EXISTS default_handler_id;
ALTER TABLE biz_party DROP COLUMN IF EXISTS default_handler_name;
ALTER TABLE biz_party DROP COLUMN IF EXISTS member_card_no;
ALTER TABLE biz_party DROP COLUMN IF EXISTS member_name;
ALTER TABLE biz_party DROP COLUMN IF EXISTS initial_points;
ALTER TABLE biz_party DROP COLUMN IF EXISTS operating_series;
ALTER TABLE biz_party DROP COLUMN IF EXISTS operating_area;
ALTER TABLE biz_party DROP COLUMN IF EXISTS party_status;
ALTER TABLE biz_party DROP COLUMN IF EXISTS grade_id;

-- ============================================================
-- 5. 类型修正：party_type VARCHAR(30) → INTEGER
-- ============================================================
ALTER TABLE biz_party ALTER COLUMN party_type DROP DEFAULT;
ALTER TABLE biz_party ALTER COLUMN party_type TYPE INTEGER
    USING CASE
        WHEN party_type IS NULL OR party_type = '' THEN 1
        WHEN LOWER(party_type) = 'customer'  THEN 1
        WHEN LOWER(party_type) = 'supplier'  THEN 2
        WHEN LOWER(party_type) = 'logistics' THEN 3
        ELSE 4
    END;
ALTER TABLE biz_party ALTER COLUMN party_type SET DEFAULT 1;
ALTER TABLE biz_party ALTER COLUMN party_type SET NOT NULL;

-- ============================================================
-- 6. 类型修正：settlement_type VARCHAR(30) → INTEGER
-- ============================================================
ALTER TABLE biz_party ALTER COLUMN settlement_type TYPE INTEGER
    USING CASE
        WHEN settlement_type IS NULL OR settlement_type = '' THEN 0
        WHEN settlement_type = '0' OR settlement_type = '现结' THEN 0
        ELSE 1
    END;
ALTER TABLE biz_party ALTER COLUMN settlement_type SET DEFAULT 0;

-- ============================================================
-- 7. 统一默认值
-- ============================================================
ALTER TABLE biz_party ALTER COLUMN status         SET DEFAULT 1;
ALTER TABLE biz_party ALTER COLUMN current_debt   SET DEFAULT 0;
ALTER TABLE biz_party ALTER COLUMN settlement_days SET DEFAULT 0;
ALTER TABLE biz_party ALTER COLUMN credit_limit   SET DEFAULT 0;

-- ============================================================
-- 8. 创建地址子表
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_party_address (
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT NOT NULL,
    address_type    INTEGER DEFAULT 1,           -- 1=注册 2=营业 3=收货 4=发货
    contact_person  VARCHAR(100),
    contact_phone   VARCHAR(50),
    country         VARCHAR(50) DEFAULT '中国',
    province        VARCHAR(50),
    city            VARCHAR(50),
    district        VARCHAR(50),
    detail_address  VARCHAR(500),
    is_default      INTEGER DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_biz_party_address_party ON biz_party_address(party_id);

-- ============================================================
-- 9. 补充 biz_party 缺失索引
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_biz_party_tenant_status ON biz_party(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_biz_party_party_level  ON biz_party(party_level);

-- ============================================================
-- 10. 验证
-- ============================================================
DO $$
DECLARE
    v_col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_col_count
    FROM information_schema.columns
    WHERE table_name = 'biz_party';
    RAISE NOTICE 'biz_party refactored: % columns remaining', v_col_count;
END $$;
