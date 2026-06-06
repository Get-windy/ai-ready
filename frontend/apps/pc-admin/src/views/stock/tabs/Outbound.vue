<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'stock-outbound-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    add-text="新建出库"
    @add="handleAdd"
    @view="handleView"
    @delete="handleDelete"
    @batch-delete="handleBatchDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    :show-export="true"
    @export="handleExport"
  >
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配出库单">
        <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
        <a-button @click="handleResetFilters">清除筛选</a-button>
      </a-empty>
      <a-empty v-else description="暂无出库单">
        <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        <a-button type="primary" @click="handleAdd">新建出库单</a-button>
      </a-empty>
    </template>

    <template #batch-actions>
      <a-button size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
    </template>

    <template #action="{ record }">
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
  </TableList>

  <a-modal v-model:open="detailVisible" title="出库单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="出库单号">{{ currentRecord.outboundNo }}</a-descriptions-item>
      <a-descriptions-item label="出库类型">{{ currentRecord.outboundType }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="出库日期">{{ currentRecord.outboundDate }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="目标单号">{{ currentRecord.targetNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="经手人">{{ currentRecord.handlerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div class="detail-modal-footer"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <!-- 新建出库弹窗 -->
  <a-modal v-model:open="addVisible" title="新建出库单" width="800px" :confirm-loading="addSubmitting"
    @ok="handleAddSubmit" @cancel="handleAddCancel">
    <a-form ref="addFormRef" :model="addForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="addFormRules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="销售订单" name="orderNo">
            <a-select v-model:value="addForm.orderNo" show-search placeholder="请选择销售订单"
              :options="salesOrderOptions" :filter-option="filterOption" allow-clear @change="handleOrderChange" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出库类型" name="outboundType">
            <a-select v-model:value="addForm.outboundType" placeholder="请选择出库类型" :options="outboundTypeOptions" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="出库仓库" name="warehouseId">
            <a-select v-model:value="addForm.warehouseId" placeholder="请选择仓库" :options="warehouseOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出库日期" name="outboundDate">
            <a-date-picker v-model:value="addForm.outboundDate" style="width: 100%" placeholder="请选择出库日期" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="承运商" name="carrier">
            <a-input v-model:value="addForm.carrier" placeholder="请输入承运商名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="运单号" name="trackingNo">
            <a-input v-model:value="addForm.trackingNo" placeholder="请输入运单号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="addForm.remark" :rows="2" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
    <a-divider style="margin: 12px 0">出库明细</a-divider>
    <div style="margin-bottom: 12px">
      <a-button type="dashed" size="small" @click="addOutboundItem"><template #icon><PlusOutlined /></template>添加明细</a-button>
    </div>
    <a-table :columns="addItemColumns" :data-source="addForm.items" :pagination="false" size="small" row-key="key" :scroll="{ y: 250 }">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'productName'">
          <a-select v-model:value="addForm.items[index].productId" show-search placeholder="选择产品"
            :options="productOptions" style="width: 100%" size="small"
            @change="(val: number) => handleItemProductChange(index, val)" />
        </template>
        <template v-else-if="column.key === 'availableQty'">
          <a-tag :color="getAvailableQtyColor(index)">{{ getAvailableQty(index) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'quantity'">
          <a-input-number v-model:value="addForm.items[index].quantity" :min="1" style="width: 100%" size="small" />
        </template>
        <template v-else-if="column.key === 'unitPrice'">
          <a-input-number v-model:value="addForm.items[index].unitPrice" :min="0" :precision="2" style="width: 100%" size="small" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="removeOutboundItem(index)">删除</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, SearchOutlined, InboxOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { outboundApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch } from '@/utils/batchOperations'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  { title: '出库单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 160, sortable: true },
  { title: '出库类型', dataIndex: 'outboundType', key: 'outboundType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '出库日期', dataIndex: 'outboundDate', key: 'outboundDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'outboundNo', label: '出库单号', type: 'input' as const, placeholder: '输入出库单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已出库', value: 2 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已出库' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 新建出库 ──
interface AddOutboundItem { key: number; productId: number | undefined; productName: string; availableQty: number; quantity: number; unitPrice: number }
let itemKeyCounter = 0
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ orderNo: undefined as string | undefined, outboundType: 1, warehouseId: undefined as number | undefined, outboundDate: dayjs(), carrier: '', trackingNo: '', remark: '', items: [] as AddOutboundItem[] })
const addFormRules = { warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }], outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }] }
const outboundTypeOptions = [
  { value: 1, label: '销售出库' }, { value: 2, label: '退货出库' },
  { value: 3, label: '调拨出库' }, { value: 4, label: '其他出库' }
]
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
const salesOrderOptions = [
  { value: 'SO-2024-001', label: 'SO-2024-001 / 客户A' },
  { value: 'SO-2024-002', label: 'SO-2024-002 / 客户B' },
  { value: 'SO-2024-003', label: 'SO-2024-003 / 客户C' }
]
const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' }, { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' }, { value: 4, label: 'PROD-004 包装箱(大)' }
]
const addItemColumns = [
  { title: '产品名称', key: 'productName' }, { title: '可用库存', key: 'availableQty', width: 90 },
  { title: '出库数量', key: 'quantity', width: 110 }, { title: '单价', key: 'unitPrice', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

const filterOption = (input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())

function handleOrderChange(value: string | undefined) {
  if (value) {
    addForm.items = [
      { key: itemKeyCounter++, productId: 1, productName: 'PROD-001', availableQty: 500, quantity: 10, unitPrice: 12.50 },
      { key: itemKeyCounter++, productId: 2, productName: 'PROD-002', availableQty: 200, quantity: 5, unitPrice: 85.00 }
    ]
  }
}
function handleItemProductChange(index: number, productId: number) {
  const product = productOptions.find(p => p.value === productId)
  if (product) {
    addForm.items[index].productName = product.label
    const stockMap: Record<number, number> = { 1: 500, 2: 200, 3: 1000, 4: 300 }
    addForm.items[index].availableQty = stockMap[productId] || 0
  }
}
function getAvailableQty(index: number) { return addForm.items[index].availableQty }
function getAvailableQtyColor(index: number) {
  const item = addForm.items[index]
  if (item.quantity > item.availableQty) return 'red'
  if (item.availableQty < 50) return 'orange'
  return 'green'
}
function addOutboundItem() {
  addForm.items.push({ key: itemKeyCounter++, productId: undefined, productName: '', availableQty: 0, quantity: 1, unitPrice: 0 })
}
function removeOutboundItem(index: number) { addForm.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await outboundApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.totalElements ?? pageData?.total ?? 0; lastUpdated.value = new Date().toISOString()
  } catch { /* 获取数据失败 */ }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  addForm.orderNo = undefined; addForm.outboundType = 1; addForm.warehouseId = undefined
  addForm.outboundDate = dayjs(); addForm.carrier = ''; addForm.trackingNo = ''
  addForm.remark = ''; addForm.items = []; itemKeyCounter = 0; addVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除出库单', content: `确认删除出库单 "${record.outboundNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await outboundApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}
async function handleBatchDelete(ids: number[]) {
  await executeBatch(ids, outboundApi.delete, '批量删除')
  fetchData()
}

const handleAddSubmit = async () => {
  try { await addFormRef.value?.validate() } catch { return }
  if (addForm.items.length === 0) { message.warning('请至少添加一条出库明细'); return }
  const overStockItem = addForm.items.find(item => item.quantity > item.availableQty)
  if (overStockItem) { message.warning(`产品 "${overStockItem.productName}" 的出库数量超过可用库存`); return }
  addSubmitting.value = true
  try {
    await outboundApi.create({
      orderNo: addForm.orderNo, outboundType: addForm.outboundType, warehouseId: addForm.warehouseId,
      outboundDate: addForm.outboundDate.format('YYYY-MM-DD'), carrier: addForm.carrier,
      trackingNo: addForm.trackingNo, remark: addForm.remark,
      items: addForm.items.map(item => ({ productId: item.productId, quantity: item.quantity, unitPrice: item.unitPrice }))
    })
    message.success('出库单创建成功'); addVisible.value = false; pagination.current = 1; fetchData()
  } catch (err: any) { message.error(err?.message || '创建失败') }
  finally { addSubmitting.value = false }
}
const handleAddCancel = () => { addVisible.value = false }

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批', content: `确定要审批通过出库单 "${record.outboundNo}" 吗？`,
    okText: '确认通过', cancelText: '取消', centered: true,
    async onOk() { try { await outboundApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err: any) { message.error(err?.message || '审批失败') } }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择出库单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() { await executeBatch(keys, outboundApi.approve, '批量审批'); fetchData() }
  })
}

function handleExport() {
  const headers = ['出库单号', '出库类型', '仓库', '出库日期', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.outboundNo || '', row.outboundType || '', row.warehouseName || '', row.outboundDate || '',
    getStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '出库单')
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
.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.detail-modal-footer { text-align: right; margin-top: 16px; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
