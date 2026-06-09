<template>
  <PageContainer title="发票管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <FileTextOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">发票总数</div>
                <div class="stat-value">{{ pagination.total }}</div>
                <div class="stat-desc">全部发票</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <SendOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">已开具</div>
                <div class="stat-value">{{ statusCounts.issued }}</div>
                <div class="stat-desc">已开具发票</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <CheckCircleOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">已收到</div>
                <div class="stat-value">{{ statusCounts.received }}</div>
                <div class="stat-desc positive">已确认收票</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <DollarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">金额合计</div>
                <div class="stat-value">¥{{ formatAmount(tableTotalAmount) }}</div>
                <div class="stat-desc">本页合计</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <a-tabs v-model:activeKey="activeTab" style="margin-bottom: 0">
        <a-tab-pane key="all" tab="全部发票" />
        <a-tab-pane key="sales" tab="销售发票" />
        <a-tab-pane key="purchase" tab="采购发票" />
      </a-tabs>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :filter-fields="filterFields"
        :show-summary="true"
        :summary-data="summaryData"
        :show-export="true"
        :selectable="true"
        add-text="新建发票"
        @add="handleAdd"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @sort-change="handleSortChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @export="handleExport"
      >
      <template #toolbar-actions>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的发票，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无发票数据，点击右上角「新建发票」开始创建
            </p>
          </div>
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
      </VxeTableList>
    </ErrorBoundary>

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
        <a-form-item label="税额"><a-input-number :value="formTaxAmount" :precision="2" disabled style="width:100%" /></a-form-item>
        <a-form-item label="价税合计"><a-input-number :value="formTotalAmount" :precision="2" disabled style="width:100%" /></a-form-item>
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
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, FileProtectOutlined, ReloadOutlined, SearchOutlined, InboxOutlined, FileTextOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import type { FormInstance } from 'ant-design-vue'
import { invoiceApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建发票')
const activeTab = ref('all')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
const selectedRowKeys = ref<number[]>([])
let autoRefreshTimer: number | null = null

// 状态统计
const statusCounts = computed(() => {
  const issued = tableData.value.filter(r => r.status === 'issued' || r.status === 'sent').length
  const received = tableData.value.filter(r => r.status === 'received').length
  return { issued, received }
})

const tableTotalAmount = computed(() => {
  return tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'invoiceNo', title: '发票号码', width: 150, sortable: true, formatter: ({ row }: any) => row.invoiceNo || '' },
  { field: 'invoiceType', title: '发票类型', width: 120, formatter: ({ row }: any) => row.invoiceTypeLabel || '' },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'invoiceDate', title: '开票日期', width: 100 },
  { field: 'amount', title: '发票金额', width: 120, align: 'right', formatter: ({ cellValue }: any) => `¥${formatAmount(cellValue)}` },
  { field: 'taxAmount', title: '税额', width: 100, align: 'right', formatter: ({ cellValue }: any) => `¥${formatAmount(cellValue)}` },
  { field: 'totalAmount', title: '价税合计', width: 120, align: 'right', formatter: ({ cellValue }: any) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }: any) => getStatusText(cellValue) },
  { field: 'action', title: '操作', width: 160, fixed: 'right', type: 'action' }
])

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

const formTaxAmount = computed(() => { const amt = formData.amount || 0; const rate = parseFloat(formData.taxRate) / 100; return amt * rate })
const formTotalAmount = computed(() => { const amt = formData.amount || 0; return amt + formTaxAmount.value })

// 自动刷新
const startAutoRefresh = () => {
  autoRefreshTimer = window.setInterval(() => {
    if (!loading.value && !modalVisible.value) {
      fetchData(true)
    }
  }, 60000)
}

const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

onMounted(() => {
  fetchData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchFilters.keyword) params.keyword = searchFilters.keyword
    if (searchFilters.status) params.status = searchFilters.status
    const res = await invoiceApi.page(params)
    const result = res as any
    tableData.value = (result.content || result.records || result.data?.records || []) as any[]
    pagination.total = result.totalElements ?? result.total ?? result.data?.total ?? 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    if (!silent) {
      hasError.value = true
      message.error('获取发票数据失败')
    }
    tableData.value = mockData()
    pagination.total = mockData().length
  }
  finally { if (!silent) loading.value = false }
}

const mockData = () => [
  { id: 1, invoiceNo: 'INV2024010001', invoiceType: 'special', invoiceTypeLabel: '增值税专用发票', customerName: '北京科技有限公司', invoiceDate: '2024-01-10', amount: 50000, taxAmount: 6500, totalAmount: 56500, status: 'issued', issuer: '张三' },
  { id: 2, invoiceNo: 'INV2024010002', invoiceType: 'normal', invoiceTypeLabel: '增值税普通发票', customerName: '上海贸易公司', invoiceDate: '2024-01-12', amount: 28000, taxAmount: 2800, totalAmount: 30800, status: 'sent', issuer: '李四' },
  { id: 3, invoiceNo: 'INV2024010003', invoiceType: 'electronic', invoiceTypeLabel: '电子发票', customerName: '广州制造企业', invoiceDate: '2024-01-15', amount: 15000, taxAmount: 900, totalAmount: 15900, status: 'received', issuer: '王五' },
  { id: 4, invoiceNo: 'INV2024010004', invoiceType: 'special', invoiceTypeLabel: '增值税专用发票', customerName: '深圳电子公司', invoiceDate: '2024-01-08', amount: 42000, taxAmount: 5460, totalAmount: 47460, status: 'draft', issuer: '赵六' }
]

const handleRefresh = () => {
  lastUpdateTime.value = ''
  fetchData()
}

function handleResetFilters() {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

function handleView(record: any) { invoiceDetail.value = { ...record, relatedOrders: ['SO20240115001', 'SO20240115002'], remark: record.remark || '' }; detailVisible.value = true }
function handleEdit(record: any) { modalTitle.value = '编辑发票'; Object.assign(formData, record); modalVisible.value = true }
function handleAdd() { modalTitle.value = '新建发票'; modalVisible.value = true }
async function handleIssue(record: any) {
  try { await invoiceApi.updateStatus(record.id, 'ISSUED'); message.success('发票已开具'); fetchData() }
  catch { message.error('开票失败') }
}
async function handleSend(record: any) {
  try { await invoiceApi.sendInvoice(record.id, 'EMAIL'); message.success('发票已发送'); fetchData() }
  catch { message.error('发送失败') }
}
function handlePrintSuccess(record: any) { message.success(`发票 ${record.invoiceNo} 打印成功`) }
function handlePrintError(error: any) { message.error(`打印失败: ${error.message || '未知错误'}`) }
function handleCancelConfirm(record: any) {
  Modal.confirm({ title: '确认作废', content: `确定要作废发票 "${record.invoiceNo}" 吗？`, okText: '确认作废', okType: 'danger', cancelText: '取消', centered: true, async onOk() {
    try { await invoiceApi.voidInvoice(record.id, '作废'); message.success('发票已作废'); fetchData() }
    catch { message.error('作废失败') }
  }})
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try {
    await invoiceApi.create(formData)
    message.success('保存成功'); modalVisible.value = false; fetchData()
  } catch { message.error('保存失败') }
  finally { submitLoading.value = false }
}
function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const headers = ['发票号码', '发票类型', '客户名称', '开票日期', '发票金额', '税额', '价税合计', '状态', '开票人']
  const rows = tableData.value.map((row: any) => [row.invoiceNo, row.invoiceTypeLabel, row.customerName, row.invoiceDate, row.amount, row.taxAmount, row.totalAmount, getStatusText(row.status), row.issuer])
  exportCsv(headers, rows, '发票')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
function handleSelectionChange(rows: any[], ids: any[]) { selectedRowKeys.value = ids }
</script>

<style scoped>
.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

.stats-cards {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-card.stat-card-blue {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border: 1px solid #91d5ff;
}

.stat-card.stat-card-green {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
}

.stat-card.stat-card-orange {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}

.stat-card.stat-card-purple {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.stat-desc.positive {
  color: #52c41a;
}

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


.invoice-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.total {
  font-weight: 600;
}





:deep(.ant-tabs) {
  margin: 0 24px;
}
</style>
