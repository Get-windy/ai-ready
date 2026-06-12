<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'
import type { PdaTaskItem } from '@/types'

const router = useRouter()

interface PickedTask {
  id: number
  taskNo: string
  status: number
  warehouseName: string
  createTime: string
}

const tasks = ref<PickedTask[]>([])
const loading = ref(false)

onMounted(async () => {
  loadTasks()
})

const loadTasks = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })

  try {
    const res = await api.task.getList()
    tasks.value = (res as unknown as PdaTaskItem[])
      .filter(t => t.taskType === 'PICK' && t.status === 2)
      .map(t => ({
        id: t.taskId,
        taskNo: t.taskNo,
        status: t.status,
        warehouseName: t.warehouseName,
        createTime: t.createTime,
      }))
  } catch (err) {
    console.error('[待复核] 加载失败', err)
    showToast('加载失败')
    tasks.value = []
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartShip = (task: PickedTask) => {
  router.push(`/ship/${task.id}`)
}

const handleViewDetail = (task: PickedTask) => {
  router.push(`/pick/${task.id}`)
}
</script>

<template>
  <div class="ship-list-page">
    <NavBar
      title="已拣货待复核"
      left-arrow
      @click-left="router.back()"
    />

    <div class="list-content">
      <Empty v-if="tasks.length === 0 && !loading" description="暂无待复核的拣货任务" />

      <Card
        v-for="task in tasks"
        :key="task.id"
        class="task-card"
      >
        <template #title>
          <div class="card-header">
            <span class="task-no">{{ task.taskNo }}</span>
            <Tag color="#07c160">已拣货</Tag>
          </div>
        </template>

        <template #desc>
          <div class="card-info">
            <div class="info-row">
              <span class="label">仓库:</span>
              <span class="value">{{ task.warehouseName }}</span>
            </div>
          </div>
        </template>

        <template #footer>
          <div class="card-actions">
            <Button
              type="primary"
              size="small"
              @click="handleStartShip(task)"
            >
              开始复核
            </Button>
            <Button
              type="default"
              size="small"
              @click="handleViewDetail(task)"
            >
              拣货详情
            </Button>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ship-list-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.list-content {
  padding: 12px;
}

.task-card {
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

  .card-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>
