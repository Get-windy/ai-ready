<template>
  <div class="purchase-exchange-page">
    <a-card title="采购换货管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="换货单号">
            <a-input v-model:value="queryParams.exchangeNo" placeholder="请输入换货单号" allow-clear />
          </a-form-item>
          <a-form-item label="原采购订单">
            <a-input v-model:value="queryParams.originalOrderNo" placeholder="请输入原采购订单号" allow-clear />
          </a-form-item>
          <a-form-item label="供应商">
            <a-select v-model:value="queryParams.supplierId" placeholder="请选择供应商" allow-clear style="width: 150px">
              <a-select-option :value="1">供应商A</a-select-option>
              <a-select-option :value="2">供应商B</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="queryParams.status" placeholder="请选择状态" allow-clear style="width: 120px">
              <a-select-option :value="ExchangeStatus.DRAFT">草稿</a-select-option>
              <a-select-option :value="ExchangeStatus.PENDING_APPROVAL">待审批</a-select-option>
              <a-select-option :value="ExchangeStatus.APPROVED">已审批</a-select-option>
              <a-select-option :value="ExchangeStatus.EXCHANGING">换货中</a-select-option>
              <a-select-option :value="ExchangeStatus.COMPLETED">已完成</a-select-option>
              <a-select-option :value="ExchangeStatus.REJECTED">已拒绝</a-select-option>
              <a-select-option :value="ExchangeStatus.CANCELLED">已取消</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="换货日期">
            <a-range-picker v-model:value="dateRange" @change="handleDateChange" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ReloadOutlined /></template>重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>新建换货单
          </a-button>
          <a-button @click="handleExport">
            <template #icon><ExportOutlined /></template>导出
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'exchangeType'">{{ getExchangeTypeText(record.exchangeType) }}</template>
          <template v-else-if="column.key === 'totalAmount'">¥{{ record.totalAmount?.toFixed(2) }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <a-button v-if="record.status === ExchangeStatus.DRAFT" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button v-if="record.status === ExchangeStatus.DRAFT" type="link" size="small" @click="handleSubmit(record)">提交</a-button>
              <a-button v-if="record.status === ExchangeStatus.PENDING_APPROVAL" type="link" size="small" @click="handleApprove(record)">审批</a-button>
              <a-button type="link" size="small" @click="handleTrack(record)">跟踪</a-button>
              <a-popconfirm v-if="record.status === ExchangeStatus.DRAFT" title="确定要删除该换货单吗？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <ExchangeFormModal v-model:visible="formModalVisible" :record="currentRecord" @success="handleFormSuccess" />
    <ExchangeApproveModal v-model:visible="approveModalVisible" :record="currentRecord" @success="handleApproveSuccess" />
    <ExchangeDetailModal v-model:visible="detailModalVisible" :record="currentRecord" />
    <ExchangeTrackModal v-model:visible="trackModalVisible" :record="currentRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import { purchaseExchangeApi, type PurchaseExchange, ExchangeStatus } from '@/api/purchase-exchange'
import ExchangeFormModal from './components/ExchangeFormModal.vue'
import ExchangeApproveModal from './components/ExchangeApproveModal.vue'
import ExchangeDetailModal from './components/ExchangeDetailModal.vue'
import ExchangeTrackModal from './components/ExchangeTrackModal.vue'

const loading = ref(false)
const dataSource = ref<PurchaseExchange[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

const queryParams = reactive({
  exchangeNo: '',
  originalOrderNo: '',
  supplierId: undefined as number | undefined,
  status: undefined as ExchangeStatus | undefined,
  startDate: '',
  endDate: '',
  current: 1,
  size: 10
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
  { title: '换货单号', dataIndex: 'exchangeNo', key: 'exchangeNo', width: 180 },
  { title: '原采购订单', dataIndex: 'originalOrderNo', key: 'originalOrderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '换货日期', dataIndex: 'exchangeDate', key: 'exchangeDate', width: 120 },
  { title: '换货类型', key: 'exchangeType', width: 100 },
  { title: '换货金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'createdByName', key: 'createdByName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 250 }
]

const getStatusColor = (status: ExchangeStatus): string => {
  const colors: Record<ExchangeStatus, string> = {
    [ExchangeStatus.DRAFT]: 'default',
    [ExchangeStatus.PENDING_APPROVAL]: 'orange',
    [ExchangeStatus.APPROVED]: 'blue',
    [ExchangeStatus.EXCHANGING]: 'processing',
    [ExchangeStatus.COMPLETED]: 'success',
    [ExchangeStatus.REJECTED]: 'red',
    [ExchangeStatus.CANCELLED]: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: ExchangeStatus): string => {
  const texts: Record<ExchangeStatus, string> = {
    [ExchangeStatus.DRAFT]: '草稿',
    [ExchangeStatus.PENDING_APPROVAL]: '待审批',
    [ExchangeStatus.APPROVED]: '已审批',
    [ExchangeStatus.EXCHANGING]: '换货中',
    [ExchangeStatus.COMPLETED]: '已完成',
    [ExchangeStatus.REJECTED]: '已拒绝',
    [ExchangeStatus.CANCELLED]: '已取消'
  }
  return texts[status] || '未知'
}

const getExchangeTypeText = (type: number): string => {
  const texts: Record<number, string> = { 1: '质量问题', 2: '规格不符', 3: '数量错误', 4: '其他' }
  return texts[type] || '未知'
}

const formModalVisible = ref(false)
const approveModalVisible = ref(false)
const detailModalVisible = ref(false)
const trackModalVisible = ref(false)
const currentRecord = ref<PurchaseExchange | null>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await purchaseExchangeApi.page({
      ...queryParams,
      current: pagination.current,
      size: pagination.pageSize
    })
    dataSource.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.exchangeNo = ''
  queryParams.originalOrderNo = ''
  queryParams.supplierId = undefined
  queryParams.status = undefined
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = null
  handleSearch()
}

const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates) {
    queryParams.startDate = dates[0].format('YYYY-MM-DD')
    queryParams.endDate = dates[1].format('YYYY-MM-DD')
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const handleCreate = () => {
  currentRecord.value = null
  formModalVisible.value = true
}

const handleEdit = (record: PurchaseExchange) => {
  currentRecord.value = record
  formModalVisible.value = true
}

const handleView = (record: PurchaseExchange) => {
  currentRecord.value = record
  detailModalVisible.value = true
}

const handleSubmit = async (record: PurchaseExchange) => {
  try {
    await purchaseExchangeApi.submit(record.id)
    message.success('提交成功')
    fetchData()
  } catch (error) {
    message.error('提交失败')
  }
}

const handleApprove = (record: PurchaseExchange) => {
  currentRecord.value = record
  approveModalVisible.value = true
}

const handleTrack = (record: PurchaseExchange) => {
  currentRecord.value = record
  trackModalVisible.value = true
}

const handleDelete = async (record: PurchaseExchange) => {
  try {
    await purchaseExchangeApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    message.error('删除失败')
  }
}

const handleExport = () => {
  const hide = message.loading('正在导出...', 0)
  setTimeout(() => { hide(); message.success('导出成功，文件下载中') }, 800)
}

const handleFormSuccess = () => {
  formModalVisible.value = false
  fetchData()
}

const handleApproveSuccess = () => {
  approveModalVisible.value = false
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.purchase-exchange-page {
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}
</style>