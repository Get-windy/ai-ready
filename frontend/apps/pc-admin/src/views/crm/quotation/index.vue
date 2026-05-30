<template>
  <div class="quotation-management">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>报价管理</h2>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建报价
        </a-button>
      </div>

      <a-form layout="inline" class="search-form">
        <a-form-item label="报价单号">
          <a-input v-model:value="searchForm.quotationNo" placeholder="请输入报价单号" allow-clear />
        </a-form-item>
        <a-form-item label="报价名称">
          <a-input v-model:value="searchForm.quotationName" placeholder="请输入报价名称" allow-clear />
        </a-form-item>
        <a-form-item label="客户名称">
          <a-input v-model:value="searchForm.customerName" placeholder="请输入客户名称" allow-clear />
        </a-form-item>
        <a-form-item label="报价状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="sent">已发送</a-select-option>
            <a-select-option value="accepted">已接受</a-select-option>
            <a-select-option value="rejected">已拒绝</a-select-option>
            <a-select-option value="expired">已过期</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quotationNo'">
            <a @click="handleView(record)">{{ record.quotationNo }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'totalAmount'">
            <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)" v-if="record.status === 'draft'">编辑</a>
              <a @click="handleSend(record)" v-if="record.status === 'draft'">发送</a>
              <a @click="handleConvert(record)" v-if="record.status === 'accepted'">转订单</a>
              <a @click="handleCopy(record)">复制</a>
              <a @click="handleDeleteConfirm(record)" class="danger-link" v-if="record.status === 'draft'">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="900px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="报价单号" name="quotationNo" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.quotationNo" placeholder="自动生成" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="报价日期" name="quotationDate" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-date-picker v-model:value="formData.quotationDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="报价名称" name="quotationName" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.quotationName" placeholder="请输入报价名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期(天)" name="validDays" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input-number v-model:value="formData.validDays" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="客户名称" name="customerId" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
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
          </a-col>
          <a-col :span="12">
            <a-form-item label="币种" name="currency" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select v-model:value="formData.currency" placeholder="请选择币种">
                <a-select-option value="CNY">人民币(CNY)</a-select-option>
                <a-select-option value="USD">美元(USD)</a-select-option>
                <a-select-option value="EUR">欧元(EUR)</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="联系人" name="contactPerson" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.contactPerson" placeholder="请输入联系人" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="联系电话" name="contactPhone" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.contactPhone" placeholder="请输入联系电话" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>报价明细</a-divider>

        <a-table
          :columns="itemColumns"
          :data-source="formData.items"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'">
              <a-input v-model:value="record.productName" placeholder="产品名称" />
            </template>
            <template v-if="column.key === 'spec'">
              <a-input v-model:value="record.spec" placeholder="规格型号" />
            </template>
            <template v-if="column.key === 'quantity'">
              <a-input-number v-model:value="record.quantity" :min="1" style="width: 80px" />
            </template>
            <template v-if="column.key === 'unit'">
              <a-input v-model:value="record.unit" placeholder="单位" style="width: 60px" />
            </template>
            <template v-if="column.key === 'price'">
              <a-input-number v-model:value="record.price" :min="0" :precision="2" style="width: 100px" />
            </template>
            <template v-if="column.key === 'discount'">
              <a-input-number v-model:value="record.discount" :min="0" :max="100" style="width: 80px" />
            </template>
            <template v-if="column.key === 'subtotal'">
              <span class="amount">¥{{ calculateItemSubtotal(record) }}</span>
            </template>
            <template v-if="column.key === 'action'">
              <a @click="removeItem(index)" v-if="formData.items.length > 1">删除</a>
            </template>
          </template>
        </a-table>
        <a-button type="dashed" block @click="addItem" style="margin-top: 16px">
          <template #icon><PlusOutlined /></template>
          添加产品
        </a-button>

        <a-divider>费用汇总</a-divider>

        <a-row :gutter="24">
          <a-col :span="8">
            <a-statistic title="产品金额" :value="calculateTotalAmount()" :precision="2" prefix="¥" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="折扣金额" :value="calculateDiscountAmount()" :precision="2" prefix="¥" />
          </a-col>
          <a-col :span="8">
            <a-statistic title="报价总额" :value="calculateGrandTotal()" :precision="2" prefix="¥" :value-style="{ color: '#f5222d' }" />
          </a-col>
        </a-row>

        <a-form-item label="报价条款" name="terms" style="margin-top: 16px">
          <a-textarea v-model:value="formData.terms" placeholder="请输入报价条款（如付款方式、交货期等）" :rows="4" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="detailVisible"
      title="报价详情"
      width="900px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="报价单号">{{ quotationDetail.quotationNo }}</a-descriptions-item>
        <a-descriptions-item label="报价名称">{{ quotationDetail.quotationName }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ quotationDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ quotationDetail.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ quotationDetail.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="报价日期">{{ quotationDetail.quotationDate }}</a-descriptions-item>
        <a-descriptions-item label="有效期">{{ quotationDetail.validDays }}天</a-descriptions-item>
        <a-descriptions-item label="币种">{{ quotationDetail.currency }}</a-descriptions-item>
        <a-descriptions-item label="报价总额">
          <span class="amount">¥{{ formatAmount(quotationDetail.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="报价状态">
          <a-tag :color="getStatusColor(quotationDetail.status)">{{ getStatusText(quotationDetail.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="报价条款" :span="2">{{ quotationDetail.terms }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ quotationDetail.remark }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>报价明细</a-divider>
      <a-table
        :columns="detailItemColumns"
        :data-source="quotationDetail.items"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'subtotal'">
            <span class="amount">¥{{ formatAmount(record.subtotal) }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'

const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建报价')
const formRef = ref<FormInstance>()

const searchForm = reactive({
  quotationNo: '',
  quotationName: '',
  customerName: '',
  status: undefined
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
  { title: '报价单号', key: 'quotationNo', dataIndex: 'quotationNo', width: 150 },
  { title: '报价名称', dataIndex: 'quotationName', width: 200 },
  { title: '客户名称', dataIndex: 'customerName', width: 150 },
  { title: '报价日期', dataIndex: 'quotationDate', width: 100 },
  { title: '有效期', dataIndex: 'validDays', width: 80 },
  { title: '报价总额', key: 'totalAmount', dataIndex: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 150 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const itemColumns = [
  { title: '产品名称', key: 'productName', width: 150 },
  { title: '规格型号', key: 'spec', width: 100 },
  { title: '数量', key: 'quantity', width: 80 },
  { title: '单位', key: 'unit', width: 60 },
  { title: '单价', key: 'price', width: 100 },
  { title: '折扣%', key: 'discount', width: 80 },
  { title: '小计', key: 'subtotal', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

const detailItemColumns = [
  { title: '产品名称', dataIndex: 'productName', width: 150 },
  { title: '规格型号', dataIndex: 'spec', width: 100 },
  { title: '数量', dataIndex: 'quantity', width: 80 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '单价', dataIndex: 'price', width: 100 },
  { title: '折扣%', dataIndex: 'discount', width: 80 },
  { title: '小计', key: 'subtotal', dataIndex: 'subtotal', width: 100 }
]

const tableData = ref<any[]>([])

const formData = reactive({
  id: undefined,
  quotationNo: '',
  quotationName: '',
  customerId: undefined,
  contactPerson: '',
  contactPhone: '',
  quotationDate: undefined,
  validDays: 30,
  currency: 'CNY',
  terms: '',
  remark: '',
  items: [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0, subtotal: 0 }]
})

const formRules = {
  quotationName: [{ required: true, message: '请输入报价名称' }],
  customerId: [{ required: true, message: '请选择客户' }],
  quotationDate: [{ required: true, message: '请选择报价日期' }]
}

const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' }
])

const quotationDetail = ref<any>({})

onMounted(() => {
  loadTableData()
})

const loadTableData = () => {
  loading.value = true
  tableData.value = [
    { id: 1, quotationNo: 'QT20240115001', quotationName: '办公用品报价', customerName: '北京科技有限公司', quotationDate: '2024-01-15', validDays: 30, totalAmount: 58000, status: 'sent', createTime: '2024-01-15 10:30' },
    { id: 2, quotationNo: 'QT20240115002', quotationName: 'IT设备报价', customerName: '上海贸易公司', quotationDate: '2024-01-15', validDays: 15, totalAmount: 128000, status: 'accepted', createTime: '2024-01-15 09:20' },
    { id: 3, quotationNo: 'QT20240114003', quotationName: '办公家具报价', customerName: '广州制造企业', quotationDate: '2024-01-14', validDays: 30, totalAmount: 256000, status: 'draft', createTime: '2024-01-14 16:45' }
  ]
  pagination.total = 3
  loading.value = false
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: 'default',
    sent: 'blue',
    accepted: 'green',
    rejected: 'red',
    expired: 'orange'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    draft: '草稿',
    sent: '已发送',
    accepted: '已接受',
    rejected: '已拒绝',
    expired: '已过期'
  }
  return texts[status] || status
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const filterOption = (input: string, option: any) => {
  return option.name.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const calculateItemSubtotal = (item: any) => {
  const quantity = item.quantity || 0
  const price = item.price || 0
  const discount = item.discount || 0
  return (quantity * price * (1 - discount / 100)).toFixed(2)
}

const calculateTotalAmount = () => {
  return formData.items.reduce((sum, item) => {
    return sum + parseFloat(calculateItemSubtotal(item))
  }, 0)
}

const calculateDiscountAmount = () => {
  return formData.items.reduce((sum, item) => {
    const quantity = item.quantity || 0
    const price = item.price || 0
    const discount = item.discount || 0
    return sum + quantity * price * (discount / 100)
  }, 0)
}

const calculateGrandTotal = () => {
  return calculateTotalAmount()
}

const addItem = () => {
  formData.items.push({ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0, subtotal: 0 })
}

const removeItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleSearch = () => {
  pagination.current = 1
  loadTableData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    quotationNo: '',
    quotationName: '',
    customerName: '',
    status: undefined
  })
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTableData()
}

const handleAdd = () => {
  modalTitle.value = '新建报价'
  generateQuotationNo()
  formData.items = [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0, subtotal: 0 }]
  modalVisible.value = true
}

const generateQuotationNo = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  formData.quotationNo = `QT${year}${month}${day}${random}`
}

const handleView = (record: any) => {
  quotationDetail.value = {
    ...record,
    items: [
      { productName: '笔记本电脑', spec: '银色/16GB/512GB', quantity: 5, unit: '台', price: 8999, discount: 0, subtotal: 44995 },
      { productName: '无线鼠标', spec: '黑色', quantity: 10, unit: '个', price: 199, discount: 5, subtotal: 1890.5 },
      { productName: '机械键盘', spec: '青轴/白色', quantity: 5, unit: '个', price: 399, discount: 0, subtotal: 1995 }
    ],
    terms: '付款方式：预付30%，发货前付清余款\n交货期：下单后15个工作日内\n质保期：产品保修1年',
    remark: ''
  }
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  modalTitle.value = '编辑报价'
  Object.assign(formData, record)
  modalVisible.value = true
}

const handleSend = (record: any) => {
  message.success('报价已发送')
  loadTableData()
}

const handleConvert = (record: any) => {
  Modal.confirm({
    title: '确认转订单',
    content: `确定要将报价 "${record.quotationName}" 转为销售订单吗？`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success('报价已成功转为订单')
      loadTableData()
    }
  })
}

const handleCopy = (record: any) => {
  Modal.confirm({
    title: '复制报价',
    content: `确定要复制报价 "${record.quotationName}" 吗？将创建一个新的草稿报价单。`,
    okText: '确认复制',
    cancelText: '取消',
    centered: true,
    onOk() {
      modalTitle.value = '新建报价（复制）'
      generateQuotationNo()
      Object.assign(formData, {
        ...record,
        id: undefined,
        quotationNo: formData.quotationNo,
        items: (record.items || [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0, subtotal: 0 }]).map((item: any) => ({ ...item }))
      })
      modalVisible.value = true
    }
  })
}

const handleDeleteConfirm = (record: any) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除报价 "${record.quotationName}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success('删除成功')
      loadTableData()
    }
  })
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
.quotation-management {
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
}

.danger-link {
  color: #f5222d;
}
</style>