<template>
  <ErrorBoundary>
    <PageContainer>
      <template #header>
        <div class="page-header">
          <span></span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </template>
      <a-tabs v-model:activeKey="activeTab">
        <!-- Route Planning -->
        <a-tab-pane key="plan" tab="路线规划">
          <a-card title="路线规划" style="max-width: 800px">
            <a-form layout="vertical">
              <a-form-item label="起点地址" required>
                <a-input v-model:value="planForm.origin.address" placeholder="请输入起点地址" />
              </a-form-item>
              <a-form-item label="起点纬度">
                <a-input v-model:value="planForm.origin.lat" placeholder="起点纬度" />
              </a-form-item>
              <a-form-item label="起点经度">
                <a-input v-model:value="planForm.origin.lng" placeholder="起点经度" />
              </a-form-item>

              <a-divider>目的地</a-divider>

              <div v-for="(dest, index) in planForm.destinations" :key="index" style="margin-bottom: 12px; border: 1px solid #f0f0f0; padding: 12px; border-radius: 6px;">
                <a-space style="margin-bottom: 8px;">
                  <strong>目的地 {{ index + 1 }}</strong>
                  <a-button type="link" danger size="small" @click="removeDestination(index)" v-if="planForm.destinations.length > 1">
                    删除
                  </a-button>
                </a-space>
                <a-form-item :label="'地址 ' + (index + 1)">
                  <a-input v-model:value="dest.address" placeholder="请输入地址" />
                </a-form-item>
                <a-space>
                  <a-form-item label="纬度">
                    <a-input v-model:value="dest.lat" placeholder="纬度" />
                  </a-form-item>
                  <a-form-item label="经度">
                    <a-input v-model:value="dest.lng" placeholder="经度" />
                  </a-form-item>
                </a-space>
              </div>

              <a-button type="dashed" block @click="addDestination" style="margin-bottom: 16px;">
                + 添加目的地
              </a-button>

              <a-button type="primary" @click="handlePlanRoute" :loading="planLoading" block>
                规划路线
              </a-button>
            </a-form>

            <!-- Results -->
            <a-card v-if="planResult.stops.length > 0" title="规划结果" style="margin-top: 16px">
              <a-descriptions :column="2">
                <a-descriptions-item label="总距离">{{ planResult.total_distance }} km</a-descriptions-item>
                <a-descriptions-item label="总步数">{{ planResult.steps?.length || 0 }} 段</a-descriptions-item>
              </a-descriptions>
              <a-table
                :data-source="planResult.stops"
                :columns="stopColumns"
                :locale="locale"
                row-key="index"
                :pagination="false as any"
                size="small"
                style="margin-top: 12px"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'type'">
                    <a-tag :color="record.type === 'start' ? 'blue' : 'green'">
                      {{ record.type === 'start' ? '起点' : '目的地' }}
                    </a-tag>
                  </template>
                </template>
              </a-table>
            </a-card>
          </a-card>
        </a-tab-pane>

        <!-- Geocode -->
        <a-tab-pane key="geocode" tab="地址编码">
          <a-card title="地址 → 经纬度" style="max-width: 600px">
            <a-form layout="vertical">
              <a-form-item label="地址" required>
                <a-input v-model:value="geocodeForm.address" placeholder="请输入地址" />
              </a-form-item>
              <a-button type="primary" @click="handleGeocode" :loading="geocodeLoading">
                查询
              </a-button>
            </a-form>
            <a-card v-if="geocodeResult" size="small" style="margin-top: 12px">
              <a-descriptions :column="1">
                <a-descriptions-item label="纬度">{{ geocodeResult.lat }}</a-descriptions-item>
                <a-descriptions-item label="经度">{{ geocodeResult.lng }}</a-descriptions-item>
                <a-descriptions-item label="地址">{{ geocodeResult.address }}</a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-card>
        </a-tab-pane>

        <!-- Reverse Geocode -->
        <a-tab-pane key="reverse" tab="逆编码">
          <a-card title="经纬度 → 地址" style="max-width: 600px">
            <a-form layout="vertical">
              <a-form-item label="纬度" required>
                <a-input-number v-model:value="reverseForm.lat" style="width: 100%" :precision="6" />
              </a-form-item>
              <a-form-item label="经度" required>
                <a-input-number v-model:value="reverseForm.lng" style="width: 100%" :precision="6" />
              </a-form-item>
              <a-button type="primary" @click="handleReverseGeocode" :loading="reverseLoading">
                查询
              </a-button>
            </a-form>
            <a-card v-if="reverseResult" size="small" style="margin-top: 12px">
              <a-descriptions :column="1">
                <a-descriptions-item label="地址">{{ reverseResult }}</a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-card>
        </a-tab-pane>

        <!-- Fence Check -->
        <a-tab-pane key="fence" tab="围栏检查">
          <a-card title="围栏检查" style="max-width: 600px">
            <a-form layout="vertical">
              <a-form-item label="骑手纬度" required>
                <a-input-number v-model:value="fenceForm.rider_lat" style="width: 100%" :precision="6" />
              </a-form-item>
              <a-form-item label="骑手经度" required>
                <a-input-number v-model:value="fenceForm.rider_lng" style="width: 100%" :precision="6" />
              </a-form-item>
              <a-form-item label="半径(米)" required>
                <a-input-number v-model:value="fenceForm.radius" style="width: 100%" :min="1" />
              </a-form-item>
              <a-button type="primary" @click="handleFenceCheck" :loading="fenceLoading">
                检查
              </a-button>
            </a-form>
            <a-card v-if="fenceResult !== null" size="small" style="margin-top: 12px">
              <a-tag :color="fenceResult ? 'green' : 'red'">
                {{ fenceResult ? '在围栏内' : '在围栏外' }}
              </a-tag>
            </a-card>
          </a-card>
        </a-tab-pane>
      </a-tabs>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { request } from '@/utils/request'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

const locale = { emptyText: '暂无路线数据' }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    message.info('请使用页面上的按钮执行操作')
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

const activeTab = ref('plan')

// ---- Route Planning ----
interface Destination {
  address: string
  lat: string
  lng: string
}

const planForm = reactive({
  origin: { address: '', lat: '', lng: '' },
  destinations: [{ address: '', lat: '', lng: '' }] as Destination[]
})

const planLoading = ref(false)

const planResult = reactive<{
  stops: any[]
  total_distance: number | string
  steps: any[]
}>({
  stops: [],
  total_distance: '',
  steps: []
})

const stopColumns = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 60 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '地址', dataIndex: 'address', key: 'address' },
  { title: '纬度', dataIndex: 'lat', key: 'lat', width: 120 },
  { title: '经度', dataIndex: 'lng', key: 'lng', width: 120 }
]

const addDestination = () => {
  planForm.destinations.push({ address: '', lat: '', lng: '' })
}

const removeDestination = (index: number) => {
  planForm.destinations.splice(index, 1)
}

const handlePlanRoute = async () => {
  if (!planForm.origin.address) {
    message.warning('请输入起点地址')
    return
  }
  if (planForm.destinations.length === 0) {
    message.warning('请添加至少一个目的地')
    return
  }
  planLoading.value = true
  try {
    const res = await request.post('/dms/route/plan', {
      origin: planForm.origin,
      destinations: planForm.destinations.filter((d) => d.address)
    })
    const data = res?.data ?? res ?? {}
    planResult.stops = data.stops ?? data.ordered_stops ?? []
    planResult.total_distance = data.total_distance ?? ''
    planResult.steps = data.steps ?? []
    message.success('路线规划成功')
  } catch (err: any) {
    message.error(err?.message || '路线规划失败')
  } finally {
    planLoading.value = false
  }
}

// ---- Geocode ----
const geocodeForm = reactive({
  address: ''
})
const geocodeLoading = ref(false)
const geocodeResult = ref<any>(null)

const handleGeocode = async () => {
  if (!geocodeForm.address) {
    message.warning('请输入地址')
    return
  }
  geocodeLoading.value = true
  try {
    const res = await request.get('/dms/geocode', {
      params: { address: geocodeForm.address }
    })
    geocodeResult.value = res?.data ?? res ?? null
  } catch (err: any) {
    message.error(err?.message || '地址编码失败')
  } finally {
    geocodeLoading.value = false
  }
}

// ---- Reverse Geocode ----
const reverseForm = reactive({
  lat: undefined as number | undefined,
  lng: undefined as number | undefined
})
const reverseLoading = ref(false)
const reverseResult = ref<string | null>(null)

const handleReverseGeocode = async () => {
  if (reverseForm.lat == null || reverseForm.lng == null) {
    message.warning('请输入纬度和经度')
    return
  }
  reverseLoading.value = true
  try {
    const res = await request.get('/dms/reverse-geocode', {
      params: { lat: reverseForm.lat, lng: reverseForm.lng }
    })
    reverseResult.value = res?.data?.address ?? res?.data ?? null
  } catch (err: any) {
    message.error(err?.message || '逆编码失败')
  } finally {
    reverseLoading.value = false
  }
}

// ---- Fence Check ----
const fenceForm = reactive({
  rider_lat: undefined as number | undefined,
  rider_lng: undefined as number | undefined,
  radius: undefined as number | undefined
})
const fenceLoading = ref(false)
const fenceResult = ref<boolean | null>(null)

const handleFenceCheck = async () => {
  if (fenceForm.rider_lat == null || fenceForm.rider_lng == null || fenceForm.radius == null) {
    message.warning('请填写完整的围栏检查参数')
    return
  }
  fenceLoading.value = true
  try {
    const res = await request.post('/dms/fence/check', {
      rider_lat: fenceForm.rider_lat,
      rider_lng: fenceForm.rider_lng,
      radius: fenceForm.radius
    })
    fenceResult.value = res?.data?.inside ?? res?.data ?? false
  } catch (err: any) {
    message.error(err?.message || '围栏检查失败')
  } finally {
    fenceLoading.value = false
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }

@media print {
  .page-header .shortcut-hints { display: none !important; }
}
</style>
