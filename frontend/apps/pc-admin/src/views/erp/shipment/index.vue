<template>
  <PageContainer full-height>
    <template #header>
      <div class="shipment-page-header">
        <div class="shipment-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>发货管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="shipment-page-title">发货管理</h2>
        </div>
        <div class="shipment-page-header-right">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
            <span v-if="lastUpdateTime" class="update-time">
              数据更新: {{ lastUpdateTime }}
            </span>
          </span>
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
            <div class="summary-title">出库单总数</div>
            <div class="summary-value">{{ statusCounts.total }}</div>
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
            <div class="summary-value warning">{{ statusCounts.pending }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <ExportOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待出库</div>
            <div class="summary-value">{{ statusCounts.processing }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statusCounts.completed }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <ErrorBoundary @reset="fetchData">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :selectable="true"
        :show-export="true"
        add-text="新建出库单"
        @add="handleCreate"
        @refresh="fetchData"
        @export="handleExport"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <span class="stats-summary">
            <span class="stats-item">
              <span class="stats-label">待审核:</span>
              <span class="stats-value pending">{{ statusCounts.pending }}</span>
            </span>
            <span class="stats-item">
              <span class="stats-label">待出库:</span>
              <span class="stats-value processing">{{ statusCounts.processing }}</span>
            </span>
            <span class="stats-item">
              <span class="stats-label">已完成:</span>
              <span class="stats-value completed">{{ statusCounts.completed }}</span>
            </span>
          </span>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的出库单，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无出库单数据，点击右上角「新建出库单」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <a-space>
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="审核">
              <a-button type="link" size="small" @click="handleApprove(record)">
                <template #icon><CheckCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="出库">
              <a-button type="link" size="small" @click="handleShip(record)">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              v-if="record.status >= 2"
              templateType="stock_out"
              :businessId="record.id"
              businessType="shipment"
              buttonText="打印"
              buttonSize="small"
              @print-success="handlePrintSuccess(record)"
              @print-error="handlePrintError"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="edit" v-if="record.status === 0">
                    <EditOutlined /> 编辑
                  </a-menu-item>
                  <a-menu-item key="delete" v-if="record.status === 0">
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                  <a-menu-item key="tracking" v-if="record.status >= 2 && !record.trackingNo">
                    <NumberOutlined /> 填写物流单号
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>
    </ErrorBoundary>

    <!-- 详情弹窗 -->
    <a-drawer
      v-model:open="detailVisible"
      title="出库单详情"
      placement="right"
      width="80vw"
      :footer="null"
    >
      <template #extra>
        <a-button size="small" @click="handlePrintFromDetail">
          <template #icon><PrinterOutlined /></template>
          打印
        </a-button>
      </template>

      <a-descriptions bordered :column="2" v-if="currentRecord" size="small">
        <a-descriptions-item label="出库单号">
          <span class="code-text">{{ currentRecord.shipmentNo }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="销售订单">
          <a @click="handleViewOrder">{{ currentRecord.orderNo }}</a>
        </a-descriptions-item>
        <a-descriptions-item label="客户名称">
          <span class="customer-name">{{ currentRecord.customerName }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="出库金额">
          <span class="amount-cell">¥{{ formatAmount(currentRecord.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :status="currentRecord.status" :map="SHIPMENT_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="出库日期">{{ currentRecord.shipmentDate }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
        <a-descriptions-item label="物流单号">
          <span v-if="currentRecord.trackingNo">{{ currentRecord.trackingNo }}</span>
          <span v-else class="empty-text">未填写</span>
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">
          <span v-if="currentRecord.remark">{{ currentRecord.remark }}</span>
          <span v-else class="empty-text">无</span>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 出库明细 -->
      <div class="detail-items-section">
        <h4 class="section-title">出库明细</h4>
        <VxeTableList
          :columns="itemColumns"
          :data-source="currentRecordItems"
          :pagination="false"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #amountCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
          </template>
        </VxeTableList>
      </div>
    </a-drawer>

    <!-- 物流单号填写弹窗 -->
    <a-modal
      v-model:open="trackingVisible"
      title="填写物流单号"
      width="400px"
      @ok="handleSaveTracking"
    >
      <a-form layout="vertical">
        <a-form-item label="物流单号">
          <a-input v-model:value="trackingForm.trackingNo" placeholder="请输入物流单号" />
        </a-form-item>
        <a-form-item label="物流公司">
          <a-select v-model:value="trackingForm.carrier" placeholder="请选择物流公司">
            <a-select-option value="SF">顺丰速运</a-select-option>
            <a-select-option value="EMS">EMS</a-select-option>
            <a-select-option value="JD">京东物流</a-select-option>
            <a-select-option value="YT">圆通速递</a-select-option>
            <a-select-option value="ZT">中通快递</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { SHIPMENT_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'
import {
  EyeOutlined,
  CheckCircleOutlined,
  ExportOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  InboxOutlined,
  ReloadOutlined,
  PrinterOutlined,
  NumberOutlined,
  FileTextOutlined,
  ClockCircleOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'

interface ShipmentItem {
  id: number
  productName: string
  productCode: string
  quantity: number
  unitPrice: number
  amount: number
}

interface Shipment {
  id: number
  shipmentNo: string
  orderNo: string
  customerName: string
  warehouseName: string
  totalAmount: number
  status: number
  shipmentDate: string
  operator: string
  trackingNo?: string
  remark?: string
  items?: ShipmentItem[]
}

const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<Shipment[]>([])
const detailVisible = ref(false)
const trackingVisible = ref(false)
const currentRecord = ref<Shipment | null>(null)
const tableRef = ref()
const lastUpdateTime = ref<string>('')
const autoRefreshCountdown = ref(0)
const selectedRows = ref<Shipment[]>([])
const selectedIds = ref<number[]>([])
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchFilters = reactive<Record<string, any>>({})
const trackingForm = reactive({
  trackingNo: '',
  carrier: ''
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 状态统计
const statusCounts = computed(() => {
  const pending = dataSource.value.filter(item => item.status === 0).length
  const processing = dataSource.value.filter(item => item.status === 1).length
  const completed = dataSource.value.filter(item => item.status >= 2).length
  const total = dataSource.value.length
  return { pending, processing, completed, total }
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})


// 详情明细
const currentRecordItems = computed(() => {
  return currentRecord.value?.items || []
})

const vxeColumns = computed(() => [
  { field: 'shipmentNo', title: '出库单号', width: 150 },
  { field: 'orderNo', title: '销售订单', width: 150 },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'totalAmount', title: '出库金额', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => SHIPMENT_STATUS[cellValue]?.text || '' },
  { field: 'shipmentDate', title: '出库日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' },
])

const itemColumns = [
  { title: '商品名称', field: 'productName', width: 200 },
  { title: '商品编码', field: 'productCode', width: 120 },
  { title: '数量', field: 'quantity', width: 80, align: 'right' },
  { title: '单价', field: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', field: 'amount', width: 120, align: 'right', slotName: 'amountCell' }
]

const filterFields = [
  { key: 'shipmentNo', label: '出库单号', type: 'input' as const, placeholder: '请输入出库单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '请输入客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已出库', value: 2 },
    { label: '已签收', value: 3 }
  ]}
]

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const handleRefresh = async () => {
  lastUpdateTime.value = ''
  await fetchData()
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

const handleCreate = () => {
  message.info('打开新建出库单表单')
}

const handleView = (record: Shipment) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleViewOrder = () => {
  if (currentRecord.value?.orderNo) {
    message.info(`查看销售订单: ${currentRecord.value.orderNo}`)
  }
}

const handleApprove = (record: Shipment) => {
  Modal.confirm({
    title: '审核确认',
    content: `确认审核出库单 ${record.shipmentNo} 吗？`,
    okText: '确认审核',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/sale/outbound/${record.id}/approve`)
        message.success('审核成功')
        fetchData()
      } catch (error) {
        console.warn('[发货管理] 审核失败', error)
        message.error('审核失败')
      }
    }
  })
}

const handleShip = (record: Shipment) => {
  Modal.confirm({
    title: '出库确认',
    content: `确认出库单 ${record.shipmentNo} 已完成出库吗？`,
    okText: '确认出库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await request.put(`/erp/sale/outbound/${record.id}/ship`)
        message.success('出库成功')
        fetchData()
      } catch (error) {
        console.warn('[发货管理] 出库失败', error)
        message.error('出库失败')
      }
    }
  })
}

const handlePrintSuccess = (record: Shipment) => {
  message.success(`出库单 ${record.shipmentNo} 打印成功`)
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

const handlePrintFromDetail = () => {
  message.info('打印当前出库单')
}

const handleDelete = async (record: Shipment) => {
  try {
    await request.delete(`/erp/sale/outbound/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.warn('[发货管理] 删除失败', error)
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: Shipment) => {
  switch (key) {
    case 'edit':
      message.info(`编辑出库单: ${record.shipmentNo}`)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: '删除后数据不可恢复，确定要删除该出库单吗？',
        okText: '确定',
        cancelText: '取消',
        onOk: () => handleDelete(record)
      })
      break
    case 'tracking':
      currentRecord.value = record
      trackingForm.trackingNo = record.trackingNo || ''
      trackingForm.carrier = ''
      trackingVisible.value = true
      break
  }
}

const handleSaveTracking = async () => {
  if (!trackingForm.trackingNo) {
    message.warning('请输入物流单号')
    return
  }
  try {
    if (currentRecord.value) {
      await request.put(`/erp/sale/outbound/${currentRecord.value.id}/tracking`, trackingForm)
      message.success('物流单号已保存')
      trackingVisible.value = false
      fetchData()
    }
  } catch (error) {
    console.warn('[发货管理] 保存物流信息失败', error)
    message.error('保存失败')
  }
}

const handleExport = () => {
  message.info('导出出库单数据')
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleSelectionChange = (rows: Shipment[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    handleRefresh()
  }
}

const fetchData = async (silent = false) => {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = {
      ...searchFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/erp/sale/outbound/page', { params })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (error) {
    console.warn('[发货管理] 获取数据失败', error)
    if (!silent) {
      hasError.value = true
      message.error('获取数据失败')
    }
    dataSource.value = []
  } finally {
    if (!silent) loading.value = false
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData(true)
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
.shipment-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.shipment-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.shipment-page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.shipment-page-header-right {
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
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
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

.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

.stats-summary {
  display: flex;
  gap: 16px;
  margin-right: 16px;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.stats-label {
  color: #666;
}

.stats-value {
  font-weight: 500;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stats-value.pending {
  color: #faad14;
}

.stats-value.processing {
  color: #1890ff;
}

.stats-value.completed {
  color: #52c41a;
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



.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.code-text {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 13px;
}

.customer-name {
  font-weight: 500;
}

.empty-text {
  color: #999;
}

.detail-items-section {
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}






/* 详情弹窗表格 */
.detail-modal :deep(.ant-descriptions-item-label) {
  font-weight: 500;
  background: #fafafa;
}

</style>
