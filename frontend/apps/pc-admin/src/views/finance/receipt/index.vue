<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="rcpt-page-header">
        <div class="rcpt-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>收款单管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="rcpt-page-header-title">收款单管理</h2>
        </div>
        <div class="rcpt-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <div class="finance-rcpt-page">
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">收款总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-verified">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.verifiedAmount) }}</div>
            <div class="stat-card-label">已核销</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.pendingAmount) }}</div>
            <div class="stat-card-label">待核销</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :min-empty-rows="12"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :show-search="false"
        export-permission="finance:receipt:list"
        :show-add="false"
        :show-edit="false"
        :show-delete="false"
        :selectable="true"
        @refresh="fetchData"
        @cell-dblclick="handleView"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <a-button type="primary" size="small" v-permission="'finance:receipt:create'" @click="debounceClick('add', handleAdd)">
            <template #icon><PlusOutlined /></template>新增
          </a-button>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>

        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="fetchData" class="table-empty-action"><ReloadOutlined /> 重试</a-button>
            </template>
            <template v-else>
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">没有符合条件的收款单，<a @click="handleResetFilters">清除筛选</a></p>
              <p v-else class="table-empty-text">暂无收款单数据</p>
            </template>
          </div>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusLabelMap[record.status] || '未知' }}</a-tag>
        </template>
        <template #receiptTypeCell="{ record }">
          <span>{{ receiptTypeLabelMap[record.sourceType] || record.sourceType || '其他' }}</span>
        </template>
        <template #action="{ record }">
          <a-space :size="0" class="action-cell-inner">
            <PrintButton
              template-type="receipt"
              :business-id="record.id"
              business-type="receipt"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-tooltip title="查看">
              <a-button type="link" size="small" v-permission="'finance:receipt:view'" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="编辑">
              <a-button type="link" size="small" v-permission="'finance:receipt:edit'" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="提交审批">
              <a-button type="link" size="small" v-permission="'finance:receipt:submit'" @click="handleSubmit(record)">
                <template #icon><SendOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="审批通过">
              <a-button type="link" size="small" v-permission="'finance:receipt:approve'" @click="handleApprove(record)">
                <template #icon><CheckOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 2" title="完成收款">
              <a-button type="link" size="small" v-permission="'finance:receipt:complete'" @click="handleComplete(record)">
                <template #icon><FileDoneOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status >= 0 && record.status < 5" title="取消">
              <a-button type="link" size="small" v-permission="'finance:receipt:cancel'" danger @click="handleCancel(record)">
                <template #icon><CloseOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 新增/编辑弹窗 -->
      <FullScreenDetail
        :visible="formVisible"
        :title="isEditing ? '编辑收款单' : '新增收款单'"
        :save-loading="formLoading"
        :dirty="formDirty"
        @save="handleFormSave"
        @close="handleFormClose"
      >
        <a-form ref="formRef" :model="formState" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }" :rules="formRules">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="客户名称" name="customerName" required>
                <a-input v-model:value="formState.customerName" placeholder="请输入客户名称" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="收款金额" name="receiptAmount" required>
                <a-input-number v-model:value="formState.receiptAmount" :min="0.01" :precision="2" style="width:100%" placeholder="请输入收款金额" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="收款方式" name="paymentMethod">
                <a-select v-model:value="formState.paymentMethod" placeholder="选择收款方式">
                  <a-select-option value="bank_transfer">银行转账</a-select-option>
                  <a-select-option value="cash">现金</a-select-option>
                  <a-select-option value="check">支票</a-select-option>
                  <a-select-option value="wechat">微信</a-select-option>
                  <a-select-option value="alipay">支付宝</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="来源类型" name="sourceType">
                <a-select v-model:value="formState.sourceType" placeholder="选择来源类型">
                  <a-select-option value="SALE_ORDER">销售回款</a-select-option>
                  <a-select-option value="PURCHASE_RETURN">采购退货退款</a-select-option>
                  <a-select-option value="PRE_RECEIPT">预收转正</a-select-option>
                  <a-select-option value="DEPOSIT">定金转正</a-select-option>
                  <a-select-option value="ASSET_DISPOSAL">资产处置</a-select-option>
                  <a-select-option value="EMPLOYEE_REFUND">员工退还</a-select-option>
                  <a-select-option value="OTHER">其他收入</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="收款日期" name="receiptDate">
                <a-date-picker v-model:value="formState.receiptDate" style="width:100%" placeholder="选择收款日期" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="银行账号" name="bankAccount">
                <a-input v-model:value="formState.bankAccount" placeholder="请输入银行账号" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="formState.remark" :rows="3" placeholder="请输入备注" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 查看详情弹窗 -->
      <FullScreenDetail
        :visible="detailVisible"
        :title="`收款单详情 - ${detailData?.receiptNo || ''}`"
        :footer="false"
        @close="handleDetailClose"
      >
        <a-descriptions v-if="detailData" :column="2" bordered size="small">
          <a-descriptions-item label="收款单号" :span="1">{{ detailData.receiptNo }}</a-descriptions-item>
          <a-descriptions-item label="状态" :span="1">
            <a-tag :color="statusColorMap[detailData.status]">{{ statusLabelMap[detailData.status] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="客户名称" :span="1">{{ detailData.customerName }}</a-descriptions-item>
          <a-descriptions-item label="收款金额" :span="1">¥{{ formatAmount(detailData.receiptAmount) }}</a-descriptions-item>
          <a-descriptions-item label="已核销" :span="1">¥{{ formatAmount(detailData.verifiedAmount) }}</a-descriptions-item>
          <a-descriptions-item label="待核销" :span="1">¥{{ formatAmount(detailData.pendingAmount) }}</a-descriptions-item>
          <a-descriptions-item label="收款方式" :span="1">{{ detailData.paymentMethod || '-' }}</a-descriptions-item>
          <a-descriptions-item label="来源类型" :span="1">{{ receiptTypeLabelMap[detailData.sourceType] || detailData.sourceType || '-' }}</a-descriptions-item>
          <a-descriptions-item label="来源单号" :span="1">{{ detailData.sourceNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收款日期" :span="1">{{ detailData.receiptDate }}</a-descriptions-item>
          <a-descriptions-item label="银行账号" :span="1">{{ detailData.bankAccount || '-' }}</a-descriptions-item>
          <a-descriptions-item label="交易流水号" :span="1">{{ detailData.transactionNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建人" :span="1">{{ detailData.createBy || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间" :span="1">{{ detailData.createTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detailData.approvedBy" label="审批人" :span="1">{{ detailData.approvedBy }}</a-descriptions-item>
          <a-descriptions-item v-if="detailData.approvedTime" label="审批时间" :span="1">{{ detailData.approvedTime }}</a-descriptions-item>
          <a-descriptions-item v-if="detailData.completedBy" label="完成人" :span="1">{{ detailData.completedBy }}</a-descriptions-item>
          <a-descriptions-item v-if="detailData.completedTime" label="完成时间" :span="1">{{ detailData.completedTime }}</a-descriptions-item>
        </a-descriptions>
      </FullScreenDetail>

      <!-- 取消确认弹窗 -->
      <a-modal :visible="cancelVisible" title="取消收款单" :confirm-loading="cancelLoading" @ok="handleCancelConfirm" @cancel="cancelVisible = false">
        <a-form layout="vertical">
          <a-descriptions v-if="cancelTarget" :column="1" bordered size="small">
            <a-descriptions-item label="收款单号">{{ cancelTarget.receiptNo }}</a-descriptions-item>
            <a-descriptions-item label="金额">¥{{ formatAmount(cancelTarget.receiptAmount) }}</a-descriptions-item>
          </a-descriptions>
          <a-form-item label="取消原因" required style="margin-top:16px">
            <a-textarea v-model:value="cancelReason" :rows="3" placeholder="请输入取消原因" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, InboxOutlined, PlusOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined, FileTextOutlined,
  SyncOutlined, ReloadOutlined, EyeOutlined, EditOutlined, SendOutlined,
  CheckOutlined, FileDoneOutlined, CloseOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { receiptApi } from '@/api/finance'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
  sourceType: undefined as string | undefined
})

const filterFields = [
  { key: 'keyword', label: '关键词', type: 'input' as const, placeholder: '收款单号/客户/来源单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '全部', value: undefined },
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '核销中', value: 3 },
    { label: '已核销', value: 4 },
    { label: '已完成', value: 5 },
    { label: '已取消', value: -1 }
  ]},
  { key: 'sourceType', label: '来源类型', type: 'select' as const, options: [
    { label: '全部', value: undefined },
    { label: '销售回款', value: 'SALE_ORDER' },
    { label: '采购退货退款', value: 'PURCHASE_RETURN' },
    { label: '预收转正', value: 'PRE_RECEIPT' },
    { label: '定金转正', value: 'DEPOSIT' },
    { label: '资产处置', value: 'ASSET_DISPOSAL' },
    { label: '员工退还', value: 'EMPLOYEE_REFUND' },
    { label: '其他收入', value: 'OTHER' }
  ]}
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

const statusColorMap: Record<number, string> = {
  [-1]: 'error',
  0: 'default',
  1: 'processing',
  2: 'success',
  3: 'warning',
  4: 'success',
  5: 'success'
}

const statusLabelMap: Record<number, string> = {
  [-1]: '已取消',
  0: '草稿',
  1: '待审批',
  2: '已审批',
  3: '核销中',
  4: '已核销',
  5: '已完成'
}

const receiptTypeLabelMap: Record<string, string> = {
  SALE_ORDER: '销售回款',
  PURCHASE_RETURN: '采购退货退款',
  PRE_RECEIPT: '预收转正',
  DEPOSIT: '定金转正',
  ASSET_DISPOSAL: '资产处置',
  EMPLOYEE_REFUND: '员工退还',
  OTHER: '其他收入'
}

const columns = computed(() => [
  { title: '收款单号', field: 'receiptNo', width: 160 },
  { title: '客户名称', field: 'customerName', width: 140 },
  { title: '金额', field: 'receiptAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已核销', field: 'verifiedAmount', width: 110, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '待核销', field: 'pendingAmount', width: 110, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '来源', field: 'sourceType', width: 110, align: 'center', slotName: 'receiptTypeCell' },
  { title: '状态', field: 'status', width: 90, align: 'center', slotName: 'statusCell' },
  { title: '收款日期', field: 'receiptDate', width: 110 },
  { title: '操作', type: 'action', width: 180, fixed: 'right' }
])

const stats = reactive({ totalAmount: 0, verifiedAmount: 0, pendingAmount: 0 })

// 表单
const formVisible = ref(false)
const formLoading = ref(false)
const isEditing = ref(false)
const formRef = ref<FormInstance>()
const formState = reactive<any>({
  customerName: '',
  receiptAmount: undefined,
  paymentMethod: 'bank_transfer',
  sourceType: 'SALE_ORDER',
  receiptDate: undefined,
  bankAccount: '',
  remark: ''
})

const formRules = {
  customerName: { required: true, message: '请输入客户名称', trigger: 'blur' },
  receiptAmount: { required: true, message: '请输入收款金额', trigger: 'blur' }
} as any

const initialFormSnapshot = ref('')
const formDirty = computed(() => {
  if (!formVisible.value) return false
  return JSON.stringify({ ...formState }) !== initialFormSnapshot.value
})

// 查看详情
const detailVisible = ref(false)
const detailData = ref<any>(null)

// 取消
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelTarget = ref<any>(null)
const cancelReason = ref('')

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status !== undefined) params.status = searchForm.status
    if (searchForm.sourceType) params.sourceType = searchForm.sourceType

    const res = await receiptApi.getPage(params)
    if (res.data) {
      const list = res.records || (res.data as any).list || []
      tableData.value = list
      pagination.total = res.total || 0
      lastUpdated.value = new Date().toISOString()
      if ((res.data as any).totalAmount !== undefined) {
        stats.totalAmount = (res.data as any).totalAmount
        stats.verifiedAmount = (res.data as any).totalVerifiedAmount || 0
        stats.pendingAmount = (res.data as any).totalPendingAmount || 0
      } else {
        stats.totalAmount = list.reduce((s: number, i: any) => s + (i.receiptAmount || 0), 0)
        stats.verifiedAmount = list.reduce((s: number, i: any) => s + (i.verifiedAmount || 0), 0)
        stats.pendingAmount = list.reduce((s: number, i: any) => s + (i.pendingAmount || 0), 0)
      }
    }
    hasError.value = false
  } catch (err) {
    console.warn('[收款单] 获取数据失败', err)
    message.error('获取收款单数据失败')
    tableData.value = []
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleView = (record: any) => {
  detailData.value = record
  detailVisible.value = true
}

const handleDetailClose = () => {
  detailVisible.value = false
  detailData.value = null
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.keyword = filters.keyword || ''
  searchForm.status = filters.status !== undefined ? Number(filters.status) : undefined
  searchForm.sourceType = filters.sourceType || undefined
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (_rows: any[], _ids: any[]) => {}

const handleAdd = () => {
  isEditing.value = false
  Object.assign(formState, { customerName: '', receiptAmount: undefined, paymentMethod: 'bank_transfer', sourceType: 'SALE_ORDER', receiptDate: undefined, bankAccount: '', remark: '' })
  initialFormSnapshot.value = JSON.stringify({ ...formState })
  formVisible.value = true
}

const handleEdit = (record: any) => {
  isEditing.value = true
  Object.assign(formState, {
    id: record.id,
    customerName: record.customerName,
    receiptAmount: record.receiptAmount,
    paymentMethod: record.paymentMethod || 'bank_transfer',
    sourceType: record.sourceType || 'SALE_ORDER',
    receiptDate: record.receiptDate ? dayjs(record.receiptDate) : undefined,
    bankAccount: record.bankAccount || '',
    remark: record.remark || ''
  })
  initialFormSnapshot.value = JSON.stringify({ ...formState })
  formVisible.value = true
}

const handleFormSave = async () => {
  try {
    await formRef.value?.validate()
    formLoading.value = true
    const data = { ...formState }
    if (data.receiptDate && dayjs.isDayjs(data.receiptDate)) {
      data.receiptDate = data.receiptDate.format('YYYY-MM-DD')
    }
    if (isEditing.value) {
      await receiptApi.update(data.id, data)
      message.success('更新成功')
    } else {
      await receiptApi.create(data)
      message.success('创建成功')
    }
    formVisible.value = false
    fetchData()
  } catch (err) {
    if (err && (err as any).errorFields) return
    console.warn('[收款单] 保存失败', err)
    message.error('保存收款单失败')
  } finally {
    formLoading.value = false
  }
}

const handleFormClose = () => {
  formVisible.value = false
  formRef.value?.resetFields()
}

const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '确认提交',
    content: `确定提交收款单 "${record.receiptNo}" 审批吗？`,
    async onOk() {
      await receiptApi.submit(record.id)
      message.success('已提交审批')
      fetchData()
    }
  })
}

const handleApprove = (record: any) => {
  Modal.confirm({
    title: '确认审批通过',
    content: `确定审批通过收款单 "${record.receiptNo}" 吗？`,
    async onOk() {
      await receiptApi.approve(record.id)
      message.success('审批通过')
      fetchData()
    }
  })
}

const handleComplete = (record: any) => {
  Modal.confirm({
    title: '确认完成',
    content: `确定完成收款单 "${record.receiptNo}" 吗？`,
    async onOk() {
      await receiptApi.complete(record.id)
      message.success('收款已完成')
      fetchData()
    }
  })
}

const handleCancel = (record: any) => {
  cancelTarget.value = record
  cancelReason.value = ''
  cancelVisible.value = true
}

const handleCancelConfirm = async () => {
  if (!cancelReason.value.trim()) {
    message.warning('请输入取消原因')
    return
  }
  cancelLoading.value = true
  try {
    await receiptApi.cancel(cancelTarget.value.id, cancelReason.value)
    message.success('收款单已取消')
    cancelVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[收款单] 取消失败', err)
    message.error('取消失败')
  } finally {
    cancelLoading.value = false
  }
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function isInput(el: Element | null): boolean {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (el as HTMLElement)?.isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleParentCreate() { handleAdd() }

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onBeforeRouteLeave((to, from, next) => {
  if (formVisible.value && formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的修改，确定要离开吗？',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => { fetchData(); autoRefreshCountdown.value = 30 }, 30000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.rcpt-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.rcpt-page-header-left { display: flex; align-items: center; gap: 12px; }
.rcpt-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.rcpt-page-header-right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.finance-rcpt-page { padding: 16px; height: 100%; display: flex; flex-direction: column; overflow: hidden; min-height: 0; gap: 16px; }
.finance-rcpt-page > :deep(.vxe-table-list-container) { flex: 1; min-height: 0; }
.stat-cards { display: flex; gap: 12px; padding: 16px; background: #fff; border-radius: 8px; }
.stat-card { flex: 1; display: flex; justify-content: space-between; align-items: center; padding: 14px; border-radius: 8px; }
.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-verified { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-card-value { font-size: 18px; font-weight: 600; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; color: #333; }
.stat-card-label { font-size: 12px; color: #666; margin-top: 4px; }
.stat-card-icon { font-size: 24px; color: rgba(0, 0, 0, 0.15); }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }
.table-empty-action { margin-top: 12px; }
.list-update-timestamp { font-size: 12px; color: var(--color-text-tertiary, #bbb); white-space: nowrap; cursor: help; margin-left: 8px; line-height: 32px; vertical-align: middle; }
.action-cell-inner { display: inline-flex; align-items: center; }
:deep(.ant-table-thead > tr > th) { padding: 6px 8px !important; font-size: 12px; }
:deep(.ant-table-tbody > tr > td) { padding: 4px 8px !important; font-size: 12px; }
:deep(.ant-card-body) { padding: 12px; }
:deep(.ant-form-item) { margin-bottom: 8px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
