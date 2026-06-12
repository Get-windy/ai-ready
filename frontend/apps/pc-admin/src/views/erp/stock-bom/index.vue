<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="bom-header">
        <div class="bom-header__left">
          <span class="bom-header__breadcrumb">ERP / 库存管理 / BOM物料清单</span>
          <h2 class="bom-header__title">BOM物料清单</h2>
        </div>
        <div class="bom-header__right">
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

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :selectable="true"
      :filter-fields="filterFields"
      add-text="新建BOM"
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
      </template>

      <template #empty>
        <div v-if="hasError" class="table-empty">
          <WarningOutlined class="table-empty-icon" />
          <p class="table-empty-text">数据加载异常，请重试</p>
          <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
        </div>
        <div v-else class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <FileTextOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的BOM，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无BOM数据，点击右上角「新建BOM」开始创建
          </p>
        </div>
      </template>

      <template #statusCell="{ record }">
        <StatusTag :status="record.status" :map="BOM_STATUS" />
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
          <a-tooltip v-if="record.status === 0" title="启用">
            <a-button type="link" size="small" style="color: #52c41a;" v-permission="'erp:stock:enable'" @click="handleEnable(record)">
              <template #icon><CheckOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-else-if="record.status === 1" title="停用">
            <a-button type="link" size="small" style="color: #ff4d4f;" v-permission="'erp:stock:disable'" @click="handleDisable(record)">
              <template #icon><StopOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="(e) => handleActionMenuClick(String(e.key), record)">
                <a-menu-item key="edit" :disabled="record.status === 1">
                  <EditOutlined /> 编辑
                </a-menu-item>
                <a-menu-item key="delete">
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="BOM详情" placement="right" width="80vw">
      <a-spin :spinning="detailLoading">
        <a-descriptions bordered :column="2" v-if="detailData">
          <a-descriptions-item label="BOM编号">{{ detailData.bomNo }}</a-descriptions-item>
          <a-descriptions-item label="BOM名称">{{ detailData.bomName }}</a-descriptions-item>
          <a-descriptions-item label="成品编码">{{ detailData.productCode }}</a-descriptions-item>
          <a-descriptions-item label="成品名称">{{ detailData.productName }}</a-descriptions-item>
          <a-descriptions-item label="BOM类型">
            {{ detailData.bomType === 1 ? '组装BOM' : detailData.bomType === 2 ? '拆分BOM' : '通用' }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <StatusTag :status="detailData.status" :map="BOM_STATUS" />
          </a-descriptions-item>
          <a-descriptions-item label="产出数量">{{ detailData.outputQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="总成本">¥{{ (detailData.totalCost || 0).toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <template v-if="detailData">
          <h4 style="margin: 16px 0 8px;">BOM明细</h4>
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
          <a-button v-if="detailData.status === 0" type="primary" v-permission="'erp:stock:enable'" @click="handleEnable(detailData)">
            <template #icon><CheckOutlined /></template>
            启用
          </a-button>
          <a-button v-if="detailData.status === 1" danger v-permission="'erp:stock:disable'" @click="handleDisable(detailData)">
            <template #icon><StopOutlined /></template>
            停用
          </a-button>
        </a-space>
      </template>
    </a-drawer>
  </PageContainer>

  <!-- 新建/编辑BOM弹窗 -->
  <a-modal
    v-model:open="createVisible"
    :title="editingId ? '编辑BOM' : '新建BOM'"
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
          <a-form-item label="BOM名称" name="bomName">
            <a-input v-model:value="createForm.bomName" placeholder="请输入BOM名称" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="成品" name="productId">
            <a-select
              v-model:value="createForm.productId"
              placeholder="请选择成品"
              show-search
              :filter-option="filterOption"
              allow-clear
              size="small"
              @change="handleProductChange"
            >
              <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
                {{ p.productCode }} - {{ p.productName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="BOM类型" name="bomType">
            <a-select v-model:value="createForm.bomType" placeholder="请选择BOM类型" size="small">
              <a-select-option :value="1">组装BOM</a-select-option>
              <a-select-option :value="2">拆分BOM</a-select-option>
              <a-select-option :value="3">通用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="产出数量" name="outputQuantity">
            <a-input-number v-model:value="createForm.outputQuantity" :min="1" :precision="0" style="width: 100%" placeholder="产出数量" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="备注" name="remark">
            <a-input v-model:value="createForm.remark" placeholder="备注信息" size="small" />
          </a-form-item>
        </a-col>
      </a-row>

      <!-- BOM明细 -->
      <div class="sub-table-header">
        <span class="sub-table-title">物料明细</span>
        <a-button type="dashed" size="small" @click="addItem"><PlusOutlined /> 添加物料</a-button>
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
            <a-input v-model:value="record.productName" placeholder="产品名称" style="width: 120px" size="small" />
            <a-tooltip title="选择产品"><a-button size="small" type="link" @click="selectItemProduct(index)"><SearchOutlined /></a-button></a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'productCode'">
            <a-input v-model:value="record.productCode" placeholder="编码" style="width: 100px" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'spec'">
            <a-input v-model:value="record.spec" placeholder="规格" style="width: 80px" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="0" :precision="2" style="width: 80px" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'unitCost'">
            <a-input-number v-model:value="record.unitCost" :min="0" :precision="2" style="width: 100px" size="small" />
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-button type="link" danger size="small" @click="removeItem(index)"><DeleteOutlined /></a-button>
          </template>
        </template>
      </a-table>
    </a-form>
  </a-modal>

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
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, SyncOutlined, FileTextOutlined,
  StopOutlined, WarningOutlined, SearchOutlined, DeleteOutlined,
  EllipsisOutlined, EyeOutlined, EditOutlined, CheckOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import request from '@/utils/request'
import { BOM_STATUS } from '@/utils/statusConfig'

// ── 类型定义 ──────────────────────────────────────────
interface BOMItem {
  tempId?: number
  productId?: number
  productCode: string
  productName: string
  spec: string
  quantity: number
  unitCost: number
}

interface BOMOrder {
  id: number
  bomNo: string
  bomName: string
  productId: number
  productCode: string
  productName: string
  bomType: number
  outputQuantity: number
  totalCost: number
  status: number
  remark?: string
  items?: BOMItem[]
}

// ── 防抖与错误处理 ─────────────────────────────────────
function handleError(err: any) { console.warn('[BOM管理] ErrorBoundary 捕获异常:', err) }

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
const selectedRows = ref<any[]>([])
const selectedIds = ref<number[]>([])

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

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
  { key: 'keyword', label: '关键字', type: 'input' as const, placeholder: '请输入BOM编号/名称' },
  { key: 'productId', label: '成品', type: 'select' as const, options: productOptions.value.map((p: any) => ({ label: `${p.productCode} - ${p.productName}`, value: p.id })) },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '停用', value: 0 },
    { label: '启用', value: 1 }
  ]}
])

const vxeColumns: any = computed(() => [
  { field: 'bomNo', title: 'BOM编号', width: 150 },
  { field: 'bomName', title: 'BOM名称', width: 160 },
  { field: 'productCode', title: '成品编码', width: 120 },
  { field: 'productName', title: '成品名称', width: 160 },
  { field: 'bomTypeLabel', title: 'BOM类型', width: 100 },
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

const handleSelectionChange = (rows: any[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await request.get('/erp/stock/bom/page', {
      params: {
        keyword: searchFilters.keyword || undefined,
        productId: searchFilters.productId || undefined,
        status: searchFilters.status,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    const data = res.data || res
    tableData.value = (data?.records || []).map((item: any) => ({
      ...item,
      bomTypeLabel: item.bomType === 1 ? '组装BOM' : item.bomType === 2 ? '拆分BOM' : '通用'
    }))
    pagination.total = data?.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch (error) {
    hasError.value = true
    console.warn('[BOM管理] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  }
}

// ════════════════════════════════════════════════════════════════
// 新建/编辑BOM
// ════════════════════════════════════════════════════════════════

let tempIdCounter = 0
function nextTempId() { return ++tempIdCounter }

const createVisible = ref(false)
const createLoading = ref(false)
const editingId = ref<number | null>(null)
const createFormRef = ref<any>(null)
const createForm = reactive({
  bomName: '',
  productId: undefined as number | undefined,
  bomType: 1,
  outputQuantity: 1,
  remark: '',
  items: [] as any[]
})
const createRules: Record<string, any[]> = {
  bomName: [{ required: true, message: '请输入BOM名称', trigger: 'blur' }],
  productId: [{ required: true, message: '请选择成品', trigger: 'change' }],
  bomType: [{ required: true, message: '请选择BOM类型', trigger: 'change' }],
  outputQuantity: [{ required: true, message: '请输入产出数量', trigger: 'blur' }]
}

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 100 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 80 },
  { title: '数量', dataIndex: 'quantity', width: 80 },
  { title: '单位成本', dataIndex: 'unitCost', width: 100 },
  { title: '操作', dataIndex: 'action', width: 60 }
]

const detailItemColumns: any = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 100 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' },
  { title: '单位成本', dataIndex: 'unitCost', width: 100, align: 'right' },
  { title: '成本', dataIndex: 'cost', width: 120, align: 'right' },
]

// 产品选项
const productOptions = ref<any[]>([])
const productLoading = ref(false)
const productSearchKeyword = ref('')
const productPickerVisible = ref(false)
let pickerTargetIndex = -1

const productPickerColumns = [
  { title: '产品编码', dataIndex: 'productCode', width: 130 },
  { title: '产品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 100 },
  { title: '单位', dataIndex: 'unit', width: 60 },
  { title: '操作', dataIndex: 'action', width: 80 }
]

function filterOption(input: string, option: any) {
  return (option.children?.toString() || '').toLowerCase().includes(input.toLowerCase())
}

function addItem() {
  createForm.items.push({
    tempId: nextTempId(),
    productId: undefined,
    productCode: '',
    productName: '',
    spec: '',
    quantity: 1,
    unitCost: 0
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
    item.spec = product.specification || ''
  }
  productPickerVisible.value = false
}

function handleProductChange(value: number) {
  if (!value) return
  const selected = productOptions.value.find(p => p.id === value)
  if (selected) {
    createForm.bomName = createForm.bomName || selected.productName + ' BOM'
  }
}

async function loadProductList() {
  try {
    const res = await request.get('/erp/product/list', { params: { pageSize: 200 } })
    const data = res?.data ?? res
    productOptions.value = Array.isArray(data) ? data : (data?.records || [])
  } catch { productOptions.value = [] }
}

const handleCreate = () => {
  editingId.value = null
  tempIdCounter = 0
  createForm.bomName = ''
  createForm.productId = undefined
  createForm.bomType = 1
  createForm.outputQuantity = 1
  createForm.remark = ''
  createForm.items = []
  createVisible.value = true
  nextTick(() => createFormRef.value?.resetFields?.())
}

const handleEdit = async (record: any) => {
  editingId.value = record.id
  tempIdCounter = 0
  try {
    const res = await request.get(`/erp/stock/bom/${record.id}`)
    const data = res.data || res
    createForm.bomName = data.bomName || ''
    createForm.productId = data.productId
    createForm.bomType = data.bomType || 1
    createForm.outputQuantity = data.outputQuantity || 1
    createForm.remark = data.remark || ''
    createForm.items = []
    try {
      const itemsRes = await request.get(`/erp/stock/bom/${record.id}/items`)
      const items = itemsRes?.data || []
      createForm.items = items.map((item: any) => ({
        tempId: nextTempId(),
        productId: item.productId,
        productCode: item.productCode || '',
        productName: item.productName || '',
        spec: item.spec || '',
        quantity: item.quantity || 1,
        unitCost: item.unitCost || 0
      }))
    } catch { /* ignore */ }
    createVisible.value = true
    nextTick(() => createFormRef.value?.resetFields?.())
  } catch (err) {
    console.warn('[BOM管理] 获取编辑数据失败', err)
    message.error('获取BOM数据失败')
  }
}

const handleCreateSubmit = async () => {
  try {
    await createFormRef.value?.validate()
  } catch { return }
  if (createForm.items.length === 0) {
    message.warning('请添加物料明细')
    return
  }
  const invalidItem = createForm.items.find((i: any) => !i.productId)
  if (invalidItem) {
    message.warning('请完善物料明细中的产品信息')
    return
  }
  createLoading.value = true
  try {
    const payload = {
      bomName: createForm.bomName,
      productId: createForm.productId,
      bomType: createForm.bomType,
      outputQuantity: createForm.outputQuantity,
      remark: createForm.remark || undefined,
      items: createForm.items.map((item: any) => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        spec: item.spec || undefined,
        quantity: item.quantity,
        unitCost: item.unitCost
      }))
    }

    if (editingId.value) {
      await request.put(`/erp/stock/bom/${editingId.value}`, payload)
      message.success('BOM更新成功')
    } else {
      await request.post('/erp/stock/bom', payload)
      message.success('BOM创建成功')
    }
    createVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[BOM管理] 保存失败', err)
    message.error(err?.message || '保存失败，请稍后重试')
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
    const res = await request.get(`/erp/stock/bom/${id}`)
    detailData.value = res.data || null
    try {
      const itemsRes = await request.get(`/erp/stock/bom/${id}/items`)
      detailItems.value = itemsRes?.data || []
    } catch { detailItems.value = [] }
  } catch (err) {
    console.warn('[BOM管理] 获取详情失败', err)
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

const handleEnable = async (record: any) => {
  Modal.confirm({
    title: '确认启用',
    content: `确认启用BOM ${record.bomNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/bom/${record.id}/enable`)
        message.success('BOM已启用')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[BOM管理] 启用失败', error)
        message.error('启用失败')
      }
    }
  })
}

const handleDisable = async (record: any) => {
  Modal.confirm({
    title: '确认停用',
    content: `确认停用BOM ${record.bomNo} 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.post(`/erp/stock/bom/${record.id}/disable`)
        message.success('BOM已停用')
        fetchData()
        if (detailVisible.value) detailVisible.value = false
      } catch (error) {
        console.warn('[BOM管理] 停用失败', error)
        message.error('停用失败')
      }
    }
  })
}

const handleDelete = async (record: any) => {
  try {
    await request.delete(`/erp/stock/bom/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[BOM管理] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: any) => {
  if (key === 'edit') {
    handleEdit(record)
  } else if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该BOM吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('erp:create', handleParentCreate)
  window.addEventListener('erp:refresh', fetchData)
  loadProductList()
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
.bom-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.bom-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bom-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.bom-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.bom-header__right {
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
