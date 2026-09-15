-- 修正采购退货单菜单组件路径指向实际组件
-- 采购退货单（70062）此前误指向废弃的 views/erp/purchase-return/form.vue
UPDATE sys_menu
SET component = 'views/purchase/return/form.vue',
    update_time = CURRENT_TIMESTAMP
WHERE id = 70062 AND deleted = 0;
