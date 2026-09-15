<template>
  <a-modal
    v-model:open="visible"
    title="配送需求归集"
    :width="1080"
    :footer="null"
    :mask-closable="false"
  >
    <a-tabs v-model:active-key="activeTab">
      <!-- ═══ ① 围栏自动归集 ═══ -->
      <a-tab-pane key="auto" tab="围栏自动归集">
        <div class="tip">
          规则：销售出库单（已发货）/ 销售订单（待发货·部分发货）的<b>客户配送坐标</b>落入路线单绑定的电子围栏内，即自动加入该路线单。
        </div>
        <a-space :size="8" class="row">
          <a-select
            v-model:value="autoForm.source"
            size="small"
            style="width: 170px"
            :options="SOURCE_OPTIONS"
          />
          <a-input
            v-model:value="autoForm.keyword"
            size="small"
            placeholder="单号/客户名"
            style="width: 200px"
            allow-clear
            @press-enter="handlePreview"
          />
          <a-select
            v-if="!props.routeId"
            v-model:value="autoForm.routeId"
            size="small"
            placeholder="全部开启自动归集的路线单"
            style="width: 240px"
            allow-clear
            show-search
            :filter-option="filterOption"
            :options="routeOptions"
          />
          <a-button
            type="primary"
            size="small"
            :loading="previewing"
            @click="handlePreview"
          >
            预览匹配结果
          </a-button>
          <a-button
            size="small"
            :disabled="!preview || preview.added === 0"
            :loading="collecting"
            @click="handleCollect"
          >
            执行归集（{{ preview?.added ?? 0 }} 条）
          </a-button>
        </a-space>

        <a-alert
          v-if="preview"
          class="row"
          type="info"
          show-icon
          :message="`扫描 ${preview.scanned} 条 · 命中围栏 ${preview.matched} 条 · 可入线 ${preview.added} 条 · 缺坐标 ${preview.missingCoordinate} 条 · 围栏外 ${preview.outsideFence} 条`"
        />

        <a-table
          v-if="preview"
          :columns="previewColumns"
          :data-source="preview.items"
          :pagination="{ pageSize: 8, size: 'small' }"
          size="small"
          row-key="billNo"
          bordered
          :scroll="{ y: 240 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'result'">
              <a-tag :color="RESULT_COLOR[record.result] || 'default'">
                {{ RESULT_TEXT[record.result] || record.result }}
              </a-tag>
            </template>
          </template>
        </a-table>

        <!-- 缺坐标客户就地补录 -->
        <div
          v-if="missingGeoCustomers.length > 0"
          class="geo-block"
        >
          <div class="geo-title">
            <ExclamationCircleOutlined />
            以下客户未维护配送坐标，无法参与围栏判定（可在此就地补录，保存后重新预览）
          </div>
          <a-table
            :columns="geoColumns"
            :data-source="missingGeoCustomers"
            :pagination="{ pageSize: 5, size: 'small' }"
            size="small"
            row-key="id"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'latitude'">
                <a-input-number
                  v-model:value="record.__lat"
                  size="small"
                  :precision="6"
                  style="width: 120px"
                  placeholder="纬度"
                />
              </template>
              <template v-else-if="column.dataIndex === 'longitude'">
                <a-input-number
                  v-model:value="record.__lng"
                  size="small"
                  :precision="6"
                  style="width: 120px"
                  placeholder="经度"
                />
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-button
                  type="link"
                  size="small"
                  :loading="record.__saving"
                  @click="saveGeo(record)"
                >
                  保存
                </a-button>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <!-- ═══ ② 手动添加（不受围栏限制） ═══ -->
      <a-tab-pane key="manual" tab="手动添加（围栏外）">
        <div class="tip">
          围栏外的订单也可<b>人工补进</b>路线单：选择目标路线单 → 勾选待配送单据 → 添加。
        </div>
        <a-space :size="8" class="row">
          <a-select
            v-if="!props.routeId"
            v-model:value="manualForm.routeId"
            size="small"
            placeholder="选择目标路线单（必选）"
            style="width: 260px"
            show-search
            :filter-option="filterOption"
            :options="routeOptions"
          />
          <a-input
            v-model:value="manualForm.keyword"
            size="small"
            placeholder="单号/客户名"
            style="width: 200px"
            allow-clear
            @press-enter="loadDemands"
          />
          <a-button
            size="small"
            :loading="demandLoading"
            @click="loadDemands"
          >
            查询待配送单据
          </a-button>
          <a-checkbox v-model:checked="manualForm.replan">
            添加后按地图能力重排
          </a-checkbox>
          <a-button
            type="primary"
            size="small"
            :disabled="selectedDemands.length === 0 || !targetRouteId"
            :loading="adding"
            @click="handleManualAdd"
          >
            添加到路线单（{{ selectedDemands.length }}）
          </a-button>
        </a-space>

        <a-table
          :columns="demandColumns"
          :data-source="demands"
          :pagination="{ pageSize: 8, size: 'small' }"
          :loading="demandLoading"
          size="small"
          row-key="__key"
          bordered
          :row-selection="{ selectedRowKeys: selectedKeys, onChange: onSelectChange }"
          :scroll="{ y: 260 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'geo'">
              <span v-if="record.latitude && record.longitude">{{ record.latitude }}, {{ record.longitude }}</span>
              <a-tag
                v-else
                color="orange"
              >
                缺坐标
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import {
  deliveryRouteApi,
  type DeliveryDemand,
  type RouteAutoCollectResult,
} from '@/api/dms/route'

const props = defineProps<{ open: boolean; routeId?: number | null }>()
const emit = defineEmits<{ 'update:open': [v: boolean]; success: [] }>()

const visible = computed({
  get: () => props.open,
  set: (v: boolean) => emit('update:open', v),
})

const activeTab = ref('auto')

const SOURCE_OPTIONS = [
  { label: '销售出库单 + 销售订单', value: 'BOTH' },
  { label: '仅销售出库单', value: 'OUT' },
  { label: '仅销售订单', value: 'SO' },
]

const RESULT_TEXT: Record<string, string> = {
  ADDED: '已入线',
  PREVIEW: '命中围栏',
  ALREADY_ON_ROUTE: '已在线上',
  NO_COORD: '缺坐标',
  OUTSIDE: '围栏外',
}
const RESULT_COLOR: Record<string, string> = {
  ADDED: 'green',
  PREVIEW: 'blue',
  ALREADY_ON_ROUTE: 'default',
  NO_COORD: 'orange',
  OUTSIDE: 'default',
}

const previewColumns: any[] = [
  { title: '结果', dataIndex: 'result', width: 90 },
  { title: '来源单号', dataIndex: 'billNo', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 140 },
  { title: '配送地址', dataIndex: 'address', ellipsis: true },
  { title: '命中围栏', dataIndex: 'fenceName', width: 140 },
  { title: '目标路线单', dataIndex: 'routeCode', width: 170 },
  { title: '说明', dataIndex: 'reason', width: 200, ellipsis: true },
]

const geoColumns: any[] = [
  { title: '客户', dataIndex: 'customer_name', width: 180 },
  { title: '地址', dataIndex: 'address', ellipsis: true },
  { title: '纬度', dataIndex: 'latitude', width: 130 },
  { title: '经度', dataIndex: 'longitude', width: 130 },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' },
]

const demandColumns: any[] = [
  { title: '来源', dataIndex: 'sourceType', width: 80 },
  { title: '单号', dataIndex: 'billNo', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 140 },
  { title: '收货人', dataIndex: 'receiverName', width: 100 },
  { title: '电话', dataIndex: 'receiverPhone', width: 130 },
  { title: '配送地址', dataIndex: 'address', ellipsis: true },
  { title: '坐标', dataIndex: 'geo', width: 170 },
]

const autoForm = reactive({ source: 'BOTH', keyword: '', routeId: undefined as number | undefined })
const manualForm = reactive({ keyword: '', routeId: undefined as number | undefined, replan: false })

const previewing = ref(false)
const collecting = ref(false)
const adding = ref(false)
const demandLoading = ref(false)
const preview = ref<RouteAutoCollectResult | null>(null)
const demands = ref<any[]>([])
const selectedKeys = ref<any[]>([])
const selectedDemands = ref<DeliveryDemand[]>([])
const routeOptions = ref<any[]>([])
const missingGeoCustomers = ref<any[]>([])

const targetRouteId = computed(() => props.routeId ?? manualForm.routeId ?? autoForm.routeId)

function filterOption(input: string, option: any) {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

watch(
  () => props.open,
  async (open) => {
    if (!open) return
    preview.value = null
    selectedKeys.value = []
    selectedDemands.value = []
    if (!props.routeId) {
      await loadRoutes()
    }
    loadDemands()
  },
)

async function loadRoutes() {
  try {
    const res: any = await deliveryRouteApi.page({ pageNum: 1, pageSize: 200, status: 'PLANNING,READY' })
    routeOptions.value = (res?.records || []).map((r: any) => ({
      label: `${r.routeCode}${r.fenceName ? ' · ' + r.fenceName : '（未绑围栏）'}`,
      value: r.id,
    }))
  } catch (e) {
    console.warn('[配送路线单] 加载路线单下拉失败', e)
  }
}

// ── 自动归集 ──────────────────────────────────────────
async function handlePreview() {
  previewing.value = true
  try {
    const res: any = await deliveryRouteApi.autoCollectPreview({
      routeId: props.routeId ?? autoForm.routeId,
      source: autoForm.source,
      keyword: autoForm.keyword || undefined,
    })
    preview.value = res
    await loadMissingGeo(res)
  } catch (e: any) {
    message.error(e?.response?.data?.message || '预览失败')
  } finally {
    previewing.value = false
  }
}

async function handleCollect() {
  collecting.value = true
  try {
    const res: any = await deliveryRouteApi.autoCollect({
      routeId: props.routeId ?? autoForm.routeId,
      source: autoForm.source,
      keyword: autoForm.keyword || undefined,
    })
    message.success(`已归集 ${res?.added ?? 0} 条配送需求`)
    emit('success')
    await handlePreview()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '归集失败')
  } finally {
    collecting.value = false
  }
}

/** 缺坐标客户：取本次「缺坐标」结果对应的客户，供就地补录 */
async function loadMissingGeo(res: RouteAutoCollectResult | null) {
  const names = new Set((res?.items || []).filter(i => i.result === 'NO_COORD').map(i => i.customerName))
  if (names.size === 0) {
    missingGeoCustomers.value = []
    return
  }
  try {
    const list: any = await deliveryRouteApi.customerGeo({ limit: 500 })
    missingGeoCustomers.value = (Array.isArray(list) ? list : [])
      .filter((c: any) => names.has(c.customer_name) && (c.latitude == null || c.longitude == null))
      .map((c: any) => ({ ...c, __lat: undefined, __lng: undefined, __saving: false }))
  } catch (e) {
    console.warn('[配送路线单] 加载客户坐标失败', e)
  }
}

async function saveGeo(record: any) {
  if (record.__lat == null || record.__lng == null) {
    message.warning('请填写经纬度')
    return
  }
  record.__saving = true
  try {
    await deliveryRouteApi.saveCustomerGeo({
      customerId: record.customer_id,
      latitude: record.__lat,
      longitude: record.__lng,
    })
    message.success('已保存，可重新预览')
    await handlePreview()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    record.__saving = false
  }
}

// ── 手动添加 ──────────────────────────────────────────
async function loadDemands() {
  demandLoading.value = true
  try {
    const res: any = await deliveryRouteApi.demands({
      source: autoForm.source,
      keyword: manualForm.keyword || undefined,
      limit: 200,
    })
    demands.value = (Array.isArray(res) ? res : []).map((d: any) => ({ ...d, __key: `${d.sourceType}:${d.sourceId}` }))
  } catch (e: any) {
    message.error(e?.response?.data?.message || '加载配送需求失败')
  } finally {
    demandLoading.value = false
  }
}

function onSelectChange(keys: any[], rows: any[]) {
  selectedKeys.value = keys
  selectedDemands.value = rows
}

async function handleManualAdd() {
  if (!targetRouteId.value) {
    message.warning('请选择目标路线单')
    return
  }
  adding.value = true
  try {
    const res: any = await deliveryRouteApi.addPoints(targetRouteId.value, {
      replan: manualForm.replan,
      items: selectedDemands.value.map(d => ({ sourceType: d.sourceType, sourceId: d.sourceId })),
    })
    message.success(res?.message || '添加成功')
    selectedKeys.value = []
    selectedDemands.value = []
    emit('success')
    loadDemands()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '添加失败')
  } finally {
    adding.value = false
  }
}
</script>

<style scoped>
.tip { font-size: 12px; color: #666; margin-bottom: 10px; line-height: 20px; }
.row { margin-bottom: 10px; }
.geo-block { margin-top: 12px; border-top: 1px dashed #e8e8e8; padding-top: 10px; }
.geo-title { font-size: 12px; color: #d46b08; margin-bottom: 8px; display: flex; align-items: center; gap: 6px; }
</style>
