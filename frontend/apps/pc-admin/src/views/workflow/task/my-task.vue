<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">我的待办</h2>
      </div>
    </div>

    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="primary" size="small" @click="showApproveModal(record)">审批</a-button>
                <a-button size="small" danger @click="handleReject(record)">驳回</a-button>
                <a-button size="small" @click="showTransferModal(record)">转交</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 审批弹窗 -->
    <a-modal v-model:open="approveVisible" title="审批处理" width="500px" @ok="handleApprove">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="节点名称">
          <span>{{ currentTask?.nodeName || '-' }}</span>
        </a-form-item>
        <a-form-item label="审批意见">
          <a-textarea v-model:value="approveComment" placeholder="请输入审批意见" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 转交弹窗 -->
    <a-modal v-model:open="transferVisible" title="转交任务" width="500px" @ok="handleTransfer">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="转交给" required>
          <a-input v-model:value="transferUserName" placeholder="请输入转交目标用户名" />
        </a-form-item>
        <a-form-item label="转交原因">
          <a-textarea v-model:value="transferComment" placeholder="请输入转交原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { workflowTaskApi, type WorkflowTask } from '@/api/workflow'

const loading = ref(false)
const tableData = ref<WorkflowTask[]>([])
const approveVisible = ref(false)
const transferVisible = ref(false)
const currentTask = ref<WorkflowTask | null>(null)
const approveComment = ref('')
const transferUserName = ref('')
const transferComment = ref('')

const columns: any[] = [
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 150 },
  { title: '审批人', dataIndex: 'assigneeName', key: 'assigneeName', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    tableData.value = await workflowTaskApi.getTodoTasks()
  } catch (e) {
    message.error('查询失败')
  } finally {
    loading.value = false
  }
}

function showApproveModal(task: WorkflowTask) {
  currentTask.value = task
  approveComment.value = ''
  approveVisible.value = true
}

async function handleApprove() {
  if (!currentTask.value) return
  try {
    await workflowTaskApi.approve(currentTask.value.id, approveComment.value)
    message.success('审批通过')
    approveVisible.value = false
    loadData()
  } catch (e) {
    message.error('审批失败')
  }
}

async function handleReject(task: WorkflowTask) {
  Modal.confirm({
    title: '确认驳回',
    content: '确定要驳回该任务吗？',
    okType: 'danger',
    onOk: async () => {
      await workflowTaskApi.reject(task.id)
      message.success('已驳回')
      loadData()
    }
  })
}

function showTransferModal(task: WorkflowTask) {
  currentTask.value = task
  transferUserName.value = ''
  transferComment.value = ''
  transferVisible.value = true
}

async function handleTransfer() {
  if (!currentTask.value || !transferUserName.value) {
    message.warning('请输入转交目标用户名')
    return
  }
  try {
    // 暂时使用用户名，实际应查询用户ID
    await workflowTaskApi.transfer(currentTask.value.id, 1, transferComment.value)
    message.success('转交成功')
    transferVisible.value = false
    loadData()
  } catch (e) {
    message.error('转交失败')
  }
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