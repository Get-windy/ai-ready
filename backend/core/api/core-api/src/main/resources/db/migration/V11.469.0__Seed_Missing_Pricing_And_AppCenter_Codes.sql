-- 补齐 7 个「上一批因判据缺陷而漏建」的权限码（E-01 修正批，2026-09-21）
--
-- 【为什么单独一个迁移】这不是新模块，而是**修工具之后暴露出来的缺口**：
--   生成器判"类级是否已有权限注解"时**没有剥注释** ⇒ 本仓 Javadoc 里写着
--   `{@code @SaCheckPermission}` 的控制器会被整类跳过（报"类级已有权限注解"），
--   于是 `PriceApprovalController` 的 8 个端点**从未被纳入建码**。
--   判据已在本轮修好（`strip_comments`），修好后重跑 `--apply` 就报出这批 missing：
--     · `pricing:approval:{view,list,detail,create,approve,reject}`（6 个，来自 8 个端点）
--     · `set:app-center:view`（3 个端点共用）
--   按铁律「码不在库就不插注解」，这 11 个端点此前仍是"登录后可访问"。
--
-- 【口径】动作词沿用该域既有写法（`pricing:` 域已有 15 个码；`set:` 域是「设置」模块前缀）。
--   `PriceApprovalController` 的审批用 `approve`/`reject` 两码分列，与
--   `purchase:order:approve`、`crm:contract:approve` 同口径（审批与驳回分开授权）。
--
-- 【id 号段】权限码 120000 起（工具槽位 slot=20）、角色关联 9710000 起。
--   实测 120000~120999 空闲；**9700000~9709999 已被 e2e 夹具占用 2 行**，故另开 9710000。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (120000, '定价审批查询', 'pricing:approval:list',   '/api/erp/pricing/approval/pending',          'GET',  1300),
 (120001, '定价审批统计', 'pricing:approval:view',   '/api/erp/pricing/approval/statistics',       'GET',  1301),
 (120002, '定价审批详情', 'pricing:approval:detail', '/api/erp/pricing/approval/{id}',             'GET',  1302),
 (120003, '定价审批申请', 'pricing:approval:create', '/api/erp/pricing/approval/apply',            'POST', 1303),
 (120004, '定价审批通过', 'pricing:approval:approve','/api/erp/pricing/approval/{id}/approve',     'PUT',  1304),
 (120005, '定价审批拒绝', 'pricing:approval:reject', '/api/erp/pricing/approval/{id}/reject',      'PUT',  1305),
 (120006, '应用中心概览', 'set:app-center:view',      '/api/set/app-center/overview',               'GET',  1306)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9710000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 120000 AND 120006
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
