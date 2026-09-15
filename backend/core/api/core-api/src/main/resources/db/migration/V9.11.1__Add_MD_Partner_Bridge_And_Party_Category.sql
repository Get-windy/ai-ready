-- V9.11.1 CRM→MD桥接字段 + 完善Party系统
-- 1. crm_customer 增加 md_partner_id 字段，关联 erp_partner
-- 2. 创建 biz_party_category 表（V8.7.0遗漏），从 erp_partner_category 迁移数据

-- ============================================================
-- 1. CRM客户表增加MD Partner桥接字段
-- ============================================================
ALTER TABLE crm_customer
    ADD COLUMN IF NOT EXISTS md_partner_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_crm_customer_md_partner ON crm_customer(md_partner_id);

-- ============================================================
-- 2. 创建业务伙伴分类表（Party Category）
-- ============================================================
CREATE TABLE IF NOT EXISTS biz_party_category (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    category_code   VARCHAR(50) NOT NULL,
    category_name   VARCHAR(200) NOT NULL,
    party_type      INTEGER DEFAULT 0,
    parent_id       BIGINT,
    level           INTEGER DEFAULT 0,
    sort_order      INTEGER DEFAULT 0,
    status          INTEGER DEFAULT 1,
    remark          VARCHAR(500),
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_biz_party_category_parent ON biz_party_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_biz_party_category_type ON biz_party_category(party_type);

-- 从 erp_partner_category 迁移数据（仅迁移未被删除的记录）
INSERT INTO biz_party_category (id, tenant_id, category_code, category_name, party_type, parent_id, level, sort_order, status, remark, create_by, create_time, update_by, update_time, deleted)
SELECT
    id, tenant_id, category_code, category_name,
    CASE category_type
        WHEN 'CUSTOMER'  THEN 1
        WHEN 'SUPPLIER'  THEN 2
        WHEN 'LOGISTICS' THEN 3
        ELSE 4
    END,
    parent_id, category_level, sort_order,
    CASE WHEN status IS NULL OR status = 1 THEN 1 ELSE 0 END,
    remark, create_by, create_time, update_by, update_time, deleted
FROM erp_partner_category
WHERE deleted = 0
ON CONFLICT (id) DO UPDATE SET
    category_code = EXCLUDED.category_code,
    category_name = EXCLUDED.category_name,
    party_type = EXCLUDED.party_type;
