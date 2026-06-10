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
            size="small"
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
            size="small"
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

    <VxeTableList
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :show-toolbar="false"
      :selectable="false"
      :show-add="false"
      :show-search="false"
      :show-export="false"
      :show-batch-delete="false"
      @cell-dblclick="handleView"
    >
      <template #empty>
        <div class="table-empty">
          <template v-if="hasError">
            <WarningOutlined class="table-empty-icon" style="color: #faad14" />
            <p class="table-empty-text">加载失败</p>
            <a-button type="primary" size="small" @click="loadMockData" class="table-empty-action">
              <ReloadOutlined /> 重试
            </a-button>
          </template>
          <template v-else>
            <InboxOutlined class="table-empty-icon" />
            <p class="table-empty-text">暂无数据</p>
          </template>
        </div>
      </template>
      <template #typeCell="{ record }">
        <a-tag :color="record.type === 'in' ? 'green' : 'red'">
          {{ record.type === 'in' ? '收入' : '支出' }}
        </a-tag>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="getStatusColor(record.status)">
          {{ getStatusText(record.status) }}
        </a-tag>
      </template>
      <template #amountCell="{ record }">
        <span>¥{{ record.amount?.toFixed(2) }}</span>
      </template>
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { message } from 'ant-design-vue'
import { WarningOutlined, ReloadOutlined, InboxOutlined } from '@ant-design/icons-vue'

interface BankRecord {
  id: number
  date: string
  type: string
  amount: number
  description: string
  status: number
}

const loading = ref(false)
const hasError = ref(false)
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
  total: 0
})

const columns = [
  { field: 'date', title: '日期', width: 120 },
  { field: 'type', title: '类型', width: 80, slotName: 'typeCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'description', title: '说明' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' }
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

const handleView = (record: BankRecord) => {
  message.info(`查看记录: ${record.id}`)
}

const handleAutoReconcile = () => {
  message.success('自动对账完成')
}

loading.value = true
hasError.value = false
function loadMockData() {
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
}
loadMockData()

function handleParentCreate() { handleAdd() }
function handleAdd() {
  message.info('创建功能由父组件触发')
}

onMounted(() => {
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', loadMockData)
})

onUnmounted(() => {
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', loadMockData)
})

defineExpose({})
</script>

<style scoped>
.bank-reconciliation {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.bank-reconciliation > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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

</style>
