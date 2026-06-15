<template>
  <ErrorBoundary @error="handleError">
    <PageContainer>
      <template #header>
        <div class="page-header">
          <SearchBar
            :fields="searchFields"
            @search="handleSearch"
            @reset="handleReset"
          />
          <div class="page-header__right">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
          </div>
        </div>
      </template>

      <SkeletonTable v-if="loading && tableData.length === 0" :columns="columns.length" :rows="8" />
      <a-table
        v-else
        :data-source="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #emptyText>
          <a-empty description="暂无订单数据">
            <a-button size="small" @click="wms.fetchData">刷新</a-button>
          </a-empty>
        </template>
        <template #bodyCell="{ column, record }">
          <!-- published_time -->
          <template v-if="column.key === 'published_time'">
            {{ formatDateTime(record.published_time) }}
          </template>
          <!-- bid_enabled -->
          <template v-if="column.key === 'bid_enabled'">
            <a-tag :color="record.bid_enabled ? 'green' : 'default'">
              {{ record.bid_enabled ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <!-- pool_status -->
          <template v-if="column.key === 'pool_status'">
            <a-tag :color="poolStatusColor(record.pool_status)">
              {{ poolStatusMap[record.pool_status] ?? record.pool_status }}
            </a-tag>
          </template>
          <!-- actions -->
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button v-permission="'dms:order-pool:view'" type="link" size="small" @click="handleViewEntries(record)">
                查看竞标
              </a-button>
              <a-button
                v-if="record.pool_status === 0 || record.pool_status === 1"
                v-permission="'dms:order-pool:assign'"
                type="link"
                size="small"
                @click="handleForceAssign(record)"
                danger
              >
                强制分配
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { request } from '@/utils/request'
import { useWmsTable } from '@/composables/useWmsTable'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import { SyncOutlined } from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS订单大厅]', err) }

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleString('zh-CN')
  } catch { return dateStr }
}

const locale = { emptyText: '暂无订单数据' }

const router = useRouter()

const poolStatusMap: Record<number, string> = {
  0: '待抢单',
  1: '竞价中',
  2: '已接单',
  3: '已过期',
  4: '已下架'
}

const poolStatusColor = (status: number): string => {
  const colors: Record<number, string> = {
    0: 'orange',
    1: 'red',
    2: 'green',
    3: 'default',
    4: 'default'
  }
  return colors[status] || 'default'
}

const searchFields = [
  {
    key: 'pool_status',
    label: '池状态',
    type: 'select',
    options: Object.entries(poolStatusMap).map(([value, label]) => ({ value: Number(value), label }))
  },
  {
    key: 'bid_enabled',
    label: '竞价启用',
    type: 'select',
    options: [
      { value: true, label: '是' },
      { value: false, label: '否' }
    ]
  }
] as any


const columns = [
  { title: '任务ID', dataIndex: 'task_id', key: 'task_id', width: 180 },
  { title: '配送费', dataIndex: 'delivery_fee', key: 'delivery_fee', width: 100 },
  { title: '竞价启用', dataIndex: 'bid_enabled', key: 'bid_enabled', width: 100 },
  { title: '当前竞价', dataIndex: 'bid_current_price', key: 'bid_current_price', width: 100 },
  { title: '竞价人数', dataIndex: 'bid_count', key: 'bid_count', width: 100 },
  { title: '池状态', dataIndex: 'pool_status', key: 'pool_status', width: 100 },
  { title: '发布时间', dataIndex: 'published_time', key: 'published_time', width: 170 },
  { title: '操作', key: 'actions', width: 200, fixed: 'right' }
] as any

const wms = useWmsTable({
  fetchFn: (params: any) => request.get('/dms/order-pool/page', { params }),
  refreshInterval: 30,
  shortcuts: { f5: 'refresh' },
})
const { tableData, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown, refresh } = wms

const handleSearch = (formData: any) => {
  Object.assign(searchParams, formData)
  wms.handleSearch()
}

const handleReset = () => {
  Object.keys(searchParams).forEach(k => { searchParams[k] = undefined })
  wms.handleReset()
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}

const handleViewEntries = (record: any) => {
  router.push({
    path: '/dms/order-pool/bid-detail',
    query: { poolId: record.id, taskId: record.task_id }
  })
}

const handleForceAssign = async (record: any) => {
  try {
    await request.post(`/dms/order-pool/${record.id}/force-assign`)
    message.success('强制分配成功')
    refresh()
  } catch (err: any) {
    message.error(err?.message || '强制分配失败')
  }
}

// ── 防抖、自动刷新、快捷键 ──────────────────────
// useWmsTable 已内置防抖、自动刷新和快捷键功能
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
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
