<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'
import type { PdaTaskItem } from '@/types'

const router = useRouter()

interface ShipTask {
  id: number
  taskNo: string
  status: number
  warehouseName: string
  createTime: string
}

const shipTasks = ref<ShipTask[]>([])
const loading = ref(false)

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待复核', color: '#969799' },
  1: { label: '复核中', color: '#1988fa' },
  2: { label: '已完成', color: '#07c160' },
}

onMounted(() => {
  loadTasks()
})

const loadTasks = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })

  try {
    const tasks = await api.task.getList()
    shipTasks.value = (tasks as unknown as PdaTaskItem[])
      .filter(t => t.taskType === ('SHIP' as string))
      .map(t => ({
        id: t.taskId,
        taskNo: t.taskNo,
        status: t.status,
        warehouseName: t.warehouseName,
        createTime: t.createTime,
      }))
  } catch (err) {
    console.error('[发货] 加载失败', err)
    showToast('加载任务失败')
    shipTasks.value = []
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartTask = async (task: ShipTask) => {
  showLoadingToast({ message: '开始复核...', forbidClick: true })
  try {
    await api.task.start(task.id, 'SHIP')
    showToast('开始复核')
    router.push(`/ship/${task.id}`)
  } catch (err) {
    console.error('[发货] 开始失败', err)
    showToast('操作失败')
  } finally {
    closeToast()
  }
}

const handleViewDetail = (task: ShipTask) => {
  router.push(`/ship/${task.id}`)
}

const handleScanShip = () => {
  router.push('/ship/scan')
}
</script>

<template>
  <div class="ship-page">
    <NavBar title="发货复核">
      <template #right>
        <Button size="small" type="primary" @click="handleScanShip">
          扫码
        </Button>
      </template>
    </NavBar>

    <div class="ship-list">
      <Empty v-if="shipTasks.length === 0 && !loading" description="暂无复核任务" />

      <Card
        v-for="task in shipTasks"
        :key="task.id"
        class="ship-card"
      >
        <template #title>
          <div class="card-header">
            <span class="task-no">{{ task.taskNo }}</span>
            <Tag :color="statusMap[task.status]?.color || '#969799'">
              {{ statusMap[task.status]?.label || task.status }}
            </Tag>
          </div>
        </template>

        <template #desc>
          <div class="card-info">
            <div class="info-row">
              <span class="label">仓库:</span>
              <span class="value">{{ task.warehouseName }}</span>
            </div>
            <div class="info-row">
              <span class="label">创建时间:</span>
              <span class="value">{{ task.createTime }}</span>
            </div>
          </div>
        </template>

        <template #footer>
          <div class="card-actions">
            <Button
              v-if="task.status === 0"
              type="primary"
              size="small"
              @click="handleStartTask(task)"
            >
              开始复核
            </Button>
            <Button
              v-if="task.status === 1"
              type="primary"
              size="small"
              @click="handleScanShip"
            >
              扫码复核
            </Button>
            <Button
              type="default"
              size="small"
              @click="handleViewDetail(task)"
            >
              详情
            </Button>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ship-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.ship-list {
  padding: 12px;
}

.ship-card {
  margin-bottom: 12px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .task-no {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .card-info {
    .info-row {
      display: flex;
      margin-top: 4px;

      .label {
        width: 70px;
        color: #969799;
      }

      .value {
        color: #333;
      }
    }
  }

  .progress-bar {
    height: 4px;
    background: #ebedf0;
    border-radius: 2px;
    margin-top: 12px;
    overflow: hidden;

    .progress-fill {
      height: 100%;
      background: #07c160;
      transition: width 0.3s;
    }
  }

  .card-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>
