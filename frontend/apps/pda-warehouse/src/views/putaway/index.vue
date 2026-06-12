<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'
import type { PdaTaskItem } from '@/types'

const router = useRouter()

interface PutawayTask {
  id: number
  taskNo: string
  status: number
  warehouseName: string
  createTime: string
}

const putawayTasks = ref<PutawayTask[]>([])
const loading = ref(false)

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '待上架', color: '#969799' },
  1: { label: '上架中', color: '#1988fa' },
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
    // 过滤出 PUTAWAY 类型的任务
    putawayTasks.value = (tasks as unknown as PdaTaskItem[])
      .filter(t => t.taskType === 'PUTAWAY')
      .map(t => ({
        id: t.taskId,
        taskNo: t.taskNo,
        status: t.status,
        warehouseName: t.warehouseName,
        createTime: t.createTime,
      }))
  } catch (err) {
    console.error('[上架] 加载失败', err)
    showToast('加载任务失败')
    putawayTasks.value = []
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartTask = async (task: PutawayTask) => {
  showLoadingToast({ message: '开始上架...', forbidClick: true })
  try {
    await api.task.start(task.id, 'PUTAWAY')
    showToast('开始上架')
    router.push(`/putaway/${task.id}`)
  } catch (err) {
    console.error('[上架] 开始失败', err)
    showToast('操作失败')
  } finally {
    closeToast()
  }
}

const handleViewDetail = (task: PutawayTask) => {
  router.push(`/putaway/${task.id}`)
}

const handleScanPutaway = () => {
  router.push('/putaway/scan')
}
</script>

<template>
  <div class="putaway-page">
    <NavBar title="上架任务">
      <template #right>
        <Button size="small" type="primary" @click="handleScanPutaway">
          扫码
        </Button>
      </template>
    </NavBar>

    <div class="putaway-list">
      <Empty v-if="putawayTasks.length === 0 && !loading" description="暂无上架任务" />

      <Card
        v-for="task in putawayTasks"
        :key="task.id"
        class="putaway-card"
      >
        <template #title>
          <div class="card-header">
            <span class="task-no">{{ task.taskNo }}</span>
            <Tag :color="statusMap[task.status].color">
              {{ statusMap[task.status].label }}
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
              开始上架
            </Button>
            <Button
              v-if="task.status === 1"
              type="primary"
              size="small"
              @click="handleScanPutaway"
            >
              扫码上架
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
.putaway-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.putaway-list {
  padding: 12px;
}

.putaway-card {
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
