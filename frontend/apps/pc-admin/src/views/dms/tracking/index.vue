<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 轨迹追踪</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>轨迹追踪</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <template #filter>
      <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />
    </template>

    <template #default>
      <div class="page-body">
        <SkeletonTable v-if="loading && dataList.length === 0" :columns="columns.length" :rows="8" />
        <a-table
          v-else
          :dataSource="dataList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          :locale="locale"
          rowKey="id"
          size="small"
          bordered
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'speed'">
              {{ record.speed != null ? record.speed.toFixed(1) + ' km/h' : '-' }}
            </template>
            <template v-if="column.dataIndex === 'direction'">
              {{ formatDirection(record.direction) }}
            </template>
            <template v-if="column.dataIndex === 'reportTime'">
              {{ formatDateTime(record.reportTime) }}
            </template>
            <template v-if="column.dataIndex === 'lat'">
              {{ record.lat != null ? record.lat.toFixed(6) : '-' }}
            </template>
            <template v-if="column.dataIndex === 'lng'">
              {{ record.lng != null ? record.lng.toFixed(6) : '-' }}
            </template>
          </template>
        </a-table>
      </div>
    </template>
  </PageContainer></ErrorBoundary>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import { trackingApi } from '@/api/dms/tracking'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  ReloadOutlined, SyncOutlined
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS追踪]', err) }

const locale = { emptyText: '暂无轨迹数据' }

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('zh-CN')
  } catch { return dateStr }
}

const columns = [
  { title: '骑手ID', dataIndex: 'riderId', width: 80 },
  { title: '骑手姓名', dataIndex: 'riderName', width: 110 },
  { title: '任务ID', dataIndex: 'taskId', width: 80 },
  { title: '经度', dataIndex: 'lng', width: 110 },
  { title: '纬度', dataIndex: 'lat', width: 110 },
  { title: '速度', dataIndex: 'speed', width: 100 },
  { title: '方向', dataIndex: 'direction', width: 80 },
  { title: '上报时间', dataIndex: 'reportTime', width: 170 },
  { title: '来源', dataIndex: 'source', width: 100 },
]

const searchFields: SearchField[] = [
  { name: 'riderId', label: '骑手ID', type: 'input', placeholder: '请输入骑手ID' },
  { name: 'taskId', label: '任务ID', type: 'input', placeholder: '请输入任务ID' },
  { name: 'startTime', label: '开始时间', type: 'date-range', placeholder: '选择开始时间' },
  { name: 'endTime', label: '结束时间', type: 'date-range', placeholder: '选择结束时间' },
]

const wms = useWmsTable({
  fetchFn: trackingApi.page as any,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown, debounce } = wms

function formatDirection(direction: number): string {
  if (direction == null) return '-'
  const dirs = ['北', '东北', '东', '东南', '南', '西南', '西', '西北']
  const index = Math.round(direction / 45) % 8
  return dirs[index] || '-'
}

function handleSearch(formData: Record<string, any>) {
  Object.assign(searchParams, formData)
  wms.handleSearch()
}

function handleReset() {
  Object.keys(searchParams).forEach(k => { searchParams[k] = undefined })
  wms.handleReset()
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.page-body { padding: 0; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

/* 打印样式 */
@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
