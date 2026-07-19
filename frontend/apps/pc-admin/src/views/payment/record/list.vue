<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">
        支付记录
      </h2>
    </div>
    <div class="page-container__body">
      <a-card
        :bordered="false"
        class="table-card"
      >
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'amount'">
              <span style="color: #52c41a; font-weight: bold">¥{{ record.amount }}</span>
            </template>
            <template v-if="column.key === 'channel'">
              <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color">
                {{ PAYMENT_CHANNEL_MAP[record.channel]?.name }}
              </a-tag>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 2 ? 'success' : 'error'">
                {{ record.status === 2 ? '成功' : '失败' }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { paymentApi, PAYMENT_CHANNEL_MAP, type PaymentRecord } from '@/api/payment'

const loading = ref(false)
const tableData = ref<PaymentRecord[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '渠道订单号', dataIndex: 'channelOrderNo', key: 'channelOrderNo', width: 150 },
  { title: '渠道交易号', dataIndex: 'channelTradeNo', key: 'channelTradeNo', width: 150 },
  { title: '支付渠道', dataIndex: 'channel', key: 'channel', width: 100 },
  { title: '支付金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '回调时间', dataIndex: 'callbackTime', key: 'callbackTime', width: 150 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await paymentApi.pageRecord({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }
onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>