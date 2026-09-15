-- 修复销售订单表单路径，使用独立全屏编辑页面而非弹窗
-- form 路径现在加载 views/erp/sale/form.vue（全屏编辑页面）
-- list 路径保持 views/erp/sale/index.vue（列表页面）
UPDATE sys_menu SET
  component = 'views/erp/sale/form.vue',
  update_time = CURRENT_TIMESTAMP
WHERE id = 70010 AND deleted = 0;
