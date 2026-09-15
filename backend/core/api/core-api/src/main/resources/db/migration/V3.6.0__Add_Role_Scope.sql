-- ============================================================
-- 角色表添加 scope 字段
-- 支持双层 RBAC：PLATFORM（平台级角色） / TENANT（租户级角色）
-- ============================================================

ALTER TABLE sys_role
    ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'TENANT'
        CONSTRAINT ck_role_scope CHECK (scope IN ('PLATFORM', 'TENANT'));

COMMENT ON COLUMN sys_role.scope IS '角色作用域：PLATFORM-平台级角色 TENANT-租户级角色';
