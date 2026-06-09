<template>
  <div class="notification-center">
    <!-- 统计卡片 -->
    <div class="stat-cards">
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
        <a-button type="link" @click="handleMarkAllRead">
          <template #icon><CheckCircleOutlined /></template>
          全部已读
        </a-button>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
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
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import { notificationApi, type NotificationInfo } from '@/api/notification'

// 表格数据
const tableData = ref<NotificationInfo[]>([])
const loading = ref(false)
const tableRef = ref()

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
const lastUpdated = ref('')

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

// 数据加载
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
      lastUpdated.value = new Date().toISOString()
    }
  } catch (error) {
    message.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await notificationApi.getUnreadCount()
    if (res.data) {
      unreadCount.value = res.data.total
    }
  } catch {
    // 忽略加载失败
  }
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
  } catch {
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
  } catch {
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
      } catch {
        message.error('删除失败')
      }
    }
  })
}

// 详情
const handleDetail = (record: NotificationInfo) => {
  currentNotification.value = record
  detailVisible.value = true
  // 如果是未读，自动标记已读
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
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault() }
}

onMounted(() => {
  fetchData()
  fetchUnreadCount()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.notification-center {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
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
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-cell-inner { flex-wrap: nowrap; }





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
