-- ============================================================
-- V9.6.3: 补齐仓储模块缺失的叶子菜单
--
-- 背景：
--   V9.6.2 恢复了 60301-60310 列组及部分叶子，但遗漏了 4 个叶子项。
--   本脚本补齐这些缺失项，并按设计文档调整排序。
--
-- 补齐项：
--   60301 其他出入库: 其他出库单
--   60303 生产:       生产模板
--   60304 库存预警:   预警查询
--   60305 借进借出:   借进借出查询
-- ============================================================

-- Step 1: 添加其他出库单 (60301 其他出入库, sort=1)
--         并将现有叶子 sort 调整为: 其他出库单=1, 其他入库单=2, 调拨单=3
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5001, 0, 60301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '其他出库单', 'erp:stock-out', 1,
   'erp/stock-out', 'erp/stock-out/index',
   'ExportOutlined', 1, 1, 1, 'tenant-admin', 1, 0, 0);

UPDATE sys_menu SET sort = 2, update_time = CURRENT_TIMESTAMP WHERE id = 5002;  -- 其他入库单
UPDATE sys_menu SET sort = 3, update_time = CURRENT_TIMESTAMP WHERE id = 5011;  -- 调拨管理

-- Step 2: 添加生产模板 (60303 生产, sort=1)
--         并将现有叶子 sort 调整为: 生产模板=1, 组装单=2, 拆卸单=3
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5014, 0, 60303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '生产模板', 'erp:stock-bom', 1,
   'erp/stock-bom', 'erp/stock-bom/index',
   'ProfileOutlined', 1, 1, 1, 'tenant-admin', 1, 0, 0);

UPDATE sys_menu SET sort = 2, update_time = CURRENT_TIMESTAMP WHERE id = 5015;  -- 组装管理
UPDATE sys_menu SET sort = 3, update_time = CURRENT_TIMESTAMP WHERE id = 5016;  -- 拆分管理

-- Step 3: 添加预警查询 (60304 库存预警, sort=1)
--         并将现有叶子 sort 调整为: 预警查询=1, 预警设置=2, 补货管理=3
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5017, 0, 60304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '预警查询', 'erp:alert-query', 1,
   'erp/alert-query', 'views/common/placeholder/index.vue',
   'AlertOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0);

UPDATE sys_menu SET sort = 2, update_time = CURRENT_TIMESTAMP WHERE id = 5013;  -- 库存预警(设置)
UPDATE sys_menu SET sort = 3, update_time = CURRENT_TIMESTAMP WHERE id = 5012;  -- 补货管理

-- Step 4: 添加借进借出查询 (60305 借进借出, sort=3)
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80018, 0, 60305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '借进借出查询', 'wh:borrow-query', 1,
   'wh/borrow-query', 'views/common/placeholder/index.vue',
   'SearchOutlined', 3, 1, 1, 'tenant-admin', 0, 0, 0);

-- Step 5: 验证
-- SELECT parent_id, COUNT(*) FROM sys_menu
-- WHERE parent_id IN (60301,60302,60303,60304,60305,60306,60307,60308,60309,60310,60311)
--   AND deleted = 0
-- GROUP BY parent_id ORDER BY parent_id;
