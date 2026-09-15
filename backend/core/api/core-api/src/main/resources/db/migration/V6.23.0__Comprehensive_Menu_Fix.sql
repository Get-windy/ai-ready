-- =====================================================
-- V6.23.0: Comprehensive Menu Fix
-- Date: 2026-06-19
-- Changes:
--   1. Rename client_type 'pc-admin' -> 'tenant-admin'
--   2. Move field visit from Sales to CRM
--   3. Move platform items from Settings to System
--   4. Convert workspace to leaf menu
--   5. Clean duplicates (print template, assets, analytics)
--   6. Add missing system module items
--   7. Move misplaced receiving business column
-- =====================================================

-- =====================
-- 1. Rename client_type: pc-admin -> tenant-admin for tenant-level menus
-- =====================
UPDATE sys_menu SET client_type = 'tenant-admin', update_time = NOW()
WHERE client_type = 'pc-admin' AND deleted = 0;

-- System-level menus (module 60013 and children) use system-admin
UPDATE sys_menu SET client_type = 'system-admin', update_time = NOW()
WHERE (id = 60013 OR parent_id = 60013 OR parent_id = 61307
    OR parent_id IN (SELECT id FROM sys_menu WHERE parent_id = 60013))
    AND deleted = 0;

-- =====================
-- 2. Move field visit (60101) from Sales(60001) to CRM(60007)
-- =====================
UPDATE sys_menu SET parent_id = 60007, sort = 150,
    menu_code = 'mega:crm:visit', update_time = NOW()
WHERE id = 60101;

-- =====================
-- 3. Create "系统管理" column in System(60013)
--    Move platform-level items from Settings→系统配置
-- =====================
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type,
    sort, visible, status, deleted, client_type, menu_level, create_time, update_time)
VALUES (61307, 0, 60013, '系统管理', 'mega:sys:admin', 0, 700, 1, 1, 0, 'tenant-admin', 0, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;

UPDATE sys_menu SET parent_id = 61307, sort = 100, update_time = NOW() WHERE id = 51605;
UPDATE sys_menu SET parent_id = 61307, sort = 200, update_time = NOW() WHERE id = 51606;
UPDATE sys_menu SET parent_id = 61307, sort = 300, update_time = NOW() WHERE id = 51609;
UPDATE sys_menu SET parent_id = 61307, sort = 400, update_time = NOW() WHERE id = 51610;
UPDATE sys_menu SET parent_id = 61307, sort = 500, update_time = NOW() WHERE id = 51612;
UPDATE sys_menu SET parent_id = 61307, sort = 600, update_time = NOW() WHERE id = 51613;

-- =====================
-- 4. Convert workspace (50010) to leaf, delete redundant child
-- =====================
UPDATE sys_menu SET menu_type = 1, component = 'views/dashboard/index.vue',
    sort = 10, update_time = NOW() WHERE id = 50010;
DELETE FROM sys_menu WHERE id = 50011;

-- =====================
-- 5. Clean duplicates
-- =====================
DELETE FROM sys_menu WHERE id IN (
    80600,  -- print template placeholder
    80125, 80126,  -- asset list/depreciation placeholders
    50107, 50108,  -- sales analysis/report (already in analytics)
    80400, 80410, 80412, 80420, 80430, 80440, 80450, 80452, 80456, 80470, 80473  -- analytics empty-path dupes
);

-- =====================
-- 6. Add missing system module items
-- =====================
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type,
    path, component, sort, visible, status, deleted, client_type, menu_level, display_mode,
    create_time, update_time)
VALUES
    (90001, 0, 61302, '错误统计', 'admin:module:error-stats', 1,
     'admin/module/error-stats', 'views/admin/module/error-stats/index.vue',
     500, 1, 1, 0, 'system-admin', 0, 0, NOW(), NOW()),
    (90002, 0, 61305, '代码生成', 'admin:dev:codegen', 1,
     'admin/dev/codegen', 'views/admin/dev/codegen/index.vue',
     500, 1, 1, 0, 'system-admin', 0, 0, NOW(), NOW()),
    (90003, 0, 61306, '数据字典', 'admin:platform:dict', 1,
     'admin/platform/dict', 'views/admin/platform/dict/index.vue',
     600, 1, 1, 0, 'system-admin', 0, 0, NOW(), NOW()) ON CONFLICT (id) DO NOTHING;

-- =====================
-- 7. Move receiving business (60403) from Warehouse to Dispatch-Receive
-- =====================
UPDATE sys_menu SET parent_id = 60004, sort = 300, update_time = NOW() WHERE id = 60403;
