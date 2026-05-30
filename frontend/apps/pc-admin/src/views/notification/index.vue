<template>
  <div class="notification-center">
    <!-- 顶部操作栏 -->
    <a-card class="toolbar-card" :bordered="false">
      <div class="toolbar-content">
        <div class="toolbar-left">
          <a-space>
            <a-select
              v-model:value="filterType"
              placeholder="通知类型"
              allow-clear
              style="width: 140px"
              @change="handleFilterChange"
            >
              <a-select-option :value="1">系统通知</a-select-option>
              <a-select-option :value="2">业务通知</a-select-option>
              <a-select-option :value="3">审批通知</a-select-option>
            </a-select>
            <a-select
              v-model:value="filterReadStatus"
              placeholder="阅读状态"
              allow-clear
              style="width: 120px"
              @change="handleFilterChange"
            >
              <a-select-option :value="0">未读</a-select-option>
              <a-select-option :value="1">已读</a-select-option>
            </a-select>
          </a-space>
        </div>
        <div class="toolbar-right">
          <a-space>
            <a-badge :count="unreadCount" :overflow-count="99">
              <BellOutlined :style="{ fontSize: '20px', cursor: 'pointer' }" @click="fetchUnreadCount" />
            </a-badge>
            <a-button type="link" @click="handleMarkAllRead">
              <template #icon><CheckCircleOutlined /></template>
              全部已读
            </a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <!-- 通知列表 -->
    <a-card class="table-card" :bordered="false">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
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

          <template v-else-if="column.key === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeName(record.type) }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'readStatus'">
            <a-badge
              :status="record.readStatus === 0 ? 'processing' : 'default'"
              :text="record.readStatus === 0 ? '未读' : '已读'"
            />
          </template>

          <template v-else-if="column.key === 'summary'">
            <span class="summary-text">{{ record.summary || record.content?.substring(0, 80) }}</span>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="record.readStatus === 0"
                type="link"
                size="small"
                @click="handleMarkRead(record)"
              >
                标记已读
              </a-button>
              <a-popconfirm
                title="确定要删除该通知吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  BellOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  NotificationOutlined,
  AuditOutlined
} from '@ant-design/icons-vue'
import { notificationApi, type NotificationInfo } from '@/api/notification'

// 筛选条件
const filterType = ref<number | undefined>(undefined)
const filterReadStatus = ref<number | undefined>(undefined)

// 表格数据
const tableData = ref<NotificationInfo[]>([])
const loading = ref(false)

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列
const columns: TableProps['columns'] = [
  { title: '标题', key: 'title', width: 300 },
  { title: '类型', key: 'type', width: 100 },
  { title: '内容摘要', key: 'summary', width: 260, ellipsis: true },
  { title: '发送时间', dataIndex: 'sendTime', width: 160 },
  { title: '状态', key: 'readStatus', width: 90 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
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
    if (filterType.value !== undefined) params.type = filterType.value
    if (filterReadStatus.value !== undefined) params.readStatus = filterReadStatus.value

    const res = await notificationApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
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
const handleFilterChange = () => {
  pagination.current = 1
  fetchData()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
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
  try {
    await notificationApi.delete(record.id)
    message.success('删除成功')
    fetchData()
    fetchUnreadCount()
  } catch {
    message.error('删除失败')
  }
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

onMounted(() => {
  fetchData()
  fetchUnreadCount()
})
</script>

<style scoped>
.notification-center {
  padding: 0;
}

.toolbar-card {
  margin-bottom: 16px;
}

.toolbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

.table-card :deep(.ant-card-head) {
  border-bottom: none;
  padding-bottom: 0;
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

@media (max-width: 768px) {
  .toolbar-content {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
