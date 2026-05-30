<template>
  <div class="batch-list-container">
    <!-- 搜索和筛选区域 -->
    <div class="batch-search-area">
      <a-row :gutter="20" class="mb-4">
        <a-col :span="6">
          <a-input
            v-model:value="searchParams.batchNo"
            placeholder="批次号"
            allow-clear
            @clear="handleSearch"
            @press-enter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="6">
          <a-input
            v-model:value="searchParams.productName"
            placeholder="产品名称"
            allow-clear
            @clear="handleSearch"
            @press-enter="handleSearch"
          >
            <template #prefix>
              <AppstoreOutlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="6">
          <a-range-picker
            v-model:value="searchParams.dateRange"
            :placeholder="['开始日期', '结束日期']"
            @change="handleSearch"
            style="width: 100%"
          />
        </a-col>
        <a-col :span="6" class="flex items-center">
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>搜索
          </a-button>
          <a-button @click="handleReset" style="margin-left: 8px">
            <template #icon><ReloadOutlined /></template>重置
          </a-button>
          <a-button @click="handleExport" style="margin-left: 8px">
            <template #icon><DownloadOutlined /></template>导出
          </a-button>
        </a-col>
      </a-row>
    </div>

    <!-- 批次列表表格 -->
    <div class="batch-table-area">
      <a-table
        :columns="columns"
        :data-source="batchList"
        :loading="loading"
        :row-selection="rowSelection"
        :pagination="tablePagination"
        row-key="id"
        bordered
        @change="handleTableChange"
        @row-click="handleRowClick"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'batchNo'">
            <a-tag>{{ record.batchNo }}</a-tag>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <span class="font-medium">{{ formatNumber(record.quantity) }}</span>
          </template>
          <template v-else-if="column.key === 'productionDate'">
            {{ formatDate(record.productionDate) }}
          </template>
          <template v-else-if="column.key === 'expiryDate'">
            <span :class="{ 'text-red-500 font-medium': isExpiring(record.expiryDate) }">
              {{ formatDate(record.expiryDate) }}
            </span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click.stop="handleViewDetail(record)">
              <template #icon><EyeOutlined /></template>详情
            </a-button>
            <a-button type="link" size="small" @click.stop="handleEdit(record)">
              <template #icon><EditOutlined /></template>编辑
            </a-button>
            <a-button type="link" danger size="small" @click.stop="handleDelete(record)">
              <template #icon><DeleteOutlined /></template>删除
            </a-button>
          </template>
        </template>
      </a-table>

      <!-- 批量操作 & 分页已集成在 a-table 中 -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined,
  AppstoreOutlined,
  ReloadOutlined,
  DownloadOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import type { BatchItem, BatchStatus, SearchParams } from './types'
import { formatDate, formatNumber } from '@/utils/formatters'

// 响应式数据
const loading = ref(false)
const batchList = ref<BatchItem[]>([])
const selectedRowKeys = ref<(string | number)[]>([])

// 搜索参数
const searchParams = reactive<SearchParams>({
  batchNo: '',
  productName: '',
  dateRange: [],
  status: '',
  warehouse: ''
})

// 分页参数
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 计算属性
const selectionCount = computed(() => selectedRowKeys.value.length)

// 表格列配置
const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 180, sorter: true },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 180 },
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 120, align: 'right' as const },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 80 },
  { title: '生产日期', dataIndex: 'productionDate', key: 'productionDate', width: 140, sorter: true },
  { title: '有效期至', dataIndex: 'expiryDate', key: 'expiryDate', width: 140, sorter: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '仓库', dataIndex: 'warehouse', key: 'warehouse', width: 150 },
  { title: '库位', dataIndex: 'location', key: 'location', width: 120 },
  { title: '供应商', dataIndex: 'supplier', key: 'supplier', width: 150 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[], rows: BatchItem[]) => {
    selectedRowKeys.value = keys
  }
}))

const tablePagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
  pageSizeOptions: ['10', '20', '50', '100']
}))

// 状态映射
const statusMap: Record<BatchStatus, { text: string; color: string }> = {
  'normal': { text: '正常', color: 'green' },
  'warning': { text: '预警', color: 'orange' },
  'expired': { text: '过期', color: 'red' },
  'locked': { text: '锁定', color: 'blue' },
  'out_of_stock': { text: '缺货', color: 'default' }
}

// 搜索
const handleSearch = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/v1/erp-batch-sn/batches/page', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        batchNo: searchParams.batchNo,
        productName: searchParams.productName,
        startDate: searchParams.dateRange?.[0],
        endDate: searchParams.dateRange?.[1],
        status: searchParams.status,
        warehouse: searchParams.warehouse
      })
    })
    const data = await response.json()
    if (data.code === 200) {
      batchList.value = data.data.records || []
      pagination.total = data.data.total || 0
    } else {
      batchList.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('搜索批次列表失败:', error)
    batchList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchParams.batchNo = ''
  searchParams.productName = ''
  searchParams.dateRange = []
  searchParams.status = ''
  searchParams.warehouse = ''
  pagination.current = 1
  handleSearch()
}

const handleExport = () => {
  // TODO: 实现导出批次数据功能
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  handleSearch()
}

const handleRowClick = (record: BatchItem) => {
  emit('row-click', record)
}

const handleViewDetail = (row: BatchItem) => {
  emit('view-detail', row)
}

const handleEdit = (row: BatchItem) => {
  emit('edit', row)
}

const handleDelete = (row: BatchItem) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除批次 ${row.batchNo} 吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      message.success('删除成功')
      handleSearch()
    },
    onCancel: () => { /* noop */ }
  })
}

const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的批次')
    return
  }

  const selectedCount = selectedRowKeys.value.length
  Modal.confirm({
    title: '确认批量删除',
    content: `确定删除选中的 ${selectedCount} 个批次吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    onOk: async () => {
      message.success('批量删除成功')
      selectedRowKeys.value = []
      handleSearch()
    }
  })
}

const getStatusColor = (status: BatchStatus) => {
  return statusMap[status]?.color || 'default'
}

const getStatusText = (status: BatchStatus) => {
  return statusMap[status]?.text || status
}

const isExpiring = (expiryDate: string) => {
  const date = new Date(expiryDate)
  const now = new Date()
  const diffDays = Math.floor((date.getTime() - now.getTime()) / (1000 * 3600 * 24))
  return diffDays <= 30 && diffDays > 0
}

// 事件定义
const emit = defineEmits<{
  'row-click': [row: BatchItem]
  'view-detail': [row: BatchItem]
  'edit': [row: BatchItem]
}>()

// 生命周期
onMounted(() => {
  handleSearch()
})
</script>

<style scoped lang="scss">
.batch-list-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;

  .batch-search-area {
    margin-bottom: 20px;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 6px;

    .flex {
      display: flex;
      align-items: center;
    }
  }

  .batch-table-area {
    .selected-info {
      color: #666;
      font-size: 14px;
    }
  }
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-4 {
  margin-top: 16px;
}

.ml-2 {
  margin-left: 8px;
}

.mr-1 {
  margin-right: 4px;
}

.text-red-500 {
  color: #f56c6c;
}

.font-medium {
  font-weight: 500;
}
</style>
