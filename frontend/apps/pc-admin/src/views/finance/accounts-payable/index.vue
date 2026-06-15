<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="accounts-payable-header">
        <div class="accounts-payable-header-left">
          <a-breadcrumb class="accounts-payable-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>应付账款</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="accounts-payable-header-title">应付账款</h2>
        </div>
        <div class="accounts-payable-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <div class="accounts-payable-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">应付总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-paid">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.paidAmount) }}</div>
            <div class="stat-card-label">已付金额</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-unpaid">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.unpaidAmount) }}</div>
            <div class="stat-card-label">未付金额</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">应付笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :min-empty-rows="12"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :show-search="false"
        :show-export="true"
        :selectable="true"
        add-text="新增应付"
        add-permission="finance:payable:create"
        export-permission="finance:payable:export"
        @add="handleAdd"
        @refresh="fetchData"
        @cell-dblclick="handleView"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @export="handleExport"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>

        <template #batch-actions>
          <!-- batch actions placeholder -->
        </template>

        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="debounceClick('refresh', fetchData)" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else>
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">
                没有符合条件的应付记录，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无应付账款数据，点击右上角「新增应付」开始创建
              </p>
            </template>
          </div>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" v-permission="'finance:payable:view'" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton :record="record" :business-id="record.id" business-type="payable" button-type="link" button-size="small" tooltip="打印" />
            <a-tooltip v-if="record.status !== 2" title="付款">
              <a-button type="link" size="small" v-permission="'finance:payable:payment'" @click="handlePayment(record)">
                <template #icon><DollarOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }: any) => handleActionMenuClick(key as string, record)">
                  <a-menu-item key="paymentHistory">
                    <HistoryOutlined /> 付款记录
                  </a-menu-item>
                  <a-menu-item key="reminder">
                    <BellOutlined /> 提醒付款
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 详情弹窗 -->
      <FullScreenDetail
        :visible="detailVisible"
        title="应付账款详情"
        @close="detailVisible = false"
      >
        <a-descriptions bordered :column="2" v-if="currentRecord">
          <a-descriptions-item label="供应商名称">
            <a @click="handleViewSupplier(currentRecord)">{{ currentRecord.supplierName }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="订单号">
            <a @click="handleViewOrder(currentRecord)">{{ currentRecord.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="应付金额">
            <span class="amount-cell">¥{{ formatAmount(currentRecord.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="已付金额">
            <span class="amount-cell success">¥{{ formatAmount(currentRecord.paidAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="未付金额">
            <span class="amount-cell warning">¥{{ formatAmount(currentRecord.unpaidAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="到期日期">
            <span :class="{ 'overdue': isOverdue(currentRecord.dueDate, currentRecord.status) }">
              {{ currentRecord.dueDate }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentRecord.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 付款记录 -->
        <div class="detail-payment-section">
          <div class="detail-payment-title">付款记录</div>
          <VxeTableList
            :columns="paymentColumns"
            :data-source="paymentRecords"
            :pagination="false as any"
            row-key="id"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #amountCell="{ record }">
              <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
            </template>
          </VxeTableList>
        </div>

        <div style="margin-top:16px;text-align:right">
          <a-button v-if="currentRecord?.status !== 2" type="primary" v-permission="'finance:payable:payment'" @click="handlePayment(currentRecord)">
            <template #icon><DollarOutlined /></template>
            付款
          </a-button>
        </div>
      </FullScreenDetail>

      <!-- 新增应付 -->
      <FullScreenDetail
        :visible="addModalVisible"
        title="新增应付账款"
        :save-loading="addSubmitting"
        :dirty="addFormDirty"
        show-save-and-new
        @save="handleAddConfirm(false)"
        @save-and-new="handleAddConfirm(true)"
        @close="handleAddCancel"
      >
        <a-form ref="addFormRef" :model="addFormState" :rules="addFormRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
          <a-form-item label="供应商" name="supplierId">
            <a-select
              v-model:value="addFormState.supplierId"
              placeholder="请选择供应商"
              size="small"
              show-search
              :filter-option="(input: string, option: any) => option.children?.toLowerCase().includes(input.toLowerCase())"
            >
              <a-select-option v-for="s in supplierOptions" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="应付金额" name="amount">
            <a-input-number v-model:value="addFormState.amount" :min="0" :precision="2" size="small" style="width:100%">
              <template #addonBefore>¥</template>
            </a-input-number>
          </a-form-item>
          <a-form-item label="到期日期" name="dueDate">
            <a-date-picker v-model:value="addFormState.dueDate" size="small" style="width:100%" />
          </a-form-item>
          <a-form-item label="订单号" name="orderNo">
            <a-input v-model:value="addFormState.orderNo" placeholder="关联订单号（可选）" size="small" />
          </a-form-item>
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="addFormState.remark" placeholder="备注信息" :rows="3" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 付款弹窗 -->
      <FullScreenDetail
        :visible="paymentModalVisible"
        title="付款"
        :save-loading="paymentSubmitting"
        @save="handlePaymentConfirm"
        @close="paymentModalVisible = false"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
          <a-form-item label="供应商">
            <span>{{ paymentRecord?.supplierName }}</span>
          </a-form-item>
          <a-form-item label="未付金额">
            <span class="amount-cell warning">¥{{ formatAmount(paymentRecord?.unpaidAmount) }}</span>
          </a-form-item>
          <a-form-item label="付款金额" required>
            <a-input-number
              v-model:value="paymentAmount"
              :min="0"
              :max="paymentRecord?.unpaidAmount"
              :precision="2"
              size="small"
              style="width: 100%"
            >
              <template #addonBefore>¥</template>
            </a-input-number>
          </a-form-item>
          <a-form-item label="付款方式">
            <a-select v-model:value="paymentMethod" placeholder="选择付款方式" size="small">
              <a-select-option :value="1">银行转账</a-select-option>
              <a-select-option :value="2">现金</a-select-option>
              <a-select-option :value="3">承兑汇票</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="付款日期">
            <a-date-picker v-model:value="paymentDate" size="small" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="paymentRemark" placeholder="备注信息" size="small" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  WarningOutlined, EyeOutlined, DollarOutlined, SearchOutlined, InboxOutlined,
  EllipsisOutlined, CheckCircleOutlined, ExclamationCircleOutlined,
  FileTextOutlined, ReloadOutlined, SyncOutlined, HistoryOutlined, BellOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { payableApi } from '@/api/finance'
import request from '@/utils/request'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const lastUpdateTime = ref('')
const lastUpdated = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)

const addModalVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref()
const addFormState = reactive({
  supplierId: undefined as number | undefined,
  amount: undefined as number | undefined,
  dueDate: undefined as any,
  orderNo: '',
  remark: ''
})
const addFormRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  amount: [{ required: true, message: '请输入应付金额', trigger: 'blur' }],
  dueDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }]
} as any
const supplierOptions = ref<{ id: number; name: string }[]>([])
const initialAddSnapshot = ref('')

const addFormDirty = computed(() => {
  if (!addModalVisible.value) return false
  return JSON.stringify({ ...addFormState }) !== initialAddSnapshot.value
})

const queryParams = reactive({
  supplierName: '',
  orderNo: '',
  status: undefined as number | undefined
})

const stats = reactive({
  totalAmount: 0,
  paidAmount: 0,
  unpaidAmount: 0
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const hasActiveFilters = computed(() => {
  return Object.values(queryParams).some(v => v !== undefined && v !== null && v !== '')
})

const filterFields = [
  { key: 'supplierName', label: '供应商名称', type: 'input' as const, placeholder: '请输入供应商名称' },
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未付款', value: 0 },
    { label: '部分付款', value: 1 },
    { label: '已付款', value: 2 }
  ]}
]

const columns = computed(() => [
  { title: '供应商名称', field: 'supplierName', width: 150 },
  { title: '订单号', field: 'orderNo', width: 150 },
  { title: '应付金额', field: 'amount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已付金额', field: 'paidAmount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '未付金额', field: 'unpaidAmount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '状态', field: 'status', width: 100, align: 'center', slotName: 'statusCell' },
  { title: '到期日期', field: 'dueDate', width: 120 },
  { title: '备注', field: 'remark', minWidth: 100 },
  { title: '操作', type: 'action', width: 150, fixed: 'right' }
])

const paymentColumns = [
  { title: '付款日期', field: 'paymentDate', width: 120 },
  { title: '付款金额', field: 'amount', width: 120, align: 'right', slotName: 'amountCell' },
  { title: '付款方式', field: 'paymentMethod', width: 100 },
  { title: '备注', field: 'remark' }
]

const paymentRecords = ref<any[]>([])
const paymentModalVisible = ref(false)
const paymentSubmitting = ref(false)
const paymentRecord = ref<any>(null)
const paymentAmount = ref(0)
const paymentMethod = ref(1)
const paymentDate = ref<any>(dayjs())
const paymentRemark = ref('')

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'warning', 1: 'processing', 2: 'success' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '未付款', 1: '部分付款', 2: '已付款' }
  return texts[status] || '未知'
}

const isOverdue = (dueDate: string, status: number) => {
  if (status === 2) return false
  return new Date(dueDate) < new Date()
}

function handleParentCreate() {
  handleAdd()
}

const handleAdd = async () => {
  addFormState.supplierId = undefined
  addFormState.amount = undefined
  addFormState.dueDate = undefined
  addFormState.orderNo = ''
  addFormState.remark = ''
  initialAddSnapshot.value = JSON.stringify({ ...addFormState })
  // 加载供应商选项
  try {
    const res = await request.get('/supplier/list')
    supplierOptions.value = (Array.isArray(res) ? res : res?.data?.records || res?.data || []).map((s: any) => ({ id: s.id, name: s.name || s.supplierName }))
  } catch { supplierOptions.value = [] }
  addModalVisible.value = true
}

const handleAddConfirm = async (stay: boolean) => {
  try {
    await addFormRef.value?.validate()
  } catch { return }
  addSubmitting.value = true
  try {
    await request.post('/finance/payable/create', {
      supplierId: addFormState.supplierId,
      amount: addFormState.amount,
      dueDate: addFormState.dueDate?.format?.('YYYY-MM-DD') || addFormState.dueDate,
      orderNo: addFormState.orderNo || undefined,
      remark: addFormState.remark
    })
    message.success('新增应付账款成功')
    if (stay) {
      addFormState.supplierId = undefined
      addFormState.amount = undefined
      addFormState.dueDate = undefined
      addFormState.orderNo = ''
      addFormState.remark = ''
      initialAddSnapshot.value = JSON.stringify({ ...addFormState })
      addFormRef.value?.resetFields()
    } else {
      addModalVisible.value = false
    }
    fetchData()
  } catch (err) {
    console.warn('[应付账款] 新增失败', err)
    message.error('新增失败')
  } finally {
    addSubmitting.value = false
  }
}

const handleAddCancel = () => {
  addModalVisible.value = false
}

const handleView = (record: any) => {
  currentRecord.value = record
  paymentRecords.value = record.paymentHistory || []
  detailVisible.value = true
}

const handleViewSupplier = (record: any) => {
  router.push(`/supplier/detail/${record.supplierId}`)
}

const handleViewOrder = (record: any) => {
  if (record.orderId) {
    router.push(`/purchase/order/${record.orderId}`)
  }
}

const handlePayment = (record: any) => {
  paymentRecord.value = record
  paymentAmount.value = record.unpaidAmount || 0
  paymentMethod.value = 1
  paymentDate.value = dayjs()
  paymentRemark.value = ''
  paymentModalVisible.value = true
}

const handlePaymentConfirm = async () => {
  if (!paymentAmount.value || paymentAmount.value <= 0) {
    message.warning('请输入付款金额')
    return
  }
  paymentSubmitting.value = true
  try {
    await request.post('/finance/payable/payment', {
      id: paymentRecord.value.id,
      amount: paymentAmount.value,
      method: paymentMethod.value,
      date: paymentDate.value?.format('YYYY-MM-DD'),
      remark: paymentRemark.value
    })
    message.success('付款成功')
    paymentModalVisible.value = false
    detailVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[应付账款] 付款失败', err)
    message.error('付款失败')
  } finally {
    paymentSubmitting.value = false
  }
}

const handleActionMenuClick = (key: string, record: any) => {
  switch (key) {
    case 'paymentHistory': handleView(record); break
    case 'reminder': message.info(`已发送付款提醒给 ${record.supplierName}`); break
  }
}

const handleResetFilters = () => {
  Object.keys(queryParams).forEach(k => { (queryParams as any)[k] = undefined })
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  queryParams.supplierName = filters.supplierName || ''
  queryParams.orderNo = filters.orderNo || ''
  queryParams.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: any[], ids: any[]) => {
  // 可以在这里处理选中行的逻辑，例如批量操作
}

const handleExport = async () => {
  try {
    const res = await request.get('/finance/payable/export', {
      supplierName: queryParams.supplierName || undefined,
      status: queryParams.status
    })
    const list = res.data || res.rows || []
    if (!list.length) { message.info('没有可导出的数据'); return }
    const headers = ['供应商名称', '订单号', '应付金额', '已付金额', '未付金额', '状态', '到期日期', '备注']
    const rows = list.map((r: any) => [
      r.supplierName || '', r.orderNo || '', formatAmount(r.amount), formatAmount(r.paidAmount),
      formatAmount(r.unpaidAmount), getStatusText(r.status), r.dueDate || '', r.remark || ''
    ])
    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `应付账款_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${list.length} 条`)
  } catch (err) {
    console.warn('[应付账款] 导出失败', err)
    message.error('导出失败')
  }
}

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const res = await request.post('/finance/payable/list', {
      supplierName: queryParams.supplierName,
      status: queryParams.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data?.records) {
      dataSource.value = res.records
      pagination.total = res.total || 0
      lastUpdated.value = new Date().toISOString()
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
      // 优先使用后端返回的汇总数据
      if (res.totalAmount !== undefined) {
        stats.totalAmount = res.totalAmount
        stats.paidAmount = res.totalPaidAmount || 0
        stats.unpaidAmount = res.totalUnpaidAmount || 0
      } else {
        stats.totalAmount = dataSource.value.reduce((sum, item) => sum + item.amount, 0)
        stats.paidAmount = dataSource.value.reduce((sum, item) => sum + item.paidAmount, 0)
        stats.unpaidAmount = dataSource.value.reduce((sum, item) => sum + item.unpaidAmount, 0)
      }
    } else {
      console.warn('应付账款列表接口返回数据结构异常', res.data)
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    hasError.value = false
  } catch (err) {
    console.warn('获取应付账款数据失败', err)
    message.error('获取数据失败')
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
  }
}

function isInput(target: Element | null): boolean {
  if (!target) return false
  const tag = target.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (target as HTMLElement).isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchData()
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
  document.addEventListener('keydown', handleKeydown)
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
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.accounts-payable-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.accounts-payable-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

.accounts-payable-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.accounts-payable-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.accounts-payable-breadcrumb {
  font-size: 13px;
}
.accounts-payable-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.accounts-payable-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
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

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-paid { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-unpaid { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 空状态 */
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

.table-empty-action {
  margin-top: 12px;
}

.supplier-link {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.success {
  color: #52c41a;
}

.amount-cell.warning {
  color: #faad14;
}

.overdue {
  color: #ff4d4f;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

/* 详情弹窗 */
.detail-payment-section {
  margin-top: 16px;
}

.detail-payment-title {
  font-weight: 500;
  margin-bottom: 8px;
}
.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}
/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
}

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
