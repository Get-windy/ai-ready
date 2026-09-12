<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="科目分类"
        :category-editable="false"
        :category-tree-data="subjectTreeData"
        :category-loading="subjectTreeLoading"
        :category-error="subjectTreeError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentCategoryPath"
        :show-table-footer="true"
        @category-retry="fetchSubjectTree"
        @category-select="onCategorySelect"
        @category-expand="onCategoryExpand"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <span class="list-title">明细账</span>
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
          </div>
          <a-space :size="2" class="quick-dates">
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

        <!-- ═══ 工具栏右侧：配置齿轮 + 刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="配置">
              <a-button size="small" :disabled="!fnEnabled('config')" @click="openPageConfig">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="fnEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="fnEnabled('printF8')" size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="fnEnabled('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：12 查询条件（可配置显隐/排序） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template v-for="qf in visibleQueryFields" :key="qf.key">
                <div v-if="qf.type === 'daterange'" class="search-field-item">
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleDateChange"
                  />
                </div>
                <div v-else-if="qf.type === 'subject'" class="search-field-item">
                  <a-tree-select
                    v-model:value="searchParams.subjectId"
                    :tree-data="subjectSelectTree"
                    :field-names="{ label: 'title', value: 'value', children: 'children' }"
                    show-search
                    allow-clear
                    tree-default-expand-all
                    :filter-option="filterSubjectOption"
                    size="small"
                    style="width: 100%"
                    placeholder="科目"
                    @change="onSubjectSelectChange"
                  />
                </div>
                <div v-else-if="qf.type === 'select'" class="search-field-item search-select-wrap">
                  <span class="search-select-label">{{ qf.label }}</span>
                  <a-select
                    v-model:value="searchParams[qf.key]"
                    size="small"
                    allow-clear
                    placeholder="全部"
                    @change="handleSearch"
                  >
                    <a-select-option :value="undefined">全部</a-select-option>
                    <a-select-option v-for="opt in qf.options" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </a-select-option>
                  </a-select>
                </div>
                <div v-else-if="qf.type === 'checkbox'" class="search-field-item search-checkbox-item">
                  <a-checkbox v-model:checked="searchParams.showRed" @change="handleSearch">
                    {{ qf.label }}
                  </a-checkbox>
                </div>
                <div v-else class="search-field-item">
                  <a-input
                    v-model:value="searchParams[qf.key]"
                    :placeholder="qf.label"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
              </template>
              <div class="search-field-item search-action-item">
                <a-space :size="4">
                  <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  <a-button size="small" @click="handleReset">重置</a-button>
                </a-space>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头 rowNo 列内置列配置齿轮） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              :summary-data="summaryColumns"
              storage-key="finance-detail-ledger-columns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #summaryCell="{ record }">
                <span v-if="!record.__ghost" :class="{ 'opening-row-text': record.rowType === 'OPENING' }">{{ record.summary || '' }}</span>
              </template>
              <template #debitAmountCell="{ record }">
                <span v-if="!record.__ghost" class="num-value">{{ formatAmount(record.debitAmount) }}</span>
              </template>
              <template #creditAmountCell="{ record }">
                <span v-if="!record.__ghost" class="num-value">{{ formatAmount(record.creditAmount) }}</span>
              </template>
              <template #balanceCell="{ record }">
                <span v-if="!record.__ghost" class="num-value">{{ formatBalance(record) }}</span>
              </template>
              <template #reconcileFlagCell="{ record }">
                <template v-if="!record.__ghost">
                  <span v-if="record.rowType === 'OPENING'">—</span>
                  <span v-else :class="['recon-flag', Number(record.reconcileFlag) === 1 ? 'recon-yes' : 'recon-no']">
                    {{ Number(record.reconcileFlag) === 1 ? '是' : '否' }}
                  </span>
                </template>
              </template>
              <template #voucherNoCell="{ record }">
                <template v-if="!record.__ghost">
                  <a-button
                    v-if="record.rowType !== 'OPENING' && record.voucherId"
                    type="link"
                    size="small"
                    @click="goVoucher(record)"
                  >{{ record.voucherNo || '-' }}</a-button>
                  <span v-else>{{ record.voucherNo || '-' }}</span>
                </template>
              </template>
              <template #voucherTypeCell="{ record }">
                <span v-if="!record.__ghost && record.rowType !== 'OPENING'">{{ formatVoucherType(record.voucherType) }}</span>
                <span v-else-if="!record.__ghost">—</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（仅「查询条件」Tab，12 字段全量） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      fields-only
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  SettingOutlined, ReloadOutlined, PrinterOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { ledgerApi } from '@/api/finance'

defineOptions({ name: 'FinanceDetailLedger' })

const router = useRouter()
const route = useRoute()

// ═══ 工具栏：查询方案 / 时间快捷段 ═══
const queryScheme = ref('')
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
const quickDate = ref('month')

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const showPageConfig = ref(false)
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// ═══ 查询参数（12 字段） ═══
const searchParams = reactive<Record<string, any>>({
  dateType: 'voucher',
  subjectId: undefined,
  settleUnit: '',
  settleDept: '',
  settleStaff: '',
  handlerName: '',
  deptName: '',
  reconcileFlag: undefined,
  summary: '',
  remark: '',
  showRed: false,
})
let dateStart = ''
let dateEnd = ''
let subjectType: number | undefined

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 合计（借方/贷方/期末余额，取查询区间全量口径） ═══
const summary = reactive({
  totalDebit: 0,
  totalCredit: 0,
  closingBalance: 0,
  closingDirection: '借',
  hasData: false,
})

const summaryColumns = computed(() => {
  if (!summary.hasData) return []
  return [
    { key: 'debitAmount', value: formatAmount(summary.totalDebit), highlight: true },
    { key: 'creditAmount', value: formatAmount(summary.totalCredit), highlight: true },
    { key: 'balance', value: `${summary.closingDirection} ${formatAmount(Math.abs(summary.closingBalance))}`, highlight: true },
  ] as any[]
})

// ═══ 数据表列（对标 15 列 + 行号列内置列配置齿轮） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '对账标记', field: 'reconcileFlag', key: 'reconcileFlag', width: 90, align: 'center', type: 'slot', slotName: 'reconcileFlagCell' },
  { title: '记账日期', field: 'postDate', key: 'postDate', width: 110 },
  { title: '单据日期', field: 'voucherDate', key: 'voucherDate', width: 110 },
  { title: '单据编号', field: 'voucherNo', key: 'voucherNo', width: 170, type: 'slot', slotName: 'voucherNoCell' },
  { title: '单据类型', field: 'voucherType', key: 'voucherType', width: 100, type: 'slot', slotName: 'voucherTypeCell' },
  { title: '摘要', field: 'summary', key: 'summary', width: 220, type: 'slot', slotName: 'summaryCell' },
  { title: '核算单位', field: 'auxUnit', key: 'auxUnit', width: 140 },
  { title: '核算部门', field: 'auxDept', key: 'auxDept', width: 120 },
  { title: '核算职员', field: 'auxStaff', key: 'auxStaff', width: 110 },
  { title: '借方发生额', field: 'debitAmount', key: 'debitAmount', width: 130, align: 'right', type: 'slot', slotName: 'debitAmountCell' },
  { title: '贷方发生额', field: 'creditAmount', key: 'creditAmount', width: 130, align: 'right', type: 'slot', slotName: 'creditAmountCell' },
  { title: '期末余额', field: 'balance', key: 'balance', width: 150, align: 'right', type: 'slot', slotName: 'balanceCell' },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 100 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 110 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 160 },
]

// ═══ 页面配置（仅「查询条件」Tab，12 字段全部默认显示） ═══
const PAGE_CONFIG_STORAGE_KEY = 'finance-detail-ledger-page-config'
interface QueryFieldSetting {
  key: string
  label: string
  visible: boolean
  type: 'daterange' | 'subject' | 'select' | 'input' | 'checkbox'
  options?: Array<{ label: string; value: any }>
}

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '日期', visible: true, type: 'daterange' },
  {
    key: 'dateType', label: '日期类型', visible: true, type: 'select',
    options: [{ label: '单据日期', value: 'voucher' }, { label: '记账日期', value: 'post' }],
  },
  { key: 'subjectId', label: '科目', visible: true, type: 'subject' },
  { key: 'settleUnit', label: '核算单位', visible: true, type: 'input' },
  { key: 'settleDept', label: '核算部门', visible: true, type: 'input' },
  { key: 'settleStaff', label: '核算职员', visible: true, type: 'input' },
  { key: 'handlerName', label: '经手人', visible: true, type: 'input' },
  { key: 'deptName', label: '部门', visible: true, type: 'input' },
  {
    key: 'reconcileFlag', label: '对账标记', visible: true, type: 'select',
    options: [{ label: '是', value: 1 }, { label: '否', value: 0 }],
  },
  { key: 'summary', label: '摘要', visible: true, type: 'input' },
  { key: 'remark', label: '单据备注', visible: true, type: 'input' },
  { key: 'showRed', label: '显示红冲', visible: true, type: 'checkbox' },
]

const DEFAULT_FUNCTION_BUTTONS = [
  { key: 'config', label: '配置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFieldConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryFieldConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons.find((f: any) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch {
    // 本地配置损坏时回落到默认
  }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || queryFieldConfig.value,
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

function openPageConfig() {
  showPageConfig.value = true
}

function fnEnabled(key: string): boolean {
  const btn = functionButtonConfig.value.find(b => b.key === key)
  return btn ? btn.enabled : true
}

/** 按页面配置的显隐与排序渲染查询条件，「显示红冲」始终排在末尾由复选框渲染 */
const visibleQueryFields = computed(() =>
  queryFieldConfig.value.filter(f => f.visible),
)

// ═══ 科目分类树（全部 / 资产类 / 负债类 / 权益类 / 成本类 / 损益类） ═══
const subjectTreeLoading = ref(false)
const subjectTreeError = ref(false)
const subjectTree = ref<any[]>([])
const selectedCategoryId = ref<string>('0')
const expandedKeys = ref<(string | number)[]>([])
const subjectTreeData = computed(() => subjectTree.value)

/** 查询区「科目」下拉：仅保留具体科目（剔除「全部 / XX类」分组节点，保留下级层级） */
const subjectSelectTree = computed(() => {
  const build = (nodes: any[]): any[] => {
    const result: any[] = []
    for (const node of nodes || []) {
      if (node.subjectId != null) {
        result.push({ value: node.subjectId, title: node.categoryName, children: build(node.children) })
      } else {
        result.push(...build(node.children))
      }
    }
    return result
  }
  return build(subjectTree.value)
})

const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === '0' || !subjectTree.value.length) return '全部科目'
  const path: string[] = []
  const find = (nodes: any[], target: string): boolean => {
    for (const node of nodes) {
      path.push(node.categoryName)
      if (String(node.id) === target) return true
      if (node.children?.length && find(node.children, target)) return true
      path.pop()
    }
    return false
  }
  find(subjectTree.value, selectedCategoryId.value)
  return path.length ? path.join(' / ') : '全部科目'
})

async function fetchSubjectTree() {
  subjectTreeLoading.value = true
  subjectTreeError.value = false
  try {
    const res: any = await ledgerApi.getSubjectTree()
    const data: any[] = Array.isArray(res) ? res : (res?.data || [])
    subjectTree.value = data
    expandedKeys.value = data.map((n: any) => n.id)
  } catch (error) {
    console.warn('[明细账] 加载科目分类树失败', error)
    subjectTreeError.value = true
    subjectTree.value = []
  } finally {
    subjectTreeLoading.value = false
  }
}

function onCategoryExpand(keys: (string | number)[]) {
  expandedKeys.value = keys
}

/** 科目树选中：'0'=全部；'type_x'=科目类型；其余为具体科目ID（含下级） */
function onCategorySelect(keys: (string | number)[]) {
  const key = keys[0] != null ? String(keys[0]) : '0'
  selectedCategoryId.value = key
  if (key === '0') {
    searchParams.subjectId = undefined
    subjectType = undefined
  } else if (key.startsWith('type_')) {
    searchParams.subjectId = undefined
    subjectType = Number(key.slice(5))
  } else {
    searchParams.subjectId = Number(key)
    subjectType = undefined
  }
  handleSearch()
}

/** 查询区「科目」选择：同步左侧分类树高亮 */
function onSubjectSelectChange(value: any) {
  subjectType = undefined
  selectedCategoryId.value = value != null ? String(value) : '0'
  handleSearch()
}

function filterSubjectOption(input: string, option: any) {
  const text = option?.title || option?.label || ''
  return String(text).toLowerCase().includes(input.toLowerCase())
}

// ═══ 日期快捷段 ═══
function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs
  let end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.startOf('month'); end = now
  }
  dateRange.value = [start, end]
  dateStart = start.format('YYYY-MM-DD')
  dateEnd = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates[0] && dates[1]) {
    dateStart = dates[0].format('YYYY-MM-DD')
    dateEnd = dates[1].format('YYYY-MM-DD')
  } else {
    dateStart = ''
    dateEnd = ''
  }
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      showRed: !!searchParams.showRed,
    }
    if (dateStart) params.dateStart = dateStart
    if (dateEnd) params.dateEnd = dateEnd
    if (searchParams.dateType) params.dateType = searchParams.dateType
    if (searchParams.subjectId != null) params.subjectId = searchParams.subjectId
    else if (subjectType != null) params.subjectType = subjectType
    if (searchParams.settleUnit) params.settleUnit = searchParams.settleUnit
    if (searchParams.settleDept) params.settleDept = searchParams.settleDept
    if (searchParams.settleStaff) params.settleStaff = searchParams.settleStaff
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.reconcileFlag !== undefined && searchParams.reconcileFlag !== null && searchParams.reconcileFlag !== '') {
      params.reconcileFlag = searchParams.reconcileFlag
    }
    if (searchParams.summary) params.summary = searchParams.summary
    if (searchParams.remark) params.remark = searchParams.remark

    const res: any = await ledgerApi.getDetailPage(params)
    const body = res?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
    summary.totalDebit = Number(body?.totalDebit) || 0
    summary.totalCredit = Number(body?.totalCredit) || 0
    summary.closingBalance = Number(body?.closingBalance) || 0
    summary.closingDirection = body?.closingDirection || '借'
    summary.hasData = tableData.value.length > 0
  } catch (error: any) {
    console.warn('[明细账] 获取数据失败', error)
    message.error(error?.response?.data?.message || '获取明细账失败')
    tableData.value = []
    pagination.total = 0
    summary.hasData = false
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchParams.settleUnit = ''
  searchParams.settleDept = ''
  searchParams.settleStaff = ''
  searchParams.handlerName = ''
  searchParams.deptName = ''
  searchParams.reconcileFlag = undefined
  searchParams.summary = ''
  searchParams.remark = ''
  searchParams.showRed = false
  searchParams.dateType = 'voucher'
  searchParams.subjectId = undefined
  subjectType = undefined
  selectedCategoryId.value = '0'
  setQuickDate('month')
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleRefresh() {
  fetchData()
}

// ═══ 行操作：凭证穿透 ═══
function goVoucher(record: any) {
  if (!record?.voucherId) return
  router.push(`/finance/voucher/form?id=${record.voucherId}`)
}

// ═══ 打印(F8) ═══
function handlePrintF8() {
  if (tableData.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}
function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrintF8()
  }
}

// ═══ 导出（CSV，导出全部命中行） ═══
async function handleExport() {
  try {
    const params: Record<string, any> = {
      pageNum: 1,
      pageSize: 9999,
      showRed: !!searchParams.showRed,
    }
    if (dateStart) params.dateStart = dateStart
    if (dateEnd) params.dateEnd = dateEnd
    if (searchParams.dateType) params.dateType = searchParams.dateType
    if (searchParams.subjectId != null) params.subjectId = searchParams.subjectId
    else if (subjectType != null) params.subjectType = subjectType
    if (searchParams.settleUnit) params.settleUnit = searchParams.settleUnit
    if (searchParams.settleDept) params.settleDept = searchParams.settleDept
    if (searchParams.settleStaff) params.settleStaff = searchParams.settleStaff
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.reconcileFlag !== undefined && searchParams.reconcileFlag !== null && searchParams.reconcileFlag !== '') {
      params.reconcileFlag = searchParams.reconcileFlag
    }
    if (searchParams.summary) params.summary = searchParams.summary
    if (searchParams.remark) params.remark = searchParams.remark

    const res: any = await ledgerApi.getDetailPage(params)
    const body = res?.data ?? res
    const rows: any[] = body?.records || []
    if (rows.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const cols = columns.filter(c => c.key !== 'rowNo' && c.title)
    const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
    const header = cols.map(c => escape(c.title)).join(',')
    // 期初余额行（rowType=OPENING）非凭证分录，单据类型/对账标记与页面一致留「—」
    const lines = rows.map(r => cols.map(c => {
      if (c.key === 'balance') return escape(formatBalance(r))
      if (c.key === 'debitAmount' || c.key === 'creditAmount') return escape(formatAmount(r[c.field]))
      if (c.key === 'reconcileFlag') return escape(formatReconcileFlag(r))
      if (c.key === 'voucherType') return escape(r.rowType === 'OPENING' ? '—' : formatVoucherType(r.voucherType))
      return escape(r[c.field])
    }).join(','))
    const csv = '\uFEFF' + [header, ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `明细账_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ═══ 格式化 ═══
function formatAmount(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatBalance(record: any): string {
  if (record?.balance === null || record?.balance === undefined || record?.balance === '') return '-'
  const direction = record.balanceDirection || (Number(record.balance) < 0 ? '贷' : '借')
  return `${direction} ${formatAmount(Math.abs(Number(record.balance)))}`
}

function formatVoucherType(type: string | null | undefined): string {
  if (type === 'system') return '系统凭证'
  return '手工凭证'
}

function formatReconcileFlag(record: any): string {
  if (!record || record.rowType === 'OPENING') return '—'
  return Number(record.reconcileFlag) === 1 ? '是' : '否'
}

function handleError(error: Error) {
  console.error('[明细账] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

/**
 * 总账 / 辅助核算余额表「明细对账」下钻：按路由参数（科目 + 会计月区间 + 核算项）定位明细
 * @returns 是否命中下钻参数
 */
function applyRouteQuery(): boolean {
  const query = route.query
  if (!query || (!query.subjectId && !query.dateStart)) {
    return false
  }
  if (query.subjectId) {
    searchParams.subjectId = Number(query.subjectId)
    // 同步左侧科目分类树高亮，保证下钻后「树选中科目」与「查询条件」一致
    selectedCategoryId.value = String(query.subjectId)
    subjectType = undefined
  }
  // 核算项过滤：辅助核算余额表按核算项下钻时带入（往来单位 / 职员 / 部门）
  if (query.settleUnit) {
    searchParams.settleUnit = String(query.settleUnit)
  }
  if (query.settleStaff) {
    searchParams.settleStaff = String(query.settleStaff)
  }
  if (query.settleDept) {
    searchParams.settleDept = String(query.settleDept)
  }
  if (query.dateStart) {
    dateStart = String(query.dateStart)
  }
  if (query.dateEnd) {
    dateEnd = String(query.dateEnd)
  }
  if (dateStart && dateEnd) {
    dateRange.value = [dayjs(dateStart), dayjs(dateEnd)]
  }
  quickDate.value = ''
  return true
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  fetchSubjectTree()
  // 命中总账下钻参数时按传入科目/区间查询，否则按默认「本月」
  if (applyRouteQuery()) {
    fetchData()
  } else {
    setQuickDate('month')
  }
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
.query-scheme-wrap { display: inline-flex; align-items: center; gap: 8px; margin-right: 8px; }
.list-title { font-size: 15px; font-weight: 600; color: #303133; }
.quick-dates { flex-wrap: wrap; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.search-field-item { display: flex; align-items: center; min-width: 0; }
.search-field-item :deep(.ant-input),
.search-field-item :deep(.ant-select),
.search-field-item :deep(.ant-picker),
.search-field-item :deep(.ant-tree-select) { font-size: 13px; }
.search-field-item :deep(.ant-input) { width: 140px; }
.search-field-item :deep(.ant-picker) { width: 220px; }
.search-field-item :deep(.ant-select) { width: 120px; }
.search-field-item :deep(.ant-tree-select) { width: 170px; }
.search-select-wrap { display: flex; align-items: center; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0, 0, 0, 0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 90px; }
.search-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important;
  box-shadow: none !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  display: flex;
  align-items: center;
}
.search-checkbox-item { padding: 0 4px; }
.search-action-item { flex-shrink: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.num-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
.opening-row-text { font-weight: 600; color: #1f2d3d; }
.recon-flag { font-size: 12px; }
.recon-yes { color: #52c41a; }
.recon-no { color: #bfbfbf; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
