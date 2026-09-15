-- ============================================================
-- V9.16.0: 修复遗留占位符组件路径（已有实现文件）
--
-- 审计发现以下菜单的组件路径仍指向 placeholder，但其对应页面
-- 文件已存在于代码仓库中：
--   1. 70330 报价单 → 已有 crm/quotation/form.vue
--   2. 70350 发票 → 已有 crm/invoice/form.vue
--   3. 80201 客户跟进 → 已有 crm/customer-follow/index.vue
-- ============================================================

-- 1. 70330 报价单 → crm/quotation/form.vue
UPDATE sys_menu
SET component = 'views/crm/quotation/form.vue',
    update_time = CURRENT_TIMESTAMP
WHERE id = 70330 AND deleted = 0
  AND component LIKE '%placeholder%';

-- 2. 70350 发票 → crm/invoice/form.vue
UPDATE sys_menu
SET component = 'views/crm/invoice/form.vue',
    update_time = CURRENT_TIMESTAMP
WHERE id = 70350 AND deleted = 0
  AND component LIKE '%placeholder%';

-- 3. 80201 客户跟进 → crm/customer-follow/index.vue
UPDATE sys_menu
SET component = 'views/crm/customer-follow/index.vue',
    update_time = CURRENT_TIMESTAMP
WHERE id = 80201 AND deleted = 0
  AND component LIKE '%placeholder%';

-- ============================================================
-- 验证
-- ============================================================
DO $$
DECLARE
    updated_70330 TEXT;
    updated_70350 TEXT;
    updated_80201 TEXT;
    total_placeholders INTEGER;
BEGIN
    SELECT component INTO updated_70330 FROM sys_menu WHERE id = 70330 AND deleted = 0;
    SELECT component INTO updated_70350 FROM sys_menu WHERE id = 70350 AND deleted = 0;
    SELECT component INTO updated_80201 FROM sys_menu WHERE id = 80201 AND deleted = 0;

    SELECT COUNT(*) INTO total_placeholders FROM sys_menu
    WHERE menu_type = 1 AND deleted = 0
      AND component LIKE '%placeholder%';

    RAISE NOTICE '=== V9.16.0 CRM组件路径修复报告 ===';
    RAISE NOTICE '70330 报价单: %', COALESCE(updated_70330, '未找到');
    RAISE NOTICE '70350 发票: %', COALESCE(updated_70350, '未找到');
    RAISE NOTICE '80201 客户跟进: %', COALESCE(updated_80201, '未找到');
    RAISE NOTICE '剩余占位符: %', total_placeholders;
END $$;
