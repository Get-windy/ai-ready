-- 费用类型 E2E 专用验收账号（避免与并行会话共用 admin 互相踢下线）
-- 用法：psql -h localhost -U devuser -d devdb -f tools/e2e-expense-type-user.sql
-- 说明：账号等权于 admin（role_id=1 SUPER_ADMIN），验收完成后应执行文件末尾的清理语句。
-- 脚本 tools/e2e-expense-type.cjs 会优先用该账号登录，不可用时回退 admin。

DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_expense');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_expense');
DELETE FROM sys_user        WHERE username = 'e2e_expense';

-- 密码哈希复用 admin（即 admin123），避免额外维护一套凭据
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000009001, 1, 0, now(), now(), 'e2e_expense', password, 'E2E费用类型', 'E2E费用类型',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009002, 2099000000000009001, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009003, 2099000000000009001, 1, true, 1, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000009001;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000009001;
-- DELETE FROM sys_user        WHERE id = 2099000000000009001;
