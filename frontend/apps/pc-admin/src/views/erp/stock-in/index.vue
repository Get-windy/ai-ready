<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="stock-in-header">
        <div class="stock-in-header__left">
          <span class="stock-in-header__breadcrumb">ERP / 采购管理 / 入库管理</span>
          <h2 class="stock-in-header__title">入库管理</h2>
        </div>
        <div class="stock-in-header__right">
          <a-space :size="12">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">
                数据更新: {{ lastUpdateTime }}
              </span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
          </a-space>
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
            <div class="summary-title">入库单总数</div>
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
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statistics.completedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">入库金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="入库管理" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <!-- 搜索栏 -->
      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button v-permission="'stock:inbound:create'" type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新建入库单
          </a-button>
          <a-button v-permission="'stock:inbound:export'" @click="debounceClick('export', handleExport)">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @cell-dblclick="handleView"
        @page-change="handlePageChange"
      >
        <template #empty>
          <EmptyState v-if="hasError" image="error" title="数据加载异常" description="数据获取失败，请检查后重试" :show-add="false" size="small" @refresh="fetchData" />
          <EmptyState v-else image="no-data" title="暂无入库单" description="当前没有入库单数据" add-text="新建入库单" size="small" @refresh="fetchData" @add="handleCreate" />
        </template>
        <template #statusCell="{ record }">
          <StatusTag :status="record.status" :map="INBOUND_STATUS" />
        </template>
        <template #totalAmountCell="{ record }">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
        <template #action="{ record }">
          <a-space>
            <a @click="handleView(record)">查看</a>
            <a v-if="record.status === 0" @click="handleApprove(record)">审核</a>
            <a v-if="record.status === 1" @click="handleExecuteInbound(record)">入库</a>
            <PrintButton
              v-if="record.status >= 2"
              templateType="stock_in"
              :businessId="record.id"
              businessType="stock_in"
              buttonText="打印"
              buttonSize="small"
              @print-success="() => message.success(`入库单 ${record.inboundNo} 打印成功`)"
              @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
            />
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 详情弹窗 -->
    <a-drawer v-model:open="detailVisible" title="入库单详情" placement="right" width="80vw">
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="入库单号">{{ detailData.inboundNo }}</a-descriptions-item>
          <a-descriptions-item label="采购订单号">{{ detailData.purchaseOrderNo }}</a-descriptions-item>
          <a-descriptions-item label="供应商">{{ detailData.supplierName }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="入库金额">¥{{ detailData.totalAmount?.toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="INBOUND_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="入库日期">{{ detailData.inboundDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="采购员">{{ detailData.purchaserName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="部门">{{ detailData.departmentName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="物流公司">{{ detailData.logisticsCompany || '-' }}</a-descriptions-item>
          <a-descriptions-item label="运单号">{{ detailData.trackingNumber || '-' }}</a-descriptions-item>
          <a-descriptions-item label="操作人">{{ detailData.operator || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 商品明细 -->
        <div v-if="detailData?.items?.length" style="margin-top: 16px;">
          <h4 style="margin-bottom: 8px; font-weight: 600;">商品明细</h4>
          <a-table
            :dataSource="detailData.items"
            :columns="detailItemColumns"
            :pagination="false as any"
            size="small"
            row-key="id"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'amount'">
                ¥{{ ((record.orderQuantity || 0) * (record.unitPrice || 0)).toFixed(2) }}
              </template>
              <template v-else-if="column.dataIndex === 'unitPrice'">
                ¥{{ (record.unitPrice || 0).toFixed(2) }}
              </template>
            </template>
          </a-table>
        </div>
      </a-spin>

      <template #footer>
        <div style="text-align: right;">
          <a-space>
            <a-button @click="detailVisible = false">关闭</a-button>
            <a-button v-if="detailData?.status === 0" type="primary" @click="handleApprove(detailData)">审核</a-button>
            <a-button v-if="detailData?.status === 1" type="primary" @click="handleExecuteInbound(detailData)">入库</a-button>
            <PrintButton
              v-if="detailData?.status >= 2"
              templateType="stock_in"
              :businessId="detailData?.id"
              businessType="stock_in"
              buttonText="打印"
              @print-success="() => message.success(`入库单 ${detailData?.inboundNo} 打印成功`)"
              @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
            />
          </a-space>
        </div>
      </template>
    </a-drawer>
  </PageContainer>

  <!-- ════════════════════════════════════════════════════════════ -->
  <!-- 新建入库单弹窗 -->
  <!-- ════════════════════════════════════════════════════════════ -->
  <a-modal
    v-model:open="createVisible"
    title="新建入库单"
    width="800px"
    :confirm-loading="createLoading"
    @ok="handleCreateSubmit"
    @cancel="handleCreateCancel"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="入库类型" name="inboundType">
            <a-select v-model:value="createForm.inboundType" placeholder="入库类型">
              <a-select-option :value="1">采购入库</a-select-option>
              <a-select-option :value="2">采购退货</a-select-option>
              <a-select-option :value="3">销售退货</a-select-option>
              <a-select-option :value="4">调拨入库</a-select-option>
              <a-select-option :value="5">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="入库日期" name="inboundDate">
            <a-date-picker v-model:value="createForm.inboundDate" style="width: 100%" value-format="YYYY-MM-DD" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="仓库" name="warehouseId">
            <a-select v-model:value="createForm.warehouseId" placeholder="选择仓库" show-search :filter-option="filterOption" @change="onWarehouseChange">
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.warehouseName }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="供应商" name="supplierName">
            <a-select v-model:value="createForm.supplierId" placeholder="选择供应商" show-search :filter-option="filterOption" allow-clear @change="onSupplierChange">
              <a-select-option v-for="s in supplierOptions" :key="s.id" :value="s.id">{{ s.name || s.supplierName }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="采购员">
            <a-input v-model:value="createForm.purchaserName" placeholder="采购员姓名" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="采购部门">
            <a-input v-model:value="createForm.departmentName" placeholder="部门名称" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="物流公司">
            <a-input v-model:value="createForm.logisticsCompany" placeholder="物流公司" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="运单号">
            <a-input v-model:value="createForm.trackingNumber" placeholder="运单号" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 入库明细 -->
      <div class="sub-table-header">
        <span class="sub-table-title">入库明细</span>
        <a-button type="dashed" size="small" @click="addItem"><PlusOutlined /> 添加产品</a-button>
      </div>
      <a-table
        :dataSource="createForm.items"
        :columns="itemColumns"
        :pagination="false as any"
        size="small"
        row-key="tempId"
        style="margin-bottom: 12px;"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'productName'">
            <a-input v-model:value="record.productName" placeholder="产品名称" style="width: 120px" />
            <a-tooltip title="选择产品"><a-button size="small" type="link" @click="selectItemProduct(index)"><SearchOutlined /></a-button></a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'productSpec'">
            <a-input v-model:value="record.productSpec" placeholder="规格" style="width: 80px" />
          </template>
          <template v-else-if="column.dataIndex === 'orderQuantity'">
            <a-input-number v-model:value="record.orderQuantity" :min="0" :precision="0" style="width: 80px" />
          </template>
          <template v-else-if="column.dataIndex === 'unitPrice'">
            <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" style="width: 100px" />
          </template>
          <template v-else-if="column.dataIndex === 'taxRate'">
            <a-select v-model:value="record.taxRate" style="width: 80px">
              <a-select-option :value="0">0%</a-select-option>
              <a-select-option :value="0.03">3%</a-select-option>
              <a-select-option :value="0.06">6%</a-select-option>
              <a-select-option :value="0.09">9%</a-select-option>
              <a-select-option :value="0.13">13%</a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button type="link" danger size="small" @click="removeItem(index)"><DeleteOutlined /></a-button>
          </template>
        </template>
      </a-table>

      <a-form-item label="备注">
        <a-textarea v-model:value="createForm.remark" :rows="2" placeholder="备注信息" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 商品选择弹窗 -->
  <a-modal v-model:open="productPickerVisible" title="选择产品" width="640px" :footer="null" destroy-on-close>
    <a-input-search v-model:value="productSearchKeyword" placeholder="搜索产品编码/名称" @search="loadProductOptions" />
    <a-table
      :dataSource="productOptions"
      :columns="productPickerColumns"
      :pagination="{ pageSize: 5 }"
      :loading="productLoading"
      size="small"
      row-key="id"
      style="margin-top: 12px;"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <a-button type="primary" size="small" @click="pickProduct(record)">选择</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, ReloadOutlined, SyncOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, DollarOutlined, WarningOutlined, SearchOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import request from '@/utils/request'
import { INBOUND_STATUS } from '@/utils/statusConfig'

// ── 类型定义 ──────────────────────────────────────────
interface StockInItem {
  tempId?: number
  productId?: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  orderQuantity: number
  unitPrice: number
  taxRate: number
  remark?: string
}

interface StockInOrder {
  id: number
  inboundNo: string
  purchaseOrderNo: string
  supplierName: string
  warehouseName: string
  totalAmount: number
  status: number
  inboundDate: string
  operator: string
  purchaserName?: string
  departmentName?: string
  logisticsCompany?: string
  trackingNumber?: string
  createTime?: string
  remark?: string
  items?: StockInItem[]
}

// ── 防抖工具 ──────────────────────────────────────────
function handleError(err: any) { console.warn('[StockIn]', err) }

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
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); debounceClick('export', handleExport); return }
}

const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const tableRef = ref()

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  completedCount: 0,
  totalAmount: 0
})

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const searchFields: any = [
  { name: 'inboundNo', label: '入库单号', type: 'input', placeholder: '请输入入库单号' },
  { name: 'purchaseOrderNo', label: '采购订单', type: 'input', placeholder: '请输入采购订单号' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择',
    options: Object.entries(INBOUND_STATUS).map(([k, v]) => ({ label: v.text, value: Number(k) }))
  },
]

const searchParams = reactive({
  inboundNo: '',
  purchaseOrderNo: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const vxeColumns: any = computed(() => [
  { field: 'inboundNo', title: '入库单号', width: 150 },
  { field: 'purchaseOrderNo', title: '采购订单', width: 150 },
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'totalAmount', title: '入库金额', width: 120, slotName: 'totalAmountCell' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'inboundDate', title: '入库日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { field: 'action', title: '操作', width: 220, fixed: 'right', type: 'action' },
])

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await request.get('/erp/purchase/inbound/page', {
      params: { ...searchParams, pageNum: pagination.current, pageSize: pagination.pageSize }
    })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    // 更新统计：优先使用API返回的统计数据
    if (data.totalCount !== undefined) {
      statistics.value.totalCount = data.totalCount
      statistics.value.pendingCount = data.pendingCount || 0
      statistics.value.completedCount = data.completedCount || 0
      statistics.value.totalAmount = data.totalAmount || 0
    } else {
      // 兼容：从当前页数据计算
      statistics.value.totalCount = tableData.value.length
      statistics.value.pendingCount = tableData.value.filter((r: any) => r.status === 0).length
      statistics.value.completedCount = tableData.value.filter((r: any) => r.status >= 2).length
      statistics.value.totalAmount = tableData.value.reduce((sum: number, r: any) => sum + (r.totalAmount || 0), 0)
    }
  } catch (error) {
    hasError.value = true
    console.warn('[入库管理] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  }
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    Object.assign(searchParams, values)
  }
  pagination.current = 1
  fetchData()
}
const handleReset = () => {
  Object.assign(searchParams, { inboundNo: '', purchaseOrderNo: '', status: undefined })
  pagination.current = 1
  fetchData()
}
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

// ════════════════════════════════════════════════════════════════
// 新建入库单表单
// ════════════════════════════════════════════════════════════════

let tempIdCounter = 0
function nextTempId() { return ++tempIdCounter }

const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<any>(null)
const createForm = reactive({
  inboundType: 1,
  inboundDate: '',
  warehouseId: undefined as number | undefined,
  warehouseName: '',
  supplierId: undefined as number | undefined,
  supplierName: '',
  purchaserName: '',
  departmentName: '',
  logisticsCompany: '',
  trackingNumber: '',
  remark: '',
  items: [] as any[]
})
const createRules: Record<string, any[]> = {
  inboundType: [{ required: true, message: '请选择入库类型' }],
  inboundDate: [{ required: true, message: '请选择入库日期' }],
  warehouseId: [{ required: true, message: '请选择仓库' }]
}

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 100 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'productSpec', width: 80 },
  { title: '单位', dataIndex: 'productUnit', width: 60 },
  { title: '数量', dataIndex: 'orderQuantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', width: 100 },
  { title: '税率', dataIndex: 'taxRate', width: 80 },
  { title: '操作', dataIndex: 'action', width: 60 }
]

const detailItemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'productSpec', width: 100 },
  { title: '数量', dataIndex: 'orderQuantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'amount', width: 120, align: 'right' },
]

// 仓库选项
const warehouseOptions = ref<any[]>([])
// 供应商选项
const supplierOptions = ref<any[]>([])
// 产品选项
const productOptions = ref<any[]>([])
const productLoading = ref(false)
const productSearchKeyword = ref('')
const productPickerVisible = ref(false)
let pickerTargetIndex = -1

const productPickerColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 130 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '操作', dataIndex: 'action', width: 80 }
]

function filterOption(input: string, option: any) {
  return (option.children?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

function onWarehouseChange(value: number) {
  const w = warehouseOptions.value.find(x => x.id === value)
  if (w) createForm.warehouseName = w.warehouseName
}

function onSupplierChange(value: number | undefined) {
  if (!value) { createForm.supplierName = ''; return }
  const s = supplierOptions.value.find(x => x.id === value)
  if (s) createForm.supplierName = s.name || s.supplierName || ''
}

function addItem() {
  createForm.items.push({
    tempId: nextTempId(),
    productId: undefined,
    productCode: '',
    productName: '',
    productSpec: '',
    productUnit: '',
    orderQuantity: 1,
    unitPrice: 0,
    taxRate: 0.13,
    remark: ''
  })
}

function removeItem(index: number) {
  createForm.items.splice(index, 1)
}

function selectItemProduct(index: number) {
  pickerTargetIndex = index
  productPickerVisible.value = true
  productSearchKeyword.value = ''
  productOptions.value = []
  loadProductOptions()
}

async function loadProductOptions() {
  productLoading.value = true
  try {
    const res = await request.get('/erp/product/list', {
      params: { keyword: productSearchKeyword.value || undefined, pageSize: 50 }
    })
    const data = res?.data ?? res
    productOptions.value = Array.isArray(data) ? data : []
  } catch { productOptions.value = [] }
  finally { productLoading.value = false }
}

function pickProduct(product: any) {
  if (pickerTargetIndex >= 0 && pickerTargetIndex < createForm.items.length) {
    const item = createForm.items[pickerTargetIndex]
    item.productId = product.id
    item.productCode = product.productCode
    item.productName = product.productName
    item.productSpec = product.specification || ''
    item.productUnit = product.unit || ''
  }
  productPickerVisible.value = false
}

async function loadWarehouseOptions() {
  try {
    const res = await request.get('/erp/stock/warehouses')
    const data = res?.data ?? res
    warehouseOptions.value = Array.isArray(data) ? data : []
  } catch { /* ignore */ }
}

async function loadSupplierOptions() {
  try {
    const res = await request.get('/erp/partner/list', {
      params: { partnerType: 'SUPPLIER', pageSize: 200 }
    })
    const data = res?.data ?? res
    supplierOptions.value = Array.isArray(data) ? data : (data?.records || [])
  } catch { /* ignore */ }
}

const handleCreate = () => {
  tempIdCounter = 0
  createForm.inboundType = 1
  createForm.inboundDate = new Date().toISOString().slice(0, 10)
  createForm.warehouseId = undefined
  createForm.warehouseName = ''
  createForm.supplierId = undefined
  createForm.supplierName = ''
  createForm.purchaserName = ''
  createForm.departmentName = ''
  createForm.logisticsCompany = ''
  createForm.trackingNumber = ''
  createForm.remark = ''
  createForm.items = []
  createVisible.value = true
  nextTick(() => createFormRef.value?.resetFields?.())
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch { return }
  if (createForm.items.length === 0) {
    message.warning('请添加入库明细')
    return
  }
  createLoading.value = true
  try {
    await request.post('/erp/purchase/inbound', {
      inboundType: createForm.inboundType,
      inboundDate: createForm.inboundDate,
      warehouseId: createForm.warehouseId,
      warehouseName: createForm.warehouseName,
      supplierId: createForm.supplierId || undefined,
      supplierName: createForm.supplierName,
      purchaserName: createForm.purchaserName || undefined,
      departmentName: createForm.departmentName || undefined,
      logisticsCompany: createForm.logisticsCompany || undefined,
      trackingNumber: createForm.trackingNumber || undefined,
      remark: createForm.remark || undefined,
      items: createForm.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        productSpec: item.productSpec || undefined,
        productUnit: item.productUnit || undefined,
        orderQuantity: item.orderQuantity,
        unitPrice: item.unitPrice || undefined,
        taxRate: item.taxRate || undefined
      }))
    })
    message.success('入库单创建成功')
    createVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[入库管理] 创建失败', err)
    message.error(err?.message || '创建失败，请稍后重试')
  } finally {
    createLoading.value = false
  }
}

const handleCreateCancel = () => {
  createVisible.value = false
}

function handleParentCreate() { handleCreate() }

// ════════════════════════════════════════════════════════════════
const detailData = ref<any>(null)
const detailLoading = ref(false)

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await request.get(`/erp/purchase/inbound/${id}`)
    detailData.value = res.data || null
  } catch (err) {
    console.warn('[入库管理] 获取详情失败', err)
    detailData.value = tableData.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: any) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleApprove = async (record: any) => {
  Modal.confirm({
    title: '审核确认',
    content: '确认审核该入库单吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/purchase/inbound/${record.id}/approve`)
        message.success('审核成功')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[入库管理] 审核失败', error)
        message.error('审核失败')
      }
    }
  })
}

const handleExecuteInbound = async (record: any) => {
  Modal.confirm({
    title: '入库确认',
    content: '确认执行该入库操作吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/purchase/inbound/${record.id}/execute`)
        message.success('入库完成')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[入库管理] 入库操作失败', error)
        message.error('入库操作失败')
      }
    }
  })
}

const handleExport = async () => {
  try {
    const res = await request.get('/erp/purchase/inbound/export', {
      params: { ...searchParams, pageNum: pagination.current, pageSize: pagination.pageSize },
      responseType: 'blob'
    })
    const blob = new Blob([res.data || res], { type: 'application/vnd.ms-excel' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `入库单_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error) {
    console.warn('[入库管理] 导出失败', error)
    message.error('导出失败')
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  loadWarehouseOptions()
  loadSupplierOptions()
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
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener("erp:create", handleParentCreate)
  window.removeEventListener("erp:refresh", fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.stock-in-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.stock-in-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stock-in-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.stock-in-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.stock-in-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

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

.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }

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
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
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
