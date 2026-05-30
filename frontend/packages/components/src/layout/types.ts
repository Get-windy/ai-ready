/**
 * 布局组件共享类型定义
 */

// ── 面包屑 ────────────────────────────────────────────
export interface BreadcrumbItem {
  text: string
  path?: string
}

// ── 视图切换 ──────────────────────────────────────────
export interface ViewOption {
  value: string
  label: string
  icon: string
}

export const DEFAULT_VIEW_OPTIONS: ViewOption[] = [
  { value: 'list', label: '列表', icon: '☰' },
  { value: 'kanban', label: '看板', icon: '▦' },
  { value: 'calendar', label: '日历', icon: '📅' },
  { value: 'graph', label: '图表', icon: '📊' }
]

export const DEFAULT_PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

// ── Tab 导航 ──────────────────────────────────────────
export interface TabItem {
  key: string
  label: string
  /** 可选徽标数量 */
  count?: number
  /** 是否禁用该 tab */
  disabled?: boolean
}

export interface ModuleTabsItem extends TabItem {
  /** 可选图标名称 */
  icon?: string
}

// ── 搜索 / 筛选 ───────────────────────────────────────
export interface FilterOption {
  value: any
  label: string
}

export interface FilterItem {
  key: string
  label: string
  type: 'checkbox' | 'select' | 'date' | 'daterange'
  options?: FilterOption[]
  value?: any
}

// ── ControlPanel ──────────────────────────────────────
export interface ControlPanelProps {
  breadcrumbItems?: BreadcrumbItem[]
  currentView?: string
  availableViews?: ViewOption[]
  showViewSwitch?: boolean
  selectedCount?: number
  showSearch?: boolean
  searchPlaceholder?: string
  searchValue?: string
  showFilterButton?: boolean
  activeFilterCount?: number
  showPagination?: boolean
  currentPage?: number
  totalPages?: number
  pageSize?: number
  pageSizeOptions?: number[]
  totalItems?: number
  /** 是否显示加载状态 */
  loading?: boolean
  /** 搜索防抖毫秒数 */
  searchDebounce?: number
}

export interface ControlPanelEmits {
  (e: 'breadcrumbClick', item: BreadcrumbItem, index: number): void
  (e: 'viewChange', view: string): void
  (e: 'clearSelection'): void
  (e: 'searchInput', value: string): void
  (e: 'searchSubmit', value: string): void
  (e: 'filterToggle'): void
  (e: 'pageChange', page: number): void
  (e: 'pageSizeChange', size: number): void
  (e: 'update:searchValue', value: string): void
  (e: 'update:pageSize', size: number): void
}

// ── ModuleLayout ──────────────────────────────────────
export interface ModuleLayoutProps {
  breadcrumbItems?: BreadcrumbItem[]
  currentView?: string
  availableViews?: ViewOption[]
  showViewSwitch?: boolean
  selectedCount?: number
  showSearch?: boolean
  searchPlaceholder?: string
  searchValue?: string
  showFilterButton?: boolean
  activeFilterCount?: number
  showPagination?: boolean
  currentPage?: number
  totalPages?: number
  pageSize?: number
  pageSizeOptions?: number[]
  totalItems?: number
  showSearchPanel?: boolean
  searchPanelCollapsed?: boolean
  filters?: FilterItem[]
  activeFilters?: Record<string, any>
  /** 模块级 Tab 项列表 */
  tabs?: ModuleTabsItem[]
  /** 当前激活的 tab key */
  activeTab?: string
  /** 是否显示加载状态 */
  loading?: boolean
  /** 错误信息，非空时显示错误状态 */
  error?: string | null
  /** 数据是否为空（用于空状态提示） */
  empty?: boolean
  /** 空状态描述文本 */
  emptyText?: string
}

export interface ModuleLayoutEmits {
  (e: 'breadcrumbClick', item: BreadcrumbItem, index: number): void
  (e: 'viewChange', view: string): void
  (e: 'clearSelection'): void
  (e: 'searchInput', value: string): void
  (e: 'searchSubmit', value: string): void
  (e: 'filterToggle'): void
  (e: 'pageChange', page: number): void
  (e: 'pageSizeChange', size: number): void
  (e: 'searchPanelToggle', collapsed: boolean): void
  (e: 'filterChange', key: string, value: any): void
  (e: 'clearAllFilters'): void
  (e: 'tabChange', key: string): void
  (e: 'update:activeTab', key: string): void
}

// ── DetailLayout ──────────────────────────────────────
export interface MoreAction {
  key: string
  label: string
  /** 是否显示为危险操作（红色） */
  danger?: boolean
  /** 操作是否禁用 */
  disabled?: boolean
}

export interface RelatedDocument {
  id: string | number
  type: string
  no: string
  status?: string
  path?: string
}

export interface ActivityLog {
  id: string | number
  time: string
  user: string
  action: string
}

export interface DetailLayoutProps {
  breadcrumbItems?: BreadcrumbItem[]
  title?: string
  status?: string
  statusType?: 'success' | 'warning' | 'danger' | 'info' | 'default'
  showPager?: boolean
  currentIndex?: number
  totalCount?: number
  showMoreActions?: boolean
  moreActions?: MoreAction[]
  tabs?: TabItem[]
  activeTab?: string
  showRelatedDocuments?: boolean
  relatedDocuments?: RelatedDocument[]
  showActivityLog?: boolean
  activityLogs?: ActivityLog[]
  /** 是否显示加载状态 */
  loading?: boolean
  /** 错误信息，非空时显示错误状态 */
  error?: string | null
}

export interface DetailLayoutEmits {
  (e: 'breadcrumbClick', item: BreadcrumbItem, index: number): void
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'tabChange', key: string): void
  (e: 'moreActionClick', action: MoreAction): void
  (e: 'relatedClick', doc: RelatedDocument): void
  (e: 'update:activeTab', key: string): void
}

// ── SearchPanel ───────────────────────────────────────
export interface SearchPanelProps {
  filters?: FilterItem[]
  activeFilters?: Record<string, any>
  /** 是否显示加载状态 */
  loading?: boolean
}

export interface SearchPanelEmits {
  (e: 'filterChange', key: string, value: any): void
  (e: 'clearAll'): void
  (e: 'apply'): void
}
