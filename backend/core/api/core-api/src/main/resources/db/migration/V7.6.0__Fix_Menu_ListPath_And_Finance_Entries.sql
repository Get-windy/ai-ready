-- V7.6.0: 修复菜单 list_path 及补充缺失的 Finance 菜单条目
-- 问题：V6.30.0 将 list_path 改为 xxx/form（表单页），导致"历史"标签也渲染表单而非列表
--       V7.2.0 UPDATE 语句匹配不到 finance/receipt-doc 等路径（从未 INSERT 过）
-- 修复：1) list_path 统一改为 xxx/index（列表页）
--       2) 补充 Finance 缺失的菜单条目（后发现 80xxx 已有，删除新建的重复条目）
--       3) CRM/DMS component 统一指向 form.vue

-- ============================================================
-- 1. CRM: 修复 list_path 从 xxx/form → xxx/index
-- ============================================================
UPDATE sys_menu SET list_path = 'crm/customer/index', update_time = CURRENT_TIMESTAMP WHERE id = 80200 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/lead/index', update_time = CURRENT_TIMESTAMP WHERE id = 80210 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/opportunity/index', update_time = CURRENT_TIMESTAMP WHERE id = 80220 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/contract/index', update_time = CURRENT_TIMESTAMP WHERE id = 80230 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/quotation/index', update_time = CURRENT_TIMESTAMP WHERE id = 70330 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/invoice/index', update_time = CURRENT_TIMESTAMP WHERE id = 70350 AND deleted = 0;

-- ============================================================
-- 2. DMS: 修复 list_path 并修正 component 指向 form.vue
-- ============================================================
UPDATE sys_menu SET list_path = 'dms/rider/index', component = 'views/dms/rider/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50503 AND deleted = 0;
UPDATE sys_menu SET list_path = 'dms/vehicle/index', component = 'views/dms/vehicle/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50504 AND deleted = 0;

-- ============================================================
-- 3. Finance 80xxx 系列: 修复 list_path 加 /index 后缀
--    80101=收款单, 80102=预收款单, 80111=付款单, 80112=预付款单
--    80115=费用单, 80116=其他收入, 80117=应收应付调整, 80120=会计凭证
-- ============================================================
UPDATE sys_menu SET list_path = 'finance/receipt-doc/index', update_time = CURRENT_TIMESTAMP WHERE id = 80101 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/advance-receipt/index', update_time = CURRENT_TIMESTAMP WHERE id = 80102 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/payment-doc/index', update_time = CURRENT_TIMESTAMP WHERE id = 80111 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/advance-payment/index', update_time = CURRENT_TIMESTAMP WHERE id = 80112 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/expense-doc/index', update_time = CURRENT_TIMESTAMP WHERE id = 80115 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/other-income-doc/index', update_time = CURRENT_TIMESTAMP WHERE id = 80116 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/ar-ap-adjust/index', update_time = CURRENT_TIMESTAMP WHERE id = 80117 AND deleted = 0;
UPDATE sys_menu SET list_path = 'finance/voucher/index', update_time = CURRENT_TIMESTAMP WHERE id = 80120 AND deleted = 0;

-- ============================================================
-- 4. Finance voucher: 更新 component 指向 form.vue
-- ============================================================
UPDATE sys_menu SET component = 'views/finance/voucher/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50702 AND deleted = 0;

-- ============================================================
-- 5. 清理重复条目: 50720-50722 与 80101/80111/80115 重复
--    保留 80xxx 系列（历史更完整），标记 50720-50722 为 deleted
-- ============================================================
UPDATE sys_menu SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id IN (50720, 50721, 50722);

-- ============================================================
-- 6. 验证
-- ============================================================
SELECT id, menu_name, path, component, list_path, display_mode, tag_label
FROM sys_menu
WHERE deleted = 0
  AND (id IN (80200, 80210, 80220, 80230, 70330, 70350, 50503, 50504, 50702,
              80101, 80102, 80111, 80112, 80115, 80116, 80117, 80120))
ORDER BY id;
