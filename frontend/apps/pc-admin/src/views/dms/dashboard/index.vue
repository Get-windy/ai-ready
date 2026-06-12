<template>
  <ErrorBoundary @error="handleError"><PageContainer>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 仪表盘</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>DMS 仪表盘</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="debounceClick('refresh', initData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <template #default>
      <div class="dashboard-body">
        <!-- 统计卡片 -->
        <a-row :gutter="[16, 16]">
          <a-col :span="6">
            <div class="stat-card-wrapper">
              <StatCard title="车辆总数" :value="stats.totalVehicles ?? '-'" color="#1890ff" />
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card-wrapper">
              <StatCard title="活跃骑手" :value="stats.activeRiders ?? '-'" color="#52c41a" />
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card-wrapper">
              <StatCard title="待处理任务" :value="stats.pendingTasks ?? '-'" color="#faad14" />
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card-wrapper">
              <StatCard title="今日配送" :value="stats.todayDeliveries ?? '-'" color="#722ed1" />
            </div>
          </a-col>
        </a-row>

        <!-- 活跃数据卡片 -->
        <a-row :gutter="[16, 16]" style="margin-top: 16px;">
          <!-- 活跃绑定 -->
          <a-col :span="12">
            <div class="section-card">
              <div class="section-card__header">
                <h3>活跃绑定</h3>
                <a-button type="link" size="small" @click="refreshActiveBindings">刷新</a-button>
              </div>
              <SkeletonTable v-if="bindingsLoading && activeBindings.length === 0" :columns="bindingColumns.length" :rows="4" />
              <a-table
                v-else
                :dataSource="activeBindings"
                :columns="bindingColumns"
                :loading="bindingsLoading"
                rowKey="id"
                size="small"
                bordered
                :pagination="false as any"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'status'">
                    <a-tag :color="record.status === 1 ? 'blue' : 'green'">
                      {{ record.status === 1 ? '绑定中' : '已交车' }}
                    </a-tag>
                  </template>
                </template>
              </a-table>
              <div v-if="!bindingsLoading && (!activeBindings || activeBindings.length === 0)" class="empty-hint">
                暂无活跃绑定
              </div>
            </div>
          </a-col>

          <!-- 待处理预警 -->
          <a-col :span="12">
            <div class="section-card">
              <div class="section-card__header">
                <h3>待处理预警</h3>
                <a-button type="link" size="small" @click="refreshPendingAlerts">刷新</a-button>
              </div>
              <a-table
                :dataSource="pendingAlerts"
                :columns="alertColumns"
                :loading="alertsLoading"
                rowKey="id"
                size="small"
                bordered
                :pagination="false as any"
                :locale="locale"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'alertType'">
                    <a-tag :color="alertTypeTagMap[record.alertType]?.color || 'default'">
                      {{ alertTypeTagMap[record.alertType]?.text || record.alertType }}
                    </a-tag>
                  </template>
                  <template v-if="column.dataIndex === 'alertLevel'">
                    <a-tag :color="alertLevelMap[record.alertLevel]?.color || 'default'">
                      {{ alertLevelMap[record.alertLevel]?.text || record.alertLevel }}
                    </a-tag>
                  </template>
                </template>
              </a-table>
              <div v-if="!alertsLoading && (!pendingAlerts || pendingAlerts.length === 0)" class="empty-hint">
                暂无待处理预警
              </div>
            </div>
          </a-col>
        </a-row>

        <!-- 任务状态汇总 -->
        <a-row :gutter="[16, 16]" style="margin-top: 16px;">
          <a-col :span="12">
            <div class="section-card">
              <div class="section-card__header">
                <h3>任务状态汇总</h3>
                <a-button type="link" size="small" @click="refreshTaskSummary">刷新</a-button>
              </div>
              <a-table
                :dataSource="taskSummary"
                :columns="taskSummaryColumns"
                :loading="taskSummaryLoading"
                rowKey="id"
                size="small"
                bordered
                :pagination="false as any"
                :locale="locale"
              />
              <div v-if="!taskSummaryLoading && (!taskSummary || taskSummary.length === 0)" class="empty-hint">
                暂无任务数据
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </template>
  </PageContainer></ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import StatCard from '@/components/business/StatCard/StatCard.vue'
import { dashboardApi, type DmsDashboardStats } from '@/api/dms/dashboard'
import {
  ReloadOutlined, SyncOutlined
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS仪表盘]', err) }

const locale = { emptyText: '暂无数据' }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const loading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const bindingsLoading = ref(false)
const alertsLoading = ref(false)
const taskSummaryLoading = ref(false)

const stats = reactive<Record<string, any>>({
  totalVehicles: '-',
  activeRiders: '-',
  pendingTasks: '-',
  todayDeliveries: '-',
})

const activeBindings = ref<any[]>([])
const pendingAlerts = ref<any[]>([])
const taskSummary = ref<any[]>([])

const bindingColumns = [
  { title: '骑手', dataIndex: 'riderName', width: 100 },
  { title: '车牌号', dataIndex: 'plateNo', width: 110 },
  { title: '绑定时间', dataIndex: 'bindTime', width: 160 },
  { title: '状态', dataIndex: 'status', width: 80 },
]

const alertColumns = [
  { title: '类型', dataIndex: 'alertType', width: 100 },
  { title: '级别', dataIndex: 'alertLevel', width: 70 },
  { title: '内容', dataIndex: 'alertContent', ellipsis: true },
  { title: '时间', dataIndex: 'createTime', width: 160 },
]

const taskSummaryColumns = [
  { title: '状态', dataIndex: 'statusName', width: 100 },
  { title: '数量', dataIndex: 'count', width: 80 },
]

const alertTypeTagMap: Record<number, { text: string; color: string }> = {
  1: { text: '人车分离', color: 'red' },
  2: { text: '异常滞留', color: 'orange' },
  3: { text: '绑定超时', color: 'gold' },
}

const alertLevelMap: Record<number, { text: string; color: string }> = {
  1: { text: '低', color: 'green' },
  2: { text: '中', color: 'orange' },
  3: { text: '高', color: 'red' },
}

async function fetchStats() {
  try {
    const res: any = await dashboardApi.stats()
    const data = res?.data ?? res
    if (data) {
      Object.assign(stats, {
        totalVehicles: data.totalVehicles ?? data.totalVehicles ?? '-',
        activeRiders: data.activeRiders ?? '-',
        pendingTasks: data.pendingTasks ?? data.pendingOrders ?? '-',
        todayDeliveries: data.todayDeliveries ?? data.todayOrders ?? '-',
      })
    }
  } catch (err: any) {
    console.warn('[DMS仪表盘] 获取统计失败', err)
  }
}

async function fetchActiveBindings() {
  bindingsLoading.value = true
  try {
    const res: any = await dashboardApi.activeBindings()
    activeBindings.value = res?.data ?? res ?? []
  } catch { activeBindings.value = [] }
  finally { bindingsLoading.value = false }
}

async function fetchPendingAlerts() {
  alertsLoading.value = true
  try {
    const res: any = await dashboardApi.pendingAlerts()
    pendingAlerts.value = res?.data ?? res ?? []
  } catch { pendingAlerts.value = [] }
  finally { alertsLoading.value = false }
}

async function fetchTaskSummary() {
  taskSummaryLoading.value = true
  try {
    const res: any = await dashboardApi.taskSummary()
    taskSummary.value = res?.data ?? res ?? []
  } catch { taskSummary.value = [] }
  finally { taskSummaryLoading.value = false }
}

function refreshActiveBindings() { fetchActiveBindings() }
function refreshPendingAlerts() { fetchPendingAlerts() }
function refreshTaskSummary() { fetchTaskSummary() }

async function initData() {
  loading.value = true
  await Promise.all([
    fetchStats(),
    fetchActiveBindings(),
    fetchPendingAlerts(),
    fetchTaskSummary(),
  ])
  loading.value = false
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', initData)
  }
}

onMounted(() => {
  initData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => { initData(); autoRefreshCountdown.value = 30 }, 30000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }

.dashboard-body { padding: 0; }

.stat-card-wrapper {
  background: #fff;
  border-radius: 6px;
  padding: 16px 20px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.section-card {
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e8e8e8;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.section-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-card__header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.empty-hint {
  text-align: center;
  padding: 24px 0;
  color: #999;
  font-size: 13px;
}
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }
@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
