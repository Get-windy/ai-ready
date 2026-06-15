<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="transfer-header">
        <div class="transfer-header__left">
          <span class="transfer-header__breadcrumb">ERP / 库存管理 / 库存调拨</span>
          <h2 class="transfer-header__title">库存调拨</h2>
        </div>
        <div class="transfer-header__right">
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
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <SwapOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">调拨单总数</div>
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
            <div class="summary-title">待审批</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <AuditOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已审批</div>
            <div class="summary-value">{{ statistics.approvedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已执行</div>
            <div class="summary-value">{{ statistics.executedCount }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新建调拨单"
      style="flex: 1;"
      @add="handleCreate"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss') }}</span>
        <a-button v-if="selectedIds.length > 0" size="small" danger v-permission="'erp:stock:batchdelete'" @click="handleBatchDelete">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
      </template>

      <template #empty>
        <div v-if="hasError" class="table-empty">
          <WarningOutlined class="table-empty-icon" />
          <p class="table-empty-text">数据加载异常，请重试</p>
          <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
        </div>
        <div v-else class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <SwapOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的调拨单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无调拨单数据，点击右上角「新建调拨单」开始创建
          </p>
        </div>
      </template>

      <template #statusCell="{ record }">
        <StatusTag :status="record.status" :map="TRANSFER_STATUS" />
      </template>

      <template #totalAmountCell="{ record }">
        ¥{{ (record.totalAmount || 0).toFixed(2) }}
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看">
            <a-button type="link" size="small" v-permission="'erp:stock:view'" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 草稿 -> 提交审批 -->
          <a-tooltip v-if="record.status === 0" title="提交审批">
            <a-button type="link" size="small" style="color: #1890ff;" v-permission="'erp:stock:submit'" @click="handleSubmit(record)">
              <template #icon><SendOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 待审批 -> 审批/拒绝 -->
          <template v-if="record.status === 1">
            <a-tooltip title="审批通过">
              <a-button type="link" size="small" style="color: #52c41a;" v-permission="'erp:stock:approve'" @click="handleApprove(record)">
                <template #icon><CheckOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="拒绝">
              <a-button type="link" size="small" style="color: #ff4d4f;" v-permission="'erp:stock:reject'" @click="handleReject(record)">
                <template #icon><CloseOutlined /></template>
              </a-button>
            </a-tooltip>
          </template>
          <!-- 已审批 -> 执行调拨 -->
          <a-tooltip v-if="record.status === 2" title="执行调拨">
            <a-button type="link" size="small" style="color: #1890ff;" v-permission="'erp:stock:execute'" @click="handleExecute(record)">
              <template #icon><AuditOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 已执行 -> 打印 -->
          <PrintButton
            v-if="record.status === 3"
            template-type="stock_transfer"
            :business-id="record.id"
            business-type="stock_transfer"
            button-size="small"
            @print-success="() => message.success(`调拨单 ${record.transferNo} 打印成功`)"
            @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
          />
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="(e) => handleActionMenuClick(String(e.key), record)">
                <a-menu-item v-if="[0, 4, 5].includes(record.status)" key="delete">
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      title="调拨单详情"
      placement="right"
      width="80vw"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="调拨单号">{{ detailData.transferNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="TRANSFER_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="调出仓库">{{ detailData.fromWarehouseName }}</a-descriptions-item>
          <a-descriptions-item label="调入仓库">{{ detailData.toWarehouseName }}</a-descriptions-item>
          <a-descriptions-item label="调拨数量">{{ detailData.totalQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="调拨金额">¥{{ (detailData.totalAmount || 0).toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="申请人">{{ detailData.applicantName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime ? dayjs(detailData.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <template v-if="detailData">
          <h4 style="margin: 16px 0 8px;">调拨明细</h4>
          <a-table
            :data-source="detailItems"
            :columns="detailItemColumns"
            :pagination="false as any"
            size="small"
            bordered
            row-key="id"
          />
        </template>
      </a-spin>
      <template #footer v-if="detailData">
        <a-space>
          <a-button @click="detailVisible = false">关闭</a-button>
          <a-button v-if="detailData.status === 0" v-permission="'erp:stock:submit'" @click="handleSubmit(detailData)">
            <template #icon><SendOutlined /></template>
            提交审批
          </a-button>
          <template v-if="detailData.status === 1">
            <a-button type="primary" v-permission="'erp:stock:approve'" @click="handleApprove(detailData)">
              <template #icon><CheckOutlined /></template>
              审批通过
            </a-button>
            <a-button danger v-permission="'erp:stock:reject'" @click="handleReject(detailData)">
              <template #icon><CloseOutlined /></template>
              拒绝
            </a-button>
          </template>
          <a-button v-if="detailData.status === 2" type="primary" v-permission="'erp:stock:execute'" @click="handleExecute(detailData)">
            <template #icon><AuditOutlined /></template>
            执行调拨
          </a-button>
          <PrintButton
            v-if="detailData.status >= 3"
            template-type="stock_transfer"
            :business-id="detailData.id"
            business-type="stock_transfer"
            button-text="打印"
            button-size="small"
          />
        </a-space>
      </template>
    </a-drawer>
  </PageContainer>

  <!-- 新建调拨单弹窗 -->
  <FullScreenDetail
    :visible="createModalVisible"
    title="新建调拨单"
    :save-loading="submitLoading"
    @close="handleCreateCancel"
    @save="handleCreateSubmit"
  >
    <a-form
      ref="createFormRef"
      :model="createForm"
      :rules="formRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="调出仓库" name="fromWarehouseId">
            <a-select
              v-model:value="createForm.fromWarehouseId"
              placeholder="请选择调出仓库"
              show-search
              :filter-option="(input: string, option: any) => option.label?.toLowerCase().includes(input.toLowerCase())"
              allow-clear
              size="small"
            >
              <a-select-option v-for="w in warehouseOptions" :key="w.value" :value="w.value">{{ w.label }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="调入仓库" name="toWarehouseId">
            <a-select
              v-model:value="createForm.toWarehouseId"
              placeholder="请选择调入仓库"
              show-search
              :filter-option="(input: string, option: any) => option.label?.toLowerCase().includes(input.toLowerCase())"
              allow-clear
              size="small"
            >
              <a-select-option v-for="w in warehouseOptions" :key="w.value" :value="w.value">{{ w.label }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="createForm.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>

      <div class="sub-table-header">
        <span class="sub-table-title">调拨明细</span>
        <a-button type="dashed" size="small" @click="addItem"><PlusOutlined /> 添加产品</a-button>
      </div>
      <a-table
        :data-source="createForm.items"
        :columns="itemColumns"
        :pagination="false as any"
        size="small"
        row-key="tempId"
        style="margin-bottom: 12px;"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.dataIndex === 'productName'">
            <a-input v-model:value="record.productName" placeholder="产品名称" style="width: 130px" size="small" />
            <a-tooltip title="选择产品"><a-button size="small" type="link" @click="selectItemProduct(index)"><SearchOutlined /></a-button></a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'productCode'">
            <a-input v-model:value="record.productCode" placeholder="编码" style="width: 90px" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'specification'">
            <span>{{ record.specification || '-' }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'unit'">
            <span>{{ record.unit || '-' }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" :precision="0" style="width: 80px" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button type="link" danger size="small" @click="removeItem(index)"><DeleteOutlined /></a-button>
          </template>
        </template>
      </a-table>
    </a-form>
  </FullScreenDetail>

  <!-- 产品选择弹窗 -->
  <a-modal v-model:open="productPickerVisible" title="选择产品" width="640px" :footer="null" destroy-on-close>
    <a-input-search v-model:value="productSearchKeyword" placeholder="搜索产品编码/名称" @search="loadProductOptions" size="small" />
    <a-table
      :data-source="productOptions"
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

  <!-- 理由输入弹窗（拒绝） -->
  <a-modal
    v-model:open="reasonModalVisible"
    :title="reasonModalTitle"
    width="480px"
    :confirm-loading="reasonLoading"
    @ok="handleReasonConfirm"
    @cancel="handleReasonCancel"
    destroy-on-close
  >
    <a-textarea
      v-model:value="reasonText"
      :placeholder="reasonPlaceholder"
      :rows="4"
    />
  </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { message, Modal } from 'ant-design-vue'
import request from '@/utils/request'
import {
  PlusOutlined, ReloadOutlined, SyncOutlined, EyeOutlined,
  CheckOutlined, CloseOutlined, SendOutlined, AuditOutlined,
  DeleteOutlined, SearchOutlined, SwapOutlined,
  ClockCircleOutlined, WarningOutlined,
  EllipsisOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { TRANSFER_STATUS } from '@/utils/statusConfig'

// ── 类型定义 ──────────────────────────────────────────
interface TransferItem {
  tempId?: number
  id?: number
  productId?: number
  productCode: string
  productName: string
  specification: string
  unit: string
  quantity: number
}

interface TransferOrder {
  id: number
  transferNo: string
  fromWarehouseName: string
  toWarehouseName: string
  totalQuantity: number
  totalAmount: number
  status: number
  applicantName: string
  remark?: string
  createTime?: string
  items?: TransferItem[]
}

// ── 防抖与错误处理 ─────────────────────────────────────
function handleError(err: any) { console.warn('[库存调拨] ErrorBoundary 捕获异常:', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 状态变量 ──────────────────────────────────────────
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const dataSource = ref<TransferOrder[]>([])
const detailVisible = ref(false)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())
const selectedRows = ref<TransferOrder[]>([])
const selectedIds = ref<number[]>([])

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ──────────────────────────────────────────
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  approvedCount: 0,
  executedCount: 0
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const tableDataSource = dataSource

// ── 列定义 ──────────────────────────────────────────
const vxeColumns: any = computed(() => [
  { field: 'transferNo', title: '调拨单号', width: 150 },
  { field: 'fromWarehouseName', title: '调出仓库', width: 120 },
  { field: 'toWarehouseName', title: '调入仓库', width: 120 },
  { field: 'totalQuantity', title: '调拨数量', width: 90, align: 'center' },
  { field: 'totalAmount', title: '调拨金额', width: 120, slotName: 'totalAmountCell' },
  { field: 'status', title: '状态', width: 100, align: 'center', slotName: 'statusCell' },
  { field: 'applicantName', title: '申请人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' }
])

// ── 仓库选项 ──────────────────────────────────────────
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const loadWarehouses = async () => {
  try {
    const res = await request.get('/erp/warehouse/list')
    const list = res?.data || []
    warehouseOptions.value = list.map((w: any) => ({ label: w.name, value: w.id }))
  } catch (err) {
    console.warn('[库存调拨] 加载仓库列表失败', err)
  }
}

// ── 筛选字段 ──────────────────────────────────────────
const filterFields = computed(() => [
  { key: 'keyword', label: '关键字', type: 'input' as const, placeholder: '请输入调拨单号/关键字' },
  { key: 'fromWarehouseId', label: '调出仓库', type: 'select' as const, options: warehouseOptions.value },
  { key: 'toWarehouseId', label: '调入仓库', type: 'select' as const, options: warehouseOptions.value },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '已执行', value: 3 },
    { label: '已拒绝', value: 4 },
    { label: '已取消', value: 5 }
  ]}
])

// ── 搜索/筛选/分页 ────────────────────────────────────
const handleSearch = () => {
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

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleSelectionChange = (rows: TransferOrder[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

// ── 获取数据 ──────────────────────────────────────────
const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const params: Record<string, any> = {
      keyword: searchFilters.keyword || undefined,
      fromWarehouseId: searchFilters.fromWarehouseId,
      toWarehouseId: searchFilters.toWarehouseId,
      status: searchFilters.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/erp/stock/transfer/page', { params })
    if (res.data?.records) {
      dataSource.value = res.records.map((item: any) => ({
        id: item.id,
        transferNo: item.transferNo,
        fromWarehouseName: item.fromWarehouseName,
        toWarehouseName: item.toWarehouseName,
        totalQuantity: item.totalQuantity || 0,
        totalAmount: item.totalAmount || 0,
        status: item.status,
        applicantName: item.applicantName || '',
        remark: item.remark || '',
        createTime: item.createTime
      }))
      pagination.total = res.total || 0
      // 优先使用 API 返回的全局统计数据
      if (res.totalCount !== undefined) {
        statistics.value.totalCount = res.totalCount
        statistics.value.pendingCount = res.pendingCount || 0
        statistics.value.approvedCount = res.approvedCount || 0
        statistics.value.executedCount = res.executedCount || 0
      } else {
        statistics.value.totalCount = dataSource.value.length
        statistics.value.pendingCount = dataSource.value.filter(item => item.status === 1).length
        statistics.value.approvedCount = dataSource.value.filter(item => item.status === 2).length
        statistics.value.executedCount = dataSource.value.filter(item => item.status === 3).length
      }
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdated.value = new Date().toISOString()
  } catch (error) {
    hasError.value = true
    console.warn('[库存调拨] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
}

function handleParentCreate() { handleCreate() }

// ── 新建调拨单 ──────────────────────────────────────────
const createModalVisible = ref(false)
const submitLoading = ref(false)
const createFormRef = ref()
let tempIdCounter = 0
function nextTempId() { return ++tempIdCounter }

const createForm = reactive({
  fromWarehouseId: undefined as number | undefined,
  toWarehouseId: undefined as number | undefined,
  remark: '',
  items: [] as TransferItem[]
})

const formRules: Record<string, any[]> = {
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }]
}

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 100 },
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '规格', dataIndex: 'specification', width: 80 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '调拨数量', dataIndex: 'quantity', width: 100 },
  { title: '操作', dataIndex: 'action', width: 60 }
]

const detailItemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '调拨数量', dataIndex: 'quantity', width: 80, align: 'right' }
]

const handleCreate = () => {
  tempIdCounter = 0
  createForm.fromWarehouseId = undefined
  createForm.toWarehouseId = undefined
  createForm.remark = ''
  createForm.items = []
  createModalVisible.value = true
  nextTick(() => createFormRef.value?.resetFields?.())
}

function addItem() {
  createForm.items.push({
    tempId: nextTempId(),
    productId: undefined,
    productCode: '',
    productName: '',
    specification: '',
    unit: '',
    quantity: 1
  })
}

function removeItem(index: number) {
  createForm.items.splice(index, 1)
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch { return }

  if (createForm.items.length === 0) {
    message.warning('请添加调拨明细')
    return
  }
  const invalidItem = createForm.items.find(i => !i.productId)
  if (invalidItem) {
    message.warning('请完善调拨明细中的产品信息')
    return
  }
  if (createForm.fromWarehouseId === createForm.toWarehouseId) {
    message.warning('调出仓库和调入仓库不能相同')
    return
  }

  submitLoading.value = true
  try {
    await request.post('/erp/stock/transfer', {
      fromWarehouseId: createForm.fromWarehouseId,
      toWarehouseId: createForm.toWarehouseId,
      remark: createForm.remark || undefined,
      items: createForm.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        specification: item.specification || undefined,
        unit: item.unit || undefined,
        quantity: item.quantity
      }))
    })
    message.success('调拨单创建成功')
    createModalVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[库存调拨] 创建失败', err)
    message.error(err?.message || '创建失败，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

const handleCreateCancel = () => {
  createModalVisible.value = false
}

// ── 产品选择 ──────────────────────────────────────────
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

function selectItemProduct(index: number) {
  pickerTargetIndex = index
  productPickerVisible.value = true
  productSearchKeyword.value = ''
  productOptions.value = []
  loadProductOptions()
}

function pickProduct(product: any) {
  if (pickerTargetIndex >= 0 && pickerTargetIndex < createForm.items.length) {
    const item = createForm.items[pickerTargetIndex]
    item.productId = product.id
    item.productCode = product.productCode
    item.productName = product.productName
    item.specification = product.specification || ''
    item.unit = product.unit || ''
  }
  productPickerVisible.value = false
}

// ── 详情 ──────────────────────────────────────────
const detailData = ref<TransferOrder | null>(null)
const detailLoading = ref(false)
const detailItems = ref<any[]>([])

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  detailItems.value = []
  try {
    const res = await request.get(`/erp/stock/transfer/${id}`)
    detailData.value = res.data || null
    // 获取调拨明细
    try {
      const itemsRes = await request.get(`/erp/stock/transfer/${id}/items`)
      detailItems.value = itemsRes?.data || []
    } catch {
      detailItems.value = []
    }
  } catch (err) {
    console.warn('[库存调拨] 获取详情失败', err)
    detailData.value = dataSource.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: TransferOrder) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

// ── 操作：提交审批 ────────────────────────────────────
const handleSubmit = async (record: TransferOrder) => {
  Modal.confirm({
    title: '提交审批',
    content: `确认提交调拨单 ${record.transferNo} 进行审批吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/transfer/${record.id}/submit`)
        message.success('提交成功')
        detailVisible.value = false
        fetchData()
      } catch (error) {
        console.warn('[库存调拨] 提交失败', error)
        message.error('提交失败')
      }
    }
  })
}

// ── 操作：审批通过 ────────────────────────────────────
const handleApprove = async (record: TransferOrder) => {
  Modal.confirm({
    title: '审批确认',
    content: `确认审批通过调拨单 ${record.transferNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/transfer/${record.id}/approve`)
        message.success('审批通过')
        detailVisible.value = false
        fetchData()
      } catch (error) {
        console.warn('[库存调拨] 审批失败', error)
        message.error('审批失败')
      }
    }
  })
}

// ── 操作：拒绝（需输入原因） ────────────────────────────
const reasonModalVisible = ref(false)
const reasonModalTitle = ref('')
const reasonText = ref('')
const reasonPlaceholder = ref('')
const reasonLoading = ref(false)
let pendingReasonRecord: TransferOrder | null = null

const handleReject = (record: TransferOrder) => {
  pendingReasonRecord = record
  reasonModalTitle.value = '拒绝调拨单'
  reasonText.value = ''
  reasonPlaceholder.value = '请输入拒绝原因（必填）'
  reasonModalVisible.value = true
}

const handleReasonConfirm = async () => {
  if (!pendingReasonRecord) return
  if (!reasonText.value.trim()) {
    message.warning('请输入原因')
    return
  }
  reasonLoading.value = true
  try {
    await request.post(`/erp/stock/transfer/${pendingReasonRecord.id}/reject`, { reason: reasonText.value.trim() })
    message.success('已拒绝')
    reasonModalVisible.value = false
    detailVisible.value = false
    pendingReasonRecord = null
    fetchData()
  } catch (error) {
    console.warn('[库存调拨] 拒绝失败', error)
    message.error('拒绝失败')
  } finally {
    reasonLoading.value = false
  }
}

const handleReasonCancel = () => {
  reasonModalVisible.value = false
  pendingReasonRecord = null
}

// ── 操作：执行调拨 ────────────────────────────────────
const handleExecute = async (record: TransferOrder) => {
  Modal.confirm({
    title: '执行调拨确认',
    content: `确认执行调拨单 ${record.transferNo} 吗？执行后将更新库存。`,
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/transfer/${record.id}/execute`)
        message.success('调拨执行成功')
        detailVisible.value = false
        fetchData()
      } catch (error) {
        console.warn('[库存调拨] 执行失败', error)
        message.error('执行失败')
      }
    }
  })
}

// ── 操作：删除 ────────────────────────────────────────
const handleDelete = async (record: TransferOrder) => {
  try {
    await request.delete(`/erp/stock/transfer/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[库存调拨] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: TransferOrder) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该调拨单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

// ── 操作：批量删除 ────────────────────────────────────
const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedIds.value.length} 条调拨单吗？删除后数据不可恢复。`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.delete('/erp/stock/transfer/batch', {
          params: { ids: selectedIds.value.join(',') }
        })
        message.success('批量删除成功')
        selectedIds.value = []
        selectedRows.value = []
        fetchData()
      } catch (error) {
        console.warn('[库存调拨] 批量删除失败', error)
        message.error('批量删除失败')
      }
    }
  })
}

// ── 生命周期 ──────────────────────────────────────────
onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('erp:create', handleParentCreate)
  window.addEventListener('erp:refresh', fetchData)
  loadWarehouses()
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
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('erp:create', handleParentCreate)
  window.removeEventListener('erp:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.transfer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.transfer-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.transfer-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.transfer-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.transfer-header__right {
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

.sub-table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.sub-table-title {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
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
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7e6 100%);
  border: 1px solid #b7eb8f;
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
