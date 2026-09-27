<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        查资金（分析 → 财务分析 → 查资金，菜单 80453）
        对标 ql361：**3 个视图 Tab**（资金余额 / 收支汇总 / 资金流水），逐 Tab 独立列定义 + 独立查询项 +
        独立列配置 storage-key；底部合计行；工具栏 刷新｜打印(F8)｜导出。
        列名与顺序逐字取自《查资金开发文档》§3：
          · 资金余额 6 列 / 默认 6（发生额为分组表头，下辖借方/贷方）
          · 收支汇总 73 列 / 默认 72（仅「预收款使用」默认隐藏）
          · 资金流水 14 列 / 默认 9（defaultHidden 5）

        取数（真实接口，无硬编码数据）：
          · 资金余额：reportApi.getTrialBalancePage → /erp/finance/report/v2/trial-balance-page
            subjectKeyword=1001 / 1002（库存现金 / 银行存款，含下级科目），按科目层级给出
            期初余额 / 本期借贷发生额 / 期末余额。
          · 收支汇总：capitalFlowApi.exportList → /erp/capital-flow/export
            资金流水逐笔取回后**按日 × 账户**透视（收入合计 / 支出合计 / 净收入合计 / 各账户收支 / 预收款使用），
            口径与对标「按日收支汇总矩阵」一致。
          · 资金流水：capitalFlowApi.getPage → /erp/capital-flow/page（分页）。
            行级「对账标记」调用 capitalFlowApi.toggleReconcile（PUT /erp/capital-flow/reconcile/{id}，既有未接线端点）。
        后端缺口（见开发文档 §5，已在汇报中列出）：无「按会计科目层级汇总资金余额」与「按日×账户聚合收付」的专用端点
        （分别以科目余额表与流水透视替代）；`/erp/capital-flow/statistics` 的 startDate/endDate 参数后端未生效，
        故本页合计行按当前数据实时汇总，不采用该端点。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-check-fund\index.vue-query-scheme"
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

        <!-- ═══ 查询区（对标实测：日期范围 + 日期类型 + 单据类型 + 账户 + 往来单位 + 经手人 + 流水类型 + 显示红冲） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible(`${activeTab}.dateRange`)" class="search-item">
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
              <div v-if="isQueryVisible(`${activeTab}.dateType`)" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select
                  v-model:value="query.dateType"
                  size="small"
                  style="width: 120px"
                  disabled
                  title="资金端点均无日期类型条件（统一按发生日期），待补"
                  :options="DATE_TYPE_OPTIONS"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.docType`)" class="search-item">
                <span class="search-label">单据类型</span>
                <a-select
                  v-model:value="query.docType"
                  size="small"
                  allow-clear
                  placeholder="全部单据"
                  style="width: 150px"
                  disabled
                  title="资金流水端点按「流水类型」过滤，无独立单据类型条件，待补"
                  :options="FLOW_TYPE_OPTIONS"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.account`)" class="search-item">
                <span class="search-label">账户</span>
                <a-input
                  v-model:value="query.account"
                  size="small"
                  placeholder="账户"
                  allow-clear
                  disabled
                  style="width: 140px"
                  title="资金流水端点无账户条件，待补"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.partyName`)" class="search-item">
                <span class="search-label">往来单位</span>
                <a-input
                  v-model:value="query.partyName"
                  size="small"
                  placeholder="往来单位"
                  allow-clear
                  disabled
                  style="width: 160px"
                  title="资金流水端点无往来单位条件（可按 partyId 精确匹配），待补"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.handlerName`)" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.handlerName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  disabled
                  style="width: 130px"
                  title="资金流水端点无经手人条件，待补"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.flowType`)" class="search-item">
                <span class="search-label">流水类型</span>
                <a-select
                  v-model:value="query.flowType"
                  size="small"
                  allow-clear
                  placeholder="全部"
                  style="width: 140px"
                  :options="FLOW_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox
                  v-model:checked="query.showReversed"
                  disabled
                  title="资金流水端点无红冲口径开关，待补"
                >
                  显示红冲
                </a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（逐 Tab 独立列配置 storage-key + 合计行） ═══ -->
        <template #table>
          <div class="table-area">
            <div v-if="activeTab === 'flow' && flowDrill" class="drill-bar">
              <a-tag closable color="blue" @close="clearDrill">
                下钻过滤：{{ flowDrill.kind === 'account' ? '账户' : '日期' }} = {{ flowDrill.value }}（本页内过滤）
              </a-tag>
            </div>
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              :storage-key="`analytics-check-fund-columns-${activeTab}`"
              :global-config-key="`analytics-check-fund-columns-${activeTab}`"
            >
              <template #actionCell="{ record }">
                <a-button v-if="activeTab === 'balance'" type="link" size="small" @click="drillToFlow('account', record.subjectName)">对账</a-button>
                <a-button v-else-if="activeTab === 'statement'" type="link" size="small" @click="drillToFlow('date', record.date)">明细</a-button>
              </template>
              <template #reconcileFlagCell="{ record }">
                <a-tag
                  :color="Number(record.reconcileFlag) === 1 ? 'green' : 'default'"
                  class="reconcile-tag"
                  @click="toggleReconcile(record)"
                >
                  {{ Number(record.reconcileFlag) === 1 ? '已对账' : '未对账' }}
                </a-tag>
              </template>
              <template #flowTypeCell="{ record }">
                {{ FLOW_TYPE_MAP[record.flowType] || record.flowType || '-' }}
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

      <!-- ═══ 页面配置（对标有逐 Tab「页面配置弹窗」实测截图 → 逐 Tab 独立 storage-key） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="activeCfg.queryFields.value"
        :function-buttons-config="activeCfg.functionButtons.value"
        :default-query-fields-config="activeQueryDefaults"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="activeCfg.pageConfigStorageKey"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="activeCfg.handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="analytics-check-fund"
      :print-data="printData"
    />
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
import { capitalFlowApi, reportApi } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { formatMoney } from '../shared/docActions'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsCheckFund' })

/** 视图 Tab（顺序逐字取自对标实测：资金余额 / 收支汇总 / 资金流水） */
const TABS = [
  { key: 'balance', label: '资金余额' },
  { key: 'statement', label: '收支汇总' },
  { key: 'flow', label: '资金流水' }
]
const activeTab = ref('balance')

const quickDate = ref('month')

const DATE_TYPE_OPTIONS = [
  { label: '单据日期', value: 'bizDate' },
  { label: '记账日期', value: 'bookkeepingDate' }
]

/** 流水/单据类型（与后端 CapitalFlow flowType 赋值口径一致，含费用支付 CASH_TRANSFER/EXPENSE_DOC） */
const FLOW_TYPE_MAP: Record<string, string> = {
  RECEIPT: '收款',
  PAYMENT: '付款',
  PRE_RECEIPT: '预收',
  PRE_PAYMENT: '预付',
  OFFSET: '核销',
  PRE_RECEIPT_REFUND: '预收退还',
  PRE_RECEIPT_FORFEIT: '预收没收',
  PRE_PAYMENT_REFUND: '预付退还',
  PRE_PAYMENT_RECOVER: '预付收回',
  EXPENSE_DOC: '费用支付',
  CASH_TRANSFER: '现转款'
}
const FLOW_TYPE_OPTIONS = Object.entries(FLOW_TYPE_MAP).map(([value, label]) => ({ label, value }))

const query = reactive({
  dateRange: quickDateRange('month') as [string, string],
  dateType: 'bizDate',
  docType: undefined as string | undefined,
  account: '',
  partyName: '',
  handlerName: '',
  flowType: undefined as string | undefined,
  showReversed: false
})

const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
/** 当前 Tab 的合计行取值（key → 数值） */
const summaryMap = ref<Record<string, number>>({})

function num(v: any): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

// ═══ Tab1「资金余额」列定义：6 列 / 默认 6（发生额为分组表头，下辖借方/贷方） ═══
const balanceColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'subjectName', title: '科目名称', width: 220 },
  { key: 'subjectCode', title: '科目编号', width: 130 },
  { key: 'openingBalance', title: '期初金额', width: 130, align: 'right', formatter: v => formatMoney(v) },
  {
    key: '__accrual',
    title: '发生额',
    children: [
      { key: 'periodDebit', title: '借方', width: 130, align: 'right', formatter: v => formatMoney(v) },
      { key: 'periodCredit', title: '贷方', width: 130, align: 'right', formatter: v => formatMoney(v) }
    ]
  },
  { key: 'closingBalance', title: '期末余额', width: 130, align: 'right', formatter: v => formatMoney(v) }
]

// ═══ Tab2「收支汇总」列定义：73 列 / 默认 72（仅「预收款使用」默认隐藏） ═══
// 账户列顺序逐字取自《查资金开发文档》§3 第 5–72 项（每个账户下辖「收入 / 支出」两列）
const STATEMENT_ACCOUNTS = [
  '现金', '农业银行一', '农业银行二', '建设银行', '工商银行', '中国银行', '酒泉农商行', '皇朝银行对公账户',
  '信合对公账户', '兰州银行', '农行信用卡', '农行快E贷', '农行乐易分期', '工行信用卡', '交行信用卡', '建行信用卡',
  '建行快贷', '高晓丽建行信用卡', '高晓丽建行乐易分期', '高晓丽农行信用卡', '腾讯财付通', '易宝账户', '杨生淮个人微信',
  '高晓丽农业银行卡', '干饭郎对公帐户', '众鲜云集交通对公帐户', '糖仁食品对公帐户', '兰州银行快袋', '中国农业银行',
  '中国建设银行', '微信账户', '开单微信钱包', '支付宝账户', '蚂蚁花呗'
]

const statementColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'date', title: '日期', width: 120 },
  { key: 'incomeTotal', title: '收入合计', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'expenseTotal', title: '支出合计', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'netTotal', title: '净收入合计', width: 130, align: 'right', formatter: v => formatMoney(v) },
  ...STATEMENT_ACCOUNTS.map(name => ({
    key: `acct_${name}`,
    title: name,
    children: [
      { key: `acct_${name}_in`, title: '收入', width: 110, align: 'right' as const, formatter: (v: any) => formatMoney(v) },
      { key: `acct_${name}_out`, title: '支出', width: 110, align: 'right' as const, formatter: (v: any) => formatMoney(v) }
    ]
  })),
  { key: 'preReceiptUsed', title: '预收款使用', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true }
]

// ═══ Tab3「资金流水」列定义：14 列 / 默认 9（defaultHidden 5） ═══
const flowColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'reconcileFlag', title: '对账标记', type: 'slot', slotName: 'reconcileFlagCell', width: 100 },
  { key: 'bookkeepingDate', title: '记账日期', width: 120 },
  { key: 'refNo', title: '单据编号', width: 180 },
  { key: 'bizDate', title: '单据日期', width: 120 },
  { key: 'flowType', title: '单据类型', type: 'slot', slotName: 'flowTypeCell', width: 110, defaultHidden: true },
  { key: 'partyName', title: '往来单位', width: 160 },
  { key: 'handlerName', title: '经手人', width: 110 },
  { key: 'departmentName', title: '部门', width: 110, defaultHidden: true },
  { key: 'account', title: '账户', width: 150 },
  { key: 'income', title: '收入', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'expense', title: '支出', width: 120, align: 'right', formatter: v => formatMoney(v) },
  { key: 'creatorName', title: '制单人', width: 110, defaultHidden: true },
  { key: 'createTime', title: '制单时间', width: 160, defaultHidden: true },
  { key: 'remark', title: '备注', width: 200, defaultHidden: true }
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'balance') return balanceColumns
  if (activeTab.value === 'statement') return statementColumns
  return flowColumns
})

/** 合计行（逐 Tab 的合计列不同） */
const summaryColumns = computed(() => {
  const m = summaryMap.value || {}
  if (activeTab.value === 'balance') {
    return [
      { key: 'openingBalance', value: num(m.openingBalance) },
      { key: 'periodDebit', value: num(m.periodDebit) },
      { key: 'periodCredit', value: num(m.periodCredit) },
      { key: 'closingBalance', value: num(m.closingBalance) }
    ]
  }
  if (activeTab.value === 'statement') {
    return [
      { key: 'incomeTotal', value: num(m.incomeTotal) },
      { key: 'expenseTotal', value: num(m.expenseTotal) },
      { key: 'netTotal', value: num(m.netTotal) },
      { key: 'preReceiptUsed', value: num(m.preReceiptUsed) }
    ]
  }
  return [
    { key: 'income', value: num(m.income) },
    { key: 'expense', value: num(m.expense) }
  ]
})

// ═══ 页面配置（逐 Tab 独立一套） ═══
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const QUERY_FIELDS_BY_TAB: Record<string, QueryFieldSetting[]> = {
  balance: [
    { key: 'balance.dateRange', label: '日期', visible: true },
    { key: 'balance.dateType', label: '日期类型', visible: true },
    { key: 'balance.account', label: '账户', visible: true }
  ],
  statement: [
    { key: 'statement.dateRange', label: '日期', visible: true },
    { key: 'statement.dateType', label: '日期类型', visible: true },
    { key: 'statement.account', label: '账户', visible: true }
  ],
  flow: [
    { key: 'flow.dateRange', label: '日期', visible: true },
    { key: 'flow.dateType', label: '日期类型', visible: true },
    { key: 'flow.docType', label: '单据类型', visible: true },
    { key: 'flow.account', label: '账户', visible: true },
    { key: 'flow.partyName', label: '往来单位', visible: true },
    { key: 'flow.handlerName', label: '经手人', visible: true },
    { key: 'flow.flowType', label: '流水类型', visible: true }
  ]
}

const cfgByTab = {
  balance: useAnalyticsPageConfig({
    storageKey: 'analytics-check-fund-page-config-balance',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.balance,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  statement: useAnalyticsPageConfig({
    storageKey: 'analytics-check-fund-page-config-statement',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.statement,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  flow: useAnalyticsPageConfig({
    storageKey: 'analytics-check-fund-page-config-flow',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.flow,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  })
}
type Cfg = typeof cfgByTab.balance
const activeCfg = computed<Cfg>(() => (cfgByTab as any)[activeTab.value])
const activeQueryDefaults = computed(() => (QUERY_FIELDS_BY_TAB as any)[activeTab.value])
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  return activeCfg.value.isQueryVisible(key)
}
function isButtonEnabled(key: string): boolean {
  return activeCfg.value.isButtonEnabled(key)
}

/** 日期范围 → 会计年 / 会计月（资金余额按会计期间取数） */
function periodRange() {
  const [start, end] = query.dateRange
  const endDate = new Date(String(end).replace(/-/g, '/'))
  const startDate = new Date(String(start).replace(/-/g, '/'))
  const fiscalYear = endDate.getFullYear()
  const startPeriod = startDate.getFullYear() === fiscalYear ? startDate.getMonth() + 1 : 1
  const endPeriod = endDate.getMonth() + 1
  return { fiscalYear, startPeriod, endPeriod }
}

/** 资金余额：库存现金（1001）与银行存款（1002）两级科目行（含下级科目），合并去重 */
async function fetchBalance() {
  const { fiscalYear, startPeriod, endPeriod } = periodRange()
  const seen = new Map<string, any>()
  for (const keyword of ['1001', '1002']) {
    const res: any = await reportApi.getTrialBalancePage({
      fiscalYear,
      startPeriod,
      endPeriod,
      subjectLevel: 2,
      subjectKeyword: keyword
    })
    for (const r of res?.records || []) {
      if (r?.subjectCode && !seen.has(r.subjectCode)) seen.set(r.subjectCode, r)
    }
  }
  const all = [...seen.values()].map((r, idx) => ({
    rowKey: r.subjectCode || String(idx),
    subjectName: r.subjectName,
    subjectCode: r.subjectCode,
    openingBalance: num(r.openingBalance),
    periodDebit: num(r.periodDebit),
    periodCredit: num(r.periodCredit),
    closingBalance: num(r.closingBalance),
    level: r.level
  }))
  // 合计只累加一级科目，避免父子重复累加（后端 summary 亦按此口径）
  const top = all.filter(r => Number(r.level) === 1)
  summaryMap.value = {
    openingBalance: top.reduce((a, r) => a + r.openingBalance, 0),
    periodDebit: top.reduce((a, r) => a + r.periodDebit, 0),
    periodCredit: top.reduce((a, r) => a + r.periodCredit, 0),
    closingBalance: top.reduce((a, r) => a + r.closingBalance, 0)
  }
  balanceAll.value = all
  pagination.total = all.length
  applyClientPage(all)
}

/**
 * 取当前过滤条件下的资金流水全量（分页端点逐页取；单次 500 条、最多 20 页）
 *
 * 说明：不使用 capitalFlowApi.exportList —— 该封装把 `{ params, responseType: 'blob' }` 传给了
 * request.get 的第二参（会被判定为 options），导致查询参数丢失且响应被当作 Blob 返回。
 * 该问题在 api/finance/index.ts 中，本次不改动接口文件，已列入汇报。
 */
async function fetchAllFlows(): Promise<any[]> {
  const size = 500
  const all: any[] = []
  for (let p = 1; p <= 20; p++) {
    const res: any = await capitalFlowApi.getPage({
      pageNum: p,
      pageSize: size,
      flowType: query.flowType || undefined,
      startDate: query.dateRange[0],
      endDate: query.dateRange[1]
    })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

/** 收支汇总：资金流水按「日 × 账户」透视（口径与对标按日收支汇总矩阵一致） */
async function fetchStatement() {
  const flows = await fetchAllFlows()
  const byDate = new Map<string, any>()
  for (const f of flows) {
    const d = String(f.occurDate || f.createTime || '').slice(0, 10)
    if (!d) continue
    let row = byDate.get(d)
    if (!row) {
      row = { rowKey: d, date: d, incomeTotal: 0, expenseTotal: 0, preReceiptUsed: 0 }
      byDate.set(d, row)
    }
    const amt = num(f.amount)
    const isIn = f.direction === 'IN'
    if (isIn) row.incomeTotal += amt
    else row.expenseTotal += amt
    if (f.flowType === 'PRE_RECEIPT' && isIn) row.preReceiptUsed += amt
    const acct = f.bankAccount || f.bankName
    if (acct && STATEMENT_ACCOUNTS.includes(String(acct))) {
      const k = `acct_${acct}_${isIn ? 'in' : 'out'}`
      row[k] = num(row[k]) + amt
    }
  }
  const all = [...byDate.values()]
    .map(r => ({ ...r, netTotal: r.incomeTotal - r.expenseTotal }))
    .sort((a, b) => String(a.date).localeCompare(String(b.date)))
  pagination.total = all.length
  applyClientPage(all)
  summaryMap.value = {
    incomeTotal: all.reduce((a, r) => a + r.incomeTotal, 0),
    expenseTotal: all.reduce((a, r) => a + r.expenseTotal, 0),
    netTotal: all.reduce((a, r) => a + r.netTotal, 0),
    preReceiptUsed: all.reduce((a, r) => a + r.preReceiptUsed, 0)
  }
  statementAll.value = all
}

const statementAll = ref<any[]>([])
const balanceAll = ref<any[]>([])

/** 前端分页（资金余额 / 收支汇总两 Tab 为整表数据，按页切片展示） */
function applyClientPage(all: any[]) {
  const size = pagination.pageSize
  const start = (pagination.current - 1) * size
  dataSource.value = all.slice(start, start + size)
}

/** 资金流水行（流动到表格前的统一口径，导出/打印复用） */
function normalizeFlow(f: any) {
  const isIn = f.direction === 'IN'
  return {
    rowKey: String(f.id),
    id: f.id,
    reconcileFlag: num(f.reconcileFlag),
    bookkeepingDate: String(f.createTime || '').slice(0, 10),
    refNo: f.refNo,
    bizDate: String(f.occurDate || '').slice(0, 10),
    flowType: f.flowType,
    partyName: f.partyName,
    account: f.bankAccount || f.bankName,
    income: isIn ? num(f.amount) : undefined,
    expense: isIn ? undefined : num(f.amount),
    createTime: String(f.createTime || '').replace('T', ' ').slice(0, 19),
    remark: f.remark
  }
}

/** 资金流水：服务端分页 + 下钻过滤（本页内过滤，端点无账户/关键字条件） */
async function fetchFlows() {
  const res: any = await capitalFlowApi.getPage({
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    flowType: query.flowType || undefined,
    direction: undefined,
    startDate: query.dateRange[0],
    endDate: query.dateRange[1]
  })
  let rows = (res?.records || []).map(normalizeFlow)
  if (flowDrill.value) {
    const { kind, value } = flowDrill.value
    rows = rows.filter((r: any) => (kind === 'account' ? r.account === value : r.bizDate === value))
  }
  dataSource.value = rows
  pagination.total = flowDrill.value ? rows.length : Number(res?.total) || 0
  summaryMap.value = {
    income: rows.reduce((a, r) => a + num(r.income), 0),
    expense: rows.reduce((a, r) => a + num(r.expense), 0)
  }
}

async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'balance') await fetchBalance()
    else if (activeTab.value === 'statement') await fetchStatement()
    else await fetchFlows()
  } catch (e: any) {
    console.warn('[查资金] 取数失败', e)
    message.error('获取资金数据失败')
    dataSource.value = []
    pagination.total = 0
    summaryMap.value = {}
  } finally {
    loading.value = false
  }
}

// ═══ 下钻（Tab1「对账」→ 账户流水；Tab2「明细」→ 当日流水） ═══
const flowDrill = ref<{ kind: 'account' | 'date'; value: string } | null>(null)

function drillToFlow(kind: 'account' | 'date', value: string) {
  flowDrill.value = { kind, value }
  activeTab.value = 'flow'
  pagination.current = 1
  return fetchData()
}

function clearDrill() {
  flowDrill.value = null
  pagination.current = 1
  return fetchData()
}

/** 对账标记切换（既有未接线端点 PUT /erp/capital-flow/reconcile/{id}） */
async function toggleReconcile(record: any) {
  try {
    const next = Number(record.reconcileFlag) === 1 ? 0 : 1
    await capitalFlowApi.toggleReconcile(record.id, next)
    message.success(next === 1 ? '已标记对账' : '已取消对账标记')
    await fetchData()
  } catch (e: any) {
    console.warn('[查资金] 对账标记切换失败', e)
    message.error('对账标记切换失败')
  }
}

// ═══ 查询交互 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  flowDrill.value = null
  pagination.current = 1
  return fetchData()
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
  const sizeChanged = size !== pagination.pageSize
  pagination.pageSize = size
  pagination.current = sizeChanged ? 1 : page
  if (activeTab.value === 'flow') return fetchData()
  // 资金余额 / 收支汇总为整表数据 → 本地切片
  applyClientPage(activeTab.value === 'statement' ? statementAll.value : balanceAll.value)
  return Promise.resolve()
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
    docType: undefined,
    account: '',
    partyName: '',
    handlerName: '',
    flowType: undefined,
    showReversed: false
  })
  handleSearch()
}

// ═══ 打印(F8) ═══
/** 叶子列（资金余额/收支汇总含分组表头，打印与导出按叶子列展开） */
function leafColumns(cols: DetailColumnConfig[]): DetailColumnConfig[] {
  const out: DetailColumnConfig[] = []
  for (const c of cols) {
    if (c.children?.length) out.push(...leafColumns(c.children))
    else if (c.key !== 'rowNo' && c.key !== 'action') out.push(c)
  }
  return out
}

const printColumns = computed(() => leafColumns(activeColumns.value))

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open 打印窗口，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列随 Tab 变（余额/收支汇总/资金流水，均 computed）→ 冻结不进模板，明确按数据列打。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-check-fund',
  title: () => `${TABS.find(t => t.key === activeTab.value)?.label || '查资金'}（${query.dateRange[0]} ~ ${query.dateRange[1]}）`,
  useDataColumns: true,
  columns: () => printColumns.value,
  rows: () => dataSource.value,
  // 原表尾合计逐 Tab 取后端 summary（非本页求和），交给页脚打一行文字
  totalText: () => {
    const titleOf = new Map(printColumns.value.map(c => [c.key, c.title]))
    return '合计：' + summaryColumns.value
      .map(s => `${titleOf.get(s.key) || s.key} ${formatMoney(s.value)}`)
      .join('，')
  },
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV；逐 Tab 取当前过滤条件下全量） ═══
const { execute: executeExport, exporting } = useExport()

function toExportRow(r: any): string[] {
  return printColumns.value.map(c => {
    const v = r[c.key]
    if (v === undefined || v === null) return ''
    return c.align === 'right' ? formatMoney(v) : String(v)
  })
}

async function fetchAllRows(): Promise<any[]> {
  if (activeTab.value === 'statement') return statementAll.value
  if (activeTab.value === 'balance') {
    // 重新取一次全量（不切页）
    const { fiscalYear, startPeriod, endPeriod } = periodRange()
    const seen = new Map<string, any>()
    for (const keyword of ['1001', '1002']) {
      const res: any = await reportApi.getTrialBalancePage({ fiscalYear, startPeriod, endPeriod, subjectLevel: 2, subjectKeyword: keyword })
      for (const r of res?.records || []) if (r?.subjectCode && !seen.has(r.subjectCode)) seen.set(r.subjectCode, r)
    }
    return [...seen.values()].map(r => ({
      subjectName: r.subjectName,
      subjectCode: r.subjectCode,
      openingBalance: num(r.openingBalance),
      periodDebit: num(r.periodDebit),
      periodCredit: num(r.periodCredit),
      closingBalance: num(r.closingBalance)
    }))
  }
  const flows = await fetchAllFlows()
  return flows.map(normalizeFlow)
}

function handleExport() {
  executeExport({
    fileName: `查资金-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: printColumns.value.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(toExportRow),
    fallbackRows: () => dataSource.value.map(toExportRow)
  })
}

function handleError(err: any) {
  console.error('[查资金] 页面异常', err)
}

onMounted(async () => {
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
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.drill-bar { padding: 4px 8px; }
.reconcile-tag { cursor: pointer; }
</style>
