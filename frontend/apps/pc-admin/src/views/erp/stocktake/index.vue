<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="stocktake-header">
        <div class="stocktake-header__left">
          <span class="stocktake-header__breadcrumb">ERP / 库存管理 / 库存盘点</span>
          <h2 class="stocktake-header__title">库存盘点</h2>
        </div>
        <div class="stocktake-header__right">
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
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">盘点单总数</div>
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
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <FormOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">盘点中</div>
            <div class="summary-value">{{ statistics.processingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statistics.completedCount }}</div>
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
      add-text="新建盘点单"
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
        <a-button size="small" v-permission="'erp:stock:export'" @click="handleExport">
          <template #icon><DownloadOutlined /></template>
          导出
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
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的盘点单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无盘点单数据，点击右上角「新建盘点单」开始创建
          </p>
        </div>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-tooltip title="查看">
            <a-button type="link" size="small" v-permission="'erp:stock:view'" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 草稿 -> 编辑 -->
          <a-tooltip v-if="record.status === 0" title="编辑">
            <a-button type="link" size="small" v-permission="'erp:stock:view'" @click="handleView(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 已审批 -> 开始盘点 -->
          <a-tooltip v-if="record.status === 2" title="开始盘点">
            <a-button type="link" size="small" @click="confirmStart(record)">
              <template #icon><FormOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 进行中 -> 完成盘点 -->
          <a-tooltip v-if="record.status === 5" title="完成盘点">
            <a-button type="link" size="small" @click="confirmComplete(record)">
              <template #icon><CheckOutlined /></template>
            </a-button>
          </a-tooltip>
          <!-- 已完成有差异 -> 库存调整 -->
          <a-tooltip v-if="record.status === 6 && record.diffItems > 0" title="库存调整">
            <a-button type="link" size="small" @click="confirmAdjust(record)">
              <template #icon><AuditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="(e) => handleActionMenuClick(e.key, record)">
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
      title="盘点单详情"
      placement="right"
      width="80vw"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="盘点单号">{{ detailData.checkNo || detailData.stocktakeNo }}</a-descriptions-item>
          <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
          <a-descriptions-item label="盘点日期">{{ detailData.checkDate ? dayjs(detailData.checkDate).format('YYYY-MM-DD') : (detailData.stocktakeDate || '-') }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="STOCKTAKE_STATUS_ORDER" />
          </a-descriptions-item>
          <a-descriptions-item label="盘点类型">
            {{ detailData.checkType === 1 ? '全盘' : detailData.checkType === 2 ? '抽盘' : detailData.checkType === 3 ? '动态盘点' : '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="操作人">{{ detailData.creatorName || detailData.operator || '-' }}</a-descriptions-item>
          <a-descriptions-item label="盘点人">{{ detailData.checkerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="监盘人">{{ detailData.supervisorName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="系统数量">{{ detailData.totalBookQuantity ?? detailData.systemQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="实际数量">{{ detailData.totalActualQuantity ?? detailData.actualQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="差异">
            <span :class="{ 'positive': (detailData.totalDiffQuantity ?? detailData.difference ?? 0) > 0, 'negative': (detailData.totalDiffQuantity ?? detailData.difference ?? 0) < 0 }">
              {{ (detailData.totalDiffQuantity ?? detailData.difference ?? 0) > 0 ? '+' : '' }}{{ detailData.totalDiffQuantity ?? detailData.difference ?? 0 }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="差异项数">{{ detailData.diffItems ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <template v-if="detailData">
          <h4 style="margin: 16px 0 8px;">盘点明细</h4>
          <a-table
            :data-source="detailItems"
            :columns="detailColumns"
            :pagination="false as any"
            size="small"
            bordered
            row-key="id"
          />
        </template>
      </a-spin>
      <template #footer v-if="detailData">
        <a-space>
          <a-button v-if="detailData.status === 0" v-permission="'erp:stock:edit'" @click="handleEdit(detailData)">
            <template #icon><EditOutlined /></template>
            编辑
          </a-button>
          <a-button v-if="detailData.status === 2" type="primary" @click="confirmStart(detailData)">
            <template #icon><FormOutlined /></template>
            开始盘点
          </a-button>
          <a-button v-if="detailData.status === 5" type="primary" @click="confirmComplete(detailData)">
            <template #icon><CheckOutlined /></template>
            完成盘点
          </a-button>
          <a-button v-if="detailData.status === 6 && detailData.diffItems > 0" type="primary" @click="confirmAdjust(detailData)">
            <template #icon><AuditOutlined /></template>
            库存调整
          </a-button>
          <PrintButton v-if="detailData.status >= 6" template-type="stocktake" :business-id="detailData.id" business-type="stocktake" button-text="打印" button-size="small" />
        </a-space>
      </template>
    </a-drawer>
  </PageContainer>

  <!-- 新建盘点单弹窗 -->
  <a-modal
    v-model:open="createModalVisible"
    title="新建盘点单"
    width="600px"
    :confirm-loading="submitLoading"
    @ok="handleCreateSubmit"
    @cancel="handleCreateCancel"
    :mask-closable="false"
    destroy-on-close
  >
    <a-form
      ref="createFormRef"
      :model="createForm"
      :rules="formRules"
      layout="vertical"
    >
      <a-form-item label="创建方式">
        <a-radio-group v-model:value="createMode">
          <a-radio value="blank">创建空白盘点单</a-radio>
          <a-radio value="from-stock">从仓库库存生成明细</a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="仓库" name="warehouseId">
        <a-select
          v-model:value="createForm.warehouseId"
          :options="warehouseOptions"
          placeholder="请选择仓库"
          show-search
          :filter-option="(input, option) => option.label.toLowerCase().includes(input.toLowerCase())"
          allow-clear
        />
      </a-form-item>

      <a-form-item label="盘点日期" name="checkDate">
        <a-date-picker
          v-model:value="createForm.checkDate"
          value-format="YYYY-MM-DD"
          style="width: 100%"
          placeholder="请选择盘点日期"
        />
      </a-form-item>

      <a-form-item label="盘点类型" name="checkType">
        <a-select
          v-model:value="createForm.checkType"
          placeholder="请选择盘点类型"
          :options="[
            { label: '全盘', value: 1 },
            { label: '抽盘', value: 2 },
            { label: '动态盘点', value: 3 },
          ]"
        />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="盘点人" name="checkerName">
            <a-input v-model:value="createForm.checkerName" placeholder="请输入盘点人姓名" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="监盘人" name="supervisorName">
            <a-input v-model:value="createForm.supervisorName" placeholder="请输入监盘人姓名" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="createForm.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { STOCKTAKE_STATUS_ORDER } from '@/utils/statusConfig'
import { message, Modal } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import {
  DownloadOutlined,
  EyeOutlined,
  FormOutlined,
  CheckOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  SearchOutlined,
  InboxOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  ReloadOutlined,
  WarningOutlined,
  EditOutlined,
  AuditOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

// ── 防抖工具 ──────────────────────────────────────────
function handleError(err: any) { console.warn('[Stocktake]', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

interface Stocktake {
  id: number
  stocktakeNo: string
  warehouseName: string
  stocktakeDate: string
  systemQuantity: number
  actualQuantity: number
  difference: number
  status: number
  operator: string
  remark?: string
}

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const dataSource = ref<Stocktake[]>([])
const detailVisible = ref(false)
const currentRecord = ref<Stocktake | null>(null)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())
const selectedRows = ref<Stocktake[]>([])
const selectedIds = ref<number[]>([])

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  processingCount: 0,
  completedCount: 0
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

const vxeColumns: any = computed(() => [
  { field: 'stocktakeNo', title: '盘点单号', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'stocktakeDate', title: '盘点日期', width: 120 },
  { field: 'systemQuantity', title: '系统数量', width: 100, align: 'right' },
  { field: 'actualQuantity', title: '实际数量', width: 100, align: 'right' },
  { field: 'difference', title: '差异', width: 100, align: 'right', formatter: ({ cellValue }) => {
    const prefix = cellValue > 0 ? '+' : ''
    return `${prefix}${cellValue}`
  }},
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => STOCKTAKE_STATUS_ORDER[cellValue]?.text || '' },
  { field: 'operator', title: '操作人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' },
])

const warehouseOptions = ref<{ label: string; value: number }[]>([])

const filterFields = computed(() => [
  { key: 'stocktakeNo', label: '盘点单号', type: 'input' as const, placeholder: '请输入盘点单号' },
  { key: 'warehouseId', label: '仓库', type: 'select' as const, options: warehouseOptions.value },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '已拒绝', value: 3 },
    { label: '盘点中', value: 4 },
    { label: '进行中', value: 5 },
    { label: '已完成', value: 6 },
    { label: '已调整', value: 7 },
    { label: '已取消', value: 8 },
  ]},
])

const loadWarehouses = async () => {
  try {
    const res = await request.get('/erp/warehouse/list')
    const list = res?.data || []
    warehouseOptions.value = list.map((w: any) => ({ label: w.name, value: w.id }))
  } catch (err) {
    console.warn('[库存盘点] 加载仓库列表失败', err)
  }
}

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

// ── 新建盘点单弹窗 ──────────────────────────────────────────
const createModalVisible = ref(false)
const createMode = ref<'blank' | 'from-stock'>('blank')
const submitLoading = ref(false)
const createFormRef = ref()

const createForm = reactive({
  warehouseId: undefined as number | undefined,
  checkDate: dayjs().format('YYYY-MM-DD'),
  checkType: 1,
  checkerName: (userStore as any)?.realName || (userStore as any)?.username || '',
  supervisorName: '',
  remark: ''
})

const formRules: Record<string, any[]> = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
}

const handleCreate = () => {
  createMode.value = 'blank'
  createForm.warehouseId = undefined
  createForm.checkDate = dayjs().format('YYYY-MM-DD')
  createForm.checkType = 1
  createForm.checkerName = (userStore as any)?.realName || (userStore as any)?.username || ''
  createForm.supervisorName = ''
  createForm.remark = ''
  createModalVisible.value = true
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    const payload: Record<string, any> = {
      checkType: createForm.checkType,
      warehouseId: createForm.warehouseId,
      warehouseName: warehouseOptions.value.find(o => o.value === createForm.warehouseId)?.label || '',
      checkDate: createForm.checkDate,
    }
    if (createForm.checkerName) payload.checkerName = createForm.checkerName
    if (createForm.supervisorName) payload.supervisorName = createForm.supervisorName
    if (createForm.remark) payload.remark = createForm.remark

    if (createMode.value === 'from-stock' && createForm.warehouseId) {
      await request.post(`/erp/stock/check/create-with-items/${createForm.warehouseId}`, payload)
      message.success('盘点单创建成功（含库存明细）')
    } else {
      await request.post('/erp/stock/check', payload)
      message.success('空白盘点单创建成功')
    }

    createModalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[库存盘点] 创建盘点单失败', err)
    message.error('创建盘点单失败')
  } finally {
    submitLoading.value = false
  }
}

const handleCreateCancel = () => {
  createModalVisible.value = false
}

const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailItems = ref<any[]>([])
const detailColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode' },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec' },
  { title: '系统数量', dataIndex: 'bookQuantity', key: 'bookQuantity' },
  { title: '实际数量', dataIndex: 'actualQuantity', key: 'actualQuantity' },
  {
    title: '差异',
    dataIndex: 'diffQuantity',
    key: 'diffQuantity',
    customRender: ({ text }: { text: number }) => {
      const prefix = text > 0 ? '+' : ''
      const color = text > 0 ? '#3f8600' : text < 0 ? '#ff4d4f' : undefined
      return h('span', { style: { color, fontWeight: 'bold' } }, `${prefix}${text}`)
    }
  },
]

const fetchDetail = async (id: number) => {
  detailLoading.value = true
  detailItems.value = []
  try {
    const res = await request.get(`/erp/stock/check/${id}`)
    detailData.value = res.data || null
    // 获取盘点明细
    try {
      const itemsRes = await request.get(`/erp/stock/check/${id}/items`)
      detailItems.value = itemsRes?.data || []
    } catch {
      detailItems.value = []
    }
  } catch (err) {
    console.warn('[库存盘点] 获取详情失败', err)
    detailData.value = dataSource.value.find(item => item.id === id) || null
  } finally {
    detailLoading.value = false
  }
}

const handleView = (record: Stocktake) => {
  detailVisible.value = true
  fetchDetail(record.id)
}

const handleStart = async (record: Stocktake) => {
  try {
    await request.put(`/erp/stock/check/${record.id}/start`)
    message.success(`开始盘点: ${record.stocktakeNo}`)
    detailVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[库存盘点] 开始盘点失败', err)
    message.error('开始盘点失败')
  }
}

const handleComplete = async (record: Stocktake) => {
  try {
    await request.put(`/erp/stock/check/${record.id}/complete`)
    message.success(`盘点完成: ${record.stocktakeNo}`)
    detailVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[库存盘点] 完成盘点失败', err)
    message.error('完成盘点失败')
  }
}

const handleDelete = async (record: Stocktake) => {
  try {
    await request.delete(`/erp/stockCheck/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[库存盘点] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: any, record: Stocktake) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该盘点单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

const handleEdit = (record: Stocktake) => {
  router.push(`/erp/stocktake/${record.id}`)
}

const confirmStart = (record: Stocktake) => {
  Modal.confirm({
    title: '确认开始盘点',
    content: `确认开始盘点 ${record.stocktakeNo} 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => handleStart(record)
  })
}

const confirmComplete = (record: Stocktake) => {
  Modal.confirm({
    title: '确认完成盘点',
    content: `确认完成盘点 ${record.stocktakeNo} 吗？完成后可进行库存调整。`,
    okText: '确定',
    cancelText: '取消',
    onOk: () => handleComplete(record)
  })
}

const confirmAdjust = (record: Stocktake) => {
  Modal.confirm({
    title: '库存调整确认',
    content: `盘点单 ${record.stocktakeNo} 存在差异，确认按盘点结果调整库存？`,
    okText: '确认调整',
    cancelText: '取消',
    onOk: () => handleAdjust(record)
  })
}

const handleAdjust = async (record: Stocktake) => {
  try {
    await request.post(`/erp/stock/check/${record.id}/adjust`)
    message.success(`库存调整完成: ${record.stocktakeNo}`)
    detailVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[库存盘点] 库存调整失败', err)
    message.error('库存调整失败')
  }
}

const handleExport = async () => {
  try {
    const res = await request.get('/erp/stock/check/export', null, {
      params: { ...searchFilters },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `盘点单_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err) {
    console.warn('[库存盘点] 导出失败', err)
    message.error('导出失败')
  }
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleSelectionChange = (rows: Stocktake[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('erp:create', handleParentCreate)
  window.addEventListener('erp:refresh', () => fetchData())
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
  window.removeEventListener('erp:refresh', () => fetchData())
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await request.get('/erp/stock/check/page', { params: {
      tenantId: userStore.tenantId,
      keyword: searchFilters.stocktakeNo || undefined,
      status: searchFilters.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }})
    if (res.data?.records) {
      dataSource.value = res.records.map((item) => ({
        id: item.id,
        stocktakeNo: item.checkNo,
        warehouseName: item.warehouseName,
        stocktakeDate: item.checkDate,
        systemQuantity: item.systemQuantity || 0,
        actualQuantity: item.actualQuantity || 0,
        difference: (item.actualQuantity || 0) - (item.systemQuantity || 0),
        status: item.status,
        operator: item.creatorName || '',
        remark: item.remark || ''
      }))
      pagination.total = res.total || 0
      // 优先使用 API 返回的全局统计数据，避免仅基于当前页计算
      if (res.totalCount !== undefined) {
        statistics.value.totalCount = res.totalCount
        statistics.value.pendingCount = res.pendingCount || 0
        statistics.value.processingCount = res.processingCount || 0
        statistics.value.completedCount = res.completedCount || 0
      } else {
        statistics.value.totalCount = dataSource.value.length
        statistics.value.pendingCount = dataSource.value.filter(item => item.status === 0).length
        statistics.value.processingCount = dataSource.value.filter(item => item.status === 1).length
        statistics.value.completedCount = dataSource.value.filter(item => item.status === 2).length
      }
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdated.value = new Date().toISOString()
  } catch (error) {
    hasError.value = true
    console.warn('[库存盘点] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.stocktake-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.stocktake-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stocktake-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.stocktake-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.stocktake-header__right {
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

.stocktake-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
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

.positive {
  color: #3f8600;
  font-weight: bold;
}

.negative {
  color: #ff4d4f;
  font-weight: bold;
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
