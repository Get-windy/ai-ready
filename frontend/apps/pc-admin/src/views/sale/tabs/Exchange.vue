<template>
  <div class="sale-exchange-page">
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
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">换货单总数</div>
        </div>
        <SwapOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'sale-exchange-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建换货"
      add-permission="'sale:exchange:create'"
      @add="handleAdd"
      @edit="handleEdit"
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
            当前筛选条件下无匹配换货单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无换货单，点击「新建换货单」开始创建
          </p>
        </template>
      </div>
    </template>

    <template #batch-actions>
      <a-button v-permission="'sale:exchange:approve'" size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #action="{ record }">
      <a-space :size="0" class="action-cell-inner">
        <a-tooltip title="查看详情">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 0" title="编辑">
          <a-button v-permission.disabled="'sale:exchange:update'" type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
		  <PrintButton :record="record" :business-id="record.id" business-type="sale_exchange" button-type="link" button-size="small" tooltip="打印" />
        <a-dropdown trigger="click">
          <a-button type="link" size="small" class="action-more-btn">
            <template #icon><EllipsisOutlined /></template>
          </a-button>
          <template #overlay>
            <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
              <a-menu-item v-if="record.status === 0" key="submit">
                <SendOutlined /> 提交
              </a-menu-item>
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

  <a-drawer v-model:open="detailVisible" title="换货单详情" placement="right" width="80vw" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="换货单号">{{ currentRecord.exchangeNo }}</a-descriptions-item>
      <a-descriptions-item label="关联订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="换货日期">{{ currentRecord.exchangeDate }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="换货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
  </a-drawer>

  <a-modal v-model:open="formModalVisible" :title="formMode === 'add' ? '新建换货单' : '编辑换货单'" width="700px" centered
    :confirm-loading="formSubmitting" ok-text="确认" cancel-text="取消"
    @ok="handleFormSubmit" @cancel="formModalVisible = false">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="关联订单" name="orderNo"><a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" size="small" /></a-form-item>
      <a-form-item label="客户" name="customerName"><a-input v-model:value="formData.customerName" placeholder="请输入客户名称" size="small" /></a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="换货原因" name="reason" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.reason" placeholder="请选择换货原因" size="small">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="规格不符">规格不符</a-select-option>
              <a-select-option value="数量错误">数量错误</a-select-option>
              <a-select-option value="客户要求">客户要求</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="换货日期" name="exchangeDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.exchangeDate" size="small" style="width: 100%" /></a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="退换产品" name="outItem"><a-input v-model:value="formData.outItem" placeholder="请输入需要退换的产品名称" size="small" /></a-form-item>
      <a-form-item label="替换产品" name="inItem"><a-input v-model:value="formData.inItem" placeholder="请输入替换的产品名称" size="small" /></a-form-item>
      <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" /></a-form-item>
    </a-form>
  </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleExchangeTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, CheckCircleOutlined, InboxOutlined, SearchOutlined, EllipsisOutlined, FileOutlined, ClockCircleOutlined, SwapOutlined, WarningOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { saleExchangeApi } from '@/api/erp'
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

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => dataSource.value.filter(r => r.status === 0).length)
const pendingCount = computed(() => dataSource.value.filter(r => r.status === 1).length)
const completedCount = computed(() => dataSource.value.filter(r => r.status === 4).length)

const tableDataSource = dataSource

const vxeColumns = computed(() => [
  { title: '换货单号', field: 'exchangeNo', width: 160, sortable: true },
  { title: '关联订单', field: 'orderNo', width: 160 },
  { title: '客户', field: 'customerName', width: 140 },
  { title: '换货日期', field: 'exchangeDate', width: 110 },
  { title: '状态', field: 'status', width: 100, formatter: ({ cellValue }) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>` },
  { title: '创建人', field: 'creatorName', width: 100 },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', field: 'action', width: 130, fixed: 'right', type: 'action' }
])

  // 选择变化处理
  const selectedRowKeys = ref<number[]>([])
  function handleSelectionChange(keys: number[]) {
    selectedRowKeys.value = keys
  }
const filterFields = [
  { key: 'exchangeNo', label: '换货单号', type: 'input' as const, placeholder: '输入换货单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已审批', value: 2 },
    { label: '换货中', value: 3 }, { label: '完成', value: 4 }, { label: '已取消', value: 5 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]
const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '换货中', 4: '完成', 5: '已取消' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()
const formData = reactive({ id: undefined as number | undefined, orderNo: '', customerName: '', reason: undefined as string | undefined, exchangeDate: undefined as any, outItem: '', inItem: '', remark: '' })
const formRules = {
  orderNo: [{ required: true, message: '请输入关联订单号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择换货原因', trigger: 'change' }],
  exchangeDate: [{ required: true, message: '请选择换货日期', trigger: 'change' }]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await saleExchangeApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
  } catch (err) { console.warn('[销售换货] 获取换货单列表', err); hasError.value = true }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  formMode.value = 'add'; formData.id = undefined; formData.orderNo = ''; formData.customerName = ''
  formData.reason = undefined; formData.exchangeDate = undefined; formData.outItem = ''; formData.inItem = ''; formData.remark = ''
  formModalVisible.value = true
}
function handleParentCreate() { handleAdd() }
function handleEdit(record: any) {
  formMode.value = 'edit'; formData.id = record.id; formData.orderNo = record.orderNo || ''
  formData.customerName = record.customerName || ''; formData.reason = record.reason || undefined
  formData.exchangeDate = record.exchangeDate || undefined; formData.outItem = record.outItem || ''
  formData.inItem = record.inItem || ''; formData.remark = record.remark || ''
  formModalVisible.value = true
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除换货单「${record.exchangeNo}」吗？删除后数据不可恢复。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    onOk: async () => {
      try { await saleExchangeApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (err) { console.warn('[销售换货] 删除换货单', err); message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}
async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => saleExchangeApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    if (formMode.value === 'add') {
      await saleExchangeApi.create({ ...formData })
    } else {
      await saleExchangeApi.update(formData.id!, { ...formData })
    }
    message.success(formMode.value === 'add' ? '新建换货单成功' : '编辑换货单成功')
    formModalVisible.value = false; pagination.current = 1; fetchData()
  } catch (err) { console.warn('[销售换货] 保存换货单', err); message.error('操作失败') }
  finally { formSubmitting.value = false }
}

function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交换货单', content: `提交换货单 "${record.exchangeNo}" ？`, okText: '确认提交', centered: true,
    async onOk() { try { await saleExchangeApi.submit(record.id); message.success('提交成功'); fetchData() } catch (err) { console.warn('[销售换货] 提交换货单', err); message.error('提交失败') } }
  })
}
function handleApprove(record: any) {
  Modal.confirm({
    title: '审批换货单', content: `审批换货单 "${record.exchangeNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await saleExchangeApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err) { console.warn('[销售换货] 审批换货单', err); message.error('审批失败') } }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择换货单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => saleExchangeApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '换货单',
    headers: ['换货单号', '关联订单', '客户', '换货日期', '状态', '创建人', '创建时间'],
    fetchAll: () => saleExchangeApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.customerName || '', row.exchangeDate || '',
      getStatusText(row.status), row.creatorName || '', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.customerName || '', row.exchangeDate || '',
      getStatusText(row.status), row.creatorName || '', row.createTime || ''
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
.sale-exchange-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  padding: 16px;
}

.sale-exchange-page > :deep(.vxe-table-list-container) {
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
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}






/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
  .sale-exchange-page { padding: 8px; }
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
</style>
