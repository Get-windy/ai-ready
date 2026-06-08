<template>
  <div class="transfer-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pendingCount }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ completedCount }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">调拨单总数</div>
        </div>
        <SwapOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'stock-transfer-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :selectable="true"
      add-text="新建调拨"
      @add="handleAdd"
      @view="handleView"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
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
        <a-button type="primary" ghost @click="handleBatchApprove">
          <template #icon><CheckOutlined /></template>
          批量审批
        </a-button>
      </template>

      <template #action="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else>
          <a-space :size="0" class="action-cell-inner">
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="审批">
              <a-button type="link" size="small" @click="handleApprove(record)">
                <template #icon><CheckCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的调拨单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无调拨单，点击「新建调拨」开始创建
          </p>
        </div>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="调拨单详情" width="700px" :footer="null">
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="调拨单号">{{ currentRecord.transferNo }}</a-descriptions-item>
        <a-descriptions-item label="调出仓库">{{ currentRecord.fromWarehouse }}</a-descriptions-item>
        <a-descriptions-item label="调入仓库">{{ currentRecord.toWarehouse }}</a-descriptions-item>
        <a-descriptions-item label="调拨日期">{{ currentRecord.transferDate }}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
        <a-descriptions-item label="调拨数量">{{ currentRecord.quantity || '-' }}</a-descriptions-item>
        <a-descriptions-item label="经手人">{{ currentRecord.handlerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-modal-footer"><a-button @click="detailVisible = false">关闭</a-button></div>
    </a-modal>

    <!-- 新建调拨弹窗 -->
    <a-modal v-model:open="addVisible" title="新建调拨单" width="800px" :confirm-loading="addSubmitting"
      @ok="handleAddSubmit" @cancel="handleAddCancel">
      <a-form ref="addFormRef" :model="addForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="addFormRules">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="调出仓库" name="fromWarehouseId">
              <a-select v-model:value="addForm.fromWarehouseId" placeholder="请选择调出仓库" :options="warehouseOptions" @change="handleFromWarehouseChange" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调入仓库" name="toWarehouseId">
              <a-select v-model:value="addForm.toWarehouseId" placeholder="请选择调入仓库" :options="filteredToWarehouseOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="调拨日期" name="transferDate">
              <a-date-picker v-model:value="addForm.transferDate" style="width: 100%" placeholder="请选择调拨日期" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调拨类型" name="transferType">
              <a-select v-model:value="addForm.transferType" placeholder="请选择调拨类型" :options="transferTypeOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="调拨原因" name="reason" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea v-model:value="addForm.reason" :rows="2" placeholder="请输入调拨原因" />
        </a-form-item>
      </a-form>
      <a-divider style="margin: 12px 0">调拨明细</a-divider>
      <div style="margin-bottom: 12px">
        <a-button type="dashed" size="small" @click="addTransferItem"><template #icon><PlusOutlined /></template>添加产品</a-button>
        <span v-if="addForm.items.length > 0" style="margin-left: 8px; color: #888; font-size: 12px">共 {{ addForm.items.length }} 条，合计数量: {{ totalTransferQty }}</span>
      </div>
      <a-table :columns="addItemColumns" :data-source="addForm.items" :pagination="false" size="small" row-key="key" :scroll="{ y: 250 }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <a-select v-model:value="addForm.items[index].productId" show-search placeholder="选择产品"
              :options="productOptions" style="width: 100%" size="small"
              @change="(val: number) => handleItemProductChange(index, val)" />
          </template>
          <template v-else-if="column.key === 'availableQty'">
            <a-tag :color="record.availableQty > 0 ? 'green' : 'red'">{{ record.availableQty }} {{ record.unit || '' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="addForm.items[index].quantity" :min="1" :max="record.availableQty" style="width: 100%" size="small" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="removeTransferItem(index)">删除</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, SearchOutlined, InboxOutlined, EllipsisOutlined,
  ClockCircleOutlined, SwapOutlined, CheckOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { stockTransferApi } from '@/api/erp'
import { executeBatch } from '@/utils/batchOperations'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

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
const pendingCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const completedCount = computed(() => tableData.value.filter(r => r.status === 2).length)

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
  { field: 'transferNo', title: '调拨单号', width: 160, sortable: true },
  { field: 'fromWarehouse', title: '调出仓库', width: 120 },
  { field: 'toWarehouse', title: '调入仓库', width: 120 },
  { field: 'transferDate', title: '调拨日期', width: 110 },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>`,
  },
  {
    field: 'quantity',
    title: '调拨数量',
    width: 100,
    align: 'right',
    formatter: ({ cellValue }: any) => `<span class="qty-cell">${cellValue || '-'}</span>`,
  },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 140, fixed: 'right', type: 'action' },
])

const selectedRowKeys = ref<number[]>([])
function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

const filterFields = [
  { key: 'transferNo', label: '调拨单号', type: 'input' as const, placeholder: '输入调拨单号' },
  { key: 'fromWarehouse', label: '调出仓库', type: 'input' as const, placeholder: '输入仓库名称' },
  { key: 'toWarehouse', label: '调入仓库', type: 'input' as const, placeholder: '输入仓库名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已完成', value: 2 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已完成' }
const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  return [{ label: '本页数量', value: tableData.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 新建调拨 ──
interface AddTransferItem { key: number; productId: number | undefined; productName: string; unit: string; availableQty: number; quantity: number }
let itemKeyCounter = 0
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ fromWarehouseId: undefined as number | undefined, toWarehouseId: undefined as number | undefined, transferDate: dayjs(), transferType: 1, reason: '', items: [] as AddTransferItem[] })
const addFormRules = {
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }],
  transferDate: [{ required: true, message: '请选择调拨日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入调拨原因', trigger: 'blur' }]
}
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
const filteredToWarehouseOptions = computed(() => warehouseOptions.filter(w => w.value !== addForm.fromWarehouseId))
const transferTypeOptions = [
  { value: 1, label: '仓库间调拨' }, { value: 2, label: '生产领料' }, { value: 3, label: '退料回库' }
]
const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' }, { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' }, { value: 4, label: 'PROD-004 包装箱(大)' }
]
const addItemColumns = [
  { title: '产品名称', key: 'productName' }, { title: '可用库存', key: 'availableQty', width: 100 },
  { title: '调拨数量', key: 'quantity', width: 110 }, { title: '操作', key: 'action', width: 60 }
]
const totalTransferQty = computed(() => addForm.items.reduce((sum, item) => sum + (item.quantity || 0), 0))

function handleFromWarehouseChange() {
  if (addForm.toWarehouseId === addForm.fromWarehouseId) addForm.toWarehouseId = undefined
  addForm.items = []
}
function handleItemProductChange(index: number, productId: number) {
  const product = productOptions.find(p => p.value === productId)
  if (product) {
    addForm.items[index].productName = product.label
    const stockMap: Record<number, { qty: number; unit: string }> = {
      1: { qty: 500, unit: '套' }, 2: { qty: 200, unit: '张' },
      3: { qty: 10000, unit: '个' }, 4: { qty: 300, unit: '个' }
    }
    const stock = stockMap[productId] || { qty: 0, unit: '-' }
    addForm.items[index].availableQty = stock.qty; addForm.items[index].unit = stock.unit
  }
}
function addTransferItem() { addForm.items.push({ key: itemKeyCounter++, productId: undefined, productName: '', unit: '', availableQty: 0, quantity: 1 }) }
function removeTransferItem(index: number) { addForm.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await stockTransferApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    tableData.value = pageData?.records || mockData()
    pagination.total = pageData?.totalElements ?? pageData?.total ?? mockData().length
    lastUpdated.value = new Date().toISOString()
  } catch {
    tableData.value = mockData()
    pagination.total = mockData().length
  }
  finally { loading.value = false }
}

const mockData = (): any[] => [
  { id: 1, transferNo: 'TR-2024-001', fromWarehouse: '主仓库', toWarehouse: '成品仓库', transferDate: '2024-01-25', status: 2, createTime: '2024-01-24 10:00', quantity: 100, handlerName: '张三', remark: '生产需要' },
  { id: 2, transferNo: 'TR-2024-002', fromWarehouse: '备品仓库', toWarehouse: '主仓库', transferDate: '2024-02-01', status: 1, createTime: '2024-01-31 14:00', quantity: 50, handlerName: '李四', remark: '补充库存' },
  { id: 3, transferNo: 'TR-2024-003', fromWarehouse: '半成品仓库', toWarehouse: '成品仓库', transferDate: '2024-02-10', status: 2, createTime: '2024-02-09 09:00', quantity: 200, handlerName: '王五', remark: '组装完成' },
  { id: 4, transferNo: 'TR-2024-004', fromWarehouse: '主仓库', toWarehouse: '备品仓库', transferDate: '2024-02-15', status: 0, createTime: '2024-02-14 16:00', quantity: 30, handlerName: '赵六', remark: '' },
]

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  addForm.fromWarehouseId = undefined; addForm.toWarehouseId = undefined; addForm.transferDate = dayjs()
  addForm.transferType = 1; addForm.reason = ''; addForm.items = []; itemKeyCounter = 0; addVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除调拨单', content: `确认删除调拨单 "${record.transferNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await stockTransferApi.create({ id: record.id, action: 'delete' } as any); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}
async function handleBatchDelete(ids: number[]) {
  await executeBatch(ids, (id: number) => stockTransferApi.create({ id, action: 'delete' } as any), '批量删除')
  fetchData()
}

const handleAddSubmit = async () => {
  try { await addFormRef.value?.validate() } catch { return }
  if (addForm.items.length === 0) { message.warning('请至少添加一条调拨明细'); return }
  const overStockItem = addForm.items.find(item => item.quantity > item.availableQty)
  if (overStockItem) { message.warning(`产品 "${overStockItem.productName}" 的调拨数量超过可用库存`); return }
  addSubmitting.value = true
  try {
    await stockTransferApi.create({
      fromWarehouseId: addForm.fromWarehouseId, toWarehouseId: addForm.toWarehouseId,
      transferDate: addForm.transferDate.format('YYYY-MM-DD'), transferType: addForm.transferType,
      reason: addForm.reason,
      items: addForm.items.map(item => ({ productId: item.productId, quantity: item.quantity }))
    })
    message.success('调拨单创建成功'); addVisible.value = false; pagination.current = 1; fetchData()
  } catch (err: any) { message.error(err?.message || '创建失败') }
  finally { addSubmitting.value = false }
}
const handleAddCancel = () => { addVisible.value = false }

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批', content: `确定要审批通过调拨单 "${record.transferNo}" 吗？审批通过后将自动生成对应的出库单和入库单。`,
    okText: '确认通过', cancelText: '取消', centered: true,
    async onOk() { try { await stockTransferApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err: any) { message.error(err?.message || '审批失败') } }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择调拨单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() { await executeBatch(keys, stockTransferApi.approve, '批量审批'); fetchData() }
  })
}

function handleExport() {
  const headers = ['调拨单号', '调出仓库', '调入仓库', '调拨日期', '状态', '调拨数量', '创建时间']
  const rows = tableData.value.map((row: any) => [
    row.transferNo || '', row.fromWarehouse || '', row.toWarehouse || '', row.transferDate || '',
    getStatusText(row.status), row.quantity || 0, row.createTime || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `调拨单_${new Date().toISOString().slice(0, 10)}.csv`
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

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'delete': handleDelete(record); break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
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
.transfer-list-page {
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

.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
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

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
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