-- 修复销售订单菜单组件路径，指向实际存在的列表页
-- 同时保留 display_mode=1 的双入口模式（form路径打开新增弹窗，list路径打开列表）
UPDATE sys_menu SET
  component = 'views/erp/sale/index.vue',
  update_time = CURRENT_TIMESTAMP
WHERE id = 70010 AND deleted = 0;
