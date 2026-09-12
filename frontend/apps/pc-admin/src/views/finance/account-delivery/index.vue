<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="false"
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

        <!-- ═══ 工具栏右侧：功能按钮（随视图变化） ═══ -->
        <!-- 列配置走数据表表头齿轮（BillDetailTable 内置），工具栏不再放重复入口 -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="activeTab === 'staff' && fnEnabled('deliver')"
              type="primary"
              size="small"
              :disabled="selectedRows.length === 0"
              @click="handleDeliver"
            >
              <AccountBookOutlined /> 去交账
            </a-button>
            <a-button
              v-if="activeTab === 'doc' && fnEnabled('deliver')"
              type="primary"
              size="small"
              :disabled="selectedRows.length === 0"
              @click="handleDeliver"
            >
              <AccountBookOutlined /> 配送退货
            </a-button>
            <a-button v-if="fnEnabled('refresh')" size="small" @click="fetchData">
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

        <!-- ═══ 查询区（按页面配置动态渲染） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template v-for="field in visibleSearchFields" :key="field.key">
                <!-- 日期范围 -->
                <div v-if="field.type === 'dateRange'" class="search-field-item">
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 220px"
                    @change="handleDateChange"
                  />
                </div>
                <!-- 下拉选择 -->
                <div v-else-if="field.type === 'select'" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">{{ field.label }}</span>
                    <a-select
                      v-model:value="searchValues[field.key]"
                      placeholder="全部"
                      allow-clear
                      size="small"
                    >
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <!-- 勾选项 -->
                <div v-else-if="field.type === 'checkbox'" class="search-field-item">
                  <a-checkbox v-model:checked="searchValues[field.key]">
                    {{ field.label }}
                  </a-checkbox>
                </div>
                <!-- 文本输入 -->
                <div v-else class="search-field-item">
                  <a-input
                    v-model:value="searchValues[field.key]"
                    :placeholder="field.placeholder || field.label"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
              </template>
              <div class="search-field-item">
                <a-button type="primary" size="small" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 + 五档统计卡片 ═══ -->
        <template #table>
          <div class="table-area">
            <ARStatCards :items="statCards" :loading="loading" class="stat-cards" />
            <BillTableList
              :columns="currentColumns"
              :data-source="currentRows"
              :storage-key="activeTab === 'staff' ? 'account-delivery-table-columns-staff' : 'account-delivery-table-columns-doc'"
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
              @selection-change="handleSelectionChange"
            >
              <!-- 账款确认 -->
              <template #confirmFlagCell="{ record }">
                <span :class="{ 'reconcile-yes': record.confirmFlag }">{{ record.confirmFlag ? '√' : '否' }}</span>
              </template>
              <!-- 交账状态 -->
              <template #deliverStatusCell="{ record }">
                <a-tag :color="record.deliverStatus === '已交账' ? 'green' : record.deliverStatus === '部分交账' ? 'orange' : 'red'" size="small">
                  {{ record.deliverStatus || '未交账' }}
                </a-tag>
              </template>
              <!-- 单据编号（扫码定位） -->
              <template #documentNoCell="{ record }">
                <a-tooltip title="扫码定位">
                  <span class="cell-link">{{ record.documentNo }}</span>
                </a-tooltip>
              </template>
              <!-- 收款单详情 -->
              <template #receiptDetailCell="{ record }">
                <span v-if="record.receiptDetail">{{ record.receiptDetail }}</span>
                <span v-else class="text-muted">-</span>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFieldDefs"
      :function-buttons-config="activeFunctionButtonDefs"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SearchOutlined, SettingOutlined,
  PrinterOutlined, ExportOutlined, AccountBookOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { accountDeliveryApi } from '@/api/finance'

defineOptions({ name: 'FinanceAccountDelivery' })

// ═══ 双视图 Tab ═══
const tabs = [
  { key: 'staff', label: '按职员' },
  { key: 'doc', label: '按单据' },
]
const activeTab = ref<'staff' | 'doc'>('staff')

// ═══ 快捷日期（比其它页多 近两日/近一月/上周） ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'twoDays', label: '近两日' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'lastMonth', label: '近一月' },
  { key: 'week', label: '本周' },
  { key: 'prevWeek', label: '上周' },
  { key: 'month', label: '本月' },
  { key: 'prevMonth', label: '上月' },
]
const quickDate = ref('today')
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const allRows = ref<any[]>([])
const selectedRows = ref<any[]>([])
const summary = ref<Record<string, any>>({})
// 列配置走数据表表头齿轮（storage-key=account-delivery-table-columns-staff / -doc）
const showPageConfig = ref(false)

// ═══ 日期范围（默认今日） ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs(), dayjs()])

// ═══ 搜索参数 ═══
const searchValues = reactive<Record<string, any>>({
  staffName: '',
  customerName: '',
  settlementUnit: '',
  receiptAccount: '',
  documentNo: '',
  deliveryTaskNo: '',
  remark: '',
  confirmFlag: '',
  deliveryType: '',
  deliverStatus: '',
  businessType: '',
  paymentMethod: '',
  documentType: '',
  settlementMethod: '',
  receiptStatus: '',
  receiptNo: '',
  onlinePayNo: '',
  handlerName: '',
  confirmStaff: '',
  showAllPending: true,
})
const dateStart = ref(dayjs().format('YYYY-MM-DD'))
const dateEnd = ref(dayjs().format('YYYY-MM-DD'))

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 表格数据（staff视图内存分页 / doc视图后端分页） ═══
const currentRows = computed(() => {
  if (activeTab.value === 'doc') return allRows.value
  // staff 视图内存分页
  const start = (pagination.current - 1) * pagination.pageSize
  return allRows.value.slice(start, start + pagination.pageSize)
})

// ═══ 五档统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const s = summary.value || {}
  return [
    { label: '待交账单据', value: Number(s.docCount) || 0, suffix: '张' },
    { label: '待交账金额', value: Number(s.totalAmount) || 0, precision: 2, prefix: '¥' },
    { label: '待交账收款金额', value: Number(s.receiveAmount) || 0, precision: 2, prefix: '¥' },
    { label: '待交账优惠', value: Number(s.favorableAmount) || 0, precision: 2, prefix: '¥' },
    { label: '待交账欠款金额', value: Number(s.arrearsAmount) || 0, precision: 2, prefix: '¥' },
  ]
})

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
  sortable?: boolean
}

function moneyFormatter(v: any): string {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 按职员视图列（6 + 派生分组：使用预收/收款合计）
const STAFF_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '交账职员', key: 'deliverStaff', field: 'deliverStaff', width: 140 },
  { title: '部门', key: 'departmentName', field: 'departmentName', width: 120 },
  { title: '待交账单据', key: 'docCount', field: 'docCount', width: 110, align: 'right' },
  { title: '待交账金额', key: 'totalAmount', field: 'totalAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '使用预收', key: 'usedAdvance', field: 'usedAdvance', width: 120, align: 'right', formatter: moneyFormatter },
  { title: '收款合计', key: 'receiveTotal', field: 'receiveTotal', width: 120, align: 'right', formatter: moneyFormatter },
  { title: '待交账优惠', key: 'favorableAmount', field: 'favorableAmount', width: 120, align: 'right', formatter: moneyFormatter },
  { title: '待交账欠款金额', key: 'arrearsAmount', field: 'arrearsAmount', width: 130, align: 'right', formatter: moneyFormatter },
]

// 按单据视图列（19 可配 + 派生冻结列）
const DOC_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '账款确认', key: 'confirmFlag', field: 'confirmFlag', width: 90, align: 'center', type: 'slot', slotName: 'confirmFlagCell' },
  { title: '业务日期', key: 'bizDate', field: 'bizDate', width: 110, sortable: true },
  { title: '单据编号', key: 'documentNo', field: 'documentNo', width: 170, type: 'slot', slotName: 'documentNoCell' },
  { title: '结款方式', key: 'settlementMethod', field: 'settlementMethod', width: 110 },
  { title: '客户', key: 'customerName', field: 'customerName', width: 170 },
  { title: '交账职员', key: 'deliverStaff', field: 'deliverStaff', width: 120 },
  { title: '账款类型', key: 'deliveryType', field: 'deliveryType', width: 110 },
  { title: '交账状态', key: 'deliverStatus', field: 'deliverStatus', width: 110, align: 'center', type: 'slot', slotName: 'deliverStatusCell' },
  { title: '业务类型', key: 'businessType', field: 'businessType', width: 110 },
  { title: '应交金额', key: 'dueAmount', field: 'dueAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '收款优惠', key: 'favorableAmount', field: 'favorableAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '欠款金额', key: 'arrearsAmount', field: 'arrearsAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '收款单详情', key: 'receiptDetail', field: 'receiptDetail', width: 140, type: 'slot', slotName: 'receiptDetailCell' },
  { title: '单据类型', key: 'documentType', field: 'documentType', width: 110, defaultHidden: true },
  { title: '配送任务编号', key: 'deliveryTaskNo', field: 'deliveryTaskNo', width: 160, defaultHidden: true },
  { title: '结算单位', key: 'settlementUnit', field: 'settlementUnit', width: 120, defaultHidden: true },
  { title: '业务经手人', key: 'handlerName', field: 'handlerName', width: 120, defaultHidden: true },
  { title: '确认交账人', key: 'confirmStaff', field: 'confirmStaff', width: 120, defaultHidden: true },
  { title: '确认交账时间', key: 'confirmTime', field: 'confirmTime', width: 170, defaultHidden: true },
  // 派生冻结列（收款账户分组 / 原单详情分组）
  { title: '收款账户1', key: 'paymentAccount1', field: 'paymentAccount1', width: 150, defaultHidden: true },
  { title: '收款账户2', key: 'paymentAccount2', field: 'paymentAccount2', width: 150, defaultHidden: true },
  { title: '收款账户3', key: 'paymentAccount3', field: 'paymentAccount3', width: 150, defaultHidden: true },
  { title: '收款账户4', key: 'paymentAccount4', field: 'paymentAccount4', width: 150, defaultHidden: true },
  { title: '使用预收', key: 'usedAdvance', field: 'usedAdvance', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '收款合计', key: 'receiveTotal', field: 'receiveTotal', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '发货数量', key: 'shipQty', field: 'shipQty', width: 100, align: 'right', defaultHidden: true },
  { title: '发货金额', key: 'shipAmount', field: 'shipAmount', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '签收金额', key: 'receiveAmount', field: 'receiveAmount', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '退货数量', key: 'returnQty', field: 'returnQty', width: 100, align: 'right', defaultHidden: true },
  { title: '退货金额', key: 'returnAmount', field: 'returnAmount', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '已收订金', key: 'orderDeposit', field: 'orderDeposit', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '优惠', key: 'discountAmount', field: 'discountAmount', width: 120, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '其他费用', key: 'otherFee', field: 'otherFee', width: 110, align: 'right', formatter: moneyFormatter, defaultHidden: true },
  { title: '单据备注', key: 'remark', field: 'remark', width: 160, defaultHidden: true },
]

// ═══ 列定义：每个视图取原始列（显隐由表头齿轮按 storage-key 管理） ═══
const currentColumns = computed(() =>
  activeTab.value === 'staff' ? STAFF_COLUMNS : DOC_COLUMNS
)

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface SearchFieldDef {
  key: string
  label: string
  type: 'input' | 'select' | 'dateRange' | 'checkbox'
  placeholder?: string
  options?: Array<{ label: string; value: string }>
}

// 按职员视图：2 查询字段
const STAFF_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '业务日期', type: 'dateRange' },
  { key: 'staffName', label: '交账职员', type: 'input', placeholder: '交账职员' },
]

// 按单据视图：18 查询字段 + 勾选
const DOC_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'date', label: '业务日期', type: 'dateRange' },
  { key: 'customerName', label: '客户', type: 'input', placeholder: '客户' },
  { key: 'settlementUnit', label: '结算单位', type: 'input', placeholder: '结算单位' },
  { key: 'receiptAccount', label: '收款账户', type: 'input', placeholder: '收款账户' },
  { key: 'documentNo', label: '单据编号', type: 'input', placeholder: '单据编号' },
  { key: 'deliveryTaskNo', label: '配送任务编号', type: 'input', placeholder: '配送任务编号' },
  { key: 'remark', label: '单据备注', type: 'input', placeholder: '单据备注' },
  {
    key: 'confirmFlag', label: '账款确认', type: 'select',
    options: [{ label: '√', value: 'Y' }, { label: '否', value: 'N' }],
  },
  {
    key: 'deliveryType', label: '账款类型', type: 'select',
    options: [{ label: '全部', value: '' }, { label: '送货代收', value: '送货代收' }],
  },
  {
    key: 'deliverStatus', label: '交账状态', type: 'select',
    options: [{ label: '未交账', value: '未交账' }, { label: '部分交账', value: '部分交账' }, { label: '已交账', value: '已交账' }],
  },
  {
    key: 'businessType', label: '业务类型', type: 'select',
    options: [{ label: '销售出库单', value: 'OUTBOUND' }, { label: '销售退货单', value: 'RETURN' }],
  },
  {
    key: 'paymentMethod', label: '支付方式', type: 'select',
    options: [{ label: '现金', value: '现金' }, { label: '银行转账', value: '银行转账' }, { label: '在线支付', value: '在线支付' }],
  },
  {
    key: 'documentType', label: '单据类型', type: 'select',
    options: [{ label: '销售出库单', value: 'OUTBOUND' }, { label: '销售退货单', value: 'RETURN' }],
  },
  { key: 'settlementMethod', label: '客户结款方式', type: 'input', placeholder: '客户结款方式' },
  { key: 'receiptStatus', label: '收款单状态', type: 'input', placeholder: '收款单状态' },
  { key: 'receiptNo', label: '收款单编号', type: 'input', placeholder: '收款单编号' },
  { key: 'onlinePayNo', label: '在线支付流水号', type: 'input', placeholder: '在线支付流水号' },
  { key: 'handlerName', label: '业务经手人', type: 'input', placeholder: '业务经手人' },
  { key: 'confirmStaff', label: '确认交账人', type: 'input', placeholder: '确认交账人' },
  { key: 'showAllPending', label: '显示全部待交账单据', type: 'checkbox' },
]

function makeQueryFieldDefs(fields: SearchFieldDef[]): QueryFieldSetting[] {
  return fields.map(f => ({ key: f.key, label: f.label, visible: true }))
}

const STAFF_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'deliver', label: '去交账', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const DOC_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'deliver', label: '配送退货', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const currentSearchFields = computed(() =>
  activeTab.value === 'staff' ? STAFF_SEARCH_FIELDS : DOC_SEARCH_FIELDS
)
const activeQueryFieldDefs = computed(() => makeQueryFieldDefs(currentSearchFields.value))
const activeFunctionButtonDefs = computed(() =>
  activeTab.value === 'staff' ? STAFF_FUNCTION_BUTTONS : DOC_FUNCTION_BUTTONS
)

// 查询字段显隐（页面配置持久化到 localStorage）
const queryFieldVis: Record<string, boolean> = reactive({})
const queryFieldsConfig = ref<QueryFieldSetting[]>(activeQueryFieldDefs.value)
const functionButtonConfig = ref<FunctionButtonSetting[]>(activeFunctionButtonDefs.value)

const pageConfigStorageKey = computed(() => `account-delivery-page-config-${activeTab.value}`)

const visibleSearchFields = computed(() =>
  currentSearchFields.value.filter(f => {
    const c = queryFieldsConfig.value.find(qf => qf.key === f.key)
    return c ? c.visible : true
  })
)

function loadPageConfig() {
  loadPageConfigFor('account-delivery-page-config-staff', STAFF_FUNCTION_BUTTONS, STAFF_SEARCH_FIELDS)
  loadPageConfigFor('account-delivery-page-config-doc', DOC_FUNCTION_BUTTONS, DOC_SEARCH_FIELDS)
}
function loadPageConfigFor(storage: string, fbDefs: FunctionButtonSetting[], fieldDefs: SearchFieldDef[]) {
  const targetKey = storage.endsWith('staff') ? 'staff' : 'doc'
  try {
    const raw = localStorage.getItem(storage)
    const fields = makeQueryFieldDefs(fieldDefs)
    const buttons = fbDefs.map(b => ({ ...b }))
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        fields.forEach(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          if (saved) df.visible = saved.visible !== false
        })
      }
      if (parsed.functionButtons) {
        buttons.forEach(b => {
          const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === b.key)
          if (saved) b.enabled = saved.enabled
        })
      }
    }
    if (targetKey === 'staff') queryFieldsConfig.value = fields
    else queryFieldsConfig.value = fields
    functionButtonConfig.value = buttons
  } catch {
    // ignore
  }
}

function handlePageConfigChange(config: any) {
  const queryFields = config.queryFields || []
  const functions = config.functionButtons || []
  // 合并默认
  queryFieldsConfig.value = activeQueryFieldDefs.value.map(df => {
    const saved = queryFields.find((f: QueryFieldSetting) => f.key === df.key)
    return saved ? { ...df, ...saved } : { ...df }
  })
  functionButtonConfig.value = activeFunctionButtonDefs.value.map(b => {
    const saved = functions.find((f: FunctionButtonSetting) => f.key === b.key)
    return saved ? { ...b, ...saved } : { ...b }
  })
  localStorage.setItem(pageConfigStorageKey.value, JSON.stringify({
    queryFields: queryFieldsConfig.value,
    functionButtons: functionButtonConfig.value,
  }))
}

function fnEnabled(key: string): boolean {
  const c = functionButtonConfig.value.find(f => f.key === key)
  return c ? c.enabled : true
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'staff') {
      await fetchStaff()
    } else {
      await fetchDoc()
    }
  } catch (e: any) {
    console.warn('[账款交账] 获取数据失败', e)
    message.error(e?.response?.data?.message || '查询失败，请检查网络后重试')
    allRows.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

async function fetchStaff() {
  const params: any = buildCommonParams()
  if (searchValues.staffName) params.staffName = searchValues.staffName
  const res: any = await accountDeliveryApi.staff(params)
  const data = res?.data || res
  allRows.value = (data?.staffList || []).map((r: any, i: number) => mapStaffRow(r, i))
  pagination.total = allRows.value.length
  summary.value = data?.summary || {}
}

async function fetchDoc() {
  const params: any = {
    current: pagination.current,
    size: pagination.pageSize,
    ...buildCommonParams(),
    customerName: searchValues.customerName || undefined,
    settlementUnit: searchValues.settlementUnit || undefined,
    receiptAccount: searchValues.receiptAccount || undefined,
    documentNo: searchValues.documentNo || undefined,
    deliveryTaskNo: searchValues.deliveryTaskNo || undefined,
    remark: searchValues.remark || undefined,
    confirmFlag: searchValues.confirmFlag || undefined,
    deliveryType: searchValues.deliveryType || undefined,
    deliverStatus: searchValues.deliverStatus || undefined,
    businessType: searchValues.businessType || undefined,
    paymentMethod: searchValues.paymentMethod || undefined,
    documentType: searchValues.documentType || undefined,
    settlementMethod: searchValues.settlementMethod || undefined,
    receiptStatus: searchValues.receiptStatus || undefined,
    receiptNo: searchValues.receiptNo || undefined,
    onlinePayNo: searchValues.onlinePayNo || undefined,
    handlerName: searchValues.handlerName || undefined,
    confirmStaff: searchValues.confirmStaff || undefined,
  }
  const res: any = await accountDeliveryApi.docPage(params)
  const data = res?.data || res
  const records: any[] = data?.records || data?.content || data?.list || []
  allRows.value = records.map((r: any, i: number) => mapDocRow(r, i))
  pagination.total = Number(data?.total) || allRows.value.length
  summary.value = data?.summary || {}
}

function buildCommonParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (dateStart.value) params.dateStart = dateStart.value
  if (dateEnd.value) params.dateEnd = dateEnd.value
  return params
}

function mapStaffRow(r: any, i: number): any {
  return { ...r, rowKey: `staff-${r.deliverStaff}-${i}` }
}

function mapDocRow(r: any, i: number): any {
  return {
    ...r,
    rowKey: `${r.sourceType || 'OUTBOUND'}-${r.id}-${i}`,
    confirmFlag: r.confirmFlag === true || r.confirmFlag === 'Y' || r.confirmFlag === 1,
    bizDate: r.bizDate,
    documentNo: r.documentNo,
    settleMethod: r.settlementMethod,
  }
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key as 'staff' | 'doc'
  pagination.current = 1
  selectedRows.value = []
  loadPageConfigFor(activeTab.value === 'staff'
    ? 'account-delivery-page-config-staff'
    : 'account-delivery-page-config-doc',
    activeTab.value === 'staff' ? STAFF_FUNCTION_BUTTONS : DOC_FUNCTION_BUTTONS,
    activeTab.value === 'staff' ? STAFF_SEARCH_FIELDS : DOC_SEARCH_FIELDS)
  fetchData()
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  if (activeTab.value === 'doc') fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
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

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'twoDays': start = now.subtract(1, 'day'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month'); end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'prevWeek': start = now.subtract(1, 'week').startOf('week'); end = now.subtract(1, 'week').endOf('week'); break
    case 'month': start = now.startOf('month'); end = now; break
    case 'prevMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    default: start = now; end = now
  }
  dateRange.value = [start, end]
  dateStart.value = start.format('YYYY-MM-DD')
  dateEnd.value = end.format('YYYY-MM-DD')
  handleSearch()
}

// ═══ 交账动作 ═══
async function handleDeliver() {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要交账的记录')
    return
  }
  const isStaff = activeTab.value === 'staff'
  const label = isStaff ? '去交账' : '配送退货'
  try {
    if (isStaff) {
      const staffs = [...new Set(selectedRows.value.map(r => r.deliverStaff).filter(Boolean))]
      await accountDeliveryApi.deliver({ deliverStaff: staffs[0], sourceType: 'OUTBOUND' })
    } else {
      const docIds = selectedRows.value.map(r => r.id).filter(Boolean)
      await accountDeliveryApi.deliver({ sourceType: 'OUTBOUND', sourceIds: docIds })
    }
    message.success(`${label}成功，职员待交账已清零`)
    fetchData()
  } catch (e: any) {
    console.warn('[账款交账] 交账失败', e)
    message.error(e?.response?.data?.message || `${label}失败，请重试`)
  }
}

// ═══ 工具栏操作 ═══
function handlePrintF8() {
  if (allRows.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}

function handleExport() {
  if (allRows.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const cols = currentColumns.value.filter((c: any) => c.key !== 'rowNo' && c.title)
  const headers = cols.map((c: any) => c.title)
  const lines = allRows.value.map((r: any) => cols.map((c: any) => {
    const v = r[c.field ?? c.key]
    if (v === null || v === undefined) return ''
    return String(v).replace(/,/g, '')
  }).join(','))
  const csv = '\uFEFF' + [headers.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `账款交账-${activeTab.value === 'staff' ? '按职员' : '按单据'}-${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 工具 ═══
function handleError(error: Error) {
  console.error('[账款交账] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('today')
})
</script>

<style scoped>
.query-scheme-wrap { display: inline-flex; align-items: center; margin-right: 8px; }
.quick-dates { flex-wrap: wrap; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.search-field-item { display: flex; align-items: center; min-width: 0; }
.search-field-item :deep(.ant-input),
.search-field-item :deep(.ant-select),
.search-field-item :deep(.ant-picker) { font-size: 13px; }
.search-field-item :deep(.ant-input) { width: 160px; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 120px; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.stat-cards { flex-shrink: 0; margin-bottom: 8px; }
.reconcile-yes { color: #52c41a; font-weight: 600; }
.cell-link { color: #1668dc; }
.text-muted { color: rgba(0,0,0,0.35); }

:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
