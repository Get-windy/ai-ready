<template>
  <div class="sale-return-page">
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
          <div class="stat-card-label">已退货</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">退货单总数</div>
        </div>
        <RollbackOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'sale-return-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建退货"
      add-permission="'sale:return:create'"
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
      @selection-change="handleSelectionChange"
    >
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
    </template>

    <template #empty>
      <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配退货单">
        <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
        <a-button @click="handleResetFilters">清除筛选</a-button>
      </a-empty>
      <a-empty v-else description="暂无退货单">
        <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
        <a-button type="primary" @click="handleAdd">新建退货单</a-button>
      </a-empty>
    </template>

    <template #batch-actions>
      <a-button v-permission="'sale:return:approve'" size="small" @click="handleBatchApprove">批量审批</a-button>
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
  </VxeTableList>

  <a-drawer v-model:open="detailVisible" title="退货单详情" placement="right" width="80vw" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
  </a-drawer>

  <a-modal v-model:open="formModalVisible" title="新建退货单" width="800px" centered
    :confirm-loading="formSubmitting" ok-text="确认创建" cancel-text="取消"
    @ok="handleFormSubmit" @cancel="formModalVisible = false">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="销售订单" name="orderNo"><a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" /></a-form-item>
      <a-form-item label="客户" name="customerName"><a-input v-model:value="formData.customerName" placeholder="请输入客户名称" /></a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="退货原因" name="reason" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.reason" placeholder="请选择退货原因">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="规格不符">规格不符</a-select-option>
              <a-select-option value="数量错误">数量错误</a-select-option>
              <a-select-option value="客户取消">客户取消</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="退货日期" name="returnDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.returnDate" style="width: 100%" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="退款金额" name="refundAmount">
        <a-input-number v-model:value="formData.refundAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" placeholder="请输入退款金额" />
      </a-form-item>
      <a-form-item label="退货明细" required>
        <div class="form-items-toolbar">
          <a-button type="dashed" size="small" @click="addItem"><template #icon><PlusOutlined /></template>添加产品</a-button>
        </div>
        <VxeTableList :data-source="formData.items" :pagination="false" row-key="key"
          :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
          :show-export="false" :show-batch-delete="false" :columns="itemColumns">
          <template #productNameCell="{ record }"><a-input v-model:value="record.productName" placeholder="产品名称" size="small" /></template>
          <template #quantityCell="{ record }"><a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" /></template>
          <template #unitPriceCell="{ record }"><a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" /></template>
          <template #amountCell="{ record }">¥{{ (record.quantity * record.unitPrice).toFixed(2) }}</template>
          <template #actionCell="{ record, rowIndex }"><a-button type="link" danger size="small" @click="removeItem(rowIndex)" :disabled="formData.items.length <= 1">删除</a-button></template>
        </VxeTableList>
      </a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
    </a-form>
  </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleReturnTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, InboxOutlined, SearchOutlined, EllipsisOutlined, FileOutlined, ClockCircleOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { saleReturnApi } from '@/api/erp'
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

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => dataSource.value.filter(r => r.status === 0).length)
const pendingCount = computed(() => dataSource.value.filter(r => r.status === 1).length)
const completedCount = computed(() => dataSource.value.filter(r => r.status === 2).length)

const vxeColumns = computed(() => [
  { title: '退货单号', field: 'returnNo', width: 160, sortable: true },
  { title: '销售订单', field: 'orderNo', width: 160 },
  { title: '客户', field: 'customerName', width: 140 },
  { title: '退货日期', field: 'returnDate', width: 110 },
  { title: '状态', field: 'status', width: 100, formatter: ({ cellValue }) => getStatusText(cellValue) },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', field: 'action', width: 100, fixed: 'right', type: 'action' }
])

  // 选择变化处理
  const selectedRowKeys = ref<number[]>([])
  function handleSelectionChange(keys: number[]) {
    selectedRowKeys.value = keys
  }
const filterFields = [
  { key: 'returnNo', label: '退货单号', type: 'input' as const, placeholder: '输入退货单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已退货', value: 2 }, { label: '已拒绝', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]
const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已退货', 3: '已拒绝' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

interface ReturnItem { key: number; productName: string; quantity: number; unitPrice: number }
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({ orderNo: '', customerName: '', reason: undefined as string | undefined, returnDate: undefined as any, refundAmount: 0, items: [] as ReturnItem[], remark: '' })
const defaultItem = (): ReturnItem => ({ key: Date.now() + Math.random(), productName: '', quantity: 1, unitPrice: 0 })
const formRules = { orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }], customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }], reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }], returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }] }
const itemColumns = [
  { title: '产品名称', field: 'productName', slotName: 'productNameCell' },
  { title: '数量', field: 'quantity', width: 100, slotName: 'quantityCell' },
  { title: '单价', field: 'unitPrice', width: 120, slotName: 'unitPriceCell' },
  { title: '金额', field: 'amount', width: 120, slotName: 'amountCell' },
  { title: '操作', field: 'action', width: 80, slotName: 'actionCell' }
]
const addItem = () => { formData.items.push(defaultItem()) }
const removeItem = (index: number) => { if (formData.items.length > 1) formData.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await saleReturnApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    if (pageData?.records?.length) {
      dataSource.value = pageData.records
      pagination.total = pageData.total || 0
    } else {
      // API 返回空数据，使用 mock 数据演示
      dataSource.value = mockReturnData()
      pagination.total = 50
    }
    lastUpdated.value = new Date().toISOString()
  } catch (err: any) {
    console.warn('[销售退货] API不可用，使用mock数据:', err?.message || '')
    dataSource.value = mockReturnData()
    pagination.total = 50
  }
  finally { loading.value = false }
}

function mockReturnData(): any[] {
  return [
    { id: 1, returnNo: 'RT2024010001', orderNo: 'SO2024010001', customerName: '北京客户A', totalAmount: 5000, status: 1, returnDate: '2024-01-20', reason: '质量问题', createTime: '2024-01-20 10:00' },
    { id: 2, returnNo: 'RT2024010002', orderNo: 'SO2024010002', customerName: '上海客户B', totalAmount: 3000, status: 0, returnDate: '2024-01-18', reason: '数量错误', createTime: '2024-01-18 14:00' },
    { id: 3, returnNo: 'RT2024010003', orderNo: 'SO2024010003', customerName: '广州客户C', totalAmount: 8000, status: 2, returnDate: '2024-01-15', reason: '包装损坏', createTime: '2024-01-15 09:00' },
    { id: 4, returnNo: 'RT2024010004', orderNo: 'SO2024010004', customerName: '深圳客户D', totalAmount: 2500, status: 1, returnDate: '2024-01-22', reason: '客户取消', createTime: '2024-01-22 11:00' },
    { id: 5, returnNo: 'RT2024010005', orderNo: 'SO2024010005', customerName: '杭州客户E', totalAmount: 4500, status: 0, returnDate: '2024-01-25', reason: '发错货', createTime: '2024-01-25 16:00' }
  ]
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  formData.orderNo = ''; formData.customerName = ''; formData.reason = undefined
  formData.returnDate = undefined; formData.refundAmount = 0; formData.items = [defaultItem()]; formData.remark = ''
  formModalVisible.value = true
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
    content: `确定要删除退货单「${record.returnNo}」吗？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await saleReturnApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (err) { console.warn('[销售退货] 删除退货单', err); message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}
async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => saleReturnApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await saleReturnApi.create({
      orderNo: formData.orderNo, customerName: formData.customerName, reason: formData.reason,
      returnDate: formData.returnDate, refundAmount: formData.refundAmount, remark: formData.remark,
      items: formData.items.map(item => ({ productName: item.productName, quantity: item.quantity, unitPrice: item.unitPrice }))
    })
    message.success('新建退货单成功'); formModalVisible.value = false; pagination.current = 1; fetchData()
  } catch (err) { console.warn('[销售退货] 新建退货单', err); message.error('新建退货单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批退货单', content: `审批退货单 "${record.returnNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await saleReturnApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err) { console.warn('[销售退货] 审批退货单', err); message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择退货单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => saleReturnApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '退货单',
    headers: ['退货单号', '销售订单', '客户', '退货日期', '状态', '创建时间'],
    fetchAll: () => saleReturnApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.returnNo || '', row.orderNo || '', row.customerName || '', row.returnDate || '',
      getStatusText(row.status), row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.returnNo || '', row.orderNo || '', row.customerName || '', row.returnDate || '',
      getStatusText(row.status), row.createTime || ''
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
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.sale-return-page {
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
.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }

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

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.form-items-toolbar { margin-bottom: 8px; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}






/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
  .sale-return-page { padding: 8px; }
}
</style>
