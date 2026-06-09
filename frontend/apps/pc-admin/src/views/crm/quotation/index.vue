<template>
  <PageContainer full-height>
    <template #header>
      <div class="quotation-page-header">
        <div class="quotation-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>报价管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="quotation-page-header-title">报价管理</h2>
        </div>
        <div class="quotation-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
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
                <div class="stat-title">报价总数</div>
                <div class="stat-value">{{ pagination.total }}</div>
                <div class="stat-desc">全部报价</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <SendOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">已发送</div>
                <div class="stat-value">{{ statusCounts.sent }}</div>
                <div class="stat-desc">等待回复</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <CheckCircleOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">已接受</div>
                <div class="stat-value">{{ statusCounts.accepted }}</div>
                <div class="stat-desc positive">可转订单</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <DollarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">报价总额</div>
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
        add-text="新建报价"
        @add="handleAdd"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @export="handleExport"
      >
        <template #toolbar-actions>
          <a-button size="small" @click="handleBatchSend">
            <template #icon><SendOutlined /></template>
            批量发送
          </a-button>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的报价，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无报价数据，点击右上角「新建报价」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'draft'" title="编辑">
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'draft'" title="发送">
              <a-button type="link" size="small" @click="handleSend(record)">
                <template #icon><SendOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'accepted'" title="转订单">
              <a-button type="link" size="small" @click="handleConvert(record)">
                <template #icon><FileProtectOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><MoreOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="copy"><CopyOutlined /> 复制报价</a-menu-item>
                  <a-menu-item key="download"><DownloadOutlined /> 下载PDF</a-menu-item>
                  <a-menu-item key="history"><HistoryOutlined /> 版本历史</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" v-if="record.status === 'draft'" danger><DeleteOutlined /> 删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>
    </ErrorBoundary>

    <!-- 报价表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="900px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
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
          <a-col :span="12">
            <a-form-item label="客户名称" name="customerId" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
                <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
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
        <VxeTableList :columns="itemVxeColumns" :data-source="formData.items" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false">
          <template #productNameCell="{ record }">
            <a-input v-model:value="record.productName" placeholder="产品名称" />
          </template>
          <template #specCell="{ record }">
            <a-input v-model:value="record.spec" placeholder="规格型号" />
          </template>
          <template #quantityCell="{ record }">
            <a-input-number v-model:value="record.quantity" :min="1" style="width: 80px" />
          </template>
          <template #unitCell="{ record }">
            <a-input v-model:value="record.unit" placeholder="单位" style="width: 60px" />
          </template>
          <template #priceCell="{ record }">
            <a-input-number v-model:value="record.price" :min="0" :precision="2" style="width: 100px" />
          </template>
          <template #discountCell="{ record }">
            <a-input-number v-model:value="record.discount" :min="0" :max="100" style="width: 80px" />
          </template>
          <template #subtotalCell="{ record }">
            <span class="amount-cell">¥{{ calcItemSubtotal(record) }}</span>
          </template>
          <template #action="{ index }">
            <a @click="removeItem(index)" v-if="formData.items.length > 1" class="delete-link">删除</a>
          </template>
        </VxeTableList>
        <a-button type="dashed" block @click="addItem" style="margin-top: 16px">
          <template #icon><PlusOutlined /></template>
          添加产品
        </a-button>

        <a-divider>费用汇总</a-divider>
        <a-row :gutter="24" class="summary-row">
          <a-col :span="8">
            <div class="summary-item">
              <span class="summary-label">产品金额</span>
              <span class="summary-value">¥{{ formatAmount(calcTotalAmount()) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="summary-item">
              <span class="summary-label">折扣金额</span>
              <span class="summary-value discount">¥{{ formatAmount(calcDiscountAmount()) }}</span>
            </div>
          </a-col>
          <a-col :span="8">
            <div class="summary-item total">
              <span class="summary-label">报价总额</span>
              <span class="summary-value">¥{{ formatAmount(calcGrandTotal()) }}</span>
            </div>
          </a-col>
        </a-row>

        <a-form-item label="报价条款" name="terms" style="margin-top: 16px">
          <a-textarea v-model:value="formData.terms" placeholder="请输入报价条款" :rows="3" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-drawer v-model:open="detailVisible" title="报价详情" placement="right" width="80vw" :footer="null">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="报价单号">
          <span class="quotation-no">{{ quotationDetail.quotationNo }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="报价名称">{{ quotationDetail.quotationName }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ quotationDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ quotationDetail.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ quotationDetail.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="报价日期">{{ quotationDetail.quotationDate }}</a-descriptions-item>
        <a-descriptions-item label="有效期">{{ quotationDetail.validDays }}天</a-descriptions-item>
        <a-descriptions-item label="币种">{{ quotationDetail.currency }}</a-descriptions-item>
        <a-descriptions-item label="报价总额">
          <span class="amount-cell">¥{{ formatAmount(quotationDetail.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="报价状态">
          <a-tag :color="getStatusColor(quotationDetail.status)">{{ getStatusText(quotationDetail.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="报价条款" :span="2">{{ quotationDetail.terms || '无' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ quotationDetail.remark || '无' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>报价明细</a-divider>
      <VxeTableList :columns="detailItemVxeColumns" :data-source="quotationDetail.items" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false">
        <template #subtotalCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.subtotal) }}</span>
        </template>
        <template #priceCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.price) }}</span>
        </template>
      </VxeTableList>

      <div class="detail-footer">
        <a-space>
          <a-button type="primary" @click="handleDownloadPDF"><DownloadOutlined /> 下载PDF</a-button>
          <a-button v-if="quotationDetail.status === 'accepted'" type="primary" @click="handleConvertFromDetail">转为订单</a-button>
          <a-button v-if="quotationDetail.status === 'draft'" @click="handleSendFromDetail"><SendOutlined /> 发送报价</a-button>
        </a-space>
      </div>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import { quotationApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  SendOutlined,
  CopyOutlined,
  FileProtectOutlined,
  ReloadOutlined,
  SyncOutlined,
  SearchOutlined,
  InboxOutlined,
  MoreOutlined,
  DownloadOutlined,
  HistoryOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const modalTitle = ref('新建报价')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
const selectedRowKeys = ref<number[]>([])
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const vxeColumns = computed(() => [
  { field: 'quotationNo', title: '报价单号', width: 150, formatter: ({ row }: any) => row.quotationNo || '' },
  { field: 'quotationName', title: '报价名称', width: 200 },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'quotationDate', title: '报价日期', width: 100 },
  { field: 'validDays', title: '有效期', width: 80, align: 'right', formatter: ({ cellValue }: any) => `${cellValue || 0}天` },
  { field: 'totalAmount', title: '报价总额', width: 130, align: 'right', formatter: ({ cellValue }: any) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }: any) => getStatusText(cellValue) },
  { field: 'createTime', title: '创建时间', width: 150 },
  { field: 'action', title: '操作', width: 160, fixed: 'right', type: 'action' }
])

const filterFields = [
  { key: 'quotationNo', label: '报价单号', type: 'input' as const, placeholder: '输入报价单号' },
  { key: 'quotationName', label: '报价名称', type: 'input' as const, placeholder: '输入报价名称' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已发送', value: 'sent' },
    { label: '已接受', value: 'accepted' },
    { label: '已拒绝', value: 'rejected' },
    { label: '已过期', value: 'expired' }
  ]}
]

const statusColorMap: Record<string, string> = { draft: 'default', sent: 'blue', accepted: 'green', rejected: 'red', expired: 'orange' }
const statusTextMap: Record<string, string> = { draft: '草稿', sent: '已发送', accepted: '已接受', rejected: '已拒绝', expired: '已过期' }

// 状态统计
const statusCounts = computed(() => {
  const sent = tableData.value.filter(r => r.status === 'sent').length
  const accepted = tableData.value.filter(r => r.status === 'accepted').length
  return { sent, accepted }
})

const totalAmount = computed(() => {
  return tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = tableData

function getStatusColor(status: string): string { return statusColorMap[status] || 'default' }
function getStatusText(status: string): string { return statusTextMap[status] || '未知' }
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

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
  items: [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]
})
const formRules = {
  quotationName: [{ required: true, message: '请输入报价名称' }],
  customerId: [{ required: true, message: '请选择客户' }],
  quotationDate: [{ required: true, message: '请选择报价日期' }]
}
const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' },
  { id: 4, name: '深圳电子公司' },
  { id: 5, name: '杭州互联网公司' }
])
const quotationDetail = ref<any>({})

const itemVxeColumns = [
  { field: 'productName', title: '产品名称', width: 150, slotName: 'productNameCell' },
  { field: 'spec', title: '规格型号', width: 100, slotName: 'specCell' },
  { field: 'quantity', title: '数量', width: 80, align: 'right', slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 60, align: 'center', slotName: 'unitCell' },
  { field: 'price', title: '单价', width: 100, align: 'right', slotName: 'priceCell' },
  { field: 'discount', title: '折扣%', width: 80, align: 'right', slotName: 'discountCell' },
  { field: 'subtotal', title: '小计', width: 100, align: 'right', slotName: 'subtotalCell' },
  { field: 'action', title: '操作', width: 60, align: 'center', type: 'action' }
]
const detailItemVxeColumns = [
  { field: 'productName', title: '产品名称', width: 150 },
  { field: 'spec', title: '规格型号', width: 100 },
  { field: 'quantity', title: '数量', width: 80, align: 'right' },
  { field: 'unit', title: '单位', width: 60, align: 'center' },
  { field: 'price', title: '单价', width: 100, align: 'right', slotName: 'priceCell' },
  { field: 'discount', title: '折扣%', width: 80, align: 'right' },
  { field: 'subtotal', title: '小计', width: 100, align: 'right', slotName: 'subtotalCell' }
]

function calcItemSubtotal(item: any) {
  const qty = item.quantity || 0
  const price = item.price || 0
  const d = item.discount || 0
  return (qty * price * (1 - d / 100)).toFixed(2)
}
function calcTotalAmount() {
  return formData.items.reduce((s, item) => s + parseFloat(calcItemSubtotal(item)), 0)
}
function calcDiscountAmount() {
  return formData.items.reduce((s, item) => {
    const qty = item.quantity || 0
    const price = item.price || 0
    const d = item.discount || 0
    return s + qty * price * (d / 100)
  }, 0)
}
function calcGrandTotal() { return calcTotalAmount() }
function addItem() { formData.items.push({ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }) }
function removeItem(index: number) { formData.items.splice(index, 1) }

function generateQuotationNo() {
  const now = new Date()
  formData.quotationNo = `QT${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const res = await quotationApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    tableData.value = (res as any).records || []
    pagination.total = (res as any).total || 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    if (!silent) {
      hasError.value = true
      message.error('获取报价数据失败')
    }
    console.warn('[CRM报价] 获取报价列表失败', err)
    tableData.value = []
  } finally {
    if (!silent) loading.value = false
    refreshLoading.value = false
  }
}

const handleRefresh = () => {
  lastUpdateTime.value = ''
  fetchData()
}

function handleView(record: any) {
  quotationDetail.value = {
    ...record,
    items: [
      { productName: '笔记本电脑', spec: '银色/16GB/512GB', quantity: 5, unit: '台', price: 8999, discount: 0, subtotal: 44995 },
      { productName: '无线鼠标', spec: '黑色', quantity: 10, unit: '个', price: 199, discount: 5, subtotal: 1890.5 },
      { productName: '机械键盘', spec: '青轴/白色', quantity: 5, unit: '个', price: 399, discount: 0, subtotal: 1995 }
    ],
    terms: '付款方式：预付30%，发货前付清余款\n交货期：下单后15个工作日内\n质保期：产品保修1年',
    remark: '',
    currency: 'CNY',
    contactPerson: '张总',
    contactPhone: '13800138001'
  }
  detailVisible.value = true
}

function handleEdit(record: any) {
  modalTitle.value = '编辑报价'
  Object.assign(formData, record)
  formData.items = record.items || [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]
  modalVisible.value = true
}

function handleAdd() {
  modalTitle.value = '新建报价'
  generateQuotationNo()
  Object.assign(formData, {
    id: undefined, quotationName: '', customerId: undefined, contactPerson: '', contactPhone: '',
    quotationDate: undefined, validDays: 30, currency: 'CNY', terms: '', remark: '',
    items: [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]
  })
  modalVisible.value = true
}

function handleResetFilters() {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

async function handleSend(record: any) {
  try { await quotationApi.send(record.id); message.success('报价已发送'); fetchData() }
  catch (err) { console.warn('[CRM报价] 发送报价失败', err); message.error('发送失败') }
}

function handleConvert(record: any) {
  Modal.confirm({
    title: '确认转订单',
    content: `确定要将报价 "${record.quotationName}" 转为销售订单吗？`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try { await quotationApi.convertToOrder(record.id); message.success('报价已成功转为订单'); fetchData() }
      catch (err) { console.warn('[CRM报价] 转换订单失败', err); message.error('转换失败') }
    }
  })
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'copy':
      Modal.confirm({
        title: '复制报价',
        content: `确定要复制报价 "${record.quotationName}" 吗？`,
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
            items: (record.items || [{ productName: '', spec: '', quantity: 1, unit: '', price: 0, discount: 0 }]).map((i: any) => ({ ...i }))
          })
          modalVisible.value = true
        }
      })
      break
    case 'download':
      message.info(`下载报价PDF: ${record.quotationNo}`)
      break
    case 'history':
      message.info(`版本历史: ${record.quotationNo}`)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除报价 "${record.quotationName}" 吗？`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk: async () => {
          try { await quotationApi.delete(record.id); message.success('删除成功'); fetchData() }
          catch (err) { console.warn('[CRM报价] 删除报价失败', err); message.error('删除失败') }
        }
      })
      break
  }
}

function handleBatchSend() {
  message.info('批量发送报价')
}

function handleDownloadPDF() {
  if (quotationDetail.value.quotationNo) {
    message.info(`下载报价PDF: ${quotationDetail.value.quotationNo}`)
  }
}

function handleConvertFromDetail() {
  handleConvert(quotationDetail.value)
  detailVisible.value = false
}

function handleSendFromDetail() {
  handleSend(quotationDetail.value)
  detailVisible.value = false
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch (err) { console.warn('[CRM报价] 表单验证失败', err); return }
  submitLoading.value = true
  try {
    const payload = {
      ...formData,
      quotationDate: formData.quotationDate ? dayjs(formData.quotationDate).format('YYYY-MM-DD') : undefined
    }
    if (formData.id) { await quotationApi.update(formData.id, payload) } else { await quotationApi.create(payload) }
    message.success('保存成功')
    modalVisible.value = false
    fetchData()
  } catch (err: any) { console.warn('[CRM报价] 保存报价失败', err); message.error(err?.message || '保存失败') }
  finally { submitLoading.value = false }
}

function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  const headers = ['报价单号', '报价名称', '客户名称', '报价日期', '有效期', '报价总额', '状态', '创建时间']
  const rows = tableData.value.map((row: any) => [
    row.quotationNo, row.quotationName, row.customerName, row.quotationDate, row.validDays, row.totalAmount, getStatusText(row.status), row.createTime
  ])
  exportCsv(headers, rows, '报价')
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
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.quotation-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.quotation-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.quotation-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.quotation-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
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


.quotation-no {
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

.delete-link {
  color: #ff4d4f;
}

.summary-row {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-label {
  font-size: 14px;
  color: #666;
}

.summary-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.summary-item.total .summary-value {
  color: #f5222d;
}

.summary-value.discount {
  color: #faad14;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
