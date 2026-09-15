-- V3.10.0__Add_Super_Admin_Constraint.sql
-- 添加 is_super_admin 唯一约束，实现数据库级超级管理员保护

-- 1. 添加 is_super_admin 列
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS is_super_admin BOOLEAN DEFAULT FALSE;

-- 2. 将现有 user_type=0 的系统用户标记为超级管理员
UPDATE sys_user SET is_super_admin = TRUE WHERE user_type = 0 AND deleted = 0;

-- 3. 创建唯一部分索引：最多只能有一条记录的 is_super_admin = TRUE
CREATE UNIQUE INDEX IF NOT EXISTS uk_super_admin ON sys_user (is_super_admin) WHERE is_super_admin = TRUE;

-- 4. 添加注释
COMMENT ON COLUMN sys_user.is_super_admin IS '是否超级管理员（唯一，TRUE=是 FALSE=否）';
