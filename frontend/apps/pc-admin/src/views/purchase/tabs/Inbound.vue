<template>
  <div class="purchase-inbound-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.pending }}</div>
          <div class="stat-card-label">待收货</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.completed }}</div>
          <div class="stat-card-label">已入库</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalAmount) }}</div>
          <div class="stat-card-label">入库金额合计</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">入库单总数</div>
        </div>
        <ImportOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'purchase-inbound-list'"
      :filter-fields="filterFields"
      :show-export="true"
      :selectable="true"
      add-text="新建入库"
      @add="handleAdd"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @cell-dblclick="handleView"
      @selection-change="(keys: number[]) => { selectedRowKeys = keys }"
    >
      <template #batch-actions>
        <a-button size="small" type="primary" ghost @click="handleBatchApprove">
          <template #icon><CheckCircleOutlined /></template>
          批量审批
        </a-button>
      </template>

      <template #empty>
        <div class="table-empty">
          <template v-if="hasError">
            <WarningOutlined class="table-empty-icon" style="color: #faad14" />
            <p class="table-empty-text">加载失败</p>
            <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
              <ReloadOutlined /> 重试
            </a-button>
          </template>
          <template v-else>
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的入库单，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无入库单数据，点击右上角「新建入库」开始创建
            </p>
          </template>
        </div>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 0" title="审批">
            <a-button type="link" size="small" @click="handleApprove(record)">
              <template #icon><CheckCircleOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 1" title="确认入库">
            <a-button type="link" size="small" @click="handleConfirm(record)">
              <template #icon><ImportOutlined /></template>
            </a-button>
          </a-tooltip>
          <PrintButton
            template-type="inbound"
            :business-id="record.id"
            business-type="purchase_inbound"
            button-text=""
            button-size="small"
            button-type="link"
            tooltip="打印"
          />
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
                <a-menu-divider />
                <a-menu-item key="delete" v-if="record.status === 0" danger><DeleteOutlined /> 删除</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情全屏覆盖层 -->
    <a-drawer
      v-model:open="detailVisible"
      title="入库单详情"
      placement="right"
      width="80vw"
      class="inbound-detail-drawer"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
            <template #icon><ReloadOutlined /></template>
          </a-button>
          <PrintButton
            template-type="inbound"
            :business-id="currentRecord?.id"
            business-type="purchase_inbound"
            button-text="打印"
            button-size="small"
          />
          <a-button v-if="currentRecord?.status === 1" type="primary" size="small" @click="handleConfirmFromDetail">
            <template #icon><ImportOutlined /></template>
            确认入库
          </a-button>
        </a-space>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 10 }">
        <template v-if="detailData">
          <a-descriptions bordered :column="2" size="small">
            <a-descriptions-item label="入库单号">
              <span class="inbound-no">{{ detailData.inboundNo }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="关联订单">
              <a @click="handleViewOrder(detailData)" class="order-link">{{ detailData.orderNo }}</a>
            </a-descriptions-item>
            <a-descriptions-item label="供应商">{{ detailData.supplierName }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
            <a-descriptions-item label="入库日期">{{ detailData.inboundDate }}</a-descriptions-item>
            <a-descriptions-item label="入库金额">
              <span class="amount-cell">¥{{ formatAmount(detailData.totalAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建人">{{ detailData.creatorName }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '无' }}</a-descriptions-item>
          </a-descriptions>

          <a-divider>入库明细</a-divider>
          <VxeTableList
            :columns="detailItemColumns"
            :data-source="detailItems"
            :pagination="false as any"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #amountCell="{ record }">
              <span class="amount-cell">¥{{ formatAmount(record.actualQty * record.unitPrice) }}</span>
            </template>
          </VxeTableList>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.id)">重试</a-button>
          </template>
        </a-result>
      </a-skeleton>
    </a-drawer>

    <!-- 新建入库弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      title="新建入库单"
      width="750px"
      :confirm-loading="formSubmitting"
      ok-text="确认创建"
      cancel-text="取消"
      @ok="handleFormSubmit"
      @cancel="formModalVisible = false"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="关联订单号" name="orderNo" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.orderNo" placeholder="请输入采购订单号" @blur="handleOrderNoBlur" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-input v-model:value="formData.supplierName" placeholder="自动关联" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="仓库" name="warehouseId" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select v-model:value="formData.warehouseId" size="small" placeholder="请选择入库仓库">
                <a-select-option v-for="w in warehouseList" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="入库日期" name="inboundDate" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-date-picker v-model:value="formData.inboundDate" size="small" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>入库物料明细</a-divider>
        <VxeTableList
          :columns="inboundItemColumns"
          :data-source="formData.items"
          :pagination="false as any"
          row-key="tempKey"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #productNameCell="{ record }">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template #expectedQtyCell="{ record }">
            <a-input-number v-model:value="record.expectedQty" :min="0" size="small" style="width: 100%" disabled />
          </template>
          <template #actualQtyCell="{ record }">
            <a-input-number v-model:value="record.actualQty" :min="0" size="small" style="width: 100%" />
          </template>
          <template #unitPriceCell="{ record }">
            <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
          <template #actionCell="{ record, index }">
            <a-button type="link" danger size="small" @click="handleRemoveInboundItem(index)">删除</a-button>
          </template>
        </VxeTableList>
        <a-button type="dashed" block @click="handleAddInboundItem" style="margin-top: 12px">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInboundTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  EyeOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  SearchOutlined,
  InboxOutlined,
  EllipsisOutlined,
  ImportOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  WarningOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inboundApi } from '@/api/erp'
import { optionsApi } from '@/api/options'
import { useUserStore } from '@/stores/user'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const pending = dataSource.value.filter(r => r.status === 1).length
  const completed = dataSource.value.filter(r => r.status === 2).length
  return { pending, completed }
})

const totalAmount = computed(() => {
  return dataSource.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
})

const vxeColumns = computed(() => [
  { title: '入库单号', field: 'inboundNo', width: 160 },
  { title: '关联订单', field: 'orderNo', width: 160 },
  { title: '供应商', field: 'supplierName', width: 140 },
  { title: '入库日期', field: 'inboundDate', width: 110 },
  { title: '金额', field: 'totalAmount', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>` },
  { title: '创建人', field: 'creatorName', width: 100 },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 140, fixed: 'right' }
])

const filterFields = [
  { key: 'inboundNo', label: '入库单号', type: 'input' as const, placeholder: '输入入库单号' },
  { key: 'orderNo', label: '关联订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待收货', value: 1 },
    { label: '已入库', value: 2 }
  ]}
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待收货', 2: '已入库' }

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

// 详情弹窗
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const detailRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const detailItems = ref<any[]>([])

const detailItemColumns = [
  { title: '物料名称', field: 'productName', width: 150 },
  { title: '应入库数量', field: 'expectedQty', width: 100, align: 'right' },
  { title: '实际入库数量', field: 'actualQty', width: 120, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', field: 'amount', width: 120, align: 'right', slotName: 'amountCell' }
]

// 表单状态
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const warehouseList = ref<any[]>([])

let itemCounter = 0
const genTempKey = () => `inbound_item_${++itemCounter}_${Date.now()}`

interface InboundItemForm {
  tempKey: string
  productName: string
  expectedQty: number
  actualQty: number
  unitPrice: number
}

const formData = reactive({
  orderNo: '',
  supplierName: '',
  warehouseId: undefined as number | undefined,
  inboundDate: undefined as any,
  remark: '',
  items: [] as InboundItemForm[]
})

const formRules: any = {
  orderNo: [{ required: true, message: '请输入关联订单号', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  inboundDate: [{ required: true, message: '请选择入库日期', trigger: 'change' }]
}

const inboundItemColumns = [
  { title: '物料名称', field: 'productName', width: 150, slotName: 'productNameCell' },
  { title: '应入库数量', field: 'expectedQty', width: 100, align: 'right', slotName: 'expectedQtyCell' },
  { title: '实际入库数量', field: 'actualQty', width: 120, align: 'right', slotName: 'actualQtyCell' },
  { title: '单价', field: 'unitPrice', width: 100, align: 'right', slotName: 'unitPriceCell' },
  { title: '操作', field: 'action', width: 60, align: 'center', slotName: 'actionCell' }
]

async function loadWarehouses() {
  try {
    const res = await optionsApi.getWarehouses()
    const list = (res as any).data ?? res
    warehouseList.value = (Array.isArray(list) ? list : []).map((w: any) => ({ id: w.id ?? w.value, name: w.name ?? w.label }))
  } catch (e) {
    console.warn('[采购入库] 加载仓库列表失败', e)
    message.error('加载仓库列表失败')
    warehouseList.value = []
  }
}

const handleAddInboundItem = () => {
  formData.items.push({
    tempKey: genTempKey(),
    productName: '',
    expectedQty: 0,
    actualQty: 0,
    unitPrice: 0
  })
}

const handleRemoveInboundItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleOrderNoBlur = () => {
  if (formData.orderNo) {
    const matched = dataSource.value.find((d: any) => d.orderNo === formData.orderNo)
    if (matched) { formData.supplierName = matched.supplierName }
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await inboundApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      tenantId: userStore.tenantId,
      ...searchFilters
    })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
    hasError.value = false
  } catch (e) {
    console.warn('[采购入库] 获取列表失败', e)
    message.error('获取入库单列表失败')
    dataSource.value = []
    hasError.value = true
  } finally {
    loading.value = false
  }
}

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    const res = await inboundApi.getById(id) as any
    const data = (res as any).data ?? res
    detailData.value = data
    detailItems.value = data.items || []
  } catch (err: any) {
    console.warn('[采购入库] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

function handleView(record: any) {
  currentRecord.value = record
  detailRecord.value = record
  detailVisible.value = true
  fetchDetail(record.id)
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = null
  detailError.value = null
  detailItems.value = []
}

function handleDetailRefresh() {
  if (detailRecord.value?.id) fetchDetail(detailRecord.value.id)
}

function handleViewOrder(record: any) {
  if (record.orderNo) {
    // 通过路由跳转到关联的采购订单详情
    const matchedOrder = dataSource.value.find(d => d.orderNo === record.orderNo)
    if (matchedOrder?.orderId) {
      window.dispatchEvent(new CustomEvent('purchase:view-order', { detail: { orderNo: record.orderNo } }))
    }
  }
}

function handleAdd() {
  formData.orderNo = ''
  formData.supplierName = ''
  formData.warehouseId = undefined
  formData.inboundDate = undefined
  formData.remark = ''
  formData.items = []
  loadWarehouses()
  formModalVisible.value = true
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '删除入库单',
    content: `确认删除入库单 "${record.inboundNo}"？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try { await inboundApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (e) { console.warn('[采购入库] 删除失败', e); message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'delete':
      handleDelete(record)
      break
  }
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await inboundApi.create({
      orderNo: formData.orderNo,
      supplierName: formData.supplierName,
      warehouseId: formData.warehouseId,
      inboundDate: formData.inboundDate,
      remark: formData.remark,
      items: formData.items.map(item => ({
        productName: item.productName,
        expectedQty: item.expectedQty,
        actualQty: item.actualQty,
        unitPrice: item.unitPrice
      }))
    })
    message.success('新建入库单成功')
    formModalVisible.value = false
    fetchData()
  } catch (e) {
    console.warn('[采购入库] 新建失败', e)
    message.error('新建入库单失败')
  } finally {
    formSubmitting.value = false
  }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批入库单',
    content: `审批入库单 "${record.inboundNo}" ？`,
    okText: '确认审批',
    centered: true,
    onOk: async () => {
      try { await inboundApi.approve(record.id); message.success('审批成功'); fetchData() }
      catch (e) { console.warn('[采购入库] 审批失败', e); message.error('审批失败') }
    }
  })
}

function handleConfirm(record: any) {
  Modal.confirm({
    title: '确认入库',
    content: `确认入库单 "${record.inboundNo}" 已完成入库？`,
    okText: '确认入库',
    centered: true,
    onOk: async () => {
      try {
        await inboundApi.confirmWarehouse(record.id)
        message.success('入库确认成功')
        fetchData()
      } catch (e) {
        console.warn('[采购入库] 入库确认失败', e)
        message.error('入库确认失败')
      }
    }
  })
}

function handleConfirmFromDetail() {
  if (currentRecord.value) {
    handleConfirm(currentRecord.value)
    detailVisible.value = false
  }
}

function handleBatchApprove() {
  const keys = selectedRowKeys.value
  if (keys.length === 0) {
    message.warning('请选择要审批的入库单')
    return
  }
  Modal.confirm({
    title: '批量审批',
    content: `审批选中的 ${keys.length} 条入库单？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      try {
        await Promise.all(keys.map(id => inboundApi.approve(id)))
        message.success(`成功审批 ${keys.length} 条入库单`)
        fetchData()
      } catch (e) {
        console.warn('[采购入库] 批量审批失败', e)
        message.error('批量审批失败')
      }
    }
  })
}

function csvEscape(val: any): string {
  const str = String(val ?? '')
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return `"${str.replace(/"/g, '""')}"`
  }
  return str
}

function handleExport() {
  const headers = ['入库单号', '关联订单', '供应商', '入库日期', '金额', '状态', '创建人', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.inboundNo, row.orderNo, row.supplierName, row.inboundDate,
    (row.totalAmount || 0).toFixed(2),
    getStatusText(row.status),
    row.creatorName, row.createTime
  ].map(csvEscape))
  const csv = [headers.map(csvEscape).join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `入库单_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  console.warn('[采购入库] 导出成功（前端CSV导出，无后端接口）'); message.success('导出成功')
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const debouncedFetch = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    debounceClick('add', handleAdd)
  }
}

onMounted(() => {
  fetchData()
  loadWarehouses()
  document.addEventListener('keydown', handleKeydown)
  refreshTimer = setInterval(() => fetchData(), 30000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  clearTimeout(debouncedFetch.value)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.purchase-inbound-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.purchase-inbound-tab > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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

.stat-card-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-card-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-card-amount { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-card-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 32px;
  color: #999;
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

.inbound-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
}

.order-link {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

/* 详情抽屉 */
:deep(.inbound-detail-drawer .ant-drawer-body) {
  padding: 16px 24px;
  overflow-y: auto;
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

</style>
