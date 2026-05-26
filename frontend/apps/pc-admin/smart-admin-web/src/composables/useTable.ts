/**
 * 通用表格操作 Composable
 * 提供表格数据加载、搜索、分页、选择等功能
 */
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'

export interface TableOptions<T, Q> {
  // 数据加载函数
  fetchFn: (params: Q & { pageNum: number; pageSize: number }) => Promise<{
    records: T[]
    total: number
  }>
  // 默认搜索参数
  defaultSearchParams?: Partial<Q>
  // 默认分页配置
  defaultPagination?: Partial<{
    current: number
    pageSize: number
  }>
}

export function useTable<T extends { id: number }, Q extends Record<string, any>>(
  options: TableOptions<T, Q>
) {
  const { fetchFn, defaultSearchParams = {}, defaultPagination = {} } = options

  // 表格数据
  const tableData = ref<T[]>([])
  const loading = ref(false)
  const selectedRowKeys = ref<number[]>([])

  // 搜索参数
  const searchParams = reactive<Q>({
    ...defaultSearchParams
  } as Q)

  // 分页配置
  const pagination = reactive({
    current: defaultPagination.current || 1,
    pageSize: defaultPagination.pageSize || 10,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条`
  })

  // 加载数据
  const fetchData = async () => {
    loading.value = true
    try {
      const res = await fetchFn({
        ...searchParams,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      })
      tableData.value = res.records
      pagination.total = res.total
    } catch (error) {
      message.error('加载数据失败')
    } finally {
      loading.value = false
    }
  }

  // 搜索
  const handleSearch = () => {
    pagination.current = 1
    fetchData()
  }

  // 重置搜索
  const handleReset = (resetFields?: (keyof Q)[]) => {
    if (resetFields) {
      resetFields.forEach(field => {
        delete searchParams[field]
      })
    } else {
      Object.keys(searchParams).forEach(key => {
        delete searchParams[key as keyof Q]
      })
      Object.assign(searchParams, defaultSearchParams)
    }
    handleSearch()
  }

  // 表格变化
  const handleTableChange: TableProps['onChange'] = (pag) => {
    pagination.current = pag.current || 1
    pagination.pageSize = pag.pageSize || 10
    fetchData()
  }

  // 选择变化
  const onSelectChange = (keys: number[]) => {
    selectedRowKeys.value = keys
  }

  // 清空选择
  const clearSelection = () => {
    selectedRowKeys.value = []
  }

  // 刷新当前页
  const refresh = () => {
    fetchData()
  }

  // 刷新并回到第一页
  const reload = () => {
    pagination.current = 1
    fetchData()
  }

  // 行选择配置
  const rowSelection = {
    selectedRowKeys,
    onChange: onSelectChange
  }

  // 初始化加载
  onMounted(() => {
    fetchData()
  })

  return {
    // 数据
    tableData,
    loading,
    selectedRowKeys,
    searchParams,
    pagination,
    rowSelection,
    
    // 方法
    fetchData,
    handleSearch,
    handleReset,
    handleTableChange,
    onSelectChange,
    clearSelection,
    refresh,
    reload
  }
}

/**
 * 通用弹窗操作 Composable
 */
export interface ModalOptions<T> {
  // 创建函数
  createFn?: (data: T) => Promise<void>
  // 更新函数
  updateFn?: (id: number, data: T) => Promise<void>
  // 默认表单数据
  defaultFormData?: Partial<T>
}

export function useModal<T extends Record<string, any>>(options: ModalOptions<T> = {}) {
  const { createFn, updateFn, defaultFormData = {} } = options

  const modalVisible = ref(false)
  const modalLoading = ref(false)
  const isEdit = ref(false)
  const formData = reactive<T>({ ...defaultFormData } as T)

  // 打开新增弹窗
  const openCreate = () => {
    isEdit.value = false
    Object.assign(formData, defaultFormData)
    modalVisible.value = true
  }

  // 打开编辑弹窗
  const openEdit = (record: T) => {
    isEdit.value = true
    Object.assign(formData, record)
    modalVisible.value = true
  }

  // 关闭弹窗
  const closeModal = () => {
    modalVisible.value = false
    modalLoading.value = false
  }

  // 提交表单
  const submitForm = async (validate?: () => Promise<void>) => {
    try {
      if (validate) {
        await validate()
      }
      
      modalLoading.value = true
      
      if (isEdit.value && updateFn) {
        await updateFn(formData.id, formData)
        message.success('更新成功')
      } else if (createFn) {
        await createFn(formData)
        message.success('创建成功')
      }
      
      closeModal()
      return true
    } catch (error: any) {
      if (error?.errorFields) {
        // 表单验证错误
        return false
      }
      message.error('操作失败')
      return false
    } finally {
      modalLoading.value = false
    }
  }

  return {
    modalVisible,
    modalLoading,
    isEdit,
    formData,
    
    openCreate,
    openEdit,
    closeModal,
    submitForm
  }
}

/**
 * 通用删除操作 Composable
 */
export function useDelete<T extends { id: number }>() {
  const deleteLoading = ref(false)

  // 单个删除
  const handleDelete = async (
    record: T,
    deleteFn: (id: number) => Promise<void>,
    onSuccess?: () => void
  ) => {
    deleteLoading.value = true
    try {
      await deleteFn(record.id)
      message.success('删除成功')
      onSuccess?.()
    } catch (error: any) {
      message.error(error.message || '删除失败')
    } finally {
      deleteLoading.value = false
    }
  }

  // 批量删除
  const handleBatchDelete = async (
    ids: number[],
    deleteFn: (ids: number[]) => Promise<void>,
    onSuccess?: () => void
  ) => {
    if (!ids.length) {
      message.warning('请选择要删除的数据')
      return
    }
    
    deleteLoading.value = true
    try {
      await deleteFn(ids)
      message.success('删除成功')
      onSuccess?.()
    } catch (error: any) {
      message.error(error.message || '删除失败')
    } finally {
      deleteLoading.value = false
    }
  }

  return {
    deleteLoading,
    handleDelete,
    handleBatchDelete
  }
}