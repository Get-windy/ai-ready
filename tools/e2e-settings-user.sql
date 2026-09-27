-- 设置模块 E2E 专用账号（避免与并行会话共用 admin 造成 sa-token 互踢）
-- 用法：跑 E2E 前执行一次。密码与 admin 相同（直接复制其 BCrypt 哈希）。
--
-- 为什么必须独立账号：sa-token 配了 `is-concurrent: false`（新登录踢掉旧会话），
-- 而本项目常有多会话并行在同一 dev 库上跑验收（设置模块 / 系统模块），
-- 共用 admin 时双方会不停互踢，UI 侧表现为「每个页面都被重定向到登录页」——
-- 看起来像页面缺陷，其实是会话被顶替（2026-09-18 实踩）。
--
-- 账号行为与 admin 一致（挂 role_id = 1），故「超管会话整体豁免多租户」的口径不变。

-- ═══ ① 清理历史 ═══
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_settings');
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'e2e_settings');
DELETE FROM sys_user        WHERE username = 'e2e_settings';

-- ═══ ② 验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000020000, 1, 0, now(), now(), 'e2e_settings', password, 'E2E设置', 'E2E设置',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000020001, 2099000000000020000, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000020002, 2099000000000020000, 1, true, 1, now(), now());
