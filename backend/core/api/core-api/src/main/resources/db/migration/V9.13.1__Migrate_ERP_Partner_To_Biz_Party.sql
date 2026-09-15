-- V9.13.1 数据迁移：erp_partner → biz_party
-- 将旧版 erp_partner 表中存量数据迁移到标准 biz_party 表
-- 迁移完成后 erp_partner 表保留不动，标记为只读

-- ============================================================
-- 0. 补充 biz_party 表缺失字段（V8.7.0 初始表设计未包含）
-- ============================================================
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS short_name             VARCHAR(100);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS party_level            VARCHAR(100);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS current_debt           DECIMAL(18,2) DEFAULT 0;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS settlement_type        VARCHAR(30);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS settlement_days        INTEGER DEFAULT 0;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS registered_address     VARCHAR(500);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS unified_code           VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS business_license       VARCHAR(100);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS tax_number             VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS business_address       VARCHAR(500);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS fax                    VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS legal_person_phone     VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS business_contact       VARCHAR(100);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS business_contact_phone VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS finance_contact        VARCHAR(100);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS finance_contact_phone  VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS first_trade_date       DATE;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS last_trade_date        DATE;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS trade_count            INTEGER DEFAULT 0;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS trade_amount           DECIMAL(18,2) DEFAULT 0;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS status                 INTEGER DEFAULT 1;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS bank_name              VARCHAR(200);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS bank_account           VARCHAR(100);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS phone                  VARCHAR(50);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS email                  VARCHAR(200);
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS website                VARCHAR(200);

-- ============================================================
-- 1. 迁移 erp_partner → biz_party（仅迁移未被删除的记录）
-- ============================================================
INSERT INTO biz_party (
    id, party_code, party_name, short_name, party_type,
    category_id, party_level,
    credit_limit, current_debt,
    settlement_type, settlement_days,
    unified_code, business_license, tax_number,
    bank_name, bank_account,
    registered_address, business_address,
    phone, fax, email, website,
    legal_person, legal_person_phone,
    business_contact, business_contact_phone,
    finance_contact, finance_contact_phone,
    first_trade_date, last_trade_date,
    trade_count, trade_amount,
    status, remark,
    create_by, create_time, update_by, update_time, deleted
)
SELECT
    -- ID 保持不变（避免外键断裂）
    p.id,
    -- 编码/名称
    p.partner_code,
    p.partner_name,
    p.partner_short_name,
    -- party_type: String → Integer ('customer'→1, 'supplier'→2, 'logistics'→3, else→4)
    CASE lower(p.partner_type)
        WHEN 'customer'  THEN 1
        WHEN 'supplier'  THEN 2
        WHEN 'logistics' THEN 3
        ELSE 4
    END,
    -- category_id
    p.partner_category_id,
    -- party_level (从 partner_grade 表查询等级名称)
    pg.grade_name,
    -- 信用
    p.credit_limit,
    p.current_balance,
    -- settlement_type: String → Integer ('现结'→0, else→1)
    CASE WHEN p.settle_type = '现结' THEN 0 ELSE 1 END,
    p.credit_days,
    -- 证件
    p.unified_social_code,
    NULL,
    p.tax_id,
    -- 银行（erp_partner 无银行字段，填 NULL）
    NULL,
    NULL,
    -- 地址
    NULL,
    p.detail_address,
    -- 联系方式
    p.contact_phone,
    NULL,
    p.contact_email,
    p.company_website,
    -- 法人
    p.legal_person,
    NULL,
    -- 业务联系人
    p.contact_person,
    p.contact_phone,
    NULL,
    NULL,
    -- 交易信息
    p.first_order_time::date,
    p.last_order_time::date,
    p.total_order_count,
    p.total_order_amount,
    -- status: String → Integer ('ENABLED'→1, 'DISABLED'→0, else→1)
    CASE WHEN p.status = 'DISABLED' THEN 0 ELSE 1 END,
    p.remark,
    -- 审计
    p.create_by,
    p.create_time,
    p.update_by,
    p.update_time,
    p.deleted
FROM erp_partner p
LEFT JOIN erp_partner_grade pg ON p.partner_grade_id = pg.id AND pg.deleted = 0
WHERE p.deleted = 0
  -- 避免重复迁移：已存在的 biz_party 记录跳过
  AND NOT EXISTS (SELECT 1 FROM biz_party bp WHERE bp.id = p.id)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2. 补充：将 erp_partner 中也 biz_party 没有的记录插入
--    使用自增ID（如果上面因 ON CONFLICT 跳过的）
-- ============================================================

-- ============================================================
-- 3. 创建迁移日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_migration_log (
    id              BIGSERIAL PRIMARY KEY,
    source_table    VARCHAR(100) NOT NULL,
    target_table    VARCHAR(100) NOT NULL,
    source_id       BIGINT,
    target_id       BIGINT,
    status          VARCHAR(20) DEFAULT 'SUCCESS',
    error_msg       TEXT,
    migrate_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 记录迁移统计
DO $$
DECLARE
    v_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO v_count FROM erp_partner WHERE deleted = 0;
    INSERT INTO biz_migration_log (source_table, target_table, source_id, status)
    VALUES ('erp_partner', 'biz_party', v_count, 'SUCCESS');
END $$;

-- ============================================================
-- 4. 更新 CRM 桥接字段注释（md_partner_id 现在指向 biz_party）
-- ============================================================
COMMENT ON COLUMN crm_customer.md_partner_id IS '关联MD客户(biz_party)ID，CRM成交后提升为MD客户';
