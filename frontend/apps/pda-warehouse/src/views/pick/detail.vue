<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Steps, Step, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

interface PickItem {
  id: number
  productName: string
  productCode: string
  batchNo: string
  location: string
  quantity: number
  pickedQuantity: number
  status: number
}

const task = ref<any>(null)
const items = ref<PickItem[]>([])
const currentItem = ref<PickItem | null>(null)
const activeStep = ref(0)

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const taskId = Number(router.currentRoute.value.params.id)
    const res = await api.pick.getDetail(taskId)
    task.value = (res as any)?.task || res
    items.value = ((res as any)?.items || []) as PickItem[]

    const firstPending = items.value.find(i => i.status === 0)
    if (firstPending) {
      currentItem.value = firstPending
    }
  } catch (err) {
    console.error('[拣货详情] 加载失败', err)
    showToast('加载失败')
  } finally {
    closeToast()
  }
})

const handleScanProduct = () => {
  router.push({
    path: '/pick/scan',
    query: { taskId: task.value?.id, itemId: currentItem.value?.id }
  })
}

const handleConfirmPick = async () => {
  if (!currentItem.value) return

  showLoadingToast({ message: '确认中...', forbidClick: true })
  try {
    await api.pick.confirmItem(currentItem.value.id, currentItem.value.pickedQuantity)
    showToast('拣货成功')

    currentItem.value.status = 1
    const nextItem = items.value.find(i => i.status === 0)
    if (nextItem) {
      currentItem.value = nextItem
    } else {
      showToast('拣货完成')
      router.push('/pick')
    }
  } catch (err) {
    console.error('[拣货] 确认失败', err)
    showToast('操作失败')
  } finally {
    closeToast()
  }
}

const handleReportException = () => {
  router.push({
    path: '/pick/exception',
    query: { taskId: task.value?.id, itemId: currentItem.value?.id }
  })
}

const getProgress = () => {
  const picked = items.value.filter(i => i.status === 1).length
  return Math.round((picked / items.value.length) * 100)
}
</script>

<template>
  <div class="pick-detail-page">
    <NavBar
      title="拣货详情"
      left-arrow
      @click-left="router.back()"
    />

    <div class="task-info">
      <div class="info-header">
        <span class="task-no">{{ task?.taskNo }}</span>
        <Tag color="#07c160">拣货</Tag>
      </div>
      <div class="progress-info">
        <span>进度: {{ getProgress() }}%</span>
        <span>{{ items.filter(i => i.status === 1).length }}/{{ items.length }}</span>
      </div>
    </div>

    <Steps :active="activeStep" active-color="#07c160">
      <Step>扫描库位</Step>
      <Step>扫描商品</Step>
      <Step>确认数量</Step>
      <Step>完成拣货</Step>
    </Steps>

    <div v-if="currentItem" class="current-item">
      <Card class="item-card">
        <template #title>
          <div class="item-header">
            <span class="product-name">{{ currentItem.productName }}</span>
            <Tag :color="currentItem.status === 0 ? '#1988fa' : '#07c160'">
              {{ currentItem.status === 0 ? '待拣' : '已拣' }}
            </Tag>
          </div>
        </template>

        <template #desc>
          <div class="item-info">
            <div class="info-row">
              <span class="label">商品编码:</span>
              <span class="value">{{ currentItem.productCode }}</span>
            </div>
            <div class="info-row">
              <span class="label">批次号:</span>
              <span class="value">{{ currentItem.batchNo }}</span>
            </div>
            <div class="info-row">
              <span class="label">库位:</span>
              <span class="value highlight">{{ currentItem.location }}</span>
            </div>
            <div class="info-row">
              <span class="label">拣货数量:</span>
              <span class="value quantity">{{ currentItem.quantity }}</span>
            </div>
          </div>
        </template>
      </Card>

      <div class="action-buttons">
        <Button
          type="primary"
          size="large"
          icon="scan"
          @click="handleScanProduct"
        >
          扫码拣货
        </Button>
        <Button
          type="success"
          size="large"
          icon="passed"
          @click="handleConfirmPick"
        >
          确认拣货
        </Button>
        <Button
          type="default"
          size="large"
          icon="warning-o"
          @click="handleReportException"
        >
          异常上报
        </Button>
      </div>
    </div>

    <div class="item-list">
      <div class="list-title">拣货清单</div>
      <div
        v-for="item in items"
        :key="item.id"
        class="list-item"
        :class="{ completed: item.status === 1 }"
        @click="currentItem = item"
      >
        <div class="item-name">{{ item.productName }}</div>
        <div class="item-location">{{ item.location }}</div>
        <div class="item-quantity">{{ item.quantity }} 件</div>
        <Tag
          v-if="item.status === 1"
          color="#07c160"
        >
          已拣
        </Tag>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.pick-detail-page {
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

.current-item {
  margin: 16px;

  .item-card {
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .product-name {
        font-size: 16px;
        font-weight: 600;
      }
    }

    .item-info {
      .info-row {
        display: flex;
        margin-top: 8px;

        .label {
          width: 80px;
          color: #969799;
        }

        .value {
          color: #333;

          &.highlight {
            color: #1988fa;
            font-weight: 600;
          }

          &.quantity {
            color: #f44;
            font-weight: 600;
          }
        }
      }
    }
  }
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
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
    display: flex;
    align-items: center;
    padding: 12px;
    border-bottom: 1px solid #ebedf0;

    .item-name {
      flex: 1;
      font-size: 14px;
    }

    .item-location {
      color: #1988fa;
      margin-right: 12px;
    }

    .item-quantity {
      color: #969799;
      margin-right: 8px;
    }

    &.completed {
      opacity: 0.7;
    }

    &:last-child {
      border-bottom: none;
    }
  }
}
</style>
