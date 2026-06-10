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
      <template #amountCell="{ record }">
        <span>¥{{ record.amount?.toFixed(2) }}</span>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="getStatusColor(record.status)">
          {{ getStatusText(record.status) }}
        </a-tag>
      </template>
      <template #actionCell="{ record }">
        <a-space>
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
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { message } from 'ant-design-vue'
import { WarningOutlined, ReloadOutlined, InboxOutlined } from '@ant-design/icons-vue'

interface DifferenceRecord {
  id: number
  type: string
  description: string
  amount: number
  status: number
}

const loading = ref(false)
const hasError = ref(false)
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
  total: 0
})

const columns = [
  { field: 'type', title: '差异类型' },
  { field: 'description', title: '描述' },
  { field: 'amount', title: '金额', slotName: 'amountCell' },
  { field: 'status', title: '状态', slotName: 'statusCell' },
  { field: 'action', title: '操作', width: 150, slotName: 'actionCell' }
]

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

const handleView = (record: DifferenceRecord) => {
  message.info(`查看差异: ${record.description}`)
}

const handleIgnore = (record: DifferenceRecord) => {
  message.success(`已忽略差异: ${record.description}`)
}

loading.value = true
hasError.value = false
function loadMockData() {
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
.difference-handling {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.difference-handling > :deep(.vxe-table-list-container) {
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

/* 网格边框样式 */

</style>
