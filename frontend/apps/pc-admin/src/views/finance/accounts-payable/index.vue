<template>
  <PageContainer title="应付账款" full-height>
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

    <div class="accounts-payable-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">应付总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-paid">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.paidAmount) }}</div>
            <div class="stat-card-label">已付金额</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-unpaid">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.unpaidAmount) }}</div>
            <div class="stat-card-label">未付金额</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">应付笔数</div>
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
        add-text="新增应付"
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
              没有符合条件的应付记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无应付账款数据，点击右上角「新增应付」开始创建
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
            <a-tooltip v-if="record.status !== 2" title="付款">
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
                    <HistoryOutlined /> 付款记录
                  </a-menu-item>
                  <a-menu-item key="reminder">
                    <BellOutlined /> 提醒付款
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
        title="应付账款详情"
        width="800px"
        centered
        :footer="null"
      >
        <a-descriptions bordered :column="2" v-if="currentRecord">
          <a-descriptions-item label="供应商名称">
            <a @click="handleViewSupplier(currentRecord)">{{ currentRecord.supplierName }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="订单号">
            <a @click="handleViewOrder(currentRecord)">{{ currentRecord.orderNo }}</a>
          </a-descriptions-item>
          <a-descriptions-item label="应付金额">
            <span class="amount-cell">¥{{ formatAmount(currentRecord.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="已付金额">
            <span class="amount-cell success">¥{{ formatAmount(currentRecord.paidAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="未付金额">
            <span class="amount-cell warning">¥{{ formatAmount(currentRecord.unpaidAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="到期日期">
            <span :class="{ 'overdue': isOverdue(currentRecord.dueDate, currentRecord.status) }">
              {{ currentRecord.dueDate }}
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentRecord.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <!-- 付款记录 -->
        <div class="detail-payment-section">
          <div class="detail-payment-title">付款记录</div>
          <VxeTableList
            :columns="paymentColumns"
            :data-source="paymentRecords"
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

        <div class="detail-modal-footer">
          <a-button v-if="currentRecord?.status !== 2" type="primary" @click="handlePayment(currentRecord)">
            <template #icon><DollarOutlined /></template>
            付款
          </a-button>
          <a-button @click="detailVisible = false">关闭</a-button>
        </div>
      </a-modal>

      <!-- 付款弹窗 -->
      <a-modal
        v-model:open="paymentModalVisible"
        title="付款"
        width="500px"
        centered
        :confirm-loading="paymentSubmitting"
        @ok="handlePaymentConfirm"
        @cancel="paymentModalVisible = false"
      >
        <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
          <a-form-item label="供应商">
            <span>{{ paymentRecord?.supplierName }}</span>
          </a-form-item>
          <a-form-item label="未付金额">
            <span class="amount-cell warning">¥{{ formatAmount(paymentRecord?.unpaidAmount) }}</span>
          </a-form-item>
          <a-form-item label="付款金额" required>
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
          <a-form-item label="付款方式">
            <a-select v-model:value="paymentMethod" placeholder="选择付款方式">
              <a-select-option :value="1">银行转账</a-select-option>
              <a-select-option :value="2">现金</a-select-option>
              <a-select-option :value="3">承兑汇票</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="付款日期">
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
  EyeOutlined, DollarOutlined, SearchOutlined, InboxOutlined,
  EllipsisOutlined, CheckCircleOutlined, ExclamationCircleOutlined,
  FileTextOutlined, ReloadOutlined, HistoryOutlined, BellOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import request from '@/utils/request'

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const lastUpdateTime = ref('')
const lastUpdated = ref('')

const queryParams = reactive({
  supplierName: '',
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

const filterFields = [
  { key: 'supplierName', label: '供应商名称', type: 'input' as const, placeholder: '请输入供应商名称' },
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未付款', value: 0 },
    { label: '部分付款', value: 1 },
    { label: '已付款', value: 2 }
  ]}
]

const columns = computed(() => [
  { title: '供应商名称', field: 'supplierName', width: 150 },
  { title: '订单号', field: 'orderNo', width: 150 },
  { title: '应付金额', field: 'amount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已付金额', field: 'paidAmount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '未付金额', field: 'unpaidAmount', width: 130, align: 'right', formatter: ({ cellValue }) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }) => getStatusText(cellValue) },
  { title: '到期日期', field: 'dueDate', width: 120 },
  { title: '备注', field: 'remark', minWidth: 100 },
  { title: '操作', type: 'action', width: 150, fixed: 'right' }
])

const paymentColumns = [
  { title: '付款日期', field: 'paymentDate', width: 120 },
  { title: '付款金额', field: 'amount', width: 120, align: 'right', slotName: 'amountCell' },
  { title: '付款方式', field: 'paymentMethod', width: 100 },
  { title: '备注', field: 'remark' }
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
  const colors: Record<number, string> = { 0: 'warning', 1: 'processing', 2: 'success' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '未付款', 1: '部分付款', 2: '已付款' }
  return texts[status] || '未知'
}

const isOverdue = (dueDate: string, status: number) => {
  if (status === 2) return false
  return new Date(dueDate) < new Date()
}

const handleAdd = () => {
  message.info('打开新增应付表单')
}

const handleView = (record: any) => {
  currentRecord.value = record
  paymentRecords.value = record.paymentHistory || mockPaymentHistory()
  detailVisible.value = true
}

const handleViewSupplier = (record: any) => {
  router.push(`/supplier/detail/${record.supplierId}`)
}

const handleViewOrder = (record: any) => {
  if (record.orderId) {
    router.push(`/purchase/order/${record.orderId}`)
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
    message.warning('请输入付款金额')
    return
  }
  paymentSubmitting.value = true
  try {
    await request.post('/finance/payable/payment', {
      id: paymentRecord.value.id,
      amount: paymentAmount.value,
      method: paymentMethod.value,
      date: paymentDate.value?.format('YYYY-MM-DD'),
      remark: paymentRemark.value
    })
    message.success('付款成功')
    paymentModalVisible.value = false
    detailVisible.value = false
    fetchData()
  } catch {
    message.error('付款失败')
  } finally {
    paymentSubmitting.value = false
  }
}

const handleActionMenuClick = (key: string, record: any) => {
  switch (key) {
    case 'paymentHistory': handleView(record); break
    case 'reminder': message.info(`已发送付款提醒给 ${record.supplierName}`); break
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
  queryParams.supplierName = filters.supplierName || ''
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
  const headers = ['供应商名称', '订单号', '应付金额', '已付金额', '未付金额', '状态', '到期日期', '备注']
  const rows = dataSource.value.map(r => [
    r.supplierName || '', r.orderNo || '', formatAmount(r.amount), formatAmount(r.paidAmount),
    formatAmount(r.unpaidAmount), getStatusText(r.status), r.dueDate || '', r.remark || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `应付账款_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

const mockPaymentHistory = (): any[] => [
  { id: 1, paymentDate: '2024-01-10', amount: 20000, paymentMethod: '银行转账', remark: '首批付款' }
]

const mockData = (): any[] => [
  { id: 1, supplierName: '北京供应商', supplierId: 1, orderNo: 'PO2024010001', orderId: 1, amount: 58000, paidAmount: 28000, unpaidAmount: 30000, status: 1, dueDate: '2024-02-15', remark: '', createTime: '2024-01-15 10:00' },
  { id: 2, supplierName: '上海贸易公司', supplierId: 2, orderNo: 'PO2024010002', orderId: 2, amount: 32000, paidAmount: 0, unpaidAmount: 32000, status: 0, dueDate: '2024-02-18', remark: '', createTime: '2024-01-18 11:00' },
  { id: 3, supplierName: '广州制造企业', supplierId: 3, orderNo: 'PO2024010003', orderId: 3, amount: 15000, paidAmount: 15000, unpaidAmount: 0, status: 2, dueDate: '2024-02-01', remark: '已结清', createTime: '2024-01-20 09:00' },
  { id: 4, supplierName: '深圳电子公司', supplierId: 4, orderNo: 'PO2024010004', orderId: 4, amount: 42000, paidAmount: 20000, unpaidAmount: 22000, status: 1, dueDate: '2024-01-30', remark: '', createTime: '2024-01-12 14:00' },
  { id: 5, supplierName: '杭州供应商', supplierId: 5, orderNo: 'PO2024010005', orderId: 5, amount: 8000, paidAmount: 8000, unpaidAmount: 0, status: 2, dueDate: '2024-02-01', remark: '', createTime: '2024-01-22 15:00' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.post('/finance/payable/list', {
      supplierName: queryParams.supplierName,
      status: queryParams.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
      stats.totalAmount = dataSource.value.reduce((sum, item) => sum + item.amount, 0)
      stats.paidAmount = dataSource.value.reduce((sum, item) => sum + item.paidAmount, 0)
      stats.unpaidAmount = dataSource.value.reduce((sum, item) => sum + item.unpaidAmount, 0)
    } else {
      dataSource.value = mockData()
      pagination.total = mockData().length
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
.accounts-payable-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
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

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-paid { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-unpaid { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

.supplier-link {
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





/* 详情弹窗表格网格边框 */




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
