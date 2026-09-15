-- ============================================================
-- V3.8.0: 权限模块安全增强
-- 1. 超级管理员唯一索引
-- 2. 审计日志增加 diff_data 字段
-- 3. 用户表增加 data_scope 字段
-- ============================================================

-- 1. 清理可能的重复超级管理员数据（保留 ID 最小的那个）
UPDATE sys_user SET is_super_admin = false
WHERE is_super_admin = true
  AND id NOT IN (
    SELECT MIN(id) FROM sys_user WHERE is_super_admin = true
  );

-- 创建超级管理员唯一索引（部分索引，仅对 is_super_admin = true 生效）
CREATE UNIQUE INDEX IF NOT EXISTS uk_super_admin ON sys_user (is_super_admin) WHERE is_super_admin = true;

COMMENT ON INDEX uk_super_admin IS '超级管理员唯一约束：全系统仅允许一个超级管理员';

-- 2. 操作日志表增加 diff_data 字段，用于记录权限变更的详细对比
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS diff_data TEXT;

COMMENT ON COLUMN sys_oper_log.diff_data IS '变更对比数据（JSON格式，记录修改前后的内容差异）';

-- 3. 用户表增加数据权限范围字段，允许用户级覆盖角色级 dataScope
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS data_scope VARCHAR(20) DEFAULT NULL;

COMMENT ON COLUMN sys_user.data_scope IS '数据权限范围（ALL-全数据 DEPT-本部门 DEPT_CHILD-本部门及子部门 SELF-仅本人）';
