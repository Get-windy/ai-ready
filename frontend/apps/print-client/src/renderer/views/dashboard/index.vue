<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Card from '@/components/common/Card.vue'
import StatCard from '@/components/dashboard/StatCard.vue'
import TaskList from '@/components/dashboard/TaskList.vue'
import PrinterStatus from '@/components/dashboard/PrinterStatus.vue'

const router = useRouter()

const stats = ref({
  totalTasks: 0,
  pendingTasks: 0,
  completedTasks: 0,
  failedTasks: 0,
  todayTasks: 0
})

const recentTasks = ref<any[]>([])
const printers = ref<any[]>([])
const connectionStatus = ref<'connected' | 'disconnected'>('disconnected')

onMounted(async () => {
  await loadData()
})

const loadData = async () => {
  const tasks = await window.electronAPI.tasks.getTasks()
  
  stats.value = {
    totalTasks: tasks.length,
    pendingTasks: tasks.filter(t => t.status === 'pending').length,
    completedTasks: tasks.filter(t => t.status === 'completed').length,
    failedTasks: tasks.filter(t => t.status === 'failed').length,
    todayTasks: tasks.filter(t => {
      const today = new Date().toDateString()
      return new Date(t.createTime).toDateString() === today
    }).length
  }
  
  recentTasks.value = tasks
    .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
    .slice(0, 5)
  
  printers.value = await window.electronAPI.printers.getPrinters()
  
  const statusResp = await window.electronAPI.connection.getStatus()
  connectionStatus.value = statusResp?.isConnected ? 'connected' : 'disconnected'
}

const handleViewQueue = () => {
  router.push('/queue')
}

const handleViewSettings = () => {
  router.push('/settings')
}

const handleViewLogs = () => {
  router.push('/logs')
}
</script>

<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h1>仪表盘</h1>
      <p class="subtitle">打印客户端状态概览</p>
    </div>
    
    <div class="stats-grid">
      <StatCard 
        title="待打印"
        :value="stats.pendingTasks"
        icon="pending"
        color="#ff976a"
        @click="handleViewQueue"
      />
      <StatCard 
        title="已完成"
        :value="stats.completedTasks"
        icon="success"
        color="#07c160"
      />
      <StatCard 
        title="失败"
        :value="stats.failedTasks"
        icon="error"
        color="#f44"
        @click="handleViewLogs"
      />
      <StatCard 
        title="今日任务"
        :value="stats.todayTasks"
        icon="calendar"
        color="#1988fa"
      />
    </div>
    
    <div class="content-grid">
      <Card title="最近任务" class="tasks-card">
        <TaskList :tasks="recentTasks" />
        <div class="card-footer">
          <button class="view-all-btn" @click="handleViewQueue">
            查看全部
          </button>
        </div>
      </Card>
      
      <Card title="打印机状态" class="printers-card">
        <PrinterStatus :printers="printers" />
        <div class="card-footer">
          <button class="view-all-btn" @click="router.push('/printers')">
            管理打印机
          </button>
        </div>
      </Card>
      
      <Card title="连接状态" class="connection-card">
        <div class="connection-info">
          <div class="status-indicator" :class="connectionStatus">
            {{ connectionStatus === 'connected' ? '已连接' : '未连接' }}
          </div>
          <div class="server-info">
            <span class="label">服务器:</span>
            <span class="value">{{ connectionStatus === 'connected' ? '已连接' : '未配置' }}
            </span>
          </div>
        </div>
        <div class="card-footer">
          <button class="settings-btn" @click="handleViewSettings">
            设置
          </button>
        </div>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.dashboard-page {
  .page-header {
    margin-bottom: 24px;
    
    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
    
    .subtitle {
      font-size: 14px;
      color: #969799;
      margin-top: 4px;
    }
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
  
  .tasks-card {
    min-height: 300px;
  }
  
  .printers-card, .connection-card {
    min-height: 200px;
  }
}

.card-footer {
  padding: 12px;
  border-top: 1px solid #ebedf0;
  
  .view-all-btn, .settings-btn {
    width: 100%;
    padding: 8px 16px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    
    &:hover {
      background: #0e7cd3;
    }
  }
}

.connection-info {
  padding: 16px;
  
  .status-indicator {
    display: inline-flex;
    align-items: center;
    padding: 8px 16px;
    border-radius: 4px;
    font-size: 14px;
    
    &.connected {
      background: #e6f7ff;
      color: #1988fa;
    }
    
    &.disconnected {
      background: #fff1e6;
      color: #ff976a;
    }
  }
  
  .server-info {
    margin-top: 12px;
    
    .label {
      color: #969799;
    }
    
    .value {
      color: #333;
      margin-left: 8px;
    }
  }
}
</style>