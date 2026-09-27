<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
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

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 批量确认 + 打印(F8) + 导出 ═══ -->
        <!-- 列配置走数据表表头齿轮（BillDetailTable 内置），工具栏不再放重复入口 -->
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
            <a-button
              type="primary"
              size="small"
              :disabled="selectedRows.length === 0"
              @click="handleBatchConfirm"
            >
              <CheckOutlined /> 批量确认
            </a-button>
            <a-button size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（按页面配置动态渲染） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template v-for="field in visibleQueryFields" :key="field.key">
                <div class="search-field-item">
                  <template v-if="field.key === 'date'">
                    <a-range-picker
                      v-model:value="dateRange"
                      size="small"
                      style="width: 220px"
                      @change="handleDateChange"
                    />
                  </template>
                  <a-input
                    v-else
                    v-model:value="searchValues[field.key]"
                    :placeholder="field.label"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
              </template>
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
              ref="tableRef"
              :columns="currentColumns"
              :data-source="tableData"
              :storage-key="activeTab === 'receipt' ? 'pending-confirm-table-columns-receipt' : 'pending-confirm-table-columns-prereceipt'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              row-key="rowKey"
              @page-change="handlePageChange"
              @selection-change="onSelectionChange"
            >
              <template #statusCell="{ record }">
                <a-tag :color="statusColor(record.status)">
                  {{ statusLabel(record.status, record.statusDesc) }}
                </a-tag>
              </template>
              <template #actionCell="{ record }">
                <a-popconfirm
                  v-if="canConfirm(record)"
                  :title="confirmTitle"
                  @confirm="handleConfirm(record)"
                >
                  <a-button type="link" size="small">确认</a-button>
                </a-popconfirm>
                <span v-else>-</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（仅「查询条件」Tab） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFieldDefs"
      :storage-key="pageConfigStorageKey"
      :fields-only="true"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="finance-pending-confirm"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, SettingOutlined, ReloadOutlined, PrinterOutlined, ExportOutlined, CheckOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { receiptApi, preReceiptApi } from '@/api/finance'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'FinancePendingConfirm' })

// ═══ 方向 Tab（收款待确认 / 预收款待确认） ═══
const tabs = [
  { key: 'receipt', label: '收款待确认' },
  { key: 'pre', label: '预收款待确认' },
]
const activeTab = ref<'receipt' | 'pre'>('receipt')

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
const selectedRows = ref<any[]>([])
const tableRef = ref<any>()

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数 ═══
const searchValues = reactive<Record<string, string>>({})
const dateStart = ref(dayjs().subtract(7, 'day').format('YYYY-MM-DD'))
const dateEnd = ref(dayjs().format('YYYY-MM-DD'))

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const tableData = computed(() => allRows.value)

// ═══ 页面配置弹窗（列配置走数据表表头齿轮，storage-key=pending-confirm-table-columns-*） ═══
const showPageConfig = ref(false)

// ═══ 列定义 ═══
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
  formatter?: (v: any, record: any) => string
  sortable?: boolean
}

function moneyFormatter(v: any): string {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function makeReceiptColumns(): ColumnDef[] {
  return [
    { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
    { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
    { title: '单据日期', key: 'docDate', field: 'docDate', width: 110 },
    { title: '单据编号', key: 'docNo', field: 'docNo', width: 170 },
    { title: '往来单位', key: 'partyName', field: 'partyName', width: 170 },
    { title: '单据状态', key: 'status', field: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
    { title: '金额', key: 'amount', field: 'amount', width: 130, align: 'right', formatter: moneyFormatter },
    { title: '收款账户1', key: 'account1', field: 'account1', width: 140 },
    { title: '收款金额1', key: 'amount1', field: 'amount1', width: 130, align: 'right', formatter: moneyFormatter },
    { title: '收款账户2', key: 'account2', field: 'account2', width: 140 },
    { title: '收款金额2', key: 'amount2', field: 'amount2', width: 130, align: 'right', formatter: moneyFormatter },
    { title: '收款账户3', key: 'account3', field: 'account3', width: 140, defaultHidden: true },
    { title: '收款金额3', key: 'amount3', field: 'amount3', width: 130, align: 'right', formatter: moneyFormatter, defaultHidden: true },
    { title: '收款账户4', key: 'account4', field: 'account4', width: 140, defaultHidden: true },
    { title: '收款金额4', key: 'amount4', field: 'amount4', width: 130, align: 'right', formatter: moneyFormatter, defaultHidden: true },
    { title: '来源订单', key: 'orderNo', field: 'orderNo', width: 170 },
    { title: '配送任务编号', key: 'deliveryNo', field: 'deliveryNo', width: 150 },
    { title: '经手人', key: 'handlerName', field: 'handlerName', width: 110 },
    { title: '制单人', key: 'creatorName', field: 'creatorName', width: 110, defaultHidden: true },
    { title: '摘要', key: 'summary', field: 'summary', width: 180, defaultHidden: true },
    { title: '凭证', key: 'voucherNo', field: 'voucherNo', width: 130, defaultHidden: true },
    { title: '单据备注', key: 'remark', field: 'remark', width: 180, defaultHidden: true },
    { title: '制单时间', key: 'createTime', field: 'createTime', width: 170, defaultHidden: true },
    { title: '操作', key: 'action', type: 'slot', slotName: 'actionCell', width: 100, fixed: 'right' },
  ]
}

function makePreColumns(): ColumnDef[] {
  return [
    { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
    { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
    { title: '单据日期', key: 'docDate', field: 'docDate', width: 110 },
    { title: '单据编号', key: 'docNo', field: 'docNo', width: 170 },
    { title: '往来单位', key: 'partyName', field: 'partyName', width: 170 },
    { title: '单据状态', key: 'status', field: 'status', width: 90, align: 'center', type: 'slot', slotName: 'statusCell' },
    { title: '金额', key: 'amount', field: 'amount', width: 130, align: 'right', formatter: moneyFormatter },
    { title: '收款账户', key: 'account', field: 'account', width: 140 },
    { title: '收款金额', key: 'accountAmount', field: 'amount', width: 130, align: 'right', formatter: moneyFormatter },
    { title: '来源订单', key: 'orderNo', field: 'orderNo', width: 170 },
    { title: '经手人', key: 'handlerName', field: 'handlerName', width: 110 },
    { title: '制单人', key: 'creatorName', field: 'creatorName', width: 110, defaultHidden: true },
    { title: '摘要', key: 'summary', field: 'summary', width: 180, defaultHidden: true },
    { title: '凭证', key: 'voucherNo', field: 'voucherNo', width: 130, defaultHidden: true },
    { title: '单据备注', key: 'remark', field: 'remark', width: 180, defaultHidden: true },
    { title: '制单时间', key: 'createTime', field: 'createTime', width: 170, defaultHidden: true },
    { title: '操作', key: 'action', type: 'slot', slotName: 'actionCell', width: 100, fixed: 'right' },
  ]
}

const receiptColumnDefs = makeReceiptColumns()
const preColumnDefs = makePreColumns()

const currentColumns = computed(() =>
  activeTab.value === 'receipt' ? receiptColumnDefs : preColumnDefs
)

// ═══ 页面配置（查询条件显隐） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[] }

const RECEIPT_STORAGE = 'pending-confirm-page-config-receipt'
const PRE_STORAGE = 'pending-confirm-page-config-pre'

const RECEIPT_QUERY_FIELD_DEFS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'customerName', label: '往来单位', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'sourceNo', label: '来源订单', visible: true },
  { key: 'deliveryNo', label: '配送任务编号', visible: true },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'account1', label: '收款账户1', visible: false },
  { key: 'account2', label: '收款账户2', visible: false },
]

const PRE_QUERY_FIELD_DEFS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'customerName', label: '往来单位', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'sourceNo', label: '来源订单', visible: true },
  { key: 'account', label: '收款账户', visible: true },
  { key: 'departmentName', label: '部门', visible: false },
  { key: 'creatorName', label: '制单人', visible: false },
]

const receiptQueryFields = ref<QueryFieldSetting[]>(RECEIPT_QUERY_FIELD_DEFS.map(f => ({ ...f })))
const preQueryFields = ref<QueryFieldSetting[]>(PRE_QUERY_FIELD_DEFS.map(f => ({ ...f })))

const activeQueryFieldDefs = computed(() =>
  activeTab.value === 'receipt' ? RECEIPT_QUERY_FIELD_DEFS : PRE_QUERY_FIELD_DEFS
)
const activeQueryFieldsRef = computed(() =>
  activeTab.value === 'receipt' ? receiptQueryFields : preQueryFields
)
const visibleQueryFields = computed(() =>
  (activeTab.value === 'receipt' ? receiptQueryFields.value : preQueryFields.value).filter(f => f.visible)
)
const pageConfigStorageKey = computed(() =>
  activeTab.value === 'receipt' ? RECEIPT_STORAGE : PRE_STORAGE
)

function loadPageConfig() {
  loadPageConfigFor(RECEIPT_STORAGE, receiptQueryFields, RECEIPT_QUERY_FIELD_DEFS)
  loadPageConfigFor(PRE_STORAGE, preQueryFields, PRE_QUERY_FIELD_DEFS)
}
function loadPageConfigFor(storage: string, target: { value: QueryFieldSetting[] }, defs: QueryFieldSetting[]) {
  try {
    const raw = localStorage.getItem(storage)
    if (!raw) return
    const parsed = JSON.parse(raw) as PageConfigData
    if (parsed.queryFields) {
      target.value = defs.map(df => {
        const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    }
  } catch {
    // ignore
  }
}

function handlePageConfigChange(config: any) {
  const storage = pageConfigStorageKey.value
  const target = activeQueryFieldsRef.value
  const fields = (config.queryFields || []).map((f: QueryFieldSetting) => ({ ...f }))
  localStorage.setItem(storage, JSON.stringify({ queryFields: fields }))
  // 合并默认（保留未出现在配置里的字段）
  target.value = activeQueryFieldDefs.value.map(df => {
    const saved = fields.find((f: QueryFieldSetting) => f.key === df.key)
    return saved ? { ...df, ...saved } : { ...df }
  })
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'receipt') {
      const params: any = {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        statuses: '4,5', // 待核销 + 核销中（已申报未到账）
      }
      applySearchParams(params, 'receipt')
      const res: any = await receiptApi.getPage(params)
      allRows.value = (res?.records || res?.list || []).map((r: any) => mapReceiptRow(r))
      pagination.total = Number(res?.total) || allRows.value.length
    } else {
      const params: any = {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        status: 'draft', // 草稿待确认
      }
      applySearchParams(params, 'pre')
      const res: any = await preReceiptApi.getPage(params)
      allRows.value = (res?.records || res?.list || []).map((r: any) => mapPreRow(r))
      pagination.total = Number(res?.total) || allRows.value.length
    }
    selectedRows.value = []
    tableRef.value?.clearSelection?.()
  } catch (error: any) {
    console.warn('[待确认款项] 获取数据失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
    allRows.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function applySearchParams(params: Record<string, any>, tab: 'receipt' | 'pre') {
  if (dateStart.value) params[tab === 'receipt' ? 'startDate' : 'dateStart'] = dateStart.value
  if (dateEnd.value) params[tab === 'receipt' ? 'endDate' : 'dateEnd'] = dateEnd.value
  ;(['customerName', 'handlerName', 'departmentName', 'creatorName'] as const).forEach(k => {
    if (searchValues[k]) params[k] = searchValues[k]
  })
  if (tab === 'receipt') {
    if (searchValues.docNo) params.receiptNo = searchValues.docNo
    if (searchValues.sourceNo) params.orderNo = searchValues.sourceNo
    if (searchValues.deliveryNo) params.deliveryNo = searchValues.deliveryNo
    if (searchValues.account1) params.receiptAccount1 = searchValues.account1
    if (searchValues.account2) params.receiptAccount2 = searchValues.account2
  } else {
    if (searchValues.docNo) params.preReceiptNo = searchValues.docNo
    if (searchValues.sourceNo) params.sourceNo = searchValues.sourceNo
    if (searchValues.account) params.bankAccount = searchValues.account
  }
}

function mapReceiptRow(r: any): any {
  return {
    rowKey: `receipt-${r.id}`,
    id: r.id,
    docNo: r.receiptNo,
    partyName: r.customerName,
    docDate: r.receiptDate,
    status: r.status,
    statusDesc: r.statusDesc,
    amount: r.receiptAmount,
    account1: r.receiptAccount1,
    amount1: r.receiptAmount1,
    account2: r.receiptAccount2,
    amount2: r.receiptAmount2,
    account3: r.receiptAccount3,
    amount3: r.receiptAmount3,
    account4: r.receiptAccount4,
    amount4: r.receiptAmount4,
    orderNo: r.orderNo,
    deliveryNo: r.deliveryNo,
    handlerName: r.salesPersonName,
    creatorName: r.creatorName,
    summary: r.summary,
    voucherNo: r.voucherNo,
    remark: r.remark,
    createTime: r.createTime,
  }
}

function mapPreRow(r: any): any {
  return {
    rowKey: `pre-${r.id}`,
    id: r.id,
    docNo: r.preReceiptNo,
    partyName: r.customerName,
    docDate: r.receiptDate,
    status: r.status,
    statusDesc: r.status,
    amount: r.amount,
    accountAmount: r.amount,
    account: r.bankAccount,
    orderNo: r.sourceNo,
    handlerName: r.handlerName,
    creatorName: r.creatorName,
    summary: r.summary,
    voucherNo: r.voucherNo,
    remark: r.remark,
    createTime: r.createTime,
  }
}

// ═══ 状态显示 ═══
const RECEIPT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已拒绝', color: 'red' },
  4: { label: '待核销', color: 'orange' },
  5: { label: '核销中', color: 'orange' },
  7: { label: '已完成', color: 'green' },
  6: { label: '已核销', color: 'green' },
  8: { label: '已取消', color: 'red' },
}
const PRE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  draft: { label: '待确认', color: 'orange' },
  confirmed: { label: '已确认', color: 'green' },
  received: { label: '已收款', color: 'green' },
  offset: { label: '已冲抵', color: 'blue' },
  forfeited: { label: '已没收', color: 'red' },
  refunded: { label: '已退还', color: 'default' },
}

function statusColor(status: any): string {
  if (activeTab.value === 'receipt') return RECEIPT_STATUS_MAP[status]?.color || 'default'
  return PRE_STATUS_MAP[status]?.color || 'default'
}
function statusLabel(status: any, desc?: string): string {
  if (activeTab.value === 'receipt') return RECEIPT_STATUS_MAP[status]?.label || desc || String(status ?? '')
  return PRE_STATUS_MAP[status]?.label || desc || String(status ?? '')
}

function canConfirm(record: any): boolean {
  if (activeTab.value === 'receipt') return [4, 5].includes(record.status)
  return record.status === 'draft'
}

const confirmTitle = computed(() =>
  activeTab.value === 'receipt' ? '确认该笔款项到账入账？' : '确认预收款入账？'
)

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key as 'receipt' | 'pre'
  pagination.current = 1
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
  fetchData()
}

function onSelectionChange(rows: any[]) {
  selectedRows.value = rows
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
  dateStart.value = start.format('YYYY-MM-DD')
  dateEnd.value = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    dateStart.value = dates[0]?.format('YYYY-MM-DD') || ''
    dateEnd.value = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    dateStart.value = ''
    dateEnd.value = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 确认动作 ═══
async function handleConfirm(record: any) {
  try {
    if (activeTab.value === 'receipt') await receiptApi.completeVerify(record.id)
    else await preReceiptApi.confirm(record.id)
    message.success('确认成功')
    fetchData()
  } catch (e) {
    console.warn('[待确认款项] 确认失败', e)
    message.error('确认失败，请重试')
  }
}

async function handleBatchConfirm() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要确认的记录')
    return
  }
  const ids = selectedRows.value.map(r => r.id)
  try {
    if (activeTab.value === 'receipt') await receiptApi.batchConfirm(ids)
    else await preReceiptApi.batchConfirm(ids)
    message.success(`已确认 ${ids.length} 笔`)
    fetchData()
  } catch (e) {
    console.warn('[待确认款项] 批量确认失败', e)
    message.error('批量确认失败，请重试')
  }
}

// ═══ 工具栏操作 ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint: handlePrintF8 } = useListPrint({
  pageCode: 'finance-pending-confirm',
  // 列是 computed（随 Tab / 列配置变），静态生成器写不进模板 → 明确按数据列打
  useDataColumns: true,
  title: '待确认',
  columns: () => currentColumns,
  rows: () => allRows.value,
  selectedRows: () => selectedRows.value,
  emptyTip: '没有可打印的数据',
})

async function handleExport() {
  if (allRows.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const cols = currentColumns.value.filter((c: any) => c.key !== 'rowNo' && c.title)
  const header = cols.map((c: any) => c.title).join(',')
  const lines = allRows.value.map((r: any) => cols.map((c: any) => {
    const v = r[c.field]
    return v === null || v === undefined ? '' : String(v).replace(/,/g, '')
  }).join(','))
  const csv = '\uFEFF' + [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `待确认款项-${activeTab.value === 'receipt' ? '收款待确认' : '预收款待确认'}-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 工具 ═══
function handleError(error: Error) {
  console.error('[待确认款项] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('week')
  fetchData()
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
.cell-link { color: #1668dc; }
:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
