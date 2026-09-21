-- 预算域权限码种子（E-04，2026-09-21）
--
-- 【为什么】budget 域在 sys_permission 中没有任何码，而 E-01 要给该域控制器
--   补 @SaCheckPermission —— 没有码就直接补注解，会让这些接口对**所有非超管一律 403**
--   （本仓历史事故，铁律：先补种子后补注解）。
--
-- 【粒度】一码 = 一「资源 × 动作」，对齐既有约定（实测 PurchaseOrderController
--   16 端点 → 8 码）。本批：54 端点 → 36 码。
--
-- 【id 号段】权限码 100000 起、角色关联 9500000 起（2026-09-21 实测两段均空闲）。
--   9xxxx 段已被 V11.42x 系列的短块占满，故另开 100000 段。
--
-- 【必须关联超管角色】本仓超管权限来自 sys_role_permission，不关联则平台管理员自己也会被拒。
-- =============================================================

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
                            permission_name, permission_code, permission_type,
                            api_path, method, sort, visible, status)
VALUES
 (100000, 0, 0, 0, now(), now(), '预算调整审批', 'budget:adjustment:approve', 3, '/api/erp/budget/adjustment/{id}/approve', 'POST', 1200, 1, 0),
 (100001, 0, 0, 0, now(), now(), '预算调整新增', 'budget:adjustment:create', 3, '/api/erp/budget/adjustment', 'POST', 1201, 1, 0),
 (100002, 0, 0, 0, now(), now(), '预算调整删除', 'budget:adjustment:delete', 3, '/api/erp/budget/adjustment/{id}', 'DELETE', 1202, 1, 0),
 (100003, 0, 0, 0, now(), now(), '预算调整详情', 'budget:adjustment:detail', 3, '/api/erp/budget/adjustment/{id}', 'GET', 1203, 1, 0),
 (100004, 0, 0, 0, now(), now(), '预算调整导出', 'budget:adjustment:export', 3, '/api/erp/budget/adjustment/export', 'GET', 1204, 1, 0),
 (100005, 0, 0, 0, now(), now(), '预算调整查询', 'budget:adjustment:list', 3, '/api/erp/budget/adjustment/page', 'GET', 1205, 1, 0),
 (100006, 0, 0, 0, now(), now(), '预算调整提交', 'budget:adjustment:submit', 3, '/api/erp/budget/adjustment/{id}/submit', 'POST', 1206, 1, 0),
 (100007, 0, 0, 0, now(), now(), '预算调整编辑', 'budget:adjustment:update', 3, '/api/erp/budget/adjustment/{id}', 'PUT', 1207, 1, 0),
 (100008, 0, 0, 0, now(), now(), '预算年度审批', 'budget:annual:approve', 3, '/api/erp/budget/annual/batch-approve', 'POST', 1208, 1, 0),
 (100009, 0, 0, 0, now(), now(), '预算年度关闭', 'budget:annual:close', 3, '/api/erp/budget/annual/{id}/close', 'POST', 1209, 1, 0),
 (100010, 0, 0, 0, now(), now(), '预算年度新增', 'budget:annual:create', 3, '/api/erp/budget/annual', 'POST', 1210, 1, 0),
 (100011, 0, 0, 0, now(), now(), '预算年度删除', 'budget:annual:delete', 3, '/api/erp/budget/annual/{id}', 'DELETE', 1211, 1, 0),
 (100012, 0, 0, 0, now(), now(), '预算年度详情', 'budget:annual:detail', 3, '/api/erp/budget/annual/{id}', 'GET', 1212, 1, 0),
 (100013, 0, 0, 0, now(), now(), '预算年度执行', 'budget:annual:execute', 3, '/api/erp/budget/annual/{id}/start-exec', 'POST', 1213, 1, 0),
 (100014, 0, 0, 0, now(), now(), '预算年度导出', 'budget:annual:export', 3, '/api/erp/budget/annual/export', 'GET', 1214, 1, 0),
 (100015, 0, 0, 0, now(), now(), '预算年度查询', 'budget:annual:list', 3, '/api/erp/budget/annual/page', 'GET', 1215, 1, 0),
 (100016, 0, 0, 0, now(), now(), '预算年度打印', 'budget:annual:print', 3, '/api/erp/budget/annual/{id}/print', 'POST', 1216, 1, 0),
 (100017, 0, 0, 0, now(), now(), '预算年度提交', 'budget:annual:submit', 3, '/api/erp/budget/annual/batch-submit', 'POST', 1217, 1, 0),
 (100018, 0, 0, 0, now(), now(), '预算年度编辑', 'budget:annual:update', 3, '/api/erp/budget/annual/{id}', 'PUT', 1218, 1, 0),
 (100019, 0, 0, 0, now(), now(), '预算控制校验', 'budget:control:check', 3, '/api/erp/budget/control/check', 'POST', 1219, 1, 0),
 (100020, 0, 0, 0, now(), now(), '预算控制占用', 'budget:control:consume', 3, '/api/erp/budget/control/consume', 'POST', 1220, 1, 0),
 (100021, 0, 0, 0, now(), now(), '预算控制冻结', 'budget:control:freeze', 3, '/api/erp/budget/control/freeze', 'POST', 1221, 1, 0),
 (100022, 0, 0, 0, now(), now(), '预算控制解冻', 'budget:control:release', 3, '/api/erp/budget/control/release', 'POST', 1222, 1, 0),
 (100023, 0, 0, 0, now(), now(), '预算执行查询', 'budget:execution:list', 3, '/api/erp/budget/execution/rows', 'GET', 1223, 1, 0),
 (100024, 0, 0, 0, now(), now(), '预算明细详情', 'budget:item:detail', 3, '/api/erp/budget/item/{id}', 'GET', 1224, 1, 0),
 (100025, 0, 0, 0, now(), now(), '预算明细导出', 'budget:item:export', 3, '/api/erp/budget/item/export/{budgetId}', 'GET', 1225, 1, 0),
 (100026, 0, 0, 0, now(), now(), '预算明细查询', 'budget:item:list', 3, '/api/erp/budget/item/list-by-budget/{budgetId}', 'GET', 1226, 1, 0),
 (100027, 0, 0, 0, now(), now(), '预算明细编辑', 'budget:item:update', 3, '/api/erp/budget/item/{id}', 'PUT', 1227, 1, 0),
 (100028, 0, 0, 0, now(), now(), '预算报表查看', 'budget:report:view', 3, '/api/erp/budget/report/execution-summary', 'GET', 1228, 1, 0),
 (100029, 0, 0, 0, now(), now(), '预算模板新增', 'budget:template:create', 3, '/api/erp/budget/template', 'POST', 1229, 1, 0),
 (100030, 0, 0, 0, now(), now(), '预算模板删除', 'budget:template:delete', 3, '/api/erp/budget/template/{id}', 'DELETE', 1230, 1, 0),
 (100031, 0, 0, 0, now(), now(), '预算模板详情', 'budget:template:detail', 3, '/api/erp/budget/template/{id}', 'GET', 1231, 1, 0),
 (100032, 0, 0, 0, now(), now(), '预算模板导出', 'budget:template:export', 3, '/api/erp/budget/template/export', 'GET', 1232, 1, 0),
 (100033, 0, 0, 0, now(), now(), '预算模板查询', 'budget:template:list', 3, '/api/erp/budget/template/page', 'GET', 1233, 1, 0),
 (100034, 0, 0, 0, now(), now(), '预算模板发布', 'budget:template:publish', 3, '/api/erp/budget/template/{id}/publish', 'POST', 1234, 1, 0),
 (100035, 0, 0, 0, now(), now(), '预算模板编辑', 'budget:template:update', 3, '/api/erp/budget/template/{id}', 'PUT', 1235, 1, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time)
SELECT 9500000 + row_number() OVER (ORDER BY p.id), 1, p.id, 1, now()
FROM sys_permission p
WHERE p.permission_code LIKE 'budget:%' AND p.id BETWEEN 100000 AND 100036
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp
                  WHERE rp.role_id = 1 AND rp.permission_id = p.id);
