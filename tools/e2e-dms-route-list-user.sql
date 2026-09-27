-- 配送路线单（配送 → 配送路线 → 配送路线单）E2E 专用验收账号
-- 避免与并行会话共用 admin / e2e_route 被 sa-token 互踢（页面伪装成"跳登录、断言全空"的假失败）
-- 用法：python tools/dbq.py "<本文件的 INSERT 语句>"；验收结束后执行文件末尾清理语句。
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_route_doc');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_route_doc');
DELETE FROM sys_user        WHERE username = 'e2e_route_doc';

-- 密码哈希复用 admin（即 admin123）
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000001200, 1, 0, now(), now(), 'e2e_route_doc', password, 'E2E配送路线单', 'E2E配送路线单',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000001291, 2099000000000001200, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000001292, 2099000000000001200, 1, true, 1, now(), now());

-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000001200;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000001200;
-- DELETE FROM sys_user        WHERE id = 2099000000000001200;
