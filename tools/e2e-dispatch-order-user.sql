-- 配送单 E2E 专用验收账号（避免与并行会话共用 admin 互相踢下线）
-- ⚠️ 2026-09-14：**请勿手工执行本文件**。脚本 tools/e2e-dispatch-order.cjs 自行开通账号
--    （动态取 id：`SELECT MAX(id)+1 FROM sys_user WHERE id BETWEEN 2099000000000002000 AND ...2999`）；
--    下方硬编码的 2099000000000000981 已过时 —— 该 id 现属《采购订货收货》E2E 的 `e2e_purrecv`，
--    直接执行会报「重复键违反唯一约束 sys_user_pkey」（2026-09-14 实踩）。
-- 说明：账号等权于 admin（role_id=1 SUPER_ADMIN）。
--
-- ⚠️ 清理（验收结束后执行）：
-- DELETE FROM sys_user_role   WHERE user_id = 2099000000000000981;
-- DELETE FROM sys_user_tenant WHERE user_id = 2099000000000000981;
-- DELETE FROM sys_user        WHERE id = 2099000000000000981;

DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dispatch_order');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_dispatch_order');
DELETE FROM sys_user        WHERE username = 'e2e_dispatch_order';

-- 密码哈希复用 admin（即 admin123）
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000000981, 1, 0, now(), now(), 'e2e_dispatch_order', password, 'E2E配送单', 'E2E配送单',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000000982, 2099000000000000981, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000000983, 2099000000000000981, 1, true, 1, now(), now());
