<template>
  <div class="contract-management">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>合同管理</h2>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建合同
        </a-button>
      </div>

      <a-form layout="inline" class="search-form">
        <a-form-item label="合同编号">
          <a-input v-model:value="searchForm.contractNo" placeholder="请输入合同编号" allow-clear />
        </a-form-item>
        <a-form-item label="合同名称">
          <a-input v-model:value="searchForm.contractName" placeholder="请输入合同名称" allow-clear />
        </a-form-item>
        <a-form-item label="客户名称">
          <a-input v-model:value="searchForm.customerName" placeholder="请输入客户名称" allow-clear />
        </a-form-item>
        <a-form-item label="合同类型">
          <a-select v-model:value="searchForm.contractType" placeholder="请选择合同类型" allow-clear style="width: 150px">
            <a-select-option value="sales">销售合同</a-select-option>
            <a-select-option value="purchase">采购合同</a-select-option>
            <a-select-option value="service">服务合同</a-select-option>
            <a-select-option value="lease">租赁合同</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="合同状态">
          <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
            <a-select-option value="draft">草稿</a-select-option>
            <a-select-option value="pending">待审批</a-select-option>
            <a-select-option value="approved">已审批</a-select-option>
            <a-select-option value="executing">执行中</a-select-option>
            <a-select-option value="completed">已完成</a-select-option>
            <a-select-option value="terminated">已终止</a-select-option>
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
          <template v-if="column.key === 'contractNo'">
            <a @click="handleView(record)">{{ record.contractNo }}</a>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'contractAmount'">
            <span class="amount">¥{{ formatAmount(record.contractAmount) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)" v-if="record.status === 'draft'">编辑</a>
              <a @click="handleApprove(record)" v-if="record.status === 'pending'">审批</a>
              <a @click="handleSign(record)" v-if="record.status === 'approved'">签订</a>
              <a-popconfirm title="确定要删除吗？" @confirm="handleDelete(record)">
                <a class="danger-link" v-if="record.status === 'draft'">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
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
        <a-form-item label="合同编号" name="contractNo">
          <a-input v-model:value="formData.contractNo" placeholder="自动生成" disabled />
        </a-form-item>
        <a-form-item label="合同名称" name="contractName">
          <a-input v-model:value="formData.contractName" placeholder="请输入合同名称" />
        </a-form-item>
        <a-form-item label="合同类型" name="contractType">
          <a-select v-model:value="formData.contractType" placeholder="请选择合同类型">
            <a-select-option value="sales">销售合同</a-select-option>
            <a-select-option value="purchase">采购合同</a-select-option>
            <a-select-option value="service">服务合同</a-select-option>
            <a-select-option value="lease">租赁合同</a-select-option>
          </a-select>
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
        <a-form-item label="开始日期" name="startDate">
          <a-date-picker v-model:value="formData.startDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="结束日期" name="endDate">
          <a-date-picker v-model:value="formData.endDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="合同金额" name="contractAmount">
          <a-input-number v-model:value="formData.contractAmount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="付款方式" name="paymentMethod">
          <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
            <a-select-option value="once">一次性付款</a-select-option>
            <a-select-option value="installment">分期付款</a-select-option>
            <a-select-option value="prepaid">预付款+尾款</a-select-option>
            <a-select-option value="monthly">月结</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="签订日期" name="signDate">
          <a-date-picker v-model:value="formData.signDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="签订人" name="signPerson">
          <a-input v-model:value="formData.signPerson" placeholder="请输入签订人" />
        </a-form-item>
        <a-form-item label="合同条款" name="terms">
          <a-textarea v-model:value="formData.terms" placeholder="请输入合同主要条款" :rows="4" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
        <a-form-item label="附件" name="attachments">
          <a-upload
            v-model:file-list="formData.attachments"
            :action="uploadUrl"
            :headers="uploadHeaders"
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
      title="合同详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="合同编号">{{ contractDetail.contractNo }}</a-descriptions-item>
        <a-descriptions-item label="合同名称">{{ contractDetail.contractName }}</a-descriptions-item>
        <a-descriptions-item label="合同类型">{{ contractDetail.contractTypeLabel }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ contractDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="开始日期">{{ contractDetail.startDate }}</a-descriptions-item>
        <a-descriptions-item label="结束日期">{{ contractDetail.endDate }}</a-descriptions-item>
        <a-descriptions-item label="合同金额">¥{{ formatAmount(contractDetail.contractAmount) }}</a-descriptions-item>
        <a-descriptions-item label="付款方式">{{ contractDetail.paymentMethodLabel }}</a-descriptions-item>
        <a-descriptions-item label="签订日期">{{ contractDetail.signDate }}</a-descriptions-item>
        <a-descriptions-item label="签订人">{{ contractDetail.signPerson }}</a-descriptions-item>
        <a-descriptions-item label="合同状态">
          <a-tag :color="getStatusColor(contractDetail.status)">{{ getStatusText(contractDetail.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ contractDetail.createTime }}</a-descriptions-item>
        <a-descriptions-item label="合同条款" :span="2">{{ contractDetail.terms }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ contractDetail.remark }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>审批流程</a-divider>
      <a-steps :current="contractDetail.currentStep" status="process">
        <a-step title="提交申请" :description="contractDetail.creator" />
        <a-step title="部门主管审批" :description="contractDetail.departmentApprover" />
        <a-step title="财务审批" :description="contractDetail.financeApprover" />
        <a-step title="总经理审批" :description="contractDetail.generalApprover" />
      </a-steps>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'

const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建合同')
const formRef = ref<FormInstance>()

const searchForm = reactive({
  contractNo: '',
  contractName: '',
  customerName: '',
  contractType: undefined,
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
  { title: '合同编号', key: 'contractNo', dataIndex: 'contractNo', width: 150 },
  { title: '合同名称', dataIndex: 'contractName', width: 200 },
  { title: '客户名称', dataIndex: 'customerName', width: 150 },
  { title: '合同类型', dataIndex: 'contractTypeLabel', width: 100 },
  { title: '合同金额', key: 'contractAmount', dataIndex: 'contractAmount', width: 120 },
  { title: '开始日期', dataIndex: 'startDate', width: 100 },
  { title: '结束日期', dataIndex: 'endDate', width: 100 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 150 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const tableData = ref<any[]>([])

const formData = reactive({
  id: undefined,
  contractNo: '',
  contractName: '',
  contractType: undefined,
  customerId: undefined,
  startDate: undefined,
  endDate: undefined,
  contractAmount: undefined,
  paymentMethod: undefined,
  signDate: undefined,
  signPerson: '',
  terms: '',
  remark: '',
  attachments: []
})

const formRules = {
  contractName: [{ required: true, message: '请输入合同名称' }],
  contractType: [{ required: true, message: '请选择合同类型' }],
  customerId: [{ required: true, message: '请选择客户' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }],
  contractAmount: [{ required: true, message: '请输入合同金额' }]
}

const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' }
])

const contractDetail = ref<any>({})

const uploadUrl = '/api/upload'
const uploadHeaders = {}

onMounted(() => {
  loadTableData()
})

const loadTableData = () => {
  loading.value = true
  tableData.value = [
    { id: 1, contractNo: 'CT20240115001', contractName: '办公用品采购合同', customerName: '北京科技有限公司', contractType: 'purchase', contractTypeLabel: '采购合同', contractAmount: 58000, startDate: '2024-01-15', endDate: '2024-12-31', status: 'executing', createTime: '2024-01-15 10:30' },
    { id: 2, contractNo: 'CT20240115002', contractName: 'IT服务合同', customerName: '上海贸易公司', contractType: 'service', contractTypeLabel: '服务合同', contractAmount: 128000, startDate: '2024-01-01', endDate: '2024-06-30', status: 'approved', createTime: '2024-01-15 09:20' },
    { id: 3, contractNo: 'CT20240114003', contractName: '销售合同', customerName: '广州制造企业', contractType: 'sales', contractTypeLabel: '销售合同', contractAmount: 256000, startDate: '2024-02-01', endDate: '2024-12-31', status: 'pending', createTime: '2024-01-14 16:45' }
  ]
  pagination.total = 3
  loading.value = false
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: 'default',
    pending: 'orange',
    approved: 'blue',
    executing: 'green',
    completed: 'green',
    terminated: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    draft: '草稿',
    pending: '待审批',
    approved: '已审批',
    executing: '执行中',
    completed: '已完成',
    terminated: '已终止'
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
    contractNo: '',
    contractName: '',
    customerName: '',
    contractType: undefined,
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
  modalTitle.value = '新建合同'
  generateContractNo()
  modalVisible.value = true
}

const generateContractNo = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  formData.contractNo = `CT${year}${month}${day}${random}`
}

const handleView = (record: any) => {
  contractDetail.value = {
    ...record,
    currentStep: 2,
    creator: '张三',
    departmentApprover: '李四',
    financeApprover: '王五',
    generalApprover: '赵六'
  }
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  modalTitle.value = '编辑合同'
  Object.assign(formData, record)
  modalVisible.value = true
}

const handleApprove = (record: any) => {
  message.info('审批功能开发中')
}

const handleSign = (record: any) => {
  message.info('签订功能开发中')
}

const handleDelete = (record: any) => {
  message.success('删除成功')
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
.contract-management {
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