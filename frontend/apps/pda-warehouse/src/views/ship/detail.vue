<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Button, Tag, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const taskId = Number(route.params.id)
const task = ref<any>(null)
const items = ref<any[]>([])

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.task.getDetail(taskId, 'SHIP')
    task.value = (res as any)?.task || res
    items.value = (res as any)?.items || (res as any)?.details || []
  } catch (err) {
    console.error('[复核详情] 加载失败', err)
    showToast('加载失败')
  } finally {
    closeToast()
  }
})

const handleScanShip = () => {
  router.push({ path: '/ship/scan', query: { taskId: String(taskId) } })
}

const handleCompleteTask = async () => {
  showLoadingToast({ message: '完成中...', forbidClick: true })
  try {
    await api.task.complete(taskId, 'SHIP')
    showToast('复核完成')
    router.push('/ship')
  } catch (err) {
    console.error('[复核] 完成失败', err)
    showToast('操作失败')
  } finally {
    closeToast()
  }
}

const getProgress = () => {
  if (!items.value.length) return 0
  const verified = items.value.filter((i: any) => i.status === 1 || i.status === 'verified').length
  return Math.round((verified / items.value.length) * 100)
}
</script>

<template>
  <div class="ship-detail-page">
    <NavBar
      title="复核详情"
      left-arrow
      @click-left="router.back()"
    />

    <div class="task-info">
      <div class="info-header">
        <span class="task-no">{{ task?.taskNo }}</span>
        <Tag :color="task?.status === 0 ? '#969799' : task?.status === 1 ? '#1988fa' : '#07c160'">
          {{ task?.status === 0 ? '待复核' : task?.status === 1 ? '复核中' : '已完成' }}
        </Tag>
      </div>
      <div class="progress-info">
        <span>进度: {{ getProgress() }}%</span>
        <span>{{ items.filter((i: any) => i.status === 1 || i.status === 'verified').length }}/{{ items.length }}</span>
      </div>
    </div>

    <div class="item-list">
      <div class="list-title">复核清单</div>
      <div
        v-for="item in items"
        :key="item.id"
        class="list-item"
        :class="{ completed: item.status === 1 || item.status === 'verified' }"
      >
        <div class="item-main">
          <div class="item-name">{{ item.productName }}</div>
          <Tag
            :color="item.status === 1 || item.status === 'verified' ? '#07c160' : '#969799'"
          >
            {{ item.status === 1 || item.status === 'verified' ? '已复核' : '待复核' }}
          </Tag>
        </div>
        <div class="item-meta">
          <span class="meta-text">
            <span class="label">编码:</span>
            <span>{{ item.productCode || '-' }}</span>
          </span>
          <span class="meta-text">
            <span class="label">数量:</span>
            <span class="quantity">{{ item.quantity }}</span>
          </span>
        </div>
      </div>
    </div>

    <div class="action-buttons">
      <Button
        type="primary"
        size="large"
        icon="scan"
        @click="handleScanShip"
      >
        扫码复核
      </Button>
      <Button
        v-if="task?.status === 1"
        type="success"
        size="large"
        icon="passed"
        @click="handleCompleteTask"
      >
        完成任务
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ship-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 20px;
}

.task-info {
  padding: 16px;
  background: #fff;

  .info-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .task-no {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .progress-info {
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
    color: #969799;
  }
}

.item-list {
  margin: 16px;
  background: #fff;
  border-radius: 8px;

  .list-title {
    padding: 12px;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid #ebedf0;
  }

  .list-item {
    padding: 12px;
    border-bottom: 1px solid #ebedf0;

    .item-main {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .item-name {
        font-size: 14px;
        font-weight: 500;
      }
    }

    .item-meta {
      display: flex;
      gap: 16px;
      margin-top: 6px;

      .meta-text {
        font-size: 13px;
        color: #969799;

        .label {
          margin-right: 4px;
        }

        .quantity {
          color: #333;
          font-weight: 600;
        }
      }
    }

    &.completed {
      opacity: 0.7;
    }

    &:last-child {
      border-bottom: none;
    }
  }
}

.action-buttons {
  margin: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
