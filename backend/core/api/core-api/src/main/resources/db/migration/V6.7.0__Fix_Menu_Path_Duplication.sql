-- ============================================================
-- V6.7.0: 修复菜单路径无限叠加问题
--
-- 问题现象：点击菜单销售订单，URL变成
-- /dashboard/crm/crm/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/erp/supplier/crm/sale
--
-- 根本原因：菜单数据中path字段配置错误，包含重复路径段或循环引用
-- ============================================================

-- 1. 首先检查并修复菜单的循环引用问题
-- 删除parent_id指向不存在或已删除的菜单（形成孤立节点）
UPDATE sys_menu SET parent_id = 0
WHERE deleted = 0
  AND parent_id > 0
  AND parent_id NOT IN (SELECT id FROM sys_menu WHERE deleted = 0);

-- 2. 清理包含重复路径段的菜单记录
-- 查找path中包含重复路径段的菜单（如 'erp/erp', 'crm/crm'）
UPDATE sys_menu
SET path = REPLACE(
  REPLACE(
    REPLACE(
      REPLACE(path, 'erp/erp/', 'erp/'),
      'crm/crm/', 'crm/'
    ),
    'wms/wms/', 'wms/'
  ),
  'dms/dms/', 'dms/'
)
WHERE deleted = 0
  AND (path LIKE '%erp/erp%' OR path LIKE '%crm/crm%' OR path LIKE '%wms/wms%' OR path LIKE '%dms/dms%');

-- 3. 修复销售作业下的菜单路径
-- 根据 V6.3.0 配置，子菜单path应该是相对路径或完整路径，不应重复

-- 销售作业(50100)下的子菜单
UPDATE sys_menu SET path = 'crm/quotation' WHERE id = 50101 AND deleted = 0;
UPDATE sys_menu SET path = 'sale' WHERE id = 50102 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/sale' WHERE id = 50103 AND deleted = 0;
UPDATE sys_menu SET path = 'stock' WHERE id = 50104 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/shipment' WHERE id = 50105 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/return' WHERE id = 50106 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/sales-analysis' WHERE id = 50107 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/sales-report' WHERE id = 50108 AND deleted = 0;

-- 采购作业(50200)下的子菜单
UPDATE sys_menu SET path = 'purchase' WHERE id = 50201 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/purchase' WHERE id = 50202 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-in' WHERE id = 50203 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/purchase-return' WHERE id = 50204 AND deleted = 0;  -- 采购退货独立路径
UPDATE sys_menu SET path = 'erp/purchase-exchange' WHERE id = 50205 AND deleted = 0;

-- 仓储作业(50300)下的子菜单
UPDATE sys_menu SET path = 'erp/stock' WHERE id = 50301 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stocktake' WHERE id = 50302 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-transfer' WHERE id = 50303 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-cost-adjust' WHERE id = 50304 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-overflow' WHERE id = 50305 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-damage' WHERE id = 50306 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/batch' WHERE id = 50307 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/serial' WHERE id = 50308 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-bom' WHERE id = 50309 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-assemble' WHERE id = 50310 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-split' WHERE id = 50311 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock-alert-config' WHERE id = 50312 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/stock/replenishment' WHERE id = 50313 AND deleted = 0;

-- WMS仓储执行(50400)下的子菜单
UPDATE sys_menu SET path = 'wms/warehouse' WHERE id = 50401 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/location' WHERE id = 50402 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/receipt' WHERE id = 50403 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/putaway' WHERE id = 50404 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/pick' WHERE id = 50405 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/wave' WHERE id = 50406 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/ship' WHERE id = 50407 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/inventory' WHERE id = 50408 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/move' WHERE id = 50409 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/check' WHERE id = 50410 AND deleted = 0;
UPDATE sys_menu SET path = 'wms/event' WHERE id = 50411 AND deleted = 0;

-- 配送管理(50500)下的子菜单
UPDATE sys_menu SET path = 'dms/dashboard' WHERE id = 50501 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/channel' WHERE id = 50502 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/rider' WHERE id = 50503 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/vehicle' WHERE id = 50504 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/route' WHERE id = 50505 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/dispatch' WHERE id = 50506 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/order-pool' WHERE id = 50507 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/tracking' WHERE id = 50508 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/verification' WHERE id = 50509 AND deleted = 0;
UPDATE sys_menu SET path = 'dms/config' WHERE id = 50510 AND deleted = 0;

-- 客户关系(50600)下的子菜单
UPDATE sys_menu SET path = 'crm/lead' WHERE id = 50601 AND deleted = 0;
UPDATE sys_menu SET path = 'crm/opportunity' WHERE id = 50602 AND deleted = 0;
UPDATE sys_menu SET path = 'crm/customer' WHERE id = 50603 AND deleted = 0;
UPDATE sys_menu SET path = 'crm/contract' WHERE id = 50604 AND deleted = 0;
UPDATE sys_menu SET path = 'crm/invoice' WHERE id = 50605 AND deleted = 0;
UPDATE sys_menu SET path = 'supplier' WHERE id = 50606 AND deleted = 0;
UPDATE sys_menu SET path = 'supplier/inquiry' WHERE id = 50607 AND deleted = 0;
UPDATE sys_menu SET path = 'supplier/performance' WHERE id = 50608 AND deleted = 0;

-- 财务管理(50700)下的子菜单
UPDATE sys_menu SET path = 'finance/subject' WHERE id = 50701 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/voucher' WHERE id = 50702 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/receivable' WHERE id = 50703 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/payable' WHERE id = 50704 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/accounts-receivable' WHERE id = 50705 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/accounts-receivable/aging-analysis' WHERE id = 50706 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/accounts-payable' WHERE id = 50707 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/receipt' WHERE id = 50708 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/payment' WHERE id = 50709 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/pre-receipt' WHERE id = 50710 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/pre-payment' WHERE id = 50711 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/deposit' WHERE id = 50712 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/write-off' WHERE id = 50713 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/offset' WHERE id = 50714 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/capital-flow' WHERE id = 50715 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/report' WHERE id = 50716 AND deleted = 0;
UPDATE sys_menu SET path = 'finance/reconciliation' WHERE id = 50717 AND deleted = 0;

-- 费用管理(50800)下的子菜单
UPDATE sys_menu SET path = 'erp/expense/application' WHERE id = 50801 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/expense/reimbursement' WHERE id = 50802 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/expense/approval' WHERE id = 50803 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/expense/payment' WHERE id = 50804 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/expense/statistics' WHERE id = 50805 AND deleted = 0;

-- 资产管理(50900)下的子菜单
UPDATE sys_menu SET path = 'fixed-asset/asset' WHERE id = 50901 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/category' WHERE id = 50902 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/purchase' WHERE id = 50903 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/depreciation' WHERE id = 50904 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/transfer' WHERE id = 50905 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/disposal' WHERE id = 50906 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/inventory' WHERE id = 50907 AND deleted = 0;
UPDATE sys_menu SET path = 'fixed-asset/report' WHERE id = 50908 AND deleted = 0;

-- 预算管理(51000)下的子菜单
UPDATE sys_menu SET path = 'budget/index' WHERE id = 51001 AND deleted = 0;
UPDATE sys_menu SET path = 'budget/template' WHERE id = 51002 AND deleted = 0;
UPDATE sys_menu SET path = 'budget/annual' WHERE id = 51003 AND deleted = 0;
UPDATE sys_menu SET path = 'budget/adjustment' WHERE id = 51004 AND deleted = 0;
UPDATE sys_menu SET path = 'budget/report' WHERE id = 51005 AND deleted = 0;

-- 商城管理(51100)下的子菜单
UPDATE sys_menu SET path = 'mall/config' WHERE id = 51101 AND deleted = 0;
UPDATE sys_menu SET path = 'mall/product' WHERE id = 51102 AND deleted = 0;
UPDATE sys_menu SET path = 'mall/order' WHERE id = 51103 AND deleted = 0;
UPDATE sys_menu SET path = 'mall/user-audit' WHERE id = 51104 AND deleted = 0;
UPDATE sys_menu SET path = 'mall/banner' WHERE id = 51105 AND deleted = 0;

-- 订单中心(51200)下的子菜单
UPDATE sys_menu SET path = 'order-center' WHERE id = 51201 AND deleted = 0;

-- 工作流(51300)下的子菜单
UPDATE sys_menu SET path = 'workflow/instance-monitor' WHERE id = 51301 AND deleted = 0;
UPDATE sys_menu SET path = 'workflow/task-management' WHERE id = 51302 AND deleted = 0;
UPDATE sys_menu SET path = 'workflow/process-analysis' WHERE id = 51303 AND deleted = 0;

-- 产品数据(51400)下的子菜单
UPDATE sys_menu SET path = 'erp/product' WHERE id = 51401 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/partner' WHERE id = 51402 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/pricing' WHERE id = 51403 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/pricing/approval' WHERE id = 51404 AND deleted = 0;
UPDATE sys_menu SET path = 'erp/pricing/tiers' WHERE id = 51405 AND deleted = 0;

-- 打印管理(51500)下的子菜单
UPDATE sys_menu SET path = 'printing/template' WHERE id = 51501 AND deleted = 0;
UPDATE sys_menu SET path = 'printing/chain' WHERE id = 51502 AND deleted = 0;
UPDATE sys_menu SET path = 'printing/client' WHERE id = 51503 AND deleted = 0;
UPDATE sys_menu SET path = 'printing/task' WHERE id = 51504 AND deleted = 0;

-- 系统管理(51600)下的子菜单
UPDATE sys_menu SET path = 'system/department' WHERE id = 51601 AND deleted = 0;
UPDATE sys_menu SET path = 'system/position' WHERE id = 51602 AND deleted = 0;
UPDATE sys_menu SET path = 'system/user' WHERE id = 51603 AND deleted = 0;
UPDATE sys_menu SET path = 'system/role' WHERE id = 51604 AND deleted = 0;
UPDATE sys_menu SET path = 'system/permission' WHERE id = 51605 AND deleted = 0;
UPDATE sys_menu SET path = 'system/menu' WHERE id = 51606 AND deleted = 0;
UPDATE sys_menu SET path = 'system/tenant' WHERE id = 51607 AND deleted = 0;
UPDATE sys_menu SET path = 'system/tenant-approval' WHERE id = 51608 AND deleted = 0;
UPDATE sys_menu SET path = 'system/dict' WHERE id = 51609 AND deleted = 0;
UPDATE sys_menu SET path = 'system/config' WHERE id = 51610 AND deleted = 0;
UPDATE sys_menu SET path = 'system/log' WHERE id = 51611 AND deleted = 0;
UPDATE sys_menu SET path = 'system/data-import' WHERE id = 51612 AND deleted = 0;
UPDATE sys_menu SET path = 'notification' WHERE id = 51613 AND deleted = 0;

-- 图表(51700)下的子菜单
UPDATE sys_menu SET path = 'charts' WHERE id = 51701 AND deleted = 0;

-- 工作台(50010)下的子菜单
UPDATE sys_menu SET path = 'dashboard' WHERE id = 50011 AND deleted = 0;

-- ============================================================
-- 验证修复结果
-- ============================================================
-- 查询所有菜单的path，确认没有重复路径段
SELECT id, parent_id, menu_name, path FROM sys_menu
WHERE deleted = 0 AND path IS NOT NULL
  AND (path LIKE '%erp/erp%' OR path LIKE '%crm/crm%' OR path LIKE '%wms/wms%' OR path LIKE '%dms/dms%');

-- 查询所有一级菜单
SELECT id, parent_id, menu_name, path FROM sys_menu WHERE deleted = 0 AND parent_id = 0 ORDER BY sort;