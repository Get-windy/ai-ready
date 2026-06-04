<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'sale-return-list'"
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

  <a-modal v-model:open="detailVisible" title="退货单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <a-modal v-model:open="formModalVisible" title="新建退货单" width="800px" centered
    :confirm-loading="formSubmitting" ok-text="确认创建" cancel-text="取消"
    @ok="handleFormSubmit" @cancel="formModalVisible = false">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="销售订单" name="orderNo"><a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" /></a-form-item>
      <a-form-item label="客户" name="customerName"><a-input v-model:value="formData.customerName" placeholder="请输入客户名称" /></a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="退货原因" name="reason" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.reason" placeholder="请选择退货原因">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="规格不符">规格不符</a-select-option>
              <a-select-option value="数量错误">数量错误</a-select-option>
              <a-select-option value="客户取消">客户取消</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="退货日期" name="returnDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.returnDate" style="width: 100%" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="退款金额" name="refundAmount">
        <a-input-number v-model:value="formData.refundAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" placeholder="请输入退款金额" />
      </a-form-item>
      <a-form-item label="退货明细" required>
        <div style="margin-bottom: 8px">
          <a-button type="dashed" size="small" @click="addItem"><template #icon><PlusOutlined /></template>添加产品</a-button>
        </div>
        <a-table :data-source="formData.items" :pagination="false" row-key="key" size="small" bordered :columns="itemColumns">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'"><a-input v-model:value="record.productName" placeholder="产品名称" size="small" /></template>
            <template v-else-if="column.key === 'quantity'"><a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" /></template>
            <template v-else-if="column.key === 'unitPrice'"><a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" /></template>
            <template v-else-if="column.key === 'amount'">¥{{ (record.quantity * record.unitPrice).toFixed(2) }}</template>
            <template v-else-if="column.key === 'action'"><a-button type="link" danger size="small" @click="removeItem(index)" :disabled="formData.items.length <= 1">删除</a-button></template>
          </template>
        </a-table>
      </a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { saleReturnApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch } from '@/utils/batchOperations'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 160, sortable: true },
  { title: '销售订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '退货日期', dataIndex: 'returnDate', key: 'returnDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]
const filterFields = [
  { key: 'returnNo', label: '退货单号', type: 'input' as const, placeholder: '输入退货单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已退货', value: 2 }, { label: '已拒绝', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]
const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已退货', 3: '已拒绝' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

interface ReturnItem { key: number; productName: string; quantity: number; unitPrice: number }
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({ orderNo: '', customerName: '', reason: undefined as string | undefined, returnDate: undefined as any, refundAmount: 0, items: [] as ReturnItem[], remark: '' })
const defaultItem = (): ReturnItem => ({ key: Date.now() + Math.random(), productName: '', quantity: 1, unitPrice: 0 })
const formRules = { orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }], customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }], reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }], returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }] }
const itemColumns = [
  { title: '产品名称', key: 'productName', dataIndex: 'productName' },
  { title: '数量', key: 'quantity', dataIndex: 'quantity', width: 100 },
  { title: '单价', key: 'unitPrice', dataIndex: 'unitPrice', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]
const addItem = () => { formData.items.push(defaultItem()) }
const removeItem = (index: number) => { if (formData.items.length > 1) formData.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await saleReturnApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
  } catch { message.error('获取退货单列表失败') }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  formData.orderNo = ''; formData.customerName = ''; formData.reason = undefined
  formData.returnDate = undefined; formData.refundAmount = 0; formData.items = [defaultItem()]; formData.remark = ''
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  try { await saleReturnApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}
async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => saleReturnApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await saleReturnApi.create({
      orderNo: formData.orderNo, customerName: formData.customerName, reason: formData.reason,
      returnDate: formData.returnDate, refundAmount: formData.refundAmount, remark: formData.remark,
      items: formData.items.map(item => ({ productName: item.productName, quantity: item.quantity, unitPrice: item.unitPrice }))
    })
    message.success('新建退货单成功'); formModalVisible.value = false; fetchData()
  } catch { message.error('新建退货单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批退货单', content: `审批退货单 "${record.returnNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await saleReturnApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择退货单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => saleReturnApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  const headers = ['退货单号', '销售订单', '客户', '退货日期', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.returnNo || '', row.orderNo || '', row.customerName || '', row.returnDate || '',
    getStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '退货单')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
