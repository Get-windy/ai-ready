<template>
  <PageContainer full-height>
    <template #header>
      <div class="return-page-header">
        <div class="return-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>退货管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="return-page-title">退货管理</h2>
        </div>
        <div class="return-page-header-right">
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="loading" @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">退货单总数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审核</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已退款</div>
            <div class="summary-value">{{ statistics.refundedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">退货金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      :show-export="true"
      add-text="新建退货申请"
      style="flex: 1;"
      @add="handleCreate"
      @refresh="fetchData"
      @export="handleExport"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的退货单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无退货单数据，点击右上角「新建退货申请」开始创建
          </p>
        </div>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 0" title="审核">
            <a-button type="link" size="small" @click="handleApprove(record)">
              <template #icon><CheckCircleOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 1" title="入库">
            <a-button type="link" size="small" @click="handleReceive(record)">
              <template #icon><DownloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 2" title="退款">
            <a-button type="link" size="small" @click="handleRefund(record)">
              <template #icon><RollbackOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                <a-menu-item key="delete">
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <a-drawer
      v-model:open="detailVisible"
      title="退货单详情"
      placement="right"
      width="80vw"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
        <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ currentRecord.customerName }}</a-descriptions-item>
        <a-descriptions-item label="退货金额">¥{{ currentRecord.returnAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.returnReason }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :status="currentRecord.status" :map="RETURN_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { PageContainer } from '@/components'
import { RETURN_STATUS } from '@/utils/statusConfig'
import { message, Modal } from 'ant-design-vue'
import { saleReturnApi } from '@/api/erp'
import request from '@/utils/request'
import {
  EyeOutlined,
  CheckCircleOutlined,
  DownloadOutlined,
  RollbackOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  SearchOutlined,
  InboxOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  SyncOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'

interface ReturnOrder {
  id: number
  returnNo: string
  orderNo: string
  customerName: string
  returnAmount: number
  returnReason: string
  status: number
  returnDate: string
  operator: string
  remark?: string
}

const loading = ref(false)
const autoRefreshCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const dataSource = ref<ReturnOrder[]>([])
const detailVisible = ref(false)
const currentRecord = ref<ReturnOrder | null>(null)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())
const selectedRows = ref<ReturnOrder[]>([])
const selectedIds = ref<number[]>([])

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  refundedCount: 0,
  totalAmount: 0
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = dataSource

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const vxeColumns = computed(() => [
  { field: 'returnNo', title: '退货单号', width: 150 },
  { field: 'orderNo', title: '销售订单', width: 150 },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'returnAmount', title: '退货金额', width: 120, align: 'right', formatter: ({ cellValue }) => `¥${cellValue?.toFixed(2) || '0.00'}` },
  { field: 'returnReason', title: '退货原因', minWidth: 100, showOverflow: 'tooltip' },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => RETURN_STATUS[cellValue]?.text || '' },
  { field: 'returnDate', title: '退货日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' },
])

const filterFields = [
  { key: 'returnNo', label: '退货单号', type: 'input' as const, placeholder: '请输入退货单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已入库', value: 2 },
    { label: '已退款', value: 3 },
  ]},
]

const handleRefresh = () => {
  autoRefreshCountdown.value = 30
  fetchData()
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

const handleResetFilters = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: ReturnOrder[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handleCreate = () => {
  message.info('打开新建退货申请表单')
}

const handleView = (record: ReturnOrder) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleApprove = async (record: ReturnOrder) => {
  try {
    await saleReturnApi.approve(record.id)
    message.success(`审核退货单成功: ${record.returnNo}`)
    fetchData()
  } catch (e) {
    console.warn('[退货管理] 审核失败', e)
    message.error('审核失败')
  }
}

const handleReceive = async (record: ReturnOrder) => {
  try {
    await saleReturnApi.receive(record.id)
    message.success(`入库完成: ${record.returnNo}`)
    fetchData()
  } catch (e) {
    console.warn('[退货管理] 入库失败', e)
    message.error('入库失败')
  }
}

const handleRefund = async (record: ReturnOrder) => {
  try {
    await saleReturnApi.refund(record.id)
    message.success(`退款完成: ${record.returnNo}`)
    fetchData()
  } catch (e) {
    console.warn('[退货管理] 退款失败', e)
    message.error('退款失败')
  }
}

const handleDelete = async (record: ReturnOrder) => {
  try {
    await request.delete(`/erp/sale/return/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[退货管理] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: ReturnOrder) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该退货单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

const handleExport = () => {
  message.info('导出退货单')
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
}

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      ...searchFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/erp/sale/return/page', { params })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
      // 更新统计
      statistics.value.totalCount = dataSource.value.length
      statistics.value.pendingCount = dataSource.value.filter(r => r.status === 0).length
      statistics.value.refundedCount = dataSource.value.filter(r => r.status === 3).length
      statistics.value.totalAmount = dataSource.value.reduce((sum, r) => sum + (r.returnAmount || 0), 0)
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdated.value = new Date().toISOString()
  } catch (error) {
    console.warn('[退货管理] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.return-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.return-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.return-page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.return-page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.list-update-timestamp {
  color: #999;
  font-size: 12px;
  margin-right: 12px;
}

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

.action-more-btn {
  padding: 0 4px;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
  border: 1px solid #ffa39e;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}






/* 空占位行 */
</style>
