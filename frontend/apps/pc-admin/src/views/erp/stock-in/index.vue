<template>
  <div class="stock-in-page">
    <a-card title="入库管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form
          layout="inline"
          :model="queryParams"
        >
          <a-form-item label="入库单号">
            <a-input
              v-model:value="queryParams.inboundNo"
              placeholder="请输入入库单号"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="采购订单">
            <a-input
              v-model:value="queryParams.purchaseOrderNo"
              placeholder="请输入采购订单号"
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
            新建入库单
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
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
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
                @click="handleInbound(record)"
              >
                入库
              </a-button>
              <PrintButton 
                v-if="record.status >= 2"
                templateType="stock_in"
                :businessId="record.id"
                businessType="stock_in"
                buttonText="打印"
                buttonSize="small"
                @print-success="handlePrintSuccess(record)"
                @print-error="handlePrintError"
              />
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailVisible"
      title="入库单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="入库单号">{{ currentRecord.inboundNo }}</a-descriptions-item>
        <a-descriptions-item label="采购订单">{{ currentRecord.purchaseOrderNo }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="入库金额">¥{{ currentRecord.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="入库日期">{{ currentRecord.inboundDate }}</a-descriptions-item>
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
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'

interface StockIn {
  id: number
  inboundNo: string
  purchaseOrderNo: string
  supplierName: string
  warehouseName: string
  totalAmount: number
  status: number
  inboundDate: string
  operator: string
}

const loading = ref(false)
const dataSource = ref<StockIn[]>([])
const detailVisible = ref(false)
const currentRecord = ref<StockIn | null>(null)

const queryParams = reactive({
  inboundNo: '',
  purchaseOrderNo: '',
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
    title: '入库单号',
    dataIndex: 'inboundNo',
    key: 'inboundNo',
    width: 150
  },
  {
    title: '采购订单',
    dataIndex: 'purchaseOrderNo',
    key: 'purchaseOrderNo',
    width: 150
  },
  {
    title: '供应商',
    dataIndex: 'supplierName',
    key: 'supplierName',
    width: 150
  },
  {
    title: '仓库',
    dataIndex: 'warehouseName',
    key: 'warehouseName',
    width: 120
  },
  {
    title: '入库金额',
    key: 'totalAmount',
    width: 120
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '入库日期',
    dataIndex: 'inboundDate',
    key: 'inboundDate',
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
    2: 'success'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待审核',
    1: '已审核',
    2: '已入库'
  }
  return texts[status] || '未知'
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.inboundNo = ''
  queryParams.purchaseOrderNo = ''
  queryParams.status = undefined
  handleSearch()
}

const handleCreate = () => {
  message.info('打开新建入库单表单')
}

const handleView = (record: StockIn) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleApprove = (record: StockIn) => {
  message.success(`审核入库单: ${record.inboundNo}`)
}

const handleInbound = (record: StockIn) => {
  message.success(`入库完成: ${record.inboundNo}`)
}

const handlePrintSuccess = (record: StockIn) => {
  message.success(`入库单 ${record.inboundNo} 打印成功`)
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

const handleExport = () => {
  message.info('导出入库单')
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/erp/purchase/inbound/page', {
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
.stock-in-page {
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}
</style>