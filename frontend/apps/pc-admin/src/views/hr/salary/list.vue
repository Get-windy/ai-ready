<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">薪资发放</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actualAmount'">
              <span style="color: #52c41a; font-weight: bold">¥{{ record.actualAmount }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'warning'">{{ record.status === 1 ? '已发放' : '待发放' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" v-if="record.status === 0" @click="handleConfirm(record)">确认发放</a-button>
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
import { hrSalaryApi, type HrSalaryPayment } from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrSalaryPayment[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '发放月份', dataIndex: 'paymentMonth', key: 'paymentMonth', width: 100 },
  { title: '基本工资', dataIndex: 'baseAmount', key: 'baseAmount', width: 100 },
  { title: '绩效工资', dataIndex: 'performanceAmount', key: 'performanceAmount', width: 100 },
  { title: '扣款合计', dataIndex: 'deductAmount', key: 'deductAmount', width: 100 },
  { title: '实发金额', dataIndex: 'actualAmount', key: 'actualAmount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrSalaryApi.pagePayments({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function handleConfirm(record: HrSalaryPayment) {
  Modal.confirm({
    title: '确认发放',
    content: `确定要发放月份 "${record.paymentMonth}" 的薪资吗？`,
    onOk: async () => {
      await hrSalaryApi.confirmPayment(record.id)
      message.success('发放成功')
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