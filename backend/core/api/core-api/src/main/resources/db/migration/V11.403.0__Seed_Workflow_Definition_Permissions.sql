-- =============================================================================
-- 「流程定义」（设置 → 工作流 → 流程定义，菜单 801 / workflow-definition）
-- 「流程设计」（设置 → 工作流 → 流程设计，菜单 80610 / set:workflow-designer）
-- 两页共用组件 views/workflow/designer/index.vue 的权限码种子
-- V11.403.0 · 2026-09-18
--
-- 【为什么需要本迁移】
--   《流程定义开发文档》§5.7 / §9.1 P0-③ 与《流程设计开发文档》§5.7 / §9.1 P0-④ 实测：
--     `SELECT permission_code FROM sys_permission WHERE permission_code LIKE 'workflow%'`
--     在本轮之前**只有 V11.398.0 播的 workflow:audit:list / :update 两条**，
--     而 WorkflowController 的流程定义端点当时**零方法级注解**（仅类级 @SaCheckLogin）
--     → 任意登录用户都能新建 / 保存 / 启停 / 删除流程定义（越权）。
--
-- 【与后端注解一一对应（本轮同一提交内为 WorkflowController 补的 7 处 @SaCheckPermission）】
--   GET    /api/workflow/definitions                              → workflow:definition:list
--   GET    /api/workflow/definitions/{definitionId}               → workflow:definition:list
--   POST   /api/workflow/definitions                              → workflow:definition:save
--   PUT    /api/workflow/definitions/{definitionId}               → workflow:definition:save
--   POST   /api/workflow/definitions/{definitionId}/publish       → workflow:definition:publish
--   POST   /api/workflow/definitions/{definitionId}/disable       → workflow:definition:disable
--   DELETE /api/workflow/definitions/{definitionId}               → workflow:definition:delete
--
-- 【命名口径】沿用本控制器既有做法 workflow:<域>:<动作>（V11.398.0 的 workflow:audit:*；
--   V11.404.0 的 workflow:instance:*），域段取本页语义 definition。
--
-- 【id 号段纪律 —— 本轮已发生并行会话撞主键导致迁移失败，落地前必须实测】
--   permission      : 91370 ~ 91374
--   role_permission : 9137001 ~ 9137005
--   实测（devdb，2026-09-18，本迁移落地前）：
--     SELECT id FROM sys_permission      WHERE id BETWEEN 91362 AND 91399  → 91380/91381/91382（V11.404.0 已占用），其余空闲
--     SELECT id FROM sys_role_permission WHERE id BETWEEN 9137000 AND 9137099 → 0 行
--     → 91370~91374 与 9137001~9137005 确认空闲（V11.404.0 用的是 91380~91382 / 9138001~9138003，不重叠）。
--
-- 【授权口径】只授权超级管理员角色（role_id = 1），与 V11.394.0 / V11.395.0 / V11.396.0 / V11.402.0 /
--   V11.404.0 同口径；普通租户角色由租户管理员在「系统 → 角色管理」按需勾选，本迁移不越权代配。
--   （超管另有 `*` 通配，此处补行只为让「权限清单」完整可审计。）
--
-- 【幂等】按 permission_code 做 NOT EXISTS 守卫；授权按 (role_id, permission_id) 判重；可重复执行。
-- 【本迁移不新增列 / 表】—— 流程定义所需的列（process_config / version / status 等）均已存在。
-- =============================================================================

-- ── ① 权限码种子 ─────────────────────────────────────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91370::BIGINT, '流程定义查询', 'workflow:definition:list',    '/api/workflow/definitions',                                    'GET',    406),
    (91371::BIGINT, '流程定义保存', 'workflow:definition:save',    '/api/workflow/definitions',                                    'POST',   407),
    (91372::BIGINT, '流程定义发布', 'workflow:definition:publish', '/api/workflow/definitions/{definitionId}/publish',             'POST',   408),
    (91373::BIGINT, '流程定义停用', 'workflow:definition:disable', '/api/workflow/definitions/{definitionId}/disable',             'POST',   409),
    (91374::BIGINT, '流程定义删除', 'workflow:definition:delete',  '/api/workflow/definitions/{definitionId}',                     'DELETE', 410)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- ── ② 授权给超级管理员角色（role_id = 1），已存在则跳过 ────────────────────────
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9137000 + (p.sort - 405), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN (
        'workflow:definition:list',
        'workflow:definition:save',
        'workflow:definition:publish',
        'workflow:definition:disable',
        'workflow:definition:delete')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
