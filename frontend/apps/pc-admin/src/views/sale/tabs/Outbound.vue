<template>
  <div class="outbound-page">
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
          <div class="stat-card-label">已出库</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-partial">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ partialCount }}</div>
          <div class="stat-card-label">部分出库</div>
        </div>
        <LoadingOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'sale-outbound-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建出库"
      add-permission="'sale:outbound:create'"
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
          <a-button type="primary" size="small" @click="fetchData as any" class="table-empty-action">
            <ReloadOutlined /> 重试
          </a-button>
        </template>
        <template v-else>
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            当前筛选条件下无匹配出库单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无出库单，点击「新建出库」开始创建
          </p>
        </template>
      </div>
    </template>

    <template #batch-actions>
  <a-button v-permission="'sale:outbound:approve'" size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #action="{ record }">
      <a-space :size="0" class="action-cell-inner">
        <a-tooltip title="查看详情">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
		  <PrintButton :record="record" :business-id="record.id" business-type="sale_outbound" button-type="link" button-size="small" tooltip="打印" />
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

  <a-drawer v-model:open="detailVisible" title="出库单详情" placement="right" width="80vw" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="出库单号">{{ currentRecord.outboundNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="出库日期">{{ currentRecord.outboundDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="物流单号">{{ currentRecord.trackingNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
  </a-drawer>

  <a-modal v-model:open="formModalVisible" title="新建出库单" width="800px" centered
    :confirm-loading="formSubmitting" ok-text="确认创建" cancel-text="取消"
    @ok="handleFormSubmit" @cancel="formModalVisible = false">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="销售订单" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" size="small" />
      </a-form-item>
      <a-form-item label="客户" name="customerName">
        <a-input v-model:value="formData.customerName" placeholder="请输入客户名称" size="small" />
      </a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="仓库" name="warehouseName" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.warehouseName" placeholder="请输入仓库名称" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="物流单号" name="trackingNo" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.trackingNo" placeholder="请输入物流单号" size="small" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="出库日期" name="outboundDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.outboundDate" size="small" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="承运商" name="carrier" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.carrier" placeholder="请输入承运商" size="small" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="出库明细" required>
        <div class="form-items-toolbar">
          <a-button type="dashed" size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>添加产品
          </a-button>
        </div>
        <VxeTableList :data-source="formData.items" :pagination="false as any" row-key="key"
          :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
          :show-export="false" :show-batch-delete="false" :columns="itemColumns">
          <template #productNameCell="{ record }">
            <a-input v-model:value="record.productName" placeholder="产品名称" size="small" />
          </template>
          <template #quantityCell="{ record }">
            <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
          </template>
          <template #unitCell="{ record }">
            <a-input v-model:value="record.unit" placeholder="单位" size="small" />
          </template>
          <template #actionCell="{ record, index }">
            <a-button type="link" danger size="small" @click="removeItem(index)" :disabled="formData.items.length <= 1">删除</a-button>
          </template>
        </VxeTableList>
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleOutboundTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, InboxOutlined, SearchOutlined, EllipsisOutlined, FileOutlined, ClockCircleOutlined, LoadingOutlined, WarningOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { outboundApi } from '@/api/erp'
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
const partialCount = computed(() => dataSource.value.filter(r => r.status === 3).length)

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const vxeColumns = computed(() => [
  { title: '出库单号', field: 'outboundNo', width: 160, sortable: true },
  { title: '销售订单', field: 'orderNo', width: 160 },
  { title: '客户', field: 'customerName', width: 140 },
  { title: '出库日期', field: 'outboundDate', width: 110 },
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
  { key: 'outboundNo', label: '出库单号', type: 'input' as const, placeholder: '输入出库单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已出库', value: 2 }, { label: '部分出库', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已出库', 3: '部分出库' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单 ──
interface OutboundItem { key: number; productName: string; quantity: number; unit: string }
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({ orderNo: '', customerName: '', warehouseName: '', outboundDate: undefined as any, trackingNo: '', carrier: '', items: [] as OutboundItem[], remark: '' })
const defaultItem = (): OutboundItem => ({ key: Date.now() + Math.random(), productName: '', quantity: 1, unit: '' })
const formRules: any = { orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }], customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }], warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }], outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }] }
const itemColumns = [
  { title: '产品名称', field: 'productName', slotName: 'productNameCell' },
  { title: '数量', field: 'quantity', width: 100, slotName: 'quantityCell' },
  { title: '单位', field: 'unit', width: 100, slotName: 'unitCell' },
  { title: '操作', field: 'action', width: 80, slotName: 'actionCell' }
]
const addItem = () => { formData.items.push(defaultItem()) }
const removeItem = (index: number) => { if (formData.items.length > 1) formData.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await outboundApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.total || 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
  } catch (err) { console.warn('[销售出库] 获取出库单列表', err); hasError.value = true }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  formData.orderNo = ''; formData.customerName = ''; formData.warehouseName = ''
  formData.outboundDate = undefined; formData.trackingNo = ''; formData.carrier = ''
  formData.items = [defaultItem()]; formData.remark = ''; formModalVisible.value = true
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
    content: `确定要删除出库单「${record.outboundNo}」吗？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await (outboundApi as any).delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (err) { console.warn('[销售出库] 删除出库单', err); message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}
async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => (outboundApi as any).delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await outboundApi.create({
      orderNo: formData.orderNo, customerName: formData.customerName,
      warehouseName: formData.warehouseName, outboundDate: formData.outboundDate,
      trackingNo: formData.trackingNo, carrier: formData.carrier, remark: formData.remark,
      items: formData.items.map(item => ({ productName: item.productName, quantity: item.quantity, unit: item.unit }))
    })
    message.success('新建出库单成功'); formModalVisible.value = false; pagination.current = 1; fetchData()
  } catch (err) { console.warn('[销售出库] 新建出库单', err); message.error('新建出库单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批出库单', content: `审批出库单 "${record.outboundNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await outboundApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err) { console.warn('[销售出库] 审批出库单', err); message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择出库单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => outboundApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '出库单',
    headers: ['出库单号', '销售订单', '客户', '出库日期', '状态', '创建时间'],
    fetchAll: () => outboundApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.outboundNo || '', row.orderNo || '', row.customerName || '', row.outboundDate || '',
      getStatusText(row.status), row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.outboundNo || '', row.orderNo || '', row.customerName || '', row.outboundDate || '',
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
.outbound-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  padding: 16px;
}

.outbound-page > :deep(.vxe-table-list-container) {
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
.stat-partial { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

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

.action-more-btn {
  padding: 0 4px;
  font-size: 16px;
  vertical-align: middle;
}

.form-items-toolbar {
  margin-bottom: 8px;
}


.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
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
  .outbound-page {
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
