-- V7.4.0: 往来单位管理模块 - 客户/供应商/物流公司/其他往来单位 表单页实现
-- 1. 更新现有菜单的组件指向新的表单页面
-- 2. 更新 listPath 指向列表页面
-- 3. 新增"其他往来单位"菜单

-- ============================================================
-- A. 更新现有往来单位子菜单的组件路径和列表路径
-- ============================================================

-- 客户 (80510): 表单页 + 列表页
UPDATE sys_menu SET
  component = 'views/md/customer/form.vue',
  list_path = 'md/customer/index',
  update_time = CURRENT_TIMESTAMP
WHERE id = 80510 AND deleted = 0;

-- 供应商 (80511): 表单页 + 列表页
UPDATE sys_menu SET
  component = 'views/md/supplier/form.vue',
  list_path = 'md/supplier/index',
  update_time = CURRENT_TIMESTAMP
WHERE id = 80511 AND deleted = 0;

-- 物流公司 (80512): 表单页 + 列表页
UPDATE sys_menu SET
  component = 'views/md/logistics/form.vue',
  list_path = 'md/logistics/index',
  update_time = CURRENT_TIMESTAMP
WHERE id = 80512 AND deleted = 0;

-- ============================================================
-- B. 新增"其他往来单位"菜单（在物流公司下方）
-- ============================================================
INSERT INTO sys_menu (id, tenant_id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, display_mode, list_path, tag_label, client_type, display_group, biz_flow_tag, link_icon, menu_level, visible, status)
VALUES
(80513, 0, 61102, '其他往来单位', 'md:partner', 1, 'md/partner', 'views/md/partner/form.vue', 'AppstoreOutlined', 4, 1, 'md/partner/index', '添加', 'tenant-admin', 0, NULL, NULL, 3, 1, 1) ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- C. 同步更新 V6.21.0 中可能存在的重复记录
-- ============================================================
UPDATE sys_menu SET
  component = 'views/md/customer/form.vue',
  list_path = 'md/customer/index',
  update_time = CURRENT_TIMESTAMP
WHERE id = 70510 AND deleted = 0;

UPDATE sys_menu SET
  component = 'views/md/supplier/form.vue',
  list_path = 'md/supplier/index',
  update_time = CURRENT_TIMESTAMP
WHERE id = 70511 AND deleted = 0;

UPDATE sys_menu SET
  component = 'views/md/logistics/form.vue',
  list_path = 'md/logistics/index',
  update_time = CURRENT_TIMESTAMP
WHERE id = 70512 AND deleted = 0;
