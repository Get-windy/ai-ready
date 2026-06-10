<template>
  <PageContainer full-height>
    <template #header>
      <div class="notification-page-header">
        <div class="notification-page-header-left">
          <a-breadcrumb class="notification-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>通知中心</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="notification-page-header-title">通知中心</h2>
        </div>
        <div class="notification-page-header-right">
          <span v-if="lastUpdated" class="update-time">更新于 {{ dayjs(lastUpdated).format('HH:mm:ss') }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', handleRefresh)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="notification-center">
      <!-- 统计卡片骨架 -->
      <template v-if="loading && tableData.length === 0">
        <div class="stat-cards" style="margin-bottom: 16px;">
          <a-card v-for="i in 4" :key="i" :bordered="false" class="stat-skeleton">
            <a-skeleton active :paragraph="{ rows: 1 }" :title="{ width: '60%' }" />
          </a-card>
        </div>
      </template>
      <div v-else class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">通知总数</div>
          </div>
          <BellOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-unread">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ unreadCount }}</div>
            <div class="stat-card-label">未读通知</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-read">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ readCount }}</div>
            <div class="stat-card-label">已读通知</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-system">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ systemCount }}</div>
            <div class="stat-card-label">系统通知</div>
          </div>
          <InfoCircleOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :filter-fields="filterFields"
        :selectable="true"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <a-badge :count="unreadCount" :overflow-count="99">
            <BellOutlined :style="{ fontSize: '20px', cursor: 'pointer' }" @click="fetchUnreadCount" />
          </a-badge>
          <a-button type="link" @click="debounceClick('markAllRead', handleMarkAllRead)">
            <template #icon><CheckCircleOutlined /></template>
            全部已读
          </a-button>
        </template>

        <template #empty>
          <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配通知">
            <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
            <a-button @click="handleResetFilters">清除筛选</a-button>
          </a-empty>
          <a-empty v-else description="暂无通知消息">
            <template #image><BellOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          </a-empty>
        </template>

        <template #title="{ record }">
          <a-space align="start">
            <a-badge :dot="record.readStatus === 0" :offset="[-2, 2]">
              <component :is="getTypeIcon(record.type)" :style="{ fontSize: '16px' }" />
            </a-badge>
            <div>
              <a
                :style="{ fontWeight: record.readStatus === 0 ? 'bold' : 'normal' }"
                @click="handleDetail(record)"
                class="notification-title"
              >
                {{ record.title }}
              </a>
            </div>
          </a-space>
        </template>

        <template #type="{ record }">
          <a-tag :color="getTypeColor(record.type)">
            {{ getTypeName(record.type) }}
          </a-tag>
        </template>

        <template #readStatus="{ record }">
          <a-badge
            :status="record.readStatus === 0 ? 'processing' : 'default'"
            :text="record.readStatus === 0 ? '未读' : '已读'"
          />
        </template>

        <template #summary="{ record }">
          <span class="summary-text">{{ record.summary || record.content?.substring(0, 80) }}</span>
        </template>

        <template #action="{ record }">
          <a-space :size="0" class="action-cell-inner">
            <a-tooltip v-if="record.readStatus === 0" title="标记已读">
              <a-button type="link" size="small" @click="handleMarkRead(record)">
                <template #icon><CheckOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item v-if="record.readStatus === 0" key="mark_read">
                    <CheckOutlined /> 标记已读
                  </a-menu-item>
                  <a-menu-divider v-if="record.readStatus === 0" />
                  <a-menu-item key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>
    </div>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="currentNotification?.title"
      :footer="null"
      width="600px"
    >
      <template v-if="currentNotification">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="类型">
            <a-tag :color="getTypeColor(currentNotification.type)">
              {{ getTypeName(currentNotification.type) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发送时间">{{ currentNotification.sendTime }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge
              :status="currentNotification.readStatus === 0 ? 'processing' : 'default'"
              :text="currentNotification.readStatus === 0 ? '未读' : '已读'"
            />
          </a-descriptions-item>
        </a-descriptions>
        <a-divider />
        <div class="notification-content">
          {{ currentNotification.content || currentNotification.summary }}
        </div>
      </template>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { PageContainer } from '@/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { message, Modal } from 'ant-design-vue'
import {
  BellOutlined,
  CheckCircleOutlined,
  CheckOutlined,
  DeleteOutlined,
  EllipsisOutlined,
  SearchOutlined,
  InfoCircleOutlined,
  NotificationOutlined,
  AuditOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import { notificationApi, type NotificationInfo } from '@/api/notification'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 表格数据 ────────────────────────────────────────────
const tableData = ref<NotificationInfo[]>([])
const loading = ref(false)
const refreshLoading = ref(false)
const tableRef = ref()

// ── 自动刷新 ────────────────────────────────────────────
const autoRefreshCountdown = ref(0)
const lastUpdated = ref('')

// ── 统计数据 ────────────────────────────────────────────
const readCount = computed(() => tableData.value.filter(r => r.readStatus === 1).length)
const systemCount = computed(() => tableData.value.filter(r => r.type === 1).length)

const searchFilters = reactive<Record<string, any>>({})

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 表格列
const vxeColumns = computed(() => [
  { title: '标题', field: 'title', width: 300, slotName: 'title' },
  { title: '类型', field: 'type', width: 100, slotName: 'type' },
  { title: '内容摘要', field: 'summary', width: 260, slotName: 'summary' },
  { title: '发送时间', field: 'sendTime', width: 160 },
  { title: '状态', field: 'readStatus', width: 90, slotName: 'readStatus' },
  { title: '操作', field: 'action', width: 140, fixed: 'right', type: 'action' }
])

// 选择变化处理
const selectedRowKeys = ref<(string | number)[]>([])
const handleSelectionChange = (rows: any[], ids: (string | number)[]) => {
  selectedRowKeys.value = ids
}

const filterFields = [
  { key: 'type', label: '通知类型', type: 'select' as const, options: [
    { label: '系统通知', value: 1 },
    { label: '业务通知', value: 2 },
    { label: '审批通知', value: 3 },
  ]},
  { key: 'readStatus', label: '阅读状态', type: 'select' as const, options: [
    { label: '未读', value: 0 },
    { label: '已读', value: 1 },
  ]},
]

// 未读数量
const unreadCount = ref(0)

// 详情弹窗
const detailVisible = ref(false)
const currentNotification = ref<NotificationInfo | null>(null)

// ── 数据加载 ────────────────────────────────────────────
const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchFilters.type !== undefined) params.type = searchFilters.type
    if (searchFilters.readStatus !== undefined) params.readStatus = searchFilters.readStatus

    const res = await notificationApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
    lastUpdated.value = new Date().toISOString()
  } catch (err) {
    console.warn('[通知] 加载通知失败', err)
    message.error('加载通知失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await notificationApi.getUnreadCount()
    if (res.data) {
      unreadCount.value = res.data.total
    }
  } catch (err) {
    console.warn('[通知] 获取未读数量失败', err)
  }
}

function handleRefresh() {
  refreshLoading.value = true
  fetchData()
  fetchUnreadCount()
}

// 筛选
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

// 标记已读
const handleMarkRead = async (record: NotificationInfo) => {
  try {
    await notificationApi.markAsRead(record.id)
    record.readStatus = 1
    message.success('已标记为已读')
    fetchUnreadCount()
  } catch (err) {
    console.warn('[通知] 标记已读失败', err)
    message.error('操作失败')
  }
}

// 全部已读
const handleMarkAllRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    message.success('已全部标记为已读')
    fetchData()
    fetchUnreadCount()
  } catch (err) {
    console.warn('[通知] 全部已读失败', err)
    message.error('操作失败')
  }
}

// 删除
const handleDelete = async (record: NotificationInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该通知吗？删除后数据不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await notificationApi.delete(record.id)
        message.success('删除成功')
        fetchData()
        fetchUnreadCount()
      } catch (err) {
        console.warn('[通知] 删除通知失败', err)
        message.error('删除失败')
      }
    }
  })
}

// 详情
const handleDetail = (record: NotificationInfo) => {
  currentNotification.value = record
  detailVisible.value = true
  if (record.readStatus === 0) {
    handleMarkRead(record)
  }
}

// 辅助函数
const getTypeColor = (type: number) => {
  const colors: Record<number, string> = { 1: '#1890ff', 2: '#52c41a', 3: '#faad14' }
  return colors[type] || 'default'
}

const getTypeName = (type: number) => {
  const names: Record<number, string> = { 1: '系统通知', 2: '业务通知', 3: '审批通知' }
  return names[type] || '未知'
}

const getTypeIcon = (type: number) => {
  const icons: Record<number, any> = {
    1: InfoCircleOutlined,
    2: NotificationOutlined,
    3: AuditOutlined
  }
  return icons[type] || InfoCircleOutlined
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: NotificationInfo) {
  switch (key) {
    case 'mark_read': handleMarkRead(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', handleRefresh); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault() }
}

// ── 自动刷新 ────────────────────────────────────────────
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleParentCreate() { handleAdd() }

onMounted(() => {
  fetchData()
  fetchUnreadCount()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    fetchUnreadCount()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('notification:create', handleParentCreate)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('notification:create', handleParentCreate)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.notification-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.notification-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.notification-breadcrumb {
  font-size: 13px;
}
.notification-breadcrumb :deep(li) {
  font-size: 13px;
}
.notification-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.notification-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.notification-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片骨架 */
.stat-skeleton {
  flex: 1;
  border-radius: 8px;
}
.stat-skeleton :deep(.ant-card-body) {
  padding: 12px 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-unread { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-read { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-system { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.notification-title {
  cursor: pointer;
  color: rgba(0, 0, 0, 0.85);
  text-decoration: none;
}

.notification-title:hover {
  color: #1890ff;
}

.summary-text {
  color: #999;
  font-size: 13px;
}

.notification-content {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.action-cell-inner { flex-wrap: nowrap; }

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
