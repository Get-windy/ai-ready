<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="true"
        category-title="商品分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeysTree"
        :show-table-footer="true"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeysTree = keys)"
      >
        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              type="primary"
              :disabled="selectedRows.length === 0"
              style="background: #fa541c; border-color: #fa541c"
              @click="handlePurchase"
            >
              <ShoppingCartOutlined /> 采购
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 + 说明条 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="hint-bar">
              <span class="hint-text">计划采购数量 = 备货天数 ✕ 日均销量 + 待发货数量 - 待收货数量 - 账面库存</span>
            </div>
            <div class="search-grid">
              <!-- 销售日期 -->
              <div class="search-field-item">
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDateRangeChange"
                />
              </div>
              <!-- 备货天数 -->
              <div class="search-field-item">
                <a-input-number
                  v-model:value="searchParams.stockDays"
                  size="small"
                  :min="0"
                  placeholder="备货天数"
                  style="width: 100%"
                  @change="handleSearch"
                />
              </div>
              <!-- 商品 -->
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.productKeyword"
                  placeholder="商品"
                  allow-clear
                  size="small"
                  @press-enter="handleSearch"
                />
              </div>
              <!-- 仓库 -->
              <div class="search-field-item">
                <a-select
                  v-model:value="searchParams.warehouseId"
                  placeholder="仓库"
                  allow-clear
                  size="small"
                  :options="warehouseOptions"
                  @change="handleSearch"
                />
              </div>
              <!-- 供应商 -->
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.supplierName"
                  placeholder="供应商"
                  allow-clear
                  size="small"
                  @press-enter="handleSearch"
                />
              </div>
              <!-- 计划采购数量 >= -->
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">计划采购数量</span>
                  <a-select
                    v-model:value="minPlanOp"
                    size="small"
                    style="width: 52px"
                    @change="handleSearch"
                  >
                    <a-select-option value=">=">≥</a-select-option>
                  </a-select>
                  <a-input-number
                    v-model:value="searchParams.minPlanQty"
                    size="small"
                    :min="0"
                    :precision="2"
                    style="flex: 1"
                    @change="handleSearch"
                  />
                </div>
              </div>
              <!-- 查询按钮 -->
              <div class="search-field-item search-action-item">
                <a-button type="primary" size="small" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="columns"
            :storage-key="'purchase-smart-replenish-table-columns'"
            :data-source="tableData"
            :loading="loading"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="true"
            row-key="id"
            @page-change="handlePageChange"
            @selection-change="handleSelectionChange"
          >
            <template #imageCell="{ record }">
              <a-avatar
                shape="square"
                :size="36"
                :src="record.image || undefined"
              >
                <template #icon>
                  <PictureOutlined />
                </template>
              </a-avatar>
            </template>
            <template #planPurchaseCell="{ record }">
              <span class="plan-qty">{{ formatQty(record.planPurchaseQty) }}</span>
            </template>
          </BillTableList>
        </template>

        <!-- ═══ 表格底部：合计 ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <div class="footer-label">合计</div>
            <div class="footer-values">
              <span v-if="summaryData.salesQty">销售数量: {{ summaryData.salesQty }}</span>
              <span v-if="summaryData.salesAmount">销售金额: {{ summaryData.salesAmount }}</span>
              <span v-if="summaryData.purchaseAmount">采购金额: {{ summaryData.purchaseAmount }}</span>
              <span v-if="summaryData.inTransitQty">待收货数量: {{ summaryData.inTransitQty }}</span>
              <span v-if="summaryData.pendingShipQty">待发货数量: {{ summaryData.pendingShipQty }}</span>
              <span>账面库存: {{ summaryData.bookQty }}</span>
              <span v-if="summaryData.availableQty">可用库存: {{ summaryData.availableQty }}</span>
              <span class="footer-highlight">计划采购数量: {{ summaryData.planPurchaseQty }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- 打印：结果集打印（勾选则打勾选，否则打当前这批） -->
    <PrintDialog
      ref="printDialogRef"
      page-code="purchase-smart-replenish"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, PrinterOutlined, ExportOutlined,
  ReloadOutlined, ShoppingCartOutlined, PictureOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { stockReportApi, type SmartReplenishItem } from '@/api/analytics'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'
import { optionsApi } from '@/api/options'
import request from '@/utils/request'

const router = useRouter()
defineOptions({ name: 'PurchaseSmartReplenish' })

const handleError = (e: any) => console.warn('[智能补货] ErrorBoundary:', e)

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<SmartReplenishItem[]>([])
const selectedRows = ref<SmartReplenishItem[]>([])

// ═══ 分类树 ═══
const categoryLoading = ref(false)
const categoryTreeData = ref<any[]>([])
const expandedKeysTree = ref<(string | number)[]>([])
const selectedCategoryId = ref<string | number>('__all__')

// ═══ 搜索参数 ═══
const minPlanOp = ref('>=')
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const searchParams = reactive({
  stockDays: 0,
  productKeyword: '',
  warehouseId: undefined as number | undefined,
  supplierName: '',
  minPlanQty: undefined as number | undefined,
  startDate: '',
  endDate: '',
})
const warehouseOptions = ref<{ label: string; value: number }[]>([])

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 格式化 ═══
function formatQty(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '0'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}
function formatMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 25 列表格 ═══
const columns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '图片', field: 'image', key: 'image', width: 60, type: 'slot', slotName: 'imageCell' },
  { title: '商品名称', field: 'productName', key: 'productName', width: 190, ellipsis: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 120 },
  { title: '单位', field: 'unit', key: 'unit', width: 70 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 130 },
  { title: '型号', field: 'model', key: 'model', width: 100 },
  { title: '产地', field: 'origin', key: 'origin', width: 100 },
  { title: '品牌', field: 'brand', key: 'brand', width: 100, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140 },
  { title: '销售数量', field: 'salesQty', key: 'salesQty', width: 100, align: 'right', formatter: formatQty },
  { title: '销售金额', field: 'salesAmount', key: 'salesAmount', width: 120, align: 'right', formatter: formatMoney },
  { title: '采购金额', field: 'purchaseAmount', key: 'purchaseAmount', width: 120, align: 'right', formatter: formatMoney },
  { title: '日均销量', field: 'avgDailySales', key: 'avgDailySales', width: 100, align: 'right', formatter: formatQty },
  { title: '备货天数', field: 'stockDays', key: 'stockDays', width: 90, align: 'right', defaultHidden: true, formatter: formatQty },
  { title: '规格', field: 'specification', key: 'specification', width: 110, defaultHidden: true },
  { title: '待收货数量', field: 'inTransitQty', key: 'inTransitQty', width: 110, align: 'right', formatter: formatQty },
  { title: '待发货数量', field: 'pendingShipQty', key: 'pendingShipQty', width: 110, align: 'right', defaultHidden: true, formatter: formatQty },
  { title: '采购数量', field: 'purchaseQty', key: 'purchaseQty', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { title: '账面库存', field: 'bookQty', key: 'bookQty', width: 100, align: 'right', defaultHidden: true, formatter: formatQty },
  { title: '账面库存换算结果', field: 'bookQtyConverted', key: 'bookQtyConverted', width: 120, align: 'right', defaultHidden: true, formatter: formatQty },
  { title: '计划采购数量', field: 'planPurchaseQty', key: 'planPurchaseQty', width: 120, align: 'right', type: 'slot', slotName: 'planPurchaseCell' },
  { title: '可用库存', field: 'availableQty', key: 'availableQty', width: 100, align: 'right', formatter: formatQty },
  { title: '可用库存换算结果', field: 'availableQtyConverted', key: 'availableQtyConverted', width: 120, align: 'right', defaultHidden: true, formatter: formatQty },
  { title: '最近销售日期', field: 'lastSaleDate', key: 'lastSaleDate', width: 120, defaultHidden: true },
  { title: '最近进货日期', field: 'lastPurchaseDate', key: 'lastPurchaseDate', width: 120, defaultHidden: true },
]

// ═══ 日期处理 ═══
const handleDateRangeChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
  handleSearch()
}

// ═══ 合计 ═══
const summaryData = computed(() => {
  const data = tableData.value
  const sum = (key: keyof SmartReplenishItem) =>
    data.reduce((acc, r) => acc + (Number(r[key]) || 0), 0)
  return {
    salesQty: sum('salesQty') ? formatQty(sum('salesQty')) : '',
    salesAmount: sum('salesAmount') ? formatMoney(sum('salesAmount')) : '',
    purchaseAmount: sum('purchaseAmount') ? formatMoney(sum('purchaseAmount')) : '',
    inTransitQty: sum('inTransitQty') ? formatQty(sum('inTransitQty')) : '',
    pendingShipQty: sum('pendingShipQty') ? formatQty(sum('pendingShipQty')) : '',
    bookQty: formatQty(sum('bookQty')),
    availableQty: sum('availableQty') ? formatQty(sum('availableQty')) : '',
    planPurchaseQty: formatQty(sum('planPurchaseQty')),
  }
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      stockDays: searchParams.stockDays ?? 0,
    }
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    if (searchParams.productKeyword) params.productKeyword = searchParams.productKeyword
    if (searchParams.warehouseId != null) params.warehouseId = searchParams.warehouseId
    if (searchParams.supplierName) params.supplierName = searchParams.supplierName
    if (searchParams.minPlanQty != null) params.minPlanQty = searchParams.minPlanQty
    if (selectedCategoryId.value && selectedCategoryId.value !== '__all__') {
      params.categoryId = selectedCategoryId.value
    }
    const res: any = await stockReportApi.smartReplenishPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    console.warn('[智能补货] 加载失败', e)
    message.error('查询失败，请检查网络后重试')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows || []
}

// ═══ 分类树 ═══
async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await request.get('/erp/product-category/tree')
    const tree = Array.isArray(res?.data) ? res.data : (Array.isArray(res) ? res : [])
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: tree }]
    expandedKeysTree.value = ['__all__', ...tree.filter((c: any) => c.children?.length).map((c: any) => c.id)]
  } catch (e) {
    console.warn('[智能补货] 分类树加载失败', e)
    categoryTreeData.value = [{ id: '__all__', categoryName: '全部商品', children: [] }]
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys && keys.length ? keys[0] : '__all__'
  handleSearch()
}

// ═══ 下拉选项 ═══
async function loadWarehouses() {
  try {
    const wh = await optionsApi.getWarehouses()
    warehouseOptions.value = (wh || []).map((w: any) => ({ label: w.name || w.warehouseName || w.warehouse_name, value: w.id }))
  } catch { warehouseOptions.value = [] }
}

// ═══ 导出 ═══
function handleExport() {
  if (!tableData.value.length) {
    message.warning('没有可导出的数据')
    return
  }
  const exportCols = columns.filter(c => c.key !== 'image' && c.type !== 'slot')
  const headers = exportCols.map(c => c.title)
  const rows = tableData.value.map(row =>
    exportCols.map(col => {
      const val = row[col.key as keyof SmartReplenishItem]
      const display = col.formatter ? col.formatter(val as any) : (val ?? '')
      return `"${String(display).replace(/"/g, '""')}"`
    }),
  )
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `智能补货_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面。勾选了就打勾选的，否则打当前这批。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'purchase-smart-replenish',
  title: '智能补货',
  columns: () => columns,
  rows: () => tableData.value,
  selectedRows: () => selectedRows.value,
  emptyTip: '没有可打印的数据',
})

// ═══ 采购：跳转采购订单表单，预填选中商品 ═══
function handlePurchase() {
  const productIds = Array.from(new Set(selectedRows.value.map(r => r.productId).filter(Boolean)))
  if (!productIds.length) {
    message.warning('请先勾选需要采购的商品')
    return
  }
  router.push({
    path: '/erp/purchase/form',
    query: { productIds: productIds.join(',') },
  })
}

onMounted(() => {
  loadCategoryTree()
  loadWarehouses()
  const start = dayjs().subtract(7, 'day')
  const end = dayjs()
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.hint-bar {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 4px;
  padding: 6px 12px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #7a4d00;
}
.hint-text { line-height: 20px; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-field-item :deep(.ant-input-number) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 12px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 0 0 52px; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; display: flex; align-items: center; }
.search-select-wrap :deep(.ant-input-number) { flex: 1; min-width: 0; border: none; border-radius: 0; }
.search-action-item { flex-shrink: 0; }

.table-footer { display: flex; align-items: center; padding: 6px 12px; background: #fafafa; border: 1px solid #f0f0f0; border-top: none; font-size: 12px; color: #333; }
.footer-label { font-weight: 600; min-width: 90px; }
.footer-values { flex: 1; display: flex; gap: 18px; flex-wrap: wrap; }
.footer-highlight { color: #f5222d; font-weight: 600; }
.plan-qty { font-weight: 600; color: #f5222d; }
</style>
