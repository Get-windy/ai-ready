<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import message from '@/utils/message'
import Card from '@/components/common/Card.vue'

const router = useRouter()
const route = useRoute()

const taskId = computed(() => route.params.id as string)

interface PrintTask {
  id: string
  taskNo: string
  templateName: string
  printerName: string
  status: 'pending' | 'printing' | 'completed' | 'failed'
  createTime: string
  startTime: string
  endTime: string
  pageCount: number
  copies: number
  fileSize: number
  fileType: string
  source: string
  priority: 'high' | 'normal' | 'low'
  error: string
  previewUrl: string
}

const taskDetail = ref<PrintTask | null>(null)
const loading = ref(false)

const statusMap = {
  pending: { label: '待打印', color: '#969799' },
  printing: { label: '打印中', color: '#1988fa' },
  completed: { label: '已完成', color: '#07c160' },
  failed: { label: '失败', color: '#f44' }
}

const priorityMap = {
  high: { label: '紧急', color: '#f44' },
  normal: { label: '普通', color: '#1988fa' },
  low: { label: '低', color: '#969799' }
}

onMounted(async () => {
  loadTaskDetail()
})

const loadTaskDetail = async () => {
  loading.value = true
  
  try {
    const task = await window.electronAPI.tasks.getTaskById(taskId.value)
    taskDetail.value = task || {
      id: taskId.value,
      taskNo: 'PT202401001',
      templateName: '销售订单模板',
      printerName: 'HP LaserJet Pro',
      status: 'completed',
      createTime: '2024-01-15 10:30:00',
      startTime: '2024-01-15 10:31:00',
      endTime: '2024-01-15 10:32:00',
      pageCount: 3,
      copies: 2,
      fileSize: 256000,
      fileType: 'PDF',
      source: '销售系统',
      priority: 'normal',
      error: '',
      previewUrl: ''
    }
  } finally {
    loading.value = false
  }
}

const handleReprint = async () => {
  if (!taskDetail.value) return
  
  try {
    await window.electronAPI.tasks.reprint(taskId.value)
    message.success('已重新发送打印任务')
  } catch (err) {
    message.error('重新打印失败: ' + (err?.message || err))
  }
}

const handleCancel = async () => {
  if (!taskDetail.value) return
  
  if (confirm('确定取消该打印任务？')) {
    try {
      await window.electronAPI.tasks.cancel(taskId.value)
      router.back()
    } catch (err) {
      message.error('取消失败: ' + (err?.message || err))
    }
  }
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="task-detail-page">
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        ← 返回
      </button>
      <h1>任务详情</h1>
    </div>
    
    <div v-if="taskDetail" class="detail-content">
      <Card title="基本信息">
        <div class="info-grid">
          <div class="info-item">
            <span class="label">任务编号:</span>
            <span class="value">{{ taskDetail.taskNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">状态:</span>
            <span class="value status" :style="{ color: statusMap[taskDetail.status].color }">
              {{ statusMap[taskDetail.status].label }}
            </span>
          </div>
          <div class="info-item">
            <span class="label">优先级:</span>
            <span class="value" :style="{ color: priorityMap[taskDetail.priority].color }">
              {{ priorityMap[taskDetail.priority].label }}
            </span>
          </div>
          <div class="info-item">
            <span class="label">模板:</span>
            <span class="value">{{ taskDetail.templateName }}</span>
          </div>
          <div class="info-item">
            <span class="label">打印机:</span>
            <span class="value">{{ taskDetail.printerName }}</span>
          </div>
          <div class="info-item">
            <span class="label">来源:</span>
            <span class="value">{{ taskDetail.source }}</span>
          </div>
        </div>
      </Card>
      
      <Card title="打印参数">
        <div class="info-grid">
          <div class="info-item">
            <span class="label">页数:</span>
            <span class="value">{{ taskDetail.pageCount }} 页</span>
          </div>
          <div class="info-item">
            <span class="label">份数:</span>
            <span class="value">{{ taskDetail.copies }} 份</span>
          </div>
          <div class="info-item">
            <span class="label">文件大小:</span>
            <span class="value">{{ formatFileSize(taskDetail.fileSize) }}</span>
          </div>
          <div class="info-item">
            <span class="label">文件类型:</span>
            <span class="value">{{ taskDetail.fileType }}</span>
          </div>
        </div>
      </Card>
      
      <Card title="时间信息">
        <div class="info-grid">
          <div class="info-item">
            <span class="label">创建时间:</span>
            <span class="value">{{ taskDetail.createTime }}</span>
          </div>
          <div class="info-item">
            <span class="label">开始时间:</span>
            <span class="value">{{ taskDetail.startTime || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">完成时间:</span>
            <span class="value">{{ taskDetail.endTime || '-' }}</span>
          </div>
        </div>
      </Card>
      
      <Card v-if="taskDetail.error" title="错误信息">
        <div class="error-content">
          {{ taskDetail.error }}
        </div>
      </Card>
      
      <div class="action-buttons">
        <button 
          v-if="taskDetail.status === 'failed'"
          class="btn primary"
          @click="handleReprint"
        >
          重新打印
        </button>
        <button 
          v-if="taskDetail.status === 'pending' || taskDetail.status === 'printing'"
          class="btn danger"
          @click="handleCancel"
        >
          取消任务
        </button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.task-detail-page {
  .page-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    
    .back-btn {
      padding: 8px 16px;
      background: #f7f8fa;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      
      &:hover {
        background: #ebedf0;
      }
    }
    
    h1 {
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }
  }
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px;
  
  .info-item {
    display: flex;
    
    .label {
      width: 100px;
      color: #969799;
    }
    
    .value {
      color: #333;
      
      &.status {
        font-weight: 600;
      }
    }
  }
}

.error-content {
  padding: 16px;
  background: #fff1e6;
  color: #f44;
  border-radius: 4px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  
  .btn {
    padding: 12px 24px;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    
    &.primary {
      background: #1988fa;
      color: #fff;
      
      &:hover {
        background: #0e7cd3;
      }
    }
    
    &.danger {
      background: #f44;
      color: #fff;
      
      &:hover {
        background: #d63030;
      }
    }
  }
}
</style>