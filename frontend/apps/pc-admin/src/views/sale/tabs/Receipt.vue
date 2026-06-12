<template>
  <div class="receipt-page">
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
          <div class="stat-card-label">已收款</div>
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

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'sale-receipt-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建收款"
      add-permission="'sale:receipt:create'"
      @add="handleAdd"
      @view="handleView"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="debounceClick('refresh', fetchData)"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @cell-dblclick="handleView"
      @selection-change="handleSelectionChange"
    >
    <template #toolbar-actions>
      <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
        更新 {{ dayjs(lastUpdated).format('HH:mm') }}
      </span>
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
            当前筛选条件下无匹配收款单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无收款单，点击「新建收款单」开始创建
          </p>
        </template>
      </div>
    </template>

    <template #batch-actions>
      <a-button v-permission="'sale:receipt:approve'" size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #action="{ record }">
      <a-space :size="0" class="action-cell-inner">
        <a-tooltip title="查看详情">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
		  <PrintButton :record="record" :business-id="record.id" business-type="sale_receipt" button-type="link" button-size="small" tooltip="打印" />
        <a-dropdown trigger="click">
          <a-button type="link" size="small" class="action-more-btn">
            <template #icon><EllipsisOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
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
  </div>

  <a-drawer v-model:open="detailVisible" title="收款单详情" placement="right" width="80vw" :footer="null">
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
  </a-drawer>

  <a-modal v-model:open="receiptFormVisible" title="新建收款单" width="600px" centered
    :confirm-loading="receiptFormSubmitting" ok-text="确认创建" cancel-text="取消"
    @ok="handleReceiptFormSubmit" @cancel="receiptFormVisible = false">
    <a-form ref="receiptFormRef" :model="receiptFormData" :rules="receiptFormRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="销售订单" name="orderNo"><a-input v-model:value="receiptFormData.orderNo" placeholder="请输入销售订单号" size="small" /></a-form-item>
      <a-form-item label="客户" name="customerName"><a-input v-model:value="receiptFormData.customerName" placeholder="请输入客户名称" size="small" /></a-form-item>
      <a-form-item label="收款金额" name="receiptAmount"><a-input-number v-model:value="receiptFormData.receiptAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" placeholder="请输入收款金额" /></a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="收款方式" name="receiptMethod" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="receiptFormData.receiptMethod" placeholder="请选择收款方式" size="small">
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
            <a-date-picker v-model:value="receiptFormData.receiptDate" size="small" style="width: 100%" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="收款账户" name="bankAccount"><a-input v-model:value="receiptFormData.bankAccount" placeholder="请输入收款账户" size="small" /></a-form-item>
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
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, InboxOutlined, SearchOutlined, EllipsisOutlined, FileOutlined, ClockCircleOutlined, DollarOutlined, WarningOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { receiptApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { executeBatch } from '@/utils/batchOperations'
import { useExport } from '@/composables/useExport'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const { execute: executeExport } = useExport()
const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => dataSource.value.filter(r => r.status === 0).length)
const pendingCount = computed(() => dataSource.value.filter(r => r.status === 1).length)
const completedCount = computed(() => dataSource.value.filter(r => r.status === 2).length)
const totalAmount = computed(() => dataSource.value.reduce((s, r) => s + (r.receiptAmount || 0), 0))

const tableDataSource = dataSource

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const vxeColumns = computed(() => [
  { title: '收款单号', field: 'receiptNo', width: 160, sortable: true },
  { title: '销售订单', field: 'orderNo', width: 160 },
  { title: '客户', field: 'customerName', width: 140 },
  { title: '收款日期', field: 'receiptDate', width: 110 },
  { title: '收款金额', field: 'receiptAmount', width: 120, sortable: true, formatter: ({ cellValue }) => cellValue ? `¥${cellValue.toFixed(2)}` : '¥0.00' },
  { title: '收款方式', field: 'receiptMethod', width: 100 },
  { title: '状态', field: 'status', width: 100, formatter: ({ cellValue }) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>` },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', field: 'action', width: 100, fixed: 'right', type: 'action' }
])

  // 选择变化处理
  const selectedRowKeys = ref<number[]>([])
  function handleSelectionChange(keys: number[]) {
    selectedRowKeys.value = keys
  }
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
const receiptFormRules: any = {
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
    hasError.value = false
  } catch (err) { console.warn('[销售收款] 获取收款单列表', err); hasError.value = true }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  receiptFormData.orderNo = ''; receiptFormData.customerName = ''; receiptFormData.receiptAmount = 0
  receiptFormData.receiptMethod = undefined; receiptFormData.receiptDate = undefined
  receiptFormData.bankAccount = ''; receiptFormData.remark = ''
  receiptFormVisible.value = true
}

function handleParentCreate() {
  handleAdd()
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
      catch (err) { console.warn('[销售收款] 删除收款单', err); message.error('删除失败') }
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
  } catch (err) { console.warn('[销售收款] 新建收款单', err); message.error('新建收款单失败') }
  finally { receiptFormSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批收款单', content: `审批收款单 "${record.receiptNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await receiptApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err) { console.warn('[销售收款] 审批收款单', err); message.error('审批失败') } }
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
  window.addEventListener('sale:create', handleParentCreate)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('sale:refresh', fetchData)
  window.removeEventListener('sale:create', handleParentCreate)
})

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.receipt-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  padding: 16px;
}

.receipt-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
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
  .receipt-page {
    padding: 8px;
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
