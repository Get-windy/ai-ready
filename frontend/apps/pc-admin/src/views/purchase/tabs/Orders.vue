<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :show-export="true"
    :table-key="'purchase-order-list'"
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
      <a-button size="small" @click="handleBatchClose">批量关闭</a-button>
      <a-button size="small" @click="handleBatchPrint">批量打印</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
    </template>

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
        <a-tooltip v-if="record.status === 1" title="审核通过">
          <a-button type="link" size="small" @click="handleApprove(record)">
            <template #icon><AuditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 2" title="关闭">
          <a-button type="link" size="small" @click="handleClose(record)">
            <template #icon><StopOutlined /></template>
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
  <PurchaseOrderFormModal
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
  EyeOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined,
  AuditOutlined, PrinterOutlined, ImportOutlined, StopOutlined
} from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import PurchaseOrderFormModal from '../components/PurchaseOrderFormModal.vue'
import { purchaseOrderApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch, validateSelection } from '@/utils/batchOperations'

interface PurchaseOrder {
  id: number; orderNo: string; supplierName: string; orderDate: string
  totalAmount: number; totalAmountWithTax: number; status: number
  purchaserName?: string; buyerName?: string; createTime: string; remark?: string
}

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160, sortable: true },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110, type: 'date' as const },
  { title: '订单金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 120, type: 'currency' as const, sortable: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '采购员', dataIndex: 'purchaserName', key: 'purchaserName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 },
    { label: '已审批', value: 2 }, { label: '部分入库', value: 3 },
    { label: '已完成', value: 4 }, { label: '已关闭', value: 5 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red'
}
const statusTextMap: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '部分入库', 4: '已完成', 5: '已关闭'
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

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalAmount = dataSource.value.reduce((s, r) => s + (r.totalAmountWithTax || 0), 0)
  return [
    { label: '本页金额合计', value: totalAmount, type: 'currency' as const },
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseOrderApi.getPage({ current: pagination.current, size: pagination.pageSize, ...searchFilters })
    // 兼容标准包装响应和无包装响应
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch {
    message.error('获取采购订单列表失败')
    dataSource.value = []
  } finally { loading.value = false }
}

function handleView(record: PurchaseOrder) { router.push(`/purchase/order/${record.id}`) }
function handleAdd() { isEdit.value = false; editRecord.value = null; formVisible.value = true }
function handleEdit(record: PurchaseOrder) { isEdit.value = true; editRecord.value = { ...record }; formVisible.value = true }
function handleFormSuccess() { formVisible.value = false; fetchData() }

async function handleDelete(record: PurchaseOrder) {
  try { await purchaseOrderApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}

async function handleBatchDelete(ids: number[]) {
  try { await purchaseOrderApi.batchDelete(ids); message.success(`成功删除 ${ids.length} 条记录`); fetchData() }
  catch { message.error('批量删除失败') }
}

function handleSubmit(record: PurchaseOrder) {
  Modal.confirm({ title: '确认提交', content: `提交订单 "${record.orderNo}" ？`, okText: '确认', centered: true,
    async onOk() { try { await purchaseOrderApi.submit(record.id); message.success('提交成功'); fetchData() } catch { message.error('提交失败') } }
  })
}

function handleApprove(record: PurchaseOrder) {
  Modal.confirm({ title: '确认审批', content: `审批通过 "${record.orderNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await purchaseOrderApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleClose(record: PurchaseOrder) {
  Modal.confirm({ title: '确认关闭', content: `关闭订单 "${record.orderNo}" ？`, okText: '确认关闭', centered: true,
    async onOk() { try { await purchaseOrderApi.close(record.id); message.success('已关闭'); fetchData() } catch { message.error('关闭失败') } }
  })
}

function handlePrint(record: PurchaseOrder) {
  Modal.confirm({ title: '打印', content: `打印订单 "${record.orderNo}" ？`, okText: '确认', centered: true,
    async onOk() { try { await purchaseOrderApi.print(record.id); message.success('打印任务已提交') } catch { message.error('打印失败') } }
  })
}

function handleImport() {
  Modal.confirm({
    title: '导入采购订单',
    content: '导入功能尚在开发中，敬请期待。',
    okText: '知道了', cancelText: null, centered: true
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (!validateSelection(keys, '审批')) return
  Modal.confirm({ title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() { try { await purchaseOrderApi.batchApprove(keys); message.success(`成功审批 ${keys.length} 条`); fetchData() } catch { message.error('批量审批失败') } }
  })
}

async function handleBatchClose() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (!validateSelection(keys, '关闭')) return
  Modal.confirm({ title: '批量关闭', content: `关闭选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => purchaseOrderApi.close(id), '批量关闭')
      if (result.successCount > 0) fetchData()
    }
  })
}

async function handleBatchPrint() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (!validateSelection(keys, '打印')) return
  Modal.confirm({ title: '批量打印', content: `打印选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => purchaseOrderApi.print(id), '批量打印')
    }
  })
}

function handleExport() {
  const headers = ['订单号', '供应商', '订单日期', '订单金额', '状态', '采购员', '创建时间']
  const rows = dataSource.value.map(r => [
    r.orderNo || '', r.supplierName || '', r.orderDate || '',
    (r.totalAmountWithTax || 0).toFixed(2), getStatusText(r.status),
    r.purchaserName || r.buyerName || '', r.createTime || ''
  ])
  exportCsv(headers, rows, '采购订单')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => { fetchData() })
</script>
