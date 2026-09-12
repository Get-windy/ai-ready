<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
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
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
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

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 打印(F8) + 导出（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button size="small" @click="fetchData">
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

        <!-- ═══ 查询区 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-field-item">
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 100%"
                  @change="handleDateChange"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.productName"
                  placeholder="商品"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.partnerName"
                  placeholder="往来单位"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.handlerName"
                  placeholder="经手人"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-field-item">
                <a-input
                  v-model:value="searchParams.deptName"
                  placeholder="部门"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-field-item">
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
              :columns="currentColumns"
              :storage-key="activeTab === 'in' ? 'borrow-query-table-columns-in' : 'borrow-query-table-columns-out'"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              row-key="rowKey"
              @page-change="handlePageChange"
            >
              <template #imageCell="{ record }">
                <img
                  v-if="record.imageUrl"
                  :src="record.imageUrl"
                  class="row-img"
                  alt=""
                />
                <span v-else>—</span>
              </template>
              <template #productNameCell="{ record }">
                <a-tooltip :title="record.productName">
                  <span class="cell-link">{{ record.productName }}</span>
                </a-tooltip>
              </template>
              <template #borrowStockQtyCell="{ record }">
                <span class="num-value">{{ formatQty(record.borrowStockQty) }}</span>
              </template>
              <template #borrowStockAmountCell="{ record }">
                <span class="num-value">{{ formatAmount(record.borrowStockAmount) }}</span>
              </template>
              <template #borrowQueryQtyCell="{ record }">
                <span class="num-value">{{ formatQty(record.borrowQueryQty) }}</span>
              </template>
              <template #borrowQueryAmountCell="{ record }">
                <span class="num-value">{{ formatAmount(record.borrowQueryAmount) }}</span>
              </template>
              <template #returnQtyCell="{ record }">
                <span class="num-value">{{ formatQty(record.returnQty) }}</span>
              </template>
              <template #returnAmountCell="{ record }">
                <span class="num-value">{{ formatAmount(record.returnAmount) }}</span>
              </template>
              <template #convertQtyCell="{ record }">
                <span class="num-value">{{ formatQty(record.convertQty) }}</span>
              </template>
              <template #convertAmountCell="{ record }">
                <span class="num-value">{{ formatAmount(record.convertAmount) }}</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, SettingOutlined, ReloadOutlined, PrinterOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { productCategoryApi } from '@/api/erp/product'
import { borrowApi } from '@/api/wms/borrow'

defineOptions({ name: 'WhBorrowQuery' })

// ═══ 方向 Tab（借进商品查询 / 借出商品查询） ═══
const tabs = [
  { key: 'in', label: '借进商品查询' },
  { key: 'out', label: '借出商品查询' },
]
const activeTab = ref<'in' | 'out'>('in')

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' },
]
const quickDate = ref('week')
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const allRows = ref<any[]>([])

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  productName: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  dateStart: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  dateEnd: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页（前端内存分页聚合结果） ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const tableData = computed(() =>
  allRows.value.slice((pagination.current - 1) * pagination.pageSize, pagination.current * pagination.pageSize)
)

// ═══ 页面配置弹窗（列配置走数据表表头齿轮） ═══
const showPageConfig = ref(false)

// ═══ 列定义（对标文档30列 + 序号） ═══
interface ColumnDef {
  title: string
  key: string
  field?: string
  type?: string
  slotName?: string
  width?: number
  fixed?: string
  align?: string
  defaultHidden?: boolean
  sortable?: boolean
}

function makeColumns(isIn: boolean): ColumnDef[] {
  const st = isIn ? '借进' : '借出'                       // 库存前缀：借进库存 / 借出库存
  const q = isIn ? '借进查询借进' : '借出查询借出'          // 借进/借出数量·金额
  const r = isIn ? '借进查询还出' : '借出查询还回'          // 还出/还回数量·金额
  const cv = isIn ? '借进查询借转采购' : '借出查询借转销售'  // 借转采购/借转销售数量·金额
  return [
    { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
    { title: '图片', key: 'imageUrl', field: 'imageUrl', width: 70, type: 'slot', slotName: 'imageCell' },
    { title: '商品名称', key: 'productName', field: 'productName', width: 190, type: 'slot', slotName: 'productNameCell' },
    { title: '货号', key: 'productCode', field: 'productCode', width: 110 },
    { title: '规格', key: 'productSpec', field: 'productSpec', width: 100 },
    { title: '型号', key: 'model', field: 'model', width: 90 },
    { title: '产地', key: 'origin', field: 'origin', width: 90 },
    { title: '品牌', key: 'brand', field: 'brand', width: 90 },
    { title: '条码', key: 'barcode', field: 'barcode', width: 120 },
    { title: '往来单位编号', key: 'partnerCode', field: 'partnerCode', width: 120 },
    { title: '往来单位', key: 'partnerName', field: 'partnerName', width: 180 },
    { title: '客户级别', key: 'customerLevel', field: 'customerLevel', width: 100 },
    { title: '联系人', key: 'contact', field: 'contact', width: 100 },
    { title: '地址', key: 'address', field: 'address', width: 150 },
    { title: '默认经手人', key: 'defaultHandler', field: 'defaultHandler', width: 100 },
    { title: '客户一票通', key: 'oneBill', field: 'oneBill', width: 100 },
    { title: '客户备注', key: 'customerRemark', field: 'customerRemark', width: 130 },
    { title: '小单位', key: 'smallUnit', field: 'smallUnit', width: 80 },
    { title: '换算关系', key: 'conversionRelation', field: 'conversionRelation', width: 100 },
    { title: '商品备注', key: 'productRemark', field: 'productRemark', width: 130 },
    { title: '单位', key: 'unit', field: 'unit', width: 70 },
    { title: `${st}库存数量`, key: 'borrowStockQty', field: 'borrowStockQty', width: 116, align: 'right', type: 'slot', slotName: 'borrowStockQtyCell' },
    { title: `${st}库存换算结果`, key: 'borrowStockConversion', field: 'borrowStockConversion', width: 116, align: 'right' },
    { title: `${st}库存浮动数量`, key: 'borrowStockFloat', field: 'borrowStockFloat', width: 116, align: 'right' },
    { title: `${st}库存金额`, key: 'borrowStockAmount', field: 'borrowStockAmount', width: 126, align: 'right', type: 'slot', slotName: 'borrowStockAmountCell' },
    { title: `${q}数量`, key: 'borrowQueryQty', field: 'borrowQueryQty', width: 126, align: 'right', type: 'slot', slotName: 'borrowQueryQtyCell' },
    { title: `${q}金额`, key: 'borrowQueryAmount', field: 'borrowQueryAmount', width: 126, align: 'right', type: 'slot', slotName: 'borrowQueryAmountCell' },
    { title: `${r}数量`, key: 'returnQty', field: 'returnQty', width: 126, align: 'right', type: 'slot', slotName: 'returnQtyCell' },
    { title: `${r}金额`, key: 'returnAmount', field: 'returnAmount', width: 126, align: 'right', type: 'slot', slotName: 'returnAmountCell' },
    { title: `${cv}数量`, key: 'convertQty', field: 'convertQty', width: 136, align: 'right', type: 'slot', slotName: 'convertQtyCell' },
    { title: `${cv}金额`, key: 'convertAmount', field: 'convertAmount', width: 136, align: 'right', type: 'slot', slotName: 'convertAmountCell' },
  ]
}

const inColumnDefs = makeColumns(true)
const outColumnDefs = makeColumns(false)

// ═══ 列配置（走数据表表头齿轮：storage-key=borrow-query-table-columns-in/out） ═══
const currentColumns = computed(() =>
  activeTab.value === 'in' ? inColumnDefs : outColumnDefs
)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'borrow-query-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '单据日期', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'config', label: '配置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFieldConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const pageConfigStorageKey = computed(() => `page-config:${PAGE_CONFIG_STORAGE_KEY}`)
const activeQueryFields = computed(() => queryFieldConfig.value)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.queryFields) {
        queryFieldConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch {
    // ignore
  }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

// ═══ 商品分类树 ═══
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
    console.warn('[借进借出查询] 加载分类树失败', e)
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

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      direction: activeTab.value === 'in' ? 1 : 2,
      dateStart: searchParams.dateStart || undefined,
      dateEnd: searchParams.dateEnd || undefined,
    }
    if (searchParams.productName) params.productName = searchParams.productName
    if (searchParams.partnerName) params.partnerName = searchParams.partnerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (selectedCategoryId.value !== '0') params.categoryId = selectedCategoryId.value

    const res: any = await borrowApi.aggregate(params)
    // request 拦截器已解包 data，res 为数组
    const data: any[] = Array.isArray(res) ? res : (res?.data || res?.records || [])
    allRows.value = data.map((r: any, i: number) => ({
      ...r,
      rowKey: `${activeTab.value}-${r.productId}-${r.partnerId}-${i}`,
    }))
    pagination.total = data.length
    pagination.current = 1
  } catch (error: any) {
    console.warn('[借进借出查询] 获取聚合数据失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
    allRows.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key as 'in' | 'out'
  pagination.current = 1
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.subtract(7, 'day'); end = now
  }
  dateRange.value = [start, end]
  searchParams.dateStart = start.format('YYYY-MM-DD')
  searchParams.dateEnd = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.dateStart = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.dateEnd = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.dateStart = ''
    searchParams.dateEnd = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
}

// ═══ 工具栏操作 ═══
function handleRefresh() { fetchData() }

function handlePrintF8() {
  if (allRows.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}

async function handleExport() {
  if (allRows.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const cols = currentColumns.value.filter((c: any) => c.key !== 'rowNo' && c.title)
  const header = cols.map((c: any) => c.title).join(',')
  const lines = allRows.value.map((r: any) => cols.map((c: any) => {
    const v = r[c.field]
    return v === null || v === undefined ? '' : (['qty', 'amount'].includes(c.align) && typeof v === 'number' ? v : String(v).replace(/,/g, ''))
  }).join(','))
  const csv = '\uFEFF' + [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `借进借出查询-${activeTab.value === 'in' ? '借进商品' : '借出商品'}-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 工具 ═══
function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}
function formatAmount(val: number | null | undefined): string {
  if (val === null || val === undefined) return '-'
  return `¥${Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function handleError(error: Error) {
  console.error('[借进借出查询] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('week')
  fetchCategoryTree()
})
</script>

<style scoped>
.query-scheme-wrap { display: inline-flex; align-items: center; margin-right: 8px; }
.quick-dates { flex-wrap: wrap; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.search-field-item { display: flex; align-items: center; }
.search-field-item :deep(.ant-input),
.search-field-item :deep(.ant-select),
.search-field-item :deep(.ant-picker) { font-size: 13px; }
.search-field-item .ant-input { width: 150px; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.row-img { width: 36px; height: 36px; object-fit: cover; border-radius: 4px; vertical-align: middle; }
.cell-link { color: #1668dc; }
.num-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
