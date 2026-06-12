<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="销售报表" full-height>
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <StatusTag :status="dataFreshness" :map="DATA_FRESHNESS" />
            <span v-if="lastUpdateTime" class="update-time">
              数据更新: {{ lastUpdateTime }}
            </span>
            <span v-if="lastUpdateTimestamp && relativeTimeText" class="update-time relative-time">
              上次更新: {{ relativeTimeText }}
            </span>
            <span v-if="dataDelayWarning" class="delay-warning">
              <WarningOutlined /> 数据延迟
            </span>
            <span v-if="autoRefreshEnabled && autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
          </span>
          <a-tooltip title="自动刷新 (每30秒)">
            <a-switch v-model:checked="autoRefreshEnabled" size="small" />
          </a-tooltip>
          <a-tooltip title="F5 刷新 | Ctrl+E 导出 | Ctrl+N 新建">
            <a-button size="small" v-permission="'erp:sales:refresh'" @click="debounceClick('refresh', handleRefresh)">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-tooltip>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>N</kbd> 新建</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
          </span>
          <a-tooltip title="Ctrl+N 新建报表">
            <a-button size="small" v-permission="'erp:sales:create'" @click="debounceClick('create', handleCreate)">
              <template #icon><PlusOutlined /></template>
              新建
            </a-button>
          </a-tooltip>
          <a-button size="small" v-permission="'erp:sales:openexportmodal'" @click="handleOpenExportModal">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
          <PrintButton page-code="erp/sales-report" button-size="small" tooltip="打印当前报表" />
        </a-space>
      </template>

      <!-- 搜索筛选 -->
      <template #filter>
        <SearchBar
          :fields="searchFields"
          :loading="loading"
          @search="handleSearch"
          @reset="handleResetFilter"
          :expandable="false"
          :show-result-count="false"
        />
      </template>

      <!-- 比较模式与显示模式切换条 -->
      <div class="compare-toolbar">
        <a-space :size="16">
          <span class="toolbar-label">对比方式:</span>
          <a-radio-group v-model:value="compareMode" size="small" @change="handleCompareModeChange">
            <a-radio-button value="mom">环比</a-radio-button>
            <a-radio-button value="yoy">同比</a-radio-button>
          </a-radio-group>
          <a-divider type="vertical" />
          <span class="toolbar-label">显示:</span>
          <a-radio-group v-model:value="displayMode" size="small" @change="handleDisplayModeChange">
            <a-radio-button value="absolute">绝对值</a-radio-button>
            <a-radio-button value="growth">增长率</a-radio-button>
          </a-radio-group>
        </a-space>
        <div v-if="activeCardFilter" class="active-filter-tip">
          <a-tag closable @close="handleClearFilter">
            当前筛选: {{ filterLabelMap[activeCardFilter] || activeCardFilter }}
          </a-tag>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="summary-cards" style="padding: 16px 0;">
        <a-row :gutter="16">
          <a-col :span="6">
            <div
              class="summary-card"
              :class="{ clickable: true, active: activeCardFilter === 'amount' }"
              @click="handleCardClick('amount')"
            >
              <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <DollarOutlined />
              </div>
              <div class="summary-content">
                <div class="summary-title">本月销售额</div>
                <div class="summary-value">¥{{ formatAmount(summary.monthAmount) }}</div>
                <!-- 上期值对比 -->
                <div v-if="summary.prevMonthAmount !== undefined" class="summary-prev">
                  上期: ¥{{ formatAmount(summary.prevMonthAmount) }}
                </div>
                <!-- 增长率 -->
                <div v-if="currentGrowth.amount !== null" class="summary-change" :class="(currentGrowth.amount ?? 0) >= 0 ? 'positive' : 'negative'">
                  <ArrowUpOutlined v-if="(currentGrowth.amount ?? 0) >= 0" />
                  <ArrowDownOutlined v-else />
                  {{ Math.abs(currentGrowth.amount ?? 0) }}%
                </div>
                <!-- 迷你趋势图 -->
                <div v-if="summary.amountSparkline && summary.amountSparkline.length > 1" class="sparkline">
                  <svg :width="sparklineWidth" :height="sparklineHeight" :viewBox="`0 0 ${sparklineWidth} ${sparklineHeight}`">
                    <polyline
                      :points="getSparklinePoints(summary.amountSparkline)"
                      :fill="(currentGrowth.amount ?? 0) >= 0 ? 'none' : 'none'"
                      :stroke="(currentGrowth.amount ?? 0) >= 0 ? '#52c41a' : '#f5222d'"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div
              class="summary-card"
              :class="{ clickable: true, active: activeCardFilter === 'orders' }"
              @click="handleCardClick('orders')"
            >
              <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <ShoppingOutlined />
              </div>
              <div class="summary-content">
                <div class="summary-title">本月订单数</div>
                <div class="summary-value">{{ summary.monthOrders }}</div>
                <div v-if="summary.prevMonthOrders !== undefined" class="summary-prev">
                  上期: {{ summary.prevMonthOrders }}
                </div>
                <div v-if="currentGrowth.orders !== null" class="summary-change" :class="(currentGrowth.orders ?? 0) >= 0 ? 'positive' : 'negative'">
                  <ArrowUpOutlined v-if="(currentGrowth.orders ?? 0) >= 0" />
                  <ArrowDownOutlined v-else />
                  {{ Math.abs(currentGrowth.orders ?? 0) }}%
                </div>
                <div v-if="summary.ordersSparkline && summary.ordersSparkline.length > 1" class="sparkline">
                  <svg :width="sparklineWidth" :height="sparklineHeight" :viewBox="`0 0 ${sparklineWidth} ${sparklineHeight}`">
                    <polyline
                      :points="getSparklinePoints(summary.ordersSparkline)"
                      fill="none"
                      :stroke="(currentGrowth.orders ?? 0) >= 0 ? '#52c41a' : '#f5222d'"
                      stroke-width="1.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div
              class="summary-card"
              :class="{ clickable: true, active: activeCardFilter === 'customers' }"
              @click="handleCardClick('customers')"
            >
              <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <TeamOutlined />
              </div>
              <div class="summary-content">
                <div class="summary-title">活跃客户</div>
                <div class="summary-value">{{ summary.activeCustomers }}</div>
                <div v-if="summary.prevActiveCustomers !== undefined" class="summary-prev">
                  上期: {{ summary.prevActiveCustomers }}
                </div>
                <div v-if="currentGrowth.customers !== null" class="summary-change" :class="(currentGrowth.customers ?? 0) >= 0 ? 'positive' : 'negative'">
                  <ArrowUpOutlined v-if="(currentGrowth.customers ?? 0) >= 0" />
                  <ArrowDownOutlined v-else />
                  {{ Math.abs(currentGrowth.customers ?? 0) }}%
                </div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div
              class="summary-card highlight"
              :class="{ clickable: true, active: activeCardFilter === 'growth' }"
              @click="handleCardClick('growth')"
            >
              <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <RiseOutlined />
              </div>
              <div class="summary-content">
                <div class="summary-title">{{ compareMode === 'yoy' ? '同比增长' : '环比增长' }}</div>
                <div class="summary-value">{{ comparisonGrowthRate }}%</div>
                <div class="summary-prev-label">
                  {{ compareMode === 'yoy' ? '与去年同期对比' : '与上期对比' }}
                </div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 月度趋势柱状图 -->
      <div v-if="trendDataCache.length > 1" class="trend-barchart">
        <div class="barchart-header">
          <span class="barchart-title">月度销售趋势</span>
          <span class="barchart-hint">近 {{ trendDataCache.length }} 个月</span>
        </div>
        <div class="barchart-container">
          <div
            v-for="(item, index) in trendDataCache"
            :key="index"
            class="barchart-bar-wrapper"
          >
            <div class="barchart-bar" :style="{ height: getBarHeight(item.sales) }">
              <span class="barchart-value">¥{{ formatCompactAmount(item.sales) }}</span>
            </div>
            <span class="barchart-label">{{ formatMonthLabel(item.date) }}</span>
          </div>
        </div>
      </div>

      <!-- 全局错误提示 -->
      <div v-if="sectionErrors.summary" class="error-banner">
        <a-alert
          type="error"
          message="数据加载失败"
          description="系统异常，请检查网络连接后重试"
          show-icon
          closable
          @close="sectionErrors.summary = false"
        >
          <template #action>
            <a-button size="small" type="primary" v-permission="'erp:sales:refresh'" @click="handleRefresh">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </template>
        </a-alert>
      </div>

      <!-- 各分区错误提示 -->
      <div v-if="hasSectionError" class="section-errors">
        <a-alert
          v-if="sectionErrors.statistics"
          type="warning"
          message="销售统计模块异常"
          description="该模块数据暂时不可用，其他模块不受影响"
          show-icon
          closable
          @close="sectionErrors.statistics = false"
        >
          <template #action>
            <a-button size="small" v-permission="'erp:sales:retrysection'" @click="handleRetrySection('statistics')">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </template>
        </a-alert>
        <a-alert
          v-if="sectionErrors.customer"
          type="warning"
          message="客户排行模块异常"
          description="该模块数据暂时不可用，其他模块不受影响"
          show-icon
          closable
          @close="sectionErrors.customer = false"
        >
          <template #action>
            <a-button size="small" v-permission="'erp:sales:retrysection'" @click="handleRetrySection('customer')">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </template>
        </a-alert>
        <a-alert
          v-if="sectionErrors.product"
          type="warning"
          message="商品排行模块异常"
          description="该模块数据暂时不可用，其他模块不受影响"
          show-icon
          closable
          @close="sectionErrors.product = false"
        >
          <template #action>
            <a-button size="small" v-permission="'erp:sales:retrysection'" @click="handleRetrySection('product')">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </template>
        </a-alert>
        <a-alert
          v-if="sectionErrors.trend"
          type="warning"
          message="销售趋势模块异常"
          description="该模块数据暂时不可用，其他模块不受影响"
          show-icon
          closable
          @close="sectionErrors.trend = false"
        >
          <template #action>
            <a-button size="small" v-permission="'erp:sales:retrysection'" @click="handleRetrySection('trend')">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </template>
        </a-alert>
      </div>

      <div class="report-content">
        <a-tabs v-model:activeKey="activeTab" type="card" size="small">
          <a-tab-pane key="statistics" tab="销售统计">
            <SalesStatistics ref="statisticsRef" />
          </a-tab-pane>
          <a-tab-pane key="customer" tab="客户排行">
            <CustomerRanking ref="customerRef" />
          </a-tab-pane>
          <a-tab-pane key="product" tab="商品排行">
            <ProductRanking ref="productRef" />
          </a-tab-pane>
          <a-tab-pane key="trend" tab="销售趋势">
            <SalesTrend ref="trendRef" />
          </a-tab-pane>
        </a-tabs>
      </div>

      <!-- 新建报表对话框 -->
      <a-modal
        v-model:open="showCreateModal"
        title="新建报表"
        ok-text="创建"
        cancel-text="取消"
        @ok="confirmCreate"
      >
        <a-input
          v-model:value="newAnalysisName"
          placeholder="请输入报表名称"
          @press-enter="confirmCreate"
        />
      </a-modal>

      <!-- 导出配置对话框 -->
      <a-modal
        v-model:open="showExportModal"
        title="导出报表"
        ok-text="确认导出"
        cancel-text="取消"
        :confirm-loading="exportLoading"
        @ok="handleExportConfirm"
      >
        <a-form layout="vertical">
          <a-form-item label="导出格式">
            <a-radio-group v-model:value="exportFormat">
              <a-radio-button value="xlsx">Excel (.xlsx)</a-radio-button>
              <a-radio-button value="csv">CSV (.csv)</a-radio-button>
              <a-radio-button value="pdf">PDF (.pdf)</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="导出范围">
            <a-radio-group v-model:value="exportScope">
              <a-radio-button value="current">当前统计数据</a-radio-button>
              <a-radio-button value="all">全量数据</a-radio-button>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="日期范围">
            <a-range-picker
              v-model:value="exportDateRange"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item v-if="exportScope === 'all'">
            <a-checkbox v-model:checked="exportIncludeDetail">包含子模块明细数据</a-checkbox>
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 明细数据抽屉 -->
      <a-drawer
        v-model:open="detailDrawerVisible"
        title="明细数据"
        placement="right"
        width="600"
        :loading="detailLoading"
      >
        <template v-if="detailData.length > 0">
          <div class="drawer-summary">
            <span>期间范围: {{ detailRange }}</span>
            <span>共 {{ detailData.length }} 条记录</span>
          </div>
          <vxe-table :data="detailData" border size="small" max-height="500" :row-config="{ isHover: true }">
            <vxe-column type="seq" title="#" width="50" />
            <vxe-column field="period" title="期间" width="110" />
            <vxe-column field="amount" title="金额" width="130">
              <template #default="{ row }">
                ¥{{ formatAmount(row.amount) }}
              </template>
            </vxe-column>
            <vxe-column field="quantity" title="数量" width="90" />
            <vxe-column field="growthRate" title="增长率" width="110">
              <template #default="{ row }">
                <span :class="(row.growthRate ?? 0) >= 0 ? 'text-positive' : 'text-negative'">
                  {{ row.growthRate != null ? `${row.growthRate >= 0 ? '+' : ''}${row.growthRate}%` : '-' }}
                </span>
              </template>
            </vxe-column>
          </vxe-table>
        </template>
        <EmptyState v-else title="暂无明细数据" size="small" :show-actions="false" />
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, reactive } from 'vue'
import {
  ReloadOutlined,
  SyncOutlined,
  WarningOutlined,
  DollarOutlined,
  ShoppingOutlined,
  TeamOutlined,
  RiseOutlined,
  ExportOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import type { StatusMap } from '@/utils/statusConfig'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import {
  salesReportApi,
  type SalesReportQuery,
  type ReportSummaryData,
  type ExportFileFormat,
  type CompareMode,
  type DisplayMode,
  type CardFilterKey,
  type SectionErrors,
  type TrendData
} from '@/api/sales-report'
import SalesStatistics from './components/SalesStatistics.vue'
import CustomerRanking from './components/CustomerRanking.vue'
import ProductRanking from './components/ProductRanking.vue'
import SalesTrend from './components/SalesTrend.vue'

// ── 类型定义 ──────────────────────────────────────────────

/** 增长率数据 */
interface GrowthData {
  amount: number | null
  orders: number | null
  customers: number | null
}

/** 明细数据项 */
interface DetailDataItem {
  period: string
  amount: number
  quantity: number
  growthRate: number | null
}

/** 子组件引用类型 */
interface SubComponentRef {
  handleQuery?: (params: Record<string, unknown>) => Promise<void>
}

// ── 常量配置 ──────────────────────────────────────────────

/** 数据新鲜度映射 */
const DATA_FRESHNESS: StatusMap = {
  fresh: { text: '数据正常', color: 'success' },
  error: { text: '数据异常', color: 'error' },
  loading: { text: '加载中', color: 'processing' },
}

/** 卡片筛选标签映射 */
const filterLabelMap: Record<string, string> = {
  amount: '销售额筛选',
  orders: '订单数筛选',
  customers: '活跃客户筛选',
  growth: '增长率筛选',
}

/** 迷你趋势图尺寸 */
const sparklineWidth = 80
const sparklineHeight = 24

/** 自动刷新间隔（毫秒） */
const AUTO_REFRESH_INTERVAL = 30000

/** 防抖延迟（毫秒） */
const DEBOUNCE_DELAY = 300

// ── 工具函数 ──────────────────────────────────────────────

function handleError(err: unknown) {
  console.warn('[销售报表] ErrorBoundary 捕获异常:', err)
}

/** 防抖点击 */
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = DEBOUNCE_DELAY) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

/** 金额格式化 */
const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

/**
 * 生成迷你趋势图 SVG polyline 点坐标
 * 将数据序列映射到指定宽高的 SVG 视口内
 */
function getSparklinePoints(values: number[]): string {
  if (!values || values.length < 2) return ''
  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = max - min || 1
  const padding = 2
  const drawHeight = sparklineHeight - padding * 2
  const stepX = sparklineWidth / (values.length - 1)
  return values
    .map((v, i) => {
      const x = i * stepX
      const y = sparklineHeight - padding - ((v - min) / range) * drawHeight
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

/** 格式化金额为简洁显示（用于柱状图标签） */
function formatCompactAmount(amount: number): string {
  if (amount >= 100000000) return (amount / 100000000).toFixed(1) + '亿'
  if (amount >= 10000) return (amount / 10000).toFixed(1) + '万'
  return amount.toLocaleString('zh-CN')
}

/** 计算柱状图高度百分比 */
function getBarHeight(value: number): string {
  const sales = trendDataCache.value.map(d => d.sales)
  const maxVal = Math.max(...sales, 1)
  const pct = (value / maxVal) * 100
  return `calc(${Math.max(pct, 2)}% + 20px)`
}

/** 格式化月份标签 */
function formatMonthLabel(dateStr: string): string {
  if (!dateStr) return ''
  // 尝试解析 YYYY-MM 或 YYYY-MM-DD 格式
  const parts = dateStr.split('-')
  if (parts.length >= 2) return `${parts[0].slice(2)}/${parts[1]}`
  return dateStr.slice(0, 7)
}

// ── 键盘快捷键 ────────────────────────────────────────────

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
    return
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleOpenExportModal()
    return
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    debounceClick('create', handleCreate)
  }
}

// ── 搜索字段配置 ─────────────────────────────────────────

const searchFields: any = [
  {
    name: 'dateRange',
    label: '日期范围',
    type: 'date-range',
  },
  {
    name: 'keyword',
    label: '关键字',
    type: 'input',
    placeholder: '客户名称/产品名称',
  },
]

// ── 响应式状态 ────────────────────────────────────────────

const searchParams = ref<SalesReportQuery>({})

const activeTab = ref('statistics')
const loading = ref(false)
const hasError = ref(false)
const lastUpdateTime = ref<string>('')

/** 统计数据 */
const summary = ref<ReportSummaryData>({
  monthAmount: 0,
  monthOrders: 0,
  activeCustomers: 0,
  growthRate: 0,
})

/** 缓存趋势数据，用于生成 Sparkline、计算增长率和柱状图 */
const trendDataCache = ref<TrendData[]>([])

/** 明细抽屉状态 */
const detailDrawerVisible = ref(false)
const detailData = ref<DetailDataItem[]>([])
const detailLoading = ref(false)
const detailRange = ref('')

/** 上次更新时间戳（毫秒），用于相对时间计算 */
const lastUpdateTimestamp = ref(0)

/** 比较模式 */
const compareMode = ref<CompareMode>('mom')
/** 显示模式 */
const displayMode = ref<DisplayMode>('absolute')

/** 活跃的卡片筛选键 */
const activeCardFilter = ref<CardFilterKey>(null)

/** 各分区错误状态 */
const sectionErrors = reactive<SectionErrors>({
  summary: false,
  statistics: false,
  customer: false,
  product: false,
  trend: false,
})

/** 导出相关 */
const showExportModal = ref(false)
const exportFormat = ref<ExportFileFormat>('xlsx')
const exportDateRange = ref<any>([])
const exportScope = ref<'current' | 'all'>('current')
const exportIncludeDetail = ref(false)
const exportLoading = ref(false)

/** 自动刷新 */
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

/** 相对时间更新滴答（驱动 computed 重新计算） */
const tick = ref(0)
let relativeTimeTimer: ReturnType<typeof setInterval> | null = null

/** 新建报表 */
const showCreateModal = ref(false)
const newAnalysisName = ref('')

// ── 计算属性 ──────────────────────────────────────────────

const dataFreshness = computed(() => {
  if (loading.value) return 'loading'
  if (hasError.value) return 'error'
  return 'fresh'
})

/** 是否有任一分区错误 */
const hasSectionError = computed(() => {
  return sectionErrors.statistics || sectionErrors.customer || sectionErrors.product || sectionErrors.trend
})

/**
 * 根据比较模式计算当前增长率
 * compareMode === 'yoy' 时使用同比数据，否则使用环比数据
 */
const currentGrowth = computed<GrowthData>(() => {
  if (compareMode.value === 'yoy') {
    return {
      amount: summary.value.yoyAmount ?? null,
      orders: summary.value.yoyOrders ?? null,
      customers: summary.value.yoyCustomers ?? null,
    }
  }
  return {
    amount: summary.value.monthAmountGrowth ?? null,
    orders: summary.value.monthOrdersGrowth ?? null,
    customers: summary.value.activeCustomersGrowth ?? null,
  }
})

/** 对比增长率（第4张卡片显示） */
const comparisonGrowthRate = computed(() => {
  const rate = summary.value.growthRate ?? 0
  return rate
})

/** 相对时间文本 */
const relativeTimeText = computed(() => {
  tick.value // 依赖 tick 以实现定时更新
  if (!lastUpdateTimestamp.value) return ''
  const diffMs = Date.now() - lastUpdateTimestamp.value
  const diffMinutes = Math.floor(diffMs / 60000)
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours}小时前`
  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays}天前`
})

/** 数据延迟警告：超过30分钟未更新 */
const dataDelayWarning = computed(() => {
  if (!lastUpdateTimestamp.value) return false
  const diffMs = Date.now() - lastUpdateTimestamp.value
  return diffMs > 30 * 60 * 1000
})

// ── 数据加载 ──────────────────────────────────────────────

const loadData = async () => {
  hasError.value = false
  sectionErrors.summary = false
  try {
    // 并行加载统计数据和趋势数据
    const [statsRes, trendRes] = await Promise.allSettled([
      salesReportApi.getStatistics(searchParams.value),
      salesReportApi.getSalesTrend(searchParams.value),
    ])

    // 处理统计数据
    if (statsRes.status === 'fulfilled' && statsRes.value?.data) {
      const statsData = statsRes.value.data as any
      summary.value.monthAmount = statsData.totalSales ?? statsData.monthAmount ?? 0
      summary.value.monthOrders = statsData.orderCount ?? statsData.monthOrders ?? 0
    }

    // 处理趋势数据
    if (trendRes.status === 'fulfilled' && trendRes.value?.data) {
      const trendData = trendRes.value.data as TrendData[]
      trendDataCache.value = trendData
      computeGrowthFromTrend(trendData)
      // 提取 sparkline 数据
      summary.value.amountSparkline = trendData.map(d => d.sales)
      summary.value.ordersSparkline = trendData.map(d => d.orders)

      // 计算上期值
      if (trendData.length >= 2) {
        const latest = trendData[trendData.length - 1]
        const prev = trendData[trendData.length - 2]
        summary.value.prevMonthAmount = prev.sales
        summary.value.prevMonthOrders = prev.orders
      }
    } else {
      trendDataCache.value = []
    }
  } catch (err) {
    hasError.value = true
    sectionErrors.summary = true
    console.warn('[销售报表] 加载统计数据失败', err)
    message.error('数据加载失败，请稍后重试')
  }
}

/**
 * 从趋势数据计算环比和同比增长率
 */
function computeGrowthFromTrend(trendData: TrendData[]) {
  if (trendData.length < 2) return

  const latest = trendData[trendData.length - 1]
  const prev = trendData[trendData.length - 2]

  // 环比增长
  if (prev.sales > 0) {
    summary.value.monthAmountGrowth = Number(((latest.sales - prev.sales) / prev.sales * 100).toFixed(1))
  }
  if (prev.orders > 0) {
    summary.value.monthOrdersGrowth = Number(((latest.orders - prev.orders) / prev.orders * 100).toFixed(1))
  }

  // 同比增长（取前第13个数据点作为去年同期，如果数据足够多）
  if (trendData.length >= 13) {
    const yoyPrev = trendData[trendData.length - 13]
    if (yoyPrev.sales > 0) {
      summary.value.yoyAmount = Number(((latest.sales - yoyPrev.sales) / yoyPrev.sales * 100).toFixed(1))
    }
    if (yoyPrev.orders > 0) {
      summary.value.yoyOrders = Number(((latest.orders - yoyPrev.orders) / yoyPrev.orders * 100).toFixed(1))
    }
  }
}

// ── 子组件引用 ────────────────────────────────────────────

const statisticsRef = ref<SubComponentRef>()
const customerRef = ref<SubComponentRef>()
const productRef = ref<SubComponentRef>()
const trendRef = ref<SubComponentRef>()

const refreshSubComponents = (extraParams?: Record<string, unknown>) => {
  const mergedParams = { ...searchParams.value, ...(extraParams || {}) }
  const refMap: Record<string, SubComponentRef | undefined> = {
    statistics: statisticsRef.value,
    customer: customerRef.value,
    product: productRef.value,
    trend: trendRef.value,
  }
  Object.entries(refMap).forEach(([key, ref]) => {
    if (ref?.handleQuery) {
      ref.handleQuery(mergedParams).catch(() => {
        sectionErrors[key] = true
      })
    }
  })
}

// ── 搜索处理 ──────────────────────────────────────────────

const handleSearch = (formData: SalesReportQuery) => {
  searchParams.value = formData
  loadData()
  // 同时刷新所有子组件，携带卡片筛选信息
  refreshSubComponents(activeCardFilter.value ? { cardFilter: activeCardFilter.value } : undefined)
}

const handleResetFilter = () => {
  searchParams.value = {}
  activeCardFilter.value = null
  loadData()
  refreshSubComponents()
}

// ── 刷新 ──────────────────────────────────────────────────

const handleRefresh = async () => {
  loading.value = true
  const now = Date.now()
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  lastUpdateTimestamp.value = now
  // 先刷新父组件数据
  await loadData()
  // 刷新当前 Tab 的数据
  const refMap: Record<string, SubComponentRef | undefined> = {
    statistics: statisticsRef.value,
    customer: customerRef.value,
    product: productRef.value,
    trend: trendRef.value,
  }
  const currentRef = refMap[activeTab.value]
  if (currentRef?.handleQuery) {
    try {
      await currentRef.handleQuery(searchParams.value)
    } catch {
      sectionErrors[activeTab.value] = true
    }
  }
  loading.value = false
}

watch(activeTab, () => {
  lastUpdateTime.value = ''
})

// ── 交互式筛选（卡片点击）────────────────────────────────

const handleCardClick = (key: CardFilterKey) => {
  if (activeCardFilter.value === key) {
    // 点击已激活的卡片取消筛选
    activeCardFilter.value = null
  } else {
    activeCardFilter.value = key
  }
  // 通知子组件筛选状态变化
  refreshSubComponents(activeCardFilter.value ? { cardFilter: activeCardFilter.value } : undefined)
  // 打开明细抽屉
  handleOpenDetail(key || 'amount')
}

const handleClearFilter = () => {
  activeCardFilter.value = null
  refreshSubComponents()
}

/** 打开明细抽屉 */
function handleOpenDetail(cardKey: string) {
  if (trendDataCache.value.length === 0) return
  detailLoading.value = true
  detailDrawerVisible.value = true

  // 从趋势数据构造明细列表
  const items: DetailDataItem[] = []
  for (let i = 0; i < trendDataCache.value.length; i++) {
    const curr = trendDataCache.value[i]
    const prev = i > 0 ? trendDataCache.value[i - 1] : undefined
    let amount = 0
    let quantity = 0
    let growthRate: number | null = null

    switch (cardKey) {
      case 'amount':
        amount = curr.sales
        quantity = curr.orders
        if (prev && prev.sales > 0) {
          growthRate = Number(((curr.sales - prev.sales) / prev.sales * 100).toFixed(1))
        }
        break
      case 'orders':
        amount = curr.orders
        quantity = curr.sales
        if (prev && prev.orders > 0) {
          growthRate = Number(((curr.orders - prev.orders) / prev.orders * 100).toFixed(1))
        }
        break
      case 'customers':
        amount = curr.sales
        quantity = curr.orders
        growthRate = null
        break
      case 'growth':
        amount = curr.sales
        quantity = curr.orders
        if (prev && prev.sales > 0) {
          growthRate = Number(((curr.sales - prev.sales) / prev.sales * 100).toFixed(1))
        }
        break
      default:
        amount = curr.sales
        quantity = curr.orders
        break
    }

    items.push({
      period: formatMonthLabel(curr.date),
      amount,
      quantity,
      growthRate,
    })
  }
  detailData.value = items.reverse()
  detailRange.value = `${items[items.length - 1]?.period || ''} ~ ${items[0]?.period || ''}`
  detailLoading.value = false
}

// ── 对比模式切换 ──────────────────────────────────────────

const handleCompareModeChange = () => {
  // 对比模式变化时刷新子组件
  refreshSubComponents({
    cardFilter: activeCardFilter.value,
    compareMode: compareMode.value,
  })
}

const handleDisplayModeChange = () => {
  refreshSubComponents({
    cardFilter: activeCardFilter.value,
    displayMode: displayMode.value,
  })
}

// ── 分区重试 ──────────────────────────────────────────────

const handleRetrySection = (sectionKey: string) => {
  sectionErrors[sectionKey] = false
  const refMap: Record<string, SubComponentRef | undefined> = {
    statistics: statisticsRef.value,
    customer: customerRef.value,
    product: productRef.value,
    trend: trendRef.value,
  }
  const ref = refMap[sectionKey]
  if (ref?.handleQuery) {
    ref.handleQuery(searchParams.value).catch(() => {
      sectionErrors[sectionKey] = true
    })
  }
}

// ── 导出功能 ──────────────────────────────────────────────

const handleOpenExportModal = () => {
  // 预填搜索日期范围
  const dr = searchParams.value.dateRange
  exportDateRange.value = Array.isArray(dr) ? [...dr] : []
  showExportModal.value = true
}

const handleExportConfirm = async () => {
  exportLoading.value = true
  try {
    const options = {
      format: exportFormat.value,
      scope: exportScope.value,
      dateRange: exportDateRange.value.length === 2
        ? [exportDateRange.value[0], exportDateRange.value[1]] as [string, string]
        : undefined,
      includeDetail: exportIncludeDetail.value,
    }
    await performExport(options)
    showExportModal.value = false
    message.success('报表导出成功')
  } catch (err: unknown) {
    console.warn('[销售报表] 导出失败', err)
    message.error(err instanceof Error ? err.message : '导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/** 实际执行导出 */
async function performExport(options: {
  format: ExportFileFormat
  scope: 'current' | 'all'
  dateRange?: [string, string]
  includeDetail: boolean
}): Promise<void> {
  const exportData = buildExportData()
  const dateStr = new Date().toISOString().slice(0, 10)
  const filenameBase = `销售报表_${dateStr}`

  switch (options.format) {
    case 'csv':
      // CSV: 客户端生成
      exportAsCsv(exportData, filenameBase)
      break
    case 'xlsx':
    case 'pdf': {
      // XLSX 和 PDF: 调用服务端 API
      const formatExt = options.format === 'xlsx' ? 'xlsx' : 'pdf'
      await exportViaApi(options.format, `${filenameBase}.${formatExt}`)
      break
    }
    default:
      throw new Error('不支持的导出格式')
  }
}

/** 通过 API 导出（用于 XLSX 和 PDF 格式） */
async function exportViaApi(format: ExportFileFormat, filename: string): Promise<void> {
  const blob = await salesReportApi.exportReport({
    format,
    scope: exportScope.value,
    dateRange: exportDateRange.value.length === 2
      ? [exportDateRange.value[0], exportDateRange.value[1]] as [string, string]
      : undefined,
    includeDetail: exportIncludeDetail.value,
  })
  const contentType = format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  const blobWithType = new Blob([blob], { type: contentType })
  downloadBlob(blobWithType, filename)
}

/** 构建导出数据 */
function buildExportData() {
  return {
    metadata: {
      exportTime: new Date().toLocaleString('zh-CN'),
      dateRange: searchParams.value.dateRange || [],
      keyword: searchParams.value.keyword || '',
      compareMode: compareMode.value,
      displayMode: displayMode.value,
    },
    summary: {
      本月销售额: summary.value.monthAmount,
      本月订单数: summary.value.monthOrders,
      活跃客户数: summary.value.activeCustomers,
      环比增长: `${summary.value.growthRate}%`,
      销售额环比增长: summary.value.monthAmountGrowth != null ? `${summary.value.monthAmountGrowth}%` : '-',
      订单数环比增长: summary.value.monthOrdersGrowth != null ? `${summary.value.monthOrdersGrowth}%` : '-',
    },
    growth: {
      yoy: {
        amount: summary.value.yoyAmount,
        orders: summary.value.yoyOrders,
      },
      mom: {
        amount: summary.value.monthAmountGrowth,
        orders: summary.value.monthOrdersGrowth,
      },
    },
  }
}

/** 导出为 CSV */
function exportAsCsv(data: ReturnType<typeof buildExportData>, filename: string) {
  const headers = ['指标', '当前值', '上期值', '环比增长', '同比增长']
  const rows = [
    ['本月销售额', String(data.summary['本月销售额']), String(summary.value.prevMonthAmount ?? '-'), data.summary['销售额环比增长'], data.growth.yoy?.amount != null ? `${data.growth.yoy.amount}%` : '-'],
    ['本月订单数', String(data.summary['本月订单数']), String(summary.value.prevMonthOrders ?? '-'), data.summary['订单数环比增长'], data.growth.yoy?.orders != null ? `${data.growth.yoy.orders}%` : '-'],
    ['活跃客户数', String(data.summary['活跃客户数']), String(summary.value.prevActiveCustomers ?? '-'), '-', '-'],
    ['环比增长', data.summary['环比增长'], '-', '-', '-'],
  ]
  const csvContent = [
    `# 导出时间: ${data.metadata.exportTime}`,
    `# 日期范围: ${data.metadata.dateRange.join(' ~ ') || '全部'}`,
    '',
    headers.join(','),
    ...rows.map(r => r.map(v => `"${v}"`).join(',')),
  ].join('\n')

  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
  downloadBlob(blob, `${filename}.csv`)
}

/** 导出为 Excel（使用 HTML 表格格式，兼容 Excel 打开） */
function exportAsExcel(data: ReturnType<typeof buildExportData>, filename: string) {
  const rows = [
    ['指标', '当前值', '上期值', '环比增长', '同比增长'],
    ['本月销售额', String(data.summary['本月销售额']), String(summary.value.prevMonthAmount ?? '-'), data.summary['销售额环比增长'], data.growth.yoy?.amount != null ? `${data.growth.yoy.amount}%` : '-'],
    ['本月订单数', String(data.summary['本月订单数']), String(summary.value.prevMonthOrders ?? '-'), data.summary['订单数环比增长'], data.growth.yoy?.orders != null ? `${data.growth.yoy.orders}%` : '-'],
    ['活跃客户数', String(data.summary['活跃客户数']), String(summary.value.prevActiveCustomers ?? '-'), '-', '-'],
    ['环比增长', data.summary['环比增长'], '-', '-', '-'],
  ]
  const html = [
    '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">',
    '<head><meta charset="UTF-8"><style>td{text-align:right;padding:4px 8px}td:first-child{text-align:left;font-weight:bold}th{text-align:center;padding:4px 8px;background:#f0f0f0}</style></head>',
    '<body><table>',
    `<tr><td colspan="5">导出时间: ${data.metadata.exportTime}</td></tr>`,
    `<tr><td colspan="5">日期范围: ${data.metadata.dateRange.join(' ~ ') || '全部'}</td></tr>`,
    '<tr></tr>',
    `<tr>${rows[0].map(h => `<th>${h}</th>`).join('')}</tr>`,
    ...rows.slice(1).map(r => `<tr>${r.map(v => `<td>${v}</td>`).join('')}</tr>`),
    '</table></body></html>',
  ].join('\n')

  const blob = new Blob(['\ufeff' + html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  downloadBlob(blob, `${filename}.xls`)
}

/** 通用 Blob 下载 */
function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

defineExpose({ handleQuery: loadData })

// ── 新建报表 ──────────────────────────────────────────────

const handleCreate = () => {
  showCreateModal.value = true
  newAnalysisName.value = ''
}

const confirmCreate = () => {
  const name = newAnalysisName.value.trim()
  if (!name) {
    message.warning('请输入报表名称')
    return
  }
  showCreateModal.value = false
  message.success(`已创建报表「${name}」`)
  handleResetFilter()
}

// ── 自动刷新控制 ──────────────────────────────────────────

const startAutoRefresh = () => {
  stopAutoRefresh()
  if (!autoRefreshEnabled.value) return
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadData()
    autoRefreshCountdown.value = 30
  }, AUTO_REFRESH_INTERVAL)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

watch(autoRefreshEnabled, startAutoRefresh)

// ── 生命周期 ──────────────────────────────────────────────

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  loadData()
  const now = Date.now()
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  lastUpdateTimestamp.value = now
  startAutoRefresh()
  // 每30秒更新相对时间
  relativeTimeTimer = setInterval(() => {
    tick.value++
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  stopAutoRefresh()
  if (relativeTimeTimer) {
    clearInterval(relativeTimeTimer)
    relativeTimeTimer = null
  }
})
</script>

<style scoped>
/* ── 错误条 ──────────────────────────────────────── */
.error-banner {
  padding: 0 0 12px 0;
}

.section-errors {
  padding: 0 0 8px 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ── 比较工具栏 ────────────────────────────────── */
.compare-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 0 0;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-label {
  font-size: 13px;
  color: #606266;
}

.active-filter-tip {
  display: flex;
  align-items: center;
}

/* ── 报表内容区域 ──────────────────────────────── */
.report-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 数据状态 ──────────────────────────────────── */
.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

/* ── 统计卡片动画 ────────────────────────────── */
@keyframes slideUpFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ── 统计卡片样式 ────────────────────────────── */
.summary-card {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  animation: slideUpFadeIn 0.4s ease-out both;
}

.summary-card:nth-child(1) { animation-delay: 0s; }
.summary-card:nth-child(2) { animation-delay: 0.08s; }
.summary-card:nth-child(3) { animation-delay: 0.16s; }
.summary-card:nth-child(4) { animation-delay: 0.24s; }

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.clickable {
  cursor: pointer;
}

.summary-card.clickable:active {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.summary-card.active {
  border: 2px solid #1890ff;
  box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.15);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}

.summary-card.highlight.active {
  border-color: #faad14;
  box-shadow: 0 0 0 3px rgba(250, 173, 20, 0.2);
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
  flex-shrink: 0;
}

.summary-content {
  flex: 1;
  min-width: 0;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  transition: color 0.3s;
}

.summary-prev {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  line-height: 1.4;
}

.summary-prev-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.summary-change {
  font-size: 12px;
  margin-top: 4px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.summary-change.positive {
  color: #52c41a;
}

.summary-change.negative {
  color: #f5222d;
}

/* ── 迷你趋势图 ──────────────────────────────── */
.sparkline {
  margin-top: 6px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.summary-card:hover .sparkline {
  opacity: 1;
}

/* ── Tabs ──────────────────────────────────────── */
:deep(.ant-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.ant-tabs-content) {
  flex: 1;
  overflow: auto;
}

:deep(.ant-tabs-tabpane) {
  height: 100%;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 相对时间和延迟警告 ──────────────────────── */
.relative-time {
  color: #909399;
  font-size: 11px;
}

.delay-warning {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #faad14;
  padding: 2px 8px;
  border-radius: 4px;
  background: #fff7e6;
  animation: pulse-warning 2s ease-in-out infinite;
}

@keyframes pulse-warning {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* ── 月度趋势柱状图 ──────────────────────────── */
.trend-barchart {
  margin: 8px 0 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.barchart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.barchart-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.barchart-hint {
  font-size: 12px;
  color: #909399;
}

.barchart-container {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 160px;
  padding: 0 8px;
}

.barchart-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.barchart-bar {
  width: 100%;
  max-width: 48px;
  min-height: 24px;
  background: linear-gradient(180deg, #1890ff 0%, #69c0ff 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
  position: relative;
  cursor: pointer;
}

.barchart-bar:hover {
  opacity: 0.85;
}

.barchart-value {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: #606266;
  white-space: nowrap;
  display: none;
}

.barchart-bar-wrapper:hover .barchart-value {
  display: block;
}

.barchart-label {
  font-size: 11px;
  color: #909399;
  margin-top: 6px;
  white-space: nowrap;
}

/* ── 明细抽屉 ────────────────────────────────── */
.drawer-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

/* ── 文本颜色工具 ────────────────────────────── */
.text-positive {
  color: #52c41a;
  font-weight: 500;
}

.text-negative {
  color: #f5222d;
  font-weight: 500;
}
</style>
