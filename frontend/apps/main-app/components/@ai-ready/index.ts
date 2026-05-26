// AI-Ready 组件库入口文件
// 统一注册所有模块组件

import type { App } from 'vue'
import { setupElementPlus } from './COMPONENT_CONFIG'

// 导入各模块组件
// 用户管理模块
export { default as UserList } from './user-management/components/UserList.vue'
export { default as UserForm } from './user-management/components/UserForm.vue'
export { default as UserDetail } from './user-management/views/UserDetail.vue'
export { default as UserManage } from './user-management/views/UserManage.vue'

// 订单管理模块
export { default as OrderList } from './order-management/components/OrderList.vue'
export { default as OrderForm } from './order-management/components/OrderForm.vue'
export { default as OrderDetail } from './order-management/views/OrderDetail.vue'
export { default as OrderManage } from './order-management/views/OrderManage.vue'

// 库存管理模块
export { default as InventoryList } from './inventory-management/components/InventoryList.vue'
export { default as InventoryForm } from './inventory-management/components/InventoryForm.vue'
export { default as InventoryDetail } from './inventory-management/views/InventoryDetail.vue'
export { default as InventoryManage } from './inventory-management/views/InventoryManage.vue'

// 采购管理模块
export { default as PurchaseList } from './purchase-management/components/PurchaseList.vue'
export { default as PurchaseForm } from './purchase-management/components/PurchaseForm.vue'
export { default as PurchaseDetail } from './purchase-management/views/PurchaseDetail.vue'
export { default as PurchaseManage } from './purchase-management/views/PurchaseManage.vue'

// 财务管理模块
export { default as FinanceList } from './finance-management/components/FinanceList.vue'
export { default as FinanceForm } from './finance-management/components/FinanceForm.vue'
export { default as FinanceDetail } from './finance-management/views/FinanceDetail.vue'
export { default as FinanceManage } from './finance-management/views/FinanceManage.vue'

// AI智能功能模块
export { default as AIAssistant } from './ai-features/components/AIAssistant.vue'
export { default as AIChat } from './ai-features/components/AIChat.vue'
export { default as AIAnalysis } from './ai-features/views/AIAnalysis.vue'

// 通用组件
export { default as CommonTable } from './common/components/CommonTable.vue'
export { default as CommonForm } from './common/components/CommonForm.vue'
export { default as CommonModal } from './common/components/CommonModal.vue'

// 组件列表
const components = {
  // 用户管理
  UserList,
  UserForm,
  UserDetail,
  UserManage,

  // 订单管理
  OrderList,
  OrderForm,
  OrderDetail,
  OrderManage,

  // 库存管理
  InventoryList,
  InventoryForm,
  InventoryDetail,
  InventoryManage,

  // 采购管理
  PurchaseList,
  PurchaseForm,
  PurchaseDetail,
  PurchaseManage,

  // 财务管理
  FinanceList,
  FinanceForm,
  FinanceDetail,
  FinanceManage,

  // AI智能功能
  AIAssistant,
  AIChat,
  AIAnalysis,

  // 通用组件
  CommonTable,
  CommonForm,
  CommonModal,
}

// 安装函数
export function install(app: App) {
  // 设置 Element Plus
  setupElementPlus(app)

  // 注册所有组件
  Object.entries(components).forEach(([name, component]) => {
    if (component) {
      app.component(name, component)
    }
  })
}

// 默认导出
export default {
  install,
  ...components,
}

// Vue 插件类型声明
declare module '@vue/runtime-core' {
  export interface GlobalComponents {
    // 用户管理
    UserList: typeof UserList
    UserForm: typeof UserForm
    UserDetail: typeof UserDetail
    UserManage: typeof UserManage

    // 订单管理
    OrderList: typeof OrderList
    OrderForm: typeof OrderForm
    OrderDetail: typeof OrderDetail
    OrderManage: typeof OrderManage

    // 库存管理
    InventoryList: typeof InventoryList
    InventoryForm: typeof InventoryForm
    InventoryDetail: typeof InventoryDetail
    InventoryManage: typeof InventoryManage

    // 采购管理
    PurchaseList: typeof PurchaseList
    PurchaseForm: typeof PurchaseForm
    PurchaseDetail: typeof PurchaseDetail
    PurchaseManage: typeof PurchaseManage

    // 财务管理
    FinanceList: typeof FinanceList
    FinanceForm: typeof FinanceForm
    FinanceDetail: typeof FinanceDetail
    FinanceManage: typeof FinanceManage

    // AI智能功能
    AIAssistant: typeof AIAssistant
    AIChat: typeof AIChat
    AIAnalysis: typeof AIAnalysis

    // 通用组件
    CommonTable: typeof CommonTable
    CommonForm: typeof CommonForm
    CommonModal: typeof CommonModal
  }
}