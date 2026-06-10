<template>
  <div class="purchase-payment-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.pending }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-paid">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalPaidAmount) }}</div>
          <div class="stat-card-label">已付款金额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-unpaid">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalPendingAmount) }}</div>
          <div class="stat-card-label">待付款金额</div>
        </div>
        <ExclamationCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">付款单总数</div>
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
      :table-key="'purchase-payment-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
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
        <a-button size="small" @click="handleBatchPrint">
          <template #icon><PrinterOutlined /></template>
          批量打印
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
              没有符合条件的付款单，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无付款单数据，点击右上角「新建付款」开始创建
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
              template-type="payment"
              :business-id="record.id"
              business-type="purchase_payment"
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
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item v-if="record.status === 1" key="approve">
                    <CheckCircleOutlined /> 审批通过
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
      title="付款单详情"
      placement="right"
      width="80vw"
      class="payment-detail-drawer"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
            <template #icon><ReloadOutlined /></template>
          </a-button>
          <a-button v-if="currentRecord?.status === 1" type="primary" size="small" @click="handleApprove(currentRecord)">审批通过</a-button>
          <PrintButton
            template-type="payment"
            :business-id="currentRecord?.id"
            business-type="purchase_payment"
            button-text="打印"
            button-size="small"
          />
        </a-space>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 10 }">
        <template v-if="detailData">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="付款单号">{{ detailData.paymentNo }}</a-descriptions-item>
            <a-descriptions-item label="采购订单">
              <a @click="handleViewOrder(currentRecord)">{{ detailData.orderNo }}</a>
            </a-descriptions-item>
            <a-descriptions-item label="供应商">{{ detailData.supplierName }}</a-descriptions-item>
            <a-descriptions-item label="付款日期">{{ detailData.paymentDate }}</a-descriptions-item>
            <a-descriptions-item label="付款金额">
              <span class="amount-cell">¥{{ formatAmount(detailData.paymentAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="付款方式">{{ getPaymentMethodText(detailData.paymentMethod) }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="收款账户">{{ detailData.bankAccount || '-' }}</a-descriptions-item>
            <a-descriptions-item label="经办人">{{ detailData.operatorName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.id)">重试</a-button>
          </template>
        </a-result>
      </a-skeleton>
    </a-drawer>

    <!-- 新建付款弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      title="新建付款单"
      width="700px"
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
        <a-form-item label="付款金额" name="paymentAmount">
          <a-input-number v-model:value="formData.paymentAmount" :min="0" :precision="2" placeholder="请输入付款金额" style="width: 100%">
            <template #addonBefore>¥</template>
          </a-input-number>
        </a-form-item>
        <a-form-item label="付款方式" name="paymentMethod">
          <a-select v-model:value="formData.paymentMethod" size="small" placeholder="请选择付款方式">
            <a-select-option :value="1">银行转账</a-select-option>
            <a-select-option :value="2">现金</a-select-option>
            <a-select-option :value="3">承兑汇票</a-select-option>
            <a-select-option :value="4">微信/支付宝</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="收款账户" name="bankAccount">
          <a-input v-model:value="formData.bankAccount" placeholder="请输入收款账户（可选）" />
        </a-form-item>
        <a-form-item label="付款日期" name="paymentDate">
          <a-date-picker v-model:value="formData.paymentDate" size="small" style="width: 100%" />
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
      <div v-if="printItems.length === 0" class="print-empty">
        暂无选中付款单
      </div>
      <template v-else>
        <div class="print-header">
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
        <VxeTableList
          :columns="printTableColumns"
          :data-source="printItems"
          :pagination="false"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #checkedCell="{ record }">
            <a-checkbox v-model:checked="record.checked" />
          </template>
          <template #paymentAmountCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.paymentAmount) }}</span>
          </template>
        </VxeTableList>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchasePaymentTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  EyeOutlined, DeleteOutlined, CheckCircleOutlined, PrinterOutlined,
  SearchOutlined, InboxOutlined, EllipsisOutlined, ClockCircleOutlined,
  DollarOutlined, ExclamationCircleOutlined, FileTextOutlined,
  WarningOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { paymentApi } from '@/api/erp'
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
  return { pending }
})

const totalPaidAmount = computed(() => {
  return dataSource.value.filter(r => r.status === 2).reduce((s, r) => s + (r.paymentAmount || 0), 0)
})

const totalPendingAmount = computed(() => {
  return dataSource.value.filter(r => r.status === 1).reduce((s, r) => s + (r.paymentAmount || 0), 0)
})

const vxeColumns = computed(() => [
  { title: '付款单号', field: 'paymentNo', width: 160 },
  { title: '采购订单', field: 'orderNo', width: 160 },
  { title: '供应商', field: 'supplierName', width: 140 },
  { title: '付款日期', field: 'paymentDate', width: 110 },
  { title: '付款金额', field: 'paymentAmount', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { title: '付款方式', field: 'paymentMethod', width: 100, formatter: ({ cellValue }) => getPaymentMethodText(cellValue) },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getStatusColor(cellValue)}">${getStatusText(cellValue)}</span>` },
  { title: '经办人', field: 'operatorName', width: 100 },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 140, fixed: 'right' }
])

const filterFields = [
  { key: 'paymentNo', label: '付款单号', type: 'input' as const, placeholder: '输入付款单号' },
  { key: 'orderNo', label: '采购订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已付款', value: 2 }, { label: '部分付款', value: 3 }
  ]},
  { key: 'paymentMethod', label: '付款方式', type: 'select' as const, options: [
    { label: '银行转账', value: 1 }, { label: '现金', value: 2 }, { label: '承兑汇票', value: 3 }, { label: '微信/支付宝', value: 4 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已付款', 3: '部分付款' }
const paymentMethodTextMap: Record<number, string> = { 1: '银行转账', 2: '现金', 3: '承兑汇票', 4: '微信/支付宝' }

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
function getPaymentMethodText(method: number): string { return paymentMethodTextMap[method] || '未知' }
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

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    const res = await paymentApi.getById(id) as any
    const data = (res as any).data ?? res
    detailData.value = data
  } catch (err: any) {
    console.warn('[采购付款] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
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

const formData = reactive({
  orderNo: '', paymentAmount: undefined as number | undefined,
  paymentMethod: 1, bankAccount: '', paymentDate: undefined as any, remark: ''
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
  { title: '选择', field: 'checked', width: 60, slotName: 'checkedCell' },
  { title: '付款单号', field: 'paymentNo', width: 150 },
  { title: '供应商', field: 'supplierName' },
  { title: '付款金额', field: 'paymentAmount', width: 100, slotName: 'paymentAmountCell' },
  { title: '付款日期', field: 'paymentDate', width: 120 }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await paymentApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
  } catch (e) {
    console.warn('[采购付款] 获取列表失败', e)
    message.error('获取付款单列表失败')
    dataSource.value = []
    hasError.value = true
  } finally { loading.value = false }
}

function handleAdd() {
  formData.orderNo = ''; formData.paymentAmount = undefined; formData.paymentMethod = 1
  formData.bankAccount = ''; formData.paymentDate = undefined; formData.remark = ''
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除付款单', content: `确认删除付款单 "${record.paymentNo}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await paymentApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (e) { console.warn('[采购付款] 删除失败', e); message.error('删除失败') }
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
    case 'delete': handleDelete(record); break
  }
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
      paymentMethod: formData.paymentMethod, bankAccount: formData.bankAccount,
      paymentDate: formData.paymentDate, remark: formData.remark
    })
    message.success('新建付款单成功'); formModalVisible.value = false; fetchData()
  } catch (e) { console.warn('[采购付款] 新建失败', e); message.error('新建付款单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批付款单', content: `审批通过付款单 "${record.paymentNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await paymentApi.approve(record.id); message.success('审批成功'); fetchData() } catch (e) { console.warn('[采购付款] 审批失败', e); message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = selectedRowKeys.value
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
  executeExport({
    fileName: '付款单',
    headers: ['付款单号', '采购订单', '供应商', '付款日期', '付款金额', '付款方式', '状态', '经办人', '创建时间'],
    fetchAll: () => paymentApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.paymentNo || '', row.orderNo || '', row.supplierName || '', row.paymentDate || '',
      (row.paymentAmount || 0).toFixed(2), getPaymentMethodText(row.paymentMethod),
      getStatusText(row.status), row.operatorName || '', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.paymentNo || '', row.orderNo || '', row.supplierName || '', row.paymentDate || '',
      (row.paymentAmount || 0).toFixed(2), getPaymentMethodText(row.paymentMethod),
      getStatusText(row.status), row.operatorName || '', row.createTime || ''
    ]),
    total: pagination.total,
  })
}

const handlePrintCheckAll = (e: any) => {
  const checked = e.target.checked
  printItems.value.forEach((item: any) => { item.checked = checked })
}

const handlePrintAll = () => {
  const toPrint = printItems.value.filter((item: any) => item.checked)
  if (toPrint.length === 0) { message.warning('请选择要打印的付款单'); return }
  console.warn('[采购付款] 发送打印任务', toPrint);
  message.success(`正在发送 ${toPrint.length} 个付款单的打印任务...`)
  batchPrintModalVisible.value = false
}

const handleBatchPrint = () => {
  const keys = selectedRowKeys.value
  if (keys.length === 0) { message.warning('请选择付款单'); return }
  const selected = dataSource.value.filter((item: any) => keys.includes(item.id))
  printItems.value = selected.map((item: any) => ({ ...item, checked: true }))
  batchPrintModalVisible.value = true
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
.purchase-payment-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.purchase-payment-tab > :deep(.vxe-table-list-container) {
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

.stat-card-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-card-paid { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-card-unpaid { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
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


.payment-no, .order-link {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Meno, 'Courier New', monospace;
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

/* 详情抽屉 */
:deep(.payment-detail-drawer .ant-drawer-body) {
  padding: 16px 24px;
  overflow-y: auto;
}

/* 批量打印 */
.print-empty {
  text-align: center;
  padding: 40px;
  color: #999;
}

.print-header {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
</style>
