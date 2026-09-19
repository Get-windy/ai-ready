-- =============================================================================
-- 「流程实例」（设置 → 审批 → 流程实例，菜单 802 / workflow-instance）金标准落地
-- 2026-09-18
--
-- 背景（《流程实例开发文档》§5.6 / §9.1 P0-① / §12 P0①）：
--   本页前端 3 个 `v-permission` 用的是 workflow:instance:view / :diagram / :intervene，
--   而实测 devdb（2026-09-18）`SELECT permission_code FROM sys_permission WHERE permission_code LIKE 'workflow%'`
--   除 V11.398.0 刚播的 workflow:audit:list / :update（id 91321/91322）外**再无其它行**
--   → 非超管用户在本页三个行内操作（查看详情 / 流程图 / 流程干预）全部被
--   `directives/permission.ts` 置 `display:none`。本迁移补齐这 3 个权限码。
--
-- 与后端方法级注解一一对应（本轮同一提交内给 WorkflowController 的 5 个端点补 @SaCheckPermission）：
--   GET  /api/workflow/instance/page                        → workflow:instance:view
--   GET  /api/workflow/instance/detail                      → workflow:instance:view
--   GET  /api/workflow/instance/stat                        → workflow:instance:view
--   GET  /api/workflow/{instanceId}/records                 → workflow:instance:view（详情抽屉的审批记录时间线）
--   GET  /api/workflow/instance/diagram                     → workflow:instance:diagram
--   POST /api/workflow/instance/{instanceId}/intervene      → workflow:instance:intervene
--
-- id 区间：permission 91380–91382 / role_permission 9138001–9138003
--   ⚠️ 号段纪律：本仓 9xxxx 为多模块手工共用号段，本轮已发生并行会话撞主键导致迁移失败
--      （见 V11.398.0 注释）。落地前已用 devdb 实测：
--      `SELECT id FROM sys_permission WHERE id BETWEEN 91370 AND 91395` → 0 行；
--      `SELECT id FROM sys_role_permission WHERE id BETWEEN 9138000 AND 9138010` → 0 行。
--
-- 幂等：权限种子用 WHERE NOT EXISTS（按 permission_code）防重复；可重复执行。
-- =============================================================================

-- ── 权限码种子 ──
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91380::BIGINT, '流程实例查看', 'workflow:instance:view',      '/api/workflow/instance/page',                   'GET',  401),
    (91381::BIGINT, '流程实例流程图', 'workflow:instance:diagram',  '/api/workflow/instance/diagram',                'GET',  402),
    (91382::BIGINT, '流程实例干预', 'workflow:instance:intervene',  '/api/workflow/instance/{instanceId}/intervene', 'POST', 403)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1）
-- 口径同 V11.362.0 / V11.394.0 / V11.398.0：超管另有 `*` 通配，此处只为让「权限清单」完整可审计；
-- 普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9138000 + (p.sort - 400), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('workflow:instance:view', 'workflow:instance:diagram', 'workflow:instance:intervene')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
