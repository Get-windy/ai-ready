-- V7.2.0: Phase 5 财务/仓储/DMS 表单页注册
-- 为 displayMode=1 的菜单项设置正确的 form 组件路径

-- ============================================================
-- Phase 5A: 仓储作业单据（复用 ERP 组件或 placeholder）
-- ============================================================

-- 其他出库单 → 复用 ERP 报溢
UPDATE sys_menu SET component = 'views/wh/other-outbound/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/other-outbound/form' AND deleted = 0;

-- 其他入库单 → 复用 ERP 报溢
UPDATE sys_menu SET component = 'views/wh/other-inbound/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/other-inbound/form' AND deleted = 0;

-- 调拨单
UPDATE sys_menu SET component = 'views/wh/transfer/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/transfer/form' AND deleted = 0;

-- 报损单
UPDATE sys_menu SET component = 'views/wh/damage/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/damage/form' AND deleted = 0;

-- 报溢单
UPDATE sys_menu SET component = 'views/wh/overflow/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/overflow/form' AND deleted = 0;

-- 盘点单
UPDATE sys_menu SET component = 'views/wh/stocktake/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/stocktake/form' AND deleted = 0;

-- 成本调价单
UPDATE sys_menu SET component = 'views/wh/cost-adjust/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/cost-adjust/form' AND deleted = 0;

-- 组装单/拆卸单/借进单/借出单 → placeholder
UPDATE sys_menu SET component = 'views/wh/assemble/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/assemble/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/wh/disassemble/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/disassemble/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/wh/borrow-in/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/borrow-in/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/wh/borrow-out/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'wh/borrow-out/form' AND deleted = 0;

-- ============================================================
-- Phase 5B: 财务单据
-- ============================================================

UPDATE sys_menu SET component = 'views/finance/receipt-doc/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/receipt-doc/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/payment-doc/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/payment-doc/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/expense-doc/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/expense-doc/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/voucher/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/voucher/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/advance-receipt/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/advance-receipt/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/advance-payment/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/advance-payment/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/other-income-doc/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/other-income-doc/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/finance/ar-ap-adjust/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'finance/ar-ap-adjust/form' AND deleted = 0;

-- ============================================================
-- Phase 5C: DMS 模块
-- ============================================================

UPDATE sys_menu SET component = 'views/dms/rider/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'dms/rider/form' AND deleted = 0;

UPDATE sys_menu SET component = 'views/dms/vehicle/form.vue', update_time = CURRENT_TIMESTAMP
WHERE path = 'dms/vehicle/form' AND deleted = 0;

-- 验证
SELECT id, menu_name, path, component, display_mode
FROM sys_menu
WHERE (path LIKE 'wh/%/form' OR path LIKE 'finance/%/form' OR path LIKE 'dms/%/form')
  AND deleted = 0
ORDER BY path;
