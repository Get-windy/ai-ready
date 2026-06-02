<template>
  <div class="invoice-management">
    <a-tabs v-model:activeKey="activeTab" style="margin-bottom: 0">
      <a-tab-pane key="all" tab="全部发票" />
      <a-tab-pane key="sales" tab="销售发票" />
      <a-tab-pane key="purchase" tab="采购发票" />
    </a-tabs>

    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'crm-invoice-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      add-text="新建发票"
      @add="handleAdd"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <a-button @click="handleExport"><template #icon><ExportOutlined /></template>导出</a-button>
      </template>

      <template #invoiceNo="{ record }">
        <a @click="handleView(record)">{{ record.invoiceNo }}</a>
      </template>
      <template #invoiceType="{ record }">
        <a-tag :color="getInvoiceTypeColor(record.invoiceType)">{{ record.invoiceTypeLabel }}</a-tag>
      </template>
      <template #amount="{ record }">
        <span class="amount">¥{{ formatAmount(record.amount) }}</span>
      </template>
      <template #totalAmount="{ record }">
        <span class="amount total">¥{{ formatAmount(record.totalAmount) }}</span>
      </template>
      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="开具"><a-button type="link" size="small" @click="handleIssue(record)"><template #icon><FileProtectOutlined /></template></a-button></a-tooltip>
          <a-tooltip v-if="record.status === 'issued'" title="发送"><a-button type="link" size="small" @click="handleSend(record)"><template #icon><SendOutlined /></template></a-button></a-tooltip>
          <PrintButton v-if="record.status === 'issued'" templateType="invoice" :businessId="record.id" businessType="invoice" buttonText="" buttonSize="small" @print-success="handlePrintSuccess(record)" @print-error="handlePrintError" />
          <a-tooltip v-if="record.status === 'issued'" title="作废"><a-button type="link" danger size="small" @click="handleCancelConfirm(record)"><template #icon><DeleteOutlined /></template></a-button></a-tooltip>
        </a-space>
      </template>
    </TableList>

    <a-modal v-model:open="modalVisible" :title="modalTitle" width="700px" :confirm-loading="submitLoading" @ok="handleSubmit" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="发票号码" name="invoiceNo"><a-input v-model:value="formData.invoiceNo" placeholder="请输入发票号码" /></a-form-item>
        <a-form-item label="发票类型" name="invoiceType">
          <a-select v-model:value="formData.invoiceType" placeholder="请选择发票类型">
            <a-select-option value="special">增值税专用发票</a-select-option>
            <a-select-option value="normal">增值税普通发票</a-select-option>
            <a-select-option value="electronic">电子发票</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开票日期" name="invoiceDate"><a-date-picker v-model:value="formData.invoiceDate" style="width:100%" /></a-form-item>
        <a-form-item label="客户名称" name="customerId">
          <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
            <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发票金额" name="amount"><a-input-number v-model:value="formData.amount" :min="0" :precision="2" style="width:100%" /></a-form-item>
        <a-form-item label="税率" name="taxRate">
          <a-select v-model:value="formData.taxRate" placeholder="请选择税率">
            <a-select-option value="13">13%</a-select-option>
            <a-select-option value="9">9%</a-select-option>
            <a-select-option value="6">6%</a-select-option>
            <a-select-option value="3">3%</a-select-option>
            <a-select-option value="0">0%</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="税额"><a-input-number :value="taxAmount" :precision="2" disabled style="width:100%" /></a-form-item>
        <a-form-item label="价税合计"><a-input-number :value="totalAmount" :precision="2" disabled style="width:100%" /></a-form-item>
        <a-form-item label="关联订单" name="relatedOrders">
          <a-select v-model:value="formData.relatedOrders" mode="multiple" placeholder="请选择关联订单">
            <a-select-option v-for="o in orderList" :key="o.id" :value="o.id">{{ o.orderNo }} - ¥{{ o.amount }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="detailVisible" title="发票详情" width="700px" :footer="null">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="发票号码">{{ invoiceDetail.invoiceNo }}</a-descriptions-item>
        <a-descriptions-item label="发票类型"><a-tag :color="getInvoiceTypeColor(invoiceDetail.invoiceType)">{{ invoiceDetail.invoiceTypeLabel }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ invoiceDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="开票日期">{{ invoiceDetail.invoiceDate }}</a-descriptions-item>
        <a-descriptions-item label="发票金额"><span class="amount">¥{{ formatAmount(invoiceDetail.amount) }}</span></a-descriptions-item>
        <a-descriptions-item label="税率">{{ invoiceDetail.taxRate }}%</a-descriptions-item>
        <a-descriptions-item label="税额">¥{{ formatAmount(invoiceDetail.taxAmount) }}</a-descriptions-item>
        <a-descriptions-item label="价税合计"><span class="amount total">¥{{ formatAmount(invoiceDetail.totalAmount) }}</span></a-descriptions-item>
        <a-descriptions-item label="发票状态"><a-tag :color="getStatusColor(invoiceDetail.status)">{{ getStatusText(invoiceDetail.status) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="开票人">{{ invoiceDetail.issuer }}</a-descriptions-item>
        <a-descriptions-item label="关联订单" :span="2"><a-space><a-tag v-for="o in invoiceDetail.relatedOrders" :key="o">{{ o }}</a-tag></a-space></a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ invoiceDetail.remark }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, FileProtectOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import type { FormInstance } from 'ant-design-vue'

const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建发票')
const activeTab = ref('all')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])

const columns = [
  { title: '发票号码', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 150, sortable: true, slotName: 'invoiceNo' },
  { title: '发票类型', dataIndex: 'invoiceType', key: 'invoiceType', width: 120, slotName: 'invoiceType' },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '开票日期', dataIndex: 'invoiceDate', key: 'invoiceDate', width: 100, type: 'date' as const },
  { title: '发票金额', dataIndex: 'amount', key: 'amount', width: 120, slotName: 'amount' },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100 },
  { title: '价税合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, slotName: 'totalAmount' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'invoiceNo', label: '发票号码', type: 'input' as const, placeholder: '输入发票号码' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'invoiceType', label: '发票类型', type: 'select' as const, options: [
    { label: '增值税专用发票', value: 'special' }, { label: '增值税普通发票', value: 'normal' }, { label: '电子发票', value: 'electronic' }
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' }, { label: '已开具', value: 'issued' }, { label: '已发送', value: 'sent' }, { label: '已收到', value: 'received' }, { label: '已作废', value: 'cancelled' }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<string, string> = { draft: 'default', issued: 'blue', sent: 'cyan', received: 'green', cancelled: 'red' }
const statusTextMap: Record<string, string> = { draft: '草稿', issued: '已开具', sent: '已发送', received: '已收到', cancelled: '已作废' }

const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  const totalAmt = tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  return [
    { label: '本页数量', value: tableData.value.length, type: 'default' as const },
    { label: '价税合计', value: `¥${totalAmt.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`, type: 'primary' as const }
  ]
})

function getStatusColor(status: string): string { return statusColorMap[status] || 'default' }
function getStatusText(status: string): string { return statusTextMap[status] || status }
function formatAmount(amount: number): string { return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

const invoiceTypeColorMap: Record<string, string> = { special: 'blue', normal: 'green', electronic: 'orange' }
function getInvoiceTypeColor(type: string): string { return invoiceTypeColorMap[type] || 'default' }

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

const formData = reactive({ id: undefined, invoiceNo: '', invoiceType: undefined, invoiceDate: undefined, customerId: undefined, amount: undefined, taxRate: '13', remark: '', relatedOrders: [], attachments: [] })
const formRules = { invoiceNo: [{ required: true, message: '请输入发票号码' }], invoiceType: [{ required: true, message: '请选择发票类型' }], invoiceDate: [{ required: true, message: '请选择开票日期' }], customerId: [{ required: true, message: '请选择客户' }], amount: [{ required: true, message: '请输入发票金额' }] }
const customerList = ref([{ id: 1, name: '北京科技有限公司' }, { id: 2, name: '上海贸易公司' }, { id: 3, name: '广州制造企业' }])
const orderList = ref([{ id: 1, orderNo: 'SO20240115001', amount: '58,000' }, { id: 2, orderNo: 'SO20240115002', amount: '32,500' }, { id: 3, orderNo: 'SO20240114003', amount: '128,000' }])
const invoiceDetail = ref<any>({})

const taxAmount = computed(() => { const amt = formData.amount || 0; const rate = parseFloat(formData.taxRate) / 100; return amt * rate })
const totalAmount = computed(() => { const amt = formData.amount || 0; return amt + taxAmount.value })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    await new Promise(r => setTimeout(r, 300))
    tableData.value = [
      { id: 1, invoiceNo: 'INV20240115001', invoiceType: 'special', invoiceTypeLabel: '增值税专用发票', customerName: '北京科技有限公司', invoiceDate: '2024-01-15', amount: 58000, taxAmount: 7540, totalAmount: 65540, status: 'issued', issuer: '张三' },
      { id: 2, invoiceNo: 'INV20240115002', invoiceType: 'normal', invoiceTypeLabel: '增值税普通发票', customerName: '上海贸易公司', invoiceDate: '2024-01-15', amount: 32500, taxAmount: 4225, totalAmount: 36725, status: 'sent', issuer: '李四' },
      { id: 3, invoiceNo: 'INV20240114003', invoiceType: 'electronic', invoiceTypeLabel: '电子发票', customerName: '广州制造企业', invoiceDate: '2024-01-14', amount: 128000, taxAmount: 16640, totalAmount: 144640, status: 'draft', issuer: '王五' },
      { id: 4, invoiceNo: 'INV20240120004', invoiceType: 'special', invoiceTypeLabel: '增值税专用发票', customerName: '深圳科技公司', invoiceDate: '2024-01-20', amount: 85000, taxAmount: 11050, totalAmount: 96050, status: 'received', issuer: '张三' },
      { id: 5, invoiceNo: 'INV20240122005', invoiceType: 'electronic', invoiceTypeLabel: '电子发票', customerName: '杭州互联网公司', invoiceDate: '2024-01-22', amount: 45000, taxAmount: 2700, totalAmount: 47700, status: 'cancelled', issuer: '李四' }
    ]
    pagination.total = 5
  } catch { message.error('获取数据失败') }
  finally { loading.value = false }
}

function handleView(record: any) { invoiceDetail.value = { ...record, relatedOrders: ['SO20240115001', 'SO20240115002'], remark: record.remark || '' }; detailVisible.value = true }
function handleEdit(record: any) { modalTitle.value = '编辑发票'; Object.assign(formData, record); modalVisible.value = true }
function handleAdd() { modalTitle.value = '新建发票'; modalVisible.value = true }
function handleIssue(record: any) { message.success('发票已开具'); fetchData() }
function handleSend(record: any) { message.success('发票已发送'); fetchData() }
function handlePrintSuccess(record: any) { message.success(`发票 ${record.invoiceNo} 打印成功`) }
function handlePrintError(error: any) { message.error(`打印失败: ${error.message || '未知错误'}`) }
function handleCancelConfirm(record: any) { Modal.confirm({ title: '确认作废', content: `确定要作废发票 "${record.invoiceNo}" 吗？`, okText: '确认作废', okType: 'danger', cancelText: '取消', centered: true, async onOk() { message.success('发票已作废'); fetchData() } }) }

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try { message.success('保存成功'); modalVisible.value = false; fetchData() }
  finally { submitLoading.value = false }
}
function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['发票号码', '发票类型', '客户名称', '开票日期', '发票金额', '税额', '价税合计', '状态', '开票人']
    const rows = tableData.value.map((row: any) => [row.invoiceNo, row.invoiceTypeLabel, row.customerName, row.invoiceDate, row.amount, row.taxAmount, row.totalAmount, getStatusText(row.status), row.issuer])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = `发票_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(link); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
</script>

<style scoped lang="scss">
.invoice-management { :deep(.ant-tabs) { margin: 0 24px; } }
.amount { color: #f5222d; font-weight: 500; &.total { font-weight: 600; } }
</style>
