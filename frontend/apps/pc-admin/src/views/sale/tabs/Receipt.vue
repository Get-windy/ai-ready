<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'sale-receipt-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    :show-export="true"
    add-text="新建收款"
    add-permission="'sale:receipt:create'"
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
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配收款单">
        <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
        <a-button @click="handleResetFilters">清除筛选</a-button>
      </a-empty>
      <a-empty v-else description="暂无收款单">
        <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        <a-button type="primary" @click="handleAdd">新建收款单</a-button>
      </a-empty>
    </template>

    <template #batch-actions>
      <a-button v-permission="'sale:receipt:approve'" size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="0" class="action-cell-inner">
        <a-tooltip title="查看详情">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-dropdown trigger="click">
          <a-button type="link" size="small" class="action-more-btn">
            <template #icon><EllipsisOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
              <a-menu-item v-if="record.status === 1" key="approve">
                <CheckCircleOutlined /> 审批
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="delete" danger>
                <DeleteOutlined /> 删除
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </a-space>
    </template>
  </TableList>

  <a-modal v-model:open="detailVisible" title="收款单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="收款单号">{{ currentRecord.receiptNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="收款日期">{{ currentRecord.receiptDate }}</a-descriptions-item>
      <a-descriptions-item label="收款金额">¥{{ currentRecord.receiptAmount?.toFixed(2) }}</a-descriptions-item>
      <a-descriptions-item label="收款方式">{{ currentRecord.receiptMethod }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="收款账户">{{ currentRecord.bankAccount || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div class="detail-modal-footer"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <a-modal v-model:open="receiptFormVisible" title="新建收款单" width="600px" centered
    :confirm-loading="receiptFormSubmitting" ok-text="确认创建" cancel-text="取消"
    @ok="handleReceiptFormSubmit" @cancel="receiptFormVisible = false">
    <a-form ref="receiptFormRef" :model="receiptFormData" :rules="receiptFormRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="销售订单" name="orderNo"><a-input v-model:value="receiptFormData.orderNo" placeholder="请输入销售订单号" /></a-form-item>
      <a-form-item label="客户" name="customerName"><a-input v-model:value="receiptFormData.customerName" placeholder="请输入客户名称" /></a-form-item>
      <a-form-item label="收款金额" name="receiptAmount"><a-input-number v-model:value="receiptFormData.receiptAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" placeholder="请输入收款金额" /></a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="收款方式" name="receiptMethod" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="receiptFormData.receiptMethod" placeholder="请选择收款方式">
              <a-select-option value="银行转账">银行转账</a-select-option>
              <a-select-option value="现金">现金</a-select-option>
              <a-select-option value="微信">微信</a-select-option>
              <a-select-option value="支付宝">支付宝</a-select-option>
              <a-select-option value="支票">支票</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收款日期" name="receiptDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="receiptFormData.receiptDate" style="width: 100%" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="收款账户" name="bankAccount"><a-input v-model:value="receiptFormData.bankAccount" placeholder="请输入收款账户" /></a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="receiptFormData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleReceiptTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, InboxOutlined, SearchOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { receiptApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { executeBatch } from '@/utils/batchOperations'
import { useExport } from '@/composables/useExport'

const { execute: executeExport } = useExport()
const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  { title: '收款单号', dataIndex: 'receiptNo', key: 'receiptNo', width: 160, sortable: true },
  { title: '销售订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '收款日期', dataIndex: 'receiptDate', key: 'receiptDate', width: 110, type: 'date' as const },
  { title: '收款金额', dataIndex: 'receiptAmount', key: 'receiptAmount', width: 120, type: 'currency' as const, sortable: true },
  { title: '收款方式', dataIndex: 'receiptMethod', key: 'receiptMethod', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 100, fixed: 'right' as const, type: 'action' as const }
]
const filterFields = [
  { key: 'receiptNo', label: '收款单号', type: 'input' as const, placeholder: '输入收款单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已收款', value: 2 }, { label: '部分收款', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]
const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已收款', 3: '部分收款' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalAmount = dataSource.value.reduce((s, r) => s + (r.receiptAmount || 0), 0)
  return [
    { label: '本页金额合计', value: totalAmount, type: 'currency' as const },
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const receiptFormVisible = ref(false)
const receiptFormSubmitting = ref(false)
const receiptFormRef = ref<FormInstance>()
const receiptFormData = reactive({ orderNo: '', customerName: '', receiptAmount: 0, receiptMethod: undefined as string | undefined, receiptDate: undefined as any, bankAccount: '', remark: '' })
const receiptFormRules = {
  orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  receiptAmount: [{ required: true, type: 'number' as const, message: '请输入收款金额', trigger: 'blur' }],
  receiptMethod: [{ required: true, message: '请选择收款方式', trigger: 'change' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await receiptApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch { message.error('获取收款单列表失败') }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  receiptFormData.orderNo = ''; receiptFormData.customerName = ''; receiptFormData.receiptAmount = 0
  receiptFormData.receiptMethod = undefined; receiptFormData.receiptDate = undefined
  receiptFormData.bankAccount = ''; receiptFormData.remark = ''
  receiptFormVisible.value = true
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'approve': handleApprove(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除收款单「${record.receiptNo}」吗？删除后数据不可恢复。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    onOk: async () => {
      try { await receiptApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => receiptApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleReceiptFormSubmit = async () => {
  try { await receiptFormRef.value?.validate() } catch { return }
  receiptFormSubmitting.value = true
  try {
    await receiptApi.create({
      orderNo: receiptFormData.orderNo, customerName: receiptFormData.customerName,
      receiptAmount: receiptFormData.receiptAmount, receiptMethod: receiptFormData.receiptMethod,
      receiptDate: receiptFormData.receiptDate, bankAccount: receiptFormData.bankAccount, remark: receiptFormData.remark
    })
    message.success('新建收款单成功'); receiptFormVisible.value = false; pagination.current = 1; fetchData()
  } catch { message.error('新建收款单失败') }
  finally { receiptFormSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批收款单', content: `审批收款单 "${record.receiptNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await receiptApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择收款单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => receiptApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '收款单',
    headers: ['收款单号', '销售订单', '客户', '收款日期', '收款金额', '收款方式', '状态', '创建时间'],
    fetchAll: () => receiptApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.receiptNo || '', row.orderNo || '', row.customerName || '', row.receiptDate || '',
      row.receiptAmount?.toFixed(2) || '0.00', row.receiptMethod || '', getStatusText(row.status), row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.receiptNo || '', row.orderNo || '', row.customerName || '', row.receiptDate || '',
      row.receiptAmount?.toFixed(2) || '0.00', row.receiptMethod || '', getStatusText(row.status), row.createTime || ''
    ]),
    total: pagination.total,
  })
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('sale:refresh', fetchData)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('sale:refresh', fetchData)
})

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}
</script>

<style scoped>
.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.detail-modal-footer { text-align: right; margin-top: 16px; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
