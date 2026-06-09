<template>
  <PageContainer full-height>
    <template #header>
      <div class="workflow-page-header">
        <div class="workflow-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>任务管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="workflow-page-header-title">任务管理</h2>
        </div>
        <div class="workflow-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="handleQuery">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="workflow-tasks">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">任务总数</div>
          </div>
          <ScheduleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-todo">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ todoCount }}</div>
            <div class="stat-card-label">待办任务</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-high">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ highPriorityCount }}</div>
            <div class="stat-card-label">高优先级</div>
          </div>
          <FireOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-overdue">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ overdueCount }}</div>
            <div class="stat-card-label">超时任务</div>
          </div>
          <WarningOutlined class="stat-card-icon" />
        </div>
      </div>

    <a-card>
      <template #title>
        <a-tabs
          v-model:active-key="activeTab"
          @change="handleTabChange"
        >
          <a-tab-pane
            key="todo"
            tab="待办任务"
          />
          <a-tab-pane
            key="done"
            tab="已办任务"
          />
        </a-tabs>
      </template>

      <!-- 查询表单 -->
      <a-form
        :model="queryForm"
        layout="inline"
        class="query-form"
      >
        <a-form-item label="任务名称">
          <a-input
            v-model:value="queryForm.taskName"
            placeholder="请输入任务名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="流程名称">
          <a-input
            v-model:value="queryForm.processName"
            placeholder="请输入流程名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="优先级">
          <a-select
            v-model:value="queryForm.priority"
            placeholder="请选择优先级"
            allow-clear
            style="width: 100px"
          >
            <a-select-option value="high">高</a-select-option>
            <a-select-option value="medium">中</a-select-option>
            <a-select-option value="low">低</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="创建时间">
          <a-range-picker
            v-model:value="queryForm.dateRange"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleQuery">查询</a-button>
          <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
        </a-form-item>
      </a-form>

      <!-- 数据表格 -->
      <VxeTableList
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="taskId"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
      >
        <template #priorityCell="{ record }">
          <a-tag :color="getPriorityColor(record.priority)">
            {{ getPriorityLabel(record.priority) }}
          </a-tag>
        </template>
        <template #action="{ record }">
          <a-button type="link" size="small" @click="handleViewDetail(record)">查看详情</a-button>
          <a-button v-if="activeTab === 'todo'" type="link" size="small" @click="handleApprove(record)">审批</a-button>
          <a-dropdown v-if="activeTab === 'todo'">
            <a-button type="link" size="small">
              转办/委托 <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu @click="(e) => handleTransfer(e.key as string, record)">
                <a-menu-item key="transfer">转办</a-menu-item>
                <a-menu-item key="delegate">委托</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 详情对话框 -->
    <a-drawer v-model:open="detailVisible" title="任务详情" placement="right" width="80vw" :footer="null">
      <a-descriptions bordered :column="2">
        <a-descriptions-item label="任务ID">{{ detailData.taskId }}</a-descriptions-item>
        <a-descriptions-item label="任务名称">{{ detailData.taskName }}</a-descriptions-item>
        <a-descriptions-item label="流程名称">{{ detailData.processName }}</a-descriptions-item>
        <a-descriptions-item label="优先级">
          <a-tag :color="getPriorityColor(detailData.priority)">{{ getPriorityLabel(detailData.priority) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="处理人">{{ detailData.assignee }}</a-descriptions-item>
        <a-descriptions-item label="当前节点">{{ detailData.currentNode }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
        <a-descriptions-item label="截止时间">{{ detailData.dueTime }}</a-descriptions-item>
        <a-descriptions-item label="任务描述" :span="2">{{ detailData.description }}</a-descriptions-item>
        <a-descriptions-item label="业务数据" :span="2">
          <pre>{{ detailData.businessData ? JSON.stringify(JSON.parse(detailData.businessData), null, 2) : '无' }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-drawer>

    <!-- 审批对话框 -->
    <a-modal v-model:open="approveVisible" title="审批" :width="600" @ok="handleConfirmApprove" @cancel="handleCancelApprove">
      <a-form :model="approveForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="审批意见">
          <a-radio-group v-model:value="approveForm.approval">
            <a-radio value="approve">同意</a-radio>
            <a-radio value="reject">拒绝</a-radio>
            <a-radio value="return">退回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批备注">
          <a-textarea v-model:value="approveForm.comment" :rows="4" placeholder="请输入审批备注" />
        </a-form-item>
        <a-form-item v-if="approveForm.approval === 'return'" label="退回节点">
          <a-select v-model:value="approveForm.returnNode" placeholder="请选择退回节点">
            <a-select-option value="start">发起人</a-select-option>
            <a-select-option value="previous">上一节点</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 转办/委托对话框 -->
    <a-modal v-model:open="transferVisible" :title="transferDialogTitle" :width="500" @ok="handleConfirmTransfer" @cancel="handleCancelTransfer">
      <a-form :model="transferForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="目标用户">
          <a-select v-model:value="transferForm.targetUser" placeholder="请选择用户" show-search :filter-option="filterUserOption">
            <a-select-option v-for="u in userList" :key="u.id" :value="u.id">
              {{ u.nickname || u.username }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="transferForm.comment" :rows="4" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined, ScheduleOutlined, ClockCircleOutlined, FireOutlined, WarningOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import type { MenuInfo } from 'ant-design-vue/lib/menu/src/interface'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import request from '@/utils/request'
import { userApi } from '@/api/user'

// 自动刷新
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 当前标签页
const activeTab = ref('todo')

// 查询表单
const queryForm = reactive({
  taskName: '',
  processName: '',
  priority: undefined as string | undefined,
  dateRange: [] as any[]
})

// 表格数据
const tableData = ref<any[]>([])

// ── 统计数据 ────────────────────────────────────────────
const todoCount = computed(() => activeTab.value === 'todo' ? tableData.value.length : 0)
const highPriorityCount = computed(() => tableData.value.filter(r => r.priority === 'high').length)
const overdueCount = computed(() => tableData.value.filter(r => r.dueTime && new Date(r.dueTime) < new Date()).length)

// 表格列定义
const vxeColumns = [
  { field: 'taskId', title: '任务ID', width: 180 },
  { field: 'taskName', title: '任务名称', minWidth: 150 },
  { field: 'processName', title: '流程名称', minWidth: 120 },
  { field: 'priority', title: '优先级', width: 80, slotName: 'priorityCell' },
  { field: 'assignee', title: '处理人', width: 100 },
  { field: 'createTime', title: '创建时间', width: 180 },
  { field: 'dueTime', title: '截止时间', width: 180 },
  { field: 'action', title: '操作', width: 300, fixed: 'right', type: 'action' }
]

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

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<any>({})

// 审批对话框
const approveVisible = ref(false)
const approveForm = reactive({
  approval: 'approve',
  comment: '',
  returnNode: undefined as string | undefined
})
const currentApproveTaskId = ref<string | null>(null)

// 转办/委托对话框
const transferVisible = ref(false)
const transferDialogTitle = computed(() => {
  return transferForm.type === 'transfer' ? '转办任务' : '委托任务'
})
const transferForm = reactive({
  type: 'transfer',
  targetUser: undefined as string | undefined,
  comment: ''
})
const currentTransferTaskId = ref<string | null>(null)

// 用户列表（用于转办/委托）
const userList = ref<{ id: string; username: string; nickname: string }[]>([])

const fetchUserList = async () => {
  try {
    const res = await userApi.getList({ pageSize: 1000 })
    const data = (res as any).data
    userList.value = (data?.records || data || []).map((u: any) => ({ id: u.id, username: u.username, nickname: u.nickname }))
  } catch (err) {
    userList.value = []
    console.warn('[工作流] 加载用户列表失败', err)
  }
}

const filterUserOption = (input: string, option: any) => {
  const label = option.children?.toString() || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

// 切换标签页
const handleTabChange = () => {
  pagination.current = 1
  handleQuery()
}

// 查询
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await request.get('/workflow/task/page', {
      params: {
        tab: activeTab.value,
        taskName: queryForm.taskName || undefined,
        processName: queryForm.processName || undefined,
        priority: queryForm.priority,
        startDate: queryForm.dateRange?.[0],
        endDate: queryForm.dateRange?.[1],
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error: any) {
    tableData.value = []
    pagination.total = 0
    console.warn('[工作流] 获取任务列表失败', error)
    message.error(error?.response?.data?.message || '获取工作流数据失败，请稍后重试')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 重置
const handleReset = () => {
  queryForm.taskName = ''
  queryForm.processName = ''
  queryForm.priority = undefined
  queryForm.dateRange = []
  pagination.current = 1
  handleQuery()
}

// 表格变化
const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  handleQuery()
}

// 查看详情
const handleViewDetail = (record: any) => {
  detailData.value = record
  detailVisible.value = true
}

// 审批
const handleApprove = (record: any) => {
  currentApproveTaskId.value = record.taskId
  approveForm.approval = 'approve'
  approveForm.comment = ''
  approveForm.returnNode = undefined
  approveVisible.value = true
}

// 确认审批
const handleConfirmApprove = async () => {
  if (!currentApproveTaskId.value) return
  const actionLabels: Record<string, string> = { approve: '同意', reject: '拒绝', return: '退回' }
  Modal.confirm({
    title: '确认审批',
    content: `确定要${actionLabels[approveForm.approval] || '审批'}该任务吗？`,
    async onOk() {
      try {
        await request.post('/workflow/task/approve', {
          taskId: currentApproveTaskId.value,
          approval: approveForm.approval,
          comment: approveForm.comment,
          returnNode: approveForm.approval === 'return' ? approveForm.returnNode : undefined
        })
        console.warn('[工作流] 操作成功: 审批成功')
        message.success('审批成功')
        approveVisible.value = false
        currentApproveTaskId.value = null
        handleQuery()
      } catch (err) {
        console.warn('[工作流] 审批失败', err)
        message.error('审批失败')
      }
    }
  })
}

// 取消审批
const handleCancelApprove = () => {
  currentApproveTaskId.value = null
}

// 转办/委托
const handleTransfer = (command: string, record: any) => {
  currentTransferTaskId.value = record.taskId
  transferForm.type = command
  transferForm.targetUser = undefined
  transferForm.comment = ''
  transferVisible.value = true
}

// 确认转办/委托
const handleConfirmTransfer = async () => {
  if (!transferForm.targetUser) {
    message.warning('请选择目标用户')
    return
  }

  const actionLabel = transferForm.type === 'transfer' ? '转办' : '委托'
  Modal.confirm({
    title: `确认${actionLabel}`,
    content: `确定要${actionLabel}该任务给用户"${transferForm.targetUser}"吗？`,
    async onOk() {
      try {
        await request.post('/workflow/task/transfer', {
          taskId: currentTransferTaskId.value,
          type: transferForm.type,
          targetUser: transferForm.targetUser,
          comment: transferForm.comment
        })
        console.warn('[工作流] 操作成功: 转办/委托成功')
        message.success(`${actionLabel}成功`)
        transferVisible.value = false
        currentTransferTaskId.value = null
        handleQuery()
      } catch (err) {
        console.warn('[工作流] 转办/委托失败', err)
        message.error(`${actionLabel}失败`)
      }
    }
  })
}

// 取消转办/委托
const handleCancelTransfer = () => {
  currentTransferTaskId.value = null
}

// 获取优先级颜色
const getPriorityColor = (priority: string) => {
  const colors: Record<string, string> = {
    high: 'red',
    medium: 'orange',
    low: 'default'
  }
  return colors[priority] || 'default'
}

// 获取优先级标签
const getPriorityLabel = (priority: string) => {
  const labels: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return labels[priority] || priority
}

// 初始加载
onMounted(() => {
  handleQuery()
  fetchUserList()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    handleQuery()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery })
</script>

<style scoped>
.workflow-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.workflow-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.workflow-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.workflow-page-header-right {
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

.workflow-tasks {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
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
.stat-todo { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-high { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-overdue { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

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

.query-form {
  margin-bottom: 20px;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
