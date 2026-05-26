// 供应商门户组件库入口文件

// 导出基础组件
export { default as SupplierButton } from './components/SupplierButton.vue'
export { default as SupplierInput } from './components/SupplierInput.vue'
export { default as SupplierCard } from './components/SupplierCard.vue'

// 导出产品管理组件
export { default as ProductGrid } from './components/ProductGrid.vue'
export { default as ProductCard } from './components/ProductCard.vue'

// 导出交易流程组件
export { default as QuotationForm } from './components/QuotationForm.vue'
export { default as OrderTracking } from './components/OrderTracking.vue'

// 导出供应商管理组件
export { default as SupplierInfo } from './components/SupplierInfo.vue'
export { default as SupplierQualification } from './components/SupplierQualification.vue'

// 导出复合组件
export { default as SupplierDashboard } from './components/SupplierDashboard.vue'
export { default as ProductManagement } from './components/ProductManagement.vue'

// 导出工具函数和类型
export * from './types/supplier'
export * from './types/product'
export * from './types/transaction'

// 样式文件
import './styles/supplier-portal.css'

// 版本信息
export const VERSION = '1.0.0'
export const COMPONENT_LIBRARY_NAME = 'ai-ready-supplier-portal'

// 组件库配置
export const config = {
  theme: 'supplier-blue',
  version: VERSION,
  components: [
    'SupplierButton',
    'SupplierInput',
    'SupplierCard',
    'ProductGrid',
    'ProductCard',
    'QuotationForm',
    'OrderTracking',
    'SupplierInfo',
    'SupplierQualification',
    'SupplierDashboard',
    'ProductManagement'
  ]
}