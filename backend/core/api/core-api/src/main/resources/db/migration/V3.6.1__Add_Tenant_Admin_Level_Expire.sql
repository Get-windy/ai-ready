-- ============================================================
-- 租户表添加 admin_user_id, level, expire_time 字段
-- 支持租户管理员分配、等级管理和过期控制
-- ============================================================

ALTER TABLE sys_tenant
    ADD COLUMN IF NOT EXISTS admin_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS level VARCHAR(30) NOT NULL DEFAULT 'basic'
        CONSTRAINT ck_tenant_level CHECK (level IN ('basic', 'professional', 'enterprise')),
    ADD COLUMN IF NOT EXISTS expire_time TIMESTAMP;

COMMENT ON COLUMN sys_tenant.admin_user_id IS '租户管理员用户ID';
COMMENT ON COLUMN sys_tenant.level IS '租户等级：basic-基础版 professional-专业版 enterprise-企业版';
COMMENT ON COLUMN sys_tenant.expire_time IS '租户到期时间';
