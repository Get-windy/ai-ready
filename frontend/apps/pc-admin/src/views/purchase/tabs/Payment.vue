<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'purchase-payment-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建付款"
    @add="handleAdd"
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
    </template>

    <template #batch-actions>
      <a-button size="small" @click="handleBatchApprove">批量审批</a-button>
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
        <a-tooltip v-if="record.status === 1" title="审批">
          <a-button type="link" size="small" @click="handleApprove(record)">
            <template #icon><CheckCircleOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </template>
  </TableList>

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="付款单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="付款单号">{{ currentRecord.paymentNo }}</a-descriptions-item>
      <a-descriptions-item label="采购订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="付款日期">{{ currentRecord.paymentDate }}</a-descriptions-item>
      <a-descriptions-item label="付款金额">¥{{ currentRecord.paymentAmount?.toFixed(2) }}</a-descriptions-item>
      <a-descriptions-item label="付款方式">{{ getPaymentMethodText(currentRecord.paymentMethod) }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="收款账户">{{ currentRecord.bankAccount || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建付款弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    title="新建付款单"
    width="700px"
    centered
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
      <a-form-item label="采购订单号" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入采购订单号" />
      </a-form-item>
      <a-form-item label="付款金额" name="paymentAmount">
        <a-input-number v-model:value="formData.paymentAmount" :min="0" :precision="2" placeholder="请输入付款金额" style="width: 100%" />
      </a-form-item>
      <a-form-item label="付款方式" name="paymentMethod">
        <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
          <a-select-option :value="1">银行转账</a-select-option>
          <a-select-option :value="2">现金</a-select-option>
          <a-select-option :value="3">承兑汇票</a-select-option>
          <a-select-option :value="4">微信/支付宝</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="付款日期" name="paymentDate">
        <a-date-picker v-model:value="formData.paymentDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 批量打印弹窗 -->
  <a-modal
    v-model:open="batchPrintModalVisible"
    title="批量打印"
    width="750px"
    centered
    :footer="null"
  >
    <div v-if="printItems.length === 0" style="text-align: center; padding: 40px; color: #999;">
      暂无选中付款单
    </div>
    <template v-else>
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <a-checkbox
          :checked="printItems.every((item: any) => item.checked)"
          :indeterminate="printItems.some((item: any) => item.checked) && !printItems.every((item: any) => item.checked)"
          @change="handlePrintCheckAll"
        >
          全选
        </a-checkbox>
        <a-button type="primary" size="small" @click="handlePrintAll">
          <template #icon><PrinterOutlined /></template>
          全部打印
        </a-button>
      </div>
      <a-table
        :columns="printTableColumns"
        :data-source="printItems"
        :pagination="false"
        :scroll="{ y: 300 }"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'checked'">
            <a-checkbox v-model:checked="record.checked" />
          </template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import TableList from '@/components/TableList/TableList.vue'
import { paymentApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch, validateSelection } from '@/utils/batchOperations'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 160, sortable: true },
  { title: '采购订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '付款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 110, type: 'date' as const },
  { title: '付款金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 120, type: 'currency' as const, sortable: true },
  { title: '付款方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'paymentNo', label: '付款单号', type: 'input' as const, placeholder: '输入付款单号' },
  { key: 'orderNo', label: '采购订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已付款', value: 2 }, { label: '部分付款', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已付款', 3: '部分付款' }

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalAmount = dataSource.value.reduce((s, r) => s + (r.paymentAmount || 0), 0)
  return [
    { label: '本页金额合计', value: totalAmount, type: 'currency' as const },
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function getPaymentMethodText(method: number): string {
  const texts: Record<number, string> = { 1: '银行转账', 2: '现金', 3: '承兑汇票', 4: '微信/支付宝' }
  return texts[method] || '未知'
}

// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  orderNo: '', paymentAmount: undefined as number | undefined,
  paymentMethod: 1, paymentDate: undefined as any, remark: ''
})

const formRules = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  paymentAmount: [{ required: true, message: '请输入付款金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择付款方式', trigger: 'change' }],
  paymentDate: [{ required: true, message: '请选择付款日期', trigger: 'change' }]
}

// ── 批量打印状态 ──────────────────────────────────────
const batchPrintModalVisible = ref(false)
const printItems = ref<any[]>([])

const printTableColumns = [
  { title: '选择', key: 'checked', width: 60 },
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 150 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '付款金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 100 },
  { title: '付款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 120 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await paymentApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
  } catch {
    message.error('获取付款单列表失败')
  } finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }

function handleAdd() {
  formData.orderNo = ''; formData.paymentAmount = undefined; formData.paymentMethod = 1
  formData.paymentDate = undefined; formData.remark = ''
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  try { await paymentApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => paymentApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await paymentApi.create({
      orderNo: formData.orderNo, paymentAmount: formData.paymentAmount,
      paymentMethod: formData.paymentMethod, paymentDate: formData.paymentDate, remark: formData.remark
    })
    message.success('新建付款单成功'); formModalVisible.value = false; fetchData()
  } catch { message.error('新建付款单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批付款单', content: `审批付款单 "${record.paymentNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await paymentApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (!validateSelection(keys, '审批')) return
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => paymentApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  const headers = ['付款单号', '采购订单', '供应商', '付款日期', '付款金额', '付款方式', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.paymentNo || '', row.orderNo || '', row.supplierName || '', row.paymentDate || '',
    (row.paymentAmount || 0).toFixed(2), getPaymentMethodText(row.paymentMethod),
    getStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '付款单')
}

const handlePrintCheckAll = (e: any) => {
  const checked = e.target.checked
  printItems.value.forEach((item: any) => { item.checked = checked })
}

const handlePrintAll = () => {
  const toPrint = printItems.value.filter((item: any) => item.checked)
  if (toPrint.length === 0) { message.warning('请选择要打印的付款单'); return }
  message.success(`正在发送 ${toPrint.length} 个付款单的打印任务...`)
}

const handleBatchPrint = () => {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择付款单'); return }
  const selected = dataSource.value.filter((item: any) => keys.includes(item.id))
  printItems.value = selected.map((item: any) => ({ ...item, checked: true }))
  batchPrintModalVisible.value = true
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
