<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :show-export="true"
    :table-key="'sale-order-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    add-text="新建订单"
    @add="handleAdd"
    @edit="handleEdit"
    @view="handleView"
    @delete="handleDelete"
    @batch-delete="handleBatchDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    @export="handleExport"
  >
    <template #toolbar-actions>
      <a-button @click="handleImport">
        <template #icon><ImportOutlined /></template>
        导入
      </a-button>
    </template>

    <template #batch-actions>
      <a-button size="small" @click="handleBatchApprove">批量审批</a-button>
      <a-button size="small" @click="handleBatchPrint">批量打印</a-button>
    </template>

    <!-- 自定义列：状态 -->
    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
    </template>

    <!-- 自定义列：操作 -->
    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 0" title="编辑">
          <a-button type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 0" title="提交审核">
          <a-button type="link" size="small" @click="handleSubmit(record)">
            <template #icon><CheckCircleOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 1" title="审核">
          <a-button type="link" size="small" @click="handleApprove(record)">
            <template #icon><AuditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="打印">
          <a-button type="link" size="small" @click="handlePrint(record)">
            <template #icon><PrinterOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-popconfirm
          v-if="record.status === 0 || record.status === 5"
          title="确定删除该订单？"
          @confirm="handleDelete(record)"
        >
          <a-tooltip title="删除">
            <a-button type="link" size="small" danger>
              <template #icon><DeleteOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-popconfirm>
      </a-space>
    </template>
  </TableList>

  <!-- 新建/编辑订单弹窗 -->
  <SaleOrderFormModal
    v-model:open="formVisible"
    :is-edit="isEdit"
    :record="editRecord"
    @success="handleFormSuccess"
  />
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  CheckCircleOutlined,
  AuditOutlined,
  PrinterOutlined,
  ImportOutlined
} from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import SaleOrderFormModal from '../components/SaleOrderFormModal.vue'
import { saleOrderApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'

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

// 列定义
const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160, sortable: true },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110, type: 'date' as const, dateFormat: 'YYYY-MM-DD' },
  { title: '订单金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 120, type: 'currency' as const, sortable: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '销售员', dataIndex: 'salesmanName', key: 'salesmanName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const, type: 'action' as const }
]

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

const statusColorMap: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red'
}

const statusTextMap: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '部分出库', 4: '已完成', 5: '已取消'
}

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<SaleOrder[]>([])
const formVisible = ref(false)
const isEdit = ref(false)
const editRecord = ref<SaleOrder | null>(null)
const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalAmount = dataSource.value.reduce((s, r) => s + (r.totalAmountWithTax || 0), 0)
  return [
    { label: '本页金额合计', value: totalAmount, type: 'currency' as const },
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})


function getStatusColor(status: number): string {
  return statusColorMap[status] || 'default'
}

function getStatusText(status: number): string {
  return statusTextMap[status] || '未知'
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
    pagination.total = res.data?.total || 0
  } catch (error) {
    message.error('获取销售订单列表失败')
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

function handleView(record: SaleOrder) {
  router.push(`/sale/order/${record.id}`)
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
  try {
    await saleOrderApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch {
    message.error('删除失败')
  }
}

async function handleBatchDelete(ids: number[]) {
  try {
    await saleOrderApi.batchDelete(ids)
    message.success(`成功删除 ${ids.length} 条记录`)
    fetchData()
  } catch {
    message.error('批量删除失败')
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
  Modal.confirm({
    title: '导入销售订单',
    content: '导入功能可通过 Excel/CSV 文件批量创建销售订单。功能尚在完善中，请关注后续版本更新。',
    okText: '知道了',
    centered: true
  })
}

function handleExport() {
  const headers = ['订单号', '客户', '订单日期', '订单金额', '状态', '销售员', '创建时间']
  const rows = dataSource.value.map((row: SaleOrder) => [
    row.orderNo || '', row.customerName || '', row.orderDate || '',
    (row.totalAmountWithTax || 0).toFixed(2), getStatusText(row.status),
    row.salesmanName || '', row.createTime || ''
  ])
  exportCsv(headers, rows, '销售订单')
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要审批的订单')
    return
  }
  Modal.confirm({
    title: '确认批量审批',
    content: `确定要批量审批选中的 ${keys.length} 条记录吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await saleOrderApi.batchApprove(keys)
        message.success(`成功审批 ${keys.length} 条记录`)
        fetchData()
      } catch { message.error('批量审批失败') }
    }
  })
}

function handleBatchPrint() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要打印的订单')
    return
  }
  Modal.confirm({
    title: '确认批量打印',
    content: `确定要批量打印选中的 ${keys.length} 条记录吗？`,
    okText: '确认打印',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await saleOrderApi.batchPrint(keys)
        message.success(`打印任务已提交 (${keys.length} 条)`)
      } catch { message.error('批量打印失败') }
    }
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

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>
