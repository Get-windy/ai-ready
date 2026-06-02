<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()
const taskId = route.params.id as string

interface TaskDetail {
  id: string
  taskNo: string
  taskType: 'receive' | 'pick' | 'check' | 'transfer'
  taskStatus: 'pending' | 'in_progress' | 'completed' | 'cancelled'
  priority: 'high' | 'medium' | 'low'
  warehouseId: string
  warehouseName: string
  locationCode: string
  productId: string
  productName: string
  productCode: string
  productSpec: string
  quantity: number
  unit: string
  batchNo?: string
  sourceOrderNo: string
  sourceOrderType: string
  createTime: string
  assignTime: string
  startTime?: string
  completeTime?: string
  assignee: string
  remark: string
  operationLogs: OperationLog[]
}

interface OperationLog {
  id: string
  actionType: 'assign' | 'start' | 'scan' | 'confirm' | 'complete' | 'cancel'
  actionTime: string
  operator: string
  quantity?: number
  location?: string
  remark: string
}

const task = ref<TaskDetail | null>(null)
const loading = ref(false)
const scanning = ref(false)
const scannedQuantity = ref(0)

const taskTypeMap = {
  receive: { label: '收货任务', color: '#07c160', icon: '📦' },
  pick: { label: '拣货任务', color: '#1988fa', icon: '📋' },
  check: { label: '盘点任务', color: '#ff976a', icon: '🔍' },
  transfer: { label: '转移任务', color: '#7232dd', icon: '🔄' }
}

const taskStatusMap = {
  pending: { label: '待处理', color: '#1988fa', bg: '#e8f4ff' },
  in_progress: { label: '进行中', color: '#ff976a', bg: '#fff3e0' },
  completed: { label: '已完成', color: '#07c160', bg: '#e8f7e8' },
  cancelled: { label: '已取消', color: '#969799', bg: '#f7f8fa' }
}

const priorityMap = {
  high: { label: '高', color: '#f44' },
  medium: { label: '中', color: '#ff976a' },
  low: { label: '低', color: '#969799' }
}

const actionTypeMap = {
  assign: { label: '分配', color: '#1988fa' },
  start: { label: '开始', color: '#ff976a' },
  scan: { label: '扫码', color: '#07c160' },
  confirm: { label: '确认', color: '#1988fa' },
  complete: { label: '完成', color: '#07c160' },
  cancel: { label: '取消', color: '#f44' }
}

const progressPercent = computed(() => {
  if (!task.value) return 0
  if (task.value.taskStatus === 'completed') return 100
  if (task.value.taskStatus === 'pending') return 0
  return Math.round((scannedQuantity.value / task.value.quantity) * 100)
})

onMounted(async () => {
  await loadTaskDetail()
})

const loadTaskDetail = async () => {
  loading.value = true
  try {
    const res: any = await api.task.getDetail(taskId)
    task.value = res?.data || res
  } catch {
    showToast({ type: 'fail', message: '加载任务详情失败' })
    task.value = null
  } finally {
    loading.value = false
  }
}


const handleStartTask = async () => {
  if (confirm('确定开始执行此任务？')) {
    task.value!.taskStatus = 'in_progress'
    task.value!.startTime = new Date().toLocaleString()
    scannedQuantity.value = 0
    alert('任务已开始')
  }
}

const handleScan = async () => {
  scanning.value = true
  try {
    const result = await window.electronAPI?.scanner?.scan?.()
    if (result) {
      scannedQuantity.value++
      if (scannedQuantity.value >= task.value!.quantity) {
        alert('已完成全部拣货')
      }
    }
  } finally {
    scanning.value = false
  }
}

const handleManualInput = () => {
  const qty = prompt('请输入拣货数量')
  if (qty && !isNaN(Number(qty))) {
    scannedQuantity.value += Number(qty)
    if (scannedQuantity.value >= task.value!.quantity) {
      alert('已完成全部拣货')
    }
  }
}

const handleComplete = async () => {
  if (scannedQuantity.value < task.value!.quantity) {
    alert(`还需拣货 ${task.value!.quantity - scannedQuantity.value} ${task.value!.unit}`)
    return
  }
  if (confirm('确定完成此任务？')) {
    task.value!.taskStatus = 'completed'
    task.value!.completeTime = new Date().toLocaleString()
    alert('任务已完成')
  }
}

const handleCancel = async () => {
  const reason = prompt('请输入取消原因')
  if (reason) {
    task.value!.taskStatus = 'cancelled'
    alert('任务已取消')
  }
}

const handleBack = () => {
  router.push('/task')
}
</script>

<template>
  <div class="task-detail-page">
    <div class="page-header">
      <button class="back-btn" @click="handleBack">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
        返回
      </button>
      <h1>任务详情</h1>
      <div class="header-actions">
        <span class="priority-badge" :style="{ color: priorityMap[task?.priority || 'medium'].color }">
          {{ priorityMap[task?.priority || 'medium'].label }}优先
        </span>
      </div>
    </div>

    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div class="detail-content" v-if="task && !loading">
      <div class="status-banner" :style="{ background: taskStatusMap[task.taskStatus].bg }">
        <div class="task-info">
          <span class="task-type-icon">{{ taskTypeMap[task.taskType].icon }}</span>
          <span class="task-no">{{ task.taskNo }}</span>
          <span class="task-type" :style="{ color: taskTypeMap[task.taskType].color }">
            {{ taskTypeMap[task.taskType].label }}
          </span>
          <span class="task-status" :style="{ color: taskStatusMap[task.taskStatus].color }">
            {{ taskStatusMap[task.taskStatus].label }}
          </span>
        </div>
        <div class="progress-info" v-if="task.taskStatus === 'in_progress'">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%', background: '#07c160' }"></div>
          </div>
          <span class="progress-text">{{ scannedQuantity }}/{{ task.quantity }} {{ task.unit }}</span>
        </div>
      </div>

      <div class="info-cards">
        <div class="info-card">
          <h3>商品信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">商品名称</span>
              <span class="value highlight">{{ task.productName }}</span>
            </div>
            <div class="info-item">
              <span class="label">商品编码</span>
              <span class="value">{{ task.productCode }}</span>
            </div>
            <div class="info-item">
              <span class="label">规格</span>
              <span class="value">{{ task.productSpec }}</span>
            </div>
            <div class="info-item">
              <span class="label">数量</span>
              <span class="value">{{ task.quantity }} {{ task.unit }}</span>
            </div>
            <div class="info-item" v-if="task.batchNo">
              <span class="label">批次号</span>
              <span class="value">{{ task.batchNo }}</span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <h3>位置信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">仓库</span>
              <span class="value">{{ task.warehouseName }}</span>
            </div>
            <div class="info-item">
              <span class="label">库位</span>
              <span class="value">{{ task.locationCode }}</span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <h3>来源信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">来源单号</span>
              <span class="value">{{ task.sourceOrderNo }}</span>
            </div>
            <div class="info-item">
              <span class="label">来源类型</span>
              <span class="value">{{ task.sourceOrderType }}</span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <h3>时间信息</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">创建时间</span>
              <span class="value">{{ task.createTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">分配时间</span>
              <span class="value">{{ task.assignTime }}</span>
            </div>
            <div class="info-item" v-if="task.startTime">
              <span class="label">开始时间</span>
              <span class="value">{{ task.startTime }}</span>
            </div>
            <div class="info-item" v-if="task.completeTime">
              <span class="label">完成时间</span>
              <span class="value">{{ task.completeTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">执行人</span>
              <span class="value">{{ task.assignee }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="operation-logs">
        <h3>操作记录</h3>
        <div class="log-list">
          <div v-for="log in task.operationLogs" :key="log.id" class="log-item">
            <div class="log-icon" :style="{ background: actionTypeMap[log.actionType].color + '20' }">
              <span :style="{ color: actionTypeMap[log.actionType].color }">●</span>
            </div>
            <div class="log-content">
              <div class="log-header">
                <span class="action-type" :style="{ color: actionTypeMap[log.actionType].color }">
                  {{ actionTypeMap[log.actionType].label }}
                </span>
                <span class="action-time">{{ log.actionTime }}</span>
              </div>
              <div class="log-details">
                <span class="operator">操作人: {{ log.operator }}</span>
                <span v-if="log.quantity" class="quantity">数量: {{ log.quantity }}</span>
                <span v-if="log.location" class="location">位置: {{ log.location }}</span>
              </div>
              <div v-if="log.remark" class="log-remark">{{ log.remark }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="action-buttons">
        <button 
          v-if="task.taskStatus === 'pending'"
          class="action-btn start"
          @click="handleStartTask"
        >
          开始任务
        </button>
        <button 
          v-if="task.taskStatus === 'in_progress'"
          class="action-btn scan"
          @click="handleScan"
          :disabled="scanning"
        >
          {{ scanning ? '扫码中...' : '扫码拣货' }}
        </button>
        <button 
          v-if="task.taskStatus === 'in_progress'"
          class="action-btn manual"
          @click="handleManualInput"
        >
          手动输入
        </button>
        <button 
          v-if="task.taskStatus === 'in_progress'"
          class="action-btn complete"
          @click="handleComplete"
        >
          完成任务
        </button>
        <button 
          v-if="task.taskStatus === 'pending' || task.taskStatus === 'in_progress'"
          class="action-btn cancel"
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
  padding: 16px;
  background: #f7f8fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h1 {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    flex: 1;
    text-align: center;
  }

  .back-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    background: none;
    color: #333;
    border: none;
    cursor: pointer;
  }

  .priority-badge {
    font-size: 12px;
    font-weight: 600;
  }
}

.status-banner {
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;

  .task-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .task-type-icon {
      font-size: 24px;
    }

    .task-no {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }

    .task-type, .task-status {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .progress-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .progress-bar {
      flex: 1;
      height: 8px;
      background: #ebedf0;
      border-radius: 4px;
      overflow: hidden;

      .progress-fill {
        height: 100%;
        border-radius: 4px;
        transition: width 0.3s;
      }
    }

    .progress-text {
      font-size: 12px;
      color: #666;
    }
  }
}

.info-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;

  .info-card {
    background: #fff;
    border-radius: 8px;
    padding: 16px;

    h3 {
      font-size: 14px;
      font-weight: 600;
      color: #333;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #ebedf0;
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;

      .info-item {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .label {
          font-size: 12px;
          color: #969799;
        }

        .value {
          font-size: 14px;
          color: #333;

          &.highlight {
            font-weight: 600;
          }
        }
      }
    }
  }
}

.operation-logs {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  h3 {
    font-size: 14px;
    font-weight: 600;
    color: #333;
    margin-bottom: 12px;
  }

  .log-list {
    .log-item {
      display: flex;
      gap: 12px;
      padding: 12px;
      border-bottom: 1px solid #ebedf0;

      &:last-child {
        border-bottom: none;
      }

      .log-icon {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .log-content {
        flex: 1;

        .log-header {
          display: flex;
          justify-content: space-between;
          margin-bottom: 4px;

          .action-type {
            font-size: 14px;
            font-weight: 600;
          }

          .action-time {
            font-size: 12px;
            color: #969799;
          }
        }

        .log-details {
          display: flex;
          gap: 12px;
          font-size: 12px;
          color: #666;
        }

        .log-remark {
          font-size: 12px;
          color: #969799;
          margin-top: 4px;
        }
      }
    }
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .action-btn {
    flex: 1;
    min-width: 120px;
    padding: 14px;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    cursor: pointer;

    &.start {
      background: #1988fa;
      color: #fff;
    }

    &.scan {
      background: #07c160;
      color: #fff;
    }

    &.manual {
      background: #ff976a;
      color: #fff;
    }

    &.complete {
      background: #07c160;
      color: #fff;
    }

    &.cancel {
      background: #f7f8fa;
      color: #f44;
      border: 1px solid #f44;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  color: #969799;

  .loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #ebedf0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  p {
    margin-top: 12px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>