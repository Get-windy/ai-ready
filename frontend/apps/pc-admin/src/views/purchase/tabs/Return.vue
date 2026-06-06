<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'purchase-return-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建退货"
    @add="handleAdd"
    @view="handleView"
    @delete="handleDelete"
    @batch-delete="handleBatchDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    @export="handleExport"
  >
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配退货单">
        <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
        <a-button @click="handleResetFilters">清除筛选</a-button>
      </a-empty>
      <a-empty v-else description="暂无退货单">
        <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        <a-button type="primary" @click="handleAdd">新建退货单</a-button>
      </a-empty>
    </template>

    <template #batch-actions>
      <a-button size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
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
      </a-space>
    </template>
  </TableList>

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="退货单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
      <a-descriptions-item label="采购订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
      <a-descriptions-item label="退货金额">¥{{ currentRecord.totalAmount?.toFixed(2) }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="备注">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div class="detail-modal-footer">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建退货弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    title="新建退货单"
    width="750px"
    centered
    :confirm-loading="formSubmitting"
    ok-text="确认创建"
    cancel-text="取消"
    @ok="handleFormSubmit"
    @cancel="formModalVisible = false"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 19 }"
    >
      <a-form-item label="采购订单号" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入采购订单号" />
      </a-form-item>
      <a-form-item label="退货原因" name="reason">
        <a-select v-model:value="formData.reason" placeholder="请选择退货原因">
          <a-select-option value="质量问题">质量问题</a-select-option>
          <a-select-option value="规格不符">规格不符</a-select-option>
          <a-select-option value="数量错误">数量错误</a-select-option>
          <a-select-option value="延迟交货">延迟交货</a-select-option>
          <a-select-option value="其他">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="退款方式" name="refundType">
        <a-radio-group v-model:value="formData.refundType">
          <a-radio :value="1">原路退回</a-radio>
          <a-radio :value="2">抵扣货款</a-radio>
          <a-radio :value="3">线下退款</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="退货日期" name="returnDate">
        <a-date-picker v-model:value="formData.returnDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
    <div class="form-items-section">
      <div class="form-items-header">
        <span class="form-items-title">退货物料明细</span>
        <a-button size="small" type="dashed" @click="handleAddReturnItem">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </div>
      <a-table
        :columns="returnItemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        row-key="tempKey"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="handleRemoveReturnItem(index)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseReturnTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, EyeOutlined, CheckCircleOutlined, SearchOutlined, InboxOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import TableList from '@/components/TableList/TableList.vue'
import { purchaseReturnApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'
import { executeBatch, validateSelection } from '@/utils/batchOperations'

const { execute: executeExport } = useExport()
const userStore = useUserStore()
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
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 160, sortable: true },
  { title: '采购订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '退货日期', dataIndex: 'returnDate', key: 'returnDate', width: 110, type: 'date' as const },
  { title: '退货金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, type: 'currency' as const, sortable: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'returnNo', label: '退货单号', type: 'input' as const, placeholder: '输入退货单号' },
  { key: 'orderNo', label: '采购订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已退货', value: 2 }, { label: '已拒绝', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已退货', 3: '已拒绝' }

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalAmount = dataSource.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  return [
    { label: '本页金额合计', value: totalAmount, type: 'currency' as const },
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

let itemCounter = 0
const genTempKey = () => `return_item_${++itemCounter}_${Date.now()}`

interface ReturnItemForm {
  tempKey: string
  productName: string
  quantity: number
  unitPrice: number
}

const formData = reactive({
  orderNo: '',
  reason: undefined as string | undefined,
  refundType: 1,
  returnDate: undefined as any,
  remark: '',
  items: [] as ReturnItemForm[]
})

const formRules = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
}

const returnItemColumns = [
  { title: '物料名称', key: 'productName', width: 150 },
  { title: '退货数量', key: 'quantity', width: 100 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

const handleAddReturnItem = () => {
  formData.items.push({ tempKey: genTempKey(), productName: '', quantity: 1, unitPrice: 0 })
}
const handleRemoveReturnItem = (index: number) => { formData.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseReturnApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch {
    message.error('获取退货单列表失败')
  } finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }

function handleAdd() {
  formData.orderNo = ''; formData.reason = undefined; formData.refundType = 1
  formData.returnDate = undefined; formData.remark = ''; formData.items = []
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除退货单', content: `确认删除退货单 "${record.returnNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await purchaseReturnApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => purchaseReturnApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await purchaseReturnApi.create({
      orderNo: formData.orderNo, reason: formData.reason, refundType: formData.refundType,
      returnDate: formData.returnDate, remark: formData.remark,
      items: formData.items.map(item => ({ productName: item.productName, quantity: item.quantity, unitPrice: item.unitPrice }))
    })
    message.success('新建退货单成功'); formModalVisible.value = false; fetchData()
  } catch { message.error('新建退货单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批退货单', content: `审批退货单 "${record.returnNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await purchaseReturnApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (!validateSelection(keys, '审批')) return
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => purchaseReturnApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '退货单',
    headers: ['退货单号', '采购订单', '供应商', '退货日期', '退货金额', '状态', '创建时间'],
    fetchAll: () => purchaseReturnApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.returnNo || '', row.orderNo || '', row.supplierName || '', row.returnDate || '',
      (row.totalAmount || 0).toFixed(2), getStatusText(row.status), row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.returnNo || '', row.orderNo || '', row.supplierName || '', row.returnDate || '',
      (row.totalAmount || 0).toFixed(2), getStatusText(row.status), row.createTime || ''
    ]),
    total: pagination.total,
  })
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }

const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('purchase:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('purchase:refresh', fetchData)
})
</script>

<style scoped>
.detail-modal-footer { text-align: right; margin-top: 16px; }
.form-items-section { margin-top: 16px; }
.form-items-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.form-items-title { font-weight: 500; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
