<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const activeTab = ref('pending')

interface ReceiveTask {
  id: number
  taskNo: string
  status: number
  warehouseName: string
  createTime: string
}

const pendingTasks = ref<ReceiveTask[]>([])
const inProgressTasks = ref<ReceiveTask[]>([])
const completedTasks = ref<ReceiveTask[]>([])

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待收货', color: '#969799' },
  1: { label: '收货中', color: '#1988fa' },
  2: { label: '已完成', color: '#07c160' },
}

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const tasks = await api.receive.getList()
    pendingTasks.value = (tasks as any[]).filter(t => t.status === 0)
    inProgressTasks.value = (tasks as any[]).filter(t => t.status === 1)
    completedTasks.value = (tasks as any[]).filter(t => t.status === 2)
  } catch (err) {
    console.error('[收货] 加载失败', err)
    showToast('加载失败')
  } finally {
    closeToast()
  }
})

const handleTaskClick = (task: ReceiveTask) => {
  router.push(`/receive/${task.id}`)
}

const handleStartReceive = async (task: ReceiveTask) => {
  showLoadingToast({ message: '开始收货...', forbidClick: true })
  try {
    await api.task.start(task.id, 'RECEIPT')
    router.push(`/receive/${task.id}`)
  } catch (err) {
    console.error('[收货] 开始失败', err)
    showToast('操作失败')
  } finally {
    closeToast()
  }
}

const handleScanReceive = () => {
  router.push('/receive/scan')
}
</script>

<template>
  <div class="receive-page">
    <NavBar title="收货任务" />

    <Tabs v-model:active="activeTab" sticky>
      <Tab name="pending" title="待收货">
        <div class="task-list">
          <div v-if="pendingTasks.length === 0" class="empty-container">
            <Empty description="暂无待收货任务" />
          </div>

          <Card
            v-for="task in pendingTasks"
            :key="task.id"
            class="task-card"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="statusMap[task.status]?.color || '#969799'">
                  {{ statusMap[task.status]?.label || task.status }}
                </Tag>
              </div>
            </template>

            <template #desc>
              <div class="task-info">
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
              <div class="task-actions">
                <Button
                  type="primary"
                  size="small"
                  @click="handleStartReceive(task)"
                >
                  开始收货
                </Button>
                <Button
                  type="default"
                  size="small"
                  icon="scan"
                  @click="handleScanReceive"
                >
                  扫码收货
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>

      <Tab name="in_progress" title="收货中">
        <div class="task-list">
          <Card
            v-for="task in inProgressTasks"
            :key="task.id"
            class="task-card"
            @click="handleTaskClick(task)"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag color="#1988fa">收货中</Tag>
              </div>
            </template>
          </Card>
        </div>
      </Tab>

      <Tab name="completed" title="已完成">
        <div class="task-list">
          <Card
            v-for="task in completedTasks"
            :key="task.id"
            class="task-card completed"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag color="#07c160">已完成</Tag>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
    </Tabs>
  </div>
</template>

<style lang="scss" scoped>
.receive-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.empty-container {
  padding: 60px 20px;
  text-align: center;
}

.task-list {
  padding: 12px;
}

.task-card {
  margin-bottom: 12px;

  .task-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .task-no {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .task-info {
    .info-row {
      display: flex;
      margin-top: 8px;

      .label {
        width: 70px;
        color: #969799;
      }

      .value {
        color: #333;
      }
    }
  }

  .task-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }

  &.completed {
    opacity: 0.7;
  }
}
</style>
