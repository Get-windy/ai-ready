-- ============================================================
-- V9.46.0: 添加销售退货单表单路由
--
-- 为销售退货单添加表单页面路由，使其可以正常访问新增/编辑页面
-- ============================================================

-- 添加退货单表单路由（新增）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80603, 0, 70022, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '新增退货单', 'sales:return-doc:create', 1,
   'sales/return-doc/create', 'views/sales/return-doc/form.vue',
   NULL, 100, 0, 1, 'pc-admin', 0, 0, 1)
ON CONFLICT (id) DO UPDATE SET
  component = 'views/sales/return-doc/form.vue',
  update_time = CURRENT_TIMESTAMP;

-- 添加退货单表单路由（编辑/查看）
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80604, 0, 70022, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '编辑退货单', 'sales:return-doc:edit', 1,
   'sales/return-doc/form/:id', 'views/sales/return-doc/form.vue',
   NULL, 101, 0, 1, 'pc-admin', 0, 0, 1)
ON CONFLICT (id) DO UPDATE SET
  component = 'views/sales/return-doc/form.vue',
  update_time = CURRENT_TIMESTAMP;

-- 更新退货单列表菜单的component指向（确保使用完整实现的页面）
UPDATE sys_menu
SET component = 'views/sales/return-doc/index.vue',
    update_time = CURRENT_TIMESTAMP
WHERE id = 70022
  AND component LIKE '%placeholder%';

-- 为角色授权退货单表单菜单（使用系统管理员角色）
INSERT INTO sys_role_menu (id, role_id, menu_id, tenant_id, create_time)
SELECT
  900000500 + ROW_NUMBER() OVER (),
  r.id,
  m.id,
  0,
  CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN (VALUES (80603), (80604)) AS m(id)
WHERE r.role_code = 'admin'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = 0
  );

-- 为租户授权退货单表单菜单
INSERT INTO sys_tenant_menu (id, tenant_id, menu_id, create_time)
SELECT
  900000510 + ROW_NUMBER() OVER (),
  0,
  m.id,
  CURRENT_TIMESTAMP
FROM (VALUES (80603), (80604)) AS m(id)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_tenant_menu tm
  WHERE tm.tenant_id = 0 AND tm.menu_id = m.id AND tm.deleted = 0
);
