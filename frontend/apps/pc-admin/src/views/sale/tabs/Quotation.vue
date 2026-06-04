<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'sale-quotation-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建报价"
    @add="handleAdd"
    @edit="handleEdit"
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
        <a-tooltip v-if="record.status === 0" title="编辑">
          <a-button type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 0" title="发送">
          <a-button type="link" size="small" @click="handleSend(record)">
            <template #icon><SendOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 1" title="转订单">
          <a-button type="link" size="small" @click="handleConvert(record)">
            <template #icon><SwapOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </template>
  </TableList>

  <a-modal v-model:open="detailVisible" title="报价单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="报价单号">{{ currentRecord.quotationNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="报价日期">{{ currentRecord.quotationDate }}</a-descriptions-item>
      <a-descriptions-item label="有效期">{{ currentRecord.validDate }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <a-modal v-model:open="formVisible" :title="isEdit ? '编辑报价单' : '新建报价单'" width="800px" :confirm-loading="formSubmitting" @ok="handleFormSubmit" @cancel="formVisible = false">
    <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="客户" name="customerName"><a-input v-model:value="formState.customerName" placeholder="请输入客户名称" /></a-form-item>
      <a-row>
        <a-col :span="12"><a-form-item label="报价日期" name="quotationDate" :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }"><a-date-picker v-model:value="formState.quotationDate" style="width: 100%" /></a-form-item></a-col>
        <a-col :span="12"><a-form-item label="有效期至" name="validUntil" :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }"><a-date-picker v-model:value="formState.validUntil" style="width: 100%" /></a-form-item></a-col>
      </a-row>
      <a-form-item label="产品明细" required>
        <div style="margin-bottom: 8px"><a-button type="dashed" size="small" @click="addItem"><template #icon><PlusOutlined /></template>添加产品</a-button></div>
        <a-table :data-source="formState.items" :pagination="false" row-key="key" size="small" bordered :columns="itemColumns">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'"><a-input v-model:value="record.productName" placeholder="产品名称" size="small" /></template>
            <template v-else-if="column.key === 'quantity'"><a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" /></template>
            <template v-else-if="column.key === 'unitPrice'"><a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" /></template>
            <template v-else-if="column.key === 'discount'"><a-input-number v-model:value="record.discount" :min="0" :max="100" size="small" style="width: 100%" />%</template>
            <template v-else-if="column.key === 'amount'">¥{{ (record.quantity * record.unitPrice * (1 - record.discount / 100)).toFixed(2) }}</template>
            <template v-else-if="column.key === 'action'"><a-button type="link" danger size="small" @click="removeItem(index)" :disabled="formState.items.length <= 1">删除</a-button></template>
          </template>
        </a-table>
      </a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="formState.remark" :rows="3" placeholder="请输入备注" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, SwapOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { quotationApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch } from '@/utils/batchOperations'
import type { Dayjs } from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '报价单号', dataIndex: 'quotationNo', key: 'quotationNo', width: 160, sortable: true },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '报价日期', dataIndex: 'quotationDate', key: 'quotationDate', width: 110, type: 'date' as const },
  { title: '有效期', dataIndex: 'validDate', key: 'validDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const, type: 'action' as const }
]
const filterFields = [
  { key: 'quotationNo', label: '报价单号', type: 'input' as const, placeholder: '输入报价单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '已发送', value: 1 }, { label: '已接受', value: 2 }, { label: '已拒绝', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]
const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '已发送', 2: '已接受', 3: '已拒绝' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单 ──
const formVisible = ref(false)
const formSubmitting = ref(false)
const isEdit = ref(false)
const editRecordId = ref<number | null>(null)
const formRef = ref()

interface QuotationItem { key: number; productName: string; quantity: number; unitPrice: number; discount: number }
const defaultItem = (): QuotationItem => ({ key: Date.now() + Math.random(), productName: '', quantity: 1, unitPrice: 0, discount: 0 })
const formState = reactive({ customerName: '', quotationDate: undefined as Dayjs | undefined, validUntil: undefined as Dayjs | undefined, items: [defaultItem()], remark: '' })
const formRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  quotationDate: [{ required: true, message: '请选择报价日期', trigger: 'change', type: 'object' as const }],
  validUntil: [{ required: true, message: '请选择有效期', trigger: 'change', type: 'object' as const }]
}
const itemColumns = [
  { title: '产品名称', key: 'productName', dataIndex: 'productName' },
  { title: '数量', key: 'quantity', dataIndex: 'quantity', width: 80 },
  { title: '单价', key: 'unitPrice', dataIndex: 'unitPrice', width: 120 },
  { title: '折扣(%)', key: 'discount', dataIndex: 'discount', width: 100 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]
const addItem = () => { formState.items.push(defaultItem()) }
const removeItem = (index: number) => { if (formState.items.length > 1) formState.items.splice(index, 1) }
const resetForm = () => {
  formState.customerName = ''; formState.quotationDate = undefined; formState.validUntil = undefined
  formState.items = [defaultItem()]; formState.remark = ''; editRecordId.value = null; isEdit.value = false; formRef.value?.clearValidate()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await quotationApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
  } catch { message.error('获取报价单列表失败') }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() { resetForm(); formVisible.value = true }
function handleEdit(record: any) {
  resetForm(); isEdit.value = true; editRecordId.value = record.id
  formState.customerName = record.customerName || ''; formState.remark = record.remark || ''
  formState.items = record.items?.length
    ? record.items.map((item: any, idx: number) => ({ key: idx, productName: item.productName || '', quantity: item.quantity || 1, unitPrice: item.unitPrice || 0, discount: item.discount || 0 }))
    : [defaultItem()]
  formVisible.value = true
}

async function handleDelete(record: any) {
  try { await quotationApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}
async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => quotationApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    if (isEdit.value && editRecordId.value) {
      await quotationApi.update(editRecordId.value, { ...formState })
      message.success('编辑成功')
    } else {
      await quotationApi.create({ ...formState })
      message.success('创建成功')
    }
    formVisible.value = false; fetchData()
  } catch { message.error(isEdit.value ? '编辑失败' : '创建失败') }
  finally { formSubmitting.value = false }
}

function handleSend(record: any) {
  Modal.confirm({
    title: '发送报价单', content: `发送报价单 "${record.quotationNo}" 给客户？`, okText: '确认发送', centered: true,
    async onOk() { try { await quotationApi.send(record.id); message.success('发送成功'); fetchData() } catch { message.error('发送失败') } }
  })
}

function handleConvert(record: any) {
  Modal.confirm({
    title: '转销售订单', content: `将报价 "${record.quotationNo}" 转为销售订单？`, okText: '确认转换', centered: true,
    async onOk() {
      try { await quotationApi.convertToOrder(record.id); message.success('转订单成功'); router.push(`/sale/order/new`) }
      catch { message.error('转换失败') }
    }
  })
}

function handleExport() {
  const headers = ['报价单号', '客户', '报价日期', '有效期', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.quotationNo || '', row.customerName || '', row.quotationDate || '', row.validDate || '',
    getStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '报价单')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
