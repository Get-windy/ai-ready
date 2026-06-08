<template>
  <div class="difference-handling">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff">
        <div class="summary-card-title">差异总数</div>
        <div class="summary-card-value">{{ summaryData.totalCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14">
        <div class="summary-card-title">待处理差异</div>
        <div class="summary-card-value">{{ summaryData.pendingCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a">
        <div class="summary-card-title">已处理差异</div>
        <div class="summary-card-value">{{ summaryData.processedCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1">
        <div class="summary-card-title">差异总金额</div>
        <div class="summary-card-value">¥{{ summaryData.totalAmount.toFixed(2) }}</div>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :bordered="true"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">
          <span v-if="record.type">¥{{ record.amount?.toFixed(2) }}</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag v-if="record.type" :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space v-if="record.type">
            <a-button
              type="link"
              size="small"
              @click="handleAdjust(record)"
            >
              调整
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleIgnore(record)"
            >
              忽略
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'

const MIN_TABLE_ROWS = 20

interface DifferenceRecord {
  id: number
  type: string
  description: string
  amount: number
  status: number
}

const loading = ref(false)
const dataSource = ref<DifferenceRecord[]>([])

const summaryData = reactive({
  totalCount: 18,
  pendingCount: 5,
  processedCount: 13,
  totalAmount: 12500.00
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true
})

const columns = [
  { title: '差异类型', dataIndex: 'type', key: 'type' },
  { title: '描述', dataIndex: 'description', key: 'description' },
  { title: '金额', key: 'amount' },
  { title: '状态', key: 'status' },
  { title: '操作', key: 'action', width: 150 }
]

// 表格空行填充
const tableData = computed(() => {
  const data = [...dataSource.value]
  while (data.length < MIN_TABLE_ROWS) {
    data.push({
      id: -(data.length + 1),
      type: '',
      description: '',
      amount: 0,
      status: 0
    } as DifferenceRecord)
  }
  return data
})

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'warning',
    1: 'processing',
    2: 'success'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待处理',
    1: '处理中',
    2: '已处理'
  }
  return texts[status] || '未知'
}

const handleAdjust = (record: DifferenceRecord) => {
  message.info(`调整差异: ${record.description}`)
}

const handleIgnore = (record: DifferenceRecord) => {
  message.success(`已忽略差异: ${record.description}`)
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      type: '金额不一致',
      description: '系统与银行流水金额不符',
      amount: 100,
      status: 0
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.difference-handling {
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

/* 网格边框样式 */
:deep(.ant-table-thead > tr > th) {
  border: 2px solid #f0f0f0;
  background: #fafafa;
}

:deep(.ant-table-tbody > tr > td) {
  border: 1px solid #f0f0f0;
}
</style>