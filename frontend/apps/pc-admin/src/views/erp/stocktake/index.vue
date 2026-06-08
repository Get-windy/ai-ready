<template>
  <div class="stocktake-page" style="padding: 16px; height: 100%; display: flex; flex-direction: column;">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">盘点单总数</div>
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
            <div class="summary-title">待审核</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <FormOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">盘点中</div>
            <div class="summary-value">{{ statistics.processingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statistics.completedCount }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新建盘点单"
      style="flex: 1;"
      @add="handleCreate"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的盘点单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无盘点单数据，点击右上角「新建盘点单」开始创建
          </p>
        </div>
      </template>

      <template #action="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else>
          <a-space>
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="开始盘点">
              <a-button type="link" size="small" @click="handleStart(record)">
                <template #icon><FormOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="完成盘点">
              <a-button type="link" size="small" @click="handleComplete(record)">
                <template #icon><CheckOutlined /></template>
              </a-button>
            </a-tooltip>
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
    </VxeTableList>

    <a-modal
      v-model:open="detailVisible"
      title="盘点单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="盘点单号">{{ currentRecord.stocktakeNo }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="盘点日期">{{ currentRecord.stocktakeDate }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <StatusTag :status="currentRecord.status" :map="STOCKTAKE_STATUS_ORDER" />
        </a-descriptions-item>
        <a-descriptions-item label="系统数量">{{ currentRecord.systemQuantity }}</a-descriptions-item>
        <a-descriptions-item label="实际数量">{{ currentRecord.actualQuantity }}</a-descriptions-item>
        <a-descriptions-item label="差异">
          <span :class="{ 'positive': currentRecord.difference > 0, 'negative': currentRecord.difference < 0 }">
            {{ currentRecord.difference > 0 ? '+' : '' }}{{ currentRecord.difference }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
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
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { STOCKTAKE_STATUS_ORDER } from '@/utils/statusConfig'
import { message, Modal } from 'ant-design-vue'
import { stockCheckApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import {
  EyeOutlined,
  FormOutlined,
  CheckOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  SearchOutlined,
  InboxOutlined,
  FileTextOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'

interface Stocktake {
  id: number
  stocktakeNo: string
  warehouseName: string
  stocktakeDate: string
  systemQuantity: number
  actualQuantity: number
  difference: number
  status: number
  operator: string
  remark?: string
}

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref<Stocktake[]>([])
const detailVisible = ref(false)
const currentRecord = ref<Stocktake | null>(null)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())
const selectedRows = ref<Stocktake[]>([])
const selectedIds = ref<number[]>([])

// 统计数据
const statistics = ref({
  totalCount: 0,
  pendingCount: 0,
  processingCount: 0,
  completedCount: 0
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
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

const vxeColumns = computed(() => [
  { field: 'stocktakeNo', title: '盘点单号', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'stocktakeDate', title: '盘点日期', width: 120 },
  { field: 'systemQuantity', title: '系统数量', width: 100, align: 'right' },
  { field: 'actualQuantity', title: '实际数量', width: 100, align: 'right' },
  { field: 'difference', title: '差异', width: 100, align: 'right', formatter: ({ cellValue }) => {
    const prefix = cellValue > 0 ? '+' : ''
    return `${prefix}${cellValue}`
  }},
  { field: 'status', title: '状态', width: 100, align: 'center', formatter: ({ cellValue }) => STOCKTAKE_STATUS_ORDER[cellValue]?.text || '' },
  { field: 'operator', title: '操作人', width: 100 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' },
])

const filterFields = [
  { key: 'stocktakeNo', label: '盘点单号', type: 'input' as const, placeholder: '请输入盘点单号' },
  { key: 'warehouseId', label: '仓库', type: 'select' as const, options: [
    { label: '主仓库', value: 1 },
    { label: '分仓库', value: 2 },
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '盘点中', value: 1 },
    { label: '已完成', value: 2 },
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
  message.info('打开新建盘点单表单')
}

const handleView = (record: Stocktake) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleStart = (record: Stocktake) => {
  message.success(`开始盘点: ${record.stocktakeNo}`)
}

const handleComplete = (record: Stocktake) => {
  message.success(`盘点完成: ${record.stocktakeNo}`)
}

const handleDelete = async (record: Stocktake) => {
  try {
    await request.delete(`/erp/stockCheck/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: Stocktake) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该盘点单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleSelectionChange = (rows: Stocktake[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
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
    const res = await stockCheckApi.page({
      tenantId: userStore.tenantId,
      keyword: searchFilters.stocktakeNo || undefined,
      status: searchFilters.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data?.records) {
      dataSource.value = res.data.records.map((item) => ({
        id: item.id,
        stocktakeNo: item.checkNo,
        warehouseName: item.warehouseName,
        stocktakeDate: item.checkDate,
        systemQuantity: item.systemQuantity || 0,
        actualQuantity: item.actualQuantity || 0,
        difference: (item.actualQuantity || 0) - (item.systemQuantity || 0),
        status: item.status,
        operator: item.creatorName || '',
        remark: item.remark || ''
      }))
      pagination.total = res.data.total || 0
      // 更新统计
      statistics.value.totalCount = dataSource.value.length
      statistics.value.pendingCount = dataSource.value.filter(item => item.status === 0).length
      statistics.value.processingCount = dataSource.value.filter(item => item.status === 1).length
      statistics.value.completedCount = dataSource.value.filter(item => item.status === 2).length
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
.stocktake-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
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

.positive {
  color: #3f8600;
  font-weight: bold;
}

.negative {
  color: #ff4d4f;
  font-weight: bold;
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
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7e6 100%);
  border: 1px solid #b7eb8f;
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
