<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">支付对账</h2>
      </div>
      <div class="page-header__right">
        <a-button type="primary" @click="showExecuteModal">执行对账</a-button>
      </div>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'channel'">
              <a-tag :color="PAYMENT_CHANNEL_MAP[record.channel]?.color">{{ PAYMENT_CHANNEL_MAP[record.channel]?.name }}</a-tag>
            </template>
            <template v-if="column.key === 'totalAmount'">
              <span style="font-weight: bold">¥{{ record.totalAmount }}</span>
            </template>
            <template v-if="column.key === 'successAmount'">
              <span style="color: #52c41a; font-weight: bold">¥{{ record.successAmount }}</span>
            </template>
            <template v-if="column.key === 'diffAmount'">
              <span v-if="record.diffAmount > 0" style="color: #f5222d; font-weight: bold">¥{{ record.diffAmount }}</span>
              <span v-else>¥0</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="RECON_STATUS_MAP[record.status]?.color">{{ RECON_STATUS_MAP[record.status]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" v-if="record.status === 2" @click="showHandleModal(record)">处理差异</a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="executeModalVisible" title="执行日对账" @ok="handleExecute">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="对账日期">
          <a-date-picker v-model:value="executeForm.date" style="width: 100%" />
        </a-form-item>
        <a-form-item label="渠道">
          <a-select v-model:value="executeForm.channel" placeholder="全部渠道" allowClear>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="WECHAT">微信支付</a-select-option>
            <a-select-option value="UNIONPAY">银联支付</a-select-option>
            <a-select-option value="BANK">银行转账</a-select-option>
            <a-select-option value="CASH">现金/线下</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="handleModalVisible" title="处理差异" @ok="handleDifference">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="处理备注">
          <a-textarea v-model:value="handleForm.remark" placeholder="请输入处理备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { reconciliationApi, PAYMENT_CHANNEL_MAP, RECON_STATUS_MAP, type PaymentReconciliation } from '@/api/payment'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<PaymentReconciliation[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const executeModalVisible = ref(false)
const handleModalVisible = ref(false)
const executeForm = reactive({ date: dayjs(), channel: undefined as string | undefined })
const handleForm = reactive({ id: 0, remark: '' })

const columns: any[] = [
  { title: '对账日期', dataIndex: 'reconcileDate', key: 'reconcileDate', width: 100 },
  { title: '渠道', dataIndex: 'channel', key: 'channel', width: 100 },
  { title: '总笔数', dataIndex: 'totalCount', key: 'totalCount', width: 80 },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 100 },
  { title: '成功笔数', dataIndex: 'successCount', key: 'successCount', width: 80 },
  { title: '成功金额', dataIndex: 'successAmount', key: 'successAmount', width: 100 },
  { title: '差异笔数', dataIndex: 'diffCount', key: 'diffCount', width: 80 },
  { title: '差异金额', dataIndex: 'diffAmount', key: 'diffAmount', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await reconciliationApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showExecuteModal() {
  executeForm.date = dayjs()
  executeForm.channel = undefined
  executeModalVisible.value = true
}

async function handleExecute() {
  const dateStr = executeForm.date.format('YYYY-MM-DD')
  await reconciliationApi.execute(dateStr, executeForm.channel)
  message.success('对账执行完成')
  executeModalVisible.value = false
  loadData()
}

function showHandleModal(record: PaymentReconciliation) {
  handleForm.id = record.id
  handleForm.remark = ''
  handleModalVisible.value = true
}

async function handleDifference() {
  if (!handleForm.remark) { message.warning('请输入处理备注'); return }
  await reconciliationApi.handleDifference(handleForm.id, handleForm.remark)
  message.success('处理完成')
  handleModalVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>