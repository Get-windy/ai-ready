<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Card, Button, Tag, Stepper, Field, showToast, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const taskId = route.params.id as string
const task = ref<any>(null)
const items = ref<any[]>([])
const currentItem = ref<any>(null)
const actualQuantity = ref(0)
const remark = ref('')

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.check.getDetail(taskId)
    task.value = res.data.task
    items.value = res.data.items
    
    const firstPending = items.value.find(i => i.status === 'pending')
    if (firstPending) {
      currentItem.value = firstPending
      actualQuantity.value = firstPending.systemQuantity
    }
  } finally {
    closeToast()
  }
})

const handleScanLocation = async () => {
  router.push({ 
    path: '/check/scan', 
    query: { taskId, type: 'location' }
  })
}

const handleScanProduct = async () => {
  router.push({ 
    path: '/check/scan', 
    query: { taskId, type: 'product', itemId: currentItem.value?.id }
  })
}

const handleConfirmCheck = async () => {
  if (!currentItem.value) return
  
  Dialog.confirm({
    title: '确认盘点',
    message: `系统数量: ${currentItem.value.systemQuantity}，实际数量: ${actualQuantity.value}`
  }).then(async () => {
    showLoadingToast({ message: '确认中...', forbidClick: true })
    try {
      await api.check.scanProduct(taskId, currentItem.value.barcode, actualQuantity.value)
      
      currentItem.value.status = 'checked'
      currentItem.value.actualQuantity = actualQuantity.value
      
      if (actualQuantity.value !== currentItem.value.systemQuantity) {
        showToast({ type: 'warning', message: '存在差异' })
      } else {
        showToast({ type: 'success', message: '盘点完成' })
      }
      
      const nextItem = items.value.find(i => i.status === 'pending')
      if (nextItem) {
        currentItem.value = nextItem
        actualQuantity.value = nextItem.systemQuantity
        remark.value = ''
      } else {
        showToast({ type: 'success', message: '盘点完成' })
        router.push('/check')
      }
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleReportDifference = async () => {
  router.push({ 
    path: '/check/difference', 
    query: { taskId, itemId: currentItem.value?.id }
  })
}

const getProgress = () => {
  const checked = items.value.filter(i => i.status === 'checked').length
  return Math.round((checked / items.value.length) * 100)
}

const getDifference = () => {
  if (!currentItem.value) return 0
  return actualQuantity.value - currentItem.value.systemQuantity
}
</script>

<template>
  <div class="check-detail-page">
    <NavBar 
      title="盘点详情" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div class="task-info">
      <div class="info-header">
        <span class="task-no">{{ task?.taskNo }}</span>
        <Tag color="#ff976a">盘点</Tag>
      </div>
      <div class="progress-info">
        <span>进度: {{ getProgress() }}%</span>
        <span>{{ items.filter(i => i.status === 'checked').length }}/{{ items.length }}</span>
      </div>
    </div>
    
    <div v-if="currentItem" class="current-item">
      <Card class="item-card">
        <template #title>
          <div class="item-header">
            <span class="product-name">{{ currentItem.productName }}</span>
            <Tag :color="currentItem.status === 'pending' ? '#ff976a' : '#07c160'">
              {{ currentItem.status === 'pending' ? '待盘' : '已盘' }}
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
              <span class="label">库位:</span>
              <span class="value highlight">{{ currentItem.location }}</span>
            </div>
            <div class="info-row">
              <span class="label">系统数量:</span>
              <span class="value system">{{ currentItem.systemQuantity }}</span>
            </div>
            <div class="info-row">
              <span class="label">批次号:</span>
              <span class="value">{{ currentItem.batchNo }}</span>
            </div>
          </div>
        </template>
      </Card>
      
      <div class="check-form">
        <div class="form-title">盘点数量</div>
        
        <div class="form-row">
          <span class="label">实际数量</span>
          <Stepper 
            v-model="actualQuantity"
            :min="0"
          />
        </div>
        
        <div class="difference-display" v-if="getDifference() !== 0">
          <span class="label">差异:</span>
          <span :class="['value', getDifference() > 0 ? 'positive' : 'negative']">
            {{ getDifference() > 0 ? '+' : '' }}{{ getDifference() }}
          </span>
        </div>
        
        <Field
          v-model="remark"
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
          扫码盘点
        </Button>
        <Button 
          type="success" 
          size="large"
          icon="passed"
          @click="handleConfirmCheck"
        >
          确认盘点
        </Button>
        <Button 
          v-if="getDifference() !== 0"
          type="warning" 
          size="large"
          icon="warning-o"
          @click="handleReportDifference"
        >
          上报差异
        </Button>
      </div>
    </div>
    
    <div class="item-list">
      <div class="list-title">盘点清单</div>
      <div 
        v-for="item in items"
        :key="item.id"
        class="list-item"
        :class="{ completed: item.status === 'checked', difference: item.actualQuantity !== item.systemQuantity }"
        @click="currentItem = item; actualQuantity = item.actualQuantity || item.systemQuantity"
      >
        <div class="item-name">{{ item.productName }}</div>
        <div class="item-location">{{ item.location }}</div>
        <div class="item-quantity">
          系统: {{ item.systemQuantity }}
          <span v-if="item.status === 'checked'" class="actual">
            / 实际: {{ item.actualQuantity }}
          </span>
        </div>
        <Tag 
          v-if="item.status === 'checked'" 
          :color="item.actualQuantity === item.systemQuantity ? '#07c160' : '#f44'"
          size="small"
        >
          {{ item.actualQuantity === item.systemQuantity ? '正常' : '差异' }}
        </Tag>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.check-detail-page {
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
          
          &.system {
            color: #333;
            font-weight: 600;
          }
        }
      }
    }
  }
}

.check-form {
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
  
  .difference-display {
    display: flex;
    align-items: center;
    padding: 12px;
    background: #f7f8fa;
    border-radius: 4px;
    margin-top: 12px;
    
    .label {
      color: #969799;
    }
    
    .value {
      font-size: 18px;
      font-weight: 600;
      margin-left: 8px;
      
      &.positive {
        color: #07c160;
      }
      
      &.negative {
        color: #f44;
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
      
      .actual {
        color: #333;
      }
    }
    
    &.completed {
      opacity: 0.7;
    }
    
    &.difference {
      background: #fff1e6;
    }
    
    &:last-child {
      border-bottom: none;
    }
  }
}
</style>