import { RouterView } from 'vue-router'
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
  // 协议模块（菜单由后端迁移落库，component 写 views/agreement/index）
  'agreement/index': () => import('@/views/agreement/index.vue'),
  'agreement/detail': () => import('@/views/agreement/detail.vue'),
  'agreement/term-option/index': () => import('@/views/agreement/term-option/index.vue'),
  'agreement/term-option': () => import('@/views/agreement/term-option/index.vue'),
  // 发起契约向导（隐藏路由，入口在协议列表工具栏「发起契约」）
  'agreement/wizard/index': () => import('@/views/agreement/wizard/index.vue'),
  'agreement/wizard': () => import('@/views/agreement/wizard/index.vue'),
  // 契约模板管理（隐藏路由，入口在协议列表工具栏「契约模板」）
  'agreement/template/index': () => import('@/views/agreement/template/index.vue'),
  'agreement/template': () => import('@/views/agreement/template/index.vue'),
  // 受邀方打开契约邀请（短链由后端唯一送达下发：/agreement/invite?token=xxx）
  'agreement/invite/index': () => import('@/views/agreement/invite/index.vue'),
  'agreement/invite': () => import('@/views/agreement/invite/index.vue'),
  'erp/product/index': () => import('@/views/erp/product/index.vue'),
  'erp/product/create': () => import('@/views/erp/product/form.vue'),
  'erp/product/form': () => import('@/views/erp/product/form.vue'),
  'erp/product/inventory-mode': () => import('@/views/erp/product/inventory-mode.vue'),
  'erp/product/grade': () => import('@/views/erp/product/grade.vue'),
  'erp/column-config/sale-order-item': () => import('@/views/erp/column-config/SaleOrderItemColumnConfig.vue'),
  'partner/index': () => import('@/views/erp/partner/index.vue'),
  'partner/detail': () => import('@/views/erp/partner/detail.vue'),
  'erp/partner/index': () => import('@/views/erp/partner/index.vue'),
  'erp/partner/detail': () => import('@/views/erp/partner/detail.vue'),
  'crm/contract/index': () => import('@/views/crm/contract/index.vue'),
  'crm/customer/index': () => import('@/views/crm/customer/index.vue'),
  'finance/invoice/index': () => import('@/views/finance/invoice/index.vue'),
  'crm/lead/index': () => import('@/views/crm/lead/index.vue'),
  'crm/opportunity/index': () => import('@/views/crm/opportunity/index.vue'),
  'crm/quotation/index': () => import('@/views/crm/quotation/index.vue'),
  // 'crm/supplier/index' 的映射已移除：V11.422.0 同期按《CRM模块/README.md》P2 裁定
  // 「与 ERP 供应商重复实现 → 删除页面与路由」处理，该页（1506 行，走旧 supplierApi）
  // 已删除。在用实现是菜单 80511「供应商」→ views/md/supplier/index.vue（走 partnerApi）。
  // 注意 supplierApi（/api/supplier/*）本身仍有 9 个其它页面在用，接口未删。
  'dashboard/index': () => import('@/views/dashboard/index.vue'),
  'finance/index': () => import('@/views/finance/index.vue'),
  'finance/reconciliation/index': () => import('@/views/finance/reconciliation/index.vue'),
  'finance/voucher/index': () => import('@/views/finance/voucher/index.vue'),
  'finance/voucher/VoucherDetail': () => import('@/views/finance/voucher/VoucherDetail.vue'),
  'finance/receivable/index': () => import('@/views/finance/receivable/index.vue'),
  'finance/payable/index': () => import('@/views/finance/payable/index.vue'),
  'finance/pre-payment/index': () => import('@/views/finance/pre-payment/index.vue'),
  'finance/deposit/index': () => import('@/views/finance/deposit/index.vue'),
  'finance/write-off/index': () => import('@/views/finance/write-off/index.vue'),
  'finance/offset/index': () => import('@/views/finance/offset/index.vue'),
  'finance/receipt/index': () => import('@/views/finance/receipt/index.vue'),
  'finance/payment/index': () => import('@/views/finance/payment/index.vue'),
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
  'system/dict/index': () => import('@/views/system/dict/index.vue'),
  'system/log/index': () => import('@/views/system/log/index.vue'),
  'system/menu/index': () => import('@/views/system/menu/index.vue'),
  // 'system/permission/index' —— 权限配置页，2026-09-19 已挂菜单 6130704（系统 → 系统管理）
  'system/permission/index': () => import('@/views/system/permission/index.vue'),
  'system/position/index': () => import('@/views/system/position/index.vue'),
  'system/role/index': () => import('@/views/system/role/index.vue'),
  'system/user/index': () => import('@/views/system/user/index.vue'),
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
  'erp/sales-report/index': () => import('@/views/erp/sales-report/index.vue'),
  'erp/stock-in/index': () => import('@/views/erp/stock-in/index.vue'),
  'erp/stock-out/index': () => import('@/views/erp/stock-out/index.vue'),
  'erp/stocktake/index': () => import('@/views/erp/stocktake/index.vue'),
  'erp/shipment/index': () => import('@/views/erp/shipment/index.vue'),
  // ── 价格引擎模块 ──
  // 'erp/pricing/index' 的映射已移除：该页（1704 行）与菜单 70503「商品价格管理」
  // （views/md/product-price，1728 行、12+ 个 API）功能重叠，且它所谓的独有功能
  // （对比等级/季节性调价/变更记录）依赖 /erp/pricing/partner-grade-prices/* ——
  // 该接口后端**不存在**（只有 /api/erp/product-grade-price）⇒ 调用必 404；
  // 其 v-permission 码 pricing:compare/seasonal/history 在 sys_permission 中也不存在
  // ⇒ 按钮对非超管直接隐藏。既是坏功能又无入口，故直接清理，页面文件已删除。
  'erp/pricing/approval/index': () => import('@/views/erp/pricing/approval/index.vue'),
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
  'erp/stock-alert-config/index': () => import('@/views/erp/stock-alert-config/index.vue'),
  'erp/stock-bom/index': () => import('@/views/erp/stock-bom/index.vue'),
  'erp/stock-bom/form': () => import('@/views/erp/stock-bom/form.vue'),
  'erp/stock-out/form': () => import('@/views/erp/stock-out/form.vue'),
  'erp/stock-assemble/index': () => import('@/views/erp/stock-assemble/index.vue'),
  'erp/stock-assemble/form': () => import('@/views/erp/stock-assemble/form.vue'),
  'erp/stock-split/index': () => import('@/views/erp/stock-split/index.vue'),
  'erp/stock-split/form': () => import('@/views/erp/stock-split/form.vue'),

  // ── WMS 仓储管理模块 ──
  'wms/putaway/index': () => import('@/views/wh/putaway-order/index.vue'),
  'wms/pick/index': () => import('@/views/wh/picking-order/index.vue'),
  'wms/wave/index': () => import('@/views/wms/wave/index.vue'),
  'wms/ship/index': () => import('@/views/wh/shipping-order/index.vue'),
  'wms/move/index': () => import('@/views/wh/move-order/index.vue'),
  'wms/event/index': () => import('@/views/wms/event/index.vue'),

  // ── 商城管理模块 ──
  'erp/mall/product/index': () => import('@/views/erp/mall/product/index.vue'),
  // 商城顶级路由
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

  'budget/index': () => import('@/views/budget/index.vue'),
  'budget/template/index': () => import('@/views/budget/template/index.vue'),
  'budget/adjustment/index': () => import('@/views/budget/adjustment/index.vue'),
  'budget/report/index': () => import('@/views/budget/report/index.vue'),

  // ── DMS 配送管理模块 ──
  'dms/dashboard/index': () => import('@/views/dms/dashboard/index.vue'),
  'dms/channel/index': () => import('@/views/dms/channel/index.vue'),
  'dms/rider/index': () => import('@/views/dms/rider/index.vue'),
  'dms/vehicle/index': () => import('@/views/dms/vehicle/index.vue'),
  'dms/vehicle/maintenance': () => import('@/views/dms/vehicle/maintenance.vue'),
  'dms/verification/index': () => import('@/views/dms/verification/index.vue'),
  'dms/vehicle/usage/index': () => import('@/views/dms/vehicle/usage/index.vue'),
  'dms/route/index': () => import('@/views/dms/route/index.vue'),
  'dms/dispatch/index': () => import('@/views/dms/dispatch/index.vue'),
  'dms/order-pool/index': () => import('@/views/dms/order-pool/index.vue'),
  'dms/order-pool/bid-detail': () => import('@/views/dms/order-pool/bid-detail.vue'),
  'dms/config/index': () => import('@/views/dms/config/index.vue'),
  'dms/tracking/index': () => import('@/views/dms/tracking/index.vue'),
  'dms/sign/index': () => import('@/views/dms/sign/index.vue'),
  'dms/settlement/index': () => import('@/views/dms/settlement/index.vue'),
  'dms/payment/index': () => import('@/views/dms/payment/index.vue'),

  // ── 系统级新增路由 ──
  'admin/tenant/approval': () => import('@/views/admin/tenant/approval/index.vue'),
  // ⚠️ 别名：菜单 62204「系统日志」的 component 是 `views/admin/monitor/log`（见 V6.15.0），
  //    归一化后命中本键，因此该菜单的**实际实现是 `views/system/log/index.vue`**。
  //    原同名文件 `views/admin/monitor/log/index.vue`（167 行，只调不存在的 /api/audit-log/page）
  //    因本别名而永不可达，已于 2026-09-19 删除。新增「系统日志」相关逻辑请改 system/log/index.vue。
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

  // ── displayMode=1 表单页路由（Phase 1 ERP 核心单据） ──
  'erp/shipment/form': () => import('@/views/erp/shipment/form.vue'),
  'erp/return/form': () => import('@/views/erp/return/form.vue'),
  'erp/stock-in/form': () => import('@/views/erp/stock-in/form.vue'),
  'erp/stocktake/form': () => import('@/views/erp/stocktake/form.vue'),

  // ── displayMode=1 表单页路由（Phase 2 WMS 仓储执行） ──
  'wms/putaway/form': () => import('@/views/wh/putaway-order/form/index.vue'),
  'wms/pick/form': () => import('@/views/wh/picking-order/form/index.vue'),
  'wms/ship/form': () => import('@/views/wh/shipping-order/form/index.vue'),
  'wms/move/form': () => import('@/views/wh/move-order/form/index.vue'),

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
  // 旧 erp/return/index.vue 已于 2026-09-19 删除，本别名键被菜单 list_path=sales/return 使用，
  // 改指《CLEANUP_SCOPE_20260919.md》记录的取代页 sales/return-doc（70022）。
  'sales/return': () => import('@/views/sales/return-doc/index.vue'),
  'sales/shipment': () => import('@/views/erp/shipment/index.vue'),
  'wms/putaway': () => import('@/views/wh/putaway-order/index.vue'),
  'wms/pick': () => import('@/views/wh/picking-order/index.vue'),
  'wms/ship': () => import('@/views/wh/shipping-order/index.vue'),
  'wms/move': () => import('@/views/wh/move-order/index.vue'),
  'crm/quotation': () => import('@/views/crm/quotation/index.vue'),
  'finance/invoice': () => import('@/views/finance/invoice/index.vue'),
  'crm/customer': () => import('@/views/crm/customer/index.vue'),
  'crm/lead': () => import('@/views/crm/lead/index.vue'),
  'crm/opportunity': () => import('@/views/crm/opportunity/index.vue'),
  'crm/contract': () => import('@/views/crm/contract/index.vue'),
  'dms/rider': () => import('@/views/dms/rider/index.vue'),
  'dms/vehicle': () => import('@/views/dms/vehicle/index.vue'),
  'dms/sign': () => import('@/views/dms/sign/index.vue'),
  'dms/settlement': () => import('@/views/dms/settlement/index.vue'),
  'dms/payment': () => import('@/views/dms/payment/index.vue'),
  // 旧 erp/return/index.vue 已删除，本别名键改指取代页 sales/return-doc（70022）。
  'erp/return': () => import('@/views/sales/return-doc/index.vue'),
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
  // 采购询价：此前只挂了静态路由 erp/purchase/inquiry，从未登记进 componentMap，
  // 所以新加的菜单（path=purchase/inquiry）能注册路由却解析不到组件，页面渲染成
  // 「页面组件未找到: purchase/inquiry/index」。
  'purchase/inquiry': () => import('@/views/purchase/inquiry/index.vue'),
  'purchase/inquiry/index': () => import('@/views/purchase/inquiry/index.vue'),
  'purchase/inquiry/form': () => import('@/views/purchase/inquiry/form.vue'),

  // 配送模块
  'dispatch/query': () => import('@/views/dispatch/query/index.vue'),
  'dispatch/query/index': () => import('@/views/dispatch/query/index.vue'),
  'dispatch/logistics-ship': () => import('@/views/dispatch/logistics-ship/index.vue'),
  'dispatch/logistics-ship/index': () => import('@/views/dispatch/logistics-ship/index.vue'),
  'dispatch/freight-reconcile': () => import('@/views/dispatch/freight-reconcile/index.vue'),
  'dispatch/freight-reconcile/index': () => import('@/views/dispatch/freight-reconcile/index.vue'),
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
  'finance/expense-approval': () => import('@/views/finance/expense-approval/index.vue'),
  'finance/expense-approval/index': () => import('@/views/finance/expense-approval/index.vue'),
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
  // 客户公海（80202）：后端 CustomerPoolController 的 10 个端点此前前端零引用
  'crm/customer-pool': () => import('@/views/crm/customer-pool/index.vue'),
  'crm/customer-pool/index': () => import('@/views/crm/customer-pool/index.vue'),
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
  'hr/attendance/list': () => import('@/views/hr/attendance/list.vue'),
  // `hr/attendance/index` 键与 views/hr/attendance/index.vue 已于 2026-09-23 删除：
  //   全库没有任何菜单的 component/list_path 会解析到它（菜单 90002 指向 list.vue），
  //   属「有键无入口」的僵尸路由；且该页的签到/签退写死 `clockIn(0)`（会给 employeeId=0
  //   建考勤记录），一旦被走到就是脏数据。详见 HR_MODULE_AUDIT_20260923.md §P1-3。
  'hr/leave/list': () => import('@/views/hr/leave/list.vue'),
  'hr/salary/list': () => import('@/views/hr/salary/list.vue'),
  'hr/performance/list': () => import('@/views/hr/performance/list.vue'),
  'hr/recruitment/list': () => import('@/views/hr/recruitment/list.vue'),
  'hr/organization/position-list': () => import('@/views/hr/organization/position-list.vue'),

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
  // 打印设置（设置 → 打印管理 → 打印设置，菜单 80930，挂 61205 打印管理）
  'set/print-config': () => import('@/views/set/print-config/index.vue'),
  'set/print-config/index': () => import('@/views/set/print-config/index.vue'),

  // ── displayMode=1 添加标签的 form 页面（Phase 3 CRM） ──
  'crm/customer/form': () => import('@/views/crm/customer/form.vue'),
  'crm/lead/form': () => import('@/views/crm/lead/form.vue'),
  'crm/opportunity/form': () => import('@/views/crm/opportunity/form.vue'),
  'crm/contract/form': () => import('@/views/crm/contract/form.vue'),
  'crm/quotation/form': () => import('@/views/crm/quotation/form.vue'),
  'finance/invoice/form': () => import('@/views/finance/invoice/form.vue'),
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
  'marketing/stored-card': () => import('@/views/marketing/stored-card/index.vue'),
  'marketing/stored-card/index': () => import('@/views/marketing/stored-card/index.vue'),
  'marketing/auto-campaign': () => import('@/views/marketing/auto-campaign/index.vue'),
  'marketing/auto-campaign/index': () => import('@/views/marketing/auto-campaign/index.vue'),
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
  // md/staff-role、md/staff-all 的映射已移除：V11.422.0 把菜单 80531/80532 改指
  // views/system/position|user/index.vue（功能更完整的实现），这两页已废弃删除。
  // md/staff-dept 暂留：菜单 80530 仍指向它，收敛前需先做能力差集比对（见《职员部门开发文档》§5.5）。
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
 * 目录节点（menuType=0，无自身页面组件）的透传组件。
 *
 * 嵌套路由渲染时，若某一层记录的 `components.default` 为空，Vue 会在该层渲染 `<!---->`，
 * 其下所有子页面都不会显示（表现为「点击菜单空白、刷新后正常」）。补一个只渲染
 * `<router-view/>` 的透传组件，即可让嵌套链贯通。
 */
const ROUTE_PASSTHROUGH = defineComponent({
  name: 'RoutePassthrough',
  render: () => h(RouterView),
})

/**
 * 将后台菜单转换为 Vue Router 路由配置，返回数组以支持双入口（displayMode=1 时生成两条路由）
 */
function transformMenuToRoutes(menu: MenuItem, parentPath: string = ''): RouteRecordRaw[] {
  // ⚠️ 按钮类型（menuType=2/3）只承载权限码，**没有页面**，不生成路由。
  //
  // 不跳过会出真事故：按钮项的 path / component 都是空，`routePath` 算出来是 ''，
  // 而「空 path 的子路由」与父路由同 URL ⇒ 会**抢走父级的路由匹配**，
  // 其 meta.title 覆盖父级标题。实测：
  //   · 803「我的待办」下有 8031~8034（审批通过/驳回/转交/撤回，menuType=2）
  //     ⇒ /workflow/task 的浏览器标题变成「审批通过」而不是「我的待办」；
  //   · 801「流程定义」下的 8011~8015、907「招聘管理」下的 9071~9073 同理。
  // 页面内容不受影响（仍渲染父级组件），只有 meta 被污染 —— 症状隐蔽，故在此拦掉。
  if (menu.menuType === 2 || menu.menuType === 3) {
    return []
  }

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
  } else {
    // 目录/分组节点没有自身组件：必须给透传组件，否则该层 components.default 为空，
    // 嵌套路由链在此断开，其下所有子页面渲染 <!---->（点击菜单空白、刷新才显示）
    ;(route as any).component = ROUTE_PASSTHROUGH
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

    // 注册前做一次重名路由体检（只告警、不阻塞）。详见 assertNoDuplicateRouteName 的注释。
    assertNoDuplicateRouteName(layoutRoute.children as RouteRecordRaw[])

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
/**
 * 注册前检查路由树中的**重名路由**并告警（2026-09-23 分析模块审计后新增）。
 *
 * 背景：本文件 `transformMenuToRoutes` 的路由名规则是 `route_name || menu_code`（见该函数内
 * `name: menu.routeName || menu.menuCode`）。两条菜单若 `route_name` 为空且 `menu_code` 相同，
 * 就会生成**同名路由**；Vue Router 对同名路由是「后者覆盖前者」⇒ path 不同的那一条**永远没有路由**，
 * 访问时落到 catch-all 404 页，且 **URL 不变、无 4xx、控制台无报错**，极难排查。
 *
 * 实测（2026-09-23）：该写法曾让「采购分析」(80421)、「库存预警补货」(70051)、「缺货补货」(70052)
 * 三个页面 100% 打不开（已由迁移 V11.499.0 补 unique route_name 修复）。
 *
 * 这里**只告警不抛错**：生产环境宁可保留可用路由，也不应因一条脏菜单让整站路由注册失败。
 */
function assertNoDuplicateRouteName(routes: RouteRecordRaw[]) {
  const seen = new Map<string, string>()
  const dup: string[] = []
  const walk = (list: RouteRecordRaw[]) => {
    for (const r of list) {
      const name = r.name as string | undefined
      if (name) {
        const path = String(r.path)
        if (seen.has(name)) dup.push(`「${name}」被 ${seen.get(name)} 与 ${path} 共用`)
        else seen.set(name, path)
      }
      if (r.children?.length) walk(r.children as RouteRecordRaw[])
    }
  }
  walk(routes)
  if (dup.length) {
    console.error(
      `[动态路由] 检测到 ${dup.length} 组重名路由，后者会覆盖前者、被覆盖的地址将渲染 404：\n  - ` +
        dup.join('\n  - ') +
        '\n  修法：给这些菜单补唯一的 sys_menu.route_name（或让 menu_code 不重复）。'
    )
  }
}

function getRequiredRoutes(): RouteRecordRaw[] {
  return [
    // ── 协议模块：详情页与平台侧条款字典不挂菜单，但必须常驻 ──
    // 详情页（列表页行内「查看详情」/「发起变更」跳这里；不依赖后端菜单树）
    {
      path: 'agreement/detail/:id',
      name: 'AgreementDetail',
      component: () => import('@/views/agreement/detail.vue'),
      meta: { title: '协议详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // 平台侧条款字典维护（入口在协议列表工具栏，按平台权限码隔离）
    {
      path: 'agreement/term-option',
      name: 'AgreementTermOption',
      component: () => import('@/views/agreement/term-option/index.vue'),
      meta: { title: '条款字典维护', icon: 'BookOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // 发起契约向导（入口在协议列表工具栏「发起契约」；选模板或从零起草都走这里）
    {
      path: 'agreement/wizard',
      name: 'AgreementWizard',
      component: () => import('@/views/agreement/wizard/index.vue'),
      meta: { title: '发起契约', icon: 'FileAddOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // 契约模板管理（入口在协议列表工具栏「契约模板」；平台侧与租户侧共用，可见范围由服务端判定）
    {
      path: 'agreement/template',
      name: 'AgreementTemplate',
      component: () => import('@/views/agreement/template/index.vue'),
      meta: { title: '契约模板', icon: 'ProfileOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    // 受邀方打开契约邀请 —— ⚠️ 路径必须与后端下发的短链一致
    // （AgreementInviteToken.shortLink = /agreement/invite?token=xxx），否则唯一送达的链接点进来是 404
    {
      path: 'agreement/invite',
      name: 'AgreementInviteOpen',
      component: () => import('@/views/agreement/invite/index.vue'),
      meta: { title: '打开契约邀请', icon: 'LinkOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
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
      // 询价建单入口（此前只有只读详情，功能齐备却无法新建）
      path: 'purchase/inquiry/form',
      name: 'PurchaseInquiryForm',
      component: () => import('@/views/purchase/inquiry/form.vue'),
      meta: { title: '采购询价', icon: 'ProfileOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '504' }
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
    // 'workflow/task-management' 的兜底条目已移除（2026-09-19）：
    //   · 该路径**已无任何菜单使用** —— 803「我的待办」/ 804「我的已办」的 path 分别是
    //     /workflow/task 与 /workflow/done（sys_menu 实测），本条目成了孤儿路由；
    //   · 它的 name `WorkflowTaskManagement` 不在 task-management.vue 的 DONE_ROUTE_NAMES 里，
    //     谁访问这个旧路径看到的都会是「待办」视图 —— 语义误导，删掉更安全。
    // ⚠️ componentMap 里的 `'workflow/task-management'` 键**必须保留**：803/804 的菜单
    //    component（views/workflow/task-management[.vue]）归一化后正是这个 key，删了菜单会解析不到组件。
    // 流程分析（设置 → 工作流 → 流程分析，菜单 80611 / set:workflow-analysis）：
    // 菜单行已于迁移 V11.417.0 补齐，正常情况由后端菜单树生成路由（path /workflow/process-analysis）。
    // 此处刻意**保留**同路径兜底条目，理由：
    //   ① 菜单树有 30 分钟本地缓存（menu_cache_*），刚补的菜单在旧缓存会话里点不到，
    //      但按页面收到的「URL 直达」请求仍应能打开；
    //   ② 菜单接口异常时走 getFallbackRoutes()，该分支不含本页，此处兜底不影响正常菜单路由
    //      （动态菜单树先注册、同路径优先生效，两条路由 component 指向同一文件，行为一致）。
    {
      path: 'workflow/process-analysis',
      name: 'WorkflowProcessAnalysis',
      component: () => import('@/views/workflow/process-analysis.vue'),
      meta: { title: '流程分析', icon: 'AuditOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      // ⚠️ path 必须是 'notification'（不是 'notification/index'）：
      // 顶栏通知铃铛走 useNotification.ts:108 的 router.push('/notification')，
      // 注册成 /notification/index 会让该入口 404。全仓无人使用带 /index 的写法。
      path: 'notification',
      name: 'Notification',
      component: () => import('@/views/notification/index.vue'),
      meta: { title: '通知公告', icon: 'BellOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      // ⚠️ path 必须是 'profile'：用户下拉「个人中心」走 BasicLayout.vue:367 的
      // navigateTo('/profile')，而 navigateTo 对以 / 开头的路径直接 router.push，
      // 注册成 /profile/index 时该入口必 404。全仓无人使用带 /index 的写法。
      path: 'profile',
      name: 'Profile',
      component: () => import('@/views/profile/index.vue'),
      meta: { title: '个人中心', icon: 'UserOutlined', keepAlive: true, requiresAuth: true }
    },
    {
      path: 'system/data-import',
      name: 'DataImport',
      component: () => import('@/views/system/data-import/index.vue'),
      meta: { title: '数据导入', icon: 'ImportOutlined', keepAlive: true, requiresAuth: true }
    },

    // ── 商城管理模块 ──
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
      meta: { title: '库存管理', icon: 'ContainerOutlined', keepAlive: true, requiresAuth: true, billType: '601' }
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
    // 'erp/pricing'（价格管理）的硬编码路由已移除：与菜单 70503 功能重叠，
    // 且其独有功能依赖不存在的后端接口（详见 componentMap 处的说明），页面已删除。
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
      // 2026-09-19：原先 component 错配到 @/views/erp/purchase/index.vue（那是采购查询组件），
      // 导致「采购询价」菜单点开的是采购查询页。真实询价列表页已按后端
      // PurchaseInquiryController 的 13 个端点（/api/erp/purchase/inquiry/**）新建。
      path: 'erp/purchase/inquiry',
      name: 'ErpPurchaseInquiry',
      component: () => import('@/views/purchase/inquiry/index.vue'),
      meta: { title: '采购询价', icon: 'SearchOutlined', keepAlive: true, requiresAuth: true, billType: '504' }
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
    // 订单池「查看竞标」子页（无独立菜单，由订单池列表跳转；此前只在 componentMap 注册 → 直接 404）
    {
      path: 'dms/order-pool/bid-detail',
      name: 'DmsOrderPoolBidDetail',
      component: () => import('@/views/dms/order-pool/bid-detail.vue'),
      meta: { title: '竞标详情', icon: 'TrophyOutlined', keepAlive: false, requiresAuth: true, hidden: true }
    },
    {
      path: 'sales/return-apply/form/:id',
      name: 'SaleReturnApplyFormEdit',
      component: () => import('@/views/sales/return-apply/form.vue'),
      meta: { title: '编辑销售退货申请', icon: 'RollbackOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    // ═══ 销售模块：列表「新增/详情/编辑」与「返回列表」路由补齐（2026-09-22）═══
    //
    // 为什么必须在这里补：这批页面在数据库里**有**菜单记录（新增 80601/80602/80603/80604、
    // 表单 80092），但它们的 `client_type='pc-admin'` 且 `visible=0`；而本前端固定请求
    // `/menu/user/mega/tenant-admin`（见文件顶部 CLIENT_TYPE），服务端
    // `SysMenuServiceImpl.getUserMegaMenus` 又强制 `client_type = ? AND visible = 1`
    // ⇒ 这批菜单**永远不会下发**，依赖菜单树注册的路由也就永远不会存在。
    // 症状：销售退货单列表的「新增 / 详情 / 编辑 / 打印」、退货申请列表的「新增」、
    // 订单中心「详情」、各表单的「历史」按钮，点下去都落到 Layout 下的 catch-all 404 页
    //（URL 不变但内容是 404，比被弹回工作台更隐蔽）。
    // 判定与实测证据：`tools/verify-sales-routes.cjs`（修复前 14/22 通过）。
    // 与既有做法一致：pre-order / retail 的同类补齐见本函数下方注释。
    {
      path: 'sales/return-doc/create',
      name: 'SaleReturnDocCreate',
      component: () => import('@/views/sales/return-doc/form.vue'),
      meta: { title: '新增销售退货单', icon: 'RollbackOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'sales/return-doc/form/:id',
      name: 'SaleReturnDocFormEdit',
      component: () => import('@/views/sales/return-doc/form.vue'),
      meta: { title: '销售退货单详情', icon: 'RollbackOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    // 列表别名：菜单 70022 的主路由是 sales/return-doc/form（双入口的「表单」入口），
    // 表单页「历史」按钮跳的却是文档约定的列表地址 /sales/return-doc（无后缀），此前无路由。
    {
      path: 'sales/return-doc',
      name: 'SaleReturnDocListAlias',
      component: () => import('@/views/sales/return-doc/index.vue'),
      meta: { title: '销售退货单', icon: 'RollbackOutlined', keepAlive: true, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'sales/outbound/create',
      name: 'SaleOutboundCreate',
      component: () => import('@/views/sales/outbound/form.vue'),
      meta: { title: '新增销售出库单', icon: 'SendOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'sales/outbound',
      name: 'SaleOutboundListAlias',
      component: () => import('@/views/sales/outbound/index.vue'),
      meta: { title: '销售出库单', icon: 'SendOutlined', keepAlive: true, requiresAuth: true, hidden: true, billType: '601' }
    },
    {
      path: 'sales/return-apply/create',
      name: 'SaleReturnApplyCreate',
      component: () => import('@/views/sales/return-apply/form.vue'),
      meta: { title: '新增销售退货申请', icon: 'RollbackOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '601' }
    },
    // 列表别名：退货申请文档约定列表地址为 /sales/return-apply，而菜单 70011 注册的列表路由
    // 是它的 list_path（/sales/return-apply/index）⇒ 直接访问文档地址会 404。
    {
      path: 'sales/return-apply',
      name: 'SaleReturnApplyListAlias',
      component: () => import('@/views/sales/return-apply/index.vue'),
      meta: { title: '销售退货申请', icon: 'RollbackOutlined', keepAlive: true, requiresAuth: true, hidden: true, billType: '601' }
    },
    // 订单中心「详情」与销售退货申请「跳源订单」都跳 /sales/order/form/:id（带 id）。
    // 菜单 70010 注册的是「表单入口」sales/order/form，带 id 的详情页不在菜单树内。
    {
      path: 'sales/order/form/:id',
      name: 'SaleOrderFormDetail',
      component: () => import('@/views/erp/sale/form.vue'),
      meta: { title: '销售订单详情', icon: 'FileTextOutlined', keepAlive: false, requiresAuth: true, hidden: true, billType: '604' }
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
        // ⚠️ 这里**刻意不注入任何业务菜单**。
        //
        // 本函数只在「菜单接口完全不可用」时兜底，而业务路由的可见性本应由后端菜单树
        // 下发的权限上下文决定。旧实现在此处塞进了 41 条全量业务菜单（含 system/user、
        // system/role、system/menu、finance、mall/* 等），等于把权限判断悄悄降级成
        // 「路径在不在 MODULE_ROUTE_MAP 里」——而其中大半条目连 billType 都没有，
        // 只受 hasValidModule 约束，该方法在模块授权数据缺失时还会直接放行。
        //
        // 结论：降级时宁可不显示入口，也不能凭空给出可能越权的入口。
        // 仅保留与角色/模块无关、任何登录用户都该能进的通用页。
        // path 用 'notification' / 'profile'：与 requiredRoutes 保持一致，
        // 否则走 fallback 分支时顶栏通知铃铛与「个人中心」入口仍会 404（详见 requiredRoutes 处的注释）
        { path: 'notification', name: 'Notification', component: () => import('@/views/notification/index.vue'), meta: { title: '通知公告', icon: 'BellOutlined', keepAlive: true, requiresAuth: true } },
        { path: 'profile', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { title: '个人中心', icon: 'UserOutlined', keepAlive: true, requiresAuth: true } },
      ]
    }
  ]
}

/**
 * 「受租户模块授权管辖」的路径 → 模块码 映射。
 *
 * ⚠️ 只登记 `sys_tenant_module` 里**真实存在**的模块码。devdb 实测（2026-09-19）该表
 *    总共只有 5 个码：crm / finance / purchase / sale / warehouse（租户 1、2 各 5 行）。
 *
 * 系统里还有 hr / mall / printing / wms / quality / trade / payment 等模块，但**不在租户
 * 模块授权表里**。把它们登记进来，`hasValidModule` 会因为 `validModuleCodes` 里没有这些码
 * 而一律判 false ⇒ 整块页面被锁死（连工作台都进不去）。所以刻意不登记：
 *   不登记 ⇒ getRouteModule 返回 undefined ⇒ checkRouteAccess 跳过模块校验 ⇒ 放行。
 * 要纳入管辖，前提是先把这些模块码补进 `sys_tenant_module`（属业务决策）。
 *
 * ⚠️ 另一处历史缺陷已修正：原 key 写作 `'customer'`，而库里实际模块码是 **`crm`** ⇒
 *    「客户/合同/线索/商机/报价/发票」六页在模块校验真正生效时会被误拒
 *    （`validModuleCodes.includes('customer')` 恒为 false）。
 */
const MODULE_ROUTE_MAP: Record<string, string[]> = {
  'sale': ['sale', 'erp/sale', 'erp/sales-report',
           'sales/outbound', 'sales/pre-order', 'sales/retail', 'sales/return-apply',
           'sales/exchange', 'sales/return-doc', 'sales/order-center'],
  'purchase': ['purchase', 'erp/purchase', 'purchase/exchange', 'purchase/return',
               'erp/purchase-contract', 'purchase/inquiry'],
  'warehouse': ['stock', 'erp/stock', 'erp/stock-in', 'erp/stocktake',
                'erp/shipment', 'erp/batch', 'erp/serial', 'erp/stock-bom',
                'erp/stock-assemble', 'erp/stock-split', 'wms', 'wh'],
  'finance': ['finance', 'erp/finance', 'budget', 'fixed-asset', 'finance/invoice'],
  'crm': ['crm'],   // ← 原为 'customer'（库中无此码），按实际模块码修正
}

/**
 * 根据路由路径推断所属模块编码
 */
/**
 * 根据路由路径推断所属模块编码；返回 `undefined` 表示该路径**不受模块授权管辖**，
 * 调用方 checkRouteAccess 会跳过模块校验。
 *
 * ⚠️ 不要恢复「用路径首段兜底」的旧写法。那会把 dashboard / profile / notification /
 *    hr / mall / printing 等**未登记**的路径也算成模块码，再配合 hasValidModule 现在的
 *    fail-closed 语义，就会把这些页面直接锁死——工作台都会被判「无权访问」。
 *    只有明确登记在 MODULE_ROUTE_MAP 里的路径才参与模块校验。
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
  return undefined
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