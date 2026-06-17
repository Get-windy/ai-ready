<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>服务状态</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">服务状态</h2>
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
      <!-- 综合健康状态 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="24">
          <a-card :bordered="false">
            <template #title>综合健康状态</template>
            <template #extra>
              <a-tag :color="healthStatus === 'UP' ? 'green' : healthStatus === 'WARNING' ? 'orange' : 'red'">
                {{ healthStatus === 'UP' ? '正常' : healthStatus === 'WARNING' ? '警告' : '异常' }}
              </a-tag>
            </template>
            <a-row :gutter="[16,16]">
              <a-col :span="6">
                <a-statistic title="健康分数" :value="healthScore" suffix="分">
                  <template #prefix><CheckCircleOutlined v-if="healthScore >= 90" style="color:#52c41a" /></template>
                </a-statistic>
              </a-col>
              <a-col :span="6">
                <a-statistic title="服务名称" value="ai-ready-core-api" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="运行状态" value="运行中">
                  <template #prefix><SyncOutlined spin style="color:#1890ff" /></template>
                </a-statistic>
              </a-col>
              <a-col :span="6">
                <a-statistic title="服务时间" :value="currentTime" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>

      <!-- 依赖服务状态 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="24">
          <a-card :bordered="false" title="依赖服务状态">
            <a-row :gutter="[16,16]">
              <a-col :span="8" v-for="dep in dependencies" :key="dep.name">
                <a-card size="small" :style="{ borderLeft: `3px solid ${dep.status === 'UP' ? '#52c41a' : '#ff4d4f'}` }">
                  <a-row align="middle">
                    <a-col flex="auto">
                      <div style="font-weight:500">{{ dep.name }}</div>
                      <div style="font-size:12px;color:#999">{{ dep.status === 'UP' ? '正常运行' : '服务异常' }}</div>
                    </a-col>
                    <a-col>
                      <a-badge :status="dep.status === 'UP' ? 'success' : 'error'" />
                    </a-col>
                  </a-row>
                </a-card>
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>

      <!-- JVM 状态 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="24">
          <a-card :bordered="false" title="JVM 运行状态">
            <a-row :gutter="[16,16]">
              <a-col :span="6">
                <a-statistic title="堆内存使用" :value="jvmHeapUsage" suffix="%" :value-style="{ color: jvmHeapUsage > 80 ? '#ff4d4f' : '#52c41a' }" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="已用堆内存" :value="formatBytes(jvmHeapUsed)" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="最大堆内存" :value="formatBytes(jvmHeapMax)" />
              </a-col>
              <a-col :span="6">
                <a-statistic title="线程数" :value="jvmThreadCount" />
              </a-col>
            </a-row>
          </a-card>
        </a-col>
      </a-row>

      <!-- 磁盘状态 -->
      <a-row :gutter="16">
        <a-col :span="24">
          <a-card :bordered="false" title="磁盘状态">
            <a-table :data-source="disks" :columns="diskColumns" row-key="path" :pagination="false" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'usagePercent'">
                  <a-progress :percent="record.usagePercent" size="small" :status="record.usagePercent > 90 ? 'exception' : 'active'" />
                </template>
                <template v-if="column.key === 'status'">
                  <a-tag :color="record.status === 'UP' ? 'green' : 'red'">{{ record.status === 'UP' ? '正常' : '异常' }}</a-tag>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ReloadOutlined, CheckCircleOutlined, SyncOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const healthStatus = ref('UP')
const healthScore = ref(100)
const currentTime = ref('')
const dependencies = ref<any[]>([])
const jvmHeapUsage = ref(0)
const jvmHeapUsed = ref(0)
const jvmHeapMax = ref(0)
const jvmThreadCount = ref(0)
const disks = ref<any[]>([])

const diskColumns = [
  { title: '路径', dataIndex: 'path', key: 'path', width: 200 },
  { title: '总空间', dataIndex: 'totalSpace', key: 'totalSpace', width: 120 },
  { title: '已用空间', dataIndex: 'usedSpace', key: 'usedSpace', width: 120 },
  { title: '空闲空间', dataIndex: 'freeSpace', key: 'freeSpace', width: 120 },
  { title: '使用率', key: 'usagePercent', width: 180 },
  { title: '状态', key: 'status', width: 80 },
]

let timer: any = null

function formatBytes(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

async function fetchHealthStatus() {
  try {
    const res = await request.get('/monitor/health/status')
    if (res) {
      healthStatus.value = res.status || 'UP'
      healthScore.value = res.healthScore || 100
    }
  } catch { /* ignore */ }
}

async function fetchJvmHealth() {
  try {
    const res = await request.get('/monitor/health/jvm')
    if (res && res.heapMemory) {
      jvmHeapUsed.value = res.heapMemory.used || 0
      jvmHeapMax.value = res.heapMemory.max || 1
      jvmHeapUsage.value = res.heapMemory.usagePercent || 0
    }
    if (res && res.threads) {
      jvmThreadCount.value = res.threads.threadCount || 0
    }
  } catch { /* ignore */ }
}

async function fetchDiskHealth() {
  try {
    const res = await request.get('/monitor/health/disk')
    if (res && res.disks) {
      disks.value = res.disks.map((d: any) => ({
        ...d,
        totalSpace: formatBytes(d.totalSpace),
        usedSpace: formatBytes(d.usedSpace),
        freeSpace: formatBytes(d.freeSpace),
        usagePercent: Number(d.usagePercent?.toFixed?.(1) || 0),
      }))
    }
  } catch { /* ignore */ }
}

async function fetchDependencies() {
  try {
    const res = await request.get('/monitor/health/dependencies')
    if (res && res.dependencies) {
      dependencies.value = Object.entries(res.dependencies).map(([key, val]: any) => ({
        name: val.name || key,
        status: val.status || 'UP',
      }))
    }
  } catch { /* ignore */ }
}

async function refreshAll() {
  loading.value = true
  currentTime.value = new Date().toLocaleTimeString()
  await Promise.all([fetchHealthStatus(), fetchJvmHealth(), fetchDiskHealth(), fetchDependencies()])
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
