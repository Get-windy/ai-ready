-- ============================================================
-- V9.17.0: 修复营销/商城/财务页面的占位符组件路径
--
-- 已创建对应页面文件，更新菜单组件路径指向实际文件
-- ============================================================

-- Marketing 营销模块 (17 pages)
UPDATE sys_menu SET component = 'views/marketing/member-manage/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80300 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/points-exchange/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80301 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/member-config/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80302 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/sms-send/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80310 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/coupon/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80311 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/product-promo/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80312 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/order-promo/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80313 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/special-price/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80314 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/package-deal/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80315 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/mall-group/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80320 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/mall-flash/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80321 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/mall-presale/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80322 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/mall-popup/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80323 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/add-on-deal/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80324 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/hot-keywords/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80325 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/promote-create/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80330 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/marketing/promote-history/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80331 AND component LIKE '%placeholder%';

-- Mall 商城模块 (11 pages)
UPDATE sys_menu SET component = 'views/mall/order-process/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80350 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/return-process/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80351 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/product-shelf/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80360 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/unit-display/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80361 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/buyer-apply/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80362 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/buyer-account/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80363 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/product-combo/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80364 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/basic-config/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80370 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/shop-config/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80371 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/freight-config/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80372 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/shop-decoration/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80373 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/notice-config/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80374 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/mall/keyword-bank/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80375 AND component LIKE '%placeholder%';

-- Finance 财务模块补充 (7 pages)
UPDATE sys_menu SET component = 'views/finance/receipt-by-doc/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80100 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/pending-confirm/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80104 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/online-payment-reconcile/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80105 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/payment-by-doc/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80110 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/month-closing/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80121 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/budget-plan/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80130 AND component LIKE '%placeholder%';
UPDATE sys_menu SET component = 'views/finance/budget-exec/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80131 AND component LIKE '%placeholder%';

-- ============================================================
-- 验证
-- ============================================================
DO $$
DECLARE
    total_placeholders INTEGER;
    fixed_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_placeholders FROM sys_menu
    WHERE menu_type = 1 AND deleted = 0
      AND component LIKE '%placeholder%';

    RAISE NOTICE '=== V9.17.0 修复报告 ===';
    RAISE NOTICE '修复页数: 17 营销 + 13 商城 + 7 财务 = 37';
    RAISE NOTICE '剩余占位符: %', total_placeholders;
END $$;
