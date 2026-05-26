<template>
  <div class="bank-reconciliation">
    <div class="filter-area">
      <a-form
        layout="inline"
        :model="queryParams"
      >
        <a-form-item label="银行账户">
          <a-select
            v-model:value="queryParams.bankAccount"
            placeholder="请选择银行账户"
            allow-clear
            style="width: 200px"
          >
            <a-select-option value="001">
              工商银行-123456
            </a-select-option>
            <a-select-option value="002">
              建设银行-789012
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="对账日期">
          <a-month-picker
            v-model:value="queryParams.month"
            format="YYYY-MM"
            value-format="YYYY-MM"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-button @click="handleAutoReconcile">
              自动对账
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 对账结果 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'in' ? 'green' : 'red'">
            {{ record.type === 'in' ? '收入' : '支出' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface BankRecord {
  id: number
  date: string
  type: string
  amount: number
  description: string
  status: number
}

const loading = ref(false)
const dataSource = ref<BankRecord[]>([])

const queryParams = reactive({
  bankAccount: undefined as string | undefined,
  month: undefined as string | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true
})

const columns = [
  { title: '日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '类型', key: 'type', width: 80 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '状态', key: 'status', width: 100 }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'success',
    1: 'warning',
    2: 'error'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '已对账',
    1: '待对账',
    2: '异常'
  }
  return texts[status] || '未知'
}

const handleSearch = () => {
  message.info('查询银行对账记录')
}

const handleAutoReconcile = () => {
  message.success('自动对账完成')
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      date: '2026-04-13',
      type: 'in',
      amount: 10000,
      description: '销售收款',
      status: 0
    },
    {
      id: 2,
      date: '2026-04-13',
      type: 'out',
      amount: 5000,
      description: '采购付款',
      status: 1
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.bank-reconciliation {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>