<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/mall">商城管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>商品管理</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <a-space :size="8">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>N</kbd> 新增</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
            </span>
            <a-tooltip title="F5: 刷新 | Ctrl+N: 新增 | Ctrl+E: 导出">
              <a-button size="small" @click="debounceClick('refresh', fetchData)">
                <template #icon><ReloadOutlined /></template>刷新
              </a-button>
            </a-tooltip>
            <a-tooltip title="导出 (Ctrl+E)">
              <a-button size="small" @click="debounceClick('export', handleExport)">
                <template #icon><ExportOutlined /></template>导出
              </a-button>
            </a-tooltip>
            <PrintButton page-code="erp/mall/product" button-size="small" tooltip="打印" />
          </a-space>
        </div>
      </div>
    </template>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      :row-selection="true"
      v-model:selected-row-keys="selectedRowKeys"
      add-text="新增商品"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @refresh="debounceClick('refresh', fetchData)"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-extra>
        <a-button v-if="selectedRowKeys.length > 0" size="small" danger @click="handleBatchDelete">
          批量删除 ({{ selectedRowKeys.length }})
        </a-button>
      </template>
      <template #empty>
        <a-empty v-if="!hasError" description="暂无商品数据" />
        <a-result v-else status="error" title="数据加载失败">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>重新加载
            </a-button>
          </template>
        </a-result>
      </template>

      <template #imageCell="{ record }">
        <a-image
          v-if="record.imageUrl"
          :src="record.imageUrl"
          :width="60"
          :height="60"
          style="object-fit: cover; border-radius: 4px; cursor: pointer;"
          fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        />
        <span v-else style="color: #ccc">无图片</span>
      </template>

      <template #statusCell="{ record }">
        <a-tag v-if="record.status === 'ON_SHELF'" color="green">上架</a-tag>
        <a-tag v-else color="default">下架</a-tag>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button v-permission="'erp:mall:product:edit'" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button v-permission="'erp:mall:product:delete'" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 商品表单弹窗 -->
    <FullScreenDetail
      :visible="modalVisible"
      :title="modalTitle"
      :save-loading="modalLoading"
      @save="handleModalOk"
      @close="handleFormClose"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
        :rules="formRules"
      >
        <a-form-item label="商品编码" name="productId">
          <a-input v-model:value="formState.productId" placeholder="输入商品编码" />
        </a-form-item>
        <a-form-item label="商品名称" name="productName">
          <a-input v-model:value="formState.productName" placeholder="输入商品名称" />
        </a-form-item>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="销售价" name="salePrice">
              <a-input-number v-model:value="formState.salePrice" :min="0" :precision="2" :style="{ width: '100%' }" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="市场价" name="marketPrice">
              <a-input-number v-model:value="formState.marketPrice" :min="0" :precision="2" :style="{ width: '100%' }" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="分类" name="categoryName">
              <a-input v-model:value="formState.categoryName" placeholder="分类名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="formState.status">
                <a-select-option value="ON_SHELF">上架</a-select-option>
                <a-select-option value="OFF_SHELF">下架</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="图片URL" name="imageUrl">
          <a-input v-model:value="formState.imageUrl" placeholder="商品图片URL" />
        </a-form-item>
        <a-form-item label="商品描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="3" />
        </a-form-item>
      </a-form>
    </FullScreenDetail>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallProductList' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ReloadOutlined, ExportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import { mallProductApi } from '@/api/erp/mall'
import type { MallProduct } from '@/api/erp/mall'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<MallProduct[]>([])
const tableDataSource = tableData
const selectedRowKeys = ref<number[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const searchForm = reactive({
  keyword: undefined as string | undefined,
  status: undefined as string | undefined
})

const vxeColumns: any = computed(() => [
  { type: 'checkbox', width: 40 },
  { field: 'productId', title: '商品编码', width: 120 },
  { field: 'imageUrl', title: '图片', width: 100, slotName: 'imageCell' },
  { field: 'productName', title: '商品名称', width: 200, showOverflow: 'tooltip' },
  { field: 'categoryName', title: '分类', width: 100 },
  { field: 'salePrice', title: '销售价', width: 100 },
  { field: 'marketPrice', title: '市场价', width: 100 },
  { field: 'stockQuantity', title: '库存', width: 70 },
  { field: 'status', title: '状态', width: 70, slotName: 'statusCell' },
  { field: 'salesCount', title: '销量', width: 70 },
  { type: 'action', title: '操作', width: 140, fixed: 'right' }
])

const filterFields = computed<FilterField[]>(() => [
  { key: 'keyword', label: '关键词', type: 'input', placeholder: '商品名称/编码' },
  { key: 'status', label: '状态', type: 'select', options: [
    { label: '全部', value: undefined },
    { label: '上架', value: 'ON_SHELF' },
    { label: '下架', value: 'OFF_SHELF' }
  ]}
])

// ── 防抖 ──
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await mallProductApi.page({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    } as any)
    if (res?.data) {
      tableData.value = res.records || []
      pagination.total = res.total || 0
    }
  } catch (err) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[商品管理] 加载商品列表失败', err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { keyword: undefined, status: undefined })
  } else {
    searchForm.keyword = filters.keyword
    searchForm.status = filters.status
  }
  pagination.current = 1
  fetchData()
}

// ── 弹窗 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => isEdit.value ? '编辑商品' : '新增商品')

const formState = reactive<MallProduct>({
  productId: '',
  productName: '',
  imageUrl: '',
  salePrice: 0,
  marketPrice: 0,
  categoryName: '',
  status: 'OFF_SHELF',
  description: ''
})

const formRules: Record<string, any> = {
  productId: { required: true, message: '请输入商品编码', trigger: 'blur' },
  productName: { required: true, message: '请输入商品名称', trigger: 'blur' }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { productId: '', productName: '', imageUrl: '', salePrice: 0, marketPrice: 0, categoryName: '', status: 'OFF_SHELF', description: '' })
  modalVisible.value = true
}

const handleEdit = (record: MallProduct) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    productId: record.productId,
    productName: record.productName,
    imageUrl: record.imageUrl,
    salePrice: record.salePrice,
    marketPrice: record.marketPrice,
    categoryName: record.categoryName,
    status: record.status,
    description: record.description
  })
  modalVisible.value = true
}

const handleDelete = (record: MallProduct) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品 "${record.productName || record.productId}" 吗？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await mallProductApi.delete(record.id!)
        message.success('删除成功')
        fetchData()
      } catch (err) {
        console.warn('[商品管理] 删除失败', err)
      }
    }
  })
}

const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) return
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个商品吗？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await Promise.all(selectedRowKeys.value.map(id => mallProductApi.delete(id)))
        message.success(`成功删除 ${selectedRowKeys.value.length} 个商品`)
        selectedRowKeys.value = []
        fetchData()
      } catch (err) {
        console.warn('[商品管理] 批量删除失败', err)
      }
    }
  })
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    if (isEdit.value) {
      await mallProductApi.update(formState.id!, formState as MallProduct)
      message.success('更新成功')
    } else {
      await mallProductApi.create(formState as MallProduct)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[商品管理] 保存失败', err)
  } finally {
    modalLoading.value = false
  }
}

const handleFormClose = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// ── 导出 ──
async function handleExport() {
  try {
    const blob = await request.get('/erp/mall/product/export', {
      params: { keyword: searchForm.keyword, status: searchForm.status },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商城商品_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败')
  }
}

// ── 键盘快捷键 & 自动刷新 ──
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (e.key === 'F5') {
    e.preventDefault(); debounceClick('refresh', fetchData)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault(); handleAdd()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault(); debounceClick('export', handleExport)
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) { console.warn('[MallProductList]', err) }

defineExpose({ fetchData })
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
