<template>
  <div class="collection-reminder-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.pendingCount }}</div>
          <div class="stat-card-label">待提醒</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-reminded">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.remindedCount }}</div>
          <div class="stat-card-label">已提醒</div>
        </div>
        <BellOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-collected">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.collectedCount }}</div>
          <div class="stat-card-label">已收款</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.totalOverdueAmount) }}</div>
          <div class="stat-card-label">逾期总额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <a-form
        layout="inline"
        :model="queryParams"
      >
        <a-form-item label="客户名称">
          <a-input
            v-model:value="queryParams.customerName"
            placeholder="请输入客户名称"
            allow-clear
            size="small"
          />
        </a-form-item>
        <a-form-item label="提醒状态">
          <a-select
            v-model:value="queryParams.status"
            placeholder="请选择"
            allow-clear
            size="small"
            style="width: 120px"
          >
            <a-select-option :value="0">
              待提醒
            </a-select-option>
            <a-select-option :value="1">
              已提醒
            </a-select-option>
            <a-select-option :value="2">
              已收款
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-button @click="handleReset">
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 操作按钮 -->
    <div class="action-area">
      <a-space>
        <a-button @click="handleBatchRemind">
          <template #icon>
            <BellOutlined />
          </template>
          批量提醒
        </a-button>
        <a-button @click="handleExport">
          <template #icon>
            <ExportOutlined />
          </template>
          导出
        </a-button>
      </a-space>
    </div>

    <!-- 数据表格 -->
    <div class="table-area">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :selectable="true"
        :show-toolbar="false"
        :show-search="false"
        :show-add="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
        @selection-change="handleSelectionChange"
        @cell-dblclick="handleView"
      >
        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else>
              <p class="table-empty-text">暂无数据</p>
            </template>
          </div>
        </template>
        <template #amountCell="{ record }">
          <span class="amount-cell">¥{{ record.amount?.toFixed(2) }}</span>
        </template>
        <template #overdueDaysCell="{ record }">
          <a-tag color="red">
            {{ record.overdueDays }}天
          </a-tag>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusText(record.status) }}
          </a-tag>
        </template>
        <template #nextReminderDateCell="{ record }">
          <span :class="{ 'urgent': isUrgent(record.nextReminderDate) }">
            {{ record.nextReminderDate }}
          </span>
        </template>
        <template #action="{ record }">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="handleRemind(record)"
            >
              发送提醒
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleEdit(record)"
            >
              设置
            </a-button>
          </a-space>
        </template>
      </VxeTableList>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { WarningOutlined, ReloadOutlined, BellOutlined, ExportOutlined, ClockCircleOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

interface CollectionReminder {
  id: number
  customerName: string
  orderNo: string
  amount: number
  overdueDays: number
  status: number
  lastReminderDate: string
  nextReminderDate: string
  reminderCount: number
}

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<CollectionReminder[]>([])
const selectedRowKeys = ref<number[]>([])
const tableRef = ref()

const queryParams = reactive({
  customerName: '',
  status: undefined as number | undefined
})

const stats = reactive({
  pendingCount: 8,
  remindedCount: 15,
  collectedCount: 32,
  totalOverdueAmount: 256000
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})



const vxeColumns = computed(() => [
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'orderNo', title: '订单号', width: 150 },
  { field: 'amount', title: '应收金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'overdueDays', title: '逾期天数', width: 100, slotName: 'overdueDaysCell' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'lastReminderDate', title: '上次提醒', width: 120 },
  { field: 'nextReminderDate', title: '下次提醒', width: 120, slotName: 'nextReminderDateCell' },
  { field: 'reminderCount', title: '提醒次数', width: 100 },
  { field: 'action', title: '操作', width: 200, fixed: 'right', type: 'action' },
])

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'orange',
    1: 'blue',
    2: 'green'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待提醒',
    1: '已提醒',
    2: '已收款'
  }
  return texts[status] || '未知'
}

const isUrgent = (date: string) => {
  const reminderDate = new Date(date)
  const today = new Date()
  const diffDays = Math.ceil((reminderDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  return diffDays <= 2
}

function handleParentCreate() {
  handleAdd()
}

function isInput(target: Element | null): boolean {
  if (!target) return false
  const tag = target.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || target.isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleAdd() {
  // 催收页面无需直接新增
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleView = (record: CollectionReminder) => {
  message.info(`查看详情: ${record.customerName}`)
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.customerName = ''
  queryParams.status = undefined
  handleSearch()
}

const handleBatchRemind = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要提醒的客户')
    return
  }
  message.success(`已发送提醒给 ${selectedRowKeys.value.length} 个客户`)
}

const handleRemind = (record: CollectionReminder) => {
  message.success(`已发送提醒给 ${record.customerName}`)
}

const handleEdit = (record: CollectionReminder) => {
  message.info(`打开提醒设置: ${record.customerName}`)
}

const handleExport = () => {
  message.info('导出催收列表')
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleSelectionChange = (rows: any[], ids: any[]) => {
  selectedRowKeys.value = ids
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/finance/collection-reminder/page', {
      params: {
        ...queryParams,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.data?.records) {
      tableData.value = res.data.records
      pagination.total = res.data.total || 0
    } else {
      tableData.value = []
      pagination.total = 0
    }
    hasError.value = false
  } catch (error) {
    message.error('获取数据失败')
    hasError.value = true
  } finally {
    loading.value = false
  }
}

fetchData()

defineExpose({ handleQuery: fetchData })

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
})
</script>

<style scoped>
.collection-reminder-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

.collection-reminder-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

.collection-reminder-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-reminded { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-collected { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-amount { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 24px;
  color: rgba(0, 0, 0, 0.15);
}

.search-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.action-area {
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
}

.table-area {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  flex: 1;
  overflow: hidden;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.urgent {
  color: #ff4d4f;
  font-weight: bold;
}





/* 响应式 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.table-empty-action {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
}
</style>
