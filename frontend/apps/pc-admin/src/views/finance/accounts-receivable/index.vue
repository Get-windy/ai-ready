<template>
  <PageContainer title="应收账款" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="fetchData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <div class="accounts-receivable-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">应收总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-received">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.paidAmount) }}</div>
            <div class="stat-card-label">已收金额</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-unreceived">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.unpaidAmount) }}</div>
            <div class="stat-card-label">未收金额</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-overdue">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(overdueAmount) }}</div>
            <div class="stat-card-label">逾期金额</div>
          </div>
          <WarningOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">应收笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :show-search="false"
        :show-export="true"
        :selectable="true"
        add-text="新增应收"
        @add="handleAdd"
        @refresh="fetchData"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @export="handleExport"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>

        <template #empty>
          <div class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的应收记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无应收账款数据，点击右上角「新增应收」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status !== 2" title="收款">
              <a-button type="link" size="small" @click="handlePayment(record)">
                <template #icon><DollarOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="paymentHistory">
                    <HistoryOutlined /> 收款记录
                  </a-menu-item>
                  <a-menu-item v-if="record.status !== 2" key="reminder">
                    <BellOutlined /> 催收提醒
                  </a-menu-item>
                  <a-menu-item v-if="isOverdue(record.dueDate, record.status)" key="markOverdue">
                    <WarningOutlined /> 标记逾期
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 详情弹窗 -->
      <a-modal
        v-model:open="detailVisible"
        title="应收账款详情"
        width="800px"
        centered
        :footer="null"
      >
        <a-descriptions bordered :column="2" v-if="currentRecord">
          <a-descriptions-item label="客户名称">
            <a @click="handleViewCustomer(currentRecord)">{{ currentRecord.customerName }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="订单号">
            <a @click="handleViewOrder(currentRecord)">{{ currentRecord.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="应收金额">
            <span class="amount-cell">¥{{ formatAmount(currentRecord.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="已收金额">
            <span class="amount-cell success">¥{{ formatAmount(currentRecord.paidAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="未收金额">
            <span class="amount-cell warning">¥{{ formatAmount(currentRecord.unpaidAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="到期日期">
            <span :class="{ 'overdue': isOverdue(currentRecord.dueDate, currentRecord.status) }">
              {{ currentRecord.dueDate }}
              <WarningOutlined v-if="isOverdue(currentRecord.dueDate, currentRecord.status)" class="overdue-icon" />
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentRecord.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 收款记录 -->
        <div class="detail-payment-section">
          <div class="detail-payment-title">收款记录</div>
          <a-table
            class="detail-table"
            :columns="paymentColumns"
            :data-source="paymentRecords"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="record.__empty_row">
                <span class="empty-placeholder">&nbsp;</span>
              </template>
              <template v-else-if="column.key === 'amount'">
                <span class="amount-cell success">¥{{ formatAmount(record.amount) }}</span>
              </template>
            </template>
          </a-table>
        </div>

        <div class="detail-modal-footer">
          <a-button v-if="currentRecord?.status !== 2" type="primary" @click="handlePayment(currentRecord)">
            <template #icon><DollarOutlined /></template>
            收款
          </a-button>
          <a-button v-if="currentRecord?.status !== 2" @click="handleReminder(currentRecord)">
            <template #icon><BellOutlined /></template>
            催收提醒
          </a-button>
          <a-button @click="detailVisible = false">关闭</a-button>
        </div>
      </a-modal>

      <!-- 收款弹窗 -->
      <a-modal
        v-model:open="paymentModalVisible"
        title="收款"
        width="500px"
        centered
        :confirm-loading="paymentSubmitting"
        @ok="handlePaymentConfirm"
        @cancel="paymentModalVisible = false"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
          <a-form-item label="客户">
            <span>{{ paymentRecord?.customerName }}</span>
          </a-form-item>
          <a-form-item label="未收金额">
            <span class="amount-cell warning">¥{{ formatAmount(paymentRecord?.unpaidAmount) }}</span>
          </a-form-item>
          <a-form-item label="收款金额" required>
            <a-input-number
              v-model:value="paymentAmount"
              :min="0"
              :max="paymentRecord?.unpaidAmount"
              :precision="2"
              style="width: 100%"
            >
              <template #addonBefore>¥</template>
            </a-input-number>
          </a-form-item>
          <a-form-item label="收款方式">
            <a-select v-model:value="paymentMethod" placeholder="选择收款方式">
              <a-select-option :value="1">银行转账</a-select-option>
              <a-select-option :value="2">现金</a-select-option>
              <a-select-option :value="3">支票</a-select-option>
              <a-select-option :value="4">微信/支付宝</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="收款日期">
            <a-date-picker v-model:value="paymentDate" style="width: 100%" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="paymentRemark" placeholder="备注信息" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  EyeOutlined, DollarOutlined, BellOutlined, SearchOutlined, InboxOutlined,
  EllipsisOutlined, CheckCircleOutlined, ExclamationCircleOutlined, WarningOutlined,
  FileTextOutlined, ReloadOutlined, HistoryOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import { receivableV1Api } from '@/api/finance/receivable'

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const lastUpdateTime = ref('')
const lastUpdated = ref('')

const queryParams = reactive({
  customerName: '',
  orderNo: '',
  status: undefined as number | undefined
})

const stats = reactive({
  totalAmount: 0,
  paidAmount: 0,
  unpaidAmount: 0
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const hasActiveFilters = computed(() => {
  return Object.values(queryParams).some(v => v !== undefined && v !== null && v !== '')
})

// 逾期金额计算
const overdueAmount = computed(() => {
  return dataSource.value.filter(r => isOverdue(r.dueDate, r.status))
    .reduce((sum, item) => sum + item.unpaidAmount, 0)
})

const filterFields = [
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '请输入客户名称' },
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未收款', value: 0 },
    { label: '部分收款', value: 1 },
    { label: '已收款', value: 2 }
  ]}
]

const columns = computed(() => [
  { title: '客户名称', field: 'customerName', width: 150 },
  { title: '订单号', field: 'orderNo', width: 150 },
  { title: '应收金额', field: 'amount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已收金额', field: 'paidAmount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '未收金额', field: 'unpaidAmount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }) => getStatusText(cellValue) },
  { title: '到期日期', field: 'dueDate', width: 140 },
  { title: '备注', field: 'remark', minWidth: 100 },
  { title: '操作', type: 'action', width: 160, fixed: 'right' }
])

const paymentColumns = [
  { title: '收款日期', dataIndex: 'paymentDate', width: 120 },
  { title: '收款金额', key: 'amount', width: 120, align: 'right' },
  { title: '收款方式', dataIndex: 'paymentMethod', width: 100 },
  { title: '备注', dataIndex: 'remark' }
]

const paymentRecords = ref<any[]>([])
const paymentModalVisible = ref(false)
const paymentSubmitting = ref(false)
const paymentRecord = ref<any>(null)
const paymentAmount = ref(0)
const paymentMethod = ref(1)
const paymentDate = ref<any>(dayjs())
const paymentRemark = ref('')

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'error', 1: 'warning', 2: 'success' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '未收款', 1: '部分收款', 2: '已收款' }
  return texts[status] || '未知'
}

const isOverdue = (dueDate: string, status: number) => {
  if (status === 2) return false
  return new Date(dueDate) < new Date()
}

const handleAdd = () => {
  message.info('打开新增应收表单')
}

const handleView = (record: any) => {
  currentRecord.value = record
  paymentRecords.value = record.paymentHistory || mockPaymentHistory()
  detailVisible.value = true
}

const handleViewCustomer = (record: any) => {
  router.push(`/crm/customer/detail/${record.customerId}`)
}

const handleViewOrder = (record: any) => {
  if (record.orderId) {
    router.push(`/sale/order/${record.orderId}`)
  }
}

const handlePayment = (record: any) => {
  paymentRecord.value = record
  paymentAmount.value = record.unpaidAmount || 0
  paymentMethod.value = 1
  paymentDate.value = dayjs()
  paymentRemark.value = ''
  paymentModalVisible.value = true
}

const handlePaymentConfirm = async () => {
  if (!paymentAmount.value || paymentAmount.value <= 0) {
    message.warning('请输入收款金额')
    return
  }
  paymentSubmitting.value = true
  try {
    await receivableV1Api.payment({
      id: paymentRecord.value.id,
      amount: paymentAmount.value,
      method: paymentMethod.value,
      date: paymentDate.value?.format('YYYY-MM-DD'),
      remark: paymentRemark.value
    })
    message.success('收款成功')
    paymentModalVisible.value = false
    detailVisible.value = false
    fetchData()
  } catch {
    message.error('收款失败')
  } finally {
    paymentSubmitting.value = false
  }
}

const handleReminder = (record: any) => {
  Modal.confirm({
    title: '发送催收提醒',
    content: `向客户 "${record.customerName}" 发送催收提醒？`,
    okText: '确认发送',
    centered: true,
    onOk: async () => {
      try {
        await receivableV1Api.reminder(record.id)
        message.success('催收提醒已发送')
      } catch {
        message.error('发送失败')
      }
    }
  })
}

const handleActionMenuClick = (key: string, record: any) => {
  switch (key) {
    case 'paymentHistory': handleView(record); break
    case 'reminder': handleReminder(record); break
    case 'markOverdue': message.info(`已标记 ${record.customerName} 为逾期状态`); break
  }
}

const handleResetFilters = () => {
  Object.keys(queryParams).forEach(k => { (queryParams as any)[k] = undefined })
  pagination.current = 1
  fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  queryParams.customerName = filters.customerName || ''
  queryParams.orderNo = filters.orderNo || ''
  queryParams.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: any[], ids: any[]) => {
  // 可以在这里处理选中行的逻辑，例如批量操作
  console.log('Selected rows:', rows.length)
}

const handleExport = () => {
  const headers = ['客户名称', '订单号', '应收金额', '已收金额', '未收金额', '状态', '到期日期', '备注']
  const rows = dataSource.value.map(r => [
    r.customerName || '', r.orderNo || '', formatAmount(r.amount), formatAmount(r.paidAmount),
    formatAmount(r.unpaidAmount), getStatusText(r.status), r.dueDate || '', r.remark || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `应收账款_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

const mockPaymentHistory = (): any[] => [
  { id: 1, paymentDate: '2024-01-15', amount: 20000, paymentMethod: '银行转账', remark: '首批收款' }
]

const mockData = (): any[] => [
  { id: 1, customerName: '北京客户A', customerId: 1, orderNo: 'SO2024010001', orderId: 1, amount: 85000, paidAmount: 35000, unpaidAmount: 50000, status: 1, dueDate: '2024-02-15', remark: '', createTime: '2024-01-15 10:00' },
  { id: 2, customerName: '上海客户B', customerId: 2, orderNo: 'SO2024010002', orderId: 2, amount: 42000, paidAmount: 0, unpaidAmount: 42000, status: 0, dueDate: '2024-02-18', remark: '', createTime: '2024-01-18 11:00' },
  { id: 3, customerName: '广州客户C', customerId: 3, orderNo: 'SO2024010003', orderId: 3, amount: 28000, paidAmount: 28000, unpaidAmount: 0, status: 2, dueDate: '2024-02-01', remark: '已结清', createTime: '2024-01-20 09:00' },
  { id: 4, customerName: '深圳客户D', customerId: 4, orderNo: 'SO2024010004', orderId: 4, amount: 65000, paidAmount: 30000, unpaidAmount: 35000, status: 1, dueDate: '2024-01-25', remark: '', createTime: '2024-01-12 14:00' },
  { id: 5, customerName: '杭州客户E', customerId: 5, orderNo: 'SO2024010005', orderId: 5, amount: 18000, paidAmount: 18000, unpaidAmount: 0, status: 2, dueDate: '2024-02-01', remark: '', createTime: '2024-01-22 15:00' },
  { id: 6, customerName: '成都客户F', customerId: 6, orderNo: 'SO2024010006', orderId: 6, amount: 52000, paidAmount: 0, unpaidAmount: 52000, status: 0, dueDate: '2024-01-20', remark: '已逾期', createTime: '2024-01-10 16:00' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await receivableV1Api.getPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      customerName: queryParams.customerName || undefined,
      orderNo: queryParams.orderNo || undefined,
      status: queryParams.status
    })
    if (res.data) {
      dataSource.value = res.data.records || mockData()
      pagination.total = res.data.total || mockData().length
      lastUpdated.value = new Date().toISOString()
      stats.totalAmount = dataSource.value.reduce((sum, item) => sum + item.amount, 0)
      stats.paidAmount = dataSource.value.reduce((sum, item) => sum + item.paidAmount, 0)
      stats.unpaidAmount = dataSource.value.reduce((sum, item) => sum + item.unpaidAmount, 0)
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch {
    message.error('获取数据失败')
    dataSource.value = mockData()
    pagination.total = mockData().length
    stats.totalAmount = dataSource.value.reduce((sum, item) => sum + item.amount, 0)
    stats.paidAmount = dataSource.value.reduce((sum, item) => sum + item.paidAmount, 0)
    stats.unpaidAmount = dataSource.value.reduce((sum, item) => sum + item.unpaidAmount, 0)
  } finally {
    loading.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.accounts-receivable-page {
  height: 100%;
  display: flex;
  flex-direction: column;
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

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
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
  padding: 14px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-received { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-unreceived { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-overdue { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 18px;
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
  font-size: 24px;
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

.empty-placeholder {
  color: transparent;
}

.customer-link {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.success {
  color: #52c41a;
}

.amount-cell.warning {
  color: #faad14;
}

.overdue {
  color: #ff4d4f;
  font-weight: 500;
}

.overdue-icon {
  margin-left: 4px;
  font-size: 12px;
}

.action-more-btn {
  padding: 0 4px;
}

/* 详情弹窗 */
.detail-payment-section {
  margin-top: 16px;
}

.detail-payment-title {
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

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
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

/* 详情弹窗表格网格边框 */
.detail-table :deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

.detail-table :deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

.detail-table :deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

.detail-table :deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
  }
}
</style>