-- 财务收付域权限码种子（E-04，2026-09-21）
--
-- 【为什么】payment 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器
--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**
--   （本仓历史事故，铁律：先补种子后补注解）。
--
-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController
--   16 端点 → 8 码）。本批：95 端点 → 37 码。
--
-- 【id 号段】权限码 106000 起（本模块独占槽位 slot=6）、角色关联 9560000 起。
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
 (106000, '财务收付capital-flow导出', 'finance:capital-flow:export', '/api/erp/capital-flow/export', 'GET', 1200),
 (106001, '财务收付capital-flow查询', 'finance:capital-flow:list', '/api/erp/capital-flow/page', 'GET', 1201),
 (106002, '财务收付capital-flow编辑', 'finance:capital-flow:update', '/api/erp/capital-flow/reconcile/{id}', 'PUT', 1202),
 (106003, '财务收付capital-flow查看', 'finance:capital-flow:view', '/api/erp/capital-flow/statistics', 'GET', 1203),
 (106004, '财务收付deposit-condition新增', 'finance:deposit-condition:create', '/api/erp/deposit-condition', 'POST', 1204),
 (106005, '财务收付deposit-condition详情', 'finance:deposit-condition:detail', '/api/erp/deposit-condition/pre-receipt/{preReceiptId}', 'GET', 1205),
 (106006, '财务收付deposit-condition查看', 'finance:deposit-condition:view', '/api/erp/deposit-condition/source', 'GET', 1206),
 (106007, '财务收付offset新增', 'finance:offset:create', '/api/erp/offset', 'POST', 1207),
 (106008, '财务收付offset详情', 'finance:offset:detail', '/api/erp/offset/{id}', 'GET', 1208),
 (106009, '财务收付offset查询', 'finance:offset:list', '/api/erp/offset/page', 'GET', 1209),
 (106010, '财务收付pre-payment新增', 'finance:pre-payment:create', '/api/erp/pre-payment/save', 'POST', 1210),
 (106011, '财务收付pre-payment详情', 'finance:pre-payment:detail', '/api/erp/pre-payment/{id}', 'GET', 1211),
 (106012, '财务收付pre-payment查询', 'finance:pre-payment:list', '/api/erp/pre-payment/next-no', 'GET', 1212),
 (106013, '财务收付pre-payment查看', 'finance:pre-payment:view', '/api/erp/pre-payment/advance-balance', 'GET', 1213),
 (106014, '财务收付pre-receipt新增', 'finance:pre-receipt:create', '/api/erp/pre-receipt/save', 'POST', 1214),
 (106015, '财务收付pre-receipt详情', 'finance:pre-receipt:detail', '/api/erp/pre-receipt/{id}', 'GET', 1215),
 (106016, '财务收付pre-receipt查询', 'finance:pre-receipt:list', '/api/erp/pre-receipt/next-no', 'GET', 1216),
 (106017, '财务收付pre-receipt查看', 'finance:pre-receipt:view', '/api/erp/pre-receipt/advance-balance', 'GET', 1217),
 (106018, '财务收付receipt审批', 'finance:receipt:approve', '/api/erp/receipt/{id}/approve', 'POST', 1218),
 (106019, '财务收付receipt新增', 'finance:receipt:create', '/api/erp/receipt/batch-confirm', 'POST', 1219),
 (106020, '财务收付receipt删除', 'finance:receipt:delete', '/api/erp/receipt/{id}/items/{itemId}', 'DELETE', 1220),
 (106021, '财务收付receipt详情', 'finance:receipt:detail', '/api/erp/receipt/{id}', 'GET', 1221),
 (106022, '财务收付receipt查询', 'finance:receipt:list', '/api/erp/receipt/page', 'GET', 1222),
 (106023, '财务收付receipt提交', 'finance:receipt:submit', '/api/erp/receipt/{id}/submit', 'POST', 1223),
 (106024, '财务收付receipt编辑', 'finance:receipt:update', '/api/erp/receipt/{id}', 'PUT', 1224),
 (106025, '财务收付receipt查看', 'finance:receipt:view', '/api/erp/receipt/page-detail', 'GET', 1225),
 (106026, '财务收付write-off详情', 'finance:write-off:detail', '/api/erp/write-off/{id}', 'GET', 1226),
 (106027, '财务收付write-off查询', 'finance:write-off:list', '/api/erp/write-off/page', 'GET', 1227),
 (106028, '财务收付审批', 'payment:approve', '/api/erp/payment/{id}/approve', 'POST', 1228),
 (106029, '财务收付新增', 'payment:create', '/api/erp/payment', 'POST', 1229),
 (106030, '财务收付删除', 'payment:delete', '/api/erp/payment/{id}/items/{itemId}', 'DELETE', 1230),
 (106031, '财务收付详情', 'payment:detail', '/api/erp/payment/{id}', 'GET', 1231),
 (106032, '财务收付导出', 'payment:export', '/api/erp/payment/export', 'GET', 1232),
 (106033, '财务收付查询', 'payment:list', '/api/erp/payment/page', 'GET', 1233),
 (106034, '财务收付提交', 'payment:submit', '/api/erp/payment/{id}/submit', 'POST', 1234),
 (106035, '财务收付编辑', 'payment:update', '/api/erp/payment/{id}', 'PUT', 1235),
 (106036, '财务收付查看', 'payment:view', '/api/erp/payment/page-detail', 'GET', 1236)
) AS v(id, name, code, api, method, sort)
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.permission_code = v.code);

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9560000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.id BETWEEN 106000 AND 106037
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
