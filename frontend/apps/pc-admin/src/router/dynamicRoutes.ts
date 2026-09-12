import type { RouteRecordRaw, Router } from 'vue-router'
import { h, defineComponent } from 'vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const CLIENT_TYPE = 'tenant-admin'

/**
 * 递归剥离路由树中所有层级的 redirect 属性
 * vue-router v4 将 redirect: '' 视为 !== undefined → 执行重定向 → pushWithRedirect 无限递归
 */
function stripAllRedirects(route: any): any {
  const { redirect, ...rest } = route
  if (rest.children) {
    rest.children = rest.children.map((child: any) => stripAllRedirects(child))
  }
  return rest
}

interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuCode: string
  menuType: number
  path?: string
  component?: string
  routeName?: string
  redirect?: string
  icon?: string
  sortOrder: number
  isExternal?: number
  isCache?: number
  visible: number
  status: number
  clientType?: string
  billType?: string
  bizFlowTag?: string
  displayGroup?: number
  linkIcon?: string
  displayMode?: number
  listPath?: string
  tagLabel?: string
  menuLevel?: number
  children?: MenuItem[]
}

const componentMap: Record<string, () => Promise<any>> = {
  'erp/product/index': () => import('@/views/erp/product/index.vue'),
  'erp/product/create': () => import('@/views/erp/product/form.vue'),
  'erp/product/form': () => import('@/views/erp/product/form.vue'),
  'erp/product/price-batch': () => import('@/views/erp/product/price-batch.vue'),
  'erp/product/inventory-mode': () => import('@/views/erp/product/inventory-mode.vue'),
  'erp/product/grade': () => import('@/views/erp/product/grade.vue'),
  'erp/column-config/sale-order-item': () => import('@/views/erp/column-config/SaleOrderItemColumnConfig.vue'),
  'partner/index': () => import('@/views/erp/partner/index.vue'),
  'partner/detail': () => import('@/views/erp/partner/detail.vue'),
  'erp/partner/index': () => import('@/views/erp/partner/index.vue'),
  'erp/partner/detail': () => import('@/views/erp/partner/detail.vue'),
  'pricing/index': () => import('@/views/erp/pricing/index.vue'),
  'charts/index': () => import('@/views/charts/index.vue'),
  'crm/contract/index': () => import('@/views/crm/contract/index.vue'),
  'crm/customer/index': () => import('@/views/crm/customer/index.vue'),
  'crm/invoice/index': () => import('@/views/crm/invoice/index.vue'),
  'crm/lead/index': () => import('@/views/crm/lead/index.vue'),
  'crm/opportunity/index': () => import('@/views/crm/opportunity/index.vue'),
  'crm/quotation/index': () => import('@/views/crm/quotation/index.vue'),
  'crm/supplier/index': () => import('@/views/crm/supplier/index.vue'),
  'dashboard/index': () => import('@/views/dashboard/index.vue'),
  'finance/index': () => import('@/views/finance/index.vue'),
  'finance/reconciliation/index': () => import('@/views/finance/reconciliation/index.vue'),
  'finance/reports/index': () => import('@/views/finance/reports/index.vue'),
  'finance/voucher/index': () => import('@/views/finance/voucher/index.vue'),
  'finance/voucher/VoucherDetail': () => import('@/views/finance/voucher/VoucherDetail.vue'),
  'finance/subject/index': () => import('@/views/finance/subject/index.vue'),
  'finance/receivable/index': () => import('@/views/finance/receivable/index.vue'),
  'finance/payable/index': () => import('@/views/finance/payable/index.vue'),
  'finance/pre-receipt/index': () => import('@/views/finance/pre-receipt/index.vue'),
  'finance/pre-payment/index': () => import('@/views/finance/pre-payment/index.vue'),
  'finance/deposit/index': () => import('@/views/finance/deposit/index.vue'),
  'finance/write-off/index': () => import('@/views/finance/write-off/index.vue'),
  'finance/offset/index': () => import('@/views/finance/offset/index.vue'),
  'finance/capital-flow/index': () => import('@/views/finance/capital-flow/index.vue'),
  'finance/receipt/index': () => import('@/views/finance/receipt/index.vue'),
  'finance/payment/index': () => import('@/views/finance/payment/index.vue'),
  'finance/report/index': () => import('@/views/finance/report/index.vue'),
  'notification/index': () => import('@/views/notification/index.vue'),
  'order-center/index': () => import('@/views/order-center/index.vue'),
  'profile/index': () => import('@/views/profile/index.vue'),
  'purchase/detail/inbound/InboundDetail': () => import('@/views/purchase/detail/inbound/InboundDetail.vue'),
  'purchase/detail/inquiry/InquiryDetail': () => import('@/views/purchase/detail/inquiry/InquiryDetail.vue'),
  'purchase/index': () => import('@/views/erp/purchase/index.vue'),
  'sale/index': () => import('@/views/erp/sale/index.vue'),
  'stock/detail/StockDetail': () => import('@/views/erp/stock/index.vue'),
  'stock/index': () => import('@/views/erp/stock/index.vue'),
  'supplier/create': () => import('@/views/supplier/create.vue'),
  'supplier/detail': () => import('@/views/supplier/detail.vue'),
  'supplier/edit': () => import('@/views/supplier/edit.vue'),
  'supplier/index': () => import('@/views/supplier/index.vue'),
  'supplier/inquiry/index': () => import('@/views/supplier/inquiry/index.vue'),
  'supplier/performance/index': () => import('@/views/supplier/performance/index.vue'),
  'system/config/index': () => import('@/views/system/config/index.vue'),
  'system/department/index': () => import('@/views/system/department/index.vue'),
  'system/dict/index': () => import('@/views/system/dict/index.vue'),
  'system/log/index': () => import('@/views/system/log/index.vue'),
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
  'system/permission/index': () => import('@/views/system/permission/index.vue'),
  'admin/sys/permissions/index': () => import('@/views/admin/sys/permissions/index.vue'),
  'admin/tenant/permissions/index': () => import('@/views/admin/tenant/permissions/index.vue'),
  'system/position/index': () => import('@/views/system/position/index.vue'),
  'system/role/index': () => import('@/views/system/role/index.vue'),
  'system/user/index': () => import('@/views/system/user/index.vue'),
  'system/tenant/index': () => import('@/views/system/tenant/index.vue'),
  'system/tenant-approval/index': () => import('@/views/system/tenant-approval/index.vue'),
  'system/data-import/index': () => import('@/views/system/data-import/index.vue'),
  'workflow/instance-monitor': () => import('@/views/workflow/instance-monitor.vue'),
  // 流程定义（菜单801 /workflow/definition）与流程设计（菜单80610 set/workflow-designer）共用流程设计器
  'workflow/definition': () => import('@/views/workflow/designer/index.vue'),
  'set/workflow-designer': () => import('@/views/workflow/designer/index.vue'),
  'workflow/process-analysis': () => import('@/views/workflow/process-analysis.vue'),
  'workflow/task-management': () => import('@/views/workflow/task-management.vue'),

  // 打印模块
  'printing/template/index': () => import('@/views/printing/template/index.vue'),
  'printing/chain/index': () => import('@/views/printing/chain/index.vue'),
  'printing/client/index': () => import('@/views/printing/client/index.vue'),
  'printing/task/index': () => import('@/views/printing/task/index.vue'),
  'printing/designer/index': () => import('@/views/printing/designer/index.vue'),

  // ERP 模块
  'erp/sale/index': () => import('@/views/erp/sale/index.vue'),
  'erp/sale/form': () => import('@/views/erp/sale/form.vue'),
  'erp/stock/index': () => import('@/views/erp/stock/index.vue'),
  'erp/sales-analysis/index': () => import('@/views/erp/sales-analysis/index.vue'),
  'erp/sales-report/index': () => import('@/views/erp/sales-report/index.vue'),
  'erp/stock-in/index': () => import('@/views/erp/stock-in/index.vue'),
  'erp/stock-out/index': () => import('@/views/erp/stock-out/index.vue'),
  'erp/stock/replenishment/index': () => import('@/views/erp/stock/replenishment/index.vue'),
  'erp/stocktake/index': () => import('@/views/erp/stocktake/index.vue'),
  'erp/return/index': () => import('@/views/erp/return/index.vue'),
  'erp/shipment/index': () => import('@/views/erp/shipment/index.vue'),
  // ── 价格引擎模块 ──'pricing/index': () => import('@/views/erp/pricing/index.vue'),
  'erp/pricing/index': () => import('@/views/erp/pricing/index.vue'),
  'erp/pricing/approval/index': () => import('@/views/erp/pricing/approval/index.vue'),
  'erp/pricing/tiers/index': () => import('@/views/erp/pricing/tiers/index.vue'),
  'erp/dashboard/index': () => import('@/views/erp/dashboard/index.vue'),
  'erp/purchase/index': () => import('@/views/erp/purchase/index.vue'),
  'erp/purchase/form': () => import('@/views/erp/purchase/form.vue'),
  'erp/purchase-contract/index': () => import('@/views/erp/purchase-contract/index.vue'),
  'erp/purchase-contract/form': () => import('@/views/erp/purchase-contract/form.vue'),
  'erp/batch/index': () => import('@/views/erp/batch/index.vue'),
  'erp/serial/index': () => import('@/views/erp/serial/index.vue'),
  'erp/serial/detail': () => import('@/views/erp/serial/detail.vue'),

  // ── 库存扩展模块 ──
  'erp/stock-cost-adjust/index': () => import('@/views/erp/stock-cost-adjust/index.vue'),
  'erp/stock-cost-adjust/form': () => import('@/views/erp/stock-cost-adjust/form.vue'),
  'erp/stock-overflow/index': () => import('@/views/erp/stock-overflow/index.vue'),
  'erp/stock-overflow/form': () => import('@/views/erp/stock-overflow/form.vue'),
  'erp/stock-damage/index': () => import('@/views/erp/stock-damage/index.vue'),
  'erp/stock-damage/form': () => import('@/views/erp/stock-damage/form.vue'),
  'erp/stock-transfer/index': () => import('@/views/erp/stock-transfer/index.vue'),
  'erp/stock-transfer/form': () => import('@/views/erp/stock-transfer/form.vue'),
  'erp/stock-replenishment/index': () => import('@/views/erp/stock/replenishment/index.vue'),
  'erp/stock-alert-config/index': () => import('@/views/erp/stock-alert-config/index.vue'),
  'erp/stock-bom/index': () => import('@/views/erp/stock-bom/index.vue'),
  'erp/stock-bom/form': () => import('@/views/erp/stock-bom/form.vue'),
  'erp/stock-out/form': () => import('@/views/erp/stock-out/form.vue'),
  'erp/stock-assemble/index': () => import('@/views/erp/stock-assemble/index.vue'),
  'erp/stock-assemble/form': () => import('@/views/erp/stock-assemble/form.vue'),
  'erp/stock-split/index': () => import('@/views/erp/stock-split/index.vue'),
  'erp/stock-split/form': () => import('@/views/erp/stock-split/form.vue'),

  // ── WMS 仓储管理模块 ──
  'wms/warehouse/index': () => import('@/views/wms/warehouse/index.vue'),
  'wms/location/index': () => import('@/views/wms/location/index.vue'),
  'wms/receipt/index': () => import('@/views/wms/receipt/index.vue'),
  'wms/putaway/index': () => import('@/views/wh/putaway-order/index.vue'),
  'wms/pick/index': () => import('@/views/wh/picking-order/index.vue'),
  'wms/wave/index': () => import('@/views/wms/wave/index.vue'),
  'wms/ship/index': () => import('@/views/wh/shipping-order/index.vue'),
  'wms/inventory/index': () => import('@/views/wms/inventory/index.vue'),
  'wms/move/index': () => import('@/views/wh/move-order/index.vue'),
  'wms/check/index': () => import('@/views/wms/check/index.vue'),
  'wms/event/index': () => import('@/views/wms/event/index.vue'),

  // ── 商城管理模块 ──
  'erp/mall/config/index': () => import('@/views/erp/mall/config/index.vue'),
  'erp/mall/user-audit/index': () => import('@/views/erp/mall/user-audit/index.vue'),
  'erp/mall/banner/index': () => import('@/views/erp/mall/banner/index.vue'),
  'erp/mall/order/index': () => import('@/views/erp/mall/order/index.vue'),
  'erp/mall/product/index': () => import('@/views/erp/mall/product/index.vue'),
  // 商城顶级路由
  'mall/config/index': () => import('@/views/erp/mall/config/index.vue'),
  'mall/user-audit/index': () => import('@/views/erp/mall/user-audit/index.vue'),
  'mall/banner/index': () => import('@/views/erp/mall/banner/index.vue'),
  'mall/order/index': () => import('@/views/erp/mall/order/index.vue'),
  'mall/product/index': () => import('@/views/erp/mall/product/index.vue'),

  'fixed-asset/index': () => import('@/views/fixed-asset/index.vue'),
  'fixed-asset/asset/index': () => import('@/views/fixed-asset/asset/index.vue'),
  'fixed-asset/asset/detail': () => import('@/views/fixed-asset/asset/AssetDetail.vue'),
  'fixed-asset/category/index': () => import('@/views/fixed-asset/category/index.vue'),
  'fixed-asset/depreciation/index': () => import('@/views/fixed-asset/depreciation/index.vue'),
  'fixed-asset/transfer/index': () => import('@/views/fixed-asset/transfer/index.vue'),
  'fixed-asset/disposal/index': () => import('@/views/fixed-asset/disposal/index.vue'),
  'fixed-asset/inventory/index': () => import('@/views/fixed-asset/inventory/index.vue'),
  'fixed-asset/report/index': () => import('@/views/fixed-asset/report/index.vue'),
  'fixed-asset/purchase/index': () => import('@/views/fixed-asset/purchase/index.vue'),

  // ── 费用管理模块 ──
  'erp/expense/application/index': () => import('@/views/erp/expense/application/index.vue'),
  'erp/expense/application/detail': () => import('@/views/erp/expense/application/detail.vue'),
  'erp/expense/reimbursement/index': () => import('@/views/erp/expense/reimbursement/index.vue'),
  'erp/expense/reimbursement/detail': () => import('@/views/erp/expense/reimbursement/detail.vue'),
  'erp/expense/approval/index': () => import('@/views/erp/expense/approval/index.vue'),
  'erp/expense/payment/index': () => import('@/views/erp/expense/payment/index.vue'),
  'erp/expense/statistics/index': () => import('@/views/erp/expense/statistics/index.vue'),

  'budget/index': () => import('@/views/budget/index.vue'),
  'budget/template/index': () => import('@/views/budget/template/index.vue'),
  'budget/annual/index': () => import('@/views/budget/annual/index.vue'),
  'budget/annual/detail': () => import('@/views/budget/annual/BudgetDetail.vue'),
  'budget/adjustment/index': () => import('@/views/budget/adjustment/index.vue'),
  'budget/report/index': () => import('@/views/budget/report/index.vue'),

  // ── DMS 配送管理模块 ──
  'dms/dashboard/index': () => import('@/views/dms/dashboard/index.vue'),
  'dms/channel/index': () => import('@/views/dms/channel/index.vue'),
  'dms/rider/index': () => import('@/views/dms/rider/index.vue'),
  'dms/vehicle/index': () => import('@/views/dms/vehicle/index.vue'),
  'dms/vehicle/maintenance': () => import('@/views/dms/vehicle/maintenance.vue'),
  'dms/verification/index': () => import('@/views/dms/verification/index.vue'),
  'dms/verification/binding-detail': () => import('@/views/dms/verification/binding-detail.vue'),
  'dms/route/index': () => import('@/views/dms/route/index.vue'),
  'dms/dispatch/index': () => import('@/views/dms/dispatch/index.vue'),
  'dms/order-pool/index': () => import('@/views/dms/order-pool/index.vue'),
  'dms/order-pool/bid-detail': () => import('@/views/dms/order-pool/bid-detail.vue'),
  'dms/config/index': () => import('@/views/dms/config/index.vue'),
  'dms/tracking/index': () => import('@/views/dms/tracking/index.vue'),

  // ── 系统级新增路由 ──
  'admin/tenant/approval': () => import('@/views/admin/tenant/approval/index.vue'),
  'admin/monitor/log': () => import('@/views/system/log/index.vue'),
  'admin/tenant/list': () => import('@/views/admin/tenant/list/index.vue'),
  'admin/tenant/package': () => import('@/views/admin/tenant/package/index.vue'),
  'admin/tenant/module-auth': () => import('@/views/admin/tenant/module-auth/index.vue'),
  'admin/tenant/quota': () => import('@/views/admin/tenant/quota/index.vue'),
  'admin/monitor/health': () => import('@/views/admin/monitor/health/index.vue'),
  'admin/monitor/performance': () => import('@/views/admin/monitor/performance/index.vue'),
  'admin/monitor/api': () => import('@/views/admin/monitor/api/index.vue'),
  'admin/monitor/audit': () => import('@/views/admin/monitor/audit/index.vue'),
  'admin/monitor/cache': () => import('@/views/admin/monitor/cache/index.vue'),
  'admin/data/connection': () => import('@/views/admin/data/connection/index.vue'),
  'admin/data/slow-query': () => import('@/views/admin/data/slow-query/index.vue'),
  'admin/data/backup': () => import('@/views/admin/data/backup/index.vue'),
  'admin/data/sync': () => import('@/views/admin/data/sync/index.vue'),
  'admin/data/cleanup': () => import('@/views/admin/data/cleanup/index.vue'),
  'admin/module/list': () => import('@/views/admin/module/list/index.vue'),
  'admin/module/version': () => import('@/views/admin/module/version/index.vue'),
  'admin/module/release': () => import('@/views/admin/module/release/index.vue'),
  'admin/module/usage': () => import('@/views/admin/module/usage/index.vue'),
  'admin/dev/template': () => import('@/views/admin/dev/template/index.vue'),
  'admin/dev/api-doc': () => import('@/views/admin/dev/api-doc/index.vue'),
  'admin/dev/api-test': () => import('@/views/admin/dev/api-test/index.vue'),
  'admin/dev/scheduler': () => import('@/views/admin/dev/scheduler/index.vue'),
  'admin/platform/params': () => import('@/views/admin/platform/params/index.vue'),
  'admin/platform/mail': () => import('@/views/admin/platform/mail/index.vue'),
  'admin/platform/sms': () => import('@/views/admin/platform/sms/index.vue'),
  'admin/platform/storage': () => import('@/views/admin/platform/storage/index.vue'),
  'admin/platform/security': () => import('@/views/admin/platform/security/index.vue'),

  // ── 系统级占位页面（新功能未实现时使用） ──
  'common/placeholder/index': () => import('@/views/common/placeholder/index.vue'),

  // ── displayMode=1 表单页路由（Phase 1 ERP 核心单据） ──
  'erp/shipment/form': () => import('@/views/erp/shipment/form.vue'),
  'erp/return/form': () => import('@/views/erp/return/form.vue'),
  'erp/stock-in/form': () => import('@/views/erp/stock-in/form.vue'),
  'erp/stocktake/form': () => import('@/views/erp/stocktake/form.vue'),

  // ── displayMode=1 表单页路由（Phase 2 WMS 仓储执行） ──
  'wms/receipt/form': () => import('@/views/wms/receipt/form.vue'),
  'wms/putaway/form': () => import('@/views/wh/putaway-order/form/index.vue'),
  'wms/pick/form': () => import('@/views/wh/picking-order/form/index.vue'),
  'wms/wave/form': () => import('@/views/wms/wave/form.vue'),
  'wms/ship/form': () => import('@/views/wh/shipping-order/form/index.vue'),
  'wms/move/form': () => import('@/views/wh/move-order/form/index.vue'),
  'wms/check/form': () => import('@/views/wms/check/form.vue'),

  // ── Phase 5B: 财务单据 form.vue ──
  'finance/receipt-doc/form': () => import('@/views/finance/receipt-doc/form.vue'),
  'finance/receipt-doc': () => import('@/views/finance/receipt-doc/index.vue'),
  'finance/receipt-doc/index': () => import('@/views/finance/receipt-doc/index.vue'),
  'finance/payment-doc/form': () => import('@/views/finance/payment-doc/form.vue'),
  'finance/payment-doc': () => import('@/views/finance/payment-doc/index.vue'),
  'finance/payment-doc/index': () => import('@/views/finance/payment-doc/index.vue'),
  'finance/expense-doc/form': () => import('@/views/finance/expense-doc/form.vue'),
  'finance/voucher/form': () => import('@/views/finance/voucher/form.vue'),
  'finance/advance-receipt/form': () => import('@/views/finance/advance-receipt/form.vue'),
  'finance/advance-payment/form': () => import('@/views/finance/advance-payment/form.vue'),
  'finance/ar-ap-adjust/form': () => import('@/views/finance/ar-ap-adjust/form.vue'),
  'finance/budget-plan/form': () => import('@/views/finance/budget-plan/form.vue'),

  // ── Phase 5C: DMS 模块 form.vue ──
  'dms/rider/form': () => import('@/views/dms/rider/form.vue'),
  'dms/vehicle/form': () => import('@/views/dms/vehicle/form.vue'),

  // ── displayMode=1 列表页 URL→组件映射（有实际列表组件） ──
  'sales/order': () => import('@/views/erp/sale/index.vue'),
  'purchase/order': () => import('@/views/erp/purchase/index.vue'),
  'sales/return': () => import('@/views/erp/return/index.vue'),
  'sales/shipment': () => import('@/views/erp/shipment/index.vue'),
  'wms/receipt': () => import('@/views/wms/receipt/index.vue'),
  'wms/putaway': () => import('@/views/wh/putaway-order/index.vue'),
  'wms/pick': () => import('@/views/wh/picking-order/index.vue'),
  'wms/wave': () => import('@/views/wms/wave/index.vue'),
  'wms/ship': () => import('@/views/wh/shipping-order/index.vue'),
  'wms/move': () => import('@/views/wh/move-order/index.vue'),
  'wms/check': () => import('@/views/wms/check/index.vue'),
  'mall/user-audit': () => import('@/views/erp/mall/user-audit/index.vue'),
  'crm/quotation': () => import('@/views/crm/quotation/index.vue'),
  'crm/invoice': () => import('@/views/crm/invoice/index.vue'),
  'crm/customer': () => import('@/views/crm/customer/index.vue'),
  'crm/lead': () => import('@/views/crm/lead/index.vue'),
  'crm/opportunity': () => import('@/views/crm/opportunity/index.vue'),
  'crm/contract': () => import('@/views/crm/contract/index.vue'),
  'dms/rider': () => import('@/views/dms/rider/index.vue'),
  'dms/vehicle': () => import('@/views/dms/vehicle/index.vue'),
  'erp/return': () => import('@/views/erp/return/index.vue'),
  'erp/shipment': () => import('@/views/erp/shipment/index.vue'),

  // ── displayMode=1 列表页 URL→组件映射（Phase 4 - 全量实现） ──
  // 销售模块
  'sales/visit-plan': () => import('@/views/sales/visit-plan/index.vue'),
  'sales/visit-plan/index': () => import('@/views/sales/visit-plan/index.vue'),
  'sales/visit-exec': () => import('@/views/sales/visit-exec/index.vue'),
  'sales/visit-exec/index': () => import('@/views/sales/visit-exec/index.vue'),
  'sales/visit-review': () => import('@/views/sales/visit-review/index.vue'),
  'sales/visit-review/index': () => import('@/views/sales/visit-review/index.vue'),
  'sales/return-apply': () => import('@/views/sales/return-apply/index.vue'),
  'sales/return-apply/index': () => import('@/views/sales/return-apply/index.vue'),
  'sales/return-apply/form': () => import('@/views/sales/return-apply/form.vue'),
  'sales/return-apply/form/:id': () => import('@/views/sales/return-apply/form.vue'),
  'sales/return-apply/create': () => import('@/views/sales/return-apply/form.vue'),
  'sales/pre-order/index': () => import('@/views/sales/pre-order/index.vue'),
  'sales/pre-order/form': () => import('@/views/sales/pre-order/form.vue'),
  'sales/pre-order/form/:id': () => import('@/views/sales/pre-order/form.vue'),
  'sales/pre-order/create': () => import('@/views/sales/pre-order/form.vue'),
  'sales/retail': () => import('@/views/sales/retail/index.vue'),
  'sales/retail/index': () => import('@/views/sales/retail/index.vue'),
  'sales/retail/form': () => import('@/views/sales/retail/form.vue'),
  'sales/retail/form/:id': () => import('@/views/sales/retail/form.vue'),
  'sales/retail/create': () => import('@/views/sales/retail/form.vue'),
  'sales/outbound': () => import('@/views/sales/outbound/index.vue'),
  'sales/outbound/index': () => import('@/views/sales/outbound/index.vue'),
  'sales/outbound/form': () => import('@/views/sales/outbound/form.vue'),
  'sales/outbound/form/:id': () => import('@/views/sales/outbound/form.vue'),
  'sales/outbound/create': () => import('@/views/sales/outbound/form.vue'),
  'sales/return-doc': () => import('@/views/sales/return-doc/index.vue'),
  'sales/return-doc/index': () => import('@/views/sales/return-doc/index.vue'),
  'sales/return-doc/form': () => import('@/views/sales/return-doc/form.vue'),
  'sales/return-doc/form/:id': () => import('@/views/sales/return-doc/form.vue'),
  'sales/return-doc/create': () => import('@/views/sales/return-doc/form.vue'),
  'sales/exchange': () => import('@/views/sales/exchange/index.vue'),
  'sales/exchange/index': () => import('@/views/sales/exchange/index.vue'),
  'sales/exchange/form': () => import('@/views/sales/exchange/form.vue'),
  'sales/doc-query': () => import('@/views/sales/doc-query/index.vue'),
  'sales/doc-query/index': () => import('@/views/sales/doc-query/index.vue'),
  'sales/detail-query': () => import('@/views/sales/detail-query/index.vue'),
  'sales/detail-query/index': () => import('@/views/sales/detail-query/index.vue'),
  'sales/price-track': () => import('@/views/sales/price-track/index.vue'),
  'sales/price-track/index': () => import('@/views/sales/price-track/index.vue'),

  // 采购模块
  'purchase/alert-replenish': () => import('@/views/purchase/alert-replenish/index.vue'),
  'purchase/alert-replenish/index': () => import('@/views/purchase/alert-replenish/index.vue'),
  'purchase/shortage-replenish': () => import('@/views/purchase/shortage-replenish/index.vue'),
  'purchase/shortage-replenish/index': () => import('@/views/purchase/shortage-replenish/index.vue'),
  'purchase/smart-replenish': () => import('@/views/purchase/smart-replenish/index.vue'),
  'purchase/smart-replenish/index': () => import('@/views/purchase/smart-replenish/index.vue'),
  'purchase/sales-driven': () => import('@/views/purchase/sales-driven/index.vue'),
  'purchase/sales-driven/index': () => import('@/views/purchase/sales-driven/index.vue'),
  'purchase/inbound': () => import('@/views/purchase/inbound/index.vue'),
  'purchase/inbound/index': () => import('@/views/purchase/inbound/index.vue'),
  'purchase/inbound/form': () => import('@/views/purchase/inbound/form.vue'),
  'purchase/return': () => import('@/views/purchase/return/index.vue'),
  'purchase/return/index': () => import('@/views/purchase/return/index.vue'),
  'purchase/return/form': () => import('@/views/purchase/return/form.vue'),
  'purchase/exchange': () => import('@/views/purchase/exchange/index.vue'),
  'purchase/exchange/index': () => import('@/views/purchase/exchange/index.vue'),
  'purchase/exchange/form': () => import('@/views/purchase/exchange/form.vue'),
  'purchase/cost-sharing': () => import('@/views/purchase/cost-sharing/index.vue'),
  'purchase/cost-sharing/index': () => import('@/views/purchase/cost-sharing/index.vue'),
  'purchase/cost-sharing/form': () => import('@/views/purchase/cost-sharing/form.vue'),
  'purchase/doc-query': () => import('@/views/purchase/doc-query/index.vue'),
  'purchase/doc-query/index': () => import('@/views/purchase/doc-query/index.vue'),
  'purchase/detail-query': () => import('@/views/purchase/detail-query/index.vue'),
  'purchase/detail-query/index': () => import('@/views/purchase/detail-query/index.vue'),
  'purchase/price-track': () => import('@/views/purchase/price-track/index.vue'),
  'purchase/price-track/index': () => import('@/views/purchase/price-track/index.vue'),

  // 配送模块
  'dispatch/query': () => import('@/views/dispatch/query/index.vue'),
  'dispatch/query/index': () => import('@/views/dispatch/query/index.vue'),
  'dispatch/logistics-ship': () => import('@/views/dispatch/logistics-ship/index.vue'),
  'dispatch/logistics-ship/index': () => import('@/views/dispatch/logistics-ship/index.vue'),
  'dispatch/ship-query': () => import('@/views/dispatch/ship-query/index.vue'),
  'dispatch/ship-query/index': () => import('@/views/dispatch/ship-query/index.vue'),
  'dispatch/return-receive': () => import('@/views/dispatch/return-receive/index.vue'),
  'dispatch/return-receive/index': () => import('@/views/dispatch/return-receive/index.vue'),
  'dispatch/purchase-receive': () => import('@/views/dispatch/purchase-receive/index.vue'),
  'dispatch/purchase-receive/index': () => import('@/views/dispatch/purchase-receive/index.vue'),
  'dispatch/dispatch-order': () => import('@/views/dispatch/dispatch-order/index.vue'),
  'dispatch/dispatch-order/index': () => import('@/views/dispatch/dispatch-order/index.vue'),

  // 财务模块
  'finance/expense-doc': () => import('@/views/finance/expense-doc/index.vue'),
  'finance/expense-doc/index': () => import('@/views/finance/expense-doc/index.vue'),
  'finance/other-income': () => import('@/views/finance/other-income-doc/index.vue'),
  'finance/other-income/index': () => import('@/views/finance/other-income-doc/index.vue'),
  'finance/ar-ap-adjust': () => import('@/views/finance/ar-ap-adjust/index.vue'),
  'finance/ar-ap-adjust/index': () => import('@/views/finance/ar-ap-adjust/index.vue'),
  'finance/account-delivery': () => import('@/views/finance/account-delivery/index.vue'),
  'finance/account-delivery/index': () => import('@/views/finance/account-delivery/index.vue'),
  'finance/general-ledger': () => import('@/views/finance/general-ledger/index.vue'),
  'finance/general-ledger/index': () => import('@/views/finance/general-ledger/index.vue'),
  'finance/detail-ledger': () => import('@/views/finance/detail-ledger/index.vue'),
  'finance/detail-ledger/index': () => import('@/views/finance/detail-ledger/index.vue'),
  'finance/balance-sheet': () => import('@/views/finance/balance-sheet/index.vue'),
  'finance/balance-sheet/index': () => import('@/views/finance/balance-sheet/index.vue'),
  'finance/aux-balance': () => import('@/views/finance/aux-balance/index.vue'),
  'finance/aux-balance/index': () => import('@/views/finance/aux-balance/index.vue'),
  'finance/balance-report': () => import('@/views/finance/balance-report/index.vue'),
  'finance/balance-report/index': () => import('@/views/finance/balance-report/index.vue'),
  'finance/profit-report': () => import('@/views/finance/profit-report/index.vue'),
  'finance/profit-report/index': () => import('@/views/finance/profit-report/index.vue'),
  'finance/expense-apply': () => import('@/views/finance/expense-apply/index.vue'),
  'finance/expense-apply/index': () => import('@/views/finance/expense-apply/index.vue'),
  'finance/expense-reimburse': () => import('@/views/finance/expense-reimburse/index.vue'),
  'finance/expense-reimburse/index': () => import('@/views/finance/expense-reimburse/index.vue'),
  'finance/expense-approval': () => import('@/views/finance/expense-approval/index.vue'),
  'finance/expense-approval/index': () => import('@/views/finance/expense-approval/index.vue'),
  'finance/expense-pay': () => import('@/views/finance/expense-pay/index.vue'),
  'finance/expense-pay/index': () => import('@/views/finance/expense-pay/index.vue'),
  'finance/expense-stats': () => import('@/views/finance/expense-stats/index.vue'),
  'finance/expense-stats/index': () => import('@/views/finance/expense-stats/index.vue'),
  'finance/advance-receipt': () => import('@/views/finance/advance-receipt/index.vue'),
  'finance/advance-receipt/index': () => import('@/views/finance/advance-receipt/index.vue'),
  'finance/cash-transfer': () => import('@/views/finance/cash-transfer/index.vue'),
  'finance/cash-transfer/index': () => import('@/views/finance/cash-transfer/index.vue'),

  // ── Finance 财务模块补充页 ──
  'finance/receipt-by-doc': () => import('@/views/finance/receipt-by-doc/index.vue'),
  'finance/receipt-by-doc/index': () => import('@/views/finance/receipt-by-doc/index.vue'),
  'finance/pending-confirm': () => import('@/views/finance/pending-confirm/index.vue'),
  'finance/pending-confirm/index': () => import('@/views/finance/pending-confirm/index.vue'),
  'finance/online-payment-reconcile': () => import('@/views/finance/online-payment-reconcile/index.vue'),
  'finance/online-payment-reconcile/index': () => import('@/views/finance/online-payment-reconcile/index.vue'),
  'finance/payment-by-doc': () => import('@/views/finance/payment-by-doc/index.vue'),
  'finance/payment-by-doc/index': () => import('@/views/finance/payment-by-doc/index.vue'),
  'finance/month-closing': () => import('@/views/finance/month-closing/index.vue'),
  'finance/month-closing/index': () => import('@/views/finance/month-closing/index.vue'),
  'finance/budget-plan': () => import('@/views/finance/budget-plan/index.vue'),
  'finance/budget-plan/index': () => import('@/views/finance/budget-plan/index.vue'),
  'finance/budget-exec': () => import('@/views/finance/budget-exec/index.vue'),
  'finance/budget-exec/index': () => import('@/views/finance/budget-exec/index.vue'),
  'finance/advance-payment': () => import('@/views/finance/advance-payment/index.vue'),
  'finance/advance-payment/index': () => import('@/views/finance/advance-payment/index.vue'),
  'finance/other-income-doc': () => import('@/views/finance/other-income-doc/index.vue'),
  'finance/other-income-doc/index': () => import('@/views/finance/other-income-doc/index.vue'),
  'finance/other-income-doc/form': () => import('@/views/finance/other-income-doc/form.vue'),

  // CRM 补充
  'crm/customer-follow': () => import('@/views/crm/customer-follow/index.vue'),
  'crm/customer-follow/index': () => import('@/views/crm/customer-follow/index.vue'),
  'crm/customer-grade': () => import('@/views/crm/customer-grade/index.vue'),
  'crm/customer-grade/index': () => import('@/views/crm/customer-grade/index.vue'),
  'crm/lead-convert': () => import('@/views/crm/lead-convert/index.vue'),
  'crm/lead-convert/index': () => import('@/views/crm/lead-convert/index.vue'),
  'crm/opportunity-stage': () => import('@/views/crm/opportunity-stage/index.vue'),
  'crm/opportunity-stage/index': () => import('@/views/crm/opportunity-stage/index.vue'),
  'crm/contract-approval': () => import('@/views/crm/contract-approval/index.vue'),
  'crm/contract-approval/index': () => import('@/views/crm/contract-approval/index.vue'),
  'crm/funnel': () => import('@/views/crm/funnel/index.vue'),
  'crm/funnel/index': () => import('@/views/crm/funnel/index.vue'),
  'crm/customer-analysis': () => import('@/views/crm/customer-analysis/index.vue'),
  'crm/customer-analysis/index': () => import('@/views/crm/customer-analysis/index.vue'),

  // ── HR 人力资源模块 ──
  'hr/employee/list': () => import('@/views/hr/employee/list.vue'),
  'hr/employee/index': () => import('@/views/hr/employee/index.vue'),
  'hr/employee/form': () => import('@/views/hr/employee/form.vue'),
  'hr/attendance/list': () => import('@/views/hr/attendance/list.vue'),
  'hr/attendance/index': () => import('@/views/hr/attendance/index.vue'),
  'hr/leave/list': () => import('@/views/hr/leave/list.vue'),
  'hr/leave/index': () => import('@/views/hr/leave/index.vue'),
  'hr/salary/list': () => import('@/views/hr/salary/list.vue'),
  'hr/salary/index': () => import('@/views/hr/salary/index.vue'),
  'hr/salary/form': () => import('@/views/hr/salary/form.vue'),
  'hr/performance/list': () => import('@/views/hr/performance/list.vue'),
  'hr/performance/index': () => import('@/views/hr/performance/index.vue'),
  'hr/recruitment/list': () => import('@/views/hr/recruitment/list.vue'),
  'hr/organization/position-list': () => import('@/views/hr/organization/position-list.vue'),
  'hr/organization/index': () => import('@/views/hr/organization/index.vue'),

  // ── Trade 交易模块 ──
  'trade/mall-order/list': () => import('@/views/trade/mall-order/list.vue'),
  'trade/cart/list': () => import('@/views/trade/cart/list.vue'),
  'trade/api-monitor/list': () => import('@/views/trade/api-monitor/list.vue'),
  'trade/inventory-sync/list': () => import('@/views/trade/inventory-sync/list.vue'),
  'trade/external-order/list': () => import('@/views/trade/external-order/list.vue'),
  'trade/channel/config': () => import('@/views/trade/channel/config.vue'),

  // ── Quality 质量模块 ──
  'quality/inspection/form': () => import('@/views/quality/inspection/form.vue'),
  'quality/inspection/list': () => import('@/views/quality/inspection/list.vue'),
  'quality/standard/list': () => import('@/views/quality/standard/list.vue'),
  'quality/standard/form': () => import('@/views/quality/standard/form.vue'),
  'quality/defect/list': () => import('@/views/quality/defect/list.vue'),
  'quality/certificate/list': () => import('@/views/quality/certificate/list.vue'),

  // ── Payment 支付模块 ──
  'payment/request/list': () => import('@/views/payment/request/list.vue'),
  'payment/record/list': () => import('@/views/payment/record/list.vue'),
  'payment/refund/list': () => import('@/views/payment/refund/list.vue'),
  'payment/reconciliation/daily': () => import('@/views/payment/reconciliation/daily.vue'),

  // ── 资料管理 ──
  'md/barcode': () => import('@/views/md/barcode/index.vue'),
  'md/barcode/index': () => import('@/views/md/barcode/index.vue'),
  'md/product-price': () => import('@/views/md/product-price/index.vue'),
  'md/product-price/index': () => import('@/views/md/product-price/index.vue'),
  // 商品辅助资料：历史桩目录 views/md/product-aux 已删除，统一收敛到 product-supplement
  'md/product-aux': () => import('@/views/md/product-supplement/index.vue'),
  'md/product-aux/index': () => import('@/views/md/product-supplement/index.vue'),
  'md/product-supplement': () => import('@/views/md/product-supplement/index.vue'),
  'md/product-supplement/index': () => import('@/views/md/product-supplement/index.vue'),
  'md/image': () => import('@/views/md/image/index.vue'),
  'md/image/index': () => import('@/views/md/image/index.vue'),
  'md/linked-account': () => import('@/views/md/linked-account/index.vue'),
  'md/linked-account/index': () => import('@/views/md/linked-account/index.vue'),
  'md/location': () => import('@/views/md/location/index.vue'),
  'md/location/index': () => import('@/views/md/location/index.vue'),
  'md/route': () => import('@/views/md/route/index.vue'),
  'md/route/index': () => import('@/views/md/route/index.vue'),
  'md/bank-account': () => import('@/views/md/bank-account/index.vue'),
  'md/bank-account/index': () => import('@/views/md/bank-account/index.vue'),
  'md/expense-type': () => import('@/views/md/expense-type/index.vue'),
  'md/expense-type/index': () => import('@/views/md/expense-type/index.vue'),
  'md/other-income': () => import('@/views/md/other-income/index.vue'),
  'md/other-income/index': () => import('@/views/md/other-income/index.vue'),
  'md/accounting-subject': () => import('@/views/md/accounting-subject/index.vue'),
  'md/accounting-subject/index': () => import('@/views/md/accounting-subject/index.vue'),

  // 系统设置
  'set/initial-stock': () => import('@/views/set/initial-stock/index.vue'),
  'set/initial-stock/index': () => import('@/views/set/initial-stock/index.vue'),
  'set/initial-finance': () => import('@/views/set/initial-finance/index.vue'),
  'set/initial-finance/index': () => import('@/views/set/initial-finance/index.vue'),
  'set/rebuild': () => import('@/views/set/rebuild/index.vue'),
  'set/rebuild/index': () => import('@/views/set/rebuild/index.vue'),
  'set/system-task': () => import('@/views/set/system-task/index.vue'),
  'set/system-task/index': () => import('@/views/set/system-task/index.vue'),
  'set/accounting-period': () => import('@/views/set/accounting-period/index.vue'),
  'set/accounting-period/index': () => import('@/views/set/accounting-period/index.vue'),

  // ── 系统设置 - 系统配置子页面 ──
  'set/operation-log': () => import('@/views/set/operation-log/index.vue'),
  'set/operation-log/index': () => import('@/views/set/operation-log/index.vue'),
  'set/sys-params': () => import('@/views/set/sys-params/index.vue'),
  'set/sys-params/index': () => import('@/views/set/sys-params/index.vue'),
  'set/company-info': () => import('@/views/set/company-info/index.vue'),
  'set/company-info/index': () => import('@/views/set/company-info/index.vue'),
  'set/menu-config': () => import('@/views/set/menu-config/index.vue'),
  'set/menu-config/index': () => import('@/views/set/menu-config/index.vue'),
  'set/audit-config': () => import('@/views/set/audit-config/index.vue'),
  'set/audit-config/index': () => import('@/views/set/audit-config/index.vue'),
  'set/audit-config/form': () => import('@/views/set/audit-config/form.vue'),
  'set/payment-config': () => import('@/views/set/payment-config/index.vue'),
  'set/payment-config/index': () => import('@/views/set/payment-config/index.vue'),
  'set/app-center': () => import('@/views/set/app-center/index.vue'),
  'set/app-center/index': () => import('@/views/set/app-center/index.vue'),

  // ── displayMode=1 添加标签的 form 页面（Phase 3 CRM） ──
  'crm/customer/form': () => import('@/views/crm/customer/form.vue'),
  'crm/lead/form': () => import('@/views/crm/lead/form.vue'),
  'crm/opportunity/form': () => import('@/views/crm/opportunity/form.vue'),
  'crm/contract/form': () => import('@/views/crm/contract/form.vue'),
  'crm/quotation/form': () => import('@/views/crm/quotation/form.vue'),
  'crm/invoice/form': () => import('@/views/crm/invoice/form.vue'),
  'md/product/form': () => import('@/views/erp/product/form.vue'),
  'md/product': () => import('@/views/erp/product/index.vue'),
  'md/customer/form': () => import('@/views/md/customer/form.vue'),
  'md/supplier/form': () => import('@/views/md/supplier/form.vue'),
  'md/logistics/form': () => import('@/views/md/logistics/form.vue'),
  'md/partner/form': () => import('@/views/md/partner/form.vue'),

  // ── 资料 > 往来单位 列表页（基础路径 + /index 双映射） ──
  'md/customer': () => import('@/views/md/customer/index.vue'),
  'md/supplier': () => import('@/views/md/supplier/index.vue'),
  'md/logistics': () => import('@/views/md/logistics/index.vue'),
  'md/partner': () => import('@/views/md/partner/index.vue'),
  'md/customer/index': () => import('@/views/md/customer/index.vue'),
  'md/supplier/index': () => import('@/views/md/supplier/index.vue'),
  'md/logistics/index': () => import('@/views/md/logistics/index.vue'),
  'md/partner/index': () => import('@/views/md/partner/index.vue'),

  // ── Marketing 营销模块 ──
  'marketing/member-manage': () => import('@/views/marketing/member-manage/index.vue'),
  'marketing/member-manage/index': () => import('@/views/marketing/member-manage/index.vue'),
  'marketing/points-exchange': () => import('@/views/marketing/points-exchange/index.vue'),
  'marketing/points-exchange/index': () => import('@/views/marketing/points-exchange/index.vue'),
  'marketing/member-config': () => import('@/views/marketing/member-config/index.vue'),
  'marketing/member-config/index': () => import('@/views/marketing/member-config/index.vue'),
  'marketing/sms-send': () => import('@/views/marketing/sms-send/index.vue'),
  'marketing/sms-send/index': () => import('@/views/marketing/sms-send/index.vue'),
  'marketing/coupon': () => import('@/views/marketing/coupon/index.vue'),
  'marketing/coupon/index': () => import('@/views/marketing/coupon/index.vue'),
  'marketing/product-promo': () => import('@/views/marketing/product-promo/index.vue'),
  'marketing/product-promo/index': () => import('@/views/marketing/product-promo/index.vue'),
  'marketing/order-promo': () => import('@/views/marketing/order-promo/index.vue'),
  'marketing/order-promo/index': () => import('@/views/marketing/order-promo/index.vue'),
  'marketing/special-price': () => import('@/views/marketing/special-price/index.vue'),
  'marketing/special-price/index': () => import('@/views/marketing/special-price/index.vue'),
  'marketing/package-deal': () => import('@/views/marketing/package-deal/index.vue'),
  'marketing/package-deal/index': () => import('@/views/marketing/package-deal/index.vue'),
  'marketing/mall-group': () => import('@/views/marketing/mall-group/index.vue'),
  'marketing/mall-group/index': () => import('@/views/marketing/mall-group/index.vue'),
  'marketing/mall-flash': () => import('@/views/marketing/mall-flash/index.vue'),
  'marketing/mall-flash/index': () => import('@/views/marketing/mall-flash/index.vue'),
  'marketing/mall-presale': () => import('@/views/marketing/mall-presale/index.vue'),
  'marketing/mall-presale/index': () => import('@/views/marketing/mall-presale/index.vue'),
  'marketing/mall-popup': () => import('@/views/marketing/mall-popup/index.vue'),
  'marketing/mall-popup/index': () => import('@/views/marketing/mall-popup/index.vue'),
  'marketing/add-on-deal': () => import('@/views/marketing/add-on-deal/index.vue'),
  'marketing/add-on-deal/index': () => import('@/views/marketing/add-on-deal/index.vue'),
  'marketing/hot-keywords': () => import('@/views/marketing/hot-keywords/index.vue'),
  'marketing/hot-keywords/index': () => import('@/views/marketing/hot-keywords/index.vue'),
  'marketing/promote-create': () => import('@/views/marketing/promote-create/index.vue'),
  'marketing/promote-create/index': () => import('@/views/marketing/promote-create/index.vue'),
  'marketing/promote-history': () => import('@/views/marketing/promote-history/index.vue'),
  'marketing/promote-history/index': () => import('@/views/marketing/promote-history/index.vue'),

  // ── Mall 商城模块 ──
  'mall/order-process': () => import('@/views/mall/order-process/index.vue'),
  'mall/order-process/index': () => import('@/views/mall/order-process/index.vue'),
  'mall/return-process': () => import('@/views/mall/return-process/index.vue'),
  'mall/return-process/index': () => import('@/views/mall/return-process/index.vue'),
  'mall/product-shelf': () => import('@/views/mall/product-shelf/index.vue'),
  'mall/product-shelf/index': () => import('@/views/mall/product-shelf/index.vue'),
  'mall/unit-display': () => import('@/views/mall/unit-display/index.vue'),
  'mall/unit-display/index': () => import('@/views/mall/unit-display/index.vue'),
  'mall/buyer-apply': () => import('@/views/mall/buyer-apply/index.vue'),
  'mall/buyer-apply/index': () => import('@/views/mall/buyer-apply/index.vue'),
  'mall/buyer-account': () => import('@/views/mall/buyer-account/index.vue'),
  'mall/buyer-account/index': () => import('@/views/mall/buyer-account/index.vue'),
  'mall/product-combo': () => import('@/views/mall/product-combo/index.vue'),
  'mall/product-combo/index': () => import('@/views/mall/product-combo/index.vue'),
  'mall/basic-config': () => import('@/views/mall/basic-config/index.vue'),
  'mall/basic-config/index': () => import('@/views/mall/basic-config/index.vue'),
  'mall/shop-config': () => import('@/views/mall/shop-config/index.vue'),
  'mall/shop-config/index': () => import('@/views/mall/shop-config/index.vue'),
  'mall/freight-config': () => import('@/views/mall/freight-config/index.vue'),
  'mall/freight-config/index': () => import('@/views/mall/freight-config/index.vue'),
  'mall/shop-decoration': () => import('@/views/mall/shop-decoration/index.vue'),
  'mall/shop-decoration/index': () => import('@/views/mall/shop-decoration/index.vue'),
  'mall/notice-config': () => import('@/views/mall/notice-config/index.vue'),
  'mall/notice-config/index': () => import('@/views/mall/notice-config/index.vue'),
  'mall/keyword-bank': () => import('@/views/mall/keyword-bank/index.vue'),
  'mall/keyword-bank/index': () => import('@/views/mall/keyword-bank/index.vue'),

  // ── Trade 交易模块（新增页面） ──
  'trade/mall-return': () => import('@/views/trade/mall-return/index.vue'),
  'trade/mall-return/index': () => import('@/views/trade/mall-return/index.vue'),

  // ── Member 会员模块 ──
  'member/profile': () => import('@/views/member/profile/index.vue'),
  'member/profile/index': () => import('@/views/member/profile/index.vue'),
  'member/points-history': () => import('@/views/member/points-history/index.vue'),
  'member/points-history/index': () => import('@/views/member/points-history/index.vue'),

  // ── Analytics 分析模块 ──
  'analytics/pending-approval': () => import('@/views/analytics/pending-approval/index.vue'),
  'analytics/pending-approval/index': () => import('@/views/analytics/pending-approval/index.vue'),
  'analytics/draft': () => import('@/views/analytics/draft/index.vue'),
  'analytics/draft/index': () => import('@/views/analytics/draft/index.vue'),
  'analytics/business-history': () => import('@/views/analytics/business-history/index.vue'),
  'analytics/business-history/index': () => import('@/views/analytics/business-history/index.vue'),
  'analytics/sales-performance': () => import('@/views/analytics/sales-performance/index.vue'),
  'analytics/sales-performance/index': () => import('@/views/analytics/sales-performance/index.vue'),
  'analytics/sales-analysis': () => import('@/views/analytics/sales-analysis/index.vue'),
  'analytics/sales-analysis/index': () => import('@/views/analytics/sales-analysis/index.vue'),
  'analytics/sales-fulfillment': () => import('@/views/analytics/sales-fulfillment/index.vue'),
  'analytics/sales-fulfillment/index': () => import('@/views/analytics/sales-fulfillment/index.vue'),
  'analytics/sales-debt': () => import('@/views/analytics/sales-debt/index.vue'),
  'analytics/sales-debt/index': () => import('@/views/analytics/sales-debt/index.vue'),
  'analytics/sales-expense': () => import('@/views/analytics/sales-expense/index.vue'),
  'analytics/sales-expense/index': () => import('@/views/analytics/sales-expense/index.vue'),
  'analytics/customer-active': () => import('@/views/analytics/customer-active/index.vue'),
  'analytics/customer-active/index': () => import('@/views/analytics/customer-active/index.vue'),
  'analytics/promotion-analysis': () => import('@/views/analytics/promotion-analysis/index.vue'),
  'analytics/promotion-analysis/index': () => import('@/views/analytics/promotion-analysis/index.vue'),
  'analytics/pre-order-query': () => import('@/views/analytics/pre-order-query/index.vue'),
  'analytics/pre-order-query/index': () => import('@/views/analytics/pre-order-query/index.vue'),
  'analytics/purchase-analysis': () => import('@/views/analytics/purchase-analysis/index.vue'),
  'analytics/purchase-analysis/index': () => import('@/views/analytics/purchase-analysis/index.vue'),
  'analytics/purchase-prep': () => import('@/views/analytics/purchase-prep/index.vue'),
  'analytics/purchase-prep/index': () => import('@/views/analytics/purchase-prep/index.vue'),
  'analytics/check-stock': () => import('@/views/analytics/check-stock/index.vue'),
  'analytics/check-stock/index': () => import('@/views/analytics/check-stock/index.vue'),
  'analytics/check-batch': () => import('@/views/analytics/check-batch/index.vue'),
  'analytics/check-batch/index': () => import('@/views/analytics/check-batch/index.vue'),
  'analytics/inventory-analysis': () => import('@/views/analytics/inventory-analysis/index.vue'),
  'analytics/inventory-analysis/index': () => import('@/views/analytics/inventory-analysis/index.vue'),
  'analytics/stock-detail': () => import('@/views/analytics/stock-detail/index.vue'),
  'analytics/stock-detail/index': () => import('@/views/analytics/stock-detail/index.vue'),
  'analytics/staff-commission': () => import('@/views/analytics/staff-commission/index.vue'),
  'analytics/staff-commission/index': () => import('@/views/analytics/staff-commission/index.vue'),
  'analytics/collection-stats': () => import('@/views/analytics/collection-stats/index.vue'),
  'analytics/collection-stats/index': () => import('@/views/analytics/collection-stats/index.vue'),
  'analytics/commission-center': () => import('@/views/analytics/commission-center/index.vue'),
  'analytics/commission-center/index': () => import('@/views/analytics/commission-center/index.vue'),
  'analytics/business-analysis': () => import('@/views/analytics/business-analysis/index.vue'),
  'analytics/business-analysis/index': () => import('@/views/analytics/business-analysis/index.vue'),
  'analytics/check-fund': () => import('@/views/analytics/check-fund/index.vue'),
  'analytics/check-fund/index': () => import('@/views/analytics/check-fund/index.vue'),
  'analytics/check-expense': () => import('@/views/analytics/check-expense/index.vue'),
  'analytics/check-expense/index': () => import('@/views/analytics/check-expense/index.vue'),
  'analytics/invoice-stats': () => import('@/views/analytics/invoice-stats/index.vue'),
  'analytics/invoice-stats/index': () => import('@/views/analytics/invoice-stats/index.vue'),
  'analytics/check-receivable': () => import('@/views/analytics/check-receivable/index.vue'),
  'analytics/check-receivable/index': () => import('@/views/analytics/check-receivable/index.vue'),
  'analytics/check-payable': () => import('@/views/analytics/check-payable/index.vue'),
  'analytics/check-payable/index': () => import('@/views/analytics/check-payable/index.vue'),
  'analytics/ar-balance-sheet': () => import('@/views/analytics/ar-balance-sheet/index.vue'),
  'analytics/ar-balance-sheet/index': () => import('@/views/analytics/ar-balance-sheet/index.vue'),
  'analytics/mkt-activity-analysis': () => import('@/views/analytics/mkt-activity-analysis/index.vue'),
  'analytics/mkt-activity-analysis/index': () => import('@/views/analytics/mkt-activity-analysis/index.vue'),
  'analytics/mkt-promote-analysis': () => import('@/views/analytics/mkt-promote-analysis/index.vue'),
  'analytics/mkt-promote-analysis/index': () => import('@/views/analytics/mkt-promote-analysis/index.vue'),
  'analytics/trade-analysis': () => import('@/views/analytics/trade-analysis/index.vue'),
  'analytics/trade-analysis/index': () => import('@/views/analytics/trade-analysis/index.vue'),
  'analytics/mall-customer-list': () => import('@/views/analytics/mall-customer-list/index.vue'),
  'analytics/mall-customer-list/index': () => import('@/views/analytics/mall-customer-list/index.vue'),

  // ── DMS 配送模块补充 ──
  'dms/route-list': () => import('@/views/dms/route-list/index.vue'),
  'dms/route-list/index': () => import('@/views/dms/route-list/index.vue'),
  'dms/dispatch-task': () => import('@/views/dms/dispatch-task/index.vue'),
  'dms/dispatch-task/index': () => import('@/views/dms/dispatch-task/index.vue'),
  'dms/realtime-tracking': () => import('@/views/dms/realtime-tracking/index.vue'),
  'dms/realtime-tracking/index': () => import('@/views/dms/realtime-tracking/index.vue'),
  'dms/config-params': () => import('@/views/dms/config-params/index.vue'),
  'dms/config-params/index': () => import('@/views/dms/config-params/index.vue'),

  // ── 预警查询 / WH仓储单据 ──
  'erp/alert-query': () => import('@/views/erp/alert-query/index.vue'),
  'erp/alert-query/index': () => import('@/views/erp/alert-query/index.vue'),
  'wh/borrow-in/form': () => import('@/views/wh/borrow-in/form/index.vue'),
  'wh/borrow-out/form': () => import('@/views/wh/borrow-out/form/index.vue'),
  'wh/receiving-order/form': () => import('@/views/wh/receiving-order/form/index.vue'),
  'wms/receive/form': () => import('@/views/wh/receiving-order/form/index.vue'),
  'wms/receive': () => import('@/views/wh/receiving-order/index.vue'),
  'wms/receive/index': () => import('@/views/wh/receiving-order/index.vue'),
  'wh/putaway-order/form': () => import('@/views/wh/putaway-order/form/index.vue'),
  'wh/picking-order/form': () => import('@/views/wh/picking-order/form/index.vue'),
  'wh/shipping-order/form': () => import('@/views/wh/shipping-order/form/index.vue'),
  'wh/move-order/form': () => import('@/views/wh/move-order/form/index.vue'),
  'wh/inventory-order/form': () => import('@/views/wh/inventory-order/form/index.vue'),
  'wh/borrow-query': () => import('@/views/wh/borrow-query/index.vue'),
  'wh/borrow-query/index': () => import('@/views/wh/borrow-query/index.vue'),
  // ── WMS 8 作业单列表页（批次 0.1 新增） ──
  'wh/borrow-in/index': () => import('@/views/wh/borrow-in/index.vue'),
  'wh/borrow-out/index': () => import('@/views/wh/borrow-out/index.vue'),
  'wh/receiving-order/index': () => import('@/views/wh/receiving-order/index.vue'),
  'wh/putaway-order/index': () => import('@/views/wh/putaway-order/index.vue'),
  'wh/picking-order/index': () => import('@/views/wh/picking-order/index.vue'),
  'wh/shipping-order/index': () => import('@/views/wh/shipping-order/index.vue'),
  'wh/move-order/index': () => import('@/views/wh/move-order/index.vue'),
  'wh/inventory-order/index': () => import('@/views/wh/inventory-order/index.vue'),

  // ── 订单中心 / 财务 ──
  'sales/order-center': () => import('@/views/sales/order-center/index.vue'),
  'sales/order-center/index': () => import('@/views/sales/order-center/index.vue'),
  'finance/cash-transfer/form': () => import('@/views/finance/cash-transfer/form/index.vue'),

  // ── 辅助核算 ──
  'finance/auxiliary/index': () => import('@/views/finance/auxiliary/index.vue'),
  'finance/auxiliary': () => import('@/views/finance/auxiliary/index.vue'),

  // ── 资料管理 (MD) ──
  'md/warehouse-plan': () => import('@/views/md/warehouse-plan/index.vue'),
  'md/warehouse-plan/index': () => import('@/views/md/warehouse-plan/index.vue'),
  'md/staff-dept': () => import('@/views/md/staff-dept/index.vue'),
  'md/staff-dept/index': () => import('@/views/md/staff-dept/index.vue'),
  'md/staff-role': () => import('@/views/md/staff-role/index.vue'),
  'md/staff-role/index': () => import('@/views/md/staff-role/index.vue'),
  'md/staff-all': () => import('@/views/md/staff-all/index.vue'),
  'md/staff-all/index': () => import('@/views/md/staff-all/index.vue'),
  'md/payment-method': () => import('@/views/md/payment-method/index.vue'),
  'md/payment-method/index': () => import('@/views/md/payment-method/index.vue'),
  'md/payment-channel': () => import('@/views/md/payment-channel/index.vue'),
  'md/payment-channel/index': () => import('@/views/md/payment-channel/index.vue'),
  'md/payment-account': () => import('@/views/md/payment-account/index.vue'),
  'md/payment-account/index': () => import('@/views/md/payment-account/index.vue'),

  // ── 配送单 ──
  'dispatch/dispatch-order/form': () => import('@/views/dispatch/dispatch-order/form/index.vue'),
}

/**
 * 路由路径 → 单据类型代码映射表
 * 用于自动为动态路由分配 billType 元数据，配合 checkRouteAccess 实现单据类型级路由访问控制
 *
 * 单据类型代码说明：
 *   504 = 采购订单, 601 = 销售出库, 604 = 销售订单, 801 = 收款单, 802 = 付款单
 */
const routeBillTypeMap: Record<string, string> = {
  // ── 销售订单 (604) ──
  'sale': '604',
  'sale/index': '604',
  'sale/order': '604',
  'erp/sale': '604',
  'erp/sales-analysis': '604',
  'erp/sales-report': '604',
  'sales/pre-order': '604',

  // ── 销售出库 (601) ──
  'stock': '601',
  'stock/index': '601',
  'stock/detail': '601',
  'erp/stock': '601',
  'erp/stock-in': '601',
  'erp/shipment': '601',
  'erp/stocktake': '601',
  'erp/stock-cost-adjust': '601',
  'erp/stock-overflow': '601',
  'erp/stock-damage': '601',
  'erp/stock-transfer': '601',
  'erp/stock-bom': '601',
  'erp/stock-assemble': '601',
  'erp/stock-split': '601',
  'erp/stock-alert-config': '601',
  'erp/stock-replenishment': '601',
  'purchase/exchange': '601',
  'erp/return': '601',
  'sales/outbound': '601',

  // ── 采购订单 (504) ──
  'purchase': '504',
  'purchase/index': '504',
  'purchase/order': '504',
  'purchase/inquiry': '504',
  'purchase/inbound': '504',
  'purchase/return': '504',

  // ── 收款单 (801) ──
  'finance/receivable': '801',
  'finance/receivable/index': '801',
  'finance/pre-receipt': '801',
  'finance/pre-receipt/index': '801',
  'finance/advance-receipt': '801',
  'finance/advance-receipt/index': '801',
  'finance/advance-receipt/form': '801',
  'finance/deposit': '801',
  'finance/deposit/index': '801',
  'erp/finance/receivable': '801',
  'finance/receipt': '801',
  'finance/receipt/index': '801',

  // ── 付款单 (802) ──
  'finance/payable': '802',
  'finance/payable/index': '802',
  'finance/pre-payment': '802',
  'finance/pre-payment/index': '802',
  'finance/advance-payment': '802',
  'finance/advance-payment/index': '802',
  'finance/advance-payment/form': '802',
  'erp/finance/payable': '802',
  'finance/payment': '802',
  'finance/payment/index': '802',
}

/**
 * 根据路由路径获取对应的单据类型代码
 * 移除动态路径参数（如 :id）后进行前缀匹配
 */
function getBillTypeForRoute(routePath: string): string | undefined {
  const normalized = routePath.replace(/^\/+|\/+$/g, '').replace(/\/:\w+\??/g, '').replace(/\/:\w+/g, '')

  // 精确匹配
  if (routeBillTypeMap[normalized]) return routeBillTypeMap[normalized]

  // 逐级前缀匹配（处理嵌套路由）
  const parts = normalized.split('/').filter(Boolean)
  for (let i = parts.length - 1; i >= 1; i--) {
    const prefix = parts.slice(0, i).join('/')
    if (routeBillTypeMap[prefix]) return routeBillTypeMap[prefix]
  }

  return undefined
}

function getComponent(componentPath: string) {
  const normalizedPath = componentPath.replace(/^views\//, '').replace(/\.vue$/, '')
  if (componentMap[normalizedPath]) {
    return componentMap[normalizedPath]
  }
  // 尝试追加 /index（数据库存 system/menu/index，路由转换后可能只传 system/menu）
  if (componentMap[normalizedPath + '/index']) {
    return componentMap[normalizedPath + '/index']
  }
  // 双入口支持：路径以 /index 结尾时，尝试去掉 /index 再查找（如 sales/order/index → sales/order）
  if (normalizedPath.endsWith('/index')) {
    const basePath = normalizedPath.slice(0, -'/index'.length)
    if (componentMap[basePath]) {
      return componentMap[basePath]
    }
  }
  // Vite 动态 import 变量只支持单层路径，多层路径无法解析，返回空组件
  if (normalizedPath.includes('/') || normalizedPath.includes('\\')) {
    // 尝试动态导入 index.vue（views/system/menu/index.vue 形式）
    return () => import(`../views/${normalizedPath}/index.vue`).catch(() => {
      console.warn('[动态路由] 组件加载失败:', normalizedPath)
      return defineComponent({
        render: () => h('div', { style: 'padding:40px;text-align:center;color:#999' }, '页面组件未找到: ' + normalizedPath)
      })
    })
  }
  return () => import(`../views/${normalizedPath}.vue`)
}

/**
 * 将后台菜单转换为 Vue Router 路由配置，返回数组以支持双入口（displayMode=1 时生成两条路由）
 */
function transformMenuToRoutes(menu: MenuItem, parentPath: string = ''): RouteRecordRaw[] {
  let routePath = menu.path || ''

  if (parentPath && menu.path && menu.path.startsWith(parentPath + '/')) {
    routePath = menu.path.substring(parentPath.length + 1)
  } else if (menu.path && menu.path.startsWith('/')) {
    routePath = menu.path.substring(1)
  }

  // 子路径与父路径完全相同时，视为空路径（子路由继承父路由的 URL）
  // 典型场景：工作台目录(path=dashboard) 的子菜单(path=dashboard)
  if (routePath && routePath === parentPath) {
    routePath = ''
  }

  // 自动根据路径分配 billType（后端菜单返回时可覆盖此自动推断）
  const billType = menu.billType || getBillTypeForRoute(routePath)

  const route = {
    path: routePath,
    name: menu.routeName || menu.menuCode,
    meta: {
      title: menu.menuName,
      icon: menu.icon,
      keepAlive: menu.isCache === 1,
      hidden: menu.visible === 0,
      requiresAuth: true,
      ...(billType ? { billType } : {}),
      // 双入口模式：表单路径标记 autoCreate，页面通过 erp:create 事件自动打开新增弹窗
      ...(menu.displayMode === 1 ? { autoCreate: true } : {})
    }
  } as RouteRecordRaw

  if (menu.menuType === 1 && menu.component) {
    const componentPath = menu.component.replace(/^views\//, '').replace(/\.vue$/, '')
    ;(route as any).component = getComponent(componentPath)
  }

  if (menu.children && menu.children.length > 0) {
    route.children = menu.children.flatMap(child => transformMenuToRoutes(child, menu.path || parentPath))
    if (menu.menuType === 0 && !menu.redirect) {
      route.redirect = route.children[0].path || ''
    }
  }

  if (menu.redirect) {
    route.redirect = menu.redirect
  }

  const routes: RouteRecordRaw[] = [route]

  // 双入口：displayMode=1 时额外注册一条隐藏列表页路由
  if (menu.displayMode === 1 && menu.listPath) {
    let listRoutePath = menu.listPath
    if (parentPath && menu.listPath.startsWith(parentPath + '/')) {
      listRoutePath = menu.listPath.substring(parentPath.length + 1)
    } else if (menu.listPath.startsWith('/')) {
      listRoutePath = menu.listPath.substring(1)
    }

    const listBillType = billType
    // 双入口第二标签标题：指向表单组件时为「XX编辑」（对标 ql361 供应商编辑页签），
    // 指向列表组件时为「XX列表」（path=X/form + list_path=X/index 的常规单据约定）。
    const secondaryTitle = menu.listPath.includes('/form')
      ? `${menu.menuName}编辑`
      : `${menu.menuName}列表`
    const listRoute: RouteRecordRaw = {
      path: listRoutePath,
      name: `${menu.menuCode}_list`,
      component: getComponent(menu.listPath!),
      meta: {
        title: secondaryTitle,
        icon: menu.icon,
        keepAlive: menu.isCache === 1,
        hidden: true,
        requiresAuth: true,
        ...(listBillType ? { billType: listBillType } : {})
      }
    }

    routes.push(listRoute)
  }

  return routes
}

function buildMenuTree(menus: MenuItem[], parentId: number = 0): MenuItem[] {
  return menus
    .filter(menu => menu.parentId === parentId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map(menu => ({
      ...menu,
      children: buildMenuTree(menus, menu.id)
    }))
}

/**
 * 展平 display_group=1 的纯展示分组节点
 * 这些节点只用于sidebar的分组显示，不生成路由嵌套
 * 子菜单保持原有完整路径注册到父级层级
 */
function flattenDisplayGroup(menus: MenuItem[]): MenuItem[] {
  const result: MenuItem[] = []
  for (const menu of menus) {
    if (menu.displayGroup === 1 && menu.children && menu.children.length > 0) {
      // 不创建路由，提升子菜单到当前层级（递归处理嵌套的display_group）
      result.push(...flattenDisplayGroup(menu.children))
    } else {
      // 正常节点，递归处理其子节点
      const processed = { ...menu }
      if (processed.children && processed.children.length > 0) {
        processed.children = flattenDisplayGroup(processed.children)
      }
      result.push(processed)
    }
  }
  return result
}

export async function loadDynamicRoutes(router?: Router): Promise<RouteRecordRaw[]> {
  const userStore = useUserStore()
  const userId = userStore.userId
  const tenantId = userStore.tenantId || 1

  // ── 缓存策略：优先使用本地缓存，避免每次刷新都请求慢速菜单 API ──
  const cacheKey = `menu_cache_${userId}_${tenantId}_${CLIENT_TYPE}`
  const cacheExpiryKey = `menu_cache_expiry_${userId}_${tenantId}_${CLIENT_TYPE}`
  const CACHE_TTL = 30 * 60 * 1000 // 30 分钟缓存

  let menuTree: any[] | null = null

  // 尝试读取缓存
  try {
    const cachedData = localStorage.getItem(cacheKey)
    const cachedExpiry = localStorage.getItem(cacheExpiryKey)
    if (cachedData && cachedExpiry && Date.now() < Number(cachedExpiry)) {
      menuTree = JSON.parse(cachedData)
      console.info('[动态路由] 使用菜单缓存')
    }
  } catch (e) {
    console.warn('[动态路由] 读取缓存失败:', e)
  }

  // 先检查 Token 是否有效，避免因后端 Sa-Token 会话过期导致菜单接口异常
  // 增加重试机制：登录后立即check可能因Redis同步延迟返回valid=false
  try {
    let checkRes = await request.get('/auth/check', { _skipAuthRefresh: true })
    console.info('[动态路由] Token检查结果:', checkRes)

    // 如果第一次check返回valid=false，等待后重试（可能是Redis同步延迟）
    if (!checkRes?.valid) {
      console.warn('[动态路由] Token首次验证失败，等待500ms后重试...')
      await new Promise(resolve => setTimeout(resolve, 500))
      checkRes = await request.get('/auth/check', { _skipAuthRefresh: true })
      console.info('[动态路由] Token重试检查结果:', checkRes)

      if (!checkRes?.valid) {
        console.warn('[动态路由] Token验证失败（重试后仍无效），准备跳转登录页')
        // Token 失效时清除缓存
        localStorage.removeItem(cacheKey)
        localStorage.removeItem(cacheExpiryKey)
        const err: any = new Error('Token 已失效')
        err.status = 401
        throw err
      }
    }
  } catch (checkErr: any) {
    if (checkErr?.status === 401 || checkErr?.response?.status === 401) {
      throw checkErr
    }
    // 其他错误（如网络错误）如果有缓存则继续使用缓存
    if (menuTree) {
      console.warn('[动态路由] Token 检查失败，使用缓存菜单:', checkErr?.message)
    } else {
      console.warn('[动态路由] Token 检查失败，继续尝试加载菜单:', checkErr?.message)
    }
  }

  // 如果缓存无效，从后端加载菜单
  if (!menuTree) {
    try {
      const res = await request.get(`/menu/user/mega/${CLIENT_TYPE}`, { userId, tenantId })

      if (res && res.length > 0) {
        menuTree = res
        // 缓存菜单数据
        try {
          localStorage.setItem(cacheKey, JSON.stringify(menuTree))
          localStorage.setItem(cacheExpiryKey, String(Date.now() + CACHE_TTL))
          console.info('[动态路由] 菜单数据已缓存，TTL:', CACHE_TTL / 60000, '分钟')
        } catch (e) {
          console.warn('[动态路由] 缓存菜单失败:', e)
        }
      }
    } catch (error: any) {
      console.error('[动态路由] 加载菜单失败:', error)

      // 检查是否是401错误，如果是则抛出错误让路由守卫处理
      if (error?.response?.status === 401 || error?.status === 401) {
        throw error
      }

      // 菜单接口返回 500 时，二次验证 Token 是否已失效
      if (error?.response?.status === 500) {
        try {
          const checkRes = await request.get('/auth/check', { _skipAuthRefresh: true })
          if (!checkRes?.valid) {
            console.warn('[动态路由] Token 已失效（菜单接口500确认）')
            const err: any = new Error('Token 已失效')
            err.status = 401
            throw err
          }
        } catch (secondaryErr: any) {
          if (secondaryErr?.status === 401 || secondaryErr?.response?.status === 401) {
            throw secondaryErr
          }
        }
      }
    }
  }

  // ── 构建路由 ──
  let routes: RouteRecordRaw[]
  if (menuTree && menuTree.length > 0) {
    // 存储原始菜单树供sidebar渲染（包含display_group=1的分组节点）
    userStore.menus = menuTree as any

    // 注册路由时展平display_group=1的分组节点，只注册真实路由
    const flatTree = flattenDisplayGroup(menuTree)

    // 注入必须始终存在的路由（不依赖后端菜单树的隐藏详情页等）
    const requiredRoutes = getRequiredRoutes()

    const layoutRoute: RouteRecordRaw = {
      path: '/',
      name: 'Layout',
      component: () => import('@/layouts/BasicLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        ...flatTree.flatMap(menu => transformMenuToRoutes(menu)),
        ...requiredRoutes,
        {
          path: '/:pathMatch(.*)*',
          name: 'NotFound',
          component: () => import('@/views/error/404.vue'),
          meta: { title: '页面不存在', requiresAuth: false }
        }
      ]
    }

    routes = [layoutRoute]
  } else {
    routes = getFallbackRoutes()
  }

  // 如果提供了 router 实例，直接将路由注册到 router（登录组件预加载场景）
  if (router) {
    for (const route of routes) {
      if (route.name === 'Layout' && route.children) {
        const children = [...route.children]
        const layoutParent: any = {
          path: route.path,
          name: route.name,
          component: route.component,
          meta: route.meta,
        }
        router.addRoute(layoutParent)
        for (const child of children) {
          router.addRoute('Layout', stripAllRedirects(child))
        }
      } else {
        router.addRoute(stripAllRedirects(route))
      }
    }
    console.info('[动态路由] 路由已注册到 router，共', routes.length, '条顶层路由')
  }

  return routes
}

/**
 * 必须始终存在的隐藏路由（详情页、编辑页等，不依赖后端菜单树）
 * 无论后端菜单 API 返回什么数据，这些路由都会注入到 Layout.children 中
 */
function getRequiredRoutes(): RouteRecordRaw[] {
  return [
    {
      path: 'erp/product/create',
      name: 'ErpProductCreate',
      component: () => import('@/views/erp/product/form.vue'),
      meta: { title: '新增商品', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/form/:id',
      name: 'ErpProductFormEdit',
      component: () => import('@/views/erp/product/form.vue'),
      meta: { title: '编辑商品', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/price-batch',
      name: 'ErpProductPriceBatch',
      component: () => import('@/views/erp/product/price-batch.vue'),
      meta: { title: '批量价格管理', icon: 'DollarOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/inventory-mode',
      name: 'ErpProductInventoryMode',
      component: () => import('@/views/erp/product/inventory-mode.vue'),
      meta: { title: '库存管理模式', icon: 'SettingOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/product/grade',
      name: 'ErpProductGrade',
      component: () => import('@/views/erp/product/grade.vue'),
      meta: { title: '价格等级管理', icon: 'CrownOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/purchase/form',
      name: 'ErpPurchaseFormCreate',
      component: () => import('@/views/erp/purchase/form.vue'),
      meta: { title: '采购订单', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'purchase/order/:id',
      name: 'PurchaseOrderDetail',
      component: () => import('@/views/erp/purchase/form.vue'),
      meta: { title: '采购订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'purchase/inquiry/:id',
      name: 'PurchaseInquiryDetail',
      component: () => import('@/views/purchase/detail/inquiry/InquiryDetail.vue'),
      meta: { title: '询价详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'purchase/inbound/:id',
      name: 'PurchaseInboundDetail',
      component: () => import('@/views/purchase/detail/inbound/InboundDetail.vue'),
      meta: { title: '入库详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'purchase/return/:id',
      name: 'PurchaseReturnDetail',
      component: () => import('@/views/purchase/return/form.vue'),
      meta: { title: '采购退货单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'sale/order/:id',
      name: 'SaleOrderDetail',
      component: () => import('@/views/erp/sale/form.vue'),
      meta: { title: '销售订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
    },
    {
      path: 'crm/customer/:id',
      name: 'CrmCustomerDetail',
      component: () => import('@/views/crm/customer/form.vue'),
      meta: { title: '客户详情', icon: 'UserOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'stock/detail/:id',
      name: 'StockDetail',
      component: () => import('@/views/erp/stock/index.vue'),
      meta: { title: '库存详情', icon: 'ContainerOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'erp/partner',
      name: 'ErpPartner',
      component: () => import('@/views/erp/partner/index.vue'),
      meta: { title: '往来单位管理', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/partner/:id',
      name: 'ErpPartnerDetail',
      component: () => import('@/views/erp/partner/detail.vue'),
      meta: { title: '单位详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/partner/create',
      name: 'ErpPartnerCreate',
      component: () => import('@/views/erp/partner/detail.vue'),
      meta: { title: '新增单位', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/stock-in/:id',
      name: 'ErpStockInDetail',
      component: () => import('@/views/erp/stock-in/form.vue'),
      meta: { title: '入库单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/shipment/:id',
      name: 'ErpShipmentDetail',
      component: () => import('@/views/erp/shipment/form.vue'),
      meta: { title: '出库单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/stocktake/:id',
      name: 'ErpStocktakeDetail',
      component: () => import('@/views/erp/stocktake/form.vue'),
      meta: { title: '盘点单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/return/:id',
      name: 'ErpReturnDetail',
      component: () => import('@/views/erp/return/form.vue'),
      meta: { title: '退货单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/batch',
      name: 'ErpBatch',
      component: () => import('@/views/erp/batch/index.vue'),
      meta: { title: '批次管理', icon: 'BarcodeOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/serial',
      name: 'ErpSerial',
      component: () => import('@/views/erp/serial/index.vue'),
      meta: { title: '序列号管理', icon: 'NumberOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/serial/:id',
      name: 'ErpSerialDetail',
      component: () => import('@/views/erp/serial/detail.vue'),
      meta: { title: '序列号追溯', icon: 'SearchOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'erp/column-config/sale-order-item',
      name: 'ErpSaleOrderItemColumnConfig',
      component: () => import('@/views/erp/column-config/SaleOrderItemColumnConfig.vue'),
      meta: { title: '销售订单明细列配置', icon: 'SettingOutlined', keepAlive: false, requiresAuth: true, hidden: false }
    },
    {
      path: 'supplier/detail/:id',
      name: 'SupplierDetail',
      component: () => import('@/views/supplier/detail.vue'),
      meta: { title: '供应商详情', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/create',
      name: 'SupplierCreate',
      component: () => import('@/views/supplier/create.vue'),
      meta: { title: '新增供应商', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'supplier/edit/:id',
      name: 'SupplierEdit',
      component: () => import('@/views/supplier/edit.vue'),
      meta: { title: '编辑供应商', icon: 'TeamOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // supplier/inquiry 和 supplier/performance 已通过动态菜单路由注册，此处不再重复
    {
      path: 'workflow/instance-monitor',
      name: 'WorkflowInstanceMonitor',
      component: () => import('@/views/workflow/instance-monitor.vue'),
      meta: { title: '流程监控', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'workflow/task-management',
      name: 'WorkflowTaskManagement',
      component: () => import('@/views/workflow/task-management.vue'),
      meta: { title: '任务管理', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'workflow/process-analysis',
      name: 'WorkflowProcessAnalysis',
      component: () => import('@/views/workflow/process-analysis.vue'),
      meta: { title: '流程分析', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'notification/index',
      name: 'Notification',
      component: () => import('@/views/notification/index.vue'),
      meta: { title: '通知公告', icon: 'BellOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'profile/index',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { title: '个人中心', icon: 'UserOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'charts/index',
      name: 'Charts',
      component: () => import('@/views/charts/index.vue'),
      meta: { title: '图表', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'system/data-import',
      name: 'DataImport',
      component: () => import('@/views/system/data-import/index.vue'),
      meta: { title: '数据导入', icon: 'ImportOutlined', keepAlive: true, requiresAuth: true }
    },

    // ── 商城管理模块 ──
    {
      path: 'mall/config',
      name: 'MallConfig',
      component: () => import('@/views/erp/mall/config/index.vue'),
      meta: { title: '商城配置', icon: 'SettingOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'mall/user-audit',
      name: 'MallUserAudit',
      component: () => import('@/views/erp/mall/user-audit/index.vue'),
      meta: { title: '用户审核', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'mall/banner',
      name: 'MallBanner',
      component: () => import('@/views/erp/mall/banner/index.vue'),
      meta: { title: '轮播图管理', icon: 'PictureOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'mall/order',
      name: 'MallOrder',
      component: () => import('@/views/erp/mall/order/index.vue'),
      meta: { title: '订单管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    {
      path: 'mall/product',
      name: 'MallProduct',
      component: () => import('@/views/erp/mall/product/index.vue'),
      meta: { title: '商品管理', icon: 'AppstoreOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },

    // ── 必须存在的业务页面（确保即使后端菜单未配置也能访问）──
    {
      path: 'stock',
      name: 'Stock',
      component: () => import('@/views/erp/stock/index.vue'),
      meta: { title: '销售出库', icon: 'ExportOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'finance',
      name: 'Finance',
      component: () => import('@/views/finance/index.vue'),
      meta: { title: '财务管理首页', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
    },

    // ── ERP 核心业务页面（测试覆盖率所需，始终注册避免 404）──
    {
      path: 'erp/pricing/approval',
      name: 'ErpPricingApproval',
      component: () => import('@/views/erp/pricing/approval/index.vue'),
      meta: { title: '价格审批', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'erp/pricing',
      name: 'ErpPricing',
      component: () => import('@/views/erp/pricing/index.vue'),
      meta: { title: '价格管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'erp/purchase',
      name: 'ErpPurchase',
      component: () => import('@/views/erp/purchase/index.vue'),
      meta: { title: '采购管理', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, billType: '504' }
    },
    {
      path: 'erp/purchase-contract',
      name: 'ErpPurchaseContract',
      component: () => import('@/views/erp/purchase-contract/index.vue'),
      meta: { title: '采购合同', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true, hidden: true, billType: '504' }
    },
    {
      path: 'erp/purchase/inquiry',
      name: 'ErpPurchaseInquiry',
      component: () => import('@/views/erp/purchase/index.vue'),
      meta: { title: '采购询价', icon: 'SearchOutlined', keepAlive: true, requiresAuth: true, billType: '504' }
    },
    {
      path: 'erp/sales-analysis',
      name: 'ErpSalesAnalysis',
      component: () => import('@/views/erp/sales-analysis/index.vue'),
      meta: { title: '销售分析', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true, billType: '604' }
    },
    {
      path: 'erp/sales-report',
      name: 'ErpSalesReport',
      component: () => import('@/views/erp/sales-report/index.vue'),
      meta: { title: '销售报表', icon: 'LineChartOutlined', keepAlive: true, requiresAuth: true, billType: '604' }
    },
    {
      path: 'erp/stock',
      name: 'ErpStock',
      component: () => import('@/views/erp/stock/index.vue'),
      meta: { title: '库存管理', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'erp/stock-in',
      name: 'ErpStockIn',
      component: () => import('@/views/erp/stock-in/index.vue'),
      meta: { title: '入库管理', icon: 'InboxOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'erp/stocktake',
      name: 'ErpStocktake',
      component: () => import('@/views/erp/stocktake/index.vue'),
      meta: { title: '库存盘点', icon: 'CheckSquareOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'erp/stock/replenishment',
      name: 'ErpStockReplenishment',
      component: () => import('@/views/erp/stock/replenishment/index.vue'),
      meta: { title: '智能补货', icon: 'RocketOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'erp/batch',
      name: 'ErpBatchMain',
      component: () => import('@/views/erp/batch/index.vue'),
      meta: { title: '批次管理', icon: 'BarcodeOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'erp/serial',
      name: 'ErpSerialMain',
      component: () => import('@/views/erp/serial/index.vue'),
      meta: { title: '序列号管理', icon: 'NumberOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },
    {
      path: 'erp/return',
      name: 'ErpReturnMain',
      component: () => import('@/views/erp/return/index.vue'),
      meta: { title: '退货管理', icon: 'RollbackOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
    },

    // ── 脏路由修复：存在组件但无菜单覆盖的兜底路由 ──
    {
      path: 'erp/stock-bom',
      name: 'ErpStockBom',
      component: () => import('@/views/erp/stock-bom/index.vue'),
      meta: { title: '生产模板', icon: 'DeploymentUnitOutlined', keepAlive: true, requiresAuth: true, hidden: true, billType: '601' }
    },
    // 生产模板新增表单路由由数据库 displayMode=1 动态注册，此处仅保留 :id 编辑路由
    {
      path: 'erp/stock-bom/form/:id',
      name: 'StockBomFormEdit',
      component: () => import('@/views/erp/stock-bom/form.vue'),
      meta: { title: '编辑生产模板', icon: 'DeploymentUnitOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // ═══ 销售出库单/退货申请/零售单 的 form 路由已迁移到数据库动态管理（display_mode=1）═══
    // 零售单列表页：菜单 list_path=sales/retail/index，文档约定地址为 sales/retail（两者均指向列表组件）
    {
      path: 'sales/retail',
      name: 'RetailList',
      component: () => import('@/views/sales/retail/index.vue'),
      meta: { title: '零售单', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, hidden: true }
    },
    // 零售单新增（文档约定 sales/retail/create 与 sales/retail/form 均可进入新增模式）
    {
      path: 'sales/retail/create',
      name: 'RetailFormCreate',
      component: () => import('@/views/sales/retail/form.vue'),
      meta: { title: '新增零售单', icon: 'ShoppingCartOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // 仅保留 :id 详情页路由（编辑模式，由列表页跳转，无对应菜单项）
    {
      path: 'sales/retail/form/:id',
      name: 'RetailFormDetail',
      component: () => import('@/views/sales/retail/form.vue'),
      meta: { title: '零售单详情', icon: 'ShoppingCartOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'sales/outbound/form/:id',
      name: 'SalesOutboundFormEdit',
      component: () => import('@/views/sales/outbound/form.vue'),
      meta: { title: '编辑销售出库单', icon: 'SendOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'sales/return-apply/form/:id',
      name: 'SaleReturnApplyFormEdit',
      component: () => import('@/views/sales/return-apply/form.vue'),
      meta: { title: '编辑销售退货申请', icon: 'RollbackOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    // ═══ 商城用户审核表单 ═══
    {
      path: 'mall/user-audit/form',
      name: 'MallUserAuditForm',
      component: () => import('@/views/erp/mall/user-audit/form.vue'),
      meta: { title: '用户审核', icon: 'UserOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'mall/user-audit/form/:id',
      name: 'MallUserAuditFormDetail',
      component: () => import('@/views/erp/mall/user-audit/form.vue'),
      meta: { title: '用户审核详情', icon: 'UserOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // ═══ 资料 > 物流公司编辑路由 ═══
    // 列表路由由菜单 displayMode=1 动态注册（path=md/logistics/index，list_path=md/logistics/form），
    // 编辑（带 id）不在菜单树内，此处补隐藏路由，避免列表「修改」跳转 404。
    {
      path: 'md/logistics/form/:id',
      name: 'MdLogisticsFormEdit',
      component: () => import('@/views/md/logistics/form.vue'),
      meta: { title: '编辑物流公司', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // ═══ 预订货单列表别名路由 ═══
    // 菜单 displayMode=1 注册的列表路由为 list_path（sales/pre-order/index）；
    // 文档/历史链接与表单保存后的跳转都使用 /sales/pre-order，此处补别名，避免 404。
    {
      path: 'sales/pre-order',
      name: 'PreOrderListAlias',
      component: () => import('@/views/sales/pre-order/index.vue'),
      meta: { title: '预订货单', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
    },
    // ═══ 预订货单编辑路由（列表路由由菜单displayMode=1动态注册，新增通过/form无参访问） ═══
    {
      path: 'sales/pre-order/form',
      name: 'PreOrderForm',
      component: () => import('@/views/sales/pre-order/form.vue'),
      meta: { title: '新增预订货单', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
    },
    {
      path: 'sales/pre-order/form/:id',
      name: 'PreOrderFormEdit',
      component: () => import('@/views/sales/pre-order/form.vue'),
      meta: { title: '编辑预订货单', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
    },
    {
      path: 'sales/pre-order/create',
      name: 'PreOrderCreate',
      component: () => import('@/views/sales/pre-order/form.vue'),
      meta: { title: '新增预订货单', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
    },
    {
      path: 'erp/stock-assemble/form',
      name: 'StockAssembleForm',
      component: () => import('@/views/erp/stock-assemble/form.vue'),
      meta: { title: '新增组装单', icon: 'BuildOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'erp/stock-assemble/form/:id',
      name: 'StockAssembleFormEdit',
      component: () => import('@/views/erp/stock-assemble/form.vue'),
      meta: { title: '编辑组装单', icon: 'BuildOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'erp/stock-split/form',
      name: 'StockSplitForm',
      component: () => import('@/views/erp/stock-split/form.vue'),
      meta: { title: '新增拆分单', icon: 'ScissorOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'erp/stock-split/form/:id',
      name: 'StockSplitFormEdit',
      component: () => import('@/views/erp/stock-split/form.vue'),
      meta: { title: '编辑拆分单', icon: 'ScissorOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
  ]
}

/**
 * 应急回退路由（仅后端菜单API完全不可用时使用）
 * 注意：种子数据部署后，正常情况下API总会返回菜单数据，
 * 此回退仅在API服务完全宕机时作为兜底
 */
function getFallbackRoutes(): RouteRecordRaw[] {
  return [
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/layouts/BasicLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        // ── 工作台 ──
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台', icon: 'DashboardOutlined', keepAlive: true, requiresAuth: true } },
        // ─ 销售作业 ──
        { path: 'sale', name: 'Sale', component: () => import('@/views/erp/sale/index.vue'), meta: { title: '销售订单', icon: 'ShoppingOutlined', keepAlive: true, requiresAuth: true, billType: '604' } },
        { path: 'erp/sale', name: 'ErpSale', component: () => import('@/views/erp/sale/index.vue'), meta: { title: '销售管理(ERP)', icon: 'ShoppingOutlined', keepAlive: true, requiresAuth: true, billType: '604', hidden: true } },
        { path: 'stock', name: 'Stock', component: () => import('@/views/erp/stock/index.vue'), meta: { title: '销售出库', icon: 'ExportOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        { path: 'erp/shipment', name: 'ErpShipment', component: () => import('@/views/erp/shipment/index.vue'), meta: { title: '发货管理', icon: 'SendOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        { path: 'erp/return', name: 'ErpReturn', component: () => import('@/views/erp/return/index.vue'), meta: { title: '退货管理', icon: 'RollbackOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        { path: 'erp/sales-analysis', name: 'ErpSalesAnalysis', component: () => import('@/views/erp/sales-analysis/index.vue'), meta: { title: '销售分析', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true, billType: '604' } },
        { path: 'erp/sales-report', name: 'ErpSalesReport', component: () => import('@/views/erp/sales-report/index.vue'), meta: { title: '销售报表', icon: 'LineChartOutlined', keepAlive: true, requiresAuth: true, billType: '604' } },
        // ── 采购作业 ──
        { path: 'purchase', name: 'Purchase', component: () => import('@/views/erp/purchase/index.vue'), meta: { title: '采购订单', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, billType: '504' } },
        { path: 'erp/purchase', name: 'ErpPurchase', component: () => import('@/views/erp/purchase/index.vue'), meta: { title: '采购管理(ERP)', icon: 'ShoppingCartOutlined', keepAlive: true, requiresAuth: true, billType: '504', hidden: true } },
        { path: 'erp/stock-in', name: 'ErpStockIn', component: () => import('@/views/erp/stock-in/index.vue'), meta: { title: '入库管理', icon: 'InboxOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        // ── 仓储作业 ──
        { path: 'erp/stock', name: 'ErpStock', component: () => import('@/views/erp/stock/index.vue'), meta: { title: '库存管理(ERP)', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        { path: 'erp/stocktake', name: 'ErpStocktake', component: () => import('@/views/erp/stocktake/index.vue'), meta: { title: '库存盘点', icon: 'CheckSquareOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        { path: 'erp/batch', name: 'ErpBatch', component: () => import('@/views/erp/batch/index.vue'), meta: { title: '批次管理', icon: 'BarcodeOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        { path: 'erp/serial', name: 'ErpSerial', component: () => import('@/views/erp/serial/index.vue'), meta: { title: '序列号管理', icon: 'NumberOutlined', keepAlive: true, requiresAuth: true, billType: '601' } },
        // ── 客户关系 ──
        { path: 'crm/customer', name: 'CrmCustomer', component: () => import('@/views/crm/customer/index.vue'), meta: { title: '客户管理', icon: 'TeamOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'crm/lead', name: 'CrmLead', component: () => import('@/views/crm/lead/index.vue'), meta: { title: '线索管理', icon: 'HighlightOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'crm/opportunity', name: 'CrmOpportunity', component: () => import('@/views/crm/opportunity/index.vue'), meta: { title: '商机管理', icon: 'BulbOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'crm/quotation', name: 'CrmQuotation', component: () => import('@/views/crm/quotation/index.vue'), meta: { title: '报价管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'crm/contract', name: 'CrmContract', component: () => import('@/views/crm/contract/index.vue'), meta: { title: '合同管理', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'crm/invoice', name: 'CrmInvoice', component: () => import('@/views/crm/invoice/index.vue'), meta: { title: '发票管理', icon: 'FileProtectOutlined', keepAlive: true, requiresAuth: true } },
        // ── 财务管理 ──
        { path: 'finance', name: 'Finance', component: () => import('@/views/finance/index.vue'), meta: { title: '财务管理', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true } },
        // ── 费用管理 ──
        { path: 'erp/expense/application', name: 'ExpenseApplication', component: () => import('@/views/erp/expense/application/index.vue'), meta: { title: '费用申请', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'erp/expense/reimbursement', name: 'ExpenseReimbursement', component: () => import('@/views/erp/expense/reimbursement/index.vue'), meta: { title: '费用报销', icon: 'DollarOutlined', keepAlive: true, requiresAuth: true } },
        // ── 资产管理 ──
        { path: 'fixed-asset/asset', name: 'FixedAssetList', component: () => import('@/views/fixed-asset/asset/index.vue'), meta: { title: '资产列表', icon: 'BankOutlined', keepAlive: true, requiresAuth: true } },
        // ── 预算管理 ──
        { path: 'budget', name: 'Budget', component: () => import('@/views/budget/index.vue'), meta: { title: '预算管理', icon: 'FundOutlined', keepAlive: true, requiresAuth: true } },
        // ── 商城管理 ──
        { path: 'mall/config', name: 'MallConfig', component: () => import('@/views/erp/mall/config/index.vue'), meta: { title: '商城配置', icon: 'SettingOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'mall/banner', name: 'MallBanner', component: () => import('@/views/erp/mall/banner/index.vue'), meta: { title: '轮播图管理', icon: 'PictureOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'mall/product', name: 'MallProduct', component: () => import('@/views/erp/mall/product/index.vue'), meta: { title: '商品管理', icon: 'AppstoreOutlined', keepAlive: true, requiresAuth: true } },
        // ── 产品数据 ──
        { path: 'erp/product', name: 'ErpProduct', component: () => import('@/views/erp/product/index.vue'), meta: { title: '产品管理', icon: 'AppstoreOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'erp/partner', name: 'ErpPartner', component: () => import('@/views/erp/partner/index.vue'), meta: { title: '往来单位管理', icon: 'TeamOutlined', keepAlive: true, requiresAuth: true } },
        // ── 打印管理 ──
        { path: 'printing/template', name: 'PrintTemplate', component: () => import('@/views/printing/template/index.vue'), meta: { title: '打印模板', icon: 'FileTextOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'printing/chain', name: 'PrintChain', component: () => import('@/views/printing/chain/index.vue'), meta: { title: '打印链路', icon: 'LinkOutlined', keepAlive: true, requiresAuth: true } },
        // ── 系统管理 ──
        { path: 'system/user', name: 'SystemUser', component: () => import('@/views/system/user/index.vue'), meta: { title: '用户管理', icon: 'UserOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'system/role', name: 'SystemRole', component: () => import('@/views/system/role/index.vue'), meta: { title: '角色管理', icon: 'SafetyOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'system/menu', name: 'SystemMenu', component: () => import('@/views/system/menu/index.vue'), meta: { title: '菜单管理', icon: 'MenuOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'system/config', name: 'SystemConfig', component: () => import('@/views/system/config/index.vue'), meta: { title: '系统配置', icon: 'SettingOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'charts/index', name: 'Charts', component: () => import('@/views/charts/index.vue'), meta: { title: '图表', icon: 'BarChartOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'notification/index', name: 'Notification', component: () => import('@/views/notification/index.vue'), meta: { title: '通知公告', icon: 'BellOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'profile/index', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { title: '个人中心', icon: 'UserOutlined', keepAlive: true, requiresAuth: true } },
      ]
    }
  ]
}

const MODULE_ROUTE_MAP: Record<string, string[]> = {
  'sale': ['sale', 'erp/sale', 'erp/sales-analysis', 'erp/sales-report', 'sales/outbound', 'sales/pre-order'],
  'purchase': ['purchase', 'erp/purchase', 'purchase/exchange', 'purchase/return'],
  'warehouse': ['stock', 'erp/stock', 'erp/stock-in', 'erp/stocktake', 'erp/return', 'erp/shipment', 'erp/batch', 'erp/serial'],
  'finance': ['finance', 'erp/finance'],
  'customer': ['crm/customer', 'crm/contract', 'crm/lead', 'crm/opportunity', 'crm/quotation', 'crm/invoice'],
  'supplier': ['supplier'],
  'report': ['charts', 'erp/dashboard'],
  'system': ['system'],
  'printing': ['printing'],
  'notification': ['notification'],
  'wms': ['wms'],
  'mall': ['mall'],
  'hr': ['hr'],
  'trade': ['trade'],
  'quality': ['quality'],
  'payment': ['payment'],
}

/**
 * 根据路由路径推断所属模块编码
 */
function getRouteModule(routePath: string): string | undefined {
  const normalized = routePath.replace(/^\/+|\/+$/g, '').replace(/\/:\w+\??/g, '')
  for (const [moduleCode, paths] of Object.entries(MODULE_ROUTE_MAP)) {
    for (const p of paths) {
      if (normalized === p || normalized.startsWith(p + '/')) {
        return moduleCode
      }
    }
  }
  // 取路径首段作为模块
  const firstSegment = normalized.split('/')[0]
  return firstSegment || undefined
}

export function checkRouteAccess(route: any): boolean {
  const userStore = useUserStore()
  const permissions = route.meta?.permissions as string[] | undefined
  const billType = route.meta?.billType as string | undefined

  const userPermissions = userStore.permissions || []
  if (userPermissions.includes('*')) {
    return true
  }

  // 检查权限码
  if (permissions && permissions.length > 0) {
    if (!permissions.some(permission => userPermissions.includes(permission))) {
      return false
    }
  }

  // 检查单据类型权限
  if (billType) {
    const userBillTypes = new Set(userStore.billTypes || [])
    if (!userBillTypes.has(billType)) {
      return false
    }
  }

  // 租户模块有效性校验（双重检查：角色权限 + 模块有效性）
  const routePath = route.path?.replace(/^\//, '') || ''
  if (routePath && route.meta?.requiresAuth !== false) {
    const routeModule = getRouteModule(routePath)
    if (routeModule && !userStore.hasValidModule(routeModule)) {
      return false
    }
  }

  return true
}

export function filterRoutesByPermission(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  return routes.filter(route => {
    if (!checkRouteAccess(route)) {
      return false
    }
    
    if (route.children) {
      route.children = filterRoutesByPermission(route.children)
    }
    
    return true
  })
}