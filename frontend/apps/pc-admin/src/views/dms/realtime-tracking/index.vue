<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        实时跟踪（配送 → 配送跟踪 → 实时跟踪，菜单 80740 `dms:realtime-tracking`）
        · 定位（《实时跟踪开发文档》§1）：在线配送员【最新位置 + 轨迹回放】的可视化盯盘页
          —— 与《配送跟踪》（明细台账/可导出对账）分工不同
        · 骨架：CategoryListLayout（左侧在线状态分组）+ BillTableList（序号齿轮列配置）+ PageConfigPanel
        · 地图：复用 components/business/RouteMapCanvas（无 Key 时保持矢量画布，见 §配置落位）
        · 数据：统计卡与配送员位置均由后端一次聚合（/tracking/stat、/tracking/rider-page），
                取代改造前的「拉 100 个配送员 + 逐个 /latest」N+1
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="配送员状态"
        :category-editable="false"
        :category-tree-data="panelTree"
        :selected-category-id="panelKey"
        :show-table-footer="true"
        @category-select="handlePanelSelect"
      >
        <!-- ═══ 工具栏左侧：异常预警 + 全屏地图 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-badge
              :count="stat.alertCount"
              :offset="[-4, 4]"
              :number-style="{ backgroundColor: '#ff4d4f' }"
            >
              <a-button
                v-if="isButtonEnabled('alerts')"
                size="small"
                @click="openAlerts"
              >
                <AlertOutlined /> 异常预警
              </a-button>
            </a-badge>
            <a-button
              v-if="isButtonEnabled('fullscreenMap')"
              size="small"
              @click="openMapFullscreen"
            >
              <FullscreenOutlined /> 全屏地图
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 自动刷新 + 刷新 + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <span
              v-if="isButtonEnabled('autoRefresh')"
              class="auto-refresh"
            >
              <a-switch
                v-model:checked="autoRefresh"
                size="small"
              />
              <span class="auto-refresh-text">{{ autoRefresh ? `${countdown}s` : '自动刷新' }}</span>
            </span>
            <span
              class="stream-state"
              :class="streamConnected ? 'stream-on' : 'stream-off'"
              :title="streamConnected ? 'SSE 实时推送已连接' : 'SSE 未连接，使用 30s 兜底轮询'"
            >
              {{ streamConnected ? '实时推送已连接' : '实时推送轮询中' }}
            </span>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              :disabled="!currentRider"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">配送员</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="姓名/手机号"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">在线状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('onlineOnly')"
                class="search-item"
              >
                <a-checkbox
                  v-model:checked="searchForm.onlineOnly"
                  @change="handleSearch"
                >
                  仅看在线
                </a-checkbox>
              </div>
              <div
                v-if="isQueryVisible('replayRange')"
                class="search-item"
              >
                <span class="search-label">回放时间</span>
                <a-range-picker
                  v-model:value="replayRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 320px"
                  @change="loadReplayIfSelected"
                />
              </div>
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
              <span
                v-if="currentRider"
                class="picked-tip"
              >
                已选：{{ currentRider.riderName || currentRider.riderId }}
              </span>
            </div>
          </div>
        </template>

        <!-- ═══ 数据区：统计卡 + 地图 + 明细表 ═══ -->
        <template #table>
          <div class="table-area">
            <!-- 统计卡（后端聚合） -->
            <div class="stat-bar">
              <div class="stat-item">
                <span class="stat-label">配送员总数</span>
                <span class="stat-value">{{ stat.riderTotal }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">在线</span>
                <span class="stat-value stat-ok">{{ stat.onlineCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">有位置</span>
                <span class="stat-value">{{ stat.withLocationCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">无位置</span>
                <span class="stat-value stat-muted">{{ stat.noLocationCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">在途任务</span>
                <span class="stat-value">{{ stat.activeTaskCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">超时在途</span>
                <span class="stat-value stat-danger">{{ stat.overdueTaskCount }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">异常预警</span>
                <span class="stat-value stat-warn">{{ stat.alertCount }}</span>
              </div>
              <span class="online-tip">在线判定：最近 {{ stat.onlineMinutes }} 分钟内有上报</span>
            </div>

            <!-- 地图 + 回放（复用矢量地图组件） -->
            <div class="map-card">
              <div class="map-toolbar">
                <span class="map-title">
                  {{ currentRider ? `${currentRider.riderName || currentRider.riderId} · 轨迹回放` : '配送员位置（点击下方「查看轨迹」查看回放）' }}
                </span>
                <a-space :size="8">
                  <a-select
                    v-if="replayPoints.length > 1"
                    v-model:value="replaySpeed"
                    size="small"
                    style="width: 90px"
                    :options="SPEED_OPTIONS"
                  />
                  <a-button
                    size="small"
                    :disabled="replayPoints.length < 2"
                    @click="toggleReplay"
                  >
                    {{ replaying ? '暂停' : '播放' }}
                  </a-button>
                  <span class="sub-text">{{ replayIndex }} / {{ replayPoints.length }}</span>
                  <a-button
                    size="small"
                    :disabled="replayPoints.length === 0"
                    @click="resetReplay"
                  >
                    重置
                  </a-button>
                </a-space>
              </div>
              <RouteMapCanvas
                :markers="mapMarkers"
                :polyline="mapPolyline"
                :height="'240px'"
                :interactive="true"
                title="实时位置"
                empty-text="暂无位置数据（选择配送员后显示其轨迹回放）"
              />
            </div>

            <!-- 明细表：配送员位置 / 轨迹点 双视图（§3.4 两套列配置） -->
            <a-tabs
              v-model:active-key="activeTab"
              size="small"
              class="detail-tabs"
            >
              <a-tab-pane
                key="rider"
                tab="配送员位置"
              />
              <a-tab-pane
                key="point"
                tab="轨迹点明细"
              />
            </a-tabs>
            <BillTableList
              :key="activeTab"
              :columns="activeTab === 'rider' ? riderColumns : pointColumns"
              :data-source="activeTab === 'rider' ? riderRows : pointRows"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              :storage-key="activeTab === 'rider' ? 'dms-realtime-tracking-columns' : 'dms-realtime-tracking-point-columns'"
              :global-config-key="activeTab === 'rider' ? 'dms-realtime-tracking-columns' : 'dms-realtime-tracking-point-columns'"
              row-key="riderId"
            >
              <template #riderCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.riderName || record.riderId }}
                  <span
                    v-if="record.riderPhone"
                    class="sub-text"
                  >{{ record.riderPhone }}</span>
                </span>
              </template>

              <template #onlineCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.online ? 'green' : 'default'"
                >
                  {{ record.online ? '在线' : '离线' }}
                </a-tag>
              </template>

              <template #statusCell="{ record }">
                <span v-if="!record.__ghost">{{ record.statusText || '-' }}</span>
              </template>

              <template #coordCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtCoord(record.lat, record.lng) }}</span>
              </template>

              <template #timeCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ fmtDateTime(record.lastReportTime) }}
                  <span
                    v-if="record.lastReportAgoSeconds != null"
                    class="sub-text"
                  >{{ fmtAgo(record.lastReportAgoSeconds) }}</span>
                </span>
              </template>

              <template #mileageCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtKm(record.todayMileageKm) }}</span>
              </template>

              <template #riderActionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="selectRider(record)"
                  >
                    查看轨迹
                  </a-button>
                </a-space>
              </template>

              <template #pointRiderCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.riderName || record.riderId }}
                </span>
              </template>

              <template #pointSpeedCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="record.speedAbnormal ? 'speed-abnormal' : ''"
                >
                  {{ fmtNum(record.speed) }}
                </span>
              </template>

              <template #pointSourceCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  color="blue"
                >
                  {{ record.sourceText || '-' }}
                </a-tag>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（配送员位置视图） ═══ -->
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

      <!-- ═══ 异常预警抽屉 ═══ -->
      <a-drawer
        v-model:open="alertsVisible"
        title="异常预警（超速 / 异常停留 / 偏航 / 超时在途）"
        :width="760"
        destroy-on-close
      >
        <a-table
          :data-source="alerts"
          :columns="alertColumns"
          :loading="alertsLoading"
          :pagination="{ pageSize: 20 }"
          row-key="rowKey"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'alertTypeText'">
              <a-tag :color="record.level === 'DANGER' ? 'red' : 'orange'">
                {{ record.alertTypeText }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'eventTime'">
              {{ fmtDateTime(record.eventTime) }}
            </template>
          </template>
        </a-table>
      </a-drawer>

      <!-- ═══ 全屏地图抽屉 ═══ -->
      <a-drawer
        v-model:open="mapFullscreen"
        title="全屏地图 · 配送员位置"
        :width="'100%'"
        destroy-on-close
      >
        <RouteMapCanvas
          :markers="allRiderMarkers"
          :polyline="mapPolyline"
          :height="'calc(100vh - 180px)'"
          :interactive="true"
          title="配送员位置"
          empty-text="暂无在线配送员位置"
        />
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-realtime-tracking-page-config"
        hide-print-config
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
  ReloadOutlined, DownloadOutlined, SettingOutlined, AlertOutlined, FullscreenOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import RouteMapCanvas from '@/components/business/RouteMapCanvas/index.vue'
import { trackingApi, type RiderLocationVO, type TrackingAlert, type TrackingVO } from '@/api/dms/tracking'

defineOptions({ name: 'DmsRealtimeTracking' })

const STATUS_OPTIONS = [
  { label: '离线', value: 0 }, { label: '空闲', value: 1 }, { label: '忙碌', value: 2 }, { label: '休息', value: 3 },
]
const SPEED_OPTIONS = [
  { label: '0.5x', value: 600 }, { label: '1x', value: 300 }, { label: '2x', value: 150 }, { label: '4x', value: 80 },
]

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const riderRows = ref<RiderLocationVO[]>([])
const pointRows = ref<TrackingVO[]>([])
const currentRider = ref<RiderLocationVO | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const activeTab = ref<'rider' | 'point'>('rider')

const stat = reactive({
  riderTotal: 0, onlineCount: 0, offlineCount: 0, withLocationCount: 0, noLocationCount: 0,
  activeTaskCount: 0, overdueTaskCount: 0, todayPointCount: 0, alertCount: 0, onlineMinutes: 2,
})

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
  onlineOnly: false,
})
const replayRange = ref<[string, string] | undefined>()

/** 左面板：在线状态分组 */
type PanelKey = 'ALL' | 'ONLINE' | 'OFFLINE'
const panelKey = ref<PanelKey>('ALL')
const panelTree = computed(() => ([
  { id: 'ALL', categoryName: `全部配送员(${stat.riderTotal})` },
  { id: 'ONLINE', categoryName: `在线(${stat.onlineCount})` },
  { id: 'OFFLINE', categoryName: `离线(${stat.offlineCount})` },
]))

function handlePanelSelect(keys: (string | number)[]) {
  panelKey.value = (keys?.[0] as PanelKey) || 'ALL'
  pagination.current = 1
  fetchRiders()
}

// ═══ 列配置（§3.4 两套列） ═══
const riderColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'riderActionCell', width: 110, fixed: 'left' },
  { title: '配送员', key: 'riderName', type: 'slot', slotName: 'riderCell', width: 150 },
  { title: '在线状态', key: 'online', type: 'slot', slotName: 'onlineCell', width: 100 },
  { title: '工作状态', key: 'statusText', type: 'slot', slotName: 'statusCell', width: 100 },
  { title: '当前位置', key: 'position', type: 'slot', slotName: 'coordCell', width: 190 },
  { title: '当前速度(km/h)', key: 'speed', width: 130 },
  { title: '方向', key: 'directionText', width: 90 },
  { title: '最近上报', key: 'lastReportTime', type: 'slot', slotName: 'timeCell', width: 200 },
  { title: '今日里程(km)', key: 'todayMileageKm', type: 'slot', slotName: 'mileageCell', width: 120 },
  { title: '今日单量', key: 'todayDoneTasks', width: 100 },
  { title: '在途负载', key: 'activeTasks', width: 100 },
  { title: '车辆', key: 'vehicleName', width: 110, defaultHidden: true },
  { title: '定位精度(米)', key: 'accuracy', width: 120, defaultHidden: true },
  { title: '地址', key: 'address', width: 200, defaultHidden: true },
  { title: '今日点数', key: 'todayPointCount', width: 100, defaultHidden: true },
]

const pointColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '上报时间', key: 'reportTime', width: 170 },
  { title: '配送员', key: 'riderName', type: 'slot', slotName: 'pointRiderCell', width: 150 },
  { title: '任务编号', key: 'taskNo', width: 150 },
  { title: '经度', key: 'lng', width: 120 },
  { title: '纬度', key: 'lat', width: 120 },
  { title: '速度(km/h)', key: 'speed', type: 'slot', slotName: 'pointSpeedCell', width: 120 },
  { title: '方向', key: 'directionText', width: 100 },
  { title: '来源', key: 'sourceText', type: 'slot', slotName: 'pointSourceCell', width: 110 },
  { title: '定位精度(米)', key: 'accuracy', width: 120, defaultHidden: true },
  { title: '地址', key: 'address', width: 200, defaultHidden: true },
]

const alertColumns = [
  { title: '类型', dataIndex: 'alertTypeText', width: 110 },
  { title: '配送员', dataIndex: 'riderName', width: 130 },
  { title: '任务编号', dataIndex: 'taskNo', width: 150 },
  { title: '时间', dataIndex: 'eventTime', width: 160 },
  { title: '说明', dataIndex: 'alertText' },
]

// ═══ 数据加载 ═══
function buildRiderParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.onlineOnly) params.onlineOnly = true
  if (panelKey.value === 'ONLINE') params.onlineOnly = true
  if (panelKey.value === 'OFFLINE') params.status = 0
  return params
}

async function fetchRiders() {
  loading.value = true
  try {
    const res: any = await trackingApi.riderPage(buildRiderParams())
    riderRows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载配送员位置失败')
  } finally {
    loading.value = false
  }
}

async function fetchStat() {
  try {
    const res: any = await trackingApi.stat()
    Object.assign(stat, {
      riderTotal: Number(res?.riderTotal) || 0,
      onlineCount: Number(res?.onlineCount) || 0,
      offlineCount: Number(res?.offlineCount) || 0,
      withLocationCount: Number(res?.withLocationCount) || 0,
      noLocationCount: Number(res?.noLocationCount) || 0,
      activeTaskCount: Number(res?.activeTaskCount) || 0,
      overdueTaskCount: Number(res?.overdueTaskCount) || 0,
      todayPointCount: Number(res?.todayPointCount) || 0,
      alertCount: Number(res?.alertCount) || 0,
      onlineMinutes: Number(res?.onlineMinutes) || 2,
    })
  } catch (error) {
    console.warn('[实时跟踪] 统计加载失败', error)
  }
}

function refreshAll() {
  fetchStat()
  fetchRiders()
  if (currentRider.value) loadReplayIfSelected()
}

function handleSearch() {
  pagination.current = 1
  refreshAll()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = undefined
  searchForm.onlineOnly = false
  panelKey.value = 'ALL'
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchRiders()
}

// ═══ 选中配送员 → 轨迹回放 ═══
const replayPoints = ref<TrackingVO[]>([])
const replayIndex = ref(0)
const replaying = ref(false)
const replaySpeed = ref(300)
let replayTimer: any = null

async function selectRider(record: RiderLocationVO) {
  currentRider.value = record
  stopReplay()
  await loadReplayIfSelected()
  await loadPoints(record)
}

async function loadPoints(record: RiderLocationVO) {
  if (!record?.riderId) return
  try {
    const res: any = await trackingApi.trackVO({
      // ⚠️ 雪花ID 超 JS 安全整数：必须原样传字符串，Number() 会丢精度（2099...8010 → 2099...8000）
      riderId: record.riderId,
      startTime: replayRange.value?.[0],
      endTime: replayRange.value?.[1],
      maxPoints: 200,
    })
    pointRows.value = Array.isArray(res) ? res : []
  } catch (error) {
    pointRows.value = []
  }
}

async function loadReplayIfSelected() {
  const rider = currentRider.value
  if (!rider?.riderId) return
  try {
    const res: any = await trackingApi.replay({
      riderId: rider.riderId,
      startTime: replayRange.value?.[0],
      endTime: replayRange.value?.[1],
      maxPoints: 500,
    })
    replayPoints.value = Array.isArray(res) ? res : []
    replayIndex.value = replayPoints.value.length
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载轨迹回放失败')
    replayPoints.value = []
  }
}

const revealCount = computed(() => (replayIndex.value > 0 ? replayIndex.value : replayPoints.value.length))

const mapPolyline = computed(() =>
  replayPoints.value
    .slice(0, Math.max(revealCount.value, 2))
    .filter(p => p.lat != null && p.lng != null)
    .map(p => ({ lat: Number(p.lat), lng: Number(p.lng) })))

const mapMarkers = computed(() => {
  const pts = replayPoints.value.filter(p => p.lat != null && p.lng != null)
  if (pts.length > 1) {
    const first = pts[0]
    const last = pts[pts.length - 1]
    return [
      { lat: Number(first.lat), lng: Number(first.lng), label: '起点', color: '#52c41a' },
      { lat: Number(last.lat), lng: Number(last.lng), label: '终点', color: '#f5222d' },
    ]
  }
  return allRiderMarkers.value
})

/** 全部有位置的配送员标记（全屏地图用） */
const allRiderMarkers = computed(() =>
  riderRows.value
    .filter(r => r.lat != null && r.lng != null)
    .map(r => ({
      lat: Number(r.lat),
      lng: Number(r.lng),
      label: r.riderName || String(r.riderId),
      color: r.online ? '#52c41a' : '#8c8c8c',
    })))

function toggleReplay() {
  if (replaying.value) {
    stopReplay()
    return
  }
  if (replayPoints.value.length < 2) return
  replaying.value = true
  replayIndex.value = 2
  replayTimer = setInterval(() => {
    replayIndex.value += 1
    if (replayIndex.value >= replayPoints.value.length) stopReplay()
  }, replaySpeed.value)
}

function stopReplay() {
  replaying.value = false
  if (replayTimer) {
    clearInterval(replayTimer)
    replayTimer = null
  }
}

function resetReplay() {
  stopReplay()
  replayIndex.value = replayPoints.value.length
}

// ═══ 异常预警 / 全屏地图 ═══
const alertsVisible = ref(false)
const alertsLoading = ref(false)
const alerts = ref<(TrackingAlert & { rowKey: string })[]>([])
const mapFullscreen = ref(false)

async function openAlerts() {
  alertsVisible.value = true
  alertsLoading.value = true
  try {
    const res: any = await trackingApi.alerts()
    const list: TrackingAlert[] = Array.isArray(res) ? res : []
    alerts.value = list.map((a, i) => ({ ...a, rowKey: `${a.alertType}-${a.riderId}-${i}` }))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载异常预警失败')
  } finally {
    alertsLoading.value = false
  }
}

function openMapFullscreen() {
  if (riderRows.value.length === 0) {
    message.warning('当前没有可展示的配送员位置')
    return
  }
  mapFullscreen.value = true
}

// ═══ 自动刷新（30s 兜底；实时推送通道见文档遗留项） ═══
const autoRefresh = ref(true)
const countdown = ref(30)
let autoTimer: any = null

function startAutoRefresh() {
  stopAutoRefresh()
  countdown.value = 30
  autoTimer = setInterval(() => {
    if (!autoRefresh.value) {
      countdown.value = 30
      return
    }
    countdown.value -= 1
    if (countdown.value <= 0) {
      countdown.value = 30
      refreshAll()
    }
  }, 1000)
}

function stopAutoRefresh() {
  if (autoTimer) {
    clearInterval(autoTimer)
    autoTimer = null
  }
}

// ═══ 实时推送（SSE，§3.6 下发端；断线由 30s 兜底轮询补偿） ═══
const streamConnected = ref(false)
let streamAbort: AbortController | null = null
let streamTimer: any = null
let streamDisposed = false

/** 把推送帧按 riderId 合并进当前列表（不改分页/排序，避免打断用户操作） */
function applyStreamFrame(list: any[]) {
  if (!Array.isArray(list) || list.length === 0) return
  const byId = new Map(riderRows.value.map(r => [String(r.riderId), r]))
  for (const item of list) {
    const hit = item?.riderId != null ? byId.get(String(item.riderId)) : undefined
    if (!hit) continue
    if (item.lat != null) hit.lat = item.lat
    if (item.lng != null) hit.lng = item.lng
    if (item.speed != null) hit.speed = item.speed
    if (item.direction != null) hit.direction = item.direction
    if (item.directionText != null) hit.directionText = item.directionText
    if (item.accuracy != null) hit.accuracy = item.accuracy
    if (item.address != null) hit.address = item.address
    if (item.online != null) hit.online = item.online
    if (item.lastReportTime) hit.lastReportTime = item.lastReportTime
    if (item.lastReportAgoSeconds != null) hit.lastReportAgoSeconds = item.lastReportAgoSeconds
  }
}

async function connectStream() {
  stopStream()
  if (streamDisposed) return
  const token = localStorage.getItem('token')
  if (!token) return
  const controller = new AbortController()
  streamAbort = controller
  try {
    const realtimeTenantId = localStorage.getItem('tenantId')
    const res = await fetch('/api/dms/tracking/stream', {
      headers: {
        Authorization: `Bearer ${token}`,
        // 仅在拿到真实租户时才发送（不再回落 '1'，否则与后端 TenantHeaderInterceptor 冲突而 403）
        ...(realtimeTenantId ? { tenantId: realtimeTenantId } : {}),
      },
      signal: controller.signal,
    })
    if (!res.ok || !res.body) return
    streamConnected.value = true
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const frames = buffer.split('\n\n')
      buffer = frames.pop() || ''
      for (const frame of frames) {
        const dataLine = frame.split('\n').find(line => line.startsWith('data:'))
        if (!dataLine) continue
        try {
          applyStreamFrame(JSON.parse(dataLine.slice(5).trim()))
        } catch {
          // 非 JSON 帧（如 ready）忽略
        }
      }
    }
  } catch {
    // 网络中断 / 主动断开：交由 30s 兜底轮询
  } finally {
    if (streamAbort === controller) streamAbort = null
    streamConnected.value = false
    if (!streamDisposed) streamTimer = setTimeout(connectStream, 30000)
  }
}

function stopStream() {
  if (streamTimer) {
    clearTimeout(streamTimer)
    streamTimer = null
  }
  if (streamAbort) {
    streamAbort.abort()
    streamAbort = null
  }
  streamConnected.value = false
}

// ═══ 导出（选中配送员的轨迹点） ═══
async function handleExport() {
  if (!currentRider.value?.riderId) {
    message.warning('请先选择配送员')
    return
  }
  exporting.value = true
  try {
    const blob: any = await trackingApi.export({
      riderId: currentRider.value.riderId,
      startTime: replayRange.value?.[0],
      endTime: replayRange.value?.[1],
    })
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `轨迹_${currentRider.value.riderName || currentRider.value.riderId}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '配送员', visible: true },
  { key: 'status', label: '在线状态', visible: true },
  { key: 'onlineOnly', label: '仅看在线', visible: true },
  { key: 'replayRange', label: '回放时间', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'alerts', label: '异常预警', enabled: true },
  { key: 'fullscreenMap', label: '全屏地图', enabled: true },
  { key: 'autoRefresh', label: '自动刷新(30s)', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

// ═══ 展示辅助 ═══
function fmtDateTime(val?: string | null) {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function fmtCoord(lat?: number | null, lng?: number | null) {
  if (lat == null || lng == null) return '-'
  return `${Number(lng).toFixed(6)}, ${Number(lat).toFixed(6)}`
}

function fmtNum(val?: number | null) {
  if (val == null) return '-'
  const n = Number(val)
  return Number.isNaN(n) ? '-' : String(n)
}

function fmtKm(val?: number | null) {
  if (val == null) return '-'
  return `${Number(val).toFixed(2)} km`
}

function fmtAgo(seconds?: number | null) {
  if (seconds == null) return ''
  if (seconds < 60) return `${seconds} 秒前`
  if (seconds < 3600) return `${Math.floor(seconds / 60)} 分钟前`
  return `${Math.floor(seconds / 3600)} 小时前`
}

function handleError(error: Error) {
  console.error('[实时跟踪] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  refreshAll()
  startAutoRefresh()
  connectStream()
})
onBeforeUnmount(() => {
  streamDisposed = true
  stopAutoRefresh()
  stopReplay()
  stopStream()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.picked-tip { font-size: 12px; color: #1890ff; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.stat-bar {
  display: flex;
  align-items: baseline;
  gap: 22px;
  padding: 8px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.stat-item { display: flex; align-items: baseline; gap: 6px; }
.stat-label { font-size: 12px; color: #8c8c8c; }
.stat-value { font-size: 16px; font-weight: 600; color: #262626; }
.stat-ok { color: #389e0d; }
.stat-warn { color: #d46b08; }
.stat-danger { color: #cf1322; }
.stat-muted { color: #8c8c8c; }
.online-tip { margin-left: auto; font-size: 12px; color: #8c8c8c; }

.map-card {
  flex-shrink: 0;
  padding: 8px 16px 0;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}
.map-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.map-title { font-size: 13px; color: #595959; }

.detail-tabs { padding: 0 16px; flex-shrink: 0; }
.detail-tabs :deep(.ant-tabs-nav) { margin-bottom: 4px; }

.sub-text { font-size: 12px; color: #8c8c8c; margin-left: 4px; }
.speed-abnormal { color: #cf1322; font-weight: 600; }
.auto-refresh { display: inline-flex; align-items: center; gap: 6px; }
.auto-refresh-text { font-size: 12px; color: #8c8c8c; }
.stream-state { font-size: 12px; white-space: nowrap; }
.stream-on { color: #389e0d; }
.stream-off { color: #8c8c8c; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
