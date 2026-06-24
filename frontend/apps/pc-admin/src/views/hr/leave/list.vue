<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">请假管理</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'leaveType'">
              <span>{{ LEAVE_TYPE_MAP[record.leaveType] || record.leaveType }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="LEAVE_STATUS_MAP[record.status]?.color">{{ LEAVE_STATUS_MAP[record.status]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" v-if="record.status === 0" @click="handleApprove(record)">批准</a-button>
                <a-button type="link" size="small" danger v-if="record.status === 0" @click="handleReject(record)">拒绝</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { hrLeaveRequestApi, type HrLeaveRequest, LEAVE_TYPE_MAP, LEAVE_STATUS_MAP } from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrLeaveRequest[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 120 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 120 },
  { title: '请假天数', dataIndex: 'days', key: 'days', width: 80 },
  { title: '请假原因', dataIndex: 'reason', key: 'reason', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 120 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrLeaveRequestApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function handleApprove(record: HrLeaveRequest) {
  Modal.confirm({ title: '确认批准', content: '确定批准该请假申请吗？', onOk: async () => {
    await hrLeaveRequestApi.approve(record.id)
    message.success('批准成功')
    loadData()
  }})
}

async function handleReject(record: HrLeaveRequest) {
  Modal.confirm({ title: '确认拒绝', content: '确定拒绝该请假申请吗？', okType: 'danger', onOk: async () => {
    await hrLeaveRequestApi.reject(record.id)
    message.success('已拒绝')
    loadData()
  }})
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