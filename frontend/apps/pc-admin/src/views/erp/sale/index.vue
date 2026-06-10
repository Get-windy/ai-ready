<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="销售订单管理" full-height>
      <!-- 状态栏：数据时效感 -->
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : (hasError ? 'error' : 'success')" />
            <span v-if="lastUpdateTime" class="update-time">
              最后更新: {{ lastUpdateTime }}
            </span>
            <span v-if="hasError" class="error-text">数据加载异常</span>
          </span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-tooltip title="自动刷新 (每30秒)">
            <a-switch v-model:checked="autoRefresh" size="small" />
          </a-tooltip>
          <a-tooltip title="手动刷新">
            <a-button size="small" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </template>

    <!-- 筛选区 -->
    <template #filter>
      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />
    </template>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-draft">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ draftCount }}</div>
          <div class="stat-card-label">草稿</div>
        </div>
        <FileOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pendingCount }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ completedCount }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalAmount) }}</div>
          <div class="stat-card-label">订单金额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- 表格列表 -->
    <VxeTableList
      ref="tableRef"
      table-key="sales-order-list"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      :show-export="true"
      :show-summary="true"
      :summary-data="summaryData"
      add-text="新增"
      @add="handleAdd"
      @refresh="fetchData"
      @export="handleExport"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
      @cell-dblclick="handleView"
    >
      <template #empty>
        <EmptyState
          v-if="hasError"
          image="error"
          title="数据加载异常"
          description="数据获取失败，请检查网络后重试"
          :show-add="false"
          :show-refresh="true"
          :show-actions="true"
          size="small"
          @refresh="fetchData"
        />
        <EmptyState
          v-else
          image="no-data"
          title="暂无销售订单"
          description="当前没有销售订单数据，可点击新增创建"
          :show-add="true"
          :show-refresh="true"
          :show-actions="true"
          size="small"
          add-text="新增订单"
          @add="handleAdd"
          @refresh="fetchData"
        />
      </template>
      <!-- 操作列自定义 -->
      <template #action="{ record }">
        <a-space :size="4">
          <!-- 提交审批 -->
          <a-tooltip v-if="record.status === OrderStatus.DRAFT" title="提交审批 (仅草稿可提交)">
            <a-button type="link" size="small" @click="handleSubmit(record)">
              提交
            </a-button>
          </a-tooltip>
          <a-tooltip v-else-if="record.status > OrderStatus.DRAFT" title="已提交，不可重复提交">
            <a-button type="link" size="small" disabled>提交</a-button>
          </a-tooltip>
          <!-- 审批 -->
          <a-tooltip v-if="record.status === OrderStatus.PENDING" title="审批订单 (仅待审批可操作)">
            <a-button type="link" size="small" @click="handleApprove(record)">
              审批
            </a-button>
          </a-tooltip>
          <a-tooltip v-else-if="record.status !== OrderStatus.PENDING" title="非待审批状态，不可审批">
            <a-button type="link" size="small" disabled>审批</a-button>
          </a-tooltip>
          <!-- 打印 -->
          <PrintButton
            v-if="record.status >= OrderStatus.APPROVED"
            template-type="order"
            :business-id="record.id"
            business-type="sales_order"
            button-text="打印"
            button-size="small"
            @print-success="() => message.success(`订单 ${record.orderNo} 打印成功`)"
            @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
          />
          <a-tooltip v-else title="需审批后才可打印">
            <a-button type="link" size="small" disabled>打印</a-button>
          </a-tooltip>
          <!-- 取消（使用 Modal.confirm 替代 a-popconfirm） -->
          <a-tooltip v-if="record.status < OrderStatus.COMPLETED && record.status !== OrderStatus.CANCELLED" title="取消订单 (未完成状态可取消)">
            <a-button type="link" size="small" danger @click="handleCancelClick(record)">取消</a-button>
          </a-tooltip>
          <a-tooltip v-else title="已完成或已取消，不可操作">
            <a-button type="link" size="small" disabled danger>取消</a-button>
          </a-tooltip>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-drawer
      v-model:open="detailVisible"
      title="销售订单详情"
      placement="right"
      width="80vw"
      :footer="null"
      destroy-on-close
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="订单号">{{ detailData.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="客户">{{ detailData.customerName }}</a-descriptions-item>
          <a-descriptions-item label="订单日期">{{ detailData.orderDate }}</a-descriptions-item>
          <a-descriptions-item label="交货日期">{{ detailData.deliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="销售员">{{ detailData.salesperson }}</a-descriptions-item>
          <a-descriptions-item label="订单金额">
            <span class="currency-value">¥{{ detailData.totalAmount?.toFixed(2) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="折扣金额">
            <span class="currency-value">¥{{ detailData.discountAmount?.toFixed(2) || '0.00' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="税额">
            <span class="currency-value">¥{{ detailData.taxAmount?.toFixed(2) || '0.00' }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="最终金额">
            <span class="currency-value" style="font-weight: 600; color: #1890ff;">
              ¥{{ detailData.finalAmount?.toFixed(2) || detailData.totalAmount?.toFixed(2) }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="SALES_ORDER_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ detailData.updateTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 订单明细 -->
        <a-divider>订单明细</a-divider>
        <VxeTableList
          v-if="detailData?.details && detailData.details.length > 0"
          :columns="detailColumns"
          :data-source="detailData.details"
        :pagination="false"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #unitPriceCell="{ record }">
          <span class="currency-value">¥{{ record.unitPrice?.toFixed(2) }}</span>
        </template>
        <template #totalAmountCell="{ record }">
          <span class="currency-value">¥{{ record.totalAmount?.toFixed(2) }}</span>
        </template>
        <template #taxAmountCell="{ record }">
          <span class="currency-value">¥{{ record.taxAmount?.toFixed(2) }}</span>
        </template>
      </VxeTableList>
      <EmptyState v-else image="no-data" title="暂无订单明细" description="该订单暂无明细数据" :show-actions="false" size="small" />
      </a-spin>
    </a-drawer>

    <!-- 新建/编辑表单弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑销售订单' : '新建销售订单'"
      :width="600"
      :confirm-loading="formLoading"
      destroy-on-close
      @ok="handleFormSubmit"
      @cancel="handleFormCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="客户名称" name="customerId">
          <a-select
            size="small"
            v-model:value="formData.customerId"
            placeholder="请选择客户"
            show-search
            :filter-option="filterCustomerOption"
          >
            <a-select-option v-for="c in customerOptions" :key="c.id" :value="c.id">
              {{ c.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="订单日期" name="orderDate">
          <a-date-picker size="small" v-model:value="formData.orderDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="交货日期">
          <a-date-picker size="small" v-model:value="formData.deliveryDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="销售员" name="salesperson">
          <a-input size="small" v-model:value="formData.salesperson" placeholder="请输入销售员" />
        </a-form-item>
        <a-form-item label="订单金额" name="totalAmount">
          <a-input-number size="small"
            v-model:value="formData.totalAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入金额"
          />
        </a-form-item>
        <a-form-item label="折扣金额">
          <a-input-number size="small"
            v-model:value="formData.discountAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入折扣金额"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea size="small" v-model:value="formData.remark" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量编辑弹窗 -->
    <a-modal
      v-model:open="batchEditVisible"
      title="批量编辑"
      :width="500"
      :confirm-loading="batchEditLoading"
      destroy-on-close
      @ok="handleBatchEditSubmit"
    >
      <a-alert
        message="将批量修改以下订单"
        :description="`已选择 ${selectedIds.length} 个订单`"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="销售员">
          <a-input size="small" v-model:value="batchEditData.salesperson" placeholder="批量修改销售员" />
        </a-form-item>
        <a-form-item label="交货日期">
          <a-date-picker size="small" v-model:value="batchEditData.deliveryDate" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined, FileOutlined, ClockCircleOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import { salesOrderApi, OrderStatus, type SalesOrder } from '@/api/order'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { useUserStore } from '@/stores/user'
import { requiredSelectRule, requiredRule } from '@/utils/formRules'
import { SALES_ORDER_STATUS } from '@/utils/statusConfig'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); handleExport(); return }
}

// ── 状态定义 ────────────────────────────────────────

const userStore = useUserStore()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<SalesOrder[]>([])
const tableRef = ref()
const lastUpdateTime = ref<string>('')
const autoRefresh = ref(false)
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => tableData.value.filter(r => r.status === OrderStatus.DRAFT).length)
const pendingCount = computed(() => tableData.value.filter(r => r.status === OrderStatus.PENDING).length)
const completedCount = computed(() => tableData.value.filter(r => r.status === OrderStatus.COMPLETED).length)
const totalAmount = computed(() => tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0))

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const detailVisible = ref(false)
const detailData = ref<SalesOrder | null>(null)
const detailLoading = ref(false)
const currentRecord = ref<SalesOrder | null>(null)

const formVisible = ref(false)
const formLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const batchEditVisible = ref(false)
const batchEditLoading = ref(false)
const selectedIds = ref<number[]>([])
const selectedRows = ref<SalesOrder[]>([])

const customerOptions = ref<{ id: number; name: string }[]>([])

// ── 搜索配置 ────────────────────────────────────────

const searchFields: SearchField[] = [
  { name: 'orderNo', label: '订单号', type: 'input', placeholder: '请输入订单号' },
  { name: 'customerId', label: '客户', type: 'select', placeholder: '请选择客户', options: [] },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [] },
  { name: 'salesperson', label: '销售员', type: 'input', placeholder: '请输入销售员' },
  { name: 'orderDateRange', label: '订单日期', type: 'date-range' }
]

const filterFields: FilterField[] = [
  { key: 'orderNo', label: '订单号', type: 'input', placeholder: '请输入订单号', span: 6 },
  { key: 'customerId', label: '客户', type: 'select', placeholder: '请选择客户', span: 6 },
  { key: 'status', label: '状态', type: 'select', placeholder: '请选择状态', span: 6 }
]

const searchParams = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  status: undefined as number | undefined,
  salesperson: '',
  startDate: '',
  endDate: ''
})

// ── 分页配置 ────────────────────────────────────────

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条记录`
})

// ── 表格列配置 ────────────────────────────────────────

const vxeColumns = computed(() => [
  { field: 'orderNo', title: '订单号', width: 140 },
  { field: 'customerName', title: '客户名称', width: 160, showOverflow: 'tooltip' },
  { field: 'orderDate', title: '订单日期', width: 110 },
  { field: 'deliveryDate', title: '交货日期', width: 110 },
  { field: 'salesperson', title: '销售员', width: 100 },
  { field: 'totalAmount', title: '订单金额', width: 120, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'discountAmount', title: '折扣', width: 90, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'taxAmount', title: '税额', width: 90, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'finalAmount', title: '最终金额', width: 120, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => SALES_ORDER_STATUS[cellValue]?.text || '' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 180, fixed: 'right' },
])

// ── 订单明细列配置 ────────────────────────────────────────

const detailColumns = [
  { title: '产品编码', field: 'productCode', width: 120 },
  { title: '产品名称', field: 'productName', width: 180 },
  { title: '数量', field: 'quantity', width: 80, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 100, align: 'right', slotName: 'unitPriceCell' },
  { title: '折扣', field: 'discount', width: 80, align: 'right' },
  { title: '税率', field: 'taxRate', width: 80, align: 'right' },
  { title: '税额', field: 'taxAmount', width: 100, align: 'right', slotName: 'taxAmountCell' },
  { title: '金额', field: 'totalAmount', width: 120, align: 'right', slotName: 'totalAmountCell' },
  { title: '备注', field: 'remark', width: 150 }
]

// ── 汇总数据 ────────────────────────────────────────

const summaryData = computed(() => {
  if (tableData.value.length === 0) return []
  const totalAmount = tableData.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0)
  const finalAmount = tableData.value.reduce((sum, r) => sum + (r.finalAmount || r.totalAmount || 0), 0)
  return [
    { label: '订单金额', value: totalAmount, type: 'currency' },
    { label: '最终金额', value: finalAmount, type: 'currency' }
  ]
})

// ── 表单配置 ────────────────────────────────────────

const formRules = {
  customerId: [requiredSelectRule('客户')],
  orderDate: [requiredRule('订单日期')],
  salesperson: [requiredRule('销售员')],
  totalAmount: [requiredRule('订单金额')]
}

const formData = reactive({
  customerId: undefined as number | undefined,
  orderDate: undefined as any,
  deliveryDate: undefined as any,
  salesperson: '',
  totalAmount: undefined as number | undefined,
  discountAmount: undefined as number | undefined,
  remark: ''
})

const batchEditData = reactive({
  salesperson: '',
  deliveryDate: undefined as any
})

// ── 数据加载 ────────────────────────────────────────

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await salesOrderApi.getPage({
      tenantId: userStore.tenantId,
      ...searchParams,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售订单] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// 错误处理
const handleError = (error: Error) => {
  hasError.value = true
  console.warn('[销售订单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// 离开拦截（表单未保存时提醒）
const hasUnsavedChanges = computed(() => {
  return formVisible.value && formRef.value?.isFieldTouched?.()
})

onBeforeRouteLeave((to, from, next) => {
  if (hasUnsavedChanges.value) {
    Modal.confirm({
      title: '离开确认',
      content: '当前表单有未保存的更改，确定要离开吗？',
      okText: '离开',
      cancelText: '取消',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

// 自动刷新
watch(autoRefresh, (enabled) => {
  if (enabled) {
    autoRefreshCountdown.value = 30
    refreshTimer = setInterval(() => {
      fetchData()
      autoRefreshCountdown.value = 30
    }, 30000) // 每30秒刷新
    countdownTimer = setInterval(() => {
      if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
    }, 1000)
  } else {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
    if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener("erp:create", handleParentCreate)
  window.removeEventListener("erp:refresh", fetchData)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

// ── 事件处理 ────────────────────────────────────────

const handleSearch = (values: Record<string, any>) => {
  Object.assign(searchParams, {
    orderNo: values.orderNo || '',
    customerId: values.customerId,
    status: values.status,
    salesperson: values.salesperson || '',
    startDate: values.orderDateRange?.[0] || '',
    endDate: values.orderDateRange?.[1] || ''
  })
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchParams, {
    orderNo: '',
    customerId: undefined,
    status: undefined,
    salesperson: '',
    startDate: '',
    endDate: ''
  })
  pagination.current = 1
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  Object.assign(searchParams, filters)
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleSelectionChange = (rows: SalesOrder[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

// ── 单条操作 ────────────────────────────────────────

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await salesOrderApi.getById(id)
    detailData.value = res.data || null
  } catch (error: any) {
    console.warn('[销售订单] 获取详情失败', error)
    message.error(error?.response?.data?.message || '获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleView = async (record: SalesOrder) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

function handleParentCreate() { handleAdd() }

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  Object.assign(formData, {
    customerId: undefined,
    orderDate: undefined,
    deliveryDate: undefined,
    salesperson: '',
    totalAmount: undefined,
    discountAmount: undefined,
    remark: ''
  })
  formVisible.value = true
}

const handleEdit = (record: SalesOrder) => {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    customerId: record.customerId,
    orderDate: record.orderDate,
    deliveryDate: record.deliveryDate,
    salesperson: record.salesperson,
    totalAmount: record.totalAmount,
    discountAmount: record.discountAmount,
    remark: record.remark
  })
  formVisible.value = true
}

const handleDelete = async (record: SalesOrder) => {
  try {
    await salesOrderApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error: any) {
    console.warn('[销售订单] 删除失败', error)
    message.error(error?.response?.data?.message || '删除失败')
  }
}

const handleSubmit = async (record: SalesOrder) => {
  try {
    await salesOrderApi.submit(record.id)
    message.success('提交成功，等待审批')
    fetchData()
  } catch (error: any) {
    console.warn('[销售订单] 提交失败', error)
    message.error(error?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async (record: SalesOrder) => {
  Modal.confirm({
    title: '审批订单',
    content: `确定审批订单 ${record.orderNo} 吗？`,
    okText: '通过',
    cancelText: '拒绝',
    onOk: async () => {
      try {
        await salesOrderApi.approve(record.id)
        message.success('审批通过')
        fetchData()
      } catch (error: any) {
        console.warn('[销售订单] 审批失败', error)
        message.error(error?.response?.data?.message || '审批失败')
      }
    },
    onCancel: async () => {
      Modal.confirm({
        title: '拒绝订单',
        content: '请输入拒绝原因',
        okText: '确认拒绝',
        cancelText: '取消',
        onOk: async () => {
          try {
            await salesOrderApi.reject(record.id, '不符合要求')
            message.success('已拒绝')
            fetchData()
          } catch (error: any) {
            console.warn('[销售订单] 拒绝失败', error)
            message.error(error?.response?.data?.message || '操作失败')
          }
        }
      })
    }
  })
}

const handleCancelClick = (record: SalesOrder) => {
  Modal.confirm({
    title: '取消订单',
    content: `确定要取消订单 ${record.orderNo} 吗？此操作不可恢复。`,
    okText: '确认取消',
    okType: 'danger',
    cancelText: '保留订单',
    onOk: () => handleCancel(record)
  })
}

const handleCancel = async (record: SalesOrder) => {
  try {
    await salesOrderApi.cancel(record.id)
    message.success('订单已取消')
    fetchData()
  } catch (error: any) {
    console.warn('[销售订单] 取消失败', error)
    message.error(error?.response?.data?.message || '取消失败')
  }
}

// ── 批量操作 ────────────────────────────────────────

const handleBatchDelete = async (ids: number[]) => {
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${ids.length} 个订单吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await salesOrderApi.batchDelete(ids)
        message.success(`成功删除 ${ids.length} 个订单`)
        tableRef.value?.clearSelection()
        fetchData()
      } catch (error: any) {
        console.warn('[销售订单] 批量删除失败', error)
        message.error(error?.response?.data?.message || '批量删除失败')
      }
    }
  })
}

const handleBatchEdit = (ids: number[]) => {
  selectedIds.value = ids
  Object.assign(batchEditData, { salesperson: '', deliveryDate: undefined })
  batchEditVisible.value = true
}

const handleBatchEditSubmit = async () => {
  batchEditLoading.value = true
  try {
    // 批量更新逻辑（实际项目中需要后端支持）
    for (const id of selectedIds.value) {
      const updateData: Partial<SalesOrder> = {}
      if (batchEditData.salesperson) updateData.salesperson = batchEditData.salesperson
      if (batchEditData.deliveryDate) updateData.deliveryDate = batchEditData.deliveryDate
      if (Object.keys(updateData).length > 0) {
        await salesOrderApi.update(id, updateData)
      }
    }
    message.success(`成功修改 ${selectedIds.value.length} 个订单`)
    batchEditVisible.value = false
    tableRef.value?.clearSelection()
    fetchData()
  } catch (error: any) {
    console.warn('[销售订单] 批量修改失败', error)
    message.error(error?.response?.data?.message || '批量修改失败')
  } finally {
    batchEditLoading.value = false
  }
}

// ── 导出 ────────────────────────────────────────

const handleExport = async () => {
  const hide = message.loading('正在导出...', 0)
  try {
    const res = await salesOrderApi.export({
      tenantId: userStore.tenantId,
      ...searchParams
    })
    if (res.data) {
      // 前端生成 CSV
      const data = res.data
      const headers = ['订单号', '客户', '订单日期', '交货日期', '销售员', '金额', '状态']
      const rows = data.map((r: SalesOrder) => [
        r.orderNo, r.customerName, r.orderDate, r.deliveryDate || '',
        r.salesperson, r.totalAmount, SALES_ORDER_STATUS[r.status]?.text || ''
      ])
      const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `销售订单_${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      window.URL.revokeObjectURL(url)
      message.success('导出成功')
    }
  } catch (error: any) {
    console.warn('[销售订单] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    hide()
  }
}

// ── 表单提交 ────────────────────────────────────────

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
    formLoading.value = true
    if (isEdit.value && editingId.value) {
      await salesOrderApi.update(editingId.value, formData as Partial<SalesOrder>)
      message.success('更新成功')
    } else {
      await salesOrderApi.create({ tenantId: userStore.tenantId, ...formData } as Partial<SalesOrder>)
      message.success('创建成功')
    }
    formVisible.value = false
    fetchData()
  } catch (error: any) {
    if (error?.errorFields) return // 表单校验错误
    console.warn('[销售订单] 表单提交失败', error)
    message.error(error?.response?.data?.message || '操作失败')
  } finally {
    formLoading.value = false
  }
}

const handleFormCancel = () => {
  formVisible.value = false
}

// ── 辅助函数 ────────────────────────────────────────

const filterCustomerOption = (input: string, option: any) => {
  return option.children?.[0]?.children?.toLowerCase?.().includes(input.toLowerCase())
}

// ── 初始化 ────────────────────────────────────────

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  fetchData()
  window.addEventListener("erp:create", handleParentCreate)
  window.addEventListener("erp:refresh", fetchData)
  // 加载客户选项
  loadCustomerOptions()
  // 更新搜索栏选项
  searchFields[2].options = Object.entries(SALES_ORDER_STATUS).map(([k, v]) => ({
    label: v.text,
    value: Number(k)
  }))
  filterFields[2].options = Object.entries(SALES_ORDER_STATUS).map(([k, v]) => ({
    label: v.text,
    value: Number(k)
  }))
})

async function loadCustomerOptions() {
  try {
    const { customerApi } = await import('@/api/customer')
    const res = await customerApi.getOptions()
    customerOptions.value = (res.data || res || []).map((c: any) => ({ id: c.id, name: c.name }))
  } catch (e) {
    console.warn('[销售订单] 加载客户选项失败', e)
    customerOptions.value = []
  }
  searchFields[1].options = customerOptions.value.map(c => ({ label: c.name, value: c.id }))
  filterFields[1].options = customerOptions.value.map(c => ({ label: c.name, value: c.id }))
}
</script>

<style scoped>
/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-amount { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }

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

.currency-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
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

.error-text {
  color: #ff4d4f;
  font-weight: 500;
}

/* 表格容器自动撑满 */
:deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
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
