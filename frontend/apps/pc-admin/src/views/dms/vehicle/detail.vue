<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item><router-link to="/dms/vehicle">车辆管理</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>车辆详情</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>车辆详情</h2>
        </div>
        <div class="page-header__right">
          <a-button size="small" :loading="loading" @click="fetchDetail">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button size="small" @click="goBack"><ArrowLeftOutlined /> 返回</a-button>
        </div>
      </div>
    </template>

    <template #default>
      <div v-if="loading" class="loading-wrapper">
        <a-spin size="large" />
      </div>
      <div v-else class="page-body">
        <!-- Tabs -->
        <a-tabs v-model:activeKey="activeTab">
          <!-- Tab 1: 基本信息 -->
          <a-tab-pane key="basic" tab="基本信息">
            <a-descriptions :column="2" bordered size="small" style="background:#fff;">
              <a-descriptions-item label="车牌号" :span="1">{{ vehicle.plateNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="车辆类型" :span="1">{{ vehicleTypeMap[vehicle.vehicleType]?.text || '-' }}</a-descriptions-item>
              <a-descriptions-item label="品牌" :span="1">{{ vehicle.brand || '-' }}</a-descriptions-item>
              <a-descriptions-item label="型号" :span="1">{{ vehicle.model || '-' }}</a-descriptions-item>
              <a-descriptions-item label="颜色" :span="1">{{ vehicle.color || '-' }}</a-descriptions-item>
              <a-descriptions-item label="车架号(VIN)" :span="1">{{ vehicle.vin || '-' }}</a-descriptions-item>
              <a-descriptions-item label="发动机号" :span="1">{{ vehicle.engineNo || '-' }}</a-descriptions-item>
              <a-descriptions-item label="车辆状态" :span="1">
                <a-tag :color="statusMap[vehicle.status]?.color">{{ statusMap[vehicle.status]?.text || vehicle.status }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="当前骑手" :span="1">{{ vehicle.riderName || '-' }}</a-descriptions-item>
              <a-descriptions-item label="车队长" :span="1">{{ vehicle.vehicleManagerName || '-' }}</a-descriptions-item>
              <a-descriptions-item label="所属性质" :span="1">{{ ownershipMap[vehicle.ownershipType] || '-' }}</a-descriptions-item>
              <a-descriptions-item label="所属部门" :span="1">{{ vehicle.department || '-' }}</a-descriptions-item>
              <a-descriptions-item label="核定载重(kg)" :span="1">{{ vehicle.ratedLoad ?? '-' }}</a-descriptions-item>
              <a-descriptions-item label="核定载客" :span="1">{{ vehicle.ratedPassenger ?? '-' }}</a-descriptions-item>
              <a-descriptions-item label="货箱容积(m³)" :span="1">{{ vehicle.cargoVolume ?? '-' }}</a-descriptions-item>
              <a-descriptions-item label="保养间隔(km)" :span="1">{{ vehicle.maintenanceIntervalKm ?? '-' }}</a-descriptions-item>
              <a-descriptions-item label="注册日期" :span="1">{{ vehicle.registerDate?.slice(0, 10) || '-' }}</a-descriptions-item>
              <a-descriptions-item label="保险到期" :span="1">
                <a-tag :color="getExpireInfo(vehicle.insuranceExpireDate)?.color">
                  {{ getExpireInfo(vehicle.insuranceExpireDate)?.text || vehicle.insuranceExpireDate?.slice(0, 10) || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="年检到期" :span="1">
                <a-tag :color="getExpireInfo(vehicle.inspectionExpireDate)?.color">
                  {{ getExpireInfo(vehicle.inspectionExpireDate)?.text || vehicle.inspectionExpireDate?.slice(0, 10) || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="备注" :span="2">{{ vehicle.remark || '-' }}</a-descriptions-item>
            </a-descriptions>
          </a-tab-pane>

          <!-- Tab 2: 维保记录 -->
          <a-tab-pane key="maintenance" tab="维保记录">
            <VehicleMaintenance :vehicleId="detailId" :embedded="true" />
          </a-tab-pane>

          <!-- Tab 3: 绑定历史 -->
          <a-tab-pane key="binding" tab="绑定历史">
            <div class="section-header">
              <h4>绑定记录</h4>
              <a-button size="small" :loading="bindingLoading" @click="fetchBindingHistory"><ReloadOutlined /> 刷新</a-button>
            </div>
            <a-table
              :dataSource="bindingHistory"
              :columns="bindingColumns"
              :loading="bindingLoading"
              rowKey="id"
              size="small"
              bordered
              :pagination="false as any"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'status'">
                  <a-tag :color="record.status === 1 ? 'blue' : 'default'">
                    {{ record.status === 1 ? '绑定中' : record.handoverTime ? '已交车' : '已解绑' }}
                  </a-tag>
                </template>
              </template>
            </a-table>
            <div v-if="!bindingLoading && (!bindingHistory || bindingHistory.length === 0)" class="empty-hint">
              暂无绑定历史
            </div>
          </a-tab-pane>
        </a-tabs>
      </div>
    </template>
  </PageContainer></ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VehicleMaintenance from './maintenance.vue'
import { vehicleApi, type DmsVehicle, type DmsBindingHistory } from '@/api/dms/vehicle'
import {
  ReloadOutlined, ArrowLeftOutlined
} from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'

function handleError(err: any) { console.warn('[DMS车辆详情]', err) }

const route = useRoute()
const router = useRouter()
const detailId = Number(route.params.id || route.query.id)

const loading = ref(false)
const activeTab = ref('basic')

const vehicle = reactive<Record<string, any>>({})

const bindingLoading = ref(false)
const bindingHistory = ref<DmsBindingHistory[]>([])

const vehicleTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '电动车', color: 'green' },
  2: { text: '小货车', color: 'blue' },
  3: { text: '面包车', color: 'orange' },
  4: { text: '厢式货车', color: 'purple' },
  5: { text: '冷藏车', color: 'cyan' },
  6: { text: '三轮车', color: 'gold' },
}

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '空闲', color: 'green' },
  1: { text: '使用中', color: 'blue' },
  2: { text: '维修中', color: 'orange' },
  3: { text: '已报废', color: 'red' },
}

const ownershipMap: Record<number, string> = {
  1: '公司自有', 2: '个人自带', 3: '租赁',
}

function getExpireInfo(dateStr?: string): { text: string; color: string } | null {
  if (!dateStr) return null
  const now = new Date()
  const expire = new Date(dateStr)
  const diff = Math.ceil((expire.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  if (diff < 0) return { text: `已过期 (${dateStr.slice(0, 10)})`, color: 'red' }
  if (diff <= 30) return { text: `即将到期 (${diff}天)`, color: 'orange' }
  return null
}

const bindingColumns = [
  { title: '骑手ID', dataIndex: 'riderId', width: 80 },
  { title: '骑手姓名', dataIndex: 'riderName', width: 100 },
  { title: '绑定时间', dataIndex: 'bindTime', width: 170 },
  { title: '交车时间', dataIndex: 'handoverTime', width: 170 },
  { title: '状态', dataIndex: 'status', width: 80 },
]

async function fetchDetail() {
  if (!detailId) return
  loading.value = true
  try {
    const res: any = await vehicleApi.getById(detailId)
    const data = res?.data ?? res
    if (data) Object.assign(vehicle, data)
  } catch (err: any) {
    message.error(err?.message || '获取车辆详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchBindingHistory() {
  if (!detailId) return
  bindingLoading.value = true
  try {
    const res: any = await vehicleApi.bindingHistory(detailId)
    bindingHistory.value = res?.data ?? res ?? []
  } catch {
    bindingHistory.value = []
  } finally {
    bindingLoading.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchDetail()
  fetchBindingHistory()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.page-body { padding: 0; }
.loading-wrapper { display: flex; justify-content: center; align-items: center; min-height: 200px; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.section-header h4 { margin: 0; font-size: 14px; font-weight: 600; color: #303133; }
.empty-hint { text-align: center; padding: 24px 0; color: #999; font-size: 13px; }
</style>
