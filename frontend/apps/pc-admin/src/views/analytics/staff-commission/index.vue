<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        业务员提成（分析 → 提成分析 → 提成统计 → 业务员提成，菜单 80441）
        对标 ql361：**计算型页面**——首屏不预加载，空态文案「还没有内容哦，点击查询才能计算提成统计数据!」，
        点「查询」才计算；单视图（tabs={}）；工具栏 打印(F8)｜导出｜刷新；
        有「页面配置」弹窗实据（tool-results/ql361/分析-live/shots/业务员提成-页面配置弹窗.png）→ 接 PageConfigPanel。
        ⚠️ 列清单判定依据（对标 columnConfig.err="no gear"、§3.2 明确「待复核」，故不照抄、不编造）：
          取「本页原有源码列（已跑通）」∩「commissionApi 真实返回字段」：
          StaffCommissionSummaryItem = referrerId / staffName / recordCount / orderCount /
          totalOrderAmount / totalCommissionAmount / settledCommissionAmount / settledCount /
          unsettledCommissionAmount / unsettledCount。
          对标 §3.3 的 DOM 残留表头（按金额/按毛利/按数量/按回款/按单价、各类基数与分项提成）在后端
          erp_commission_record（orderAmount/commissionRate/commissionAmount/referrerId/status…）中**无对应字段**，
          且对标侧本身未确认 → 一律不做（不编造列）。
        ⚠️ 查询项：后端 /erp/marketing/commission/staff-summary/page 仅支持 page/size/keyword/startDate/endDate，
          故仅落地「时间快捷段（7 段，含上周）/ 日期范围 / 经手人」；对标的 部门/商品/单据类型/销售类型/
          按已结算提成/查询方案 后端无对应参数，未落地（见汇报）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：时间快捷段（对标 7 段，含「上周」；不含近一周/本年） ═══ -->
        <template #toolbar-left>
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES_7"
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

        <!-- ═══ 查询区（横向网格：日期范围 + 经手人） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('commission.dateRange')" class="search-item">
                <span class="search-label">提成日期</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 240px"
                  value-format="YYYY-MM-DD"
                  @change="quickDate = ''"
                />
              </div>
              <div v-if="isQueryVisible('commission.handler')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <span v-if="!queried" class="search-tip">还没有内容哦，点击查询才能计算提成统计数据!</span>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（不查询不出数：未查询时 min-rows=0 显示空提示） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="queried ? 20 : 0"
              row-key="rowKey"
              storage-key="analytics-staff-commission-columns"
              global-config-key="analytics-staff-commission-columns"
            >
              <template #staffNameCell="{ record }">
                <span>{{ record.staffName || '-' }}</span>
              </template>
              <template #amountCell="{ record, column }">
                <span>{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template #countCell="{ record, column }">
                <span>{{ formatNumber(record[column.key]) }}</span>
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

      <!-- ═══ 页面配置（对标有「业务员提成-页面配置弹窗」实据 → 接 PageConfigPanel） ═══ -->
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

    <!-- 打印：结果集打印 -->
    <PrintDialog ref="printDialogRef" page-code="analytics-staff-commission" :print-data="printData" />
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
import { commissionApi } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsStaffCommission' })

/** 时间快捷段（对标实测 7 段：昨日/今日/本周/上周/本月/上月/近三月，**不含**近一周、本年） */
const QUICK_DATES_7 = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '上周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' }
]

function pad(n: number) {
  return String(n).padStart(2, '0')
}
function fmt(d: Date) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 快捷段 → [start, end]（yyyy-MM-dd） */
function quickRange(key: string): [string, string] {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  switch (key) {
    case 'yesterday': {
      const d = new Date(y, m, now.getDate() - 1)
      return [fmt(d), fmt(d)]
    }
    case 'today':
      return [fmt(now), fmt(now)]
    case 'week': {
      const day = now.getDay() === 0 ? 7 : now.getDay()
      return [fmt(new Date(y, m, now.getDate() - day + 1)), fmt(now)]
    }
    case 'lastWeek': {
      const day = now.getDay() === 0 ? 7 : now.getDay()
      const thisMonday = new Date(y, m, now.getDate() - day + 1)
      const lastMonday = new Date(y, m, now.getDate() - day - 6)
      const lastSunday = new Date(thisMonday.getTime() - 86400000)
      return [fmt(lastMonday), fmt(lastSunday)]
    }
    case 'lastMonth':
      return [fmt(new Date(y, m - 1, 1)), fmt(new Date(y, m, 0))]
    case 'last3Month':
      return [fmt(new Date(y, m - 2, 1)), fmt(now)]
    case 'month':
    default:
      return [fmt(new Date(y, m, 1)), fmt(now)]
  }
}

const quickDate = ref('month')
const dateRange = ref<[string, string] | null>(quickRange('month'))
const query = reactive({ keyword: '' })

const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
/** 计算型页面语义：未点「查询」不出数（对标首屏空态） */
const queried = ref(false)

// ═══ 列定义（对标列清单未定标 → 取真实返回字段，见文件头说明） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'staffName', title: '业务员', type: 'slot', slotName: 'staffNameCell', width: 130, fixed: 'left' },
  { key: 'recordCount', title: '提成记录数', type: 'slot', slotName: 'countCell', width: 110, align: 'right' },
  { key: 'orderCount', title: '成单数', type: 'slot', slotName: 'countCell', width: 90, align: 'right' },
  { key: 'totalOrderAmount', title: '订单金额合计', type: 'slot', slotName: 'amountCell', width: 130, align: 'right' },
  { key: 'totalCommissionAmount', title: '提成金额合计', type: 'slot', slotName: 'amountCell', width: 130, align: 'right' },
  { key: 'settledCommissionAmount', title: '已结提成', type: 'slot', slotName: 'amountCell', width: 120, align: 'right' },
  { key: 'settledCount', title: '已结笔数', type: 'slot', slotName: 'countCell', width: 100, align: 'right' },
  { key: 'unsettledCommissionAmount', title: '未结提成', type: 'slot', slotName: 'amountCell', width: 120, align: 'right' },
  { key: 'unsettledCount', title: '未结笔数', type: 'slot', slotName: 'countCell', width: 100, align: 'right' }
]

// ═══ 取数 ═══
async function fetchData() {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value || []
    const res: any = await commissionApi.staffSummaryPage({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: query.keyword || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    dataSource.value = (res?.records || []).map((r: any) => ({
      ...r,
      rowKey: String(r.referrerId ?? r.staffName ?? Math.random())
    }))
    pagination.total = Number(res?.total) || 0
    queried.value = true
  } catch (e) {
    console.warn('[业务员提成] 取数失败', e)
    message.error('获取数据失败')
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
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

function handleRefresh() {
  return fetchData()
}

function handleReset() {
  quickDate.value = 'month'
  dateRange.value = quickRange('month')
  query.keyword = ''
  return handleSearch()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickRange(key)
  handleSearch()
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'commission.dateRange', label: '提成日期', visible: true },
  { key: 'commission.handler', label: '经手人', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
/** 打印配置项（对标有打印(F8)；打印只对当前操作员生效） */
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-staff-commission-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 格式化 ═══
function formatNumber(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 打印 / 导出列：剔除序号列 */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  columns.filter(c => c.key !== 'rowNo')
)

const MONEY_KEYS = [
  'totalOrderAmount', 'totalCommissionAmount',
  'settledCommissionAmount', 'unsettledCommissionAmount'
]

function cellText(col: DetailColumnConfig, r: any): string {
  return MONEY_KEYS.includes(col.key) ? formatMoney(r[col.key]) : formatNumber(r[col.key])
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML 再调浏览器打印，现在交给 PrintDialog：
// 列与行由页面给，模板负责版式（列随「打印/导出列」变化，故 useDataColumns）。
const { printDialogRef, printData, handlePrint: doPrint } = useListPrint({
  pageCode: 'analytics-staff-commission',
  title: () => `业务员提成（${dateRange.value?.[0] || ''} ~ ${dateRange.value?.[1] || ''}）`,
  rows: () => dataSource.value,
  columns: () => printableColumns.value,
  useDataColumns: true,
  emptyTip: '没有可打印的数据',
})

// 保留原有前置校验：未点「查询」不出数（界面会先提示）
function handlePrint() {
  if (!queried.value) {
    message.warning('请先点「查询」计算提成统计数据')
    return
  }
  doPrint()
}

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（逐页取全量 → CSV；未查询不出数） ═══
const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 100
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const [startDate, endDate] = dateRange.value || []
    const res: any = await commissionApi.staffSummaryPage({
      page: p,
      size,
      keyword: query.keyword || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
    const list: any[] = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  if (!queried.value) {
    message.warning('请先点「查询」计算提成统计数据')
    return
  }
  const cols = printableColumns.value
  executeExport({
    fileName: '业务员提成',
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => dataSource.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[业务员提成] 页面异常', err)
}

onMounted(() => {
  // 计算型页面：首屏不预加载（对标空态），仅在用户点「查询」后取数
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
.search-tip { color: #fa8c16; font-size: 12px; margin-right: 8px; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
