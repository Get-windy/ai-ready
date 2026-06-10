<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="采购订单管理" full-height>
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
          <a-button size="small" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </a-space>
      </template>

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
            <div class="stat-card-value">{{ statDraft }}</div>
            <div class="stat-card-label">草稿</div>
          </div>
          <FileOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statPending }}</div>
            <div class="stat-card-label">待审批</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-completed">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statCompleted }}</div>
            <div class="stat-card-label">已完成</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-amount">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(statTotalAmount) }}</div>
            <div class="stat-card-label">订单金额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
      </div>

      <!-- 表格 -->
      <VxeTableList
        ref="tableRef"
        table-key="purchase-order-list"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :selectable="true"
        add-text="新建采购单"
        @add="handleCreate"
        @refresh="fetchData"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @cell-dblclick="handleView"
      >
        <template #toolbar-actions>
          <a-tooltip title="导出">
            <a-button size="small" @click="handleExport">
              <template #icon><ExportOutlined /></template>
            </a-button>
          </a-tooltip>
        </template>

        <template #empty>
          <div v-if="hasError" class="table-empty">
            <WarningOutlined class="table-empty-icon" />
            <p class="table-empty-text">数据加载异常，请重试</p>
            <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
          </div>
          <div v-else class="table-empty">
            <InboxOutlined v-if="!hasActiveFilters" class="table-empty-icon" />
            <SearchOutlined v-else class="table-empty-icon" />
            <p v-if="!hasActiveFilters" class="table-empty-text">暂无采购单数据，点击右上角新建</p>
            <p v-else class="table-empty-text">没有符合条件的采购单</p>
          </div>
        </template>

        <template #bodyCell="{ column, record }">
          <template v-if="column.field === 'status'">
            <StatusTag :status="record.status" :map="PURCHASE_ORDER_STATUS" />
          </template>
          <template v-else-if="column.field === 'totalAmount'">
            <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template v-else-if="column.type === 'action'">
            <a-space>
              <a-tooltip title="查看">
                <a-button type="link" size="small" @click="handleView(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 0" title="提交审批">
                <a-button type="link" size="small" @click="handleSubmit(record)">
                  <template #icon><SendOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 1" title="审批通过">
                <a-button type="link" size="small" @click="handleApprove(record)">
                  <template #icon><CheckCircleOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </VxeTableList>
    </PageContainer>

    <!-- 详情弹窗 -->
    <a-drawer
      v-model:open="detailVisible"
      title="采购单详情"
      placement="right"
      width="80vw"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData" size="small">
          <a-descriptions-item label="采购单号">{{ detailData.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="供应商">{{ detailData.supplierName }}</a-descriptions-item>
          <a-descriptions-item label="采购员">{{ detailData.purchaserName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="订单日期">{{ detailData.orderDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预计到货">{{ detailData.expectedDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="订单金额">
            <span class="amount-cell">¥{{ formatAmount(detailData.totalAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="PURCHASE_ORDER_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <h4 style="margin: 16px 0 8px;">订单明细</h4>
        <a-table
          :data-source="detailItems"
          :columns="detailItemColumns"
          :pagination="false"
          size="small"
          bordered
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'amount'">
              <span class="amount-cell">¥{{ formatAmount(record.amount || record.quantity * record.price) }}</span>
            </template>
          </template>
        </a-table>
      </a-spin>

      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 8px;">
          <PrintButton
            v-if="detailData"
            :business-id="detailData.id"
            :record="detailData"
            business-type="purchase"
            button-text="打印"
            button-size="small"
            button-type="default"
          />
          <a-button @click="detailVisible = false">关闭</a-button>
          <a-button v-if="detailData?.status === 0" type="primary" @click="handleSubmit(detailData)">
            <SendOutlined /> 提交审批
          </a-button>
          <a-button v-if="detailData?.status === 1" type="primary" @click="handleApprove(detailData)">
            <CheckCircleOutlined /> 审批通过
          </a-button>
        </div>
      </template>
    </a-drawer>

    <!-- 新建采购单弹窗 -->
    <a-modal
      v-model:open="createVisible"
      title="新建采购单"
      width="800px"
      :confirm-loading="createLoading"
      @ok="handleCreateSubmit"
      @cancel="handleCreateCancel"
      :destroy-on-close="true"
    >
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="供应商" name="supplierId">
              <a-select
                v-model:value="createForm.supplierId"
                placeholder="选择供应商"
                show-search
                :filter-option="false"
                :options="supplierOptions"
                :loading="supplierLoading"
                @search="handleSupplierSearch"
                @change="handleSupplierChange"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="采购员" name="purchaserName">
              <a-input v-model:value="createForm.purchaserName" placeholder="采购员姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="仓库" name="warehouseId">
              <a-select
                v-model:value="createForm.warehouseId"
                placeholder="选择仓库"
                show-search
                :filter-option="filterOption"
                :options="warehouseOptions"
                @change="handleWarehouseChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="订单日期" name="orderDate">
              <a-date-picker v-model:value="createForm.orderDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="预计到货">
              <a-date-picker v-model:value="createForm.expectedDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider />
        <div style="margin-bottom: 12px; font-weight: 600;">采购明细</div>
        <a-button type="dashed" block style="margin-bottom: 12px;" @click="handleAddItem">
          <PlusOutlined /> 添加产品
        </a-button>
        <a-table
          :data-source="createForm.items"
          :columns="createItemColumns"
          :pagination="false"
          row-key="tempId"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'product'">
              <a-select
                v-model:value="record.productId"
                placeholder="搜索选择产品"
                style="width: 100%"
                show-search
                allow-clear
                :filter-option="false"
                :options="productOptions"
                :loading="productLoading"
                @search="(val: string) => handleProductSearch(val)"
                @change="(val: number) => handleProductChange(val, index)"
              >
                <template #option="{ label, productCode, productSpec }">
                  <div>
                    <div>{{ label }}</div>
                    <div style="font-size: 12px; color: #999;">
                      {{ productCode }}{{ productSpec ? ` / ${productSpec}` : '' }}
                    </div>
                  </div>
                </template>
              </a-select>
            </template>
            <template v-else-if="column.key === 'quantity'">
              <a-input-number v-model:value="record.quantity" :min="1" :precision="0" style="width: 100%" placeholder="数量" />
            </template>
            <template v-else-if="column.key === 'price'">
              <a-input-number v-model:value="record.price" :min="0" :precision="2" style="width: 100%" placeholder="单价" />
            </template>
            <template v-else-if="column.key === 'amount'">
              ¥{{ formatAmount((record.quantity || 0) * (record.price || 0)) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="handleRemoveItem(index)">
                <DeleteOutlined />
              </a-button>
            </template>
          </template>
        </a-table>

        <a-form-item label="备注" style="margin-top: 12px;">
          <a-textarea v-model:value="createForm.remark" :rows="2" placeholder="备注信息" />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SyncOutlined, FileOutlined, ClockCircleOutlined,
  CheckCircleOutlined, DollarOutlined, WarningOutlined, InboxOutlined,
  SearchOutlined, EyeOutlined, SendOutlined, ExportOutlined,
  PlusOutlined, DeleteOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer, SearchBar } from '@/components'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { PURCHASE_ORDER_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 类型 ──────────────────────────────────────────────
interface PurchaseOrder {
  id: number
  orderNo: string
  supplierId: number
  supplierName: string
  purchaserName: string
  warehouseId: number
  warehouseName: string
  orderDate: string
  expectedDate: string
  totalAmount: number
  status: number
  remark: string
  items?: PurchaseOrderItem[]
}

interface PurchaseOrderItem {
  id: number
  productId: number
  productCode: string
  productName: string
  productSpec: string
  quantity: number
  price: number
  amount: number
}

// ── 状态 ──────────────────────────────────────────────
const loading = ref(false)
const hasError = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const tableData = ref<PurchaseOrder[]>([])
const tableRef = ref()
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<PurchaseOrder | null>(null)
const detailItems = ref<PurchaseOrderItem[]>([])
const selectedRows = ref<PurchaseOrder[]>([])
const currentRecord = ref<PurchaseOrder | null>(null)

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计 ──────────────────────────────────────────────
const statDraft = ref(0)
const statPending = ref(0)
const statCompleted = ref(0)
const statTotalAmount = ref(0)

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

// ── 搜索 ──────────────────────────────────────────────
const searchFields = [
  { key: 'orderNo', label: '采购单号', type: 'input', placeholder: '请输入采购单号' },
  { key: 'supplierName', label: '供应商', type: 'input', placeholder: '请输入供应商名称' },
  { key: 'status', label: '状态', type: 'select', options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '已完成', value: 4 },
  ]},
] as any[]

const searchForm = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

const filterFields = computed(() => [
  { key: 'orderNo', label: '采购单号', type: 'input' as const, placeholder: '请输入采购单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '请输入供应商名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '已完成', value: 4 },
  ]},
])

const vxeColumns = computed(() => [
  { field: 'orderNo', title: '采购单号', width: 160 },
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'purchaserName', title: '采购员', width: 100 },
  { field: 'totalAmount', title: '金额', width: 130, align: 'right' },
  { field: 'status', title: '状态', width: 100, align: 'center' },
  { field: 'orderDate', title: '订单日期', width: 120 },
  { field: 'createTime', title: '创建时间', width: 150 },
  { type: 'action', title: '操作', width: 150, fixed: 'right' },
])

// ── 详情 ──────────────────────────────────────────────
const detailItemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '规格', dataIndex: 'productSpec', width: 100 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'price', width: 100, align: 'right' },
  { title: '小计', dataIndex: 'amount', width: 120, align: 'right' },
]

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  detailItems.value = []
  try {
    const [orderRes, itemsRes] = await Promise.all([
      request.get(`/erp/purchase/order/${id}`),
      request.get(`/erp/purchase/order/${id}/items`).catch(() => ({ data: [] })),
    ])
    detailData.value = orderRes.data || null
    detailItems.value = itemsRes.data || []
  } catch (err) {
    console.warn('[采购管理] 获取详情失败', err)
    detailData.value = tableData.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: PurchaseOrder) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

// ── 状态操作 ──────────────────────────────────────────
const handleSubmit = (record: PurchaseOrder) => {
  Modal.confirm({
    title: '提交审批',
    content: `确认提交采购单 ${record.orderNo} 进行审批吗？`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/order/${record.id}/submit`)
        message.success('已提交审批')
        fetchData()
        if (detailVisible.value && detailData.value?.id === record.id) detailVisible.value = false
      } catch (error) {
        console.warn('[采购管理] 提交失败', error)
        message.error('提交失败')
      }
    }
  })
}

const handleApprove = (record: PurchaseOrder) => {
  Modal.confirm({
    title: '审批确认',
    content: `确认审批通过采购单 ${record.orderNo} 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/purchase/order/${record.id}/approve`)
        message.success('审批通过')
        fetchData()
        if (detailVisible.value && detailData.value?.id === record.id) detailVisible.value = false
      } catch (error) {
        console.warn('[采购管理] 审批失败', error)
        message.error('审批失败')
      }
    }
  })
}

// ── 数据加载 ──────────────────────────────────────────
const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const params: Record<string, any> = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    Object.keys(params).forEach(k => { if (params[k] === undefined || params[k] === '') delete params[k] })

    const res = await request.get('/erp/purchase/order/page', { params })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0

    // 统计
    statDraft.value = tableData.value.filter(r => r.status === 0).length
    statPending.value = tableData.value.filter(r => r.status === 1).length
    statCompleted.value = tableData.value.filter(r => r.status >= 4).length
    statTotalAmount.value = tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  } catch (error) {
    hasError.value = true
    console.warn('[采购管理] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { Object.keys(searchForm).forEach(k => searchForm[k] = undefined); handleSearch() }
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchForm, filters); pagination.current = 1; fetchData() }
function handleSelectionChange(rows: PurchaseOrder[], ids: number[]) { selectedRows.value = rows }
const handleError = () => { hasError.value = true }

// ── 导出 ──────────────────────────────────────────────
const handleExport = async () => {
  try {
    const blob = await request.get('/erp/purchase/order/export', {
      params: { ...searchForm },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `采购订单_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error) {
    console.warn('[采购管理] 导出失败', error)
    message.error('导出失败')
  }
}

// ── 新建表单 ──────────────────────────────────────────
const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref()
const supplierOptions = ref<any[]>([])
const supplierLoading = ref(false)
const warehouseOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const productLoading = ref(false)

let tempIdCounter = 0
function nextTempId() { return ++tempIdCounter }

const createForm = reactive({
  supplierId: undefined as number | undefined,
  supplierName: '',
  purchaserName: '',
  warehouseId: undefined as number | undefined,
  warehouseName: '',
  orderDate: '',
  expectedDate: '',
  remark: '',
  items: [] as any[]
})

// ── 表单脏数据追踪 ──────────────────────────────────
const formDirty = ref(false)
const originalFormJson = ref('')

watch(createForm, () => {
  formDirty.value = JSON.stringify(createForm) !== originalFormJson.value
}, { deep: true })

onBeforeRouteLeave((to, from) => {
  if (formDirty.value) {
    return new Promise<boolean>((resolve) => {
      Modal.confirm({
        title: '确认离开',
        content: '当前表单有未保存的更改，确定要离开吗？',
        okText: '离开',
        cancelText: '取消',
        onOk: () => { resolve(true) },
        onCancel: () => { resolve(false) },
      })
    })
  }
  return true
})

const createRules: Record<string, any> = {
  supplierId: [{ required: true, message: '请选择供应商' }],
  orderDate: [{ required: true, message: '请选择订单日期' }],
}

const createItemColumns = [
  { title: '产品', key: 'product', width: 250 },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '单价', key: 'price', width: 120 },
  { title: '小计', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 },
]

const filterOption = (input: string, option: any) => {
  return option?.label?.toLowerCase()?.includes(input.toLowerCase())
}

const handleSupplierSearch = async (keyword: string) => {
  supplierLoading.value = true
  try {
    const res = await request.get('/erp/partner/list', {
      params: { partnerType: 'SUPPLIER', keyword, pageSize: 50 }
    })
    supplierOptions.value = (res.data?.records || res.data || []).map((s: any) => ({
      value: s.id,
      label: s.name || s.supplierName,
    }))
  } catch { /* ignore */ }
  finally { supplierLoading.value = false }
}

const handleSupplierChange = (value: number) => {
  const found = supplierOptions.value.find(s => s.value === value)
  createForm.supplierName = found?.label || ''
}

const handleWarehouseChange = (value: number) => {
  const found = warehouseOptions.value.find(w => w.value === value)
  createForm.warehouseName = found?.label || ''
}

const loadWarehouseOptions = async () => {
  try {
    const res = await request.get('/erp/stock/warehouses')
    const list = res.data || []
    warehouseOptions.value = list.map((w: any) => ({ value: w.id, label: w.name || w.warehouseName }))
  } catch { /* ignore */ }
}

const handleProductSearch = async (keyword: string) => {
  if (!keyword) return
  productLoading.value = true
  try {
    const res = await request.get('/erp/product/list', { params: { keyword, pageSize: 50 } })
    productOptions.value = ((res.data?.records || res.data) || []).map((p: any) => ({
      value: p.id,
      label: p.name || p.productName,
      productCode: p.code || p.productCode,
      productSpec: p.spec || p.productSpec,
      productUnit: p.unit || p.productUnit,
    }))
  } catch { /* ignore */ }
  finally { productLoading.value = false }
}

const handleProductChange = (value: number, index: number) => {
  const item = createForm.items[index]
  if (!item || !value) return
  const found = productOptions.value.find(p => p.value === value)
  if (found) {
    item.productId = found.value
    item.productCode = found.productCode
    item.productName = found.label
    item.productSpec = found.productSpec
    item.productUnit = found.productUnit
  }
}

const handleAddItem = () => {
  createForm.items.push({
    tempId: nextTempId(),
    productId: undefined,
    productCode: '',
    productName: '',
    productSpec: '',
    productUnit: '',
    quantity: 1,
    price: 0,
  })
}

const handleRemoveItem = (index: number) => {
  createForm.items.splice(index, 1)
}

const handleCreate = () => {
  tempIdCounter = 0
  createForm.supplierId = undefined
  createForm.supplierName = ''
  createForm.purchaserName = ''
  createForm.warehouseId = undefined
  createForm.warehouseName = ''
  createForm.orderDate = new Date().toISOString().slice(0, 10)
  createForm.expectedDate = ''
  createForm.remark = ''
  createForm.items = []
  handleAddItem()
  // 捕获初始表单快照，用于脏数据追踪
  originalFormJson.value = JSON.stringify(createForm)
  formDirty.value = false
  createVisible.value = true
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch { return }
  if (createForm.items.length === 0) {
    message.warning('请至少添加一个采购产品')
    return
  }
  createLoading.value = true
  try {
    const payload = {
      supplierId: createForm.supplierId,
      supplierName: createForm.supplierName,
      purchaserName: createForm.purchaserName || undefined,
      warehouseId: createForm.warehouseId,
      warehouseName: createForm.warehouseName,
      orderDate: createForm.orderDate,
      expectedDate: createForm.expectedDate || undefined,
      remark: createForm.remark || undefined,
      items: createForm.items.map((item: any) => {
        const { tempId, productUnit, productCode, productName, productSpec, ...rest } = item
        return { ...rest, productCode: item.productCode, productName: item.productName, productSpec: item.productSpec }
      })
    }
    await request.post('/erp/purchase/order', payload)
    message.success('采购单创建成功')
    createVisible.value = false
    formDirty.value = false
    fetchData()
  } catch (error: any) {
    console.warn('[采购管理] 创建失败', error)
    message.error(error?.response?.data?.message || '创建失败')
  } finally {
    createLoading.value = false
  }
}

const handleCreateCancel = () => {
  createVisible.value = false
  formDirty.value = false
}

// ── 键盘快捷键 ──
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
}

onMounted(() => {
  loadWarehouseOptions()
  fetchData()
  autoRefreshCountdown.value = 30
  window.addEventListener('keydown', handleKeydown)
  refreshTimer = setInterval(() => { fetchData(); autoRefreshCountdown.value = 30 }, 30000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stat-card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-card-value {
  font-size: 24px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.stat-card-label {
  font-size: 13px;
  color: #666;
}

.stat-card-icon {
  font-size: 32px;
  opacity: 0.3;
}

.stat-draft { border-left: 3px solid #d9d9d9; }
.stat-draft .stat-card-value { color: #666; }
.stat-pending { border-left: 3px solid #faad14; }
.stat-pending .stat-card-value { color: #faad14; }
.stat-completed { border-left: 3px solid #52c41a; }
.stat-completed .stat-card-value { color: #52c41a; }
.stat-amount { border-left: 3px solid #722ed1; }
.stat-amount .stat-card-value { color: #722ed1; }

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.error-text {
  color: #ff4d4f;
  font-size: 12px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
}

.amount-cell {
  color: #ff4d4f;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
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
  margin-bottom: 12px;
}

.table-empty-text {
  color: #999;
  margin-bottom: 16px;
}
</style>
