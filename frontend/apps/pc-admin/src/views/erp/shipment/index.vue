<template>
  <div class="shipment-page">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'erp-shipment-list'"
      :filter-fields="filterFields"
      :show-export="true"
      add-text="新建出库单"
      @add="handleCreate"
      @refresh="fetchData"
      @export="handleExport"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的出库单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无出库单数据，点击右上角「新建出库单」开始创建
          </p>
        </div>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusTag :status="record.status" :map="SHIPMENT_STATUS" />
        </template>
        <template v-else-if="column.key === 'totalAmount'">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="审核">
              <a-button type="link" size="small" @click="handleApprove(record)">
                <template #icon><CheckCircleOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="出库">
              <a-button type="link" size="small" @click="handleShip(record)">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              v-if="record.status >= 2"
              templateType="stock_out"
              :businessId="record.id"
              businessType="shipment"
              buttonText="打印"
              buttonSize="small"
              @print-success="handlePrintSuccess(record)"
              @print-error="handlePrintError"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="delete">
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </TableList>

    <a-modal
      v-model:open="detailVisible"
      title="出库单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="出库单号">{{ currentRecord.shipmentNo }}</a-descriptions-item>
        <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ currentRecord.customerName }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="出库金额">¥{{ currentRecord.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :status="currentRecord.status" :map="SHIPMENT_STATUS" />
        </a-descriptions-item>
        <a-descriptions-item label="出库日期">{{ currentRecord.shipmentDate }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
        <a-descriptions-item label="物流单号">{{ currentRecord.trackingNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div class="detail-modal-footer">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { SHIPMENT_STATUS } from '@/utils/statusConfig'
import { message, Modal } from 'ant-design-vue'
import { outboundApi } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import {
  EyeOutlined,
  CheckCircleOutlined,
  ExportOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  SearchOutlined,
  InboxOutlined
} from '@ant-design/icons-vue'

interface Shipment {
  id: number
  shipmentNo: string
  orderNo: string
  customerName: string
  warehouseName: string
  totalAmount: number
  status: number
  shipmentDate: string
  operator: string
  trackingNo?: string
  remark?: string
}

const loading = ref(false)
const dataSource = ref<Shipment[]>([])
const detailVisible = ref(false)
const currentRecord = ref<Shipment | null>(null)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  {
    title: '出库单号',
    dataIndex: 'shipmentNo',
    key: 'shipmentNo',
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
    title: '仓库',
    dataIndex: 'warehouseName',
    key: 'warehouseName',
    width: 120
  },
  {
    title: '出库金额',
    key: 'totalAmount',
    width: 120
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '出库日期',
    dataIndex: 'shipmentDate',
    key: 'shipmentDate',
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

const filterFields = [
  { key: 'shipmentNo', label: '出库单号', type: 'input' as const, placeholder: '请输入出库单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '请输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '已审核', value: 1 },
    { label: '已出库', value: 2 },
  ]},
]

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

const handleResetFilters = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
}

const handleCreate = () => {
  message.info('打开新建出库单表单')
}

const handleView = (record: Shipment) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleApprove = (record: Shipment) => {
  message.success(`审核出库单: ${record.shipmentNo}`)
}

const handleShip = (record: Shipment) => {
  message.success(`出库完成: ${record.shipmentNo}`)
}

const handlePrintSuccess = (record: Shipment) => {
  message.success(`出库单 ${record.shipmentNo} 打印成功`)
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

const handleDelete = async (record: Shipment) => {
  try {
    await request.delete(`/erp/sale/outbound/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: Shipment) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该出库单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

const handleExport = () => {
  message.info('导出出库单')
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
}

window.addEventListener('keydown', handleKeydown)
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      ...searchFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res = await request.get('/erp/sale/outbound/page', { params })
    if (res.data?.records) {
      dataSource.value = res.data.records
      pagination.total = res.data.total || 0
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdated.value = new Date().toISOString()
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

fetchData()
</script>

<style scoped>
.shipment-page {
  padding: 24px;
}

.list-update-timestamp {
  color: #999;
  font-size: 12px;
  margin-right: 12px;
}

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

.action-more-btn {
  padding: 0 4px;
}

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
}
</style>
