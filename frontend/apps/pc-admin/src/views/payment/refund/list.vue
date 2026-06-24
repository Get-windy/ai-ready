<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">退款管理</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">
              <span style="color: #f5222d; font-weight: bold">¥{{ record.amount }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="REFUND_STATUS_MAP[record.status]?.color">{{ REFUND_STATUS_MAP[record.status]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" v-if="record.status === 0" @click="handleApprove(record, true)">批准</a-button>
                <a-button type="link" size="small" danger v-if="record.status === 0" @click="handleApprove(record, false)">拒绝</a-button>
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
import { refundApi, REFUND_STATUS_MAP, type RefundRequest } from '@/api/payment'

const loading = ref(false)
const tableData = ref<RefundRequest[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '支付请求ID', dataIndex: 'paymentId', key: 'paymentId', width: 100 },
  { title: '退款金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '退款原因', dataIndex: 'reason', key: 'reason', width: 200 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '渠道退款号', dataIndex: 'channelRefundNo', key: 'channelRefundNo', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await refundApi.pageRequest({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function handleApprove(record: RefundRequest, approved: boolean) {
  Modal.confirm({
    title: approved ? '确认批准' : '确认拒绝',
    content: approved ? '确定批准该退款请求吗？' : '确定拒绝该退款请求吗？',
    okType: approved ? undefined : 'danger',
    onOk: async () => {
      await refundApi.approve(record.id, approved)
      message.success(approved ? '已批准' : '已拒绝')
      loadData()
    }
  })
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