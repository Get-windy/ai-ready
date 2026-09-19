<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        库存明细（分析 → 仓配分析 → 库存明细，菜单 80434）
        对标 ql361「分析 → 仓配分析 → 库存明细」：单视图库存流水明细账（45 列 / 默认 17），
        查询区「查询方案 + 8 段时间快捷 + 日期范围 + 日期类型 + 仓库 + 商品 + 商品属性 + 显示红冲」，
        工具栏 刷新｜打印(F8)｜导出｜页面配置，行级「批次号」链接，无分类树、无页内 Tab。
        取数：/erp/stock/flow/page（StockFlowVO，单列带符号变动量 → 前端按方向拆成账面入库/账面出库两组）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段（对标实测：本月高亮） ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-stock-detail\index.vue-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 7 项 + 「显示红冲」勾选，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('stockDetail.dateType')" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select v-model:value="query.dateType" size="small" style="width: 120px" :options="DATE_TYPE_OPTIONS" />
              </div>
              <div v-if="isQueryVisible('stockDetail.dateRange')" class="search-item">
                <span class="search-label">日期范围</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 240px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('stockDetail.warehouseId')" class="search-item">
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
              <div v-if="isQueryVisible('stockDetail.keyword')" class="search-item">
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="商品名称/货号"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('stockDetail.productAttribute')" class="search-item">
                <span class="search-label">商品属性</span>
                <a-input
                  v-model:value="query.productAttribute"
                  size="small"
                  placeholder="商品属性"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.includeReversed" @change="handleSearch">显示红冲</a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置，含 账面入库 / 账面出库 两个分组表头） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              storage-key="analytics-stock-detail-columns"
              global-config-key="analytics-stock-detail-columns"
            >
              <template #docNoCell="{ record }">
                <span class="doc-no">{{ record.docNo }}</span>
              </template>
              <template #qtyCell="{ record, column }">
                <span :class="{ 'qty-out': Number(record[column.key]) < 0 }">{{ formatNumber(record[column.key]) }}</span>
              </template>
              <template #amountCell="{ record, column }">
                <span :class="{ 'amount-out': Number(record[column.key]) < 0 }">{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-button type="link" size="small" @click="goBatchTrace(record)">批次号</a-button>
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

      <!-- ═══ 页面配置（对标实测有「库存明细-页面配置弹窗」截图 → 接 PageConfigPanel） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined, PrinterOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { stockReportApi, stockApi } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsStockDetail' })

const router = useRouter()
const route = useRoute()

const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])

/** 查询态（逐项对应对标查询条件：日期类型 / 日期范围 / 仓库 / 商品 / 商品属性 / 显示红冲） */
const query = reactive({
  dateType: 'bizDate',
  warehouseId: undefined as number | undefined,
  keyword: '',
  productAttribute: '',
  includeReversed: false
})

/** 日期类型：对标实测默认「单据日期」，另一口径取本系统可对应的「记账时间」 */
const DATE_TYPE_OPTIONS = [
  { label: '单据日期', value: 'bizDate' },
  { label: '记账时间', value: 'bookkeepingTime' }
]

const loading = ref(false)
const dataSource = ref<any[]>([])
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 列定义（对标 45 列 / 默认 17；账面入库、账面出库各为 1 个分组头 × 4 个叶子列） ═══
// 列名与顺序逐字取自《库存明细开发文档》§3；defaultHidden 共 28 项 = 45 − 17。
// 后端 StockFlowVO 未提供的列（规格/型号/产地/条码/批次/往来单位/收货人…）保留列位但不造值。
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 86, fixed: 'left' },
  // 默认可见 11 个平铺列
  { key: 'bizDate', title: '单据日期', width: 110 },
  { key: 'docNo', title: '单据编号', type: 'slot', slotName: 'docNoCell', width: 170 },
  { key: 'productName', title: '商品名称', width: 190 },
  { key: 'taste', title: '口味', width: 100 },
  { key: 'batchBarcode', title: '批次条码', width: 150 },
  { key: 'productionDate', title: '生产日期', width: 110 },
  { key: 'expirationDate', title: '到期日期', width: 110 },
  { key: 'unit', title: '单位', width: 70 },
  { key: 'partnerName', title: '往来单位', width: 180 },
  { key: 'warehouseName', title: '仓库', width: 120 },
  { key: 'handlerName', title: '经手人', width: 100 },
  // 默认隐藏 28 个
  { key: 'docTypeName', title: '单据类型', width: 100, defaultHidden: true },
  { key: 'sourceOrderNo', title: '来源订单', width: 160, defaultHidden: true },
  { key: 'productCode', title: '货号', width: 110, defaultHidden: true },
  { key: 'specification', title: '规格', width: 120, defaultHidden: true },
  { key: 'model', title: '型号', width: 110, defaultHidden: true },
  { key: 'origin', title: '产地', width: 100, defaultHidden: true },
  { key: 'barcode', title: '条码', width: 130, defaultHidden: true },
  { key: 'productRemark', title: '商品备注', width: 140, defaultHidden: true },
  { key: 'customerLevel', title: '客户级别', width: 100, defaultHidden: true },
  { key: 'categoryName', title: '所属分类', width: 120, defaultHidden: true },
  { key: 'address', title: '地址', width: 160, defaultHidden: true },
  { key: 'belongWarehouse', title: '所属仓库', width: 120, defaultHidden: true },
  { key: 'contactName', title: '联系人', width: 100, defaultHidden: true },
  { key: 'customerRemark', title: '客户备注', width: 140, defaultHidden: true },
  { key: 'weight', title: '重量（kg）', width: 100, align: 'right', defaultHidden: true },
  { key: 'volume', title: '体积（m³）', width: 100, align: 'right', defaultHidden: true },
  { key: 'smallUnit', title: '小单位', width: 80, defaultHidden: true },
  { key: 'unitCode', title: '单位编号', width: 100, defaultHidden: true },
  { key: 'departmentName', title: '部门', width: 110, defaultHidden: true },
  { key: 'brand', title: '品牌', width: 100, defaultHidden: true },
  { key: 'receiverName', title: '收货人姓名', width: 110, defaultHidden: true },
  { key: 'receiverPhone', title: '收货人电话', width: 120, defaultHidden: true },
  { key: 'receiverAddress', title: '收货人地址', width: 160, defaultHidden: true },
  // 账面入库（4 叶子：3 默认可见 + 小单位数量默认隐藏）
  {
    key: 'inBookGroup',
    title: '账面入库',
    children: [
      { key: 'inQty', title: '数量', type: 'slot', slotName: 'qtyCell', width: 100, align: 'right' },
      { key: 'inQtySmall', title: '小单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'inUnitCost', title: '成本单价', width: 100, align: 'right' },
      { key: 'inAmount', title: '成本金额', type: 'slot', slotName: 'amountCell', width: 110, align: 'right' }
    ]
  },
  // 账面出库（4 叶子：3 默认可见 + 小单位数量默认隐藏）
  {
    key: 'outBookGroup',
    title: '账面出库',
    children: [
      { key: 'outQty', title: '数量', type: 'slot', slotName: 'qtyCell', width: 100, align: 'right' },
      { key: 'outQtySmall', title: '小单位数量', width: 110, align: 'right', defaultHidden: true },
      { key: 'outUnitCost', title: '成本单价', width: 100, align: 'right' },
      { key: 'outAmount', title: '成本金额', type: 'slot', slotName: 'amountCell', width: 110, align: 'right' }
    ]
  },
  { key: 'isGift', title: '是否赠品', width: 90, align: 'center', defaultHidden: true },
  { key: 'itemRemark', title: '单据明细备注', width: 160, defaultHidden: true },
  { key: 'bookkeepingTime', title: '记账时间', width: 160, defaultHidden: true }
]

function formatNumber(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDate(val: any): string {
  if (!val) return ''
  const s = String(val)
  return s.length >= 10 ? s.slice(0, 10) : s
}

/**
 * 流水行归一：后端 StockFlowVO 是「单列带符号变动量」口径（qty/unitCost/amount），
 * 对标是「账面入库 / 账面出库 分列」口径 → 按方向把同一笔流水落到对应组（另一组补 0）。
 */
function normalizeRow(r: any, index: number) {
  const qty = Number(r.qty) || 0
  const unitCost = Number(r.unitCost) || 0
  const amount = r.amount === null || r.amount === undefined ? qty * unitCost : Number(r.amount)
  const inbound = qty >= 0
  return {
    ...r,
    rowKey: `${r.docNo || ''}-${r.productId || ''}-${r.moveTime || ''}-${index}`,
    bizDate: formatDate(r.moveTime),
    inQty: inbound ? qty : 0,
    inQtySmall: '',
    inUnitCost: inbound ? unitCost : 0,
    inAmount: inbound ? amount : 0,
    outQty: inbound ? 0 : Math.abs(qty),
    outQtySmall: '',
    outUnitCost: inbound ? 0 : unitCost,
    outAmount: inbound ? 0 : Math.abs(amount),
    handlerName: r.operatorName || ''
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      warehouseId: query.warehouseId,
      keyword: query.keyword || undefined,
      // 商品属性 / 日期类型 / 显示红冲：后端 flow/page 暂无对应参数，先按原样透传（后端补齐即生效）
      productAttribute: query.productAttribute || undefined,
      dateType: query.dateType,
      includeReversed: query.includeReversed ? true : undefined,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1]
    }
    const res: any = await stockReportApi.flowPage(params)
    const list: any[] = res?.records || res?.list || []
    dataSource.value = list.map(normalizeRow)
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    console.warn('[库存明细] 获取数据失败', e)
    message.error('获取数据失败')
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

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
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}

function handleReset() {
  Object.assign(query, { dateType: 'bizDate', warehouseId: undefined, keyword: '', productAttribute: '', includeReversed: false })
  handleSearch()
}

/** 行级「批次号」→ 查批次（近效期预警视图由批次条码定位） */
function goBatchTrace(record: any) {
  if (!record.batchBarcode) {
    message.info('该行无批次条码')
    return
  }
  router.push({ path: '/analytics/check-batch', query: { batchNo: record.batchBarcode } })
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'stockDetail.dateType', label: '日期类型', visible: true },
  { key: 'stockDetail.dateRange', label: '日期范围', visible: true },
  { key: 'stockDetail.warehouseId', label: '仓库', visible: true },
  { key: 'stockDetail.keyword', label: '商品', visible: true },
  { key: 'stockDetail.productAttribute', label: '商品属性', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
/** 打印配置项（对标本页有打印） */
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-stock-detail-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 打印(F8) / 导出 共用列口径 ═══
/** 叶子列（分组取 children），页面上无法读取数据表组件的运行时列显隐，故按默认可见列口径输出 */
const leafColumns = computed<DetailColumnConfig[]>(() =>
  columns.flatMap(c => (c.children?.length ? c.children : [c]))
)
/** 打印 / 导出列：剔除系统列（序号 / 操作），仅取默认可见列，等价对标「所见即所打」 */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.key !== 'rowNo' && c.key !== 'action' && !c.defaultHidden)
)

function cellText(col: DetailColumnConfig, r: any): string {
  const v = r[col.key]
  if (col.key === 'inAmount' || col.key === 'outAmount' || col.key === 'inUnitCost' || col.key === 'outUnitCost') return formatMoney(v)
  if (col.key === 'inQty' || col.key === 'outQty') return formatNumber(v)
  return v === null || v === undefined || v === '' ? '' : String(v)
}

function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = dataSource.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const html = `<html><head><meta charset="utf-8"><title>库存明细</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>库存明细（${dateRange.value?.[0] || ''} ~ ${dateRange.value?.[1] || ''}）</h3>
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

// ═══ 导出（后端逐页取全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()
const EXPORT_COLUMNS = printableColumns

function buildParams() {
  return {
    warehouseId: query.warehouseId,
    keyword: query.keyword || undefined,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1]
  }
}

async function fetchAllRows(): Promise<any[]> {
  const size = 500
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await stockReportApi.flowPage({ ...buildParams(), pageNum: p, pageSize: size })
    const list: any[] = res?.records || []
    all.push(...list.map(normalizeRow))
    if (list.length < size) break
  }
  return all
}

function mapExportRows(list: any[]): string[][] {
  return list.map(r => EXPORT_COLUMNS.value.map(c => cellText(c, r)))
}

function handleExport() {
  executeExport({
    fileName: '库存明细',
    headers: EXPORT_COLUMNS.value.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: mapExportRows,
    fallbackRows: () => mapExportRows(dataSource.value)
  })
}

function handleError(err: any) {
  console.error('[库存明细] 页面异常', err)
}

onMounted(async () => {
  // 查库存行级「明细 / 对账」跳转带参（/analytics/stock-detail?keyword=xxx&includeReversed=1）
  if (route.query.keyword) query.keyword = String(route.query.keyword)
  if (route.query.includeReversed === '1') query.includeReversed = true
  if (route.query.warehouseId) query.warehouseId = Number(route.query.warehouseId)
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch (e) {
    console.warn('[库存明细] 仓库列表获取失败', e)
  }
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
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.doc-no { color: #333; }
.qty-out { color: #cf1322; }
.amount-out { color: #cf1322; }
</style>
