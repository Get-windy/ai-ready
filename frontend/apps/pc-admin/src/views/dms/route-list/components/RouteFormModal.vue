<template>
  <a-modal
    v-model:open="visible"
    :title="form.id ? '修改配送路线单' : '新增配送路线单'"
    :width="980"
    :mask-closable="false"
    :confirm-loading="saving"
    ok-text="保存(Enter)"
    cancel-text="关闭(Esc)"
    @ok="handleSave"
  >
    <a-form
      :label-col="{ span: 3 }"
      :wrapper-col="{ span: 21 }"
      size="small"
    >
      <a-row :gutter="12">
        <a-col :span="8">
          <a-form-item label="路线编号" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" required>
            <a-input
              v-model:value="form.routeCode"
              placeholder="留空自动生成"
              :maxlength="50"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="配送线路" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select
              v-model:value="form.routeId"
              placeholder="选择线路档案(可空)"
              size="small"
              show-search
              allow-clear
              :options="routeOptions"
              :filter-option="filterOption"
              :not-found-content="routeLoading ? undefined : '暂无启用线路，请先在「资料 → 配送管理 → 线路」维护'"
              @change="handleRouteChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="计划日期" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" required>
            <a-date-picker
              v-model:value="form.planDate"
              value-format="YYYY-MM-DD"
              size="small"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="配送员" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }" required>
            <a-select
              v-model:value="form.deliveryPersonId"
              placeholder="选择配送员"
              size="small"
              show-search
              allow-clear
              :options="riderOptions"
              :filter-option="filterOption"
              :not-found-content="riderLoading ? undefined : '暂无配送员档案，请先维护配送员'"
              @change="handleRiderChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="车辆" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select
              v-model:value="form.vehicleId"
              placeholder="选择车辆(可空)"
              size="small"
              show-search
              allow-clear
              :options="vehicleOptions"
              :filter-option="filterOption"
              :not-found-content="vehicleLoading ? undefined : '暂无车辆档案，请先维护车辆'"
              @change="handleVehicleChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="配送围栏" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-select
              v-model:value="form.fenceId"
              placeholder="选择电子围栏(可空)"
              size="small"
              show-search
              allow-clear
              :options="fenceOptions"
              :filter-option="filterOption"
              :not-found-content="fenceLoading ? undefined : '暂无围栏档案，请先在「配送 → 路线规划」维护'"
              @change="handleFenceChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="围栏归集" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-switch
              :checked="form.autoCollect === 1"
              :disabled="!form.fenceId"
              checked-children="参与"
              un-checked-children="不参与"
              @change="(v: any) => (form.autoCollect = v ? 1 : 0)"
            />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="起点地址" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-input
              v-model:value="form.startPoint"
              placeholder="留空取首个点位地址"
              :maxlength="200"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
            <a-textarea
              v-model:value="form.remark"
              placeholder="请输入备注"
              :rows="2"
              :maxlength="500"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>

    <!-- 点位明细（多点配送：一个路线单含多个点位，逐点签收） -->
    <div class="points-header">
      <span class="points-title">配送点位（{{ form.points.length }}）</span>
      <a-button
        type="link"
        size="small"
        @click="addPoint"
      >
        <PlusOutlined /> 添加点位
      </a-button>
    </div>
    <div class="points-wrap">
      <a-table
        :columns="pointColumns"
        :data-source="form.points"
        :pagination="false"
        size="small"
        row-key="__key"
        bordered
      >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.dataIndex === 'pointOrder'">
          <span>{{ index + 1 }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'customerName'">
          <a-input v-model:value="record.customerName" size="small" placeholder="客户名称" />
        </template>
        <template v-else-if="column.dataIndex === 'customerPhone'">
          <a-input v-model:value="record.customerPhone" size="small" placeholder="联系电话" />
        </template>
        <template v-else-if="column.dataIndex === 'orderNo'">
          <a-input v-model:value="record.orderNo" size="small" placeholder="来源单号" />
        </template>
        <template v-else-if="column.dataIndex === 'address'">
          <a-input v-model:value="record.address" size="small" placeholder="地址（必填）" />
        </template>
        <template v-else-if="column.dataIndex === 'remark'">
          <a-input v-model:value="record.remark" size="small" placeholder="备注" />
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-button
            type="link"
            size="small"
            danger
            @click="removePoint(index)"
          >
            删除
          </a-button>
        </template>
      </template>
      </a-table>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { deliveryRouteApi, type DeliveryRoute, type DeliveryRoutePoint } from '@/api/dms/route'
import { mdRouteApi } from '@/api/md'
import { geoFenceApi } from '@/api/dms/route'
import { riderApi } from '@/api/dms/rider'
import { vehicleApi } from '@/api/dms/vehicle'

const props = defineProps<{ open: boolean; recordId?: number | null }>()
const emit = defineEmits<{ 'update:open': [v: boolean]; success: [] }>()

const visible = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const saving = ref(false)

const form = reactive({
  id: null as number | null,
  routeCode: '',
  routeId: undefined as number | undefined,
  planDate: '' as string,
  deliveryPersonId: undefined as string | undefined,
  deliveryPersonName: '',
  vehicleId: undefined as number | undefined,
  vehicleNo: '',
  fenceId: undefined as number | undefined,
  autoCollect: 1,
  startPoint: '',
  remark: '',
  points: [] as any[],
})

const pointColumns: any[] = [
  { title: '序号', dataIndex: 'pointOrder', width: 60, align: 'center' },
  { title: '客户名称', dataIndex: 'customerName', width: 150 },
  { title: '联系电话', dataIndex: 'customerPhone', width: 130 },
  { title: '来源单号', dataIndex: 'orderNo', width: 140 },
  { title: '地址', dataIndex: 'address' },
  { title: '备注', dataIndex: 'remark', width: 140 },
  { title: '操作', dataIndex: 'action', width: 70, align: 'center' },
]

// ── 下拉选项 ────────────────────────────────────────────
const routeOptions = ref<any[]>([])
const fenceOptions = ref<any[]>([])
const fenceLoading = ref(false)
const riderOptions = ref<any[]>([])
const vehicleOptions = ref<any[]>([])
const routeLoading = ref(false)
const riderLoading = ref(false)
const vehicleLoading = ref(false)
let keySeed = 0

function filterOption(input: string, option: any) {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

async function loadRouteOptions() {
  routeLoading.value = true
  try {
    const res: any = await mdRouteApi.options()
    const list: any[] = Array.isArray(res) ? res : res?.records || []
    routeOptions.value = list.map(r => ({
      label: `${r.routeCode || ''} ${r.routeName || ''}`.trim(),
      value: r.id,
      raw: r,
    }))
  } catch (e) {
    console.warn('[配送路线单] 加载线路下拉失败', e)
  } finally {
    routeLoading.value = false
  }
}

async function loadFenceOptions() {
  fenceLoading.value = true
  try {
    // 围栏档案统一由《路线规划》维护，本页只消费其下拉（研判边界：不自建围栏）
    const res: any = await geoFenceApi.options()
    const list: any[] = Array.isArray(res) ? res : res?.records || []
    fenceOptions.value = list.map(f => ({
      label: f.fenceName ? `${f.fenceName}（${f.fenceType === 'POLYGON' ? '多边形' : '圆形'}）` : `#${f.id}`,
      value: f.id,
    }))
  } catch (e) {
    console.warn('[配送路线单] 加载围栏下拉失败', e)
  } finally {
    fenceLoading.value = false
  }
}

function handleFenceChange(val: any) {
  if (!val) {
    form.autoCollect = 0
  }
}

async function loadRiderOptions() {
  riderLoading.value = true
  try {
    const res: any = await riderApi.page({ pageNum: 1, pageSize: 500 })
    const list: any[] = res?.records || []
    riderOptions.value = list.map(r => ({
      label: r.realName ? `${r.realName}${r.phone ? ' ' + r.phone : ''}` : `#${r.id}`,
      // 姓名单独存一份：label 里带手机号，不能靠 split(' ') 反推（姓名本身可能含空格）
      name: r.realName || `#${r.id}`,
      value: String(r.id),
    }))
  } catch (e) {
    console.warn('[配送路线单] 加载配送员下拉失败', e)
  } finally {
    riderLoading.value = false
  }
}

async function loadVehicleOptions() {
  vehicleLoading.value = true
  try {
    const res: any = await vehicleApi.page({ pageNum: 1, pageSize: 500 })
    const list: any[] = res?.records || []
    vehicleOptions.value = list.map(v => ({ label: v.plateNo || `#${v.id}`, value: v.id, raw: v }))
  } catch (e) {
    console.warn('[配送路线单] 加载车辆下拉失败', e)
  } finally {
    vehicleLoading.value = false
  }
}

function handleRouteChange(val: any) {
  const hit = routeOptions.value.find(o => o.value === val)
  form.routeId = hit ? hit.value : undefined
}

function handleRiderChange(val: any) {
  const hit = riderOptions.value.find(o => o.value === val)
  form.deliveryPersonName = hit ? String(hit.name || '') : ''
}

function handleVehicleChange(val: any) {
  const hit = vehicleOptions.value.find(o => o.value === val)
  form.vehicleNo = hit ? String(hit.label) : ''
}

// ── 点位行 ──────────────────────────────────────────────
function addPoint() {
  form.points.push({
    __key: `p${++keySeed}`,
    customerName: '',
    customerPhone: '',
    orderNo: '',
    address: '',
    remark: '',
  })
}

function removePoint(index: number) {
  form.points.splice(index, 1)
}

/** 本地日期（勿用 toISOString()：UTC 时区在东八区 08:00 前会退到昨天） */
function todayLocal(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function resetForm() {
  form.id = null
  form.routeCode = ''
  form.routeId = undefined
  form.planDate = todayLocal()
  form.deliveryPersonId = undefined
  form.deliveryPersonName = ''
  form.vehicleId = undefined
  form.vehicleNo = ''
  form.fenceId = undefined
  form.autoCollect = 1
  form.startPoint = ''
  form.remark = ''
  form.points = []
  keySeed = 0
  addPoint()
}

// ── 打开弹窗 ────────────────────────────────────────────
watch(
  () => props.open,
  async (open) => {
    if (!open) return
    resetForm()
    if (routeOptions.value.length === 0) loadRouteOptions()
    if (fenceOptions.value.length === 0) loadFenceOptions()
    if (riderOptions.value.length === 0) loadRiderOptions()
    if (vehicleOptions.value.length === 0) loadVehicleOptions()
    if (props.recordId) {
      await loadDetail(props.recordId)
    } else {
      await fillNextNo()
    }
  },
)

async function fillNextNo() {
  try {
    const res: any = await deliveryRouteApi.nextNo()
    form.routeCode = typeof res === 'string' ? res : res?.data || ''
  } catch (e) {
    console.warn('[配送路线单] 生成编号失败', e)
  }
}

async function loadDetail(id: number) {
  try {
    const detail: any = await deliveryRouteApi.detail(id)
    if (!detail) return
    form.id = detail.id
    form.routeCode = detail.routeCode || ''
    form.routeId = detail.routeId ?? undefined
    form.planDate = detail.planDate || ''
    form.deliveryPersonId = detail.deliveryPersonId || undefined
    form.deliveryPersonName = detail.deliveryPersonName || ''
    form.vehicleId = detail.vehicleId ?? undefined
    form.vehicleNo = detail.vehicleNo || ''
    form.fenceId = detail.fenceId ?? undefined
    form.autoCollect = detail.autoCollect ?? 0
    form.startPoint = detail.startPoint || ''
    form.remark = detail.remark || ''
    const points: DeliveryRoutePoint[] = detail.points || []
    form.points = points.map(p => ({
      __key: `p${++keySeed}`,
      customerName: p.customerName || '',
      customerPhone: p.customerPhone || '',
      orderNo: p.orderNo || '',
      address: p.address || '',
      remark: p.remark || '',
    }))
    if (form.points.length === 0) addPoint()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载配送路线详情失败')
  }
}

// ── 保存 ────────────────────────────────────────────────
async function handleSave() {
  if (!form.planDate) {
    message.warning('请选择计划配送日期')
    return
  }
  if (!form.deliveryPersonId) {
    message.warning('请选择配送员')
    return
  }
  if (form.points.length === 0) {
    message.warning('请至少添加 1 个配送点位')
    return
  }
  for (let i = 0; i < form.points.length; i++) {
    if (!String(form.points[i].address || '').trim()) {
      message.warning(`第 ${i + 1} 个点位的地址不能为空`)
      return
    }
  }

  const payload: Partial<DeliveryRoute> = {
    routeCode: form.routeCode?.trim() || undefined,
    routeId: form.routeId ?? undefined,
    planDate: form.planDate,
    deliveryPersonId: form.deliveryPersonId,
    deliveryPersonName: form.deliveryPersonName || undefined,
    vehicleId: form.vehicleId ?? undefined,
    vehicleNo: form.vehicleNo || undefined,
    fenceId: form.fenceId ?? undefined,
    autoCollect: form.autoCollect,
    clearFence: !form.fenceId,
    startPoint: form.startPoint?.trim() || undefined,
    remark: form.remark || undefined,
    points: form.points.map((p, i) => ({
      pointOrder: i + 1,
      customerName: p.customerName || undefined,
      customerPhone: p.customerPhone || undefined,
      orderNo: p.orderNo || undefined,
      address: String(p.address).trim(),
      remark: p.remark || undefined,
    })) as any,
  }

  saving.value = true
  try {
    if (form.id) {
      await deliveryRouteApi.update(form.id, payload)
      message.success('修改成功')
    } else {
      await deliveryRouteApi.create(payload)
      message.success('新增成功')
    }
    visible.value = false
    emit('success')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.points-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 8px;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
}
.points-title { font-size: 13px; font-weight: 600; color: #333; }
/* 点位较多时在弹窗内滚动，避免弹窗被撑高 */
.points-wrap { max-height: 300px; overflow: auto; }
</style>
