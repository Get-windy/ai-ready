-- V8.7.0: Migrate partner data from erp_partner_* tables to biz_party_* tables
-- This script migrates all existing partner data to the new party management system

-- Create the new party tables if they don't exist
CREATE TABLE IF NOT EXISTS biz_party (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL DEFAULT 0,
    party_code          VARCHAR(50) NOT NULL,
    party_name          VARCHAR(200) NOT NULL,
    party_type          VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    party_status        VARCHAR(20) DEFAULT 'ENABLED',
    category_id         BIGINT,
    grade_id            BIGINT,

    -- Basic Info
    company_full_name   VARCHAR(200),
    unified_social_code VARCHAR(50),
    tax_id              VARCHAR(50),
    legal_person        VARCHAR(100),
    registered_capital  DECIMAL(20,2) DEFAULT 0,
    company_phone       VARCHAR(50),
    company_email       VARCHAR(200),
    company_website     VARCHAR(200),
    industry            VARCHAR(100),
    source_channel      VARCHAR(50),

    -- Address
    country             VARCHAR(50) DEFAULT '中国',
    province            VARCHAR(50),
    city                VARCHAR(50),
    district            VARCHAR(50),
    detail_address      VARCHAR(500),

    -- Financial
    credit_limit        DECIMAL(20,2) DEFAULT 0,
    credit_days         INTEGER DEFAULT 0,
    tax_rate            DECIMAL(5,2) DEFAULT 13.00,
    settle_type         VARCHAR(30) DEFAULT 'MONTHLY',
    opening_balance     DECIMAL(20,2) DEFAULT 0,
    opening_prepaid     DECIMAL(20,2) DEFAULT 0,

    -- Default Settings
    default_warehouse_id    BIGINT,
    default_handler_id      BIGINT,
    default_handler_name    VARCHAR(100),

    -- Other Info
    member_card_no      VARCHAR(50),
    member_name         VARCHAR(100),
    initial_points      INTEGER DEFAULT 0,
    operating_series    VARCHAR(100),
    operating_area      DECIMAL(20,2),

    -- System
    remark              VARCHAR(500),
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_by           BIGINT,
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by           BIGINT,
    update_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS biz_party_contact (
    id              BIGSERIAL PRIMARY KEY,
    party_id        BIGINT NOT NULL,
    contact_name    VARCHAR(100),
    position        VARCHAR(100),
    department      VARCHAR(100),
    phone           VARCHAR(50),
    mobile          VARCHAR(50),
    email           VARCHAR(200),
    wechat          VARCHAR(100),
    qq              VARCHAR(50),
    is_primary      INTEGER DEFAULT 0,
    contact_role    INTEGER,
    status          INTEGER DEFAULT 1,
    remark          VARCHAR(500),
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_party_role (
    id              BIGSERIAL PRIMARY KEY,
    role_name       VARCHAR(100) NOT NULL,
    role_code       VARCHAR(50) NOT NULL,
    description     VARCHAR(500),
    status          INTEGER DEFAULT 1,
    sort_order      INTEGER DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0
);

-- Migrate data from old erp_partner table to new biz_party table
INSERT INTO biz_party (
    id, tenant_id, party_code, party_name, party_type, party_status, category_id, grade_id,
    company_full_name, unified_social_code, tax_id, legal_person, registered_capital,
    company_phone, company_email, company_website, industry, source_channel,
    country, province, city, district, detail_address,
    credit_limit, credit_days, tax_rate, settle_type, opening_balance, opening_prepaid,
    default_warehouse_id, default_handler_id, default_handler_name,
    member_card_no, member_name, initial_points, operating_series, operating_area,
    remark, deleted, create_by, create_time, update_by, update_time
)
SELECT
    id, tenant_id, partner_code, partner_name, partner_type, status, partner_category_id, partner_grade_id,
    company_full_name, unified_social_code, tax_id, legal_person, registered_capital,
    company_phone, company_email, company_website, industry, source_channel,
    country, province, city, district, detail_address,
    credit_limit, credit_days, tax_rate, settle_type, opening_balance, opening_prepaid,
    default_warehouse_id, default_handler_id, default_handler_name,
    member_card_no, member_name, initial_points, operating_series, operating_area,
    remark, deleted, create_by, create_time, update_by, update_time
FROM erp_partner
WHERE deleted = 0
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    party_code = EXCLUDED.party_code,
    party_name = EXCLUDED.party_name,
    party_type = EXCLUDED.party_type,
    party_status = EXCLUDED.party_status,
    category_id = EXCLUDED.category_id,
    grade_id = EXCLUDED.grade_id,
    company_full_name = EXCLUDED.company_full_name,
    unified_social_code = EXCLUDED.unified_social_code,
    tax_id = EXCLUDED.tax_id,
    legal_person = EXCLUDED.legal_person,
    registered_capital = EXCLUDED.registered_capital,
    company_phone = EXCLUDED.company_phone,
    company_email = EXCLUDED.company_email,
    company_website = EXCLUDED.company_website,
    industry = EXCLUDED.industry,
    source_channel = EXCLUDED.source_channel,
    country = EXCLUDED.country,
    province = EXCLUDED.province,
    city = EXCLUDED.city,
    district = EXCLUDED.district,
    detail_address = EXCLUDED.detail_address,
    credit_limit = EXCLUDED.credit_limit,
    credit_days = EXCLUDED.credit_days,
    tax_rate = EXCLUDED.tax_rate,
    settle_type = EXCLUDED.settle_type,
    opening_balance = EXCLUDED.opening_balance,
    opening_prepaid = EXCLUDED.opening_prepaid,
    default_warehouse_id = EXCLUDED.default_warehouse_id,
    default_handler_id = EXCLUDED.default_handler_id,
    default_handler_name = EXCLUDED.default_handler_name,
    member_card_no = EXCLUDED.member_card_no,
    member_name = EXCLUDED.member_name,
    initial_points = EXCLUDED.initial_points,
    operating_series = EXCLUDED.operating_series,
    operating_area = EXCLUDED.operating_area,
    remark = EXCLUDED.remark,
    deleted = EXCLUDED.deleted,
    create_by = EXCLUDED.create_by,
    create_time = EXCLUDED.create_time,
    update_by = EXCLUDED.update_by,
    update_time = EXCLUDED.update_time;

-- Migrate data from old erp_partner_contact table to new biz_party_contact table
INSERT INTO biz_party_contact (
    id, party_id, contact_name, position, department,
    phone, mobile, email, is_primary, remark, create_by, create_time, update_by, update_time, deleted
)
SELECT
    id, partner_id, contact_name, position, department,
    contact_phone, contact_phone, contact_email, is_default, remark, create_by, create_time, update_by, update_time, deleted
FROM erp_partner_contact
WHERE deleted = 0
ON CONFLICT (id) DO UPDATE SET
    party_id = EXCLUDED.party_id,
    contact_name = EXCLUDED.contact_name,
    position = EXCLUDED.position,
    department = EXCLUDED.department,
    phone = EXCLUDED.phone,
    mobile = EXCLUDED.mobile,
    email = EXCLUDED.email,
    is_primary = EXCLUDED.is_primary,
    remark = EXCLUDED.remark,
    deleted = EXCLUDED.deleted,
    create_by = EXCLUDED.create_by,
    create_time = EXCLUDED.create_time,
    update_by = EXCLUDED.update_by,
    update_time = EXCLUDED.update_time;

-- Add additional fields to biz_party_contact that were added later
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_party_contact' AND column_name = 'region') THEN
        ALTER TABLE biz_party_contact ADD COLUMN region VARCHAR(100);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_party_contact' AND column_name = 'detail_address') THEN
        ALTER TABLE biz_party_contact ADD COLUMN detail_address VARCHAR(500);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_party_contact' AND column_name = 'delivery_method') THEN
        ALTER TABLE biz_party_contact ADD COLUMN delivery_method VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_party_contact' AND column_name = 'delivery_route') THEN
        ALTER TABLE biz_party_contact ADD COLUMN delivery_route VARCHAR(200);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'biz_party_contact' AND column_name = 'open_mall_account') THEN
        ALTER TABLE biz_party_contact ADD COLUMN open_mall_account INTEGER DEFAULT 0;
    END IF;
END $$;

-- Update the additional fields with data from old table where applicable
UPDATE biz_party_contact bpc
SET
    region = epc.region,
    detail_address = epc.detail_address,
    delivery_method = epc.delivery_method,
    delivery_route = epc.delivery_route,
    open_mall_account = epc.open_mall_account
FROM erp_partner_contact epc
WHERE bpc.id = epc.id;

-- Create indexes for the new tables
CREATE INDEX IF NOT EXISTS idx_biz_party_tenant ON biz_party(tenant_id);
CREATE INDEX IF NOT EXISTS idx_biz_party_type ON biz_party(party_type);
CREATE INDEX IF NOT EXISTS idx_biz_party_contact_party ON biz_party_contact(party_id);