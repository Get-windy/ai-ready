-- =============================================================================
-- 「我的待办 / 我的已办」（设置 → 审批，菜单 803 / 804）金标准落地
-- 2026-09-18
--
-- 背景（《我的待办开发文档》§5.5 / §5.8 / §9.1 P0-①③，《我的已办开发文档》§9.1 P0-②③）：
--   ① 前端 3 个 `v-permission` 用的是 workflow:task:view / :approve / :transfer，
--      而 devdb 实测（permission_code LIKE 'workflow%'）除 V11.398.0 播的
--      workflow:audit:list/:update（91321/91322）与 V11.404.0 播的 workflow:instance:* 外
--      **再无其它行** → 非超管用户在本页看不到「查看详情 / 审批 / 转办」任何行内操作，
--      而「已办」页更严重：它唯一的查看入口就是「查看详情」，被隐藏后整页无操作。
--   ② `workflow_task` 表**没有 priority 列**，后端 `WorkflowServiceImpl` 曾硬编码
--      `record.put("priority", "medium")`（分页与详情两处）→ 「优先级」列恒「中」、
--      「高优先级」统计卡恒 0，且前端的优先级查询条件因后端无列无参而**静默失效**。
--      本迁移新增**可空**列 workflow_task.priority（注意：本系统当前**没有任何**
--      优先级写入来源 —— 流程定义 / 节点 / 发起流程均未提供优先级录入），
--      因此存量行与新增行一律为 NULL；后端改为**如实返回该列的真实值**，
--      前端对空值显示「-」，不再造假。优先级筛选对该列真实生效。
--
-- 与后端方法级注解一一对应（本轮同一提交内给 WorkflowController 的端点补 @SaCheckPermission）：
--   GET  /api/workflow/task/page      → workflow:task:view
--   GET  /api/workflow/task/detail    → workflow:task:view
--   GET  /api/workflow/task/stat      → workflow:task:view
--   POST /api/workflow/task/approve   → workflow:task:approve
--   POST /api/workflow/task/transfer  → workflow:task:transfer
--
-- id 区间：permission 91390–91392 / role_permission 9139001–9139003
--   ⚠️ 号段纪律：本仓 9xxxx 为多模块手工共用号段，本轮已发生并行会话撞主键导致迁移失败
--      （见 V11.398.0 / V11.404.0 注释）。落地前已用 devdb 实测：
--      `SELECT count(*) FROM sys_permission WHERE id BETWEEN 91390 AND 91399` → 0；
--      `SELECT count(*) FROM sys_role_permission WHERE id BETWEEN 9139001 AND 9139010` → 0。
--
-- 幂等：权限种子用 WHERE NOT EXISTS（按 permission_code）防重复；列用 ADD COLUMN IF NOT EXISTS。
-- =============================================================================

-- ── 1. 任务优先级列（可空；当前无写入来源，全部为 NULL，前端显示「-」） ──
ALTER TABLE workflow_task ADD COLUMN IF NOT EXISTS priority VARCHAR(16);

COMMENT ON COLUMN workflow_task.priority IS
    '任务优先级（高/中/低；取值 high/medium/low）。可空：本系统暂无优先级录入来源，NULL = 未设置';

-- ── 2. 权限码种子 ──
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT v.id, 1, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
    (91390::BIGINT, '我的待办查看', 'workflow:task:view',     '/api/workflow/task/page',    'GET',  404),
    (91391::BIGINT, '我的待办审批', 'workflow:task:approve',  '/api/workflow/task/approve', 'POST', 405),
    (91392::BIGINT, '我的待办转办', 'workflow:task:transfer', '/api/workflow/task/transfer','POST', 406)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

-- 授权给超级管理员角色（role_id = 1）
-- 口径同 V11.362.0 / V11.394.0 / V11.398.0 / V11.404.0：超管另有 `*` 通配，此处只为让「权限清单」完整可审计；
-- 普通租户角色请由租户管理员在「系统 → 角色管理」中按需勾选（本迁移不越权代配）。
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9139000 + (p.sort - 403), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code IN ('workflow:task:view', 'workflow:task:approve', 'workflow:task:transfer')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
