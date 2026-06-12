<template>
  <div class="purchase-return-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.pending }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.completed }}</div>
          <div class="stat-card-label">已退货</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalReturnAmount) }}</div>
          <div class="stat-card-label">退货金额合计</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">退货单总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'purchase-return-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建退货"
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
      @cell-dblclick="handleView"
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
              没有符合条件的退货单，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无退货单数据，点击右上角「新建退货」开始创建
            </p>
          </template>
        </div>
      </template>

      <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="审批">
              <a-button type="link" size="small" @click="handleApprove(record)">
                <template #icon><CheckCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              template-type="return"
              :business-id="record.id"
              business-type="purchase_return"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
                  <a-menu-item v-if="record.status === 1" key="approve">
                    <CheckCircleOutlined /> 审批通过
                  </a-menu-item>
                  <a-menu-item v-if="record.status === 1" key="reject">
                    <CloseCircleOutlined /> 审批拒绝
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
    </VxeTableList>

    <!-- 详情全屏覆盖层 -->
    <a-drawer
      v-model:open="detailVisible"
      title="退货单详情"
      placement="right"
      width="80vw"
      class="return-detail-drawer"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
            <template #icon><ReloadOutlined /></template>
          </a-button>
          <PrintButton
            template-type="return"
            :business-id="currentRecord?.id"
            business-type="purchase_return"
            button-text="打印"
            button-size="small"
          />
        </a-space>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 10 }">
        <template v-if="detailData">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="退货单号">{{ detailData.returnNo }}</a-descriptions-item>
            <a-descriptions-item label="采购订单">
              <a @click="handleViewOrder(currentRecord)">{{ detailData.orderNo }}</a>
            </a-descriptions-item>
            <a-descriptions-item label="供应商">{{ detailData.supplierName }}</a-descriptions-item>
            <a-descriptions-item label="退货日期">{{ detailData.returnDate }}</a-descriptions-item>
            <a-descriptions-item label="退货金额">
              <span class="amount-cell">¥{{ formatAmount(detailData.totalAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="退款方式">{{ getRefundTypeText(detailData.refundType) }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="退货原因" :span="2">{{ detailData.reason || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 退货明细表格 -->
          <div class="detail-items-section">
            <div class="detail-items-title">退货物料明细</div>
            <VxeTableList
              :columns="detailItemColumns"
              :data-source="detailItems"
              :pagination="false as any"
              row-key="id"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #unitPriceCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.unitPrice) }}</span>
              </template>
              <template #amountCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
              </template>
            </VxeTableList>
          </div>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.id)">重试</a-button>
          </template>
        </a-result>
      </a-skeleton>
    </a-drawer>

    <!-- 新建退货弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      title="新建退货单"
      width="750px"
      centered
      :confirm-loading="formSubmitting"
      :maskClosable="false"
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
        <a-form-item label="退货原因" name="reason">
          <a-select v-model:value="formData.reason" size="small" placeholder="请选择退货原因">
            <a-select-option value="质量问题">质量问题</a-select-option>
            <a-select-option value="规格不符">规格不符</a-select-option>
            <a-select-option value="数量错误">数量错误</a-select-option>
            <a-select-option value="延迟交货">延迟交货</a-select-option>
            <a-select-option value="其他">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="退款方式" name="refundType">
          <a-radio-group v-model:value="formData.refundType">
            <a-radio :value="1">原路退回</a-radio>
            <a-radio :value="2">抵扣货款</a-radio>
            <a-radio :value="3">线下退款</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="退货日期" name="returnDate">
          <a-date-picker v-model:value="formData.returnDate" size="small" style="width: 100%" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
        </a-form-item>
      </a-form>

      <div class="form-items-section">
        <div class="form-items-header">
          <span class="form-items-title">退货物料明细</span>
          <a-button size="small" type="dashed" @click="handleAddReturnItem">
            <template #icon><PlusOutlined /></template>
            添加物料
          </a-button>
        </div>
        <VxeTableList
          :columns="returnItemColumns"
          :data-source="formData.items"
          :pagination="false as any"
          row-key="tempKey"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #productNameCell="{ record }">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template #quantityCell="{ record }">
            <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
          </template>
          <template #unitPriceCell="{ record }">
            <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
          <template #amountCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.quantity * record.unitPrice) }}</span>
          </template>
          <template #actionCell="{ record, index }">
            <a-button type="link" danger size="small" @click="handleRemoveReturnItem(index)">删除</a-button>
          </template>
        </VxeTableList>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseReturnTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined, EyeOutlined, CheckCircleOutlined, CloseCircleOutlined,
  SearchOutlined, InboxOutlined, EllipsisOutlined,
  DeleteOutlined, ClockCircleOutlined, DollarOutlined, FileTextOutlined,
  WarningOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { purchaseReturnApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'
import { executeBatch, validateSelection } from '@/utils/batchOperations'

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
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
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
  const completed = dataSource.value.filter(r => r.status === 2).length
  return { pending, completed }
})

const totalReturnAmount = computed(() => {
  return dataSource.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
})

const vxeColumns = computed(() => [
  { title: '退货单号', field: 'returnNo', width: 160 },
  { title: '采购订单', field: 'orderNo', width: 160 },
  { title: '供应商', field: 'supplierName', width: 140 },
  { title: '退货日期', field: 'returnDate', width: 110 },
  { title: '退货金额', field: 'totalAmount', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>` },
  { title: '退款方式', field: 'refundType', width: 100, formatter: ({ cellValue }) => getRefundTypeText(cellValue) },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 140, fixed: 'right' }
])

const filterFields = [
  { key: 'returnNo', label: '退货单号', type: 'input' as const, placeholder: '输入退货单号' },
  { key: 'orderNo', label: '采购订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已退货', value: 2 }, { label: '已拒绝', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已退货', 3: '已拒绝' }
const refundTypeTextMap: Record<number, string> = { 1: '原路退回', 2: '抵扣货款', 3: '线下退款' }

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalAmount = dataSource.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  return [
    { label: '本页金额合计', value: totalAmount, type: 'currency' as const },
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }
function getRefundTypeText(type: number): string { return refundTypeTextMap[type] || '未知' }
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}


// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const detailRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const detailItems = ref<any[]>([])

const detailItemColumns = [
  { title: '物料编码', field: 'productCode', width: 120 },
  { title: '物料名称', field: 'productName', width: 150 },
  { title: '规格型号', field: 'spec', width: 100 },
  { title: '退货数量', field: 'quantity', width: 80, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 100, align: 'right', slotName: 'unitPriceCell' },
  { title: '金额', field: 'amount', width: 100, align: 'right', slotName: 'amountCell' },
  { title: '备注', field: 'remark', width: 120 }
]

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    const res = await purchaseReturnApi.getById(id) as any
    const data = (res as any).data ?? res
    detailData.value = data
    detailItems.value = data.items || []
  } catch (err: any) {
    console.warn('[采购退货] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

function handleView(record: any) {
  currentRecord.value = record
  detailRecord.value = record
  detailVisible.value = true
  fetchDetail(record.id)
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = null
  detailError.value = null
  detailItems.value = []
}

function handleDetailRefresh() {
  if (detailRecord.value?.id) fetchDetail(detailRecord.value.id)
}

function handleViewOrder(record: any) {
  if (record.orderId) {
    router.push(`/purchase/order/${record.orderId}`)
  } else {
    message.info('订单详情功能开发中')
  }
}

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

let itemCounter = 0
const genTempKey = () => `return_item_${++itemCounter}_${Date.now()}`

interface ReturnItemForm {
  tempKey: string
  productName: string
  quantity: number
  unitPrice: number
}

const formData = reactive({
  orderNo: '',
  reason: undefined as string | undefined,
  refundType: 1,
  returnDate: undefined as any,
  remark: '',
  items: [] as ReturnItemForm[]
})

const formRules: any = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
}

const returnItemColumns = [
  { title: '物料名称', field: 'productName', width: 150, slotName: 'productNameCell' },
  { title: '退货数量', field: 'quantity', width: 100, slotName: 'quantityCell' },
  { title: '单价', field: 'unitPrice', width: 100, slotName: 'unitPriceCell' },
  { title: '金额', field: 'amount', width: 100, slotName: 'amountCell' },
  { title: '操作', field: 'action', width: 80, slotName: 'actionCell' }
]

const handleAddReturnItem = () => {
  formData.items.push({ tempKey: genTempKey(), productName: '', quantity: 1, unitPrice: 0 })
}
const handleRemoveReturnItem = (index: number) => { formData.items.splice(index, 1) }

async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseReturnApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
  } catch (e) {
    console.warn('[采购退货] 获取列表失败', e)
    message.error('获取退货单列表失败')
    dataSource.value = []
    hasError.value = true
  } finally { loading.value = false }
}

function handleAdd() {
  formData.orderNo = ''; formData.reason = undefined; formData.refundType = 1
  formData.returnDate = undefined; formData.remark = ''; formData.items = []
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除退货单', content: `确认删除退货单 "${record.returnNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await purchaseReturnApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (e) { console.warn('[采购退货] 删除失败', e); message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'approve': handleApprove(record); break
    case 'reject': handleReject(record); break
    case 'delete': handleDelete(record); break
  }
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => purchaseReturnApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  if (formData.items.length === 0) { message.warning('请添加退货物料明细'); return }
  formSubmitting.value = true
  try {
    await purchaseReturnApi.create({
      orderNo: formData.orderNo, reason: formData.reason, refundType: formData.refundType,
      returnDate: formData.returnDate, remark: formData.remark,
      items: formData.items.map(item => ({ productName: item.productName, quantity: item.quantity, unitPrice: item.unitPrice }))
    })
    message.success('新建退货单成功'); formModalVisible.value = false; fetchData()
  } catch (e) { console.warn('[采购退货] 新建失败', e); message.error('新建退货单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批退货单', content: `审批通过退货单 "${record.returnNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await purchaseReturnApi.approve(record.id); message.success('审批成功'); fetchData() } catch (e) { console.warn('[采购退货] 审批失败', e); message.error('审批失败') } }
  })
}

function handleReject(record: any) {
  Modal.confirm({
    title: '拒绝退货单', content: `拒绝退货单 "${record.returnNo}" ？`, okText: '确认拒绝', okType: 'danger', centered: true,
    async onOk() { try { await purchaseReturnApi.reject(record.id); message.success('已拒绝'); fetchData() } catch (e) { console.warn('[采购退货] 拒绝失败', e); message.error('操作失败') } }
  })
}

function handleBatchApprove() {
  const keys = selectedRowKeys.value
  if (!validateSelection(keys, '审批')) return
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      const result = await executeBatch(keys, (id) => purchaseReturnApi.approve(id), '批量审批')
      if (result.successCount > 0) fetchData()
    }
  })
}

function handleExport() {
  executeExport({
    fileName: '退货单',
    headers: ['退货单号', '采购订单', '供应商', '退货日期', '退货金额', '退款方式', '状态', '创建时间'],
    fetchAll: () => purchaseReturnApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.returnNo || '', row.orderNo || '', row.supplierName || '', row.returnDate || '',
      (row.totalAmount || 0).toFixed(2), getRefundTypeText(row.refundType), getStatusText(row.status), row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.returnNo || '', row.orderNo || '', row.supplierName || '', row.returnDate || '',
      (row.totalAmount || 0).toFixed(2), getRefundTypeText(row.refundType), getStatusText(row.status), row.createTime || ''
    ]),
    total: pagination.total,
  })
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }

const debouncedFetch = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleParentCreate() { handleAdd() }

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd) }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('purchase:create', handleParentCreate)
  window.addEventListener('purchase:refresh', fetchData)
  refreshTimer = setInterval(() => fetchData(), 30000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('purchase:create', handleParentCreate)
  window.removeEventListener('purchase:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  clearTimeout(debouncedFetch.value)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.purchase-return-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.purchase-return-tab > :deep(.vxe-table-list-container) {
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
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
}

.stat-card-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-card-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-card-amount { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
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


.return-no, .order-link {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
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

/* 详情抽屉 */
:deep(.return-detail-drawer .ant-drawer-body) {
  padding: 16px 24px;
  overflow-y: auto;
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
