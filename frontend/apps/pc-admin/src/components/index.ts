// 错误边界组件
export { default as ErrorBoundary } from './ErrorBoundary/ErrorBoundary.vue'

// 页面容器组件
export { default as PageContainer } from './PageContainer/PageContainer.vue'

// 表格列表组件
export { default as TableList } from './TableList/TableList.vue'
export type { TableColumn } from './TableList/TableList.vue'
export type PaginationConfig = {
  current?: number
  pageSize?: number
  total?: number
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  pageSizeOptions?: string[]
  showTotal?: boolean | ((total: number, range: [number, number]) => string)
}

// 搜索栏组件
export { default as SearchBar } from './SearchBar/SearchBar.vue'
export type { SearchField } from './SearchBar/SearchBar.vue'

// UI组件
export { default as EmptyState } from './EmptyState/EmptyState.vue'
export { default as FullScreenDetail } from './FullScreenDetail/FullScreenDetail.vue'

// 默认导出
import TableList from './TableList/TableList.vue'
import SearchBar from './SearchBar/SearchBar.vue'
import EmptyState from './EmptyState/EmptyState.vue'

export default {
  TableList,
  SearchBar,
  EmptyState
}