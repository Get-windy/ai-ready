<script setup lang="ts">
/**
 * 我的配送路线（司机端）
 *
 * 与 PC 端《配送路线单》同一后端：
 *   待出发列表 /delivery/route/page（按 deliveryPersonId 过滤）
 *   进行中     /delivery/route/active/{deliveryPersonId}
 *   逐点签收   /delivery/route/{id}/point/{pointId}/sign
 * 规则（状态机、必填校验、统计口径）完全由后端负责，司机端只做呈现与提交，不重复实现。
 */
import { ref, computed, onMounted } from 'vue'
import { NavBar, Card, Button, Tag, Empty, CellGroup, Cell, Popup, Field, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { request } from '@/utils/request'
import { deliveryRouteApi, type DriverRoute, type RoutePoint } from '@/api/delivery-route'
import { dmsApi } from '@/api/dms'

const riderId = ref<string>('')
const riderName = ref<string>('')
const loading = ref(false)
const pendingRoutes = ref<DriverRoute[]>([])
const activeRoute = ref<DriverRoute | null>(null)
const etaMap = ref<Record<number, string>>({})

const POINT_STATUS: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待配送', color: '#969799' },
  IN_ROUTE: { label: '在途中', color: '#1988fa' },
  ARRIVED: { label: '已到达', color: '#ff976a' },
  DELIVERED: { label: '已送达', color: '#07c160' },
  FAILED: { label: '配送失败', color: '#ee0a24' },
  SKIPPED: { label: '已跳过', color: '#969799' },
}

const points = computed<RoutePoint[]>(() => activeRoute.value?.points || [])

function pointStatusText(status?: string) {
  return POINT_STATUS[status || '']?.label || status || '-'
}

function pointStatusColor(status?: string) {
  return POINT_STATUS[status || '']?.color || '#969799'
}

function canOperate(p: RoutePoint) {
  return p.status !== 'DELIVERED' && p.status !== 'FAILED' && p.status !== 'SKIPPED'
}

function fmt(v?: string | null) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

async function loadMe() {
  try {
    const res: any = await dmsApi.getMyRider()
    const rider = res?.data || res
    if (rider?.id) {
      riderId.value = String(rider.id)
      riderName.value = rider.realName || ''
    }
  } catch (e) {
    console.warn('[我的配送路线] 获取司机档案失败', e)
  }
}

async function loadAll() {
  if (!riderId.value) {
    await loadMe()
  }
  if (!riderId.value) {
    showToast('未查询到司机档案，请先在「我的 → 实名认证」完成绑定')
    return
  }
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    // 待出发（可开始）
    const page: any = await request.get('/delivery/route/page', {
      pageNum: 1,
      pageSize: 20,
      deliveryPersonId: riderId.value,
      status: 'PLANNING,READY',
    })
    pendingRoutes.value = page?.records || []
    // 进行中
    const active: any = await deliveryRouteApi.active(riderId.value)
    const route = active?.data ?? active
    if (route && route.id) {
      const detail: any = await deliveryRouteApi.detail(route.id)
      activeRoute.value = (detail?.data ?? detail) as DriverRoute
    } else {
      activeRoute.value = null
      etaMap.value = {}
    }
  } catch (e: any) {
    showToast(e?.message || '加载失败')
  } finally {
    loading.value = false
    closeToast()
  }
}

async function handleStart(route: DriverRoute) {
  await Dialog.confirm({ title: '开始配送', message: `确定开始配送路线「${route.routeCode}」吗？` })
  try {
    await deliveryRouteApi.start(route.id)
    showToast('已开始配送')
    loadAll()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '操作失败')
  }
}

/** 到达：确认后直接提交 */
async function handleArrive(point: RoutePoint) {
  const ok = await Dialog.confirm({ title: '已到达', message: '确认已到达该点位？' }).catch(() => null)
  if (!ok) return
  await submitSign(point, 'ARRIVED', {})
}

/** 送达 / 失败：必须录入签收人或失败原因（后端强校验，前端先拦一道） */
const signPopup = ref(false)
const signMode = ref<'DELIVERED' | 'FAILED'>('DELIVERED')
const signTarget = ref<RoutePoint | null>(null)
const signForm = ref({ signee: '', failReason: '', remark: '' })

function openSign(point: RoutePoint, mode: 'DELIVERED' | 'FAILED') {
  signTarget.value = point
  signMode.value = mode
  signForm.value = { signee: point.customerName || '', failReason: '', remark: '' }
  signPopup.value = true
}

async function submitSignPopup() {
  const point = signTarget.value
  if (!point) return
  if (signMode.value === 'DELIVERED' && !signForm.value.signee.trim()) {
    showToast('请填写签收人')
    return
  }
  if (signMode.value === 'FAILED' && !signForm.value.failReason.trim()) {
    showToast('请填写配送失败原因')
    return
  }
  const ok = await submitSign(point, signMode.value, {
    signee: signForm.value.signee || undefined,
    failReason: signForm.value.failReason || undefined,
    remark: signForm.value.remark || undefined,
  })
  if (ok) {
    signPopup.value = false
  }
}

async function submitSign(
  point: RoutePoint,
  status: 'ARRIVED' | 'DELIVERED' | 'FAILED',
  payload: { signee?: string; failReason?: string; remark?: string },
): Promise<boolean> {
  if (!activeRoute.value) return false
  const label = status === 'ARRIVED' ? '到达' : status === 'DELIVERED' ? '送达' : '配送失败'
  try {
    await deliveryRouteApi.signPoint(activeRoute.value.id, point.pointId, { status, ...payload })
    showToast(`${label}成功`)
    await loadAll()
    return true
  } catch (e: any) {
    showToast(e?.response?.data?.message || `${label}失败`)
    return false
  }
}

async function handleComplete() {
  if (!activeRoute.value) return
  const pending = points.value.filter(p => canOperate(p)).length
  await Dialog.confirm({
    title: '完成配送',
    message: pending > 0
      ? `还有 ${pending} 个点位未处理，完成后将自动标记为「已跳过」。确定完成？`
      : '确定完成本次配送吗？',
  })
  try {
    await deliveryRouteApi.complete(activeRoute.value.id)
    showToast('已完成配送')
    loadAll()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '操作失败')
  }
}

async function handlePlan() {
  if (!activeRoute.value) return
  try {
    const res: any = await deliveryRouteApi.planOrder(activeRoute.value.id)
    showToast(res?.data?.message || res?.message || '已重新规划')
    loadAll()
  } catch (e: any) {
    showToast(e?.response?.data?.message || '规划失败（请确认点位已维护坐标）')
  }
}

async function handleEta() {
  if (!activeRoute.value) return
  try {
    const res: any = await deliveryRouteApi.eta(activeRoute.value.id)
    const data = res?.data ?? res
    const map: Record<number, string> = {}
    ;(data?.points || []).forEach((it: any) => { if (it.etaTime) map[it.pointId] = it.etaTime })
    etaMap.value = map
    if (data?.message) {
      showToast(data.message)
    } else {
      showToast('已刷新预计到达时间')
    }
  } catch (e: any) {
    showToast(e?.response?.data?.message || 'ETA 预估失败')
  }
}

function openNavigation(point: RoutePoint) {
  if (!point.latitude || !point.longitude) {
    showToast('该点位未维护坐标，无法导航')
    return
  }
  // 复用系统地图能力（与《路线规划》同一坐标系 GCJ-02）
  window.open(`https://uri.amap.com/marker?position=${point.longitude},${point.latitude}&name=${encodeURIComponent(point.address || '配送点')}`, '_blank')
}

onMounted(loadAll)
</script>

<template>
  <div class="my-route-page">
    <NavBar title="我的配送路线" fixed placeholder />

    <div class="content">
      <!-- 待出发 -->
      <div v-if="pendingRoutes.length > 0" class="card block">
        <div class="card-title">待出发路线</div>
        <CellGroup inset>
          <Cell
            v-for="r in pendingRoutes"
            :key="r.id"
            :title="r.routeCode"
            :label="`${r.fenceName || '未绑围栏'} · ${r.totalPoints || 0} 个点位 · 计划 ${r.planDate || '-'}`"
          >
            <template #value>
              <Button size="mini" type="primary" @click="handleStart(r)">开始配送</Button>
            </template>
          </Cell>
        </CellGroup>
      </div>

      <!-- 进行中 -->
      <template v-if="activeRoute">
        <div class="card block">
          <div class="route-head card-title">
            <span>{{ activeRoute.routeCode }}</span>
            <Tag type="primary">{{ activeRoute.statusText || activeRoute.status }}</Tag>
          </div>
          <CellGroup inset>
            <Cell title="完成进度" :value="activeRoute.progress || `0 / ${activeRoute.totalPoints || 0}`" />
            <Cell title="配送围栏" :value="activeRoute.fenceName || '-'" />
            <Cell title="车辆" :value="activeRoute.vehicleNo || '-'" />
            <Cell title="起点" :value="activeRoute.startPoint || '-'" />
            <Cell title="里程/时长" :value="`${activeRoute.totalDistance ?? '-'} km / ${activeRoute.totalDuration ?? '-'} 分钟`" />
          </CellGroup>
          <div class="actions">
            <Button size="small" @click="handlePlan">重新规划</Button>
            <Button size="small" @click="handleEta">刷新ETA</Button>
            <Button size="small" type="success" @click="handleComplete">完成配送</Button>
          </div>
        </div>

        <div
          v-for="p in points"
          :key="p.pointId"
          class="card block point-card"
          :class="{ 'point-done': !canOperate(p) }"
        >
          <div class="point-head">
            <span class="seq">{{ p.pointOrder }}</span>
            <Tag :color="pointStatusColor(p.status)" text-color="#fff">{{ pointStatusText(p.status) }}</Tag>
            <Tag v-if="p.expedited === 1" color="#ee0a24" text-color="#fff">催单</Tag>
          </div>
          <div class="point-body">
            <div class="row"><span class="label">客户</span><span>{{ p.customerName || '-' }}</span></div>
            <div class="row"><span class="label">电话</span>
              <a v-if="p.customerPhone" :href="`tel:${p.customerPhone}`">{{ p.customerPhone }}</a>
              <span v-else>-</span>
            </div>
            <div class="row"><span class="label">地址</span><span>{{ p.address }}</span></div>
            <div class="row"><span class="label">预计到达</span><span>{{ etaMap[p.pointId] ? fmt(etaMap[p.pointId]) : fmt(p.etaTime) }}</span></div>
            <div v-if="p.signee" class="row"><span class="label">签收人</span><span>{{ p.signee }} · {{ fmt(p.signTime) }}</span></div>
            <div v-if="p.failReason" class="row"><span class="label">失败原因</span><span>{{ p.failReason }}</span></div>
          </div>
          <div class="actions">
            <Button size="small" @click="openNavigation(p)">导航</Button>
            <template v-if="canOperate(p)">
              <Button size="small" plain type="primary" @click="handleArrive(p)">到达</Button>
              <Button size="small" type="success" @click="openSign(p, 'DELIVERED')">送达</Button>
              <Button size="small" plain type="danger" @click="openSign(p, 'FAILED')">失败</Button>
            </template>
          </div>
        </div>
      </template>

      <Empty v-else-if="!loading" description="暂无进行中的配送路线">
        <Button type="primary" size="small" @click="loadAll">刷新</Button>
      </Empty>
    </div>

    <!-- 签收 / 失败录入 -->
    <Popup v-model:show="signPopup" position="bottom" round :style="{ padding: '16px' }">
      <div class="popup-title">{{ signMode === 'DELIVERED' ? '确认送达' : '配送失败' }}</div>
      <div class="popup-sub">{{ signTarget?.address }}</div>
      <Field
        v-if="signMode === 'DELIVERED'"
        v-model="signForm.signee"
        label="签收人"
        placeholder="请输入签收人姓名（必填）"
        maxlength="50"
      />
      <Field
        v-else
        v-model="signForm.failReason"
        label="失败原因"
        type="textarea"
        rows="2"
        autosize
        placeholder="请输入配送失败原因（必填）"
        maxlength="200"
      />
      <Field v-model="signForm.remark" label="备注" placeholder="选填" maxlength="200" />
      <div class="popup-actions">
        <Button block @click="signPopup = false">取消</Button>
        <Button block type="primary" @click="submitSignPopup">
          {{ signMode === 'DELIVERED' ? '确认送达' : '确认失败' }}
        </Button>
      </div>
    </Popup>
  </div>
</template>

<style scoped>
.my-route-page { min-height: 100vh; background: #f7f8fa; padding-bottom: 60px; }
.content { padding: 10px 0; }
.block { margin: 10px; }
.card { background: #fff; border-radius: 8px; padding: 10px 0 6px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04); }
.card-title { font-size: 14px; font-weight: 600; color: #323233; padding: 0 12px 8px; }
.route-head { display: flex; align-items: center; gap: 8px; }
.actions { display: flex; gap: 8px; padding: 10px 12px 4px; flex-wrap: wrap; }
.point-card { border-left: 3px solid #1988fa; }
.point-done { border-left-color: #07c160; opacity: 0.85; }
.point-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.seq { width: 22px; height: 22px; line-height: 22px; text-align: center; border-radius: 50%; background: #1988fa; color: #fff; font-size: 12px; }
.point-body { font-size: 13px; color: #323233; }
.popup-title { font-size: 16px; font-weight: 600; margin-bottom: 4px; }
.popup-sub { font-size: 12px; color: #969799; margin-bottom: 10px; }
.popup-actions { display: flex; gap: 10px; margin-top: 12px; }
.row { display: flex; gap: 8px; padding: 2px 0; }
.label { color: #969799; min-width: 62px; }
</style>
