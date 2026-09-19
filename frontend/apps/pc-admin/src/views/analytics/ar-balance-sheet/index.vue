<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        往来余额表（分析 → 财务分析 → 往来余额表，菜单 80459）
        对标 ql361「往来余额表」：单视图，**多级表头**（仅一层 children），
        3 个固定列（操作 / 结算单位编号 / 结算单位）+ 5 分组 × 3 叶子（应收/预收/应付/预付的期初·本期·期末 + 往来合计）= 18 列全可见。
        ⚠️ 本页 `操作` **进入列配置弹窗**（对标特例：与《查应收/查应付》把操作当固定列不同），
           故其 key 不走 rowNo/checkbox/action 锁定判定。
        口径：期末 = 期初 + 本期；往来合计 = 应收 + 预付 − 预收 − 应付（正 = 对方欠我）。
        行级动作：清账（应收与应付对冲，经会计凭证）/ 对账（四象限来源流水）。
        工具栏另有「清账历史」。取数：/erp/finance/analytics/partner-balance/*（后端 PartnerLedgerController）。
      -->
      <CategoryListLayout
        :active-tab="'main'"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-ar-balance-sheet-query-scheme"
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

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜清账历史 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" :loading="loading" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-button size="small" @click="openReconcileHistory">
              <HistoryOutlined /> 清账历史
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标：日期范围 / 结算单位 + 3 勾选项 + 黄条提示；横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
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
              <div class="search-item">
                <span class="search-label">结算单位</span>
                <a-input
                  v-model:value="query.partnerName"
                  size="small"
                  placeholder="单位名称"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.onlyBoth" @change="handleSearch">仅显示既是客户又是供应商</a-checkbox>
                <a-checkbox v-model:checked="query.showDisabled" @change="handleSearch">显示停用</a-checkbox>
                <a-checkbox v-model:checked="query.showZero" @change="handleSearch">显示本期金额为0的数据</a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
            <div class="search-tip">只支持对应收、应付账款清账</div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置） ═══ -->
        <template #table>
          <div class="table-area" :data-cols="colSummary">
            <BillDetailTable
              :data-source="rows"
              :columns="COLUMNS"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="partnerName"
              storage-key="analytics-ar-balance-sheet-columns-main"
              global-config-key="analytics-ar-balance-sheet-columns-main"
            >
              <template #reconcileCell="{ record }">
                <a-space :size="2">
                  <a-button type="link" size="small" @click="openReconcile(record)">清账</a-button>
                  <a-button type="link" size="small" @click="openDetail(record)">对账</a-button>
                </a-space>
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

      <!-- ═══ 行级「对账」：四象限来源流水 ═══ -->
      <a-modal v-model:open="detailVisible" title="往来对账" :width="1000" :footer="null">
        <div class="detail-tip">结算单位：<b>{{ currentPartner }}</b>；下列为该单位四象限的往来流水（未结余额）。</div>
        <a-table
          :data-source="detailRows"
          :columns="DETAIL_COLUMNS"
          :loading="detailLoading"
          size="small"
          row-key="__key"
          :pagination="{ pageSize: 20, size: 'small' }"
          :scroll="{ y: 420 }"
        />
      </a-modal>

      <!-- ═══ 行级「清账」：应收与应付对冲 ═══ -->
      <a-modal
        v-model:open="reconcileVisible"
        title="应收应付清账"
        :width="520"
        :confirm-loading="reconcileLoading"
        ok-text="确认清账"
        @ok="submitReconcile"
      >
        <a-form layout="vertical">
          <a-form-item label="结算单位">
            <a-input :value="currentPartner" disabled />
          </a-form-item>
          <a-form-item label="应收余额 / 应付余额">
            <span>{{ fmtMoney(currentRecord?.arEnd) }} / {{ fmtMoney(currentRecord?.apEnd) }}</span>
          </a-form-item>
          <a-form-item label="清账金额">
            <a-input-number v-model:value="reconcileAmount" :min="0.01" :precision="2" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="reconcileRemark" placeholder="选填" :maxlength="60" />
          </a-form-item>
          <div class="detail-tip">清账经会计凭证记账（Dr 应付账款 / Cr 应收账款），并同步调减应收与应付余额。</div>
        </a-form>
      </a-modal>

      <!-- ═══ 工具栏「清账历史」 ═══ -->
      <a-modal v-model:open="historyVisible" title="清账历史" :width="900" :footer="null">
        <a-table
          :data-source="historyRows"
          :columns="HISTORY_COLUMNS"
          :loading="historyLoading"
          size="small"
          row-key="docNo"
          :pagination="{ pageSize: 20, size: 'small' }"
        />
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined, HistoryOutlined, PrinterOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { partnerLedgerApi } from '@/api/analytics-finance'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'

defineOptions({ name: 'AnalyticsArBalanceSheet' })

const quickDate = ref('week')
const dateRange = ref<[string, string]>(quickDateRange('week') as [string, string])
const query = reactive({
  partnerName: '',
  onlyBoth: false,
  showDisabled: false,
  showZero: false
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
function fmtText(v: any): string {
  return v === null || v === undefined || v === '' ? '-' : String(v)
}

function moneyCol(key: string, title: string, width = 130): DetailColumnConfig {
  return { key, title, width, align: 'right', formatter: fmtMoney }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }
/** 操作列：本页属列配置第 1 项（对标特例），故不使用 rowNo/checkbox/action 锁定 key */
const OP_ACTION: DetailColumnConfig = {
  key: 'opAction', title: '操作', type: 'slot', slotName: 'reconcileCell', width: 120, fixed: 'left', align: 'left'
}

/** 单象限三列（期初 / 本期 / 期末） */
function quadGroup(key: string, title: string, prefix: string, labels: [string, string, string]): DetailColumnConfig {
  return {
    key,
    title,
    children: [
      moneyCol(`${prefix}Begin`, labels[0], 130),
      moneyCol(`${prefix}Period`, labels[1], 120),
      moneyCol(`${prefix}End`, labels[2], 130)
    ]
  }
}

// ── 全部 18 列 / 默认 18（全可见），多级表头仅一层 ──
const COLUMNS: DetailColumnConfig[] = [
  ROW_NO,
  OP_ACTION,
  { key: 'partnerCode', title: '结算单位编号', width: 130, align: 'left', formatter: fmtText },
  { key: 'partnerName', title: '结算单位', width: 200, align: 'left', formatter: fmtText },
  quadGroup('arGroup', '应收金额', 'ar', ['期初应收余额', '本期应收', '期末应收余额']),
  quadGroup('preReceiptGroup', '预收金额', 'preReceipt', ['期初预收金额', '本期预收', '期末预收金额']),
  quadGroup('apGroup', '应付金额', 'ap', ['期初应付金额', '本期应付', '期末应付金额']),
  quadGroup('prePayGroup', '预付金额', 'prePay', ['期初预付金额', '本期预付', '期末预付金额']),
  quadGroup('netGroup', '往来合计', 'net', ['期初余额', '本期发生', '期末余额'])
]

const leafColumns = computed<DetailColumnConfig[]>(() =>
  COLUMNS.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 18/18） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

/** 底部合计行：取后端 summary（按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['arBegin', 'arPeriod', 'arEnd', 'preReceiptBegin', 'preReceiptPeriod', 'preReceiptEnd',
  'apBegin', 'apPeriod', 'apEnd', 'prePayBegin', 'prePayPeriod', 'prePayEnd',
  'netBegin', 'netPeriod', 'netEnd']
const summaryColumns = computed(() =>
  SUMMARY_KEYS.filter(k => leafColumns.value.some(c => c.key === k))
    .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 查询方案 ═══
function schemeSnapshot(): Record<string, any> {
  return { dateRange: [...(dateRange.value || [])], quickDate: quickDate.value, ...query }
}
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  Object.keys(query).forEach(k => { if (k in v) (query as any)[k] = v[k] })
  handleSearch()
}

// ═══ 取数 ═══
function buildParams(extra: Record<string, any> = {}) {
  return {
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    partnerName: query.partnerName || undefined,
    onlyBoth: query.onlyBoth || undefined,
    showDisabled: query.showDisabled || undefined,
    showZero: query.showZero || undefined,
    page: pagination.page,
    size: pagination.size,
    ...extra
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await partnerLedgerApi.page(buildParams())
    rows.value = (res?.records || []).map((r: any, i: number) => ({ ...r, __rowKey: `${r.partnerName}-${i}` }))
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[往来余额表] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.page = 1; fetchData() }
function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}
function handleReset() {
  query.partnerName = ''
  query.onlyBoth = false
  query.showDisabled = false
  query.showZero = false
  quickDate.value = 'week'
  dateRange.value = quickDateRange('week') as [string, string]
  handleSearch()
}
function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}

// ═══ 行级「对账」 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRows = ref<any[]>([])
const currentPartner = ref('')
const currentRecord = ref<any>(null)
const DETAIL_COLUMNS = [
  { title: '象限', dataIndex: 'quadrant', width: 110 },
  { title: '单据编号', dataIndex: 'docNo', width: 200 },
  { title: '单据日期', dataIndex: 'bizDate', width: 120 },
  { title: '未结余额', dataIndex: 'balance', width: 130, align: 'right' as const }
]

async function openDetail(record: any) {
  currentPartner.value = record.partnerName
  detailVisible.value = true
  detailLoading.value = true
  detailRows.value = []
  try {
    const res: any = await partnerLedgerApi.detail({ partnerName: record.partnerName })
    const list: any[] = []
    ;[['receivables', '应收'], ['preReceipts', '预收'], ['payables', '应付'], ['prePayments', '预付']]
      .forEach(([k, label]) => {
        for (const r of (res?.[k] || [])) list.push({ ...r, quadrant: label })
      })
    detailRows.value = list.map((r, i) => ({ ...r, __key: `${r.quadrant}-${r.docNo}-${i}` }))
    if (!list.length) message.info('该单位在当前区间内没有往来流水')
  } catch (e) {
    console.warn('[往来余额表] 对账取数失败', e)
    message.error('获取对账明细失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 行级「清账」 ═══
const reconcileVisible = ref(false)
const reconcileLoading = ref(false)
const reconcileAmount = ref<number | undefined>(undefined)
const reconcileRemark = ref('')

function openReconcile(record: any) {
  currentRecord.value = record
  currentPartner.value = record.partnerName
  const max = Math.min(Number(record.arEnd) || 0, Math.abs(Number(record.apEnd) || 0))
  reconcileAmount.value = max > 0 ? Number(max.toFixed(2)) : undefined
  reconcileRemark.value = ''
  reconcileVisible.value = true
}

async function submitReconcile() {
  if (!reconcileAmount.value || reconcileAmount.value <= 0) {
    message.warning('请填写大于 0 的清账金额')
    return
  }
  reconcileLoading.value = true
  try {
    const res = await partnerLedgerApi.reconcile(currentPartner.value, reconcileAmount.value, reconcileRemark.value || undefined)
    message.success(`清账成功，单号 ${res?.docNo || ''}`)
    reconcileVisible.value = false
    fetchData()
  } catch (e: any) {
    console.warn('[往来余额表] 清账失败', e)
    message.error(e?.response?.data?.message || e?.message || '清账失败')
  } finally {
    reconcileLoading.value = false
  }
}

// ═══ 工具栏「清账历史」 ═══
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyRows = ref<any[]>([])
const HISTORY_COLUMNS = [
  { title: '清账单号', dataIndex: 'docNo', width: 200 },
  { title: '清账日期', dataIndex: 'bizDate', width: 130 },
  { title: '结算单位', dataIndex: 'partnerName', width: 200 },
  { title: '清账金额', dataIndex: 'amount', width: 130, align: 'right' as const }
]

async function openReconcileHistory() {
  historyVisible.value = true
  historyLoading.value = true
  historyRows.value = []
  try {
    const res = await partnerLedgerApi.reconcileHistory(1, 100)
    historyRows.value = res?.records || []
    if (!historyRows.value.length) message.info('暂无清账记录')
  } catch (e) {
    console.warn('[往来余额表] 清账历史取数失败', e)
    message.error('获取清账历史失败')
  } finally {
    historyLoading.value = false
  }
}

// ═══ 打印(F8) / 导出 ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && !c.defaultHidden))

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : fmtText(raw)
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
  const html = `<html><head><meta charset="utf-8"><title>往来余额表</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>往来余额表（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

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
    const res = await partnerLedgerApi.page(buildParams({ page: p, size }))
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: '往来余额表',
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[往来余额表] 页面异常', err)
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
.search-tip {
  margin-top: 6px; padding: 2px 8px; display: inline-block;
  background: #fffbe6; border: 1px solid #ffe58f; border-radius: 2px;
  color: #ad6800; font-size: 12px;
}

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.detail-tip { margin-bottom: 8px; color: #666; font-size: 12px; }
</style>
