<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">我的已办</h2>
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
              <a-tag :color="TASK_ACTION_MAP[record.action]?.color">
                {{ TASK_ACTION_MAP[record.action]?.text }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { workflowTaskApi, type WorkflowTask, TASK_ACTION_MAP } from '@/api/workflow'

const loading = ref(false)
const tableData = ref<WorkflowTask[]>([])

const columns: any[] = [
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 150 },
  { title: '处理动作', dataIndex: 'action', key: 'action', width: 100 },
  { title: '审批意见', dataIndex: 'comment', key: 'comment', width: 200 },
  { title: '处理时间', dataIndex: 'handleTime', key: 'handleTime', width: 180 }
]

async function loadData() {
  loading.value = true
  try {
    tableData.value = await workflowTaskApi.getDoneTasks()
  } catch (e) {
    message.error('查询失败')
  } finally {
    loading.value = false
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