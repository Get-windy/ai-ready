<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Field, Button, Card, Tag, Stepper, Dialog, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const taskId = Number(route.query.taskId) || 0
const scanCode = ref('')
const scannedItems = ref<any[]>([])
const scanning = ref(false)
const manualQuantity = ref(1)

onMounted(async () => {
  if (!taskId) {
    showToast('请从盘点任务中选择')
  }
})

const handleScan = async () => {
  if (!scanCode.value) {
    Dialog.alert({ message: '请输入或扫描条码' })
    return
  }

  if (!taskId) {
    Dialog.alert({ message: '请先选择盘点任务' })
    return
  }

  scanning.value = true
  showLoadingToast({ message: '验证中...', forbidClick: true })

  try {
    const res = await api.check.scanProduct(taskId, scanCode.value, manualQuantity.value)
    showToast('盘点成功')

    scannedItems.value.push({
      barcode: scanCode.value,
      productName: (res as any)?.productName || '未知商品',
      systemQuantity: (res as any)?.systemQuantity || 0,
      actualQuantity: manualQuantity.value,
      location: (res as any)?.location || '',
      status: 'success'
    })
  } catch {
    scannedItems.value.push({
      barcode: scanCode.value,
      productName: '扫描失败',
      systemQuantity: 0,
      actualQuantity: 0,
      location: '',
      status: 'error'
    })
    showToast('盘点验证失败')
  } finally {
    scanning.value = false
    scanCode.value = ''
    manualQuantity.value = 1
    closeToast()
  }
}

const handleSubmit = async () => {
  if (scannedItems.value.length === 0) {
    Dialog.alert({ message: '请先扫码盘点' })
    return
  }

  Dialog.confirm({
    title: '提交盘点',
    message: `已盘点 ${scannedItems.value.length} 件商品，确认提交？`
  }).then(async () => {
    showLoadingToast({ message: '提交中...', forbidClick: true })
    try {
      await api.check.submitResult(taskId)
      showToast('盘点提交成功')
      router.back()
    } catch (err) {
      console.error('[盘点] 提交失败', err)
      showToast('提交失败')
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleClearItems = () => {
  Dialog.confirm({
    title: '清空列表',
    message: '确定清空已盘点的商品列表？'
  }).then(() => {
    scannedItems.value = []
  }).catch(() => {})
}
</script>

<template>
  <div class="scan-page">
    <NavBar
      title="扫码盘点"
      left-arrow
      @click-left="router.back()"
    />

    <div class="scan-input">
      <div class="quantity-row">
        <span class="quantity-label">实盘数量:</span>
        <Stepper v-model="manualQuantity" :min="0" />
      </div>
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
            盘点
          </Button>
        </template>
      </Field>
    </div>

    <div class="scan-result">
      <div class="result-header">
        <span class="header-title">已盘点商品</span>
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
        请扫描商品条码进行盘点
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
              <span class="label">系统:</span>
              <span class="value">{{ item.systemQuantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">实盘:</span>
              <span class="value">{{ item.actualQuantity }}</span>
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
        @click="handleSubmit"
      >
        提交盘点
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

  .quantity-row {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    .quantity-label {
      font-size: 14px;
      color: #333;
      margin-right: 12px;
    }
  }
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
