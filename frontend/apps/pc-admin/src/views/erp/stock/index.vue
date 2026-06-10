<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="stock-page-header">
        <div class="stock-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>库存管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="stock-page-title">库存管理</h2>
        </div>
        <div class="stock-page-header-right">
          <a-switch size="small" v-model:checked="autoRefreshEnabled" checked-children="自动" un-checked-children="手动" @change="handleAutoRefreshChange" />
          <span v-if="autoRefreshEnabled && autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
          <a-button size="small" :loading="loading" @click="debounceClick('refresh', fetchData)">
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
            <DatabaseOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">总库存SKU</div>
            <div class="summary-value">{{ statistics.totalSku }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <AlertOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">低库存预警</div>
            <div class="summary-value warning">{{ statistics.lowStockCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);">
            <ExclamationCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">超储预警</div>
            <div class="summary-value warning">{{ statistics.overStockCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">正常库存</div>
            <div class="summary-value">{{ statistics.normalCount }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="库存管理" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <!-- 搜索区域 -->
      <SearchBar
        :fields="stockSearchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleInbound">
            <template #icon><LoginOutlined /></template>
            入库
          </a-button>
          <a-button @click="handleOutbound">
            <template #icon><LogoutOutlined /></template>
            出库
          </a-button>
          <a-button @click="handleStocktake">
            <template #icon><AuditOutlined /></template>
            盘点
          </a-button>
          <a-button @click="handleLock">
            <template #icon><LockOutlined /></template>
            锁定
          </a-button>
          <a-button @click="handleExport">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
        </a-space>
      </div>

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
          <div v-if="hasError" class="table-empty">
            <WarningOutlined class="table-empty-icon" />
            <p class="table-empty-text">数据加载异常，请重试</p>
            <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
          </div>
          <EmptyState v-else title="暂无数据" description="暂无库存数据" size="small" :show-actions="false" />
        </template>
        <template #quantityCell="{ record }">
          <span :class="getStockClass(record)">
            {{ record.quantity }} {{ record.unit }}
          </span>
        </template>
        <template #warningStatusCell="{ record }">
          <StatusTag :status="getStockWarningStatusKey(record)" :map="STOCK_WARNING_STATUS" />
        </template>
        <template #action="{ record }">
          <a-space :size="4">
            <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="handleStockLog(record)">库存明细</a-button>
            <a-dropdown>
              <a-button type="link" size="small">
                库存操作 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleFreezeStock(record)">
                    <LockOutlined /> 冻结库存
                  </a-menu-item>
                  <a-menu-item @click="handleUnfreezeStock(record)">
                    <UnlockOutlined /> 解冻库存
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 库存明细弹窗 -->
    <a-modal v-model:open="logModalVisible" title="库存明细" :footer="null" width="800px">
      <VxeTableList
        :columns="logVxeColumns"
        :data-source="stockLogs"
        :show-toolbar="false"
        :selectable="false"
        :pagination="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #typeCell="{ record }">
          <a-tag :color="record.type === 'in' ? 'green' : 'red'">{{ record.type === 'in' ? '入库' : '出库' }}</a-tag>
        </template>
      </VxeTableList>
    </a-modal>

    <!-- 库存详情弹窗 -->
    <a-drawer v-model:open="detailVisible" title="库存详情" placement="right" width="80vw">
      <div style="text-align: right; margin-bottom: 12px;">
        <PrintButton :record="detailData" business-type="STOCK" button-size="small" />
      </div>
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="商品编码">{{ detailData.productCode }}</a-descriptions-item>
          <a-descriptions-item label="商品名称">{{ detailData.productName }}</a-descriptions-item>
          <a-descriptions-item label="规格">{{ detailData.specification }}</a-descriptions-item>
          <a-descriptions-item label="单位">{{ detailData.unit }}</a-descriptions-item>
          <a-descriptions-item label="库存数量">
            <span :class="getStockClass(detailData)">{{ detailData.quantity }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="最低库存">{{ detailData.minStock }}</a-descriptions-item>
          <a-descriptions-item label="最高库存">{{ detailData.maxStock }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="最后入库">{{ detailData.lastInboundDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="最后出库">{{ detailData.lastOutboundDate || '-' }}</a-descriptions-item>
        </a-descriptions>
      </a-spin>
    </a-drawer>

    <!-- 编辑库存弹窗 -->
    <a-modal v-model:open="editModalVisible" title="编辑库存" @ok="handleEditSubmit" :confirm-loading="editLoading" destroy-on-close>
      <a-form :model="editForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="商品编码">{{ editForm.productCode }}</a-form-item>
        <a-form-item label="商品名称">{{ editForm.productName }}</a-form-item>
        <a-form-item label="最低库存">
          <a-input-number v-model:value="editForm.minStock" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="最高库存">
          <a-input-number v-model:value="editForm.maxStock" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 冻结库存弹窗 -->
    <a-modal v-model:open="freezeModalVisible" title="冻结库存" @ok="handleFreezeSubmit" :confirm-loading="freezeLoading" destroy-on-close>
      <a-form :model="lockForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="商品编码">{{ lockForm.productCode }}</a-form-item>
        <a-form-item label="商品名称">{{ lockForm.productName }}</a-form-item>
        <a-form-item label="仓库">{{ lockForm.warehouseName }}</a-form-item>
        <a-form-item label="可用库存">
          <span style="color: #52c41a; font-weight: bold;">{{ lockForm.availableQuantity }}</span>
        </a-form-item>
        <a-form-item label="已冻结">
          <span style="color: #faad14;">{{ lockForm.frozenQuantity }}</span>
        </a-form-item>
        <a-form-item label="冻结数量" required>
          <a-input-number v-model:value="lockForm.quantity" :min="1" :max="lockForm.availableQuantity" style="width: 100%" placeholder="输入冻结数量" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 解冻库存弹窗 -->
    <a-modal v-model:open="unfreezeModalVisible" title="解冻库存" @ok="handleUnfreezeSubmit" :confirm-loading="unfreezeLoading" destroy-on-close>
      <a-form :model="lockForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="商品编码">{{ lockForm.productCode }}</a-form-item>
        <a-form-item label="商品名称">{{ lockForm.productName }}</a-form-item>
        <a-form-item label="仓库">{{ lockForm.warehouseName }}</a-form-item>
        <a-form-item label="已冻结数量">
          <span style="color: #faad14; font-weight: bold;">{{ lockForm.frozenQuantity }}</span>
        </a-form-item>
        <a-form-item label="解冻数量" required>
          <a-input-number v-model:value="lockForm.quantity" :min="1" :max="lockForm.frozenQuantity" style="width: 100%" placeholder="输入解冻数量" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { LoginOutlined, LogoutOutlined, AuditOutlined, ExportOutlined, DatabaseOutlined, AlertOutlined, ExclamationCircleOutlined, CheckCircleOutlined, SyncOutlined, ReloadOutlined, WarningOutlined, LockOutlined, UnlockOutlined, DownOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import { stockApi } from '@/api/erp'
import request from '@/utils/request'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'

// ── 类型定义 ──────────────────────────────────────────
export interface StockItem {
  id: number
  productCode: string
  productName: string
  specification?: string
  unit: string
  quantity: number
  frozenQuantity: number
  availableQuantity: number
  minStock: number
  maxStock: number
  warehouseName: string
  warehouseId?: number
  lastInboundDate?: string
  lastOutboundDate?: string
  warningStatus?: string
}

export interface StockStatistics {
  totalSku: number
  lowStockCount: number
  overStockCount: number
  normalCount: number
}

export interface StockLogItem {
  id: number
  type: 'in' | 'out'
  quantity: number
  orderNo: string
  time: string
  operator: string
}

// ── 库存预警状态映射 ──────────────────────────────────
const STOCK_WARNING_STATUS = {
  low: { text: '低库存', color: 'red' },
  over: { text: '超储', color: 'orange' },
  normal: { text: '正常', color: 'green' }
} as const

function getStockWarningStatusKey(record: any): string {
  if (record.quantity < (record.minStock ?? 0)) return 'low'
  if (record.quantity > (record.maxStock ?? 999999)) return 'over'
  return 'normal'
}

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
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleInbound(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); handleExport(); return }
}

const router = useRouter()
const loading = ref(false)
const lastUpdateTime = ref('')
const hasError = ref(false)
const tableData = ref<any[]>([])
const logModalVisible = ref(false)
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const stockLogs = ref<any[]>([])
const warehouseOptions = ref<{ id: number; name: string }[]>([])

// 统计数据
const statistics = ref({
  totalSku: 0,
  lowStockCount: 0,
  overStockCount: 0,
  normalCount: 0
})

function handleError(err: any) { hasError.value = true; console.warn("[库存管理] ErrorBoundary 捕获异常:", err) }
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const tableRef = ref()

const searchParams = reactive({
  productCode: '',
  productName: '',
  warehouseId: undefined as number | undefined
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// ── 搜索配置 ────────────────────────────────────────
const stockSearchFields = computed<SearchField[]>(() => [
  { name: 'productCode', label: '商品编码', type: 'input', placeholder: '请输入商品编码' },
  { name: 'productName', label: '商品名称', type: 'input', placeholder: '请输入商品名称' },
  { name: 'warehouseId', label: '仓库', type: 'select', placeholder: '请选择',
    options: warehouseOptions.value.map(w => ({ label: w.name, value: w.id })) },
])

// ── 锁定/解冻状态 ────────────────────────────────────
const freezeModalVisible = ref(false)
const unfreezeModalVisible = ref(false)
const freezeLoading = ref(false)
const unfreezeLoading = ref(false)
const lockForm = reactive({
  productId: 0,
  warehouseId: 0,
  productCode: '',
  productName: '',
  warehouseName: '',
  availableQuantity: 0,
  frozenQuantity: 0,
  quantity: 1
})

function resetLockForm() {
  lockForm.productId = 0
  lockForm.warehouseId = 0
  lockForm.productCode = ''
  lockForm.productName = ''
  lockForm.warehouseName = ''
  lockForm.availableQuantity = 0
  lockForm.frozenQuantity = 0
  lockForm.quantity = 1
}

const handleFreezeStock = (record: any) => {
  resetLockForm()
  lockForm.productId = record.productId || record.id
  lockForm.warehouseId = record.warehouseId
  lockForm.productCode = record.productCode
  lockForm.productName = record.productName
  lockForm.warehouseName = record.warehouseName
  lockForm.availableQuantity = record.availableQuantity ?? (record.quantity - (record.frozenQuantity || 0))
  lockForm.frozenQuantity = record.frozenQuantity || 0
  lockForm.quantity = 1
  freezeModalVisible.value = true
}

const handleFreezeSubmit = async () => {
  if (!lockForm.quantity || lockForm.quantity <= 0) {
    message.warning('请输入有效的冻结数量')
    return
  }
  if (lockForm.quantity > lockForm.availableQuantity) {
    message.warning('冻结数量不能超过可用库存')
    return
  }
  freezeLoading.value = true
  try {
    await request.post('/erp/stock/freeze', null, {
      params: {
        productId: lockForm.productId,
        warehouseId: lockForm.warehouseId,
        quantity: lockForm.quantity
      }
    })
    message.success('冻结成功')
    freezeModalVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[库存管理] 冻结失败', err)
    message.error(err?.message || '冻结失败')
  } finally {
    freezeLoading.value = false
  }
}

const handleUnfreezeStock = (record: any) => {
  resetLockForm()
  lockForm.productId = record.productId || record.id
  lockForm.warehouseId = record.warehouseId
  lockForm.productCode = record.productCode
  lockForm.productName = record.productName
  lockForm.warehouseName = record.warehouseName
  lockForm.availableQuantity = record.availableQuantity ?? (record.quantity - (record.frozenQuantity || 0))
  lockForm.frozenQuantity = record.frozenQuantity || 0
  lockForm.quantity = 1
  unfreezeModalVisible.value = true
}

const handleUnfreezeSubmit = async () => {
  if (!lockForm.quantity || lockForm.quantity <= 0) {
    message.warning('请输入有效的解冻数量')
    return
  }
  if (lockForm.quantity > lockForm.frozenQuantity) {
    message.warning('解冻数量不能超过已冻结数量')
    return
  }
  unfreezeLoading.value = true
  try {
    await request.post('/erp/stock/unfreeze', null, {
      params: {
        productId: lockForm.productId,
        warehouseId: lockForm.warehouseId,
        quantity: lockForm.quantity
      }
    })
    message.success('解冻成功')
    unfreezeModalVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[库存管理] 解冻失败', err)
    message.error(err?.message || '解冻失败')
  } finally {
    unfreezeLoading.value = false
  }
}

// ── 编辑状态 ────────────────────────────────────────
const editModalVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: 0,
  productCode: '',
  productName: '',
  minStock: 0,
  maxStock: 999999
})

const vxeColumns = computed(() => [
  { field: 'productCode', title: '商品编码', width: 130 },
  { field: 'productName', title: '商品名称', width: 150 },
  { field: 'specification', title: '规格', width: 100 },
  { field: 'quantity', title: '库存数量', width: 110, slotName: 'quantityCell' },
  { field: 'availableQuantity', title: '可用数量', width: 90, align: 'center' },
  { field: 'frozenQuantity', title: '冻结数量', width: 90, align: 'center' },
  { field: 'minStock', title: '最低库存', width: 90, align: 'center' },
  { field: 'maxStock', title: '最高库存', width: 90, align: 'center' },
  { field: 'warningStatus', title: '预警', width: 80, align: 'center', slotName: 'warningStatusCell' },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'lastInboundDate', title: '最后入库', width: 110 },
  { field: 'lastOutboundDate', title: '最后出库', width: 110 },
  { field: 'action', title: '操作', width: 200, fixed: 'right', type: 'action' },
])

const logVxeColumns = computed(() => [
  { field: 'type', title: '类型', width: 80, slotName: 'typeCell' },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'orderNo', title: '关联单号', width: 160 },
  { field: 'time', title: '时间', width: 160 },
  { field: 'operator', title: '操作人', width: 100 },
])

const getStockClass = (record: any) => ({
  'low-stock': record.quantity < (record.minStock ?? 0),
  'over-stock': record.quantity > (record.maxStock ?? 999999)
})

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await stockApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchParams
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
    // 更新统计
    statistics.value.totalSku = tableData.value.length
    statistics.value.lowStockCount = tableData.value.filter(r => r.quantity < (r.minStock ?? 0)).length
    statistics.value.overStockCount = tableData.value.filter(r => r.quantity > (r.maxStock ?? 999999)).length
    statistics.value.normalCount = tableData.value.filter(r => r.quantity >= (r.minStock ?? 0) && r.quantity <= (r.maxStock ?? 999999)).length
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (err: any) {
    hasError.value = true
    console.warn('[库存管理] 加载数据失败', err)
    message.error(err?.message || '加载库存数据失败')
  } finally {
    loading.value = false
  }
}

function handleParentCreate() { handleInbound() }

const handleRefresh = () => {
  autoRefreshCountdown.value = 30
  fetchData()
}

const handleAutoRefreshChange = (checked: boolean) => {
  if (checked) {
    autoRefreshCountdown.value = 30
  } else {
    autoRefreshCountdown.value = 0
  }
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    searchParams.productCode = values.productCode || ''
    searchParams.productName = values.productName || ''
    searchParams.warehouseId = values.warehouseId
  }
  pagination.current = 1
  fetchData()
}
const handleReset = () => {
  Object.assign(searchParams, { productCode: '', productName: '', warehouseId: undefined })
  pagination.current = 1
  fetchData()
}
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

const handleInbound = () => router.push('/erp/stock-in')
const handleOutbound = () => router.push('/erp/outbound')
const handleStocktake = () => router.push('/erp/stocktake')

const detailData = ref<any>(null)
const detailLoading = ref(false)

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const res = await request.get(`/erp/stock/${id}`)
    detailData.value = res.data || null
  } catch (err) {
    console.warn('[库存管理] 获取详情失败', err)
    detailData.value = tableData.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: any) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleStockLog = async (record: any) => {
  try {
    const res = await request.get(`/erp/stock/${record.id}/logs`)
    stockLogs.value = res?.data || []
    logModalVisible.value = true
  } catch (err) { console.warn('[库存管理] 获取库存流水失败', err); message.warning('暂无库存流水数据') }
}

const handleLock = () => {
  if (tableData.value.length === 0) {
    message.warning('暂无库存数据可锁定')
    return
  }
  // 默认选中第一条记录打开冻结弹窗
  handleFreezeStock(tableData.value[0])
}

const handleExport = async () => {
  try {
    const blob = await request.get('/erp/stock/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `库存_${new Date().toISOString().slice(0, 10)}.xlsx`; a.click()
    window.URL.revokeObjectURL(url); message.success('导出成功')
  } catch (err) { console.warn('[库存管理] 导出失败', err); message.warning('导出失败') }
}

const handleEdit = (record: any) => {
  editForm.id = record.id
  editForm.productCode = record.productCode
  editForm.productName = record.productName
  editForm.minStock = record.minStock ?? 0
  editForm.maxStock = record.maxStock ?? 999999
  editModalVisible.value = true
}

const handleEditSubmit = async () => {
  editLoading.value = true
  try {
    await request.put(`/erp/stock/${editForm.id}`, {
      minStock: editForm.minStock,
      maxStock: editForm.maxStock
    })
    message.success('更新成功')
    editModalVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[库存管理] 更新失败', err)
    message.error(err?.message || '更新失败')
  } finally {
    editLoading.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  fetchData()
  window.addEventListener("erp:create", handleParentCreate)
  window.addEventListener("erp:refresh", fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (autoRefreshEnabled.value) {
      fetchData()
      autoRefreshCountdown.value = 30
    }
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshEnabled.value && autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener("erp:create", handleParentCreate)
  window.removeEventListener("erp:refresh", fetchData)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.low-stock { color: #ff4d4f; font-weight: bold; }
.over-stock { color: #fa8c16; font-weight: bold; }

.stock-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.stock-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stock-page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.stock-page-header-right {
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

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
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
  color: #f5222d;
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
