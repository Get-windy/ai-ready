-- 其他往来单位 E2E 专用验收账号（避免与并行会话共用 admin 互相踢下线）
-- 用法：psql -h localhost -U devuser -d devdb -f tools/e2e-partner-user.sql
-- 说明：账号等权于 admin（role_id=1 SUPER_ADMIN），验收完成后执行文件末尾的清理语句。
-- 脚本 verify-md-partner.cjs / tools/e2e-partner.cjs 会优先用该账号登录，不可用时回退 admin。

DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_partner');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_partner');
DELETE FROM sys_user        WHERE username = 'e2e_partner';

-- 密码哈希复用 admin（即 admin123），避免额外维护一套凭据
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000801, 1, 0, now(), now(), 'e2e_partner', password, 'E2E其他往来单位', 'E2E其他往来单位',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000802, 2099000000000000801, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000803, 2099000000000000801, 1, true, 1, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000801;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000801;
-- DELETE FROM sys_user        WHERE id = 2099000000000000801;
