<template>
  <div class="depreciation-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalDepreciation) }}</div>
          <div class="stat-card-label">累计折旧总额</div>
        </div>
        <CalculatorOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-period">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(periodDepreciation) }}</div>
          <div class="stat-card-label">本期折旧额</div>
        </div>
        <CalendarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">折旧记录数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-depreciation-list'"
      :filter-fields="filterFields"
      :show-export="true"
      :selectable="true"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
        <a-button type="primary" ghost @click="handleBatchCalculate">
          <template #icon><CalculatorOutlined /></template>
          批量计提折旧
        </a-button>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的折旧记录，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无折旧记录，点击「批量计提折旧」开始计提
          </p>
        </div>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else-if="column.field === 'assetCode'">
          <a @click="handleViewAsset(record)" class="asset-code">{{ record.assetCode }}</a>
        </template>
        <template v-else-if="column.field === 'assetName'">
          <a @click="handleViewAsset(record)" class="asset-name">{{ record.assetName }}</a>
        </template>
        <template v-else-if="column.field === 'periodAmount'">
          <span class="amount-cell depreciation">¥{{ formatAmount(record.periodAmount) }}</span>
        </template>
        <template v-else-if="column.field === 'accumulatedDepreciation'">
          <span class="amount-cell">¥{{ formatAmount(record.accumulatedDepreciation) }}</span>
        </template>
        <template v-else-if="column.field === 'netValue'">
          <span class="amount-cell success">¥{{ formatAmount(record.netValue) }}</span>
        </template>
        <template v-else-if="column.field === 'assetOriginalValue'">
          <span class="amount-cell">¥{{ formatAmount(record.assetOriginalValue) }}</span>
        </template>
        <template v-else-if="column.field === 'status'">
          <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
            {{ record.status === 'completed' ? '已完成' : '待处理' }}
          </a-tag>
        </template>
      </template>
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, CalculatorOutlined, CalendarOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { depreciationApi } from '@/api/fixed-asset'

const emit = defineEmits(['update-count'])

const loading = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const selectedRowKeys = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

// ── 统计数据 ────────────────────────────────────────────
const totalDepreciation = computed(() => {
  return tableData.value.reduce((sum, r) => sum + (r.accumulatedDepreciation || 0), 0)
})

const periodDepreciation = computed(() => {
  return tableData.value.reduce((sum, r) => sum + (r.periodAmount || 0), 0)
})

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const vxeColumns = computed(() => [
  { field: 'assetCode', title: '资产编码', width: 140 },
  { field: 'assetName', title: '资产名称', width: 180 },
  { field: 'period', title: '期间', width: 100 },
  { field: 'depreciationDate', title: '折旧日期', width: 120 },
  { field: 'periodAmount', title: '本期折旧', width: 130, align: 'right' },
  { field: 'accumulatedDepreciation', title: '累计折旧', width: 130, align: 'right' },
  { field: 'netValue', title: '净值', width: 130, align: 'right' },
  { field: 'assetOriginalValue', title: '资产原值', width: 130, align: 'right' },
  { field: 'status', title: '状态', width: 80, align: 'center' },
])

const filterFields = [
  { key: 'assetId', label: '资产ID', type: 'input' as const, placeholder: '资产ID' },
  { key: 'period', label: '期间', type: 'input' as const, placeholder: 'yyyy-MM' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '已完成', value: 'completed' },
    { label: '待处理', value: 'pending' },
  ]},
]

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:refresh', fetchData)
})

function fetchData() {
  loading.value = true
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.assetId) params.assetId = Number(searchFilters.assetId)
  if (searchFilters.period) params.period = searchFilters.period
  if (searchFilters.status) params.status = searchFilters.status

  depreciationApi.getPage(params).then((res: any) => {
    if (res.data) {
      tableData.value = res.data.content || res.data.records || mockData()
      pagination.total = res.data.totalElements || res.data.total || mockData().length
      lastUpdated.value = new Date().toISOString()
      emit('update-count', pagination.total)
    }
  }).catch(() => {
    tableData.value = mockData()
    pagination.total = mockData().length
    emit('update-count', pagination.total)
  }).finally(() => {
    loading.value = false
  })
}

const mockData = (): any[] => [
  { id: 1, assetId: 1, assetCode: 'FA001', assetName: '办公电脑', period: '2024-01', depreciationDate: '2024-01-31', periodAmount: 125, accumulatedDepreciation: 500, netValue: 4500, assetOriginalValue: 5000, status: 'completed' },
  { id: 2, assetId: 2, assetCode: 'FA002', assetName: '打印机', period: '2024-01', depreciationDate: '2024-01-31', periodAmount: 75, accumulatedDepreciation: 200, netValue: 1800, assetOriginalValue: 2000, status: 'completed' },
  { id: 3, assetId: 1, assetCode: 'FA001', assetName: '办公电脑', period: '2024-02', depreciationDate: '2024-02-28', periodAmount: 125, accumulatedDepreciation: 625, netValue: 4375, assetOriginalValue: 5000, status: 'completed' },
  { id: 4, assetId: 2, assetCode: 'FA002', assetName: '打印机', period: '2024-02', depreciationDate: '2024-02-28', periodAmount: 75, accumulatedDepreciation: 275, netValue: 1725, assetOriginalValue: 2000, status: 'completed' },
]

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function handleBatchCalculate() {
  Modal.confirm({
    title: '批量计提折旧',
    content: '对本期间所有资产进行折旧计提？',
    okText: '确认计提',
    centered: true,
    onOk: async () => {
      try {
        await depreciationApi.batchCalculate()
        message.success('批量折旧计提完成')
        fetchData()
      } catch {
        message.error('批量折旧失败')
      }
    }
  })
}

function handleViewAsset(record: any) {
  message.info(`查看资产详情: ${record.assetCode}`)
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1
  fetchData()
}

function handleExport() {
  const headers = ['资产编码', '资产名称', '期间', '折旧日期', '本期折旧', '累计折旧', '净值', '资产原值', '状态']
  const rows = tableData.value.map(r => [
    r.assetCode, r.assetName, r.period, r.depreciationDate,
    formatAmount(r.periodAmount), formatAmount(r.accumulatedDepreciation),
    formatAmount(r.netValue), formatAmount(r.assetOriginalValue),
    r.status === 'completed' ? '已完成' : '待处理'
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `折旧记录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleBatchCalculate() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:refresh', fetchData)
})
</script>

<style scoped>
.depreciation-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-period { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
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
  font-size: 28px;
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

.asset-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.asset-name {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.depreciation {
  color: #faad14;
}

.amount-cell.success {
  color: #52c41a;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
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
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>