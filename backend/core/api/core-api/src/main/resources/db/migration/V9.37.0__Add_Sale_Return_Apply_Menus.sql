-- ============================================================
-- V9.37.0: 添加销售退货申请菜单
--
-- 新增：
--   - 销售退货申请（列表页，双入口模式 displayMode=1）
--   - 销售退货申请表单页（隐藏路由）
--
-- 菜单位置：销售(60001) > 销售业务(60103)
-- 同时补齐 sys_role_menu / sys_tenant_menu 缺失的 deleted 字段
-- ============================================================

-- ═══ 补齐关联表软删除字段（与系统其他实体保持一致） ═══
ALTER TABLE sys_role_menu ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0;
ALTER TABLE sys_tenant_menu ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0;

-- ═══ 销售退货申请 - 列表页（双入口模式） ═══
-- displayMode=1: 双入口模式，点击进表单 + [历史]进列表
-- listPath: 列表页路由路径
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, list_path, display_group, menu_level)
VALUES
  (80091, 0, 60103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '销售退货申请', 'sales:return-apply', 1,
   'sales/return-apply', 'views/sales/return-apply/index.vue',
   'RollbackOutlined', 600, 1, 1, 'pc-admin',
   1, 'sales/return-apply', 0, 0);

-- ══ 销售退货申请 - 表单页（隐藏路由，双入口自动跳转） ═══
INSERT INTO sys_menu (id, tenant_id, parent_id, deleted, create_time, update_time,
    menu_name, menu_code, menu_type, path, component, icon, sort,
    visible, status, client_type, display_mode, display_group, menu_level)
VALUES
  (80092, 0, 60103, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
   '销售退货申请表单', 'sales:return-apply:form', 1,
   'sales/return-apply/form/:id', 'views/sales/return-apply/form.vue',
   NULL, 601, 0, 1, 'pc-admin',
   0, 0, 0);

-- ═══ 角色菜单授权（默认授权给所有角色） ═══
INSERT INTO sys_role_menu (id, role_id, menu_id, tenant_id, create_time)
SELECT
    900000000 + ROW_NUMBER() OVER () AS id,
    rm.role_id, 80091, 0, CURRENT_TIMESTAMP
FROM sys_role_menu rm
WHERE rm.menu_id = 60103 AND rm.deleted = 0;

INSERT INTO sys_role_menu (id, role_id, menu_id, tenant_id, create_time)
SELECT
    900000100 + ROW_NUMBER() OVER () AS id,
    rm.role_id, 80092, 0, CURRENT_TIMESTAMP
FROM sys_role_menu rm
WHERE rm.menu_id = 60103 AND rm.deleted = 0;

-- ═══ 租户菜单授权 ═══
INSERT INTO sys_tenant_menu (id, tenant_id, menu_id, create_time)
SELECT
    900000200 + ROW_NUMBER() OVER () AS id,
    tm.tenant_id, 80091, CURRENT_TIMESTAMP
FROM sys_tenant_menu tm
WHERE tm.menu_id = 60103 AND tm.deleted = 0;

INSERT INTO sys_tenant_menu (id, tenant_id, menu_id, create_time)
SELECT
    900000300 + ROW_NUMBER() OVER () AS id,
    tm.tenant_id, 80092, CURRENT_TIMESTAMP
FROM sys_tenant_menu tm
WHERE tm.menu_id = 60103 AND tm.deleted = 0;

-- ═══ 验证 ═══
DO $$
DECLARE
    list_count INTEGER;
    form_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO list_count FROM sys_menu WHERE id = 80091 AND deleted = 0;
    SELECT COUNT(*) INTO form_count FROM sys_menu WHERE id = 80092 AND deleted = 0;

    RAISE NOTICE '=== V9.37.0 销售退货申请菜单 ===';
    RAISE NOTICE '列表页菜单(80091): %', CASE WHEN list_count > 0 THEN 'OK' ELSE 'MISSING' END;
    RAISE NOTICE '表单页菜单(80092): %', CASE WHEN form_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
