<template>
  <div class="accounts-payable-page">
    <a-card title="应付账款管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form
          layout="inline"
          :model="queryParams"
        >
          <a-form-item label="供应商名称">
            <a-input
              v-model:value="queryParams.supplierName"
              placeholder="请输入供应商名称"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="订单号">
            <a-input
              v-model:value="queryParams.orderNo"
              placeholder="请输入订单号"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="queryParams.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="0">
                未付款
              </a-select-option>
              <a-select-option :value="1">
                部分付款
              </a-select-option>
              <a-select-option :value="2">
                已付款
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
            新增应付
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
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'paidAmount'">
            ¥{{ record.paidAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'unpaidAmount'">
            ¥{{ record.unpaidAmount?.toFixed(2) }}
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
                v-if="record.status !== 2"
                type="link"
                size="small"
                @click="handlePayment(record)"
              >
                付款
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
    width: 120
  },
  {
    title: '已付金额',
    key: 'paidAmount',
    width: 120
  },
  {
    title: '未付金额',
    key: 'unpaidAmount',
    width: 120
  },
  {
    title: '状态',
    key: 'status',
    width: 100
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
    fixed: 'right'
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

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/finance/accounts-payable/page', {
      params: {
        ...queryParams,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
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

fetchData()
</script>

<style scoped>
.accounts-payable-page {
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