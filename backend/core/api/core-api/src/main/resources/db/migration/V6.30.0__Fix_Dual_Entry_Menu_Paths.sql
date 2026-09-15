-- V6.30.0: 修复所有 displayMode=1 双标签按钮的路径和组件规范
-- 历史/列表标签约定: path(主按钮)=xxx/form(表单页), listPath(标签按钮)=xxx(列表页)
-- 添加标签约定: path(主按钮)=xxx(列表页), listPath(标签按钮)=xxx/form(表单页)

-- ============================================================
-- A. path 加 /form 后缀 + component 从 index.vue 改为 form.vue
--    (历史/列表标签，path 缺少 /form 的条目)
-- ============================================================

-- 发货管理
UPDATE sys_menu SET path = 'erp/shipment/form', component = 'views/erp/shipment/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50105 AND deleted = 0;
-- 退货管理
UPDATE sys_menu SET path = 'erp/return/form', component = 'views/erp/return/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50106 AND deleted = 0;
-- WMS 收货
UPDATE sys_menu SET path = 'wms/receipt/form', component = 'views/wms/receipt/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50403 AND deleted = 0;
-- WMS 上架
UPDATE sys_menu SET path = 'wms/putaway/form', component = 'views/wms/putaway/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50404 AND deleted = 0;
-- WMS 拣货
UPDATE sys_menu SET path = 'wms/pick/form', component = 'views/wms/pick/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50405 AND deleted = 0;
-- WMS 波次
UPDATE sys_menu SET path = 'wms/wave/form', component = 'views/wms/wave/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50406 AND deleted = 0;
-- WMS 发货
UPDATE sys_menu SET path = 'wms/ship/form', component = 'views/wms/ship/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50407 AND deleted = 0;
-- WMS 移库
UPDATE sys_menu SET path = 'wms/move/form', component = 'views/wms/move/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50409 AND deleted = 0;
-- WMS 盘点
UPDATE sys_menu SET path = 'wms/check/form', component = 'views/wms/check/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 50410 AND deleted = 0;
-- 商城用户审核
UPDATE sys_menu SET path = 'mall/user-audit/form', component = 'views/erp/mall/user-audit/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 51104 AND deleted = 0;
-- CRM 报价单
UPDATE sys_menu SET path = 'crm/quotation/form', component = 'views/common/placeholder/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 70330 AND deleted = 0;
-- CRM 发票
UPDATE sys_menu SET path = 'crm/invoice/form', component = 'views/common/placeholder/index.vue', update_time = CURRENT_TIMESTAMP WHERE id = 70350 AND deleted = 0;

-- ============================================================
-- B. 采购订单: path 已正确(purchase/order/form), 只需修 component
-- ============================================================
UPDATE sys_menu SET component = 'views/erp/purchase/form.vue', update_time = CURRENT_TIMESTAMP WHERE id = 70060 AND deleted = 0;

-- ============================================================
-- C. listPath 修正: /add -> /form (添加标签约定)
-- ============================================================
UPDATE sys_menu SET list_path = 'dms/rider/form', update_time = CURRENT_TIMESTAMP WHERE id = 50503 AND deleted = 0;
UPDATE sys_menu SET list_path = 'dms/vehicle/form', update_time = CURRENT_TIMESTAMP WHERE id = 50504 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/customer/form', update_time = CURRENT_TIMESTAMP WHERE id = 80200 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/lead/form', update_time = CURRENT_TIMESTAMP WHERE id = 80210 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/opportunity/form', update_time = CURRENT_TIMESTAMP WHERE id = 80220 AND deleted = 0;
UPDATE sys_menu SET list_path = 'crm/contract/form', update_time = CURRENT_TIMESTAMP WHERE id = 80230 AND deleted = 0;
UPDATE sys_menu SET list_path = 'md/product/form', update_time = CURRENT_TIMESTAMP WHERE id = 80500 AND deleted = 0;
UPDATE sys_menu SET list_path = 'md/customer/form', update_time = CURRENT_TIMESTAMP WHERE id = 80510 AND deleted = 0;
UPDATE sys_menu SET list_path = 'md/supplier/form', update_time = CURRENT_TIMESTAMP WHERE id = 80511 AND deleted = 0;
UPDATE sys_menu SET list_path = 'md/logistics/form', update_time = CURRENT_TIMESTAMP WHERE id = 80512 AND deleted = 0;
