<template>
  <div class="collection-reminder-page">
    <a-card title="催收提醒设置">
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
          <a-form-item label="提醒状态">
            <a-select
              v-model:value="queryParams.status"
              placeholder="请选择"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="0">
                待提醒
              </a-select-option>
              <a-select-option :value="1">
                已提醒
              </a-select-option>
              <a-select-option :value="2">
                已收款
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
          <a-button @click="handleBatchRemind">
            <template #icon>
              <BellOutlined />
            </template>
            批量提醒
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
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'overdueDays'">
            <a-tag color="red">
              {{ record.overdueDays }}天
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'nextReminderDate'">
            <span :class="{ 'urgent': isUrgent(record.nextReminderDate) }">
              {{ record.nextReminderDate }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleRemind(record)"
              >
                发送提醒
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                设置
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { BellOutlined, ExportOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

interface CollectionReminder {
  id: number
  customerName: string
  orderNo: string
  amount: number
  overdueDays: number
  status: number
  lastReminderDate: string
  nextReminderDate: string
  reminderCount: number
}

const loading = ref(false)
const dataSource = ref<CollectionReminder[]>([])
const selectedRowKeys = ref<number[]>([])

const queryParams = reactive({
  customerName: '',
  status: undefined as number | undefined
})

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
    title: '应收金额',
    key: 'amount',
    width: 120
  },
  {
    title: '逾期天数',
    key: 'overdueDays',
    width: 100
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '上次提醒',
    dataIndex: 'lastReminderDate',
    key: 'lastReminderDate',
    width: 120
  },
  {
    title: '下次提醒',
    key: 'nextReminderDate',
    width: 120
  },
  {
    title: '提醒次数',
    dataIndex: 'reminderCount',
    key: 'reminderCount',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'orange',
    1: 'blue',
    2: 'green'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待提醒',
    1: '已提醒',
    2: '已收款'
  }
  return texts[status] || '未知'
}

const isUrgent = (date: string) => {
  const reminderDate = new Date(date)
  const today = new Date()
  const diffDays = Math.ceil((reminderDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  return diffDays <= 2
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.customerName = ''
  queryParams.status = undefined
  handleSearch()
}

const handleBatchRemind = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要提醒的客户')
    return
  }
  message.success(`已发送提醒给 ${selectedRowKeys.value.length} 个客户`)
}

const handleRemind = (record: CollectionReminder) => {
  message.success(`已发送提醒给 ${record.customerName}`)
}

const handleEdit = (record: CollectionReminder) => {
  message.info(`打开提醒设置: ${record.customerName}`)
}

const handleExport = () => {
  message.info('导出催收列表')
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/finance/collection-reminder/page', {
      params: {
        ...queryParams,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
    } else {
      dataSource.value = []
      pagination.total = 0
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
.collection-reminder-page {
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

.urgent {
  color: #ff4d4f;
  font-weight: bold;
}
</style>