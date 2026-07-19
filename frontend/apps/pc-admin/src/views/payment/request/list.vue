<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">
        支付请求
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
              <a-tag :color="PAYMENT_STATUS_MAP[record.status]?.color">
                {{ PAYMENT_STATUS_MAP[record.status]?.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button
                  v-if="record.status === 1 && (record.channel === 'CASH' || record.channel === 'BANK')"
                  type="link"
                  size="small"
                  @click="showConfirmModal(record)"
                >
                  确认收款
                </a-button>
                <a-button
                  v-if="record.status === 0 || record.status === 1"
                  type="link"
                  size="small"
                  danger
                  @click="handleCancel(record)"
                >
                  取消
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal
      v-model:open="confirmModalVisible"
      title="确认线下收款"
      @ok="handleConfirm"
    >
      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="收款单号">
          <a-input
            v-model:value="confirmForm.channelOrderNo"
            placeholder="请输入收款单号/流水号"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { paymentApi, PAYMENT_CHANNEL_MAP, PAYMENT_STATUS_MAP, type PaymentRequest } from '@/api/payment'

const loading = ref(false)
const tableData = ref<PaymentRequest[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const confirmModalVisible = ref(false)
const confirmForm = reactive({ id: 0, channelOrderNo: '' })

const columns: any[] = [
  { title: '业务类型', dataIndex: 'bizType', key: 'bizType', width: 100 },
  { title: '业务单号', dataIndex: 'bizNo', key: 'bizNo', width: 120 },
  { title: '支付金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '支付渠道', dataIndex: 'channel', key: 'channel', width: 100 },
  { title: '渠道订单号', dataIndex: 'channelOrderNo', key: 'channelOrderNo', width: 150 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await paymentApi.pageRequest({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showConfirmModal(record: any) {
  confirmForm.id = record.id
  confirmForm.channelOrderNo = ''
  confirmModalVisible.value = true
}

async function handleConfirm() {
  if (!confirmForm.channelOrderNo) { message.warning('请输入收款单号'); return }
  await paymentApi.confirmOffline(confirmForm.id, confirmForm.channelOrderNo)
  message.success('确认成功')
  confirmModalVisible.value = false
  loadData()
}

async function handleCancel(record: any) {
  Modal.confirm({
    title: '确认取消',
    content: '确定取消该支付请求吗？',
    okType: 'danger',
    onOk: async () => {
      await paymentApi.cancel(record.id)
      message.success('已取消')
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