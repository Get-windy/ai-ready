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
        <!-- ═══ 工具栏左侧：标题 + 超支提示 ═══ -->
        <template #toolbar-left>
          <div class="toolbar-title-wrap">
            <span class="list-title">预算执行</span>
            <a-tag
              v-if="statSummary.overBudgetCount > 0"
              color="red"
              class="over-tag"
              @click="showOverBudgetOnly"
            >
              超支 {{ statSummary.overBudgetCount }} 项
            </a-tag>
            <a-tag
              v-else-if="statSummary.warningCount > 0"
              color="orange"
              class="over-tag"
              @click="showWarningOnly"
            >
              预警 {{ statSummary.warningCount }} 项
            </a-tag>
          </div>
        </template>

        <!-- ═══ 工具栏右侧：功能按钮 ═══ -->
        <!-- 列配置走数据表表头齿轮（BillDetailTable 内置），工具栏不再放重复入口 -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="页面配置">
              <a-button
                v-if="fnEnabled('config')"
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="fnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="fnEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="fnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（按页面配置动态渲染） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template
                v-for="field in visibleSearchFields"
                :key="field.key"
              >
                <div
                  v-if="field.type === 'select'"
                  class="search-field-item"
                >
                  <div class="search-select-wrap">
                    <span class="search-select-label">{{ field.label }}</span>
                    <a-select
                      v-model:value="searchValues[field.key]"
                      :placeholder="field.placeholder || '全部'"
                      allow-clear
                      size="small"
                      @change="handleSearch"
                    >
                      <a-select-option
                        v-for="opt in field.options"
                        :key="String(opt.value)"
                        :value="opt.value"
                      >
                        {{ opt.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <div
                  v-else-if="field.type === 'checkbox'"
                  class="search-field-item"
                >
                  <a-checkbox
                    v-model:checked="searchValues[field.key]"
                    @change="handleSearch"
                  >
                    {{ field.label }}
                  </a-checkbox>
                </div>
                <div
                  v-else
                  class="search-field-item"
                >
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
                <a-space :size="4">
                  <a-button
                    type="primary"
                    size="small"
                    @click="handleSearch"
                  >
                    <SearchOutlined /> 查询
                  </a-button>
                  <a-button
                    size="small"
                    @click="handleReset"
                  >
                    重置
                  </a-button>
                </a-space>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 概要卡片 + 执行明细表 ═══ -->
        <template #table>
          <div class="table-area">
            <ARStatCards
              :items="statCards"
              :loading="loading"
              class="stat-cards"
            />
            <BillTableList
              :columns="currentColumns"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              :storage-key="storageKey"
              row-key="rowKey"
              @page-change="handlePageChange"
            >
              <template #statusNameCell="{ record }">
                <a-tag :color="STATUS_COLOR[record.status] || 'default'">
                  {{ record.statusName || record.status }}
                </a-tag>
              </template>
              <template #executionRateCell="{ record }">
                <div v-if="record.budgetNo" class="rate-cell">
                  <a-progress
                    :percent="Math.min(Number(record.executionRate) || 0, 100)"
                    :status="Number(record.executionRate) > 100 ? 'exception' : 'normal'"
                    :stroke-color="rateColor(record.executionRate)"
                    size="small"
                    :show-info="false"
                  />
                  <span :class="['rate-text', rateClass(record.executionRate)]">
                    {{ formatRate(record.executionRate) }}
                  </span>
                </div>
              </template>
              <template #warnMessageCell="{ record }">
                <a-tooltip v-if="record.warnMessage" :title="record.warnMessage">
                  <a-tag :color="record.warnLevel === 'over' ? 'red' : 'orange'">
                    {{ record.warnLevel === 'over' ? '超支' : '预警' }}
                  </a-tag>
                </a-tooltip>
                <span v-else class="text-muted">-</span>
              </template>
              <template #remainingAmountCell="{ record }">
                <span :class="{ 'text-danger': Number(record.remainingAmount) < 0 }">
                  {{ formatMoney(record.remainingAmount) }}
                </span>
              </template>
              <template #actionCell="{ record }">
                <a-button
                  v-if="record.budgetNo"
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  明细
                </a-button>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 执行明细抽屉：预算科目明细 + 执行流水 ═══ -->
    <a-drawer
      v-model:open="detailOpen"
      :title="detailTitle"
      width="1040"
      destroy-on-close
    >
      <a-descriptions
        v-if="currentRecord"
        :column="4"
        bordered
        size="small"
        class="detail-desc"
      >
        <a-descriptions-item label="预算金额">
          {{ formatMoney(currentRecord.totalAmount ?? currentRecord.budgetAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="已执行">
          {{ formatMoney(currentRecord.usedAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="冻结">
          {{ formatMoney(currentRecord.frozenAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="剩余">
          {{ formatMoney(currentRecord.remainingAmount) }}
        </a-descriptions-item>
        <a-descriptions-item label="执行进度">
          {{ formatRate(currentRecord.executionRate) }}
        </a-descriptions-item>
        <a-descriptions-item label="最近执行">
          {{ currentRecord.lastExecDate || '-' }}
        </a-descriptions-item>
        <a-descriptions-item
          v-if="currentRecord.subjectCode"
          label="预算科目"
        >
          {{ currentRecord.subjectName }} ({{ currentRecord.subjectCode }})
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          {{ currentRecord.statusName || currentRecord.status }}
        </a-descriptions-item>
      </a-descriptions>

      <div
        v-if="currentRecord?.warnMessage"
        class="warn-banner"
        :class="currentRecord.warnLevel === 'over' ? 'is-over' : 'is-warn'"
      >
        {{ currentRecord.warnMessage }}
      </div>

      <!-- 预算科目明细（按预算单维度时展示） -->
      <template v-if="activeTab === 'rows'">
        <div class="drawer-section-title">
          预算科目明细
        </div>
        <a-table
          :columns="itemColumns"
          :data-source="budgetItems"
          :loading="itemLoading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="['budgetAmount', 'usedAmount', 'frozenAmount', 'remainingAmount'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'executionRate'">
              <span :class="rateClass(text)">{{ formatRate(text) }}</span>
            </template>
          </template>
        </a-table>
      </template>

      <!-- 执行流水 -->
      <div class="drawer-section-title">
        预算执行流水
        <span class="drawer-section-tip">冻结 / 释放 / 消耗均记录来源单据</span>
      </div>
      <a-table
        :columns="logColumns"
        :data-source="logs"
        :loading="logLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record, text }">
          <template v-if="column.dataIndex === 'executionTypeName'">
            <a-tag :color="EXEC_TYPE_COLOR[record.executionType] || 'default'">
              {{ text }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'amount'">
            {{ formatMoney(text) }}
          </template>
          <template v-else-if="!text">
            -
          </template>
        </template>
      </a-table>
    </a-drawer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="finance-budget-exec"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, SettingOutlined,
  ReloadOutlined, PrinterOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { budgetExecutionApi, budgetItemApi, budgetReportApi } from '@/api/budget'
import { optionsApi } from '@/api/options'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'FinanceBudgetExec' })

const PAGE_CONFIG_STORAGE_KEY = 'finance-budget-exec-page-config'

// ═══ 双维度 Tab：按预算单 / 按预算科目 ═══
const tabs = [
  { key: 'rows', label: '按预算单' },
  { key: 'items', label: '按预算科目' },
]
const activeTab = ref<'rows' | 'items'>('rows')

const STATUS_COLOR: Record<string, string> = {
  approved: 'blue',
  executing: 'green',
  closed: 'default',
  draft: 'default',
  submitted: 'orange',
  rejected: 'red',
}
const EXEC_TYPE_COLOR: Record<string, string> = {
  freeze: 'orange',
  unfreeze: 'blue',
  consume: 'green',
}

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const statSummary = ref<Record<string, any>>({})
// 列配置走数据表表头齿轮（storage-key=budget-exec-table-columns-rows / -items）
const showPageConfig = ref(false)
const departmentOptions = ref<any[]>([])

// ═══ 财政年度（含下一年度：预算通常按次年编制） ═══
const currentYear = new Date().getFullYear()
const YEAR_OPTIONS = [1, 0, -1, -2].map(i => {
  const y = currentYear + i
  return { label: `${y}年`, value: y }
})

// ═══ 查询参数 ═══
const searchValues = reactive<Record<string, any>>({
  fiscalYear: currentYear,
  departmentId: undefined,
  status: undefined,
  subjectCode: '',
  keyword: '',
  overBudgetOnly: false,
  warningOnly: false,
})

interface SearchFieldDef {
  key: string
  label: string
  type: 'input' | 'select' | 'checkbox'
  placeholder?: string
  options?: Array<{ label: string; value: any }>
}

const SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'fiscalYear', label: '财政年度', type: 'select', options: YEAR_OPTIONS },
  { key: 'departmentId', label: '部门', type: 'select', options: [] },
  {
    key: 'status', label: '预算状态', type: 'select',
    options: [
      { label: '已审批', value: 'approved' },
      { label: '执行中', value: 'executing' },
      { label: '已关闭', value: 'closed' },
    ],
    placeholder: '默认为已转入执行',
  },
  { key: 'subjectCode', label: '预算科目', type: 'input', placeholder: '预算科目编码' },
  { key: 'keyword', label: '关键字', type: 'input', placeholder: '预算编号/部门/科目名称' },
  { key: 'overBudgetOnly', label: '仅看超支', type: 'checkbox' },
  { key: 'warningOnly', label: '仅看预警', type: 'checkbox' },
]

const FUNCTION_BUTTONS = [
  { key: 'config', label: '列配置/页面配置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFieldConfig = ref(SEARCH_FIELDS.map(f => ({ key: f.key, label: f.label, visible: true })))
const functionButtonConfig = ref(FUNCTION_BUTTONS.map(b => ({ ...b })))

const visibleSearchFields = computed(() =>
  SEARCH_FIELDS.filter(f => {
    const c = queryFieldConfig.value.find(q => q.key === f.key)
    return c ? c.visible : true
  }).map(f => (f.key === 'departmentId' ? { ...f, options: departmentOptions.value } : f))
)

function fnEnabled(key: string): boolean {
  const c = functionButtonConfig.value.find(f => f.key === key)
  return c ? c.enabled : true
}

// ═══ 概要卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const s = statSummary.value || {}
  return [
    { label: '预算总额', value: Number(s.totalBudgetAmount) || 0, precision: 2, prefix: '¥' },
    { label: '已执行', value: Number(s.totalUsedAmount) || 0, precision: 2, prefix: '¥' },
    { label: '冻结金额', value: Number(s.totalFrozenAmount) || 0, precision: 2, prefix: '¥' },
    { label: '剩余额度', value: Number(s.totalRemainingAmount) || 0, precision: 2, prefix: '¥' },
    { label: '执行进度', value: Number(s.executionRate) || 0, precision: 2, suffix: '%' },
    { label: '预算单数', value: Number(s.approvedCount) || 0, suffix: '份' },
    { label: '超支科目', value: Number(s.overBudgetCount) || 0, suffix: '项' },
    { label: '预警科目', value: Number(s.warningCount) || 0, suffix: '项' },
  ]
})

// ═══ 列定义 ═══
const ROW_COLUMNS: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '预算编号', key: 'budgetNo', field: 'budgetNo', width: 170 },
  { title: '财政年度', key: 'fiscalYear', field: 'fiscalYear', width: 90, align: 'center' },
  { title: '部门', key: 'departmentName', field: 'departmentName', width: 130 },
  { title: '预算单名称', key: 'budgetName', field: 'budgetName', width: 180 },
  { title: '状态', key: 'statusName', field: 'statusName', width: 90, align: 'center', type: 'slot', slotName: 'statusNameCell' },
  { title: '预算金额', key: 'totalAmount', field: 'totalAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '已执行', key: 'usedAmount', field: 'usedAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '冻结金额', key: 'frozenAmount', field: 'frozenAmount', width: 120, align: 'right', formatter: moneyFormatter },
  { title: '剩余额度', key: 'remainingAmount', field: 'remainingAmount', width: 130, align: 'right', type: 'slot', slotName: 'remainingAmountCell' },
  { title: '执行进度', key: 'executionRate', field: 'executionRate', width: 160, align: 'center', type: 'slot', slotName: 'executionRateCell' },
  { title: '预警', key: 'warnMessage', field: 'warnMessage', width: 80, align: 'center', type: 'slot', slotName: 'warnMessageCell' },
  { title: '预算科目数', key: 'itemCount', field: 'itemCount', width: 100, align: 'right' },
  { title: '超支科目数', key: 'overItemCount', field: 'overItemCount', width: 100, align: 'right' },
  { title: '最近执行', key: 'lastExecDate', field: 'lastExecDate', width: 110, align: 'center' },
  { title: '操作', key: 'action', width: 80, fixed: 'right', type: 'slot', slotName: 'actionCell' },
]

const ITEM_COLUMNS: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '预算编号', key: 'budgetNo', field: 'budgetNo', width: 170 },
  { title: '财政年度', key: 'fiscalYear', field: 'fiscalYear', width: 90, align: 'center' },
  { title: '部门', key: 'departmentName', field: 'departmentName', width: 130 },
  { title: '预算科目编码', key: 'subjectCode', field: 'subjectCode', width: 130 },
  { title: '预算科目名称', key: 'subjectName', field: 'subjectName', width: 180 },
  { title: '状态', key: 'statusName', field: 'statusName', width: 90, align: 'center', type: 'slot', slotName: 'statusNameCell' },
  { title: '预算金额', key: 'budgetAmount', field: 'budgetAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '已执行', key: 'usedAmount', field: 'usedAmount', width: 130, align: 'right', formatter: moneyFormatter },
  { title: '冻结金额', key: 'frozenAmount', field: 'frozenAmount', width: 120, align: 'right', formatter: moneyFormatter },
  { title: '剩余额度', key: 'remainingAmount', field: 'remainingAmount', width: 130, align: 'right', type: 'slot', slotName: 'remainingAmountCell' },
  { title: '执行进度', key: 'executionRate', field: 'executionRate', width: 160, align: 'center', type: 'slot', slotName: 'executionRateCell' },
  { title: '超支金额', key: 'overAmount', field: 'overAmount', width: 120, align: 'right', formatter: moneyFormatter },
  { title: '执行笔数', key: 'execCount', field: 'execCount', width: 90, align: 'right' },
  { title: '最近执行', key: 'lastExecDate', field: 'lastExecDate', width: 110, align: 'center' },
  { title: '预警', key: 'warnMessage', field: 'warnMessage', width: 80, align: 'center', type: 'slot', slotName: 'warnMessageCell' },
  { title: '备注', key: 'remark', field: 'remark', width: 160, defaultHidden: true },
  { title: '操作', key: 'action', width: 80, fixed: 'right', type: 'slot', slotName: 'actionCell' },
]

// ═══ 列配置：每个视图独立存储键，列取原始定义（显隐由表头齿轮管理） ═══
const currentColumns = computed(() =>
  activeTab.value === 'rows' ? ROW_COLUMNS : ITEM_COLUMNS
)
const storageKey = computed(() =>
  activeTab.value === 'rows' ? 'budget-exec-table-columns-rows' : 'budget-exec-table-columns-items'
)

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 明细抽屉 ═══
const detailOpen = ref(false)
const currentRecord = ref<any>(null)
const budgetItems = ref<any[]>([])
const itemLoading = ref(false)
const logs = ref<any[]>([])
const logLoading = ref(false)

const detailTitle = computed(() => {
  const r = currentRecord.value
  if (!r) return '预算执行明细'
  return r.subjectCode
    ? `预算执行明细 — ${r.budgetNo || ''} / ${r.subjectName || r.subjectCode}`
    : `预算执行明细 — ${r.budgetNo || ''}`
})

const itemColumns: any[] = [
  { title: '科目代码', dataIndex: 'subjectCode', key: 'subjectCode', width: 100 },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName', ellipsis: true },
  { title: '预算金额', dataIndex: 'budgetAmount', key: 'budgetAmount', width: 110, align: 'right' },
  { title: '已执行', dataIndex: 'usedAmount', key: 'usedAmount', width: 110, align: 'right' },
  { title: '冻结', dataIndex: 'frozenAmount', key: 'frozenAmount', width: 90, align: 'right' },
  { title: '剩余', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 110, align: 'right' },
  { title: '执行进度', dataIndex: 'executionRate', key: 'executionRate', width: 90, align: 'right' },
  { title: '最近执行', dataIndex: 'lastExecDate', key: 'lastExecDate', width: 100, align: 'center' },
]

const logColumns: any[] = [
  { title: '执行日期', dataIndex: 'executionDate', key: 'executionDate', width: 110 },
  { title: '类型', dataIndex: 'executionTypeName', key: 'executionTypeName', width: 90, align: 'center' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '来源单据', dataIndex: 'sourceTypeName', key: 'sourceTypeName', width: 100 },
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 190 },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true },
]

// ═══ 格式化 ═══
function moneyFormatter(v: any): string {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatMoney(v: any): string {
  return moneyFormatter(v)
}
function formatRate(v: any): string {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (isNaN(n)) return '-'
  return `${n.toFixed(2)}%`
}
function rateColor(v: any): string {
  const n = Number(v) || 0
  if (n > 100) return '#ff4d4f'
  if (n >= 90) return '#faad14'
  return '#52c41a'
}
function rateClass(v: any): string {
  const n = Number(v) || 0
  if (n > 100) return 'text-danger'
  if (n >= 90) return 'text-warning'
  return ''
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    fiscalYear: searchValues.fiscalYear,
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchValues.departmentId) params.departmentId = String(searchValues.departmentId)
  if (searchValues.status) params.status = searchValues.status
  if (searchValues.subjectCode) params.subjectCode = searchValues.subjectCode
  if (searchValues.keyword) params.keyword = searchValues.keyword
  if (searchValues.overBudgetOnly) params.overBudgetOnly = true
  if (searchValues.warningOnly) params.warningOnly = true
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const api = activeTab.value === 'rows' ? budgetExecutionApi.rows : budgetExecutionApi.items
    const res: any = await api(buildParams())
    const body = res?.data ?? res
    const records: any[] = body?.records || []
    tableData.value = records.map((r: any, i: number) => ({
      ...r,
      rowKey: `${activeTab.value}-${r.budgetId}-${r.id ?? ''}-${i}`,
    }))
    pagination.total = Number(body?.total) || 0
  } catch (e: any) {
    console.warn('[预算执行] 获取执行明细失败', e)
    message.error(e?.response?.data?.message || '查询失败，请稍后重试')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const res: any = await budgetReportApi.executionSummary(searchValues.fiscalYear)
    statSummary.value = res?.data ?? res ?? {}
  } catch (e) {
    console.warn('[预算执行] 获取执行概览失败', e)
    statSummary.value = {}
  }
}

async function loadDepartments() {
  try {
    const list = await optionsApi.getDepartments()
    departmentOptions.value = (list || []).map((d: any) => ({ label: d.name, value: String(d.id) }))
  } catch {
    departmentOptions.value = []
  }
}

// ═══ 事件 ═══
function handleTabChange(key: string) {
  activeTab.value = key as 'rows' | 'items'
  pagination.current = 1
  fetchData()
}

function handleSearch() {
  pagination.current = 1
  fetchSummary()
  fetchData()
}

function handleReset() {
  searchValues.departmentId = undefined
  searchValues.status = undefined
  searchValues.subjectCode = ''
  searchValues.keyword = ''
  searchValues.overBudgetOnly = false
  searchValues.warningOnly = false
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function showOverBudgetOnly() {
  searchValues.overBudgetOnly = true
  searchValues.warningOnly = false
  handleSearch()
}

function showWarningOnly() {
  searchValues.warningOnly = true
  searchValues.overBudgetOnly = false
  handleSearch()
}

async function openDetail(record: any) {
  currentRecord.value = record
  detailOpen.value = true
  logs.value = []
  budgetItems.value = []

  logLoading.value = true
  budgetExecutionApi.logs(
    activeTab.value === 'items'
      ? { budgetId: record.budgetId, budgetItemId: record.id }
      : { budgetId: record.budgetId }
  ).then((res: any) => {
    logs.value = res?.data ?? res ?? []
  }).catch(e => {
    console.warn('[预算执行] 获取执行流水失败', e)
    logs.value = []
  }).finally(() => {
    logLoading.value = false
  })

  if (activeTab.value === 'rows' && record.budgetId) {
    itemLoading.value = true
    try {
      const res: any = await budgetItemApi.listByBudget(record.budgetId)
      budgetItems.value = res?.data ?? res ?? []
    } catch (e) {
      console.warn('[预算执行] 获取预算科目明细失败', e)
      budgetItems.value = []
    } finally {
      itemLoading.value = false
    }
  }
}

// ═══ 页面配置 ═══
function loadPageConfig() {
  const fields = SEARCH_FIELDS.map(f => ({ key: f.key, label: f.label, visible: true }))
  const buttons = FUNCTION_BUTTONS.map(b => ({ ...b }))
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        fields.forEach(df => {
          const saved = parsed.queryFields.find((f: any) => f.key === df.key)
          if (saved) df.visible = saved.visible !== false
        })
      }
      if (parsed.functionButtons) {
        buttons.forEach(b => {
          const saved = parsed.functionButtons.find((f: any) => f.key === b.key)
          if (saved) b.enabled = saved.enabled !== false
        })
      }
    }
  } catch {
    // ignore
  }
  queryFieldConfig.value = fields
  functionButtonConfig.value = buttons
}

function handlePageConfigChange(config: any) {
  const queryFields = config.queryFields || []
  const functions = config.functionButtons || []
  queryFieldConfig.value = SEARCH_FIELDS.map(f => ({ key: f.key, label: f.label, visible: true })).map(df => {
    const saved = queryFields.find((f: any) => f.key === df.key)
    return saved ? { ...df, ...saved } : df
  })
  functionButtonConfig.value = FUNCTION_BUTTONS.map(b => {
    const saved = functions.find((f: any) => f.key === b.key)
    return saved ? { ...b, ...saved } : { ...b }
  })
}

// ═══ 打印 / 导出 ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'finance-budget-exec',
  title: '页面配置',
  columns: () => currentColumns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

function handleExport() {
  if (tableData.value.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const cols = currentColumns.value.filter((c: any) => c.key !== 'rowNo' && c.key !== 'action' && c.title)
  const headers = cols.map((c: any) => c.title)
  const lines = tableData.value.map(r =>
    cols.map((c: any) => {
      const v = r[c.field ?? c.key]
      if (v === null || v === undefined) return ''
      return String(v).replace(/,/g, '')
    }).join(',')
  )
  const csv = '\uFEFF' + [headers.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `预算执行-${activeTab.value === 'rows' ? '按预算单' : '按预算科目'}-${searchValues.fiscalYear}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ F8 打印快捷键 ═══
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[预算执行] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  loadDepartments()
  fetchSummary()
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
.toolbar-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.list-title {
  font-size: 14px;
  font-weight: 600;
}
.over-tag {
  cursor: pointer;
}

.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}
.search-grid {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.search-field-item {
  display: flex;
  align-items: center;
  min-width: 0;
}
.search-field-item :deep(.ant-input) {
  width: 170px;
}
.search-select-wrap {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
}
.search-select-wrap:hover {
  border-color: #4096ff;
}
.search-select-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
  flex-shrink: 0;
  padding-left: 8px;
}
.search-select-wrap :deep(.ant-select) {
  flex: 1;
  min-width: 130px;
}
.search-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  display: flex;
  align-items: center;
}

.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.stat-cards {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.rate-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rate-cell :deep(.ant-progress) {
  flex: 1;
  min-width: 50px;
  margin: 0;
}
.rate-text {
  font-size: 12px;
  white-space: nowrap;
}

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}
.text-warning {
  color: #faad14;
  font-weight: 600;
}
.text-muted {
  color: rgba(0, 0, 0, 0.35);
}

.detail-desc {
  margin-bottom: 16px;
}
.warn-banner {
  margin-bottom: 16px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
}
.warn-banner.is-over {
  background: #fff1f0;
  border: 1px solid #ffccc7;
  color: #cf1322;
}
.warn-banner.is-warn {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  color: #d48806;
}
.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  margin: 16px 0 8px;
}
.drawer-section-tip {
  font-weight: 400;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-left: 8px;
}

:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
