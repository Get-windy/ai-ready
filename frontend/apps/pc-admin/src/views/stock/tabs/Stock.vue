<template>
  <div class="stock-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatNumber(totalQuantity) }}</div>
          <div class="stat-card-label">库存总量</div>
        </div>
        <DatabaseOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-available">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatNumber(totalAvailable) }}</div>
          <div class="stat-card-label">可用库存</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-frozen">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatNumber(totalFrozen) }}</div>
          <div class="stat-card-label">冻结库存</div>
        </div>
        <LockOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">产品种类</div>
        </div>
        <AppstoreOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :filter-fields="filterFields"
      :show-summary="true"
      :selectable="true"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @selection-change="handleSelectionChange"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #batch-actions="{ selectedRows }">
        <a-button size="small" @click="handleBatchExport(selectedRows)">
          <template #icon><DownloadOutlined /></template>
          导出选中
        </a-button>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="盘点">
            <a-button type="link" size="small" @click="handleCheck(record)">
              <template #icon><CheckSquareOutlined /></template>
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
              没有符合条件的库存记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无库存记录
            </p>
          </template>
        </div>
      </template>
    </VxeTableList>

    <!-- 盘点弹窗 -->
    <a-modal
      v-model:open="checkVisible"
      title="库存盘点"
      width="900px"
      :confirm-loading="checkSubmitting"
      @ok="handleCheckSubmit"
      @cancel="handleCheckCancel"
    >
      <a-form
        ref="checkFormRef"
        :model="checkForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :rules="checkFormRules"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="盘点仓库" name="warehouseId">
              <a-select v-model:value="checkForm.warehouseId" size="small" placeholder="请选择仓库" :options="warehouseOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="盘点日期" name="checkDate">
              <a-date-picker v-model:value="checkForm.checkDate" size="small" style="width: 100%" placeholder="请选择盘点日期" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea v-model:value="checkForm.remark" :rows="2" placeholder="请输入盘点备注" />
        </a-form-item>
      </a-form>

      <a-divider style="margin: 12px 0">盘点明细</a-divider>

      <VxeTableList
        :data-source="checkItems"
        :pagination="false as any"
        row-key="id"
        :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
        :show-export="false" :show-batch-delete="false"
        :columns="checkItemColumns"
      >
        <template #systemQtyCell="{ record }">
          {{ record.quantity || 0 }} {{ record.unit || '' }}
        </template>
        <template #actualQtyCell="{ record, index }">
          <a-input-number v-model:value="checkItems[index].actualQty" :min="0" size="small" style="width: 100%" placeholder="实盘数量" />
        </template>
        <template #diffCell="{ record, index }">
          <a-tag :color="getDiffColor(index)">{{ getDiffQty(index) }}</a-tag>
        </template>
      </VxeTableList>
    </a-modal>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      :title="detailRecord?.productName || '库存详情'"
      placement="right"
      width="90vw"
      :footer-style="{ textAlign: 'right' }"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 12 }">
        <template v-if="detailData">
          <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
            <a-descriptions-item label="产品编码">{{ detailData.productCode }}</a-descriptions-item>
            <a-descriptions-item label="产品名称">{{ detailData.productName }}</a-descriptions-item>
            <a-descriptions-item label="规格型号">{{ detailData.specification || '-' }}</a-descriptions-item>
            <a-descriptions-item label="单位">{{ detailData.unit || '-' }}</a-descriptions-item>
            <a-descriptions-item label="当前库存">
              <span :style="{color: (detailData.quantity || 0) <= (detailData.minStock || 0) ? '#ff4d4f' : '#3f8600', fontWeight: 600}">{{ detailData.quantity || 0 }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="可用库存">{{ detailData.availableQuantity || 0 }}</a-descriptions-item>
            <a-descriptions-item label="锁定库存">{{ detailData.lockedQuantity || detailData.frozenQuantity || 0 }}</a-descriptions-item>
            <a-descriptions-item label="安全库存">{{ detailData.minStock || '-' }}</a-descriptions-item>
            <a-descriptions-item label="最大库存">{{ detailData.maxStock || '-' }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="最后入库">{{ detailData.lastInboundDate || '-' }}</a-descriptions-item>
            <a-descriptions-item label="最后出库">{{ detailData.lastOutboundDate || '-' }}</a-descriptions-item>
          </a-descriptions>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.id)">重试</a-button>
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
  EyeOutlined, CheckSquareOutlined, SearchOutlined, InboxOutlined, WarningOutlined,
  DatabaseOutlined, CheckCircleOutlined, LockOutlined, AppstoreOutlined, ReloadOutlined, DownloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { stockApi, stockCheckApi } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
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
const exportLoading = ref(false)
const tableData = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')
const warehouseOptions = ref<{ value: number; label: string }[]>([])
const warehouseOptionsLoading = ref(false)

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const totalQuantity = computed(() => tableData.value.reduce((s, r) => s + (r.quantity || 0), 0))
const totalAvailable = computed(() => tableData.value.reduce((s, r) => s + (r.availableQuantity || 0), 0))
const totalFrozen = computed(() => tableData.value.reduce((s, r) => s + (r.frozenQuantity || 0), 0))



// vxe-table 列定义
const vxeColumns = computed(() => [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称', width: 160, sortable: true },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  {
    field: 'quantity',
    title: '库存数量',
    width: 100,
    align: 'right',
    formatter: ({ cellValue, row }: any) => `${cellValue || 0} ${row.unit || ''}`,
  },
  {
    field: 'availableQuantity',
    title: '可用数量',
    width: 100,
    align: 'center',
    formatter: ({ cellValue, row }: any) => {
      const color = cellValue > 0 ? 'green' : 'red'
      return `<span class="ant-tag ant-tag-${color}">${cellValue} ${row.unit || ''}</span>`
    },
  },
  {
    field: 'frozenQuantity',
    title: '冻结数量',
    width: 100,
    align: 'right',
    formatter: ({ cellValue }: any) => `<span class="qty-cell frozen">${cellValue || 0}</span>`,
  },
  { field: 'action', title: '操作', width: 120, fixed: 'right', type: 'action' },
])

const filterFields = [
  { key: 'productCode', label: '产品编码', type: 'input' as const, placeholder: '输入产品编码' },
  { key: 'productName', label: '产品名称', type: 'input' as const, placeholder: '输入产品名称' },
  { key: 'warehouseName', label: '仓库', type: 'input' as const, placeholder: '输入仓库名称' }
]

function formatNumber(num: number): string {
  return num?.toLocaleString?.('zh-CN') || '0'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await stockApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    tableData.value = pageData?.records || []
    pagination.total = pageData?.totalElements ?? pageData?.total ?? 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
    emit('update-count', pagination.total)
  } catch (err) {
    console.warn('[库存管理] 获取库存列表失败', err)
    tableData.value = []
    pagination.total = 0
    hasError.value = true
    emit('update-count', pagination.total)
  }
  finally { loading.value = false }
}


// ── 详情抽屉 ──
const detailVisible = ref(false)
const detailRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    detailData.value = await stockApi.getById(id) as any
  } catch (err: any) {
    console.warn('[库存管理] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function handleView(record: any) {
  detailRecord.value = record
  detailVisible.value = true
  fetchDetail(record.id)
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = null
  detailError.value = null
}

function handleDetailRefresh() {
  if (detailRecord.value?.id) fetchDetail(detailRecord.value.id)
}

// ── 盘点 ──
const checkVisible = ref(false)
const checkSubmitting = ref(false)
const checkFormRef = ref<FormInstance>()
const checkForm = reactive({ warehouseId: undefined as number | undefined, checkDate: dayjs(), remark: '' })
const checkFormRules: any = {
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }]
}
async function loadWarehouses() {
  warehouseOptionsLoading.value = true
  try {
    const res = await stockApi.getWarehouses()
    const data = (res as any)?.data ?? res
    warehouseOptions.value = (Array.isArray(data) ? data : []).map((w: any) => ({
      value: w.id, label: w.warehouseName || w.name
    }))
    if (warehouseOptions.value.length === 0) {
      warehouseOptions.value = [
        { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
        { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
      ]
    }
  } catch {
    warehouseOptions.value = [
      { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
      { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
    ]
  } finally {
    warehouseOptionsLoading.value = false
  }
}
interface CheckItem { id: number; productCode: string; productName: string; specification?: string; unit?: string; quantity: number; actualQty: number | null; warehouseName?: string }
const checkItems = ref<CheckItem[]>([])
const checkItemColumns = [
  { title: '产品编码', field: 'productCode', width: 140 },
  { title: '产品名称', field: 'productName' },
  { title: '规格型号', field: 'specification', width: 100 },
  { title: '系统库存', field: 'systemQty', width: 110, slotName: 'systemQtyCell' },
  { title: '实盘数量', field: 'actualQty', width: 120, slotName: 'actualQtyCell' },
  { title: '差异', field: 'diff', width: 100, slotName: 'diffCell' }
]
function getDiffQty(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return '-'
  const diff = item.actualQty - (item.quantity || 0)
  return diff === 0 ? '无差异' : (diff > 0 ? `+${diff}` : `${diff}`)
}
function getDiffColor(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return 'default'
  const diff = item.actualQty - (item.quantity || 0)
  if (diff === 0) return 'green'
  if (diff > 0) return 'blue'
  return 'red'
}
function handleCheck(record: any) {
  checkForm.warehouseId = undefined
  checkForm.checkDate = dayjs()
  checkForm.remark = ''
  if (record.warehouseName) {
    const matched = (warehouseOptions as any).find(w => w.label === record.warehouseName)
    if (matched) checkForm.warehouseId = matched.value
  }
  checkItems.value = [{
    id: record.id || 1, productCode: record.productCode || '', productName: record.productName || '',
    specification: record.specification || '', unit: record.unit || '个', quantity: record.quantity || 0, actualQty: null
  }]
  checkVisible.value = true
}
async function handleCheckSubmit() {
  try { await checkFormRef.value?.validate() } catch (err) { console.warn('[库存管理] 表单验证失败', err); return }
  checkSubmitting.value = true
  try {
    await stockCheckApi.create({
      warehouseId: checkForm.warehouseId, checkDate: checkForm.checkDate.format('YYYY-MM-DD'),
      remark: checkForm.remark, items: checkItems.value.map(item => ({
        productId: item.id, systemQty: item.quantity, actualQty: item.actualQty ?? 0
      }))
    })
    message.success('盘点单创建成功')
    checkVisible.value = false
    fetchData()
  } catch (err: any) { console.warn('[库存管理] 提交盘点失败', err); message.error(err?.message || '盘点提交失败') }
  finally { checkSubmitting.value = false }
}
function handleCheckCancel() { checkVisible.value = false }

const selectedRowKeys = ref<number[]>([])
function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

function handleBatchExport(rows: any[]) {
  if (rows.length === 0) { message.warning('请先选择要导出的数据'); return }
  exportLoading.value = true
  try {
    const headers = ['产品编码', '产品名称', '规格型号', '仓库', '库存数量', '可用数量', '冻结数量']
    const csvRows = rows.map((row: any) => [
      row.productCode || '', row.productName || '', row.specification || '', row.warehouseName || '',
      row.quantity || 0, row.availableQuantity || 0, row.frozenQuantity || 0
    ])
    const csv = ['\ufeff' + headers.join(','), ...csvRows.map(r => r.join(','))].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `库存导出_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('选中数据已导出')
  } finally {
    exportLoading.value = false
  }
}

// ── 导出 ──
async function handleExport() {
  exportLoading.value = true
  try {
    const res = await stockApi.export({ ...searchFilters })
    let rows: any[]
    if (Array.isArray(res)) rows = res
    else if ((res as any)?.data && Array.isArray((res as any).data)) rows = (res as any).data
    else rows = tableData.value

    const headers = ['产品编码', '产品名称', '规格型号', '仓库', '库存数量', '可用数量', '冻结数量']
    const csvRows = rows.map((row: any) => [
      row.productCode || '', row.productName || '', row.specification || '', row.warehouseName || '',
      row.quantity || 0, row.availableQuantity || 0, row.frozenQuantity || 0
    ])
    const csv = ['\ufeff' + headers.join(','), ...csvRows.map(r => r.join(','))].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `库存报表_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err: any) {
    console.warn('[库存管理] 导出失败', err)
    message.error(err?.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
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

function handleParentCreate() {
  const first = tableData.value[0]
  if (first) handleView(first)
}

function handleParentFilter() {
  // 触发筛选面板展开
  tableRef.value?.toggleFilter?.()
}

onMounted(() => {
  fetchData()
  loadWarehouses()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('stock:create', handleParentCreate)
  window.addEventListener('stock:filter', handleParentFilter)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('stock:create', handleParentCreate)
  window.removeEventListener('stock:filter', handleParentFilter)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.stock-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.stock-list-page > :deep(.vxe-table-list-container) {
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

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-available { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-frozen { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
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


.qty-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

.qty-cell.frozen {
  color: #faad14;
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
