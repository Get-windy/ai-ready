-- V7.0.0: Phase 1-3 菜单路径和组件修复
-- 确保 displayMode=1 的菜单项 path→form.vue, listPath→列表页

-- ============================================================
-- Phase 1: ERP 核心单据 form.vue 已实现
-- ============================================================

-- 发货管理: path→form.vue, component→form.vue
UPDATE sys_menu SET component = 'views/erp/shipment/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50105 AND deleted = 0;
-- 退货管理
UPDATE sys_menu SET component = 'views/erp/return/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50106 AND deleted = 0;
-- 入库管理 (stock-in)
UPDATE sys_menu SET component = 'views/erp/stock-in/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50203 AND deleted = 0;
-- 库存盘点
UPDATE sys_menu SET component = 'views/erp/stocktake/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50302 AND deleted = 0;
-- 采购订单
UPDATE sys_menu SET component = 'views/erp/purchase/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 70060 AND deleted = 0;

-- ============================================================
-- Phase 2: WMS 仓储执行 form.vue 已实现
-- ============================================================

-- WMS 收货
UPDATE sys_menu SET component = 'views/wms/receipt/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50403 AND deleted = 0;
-- WMS 上架
UPDATE sys_menu SET component = 'views/wms/putaway/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50404 AND deleted = 0;
-- WMS 拣货
UPDATE sys_menu SET component = 'views/wms/pick/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50405 AND deleted = 0;
-- WMS 波次
UPDATE sys_menu SET component = 'views/wms/wave/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50406 AND deleted = 0;
-- WMS 发货
UPDATE sys_menu SET component = 'views/wms/ship/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50407 AND deleted = 0;
-- WMS 移库
UPDATE sys_menu SET component = 'views/wms/move/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50409 AND deleted = 0;
-- WMS 盘点
UPDATE sys_menu SET component = 'views/wms/check/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50410 AND deleted = 0;

-- ============================================================
-- Phase 3: CRM 客户关系 form.vue 已实现
-- ============================================================

-- CRM 客户 form页
UPDATE sys_menu SET component = 'views/crm/customer/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80200 AND deleted = 0;
-- CRM 线索 form页
UPDATE sys_menu SET component = 'views/crm/lead/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80210 AND deleted = 0;
-- CRM 商机 form页
UPDATE sys_menu SET component = 'views/crm/opportunity/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80220 AND deleted = 0;
-- CRM 合同 form页
UPDATE sys_menu SET component = 'views/crm/contract/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 80230 AND deleted = 0;
-- CRM 报价单 form页
UPDATE sys_menu SET component = 'views/crm/quotation/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 70330 AND deleted = 0;
-- CRM 发票 form页
UPDATE sys_menu SET component = 'views/crm/invoice/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 70350 AND deleted = 0;

-- ============================================================
-- 验证: 检查所有 displayMode=1 菜单的组件是否已更新
-- ============================================================
SELECT id, menu_name, path, component, list_path, tag_label, display_mode
FROM sys_menu
WHERE display_mode = 1 AND deleted = 0
  AND component NOT LIKE '%placeholder%'
ORDER BY id;
