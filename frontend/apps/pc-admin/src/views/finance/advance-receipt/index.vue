<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab栏 + 工具栏 ═══ -->
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
              <a-select-option value="">
                --查询方案--
              </a-select-option>
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

        <!-- ═══ 工具栏右侧：页面配置/新增/刷新/打印(F8)/导出（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增
            </a-button>
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

        <!-- ═══ 搜索区域（12 查询条件） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid" ref="searchGridRef">
                <div class="search-field-item">
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleDateChange"
                  />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.preReceiptNo" placeholder="单据编号" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.customerName" placeholder="结算单位" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.bookkeeperName" placeholder="记账人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.auditorName" placeholder="审核人" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="searchParams.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="draft">草稿</a-select-option>
                      <a-select-option value="confirmed">已记账</a-select-option>
                      <a-select-option value="received">已收款</a-select-option>
                      <a-select-option value="offset">已冲抵</a-select-option>
                      <a-select-option value="forfeited">已没收</a-select-option>
                      <a-select-option value="refunded">已退款</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.sourceNo" placeholder="来源订单" allow-clear size="small" />
                </div>
                <div class="search-action-group" ref="searchActionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                  <div class="search-field-item search-action-item">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  </div>
                  <div class="search-field-item">
                    <a-checkbox v-model:checked="searchParams.showRed">显示红冲</a-checkbox>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'advance-receipt-table-columns'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :row-selection="rowSelection"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #preReceiptNoCell="{ record }">
                <a-button type="link" size="small" @click="handleView(record)">
                  {{ record.preReceiptNo }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
              </template>
              <template #amountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.amount) }}</span>
              </template>
              <template #giftAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.giftAmount) }}</span>
              </template>
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount ?? record.amount) }}</span>
              </template>
              <template #printCountCell="{ record }">
                <span class="currency-value">{{ record.printCount ?? 0 }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button v-if="['draft'].includes(record.status)" type="link" size="small" @click="handleEdit(record)">修改</a-button>
                  <a-button
                    v-if="['draft'].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleConfirm(record)"
                  >记账</a-button>
                  <a-button
                    v-if="['confirmed', 'received'].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleForfeit(record)"
                  >没收</a-button>
                  <a-button
                    v-if="['confirmed', 'received'].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleRefund(record)"
                  >退款</a-button>
                </a-space>
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
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { preReceiptApi } from '@/api/finance'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// ═══ Tab 配置（单 Tab：预收款单-历史） ═══
const tabs = [{ key: 'doc', label: '预收款单-历史' }]
const activeTab = ref('doc')

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
const tableData = ref<any[]>([])

const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])

// ═══ 搜索参数（12 查询条件） ═══
const searchParams = reactive({
  preReceiptNo: '',
  customerName: '',
  sourceNo: '',
  handlerName: '',
  deptName: '',
  creatorName: '',
  bookkeeperName: '',
  auditorName: '',
  remark: '',
  status: undefined as string | undefined,
  showRed: false,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

// ═══ 页面配置弹窗（列配置走数据表表头齿轮，storage-key=advance-receipt-table-columns） ═══
const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'advance-receipt-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true },
  { key: 'preReceiptNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '结算单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'bookkeeperName', label: '记账人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'sourceNo', label: '来源订单', visible: true },
  { key: 'showRed', label: '显示红冲', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() => queryConfig.value)
const pageConfigStorageKey = computed(() => PAGE_CONFIG_STORAGE_KEY)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.queryFields) {
        queryConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
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
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

// ═══ 列定义（20 列，默认显示 8 列） ═══
const allColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 190, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'receiptDate', key: 'receiptDate', width: 110, sortable: true },
  { title: '单据编号', field: 'preReceiptNo', key: 'preReceiptNo', width: 170, type: 'slot', slotName: 'preReceiptNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '结算单位', field: 'customerName', key: 'customerName', width: 180, sortable: true },
  { title: '结算单位编号', field: 'partnerCode', key: 'partnerCode', width: 120, defaultHidden: true },
  { title: '来源订单', field: 'sourceNo', key: 'sourceNo', width: 150, defaultHidden: true },
  { title: '本次预收', field: 'amount', key: 'amount', width: 120, align: 'right', type: 'slot', slotName: 'amountCell', sortable: true, defaultHidden: true },
  { title: '赠送金额', field: 'giftAmount', key: 'giftAmount', width: 110, align: 'right', type: 'slot', slotName: 'giftAmountCell', defaultHidden: true },
  { title: '总金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, sortable: true, defaultHidden: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, sortable: true, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 90, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 140, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80, defaultHidden: false },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 140, defaultHidden: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 140, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right', type: 'slot', slotName: 'printCountCell' },
]

// ═══ 状态映射 ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  confirmed: { text: '已记账', color: 'blue' },
  received: { text: '已收款', color: 'green' },
  offset: { text: '已冲抵', color: 'gold' },
  forfeited: { text: '已没收', color: 'red' },
  refunded: { text: '已退款', color: 'orange' },
}
function getStatusText(status: string): string {
  return STATUS_MAP[status]?.text || status || '未知'
}
function getStatusColor(status: string): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (r.totalAmount ?? r.amount ?? 0), 0)
  const totalPre = tableData.value.reduce((s: number, r: any) => s + (r.amount ?? 0), 0)
  return [
    { key: 'amount', value: totalPre, highlight: true },
    { key: 'totalAmount', value: totalAmount, highlight: true },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.preReceiptNo) params.preReceiptNo = searchParams.preReceiptNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.sourceNo) params.sourceNo = searchParams.sourceNo
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.bookkeeperName) params.bookkeeperName = searchParams.bookkeeperName
    if (searchParams.auditorName) params.auditorName = searchParams.auditorName
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate

    const res: any = await preReceiptApi.getPage(params)
    if (res) {
      const body = (res as any)?.data ?? res
      tableData.value = body?.records || []
      pagination.total = Number(body?.total) || 0
    }
  } catch (error: any) {
    console.warn('[预收款单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key
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
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
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

// ═══ 操作 ═══
function currentOperator() {
  return { id: userStore?.userId || 0, name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统' }
}

function handleAdd() {
  router.push('/finance/advance-receipt/form')
}
function handleView(record: any) {
  router.push(`/finance/advance-receipt/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/finance/advance-receipt/form?id=${record.id}`)
}
function handleConfirm(record: any) {
  Modal.confirm({
    title: '记账确认',
    content: `确定对预收款单 ${record.preReceiptNo} 执行记账吗？记账后结算单位预收余额增加并生成凭证。`,
    okText: '确认记账',
    cancelText: '取消',
    onOk: async () => {
      try {
        const u = currentOperator()
        await preReceiptApi.confirm(record.id, u.id, u.name)
        message.success('记账完成')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '记账失败')
      }
    },
  })
}
function handleForfeit(record: any) {
  Modal.confirm({
    title: '没收预收款',
    content: `确定没收预收款单 ${record.preReceiptNo}？`,
    okText: '确认没收',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await preReceiptApi.forfeit(record.id, '没收')
        message.success('已没收')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '没收失败')
      }
    },
  })
}
function handleRefund(record: any) {
  Modal.confirm({
    title: '退还预收款',
    content: `确定退还预收款单 ${record.preReceiptNo}？`,
    okText: '确认退款',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await preReceiptApi.refund(record.id, '退还')
        message.success('已退款')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '退款失败')
      }
    },
  })
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的预收款单')
    return
  }
  router.push(`/finance/advance-receipt/form?id=${selectedRowKeys.value[0]}`)
}
async function handleExport() {
  try {
    const params: any = { pageNum: 1, pageSize: 9999 }
    if (searchParams.preReceiptNo) params.preReceiptNo = searchParams.preReceiptNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    const res: any = await preReceiptApi.getPage(params)
    const body = (res as any)?.data ?? res
    const data = body?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据日期', '结算单位', '本次预收', '赠送金额', '总金额', '经手人', '状态']
    const rows = data.map((r: any) => [
      r.preReceiptNo, r.receiptDate, r.customerName, r.amount, r.giftAmount,
      r.totalAmount ?? r.amount, r.handlerName, getStatusText(r.status),
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预收款单_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[预收款单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: number): string {
  if (amount === undefined || amount === null) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('lastWeek')
  fetchData()
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
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
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
