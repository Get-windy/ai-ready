<template>
  <div class="purchase-orders-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.pending }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.completed }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalAmount) }}</div>
          <div class="stat-card-label">订单金额合计</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">订单总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :show-export="true"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新建订单"
      @add="handleAdd"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
      @batch-delete="handleBatchDelete"
      @export="handleExport"
    >
      <template #toolbar-actions>
        <a-button size="small" @click="handleImport">
          <template #icon><ImportOutlined /></template>
          导入
        </a-button>
      </template>

      <template #batch-actions="{ selectedRows }">
        <a-button size="small" type="primary" ghost @click="handleBatchApprove(selectedRows)">
          <template #icon><CheckCircleOutlined /></template>
          批量审批
        </a-button>
        <a-button size="small" @click="handleBatchClose(selectedRows)">
          <template #icon><StopOutlined /></template>
          批量关闭
        </a-button>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 0" title="编辑">
            <a-button type="link" size="small" @click="handleEdit(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item v-if="record.status === 0" key="submit">
                  <CheckCircleOutlined /> 提交审核
                </a-menu-item>
                <a-menu-item v-if="record.status === 1" key="approve">
                  <AuditOutlined /> 审批通过
                </a-menu-item>
                <a-menu-item v-if="record.status === 2" key="inbound">
                  <ImportOutlined /> 创建入库单
                </a-menu-item>
                <a-menu-item v-if="record.status >= 2" key="close">
                  <StopOutlined /> 关闭订单
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="print">
                  <PrinterOutlined /> 打印
                </a-menu-item>
                <a-menu-item key="copy">
                  <CopyOutlined /> 复制
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item v-if="record.status === 0" key="delete" danger>
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的订单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无采购订单数据，点击右上角「新建订单」开始创建
          </p>
        </div>
      </template>
    </VxeTableList>

    <!-- 新建/编辑订单弹窗 -->
    <PurchaseOrderFormModal
      v-model:open="formVisible"
      :is-edit="isEdit"
      :record="editRecord"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseOrdersTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined,
  AuditOutlined, PrinterOutlined, ImportOutlined, StopOutlined,
  SearchOutlined, InboxOutlined, EllipsisOutlined, CopyOutlined,
  ClockCircleOutlined, DollarOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PurchaseOrderFormModal from '../components/PurchaseOrderFormModal.vue'
import { purchaseOrderApi } from '@/api/erp'

interface PurchaseOrder {
  id: number
  orderNo: string
  supplierName: string
  orderDate: string
  totalAmount: number
  totalAmountWithTax: number
  status: number
  purchaserName?: string
  buyerName?: string
  createTime: string
  remark?: string
}

// vxe-table 列定义
const vxeColumns = computed(() => [
  {
    field: 'orderNo',
    title: '订单号',
    width: 160,
    formatter: ({ cellValue }: any) => `<a style="color: #1890ff; cursor: pointer;">${cellValue}</a>`,
  },
  { field: 'supplierName', title: '供应商', width: 140 },
  { field: 'orderDate', title: '订单日期', width: 110 },
  {
    field: 'totalAmountWithTax',
    title: '订单金额',
    width: 130,
    align: 'right',
    formatter: ({ cellValue }: any) => `¥${(cellValue || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`,
  },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>`,
  },
  { field: 'purchaserName', title: '采购员', width: 100 },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 140, fixed: 'right', type: 'action' },
])

const filterFields = [
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '部分入库', value: 3 },
    { label: '已完成', value: 4 },
    { label: '已关闭', value: 5 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red'
}
const statusTextMap: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '部分入库', 4: '已完成', 5: '已关闭'
}

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<PurchaseOrder[]>([])
const formVisible = ref(false)
const isEdit = ref(false)
const editRecord = ref<PurchaseOrder | null>(null)
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const pending = dataSource.value.filter(r => r.status === 1).length
  const completed = dataSource.value.filter(r => r.status === 4).length
  return { pending, completed }
})

const totalAmount = computed(() => {
  return dataSource.value.reduce((s, r) => s + (r.totalAmountWithTax || 0), 0)
})

// 空行填充
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...dataSource.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseOrderApi.getPage({
      current: pagination.current,
      size: pagination.pageSize,
      ...searchFilters
    })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || mockData()
    pagination.total = pageData.total || mockData().length
  } catch {
    message.error('获取采购订单列表失败')
    dataSource.value = mockData()
  } finally {
    loading.value = false
  }
}

const mockData = (): PurchaseOrder[] => [
  { id: 1, orderNo: 'PO2024010001', supplierName: '北京供应商', orderDate: '2024-01-10', totalAmountWithTax: 58000, status: 2, purchaserName: '张三', createTime: '2024-01-10 10:00' },
  { id: 2, orderNo: 'PO2024010002', supplierName: '上海贸易公司', orderDate: '2024-01-12', totalAmountWithTax: 32000, status: 1, purchaserName: '李四', createTime: '2024-01-12 11:00' },
  { id: 3, orderNo: 'PO2024010003', supplierName: '广州制造企业', orderDate: '2024-01-15', totalAmountWithTax: 15000, status: 0, purchaserName: '王五', createTime: '2024-01-15 09:00' },
  { id: 4, orderNo: 'PO2024010004', supplierName: '深圳电子公司', orderDate: '2024-01-08', totalAmountWithTax: 42000, status: 4, purchaserName: '张三', createTime: '2024-01-08 14:00' },
  { id: 5, orderNo: 'PO2024010005', supplierName: '杭州供应商', orderDate: '2024-01-18', totalAmountWithTax: 8000, status: 3, purchaserName: '李四', createTime: '2024-01-18 15:00' }
]

function handleView(record: PurchaseOrder) {
  router.push(`/purchase/order/${record.id}`)
}

function handleAdd() {
  isEdit.value = false
  editRecord.value = null
  formVisible.value = true
}

function handleEdit(record: PurchaseOrder) {
  isEdit.value = true
  editRecord.value = { ...record }
  formVisible.value = true
}

function handleFormSuccess() {
  formVisible.value = false
  fetchData()
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: PurchaseOrder) {
  switch (key) {
    case 'submit':
      handleSubmit(record)
      break
    case 'approve':
      handleApprove(record)
      break
    case 'inbound':
      message.info(`创建入库单: ${record.orderNo}`)
      break
    case 'close':
      handleClose(record)
      break
    case 'print':
      message.info(`打印订单: ${record.orderNo}`)
      break
    case 'copy':
      message.info(`复制订单: ${record.orderNo}`)
      break
    case 'delete':
      handleDelete(record)
      break
  }
}

function handleSubmit(record: PurchaseOrder) {
  Modal.confirm({
    title: '确认提交',
    content: `提交订单 "${record.orderNo}" 进行审核？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      try { await purchaseOrderApi.submit(record.id); message.success('提交成功'); fetchData() }
      catch { message.error('提交失败') }
    }
  })
}

function handleApprove(record: PurchaseOrder) {
  Modal.confirm({
    title: '确认审批',
    content: `审批通过订单 "${record.orderNo}" ？`,
    okText: '确认审批',
    centered: true,
    onOk: async () => {
      try { await purchaseOrderApi.approve(record.id); message.success('审批成功'); fetchData() }
      catch { message.error('审批失败') }
    }
  })
}

function handleClose(record: PurchaseOrder) {
  Modal.confirm({
    title: '确认关闭',
    content: `关闭订单 "${record.orderNo}" ？`,
    okText: '确认关闭',
    centered: true,
    onOk: async () => {
      try { await purchaseOrderApi.close(record.id); message.success('已关闭'); fetchData() }
      catch { message.error('关闭失败') }
    }
  })
}

function handleDelete(record: PurchaseOrder) {
  Modal.confirm({
    title: '删除订单',
    content: `确认删除订单 "${record.orderNo}"？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try { await purchaseOrderApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

const selectedRowKeys = ref<number[]>([])

function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

function handleImport() {
  message.info('导入功能开发中')
}

function handleBatchApprove(selectedRows: any[]) {
  if (!selectedRows || selectedRows.length === 0) {
    message.warning('请选择要审批的订单')
    return
  }
  const keys = selectedRows.map(r => r.id)
  Modal.confirm({
    title: '批量审批',
    content: `审批选中的 ${keys.length} 条订单？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      try { await purchaseOrderApi.batchApprove(keys); message.success(`成功审批 ${keys.length} 条`); tableRef.value?.clearSelection(); fetchData() }
      catch { message.error('批量审批失败') }
    }
  })
}

function handleBatchClose(selectedRows: any[]) {
  if (!selectedRows || selectedRows.length === 0) {
    message.warning('请选择要关闭的订单')
    return
  }
  const keys = selectedRows.map(r => r.id)
  Modal.confirm({
    title: '批量关闭',
    content: `关闭选中的 ${keys.length} 条订单？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      message.success(`成功关闭 ${keys.length} 条订单`)
      tableRef.value?.clearSelection()
      fetchData()
    }
  })
}

function handleExport() {
  const headers = ['订单号', '供应商', '订单日期', '订单金额', '状态', '采购员', '创建时间']
  const rows = dataSource.value.map((r: any) => [
    r.orderNo || '',
    r.supplierName || '',
    r.orderDate || '',
    (r.totalAmountWithTax || 0).toFixed(2),
    getStatusText(r.status),
    r.purchaserName || r.buyerName || '',
    r.createTime || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `采购订单_${new Date().toISOString().slice(0, 10)}.csv`
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
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.purchase-orders-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
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

.empty-placeholder {
  color: transparent;
}

.order-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
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

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
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