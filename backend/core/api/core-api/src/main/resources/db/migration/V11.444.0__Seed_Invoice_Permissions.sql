-- 发票域权限码种子（E-04，2026-09-21）
--
-- 【为什么】invoice 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器
--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**
--   （本仓历史事故，铁律：先补种子后补注解）。
--
-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController
--   16 端点 → 8 码）。本批：42 端点 → 14 码。
--
-- 【id 号段】权限码 105000 起（本模块独占槽位 slot=5）、角色关联 9550000 起。
--   9xxxx 段已被 V11.42x 系列的短块占满，故另开 100000 段；
--   各模块用不同槽位避免多迁移撞主键（本仓 sys_permission.id 无序列默认值）。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
SELECT v.id, 0, 0, 0, now(), now(), v.name, v.code, 3, v.api, v.method, v.sort, 1, 0
FROM (VALUES
 (105000, '发票application审批', 'invoice:application:approve', '/api/erp/invoice/application/{id}/approve', 'POST', 1200),
 (105001, '发票application新增', 'invoice:application:create', '/api/erp/invoice/application', 'POST', 1201),
 (105002, '发票application删除', 'invoice:application:delete', '/api/erp/invoice/application/{id}', 'DELETE', 1202),
 (105003, '发票application详情', 'invoice:application:detail', '/api/erp/invoice/application/{id}', 'GET', 1203),
 (105004, '发票application查询', 'invoice:application:list', '/api/erp/invoice/application/list', 'GET', 1204),
 (105005, '发票application提交', 'invoice:application:submit', '/api/erp/invoice/application/{id}/submit', 'POST', 1205),
 (105006, '发票application编辑', 'invoice:application:update', '/api/erp/invoice/application/{id}', 'PUT', 1206),
 (105007, '发票application查看', 'invoice:application:view', '/api/erp/invoice/application/date-range', 'GET', 1207),
 (105008, '发票新增', 'invoice:create', '/api/erp/invoice/create-from-application', 'POST', 1208),
 (105009, '发票详情', 'invoice:detail', '/api/erp/invoice/{id}', 'GET', 1209),
 (105010, '发票导出', 'invoice:export', '/api/erp/invoice/{invoiceId}/download', 'GET', 1210),
 (105011, '发票查询', 'invoice:list', '/api/erp/invoice/list', 'GET', 1211),
 (105012, '发票编辑', 'invoice:update', '/api/erp/invoice/{invoiceId}', 'PUT', 1212),
 (105013, '发票查看', 'invoice:view', '/api/erp/invoice/query', 'GET', 1213)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9550000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 105000 AND 105014
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
