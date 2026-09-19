<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        预订货查询（分析 → 采销分析 → 销售分析 → 预订货查询，菜单 80419）
        对标 ql361「预订货查询」：2 个视图 Tab（商品预订货分析 28 列 / 默认 11、客户预订货分析 23 列 / 默认 6），
        逐 Tab 独立列配置；查询区 查询方案 + 8 段时间快捷段 + 日期范围 + 客户 + 商品 + 经手人 + 查询；
        工具栏 刷新｜打印(F8)｜导出｜页面配置；黄底红线提示条「已取消、已驳回订单不参与统计」。
        取数：/erp/sale/pre-order/analysis/page?tab=product|customer（后端按商品/按客户聚合，status >= 0 剔除已取消/已驳回）。
        ⚠️ 无数据源列（后端返回 null、本页显示 -，不填 0 冒充）：
           含税单价 / 价税合计 / 税额 —— 预订货单表全表无税额、税率、含税金额列（已核 information_schema）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="onTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <!-- 查询方案（对标 `--查询方案--` 下拉 + 保存，落本机 localStorage） -->
          <QuerySchemeBar
            storage-key="analytics-pre-order-query-query-scheme"
            :snapshot="schemeSnapshot"
            @apply="applyScheme"
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
            <a-button v-if="isButtonEnabled('print')" size="small" @click="handlePrint">
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

        <!-- ═══ 查询区（对标：日期范围 + 客户 + 商品 + 经手人，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('preOrder.dateRange')" class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :allow-clear="false"
                  style="width: 240px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('preOrder.customerName')" class="search-item">
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="query.customerName"
                  size="small"
                  placeholder="客户名称"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('preOrder.keyword')" class="search-item">
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="商品名称/货号/条码"
                  allow-clear
                  style="width: 190px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('preOrder.handlerName')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.handlerName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置，逐 Tab 一套） ═══ -->
        <template #table>
          <div class="table-area">
            <a-alert
              class="redline-alert"
              type="warning"
              show-icon
              message="已取消、已驳回订单不参与统计"
            />
            <BillDetailTable
              :data-source="rows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              :storage-key="`analytics-pre-order-query-columns-${activeTab}`"
              :global-config-key="`analytics-pre-order-query-columns-${activeTab}`"
            >
              <template #imageCell="{ record }">
                <span v-if="record.image" class="cell-image">{{ record.image }}</span>
                <span v-else class="cell-empty">-</span>
              </template>
              <template #emptyCell>
                <span class="cell-empty">-</span>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.page"
            :page-size="pagination.size"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（对标有「页面配置」弹窗 → 接 PageConfigPanel） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
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
import { preOrderAnalysisApi } from '@/api/analytics-supply'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsPreOrderQuery' })

// ═══ 视图 Tab（顺序逐字取自对标：商品预订货分析 / 客户预订货分析） ═══
const TABS = [
  { key: 'product', label: '商品预订货分析' },
  { key: 'customer', label: '客户预订货分析' }
]
const activeTab = ref('product')

const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  customerName: '',
  keyword: '',
  handlerName: ''
})

const loading = ref(false)
const rows = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const summary = ref<Record<string, any>>({})

// ═══ 数值格式化 ═══
function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function fmtMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

type Kind = 'money' | 'num' | 'text'

const FORMATTER: Record<Kind, (v: any) => string> = {
  money: fmtMoney,
  num: fmtNum,
  text: (v: any) => (v === null || v === undefined || v === '' ? '-' : String(v))
}

function col(key: string, title: string, kind: Kind, width = 110, hidden = false): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: kind === 'text' ? 'left' : 'right',
    formatter: FORMATTER[kind],
    ...(hidden ? { defaultHidden: true } : {})
  }
}

/** 无数据源列：仍占列位与列名，取值为空并显示 `-` */
function gap(key: string, title: string, kind: Kind = 'money', width = 110, hidden = true): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: 'right',
    type: 'slot',
    slotName: 'emptyCell',
    ...(hidden ? { defaultHidden: true } : {})
  }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

// ── Tab1「商品预订货分析」：全部 28 列 / 默认 11 ──
const productColumns: DetailColumnConfig[] = [
  ROW_NO,
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 80, align: 'center' },
  col('dimLabel', '商品名称', 'text', 200),
  col('spec', '规格', 'text', 120, true),
  col('model', '型号', 'text', 110, true),
  col('origin', '产地', 'text', 100, true),
  col('barcode', '条码', 'text', 130, true),
  col('remark', '备注', 'text', 150, true),
  col('productCode', '货号', 'text', 110, true),
  col('brand', '品牌', 'text', 100, true),
  col('unit', '单位', 'text', 70),
  col('quantity', '预订货数量', 'num', 110),
  col('conversionResult', '换算结果', 'num', 100, true),
  col('smallUnit', '小单位', 'text', 90, true),
  col('smallUnitQty', '小单位数量', 'num', 110, true),
  col('unitPrice', '单价', 'money', 100, true),
  col('amount', '金额', 'money', 110),
  col('discountedPrice', '折后单价', 'money', 100, true),
  col('discountedAmount', '折后金额', 'money', 110, true),
  gap('taxUnitPrice', '含税单价', 'money', 100),
  gap('taxAmount', '价税合计', 'money', 110, false),
  gap('taxFee', '税额', 'money', 100),
  col('giftQty', '赠品数量', 'num', 100),
  col('giftOrderQty', '赠品订货数量', 'num', 110),
  col('giftShipQty', '赠品发货数量', 'num', 110),
  col('orderedQty', '已订数量', 'num', 100),
  col('unOrderedQty', '未订数量', 'num', 100, true),
  col('shippedQty', '已发数量', 'num', 100),
  col('unShippedQty', '未发数量', 'num', 100, true)
]

// ── Tab2「客户预订货分析」：全部 23 列 / 默认 6 ──
const customerColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '客户名称', 'text', 200),
  col('defaultHandler', '默认经手人', 'text', 110, true),
  col('customerCode', '客户编号', 'text', 110, true),
  col('customerLevel', '客户级别', 'text', 100, true),
  col('categoryName', '所属分类', 'text', 120, true),
  col('address', '地址', 'text', 180, true),
  col('warehouseName', '所属仓库', 'text', 120, true),
  col('region', '所属区域', 'text', 110, true),
  col('contactName', '联系人', 'text', 100, true),
  col('remark', '备注', 'text', 150, true),
  col('customerTicket', '客户一票通', 'text', 120, true),
  col('quantity', '预订货数量', 'num', 110),
  col('amount', '金额', 'money', 110),
  col('discountedAmount', '折后金额', 'money', 110, true),
  gap('taxAmount', '价税合计', 'money', 110, false),
  gap('taxFee', '税额', 'money', 100),
  col('orderedQty', '已订数量', 'num', 100),
  col('orderedAmount', '已订金额', 'money', 110, true),
  col('shippedQty', '已发数量', 'num', 100),
  col('shippedAmount', '已发金额', 'money', 110, true),
  col('receivedDeposit', '已收预订金', 'money', 110, true),
  col('unreceivedDeposit', '未收预订金', 'money', 110, true),
  col('depositBalance', '预订金余额', 'money', 110, true)
]

const activeColumns = computed<DetailColumnConfig[]>(() =>
  activeTab.value === 'customer' ? customerColumns : productColumns)

/** 底部合计行（后端按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['quantity', 'amount', 'discountedAmount', 'giftQty', 'giftOrderQty',
  'giftShipQty', 'orderedQty', 'shippedQty', 'orderedAmount', 'shippedAmount',
  'receivedDeposit', 'unreceivedDeposit', 'depositBalance', 'smallUnitQty']
const summaryColumns = computed(() => {
  const leaf = activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
  return SUMMARY_KEYS
    .filter(k => leaf.some(c => c.key === k))
    .map(k => ({ key: k, value: Number(summary.value[k]) || 0 }))
})

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'preOrder.dateRange', label: '日期', visible: true },
  { key: 'preOrder.customerName', label: '客户', visible: true },
  { key: 'preOrder.keyword', label: '商品', visible: true },
  { key: 'preOrder.handlerName', label: '经手人', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-pre-order-query-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 查询方案（本机 localStorage，方案内容 = 当前查询条件快照） ═══
function schemeSnapshot(): Record<string, any> {
  return { dateRange: [...(dateRange.value || [])], quickDate: quickDate.value, ...query }
}

/** 回填方案：日期区间与快捷段一并还原（方案里存的就是这两者 + 业务筛选） */
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  Object.keys(query).forEach(k => {
    if (k in v) (query as any)[k] = v[k]
  })
  handleSearch()
}

// ═══ 取数 ═══
async function fetchData() {
  loading.value = true
  try {
    const res = await preOrderAnalysisApi.page({
      tab: activeTab.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      customerName: query.customerName || undefined,
      keyword: query.keyword || undefined,
      handlerName: query.handlerName || undefined,
      page: pagination.page,
      size: pagination.size
    })
    rows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[预订货查询] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}

function handleReset() {
  query.customerName = ''
  query.keyword = ''
  query.handlerName = ''
  quickDate.value = 'month'
  dateRange.value = quickDateRange('month') as [string, string]
  handleSearch()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}

function onTabChange(key: string) {
  activeTab.value = key
  pagination.page = 1
  fetchData()
}

// ═══ 打印(F8) / 导出（当前 Tab 的默认可见列，所见即所打） ═══
const printableColumns = computed<DetailColumnConfig[]>(() => {
  const leaf = activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
  return leaf.filter(c => c.type !== 'rowNo' && !c.defaultHidden)
})

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  if (c.type === 'slot') return raw === null || raw === undefined || raw === '' ? '-' : String(raw)
  return c.formatter ? c.formatter(raw, r) : (raw === null || raw === undefined || raw === '' ? '-' : String(raw))
}

function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = rows.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1400,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '预订货查询'
  const html = `<html><head><meta charset="utf-8"><title>预订货查询-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>预订货查询 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）</h3>
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

const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 200
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res = await preOrderAnalysisApi.page({
      tab: activeTab.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      customerName: query.customerName || undefined,
      keyword: query.keyword || undefined,
      handlerName: query.handlerName || undefined,
      page: p,
      size
    })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `预订货查询-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[预订货查询] 页面异常', err)
}

onMounted(() => {
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
.redline-alert { margin-bottom: 8px; flex-shrink: 0; }
.cell-image { color: #1677ff; }
.cell-empty { color: #bfbfbf; }
</style>
