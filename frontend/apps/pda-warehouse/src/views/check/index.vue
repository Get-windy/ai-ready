<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const activeTab = ref('pending')

interface CheckTaskItem {
  id: number
  taskNo: string
  warehouseName: string
  status: number
  createTime: string
}

const pendingTasks = ref<CheckTaskItem[]>([])
const inProgressTasks = ref<CheckTaskItem[]>([])
const completedTasks = ref<CheckTaskItem[]>([])

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待盘点', color: '#969799' },
  1: { label: '盘点中', color: '#ff976a' },
  2: { label: '已完成', color: '#07c160' },
}

onMounted(() => {
  loadTasks()
})

const loadTasks = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const tasks = await api.check.getList()
    pendingTasks.value = tasks.filter(t => t.status === 0)
    inProgressTasks.value = tasks.filter(t => t.status === 1)
    completedTasks.value = tasks.filter(t => t.status === 2)
  } catch (err) {
    console.error('[盘点] 加载失败', err)
    showToast('加载盘点任务失败')
  } finally {
    closeToast()
  }
}

const handleTaskClick = (task: CheckTaskItem) => {
  router.push(`/check/${task.id}`)
}

const handleStartCheck = async (task: CheckTaskItem) => {
  Dialog.confirm({
    title: '开始盘点',
    message: `确定开始盘点 ${task.taskNo} 吗？`,
  }).then(async () => {
    showLoadingToast({ message: '开始盘点...', forbidClick: true })
    try {
      await api.task.start(task.id, 'CHECK')
      router.push(`/check/${task.id}`)
    } catch (err) {
      console.error('[盘点] 开始失败', err)
      showToast('开始盘点失败')
    } finally {
      closeToast()
    }
  }).catch(() => {
    // 用户取消
  })
}
</script>

<template>
  <div class="check-page">
    <NavBar title="盘点任务" />

    <Tabs v-model:active="activeTab" sticky>
      <Tab name="pending" title="待盘点">
        <div class="task-list">
          <div v-if="pendingTasks.length === 0" class="empty-container">
            <Empty description="暂无待盘点任务" />
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
                <Button type="primary" size="small" @click="handleStartCheck(task)">
                  开始盘点
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>

      <Tab name="in_progress" title="盘点中">
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
                <Tag color="#ff976a">盘点中</Tag>
              </div>
            </template>

            <template #desc>
              <div class="info-row">
                <span class="label">仓库:</span>
                <span class="value">{{ task.warehouseName }}</span>
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
.check-page {
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
