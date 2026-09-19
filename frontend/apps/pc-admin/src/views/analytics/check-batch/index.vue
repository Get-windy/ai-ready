<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        查批次（分析 → 仓配分析 → 查批次，菜单 80432）
        对标 ql361「分析 → 仓配分析 → 查批次」：三视图 Tab（商品批次查询 19/13、商品批次跟踪 23/17、
        近效期预警查询 20/12），逐 Tab 独立列配置；查询区「查询方案 / 筛选条件 / 仓库 / 商品 / 品牌 /
        商品状态 / 批次条码」+ 显示层次结构 / 显示停用两个勾选项；工具栏 刷新｜打印(F8)｜导出；
        仅「商品批次查询」有行级「批次跟踪」；左侧商品分类树仅「近效期预警查询」有（对标实测）。
        取数：/erp/batch-sn/batches/page（batch_number，批次时点存量）。
        注：对标「商品批次跟踪」为单据×批次全链路流水，本系统后端无对应端点，该视图只落列结构（详见开发文档缺口）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="hasCategoryTree"
        category-title="商品分类"
        :category-tree-data="categoryTree"
        :category-loading="categoryLoading"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :current-path="currentPath"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 7 项 + 2 勾选项，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">查询方案</span>
                <QuerySchemeBar
                  storage-key="analytics-check-batch\index.vue-query-scheme"
                  :snapshot="querySnapshot"
                  @apply="applyQuerySnapshot"
                />
              </div>
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="商品名称/货号/批次条码"
                  allow-clear
                  style="width: 200px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">仓库</span>
                <a-select
                  v-model:value="query.warehouseId"
                  size="small"
                  placeholder="全部仓库"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  style="width: 160px"
                  :options="warehouseOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="query.productCode"
                  size="small"
                  placeholder="商品编码/货号"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">品牌</span>
                <a-input
                  v-model:value="query.brand"
                  size="small"
                  placeholder="品牌"
                  allow-clear
                  style="width: 120px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">商品状态</span>
                <a-select v-model:value="query.batchStatus" size="small" style="width: 120px" :options="STATUS_OPTIONS" @change="handleSearch" />
              </div>
              <div class="search-item">
                <span class="search-label">批次条码</span>
                <a-input
                  v-model:value="query.batchNo"
                  size="small"
                  placeholder="批次条码"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.showHierarchy" @change="handleSearch">显示层次结构</a-checkbox>
                <a-checkbox v-model:checked="query.showDisabled" @change="handleSearch">
                  显示停用（过期商品橙色显示，停用商品红色显示）
                </a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置，逐 Tab 一套） ═══ -->
        <template #table>
          <div class="table-area">
            <!-- 商品批次跟踪：后端无「单据×批次」流水端点，如实提示，不造假数据 -->
            <a-alert
              v-if="activeTab === 'trace'"
              class="trace-alert"
              type="warning"
              show-icon
              message="本系统后端暂无「商品批次跟踪」（单据 × 批次全链路流水）取数端点，本视图列结构已按对标落地，待后端补齐 /erp/batch-sn/batches/trace/page 后即可出数。"
            />
            <BillDetailTable
              :data-source="pagedRows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="`analytics-check-batch-columns-${activeTab}`"
              :global-config-key="`analytics-check-batch-columns-${activeTab}`"
            >
              <template #productNameCell="{ record }">
                <span :class="nameClass(record)">{{ record.productName || '-' }}</span>
              </template>
              <template #imageCell="{ record }">
                <span v-if="record.image" class="cell-image">{{ record.image }}</span>
                <span v-else class="cell-empty">-</span>
              </template>
              <template #uploadCell>
                <span class="cell-disabled" title="后端 batch_number 无批次附件字段，待补齐后开放">上传附件</span>
              </template>
              <template #attachmentCell="{ record }">
                <span v-if="record.attachmentUrl" class="cell-image">{{ record.attachmentUrl }}</span>
                <span v-else class="cell-empty">-</span>
              </template>
              <template #actionCell="{ record }">
                <a-button type="link" size="small" @click="goTrace(record)">批次跟踪</a-button>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownloadOutlined, PrinterOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { batchSnApi, stockApi } from '@/api/analytics'
import { productCategoryApi } from '@/api/erp/product'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsCheckBatch' })

const route = useRoute()

// ═══ 视图 Tab（对标实测顺序：商品批次查询 / 商品批次跟踪 / 近效期预警查询） ═══
const TABS = [
  { key: 'query', label: '商品批次查询' },
  { key: 'trace', label: '商品批次跟踪' },
  { key: 'expiring', label: '近效期预警查询' }
]
const activeTab = ref('query')
/** 对标实测：仅「近效期预警查询」视图有左侧商品分类树 */
const hasCategoryTree = computed(() => activeTab.value === 'expiring')

const query = reactive({
  keyword: '',
  warehouseId: undefined as number | undefined,
  productCode: '',
  brand: '',
  batchStatus: undefined as string | undefined,
  batchNo: '',
  showHierarchy: true,
  showDisabled: false,
  categoryId: undefined as number | undefined
})

const STATUS_OPTIONS = [
  { label: '全部', value: '' },
  { label: '活跃中', value: 'ACTIVE' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '隔离中', value: 'QUARANTINED' }
]

const loading = ref(false)
const dataSource = ref<any[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/** 近效期视图为「按到期日期升序」的全量呈现，故设全量拉取上限 */
const MAX_ROWS = 2000
/** 后端 BatchQueryRequest 对 size 有 @Max(100)，超过直接 400，切勿调大 */
const FETCH_PAGE_SIZE = 100

// ═══ 左侧商品分类树 ═══
const categoryTree = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const currentPath = ref('全部商品')

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(res) ? res : res?.data || []
  } catch (e) {
    console.warn('[查批次] 商品分类树获取失败', e)
    categoryTree.value = []
  } finally {
    categoryLoading.value = false
  }
}

function findPath(nodes: any[], id: string, trail: string[] = []): string[] | null {
  for (const n of nodes || []) {
    const next = [...trail, n.categoryName]
    if (String(n.id) === id) return next
    const found = n.children?.length ? findPath(n.children, id, next) : null
    if (found) return found
  }
  return null
}

/** 分类过滤后端未支持，只在首次选中时提示一次（避免静默失效） */
const categoryTipShown = ref(false)

function onCategorySelect(keys: (string | number)[]) {
  const key = keys?.length ? String(keys[0]) : ''
  selectedCategoryId.value = key || '0'
  query.categoryId = key ? Number(key) : undefined
  if (key && !categoryTipShown.value) {
    categoryTipShown.value = true
    message.warning('后端 /erp/batch-sn/batches/page 暂不支持按商品分类过滤，分类条件已透传但本次结果不会收敛')
  }
  currentPath.value = key ? (findPath(categoryTree.value, key) || []).join(' / ') || '全部商品' : '全部商品'
  handleSearch()
}

// ═══ 行归一（批次时点存量 + 效期天数计算） ═══
function diffDays(from: string, to: string): number | '' {
  const a = new Date(`${from}T00:00:00`).getTime()
  const b = new Date(`${to}T00:00:00`).getTime()
  if (isNaN(a) || isNaN(b)) return ''
  return Math.round((b - a) / 86400000)
}

function todayStr(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function normalizeBatch(r: any) {
  const production = r.productionDate ? String(r.productionDate).slice(0, 10) : ''
  const expiration = r.expirationDate ? String(r.expirationDate).slice(0, 10) : ''
  const shelf = production && expiration ? diffDays(production, expiration) : ''
  const valid = expiration ? diffDays(todayStr(), expiration) : ''
  return {
    ...r,
    rowKey: String(r.id ?? `${r.batchNo}-${r.warehouseId ?? ''}`),
    // 批次条码 = 批次唯一标识（对标「批次条码」列与「批号」列同源）
    batchBarcode: r.batchNo || '',
    productionDate: production,
    expirationDate: expiration,
    shelfLife: shelf === '' ? '' : `${shelf}天`,
    validDays: valid === '' ? '' : `${valid}天`,
    validDaysRaw: valid,
    nearExpiryDays: valid === '' ? '' : String(valid),
    costAmount: ''
  }
}

/** 效期色标：过期商品橙色、停用(隔离)商品红色（对标查询区勾选项文案） */
function nameClass(record: any) {
  if (record.batchStatus === 'QUARANTINED') return 'name-disabled'
  if (typeof record.validDaysRaw === 'number' && record.validDaysRaw < 0) return 'name-expired'
  return ''
}

// ═══ 列定义（列名与顺序逐字取自《查批次开发文档》§3） ═══

/** Tab1「商品批次查询」：全部 19 列 / 默认 13 */
const queryColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 96, fixed: 'left' },
  { key: 'warehouseName', title: '仓库', width: 130 },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '货号', width: 110 },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'specification', title: '规格', width: 120, defaultHidden: true },
  { key: 'model', title: '型号', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'productionDate', title: '生产日期', width: 110 },
  { key: 'shelfLife', title: '保质期', width: 100, defaultHidden: true },
  { key: 'expirationDate', title: '到期日期', width: 110 },
  { key: 'batchBarcode', title: '批次条码', width: 160 },
  { key: 'availableQuantity', title: '可用数量', width: 100, align: 'right' },
  { key: 'totalQuantity', title: '账面数量', width: 100, align: 'right' },
  { key: 'unitCost', title: '成本单价', width: 100, align: 'right' },
  { key: 'costAmount', title: '成本金额', width: 110, align: 'right' },
  { key: 'uploadAttachment', title: '上传附件', type: 'slot', slotName: 'uploadCell', width: 100, align: 'center' },
  { key: 'attachment', title: '附件', type: 'slot', slotName: 'attachmentCell', width: 90, align: 'center' }
]

/** Tab2「商品批次跟踪」：全部 23 列 / 默认 17 */
const traceColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'bizDate', title: '单据日期', width: 110 },
  { key: 'docNo', title: '单据编号', width: 170 },
  { key: 'productName', title: '商品名称', width: 200 },
  { key: 'productCode', title: '货号', width: 110 },
  { key: 'docTypeName', title: '单据类型', width: 110 },
  { key: 'partnerName', title: '往来单位', width: 180 },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'specification', title: '规格', width: 120, defaultHidden: true },
  { key: 'model', title: '型号', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'warehouseName', title: '仓库', width: 130 },
  { key: 'batchNo', title: '批号', width: 150 },
  { key: 'productionDate', title: '生产日期', width: 110 },
  { key: 'shelfLife', title: '保质期', width: 100, defaultHidden: true },
  { key: 'expirationDate', title: '到期日期', width: 110 },
  { key: 'batchBarcode', title: '批次条码', width: 160 },
  { key: 'qty', title: '数量', width: 100, align: 'right' },
  { key: 'unitCost', title: '成本单价', width: 100, align: 'right' },
  { key: 'costAmount', title: '成本金额', width: 110, align: 'right' },
  { key: 'statusText', title: '状态', width: 90 },
  { key: 'handlerName', title: '经手人', width: 100 }
]

/** Tab3「近效期预警查询」：全部 20 列 / 默认 12 */
const expiringColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 70, align: 'center' },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '货号', width: 110 },
  { key: 'warehouseName', title: '仓库', width: 130 },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'specification', title: '规格', width: 120, defaultHidden: true },
  { key: 'model', title: '型号', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'batchNo', title: '批号', width: 150 },
  { key: 'productionDate', title: '生产日期', width: 110 },
  { key: 'expirationDate', title: '到期日期', width: 110 },
  { key: 'shelfLife', title: '保质期', width: 100, defaultHidden: true },
  { key: 'nearExpiryDays', title: '近效期天数', width: 110, align: 'right', defaultHidden: true },
  { key: 'validDays', title: '有效天数', width: 110, align: 'right', headerTip: '有效天数 = 到期日期 − 当天（负数为已过期）' },
  { key: 'batchBarcode', title: '批次条码', width: 160 },
  { key: 'unit', title: '常用单位', width: 90 },
  { key: 'availableQuantity', title: '可用库存', width: 100, align: 'right' },
  { key: 'totalQuantity', title: '账面库存', width: 100, align: 'right' },
  { key: 'remark', title: '备注', width: 150, defaultHidden: true }
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'trace') return traceColumns
  if (activeTab.value === 'expiring') return expiringColumns
  return queryColumns
})

// ═══ 取数 ═══
function buildParams(extra: Record<string, any> = {}) {
  // 商品状态优先；未选且未勾「显示停用」时只看活跃中批次（对标：停用/过期默认不入列）
  const status = query.batchStatus || (!query.showDisabled ? 'ACTIVE' : undefined)
  return {
    batchNo: query.batchNo || undefined,
    productCode: query.productCode || undefined,
    status,
    warehouseId: query.warehouseId,
    // 筛选条件 / 品牌 / 分类：后端 /batches/page 暂无对应参数，先按原样透传（后端补齐即生效）
    keyword: query.keyword || undefined,
    brand: query.brand || undefined,
    categoryId: query.categoryId,
    ...extra
  }
}

/** Tab1 商品批次查询：服务端分页 */
async function fetchQueryTab() {
  const res: any = await batchSnApi.page({ ...buildParams(), pageNum: pagination.current, pageSize: pagination.pageSize } as any)
  const list: any[] = res?.records || []
  dataSource.value = list.map(normalizeBatch)
  pagination.total = Number(res?.total) || 0
}

/** Tab3 近效期预警查询：全量拉取后按到期日期升序（前端分页） */
async function fetchExpiringTab() {
  const acc: any[] = []
  let capped = false
  for (let page = 1; ; page += 1) {
    const res: any = await batchSnApi.page({ ...buildParams(), pageNum: page, pageSize: FETCH_PAGE_SIZE } as any)
    const list: any[] = res?.records || []
    acc.push(...list)
    if (list.length < FETCH_PAGE_SIZE) break
    if (acc.length >= MAX_ROWS) {
      capped = true
      break
    }
  }
  const rows = acc.map(normalizeBatch)
  rows.sort((a, b) => String(a.expirationDate || '9999-12-31').localeCompare(String(b.expirationDate || '9999-12-31')))
  dataSource.value = rows
  pagination.total = rows.length
  if (capped) message.warning(`数据量过大，仅加载前 ${MAX_ROWS} 条，请缩小仓库或商品范围`)
}

async function fetchData() {
  if (activeTab.value === 'trace') {
    // 后端无「单据 × 批次」流水端点，保持空表（不造假数据）
    dataSource.value = []
    pagination.total = 0
    return
  }
  loading.value = true
  try {
    if (activeTab.value === 'expiring') await fetchExpiringTab()
    else await fetchQueryTab()
  } catch (e) {
    console.warn('[查批次] 获取数据失败', e)
    message.error('获取数据失败')
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 前端分页（近效期视图全量已在前端） ═══
const pagedRows = computed(() => {
  if (activeTab.value !== 'expiring') return dataSource.value
  const start = (pagination.current - 1) * pagination.pageSize
  return dataSource.value.slice(start, start + pagination.pageSize)
})

// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { ...query }
}

function applyQuerySnapshot(v: Record<string, any>) {
  Object.assign(query, v)
  if (typeof quickDate !== 'undefined') quickDate.value = ''
  handleSearch()
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  if (activeTab.value === 'expiring') return
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    keyword: '', warehouseId: undefined, productCode: '', brand: '',
    batchStatus: undefined, batchNo: '', showHierarchy: true, showDisabled: false
  })
  selectedCategoryId.value = '0'
  currentPath.value = '全部商品'
  query.categoryId = undefined
  handleSearch()
}

function onTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

/** 行级「批次跟踪」→ 切到跟踪视图并带出批次条码 */
function goTrace(record: any) {
  query.batchNo = record.batchBarcode || record.batchNo || ''
  activeTab.value = 'trace'
  pagination.current = 1
  fetchData()
}

// ═══ 打印(F8) / 导出 共用列口径 ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.filter(c => c.key !== 'rowNo' && c.key !== 'action' && !c.defaultHidden)
)

function formatNumber(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function cellText(col: DetailColumnConfig, r: any): string {
  const v = r[col.key]
  if (col.key === 'uploadAttachment') return r.attachmentUrl ? '已上传' : ''
  if (col.key === 'attachment') return r.attachmentUrl || ''
  if (/价|金额/.test(col.title)) return formatMoney(v)
  if (/数量|库存|天数|保质期/.test(col.title)) return typeof v === 'string' && v.endsWith('天') ? v : formatNumber(v)
  return v === null || v === undefined || v === '' ? '' : String(v)
}

function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = pagedRows.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1400,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '查批次'
  const html = `<html><head><meta charset="utf-8"><title>查批次-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>查批次 · ${tabLabel}</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

function handleExport() {
  const cols = printableColumns.value
  const mapRows = (list: any[]) => list.map(r => cols.map(c => cellText(c, r)))
  executeExport({
    fileName: `查批次-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: async () => {
      if (activeTab.value !== 'query') return dataSource.value
      // 批次查询：逐页取全量
      const size = FETCH_PAGE_SIZE
      const all: any[] = []
      const pages = Math.max(1, Math.ceil(pagination.total / size))
      for (let p = 1; p <= pages; p++) {
        const res: any = await batchSnApi.page({ ...buildParams(), pageNum: p, pageSize: size } as any)
        const list: any[] = res?.records || []
        all.push(...list.map(normalizeBatch))
        if (list.length < size) break
      }
      return all
    },
    mapToRows: mapRows,
    fallbackRows: () => mapRows(dataSource.value)
  })
}

function handleError(err: any) {
  console.error('[查批次] 页面异常', err)
}

onMounted(async () => {
  // 库存明细行级「批次号」跳转带参（/analytics/check-batch?batchNo=xxx）
  const batchNoFromRoute = route.query.batchNo ? String(route.query.batchNo) : ''
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch (e) {
    console.warn('[查批次] 仓库列表获取失败', e)
  }
  if (batchNoFromRoute) {
    query.batchNo = batchNoFromRoute
    activeTab.value = 'expiring'
  }
  fetchCategoryTree()
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不受宿主 scoped 样式影响，查询区样式随页面自带 */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.trace-alert { margin-bottom: 8px; flex-shrink: 0; }
.name-expired { color: #fa8c16; }
.name-disabled { color: #ff4d4f; }
.cell-image { color: #1677ff; }
.cell-empty { color: #bfbfbf; }
.cell-disabled { color: #bfbfbf; cursor: not-allowed; }
</style>
