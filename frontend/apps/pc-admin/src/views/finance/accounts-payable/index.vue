<template>
  <div class="accounts-payable-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stats-area">
      <a-col :span="6">
        <a-statistic
          title="应付总额"
          :value="stats.totalAmount"
          :precision="2"
          prefix="¥"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="已付金额"
          :value="stats.paidAmount"
          :precision="2"
          prefix="¥"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="未付金额"
          :value="stats.unpaidAmount"
          :precision="2"
          prefix="¥"
          :value-style="{ color: '#fa8c16' }"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="应付笔数"
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
      :table-key="'finance-accounts-payable-list'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-export="true"
      :selectable="false"
      add-text="新增应付"
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
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配应付记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无应付账款">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button type="primary" @click="handleAdd">新增应付</a-button>
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
      <template #action="{ record }">
        <a-space :size="0" class="action-cell-inner">
          <a-tooltip title="查看">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status !== 2" title="付款">
            <a-button type="link" size="small" @click="handlePayment(record)">
              <template #icon><DollarOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </template>
    </TableList>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="应付账款详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="供应商名称">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="订单号">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="应付金额">¥{{ currentRecord.amount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="已付金额">¥{{ currentRecord.paidAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="未付金额">¥{{ currentRecord.unpaidAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="到期日期">{{ currentRecord.dueDate }}</a-descriptions-item>
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
import { EyeOutlined, DollarOutlined, SearchOutlined, InboxOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import dayjs from 'dayjs'
import request from '@/utils/request'

interface AccountsPayable {
  id: number
  supplierName: string
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
const dataSource = ref<AccountsPayable[]>([])
const detailVisible = ref(false)
const currentRecord = ref<AccountsPayable | null>(null)

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
  { key: 'supplierName', label: '供应商名称', type: 'input' as const, placeholder: '请输入供应商名称' },
  { key: 'orderNo', label: '订单号', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '未付款', value: 0 },
    { label: '部分付款', value: 1 },
    { label: '已付款', value: 2 }
  ]}
]

const columns = [
  {
    title: '供应商名称',
    dataIndex: 'supplierName',
    key: 'supplierName',
    width: 150
  },
  {
    title: '订单号',
    dataIndex: 'orderNo',
    key: 'orderNo',
    width: 150
  },
  {
    title: '应付金额',
    key: 'amount',
    width: 120,
    slotName: 'amount'
  },
  {
    title: '已付金额',
    key: 'paidAmount',
    width: 120,
    slotName: 'paidAmount'
  },
  {
    title: '未付金额',
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
    dataIndex: 'dueDate',
    key: 'dueDate',
    width: 120
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
    width: 150,
    fixed: 'right' as const,
    slotName: 'action'
  }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'warning',
    1: 'processing',
    2: 'success'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '未付款',
    1: '部分付款',
    2: '已付款'
  }
  return texts[status] || '未知'
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.supplierName = ''
  queryParams.orderNo = ''
  queryParams.status = undefined
  handleSearch()
}

const handleAdd = () => {
  message.info('打开新增应付表单')
}

const handleView = (record: AccountsPayable) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handlePayment = (record: AccountsPayable) => {
  message.info(`打开付款页面: ${record.supplierName}`)
}

const handleExport = () => {
  message.info('导出应付账款')
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
      stats.totalAmount = dataSource.value.reduce((sum, item) => sum + item.amount, 0)
      stats.paidAmount = dataSource.value.reduce((sum, item) => sum + item.paidAmount, 0)
      stats.unpaidAmount = dataSource.value.reduce((sum, item) => sum + item.unpaidAmount, 0)
    } else {
      dataSource.value = []
      pagination.total = 0
      stats.totalAmount = 0
      stats.paidAmount = 0
      stats.unpaidAmount = 0
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleResetFilters() {
  Object.keys(queryParams).forEach(k => { (queryParams as any)[k] = undefined })
  pagination.current = 1; fetchData()
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
  padding: 24px;
}

.stats-area {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}
.detail-modal-footer { text-align: right; margin-top: 16px; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
</style>
