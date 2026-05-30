<template>
  <div class="payment-record-page">
    <a-card title="收款记录">
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

      <!-- 统计卡片 -->
      <a-row
        :gutter="16"
        class="stats-area"
      >
        <a-col :span="8">
          <a-statistic
            title="收款总额"
            :value="stats.totalAmount"
            :precision="2"
            prefix="¥"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="今日收款"
            :value="stats.todayAmount"
            :precision="2"
            prefix="¥"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="收款笔数"
            :value="totalCount"
            suffix="笔"
          />
        </a-col>
      </a-row>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'paymentMethod'">
            <a-tag>{{ getPaymentMethodText(record.paymentMethod) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
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
        </template>
      </a-table>
    </a-card>

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
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
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
const dataSource = ref<PaymentRecord[]>([])
const detailVisible = ref(false)
const currentRecord = ref<PaymentRecord | null>(null)

const queryParams = reactive({
  customerName: '',
  dateRange: [] as string[],
  paymentMethod: undefined as string | undefined
})

const stats = reactive({
  totalAmount: 0,
  todayAmount: 0
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
    title: '收款金额',
    key: 'amount',
    width: 120
  },
  {
    title: '收款方式',
    key: 'paymentMethod',
    width: 100
  },
  {
    title: '收款日期',
    dataIndex: 'paymentDate',
    key: 'paymentDate',
    width: 120
  },
  {
    title: '操作人',
    dataIndex: 'operator',
    key: 'operator',
    width: 100
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
    fixed: 'right'
  }
]

const getPaymentMethodText = (method: string) => {
  const methods: Record<string, string> = {
    cash: '现金',
    bank: '银行转账',
    wechat: '微信支付',
    alipay: '支付宝'
  }
  return methods[method] || method
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

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
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
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
      stats.totalAmount = res.data.totalAmount ?? dataSource.value.reduce((sum, item) => sum + item.amount, 0)
      stats.todayAmount = res.data.todayAmount ?? 0
    } else {
      dataSource.value = []
      pagination.total = 0
      stats.totalAmount = 0
      stats.todayAmount = 0
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
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

.stats-area {
  margin-bottom: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}
</style>