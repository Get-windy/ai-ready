<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/mall">商城管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>订单管理</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <a-space :size="8">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
            </span>
            <a-tooltip title="F5: 刷新 | Ctrl+E: 导出">
              <a-button size="small" @click="debounceClick('refresh', fetchData)">
                <template #icon><ReloadOutlined /></template>刷新
              </a-button>
            </a-tooltip>
            <a-tooltip title="导出 (Ctrl+E)">
              <a-button size="small" @click="debounceClick('export', handleExport)">
                <template #icon><ExportOutlined /></template>导出
              </a-button>
            </a-tooltip>
            <PrintButton :record="{ orderNo: '' }" business-type="mall_order" button-size="small" tooltip="打印" />
          </a-space>
        </div>
      </div>
    </template>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      @refresh="debounceClick('refresh', fetchData)"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #empty>
        <a-empty v-if="!hasError" description="暂无订单数据" />
        <a-result v-else status="error" title="数据加载失败">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>重新加载
            </a-button>
          </template>
        </a-result>
      </template>

      <template #orderStatusCell="{ record }">
        <a-tag v-if="record.orderStatus === 'PENDING_PAYMENT'" color="orange">待付款</a-tag>
        <a-tag v-else-if="record.orderStatus === 'PAID'" color="blue">已付款</a-tag>
        <a-tag v-else-if="record.orderStatus === 'APPROVED'" color="cyan">已审核</a-tag>
        <a-tag v-else-if="record.orderStatus === 'SHIPPED'" color="purple">已发货</a-tag>
        <a-tag v-else-if="record.orderStatus === 'COMPLETED'" color="green">已完成</a-tag>
        <a-tag v-else-if="record.orderStatus === 'CANCELLED'" color="red">已取消</a-tag>
        <a-tag v-else-if="record.orderStatus === 'REJECTED'" color="red">已驳回</a-tag>
        <span v-else>{{ record.orderStatus }}</span>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button type="link" size="small" @click="handleViewDetail(record)">详情</a-button>
          <template v-if="record.orderStatus === 'PAID'">
            <a-button v-permission="'erp:mall:order:approve'" type="link" size="small" style="color: #52c41a" @click="handleApprove(record)">通过</a-button>
            <a-button v-permission="'erp:mall:order:reject'" type="link" size="small" danger @click="handleReject(record)">驳回</a-button>
          </template>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 订单详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="'订单详情 - ' + (detailData?.orderNo || '')"
      :footer="null"
      width="720px"
    >
      <a-descriptions v-if="detailData" bordered :column="2" size="small">
        <a-descriptions-item label="订单编号" :span="2">{{ detailData.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ detailData.customerName }}</a-descriptions-item>
        <a-descriptions-item label="订单金额">¥{{ detailData.totalAmount }}</a-descriptions-item>
        <a-descriptions-item label="订单状态">{{ statusLabel(detailData.orderStatus) }}</a-descriptions-item>
        <a-descriptions-item label="支付状态">{{ detailData.paymentStatus }}</a-descriptions-item>
        <a-descriptions-item label="收货人">{{ detailData.consignee }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ detailData.phone }}</a-descriptions-item>
        <a-descriptions-item label="收货地址" :span="2">{{ detailData.address }}</a-descriptions-item>
        <a-descriptions-item label="下单时间" :span="2">{{ detailData.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>商品明细</a-divider>
      <a-table
        :data-source="detailData?.orderItems || []"
        :columns="detailColumns"
        :pagination="false as any"
        size="small"
        row-key="id"
      />
    </a-modal>

    <!-- 驳回原因弹窗 -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="驳回原因"
      :confirm-loading="rejectLoading"
      @ok="handleRejectConfirm"
    >
      <a-textarea
        v-model:value="rejectReason"
        placeholder="请输入驳回原因"
        :rows="3"
        :maxlength="500"
      />
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallOrderList' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, ExportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import { mallOrderApi, type MallOrder } from '@/api/erp/mall'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<MallOrder[]>([])
const tableDataSource = tableData

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const searchForm = reactive({
  keyword: undefined as string | undefined,
  orderStatus: undefined as string | undefined
})

const detailVisible = ref(false)
const detailData = ref<MallOrder | null>(null)

const rejectModalVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const currentRejectRecord = ref<MallOrder | null>(null)

const orderStatusOptions = [
  { label: '全部', value: undefined },
  { label: '待付款', value: 'PENDING_PAYMENT' },
  { label: '已付款', value: 'PAID' },
  { label: '已审核', value: 'APPROVED' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已驳回', value: 'REJECTED' }
]

function statusLabel(status: string | undefined): string {
  return orderStatusOptions.find(o => o.value === status)?.label || status || '-'
}

const vxeColumns: any = computed(() => [
  { type: 'checkbox', width: 40 },
  { field: 'orderNo', title: '订单编号', width: 200 },
  { field: 'customerName', title: '客户名称', width: 140 },
  { field: 'totalAmount', title: '订单金额', width: 110 },
  { field: 'payAmount', title: '应付金额', width: 110 },
  { field: 'orderStatus', title: '订单状态', width: 100, slotName: 'orderStatusCell' },
  { field: 'consignee', title: '收货人', width: 80 },
  { field: 'createTime', title: '下单时间', width: 160 },
  { type: 'action', title: '操作', width: 180, fixed: 'right' }
])

const detailColumns: any = [
  { title: '商品编码', dataIndex: 'productId', width: 100 },
  { title: '商品名称', dataIndex: 'productName', width: 200 },
  { title: '单价', dataIndex: 'price', width: 80 },
  { title: '数量', dataIndex: 'quantity', width: 60 },
  { title: '小计', dataIndex: 'subtotal', width: 80 }
]

const filterFields = computed<FilterField[]>(() => [
  { key: 'keyword', label: '关键词', type: 'input', placeholder: '订单号/客户名称' },
  { key: 'orderStatus', label: '订单状态', type: 'select', options: orderStatusOptions }
])

// ── 防抖 ──
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await mallOrderApi.page({
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res) {
      tableData.value = res.records || []
      pagination.total = res.total || 0
    }
  } catch (err) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[订单管理] 加载订单列表失败', err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { keyword: undefined, orderStatus: undefined })
  } else {
    searchForm.keyword = filters.keyword
    searchForm.orderStatus = filters.orderStatus
  }
  pagination.current = 1
  fetchData()
}

const handleViewDetail = async (record: MallOrder) => {
  try {
    const res = await mallOrderApi.getDetail(record.id!)
    detailData.value = res.data
    detailVisible.value = true
  } catch (err) {
    console.warn('[订单管理] 加载订单详情失败', err)
  }
}

const handleApprove = async (record: MallOrder) => {
  try {
    await mallOrderApi.approve(record.id!)
    message.success('订单已审核通过')
    fetchData()
  } catch (err) {
    console.warn('[订单管理] 审核通过失败', err)
  }
}

const handleReject = (record: MallOrder) => {
  currentRejectRecord.value = record
  rejectReason.value = ''
  rejectModalVisible.value = true
}

const handleRejectConfirm = async () => {
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  if (!currentRejectRecord.value) return
  rejectLoading.value = true
  try {
    await mallOrderApi.reject(currentRejectRecord.value.id!, rejectReason.value)
    message.success('订单已驳回')
    rejectModalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[订单管理] 驳回失败', err)
  } finally {
    rejectLoading.value = false
  }
}

// ── 导出 ──
async function handleExport() {
  try {
    const blob = await request.get('/erp/mall/order/export', {
      params: {
        keyword: searchForm.keyword,
        orderStatus: searchForm.orderStatus
      },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商城订单_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败')
  }
}

// ── 键盘快捷键 & 自动刷新 ──
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  // 输入框中不触发快捷键
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (e.key === 'F5') {
    e.preventDefault(); debounceClick('refresh', fetchData)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault(); debounceClick('export', handleExport)
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) { console.warn('[MallOrderList]', err) }

defineExpose({ fetchData })
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
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
