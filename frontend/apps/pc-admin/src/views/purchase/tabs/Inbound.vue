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
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的入库单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无入库单数据，点击右上角「新建入库」开始创建
          </p>
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
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item key="print"><PrinterOutlined /> 打印</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="delete" v-if="record.status === 0" danger><DeleteOutlined /> 删除</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="入库单详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" size="small" v-if="currentRecord">
        <a-descriptions-item label="入库单号">
          <span class="inbound-no">{{ currentRecord.inboundNo }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="关联订单">
          <a @click="handleViewOrder(currentRecord)" class="order-link">{{ currentRecord.orderNo }}</a>
        </a-descriptions-item>
        <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="入库日期">{{ currentRecord.inboundDate }}</a-descriptions-item>
        <a-descriptions-item label="入库金额">
          <span class="amount-cell">¥{{ formatAmount(currentRecord.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '无' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>入库明细</a-divider>
      <VxeTableList
        :columns="detailItemColumns"
        :data-source="detailItems"
        :pagination="false"
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

      <div class="detail-modal-footer">
        <a-space>
          <a-button type="primary" @click="handlePrint"><PrinterOutlined /> 打印</a-button>
          <a-button v-if="currentRecord?.status === 1" type="primary" @click="handleConfirmFromDetail">
            <template #icon><ImportOutlined /></template>
            确认入库
          </a-button>
          <a-button @click="detailVisible = false">关闭</a-button>
        </a-space>
      </div>
    </a-modal>

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
              <a-select v-model:value="formData.warehouseId" placeholder="请选择入库仓库">
                <a-select-option v-for="w in warehouseList" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="入库日期" name="inboundDate" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-date-picker v-model:value="formData.inboundDate" style="width: 100%" />
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
          :pagination="false"
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
          <template #actionCell="{ record, rowIndex }">
            <a-button type="link" danger size="small" @click="handleRemoveInboundItem(rowIndex)">删除</a-button>
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
  PrinterOutlined,
  ClockCircleOutlined,
  DollarOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inboundApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
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
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }) => getStatusText(cellValue) },
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
const detailItems = ref<any[]>([])

const detailItemColumns = [
  { title: '物料名称', field: 'productName', width: 150 },
  { title: '应入库数量', field: 'expectedQty', width: 100, align: 'right' },
  { title: '实际入库数量', field: 'actualQty', width: 120, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', field: 'amount', width: 120, align: 'right', slotName: 'amountCell' }
]

const mockDetailItems = () => [
  { productName: '工业传感器', expectedQty: 100, actualQty: 100, unitPrice: 800 },
  { productName: '智能控制器', expectedQty: 50, actualQty: 48, unitPrice: 1200 },
  { productName: '连接线缆', expectedQty: 200, actualQty: 200, unitPrice: 50 }
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

const formRules = {
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
  warehouseList.value = [
    { id: 1, name: '主仓库' },
    { id: 2, name: '备品仓库' },
    { id: 3, name: '原料仓库' },
    { id: 4, name: '成品仓库' }
  ]
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
    dataSource.value = pageData.records || mockData()
    pagination.total = pageData.total || mockData().length
  } catch {
    message.error('获取入库单列表失败')
    dataSource.value = mockData()
  } finally {
    loading.value = false
  }
}

const mockData = () => [
  { id: 1, inboundNo: 'IB2024010001', orderNo: 'PO2024010001', supplierName: '北京供应商', inboundDate: '2024-01-10', totalAmount: 80000, status: 2, creatorName: '张三', createTime: '2024-01-10 10:00', warehouseName: '主仓库' },
  { id: 2, inboundNo: 'IB2024010002', orderNo: 'PO2024010002', supplierName: '上海供应商', inboundDate: '2024-01-12', totalAmount: 60000, status: 1, creatorName: '李四', createTime: '2024-01-12 11:00', warehouseName: '备品仓库' },
  { id: 3, inboundNo: 'IB2024010003', orderNo: 'PO2024010003', supplierName: '广州供应商', inboundDate: '2024-01-15', totalAmount: 15000, status: 0, creatorName: '王五', createTime: '2024-01-15 09:00', warehouseName: '原料仓库' },
  { id: 4, inboundNo: 'IB2024010004', orderNo: 'PO2024010004', supplierName: '深圳供应商', inboundDate: '2024-01-08', totalAmount: 42000, status: 2, creatorName: '张三', createTime: '2024-01-08 14:00', warehouseName: '主仓库' },
  { id: 5, inboundNo: 'IB2024010005', orderNo: 'PO2024010005', supplierName: '杭州供应商', inboundDate: '2024-01-18', totalAmount: 8000, status: 1, creatorName: '李四', createTime: '2024-01-18 15:00', warehouseName: '成品仓库' }
]

function handleView(record: any) {
  currentRecord.value = { ...record, items: mockDetailItems() }
  detailItems.value = currentRecord.value.items || mockDetailItems()
  detailVisible.value = true
}

function handleViewOrder(record: any) {
  message.info(`查看采购订单: ${record.orderNo}`)
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
      catch { message.error('删除失败') }
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
    case 'print':
      message.info(`打印入库单: ${record.inboundNo}`)
      break
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
  } catch {
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
      catch { message.error('审批失败') }
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
      message.success('入库确认成功')
      fetchData()
    }
  })
}

function handleConfirmFromDetail() {
  if (currentRecord.value) {
    handleConfirm(currentRecord.value)
    detailVisible.value = false
  }
}

function handlePrint() {
  if (currentRecord.value?.inboundNo) {
    message.info(`打印入库单: ${currentRecord.value.inboundNo}`)
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
      message.success(`成功审批 ${keys.length} 条入库单`)
      fetchData()
    }
  })
}

function handleExport() {
  const headers = ['入库单号', '关联订单', '供应商', '入库日期', '金额', '状态', '创建人', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.inboundNo || '',
    row.orderNo || '',
    row.supplierName || '',
    row.inboundDate || '',
    (row.totalAmount || 0).toFixed(2),
    getStatusText(row.status),
    row.creatorName || '',
    row.createTime || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `入库单_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
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
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
}

onMounted(() => {
  fetchData()
  loadWarehouses()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.purchase-inbound-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

.detail-modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
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
</style>
