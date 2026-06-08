<template>
  <div class="finance-receivable-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
          <div class="stat-card-label">应收总额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-written-off">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.writtenOffAmount) }}</div>
          <div class="stat-card-label">已核销</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-balance">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.balanceAmount) }}</div>
          <div class="stat-card-label">未核销</div>
        </div>
        <ExclamationCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-overdue">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(overdueAmount) }}</div>
          <div class="stat-card-label">逾期金额</div>
        </div>
        <WarningOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">应收笔数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <!-- 列表标签 -->
      <a-tab-pane key="list" tab="应收列表">
        <VxeTableList
          ref="tableRef"
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          :row-key="'id'"
          :filter-fields="filterFields"
          :show-search="false"
          :show-export="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :selectable="true"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
          @selection-change="handleSelectionChange"
        >
          <template #toolbar-actions>
            <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
              更新 {{ dayjs(lastUpdated).format('HH:mm') }}
            </span>
          </template>

          <template #empty>
            <div class="table-empty">
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">
                没有符合条件的应收记录，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无应收账款数据
              </p>
            </div>
          </template>

          <template #action="{ record }">
            <a-space :size="0" class="action-cell-inner">
              <a-tooltip :title="record.status === 2 || record.status === 3 ? '' : '核销'">
                <a-button type="link" size="small" :disabled="record.status === 2 || record.status === 3" @click="handleWriteOff(record)">
                  <template #icon><CheckCircleOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><EllipsisOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                    <a-menu-item key="badDebt" :disabled="record.status === 2 || record.status === 3" danger>
                      <DeleteOutlined /> 坏账标记
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </VxeTableList>
      </a-tab-pane>

      <!-- 账龄分析标签 -->
      <a-tab-pane key="aging" tab="账龄分析">
        <div ref="agingChartRef" style="height: 400px"></div>
      </a-tab-pane>
    </a-tabs>

    <!-- 核销弹窗 -->
    <a-modal
      v-model:open="writeOffVisible"
      title="应收账款核销"
      :confirm-loading="writeOffLoading"
      @ok="handleWriteOffConfirm"
      @cancel="handleWriteOffCancel"
    >
      <a-descriptions v-if="writeOffTarget" :column="1" bordered size="small">
        <a-descriptions-item label="客户">{{ writeOffTarget.customerName }}</a-descriptions-item>
        <a-descriptions-item label="来源单号">{{ writeOffTarget.sourceNo }}</a-descriptions-item>
        <a-descriptions-item label="应收总额">
          {{ formatAmount(writeOffTarget.amount) }}
        </a-descriptions-item>
        <a-descriptions-item label="已核销金额">
          {{ formatAmount(writeOffTarget.writtenOff) }}
        </a-descriptions-item>
        <a-descriptions-item label="剩余金额">
          {{ formatAmount(writeOffTarget.balance) }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item label="核销金额" required>
          <a-input-number
            v-model:value="writeOffAmount"
            :min="0.01"
            :max="writeOffTarget?.balance || 0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入核销金额"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, DeleteOutlined, EllipsisOutlined, InboxOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { receivableApi } from '@/api/finance'

const tableRef = ref()
const activeTab = ref('list')
const loading = ref(false)
const tableData = ref<any[]>([])

const searchForm = reactive({
  customerName: '',
  status: undefined as number | undefined
})

const filterFields = [
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未核销', value: 0 },
    { label: '部分核销', value: 1 },
    { label: '已核销', value: 2 },
    { label: '坏账', value: 3 }
  ]}
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const stats = reactive({
  totalAmount: 0,
  writtenOffAmount: 0,
  balanceAmount: 0
})

const overdueAmount = computed(() => {
  return tableData.value
    .filter(r => isOverdue(r.dueDate) && r.balance > 0)
    .reduce((sum, item) => sum + item.balance, 0)
})

const statusColorMap: Record<number, string> = {
  0: 'error',
  1: 'warning',
  2: 'success',
  3: 'default'
}

const statusLabelMap: Record<number, string> = {
  0: '未核销',
  1: '部分核销',
  2: '已核销',
  3: '坏账'
}

const columns = computed(() => [
  { title: '来源单号', field: 'sourceNo', width: 150 },
  { title: '客户', field: 'customerName', width: 150 },
  { title: '总额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已核销', field: 'writtenOff', width: 120, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '余额', field: 'balance', width: 120, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '到期日', field: 'dueDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }) => statusLabelMap[cellValue] || '未知' },
  { title: '操作', type: 'action', width: 160, fixed: 'right' }
])

// 核销弹窗
const writeOffVisible = ref(false)
const writeOffLoading = ref(false)
const writeOffTarget = ref<any>(null)
const writeOffAmount = ref(0)

// 账龄图表
const agingChartRef = ref<HTMLDivElement>()
let agingChart: echarts.ECharts | null = null

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.customerName) params.customerName = searchForm.customerName
    if (searchForm.status !== undefined) params.status = searchForm.status

    const res = await receivableApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
      // 更新统计
      stats.totalAmount = tableData.value.reduce((sum, item) => sum + item.amount, 0)
      stats.writtenOffAmount = tableData.value.reduce((sum, item) => sum + item.writtenOff, 0)
      stats.balanceAmount = tableData.value.reduce((sum, item) => sum + item.balance, 0)
    }
  } catch {
    message.error('获取应收账款数据失败')
    // Mock data
    tableData.value = mockData()
    pagination.total = mockData().length
    stats.totalAmount = tableData.value.reduce((sum, item) => sum + item.amount, 0)
    stats.writtenOffAmount = tableData.value.reduce((sum, item) => sum + item.writtenOff, 0)
    stats.balanceAmount = tableData.value.reduce((sum, item) => sum + item.balance, 0)
  } finally {
    loading.value = false
  }
}

const fetchAgingData = async () => {
  try {
    const res = await receivableApi.getAging()
    if (res.data) {
      initAgingChart(res.data)
    }
  } catch {
    // Silently handle
  }
}

const initAgingChart = (data: any) => {
  if (!agingChartRef.value) return
  agingChart?.dispose()
  agingChart = echarts.init(agingChartRef.value)

  const buckets = data.buckets || data || []
  const categories = buckets.map((b: any) => b.label || b.name || '')
  const values = buckets.map((b: any) => b.amount || b.value || 0)

  agingChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}<br/>金额：¥${Number(p.value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: categories, axisLabel: { rotate: 15 } },
    yAxis: { type: 'value', name: '金额 (元)' },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#1890ff' },
          { offset: 1, color: '#69c0ff' }
        ])
      },
      barMaxWidth: 60
    }]
  })
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.customerName = ''
  searchForm.status = undefined
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.customerName = filters.customerName || ''
  searchForm.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: any[], ids: any[]) => {
  // 可以在这里处理选中行的逻辑，例如批量操作
  console.log('Selected rows:', rows.length)
}

const handleWriteOff = (record: any) => {
  writeOffTarget.value = record
  writeOffAmount.value = record.balance
  writeOffVisible.value = true
}

const handleWriteOffConfirm = async () => {
  if (!writeOffAmount.value || writeOffAmount.value <= 0) {
    message.warning('请输入有效的核销金额')
    return
  }
  if (writeOffAmount.value > (writeOffTarget.value?.balance || 0)) {
    message.warning('核销金额不能大于剩余金额')
    return
  }
  writeOffLoading.value = true
  try {
    await receivableApi.writeOff(writeOffTarget.value.id, writeOffAmount.value)
    message.success('核销成功')
    writeOffVisible.value = false
    fetchData()
  } catch {
    message.error('核销失败')
  } finally {
    writeOffLoading.value = false
  }
}

const handleWriteOffCancel = () => {
  writeOffVisible.value = false
}

const handleMarkBadDebt = (record: any) => {
  Modal.confirm({
    title: '确认标记坏账',
    content: `确定将 "${record.customerName}" 的应收款标记为坏账吗？`,
    async onOk() {
      try {
        await receivableApi.markBadDebt(record.id)
        message.success('已标记为坏账')
        fetchData()
      } catch {
        message.error('操作失败')
      }
    }
  })
}

const isOverdue = (dueDate: string) => {
  return dueDate && new Date(dueDate) < new Date()
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'badDebt': handleMarkBadDebt(record); break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

const mockData = () => [
  { id: 1, sourceNo: 'AR2024010001', customerName: '北京客户A', amount: 85000, writtenOff: 35000, balance: 50000, dueDate: '2024-02-15', status: 1 },
  { id: 2, sourceNo: 'AR2024010002', customerName: '上海客户B', amount: 42000, writtenOff: 0, balance: 42000, dueDate: '2024-02-18', status: 0 },
  { id: 3, sourceNo: 'AR2024010003', customerName: '广州客户C', amount: 28000, writtenOff: 28000, balance: 0, dueDate: '2024-02-01', status: 2 },
  { id: 4, sourceNo: 'AR2024010004', customerName: '深圳客户D', amount: 65000, writtenOff: 30000, balance: 35000, dueDate: '2024-01-25', status: 1 },
  { id: 5, sourceNo: 'AR2024010005', customerName: '杭州客户E', amount: 18000, writtenOff: 18000, balance: 0, dueDate: '2024-02-01', status: 2 },
  { id: 6, sourceNo: 'AR2024010006', customerName: '成都客户F', amount: 52000, writtenOff: 0, balance: 52000, dueDate: '2024-01-20', status: 0 }
]

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

// Watch for tab change to init chart
watch(activeTab, (val) => {
  if (val === 'aging') {
    nextTick(() => {
      fetchAgingData()
    })
  }
})
</script>

<style scoped>
.finance-receivable-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-written-off { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-balance { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-overdue { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

/* 空状态 */
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

.empty-placeholder {
  color: transparent;
}

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.amount-cell.success {
  color: #52c41a;
}

.amount-cell.warning {
  color: #faad14;
}

.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}

.action-more-btn {
  padding: 0 4px;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
  }
}
</style>
