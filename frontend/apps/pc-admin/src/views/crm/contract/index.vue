<template>
  <PageContainer title="合同管理" full-height>
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
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <FileTextOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">待审批</div>
                <div class="stat-value">{{ statusCounts.pending }}</div>
                <div class="stat-desc">需审批处理</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <EditOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">待签署</div>
                <div class="stat-value">{{ statusCounts.signing }}</div>
                <div class="stat-desc">等待签署</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <CheckCircleOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">生效中</div>
                <div class="stat-value">{{ statusCounts.effective }}</div>
                <div class="stat-desc">正在执行</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <DollarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">合同总额</div>
                <div class="stat-value">¥{{ formatAmount(totalAmount) }}</div>
                <div class="stat-desc">本页合计</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :filter-fields="filterFields"
        :show-export="true"
        :selectable="true"
        add-text="新建合同"
        @add="handleAdd"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @export="handleExport"
      >
        <template #toolbar-actions>
          <a-button size="small" @click="handleBatchApprove">
            <template #icon><CheckCircleOutlined /></template>
            批量审批
          </a-button>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的合同，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无合同数据，点击右上角「新建合同」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-else>
            <a-space :size="4">
              <a-tooltip title="查看详情">
                <a-button type="link" size="small" @click="handleView(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 0" title="编辑">
                <a-button type="link" size="small" @click="handleEdit(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 1" title="审批">
                <a-button type="link" size="small" @click="handleApprove(record)">
                  <template #icon><CheckCircleOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 2" title="签订">
                <a-button type="link" size="small" @click="handleSign(record)">
                  <template #icon><FileDoneOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><MoreOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                    <a-menu-item key="download"><DownloadOutlined /> 下载合同</a-menu-item>
                    <a-menu-item key="renew"><HistoryOutlined /> 续签申请</a-menu-item>
                    <a-menu-item key="invoice"><FileTextOutlined /> 开票申请</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="terminate" v-if="record.status >= 5" danger><StopOutlined /> 终止合同</a-menu-item>
                    <a-menu-item key="delete" v-if="record.status === 0" danger><DeleteOutlined /> 删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </VxeTableList>
    </ErrorBoundary>

    <!-- 合同表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="合同编号" name="contractNo">
              <a-input v-model:value="formData.contractNo" placeholder="自动生成" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同名称" name="contractName">
              <a-input v-model:value="formData.contractName" placeholder="请输入合同名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同类型" name="contractType">
              <a-select v-model:value="formData.contractType" placeholder="请选择合同类型">
                <a-select-option value="sales">销售合同</a-select-option>
                <a-select-option value="purchase">采购合同</a-select-option>
                <a-select-option value="service">服务合同</a-select-option>
                <a-select-option value="lease">租赁合同</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户名称" name="customerId">
              <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
                <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate">
              <a-date-picker v-model:value="formData.startDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate">
              <a-date-picker v-model:value="formData.endDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="合同金额" name="contractAmount">
              <a-input-number v-model:value="formData.contractAmount" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="付款方式" name="paymentMethod">
              <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
                <a-select-option value="once">一次性付款</a-select-option>
                <a-select-option value="installment">分期付款</a-select-option>
                <a-select-option value="prepaid">预付款+尾款</a-select-option>
                <a-select-option value="monthly">月结</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="签订日期" name="signDate">
              <a-date-picker v-model:value="formData.signDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="签订人" name="signPerson">
              <a-input v-model:value="formData.signPerson" placeholder="请输入签订人" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="合同条款" name="terms" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formData.terms" placeholder="请输入合同主要条款" :rows="3" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="合同详情" width="800px" :footer="null">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="合同编号">
          <span class="contract-no">{{ contractDetail.contractNo }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="合同名称">{{ contractDetail.contractName }}</a-descriptions-item>
        <a-descriptions-item label="合同类型">{{ contractDetail.contractTypeLabel }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ contractDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="开始日期">{{ contractDetail.startDate }}</a-descriptions-item>
        <a-descriptions-item label="结束日期">{{ contractDetail.endDate }}</a-descriptions-item>
        <a-descriptions-item label="合同金额">
          <span class="amount-cell">¥{{ formatAmount(contractDetail.contractAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="付款方式">{{ contractDetail.paymentMethodLabel }}</a-descriptions-item>
        <a-descriptions-item label="签订日期">{{ contractDetail.signDate }}</a-descriptions-item>
        <a-descriptions-item label="签订人">{{ contractDetail.signPerson }}</a-descriptions-item>
        <a-descriptions-item label="合同状态">
          <a-tag :color="getStatusColor(contractDetail.status)">{{ getStatusText(contractDetail.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ contractDetail.createTime }}</a-descriptions-item>
        <a-descriptions-item label="合同条款" :span="2">{{ contractDetail.terms || '无' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ contractDetail.remark || '无' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>审批流程</a-divider>
      <a-steps :current="contractDetail.currentStep" status="process" size="small">
        <a-step title="提交申请" :description="contractDetail.creator" />
        <a-step title="部门主管审批" :description="contractDetail.departmentApprover" />
        <a-step title="财务审批" :description="contractDetail.financeApprover" />
        <a-step title="总经理审批" :description="contractDetail.generalApprover" />
      </a-steps>

      <div class="detail-footer">
        <a-space>
          <a-button type="primary" @click="handleDownload"><DownloadOutlined /> 下载合同</a-button>
          <a-button v-if="contractDetail.status >= 5" @click="handleRenewApply">续签申请</a-button>
        </a-space>
      </div>
    </a-modal>

    <!-- 签订弹窗 -->
    <a-modal v-model:open="signModalVisible" title="合同签订" width="500px" :confirm-loading="signLoading" @ok="handleSignSubmit" @cancel="signModalVisible = false">
      <a-form :model="signForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="合同名称">
          <a-input :value="signForm.contractName" disabled />
        </a-form-item>
        <a-form-item label="签订日期" required>
          <a-date-picker v-model:value="signForm.signDate" style="width: 100%" placeholder="请选择签订日期" />
        </a-form-item>
        <a-form-item label="签订人" required>
          <a-input v-model:value="signForm.signPerson" placeholder="请输入签订人姓名" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import { contractApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  SearchOutlined,
  InboxOutlined,
  MoreOutlined,
  DownloadOutlined,
  HistoryOutlined,
  FileTextOutlined,
  StopOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const submitLoading = ref(false)
const signLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const signModalVisible = ref(false)
const modalTitle = ref('新建合同')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
const selectedRowKeys = ref<number[]>([])
let autoRefreshTimer: number | null = null

const vxeColumns = computed(() => [
  { field: 'contractNo', title: '合同编号', width: 150, formatter: ({ row }: any) => row.contractNo || '' },
  { field: 'contractName', title: '合同名称', width: 200 },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'contractTypeLabel', title: '合同类型', width: 100, align: 'center' },
  { field: 'contractAmount', title: '合同金额', width: 130, align: 'right', formatter: ({ cellValue }: any) => `¥${formatAmount(cellValue)}` },
  { field: 'startDate', title: '开始日期', width: 100 },
  { field: 'endDate', title: '结束日期', width: 100 },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }: any) => getStatusText(cellValue) },
  { field: 'createTime', title: '创建时间', width: 150 },
  { field: 'action', title: '操作', width: 160, fixed: 'right', type: 'action' }
])

const filterFields = [
  { key: 'contractNo', label: '合同编号', type: 'input' as const, placeholder: '输入合同编号' },
  { key: 'contractName', label: '合同名称', type: 'input' as const, placeholder: '输入合同名称' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'contractType', label: '合同类型', type: 'select' as const, options: [
    { label: '销售合同', value: 'sales' },
    { label: '采购合同', value: 'purchase' },
    { label: '服务合同', value: 'service' },
    { label: '租赁合同', value: 'lease' }
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '待签署', value: 3 },
    { label: '已签署', value: 4 },
    { label: '生效中', value: 5 },
    { label: '已完成', value: 7 },
    { label: '已终止', value: 8 }
  ]}
]

const statusColorMap: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'blue', 3: 'geekblue', 4: 'purple', 5: 'green', 6: 'green', 7: 'green', 8: 'red', 9: 'red', 10: 'red'
}
const statusTextMap: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '待签署', 4: '已签署', 5: '生效中', 6: '执行中', 7: '已完成', 8: '已终止', 9: '已过期', 10: '已取消'
}

// 状态统计
const statusCounts = computed(() => {
  const pending = tableData.value.filter(r => r.status === 1).length
  const signing = tableData.value.filter(r => r.status === 3 || r.status === 2).length
  const effective = tableData.value.filter(r => r.status >= 5 && r.status < 8).length
  return { pending, signing, effective }
})

const totalAmount = computed(() => {
  return tableData.value.reduce((s, r) => s + (r.contractAmount || 0), 0)
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 空行填充
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

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
  { id: 3, name: '广州制造企业' },
  { id: 4, name: '深圳电子公司' },
  { id: 5, name: '杭州互联网公司' }
])
const contractDetail = ref<any>({})
const signForm = reactive({ contractId: undefined, contractName: '', signDate: undefined as any, signPerson: '' })

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

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

function generateContractNo() {
  const now = new Date()
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0')
  formData.contractNo = `CT${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${random}`
}

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchFilters.keyword) params.keyword = searchFilters.keyword
    if (searchFilters.status !== undefined && searchFilters.status !== null) params.status = Number(searchFilters.status)
    const res = await contractApi.page(params)
    const result = res as any
    const records = result.records || result.data?.records || []
    tableData.value = records.map((r: any) => ({
      ...r,
      contractTypeLabel: r.contractTypeLabel || r.contractType || ''
    }))
    pagination.total = result.total ?? result.data?.total ?? 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    if (!silent) {
      hasError.value = true
      message.error('获取合同数据失败')
    }
    tableData.value = mockData()
    pagination.total = mockData().length
  } finally {
    if (!silent) loading.value = false
  }
}

const mockData = () => [
  { id: 1, contractNo: 'CT2024010001', contractName: 'ERP系统销售合同', customerName: '北京科技有限公司', contractTypeLabel: '销售合同', contractAmount: 580000, startDate: '2024-01-01', endDate: '2024-12-31', status: 5, createTime: '2024-01-10 10:00', signDate: '2024-01-05', signPerson: '张三' },
  { id: 2, contractNo: 'CT2024010002', contractName: '智能制造升级合同', customerName: '上海贸易公司', contractTypeLabel: '服务合同', contractAmount: 320000, startDate: '2024-02-01', endDate: '2025-01-31', status: 3, createTime: '2024-01-12 11:00' },
  { id: 3, contractNo: 'CT2024010003', contractName: '数据分析平台合同', customerName: '广州制造企业', contractTypeLabel: '销售合同', contractAmount: 150000, startDate: '2024-03-01', endDate: '2024-08-31', status: 1, createTime: '2024-01-15 09:00' },
  { id: 4, contractNo: 'CT2024010004', contractName: '云服务迁移合同', customerName: '深圳电子公司', contractTypeLabel: '服务合同', contractAmount: 420000, startDate: '2024-01-15', endDate: '2024-06-30', status: 7, createTime: '2024-01-08 14:00', signDate: '2024-01-10', signPerson: '李四' },
  { id: 5, contractNo: 'CT2024010005', contractName: '办公设备采购合同', customerName: '杭州互联网公司', contractTypeLabel: '采购合同', contractAmount: 80000, startDate: '2024-01-01', endDate: '2024-01-31', status: 0, createTime: '2024-01-18 15:00' }
]

const handleRefresh = () => {
  lastUpdateTime.value = ''
  fetchData()
}

function handleView(record: any) {
  contractDetail.value = {
    ...record,
    currentStep: record.status >= 5 ? 4 : record.status >= 2 ? 3 : record.status >= 1 ? 2 : 1,
    creator: '张三',
    departmentApprover: record.status >= 1 ? '李四' : '',
    financeApprover: record.status >= 2 ? '王五' : '',
    generalApprover: record.status >= 3 ? '赵六' : '',
    paymentMethodLabel: '分期付款'
  }
  detailVisible.value = true
}

function handleEdit(record: any) {
  modalTitle.value = '编辑合同'
  Object.assign(formData, record)
  modalVisible.value = true
}

function handleAdd() {
  modalTitle.value = '新建合同'
  generateContractNo()
  Object.assign(formData, { id: undefined, contractName: '', contractType: undefined, customerId: undefined, startDate: undefined, endDate: undefined, contractAmount: undefined, paymentMethod: undefined, signDate: undefined, signPerson: '', terms: '', remark: '' })
  modalVisible.value = true
}

function handleResetFilters() {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批',
    content: `确定要审批合同 "${record.contractName}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try { await contractApi.approve(record.id); message.success('审批成功'); fetchData() }
      catch { message.error('审批失败') }
    }
  })
}

function handleSign(record: any) {
  signForm.contractId = record.id
  signForm.contractName = record.contractName
  signForm.signDate = undefined
  signForm.signPerson = record.signPerson || ''
  signModalVisible.value = true
}

async function handleSignSubmit() {
  if (!signForm.signDate) { message.warning('请选择签订日期'); return }
  if (!signForm.signPerson.trim()) { message.warning('请输入签订人'); return }
  signLoading.value = true
  try {
    await contractApi.sign(signForm.contractId!, 'manual')
    message.success(`合同"${signForm.contractName}"签订成功`)
    signModalVisible.value = false
    fetchData()
  } catch {
    message.error('签订失败')
  } finally {
    signLoading.value = false
  }
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'download':
      message.info(`下载合同: ${record.contractNo}`)
      break
    case 'renew':
      message.info(`续签申请: ${record.contractName}`)
      break
    case 'invoice':
      message.info(`开票申请: ${record.contractName}`)
      break
    case 'terminate':
      Modal.confirm({
        title: '确认终止',
        content: `确定要终止合同 "${record.contractName}" 吗？`,
        okText: '确认终止',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk: async () => {
          try { await contractApi.terminate(record.id, '手动终止'); message.success('合同已终止'); fetchData() }
          catch { message.error('终止失败') }
        }
      })
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除合同 "${record.contractName}" 吗？`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk: async () => {
          try { await contractApi.terminate(record.id, '删除'); message.success('删除成功'); fetchData() }
          catch { message.error('删除失败') }
        }
      })
      break
  }
}

function handleBatchApprove() {
  message.info('批量审批合同')
}

function handleDownload() {
  if (contractDetail.value.contractNo) {
    message.info(`下载合同: ${contractDetail.value.contractNo}`)
  }
}

function handleRenewApply() {
  message.info(`续签申请: ${contractDetail.value.contractName}`)
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try {
    const data = { ...formData }
    if (data.id) {
      await contractApi.update(data.id, data)
    } else {
      await contractApi.create(data)
    }
    message.success('保存成功')
    modalVisible.value = false
    fetchData()
  } catch {
    message.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const headers = ['合同编号', '合同名称', '客户名称', '合同类型', '合同金额', '开始日期', '结束日期', '状态', '创建时间']
  const rows = tableData.value.map((row: any) => [
    row.contractNo, row.contractName, row.customerName, row.contractTypeLabel, row.contractAmount,
    row.startDate, row.endDate, getStatusText(row.status), row.createTime
  ])
  exportCsv(headers, rows, '合同')
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}
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

.empty-placeholder {
  color: transparent;
}

.contract-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}
</style>