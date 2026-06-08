<template>
  <div class="batch-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-qualified">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ qualifiedCount }}</div>
          <div class="stat-card-label">合格批次</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-expiring">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ expiringCount }}</div>
          <div class="stat-card-label">临期批次</div>
        </div>
        <WarningOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-expired">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ expiredCount }}</div>
          <div class="stat-card-label">过期批次</div>
        </div>
        <StopOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">批次总数</div>
        </div>
        <TagOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'stock-batch-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :selectable="true"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
      :show-export="true"
      @export="handleExport"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #action="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else>
          <a-space :size="4">
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的批次记录，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无批次记录
          </p>
        </div>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="批次详情" width="700px" :footer="null">
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="批次号">{{ currentRecord.batchNo }}</a-descriptions-item>
        <a-descriptions-item label="产品编码">{{ currentRecord.productCode }}</a-descriptions-item>
        <a-descriptions-item label="产品名称">{{ currentRecord.productName }}</a-descriptions-item>
        <a-descriptions-item label="生产日期">{{ currentRecord.productionDate }}</a-descriptions-item>
        <a-descriptions-item label="有效期"><a-tag :color="getExpiryColor(currentRecord.expiryDate)">{{ currentRecord.expiryDate }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="getBatchStatusColor(currentRecord.status)">{{ getBatchStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="库存数量">{{ currentRecord.quantity || '-' }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-modal-footer"><a-button @click="detailVisible = false">关闭</a-button></div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  EyeOutlined, SearchOutlined, InboxOutlined,
  CheckCircleOutlined, WarningOutlined, StopOutlined, TagOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { batchApi } from '@/api/erp'
import dayjs from 'dayjs'

const emit = defineEmits(['update-count'])

const tableRef = ref()
const loading = ref(false)
const tableData = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const qualifiedCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const expiringCount = computed(() => tableData.value.filter(r => r.status === 2).length)
const expiredCount = computed(() => tableData.value.filter(r => r.status === 3).length)

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

// vxe-table 列定义
const vxeColumns = computed(() => [
  { field: 'batchNo', title: '批次号', width: 180, sortable: true },
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称', width: 160 },
  { field: 'productionDate', title: '生产日期', width: 110 },
  {
    field: 'expiryDate',
    title: '有效期',
    width: 110,
    align: 'center',
    formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getExpiryColor(cellValue)}">${cellValue || '-'}</span>`,
  },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getBatchStatusColor(cellValue)}">${getBatchStatusText(cellValue)}</span>`,
  },
  {
    field: 'quantity',
    title: '库存数量',
    width: 100,
    align: 'right',
    formatter: ({ cellValue }: any) => `<span class="qty-cell">${cellValue || '-'}</span>`,
  },
  { field: 'action', title: '操作', width: 80, fixed: 'right', type: 'action' },
])

const selectedRowKeys = ref<number[]>([])
function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

const filterFields = [
  { key: 'batchNo', label: '批次号', type: 'input' as const, placeholder: '输入批次号' },
  { key: 'productCode', label: '产品编码', type: 'input' as const, placeholder: '输入产品编码' },
  { key: 'productName', label: '产品名称', type: 'input' as const, placeholder: '输入产品名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待检', value: 0 }, { label: '合格', value: 1 }, { label: '临期', value: 2 }, { label: '过期', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  return [{ label: '本页数量', value: tableData.value.length, type: 'default' as const }]
})

function getBatchStatusColor(status: number): string {
  const colors: Record<number, string> = { 0: 'default', 1: 'green', 2: 'orange', 3: 'red' }
  return colors[status] || 'default'
}
function getBatchStatusText(status: number): string {
  const texts: Record<number, string> = { 0: '待检', 1: '合格', 2: '临期', 3: '过期' }
  return texts[status] || '未知'
}
function getExpiryColor(expiryDate: string): string {
  if (!expiryDate) return 'default'
  const today = new Date()
  const expiry = new Date(expiryDate)
  const days = Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (days < 0) return 'red'
  if (days < 30) return 'orange'
  return 'green'
}

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

async function fetchData() {
  loading.value = true
  try {
    const res = await batchApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    tableData.value = pageData?.records || mockData()
    pagination.total = pageData?.totalElements ?? pageData?.total ?? mockData().length
    lastUpdated.value = new Date().toISOString()
    emit('update-count', pagination.total)
  } catch {
    tableData.value = mockData()
    pagination.total = mockData().length
    emit('update-count', pagination.total)
  }
  finally { loading.value = false }
}

const mockData = (): any[] => [
  { id: 1, batchNo: 'B-2024-001', productCode: 'PROD-001', productName: '螺丝螺母套装', productionDate: '2024-01-01', expiryDate: '2025-01-01', status: 1, quantity: 500, warehouseName: '主仓库', createTime: '2024-01-02 10:00' },
  { id: 2, batchNo: 'B-2024-002', productCode: 'PROD-002', productName: '不锈钢板材', productionDate: '2024-01-15', expiryDate: '2024-07-15', status: 2, quantity: 200, warehouseName: '主仓库', createTime: '2024-01-16 14:00' },
  { id: 3, batchNo: 'B-2024-003', productCode: 'PROD-003', productName: '电子元件A型', productionDate: '2024-02-01', expiryDate: '2024-03-01', status: 3, quantity: 50, warehouseName: '备品仓库', createTime: '2024-02-02 09:00' },
  { id: 4, batchNo: 'B-2024-004', productCode: 'PROD-004', productName: '包装箱(大)', productionDate: '2024-02-10', expiryDate: '2026-02-10', status: 1, quantity: 300, warehouseName: '成品仓库', createTime: '2024-02-11 16:00' },
  { id: 5, batchNo: 'B-2024-005', productCode: 'PROD-005', productName: '电机驱动器', productionDate: '2024-02-20', expiryDate: '2024-03-20', status: 0, quantity: 20, warehouseName: '半成品仓库', createTime: '2024-02-21 11:00' },
]

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }

function handleExport() {
  const headers = ['批次号', '产品编码', '产品名称', '生产日期', '有效期', '状态', '库存数量', '创建时间']
  const rows = tableData.value.map((row: any) => [
    row.batchNo || '', row.productCode || '', row.productName || '', row.productionDate || '',
    row.expiryDate || '', getBatchStatusText(row.status), row.quantity || 0, row.createTime || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `批次报表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleView(tableData.value[0]) }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.batch-list-page {
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

.stat-qualified { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-expiring { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-expired { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

.qty-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

.detail-modal-footer { text-align: right; margin-top: 16px; }

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