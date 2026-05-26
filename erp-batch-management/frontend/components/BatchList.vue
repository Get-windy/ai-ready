<template>
  <div class="batch-list-container">
    <el-card class="batch-list-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">批次列表</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleCreateBatch">
              <el-icon><Plus /></el-icon> 新建批次
            </el-button>
            <el-button @click="handleRefresh">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="query-form">
        <el-form :model="queryParams" inline>
          <el-form-item label="批次号">
            <el-input v-model="queryParams.batchNo" placeholder="请输入批次号" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="待审核" value="pending" />
              <el-option label="已审核" value="approved" />
              <el-option label="已取消" value="cancelled" />
              <el-option label="已完成" value="completed" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="batchList"
        v-loading="loading"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="batchNo" label="批次号" width="180" />
        <el-table-column prop="productName" label="产品名称" width="200" />
        <el-table-column prop="quantity" label="数量" width="100" align="right">
          <template #default="{ row }">
            {{ row.quantity }} {{ row.unit }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdBy" label="创建人" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button
              size="small"
              type="primary"
              @click.stop="handleEdit(row)"
              v-if="row.status === 'pending'"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click.stop="handleDelete(row)"
              v-if="row.status === 'pending'"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// 响应式数据
const loading = ref(false)
const batchList = ref([])
const total = ref(0)

// 查询参数
const queryParams = reactive({
  batchNo: '',
  status: '',
  dateRange: [],
  pageNum: 1,
  pageSize: 10
})

// 状态映射
const statusMap = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已审核', type: 'success' },
  cancelled: { label: '已取消', type: 'danger' },
  completed: { label: '已完成', type: 'info' }
}

// 生命周期
onMounted(() => {
  fetchBatchList()
})

// 方法定义
const fetchBatchList = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟数据
    batchList.value = Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      batchNo: `BATCH-2026-05-0${i + 1}`,
      productName: `产品 ${i + 1}`,
      quantity: Math.floor(Math.random() * 1000) + 100,
      unit: '件',
      status: ['pending', 'approved', 'cancelled', 'completed'][i % 4],
      createdBy: `用户${i + 1}`,
      createdAt: dayjs().subtract(i, 'day').format('YYYY-MM-DD HH:mm:ss')
    }))
    total.value = 50
  } catch (error) {
    ElMessage.error('获取批次列表失败')
    console.error('Error fetching batch list:', error)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  return statusMap[status]?.type || 'default'
}

const getStatusLabel = (status) => {
  return statusMap[status]?.label || status
}

const formatDate = (dateStr) => {
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchBatchList()
}

const handleReset = () => {
  Object.assign(queryParams, {
    batchNo: '',
    status: '',
    dateRange: [],
    pageNum: 1,
    pageSize: 10
  })
  fetchBatchList()
}

const handleSizeChange = (val) => {
  queryParams.pageSize = val
  fetchBatchList()
}

const handleCurrentChange = (val) => {
  queryParams.pageNum = val
  fetchBatchList()
}

const handleCreateBatch = () => {
  ElMessage.info('创建批次功能待实现')
}

const handleRefresh = () => {
  fetchBatchList()
}

const handleRowClick = (row) => {
  console.log('点击行:', row)
}

const handleViewDetail = (row) => {
  ElMessage.info(`查看批次详情: ${row.batchNo}`)
}

const handleEdit = (row) => {
  ElMessage.info(`编辑批次: ${row.batchNo}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除批次 ${row.batchNo} 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    ElMessage.success('删除成功')
    fetchBatchList()
  } catch {
    // 用户取消删除
  }
}
</script>

<style scoped>
.batch-list-container {
  padding: 20px;
}

.batch-list-card {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.query-form {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>