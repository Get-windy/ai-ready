<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="return-page-header">
        <div class="return-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>退货管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="return-page-title">退货管理</h2>
        </div>
        <div class="return-page-header-right">
          <span class="data-status">
            <a-badge :count="statistics.pendingCount" :overflow-count="999" :number-style="{ backgroundColor: '#faad14', fontSize: 11, padding: '0 6px', minWidth: 18, height: 18, lineHeight: '18px' }">
              <span style="padding: 0 4px; font-size: 13px; color: #606266;">待处理</span>
            </a-badge>
          </span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', handleRefresh)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">退货单总数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审核</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已退款</div>
            <div class="summary-value">{{ statistics.refundedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">退货金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 搜索栏 -->
    <SearchBar
      :fields="searchFields"
      :loading="loading"
      @search="handleSearch"
      @reset="handleReset"
    />

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      :show-export="true"
      add-text="新建退货申请"
      style="flex: 1;"
      @add="handleCreate"
      @refresh="fetchData"
      @export="handleExport"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdateTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </template>

      <template #empty>
        <EmptyState v-if="hasError" image="error" title="数据加载异常" description="数据获取失败，请检查后重试" :show-add="false" size="small" @refresh="fetchData" />
        <EmptyState v-else-if="hasActiveFilters" image="no-data" title="没有符合条件的退货单" description="请尝试修改筛选条件" :show-add="false" size="small" @refresh="fetchData" />
        <EmptyState v-else image="no-data" title="暂无退货单" description="当前没有退货单数据" add-text="新建退货申请" size="small" @refresh="fetchData" @add="handleCreate" />
      </template>

      <template #action="{ record }">
        <a-space>
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 0" title="审核">
            <a-button type="link" size="small" @click="handleApprove(record)">
              <template #icon><CheckCircleOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 1" title="入库">
            <a-button type="link" size="small" @click="handleReceive(record)">
              <template #icon><DownloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 2" title="退款">
            <a-button type="link" size="small" @click="handleRefund(record)">
              <template #icon><RollbackOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item key="delete">
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <a-drawer
      v-model:open="detailVisible"
      title="退货单详情"
      placement="right"
      width="80vw"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detailData">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="退货单号">{{ detailData.returnNo }}</a-descriptions-item>
            <a-descriptions-item label="销售订单">{{ detailData.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="客户名称">{{ detailData.customerName }}</a-descriptions-item>
            <a-descriptions-item label="退货金额">¥{{ detailData.returnAmount?.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="退货原因" :span="2">{{ detailData.returnReason }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <StatusTag :status="detailData.status" :map="RETURN_STATUS" />
            </a-descriptions-item>
            <a-descriptions-item label="退货日期">{{ detailData.returnDate }}</a-descriptions-item>
            <a-descriptions-item label="操作人">{{ detailData.operator }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 退货明细表格 -->
          <a-divider style="margin-top: 24px;" />
          <div style="margin-bottom: 12px; font-weight: 600; font-size: 15px;">退货明细</div>
          <a-table
            :data-source="detailData.items || []"
            :columns="detailItemColumns"
            :pagination="false"
            row-key="productCode"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'subtotal'">
                ¥{{ ((record.returnQuantity || 0) * (record.unitPrice || 0)).toFixed(2) }}
              </template>
            </template>
            <template #empty>
              <a-empty description="暂无明细数据" />
            </template>
          </a-table>
        </template>
      </a-spin>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <a-button @click="detailVisible = false">关闭</a-button>
          <a-button
            v-if="detailData?.status === 0"
            type="primary"
            @click="handleApprove(detailData)"
          >
            <template #icon><CheckCircleOutlined /></template>
            审核
          </a-button>
          <a-button
            v-if="detailData?.status === 1"
            type="primary"
            @click="handleReceive(detailData)"
          >
            <template #icon><DownloadOutlined /></template>
            入库
          </a-button>
          <a-button
            v-if="detailData?.status === 2"
            type="primary"
            @click="handleRefund(detailData)"
          >
            <template #icon><RollbackOutlined /></template>
            退款
          </a-button>
          <PrintButton v-if="detailData && detailData.status >= 3" :record="detailData" />
        </div>
      </template>
    </a-drawer>

    <!-- 新建退货申请弹窗 -->
    <a-modal
      v-model:open="createVisible"
      title="新建退货申请"
      width="700px"
      :confirm-loading="createLoading"
      @ok="handleCreateSubmit"
      @cancel="handleCreateCancel"
      :destroy-on-close="true"
    >
      <a-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        layout="vertical"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="客户" name="customerId">
              <a-select
                v-model:value="createForm.customerId"
                show-search
                :filter-option="false"
                placeholder="请搜索选择客户"
                :options="customerOptions"
                :field-names="{ value: 'id', label: 'name' }"
                @search="handleCustomerSearch"
                @change="handleCustomerChange"
                :loading="customerLoading"
                allow-clear
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联销售订单" name="saleOrderNo">
              <a-input v-model:value="createForm.saleOrderNo" placeholder="请输入销售订单号（可选）" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="退货类型" name="returnType">
              <a-select v-model:value="createForm.returnType" placeholder="请选择退货类型">
                <a-select-option :value="1">质量不良</a-select-option>
                <a-select-option :value="2">数量不符</a-select-option>
                <a-select-option :value="3">交期延误</a-select-option>
                <a-select-option :value="4">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="退货原因" name="reason">
              <a-input v-model:value="createForm.reason" placeholder="请输入退货原因" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="退货日期" name="returnDate">
              <a-date-picker v-model:value="createForm.returnDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="createForm.remark" :rows="2" placeholder="请输入备注" />
        </a-form-item>

        <a-divider />
        <div style="margin-bottom: 12px; font-weight: 600;">退货明细</div>
        <a-button type="dashed" block style="margin-bottom: 12px;" @click="handleAddItem">
          <PlusOutlined /> 添加商品
        </a-button>
        <a-table
          :data-source="createForm.items"
          :columns="itemColumns"
          :pagination="false"
          row-key="tempId"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'">
              {{ record.productName }}
            </template>
            <template v-if="column.key === 'productCode'">
              {{ record.productCode }}
            </template>
            <template v-if="column.key === 'returnQuantity'">
              <a-input-number
                v-model:value="record.returnQuantity"
                :min="0.01"
                :precision="2"
                style="width: 100%"
                placeholder="数量"
              />
            </template>
            <template v-if="column.key === 'unitPrice'">
              <a-input-number
                v-model:value="record.unitPrice"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="单价"
              />
            </template>
            <template v-if="column.key === 'reason'">
              <a-input v-model:value="record.reason" placeholder="退货原因" style="width: 100%" />
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="handleRemoveItem(index)">
                <DeleteOutlined />
              </a-button>
            </template>
          </template>
          <template #empty>
            <a-empty description="请点击上方按钮添加商品" />
          </template>
        </a-table>
      </a-form>
    </a-modal>

    <!-- 商品选择弹窗 -->
    <a-modal
      v-model:open="productPickerVisible"
      title="选择商品"
      width="600px"
      :footer="null"
      :destroy-on-close="true"
    >
      <a-input-search
        v-model:value="productPickerKeyword"
        placeholder="搜索商品编码/名称"
        @search="fetchProducts"
        style="margin-bottom: 12px;"
      />
      <a-table
        :data-source="productOptions"
        :columns="productPickerColumns"
        :pagination="{ pageSize: 10, size: 'small' }"
        :loading="productLoading"
        row-key="id"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button
              type="primary"
              size="small"
              @click="handleSelectProduct(record)"
              :disabled="createForm.items.some(item => item.productId === record.id)"
            >
              选择
            </a-button>
          </template>
        </template>
      </a-table>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import { RETURN_STATUS } from '@/utils/statusConfig'
import { message, Modal } from 'ant-design-vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import {
  PlusOutlined, EyeOutlined, CheckCircleOutlined, DownloadOutlined, RollbackOutlined,
  EllipsisOutlined, ExportOutlined, DeleteOutlined, SearchOutlined, InboxOutlined, FileTextOutlined,
  ClockCircleOutlined, DollarOutlined, SyncOutlined, ReloadOutlined, WarningOutlined
} from '@ant-design/icons-vue'

// ── 防抖工具 ──────────────────────────────────────────
function handleError(err: any) { console.warn('[Return]', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

interface ReturnOrderItem {
  productCode: string
  productName: string
  returnQuantity: number
  unitPrice: number
  subtotal?: number
}

interface ReturnOrder {
  id: number
  returnNo: string
  orderNo: string
  customerName: string
  returnAmount: number
  returnReason: string
  status: number
  returnDate: string
  operator: string
  remark?: string
  items?: ReturnOrderItem[]
}

const loading = ref(false)
const refreshLoading = ref(false)
const hasError = ref(false)
const autoRefreshCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const dataSource = ref<ReturnOrder[]>([])
const detailVisible = ref(false)
const currentRecord = ref<ReturnOrder | null>(null)
const tableRef = ref()
const lastUpdateTime = ref(new Date().toISOString())
const selectedRows = ref<ReturnOrder[]>([])
const selectedIds = ref<number[]>([])

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  refundedCount: 0,
  totalAmount: 0
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = dataSource

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const vxeColumns = computed(() => [
  { field: 'returnNo', title: '退货单号', width: 150 },
  { field: 'orderNo', title: '销售订单', width: 150 },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'returnAmount', title: '退货金额', width: 120, align: 'right', formatter: ({ cellValue }) => `¥${cellValue?.toFixed(2) || '0.00'}` },
  { field: 'returnReason', title: '退货原因', minWidth: 100, showOverflow: 'tooltip' },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => RETURN_STATUS[cellValue]?.text || '' },
  { field: 'returnDate', title: '退货日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' },
])

// 详情抽屉中的退货明细表格列
const detailItemColumns = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 200 },
  { title: '数量', dataIndex: 'returnQuantity', key: 'returnQuantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right', customRender: ({ text }: { text: number }) => `¥${text?.toFixed(2) || '0.00'}` },
  { title: '小计', dataIndex: 'subtotal', key: 'subtotal', width: 100, align: 'right' },
]

const filterFields = [
  { key: 'returnNo', label: '退货单号', type: 'input' as const, placeholder: '请输入退货单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已入库', value: 2 },
    { label: '已退款', value: 3 },
  ]},
]

const searchFields: SearchField[] = [
  { name: 'returnNo', label: '退货单号', type: 'input', placeholder: '请输入退货单号' },
  { name: 'orderNo', label: '销售订单', type: 'input', placeholder: '请输入订单号' },
  { name: 'status', label: '状态', type: 'select', options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已入库', value: 2 },
    { label: '已退款', value: 3 },
  ]},
]

const handleRefresh = () => {
  autoRefreshCountdown.value = 30
  refreshLoading.value = true
  fetchData().finally(() => { refreshLoading.value = false })
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    Object.assign(searchFilters, values)
  }
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

const handleResetFilters = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: ReturnOrder[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

function handleParentCreate() { handleCreate() }

// ── 新建退货申请表单 ──────────────────────────────────
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const customerOptions = ref<any[]>([])
const customerLoading = ref(false)
const productOptions = ref<any[]>([])
const productLoading = ref(false)
const productPickerVisible = ref(false)
const productPickerKeyword = ref('')

interface CreateItem {
  tempId: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  returnQuantity: number
  unitPrice: number
  reason: string
}

let tempIdCounter = 0

const createForm = reactive({
  customerId: undefined as number | undefined,
  customerName: '',
  saleOrderNo: '',
  returnType: undefined as number | undefined,
  reason: '',
  returnDate: dayjs().format('YYYY-MM-DD'),
  remark: '',
  items: [] as CreateItem[],
})

const createRules: Record<string, any> = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  returnType: [{ required: true, message: '请选择退货类型', trigger: 'change' }],
}

const itemColumns = [
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 160 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '退货数量', dataIndex: 'returnQuantity', key: 'returnQuantity', width: 100 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '退货原因', dataIndex: 'reason', key: 'reason', width: 140 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 60, fixed: 'right' },
]

const productPickerColumns = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 200 },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec', width: 120 },
  { title: '单位', dataIndex: 'productUnit', key: 'productUnit', width: 60 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 60 },
]

const handleCreate = () => {
  createForm.customerId = undefined
  createForm.customerName = ''
  createForm.saleOrderNo = ''
  createForm.returnType = undefined
  createForm.reason = ''
  createForm.returnDate = dayjs().format('YYYY-MM-DD')
  createForm.remark = ''
  createForm.items = []
  createVisible.value = true
  fetchCustomers()
}

const handleCreateCancel = () => {
  createVisible.value = false
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  if (createForm.items.length === 0) {
    message.warning('请至少添加一个退货商品')
    return
  }
  createLoading.value = true
  try {
    const payload = {
      customerId: createForm.customerId,
      customerName: createForm.customerName,
      saleOrderNo: createForm.saleOrderNo || undefined,
      returnType: createForm.returnType,
      reason: createForm.reason,
      returnDate: createForm.returnDate,
      remark: createForm.remark,
      items: createForm.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        productSpec: item.productSpec,
        productUnit: item.productUnit,
        returnQuantity: item.returnQuantity,
        unitPrice: item.unitPrice,
        reason: item.reason,
      })),
    }
    await request.post('/erp/sale/return', payload)
    message.success('新建退货申请成功')
    createVisible.value = false
    fetchData()
  } catch (e: any) {
    console.warn('[退货管理] 新建退货申请失败', e)
    message.error(e?.response?.data?.message || '新建退货申请失败')
  } finally {
    createLoading.value = false
  }
}

const fetchCustomers = async (keyword?: string) => {
  customerLoading.value = true
  try {
    const params: Record<string, any> = { partnerType: 'CUSTOMER', pageSize: 200 }
    if (keyword) params.keyword = keyword
    const res = await request.get('/erp/partner/list', { params })
    customerOptions.value = res.data?.records || res.data || []
  } catch (e) {
    console.warn('[退货管理] 获取客户列表失败', e)
  } finally {
    customerLoading.value = false
  }
}

const handleCustomerSearch = (value: string) => {
  fetchCustomers(value)
}

const handleCustomerChange = (value: number) => {
  if (!value) {
    createForm.customerName = ''
    return
  }
  const found = customerOptions.value.find((c: any) => c.id === value)
  createForm.customerName = found?.name || ''
}

const fetchProducts = async (keyword?: string) => {
  productLoading.value = true
  try {
    const params: Record<string, any> = { pageSize: 50 }
    if (keyword) params.keyword = keyword
    const res = await request.get('/erp/product/list', { params })
    productOptions.value = res.data?.records || res.data || []
  } catch (e) {
    console.warn('[退货管理] 获取商品列表失败', e)
  } finally {
    productLoading.value = false
  }
}

const handleAddItem = () => {
  productPickerKeyword.value = ''
  productPickerVisible.value = true
  fetchProducts()
}

const handleSelectProduct = (product: any) => {
  createForm.items.push({
    tempId: ++tempIdCounter,
    productId: product.id,
    productCode: product.productCode,
    productName: product.productName,
    productSpec: product.productSpec || '',
    productUnit: product.productUnit || '',
    returnQuantity: 1,
    unitPrice: product.unitPrice || 0,
    reason: '',
  })
  message.success(`已添加商品: ${product.productName}`)
}

const handleRemoveItem = (index: number) => {
  createForm.items.splice(index, 1)
}

const detailData = ref<ReturnOrder | null>(null)
const detailLoading = ref(false)

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await request.get(`/erp/sale/return/${id}`)
    detailData.value = res.data
  } catch (err) {
    console.warn('[退货管理] 获取详情失败', err)
    detailData.value = dataSource.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: ReturnOrder) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleApprove = (record: ReturnOrder) => {
  Modal.confirm({
    title: '确认审核',
    content: `确定审核通过退货单「${record.returnNo}」吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/sale/return/${record.id}/approve`)
        message.success(`审核退货单成功: ${record.returnNo}`)
        fetchData()
      } catch (e) {
        console.warn('[退货管理] 审核失败', e)
        message.error('审核失败')
      }
    }
  })
}

const handleReceive = (record: ReturnOrder) => {
  Modal.confirm({
    title: '确认入库',
    content: `确定已将退货单「${record.returnNo}」的商品入库吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/sale/return/${record.id}/receive`)
        message.success(`入库完成: ${record.returnNo}`)
        fetchData()
      } catch (e) {
        console.warn('[退货管理] 入库失败', e)
        message.error('入库失败')
      }
    }
  })
}

const handleRefund = (record: ReturnOrder) => {
  Modal.confirm({
    title: '确认退款',
    content: `确定对退货单「${record.returnNo}」执行退款操作吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/sale/return/${record.id}/refund`)
        message.success(`退款完成: ${record.returnNo}`)
        fetchData()
      } catch (e) {
        console.warn('[退货管理] 退款失败', e)
        message.error('退款失败')
      }
    }
  })
}

const handleDelete = async (record: ReturnOrder) => {
  try {
    await request.delete(`/erp/sale/return/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[退货管理] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: ReturnOrder) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该退货单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

const handleExport = async () => {
  try {
    const res = await request.get('/erp/return/export', {
      params: { ...searchFilters },
      responseType: 'blob'
    })
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `退货单_${dayjs().format('YYYYMMDDHHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    console.warn('[退货管理] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    debounceClick('export', handleExport)
  }
}

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const params: any = {
      ...searchFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/erp/sale/return/page', { params })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
      // 从 API 返回值读取全局统计
      if (res.data.statistics) {
        statistics.value = {
          totalCount: res.data.statistics.totalCount ?? dataSource.value.length,
          pendingCount: res.data.statistics.pendingCount ?? 0,
          refundedCount: res.data.statistics.refundedCount ?? 0,
          totalAmount: res.data.statistics.totalAmount ?? 0
        }
      } else {
        // 兼容旧版 API：从当前页数据计算（仅显示当页统计）
        statistics.value.totalCount = dataSource.value.length
        statistics.value.pendingCount = dataSource.value.filter(r => r.status === 0).length
        statistics.value.refundedCount = dataSource.value.filter(r => r.status === 3).length
        statistics.value.totalAmount = dataSource.value.reduce((sum, r) => sum + (r.returnAmount || 0), 0)
      }
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdateTime.value = new Date().toISOString()
  } catch (error) {
    hasError.value = true
    console.warn('[退货管理] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
  window.addEventListener("erp:create", handleParentCreate)
  window.addEventListener("erp:refresh", fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.return-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.return-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.return-page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.return-page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
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

.list-update-timestamp {
  color: #999;
  font-size: 12px;
  margin-right: 12px;
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

.action-more-btn {
  padding: 0 4px;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  border: 1px solid #ffa39e;
}

.summary-icon {
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

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}

/* 表格容器自动撑满 */
:deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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
