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
      :data-source="tableData"
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
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
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
              没有符合条件的批次记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无批次记录
            </p>
          </template>
        </div>
      </template>
    </VxeTableList>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      :title="detailData?.batchNo || '批次详情'"
      placement="right"
      width="90vw"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 12 }">
        <template v-if="detailData">
          <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
            <a-descriptions-item label="批次号">{{ detailData.batchNo }}</a-descriptions-item>
            <a-descriptions-item label="产品编码">{{ detailData.productCode }}</a-descriptions-item>
            <a-descriptions-item label="产品名称">{{ detailData.productName }}</a-descriptions-item>
            <a-descriptions-item label="生产日期">{{ detailData.productionDate }}</a-descriptions-item>
            <a-descriptions-item label="有效期"><a-tag :color="getExpiryColor(detailData.expiryDate)">{{ detailData.expiryDate }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="状态"><a-tag :color="getBatchStatusColor(detailData.status)">{{ getBatchStatusText(detailData.status) }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="库存数量">{{ detailData.quantity || '-' }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(currentRecord?.id)">重试</a-button>
          </template>
        </a-result>
      </a-skeleton>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  EyeOutlined, SearchOutlined, InboxOutlined,
  CheckCircleOutlined, WarningOutlined, StopOutlined, TagOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { batchApi } from '@/api/erp'
import dayjs from 'dayjs'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const emit = defineEmits(['update-count'])

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
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

// ── 详情抽屉 ──
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    detailData.value = await batchApi.getById(id) as any
  } catch (err: any) {
    console.warn('[批次管理] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = null
  detailError.value = null
}

function handleDetailRefresh() {
  if (currentRecord.value?.id) fetchDetail(currentRecord.value.id)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await batchApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    tableData.value = pageData?.records || []
    pagination.total = pageData?.totalElements ?? pageData?.total ?? 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
    emit('update-count', pagination.total)
  } catch (err) {
    console.warn('[批次管理] 获取批次列表失败', err)
    tableData.value = []
    pagination.total = 0
    hasError.value = true
    emit('update-count', pagination.total)
  }
  finally { loading.value = false }
}


function handleView(record: any) {
  currentRecord.value = record
  detailData.value = null
  detailError.value = null
  detailVisible.value = true
  fetchDetail(record.id)
}

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
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleView(tableData.value[0]) }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('stock:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('stock:refresh', fetchData)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.batch-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.batch-list-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
