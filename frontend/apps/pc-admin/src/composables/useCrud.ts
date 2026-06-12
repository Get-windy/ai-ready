/**
 * 通用 CRUD Composable
 * 整合表格、弹窗、删除、搜索等操作，统一列表页开发模式
 *
 * 使用方式:
 * ```ts
 * const crud = useCrud({
 *   fetchFn: userApi.getPage,
 *   createFn: userApi.create,
 *   updateFn: userApi.update,
 *   deleteFn: userApi.delete,
 *   defaultSearch: { status: undefined }
 * })
 * // 模板中可直接绑定: tableData, loading, pagination 等
 * ```
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'

// ── 类型 ──────────────────────────────────────────────

export interface CrudOptions<T, Q extends Record<string, any>> {
  /** 分页查询函数 */
  fetchFn: (params: any) => Promise<{ data?: { records: T[]; total: number } | T[]; total?: number }>
  /** 创建函数 */
  createFn?: (data: Partial<T>) => Promise<any>
  /** 更新函数 */
  updateFn?: (id: number, data: Partial<T>) => Promise<any>
  /** 删除函数 */
  deleteFn?: (id: number) => Promise<any>
  /** 批量删除函数 */
  batchDeleteFn?: (ids: number[]) => Promise<any>
  /** 获取详情函数 */
  getFn?: (id: number) => Promise<any>
  /** 默认搜索参数 */
  defaultSearch?: Q
  /** 默认分页大小 */
  defaultPageSize?: number
  /** 其他默认分页参数 */
  pagination?: Partial<{
    showSizeChanger: boolean
    showQuickJumper: boolean
    pageSizeOptions: string[]
  }>
  /** 主键字段名，默认 'id' */
  rowKey?: string
  /** 是否在创建时自动挂载加载数据 */
  immediate?: boolean
}

export interface CrudState<T> {
  // 数据
  tableData: T[]
  loading: boolean
  pagination: {
    current: number
    pageSize: number
    total: number
    showSizeChanger: boolean
    showQuickJumper: boolean
    pageSizeOptions: string[]
    showTotal: (total: number) => string
  }
  searchParams: Record<string, any>
  selectedRowKeys: (string | number)[]

  // 弹窗
  modalVisible: boolean
  modalLoading: boolean
  isEdit: boolean
  editId: number | null
  formData: Record<string, any>

  // 详情弹窗
  detailVisible: boolean
  detailData: T | null

  // 方法
  fetchData: () => Promise<void>
  handleSearch: () => void
  handleReset: () => void
  handlePageChange: (page: number, pageSize: number) => void
  handleAdd: () => void
  handleEdit: (record: T) => void
  handleView: (record: T) => void
  handleDelete: (record: T, confirmMsg?: string) => void
  handleBatchDelete: () => void
  handleModalOk: (submitFn: () => Promise<void>) => Promise<void>
  handleModalCancel: () => void
  closeDetail: () => void
  clearSelection: () => void
  refresh: () => void
}

// ── 实现 ──────────────────────────────────────────────

export function useCrud<T extends { id?: number; [key: string]: any }, Q extends Record<string, any> = Record<string, any>>(
  options: CrudOptions<T, Q>
): CrudState<T> {
  const {
    fetchFn,
    createFn,
    updateFn,
    deleteFn,
    batchDeleteFn,
    defaultSearch = {} as Q,
    defaultPageSize = 10,
    pagination: paginationOptions = {},
    rowKey = 'id',
    immediate = true
  } = options

  // ── 数据 ──────────────────────────
  const tableData = ref<T[]>([])
  const loading = ref(false)

  const pagination = reactive({
    current: 1,
    pageSize: defaultPageSize,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total: number) => `共 ${total} 条`,
    ...paginationOptions
  })

  const searchParams = reactive<Record<string, any>>({ ...defaultSearch })
  const selectedRowKeys = ref<(string | number)[]>([])

  // ── 弹窗 ──────────────────────────
  const modalVisible = ref(false)
  const modalLoading = ref(false)
  const isEdit = ref(false)
  const editId = ref<number | null>(null)
  const formData = reactive<Record<string, any>>({})

  // ── 详情 ──────────────────────────
  const detailVisible = ref(false)
  const detailData = ref<T | null>(null)

  // ── 获取数据 ──────────────────────
  const fetchData = async () => {
    loading.value = true
    try {
      const res: any = await fetchFn({
        current: pagination.current,
        size: pagination.pageSize,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        ...searchParams
      })
      // 兼容多种返回格式：{ data: { records, total } } 或 { records, total } 或 data 就是数组
      const responseData = res?.data ?? res
      if (Array.isArray(responseData)) {
        tableData.value = responseData as T[]
        pagination.total = (res?.total || responseData.length)
      } else if (responseData?.records) {
        tableData.value = responseData.records as T[]
        pagination.total = responseData.total || 0
      } else {
        tableData.value = []
        pagination.total = 0
      }
    } catch (error: any) {
      console.error('[useCrud] fetchData error:', error)
      message.error(error?.response?.data?.message || error?.message || '获取数据失败')
      tableData.value = []
    } finally {
      loading.value = false
    }
  }

  // ── 搜索重置 ──────────────────────
  const handleSearch = () => {
    pagination.current = 1
    fetchData()
  }

  const handleReset = () => {
    Object.keys(searchParams).forEach(key => { searchParams[key] = undefined })
    Object.assign(searchParams, defaultSearch)
    handleSearch()
  }

  // ── 分页 ──────────────────────────
  const handlePageChange = (page: number, pageSize: number) => {
    pagination.current = page
    pagination.pageSize = pageSize
    fetchData()
  }

  // ── 新增编辑 ──────────────────────
  const handleAdd = () => {
    isEdit.value = false
    editId.value = null
    Object.keys(formData).forEach(k => { delete formData[k] })
    modalVisible.value = true
  }

  const handleEdit = (record: T) => {
    isEdit.value = true
    editId.value = record.id ?? null
    Object.assign(formData, { ...record })
    modalVisible.value = true
  }

  // ── 查看详情 ──────────────────────
  const handleView = (record: T) => {
    detailData.value = record
    detailVisible.value = true
  }

  const closeDetail = () => {
    detailVisible.value = false
    detailData.value = null
  }

  // ── 删除 ──────────────────────────
  const handleDelete = (record: T, confirmMsg?: string) => {
    if (!deleteFn) return
    Modal.confirm({
      title: '确认删除',
      content: confirmMsg || `确定要删除该记录吗？`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteFn(record.id!)
          message.success('删除成功')
          fetchData()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '删除失败')
        }
      }
    })
  }

  // ── 批量删除 ──────────────────────
  const handleBatchDelete = () => {
    if (!batchDeleteFn) return
    const keys = selectedRowKeys.value
    if (!keys.length) {
      message.warning('请先选择要删除的数据')
      return
    }
    Modal.confirm({
      title: '确认删除',
      content: `确定要批量删除选中的 ${keys.length} 条记录吗？`,
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await batchDeleteFn(keys as number[])
          message.success('删除成功')
          selectedRowKeys.value = []
          fetchData()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '删除失败')
        }
      }
    })
  }

  // ── 弹窗提交 ──────────────────────
  const handleModalOk = async (submitFn: () => Promise<void>) => {
    try {
      await submitFn()
      modalVisible.value = false
      fetchData()
    } catch {
      // submitFn 内部已处理错误提示
    }
  }

  const handleModalCancel = () => {
    modalVisible.value = false
  }

  // ── 辅助 ──────────────────────────
  const clearSelection = () => { selectedRowKeys.value = [] }
  const refresh = () => { fetchData() }

  // ── 初始化 ────────────────────────
  if (immediate) {
    onMounted(() => { fetchData() })
  }

  return {
    tableData, loading, pagination, searchParams, selectedRowKeys,
    modalVisible, modalLoading, isEdit, editId, formData,
    detailVisible, detailData,
    fetchData, handleSearch, handleReset, handlePageChange,
    handleAdd, handleEdit, handleView, handleDelete, handleBatchDelete,
    handleModalOk, handleModalCancel, closeDetail, clearSelection, refresh
  } as unknown as CrudState<T>
}
