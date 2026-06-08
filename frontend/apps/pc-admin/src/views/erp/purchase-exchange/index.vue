<template>
  <div class="purchase-exchange-page" style="padding: 16px; height: 100%; display: flex; flex-direction: column;">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">换货单总数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审批</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statistics.completedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">换货金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="采购换货管理" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
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
      <a-table :columns="columns" :data-source="tableDataSource" :loading="loading" :pagination="pagination" row-key="id" style="flex: 1; overflow: auto;" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-if="column.key === 'status'">
            <StatusTag :status="record.status" :map="RETURN_EXCHANGE_STATUS" />
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

    <ExchangeFormModal v-model:open="formModalVisible" :record="currentRecord" @success="handleFormSuccess" />
    <ExchangeApproveModal v-model:open="approveModalVisible" :record="currentRecord" @success="handleApproveSuccess" />
    <ExchangeDetailModal v-model:open="detailModalVisible" :record="currentRecord" />
    <ExchangeTrackModal v-model:open="trackModalVisible" :record="currentRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ReloadOutlined, ExportOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'
import { purchaseExchangeApi, type PurchaseExchange, ExchangeStatus } from '@/api/purchase-exchange'
import ExchangeFormModal from './components/ExchangeFormModal.vue'
import ExchangeApproveModal from './components/ExchangeApproveModal.vue'
import ExchangeDetailModal from './components/ExchangeDetailModal.vue'
import ExchangeTrackModal from './components/ExchangeTrackModal.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { RETURN_EXCHANGE_STATUS } from '@/utils/statusConfig'
import { getStatusText } from '@/utils/statusConfig'
import { exportCsv } from '@/utils/exportCsv'

const loading = ref(false)
const dataSource = ref<PurchaseExchange[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  completedCount: 0,
  totalAmount: 0
})

// 空行填充
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...dataSource.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

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
    // 更新统计
    statistics.value.totalCount = dataSource.value.length
    statistics.value.pendingCount = dataSource.value.filter(r => r.status === ExchangeStatus.PENDING_APPROVAL).length
    statistics.value.completedCount = dataSource.value.filter(r => r.status === ExchangeStatus.COMPLETED).length
    statistics.value.totalAmount = dataSource.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0)
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
  const headers = ['换货单号', '原采购订单', '供应商', '换货日期', '换货类型', '换货金额', '状态', '创建人', '创建时间']
  const rows = dataSource.value.map((row: PurchaseExchange) => [
    row.exchangeNo || '', row.originalOrderNo || '', row.supplierName || '', row.exchangeDate || '',
    getExchangeTypeText(row.exchangeType), row.totalAmount?.toFixed(2) || '',
    getStatusText(row.status), row.createdByName || '', row.createTime || ''
  ])
  exportCsv(headers, rows, '采购换货单')
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
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}

.empty-placeholder {
  color: transparent;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 空占位行 */
:deep(.ant-table-tbody > tr:not(.ant-table-row):has(.empty-placeholder) > td) {
  background: #fff !important;
  height: 40px !important;
}
</style>