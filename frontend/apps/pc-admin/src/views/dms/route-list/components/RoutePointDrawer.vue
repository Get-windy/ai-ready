<template>
  <a-drawer
    v-model:open="visible"
    :width="1100"
    :title="`配送路线单 ${detail?.routeCode || ''}`"
    :body-style="{ paddingBottom: '16px' }"
    @close="handleClose"
  >
    <div class="drawer-toolbar">
      <a-space :size="8">
        <a-tag :color="statusColor(detail?.status)">
          {{ detail?.statusText || '-' }}
        </a-tag>
        <span class="progress-text">
          完成进度 {{ detail?.completedPoints ?? 0 }} / {{ detail?.totalPoints ?? 0 }}
          （失败 {{ detail?.failedPoints ?? 0 }}）
        </span>
      </a-space>
      <a-space :size="8">
        <a-button
          v-if="canOperate"
          size="small"
          :loading="planning"
          @click="handlePlanOrder"
        >
          <AimOutlined /> 路线规划
        </a-button>
        <a-button
          v-if="canOperate"
          size="small"
          :loading="etaLoading"
          @click="handleEta"
        >
          <ClockCircleOutlined /> ETA 预估
        </a-button>
        <a-button
          v-if="canOperate"
          size="small"
          :loading="notifying"
          @click="handleNotifyEta"
        >
          <BellOutlined /> 推送客户
        </a-button>
        <a-button
          v-if="canOperate"
          size="small"
          @click="collectVisible = true"
        >
          <PlusOutlined /> 添加配送点位
        </a-button>
        <a-button
          size="small"
          @click="openMap"
        >
          <EnvironmentOutlined /> 地图查看
        </a-button>
        <a-button
          size="small"
          @click="openNotifyLedger"
        >
          <BellOutlined /> 通知台账
        </a-button>
        <a-button
          size="small"
          :loading="loading"
          @click="loadDetail"
        >
          <ReloadOutlined /> 刷新
        </a-button>
        <a-button
          size="small"
          @click="handlePrint"
        >
          <PrinterOutlined /> 打印路线单
        </a-button>
      </a-space>
    </div>

    <a-descriptions
      size="small"
      bordered
      :column="4"
      class="route-desc"
    >
      <a-descriptions-item label="路线编号">{{ detail?.routeCode || '-' }}</a-descriptions-item>
      <a-descriptions-item label="配送线路">{{ detail?.routeName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="线路类型">{{ detail?.routeTypeText || '-' }}</a-descriptions-item>
      <a-descriptions-item label="配送员">{{ detail?.deliveryPersonName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="车辆">{{ detail?.vehicleNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="计划日期">{{ detail?.planDate || '-' }}</a-descriptions-item>
      <a-descriptions-item label="总里程">{{ detail?.totalDistance != null ? `${detail.totalDistance} km` : '-' }}</a-descriptions-item>
      <a-descriptions-item label="预计时长">{{ detail?.totalDuration != null ? `${detail.totalDuration} 分钟` : '-' }}</a-descriptions-item>
      <a-descriptions-item label="实际时长">{{ detail?.actualDuration != null ? `${detail.actualDuration} 分钟` : '-' }}</a-descriptions-item>
      <a-descriptions-item label="起点">{{ detail?.startPoint || '-' }}</a-descriptions-item>
      <a-descriptions-item label="终点">{{ detail?.endPoint || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建人">{{ detail?.createByName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="开始时间">{{ fmt(detail?.startTime) }}</a-descriptions-item>
      <a-descriptions-item label="完成时间">{{ fmt(detail?.completeTime) }}</a-descriptions-item>
      <a-descriptions-item label="取消时间">{{ fmt(detail?.cancelTime) }}</a-descriptions-item>
      <a-descriptions-item label="取消原因">{{ detail?.cancelReason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="4">{{ detail?.remark || '-' }}</a-descriptions-item>
    </a-descriptions>

    <div class="points-title">点位明细（{{ points.length }}）</div>
    <a-table
      :columns="pointColumns"
      :data-source="points"
      :pagination="false"
      :loading="loading"
      size="small"
      row-key="pointId"
      bordered
      :scroll="{ x: 1400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="pointStatusColor(record.status)">
            {{ POINT_STATUS_MAP[record.status] || record.status || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'arriveTime'">
          {{ fmt(record.arriveTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'signTime'">
          {{ fmt(record.signTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'etaTime'">
          <span>{{ fmt(record.etaTime) }}</span>
          <a-tag
            v-if="record.expedited === 1"
            color="red"
            class="expedited-tag"
          >
            催单
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space :size="0">
            <a-button
              v-if="canOperate && canSign(record)"
              type="link"
              size="small"
              @click="openExpedite(record)"
            >
              提前
            </a-button>
            <a-button
              v-if="canSign(record)"
              type="link"
              size="small"
              @click="openSign(record, 'ARRIVED')"
            >
              到达
            </a-button>
            <a-button
              v-if="canSign(record)"
              type="link"
              size="small"
              @click="openSign(record, 'DELIVERED')"
            >
              送达
            </a-button>
            <a-button
              v-if="canSign(record)"
              type="link"
              size="small"
              danger
              @click="openSign(record, 'FAILED')"
            >
              失败
            </a-button>
            <span v-if="!canSign(record)" class="done-text">已处理</span>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 点位签收 -->
    <a-modal
      v-model:open="signVisible"
      :title="signTitle"
      :width="460"
      :confirm-loading="signing"
      ok-text="确认"
      cancel-text="取消"
      @ok="submitSign"
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }" size="small">
        <a-form-item label="点位">
          <span>{{ signTarget?.address || '-' }}</span>
        </a-form-item>
        <a-form-item v-if="signMode === 'DELIVERED'" label="签收人" required>
          <a-input v-model:value="signForm.signee" placeholder="请输入签收人" :maxlength="100" />
        </a-form-item>
        <a-form-item v-if="signMode === 'FAILED'" label="失败原因" required>
          <a-textarea v-model:value="signForm.failReason" placeholder="请输入配送失败原因" :rows="2" :maxlength="500" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="signForm.remark" placeholder="选填" :maxlength="500" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 催单：提前到指定序号 -->
    <a-modal
      v-model:open="expediteVisible"
      title="催单：提前配送"
      :width="460"
      :confirm-loading="expediting"
      ok-text="确认调整"
      cancel-text="取消"
      @ok="submitExpedite"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 17 }" size="small">
        <a-form-item label="客户">
          <span>{{ expediteTarget?.customerName || expediteTarget?.address || '-' }}</span>
        </a-form-item>
        <a-form-item label="当前序号">
          <span>第 {{ expediteTarget?.pointOrder }} 位</span>
        </a-form-item>
        <a-form-item label="提前到" required>
          <a-input-number
            v-model:value="expediteForm.targetSeq"
            :min="1"
            :max="points.length || 1"
            :precision="0"
            style="width: 120px"
          />
          <span class="hint">位（其余点位顺序顺延）</span>
        </a-form-item>
        <a-form-item label="后续重排">
          <a-switch v-model:checked="expediteForm.replanRest" />
          <span class="hint">开启后由地图能力重排该点位之后的剩余点位</span>
        </a-form-item>
        <a-form-item label="催单原因">
          <a-input v-model:value="expediteForm.reason" placeholder="如：客户催单" :maxlength="200" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 地图查看（复用《路线规划》的矢量地图组件，不自建地图） -->
    <a-modal
      v-model:open="mapVisible"
      title="路线地图查看"
      :width="900"
      :footer="null"
      :mask-closable="false"
    >
      <RouteMapCanvas
        v-if="mapVisible"
        title="配送点位（按访问顺序连线）"
        height="520px"
        :markers="mapMarkers"
        :polyline="mapPolyline"
        :circle="mapCircle"
        :polygon="mapPolygon"
        :interactive="false"
        empty-text="点位未维护经纬度，无法落图（可在客户配送坐标或点位坐标中补录）"
      />
    </a-modal>

    <!-- ETA 通知台账（通道未接入：可人工标记已发送/失败/作废） -->
    <a-modal
      v-model:open="ledgerVisible"
      title="ETA 通知台账"
      :width="1000"
      :footer="null"
      :mask-closable="false"
    >
      <a-alert
        type="warning"
        show-icon
        class="ledger-tip"
        message="短信发送由系统消息中心统一发送（通道见 sms.* 配置）：未配置通道时消息保持待发送并按策略重试；也可「补投递到消息中心」后在线下发送，用「标记已发送」回写状态。"
      />
      <a-space :size="8" class="ledger-bar">
        <a-select
          v-model:value="ledgerQuery.status"
          size="small"
          style="width: 140px"
          allow-clear
          placeholder="全部状态"
          :options="LEDGER_STATUS_OPTIONS"
          @change="loadLedger"
        />
        <a-button size="small" :loading="ledgerLoading" @click="loadLedger">刷新</a-button>
        <a-button
          size="small"
          :loading="dispatching"
          @click="handleDispatch"
        >
          补投递到消息中心
        </a-button>
        <a-button
          size="small"
          :disabled="ledgerSelected.length === 0"
          @click="handleLedgerSend"
        >
          检查通道状态
        </a-button>
        <a-button
          size="small"
          :disabled="ledgerSelected.length === 0"
          @click="handleLedgerMark('SENT')"
        >
          标记已发送
        </a-button>
        <a-button
          size="small"
          :disabled="ledgerSelected.length === 0"
          @click="handleLedgerMark('CANCELLED')"
        >
          作废
        </a-button>
      </a-space>
      <a-table
        :columns="ledgerColumns"
        :data-source="ledgerRows"
        :pagination="{ pageSize: 8, size: 'small' }"
        :loading="ledgerLoading"
        size="small"
        row-key="id"
        bordered
        :row-selection="{ selectedRowKeys: ledgerKeys, onChange: onLedgerSelect }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="LEDGER_COLOR[record.status] || 'default'">
              {{ LEDGER_TEXT[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'channelStatus'">
            <a-tag :color="MSG_STATUS_COLOR[record.channelStatus ?? -1] || 'default'">
              {{ MSG_STATUS_TEXT[record.channelStatus ?? -1] || '未投递' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'content'">
            <span class="ledger-content">{{ record.content }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 手动添加配送点位（不受围栏限制） -->
    <RouteCollectModal
      v-model:open="collectVisible"
      :route-id="props.routeId"
      @success="onCollected"
    />
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  AimOutlined,
  ClockCircleOutlined,
  BellOutlined,
  PlusOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons-vue'
import { deliveryRouteApi, type DeliveryRoute, type DeliveryRoutePoint } from '@/api/dms/route'
import RouteCollectModal from './RouteCollectModal.vue'
import RouteMapCanvas from '@/components/business/RouteMapCanvas/index.vue'
import { geoFenceApi } from '@/api/dms/route'

const props = defineProps<{ open: boolean; routeId?: number | null }>()
const emit = defineEmits<{
  'update:open': [v: boolean]
  changed: []
  /**
   * 请求宿主打印当前这条路线。
   *
   * 抽屉是共享组件、没有业务上下文，**不自带打印**（与 md/components/PartnerListPage 同一规矩）：
   * 只把「要打印的主键」交出去，由宿主的 handlePrint 走 `<PrintDialog page-code="dms-route-list">`
   * ——后端装配器（DeliveryRoutePrintDataProvider）已按路线带出主档 + 全部点位。
   * 传 { id } 是为了对上宿主 handlePrint(record) 的入参形状。
   */
  print: [record: { id: number }]
}>()

const visible = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const loading = ref(false)
const detail = ref<DeliveryRoute | null>(null)
const points = ref<DeliveryRoutePoint[]>([])

const POINT_STATUS_MAP: Record<string, string> = {
  PENDING: '待配送',
  IN_ROUTE: '在途中',
  ARRIVED: '已到达',
  DELIVERED: '已送达',
  FAILED: '配送失败',
  SKIPPED: '已跳过',
}

const STATUS_COLOR: Record<string, string> = {
  PLANNING: 'default',
  READY: 'orange',
  IN_PROGRESS: 'processing',
  COMPLETED: 'green',
  CANCELLED: 'red',
}

const POINT_COLOR: Record<string, string> = {
  PENDING: 'default',
  IN_ROUTE: 'processing',
  ARRIVED: 'blue',
  DELIVERED: 'green',
  FAILED: 'red',
  SKIPPED: 'default',
}

const pointColumns: any[] = [
  { title: '序号', dataIndex: 'pointOrder', width: 60, align: 'center' },
  { title: '状态', dataIndex: 'status', width: 90, align: 'center' },
  { title: '客户名称', dataIndex: 'customerName', width: 140 },
  { title: '联系电话', dataIndex: 'customerPhone', width: 130 },
  { title: '来源单号', dataIndex: 'orderNo', width: 150 },
  { title: '地址', dataIndex: 'address', width: 260 },
  { title: '纬度', dataIndex: 'latitude', width: 120 },
  { title: '经度', dataIndex: 'longitude', width: 120 },
  { title: '到达时间', dataIndex: 'arriveTime', width: 160 },
  { title: '预估到达(ETA)', dataIndex: 'etaTime', width: 175 },
  { title: '签收人', dataIndex: 'signee', width: 100 },
  { title: '签收时间', dataIndex: 'signTime', width: 160 },
  { title: '失败原因', dataIndex: 'failReason', width: 160 },
  { title: '备注', dataIndex: 'remark', width: 140 },
  { title: '操作', dataIndex: 'action', width: 230, fixed: 'right' },
]

function statusColor(status?: string) {
  return STATUS_COLOR[status || ''] || 'default'
}

function pointStatusColor(status?: string) {
  return POINT_COLOR[status || ''] || 'default'
}

function fmt(val?: string | null): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function canSign(record: DeliveryRoutePoint): boolean {
  return record.status !== 'DELIVERED' && record.status !== 'FAILED' && record.status !== 'SKIPPED'
}

watch(
  () => props.open,
  (open) => {
    if (open && props.routeId) loadDetail()
  },
)

async function loadDetail() {
  if (!props.routeId) return
  loading.value = true
  try {
    const res: any = await deliveryRouteApi.detail(props.routeId)
    detail.value = res || null
    points.value = res?.points || []
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载点位明细失败')
  } finally {
    loading.value = false
  }
}

function handleClose() {
  detail.value = null
  points.value = []
}

// ── 路线规划 / ETA / 推送 ───────────────────────────────
const planning = ref(false)
const etaLoading = ref(false)
const notifying = ref(false)
const collectVisible = ref(false)

/** 仅「规划中/待出发/配送中」可做规划、催单、推送 */
const canOperate = computed(() => {
  const s = detail.value?.status
  return s === 'PLANNING' || s === 'READY' || s === 'IN_PROGRESS'
})

async function handlePlanOrder() {
  if (!props.routeId) return
  planning.value = true
  try {
    const res: any = await deliveryRouteApi.planOrder(props.routeId)
    message.success(res?.message || '已按地图能力完成规划')
    await loadDetail()
    emit('changed')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '路线规划失败（请确认点位已维护坐标）')
  } finally {
    planning.value = false
  }
}

async function handleEta() {
  if (!props.routeId) return
  etaLoading.value = true
  try {
    const res: any = await deliveryRouteApi.eta(props.routeId)
    await loadDetail()
    const tip = res?.degraded
      ? `已按直线距离降级估算 ${res?.points?.length ?? 0} 个点位 ETA`
      : `已按地图能力预估 ${res?.points?.length ?? 0} 个点位 ETA`
    message.success(tip)
  } catch (e: any) {
    message.error(e?.response?.data?.message || 'ETA 预估失败')
  } finally {
    etaLoading.value = false
  }
}

function handleNotifyEta() {
  if (!props.routeId) return
  Modal.confirm({
    title: '推送预计到达时间给客户',
    content: '将按当前 ETA 生成客户通知。短信/推送通道尚未接入，记录会停留在「待发送」状态。',
    okText: '生成通知',
    onOk: async () => {
      notifying.value = true
      try {
        const res: any = await deliveryRouteApi.notifyEta(props.routeId!, { channel: 'SMS' })
        message.success(res?.message || '已生成通知')
      } catch (e: any) {
        message.error(e?.response?.data?.message || '生成通知失败')
      } finally {
        notifying.value = false
      }
    },
  })
}

function onCollected() {
  loadDetail()
  emit('changed')
}

// ── 催单：提前 ──────────────────────────────────────────
const expediteVisible = ref(false)
const expediting = ref(false)
const expediteTarget = ref<DeliveryRoutePoint | null>(null)
const expediteForm = reactive({ targetSeq: 1, replanRest: false, reason: '' })

function openExpedite(record: DeliveryRoutePoint) {
  expediteTarget.value = record
  expediteForm.targetSeq = 1
  expediteForm.replanRest = false
  expediteForm.reason = '客户催单'
  expediteVisible.value = true
}

async function submitExpedite() {
  if (!props.routeId || !expediteTarget.value?.pointId) return
  if (!expediteForm.targetSeq || expediteForm.targetSeq < 1) {
    message.warning('请输入有效的目标序号')
    return
  }
  expediting.value = true
  try {
    const res: any = await deliveryRouteApi.expeditePoint(props.routeId, expediteTarget.value.pointId, {
      targetSeq: expediteForm.targetSeq,
      replanRest: expediteForm.replanRest,
      reason: expediteForm.reason || undefined,
    })
    message.success(res?.message || '已调整顺序')
    expediteVisible.value = false
    await loadDetail()
    emit('changed')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '调整失败')
  } finally {
    expediting.value = false
  }
}

// ── 点位签收 ────────────────────────────────────────────
const signVisible = ref(false)
const signing = ref(false)
const signMode = ref<'ARRIVED' | 'DELIVERED' | 'FAILED'>('DELIVERED')
const signTarget = ref<DeliveryRoutePoint | null>(null)
const signForm = reactive({ signee: '', failReason: '', remark: '' })

const signTitle = computed(() => {
  if (signMode.value === 'ARRIVED') return '点位到达'
  if (signMode.value === 'FAILED') return '配送失败'
  return '点位签收'
})

function openSign(record: DeliveryRoutePoint, mode: 'ARRIVED' | 'DELIVERED' | 'FAILED') {
  signMode.value = mode
  signTarget.value = record
  signForm.signee = record.customerName || ''
  signForm.failReason = ''
  signForm.remark = record.remark || ''
  signVisible.value = true
}

async function submitSign() {
  if (!signTarget.value?.pointId || !props.routeId) return
  if (signMode.value === 'DELIVERED' && !signForm.signee.trim()) {
    message.warning('请输入签收人')
    return
  }
  if (signMode.value === 'FAILED' && !signForm.failReason.trim()) {
    message.warning('请输入配送失败原因')
    return
  }
  signing.value = true
  try {
    await deliveryRouteApi.signPoint(props.routeId, signTarget.value.pointId, {
      status: signMode.value,
      signee: signForm.signee || undefined,
      failReason: signForm.failReason || undefined,
      remark: signForm.remark || undefined,
    })
    message.success('操作成功')
    signVisible.value = false
    await loadDetail()
    emit('changed')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    signing.value = false
  }
}

// ── 打印路线单：委托宿主 ────────────────────────────────
// 原先是自拼 HTML + win.print()（打出来只这份抽屉的数据，且与全站模板引擎不一致）。
// 现在只把主键交给宿主，由宿主调 handlePrint(record) 走 PrintDialog（模板 + 后端装配器）。
function handlePrint() {
  const id = detail.value?.id ?? props.routeId
  if (!id) {
    message.warning('没有可打印的数据')
    return
  }
  emit('print', { id: Number(id) })
}

// ── 地图查看（复用 RouteMapCanvas，不自建地图） ─────────
const mapVisible = ref(false)
const mapMarkers = ref<any[]>([])
const mapPolyline = ref<any[]>([])
const mapCircle = ref<any>(null)
const mapPolygon = ref<any[]>([])

function openMap() {
  const located = (points.value || []).filter(p => p.latitude && p.longitude)
  mapMarkers.value = located.map(p => ({
    lat: Number(p.latitude),
    lng: Number(p.longitude),
    label: String(p.pointOrder ?? ''),
    type: 'point',
    color: p.status === 'DELIVERED' ? '#52c41a' : p.status === 'FAILED' ? '#ff4d4f' : '#1890ff',
  }))
  mapPolyline.value = located.map(p => ({ lat: Number(p.latitude), lng: Number(p.longitude) }))
  mapVisible.value = true
  loadFenceGeometry()
}

/** 叠画绑定围栏的几何（圆形/多边形），复用《路线规划》的围栏档案 */
async function loadFenceGeometry() {
  mapCircle.value = null
  mapPolygon.value = []
  const fenceId = detail.value?.fenceId
  if (!fenceId) return
  try {
    const fence: any = await geoFenceApi.detail(fenceId)
    if (!fence) return
    if (fence.fenceType === 'POLYGON' && fence.polygonPoints) {
      mapPolygon.value = String(fence.polygonPoints).split(';').map(seg => {
        const [lng, lat] = seg.split(',').map(Number)
        return { lat, lng }
      }).filter(pt => !Number.isNaN(pt.lat) && !Number.isNaN(pt.lng))
    } else if (fence.centerLat != null && fence.centerLng != null) {
      mapCircle.value = {
        lat: Number(fence.centerLat),
        lng: Number(fence.centerLng),
        radiusMeters: Number(fence.radiusMeters || 0),
      }
    }
  } catch (e) {
    console.warn('[配送路线单] 加载围栏几何失败', e)
  }
}

// ── ETA 通知台账 ────────────────────────────────────────
const LEDGER_TEXT: Record<string, string> = {
  PENDING: '待发送', SENT: '已发送', FAILED: '失败', CANCELLED: '已作废',
}
const LEDGER_COLOR: Record<string, string> = {
  PENDING: 'orange', SENT: 'green', FAILED: 'red', CANCELLED: 'default',
}
const LEDGER_STATUS_OPTIONS = Object.entries(LEDGER_TEXT).map(([value, label]) => ({ label, value }))

/** 消息底座发送结果（sys_message.send_status）：0待发送 1发送中 2成功 3失败；-1 未投递 */
const MSG_STATUS_TEXT: Record<number, string> = { 0: '待发送', 1: '发送中', 2: '已发送', 3: '发送失败', [-1]: '未投递' }
const MSG_STATUS_COLOR: Record<number, string> = { 0: 'orange', 1: 'processing', 2: 'green', 3: 'red', [-1]: 'default' }

const ledgerColumns: any[] = [
  { title: '序号', dataIndex: 'pointSeq', width: 60, align: 'center' },
  { title: '状态', dataIndex: 'status', width: 90, align: 'center' },
  { title: '客户', dataIndex: 'customerName', width: 110 },
  { title: '手机号', dataIndex: 'customerPhone', width: 130 },
  { title: '预估到达', dataIndex: 'etaTime', width: 160 },
  { title: '通道', dataIndex: 'channel', width: 80 },
  { title: '发送结果', dataIndex: 'channelStatus', width: 100, align: 'center' },
  { title: '通知内容', dataIndex: 'content', ellipsis: true },
]

const ledgerVisible = ref(false)
const ledgerLoading = ref(false)
const ledgerRows = ref<any[]>([])
const ledgerKeys = ref<any[]>([])
const ledgerSelected = ref<any[]>([])
const ledgerQuery = reactive({ status: undefined as string | undefined })

function fmtLedgerTime(v?: string | null) {
  return fmt(v)
}

function openNotifyLedger() {
  ledgerKeys.value = []
  ledgerSelected.value = []
  ledgerVisible.value = true
  loadLedger()
}

async function loadLedger() {
  if (!props.routeId) return
  ledgerLoading.value = true
  try {
    const res: any = await deliveryRouteApi.etaNotifyPage({
      routeId: props.routeId,
      status: ledgerQuery.status || undefined,
      pageNum: 1,
      pageSize: 100,
    })
    ledgerRows.value = (res?.records || []).map((r: any) => ({ ...r, etaTime: fmtLedgerTime(r.etaTime) }))
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载通知台账失败')
  } finally {
    ledgerLoading.value = false
  }
}

function onLedgerSelect(keys: any[], rows: any[]) {
  ledgerKeys.value = keys
  ledgerSelected.value = rows
}

async function handleLedgerMark(status: 'SENT' | 'CANCELLED') {
  const label = status === 'SENT' ? '标记已发送' : '作废'
  Modal.confirm({
    title: `确认${label}`,
    content: `已选择 ${ledgerSelected.value.length} 条通知，确定${label}吗？`,
    onOk: async () => {
      try {
        await deliveryRouteApi.updateEtaNotifyStatus({
          ids: ledgerKeys.value,
          status,
          errorMsg: status === 'CANCELLED' ? '运营作废' : undefined,
        })
        message.success(`已${label}`)
        loadLedger()
      } catch (e: any) {
        message.error(e?.response?.data?.message || `${label}失败`)
      }
    },
  })
}

async function handleLedgerSend() {
  try {
    const res: any = await deliveryRouteApi.sendEtaNotify({ ids: ledgerKeys.value })
    if (res?.channelReady === false) {
      message.warning('短信通道未配置（sms.enabled/sms.endpoint）：未发送，可在《设置 → 消息中心》配置后由定时任务自动发出')
    } else {
      message.success(res?.message || '已触发发送')
    }
    loadLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '发送失败')
  }
}

const dispatching = ref(false)

async function handleDispatch() {
  dispatching.value = true
  try {
    const res: any = await deliveryRouteApi.dispatchEtaNotify({ routeId: props.routeId ?? undefined })
    message.success(res?.message || '已补投递')
    loadLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '补投递失败')
  } finally {
    dispatching.value = false
  }
}

defineExpose({ loadDetail })
</script>

<style scoped>
.drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.progress-text { font-size: 13px; color: #666; }
.route-desc { margin-bottom: 16px; }
.points-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.done-text { color: #999; font-size: 12px; }
.expedited-tag { margin-left: 6px; transform: scale(0.85); }
.hint { margin-left: 8px; font-size: 12px; color: #999; }
.ledger-tip { margin-bottom: 10px; }
.ledger-bar { margin-bottom: 10px; }
.ledger-content { font-size: 12px; color: #555; }
</style>
