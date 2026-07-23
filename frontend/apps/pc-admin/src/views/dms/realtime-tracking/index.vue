<template>
  <ErrorBoundary>
    <PageContainer title="实时跟踪">
      <a-alert
        type="info"
        show-icon
        class="tracking-alert"
        message="后端暂无轨迹分页接口（/dms/tracking/page 未实现），页面未接入地图 SDK，实时跟踪以「骑手最新位置 + 轨迹点列表」形式展示。"
      />

      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="loading"
      />

      <!-- ═══ 骑手实时位置 ═══ -->
      <div class="table-area">
        <div class="table-toolbar">
          <span class="table-title">骑手最新位置</span>
          <a-space>
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <a-button
              size="small"
              :loading="loading"
              @click="loadData"
            >
              <template #icon>
                <ReloadOutlined />
              </template>刷新
            </a-button>
          </a-space>
        </div>
        <a-table
          :columns="riderColumns"
          :data-source="riderPositions"
          :loading="loading"
          :pagination="{ pageSize: 20, showTotal: (t: number) => `共 ${t} 条` }"
          :locale="{ emptyText: '暂无骑手数据' }"
          row-key="riderId"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'online'">
              <a-tag :color="record.online ? 'green' : 'default'">
                {{ record.online ? '在线' : '离线' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'lng'">
              {{ record.lng != null ? Number(record.lng).toFixed(6) : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'lat'">
              {{ record.lat != null ? Number(record.lat).toFixed(6) : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'speed'">
              {{ record.speed != null ? `${Number(record.speed).toFixed(1)} km/h` : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'direction'">
              {{ formatDirection(record.direction) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="handleViewTrack(record as any)"
              >
                查看轨迹
              </a-button>
            </template>
          </template>
        </a-table>
      </div>

      <!-- ═══ 轨迹明细 ═══ -->
      <div class="table-area">
        <div class="table-toolbar">
          <span class="table-title">
            轨迹明细<template v-if="selectedRiderName"> — {{ selectedRiderName }}</template>
          </span>
          <a-space>
            <a-range-picker
              v-model:value="trackRange"
              show-time
              size="small"
              style="width: 340px"
            />
            <a-button
              type="primary"
              size="small"
              :disabled="!selectedRiderId"
              :loading="trackLoading"
              @click="loadTrack"
            >
              查询轨迹
            </a-button>
          </a-space>
        </div>
        <a-table
          :columns="trackColumns"
          :data-source="trackList"
          :loading="trackLoading"
          :pagination="{ pageSize: 20, showTotal: (t: number) => `共 ${t} 条` }"
          :locale="{ emptyText: selectedRiderId ? '该时间段内暂无轨迹点' : '请先在上方选择骑手' }"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'lng'">
              {{ record.lng != null ? Number(record.lng).toFixed(6) : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'lat'">
              {{ record.lat != null ? Number(record.lat).toFixed(6) : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'speed'">
              {{ record.speed != null ? `${Number(record.speed).toFixed(1)} km/h` : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'direction'">
              {{ formatDirection(record.direction) }}
            </template>
            <template v-else-if="column.dataIndex === 'source'">
              {{ SOURCE_MAP[record.source] || '-' }}
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { riderApi, type DmsRider } from '@/api/dms/rider'
import { trackingApi, type DmsTrackingRecord } from '@/api/dms/tracking'

const route = useRoute()

const SOURCE_MAP: Record<number, string> = { 1: 'APP上报', 2: '后台查询' }

/** 在线判定阈值：10 分钟内有位置上报 */
const ONLINE_THRESHOLD_MINUTES = 10

interface RiderPosition {
  riderId: number
  realName: string
  phone?: string
  online: boolean
  lng?: number
  lat?: number
  speed?: number
  direction?: number
  reportTime?: string
}

const loading = ref(false)
const lastUpdateTime = ref('')
const riderPositions = ref<RiderPosition[]>([])

const selectedRiderId = ref<number | null>(null)
const selectedRiderName = ref('')
const trackRange = ref<[Dayjs, Dayjs]>([dayjs().startOf('day'), dayjs().endOf('day')])
const trackList = ref<DmsTrackingRecord[]>([])
const trackLoading = ref(false)

// ═══ 统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const total = riderPositions.value.length
  const withPos = riderPositions.value.filter(r => r.reportTime).length
  const online = riderPositions.value.filter(r => r.online).length
  return [
    { label: '骑手总数', value: total, suffix: '人' },
    { label: '在线骑手', value: online, suffix: '人', valueStyle: { color: '#52c41a' } },
    { label: '有位置上报', value: withPos, suffix: '人' },
    { label: '无位置数据', value: total - withPos, suffix: '人', valueStyle: total - withPos > 0 ? { color: '#faad14' } : undefined }
  ]
})

// ═══ 表格列 ═══
const riderColumns: any[] = [
  { title: '骑手ID', dataIndex: 'riderId', key: 'riderId', width: 80 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 110 },
  { title: '电话', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '状态', dataIndex: 'online', key: 'online', width: 80 },
  { title: '经度', dataIndex: 'lng', key: 'lng', width: 110 },
  { title: '纬度', dataIndex: 'lat', key: 'lat', width: 110 },
  { title: '速度', dataIndex: 'speed', key: 'speed', width: 100 },
  { title: '方向', dataIndex: 'direction', key: 'direction', width: 80 },
  { title: '上报时间', dataIndex: 'reportTime', key: 'reportTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const trackColumns: any[] = [
  { title: '上报时间', dataIndex: 'reportTime', key: 'reportTime', width: 170 },
  { title: '经度', dataIndex: 'lng', key: 'lng', width: 120 },
  { title: '纬度', dataIndex: 'lat', key: 'lat', width: 120 },
  { title: '速度', dataIndex: 'speed', key: 'speed', width: 100 },
  { title: '方向', dataIndex: 'direction', key: 'direction', width: 80 },
  { title: '任务ID', dataIndex: 'taskId', key: 'taskId', width: 90 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 100 }
]

function formatDirection(direction: number | null | undefined): string {
  if (direction === null || direction === undefined) return '-'
  const dirs = ['北', '东北', '东', '东南', '南', '西南', '西', '西北']
  return dirs[Math.round(Number(direction) / 45) % 8] || '-'
}

function isOnline(reportTime?: string): boolean {
  if (!reportTime) return false
  return dayjs().diff(dayjs(reportTime), 'minute') <= ONLINE_THRESHOLD_MINUTES
}

// ═══ 加载骑手 + 最新位置 ═══
async function loadData() {
  loading.value = true
  try {
    const res: any = await riderApi.page({ pageNum: 1, pageSize: 100 })
    const riders: DmsRider[] = res?.records || res?.list || []
    // 逐个取最新位置（后端无批量接口），失败视为无位置数据
    const results = await Promise.allSettled(riders.map(r => trackingApi.latest(r.id)))
    riderPositions.value = riders.map((r, i) => {
      const settled = results[i]
      const pos = settled.status === 'fulfilled' ? (settled.value as DmsTrackingRecord | null) : null
      return {
        riderId: r.id,
        realName: r.realName,
        phone: r.phone,
        online: isOnline(pos?.reportTime),
        lng: pos?.lng != null ? Number(pos.lng) : undefined,
        lat: pos?.lat != null ? Number(pos.lat) : undefined,
        speed: pos?.speed != null ? Number(pos.speed) : undefined,
        direction: pos?.direction != null ? Number(pos.direction) : undefined,
        reportTime: pos?.reportTime
      }
    })
    lastUpdateTime.value = dayjs().format('HH:mm:ss')
  } catch (e) {
    riderPositions.value = []
    console.warn('[实时跟踪] 骑手列表获取失败', e)
  } finally {
    loading.value = false
  }
}

// ═══ 轨迹查询 ═══
function handleViewTrack(record: RiderPosition) {
  selectedRiderId.value = record.riderId
  selectedRiderName.value = record.realName
  loadTrack()
}

async function loadTrack() {
  if (!selectedRiderId.value) return
  if (!trackRange.value?.[0] || !trackRange.value?.[1]) {
    message.warning('请选择轨迹时间范围')
    return
  }
  trackLoading.value = true
  try {
    const list = await trackingApi.track({
      riderId: selectedRiderId.value,
      startTime: trackRange.value[0].format('YYYY-MM-DDTHH:mm:ss'),
      endTime: trackRange.value[1].format('YYYY-MM-DDTHH:mm:ss')
    })
    trackList.value = Array.isArray(list) ? list : []
  } catch (e) {
    trackList.value = []
    console.warn('[实时跟踪] 轨迹获取失败', e)
  } finally {
    trackLoading.value = false
  }
}

/** 如果从其他页面导航过来携带 riderId 查询参数，自动选中并加载轨迹 */
function autoSelectRider() {
  const riderId = route.query.riderId
  if (riderId) {
    const id = Number(riderId)
    const found = riderPositions.value.find(r => r.riderId === id)
    if (found) {
      handleViewTrack(found)
    } else {
      // 骑手数据还未加载，等待后重试
      const unwatch = watch(riderPositions, (list) => {
        const f = list.find(r => r.riderId === id)
        if (f) { handleViewTrack(f); unwatch() }
      })
      setTimeout(() => unwatch(), 10000) // 10秒超时
    }
  }
}

onMounted(async () => {
  await loadData()
  autoSelectRider()
})
</script>

<style scoped>
.tracking-alert {
  margin-bottom: 16px;
}

.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.update-time {
  font-size: 12px;
  color: #999;
}
</style>
