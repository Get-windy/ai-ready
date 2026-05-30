/**
 * ModuleLayout 通用列表页 Composable
 *
 * 封装 ModuleLayout 组件所需的所有状态和事件处理方法，
 * 消除各 Tab 页面中的重复代码。
 *
 * 使用方式：
 * ```ts
 * const {
 *   loading, dataSource, pagination,
 *   selectedRowKeys, currentView, activeFilters,
 *   showSearchPanel, searchPanelCollapsed,
 *   fetchData, handleViewChange, handleClearSelection,
 *   handleSearchSubmit, handlePageChange, handlePageSizeChange,
 *   breadcrumbItems, moduleTitle
 * } = useModulePage({
 *   fetchFn: purchaseOrderApi.page,
 *   moduleName: '采购管理',
 *   pageTitle: '采购订单',
 *   modulePath: '/purchase',
 *   statusGroups: [...] // optional - for kanban view
 * })
 * ```
 */
import { ref, reactive, computed, type Ref, type ComputedRef } from 'vue'
import { useRoute } from 'vue-router'
import type { ViewOption, FilterItem, BreadcrumbItem } from '@ai-ready/components'

// ── 类型 ──────────────────────────────────────────────

export interface PaginationState {
  current: number
  pageSize: number
  total: number
}

export interface FetchParams {
  current: number
  size: number
  [key: string]: any
}

export interface FetchResult<T> {
  records: T[]
  total: number
}

export type FetchFunction<T> = (params: FetchParams) => Promise<FetchResult<T>>

export interface ModulePageOptions<T> {
  /** 数据获取函数 — 接收 { current, size, ...filters }，返回 { records, total } */
  fetchFn: FetchFunction<T>
  /** 默认分页大小 */
  defaultPageSize?: number
  /** 分页大小选项 */
  pageSizeOptions?: number[]
  /** 筛选器配置 */
  filters?: FilterItem[]
  /** 可用视图列表 */
  availableViews?: ViewOption[]
  /** 模块名称（用于面包屑） */
  moduleName?: string
  /** 页面标题（用于面包屑最后一项） */
  pageTitle?: string
  /** 模块路径（用于面包屑点击返回） */
  modulePath?: string
  /** 默认搜索占位文本 */
  searchPlaceholder?: string
  /** 是否显示搜索面板 */
  defaultShowSearchPanel?: boolean
}

export interface ModulePageState<T> {
  // ── 数据 ──────────────────────────
  loading: Ref<boolean>
  dataSource: Ref<T[]>
  pagination: PaginationState
  error: Ref<string | null>

  // ── 选择 ──────────────────────────
  selectedRowKeys: Ref<(string | number)[]>

  // ── 视图 ──────────────────────────
  currentView: Ref<string>

  // ── 筛选 ──────────────────────────
  activeFilters: Record<string, any>
  showSearchPanel: Ref<boolean>
  searchPanelCollapsed: Ref<boolean>
  searchValue: Ref<string>

  // ── 面包屑 ────────────────────────
  breadcrumbItems: ComputedRef<BreadcrumbItem[]>

  // ── 方法 ──────────────────────────
  /** 加载数据（会自动带上当前分页和筛选条件） */
  fetchData: () => Promise<void>
  /** 设置搜索值触发数据刷新 */
  handleSearchSubmit: (value: string) => void
  /** 切换视图 */
  handleViewChange: (view: string) => void
  /** 清除选中 */
  handleClearSelection: () => void
  /** 切换筛选面板 */
  handleFilterToggle: () => void
  /** 筛选值变更 */
  handleFilterChange: (key: string, value: any) => void
  /** 清除所有筛选 */
  handleClearAllFilters: () => void
  /** 分页 */
  handlePageChange: (page: number) => void
  /** 每页条数变更 */
  handlePageSizeChange: (size: number) => void
  /** 搜索面板折叠切换 */
  handleSearchPanelToggle: (collapsed: boolean) => void
}

// ── 实现 ──────────────────────────────────────────────

export function useModulePage<T extends Record<string, any>>(
  options: ModulePageOptions<T>
): ModulePageState<T> {
  const {
    fetchFn,
    defaultPageSize = 10,
    pageSizeOptions,
    filters: filterConfig = [],
    availableViews,
    moduleName = '',
    pageTitle = '',
    modulePath = '/',
    searchPlaceholder = '搜索...',
    defaultShowSearchPanel = false
  } = options

  const route = useRoute()

  // ── 响应式状态 ──────────────────────────────────────
  const loading = ref(false)
  const dataSource = ref<T[]>([]) as Ref<T[]>
  const error = ref<string | null>(null)
  const selectedRowKeys = ref<(string | number)[]>([])
  const currentView = ref('list')
  const showSearchPanel = ref(defaultShowSearchPanel)
  const searchPanelCollapsed = ref(false)
  const searchValue = ref('')

  const pagination = reactive<PaginationState>({
    current: 1,
    pageSize: defaultPageSize,
    total: 0
  })

  const activeFilters = reactive<Record<string, any>>({})

  // ── 面包屑 ──────────────────────────────────────────
  const breadcrumbItems = computed<BreadcrumbItem[]>(() => {
    const items: BreadcrumbItem[] = []
    if (moduleName) {
      items.push({ text: moduleName, path: modulePath })
    }
    if (pageTitle) {
      items.push({ text: pageTitle })
    }
    return items
  })

  // ── 数据加载 ────────────────────────────────────────
  async function fetchData() {
    loading.value = true
    error.value = null
    try {
      const params: FetchParams = {
        current: pagination.current,
        size: pagination.pageSize,
        ...activeFilters
      }
      if (searchValue.value) {
        params.searchKeyword = searchValue.value
      }
      const res = await fetchFn(params)
      dataSource.value = res.records || []
      pagination.total = res.total || 0
    } catch (err: any) {
      const msg = err?.message || err?.msg || '获取数据失败'
      error.value = msg
      console.error('[useModulePage] fetchData error:', err)
    } finally {
      loading.value = false
    }
  }

  // ── 事件处理器 ──────────────────────────────────────
  function handleSearchSubmit(value: string) {
    searchValue.value = value
    pagination.current = 1
    fetchData()
  }

  function handleViewChange(view: string) {
    currentView.value = view
  }

  function handleClearSelection() {
    selectedRowKeys.value = []
  }

  function handleFilterToggle() {
    showSearchPanel.value = !showSearchPanel.value
    if (showSearchPanel.value) {
      searchPanelCollapsed.value = false
    }
  }

  function handleFilterChange(key: string, value: any) {
    activeFilters[key] = value
  }

  function handleClearAllFilters() {
    Object.keys(activeFilters).forEach(key => {
      delete activeFilters[key]
    })
    pagination.current = 1
    fetchData()
  }

  function handlePageChange(page: number) {
    pagination.current = page
    fetchData()
  }

  function handlePageSizeChange(size: number) {
    pagination.pageSize = size
    pagination.current = 1
    fetchData()
  }

  function handleSearchPanelToggle(collapsed: boolean) {
    searchPanelCollapsed.value = collapsed
  }

  return {
    loading,
    dataSource,
    pagination,
    error,
    selectedRowKeys,
    currentView,
    activeFilters,
    showSearchPanel,
    searchPanelCollapsed,
    searchValue,
    breadcrumbItems,
    fetchData,
    handleSearchSubmit,
    handleViewChange,
    handleClearSelection,
    handleFilterToggle,
    handleFilterChange,
    handleClearAllFilters,
    handlePageChange,
    handlePageSizeChange,
    handleSearchPanelToggle
  }
}
