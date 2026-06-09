<template>
  <div class="orders-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-draft">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ draftCount }}</div>
          <div class="stat-card-label">草稿</div>
        </div>
        <FileOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pendingCount }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ completedCount }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalAmount) }}</div>
          <div class="stat-card-label">本页金额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- vxe-table 表格 -->
    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :show-export="true"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :selectable="true"
      add-text="新建订单"
      @add="handleAdd"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @batch-delete="handleBatchDelete"
      @selection-change="handleSelectionChange"
      @view="handleView"
    >
      <template #toolbar-actions>
        <a-tooltip title="从Excel/CSV文件批量导入订单数据">
          <a-button v-permission="'sale:order:create'" @click="handleImport">
            <template #icon><ImportOutlined /></template>
            导入
          </a-button>
        </a-tooltip>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          上次更新: {{ formatRelativeTime(lastUpdated) }}
        </span>
      </template>

      <template #batch-actions="{ selectedRows }">
        <a-button v-permission="'sale:order:approve'" size="small" :loading="batchApproving" @click="handleBatchApprove(selectedRows)">批量审批</a-button>
        <a-button v-permission="'sale:order:list'" size="small" :loading="batchPrinting" @click="handleBatchPrint(selectedRows)">批量打印</a-button>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip v-if="canEdit(record.status)" title="编辑">
            <a-button type="link" size="small" @click="handleEdit(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown v-if="canSubmit(record.status) || canApprove(record.status) || canDelete(record.status)">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><MoreOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="(e: any) => handleActionMenuClick(e.key, record)">
                <a-menu-item v-if="canSubmit(record.status)" key="submit">提交审批</a-menu-item>
                <a-menu-item v-if="canApprove(record.status)" key="approve">审批通过</a-menu-item>
                <a-menu-item v-if="canDelete(record.status)" key="delete" danger>删除</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>

      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配订单">
          <template #image>
            <SearchOutlined style="font-size: 48px; color: #faad14" />
          </template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无销售订单">
          <template #image>
            <InboxOutlined style="font-size: 48px; color: #d9d9d9" />
          </template>
          <a-button type="primary" @click="handleAdd">创建第一个订单</a-button>
        </a-empty>
      </template>
    </VxeTableList>

    <!-- 新建/编辑订单弹窗 -->
    <SaleOrderFormModal
      v-model:open="formVisible"
      :is-edit="isEdit"
      :record="editRecord"
      @success="handleFormSuccess"
    />

    <!-- 导入订单弹窗 -->
    <SaleOrderImportModal
      v-model:open="importModalVisible"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleOrdersTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')
import {
  FileOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  ImportOutlined,
  ExportOutlined,
  InboxOutlined,
  SearchOutlined,
  EditOutlined,
  EyeOutlined,
  MoreOutlined,
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import SaleOrderFormModal from '../components/SaleOrderFormModal.vue'
import SaleOrderImportModal from '../components/SaleOrderImportModal.vue'
import { saleOrderApi } from '@/api/erp'
import { useExport } from '@/composables/useExport'
import {
  ORDER_STATUS_TEXT,
  ORDER_STATUS_COLOR,
  canEdit,
  canSubmit,
  canApprove,
  canDelete,
} from '../constants/orderStatus'

interface SaleOrder {
  id: number
  orderNo: string
  customerName: string
  orderDate: string
  totalAmount: number
  totalAmountWithTax: number
  status: number
  salesmanName: string
  createTime: string
  remark?: string
}

// vxe-table 列定义
const vxeColumns = computed(() => [
  {
    field: 'orderNo',
    title: '订单号',
    width: 160,
    sortable: true,
    formatter: ({ cellValue, row }: any) => `<a style="color: #1890ff; cursor: pointer;">${cellValue}</a>`,
  },
  { field: 'customerName', title: '客户', width: 140 },
  { field: 'orderDate', title: '订单日期', width: 110 },
  {
    field: 'totalAmountWithTax',
    title: '订单金额',
    width: 120,
    sortable: true,
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
  { field: 'salesmanName', title: '销售员', width: 100 },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 140, fixed: 'right', type: 'action' },
])

// 筛选字段
const filterFields = [
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '待审批', value: 1 },
    { label: '已审批', value: 2 },
    { label: '部分出库', value: 3 },
    { label: '已完成', value: 4 },
    { label: '已取消', value: 5 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

// 是否启用了筛选条件
const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<SaleOrder[]>([])
const formVisible = ref(false)
const isEdit = ref(false)
const editRecord = ref<SaleOrder | null>(null)
const searchFilters = reactive<Record<string, any>>({})
const lastUpdated = ref<string>('')
const importModalVisible = ref(false)
const selectedRowKeys = ref<number[]>([])

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => dataSource.value.filter(r => r.status === 0).length)
const pendingCount = computed(() => dataSource.value.filter(r => r.status === 1).length)
const completedCount = computed(() => dataSource.value.filter(r => r.status === 4).length)
const totalAmount = computed(() => dataSource.value.reduce((s, r) => s + (r.totalAmountWithTax || 0), 0))

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

// 批量操作独立 loading
const batchApproving = ref(false)
const batchPrinting = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const total = dataSource.value.reduce((s, r) => s + (r.totalAmountWithTax || 0), 0)
  return [
    { label: '本页小计', value: total, type: 'currency' as const },
  ]
})

function formatRelativeTime(time: string): string {
  return dayjs(time).fromNow()
}

function getStatusColor(status: number): string {
  return ORDER_STATUS_COLOR[status] || 'default'
}

function getStatusText(status: number): string {
  return ORDER_STATUS_TEXT[status] || '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.pageSize,
      ...searchFilters
    }
    const res = await saleOrderApi.getPage(params)
    dataSource.value = res.data?.records || []
    const total = res.data?.total || 0
    pagination.total = total
    if (total > 500) {
      message.info(`当前共 ${total} 条记录，建议添加筛选条件缩小范围`, 3)
    }
    lastUpdated.value = new Date().toISOString()
    if (searchFilters.keyword || Object.keys(searchFilters).some(k => k !== 'sortField' && k !== 'sortOrder' && searchFilters[k])) {
      message.info(`共找到 ${res.data?.total || 0} 条匹配结果`)
    }
  } catch (error) {
    message.error('获取销售订单列表失败')
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

function navigateToDetail(record: SaleOrder, newTab = false) {
  const listIds = dataSource.value.map(item => item.id)
  sessionStorage.setItem('sale_order_list_ids', JSON.stringify(listIds))
  if (newTab) {
    window.open(`/sale/order/${record.id}`, '_blank')
  } else {
    router.push(`/sale/order/${record.id}`)
  }
}

function handleView(record: SaleOrder) {
  navigateToDetail(record)
}

function handleActionMenuClick(key: string, record: SaleOrder) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'print': handlePrint(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}

function handleOrderNoMiddleClick(e: MouseEvent, record: SaleOrder) {
  if (e.button === 1) {
    e.preventDefault()
    navigateToDetail(record, true)
  }
}

function handleAdd() {
  isEdit.value = false
  editRecord.value = null
  formVisible.value = true
}

function handleEdit(record: SaleOrder) {
  isEdit.value = true
  editRecord.value = { ...record }
  formVisible.value = true
}

function handleFormSuccess() {
  formVisible.value = false
  fetchData()
}

async function handleDelete(record: SaleOrder) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除订单「${record.orderNo}」吗？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await saleOrderApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch {
        message.error('删除失败')
      }
    }
  })
}

async function handleBatchDelete(ids: number[]) {
  if (ids.length === 0) return
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${ids.length} 条订单吗？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await saleOrderApi.batchDelete(ids)
        message.success(`成功删除 ${ids.length} 条记录`)
        tableRef.value?.clearSelection()
        fetchData()
      } catch {
        message.error('批量删除失败')
      }
    }
  })
}

function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

async function handleBatchApprove(selectedRows: any[]) {
  if (!selectedRows || selectedRows.length === 0) {
    message.warning('请选择要审批的订单')
    return
  }
  const validRows = selectedRows.filter(r => r.status === 1)
  if (validRows.length === 0) {
    message.warning('选中的订单中没有待审批状态的订单')
    return
  }
  Modal.confirm({
    title: '批量审批',
    content: `确定要审批 ${validRows.length} 条待审批订单吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      batchApproving.value = true
      try {
        await saleOrderApi.batchApprove(validRows.map(r => r.id))
        message.success(`成功审批 ${validRows.length} 条订单`)
        tableRef.value?.clearSelection()
        fetchData()
      } catch {
        message.error('批量审批失败')
      } finally {
        batchApproving.value = false
      }
    }
  })
}

async function handleBatchPrint(selectedRows: any[]) {
  if (!selectedRows || selectedRows.length === 0) {
    message.warning('请选择要打印的订单')
    return
  }
  batchPrinting.value = true
  try {
    await saleOrderApi.batchPrint(selectedRows.map(r => r.id))
    message.success(`打印任务已提交 (${selectedRows.length} 条)`)
  } catch {
    message.error('批量打印失败')
  } finally {
    batchPrinting.value = false
  }
}

function handleSubmit(record: SaleOrder) {
  Modal.confirm({
    title: '确认提交',
    content: `确定要提交订单 "${record.orderNo}" 吗？提交后将进入审批流程。`,
    okText: '确认提交',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await saleOrderApi.submit(record.id)
        message.success('提交成功')
        fetchData()
      } catch {
        message.error('提交失败')
      }
    }
  })
}

function handleApprove(record: SaleOrder) {
  Modal.confirm({
    title: '确认审批',
    content: `确定审批通过订单 "${record.orderNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await saleOrderApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch {
        message.error('审批失败')
      }
    }
  })
}

function handlePrint(record: SaleOrder) {
  Modal.confirm({
    title: '打印',
    content: `打印订单 "${record.orderNo}" ？`,
    okText: '确认打印',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await saleOrderApi.print(record.id)
        message.success('打印任务已提交')
      } catch {
        message.error('打印失败')
      }
    }
  })
}

function handleImport() {
  importModalVisible.value = true
}

async function handleExport() {
  await executeExport({
    fileName: '销售订单',
    total: pagination.total,
    headers: ['订单号', '客户', '订单日期', '订单金额', '状态', '销售员', '创建时间'],
    fetchAll: () => saleOrderApi.export({ ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.orderNo || '', row.customerName || '', row.orderDate || '',
      (row.totalAmountWithTax || row.totalAmount || 0).toFixed(2),
      getStatusText(row.status),
      row.salesmanName || row.salesperson || '', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: SaleOrder) => [
      row.orderNo || '', row.customerName || '', row.orderDate || '',
      (row.totalAmountWithTax || 0).toFixed(2), getStatusText(row.status),
      row.salesmanName || '', row.createTime || ''
    ]),
    maxRows: 10000,
  })
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

function handleSortChange(field: string, order: string) {
  searchFilters.sortField = field
  searchFilters.sortOrder = order
  fetchData()
}

const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => {
    fetchData()
  }, 400)
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('sale:refresh', fetchData)
  window.addEventListener('sale:create', handleAdd)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('sale:refresh', fetchData)
  window.removeEventListener('sale:create', handleAdd)
})

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
}
</script>

<style scoped>
.orders-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  padding: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-pending { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-amount { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 操作列「更多」按钮 */
.action-more-btn {
  padding: 0 4px;
  font-size: 16px;
  vertical-align: middle;
}

/* 列表更新时间戳 */
.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}


.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
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
  .orders-page {
    padding: 8px;
  }
}
</style>
