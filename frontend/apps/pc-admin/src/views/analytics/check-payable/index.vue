<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        查应付（分析 → 财务分析 → 查应付，菜单 80458）
        对标 ql361：结算单位级**应付滚动台账**（全部可配置列 21 / 默认 8）+ 行级「付款 / 对账」+ 合计行。
        列名与顺序逐字取自《查应付开发文档》§3，defaultHidden 个数 = 21 − 8 = 13。

        取数（真实接口，无硬编码数据）：
          · 主表 auxiliaryBalanceApi.getPage → /erp/finance/auxiliary/balance/page
            auxType=SUPPLIER + subjectCode=2202（应付账款），按结算单位给出四段余额：
            期初借贷 / 本期借贷 / 本年借贷 / 期末借贷 —— 对应对标「期初应付 / 本期应付款 / 本期已结 / 应付余额」。
          · 辅表 financeAnalyticsApi.partnerBalancePage → /erp/finance/partner-balance/page
            按结算单位名补齐「预付余额」与对方ID，用于「期末余额 = 应付余额 − 预付余额」与「对账」明细。
        后端缺口（见开发文档 §5，已在汇报中列出）：无「优惠 / 付款合计 / 信用额度 / 可用额度 / 动态付款期限 /
        固定账期 / 结算期 / 联系人 / 联系电话 / 供应商备注 / 已开票未付款」等列的数据源，
        这些列以空白呈现（不做假数据填充）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-check-payable\index.vue-query-scheme"
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

        <!-- ═══ 查询区（对标查询项，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('payable.dateRange')" class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="query.dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :allow-clear="false"
                  style="width: 230px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('payable.dateType')" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select
                  v-model:value="query.dateType"
                  size="small"
                  style="width: 120px"
                  disabled
                  title="后端辅助核算余额端点无日期类型条件，待补"
                  :options="DATE_TYPE_OPTIONS"
                />
              </div>
              <div v-if="isQueryVisible('payable.partnerName')" class="search-item">
                <span class="search-label">结算单位</span>
                <a-input
                  v-model:value="query.partnerName"
                  size="small"
                  placeholder="结算单位"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('payable.departmentName')" class="search-item">
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="query.departmentName"
                  size="small"
                  placeholder="部门"
                  allow-clear
                  disabled
                  style="width: 130px"
                  title="后端辅助核算余额端点无部门条件，待补"
                />
              </div>
              <div v-if="isQueryVisible('payable.handlerName')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.handlerName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  disabled
                  style="width: 130px"
                  title="后端辅助核算余额端点无经手人条件，待补"
                />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
            <div class="search-grid search-grid-sub">
              <a-checkbox v-model:checked="query.showZeroPeriod" @change="handleSearch">显示有发生期末为0的结算单位</a-checkbox>
              <a-checkbox v-model:checked="query.showAllPartners" @change="handleSearch">显示全部结算单位</a-checkbox>
              <a-checkbox
                v-model:checked="query.includeUnposted"
                disabled
                title="后端辅助核算余额端点无此口径开关，待补"
              >
                包含已审核未记账付款单/预付款单
              </a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置 + 合计行） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              storage-key="analytics-check-payable-columns"
              global-config-key="analytics-check-payable-columns"
            >
              <template #partnerCodeCell="{ record }">
                <span>{{ record.partnerCode || '-' }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="2">
                  <a-button type="link" size="small" @click="handlePayment(record)">付款</a-button>
                  <a-button type="link" size="small" @click="openReconcile(record)">对账</a-button>
                </a-space>
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

      <!-- ═══ 页面配置（对标有「查应付-页面配置弹窗」实测截图 → 接 PageConfigPanel） ═══ -->
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

      <!-- ═══ 对账（行级「对账」：四段余额明细 + 该结算单位应付单据明细，均为真实接口数据） ═══ -->
      <a-modal
        v-model:open="reconcileVisible"
        :title="`对账 · ${reconcileRow?.partnerName || ''}`"
        :width="820"
        :footer="null"
      >
        <a-descriptions
          bordered
          size="small"
          :column="3"
          class="reconcile-desc"
        >
          <a-descriptions-item label="期初应付">{{ formatMoney(reconcileRow?.openingPayable) }}</a-descriptions-item>
          <a-descriptions-item label="本期应付款">{{ formatMoney(reconcileRow?.currentPayable) }}</a-descriptions-item>
          <a-descriptions-item label="本期已结">{{ formatMoney(reconcileRow?.currentSettled) }}</a-descriptions-item>
          <a-descriptions-item label="本年借方">{{ formatMoney(reconcileRow?.yearDebit) }}</a-descriptions-item>
          <a-descriptions-item label="本年贷方">{{ formatMoney(reconcileRow?.yearCredit) }}</a-descriptions-item>
          <a-descriptions-item label="应付余额">{{ formatMoney(reconcileRow?.payableBalance) }}</a-descriptions-item>
          <a-descriptions-item label="预付余额">{{ formatMoney(reconcileRow?.prePaymentBalance) }}</a-descriptions-item>
          <a-descriptions-item label="期末余额">{{ formatMoney(reconcileRow?.endBalance) }}</a-descriptions-item>
          <a-descriptions-item label="科目">{{ reconcileRow?.subjectCode }} {{ reconcileRow?.subjectName }}</a-descriptions-item>
        </a-descriptions>

        <div class="reconcile-detail">
          <div class="reconcile-detail-title">
            应付明细
            <span v-if="!reconcileRow?.partnerId" class="reconcile-hint">（未匹配到结算单位主数据，暂无法联查应付单据明细）</span>
          </div>
          <a-table
            :columns="DETAIL_COLUMNS"
            :data-source="reconcileDetails"
            :loading="reconcileLoading"
            :pagination="false"
            row-key="id"
            size="small"
            :locale="{ emptyText: '暂无应付单据明细' }"
            :scroll="{ y: 240 }"
          >
            <template #bodyCell="{ column, text }">
              <template v-if="['totalAmount', 'paidAmount', 'remainingAmount'].includes(column.dataIndex as string)">
                {{ formatMoney(text) }}
              </template>
            </template>
          </a-table>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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
import { financeAnalyticsApi, payableApi } from '@/api/analytics'
import { auxiliaryBalanceApi } from '@/api/finance'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { formatMoney } from '../shared/docActions'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsCheckPayable' })

const router = useRouter()

const quickDate = ref('month')

/** 日期类型下拉（对标实测默认「单据日期」） */
const DATE_TYPE_OPTIONS = [
  { label: '单据日期', value: 'bizDate' },
  { label: '记账日期', value: 'bookkeepingDate' }
]

/** 查询态（对标：日期范围 + 日期类型 + 结算单位 + 部门 + 经手人 + 3 个勾选项） */
const query = reactive({
  dateRange: quickDateRange('month') as [string, string],
  dateType: 'bizDate',
  partnerName: '',
  departmentName: '',
  handlerName: '',
  showZeroPeriod: true,
  showAllPartners: true,
  includeUnposted: false
})

const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
/** 后端返回的四段余额合计（按当前过滤范围，非本页求和） */
const summaryRaw = ref<any>({})

/** 结算单位名 → { partnerId, prePayment }（来自往来余额表，用于预付余额与对账联查） */
const partnerMap = ref(new Map<string, { partnerId: string; prePayment: number }>())

function num(v: any): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

// ═══ 列定义（对标「查应付」21 列 / 默认 8，顺序逐字取自开发文档 §3） ═══
// defaultHidden 个数 = 21 − 8 = 13
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { key: 'partnerCode', title: '结算单位编号', type: 'slot', slotName: 'partnerCodeCell', width: 120, defaultHidden: true },
  { key: 'partnerName', title: '结算单位', width: 200 },
  { key: 'openingPayable', title: '期初应付', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'currentPayable', title: '本期应付款', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'currentSettled', title: '本期已结', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'discount', title: '优惠', width: 100, align: 'right', formatter: v => formatMoney(v) },
  { key: 'payableBalance', title: '应付余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  { key: 'openingPrePayment', title: '期初预付', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'currentPrePayment', title: '本期预付', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'prePaymentBalance', title: '预付余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  { key: 'endBalance', title: '期末余额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  { key: 'paymentTotal', title: '付款合计', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'creditLimit', title: '信用额度', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'availableLimit', title: '可用额度', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
  { key: 'dynamicPaymentTerm', title: '动态付款期限', width: 130, defaultHidden: true },
  { key: 'fixedAccountPeriod', title: '固定账期', width: 110, defaultHidden: true },
  { key: 'settlementPeriod', title: '结算期', width: 110, defaultHidden: true },
  { key: 'contactName', title: '联系人', width: 110, defaultHidden: true },
  { key: 'contactPhone', title: '联系电话', width: 130, defaultHidden: true },
  { key: 'supplierRemark', title: '供应商备注', width: 160, defaultHidden: true },
  { key: 'invoicedUnpaid', title: '已开票未付款', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true }
]

/** 合计行：四段余额取后端 summary（全量口径），预付/期末按已取到的结算单位求和 */
const summaryColumns = computed(() => {
  const s = summaryRaw.value || {}
  const preSum = dataSource.value.reduce((acc, r) => acc + (Number(r.prePaymentBalance) || 0), 0)
  const endSum = dataSource.value.reduce((acc, r) => acc + (Number(r.endBalance) || 0), 0)
  return [
    { key: 'openingPayable', value: num(s.beginCredit) - num(s.beginDebit) },
    { key: 'currentPayable', value: num(s.periodCredit) },
    { key: 'currentSettled', value: num(s.periodDebit) },
    { key: 'payableBalance', value: num(s.endCredit) - num(s.endDebit) },
    { key: 'prePaymentBalance', value: preSum },
    { key: 'endBalance', value: endSum }
  ]
})

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'payable.dateRange', label: '日期', visible: true },
  { key: 'payable.dateType', label: '日期类型', visible: true },
  { key: 'payable.partnerName', label: '结算单位', visible: true },
  { key: 'payable.departmentName', label: '部门', visible: true },
  { key: 'payable.handlerName', label: '经手人', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-check-payable-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 取数 ═══
/** 结算单位 → 预付余额/对方ID（往来余额表；失败不影响主表） */
async function loadPartnerMap() {
  try {
    const res: any = await financeAnalyticsApi.partnerBalancePage({ partnerType: 'supplier', page: 1, size: 500 })
    const next = new Map<string, { partnerId: string; prePayment: number }>()
    for (const r of res?.records || []) {
      if (r?.partnerName) next.set(String(r.partnerName), { partnerId: String(r.partnerId), prePayment: num(r.prePaymentBalance) })
    }
    partnerMap.value = next
  } catch (e) {
    console.warn('[查应付] 往来余额取数失败，预付余额列将为空白', e)
    partnerMap.value = new Map()
  }
}

/** 组装辅助核算余额查询参数（主表与导出共用，保证同一过滤口径） */
function buildAuxParams(pageNum: number, pageSize: number) {
  const [start, end] = query.dateRange
  return {
    auxType: 'SUPPLIER',
    subjectCode: '2202',
    startMonth: start?.slice(0, 7),
    endMonth: end?.slice(0, 7),
    keyword: query.partnerName || undefined,
    hideZeroBalance: query.showAllPartners ? undefined : true,
    hideNoPeriodAmount: query.showZeroPeriod ? undefined : true,
    pageNum,
    pageSize
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await auxiliaryBalanceApi.getPage(buildAuxParams(pagination.current, pagination.pageSize))
    const records = res?.records || []
    dataSource.value = records.map((r: any, idx: number) => {
      const openingPayable = num(r.beginCredit) - num(r.beginDebit)
      const payableBalance = num(r.endCredit) - num(r.endDebit)
      const mate = partnerMap.value.get(String(r.auxName))
      const prePaymentBalance = mate ? mate.prePayment : undefined
      return {
        rowKey: `${r.auxCode || r.auxName || 'row'}-${idx}`,
        partnerCode: r.auxCode,
        partnerName: r.auxName,
        partnerId: mate?.partnerId,
        subjectCode: r.subjectCode,
        subjectName: r.subjectName,
        openingPayable,
        currentPayable: num(r.periodCredit),
        currentSettled: num(r.periodDebit),
        payableBalance,
        yearDebit: num(r.yearDebit),
        yearCredit: num(r.yearCredit),
        prePaymentBalance,
        endBalance: prePaymentBalance === undefined ? undefined : payableBalance - prePaymentBalance
      }
    })
    pagination.total = Number(res?.total) || 0
    summaryRaw.value = res?.summary || {}
  } catch (e: any) {
    console.warn('[查应付] 取数失败', e)
    message.error('获取应付台账失败')
    dataSource.value = []
    pagination.total = 0
    summaryRaw.value = {}
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
  return fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  return fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const [start, end] = quickDateRange(key)
  query.dateRange = [start, end]
  handleSearch()
}

function handleRefresh() {
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    dateRange: quickDateRange('month'),
    dateType: 'bizDate',
    partnerName: '',
    departmentName: '',
    handlerName: '',
    showZeroPeriod: true,
    showAllPartners: true,
    includeUnposted: false
  })
  handleSearch()
}

// ═══ 行级动作 ═══
/** 付款：跳付款单表单页（财务域既有路由，不新建端点） */
function handlePayment(record: any) {
  router.push({ path: '/finance/payment-doc/form', query: { partnerName: record.partnerName } })
}

const reconcileVisible = ref(false)
const reconcileLoading = ref(false)
const reconcileRow = ref<any>(null)
const reconcileDetails = ref<any[]>([])
const DETAIL_COLUMNS = [
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 160 },
  { title: '应付总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '已付金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 110, align: 'right' },
  { title: '未付余额', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 110, align: 'right' },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 }
]

/** 对账：展示该结算单位四段余额（本行真实数据）+ 应付单据明细（payableApi 真实接口） */
async function openReconcile(record: any) {
  reconcileRow.value = record
  reconcileDetails.value = []
  reconcileVisible.value = true
  if (!record.partnerId) return
  reconcileLoading.value = true
  try {
    const res: any = await payableApi.getPage({ supplierId: record.partnerId, page: 1, size: 20 })
    reconcileDetails.value = res?.records || []
  } catch (e) {
    console.warn('[查应付] 应付明细取数失败', e)
    message.error('获取应付明细失败')
  } finally {
    reconcileLoading.value = false
  }
}

// ═══ 打印(F8) ═══
const printColumns = computed(() => columns.filter(c => c.key !== 'action' && c.key !== 'rowNo'))

function handlePrint() {
  const header = printColumns.value.map(c => c.title)
  const body = dataSource.value.map(r => printColumns.value.map(c => {
    const v = r[c.key]
    if (v === undefined || v === null) return ''
    return c.align === 'right' ? formatMoney(v) : String(v)
  }))
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const total = printColumns.value.map(c => {
    const found = summaryColumns.value.find(s => s.key === c.key)
    return found ? formatMoney(found.value) : ''
  })
  const html = `<html><head><meta charset="utf-8"><title>查应付</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}
    tfoot td{font-weight:600}</style></head><body>
    <h3>查应付（${query.dateRange[0]} ~ ${query.dateRange[1]}）</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    <tfoot><tr>${total.map((v, i) => `<td>${i === 0 ? '合计' : v}</td>`).join('')}</tr></tfoot>
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

// ═══ 导出（CSV；取当前过滤条件下的全量） ═══
const { execute: executeExport, exporting } = useExport()
const EXPORT_HEADERS = printColumns.value.map(c => c.title)
const EXPORT_FIELDS = printColumns.value.map(c => c.key)

/** 把主表记录整理成导出行（与列定义同口径） */
function normalize(r: any) {
  const mate = partnerMap.value.get(String(r.auxName))
  const prePaymentBalance = mate ? mate.prePayment : undefined
  const payableBalance = num(r.endCredit) - num(r.endDebit)
  return {
    partnerCode: r.auxCode,
    partnerName: r.auxName,
    openingPayable: num(r.beginCredit) - num(r.beginDebit),
    currentPayable: num(r.periodCredit),
    currentSettled: num(r.periodDebit),
    payableBalance,
    prePaymentBalance,
    endBalance: prePaymentBalance === undefined ? undefined : payableBalance - prePaymentBalance
  }
}

async function fetchAllRows(): Promise<any[]> {
  const size = 200
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await auxiliaryBalanceApi.getPage(buildAuxParams(p, size))
    const list = res?.records || []
    all.push(...list.map(normalize))
    if (list.length < size) break
  }
  return all
}

function toExportRow(r: any): string[] {
  return EXPORT_FIELDS.map(k => {
    const v = r[k]
    if (v === undefined || v === null) return ''
    return (k === 'partnerCode' || k === 'partnerName') ? String(v) : formatMoney(v)
  })
}

function handleExport() {
  executeExport({
    fileName: '查应付',
    headers: EXPORT_HEADERS,
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(toExportRow),
    fallbackRows: () => dataSource.value.map(toExportRow)
  })
}

function handleError(err: any) {
  console.error('[查应付] 页面异常', err)
}

onMounted(async () => {
  await loadPartnerMap()
  await fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不在 scoped 作用域内，样式随页面自带（见《插槽内容的样式必须自备》） */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-grid-sub { margin-top: 8px; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.reconcile-desc { margin-bottom: 12px; }
.reconcile-detail-title { margin-bottom: 8px; font-weight: 600; }
.reconcile-hint { font-weight: 400; color: #999; font-size: 12px; }
</style>
