<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>性能监控</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">性能监控</h2>
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
      <!-- 实时性能指标 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="CPU使用率" :value="cpuUsage" suffix="%" :precision="1"
              :value-style="{ color: cpuUsage > 80 ? '#ff4d4f' : '#52c41a' }">
              <template #prefix><DashboardOutlined /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="内存使用率" :value="memUsage" suffix="%" :precision="1"
              :value-style="{ color: memUsage > 85 ? '#ff4d4f' : '#52c41a' }">
              <template #prefix><RocketOutlined /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="GC时间占比" :value="gcTimePercent" suffix="%" :precision="2"
              :value-style="{ color: gcTimePercent > 10 ? '#ff4d4f' : '#52c41a' }">
              <template #prefix><ClockCircleOutlined /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="活跃线程" :value="threadCount" :value-style="{ color: threadCount > 200 ? '#ff4d4f' : '#52c41a' }">
              <template #prefix><TeamOutlined /></template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- CPU详细信息 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="12">
          <a-card :bordered="false" title="CPU 详情">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="CPU核心数">{{ cpuCores }}</a-descriptions-item>
              <a-descriptions-item label="系统负载">{{ cpuLoadAverage }}</a-descriptions-item>
              <a-descriptions-item label="系统CPU使用率">{{ cpuSystemUsage }}%</a-descriptions-item>
              <a-descriptions-item label="进程CPU使用率">{{ cpuProcessUsage }}%</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card :bordered="false" title="GC 详情">
            <a-table :data-source="gcCollectors" :columns="gcColumns" row-key="name" :pagination="false" size="small" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 瓶颈分析 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="24">
          <a-card :bordered="false" title="性能瓶颈分析">
            <template #extra>
              <a-tag :color="bottleneckStatus === 'HEALTHY' ? 'green' : bottleneckStatus === 'WARNING' ? 'orange' : 'red'">
                {{ bottleneckStatus === 'HEALTHY' ? '健康' : bottleneckStatus === 'WARNING' ? '警告' : '严重' }}
              </a-tag>
            </template>
            <a-empty v-if="!bottlenecks.length" description="未检测到性能瓶颈" />
            <a-list v-else :data-source="bottlenecks" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      <a-tag :color="item.severity === 'CRITICAL' ? 'red' : 'orange'">{{ item.type }}</a-tag>
                      {{ item.recommendation }}
                    </template>
                    <template #description>
                      当前值: {{ item.currentValue }} | 阈值: {{ item.threshold }}
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>
      </a-row>

      <!-- 类加载 & 编译 -->
      <a-row :gutter="16">
        <a-col :span="12">
          <a-card :bordered="false" title="类加载信息">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="已加载类数">{{ classLoadedCount }}</a-descriptions-item>
              <a-descriptions-item label="累计加载类数">{{ classTotalLoadedCount }}</a-descriptions-item>
              <a-descriptions-item label="已卸载类数">{{ classUnloadedCount }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card :bordered="false" title="编译信息">
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="编译器">{{ compilationName }}</a-descriptions-item>
              <a-descriptions-item label="编译总耗时">{{ compilationTime }}ms</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ReloadOutlined, DashboardOutlined, RocketOutlined, ClockCircleOutlined, TeamOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)

// CPU
const cpuUsage = ref(0)
const cpuCores = ref(0)
const cpuLoadAverage = ref(0)
const cpuSystemUsage = ref(0)
const cpuProcessUsage = ref(0)

// 内存
const memUsage = ref(0)

// GC
const gcTimePercent = ref(0)
const gcCollectors = ref<any[]>([])

const gcColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'GC次数', dataIndex: 'count', key: 'count', width: 100 },
  { title: '总耗时(ms)', dataIndex: 'time', key: 'time', width: 120 },
]

// 线程
const threadCount = ref(0)

// 瓶颈
const bottlenecks = ref<any[]>([])
const bottleneckStatus = ref('HEALTHY')

// 类加载
const classLoadedCount = ref(0)
const classTotalLoadedCount = ref(0)
const classUnloadedCount = ref(0)

// 编译
const compilationName = ref('')
const compilationTime = ref(0)

let timer: any = null

async function fetchRealtimeMetrics() {
  try {
    const res = await request.get('/monitor/performance/realtime')
    if (!res) return

    // CPU
    if (res.cpu) {
      cpuUsage.value = Number(res.cpu.usage?.toFixed?.(1) || 0)
      cpuCores.value = res.cpu.cores || 0
      cpuLoadAverage.value = Number(res.cpu.loadAverage?.toFixed?.(2) || 0)
      cpuSystemUsage.value = Number(((res.cpu.systemUsage || 0) * 100).toFixed(1))
      cpuProcessUsage.value = Number(((res.cpu.processUsage || 0) * 100).toFixed(1))
    }

    // 内存
    if (res.memory) {
      memUsage.value = Number(((res.memory.usage || 0)).toFixed(1))
    }

    // GC
    if (res.gc) {
      gcTimePercent.value = Number(res.gc.gcTimePercent?.toFixed?.(2) || 0)
      gcCollectors.value = (res.gc.collectors || []).map((c: any) => ({
        name: c.name,
        count: c.count,
        time: c.time,
      }))
    }

    // 线程
    if (res.threads) {
      threadCount.value = res.threads.count || 0
    }

    // 类加载
    if (res.classLoading) {
      classLoadedCount.value = res.classLoading.loadedClassCount || 0
      classTotalLoadedCount.value = res.classLoading.totalLoadedClassCount || 0
      classUnloadedCount.value = res.classLoading.unloadedClassCount || 0
    }

    // 编译
    if (res.compilation) {
      compilationName.value = res.compilation.name || ''
      compilationTime.value = res.compilation.totalCompilationTime || 0
    }
  } catch { /* ignore */ }
}

async function fetchBottleneck() {
  try {
    const res = await request.get('/monitor/performance/bottleneck')
    if (res) {
      bottlenecks.value = res.bottlenecks || []
      bottleneckStatus.value = res.overallStatus || 'HEALTHY'
    }
  } catch { /* ignore */ }
}

async function refreshAll() {
  loading.value = true
  await Promise.all([fetchRealtimeMetrics(), fetchBottleneck()])
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
