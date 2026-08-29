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
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 130px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button type="link" size="small" style="padding: 0 4px">
              <PlusOutlined />
            </a-button>
          </div>
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in quickDates"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

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
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(PDF)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <a-button size="small" @click="showColumnConfig = true">
              <TableOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <!-- 日期 -->
              <div class="search-field-item">
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDateRangeChange"
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
              <!-- 客户 -->
              <div class="search-field-item">
                <a-select
                  v-model:value="searchParams.customerId"
                  placeholder="客户"
                  allow-clear
                  size="small"
                  show-search
                  :filter-option="filterOption"
                  :options="customerOptions"
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
              <!-- 经手人 -->
              <div class="search-field-item">
                <a-select
                  v-model:value="searchParams.salesmanId"
                  placeholder="经手人"
                  allow-clear
                  size="small"
                  show-search
                  :filter-option="filterOption"
                  :options="salesmanOptions"
                  @change="handleSearch"
                />
              </div>
              <!-- 单据状态 -->
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">单据状态</span>
                  <a-select v-model:value="searchParams.orderStatus" placeholder="全部" allow-clear size="small" @change="handleSearch">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option :value="2">待发货</a-select-option>
                    <a-select-option :value="3">部分发货</a-select-option>
                    <a-select-option :value="4">发货完成</a-select-option>
                    <a-select-option :value="5">交易完成</a-select-option>
                    <a-select-option :value="6">已取消</a-select-option>
                  </a-select>
                </div>
              </div>
              <!-- 订单来源 -->
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">订单来源</span>
                  <a-select v-model:value="searchParams.orderSource" placeholder="全部" allow-clear size="small" @change="handleSearch">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option :value="1">内部销售</a-select-option>
                    <a-select-option :value="2">B2B商城</a-select-option>
                    <a-select-option :value="3">B2C零售</a-select-option>
                    <a-select-option :value="4">H5商城</a-select-option>
                    <a-select-option :value="5">小程序</a-select-option>
                  </a-select>
                </div>
              </div>
              <!-- 缺货数量= -->
              <div class="search-field-item">
                <div class="search-select-wrap">
                  <span class="search-select-label">缺货数量=</span>
                  <a-select v-model:value="shortageMode" size="small" @change="handleSearch">
                    <a-select-option :value="1">待发货数量-账面库存</a-select-option>
                    <a-select-option :value="2">待发货数量-未收货数量-账面库存</a-select-option>
                  </a-select>
                </div>
              </div>
              <!-- 查询按钮 + 仅显示缺货商品 -->
              <div class="search-field-item search-action-item">
                <a-button type="primary" size="small" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
              <div class="search-field-item">
                <a-checkbox v-model:checked="onlyShortage" @change="handleSearch">
                  仅显示缺货商品
                </a-checkbox>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="visibleColumns"
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
          </BillTableList>
        </template>

        <!-- ═══ 表格底部：合计 ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <div class="footer-label">合计</div>
            <div class="footer-values">
              <span v-if="summaryData.orderQty">订单数量: {{ summaryData.orderQty }}</span>
              <span v-if="summaryData.amountWithTax">价税合计: {{ summaryData.amountWithTax }}</span>
              <span v-if="summaryData.shippedQty">已发货数量: {{ summaryData.shippedQty }}</span>
              <span v-if="summaryData.unshippedQty">待发货数量: {{ summaryData.unshippedQty }}</span>
              <span v-if="summaryData.inTransitQty">待收货数量: {{ summaryData.inTransitQty }}</span>
              <span>账面库存: {{ summaryData.bookQty }}</span>
              <span class="footer-highlight">缺货数量: {{ summaryData.shortageQty }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      storage-key="purchase-shortage-replenish-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, SettingOutlined, TableOutlined, PlusOutlined, PrinterOutlined,
  ExportOutlined, ReloadOutlined, ShoppingCartOutlined, PictureOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { stockReportApi, type ShortageReplenishItem } from '@/api/analytics'
import { optionsApi } from '@/api/options'
import request from '@/utils/request'

const router = useRouter()
defineOptions({ name: 'PurchaseShortageReplenish' })

const handleError = (e: any) => console.warn('[缺货补货] ErrorBoundary:', e)

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<ShortageReplenishItem[]>([])
const showPageConfig = ref(false)
const showColumnConfig = ref(false)
const queryScheme = ref('')
const selectedRows = ref<ShortageReplenishItem[]>([])

// ═══ 分类树 ═══
const categoryLoading = ref(false)
const categoryTreeData = ref<any[]>([])
const expandedKeysTree = ref<(string | number)[]>([])
const selectedCategoryId = ref<string | number>('__all__')

// ═══ 快捷日期 ═══
const quickDate = ref('thisWeek')
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'thisWeek', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'thisMonth', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Months', label: '近三月' },
  { key: 'thisYear', label: '本年' },
]
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// ═══ 搜索参数 ═══
const searchParams = reactive({
  productKeyword: '',
  warehouseId: undefined as number | undefined,
  customerId: undefined as number | undefined,
  supplierName: '',
  salesmanId: undefined as number | undefined,
  orderStatus: '' as number | '',
  orderSource: '' as number | '',
  startDate: '',
  endDate: '',
})
const shortageMode = ref(1)
const onlyShortage = ref(false)

// ═══ 下拉选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const customerOptions = ref<{ label: string; value: number }[]>([])
const salesmanOptions = ref<{ label: string; value: number }[]>([])

function filterOption(input: string, option: any): boolean {
  return String(option?.label || '').toLowerCase().includes(input.toLowerCase())
}

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
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function formatMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 16列表格 ═══
const columns = [
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '图片', field: 'image', key: 'image', width: 60, type: 'slot', slotName: 'imageCell' },
  { title: '商品名称', field: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '商品货号', field: 'productCode', key: 'productCode', width: 110 },
  { title: '规格', field: 'specification', key: 'specification', width: 100, defaultHidden: true },
  { title: '型号', field: 'model', key: 'model', width: 100, defaultHidden: true },
  { title: '产地', field: 'origin', key: 'origin', width: 100, defaultHidden: true },
  { title: '品牌', field: 'brand', key: 'brand', width: 100, defaultHidden: true },
  { title: '单位', field: 'unit', key: 'unit', width: 70 },
  { title: '订单数量', field: 'orderQty', key: 'orderQty', width: 100, align: 'right', formatter: formatQty },
  { title: '价税合计', field: 'amountWithTax', key: 'amountWithTax', width: 110, align: 'right', formatter: formatMoney },
  { title: '已发货数量', field: 'shippedQty', key: 'shippedQty', width: 100, align: 'right', formatter: formatQty },
  { title: '待发货数量', field: 'unshippedQty', key: 'unshippedQty', width: 100, align: 'right', formatter: formatQty },
  { title: '待收货数量', field: 'inTransitQty', key: 'inTransitQty', width: 100, align: 'right', formatter: formatQty },
  { title: '账面库存', field: 'bookQty', key: 'bookQty', width: 100, align: 'right', formatter: formatQty },
  { title: '缺货数量', field: 'shortageQty', key: 'shortageQty', width: 100, align: 'right', formatter: formatQty },
  { title: '备注', field: 'remark', key: 'remark', width: 140 },
]

const columnDefs = computed(() => columns.map(c => ({ ...c })))
const {
  visibleColumns,
  settingsColumns,
  onSettingChange,
  resetSettings,
} = useColumnConfig(columnDefs.value, 'purchase-shortage-replenish-list-columns')

function handleColumnConfigChange() { onSettingChange() }
function handleColumnConfigReset() { resetSettings() }

// ═══ 页面配置 ═══
const queryFieldsConfig = ref([
  { key: 'dateRange', label: '日期', visible: true },
  { key: 'productKeyword', label: '商品', visible: true },
  { key: 'warehouseId', label: '仓库', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'supplierName', label: '供应商', visible: true },
  { key: 'salesmanId', label: '经手人', visible: true },
  { key: 'orderStatus', label: '单据状态', visible: true },
  { key: 'orderSource', label: '订单来源', visible: true },
  { key: 'shortageMode', label: '缺货数量=', visible: true },
  { key: 'onlyShortage', label: '仅显示缺货商品', visible: true },
])

const functionButtonConfig = ref([
  { key: 'purchase', label: '采购', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(PDF)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'columnConfig', label: '列配置', enabled: true },
])

function handlePageConfigChange() {
  // 页面配置变更后由 PageConfigPanel 自行持久化
}

// ═══ 日期处理 ═══
function setQuickDate(key: string) {
  quickDate.value = key
  const today = dayjs()
  let start: Dayjs
  let end: Dayjs
  switch (key) {
    case 'yesterday': start = end = today.subtract(1, 'day'); break
    case 'today': start = end = today; break
    case 'thisWeek': start = today.startOf('week'); end = today; break
    case 'lastWeek': start = today.subtract(7, 'day'); end = today; break
    case 'thisMonth': start = today.startOf('month'); end = today; break
    case 'lastMonth': start = today.subtract(1, 'month').startOf('month'); end = today.subtract(1, 'month').endOf('month'); break
    case 'last3Months': start = today.subtract(3, 'month'); end = today; break
    case 'thisYear': start = today.startOf('year'); end = today; break
    default: return
  }
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

const handleDateRangeChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates?.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

// ═══ 数据处理 ═══
const summaryData = computed(() => {
  const data = tableData.value
  const sum = (key: keyof ShortageReplenishItem) =>
    data.reduce((acc, r) => acc + (Number(r[key]) || 0), 0)
  return {
    orderQty: sum('orderQty') ? formatQty(sum('orderQty')) : '',
    amountWithTax: sum('amountWithTax') ? formatMoney(sum('amountWithTax')) : '',
    shippedQty: sum('shippedQty') ? formatQty(sum('shippedQty')) : '',
    unshippedQty: sum('unshippedQty') ? formatQty(sum('unshippedQty')) : '',
    inTransitQty: sum('inTransitQty') ? formatQty(sum('inTransitQty')) : '',
    bookQty: formatQty(sum('bookQty')),
    shortageQty: formatQty(sum('shortageQty')),
  }
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      shortageMode: shortageMode.value,
      onlyShortage: onlyShortage.value,
    }
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    if (searchParams.productKeyword) params.productKeyword = searchParams.productKeyword
    if (searchParams.warehouseId != null) params.warehouseId = searchParams.warehouseId
    if (searchParams.customerId != null) params.customerId = searchParams.customerId
    if (searchParams.supplierName) params.supplierName = searchParams.supplierName
    if (searchParams.salesmanId != null) params.salesmanId = searchParams.salesmanId
    if (searchParams.orderStatus !== '') params.orderStatus = searchParams.orderStatus
    if (searchParams.orderSource !== '') params.orderSource = searchParams.orderSource
    if (selectedCategoryId.value && selectedCategoryId.value !== '__all__') {
      params.categoryId = selectedCategoryId.value
    }
    const res: any = await stockReportApi.shortageReplenishPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    console.warn('[缺货补货] 加载失败', e)
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
    console.warn('[缺货补货] 分类树加载失败', e)
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
async function loadOptions() {
  try {
    const wh = await optionsApi.getWarehouses()
    warehouseOptions.value = (wh || []).map((w: any) => ({ label: w.name || w.warehouseName || w.warehouse_name, value: w.id }))
  } catch { warehouseOptions.value = [] }
  try {
    const cus = await optionsApi.getCustomers()
    customerOptions.value = (cus || []).map((c: any) => ({ label: c.name || c.partyName || c.partnerName, value: c.id }))
  } catch { customerOptions.value = [] }
  try {
    const us = await optionsApi.getUsers()
    salesmanOptions.value = (us || []).map((u: any) => ({ label: u.name, value: u.id }))
  } catch { salesmanOptions.value = [] }
}

// ═══ 导出 ═══
function handleExport() {
  if (!tableData.value.length) {
    message.warning('没有可导出的数据')
    return
  }
  const exportCols = columns.filter(c => c.key !== 'image' && c.type !== 'slot' && c.type !== 'checkbox')
  const headers = exportCols.map(c => c.title)
  const rows = tableData.value.map(row =>
    exportCols.map(col => {
      const val = row[col.key as keyof ShortageReplenishItem]
      const display = col.formatter ? col.formatter(val as any) : (val ?? '')
      return `"${String(display).replace(/"/g, '""')}"`
    })
  )
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `缺货补货_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 打印 ═══
function handlePrint() {
  window.print()
}

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
  loadOptions()
  setQuickDate('thisWeek')
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn-link) { color: #555; padding: 0 8px; height: 24px; line-height: 24px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #fa8c16; border-color: #fa8c16; }
.quick-dates :deep(.ant-btn-primary:hover) { background: #fa8c16; border-color: #fa8c16; }

.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-item { flex-shrink: 0; }

.table-footer { display: flex; align-items: center; padding: 6px 12px; background: #fafafa; border: 1px solid #f0f0f0; border-top: none; font-size: 12px; color: #333; }
.footer-label { font-weight: 600; min-width: 90px; }
.footer-values { flex: 1; display: flex; gap: 18px; flex-wrap: wrap; }
.footer-highlight { color: #f5222d; font-weight: 600; }
</style>
