<template>
  <div class="batch-detail-container">
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button icon="Back" @click="handleBack">返回</el-button>
            <span class="card-title">批次详情 - {{ batchDetail?.batchNo || '' }}</span>
          </div>
          <div class="header-actions">
            <el-button v-if="canEdit" type="primary" @click="handleEdit">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button v-if="canApprove" type="success" @click="handleApprove">
              <el-icon><Check /></el-icon> 审核通过
            </el-button>
            <el-button v-if="canCancel" type="danger" @click="handleCancel">
              <el-icon><Close /></el-icon> 取消批次
            </el-button>
            <el-button type="info" @click="handleExport">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </div>
        </div>
      </template>

      <!-- 批次基本信息 -->
      <div class="basic-info-section">
        <h3 class="section-title">基本信息</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="批次号">
            <el-tag>{{ batchDetail?.batchNo || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="产品名称">
            {{ batchDetail?.productName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="产品编码">
            {{ batchDetail?.productCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="数量">
            {{ batchDetail?.quantity || 0 }} {{ batchDetail?.unit || '件' }}
          </el-descriptions-item>
          <el-descriptions-item label="批次状态">
            <el-tag :type="getStatusType(batchDetail?.status)">
              {{ getStatusLabel(batchDetail?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="getPriorityType(batchDetail?.priority)">
              {{ getPriorityLabel(batchDetail?.priority) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ batchDetail?.createdBy || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(batchDetail?.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="最后更新">
            {{ formatDate(batchDetail?.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="3">
            {{ batchDetail?.description || '无描述信息' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 物料清单 -->
      <div class="materials-section">
        <h3 class="section-title">物料清单</h3>
        <el-table :data="materials" v-loading="materialsLoading" style="width: 100%">
          <el-table-column prop="materialCode" label="物料编码" width="150" />
          <el-table-column prop="materialName" label="物料名称" width="200" />
          <el-table-column prop="specification" label="规格" width="150" />
          <el-table-column prop="requiredQuantity" label="需求数量" width="120" align="right">
            <template #default="{ row }">
              {{ row.requiredQuantity }} {{ row.unit }}
            </template>
          </el-table-column>
          <el-table-column prop="allocatedQuantity" label="已分配" width="120" align="right">
            <template #default="{ row }">
              {{ row.allocatedQuantity }} {{ row.unit }}
            </template>
          </el-table-column>
          <el-table-column prop="availableQuantity" label="可用库存" width="120" align="right">
            <template #default="{ row }">
              {{ row.availableQuantity }} {{ row.unit }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getMaterialStatusType(row.status)">
                {{ getMaterialStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                type="text"
                @click="handleViewMaterial(row)"
              >
                详情
              </el-button>
              <el-button
                size="small"
                type="text"
                @click="handleAllocateMaterial(row)"
                v-if="canAllocate(row)"
              >
                分配
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 操作记录 -->
      <div class="operations-section">
        <h3 class="section-title">操作记录</h3>
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in operationRecords"
            :key="index"
            :timestamp="formatDate(record.time)"
            :type="getRecordType(record.type)"
            :color="getRecordColor(record.type)"
          >
            <div class="record-content">
              <span class="record-action">{{ record.action }}</span>
              <span class="record-operator">{{ record.operator }}</span>
              <div class="record-remark" v-if="record.remark">
                {{ record.remark }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Edit, Check, Close, Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const batchDetail = ref(null)
const materials = ref([])
const materialsLoading = ref(false)
const operationRecords = ref([])

// 状态映射
const statusMap = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已审核', type: 'success' },
  cancelled: { label: '已取消', type: 'danger' },
  completed: { label: '已完成', type: 'info' }
}

const priorityMap = {
  high: { label: '高', type: 'danger' },
  medium: { label: '中', type: 'warning' },
  low: { label: '低', type: 'success' }
}

const materialStatusMap = {
  pending: { label: '待分配', type: 'warning' },
  allocated: { label: '已分配', type: 'info' },
  completed: { label: '已完成', type: 'success' },
  insufficient: { label: '库存不足', type: 'danger' }
}

const recordTypeMap = {
  create: { type: 'primary', color: '#409eff' },
  edit: { type: 'warning', color: '#e6a23c' },
  approve: { type: 'success', color: '#67c23a' },
  cancel: { type: 'danger', color: '#f56c6c' },
  allocate: { type: 'info', color: '#909399' }
}

// 计算属性
const canEdit = computed(() => {
  return batchDetail.value?.status === 'pending'
})

const canApprove = computed(() => {
  return batchDetail.value?.status === 'pending'
})

const canCancel = computed(() => {
  return batchDetail.value?.status === 'pending' || batchDetail.value?.status === 'approved'
})

// 生命周期
onMounted(() => {
  const batchId = route.params.id
  if (batchId) {
    fetchBatchDetail(batchId)
    fetchMaterials(batchId)
    fetchOperationRecords(batchId)
  }
})

// 方法定义
const fetchBatchDetail = async (batchId) => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    
    // 模拟数据
    batchDetail.value = {
      id: batchId,
      batchNo: 'BATCH-2026-05-001',
      productName: '智能传感器',
      productCode: 'SN-001',
      quantity: 1000,
      unit: '件',
      status: 'pending',
      priority: 'high',
      description: '智能传感器生产批次，用于物联网项目',
      createdBy: '张三',
      createdAt: '2026-05-01 09:00:00',
      updatedAt: '2026-05-05 09:30:00'
    }
  } catch (error) {
    ElMessage.error('获取批次详情失败')
    console.error('Error fetching batch detail:', error)
  } finally {
    loading.value = false
  }
}

const fetchMaterials = async (batchId) => {
  materialsLoading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 400))
    
    // 模拟数据
    materials.value = [
      {
        id: 1,
        materialCode: 'MAT-001',
        materialName: '芯片模块',
        specification: 'STM32F407',
        requiredQuantity: 1000,
        allocatedQuantity: 800,
        availableQuantity: 1200,
        unit: '个',
        status: 'allocated'
      },
      {
        id: 2,
        materialCode: 'MAT-002',
        materialName: '电路板',
        specification: 'PCB-4层',
        requiredQuantity: 1000,
        allocatedQuantity: 1000,
        availableQuantity: 1500,
        unit: '片',
        status: 'completed'
      },
      {
        id: 3,
        materialCode: 'MAT-003',
        materialName: '外壳',
        specification: 'ABS黑色',
        requiredQuantity: 1000,
        allocatedQuantity: 600,
        availableQuantity: 400,
        unit: '个',
        status: 'insufficient'
      }
    ]
  } catch (error) {
    ElMessage.error('获取物料清单失败')
    console.error('Error fetching materials:', error)
  } finally {
    materialsLoading.value = false
  }
}

const fetchOperationRecords = async (batchId) => {
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 200))
    
    // 模拟数据
    operationRecords.value = [
      {
        id: 1,
        action: '创建批次',
        operator: '张三',
        time: '2026-05-01 09:00:00',
        type: 'create',
        remark: '创建生产批次'
      },
      {
        id: 2,
        action: '更新物料清单',
        operator: '李四',
        time: '2026-05-02 14:30:00',
        type: 'edit',
        remark: '添加芯片模块需求'
      },
      {
        id: 3,
        action: '物料分配',
        operator: '王五',
        time: '2026-05-03 10:15:00',
        type: 'allocate',
        remark: '分配电路板800片'
      }
    ]
  } catch (error) {
    console.error('Error fetching operation records:', error)
  }
}

const getStatusType = (status) => {
  return statusMap[status]?.type || 'default'
}

const getStatusLabel = (status) => {
  return statusMap[status]?.label || status
}

const getPriorityType = (priority) => {
  return priorityMap[priority]?.type || 'default'
}

const getPriorityLabel = (priority) => {
  return priorityMap[priority]?.label || priority
}

const getMaterialStatusType = (status) => {
  return materialStatusMap[status]?.type || 'default'
}

const getMaterialStatusLabel = (status) => {
  return materialStatusMap[status]?.label || status
}

const getRecordType = (type) => {
  return recordTypeMap[type]?.type || 'primary'
}

const getRecordColor = (type) => {
  return recordTypeMap[type]?.color || '#409eff'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

const canAllocate = (row) => {
  return row.status === 'pending' || row.status === 'insufficient'
}

const handleBack = () => {
  router.back()
}

const handleEdit = () => {
  ElMessage.info('编辑批次功能待实现')
}

const handleApprove = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要审核通过该批次吗？',
      '确认审核',
      {
        confirmButtonText: '通过审核',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    ElMessage.success('批次审核通过')
    // 更新状态
    if (batchDetail.value) {
      batchDetail.value.status = 'approved'
      batchDetail.value.updatedAt = dayjs().format('YYYY-MM-DD HH:mm:ss')
    }
  } catch {
    // 用户取消
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.prompt(
      '请输入取消原因',
      '取消批次',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '请输入取消批次的原因...'
      }
    ).then(({ value }) => {
      ElMessage.success('批次已取消')
      if (batchDetail.value) {
        batchDetail.value.status = 'cancelled'
        batchDetail.value.updatedAt = dayjs().format('YYYY-MM-DD HH:mm:ss')
      }
    })
  } catch {
    // 用户取消
  }
}

const handleExport = () => {
  ElMessage.info('导出功能待实现')
}

const handleViewMaterial = (row) => {
  ElMessage.info(`查看物料详情: ${row.materialName}`)
}

const handleAllocateMaterial = (row) => {
  ElMessage.info(`分配物料: ${row.materialName}`)
}
</script>

<style scoped>
.batch-detail-container {
  padding: 20px;
}

.detail-card {
  min-height: 500px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.section-title {
  margin: 24px 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-left: 4px solid #409eff;
  padding-left: 12px;
}

.basic-info-section {
  margin-bottom: 32px;
}

.materials-section {
  margin-bottom: 32px;
}

.operations-section {
  margin-bottom: 32px;
}

.record-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-action {
  font-weight: 500;
  color: #303133;
}

.record-operator {
  color: #909399;
  font-size: 14px;
}

.record-remark {
  margin-top: 4px;
  color: #606266;
  font-size: 14px;
}
</style>