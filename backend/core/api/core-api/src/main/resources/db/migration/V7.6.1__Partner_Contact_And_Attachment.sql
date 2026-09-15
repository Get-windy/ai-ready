-- =====================================================
-- V7.6.0: 往来单位联系人表 + 附件表
-- =====================================================

-- 1. 往来单位联系人表
CREATE TABLE IF NOT EXISTS erp_partner_contact (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL,
    contact_name    VARCHAR(100) NOT NULL,
    contact_phone   VARCHAR(50),
    contact_email   VARCHAR(100),
    position        VARCHAR(50),
    department      VARCHAR(100),
    is_default      INTEGER NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_contact IS '往来单位联系人表';
CREATE INDEX IF NOT EXISTS idx_pcontact_partner ON erp_partner_contact(partner_id);
CREATE INDEX IF NOT EXISTS idx_pcontact_tenant ON erp_partner_contact(tenant_id);

-- 2. 往来单位附件表
CREATE TABLE IF NOT EXISTS erp_partner_attachment (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    partner_id      BIGINT NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    file_type       VARCHAR(100),
    category        VARCHAR(20) DEFAULT 'DOC',
    sort_order      INTEGER DEFAULT 0,
    deleted         INTEGER NOT NULL DEFAULT 0,
    create_by       BIGINT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       BIGINT,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE erp_partner_attachment IS '往来单位附件表';
COMMENT ON COLUMN erp_partner_attachment.category IS 'IMAGE-图片 DOC-文档 CERT-证件';
CREATE INDEX IF NOT EXISTS idx_pattach_partner ON erp_partner_attachment(partner_id);
CREATE INDEX IF NOT EXISTS idx_pattach_tenant ON erp_partner_attachment(tenant_id);
