-- 固定资产域权限码种子（E-04，2026-09-21）
--
-- 【为什么】fixedasset 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器
--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**
--   （本仓历史事故，铁律：先补种子后补注解）。
--
-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController
--   16 端点 → 8 码）。本批：50 端点 → 40 码。
--
-- 【id 号段】权限码 101000 起（本模块独占槽位 slot=1）、角色关联 9510000 起。
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
 (101000, '固定资产asset新增', 'fixed-asset:asset:create', '/api/erp/fixed-asset/asset', 'POST', 1200),
 (101001, '固定资产asset删除', 'fixed-asset:asset:delete', '/api/erp/fixed-asset/asset/{id}', 'DELETE', 1201),
 (101002, '固定资产asset详情', 'fixed-asset:asset:detail', '/api/erp/fixed-asset/asset/{id}', 'GET', 1202),
 (101003, '固定资产asset导出', 'fixed-asset:asset:export', '/api/erp/fixed-asset/asset/export', 'GET', 1203),
 (101004, '固定资产asset查询', 'fixed-asset:asset:list', '/api/erp/fixed-asset/asset/page', 'GET', 1204),
 (101005, '固定资产asset编辑', 'fixed-asset:asset:update', '/api/erp/fixed-asset/asset/{id}', 'PUT', 1205),
 (101006, '固定资产asset查看', 'fixed-asset:asset:view', '/api/erp/fixed-asset/asset/statistics', 'GET', 1206),
 (101007, '固定资产category新增', 'fixed-asset:category:create', '/api/erp/fixed-asset/category', 'POST', 1207),
 (101008, '固定资产category删除', 'fixed-asset:category:delete', '/api/erp/fixed-asset/category/{id}', 'DELETE', 1208),
 (101009, '固定资产category详情', 'fixed-asset:category:detail', '/api/erp/fixed-asset/category/{id}', 'GET', 1209),
 (101010, '固定资产category查询', 'fixed-asset:category:list', '/api/erp/fixed-asset/category/list', 'GET', 1210),
 (101011, '固定资产category编辑', 'fixed-asset:category:update', '/api/erp/fixed-asset/category/{id}', 'PUT', 1211),
 (101012, '固定资产depreciation新增', 'fixed-asset:depreciation:create', '/api/erp/fixed-asset/depreciation/batch-calculate', 'POST', 1212),
 (101013, '固定资产depreciation详情', 'fixed-asset:depreciation:detail', '/api/erp/fixed-asset/depreciation/{id}', 'GET', 1213),
 (101014, '固定资产depreciation查询', 'fixed-asset:depreciation:list', '/api/erp/fixed-asset/depreciation/page', 'GET', 1214),
 (101015, '固定资产disposal审批', 'fixed-asset:disposal:approve', '/api/erp/fixed-asset/disposal/{id}/approve', 'POST', 1215),
 (101016, '固定资产disposal新增', 'fixed-asset:disposal:create', '/api/erp/fixed-asset/disposal', 'POST', 1216),
 (101017, '固定资产disposal删除', 'fixed-asset:disposal:delete', '/api/erp/fixed-asset/disposal/{id}', 'DELETE', 1217),
 (101018, '固定资产disposal详情', 'fixed-asset:disposal:detail', '/api/erp/fixed-asset/disposal/{id}', 'GET', 1218),
 (101019, '固定资产disposal查询', 'fixed-asset:disposal:list', '/api/erp/fixed-asset/disposal/page', 'GET', 1219),
 (101020, '固定资产disposal编辑', 'fixed-asset:disposal:update', '/api/erp/fixed-asset/disposal/{id}', 'PUT', 1220),
 (101021, '固定资产inventory新增', 'fixed-asset:inventory:create', '/api/erp/fixed-asset/inventory', 'POST', 1221),
 (101022, '固定资产inventory详情', 'fixed-asset:inventory:detail', '/api/erp/fixed-asset/inventory/{id}', 'GET', 1222),
 (101023, '固定资产inventory查询', 'fixed-asset:inventory:list', '/api/erp/fixed-asset/inventory/page', 'GET', 1223),
 (101024, '固定资产inventory编辑', 'fixed-asset:inventory:update', '/api/erp/fixed-asset/inventory/{id}', 'PUT', 1224),
 (101025, '固定资产purchase审批', 'fixed-asset:purchase:approve', '/api/erp/fixed-asset/purchase/{id}/approve', 'POST', 1225),
 (101026, '固定资产purchase新增', 'fixed-asset:purchase:create', '/api/erp/fixed-asset/purchase', 'POST', 1226),
 (101027, '固定资产purchase删除', 'fixed-asset:purchase:delete', '/api/erp/fixed-asset/purchase/{id}', 'DELETE', 1227),
 (101028, '固定资产purchase详情', 'fixed-asset:purchase:detail', '/api/erp/fixed-asset/purchase/{id}', 'GET', 1228),
 (101029, '固定资产purchase查询', 'fixed-asset:purchase:list', '/api/erp/fixed-asset/purchase/page', 'GET', 1229),
 (101030, '固定资产purchase提交', 'fixed-asset:purchase:submit', '/api/erp/fixed-asset/purchase/{id}/submit', 'POST', 1230),
 (101031, '固定资产purchase编辑', 'fixed-asset:purchase:update', '/api/erp/fixed-asset/purchase/{id}', 'PUT', 1231),
 (101032, '固定资产purchase查看', 'fixed-asset:purchase:view', '/api/erp/fixed-asset/purchase/statistics', 'GET', 1232),
 (101033, '固定资产报表查看', 'fixed-asset:report:view', '/api/erp/fixed-asset/report/depreciation-summary', 'GET', 1233),
 (101034, '固定资产transfer审批', 'fixed-asset:transfer:approve', '/api/erp/fixed-asset/transfer/{id}/approve', 'POST', 1234),
 (101035, '固定资产transfer新增', 'fixed-asset:transfer:create', '/api/erp/fixed-asset/transfer', 'POST', 1235),
 (101036, '固定资产transfer删除', 'fixed-asset:transfer:delete', '/api/erp/fixed-asset/transfer/{id}', 'DELETE', 1236),
 (101037, '固定资产transfer详情', 'fixed-asset:transfer:detail', '/api/erp/fixed-asset/transfer/{id}', 'GET', 1237),
 (101038, '固定资产transfer查询', 'fixed-asset:transfer:list', '/api/erp/fixed-asset/transfer/page', 'GET', 1238),
 (101039, '固定资产transfer编辑', 'fixed-asset:transfer:update', '/api/erp/fixed-asset/transfer/{id}', 'PUT', 1239)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9510000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 101000 AND 101040
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
