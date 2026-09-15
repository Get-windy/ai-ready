-- V7.1.0: Phase 4 旧弹窗改造 + 采购换货表单页注册
-- 将 displayMode=1 的采购换货菜单 component 指向全屏表单页

-- 采购换货: component→form.vue (全屏表单)
UPDATE sys_menu SET component = 'views/erp/purchase-exchange/form.vue', update_time = CURRENT_TIMESTAMP
WHERE menu_code = 'purchase:exchange' AND deleted = 0;

-- 销售订单: 确认 form 组件路径
UPDATE sys_menu SET component = 'views/erp/sale/form.vue', update_time = CURRENT_TIMESTAMP
WHERE menu_code = 'sales:order' AND display_mode = 1 AND deleted = 0;

-- 采购订单: 确认 form 组件路径
UPDATE sys_menu SET component = 'views/erp/purchase/form.vue', update_time = CURRENT_TIMESTAMP
WHERE menu_code = 'purchase:order' AND display_mode = 1 AND deleted = 0;

-- 验证
SELECT id, menu_name, path, component, list_path, tag_label, display_mode
FROM sys_menu
WHERE menu_code IN ('purchase:exchange', 'sales:order', 'purchase:order')
  AND deleted = 0
ORDER BY id;
