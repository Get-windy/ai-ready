<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏左侧：时间范围快捷段（今日/昨日/近7日/近30日/本月/自定义） ═══ -->
        <template #toolbar-left>
          <div
            v-if="fieldVisible('range')"
            class="range-switch"
          >
            <a-button
              v-for="r in RANGE_OPTIONS"
              :key="r.key"
              :type="query.range === r.key ? 'primary' : 'link'"
              size="small"
              @click="setRange(r.key)"
            >
              {{ r.label }}
            </a-button>
            <a-range-picker
              v-if="query.range === 'custom'"
              v-model:value="customRange"
              size="small"
              style="width: 220px"
              :placeholder="['开始日期', '结束日期']"
              @change="handleCustomRange"
            />
          </div>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 图表 / 刷新 / 自动刷新 / 导出 / 全屏 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              title="页面配置"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip
              v-if="btnEnabled('charts')"
              :title="showCharts ? '隐藏图表' : '显示图表'"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                :type="showCharts ? 'primary' : 'default'"
                size="small"
                @click="toggleCharts"
              >
                <BarChartOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="loadAll()"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-tooltip
              v-if="btnEnabled('autoRefresh')"
              :title="autoRefresh ? `自动刷新中（每 ${refreshSeconds}s），点击停止` : '开启自动刷新'"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                :type="autoRefresh ? 'primary' : 'default'"
                size="small"
                @click="toggleAutoRefresh"
              >
                <SyncOutlined :spin="autoRefresh && loading" />
                <span v-if="autoRefresh">{{ countdown }}s</span>
                <span v-else>自动</span>
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
            <a-tooltip
              v-if="btnEnabled('fullscreen')"
              :title="isFullscreen ? '退出全屏' : '全屏'"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                size="small"
                @click="toggleFullscreen"
              >
                <FullscreenExitOutlined v-if="isFullscreen" />
                <FullscreenOutlined v-else />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 查询条件：运力渠道 / 订单类型（按页面配置显隐） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="fieldVisible('channelId')"
                class="search-item"
              >
                <span class="search-label">运力渠道</span>
                <a-select
                  v-model:value="query.channelId"
                  size="small"
                  style="width: 180px"
                  placeholder="全部渠道"
                  allow-clear
                  :options="channelOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('orderType')"
                class="search-item"
              >
                <span class="search-label">订单类型</span>
                <a-select
                  v-model:value="query.orderType"
                  size="small"
                  style="width: 140px"
                  placeholder="全部类型"
                  allow-clear
                  :options="ORDER_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                :loading="loading"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button
                size="small"
                @click="handleReset"
              >
                重置
              </a-button>
              <a-checkbox
                :checked="showCharts"
                @change="(e: any) => (showCharts = e.target.checked)"
              >
                显示图表
              </a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 看板主体 ═══ -->
        <template #table>
          <div class="dashboard-body">
            <!-- 口径回显条 -->
            <div class="stats-meta">
              <span class="meta-item">
                <span class="meta-label">统计口径</span>
                <span class="meta-value">{{ stats.rangeLabel || '-' }}</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">区间</span>
                <span class="meta-value">{{ stats.startTime || '-' }} ~ {{ stats.endTime || '-' }}</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">在线判定</span>
                <span class="meta-value">{{ stats.onlineThresholdMinutes }} 分钟内上报</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">更新时间</span>
                <span class="meta-value">{{ lastUpdateTime || '-' }}</span>
              </span>
              <span class="meta-item meta-tip">
                单量按任务创建时间统计；准时率 = 准时签收量 / 有截止时间的已签收量；点击卡片下钻明细
              </span>
            </div>

            <!-- KPI 卡片（可下钻） -->
            <ARStatCards
              :items="statCards"
              :loading="loading && !loaded"
              clickable
              @card-click="handleCardDrill"
            />

            <!-- 图表区：单量时效趋势 + 任务状态分布 + 渠道分布 -->
            <a-row
              v-if="showCharts"
              :gutter="12"
              class="chart-row"
            >
              <a-col :span="12">
                <ARReportChart
                  title="单量时效趋势"
                  :option="trendChartOption"
                  :loading="loading && !loaded"
                  :height="248"
                  empty-text="暂无趋势数据"
                />
              </a-col>
              <a-col :span="6">
                <ARReportChart
                  title="任务状态分布"
                  :option="statusChartOption"
                  :loading="loading && !loaded"
                  :height="248"
                  empty-text="暂无任务数据"
                  @point-click="handleStatusChartClick"
                />
              </a-col>
              <a-col :span="6">
                <ARReportChart
                  title="渠道分布"
                  :option="channelChartOption"
                  :loading="loading && !loaded"
                  :height="248"
                  empty-text="暂无渠道数据"
                />
              </a-col>
            </a-row>

            <!-- 待办区：活跃人车绑定 + 待处理预警 -->
            <a-row :gutter="12">
              <a-col :span="12">
                <div class="section-card">
                  <div class="section-card__header">
                    <h3>活跃人车绑定</h3>
                    <a-space :size="8">
                      <span class="section-count">共 {{ bindingTotal }} 条</span>
                      <a-button
                        type="link"
                        size="small"
                        @click="loadBindings"
                      >
                        刷新
                      </a-button>
                    </a-space>
                  </div>
                  <a-table
                    :data-source="activeBindings"
                    :columns="bindingColumns"
                    :loading="bindingsLoading"
                    row-key="id"
                    size="small"
                    bordered
                    :pagination="false"
                    :locale="{ emptyText: '暂无活跃绑定' }"
                    :scroll="{ y: 196 }"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'status'">
                        <a-tag :color="bindingStatusMeta(record.status).color">
                          {{ bindingStatusMeta(record.status).text }}
                        </a-tag>
                      </template>
                      <template v-if="column.dataIndex === 'bindTime'">
                        {{ formatDateTime(record.bindTime) }}
                      </template>
                    </template>
                  </a-table>
                </div>
              </a-col>

              <a-col :span="12">
                <div class="section-card">
                  <div class="section-card__header">
                    <h3>待处理预警</h3>
                    <a-space :size="8">
                      <span class="section-count">共 {{ alertTotal }} 条</span>
                      <a-button
                        type="link"
                        size="small"
                        @click="loadAlerts"
                      >
                        刷新
                      </a-button>
                    </a-space>
                  </div>
                  <a-table
                    :data-source="pendingAlerts"
                    :columns="alertColumns"
                    :loading="alertsLoading"
                    row-key="id"
                    size="small"
                    bordered
                    :pagination="false"
                    :locale="{ emptyText: '暂无待处理预警' }"
                    :scroll="{ y: 196 }"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'alertType'">
                        <a-tag :color="alertTypeMeta(record.alertType).color">
                          {{ alertTypeMeta(record.alertType).text }}
                        </a-tag>
                      </template>
                      <template v-if="column.dataIndex === 'alertLevel'">
                        <a-tag :color="alertLevelMeta(record.alertLevel).color">
                          {{ alertLevelMeta(record.alertLevel).text }}
                        </a-tag>
                      </template>
                      <template v-if="column.dataIndex === 'createTime'">
                        {{ formatDateTime(record.createTime) }}
                      </template>
                    </template>
                  </a-table>
                </div>
              </a-col>
            </a-row>

            <!-- 配送员绩效 Top -->
            <div class="section-card">
              <div class="section-card__header">
                <h3>配送员绩效 Top</h3>
                <span class="section-count">按区间接单量排序</span>
              </div>
              <a-table
                :data-source="topRiders"
                :columns="topRiderColumns"
                :loading="topRidersLoading"
                row-key="riderId"
                size="small"
                bordered
                :pagination="false"
                :locale="{ emptyText: '暂无配送员绩效数据' }"
              >
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.dataIndex === 'rank'">
                    <a-tag :color="index < 3 ? 'gold' : 'default'">
                      {{ index + 1 }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'onTimeRate'">
                    {{ record.onTimeRate === null ? '-' : record.onTimeRate + '%' }}
                  </template>
                  <template v-if="column.dataIndex === 'avgMinutes'">
                    {{ record.avgMinutes === null ? '-' : record.avgMinutes + ' 分钟' }}
                  </template>
                  <template v-if="column.dataIndex === 'ratingScore'">
                    {{ record.ratingScore === null ? '-' : record.ratingScore }}
                  </template>
                </template>
              </a-table>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :function-buttons-config="functionButtonsConfig"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      storage-key="dms-dashboard-page-config"
      :hide-print-config="true"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 配送仪表盘（配送 → 配送业务 → 配送仪表盘，菜单 80820）
 *
 * 运营看板：运力规模 / 单量结构 / 时效质量 / 金额 / 待办人车与预警 / 配送员绩效。
 * · 全部指标来自后端 `GET /api/dms/dashboard/*` 聚合（口径固化见《配送仪表盘开发文档》§3.2），
 *   页面不做二次汇总、不使用占位符；
 * · 时间范围（今日/昨日/近 7 日/近 30 日/本月/自定义）与渠道、订单类型筛选对全部接口生效；
 * · 自动刷新间隔取自后端配送参数 `dashboard.refresh.seconds`，离开页面即停轮询；
 * · 卡片下钻到对应台账页（配送查询 / 调度任务 / 结算 / 收款 / 配送员 / 车辆 / 实名认证）。
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import * as XLSX from 'xlsx'
import {
  ReloadOutlined, SyncOutlined, SettingOutlined, ExportOutlined,
  BarChartOutlined, FullscreenOutlined, FullscreenExitOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import {
  dashboardApi,
  type DmsDashboardStats, type DmsTrendPoint, type DmsTaskSummaryItem,
  type DmsDistributionItem, type DmsTopRider, type DmsActiveBinding, type DmsPendingAlert,
} from '@/api/dms/dashboard'
import { channelApi } from '@/api/dms/channel'
import {
  DMS_BINDING_STATUS, DMS_ALERT_TYPE, DMS_ALERT_LEVEL, formatDateTime,
} from '@/views/dms/shared'

defineOptions({ name: 'DmsDashboard' })

const router = useRouter()

function handleError(e: Error) {
  console.error('[配送仪表盘] 页面错误', e)
}

// ═══════════════════════════════════════════════
// 时间范围
// ═══════════════════════════════════════════════
const RANGE_OPTIONS = [
  { key: 'today', label: '今日' },
  { key: 'yesterday', label: '昨日' },
  { key: 'last7', label: '近 7 日' },
  { key: 'last30', label: '近 30 日' },
  { key: 'month', label: '本月' },
  { key: 'custom', label: '自定义' },
]

const ORDER_TYPE_OPTIONS = [
  { label: '销售配送', value: 1 },
  { label: '调拨', value: 2 },
  { label: '退货', value: 3 },
]

const query = reactive<{
  range: string
  startDate?: string
  endDate?: string
  /** 渠道 ID 保持字符串：雪花 ID 超出 JS 安全整数，转 number 会精度丢失 */
  channelId?: string | null
  orderType?: number | null
}>({
  range: 'today',
  channelId: null,
  orderType: null,
})

const customRange = ref<[Dayjs, Dayjs] | null>(null)

/** 当前区间对应的自然日（用于下钻到台账页的日期条件） */
function rangeDatePair(): [string, string] {
  const today = dayjs()
  switch (query.range) {
    case 'yesterday': {
      const d = today.subtract(1, 'day')
      return [d.format('YYYY-MM-DD'), d.format('YYYY-MM-DD')]
    }
    case 'last7':
      return [today.subtract(6, 'day').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
    case 'last30':
      return [today.subtract(29, 'day').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
    case 'month':
      return [today.startOf('month').format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
    case 'custom':
      return query.startDate && query.endDate
        ? [query.startDate, query.endDate]
        : [today.format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
    default:
      return [today.format('YYYY-MM-DD'), today.format('YYYY-MM-DD')]
  }
}

function setRange(key: string) {
  query.range = key
  if (key === 'custom' && !customRange.value) {
    // 默认展示近 7 日，便于直接确认
    customRange.value = [dayjs().subtract(6, 'day'), dayjs()]
    query.startDate = customRange.value[0].format('YYYY-MM-DD')
    query.endDate = customRange.value[1].format('YYYY-MM-DD')
  } else if (key !== 'custom') {
    query.startDate = undefined
    query.endDate = undefined
  }
  loadAll()
}

function handleCustomRange(dates: [Dayjs, Dayjs] | null) {
  if (dates?.length === 2) {
    query.startDate = dates[0].format('YYYY-MM-DD')
    query.endDate = dates[1].format('YYYY-MM-DD')
    loadAll()
  }
}

function handleSearch() {
  loadAll()
}

function handleReset() {
  query.range = 'today'
  query.startDate = undefined
  query.endDate = undefined
  query.channelId = null
  query.orderType = null
  customRange.value = null
  loadAll()
}

// ═══════════════════════════════════════════════
// 页面配置（查询条件 + 功能按钮）
// ═══════════════════════════════════════════════
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'range', label: '时间范围', visible: true },
  { key: 'channelId', label: '运力渠道', visible: true },
  { key: 'orderType', label: '订单类型', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'charts', label: '图表显示切换', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'autoRefresh', label: '自动刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'fullscreen', label: '全屏', enabled: true },
]

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig {
  queryFields?: QueryFieldSetting[]
  functionButtons?: FunctionButtonSetting[]
  printConfig?: Record<string, boolean>
}

const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

function handlePageConfigChange(config: PageConfig) {
  if (config.queryFields) queryFieldsConfig.value = config.queryFields
  if (config.functionButtons) functionButtonsConfig.value = config.functionButtons
}

// ═══════════════════════════════════════════════
// 数据状态
// ═══════════════════════════════════════════════
const loading = ref(false)
const loaded = ref(false)
const lastUpdateTime = ref('')

const stats = reactive<DmsDashboardStats>({
  totalRiders: 0, activeRiders: 0, onlineRiders: 0,
  totalVehicles: 0, activeVehicles: 0,
  orderCount: 0, pendingOrders: 0, assignedOrders: 0, inTransitOrders: 0,
  completedOrders: 0, cancelledOrders: 0, exceptionOrders: 0,
  onTimeRate: null, avgDeliveryMinutes: null,
  deliveryFee: 0, collectOnDelivery: 0, goodsAmount: 0,
  activeBindingCount: 0, pendingAlertCount: 0,
  range: 'today', rangeLabel: '', startTime: '', endTime: '',
  onlineThresholdMinutes: 2, refreshSeconds: 30,
})

const trend = ref<DmsTrendPoint[]>([])
const taskSummary = ref<DmsTaskSummaryItem[]>([])
const channelDistribution = ref<DmsDistributionItem[]>([])
const topRiders = ref<DmsTopRider[]>([])

const activeBindings = ref<DmsActiveBinding[]>([])
const bindingTotal = ref(0)
const bindingsLoading = ref(false)

const pendingAlerts = ref<DmsPendingAlert[]>([])
const alertTotal = ref(0)
const alertsLoading = ref(false)

const topRidersLoading = ref(false)
const channelOptions = ref<{ label: string; value: string }[]>([])

const showCharts = ref(true)

function toggleCharts() {
  showCharts.value = !showCharts.value
}

// ═══════════════════════════════════════════════
// 请求
// ═══════════════════════════════════════════════
function buildQuery() {
  return {
    range: query.range,
    startDate: query.range === 'custom' ? query.startDate : undefined,
    endDate: query.range === 'custom' ? query.endDate : undefined,
    channelId: query.channelId ?? undefined,
    orderType: query.orderType ?? undefined,
  }
}

async function loadStats() {
  const data = await dashboardApi.stats(buildQuery())
  if (data) Object.assign(stats, data)
}

async function loadTrend() {
  trend.value = await dashboardApi.trend(buildQuery())
}

async function loadTaskSummary() {
  taskSummary.value = await dashboardApi.taskSummary(buildQuery())
}

async function loadChannelDistribution() {
  channelDistribution.value = await dashboardApi.distribution({ ...buildQuery(), by: 'channel' })
}

async function loadTopRiders() {
  topRidersLoading.value = true
  try {
    topRiders.value = await dashboardApi.topRiders({ ...buildQuery(), limit: 10 })
  } catch (e) {
    topRiders.value = []
    console.warn('[配送仪表盘] 配送员绩效加载失败', e)
  } finally {
    topRidersLoading.value = false
  }
}

async function loadBindings() {
  bindingsLoading.value = true
  try {
    const page = await dashboardApi.activeBindings({ page: 1, size: 5 })
    activeBindings.value = page?.records || []
    bindingTotal.value = Number(page?.total) || 0
  } catch (e) {
    activeBindings.value = []
    bindingTotal.value = 0
    console.warn('[配送仪表盘] 活跃绑定加载失败', e)
  } finally {
    bindingsLoading.value = false
  }
}

async function loadAlerts() {
  alertsLoading.value = true
  try {
    const page = await dashboardApi.pendingAlerts({ page: 1, size: 5 })
    pendingAlerts.value = page?.records || []
    alertTotal.value = Number(page?.total) || 0
  } catch (e) {
    pendingAlerts.value = []
    alertTotal.value = 0
    console.warn('[配送仪表盘] 待处理预警加载失败', e)
  } finally {
    alertsLoading.value = false
  }
}

async function loadChannels() {
  try {
    const page: any = await channelApi.page({ current: 1, size: 100 })
    const records = page?.records || page?.data?.records || []
    channelOptions.value = records.map((c: any) => ({
      label: c.channelName,
      value: String(c.id),
    }))
  } catch (e) {
    channelOptions.value = []
    console.warn('[配送仪表盘] 渠道下拉加载失败', e)
  }
}

/** 首屏与手动刷新：全量并发加载 */
async function loadAll() {
  loading.value = true
  try {
    await Promise.all([
      loadStats(), loadTrend(), loadTaskSummary(),
      loadChannelDistribution(), loadTopRiders(), loadBindings(), loadAlerts(),
    ])
    lastUpdateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
    loaded.value = true
  } catch (e) {
    message.error('看板数据加载失败，请稍后重试')
    console.warn('[配送仪表盘] 加载失败', e)
  } finally {
    loading.value = false
  }
}

/** 自动刷新：静默刷新（不打断图表与表格交互） */
async function silentRefresh() {
  if (loading.value) return
  try {
    await Promise.all([
      loadStats(), loadTrend(), loadTaskSummary(),
      loadChannelDistribution(), loadTopRiders(), loadBindings(), loadAlerts(),
    ])
    lastUpdateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  } catch (e) {
    console.warn('[配送仪表盘] 静默刷新失败', e)
  }
}

// ═══════════════════════════════════════════════
// 自动刷新（间隔取自后端配送参数，离开页面即停）
// ═══════════════════════════════════════════════
const autoRefresh = ref(true)
const countdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const refreshSeconds = computed(() => {
  const s = Number(stats.refreshSeconds)
  return Number.isFinite(s) && s >= 5 ? s : 30
})

function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

function startAutoRefresh() {
  stopAutoRefresh()
  const span = refreshSeconds.value
  countdown.value = span
  refreshTimer = setInterval(() => {
    silentRefresh()
    countdown.value = span
  }, span * 1000)
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) countdown.value -= 1
  }, 1000)
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    startAutoRefresh()
    message.success(`已开启自动刷新（每 ${refreshSeconds.value} 秒）`)
  } else {
    stopAutoRefresh()
    message.info('已停止自动刷新')
  }
}

// ═══════════════════════════════════════════════
// KPI 卡片 + 下钻
// ═══════════════════════════════════════════════
const statCards = computed<StatCardItem[]>(() => {
  const s = stats
  return [
    { label: '配送单量', value: s.orderCount, clickable: true },
    { label: '待分配任务', value: s.pendingOrders, clickable: true },
    { label: '在途任务', value: s.inTransitOrders, clickable: true },
    { label: '已完成', value: s.completedOrders, clickable: true },
    { label: '异常任务', value: s.exceptionOrders, clickable: true },
    { label: '超时未签收', value: s.overdueOrders, clickable: true },
    { label: '准时率', value: s.onTimeRate === null ? '-' : s.onTimeRate, suffix: s.onTimeRate === null ? '' : '%', clickable: false },
    { label: '平均配送时长', value: s.avgDeliveryMinutes === null ? '-' : s.avgDeliveryMinutes, suffix: s.avgDeliveryMinutes === null ? '' : '分钟', clickable: false },
    { label: '配送费', value: s.deliveryFee, prefix: '¥', precision: 2, clickable: true },
    { label: '代收货款', value: s.collectOnDelivery, prefix: '¥', precision: 2, clickable: true },
    { label: '在线配送员', value: `${s.onlineRiders}/${s.totalRiders}`, clickable: true },
    { label: '活跃车辆', value: `${s.activeVehicles}/${s.totalVehicles}`, clickable: true },
    { label: '活跃人车绑定', value: s.activeBindingCount, clickable: true },
    { label: '待处理预警', value: s.pendingAlertCount, clickable: true },
  ]
})

/** 卡片下钻目标（按卡片标题映射到对应台账页，并携带筛选条件） */
function resolveDrill(label: string): { path: string; query?: Record<string, any> } | null {
  const [start, end] = rangeDatePair()
  const dateQuery = { createTimeStart: start, createTimeEnd: end }
  switch (label) {
    case '配送单量':
      return { path: '/dispatch/query', query: dateQuery }
    case '待分配任务':
      return { path: '/dms/dispatch-task', query: { status: '0' } }
    case '在途任务':
      return { path: '/dispatch/query', query: { ...dateQuery, status: 'DELIVERING' } }
    case '已完成':
      return { path: '/dispatch/query', query: { ...dateQuery, status: 'DELIVERED' } }
    case '异常任务':
      return { path: '/dms/dispatch-task', query: { status: '8' } }
    case '超时未签收':
      // 跨状态指标（待配送 + 配送中），配送查询页按逗号分隔多值承接
      return { path: '/dispatch/query', query: { ...dateQuery, status: 'PENDING,DELIVERING' } }
    case '配送费':
      return { path: '/dms/settlement' }
    case '代收货款':
      return { path: '/dms/payment' }
    case '在线配送员':
      return { path: '/dms/rider' }
    case '活跃车辆':
      return { path: '/dms/vehicle' }
    case '活跃人车绑定':
      return { path: '/dms/verification', query: { tab: 'binding' } }
    case '待处理预警':
      return { path: '/dms/verification', query: { tab: 'alert', handleStatus: '0' } }
    default:
      return null
  }
}

function handleCardDrill(item: StatCardItem) {
  const target = resolveDrill(item.label)
  if (!target) return
  router.push({ path: target.path, query: target.query })
}

// ═══════════════════════════════════════════════
// 图表
// ═══════════════════════════════════════════════
const trendChartOption = computed(() => {
  const rows = trend.value
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['单量', '完成量', '准时率'], bottom: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 11 } },
    grid: { left: 44, right: 48, top: 16, bottom: 40 },
    xAxis: {
      type: 'category',
      data: rows.map(p => p.statDate?.slice(5) || ''),
      axisLabel: { fontSize: 11 },
      axisTick: { show: false },
    },
    yAxis: [
      { type: 'value', name: '单量', nameTextStyle: { fontSize: 11 }, axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: '准时率', min: 0, max: 100, nameTextStyle: { fontSize: 11 }, axisLabel: { fontSize: 11, formatter: '{value}%' }, splitLine: { show: false } },
    ],
    series: [
      { name: '单量', type: 'bar', barMaxWidth: 16, itemStyle: { color: '#5B8FF9' }, data: rows.map(p => p.orderCount) },
      { name: '完成量', type: 'bar', barMaxWidth: 16, itemStyle: { color: '#61DDAA' }, data: rows.map(p => p.completedCount) },
      {
        name: '准时率', type: 'line', yAxisIndex: 1, smooth: true, connectNulls: true,
        itemStyle: { color: '#F6BD16' }, lineStyle: { width: 2 },
        tooltip: { valueFormatter: (v: any) => (v === null || v === undefined ? '-' : v + '%') },
        data: rows.map(p => p.onTimeRate),
      },
    ],
  }
})

const statusChartOption = computed(() => {
  const rows = taskSummary.value.filter(i => i.count > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle', itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['36%', '52%'],
      avoidLabelOverlap: true,
      label: { fontSize: 11, formatter: '{b}\n{c}' },
      labelLine: { length: 6, length2: 6 },
      data: rows.map(i => ({ name: i.statusName, value: i.count, status: i.status })),
    }],
  }
})

const channelChartOption = computed(() => {
  const rows = channelDistribution.value.filter(i => i.orderCount > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 单 ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle', itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['36%', '52%'],
      avoidLabelOverlap: true,
      label: { fontSize: 11, formatter: '{b}\n{c}' },
      labelLine: { length: 6, length2: 6 },
      data: rows.map(i => ({ name: i.itemName, value: i.orderCount })),
    }],
  }
})

/** 点击状态饼图扇区 → 调度任务页按该状态过滤 */
function handleStatusChartClick(params: any) {
  const status = params?.data?.status
  if (status === undefined || status === null) return
  router.push({ path: '/dms/dispatch-task', query: { status: String(status) } })
}

// ═══════════════════════════════════════════════
// 待办表格
// ═══════════════════════════════════════════════
const bindingColumns = [
  { title: '配送员', dataIndex: 'riderName', width: 110, ellipsis: true },
  { title: '车牌号', dataIndex: 'plateNo', width: 110 },
  { title: '绑定时间', dataIndex: 'bindTime', width: 160 },
  { title: '绑定里程', dataIndex: 'bindMileage', width: 100, align: 'right' as const },
  { title: '状态', dataIndex: 'status', width: 90 },
]

const alertColumns = [
  { title: '类型', dataIndex: 'alertType', width: 110 },
  { title: '级别', dataIndex: 'alertLevel', width: 80 },
  { title: '配送员', dataIndex: 'riderName', width: 100, ellipsis: true },
  { title: '内容', dataIndex: 'alertContent', ellipsis: true },
  { title: '时间', dataIndex: 'createTime', width: 160 },
]

const topRiderColumns = [
  { title: '排名', dataIndex: 'rank', width: 70, align: 'center' as const },
  { title: '配送员', dataIndex: 'riderName', width: 140 },
  { title: '手机号', dataIndex: 'phone', width: 130 },
  { title: '接单量', dataIndex: 'orderCount', width: 100, align: 'right' as const },
  { title: '完成量', dataIndex: 'completedCount', width: 100, align: 'right' as const },
  { title: '准时率', dataIndex: 'onTimeRate', width: 100, align: 'right' as const },
  { title: '平均时长', dataIndex: 'avgMinutes', width: 110, align: 'right' as const },
  { title: '评分', dataIndex: 'ratingScore', width: 90, align: 'right' as const },
]

function bindingStatusMeta(status: number) {
  return DMS_BINDING_STATUS[status] || { text: `状态${status}`, color: 'default' }
}

function alertTypeMeta(type: number) {
  return DMS_ALERT_TYPE[type] || { text: `类型${type}`, color: 'default' }
}

function alertLevelMeta(level: number) {
  return DMS_ALERT_LEVEL[level] || { text: `级别${level}`, color: 'default' }
}

// ═══════════════════════════════════════════════
// 导出（真实 xlsx，多 Sheet：KPI / 趋势 / 状态分布 / 渠道分布 / 配送员绩效）
// ═══════════════════════════════════════════════
function handleExport() {
  try {
    const wb = XLSX.utils.book_new()
    const s = stats
    const kpiRows = [
      { 指标: '统计口径', 值: s.rangeLabel, 说明: `${s.startTime} ~ ${s.endTime}` },
      { 指标: '配送单量', 值: s.orderCount, 说明: '按任务创建时间落在统计区间' },
      { 指标: '待分配任务', 值: s.pendingOrders, 说明: '状态=待分配' },
      { 指标: '已分配/已接单', 值: s.assignedOrders, 说明: '状态=已分配/已接单' },
      { 指标: '在途任务', 值: s.inTransitOrders, 说明: '状态=取货中/配送中' },
      { 指标: '已完成', 值: s.completedOrders, 说明: '状态=已签收/已完成' },
      { 指标: '已取消', 值: s.cancelledOrders, 说明: '状态=已取消' },
      { 指标: '异常', 值: s.exceptionOrders, 说明: '状态=异常' },
      { 指标: '准时率(%)', 值: s.onTimeRate ?? '-', 说明: '准时签收量 / 有截止时间的已签收量' },
      { 指标: '平均配送时长(分钟)', 值: s.avgDeliveryMinutes ?? '-', 说明: 'avg(完成时间 - 取货时间)' },
      { 指标: '配送费', 值: s.deliveryFee, 说明: '区间内配送费合计' },
      { 指标: '代收货款', 值: s.collectOnDelivery, 说明: '区间内代收货款合计' },
      { 指标: '货值', 值: s.goodsAmount, 说明: '区间内任务货值合计' },
      { 指标: '在线配送员', 值: s.onlineRiders, 说明: `${s.onlineThresholdMinutes} 分钟内有位置上报` },
      { 指标: '活跃配送员', 值: s.activeRiders, 说明: '状态=空闲/忙碌' },
      { 指标: '配送员总数', 值: s.totalRiders, 说明: '' },
      { 指标: '活跃车辆', 值: s.activeVehicles, 说明: '状态=使用中/已出勤' },
      { 指标: '车辆总数', 值: s.totalVehicles, 说明: '' },
      { 指标: '活跃人车绑定', 值: s.activeBindingCount, 说明: '绑定中' },
      { 指标: '待处理预警', 值: s.pendingAlertCount, 说明: '未处理核验预警' },
    ]
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(kpiRows), 'KPI')

    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(trend.value.map(p => ({
      日期: p.statDate,
      单量: p.orderCount,
      完成量: p.completedCount,
      准时量: p.onTimeCount,
      准时率: p.onTimeRate ?? '',
      平均配送时长: p.avgMinutes ?? '',
      配送费: p.deliveryFee,
    }))), '单量时效趋势')

    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(taskSummary.value.map(i => ({
      状态: i.statusName,
      数量: i.count,
      占比: i.ratio + '%',
    }))), '任务状态分布')

    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(channelDistribution.value.map(i => ({
      渠道: i.itemName,
      单量: i.orderCount,
      配送费: i.amount,
    }))), '渠道分布')

    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(topRiders.value.map((r, idx) => ({
      排名: idx + 1,
      配送员: r.riderName,
      手机号: r.phone || '',
      接单量: r.orderCount,
      完成量: r.completedCount,
      准时率: r.onTimeRate ?? '',
      平均时长: r.avgMinutes ?? '',
      评分: r.ratingScore ?? '',
    }))), '配送员绩效')

    XLSX.writeFile(wb, `配送仪表盘_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success('已导出配送仪表盘数据')
  } catch (e) {
    message.error('导出失败')
    console.warn('[配送仪表盘] 导出失败', e)
  }
}

// ═══════════════════════════════════════════════
// 全屏 / 快捷键
// ═══════════════════════════════════════════════
const isFullscreen = ref(false)

function toggleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    document.documentElement.requestFullscreen()
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    loadAll()
  }
}

// ═══════════════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════════════
onMounted(async () => {
  document.addEventListener('fullscreenchange', onFullscreenChange)
  window.addEventListener('keydown', onKeydown)
  loadChannels()
  await loadAll()
  if (autoRefresh.value) startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* 看板主体：占满表格面板并纵向滚动 */
.dashboard-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 16px 20px;
  background: #f5f6f8;
}

/* ═══ 时间范围快捷段 ═══ */
.range-switch {
  display: flex;
  align-items: center;
  gap: 2px;
}
.range-switch :deep(.ant-btn-link) {
  color: #555;
  padding: 0 6px;
  height: 24px;
  line-height: 24px;
}
.range-switch :deep(.ant-btn-primary) {
  background: #fa8c16;
  border-color: #fa8c16;
  padding: 0 10px;
  height: 24px;
  line-height: 24px;
}

/* ═══ 查询区（插槽内容需自备样式） ═══ */
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}
.search-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
}

/* ═══ 口径回显条 ═══ */
.stats-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20px;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #eef0f3;
  font-size: 12px;
  color: #606266;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.meta-label {
  color: #909399;
}
.meta-value {
  color: #303133;
  font-weight: 500;
}
.meta-tip {
  color: #fa8c16;
}

/* ═══ 图表区 ═══ */
.chart-row {
  margin-bottom: 12px;
}

/* ═══ 待办 / 绩效区块 ═══ */
.section-card {
  background: #fff;
  border-radius: 6px;
  border: 1px solid #eef0f3;
  padding: 12px 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.section-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.section-card__header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.section-count {
  font-size: 12px;
  color: #909399;
}
</style>
