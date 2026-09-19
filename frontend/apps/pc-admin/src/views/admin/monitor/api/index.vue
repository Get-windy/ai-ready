<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        接口监控（系统 → 系统监控 → 接口监控，菜单 62203）
        · 平台控制台页面：ql361 无对标，业界对标见《接口监控开发文档》§8
        · 数据源全部为后端真实接口，**无任何写死常量、无假数据兜底**：
            ① 交易侧 ApiMonitorController（`/api/trade/api-monitor/*`，开发文档 §12 路线甲＝复用而非自建）
               卡片统计 / 依赖健康 / 调用台账分页 / 实时告警判定 / 阈值
            ② 系统侧 SystemMonitorController（`/api/monitor/overview`）
               系统状态（status + CPU / 内存 / 磁盘占用）
            ③ 系统侧 AlertManagementController（`/api/monitor/alerts/*`）
               告警历史（含清空）与告警统计
        · 已彻底删除的历史桩（原 285 行整页桩）：
            GET /api/monitor/dashboard（后端零命中，必然 404）
            GET /api/monitor/info（后端零命中，必然 404）
            写死的「系统状态 = 正常」假绿常量
            恒空的「API 端点概览」折叠面板（数据源永远为空对象）
            每 30 秒静默 404 轮询 setInterval(refreshAll, 30000)
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/接口监控开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：自动刷新（默认关闭，周期取自后端阈值配置 autoRefreshSeconds） ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-switch
              v-model:checked="autoRefreshOn"
              size="small"
              @change="handleAutoRefreshChange"
            />
            <span class="toolbar-tip">
              自动刷新{{ autoRefreshOn ? `（${autoRefreshSeconds}s）` : '' }}
            </span>
            <span
              v-if="lastRefreshAt"
              class="toolbar-tip"
            >最近刷新 {{ lastRefreshAt }}</span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
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
          </a-space>
        </template>

        <!-- ═══ 查询区（横向 flex；仅使用交易侧 /calls/page 真实支持的参数） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <!--
                ⚠️ 请求方法（requestMethod）后端**不支持**作为查询条件：
                ApiMonitorController#callsPage 的入参为 channelCode / apiPath（模糊）/ direction /
                status / keyword（请求号·错误信息 模糊）/ startTime / endTime（见 ApiCallQuery）。
                故此处不提供「请求方法」下拉，避免发明后端不生效的筛选；方法仅作为台账列展示。
              -->
              <template v-if="isQueryFieldVisible('apiPath')">
                <span class="search-label">端点路径</span>
                <a-input
                  v-model:value="searchForm.apiPath"
                  placeholder="如 /api/open/health（模糊匹配）"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('direction')">
                <span class="search-label">方向</span>
                <a-select
                  v-model:value="searchForm.direction"
                  placeholder="全部方向"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="DIRECTION_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('keyword')">
                <span class="search-label">关键字</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="请求号 / 错误信息"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('timeRange')">
                <span class="search-label">调用时间</span>
                <a-range-picker
                  v-model:value="timeRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 320px"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
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
            </div>
          </div>
        </template>

        <!-- ═══ 看板主体：真实汇总卡 + 告警 + 端点调用台账 ═══ -->
        <template #table>
          <div class="monitor-body">
            <!-- ── 一、汇总卡（全部为后端真实聚合，无数据时显示「-」/0，不编数字） ── -->
            <a-row :gutter="12">
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <!-- 系统状态：取代原写死的「正常」；后端无返回时显示「未知」 -->
                  <a-statistic
                    title="系统状态"
                    :value="0"
                    :formatter="() => overviewStatusText"
                    :value-style="{ color: overviewStatusColor }"
                  >
                    <template #suffix>
                      <a-tooltip
                        :title="overview.collectTime ? `采集时间：${fmtDateTime(overview.collectTime)}` : '尚未采集到系统指标'"
                        placement="bottom"
                      >
                        <InfoCircleOutlined class="kpi-help" />
                      </a-tooltip>
                    </template>
                  </a-statistic>
                  <div class="kpi-detail">
                    CPU: {{ fmtPercent(overview.cpuUsage) }} | 内存: {{ fmtPercent(overview.memoryUsage) }} | 磁盘: {{ fmtPercent(overview.diskUsage) }}
                  </div>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="今日调用量"
                    :value="0"
                    :formatter="() => cardText.callCount"
                  >
                    <template #suffix>
                      <a-tooltip
                        :title="statWindowText"
                        placement="bottom"
                      >
                        <InfoCircleOutlined class="kpi-help" />
                      </a-tooltip>
                    </template>
                  </a-statistic>
                  <div class="kpi-detail">
                    成功: {{ cardText.successCount }} | 失败: {{ cardText.failCount }}
                  </div>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="接口成功率"
                    :value="0"
                    :formatter="() => cardText.rate"
                    :value-style="{ color: successRateColor }"
                  />
                  <div class="kpi-detail">
                    P95 耗时: {{ cardText.p95Cost }}
                  </div>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="平均耗时"
                    :value="0"
                    :formatter="() => cardText.avgCost"
                  />
                  <div class="kpi-detail">
                    最大耗时: {{ cardText.maxCost }}
                  </div>
                </a-card>
              </a-col>
            </a-row>

            <a-row
              :gutter="12"
              style="margin-top:12px"
            >
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <!-- 依赖健康：逐项探测结果（DB / Redis / MQ / 地图 / 第三方渠道） -->
                  <a-statistic
                    title="依赖健康"
                    :value="0"
                    :formatter="() => depsHealthyText"
                  >
                    <template #suffix>
                      <a-tooltip placement="bottom">
                        <template #title>
                          <div
                            v-for="dep in deps"
                            :key="dep.key"
                          >
                            {{ dep.name }}：{{ depStatusText(dep.status) }}<template v-if="dep.latencyMs != null">（{{ dep.latencyMs }}ms）</template>
                          </div>
                          <div v-if="deps.length === 0">
                            暂无依赖探测结果
                          </div>
                        </template>
                        <InfoCircleOutlined class="kpi-help" />
                      </a-tooltip>
                    </template>
                  </a-statistic>
                  <div class="kpi-detail">
                    异常: {{ depDownCount }} | 未配置: {{ depUnknownCount }}
                  </div>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="实时告警"
                    :value="alerts.length"
                    :value-style="{ color: alertCriticalCount > 0 ? '#cf1322' : undefined }"
                  />
                  <div class="kpi-detail">
                    严重: {{ alertCriticalCount }} | 警告: {{ alertWarningCount }} | 静默中: {{ alertSilencedCount }}
                  </div>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="告警历史(24h)"
                    :value="0"
                    :formatter="() => fmtCount(alertStats.totalAlerts)"
                  />
                  <div class="kpi-detail">
                    未确认: {{ fmtCount(alertStats.unacknowledgedAlerts) }} | 生效规则: {{ fmtCount(alertStats.activeRules) }}
                  </div>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="库存同步失败数"
                    :value="0"
                    :formatter="() => cardText.syncFailed"
                    :value-style="{ color: Number(stats.syncFailedCount) > 0 ? '#cf1322' : undefined }"
                  />
                  <div class="kpi-detail">
                    同步记录: {{ cardText.syncTotal }} | 待同步: {{ cardText.syncPending }}
                  </div>
                </a-card>
              </a-col>
            </a-row>

            <!--
              口径说明（后端确实没有的指标如实注明，不编数字）：
              · 「错误率」无独立指标口径 —— 可由「接口成功率」反推（错误率 = 100% - 成功率）
              · 「吞吐量」无独立指标口径 —— 后端 /stat 只给今日调用量与平均/P95 耗时；
                /calls/trend 是按小时分桶（且仅统计当日区间），不足以换算每秒吞吐
              · 「CPU / 内存 / 磁盘」来自 /api/monitor/overview，为整机口径，非接口级
            -->
            <div class="metric-note">
              口径说明：后端无独立「错误率 / 吞吐量」指标 —— 错误率可由「接口成功率」反推；
              吞吐量以「今日调用量」为准（趋势接口按小时分桶，仅统计当日区间）。
              成功率 / 平均耗时 / P95 的统计窗口与数据源见各卡片提示。
            </div>

            <!-- ── 二、告警区块（实时判定 + 历史记录，两个数据源均为真实端点） ── -->
            <a-card
              size="small"
              class="panel-card"
              :bordered="false"
            >
              <template #title>
                异常告警
                <span class="panel-sub">{{ alertPanelSub }}</span>
              </template>
              <template #extra>
                <a-popconfirm
                  v-if="isButtonEnabled('cleanAlertHistory')"
                  title="确定清空全部告警历史记录？该操作不可恢复。"
                  ok-text="清空"
                  cancel-text="取消"
                  @confirm="handleClearAlertHistory"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                    :loading="clearingHistory"
                  >
                    清空历史
                  </a-button>
                </a-popconfirm>
              </template>
              <a-tabs
                v-model:active-key="alertTab"
                size="small"
              >
                <!-- Tab1 实时判定：交易侧按当前阈值实时派生，无主键（行键在加载时补齐） -->
                <a-tab-pane
                  key="live"
                  tab="实时判定"
                >
                  <div class="threshold-tip">
                    判定阈值（配置中心 · {{ thresholds.configGroup || '配送参数 → API监控' }}）：
                    错误率 &gt; {{ fmtPlain(thresholds.errorRatePercent, '%') }}、
                    P95 &gt; {{ fmtPlain(thresholds.p95Ms, 'ms') }}、
                    失败 &gt; {{ fmtPlain(thresholds.failCount, '次') }}、
                    同步失败 &gt; {{ fmtPlain(thresholds.syncFailCount, '条') }}；
                    静默期 {{ fmtPlain(thresholds.silenceMinutes, '分钟') }}
                  </div>
                  <div class="alert-table-box">
                    <a-table
                      size="small"
                      :columns="ALERT_LIVE_COLUMNS"
                      :data-source="alerts"
                      :loading="alertsLoading"
                      :pagination="false"
                      :locale="{ emptyText: '暂无异常告警' }"
                      row-key="id"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'level'">
                          <a-tag :color="record.level === 'CRITICAL' ? 'error' : 'warning'">
                            {{ record.level === 'CRITICAL' ? '严重' : '警告' }}
                          </a-tag>
                        </template>
                        <template v-else-if="column.key === 'alertType'">
                          {{ ALERT_TYPE_MAP[record.alertType] || record.alertType || '-' }}
                        </template>
                        <template v-else-if="column.key === 'silenced'">
                          <a-tag :color="record.silenced ? 'default' : 'processing'">
                            {{ record.silenced ? '静默期内' : '已外发事件' }}
                          </a-tag>
                        </template>
                      </template>
                    </a-table>
                  </div>
                </a-tab-pane>
                <!-- Tab2 历史记录：系统侧 sys_alert_history（当前表可为 0 行，空态即真实状态） -->
                <a-tab-pane
                  key="history"
                  tab="历史记录"
                >
                  <div class="alert-table-box">
                    <a-table
                      size="small"
                      :columns="ALERT_HISTORY_COLUMNS"
                      :data-source="alertHistory"
                      :loading="alertHistoryLoading"
                      :pagination="false"
                      :locale="{ emptyText: '最近 24 小时暂无告警历史记录' }"
                      row-key="id"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'alertTime'">
                          {{ fmtDateTime(record.alertTime) }}
                        </template>
                        <template v-else-if="column.key === 'severity'">
                          <a-tag :color="record.severity === 'critical' ? 'error' : record.severity === 'warning' ? 'warning' : 'default'">
                            {{ SEVERITY_MAP[record.severity] || record.severity || '-' }}
                          </a-tag>
                        </template>
                        <template v-else-if="column.key === 'acknowledged'">
                          <a-tag :color="record.acknowledged ? 'success' : 'default'">
                            {{ record.acknowledged ? '已确认' : '未确认' }}
                          </a-tag>
                        </template>
                        <template v-else-if="column.key === 'resolved'">
                          <a-tag :color="record.resolved ? 'success' : 'default'">
                            {{ record.resolved ? '已解决' : '未解决' }}
                          </a-tag>
                        </template>
                      </template>
                    </a-table>
                  </div>
                </a-tab-pane>
              </a-tabs>
            </a-card>

            <!-- ── 三、端点调用台账（api_access_log 真实分页，表头齿轮列配置） ── -->
            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                empty-text="暂无接口调用记录"
                storage-key="system-api-monitor-table-columns"
                global-config-key="system-api-monitor-table-columns"
              >
                <!-- 调用时间 -->
                <template #accessTimeCell="{ record }">
                  {{ fmtDateTime(record.accessTime) }}
                </template>

                <!-- 方向 -->
                <template #directionCell="{ record }">
                  <a-tag :color="DIRECTION_MAP[record.direction]?.color || 'default'">
                    {{ DIRECTION_MAP[record.direction]?.label || record.direction || '-' }}
                  </a-tag>
                </template>

                <!-- 状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="record.status === 'SUCCESS' ? 'success' : 'error'">
                    {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
                  </a-tag>
                </template>

                <!-- 耗时（慢请求高亮阈值取 P95 告警线，配置中心可改） -->
                <template #costCell="{ record }">
                  <span :class="record.responseTime > slowThreshold ? 'cost-slow' : ''">
                    {{ record.responseTime == null ? '-' : `${record.responseTime} ms` }}
                  </span>
                </template>
              </BillDetailTable>
            </div>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  SettingOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import request from '@/utils/request'
import {
  apiMonitorApi,
  type ApiCallLog,
  type ApiMonitorAlert,
  type DependencyHealth,
} from '@/api/trade'

defineOptions({ name: 'SystemMonitorApi' })

const PAGE_CONFIG_STORAGE_KEY = 'system-api-monitor-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 字典（与后端枚举严格对齐：ApiCallDirection / ApiCallStatus） ═══
const DIRECTION_MAP: Record<string, { label: string; color: string }> = {
  IN: { label: '入站', color: 'blue' },
  OUT: { label: '出站', color: 'purple' },
  SANDBOX: { label: '联调', color: 'orange' },
}
const DIRECTION_OPTIONS = Object.entries(DIRECTION_MAP).map(([value, v]) => ({ value, label: v.label }))
const STATUS_OPTIONS = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAIL', label: '失败' },
]
const ALERT_TYPE_MAP: Record<string, string> = {
  ERROR_RATE: '错误率',
  P95_LATENCY: 'P95 耗时',
  FAIL_COUNT: '失败次数',
  SYNC_FAILED: '库存同步失败',
  DEPENDENCY: '依赖异常',
}
const SEVERITY_MAP: Record<string, string> = {
  critical: '严重',
  warning: '警告',
  info: '提示',
}

// ═══ 告警区块列定义（实时判定 / 历史记录） ═══
const ALERT_LIVE_COLUMNS = [
  { title: '级别', dataIndex: 'level', key: 'level', width: 80 },
  { title: '类型', dataIndex: 'alertType', key: 'alertType', width: 110 },
  { title: '标题', dataIndex: 'title', key: 'title', width: 200 },
  { title: '明细', dataIndex: 'detail', key: 'detail', ellipsis: true },
  { title: '当前值', dataIndex: 'currentValue', key: 'currentValue', width: 100 },
  { title: '阈值', dataIndex: 'threshold', key: 'threshold', width: 90 },
  { title: '事件', dataIndex: 'silenced', key: 'silenced', width: 100 },
  { title: '处置建议', dataIndex: 'suggestion', key: 'suggestion', width: 260 },
]
const ALERT_HISTORY_COLUMNS = [
  { title: '告警时间', dataIndex: 'alertTime', key: 'alertTime', width: 160 },
  { title: '规则', dataIndex: 'ruleName', key: 'ruleName', width: 150 },
  { title: '指标', dataIndex: 'metricName', key: 'metricName', width: 130 },
  { title: '当前值', dataIndex: 'metricValue', key: 'metricValue', width: 90 },
  { title: '阈值', dataIndex: 'threshold', key: 'threshold', width: 80 },
  { title: '级别', dataIndex: 'severity', key: 'severity', width: 80 },
  { title: '内容', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '确认', dataIndex: 'acknowledged', key: 'acknowledged', width: 90 },
  { title: '解决', dataIndex: 'resolved', key: 'resolved', width: 90 },
]

// ═══ 状态 ═══
const loading = ref(false)
const lastRefreshAt = ref('')
const alertTab = ref('live')

// 系统概览（/api/monitor/overview）
const overview = ref<Record<string, any>>({})

// 交易侧统计（/api/trade/api-monitor/stat）
const stats = ref<Record<string, any>>({})
const thresholds = ref<Record<string, any>>({})

// 依赖健康
const deps = ref<DependencyHealth[]>([])

// 告警
const alerts = ref<ApiMonitorAlert[]>([])
const alertsLoading = ref(false)
const alertHistory = ref<any[]>([])
const alertHistoryLoading = ref(false)
const alertStats = ref<Record<string, any>>({})
const clearingHistory = ref(false)

// 台账
const tableData = ref<ApiCallLog[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  apiPath: '' as string,
  direction: undefined as string | undefined,
  status: undefined as string | undefined,
  keyword: '' as string,
})
const timeRange = ref<[string, string] | undefined>()

// 自动刷新（默认关闭）
const autoRefreshOn = ref(false)
const autoRefreshSeconds = ref(30)
let autoTimer: any = null

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'apiPath', label: '端点路径', visible: true },
  { key: 'direction', label: '方向', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'keyword', label: '关键字', visible: true },
  { key: 'timeRange', label: '调用时间', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'autoRefresh', label: '自动刷新', enabled: true },
  { key: 'cleanAlertHistory', label: '清空告警历史', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

// ═══ 台账列（序号列承载表头「列配置」齿轮；插槽列必须 type:'slot'） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'accessTime', title: '调用时间', type: 'slot', slotName: 'accessTimeCell', width: 160, fixed: 'left' },
  { key: 'direction', title: '方向', type: 'slot', slotName: 'directionCell', width: 80 },
  { key: 'apiPath', title: '端点路径', type: 'input', width: 260 },
  { key: 'apiName', title: '接口名称', type: 'input', width: 170 },
  { key: 'requestMethod', title: '请求方法', type: 'input', width: 90 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'responseCode', title: '响应码', type: 'input', width: 80 },
  { key: 'responseTime', title: '耗时', type: 'slot', slotName: 'costCell', width: 100 },
  { key: 'channelCode', title: '渠道', type: 'input', width: 100 },
  { key: 'errorCode', title: '错误码', type: 'input', width: 140, defaultHidden: true },
  { key: 'errorMsg', title: '错误信息', type: 'input', width: 240 },
  { key: 'requestId', title: '请求号', type: 'input', width: 220, defaultHidden: true },
  { key: 'ipAddress', title: '调用方IP', type: 'input', width: 130, defaultHidden: true },
  { key: 'userAgent', title: 'User-Agent', type: 'input', width: 220, defaultHidden: true },
]

// ═══ 计算属性 ═══
/**
 * 卡片展示文本（预计算为字符串，经 a-statistic 的 formatter 原样渲染）
 *
 * ⚠️ 不能把字符串直接传给 a-statistic 的 value：其内部按 /^(-?)(\d*)(\.(\d+))?$/ 解析，
 * 「-」会被当成「负号 + 空整数 → 0」渲染成 `-0`（无数据卡片显示异常）。
 */
const cardText = computed(() => ({
  callCount: fmtCount(stats.value.todayCallCount),
  successCount: fmtCount(stats.value.todaySuccessCount),
  failCount: fmtCount(stats.value.todayFailCount),
  rate: fmtRate(stats.value.successRate),
  avgCost: fmtMs(stats.value.avgCostMs),
  maxCost: fmtMs(stats.value.maxCostMs),
  p95Cost: fmtMs(stats.value.p95CostMs),
  syncFailed: fmtCount(stats.value.syncFailedCount),
  syncTotal: fmtCount(stats.value.syncTotalCount),
  syncPending: fmtCount(stats.value.syncPendingCount),
}))

/** 系统状态文案：仅依据 /monitor/overview 的 status 字段，取不到即「未知」（绝不假绿） */
const overviewStatusText = computed(() => {
  const s = String(overview.value.status || '').toLowerCase()
  if (!s) return '未知'
  if (s === 'healthy' || s === 'up') return '正常'
  return '异常'
})
const overviewStatusColor = computed(() => {
  const s = String(overview.value.status || '').toLowerCase()
  if (!s) return '#8c8c8c'
  if (s === 'healthy' || s === 'up') return '#3f8600'
  return '#cf1322'
})

const successRateColor = computed(() => {
  const v = stats.value.successRate
  if (v === null || v === undefined || v === '') return undefined
  return Number(v) >= 99 ? '#3f8600' : Number(v) >= 90 ? '#d46b08' : '#cf1322'
})

const depDownCount = computed(() => deps.value.filter(d => d.status === 'DOWN').length)
const depUnknownCount = computed(() => deps.value.filter(d => d.status === 'NOT_CONFIGURED').length)
const depsHealthyText = computed(() => {
  if (deps.value.length === 0) return '-'
  return `${deps.value.length - depDownCount.value}/${deps.value.length}`
})

const alertCriticalCount = computed(() => alerts.value.filter(a => a.level === 'CRITICAL').length)
const alertWarningCount = computed(() => alerts.value.filter(a => a.level === 'WARNING').length)
const alertSilencedCount = computed(() => alerts.value.filter(a => a.silenced).length)
const alertPanelSub = computed(() => {
  if (alertsLoading.value || alertHistoryLoading.value) return '加载中'
  return `实时告警 ${alerts.value.length} 条 · 历史记录 ${alertHistory.value.length} 条`
})

/** 统计窗口说明（后端返回 statFrom / statTo，原样展示，避免误解为「全部历史」） */
const statWindowText = computed(() => {
  const from = stats.value.statFrom
  const to = stats.value.statTo
  const source = stats.value.callLogSource || 'api_access_log'
  if (!from || !to) return source
  return `统计窗口 ${String(from).replace('T', ' ')} ~ ${String(to).replace('T', ' ')}；数据源：${source}`
})

/** 慢请求高亮阈值：取 P95 告警线（配置中心可改） */
const slowThreshold = computed(() => Number(thresholds.value.p95Ms) || 2000)

// ═══ 格式化 ═══
function fmtCount(v: any) {
  return v === null || v === undefined || v === '' ? '-' : String(v)
}
function fmtRate(v: any) {
  return v === null || v === undefined || v === '' ? '-' : `${Number(v).toFixed(2)}%`
}
function fmtMs(v: any) {
  return v === null || v === undefined || v === '' ? '-' : `${v} ms`
}
function fmtPercent(v: any) {
  return v === null || v === undefined || v === '' ? '-' : `${Number(v).toFixed(1)}%`
}
function fmtPlain(v: any, unit: string) {
  return v === null || v === undefined || v === '' ? '-' : `${v}${unit}`
}
function fmtDateTime(val?: string | null) {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}
function depStatusText(status: string) {
  if (status === 'UP') return '正常'
  if (status === 'DOWN') return '异常'
  return '未配置'
}

// ═══ 数据加载（每个 loader 失败即清空自身数据，不留陈旧值） ═══
async function loadOverview() {
  try {
    const res: any = await request.get('/monitor/overview')
    overview.value = res && typeof res === 'object' ? res : {}
  } catch (error: any) {
    console.error('[接口监控] 系统概览加载失败', error)
    message.error(error?.message || '系统概览加载失败')
    overview.value = {}
  }
}

async function loadStats() {
  try {
    const res: any = await apiMonitorApi.stat()
    stats.value = res && typeof res === 'object' ? res : {}
  } catch (error: any) {
    console.error('[接口监控] 统计加载失败', error)
    message.error(error?.message || '接口统计加载失败')
    stats.value = {}
  }
}

async function loadThresholds() {
  try {
    const res: any = await apiMonitorApi.thresholds()
    thresholds.value = res && typeof res === 'object' ? res : {}
    // 自动刷新周期取自后端配置（配送参数 → API监控），取下限 5 秒防止误配打挂后端
    const seconds = Number(thresholds.value.autoRefreshSeconds)
    autoRefreshSeconds.value = seconds > 0 ? Math.max(seconds, 5) : 30
  } catch (error: any) {
    console.error('[接口监控] 阈值加载失败', error)
    message.error(error?.message || '告警阈值加载失败')
    thresholds.value = {}
  }
}

async function loadDeps() {
  try {
    const res: any = await apiMonitorApi.deps()
    deps.value = Array.isArray(res) ? res : []
  } catch (error: any) {
    console.error('[接口监控] 依赖健康加载失败', error)
    message.error(error?.message || '依赖健康加载失败')
    deps.value = []
  }
}

/** 实时告警为派生数据（无主键）：补稳定行键，避免表格 row-key 重复 */
async function loadAlerts() {
  alertsLoading.value = true
  try {
    const res: any = await apiMonitorApi.alerts()
    const list: ApiMonitorAlert[] = Array.isArray(res) ? res : []
    alerts.value = list.map((alert, index) => ({ ...alert, id: `${alert.alertType}-${index}` }))
  } catch (error: any) {
    console.error('[接口监控] 实时告警判定失败', error)
    message.error(error?.message || '实时告警判定失败')
    alerts.value = []
  } finally {
    alertsLoading.value = false
  }
}

async function loadAlertHistory() {
  alertHistoryLoading.value = true
  try {
    const res: any = await request.get('/monitor/alerts/history', { params: { hours: 24, limit: 50 } })
    alertHistory.value = Array.isArray(res) ? res : []
  } catch (error: any) {
    console.error('[接口监控] 告警历史加载失败', error)
    message.error(error?.message || '告警历史加载失败')
    alertHistory.value = []
  } finally {
    alertHistoryLoading.value = false
  }
}

async function loadAlertStats() {
  try {
    const res: any = await request.get('/monitor/alerts/statistics', { params: { hours: 24 } })
    alertStats.value = res && typeof res === 'object' ? res : {}
  } catch (error: any) {
    console.error('[接口监控] 告警统计加载失败', error)
    message.error(error?.message || '告警统计加载失败')
    alertStats.value = {}
  }
}

/** 台账分页参数：仅发送后端 ApiCallQuery 声明支持的字段 */
function buildQuery(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.apiPath) params.apiPath = searchForm.apiPath.trim()
  if (searchForm.direction) params.direction = searchForm.direction
  if (searchForm.status) params.status = searchForm.status
  if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
  if (timeRange.value?.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  return params
}

async function loadTable() {
  loading.value = true
  try {
    const res: any = await apiMonitorApi.callsPage(buildQuery())
    tableData.value = Array.isArray(res?.records) ? res.records : []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[接口监控] 调用台账加载失败', error)
    message.error(error?.message || '调用台账加载失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 台账数据发生变化后，统计口径需同步刷新（否则卡片与列表口径不一致） */
async function refreshAll() {
  await Promise.all([loadOverview(), loadDeps(), loadThresholds()])
  await Promise.all([loadTable(), loadAlerts(), loadAlertHistory(), loadAlertStats()])
  await loadStats()
  lastRefreshAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  searchForm.apiPath = ''
  searchForm.direction = undefined
  searchForm.status = undefined
  searchForm.keyword = ''
  timeRange.value = undefined
  pagination.current = 1
  loadTable()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadTable()
}

/** 清空告警历史：DELETE /api/monitor/alerts/history?olderThanHours=0（清空全部，含二次确认） */
async function handleClearAlertHistory() {
  clearingHistory.value = true
  try {
    await request.delete('/monitor/alerts/history', { params: { olderThanHours: 0 } })
    message.success('告警历史已清空')
    await Promise.all([loadAlertHistory(), loadAlertStats()])
  } catch (error: any) {
    console.error('[接口监控] 清空告警历史失败', error)
    message.error(error?.message || '清空告警历史失败')
  } finally {
    clearingHistory.value = false
  }
}

// ═══ 自动刷新（默认关闭；替代原「每 30 秒静默 404 轮询」） ═══
function stopAutoRefresh() {
  if (autoTimer) {
    clearInterval(autoTimer)
    autoTimer = null
  }
}

function handleAutoRefreshChange(checked: boolean | string | number) {
  const on = !!checked
  if (on && !isButtonEnabled('autoRefresh')) {
    message.warning('「自动刷新」已在页面配置中禁用')
    autoRefreshOn.value = false
    return
  }
  stopAutoRefresh()
  if (on) {
    autoTimer = setInterval(() => refreshAll(), autoRefreshSeconds.value * 1000)
    message.success(`已开启自动刷新（每 ${autoRefreshSeconds.value} 秒）`)
  }
}

function handleError(error: Error) {
  console.error('[接口监控] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  refreshAll()
})

onBeforeUnmount(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.toolbar-tip { font-size: 12px; color: #8c8c8c; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 340px; margin-top: 12px; overflow: hidden; display: flex; flex-direction: column; }

/* 看板主体：卡片与告警区固定高度，台账独占剩余空间；内容超出时整体滚动 */
.monitor-body { flex: 1; min-height: 0; overflow: auto; display: flex; flex-direction: column; padding: 12px 16px 0; }
.kpi-card { background: #fafafa; border-radius: 6px; }
.kpi-detail { margin-top: 6px; font-size: 12px; color: #8c8c8c; }
.kpi-help { color: #8c8c8c; font-size: 12px; cursor: help; }
/* 指标口径说明（后端缺失的指标如实注明，不编数字） */
.metric-note { margin-top: 10px; font-size: 12px; color: #8c8c8c; line-height: 1.7; }

.panel-card { margin-top: 12px; background: #fff; }
.panel-sub { margin-left: 8px; font-size: 12px; color: #8c8c8c; font-weight: 400; }
.threshold-tip { margin-bottom: 8px; font-size: 12px; color: #8c8c8c; }
/* 告警两张小表限高滚动，避免把台账挤出首屏 */
.alert-table-box { max-height: 200px; overflow: auto; }

.cost-slow { color: #cf1322; font-weight: 600; }

:deep(.ant-statistic-title) { font-size: 12px; }
:deep(.ant-statistic-content) { font-size: 18px; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
