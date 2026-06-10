<template>
  <PageContainer full-height>
    <template #header>
      <div class="payable-page-header">
        <div class="payable-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>应付账款</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="payable-page-header-title">应付账款</h2>
        </div>
        <div class="payable-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="finance-payable-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">应付总额</div>
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
            <div class="stat-card-label">应付笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-tabs v-model:activeKey="activeTab">
        <!-- 列表标签 -->
        <a-tab-pane key="list" tab="应付列表">
          <VxeTableList
            ref="tableRef"
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            :row-key="'id'"
            :filter-fields="filterFields"
            :show-search="false"
            export-permission="finance:payable:list"
            :show-add="false"
            :show-edit="false"
            :show-delete="false"
            :selectable="true"
            @refresh="fetchData"
            @cell-dblclick="handleView"
            @page-change="handlePageChange"
            @filter-change="handleFilterChange"
            @selection-change="handleSelectionChange"
          >
            <template #toolbar-actions>
              <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
                更新 {{ dayjs(lastUpdated).format('HH:mm') }}
              </span>
            </template>
            <template #batch-actions>
              <!-- 预留批量操作 -->
            </template>

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
                  <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
                  <InboxOutlined v-else class="table-empty-icon" />
                  <p v-if="hasActiveFilters" class="table-empty-text">
                    没有符合条件的应付记录，<a @click="handleResetFilters">清除筛选</a>
                  </p>
                  <p v-else class="table-empty-text">
                    暂无应付账款数据
                  </p>
                </template>
              </div>
            </template>

            <template #statusCell="{ record }">
              <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusLabelMap[record.status] || '未知' }}</a-tag>
            </template>
            <template #action="{ record }">
              <a-space :size="0" class="action-cell-inner">
                <PrintButton
                  template-type="payable"
                  :business-id="record.id"
                  business-type="payable"
                  button-text=""
                  button-size="small"
                  button-type="link"
                  tooltip="打印"
                />
                <a-tooltip :title="record.status === 'written_off' ? '' : '核销'">
                  <a-button type="link" size="small" :disabled="record.status === 'written_off'" @click="handleWriteOff(record)">
                    <template #icon><CheckCircleOutlined /></template>
                  </a-button>
                </a-tooltip>
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
      <FullScreenDetail
        :visible="writeOffVisible"
        title="应付账款核销"
        :save-loading="writeOffLoading"
        @save="handleWriteOffConfirm"
        @close="handleWriteOffCancel"
      >
        <a-descriptions v-if="writeOffTarget" :column="1" bordered size="small">
          <a-descriptions-item label="供应商">{{ writeOffTarget.supplierName }}</a-descriptions-item>
          <a-descriptions-item label="来源单号">{{ writeOffTarget.sourceNo }}</a-descriptions-item>
          <a-descriptions-item label="应付总额">
            {{ formatAmount(writeOffTarget.totalAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已核销金额">
            {{ formatAmount(writeOffTarget.paidAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="剩余金额">
            {{ formatAmount(writeOffTarget.remainingAmount) }}
          </a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="核销金额" required>
            <a-input-number
              v-model:value="writeOffAmount"
              :min="0.01"
              :max="writeOffTarget?.remainingAmount || 0"
              :precision="2"
              size="small"
              style="width: 100%"
              placeholder="请输入核销金额"
            />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, InboxOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined, FileTextOutlined,
  SyncOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer, FullScreenDetail } from '@/components'
import { payableApi } from '@/api/finance'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const activeTab = ref('list')
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchForm = reactive({
  supplierName: '',
  status: undefined as string | undefined
})

const filterFields = [
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '供应商名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未核销', value: 'normal' },
    { label: '部分核销', value: 'partial' },
    { label: '逾期', value: 'overdue' },
    { label: '已核销', value: 'written_off' }
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
    .filter(r => isOverdue(r.dueDate) && (r.remainingAmount || 0) > 0)
    .reduce((sum, item) => sum + (item.remainingAmount || 0), 0)
})

const statusColorMap: Record<string, string> = {
  normal: 'warning',
  partial: 'processing',
  overdue: 'error',
  written_off: 'success'
}

const statusLabelMap: Record<string, string> = {
  normal: '未核销',
  partial: '部分核销',
  overdue: '逾期',
  written_off: '已核销'
}

const columns = computed(() => [
  { title: '来源单号', field: 'sourceNo', width: 150 },
  { title: '供应商', field: 'supplierName', width: 150 },
  { title: '总额', field: 'totalAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已核销', field: 'paidAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '余额', field: 'remainingAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '到期日', field: 'dueDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', slotName: 'statusCell' },
  { title: '操作', type: 'action', width: 120, fixed: 'right' }
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
  refreshLoading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.supplierName) params.supplierName = searchForm.supplierName
    if (searchForm.status !== undefined) params.status = searchForm.status

    const res = await payableApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
      // 更新统计
      stats.totalAmount = tableData.value.reduce((sum, item) => sum + (item.totalAmount || 0), 0)
      stats.writtenOffAmount = tableData.value.reduce((sum, item) => sum + (item.paidAmount || 0), 0)
      stats.balanceAmount = tableData.value.reduce((sum, item) => sum + (item.remainingAmount || 0), 0)
    }
    hasError.value = false
  } catch (err) {
    console.warn('[应付账款] 获取数据失败', err)
    message.error('获取应付账款数据失败')
    tableData.value = []
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const fetchAgingData = async () => {
  try {
    const res = await payableApi.getAging()
    if (res.data) {
      initAgingChart(res.data)
    }
  } catch (err) {
    console.warn('[应付账款] 获取账龄分析失败', err)
    message.warning('获取账龄分析数据失败')
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
          { offset: 0, color: '#fa8c16' },
          { offset: 1, color: '#ffc069' }
        ])
      },
      barMaxWidth: 60
    }]
  })
}

const handleView = (record: any) => {
  message.info(`查看详情: ${record.supplierName}`)
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.supplierName = ''
  searchForm.status = undefined
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.supplierName = filters.supplierName || ''
  searchForm.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: any[], ids: any[]) => {
  // 可以在这里处理选中行的逻辑，例如批量操作
}

const handleWriteOff = (record: any) => {
  writeOffTarget.value = record
  writeOffAmount.value = record.remainingAmount
  writeOffVisible.value = true
}

const handleWriteOffConfirm = async () => {
  if (!writeOffAmount.value || writeOffAmount.value <= 0) {
    message.warning('请输入有效的核销金额')
    return
  }
  if (writeOffAmount.value > (writeOffTarget.value?.remainingAmount || 0)) {
    message.warning('核销金额不能大于剩余金额')
    return
  }
  writeOffLoading.value = true
  try {
    await payableApi.writeOff(writeOffTarget.value.id, writeOffAmount.value)
    message.success('核销成功')
    writeOffVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[应付账款] 核销失败', err)
    message.error('核销失败')
  } finally {
    writeOffLoading.value = false
  }
}

const handleWriteOffCancel = () => {
  writeOffVisible.value = false
}

const isOverdue = (dueDate: string) => {
  return dueDate && new Date(dueDate) < new Date()
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function isInput(el: Element | null): boolean {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (el as HTMLElement)?.isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleAdd() {
  // 应付页面无需直接新增
}

function handleParentCreate() {
  handleAdd()
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// Watch for tab change to init chart
watch(activeTab, (val) => {
  if (val === 'aging') {
    nextTick(() => {
      fetchAgingData()
    })
  }
})

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.payable-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.payable-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.payable-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.payable-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.finance-payable-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  gap: 16px;
}

.finance-payable-page > :deep(.vxe-table-list-container) {
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

/* 响应式 */

.table-empty-action {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
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
