-- =============================================================================
-- 「流程分析」（设置 → 工作流 → 流程分析，菜单 80611 / set:workflow-analysis）孤儿页收口
-- V11.417.0 · 2026-09-18
--
-- 【背景】页面 views/workflow/process-analysis.vue 一直无菜单入口（sys_menu 里
--   `component LIKE '%process-analysis%'` 实测 0 行；历史上三次挂过菜单
--   V6.3.0 id=51303 / V9.1.0 id=806 / V9.10.0 id=62503 均已删除，其中 62503 现被
--   「短信配置」占用），只能靠 router/dynamicRoutes.ts 的硬编码兜底 URL 直达打开。
--   裁定：保留页面 + 补齐菜单入口（挂在既有「设置 → 工作流」组 61206，与
--   801 流程定义 / 80610 流程设计 同组），并彻底清掉页面里写死的 2024 年 mock 数据。
--
-- 【本迁移做两件事】
--   ① 补菜单行（id 80611，parent_id 61206）——只加不改，不动 801 / 80610。
--      字段值逐列对照同组 80610 实测值（devdb，2026-09-18）：
--        parent_id=61206 / menu_type=1 / status=1 / deleted=0 / visible=1
--        tenant_id=0 / client_type='tenant-admin' / display_group=0 / display_mode=0
--        is_external=0 / is_cache=1 / route_name=NULL / redirect=NULL
--      ⚠️ 两处刻意与 80610 不同，理由如下：
--        · path 用带前导斜杠的 '/workflow/process-analysis'（80610 为 'set/workflow-designer'）：
--          transformMenuToRoutes 对以 '/' 开头的 path 会去掉前导斜杠 → 路由 '/workflow/process-analysis'，
--          与 sidebar 高亮逻辑（BasicLayout.findMenuCodeByPath 用 menu.path === route.path 比较）一致；
--          component 取 'views/workflow/process-analysis.vue'（该页真实位置，非 80610 的 designer）。
--        · menu_level 取 0（80610 为历史值 3）：0=租户级。SysMenuServiceImpl.getUserMegaMenus 对
--          「非系统租户 + 非超管」会过滤 `menu_level = 0`，取 3 会让普通租户永远看不到本菜单。
--      ⚠️ 未写 sys_tenant_menu / sys_role_menu 授权行：与同组 801 / 80610 实测口径一致
--        （两表的 menu_id IN (801,80610) 实测均为 0 行）——本模块「设置 → 工作流」组成员
--        目前仅对系统租户 + 超管可见（超管走 getAllMenusForSystemAdmin 全量返回）。
--        普通租户如需可见，由租户管理员在「系统 → 角色管理」按需勾选，本迁移不越权代配。
--
--   ② 补读端点权限码 workflow:analysis:view（本轮同一提交内给 WorkflowController 的两条
--      流程分析端点补 @SaCheckPermission）：GET /api/workflow/analysis/refresh 与
--      GET /api/workflow/analysis/report。命名口径沿用本控制器既有做法 workflow:<域>:<动作>，
--      与 V11.403.0 的 workflow:definition:* / V11.404.0 的 workflow:instance:view /
--      V11.405.0 的 workflow:task:view 完全同构。
--
-- 【id 号段纪律 —— 本仓 9xxxx 为多模块手工共用号段，并行会话已多次撞主键导致迁移失败】
--   落地前实测（devdb，2026-09-18，与本文件同一分钟内）：
--     SELECT count(*) FROM sys_menu           WHERE id = 80611                                  → 0
--     SELECT count(*) FROM sys_permission     WHERE id = 91393 OR permission_code =
--                                                  'workflow:analysis:view'                      → 0
--     SELECT count(*) FROM sys_role_permission WHERE id = 9139004                                  → 0
--   迁移版本号：落地前实测最大为 V11.416.0__Add_Scheduled_Task_Log_Creator_Id.sql，故取 V11.417.0。
--
-- 【幂等】菜单按 id 做 NOT EXISTS 守卫；权限码按 permission_code 做 NOT EXISTS 守卫；
--   授权按 (role_id, permission_id) 判重。可重复执行，不新增列 / 不新增表。
-- =============================================================================

-- ── ① 菜单行（设置 → 工作流 组 61206；sort=4 接在 801(2) / 80610(3) 之后） ──────────
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
                      menu_name, menu_code, menu_type, path, component, icon, sort,
                      visible, status, client_type, display_group, menu_level,
                      is_external, is_cache)
SELECT 80611, 0, 61206, 0, now(), now(),
       '流程分析', 'set:workflow-analysis', 1, '/workflow/process-analysis',
       'views/workflow/process-analysis.vue', 'BarChartOutlined', 4,
       1, 1, 'tenant-admin', 0, 0,
       0, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE id = 80611);

-- ── ② 权限码种子：流程分析查看（读端点，两条 GET 共用） ────────────────────────────
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type, api_path, method, sort, visible, status)
SELECT 91393, 1, 0, 0, now(), now(),
       '流程分析查看', 'workflow:analysis:view', 3, '/api/workflow/analysis/refresh', 'GET', 411, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'workflow:analysis:view');

-- ── ③ 授权给超级管理员角色（role_id = 1），口径同 V11.394.0 / V11.402.0 / V11.403.0 /
--       V11.404.0 / V11.405.0：只授超管，普通租户角色由租户管理员按需勾选 ──────────────
INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9139004, 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code = 'workflow:analysis:view'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
