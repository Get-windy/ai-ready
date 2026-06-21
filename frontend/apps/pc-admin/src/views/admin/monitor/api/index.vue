<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>接口监控</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">接口监控</h2>
        </div>
        <div class="page-header-right">
          <a-button size="small" @click="refreshAll" :loading="loading">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-spin :spinning="loading">
      <!-- 监控仪表盘汇总 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="系统状态" value="正常" :value-style="{ color: '#52c41a' }">
              <template #prefix><CheckCircleOutlined style="color:#52c41a" /></template>
            </a-statistic>
            <div class="stat-detail">CPU: {{ dashboardCpu }}% | 内存: {{ dashboardMem }}% | 磁盘: {{ dashboardDisk }}%</div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="服务健康" :value="servicesHealthy" suffix="/ {{ servicesTotal }}">
              <template #prefix><CloudServerOutlined /></template>
            </a-statistic>
            <div class="stat-detail">警告: {{ servicesWarning }} | 严重: {{ servicesCritical }}</div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="告警(24h)" :value="alertsTotal24h" :value-style="{ color: alertsUnacknowledged > 0 ? '#faad14' : '' }">
              <template #prefix><AlertOutlined /></template>
            </a-statistic>
            <div class="stat-detail">未确认: {{ alertsUnacknowledged }} | 严重: {{ alertsCritical }}</div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="性能" :value="avgResponseTime" suffix="ms">
              <template #prefix><ThunderboltOutlined /></template>
            </a-statistic>
            <div class="stat-detail">吞吐: {{ throughput }} | 错误率: {{ errorRate }}</div>
          </a-card>
        </a-col>
      </a-row>

      <!-- API端点列表 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="24">
          <a-card :bordered="false" title="API端点概览">
            <a-collapse v-model:activeKey="activeApiKeys">
              <a-collapse-panel v-for="(ep, key) in apiEndpoints" :key="key" :header="`${ep.basePath} - ${ep.description}`">
                <a-table :data-source="ep.endpoints || []" :columns="apiColumns" row-key="path" :pagination="false" size="small" />
              </a-collapse-panel>
            </a-collapse>
          </a-card>
        </a-col>
      </a-row>

      <!-- 24h告警历史 -->
      <a-row :gutter="16">
        <a-col :span="24">
          <a-card :bordered="false" title="告警历史(24h)">
            <a-row :gutter="16">
              <a-col :span="6">
                <a-statistic title="告警总数" :value="alertsTotal24h" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="严重告警" :value="alertsCritical" :value-style="{ color: 'red' }" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="警告" :value="alertsWarning" :value-style="{ color: '#faad14' }" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="信息" :value="alertsInfo" :value-style="{ color: '#1890ff' }" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ReloadOutlined, CheckCircleOutlined, CloudServerOutlined, AlertOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const activeApiKeys = ref<string[]>([])

// Dashboard
const dashboardCpu = ref(0)
const dashboardMem = ref(0)
const dashboardDisk = ref(0)

// 服务
const servicesTotal = ref(0)
const servicesHealthy = ref(0)
const servicesWarning = ref(0)
const servicesCritical = ref(0)

// 告警
const alertsTotal24h = ref(0)
const alertsUnacknowledged = ref(0)
const alertsCritical = ref(0)
const alertsWarning = ref(0)
const alertsInfo = ref(0)

// 性能
const avgResponseTime = ref('0')
const throughput = ref('0')
const errorRate = ref('0%')

// API 端点
const apiEndpoints = ref<Record<string, any>>({})
const apiColumns = [
  { title: '路径', dataIndex: 'path', key: 'path' },
  { title: '方法', dataIndex: 'method', key: 'method', width: 80 },
  { title: '描述', dataIndex: 'description', key: 'description' },
]

let timer: any = null

async function fetchDashboard() {
  try {
    const res = await request.get('/monitor/dashboard')
    if (res) {
      if (res.systemStatus) {
        dashboardCpu.value = Number(res.systemStatus.cpuUsage?.toFixed?.(1) ?? res.systemStatus.cpuUsage ?? 0)
        dashboardMem.value = Number(res.systemStatus.memoryUsage?.toFixed?.(1) ?? res.systemStatus.memoryUsage ?? 0)
        dashboardDisk.value = Number(res.systemStatus.diskUsage?.toFixed?.(1) ?? res.systemStatus.diskUsage ?? 0)
      }
      if (res.servicesStatus) {
        servicesTotal.value = res.servicesStatus.total || 0
        servicesHealthy.value = res.servicesStatus.healthy || 0
        servicesWarning.value = res.servicesStatus.warning || 0
        servicesCritical.value = res.servicesStatus.critical || 0
      }
      if (res.alertsSummary) {
        alertsTotal24h.value = res.alertsSummary.total24h || 0
        alertsUnacknowledged.value = res.alertsSummary.unacknowledged || 0
        alertsCritical.value = res.alertsSummary.critical || 0
        alertsWarning.value = res.alertsSummary.warning || 0
      }
      if (res.performanceSummary) {
        avgResponseTime.value = res.performanceSummary.avgResponseTime || '0'
        throughput.value = res.performanceSummary.throughput || '0'
        errorRate.value = res.performanceSummary.errorRate || '0%'
      }
    }
  } catch { /* ignore */ }
}

async function fetchApiInfo() {
  try {
    const res = await request.get('/monitor/info')
    if (res && res.endpoints) {
      const filtered: Record<string, any> = {}
      for (const [key, val] of Object.entries(res.endpoints)) {
        filtered[key] = val
      }
      apiEndpoints.value = filtered
      activeApiKeys.value = Object.keys(filtered)
    }
  } catch { /* ignore */ }
}

async function refreshAll() {
  loading.value = true
  await Promise.all([fetchDashboard(), fetchApiInfo()])
  loading.value = false
}

onMounted(() => {
  refreshAll()
  timer = setInterval(refreshAll, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.stat-detail {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
