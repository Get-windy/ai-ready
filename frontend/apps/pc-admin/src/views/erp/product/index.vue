<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="true"
        category-title="商品分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :tabs="tabs"
        :active-tab="activeTab"
        :pagination-current="pagination.current"
        :pagination-page-size="pagination.pageSize"
        :pagination-total="pagination.total"
        current-path="当前路径：全部商品"
        @category-add="showCategoryModal(null)"
        @category-retry="fetchCategoryTree"
        @category-select="onCategorySelect"
        @category-expand="onExpand"
        @tab-change="(key: string) => activeTab = key"
        @search="handleSearch"
        @page-change="handlePageChange"
      >
        <!-- 工具栏左侧 -->
        <template #toolbar-left>
          <a-space>
            <a-button type="primary" class="btn-add" @click="handleAdd">
              <PlusOutlined /> 新增
            </a-button>
          </a-space>
        </template>

        <!-- 工具栏右侧 -->
        <template #toolbar-right>
          <a-space>
            <a-button size="small" @click="refreshAll">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleBatchPrice">批量改价</a-menu-item>
                  <a-menu-item @click="handleBatchStatus">批量改状态</a-menu-item>
                  <a-menu-item @click="handleBatchDelete">批量删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- 搜索字段 -->
        <template #search-fields>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="商品名称/货号/条码/规格"
                size="small"
                style="width: 200px"
                allow-clear
              />
            </div>
            <div class="search-item">
              <span class="search-label">品牌</span>
              <a-input
                v-model:value="searchForm.brand"
                placeholder=""
                size="small"
                style="width: 120px"
                allow-clear
              />
            </div>
            <div class="search-item">
              <span class="search-label">新增日期（起）</span>
              <a-date-picker
                v-model:value="searchForm.createTimeStart"
                size="small"
                style="width: 140px"
                placeholder=""
              />
            </div>
            <div class="search-item">
              <span class="search-label">新增日期（止）</span>
              <a-date-picker
                v-model:value="searchForm.createTimeEnd"
                size="small"
                style="width: 140px"
                placeholder=""
              />
            </div>
            <div class="search-item">
              <span class="search-label">显示状态</span>
              <a-select
                v-model:value="searchForm.status"
                size="small"
                style="width: 120px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option value="ENABLED">已启用</a-select-option>
                <a-select-option value="DISABLED">已停用</a-select-option>
              </a-select>
            </div>
            <div class="search-item">
              <span class="search-label">使用优惠券</span>
              <a-select
                v-model:value="searchForm.useCoupon"
                size="small"
                style="width: 100px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option :value="1">是</a-select-option>
                <a-select-option :value="0">否</a-select-option>
              </a-select>
            </div>
            <div class="search-item">
              <span class="search-label">是否标品</span>
              <a-select
                v-model:value="searchForm.isStandardProduct"
                size="small"
                style="width: 100px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option :value="1">是</a-select-option>
                <a-select-option :value="0">否</a-select-option>
              </a-select>
            </div>
          </div>
          <div class="search-row second-row">
            <div class="search-item">
              <span class="search-label">所属行业类别</span>
              <a-select
                v-model:value="searchForm.industryCategory"
                size="small"
                style="width: 140px"
                placeholder="全部"
                allow-clear
              >
                <a-select-option value="">全部</a-select-option>
                <a-select-option v-for="item in industryCategoryOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </div>
            <a-button type="primary" size="small" class="btn-search" @click="handleSearch">
              查询
            </a-button>
            <a-checkbox v-model:checked="showHierarchy" size="small" style="margin-left: 12px">
              显示层次结构
            </a-checkbox>
          </div>
        </template>

        <!-- 表格 -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="detailColumns"
            :data-source="tableData"
            :loading="loading"
            :view-mode="true"
            @checkbox-change="handleCheckboxChange"
            @checkbox-all="handleCheckboxAll"
          >
            <!-- 自定义：操作列 -->
            <template #actionCell="{ record, index }">
              <a-space :size="0">
                <a-button type="link" size="small" @click="handleEdit(record)">修改</a-button>
                <a-button type="link" size="small" @click="handleToggleStatus(record)">
                  {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                </a-button>
                <a-dropdown>
                  <a-button type="link" size="small">更多</a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleView(record)">查看详情</a-menu-item>
                      <a-menu-item @click="handleCopy(record)">复制</a-menu-item>
                      <a-menu-item danger @click="handleDelete(record)">删除</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>

            <!-- 自定义：图片列 -->
            <template #imageCell="{ record }">
              <a-image
                v-if="record.imageUrl"
                :src="record.imageUrl"
                style="width: 36px; height: 36px; border-radius: 4px; object-fit: cover;"
                fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
              />
              <span v-else class="no-image">-</span>
            </template>

            <!-- 自定义：商品名称列（可点击） -->
            <template #productNameCell="{ record }">
              <a class="cell-link" @click="handleView(record)">{{ record.productName }}</a>
            </template>
          </BillDetailTable>
        </template>
      </CategoryListLayout>

      <!-- 分类新增/编辑弹窗 -->
      <FullScreenDetail
        :visible="categoryModalVisible"
        :title="editingCategory ? '编辑分类' : '新增分类'"
        :save-loading="categoryModalLoading"
        @close="handleCategoryCancel"
        @save="handleCategoryOk"
      >
        <a-form
          ref="categoryFormRef"
          :model="categoryForm"
          :rules="categoryRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
        >
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
              :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
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
      </FullScreenDetail>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  InboxOutlined,
  UploadOutlined,
  CloudUploadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { productApi, productCategoryApi } from '@/api/erp/product'
import type { ProductCategory } from '@/api/erp/product'
import request from '@/utils/request'

const router = useRouter()

// ── Tab标签 ──
const tabs = [
  { key: 'all', label: '全部商品' },
  { key: 'package', label: '套餐' },
  { key: 'shelf', label: '商品上架' },
  { key: 'auth', label: '商品授权' },
]
const activeTab = ref('all')

// ── 行业类别选项 ──
const industryCategoryOptions = ref([
  { label: '食品', value: '食品' },
  { label: '饮料', value: '饮料' },
  { label: '日用品', value: '日用品' },
  { label: '电子产品', value: '电子产品' },
  { label: '服装', value: '服装' },
  { label: '其他', value: '其他' },
])

// ── 分类管理 ──
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<number>(0)
const expandedKeys = ref<number[]>([])

const categoryTreeData = computed(() => categoryTree.value)

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(data) ? data : []
    const firstLevel = categoryTree.value.map(n => n.id)
    if (firstLevel.length > 0) {
      expandedKeys.value = [...firstLevel]
    }
  } catch (e) {
    console.error('[商品分类] 加载分类树失败', e)
    categoryError.value = true
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
const categoryFormRef = ref<any>(null)
const categoryForm = reactive({
  categoryName: '',
  categoryCode: '',
  parentId: undefined as number | undefined,
  sortOrder: 0,
})
const categoryRules = {
  categoryName: [{ required: true, message: '请输入分类名称' }],
  categoryCode: [{ required: true, message: '请输入分类编码' }],
}

function showCategoryModal(category: ProductCategory | null) {
  editingCategory.value = category
  if (category) {
    Object.assign(categoryForm, {
      categoryName: category.categoryName,
      categoryCode: category.categoryCode,
      parentId: category.parentId || undefined,
      sortOrder: category.sortOrder || 0,
    })
  } else {
    Object.assign(categoryForm, {
      categoryName: '',
      categoryCode: '',
      parentId: selectedCategoryId.value > 0 ? selectedCategoryId.value : undefined,
      sortOrder: 0,
    })
  }
  categoryModalVisible.value = true
}

async function handleCategoryOk() {
  try {
    await categoryFormRef.value?.validate()
    categoryModalLoading.value = true
    const catPayload: Partial<ProductCategory> = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId,
      sortOrder: categoryForm.sortOrder,
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

function handleCategoryCancel() {
  categoryModalVisible.value = false
}

// ── 搜索表单 ──
const searchForm = reactive({
  keyword: '',
  brand: '',
  createTimeStart: null,
  createTimeEnd: null,
  status: '',
  useCoupon: '',
  isStandardProduct: '',
  industryCategory: '',
})
const showHierarchy = ref(false)

// ── 商品列表 ──
const loading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const tableRef = ref<any>(null)

// 表格列配置
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60 },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200, sortable: true },
  { key: 'productCodeAlias', title: '商品货号', type: 'input', width: 120, sortable: true },
  { key: 'categoryName', title: '分类', type: 'input', width: 110, sortable: true },
  { key: 'gradeName', title: '等级', type: 'input', width: 80 },
  { key: 'industryCategory', title: '所属行业类别', type: 'input', width: 120, sortable: true },
  { key: 'barcode', title: '条码', type: 'input', width: 140, sortable: true },
  { key: 'spec', title: '规格', type: 'input', width: 120, sortable: true },
  { key: 'model', title: '型号', type: 'input', width: 100, sortable: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, sortable: true },
])

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

async function fetchProducts() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (selectedCategoryId.value > 0) {
      params.categoryId = selectedCategoryId.value
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    if (searchForm.status) {
      params.status = searchForm.status
    }
    if (searchForm.brand) {
      params.brand = searchForm.brand
    }
    if (searchForm.industryCategory) {
      params.industryCategory = searchForm.industryCategory
    }
    if (searchForm.createTimeStart) {
      params.createTimeStart = searchForm.createTimeStart.format('YYYY-MM-DD')
    }
    if (searchForm.createTimeEnd) {
      params.createTimeEnd = searchForm.createTimeEnd.format('YYYY-MM-DD')
    }
    if (searchForm.useCoupon !== '' && searchForm.useCoupon !== null && searchForm.useCoupon !== undefined) {
      params.useCoupon = Number(searchForm.useCoupon)
    }
    if (searchForm.isStandardProduct !== '' && searchForm.isStandardProduct !== null && searchForm.isStandardProduct !== undefined) {
      params.isStandardProduct = Number(searchForm.isStandardProduct)
    }

    const res = await productApi.page(params)
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error('[商品列表] 加载失败', e)
    message.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchProducts()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchProducts()
}

function handleCheckboxChange(record: any, _rowIndex: number, _checked: boolean) {
  // BillDetailTable 内部管理选中状态，通过 tableRef 获取
  selectedRows.value = tableRef.value?.getCheckedRecords?.() || []
}

function handleCheckboxAll(_checked: boolean, records: any[]) {
  selectedRows.value = records
}

// ── 操作按钮 ──
function handleAdd() {
  router.push('/erp/product/create')
}

function handleEdit(row: any) {
  router.push(`/erp/product/form/${row.id}`)
}

function handleView(row: any) {
  router.push(`/erp/product/form/${row.id}`)
}

function handleToggleStatus(row: any) {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  Modal.confirm({
    title: '确认',
    content: `确定要${newStatus === 'ENABLED' ? '启用' : '停用'}该商品吗？`,
    onOk: async () => {
      try {
        await productApi.updateStatus(row.id, newStatus)
        message.success(newStatus === 'ENABLED' ? '已启用' : '已停用')
        fetchProducts()
      } catch {
        message.error('操作失败')
      }
    },
  })
}

function handleDelete(row: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品 "${row.productName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productApi.delete(row.id)
        message.success('删除成功')
        fetchProducts()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

function handleCopy(row: any) {
  router.push(`/erp/product/create?copyFrom=${row.id}`)
}

function handleImport() {
  message.info('导入功能开发中')
}

function handleCloudImport() {
  message.info('云导入功能开发中')
}

function handlePrint() {
  message.info('打印功能开发中')
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/product/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商品_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

function handleBatchPrice() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择商品')
    return
  }
  router.push('/erp/product/price-batch')
}

function handleBatchStatus() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择商品')
    return
  }
  Modal.confirm({
    title: '批量改状态',
    content: `确定要将选中的 ${selectedRows.value.length} 个商品切换启用/停用状态吗？`,
    onOk: async () => {
      try {
        await Promise.all(selectedRows.value.map((row: any) => {
          const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
          return productApi.updateStatus(row.id, newStatus)
        }))
        message.success('批量改状态成功')
        fetchProducts()
      } catch {
        message.error('批量改状态失败')
      }
    },
  })
}

function handleBatchDelete() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择商品')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 个商品吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(selectedRows.value.map((row: any) => productApi.delete(row.id)))
        message.success('批量删除成功')
        fetchProducts()
      } catch {
        message.error('批量删除失败')
      }
    },
  })
}

function refreshAll() {
  fetchCategoryTree()
  fetchProducts()
}

function handleError(err: any) {
  console.warn('[商品列表] ErrorBoundary 捕获异常:', err)
}

// ─ 键盘快捷键 ──
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    refreshAll()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
}

onMounted(() => {
  fetchCategoryTree()
  fetchProducts()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ── 橙色新增按钮 ── */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* ── 表格样式 ── */
.no-image {
  color: #ccc;
}

.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}

/* ── 搜索栏布局 ── */
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row.second-row {
  margin-top: 8px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 8px;
}

/* ── BillDetailTable 样式覆盖 ── */
:deep(.ss-grid th) {
  background: #fafafa !important;
  font-weight: 600 !important;
  color: #333 !important;
}

:deep(.ss-row:hover td) {
  background: #e6f7ff !important;
}
</style>
