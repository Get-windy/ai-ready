-- 物流公司 E2E 专用验收账号（避免与并行会话共用 admin 互相踢下线）
-- 用法：python tools/dbq.py "<本文件的 INSERT 语句>" 或直接执行本文件
-- 账号等权于 admin（role_id=1 SUPER_ADMIN），验收完成后应执行文件末尾的清理语句。
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_logistics');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_logistics');
DELETE FROM sys_user        WHERE username = 'e2e_logistics';

-- 密码哈希复用 admin（即 admin123），避免额外维护一套凭据
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000888, 1, 0, now(), now(), 'e2e_logistics', password, 'E2E物流', 'E2E物流',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000889, 2099000000000000888, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000890, 2099000000000000888, 1, true, 1, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000888;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000888;
-- DELETE FROM sys_user        WHERE id = 2099000000000000888;
