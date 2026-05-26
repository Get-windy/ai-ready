// 采购管理组件库入口文件
import type { App } from 'vue'

// 组件导入
import PurchaseOrderList from './components/PurchaseOrderList.vue'
import PurchaseOrderForm from './components/PurchaseOrderForm.vue'
import PurchaseInquiryList from './components/PurchaseInquiryList.vue'
import PurchaseInquiryForm from './components/PurchaseInquiryForm.vue'
import SupplierList from './components/SupplierList.vue'
import SupplierForm from './components/SupplierForm.vue'
import PurchaseDashboard from './components/PurchaseDashboard.vue'
import PurchaseApprovalList from './components/PurchaseApprovalList.vue'
import PurchaseReport from './components/PurchaseReport.vue'
// 采购付款流程组件
import PurchaseSettlementList from './components/PurchaseSettlementList.vue'
import PurchaseSettlementForm from './components/PurchaseSettlementForm.vue'
import PurchaseSettlementDetail from './components/PurchaseSettlementDetail.vue'
import PaymentApplicationForm from './components/PaymentApplicationForm.vue'
import PaymentTracking from './components/PaymentTracking.vue'
import SettlementAnalysis from './components/SettlementAnalysis.vue'
import PurchaseReconciliation from './components/PurchaseReconciliation.vue'

// 类型导出
export * from './types'
export * from './composables'
export * from './stores'
export * from './utils'
export * from './api'

// 组件导出
export {
  PurchaseOrderList,
  PurchaseOrderForm,
  PurchaseInquiryList,
  PurchaseInquiryForm,
  SupplierList,
  SupplierForm,
  PurchaseDashboard,
  PurchaseApprovalList,
  PurchaseReport,
  // 采购付款流程组件
  PurchaseSettlementList,
  PurchaseSettlementForm,
  PurchaseSettlementDetail,
  PaymentApplicationForm,
  PaymentTracking,
  SettlementAnalysis,
  PurchaseReconciliation
}

// 组件安装函数
const components = [
  PurchaseOrderList,
  PurchaseOrderForm,
  PurchaseInquiryList,
  PurchaseInquiryForm,
  SupplierList,
  SupplierForm,
  PurchaseDashboard,
  PurchaseApprovalList,
  PurchaseReport,
  // 采购付款流程组件
  PurchaseSettlementList,
  PurchaseSettlementForm,
  PurchaseSettlementDetail,
  PaymentApplicationForm,
  PaymentTracking,
  SettlementAnalysis,
  PurchaseReconciliation
]

// 插件安装
export const PurchaseManagementPlugin = {
  install(app: App) {
    components.forEach(component => {
      app.component(component.name || '', component)
    })
  }
}

// 默认导出
export default PurchaseManagementPlugin

// 类型声明
declare module 'vue' {
  export interface GlobalComponents {
    PurchaseOrderList: typeof PurchaseOrderList
    PurchaseOrderForm: typeof PurchaseOrderForm
    PurchaseInquiryList: typeof PurchaseInquiryList
    PurchaseInquiryForm: typeof PurchaseInquiryForm
    SupplierList: typeof SupplierList
    SupplierForm: typeof SupplierForm
    PurchaseDashboard: typeof PurchaseDashboard
    PurchaseApprovalList: typeof PurchaseApprovalList
    PurchaseReport: typeof PurchaseReport
    // 采购付款流程组件
    PurchaseSettlementList: typeof PurchaseSettlementList
    PurchaseSettlementForm: typeof PurchaseSettlementForm
    PurchaseSettlementDetail: typeof PurchaseSettlementDetail
    PaymentApplicationForm: typeof PaymentApplicationForm
    PaymentTracking: typeof PaymentTracking
    SettlementAnalysis: typeof SettlementAnalysis
    PurchaseReconciliation: typeof PurchaseReconciliation
  }
}