<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        配送跟踪（配送 → 配送跟踪 → 配送跟踪，菜单 80870 `dms:tracking`）
        · 定位：轨迹点【明细台账】（可查/可导出/可对账）；与《实时跟踪》（在线盯盘+回放）分工不同
        · 骨架：CategoryListLayout（左侧配送员快速过滤面板：全部 / 在线 / 离线 / 今日有上报）
                + BillTableList（表头序号齿轮列配置）+ PageConfigPanel
        · 地图能力复用 components/business/RouteMapCanvas（不在本页另造一套）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="配送员"
        :category-editable="false"
        :category-tree-data="panelTree"
        :selected-category-id="panelKey"
        :show-table-footer="true"
        @category-select="handlePanelSelect"
      >
        <!-- ═══ 工具栏左侧：地图查看 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('map')"
              size="small"
              :disabled="selectedRows.length === 0"
              @click="openMapDrawer(selectedRows)"
            >
              <EnvironmentOutlined /> 在地图中查看
            </a-button>
            <span
              v-if="selectedRows.length > 0"
              class="sel-tip"
            >已选 {{ selectedRows.length }} 点</span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
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
                v-if="isQueryVisible('riderId')"
                class="search-item"
              >
                <span class="search-label">配送员</span>
                <a-select
                  v-model:value="searchForm.riderId"
                  placeholder="全部"
                  size="small"
                  style="width: 150px"
                  show-search
                  option-filter-prop="label"
                  allow-clear
                  :options="riderOptions"
                  :loading="riderLoading"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('taskNo')"
                class="search-item"
              >
                <span class="search-label">任务编号</span>
                <a-input
                  v-model:value="searchForm.taskNo"
                  placeholder="请输入任务编号"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('timeRange')"
                class="search-item"
              >
                <span class="search-label">上报时间</span>
                <a-range-picker
                  v-model:value="timeRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 330px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('sources')"
                class="search-item"
              >
                <span class="search-label">来源</span>
                <a-select
                  v-model:value="searchForm.sources"
                  mode="multiple"
                  placeholder="全部"
                  size="small"
                  style="min-width: 150px"
                  max-tag-count="responsive"
                  :options="SOURCE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('minSpeed')"
                class="search-item"
              >
                <span class="search-label">速度≥</span>
                <a-input-number
                  v-model:value="searchForm.minSpeed"
                  :min="0"
                  :precision="0"
                  placeholder="km/h"
                  size="small"
                  style="width: 110px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('onlyWithTask')"
                class="search-item"
              >
                <a-checkbox
                  v-model:checked="searchForm.onlyWithTask"
                  @change="handleSearch"
                >
                  仅显示有任务关联的点
                </a-checkbox>
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
            </div>
            <!-- 快捷时间（对齐 §3.3：今日 / 昨日 / 近 7 日 / 自定义） -->
            <div class="search-row quick-row">
              <span class="search-label">快捷时间</span>
              <a-space :size="6">
                <a-button
                  size="small"
                  @click="applyQuickRange('today')"
                >
                  今日
                </a-button>
                <a-button
                  size="small"
                  @click="applyQuickRange('yesterday')"
                >
                  昨日
                </a-button>
                <a-button
                  size="small"
                  @click="applyQuickRange('last7')"
                >
                  近 7 日
                </a-button>
              </a-space>
              <span
                v-if="mileageSummary"
                class="mileage-tip"
              >{{ mileageSummary }}</span>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              storage-key="dms-tracking-columns"
              global-config-key="dms-tracking-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
            >
              <template #riderCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.riderName || record.riderId || '-' }}
                  <span
                    v-if="record.riderPhone"
                    class="sub-text"
                  >{{ record.riderPhone }}</span>
                </span>
              </template>

              <template #taskCell="{ record }">
                <span v-if="!record.__ghost">{{ record.taskNo || (record.taskId ? `#${record.taskId}` : '-') }}</span>
              </template>

              <template #lngCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtCoord(record.lng) }}</span>
              </template>

              <template #latCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtCoord(record.lat) }}</span>
              </template>

              <template #speedCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="record.speedAbnormal ? 'speed-abnormal' : ''"
                >
                  {{ fmtNum(record.speed) }}
                  <span v-if="record.speedAbnormal">· 异常</span>
                </span>
              </template>

              <template #directionCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.directionText || '-' }}
                  <span
                    v-if="record.direction != null"
                    class="sub-text"
                  >{{ fmtNum(record.direction) }}°</span>
                </span>
              </template>

              <template #sourceCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="SOURCE_MAP[record.source ?? 0]?.color || 'default'"
                >
                  {{ record.sourceText || SOURCE_MAP[record.source ?? 0]?.label || '未知来源' }}
                </a-tag>
              </template>

              <template #segmentCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.segmentMeters != null ? `${Math.round(Number(record.segmentMeters))} 米` : '-' }}
                </span>
              </template>

              <template #timeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtDateTime(record.reportTime) }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openMapDrawer([record])"
                  >
                    地图查看
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
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

      <!-- ═══ 轨迹地图抽屉（复用 RouteMapCanvas：折线 + 起终点 + 逐点回放） ═══ -->
      <a-drawer
        v-model:open="mapVisible"
        title="轨迹地图"
        :width="860"
        destroy-on-close
      >
        <div class="map-toolbar">
          <span class="map-title">
            {{ mapTitle }}
          </span>
          <a-space :size="8">
            <a-button
              size="small"
              :disabled="trackPoints.length < 2"
              @click="toggleReplay"
            >
              {{ replaying ? '暂停回放' : '轨迹回放' }}
            </a-button>
            <span class="sub-text">{{ replayIndex }} / {{ trackPoints.length }}</span>
          </a-space>
        </div>
        <RouteMapCanvas
          :markers="mapMarkers"
          :polyline="mapPolyline"
          :height="'460px'"
          :interactive="true"
          title="轨迹"
          empty-text="该范围内没有轨迹点"
        />
        <div class="map-foot">
          <span>里程（直线累计）：{{ mileageKm }} km</span>
          <span>点数：{{ trackPoints.length }}（已抽稀）</span>
        </div>
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-tracking-page-config"
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
  ReloadOutlined, DownloadOutlined, SettingOutlined, EnvironmentOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import RouteMapCanvas from '@/components/business/RouteMapCanvas/index.vue'
import request from '@/utils/request'
import { trackingApi, type TrackingVO } from '@/api/dms/tracking'

defineOptions({ name: 'DmsTracking' })

/** 来源字典（与后端 TrackingSourceEnum 同一套取值，§3.5.3） */
const SOURCE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: 'APP上报', color: 'blue' },
  2: { label: '后台补录', color: 'orange' },
  3: { label: '渠道回传', color: 'purple' },
}
const SOURCE_OPTIONS = [
  { label: 'APP上报', value: 1 }, { label: '后台补录', value: 2 }, { label: '渠道回传', value: 3 },
]

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<TrackingVO[]>([])
const selectedRows = ref<TrackingVO[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })
const mileageSummary = ref('')

const searchForm = reactive({
  riderId: undefined as number | undefined,
  taskNo: '',
  sources: [] as number[],
  minSpeed: undefined as number | undefined,
  onlyWithTask: false,
})
const timeRange = ref<[string, string] | undefined>()

/** 左侧配送员快速过滤面板（§3.1） */
type PanelKey = 'ALL' | 'ONLINE' | 'OFFLINE' | 'TODAY'
const panelKey = ref<PanelKey>('ALL')
// ⚠️ CategoryListLayout 的 a-tree 用 field-names={ key:'id', title:'categoryName' }，
//    节点必须带 id / categoryName，否则面板渲染成「暂无分类」
const panelTree = computed(() => ([
  { id: 'ALL', categoryName: '全部配送员' },
  { id: 'ONLINE', categoryName: '在线' },
  { id: 'OFFLINE', categoryName: '离线' },
  { id: 'TODAY', categoryName: '今日有上报' },
]))

function handlePanelSelect(keys: (string | number)[]) {
  panelKey.value = (keys?.[0] as PanelKey) || 'ALL'
  pagination.current = 1
  refreshAll()
}

// ═══ 列（★=默认显示） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { title: '上报时间', key: 'reportTime', type: 'slot', slotName: 'timeCell', width: 160, sortable: true },
  { title: '配送员', key: 'riderName', type: 'slot', slotName: 'riderCell', width: 150 },
  { title: '任务编号', key: 'taskNo', type: 'slot', slotName: 'taskCell', width: 150 },
  { title: '经度', key: 'lng', type: 'slot', slotName: 'lngCell', width: 120 },
  { title: '纬度', key: 'lat', type: 'slot', slotName: 'latCell', width: 120 },
  { title: '速度(km/h)', key: 'speed', type: 'slot', slotName: 'speedCell', width: 120, sortable: true },
  { title: '方向', key: 'directionText', type: 'slot', slotName: 'directionCell', width: 110 },
  { title: '来源', key: 'source', type: 'slot', slotName: 'sourceCell', width: 110 },
  { title: '客户', key: 'customerName', width: 140, defaultHidden: true },
  { title: '段间里程(米)', key: 'segmentMeters', type: 'slot', slotName: 'segmentCell', width: 120, defaultHidden: true },
  { title: '定位精度(米)', key: 'accuracy', width: 120, defaultHidden: true },
  { title: '地址', key: 'address', width: 200, defaultHidden: true },
]

// ═══ 配送员下拉（选择器，禁止手输 ID） ═══
const riderLoading = ref(false)
const riderOptions = ref<{ label: string; value: number }[]>([])
async function loadRiderOptions() {
  riderLoading.value = true
  try {
    const res: any = await request.get('/dms/rider/options')
    const list: any[] = Array.isArray(res) ? res : res?.records || res?.data || []
    riderOptions.value = list.map((r: any) => ({ label: r.realName || r.riderNo || `#${r.id}`, value: r.id }))
  } catch (error) {
    console.warn('[配送跟踪] 加载配送员下拉失败', error)
    riderOptions.value = []
  } finally {
    riderLoading.value = false
  }
}

// ═══ 查询参数 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.riderId != null) params.riderId = searchForm.riderId
  if (searchForm.taskNo) params.taskNo = searchForm.taskNo.trim()
  if (searchForm.sources?.length) params.sources = searchForm.sources.join(',')
  if (searchForm.minSpeed != null) params.minSpeed = searchForm.minSpeed
  if (searchForm.onlyWithTask) params.onlyWithTask = true
  if (timeRange.value?.length === 2) {
    params.startTime = timeRange.value[0]
    params.endTime = timeRange.value[1]
  }
  // 左面板过滤
  // ⚠️「在线」是位置上报心跳口径（与《配送员管理》《实时跟踪》一致），不是人工状态 status
  if (panelKey.value === 'ONLINE') params.online = true
  if (panelKey.value === 'OFFLINE') params.online = false
  if (panelKey.value === 'TODAY') params.activeToday = true
  if (sortState.field) {
    params.sortField = sortState.field
    params.sortOrder = sortState.order
  }
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await trackingApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[配送跟踪] 加载轨迹台账失败', error)
    message.error(error?.response?.data?.message || '加载轨迹台账失败')
  } finally {
    loading.value = false
  }
}

/** 里程摘要（§3.4 /mileage：按配送员聚合取合计） */
async function fetchMileageSummary() {
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await trackingApi.mileage({ ...params, groupBy: 'rider' })
    const rows: any[] = Array.isArray(res) ? res : []
    const km = rows.reduce((sum, r) => sum + Number(r.mileageKm || 0), 0)
    const points = rows.reduce((sum, r) => sum + Number(r.pointCount || 0), 0)
    mileageSummary.value = rows.length
      ? `当前条件：${rows.length} 名配送员 / ${points} 个轨迹点 / 合计里程 ${km.toFixed(2)} km`
      : ''
  } catch (error) {
    console.warn('[配送跟踪] 里程聚合失败', error)
    mileageSummary.value = ''
  }
}

function refreshAll() {
  fetchList()
  fetchMileageSummary()
}

function handleSearch() {
  pagination.current = 1
  refreshAll()
}

function handleReset() {
  searchForm.riderId = undefined
  searchForm.taskNo = ''
  searchForm.sources = []
  searchForm.minSpeed = undefined
  searchForm.onlyWithTask = false
  timeRange.value = undefined
  panelKey.value = 'ALL'
  sortState.field = undefined
  sortState.order = undefined
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleSortChange(key: string | null, order: string | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

function handleSelectionChange(rows: TrackingVO[]) {
  selectedRows.value = (rows || []).filter(r => !(r as any).__ghost)
}

/** 快捷时间：今日 / 昨日 / 近 7 日 */
function applyQuickRange(kind: 'today' | 'yesterday' | 'last7') {
  const pad = (n: number) => String(n).padStart(2, '0')
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const today = new Date()
  if (kind === 'today') {
    timeRange.value = [`${fmt(today)} 00:00:00`, `${fmt(today)} 23:59:59`]
  } else if (kind === 'yesterday') {
    const y = new Date(today.getTime() - 86400000)
    timeRange.value = [`${fmt(y)} 00:00:00`, `${fmt(y)} 23:59:59`]
  } else {
    const from = new Date(today.getTime() - 6 * 86400000)
    timeRange.value = [`${fmt(from)} 00:00:00`, `${fmt(today)} 23:59:59`]
  }
  handleSearch()
}

// ═══ 地图抽屉（复用 RouteMapCanvas） ═══
const mapVisible = ref(false)
const trackPoints = ref<TrackingVO[]>([])
const mapTitle = ref('')
const replaying = ref(false)
const replayIndex = ref(0)
let replayTimer: any = null

/** 回放时按索引逐步揭示，否则整条折线 */
const revealCount = computed(() => (replayIndex.value > 0 ? replayIndex.value : trackPoints.value.length))

const mapPolyline = computed(() =>
  trackPoints.value
    .slice(0, Math.max(revealCount.value, 2))
    .filter(p => p.lat != null && p.lng != null)
    .map(p => ({ lat: Number(p.lat), lng: Number(p.lng) })))

const mapMarkers = computed(() => {
  const pts = trackPoints.value.filter(p => p.lat != null && p.lng != null)
  if (pts.length === 0) return []
  const first = pts[0]
  const last = pts[pts.length - 1]
  const markers: any[] = [
    { lat: Number(first.lat), lng: Number(first.lng), label: '起点', color: '#52c41a' },
  ]
  if (pts.length > 1) {
    markers.push({ lat: Number(last.lat), lng: Number(last.lng), label: '终点', color: '#f5222d' })
  }
  return markers
})

const mileageKm = computed(() => {
  const pts = trackPoints.value.filter(p => p.lat != null && p.lng != null)
  let meters = 0
  for (let i = 1; i < pts.length; i++) {
    meters += haversine(Number(pts[i - 1].lat), Number(pts[i - 1].lng), Number(pts[i].lat), Number(pts[i].lng))
  }
  return (meters / 1000).toFixed(2)
})

/** 地图查看：优先按任务取轨迹（更完整），否则按配送员 + 当前时间范围 */
async function openMapDrawer(rows: TrackingVO[]) {
  const points = (rows || []).filter(r => !(r as any).__ghost)
  if (points.length === 0) {
    message.warning('请先勾选要在地图中查看的轨迹点')
    return
  }
  stopReplay()
  replayIndex.value = 0
  mapVisible.value = true
  const riderId = points[0].riderId
  const taskId = points.find(p => p.taskId)?.taskId
  mapTitle.value = `${points[0].riderName || riderId || '-'}${taskId ? ` · 任务 ${points[0].taskNo || taskId}` : ''}`
  try {
    const params = buildParams()
    const res: any = taskId
      ? await trackingApi.trackByTaskVO(taskId, 500)
      : await trackingApi.trackVO({
        // ⚠️ 雪花ID 必须原样传字符串（Number() 会丢精度导致查不到轨迹）
        riderId,
        startTime: params.startTime,
        endTime: params.endTime,
        maxPoints: 500,
      })
    trackPoints.value = Array.isArray(res) ? res : []
    if (trackPoints.value.length === 0) {
      message.info('该范围内没有可绘制的轨迹点')
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载轨迹失败')
    trackPoints.value = []
  }
}

/** 轨迹回放：按点逐段揭示折线 */
function toggleReplay() {
  if (replaying.value) {
    stopReplay()
    return
  }
  if (trackPoints.value.length < 2) return
  replaying.value = true
  replayIndex.value = 2
  replayTimer = setInterval(() => {
    replayIndex.value += 1
    if (replayIndex.value >= trackPoints.value.length) {
      stopReplay()
    }
  }, 300)
}

function stopReplay() {
  replaying.value = false
  if (replayTimer) {
    clearInterval(replayTimer)
    replayTimer = null
  }
}

onBeforeUnmount(() => stopReplay())

// ═══ 导出 ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const blob: any = await trackingApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `轨迹台账_${new Date().toISOString().slice(0, 10)}.xlsx`
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
  { key: 'riderId', label: '配送员', visible: true },
  { key: 'taskNo', label: '任务编号', visible: true },
  { key: 'timeRange', label: '上报时间', visible: true },
  { key: 'sources', label: '来源', visible: true },
  { key: 'minSpeed', label: '速度≥阈值', visible: true },
  { key: 'onlyWithTask', label: '仅显示有任务关联的点', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'map', label: '在地图中查看', enabled: true },
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

function fmtCoord(val?: number | null) {
  if (val == null) return '-'
  return Number(val).toFixed(6)
}

function fmtNum(val?: number | null) {
  if (val == null) return '-'
  const n = Number(val)
  return Number.isNaN(n) ? '-' : String(n)
}

function haversine(lat1: number, lng1: number, lat2: number, lng2: number) {
  const rad = (d: number) => (d * Math.PI) / 180
  const dLat = rad(lat2 - lat1)
  const dLng = rad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function handleError(error: Error) {
  console.error('[配送跟踪] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  refreshAll()
  loadRiderOptions()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.quick-row { margin-top: 8px; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.sel-tip { font-size: 12px; color: #1890ff; }
.mileage-tip { font-size: 12px; color: #389e0d; margin-left: 12px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.sub-text { font-size: 12px; color: #8c8c8c; margin-left: 4px; }
.speed-abnormal { color: #cf1322; font-weight: 600; }

.map-toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.map-title { font-size: 14px; font-weight: 600; color: #262626; }
.map-foot {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
  font-size: 12px;
  color: #8c8c8c;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
