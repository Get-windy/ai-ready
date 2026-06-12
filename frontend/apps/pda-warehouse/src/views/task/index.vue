<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showToast, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'
import type { PdaTaskItem } from '@/types'

const router = useRouter()
const activeTab = ref('all')

interface Task {
  id: number
  taskNo: string
  taskType: string
  status: number
  warehouseName: string
  createTime: string
}

const tasks = ref<Task[]>([])
const pendingTasks = ref<Task[]>([])
const inProgressTasks = ref<Task[]>([])
const completedTasks = ref<Task[]>([])

const taskTypeMap: Record<string, { label: string; color: string }> = {
  RECEIPT: { label: '收货', color: '#1988fa' },
  PUTAWAY: { label: '上架', color: '#7232dd' },
  PICK: { label: '拣货', color: '#07c160' },
  MOVE: { label: '移库', color: '#ff976a' },
  CHECK: { label: '盘点', color: '#f44' },
  SHIP: { label: '发货', color: '#1988fa' },
}

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待处理', color: '#969799' },
  1: { label: '进行中', color: '#1988fa' },
  2: { label: '已完成', color: '#07c160' },
}

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.task.getList()
    const list = (res as unknown as PdaTaskItem[]).map(t => ({
      id: t.taskId,
      taskNo: t.taskNo,
      taskType: t.taskType,
      status: t.status,
      warehouseName: t.warehouseName,
      createTime: t.createTime,
    }))
    tasks.value = list
    pendingTasks.value = list.filter(t => t.status === 0)
    inProgressTasks.value = list.filter(t => t.status === 1)
    completedTasks.value = list.filter(t => t.status === 2)
  } catch (err) {
    console.error('[任务] 加载失败', err)
    showToast('加载任务列表失败')
  } finally {
    closeToast()
  }
})

const handleTaskClick = (task: Task) => {
  const routeMap: Record<string, string> = {
    RECEIPT: `/receive/${task.id}`,
    PUTAWAY: `/putaway/${task.id}`,
    PICK: `/pick/${task.id}`,
    MOVE: `/move/${task.id}`,
    CHECK: `/check/${task.id}`,
    SHIP: `/ship/${task.id}`,
  }
  router.push(routeMap[task.taskType] || `/task/${task.id}?type=${task.taskType}`)
}

const handleStartTask = async (task: Task) => {
  showLoadingToast({ message: '开始处理...', forbidClick: true })
  try {
    await api.task.start(task.id, task.taskType)
    handleTaskClick(task)
  } catch (err) {
    console.error('[任务] 开始失败', err)
  } finally {
    closeToast()
  }
}
</script>

<template>
  <div class="task-page">
    <NavBar title="任务列表" />

    <Tabs v-model:active="activeTab" sticky>
      <Tab name="all" title="全部">
        <div class="task-list">
          <div v-if="tasks.length === 0" class="empty-container">
            <Empty description="暂无任务" />
          </div>

          <Card
            v-for="task in tasks"
            :key="task.id"
            class="task-card"
            @click="handleTaskClick(task)"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="taskTypeMap[task.taskType]?.color || '#969799'">
                  {{ taskTypeMap[task.taskType]?.label || task.taskType }}
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
              <div class="task-footer">
                <Tag :color="statusMap[task.status]?.color || '#969799'">
                  {{ statusMap[task.status]?.label || task.status }}
                </Tag>
              </div>
            </template>
          </Card>
        </div>
      </Tab>

      <Tab name="pending" title="待处理">
        <div class="task-list">
          <Card
            v-for="task in pendingTasks"
            :key="task.id"
            class="task-card"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="taskTypeMap[task.taskType]?.color || '#969799'">
                  {{ taskTypeMap[task.taskType]?.label || task.taskType }}
                </Tag>
              </div>
            </template>

            <template #footer>
              <Button
                type="primary"
                size="small"
                @click="handleStartTask(task)"
              >
                开始处理
              </Button>
            </template>
          </Card>
        </div>
      </Tab>

      <Tab name="in_progress" title="进行中">
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
                <Tag :color="taskTypeMap[task.taskType]?.color || '#969799'">
                  {{ taskTypeMap[task.taskType]?.label || task.taskType }}
                </Tag>
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
.task-page {
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
        width: 80px;
        color: #969799;
      }

      .value {
        color: #333;
      }
    }
  }

  .task-footer {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &.completed {
    opacity: 0.7;
  }
}
</style>
