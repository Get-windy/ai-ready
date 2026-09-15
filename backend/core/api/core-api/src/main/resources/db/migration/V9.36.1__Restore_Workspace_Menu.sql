-- ============================================================
-- V9.36.1: 恢复工作台菜单（50010）
--
-- 背景：
--   V9.36.0 误删了旧工作台菜单（50010），需要恢复以保持向后兼容
-- ============================================================

-- 恢复工作台菜单
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status,
    client_type, biz_flow_tag, display_group, menu_level)
VALUES
  (50010, 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '工作台', 'dashboard', 0, 'dashboard', NULL, 'DashboardOutlined', 10, 1, 1,
   'tenant-admin', NULL, 0, 0);

-- 恢复工作台的子菜单（叶子节点）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort, visible, status,
    client_type, biz_flow_tag, display_group, menu_level)
VALUES
  (50011, 0, 50010, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '工作台', 'dashboard:index', 1, 'dashboard', 'views/dashboard/index.vue',
   'DashboardOutlined', 100, 1, 1, 'tenant-admin', NULL, 0, 0);
