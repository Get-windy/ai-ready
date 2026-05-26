<template>
  <div class="batch-list-container">
    <!-- 搜索和筛选区域 -->
    <div class="batch-search-area">
      <el-row :gutter="20" class="mb-4">
        <el-col :span="6">
          <el-input
            v-model="searchParams.batchNo"
            placeholder="批次号"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-input
            v-model="searchParams.productName"
            placeholder="产品名称"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Goods /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-date-picker
            v-model="searchParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleSearch"
            style="width: 100%"
          />
        </el-col>
        <el-col :span="6" class="flex items-center">
          <el-button type="primary" @click="handleSearch">
            <el-icon class="mr-1"><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon class="mr-1"><Refresh /></el-icon>重置
          </el-button>
          <el-button type="success" @click="handleExport">
            <el-icon class="mr-1"><Download /></el-icon>导出
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 批次列表表格 -->
    <div class="batch-table-area">
      <el-table
        :data="batchList"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
        highlight-current-row
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="batchNo" label="批次号" width="180" sortable>
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.batchNo }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" width="180" />
        <el-table-column prop="productCode" label="产品编码" width="150" />
        <el-table-column prop="quantity" label="数量" width="120" align="right">
          <template #default="{ row }">
            <span class="font-medium">{{ formatNumber(row.quantity) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="productionDate" label="生产日期" width="140" sortable>
          <template #default="{ row }">
            {{ formatDate(row.productionDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期至" width="140" sortable>
          <template #default="{ row }">
            <span :class="{'text-red-500 font-medium': isExpiring(row.expiryDate)}">
              {{ formatDate(row.expiryDate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" filterable>
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouse" label="仓库" width="150" />
        <el-table-column prop="location" label="库位" width="120" />
        <el-table-column prop="supplier" label="供应商" width="150" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click.stop="handleViewDetail(row)"
              link
            >
              <el-icon><View /></el-icon>详情
            </el-button>
            <el-button
              type="warning"
              size="small"
              @click.stop="handleEdit(row)"
              link
            >
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click.stop="handleDelete(row)"
              link
            >
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="mt-4 flex justify-between items-center">
        <div class="selected-info">
          已选择 {{ selectionCount }} 项
          <el-button
            v-if="selectionCount > 0"
            type="danger"
            size="small"
            @click="handleBatchDelete"
            class="ml-2"
          >
            批量删除
          </el-button>
        </div>
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Search,
  Goods,
  Refresh,
  Download,
  View,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import type { BatchItem, BatchStatus, SearchParams } from './types'
import { formatDate, formatNumber } from '@/utils/formatters'

// 响应式数据
const loading = ref(false)
const batchList = ref<BatchItem[]>([])
const selectedRows = ref<BatchItem[]>([])

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
  currentPage: 1,
  pageSize: 20,
  total: 0
})

// 计算属性
const selectionCount = computed(() => selectedRows.value.length)

// 状态映射
const statusMap: Record<BatchStatus, { text: string; type: string }> = {
  'normal': { text: '正常', type: 'success' },
  'warning': { text: '预警', type: 'warning' },
  'expired': { text: '过期', type: 'danger' },
  'locked': { text: '锁定', type: 'info' },
  'out_of_stock': { text: '缺货', type: '' }
}

// 方法
const handleSearch = async () => {
  loading.value = true
  try {
    const response = await fetch('/api/v1/erp-batch-sn/batches/page', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pageNum: pagination.currentPage,
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
  handleSearch()
}

const handleExport = () => {
  // 导出逻辑
  console.log('导出批次数据')
}

const handleSelectionChange = (rows: BatchItem[]) => {
  selectedRows.value = rows
}

const handleRowClick = (row: BatchItem) => {
  console.log('点击行:', row)
  emit('row-click', row)
}

const handleViewDetail = (row: BatchItem) => {
  emit('view-detail', row)
}

const handleEdit = (row: BatchItem) => {
  emit('edit', row)
}

const handleDelete = async (row: BatchItem) => {
  try {
    // 确认删除
    await ElMessageBox.confirm(
      `确定删除批次 ${row.batchNo} 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 调用删除API
    // await batchApi.delete(row.id)
    ElMessage.success('删除成功')
    handleSearch()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要删除的批次')
    return
  }
  
  try {
    const batchNos = selectedRows.value.map(row => row.batchNo).join(', ')
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 个批次吗？\n${batchNos}`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 批量删除API调用
    // const ids = selectedRows.value.map(row => row.id)
    // await batchApi.batchDelete(ids)
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    handleSearch()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  handleSearch()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  handleSearch()
}

const getStatusType = (status: BatchStatus) => {
  return statusMap[status]?.type || ''
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

// 模拟数据生成
const generateMockData = (): BatchItem[] => {
  const products = ['维生素C片', '阿莫西林胶囊', '布洛芬缓释片', '头孢克肟片', '板蓝根颗粒']
  const warehouses = ['原料仓库', '成品仓库', '冷库', '危险品仓库']
  const suppliers = ['上海制药', '北京医药', '广州药业', '成都生物']
  
  return Array.from({ length: 20 }, (_, i) => ({
    id: i + 1,
    batchNo: `BATCH${String(i + 1001).padStart(4, '0')}`,
    productName: products[i % products.length],
    productCode: `PROD${String(i + 1000).padStart(4, '0')}`,
    quantity: Math.floor(Math.random() * 10000) + 1000,
    unit: '盒',
    productionDate: new Date(Date.now() - Math.random() * 365 * 24 * 3600 * 1000).toISOString(),
    expiryDate: new Date(Date.now() + Math.random() * 365 * 24 * 3600 * 1000).toISOString(),
    status: ['normal', 'warning', 'expired', 'locked', 'out_of_stock'][i % 5] as BatchStatus,
    warehouse: warehouses[i % warehouses.length],
    location: `A${Math.floor(i / 5) + 1}-${(i % 5) + 1}`,
    supplier: suppliers[i % suppliers.length],
    remark: `批次备注 ${i + 1}`
  }))
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

.el-table {
  :deep(.el-table__row:hover) {
    cursor: pointer;
    background-color: #f5f7fa;
  }
}
</style>