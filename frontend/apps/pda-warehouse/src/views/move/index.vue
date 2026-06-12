<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'
import type { PdaTaskItem } from '@/types'

const router = useRouter()

interface MoveTask {
  id: number
  taskNo: string
  status: number
  warehouseName: string
  createTime: string
}

const moveTasks = ref<MoveTask[]>([])
const loading = ref(false)

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待移库', color: '#969799' },
  1: { label: '移库中', color: '#1988fa' },
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
    moveTasks.value = (tasks as unknown as PdaTaskItem[])
      .filter(t => t.taskType === 'MOVE')
      .map(t => ({
        id: t.taskId,
        taskNo: t.taskNo,
        status: t.status,
        warehouseName: t.warehouseName,
        createTime: t.createTime,
      }))
  } catch (err) {
    console.error('[移库] 加载失败', err)
    showToast('加载任务失败')
    moveTasks.value = []
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartTask = async (task: MoveTask) => {
  showLoadingToast({ message: '开始移库...', forbidClick: true })
  try {
    await api.task.start(task.id, 'MOVE')
    showToast('开始移库')
    router.push(`/move/${task.id}`)
  } catch (err) {
    console.error('[移库] 开始失败', err)
    showToast('操作失败')
  } finally {
    closeToast()
  }
}

const handleViewDetail = (task: MoveTask) => {
  router.push(`/move/${task.id}`)
}

const handleCreateMove = () => {
  router.push('/move/create')
}
</script>

<template>
  <div class="move-page">
    <NavBar title="移库任务">
      <template #right>
        <Button size="small" type="primary" @click="handleCreateMove">
          新建
        </Button>
      </template>
    </NavBar>

    <div class="move-list">
      <Empty v-if="moveTasks.length === 0 && !loading" description="暂无移库任务" />

      <Card
        v-for="task in moveTasks"
        :key="task.id"
        class="move-card"
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
              开始移库
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
.move-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.move-list {
  padding: 12px;
}

.move-card {
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
