<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="split-header">
        <div class="split-header__left">
          <span class="split-header__breadcrumb">ERP / 库存管理 / 拆分管理</span>
          <h2 class="split-header__title">拆分管理</h2>
        </div>
        <div class="split-header__right">
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
            <ScissorOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">拆分单总数</div>
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
            <div class="summary-title">费用总额</div>
            <div class="summary-value">¥{{ (statistics.totalAmount || 0).toFixed(2) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :selectable="false"
      :filter-fields="filterFields"
      add-text="新建拆分单"
      style="flex: 1;"
      @add="handleCreate"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </template>

      <template #empty>
        <div v-if="hasError" class="table-empty">
          <WarningOutlined class="table-empty-icon" />
          <p class="table-empty-text">数据加载异常，请重试</p>
          <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
        </div>
        <div v-else class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <ScissorOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的拆分单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无拆分单数据，点击右上角「新建拆分单」开始创建
          </p>
        </div>
      </template>

      <template #statusCell="{ record }">
        <StatusTag :status="record.status" :map="ASSEMBLE_STATUS" />
      </template>

      <template #totalCostCell="{ record }">
        ¥{{ (record.totalCost || 0).toFixed(2) }}
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看">
            <a-button type="link" size="small" v-permission="'erp:stock:view'" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 草稿 -> 提交 -->
          <a-tooltip v-if="record.status === 0" title="提交审批">
            <a-button type="link" size="small" style="color: #1890ff;" v-permission="'erp:stock:submitapproval'" @click="handleSubmitApproval(record)">
              <template #icon><SendOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 待审批 -> 审批/拒绝 -->
          <template v-if="record.status === 1">
            <a-dropdown trigger="click">
              <a-button type="link" size="small">
                审批 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleApprove(record)">
                    <CheckOutlined /> 审批通过
                  </a-menu-item>
                  <a-menu-item @click="handleReject(record)">
                    <CloseOutlined /> 拒绝
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <!-- 已审核 -> 执行 -->
          <a-tooltip v-if="record.status === 2" title="执行">
            <a-button type="link" size="small" style="color: #52c41a;" v-permission="'erp:stock:execute'" @click="handleExecute(record)">
              <template #icon><MinusCircleOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 已完成 -> 打印 -->
          <PrintButton
            v-if="record.status === 3"
            template-type="stock_split"
            :business-id="record.id"
            business-type="stock_split"
            button-size="small"
            @print-success="() => message.success(`拆分单 ${record.splitNo} 打印成功`)"
            @print-error="(e: any) => message.error(`打印失败: ${e.message || '未知错误'}`)"
          />
          <!-- 取消 -->
          <a-tooltip v-if="record.status === 0 || record.status === 2" title="取消">
            <a-button type="link" size="small" style="color: #ff4d4f;" v-permission="'erp:stock:cancel'" @click="handleCancel(record)">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="拆分单详情" placement="right" width="80vw">
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="拆分单号">{{ detailData.splitNo }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="产品编码">{{ detailData.productCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="产品名称">{{ detailData.productName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="拆分数量">{{ detailData.splitQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="拆分费用">¥{{ (detailData.splitFee || 0).toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="总成本">¥{{ (detailData.totalCost || 0).toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="ASSEMBLE_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <template v-if="detailData">
          <h4 style="margin: 16px 0 8px;">拆分明细</h4>
          <a-table
            :data-source="detailItems"
            :columns="detailItemColumns"
            :pagination="false as any"
            size="small"
            bordered
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'unitCost' || column.dataIndex === 'cost'">
                ¥{{ (record[column.dataIndex] || 0).toFixed(2) }}
              </template>
            </template>
          </a-table>
        </template>
      </a-spin>

      <template #footer v-if="detailData">
        <a-space>
          <a-button @click="detailVisible = false">关闭</a-button>
          <a-button v-if="detailData.status === 0" v-permission="'erp:stock:submitapproval'" @click="handleSubmitApproval(detailData)">
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
            <template #icon><MinusCircleOutlined /></template>
            执行
          </a-button>
          <PrintButton
            v-if="detailData.status >= 3"
            template-type="stock_split"
            :business-id="detailData.id"
            business-type="stock_split"
            button-text="打印"
            button-size="small"
          />
        </a-space>
      </template>
    </a-drawer>
  </PageContainer>

  <!-- 新建拆分单弹窗 -->
  <a-modal
    v-model:open="createVisible"
    title="新建拆分单"
    width="900px"
    :confirm-loading="createLoading"
    @ok="handleCreateSubmit"
    @cancel="handleCreateCancel"
    :mask-closable="false"
    destroy-on-close
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="选择BOM" name="bomId">
            <a-select
              v-model:value="createForm.bomId"
              placeholder="请选择BOM"
              show-search
              :filter-option="filterOption"
              allow-clear
              size="small"
              @change="handleBomChange"
            >
              <a-select-option v-for="b in bomOptions" :key="b.id" :value="b.id">
                {{ b.bomNo }} - {{ b.bomName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库" name="warehouseId">
            <a-select
              v-model:value="createForm.warehouseId"
              placeholder="请选择仓库"
              show-search
              :filter-option="filterOption"
              allow-clear
              size="small"
            >
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.warehouseName }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="拆分数量" name="splitQuantity">
            <a-input-number v-model:value="createForm.splitQuantity" :min="1" :precision="0" style="width: 100%" placeholder="拆分数量" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="拆分费用" name="splitFee">
            <a-input-number v-model:value="createForm.splitFee" :min="0" :precision="2" style="width: 100%" placeholder="拆分费用" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="createForm.remark" placeholder="备注信息" size="small" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 物料明细（根据BOM自动加载） -->
      <div class="sub-table-header">
        <span class="sub-table-title">拆分产出明细</span>
      </div>
      <a-table
        :data-source="createForm.items"
        :columns="itemColumns"
        :pagination="false as any"
        size="small"
        row-key="tempId"
        style="margin-bottom: 12px;"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'unitCost' || column.dataIndex === 'cost'">
            ¥{{ (record[column.dataIndex] || 0).toFixed(2) }}
          </template>
        </template>
      </a-table>
    </a-form>
  </a-modal>

  <!-- 取消原因弹窗 -->
  <a-modal v-model:open="cancelModalVisible" title="取消确认" width="480px" :confirm-loading="cancelLoading" @ok="handleCancelConfirm" @cancel="handleCancelClose" destroy-on-close>
    <a-form layout="vertical">
      <a-form-item label="取消原因" required>
        <a-textarea v-model:value="cancelReason" :rows="3" placeholder="请输入取消原因（必填）" />
      </a-form-item>
    </a-form>
  </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SyncOutlined, ClockCircleOutlined,
  CheckCircleOutlined, DollarOutlined, WarningOutlined,
  DownOutlined, CheckOutlined, CloseOutlined, SendOutlined,
  MinusCircleOutlined, EyeOutlined, ScissorOutlined, SearchOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import { ASSEMBLE_STATUS } from '@/utils/statusConfig'

// ── 类型定义 ──────────────────────────────────────────
interface SplitItem {
  tempId?: number
  productId?: number
  productCode: string
  productName: string
  spec: string
  quantity: number
  unitCost: number
  cost?: number
}

interface SplitOrder {
  id: number
  splitNo: string
  warehouseName: string
  bomId: number
  productCode: string
  productName: string
  splitQuantity: number
  totalCost: number
  splitFee: number
  status: number
  remark?: string
  createTime: string
  items?: SplitItem[]
}

// ── 防抖与错误处理 ─────────────────────────────────────
function handleError(err: any) { console.warn('[拆分管理] ErrorBoundary 捕获异常:', err) }

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
}

const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const tableData = ref<any[]>([])
const detailVisible = ref(false)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  completedCount: 0,
  totalAmount: 0
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const filterFields = computed(() => [
  { key: 'keyword', label: '关键字', type: 'input' as const, placeholder: '请输入拆分单号/关键字' },
  { key: 'warehouseId', label: '仓库', type: 'select' as const, options: warehouseOptions.value.map((w: any) => ({ label: w.warehouseName, value: w.id })) },
  { key: 'status', label: '状态', type: 'select' as const, options: Object.entries(ASSEMBLE_STATUS).map(([k, v]) => ({ label: v.text, value: Number(k) })) },
])

const vxeColumns: any = computed(() => [
  { field: 'splitNo', title: '拆分单号', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'productCode', title: '产品编码', width: 120 },
  { field: 'productName', title: '产品名称', width: 140 },
  { field: 'splitQuantity', title: '拆分数量', width: 90, align: 'center' },
  { field: 'totalCost', title: '总成本', width: 120, slotName: 'totalCostCell' },
  { field: 'status', title: '状态', width: 90, slotName: 'statusCell' },
  { field: 'action', title: '操作', width: 200, fixed: 'right', type: 'action' },
])

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

function handleParentCreate() { handleCreate() }

const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await request.get('/erp/stock/split/page', {
      params: {
        keyword: searchFilters.keyword || undefined,
        warehouseId: searchFilters.warehouseId || undefined,
        status: searchFilters.status,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    const data = res.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    if (data.totalCount !== undefined) {
      statistics.value.totalCount = data.totalCount
      statistics.value.pendingCount = data.pendingCount || 0
      statistics.value.completedCount = data.completedCount || 0
      statistics.value.totalAmount = data.totalAmount || 0
    } else {
      statistics.value.totalCount = tableData.value.length
      statistics.value.pendingCount = tableData.value.filter((r: any) => r.status === 1).length
      statistics.value.completedCount = tableData.value.filter((r: any) => r.status === 3).length
      statistics.value.totalAmount = tableData.value.reduce((sum: number, r: any) => sum + (r.totalCost || 0), 0)
    }
    lastUpdated.value = new Date().toISOString()
  } catch (error) {
    hasError.value = true
    console.warn('[拆分管理] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  }
}

// ════════════════════════════════════════════════════════════════
// 新建拆分单
// ════════════════════════════════════════════════════════════════

let tempIdCounter = 0
function nextTempId() { return ++tempIdCounter }

const createVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<any>(null)
const createForm = reactive({
  bomId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  splitQuantity: 1,
  splitFee: 0,
  remark: '',
  items: [] as any[]
})
const createRules: Record<string, any[]> = {
  bomId: [{ required: true, message: '请选择BOM', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  splitQuantity: [{ required: true, message: '请输入拆分数量', trigger: 'blur' }]
}

const itemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', width: 100 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 80 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'center' },
  { title: '单位成本', dataIndex: 'unitCost', width: 100 },
  { title: '成本', dataIndex: 'cost', width: 100 }
]

const detailItemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 100 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' },
  { title: '单位成本', dataIndex: 'unitCost', width: 100, align: 'right' },
  { title: '成本', dataIndex: 'cost', width: 120, align: 'right' },
]

// BOM选项
const bomOptions = ref<any[]>([])
// 仓库选项
const warehouseOptions = ref<any[]>([])

function filterOption(input: string, option: any) {
  return (option.children?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

async function loadBomOptions() {
  try {
    const res = await request.get('/erp/stock/bom/page', { params: { pageSize: 200 } })
    const data = res?.data ?? res
    const records = data?.records || (Array.isArray(data) ? data : [])
    bomOptions.value = records.map((item: any) => ({
      id: item.id,
      bomNo: item.bomNo,
      bomName: item.bomName,
      productId: item.productId,
      productCode: item.productCode,
      productName: item.productName
    }))
  } catch { bomOptions.value = [] }
}

async function loadWarehouseOptions() {
  try {
    const res = await request.get('/erp/warehouse/list')
    const list = res?.data || []
    warehouseOptions.value = Array.isArray(list) ? list : []
  } catch { warehouseOptions.value = [] }
}

async function handleBomChange(value: number) {
  createForm.items = []
  if (!value) return
  try {
    const res = await request.get(`/erp/stock/bom/${value}/items`)
    const items = res?.data || []
    createForm.items = items.map((item: any) => ({
      tempId: nextTempId(),
      productId: item.productId,
      productCode: item.productCode || '',
      productName: item.productName || '',
      spec: item.spec || '',
      quantity: item.quantity || 1,
      unitCost: item.unitCost || 0,
      cost: (item.quantity || 1) * (item.unitCost || 0)
    }))
  } catch {
    message.warning('加载BOM明细失败')
    createForm.items = []
  }
}

const handleCreate = () => {
  tempIdCounter = 0
  createForm.bomId = undefined
  createForm.warehouseId = undefined
  createForm.splitQuantity = 1
  createForm.splitFee = 0
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
    message.warning('请先选择BOM')
    return
  }
  createLoading.value = true
  try {
    await request.post('/erp/stock/split', {
      bomId: createForm.bomId,
      warehouseId: createForm.warehouseId,
      splitQuantity: createForm.splitQuantity,
      splitFee: createForm.splitFee || undefined,
      remark: createForm.remark || undefined,
      items: createForm.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        spec: item.spec || undefined,
        quantity: item.quantity,
        unitCost: item.unitCost
      }))
    })
    message.success('拆分单创建成功')
    createVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[拆分管理] 创建失败', err)
    message.error(err?.message || '创建失败，请稍后重试')
  } finally {
    createLoading.value = false
  }
}

const handleCreateCancel = () => {
  createVisible.value = false
}

// ════════════════════════════════════════════════════════════════
// 详情
// ════════════════════════════════════════════════════════════════
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailItems = ref<any[]>([])

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  detailItems.value = []
  try {
    const res = await request.get(`/erp/stock/split/${id}`)
    detailData.value = res.data || null
    try {
      const itemsRes = await request.get(`/erp/stock/split/${id}/items`)
      detailItems.value = itemsRes?.data || []
    } catch { detailItems.value = [] }
  } catch (err) {
    console.warn('[拆分管理] 获取详情失败', err)
    detailData.value = tableData.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: any) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

// ════════════════════════════════════════════════════════════════
// 操作
// ════════════════════════════════════════════════════════════════

const handleSubmitApproval = async (record: any) => {
  Modal.confirm({
    title: '提交审批',
    content: `确认提交拆分单 ${record.splitNo} 进行审批吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/split/${record.id}/submit`)
        message.success('提交成功')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[拆分管理] 提交失败', error)
        message.error('提交失败')
      }
    }
  })
}

const handleApprove = async (record: any) => {
  Modal.confirm({
    title: '审批确认',
    content: '确认审批通过该拆分单吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/split/${record.id}/approve`)
        message.success('审批通过')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[拆分管理] 审批失败', error)
        message.error('审批失败')
      }
    }
  })
}

const handleReject = async (record: any) => {
  Modal.confirm({
    title: '拒绝确认',
    content: '确认拒绝该拆分单吗？',
    okText: '确认拒绝',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await request.post(`/erp/stock/split/${record.id}/reject`)
        message.success('已拒绝')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[拆分管理] 拒绝失败', error)
        message.error('拒绝失败')
      }
    }
  })
}

const handleExecute = async (record: any) => {
  Modal.confirm({
    title: '执行确认',
    content: '确认执行该拆分单吗？执行后将更新库存。',
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/split/${record.id}/execute`)
        message.success('拆分执行成功')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[拆分管理] 执行失败', error)
        message.error('拆分执行失败')
      }
    }
  })
}

// ════════════════════════════════════════════════════════════════
// 取消操作
// ════════════════════════════════════════════════════════════════
const cancelModalVisible = ref(false)
const cancelReason = ref('')
const cancelLoading = ref(false)
let pendingCancelRecord: any = null

const handleCancel = (record: any) => {
  pendingCancelRecord = record
  cancelReason.value = ''
  cancelModalVisible.value = true
}

const handleCancelConfirm = async () => {
  if (!cancelReason.value.trim()) {
    message.warning('请输入取消原因')
    return
  }
  if (!pendingCancelRecord) return
  cancelLoading.value = true
  try {
    await request.post(`/erp/stock/split/${pendingCancelRecord.id}/cancel`, null, {
      params: { reason: cancelReason.value.trim() }
    })
    message.success('取消成功')
    cancelModalVisible.value = false
    pendingCancelRecord = null
    fetchData()
    if (detailVisible.value) detailVisible.value = false
  } catch (error) {
    console.warn('[拆分管理] 取消失败', error)
    message.error('取消失败')
  } finally {
    cancelLoading.value = false
  }
}

const handleCancelClose = () => {
  cancelModalVisible.value = false
  pendingCancelRecord = null
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('erp:create', handleParentCreate)
  window.addEventListener('erp:refresh', fetchData)
  loadBomOptions()
  loadWarehouseOptions()
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
.split-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.split-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.split-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.split-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.split-header__right {
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
  margin-bottom: 12px;
}

.table-empty-text {
  color: #999;
  margin-bottom: 16px;
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
