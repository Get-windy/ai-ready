-- 定时任务（开发工具 → 定时任务，菜单 62405 `system:dev:scheduler`）E2E 专用账号
-- 用法：每次跑 E2E 前整文件执行一次（幂等：先按用户名清理再插入）
--
-- 设计：
--   · e2e_scheduler_job   角色 SUPER_ADMIN（超管通配 "*"）→ 全部接口可用
--   · e2e_scheduler_plain 角色 SYSTEM_ADMIN（**未**授予 system:dev:scheduler:*）→ 断言 403
--     （2026-09-14 复核：定时任务接口原先只有 @SaCheckLogin，任意登录用户都能下发执行目标）
-- 说明：密码哈希复用 admin（即 admin123）；两个账号均不参与其它套件。

-- ═══ ① 清理历史 ═══
DELETE FROM sys_user_role   WHERE user_id IN (SELECT id FROM sys_user WHERE username IN ('e2e_scheduler_job', 'e2e_scheduler_plain'));
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE username IN ('e2e_scheduler_job', 'e2e_scheduler_plain'));
DELETE FROM sys_user        WHERE username IN ('e2e_scheduler_job', 'e2e_scheduler_plain');
DELETE FROM scheduled_task_log WHERE task_id BETWEEN 2099000000000009700 AND 2099000000000009799;
DELETE FROM scheduled_task     WHERE id      BETWEEN 2099000000000009700 AND 2099000000000009799;

-- ═══ ② 超管验收账号 ═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000009600, 1, 0, now(), now(), 'e2e_scheduler_job', password, 'E2E定时任务', 'E2E定时任务',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009601, 2099000000000009600, 1, 1, now());

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009602, 2099000000000009600, 1, true, 1, now(), now());

-- ═══ ③ 普通角色验收账号（SYSTEM_ADMIN：无定时任务权限码）═══
INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                      user_type, is_super_admin, is_tenant_admin, status, data_scope)
SELECT 2099000000000009610, 1, 0, now(), now(), 'e2e_scheduler_plain', password, 'E2E定时任务普通', 'E2E定时任务普通',
       1, false, false, 1, 'ALL'
FROM sys_user WHERE username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
SELECT 2099000000000009611, 2099000000000009610, r.id, 1, now()
FROM sys_role r
WHERE r.role_code = 'SYSTEM_ADMIN'
  AND r.deleted = 0
LIMIT 1;

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009612, 2099000000000009610, 1, true, 1, now(), now());

-- ═══ ④ 清理（验收结束后执行）═══
-- DELETE FROM sys_user_role   WHERE user_id IN (2099000000000009600, 2099000000000009610);
-- DELETE FROM sys_user_tenant WHERE user_id IN (2099000000000009600, 2099000000000009610);
-- DELETE FROM sys_user        WHERE id IN (2099000000000009600, 2099000000000009610);
