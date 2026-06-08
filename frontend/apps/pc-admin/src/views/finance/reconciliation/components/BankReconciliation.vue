<template>
  <div class="bank-reconciliation">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff">
        <div class="summary-card-title">总交易笔数</div>
        <div class="summary-card-value">{{ summaryData.totalCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14">
        <div class="summary-card-title">总收入金额</div>
        <div class="summary-card-value">¥{{ summaryData.totalIncome.toFixed(2) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a">
        <div class="summary-card-title">总支出金额</div>
        <div class="summary-card-value">¥{{ summaryData.totalExpense.toFixed(2) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1">
        <div class="summary-card-title">对账完成率</div>
        <div class="summary-card-value">{{ summaryData.completionRate }}%</div>
      </div>
    </div>

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
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :bordered="true"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
          <a-tag v-if="record.date" :color="record.type === 'in' ? 'green' : 'red'">
            {{ record.type === 'in' ? '收入' : '支出' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag v-if="record.date" :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'amount'">
          <span v-if="record.date">¥{{ record.amount?.toFixed(2) }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'

const MIN_TABLE_ROWS = 20

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

const summaryData = reactive({
  totalCount: 86,
  totalIncome: 458000.00,
  totalExpense: 236500.00,
  completionRate: 92.5
})

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
  { title: '金额', key: 'amount', width: 120 },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '状态', key: 'status', width: 100 }
]

// 表格空行填充
const tableData = computed(() => {
  const data = [...dataSource.value]
  while (data.length < MIN_TABLE_ROWS) {
    data.push({
      id: -(data.length + 1),
      date: '',
      type: '',
      amount: 0,
      description: '',
      status: 0
    } as BankRecord)
  }
  return data
})

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

/* 统计卡片样式 */
.summary-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.summary-card {
  flex: 1;
  min-width: 180px;
  padding: 16px 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--card-color), color-mix(in srgb, var(--card-color) 70%, white));
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.summary-card-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.summary-card-value {
  font-size: 24px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.filter-area {
  margin-bottom: 16px;
}

/* 网格边框样式 */
:deep(.ant-table-thead > tr > th) {
  border: 2px solid #f0f0f0;
  background: #fafafa;
}

:deep(.ant-table-tbody > tr > td) {
  border: 1px solid #f0f0f0;
}
</style>