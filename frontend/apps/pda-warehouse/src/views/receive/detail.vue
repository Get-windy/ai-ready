<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Card, Button, Tag, Stepper, Field, Uploader, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const taskId = Number(route.params.id)
const task = ref<any>(null)
const items = ref<any[]>([])
const currentItem = ref<any>(null)
const receivedQuantity = ref(0)
const batchNo = ref('')
const photos = ref<any[]>([])
const remark = ref('')

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.receive.getDetail(taskId)
    task.value = (res as any)?.task || res
    items.value = (res as any)?.items || (res as any)?.details || []

    const firstPending = items.value.find((i: any) => i.status === 0 || i.status === 'pending')
    if (firstPending) {
      currentItem.value = firstPending
      receivedQuantity.value = firstPending.quantity
    }
  } catch (err) {
    console.error('[收货详情] 加载失败', err)
    showToast('加载失败')
  } finally {
    closeToast()
  }
})

const handleScanProduct = () => {
  router.push({
    path: '/receive/scan',
    query: { taskId: String(taskId) }
  })
}

const handleConfirmReceive = async () => {
  if (!currentItem.value) return

  if (receivedQuantity.value <= 0) {
    showToast('请输入收货数量')
    return
  }

  Dialog.confirm({
    title: '确认收货',
    message: `确认收货 ${receivedQuantity.value} 件吗？`
  }).then(async () => {
    showLoadingToast({ message: '确认中...', forbidClick: true })
    try {
      await api.receive.confirmReceive(taskId, currentItem.value.locationCode)

      showToast('收货成功')

      currentItem.value.status = 1

      const nextItem = items.value.find((i: any) => i.status === 0 || i.status === 'pending')
      if (nextItem) {
        currentItem.value = nextItem
        receivedQuantity.value = nextItem.quantity
        batchNo.value = ''
        photos.value = []
        remark.value = ''
      } else {
        showToast('收货完成')
        router.push('/receive')
      }
    } catch (err) {
      console.error('[收货] 确认失败', err)
      showToast('操作失败')
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleReportException = () => {
  router.push({
    path: '/receive/exception',
    query: { taskId, itemId: currentItem.value?.id }
  })
}

const handlePhotoUpload = (file: any) => {
  photos.value.push(file)
  return false
}

const getProgress = () => {
  const received = items.value.filter((i: any) => i.status === 1 || i.status === 'received').length
  return Math.round((received / items.value.length) * 100)
}
</script>

<template>
  <div class="receive-detail-page">
    <NavBar
      title="收货详情"
      left-arrow
      @click-left="router.back()"
    />

    <div class="task-info">
      <div class="info-header">
        <span class="task-no">{{ task?.taskNo }}</span>
        <Tag color="#1988fa">收货</Tag>
      </div>
      <div class="progress-info">
        <span>进度: {{ getProgress() }}%</span>
        <span>{{ items.filter((i: any) => i.status === 1 || i.status === 'received').length }}/{{ items.length }}</span>
      </div>
    </div>

    <div v-if="currentItem" class="current-item">
      <Card class="item-card">
        <template #title>
          <div class="item-header">
            <span class="product-name">{{ currentItem.productName }}</span>
            <Tag :color="(currentItem.status === 0 || currentItem.status === 'pending') ? '#1988fa' : '#07c160'">
              {{ (currentItem.status === 0 || currentItem.status === 'pending') ? '待收' : '已收' }}
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
              <span class="label">采购数量:</span>
              <span class="value quantity">{{ currentItem.quantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">供应商:</span>
              <span class="value">{{ task?.supplierName }}</span>
            </div>
          </div>
        </template>
      </Card>

      <div class="receive-form">
        <div class="form-title">收货信息</div>

        <div class="form-row">
          <span class="label">收货数量</span>
          <Stepper
            v-model="receivedQuantity"
            :min="0"
            :max="currentItem.quantity"
          />
        </div>

        <Field
          v-model:value="batchNo"
          label="批次号"
          placeholder="请输入批次号"
        />

        <div class="form-row">
          <span class="label">拍照凭证</span>
          <Uploader
            v-model:value="photos"
            :max-count="3"
            :after-read="handlePhotoUpload"
          />
        </div>

        <Field
          v-model:value="remark"
          label="备注"
          placeholder="请输入备注"
          type="textarea"
          rows="2"
        />
      </div>

      <div class="action-buttons">
        <Button
          type="primary"
          size="large"
          icon="scan"
          @click="handleScanProduct"
        >
          扫码收货
        </Button>
        <Button
          type="success"
          size="large"
          icon="passed"
          @click="handleConfirmReceive"
        >
          确认收货
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
      <div class="list-title">收货清单</div>
      <div
        v-for="item in items"
        :key="item.id"
        class="list-item"
        :class="{ completed: item.status === 1 || item.status === 'received' }"
        @click="currentItem = item; receivedQuantity = item.quantity"
      >
        <div class="item-name">{{ item.productName }}</div>
        <div class="item-quantity">{{ item.quantity }} 件</div>
        <Tag
          v-if="item.status === 1 || item.status === 'received'"
          color="#07c160"
        >
          已收
        </Tag>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.receive-detail-page {
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

          &.quantity {
            color: #f44;
            font-weight: 600;
          }
        }
      }
    }
  }
}

.receive-form {
  margin-top: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;

  .form-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 12px;
  }

  .form-row {
    display: flex;
    align-items: center;
    padding: 12px 0;

    .label {
      width: 80px;
      color: #333;
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
