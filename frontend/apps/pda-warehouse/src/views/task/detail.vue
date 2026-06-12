<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Card, Cell, showToast, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()
const taskId = Number(route.params.id)
const taskType = (route.query.type as string) || ''

const task = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  await loadTaskDetail()
})

const loadTaskDetail = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.task.getDetail(taskId, taskType)
    task.value = (res as any)?.task || res
  } catch (err) {
    console.error('[任务详情] 加载失败', err)
    showToast('加载任务详情失败')
    task.value = null
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleBack = () => {
  router.push('/task')
}

const formatDate = (date: string): string => {
  return date ? date.replace(/-/g, '/') : '-'
}
</script>

<template>
  <div class="task-detail-page">
    <NavBar
      title="任务详情"
      left-arrow
      @click-left="handleBack"
    />

    <div class="detail-content" v-if="task">
      <Card class="info-card">
        <template #title>
          <div class="card-title">
            <span class="task-no">{{ task.taskNo }}</span>
          </div>
        </template>

        <template #desc>
          <Cell title="仓库" :value="task.warehouseName || '-'" />
          <Cell title="状态" :value="task.status === 0 ? '待处理' : task.status === 1 ? '进行中' : '已完成'" />
          <Cell title="创建时间" :value="formatDate(task.createTime)" />
          <Cell v-if="task.remark" title="备注" :value="task.remark" />
        </template>
      </Card>
    </div>

    <div v-if="!task && !loading" class="empty-state">
      <p>未找到任务信息</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.task-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.info-card {
  margin: 12px;

  .card-title {
    .task-no {
      font-size: 16px;
      font-weight: 600;
    }
  }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #969799;
}
</style>
