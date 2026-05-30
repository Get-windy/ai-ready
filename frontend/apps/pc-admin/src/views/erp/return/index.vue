<template>
  <div class="return-page">
    <a-card title="退货管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form
          layout="inline"
          :model="queryParams"
        >
          <a-form-item label="退货单号">
            <a-input
              v-model:value="queryParams.returnNo"
              placeholder="请输入退货单号"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="销售订单">
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
                待审核
              </a-select-option>
              <a-select-option :value="1">
                已审核
              </a-select-option>
              <a-select-option :value="2">
                已入库
              </a-select-option>
              <a-select-option :value="3">
                已退款
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ReloadOutlined />
                </template>
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
            @click="handleCreate"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新建退货申请
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
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'returnAmount'">
            ¥{{ record.returnAmount?.toFixed(2) }}
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
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleApprove(record)"
              >
                审核
              </a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                @click="handleReceive(record)"
              >
                入库
              </a-button>
              <a-button
                v-if="record.status === 2"
                type="link"
                size="small"
                @click="handleRefund(record)"
              >
                退款
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailVisible"
      title="退货单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
        <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ currentRecord.customerName }}</a-descriptions-item>
        <a-descriptions-item label="退货金额">¥{{ currentRecord.returnAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.returnReason }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div style="text-align: right; margin-top: 16px">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { saleReturnApi, type SaleReturn } from '@/api/erp'
import request from '@/utils/request'

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
}

const loading = ref(false)
const dataSource = ref<ReturnOrder[]>([])
const detailVisible = ref(false)
const currentRecord = ref<ReturnOrder | null>(null)

const queryParams = reactive({
  returnNo: '',
  orderNo: '',
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
    title: '退货单号',
    dataIndex: 'returnNo',
    key: 'returnNo',
    width: 150
  },
  {
    title: '销售订单',
    dataIndex: 'orderNo',
    key: 'orderNo',
    width: 150
  },
  {
    title: '客户名称',
    dataIndex: 'customerName',
    key: 'customerName',
    width: 150
  },
  {
    title: '退货金额',
    key: 'returnAmount',
    width: 120
  },
  {
    title: '退货原因',
    dataIndex: 'returnReason',
    key: 'returnReason',
    ellipsis: true
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '退货日期',
    dataIndex: 'returnDate',
    key: 'returnDate',
    width: 120
  },
  {
    title: '操作人',
    dataIndex: 'operator',
    key: 'operator',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'warning',
    3: 'success'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待审核',
    1: '已审核',
    2: '已入库',
    3: '已退款'
  }
  return texts[status] || '未知'
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.returnNo = ''
  queryParams.orderNo = ''
  queryParams.status = undefined
  handleSearch()
}

const handleCreate = () => {
  message.info('打开新建退货申请表单')
}

const handleView = (record: ReturnOrder) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleApprove = (record: ReturnOrder) => {
  message.success(`审核退货单: ${record.returnNo}`)
}

const handleReceive = (record: ReturnOrder) => {
  message.success(`入库完成: ${record.returnNo}`)
}

const handleRefund = (record: ReturnOrder) => {
  message.success(`退款完成: ${record.returnNo}`)
}

const handleExport = () => {
  message.info('导出退货单')
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/erp/sale/return/page', {
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
.return-page {
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}
</style>