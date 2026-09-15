-- ============================================================
-- V11.24.0: 补齐 erp-finance 模块缺失的权限种子数据
--
-- 背景：
--   以下权限串在后端 @PreAuthorize 中使用，但 sys_permission 未配置，
--   非超管用户会被 403 拒绝：
--     finance:reconciliation:view/create/update/delete/reconcile (对账)
--     finance:other-income-doc:view/create/update/approve/delete (其他收入单)
--     finance:month-closing:view/execute/reopen (月结)
--     finance:period:view/create/update (会计期间)
--
--   同时补齐 sys_menu 中缺失的「对账」叶子菜单
--   (前端路由 finance/reconciliation/index 已存在，挂在 60604 账务处理 下)。
--
-- 说明：
--   - permission_type=3 (按钮/API权限)，status/visible 与现有 finance 行一致
--   - ID 使用 90xxx 固定号段（现有数据均为雪花ID，不会冲突）
--   - 全部幂等：按 permission_code / menu_code + tenant_id 守卫，可重复执行
-- ============================================================

-- ----------------------------------------------------------
-- Step 1: sys_permission — finance:reconciliation:* (对账)
-- ----------------------------------------------------------
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90001, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '对账查询', 'finance:reconciliation:view', 3, '/api/erp/finance/reconciliation/view', 'GET', 201, 1, 0, '对账详情/列表/统计'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:reconciliation:view' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90002, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '对账创建', 'finance:reconciliation:create', 3, '/api/erp/finance/reconciliation/create', 'POST', 202, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:reconciliation:create' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90003, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '对账编辑', 'finance:reconciliation:update', 3, '/api/erp/finance/reconciliation/update', 'PUT', 203, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:reconciliation:update' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90004, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '对账删除', 'finance:reconciliation:delete', 3, '/api/erp/finance/reconciliation/delete', 'DELETE', 204, 1, 0, '含批量删除'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:reconciliation:delete' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90005, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '对账执行', 'finance:reconciliation:reconcile', 3, '/api/erp/finance/reconciliation/reconcile', 'POST', 205, 1, 0, '执行对账/差异处理'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:reconciliation:reconcile' AND tenant_id = 0);

-- ----------------------------------------------------------
-- Step 2: sys_permission — finance:other-income-doc:* (其他收入单)
-- ----------------------------------------------------------
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90011, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '其他收入查询', 'finance:other-income-doc:view', 3, '/api/erp/finance/other-income-doc/view', 'GET', 211, 1, 0, '分页/详情'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:other-income-doc:view' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90012, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '其他收入创建', 'finance:other-income-doc:create', 3, '/api/erp/finance/other-income-doc/create', 'POST', 212, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:other-income-doc:create' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90013, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '其他收入编辑', 'finance:other-income-doc:update', 3, '/api/erp/finance/other-income-doc/update', 'PUT', 213, 1, 0, '含提交/取消'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:other-income-doc:update' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90014, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '其他收入审核', 'finance:other-income-doc:approve', 3, '/api/erp/finance/other-income-doc/approve', 'POST', 214, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:other-income-doc:approve' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90015, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '其他收入删除', 'finance:other-income-doc:delete', 3, '/api/erp/finance/other-income-doc/delete', 'DELETE', 215, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:other-income-doc:delete' AND tenant_id = 0);

-- ----------------------------------------------------------
-- Step 3: sys_permission — finance:month-closing:* (月结)
-- ----------------------------------------------------------
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90021, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '月结查询', 'finance:month-closing:view', 3, '/api/erp/finance/month-closing/status', 'GET', 221, 1, 0, '状态/日志'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:month-closing:view' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90022, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '月结执行', 'finance:month-closing:execute', 3, '/api/erp/finance/month-closing/execute', 'POST', 222, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:month-closing:execute' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90023, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '月结反结', 'finance:month-closing:reopen', 3, '/api/erp/finance/month-closing/reopen', 'POST', 223, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:month-closing:reopen' AND tenant_id = 0);

-- ----------------------------------------------------------
-- Step 4: sys_permission — finance:period:* (会计期间)
-- ----------------------------------------------------------
INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90031, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '会计期间查询', 'finance:period:view', 3, '/api/erp/finance/period/list', 'GET', 231, 1, 0, '分页/列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:period:view' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90032, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '会计期间创建', 'finance:period:create', 3, '/api/erp/finance/period/create', 'POST', 232, 1, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:period:create' AND tenant_id = 0);

INSERT INTO sys_permission (id, tenant_id, parent_id, deleted, create_time, update_time,
    permission_name, permission_code, permission_type, api_path, method, sort, visible, status, remark)
SELECT 90033, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '会计期间编辑', 'finance:period:update', 3, '/api/erp/finance/period/update', 'PUT', 233, 1, 0, '含状态变更'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'finance:period:update' AND tenant_id = 0);

-- ----------------------------------------------------------
-- Step 5: sys_menu — 「对账」叶子菜单 (60604 账务处理, sort=3)
--         前端路由 finance/reconciliation/index 已存在
-- ----------------------------------------------------------
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
SELECT 80122, 0, 60604, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    '对账', 'finance:reconciliation', 1,
    'finance/reconciliation', 'finance/reconciliation/index', 3,
    1, 1, 'tenant-admin', 0, 0, 3
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'finance:reconciliation' AND tenant_id = 0);

-- ----------------------------------------------------------
-- 验证:
-- SELECT permission_code FROM sys_permission
-- WHERE permission_code LIKE 'finance:reconciliation:%'
--    OR permission_code LIKE 'finance:other-income-doc:%'
--    OR permission_code LIKE 'finance:month-closing:%'
--    OR permission_code LIKE 'finance:period:%'
-- ORDER BY permission_code;
-- SELECT id, parent_id, menu_name, menu_code FROM sys_menu WHERE menu_code = 'finance:reconciliation';
-- ============================================================
