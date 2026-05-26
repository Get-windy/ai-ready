// 错误边界组件导出
export { default as ErrorBoundary } from './ErrorBoundary.vue'
export { default as GlobalErrorFallback } from './GlobalErrorFallback.vue'

// 工具函数导出
export {
  createErrorBoundary,
  withErrorBoundary,
  createAsyncComponentWithErrorBoundary
} from './errorBoundaryUtils'

// 类型导出
export type { ErrorBoundaryProps } from './ErrorBoundary.vue'

// 默认导出
import ErrorBoundary from './ErrorBoundary.vue'
import GlobalErrorFallback from './GlobalErrorFallback.vue'
import {
  createErrorBoundary,
  withErrorBoundary,
  createAsyncComponentWithErrorBoundary
} from './errorBoundaryUtils'

export default {
  ErrorBoundary,
  GlobalErrorFallback,
  createErrorBoundary,
  withErrorBoundary,
  createAsyncComponentWithErrorBoundary
}