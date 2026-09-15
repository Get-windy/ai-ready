-- V3.11.0__Add_Tenant_Admin_Unique.sql
-- 添加 is_tenant_admin 唯一约束，实现数据库级租户管理员保护
-- 每个租户最多只能有一个 is_tenant_admin = TRUE 的用户

-- 1. 添加 is_tenant_admin 列
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS is_tenant_admin BOOLEAN DEFAULT FALSE;

-- 2. 将现有租户的管理员标记为租户管理员
UPDATE sys_user SET is_tenant_admin = TRUE
WHERE id IN (SELECT admin_user_id FROM sys_tenant WHERE admin_user_id IS NOT NULL AND deleted = 0)
  AND deleted = 0;

-- 3. 创建唯一部分索引：每租户最多只能有一条记录的 is_tenant_admin = TRUE
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_admin ON sys_user (tenant_id, is_tenant_admin) WHERE is_tenant_admin = TRUE;

-- 4. 添加注释
COMMENT ON COLUMN sys_user.is_tenant_admin IS '是否租户管理员（每租户唯一，TRUE=是 FALSE=否）';
