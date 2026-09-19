-- =============================================================================
-- 系统模块 → 开发工具 → API测试（菜单 62404）· 动作权限码种子
-- 2026-09-19
--
-- 【修的是什么】
--   本轮为该页补服务端安全兜底（allowlist / 凭据剥离 / 调用审计），新增端点：
--     POST /api/dev/api-test/send      发送受控出站请求
--     GET  /api/dev/api-test/policy    查询服务端实际生效的安全边界
--   两者都标了 @SaCheckPermission("system:dev:api-test:send")。
--
--   实测（devdb 2026-09-19）：sys_permission 中 `system:dev:api-test%` 命中 **0 行**
--   （`system:dev%` 前缀当时仅命中 system:dev:scheduler:* 系列）。
--   不补种子的话，除超管（UnifiedPermissionCacheService 给 `*` 通配）以外
--   所有角色的调用都会 403，且「角色-权限」配置页看不到这个码 —— 权限清单不完整、不可审计。
--
--   为什么必须有权限码（而不是依赖菜单可见性）：菜单 client_type='system-admin' 只决定
--   「谁看得见入口」。绕过页面直接打端点不受菜单约束 —— 没有权限码，任何登录用户都能把
--   服务端当跳板发起出站请求。权限码是动作层的门，菜单可见性只是入口的门。
--
-- 授权说明：按 V11.362.0 / V11.394.0 / V11.407.0 的既有做法，同时授权给超管角色（role_id = 1）。
--   超管另有 `*` 通配，这里只为让权限清单完整可审计；
--   普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
--
-- ⚠️ id 选择说明：刻意**避开** 9xxxx 手工号段（并行会话抢号严重，且 9xxxx 已被
--   data-permission/datasource/log/platform/system/tenant 等多批迁移占用到 91558）。
--   改用「菜单号派生」id（与 V11.400.0 的 70560*100+1 口径一致）：
--       sys_permission.id      = 62404 * 100 + 1   = 6240401
--       sys_role_permission.id = 62404 * 100 + 101 = 6240501
--   已核 devdb（2026-09-19）：两表在 6,240,000–6,250,000 区间均为 **0 行**，
--   也不会与雪花 id 段冲突。新增固定 id 的种子前务必重新实测占用。
--
-- 幂等：全部 WHERE NOT EXISTS，可重复执行。
-- =============================================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 6240401::BIGINT, 1, 0, 0, now(), now(),
       'API测试发送请求', 'system:dev:api-test:send', 3, '/api/dev/api-test/send', 'POST', 958, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = 'system:dev:api-test:send');

-- 授权给超级管理员角色（role_id = 1），已存在则跳过
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 6240501::BIGINT, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'system:dev:api-test:send'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
