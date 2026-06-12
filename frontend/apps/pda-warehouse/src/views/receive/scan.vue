<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Field, Button, Card, Tag, Dialog, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const taskId = Number(route.query.taskId) || 0
const scanCode = ref('')
const scannedItems = ref<any[]>([])
const scanning = ref(false)

onMounted(async () => {
  if (!taskId) {
    showToast('请从收货任务中选择')
  }
})

const handleScan = async () => {
  if (!scanCode.value) {
    Dialog.alert({ message: '请输入或扫描条码' })
    return
  }

  if (!taskId) {
    Dialog.alert({ message: '请先选择收货任务' })
    return
  }

  scanning.value = true
  showLoadingToast({ message: '验证中...', forbidClick: true })

  try {
    const res = await api.receive.scanProduct(taskId, scanCode.value)
    showToast('扫码成功')

    scannedItems.value.push({
      barcode: scanCode.value,
      productName: (res as any)?.productName || '未知商品',
      quantity: (res as any)?.quantity || 1,
      location: (res as any)?.location || '',
      status: 'success'
    })
  } catch {
    scannedItems.value.push({
      barcode: scanCode.value,
      productName: '扫描失败',
      quantity: 1,
      location: '',
      status: 'error'
    })
    showToast('商品验证失败')
  } finally {
    scanning.value = false
    scanCode.value = ''
    closeToast()
  }
}

const handleConfirmReceive = async () => {
  if (scannedItems.value.length === 0) {
    Dialog.alert({ message: '请先扫码收货' })
    return
  }

  Dialog.confirm({
    title: '确认收货',
    message: `已扫描 ${scannedItems.value.length} 件商品，确认收货？`
  }).then(async () => {
    showLoadingToast({ message: '确认中...', forbidClick: true })
    try {
      await api.receive.confirmReceive(taskId)
      showToast('收货确认成功')
      router.back()
    } catch (err) {
      console.error('[收货] 确认失败', err)
      showToast('操作失败')
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleClearItems = () => {
  Dialog.confirm({
    title: '清空列表',
    message: '确定清空已扫描的商品列表？'
  }).then(() => {
    scannedItems.value = []
  }).catch(() => {})
}
</script>

<template>
  <div class="scan-page">
    <NavBar
      title="扫码收货"
      left-arrow
      @click-left="router.back()"
    />

    <div class="scan-input">
      <Field
        v-model:value="scanCode"
        placeholder="请输入或扫描条码"
        clearable
        @keyup.enter="handleScan"
      >
        <template #button>
          <Button
            size="small"
            type="primary"
            :loading="scanning"
            @click="handleScan"
          >
            扫码
          </Button>
        </template>
      </Field>
    </div>

    <div class="scan-result">
      <div class="result-header">
        <span class="header-title">已扫描商品</span>
        <span class="header-count">{{ scannedItems.length }} 件</span>
        <Button
          v-if="scannedItems.length > 0"
          size="small"
          type="default"
          @click="handleClearItems"
        >
          清空
        </Button>
      </div>

      <div v-if="scannedItems.length === 0" class="empty-result">
        请扫描商品条码
      </div>

      <Card
        v-for="(item, index) in scannedItems"
        :key="index"
        class="item-card"
      >
        <template #title>
          <div class="item-header">
            <span class="item-barcode">{{ item.barcode }}</span>
            <Tag :color="item.status === 'success' ? '#07c160' : '#f44'">
              {{ item.status === 'success' ? '成功' : '异常' }}
            </Tag>
          </div>
        </template>

        <template #desc>
          <div class="item-info">
            <div class="info-row">
              <span class="label">商品:</span>
              <span class="value">{{ item.productName }}</span>
            </div>
            <div class="info-row">
              <span class="label">数量:</span>
              <span class="value">{{ item.quantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">库位:</span>
              <span class="value">{{ item.location || '-' }}</span>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <div class="scan-actions">
      <Button
        block
        type="primary"
        :disabled="scannedItems.length === 0"
        @click="handleConfirmReceive"
      >
        确认收货
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.scan-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.scan-input {
  padding: 12px;
  background: #fff;
}

.scan-result {
  padding: 12px;

  .result-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;

    .header-title {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .header-count {
      font-size: 14px;
      color: #969799;
    }
  }

  .empty-result {
    text-align: center;
    padding: 40px 20px;
    color: #969799;
  }
}

.item-card {
  margin-bottom: 8px;

  .item-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .item-barcode {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .item-info {
    .info-row {
      display: flex;
      margin-top: 4px;

      .label {
        width: 50px;
        color: #969799;
      }

      .value {
        color: #333;
      }
    }
  }
}

.scan-actions {
  padding: 12px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
}
</style>
