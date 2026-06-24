<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">流程实例</h2>
      </div>
    </div>

    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="INSTANCE_STATUS_MAP[record.status]?.color">
                {{ INSTANCE_STATUS_MAP[record.status]?.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showDetail(record)">查看详情</a-button>
                <a-button type="link" size="small" v-if="record.status === 0" @click="handleWithdraw(record)">撤回</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="流程实例详情" width="800px" :footer="null">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="流程标题">{{ currentInstance?.title }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ currentInstance?.applicantName }}</a-descriptions-item>
        <a-descriptions-item label="当前节点">{{ currentInstance?.currentNodeName }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="INSTANCE_STATUS_MAP[currentInstance?.status || 0]?.color">
            {{ INSTANCE_STATUS_MAP[currentInstance?.status || 0]?.text }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentInstance?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="完成时间">{{ currentInstance?.finishTime || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>审批历史</a-divider>
      <a-timeline>
        <a-timeline-item v-for="task in historyTasks" :key="task.id" :color="getTimelineColor(task)">
          <div>
            <strong>{{ task.nodeName }}</strong>
            <span style="margin-left: 8px; color: #8c8c8c">{{ task.assigneeName }}</span>
            <a-tag v-if="task.status > 0" :color="TASK_ACTION_MAP[task.action]?.color" style="margin-left: 8px">
              {{ TASK_ACTION_MAP[task.action]?.text }}
            </a-tag>
          </div>
          <div v-if="task.comment" style="color: #595959; margin-top: 4px">{{ task.comment }}</div>
          <div style="color: #8c8c8c; font-size: 12px">{{ task.handleTime || task.createTime }}</div>
        </a-timeline-item>
      </a-timeline>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  workflowInstanceApi,
  workflowTaskApi,
  type WorkflowInstance,
  type WorkflowTask,
  INSTANCE_STATUS_MAP,
  TASK_ACTION_MAP
} from '@/api/workflow'

const loading = ref(false)
const tableData = ref<WorkflowInstance[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })

const detailVisible = ref(false)
const currentInstance = ref<WorkflowInstance | null>(null)
const historyTasks = ref<WorkflowTask[]>([])

const columns: any[] = [
  { title: '流程标题', dataIndex: 'title', key: 'title', width: 200 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 120 },
  { title: '当前节点', dataIndex: 'currentNodeName', key: 'currentNodeName', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

// 模拟数据（实际应调用API）
async function loadData() {
  loading.value = true
  // 暂时使用模拟数据，后续实现实例查询API
  tableData.value = []
  pagination.total = 0
  loading.value = false
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function showDetail(record: WorkflowInstance) {
  currentInstance.value = record
  try {
    historyTasks.value = await workflowInstanceApi.getHistory(record.id)
  } catch (e) {
    historyTasks.value = []
  }
  detailVisible.value = true
}

async function handleWithdraw(record: WorkflowInstance) {
  Modal.confirm({
    title: '确认撤回',
    content: `确定要撤回流程 "${record.title}" 吗？`,
    onOk: async () => {
      await workflowInstanceApi.withdraw(record.id)
      message.success('撤回成功')
      loadData()
    }
  })
}

function getTimelineColor(task: WorkflowTask) {
  if (task.status === 0) return 'blue'
  return task.action === 1 ? 'green' : task.action === 2 ? 'red' : 'orange'
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>