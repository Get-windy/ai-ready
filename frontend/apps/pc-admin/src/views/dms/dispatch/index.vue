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

      <div v-if="selectedRowKeys.length > 0" class="batch-bar no-print">
        <span>已选择 {{ selectedRowKeys.length }} 项</span>
        <a-button size="small" @click="handleBatchCancel">批量取消</a-button>
        <a-button size="small" @click="selectedRowKeys = []">取消选择</a-button>
      </div>
      <SkeletonTable v-if="loading && tableData.length === 0" :columns="columns.length" :rows="8" />
      <a-table
        v-else
        :data-source="tableData"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        :rowSelection="rowSelection"
        row-key="id"
        @change="handleTableChange"
      >
        <template #emptyText>
          <a-empty description="暂无调度数据">
            <a-button size="small" @click="wms.fetchData">刷新</a-button>
          </a-empty>
        </template>
        <template #bodyCell="{ column, record }">
          <!-- dispatch_type -->
          <template v-if="column.key === 'dispatch_type'">
            <a-tag :color="dispatchTypeColor(record.dispatch_type)">
              {{ dispatchTypeMap[record.dispatch_type] ?? record.dispatch_type }}
            </a-tag>
          </template>
          <!-- status -->
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">
              {{ statusMap[record.status] ?? record.status }}
            </a-tag>
          </template>
          <!-- actions -->
          <template v-if="column.key === 'actions'">
            <a-space>
              <a-button v-permission="'dms:dispatch:view'" type="link" size="small" @click="handleView(record)">查看</a-button>
              <a-button
                v-if="record.status === 0"
                v-permission="'dms:dispatch:assign'"
                type="link"
                size="small"
                @click="handleAutoAssign(record)"
              >
                自动分配
              </a-button>
              <a-button
                v-if="record.status === 0"
                v-permission="'dms:dispatch:assign'"
                type="link"
                size="small"
                @click="openManualAssign(record)"
              >
                手动分配
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <!-- Manual Assign Dialog -->
      <a-modal
        v-model:open="assignDialogVisible"
        title="手动分配骑手"
        @ok="confirmManualAssign"
        :confirm-loading="assignLoading"
      >
        <a-form layout="vertical">
          <a-form-item label="任务编号">
            <a-input :value="currentTask?.task_no" disabled />
          </a-form-item>
          <a-form-item label="选择骑手" required>
            <a-select
              v-model:value="selectedRiderId"
              placeholder="请选择骑手"
              style="width: 100%"
              :loading="riderLoading"
            >
              <a-select-option
                v-for="rider in riderList"
                :key="rider.id"
                :value="rider.id"
              >
                {{ rider.name }} ({{ rider.phone }})
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { request } from '@/utils/request'
import { useWmsTable } from '@/composables/useWmsTable'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import { SyncOutlined } from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS调度]', err) }

// 行选择
const selectedRowKeys = ref<(string | number)[]>([])
const rowSelection = {
  selectedRowKeys,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys },
} as any
function handleBatchCancel() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要取消的任务')
    return
  }
  // 实际应调用批量取消 API，此处展示选中逻辑
  message.info(`已选中 ${selectedRowKeys.value.length} 个任务`)
}

const statusMap: Record<number, string> = {
  0: '待分配',
  1: '已分配',
  2: '已接单',
  3: '取货中',
  4: '配送中',
  5: '已签收',
  6: '已完成',
  7: '已取消',
  8: '异常'
}

const dispatchTypeMap: Record<number, string> = {
  1: '自动',
  2: '手动',
  3: '抢单',
  4: '竞价'
}

const statusColor = (status: number): string => {
  const colors: Record<number, string> = {
    0: 'orange',
    1: 'blue',
    2: 'cyan',
    3: 'geekblue',
    4: 'purple',
    5: 'green',
    6: 'green',
    7: 'default',
    8: 'red'
  }
  return colors[status] || 'default'
}

const dispatchTypeColor = (type: number): string => {
  const colors: Record<number, string> = {
    1: 'green',
    2: 'blue',
    3: 'orange',
    4: 'red'
  }
  return colors[type] || 'default'
}

const searchFields = [
  { key: 'task_no', label: '任务编号', type: 'input' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: Object.entries(statusMap).map(([value, label]) => ({ value: Number(value), label }))
  }
] as any


const columns = [
  { title: '任务编号', dataIndex: 'task_no', key: 'task_no', width: 180 },
  { title: '客户名称', dataIndex: 'customer_name', key: 'customer_name', width: 150 },
  { title: '配送费', dataIndex: 'delivery_fee', key: 'delivery_fee', width: 100 },
  { title: '分配方式', dataIndex: 'dispatch_type', key: 'dispatch_type', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'actions', width: 220, fixed: 'right' }
] as any

const wms = useWmsTable({
  fetchFn: (params: any) => request.get('/api/dms/dispatch/page', { params }),
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

const handleView = (record: any) => {
  // navigate to detail or open drawer
  message.info(`查看任务: ${record.task_no}`)
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}

const handleAutoAssign = async (record: any) => {
  try {
    await request.post(`/api/dms/dispatch/${record.id}/auto-assign`)
    message.success('自动分配成功')
    refresh()
  } catch (err: any) {
    message.error(err?.message || '自动分配失败')
  }
}

const assignDialogVisible = ref(false)
const assignLoading = ref(false)
const riderLoading = ref(false)
const currentTask = ref<any>(null)
const selectedRiderId = ref<number | undefined>(undefined)
const riderList = ref<any[]>([])

const openManualAssign = async (record: any) => {
  currentTask.value = record
  selectedRiderId.value = undefined
  assignDialogVisible.value = true
  riderLoading.value = true
  try {
    const res = await request.get('/api/dms/rider/list')
    riderList.value = res?.data ?? res ?? []
  } catch {
    riderList.value = []
    message.error('获取骑手列表失败')
  } finally {
    riderLoading.value = false
  }
}

const confirmManualAssign = async () => {
  if (!selectedRiderId.value) {
    message.warning('请选择骑手')
    return
  }
  assignLoading.value = true
  try {
    await request.post(`/api/dms/dispatch/${currentTask.value.id}/assign`, {
      riderId: selectedRiderId.value
    })
    message.success('分配成功')
    assignDialogVisible.value = false
    refresh()
  } catch (err: any) {
    message.error(err?.message || '分配失败')
  } finally {
    assignLoading.value = false
  }
}

// ── 防抖、自动刷新、快捷键 ──────────────────────
// useWmsTable 已内置防抖、自动刷新和快捷键功能
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.batch-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; margin-bottom: 8px; font-size: 13px; }
.batch-bar span { flex: 1; color: #1890ff; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }
@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
