-- 物流发货 E2E 专用验收账号（避免与并行会话共用 admin 互相踢下线）
-- 用法：psql -h localhost -U devuser -d devdb -f tools/e2e-logistics-ship-user.sql
-- 说明：账号等权于 admin（role_id=1 SUPER_ADMIN）；脚本 tools/e2e-logistics-ship.cjs 首次运行会自动创建。
--
-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000981;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000981;
-- DELETE FROM sys_user        WHERE id = 2099000000000000981;

DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_logistics_ship');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_logistics_ship');
DELETE FROM sys_user        WHERE username = 'e2e_logistics_ship';

-- 密码哈希复用 admin（即 admin123）
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000981, 1, 0, now(), now(), 'e2e_logistics_ship', password, 'E2E物流发货', 'E2E物流发货',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000982, 2099000000000000981, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000983, 2099000000000000981, 1, true, 1, now(), now());
