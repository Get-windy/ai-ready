-- 人力资源模块 E2E 专用账号（避免与并行会话/人工操作抢用 admin 造成 sa-token 互踢）
--
-- 背景：本仓库多个会话共用 `admin` 跑验收脚本。sa-token 对同一 loginId 的二次登录会**踢掉旧 token**，
--       表现为「页面跑到一半突然跳登录页 / 某个接口莫名 401」——极易被误判成页面缺陷。
--       故各模块 E2E 一律使用自己的专用账号（与 e2e-dms-*.sql / e2e-accounting-subject-user.sql 同口径）。
--
-- 用法：python tools/dbq.py "$(cat tools/e2e-hr-user.sql)"
--
-- 本文件创建 **3 个账号 + 1 个第二租户**，用途各不相同：
--   ① e2e_hr         （租户 1，角色 SUPER_ADMIN）—— 主力账号，跑全部 HR 断言（超管走 `*` 通配，免逐条授权）
--   ② e2e_hr_ta      （租户 1，角色 SYSTEM_ADMIN）—— **非超管**的租户管理员，用于断言「跨租户越权已被堵住」
--   ③ e2e_hr_t2      （租户 2，无角色）—— 「别的租户的账号」，作为越权断言的**靶子**，必须查不到
--   ④ tenant 2       —— 为靶子账号提供归属租户
--
-- ⚠️ 为什么必须单独造「非超管」账号：本库 `admin` / `e2e_hr` 都是 SUPER_ADMIN，
--    按「超管豁免」口径它们**本就应该**看到全部租户数据 —— 拿它们断言隔离是断言不出来的。
--
-- 密码统一沿用 admin 的哈希（保证与登录接口的校验方式一致），明文均为 admin123。

-- ── 幂等清理（先删干净再建） ──
DELETE FROM sys_user_role   WHERE user_id IN (2099000000000009001, 2099000000000009011, 2099000000000009021);
DELETE FROM sys_user_tenant WHERE user_id IN (2099000000000009001, 2099000000000009011, 2099000000000009021);
DELETE FROM sys_user        WHERE id IN (2099000000000009001, 2099000000000009011, 2099000000000009021);
DELETE FROM sys_tenant      WHERE id = 2;

-- ── ① 主力账号：租户 1 + 超级管理员角色（role_id = 1，permission 含 `*` 通配） ──
INSERT INTO sys_user (id, tenant_id, deleted, username, password, nickname, real_name,
                      user_type, status, is_super_admin, is_tenant_admin, create_time, update_time)
SELECT 2099000000000009001, 1, 0, 'e2e_hr', u.password, 'HR验收账号', 'HR验收账号',
       0, 1, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009002, 2099000000000009001, 1, 1, CURRENT_TIMESTAMP);

-- ── ④ 第二租户（靶子账号的归属），在靶子账号之前创建 ──
-- ⚠️ `status` 默认是 0（停用），**必须显式写 1**：否则该租户下的账号登录会报「租户不存在或已禁用」。
INSERT INTO sys_tenant (id, deleted, tenant_name, tenant_code, status)
VALUES (2, 0, 'E2E验收租户2', 'E2E_T2', 1);

-- ── ③ 靶子：租户 2 的账号（不给角色），越权断言里必须「查不到」 ──
INSERT INTO sys_user (id, tenant_id, deleted, username, password, nickname, real_name,
                      user_type, status, is_super_admin, is_tenant_admin, create_time, update_time)
SELECT 2099000000000009021, 2, 0, 'e2e_hr_t2', u.password, '租户2账号', '租户2账号',
       0, 1, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_user u WHERE u.username = 'admin';

-- ── ② 非超管租户管理员：角色 SYSTEM_ADMIN（role_code != SUPER_ADMIN，故不触发超管豁免） ──
--      SYSTEM_ADMIN 默认没有 system:user:list，这里补授权（幂等）
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 2099000000000009012, 2065122951570362369, p.id, 1, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'system:user:list'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 2065122951570362369 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 2099000000000009013, 2065122951570362369, p.id, 1, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'system:user:detail'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 2065122951570362369 AND rp.permission_id = p.id);

INSERT INTO sys_user (id, tenant_id, deleted, username, password, nickname, real_name,
                      user_type, status, is_super_admin, is_tenant_admin, create_time, update_time)
SELECT 2099000000000009011, 1, 0, 'e2e_hr_ta', u.password, '租户管理员验收账号', '租户管理员验收账号',
       0, 1, false, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_user u WHERE u.username = 'admin';

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009014, 2099000000000009011, 2065122951570362369, 1, CURRENT_TIMESTAMP);

-- ── 用户-租户归属（`sys_user_tenant`）──
-- ⚠️ 必须显式插入：`SysUserServiceImpl.login` 会校验 `isUserInTenant(userId, tenantId)`，
--    用的是这张关联表而不是 `sys_user.tenant_id`；缺行会直接报
--    「该用户不属于此租户，请检查租户名称」，账号建了也登不进去。
INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009003, 2099000000000009001, 1, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009015, 2099000000000009011, 1, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
VALUES (2099000000000009022, 2099000000000009021, 2, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 靶子账号也要能登录并调用 /user/page（否则拿到的是 403 而不是「范围被收敛」，断言会误判为失败）
--
-- ⚠️ 必须给**租户 2 自己的角色**，不能借用租户 1 的 SYSTEM_ADMIN：
--    角色是租户自有数据（`sys_role.tenant_id`），租户 2 的用户挂租户 1 的角色，
--    在租户条件生效后会查不出任何角色（实测 `/auth/userinfo` 返回 `"roles":[]`），
--    于是 `/user/page` 仍是 403 —— 这不是隔离缺陷，是夹具建模错了。
DELETE FROM sys_role_permission WHERE role_id = 2099000000000009031;
DELETE FROM sys_role            WHERE id = 2099000000000009031;

INSERT INTO sys_role (id, tenant_id, deleted, role_code, role_name, status, create_time)
VALUES (2099000000000009031, 2, 0, 'E2E_T2_ADMIN', 'E2E租户2管理员', 0, CURRENT_TIMESTAMP);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 2099000000000009032, 2099000000000009031, p.id, 2, CURRENT_TIMESTAMP
FROM sys_permission p WHERE p.permission_code = 'system:user:list';

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 2099000000000009033, 2099000000000009031, p.id, 2, CURRENT_TIMESTAMP
FROM sys_permission p WHERE p.permission_code = 'system:user:detail';

DELETE FROM sys_user_role WHERE user_id = 2099000000000009021;
INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
VALUES (2099000000000009023, 2099000000000009021, 2099000000000009031, 2, CURRENT_TIMESTAMP);

-- ⚠️ 执行本脚本后**若后端正在运行**，请重启后端（或等待权限缓存失效）再跑 E2E：
--    角色/权限走 L1 Caffeine(30s) + **L2 Redis** 两级缓存，新建的角色会在 Redis 里被
--    「空角色列表」缓存住，表现为 `/auth/userinfo` 返回 `"roles":[]`、受控端点 403。
--    实测：只删 L1 / 调 `/api/user-permission/cache/refresh/{id}` 都不够，**重启后端才干净**。
