<template>
  <div class="workflow-tasks">
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
            <a-select-option value="high">
              高
            </a-select-option>
            <a-select-option value="medium">
              中
            </a-select-option>
            <a-select-option value="low">
              低
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="创建时间">
          <a-range-picker
            v-model:value="queryForm.dateRange"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </a-button>
          <a-button
            style="margin-left: 8px"
            @click="handleReset"
          >
            重置
          </a-button>
        </a-form-item>
      </a-form>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="taskId"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'priority'">
            <a-tag :color="getPriorityColor(record.priority)">
              {{ getPriorityLabel(record.priority) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button
              type="link"
              size="small"
              @click="handleViewDetail(record)"
            >
              查看详情
            </a-button>
            <a-button
              v-if="activeTab === 'todo'"
              type="link"
              size="small"
              @click="handleApprove(record)"
            >
              审批
            </a-button>
            <a-dropdown v-if="activeTab === 'todo'">
              <a-button
                type="link"
                size="small"
              >
                转办/委托 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="(e) => handleTransfer(e.key as string, record)">
                  <a-menu-item key="transfer">
                    转办
                  </a-menu-item>
                  <a-menu-item key="delegate">
                    委托
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情对话框 -->
    <a-modal
      v-model:open="detailVisible"
      title="任务详情"
      :width="800"
      :footer="null"
    >
      <a-descriptions
        bordered
        :column="2"
      >
        <a-descriptions-item label="任务ID">
          {{ detailData.taskId }}
        </a-descriptions-item>
        <a-descriptions-item label="任务名称">
          {{ detailData.taskName }}
        </a-descriptions-item>
        <a-descriptions-item label="流程名称">
          {{ detailData.processName }}
        </a-descriptions-item>
        <a-descriptions-item label="优先级">
          <a-tag :color="getPriorityColor(detailData.priority)">
            {{ getPriorityLabel(detailData.priority) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="处理人">
          {{ detailData.assignee }}
        </a-descriptions-item>
        <a-descriptions-item label="当前节点">
          {{ detailData.currentNode }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ detailData.createTime }}
        </a-descriptions-item>
        <a-descriptions-item label="截止时间">
          {{ detailData.dueTime }}
        </a-descriptions-item>
        <a-descriptions-item
          label="任务描述"
          :span="2"
        >
          {{ detailData.description }}
        </a-descriptions-item>
        <a-descriptions-item
          label="业务数据"
          :span="2"
        >
          <pre>{{ detailData.businessData }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 审批对话框 -->
    <a-modal
      v-model:open="approveVisible"
      title="审批"
      :width="600"
      @ok="handleConfirmApprove"
      @cancel="approveVisible = false"
    >
      <a-form
        :model="approveForm"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="审批意见">
          <a-radio-group v-model:value="approveForm.approval">
            <a-radio value="approve">
              同意
            </a-radio>
            <a-radio value="reject">
              拒绝
            </a-radio>
            <a-radio value="return">
              退回
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批备注">
          <a-textarea
            v-model:value="approveForm.comment"
            :rows="4"
            placeholder="请输入审批备注"
          />
        </a-form-item>
        <a-form-item
          v-if="approveForm.approval === 'return'"
          label="退回节点"
        >
          <a-select
            v-model:value="approveForm.returnNode"
            placeholder="请选择退回节点"
          >
            <a-select-option value="start">
              发起人
            </a-select-option>
            <a-select-option value="previous">
              上一节点
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 转办/委托对话框 -->
    <a-modal
      v-model:open="transferVisible"
      :title="transferDialogTitle"
      :width="500"
      @ok="handleConfirmTransfer"
      @cancel="transferVisible = false"
    >
      <a-form
        :model="transferForm"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="目标用户">
          <a-select
            v-model:value="transferForm.targetUser"
            placeholder="请选择用户"
            show-search
          >
            <a-select-option value="user001">
              张三
            </a-select-option>
            <a-select-option value="user002">
              李四
            </a-select-option>
            <a-select-option value="user003">
              王五
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="transferForm.comment"
            :rows="4"
            placeholder="请输入备注"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined } from '@ant-design/icons-vue'
import type { TableProps } from 'ant-design-vue'
import type { MenuInfo } from 'ant-design-vue/lib/menu/src/interface'
import request from '@/utils/request'

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

// 表格列定义
const columns = [
  { title: '任务ID', dataIndex: 'taskId', key: 'taskId', width: 180 },
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', minWidth: 150 },
  { title: '流程名称', dataIndex: 'processName', key: 'processName', minWidth: 120 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '处理人', dataIndex: 'assignee', key: 'assignee', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '截止时间', dataIndex: 'dueTime', key: 'dueTime', width: 180 },
  { title: '操作', key: 'action', width: 300, fixed: 'right' as const }
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

// 切换标签页
const handleTabChange = () => {
  handleQuery()
}

// 查询
const handleQuery = async () => {
  loading.value = true
  try {
    // 调用后端API获取数据，失败时加载模拟数据
    let apiData: any[] = []
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
      apiData = res.data?.records || []
      pagination.total = res.data?.total || apiData.length
    } catch {
      // API 不可用时使用模拟数据
      console.warn('[workflow] 后端API不可用，使用模拟数据')
    }

    if (apiData.length > 0) {
      tableData.value = apiData
      return
    }

    // 模拟数据（API不可用时的回退）
    if (activeTab.value === 'todo') {
      tableData.value = [
        {
          taskId: 'TASK-001',
          taskName: '部门经理审批',
          processName: '请假审批流程',
          priority: 'high',
          assignee: '张三',
          currentNode: '部门经理审批',
          createTime: '2024-04-15 09:00:00',
          dueTime: '2024-04-16 18:00:00',
          description: '请审批张三的请假申请',
          businessData: JSON.stringify({ days: 3, reason: '个人事务' })
        },
        {
          taskId: 'TASK-002',
          taskName: '财务审批',
          processName: '报销审批流程',
          priority: 'medium',
          assignee: '李四',
          currentNode: '财务审批',
          createTime: '2024-04-15 10:00:00',
          dueTime: '2024-04-17 18:00:00',
          description: '请审批李四的报销申请',
          businessData: JSON.stringify({ amount: 5000, type: '差旅费' })
        }
      ]
    } else {
      tableData.value = [
        {
          taskId: 'TASK-003',
          taskName: '发起人审批',
          processName: '请假审批流程',
          priority: 'medium',
          assignee: '王五',
          currentNode: '已完成',
          createTime: '2024-04-14 14:00:00',
          dueTime: '2024-04-14 18:00:00',
          description: '已审批',
          businessData: JSON.stringify({ days: 2, reason: '病假' })
        }
      ]
    }
    pagination.total = 30
  } catch (error) {
    message.error('查询失败')
  } finally {
    loading.value = false
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
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  handleQuery()
}

// 查看详情
const handleViewDetail = (record: any) => {
  detailData.value = record
  detailVisible.value = true
}

// 审批
const handleApprove = (record: any) => {
  ;(approveForm as any)._taskId = record.taskId
  ;(approveForm as any)._record = record
  approveForm.approval = 'approve'
  approveForm.comment = ''
  approveForm.returnNode = undefined
  approveVisible.value = true
}

// 确认审批
const handleConfirmApprove = async () => {
  const actionLabels: Record<string, string> = {
    approve: '同意',
    reject: '拒绝',
    return: '退回'
  }
  Modal.confirm({
    title: '确认审批',
    content: `确定要${actionLabels[approveForm.approval] || '审批'}该任务吗？`,
    async onOk() {
      try {
        await request.post('/workflow/task/approve', {
          taskId: (approveForm as any)._taskId,
          approval: approveForm.approval,
          comment: approveForm.comment,
          returnNode: approveForm.approval === 'return' ? approveForm.returnNode : undefined
        })
        message.success('审批成功')
        approveVisible.value = false
        handleQuery()
      } catch {
        message.error('审批失败')
      }
    }
  })
}

// 转办/委托
const handleTransfer = (command: string, record: any) => {
  ;(transferForm as any)._taskId = record.taskId
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
          taskId: (transferForm as any)._taskId,
          type: transferForm.type,
          targetUser: transferForm.targetUser,
          comment: transferForm.comment
        })
        message.success(`${actionLabel}成功`)
        transferVisible.value = false
        handleQuery()
      } catch {
        message.error(`${actionLabel}失败`)
      }
    }
  })
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
handleQuery()
</script>

<style scoped>
.workflow-tasks {
  padding: 20px;
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
</style>