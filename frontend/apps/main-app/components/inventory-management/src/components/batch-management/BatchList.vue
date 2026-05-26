<template>
  <div class="batch-list-container">
    <!-- 筛选工具栏 -->
    <div class="batch-list-toolbar">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="批次号">
          <el-input
            v-model="searchForm.batchNo"
            placeholder="请输入批次号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="产品">
          <el-select
            v-model="searchForm.productId"
            placeholder="请选择产品"
            clearable
            filterable
          >
            <el-option
              v-for="product in productOptions"
              :key="product.id"
              :label="product.name"
              :value="product.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
          >
            <el-option
              v-for="status in statusOptions"
              :key="status.value"
              :label="status.label"
              :value="status.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库">
          <el-select
            v-model="searchForm.warehouseId"
            placeholder="请选择仓库"
            clearable
            filterable
          >
            <el-option
              v-for="warehouse in warehouseOptions"
              :key="warehouse.id"
              :label="warehouse.name"
              :value="warehouse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建批次
        </el-button>
        <el-button @click="handleBatchActions('export')">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button @click="handleBatchActions('print')">
          <el-icon><Printer /></el-icon>
          打印
        </el-button>
      </div>
    </div>

    <!-- 批次列表表格 -->
    <el-table
      v-loading="loading"
      :data="batchList"
      @selection-change="handleSelectionChange"
      style="width: 100%"
      border
      stripe
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="batchNo" label="批次号" width="150" />
      <el-table-column prop="productName" label="产品名称" width="200">
        <template #default="{ row }">
          <div class="product-info">
            <span class="product-name">{{ row.productName }}</span>
            <span class="product-spec">({{ row.productSpec }})</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseName" label="仓库" width="150" />
      <el-table-column prop="currentQuantity" label="当前数量" width="120">
        <template #default="{ row }">
          <span class="quantity">
            {{ formatNumber(row.currentQuantity) }} {{ row.unit }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="totalQuantity" label="原始数量" width="120">
        <template #default="{ row }">
          {{ formatNumber(row.totalQuantity) }} {{ row.unit }}
        </template>
      </el-table-column>
      <el-table-column prop="productionDate" label="生产日期" width="120">
        <template #default="{ row }">
          {{ formatDate(row.productionDate) }}
        </template>
      </el-table-column>
      <el-table-column prop="expirationDate" label="过期日期" width="120">
        <template #default="{ row }">
          <span :class="{ 'expiring-soon': isExpiringSoon(row.expirationDate) }">
            {{ formatDate(row.expirationDate) }}
          </span>
          <el-icon v-if="isExpiringSoon(row.expirationDate)" class="warning-icon">
            <WarningFilled />
          </el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="supplierName" label="供应商" width="150" />
      <el-table-column prop="createdTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.createdTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row.id)">
            查看
          </el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-dropdown @command="(command) => handleAction(command, row)">
            <el-button type="primary" link size="small">
              更多
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="inbound">入库</el-dropdown-item>
                <el-dropdown-item command="outbound">出库</el-dropdown-item>
                <el-dropdown-item command="transfer">转移</el-dropdown-item>
                <el-dropdown-item command="quality-check">质检</el-dropdown-item>
                <el-dropdown-item divided command="split">拆分</el-dropdown-item>
                <el-dropdown-item command="merge">合并</el-dropdown-item>
                <el-dropdown-item divided command="activate">激活</el-dropdown-item>
                <el-dropdown-item command="deactivate">停用</el-dropdown-item>
                <el-dropdown-item command="delete">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 批次详情对话框 -->
    <BatchDetail
      v-model:visible="detailVisible"
      :batch-id="selectedBatchId"
      @refresh="fetchBatchList"
    />

    <!-- 创建/编辑对话框 -->
    <BatchForm
      v-model:visible="formVisible"
      :mode="formMode"
      :batch-data="selectedBatch"
      @success="handleFormSuccess"
    />

    <!-- 批次操作对话框 -->
    <BatchOperation
      v-model:visible="operationVisible"
      :operation-type="operationType"
      :batch-data="selectedBatch"
      @success="handleOperationSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Download, Printer, WarningFilled, ArrowDown } from '@element-plus/icons-vue'
import BatchDetail from './BatchDetail.vue'
import BatchForm from './BatchForm.vue'
import BatchOperation from './BatchOperation.vue'
import type { IBatchItem } from '../types/batch.types'
import { fetchBatchListAPI, deleteBatchAPI } from '../api/batch.api'
import { formatDate, formatDateTime, formatNumber } from '../utils/format'
import { BATCH_STATUS, BATCH_STATUS_LABEL } from '../constants/batch.constants'

// 组件状态
const loading = ref(false)
const detailVisible = ref(false)
const formVisible = ref(false)
const operationVisible = ref(false)
const selectedBatchId = ref<number | null>(null)
const selectedBatch = ref<IBatchItem | null>(null)
const formMode = ref<'create' | 'edit'>('create')
const operationType = ref<string>('')

// 搜索表单
const searchForm = reactive({
  batchNo: '',
  productId: '',
  status: '',
  warehouseId: ''
})

// 批次列表数据
const batchList = ref<IBatchItem[]>([])

// 分页配置
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 选中行数据
const selectedRows = ref<IBatchItem[]>([])

// 选项数据（模拟）
const statusOptions = computed(() => [
  { value: 'ACTIVE', label: '激活' },
  { value: 'INACTIVE', label: '停用' },
  { value: 'LOCKED', label: '锁定' },
  { value: 'COMPLETED', label: '已完成' }
])

const productOptions = ref([
  { id: 1, name: '产品A', spec: '规格A' },
  { id: 2, name: '产品B', spec: '规格B' },
  { id: 3, name: '产品C', spec: '规格C' }
])

const warehouseOptions = ref([
  { id: 1, name: '北京仓库' },
  { id: 2, name: '上海仓库' },
  { id: 3, name: '广州仓库' }
])

// 生命周期
onMounted(() => {
  fetchBatchList()
})

// 获取批次列表
const fetchBatchList = async () => {
  try {
    loading.value = true
    const params = {
      ...searchForm,
      page: pagination.current - 1,
      size: pagination.size
    }
    
    const response = await fetchBatchListAPI(params)
    batchList.value = response.data.content || []
    pagination.total = response.data.totalElements || 0
  } catch (error) {
    console.error('获取批次列表失败:', error)
    ElMessage.error('获取批次列表失败')
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchBatchList()
}

// 处理重置
const handleReset = () => {
  Object.assign(searchForm, {
    batchNo: '',
    productId: '',
    status: '',
    warehouseId: ''
  })
  handleSearch()
}

// 处理选择变化
const handleSelectionChange = (rows: IBatchItem[]) => {
  selectedRows.value = rows
}

// 处理查看详情
const handleView = (id: number) => {
  selectedBatchId.value = id
  detailVisible.value = true
}

// 处理新建
const handleCreate = () => {
  formMode.value = 'create'
  selectedBatch.value = null
  formVisible.value = true
}

// 处理编辑
const handleEdit = (batch: IBatchItem) => {
  formMode.value = 'edit'
  selectedBatch.value = batch
  formVisible.value = true
}

// 处理表单成功
const handleFormSuccess = () => {
  formVisible.value = false
  fetchBatchList()
}

// 处理操作
const handleAction = (command: string, batch: IBatchItem) => {
  selectedBatch.value = batch
  
  switch (command) {
    case 'delete':
      handleDelete(batch.id)
      break
    case 'inbound':
    case 'outbound':
    case 'transfer':
    case 'quality-check':
    case 'split':
    case 'merge':
    case 'activate':
    case 'deactivate':
      operationType.value = command
      operationVisible.value = true
      break
    default:
      console.warn(`未知操作: ${command}`)
  }
}

// 处理删除
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除此批次吗？删除后批次将进入回收站，30天后自动永久删除。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteBatchAPI(id)
    ElMessage.success('批次删除成功')
    fetchBatchList()
  } catch (error) {
    // 用户取消或操作失败
  }
}

// 处理批量操作
const handleBatchActions = (action: string) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要操作的批次')
    return
  }
  
  switch (action) {
    case 'export':
      handleBatchExport()
      break
    case 'print':
      handleBatchPrint()
      break
    default:
      console.warn(`未知批量操作: ${action}`)
  }
}

// 批量导出
const handleBatchExport = () => {
  const batchIds = selectedRows.value.map(row => row.id)
  console.log('批量导出批次:', batchIds)
  ElMessage.info('批量导出功能开发中')
}

// 批量打印
const handleBatchPrint = () => {
  const batchIds = selectedRows.value.map(row => row.id)
  console.log('批量打印批次:', batchIds)
  ElMessage.info('批量打印功能开发中')
}

// 处理操作成功
const handleOperationSuccess = () => {
  operationVisible.value = false
  fetchBatchList()
}

// 处理分页大小变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.current = 1
  fetchBatchList()
}

// 处理当前页变化
const handleCurrentChange = (page: number) => {
  pagination.current = page
  fetchBatchList()
}

// 获取状态标签类型
const getStatusTagType = (status: string) => {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'INACTIVE': return 'info'
    case 'LOCKED': return 'warning'
    case 'COMPLETED': return 'success'
    default: return 'info'
  }
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  return BATCH_STATUS_LABEL[status as keyof typeof BATCH_STATUS_LABEL] || status
}

// 检查是否即将过期（7天内）
const isExpiringSoon = (expirationDate: string) => {
  const now = new Date()
  const expDate = new Date(expirationDate)
  const diffTime = expDate.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays <= 7 && diffDays > 0
}
</script>

<style scoped lang="scss">
.batch-list-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

  .batch-list-toolbar {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 16px;

    .search-form {
      flex: 1;
      min-width: 300px;
    }

    .action-buttons {
      display: flex;
      gap: 12px;
      flex-shrink: 0;
    }
  }

  .product-info {
    display: flex;
    flex-direction: column;
    line-height: 1.4;

    .product-name {
      font-weight: 500;
    }

    .product-spec {
      font-size: 12px;
      color: #666;
    }
  }

  .quantity {
    font-weight: 600;
    color: #409eff;
  }

  .expiring-soon {
    color: #e6a23c;
    font-weight: 500;
  }

  .warning-icon {
    color: #e6a23c;
    margin-left: 4px;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  :deep(.el-table) {
    .el-table__header-wrapper th {
      background: #f5f7fa;
      font-weight: 600;
      color: #303133;
    }
  }
}

@media (max-width: 768px) {
  .batch-list-container {
    padding: 12px;

    .batch-list-toolbar {
      flex-direction: column;
      gap: 12px;

      .search-form {
        width: 100%;
      }

      .action-buttons {
        width: 100%;
        justify-content: flex-start;
      }
    }
  }
}
</style>