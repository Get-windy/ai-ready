<template>
  <div class="accounts-receivable-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-area">
      <a-col :span="6">
        <a-statistic
          title="应收总额"
          :value="stats.totalAmount"
          :precision="2"
          prefix="¥"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="已收金额"
          :value="stats.paidAmount"
          :precision="2"
          prefix="¥"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="未收金额"
          :value="stats.unpaidAmount"
          :precision="2"
          prefix="¥"
          :value-style="{ color: '#ff4d4f' }"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="应收笔数"
          :value="totalCount"
          suffix="笔"
        />
      </a-col>
    </a-row>

    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'finance-accounts-receivable-list'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-export="true"
      :selectable="false"
      add-text="新增应收"
      @add="handleAdd"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配应收记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无应收账款">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button type="primary" @click="handleAdd">新增应收</a-button>
        </a-empty>
      </template>

      <template #status="{ record }">
        <a-tag :color="getStatusColor(record.status)">
          {{ getStatusText(record.status) }}
        </a-tag>
      </template>
      <template #amount="{ record }">
        ¥{{ record.amount?.toFixed(2) }}
      </template>
      <template #paidAmount="{ record }">
        ¥{{ record.paidAmount?.toFixed(2) }}
      </template>
      <template #unpaidAmount="{ record }">
        ¥{{ record.unpaidAmount?.toFixed(2) }}
      </template>
      <template #dueDate="{ record }">
        <span :class="{ 'overdue': isOverdue(record.dueDate) }">
          {{ record.dueDate }}
        </span>
      </template>
      <template #action="{ record }">
        <a-space :size="0" class="action-cell-inner">
          <a-tooltip title="查看">
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
                <a-menu-item key="reminder">
                  <BellOutlined /> 催收
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </TableList>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="应收账款详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="客户名称">{{ currentRecord.customerName }}</a-descriptions-item>
        <a-descriptions-item label="订单号">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="应收金额">¥{{ currentRecord.amount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="已收金额">¥{{ currentRecord.paidAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="未收金额">¥{{ currentRecord.unpaidAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="到期日期">
          <span :class="{ 'overdue': isOverdue(currentRecord.dueDate) }">{{ currentRecord.dueDate }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-modal-footer">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { EyeOutlined, DollarOutlined, BellOutlined, SearchOutlined, InboxOutlined, EllipsisOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import dayjs from 'dayjs'
import { receivableV1Api, type ReceivableItem } from '@/api/finance/receivable'

interface AccountsReceivable {
  id: number
  customerName: string
  orderNo: string
  amount: number
  paidAmount: number
  unpaidAmount: number
  status: number
  dueDate: string
  remark: string
}

const tableRef = ref()
const loading = ref(false)
const dataSource = ref<AccountsReceivable[]>([])
const detailVisible = ref(false)
const currentRecord = ref<AccountsReceivable | null>(null)

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

const totalCount = computed(() => dataSource.value.length)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(queryParams).some(v => v !== undefined && v !== null && v !== '')
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

const columns = [
  {
    title: '客户名称',
    dataIndex: 'customerName',
    key: 'customerName',
    width: 150
  },
  {
    title: '订单号',
    dataIndex: 'orderNo',
    key: 'orderNo',
    width: 150
  },
  {
    title: '应收金额',
    key: 'amount',
    width: 120,
    slotName: 'amount'
  },
  {
    title: '已收金额',
    key: 'paidAmount',
    width: 120,
    slotName: 'paidAmount'
  },
  {
    title: '未收金额',
    key: 'unpaidAmount',
    width: 120,
    slotName: 'unpaidAmount'
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    slotName: 'status'
  },
  {
    title: '到期日期',
    key: 'dueDate',
    width: 120,
    slotName: 'dueDate'
  },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    ellipsis: true
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right' as const,
    slotName: 'action'
  }
]

// 获取状态颜色
const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'error',
    1: 'warning',
    2: 'success'
  }
  return colors[status] || 'default'
}

// 获取状态文本
const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '未收款',
    1: '部分收款',
    2: '已收款'
  }
  return texts[status] || '未知'
}

// 判断是否逾期
const isOverdue = (dueDate: string) => {
  return new Date(dueDate) < new Date()
}

// 获取数据
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
      dataSource.value = res.data.records || []
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
    }

    // 获取统计信息（使用分页数据计算）
    const totalAmount = dataSource.value.reduce((sum, item) => sum + (item.amount || 0), 0)
    const paidAmount = dataSource.value.reduce((sum, item) => sum + (item.paidAmount || 0), 0)
    const unpaidAmount = dataSource.value.reduce((sum, item) => sum + (item.unpaidAmount || 0), 0)
    stats.totalAmount = totalAmount
    stats.paidAmount = paidAmount
    stats.unpaidAmount = unpaidAmount
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  queryParams.customerName = filters.customerName || ''
  queryParams.orderNo = filters.orderNo || ''
  queryParams.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

// 新增应收
const handleAdd = () => {
  message.info('打开新增应收表单')
}

// 查看
const handleView = (record: AccountsReceivable) => {
  currentRecord.value = record
  detailVisible.value = true
}

// 收款
const handlePayment = (record: AccountsReceivable) => {
  message.info(`打开收款页面: ${record.customerName}`)
}

// 催收
const handleReminder = (record: AccountsReceivable) => {
  message.info(`发送催收提醒: ${record.customerName}`)
}

function handleResetFilters() {
  Object.keys(queryParams).forEach(k => { (queryParams as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'reminder': handleReminder(record); break
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

// 导出
const handleExport = () => {
  message.info('导出应收账款')
}

</script>

<style scoped>
.accounts-receivable-page {
  padding: 24px;
}

.stats-area {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.overdue {
  color: #ff4d4f;
  font-weight: bold;
}
.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.detail-modal-footer { text-align: right; margin-top: 16px; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
