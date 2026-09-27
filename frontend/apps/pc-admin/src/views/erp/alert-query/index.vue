<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="商品分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentCategoryPath"
        :show-table-footer="true"
        @category-retry="fetchCategoryTree"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：仓库(单选) + 商品 + 品牌 + 预警类型 + 查询 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <a-radio-group
                  v-model:value="warehouseMode"
                  size="small"
                  @change="onWarehouseModeChange"
                >
                  <a-radio value="all">全部仓库</a-radio>
                  <a-radio value="specific">指定仓库</a-radio>
                </a-radio-group>
              </div>
              <div
                v-if="warehouseMode === 'specific'"
                class="search-item"
              >
                <a-select
                  v-model:value="searchParams.warehouseId"
                  style="width: 160px"
                  size="small"
                  allow-clear
                  placeholder="选择仓库"
                  :options="warehouseOptions"
                />
              </div>
              <div class="search-item">
                <a-input
                  v-model:value="searchParams.productKeyword"
                  placeholder="商品"
                  style="width: 180px"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <a-input
                  v-model:value="searchParams.brand"
                  placeholder="品牌"
                  style="width: 140px"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">预警类型</span>
                <a-select
                  v-model:value="searchParams.alertType"
                  style="width: 120px"
                  size="small"
                  @change="handleSearch"
                >
                  <a-select-option value="">全部</a-select-option>
                  <a-select-option value="LOW_STOCK">下限预警</a-select-option>
                  <a-select-option value="OVER_STOCK">上限预警</a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >查询</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="defaultColumns"
              :data-source="tableData"
              :storage-key="'alert-query-table-columns'"
              :loading="loading"
              :pagination="pagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :summary-columns="footerColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #productNameCell="{ record }">
                <span class="cell-link">{{ record.productName }}</span>
              </template>
              <template #maxStockCell="{ record }">
                <span class="num-value">{{ formatQty(record.maxStock) }}</span>
              </template>
              <template #minStockCell="{ record }">
                <span class="num-value">{{ formatQty(record.minStock) }}</span>
              </template>
              <template #bookQtyCell="{ record }">
                <span
                  :class="['num-value', record.bookQty < record.minStock ? 'text-danger' : 'text-warning']"
                >{{ formatQty(record.bookQty) }}</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="erp-alert-query"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, PrinterOutlined, ExportOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { stockAlertQueryApi } from '@/api/erp/stockAlert'
import { productCategoryApi } from '@/api/erp/product'
import request from '@/utils/request'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

// ═══ 仓库 ═══
const warehouseMode = ref('all')
const warehouseOptions = ref<any[]>([])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  warehouseId: undefined as string | number | undefined,
  productKeyword: '',
  brand: '',
  alertType: '',
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 列定义（列配置走数据表表头齿轮） ═══
const defaultColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '商品名称', field: 'productName', key: 'productName', width: 180, type: 'slot', slotName: 'productNameCell' },
  { title: '货号', field: 'productCode', key: 'productCode', width: 110 },
  { title: '口味', field: 'taste', key: 'taste', width: 80 },
  { title: '型号', field: 'model', key: 'model', width: 90 },
  { title: '单位', field: 'unit', key: 'unit', width: 70 },
  { title: '预警仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '库存上限', field: 'maxStock', key: 'maxStock', width: 100, align: 'right', type: 'slot', slotName: 'maxStockCell' },
  { title: '库存下限', field: 'minStock', key: 'minStock', width: 100, align: 'right', type: 'slot', slotName: 'minStockCell' },
  { title: '说明', field: 'remark', key: 'remark', width: 160 },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 80, defaultHidden: true },
  { title: '条码', field: 'barcode', key: 'barcode', width: 120, defaultHidden: true },
  { title: '规格', field: 'spec', key: 'spec', width: 110, defaultHidden: true },
  { title: '产地', field: 'origin', key: 'origin', width: 90, defaultHidden: true },
  { title: '品牌', field: 'brand', key: 'brand', width: 100, defaultHidden: true },
  { title: '账面库存', field: 'bookQty', key: 'bookQty', width: 100, align: 'right', type: 'slot', slotName: 'bookQtyCell', defaultHidden: true },
]

// ═══ 分类树 ═══
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref<string>('0')
const expandedKeys = ref<string[]>([])
const categoryTreeData = computed(() => categoryTree.value)

const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === '0' || !categoryTree.value.length) return '全部商品'
  const path: string[] = []
  function find(nodes: any[], target: string): boolean {
    for (const node of nodes) {
      path.push(node.categoryName)
      if (String(node.id) === target) return true
      if (node.children?.length && find(node.children, target)) return true
      path.pop()
    }
    return false
  }
  find(categoryTree.value, selectedCategoryId.value)
  return path.length ? path.join(' / ') : '全部商品'
})

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(data) ? data : []
    const firstLevel = categoryTree.value.map(n => String(n.id))
    if (firstLevel.length > 0) expandedKeys.value = [...new Set([...firstLevel, ...expandedKeys.value])]
  } catch (e) {
    console.warn('[预警查询] 加载分类树失败', e)
    categoryError.value = true
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys[0]
  selectedCategoryId.value = key != null ? String(key) : '0'
  pagination.current = 1
  fetchData()
}

// ═══ 仓库/品牌选项加载 ═══
async function loadWarehouses() {
  try {
    const res: any = await request.get('/erp/stock/warehouses')
    const list = res?.data || res || []
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName || w.name,
      value: w.id,
    }))
  } catch (e) {
    console.warn('[预警查询] 加载仓库失败', e)
    warehouseOptions.value = []
  }
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (warehouseMode.value === 'specific' && searchParams.warehouseId) params.warehouseId = searchParams.warehouseId
    if (searchParams.productKeyword) params.productKeyword = searchParams.productKeyword
    if (searchParams.brand) params.brand = searchParams.brand
    if (searchParams.alertType) params.alertType = searchParams.alertType
    if (selectedCategoryId.value !== '0') params.categoryId = selectedCategoryId.value

    const res: any = await stockAlertQueryApi.page(params)
    const body = res?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    console.warn('[预警查询] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleRefresh() { fetchData() }

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function onWarehouseModeChange() {
  if (warehouseMode.value === 'all') searchParams.warehouseId = undefined
  handleSearch()
}

// ═══ 工具栏操作 ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint: handlePrintF8 } = useListPrint({
  pageCode: 'erp-alert-query',
  title: '商品分类',
  columns: () => defaultColumns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (warehouseMode.value === 'specific' && searchParams.warehouseId) params.warehouseId = searchParams.warehouseId
    if (searchParams.productKeyword) params.productKeyword = searchParams.productKeyword
    if (searchParams.brand) params.brand = searchParams.brand
    if (searchParams.alertType) params.alertType = searchParams.alertType
    if (selectedCategoryId.value !== '0') params.categoryId = selectedCategoryId.value

    const res: any = await stockAlertQueryApi.page(params)
    const body = res?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['商品名称', '货号', '型号', '单位', '预警仓库', '库存上限', '库存下限', '账面库存', '品牌', '说明']
    const rows = data.map((r: any) => [
      r.productName, r.productCode, r.model, r.unit, r.warehouseName,
      r.maxStock, r.minStock, r.bookQty, r.brand, r.remark,
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预警查询_${new Date().toISOString().slice(0, 19).replace(/[-T:]/g, '')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ═══ 合计 ═══
const footerColumns = computed(() => {
  const maxStock = tableData.value.reduce((s: number, r: any) => s + (Number(r.maxStock) || 0), 0)
  const minStock = tableData.value.reduce((s: number, r: any) => s + (Number(r.minStock) || 0), 0)
  const bookQty = tableData.value.reduce((s: number, r: any) => s + (Number(r.bookQty) || 0), 0)
  return [
    { key: 'maxStock', value: maxStock, highlight: true },
    { key: 'minStock', value: minStock, highlight: true },
    { key: 'bookQty', value: bookQty, highlight: true },
  ]
})

// ═══ 工具 ═══
function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function handleError(error: Error) {
  console.error('[预警查询] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadWarehouses()
  fetchCategoryTree()
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-input), .search-item :deep(.ant-select) { font-size: 13px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1668dc; }
.num-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
.text-danger { color: #f5222d; font-weight: 600; }
.text-warning { color: #faad14; font-weight: 600; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
