<template>
  <div class="purchase-exchange-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.pending }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-processing">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.processing }}</div>
          <div class="stat-card-label">换货中</div>
        </div>
        <SyncOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.completed }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
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
      :table-key="'purchase-exchange-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建换货"
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
      @selection-change="(keys: number[]) => { selectedRowKeys = keys }"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #batch-actions>
        <a-button size="small" type="primary" ghost @click="handleBatchApprove">
          <template #icon><CheckCircleOutlined /></template>
          批量审批
        </a-button>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的换货单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无换货单数据，点击右上角「新建换货」开始创建
          </p>
        </div>
      </template>

      <template #action="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else>
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="编辑">
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item v-if="record.status === 0" key="submit">
                    <SendOutlined /> 提交审核
                  </a-menu-item>
                  <a-menu-item v-if="record.status === 1" key="approve">
                    <CheckCircleOutlined /> 审批通过
                  </a-menu-item>
                  <a-menu-item v-if="record.status === 2" key="confirm">
                    <CheckOutlined /> 确认换货
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="print">
                    <PrinterOutlined /> 打印
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item v-if="record.status === 0" key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="换货单详情"
      width="800px"
      centered
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="换货单号">{{ currentRecord.exchangeNo }}</a-descriptions-item>
        <a-descriptions-item label="关联订单">
          <a @click="handleViewOrder(currentRecord)">{{ currentRecord.orderNo }}</a>
        </a-descriptions-item>
        <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="换货日期">{{ currentRecord.exchangeDate }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="换货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <!-- 换货明细表格 -->
      <div class="detail-items-section">
        <div class="detail-items-title">换货物料明细</div>
        <a-table
          :columns="detailItemColumns"
          :data-source="detailItems"
          :pagination="false"
          size="small"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="record.__empty_row">
              <span class="empty-placeholder">&nbsp;</span>
            </template>
          </template>
        </a-table>
      </div>

      <div class="detail-modal-footer">
        <a-button v-if="currentRecord?.status === 2" type="primary" @click="handleConfirmExchange">确认换货完成</a-button>
        <a-button @click="handlePrintDetail">打印</a-button>
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>

    <!-- 新建/编辑换货弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      :title="formMode === 'edit' ? '编辑换货单' : '新建换货单'"
      width="750px"
      centered
      :confirm-loading="formSubmitting"
      :maskClosable="false"
      ok-text="确认"
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
        <a-form-item label="换货原因" name="reason">
          <a-select v-model:value="formData.reason" placeholder="请选择换货原因">
            <a-select-option value="质量问题">质量问题</a-select-option>
            <a-select-option value="规格不符">规格不符</a-select-option>
            <a-select-option value="数量错误">数量错误</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="换货日期" name="exchangeDate">
          <a-date-picker v-model:value="formData.exchangeDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>

      <div class="form-items-section">
        <div class="form-items-header">
          <span class="form-items-title">换货物料明细</span>
          <a-button size="small" type="dashed" @click="handleAddExchangeItem">
            <template #icon><PlusOutlined /></template>
            添加物料
          </a-button>
        </div>
        <a-table
          :columns="exchangeItemColumns"
          :data-source="formTableItems"
          :pagination="false"
          size="small"
          row-key="tempKey"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="record.__empty_row">
              <span class="empty-placeholder">&nbsp;</span>
            </template>
            <template v-else-if="column.key === 'outProductName'">
              <a-input v-model:value="record.outProductName" placeholder="换出物料" size="small" />
            </template>
            <template v-else-if="column.key === 'inProductName'">
              <a-input v-model:value="record.inProductName" placeholder="换入物料" size="small" />
            </template>
            <template v-else-if="column.key === 'quantity'">
              <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="handleRemoveExchangeItem(index)">删除</a-button>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseExchangeTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined, EyeOutlined, EditOutlined, SendOutlined, CheckCircleOutlined,
  DeleteOutlined, SearchOutlined, InboxOutlined, EllipsisOutlined, PrinterOutlined,
  CheckOutlined, ClockCircleOutlined, SyncOutlined, SwapOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { purchaseExchangeApi } from '@/api/purchase-exchange'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'
import { executeBatch, validateSelection } from '@/utils/batchOperations'

const { execute: executeExport } = useExport()
const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const selectedRowKeys = ref<number[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const pending = dataSource.value.filter(r => r.status === 1).length
  const processing = dataSource.value.filter(r => r.status === 3).length
  const completed = dataSource.value.filter(r => r.status === 4).length
  return { pending, processing, completed }
})

const vxeColumns = computed(() => [
  { title: '换货单号', field: 'exchangeNo', width: 160 },
  { title: '关联订单', field: 'orderNo', width: 160 },
  { title: '供应商', field: 'supplierName', width: 140 },
  { title: '换货日期', field: 'exchangeDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }) => getStatusText(cellValue) },
  { title: '创建人', field: 'creatorName', width: 100 },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 130, fixed: 'right' }
])

const filterFields = [
  { key: 'exchangeNo', label: '换货单号', type: 'input' as const, placeholder: '输入换货单号' },
  { key: 'orderNo', label: '关联订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
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
  return [
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...dataSource.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const detailItems = ref<any[]>([])

const detailItemColumns = [
  { title: '换出物料编码', dataIndex: 'outProductCode', width: 120 },
  { title: '换出物料名称', dataIndex: 'outProductName', width: 150 },
  { title: '换入物料编码', dataIndex: 'inProductCode', width: 120 },
  { title: '换入物料名称', dataIndex: 'inProductName', width: 150 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 120 }
]

function handleView(record: any) {
  currentRecord.value = record
  detailItems.value = record.items || mockDetailItems()
  detailVisible.value = true
}

function handleViewOrder(record: any) {
  if (record.orderId) {
    router.push(`/purchase/order/${record.orderId}`)
  } else {
    message.info('订单详情功能开发中')
  }
}

function handlePrintDetail() {
  message.info(`打印换货单: ${currentRecord.value?.exchangeNo}`)
}

function handleConfirmExchange() {
  Modal.confirm({
    title: '确认换货完成',
    content: `确认换货单 "${currentRecord.value?.exchangeNo}" 已完成？`,
    okText: '确认完成',
    centered: true,
    onOk: async () => {
      try { await purchaseExchangeApi.confirm(currentRecord.value?.id); message.success('换货已完成'); detailVisible.value = false; fetchData() }
      catch { message.error('操作失败') }
    }
  })
}

function mockDetailItems(): any[] {
  return [
    { id: 1, outProductCode: 'M001', outProductName: '物料A（问题）', inProductCode: 'M001-1', inProductName: '物料A（新）', quantity: 10, remark: '' },
    { id: 2, outProductCode: 'M002', outProductName: '物料B（问题）', inProductCode: 'M002-1', inProductName: '物料B（新）', quantity: 5, remark: '' }
  ]
}

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

let itemCounter = 0
const genTempKey = () => `exchange_item_${++itemCounter}_${Date.now()}`

interface ExchangeItemForm {
  tempKey: string
  outProductName: string
  inProductName: string
  quantity: number
}

const formData = reactive({
  orderNo: '',
  reason: undefined as string | undefined,
  exchangeDate: undefined as any,
  remark: '',
  items: [] as ExchangeItemForm[]
})

const formRules = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择换货原因', trigger: 'change' }],
  exchangeDate: [{ required: true, message: '请选择换货日期', trigger: 'change' }]
}

const exchangeItemColumns = [
  { title: '换出物料', key: 'outProductName', width: 150 },
  { title: '换入物料', key: 'inProductName', width: 150 },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

// 表格空行填充 (MIN_TABLE_ROWS = 8)
const MIN_FORM_ROWS = 8
const formTableItems = computed(() => {
  const data = [...formData.items]
  const emptyCount = Math.max(0, MIN_FORM_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, tempKey: `__empty_form_${i}` })
  }
  return data
})

const handleAddExchangeItem = () => {
  formData.items.push({ tempKey: genTempKey(), outProductName: '', inProductName: '', quantity: 1 })
}
const handleRemoveExchangeItem = (index: number) => { formData.items.splice(index, 1) }

const resetForm = () => {
  formData.orderNo = ''; formData.reason = undefined; formData.exchangeDate = undefined
  formData.remark = ''; formData.items = []; editingId.value = null
}

async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseExchangeApi.page({ current: pagination.current, size: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || mockData()
    pagination.total = pageData.total || mockData().length
    lastUpdated.value = new Date().toISOString()
  } catch {
    message.error('获取换货单列表失败')
    dataSource.value = mockData()
  } finally { loading.value = false }
}

const mockData = (): any[] => [
  { id: 1, exchangeNo: 'EX2024010001', orderNo: 'PO2024010001', supplierName: '北京供应商', exchangeDate: '2024-01-15', status: 4, creatorName: '张三', createTime: '2024-01-15 10:00' },
  { id: 2, exchangeNo: 'EX2024010002', orderNo: 'PO2024010002', supplierName: '上海贸易公司', exchangeDate: '2024-01-18', status: 1, creatorName: '李四', createTime: '2024-01-18 11:00' },
  { id: 3, exchangeNo: 'EX2024010003', orderNo: 'PO2024010003', supplierName: '广州制造企业', exchangeDate: '2024-01-20', status: 0, creatorName: '王五', createTime: '2024-01-20 09:00' },
  { id: 4, exchangeNo: 'EX2024010004', orderNo: 'PO2024010004', supplierName: '深圳电子公司', exchangeDate: '2024-01-12', status: 3, creatorName: '张三', createTime: '2024-01-12 14:00' },
  { id: 5, exchangeNo: 'EX2024010005', orderNo: 'PO2024010005', supplierName: '杭州供应商', exchangeDate: '2024-01-22', status: 2, creatorName: '李四', createTime: '2024-01-22 15:00' }
]

function handleAdd() { formMode.value = 'add'; resetForm(); formModalVisible.value = true }

function handleEdit(record: any) {
  formMode.value = 'edit'; editingId.value = record.id
  formData.orderNo = record.orderNo || ''; formData.reason = record.reason || undefined
  formData.exchangeDate = record.exchangeDate; formData.remark = record.remark || ''
  formData.items = (record.items || []).map((item: any) => ({
    tempKey: genTempKey(), outProductName: item.outProductName || '', inProductName: item.inProductName || '', quantity: item.quantity || 1
  }))
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除换货单', content: `确认删除换货单 "${record.exchangeNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await purchaseExchangeApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'confirm': handleConfirm(record); break
    case 'print': message.info(`打印换货单: ${record.exchangeNo}`); break
    case 'delete': handleDelete(record); break
  }
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => purchaseExchangeApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  if (formData.items.length === 0) { message.warning('请添加换货物料明细'); return }
  formSubmitting.value = true
  try {
    const data = {
      orderNo: formData.orderNo, reason: formData.reason, exchangeDate: formData.exchangeDate,
      remark: formData.remark,
      items: formData.items.map(item => ({ outProductName: item.outProductName, inProductName: item.inProductName, quantity: item.quantity }))
    }
    if (formMode.value === 'edit' && editingId.value) {
      await purchaseExchangeApi.update(editingId.value, data as any)
      message.success('编辑成功')
    } else {
      await purchaseExchangeApi.create(data as any)
      message.success('新建换货单成功')
    }
    formModalVisible.value = false; fetchData()
  } catch { message.error(formMode.value === 'edit' ? '编辑失败' : '新建失败') }
  finally { formSubmitting.value = false }
}

function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交换货单', content: `提交换货单 "${record.exchangeNo}" 进行审核？`, okText: '确认提交', centered: true,
    async onOk() { try { await purchaseExchangeApi.submit(record.id); message.success('提交成功'); fetchData() } catch { message.error('提交失败') } }
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批换货单', content: `审批通过换货单 "${record.exchangeNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await purchaseExchangeApi.approve(record.id, { approved: true }); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleConfirm(record: any) {
  Modal.confirm({
    title: '确认换货', content: `确认换货单 "${record.exchangeNo}" 换货完成？`, okText: '确认', centered: true,
    async onOk() { try { await purchaseExchangeApi.confirm(record.id); message.success('换货已完成'); fetchData() } catch { message.error('操作失败') } }
  })
}

function handleBatchApprove() {
  const keys = selectedRowKeys.value
  if (!validateSelection(keys, '审批')) return
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => purchaseExchangeApi.approve(id, { approved: true }), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '换货单',
    headers: ['换货单号', '关联订单', '供应商', '换货日期', '状态', '创建人', '创建时间'],
    fetchAll: () => purchaseExchangeApi.page({ current: 1, size: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.supplierName || '', row.exchangeDate || '',
      getStatusText(row.status), row.creatorName || '', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.supplierName || '', row.exchangeDate || '',
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

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('purchase:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('purchase:refresh', fetchData)
})
</script>

<style scoped>
.purchase-exchange-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
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

.stat-card-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-card-processing { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-card-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-card-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 32px;
  color: #999;
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

.empty-placeholder {
  color: transparent;
}

.exchange-no, .order-link {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

/* 详情弹窗 */
.detail-items-section {
  margin-top: 16px;
}

.detail-items-title {
  font-weight: 500;
  margin-bottom: 8px;
}

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 表单物料区 */
.form-items-section {
  margin-top: 16px;
}

.form-items-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.form-items-title {
  font-weight: 500;
}

.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
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
</style>