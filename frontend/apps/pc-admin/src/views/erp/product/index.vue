<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 基础资料 / 产品管理</span>
          <h2 class="page-header__title">产品管理</h2>
        </div>
        <div class="page-header__right">
          <a-space :size="12">
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
              <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
                <SyncOutlined /> {{ autoRefreshCountdown }}s
              </span>
            </span>
            <a-button size="small" :loading="loading" @click="debounceClick('refresh', refreshAll)">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <div class="product-main">
      <!-- ========== 左侧分类树 ========== -->
      <div class="category-panel">
        <div class="category-header">
          <span class="category-title">产品分类</span>
          <a-button v-permission="'erp:product:category-edit'" type="link" size="small" @click="showCategoryModal(null)">
            <PlusOutlined /> 新增
          </a-button>
        </div>
        <div class="category-search">
          <a-input-search
            v-model:value="categorySearch"
            placeholder="搜索分类"
            size="small"
          />
        </div>
        <div class="category-tree-container">
          <a-spin :spinning="categoryLoading">
            <a-tree
              v-if="!categoryLoading"
              :tree-data="categoryTreeData"
              :selected-keys="[selectedCategoryId]"
              :expanded-keys="expandedKeys"
              show-icon
              block-node
              @select="onCategorySelect"
              @expand="onExpand"
            >
              <template #title="{ categoryName, productCount }">
                <span>{{ categoryName }}</span>
                <span v-if="productCount !== undefined" class="cat-count">({{ productCount }})</span>
              </template>
              <template #icon="{ status }">
                <FolderOutlined v-if="status !== 0" style="color: #faad14" />
                <FolderOpenOutlined v-else />
              </template>
            </a-tree>
          </a-spin>
        </div>
        <div class="category-footer">
          <a-button type="link" size="small" :disabled="!selectedCategoryId" @click="editSelectedCategory">
            编辑
          </a-button>
          <a-button type="link" size="small" danger :disabled="!selectedCategoryId" @click="confirmDeleteCategory">删除</a-button>
        </div>
      </div>

      <!-- ========== 右侧产品列表 ========== -->
      <div class="product-panel">
        <!-- 统计卡片 -->
        <a-row :gutter="12" style="margin-bottom: 12px;">
          <a-col :span="8">
            <div class="stat-card" style="border-top: 3px solid #1890ff;">
              <div class="stat-value" style="color:#1890ff">{{ statistics.total }}</div>
              <div class="stat-label">产品总数</div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="stat-card" style="border-top: 3px solid #52c41a;">
              <div class="stat-value" style="color:#52c41a">{{ statistics.enabled }}</div>
              <div class="stat-label">启用</div>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="stat-card" style="border-top: 3px solid #ff4d4f;">
              <div class="stat-value" style="color:#ff4d4f">{{ statistics.disabled }}</div>
              <div class="stat-label">停用</div>
            </div>
          </a-col>
        </a-row>

        <div class="product-toolbar">
          <SearchBar
            :fields="searchFields"
            :loading="loading"
            @search="handleSearch"
            @reset="handleReset"
          />
          <a-space>
            <PrintButton page-code="erp/product" button-size="small" button-type="default" />
            <a-button v-permission="'erp:product:create'" type="primary" size="small" @click="router.push('/erp/product/create')">
              <PlusOutlined /> 新增产品
            </a-button>
            <a-button size="small" @click="router.push('/erp/product/price-batch')">
              <DollarOutlined /> 批量价格
            </a-button>
            <a-button size="small" @click="router.push('/erp/product/inventory-mode')">
              <SettingOutlined /> 库存模式
            </a-button>
          </a-space>
        </div>

        <VxeTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="products"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          :show-toolbar="false"
          :show-add="false"
          :show-search="false"
          :show-batch-delete="false"
          @page-change="onPageChange"
          @selection-change="onSelectionChange"
          @batch-delete="onBatchDelete"
        >
          <template #batch-actions="{ selectedRows }">
            <a-button v-permission="'erp:product:status'" size="small" @click="batchEnable(selectedRows)">
              <CheckCircleOutlined /> 批量启用
            </a-button>
            <a-button v-permission="'erp:product:status'" size="small" @click="batchDisable(selectedRows)">
              <StopOutlined /> 批量停用
            </a-button>
            <a-button v-permission="'erp:product:delete'" danger size="small" @click="batchDelete(selectedRows)">
              <DeleteOutlined /> 批量删除
            </a-button>
          </template>
          <template #empty>
            <EmptyState v-if="loading" image="no-data" title="加载中..." description="" :show-actions="false" size="small" />
            <EmptyState v-else image="no-data" title="暂无产品" description="当前没有产品数据" add-text="新增产品" size="small" @refresh="fetchProducts" @add="() => router.push('/erp/product/create')" />
          </template>
          <template #imageCell="{ record }">
            <a-image
              v-if="record.imageUrl"
              :src="record.imageUrl"
              style="width: 32px; height: 32px; border-radius: 4px; object-fit: cover;"
              fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
            />
            <span v-else class="no-image">-</span>
          </template>
          <template #statusCell="{ record }">
            <StatusTag :status="record.status" :map="PRODUCT_STATUS" />
          </template>
          <template #productTypeCell="{ record }">
            <a-tag>{{ productTypeLabel(record.productType) }}</a-tag>
          </template>
          <template #action="{ record }">
            <a-space :size="4">
              <a-button v-permission="'erp:product:view'" type="link" size="small" @click="viewProduct(record.id)">查看</a-button>
              <a-button v-permission="'erp:product:edit'" type="link" size="small" @click="router.push(`/erp/product/${record.id}`)">编辑</a-button>
              <a-button v-permission="'erp:product:delete'" type="link" size="small" danger @click="confirmDeleteProduct(record)">删除</a-button>
              <a-button v-permission="'erp:product:status'" type="link" size="small" @click="toggleStatus(record)">
                {{ record.status === 'ENABLED' ? '停用' : '启用' }}
              </a-button>
            </a-space>
          </template>
        </VxeTableList>
      </div>
    </div>

    <!-- 分类新增/编辑弹窗 -->
    <a-modal
      v-model:open="categoryModalVisible"
      :title="editingCategory ? '编辑分类' : '新增分类'"
      :confirm-loading="categoryModalLoading"
      width="500px"
      @ok="debounceClick('categoryOk', handleCategoryOk)"
    >
      <a-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="分类名称" name="categoryName">
          <a-input v-model:value="categoryForm.categoryName" placeholder="请输入分类名称" size="small" />
        </a-form-item>
        <a-form-item label="分类编码" name="categoryCode">
          <a-input v-model:value="categoryForm.categoryCode" placeholder="请输入分类编码" size="small" />
        </a-form-item>
        <a-form-item label="上级分类">
          <a-tree-select
            v-model:value="categoryForm.parentId"
            :tree-data="categoryTreeData"
            :replace-fields="{ children: 'children', label: 'categoryName', value: 'id' }"
            placeholder="无(根节点)"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="categoryForm.sortOrder" :min="0" size="small" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive, defineExpose } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TreeProps } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, FolderOutlined, FolderOpenOutlined, DollarOutlined, SettingOutlined,
  SyncOutlined, DownloadOutlined, CheckCircleOutlined, StopOutlined, DeleteOutlined
} from '@ant-design/icons-vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { PRODUCT_STATUS } from '@/utils/statusConfig'
import { productApi, productCategoryApi } from '@/api/erp/product'
import type { ProductCategory } from '@/api/erp/product'
import request from '@/utils/request'

const router = useRouter()

// ── 类型定义 ──────────────────────────────────────────
interface ProductItem {
  id: number
  productCode: string
  productName: string
  spec: string
  sku: string
  unit: string
  categoryName: string
  gradeName: string
  standardPrice: number
  productType: string
  status: string
  imageUrl?: string
}

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 分类管理 ──
const categoryLoading = ref(false)
const categoryTree = ref<ProductCategory[]>([])
const selectedCategoryId = ref<number>(0)
const expandedKeys = ref<number[]>([])
const categorySearch = ref('')

const categoryTreeData = computed(() => {
  if (!categorySearch.value) return categoryTree.value
  return filterTree(categoryTree.value, categorySearch.value)
})

function filterTree(tree: ProductCategory[], keyword: string): ProductCategory[] {
  return tree
    .map(node => ({
      ...node,
      children: node.children ? filterTree(node.children, keyword) : []
    }))
    .filter(node =>
      node.categoryName.includes(keyword) ||
      (node.children && node.children.length > 0)
    )
}

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    categoryTree.value = await productCategoryApi.getTree()
    // 展开第一层
    const firstLevel = categoryTree.value.map(n => n.id)
    if (firstLevel.length > 0) {
      expandedKeys.value = [...firstLevel]
    }
  } catch (e) {
    console.error('[产品分类] 加载分类树失败', e)
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: number[]) {
  selectedCategoryId.value = keys[0] || 0
  fetchProducts()
}

function onExpand(keys: number[]) {
  expandedKeys.value = keys
}

// ── 分类弹窗 ──
const categoryModalVisible = ref(false)
const categoryModalLoading = ref(false)
const editingCategory = ref<ProductCategory | null>(null)
const categoryFormRef = ref<FormInstance>()
const categoryForm = reactive({
  categoryName: '',
  categoryCode: '',
  parentId: undefined as number | undefined,
  sortOrder: 0
})
const categoryRules = {
  categoryName: [{ required: true, message: '请输入分类名称' }],
  categoryCode: [{ required: true, message: '请输入分类编码' }]
}

function showCategoryModal(category: ProductCategory | null) {
  editingCategory.value = category
  if (category) {
    Object.assign(categoryForm, {
      categoryName: category.categoryName,
      categoryCode: category.categoryCode,
      parentId: category.parentId || undefined,
      sortOrder: category.sortOrder || 0
    })
  } else {
    Object.assign(categoryForm, {
      categoryName: '',
      categoryCode: '',
      parentId: selectedCategoryId.value > 0 ? selectedCategoryId.value : undefined,
      sortOrder: 0
    })
  }
  categoryModalVisible.value = true
}

function editSelectedCategory() {
  if (!selectedCategoryId.value) return
  const findCat = (list: ProductCategory[]): ProductCategory | null => {
    for (const c of list) {
      if (c.id === selectedCategoryId.value) return c
      if (c.children) {
        const found = findCat(c.children)
        if (found) return found
      }
    }
    return null
  }
  const cat = findCat(categoryTree.value)
  if (cat) showCategoryModal(cat)
}

async function handleCategoryOk() {
  try {
    await categoryFormRef.value?.validate()
    categoryModalLoading.value = true
    const catPayload: Partial<import('@/api/erp/product').ProductCategory> = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId,
      sortOrder: categoryForm.sortOrder
    }
    if (editingCategory.value) {
      await productCategoryApi.update(editingCategory.value.id, catPayload)
      message.success('更新成功')
    } else {
      await productCategoryApi.create(catPayload)
      message.success('新增成功')
    }
    categoryModalVisible.value = false
    await fetchCategoryTree()
    await fetchProducts()
  } catch (e: unknown) {
    if (e && typeof e === 'object' && 'errorFields' in e) return
    const msg = e instanceof Error ? e.message : '操作失败'
    message.error(msg)
  } finally {
    categoryModalLoading.value = false
  }
}

async function deleteSelectedCategory() {
  if (!selectedCategoryId.value) return
  try {
    await productCategoryApi.delete(selectedCategoryId.value)
    message.success('删除成功')
    selectedCategoryId.value = 0
    await fetchCategoryTree()
    await fetchProducts()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '删除失败'
    message.error(msg)
  }
}

// ── 自动刷新 ──
const autoRefreshCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

// ── 产品列表 ──
const loading = ref(false)
const lastUpdateTime = ref('')
const products = ref<ProductItem[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')

const statistics = ref({ total: 0, enabled: 0, disabled: 0 })

const searchFields: SearchField[] = [
  { name: 'keyword', label: '关键词', type: 'input', placeholder: '编码/名称/规格' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '全部', value: '' },
    { label: '启用', value: 'ENABLED' },
    { label: '停用', value: 'DISABLED' }
  ]}
]

function handleError(err: any) { console.warn("[产品管理] ErrorBoundary 捕获异常:", err) }
const tableRef = ref()
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100']
})

const vxeColumns = computed(() => [
  { field: 'productCode', title: '产品编码', width: 120 },
  { field: 'productName', title: '产品名称', width: 160, minWidth: 120 },
  { field: 'imageUrl', title: '图片', width: 56, align: 'center', slots: { default: 'imageCell' } },
  { field: 'spec', title: '规格', width: 120 },
  { field: 'sku', title: 'SKU', width: 120 },
  { field: 'unit', title: '单位', width: 60, align: 'center' },
  { field: 'categoryName', title: '分类', width: 100 },
  { field: 'gradeName', title: '产品等级', width: 100 },
  { field: 'standardPrice', title: '标准售价', width: 100, align: 'right' },
  { field: 'productType', title: '类型', width: 80, align: 'center', slots: { default: 'productTypeCell' } },
  { field: 'status', title: '状态', width: 70, align: 'center', slots: { default: 'statusCell' } },
  { field: '_action', title: '操作', width: 210, fixed: 'right', slots: { default: 'action' } }
])

function productTypeLabel(type: string) {
  const map: Record<string, string> = { SINGLE: '单品', KIT: '套件', SERVICE: '服务' }
  return map[type] || type
}

async function fetchProducts() {
  loading.value = true
  try {
    const res = await productApi.page({
      categoryId: selectedCategoryId.value > 0 ? selectedCategoryId.value : undefined,
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    products.value = res.records || []
    pagination.total = res.total || 0
    // 统计（总数来自 API，启用/停用来自当前页面数据）
    const stats = { total: res.total || 0, enabled: 0, disabled: 0 }
    products.value.forEach((p) => {
      if (p.status === 'ENABLED') stats.enabled++
      else if (p.status === 'DISABLED') stats.disabled++
    })
    statistics.value = stats
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (e) {
    console.error('[产品管理] 加载产品列表失败', e)
    message.error('加载产品列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch(formData?: Record<string, any>) {
  if (formData) {
    searchKeyword.value = formData.keyword || ''
    statusFilter.value = formData.status || ''
  }
  pagination.current = 1
  fetchProducts()
}

function handleReset() {
  searchKeyword.value = ''
  statusFilter.value = ''
  pagination.current = 1
  fetchProducts()
}

function onPageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchProducts()
}

function viewProduct(id: number) {
  router.push(`/erp/product/${id}`)
}

async function deleteProduct(id: number) {
  try {
    await productApi.delete(id)
    message.success('删除成功')
    await fetchProducts()
  } catch {
    message.error('删除失败')
  }
}

async function toggleStatus(record: ProductItem) {
  const newStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await productApi.updateStatus(record.id, newStatus)
    message.success(newStatus === 'ENABLED' ? '已启用' : '已停用')
    await fetchProducts()
  } catch {
    message.error('操作失败')
  }
}

function refreshAll() {
  fetchCategoryTree()
  fetchProducts()
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/product/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `产品_${new Date().toISOString().slice(0, 10)}.xlsx`; a.click()
    window.URL.revokeObjectURL(url); message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

function confirmDeleteCategory() {
  if (!selectedCategoryId.value) return
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除此产品分类吗？如果分类下有产品，删除可能会失败。',
    okText: '确认删除',
    okType: 'danger',
    onOk: deleteSelectedCategory
  })
}

function confirmDeleteProduct(record: ProductItem) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除产品 "${record.productName || record.productCode}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: () => deleteProduct(record.id)
  })
}

// ── 批量操作 ──────────────────────────────────────────
const selectedIds = ref<number[]>([])

function onSelectionChange(rows: any[], ids: number[]) {
  selectedIds.value = ids
}

async function onBatchDelete(ids: number[]) {
  if (!ids.length) return
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${ids.length} 个产品吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(ids.map(id => productApi.delete(id)))
        message.success(`成功删除 ${ids.length} 个产品`)
        await fetchProducts()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

async function batchDelete(rows: any[]) {
  const ids = rows.map(r => r.id)
  await onBatchDelete(ids)
}

async function batchEnable(rows: any[]) {
  try {
    await Promise.all(rows.map(r => productApi.updateStatus(r.id, 'ENABLED')))
    message.success(`已启用 ${rows.length} 个产品`)
    await fetchProducts()
  } catch {
    message.error('批量启用失败')
  }
}

async function batchDisable(rows: any[]) {
  try {
    await Promise.all(rows.map(r => productApi.updateStatus(r.id, 'DISABLED')))
    message.success(`已停用 ${rows.length} 个产品`)
    await fetchProducts()
  } catch {
    message.error('批量停用失败')
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', refreshAll)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    if (categoryTree.value.length > 0) {
      router.push('/erp/product/create')
    }
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    debounceClick('export', handleExport)
  }
}

onMounted(() => {
  fetchCategoryTree()
  fetchProducts()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchCategoryTree()
    fetchProducts()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

defineExpose({ fetchData: fetchProducts })
</script>

<style scoped>
.product-main {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
}

/* ── 左侧分类面板 ── */
.category-panel {
  width: 260px;
  min-width: 200px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.category-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.category-search {
  padding: 8px 12px;
}

.category-tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.category-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
}

.cat-count {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

/* ── 右侧产品面板 ── */
.product-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.product-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px 0;
}

.no-image {
  color: #ccc;
}

/* ── 通用页面头部 ── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.page-header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.page-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
}

/* ── 紧凑尺寸 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
