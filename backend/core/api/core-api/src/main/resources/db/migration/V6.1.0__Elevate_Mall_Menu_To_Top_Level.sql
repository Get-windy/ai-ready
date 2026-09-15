-- ============================================================
-- V6.1.0: 将商城管理菜单从ERP子菜单提升为顶级导航菜单
--
-- 变更说明：
--   1. 查找 ERP管理 目录菜单
--   2. 查找其下的 商城管理 子菜单
--   3. 将 商城管理 的 parent_id 设为 0（顶级）
--   4. 更新 商城管理 的 path 为 'mall'
--   5. 同步更新子菜单的 path（移除 'erp/mall/' 前缀）
--
-- 影响范围：
--   - sys_menu: 商城管理菜单及其子菜单的 parent_id 和 path
--   - sys_role_menu: 不受影响（仍通过 menu_id 关联）
-- ============================================================

-- Step 1: 查找 ERP 管理目录菜单（兼容 menu_name 和 menu_code）
WITH erp_menu AS (
    SELECT id FROM sys_menu
    WHERE (menu_name = 'ERP管理' OR menu_code = 'erp')
      AND parent_id = 0
      AND client_type = 'pc-admin'
      AND deleted = 0
      AND status = 1
    LIMIT 1
),
-- Step 2: 查找商城管理子菜单
mall_menu AS (
    SELECT id, path FROM sys_menu
    WHERE (menu_name = '商城管理' OR menu_code IN ('erp:mall', 'mall'))
      AND parent_id = (SELECT id FROM erp_menu)
      AND client_type = 'pc-admin'
      AND deleted = 0
    LIMIT 1
)
-- Step 3: 将商城管理提升为顶级菜单
UPDATE sys_menu
SET parent_id = 0,
    path = CASE
        WHEN path LIKE 'erp/mall%' THEN 'mall'
        ELSE COALESCE(NULLIF(path, ''), 'mall')
    END,
    sort = 80,         -- 放在 ERP(10) / CRM(20) / WMS(30) / 采购/销售/库存 之后
    update_time = CURRENT_TIMESTAMP
WHERE id = (SELECT id FROM mall_menu);

-- Step 4: 更新子菜单的路由路径（移除 'erp/mall/' 前缀）
UPDATE sys_menu
SET path = REPLACE(path, 'erp/mall/', 'mall/'),
    update_time = CURRENT_TIMESTAMP
WHERE parent_id = (SELECT id FROM sys_menu
                   WHERE (menu_name = '商城管理' OR menu_code IN ('erp:mall', 'mall'))
                     AND parent_id = 0
                     AND client_type = 'pc-admin'
                     AND deleted = 0)
  AND client_type = 'pc-admin'
  AND deleted = 0;
