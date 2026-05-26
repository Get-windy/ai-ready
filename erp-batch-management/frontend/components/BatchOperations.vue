<template>
  <div class="batch-operations-container">
    <!-- 批量操作面板 -->
    <el-card class="batch-actions-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">批量操作</span>
          <div class="header-info">
            <span class="selection-count">已选择 {{ selectedBatches.length }} 个批次</span>
          </div>
        </div>
      </template>

      <div class="action-buttons">
        <el-button
          type="primary"
          :disabled="!hasSelection"
          @click="handleBatchApprove"
          :loading="batchActions.approve.loading"
        >
          <el-icon><Check /></el-icon> 批量审核
        </el-button>
        <el-button
          type="warning"
          :disabled="!hasSelection"
          @click="handleBatchCancel"
          :loading="batchActions.cancel.loading"
        >
          <el-icon><Close /></el-icon> 批量取消
        </el-button>
        <el-button
          type="success"
          :disabled="!hasSelection"
          @click="handleBatchComplete"
          :loading="batchActions.complete.loading"
        >
          <el-icon><Finished /></el-icon> 批量完成
        </el-button>
        <el-button
          type="danger"
          :disabled="!hasSelection"
          @click="handleBatchDelete"
          :loading="batchActions.delete.loading"
        >
          <el-icon><Delete /></el-icon> 批量删除
        </el-button>
        <el-button
          type="info"
          :disabled="!hasSelection"
          @click="handleBatchExport"
          :loading="batchActions.export.loading"
        >
          <el-icon><Download /></el-icon> 批量导出
        </el-button>
      </div>

      <!-- 批量操作表单 -->
      <el-form
        v-if="hasSelection"
        :model="batchForm"
        :rules="batchFormRules"
        ref="batchFormRef"
        label-width="100px"
        class="batch-form"
      >
        <el-form-item label="操作备注" prop="remark">
          <el-input
            v-model="batchForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入批量操作的备注信息..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <!-- 选中批次列表 -->
      <div v-if="hasSelection" class="selected-batches">
        <h4>已选批次列表</h4>
        <el-table
          :data="selectedBatches"
          style="width: 100%"
          max-height="300"
          size="small"
        >
          <el-table-column prop="batchNo" label="批次号" width="180" />
          <el-table-column prop="productName" label="产品名称" width="200" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getStatusType(row.status)">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="150">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                type="text"
                @click="removeFromSelection(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- 快速操作面板 -->
    <el-card class="quick-actions-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">快速操作</span>
        </div>
      </template>

      <div class="quick-actions-grid">
        <div class="quick-action-item" @click="handleCreateBatch">
          <div class="action-icon create">
            <el-icon><Plus /></el-icon>
          </div>
          <div class="action-content">
            <h4>新建批次</h4>
            <p>创建新的生产批次</p>
          </div>
        </div>

        <div class="quick-action-item" @click="handleImportBatches">
          <div class="action-icon import">
            <el-icon><Upload /></el-icon>
          </div>
          <div class="action-content">
            <h4>导入批次</h4>
            <p>从Excel文件导入批次数据</p>
          </div>
        </div>

        <div class="quick-action-item" @click="handleTemplateDownload">
          <div class="action-icon template">
            <el-icon><Download /></el-icon>
          </div>
          <div class="action-content">
            <h4>下载模板</h4>
            <p>下载批次导入模板</p>
          </div>
        </div>

        <div class="quick-action-item" @click="handleStatistics">
          <div class="action-icon statistics">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="action-content">
            <h4>批次统计</h4>
            <p>查看批次生产统计</p>
          </div>
        </div>

        <div class="quick-action-item" @click="handleSettings">
          <div class="action-icon settings">
            <el-icon><Setting /></el-icon>
          </div>
          <div class="action-content">
            <h4>批次设置</h4>
            <p>配置批次相关参数</p>
          </div>
        </div>

        <div class="quick-action-item" @click="handleHelp">
          <div class="action-icon help">
            <el-icon><QuestionFilled /></el-icon>
          </div>
          <div class="action-content">
            <h4>操作帮助</h4>
            <p>查看批次管理帮助文档</p>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 操作历史 -->
    <el-card class="operation-history-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">最近操作历史</span>
          <el-button type="text" @click="refreshHistory">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table
        :data="operationHistory"
        v-loading="historyLoading"
        style="width: 100%"
        size="small"
      >
        <el-table-column prop="time" label="时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.time) }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="batchNo" label="批次号" width="180" />
        <el-table-column prop="details" label="详情" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.success ? 'success' : 'danger'"
            >
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 批量操作确认对话框 -->
    <el-dialog
      v-model="batchConfirmDialog.visible"
      :title="batchConfirmDialog.title"
      width="500px"
      :before-close="handleBatchConfirmClose"
    >
      <div v-if="batchConfirmDialog.type">
        <p>{{ batchConfirmDialog.message }}</p>
        <el-form
          v-if="batchConfirmDialog.type === 'cancel'"
          :model="batchForm"
          :rules="batchFormRules"
          ref="confirmFormRef"
          label-width="100px"
          style="margin-top: 20px"
        >
          <el-form-item label="取消原因" prop="remark">
            <el-input
              v-model="batchForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入取消原因..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchConfirmDialog.visible = false">取消</el-button>
          <el-button
            type="primary"
            @click="confirmBatchAction"
            :loading="batchConfirmDialog.loading"
          >
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  Close,
  Finished,
  Delete,
  Download,
  Plus,
  Upload,
  DataAnalysis,
  Setting,
  QuestionFilled,
  Refresh
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// 响应式数据
const selectedBatches = ref([])
const operationHistory = ref([])
const historyLoading = ref(false)

// 批量操作状态
const batchActions = reactive({
  approve: { loading: false },
  cancel: { loading: false },
  complete: { loading: false },
  delete: { loading: false },
  export: { loading: false }
})

// 批量操作表单
const batchForm = reactive({
  remark: ''
})

const batchFormRules = {
  remark: [
    { required: true, message: '请输入操作备注', trigger: 'blur' },
    { min: 5, max: 500, message: '备注长度在 5 到 500 个字符', trigger: 'blur' }
  ]
}

const batchFormRef = ref(null)
const confirmFormRef = ref(null)

// 确认对话框
const batchConfirmDialog = reactive({
  visible: false,
  type: '',
  title: '',
  message: '',
  loading: false
})

// 计算属性
const hasSelection = computed(() => {
  return selectedBatches.value.length > 0
})

// 状态映射
const statusMap = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已审核', type: 'success' },
  cancelled: { label: '已取消', type: 'danger' },
  completed: { label: '已完成', type: 'info' }
}

// 方法定义
const getStatusType = (status) => {
  return statusMap[status]?.type || 'default'
}

const getStatusLabel = (status) => {
  return statusMap[status]?.label || status
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('MM-DD HH:mm')
}

const removeFromSelection = (batch) => {
  const index = selectedBatches.value.findIndex(b => b.id === batch.id)
  if (index !== -1) {
    selectedBatches.value.splice(index, 1)
    ElMessage.success(`已移除批次 ${batch.batchNo}`)
  }
}

const handleBatchApprove = () => {
  batchConfirmDialog.type = 'approve'
  batchConfirmDialog.title = '批量审核'
  batchConfirmDialog.message = `确定要审核通过选中的 ${selectedBatches.value.length} 个批次吗？`
  batchConfirmDialog.visible = true
}

const handleBatchCancel = () => {
  batchConfirmDialog.type = 'cancel'
  batchConfirmDialog.title = '批量取消'
  batchConfirmDialog.message = `确定要取消选中的 ${selectedBatches.value.length} 个批次吗？`
  batchConfirmDialog.visible = true
}

const handleBatchComplete = () => {
  batchConfirmDialog.type = 'complete'
  batchConfirmDialog.title = '批量完成'
  batchConfirmDialog.message = `确定要标记选中的 ${selectedBatches.value.length} 个批次为已完成吗？`
  batchConfirmDialog.visible = true
}

const handleBatchDelete = () => {
  batchConfirmDialog.type = 'delete'
  batchConfirmDialog.title = '批量删除'
  batchConfirmDialog.message = `确定要删除选中的 ${selectedBatches.value.length} 个批次吗？此操作不可恢复。`
  batchConfirmDialog.visible = true
}

const handleBatchExport = async () => {
  batchActions.export.loading = true
  try {
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('批量导出成功')
  } catch (error) {
    ElMessage.error('批量导出失败')
  } finally {
    batchActions.export.loading = false
  }
}

const handleBatchConfirmClose = (done) => {
  if (batchConfirmDialog.loading) {
    return
  }
  done()
}

const confirmBatchAction = async () => {
  if (batchConfirmDialog.type === 'cancel') {
    try {
      await confirmFormRef.value?.validate()
    } catch (error) {
      return
    }
  }

  batchConfirmDialog.loading = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    let message = ''
    switch (batchConfirmDialog.type) {
      case 'approve':
        message = '批量审核成功'
        break
      case 'cancel':
        message = '批量取消成功'
        break
      case 'complete':
        message = '批量完成成功'
        break
      case 'delete':
        message = '批量删除成功'
        break
    }
    
    ElMessage.success(message)
    batchConfirmDialog.visible = false
    selectedBatches.value = []
    batchForm.remark = ''
    
    // 刷新历史记录
    fetchOperationHistory()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    batchConfirmDialog.loading = false
  }
}

const fetchOperationHistory = async () => {
  historyLoading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    
    // 模拟数据
    operationHistory.value = [
      {
        id: 1,
        time: '2026-05-05 09:30:00',
        action: '审核通过',
        operator: '张三',
        batchNo: 'BATCH-2026-05-001',
        details: '智能传感器生产批次审核通过',
        success: true
      },
      {
        id: 2,
        time: '2026-05-05 09:15:00',
        action: '创建批次',
        operator: '李四',
        batchNo: 'BATCH-2026-05-002',
        details: '创建电路板生产批次',
        success: true
      },
      {
        id: 3,
        time: '2026-05-05 08:45:00',
        action: '取消批次',
        operator: '王五',
        batchNo: 'BATCH-2026-04-015',
        details: '取消过期批次',
        success: true
      },
      {
        id: 4,
        time: '2026-05-05 08:30:00',
        action: '批量导出',
        operator: '赵六',
        batchNo: 'BATCH-2026-04-001~010',
        details: '导出10个批次数据',
        success: true
      }
    ]
  } catch (error) {
    console.error('Error fetching operation history:', error)
  } finally {
    historyLoading.value = false
  }
}

const handleCreateBatch = () => {
  ElMessage.info('新建批次功能待实现')
}

const handleImportBatches = () => {
  ElMessage.info('导入批次功能待实现')
}

const handleTemplateDownload = () => {
  ElMessage.info('下载模板功能待实现')
}

const handleStatistics = () => {
  ElMessage.info('批次统计功能待实现')
}

const handleSettings = () => {
  ElMessage.info('批次设置功能待实现')
}

const handleHelp = () => {
  ElMessage.info('操作帮助功能待实现')
}

const refreshHistory = () => {
  fetchOperationHistory()
}

// 初始化
fetchOperationHistory()

// 模拟选中批次数据（实际应用中从父组件传入）
selectedBatches.value = [
  {
    id: 1,
    batchNo: 'BATCH-2026-05-001',
    productName: '智能传感器',
    status: 'pending',
    createdAt: '2026-05-01 09:00:00'
  },
  {
    id: 2,
    batchNo: 'BATCH-2026-05-002',
    productName: '电路板',
    status: 'approved',
    createdAt: '2026-05-02 14:30:00'
  }
]
</script>

<style scoped>
.batch-operations-container {
  padding: 20px;
}

.batch-actions-card,
.quick-actions-card,
.operation-history-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.selection-count {
  color: #409eff;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.batch-form {
  margin-top: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.selected-batches {
  margin-top: 20px;
}

.selected-batches h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
}

.quick-action-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
}

.quick-action-item:hover {
  background-color: #ecf5ff;
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.1);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.action-icon.create {
  background-color: #67c23a;
}

.action-icon.import {
  background-color: #409eff;
}

.action-icon.template {
  background-color: #e6a23c;
}

.action-icon.statistics {
  background-color: #f56c6c;
}

.action-icon.settings {
  background-color: #909399;
}

.action-icon.help {
  background-color: #6f42c1;
}

.action-content h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.action-content p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
</style>