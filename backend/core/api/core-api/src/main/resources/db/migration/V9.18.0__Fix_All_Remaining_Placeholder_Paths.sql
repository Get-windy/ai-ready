-- ============================================================
-- V9.18.0: 修复剩余所有占位符组件路径
--
-- 已创建对应页面文件，更新菜单组件路径指向实际文件
-- 至此所有 102 个占位符页面全部实现
-- ============================================================

-- 预警查询 / WH仓储单据 (11)
UPDATE sys_menu SET component = 'views/erp/alert-query/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 5017 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/borrow-in/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80010 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/borrow-out/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80011 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/receiving-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80012 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/putaway-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80013 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/picking-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80014 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/shipping-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80015 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/move-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80016 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/inventory-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80017 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/wh/borrow-query/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80018 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/sales/order-center/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80050 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/cash-transfer/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80103 AND component LIKE '%placeholder%';

-- MD / 资料管理 (7)
UPDATE sys_menu SET component = 'views/md/warehouse-plan/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80520 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/md/staff-dept/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80530 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/md/staff-role/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80531 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/md/staff-all/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80532 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/md/payment-method/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80550 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/md/payment-channel/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80551 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/md/payment-account/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80552 AND component LIKE '%placeholder%';

-- Dispatch (1)
UPDATE sys_menu SET component = 'views/dispatch/dispatch-order/form/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80760 AND component LIKE '%placeholder%';

-- ============================================================
-- 最终验证
-- ============================================================
DO $$
DECLARE
    total_leaves INTEGER;
    placeholder_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_leaves FROM sys_menu
    WHERE menu_type = 1 AND deleted = 0;

    SELECT COUNT(*) INTO placeholder_count FROM sys_menu
    WHERE menu_type = 1 AND deleted = 0
      AND component LIKE '%placeholder%';

    RAISE NOTICE '=== V9.18.0 最终修复报告 ===';
    RAISE NOTICE '本次修复: 20 页';
    RAISE NOTICE '叶子菜单总数: %', total_leaves;
    RAISE NOTICE '剩余占位符: %', placeholder_count;
    IF placeholder_count = 0 THEN
        RAISE NOTICE '✓ 所有 102 个占位符页面已全部实现！';
    END IF;
END $$;
