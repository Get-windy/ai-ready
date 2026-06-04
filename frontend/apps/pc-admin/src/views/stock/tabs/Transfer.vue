<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'stock-transfer-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
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
    :show-export="true"
    @export="handleExport"
  >

    <template #batch-actions>
      <a-button size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="4">
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
      </a-space>
    </template>
  </TableList>

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
    <div style="text-align: right; margin-top: 16px"><a-button @click="detailVisible = false">关闭</a-button></div>
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
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { stockTransferApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch } from '@/utils/batchOperations'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '调拨单号', dataIndex: 'transferNo', key: 'transferNo', width: 160, sortable: true },
  { title: '调出仓库', dataIndex: 'fromWarehouse', key: 'fromWarehouse', width: 120 },
  { title: '调入仓库', dataIndex: 'toWarehouse', key: 'toWarehouse', width: 120 },
  { title: '调拨日期', dataIndex: 'transferDate', key: 'transferDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

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
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
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
    dataSource.value = pageData?.records || []; pagination.total = pageData?.totalElements ?? pageData?.total ?? 0
  } catch { /* 获取数据失败 */ }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  addForm.fromWarehouseId = undefined; addForm.toWarehouseId = undefined; addForm.transferDate = dayjs()
  addForm.transferType = 1; addForm.reason = ''; addForm.items = []; itemKeyCounter = 0; addVisible.value = true
}

async function handleDelete(record: any) {
  try { await stockTransferApi.create({ id: record.id, action: 'delete' } as any); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
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
    message.success('调拨单创建成功'); addVisible.value = false; fetchData()
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
  const headers = ['调拨单号', '调出仓库', '调入仓库', '调拨日期', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.transferNo || '', row.fromWarehouse || '', row.toWarehouse || '', row.transferDate || '',
    getStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '调拨单')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
