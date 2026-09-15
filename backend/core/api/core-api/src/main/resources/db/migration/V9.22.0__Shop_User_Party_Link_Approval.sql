-- ============================================================
-- V9.22.0 商城用户-企业身份关联（申请-审批-撤销机制）
-- 替代原有自动匹配 biz_party_contact 的不安全方式
-- ============================================================

CREATE TABLE IF NOT EXISTS shop_user_party_link (
    id            BIGSERIAL PRIMARY KEY,
    shop_user_id  BIGINT NOT NULL,
    party_id      BIGINT NOT NULL,              -- biz_party.id（企业客户）
    status        SMALLINT NOT NULL DEFAULT 0,  -- 0=待企业审批 1=待租户审批 2=已通过 3=已驳回 4=已撤销
    applied_phone VARCHAR(30),                  -- 申请时使用的手机号

    -- 企业客户管理员审批
    enterprise_approved_by   BIGINT,            -- 企业审批人（biz_party_contact.is_primary=1 的联系人对应的 shop_user）
    enterprise_approved_time TIMESTAMP,
    enterprise_remark        VARCHAR(255),

    -- 租户管理员审批
    tenant_approved_by       BIGINT,
    tenant_approved_time     TIMESTAMP,
    tenant_remark            VARCHAR(255),

    -- 撤销
    revoked_by               BIGINT,            -- 撤销人（企业管理员 或 租户管理员）
    revoked_time             TIMESTAMP,
    revoke_reason            VARCHAR(255),

    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted     SMALLINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_link_user_party UNIQUE (shop_user_id, party_id)
);

COMMENT ON TABLE shop_user_party_link IS '商城用户-企业身份关联（申请审批制）';
COMMENT ON COLUMN shop_user_party_link.status IS '状态: 0=待企业审批 1=待租户审批 2=已通过 3=已驳回 4=已撤销';

CREATE INDEX IF NOT EXISTS idx_link_user ON shop_user_party_link(shop_user_id);
CREATE INDEX IF NOT EXISTS idx_link_party ON shop_user_party_link(party_id);
CREATE INDEX IF NOT EXISTS idx_link_status ON shop_user_party_link(status);
