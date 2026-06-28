-- ============================================================
-- V9.6.2: 恢复仓储模块丢失的列组和叶子菜单
--
-- 问题：仓储(60003)下应有 11 个列组，但实际只有 1 个（60311 质量管理）。
--       60301-60310 这 10 个列组在 V9.6.0 迁移过程中丢失（原因未明）。
--
-- 解决：恢复这些列组和相关的叶子菜单。
-- ============================================================

-- Step 1: 恢复 10 个列组
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, sort, visible, status, client_type, display_group, menu_level)
VALUES
  (60301, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他出入库', 'mega:wh:other-io', 0, 100, 1, 1, 'tenant-admin', 0, 0),
  (60302, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '盘点', 'mega:wh:stocktake', 0, 200, 1, 1, 'tenant-admin', 0, 0),
  (60303, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '生产', 'mega:wh:production', 0, 300, 1, 1, 'tenant-admin', 0, 0),
  (60304, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存预警', 'mega:wh:alert', 0, 400, 1, 1, 'tenant-admin', 0, 0),
  (60305, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借进借出', 'mega:wh:lend', 0, 500, 1, 1, 'tenant-admin', 0, 0),
  (60306, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收货作业', 'mega:wh:receiving', 0, 600, 1, 1, 'tenant-admin', 0, 0),
  (60307, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '上架作业', 'mega:wh:putaway', 0, 700, 1, 1, 'tenant-admin', 0, 0),
  (60308, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拣货作业', 'mega:wh:picking', 0, 800, 1, 1, 'tenant-admin', 0, 0),
  (60309, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货作业', 'mega:wh:shipping', 0, 900, 1, 1, 'tenant-admin', 0, 0),
  (60310, 0, 60003, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存作业', 'mega:wh:inventory', 0, 1000, 1, 1, 'tenant-admin', 0, 0);

-- Step 2: 恢复叶子菜单（占位页面，待后续开发）
-- 其他出入库(60301): 其他出库单、其他入库单、调拨单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5002, 0, 60301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '其他入库单', 'erp:stock-in', 1, 'erp/stock-in', 'erp/stock-in/index', 'InboxOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (5011, 0, 60301, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '调拨管理', 'erp:stock-transfer', 1, 'erp/stock-transfer', 'erp/stock-transfer/index', 'SwapOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0);

-- 盘点(60302): 报损单、报溢单、盘点单、成本调价单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5009, 0, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '报损管理', 'erp:stock-damage', 1, 'erp/stock-damage', 'erp/stock-damage/index', 'MinusCircleOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (5010, 0, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '溢余管理', 'erp:stock-overflow', 1, 'erp/stock-overflow', 'erp/stock-overflow/index', 'PlusCircleOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0),
  (5003, 0, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存盘点', 'erp:stocktake', 1, 'erp/stocktake', 'erp/stocktake/index', 'CheckSquareOutlined', 3, 1, 1, 'tenant-admin', 0, 0, 0),
  (5008, 0, 60302, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '成本调整', 'erp:stock-cost-adjust', 1, 'erp/stock-cost-adjust', 'erp/stock-cost-adjust/index', 'DollarOutlined', 4, 1, 1, 'tenant-admin', 0, 0, 0);

-- 生产(60303): 组装单、拆卸单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5015, 0, 60303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '组装管理', 'erp:stock-assemble', 1, 'erp/stock-assemble', 'erp/stock-assemble/index', 'ToolOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (5016, 0, 60303, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拆分管理', 'erp:stock-split', 1, 'erp/stock-split', 'erp/stock-split/index', 'ScissorOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0);

-- 库存预警(60304): 预警查询、预警设置、补货管理
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (5013, 0, 60304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '库存预警', 'erp:stock-alert-config', 1, 'erp/stock-alert-config', 'erp/stock-alert-config/index', 'AlertOutlined', 1, 1, 1, 'tenant-admin', 0, 0, 0),
  (5012, 0, 60304, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '补货管理', 'erp:stock-replenishment', 1, 'erp/stock-replenishment', 'erp/stock-replenishment/index', 'ShoppingCartOutlined', 2, 1, 1, 'tenant-admin', 0, 0, 0);

-- 借进借出(60305): 借进单、借出单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80010, 0, 60305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借进单', 'wh:borrow-in', 1, 'wh/borrow-in/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 1, 'tenant-admin', 1, 0, 0),
  (80011, 0, 60305, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '借出单', 'wh:borrow-out', 1, 'wh/borrow-out/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 1, 'tenant-admin', 1, 0, 0);

-- 收货作业(60306): 收货单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80012, 0, 60306, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '收货单', 'wh:receiving-order', 1, 'wh/receiving-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 1, 'tenant-admin', 1, 0, 0);

-- 上架作业(60307): 上架单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80013, 0, 60307, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '上架单', 'wh:putaway-order', 1, 'wh/putaway-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 1, 'tenant-admin', 1, 0, 0);

-- 拣货作业(60308): 拣货单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80014, 0, 60308, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '拣货单', 'wh:picking-order', 1, 'wh/picking-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 1, 'tenant-admin', 1, 0, 0);

-- 发货作业(60309): 发货单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80015, 0, 60309, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '发货单', 'wh:shipping-order', 1, 'wh/shipping-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 1, 'tenant-admin', 1, 0, 0);

-- 库存作业(60310): 移库单、盘点作业单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80016, 0, 60310, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '移库单', 'wh:move-order', 1, 'wh/move-order/form', 'views/common/placeholder/index.vue', NULL, 1, 1, 1, 'tenant-admin', 1, 0, 0),
  (80017, 0, 60310, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '盘点作业单', 'wh:inventory-order', 1, 'wh/inventory-order/form', 'views/common/placeholder/index.vue', NULL, 2, 1, 1, 'tenant-admin', 1, 0, 0);
