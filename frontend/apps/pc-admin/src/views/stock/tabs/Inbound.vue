<template>
  <div class="inbound-list-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
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
          <div class="stat-card-label">已入库</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">入库单总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'stock-inbound-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :selectable="true"
      add-text="新建入库"
      @add="handleAdd"
      @view="handleView"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
      :show-export="true"
      @export="handleExport"
      @cell-dblclick="handleView"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
        <a-button type="primary" ghost @click="handleBatchApprove">
          <template #icon><CheckOutlined /></template>
          批量审批
        </a-button>
      </template>

      <template #action="{ record }">
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
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
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
              没有符合条件的入库单，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无入库单，点击「新建入库」开始创建
            </p>
          </template>
        </div>
      </template>
    </VxeTableList>

    <!-- 详情抽屉 -->
    <a-drawer
      v-model:open="detailVisible"
      :title="detailData?.inboundNo || '入库单详情'"
      placement="right"
      width="90vw"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 12 }">
        <template v-if="detailData">
          <a-descriptions bordered :column="2" size="small" style="margin-bottom: 16px">
            <a-descriptions-item label="入库单号">{{ detailData.inboundNo }}</a-descriptions-item>
            <a-descriptions-item label="入库类型">{{ detailData.inboundType }}</a-descriptions-item>
            <a-descriptions-item label="仓库">{{ detailData.warehouseName }}</a-descriptions-item>
            <a-descriptions-item label="入库日期">{{ detailData.inboundDate }}</a-descriptions-item>
            <a-descriptions-item label="状态"><a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="来源单号">{{ detailData.sourceNo || '-' }}</a-descriptions-item>
            <a-descriptions-item label="经手人">{{ detailData.handlerName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(currentRecord?.id)">重试</a-button>
          </template>
        </a-result>
      </a-skeleton>
    </a-drawer>

    <!-- 新建入库弹窗 -->
    <a-modal v-model:open="addVisible" title="新建入库单" width="800px" :confirm-loading="addSubmitting"
      @ok="handleAddSubmit" @cancel="handleAddCancel">
      <a-form ref="addFormRef" :model="addForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="addFormRules">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="采购订单" name="orderNo">
              <a-select v-model:value="addForm.orderNo" size="small" show-search placeholder="请选择采购订单"
                :options="purchaseOrderOptions" :filter-option="filterOption" allow-clear @change="handleOrderChange" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="入库类型" name="inboundType">
              <a-select v-model:value="addForm.inboundType" size="small" placeholder="请选择入库类型" :options="inboundTypeOptions" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="入库仓库" name="warehouseId">
              <a-select v-model:value="addForm.warehouseId" size="small" placeholder="请选择仓库" :options="warehouseOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="预计到货日期" name="expectedDate">
              <a-date-picker v-model:value="addForm.expectedDate" size="small" style="width: 100%" placeholder="请选择预计到货日期" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
          <a-textarea v-model:value="addForm.remark" :rows="2" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
      <a-divider style="margin: 12px 0">入库明细</a-divider>
      <div style="margin-bottom: 12px">
        <a-button type="dashed" size="small" @click="addInboundItem"><template #icon><PlusOutlined /></template>添加明细</a-button>
      </div>
      <VxeTableList :data-source="addForm.items" :pagination="false" row-key="key"
        :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false"
        :show-export="false" :show-batch-delete="false" :columns="addItemColumns">
        <template #isNewCell="{ record }">
          <a-tag v-if="record.isNew" color="green">新增</a-tag><span v-else>-</span>
        </template>
        <template #productNameCell="{ record, rowIndex }">
          <a-select v-model:value="addForm.items[rowIndex].productId" show-search placeholder="选择产品"
            :options="productOptions" style="width: 100%" size="small"
            @change="(val: number) => handleItemProductChange(rowIndex, val)" />
        </template>
        <template #expectedQtyCell="{ record, rowIndex }">
          <a-input-number v-model:value="addForm.items[rowIndex].expectedQty" :min="1" style="width: 100%" size="small" />
        </template>
        <template #actualQtyCell="{ record, rowIndex }">
          <a-input-number v-model:value="addForm.items[rowIndex].actualQty"
            :min="record.isNew ? 1 : 0" style="width: 100%" size="small" />
        </template>
        <template #actionCell="{ record, rowIndex }">
          <a-button type="link" danger size="small" @click="removeInboundItem(rowIndex)">删除</a-button>
        </template>
      </VxeTableList>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined, SearchOutlined, InboxOutlined, EllipsisOutlined,
  ClockCircleOutlined, FileTextOutlined, CheckOutlined, WarningOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inboundApi } from '@/api/erp'
import { executeBatch } from '@/utils/batchOperations'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const emit = defineEmits(['update-count'])

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const pendingCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const completedCount = computed(() => tableData.value.filter(r => r.status === 2).length)

const vxeColumns = computed(() => [
  { field: 'inboundNo', title: '入库单号', width: 160, sortable: true },
  { field: 'inboundType', title: '入库类型', width: 100 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'inboundDate', title: '入库日期', width: 110 },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>`,
  },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'action', title: '操作', width: 140, fixed: 'right', type: 'action' },
])

const selectedRowKeys = ref<number[]>([])
function handleSelectionChange(rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}

const filterFields = [
  { key: 'inboundNo', label: '入库单号', type: 'input' as const, placeholder: '输入入库单号' },
  { key: 'orderNo', label: '采购订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已入库', value: 2 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已入库' }
const summaryData = computed(() => {
  if (tableData.value.length === 0) return undefined
  return [{ label: '本页数量', value: tableData.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

// ── 详情抽屉 ──
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    detailData.value = await inboundApi.getById(id) as any
  } catch (err: any) {
    console.warn('[库存入库] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = null
  detailError.value = null
}

function handleDetailRefresh() {
  if (currentRecord.value?.id) fetchDetail(currentRecord.value.id)
}

// ── 新建入库 ──
interface AddInboundItem { key: number; productId: number | undefined; productName: string; isNew: boolean; expectedQty: number; actualQty: number }
let itemKeyCounter = 0
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ orderNo: undefined as string | undefined, inboundType: 1, warehouseId: undefined as number | undefined, expectedDate: dayjs(), remark: '', items: [] as AddInboundItem[] })
const addFormRules = { warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }], expectedDate: [{ required: true, message: '请选择预计到货日期', trigger: 'change' }] }
const inboundTypeOptions = [
  { value: 1, label: '采购入库' }, { value: 2, label: '退货入库' },
  { value: 3, label: '调拨入库' }, { value: 4, label: '其他入库' }
]
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
const purchaseOrderOptions = [
  { value: 'PO-2024-001', label: 'PO-2024-001 / 供应商A' },
  { value: 'PO-2024-002', label: 'PO-2024-002 / 供应商B' },
  { value: 'PO-2024-003', label: 'PO-2024-003 / 供应商C' }
]
const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' }, { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' }, { value: 4, label: 'PROD-004 包装箱(大)' }
]
const addItemColumns = [
  { title: '来源', field: 'isNew', width: 60, slotName: 'isNewCell' },
  { title: '产品名称', field: 'productName', slotName: 'productNameCell' },
  { title: '预计数量', field: 'expectedQty', width: 100, slotName: 'expectedQtyCell' },
  { title: '实收数量', field: 'actualQty', width: 100, slotName: 'actualQtyCell' },
  { title: '操作', field: 'action', width: 60, slotName: 'actionCell' }
]

const filterOption = (input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())

function handleOrderChange(value: string | undefined) {
  if (value) {
    addForm.items = [
      { key: itemKeyCounter++, productId: 1, productName: 'PROD-001', isNew: false, expectedQty: 100, actualQty: 0 },
      { key: itemKeyCounter++, productId: 2, productName: 'PROD-002', isNew: false, expectedQty: 50, actualQty: 0 }
    ]
  }
}
function handleItemProductChange(index: number, productId: number) {
  const product = productOptions.find(p => p.value === productId)
  if (product) addForm.items[index].productName = product.label
}
function addInboundItem() {
  addForm.items.push({ key: itemKeyCounter++, productId: undefined, productName: '', isNew: true, expectedQty: 1, actualQty: 0 })
}
function removeInboundItem(index: number) { addForm.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await inboundApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    tableData.value = pageData?.records || []
    pagination.total = pageData?.totalElements ?? pageData?.total ?? 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
    emit('update-count', pagination.total)
  } catch (err) {
    console.warn('[库存入库] 获取入库单列表失败', err)
    tableData.value = []
    pagination.total = 0
    hasError.value = true
    emit('update-count', pagination.total)
  }
  finally { loading.value = false }
}


function handleView(record: any) {
  currentRecord.value = record
  detailData.value = null
  detailError.value = null
  detailVisible.value = true
  fetchDetail(record.id)
}
function handleAdd() {
  addForm.orderNo = undefined; addForm.inboundType = 1; addForm.warehouseId = undefined
  addForm.expectedDate = dayjs(); addForm.remark = ''; addForm.items = []; itemKeyCounter = 0; addVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除入库单', content: `确认删除入库单 "${record.inboundNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await inboundApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (err) { console.warn('[库存入库] 删除入库单失败', err); message.error('删除失败') }
    }
  })
}
async function handleBatchDelete(ids: number[]) {
  await executeBatch(ids, inboundApi.delete, '批量删除')
  fetchData()
}

const handleAddSubmit = async () => {
  try { await addFormRef.value?.validate() } catch (err) { console.warn('[库存入库] 表单验证失败', err); return }
  if (addForm.items.length === 0) { message.warning('请至少添加一条入库明细'); return }
  addSubmitting.value = true
  try {
    await inboundApi.create({
      orderNo: addForm.orderNo, inboundType: addForm.inboundType, warehouseId: addForm.warehouseId,
      expectedDate: addForm.expectedDate.format('YYYY-MM-DD'), remark: addForm.remark,
      items: addForm.items.map(item => ({ productId: item.productId, expectedQty: item.expectedQty, actualQty: item.actualQty }))
    })
    message.success('入库单创建成功'); addVisible.value = false; pagination.current = 1; fetchData()
  } catch (err: any) { console.warn('[库存入库] 创建入库单失败', err); message.error(err?.message || '创建失败') }
  finally { addSubmitting.value = false }
}
const handleAddCancel = () => { addVisible.value = false }

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批', content: `确定要审批通过入库单 "${record.inboundNo}" 吗？`,
    okText: '确认通过', cancelText: '取消', centered: true,
    async onOk() { try { await inboundApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err: any) { console.warn('[库存入库] 审批入库单失败', err); message.error(err?.message || '审批失败') } }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择入库单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() { await executeBatch(keys, inboundApi.approve, '批量审批'); fetchData() }
  })
}

async function handleExport() {
  try {
    const headers = ['入库单号', '入库类型', '仓库', '入库日期', '状态', '创建时间']
    const rows = tableData.value.map((row: any) => [
      row.inboundNo || '', row.inboundType || '', row.warehouseName || '', row.inboundDate || '',
      getStatusText(row.status), row.createTime || ''
    ])
    const csv = ['\ufeff' + headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `入库单_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err: any) {
    console.warn('[库存入库] 导出失败', err)
    message.error(err?.message || '导出失败')
  }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'delete': handleDelete(record); break
  }
}

function handleParentCreate() { handleAdd() }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('stock:create', handleParentCreate)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('stock:create', handleParentCreate)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.inbound-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.inbound-list-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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

.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-amount { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }

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


.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.detail-modal-footer { text-align: right; margin-top: 16px; }

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
