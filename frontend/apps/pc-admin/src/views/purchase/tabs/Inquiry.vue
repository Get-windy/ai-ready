<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'purchase-inquiry-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建询价"
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
<template #batch-actions>
      <a-button size="small" @click="handleBatchSend">批量发送</a-button>
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
        <a-popconfirm
          v-if="record.status === 0"
          title="确定删除该询价单？"
          @confirm="handleDelete(record)"
        >
          <a-tooltip title="删除">
            <a-button type="link" size="small" danger>
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-popconfirm>
      </a-space>
    </template>
  </TableList>

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="询价单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="询价单号">{{ currentRecord.inquiryNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="询价日期">{{ currentRecord.inquiryDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建/编辑询价弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    :title="formMode === 'edit' ? '编辑询价单' : '新建询价单'"
    width="750px"
    centered
    :confirm-loading="formSubmitting"
    ok-text="确认"
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
      <a-form-item label="供应商" name="supplierId">
        <a-select
          v-model:value="formData.supplierId"
          placeholder="请选择供应商"
          show-search
          @change="handleSupplierChange"
        >
          <a-select-option v-for="s in supplierList" :key="s.id" :value="s.id">
            {{ s.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="询价日期" name="inquiryDate">
        <a-date-picker v-model:value="formData.inquiryDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>
    </a-form>
    <div style="margin-top: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <span style="font-weight: 500;">询价物料明细</span>
        <a-button size="small" type="dashed" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </div>
      <a-table
        :columns="itemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        row-key="tempKey"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template v-else-if="column.key === 'specification'">
            <a-input v-model:value="record.specification" placeholder="规格" size="small" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" placeholder="数量" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unit'">
            <a-input v-model:value="record.unit" placeholder="单位" size="small" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="handleRemoveItem(index)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import TableList from '@/components/TableList/TableList.vue'
import { inquiryApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch, validateSelection } from '@/utils/batchOperations'
import optionsApi from '@/api/options'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ── 表格列定义 ──────────────────────────────────────────
const columns = [
  { title: '询价单号', dataIndex: 'inquiryNo', key: 'inquiryNo', width: 160, sortable: true },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '询价日期', dataIndex: 'inquiryDate', key: 'inquiryDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const, type: 'action' as const }
]

// ── 筛选字段 ────────────────────────────────────────────
const filterFields = [
  { key: 'inquiryNo', label: '询价单号', type: 'input' as const, placeholder: '输入询价单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '已发送', value: 1 }, { label: '已报价', value: 2 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '已发送', 2: '已报价' }

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单弹窗状态 ──────────────────────────────────────
const formModalVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const supplierList = ref<any[]>([])

interface InquiryItem {
  tempKey: string
  productName: string
  specification: string
  quantity: number
  unit: string
}

const formData = reactive({
  supplierId: undefined as number | undefined,
  inquiryDate: undefined as any,
  remark: '',
  items: [] as InquiryItem[]
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  inquiryDate: [{ required: true, message: '请选择询价日期', trigger: 'change' }]
}

const itemColumns = [
  { title: '物料名称', key: 'productName', width: 150 },
  { title: '规格', key: 'specification', width: 120 },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '单位', key: 'unit', width: 80 },
  { title: '操作', key: 'action', width: 80 }
]

let itemCounter = 0
const genTempKey = () => `item_${++itemCounter}_${Date.now()}`

const handleAddItem = () => {
  formData.items.push({
    tempKey: genTempKey(),
    productName: '',
    specification: '',
    quantity: 1,
    unit: '个'
  })
}

const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleSupplierChange = (val: number) => {
  formData.supplierId = val
}

const resetForm = () => {
  formData.supplierId = undefined
  formData.inquiryDate = undefined
  formData.remark = ''
  formData.items = []
  editingId.value = null
}

// ── 动态加载供应商列表 ──────────────────────────────────
async function loadSuppliers() {
  try {
    const res = await optionsApi.getSuppliers()
    supplierList.value = Array.isArray(res) ? res : []
  } catch {
    supplierList.value = [
      { id: 1, name: '供应商A' },
      { id: 2, name: '供应商B' },
      { id: 3, name: '供应商C' }
    ]
  }
}

// ── 数据请求 ────────────────────────────────────────────
async function fetchData() {
  loading.value = true
  try {
    const res = await inquiryApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch {
    message.error('获取询价单列表失败')
  } finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }

function handleAdd() {
  formMode.value = 'add'
  resetForm()
  loadSuppliers()
  formModalVisible.value = true
}

function handleEdit(record: any) {
  formMode.value = 'edit'
  editingId.value = record.id
  formData.supplierId = record.supplierId
  formData.inquiryDate = record.inquiryDate
  formData.remark = record.remark || ''
  formData.items = (record.items || []).map((item: any) => ({
    tempKey: genTempKey(),
    productName: item.productName || '',
    specification: item.specification || '',
    quantity: item.quantity || 1,
    unit: item.unit || '个'
  }))
  loadSuppliers()
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  try { await inquiryApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => inquiryApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

// ── 表单提交 ──────────────────────────────────────────
const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    const data = {
      supplierId: formData.supplierId,
      inquiryDate: formData.inquiryDate,
      remark: formData.remark,
      items: formData.items.map(item => ({
        productName: item.productName,
        specification: item.specification,
        quantity: item.quantity,
        unit: item.unit
      }))
    }
    if (formMode.value === 'edit' && editingId.value) {
      await inquiryApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await inquiryApi.create(data)
      message.success('新建询价单成功')
    }
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error(formMode.value === 'edit' ? '编辑失败' : '新建失败')
  } finally {
    formSubmitting.value = false
  }
}

// ── 发送询价 ──────────────────────────────────────────
function handleSend(record: any) {
  Modal.confirm({
    title: '发送询价单',
    content: `确定发送询价单 "${record.inquiryNo}" 吗？`,
    okText: '确认发送', cancelText: '取消', centered: true,
    async onOk() {
      try { await inquiryApi.send(record.id); message.success('发送成功'); fetchData() }
      catch { message.error('发送失败') }
    }
  })
}

// ── 导出 ──────────────────────────────────────────────
function handleExport() {
  const headers = ['询价单号', '供应商', '询价日期', '状态', '创建时间']
  const rows = dataSource.value.map(row => [
    row.inquiryNo || '', row.supplierName || '', row.inquiryDate || '',
    getStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '询价单')
}

// ── 批量发送 ──────────────────────────────────────────
function handleBatchSend() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (!validateSelection(keys, '发送')) return
  Modal.confirm({
    title: '批量发送', content: `发送选中的 ${keys.length} 个询价单？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => inquiryApi.send(id), '批量发送')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => { fetchData(); loadSuppliers() })
</script>
