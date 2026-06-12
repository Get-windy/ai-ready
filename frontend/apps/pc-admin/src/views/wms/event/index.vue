<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>WMS / 事件监控</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>事件监控</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button v-permission="'wms:event:process'" size="small" @click="handleProcessPending">
            <CaretRightOutlined /> 处理待发事件
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
        <div class="stat-cards" style="margin-bottom: 12px;">
          <div class="stat-card" style="border-top:3px solid #1890ff">
            <div class="stat-value" style="color:#1890ff">{{ stats.total }}</div>
            <div class="stat-label">事件总数</div>
          </div>
          <div class="stat-card" style="border-top:3px solid #faad14">
            <div class="stat-value" style="color:#faad14">{{ stats.pending }}</div>
            <div class="stat-label">待处理</div>
          </div>
          <div class="stat-card" style="border-top:3px solid #52c41a">
            <div class="stat-value" style="color:#52c41a">{{ stats.completed }}</div>
            <div class="stat-label">已处理</div>
          </div>
          <div class="stat-card" style="border-top:3px solid #ff4d4f">
            <div class="stat-value" style="color:#ff4d4f">{{ stats.failed }}</div>
            <div class="stat-label">失败</div>
          </div>
        </div>

        <a-skeleton active :loading="loading && dataList.length === 0">
        <a-table
          :dataSource="dataList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          rowKey="id"
          size="small"
          bordered
          @change="handleTableChange"
        >
          <template #emptyText>
            <div class="empty-state-wrapper">
              <template v-if="hasError">
                <WarningOutlined class="empty-state-icon" style="color: #faad14" />
                <p class="empty-state-text">加载失败</p>
                <a-button type="primary" size="small" @click="wms.debounce('refresh', wms.fetchData)" class="empty-state-action">
                  <ReloadOutlined /> 重试
                </a-button>
              </template>
              <template v-else>
                <InboxOutlined class="empty-state-icon" />
                <p class="empty-state-text">暂无事件记录</p>
              </template>
            </div>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="statusMap[record.status]?.color || 'default'">{{ statusMap[record.status]?.text || record.status }}</a-tag>
            </template>
            <template v-if="column.dataIndex === 'payload'">
              <span class="payload-preview">{{ truncate(record.payload, 60) }}</span>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-tooltip title="重试" v-if="record.status === 2 || record.status === 3">
                  <a-button v-permission="'wms:event:retry'" type="link" size="small" @click="handleRetry(record as any)"><ReloadOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="查看详情">
                  <a-button v-permission="'wms:event:view'" type="link" size="small" @click="handleView(record as any)"><EyeOutlined /></a-button>
                </a-tooltip>
              </a-space>
            </template>
          </template>
        </a-table>
        </a-skeleton>
      </div>
    </template>
  </PageContainer></ErrorBoundary>

  <!-- 事件详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="事件详情"
    width="700px"
    :footer="null"
    @cancel="detailVisible = false"
  >
    <a-descriptions v-if="detailData" bordered :column="2" size="small">
      <a-descriptions-item label="事件ID">{{ detailData.id }}</a-descriptions-item>
      <a-descriptions-item label="事件类型">{{ detailData.eventType }}</a-descriptions-item>
      <a-descriptions-item label="事件Key">{{ detailData.eventKey }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="statusMap[detailData.status]?.color || 'default'">{{ statusMap[detailData.status]?.text || detailData.status }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="重试次数">{{ detailData.retryCount }} / {{ detailData.maxRetries }}</a-descriptions-item>
      <a-descriptions-item label="最后错误">{{ detailData.lastError || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ detailData.updateTime }}</a-descriptions-item>
      <a-descriptions-item label="消息体" :span="2">
        <pre class="payload-json">{{ formatPayload(detailData.payload) }}</pre>
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { eventApi, type WmsEventOutbox } from '@/api/wms/event'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  ReloadOutlined, SyncOutlined, EyeOutlined, CaretRightOutlined,
  WarningOutlined, InboxOutlined,
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[WMSEvent]', err) }
const hasError = ref(false)

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '待处理', color: 'orange' },
  1: { text: '处理中', color: 'blue' },
  2: { text: '已完成', color: 'green' },
  3: { text: '失败', color: 'red' },
  4: { text: '已取消', color: 'default' },
}

const columns = [
  { title: '事件ID', dataIndex: 'id', width: 70 },
  { title: '事件类型', dataIndex: 'eventType', width: 150 },
  { title: '事件Key', dataIndex: 'eventKey', width: 180, ellipsis: true },
  { title: '消息体', dataIndex: 'payload', width: 200, ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '重试次数', dataIndex: 'retryCount', width: 80 },
  { title: '最后错误', dataIndex: 'lastError', width: 150, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', width: 100, fixed: 'right' },
] as any

const searchFields: SearchField[] = [
  { name: 'eventType', label: '事件类型', type: 'input', placeholder: '请输入事件类型' },
  { name: 'eventKey', label: '事件Key', type: 'input', placeholder: '请输入事件Key' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '待处理', value: 0 }, { label: '处理中', value: 1 }, { label: '已完成', value: 2 }, { label: '失败', value: 3 }, { label: '已取消', value: 4 },
  ]},
]

const wms = useWmsTable({
  fetchFn: eventApi.page as any,
  defaultPageSize: 20,
  refreshInterval: 15,
  shortcuts: { f5: 'refresh' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

const stats = computed(() => {
  const total = dataList.length
  const pending = dataList.filter(r => r.status === 0).length
  const completed = dataList.filter(r => r.status === 2).length
  const failed = dataList.filter(r => r.status === 3).length
  return { total, pending, completed, failed }
})

function truncate(str: string, len: number): string {
  if (!str) return '-'
  return str.length > len ? str.substring(0, len) + '...' : str
}

function formatPayload(payload: string): string {
  if (!payload) return '-'
  try {
    return JSON.stringify(JSON.parse(payload), null, 2)
  } catch {
    return payload
  }
}

// ── 详情 ──
const detailVisible = ref(false)
const detailData = ref<WmsEventOutbox | null>(null)

function handleView(record: WmsEventOutbox) {
  detailData.value = record
  detailVisible.value = true
}

// ── 重试 ──
async function handleRetry(record: WmsEventOutbox) {
  try {
    await eventApi.retry(record.id)
    message.success('已加入重试队列')
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '重试失败')
  }
}

// ── 处理待发事件 ──
async function handleProcessPending() {
  try {
    await eventApi.processPending()
    message.success('待发事件已开始处理')
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '处理失败')
  }
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

// ── 生命周期（Composable 已处理定时器/快捷键） ──
onUnmounted(() => { /* useWmsTable handles cleanup */ })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.page-body { padding: 0; }
.stat-card { background: #fff; border-radius: 8px; padding: 14px 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.stat-value { font-size: 22px; font-weight: 700; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; line-height: 1.2; }
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }
.stat-cards { display: flex; gap: 12px; }
.stat-card { flex: 1; }
.payload-preview { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 12px; color: #666; }
.payload-json { background: #f5f5f5; border: 1px solid #e8e8e8; border-radius: 4px; padding: 12px; font-size: 12px; max-height: 300px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 空状态 ──────────────────────── */
.empty-state-wrapper { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.empty-state-icon { font-size: 48px; color: #d9d9d9; }
.empty-state-text { color: #999; margin-top: 12px; }
.empty-state-action { margin-top: 12px; }

/* ── vxe-table 表头 2px 边框 ─────── */
:deep(.vxe-table .vxe-header--row) { border-top: 2px solid #e8e8e8; }
</style>
