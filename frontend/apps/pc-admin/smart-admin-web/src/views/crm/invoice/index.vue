<template>
  <div class="invoice-management">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>发票管理</h2>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建发票
        </a-button>
      </div>

      <a-form layout="inline" class="search-form">
        <a-form-item label="发票号码">
          <a-input v-model:value="searchForm.invoiceNo" placeholder="请输入发票号码" allow-clear />
        </a-form-item>
        <a-form-item label="客户名称">
          <a-input v-model:value="searchForm.customerName" placeholder="请输入客户名称" allow-clear />
        </a-form-item>
        <a-form-item label="发票类型">
          <a-select v-model:value="searchForm.invoiceType" placeholder="请选择发票类型" allow-clear style="width: 150px">
            <a-select-option value="special">增值税专用发票</a-select-option>
            <a-select-option value="normal">增值税普通发票</a-select-option>
            <a-select-option value="electronic">电子发票</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发票状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="issued">已开具</a-select-option>
            <a-select-option value="sent">已发送</a-select-option>
            <a-select-option value="received">已收到</a-select-option>
            <a-select-option value="cancelled">已作废</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开票日期">
          <a-range-picker v-model:value="searchForm.dateRange" style="width: 200px" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="all" tab="全部发票" />
        <a-tab-pane key="sales" tab="销售发票" />
        <a-tab-pane key="purchase" tab="采购发票" />
      </a-tabs>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'invoiceNo'">
            <a @click="handleView(record)">{{ record.invoiceNo }}</a>
          </template>
          <template v-if="column.key === 'invoiceType'">
            <a-tag :color="getInvoiceTypeColor(record.invoiceType)">{{ record.invoiceTypeLabel }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'amount'">
            <span class="amount">¥{{ formatAmount(record.amount) }}</span>
          </template>
          <template v-if="column.key === 'totalAmount'">
            <span class="amount total">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)" v-if="record.status === 'draft'">编辑</a>
              <a @click="handleIssue(record)" v-if="record.status === 'draft'">开具</a>
              <a @click="handleSend(record)" v-if="record.status === 'issued'">发送</a>
              <PrintButton 
                v-if="record.status === 'issued'"
                templateType="invoice"
                :businessId="record.id"
                businessType="invoice"
                buttonText="打印"
                buttonSize="small"
                @print-success="handlePrintSuccess(record)"
                @print-error="handlePrintError"
              />
              <a-popconfirm title="确定要作废吗？" @confirm="handleCancel(record)">
                <a class="danger-link" v-if="record.status === 'issued'">作废</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="700px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="发票号码" name="invoiceNo">
          <a-input v-model:value="formData.invoiceNo" placeholder="请输入发票号码" />
        </a-form-item>
        <a-form-item label="发票类型" name="invoiceType">
          <a-select v-model:value="formData.invoiceType" placeholder="请选择发票类型">
            <a-select-option value="special">增值税专用发票</a-select-option>
            <a-select-option value="normal">增值税普通发票</a-select-option>
            <a-select-option value="electronic">电子发票</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开票日期" name="invoiceDate">
          <a-date-picker v-model:value="formData.invoiceDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="客户名称" name="customerId">
          <a-select
            v-model:value="formData.customerId"
            placeholder="请选择客户"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="customer in customerList" :key="customer.id" :value="customer.id">
              {{ customer.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="发票金额" name="amount">
          <a-input-number v-model:value="formData.amount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="税率" name="taxRate">
          <a-select v-model:value="formData.taxRate" placeholder="请选择税率">
            <a-select-option value="13">13%</a-select-option>
            <a-select-option value="9">9%</a-select-option>
            <a-select-option value="6">6%</a-select-option>
            <a-select-option value="3">3%</a-select-option>
            <a-select-option value="0">0%</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="税额">
          <a-input-number :value="calculateTaxAmount()" :precision="2" disabled style="width: 100%" />
        </a-form-item>
        <a-form-item label="价税合计">
          <a-input-number :value="calculateTotalAmount()" :precision="2" disabled style="width: 100%" />
        </a-form-item>
        <a-form-item label="关联订单" name="relatedOrders">
          <a-select
            v-model:value="formData.relatedOrders"
            mode="multiple"
            placeholder="请选择关联订单"
          >
            <a-select-option v-for="order in orderList" :key="order.id" :value="order.id">
              {{ order.orderNo }} - ¥{{ order.amount }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
        <a-form-item label="附件" name="attachments">
          <a-upload
            v-model:file-list="formData.attachments"
            :action="uploadUrl"
            list-type="picture-card"
          >
            <div>
              <PlusOutlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="detailVisible"
      title="发票详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="发票号码">{{ invoiceDetail.invoiceNo }}</a-descriptions-item>
        <a-descriptions-item label="发票类型">
          <a-tag :color="getInvoiceTypeColor(invoiceDetail.invoiceType)">{{ invoiceDetail.invoiceTypeLabel }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ invoiceDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="开票日期">{{ invoiceDetail.invoiceDate }}</a-descriptions-item>
        <a-descriptions-item label="发票金额">¥{{ formatAmount(invoiceDetail.amount) }}</a-descriptions-item>
        <a-descriptions-item label="税率">{{ invoiceDetail.taxRate }}%</a-descriptions-item>
        <a-descriptions-item label="税额">¥{{ formatAmount(invoiceDetail.taxAmount) }}</a-descriptions-item>
        <a-descriptions-item label="价税合计">
          <span class="amount total">¥{{ formatAmount(invoiceDetail.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="发票状态">
          <a-tag :color="getStatusColor(invoiceDetail.status)">{{ getStatusText(invoiceDetail.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="开票人">{{ invoiceDetail.issuer }}</a-descriptions-item>
        <a-descriptions-item label="关联订单" :span="2">
          <a-space>
            <a-tag v-for="order in invoiceDetail.relatedOrders" :key="order">{{ order }}</a-tag>
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ invoiceDetail.remark }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建发票')
const activeTab = ref('all')
const formRef = ref<FormInstance>()

const searchForm = reactive({
  invoiceNo: '',
  customerName: '',
  invoiceType: undefined,
  status: undefined,
  dateRange: []
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '发票号码', key: 'invoiceNo', dataIndex: 'invoiceNo', width: 150 },
  { title: '发票类型', key: 'invoiceType', dataIndex: 'invoiceType', width: 120 },
  { title: '客户名称', dataIndex: 'customerName', width: 150 },
  { title: '开票日期', dataIndex: 'invoiceDate', width: 100 },
  { title: '发票金额', key: 'amount', dataIndex: 'amount', width: 120 },
  { title: '税额', dataIndex: 'taxAmount', width: 100 },
  { title: '价税合计', key: 'totalAmount', dataIndex: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const tableData = ref<any[]>([])

const formData = reactive({
  id: undefined,
  invoiceNo: '',
  invoiceType: undefined,
  invoiceDate: undefined,
  customerId: undefined,
  amount: undefined,
  taxRate: '13',
  remark: '',
  relatedOrders: [],
  attachments: []
})

const formRules = {
  invoiceNo: [{ required: true, message: '请输入发票号码' }],
  invoiceType: [{ required: true, message: '请选择发票类型' }],
  invoiceDate: [{ required: true, message: '请选择开票日期' }],
  customerId: [{ required: true, message: '请选择客户' }],
  amount: [{ required: true, message: '请输入发票金额' }]
}

const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' }
])

const orderList = ref([
  { id: 1, orderNo: 'SO20240115001', amount: '58,000' },
  { id: 2, orderNo: 'SO20240115002', amount: '32,500' },
  { id: 3, orderNo: 'SO20240114003', amount: '128,000' }
])

const invoiceDetail = ref<any>({})

const uploadUrl = '/api/upload'

const calculateTaxAmount = computed(() => {
  const amount = formData.amount || 0
  const rate = parseFloat(formData.taxRate) / 100
  return amount * rate
})

const calculateTotalAmount = computed(() => {
  const amount = formData.amount || 0
  const tax = calculateTaxAmount.value
  return amount + tax
})

onMounted(() => {
  loadTableData()
})

const loadTableData = () => {
  loading.value = true
  tableData.value = [
    { id: 1, invoiceNo: 'INV20240115001', invoiceType: 'special', invoiceTypeLabel: '增值税专用发票', customerName: '北京科技有限公司', invoiceDate: '2024-01-15', amount: 58000, taxAmount: 7540, totalAmount: 65540, status: 'issued', issuer: '张三' },
    { id: 2, invoiceNo: 'INV20240115002', invoiceType: 'normal', invoiceTypeLabel: '增值税普通发票', customerName: '上海贸易公司', invoiceDate: '2024-01-15', amount: 32500, taxAmount: 4225, totalAmount: 36725, status: 'sent', issuer: '李四' },
    { id: 3, invoiceNo: 'INV20240114003', invoiceType: 'electronic', invoiceTypeLabel: '电子发票', customerName: '广州制造企业', invoiceDate: '2024-01-14', amount: 128000, taxAmount: 16640, totalAmount: 144640, status: 'draft', issuer: '王五' }
  ]
  pagination.total = 3
  loading.value = false
}

const getInvoiceTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    special: 'blue',
    normal: 'green',
    electronic: 'orange'
  }
  return colors[type] || 'default'
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: 'default',
    issued: 'blue',
    sent: 'cyan',
    received: 'green',
    cancelled: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    draft: '草稿',
    issued: '已开具',
    sent: '已发送',
    received: '已收到',
    cancelled: '已作废'
  }
  return texts[status] || status
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const filterOption = (input: string, option: any) => {
  return option.name.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const handleSearch = () => {
  pagination.current = 1
  loadTableData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    invoiceNo: '',
    customerName: '',
    invoiceType: undefined,
    status: undefined,
    dateRange: []
  })
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTableData()
}

const handleAdd = () => {
  modalTitle.value = '新建发票'
  modalVisible.value = true
}

const handleView = (record: any) => {
  invoiceDetail.value = {
    ...record,
    relatedOrders: ['SO20240115001', 'SO20240115002'],
    remark: ''
  }
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  modalTitle.value = '编辑发票'
  Object.assign(formData, record)
  modalVisible.value = true
}

const handleIssue = (record: any) => {
  message.success('发票已开具')
  loadTableData()
}

const handleSend = (record: any) => {
  message.success('发票已发送')
  loadTableData()
}

const handlePrintSuccess = (record: any) => {
  message.success(`发票 ${record.invoiceNo} 打印成功`)
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

const handleCancel = (record: any) => {
  message.success('发票已作废')
  loadTableData()
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    message.success('保存成功')
    modalVisible.value = false
    loadTableData()
  } catch (error) {
    console.error('Validation failed:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleModalCancel = () => {
  formRef.value?.resetFields()
  modalVisible.value = false
}
</script>

<style scoped lang="scss">
.invoice-management {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
  }
}

.search-form {
  margin-bottom: 16px;
}

.amount {
  color: #f5222d;
  font-weight: 500;

  &.total {
    font-weight: 600;
  }
}

.danger-link {
  color: #f5222d;
}
</style>