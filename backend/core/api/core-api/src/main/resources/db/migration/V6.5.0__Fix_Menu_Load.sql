-- ============================================================
-- V6.5.0: 修复菜单加载问题
--
-- 1. 将 SUPER_ADMIN 角色状态改为启用 (status=1)
-- 2. 确保 admin 用户角色关联正确
-- ============================================================

-- 修复 SUPER_ADMIN 角色状态：从禁用(0)改为启用(1)
UPDATE sys_role
SET status = 1
WHERE role_code = 'SUPER_ADMIN' AND status = 0;

-- 同时修复其他可能存在的默认角色状态
UPDATE sys_role
SET status = 1
WHERE id IN (SELECT role_id FROM sys_user_role WHERE user_id = 1)
AND status = 0;

-- 确保 admin 用户有正确的角色关联
-- 如果 sys_user_role 中没有记录，则插入
INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
SELECT 1, 1, 1, 1, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1
);

-- 确保 SUPER_ADMIN 角色存在且启用
INSERT INTO sys_role (id, tenant_id, role_code, role_name, role_type, scope, data_scope, status, deleted, create_time)
VALUES (1, 1, 'SUPER_ADMIN', '超级管理员', 0, 'PLATFORM', 0, 1, 0, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE SET
    status = 1,
    scope = 'PLATFORM';