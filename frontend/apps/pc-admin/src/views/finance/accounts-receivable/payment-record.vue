<template>
  <div class="payment-record-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
          <div class="stat-card-label">收款总额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-today">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.todayAmount) }}</div>
          <div class="stat-card-label">今日收款</div>
        </div>
        <CalendarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-count">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.totalCount }}</div>
          <div class="stat-card-label">收款笔数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-avg">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(avgAmount) }}</div>
          <div class="stat-card-label">平均金额</div>
        </div>
        <LineChartOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <a-form
        layout="inline"
        :model="queryParams"
      >
        <a-form-item label="客户名称">
          <a-input
            v-model:value="queryParams.customerName"
            placeholder="请输入客户名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="收款日期">
          <a-range-picker
            v-model:value="queryParams.dateRange"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item label="收款方式">
          <a-select
            v-model:value="queryParams.paymentMethod"
            placeholder="请选择"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="cash">
              现金
            </a-select-option>
            <a-select-option value="bank">
              银行转账
            </a-select-option>
            <a-select-option value="wechat">
              微信支付
            </a-select-option>
            <a-select-option value="alipay">
              支付宝
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-button @click="handleReset">
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 操作按钮 -->
    <div class="action-area">
      <a-space>
        <a-button
          type="primary"
          @click="handleAdd"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          新增收款
        </a-button>
        <a-button @click="handleExport">
          <template #icon>
            <ExportOutlined />
          </template>
          导出
        </a-button>
      </a-space>
    </div>

    <!-- 数据表格 -->
    <div class="table-area">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
      >
        <template #amountCell="{ record }">
          <span class="amount-cell">¥{{ record.amount?.toFixed(2) }}</span>
        </template>
        <template #paymentMethodCell="{ record }">
          <a-tag>{{ getPaymentMethodText(record.paymentMethod) }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="handleView(record)"
            >
              查看
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handlePrint(record)"
            >
              打印
            </a-button>
          </a-space>
        </template>
      </VxeTableList>
    </div>

    <a-modal
      v-model:open="detailVisible"
      title="收款记录详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="客户名称">{{ currentRecord.customerName }}</a-descriptions-item>
        <a-descriptions-item label="订单号">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="收款金额">¥{{ currentRecord.amount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="收款方式">
          <a-tag>{{ getPaymentMethodText(currentRecord.paymentMethod) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="收款日期">{{ currentRecord.paymentDate }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
        <a-descriptions-item label="收款账户">{{ currentRecord.bankAccount || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div style="text-align: right; margin-top: 16px">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, DollarOutlined, CalendarOutlined, FileTextOutlined, LineChartOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'

interface PaymentRecord {
  id: number
  customerName: string
  orderNo: string
  amount: number
  paymentMethod: string
  paymentDate: string
  operator: string
  remark: string
}

const loading = ref(false)
const tableData = ref<PaymentRecord[]>([])
const detailVisible = ref(false)
const currentRecord = ref<PaymentRecord | null>(null)
const tableRef = ref()

const queryParams = reactive({
  customerName: '',
  dateRange: [] as string[],
  paymentMethod: undefined as string | undefined
})

const stats = reactive({
  totalAmount: 856000,
  todayAmount: 42000,
  totalCount: 156
})

const avgAmount = computed(() => {
  if (stats.totalCount === 0) return 0
  return Math.round(stats.totalAmount / stats.totalCount)
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})



const vxeColumns = computed(() => [
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'orderNo', title: '订单号', width: 150 },
  { field: 'amount', title: '收款金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'paymentMethod', title: '收款方式', width: 100, slotName: 'paymentMethodCell' },
  { field: 'paymentDate', title: '收款日期', width: 120 },
  { field: 'operator', title: '操作人', width: 100 },
  { field: 'remark', title: '备注', width: 150 },
  { field: 'action', title: '操作', width: 150, fixed: 'right', type: 'action' },
])

const getPaymentMethodText = (method: string) => {
  const methods: Record<string, string> = {
    cash: '现金',
    bank: '银行转账',
    wechat: '微信支付',
    alipay: '支付宝'
  }
  return methods[method] || method
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.customerName = ''
  queryParams.dateRange = []
  queryParams.paymentMethod = undefined
  handleSearch()
}

const handleAdd = () => {
  message.info('打开新增收款表单')
}

const handleView = (record: PaymentRecord) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handlePrint = (record: PaymentRecord) => {
  message.info(`打印收款单: ${record.customerName}`)
}

const handleExport = () => {
  message.info('导出收款记录')
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/finance/payment-record/page', {
      params: {
        ...queryParams,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.data?.records) {
      tableData.value = res.data.records
      pagination.total = res.data.total || 0
      stats.totalAmount = res.data.totalAmount ?? tableData.value.reduce((sum, item) => sum + item.amount, 0)
      stats.todayAmount = res.data.todayAmount ?? 0
      stats.totalCount = res.data.totalCount ?? pagination.total
    } else {
      tableData.value = []
      pagination.total = 0
      stats.totalAmount = 0
      stats.todayAmount = 0
      stats.totalCount = 0
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

fetchData()
</script>

<style scoped>
.payment-record-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
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
.stat-today { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-count { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-avg { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

.search-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.action-area {
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
}

.table-area {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  flex: 1;
  overflow: hidden;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
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
